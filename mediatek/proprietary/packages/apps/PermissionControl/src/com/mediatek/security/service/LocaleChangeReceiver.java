package com.mediatek.security.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;



public class LocaleChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "LocaleChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d("@M_" + TAG, "onReceive with action = " + action);
            if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            	// cancel it and then show ,ALPS01729286
            	PermControlUtils.cancelNotification(context);
            	PermControlUtils.showHintNotify(context);
            }
        }
    }
}