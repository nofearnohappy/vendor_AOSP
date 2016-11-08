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

package com.mediatek.dm.fumo;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.mediatek.dm.DmCommonFun;
import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmService;
import com.mediatek.dm.R;
import com.mediatek.dm.data.DownloadInfo;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.util.DialogFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmEntry extends PreferenceActivity {
    private static final String TAG = "DM/Entry";

    private Context mContext;
    private DmService mService;

    private int mDmSessionStatus;
    private int mFumoStatus;
    private Preference mUpdatePreference;
    private Preference mInstallPreference;

    private int mProgress = -1;

    private static final String VERSION_PREFERENCE = "current_version";
    private static final String UPDATE_PREFERENCE = "system_update";
    private static final String INSTALL_PREFERENCE = "update_install";
    private static final int DIALOG_COVEROLDPKG = 0;
    private static final int DIALOG_PROGRAMCRASH = 1;
    private static final int DIALOG_DLGCOMPLETE = 2;
    private static final int DIALOG_SIMNOTREGISTER = 3;

    private static final String DEFAULT_VERSION_TIME = "2011-01-01";
    private static final String DEFAULT_LOCAL = "US";
    private static final String DATE_TEMPLATE = "yyyy-MM-dd";
    private static final String ERROR_VERSION = "Unavailable";
    private static final String PROC_VERSION_PATH = "/proc/version";
    private static final int FILE_READER_SIZES = 256;
    private static final int PROC_VERSION_GROUP_LENGTH = 4;

    protected void onDestroy() {
        Log.i(TAG, "DmEntry onDestroy");
        // unbindService(mConnection);
        if (mEntryObserver != null) {
            Log.w(TAG, "onDestroy DmEntry unregister listener");
            PersistentContext.getInstance(DmEntry.this).unregisterObserver(mEntryObserver);
        }
        super.onDestroy();
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // startDmService();
        mContext = this;
        addPreferencesFromResource(R.xml.system_update);

        // get version time
        String vtime = getFormattedKernelVersion();
        String buildDate = "";
        try {
            if (!ERROR_VERSION.equals(vtime)) {
                Log.i(TAG, "version time = " + vtime);
                SimpleDateFormat df = new SimpleDateFormat(DATE_TEMPLATE, new Locale(DEFAULT_LOCAL));
                Date versionTime = new Date(Date.parse(vtime));
                buildDate = df.format(versionTime);
            } else {
                Log.e(TAG, "get build date error");
                buildDate = DEFAULT_VERSION_TIME;
            }
            Log.i(TAG, "Date = " + buildDate);
        } catch (NullPointerException e) {
            Log.e(TAG, "There is some exception accured parse date string!");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "There is some exception accured parse date string!");
            e.printStackTrace();

        }

        // fill current version info
        Preference currentVersionPreference = findPreference(VERSION_PREFERENCE);
        currentVersionPreference.setTitle(getResources().getString(R.string.current_version,
                Build.DISPLAY));
        currentVersionPreference.setSummary(getResources().getString(R.string.release_date,
                buildDate));

        mUpdatePreference = findPreference(UPDATE_PREFERENCE);
        mInstallPreference = findPreference(INSTALL_PREFERENCE);

        if (mEntryObserver != null) {
            Log.w(TAG, "onCreate DmEntry register listener");
            PersistentContext.getInstance(this).registerObserver(mEntryObserver);
        }
    }

    protected void onResume() {
        super.onResume();

        mProgress = -1;
        mFumoStatus = PersistentContext.getInstance(this).getDLSessionStatus();
        mDmSessionStatus = PersistentContext.getInstance(this).getDMSessionStatus();
        Log.i(TAG, "onResume the dlstatus is " + mFumoStatus + ",the dm status is "
                + mDmSessionStatus);
        updateUI();
    }

    protected void onPause() {
        super.onPause();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        // if sim card is not register to dm server
        if (Options.USE_SMS_REGISTER && DmCommonFun.getRegisterSubID(mContext) == -1) {
            showDialog(DIALOG_SIMNOTREGISTER);
        } else if (key.equals(UPDATE_PREFERENCE)) {
            Log.i(TAG, "System update key clicked! status is " + mFumoStatus);
            if (mFumoStatus == IDmPersistentValues.STATE_PAUSE_DOWNLOAD) {
                showDialog(DIALOG_COVEROLDPKG);
            } else if (mFumoStatus == IDmPersistentValues.STATE_DL_PKG_COMPLETE
                    || mFumoStatus == IDmPersistentValues.STATE_VERIFY_NO_STORAGE
                    || mFumoStatus == IDmPersistentValues.STATE_VERIFY_FAIL) {
                showDialog(DIALOG_DLGCOMPLETE);
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
        case DIALOG_COVEROLDPKG:
            return DialogFactory
                    .newAlert(this)
                    .setTitle(R.string.system_update)
                    .setMessage(R.string.old_pkg_exists)
                    .setPositiveButton(R.string.start_new_session,
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                        final int whichButton) {
                                    startDmActivity();
                                }
                            })
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {

                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            PersistentContext.getInstance(DmEntry.this).resetDLStatus();
                            startDmActivity();
                        }
                    }).create();
        case DIALOG_PROGRAMCRASH:
            return DialogFactory
                    .newAlert(this)
                    .setTitle(R.string.system_update)
                    .setMessage(R.string.program_crash)
                    .setPositiveButton(R.string.start_new_session,
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                        final int whichButton) {
                                    PersistentContext.getInstance(DmEntry.this).resetDLStatus();
                                    startDmActivity();
                                }
                            }).create();
        case DIALOG_DLGCOMPLETE:
            return DialogFactory
                    .newAlert(this)
                    .setTitle(R.string.system_update)
                    .setMessage(R.string.download_restart)
                    .setPositiveButton(R.string.start_new_session,
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                        final int whichButton) {
                                    PersistentContext.getInstance(DmEntry.this).resetDLStatus();
                                    startDmActivity();
                                }
                            })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            finish();
                        }
                    }).create();
        case DIALOG_SIMNOTREGISTER:
            return DialogFactory
                    .newAlert(this)
                    .setTitle(R.string.system_update)
                    .setMessage(R.string.sim_not_register)
                    .setPositiveButton(R.string.start_new_session,
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,
                                        final int whichButton) {
                                    // do nothing
                                }
                            }).create();
        default:
            return null;
        }
    }

    private void startDmActivity() {
        Intent activityIntent = new Intent(mContext, DmClient.class);
        activityIntent.setAction("com.mediatek.dm.DMCLIENT");
        startActivity(activityIntent);
    }

    private void syncDlUI() {

        switch (mFumoStatus) {
        case IDmPersistentValues.STATE_PAUSE_DOWNLOAD:
            DownloadInfo dlInfo = PersistentContext.getInstance(this).getDownloadInfo();
            String dlVersion = ((dlInfo != null) ? dlInfo.mVersion : "");
            mUpdatePreference.setSummary(getResources().getString(R.string.download_status,
                    dlVersion));
            break;
        case IDmPersistentValues.STATE_RESUME_DOWNLOAD:
        case IDmPersistentValues.STATE_START_TO_DOWNLOAD:
            mProgress = -1;
        case IDmPersistentValues.STATE_DOWNLOADING:
            long currentSize = PersistentContext.getInstance(this).getDownloadedSize();
            long totalSize = PersistentContext.getInstance(this).getSize();
            Log.v(TAG,
                    new StringBuilder("current size = ").append(currentSize)
                            .append(", and total size = ").append(totalSize).toString());
            setpartial((int) currentSize, (int) totalSize);
            break;
        case IDmPersistentValues.STATE_DL_PKG_COMPLETE:
        case IDmPersistentValues.STATE_VERIFY_NO_STORAGE:
        case IDmPersistentValues.STATE_VERIFY_FAIL:
            dlInfo = PersistentContext.getInstance(this).getDownloadInfo();
            dlVersion = ((dlInfo != null) ? dlInfo.mVersion : "");
            mInstallPreference.setEnabled(true);
            mInstallPreference.setSummary(getResources().getString(R.string.install_version,
                    dlVersion));
            mUpdatePreference.setSummary("");
            break;
        case IDmPersistentValues.STATE_NEW_VERSION_DETECTED:
            mInstallPreference.setEnabled(false);
            mInstallPreference.setSummary("");
            mUpdatePreference.setSummary(getString(R.string.status_bar_notifications_new_version));
            break;
        default:
            mUpdatePreference.setSummary("");
            break;
        }
    }

    private void updateUI() {

        Log.i(TAG,
                new StringBuilder("System update stauts is : ").append(mFumoStatus)
                        .append(" dm session status is ").append(mDmSessionStatus).toString());

        mInstallPreference.setEnabled(false);
        mInstallPreference.setSummary("");

        switch (mDmSessionStatus) {
        case IDmPersistentValues.STATE_DM_NIA_START:
        case IDmPersistentValues.STATE_DM_NIA_ALERT:
        case IDmPersistentValues.STATE_DM_USERMODE_INTERACT:
        case IDmPersistentValues.STATE_DM_USERMODE_INVISIBLE:
        case IDmPersistentValues.STATE_DM_USERMODE_VISIBLE:
            mUpdatePreference.setSummary(R.string.nia_warning);
            mUpdatePreference.setEnabled(false);
            break;
        case IDmPersistentValues.STATE_DM_NO_ACTION:
        case IDmPersistentValues.STATE_DM_NIA_CANCLE:
        case IDmPersistentValues.STATE_DM_NIA_COMPLETE:
            syncDlUI();
            mUpdatePreference.setEnabled(true);
            break;
        default:
            break;
        }

    }

    private void setpartial(final long download, final long total) {
        int progress = 0;
        try {
            progress = (int) ((double) download / (double) total * 100);
        } catch (ArithmeticException e) {
            Log.e(TAG, e.getMessage());
        }
        if (progress < 0) {
            progress = 0;
        }
        if (mProgress != progress) {
            Log.v(TAG, "set ratio to " + progress);
            mUpdatePreference.setSummary(getResources().getString(R.string.download_status,
                    (new StringBuilder().append(progress).append("%")).toString()));
            mProgress = progress;
        }
    }

    private void startDmService() {
        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction(DmConst.IntentAction.DM_CLIENT);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "DmClient gets service reference");
            mService = ((DmService.DmBinder) service).getService();
            if (mService == null) {
                Log.w(TAG, "onServiceConnected mService is null");
                return;
            }

        }

        public void onServiceDisconnected(final ComponentName className) {
            mService = null;
        }
    };

    private PersistentContext.FumoUpdateObserver mEntryObserver = new PersistentContext.FumoUpdateObserver() {

        public void syncDLstatus(int status) {
            mFumoStatus = status;
            Log.d(TAG, new StringBuilder("[syncDLstatus] syncDlUI status = ").append(status)
                    .toString());
            syncDlUI();
        }

        public void syncDmSession(int status) {
            mDmSessionStatus = status;
            Log.d(TAG, new StringBuilder("[syncDmSession] updateUI status = ").append(status)
                    .toString());
            updateUI();
        }

    };

    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(PROC_VERSION_PATH),
                    FILE_READER_SIZES);

            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            if (procVersionStr == null) {
                return ERROR_VERSION;
            }
            // M: For match proc version pattern
            procVersionStr = procVersionStr.replace(" (prerelease)", "").replace(" SMP", "");
            // M: end
            final String procVersionRegex = "\\w+\\s+" + /* ignore: Linux */
            "\\w+\\s+" + /* ignore: version */
            "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
            "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
            "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
            "([^\\s]+)\\s+" + /* group 3: #26 */
            "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
            "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(procVersionRegex);
            if (p == null) {
                return ERROR_VERSION;
            }
            Matcher m = p.matcher(procVersionStr);
            if (m == null) {
                return ERROR_VERSION;
            }
            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return ERROR_VERSION;
            } else if (m.groupCount() < PROC_VERSION_GROUP_LENGTH) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return ERROR_VERSION;
            } else {
                StringBuilder buildVersion = new StringBuilder(m.group(PROC_VERSION_GROUP_LENGTH));
                return buildVersion.toString();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "FileNotFoundException when getting kernel version for Device Info screen");
            return ERROR_VERSION;
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting kernel version for Device Info screen", e);
            return ERROR_VERSION;
        }
    }

}
