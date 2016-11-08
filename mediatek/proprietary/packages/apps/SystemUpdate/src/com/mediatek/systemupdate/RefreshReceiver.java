package com.mediatek.systemupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * Refresh.
 */
public class RefreshReceiver extends BroadcastReceiver {

    private static final String TAG = "RefreshReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("@M_" + TAG, "[onReceiver] " + intent.getAction());

        DownloadInfo downloadInfo = DownloadInfo.getInstance(context.getApplicationContext());
        downloadInfo.setIfNeedRefresh(true);
        //ALPS01676206
        if (downloadInfo.getDLSessionStatus() == DownloadInfo.STATE_NEWVERSION_READY) {
             downloadInfo.setDLSessionStatus(DownloadInfo.STATE_QUERYNEWVERSION);
        }
        (new NotifyManager(context)).clearNotification(NotifyManager.NOTIFY_NEW_VERSION);
    }
}