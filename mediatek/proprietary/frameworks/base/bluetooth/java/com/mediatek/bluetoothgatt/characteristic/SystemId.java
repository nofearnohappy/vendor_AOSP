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
 * Public API for the System ID Bluetooth GATT Characteristic.
 *
 * <p>This class provides System ID Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: System ID
 * Type: org.bluetooth.characteristic.system_id
 * UUID: 2A23
 * Last Modified: 2011-05-24
 * Revision: None
 */
public class SystemId extends CharacteristicBase {
    /**
     * System ID UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A23"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Manufacturer Identifier
     * Requirement: Mandatory
     * Format: uint40
     * Unit: None
     */
    private byte[] mManufacturerIdentifier = new byte[FormatUtils.UINT40_SIZE];

    /*
     * Field: Organizationally Unique Identifier
     * Requirement: Mandatory
     * Format: uint24
     * Unit: None
     */
    private byte[] mOrganizationallyUniqueIdentifier = new byte[FormatUtils.UINT24_SIZE];


    /**
     * Create a SystemId characteristic object.
     */
    public SystemId() {
        setCharacteristic(null);
        setManufacturerIdentifier(0);
        setOrganizationallyUniqueIdentifier(0);
    }

    /**
     * Create a SystemId characteristic object and init value.
     *
     * @param value Initial value
     */
    public SystemId(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a SystemId characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SystemId(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a SystemId characteristic object.
     *
     * @param manufacturerIdentifier Manufacturer Identifier
     * @param organizationallyUniqueIdentifier Organizationally Unique Identifier
     */
    public SystemId(
            long manufacturerIdentifier,
            int organizationallyUniqueIdentifier) {
        setCharacteristic(null);
        setManufacturerIdentifier(manufacturerIdentifier);
        setOrganizationallyUniqueIdentifier(organizationallyUniqueIdentifier);
    }

    /**
     * Create a SystemId characteristic object.
     *
     * @param manufacturerIdentifier Manufacturer Identifier
     * @param organizationallyUniqueIdentifier Organizationally Unique Identifier
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SystemId(
            long manufacturerIdentifier,
            int organizationallyUniqueIdentifier,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setManufacturerIdentifier(manufacturerIdentifier);
        setOrganizationallyUniqueIdentifier(organizationallyUniqueIdentifier);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get SystemId characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportManufacturerIdentifier()
                        ? mManufacturerIdentifier.length : 0)
                + (isSupportOrganizationallyUniqueIdentifier()
                        ? mOrganizationallyUniqueIdentifier.length : 0);
    }

    /**
     * Get SystemId characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get SystemId characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportManufacturerIdentifier()) {
            int fieldLen = mManufacturerIdentifier.length;
            System.arraycopy(mManufacturerIdentifier, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportOrganizationallyUniqueIdentifier()) {
            int fieldLen = mOrganizationallyUniqueIdentifier.length;
            System.arraycopy(mOrganizationallyUniqueIdentifier, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set SystemId characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportManufacturerIdentifier()) {
            int fieldLen = mManufacturerIdentifier.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mManufacturerIdentifier, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportOrganizationallyUniqueIdentifier()) {
            int fieldLen = mOrganizationallyUniqueIdentifier.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mOrganizationallyUniqueIdentifier, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get ManufacturerIdentifier field value with long format.
     *
     * @return ManufacturerIdentifier field value
     */
    public long getManufacturerIdentifier() {
        return FormatUtils.uint40ToLong(mManufacturerIdentifier);
    }

    /**
     * Set ManufacturerIdentifier field value by long format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ManufacturerIdentifier field
     * @return      True, if the value has been set successfully
     */
    public boolean setManufacturerIdentifier(long value) {
        if (!FormatUtils.uint40RangeCheck(value)) {
            return false;
        }
        mManufacturerIdentifier = FormatUtils.longToUint40(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if SystemId support ManufacturerIdentifier field.
     *
     * @return  True, if SystemId support ManufacturerIdentifier field.
     */
    public boolean isSupportManufacturerIdentifier() {
        return true;
    }

    /**
     * Get OrganizationallyUniqueIdentifier field value with int format.
     *
     * @return OrganizationallyUniqueIdentifier field value
     */
    public int getOrganizationallyUniqueIdentifier() {
        return FormatUtils.uint24ToInt(mOrganizationallyUniqueIdentifier);
    }

    /**
     * Set OrganizationallyUniqueIdentifier field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to OrganizationallyUniqueIdentifier field
     * @return      True, if the value has been set successfully
     */
    public boolean setOrganizationallyUniqueIdentifier(int value) {
        if (!FormatUtils.uint24RangeCheck(value)) {
            return false;
        }
        mOrganizationallyUniqueIdentifier = FormatUtils.intToUint24(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if SystemId support OrganizationallyUniqueIdentifier field.
     *
     * @return  True, if SystemId support OrganizationallyUniqueIdentifier field.
     */
    public boolean isSupportOrganizationallyUniqueIdentifier() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

