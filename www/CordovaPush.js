var exec = require('cordova/exec');

module.exports = {
    register: function(successCallback, errorCallback, options){
    	var platform = device.platform;
    	if(!options)
    		options = {};
    	if(platform === 'android'){
    		if(!options.ecb)
    			options.ecb = "window.navigator.CordovaPush.onPushReceiveGCM";
    	} else if(platform === 'ios') {
    		if(!options.ecb)
    			options.ecb = "window.navigator.CordovaPush.onPushReceiveAPN";
    		if(!option.badge)
    			options.badge = "true";
    		if(!options.sound)
    			options.sound = "true";
    		if(!options.alert)
    			options.alert = "true";
    	}
        cordova.exec(successCallback, errorCallback, "CordovaPush", "register", [options]);
    },
    unregister: function(successCallback, errorCallback){
        cordova.exec(successCallback, errorCallback, "CordovaPush", "unregister", []);
    },
    setApplicationIconBadgeNumber: function(successCallback, errorCallback, badge){
        cordova.exec(successCallback, errorCallback, "CordovaPush", "setApplicationIconBadgeNumber", [{badge: badge}]);
    },
    onPushReceiveGCM: function(e){
    	var evt = document.createEvent('CustomEvent');
    	evt.initCustomEvent('pushEvent', true, true, e);
    	window.dispatchEvent(evt);
    },
    onPushReceiveAPN: function(e){
    	var evt = document.createEvent('CustomEvent');
    	evt.initCustomEvent('pushEvent', true, true, e);
    	window.dispatchEvent(evt);
    }
};