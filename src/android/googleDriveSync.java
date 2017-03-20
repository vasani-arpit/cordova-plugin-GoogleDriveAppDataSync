package googleDriveSync;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class googleDriveSync extends CordovaPlugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    GoogleApiClient mGoogleApiClient;
    boolean mResolvingError = false;
    private String TAG = "DriveAppDataPlugin";
    private CallbackContext tryConnectCallback = null;
    PluginResult result;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("auth")) {
            final String message = args.getString(0);
            this.auth(message, callbackContext);
            result = new PluginResult(PluginResult.Status.OK, "Some Success");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        return false;
    }

    private void auth(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            mGoogleApiClient = new GoogleApiClient.Builder(this.cordova.getActivity().getApplicationContext())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(AppIndex.API).build();

            mGoogleApiClient.connect();
            tryConnectCallback = callbackContext;
            //result = new PluginResult(PluginResult.Status.OK, "Authenticating");

        } else {
            callbackContext.error("Expected one non-empty string argument.");
            result = new PluginResult(PluginResult.Status.ERROR, "Expected one non-empty string argument.");
        }
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        Log.i(TAG, String.valueOf(bundle));
        //createFile(title, mime, buff);
        PluginResult result = new PluginResult(PluginResult.Status.OK, "Successfully Connected");
        tryConnectCallback.sendPluginResult(result);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        String errormessage = result.toString();
        Log.w(TAG, errormessage);
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        this.result = new PluginResult(PluginResult.Status.ERROR, errormessage);
        tryConnectCallback.sendPluginResult(this.result);
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else {
            if (!result.hasResolution()) {
                mResolvingError = true;
                // show the localized error dialog.
                GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), cordova.getActivity(), 0).show();
                return;
            }
            try {
                mResolvingError = true;
                cordova.setActivityResultCallback(this);
                result.startResolutionForResult(cordova.getActivity(), 200);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while starting resolution activity", e);
                tryConnectCallback.error("Exception while starting resolution activity");
            }
        }
    }
}
