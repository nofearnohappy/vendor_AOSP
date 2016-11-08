package com.mediatek.systemupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageVolume;
import android.util.Log;


/**
 *
 * Listen the change of storage.
 *
 */
public class StorageReceiver extends BroadcastReceiver {

    private static final String TAG = "SystemUpdate/Storage";
    private static boolean sShutdownFlag;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (Util.Action.ACTION_SHUTDOWN.equals(action)) {

            Log.i("@M_" + TAG, "receive shutdown broadcast");
            DownloadInfo.getInstance(context).setIsShuttingDown(true);

        } else if (Util.Action.ACTION_BOOT_COMPLETED.equals(action)) {

            Log.i("@M_" + TAG, "receive bootcomplete broadcast");
            DownloadInfo.getInstance(context).setIsShuttingDown(false);

        } else {
            if (DownloadInfo.getInstance(context).getIsShuttingDown()) {
                Log.i("@M_" + TAG, "is Shutting down, ingnore media broadcast");
                return;
            }

            boolean isOtaOnly = Util.getUpdateType().equals(Util.UPDATE_TYPES.OTA_UPDATE_ONLY);
            if (Util.Action.ACTION_MEDIA_MOUNTED.equals(action) && isOtaOnly) {
                Log.w("@M_" + TAG, "--- OTA update, ingore sdcard mounted action---");
                return;
            }

            StorageVolume sv = (StorageVolume) intent.getExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
            if (sv == null) {
                Log.w("@M_" + TAG, "---StorageVolume get from intent is null---");
                return;
            }

            String storagePath = (String) sv.getPath();
            String availablePath = Util.getAvailablePath(context);
            if (isOtaOnly && availablePath != null && !availablePath.contains(storagePath)) {
                Log.w("@M_" + TAG, "--- OTA update, not default sdcard, " +
                        "ingore sdcard unmounted action---");
                return;
            }

            Intent service = new Intent(intent);
            service.setClass(context, SystemUpdateService.class);
            context.startService(service);
        }
    }

}