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
 * Public API for the Tx Power Level Bluetooth GATT Characteristic.
 *
 * <p>This class provides Tx Power Level Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Tx Power Level
 * Type: org.bluetooth.characteristic.tx_power_level
 * UUID: 2A07
 * Last Modified: None
 * Revision: None
 */
public class TxPowerLevel extends CharacteristicBase {
    /**
     * Tx Power Level UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A07"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Tx Power
     * Requirement: Mandatory
     * Format: sint8
     * Unit: org.bluetooth.unit.logarithmic_radio_quantity.decibel
     */
    private byte[] mTxPower = new byte[FormatUtils.SINT8_SIZE];


    /**
     * Create a TxPowerLevel characteristic object.
     */
    public TxPowerLevel() {
        setCharacteristic(null);
        setTxPower(-100);
    }

    /**
     * Create a TxPowerLevel characteristic object and init value.
     *
     * @param value Initial value
     */
    public TxPowerLevel(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TxPowerLevel characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TxPowerLevel(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TxPowerLevel characteristic object.
     *
     * @param txPower Tx Power
     */
    public TxPowerLevel(
            int txPower) {
        setCharacteristic(null);
        setTxPower(txPower);
    }

    /**
     * Create a TxPowerLevel characteristic object.
     *
     * @param txPower Tx Power
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TxPowerLevel(
            int txPower,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setTxPower(txPower);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get TxPowerLevel characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportTxPower()
                        ? mTxPower.length : 0);
    }

    /**
     * Get TxPowerLevel characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TxPowerLevel characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportTxPower()) {
            int fieldLen = mTxPower.length;
            System.arraycopy(mTxPower, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TxPowerLevel characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportTxPower()) {
            int fieldLen = mTxPower.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mTxPower, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get TxPower field value with int format.
     *
     * @return TxPower field value
     */
    public int getTxPower() {
        return FormatUtils.sint8ToInt(mTxPower);
    }

    /**
     * Set TxPower field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to TxPower field
     * @return      True, if the value has been set successfully
     */
    public boolean setTxPower(int value) {
        if (!FormatUtils.sint8RangeCheck(value)) {
            return false;
        }
        mTxPower = FormatUtils.intToSint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TxPowerLevel support TxPower field.
     *
     * @return  True, if TxPowerLevel support TxPower field.
     */
    public boolean isSupportTxPower() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

