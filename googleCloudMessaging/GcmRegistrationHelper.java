package com.slickup.android.googleCloudMessaging;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by Jaison on 14/04/16.
 */
public class GcmRegistrationHelper {

    public interface Listener {
        void gcmRegistrationSuccessful();
        void gcmRegistrationFailed();
    }

    private Listener listener;
    private static GcmRegistrationHelper instance;
    private Activity activity;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "GcmRegHelper";
    private boolean isReceiverRegistered;

    public static GcmRegistrationHelper getInstance() {
        if (instance == null)
            instance = new GcmRegistrationHelper();
        return instance;
    }

    public void handleActivityOnResume() {
        if (activity != null)
            registerReceiver();
    }

    public void handleActivityOnPause() {
        if (activity != null)
            unregisterReceiver();
    }

    public void handleActivityOnDestroy() {
        activity = null;
        instance = null;
        listener = null;
    }

    /**
     * REGISTERATION SET UP
     **/
    public void register(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        registerReceiver();
        startGCMRegistrationIntentService();
    }

    private void startGCMRegistrationIntentService() {
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(activity, GcmRegistrationIntentService.class);
            activity.startService(intent);
        }
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(activity).registerReceiver(gcmRegistrationBroadcastListener,
                    new IntentFilter(GcmPreferenceKeys.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(gcmRegistrationBroadcastListener);
        isReceiverRegistered = false;
    }

    private BroadcastReceiver gcmRegistrationBroadcastListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            boolean sentToken = sharedPreferences
                    .getBoolean(GcmPreferenceKeys.SENT_TOKEN_TO_SERVER, false);
            if (sentToken) {
                listener.gcmRegistrationSuccessful();
            } else {
                listener.gcmRegistrationFailed();
            }
        }
    };

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                activity.finish();
            }
            return false;
        }
        return true;
    }

}
