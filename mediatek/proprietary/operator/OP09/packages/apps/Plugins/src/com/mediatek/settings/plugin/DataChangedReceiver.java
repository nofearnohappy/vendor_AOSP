package com.mediatek.settings.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * When the data connection is idle,switch radio mode.
 */
public class DataChangedReceiver extends BroadcastReceiver {

    private static final String TAG = "DataChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isActive = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_ACTIVE, false);
        int type = intent.getIntExtra(ConnectivityManager.EXTRA_DEVICE_TYPE, 0);
        Log.i(TAG, "data is active = " + isActive + " type = " + type +
            " sSet4GMode = " + UseLteDataSettings.sSet4GMode);
        if (type != 0) {
            return;
        }
        if (!isActive) {
            Intent broadcastIntent = new Intent(UseLteDataSettings.INTENT_ACTION_DISMISS_DIALOG);
            context.sendBroadcast(broadcastIntent);
        }
        if (!isActive && UseLteDataSettings.sSet4GMode) {
            Intent serviceIntent = new Intent(context, ChangeRatModeService.class);
            context.startService(serviceIntent);
            UseLteDataSettings.sSet4GMode = false;
        }
    }
}