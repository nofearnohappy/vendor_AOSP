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
 * Public API for the Local Time Information Bluetooth GATT Characteristic.
 *
 * <p>This class provides Local Time Information Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Local Time Information
 * Type: org.bluetooth.characteristic.local_time_information
 * UUID: 2A0F
 * Last Modified: None
 * Revision: None
 */
public class LocalTimeInformation extends CharacteristicBase {
    /**
     * Local Time Information UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0F"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Time Zone
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.time_zone
     */
    private TimeZone mTimeZone = new TimeZone();

    /*
     * Field: Daylight Saving Time
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.dst_offset
     */
    private DstOffset mDaylightSavingTime = new DstOffset();


    /**
     * Create a LocalTimeInformation characteristic object.
     */
    public LocalTimeInformation() {
        setCharacteristic(null);

    }

    /**
     * Create a LocalTimeInformation characteristic object and init value.
     *
     * @param value Initial value
     */
    public LocalTimeInformation(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a LocalTimeInformation characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public LocalTimeInformation(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a LocalTimeInformation characteristic object.
     *
     * @param timeZone Time Zone
     * @param daylightSavingTime Daylight Saving Time
     */
    public LocalTimeInformation(
            TimeZone timeZone,
            DstOffset daylightSavingTime) {
        setCharacteristic(null);
        setTimeZone(timeZone);
        setDaylightSavingTime(daylightSavingTime);
    }

    /**
     * Create a LocalTimeInformation characteristic object.
     *
     * @param timeZone Time Zone
     * @param daylightSavingTime Daylight Saving Time
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public LocalTimeInformation(
            TimeZone timeZone,
            DstOffset daylightSavingTime,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setTimeZone(timeZone);
        setDaylightSavingTime(daylightSavingTime);
    }

    // Customized Start: Constructors ..............................................................
    /**
     * Create a LocalTimeInformation characteristic object and init value.
     *
     * @param tz Current timezone
     */
    public LocalTimeInformation(java.util.TimeZone tz) {
        setCharacteristic(null);
        if (tz != null) {
            setTimeZone(new TimeZone(tz));
            setDaylightSavingTime(new DstOffset(tz));
        }
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get LocalTimeInformation characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportTimeZone()
                        ? mTimeZone.length() : 0)
                + (isSupportDaylightSavingTime()
                        ? mDaylightSavingTime.length() : 0);
    }

    /**
     * Get LocalTimeInformation characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get LocalTimeInformation characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportTimeZone()) {
            int fieldLen = mTimeZone.length();
            System.arraycopy(mTimeZone.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportDaylightSavingTime()) {
            int fieldLen = mDaylightSavingTime.length();
            System.arraycopy(mDaylightSavingTime.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set LocalTimeInformation characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportTimeZone()) {
            int fieldLen = mTimeZone.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mTimeZone.setValue(buf);
        }

        if (isSupportDaylightSavingTime()) {
            int fieldLen = mDaylightSavingTime.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mDaylightSavingTime.setValue(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get TimeZone field value with TimeZone format.
     *
     * @return TimeZone field value
     */
    public TimeZone getTimeZone() {
        return mTimeZone;
    }

    /**
     * Set TimeZone field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeZone field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeZone(byte[] value) {
        if (!mTimeZone.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set TimeZone field value by TimeZone format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeZone field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeZone(TimeZone value) {
        if (!mTimeZone.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if LocalTimeInformation support TimeZone field.
     *
     * @return  True, if LocalTimeInformation support TimeZone field.
     */
    public boolean isSupportTimeZone() {
        return true;
    }

    /**
     * Get DaylightSavingTime field value with DstOffset format.
     *
     * @return DaylightSavingTime field value
     */
    public DstOffset getDaylightSavingTime() {
        return mDaylightSavingTime;
    }

    /**
     * Set DaylightSavingTime field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DaylightSavingTime field
     * @return      True, if the value has been set successfully
     */
    public boolean setDaylightSavingTime(byte[] value) {
        if (!mDaylightSavingTime.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set DaylightSavingTime field value by DstOffset format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DaylightSavingTime field
     * @return      True, if the value has been set successfully
     */
    public boolean setDaylightSavingTime(DstOffset value) {
        if (!mDaylightSavingTime.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if LocalTimeInformation support DaylightSavingTime field.
     *
     * @return  True, if LocalTimeInformation support DaylightSavingTime field.
     */
    public boolean isSupportDaylightSavingTime() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

