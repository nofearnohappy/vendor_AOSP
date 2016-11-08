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
 * Public API for the Software Revision String Bluetooth GATT Characteristic.
 *
 * <p>This class provides Software Revision String Bluetooth GATT Characteristic
 * value encode/decode functions. Allowing applications easy and quick to
 * read/write characteristic field value.
 *
 * <p>Name: Software Revision String
 * Type: org.bluetooth.characteristic.software_revision_string
 * UUID: 2A28
 * Last Modified: 2011-05-24
 * Revision: None
 */
public class SoftwareRevisionString extends CharacteristicBase {
    /**
     * Software Revision String UUID.
     */
    public static final UUID GATT_UUID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A28"));

    // Customized Start: Constant Definition .......................................................

    //....................................................... Customized End: Constant Definition //

    /*
     * Field: Software Revision
     * Requirement: Mandatory
     * Format: utf8s
     * Unit: None
     */
    private byte[] mSoftwareRevision = new byte[0];


    /**
     * Create a SoftwareRevisionString characteristic object.
     */
    public SoftwareRevisionString() {
        setCharacteristic(null);
        setSoftwareRevision("");
    }

    /**
     * Create a SoftwareRevisionString characteristic object and init value.
     *
     * @param value Initial value
     */
    public SoftwareRevisionString(byte[] value) {
        setCharacteristic(null);
        setValue(value);
    }

    /**
     * Create a SoftwareRevisionString characteristic object and init value.
     *
     * @param value Initial value
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SoftwareRevisionString(byte[] value, BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setValue(value);
    }

    /**
     * Create a SoftwareRevisionString characteristic object.
     *
     * @param softwareRevision Software Revision
     */
    public SoftwareRevisionString(
            String softwareRevision) {
        setCharacteristic(null);
        setSoftwareRevision(softwareRevision);
    }

    /**
     * Create a SoftwareRevisionString characteristic object.
     *
     * @param softwareRevision Software Revision
     * @param characteristic the gatt-characteristic this characteristic belongs to.
     */
    public SoftwareRevisionString(
            String softwareRevision,
            BluetoothGattCharacteristic characteristic) {
        setCharacteristic(characteristic);
        setSoftwareRevision(softwareRevision);
    }

    // Customized Start: Constructors ..............................................................

    //.............................................................. Customized End: Constructors //

    /**
     * Get SoftwareRevisionString characteristic byte length.
     *
     * @return Byte length of this characteristic
     */
    public int length() {
        return (isSupportSoftwareRevision()
                        ? mSoftwareRevision.length : 0);
    }

    /**
     * Get SoftwareRevisionString characteristic UUID.
     *
     * @return Characteristic UUID
     */
    @Override
    public UUID getUuid() {
        return GATT_UUID;
    }

    /**
     * Get SoftwareRevisionString characteristic value.
     *
     * @return Byte array value of this characteristic.
     */
    @Override
    public byte[] getValue() {
        byte[] value = new byte[length()];
        int dstPos = 0;

        if (isSupportSoftwareRevision()) {
            int fieldLen = mSoftwareRevision.length;
            System.arraycopy(mSoftwareRevision, 0, value, dstPos, fieldLen);
            dstPos += fieldLen;
        }

        return value;
    }

    /**
     * Set SoftwareRevisionString characteristic value.
     *
     * @param value Byte array value
     * @return      True, if the value has been set successfully
     */
    @Override
    public boolean setValue(byte[] value) {
        int srcPos = 0;

        if (isSupportSoftwareRevision()) {
            mSoftwareRevision = new byte[value.length - srcPos];
            int fieldLen = mSoftwareRevision.length;
            System.arraycopy(value, srcPos, mSoftwareRevision, 0, fieldLen);
            srcPos += fieldLen;
        }

        updateGattCharacteristic();
        return true;
    }

    /**
     * Get SoftwareRevision field value with String format.
     *
     * @return SoftwareRevision field value
     */
    public String getSoftwareRevision() {
        return FormatUtils.utf8sToString(mSoftwareRevision);
    }

    /**
     * Set SoftwareRevision field value by String format
     * and update the related Bluetooth GATT Characteristic.
     *
     * @param value Value to write to SoftwareRevision field
     * @return  True
     */
    public boolean setSoftwareRevision(String value) {
        mSoftwareRevision = FormatUtils.stringToUtf8s(value);
        updateGattCharacteristic();
        return true;
    }

    /**
     * Return true if SoftwareRevisionString support SoftwareRevision field.
     *
     * @return  True, if SoftwareRevisionString support SoftwareRevision field.
     */
    public boolean isSupportSoftwareRevision() {
        return true;
    }


    // Customized Start: Functions .................................................................

    //................................................................. Customized End: Functions //
}

