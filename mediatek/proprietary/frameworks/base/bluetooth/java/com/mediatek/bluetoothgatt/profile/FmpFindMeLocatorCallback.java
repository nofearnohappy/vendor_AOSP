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


package com.mediatek.bluetoothgatt.profile;

// Customized Start: Import ........................................................................
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.AlertLevel;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;

import java.util.UUID;
//........................................................................ Customized End: Import //

/**
 * This class is used to implement {@link FmpFindMeLocator} callbacks.
 */
public class FmpFindMeLocatorCallback extends ClientBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "FmpFindMeLocatorCallback";

    @Override
    void dispatchCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;



        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_IAS)) {
             if (charUuid.equals(GattUuid.CHAR_ALERT_LEVEL)) {
                base = new AlertLevel();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onIasAlertLevelWriteResponse(
                        (AlertLevel) base, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;



        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchDescriptorRead(BluetoothGatt gatt,
            BluetoothGattDescriptor descriptor, int status) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();



        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    @Override
    void dispatchDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
            int status) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();



        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }



    /**
     * Callback indicating the result of a
     * Ias:AlertLevel characteristic write operation.
     *
     * @param alertLevel Ias:AlertLevel characteristic
     *                       that was written to the associated remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onIasAlertLevelWriteResponse(
            AlertLevel alertLevel, int status) {
    }







}

