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
 * MediaTek Inc. (C) 2015. All rights reserved.
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

package com.mediatek.bluetoothgatt.characteristic;

import android.bluetooth.BluetoothGattCharacteristic;
// Customized Start: Import ........................................................................

import java.util.Calendar;
//........................................................................ Customized End: Import //
import java.util.UUID;

/**
 * Public API for the Date Time Bluetooth GATT Characteristic.
 *
 * <p>This class provides Date Time Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Date Time
 * Type: org.bluetooth.characteristic.date_time
 * UUID: 2A08
 * Last Modified: None
 * Revision: None
 */
public class DateTime extends CharacteristicBase {
    /**
     * Date Time UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A08"));

    // Customized Start: Constant Definition .......................................................
    public static final int YEAR_IS_NOT_KNOWN = 0;
    public static final int MONTH_IS_NOT_KNOWN = 0;
    public static final int DAY_OF_MONTH_IS_NOT_KNOWN = 0;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Year
     * Requirement: Mandatory
     * Format: uint16
     * Unit: org.bluetooth.unit.time.year
     */
    private byte[] mYear = new byte[FormatUtils.UINT16_SIZE];

    /*
     * Field: Month
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.month
     */
    private byte[] mMonth = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Day
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.day
     */
    private byte[] mDay = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Hours
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.hour
     */
    private byte[] mHours = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Minutes
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.minute
     */
    private byte[] mMinutes = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Seconds
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.second
     */
    private byte[] mSeconds = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a DateTime characteristic object.
     */
    public DateTime() {
        setCharacteristic(null);
        setYear(1582);
        setMonth(0);
        setDay(1);
        setHours(0);
        setMinutes(0);
        setSeconds(0);
    }

    /**
     * Create a DateTime characteristic object and init value.
     *
     * @param value Initial value
     */
    public DateTime(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a DateTime characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public DateTime(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a DateTime characteristic object.
     *
     * @param year Year
     * @param month Month
     * @param day Day
     * @param hours Hours
     * @param minutes Minutes
     * @param seconds Seconds
     */
    public DateTime(
            int year,
            int month,
            int day,
            int hours,
            int minutes,
            int seconds) {
        setCharacteristic(null);
        setYear(year);
        setMonth(month);
        setDay(day);
        setHours(hours);
        setMinutes(minutes);
        setSeconds(seconds);
    }

    /**
     * Create a DateTime characteristic object.
     *
     * @param year Year
     * @param month Month
     * @param day Day
     * @param hours Hours
     * @param minutes Minutes
     * @param seconds Seconds
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public DateTime(
            int year,
            int month,
            int day,
            int hours,
            int minutes,
            int seconds,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setYear(year);
        setMonth(month);
        setDay(day);
        setHours(hours);
        setMinutes(minutes);
        setSeconds(seconds);
    }

    // Customized Start: Constructors ..............................................................
    /**
     * Create a DateTime characteristic object and init value.
     *
     * @param c Current date and time
     */
    public DateTime(Calendar c) {
        setCharacteristic(null);
        if (c == null) {
            setCharacteristic(null);
            setYear(YEAR_IS_NOT_KNOWN);
            setMonth(MONTH_IS_NOT_KNOWN);
            setDay(DAY_OF_MONTH_IS_NOT_KNOWN);
            setHours(0);
            setMinutes(0);
            setSeconds(0);
        } else {
            setYear(c.get(Calendar.YEAR));
            setMonth(c.get(Calendar.MONTH) + 1);
            setDay(c.get(Calendar.DAY_OF_MONTH));
            setHours(c.get(Calendar.HOUR_OF_DAY));
            setMinutes(c.get(Calendar.MINUTE));
            setSeconds(c.get(Calendar.SECOND));
        }
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get DateTime characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportYear()
                        ? mYear.length : 0)
                + (isSupportMonth()
                        ? mMonth.length : 0)
                + (isSupportDay()
                        ? mDay.length : 0)
                + (isSupportHours()
                        ? mHours.length : 0)
                + (isSupportMinutes()
                        ? mMinutes.length : 0)
                + (isSupportSeconds()
                        ? mSeconds.length : 0);
    }

    /**
     * Get DateTime characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get DateTime characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportYear()) {
            int fieldLen = mYear.length;
            System.arraycopy(mYear, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportMonth()) {
            int fieldLen = mMonth.length;
            System.arraycopy(mMonth, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportDay()) {
            int fieldLen = mDay.length;
            System.arraycopy(mDay, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportHours()) {
            int fieldLen = mHours.length;
            System.arraycopy(mHours, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportMinutes()) {
            int fieldLen = mMinutes.length;
            System.arraycopy(mMinutes, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportSeconds()) {
            int fieldLen = mSeconds.length;
            System.arraycopy(mSeconds, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set DateTime characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportYear()) {
            int fieldLen = mYear.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mYear, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportMonth()) {
            int fieldLen = mMonth.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mMonth, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportDay()) {
            int fieldLen = mDay.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mDay, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportHours()) {
            int fieldLen = mHours.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mHours, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportMinutes()) {
            int fieldLen = mMinutes.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mMinutes, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportSeconds()) {
            int fieldLen = mSeconds.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mSeconds, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get Year field value with int format.
     *
     * @return Year field value
     */
    public int getYear() {
        return FormatUtils.uint16ToInt(mYear);
    }

    /**
     * Set Year field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Year field
     * @return      True, if the value has been set successfully
     */
    public boolean setYear(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mYear = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DateTime support Year field.
     *
     * @return  True, if DateTime support Year field.
     */
    public boolean isSupportYear() {
        return true;
    }

    /**
     * Get Month field value with int format.
     *
     * @return Month field value
     */
    public int getMonth() {
        return FormatUtils.uint8ToInt(mMonth);
    }

    /**
     * Set Month field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Month field
     * @return      True, if the value has been set successfully
     */
    public boolean setMonth(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mMonth = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DateTime support Month field.
     *
     * @return  True, if DateTime support Month field.
     */
    public boolean isSupportMonth() {
        return true;
    }

    /**
     * Get Day field value with int format.
     *
     * @return Day field value
     */
    public int getDay() {
        return FormatUtils.uint8ToInt(mDay);
    }

    /**
     * Set Day field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Day field
     * @return      True, if the value has been set successfully
     */
    public boolean setDay(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mDay = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DateTime support Day field.
     *
     * @return  True, if DateTime support Day field.
     */
    public boolean isSupportDay() {
        return true;
    }

    /**
     * Get Hours field value with int format.
     *
     * @return Hours field value
     */
    public int getHours() {
        return FormatUtils.uint8ToInt(mHours);
    }

    /**
     * Set Hours field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Hours field
     * @return      True, if the value has been set successfully
     */
    public boolean setHours(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mHours = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DateTime support Hours field.
     *
     * @return  True, if DateTime support Hours field.
     */
    public boolean isSupportHours() {
        return true;
    }

    /**
     * Get Minutes field value with int format.
     *
     * @return Minutes field value
     */
    public int getMinutes() {
        return FormatUtils.uint8ToInt(mMinutes);
    }

    /**
     * Set Minutes field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Minutes field
     * @return      True, if the value has been set successfully
     */
    public boolean setMinutes(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mMinutes = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DateTime support Minutes field.
     *
     * @return  True, if DateTime support Minutes field.
     */
    public boolean isSupportMinutes() {
        return true;
    }

    /**
     * Get Seconds field value with int format.
     *
     * @return Seconds field value
     */
    public int getSeconds() {
        return FormatUtils.uint8ToInt(mSeconds);
    }

    /**
     * Set Seconds field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Seconds field
     * @return      True, if the value has been set successfully
     */
    public boolean setSeconds(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mSeconds = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DateTime support Seconds field.
     *
     * @return  True, if DateTime support Seconds field.
     */
    public boolean isSupportSeconds() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

