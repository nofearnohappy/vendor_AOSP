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
 * Public API for the IEEE Regulatory Certification Data List Bluetooth GATT Characteristic.
 *
 * <p>This class provides IEEE Regulatory Certification Data List Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: IEEE Regulatory Certification Data List
 * Type: org.bluetooth.characteristic.ieee_11073-20601_regulatory_certification_data_list
 * UUID: 2A2A
 * Last Modified: 2011-05-24
 * Revision: None
 */
public class RegCertDataList extends CharacteristicBase {
    /**
     * IEEE Regulatory Certification Data List UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A2A"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Data
     * Requirement: Mandatory
     * Format: reg-cert-data-list
     * Unit: None
     */
    private byte[] mData = new byte[0];


    /**
     * Create a RegCertDataList characteristic object.
     */
    public RegCertDataList() {
        setCharacteristic(null);
        setData("");
    }

    /**
     * Create a RegCertDataList characteristic object and init value.
     *
     * @param value Initial value
     */
    public RegCertDataList(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a RegCertDataList characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public RegCertDataList(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a RegCertDataList characteristic object.
     *
     * @param data Data
     */
    public RegCertDataList(
            String data) {
        setCharacteristic(null);
        setData(data);
    }

    /**
     * Create a RegCertDataList characteristic object.
     *
     * @param data Data
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public RegCertDataList(
            String data,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setData(data);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get RegCertDataList characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportData()
                        ? mData.length : 0);
    }

    /**
     * Get RegCertDataList characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get RegCertDataList characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportData()) {
            int fieldLen = mData.length;
            System.arraycopy(mData, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set RegCertDataList characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportData()) {
            mData = new byte[value.length - srcPos];
            int fieldLen = mData.length;
            System.arraycopy(value, srcPos, mData, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get Data field value with String format.
     *
     * @return Data field value
     */
    public String getData() {
        return FormatUtils.utf8sToString(mData);
    }

    /**
     * Set Data field value by String format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Data field
     * @return  True
     */
    public boolean setData(String value) {
        mData = FormatUtils.stringToUtf8s(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if RegCertDataList support Data field.
     *
     * @return  True, if RegCertDataList support Data field.
     */
    public boolean isSupportData() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

