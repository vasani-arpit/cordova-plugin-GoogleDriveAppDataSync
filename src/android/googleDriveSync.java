package googleDriveSync;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet.Builder;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import googleDriveSync.IOUtils;
import java.io.IOException;
import java.util.Date;

public class googleDriveSync extends CordovaPlugin implements ConnectionCallbacks, OnConnectionFailedListener {
    private String TAG = "DriveAppDataPlugin";
    GoogleApiClient mGoogleApiClient;
    boolean mResolvingError = false;
    private CallbackContext tryConnectCallback = null;
    DriveId file;
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("auth")) {
            PluginResult result = auth(args.getString(0), callbackContext);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("putData")) {
            String filContent = args.getString(0);
            file = getOrCreateFileID("appData.txt", "text/plain");
            if (file != null) {
                try {
                    if (!writeToFile(file, filContent)) {
                        return true;
                    }
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, readFromFile(file)));
                    return true;
                } catch (IOException e) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Error in writing file."));
                    return true;
                }
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Null returned from GoogleDrive"));
            return true;
        } else {
            if (action.equals("isConnected")) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, this.mGoogleApiClient.isConnected()));
            }
            if (action.equals("getData")) {
                file = getOrCreateFileID("appData.txt", "text/plain");
                if (file != null) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, readFromFile(file)));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Null returned from GoogleDrive"));
                }
            }
            return false;
        }
    }

    private boolean writeToFile(DriveId id, String content) throws IOException {
            DriveContentsResult result = (DriveContentsResult) Drive.DriveApi.getFile(this.mGoogleApiClient, id).open(this.mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
        try {
           IOUtils.writeToStream(content, result.getDriveContents().getOutputStream());
            return ((Status) result.getDriveContents().commit(this.mGoogleApiClient, new Builder().setLastViewedByMeDate(new Date()).build()).await()).isSuccess();
        } catch (IOException io) {
            result.getDriveContents().discard(this.mGoogleApiClient);
            throw io;
        }
    }

    private DriveId getOrCreateFileID(String fileName, String mimeType) {
        DriveId file = getDriveFile(fileName, mimeType);
        if (file == null) {
            return createFile(fileName, mimeType);
        }
        return file;
    }

    private DriveId getDriveFile(String fileName, String mimeType) {
        MetadataBuffer buffer = null;
        try {
            buffer = ((MetadataBufferResult) Drive.DriveApi.getAppFolder(this.mGoogleApiClient).queryChildren(this.mGoogleApiClient, new Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.TITLE, fileName), new Filter[]{Filters.eq(SearchableField.MIME_TYPE, mimeType)})).setSortOrder(new SortOrder.Builder().addSortDescending(SortableField.MODIFIED_DATE).build()).build()).await()).getMetadataBuffer();
            DriveId driveId;
            if (buffer == null || buffer.getCount() <= 0) {
                driveId = null;
                if (buffer != null) {
                    buffer.close();
                }
                return driveId;
            }
            Log.d(this.TAG, "got buffer " + buffer.getCount());
            driveId = buffer.get(0).getDriveId();
            return driveId;
        } finally {
            if (buffer != null) {
                buffer.close();
            }
        }
    }

    private PluginResult auth(String message, CallbackContext callbackContext) {
        if (message == null || message.length() <= 0) {
            callbackContext.error("Expected one non-empty string argument.");
            return new PluginResult(PluginResult.Status.ERROR, "Expected one non-empty string argument.");
        }
        this.mGoogleApiClient = new GoogleApiClient.Builder(this.cordova.getActivity().getApplicationContext()).addApi(Drive.API).addScope(Drive.SCOPE_FILE).addScope(Drive.SCOPE_APPFOLDER).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(AppIndex.API).build();
        this.mGoogleApiClient.connect();
        this.tryConnectCallback = callbackContext;
        return new PluginResult(PluginResult.Status.OK, "Authenticating");
    }

    public void onConnected(Bundle bundle) {
        Log.i(this.TAG, "GoogleApiClient connected");
        Log.i(this.TAG, String.valueOf(bundle));
        this.tryConnectCallback.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Successfully Connected"));
    }

    private DriveId createFile(String name, String mime) {
        try {
            DriveFolder driveAppFolder = Drive.DriveApi.getAppFolder(this.mGoogleApiClient);
            DriveContents cont = ((DriveContentsResult) Drive.DriveApi.newDriveContents(this.mGoogleApiClient).await()).getDriveContents();
            DriveFile df = ((DriveFileResult) driveAppFolder.createFile(this.mGoogleApiClient, new Builder().setTitle(name).setMimeType(mime).build(), cont).await()).getDriveFile();
            Log.i(this.TAG, "" + df.getDriveId().encodeToString());
            return df.getDriveId();
        } catch (Exception e) {
            Log.i(this.TAG, "" + e);
            return null;
        }
    }


    String readFromFile(DriveId drvId) {
        DriveContents driveContents = ((DriveContentsResult) Drive.DriveApi.getFile(this.mGoogleApiClient, drvId).open(this.mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await()).getDriveContents();
        if (driveContents != null) {
            try {
                String file = IOUtils.readAsString(driveContents.getInputStream());
                Log.d(this.TAG, "file Contents are " + file);
                if (driveContents == null) {
                    return file;
                }
                driveContents.discard(this.mGoogleApiClient);
                return file;
            } catch (Exception e) {
                Log.d(this.TAG, "ERROR: " + e);
                if (driveContents != null) {
                    driveContents.discard(this.mGoogleApiClient);
                }
            } catch (Throwable th) {
                if (driveContents != null) {
                    driveContents.discard(this.mGoogleApiClient);
                }
            }
        } else {
            if (driveContents != null) {
                driveContents.discard(this.mGoogleApiClient);
            }
            return null;
        }
        return null;
    }
    public void onConnectionSuspended(int i) {
    }

    public void onConnectionFailed(ConnectionResult result) {
        Log.w(this.TAG, result.toString());
        Log.i(this.TAG, "GoogleApiClient connection failed: " + result.toString());
        this.tryConnectCallback.error("GoogleApiClient connection failed");
        if (!this.mResolvingError) {
            if (result.hasResolution()) {
                try {
                    this.mResolvingError = true;
                    this.cordova.setActivityResultCallback(this);
                    result.startResolutionForResult(this.cordova.getActivity(), 200);
                    return;
                } catch (SendIntentException e) {
                    Log.e(this.TAG, "Exception while starting resolution activity", e);
                    this.tryConnectCallback.error("Exception while starting resolution activity");
                    return;
                }
            }
            this.mResolvingError = true;
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this.cordova.getActivity(), 0).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(this.TAG, "onActivityResult: " + requestCode + " " + resultCode);
        if (requestCode == 200) {
            this.mResolvingError = false;
            if (resultCode == -1 && !this.mGoogleApiClient.isConnecting() && !this.mGoogleApiClient.isConnected()) {
                this.mGoogleApiClient.connect();
            }
        }
    }
}
