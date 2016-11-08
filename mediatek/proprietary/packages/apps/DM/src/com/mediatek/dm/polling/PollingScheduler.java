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

package com.mediatek.dm.polling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmService;
import com.mediatek.dm.bootcheck.BootService;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.util.FileLogger;
import com.mediatek.dm.xml.DmXMLParser;

import java.util.Date;

public class PollingScheduler {
    private static final String CLASS_TAG = DmConst.TAG.POLLING + "/Scheduler";

    private static final String PREF_NAME = "polling_sched";
    private static final String ALARM_TIME = "alarm_time";

    private static PollingScheduler sInstance;

    private Context mContext;
    private SharedPreferences mSettings;

    public static synchronized PollingScheduler getInstance(Context cxt) {
        if (sInstance == null) {
            sInstance = new PollingScheduler(cxt);
        }
        return sInstance;
    }

    /***
     * check if a polling alarm is time-up when boot completed.<br>
     * 1. alarm is not set ==> set an alarm after 2 weeks.<br>
     * 2. alarm is not time up ==> reset the alarm to alarm manager.<br>
     * 3. alarm time is over ==> should do polling now.<br>
     *
     * @return false for 1,2, true for 3
     */
    public boolean checkTimeup() {
        long alarmTime = mSettings.getLong(ALARM_TIME, 0L);
        if (alarmTime == 0L) {
            // set an alarm if it's never set.
            Log.d(CLASS_TAG, "alarm never set, set one now.");
            setNextAlarm();
            return false;
        }

        long currTime = System.currentTimeMillis();
        if (currTime < alarmTime) {
            // reset the alarm after reboot.
            Log.d(CLASS_TAG, "reset alarm after reboot.");
            setAlarm(alarmTime);
            return false;
        }

        Log.d(CLASS_TAG, "the saved alarm is time up!");
        return true;
    }

    /***
     * set a random alarm in around 10~17 days to alarm manager, and also save to a pref file.
     */
    public void setNextAlarm() {
        long alarmTime = getNextTime();

        // set to alarm manager
        setAlarm(alarmTime);

        // save to pref file
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(ALARM_TIME, alarmTime);
        editor.commit();
        Log.d(CLASS_TAG, "saved to preference:" + alarmTime);

        FileLogger.getInstance(mContext).logMsg("set alarm at " + new Date(alarmTime));
    }

    private void setAlarm(long time) {
        AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) {
            Log.e(CLASS_TAG, "get alarm manager failed");
            return;
        }

        Intent intent = new Intent();
        intent.setClass(mContext, BootService.class);

        intent.setAction(DmConst.IntentAction.ACTION_POLLING);
        Bundle bundle = new Bundle();
        bundle.putBoolean(DmService.INTENT_EXTRA_POLLING, true);
        intent.putExtras(bundle);

        PendingIntent pending = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(pending);

        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, time, pending);
        Log.d(CLASS_TAG, "set to alarm manager:" + time);
    }

    private long getNextTime() {
        int pollFrequency = Options.Polling.INTERVAL_BASE;

        if (DmService.isDmTreeReady()) {

            try {
                DmXMLParser parser = new DmXMLParser(DmConst.PathName.TREE_FILE_IN_DATA);
                String frequencyString = parser
                        .getValueByTreeUri(DmConst.NodeUri.FUMO_EXT_POLLFREQUENCY);

                Log.d(CLASS_TAG, "[getNextTime], get frequency value:" + frequencyString);

                pollFrequency = Integer.parseInt(frequencyString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        long curTime = System.currentTimeMillis();

        long alarmTime = curTime + pollFrequency * 24 * 3600 * 1000;

        // long curTime = System.currentTimeMillis();
        //
        // Random rd = new Random(curTime);
        // long alarmTime = curTime + Options.Polling.INTERVAL_BASE
        // + rd.nextInt(Options.Polling.INTERVAL_RANDOM);

        return alarmTime;
    }

    private PollingScheduler(Context cxt) {
        mContext = cxt;
        mSettings = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

}
