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
 * Public API for the Alert Status Bluetooth GATT Characteristic.
 *
 * <p>This class provides Alert Status Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Alert Status
 * Type: org.bluetooth.characteristic.alert_status
 * UUID: 2A3F
 * Last Modified: 2011-08-30
 * Revision: None
 */
public class AlertStatus extends CharacteristicBase {
    /**
     * Alert Status UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A3F"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Alert Status Bit Field Definition: State not active.
     */
    public static final int AS_STATE_RINGER_ACTIVE = (1 << 0);

    /**
     * Alert Status Bit Field Definition: State active.
     */
    public static final int AS_STATE_VIBRATE_ACTIVE = (1 << 1);

    /**
     * Alert Status Bit Field Definition: State active.
     */
    public static final int AS_STATE_DISPLAY_ALERT_ACTIVE = (1 << 2);
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Alert Status
     * Requirement: Mandatory
     * Format: uint8
     */
    private BfAlertStatus mAlertStatus = new BfAlertStatus();


    /**
     * Create a AlertStatus characteristic object.
     */
    public AlertStatus() {
        setCharacteristic(null);
        setAlertStatus(new BfAlertStatus());
    }

    /**
     * Create a AlertStatus characteristic object and init value.
     *
     * @param value Initial value
     */
    public AlertStatus(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a AlertStatus characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertStatus(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a AlertStatus characteristic object.
     *
     * @param alertStatus Alert Status
     */
    public AlertStatus(
            BfAlertStatus alertStatus) {
        setCharacteristic(null);
        setAlertStatus(alertStatus);
    }

    /**
     * Create a AlertStatus characteristic object.
     *
     * @param alertStatus Alert Status
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertStatus(
            BfAlertStatus alertStatus,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setAlertStatus(alertStatus);
    }

    // Customized Start: Constructors ..............................................................
    /**
     * Create a AlertStatus characteristic object and init value.
     *
     * @param alertStatus Alert Status
     */
    public AlertStatus(byte alertStatus) {
        setCharacteristic(null);
        setAlertStatus(new BfAlertStatus(new byte[]{alertStatus}));
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get AlertStatus characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportAlertStatus()
                        ? mAlertStatus.length() : 0);
    }

    /**
     * Get AlertStatus characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get AlertStatus characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportAlertStatus()) {
            int fieldLen = mAlertStatus.length();
            System.arraycopy(mAlertStatus.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set AlertStatus characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportAlertStatus()) {
            int fieldLen = mAlertStatus.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mAlertStatus.setByteArray(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get AlertStatus field value with BfAlertStatus format.
     *
     * @return AlertStatus field value
     */
    public BfAlertStatus getAlertStatus() {
        return mAlertStatus;
    }

    /**
     * Set AlertStatus field value by BfAlertStatus format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to AlertStatus field
     * @return      True
     */
    public boolean setAlertStatus(BfAlertStatus value) {
        mAlertStatus = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if AlertStatus support AlertStatus field.
     *
     * @return  True, if AlertStatus support AlertStatus field.
     */
    public boolean isSupportAlertStatus() {
        return true;
    }

    /**
     * This class provides AlertStatus BitField operations based on
     * specific definition.
     */
    public class BfAlertStatus extends BitField {
        private static final int sLength = FormatUtils.UINT8_SIZE;

        /**
         * Get BfAlertStatus BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfAlertStatus BitField object.
         */
        public BfAlertStatus() { super(sLength * 8); }

        /**
         * Create a BfAlertStatus BitField object and init value.
         *
         * @param value Initial value
         */
        public BfAlertStatus(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get RingerState bit field value.
         *
         * @return Bit field value
         */
        public int getRingerState() {
            return getValue(0, 0);
        }

        /**
         * Set RingerState bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to RingerState bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setRingerState(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get VibrateState bit field value.
         *
         * @return Bit field value
         */
        public int getVibrateState() {
            return getValue(1, 1);
        }

        /**
         * Set VibrateState bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to VibrateState bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setVibrateState(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get DisplayAlertStatus bit field value.
         *
         * @return Bit field value
         */
        public int getDisplayAlertStatus() {
            return getValue(2, 2);
        }

        /**
         * Set DisplayAlertStatus bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to DisplayAlertStatus bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setDisplayAlertStatus(int value) {
            if (!setValue(2, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

