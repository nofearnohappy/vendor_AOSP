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
 * Public API for the Temperature Type Bluetooth GATT Characteristic.
 *
 * <p>This class provides Temperature Type Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Temperature Type
 * Type: org.bluetooth.characteristic.temperature_type
 * UUID: 2A1D
 * Last Modified: None
 * Revision: None
 */
public class TemperatureType extends CharacteristicBase {
    /**
     * Temperature Type UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A1D"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Temperature Text Description Enumerations.
     */
    public static final int TEMPERATURE_TYPE_ARMPIT = 1;
    public static final int TEMPERATURE_TYPE_BODY = 2;
    public static final int TEMPERATURE_TYPE_EAR = 3;
    public static final int TEMPERATURE_TYPE_FINGER = 4;
    public static final int TEMPERATURE_TYPE_GASTRO = 5;
    public static final int TEMPERATURE_TYPE_MOUTH = 6;
    public static final int TEMPERATURE_TYPE_RECTUM = 7;
    public static final int TEMPERATURE_TYPE_TOE = 8;
    public static final int TEMPERATURE_TYPE_TYMPANUM = 9;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Temperature Text Description
     * Requirement: Mandatory
     * Format: bit8
     * Unit: None
     */
    private byte[] mTemperatureTextDescription = new byte[FormatUtils.BIT8_SIZE];


    /**
     * Create a TemperatureType characteristic object.
     */
    public TemperatureType() {
        setCharacteristic(null);
        setTemperatureTextDescription(1);
    }

    /**
     * Create a TemperatureType characteristic object and init value.
     *
     * @param value Initial value
     */
    public TemperatureType(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TemperatureType characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TemperatureType(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TemperatureType characteristic object.
     *
     * @param temperatureTextDescription Temperature Text Description
     */
    public TemperatureType(
            int temperatureTextDescription) {
        setCharacteristic(null);
        setTemperatureTextDescription(temperatureTextDescription);
    }

    /**
     * Create a TemperatureType characteristic object.
     *
     * @param temperatureTextDescription Temperature Text Description
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TemperatureType(
            int temperatureTextDescription,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setTemperatureTextDescription(temperatureTextDescription);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get TemperatureType characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportTemperatureTextDescription()
                        ? mTemperatureTextDescription.length : 0);
    }

    /**
     * Get TemperatureType characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TemperatureType characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportTemperatureTextDescription()) {
            int fieldLen = mTemperatureTextDescription.length;
            System.arraycopy(mTemperatureTextDescription, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TemperatureType characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportTemperatureTextDescription()) {
            int fieldLen = mTemperatureTextDescription.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTemperatureTextDescription, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get TemperatureTextDescription field value with int format.
     *
     * @return TemperatureTextDescription field value
     */
    public int getTemperatureTextDescription() {
        return FormatUtils.bit8ToInt(mTemperatureTextDescription);
    }

    /**
     * Set TemperatureTextDescription field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TemperatureTextDescription field
     * @return      True, if the value has been set successfully
     */
    public boolean setTemperatureTextDescription(int value) {
        if (!FormatUtils.bit8RangeCheck(value)) {
            return false;
        }
        mTemperatureTextDescription = FormatUtils.intToBit8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TemperatureType support TemperatureTextDescription field.
     *
     * @return  True, if TemperatureType support TemperatureTextDescription field.
     */
    public boolean isSupportTemperatureTextDescription() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

