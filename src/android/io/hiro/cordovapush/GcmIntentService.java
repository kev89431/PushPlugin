package io.hiro.cordovapush;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

public class GcmIntentService extends IntentService {

	private static final String TAG = "CordovaPush/GcmIntentService";
    private static final int NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            // Filter messages based on message type.
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.v(TAG, "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.v(TAG, "Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            	// If the app is running in the foreground, send the message directly to
            	// the activity. Otherwise, post a notification to the system.
				boolean	foreground = this.isForeground();
				if (foreground) {
					extras.putBoolean("foreground", foreground);
					CordovaPush.sendExtras(extras);
				} else {
					sendNotification(extras);
				}
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    private void sendNotification(Bundle extras) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        String appName = getAppName();

        Intent notificationIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtras(extras);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = 
        	new NotificationCompat.Builder(this)
				.setAutoCancel(true)
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
		        .setContentIntent(contentIntent)
		        .setContentTitle(appName)
		        .setSmallIcon(this.getApplicationInfo().icon)
		        .setTicker(appName)
		        .setWhen(System.currentTimeMillis());
		        
		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

	// Credit where credit is due - http://stackoverflow.com/a/5841353/1558645
	private String getAppName()
	{
		final PackageManager pm = getApplicationContext().getPackageManager();
		ApplicationInfo ai;
		try {
		    ai = pm.getApplicationInfo( this.getPackageName(), 0);
		} catch (final NameNotFoundException e) {
		    ai = null;
		}
		final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
		return applicationName;
	}

	private boolean isForeground()
	{
		ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> services = activityManager
				.getRunningTasks(Integer.MAX_VALUE);

		if (services.get(0).topActivity.getPackageName().toString().equalsIgnoreCase(getApplicationContext().getPackageName().toString()))
			return true;

		return false;
	}	

}