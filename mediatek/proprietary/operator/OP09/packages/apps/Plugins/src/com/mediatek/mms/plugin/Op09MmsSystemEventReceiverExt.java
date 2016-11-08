package com.mediatek.mms.plugin;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.mediatek.mms.ext.DefaultOpMmsSystemEventReceiverExt;
import com.mediatek.storage.StorageManagerEx;

public class Op09MmsSystemEventReceiverExt extends
        DefaultOpMmsSystemEventReceiverExt {

    private static final String TAG = "Mms/Op09MmsSystemEventReceiverExt";

    public Op09MmsSystemEventReceiverExt(Context context) {
        super(context);
    }

    public void onReceive(Context context, Intent intent, final int tempFileNameLen) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Op09UnreadMessageNumberExt.getIntance().updateUnreadMessageNumber(context);
        } else if (action.equals(MessageUtils.ACTION_STORAGE_LOW)) {
            Log.d(TAG, "setCTDeviceStorageLowStatus(true)");
            MessageUtils.setCTDeviceStorageLowStatus(true);

            final Context finalContext = context;
            new Thread(new Runnable() {
                public void run() {
                    deleteAttachmentCache(finalContext, tempFileNameLen);
                }
            }).start();
        } else if (action.equals(MessageUtils.ACTION_STORAGE_NOT_LOW)) {
            Log.d(TAG, "setCTDeviceStorageLowStatus(false)");
            MessageUtils.setCTDeviceStorageLowStatus(false);
            MessageUtils.cancelCTDeviceLowNotification(context);
        }
    }

    /// M: delete Attachment Cache File
    public void deleteAttachmentCache(Context c, int tempFileNameLen) {
        Log.d(TAG, "delete Attachment Cache File begin");
        File mTempDir = StorageManagerEx.getExternalCacheDir(c.getPackageName());
        if (mTempDir != null && mTempDir.exists() && mTempDir.isDirectory()) {
            File file[] = mTempDir.listFiles();
            if (file != null) {
                for (int i = 0; i < file.length; i++) {
                    if (file[i] != null && file[i].exists() && file[i].isFile()
                            && file[i].getName().length() > tempFileNameLen) {
                        file[i].delete();
                    }
                }
            }
        }
        Log.d(TAG, "delete Attachment Cache File end");
    }
    /// @}

    @Override
    public void setNotificationIndUnstarted(ContentValues values, int status) {
        if (MessageUtils.isCancelDownloadEnable()) {
            values.put(Mms.STATUS_EXT, 0);
        }
    }
}
