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
 * Public API for the Body Sensor Location Bluetooth GATT Characteristic.
 *
 * <p>This class provides Body Sensor Location Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Body Sensor Location
 * Type: org.bluetooth.characteristic.body_sensor_location
 * UUID: 2A38
 * Last Modified: None
 * Revision: None
 */
public class BodySensorLocation extends CharacteristicBase {
    /**
     * Body Sensor Location UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A38"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Body Sensor Location Enumerations.
     */
    public static final int BSL_OTHER = 0;
    public static final int BSL_CHEST = 1;
    public static final int BSL_WRIST = 2;
    public static final int BSL_FINGER = 3;
    public static final int BSL_HAND = 4;
    public static final int BSL_EAR_LOBE = 5;
    public static final int BSL_FOOT = 6;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Body Sensor Location
     * Requirement: Mandatory
     * Format: bit8
     * Unit: None
     */
    private byte[] mBodySensorLocation = new byte[FormatUtils.BIT8_SIZE];


    /**
     * Create a BodySensorLocation characteristic object.
     */
    public BodySensorLocation() {
        setCharacteristic(null);
        setBodySensorLocation(0);
    }

    /**
     * Create a BodySensorLocation characteristic object and init value.
     *
     * @param value Initial value
     */
    public BodySensorLocation(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a BodySensorLocation characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BodySensorLocation(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a BodySensorLocation characteristic object.
     *
     * @param bodySensorLocation Body Sensor Location
     */
    public BodySensorLocation(
            int bodySensorLocation) {
        setCharacteristic(null);
        setBodySensorLocation(bodySensorLocation);
    }

    /**
     * Create a BodySensorLocation characteristic object.
     *
     * @param bodySensorLocation Body Sensor Location
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BodySensorLocation(
            int bodySensorLocation,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setBodySensorLocation(bodySensorLocation);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get BodySensorLocation characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportBodySensorLocation()
                        ? mBodySensorLocation.length : 0);
    }

    /**
     * Get BodySensorLocation characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get BodySensorLocation characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportBodySensorLocation()) {
            int fieldLen = mBodySensorLocation.length;
            System.arraycopy(mBodySensorLocation, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set BodySensorLocation characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportBodySensorLocation()) {
            int fieldLen = mBodySensorLocation.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mBodySensorLocation, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get BodySensorLocation field value with int format.
     *
     * @return BodySensorLocation field value
     */
    public int getBodySensorLocation() {
        return FormatUtils.bit8ToInt(mBodySensorLocation);
    }

    /**
     * Set BodySensorLocation field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to BodySensorLocation field
     * @return      True, if the value has been set successfully
     */
    public boolean setBodySensorLocation(int value) {
        if (!FormatUtils.bit8RangeCheck(value)) {
            return false;
        }
        mBodySensorLocation = FormatUtils.intToBit8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BodySensorLocation support BodySensorLocation field.
     *
     * @return  True, if BodySensorLocation support BodySensorLocation field.
     */
    public boolean isSupportBodySensorLocation() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

