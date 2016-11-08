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

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreBinder;
import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreResultType;
import com.mediatek.backuprestore.BackupRestoreService.BackupRestoreThread;
import com.mediatek.backuprestore.ResultDialog.ResultEntity;
import com.mediatek.backuprestore.modules.AppBackupComposer;
import com.mediatek.backuprestore.modules.CalendarBackupComposer;
import com.mediatek.backuprestore.modules.Composer;
import com.mediatek.backuprestore.modules.ContactBackupComposer;
import com.mediatek.backuprestore.modules.MessageBackupComposer;
import com.mediatek.backuprestore.modules.MmsBackupComposer;
import com.mediatek.backuprestore.modules.MusicBackupComposer;
import com.mediatek.backuprestore.modules.NoteBookBackupComposer;
import com.mediatek.backuprestore.modules.PictureBackupComposer;
import com.mediatek.backuprestore.modules.SmsBackupComposer;
import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.ModuleType;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.Constants.MessageID;
import com.mediatek.backuprestore.utils.Constants.State;

public class BackupService extends BackupRestoreService implements ProgressReporter {

    private static final String TAG = "CMCCPerformanceTest";
    private BackupServiceBinder mBinder = new BackupServiceBinder();

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

    private boolean executeBackupCommand(final String folderName) {
        boolean startSuccess = true;
        mBackupRestoreFolder = folderName;
        Log.d(CLASS_TAG, "BackupService : executeBackupCommand():" + folderName);

        if (setupBackupComposer(mModuleList)) {
            mIsRunning = true;
            mBackupRestoreThread = new BackupRestoreThread();
            mBackupRestoreThread.setComposerList(mComposerList);
            mBackupRestoreThread.setMode(mCommandMode);
            mBackupRestoreThread.setService(this);
            mBackupRestoreThread.start();
        } else {
            startSuccess = false;
        }
        return startSuccess;
    }

    private boolean setupBackupComposer(final ArrayList<Integer> list) {
        Log.d(CLASS_TAG, "BackupService : setupComposer begin...");

        boolean result = true;
        for (int type : list) {
            switch (type) {
            case ModuleType.TYPE_CONTACT:
                addComposer(new ContactBackupComposer(this));
                break;

            case ModuleType.TYPE_CALENDAR:
                addComposer(new CalendarBackupComposer(this));
                break;

            case ModuleType.TYPE_SMS:
                addComposer(new SmsBackupComposer(this));
                break;

            case ModuleType.TYPE_MMS:
                addComposer(new MmsBackupComposer(this));
                break;

            case ModuleType.TYPE_MESSAGE:
                addComposer(new MessageBackupComposer(this));
                break;

            case ModuleType.TYPE_APP:
                addComposer(new AppBackupComposer(this));
                break;

            case ModuleType.TYPE_PICTURE:
                addComposer(new PictureBackupComposer(this));
                break;

            case ModuleType.TYPE_MUSIC:
                addComposer(new MusicBackupComposer(this));
                break;

            case ModuleType.TYPE_NOTEBOOK:
                addComposer(new NoteBookBackupComposer(this));
                break;

            default:
                result = false;
                break;
            }
        }

        Log.d(CLASS_TAG, "BackupService : setupComposer finish");

        return result;
    }

    class BackupServiceBinder extends BackupRestoreBinder {

        public void setBackupAppData(boolean isNeedAppData) {
            mNeedAppData = isNeedAppData;
        }

        public void setBackupModelList(ArrayList<Integer> list) {
            if (mState == State.RUNNING) {
                MyLogger.logD(CLASS_TAG, " startBackup BackupService : is running return.");
                return;
            }
            reset();
            mComposerList = new ArrayList<Composer>();
            mModuleList = list;
            if (!list.contains(ModuleType.TYPE_APP)) {
                super.updateMaxPercent(list);
            }
        }

        public void setBackupItemParam(int itemType, ArrayList<String> paraList) {
            super.setItemParam(itemType, paraList);
            if (itemType == ModuleType.TYPE_APP) {
                super.updateMaxPercent(paraList);
            }
        }

        public boolean startBackup(String folderName) {
            if (mState == State.RUNNING) {
                MyLogger.logD(CLASS_TAG, " startBackup BackupService : is running return.");
                return true;
            }
            moveToState(State.RUNNING);
            createWakeLock();
            mCommandMode = Constants.BACKUP;
            if (mWakeLock != null) {
                acquireWakeLock();
                MyLogger.logD(CLASS_TAG, "BackupService : startBackup: call acquireWakeLock()");
            }
            boolean ret = false;
            ret = executeBackupCommand(folderName);
            if (!ret) {
                moveToState(State.ERR_HAPPEN);
            }
            MyLogger.logD(CLASS_TAG, "BackupService : startBackup: " + ret);
            return ret;
        }

        public void pauseBackup() {
            super.pauseBackupRestore();
            MyLogger.logD(CLASS_TAG, "BackupService : pauseBackup");
        }

        public void cancelBackup() {
            if (mState == State.INIT && mState == State.CANCELLING) {
                MyLogger.logD(CLASS_TAG, "BackupService :alreary cancelBackup");
                return;
            }
            MyLogger.logD(CLASS_TAG, "BackupService : cancelBackup");
            super.cancelBackupRestore();
        }

        public void continueBackup() {
            mState = State.RUNNING;
            notifyThread();
            MyLogger.logD(CLASS_TAG, "BackupService : continueBackup");
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

    public void onFinishBackup(BackupRestoreResultType result) {
        MyLogger.logD(CLASS_TAG, "onFinishBackup result = " + result);
        Log.i("@M_" + TAG, "[CMCC Performance test][BackupAndRestore][Contact_Backup] Backup end ["
                + System.currentTimeMillis() + "]");
        mResultType = result;
        if (mState == State.CANCELLING) {
            MyLogger.logD(CLASS_TAG, "onFinishBackup mState == State.CANCELLING");
            result = BackupRestoreResultType.Cancel;
        }
        if (mResultList != null && result != BackupRestoreResultType.Success
                && result != BackupRestoreResultType.Cancel) {
            for (ResultEntity item : mResultList) {
                if (item.getResult() == ResultEntity.SUCCESS) {
                    item.setResult(ResultEntity.FAIL);
                }
            }
        }
        moveToState(State.FINISH);
        Message msg = new Message();
        msg.what = MessageID.BACKUP_END;
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.MESSAGE_RESULT_TYPE, result);
        if (mAppResultList != null && mAppResultList.size() > 0) {
            bundle.putParcelableArrayList(Constants.MESSAGE_RESULT_RECORD, mAppResultList);
        } else {
            bundle.putParcelableArrayList(Constants.MESSAGE_RESULT_RECORD, mResultList);
        }
        msg.setData(bundle);
        super.onFinishBackupRestore(msg);
    }
}
