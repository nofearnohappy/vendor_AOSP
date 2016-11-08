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

package com.mediatek.rcs.contacts.networkcontacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * get value of SharedPreference from Contacts Backup/Restore settings.
 *
 */
public class SettingsSharedPreference {
    /**
     * setting keys
     */
    /* boolean: if wifi only is set */
    public static final String KEY_WIFI_ONLY = "wifiOnly";
    /*boolean: if auto backup is set*/
    public static final String KEY_AUTO_BACKUP = "autoBackup";
    /*int: auto backup type*/
    public static final String KEY_BACKUP_TYPE = "backupType";

    /*String: auto backup time, hh:mm*/
    public static final String KEY_BACKUP_TIME = "backupTime";

    public static final String BACKUP_TYPE_DEFAULT = "2";   //weekly
    public static final String BACKUP_TIME_DEFAULT = "09:00";
    public static final int BACKUP_TYPE_IMMEDIATELY = 0;
    public static final int BACKUP_TYPE_DAILY = 1;
    public static final int BACKUP_TYPE_WEEKLY = 2;
    public static final int BACKUP_TYPE_MONTHLY = 3;

    private SharedPreferences mSharedPreferences;

    /**
     * Auto backup time.
     *
     */
    public static class BackupTime {
        public int mHour = 0;
        public int mMinute = 0;

        /**
         * @param h hour
         * @param m minutes
         */
        public BackupTime(int h, int m) {
            mHour = h;
            mMinute = m;
        }
    }

    /**
     * Constructor.
     * @param context app context
     */
    public SettingsSharedPreference(Context context) {
        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
    }

    /**
     * get the switch value of WiFI backup Only, default value is true.
     *
     * @return {@code true}on,{@code false}off
     */
    public Boolean isWifiBackupOnly() {
        return mSharedPreferences.getBoolean(KEY_WIFI_ONLY, true);
    }

    /**
     * get the switch value of Auto backup, default value is false.
     *
     * @return {@code true}on,{@code false}off
     */
    public Boolean isAutoBackup() {
        return mSharedPreferences.getBoolean(KEY_AUTO_BACKUP, false);
    }

    /**
     * get the type of Auto backup.
     *
     * @return autoBackupType, immediately:0;Daily:1;weekly:2;monthly:3;
     */
    public int getAutoBackupType() {
        int i = Integer.parseInt(mSharedPreferences.getString(
                KEY_BACKUP_TYPE,
                BACKUP_TYPE_DEFAULT));
        return i;
    }

    /**
     * @return backup time.
     */
    public BackupTime getAutoBackupTime() {
        String timestr = mSharedPreferences.getString(KEY_BACKUP_TIME, BACKUP_TIME_DEFAULT);
        String[] times = timestr.split(":");

        int h = Integer.parseInt(times[0]);
        int m = Integer.parseInt(times[1]);
        return new BackupTime(h, m);
    }
}
