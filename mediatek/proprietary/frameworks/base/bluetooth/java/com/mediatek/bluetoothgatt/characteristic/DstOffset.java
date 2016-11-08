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
 * Public API for the DST Offset Bluetooth GATT Characteristic.
 *
 * <p>This class provides DST Offset Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: DST Offset
 * Type: org.bluetooth.characteristic.dst_offset
 * UUID: 2A0D
 * Last Modified: None
 * Revision: None
 */
public class DstOffset extends CharacteristicBase {
    /**
     * DST Offset UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0D"));

    // Customized Start: Constant Definition .......................................................
    public static final int DST_STANDARD_TIME = 0;
    public static final int DST_HALF_AN_HOUR_DAYLIGHT_TIME = 2;
    public static final int DST_DAYLIGHT_TIME = 4;
    public static final int DST_DOUBLE_DAYLIGHT_TIME = 8;
    public static final int DST_IS_NOT_KNOWN = 255;
    //....................................................... Customized End: Constant Definition //

    /*
     * Field: DST Offset
     * Requirement: Mandatory
     * Format: uint8
     * Unit: None
     */
    private byte[] mDstOffset = new byte[FormatUtils.UINT8_SIZE];


    /**
     * Create a DstOffset characteristic object.
     */
    public DstOffset() {
        setCharacteristic(null);
        setDstOffset(0);
    }

    /**
     * Create a DstOffset characteristic object and init value.
     *
     * @param value Initial value
     */
    public DstOffset(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a DstOffset characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public DstOffset(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a DstOffset characteristic object.
     *
     * @param dstOffset DST Offset
     */
    public DstOffset(
            int dstOffset) {
        setCharacteristic(null);
        setDstOffset(dstOffset);
    }

    /**
     * Create a DstOffset characteristic object.
     *
     * @param dstOffset DST Offset
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public DstOffset(
            int dstOffset,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setDstOffset(dstOffset);
    }

    // Customized Start: Constructors ..............................................................

    /**
     * Create a DstOffset characteristic object and init value.
     *
     * @param tz Current timezone.
     */
    public DstOffset(java.util.TimeZone tz) {
        setCharacteristic(null);

        int dst = DST_IS_NOT_KNOWN;
        if (tz != null) {
            dst = tz.getDSTSavings() / 900000;  // unit: millisecond
            if (dst < DST_STANDARD_TIME || dst > DST_DOUBLE_DAYLIGHT_TIME) {
                dst = DST_IS_NOT_KNOWN;  // DST is not known
            }
        }
        setDstOffset(dst);
    }
    //.............................................................. Customized End: Constructors //

    /**
     * Get DstOffset characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportDstOffset()
                        ? mDstOffset.length : 0);
    }

    /**
     * Get DstOffset characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get DstOffset characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportDstOffset()) {
            int fieldLen = mDstOffset.length;
            System.arraycopy(mDstOffset, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set DstOffset characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportDstOffset()) {
            int fieldLen = mDstOffset.length;

            if (!setValueRangeCheck(value.length, srcPos, fieldLen)) {
                return false;
            }

            System.arraycopy(value, srcPos, mDstOffset, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get DstOffset field value with int format.
     *
     * @return DstOffset field value
     */
    public int getDstOffset() {
        return FormatUtils.uint8ToInt(mDstOffset);
    }

    /**
     * Set DstOffset field value by int format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to DstOffset field
     * @return      True, if the value has been set successfully
     */
    public boolean setDstOffset(int value) {
        if (!FormatUtils.uint8RangeCheck(value)) {
            return false;
        }
        mDstOffset = FormatUtils.intToUint8(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if DstOffset support DstOffset field.
     *
     * @return  True, if DstOffset support DstOffset field.
     */
    public boolean isSupportDstOffset() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

