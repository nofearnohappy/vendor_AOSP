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

package com.mediatek.rcs.contacts.networkcontacts.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.networkcontacts.AutoSyncManager;
import com.mediatek.rcs.contacts.networkcontacts.SettingsSharedPreference;

/**
 * Setttings preference.
 *
 */
public class SettingsPreferences extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {
    public static final String TAG = "NetworkContacts::SettingsPreferences";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Log.i(TAG, "onSharedPreferenceChanged: " + key);
        if (key.equals(SettingsSharedPreference.KEY_BACKUP_TYPE)) {
            // 1.update Auto backup Summary
            // 2.update Auto backup alarm message
            updatePreferenceSummary();
            updateAutoBackupAlarmMessage();
        } else if (key.equals(SettingsSharedPreference.KEY_AUTO_BACKUP)) {
            // 1.update Auto backup alarm message
            // 2.set backup type preference status
            updateAutoBackupAlarmMessage();
            updateBackupTypePreferenceStatus();
        } else if (key.equals(SettingsSharedPreference.KEY_WIFI_ONLY)) {
            // do nothing
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initialPreferences();
        Log.i(TAG, "onResume");
        SettingsSharedPreference shared = new SettingsSharedPreference(
                getActivity());
        Log.i(TAG, "isWifiBackupOnly" + shared.isWifiBackupOnly());
        Log.i(TAG, "isAutoBackup" + shared.isAutoBackup());
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        uninitialPreferences();
        super.onPause();
    }

    /**
     * update item's summary.
     */
    public void updatePreferenceSummary() {
        Preference backupTypePreference = findPreference(SettingsSharedPreference.KEY_BACKUP_TYPE);
        Resources res = getResources();
        String[] backupTypes = res
                .getStringArray(R.array.backup_type_list_entries);
        int i = Integer.parseInt(backupTypePreference.getSharedPreferences()
                .getString(SettingsSharedPreference.KEY_BACKUP_TYPE,
                        SettingsSharedPreference.BACKUP_TYPE_DEFAULT));
        String backupType = backupTypes[i];
        backupTypePreference.setSummary(backupType);

        Preference backupTimePreference = findPreference(SettingsSharedPreference.KEY_BACKUP_TIME);
        backupTimePreference.setSummary(backupTypePreference
                .getSharedPreferences().getString(
                        SettingsSharedPreference.KEY_BACKUP_TIME,
                        SettingsSharedPreference.BACKUP_TIME_DEFAULT));
    }

    /**
     * schedule auto backup operation when timer expires.
     */
    public void updateAutoBackupAlarmMessage() {
        AutoSyncManager autoSyncCalendar = new AutoSyncManager(getActivity());
        Preference backupTypePreference = findPreference(SettingsSharedPreference.KEY_BACKUP_TYPE);
        Preference preference = findPreference(SettingsSharedPreference.KEY_AUTO_BACKUP);
        boolean isAutoBackup = preference.getSharedPreferences().getBoolean(
                SettingsSharedPreference.KEY_AUTO_BACKUP, false);
        if (isAutoBackup) {
            // reset auto backup alarm message
            int backupType = Integer.parseInt(backupTypePreference.getSharedPreferences()
                    .getString(SettingsSharedPreference.KEY_BACKUP_TYPE,
                            SettingsSharedPreference.BACKUP_TYPE_DEFAULT));
            autoSyncCalendar.startAutoSync(backupType);
        } else {
            autoSyncCalendar.stopAutoSync();
        }
    }

    /**
     * Update BackupTypePreference enable state if value of backup_switch_key is
     * true, enable BackupTypePreference. if value of backup_switch_key is
     * false, disable BackupTypePreference.
     */
    public void updateBackupTypePreferenceStatus() {
        Preference preference = findPreference(SettingsSharedPreference.KEY_AUTO_BACKUP);
        boolean autoBackup = preference.getSharedPreferences().getBoolean(
                SettingsSharedPreference.KEY_AUTO_BACKUP, false);
        findPreference(SettingsSharedPreference.KEY_BACKUP_TYPE).setEnabled(autoBackup);
        findPreference(SettingsSharedPreference.KEY_BACKUP_TIME).setEnabled(autoBackup);
    }

    /**
     * Update BackupTypePreference info: Summary and enable.
     */
    public void updateBackupTypePreference() {
        updatePreferenceSummary();
        updateBackupTypePreferenceStatus();
    }

    /**
     * Initialize all Preferences.
     */
    public void initialPreferences() {
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        updateBackupTypePreference();
    }

    /**
     * Uninitialized all Preferences.
     */
    public void uninitialPreferences() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
