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
 * Public API for the Battery Level Bluetooth GATT Characteristic.
 *
 * <p>This class provides Battery Level Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Battery Level
 * Type: org.bluetooth.characteristic.battery_level
 * UUID: 2A19
 * Last Modified: 2011-12-05
 * Revision: None
 */
public class BatteryLevel extends CharacteristicBase {
    /**
     * Battery Level UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A19"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Level
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.percentage
     */
    private byte[] mLevel = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a BatteryLevel characteristic object.
     */
    public BatteryLevel() {
        setCharacteristic(null);
        setLevel(0);
    }

    /**
     * Create a BatteryLevel characteristic object and init value.
     *
     * @param value Initial value
     */
    public BatteryLevel(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a BatteryLevel characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BatteryLevel(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a BatteryLevel characteristic object.
     *
     * @param level Level
     */
    public BatteryLevel(
            int level) {
        setCharacteristic(null);
        setLevel(level);
    }

    /**
     * Create a BatteryLevel characteristic object.
     *
     * @param level Level
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BatteryLevel(
            int level,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setLevel(level);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get BatteryLevel characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportLevel()
                        ? mLevel.length : 0);
    }

    /**
     * Get BatteryLevel characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get BatteryLevel characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportLevel()) {
            int fieldLen = mLevel.length;
            System.arraycopy(mLevel, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set BatteryLevel characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportLevel()) {
            int fieldLen = mLevel.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mLevel, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get Level field value with int format.
     *
     * @return Level field value
     */
    public int getLevel() {
        return FormatUtils.uint8ToInt(mLevel);
    }

    /**
     * Set Level field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Level field
     * @return      True, if the value has been set successfully
     */
    public boolean setLevel(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mLevel = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BatteryLevel support Level field.
     *
     * @return  True, if BatteryLevel support Level field.
     */
    public boolean isSupportLevel() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

