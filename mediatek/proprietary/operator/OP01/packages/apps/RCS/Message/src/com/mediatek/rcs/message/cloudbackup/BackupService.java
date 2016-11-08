package com.mediatek.rcs.message.cloudbackup;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.mediatek.rcs.message.cloudbackup.modules.FavMsgBackup;
import com.mediatek.rcs.message.cloudbackup.modules.MessageBackupComposer;
import com.mediatek.rcs.message.cloudbackup.store.MessageStore;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class BackupService extends Service {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "BackupService";

    private BackupBinder mBinder = new BackupBinder();
    protected PowerManager.WakeLock mWakeLock;
    private Handler mHandler;
  //  private int mBackupItems;
    private String mBackupFolder;
    private BackupThread mBackupThread;
    private Context mContext;

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(CLASS_TAG, "onCreate");
    }

    public class BackupBinder extends Binder {
        public int startBackup() {
            Log.d(CLASS_TAG, "BackupBinder start backup");
            createWakeLock();
            if (mWakeLock != null) {
                acquireWakeLock();
                Log.d(CLASS_TAG, "BackupService : startBackup: call acquireWakeLock()");
            }

            mBackupThread = new BackupThread(mContext);
            mBackupThread.setBackupFolder(mBackupFolder);
            mBackupThread.setHandler(mHandler);
            mBackupThread.start();
            return CloudBrUtils.ResultCode.OK;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        /**
         * backupFolder -msgdataFolder -timestamp -favoriteFolder -timestamp
         * backupFolder is parent foder of msg data and fovorite data.
         *
         * @param backupFolder
         */
        public void setBackupFolder(String backupFolder) {
            Log.d(CLASS_TAG, "BackupBinder setBackupFolder = " + backupFolder);
            File file = new File(backupFolder);
            if (file != null && file.exists()) {
                FileUtils.deleteFileOrFolder(file);
            }
            mBackupFolder = backupFolder;
        }

        public void cancelBackup() {
            Log.d(CLASS_TAG, "BackupService : cancelBackup");
            if (mWakeLock != null) {
                releaseWakeLock();
                Log.d(CLASS_TAG, "cancelBackupRestore: call releseWakeLock()");
            }
            if (mBackupThread != null) {
                mBackupThread.setCancel(true);
            }
        }

        public void setContext(Context context) {
            mContext = context;
        }

        public void reset() {
            mBackupThread = null;
            mHandler = null;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(CLASS_TAG, "onStartCommand");
        return START_STICKY_COMPATIBILITY;
    }

    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(CLASS_TAG, "onRebind");
    }

    public void onDestroy() {
        if (mWakeLock != null) {
            releaseWakeLock();
            Log.d(CLASS_TAG, "onDestroy(): call releseWakeLock()");
        }
        super.onDestroy();
        Log.i(CLASS_TAG, "onDestroy");
    }

    public class BackupThread extends Thread {
        private boolean mCancel = false;
        private String mBackupFolder;
        private Context mContext;
        private FavMsgBackup mFavMsgBackup;
        private MessageBackupComposer mMessageBackupComposer;
        private String mMessageFolderPath;
        private String mFavoriteFolderPath;

        BackupThread(Context context) {
            mContext = context;
        }

        public void setCancel(boolean cancel) {
            mCancel = cancel;
            if (mMessageBackupComposer != null) {
                mMessageBackupComposer.setCancel(cancel);
            }

            if (mFavMsgBackup != null) {
                mFavMsgBackup.setCancel(cancel);
            }
        }

        public int setBackupFolder(String backupFolder) {
            mBackupFolder = backupFolder;
            File folder = new File(mBackupFolder);
            if (folder != null && folder.exists()) {
                Log.d(CLASS_TAG, "backup folder exit, delete it");
                folder.delete();
            }
            folder.mkdirs();
            return CloudBrUtils.ResultCode.OK;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void run() {
            int backupResult = CloudBrUtils.ResultCode.OK;
            if (mCancel) {
                Log.d(CLASS_TAG, "BackupThread is be canceled. return");
                sendBackupResult(CloudBrUtils.ResultCode.SERVICE_CANCELED);
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            Log.d(CLASS_TAG, "BackupThread, begin backup message msg");
            mMessageFolderPath = mBackupFolder + MessageStore.RemoteFolderName.MSG_BACKUP
                    + File.separator + dateFormat.format(new Date(System.currentTimeMillis()));

            Log.d(CLASS_TAG, "mMessageFolderPath = " + mMessageFolderPath);
            mMessageBackupComposer = new MessageBackupComposer(mContext, mMessageFolderPath);
            backupResult = mMessageBackupComposer.backupData();

            if (backupResult != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "BackupThread, mMessageBackupComposer.backupData error, return");
                sendBackupResult(backupResult);
                return;
            }
            mMessageBackupComposer = null;

            if (mCancel) {
                Log.d(CLASS_TAG, "BackupThread is be canceled. return");
                backupResult = CloudBrUtils.ResultCode.SERVICE_CANCELED;
                sendBackupResult(backupResult);
                return;
            }

            Log.d(CLASS_TAG, "BackupThread, begin backup favorite msg");
            String dateString = dateFormat.format(new Date(System.currentTimeMillis()));
            mFavoriteFolderPath = mBackupFolder + MessageStore.RemoteFolderName.MSG_FAVORITE
                    + File.separator + dateString;
            Log.d(CLASS_TAG, "favoriteFolderPath = " + mFavoriteFolderPath);

            mFavMsgBackup = new FavMsgBackup(mContext, mFavoriteFolderPath);
            backupResult = mFavMsgBackup.backupData();
            if (backupResult != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "BackupThread, mFavMsgBackup.backupData error, return");
                sendBackupResult(backupResult);
                return;
            }
            mFavMsgBackup = null;
            Log.d(CLASS_TAG, "BackupThread, backup favorite msg end");

            if (mCancel) {
                Log.d(CLASS_TAG, "BackupThread is be canceled. return");
                backupResult = CloudBrUtils.ResultCode.SERVICE_CANCELED;
                sendBackupResult(backupResult);
                return;
            }

            sendBackupResult(CloudBrUtils.ResultCode.OK);
            Log.d(CLASS_TAG, "BackupThread backup end, return");
        }

        private void sendBackupResult(int backupResult) {
            // send backup result to activity.
            Log.d(CLASS_TAG, "sendBackupResult = " + backupResult);

            Message msg = new Message();
            msg.what = CloudBrUtils.MessageID.BACKUP_END;
            Bundle bundle = new Bundle();
            bundle.putInt(CloudBrUtils.BACKUP_RESULT, backupResult);

            if (backupResult != CloudBrUtils.ResultCode.OK) {
                File backupFile = new File(mBackupFolder);
                if (backupFile != null && backupFile.exists()) {
                    Log.d(CLASS_TAG, "sendBackupResult delete backup local folder");
                    backupFile.delete();
                }
            } else {
                bundle.putString(CloudBrUtils.MESSAGE_PATH, mMessageFolderPath);
                bundle.putString(CloudBrUtils.FAVORITE_PATH, mFavoriteFolderPath);
            }

            msg.setData(bundle);
            if (mHandler != null) {
                mHandler.sendMessage(msg);
            }

            if (mWakeLock != null) {
                releaseWakeLock();
                Log.d(CLASS_TAG, "onDestroy(): call releseWakeLock()");
            }
        }
    }

    protected synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RestoreService");
            mWakeLock.setReferenceCounted(false);
            Log.d(CLASS_TAG, "createWakeLock");
        }
    }

    protected void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        mWakeLock.acquire();
        Log.d(CLASS_TAG, "acquireWakeLock");
    }

    protected void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
            Log.d(CLASS_TAG, "releaseWakeLock");
        }
        Log.d(CLASS_TAG, "releaseLock");
    }
}
