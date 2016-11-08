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
 * Public API for the Serial Number String Bluetooth GATT Characteristic.
 *
 * <p>This class provides Serial Number String Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Serial Number String
 * Type: org.bluetooth.characteristic.serial_number_string
 * UUID: 2A25
 * Last Modified: 2011-05-24
 * Revision: None
 */
public class SerialNumberString extends CharacteristicBase {
    /**
     * Serial Number String UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A25"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Serial Number
     * Requirement: Mandatory
     * Format: utf8s
     * Unit: None
     */
    private byte[] mSerialNumber = new byte[0];


    /**
     * Create a SerialNumberString characteristic object.
     */
    public SerialNumberString() {
        setCharacteristic(null);
        setSerialNumber("");
    }

    /**
     * Create a SerialNumberString characteristic object and init value.
     *
     * @param value Initial value
     */
    public SerialNumberString(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a SerialNumberString characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SerialNumberString(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a SerialNumberString characteristic object.
     *
     * @param serialNumber Serial Number
     */
    public SerialNumberString(
            String serialNumber) {
        setCharacteristic(null);
        setSerialNumber(serialNumber);
    }

    /**
     * Create a SerialNumberString characteristic object.
     *
     * @param serialNumber Serial Number
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SerialNumberString(
            String serialNumber,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setSerialNumber(serialNumber);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get SerialNumberString characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportSerialNumber()
                        ? mSerialNumber.length : 0);
    }

    /**
     * Get SerialNumberString characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get SerialNumberString characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportSerialNumber()) {
            int fieldLen = mSerialNumber.length;
            System.arraycopy(mSerialNumber, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set SerialNumberString characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportSerialNumber()) {
            mSerialNumber = new byte[value.length - srcPos];
            int fieldLen = mSerialNumber.length;
            System.arraycopy(value, srcPos, mSerialNumber, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get SerialNumber field value with String format.
     *
     * @return SerialNumber field value
     */
    public String getSerialNumber() {
        return FormatUtils.utf8sToString(mSerialNumber);
    }

    /**
     * Set SerialNumber field value by String format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to SerialNumber field
     * @return  True
     */
    public boolean setSerialNumber(String value) {
        mSerialNumber = FormatUtils.stringToUtf8s(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if SerialNumberString support SerialNumber field.
     *
     * @return  True, if SerialNumberString support SerialNumber field.
     */
    public boolean isSupportSerialNumber() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

