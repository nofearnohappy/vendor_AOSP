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

package com.mediatek.ftprecheck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.mediatek.ftprecheck.CheckItemBase.CheckResult;
import com.mediatek.storage.StorageManagerEx;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CheckResultActivity extends Activity {

    private static final String TAG = "CheckResultActivity";

    private static final int MSG_START_CHECK = 1;
    private static final int MSG_START_CHECK_DONE = 2;
    private static final int MSG_TIMER_END = 3;
    private static final int MSG_STOP_CHECK = 4;
    private static final int MSG_STOP_CHECK_DONE = 5;
    private static final int MSG_UPDATE_PROGRESS = 6;

    private static String KEY_SYSTEM_PROPERTY_LOG_PATH_TYPE = "persist.mtklog.log2sd.path";
    private static final String LOG_PATH_TYPE_INTERNAL_SD = "internal_sd";
    private static final String LOG_PATH_TYPE_EXTERNAL_SD = "external_sd";
    private static final String LOG_PATH_TYPE_PHONE = "/data";
    /**
     * MtkLogger related customer config parameters.
     */
    private static final String CUSTOMIZE_CONFIG_FILE = "/system/etc/mtklog-config.prop";
    private static final String KEY_CONFIG_FILE_LOG_PATH_TYPE = "mtklog_path";
    private static final String DEFAULT_LOG_PATH_TYPE = "internal_sd";
    private static StorageManager mStorageManager = null;
    private boolean mExported;

    private ProgressDialog mProgressDlg;
    private ActionBar mActionBar;
    private TextView mReportPath;

    private String mCheckType;
    private boolean mIsStaticCheck;
    private int mTimer;
    private boolean mIsTimerEnd;
    private ConditionManager mConditionManager;
    private List<TestCase> mCurCheckCaseList; //cases of current check type
    private HandlerThread mCheckerThread;
    private Handler mCheckerHandler;
    private Date mStartCheckDate;
    private File mExportDir;
    private File mExportFile;
    private String mNextLine = System.getProperty("line.separator");

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FTPCLog.d(TAG, "handleMessage what = " + msg);
            switch (msg.what) {
            case MSG_START_CHECK_DONE:
                handleStartCheckDone();
                break;

            case MSG_TIMER_END:
                handleTimerEnd();
                break;

            case MSG_STOP_CHECK_DONE:
                handleStopCheckDone();
                break;

            case MSG_UPDATE_PROGRESS:
                mProgressDlg.setProgress((Integer) msg.obj);
                break;

            default:
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        mCheckType = getIntent().getStringExtra("check_type");
        if (mCheckType != null) {
            mIsStaticCheck = mCheckType.equalsIgnoreCase(getString(R.string.static_check_type));
        } else {
            mIsStaticCheck = true;
        }
        mTimer = getTimerValue();

        initLayout();

        mConditionManager = ConditionManager.getConditionManager(this);
        mConditionManager.loadConditionsInfo();
        getCurrentCheckCases();
        startChecking();
    }

    private void initLayout() {
        setTitle(mCheckType);
        setContentView(R.layout.check_result);
        mReportPath = (TextView) findViewById(R.id.report_path_text);
        mReportPath.setVisibility(View.INVISIBLE);
        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    private void getCurrentCheckCases() {
        mCurCheckCaseList = mConditionManager.getCurrentCheckCases(mIsStaticCheck);
    }

    private void createCheckItems() {
        mConditionManager.createCheckItems(mCurCheckCaseList);
    }

    private void startChecking() {
        mStartCheckDate = getCurrentDate();
        mCheckerThread = new HandlerThread("checker_thread");
        mCheckerThread.start();
        FTPCLog.d(TAG, "checker thread loop: " + mCheckerThread.getLooper());
        if (mCheckerThread.getLooper() == null) {
            return ;
        }
        mCheckerHandler = new Handler(mCheckerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                FTPCLog.d("CheckerThread", "handleMessage what = " + msg);
                switch (msg.what) {
                case MSG_START_CHECK:
                    handleStartCheck();
                    break;

                case MSG_STOP_CHECK:
                    handleStopCheck();
                    break;

                default:
                    break;
                }
            }
        };
        mCheckerHandler.sendEmptyMessage(MSG_START_CHECK);

        //set timer
        mIsTimerEnd = false;
        mUiHandler.sendEmptyMessageDelayed(MSG_TIMER_END, mTimer * 1000);
        showProgressDialog();
    }

    private void showProgressDialog() {
        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDlg.setTitle("Checking...");
        mProgressDlg.setIndeterminate(false);
        mProgressDlg.setCancelable(false);
        mProgressDlg.setCanceledOnTouchOutside(false);
        mProgressDlg.setMax(mTimer);
        mProgressDlg.show();

        //Thread to update progress dialog. Just a rough indicator for the timer progress.
        new Thread(new Runnable() {

            @Override
            public void run() {
                int count = 1;
                while (count <= mTimer) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mUiHandler.sendMessage(mUiHandler.obtainMessage(MSG_UPDATE_PROGRESS, count));
                    count++;
                }
            }
        }, "ProgressUpdater").start();
    }

    /**
     * Get timer value from Setting's share preference.
     * @return value of timer in second
     */
    private int getTimerValue() {
        int timer;
        SharedPreferences settings = getSharedPreferences(
                SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        if (mIsStaticCheck) {
            timer = settings.getInt(SettingsActivity.KEY_STATIC_TIMER,
                    SettingsActivity.DEFAULT_STATIC_TIMER);
            FTPCLog.i(TAG, "static timer is " + timer);
        } else {
            timer = settings.getInt(SettingsActivity.KEY_DYNAMIC_TIMER,
                    SettingsActivity.DEFAULT_DYNAMIC_TIMER);
            FTPCLog.i(TAG, "dynamic timer is " + timer);
        }
        return timer;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

    }

    private void handleStartCheck() {
        createCheckItems();
        for (TestCase testCase : mCurCheckCaseList) {
            for (Condition condition : testCase.getConditionList()) {
                if (condition.needCheck()) {
                    condition.getCheckItem().onCheck();
                }
            }
        }
        mUiHandler.sendEmptyMessage(MSG_START_CHECK_DONE);
    }

    private void handleStopCheck() {
        for (TestCase testCase : mCurCheckCaseList) {
            for (Condition condition : testCase.getConditionList()) {
                if (condition.needCheck()) {
                    condition.getCheckItem().onStopCheck();
                }
            }
        }
        mUiHandler.sendEmptyMessage(MSG_STOP_CHECK_DONE);
    }

    private void handleStartCheckDone() {
        if (mIsTimerEnd) {
            //MSG_START_CHECK_DONE arrived after MSG_TIMER_END, below errors might has occurred:
            //(1)Timer value set too small.
            //(2)Some Condition.mCheckItem.onCheck consumes too much time,
            //may need a separate thread to do it.
            FTPCLog.e(TAG, "error: MSG_START_CHECK_DONE should arrive before MSG_TIMER_END!");
        }
    }

    private void handleTimerEnd() {
        mIsTimerEnd = true;
        mCheckerHandler.sendEmptyMessage(MSG_STOP_CHECK);
    }

    private void handleStopCheckDone() {
        constructResultCases();
        mExported = exportCheckReport();
        updateLayout();
    }

    private void constructResultCases() {
        mConditionManager.constructResultCases(mCurCheckCaseList);
    }

    /**
     * Export path should be the same with the path of mtklog.
     */
    private boolean exportCheckReport() {
        String path = getMtkLogPath(getMtkLogPathType());
        if (!checkPath(path)) {
            Toast.makeText(this, getString(R.string.log_path_not_exist, path),
                    Toast.LENGTH_SHORT).show();
            FTPCLog.e(TAG, "report export path is not valid!");
            return false;
        }
        if (!createExportDir(path)) {
            FTPCLog.i(TAG, "Create check report directory failed!");
            return false;
        }

        //create file and write
        boolean exported = false;
        String csvReportStr = generateReport_CSV();
        byte[] cvsHead = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String checkTypeEnglist = "StaticCheck"; //ddms can not pull files that file name contains Chinese
        if (!mIsStaticCheck) {
            checkTypeEnglist = "DynamicCheck";
        }
        String startCheckTime = getTimeByFormat(mStartCheckDate, "yyyyMMddHHmmss");
        String exportFileName = String.format(getString(R.string.export_file_name),
                checkTypeEnglist, startCheckTime);
        mExportFile = new File(mExportDir, exportFileName);
        try {
            FileOutputStream fout = new FileOutputStream(mExportFile);
            fout.write(cvsHead);
            fout.write(csvReportStr.getBytes());
            fout.close();
            exported = true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            FTPCLog.i(TAG, "Export check report file failed!");
            e.printStackTrace();
        }
        return exported;
    }

    /**
     * Get current mtk log path type, one of phone, internal SD card and external SD card.
     */
    private String getMtkLogPathType() {
        String logPathType = SystemProperties.get(KEY_SYSTEM_PROPERTY_LOG_PATH_TYPE);
        FTPCLog.v(TAG, "SystemProperties key=" + KEY_SYSTEM_PROPERTY_LOG_PATH_TYPE
                + " value=" + logPathType);

        if (TextUtils.isEmpty(logPathType)) {
            Properties customizeProp = new Properties();
            FileInputStream customizeInputStream = null;
            try {
                customizeInputStream = new FileInputStream(CUSTOMIZE_CONFIG_FILE);
                customizeProp.load(customizeInputStream);
                logPathType = customizeProp.getProperty(KEY_CONFIG_FILE_LOG_PATH_TYPE);
            } catch (IOException e) {
                FTPCLog.v(TAG, "but read customize config file error!" + e.toString());
            } finally {
                if (customizeInputStream != null) {
                    try {
                        customizeInputStream.close();
                    } catch (IOException e2) {
                        FTPCLog.v(TAG, "Fail to close opened customization file.");
                    }
                }
            }
        }
        if (TextUtils.isEmpty(logPathType)) {
            logPathType = DEFAULT_LOG_PATH_TYPE;
        }
        return logPathType;
    }

    /**
     * Get current detail mtk log folder path string according to given log path type.
     */

    private  String getInternalSdPath(Context context) {
        FTPCLog.d(TAG, "-->getInternalSdPath()");
        String internalPath = null;
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (StorageVolume volume : volumes) {
            String volumePathStr = volume.getPath();
            if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(volume.getState())) {
                FTPCLog.d(TAG, volumePathStr + " is mounted!");
                VolumeInfo volumeInfo = mStorageManager.findVolumeById(volume.getId());
                if (volume.isEmulated()) {
                    String viId = volumeInfo.getId();
                    FTPCLog.d(TAG, "Is emulated and volumeInfo.getId() : " + viId);
                    // If external sd card, the viId will be like "emulated:179,130"
                    if (viId.equalsIgnoreCase("emulated")) {
                        internalPath = volumePathStr;
                        break;
                    }
                } else {
                    DiskInfo diskInfo = volumeInfo.getDisk();
                    if (diskInfo == null) {
                        continue;
                    }
                    String diId = diskInfo.getId();
                    FTPCLog.i(TAG, "Is not emulated and diskInfo.getId() : " + diId);
                    // If external sd card, the diId will be like "disk:179,128"
                    if (diId.equalsIgnoreCase("disk:179,0")) {
                        internalPath = volumePathStr;
                        break;
                    }
                }
            } else {
                FTPCLog.d(TAG, volumePathStr + " is not mounted!");
            }
        }
        FTPCLog.d(TAG, "<--getInternalSdPath() : internalPath = " + internalPath);
        return internalPath;
    }


   private String getExternalSdPath(Context context) {
        FTPCLog.d(TAG, "-->getExternalSdPath()");
        String externalPath = null;
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (StorageVolume volume : volumes) {
            String volumePathStr = volume.getPath();
            if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(volume.getState())) {
                FTPCLog.d(TAG, volumePathStr + " is mounted!");
                VolumeInfo volumeInfo = mStorageManager.findVolumeById(volume.getId());
                if (volume.isEmulated()) {
                    String viId = volumeInfo.getId();
                    FTPCLog.d(TAG, "Is emulated and volumeInfo.getId() : " + viId);
                    // If external sd card, the viId will be like "emulated:179,130"
                    if (!viId.equalsIgnoreCase("emulated")) {
                        externalPath = volumePathStr;
                        break;
                    }
                } else {
                    DiskInfo diskInfo = volumeInfo.getDisk();
                    if (diskInfo == null) {
                        continue;
                    }
                    String diId = diskInfo.getId();
                    FTPCLog.d(TAG, "Is not emulated and diskInfo.getId() : " + diId);
                    // If external sd card, the diId will be like "disk:179,128"
                    if (!diId.equalsIgnoreCase("disk:179,0")) {
                        externalPath = volumePathStr;
                        break;
                    }
                }
            } else {
                FTPCLog.d(TAG, volumePathStr + " is not mounted!");
            }
        }
        FTPCLog.d(TAG, "<--getExternalSdPath() : externalPath = " + externalPath);
        return externalPath;
    }

    private String getMtkLogPath(String logPathType) {
        String logPathStr = null;
        if (LOG_PATH_TYPE_INTERNAL_SD.equals(logPathType)) {
            logPathStr = getInternalSdPath(this);
        } else if (LOG_PATH_TYPE_EXTERNAL_SD.equals(logPathType)) {
            logPathStr = getExternalSdPath(this);
        } else if (LOG_PATH_TYPE_PHONE.equals(logPathType)) {
            logPathStr = LOG_PATH_TYPE_PHONE;
        } else {
            FTPCLog.e(TAG, "Unsupported log path type: " + logPathType);
        }

        if (logPathStr == null) {
            FTPCLog.e(TAG, "Fail to get detail log path string for type: "
                          + logPathType + ", return empty to avoid NullPointerException.");
            logPathStr = "";
        }

        File logParent = new File(logPathStr);
        try {
            if (logParent.exists()) {
                logPathStr = logParent.getCanonicalPath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        FTPCLog.v(TAG, "getMtkLogPath() type=" + logPathType + ", string=" + logPathStr);

        return logPathStr;
    }

    private boolean checkPath(String path) {
        boolean isExist = true;
        if (!new File(path).exists()) {
            isExist = false;
        }
        String mountStatus = getVolumeState(this, path);
        FTPCLog.v(TAG, "checkPath(), path=" + path + ", exist=" + isExist
                + ", volumeState=" + mountStatus);
        //For /data, should not judge its volume state
        return isExist && (LOG_PATH_TYPE_PHONE.equals(path)
                || Environment.MEDIA_MOUNTED.equals(mountStatus));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getVolumeState(Context context, String pathStr) {
        String status = "Unknown";
        if (TextUtils.isEmpty(pathStr)) {
            FTPCLog.v(TAG, "Empty pathString when cal getVolumnState");
            return status;
        }
        try {
            Class storageManagerFromJB = StorageManager.class;
            Method getVolumeStateMethod = storageManagerFromJB.
                    getDeclaredMethod("getVolumeState", String.class);
            if (getVolumeStateMethod != null) {
                if (mStorageManager == null) {
                    mStorageManager = (StorageManager) context.
                            getSystemService(Context.STORAGE_SERVICE);
                }
                status = (String) getVolumeStateMethod.invoke(mStorageManager, pathStr);
                return status;
            }
        } catch (Exception e) {
            FTPCLog.v(TAG, "Fail to access StorageManager.getVolumnState(). No such method.");
        }

        return status;
    }

    private Date getCurrentDate() {
        return new java.util.Date();
    }

    private String getTimeByFormat(Date date, String format) {
        SimpleDateFormat formatTime = new SimpleDateFormat(format, Locale.getDefault());
        return formatTime.format(date);
    }

    private String getTestLocation() {
        SharedPreferences settings = getSharedPreferences(
                SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        return settings.getString(SettingsActivity.KEY_TEST_LOCATION, "");
    }

    private String generateReport_CSV() {
        //generate executive cases report
        StringBuilder execCase = new StringBuilder("");
        execCase.append(getString(R.string.report_exec_cases));
        for (TestCase tc : mConditionManager.getResultExecCases()) {
            execCase.append(mNextLine)
                    .append(tc.getId() + " " + tc.getName());
        }

        //generate none-executive cases report
        StringBuilder unexecCase = new StringBuilder("");
        unexecCase.append(getString(R.string.report_unexec_cases))
                  .append(",")
                  .append(getString(R.string.report_unexec_reason));
        for (TestCase tc : mConditionManager.getResultUnexecCases()) {
            unexecCase.append(mNextLine)
                      .append(tc.getId() + " " + tc.getName());
            int unsatCount = 0;
            for (int i = 0; i < tc.getConditionList().size(); i++) {
                Condition condition = tc.getCondition(i);
                if (!condition.needCheck()) {
                    continue;
                }
                if (condition.getCheckItem().getCheckResult().equals(CheckResult.UNSATISFIED)) {
                    unsatCount++;
                    if (unsatCount == 1) {
                        unexecCase.append(",");
                    } else {
                        unexecCase.append(mNextLine)
                                  .append("\"\"")
                                  .append(",");
                    }
                    unexecCase.append("[" + condition.getDescription() + "]:"
                                      + condition.getCheckItem().getValue());
                }
            }
        }

        // generate wifi channel info
        StringBuilder wifiInfoBuilder = new StringBuilder("");
        HashMap<Integer, Integer> infoMap = mConditionManager.getWifiChanneInfo();
        if (infoMap != null && !infoMap.isEmpty()) {
            FTPCLog.i(TAG, "mConditionManager.getWifiChanneInfo(): " + infoMap.toString());

            List<Map.Entry<Integer, Integer>> infoList =
                    new ArrayList<Map.Entry<Integer, Integer>>(infoMap.entrySet());
            Comparator<Map.Entry<Integer, Integer>> comparator =
                    new Comparator<Map.Entry<Integer, Integer>>() {

                @Override
                public int compare(Entry<Integer, Integer> object1,
                        Entry<Integer, Integer> object2) {
                    return object1.getKey() - object2.getKey();
                }
            };
            Collections.sort(infoList, comparator);
            FTPCLog.i(TAG, "sorted wifi channel info list: " + infoList.toString());

            wifiInfoBuilder.append(getString(R.string.report_wifi_chan_info))
                           .append(mNextLine)
                           .append(getString(R.string.channel));
            for (Entry<Integer, Integer> entry : infoList) {
                wifiInfoBuilder.append(",")
                               .append(entry.getKey().toString());
            }
            wifiInfoBuilder.append(mNextLine)
                           .append(getString(R.string.AP_count));
            for (Entry<Integer, Integer> entry : infoList) {
                wifiInfoBuilder.append(",")
                               .append(entry.getValue().toString());
            }

            // clear wifi channel info in case missing use in next
            // condition checking which does not contain CheckWifiAp.
            mConditionManager.setWifiChanneInfo(null);
        }

        //generate all report
        StringBuilder csvReport = new StringBuilder("");
        csvReport.append(getString(R.string.check_type))
                 .append(",")
                 .append(mCheckType)
                 .append(mNextLine)
                 .append(getString(R.string.start_check_time))
                 .append(",")
                 .append(getTimeByFormat(mStartCheckDate, "yyyy-MM-dd HH:mm:ss"))
                 .append(mNextLine)
                 .append(getString(R.string.timer))
                 .append(",")
                 .append(mTimer)
                 .append(mNextLine)
                 .append(getString(R.string.test_location))
                 .append(",")
                 .append(getTestLocation())
                 .append(mNextLine)
                 .append(mNextLine)
                 .append(execCase.toString())
                 .append(mNextLine)
                 .append(mNextLine)
                 .append(unexecCase.toString())
                 .append(mNextLine)
                 .append(mNextLine)
                 .append(wifiInfoBuilder.toString());

        return csvReport.toString();
    }

    private void setReportPathViewText(boolean exported) {
        String text;
        if (exported) {
            text = String.format(getString(R.string.report_path_succeed),
                    mExportFile.getAbsoluteFile());
        } else {
            text = getString(R.string.report_path_failed);
        }
        mReportPath.setText(text);
    }

    private boolean createExportDir(String path) {
        String exportDirStr = getString(R.string.export_dir);
        exportDirStr = String.format(exportDirStr, path);
        mExportDir = new File(exportDirStr);
        if (!mExportDir.exists()) {
            if (!mExportDir.mkdirs()) {
                return false;
            }
        }
        return true;
    }

    private void updateLayout() {
        setReportPathViewText(mExported);
        showTabs();
        mReportPath.setVisibility(View.VISIBLE);
        dismissProgressDialog();
    }

    private void dismissProgressDialog() {
        mProgressDlg.setProgress(mTimer); //set progress show completed for sure
        mProgressDlg.dismiss();
    }

    private void showTabs() {
        Tab tab = mActionBar.newTab()
                .setText(R.string.exec_cases)
                .setTabListener(new TabListener<TabFragment>(this, TabFragment.class, true));
        mActionBar.addTab(tab);
        tab = mActionBar.newTab()
                .setText(R.string.unexec_cases)
                .setTabListener(new TabListener<TabFragment>(this, TabFragment.class, false));
        mActionBar.addTab(tab);
    }

    class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Context mFragmentContext;
        private final Class<T> mFragmentClass;
        private final boolean mIsExecTab;

        public TabListener(Context fragContext, Class<T> fragClass, boolean isExecTab) {
            mFragmentContext = fragContext;
            mFragmentClass = fragClass;
            mIsExecTab = isExecTab;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // use trans instead of parameter ft to prevent below exception:
            // IllegalStateException: can not perform this action after onSaveInstanceState().
            // Because android will call ft.commit() after onTabSelected(), and if
            // ft.commit() is called after Activity's onSaveInstanceState(), android will
            // throw this exception. So here call trans.commitAllowingStateLoss() by ourselves
            // to prevent this exception.
            FragmentTransaction trans = getFragmentManager().beginTransaction();
            if (mFragment == null) {
                Bundle args = new Bundle();
                args.putBoolean("tab_type", mIsExecTab);
                mFragment = Fragment.instantiate(mFragmentContext, mFragmentClass.getName(), args);
                trans.add(R.id.tab_fragment, mFragment);
            } else {
                trans.attach(mFragment);
            }
            trans.commitAllowingStateLoss();
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            // TODO Auto-generated method stub
            if (mFragment != null) {
                FragmentTransaction trans = getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                trans.commitAllowingStateLoss();
            }
        }
    }
}
