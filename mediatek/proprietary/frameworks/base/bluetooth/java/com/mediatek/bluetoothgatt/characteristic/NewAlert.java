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
 * Public API for the New Alert Bluetooth GATT Characteristic.
 *
 * <p>This class provides New Alert Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: New Alert
 * Type: org.bluetooth.characteristic.new_alert
 * UUID: 2A46
 * Last Modified: None
 * Revision: None
 */
public class NewAlert extends CharacteristicBase {
    /**
     * New Alert UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A46"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Category ID
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.alert_category_id
     */
    private AlertCategoryId mCategoryId = new AlertCategoryId();

    /*
     * Field: Number of New Alert
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mNumberOfNewAlert = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Text String Information
     * Requirement: Optional
     * Format: utf8s
     * Unit: None
     */
    private byte[] mTextStringInformation = new byte[0];


    /**
     * Create a NewAlert characteristic object.
     */
    public NewAlert() {
        setCharacteristic(null);
        setNumberOfNewAlert(0);
        setTextStringInformation("");
    }

    /**
     * Create a NewAlert characteristic object and init value.
     *
     * @param value Initial value
     */
    public NewAlert(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a NewAlert characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public NewAlert(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a NewAlert characteristic object.
     *
     * @param categoryId Category ID
     * @param numberOfNewAlert Number of New Alert
     * @param textStringInformation Text String Information
     */
    public NewAlert(
            AlertCategoryId categoryId,
            int numberOfNewAlert,
            String textStringInformation) {
        setCharacteristic(null);
        setCategoryId(categoryId);
        setNumberOfNewAlert(numberOfNewAlert);
        setTextStringInformation(textStringInformation);
    }

    /**
     * Create a NewAlert characteristic object.
     *
     * @param categoryId Category ID
     * @param numberOfNewAlert Number of New Alert
     * @param textStringInformation Text String Information
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public NewAlert(
            AlertCategoryId categoryId,
            int numberOfNewAlert,
            String textStringInformation,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCategoryId(categoryId);
        setNumberOfNewAlert(numberOfNewAlert);
        setTextStringInformation(textStringInformation);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get NewAlert characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCategoryId()
                        ? mCategoryId.length() : 0)
                + (isSupportNumberOfNewAlert()
                        ? mNumberOfNewAlert.length : 0)
                + (isSupportTextStringInformation()
                        ? mTextStringInformation.length : 0);
    }

    /**
     * Get NewAlert characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get NewAlert characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportCategoryId()) {
            int fieldLen = mCategoryId.length();
            System.arraycopy(mCategoryId.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportNumberOfNewAlert()) {
            int fieldLen = mNumberOfNewAlert.length;
            System.arraycopy(mNumberOfNewAlert, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportTextStringInformation()) {
            int fieldLen = mTextStringInformation.length;
            System.arraycopy(mTextStringInformation, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set NewAlert characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportCategoryId()) {
            int fieldLen = mCategoryId.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mCategoryId.setValue(buf);
        }

        if (isSupportNumberOfNewAlert()) {
            int fieldLen = mNumberOfNewAlert.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mNumberOfNewAlert, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportTextStringInformation()) {
            mTextStringInformation = new byte[value.length - srcPos];
            int fieldLen = mTextStringInformation.length;
            System.arraycopy(value, srcPos, mTextStringInformation, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get CategoryId field value with AlertCategoryId format.
     *
     * @return CategoryId field value
     */
    public AlertCategoryId getCategoryId() {
        return mCategoryId;
    }

    /**
     * Set CategoryId field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryId field
     * @return      True, if the value has been set successfully
     */
    public boolean setCategoryId(byte[] value) {
        if (!mCategoryId.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set CategoryId field value by AlertCategoryId format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryId field
     * @return      True, if the value has been set successfully
     */
    public boolean setCategoryId(AlertCategoryId value) {
        if (!mCategoryId.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if NewAlert support CategoryId field.
     *
     * @return  True, if NewAlert support CategoryId field.
     */
    public boolean isSupportCategoryId() {
        return true;
    }

    /**
     * Get NumberOfNewAlert field value with int format.
     *
     * @return NumberOfNewAlert field value
     */
    public int getNumberOfNewAlert() {
        return FormatUtils.uint8ToInt(mNumberOfNewAlert);
    }

    /**
     * Set NumberOfNewAlert field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to NumberOfNewAlert field
     * @return      True, if the value has been set successfully
     */
    public boolean setNumberOfNewAlert(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mNumberOfNewAlert = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if NewAlert support NumberOfNewAlert field.
     *
     * @return  True, if NewAlert support NumberOfNewAlert field.
     */
    public boolean isSupportNumberOfNewAlert() {
        return true;
    }

    /**
     * Get TextStringInformation field value with String format.
     *
     * @return TextStringInformation field value
     */
    public String getTextStringInformation() {
        return FormatUtils.utf8sToString(mTextStringInformation);
    }

    /**
     * Set TextStringInformation field value by String format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TextStringInformation field
     * @return  True
     */
    public boolean setTextStringInformation(String value) {
        mTextStringInformation = FormatUtils.stringToUtf8s(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if NewAlert support TextStringInformation field.
     *
     * @return  True, if NewAlert support TextStringInformation field.
     */
    public boolean isSupportTextStringInformation() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

