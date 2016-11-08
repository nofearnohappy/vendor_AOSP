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
 * Public API for the Measurement Interval Bluetooth GATT Characteristic.
 *
 * <p>This class provides Measurement Interval Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Measurement Interval
 * Type: org.bluetooth.characteristic.measurement_interval
 * UUID: 2A21
 * Last Modified: None
 * Revision: None
 */
public class MeasurementInterval extends CharacteristicBase {
    /**
     * Measurement Interval UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A21"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Measurement Interval
     * Requirement: Mandatory
     * Format: uint16
     * Unit: org.bluetooth.unit.time.second
     */
    private byte[] mMeasurementInterval = new byte[FormatUtils.UINT16_SIZE];


    /**
     * Create a MeasurementInterval characteristic object.
     */
    public MeasurementInterval() {
        setCharacteristic(null);
        setMeasurementInterval(1);
    }

    /**
     * Create a MeasurementInterval characteristic object and init value.
     *
     * @param value Initial value
     */
    public MeasurementInterval(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a MeasurementInterval characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public MeasurementInterval(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a MeasurementInterval characteristic object.
     *
     * @param measurementInterval Measurement Interval
     */
    public MeasurementInterval(
            int measurementInterval) {
        setCharacteristic(null);
        setMeasurementInterval(measurementInterval);
    }

    /**
     * Create a MeasurementInterval characteristic object.
     *
     * @param measurementInterval Measurement Interval
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public MeasurementInterval(
            int measurementInterval,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setMeasurementInterval(measurementInterval);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get MeasurementInterval characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportMeasurementInterval()
                        ? mMeasurementInterval.length : 0);
    }

    /**
     * Get MeasurementInterval characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get MeasurementInterval characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportMeasurementInterval()) {
            int fieldLen = mMeasurementInterval.length;
            System.arraycopy(mMeasurementInterval, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set MeasurementInterval characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportMeasurementInterval()) {
            int fieldLen = mMeasurementInterval.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mMeasurementInterval, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get MeasurementInterval field value with int format.
     *
     * @return MeasurementInterval field value
     */
    public int getMeasurementInterval() {
        return FormatUtils.uint16ToInt(mMeasurementInterval);
    }

    /**
     * Set MeasurementInterval field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to MeasurementInterval field
     * @return      True, if the value has been set successfully
     */
    public boolean setMeasurementInterval(int value) {
        if (!FormatUtils.uint16RangeCheck(value)) {
            return false;
        }
        mMeasurementInterval = FormatUtils.intToUint16(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if MeasurementInterval support MeasurementInterval field.
     *
     * @return  True, if MeasurementInterval support MeasurementInterval field.
     */
    public boolean isSupportMeasurementInterval() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

