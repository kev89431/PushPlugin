var exec = require('cordova/exec');

module.exports = {
    register: function(successCallback, errorCallback, options){
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
    }
};