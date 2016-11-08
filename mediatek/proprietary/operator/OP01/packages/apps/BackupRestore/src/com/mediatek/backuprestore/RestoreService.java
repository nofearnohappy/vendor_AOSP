package com.mediatek.backuprestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreBinder;
import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreThread;
import com.mediatek.backuprestore.modules.AppRestoreComposer;
import com.mediatek.backuprestore.modules.CalendarRestoreComposer;
import com.mediatek.backuprestore.modules.Composer;
import com.mediatek.backuprestore.modules.ContactRestoreComposer;
import com.mediatek.backuprestore.modules.MessageRestoreComposer;
import com.mediatek.backuprestore.modules.MmsRestoreComposer;
import com.mediatek.backuprestore.modules.MusicRestoreComposer;
import com.mediatek.backuprestore.modules.NoteBookRestoreComposer;
import com.mediatek.backuprestore.modules.OldAppRestoreComposer;
import com.mediatek.backuprestore.modules.OldCalendarRestoreComposer;
import com.mediatek.backuprestore.modules.OldContactRestoreComposer;
import com.mediatek.backuprestore.modules.OldMmsRestoreComposer;
import com.mediatek.backuprestore.modules.OldMusicRestoreComposer;
import com.mediatek.backuprestore.modules.OldPictureRestoreComposer;
import com.mediatek.backuprestore.modules.OldSmsRestoreComposer;
import com.mediatek.backuprestore.modules.PictureRestoreComposer;
import com.mediatek.backuprestore.modules.SmsRestoreComposer;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.Constants.MessageID;
import com.mediatek.backuprestore.utils.ModuleType;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.SDCardUtils;
import com.mediatek.backuprestore.utils.Constants.State;

public class RestoreService extends BackupRestoreService implements ProgressReporter {

    private static final String TAG = "CMCCPerformanceTest";
    private RestoreServiceBinder mBinder = new RestoreServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return mBinder;
    }

    /*
     * public boolean onUnbind(Intent intent) { super.onUnbind(intent); return
     * true; }
     *
     * @Override public void onCreate() { super.onCreate(); }
     *
     * public int onStartCommand(Intent intent, int flags, int startId) { return
     * super.onStartCommand(intent, flags, startId); }
     *
     * public void onRebind(Intent intent) { super.onRebind(intent); }
     *
     * public void onDestroy() { super.onDestroy(); }
     */

    private boolean setupRestoreComposer(List<Integer> list) {
        boolean bSuccess = true;
        for (int type : list) {
            switch (type) {
            case ModuleType.TYPE_CONTACT:
                addComposer(new ContactRestoreComposer(this));
                break;

            case ModuleType.TYPE_MESSAGE:
                addComposer(new MessageRestoreComposer(this));
                break;

            case ModuleType.TYPE_SMS:
                addComposer(new SmsRestoreComposer(this));
                break;

            case ModuleType.TYPE_MMS:
                addComposer(new MmsRestoreComposer(this));
                break;

            case ModuleType.TYPE_PICTURE:
                addComposer(new PictureRestoreComposer(this));
                break;

            case ModuleType.TYPE_CALENDAR:
                addComposer(new CalendarRestoreComposer(this));
                break;

            case ModuleType.TYPE_APP:
                addComposer(new AppRestoreComposer(this));
                break;

            case ModuleType.TYPE_MUSIC:
                addComposer(new MusicRestoreComposer(this));
                break;

            case ModuleType.TYPE_NOTEBOOK:
                addComposer(new NoteBookRestoreComposer(this));
                break;

            default:
                bSuccess = false;
                break;
            }
        }

        return bSuccess;
    }

    class RestoreServiceBinder extends BackupRestoreBinder {

        public void setRestoreItemParam(int itemType, ArrayList<String> paraList) {
            super.setItemParam(itemType, paraList);
            super.updateMaxPercent(paraList);
        }

        public void setRestoreModelList(ArrayList<Integer> list, Boolean isMtkSms) {
            if (mState == State.RUNNING) {
                MyLogger.logD(CLASS_TAG, " startRestore RestoreService : is running return.");
                return;
            }
            reset();
            mModuleList = list;
            mIsMtkSms = isMtkSms;
            mComposerList = new ArrayList<Composer>();
            super.updateMaxPercent(list);
        }

        public boolean startRestore(String fileName, int command) {
            if (mState == State.RUNNING) {
                MyLogger.logD(CLASS_TAG, " startRestore RestoreService : is running return.");
                return true;
            }
            moveToState(State.RUNNING);
            createWakeLock();
            mCommandMode = Constants.RESTORE;
            if (mWakeLock != null) {
                acquireWakeLock();
                MyLogger.logD(CLASS_TAG, "RestoreService : startRestore: call acquireWakeLock()");
            }
            setCommand(command);
            executeRestoreCommand(fileName);
            return true;
        }

        public void pauseRestore() {
            super.pauseBackupRestore();
            MyLogger.logD(CLASS_TAG, "RestoreService : pauseRestore");
        }

        /*
         * public void continueRestore() { if (mState == State.INIT) { return; }
         * moveToState(State.RUNNING); continueRestoreComposer();
         * MyLogger.logD(CLASS_TAG, "continueRestore"); }
         */

        public void cancelRestore() {
            if (mState == State.INIT && mState == State.CANCELLING) {
                MyLogger.logD(CLASS_TAG, "BackupService :alreary cancelRestore");
                return;
            }
            super.cancelBackupRestore();
        }

        public void isOldData(boolean isOldData) {
            mOldData = isOldData;
        }
    }

    @Override
    public void onStart(Composer iComposer) {
        super.onStart(iComposer);
    }

    /*
     * @Override public void onOneFinished(Composer composer, boolean result) {
     * super.onOneFinished(composer,result); }
     *
     * @Override public void onEnd(Composer composerInfo, boolean result) {
     * super.onEnd(composerInfo,result); }
     *
     * @Override public void onErr(IOException e) { super.onErr(e); }
     */

    public void onFinishRestore(boolean bSuccess) {
        Log.i("@M_" + TAG,
                "[CMCC Performance test][BackupAndRestore][Contact_Restore] Restore end ["
                        + System.currentTimeMillis() + "]");
        moveToState(State.FINISH);
        Message msg = new Message();
        msg.what = MessageID.RESTORE_END;
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.MESSAGE_RESULT_TYPE, bSuccess);
        if (mAppResultList != null) {
            bundle.putParcelableArrayList(Constants.MESSAGE_RESULT_RECORD, mAppResultList);
        } else {
            bundle.putParcelableArrayList(Constants.MESSAGE_RESULT_RECORD, mResultList);
        }
        msg.setData(bundle);
        super.onFinishBackupRestore(msg);
    }

    public void executeRestoreCommand(final String path) {
        mZipFileName = null;
        boolean isOldBackup = false;
        File file = new File(path);
        if (file != null) {
            if (file.getParent().equals(
                    SDCardUtils.getSdCardMountPath(this) + File.separator + ".backup")) {
                isOldBackup = true;
                MyLogger.logD(CLASS_TAG, "RestoreService : the path is old Data and isOldBackup = "
                        + isOldBackup);
            }
        }
        if (isOldBackup && mModuleList.size() > 0) {
            MyLogger.logD(CLASS_TAG, "RestoreService : startRestore path:" + path);
            // mZipFileName =
            // path.substring(path.lastIndexOf(File.separator)+1);
            mZipFileName = path;
            MyLogger.logD(CLASS_TAG, "RestoreService : mZipFileName" + mZipFileName);
            setupOldRestoreComposer(mModuleList);
        } else if (path != null && mModuleList.size() > 0) {
            mBackupRestoreFolder = path;
            setupRestoreComposer(mModuleList);
        }
        mIsRunning = true;
        mBackupRestoreThread = new BackupRestoreThread();
        mBackupRestoreThread.setComposerList(mComposerList);
        mBackupRestoreThread.setMode(mCommandMode);
        mBackupRestoreThread.setService(this);
        mBackupRestoreThread.start();
    }

    // To compatible with old data.
    private boolean setupOldRestoreComposer(List<Integer> list) {
        boolean bSuccess = true;
        for (int type : list) {
            switch (type) {
            case ModuleType.TYPE_CONTACT:
                addOldComposer(new OldContactRestoreComposer(this));
                break;

            case ModuleType.TYPE_MESSAGE:
                String old = "othersSms";
                MyLogger.logD(CLASS_TAG, "RestoreService : FileUtils.isMtkOldSmsData(mZipFileName)"
                        + mZipFileName);
                // boolean isMtkSms = FileUtils.isMtkOldSmsData(mZipFileName);
                if (mIsMtkSms) {
                    old = "Mtk";
                }
                addOldComposer(new MessageRestoreComposer(this, old));
                break;

            case ModuleType.TYPE_SMS:
                addOldComposer(new OldSmsRestoreComposer(this));
                break;

            case ModuleType.TYPE_MMS:
                addOldComposer(new OldMmsRestoreComposer(this));
                break;

            case ModuleType.TYPE_PICTURE:
                addOldComposer(new OldPictureRestoreComposer(this));
                break;

            case ModuleType.TYPE_CALENDAR:
                addOldComposer(new OldCalendarRestoreComposer(this));
                break;

            case ModuleType.TYPE_APP:
                addOldComposer(new OldAppRestoreComposer(this));
                break;

            case ModuleType.TYPE_MUSIC:
                addOldComposer(new OldMusicRestoreComposer(this));
                break;

            case ModuleType.TYPE_NOTEBOOK:
                addOldComposer(new NoteBookRestoreComposer(this));
                break;

            default:
                bSuccess = false;
                break;
            }
        }

        return bSuccess;
    }
}
