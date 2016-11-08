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
 * Public API for the PnP ID Bluetooth GATT Characteristic.
 *
 * <p>This class provides PnP ID Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: PnP ID
 * Type: org.bluetooth.characteristic.pnp_id
 * UUID: 2A50
 * Last Modified: 2011-11-13
 * Revision: None
 */
public class PnpId extends CharacteristicBase {
    /**
     * PnP ID UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A50"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Vendor ID Source
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mVendorIdSource = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Vendor ID
     * Requirement: Mandatory
     * Format: uint16
     * Unit: None
     */
    private byte[] mVendorId = new byte[FormatUtils.UINT16_SIZE];

    /*
     * Field: Product ID
     * Requirement: Mandatory
     * Format: uint16
     * Unit: None
     */
    private byte[] mProductId = new byte[FormatUtils.UINT16_SIZE];

    /*
     * Field: Product Version
     * Requirement: Mandatory
     * Format: uint16
     * Unit: None
     */
    private byte[] mProductVersion = new byte[FormatUtils.UINT16_SIZE];


    /**
     * Create a PnpId characteristic object.
     */
    public PnpId() {
        setCharacteristic(null);
        setVendorIdSource(1);
        setVendorId(0);
        setProductId(0);
        setProductVersion(0);
    }

    /**
     * Create a PnpId characteristic object and init value.
     *
     * @param value Initial value
     */
    public PnpId(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a PnpId characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public PnpId(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a PnpId characteristic object.
     *
     * @param vendorIdSource Vendor ID Source
     * @param vendorId Vendor ID
     * @param productId Product ID
     * @param productVersion Product Version
     */
    public PnpId(
            int vendorIdSource,
            int vendorId,
            int productId,
            int productVersion) {
        setCharacteristic(null);
        setVendorIdSource(vendorIdSource);
        setVendorId(vendorId);
        setProductId(productId);
        setProductVersion(productVersion);
    }

    /**
     * Create a PnpId characteristic object.
     *
     * @param vendorIdSource Vendor ID Source
     * @param vendorId Vendor ID
     * @param productId Product ID
     * @param productVersion Product Version
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public PnpId(
            int vendorIdSource,
            int vendorId,
            int productId,
            int productVersion,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setVendorIdSource(vendorIdSource);
        setVendorId(vendorId);
        setProductId(productId);
        setProductVersion(productVersion);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get PnpId characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportVendorIdSource()
                        ? mVendorIdSource.length : 0)
                + (isSupportVendorId()
                        ? mVendorId.length : 0)
                + (isSupportProductId()
                        ? mProductId.length : 0)
                + (isSupportProductVersion()
                        ? mProductVersion.length : 0);
    }

    /**
     * Get PnpId characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get PnpId characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportVendorIdSource()) {
            int fieldLen = mVendorIdSource.length;
            System.arraycopy(mVendorIdSource, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportVendorId()) {
            int fieldLen = mVendorId.length;
            System.arraycopy(mVendorId, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportProductId()) {
            int fieldLen = mProductId.length;
            System.arraycopy(mProductId, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportProductVersion()) {
            int fieldLen = mProductVersion.length;
            System.arraycopy(mProductVersion, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set PnpId characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportVendorIdSource()) {
            int fieldLen = mVendorIdSource.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mVendorIdSource, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportVendorId()) {
            int fieldLen = mVendorId.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mVendorId, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportProductId()) {
            int fieldLen = mProductId.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mProductId, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportProductVersion()) {
            int fieldLen = mProductVersion.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mProductVersion, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get VendorIdSource field value with int format.
     *
     * @return VendorIdSource field value
     */
    public int getVendorIdSource() {
        return FormatUtils.uint8ToInt(mVendorIdSource);
    }

    /**
     * Set VendorIdSource field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to VendorIdSource field
     * @return      True, if the value has been set successfully
     */
    public boolean setVendorIdSource(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mVendorIdSource = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if PnpId support VendorIdSource field.
     *
     * @return  True, if PnpId support VendorIdSource field.
     */
    public boolean isSupportVendorIdSource() {
        return true;
    }

    /**
     * Get VendorId field value with int format.
     *
     * @return VendorId field value
     */
    public int getVendorId() {
        return FormatUtils.uint16ToInt(mVendorId);
    }

    /**
     * Set VendorId field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to VendorId field
     * @return      True, if the value has been set successfully
     */
    public boolean setVendorId(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mVendorId = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if PnpId support VendorId field.
     *
     * @return  True, if PnpId support VendorId field.
     */
    public boolean isSupportVendorId() {
        return true;
    }

    /**
     * Get ProductId field value with int format.
     *
     * @return ProductId field value
     */
    public int getProductId() {
        return FormatUtils.uint16ToInt(mProductId);
    }

    /**
     * Set ProductId field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ProductId field
     * @return      True, if the value has been set successfully
     */
    public boolean setProductId(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mProductId = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if PnpId support ProductId field.
     *
     * @return  True, if PnpId support ProductId field.
     */
    public boolean isSupportProductId() {
        return true;
    }

    /**
     * Get ProductVersion field value with int format.
     *
     * @return ProductVersion field value
     */
    public int getProductVersion() {
        return FormatUtils.uint16ToInt(mProductVersion);
    }

    /**
     * Set ProductVersion field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ProductVersion field
     * @return      True, if the value has been set successfully
     */
    public boolean setProductVersion(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mProductVersion = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if PnpId support ProductVersion field.
     *
     * @return  True, if PnpId support ProductVersion field.
     */
    public boolean isSupportProductVersion() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

