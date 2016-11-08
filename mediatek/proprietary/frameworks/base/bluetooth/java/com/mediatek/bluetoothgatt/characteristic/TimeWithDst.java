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
 * Public API for the Time with DST Bluetooth GATT Characteristic.
 *
 * <p>This class provides Time with DST Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Time with DST
 * Type: org.bluetooth.characteristic.time_with_dst
 * UUID: 2A11
 * Last Modified: None
 * Revision: None
 */
public class TimeWithDst extends CharacteristicBase {
    /**
     * Time with DST UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A11"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Date Time
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.date_time
     */
    private DateTime mDateTime = new DateTime();

    /*
     * Field: DST Offset
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.dst_offset
     */
    private DstOffset mDstOffset = new DstOffset();


    /**
     * Create a TimeWithDst characteristic object.
     */
    public TimeWithDst() {
        setCharacteristic(null);

    }

    /**
     * Create a TimeWithDst characteristic object and init value.
     *
     * @param value Initial value
     */
    public TimeWithDst(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TimeWithDst characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeWithDst(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TimeWithDst characteristic object.
     *
     * @param dateTime Date Time
     * @param dstOffset DST Offset
     */
    public TimeWithDst(
            DateTime dateTime,
            DstOffset dstOffset) {
        setCharacteristic(null);
        setDateTime(dateTime);
        setDstOffset(dstOffset);
    }

    /**
     * Create a TimeWithDst characteristic object.
     *
     * @param dateTime Date Time
     * @param dstOffset DST Offset
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeWithDst(
            DateTime dateTime,
            DstOffset dstOffset,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setDateTime(dateTime);
        setDstOffset(dstOffset);
    }

    // Customized Start: Constructors ..............................................................
    /**
     *  Create a TimeWithDst characteristic object and init value.
     *
     * @param c Current Time
     * @param tz Current Time zone
     */
    public TimeWithDst(Calendar c, java.util.TimeZone tz) {
        setCharacteristic(null);
        setDateTime(new DateTime(c));
        setDstOffset(new DstOffset(tz));
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get TimeWithDst characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportDateTime()
                        ? mDateTime.length() : 0)
                + (isSupportDstOffset()
                        ? mDstOffset.length() : 0);
    }

    /**
     * Get TimeWithDst characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TimeWithDst characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportDateTime()) {
            int fieldLen = mDateTime.length();
            System.arraycopy(mDateTime.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportDstOffset()) {
            int fieldLen = mDstOffset.length();
            System.arraycopy(mDstOffset.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TimeWithDst characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportDateTime()) {
            int fieldLen = mDateTime.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mDateTime.setValue(buf);
        }

        if (isSupportDstOffset()) {
            int fieldLen = mDstOffset.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mDstOffset.setValue(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get DateTime field value with DateTime format.
     *
     * @return DateTime field value
     */
    public DateTime getDateTime() {
        return mDateTime;
    }

    /**
     * Set DateTime field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DateTime field
     * @return      True, if the value has been set successfully
     */
    public boolean setDateTime(byte[] value) {
        if (!mDateTime.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set DateTime field value by DateTime format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DateTime field
     * @return      True, if the value has been set successfully
     */
    public boolean setDateTime(DateTime value) {
        if (!mDateTime.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeWithDst support DateTime field.
     *
     * @return  True, if TimeWithDst support DateTime field.
     */
    public boolean isSupportDateTime() {
        return true;
    }

    /**
     * Get DstOffset field value with DstOffset format.
     *
     * @return DstOffset field value
     */
    public DstOffset getDstOffset() {
        return mDstOffset;
    }

    /**
     * Set DstOffset field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DstOffset field
     * @return      True, if the value has been set successfully
     */
    public boolean setDstOffset(byte[] value) {
        if (!mDstOffset.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set DstOffset field value by DstOffset format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DstOffset field
     * @return      True, if the value has been set successfully
     */
    public boolean setDstOffset(DstOffset value) {
        if (!mDstOffset.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeWithDst support DstOffset field.
     *
     * @return  True, if TimeWithDst support DstOffset field.
     */
    public boolean isSupportDstOffset() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

