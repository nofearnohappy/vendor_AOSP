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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.rcs.contacts.networkcontacts.SettingsSharedPreference.BackupTime;

import java.util.Calendar;

/**
 * Contacts AutoSync Manager,remember to calculate the auto sync periodic.
 */
public class AutoSyncManager {
    private static final String TAG = "NetworkContacts::AutoSyncManager";
    private Context mContext;
    /**
     * Auto bakcup type.
     *
     */
    public static class AutoBackupType {
        public static final int IMMEDIATELY = 0;
        public static final int DAILY = 1;
        public static final int WEEKLY = 2;
        public static final int MONTHLY = 3;
    }

    /**
     * @param context context
     */
    public AutoSyncManager(Context context) {
        mContext = context;
    }
    /**
     * get next date by Monthly backup type.
     * @return Calendar
     */
    private Calendar getNextMonthlyDate(BackupTime time) {
        Calendar c = Calendar.getInstance();
        if ((c.get(Calendar.DAY_OF_MONTH)) == 1
                && (c.get(Calendar.HOUR_OF_DAY) < time.mHour)) {
            Log.d(TAG, "today is first day of month,before AM 9:00");
        } else {
            c.set(Calendar.DATE, 1);
            c.add(Calendar.MONTH, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, time.mHour);
        c.set(Calendar.MINUTE, time.mMinute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /**
     * get next date by Weekly backup type.
     *
     * @return Calendar
     */
    private Calendar getNextWeeklyDate(BackupTime time) {
        Calendar c = Calendar.getInstance();
        if ((c.get(Calendar.DAY_OF_WEEK)) == Calendar.MONDAY
                && (c.get(Calendar.HOUR_OF_DAY) < time.mHour)) {
            Log.d(TAG, "today is first day of WEEK,before AM 9:00");
        } else {
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            c.add(Calendar.DATE, 7);
        }
        c.set(Calendar.HOUR_OF_DAY, time.mHour);
        c.set(Calendar.MINUTE, time.mMinute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
    /**
     * get next date by Daily backup type.
     * @return Calendar
     */
    private Calendar getNextDailyDate(BackupTime time) {
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) < time.mHour) {
            Log.d(TAG, "Time is before AM 9:00 ");
        } else {
            c.add(Calendar.DATE, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, time.mHour);
        c.set(Calendar.MINUTE, time.mMinute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /**
     * cancel auto backup Alarm message.
     * @return void
     */
    private void cancelAlarm() {
        Log.d(TAG, "cancelAlarm +++ ");
        AlarmManager alarm = (AlarmManager) mContext
                .getSystemService(android.content.Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent();
        alarmIntent.setAction(Const.Action.AUTO_BACKUP);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.cancel(sender);
        Log.d(TAG, "cancelAlarm --- ");
    }
    /**
     * start sync service,register contacts observer.
     * Service will live in foreground forever in this case
     * @return void
     */
    private void registerContactsObserver() {
        Intent intent = new Intent(Const.Action.REGISTER_CONTACTS_OBSERVER);
        intent.setClass(mContext, SyncService.class);
        mContext.startService(intent);
    }
    /**
     * start sync service,unregister contacts observer.
     * @return void
     */
    private void unregisterContactsObserver() {
        Intent intent = new Intent(Const.Action.UNREGISTER_CONTACTS_OBSERVER);
        intent.setClass(mContext, SyncService.class);
        mContext.startService(intent);
    }

    /**
     * get next date by Daily backup type.
     * @param autoBackupType immediately:0;Daily:1;weekly:2;monthly:3;
     *
     */
    public void startAutoSync(int autoBackupType) {
        Calendar nextCalendaralendar;
        AlarmManager alarm = (AlarmManager) mContext
                .getSystemService(android.content.Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent();
        alarmIntent.setAction(Const.Action.AUTO_BACKUP);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        stopAutoSync();

        //get the time from preference
        BackupTime time = new SettingsSharedPreference(mContext).getAutoBackupTime();
        switch (autoBackupType) {
        case AutoBackupType.IMMEDIATELY: //immediately
            //start service, register contacts observer
            registerContactsObserver();
            break;
        case AutoBackupType.DAILY: //Daily
            nextCalendaralendar = getNextDailyDate(time);
            alarm.set(AlarmManager.RTC_WAKEUP, nextCalendaralendar.getTimeInMillis(), sender);
            break;
        case AutoBackupType.WEEKLY: //weekly
            nextCalendaralendar = getNextWeeklyDate(time);
            alarm.set(AlarmManager.RTC_WAKEUP, nextCalendaralendar.getTimeInMillis(), sender);
            break;
        case AutoBackupType.MONTHLY: //monthly
            nextCalendaralendar = getNextMonthlyDate(time);
            alarm.set(AlarmManager.RTC_WAKEUP, nextCalendaralendar.getTimeInMillis(), sender);
            break;
        default:
            break;
        }
    }

    /**
     * Stop auto sync mode.
     * @param
     */
    public void stopAutoSync() {
        cancelAlarm();
        unregisterContactsObserver();
    }
}
