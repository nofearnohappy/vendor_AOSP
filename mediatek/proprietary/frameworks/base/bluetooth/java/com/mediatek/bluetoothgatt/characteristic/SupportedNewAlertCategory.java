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
 * Public API for the Supported New Alert Category Bluetooth GATT Characteristic.
 *
 * <p>This class provides Supported New Alert Category Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Supported New Alert Category
 * Type: org.bluetooth.characteristic.supported_new_alert_category
 * UUID: 2A47
 * Last Modified: None
 * Revision: None
 */
public class SupportedNewAlertCategory extends CharacteristicBase {
    /**
     * Supported New Alert Category UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A47"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Category ID Bit Mask
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.alert_category_id_bit_mask
     */
    private AlertCategoryIdBitMask mCategoryIdBitMask = new AlertCategoryIdBitMask();


    /**
     * Create a SupportedNewAlertCategory characteristic object.
     */
    public SupportedNewAlertCategory() {
        setCharacteristic(null);

    }

    /**
     * Create a SupportedNewAlertCategory characteristic object and init value.
     *
     * @param value Initial value
     */
    public SupportedNewAlertCategory(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a SupportedNewAlertCategory characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SupportedNewAlertCategory(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a SupportedNewAlertCategory characteristic object.
     *
     * @param categoryIdBitMask Category ID Bit Mask
     */
    public SupportedNewAlertCategory(
            AlertCategoryIdBitMask categoryIdBitMask) {
        setCharacteristic(null);
        setCategoryIdBitMask(categoryIdBitMask);
    }

    /**
     * Create a SupportedNewAlertCategory characteristic object.
     *
     * @param categoryIdBitMask Category ID Bit Mask
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SupportedNewAlertCategory(
            AlertCategoryIdBitMask categoryIdBitMask,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCategoryIdBitMask(categoryIdBitMask);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get SupportedNewAlertCategory characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCategoryIdBitMask()
                        ? mCategoryIdBitMask.length() : 0);
    }

    /**
     * Get SupportedNewAlertCategory characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get SupportedNewAlertCategory characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportCategoryIdBitMask()) {
            int fieldLen = mCategoryIdBitMask.length();
            System.arraycopy(mCategoryIdBitMask.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set SupportedNewAlertCategory characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportCategoryIdBitMask()) {
            int fieldLen = mCategoryIdBitMask.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mCategoryIdBitMask.setValue(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get CategoryIdBitMask field value with AlertCategoryIdBitMask format.
     *
     * @return CategoryIdBitMask field value
     */
    public AlertCategoryIdBitMask getCategoryIdBitMask() {
        return mCategoryIdBitMask;
    }

    /**
     * Set CategoryIdBitMask field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryIdBitMask field
     * @return      True, if the value has been set successfully
     */
    public boolean setCategoryIdBitMask(byte[] value) {
        if (!mCategoryIdBitMask.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set CategoryIdBitMask field value by AlertCategoryIdBitMask format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryIdBitMask field
     * @return      True, if the value has been set successfully
     */
    public boolean setCategoryIdBitMask(AlertCategoryIdBitMask value) {
        if (!mCategoryIdBitMask.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if SupportedNewAlertCategory support CategoryIdBitMask field.
     *
     * @return  True, if SupportedNewAlertCategory support CategoryIdBitMask field.
     */
    public boolean isSupportCategoryIdBitMask() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

