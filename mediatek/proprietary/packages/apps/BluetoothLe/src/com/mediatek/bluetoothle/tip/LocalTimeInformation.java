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

class LocalTimeInformation extends TipCharacteristic {
    private static final String TAG = "LocalTimeInformation";
    private static final boolean DBG = true;

    private static final int TIME_ZONE_NOT_KNOWN = -128;

    private static final int DST_OFFSET_NOT_KNOWN = 255;

    private static final int TIME_ZONE_OFFSET_BASE = 0;
    private static final int TIME_ZONE_FORMAT = BluetoothGattCharacteristic.FORMAT_SINT8;
    private static final int TIME_ZONE_OFFSET = TIME_ZONE_OFFSET_BASE;

    private static final int DST_OFFSET_OFFSET_BASE = TIME_ZONE_OFFSET
            + TipServerService.getTypeLen(TIME_ZONE_FORMAT);
    private static final int DST_OFFSET_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int DST_OFFSET_OFFSET = DST_OFFSET_OFFSET_BASE;

    private int mTimeZone = TIME_ZONE_NOT_KNOWN;
    private int mDSTOffset = DST_OFFSET_NOT_KNOWN;

    @Override
    boolean fromGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    boolean setGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        boolean ret = false;
        ret = setDSTOffset(characteristic, mDSTOffset);
        ret = setTimeZone(characteristic, mTimeZone);
        return ret;
    }

    int getTimeZone() {
        if (DBG) Log.d(TAG, "getTimeZone: mTimeZone = " + mTimeZone);
        return mTimeZone;
    }

    boolean setTimeZone(final int timeZone) {
        if (DBG) Log.d(TAG, "setCurrentState: state = " + timeZone);
        mTimeZone = timeZone;
        return true;
    }

    int getDSTOffset() {
        if (DBG) Log.d(TAG, "getDSTOffset: mDSTOffset = " + mDSTOffset);
        return mDSTOffset;
    }

    boolean setDSTOffset(final int offset) {
        if (DBG) Log.d(TAG, "setDSTOffset: offset = " + offset);
        mDSTOffset = offset;
        return true;
    }

    private boolean setTimeZone(final BluetoothGattCharacteristic characteristic,
            final int timeZone) {
        if (DBG) Log.d(TAG, "setTimeZone: timeZone = " + timeZone);
        final int convertedTz = convertTimeZone(timeZone);
        return characteristic.setValue(convertedTz, TIME_ZONE_FORMAT, TIME_ZONE_OFFSET);
    }

    private boolean setDSTOffset(final BluetoothGattCharacteristic characteristic,
            final int offset) {
        if (DBG) Log.d(TAG, "setDSTOffset: offset = " + offset);
        final int convertedDSTOffset = convertDSTOffset(offset);
        return characteristic.setValue(convertedDSTOffset, DST_OFFSET_FORMAT, DST_OFFSET_OFFSET);
    }

    private int convertTimeZone(final int timeZone) {
        final int tz = timeZone / 1000 / 60 / 15;
        if (DBG) Log.d(TAG, "convertTimeZone: converted time zone = " + tz);
        if ((tz < -48) || (tz > 56)) {
            Log.e(TAG, "Invalid time zone");
            return TIME_ZONE_NOT_KNOWN;
        }
        return tz;
    }

    private int convertDSTOffset(final int offset) {
        final int dstOffset = offset / 1000 / 60 / 15;
        if (DBG) Log.d(TAG, "convertDSTOffset: converted DST offset = " + dstOffset);
        if ((dstOffset < 0) || (dstOffset > 8)) {
            Log.e(TAG, "Invalid DST offset");
            return DST_OFFSET_NOT_KNOWN;
        }
        return dstOffset;
    }
}
