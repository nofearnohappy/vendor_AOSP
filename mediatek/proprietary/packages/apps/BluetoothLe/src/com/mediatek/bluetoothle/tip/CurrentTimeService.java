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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mediatek.bluetooth.BleGattUuid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;

import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

class CurrentTimeService extends TipService {
    private static final String TAG = "CurrentTimeService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    static final int TIME_SOURCE_UNKNOWN = 0;
    static final int TIME_SOURCE_NETWORK_TIME_PROTOCOL = 1;
    static final int TIME_SOURCE_GPS = 2;
    static final int TIME_SOURCE_RADIO_TIME_SIGNAL = 3;
    static final int TIME_SOURCE_MANUAL = 4;
    static final int TIME_SOURCE_ATOMIC_CLOCK = 5;
    static final int TIME_SOURCE_CELLUALR_NETWORK = 6;

    static final int ACCURACY_OUT_OF_RANGE = 254;
    static final int ACCURACY_UNKNOWN = 255;

    static final int TIME_SINCE_UPDATE_NOT_UPDATED = 0;
    static final int TIME_SINCE_UPDATE_255_OR_MORE_DAYS = 255;

    static final int ADJUST_REASON_NONE = 0;
    static final int ADJUST_REASON_MANUAL_TIME_UPDATE = 1;
    static final int ADJUST_REASON_REFERENCE_TIME_UPDATE = 2;
    static final int ADJUST_REASON_CHANGE_OF_TIME_ZONE = 4;
    static final int ADJUST_REASON_CHANGE_OF_DST = 8;

    static final int DAY_OF_WEEK_UNKNOWN = 0;

    static final int DATE_TIME_UNKNOWN = 0;

    private static final int TIME_CHANGE_OBSERVER_DEFAULT = 0;
    private static final int TIME_UPDATE_DIFFERENCE = 1; // In minute

    private static final int MSG_ON_TIME_UPDATED = 0;
    private static final int MSG_ON_TIME_CHANGED = 1;
    private static final int MSG_NOTIFY_TIME = 2;

    private final TipServerService mTipService;
    private final ITimeChangeObserver mTimeChangeObserver;
    private final Handler mHandler;
    private final int mDrift = 0; // One day in mill-second
    private long mUpdatedTime = TIME_SINCE_UPDATE_NOT_UPDATED;
    private final int mAdjustReason = ADJUST_REASON_NONE;

    private final CurrentTime mCurrentTime = new CurrentTime();
    private final LocalTimeInformation mLocalTimeInfoChar = new LocalTimeInformation();
    private final ReferenceTimeInformation mReferenceTimeInfo = new ReferenceTimeInformation();

    private final ClientCharacteristicConfig mCharConfig = new ClientCharacteristicConfig();

    CurrentTimeService(final TipServerService tipService) {
        if (VDBG) Log.v(TAG, "Create CurrentTimeService");
        if (DBG) Log.d(TAG, "tipService = " + tipService);
        mTipService = tipService;
        mHandler = createHandler();
        mTimeChangeObserver = makeTimeChangeObserver(TIME_CHANGE_OBSERVER_DEFAULT);
        if (null != mTimeChangeObserver) {
            mTimeChangeObserver.init();
        }
    }

    @Override
    void onReadCharacteristic(final BluetoothGattCharacteristic characteristic,
            final BluetoothDevice device) {
        final UUID charUuid = characteristic.getUuid();
        if (DBG) Log.d(TAG, "onReadCharacteristic: charUuid = " + charUuid);
        if (charUuid.equals(BleGattUuid.Char.CURRENT_TIME)) {
            onCurrentTimeRead(characteristic);
        } else if (charUuid.equals(BleGattUuid.Char.LOCAL_TIME_INFO)) {
            onLocalTimeInfoRead(characteristic);
        } else if (charUuid.equals(BleGattUuid.Char.REFERENCE_TIME_INFO)) {
            onReferenceTimeInfoRead(characteristic);
        } else {
            Log.e(TAG, "Unsupported Characteristic: charUuid = " + charUuid);
        }
    }

    @Override
    void onReadDescriptor(final BluetoothGattDescriptor descriptor, final BluetoothDevice device) {
        final UUID descUuid = descriptor.getUuid();
        if (DBG) Log.d(TAG, "onReadDescriptor: descUuid = " + descUuid);
    }

    @Override
    void onWriteDescriptor(final BluetoothGattDescriptor descriptor, final BluetoothDevice device,
            final byte[] value) {
        final UUID descUuid = descriptor.getUuid();
        if (descUuid.equals(BleGattUuid.Desc.CLIENT_CHAR_CONFIG)) {
            boolean isNotify = false;
            if (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[0]
                    == (value[0] & BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[0])) {
                isNotify = true;
            }
            mCharConfig.setNotify(isNotify);
            mTipService.onNotifyChanged(device, isNotify);
        }
    }

    @Override
    void uninit() {
        mTimeChangeObserver.uninit();
    }

    void onCurrentTimeRead(final BluetoothGattCharacteristic charactertistic) {
        if (VDBG) Log.v(TAG, "onCurrentTimeRead");
        synchronized (mCurrentTime) {
            // Get time information
            final DateTime dt = DateTime.now(DateTimeZone.forTimeZone(TimeZone.getDefault()));
            // Calculate fraction 236
            final int fraction256 = dt.getMillisOfSecond() * 256 / 1000;
            // Get day of week
            final int daysOfWeek = dt.getDayOfWeek();
            // Get the time now
            final long dateTime = dt.getMillis();

            if (DBG) Log.d(TAG, "mAdjustReason = " + mAdjustReason + ", fraction256 = "
                    + fraction256 + ", daysOfWeek = " + daysOfWeek + ", date = " + dateTime);
            if (DBG) Log.d(TAG, "Time = "
                    + dt.toString("yyyy-MM-dd E HH:mm:ss.SSS ZZZZ Z", Locale.ENGLISH));
            mCurrentTime.setAdjustReason(mAdjustReason);
            mCurrentTime.setFraction256(fraction256);
            mCurrentTime.setDayOfWeek(daysOfWeek);
            mCurrentTime.setDateTime(dt);
            mCurrentTime.setGattCharacteristic(charactertistic);
        }
    }

    void onLocalTimeInfoRead(final BluetoothGattCharacteristic charactertistic) {
        if (VDBG) Log.d(TAG, "onLocalTimeInfoRead");
        // Get time information
        final TimeZone tz = TimeZone.getDefault();
        final int timeZone = tz.getRawOffset();
        final int dstOffset = tz.getDSTSavings();
        if (DBG) Log.d(TAG, "timeZone = " + timeZone + ", dstOffset = " + dstOffset);
        mLocalTimeInfoChar.setTimeZone(timeZone);
        mLocalTimeInfoChar.setDSTOffset(dstOffset);
        mLocalTimeInfoChar.setGattCharacteristic(charactertistic);
    }

    void onReferenceTimeInfoRead(final BluetoothGattCharacteristic charactertistic) {
        if (DBG) Log.d(TAG, "onReferenceTimeInfoRead: mUpdatedTime = " + mUpdatedTime);
        int timeSource = TIME_SOURCE_UNKNOWN;
        int accuracy = ACCURACY_OUT_OF_RANGE;
        int days = TIME_SINCE_UPDATE_255_OR_MORE_DAYS;
        int hours = TIME_SINCE_UPDATE_255_OR_MORE_DAYS;

        // Calculate the time since update
        if (TIME_SINCE_UPDATE_NOT_UPDATED != mUpdatedTime) {
            final LocalDateTime ldtUpdate = new LocalDateTime(mUpdatedTime);
            final LocalDateTime ldtNow = new LocalDateTime(System.currentTimeMillis());
            days = Days.daysBetween(ldtUpdate, ldtNow).getDays();
            hours = Hours.hoursBetween(ldtUpdate, ldtNow).getHours();
            // Calculate the accuracy
            accuracy = ((mDrift / 24) * hours) / 125;
            if (ACCURACY_OUT_OF_RANGE <= accuracy) {
                accuracy = ACCURACY_OUT_OF_RANGE;
            }
            if (TIME_SINCE_UPDATE_255_OR_MORE_DAYS <= days) {
                days = TIME_SINCE_UPDATE_255_OR_MORE_DAYS;
            }
            if (TIME_SINCE_UPDATE_255_OR_MORE_DAYS <= hours) {
                hours = TIME_SINCE_UPDATE_255_OR_MORE_DAYS;
            }
        }

        // Only Support NTP, and the drift information is not known
        timeSource = TIME_SOURCE_NETWORK_TIME_PROTOCOL;
        accuracy = ACCURACY_UNKNOWN;

        if (DBG) Log.d(TAG, "timeSource = " + timeSource + ", accuracy = " + accuracy
                + ", days = " + days + ", hours = " + hours);
        mReferenceTimeInfo.setTimeSource(timeSource);
        mReferenceTimeInfo.setAccuracy(accuracy);
        mReferenceTimeInfo.setDaysSinceUpdate(days);
        mReferenceTimeInfo.setHoursSinceUpdate(hours);
        mReferenceTimeInfo.setGattCharacteristic(charactertistic);
    }

    Handler createHandler() {
        Log.d(TAG, "createHandler");
        return new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                if (DBG) Log.d(TAG, "Message: " + msg.what);
                final long time = ((Long) msg.obj).longValue();
                switch (msg.what) {
                    case MSG_ON_TIME_UPDATED:
                        if (DBG) Log.d(TAG, "MSG_ON_TIME_UPDATED");
                        processOnTimeUpdated(time);
                        break;
                    case MSG_ON_TIME_CHANGED:
                        if (DBG) Log.d(TAG, "MSG_ON_TIME_CHANGED");
                        final int adjustReason = msg.arg1;
                        processOnTimeChanged(time, adjustReason);
                        break;
                    case MSG_NOTIFY_TIME:
                        if (DBG) Log.d(TAG, "MSG_NOTIFY_TIME");
                        processNotifyTime(time);
                        break;
                    default:
                        Log.e(TAG, "Unsupported Handler");
                        break;
                }
            }
        };
    }

    void onTimeUpdated(final long time) {
        if (DBG) Log.d(TAG, "onTimeUpdated: time = " + time);
        final Message msg = new Message();
        msg.what = MSG_ON_TIME_UPDATED;
        msg.obj = Long.valueOf(time);
        mHandler.sendMessage(msg);
    }

    private void processOnTimeUpdated(final long time) {
        if (DBG) Log.d(TAG, "processOnTimeUpdated: time = " + time);
        mTimeChangeObserver.onTimeUpdateFromTip(time, ADJUST_REASON_REFERENCE_TIME_UPDATE);
    }

    void onTimeChanged(final long time, final int adjustReason) {
        if (DBG) Log.d(TAG, "onTimeChanged: time = " + time + ", adjustReason = " + adjustReason);
        final Message msg = new Message();
        msg.what = MSG_ON_TIME_CHANGED;
        msg.arg1 = adjustReason;
        msg.obj = Long.valueOf(time);
        mHandler.sendMessage(msg);
    }

    private void processOnTimeChanged(final long time, final int adjustReason) {
        if (DBG) Log.d(TAG, "processOnTimeChanged: time = " + time
                + ", adjustReason = " + adjustReason);
        final long lastUpdatedTime = mUpdatedTime;
        if (ADJUST_REASON_REFERENCE_TIME_UPDATE != adjustReason) {
            mUpdatedTime = TIME_SINCE_UPDATE_NOT_UPDATED;
        } else {
            /// M: ALPS01946034: Fix TIP reference time update issue @{
            Log.d(TAG, "mUpdatedTime = " + mUpdatedTime);
            Log.d(TAG, "time = " + time);
            Log.d(TAG, "lastUpdatedTime = " + lastUpdatedTime);
            /// @}
            mUpdatedTime = time;
            // The update behavior defined by Spec.
            if (TIME_SINCE_UPDATE_NOT_UPDATED != lastUpdatedTime) {
                /// M: ALPS01946034: Fix TIP reference time update issue
                final LocalDateTime lastUpdate = new LocalDateTime(lastUpdatedTime);
                final LocalDateTime currentUpdate = new LocalDateTime(time);
                final int minutes = Minutes.minutesBetween(lastUpdate, currentUpdate).getMinutes();
                if (TIME_UPDATE_DIFFERENCE > minutes) {
                    if (DBG) Log.d(TAG, "Update in one minute, minutes = " + minutes);
                    return;
                }
            }
        }
        notifyTime(time, adjustReason);
    }

    void notifyTime(final long time, final int adjustReason) {
        if (DBG) Log.d(TAG, "notifyTime: time = " + time + ", adjustReason = " + adjustReason);
        synchronized (mCurrentTime) {
            final BluetoothGattCharacteristic currentTimeChar = mTipService
                    .getCharacteristic(BleGattUuid.Char.CURRENT_TIME);
            if (null == currentTimeChar) {
                Log.e(TAG, "currentTimeChar is null");
                return;
            }
            final DateTime dt = new DateTime(time,
                    DateTimeZone.forTimeZone(TimeZone.getDefault()));
            // Calculate fraction 236
            final int fraction256 = dt.getMillisOfSecond() * 256 / 1000;
            // Get day of week
            final int daysOfWeek = dt.getDayOfWeek();
            if (DBG) Log.d(TAG, "adjustReason = " + adjustReason + ", fraction256 = " + fraction256
                    + ", daysOfWeek = " + daysOfWeek + ", time = " + time);
            if (DBG) Log.d(TAG, "Time = " + dt.toString("yyyy-MM-dd E HH:mm:ss.SSS ZZZZ Z",
                    Locale.ENGLISH));
            mCurrentTime.setAdjustReason(adjustReason);
            mCurrentTime.setFraction256(fraction256);
            mCurrentTime.setDayOfWeek(daysOfWeek);
            mCurrentTime.setDateTime(dt);
            mCurrentTime.setGattCharacteristic(currentTimeChar);
            mTipService.notifyCharacteristic(currentTimeChar);
        }
    }

    // For public usage
    void notifyTime(final long time) {
        if (DBG) Log.d(TAG, "notifyTime: time = " + time);
        final Message msg = new Message();
        msg.what = MSG_NOTIFY_TIME;
        msg.obj = Long.valueOf(time);
        mHandler.sendMessage(msg);
    }

    private void processNotifyTime(final long time) {
        if (DBG) Log.d(TAG, "processNotifyTime: time = " + time);
        notifyTime(time, ADJUST_REASON_MANUAL_TIME_UPDATE);
    }

    private ITimeChangeObserver makeTimeChangeObserver(final int type) {
        if (DBG) Log.d(TAG, "ITimeChangeObserver: type = " + type);
        ITimeChangeObserver observer = null;
        switch (type) {
            case TIME_CHANGE_OBSERVER_DEFAULT:
                observer = new DefaultTimeChangeObserver(mTipService, this);
                break;
            default:
                Log.e(TAG, "Unsupported type");
                break;
        }
        return observer;
    }
}
