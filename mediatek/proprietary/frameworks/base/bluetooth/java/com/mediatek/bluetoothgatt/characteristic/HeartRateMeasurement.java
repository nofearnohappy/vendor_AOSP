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
 * Public API for the Heart Rate Measurement Bluetooth GATT Characteristic.
 *
 * <p>This class provides Heart Rate Measurement Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Heart Rate Measurement
 * Type: org.bluetooth.characteristic.heart_rate_measurement
 * UUID: 2A37
 * Last Modified: None
 * Revision: None
 */
public class HeartRateMeasurement extends CharacteristicBase {
    /**
     * Heart Rate Measurement UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A37"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Flags
     * Requirement: Mandatory
     * Format: bit8
     */
    private BfFlags mFlags = new BfFlags();

    /*
     * Field: Heart Rate Measurement Value (uint8)
     * Requirement: C1
     * Format: uint8
     * Unit: org.bluetooth.unit.period.beats_per_minute
     */
    private byte[] mHeartRateMeasurementValueUint8 = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Heart Rate Measurement Value (uint16)
     * Requirement: C2
     * Format: uint16
     * Unit: org.bluetooth.unit.period.beats_per_minute
     */
    private byte[] mHeartRateMeasurementValueUint16 = new byte[FormatUtils.UINT16_SIZE];

    /*
     * Field: Energy Expended
     * Requirement: C3
     * Format: uint16
     * Unit: org.bluetooth.unit.energy.joule
     */
    private byte[] mEnergyExpended = new byte[FormatUtils.UINT16_SIZE];

    /*
     * Field: RR-Interval
     * Requirement: C4
     * Format: uint16
     * Unit: org.bluetooth.unit.time.second
     */
    private byte[] mRrInterval = new byte[FormatUtils.UINT16_SIZE];


    /**
     * Create a HeartRateMeasurement characteristic object.
     */
    public HeartRateMeasurement() {
        setCharacteristic(null);
        setFlags(new BfFlags());
        setHeartRateMeasurementValueUint8(0);
        setHeartRateMeasurementValueUint16(0);
        setEnergyExpended(0);
        setRrInterval(0);
    }

    /**
     * Create a HeartRateMeasurement characteristic object and init value.
     *
     * @param value Initial value
     */
    public HeartRateMeasurement(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a HeartRateMeasurement characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public HeartRateMeasurement(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a HeartRateMeasurement characteristic object.
     *
     * @param flags Flags
     * @param heartRateMeasurementValueUint8 Heart Rate Measurement Value (uint8)
     * @param heartRateMeasurementValueUint16 Heart Rate Measurement Value (uint16)
     * @param energyExpended Energy Expended
     * @param rrInterval RR-Interval
     */
    public HeartRateMeasurement(
            BfFlags flags,
            int heartRateMeasurementValueUint8,
            int heartRateMeasurementValueUint16,
            int energyExpended,
            int rrInterval) {
        setCharacteristic(null);
        setFlags(flags);
        setHeartRateMeasurementValueUint8(heartRateMeasurementValueUint8);
        setHeartRateMeasurementValueUint16(heartRateMeasurementValueUint16);
        setEnergyExpended(energyExpended);
        setRrInterval(rrInterval);
    }

    /**
     * Create a HeartRateMeasurement characteristic object.
     *
     * @param flags Flags
     * @param heartRateMeasurementValueUint8 Heart Rate Measurement Value (uint8)
     * @param heartRateMeasurementValueUint16 Heart Rate Measurement Value (uint16)
     * @param energyExpended Energy Expended
     * @param rrInterval RR-Interval
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public HeartRateMeasurement(
            BfFlags flags,
            int heartRateMeasurementValueUint8,
            int heartRateMeasurementValueUint16,
            int energyExpended,
            int rrInterval,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setFlags(flags);
        setHeartRateMeasurementValueUint8(heartRateMeasurementValueUint8);
        setHeartRateMeasurementValueUint16(heartRateMeasurementValueUint16);
        setEnergyExpended(energyExpended);
        setRrInterval(rrInterval);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get HeartRateMeasurement characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportFlags()
                        ? mFlags.length() : 0)
                + (isSupportHeartRateMeasurementValueUint8()
                        ? mHeartRateMeasurementValueUint8.length : 0)
                + (isSupportHeartRateMeasurementValueUint16()
                        ? mHeartRateMeasurementValueUint16.length : 0)
                + (isSupportEnergyExpended()
                        ? mEnergyExpended.length : 0)
                + (isSupportRrInterval()
                        ? mRrInterval.length : 0);
    }

    /**
     * Get HeartRateMeasurement characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get HeartRateMeasurement characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportFlags()) {
            int fieldLen = mFlags.length();
            System.arraycopy(mFlags.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportHeartRateMeasurementValueUint8()) {
            int fieldLen = mHeartRateMeasurementValueUint8.length;
            System.arraycopy(mHeartRateMeasurementValueUint8, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportHeartRateMeasurementValueUint16()) {
            int fieldLen = mHeartRateMeasurementValueUint16.length;
            System.arraycopy(mHeartRateMeasurementValueUint16, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportEnergyExpended()) {
            int fieldLen = mEnergyExpended.length;
            System.arraycopy(mEnergyExpended, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportRrInterval()) {
            int fieldLen = mRrInterval.length;
            System.arraycopy(mRrInterval, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set HeartRateMeasurement characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportFlags()) {
            int fieldLen = mFlags.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mFlags.setByteArray(buf);
        }

        if (isSupportHeartRateMeasurementValueUint8()) {
            int fieldLen = mHeartRateMeasurementValueUint8.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mHeartRateMeasurementValueUint8, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportHeartRateMeasurementValueUint16()) {
            int fieldLen = mHeartRateMeasurementValueUint16.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mHeartRateMeasurementValueUint16, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportEnergyExpended()) {
            int fieldLen = mEnergyExpended.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mEnergyExpended, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportRrInterval()) {
            int fieldLen = mRrInterval.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mRrInterval, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get Flags field value with BfFlags format.
     *
     * @return Flags field value
     */
    public BfFlags getFlags() {
        return mFlags;
    }

    /**
     * Set Flags field value by BfFlags format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Flags field
     * @return      True
     */
    public boolean setFlags(BfFlags value) {
        mFlags = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if HeartRateMeasurement support Flags field.
     *
     * @return  True, if HeartRateMeasurement support Flags field.
     */
    public boolean isSupportFlags() {
        return true;
    }

    /**
     * Get HeartRateMeasurementValueUint8 field value with int format.
     *
     * @return HeartRateMeasurementValueUint8 field value
     */
    public int getHeartRateMeasurementValueUint8() {
        return FormatUtils.uint8ToInt(mHeartRateMeasurementValueUint8);
    }

    /**
     * Set HeartRateMeasurementValueUint8 field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to HeartRateMeasurementValueUint8 field
     * @return      True, if the value has been set successfully
     */
    public boolean setHeartRateMeasurementValueUint8(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mHeartRateMeasurementValueUint8 = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if HeartRateMeasurement support HeartRateMeasurementValueUint8 field.
     *
     * @return  True, if HeartRateMeasurement support HeartRateMeasurementValueUint8 field.
     */
    public boolean isSupportHeartRateMeasurementValueUint8() {
        return (mFlags.getHeartRateValueFormatBit() == 0);
    }

    /**
     * Get HeartRateMeasurementValueUint16 field value with int format.
     *
     * @return HeartRateMeasurementValueUint16 field value
     */
    public int getHeartRateMeasurementValueUint16() {
        return FormatUtils.uint16ToInt(mHeartRateMeasurementValueUint16);
    }

    /**
     * Set HeartRateMeasurementValueUint16 field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to HeartRateMeasurementValueUint16 field
     * @return      True, if the value has been set successfully
     */
    public boolean setHeartRateMeasurementValueUint16(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mHeartRateMeasurementValueUint16 = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if HeartRateMeasurement support HeartRateMeasurementValueUint16 field.
     *
     * @return  True, if HeartRateMeasurement support HeartRateMeasurementValueUint16 field.
     */
    public boolean isSupportHeartRateMeasurementValueUint16() {
        return (mFlags.getHeartRateValueFormatBit() == 1);
    }

    /**
     * Get EnergyExpended field value with int format.
     *
     * @return EnergyExpended field value
     */
    public int getEnergyExpended() {
        return FormatUtils.uint16ToInt(mEnergyExpended);
    }

    /**
     * Set EnergyExpended field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to EnergyExpended field
     * @return      True, if the value has been set successfully
     */
    public boolean setEnergyExpended(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mEnergyExpended = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if HeartRateMeasurement support EnergyExpended field.
     *
     * @return  True, if HeartRateMeasurement support EnergyExpended field.
     */
    public boolean isSupportEnergyExpended() {
        return (mFlags.getEnergyExpendedStatusBit() == 1);
    }

    /**
     * Get RrInterval field value with int format.
     *
     * @return RrInterval field value
     */
    public int getRrInterval() {
        return FormatUtils.uint16ToInt(mRrInterval);
    }

    /**
     * Set RrInterval field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to RrInterval field
     * @return      True, if the value has been set successfully
     */
    public boolean setRrInterval(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mRrInterval = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if HeartRateMeasurement support RrInterval field.
     *
     * @return  True, if HeartRateMeasurement support RrInterval field.
     */
    public boolean isSupportRrInterval() {
        return (mFlags.getRrIntervalBit() == 1);
    }

    /**
     * This class provides Flags BitField operations based on
     * specific definition.
     */
    public class BfFlags extends BitField {
        private static final int sLength = FormatUtils.BIT8_SIZE;

        /**
         * Get BfFlags BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfFlags BitField object.
         */
        public BfFlags() { super(sLength * 8); }

        /**
         * Create a BfFlags BitField object and init value.
         *
         * @param value Initial value
         */
        public BfFlags(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get HeartRateValueFormatBit bit field value.
         *
         * @return Bit field value
         */
        public int getHeartRateValueFormatBit() {
            return getValue(0, 0);
        }

        /**
         * Set HeartRateValueFormatBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to HeartRateValueFormatBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setHeartRateValueFormatBit(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get SensorContactStatusBits bit field value.
         *
         * @return Bit field value
         */
        public int getSensorContactStatusBits() {
            return getValue(1, 2);
        }

        /**
         * Set SensorContactStatusBits bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to SensorContactStatusBits bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setSensorContactStatusBits(int value) {
            if (!setValue(1, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get EnergyExpendedStatusBit bit field value.
         *
         * @return Bit field value
         */
        public int getEnergyExpendedStatusBit() {
            return getValue(3, 3);
        }

        /**
         * Set EnergyExpendedStatusBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to EnergyExpendedStatusBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setEnergyExpendedStatusBit(int value) {
            if (!setValue(3, 3, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get RrIntervalBit bit field value.
         *
         * @return Bit field value
         */
        public int getRrIntervalBit() {
            return getValue(4, 4);
        }

        /**
         * Set RrIntervalBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to RrIntervalBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setRrIntervalBit(int value) {
            if (!setValue(4, 4, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

