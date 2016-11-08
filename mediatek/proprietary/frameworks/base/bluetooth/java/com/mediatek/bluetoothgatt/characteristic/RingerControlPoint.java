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
 * Public API for the Ringer Control point Bluetooth GATT Characteristic.
 *
 * <p>This class provides Ringer Control point Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Ringer Control point
 * Type: org.bluetooth.characteristic.ringer_control_point
 * UUID: 2A40
 * Last Modified: None
 * Revision: None
 */
public class RingerControlPoint extends CharacteristicBase {
    /**
     * Ringer Control point UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A40"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Ringer Control Point Enumerations: Silent Mode.
     */
    public static final int RCP_SILENT_MODE = 1;

    /**
     * Ringer Control Point Enumerations: Mute Once.
     */
    public static final int RCP_MUTE_ONCE = 2;

    /**
     * Ringer Control Point Enumerations: Cancel Silent Mode.
     */
    public static final int RCP_CANCEL_SILENT_MODE = 3;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Ringer Control Point
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mRingerControlPoint = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a RingerControlPoint characteristic object.
     */
    public RingerControlPoint() {
        setCharacteristic(null);
        setRingerControlPoint(1);
    }

    /**
     * Create a RingerControlPoint characteristic object and init value.
     *
     * @param value Initial value
     */
    public RingerControlPoint(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a RingerControlPoint characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public RingerControlPoint(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a RingerControlPoint characteristic object.
     *
     * @param ringerControlPoint Ringer Control Point
     */
    public RingerControlPoint(
            int ringerControlPoint) {
        setCharacteristic(null);
        setRingerControlPoint(ringerControlPoint);
    }

    /**
     * Create a RingerControlPoint characteristic object.
     *
     * @param ringerControlPoint Ringer Control Point
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public RingerControlPoint(
            int ringerControlPoint,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setRingerControlPoint(ringerControlPoint);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get RingerControlPoint characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportRingerControlPoint()
                        ? mRingerControlPoint.length : 0);
    }

    /**
     * Get RingerControlPoint characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get RingerControlPoint characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportRingerControlPoint()) {
            int fieldLen = mRingerControlPoint.length;
            System.arraycopy(mRingerControlPoint, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set RingerControlPoint characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportRingerControlPoint()) {
            int fieldLen = mRingerControlPoint.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mRingerControlPoint, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get RingerControlPoint field value with int format.
     *
     * @return RingerControlPoint field value
     */
    public int getRingerControlPoint() {
        return FormatUtils.uint8ToInt(mRingerControlPoint);
    }

    /**
     * Set RingerControlPoint field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to RingerControlPoint field
     * @return      True, if the value has been set successfully
     */
    public boolean setRingerControlPoint(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mRingerControlPoint = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if RingerControlPoint support RingerControlPoint field.
     *
     * @return  True, if RingerControlPoint support RingerControlPoint field.
     */
    public boolean isSupportRingerControlPoint() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

