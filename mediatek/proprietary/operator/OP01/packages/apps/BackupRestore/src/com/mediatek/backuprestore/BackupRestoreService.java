/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.backuprestore;

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

import com.mediatek.backuprestore.ResultDialog.ResultEntity;
import com.mediatek.backuprestore.modules.Composer;
import com.mediatek.backuprestore.utils.BackupRestoreNotification;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.Constants.MessageID;
import com.mediatek.backuprestore.utils.FileUtils;
import com.mediatek.backuprestore.utils.Constants.State;
import com.mediatek.backuprestore.utils.ModuleType;
import com.mediatek.backuprestore.utils.MyLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BackupRestoreService extends Service implements ProgressReporter {
    protected static final String CLASS_TAG = MyLogger.LOG_TAG + "/BackupRestoreService";
    private static final String TAG = "CMCCPerformanceTest";
    private BackupRestoreBinder mBinder = new BackupRestoreBinder();
    protected int mState;
    protected ArrayList<ResultEntity> mResultList;
    private BackupRestoreProgress mCurrentProgress = new BackupRestoreProgress();
    private Handler mHandler;
    private OnErrListener mErrListener = null;
    protected BackupRestoreResultType mResultType;
    protected ArrayList<ResultEntity> mAppResultList;
    protected PowerManager.WakeLock mWakeLock;
    protected int mCurrentComposerCount = 0;
    HashMap<Integer, ArrayList<String>> mParasMap = new HashMap<Integer, ArrayList<String>>();

    // before BackupEngine
    // private String mRestoreFolder;
    protected String mZipFileName;
    protected boolean mIsMtkSms = false;

    public enum BackupRestoreResultType {
        Success, Fail, Error, Cancel
    }

    // private Context mContext;
    private ProgressReporter mProgressReporter;
    protected List<Composer> mComposerList;
    // private OnBackupDoneListner mBackupDoneListner;
    protected boolean mIsRunning = false;
    private boolean mIsPause = false;
    private boolean mIsCancel = false;
    private Object mLock = new Object();
    protected String mBackupRestoreFolder;
    protected ArrayList<Integer> mModuleList;
    // HashMap<Integer, ArrayList<String>> mParasMap = new HashMap<Integer,
    // ArrayList<String>>();
    protected String mCommandMode;
    private int mCommand;
    protected BackupRestoreThread mBackupRestoreThread;
    protected boolean mNeedAppData = true;
    protected boolean mOldData = false;

    protected interface OnErrListener {
        void onErr(final IOException e);
    }

    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mCurrentComposerCount = 0;
        MyLogger.logI(CLASS_TAG, "onBind and clearNotification");
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        mErrListener = null;
        stopForeground(true);
        super.onUnbind(intent);
        MyLogger.logI(CLASS_TAG, "onUnbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        moveToState(State.INIT);
        stopForeground(true);
        MyLogger.logI(CLASS_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        MyLogger.logI(CLASS_TAG, "onStartCommand");
        return START_STICKY_COMPATIBILITY;
    }

    public void onRebind(Intent intent) {
        super.onRebind(intent);
        MyLogger.logI(CLASS_TAG, "onRebind");
    }

    public void onDestroy() {
        mErrListener = null;
        stopForeground(true);
        super.onDestroy();
        MyLogger.logI(CLASS_TAG, "onDestroy");
        mCurrentComposerCount = 0;
        if (isRunning()) {
            cancelComposer();
        }
    }

    public void moveToState(int state) {
        synchronized (this) {
            mState = state;
        }
    }

    public static class BackupRestoreProgress {
        Composer mComposer;
        int mType;
        int mMax;
        int mCurNum;
    }

    public class BackupRestoreBinder extends Binder {
        public int getState() {
            synchronized (this) {
                return mState;
            }
        }

        /*
         * //set mode backup or restore public void setBackupRestoreMode(String
         * commandMode) { mCommandMode = commandMode; }
         */

        public void updateMaxPercent(ArrayList<?> list) {
            BackupRestoreNotification.getInstance(BackupRestoreService.this).setMaxPercent(
                    list.size());
        }

        public void setItemParam(int itemType, ArrayList<String> paraList) {
            mParasMap.put(itemType, paraList);
        }

        public ArrayList<String> getItemParam(int itemType) {
            return mParasMap.get(itemType);
        }

        public void pauseBackupRestore() {
            if (mState == State.INIT) {
                return;
            }
            moveToState(State.PAUSE);
            pause();
            MyLogger.logD(CLASS_TAG, "pauseBackup");
        }

        public void cancelBackupRestore() {
            moveToState(State.CANCELLING);
            cancelComposer();
            if (mWakeLock != null) {
                releaseWakeLock();
                MyLogger.logD(CLASS_TAG, "cancelBackupRestore: call releseWakeLock()");
            }
            stopForeground(true);
            mCurrentComposerCount = 0;
            MyLogger.logD(CLASS_TAG,
                    "cancelBackupRestore and stopForeground and mCurrentComposerCount = 0");
        }

        public void reset() {
            MyLogger.logD(CLASS_TAG, "reset()");
            moveToState(State.INIT);
            if (mResultList != null) {
                mResultList.clear();
                mResultList = null;
            }
            if (mAppResultList != null) {
                mAppResultList.clear();
                mAppResultList = null;
            }
            if (mParasMap != null) {
                mParasMap.clear();
            }
            // add from Engine
            if (mComposerList != null) {
                mComposerList.clear();
                mComposerList = null;
            }
            mIsPause = false;
            mIsCancel = false;
        }

        public BackupRestoreProgress getCurrentProgress() {
            return mCurrentProgress;
        }

        public ArrayList<ResultEntity> getResult() {
            return mResultList;
        }

        public BackupRestoreResultType getResultType() {
            return mResultType;
        }

        public ArrayList<ResultEntity> getAppResult() {
            return mAppResultList;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        public void setOnErrListener(OnErrListener listener) {
            mErrListener = listener;
        }

        public void setOldData(boolean mOldData) {
            // TODO Auto-generated method stub

        }
    }

    @Override
    public void onStart(Composer composer) {
        mCurrentProgress.mComposer = composer;
        mCurrentProgress.mType = composer.getModuleType();
        mCurrentProgress.mMax = composer.getCount();
        mCurrentProgress.mCurNum = 0;

        Message msg = new Message();
        msg.what = MessageID.COMPOSER_CHANGED;
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MESSAGE_MAX_PROGRESS, composer.getCount());
        bundle.putString(Constants.MESSAGE_CONTENT,
                ModuleType.getModuleStringFromType(this, composer.getModuleType()));
        msg.setData(bundle);
        MyLogger.logD(CLASS_TAG,
                "onStart mHandler.sendMessage(msg) mHandler != null" + composer.getModuleType());
        if (mHandler != null) {
            MyLogger.logD(CLASS_TAG, "onStart mHandler.sendMessage(msg) mHandler != null");
            mHandler.sendMessage(msg);
        }

        if (composer.getModuleType() == ModuleType.TYPE_APP) {
            mCurrentComposerCount = 0;
        }

        if (mCurrentProgress.mMax != 0 && mCommandMode.equals(Constants.BACKUP)) {
            BackupRestoreNotification.getInstance(BackupRestoreService.this)
                    .initBackupNotification(composer.getModuleType(), mCurrentComposerCount);
            startForeground(Constants.STARTFORGROUND,
                    BackupRestoreNotification.getInstance(BackupRestoreService.this)
                            .getNotification());
        } else if (mCurrentProgress.mMax != 0 && mCommandMode.equals(Constants.RESTORE)) {
            BackupRestoreNotification.getInstance(BackupRestoreService.this)
                    .initRestoreNotification(composer.getModuleType(), mCurrentComposerCount,
                            mOldData);
            startForeground(Constants.STARTFORGROUND,
                    BackupRestoreNotification.getInstance(BackupRestoreService.this)
                            .getNotification());
        }
        MyLogger.logD(CLASS_TAG, "onStart");
    }

    @Override
    public void onOneFinished(Composer composer, boolean result) {
        Message msg = new Message();
        msg.what = MessageID.PROGRESS_CHANGED;
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MESSAGE_MAX_PROGRESS, composer.getCount());

        mCurrentProgress.mCurNum++;
        if (composer.getModuleType() == ModuleType.TYPE_APP && mBackupRestoreFolder != null) {
            bundle.putBoolean(Constants.MESSAGE_IS_UPDATA_MSG, true);
            if (mAppResultList == null) {
                mAppResultList = new ArrayList<ResultEntity>();
            }
            int type = result ? ResultEntity.SUCCESS : ResultEntity.FAIL;
            ResultEntity entity = new ResultEntity(ModuleType.TYPE_APP, type);
            MyLogger.logD(
                    CLASS_TAG,
                    "onOneFinished entity = " + entity + "; mParasMap = " + mParasMap
                            + "; mParasMap.get(ModuleType.TYPE_APP) = "
                            + mParasMap.get(ModuleType.TYPE_APP));
            if (mParasMap != null
                    && mParasMap.get(ModuleType.TYPE_APP) != null
                    && mParasMap.get(ModuleType.TYPE_APP).get(
                            mCurrentProgress.mCurNum - 1) != null) {
                entity.setKey(mParasMap.get(ModuleType.TYPE_APP).get(
                        mCurrentProgress.mCurNum - 1));
                mAppResultList.add(entity);
            }
            BackupRestoreNotification.getInstance(BackupRestoreService.this).updateNotification(
                    composer.getModuleType(), mCurrentProgress.mCurNum);
            startForeground(Constants.STARTFORGROUND,
                    BackupRestoreNotification.getInstance(BackupRestoreService.this)
                            .getNotification());
        }

        bundle.putInt(Constants.MESSAGE_CURRENT_PROGRESS, mCurrentProgress.mCurNum);
        msg.setData(bundle);
        if (mHandler != null) {
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void onEnd(Composer composer, boolean result) {
        int resultType = ResultEntity.SUCCESS;
        mCurrentComposerCount++;
        if (mResultList == null) {
            mResultList = new ArrayList<ResultEntity>();
        }
        MyLogger.logD(CLASS_TAG, "onEnd mIsCancel = " + mIsCancel + " " + composer.isCancel());
        if (!result) {
            if (composer.getCount() == 0) {
                resultType = ResultEntity.NO_CONTENT;
            } else {
                resultType = ResultEntity.FAIL;
            }
        }
        MyLogger.logD(CLASS_TAG, "one Composer end: type = " + composer.getModuleType()
                + ", result = " + resultType);
        ResultEntity item = new ResultEntity(composer.getModuleType(), resultType);
        mResultList.add(item);
        if (composer.getModuleType() != ModuleType.TYPE_APP
                && BackupRestoreNotification.getInstance(BackupRestoreService.this) != null) {
            BackupRestoreNotification.getInstance(BackupRestoreService.this).updateNotification(
                    composer.getModuleType(), mCurrentComposerCount);
            if (BackupRestoreNotification.getInstance(BackupRestoreService.this).getNotification() != null) {
                startForeground(Constants.STARTFORGROUND,
                        BackupRestoreNotification.getInstance(BackupRestoreService.this)
                                .getNotification());
            }
        }
        MyLogger.logD(CLASS_TAG, "onEnd");
    }

    @Override
    public void onErr(IOException e) {
        MyLogger.logD(CLASS_TAG, "onErr " + e.getMessage());
        if(mErrListener != null) {
            mErrListener.onErr(e);
        }
    }

    protected void onFinishBackupRestore(Message msg) {

        if (mHandler != null) {
            mHandler.sendMessage(msg);
        }

        mCurrentComposerCount = 0;
        stopForeground(true);
        MyLogger.logD(CLASS_TAG, "onFinishBackupRestore and stopForeground");

        if (mWakeLock != null) {
            releaseWakeLock();
            MyLogger.logD(CLASS_TAG, "onFinishBackupRestore: call releseWakeLock()");
        }
    }

    protected synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RestoreService");
            mWakeLock.setReferenceCounted(false);
            MyLogger.logD(CLASS_TAG, "createWakeLock");
        }
    }

    protected void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        mWakeLock.acquire();
        MyLogger.logD(CLASS_TAG, "acquireWakeLock");
    }

    protected void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
            MyLogger.logD(CLASS_TAG, "releaseWakeLock");
        }
        MyLogger.logD(CLASS_TAG, "releaseLock");
    }

    class BackupRestoreThread extends Thread {
        BackupRestoreResultType result = null;
        private volatile boolean mPause = false;
        private volatile boolean mCancel = false;
        private volatile ArrayList<Composer> mTemComposerList = null;
        private volatile String mMode = null;
        private Service mService = null;

        public void setComposerList(List<Composer> composer) {
            mTemComposerList = new ArrayList<Composer>(composer);
        }

        public void setPause(boolean pause) {
            mPause = pause;
        }

        public void setCancel(boolean cancel) {
            mCancel = cancel;
        }

        public void setMode(String mode) {
            mMode = mode;
        }

        public void setService(Service service) {
            mService = service;
        }

        @Override
        public void run() {
            if (mMode != null && mMode.equals(Constants.BACKUP)) {
                result = BackupRestoreResultType.Fail;
            }

            Log.d(CLASS_TAG, "BackupRestoreThread begin... " + mCommandMode);
            for (Composer composer : mTemComposerList) {
                Log.d(CLASS_TAG, "BackupRestoreThread->composer:" + composer.getModuleType()
                        + " start...");
                if (!composer.isCancel()) {
                    if (mMode != null && mMode.equals(Constants.RESTORE)) {
                        composer.setCommand(mCommand);
                    }
                    composer.init();
                    composer.onStart();
                    Log.d(CLASS_TAG, "BackupRestoreThread-> composer:" + composer.getModuleType()
                            + " init finish");
                    while (!composer.isAfterLast() && !composer.isCancel()) {
                        if (mPause) {
                            synchronized (mLock) {
                                try {
                                    Log.d(CLASS_TAG, "BackupRestoreThread wait... " + mMode);
                                    mLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        composer.composeOneEntity();
                        Log.d(CLASS_TAG,
                                "BackupRestoreThread->composer:" + composer.getModuleType()
                                        + " compose one entiry");
                    }
                }

                try {
                    sleep(Constants.TIME_SLEEP_WHEN_COMPOSE_ONE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mMode != null && mMode.equals(Constants.BACKUP) && composer.onEnd()) {
                    result = BackupRestoreResultType.Success;
                } else {
                    composer.onEnd();

                }

                Log.d(CLASS_TAG, "BackupRestoreThread-> composer:  " + composer.getModuleType()
                        + mCommandMode + " finish");
            }

            Log.d(CLASS_TAG, "BackupRestoreThread run finish: " + mMode + " resule :" + result);
            mIsRunning = false;

            if (mCancel && mMode != null && mMode.equals(Constants.BACKUP)) {
                result = BackupRestoreResultType.Cancel;
                if (!mModuleList.contains(ModuleType.TYPE_APP)) {
                    FileUtils.deleteFileOrFolder(new File(mBackupRestoreFolder));
                }
            }

            if (mPause && mMode != null && mMode.equals(Constants.BACKUP)) {
                synchronized (mLock) {
                    try {
                        Log.d(CLASS_TAG, "BackupThread wait before end...");
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mMode != null && mMode.equals(Constants.BACKUP)) {
                ((BackupService) mService).onFinishBackup(result);
            } else {
                ((RestoreService) mService).onFinishRestore(true);
            }

        }
    }

    protected void addComposer(final Composer composer) {
        if (composer != null) {
            int type = composer.getModuleType();
            ArrayList<String> params = mParasMap.get(type);
            if (params != null) {
                composer.setParams(params);
            }
            if (mCommandMode != null && mCommandMode.equals(Constants.BACKUP)) {
                composer.setNeedAppData(mNeedAppData);
            }
            composer.setReporter(this);
            composer.setParentFolderPath(mBackupRestoreFolder);
            mComposerList.add(composer);
        }
    }

    public final void notifyThread() {
        if (mIsPause) {
            synchronized (mLock) {
                mIsPause = false;
                if (mBackupRestoreThread != null) {
                    mBackupRestoreThread.setPause(mIsPause);
                }
                mLock.notify();
            }
        }
    }

    public final void pause() {
        mIsPause = true;
        if (mBackupRestoreThread != null) {
            mBackupRestoreThread.setPause(mIsPause);
        }
    }

    public final boolean isPaused() {
        return mIsPause;
    }

    public final boolean isRunning() {
        return mIsRunning;
    }

    public final void cancelComposer() {
        if (mComposerList != null && mComposerList.size() > 0) {
            for (Composer composer : mComposerList) {
                composer.setCancel(true);
            }
            mIsCancel = true;
            if (mBackupRestoreThread != null) {
                mBackupRestoreThread.setCancel(mIsCancel);
            }
            notifyThread();
        }
    }

    public void setCommand(int command) {
        mCommand = command;
    }

    public void addOldComposer(Composer composer) {
        if (composer != null) {
            composer.setReporter(this);
            composer.setZipFileName(mZipFileName);
            mComposerList.add(composer);
        }
    }

}
