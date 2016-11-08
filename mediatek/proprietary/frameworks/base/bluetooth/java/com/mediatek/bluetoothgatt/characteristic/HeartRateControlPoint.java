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
 * Public API for the Heart Rate Control Point Bluetooth GATT Characteristic.
 *
 * <p>This class provides Heart Rate Control Point Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Heart Rate Control Point
 * Type: org.bluetooth.characteristic.heart_rate_control_point
 * UUID: 2A39
 * Last Modified: None
 * Revision: None
 */
public class HeartRateControlPoint extends CharacteristicBase {
    /**
     * Heart Rate Control Point UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A39"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Heart Rate Control Point Enumerations:
     *   Reset Energy Expended: resets the value of the Energy Expended field in the Heart Rate
     *   Measurement characteristic to 0.
     */
    public static final int HRCP_REST_ENERGY_EXPENDED = 1;

    /**
     * Heart Rate Control Point Enumerations: Reserved.
     */
    public static final int HRCP_RESERVED = 0;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Heart Rate Control Point
     * Requirement: Mandatory
     * Format: bit8
     * Unit: None
     */
    private byte[] mHeartRateControlPoint = new byte[FormatUtils.BIT8_SIZE];


    /**
     * Create a HeartRateControlPoint characteristic object.
     */
    public HeartRateControlPoint() {
        setCharacteristic(null);
        setHeartRateControlPoint(1);
    }

    /**
     * Create a HeartRateControlPoint characteristic object and init value.
     *
     * @param value Initial value
     */
    public HeartRateControlPoint(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a HeartRateControlPoint characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public HeartRateControlPoint(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a HeartRateControlPoint characteristic object.
     *
     * @param heartRateControlPoint Heart Rate Control Point
     */
    public HeartRateControlPoint(
            int heartRateControlPoint) {
        setCharacteristic(null);
        setHeartRateControlPoint(heartRateControlPoint);
    }

    /**
     * Create a HeartRateControlPoint characteristic object.
     *
     * @param heartRateControlPoint Heart Rate Control Point
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public HeartRateControlPoint(
            int heartRateControlPoint,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setHeartRateControlPoint(heartRateControlPoint);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get HeartRateControlPoint characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportHeartRateControlPoint()
                        ? mHeartRateControlPoint.length : 0);
    }

    /**
     * Get HeartRateControlPoint characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get HeartRateControlPoint characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportHeartRateControlPoint()) {
            int fieldLen = mHeartRateControlPoint.length;
            System.arraycopy(mHeartRateControlPoint, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set HeartRateControlPoint characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportHeartRateControlPoint()) {
            int fieldLen = mHeartRateControlPoint.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mHeartRateControlPoint, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get HeartRateControlPoint field value with int format.
     *
     * @return HeartRateControlPoint field value
     */
    public int getHeartRateControlPoint() {
        return FormatUtils.bit8ToInt(mHeartRateControlPoint);
    }

    /**
     * Set HeartRateControlPoint field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to HeartRateControlPoint field
     * @return      True, if the value has been set successfully
     */
    public boolean setHeartRateControlPoint(int value) {
        if (!FormatUtils.bit8RangeCheck(value)) {
            return false;
        }
        mHeartRateControlPoint = FormatUtils.intToBit8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if HeartRateControlPoint support HeartRateControlPoint field.
     *
     * @return  True, if HeartRateControlPoint support HeartRateControlPoint field.
     */
    public boolean isSupportHeartRateControlPoint() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

