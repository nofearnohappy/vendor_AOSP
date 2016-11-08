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
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

/**
 * Public APIs for the Bluetooth GATT characteristic.
 */
public abstract class CharacteristicBase {
    /**
     * Back-reference to the gatt-characteristic this characteristic belongs to.
     */
    private BluetoothGattCharacteristic mCharacteristic;

    /**
     * Returns the gatt-characteristic this characteristic belongs to.
     *
     * @return BluetoothGattCharacteristic
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    /**
     * Sets the gatt-characteristic with this characteristic.
     *
     * @param characteristic BluetoothGattCharacteristic
     */
    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
    }

    /*package*/ void updateGattCharacteristic() {
        if (mCharacteristic != null) {
            mCharacteristic.setValue(getValue());
        }
    }

    /**
     * Returns the UUID of this characteristic.
     *
     * @return UUID of this characteristic
     */
    public abstract UUID getUuid();

    /**
     * Get the stored value for this characteristic.
     *
     * @return Cached value of the characteristic
     */
    public abstract byte[] getValue();

    /**
     * Get a byte value for this characteristic.
     *
     * @param offset Offset at which the byte[] value can be found.
     * @return Cached value of the characteristic
     */
    public byte[] getValue(int offset) {
        byte[] value = getValue();

        if (offset != 0) {
            return Arrays.copyOfRange(value, offset, value.length - 1);
        } else {
            return value;
        }
    }

    /**
     * Updates the locally stored value of this characteristic.
     *
     * @param value New value for this characteristic
     * @return True, if the locally stored value has been set
     */
    public abstract boolean setValue(byte[] value);

    /**
     * Updates the locally stored value of this characteristic.
     *
     * @param offset Offset at which the value should be placed
     * @param value New value for this characteristic
     * @return True, if the locally stored value has been set
     */
    public boolean setValue(int offset, byte[] value) {
        byte newValue[] = getValue();
        int copyLength = value.length;

        if (offset < 0) {
            offset = 0;
        }

        if (copyLength > (newValue.length - offset)) {
            copyLength = (newValue.length - offset);
        }

        System.arraycopy(value, 0, newValue, offset, copyLength);
        return setValue(newValue);
    }

    /**
     * The byte array length of this characteristic.
     *
     * @return Byte array length
     */
    public abstract int length();

    @Override
    public String toString() {
        byte[] value = getValue();

        if (value == null) {
            return (getClass().getSimpleName() + " = [ null ]");
        }

        StringBuilder sb = new StringBuilder();

        sb.append(getClass().getSimpleName());
        sb.append(" = [ ");

        for (byte aValue : value) {
            sb.append(String.format("%02X ", aValue));
        }

        sb.append("], Length=");
        sb.append(value.length);

        return sb.toString();
    }

    /**
     * Check require size is not out of range.
     *
     * @param size Byte array size
     * @param offset Require byte array position
     * @param require Require size
     * @return True, if require size is not out of range.
     */
    public boolean setValueRangeCheck(int size, int offset, int require) {
        if ((offset + require) > size) {
            Log.e(getClass().getSimpleName(),
                    "setValueRangeCheck() Input parameter size is wrong! size=" + size +
                    ", offset=" + offset +
                    ", required=" + require);

            return false;
        }
        return true;
    }
}
