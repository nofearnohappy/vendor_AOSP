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
 * Public API for the Time Update State Bluetooth GATT Characteristic.
 *
 * <p>This class provides Time Update State Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Time Update State
 * Type: org.bluetooth.characteristic.time_update_state
 * UUID: 2A17
 * Last Modified: None
 * Revision: None
 */
public class TimeUpdateState extends CharacteristicBase {
    /**
     * Time Update State UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A17"));

    // Customized Start: Constant Definition .......................................................
    /**
     * Current State Enumerations: Idle.
     */
    public static final int CURRENT_STATE_IDLE = 0;

    /**
     * Current State Enumerations: Update Pending.
     */
    public static final int CURRENT_STATE_UPDATE_PENDING = 1;

    /**
     * Result Enumerations: Successful.
     */
    public static final int RESULT_SUCCESSFUL = 0;

    /**
     * Result Enumerations: Canceled.
     */
    public static final int RESULT_CANCELED = 1;

    /**
     * Result Enumerations: No Connection To Reference.
     */
    public static final int RESULT_NO_CONNECTION_TO_REFERENCE = 2;

    /**
     * Result Enumerations: Reference responded with an error.
     */
    public static final int RESULT_REFERENCE_RESPOND_ERROR = 3;

    /**
     * Result Enumerations: Timeout.
     */
    public static final int RESULT_TIMEOUT = 4;

    /**
     * Result Enumerations: Update not attempted after reset.
     */
    public static final int RESULT_UPDATE_NOT_ATTEMPTED = 5;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Current State
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mCurrentState = new byte[FormatUtils.UINT8_SIZE];

    /*
     * Field: Result
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mResult = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a TimeUpdateState characteristic object.
     */
    public TimeUpdateState() {
        setCharacteristic(null);
        setCurrentState(0);
        setResult(0);
    }

    /**
     * Create a TimeUpdateState characteristic object and init value.
     *
     * @param value Initial value
     */
    public TimeUpdateState(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a TimeUpdateState characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeUpdateState(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a TimeUpdateState characteristic object.
     *
     * @param currentState Current State
     * @param result Result
     */
    public TimeUpdateState(
            int currentState,
            int result) {
        setCharacteristic(null);
        setCurrentState(currentState);
        setResult(result);
    }

    /**
     * Create a TimeUpdateState characteristic object.
     *
     * @param currentState Current State
     * @param result Result
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public TimeUpdateState(
            int currentState,
            int result,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setCurrentState(currentState);
        setResult(result);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get TimeUpdateState characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportCurrentState()
                        ? mCurrentState.length : 0)
                + (isSupportResult()
                        ? mResult.length : 0);
    }

    /**
     * Get TimeUpdateState characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get TimeUpdateState characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportCurrentState()) {
            int fieldLen = mCurrentState.length;
            System.arraycopy(mCurrentState, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        if (isSupportResult()) {
            int fieldLen = mResult.length;
            System.arraycopy(mResult, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set TimeUpdateState characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportCurrentState()) {
            int fieldLen = mCurrentState.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mCurrentState, 0, fieldLen);
            srcPos += fieldLen;
        }

        if (isSupportResult()) {
            int fieldLen = mResult.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mResult, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get CurrentState field value with int format.
     *
     * @return CurrentState field value
     */
    public int getCurrentState() {
        return FormatUtils.uint8ToInt(mCurrentState);
    }

    /**
     * Set CurrentState field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to CurrentState field
     * @return      True, if the value has been set successfully
     */
    public boolean setCurrentState(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mCurrentState = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeUpdateState support CurrentState field.
     *
     * @return  True, if TimeUpdateState support CurrentState field.
     */
    public boolean isSupportCurrentState() {
        return true;
    }

    /**
     * Get Result field value with int format.
     *
     * @return Result field value
     */
    public int getResult() {
        return FormatUtils.uint8ToInt(mResult);
    }

    /**
     * Set Result field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to Result field
     * @return      True, if the value has been set successfully
     */
    public boolean setResult(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mResult = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if TimeUpdateState support Result field.
     *
     * @return  True, if TimeUpdateState support Result field.
     */
    public boolean isSupportResult() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

