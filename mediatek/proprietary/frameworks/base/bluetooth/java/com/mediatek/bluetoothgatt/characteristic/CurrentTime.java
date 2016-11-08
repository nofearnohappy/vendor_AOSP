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

import java.util.Calendar;
//........................................................................ Customized End: Import //
import java.util.UUID;

/**
 * Public API for the Current Time Bluetooth GATT Characteristic.
 *
 * <p>This class provides Current Time Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Current Time
 * Type: org.bluetooth.characteristic.current_time
 * UUID: 2A2B
 * Last Modified: None
 * Revision: None
 */
public class CurrentTime extends CharacteristicBase {
    /**
     * Current Time UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A2B"));

    // Customized Start: Constant Definition .......................................................
    public static final byte ADJ_MANUAL_TIME_UPDATE = (1 << 0);
    public static final byte ADJ_EXTERNAL_REFERENCE_TIME_UPDATE = (1 << 1);
    public static final byte ADJ_CHANGE_OF_TIME_ZONE = (1 << 2);
    public static final byte ADJ_CHANGE_OF_DST = (1 << 3);
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Exact Time 256
     * Requirement: Mandatory
     * Reference: org.bluetooth.characteristic.exact_time_256
     */
    private ExactTime256 mExactTime256 = new ExactTime256();

    /*
     * Field: Adjust Reason
     * Requirement: Mandatory
     * Format: bit8
     */
    private BfAdjustReason mAdjustReason = new BfAdjustReason();


    /**
     * Create a CurrentTime characteristic object.
     */
    public CurrentTime() {
        setCharacteristic(null);
        setAdjustReason(new BfAdjustReason());
    }

    /**
     * Create a CurrentTime characteristic object and init value.
     *
     * @param value Initial value
     */
    public CurrentTime(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a CurrentTime characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public CurrentTime(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a CurrentTime characteristic object.
     *
     * @param exactTime256 Exact Time 256
     * @param adjustReason Adjust Reason
     */
    public CurrentTime(
            ExactTime256 exactTime256,
            BfAdjustReason adjustReason) {
        setCharacteristic(null);
        setExactTime256(exactTime256);
        setAdjustReason(adjustReason);
    }

    /**
     * Create a CurrentTime characteristic object.
     *
     * @param exactTime256 Exact Time 256
     * @param adjustReason Adjust Reason
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public CurrentTime(
            ExactTime256 exactTime256,
            BfAdjustReason adjustReason,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setExactTime256(exactTime256);
        setAdjustReason(adjustReason);
    }

    // Customized Start: Constructors ..............................................................
    /**
     * Create a CurrentTime characteristic object and init value.
     *
     * @param c Current date and time.
     * @param adjReason Current time adjust reason
     */
    public CurrentTime(Calendar c, byte adjReason) {
        setCharacteristic(null);
        setExactTime256(new ExactTime256(c));
        setAdjustReason(new BfAdjustReason(new byte[]{adjReason}));
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get CurrentTime characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportExactTime256()
                        ? mExactTime256.length() : 0)
                + (isSupportAdjustReason()
                        ? mAdjustReason.length() : 0);
    }

    /**
     * Get CurrentTime characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get CurrentTime characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportExactTime256()) {
            int fieldLen = mExactTime256.length();
            System.arraycopy(mExactTime256.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportAdjustReason()) {
            int fieldLen = mAdjustReason.length();
            System.arraycopy(mAdjustReason.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set CurrentTime characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportExactTime256()) {
            int fieldLen = mExactTime256.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mExactTime256.setValue(buf);
        }

        if (isSupportAdjustReason()) {
            int fieldLen = mAdjustReason.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mAdjustReason.setByteArray(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get ExactTime256 field value with ExactTime256 format.
     *
     * @return ExactTime256 field value
     */
    public ExactTime256 getExactTime256() {
        return mExactTime256;
    }

    /**
     * Set ExactTime256 field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ExactTime256 field
     * @return      True, if the value has been set successfully
     */
    public boolean setExactTime256(byte[] value) {
        if (!mExactTime256.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set ExactTime256 field value by ExactTime256 format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to ExactTime256 field
     * @return      True, if the value has been set successfully
     */
    public boolean setExactTime256(ExactTime256 value) {
        if (!mExactTime256.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if CurrentTime support ExactTime256 field.
     *
     * @return  True, if CurrentTime support ExactTime256 field.
     */
    public boolean isSupportExactTime256() {
        return true;
    }

    /**
     * Get AdjustReason field value with BfAdjustReason format.
     *
     * @return AdjustReason field value
     */
    public BfAdjustReason getAdjustReason() {
        return mAdjustReason;
    }

    /**
     * Set AdjustReason field value by BfAdjustReason format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to AdjustReason field
     * @return      True
     */
    public boolean setAdjustReason(BfAdjustReason value) {
        mAdjustReason = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if CurrentTime support AdjustReason field.
     *
     * @return  True, if CurrentTime support AdjustReason field.
     */
    public boolean isSupportAdjustReason() {
        return true;
    }

    /**
     * This class provides AdjustReason BitField operations based on
     * specific definition.
     */
    public class BfAdjustReason extends BitField {
        private static final int sLength = FormatUtils.BIT8_SIZE;

        /**
         * Get BfAdjustReason BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfAdjustReason BitField object.
         */
        public BfAdjustReason() { super(sLength * 8); }

        /**
         * Create a BfAdjustReason BitField object and init value.
         *
         * @param value Initial value
         */
        public BfAdjustReason(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get ManualTimeUpdate bit field value.
         *
         * @return Bit field value
         */
        public int getManualTimeUpdate() {
            return getValue(0, 0);
        }

        /**
         * Set ManualTimeUpdate bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to ManualTimeUpdate bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setManualTimeUpdate(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get ExternalReferenceTimeUpdate bit field value.
         *
         * @return Bit field value
         */
        public int getExternalReferenceTimeUpdate() {
            return getValue(1, 1);
        }

        /**
         * Set ExternalReferenceTimeUpdate bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to ExternalReferenceTimeUpdate bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setExternalReferenceTimeUpdate(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get ChangeOfTimeZone bit field value.
         *
         * @return Bit field value
         */
        public int getChangeOfTimeZone() {
            return getValue(2, 2);
        }

        /**
         * Set ChangeOfTimeZone bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to ChangeOfTimeZone bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setChangeOfTimeZone(int value) {
            if (!setValue(2, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get ChangeOfDst bit field value.
         *
         * @return Bit field value
         */
        public int getChangeOfDst() {
            return getValue(3, 3);
        }

        /**
         * Set ChangeOfDst bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to ChangeOfDst bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setChangeOfDst(int value) {
            if (!setValue(3, 3, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

