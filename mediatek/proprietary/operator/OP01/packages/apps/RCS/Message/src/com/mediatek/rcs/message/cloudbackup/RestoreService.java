package com.mediatek.rcs.message.cloudbackup;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.message.cloudbackup.BackupService.BackupThread;
import com.mediatek.rcs.message.cloudbackup.modules.FavMsgBackup;
import com.mediatek.rcs.message.cloudbackup.modules.FavMsgRestore;
import com.mediatek.rcs.message.cloudbackup.modules.MessageBackupComposer;
import com.mediatek.rcs.message.cloudbackup.modules.MessageRestoreComposer;
import com.mediatek.rcs.message.cloudbackup.store.MessageStore;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class RestoreService extends Service {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "RestoreService";

    private RestoreBinder mBinder = new RestoreBinder();
    protected PowerManager.WakeLock mWakeLock;
    private String mRestoreDataFolder;
    private Handler mHandler;
    private RestoreThread mRestoreThread;
    private Context mContext;

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class RestoreBinder extends Binder {

        public int startRestore() {
            Log.d(CLASS_TAG, "RestoreBinder startRestore");
            createWakeLock();
            if (mWakeLock != null) {
                acquireWakeLock();
                Log.d(CLASS_TAG, "RestoreService : startRestore: call acquireWakeLock()");
            }
            mRestoreThread = new RestoreThread(mContext);
            mRestoreThread.setHandler(mHandler);
            mRestoreThread.setRestoreDataFolder(mRestoreDataFolder);
            mRestoreThread.start();
            return CloudBrUtils.ResultCode.OK;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        public void cancelRestore() {
            Log.d(CLASS_TAG, "RestoreService : cancelRestore");
            if (mWakeLock != null) {
                releaseWakeLock();
                Log.d(CLASS_TAG, "cancelBackupRestore: call releseWakeLock()");
            }
            if (mRestoreThread != null) {
                mRestoreThread.setCancel(true);
            }
        }

        public void setContext(Context context) {
            mContext = context;
        }

        /**
         * backupFolder -msgdataFolder -timestamp -favoriteFolder -timestamp
         * backupFolder is parent foder of msg data and fovorite data.
         *
         * @param backupFolder
         */
        public void setRestoreDataFolder(String backupFolder) {
            Log.d(CLASS_TAG, "restoreService setRestoreDataFolder = " + backupFolder);
            mRestoreDataFolder = backupFolder;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        stopForeground(true);
        Log.i(CLASS_TAG, "onCreate");
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

    protected synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RestoreService");
            mWakeLock.setReferenceCounted(false);
            Log.d(CLASS_TAG, "createWakeLock");
        }
    }

    class RestoreThread extends Thread {
        private boolean mCancel = false;
        private Context mContext;
        private String mResotreDataFolder;
        private MessageRestoreComposer mMessageRestoreComposer;
        private FavMsgRestore mFavMsgRestore;
        private FavMsgBackup mFavMsgBackup;
        private MessageBackupComposer mMessageBackupComposer;
        private MessageStore mMessageStore;
        private boolean mIsNeedRestoreOrigData = false;
        private String mMessageBackupFolderPath = FileUtils.ModulePath.RESTORE_BACKUP_FOLDER
                + MessageStore.RemoteFolderName.MSG_BACKUP;
        private String mFavoriteBackupFolderPath = FileUtils.ModulePath.RESTORE_BACKUP_FOLDER
                + MessageStore.RemoteFolderName.MSG_FAVORITE;

        RestoreThread(Context context) {
            mContext = context;
        }

        public void setCancel(boolean cancel) {
            Log.d(CLASS_TAG, "RestoreThread setCancel");
            mCancel = cancel;
            if (mFavMsgBackup != null) {
                mFavMsgBackup.setCancel(cancel);
            }
            if (mMessageRestoreComposer != null) {
                mMessageRestoreComposer.setCancel(cancel);
            }

            if (mFavMsgRestore != null) {
                mFavMsgRestore.setCancel(cancel);
            }
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        // download file in here.
        public int setRestoreDataFolder(String backupFolder) {
            mResotreDataFolder = backupFolder;
            return CloudBrUtils.ResultCode.OK;
        }

        @Override
        public void run() {
            int restoreResult = CloudBrUtils.ResultCode.OK;
            if (mCancel) {
                Log.d(CLASS_TAG, "BackupThread is be canceled. return");
                sendRestoreResult(CloudBrUtils.ResultCode.SERVICE_CANCELED);
                return;
            }

            restoreResult = prepareForRestore();
            if (restoreResult != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "backup begore restore error delete temp data , return");
                File tempFolder = new File(FileUtils.ModulePath.RESTORE_BACKUP_FOLDER);
                if (tempFolder != null && tempFolder.exists()) {
                    FileUtils.deleteFileOrFolder(tempFolder);
                }
                sendRestoreResult(CloudBrUtils.ResultCode.BACKUP_BEFOR_RESTORE_EXCEPTION);
            }
            Log.d(CLASS_TAG, "backup data befor restore end, begin delete db");

            mIsNeedRestoreOrigData = true;

            String favoriteDataFolder = mResotreDataFolder + File.separator
                    + MessageStore.RemoteFolderName.MSG_FAVORITE;
            String messageDataFolder = mResotreDataFolder + File.separator
                    + MessageStore.RemoteFolderName.MSG_BACKUP;

            Log.d(CLASS_TAG, "restoreThread  restore message begin");
            File messageFolder = new File(messageDataFolder);
            if (messageFolder != null && messageFolder.exists()) {
                mMessageRestoreComposer = new MessageRestoreComposer(mContext, messageFolder);
                restoreResult = mMessageRestoreComposer.restoreData();
                if (restoreResult != CloudBrUtils.ResultCode.OK) {
                    Log.e(CLASS_TAG,
                            "BackupThread, mMessageRestoreComposer.restoreData() error, return");
                    sendRestoreResult(restoreResult);
                    return;
                }
            } else {
                Log.d(CLASS_TAG, "messageFolder is not existes");
            }
            mMessageRestoreComposer = null;
            Log.d(CLASS_TAG, "restoreThread  restore message end");

            Log.d(CLASS_TAG, "restoreThread  restore favorite begin");
            File favoriteFolder = new File(favoriteDataFolder);
            if (favoriteFolder != null && favoriteFolder.exists()) {
                mFavMsgRestore = new FavMsgRestore(mContext, favoriteFolder);
                restoreResult = mFavMsgRestore.restoreData(favoriteFolder);
                if (restoreResult != CloudBrUtils.ResultCode.OK) {
                    Log.e(CLASS_TAG,
                            "BackupThread, mFavMsgRestore.restoreData(favoriteFolder) error, return");
                    sendRestoreResult(restoreResult);
                    return;
                }
            } else {
                Log.d(CLASS_TAG, "favoriteFolder is not existes");
            }
            mFavMsgRestore = null;
            Log.d(CLASS_TAG, "restoreThread, restore favorite end");
            mIsNeedRestoreOrigData = false;
            sendRestoreResult(CloudBrUtils.ResultCode.OK);

            Log.d(CLASS_TAG, "RestoreThread backup end, return");
        }

        private int prepareForRestore() {
            Log.d(CLASS_TAG, "RestoreThread, prepareForRestore");

            String messageFolderPath = mMessageBackupFolderPath;
            Log.d(CLASS_TAG, "messageFolderPath = " + messageFolderPath);
            mMessageBackupComposer = new MessageBackupComposer(mContext, messageFolderPath);
            int backupResult = mMessageBackupComposer.backupData();

            if (backupResult != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "BackupThread, mMessageBackupComposer.backupData error, return");
                sendRestoreResult(backupResult);
                return backupResult;
            }
            mMessageBackupComposer = null;

            if (mCancel) {
                Log.d(CLASS_TAG, "BackupThread is be canceled. return");
                backupResult = CloudBrUtils.ResultCode.SERVICE_CANCELED;
                sendRestoreResult(backupResult);
            }

            String favoriteFolderPath = mFavoriteBackupFolderPath;
            Log.d(CLASS_TAG, "favoriteFolderPath = " + favoriteFolderPath);
            mFavMsgBackup = new FavMsgBackup(mContext, favoriteFolderPath);
            backupResult = mFavMsgBackup.backupData();
            if (backupResult != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "BackupThread, mFavMsgBackup.backupData error, return");
                sendRestoreResult(backupResult);
                return backupResult;
            }
            mFavMsgBackup = null;
            Log.d(CLASS_TAG, "BackupThread, backup favorite msg end,result ok");

            if (mCancel) {
                Log.d(CLASS_TAG, "BackupThread is be canceled. return");
                backupResult = CloudBrUtils.ResultCode.SERVICE_CANCELED;
                sendRestoreResult(backupResult);
            }

            return backupResult;
        }

        private void sendRestoreResult(int restoreResult) {
            if (restoreResult != CloudBrUtils.ResultCode.OK && mIsNeedRestoreOrigData) {
                Log.e(CLASS_TAG, "restore error, begin restore original data");
                restoreOrignDd();
            }

            File backupTempFile = new File(FileUtils.ModulePath.RESTORE_BACKUP_FOLDER);
            if (backupTempFile != null && backupTempFile.exists()) {
                Log.d(CLASS_TAG, "sendRestoreResult delete temp folder");
                FileUtils.deleteFileOrFolder(backupTempFile);
            }

            File backupFile = new File(mResotreDataFolder);
            if (backupFile != null && backupFile.exists()) {
                Log.d(CLASS_TAG, "sendRestoreResult delete backupFile folder = "
            + backupFile.getAbsolutePath());
                FileUtils.deleteFileOrFolder(backupFile);
            }
            // send restore result to activity.
            Message msg = new Message();
            msg.what = CloudBrUtils.MessageID.RESTORE_END;
            Bundle bundle = new Bundle();
            bundle.putInt(CloudBrUtils.RESTORE_RESULT, restoreResult);
            msg.setData(bundle);
            if (mHandler != null) {
                mHandler.sendMessage(msg);
            }

            if (mWakeLock != null) {
                releaseWakeLock();
                Log.d(CLASS_TAG, "onDestroy(): call releseWakeLock()");
            }
        }

        private void restoreOrignDd() {
            Log.d(CLASS_TAG, "restoreThread  restore message begin");
            int restoreResult = CloudBrUtils.ResultCode.OK;
            File messageFolder = new File(mMessageBackupFolderPath);
            if (messageFolder != null && messageFolder.exists()) {
                mMessageRestoreComposer = new MessageRestoreComposer(mContext, messageFolder);
                restoreResult = mMessageRestoreComposer.restoreData();
                if (restoreResult != CloudBrUtils.ResultCode.OK) {
                    Log.e(CLASS_TAG, "messageFolder, mMessageRestoreComposer.restoreData() error");
                }
            }

            Log.d(CLASS_TAG, "restoreThread  restore favorite begin");
            File favoriteFolder = new File(mFavoriteBackupFolderPath);
            if (favoriteFolder != null && favoriteFolder.exists()) {
                mFavMsgRestore = new FavMsgRestore(mContext, favoriteFolder);
                restoreResult = mFavMsgRestore.restoreData(favoriteFolder);
                if (restoreResult != CloudBrUtils.ResultCode.OK) {
                    Log.e(CLASS_TAG,
                            "BackupThread, mFavMsgRestore.restoreData(favoriteFolder) error, return");
                }
            }
            mFavMsgRestore = null;
            Log.d(CLASS_TAG, "restoreThread, restore favorite end");
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
