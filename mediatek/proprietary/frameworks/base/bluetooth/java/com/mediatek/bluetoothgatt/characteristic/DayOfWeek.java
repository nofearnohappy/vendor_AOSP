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
 * Public API for the Day of Week Bluetooth GATT Characteristic.
 *
 * <p>This class provides Day of Week Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Day of Week
 * Type: org.bluetooth.characteristic.day_of_week
 * UUID: 2A09
 * Last Modified: None
 * Revision: None
 */
public class DayOfWeek extends CharacteristicBase {
    /**
     * Day of Week UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A09"));

    // Customized Start: Constant Definition .......................................................
    public static final int DOW_NOT_KNOWN = 0;
    public static final int DOW_MONDAY = 1;
    public static final int DOW_TUESDAY = 2;
    public static final int DOW_WEDNESDAY = 3;
    public static final int DOW_THURSDAY = 4;
    public static final int DOW_FRIDAY = 5;
    public static final int DOW_SATURDAY = 6;
    public static final int DOW_SUNDAY = 7;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Day of Week
     * Requirement: Mandatory
     * Format: uint8
     * Unit: org.bluetooth.unit.time.day
     */
    private byte[] mDayOfWeek = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a DayOfWeek characteristic object.
     */
    public DayOfWeek() {
        setCharacteristic(null);
        setDayOfWeek(0);
    }

    /**
     * Create a DayOfWeek characteristic object and init value.
     *
     * @param value Initial value
     */
    public DayOfWeek(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a DayOfWeek characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public DayOfWeek(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a DayOfWeek characteristic object.
     *
     * @param dayOfWeek Day of Week
     */
    public DayOfWeek(
            int dayOfWeek) {
        setCharacteristic(null);
        setDayOfWeek(dayOfWeek);
    }

    /**
     * Create a DayOfWeek characteristic object.
     *
     * @param dayOfWeek Day of Week
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public DayOfWeek(
            int dayOfWeek,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setDayOfWeek(dayOfWeek);
    }

    // Customized Start: Constructors ..............................................................

    /**
     * Create a DayOfWeek characteristic object and init value.
     *
     * @param c Current date and time
     */
    public DayOfWeek(Calendar c) {
        setCharacteristic(null);
        if (c == null) {
            setDayOfWeek(DOW_NOT_KNOWN);
        } else {
            int dayOfWeek = DOW_NOT_KNOWN;
            switch (c.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.SUNDAY:
                    dayOfWeek = DOW_SUNDAY;
                    break;
                default:
                    dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
            }
            setDayOfWeek(dayOfWeek);
        }
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get DayOfWeek characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportDayOfWeek()
                        ? mDayOfWeek.length : 0);
    }

    /**
     * Get DayOfWeek characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get DayOfWeek characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportDayOfWeek()) {
            int fieldLen = mDayOfWeek.length;
            System.arraycopy(mDayOfWeek, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set DayOfWeek characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportDayOfWeek()) {
            int fieldLen = mDayOfWeek.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mDayOfWeek, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get DayOfWeek field value with int format.
     *
     * @return DayOfWeek field value
     */
    public int getDayOfWeek() {
        return FormatUtils.uint8ToInt(mDayOfWeek);
    }

    /**
     * Set DayOfWeek field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DayOfWeek field
     * @return      True, if the value has been set successfully
     */
    public boolean setDayOfWeek(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mDayOfWeek = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DayOfWeek support DayOfWeek field.
     *
     * @return  True, if DayOfWeek support DayOfWeek field.
     */
    public boolean isSupportDayOfWeek() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

