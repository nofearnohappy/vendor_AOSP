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
 * Public API for the Exact Time 256 Bluetooth GATT Characteristic.
 *
 * <p>This class provides Exact Time 256 Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Exact Time 256
 * Type: org.bluetooth.characteristic.exact_time_256
 * UUID: 2A0C
 * Last Modified: None
 * Revision: None
 */
public class ExactTime256 extends CharacteristicBase {
    /**
     * Exact Time 256 UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0C"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Day Date Time
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.day_date_time
     */
    private DayDateTime mDayDateTime = new DayDateTime();

    /*
     * Field: Fractions256
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mFractions256 = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a ExactTime256 characteristic object.
     */
    public ExactTime256() {
        setCharacteristic(null);
        setFractions256(0);
    }

    /**
     * Create a ExactTime256 characteristic object and init value.
     *
     * @param value Initial value
     */
    public ExactTime256(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a ExactTime256 characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ExactTime256(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a ExactTime256 characteristic object.
     *
     * @param dayDateTime Day Date Time
     * @param fractions256 Fractions256
     */
    public ExactTime256(
            DayDateTime dayDateTime,
            int fractions256) {
        setCharacteristic(null);
        setDayDateTime(dayDateTime);
        setFractions256(fractions256);
    }

    /**
     * Create a ExactTime256 characteristic object.
     *
     * @param dayDateTime Day Date Time
     * @param fractions256 Fractions256
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ExactTime256(
            DayDateTime dayDateTime,
            int fractions256,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setDayDateTime(dayDateTime);
        setFractions256(fractions256);
    }

    // Customized Start: Constructors ..............................................................

    /**
     * Create a ExactTime256 characteristic object and init value.
     *
     * @param c Current date and time
     */
    public ExactTime256(Calendar c) {
        setCharacteristic(null);
        if (c == null) {
            setFractions256(0);
        } else {
            setDayDateTime(new DayDateTime(c));
            setFractions256((c.get(Calendar.MILLISECOND) << 8) / 1000);
        }
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get ExactTime256 characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportDayDateTime()
                        ? mDayDateTime.length() : 0)
                + (isSupportFractions256()
                        ? mFractions256.length : 0);
    }

    /**
     * Get ExactTime256 characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get ExactTime256 characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportDayDateTime()) {
            int fieldLen = mDayDateTime.length();
            System.arraycopy(mDayDateTime.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportFractions256()) {
            int fieldLen = mFractions256.length;
            System.arraycopy(mFractions256, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set ExactTime256 characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportDayDateTime()) {
            int fieldLen = mDayDateTime.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mDayDateTime.setValue(buf);
        }

        if (isSupportFractions256()) {
            int fieldLen = mFractions256.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mFractions256, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get DayDateTime field value with DayDateTime format.
     *
     * @return DayDateTime field value
     */
    public DayDateTime getDayDateTime() {
        return mDayDateTime;
    }

    /**
     * Set DayDateTime field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DayDateTime field
     * @return      True, if the value has been set successfully
     */
    public boolean setDayDateTime(byte[] value) {
        if (!mDayDateTime.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set DayDateTime field value by DayDateTime format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DayDateTime field
     * @return      True, if the value has been set successfully
     */
    public boolean setDayDateTime(DayDateTime value) {
        if (!mDayDateTime.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ExactTime256 support DayDateTime field.
     *
     * @return  True, if ExactTime256 support DayDateTime field.
     */
    public boolean isSupportDayDateTime() {
        return true;
    }

    /**
     * Get Fractions256 field value with int format.
     *
     * @return Fractions256 field value
     */
    public int getFractions256() {
        return FormatUtils.uint8ToInt(mFractions256);
    }

    /**
     * Set Fractions256 field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Fractions256 field
     * @return      True, if the value has been set successfully
     */
    public boolean setFractions256(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mFractions256 = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ExactTime256 support Fractions256 field.
     *
     * @return  True, if ExactTime256 support Fractions256 field.
     */
    public boolean isSupportFractions256() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

