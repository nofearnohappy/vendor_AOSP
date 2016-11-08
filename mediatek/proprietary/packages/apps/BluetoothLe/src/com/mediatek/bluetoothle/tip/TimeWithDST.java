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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

class TimeWithDST extends TipCharacteristic {
    private static final String TAG = "TimeWithDST";
    private static final boolean DBG = true;

    static final int DATE_TIME_OFFSET_BASE = 0;
    static final int DATE_TIME_YEAR_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT16;
    static final int DATE_TIME_YEAR_OFFSET = DATE_TIME_OFFSET_BASE;
    static final int DATE_TIME_MONTH_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int DATE_TIME_MONTH_OFFSET = DATE_TIME_YEAR_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_YEAR_FORMAT);
    static final int DATE_TIME_DAY_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int DATE_TIME_DAY_OFFSET = DATE_TIME_MONTH_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_MONTH_FORMAT);
    static final int DATE_TIME_HOUR_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int DATE_TIME_HOUR_OFFSET = DATE_TIME_DAY_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_DAY_FORMAT);
    static final int DATE_TIME_MINUTE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int DATE_TIME_MINUTE_OFFSET = DATE_TIME_HOUR_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_HOUR_FORMAT);
    static final int DATE_TIME_SECOND_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int DATE_TIME_SECOND_OFFSET = DATE_TIME_MINUTE_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_MINUTE_FORMAT);

    private static final int DST_OFFSET_OFFSET_BASE = DATE_TIME_SECOND_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_SECOND_FORMAT);
    private static final int DST_OFFSET_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final int DST_OFFSET_OFFSET = DST_OFFSET_OFFSET_BASE;

    private int mDSTOffset = 0;
    private DateTime mDateTime = null;

    int getDTSOffset() {
        if (DBG) Log.d(TAG, "getDTSOffset: mDSTOffset = " + mDSTOffset);
        return mDSTOffset;
    }

    boolean setDSTOffset(final int dstOffset) {
        if (DBG) Log.d(TAG, "setDSTOffset: dstOffset = " + dstOffset);
        mDSTOffset = dstOffset;
        return true;
    }

    DateTime getDateTime() {
        if (DBG) Log.d(TAG, "getDateTime: mDateTime = " + mDateTime);
        return mDateTime;
    }

    boolean setDateTime(final DateTime dateTime) {
        if (DBG) Log.d(TAG, "setDSTOffset: dstOffset = " + dateTime);
        mDateTime = dateTime;
        return true;
    }

    @Override
    boolean fromGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        mDateTime = getDateTime(characteristic);
        mDSTOffset = getDSTOffset(characteristic);
        return false;
    }

    @Override
    boolean setGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        setDSTOffset(characteristic, mDSTOffset);
        setDateTime(characteristic, mDateTime);
        return true;
    }

    private boolean setDSTOffset(final BluetoothGattCharacteristic characteristic,
            final int dstOffset) {
        final int offsetSecond = dstOffset / 1000;
        int offset = NextDSTChangeService.DST_OFFSET_NOT_KNOWN;
        switch (offsetSecond) {
            case 0:
                offset = NextDSTChangeService.DST_OFFSET_STANDARD;
                break;
            case 1800:
                offset = NextDSTChangeService.DST_OFFSET_HALF_AN_HOUR_DAYLIGHT;
                break;
            case 3600:
                offset = NextDSTChangeService.DST_OFFSET_DAYLIGHT;
                break;
            case 7200:
                offset = NextDSTChangeService.DST_OFFSET_DOUBLE_DAYLIGHT;
                break;
            default:
                offset = NextDSTChangeService.DST_OFFSET_NOT_KNOWN;
                break;
        }
        return characteristic.setValue(offset, DST_OFFSET_FORMAT, DST_OFFSET_OFFSET);
    }

    private boolean setDateTime(final BluetoothGattCharacteristic characteristic,
            final DateTime dt) {
        boolean ret = false;
        ret = setSecond(characteristic, dt.getSecondOfMinute());
        ret = setMinute(characteristic, dt.getMinuteOfHour());
        ret = setHour(characteristic, dt.getHourOfDay());
        ret = setDay(characteristic, dt.getDayOfMonth());
        ret = setMonth(characteristic, dt.getMonthOfYear());
        ret = setYear(characteristic, dt.getYear());
        return ret;
    }

    private boolean setYear(final BluetoothGattCharacteristic characteristic, final int year) {
        return characteristic.setValue(year, DATE_TIME_YEAR_FORMAT, DATE_TIME_YEAR_OFFSET);
    }

    private boolean setMonth(final BluetoothGattCharacteristic characteristic, final int month) {
        return characteristic.setValue(month, DATE_TIME_MONTH_FORMAT, DATE_TIME_MONTH_OFFSET);
    }

    private boolean setDay(final BluetoothGattCharacteristic characteristic, final int day) {
        return characteristic.setValue(day, DATE_TIME_DAY_FORMAT, DATE_TIME_DAY_OFFSET);
    }

    private boolean setHour(final BluetoothGattCharacteristic characteristic, final int hour) {
        return characteristic.setValue(hour, DATE_TIME_HOUR_FORMAT, DATE_TIME_HOUR_OFFSET);
    }

    private boolean setMinute(final BluetoothGattCharacteristic characteristic, final int minute) {
        return characteristic.setValue(minute, DATE_TIME_MINUTE_FORMAT, DATE_TIME_MINUTE_OFFSET);
    }

    private boolean setSecond(final BluetoothGattCharacteristic characteristic, final int second) {
        return characteristic.setValue(second, DATE_TIME_SECOND_FORMAT, DATE_TIME_SECOND_OFFSET);
    }

    private DateTime getDateTime(final BluetoothGattCharacteristic characteristic) {
        final int second = getSecond(characteristic);
        final int minute = getMinute(characteristic);
        final int hour = getHour(characteristic);
        final int day = getDay(characteristic);
        final int month = getMonth(characteristic);
        final int year = getYear(characteristic);

        final TimeZone tz = TimeZone.getDefault();
        final DateTimeZone jodaForTz = DateTimeZone.forTimeZone(tz);
        final DateTime dt = new DateTime(year, month, day, hour, minute, second, jodaForTz);
        return dt;
    }

    private int getYear(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DATE_TIME_YEAR_FORMAT,
                    DATE_TIME_YEAR_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }

    private int getMonth(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DATE_TIME_MONTH_FORMAT,
                    DATE_TIME_MONTH_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }

    private int getDay(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DATE_TIME_DAY_FORMAT,
                    DATE_TIME_DAY_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }

    private int getHour(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DATE_TIME_HOUR_FORMAT,
                    DATE_TIME_HOUR_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }

    private int getMinute(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DATE_TIME_MINUTE_FORMAT,
                    DATE_TIME_MINUTE_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }

    private int getSecond(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DATE_TIME_SECOND_FORMAT,
                    DATE_TIME_SECOND_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }

    private int getDSTOffset(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DST_OFFSET_FORMAT,
                    DST_OFFSET_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return 0;
    }
}
