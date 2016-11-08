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
 * Public API for the Manufacturer Name String Bluetooth GATT Characteristic.
 *
 * <p>This class provides Manufacturer Name String Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Manufacturer Name String
 * Type: org.bluetooth.characteristic.manufacturer_name_string
 * UUID: 2A29
 * Last Modified: 2011-05-24
 * Revision: None
 */
public class ManufacturerNameString extends CharacteristicBase {
    /**
     * Manufacturer Name String UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A29"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Manufacturer Name
     * Requirement: Mandatory
     * Format: utf8s
     * Unit: None
     */
    private byte[] mManufacturerName = new byte[0];


    /**
     * Create a ManufacturerNameString characteristic object.
     */
    public ManufacturerNameString() {
        setCharacteristic(null);
        setManufacturerName("");
    }

    /**
     * Create a ManufacturerNameString characteristic object and init value.
     *
     * @param value Initial value
     */
    public ManufacturerNameString(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a ManufacturerNameString characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ManufacturerNameString(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a ManufacturerNameString characteristic object.
     *
     * @param manufacturerName Manufacturer Name
     */
    public ManufacturerNameString(
            String manufacturerName) {
        setCharacteristic(null);
        setManufacturerName(manufacturerName);
    }

    /**
     * Create a ManufacturerNameString characteristic object.
     *
     * @param manufacturerName Manufacturer Name
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ManufacturerNameString(
            String manufacturerName,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setManufacturerName(manufacturerName);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get ManufacturerNameString characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportManufacturerName()
                        ? mManufacturerName.length : 0);
    }

    /**
     * Get ManufacturerNameString characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get ManufacturerNameString characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportManufacturerName()) {
            int fieldLen = mManufacturerName.length;
            System.arraycopy(mManufacturerName, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set ManufacturerNameString characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportManufacturerName()) {
            mManufacturerName = new byte[value.length - srcPos];
            int fieldLen = mManufacturerName.length;
            System.arraycopy(value, srcPos, mManufacturerName, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get ManufacturerName field value with String format.
     *
     * @return ManufacturerName field value
     */
    public String getManufacturerName() {
        return FormatUtils.utf8sToString(mManufacturerName);
    }

    /**
     * Set ManufacturerName field value by String format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ManufacturerName field
     * @return  True
     */
    public boolean setManufacturerName(String value) {
        mManufacturerName = FormatUtils.stringToUtf8s(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ManufacturerNameString support ManufacturerName field.
     *
     * @return  True, if ManufacturerNameString support ManufacturerName field.
     */
    public boolean isSupportManufacturerName() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

