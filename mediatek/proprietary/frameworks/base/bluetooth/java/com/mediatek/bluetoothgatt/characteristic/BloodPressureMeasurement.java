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
 * Public API for the Blood Pressure Measurement Bluetooth GATT Characteristic.
 *
 * <p>This class provides Blood Pressure Measurement Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Blood Pressure Measurement
 * Type: org.bluetooth.characteristic.blood_pressure_measurement
 * UUID: 2A35
 * Last Modified: None
 * Revision: None
 */
public class BloodPressureMeasurement extends CharacteristicBase {
    /**
     * Blood Pressure Measurement UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A35"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Flags
     * Requirement: Mandatory
     * Format: bit8
     */
    private BfFlags mFlags = new BfFlags();

    /*
     * Field: Blood Pressure Measurement Compound Value - Systolic (mmHg)
     * Requirement: C1
     * Format: sfloat
     * Unit: org.bluetooth.unit.pressure.millimetre_of_mercury
     */
    private byte[] mSystolicMmhg = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: Blood Pressure Measurement Compound Value - Diastolic (mmHg)
     * Requirement: C1
     * Format: sfloat
     * Unit: org.bluetooth.unit.pressure.millimetre_of_mercury
     */
    private byte[] mDiastolicMmhg = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: Blood Pressure Measurement Compound Value - Mean Arterial Pressure (mmHg)
     * Requirement: C1
     * Format: sfloat
     * Unit: org.bluetooth.unit.pressure.millimetre_of_mercury
     */
    private byte[] mMeanArterialPressureMmhg = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: Blood Pressure Measurement Compound Value - Systolic (kPa)
     * Requirement: C2
     * Format: sfloat
     * Unit: org.bluetooth.unit.pressure.pascal
     */
    private byte[] mSystolicKpa = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: Blood Pressure Measurement Compound Value - Diastolic (kPa)
     * Requirement: C2
     * Format: sfloat
     * Unit: org.bluetooth.unit.pressure.pascal
     */
    private byte[] mDiastolicKpa = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: Blood Pressure Measurement Compound Value - Mean Arterial Pressure (kPa)
     * Requirement: C2
     * Format: sfloat
     * Unit: org.bluetooth.unit.pressure.pascal
     */
    private byte[] mMeanArterialPressureKpa = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: Time Stamp
     * Requirement: C3
     * Reference: org.bluetooth.characteristic.date_time
     */
    private DateTime mTimeStamp = new DateTime();

    /*
     * Field: Pulse Rate
     * Requirement: C4
     * Format: sfloat
     * Unit: org.bluetooth.unit.period.beats_per_minute
     */
    private byte[] mPulseRate = new byte[FormatUtils.SFLOAT_SIZE];

    /*
     * Field: User ID
     * Requirement: C5
     * Format: uint8
     * Unit: None
     */
    private byte[] mUserId = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Measurement Status
     * Requirement: C6
     * Format: bit16
     */
    private BfMeasurementStatus mMeasurementStatus = new BfMeasurementStatus();


    /**
     * Create a BloodPressureMeasurement characteristic object.
     */
    public BloodPressureMeasurement() {
        setCharacteristic(null);
        setFlags(new BfFlags());
        setSystolicMmhg(0);
        setDiastolicMmhg(0);
        setMeanArterialPressureMmhg(0);
        setSystolicKpa(0);
        setDiastolicKpa(0);
        setMeanArterialPressureKpa(0);
        setPulseRate(0);
        setUserId(255);
        setMeasurementStatus(new BfMeasurementStatus());
    }

    /**
     * Create a BloodPressureMeasurement characteristic object and init value.
     *
     * @param value Initial value
     */
    public BloodPressureMeasurement(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a BloodPressureMeasurement characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BloodPressureMeasurement(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a BloodPressureMeasurement characteristic object.
     *
     * @param flags Flags
     * @param systolicMmhg Systolic (mmHg)
     * @param diastolicMmhg Diastolic (mmHg)
     * @param meanArterialPressureMmhg Mean Arterial Pressure (mmHg)
     * @param systolicKpa Systolic (kPa)
     * @param diastolicKpa Diastolic (kPa)
     * @param meanArterialPressureKpa Mean Arterial Pressure (kPa)
     * @param timeStamp Time Stamp
     * @param pulseRate Pulse Rate
     * @param userId User ID
     * @param measurementStatus Measurement Status
     */
    public BloodPressureMeasurement(
            BfFlags flags,
            float systolicMmhg,
            float diastolicMmhg,
            float meanArterialPressureMmhg,
            float systolicKpa,
            float diastolicKpa,
            float meanArterialPressureKpa,
            DateTime timeStamp,
            float pulseRate,
            int userId,
            BfMeasurementStatus measurementStatus) {
        setCharacteristic(null);
        setFlags(flags);
        setSystolicMmhg(systolicMmhg);
        setDiastolicMmhg(diastolicMmhg);
        setMeanArterialPressureMmhg(meanArterialPressureMmhg);
        setSystolicKpa(systolicKpa);
        setDiastolicKpa(diastolicKpa);
        setMeanArterialPressureKpa(meanArterialPressureKpa);
        setTimeStamp(timeStamp);
        setPulseRate(pulseRate);
        setUserId(userId);
        setMeasurementStatus(measurementStatus);
    }

    /**
     * Create a BloodPressureMeasurement characteristic object.
     *
     * @param flags Flags
     * @param systolicMmhg Systolic (mmHg)
     * @param diastolicMmhg Diastolic (mmHg)
     * @param meanArterialPressureMmhg Mean Arterial Pressure (mmHg)
     * @param systolicKpa Systolic (kPa)
     * @param diastolicKpa Diastolic (kPa)
     * @param meanArterialPressureKpa Mean Arterial Pressure (kPa)
     * @param timeStamp Time Stamp
     * @param pulseRate Pulse Rate
     * @param userId User ID
     * @param measurementStatus Measurement Status
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public BloodPressureMeasurement(
            BfFlags flags,
            float systolicMmhg,
            float diastolicMmhg,
            float meanArterialPressureMmhg,
            float systolicKpa,
            float diastolicKpa,
            float meanArterialPressureKpa,
            DateTime timeStamp,
            float pulseRate,
            int userId,
            BfMeasurementStatus measurementStatus,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setFlags(flags);
        setSystolicMmhg(systolicMmhg);
        setDiastolicMmhg(diastolicMmhg);
        setMeanArterialPressureMmhg(meanArterialPressureMmhg);
        setSystolicKpa(systolicKpa);
        setDiastolicKpa(diastolicKpa);
        setMeanArterialPressureKpa(meanArterialPressureKpa);
        setTimeStamp(timeStamp);
        setPulseRate(pulseRate);
        setUserId(userId);
        setMeasurementStatus(measurementStatus);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get BloodPressureMeasurement characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportFlags()
                        ? mFlags.length() : 0)
                + (isSupportSystolicMmhg()
                        ? mSystolicMmhg.length : 0)
                + (isSupportDiastolicMmhg()
                        ? mDiastolicMmhg.length : 0)
                + (isSupportMeanArterialPressureMmhg()
                        ? mMeanArterialPressureMmhg.length : 0)
                + (isSupportSystolicKpa()
                        ? mSystolicKpa.length : 0)
                + (isSupportDiastolicKpa()
                        ? mDiastolicKpa.length : 0)
                + (isSupportMeanArterialPressureKpa()
                        ? mMeanArterialPressureKpa.length : 0)
                + (isSupportTimeStamp()
                        ? mTimeStamp.length() : 0)
                + (isSupportPulseRate()
                        ? mPulseRate.length : 0)
                + (isSupportUserId()
                        ? mUserId.length : 0)
                + (isSupportMeasurementStatus()
                        ? mMeasurementStatus.length() : 0);
    }

    /**
     * Get BloodPressureMeasurement characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get BloodPressureMeasurement characteristic value.
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

        if (isSupportSystolicMmhg()) {
            int fieldLen = mSystolicMmhg.length;
            System.arraycopy(mSystolicMmhg, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportDiastolicMmhg()) {
            int fieldLen = mDiastolicMmhg.length;
            System.arraycopy(mDiastolicMmhg, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportMeanArterialPressureMmhg()) {
            int fieldLen = mMeanArterialPressureMmhg.length;
            System.arraycopy(mMeanArterialPressureMmhg, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportSystolicKpa()) {
            int fieldLen = mSystolicKpa.length;
            System.arraycopy(mSystolicKpa, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportDiastolicKpa()) {
            int fieldLen = mDiastolicKpa.length;
            System.arraycopy(mDiastolicKpa, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportMeanArterialPressureKpa()) {
            int fieldLen = mMeanArterialPressureKpa.length;
            System.arraycopy(mMeanArterialPressureKpa, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportTimeStamp()) {
            int fieldLen = mTimeStamp.length();
            System.arraycopy(mTimeStamp.getValue(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportPulseRate()) {
            int fieldLen = mPulseRate.length;
            System.arraycopy(mPulseRate, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportUserId()) {
            int fieldLen = mUserId.length;
            System.arraycopy(mUserId, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportMeasurementStatus()) {
            int fieldLen = mMeasurementStatus.length();
            System.arraycopy(mMeasurementStatus.getByteArray(), 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set BloodPressureMeasurement characteristic value.
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

        if (isSupportSystolicMmhg()) {
            int fieldLen = mSystolicMmhg.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mSystolicMmhg, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportDiastolicMmhg()) {
            int fieldLen = mDiastolicMmhg.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mDiastolicMmhg, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportMeanArterialPressureMmhg()) {
            int fieldLen = mMeanArterialPressureMmhg.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mMeanArterialPressureMmhg, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportSystolicKpa()) {
            int fieldLen = mSystolicKpa.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mSystolicKpa, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportDiastolicKpa()) {
            int fieldLen = mDiastolicKpa.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mDiastolicKpa, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportMeanArterialPressureKpa()) {
            int fieldLen = mMeanArterialPressureKpa.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mMeanArterialPressureKpa, 0, fieldLen);
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

        if (isSupportPulseRate()) {
            int fieldLen = mPulseRate.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mPulseRate, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportUserId()) {
            int fieldLen = mUserId.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mUserId, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportMeasurementStatus()) {
            int fieldLen = mMeasurementStatus.length();

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            byte[] buf = new byte[fieldLen];

            System.arraycopy(value, srcPos, buf, 0, buf.length);
            srcPos += fieldLen;

            mMeasurementStatus.setByteArray(buf);
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
     * Return true if BloodPressureMeasurement support Flags field.
     *
     * @return  True, if BloodPressureMeasurement support Flags field.
     */
    public boolean isSupportFlags() {
        return true;
    }

    /**
     * Get SystolicMmhg field value with float format.
     *
     * @return SystolicMmhg field value
     */
    public float getSystolicMmhg() {
        return FormatUtils.sfloatToFloat(mSystolicMmhg);
    }

    /**
     * Set SystolicMmhg field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to SystolicMmhg field
     * @return      True, if the value has been set successfully
     */
    public boolean setSystolicMmhg(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mSystolicMmhg = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support SystolicMmhg field.
     *
     * @return  True, if BloodPressureMeasurement support SystolicMmhg field.
     */
    public boolean isSupportSystolicMmhg() {
        return (mFlags.getBloodPressureUnitsFlag() == 0);
    }

    /**
     * Get DiastolicMmhg field value with float format.
     *
     * @return DiastolicMmhg field value
     */
    public float getDiastolicMmhg() {
        return FormatUtils.sfloatToFloat(mDiastolicMmhg);
    }

    /**
     * Set DiastolicMmhg field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DiastolicMmhg field
     * @return      True, if the value has been set successfully
     */
    public boolean setDiastolicMmhg(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mDiastolicMmhg = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support DiastolicMmhg field.
     *
     * @return  True, if BloodPressureMeasurement support DiastolicMmhg field.
     */
    public boolean isSupportDiastolicMmhg() {
        return (mFlags.getBloodPressureUnitsFlag() == 0);
    }

    /**
     * Get MeanArterialPressureMmhg field value with float format.
     *
     * @return MeanArterialPressureMmhg field value
     */
    public float getMeanArterialPressureMmhg() {
        return FormatUtils.sfloatToFloat(mMeanArterialPressureMmhg);
    }

    /**
     * Set MeanArterialPressureMmhg field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to MeanArterialPressureMmhg field
     * @return      True, if the value has been set successfully
     */
    public boolean setMeanArterialPressureMmhg(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mMeanArterialPressureMmhg = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support MeanArterialPressureMmhg field.
     *
     * @return  True, if BloodPressureMeasurement support MeanArterialPressureMmhg field.
     */
    public boolean isSupportMeanArterialPressureMmhg() {
        return (mFlags.getBloodPressureUnitsFlag() == 0);
    }

    /**
     * Get SystolicKpa field value with float format.
     *
     * @return SystolicKpa field value
     */
    public float getSystolicKpa() {
        return FormatUtils.sfloatToFloat(mSystolicKpa);
    }

    /**
     * Set SystolicKpa field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to SystolicKpa field
     * @return      True, if the value has been set successfully
     */
    public boolean setSystolicKpa(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mSystolicKpa = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support SystolicKpa field.
     *
     * @return  True, if BloodPressureMeasurement support SystolicKpa field.
     */
    public boolean isSupportSystolicKpa() {
        return (mFlags.getBloodPressureUnitsFlag() == 1);
    }

    /**
     * Get DiastolicKpa field value with float format.
     *
     * @return DiastolicKpa field value
     */
    public float getDiastolicKpa() {
        return FormatUtils.sfloatToFloat(mDiastolicKpa);
    }

    /**
     * Set DiastolicKpa field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DiastolicKpa field
     * @return      True, if the value has been set successfully
     */
    public boolean setDiastolicKpa(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mDiastolicKpa = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support DiastolicKpa field.
     *
     * @return  True, if BloodPressureMeasurement support DiastolicKpa field.
     */
    public boolean isSupportDiastolicKpa() {
        return (mFlags.getBloodPressureUnitsFlag() == 1);
    }

    /**
     * Get MeanArterialPressureKpa field value with float format.
     *
     * @return MeanArterialPressureKpa field value
     */
    public float getMeanArterialPressureKpa() {
        return FormatUtils.sfloatToFloat(mMeanArterialPressureKpa);
    }

    /**
     * Set MeanArterialPressureKpa field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to MeanArterialPressureKpa field
     * @return      True, if the value has been set successfully
     */
    public boolean setMeanArterialPressureKpa(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mMeanArterialPressureKpa = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support MeanArterialPressureKpa field.
     *
     * @return  True, if BloodPressureMeasurement support MeanArterialPressureKpa field.
     */
    public boolean isSupportMeanArterialPressureKpa() {
        return (mFlags.getBloodPressureUnitsFlag() == 1);
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
     * Return true if BloodPressureMeasurement support TimeStamp field.
     *
     * @return  True, if BloodPressureMeasurement support TimeStamp field.
     */
    public boolean isSupportTimeStamp() {
        return (mFlags.getTimeStampFlag() == 1);
    }

    /**
     * Get PulseRate field value with float format.
     *
     * @return PulseRate field value
     */
    public float getPulseRate() {
        return FormatUtils.sfloatToFloat(mPulseRate);
    }

    /**
     * Set PulseRate field value by float format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to PulseRate field
     * @return      True, if the value has been set successfully
     */
    public boolean setPulseRate(float value) {
        if (!FormatUtils.sfloatRangeCheck(value)) {
            return false;
        }
        mPulseRate = FormatUtils.floatToSfloat(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support PulseRate field.
     *
     * @return  True, if BloodPressureMeasurement support PulseRate field.
     */
    public boolean isSupportPulseRate() {
        return (mFlags.getPulseRateFlag() == 1);
    }

    /**
     * Get UserId field value with int format.
     *
     * @return UserId field value
     */
    public int getUserId() {
        return FormatUtils.uint8ToInt(mUserId);
    }

    /**
     * Set UserId field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to UserId field
     * @return      True, if the value has been set successfully
     */
    public boolean setUserId(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mUserId = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support UserId field.
     *
     * @return  True, if BloodPressureMeasurement support UserId field.
     */
    public boolean isSupportUserId() {
        return (mFlags.getUserIdFlag() == 1);
    }

    /**
     * Get MeasurementStatus field value with BfMeasurementStatus format.
     *
     * @return MeasurementStatus field value
     */
    public BfMeasurementStatus getMeasurementStatus() {
        return mMeasurementStatus;
    }

    /**
     * Set MeasurementStatus field value by BfMeasurementStatus format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to MeasurementStatus field
     * @return      True
     */
    public boolean setMeasurementStatus(BfMeasurementStatus value) {
        mMeasurementStatus = value;
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if BloodPressureMeasurement support MeasurementStatus field.
     *
     * @return  True, if BloodPressureMeasurement support MeasurementStatus field.
     */
    public boolean isSupportMeasurementStatus() {
        return (mFlags.getMeasurementStatusFlag() == 1);
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
         * Get BloodPressureUnitsFlag bit field value.
         *
         * @return Bit field value
         */
        public int getBloodPressureUnitsFlag() {
            return getValue(0, 0);
        }

        /**
         * Set BloodPressureUnitsFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to BloodPressureUnitsFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setBloodPressureUnitsFlag(int value) {
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
         * Get PulseRateFlag bit field value.
         *
         * @return Bit field value
         */
        public int getPulseRateFlag() {
            return getValue(2, 2);
        }

        /**
         * Set PulseRateFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to PulseRateFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setPulseRateFlag(int value) {
            if (!setValue(2, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get UserIdFlag bit field value.
         *
         * @return Bit field value
         */
        public int getUserIdFlag() {
            return getValue(3, 3);
        }

        /**
         * Set UserIdFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to UserIdFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setUserIdFlag(int value) {
            if (!setValue(3, 3, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get MeasurementStatusFlag bit field value.
         *
         * @return Bit field value
         */
        public int getMeasurementStatusFlag() {
            return getValue(4, 4);
        }

        /**
         * Set MeasurementStatusFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to MeasurementStatusFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setMeasurementStatusFlag(int value) {
            if (!setValue(4, 4, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }
    }

    /**
     * This class provides MeasurementStatus BitField operations based on
     * specific definition.
     */
    public class BfMeasurementStatus extends BitField {
        private static final int sLength = FormatUtils.BIT16_SIZE;

        /**
         * Get BfMeasurementStatus BitField byte length.
         *
         * @return Byte length of this BitField
         */
        @Override
        public int length() {
            return sLength;
        }

        /**
         * Create a BfMeasurementStatus BitField object.
         */
        public BfMeasurementStatus() { super(sLength * 8); }

        /**
         * Create a BfMeasurementStatus BitField object and init value.
         *
         * @param value Initial value
         */
        public BfMeasurementStatus(byte[] value) {
            super(sLength * 8);
            setByteArray(value);
        }

        /**
         * Get BodyMovementDetectionFlag bit field value.
         *
         * @return Bit field value
         */
        public int getBodyMovementDetectionFlag() {
            return getValue(0, 0);
        }

        /**
         * Set BodyMovementDetectionFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to BodyMovementDetectionFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setBodyMovementDetectionFlag(int value) {
            if (!setValue(0, 0, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get CuffFitDetectionFlag bit field value.
         *
         * @return Bit field value
         */
        public int getCuffFitDetectionFlag() {
            return getValue(1, 1);
        }

        /**
         * Set CuffFitDetectionFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to CuffFitDetectionFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setCuffFitDetectionFlag(int value) {
            if (!setValue(1, 1, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get IrregularPulseDetectionFlag bit field value.
         *
         * @return Bit field value
         */
        public int getIrregularPulseDetectionFlag() {
            return getValue(2, 2);
        }

        /**
         * Set IrregularPulseDetectionFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to IrregularPulseDetectionFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setIrregularPulseDetectionFlag(int value) {
            if (!setValue(2, 2, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get PulseRateRangeDetectionFlags bit field value.
         *
         * @return Bit field value
         */
        public int getPulseRateRangeDetectionFlags() {
            return getValue(3, 4);
        }

        /**
         * Set PulseRateRangeDetectionFlags bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to PulseRateRangeDetectionFlags bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setPulseRateRangeDetectionFlags(int value) {
            if (!setValue(3, 4, value)) {
                return false;
            }
            updateGattCharacteristic();
            return true;
        }

        /**
         * Get MeasurementPositionDetectionFlag bit field value.
         *
         * @return Bit field value
         */
        public int getMeasurementPositionDetectionFlag() {
            return getValue(5, 5);
        }

        /**
         * Set MeasurementPositionDetectionFlag bit field value
         * and update the related Bluetooth GATT Characteristic.
         *
         * @param value Value to write to MeasurementPositionDetectionFlag bit field
         * @return      True, if the value has been set successfully
         */
        public boolean setMeasurementPositionDetectionFlag(int value) {
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

