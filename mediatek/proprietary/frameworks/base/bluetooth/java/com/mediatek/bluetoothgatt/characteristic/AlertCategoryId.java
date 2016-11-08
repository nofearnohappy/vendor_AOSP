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
 * Public API for the Alert Category ID Bluetooth GATT Characteristic.
 *
 * <p>This class provides Alert Category ID Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Alert Category ID
 * Type: org.bluetooth.characteristic.alert_category_id
 * UUID: 2A43
 * Last Modified: None
 * Revision: None
 */
public class AlertCategoryId extends CharacteristicBase {
    /**
     * Alert Category ID UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A43"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Category ID: Simple Alert: General text alert or non-text alert.
     */
    public static final int CATEGORY_SIMPLE_ALERT = 0;

    /**
     * Category ID: Email: Alert when Email messages arrives.
     */
    public static final int CATEGORY_EMAIL = 1;

    /**
     * Category ID: News: News feeds such as RSS, Atom.
     */
    public static final int CATEGORY_NEWS = 2;

    /**
     * Category ID: Call: Incoming call.
     */
    public static final int CATEGORY_CALL = 3;

    /**
     * Category ID: Missed call: Missed Call.
     */
    public static final int CATEGORY_MISSED_CALL = 4;

    /**
     * Category ID: SMS/MMS: SMS/MMS message arrives.
     */
    public static final int CATEGORY_SMS_MMS = 5;

    /**
     * Category ID: Voice mail: Voice mail.
     */
    public static final int CATEGORY_VOICE_MAIL = 6;

    /**
     * Category ID: Schedule: Alert occurred on calendar, planner.
     */
    public static final int CATEGORY_SCHEDULE = 7;

    /**
     * Category ID: High Prioritized Alert: Alert that should be handled as high priority.
     */
    public static final int CATEGORY_HIGH_PRIORITIZED_ALERT = 8;

    /**
     * Category ID: Instant Message: Alert for incoming instant messages.
     */
    public static final int CATEGORY_INSTANT_MESSAGE = 9;

    /**
     * CATEGORY_ALL is using for Alert-Notification-Control-Point to config all categories.
     */
    public static final int CATEGORY_ALL = 0xFF;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Category ID
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mCategoryId = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a AlertCategoryId characteristic object.
     */
    public AlertCategoryId() {
        setCharacteristic(null);
        setCategoryId(0);
    }

    /**
     * Create a AlertCategoryId characteristic object and init value.
     *
     * @param value Initial value
     */
    public AlertCategoryId(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a AlertCategoryId characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertCategoryId(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a AlertCategoryId characteristic object.
     *
     * @param categoryId Category ID
     */
    public AlertCategoryId(
            int categoryId) {
        setCharacteristic(null);
        setCategoryId(categoryId);
    }

    /**
     * Create a AlertCategoryId characteristic object.
     *
     * @param categoryId Category ID
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertCategoryId(
            int categoryId,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCategoryId(categoryId);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get AlertCategoryId characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCategoryId()
                        ? mCategoryId.length : 0);
    }

    /**
     * Get AlertCategoryId characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get AlertCategoryId characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportCategoryId()) {
            int fieldLen = mCategoryId.length;
            System.arraycopy(mCategoryId, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set AlertCategoryId characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportCategoryId()) {
            int fieldLen = mCategoryId.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mCategoryId, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get CategoryId field value with int format.
     *
     * @return CategoryId field value
     */
    public int getCategoryId() {
        return FormatUtils.uint8ToInt(mCategoryId);
    }

    /**
     * Set CategoryId field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryId field
     * @return      True, if the value has been set successfully
     */
    public boolean setCategoryId(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mCategoryId = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if AlertCategoryId support CategoryId field.
     *
     * @return  True, if AlertCategoryId support CategoryId field.
     */
    public boolean isSupportCategoryId() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

