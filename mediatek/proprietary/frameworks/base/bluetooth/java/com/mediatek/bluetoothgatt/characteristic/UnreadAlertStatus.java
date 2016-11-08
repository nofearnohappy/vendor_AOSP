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
 * Public API for the Unread Alert Status Bluetooth GATT Characteristic.
 *
 * <p>This class provides Unread Alert Status Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Unread Alert Status
 * Type: org.bluetooth.characteristic.unread_alert_status
 * UUID: 2A45
 * Last Modified: None
 * Revision: None
 */
public class UnreadAlertStatus extends CharacteristicBase {
    /**
     * Unread Alert Status UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A45"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Category ID
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.alert_category_id
     */
    private AlertCategoryId mCategoryId = new AlertCategoryId();

    /*
     * Field: Unread count
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mUnreadCount = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a UnreadAlertStatus characteristic object.
     */
    public UnreadAlertStatus() {
        setCharacteristic(null);
        setUnreadCount(0);
    }

    /**
     * Create a UnreadAlertStatus characteristic object and init value.
     *
     * @param value Initial value
     */
    public UnreadAlertStatus(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a UnreadAlertStatus characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public UnreadAlertStatus(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a UnreadAlertStatus characteristic object.
     *
     * @param categoryId Category ID
     * @param unreadCount Unread count
     */
    public UnreadAlertStatus(
            AlertCategoryId categoryId,
            int unreadCount) {
        setCharacteristic(null);
        setCategoryId(categoryId);
        setUnreadCount(unreadCount);
    }

    /**
     * Create a UnreadAlertStatus characteristic object.
     *
     * @param categoryId Category ID
     * @param unreadCount Unread count
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public UnreadAlertStatus(
            AlertCategoryId categoryId,
            int unreadCount,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCategoryId(categoryId);
        setUnreadCount(unreadCount);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get UnreadAlertStatus characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCategoryId()
                        ? mCategoryId.length() : 0)
                + (isSupportUnreadCount()
                        ? mUnreadCount.length : 0);
    }

    /**
     * Get UnreadAlertStatus characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get UnreadAlertStatus characteristic value.
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

        if (isSupportUnreadCount()) {
            int fieldLen = mUnreadCount.length;
            System.arraycopy(mUnreadCount, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set UnreadAlertStatus characteristic value.
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

        if (isSupportUnreadCount()) {
            int fieldLen = mUnreadCount.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mUnreadCount, 0, fieldLen);
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
     * Return true if UnreadAlertStatus support CategoryId field.
     *
     * @return  True, if UnreadAlertStatus support CategoryId field.
     */
    public boolean isSupportCategoryId() {
        return true;
    }

    /**
     * Get UnreadCount field value with int format.
     *
     * @return UnreadCount field value
     */
    public int getUnreadCount() {
        return FormatUtils.uint8ToInt(mUnreadCount);
    }

    /**
     * Set UnreadCount field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to UnreadCount field
     * @return      True, if the value has been set successfully
     */
    public boolean setUnreadCount(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mUnreadCount = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if UnreadAlertStatus support UnreadCount field.
     *
     * @return  True, if UnreadAlertStatus support UnreadCount field.
     */
    public boolean isSupportUnreadCount() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

