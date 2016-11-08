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
 * Public API for the Time Source Bluetooth GATT Characteristic.
 *
 * <p>This class provides Time Source Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Time Source
 * Type: org.bluetooth.characteristic.time_source
 * UUID: 2A13
 * Last Modified: None
 * Revision: None
 */
public class TimeSource extends CharacteristicBase {
    /**
     * Time Source UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A13"));

    // Customized Start: Constant Definition .......................................................
    public static final int TIME_SOURCE_UNKNOWN = 0;
    public static final int TIME_SOURCE_NTP = 1;
    public static final int TIME_SOURCE_GPS = 2;
    public static final int TIME_SOURCE_RADIO = 3;
    public static final int TIME_SOURCE_MANUAL = 4;
    public static final int TIME_SOURCE_ATOMIC_CLOCK = 5;
    public static final int TIME_SOURCE_CELLULAR_NETWORK = 6;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Time Source
     * Requirement: Mandatory
     * Format: bit8
     * Unit: None
     */
    private byte[] mTimeSource = new byte[FormatUtils.BIT8_SIZE];


    /**
     * Create a TimeSource characteristic object.
     */
    public TimeSource() {
        setCharacteristic(null);
        setTimeSource(0);
    }

    /**
     * Create a TimeSource characteristic object and init value.
     *
     * @param value Initial value
     */
    public TimeSource(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TimeSource characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeSource(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TimeSource characteristic object.
     *
     * @param timeSource Time Source
     */
    public TimeSource(
            int timeSource) {
        setCharacteristic(null);
        setTimeSource(timeSource);
    }

    /**
     * Create a TimeSource characteristic object.
     *
     * @param timeSource Time Source
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeSource(
            int timeSource,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setTimeSource(timeSource);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get TimeSource characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportTimeSource()
                        ? mTimeSource.length : 0);
    }

    /**
     * Get TimeSource characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TimeSource characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportTimeSource()) {
            int fieldLen = mTimeSource.length;
            System.arraycopy(mTimeSource, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TimeSource characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportTimeSource()) {
            int fieldLen = mTimeSource.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTimeSource, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get TimeSource field value with int format.
     *
     * @return TimeSource field value
     */
    public int getTimeSource() {
        return FormatUtils.bit8ToInt(mTimeSource);
    }

    /**
     * Set TimeSource field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeSource field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeSource(int value) {
        if (!FormatUtils.bit8RangeCheck(value)) {
            return false;
        }
        mTimeSource = FormatUtils.intToBit8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeSource support TimeSource field.
     *
     * @return  True, if TimeSource support TimeSource field.
     */
    public boolean isSupportTimeSource() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

