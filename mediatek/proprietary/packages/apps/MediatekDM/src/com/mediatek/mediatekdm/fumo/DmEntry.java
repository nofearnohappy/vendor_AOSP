/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm.fumo;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.mediatek.custom.CustomProperties;
import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmOperationManager.IOperationStateObserver;
import com.mediatek.mediatekdm.DmOperationManager.State;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.fumo.FumoComponent.FumoBinder;
import com.mediatek.mediatekdm.mdm.fumo.MdmFumoUpdateResult;
import com.mediatek.mediatekdm.util.DialogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry for Client initiated firmware update. It's only a UI for DmClient. See 5.1.5.4.2 in China
 * Mobile Device Management Service Terminal Specification 3.0.0
 */
public class DmEntry extends PreferenceActivity {
    private static final String TAG = "DM/Entry";
    private FumoBinder mBinder = null;

    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService();
        mContext = this;
        mHandler = new Handler();
        addPreferencesFromResource(R.xml.system_update);

        String vtime = getFormattedKernelVersion();
        String buildDate = "";
        if (!vtime.equals("Unavailable")) {
            Log.i(TAG, "DmEntry: version time = " + vtime);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", new Locale("US"));
            try {
                Date versionTime = new Date(Date.parse(vtime));
                buildDate = df.format(versionTime);
            } catch (IllegalArgumentException e) {
                // Date.parse(vtime) definition throws no exceptions but it throws
                Log.e(TAG, "parse " + vtime + " error!!!");
                buildDate = "2011-01-01";
            }
        } else {
            Log.e(TAG, "get build date error");
            buildDate = "2011-01-01";
        }

        Log.i(TAG, " DmEntry: Date = " + buildDate);

        // Current version info
        Preference currentVersionPreference = findPreference(VERSION_PREFERENCE);
        currentVersionPreference.setTitle(getResources().getString(R.string.current_version) + " "
                + CustomProperties.getString("Setting", "SWVerno", Build.DISPLAY));
        currentVersionPreference.setSummary(getResources().getString(R.string.release_date) + " "
                + buildDate);

        mUpdatePreference = findPreference(UPDATE_PREFERENCE);
        mInstallPreference = findPreference(INSTALL_PREFERENCE);
    }

    private void bindService() {
        Log.d(TAG, "+bindService()");
        Intent intent = new Intent(this, DmService.class);
        intent.setAction(FumoComponent.BIND_FUMO);
        if (!bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            throw new Error("Failed to bind to fumo service.");
        }
        Log.d(TAG, "-bindService()");
    };

    protected void onStart() {
        super.onStart();
        updateUI();
    }

    protected void onDestroy() {
        Log.i(TAG, "DmEntry onDestroy");
        unbindService(mServiceConnection);
        if (mFumo != null) {
            Log.i(TAG, "DmEntry unregister listener");
            mFumo.unregisterObserver(mFumoObserver);
        }
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.i(TAG, "[DmEntry] enter in onPreferenceTreeClick.");
        String key = preference.getKey();
        // Check whether this phone has registered to DM server via SMS
        if (DmConfig.getInstance().useSmsReg()
                && PlatformManager.getInstance().getRegisteredSubId() == -1) {
            showDialog(DIALOG_SUB_NOT_REGISTERED);
        } else if (key.equals(UPDATE_PREFERENCE)) {
            // The availability is controlled by updateUI()
            int fumoState = mFumo.getFumoState();
            Log.d(TAG, "Update preference clicked with state " + fumoState);
            if (fumoState == DmFumoState.DOWNLOAD_PAUSED) {
                showDialog(DIALOG_RECOVER_OR_START_NEW);
            } else if (fumoState == DmFumoState.DOWNLOAD_COMPLETE) {
                showDialog(DIALOG_DISCARD_AND_START_NEW);
            } else {
                startDmActivity();
            }
        } else if (key.equals(INSTALL_PREFERENCE)) {
            Log.i(TAG, "Update intall key clicked!");
            startDmActivity();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_RECOVER_OR_START_NEW:
                return DialogFactory.newAlert(this).setTitle(R.string.system_update)
                        .setMessage(R.string.old_pkg_exists)
                        .setPositiveButton(R.string.start_new_session,
                        // Launch DmClient to resume last one
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        startDmActivity();
                                    }
                                }).setNegativeButton(R.string.discard,
                        // Discard last one and launch DmClient to start fresh
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // report to server
                                        mFumo.clearDlStateAndReport(10000,
                                                MdmFumoUpdateResult.ResultCode.USER_CANCELED.val);
                                        mFumo.setFumoState(DmFumoState.QUERY_NEW_VERSION,
                                                mOperationManager.current(), null);
                                        startDmActivity();
                                    }
                                }).create();
            case DIALOG_DISCARD_AND_START_NEW:
                return DialogFactory.newAlert(this).setTitle(R.string.system_update)
                        .setMessage(R.string.download_restart)
                        .setPositiveButton(R.string.start_new_session,
                        // Discard the last one and start fresh
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        PersistentContext.getInstance().deleteDeltaPackage();
                                        mFumo.setFumoState(DmFumoState.QUERY_NEW_VERSION,
                                                mOperationManager.current(), null);
                                        deleteFile(FumoComponent.FUMO_RESUME_FILE_NAME);
                                        startDmActivity();
                                    }
                                }).setNegativeButton(R.string.cancel,
                        // Do nothing
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                }).create();
            case DIALOG_SUB_NOT_REGISTERED:
                return DialogFactory.newAlert(this).setTitle(R.string.system_update)
                        .setMessage(R.string.sim_not_register)
                        .setPositiveButton(R.string.start_new_session, // TODO use a separate string
                                                                       // for this
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // do nothing
                                    }
                                }).create();
            default:
                return null;
        }
    }

    private void startDmActivity() {
        Intent activityIntent = new Intent(mContext, DmClient.class);
        activityIntent.setAction("com.mediatek.mediatekdm.DMCLIENT");
        startActivity(activityIntent);
    }

    // Check the logic here and pay attention to FumoStatus & DmSessionStatus
    private void updateUI() {
        Log.d(TAG, "+updateUI()");
        if (mBinder == null) {
            Log.d(TAG, "Service has not been connected.");
            mUpdatePreference.setEnabled(false);
            mUpdatePreference.setSummary("");
            mInstallPreference.setEnabled(false);
            mInstallPreference.setSummary("");
            Log.d(TAG, "-updateUI()");
            return;
        }

        // Set system update status
        DownloadInfo dlInfo = PersistentContext.getInstance().getDownloadInfo();
        String dlVersion = (dlInfo != null ? dlInfo.version : "");
        int fumoState = mFumo.getFumoState();
        Log.d(TAG, "Current FUMO state is " + fumoState);

        if (fumoState == DmFumoState.DOWNLOAD_PAUSED) {
            mUpdatePreference.setSummary(getResources().getString(R.string.download_status) + " "
                    + dlVersion);
        } else if (fumoState == DmFumoState.DOWNLOADING) {
            long currentSize = PersistentContext.getInstance().getDownloadedSize();
            long totalSize = PersistentContext.getInstance().getTotalSize();
            int mCurrentProgress = 0;
            mCurrentProgress = (int) ((double) currentSize / (double) totalSize * 100);
            mUpdatePreference.setSummary(getResources().getString(R.string.download_status) + " "
                    + mCurrentProgress + "%");
        } else {
            mUpdatePreference.setSummary("");
        }

        if (mOperationManager.isBusy()) {
            DmOperation op = mOperationManager.current();
            boolean isFumoDL = op.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                    && op.getBooleanProperty(KEY.FUMO_TAG, false);
            if (!isFumoDL) {
                mUpdatePreference.setSummary(getResources().getString(R.string.cmcc_task_running));
                mUpdatePreference.setEnabled(false);
            } else {
                mUpdatePreference.setEnabled(true);
            }
        } else if (mOperationManager.hasNext()) {
            mUpdatePreference.setSummary(getResources().getString(R.string.cmcc_task_running));
            mUpdatePreference.setEnabled(false);
        } else {
            mUpdatePreference.setEnabled(true);
        }

        // Set install status
        if (fumoState == DmFumoState.DOWNLOAD_COMPLETE) {
            mInstallPreference.setEnabled(true);
            mInstallPreference.setSummary(getResources().getString(R.string.install_version) + " "
                    + dlVersion);
        } else {
            mInstallPreference.setEnabled(false);
            mInstallPreference.setSummary("");
        }
        Log.d(TAG, "-updateUI()");
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.w(TAG, "onServiceConnected DmClient register listener");
            mBinder = (FumoBinder) binder;
            mFumo = mBinder.getManager();
            mFumo.registerObserver(mFumoObserver);
            mFumo.registerDownloadObserver(mDownloadObserver);
            DmOperationManager.getInstance().registerObserver(mOperationObserver);
            updateUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            mFumo.unregisterObserver(mFumoObserver);
            mFumo.unregisterDownloadObserver(mDownloadObserver);
            DmOperationManager.getInstance().unregisterObserver(mOperationObserver);
            mFumo = null;
            mBinder = null;
            Log.i(TAG, "Disconnected from service.");
        }
    };

    private String getFormattedKernelVersion() {

        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
                Log.e(TAG, "[getFormattedKernelVersion]+ procVersionStr is " + procVersionStr);
            } finally {
                reader.close();
            }
            if (procVersionStr == null) {
                return "Unavailable";
            }
            final String procVersionRegex = "\\w+\\s+" + /* ignore: Linux */
            "\\w+\\s+" + /* ignore: version */
            "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
            "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /*
                                                         * group 2: (xxxxxx
                                                         * @ xxxxx . constant )
                                                         */
            "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /*
                                                      * ignore: (gcc ..)
                                                      */
            "([^\\s]+)\\s+" + /* group 3: #26 */
            "#\\d\\s+SMP\\s+PREEMPT\\s+" + /* ignore: PREEMPT (optional) */
            "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(procVersionRegex);
            if (p == null) {
                return "Unavailable";
            }
            Matcher m = p.matcher(procVersionStr);
            if (m == null) {
                return "Unavailable";
            }
            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "Unavailable";
            } else {
                StringBuilder buildVersion = new StringBuilder(m.group(4));
                return buildVersion.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting kernel version for Device Info screen", e);

            return "Unavailable";
        }
    }

    private Context mContext;
    private FumoManager mFumo;
    private Handler mHandler;
    private DmOperationManager mOperationManager = DmOperationManager.getInstance();
    private IDmFumoStateObserver mFumoObserver = new IDmFumoStateObserver() {
        @Override
        public void notify(int newState, int previousState, final DmOperation operation,
                Object extra) {
            Log.d(TAG, "Notify DmEntry of FUMO state transition.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
    };

    private IOperationStateObserver mOperationObserver = new IOperationStateObserver() {
        @Override
        public void notify(State state, State previousState, Object extra) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
    };

    private IDmFumoDownloadProgressObserver mDownloadObserver = new IDmFumoDownloadProgressObserver() {
        @Override
        public void updateProgress(final long current, final long total) {
            if (mFumo.getFumoState() == DmFumoState.DOWNLOADING) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int mCurrentProgress = 0;
                        mCurrentProgress = (int) ((double) current / (double) total * 100);
                        mUpdatePreference.setSummary(getResources().getString(
                                R.string.download_status)
                                + " " + mCurrentProgress + "%");
                    }
                });
            }
        }
    };

    private Preference mUpdatePreference;
    private Preference mInstallPreference;
    private static final String VERSION_PREFERENCE = "current_version";
    private static final String UPDATE_PREFERENCE = "system_update";
    private static final String INSTALL_PREFERENCE = "update_install";
    private static final int DIALOG_RECOVER_OR_START_NEW = 0;
    private static final int DIALOG_DISCARD_AND_START_NEW = 1;
    private static final int DIALOG_SUB_NOT_REGISTERED = 2;
}
