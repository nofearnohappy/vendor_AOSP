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
 * Public API for the Time Update Control Point Bluetooth GATT Characteristic.
 *
 * <p>This class provides Time Update Control Point Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Time Update Control Point
 * Type: org.bluetooth.characteristic.time_update_control_point
 * UUID: 2A16
 * Last Modified: None
 * Revision: None
 */
public class TimeUpdateControlPoint extends CharacteristicBase {
    /**
     * Time Update Control Point UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A16"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Time Update Control Point Enumerations: Get Reference Update.
     */
    public static final int TIME_UPDATE_CP_GET_REFERENCE_UPDATE = 1;

    /**
     * Time Update Control Point Enumerations: Cancel Reference Update.
     */
    public static final int TIME_UPDATE_CP_CANCEL_REFERENCE_UPDATE = 2;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Time Update Control Point
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mTimeUpdateControlPoint = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a TimeUpdateControlPoint characteristic object.
     */
    public TimeUpdateControlPoint() {
        setCharacteristic(null);
        setTimeUpdateControlPoint(1);
    }

    /**
     * Create a TimeUpdateControlPoint characteristic object and init value.
     *
     * @param value Initial value
     */
    public TimeUpdateControlPoint(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TimeUpdateControlPoint characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeUpdateControlPoint(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TimeUpdateControlPoint characteristic object.
     *
     * @param timeUpdateControlPoint Time Update Control Point
     */
    public TimeUpdateControlPoint(
            int timeUpdateControlPoint) {
        setCharacteristic(null);
        setTimeUpdateControlPoint(timeUpdateControlPoint);
    }

    /**
     * Create a TimeUpdateControlPoint characteristic object.
     *
     * @param timeUpdateControlPoint Time Update Control Point
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeUpdateControlPoint(
            int timeUpdateControlPoint,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setTimeUpdateControlPoint(timeUpdateControlPoint);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get TimeUpdateControlPoint characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportTimeUpdateControlPoint()
                        ? mTimeUpdateControlPoint.length : 0);
    }

    /**
     * Get TimeUpdateControlPoint characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TimeUpdateControlPoint characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportTimeUpdateControlPoint()) {
            int fieldLen = mTimeUpdateControlPoint.length;
            System.arraycopy(mTimeUpdateControlPoint, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TimeUpdateControlPoint characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportTimeUpdateControlPoint()) {
            int fieldLen = mTimeUpdateControlPoint.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTimeUpdateControlPoint, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get TimeUpdateControlPoint field value with int format.
     *
     * @return TimeUpdateControlPoint field value
     */
    public int getTimeUpdateControlPoint() {
        return FormatUtils.uint8ToInt(mTimeUpdateControlPoint);
    }

    /**
     * Set TimeUpdateControlPoint field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeUpdateControlPoint field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeUpdateControlPoint(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mTimeUpdateControlPoint = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeUpdateControlPoint support TimeUpdateControlPoint field.
     *
     * @return  True, if TimeUpdateControlPoint support TimeUpdateControlPoint field.
     */
    public boolean isSupportTimeUpdateControlPoint() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

