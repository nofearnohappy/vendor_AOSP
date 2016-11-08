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

class CurrentTime extends TipCharacteristic {
    private static final String TAG = "CurrentTime";
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

    static final int DAY_OF_WEEK_OFFSET_BASE = DATE_TIME_SECOND_OFFSET
            + TipServerService.getTypeLen(DATE_TIME_SECOND_FORMAT);
    static final int DAY_OF_WEEK_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int DAY_OF_WEEK_OFFSET = DAY_OF_WEEK_OFFSET_BASE;

    static final int FRACTIONS256_OFFSET_BASE = DAY_OF_WEEK_OFFSET
            + TipServerService.getTypeLen(DAY_OF_WEEK_FORMAT);
    static final int FRACTIONS256_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int FRACTIONS256_OFFSET = FRACTIONS256_OFFSET_BASE;

    static final int ADJUST_REASON_OFFSET_BASE = FRACTIONS256_OFFSET
            + TipServerService.getTypeLen(FRACTIONS256_FORMAT);
    static final int ADJUST_REASON_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    static final int ADJUST_REASON_OFFSET = ADJUST_REASON_OFFSET_BASE;

    private DateTime mDateTime = null;
    private int mDayOfWeek = CurrentTimeService.DAY_OF_WEEK_UNKNOWN;
    private int mFraction256 = 0;
    private int mAdjustReason = CurrentTimeService.ADJUST_REASON_NONE;

    @Override
    boolean fromGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        mDateTime = getDateTime(characteristic);
        mDayOfWeek = getDayOfWeek(characteristic);
        mFraction256 = getFractions256(characteristic);
        mAdjustReason = getAdjustReason(characteristic);
        return true;
    }

    @Override
    boolean setGattCharacteristic(final BluetoothGattCharacteristic characteristic) {
        boolean ret = false;
        ret = setAdjustReason(characteristic, mAdjustReason);
        ret = setFractions256(characteristic, mFraction256);
        ret = setDayOfWeek(characteristic, mDayOfWeek);
        ret = setDateTime(characteristic, mDateTime);
        return ret;
    }

    DateTime getDateTime() {
        if (DBG) Log.d(TAG, "getDateTime: mDateTime = " + mDateTime);
        return mDateTime;
    }

    boolean setDateTime(final DateTime dt) {
        if (DBG) Log.d(TAG, "setDateTime: dt = " + dt);
        mDateTime = dt;
        return true;
    }

    int getDayOfWeek() {
        if (DBG) Log.d(TAG, "getDayOfWeek: mDayOfWeek = " + mDayOfWeek);
        return mDayOfWeek;
    }

    boolean setDayOfWeek(final int dow) {
        if (DBG) Log.d(TAG, "setDayOfWeek: dow = " + dow);
        mDayOfWeek = dow;
        return true;
    }

    int getFraction256() {
        if (DBG) Log.d(TAG, "getFraction256: mFraction256 = " + mFraction256);
        return mFraction256;
    }

    boolean setFraction256(final int fraction256) {
        if (DBG) Log.d(TAG, "setFraction256: fraction256 = " + fraction256);
        mFraction256 = fraction256;
        return true;
    }

    int getAdjustReason() {
        if (DBG) Log.d(TAG, "getAdjustReason: mAdjustReason = " + mAdjustReason);
        return mAdjustReason;
    }

    boolean setAdjustReason(final int ar) {
        if (DBG) Log.d(TAG, "setAdjustReason: ar = " + ar);
        mAdjustReason = ar;
        return true;
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

    private boolean setDayOfWeek(final BluetoothGattCharacteristic characteristic,
            final int dayOfWeek) {
        return characteristic.setValue(dayOfWeek, DAY_OF_WEEK_FORMAT, DAY_OF_WEEK_OFFSET);
    }

    private boolean setFractions256(final BluetoothGattCharacteristic characteristic,
            final int fractions256) {
        return characteristic.setValue(fractions256, FRACTIONS256_FORMAT, FRACTIONS256_OFFSET);
    }

    private boolean setAdjustReason(final BluetoothGattCharacteristic characteristic,
            final int reason) {
        return characteristic.setValue(reason, ADJUST_REASON_FORMAT, ADJUST_REASON_OFFSET);
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

    private int getDayOfWeek(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(DAY_OF_WEEK_FORMAT,
                    DAY_OF_WEEK_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return CurrentTimeService.DAY_OF_WEEK_UNKNOWN;
    }

    private int getFractions256(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(FRACTIONS256_FORMAT,
                    FRACTIONS256_OFFSET);
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

    private int getAdjustReason(final BluetoothGattCharacteristic characteristic) {
        if (null != characteristic) {
            final Integer intValue = characteristic.getIntValue(ADJUST_REASON_FORMAT,
                    ADJUST_REASON_OFFSET);
            if (null != intValue) {
                return intValue.intValue();
            } else {
                Log.e(TAG, "intValue is null");
            }
        } else {
            Log.e(TAG, "characteristic is null");
        }
        return CurrentTimeService.ADJUST_REASON_NONE;
    }
}
