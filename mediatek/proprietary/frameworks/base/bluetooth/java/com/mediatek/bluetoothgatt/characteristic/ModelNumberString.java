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
 * Public API for the Model Number String Bluetooth GATT Characteristic.
 *
 * <p>This class provides Model Number String Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Model Number String
 * Type: org.bluetooth.characteristic.model_number_string
 * UUID: 2A24
 * Last Modified: 2011-05-24
 * Revision: None
 */
public class ModelNumberString extends CharacteristicBase {
    /**
     * Model Number String UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A24"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Model Number
     * Requirement: Mandatory
     * Format: utf8s
     * Unit: None
     */
    private byte[] mModelNumber = new byte[0];


    /**
     * Create a ModelNumberString characteristic object.
     */
    public ModelNumberString() {
        setCharacteristic(null);
        setModelNumber("");
    }

    /**
     * Create a ModelNumberString characteristic object and init value.
     *
     * @param value Initial value
     */
    public ModelNumberString(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a ModelNumberString characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ModelNumberString(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a ModelNumberString characteristic object.
     *
     * @param modelNumber Model Number
     */
    public ModelNumberString(
            String modelNumber) {
        setCharacteristic(null);
        setModelNumber(modelNumber);
    }

    /**
     * Create a ModelNumberString characteristic object.
     *
     * @param modelNumber Model Number
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public ModelNumberString(
            String modelNumber,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setModelNumber(modelNumber);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get ModelNumberString characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportModelNumber()
                        ? mModelNumber.length : 0);
    }

    /**
     * Get ModelNumberString characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get ModelNumberString characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportModelNumber()) {
            int fieldLen = mModelNumber.length;
            System.arraycopy(mModelNumber, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set ModelNumberString characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportModelNumber()) {
            mModelNumber = new byte[value.length - srcPos];
            int fieldLen = mModelNumber.length;
            System.arraycopy(value, srcPos, mModelNumber, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get ModelNumber field value with String format.
     *
     * @return ModelNumber field value
     */
    public String getModelNumber() {
        return FormatUtils.utf8sToString(mModelNumber);
    }

    /**
     * Set ModelNumber field value by String format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ModelNumber field
     * @return  True
     */
    public boolean setModelNumber(String value) {
        mModelNumber = FormatUtils.stringToUtf8s(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if ModelNumberString support ModelNumber field.
     *
     * @return  True, if ModelNumberString support ModelNumber field.
     */
    public boolean isSupportModelNumber() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

