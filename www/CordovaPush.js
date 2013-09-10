var exec = require('cordova/exec');

module.exports = {
    register: function(successCallback, errorCallback, options){
        var opt = options;
    	var platform = device.platform;
    	if(!opt)
    		opt = {};
    	if(platform == 'Android'){
    		if(!opt.ecb)
    			opt.ecb = "window.navigator.CordovaPush.onPushReceiveGCM";
    	} else if(platform == 'iOS') {
    		if(!opt.ecb)
    			opt.ecb = "window.navigator.CordovaPush.onPushReceiveAPN";
    		if(!opt.badge)
    			opt.badge = "true";
    		if(!opt.sound)
    			opt.sound = "true";
    		if(!opt.alert)
    			opt.alert = "true";
    	}
        console.log("DUXTER EXEC: " + opt);
        cordova.exec(successCallback, errorCallback, "CordovaPush", "register", [opt]);
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