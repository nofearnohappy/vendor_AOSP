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

package com.mediatek.backuprestore.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.mediatek.backuprestore.R;
import com.mediatek.backuprestore.StorageSettingsActivity;
import com.mediatek.backuprestore.utils.Constants.MessageID;

public class BackupFileScanner {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/BackupFileScanner";
    private Handler mHandler;
    private Object mObject = new Object();
    private ScanThread mScanThread;
    private Context mContext;
    private boolean mScanAll;
    // private HashMap<String,List<?>> mDataResult = new HashMap<String,
    // List<?>>();
    private List<Object> mDataResult = new ArrayList<Object>();
    BackupAppFilePreview appData = new BackupAppFilePreview();

    public BackupFileScanner(Context context, Handler handler, boolean isFullScan) {
        mHandler = handler;
        mContext = context;
        if (mHandler == null) {
            MyLogger.logE(CLASS_TAG, "constuctor maybe failed!cause mHandler is null");
        }
        mScanAll = isFullScan;
    }

    public void setHandler(Handler handler) {
        // synchronized (mObject) {
        mHandler = handler;
        // }
    }

    public void setScanAll(boolean scanAll) {
        mScanAll = scanAll;
    }

    public boolean isScanning() {
        return mScanThread != null;
    }

    public void startScan() {
        mScanThread = new ScanThread(mContext);
        mScanThread.setScanMode(mScanAll);
        mScanThread.start();
    }

    public void quitScan() {
        // synchronized (mObject) {
        if (mScanThread != null) {
            mScanThread.cancel();
            mScanThread = null;
            MyLogger.logV(CLASS_TAG, "quitScan");
        }
        // }
    }

    public void setType(boolean type) {
        mScanAll = type;
    }

    private class ScanThread extends Thread {
        boolean mIsCanceled = false;
        List<String> mListPath = new ArrayList<String>();
        Context mContext = null;
        String mType = null;
        boolean mIsFullScan = false;

        public ScanThread(Context context) {
            mContext = context;
        }

        public void setScanMode(boolean mScanAll) {
            mIsFullScan = mScanAll;
        }

        public void cancel() {
            mIsCanceled = true;
        }

        public void setType(String type) {
            mType = type;
        }

        private String filterFile(File file, int index) {
            if (file == null) {
                return null;
            }
            if (index > 23) {
                MyLogger.logV(CLASS_TAG,
                        "scanner warning , the folder is more than 20 !!");
                return "WARNNING";
            }

            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File currentFile : files) {
                    if (mIsCanceled) {
                        break;
                    }
                    if (currentFile.exists() && currentFile.isDirectory()) {
                        String result = filterFile(currentFile, index + 1);
                        if (result != null
                                && result.length() > currentFile.getAbsolutePath()
                                        .length()) {
                            MyLogger.logV(CLASS_TAG, "warning : return result = "
                                    + result);
                            return result;
                        }
                    } else if (currentFile.exists()
                            && currentFile.isFile()
                            && checkEndsWithInStringArray(currentFile.getAbsolutePath(),
                                    (mContext.getResources()
                                            .getStringArray(R.array.apk_ending)))) {
                        /*
                         * MyLogger.logV(CLASS_TAG,
                         * "APP 1 : file getAbsolutePath = "+
                         * currentFile.getAbsolutePath());
                         * MyLogger.logV(CLASS_TAG, "APP 3 : file name  = "+
                         * currentFile.getName());
                         */
                        synchronized (mObject) {
                            if (checkDataForAdd(currentFile.getAbsolutePath(), "APP")) {
                                appData.setDataType("APP");
                                appData.setFilePath(currentFile.getAbsolutePath());
                                appData.putData(currentFile.getAbsolutePath());

                                if (!mDataResult.contains(appData)) {
                                    mDataResult.add(appData);
                                }
                                handleMessageData(mDataResult, MessageID.SCANNER_ONE_ITEM);
                            }
                        }
                    } else if (currentFile.exists()
                            && currentFile.isFile()
                            && checkEndsWithInStringArray(currentFile.getAbsolutePath(),
                                    (mContext.getResources()
                                            .getStringArray(R.array.data_ending)))) {
                        /*
                         * MyLogger.logV(CLASS_TAG,
                         * "DATA 1 : file getAbsolutePath() = "+
                         * currentFile.getAbsolutePath());
                         * MyLogger.logV(CLASS_TAG, "DATA 3 : file name  = "+
                         * currentFile.getName());
                         */
                        synchronized (mObject) {
                            if (checkDataForAdd(currentFile.getParentFile()
                                    .getParentFile().getAbsolutePath(), "PERSON_DATA")) {
                                BackupFilePreview mPersonData = new BackupFilePreview(
                                        currentFile.getParentFile().getParentFile());
                                mPersonData.setDataType("PERSON_DATA");
                                mDataResult.add(mPersonData);
                                handleMessageData(mDataResult, MessageID.SCANNER_ONE_ITEM);
                            }
                        }
                        return currentFile.getParentFile().getParentFile()
                                .getAbsolutePath();
                    } else if (currentFile.exists()
                            && currentFile.isFile()
                            && checkEndsWithInStringArray(currentFile.getAbsolutePath(),
                                    (mContext.getResources()
                                            .getStringArray(R.array.old_data_ending)))) {
                        /*
                         * MyLogger.logV(CLASS_TAG,
                         * "OLD_DATA 1 : file getAbsolutePath() = "+
                         * currentFile.getAbsolutePath());
                         * MyLogger.logV(CLASS_TAG,
                         * "OLD_DATA 3 : file name  = "+ currentFile.getName());
                         */
                        synchronized (mObject) {
                            if (checkDataForAdd(currentFile.getAbsolutePath(), "OLD_DATA")) {
                                OldBackupFilePreview backupFile = new OldBackupFilePreview(
                                        currentFile);
                                backupFile.setFilePath(currentFile.getAbsolutePath());
                                mDataResult.add(backupFile);
                                handleMessageData(mDataResult, MessageID.SCANNER_ONE_ITEM);
                            }
                        }
                    } else {
                        /*
                         * handleMessageData(mDataResult);
                         * MyLogger.logV(CLASS_TAG,
                         * "currentFile is no data path = " +
                         * currentFile.getAbsolutePath());
                         */
                    }
                }
            }

            if (mIsCanceled) {
                return null;
            } else {
                // MyLogger.logV(CLASS_TAG, "scanner Finish !");
                return "Finish";
            }
        }

        private void handleMessageData(List<Object> data, int result) {
            if (!mIsCanceled && mHandler != null) {
                MyLogger.logE(CLASS_TAG, "send message result = " + result);
                Message msg = null;
                if (data != null && data.size() > 0) {
                    MyLogger.logE(CLASS_TAG, "have data");
                    mHandler.removeMessages(result);
                    msg = mHandler.obtainMessage(result, data);
                } else {
                    MyLogger.logE(CLASS_TAG, "no data");
                    msg = mHandler.obtainMessage(result, null);
                }
                mHandler.sendMessage(msg);
            }
        }

        private boolean checkEndsWithInStringArray(String fileName, String[] fileEndings) {

            for (String aEnd : fileEndings) {
                if (fileName.matches(aEnd)) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkDataForAdd(String path, String type) {
            if (mDataResult != null && mDataResult.size() > 0) {
                for (int i = 0; i < mDataResult.size(); i++) {
                    Object obj = mDataResult.get(i);
                    if (obj instanceof BackupFilePreview && type.equals("PERSON_DATA")) {
                        BackupFilePreview backupFilePreview = (BackupFilePreview) obj;
                        if (backupFilePreview.getFile().getAbsolutePath().equals(path)) {
                            MyLogger.logD(CLASS_TAG,
                                    "checkDataForAdd : PERSON_DATA false path = " + path);
                            return false;
                        }
                    } else if (obj instanceof BackupAppFilePreview && type.equals("APP")) {
                        BackupAppFilePreview appData = (BackupAppFilePreview) obj;
                        if (appData.getAppData().contains(appData)) {
                            MyLogger.logD(CLASS_TAG,
                                    "checkDataForAdd : APP false path = " + path);
                            return false;
                        }
                    } else if (obj instanceof OldBackupFilePreview
                            && type.equals("OLD_DATA")) {
                        OldBackupFilePreview oldData = (OldBackupFilePreview) obj;
                        if (oldData.getFilePath().equals(path)) {
                            MyLogger.logD(CLASS_TAG,
                                    "checkDataForAdd : OLD_DATA false path = " + path);
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private String scanBackupFiles(String path) {
            if (path != null && !mIsCanceled) {
                String fileIndex[] = path.split(File.separator);
                String phonePath = SDCardUtils.getPhoneDataPath(mContext);
                if(phonePath != null && path.contains(phonePath)) {
                    String pathSplit[] = phonePath.split(File.separator);
                    MyLogger.logD(CLASS_TAG,
                            "scanBackuFile and splitpath length = "+pathSplit.length);
                    return filterFile(new File(path), fileIndex.length - (pathSplit.length - 1));
                }
                return filterFile(new File(path), fileIndex.length - 3);
            } else {
                return null;
            }
        }

        private String scanOldBackupFiles() {
            String path = SDCardUtils.getStoragePath(mContext);
            if (path != null) {
                int index = path.lastIndexOf(File.separator);
                path = path.substring(0, index + 1) + ".backup";
                MyLogger.logE(CLASS_TAG, "The old backup data path:" + path);
                return scanBackupFiles(path);
            } else {
                return null;
            }

        }

        @Override
        public void run() {
            // /M: 1 setup restore path
            settingsListPath();
            // /M: 2 from path to scanner file
            checkDataNeedUpdata();
            // /M: 3 scanner every path
            if (mIsCanceled) {
                return;
            }
            for (String mPath : mListPath) {
                MyLogger.logE(CLASS_TAG, "~~~ scanBackupFiles :" + mPath);
                scanBackupFiles(mPath);
            }
            handleMessageData(mDataResult, MessageID.SCANNER_FINISH);
            mScanThread = null;
        }

        private void checkDataNeedUpdata() {
            String sdcard = SDCardUtils.getSDCardDataPath(mContext);
            String phone = SDCardUtils.getPhoneStoragePath(mContext);
            if (mDataResult != null && mDataResult.size() > 0) {
                for (int i = mDataResult.size() - 1; i >= 0; i--) {
                    if (mIsCanceled) {
                        return;
                    }
                    Object obj = mDataResult.get(i);
                    if (obj instanceof BackupFilePreview) {
                        BackupFilePreview backupFilePreview = (BackupFilePreview) obj;
                        String path = backupFilePreview.getFile().getAbsolutePath();
                        File checkFile = new File(path);
                        MyLogger.logE(CLASS_TAG, " 0 checkDataNeedUpdata : path = "
                                + path);
                        if (sdcard == null && !path.startsWith(phone)) {
                            MyLogger.logE(
                                    CLASS_TAG,
                                    " 1 checkDataNeedUpdata : checkFile = "
                                            + checkFile.getAbsolutePath());
                            mDataResult.remove(i);
                        } else if (!checkFile.exists()) {
                            MyLogger.logE(
                                    CLASS_TAG,
                                    " 2 checkDataNeedUpdata : checkFile = "
                                            + checkFile.getAbsolutePath());
                            mDataResult.remove(i);
                        } else {
                            // /M : modify for the file is deleted and path is
                            // recreate,so setSize again.
                            backupFilePreview.setSize(checkFile);
                        }
                    } else if (obj instanceof BackupAppFilePreview) {
                        BackupAppFilePreview appData = (BackupAppFilePreview) obj;
                        appData.checkAppData();
                        if (appData.getAppDataSize() == 0) {
                            mDataResult.remove(i);
                        }

                    } else if (obj instanceof OldBackupFilePreview) {
                        OldBackupFilePreview oldData = (OldBackupFilePreview) obj;
                        MyLogger.logE(CLASS_TAG, " 1 checkDataNeedUpdata : oldData = "
                                + oldData.getFile().getAbsolutePath());
                        if (!oldData.getFile().exists()) {
                            mDataResult.remove(i);
                        }
                    }
                }
            }
            handleMessageData(mDataResult, MessageID.SCANNER_ONE_ITEM);
            MyLogger.logE(CLASS_TAG, "checkDataNeedUpdata : check finish!");
        }

        private void settingsListPath() {
            mListPath.clear();
            if (mIsFullScan) {
                if (SDCardUtils.getPathList(mContext) != null) {
                    mListPath = SDCardUtils.getPathList(mContext);
                }
            } else {
                String oldBackupPath = SDCardUtils.getSDCardDataPath(mContext);
                String sdcardPath = SDCardUtils.getSDCardDataPath(mContext);
                String phonePath = SDCardUtils.getPhoneDataPath(mContext);
                MyLogger.logE(CLASS_TAG, "settingsListPath : sdcardPath == " + sdcardPath);
                MyLogger.logE(CLASS_TAG, "settingsListPath : phonePath == " + phonePath);
                String customPath = StorageSettingsActivity.getCurrentPath(mContext);
                if (sdcardPath != null) {
                    mListPath.add(sdcardPath);
                }
                if (phonePath != null) {
                    mListPath.add(phonePath);
                }
                if (!mListPath.contains(customPath)) {
                    mListPath.add(customPath);
                }
                if (oldBackupPath != null) {
                    int index = oldBackupPath.lastIndexOf(File.separator);
                    oldBackupPath = oldBackupPath.substring(0, index + 1) + ".backup";
                    MyLogger.logE(CLASS_TAG, "The old backup data path:" + oldBackupPath);
                    mListPath.add(oldBackupPath);
                }

            }

        }

        private List<BackupFilePreview> generateBackupFileItems(File[] files) {
            if (files == null || mIsCanceled) {
                return null;
            }
            List<BackupFilePreview> list = new ArrayList<BackupFilePreview>();
            for (File file : files) {
                if (mIsCanceled) {
                    break;
                }
                BackupFilePreview backupFile = new BackupFilePreview(file);
                if (backupFile != null) {
                    list.add(backupFile);
                }
            }
            if (!mIsCanceled) {
                sort(list);
                return list;
            } else {
                return null;
            }
        }

        private List<OldBackupFilePreview> generateOldBackupFileItems(File[] files) {
            if (files == null || mIsCanceled) {
                MyLogger.logE(CLASS_TAG,
                        "generateOldBackupFileItems:There are no old backup data");
                return null;
            }
            List<OldBackupFilePreview> list = new ArrayList<OldBackupFilePreview>();
            for (File file : files) {
                if (mIsCanceled) {
                    break;
                }
                if (file.getAbsolutePath().endsWith(".zip")) {
                    MyLogger.logD(CLASS_TAG, "OldBackupFileItems:" + file.getName());
                    OldBackupFilePreview backupFile = new OldBackupFilePreview(file);
                    if (backupFile != null) {
                        list.add(backupFile);
                    }
                }
            }
            if (!mIsCanceled) {
                // sort(list);
                return list;
            } else {
                return null;
            }
        }

        private void sort(List<BackupFilePreview> list) {
            Collections.sort(list, new Comparator<BackupFilePreview>() {
                public int compare(BackupFilePreview object1, BackupFilePreview object2) {
                    String dateLeft = object1.getBackupTime();
                    String dateRight = object2.getBackupTime();
                    if (dateLeft != null && dateRight != null) {
                        return dateRight.compareTo(dateLeft);
                    } else {
                        return 0;
                    }
                }
            });
        }
    }

}
