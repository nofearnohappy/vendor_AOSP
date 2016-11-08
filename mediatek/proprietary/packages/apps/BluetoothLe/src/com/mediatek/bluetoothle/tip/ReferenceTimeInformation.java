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

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

class ReferenceTimeInformation extends TipCharacteristic {
    private static final String TAG = "ReferenceTimeInformation";
    private static final boolean DBG = true;

    private static final int TIME_SOURCE_OFFSET_BASE = 0;
    private static final int TIME_SOURCE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int TIME_SOURCE_OFFSET = TIME_SOURCE_OFFSET_BASE;

    private static final int ACCURACY_OFFSET_BASE = TIME_SOURCE_OFFSET
            + TipServerService.getTypeLen(TIME_SOURCE_FORMAT);
    private static final int ACCURACY_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int ACCURACY_OFFSET = ACCURACY_OFFSET_BASE;

    private static final int DAYS_SINCE_UPDATE_OFFSET_BASE = ACCURACY_OFFSET
            + TipServerService.getTypeLen(ACCURACY_FORMAT);
    private static final int DAYS_SINCE_UPDATE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int DAYS_SINCE_UPDATE_OFFSET = DAYS_SINCE_UPDATE_OFFSET_BASE;

    private static final int HOURS_SINCE_UPDATE_OFFSET_BASE = DAYS_SINCE_UPDATE_OFFSET
            + TipServerService.getTypeLen(DAYS_SINCE_UPDATE_FORMAT);
    private static final int HOURS_SINCE_UPDATE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int HOURS_SINCE_UPDATE_OFFSET = HOURS_SINCE_UPDATE_OFFSET_BASE;

    private int mTimeSource = CurrentTimeService.TIME_SOURCE_UNKNOWN;
    private int mAccuracy = CurrentTimeService.ACCURACY_UNKNOWN;
    private int mDaysSinceUpdate = CurrentTimeService.TIME_SINCE_UPDATE_255_OR_MORE_DAYS;
    private int mHoursSinceUpdate = CurrentTimeService.TIME_SINCE_UPDATE_255_OR_MORE_DAYS;

    @Override
    boolean fromGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    boolean setGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        boolean ret = false;
        ret = setHoursSinceUpdate(characteristic, mHoursSinceUpdate);
        ret = setDaysSinceUpdate(characteristic, mDaysSinceUpdate);
        ret = setAccuracy(characteristic, mAccuracy);
        ret = setTimeSource(characteristic, mTimeSource);
        return ret;
    }

    int getTimeSource() {
        if (DBG) Log.d(TAG, "getTimeSource: mTimeSource = " + mTimeSource);
        return mTimeSource;
    }

    boolean setTimeSource(final int ts) {
        if (DBG) Log.d(TAG, "setTimeSource: state = " + ts);
        mTimeSource = ts;
        return true;
    }

    int getAccuracy() {
        if (DBG) Log.d(TAG, "getAccuracy: mAccuracy = " + mAccuracy);
        return mAccuracy;
    }

    boolean setAccuracy(final int accuracy) {
        if (DBG) Log.d(TAG, "setAccuracy: accuracy = " + accuracy);
        mAccuracy = accuracy;
        return true;
    }

    int getDaysSinceUpdate() {
        if (DBG) Log.d(TAG, "getDaysSinceUpdate: mDaysSinceUpdate = " + mDaysSinceUpdate);
        return mDaysSinceUpdate;
    }

    boolean setDaysSinceUpdate(final int daysSinceUpdate) {
        if (DBG) Log.d(TAG, "setDaysSinceUpdate: daysSinceUpdate = " + daysSinceUpdate);
        mDaysSinceUpdate = daysSinceUpdate;
        return true;
    }

    int getHoursSinceUpdate() {
        if (DBG) Log.d(TAG, "getHoursSinceUpdate: mHoursSinceUpdate = " + mHoursSinceUpdate);
        return mHoursSinceUpdate;
    }

    boolean setHoursSinceUpdate(final int hoursSinceUpdate) {
        if (DBG) Log.d(TAG, "setHoursSinceUpdate: hoursSinceUpdate = " + hoursSinceUpdate);
        mHoursSinceUpdate = hoursSinceUpdate;
        return true;
    }

    private boolean setTimeSource(final BluetoothGattCharacteristic characteristic, final int ts) {
        if (DBG) Log.d(TAG, "setTimeSource: ts = " + ts);
        return characteristic.setValue(ts, TIME_SOURCE_FORMAT, TIME_SOURCE_OFFSET);
    }

    private boolean setAccuracy(final BluetoothGattCharacteristic characteristic,
            final int accuracy) {
        if (DBG) Log.d(TAG, "setAccuracy: accuracy = " + accuracy);
        return characteristic.setValue(accuracy, ACCURACY_FORMAT, ACCURACY_OFFSET);
    }

    private boolean setDaysSinceUpdate(final BluetoothGattCharacteristic characteristic,
            final int days) {
        if (DBG) Log.d(TAG, "setDaysSinceUpdate: days = " + days);
        return characteristic.setValue(days, DAYS_SINCE_UPDATE_FORMAT, DAYS_SINCE_UPDATE_OFFSET);
    }

    private boolean setHoursSinceUpdate(final BluetoothGattCharacteristic characteristic,
            final int hours) {
        if (DBG) Log.d(TAG, "setHoursSinceUpdate: hours = " + hours);
        return characteristic.setValue(hours, HOURS_SINCE_UPDATE_FORMAT,
                HOURS_SINCE_UPDATE_OFFSET);
    }
}
