var exec = require('cordova/exec');

exports.auth = function(arg0, success, error) {
    exec(success, error, "googleDriveSync", "auth", [arg0]);
};

exports.putData = function(arg0, success, error) {
    exec(success, error, "googleDriveSync", "putData", [arg0]);
};

exports.getData = function(arg0, success, error) {
    exec(success, error, "googleDriveSync", "getData", [arg0]);
};

exports.isConnected = function(arg0, success, error) {
    exec(success, error, "googleDriveSync", "isConnected", [arg0]);
};


