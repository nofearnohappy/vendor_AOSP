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

import android.util.Log;

import java.util.BitSet;

/**
 * This class provide Bit Field read/write operation for GATT characteristic BitField field type.
 */
public class BitField extends BitSet {
    private static final String TAG = "BitField";

    private static final int MAX_BIT_FIELD_SIZE = 32;
    private int mBitCount;

    /**
     * Create a BitField object.
     */
    public BitField() {
        this(MAX_BIT_FIELD_SIZE);
    }

    /**
     * Create a BitField object and init value.
     *
     * @param bitCount BitField size
     */
    public BitField(int bitCount) {
        super(bitCount);

        mBitCount = Math.min(bitCount, MAX_BIT_FIELD_SIZE);
    }

    @Override
    public int length() { return (mBitCount + 7) / 8; }

    /**
     * Sets a integer value into range of bits.
     *
     * @param fromIndex The index of start to set
     * @param toIndex The index of selection end
     * @param value New value for this BitField byte array
     * @return True, if index is not out of range
     */
    public boolean setValue(int fromIndex, int toIndex, int value) {

        if ((fromIndex | toIndex) < 0 || toIndex < fromIndex || fromIndex >= mBitCount) {
            Log.w(TAG, "setValue(" + fromIndex + ", " + toIndex + ", " + value +
                    ");  BitCount=" + mBitCount);
            return false;
        }

        toIndex = Math.min(toIndex, mBitCount - 1);

        int size = toIndex - fromIndex + 1;
        for (int i = 0; i < size; i++) {
            int state = (value >> i) & 0x1;

            if (state == 0) {
                clear(fromIndex + i);
            } else {
                set(fromIndex + i);
            }
        }

        return true;
    }

    /**
     * Retrieves a integer value from range of bits.
     *
     * @param fromIndex The index of start to retrieve
     * @param toIndex The index of selection end
     * @return a integer value
     */
    public int getValue(int fromIndex, int toIndex) {

        if ((fromIndex | toIndex) < 0 || toIndex < fromIndex || fromIndex >= mBitCount) {
            Log.w(TAG, "getValue(" + fromIndex + ", " + toIndex + ");  BitCount=" + mBitCount);
        }

        toIndex = Math.min(toIndex, mBitCount - 1);

        int size = toIndex - fromIndex + 1;
        int value = 0;

        for (int i = 0; i < size; i++) {
            if (i >= MAX_BIT_FIELD_SIZE) {
                break;
            }

            if (get(toIndex - i)) {
                value = (value << 1) | 1;
            } else {
                value = (value << 1);
            }
        }

        return value;
    }

    /**
     * Retrieves a byte value.
     *
     * @param byteIdx Byte index
     * @return a byte value
     */
    public byte getByte(int byteIdx) {
        int fromIndex = byteIdx * 8;
        int toIndex = byteIdx * 8 + 7;

        if (byteIdx < 0 || fromIndex >= mBitCount) {
            Log.w(TAG, "getByte(" + byteIdx + ")");
            return 0;
        }

        toIndex = Math.min(toIndex, mBitCount - 1);

        int b = 0;
        for (int i = fromIndex; i <= toIndex; i++) {
            if (get(i)) {
                b = b | (1 << (i - fromIndex));
            }
        }

        return (byte) (b & 0xFF);
    }

    /**
     * Retrieves a byte array value.
     *
     * @return a byte[] value
     */
    public byte[] getByteArray() {
        byte[] value = new byte[length()];

        for (int i = 0; i < length(); i++) {
            value[i] = getByte(i);
        }

        return value;
    }

    /**
     * Set a byte value of this BitField byte array.
     *
     * @param value New value for this BitField byte array
     * @param byteIdx Byte index
     * @return True, if byte index is right
     */
    public boolean setByte(byte value, int byteIdx) {
        int fromIndex = byteIdx * 8;
        int toIndex = byteIdx * 8 + 7;

        return setValue(fromIndex, toIndex, (int) value);
    }

    /**
     * Set a byte[] value of this BitField byte array.
     *
     * @param value New value for this BitField byte array
     */
    public void setByteArray(byte[] value) {
        for (int i = 0; i < value.length; i++) {
            setByte(value[i], i);
        }
    }
}

