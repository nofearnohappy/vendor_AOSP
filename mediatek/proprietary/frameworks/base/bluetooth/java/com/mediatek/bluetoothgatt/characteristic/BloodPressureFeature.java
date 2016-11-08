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
 * Public API for the Blood Pressure Feature Bluetooth GATT Characteristic.
 *
 * <p>This class provides Blood Pressure Feature Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Blood Pressure Feature
 * Type: org.bluetooth.characteristic.blood_pressure_feature
 * UUID: 2A49
 * Last Modified: None
 * Revision: None
 */
public class BloodPressureFeature extends CharacteristicBase {
    /**
     * Blood Pressure Feature UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A49"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Blood Pressure Feature
     * Requirement: Mandatory
     * Format: bit16
     */
    private BfBloodPressureFeature mBloodPressureFeature = new BfBloodPressureFeature();


    /**
     * Create a BloodPressureFeature characteristic object.
     */
    public BloodPressureFeature() {
        setCharacteristic(null);
        setBloodPressureFeature(new BfBloodPressureFeature());
    }

    /**
     * Create a BloodPressureFeature characteristic object and init value.
     *
     * @param value Initial value
     */
    public BloodPressureFeature(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a BloodPressureFeature characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BloodPressureFeature(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a BloodPressureFeature characteristic object.
     *
     * @param bloodPressureFeature Blood Pressure Feature
     */
    public BloodPressureFeature(
            BfBloodPressureFeature bloodPressureFeature) {
        setCharacteristic(null);
        setBloodPressureFeature(bloodPressureFeature);
    }

    /**
     * Create a BloodPressureFeature characteristic object.
     *
     * @param bloodPressureFeature Blood Pressure Feature
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BloodPressureFeature(
            BfBloodPressureFeature bloodPressureFeature,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setBloodPressureFeature(bloodPressureFeature);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get BloodPressureFeature characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportBloodPressureFeature()
                        ? mBloodPressureFeature.length() : 0);
    }

    /**
     * Get BloodPressureFeature characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get BloodPressureFeature characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportBloodPressureFeature()) {
            int fieldLen = mBloodPressureFeature.length();
            System.arraycopy(mBloodPressureFeature.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set BloodPressureFeature characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportBloodPressureFeature()) {
            int fieldLen = mBloodPressureFeature.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mBloodPressureFeature.setByteArray(buf);
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get BloodPressureFeature field value with BfBloodPressureFeature format.
     *
     * @return BloodPressureFeature field value
     */
    public BfBloodPressureFeature getBloodPressureFeature() {
        return mBloodPressureFeature;
    }

    /**
     * Set BloodPressureFeature field value by BfBloodPressureFeature format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to BloodPressureFeature field
     * @return      True
     */
    public boolean setBloodPressureFeature(BfBloodPressureFeature value) {
        mBloodPressureFeature = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureFeature support BloodPressureFeature field.
     *
     * @return  True, if BloodPressureFeature support BloodPressureFeature field.
     */
    public boolean isSupportBloodPressureFeature() {
        return true;
    }

    /**
     * This class provides BloodPressureFeature BitField operations based on
     * specific definition.
     */
    public class BfBloodPressureFeature extends BitField {
        private static final int sLength = FormatUtils.BIT16_SIZE;

        /**
         * Get BfBloodPressureFeature BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfBloodPressureFeature BitField object.
         */
        public BfBloodPressureFeature() { super(sLength * 8); }

        /**
         * Create a BfBloodPressureFeature BitField object and init value.
         *
         * @param value Initial value
         */
        public BfBloodPressureFeature(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get BodyMovementDetectionSupportBit bit field value.
         *
         * @return Bit field value
         */
        public int getBodyMovementDetectionSupportBit() {
            return getValue(0, 0);
        }

        /**
         * Set BodyMovementDetectionSupportBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to BodyMovementDetectionSupportBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setBodyMovementDetectionSupportBit(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get CuffFitDetectionSupportBit bit field value.
         *
         * @return Bit field value
         */
        public int getCuffFitDetectionSupportBit() {
            return getValue(1, 1);
        }

        /**
         * Set CuffFitDetectionSupportBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to CuffFitDetectionSupportBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setCuffFitDetectionSupportBit(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get IrregularPulseDetectionSupportBit bit field value.
         *
         * @return Bit field value
         */
        public int getIrregularPulseDetectionSupportBit() {
            return getValue(2, 2);
        }

        /**
         * Set IrregularPulseDetectionSupportBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to IrregularPulseDetectionSupportBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setIrregularPulseDetectionSupportBit(int value) {
            if (!setValue(2, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get PulseRateRangeDetectionSupportBit bit field value.
         *
         * @return Bit field value
         */
        public int getPulseRateRangeDetectionSupportBit() {
            return getValue(3, 3);
        }

        /**
         * Set PulseRateRangeDetectionSupportBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to PulseRateRangeDetectionSupportBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setPulseRateRangeDetectionSupportBit(int value) {
            if (!setValue(3, 3, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get MeasurementPositionDetectionSupportBit bit field value.
         *
         * @return Bit field value
         */
        public int getMeasurementPositionDetectionSupportBit() {
            return getValue(4, 4);
        }

        /**
         * Set MeasurementPositionDetectionSupportBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to MeasurementPositionDetectionSupportBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setMeasurementPositionDetectionSupportBit(int value) {
            if (!setValue(4, 4, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get MultipleBondSupportBit bit field value.
         *
         * @return Bit field value
         */
        public int getMultipleBondSupportBit() {
            return getValue(5, 5);
        }

        /**
         * Set MultipleBondSupportBit bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to MultipleBondSupportBit bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setMultipleBondSupportBit(int value) {
            if (!setValue(5, 5, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

