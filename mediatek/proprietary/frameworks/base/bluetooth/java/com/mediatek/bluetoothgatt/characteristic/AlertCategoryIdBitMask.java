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
 * Public API for the Alert Category ID Bit Mask Bluetooth GATT Characteristic.
 *
 * <p>This class provides Alert Category ID Bit Mask Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Alert Category ID Bit Mask
 * Type: org.bluetooth.characteristic.alert_category_id_bit_mask
 * UUID: 2A42
 * Last Modified: None
 * Revision: None
 */
public class AlertCategoryIdBitMask extends CharacteristicBase {
    /**
     * Alert Category ID Bit Mask UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A42"));

    // Customized Start: Constant Definition .......................................................
    public static final int CATEGORY_BM_SIMPLE_ALERT = (1 << 0);
    public static final int CATEGORY_BM_EMAIL = (1 << 1);
    public static final int CATEGORY_BM_NEWS = (1 << 2);
    public static final int CATEGORY_BM_CALL = (1 << 3);
    public static final int CATEGORY_BM_MISSED_CALL = (1 << 4);
    public static final int CATEGORY_BM_SMS_MMS = (1 << 5);
    public static final int CATEGORY_BM_VOICE_MAIL = (1 << 6);
    public static final int CATEGORY_BM_SCHEDULE = (1 << 7);
    public static final int CATEGORY_BM_HIGH_PRIORITIZED_ALERT = (1 << 8);
    public static final int CATEGORY_BM_INSTANT_MESSAGE = (1 << 9);
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Category ID Bit Mask 0
     * Requirement: Mandatory
     * Format: uint8
     */
    private BfCategoryIdBitMask0 mCategoryIdBitMask0 = new BfCategoryIdBitMask0();

    /*
     * Field: Category ID Bit Mask 1
     * Requirement: Optional
     * Format: uint8
     */
    private BfCategoryIdBitMask1 mCategoryIdBitMask1 = new BfCategoryIdBitMask1();


    /**
     * Create a AlertCategoryIdBitMask characteristic object.
     */
    public AlertCategoryIdBitMask() {
        setCharacteristic(null);
        setCategoryIdBitMask0(new BfCategoryIdBitMask0());
        setCategoryIdBitMask1(new BfCategoryIdBitMask1());
    }

    /**
     * Create a AlertCategoryIdBitMask characteristic object and init value.
     *
     * @param value Initial value
     */
    public AlertCategoryIdBitMask(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a AlertCategoryIdBitMask characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertCategoryIdBitMask(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a AlertCategoryIdBitMask characteristic object.
     *
     * @param categoryIdBitMask0 Category ID Bit Mask 0
     * @param categoryIdBitMask1 Category ID Bit Mask 1
     */
    public AlertCategoryIdBitMask(
            BfCategoryIdBitMask0 categoryIdBitMask0,
            BfCategoryIdBitMask1 categoryIdBitMask1) {
        setCharacteristic(null);
        setCategoryIdBitMask0(categoryIdBitMask0);
        setCategoryIdBitMask1(categoryIdBitMask1);
    }

    /**
     * Create a AlertCategoryIdBitMask characteristic object.
     *
     * @param categoryIdBitMask0 Category ID Bit Mask 0
     * @param categoryIdBitMask1 Category ID Bit Mask 1
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public AlertCategoryIdBitMask(
            BfCategoryIdBitMask0 categoryIdBitMask0,
            BfCategoryIdBitMask1 categoryIdBitMask1,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCategoryIdBitMask0(categoryIdBitMask0);
        setCategoryIdBitMask1(categoryIdBitMask1);
    }

    // Customized Start: Constructors ..............................................................
    /**
     * Create a AlertCategoryIdBitMask characteristic object.
     *
     * @param categoryIdBitMask Category ID Bit Mask (32 bits)
     */
    public AlertCategoryIdBitMask(int categoryIdBitMask) {
        byte bitMask0 = (byte) (categoryIdBitMask & 0xff);
        byte bitMask1 = (byte) ((categoryIdBitMask >> 8) & 0xff);

        setCharacteristic(null);
        setCategoryIdBitMask0(new BfCategoryIdBitMask0(new byte[]{bitMask0}));
        setCategoryIdBitMask1(new BfCategoryIdBitMask1(new byte[]{bitMask1}));
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get AlertCategoryIdBitMask characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCategoryIdBitMask0()
                        ? mCategoryIdBitMask0.length() : 0)
                + (isSupportCategoryIdBitMask1()
                        ? mCategoryIdBitMask1.length() : 0);
    }

    /**
     * Get AlertCategoryIdBitMask characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get AlertCategoryIdBitMask characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportCategoryIdBitMask0()) {
            int fieldLen = mCategoryIdBitMask0.length();
            System.arraycopy(mCategoryIdBitMask0.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportCategoryIdBitMask1()) {
            int fieldLen = mCategoryIdBitMask1.length();
            System.arraycopy(mCategoryIdBitMask1.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set AlertCategoryIdBitMask characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportCategoryIdBitMask0()) {
            int fieldLen = mCategoryIdBitMask0.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mCategoryIdBitMask0.setByteArray(buf);
        }

        if (isSupportCategoryIdBitMask1()) {
            int fieldLen = mCategoryIdBitMask1.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mCategoryIdBitMask1.setByteArray(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get CategoryIdBitMask0 field value with BfCategoryIdBitMask0 format.
     *
     * @return CategoryIdBitMask0 field value
     */
    public BfCategoryIdBitMask0 getCategoryIdBitMask0() {
        return mCategoryIdBitMask0;
    }

    /**
     * Set CategoryIdBitMask0 field value by BfCategoryIdBitMask0 format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryIdBitMask0 field
     * @return      True
     */
    public boolean setCategoryIdBitMask0(BfCategoryIdBitMask0 value) {
        mCategoryIdBitMask0 = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if AlertCategoryIdBitMask support CategoryIdBitMask0 field.
     *
     * @return  True, if AlertCategoryIdBitMask support CategoryIdBitMask0 field.
     */
    public boolean isSupportCategoryIdBitMask0() {
        return true;
    }

    /**
     * Get CategoryIdBitMask1 field value with BfCategoryIdBitMask1 format.
     *
     * @return CategoryIdBitMask1 field value
     */
    public BfCategoryIdBitMask1 getCategoryIdBitMask1() {
        return mCategoryIdBitMask1;
    }

    /**
     * Set CategoryIdBitMask1 field value by BfCategoryIdBitMask1 format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CategoryIdBitMask1 field
     * @return      True
     */
    public boolean setCategoryIdBitMask1(BfCategoryIdBitMask1 value) {
        mCategoryIdBitMask1 = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if AlertCategoryIdBitMask support CategoryIdBitMask1 field.
     *
     * @return  True, if AlertCategoryIdBitMask support CategoryIdBitMask1 field.
     */
    public boolean isSupportCategoryIdBitMask1() {
        return true;
    }

    /**
     * This class provides CategoryIdBitMask0 BitField operations based on
     * specific definition.
     */
    public class BfCategoryIdBitMask0 extends BitField {
        private static final int sLength = FormatUtils.UINT8_SIZE;

        /**
         * Get BfCategoryIdBitMask0 BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfCategoryIdBitMask0 BitField object.
         */
        public BfCategoryIdBitMask0() { super(sLength * 8); }

        /**
         * Create a BfCategoryIdBitMask0 BitField object and init value.
         *
         * @param value Initial value
         */
        public BfCategoryIdBitMask0(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get SimpleAlert bit field value.
         *
         * @return Bit field value
         */
        public int getSimpleAlert() {
            return getValue(0, 0);
        }

        /**
         * Set SimpleAlert bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to SimpleAlert bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setSimpleAlert(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get Email bit field value.
         *
         * @return Bit field value
         */
        public int getEmail() {
            return getValue(1, 1);
        }

        /**
         * Set Email bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to Email bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setEmail(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get News bit field value.
         *
         * @return Bit field value
         */
        public int getNews() {
            return getValue(2, 2);
        }

        /**
         * Set News bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to News bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setNews(int value) {
            if (!setValue(2, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get Call bit field value.
         *
         * @return Bit field value
         */
        public int getCall() {
            return getValue(3, 3);
        }

        /**
         * Set Call bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to Call bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setCall(int value) {
            if (!setValue(3, 3, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get MissedCall bit field value.
         *
         * @return Bit field value
         */
        public int getMissedCall() {
            return getValue(4, 4);
        }

        /**
         * Set MissedCall bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to MissedCall bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setMissedCall(int value) {
            if (!setValue(4, 4, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get SmsMms bit field value.
         *
         * @return Bit field value
         */
        public int getSmsMms() {
            return getValue(5, 5);
        }

        /**
         * Set SmsMms bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to SmsMms bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setSmsMms(int value) {
            if (!setValue(5, 5, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get VoiceMail bit field value.
         *
         * @return Bit field value
         */
        public int getVoiceMail() {
            return getValue(6, 6);
        }

        /**
         * Set VoiceMail bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to VoiceMail bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setVoiceMail(int value) {
            if (!setValue(6, 6, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get Schedule bit field value.
         *
         * @return Bit field value
         */
        public int getSchedule() {
            return getValue(7, 7);
        }

        /**
         * Set Schedule bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to Schedule bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setSchedule(int value) {
            if (!setValue(7, 7, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    /**
     * This class provides CategoryIdBitMask1 BitField operations based on
     * specific definition.
     */
    public class BfCategoryIdBitMask1 extends BitField {
        private static final int sLength = FormatUtils.UINT8_SIZE;

        /**
         * Get BfCategoryIdBitMask1 BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfCategoryIdBitMask1 BitField object.
         */
        public BfCategoryIdBitMask1() { super(sLength * 8); }

        /**
         * Create a BfCategoryIdBitMask1 BitField object and init value.
         *
         * @param value Initial value
         */
        public BfCategoryIdBitMask1(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get HighPrioritizedAlert bit field value.
         *
         * @return Bit field value
         */
        public int getHighPrioritizedAlert() {
            return getValue(0, 0);
        }

        /**
         * Set HighPrioritizedAlert bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to HighPrioritizedAlert bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setHighPrioritizedAlert(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get InstantMessage bit field value.
         *
         * @return Bit field value
         */
        public int getInstantMessage() {
            return getValue(1, 1);
        }

        /**
         * Set InstantMessage bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to InstantMessage bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setInstantMessage(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

