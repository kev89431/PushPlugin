package io.hiro.cordovapush;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

public class CordovaPush extends CordovaPlugin {

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String TAG = "CordovaPush";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_REG_ID = "registration_id";

    private Context context;
    private GoogleCloudMessaging gcm;
    private String appVersion;
    private static String ecb;
    private static CordovaWebView cwv;
    private String senderID;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	cwv = webView;
    	super.initialize(cordova, webView);
    	//this.cordova.getActivity().registerReceiver(mHandleMessageReceiver, new IntentFilter());
    	context = this.cordova.getActivity().getApplicationContext();

    	/* TODO - automatic registration
			Move id checks to the auto registration, they aren't needed for a foreceful registration
				regid = getRegistrationId(context);

	            if (regid.isEmpty()) {
	                registerInBackground();
	            }
    	*/
    }

   	@Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {

		Log.v(TAG, "Execute: action=" + action);
		Log.v(TAG, "Execute: data=" + data.toString());

		boolean result = false;
           
		if(action.equals("register")) {
			try {
	    		JSONObject jo = data.getJSONObject(0);
	    		ecb = (String) jo.get("ecb");
	    		senderID = (String) jo.get("senderID");
	    		if (checkPlayServices()) {
		            registerInBackground(callbackContext);
		            result = true;
		        } else {
		            Log.i(TAG, "Unable to register notifications on this device. Google Play Services APK not found.");
		        }
	    	} catch (JSONException e) {
	    		Log.e(TAG, "JSONException: " + e.getMessage());
	    	}
		} else if(action.equals("unregister")) {
			/// Unregister is fairly useless, so let's worry about this one later. We should implement
			// for back compat though.
		}

		return result;
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this.cordova.getActivity(),
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.i(TAG, "This device is not supported.");
	            return false;
	        }
	        return false;
	    }
	    return true;
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 *
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground(final CallbackContext callbackContext) {
	    new AsyncTask<Void, Void, Void>() {
	        @Override
	        protected Void doInBackground(Void... params) {
	        	String regID;
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                regID = gcm.register(senderID);

	                // Upon successful registration, return the ID to Cordova through the
	                // success callback.
	                callbackContext.success(regID);

	                // Return result as a notification registration to maintain compatibility
	                // for PushPlugin users. New users should use the regID returned via
	                // the success callback (above).
					try
					{
						JSONObject jo = new JSONObject().put("event", "registered");
						jo.put("regid", regID);
						CordovaPush.sendJavascript(jo);
					} catch( JSONException e)
					{
						Log.e(TAG, "Registration result: JSON exception");
					}

	                // Persist the regID
	                storeRegistrationId(context, regID);
	            } catch (IOException ex) {
	            	// Return an error to the callback so that it can be dealt with in JavaScript
	                callbackContext.error(ex.getMessage());
	            }
	            return null;
	        }
	    }.execute(null, null, null);
	}

	/**
     * Called when the activity receives a new intent.
     */
    public void onNewIntent(Intent intent) {
	    Log.v(TAG, "Activity recieved notification: " + intent);
        Bundle extras = intent.getExtras();
        sendExtras(extras);
    }

/*
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	Log.v(TAG, "Activity recieved notification: " + intent);
            //String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            Bundle extras = intent.getExtras();
            sendExtras(extras);
        }
    };
    */

	/**
	 * Gets the current registration ID for application on GCM service.
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.isEmpty()) {
	        Log.i(TAG, "Registration not found.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        Log.i(TAG, "App version changed.");
	        return "";
	    }
	    return registrationId;
	}

	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    // This sample app persists the registration ID in shared preferences, but
	    // how you store the regID in your app is up to you.
	    return this.cordova.getActivity().getSharedPreferences(CordovaPush.class.getSimpleName(),
	            Context.MODE_PRIVATE);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // Should never happen...
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

	/*
	 * Sends a json object to the client as parameter to a method which is defined in gECB.
	 */
	public static void sendJavascript(JSONObject _json) {
		String _d = "javascript:" + ecb + "(" + _json.toString() + ")";
		Log.v(TAG, "sendJavascript: " + _d);

		if (ecb != null && cwv != null) {
			cwv.sendJavascript(_d); 
		}
	}

	/*
	 * Sends the pushbundle extras to the client application.
	 * If the client application isn't currently active, it is cached for later processing.
	 */
	public static void sendExtras(Bundle extras)
	{
		if (extras != null) {
			if (ecb != null && cwv != null) {
				sendJavascript(convertBundleToJson(extras));
			} else {
				Log.v(TAG, "sendExtras: caching extras to send at a later time.");
				// gCachedExtras = extras;
				// C'mon, this should never happen, and if it does there isn't much that can be done about it.
			}
		}
	}
	
	/*
	 * Serializes a bundle to JSON.
	 */
    private static JSONObject convertBundleToJson(Bundle extras)
    {
		try
		{
			JSONObject json;
			json = new JSONObject().put("event", "message");
        
			JSONObject jsondata = new JSONObject();
			Iterator<String> it = extras.keySet().iterator();
			while (it.hasNext())
			{
				String key = it.next();
				Object value = extras.get(key);	
        	
				// System data from Android
				if (key.equals("from") || key.equals("collapse_key"))
				{
					json.put(key, value);
				}
				else if (key.equals("foreground"))
				{
					json.put(key, extras.getBoolean("foreground"));
				}
				else if (key.equals("coldstart"))
				{
					json.put(key, extras.getBoolean("coldstart"));
				}
				else
				{
					if ( value instanceof String ) {
					// Try to figure out if the value is another JSON object
						
						String strValue = (String)value;
						if (strValue.startsWith("{")) {
							try {
								JSONObject json2 = new JSONObject(strValue);
								jsondata.put(key, json2);
							}
							catch (Exception e) {
								jsondata.put(key, value);
							}
							// Try to figure out if the value is another JSON array
						}
						else if (strValue.startsWith("["))
						{
							try
							{
								JSONArray json2 = new JSONArray(strValue);
								jsondata.put(key, json2);
							}
							catch (Exception e)
							{
								jsondata.put(key, value);
							}
						}
						else
						{
							jsondata.put(key, value);
						}
					}
				}
			} // while
			json.put("payload", jsondata);
        
			Log.v(TAG, "extrasToJSON: " + json.toString());

			return json;
		}
		catch( JSONException e)
		{
			Log.e(TAG, "extrasToJSON: JSON exception");
		}        	
		return null;      	
    }
}