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
 * Public API for the Alert Level Bluetooth GATT Characteristic.
 *
 * <p>This class provides Alert Level Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Alert Level
 * Type: org.bluetooth.characteristic.alert_level
 * UUID: 2A06
 * Last Modified: None
 * Revision: None
 */
public class AlertLevel extends CharacteristicBase {
    /**
     * Alert Level UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A06"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Alert Level Enumerations: No Alert.
     */
    public static final int AL_NO_ALERT = 0;

    /**
     * Alert Level Enumerations: Mild Alert.
     */
    public static final int AL_MILD_ALERT = 1;

    /**
     * Alert Level Enumerations: high Alert.
     */
    public static final int AL_HIGH_ALERT = 2;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Alert Level
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mAlertLevel = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a AlertLevel characteristic object.
     */
    public AlertLevel() {
        setCharacteristic(null);
        setAlertLevel(0);
    }

    /**
     * Create a AlertLevel characteristic object and init value.
     *
     * @param value Initial value
     */
    public AlertLevel(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a AlertLevel characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertLevel(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a AlertLevel characteristic object.
     *
     * @param alertLevel Alert Level
     */
    public AlertLevel(
            int alertLevel) {
        setCharacteristic(null);
        setAlertLevel(alertLevel);
    }

    /**
     * Create a AlertLevel characteristic object.
     *
     * @param alertLevel Alert Level
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertLevel(
            int alertLevel,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setAlertLevel(alertLevel);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get AlertLevel characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportAlertLevel()
                        ? mAlertLevel.length : 0);
    }

    /**
     * Get AlertLevel characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get AlertLevel characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportAlertLevel()) {
            int fieldLen = mAlertLevel.length;
            System.arraycopy(mAlertLevel, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set AlertLevel characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportAlertLevel()) {
            int fieldLen = mAlertLevel.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mAlertLevel, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get AlertLevel field value with int format.
     *
     * @return AlertLevel field value
     */
    public int getAlertLevel() {
        return FormatUtils.uint8ToInt(mAlertLevel);
    }

    /**
     * Set AlertLevel field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to AlertLevel field
     * @return      True, if the value has been set successfully
     */
    public boolean setAlertLevel(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mAlertLevel = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if AlertLevel support AlertLevel field.
     *
     * @return  True, if AlertLevel support AlertLevel field.
     */
    public boolean isSupportAlertLevel() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

