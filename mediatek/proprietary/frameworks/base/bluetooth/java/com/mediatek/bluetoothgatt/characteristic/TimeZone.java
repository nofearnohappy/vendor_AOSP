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
 * Public API for the Time Zone Bluetooth GATT Characteristic.
 *
 * <p>This class provides Time Zone Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Time Zone
 * Type: org.bluetooth.characteristic.time_zone
 * UUID: 2A0E
 * Last Modified: None
 * Revision: None
 */
public class TimeZone extends CharacteristicBase {
    /**
     * Time Zone UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0E"));

    // Customized Start: Constant Definition .......................................................
    public static final int UTC_N_1200 = -48;  // UTC-12:00
    public static final int UTC_N_1100 = -44;  // UTC-11:00
    public static final int UTC_N_1000 = -40;  // UTC-10:00
    public static final int UTC_N_930 = -38;  // UTC-9:30
    public static final int UTC_N_900 = -36;  // UTC-9:00
    public static final int UTC_N_800 = -32;  // UTC-8:00
    public static final int UTC_N_700 = -28;  // UTC-7:00
    public static final int UTC_N_600 = -24;  // UTC-6:00
    public static final int UTC_N_500 = -20;  // UTC-5:00
    public static final int UTC_N_430 = -18;  // UTC-4:30
    public static final int UTC_N_400 = -16;  // UTC-4:00
    public static final int UTC_N_330 = -14;  // UTC-3:30
    public static final int UTC_N_300 = -12;  // UTC-3:00
    public static final int UTC_N_200 = -8;  // UTC-2:00
    public static final int UTC_N_100 = -4;  // UTC-1:00
    public static final int UTC_000 = 0;  // UTC+0:00
    public static final int UTC_100 = 4;  // UTC+1:00
    public static final int UTC_200 = 8;  // UTC+2:00
    public static final int UTC_300 = 12;  // UTC+3:00
    public static final int UTC_330 = 14;  // UTC+3:30
    public static final int UTC_400 = 16;  // UTC+4:00
    public static final int UTC_430 = 18;  // UTC+4:30
    public static final int UTC_500 = 20;  // UTC+5:00
    public static final int UTC_530 = 22;  // UTC+5:30
    public static final int UTC_545 = 23;  // UTC+5:45
    public static final int UTC_600 = 24;  // UTC+6:00
    public static final int UTC_630 = 26;  // UTC+6:30
    public static final int UTC_700 = 28;  // UTC+7:00
    public static final int UTC_800 = 32;  // UTC+8:00
    public static final int UTC_845 = 35;  // UTC+8:45
    public static final int UTC_900 = 36;  // UTC+9:00
    public static final int UTC_930 = 38;  // UTC+9:30
    public static final int UTC_1000 = 40;  // UTC+10:00
    public static final int UTC_1030 = 42;  // UTC+10:30
    public static final int UTC_1100 = 44;  // UTC+11:00
    public static final int UTC_1130 = 46;  // UTC+11:30
    public static final int UTC_1200 = 48;  // UTC+12:00
    public static final int UTC_1245 = 51;  // UTC+12:45
    public static final int UTC_1300 = 52;  // UTC+13:00
    public static final int UTC_1400 = 56;  // UTC+14:00
    public static final int TIME_ZONE_IS_NOT_KNOWN = -128;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Time Zone
     * Requirement: Mandatory
     * Format: sint8
     * Unit: None
     */
    private byte[] mTimeZone = new byte[FormatUtils.SINT8_SIZE];


    /**
     * Create a TimeZone characteristic object.
     */
    public TimeZone() {
        setCharacteristic(null);
        setTimeZone(-48);
    }

    /**
     * Create a TimeZone characteristic object and init value.
     *
     * @param value Initial value
     */
    public TimeZone(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TimeZone characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeZone(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TimeZone characteristic object.
     *
     * @param timeZone Time Zone
     */
    public TimeZone(
            int timeZone) {
        setCharacteristic(null);
        setTimeZone(timeZone);
    }

    /**
     * Create a TimeZone characteristic object.
     *
     * @param timeZone Time Zone
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeZone(
            int timeZone,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setTimeZone(timeZone);
    }

    // Customized Start: Constructors ..............................................................

    /**
     * Create a TimeZone characteristic object and init value.
     *
     * @param tz Current timezone
     */
    public TimeZone(java.util.TimeZone tz) {
        setCharacteristic(null);

        int zone = TIME_ZONE_IS_NOT_KNOWN;
        if (tz != null) {
            zone = tz.getRawOffset() / 900000;  // unit: millisecond
            if (zone < UTC_N_1200 || zone > UTC_1400) {
                zone = TIME_ZONE_IS_NOT_KNOWN;  // time zone offset is not known
            }
        }
        setTimeZone(zone);
    }

    //.............................................................. Customized End: Constructors //

    /**
     * Get TimeZone characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportTimeZone()
                        ? mTimeZone.length : 0);
    }

    /**
     * Get TimeZone characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TimeZone characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportTimeZone()) {
            int fieldLen = mTimeZone.length;
            System.arraycopy(mTimeZone, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TimeZone characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportTimeZone()) {
            int fieldLen = mTimeZone.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTimeZone, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get TimeZone field value with int format.
     *
     * @return TimeZone field value
     */
    public int getTimeZone() {
        return FormatUtils.sint8ToInt(mTimeZone);
    }

    /**
     * Set TimeZone field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TimeZone field
     * @return      True, if the value has been set successfully
     */
    public boolean setTimeZone(int value) {
        if (!FormatUtils.sint8RangeCheck(value)) {
            return false;
        }
        mTimeZone = FormatUtils.intToSint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeZone support TimeZone field.
     *
     * @return  True, if TimeZone support TimeZone field.
     */
    public boolean isSupportTimeZone() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

