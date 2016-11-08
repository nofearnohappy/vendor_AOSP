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

//........................................................................ Customized End: Import //
import java.util.UUID;

/**
 * Public API for the Reference Time Information Bluetooth GATT Characteristic.
 *
 * <p>This class provides Reference Time Information Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Reference Time Information
 * Type: org.bluetooth.characteristic.reference_time_information
 * UUID: 2A14
 * Last Modified: None
 * Revision: None
 */
public class ReferenceTimeInformation extends CharacteristicBase {
    /**
     * Reference Time Information UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A14"));

    // Customized Start: Constant Definition .......................................................
    public static final int DAYS_SINCE_UPDATE_MIN = 0;
    public static final int DAYS_SINCE_UPDATE_MAX = 254;
    public static final int DAYS_SINCE_UPDATE_255 = 255;  // 255 or more days
    public static final int HOURS_SINCE_UPDATE_MIN = 0;
    public static final int HOURS_SINCE_UPDATE_MAX = 254;
    public static final int HOURS_SINCE_UPDATE_255 = 255;  // 255 or more days
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Source
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.time_source
     */
    private TimeSource mSource = new TimeSource();

    /*
     * Field: Accuracy
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.time_accuracy
     */
    private TimeAccuracy mAccuracy = new TimeAccuracy();

    /*
     * Field: Days Since Update
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.day
     */
    private byte[] mDaysSinceUpdate = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Hours Since Update
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.hour
     */
    private byte[] mHoursSinceUpdate = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a ReferenceTimeInformation characteristic object.
     */
    public ReferenceTimeInformation() {
        setCharacteristic(null);
        setDaysSinceUpdate(0);
        setHoursSinceUpdate(0);
    }

    /**
     * Create a ReferenceTimeInformation characteristic object and init value.
     *
     * @param value Initial value
     */
    public ReferenceTimeInformation(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a ReferenceTimeInformation characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ReferenceTimeInformation(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a ReferenceTimeInformation characteristic object.
     *
     * @param source Source
     * @param accuracy Accuracy
     * @param daysSinceUpdate Days Since Update
     * @param hoursSinceUpdate Hours Since Update
     */
    public ReferenceTimeInformation(
            TimeSource source,
            TimeAccuracy accuracy,
            int daysSinceUpdate,
            int hoursSinceUpdate) {
        setCharacteristic(null);
        setSource(source);
        setAccuracy(accuracy);
        setDaysSinceUpdate(daysSinceUpdate);
        setHoursSinceUpdate(hoursSinceUpdate);
    }

    /**
     * Create a ReferenceTimeInformation characteristic object.
     *
     * @param source Source
     * @param accuracy Accuracy
     * @param daysSinceUpdate Days Since Update
     * @param hoursSinceUpdate Hours Since Update
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ReferenceTimeInformation(
            TimeSource source,
            TimeAccuracy accuracy,
            int daysSinceUpdate,
            int hoursSinceUpdate,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setSource(source);
        setAccuracy(accuracy);
        setDaysSinceUpdate(daysSinceUpdate);
        setHoursSinceUpdate(hoursSinceUpdate);
    }

    // Customized Start: Constructors ..............................................................
    /**
     * Create a ReferenceTimeInformation characteristic object and init value.
     *
     * @param source Time Source
     * @param accuracy Accuracy (drift) of time information in steps of 1/8 of a second (125ms)
     *                 compared to a reference time source
     * @param daysSinceUpdate Days Since Update
     * @param hoursSinceUpdate Hours Since Update
     */
    public ReferenceTimeInformation(int source, int accuracy, int daysSinceUpdate,
            int hoursSinceUpdate) {
        setCharacteristic(null);
        setSource(new TimeSource(source));
        setAccuracy(new TimeAccuracy(accuracy));
        setDaysSinceUpdate(daysSinceUpdate);
        setHoursSinceUpdate(hoursSinceUpdate);
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get ReferenceTimeInformation characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportSource()
                        ? mSource.length() : 0)
                + (isSupportAccuracy()
                        ? mAccuracy.length() : 0)
                + (isSupportDaysSinceUpdate()
                        ? mDaysSinceUpdate.length : 0)
                + (isSupportHoursSinceUpdate()
                        ? mHoursSinceUpdate.length : 0);
    }

    /**
     * Get ReferenceTimeInformation characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get ReferenceTimeInformation characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportSource()) {
            int fieldLen = mSource.length();
            System.arraycopy(mSource.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportAccuracy()) {
            int fieldLen = mAccuracy.length();
            System.arraycopy(mAccuracy.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportDaysSinceUpdate()) {
            int fieldLen = mDaysSinceUpdate.length;
            System.arraycopy(mDaysSinceUpdate, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportHoursSinceUpdate()) {
            int fieldLen = mHoursSinceUpdate.length;
            System.arraycopy(mHoursSinceUpdate, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set ReferenceTimeInformation characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportSource()) {
            int fieldLen = mSource.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mSource.setValue(buf);
        }

        if (isSupportAccuracy()) {
            int fieldLen = mAccuracy.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mAccuracy.setValue(buf);
        }

        if (isSupportDaysSinceUpdate()) {
            int fieldLen = mDaysSinceUpdate.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mDaysSinceUpdate, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportHoursSinceUpdate()) {
            int fieldLen = mHoursSinceUpdate.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mHoursSinceUpdate, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get Source field value with TimeSource format.
     *
     * @return Source field value
     */
    public TimeSource getSource() {
        return mSource;
    }

    /**
     * Set Source field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Source field
     * @return      True, if the value has been set successfully
     */
    public boolean setSource(byte[] value) {
        if (!mSource.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set Source field value by TimeSource format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Source field
     * @return      True, if the value has been set successfully
     */
    public boolean setSource(TimeSource value) {
        if (!mSource.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ReferenceTimeInformation support Source field.
     *
     * @return  True, if ReferenceTimeInformation support Source field.
     */
    public boolean isSupportSource() {
        return true;
    }

    /**
     * Get Accuracy field value with TimeAccuracy format.
     *
     * @return Accuracy field value
     */
    public TimeAccuracy getAccuracy() {
        return mAccuracy;
    }

    /**
     * Set Accuracy field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Accuracy field
     * @return      True, if the value has been set successfully
     */
    public boolean setAccuracy(byte[] value) {
        if (!mAccuracy.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set Accuracy field value by TimeAccuracy format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Accuracy field
     * @return      True, if the value has been set successfully
     */
    public boolean setAccuracy(TimeAccuracy value) {
        if (!mAccuracy.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ReferenceTimeInformation support Accuracy field.
     *
     * @return  True, if ReferenceTimeInformation support Accuracy field.
     */
    public boolean isSupportAccuracy() {
        return true;
    }

    /**
     * Get DaysSinceUpdate field value with int format.
     *
     * @return DaysSinceUpdate field value
     */
    public int getDaysSinceUpdate() {
        return FormatUtils.uint8ToInt(mDaysSinceUpdate);
    }

    /**
     * Set DaysSinceUpdate field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DaysSinceUpdate field
     * @return      True, if the value has been set successfully
     */
    public boolean setDaysSinceUpdate(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mDaysSinceUpdate = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ReferenceTimeInformation support DaysSinceUpdate field.
     *
     * @return  True, if ReferenceTimeInformation support DaysSinceUpdate field.
     */
    public boolean isSupportDaysSinceUpdate() {
        return true;
    }

    /**
     * Get HoursSinceUpdate field value with int format.
     *
     * @return HoursSinceUpdate field value
     */
    public int getHoursSinceUpdate() {
        return FormatUtils.uint8ToInt(mHoursSinceUpdate);
    }

    /**
     * Set HoursSinceUpdate field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to HoursSinceUpdate field
     * @return      True, if the value has been set successfully
     */
    public boolean setHoursSinceUpdate(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mHoursSinceUpdate = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ReferenceTimeInformation support HoursSinceUpdate field.
     *
     * @return  True, if ReferenceTimeInformation support HoursSinceUpdate field.
     */
    public boolean isSupportHoursSinceUpdate() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

