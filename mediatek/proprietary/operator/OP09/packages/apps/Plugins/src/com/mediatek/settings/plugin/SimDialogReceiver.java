package com.mediatek.settings.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * When user insert 3g cdma card or incorrect card.
 * Show a dialog
 */
public class SimDialogReceiver extends BroadcastReceiver {

    private static final String TAG = "SimDialogReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action = " + action);
        Intent serviceIntent = new Intent(SimDialogService.ACTION_NAME);
        serviceIntent.setPackage("com.mediatek.op09.plugin");
        serviceIntent.putExtra(SimDialogService.EXTRA_NAME, intent);
        context.startService(serviceIntent);
    }
}