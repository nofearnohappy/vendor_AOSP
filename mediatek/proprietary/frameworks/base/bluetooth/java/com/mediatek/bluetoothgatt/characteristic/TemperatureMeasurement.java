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
 * Public API for the Temperature Measurement Bluetooth GATT Characteristic.
 *
 * <p>This class provides Temperature Measurement Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Temperature Measurement
 * Type: org.bluetooth.characteristic.temperature_measurement
 * UUID: 2A1C
 * Last Modified: None
 * Revision: None
 */
public class TemperatureMeasurement extends CharacteristicBase {
    /**
     * Temperature Measurement UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A1C"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Flags
     * Requirement: Mandatory
     * Format: bit8
     */
    private BfFlags mFlags = new BfFlags();

    /*
     * Field: Temperature Measurement Value (Celsius)
     * Requirement: C1
     * Format: float
     * Unit: org.bluetooth.unit.thermodynamic_temperature.degree_celsius
     */
    private byte[] mTemperatureMeasurementValueCelsius = new byte[FormatUtils.FLOAT_SIZE];

    /*
     * Field: Temperature Measurement Value (Fahrenheit)
     * Requirement: C2
     * Format: float
     * Unit: org.bluetooth.unit.thermodynamic_temperature.degree_fahrenheit
     */
    private byte[] mTemperatureMeasurementValueFahrenheit = new byte[FormatUtils.FLOAT_SIZE];

    /*
     * Field: Time Stamp
     * Requirement: C3
     * Reference: org.bluetooth.characteristic.date_time
     */
    private DateTime mTimeStamp = new DateTime();

    /*
     * Field: Temperature Type
     * Requirement: C4
     * Reference: org.bluetooth.characteristic.temperature_type
     */
    private TemperatureType mTemperatureType = new TemperatureType();


    /**
     * Create a TemperatureMeasurement characteristic object.
     */
    public TemperatureMeasurement() {
        setCharacteristic(null);
        setFlags(new BfFlags());
        setTemperatureMeasurementValueCelsius(0);
        setTemperatureMeasurementValueFahrenheit(0);
    }

    /**
     * Create a TemperatureMeasurement characteristic object and init value.
     *
     * @param value Initial value
     */
    public TemperatureMeasurement(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TemperatureMeasurement characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TemperatureMeasurement(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TemperatureMeasurement characteristic object.
     *
     * @param flags Flags
     * @param temperatureMeasurementValueCelsius Temperature Measurement Value (Celsius)
     * @param temperatureMeasurementValueFahrenheit Temperature Measurement Value (Fahrenheit)
     * @param timeStamp Time Stamp
     * @param temperatureType Temperature Type
     */
    public TemperatureMeasurement(
            BfFlags flags,
            float temperatureMeasurementValueCelsius,
            float temperatureMeasurementValueFahrenheit,
            DateTime timeStamp,
            TemperatureType temperatureType) {
        setCharacteristic(null);
        setFlags(flags);
        setTemperatureMeasurementValueCelsius(temperatureMeasurementValueCelsius);
        setTemperatureMeasurementValueFahrenheit(temperatureMeasurementValueFahrenheit);
        setTimeStamp(timeStamp);
        setTemperatureType(temperatureType);
    }

    /**
     * Create a TemperatureMeasurement characteristic object.
     *
     * @param flags Flags
     * @param temperatureMeasurementValueCelsius Temperature Measurement Value (Celsius)
     * @param temperatureMeasurementValueFahrenheit Temperature Measurement Value (Fahrenheit)
     * @param timeStamp Time Stamp
     * @param temperatureType Temperature Type
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TemperatureMeasurement(
            BfFlags flags,
            float temperatureMeasurementValueCelsius,
            float temperatureMeasurementValueFahrenheit,
            DateTime timeStamp,
            TemperatureType temperatureType,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setFlags(flags);
        setTemperatureMeasurementValueCelsius(temperatureMeasurementValueCelsius);
        setTemperatureMeasurementValueFahrenheit(temperatureMeasurementValueFahrenheit);
        setTimeStamp(timeStamp);
        setTemperatureType(temperatureType);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get TemperatureMeasurement characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportFlags()
                        ? mFlags.length() : 0)
                + (isSupportTemperatureMeasurementValueCelsius()
                        ? mTemperatureMeasurementValueCelsius.length : 0)
                + (isSupportTemperatureMeasurementValueFahrenheit()
                        ? mTemperatureMeasurementValueFahrenheit.length : 0)
                + (isSupportTimeStamp()
                        ? mTimeStamp.length() : 0)
                + (isSupportTemperatureType()
                        ? mTemperatureType.length() : 0);
    }

    /**
     * Get TemperatureMeasurement characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TemperatureMeasurement characteristic value.
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

        if (isSupportTemperatureMeasurementValueCelsius()) {
            int fieldLen = mTemperatureMeasurementValueCelsius.length;
            System.arraycopy(mTemperatureMeasurementValueCelsius, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportTemperatureMeasurementValueFahrenheit()) {
            int fieldLen = mTemperatureMeasurementValueFahrenheit.length;
            System.arraycopy(mTemperatureMeasurementValueFahrenheit, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportTimeStamp()) {
            int fieldLen = mTimeStamp.length();
            System.arraycopy(mTimeStamp.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportTemperatureType()) {
            int fieldLen = mTemperatureType.length();
            System.arraycopy(mTemperatureType.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TemperatureMeasurement characteristic value.
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

        if (isSupportTemperatureMeasurementValueCelsius()) {
            int fieldLen = mTemperatureMeasurementValueCelsius.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTemperatureMeasurementValueCelsius, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportTemperatureMeasurementValueFahrenheit()) {
            int fieldLen = mTemperatureMeasurementValueFahrenheit.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTemperatureMeasurementValueFahrenheit, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportTimeStamp()) {
            int fieldLen = mTimeStamp.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mTimeStamp.setValue(buf);
        }

        if (isSupportTemperatureType()) {
            int fieldLen = mTemperatureType.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mTemperatureType.setValue(buf);
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
     * Return true if TemperatureMeasurement support Flags field.
     *
     * @return  True, if TemperatureMeasurement support Flags field.
     */
    public boolean isSupportFlags() {
        return true;
    }

    /**
     * Get TemperatureMeasurementValueCelsius field value with float format.
     *
     * @return TemperatureMeasurementValueCelsius field value
     */
    public float getTemperatureMeasurementValueCelsius() {
        return FormatUtils.floatToFloat(mTemperatureMeasurementValueCelsius);
    }

    /**
     * Set TemperatureMeasurementValueCelsius field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TemperatureMeasurementValueCelsius field
     * @return      True, if the value has been set successfully
     */
    public boolean setTemperatureMeasurementValueCelsius(float value) {
        if (!FormatUtils.floatRangeCheck(value)) {
            return false;
        }
        mTemperatureMeasurementValueCelsius = FormatUtils.floatToFloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TemperatureMeasurement support TemperatureMeasurementValueCelsius field.
     *
     * @return  True, if TemperatureMeasurement support TemperatureMeasurementValueCelsius field.
     */
    public boolean isSupportTemperatureMeasurementValueCelsius() {
        return (mFlags.getTemperatureUnitsFlag() == 0);
    }

    /**
     * Get TemperatureMeasurementValueFahrenheit field value with float format.
     *
     * @return TemperatureMeasurementValueFahrenheit field value
     */
    public float getTemperatureMeasurementValueFahrenheit() {
        return FormatUtils.floatToFloat(mTemperatureMeasurementValueFahrenheit);
    }

    /**
     * Set TemperatureMeasurementValueFahrenheit field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TemperatureMeasurementValueFahrenheit field
     * @return      True, if the value has been set successfully
     */
    public boolean setTemperatureMeasurementValueFahrenheit(float value) {
        if (!FormatUtils.floatRangeCheck(value)) {
            return false;
        }
        mTemperatureMeasurementValueFahrenheit = FormatUtils.floatToFloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TemperatureMeasurement support TemperatureMeasurementValueFahrenheit field.
     *
     * @return  True, if TemperatureMeasurement support TemperatureMeasurementValueFahrenheit field.
     */
    public boolean isSupportTemperatureMeasurementValueFahrenheit() {
        return (mFlags.getTemperatureUnitsFlag() == 1);
    }

    /**
     * Get TimeStamp field value with DateTime format.
     *
     * @return TimeStamp field value
     */
    public DateTime getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * Set TimeStamp field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeStamp field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeStamp(byte[] value) {
        if (!mTimeStamp.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set TimeStamp field value by DateTime format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeStamp field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeStamp(DateTime value) {
        if (!mTimeStamp.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TemperatureMeasurement support TimeStamp field.
     *
     * @return  True, if TemperatureMeasurement support TimeStamp field.
     */
    public boolean isSupportTimeStamp() {
        return (mFlags.getTimeStampFlag() == 1);
    }

    /**
     * Get TemperatureType field value with TemperatureType format.
     *
     * @return TemperatureType field value
     */
    public TemperatureType getTemperatureType() {
        return mTemperatureType;
    }

    /**
     * Set TemperatureType field value by byte array format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TemperatureType field
     * @return      True, if the value has been set successfully
     */
    public boolean setTemperatureType(byte[] value) {
        if (!mTemperatureType.setValue(value)) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Set TemperatureType field value by TemperatureType format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TemperatureType field
     * @return      True, if the value has been set successfully
     */
    public boolean setTemperatureType(TemperatureType value) {
        if (!mTemperatureType.setValue(value.getValue())) {
            return false;
        }
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TemperatureMeasurement support TemperatureType field.
     *
     * @return  True, if TemperatureMeasurement support TemperatureType field.
     */
    public boolean isSupportTemperatureType() {
        return (mFlags.getTemperatureTypeFlag() == 1);
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
         * Get TemperatureUnitsFlag bit field value.
         *
         * @return Bit field value
         */
        public int getTemperatureUnitsFlag() {
            return getValue(0, 0);
        }

        /**
         * Set TemperatureUnitsFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to TemperatureUnitsFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setTemperatureUnitsFlag(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get TimeStampFlag bit field value.
         *
         * @return Bit field value
         */
        public int getTimeStampFlag() {
            return getValue(1, 1);
        }

        /**
         * Set TimeStampFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to TimeStampFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setTimeStampFlag(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get TemperatureTypeFlag bit field value.
         *
         * @return Bit field value
         */
        public int getTemperatureTypeFlag() {
            return getValue(2, 2);
        }

        /**
         * Set TemperatureTypeFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to TemperatureTypeFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setTemperatureTypeFlag(int value) {
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

