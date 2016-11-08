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
 * Public API for the Alert Notification Control Point Bluetooth GATT Characteristic.
 *
 * <p>This class provides Alert Notification Control Point Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Alert Notification Control Point
 * Type: org.bluetooth.characteristic.alert_notification_control_point
 * UUID: 2A44
 * Last Modified: None
 * Revision: None
 */
public class AlertNotificationControlPoint extends CharacteristicBase {
    /**
     * Alert Notification Control Point UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A44"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Command ID Enumerations: Enable New Incoming Alert Notification.
     */
    public static final int CMD_ENABLE_NEW_ALERT_NOTIFICATION = 0;

    /**
     * Command ID Enumerations: Enable Unread Category Status Notification.
     */
    public static final int CMD_ENABLE_UNREAD_ALERT_NOTIFICATION = 1;

    /**
     * Command ID Enumerations: Disable New Incoming Alert Notification.
     */
    public static final int CMD_DISABLE_NEW_ALERT_NOTIFICATION = 2;

    /**
     * Command ID Enumerations: Disable Unread Category Status Notification.
     */
    public static final int CMD_DISABLE_UNREAD_ALERT_NOTIFICATION = 3;

    /**
     * Command ID Enumerations: Notify New Incoming Alert immediately.
     */
    public static final int CMD_NOTIFY_NEW_ALERT_IMMEDIATELY = 4;

    /**
     * Command ID Enumerations: Notify Unread Category Status immediately.
     */
    public static final int CMD_NOTIFY_UNREAD_ALERT_IMMEDIATELY = 5;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Command ID
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mCommandId = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Category ID
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.alert_category_id
     */
    private AlertCategoryId mCategoryId = new AlertCategoryId();


    /**
     * Create a AlertNotificationControlPoint characteristic object.
     */
    public AlertNotificationControlPoint() {
        setCharacteristic(null);
        setCommandId(0);
    }

    /**
     * Create a AlertNotificationControlPoint characteristic object and init value.
     *
     * @param value Initial value
     */
    public AlertNotificationControlPoint(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a AlertNotificationControlPoint characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertNotificationControlPoint(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a AlertNotificationControlPoint characteristic object.
     *
     * @param commandId Command ID
     * @param categoryId Category ID
     */
    public AlertNotificationControlPoint(
            int commandId,
            AlertCategoryId categoryId) {
        setCharacteristic(null);
        setCommandId(commandId);
        setCategoryId(categoryId);
    }

    /**
     * Create a AlertNotificationControlPoint characteristic object.
     *
     * @param commandId Command ID
     * @param categoryId Category ID
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertNotificationControlPoint(
            int commandId,
            AlertCategoryId categoryId,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCommandId(commandId);
        setCategoryId(categoryId);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get AlertNotificationControlPoint characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCommandId()
                        ? mCommandId.length : 0)
                + (isSupportCategoryId()
                        ? mCategoryId.length() : 0);
    }

    /**
     * Get AlertNotificationControlPoint characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get AlertNotificationControlPoint characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportCommandId()) {
            int fieldLen = mCommandId.length;
            System.arraycopy(mCommandId, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportCategoryId()) {
            int fieldLen = mCategoryId.length();
            System.arraycopy(mCategoryId.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set AlertNotificationControlPoint characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportCommandId()) {
            int fieldLen = mCommandId.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mCommandId, 0, fieldLen);
            srcPos += fieldLen;
        }

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

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get CommandId field value with int format.
     *
     * @return CommandId field value
     */
    public int getCommandId() {
        return FormatUtils.uint8ToInt(mCommandId);
    }

    /**
     * Set CommandId field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CommandId field
     * @return      True, if the value has been set successfully
     */
    public boolean setCommandId(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mCommandId = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if AlertNotificationControlPoint support CommandId field.
     *
     * @return  True, if AlertNotificationControlPoint support CommandId field.
     */
    public boolean isSupportCommandId() {
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
     * Return true if AlertNotificationControlPoint support CategoryId field.
     *
     * @return  True, if AlertNotificationControlPoint support CategoryId field.
     */
    public boolean isSupportCategoryId() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

