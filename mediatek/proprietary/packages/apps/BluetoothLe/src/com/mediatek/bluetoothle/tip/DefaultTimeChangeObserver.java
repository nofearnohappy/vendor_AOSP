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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetoothle.tip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.joda.time.DateTimeZone;

import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Provides a default time change observer to observe time change event.
 * The default time change events to observe are defined by TIP specification V10r00.
 */
public class DefaultTimeChangeObserver implements ITimeChangeObserver {
    private static final String TAG = "DefaultTimeChangeObserver";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private Context mContext = null;
    private CurrentTimeService mCts = null;
    private boolean mIsUpdatedFromRemote = false;
    private final Timer mTimer = new Timer("DefaultTimeChangeObserver Timer");
    private TimerTask mTimerTask;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            int reason = CurrentTimeService.ADJUST_REASON_NONE;
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                if (DBG) Log.d(TAG, "Receive Time Zone Change");
                reason = CurrentTimeService.ADJUST_REASON_CHANGE_OF_TIME_ZONE
                        | CurrentTimeService.ADJUST_REASON_MANUAL_TIME_UPDATE;
            } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                if (DBG) Log.d(TAG, "Receive Time Set");
                if (mIsUpdatedFromRemote) {
                    if (DBG) Log.d(TAG, "Time change caused by reference update");
                    mIsUpdatedFromRemote = false;
                    return;
                }
                reason = CurrentTimeService.ADJUST_REASON_MANUAL_TIME_UPDATE;
            }
            cancelDSTTimerTask();
            scheduleDSTTimerTask();
            final long currentTime = System.currentTimeMillis();
            mCts.onTimeChanged(currentTime, reason);
        }
    };;

    DefaultTimeChangeObserver(final Context ctx, final CurrentTimeService cts) {
        mContext = ctx;
        mCts = cts;
    }

    private boolean cancelDSTTimerTask() {
        if (VDBG) Log.v(TAG, "cancelDSTTimerTask");
        if (null != mTimerTask) {
            mTimerTask.cancel();
            return true;
        } else {
            Log.e(TAG, "mTimerTask is null!");
            return false;
        }
    }

    private void scheduleDSTTimerTask() {
        if (VDBG) Log.v(TAG, "scheduleDSTTimer");
        final TimeZone tz = TimeZone.getDefault();
        final boolean isUseDaylightTime = tz.useDaylightTime();
        if (!isUseDaylightTime) {
            if (DBG) Log.d(TAG, "Not use day light time");
            return;
        }
        final DateTimeZone dtz = DateTimeZone.forTimeZone(tz);
        final long ndst = dtz.nextTransition(System.currentTimeMillis());
        final Date date = new Date(ndst);
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (VDBG) Log.v(TAG, "TimeTask timeout, DST Offset");
                final long currentTime = System.currentTimeMillis();
                mCts.onTimeChanged(currentTime,
                        (CurrentTimeService.ADJUST_REASON_CHANGE_OF_DST
                                | CurrentTimeService.ADJUST_REASON_MANUAL_TIME_UPDATE));
            }
        };
        mTimer.schedule(mTimerTask, date);
    }

    @Override
    public final void init() {
        if (VDBG) Log.v(TAG, "init");
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        scheduleDSTTimerTask();
    }

    @Override
    public final void uninit() {
        if (VDBG) Log.v(TAG, "uninit");
        mContext.unregisterReceiver(mReceiver);
        cancelDSTTimerTask();
        mTimer.cancel();
    }

    @Override
    public final void onTimeUpdateFromTip(final long time, final int reason) {
        Log.d(TAG, "onTimeUpdateFromTip: time = " + time + ", reason = " + reason);
        switch (reason) {
            case CurrentTimeService.ADJUST_REASON_REFERENCE_TIME_UPDATE:
                mIsUpdatedFromRemote = true;
                mCts.onTimeChanged(time, reason);
                break;
            default:
                Log.e(TAG, "The reason is not supported");
                break;
        }
    }
}
