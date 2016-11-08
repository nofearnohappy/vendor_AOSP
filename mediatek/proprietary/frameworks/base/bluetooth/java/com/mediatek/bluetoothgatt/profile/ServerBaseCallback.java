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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.util.Log;

import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;

import java.util.UUID;

/**
 * This class is used to implement ServerBase callbacks.
 */
public abstract class ServerBaseCallback extends BluetoothGattServerCallback {
    private static final boolean DBG = true;
    private static final String TAG = "ServerBaseCallback";

    protected ServerBase mProfileServer = null;
    void setProfileServer(ServerBase server) {
        mProfileServer = server;
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
            BluetoothGattCharacteristic characteristic) {
        if (mProfileServer == null) {
            Log.w(TAG, "mProfileServer is null");
            return;
        }

        if (offset != 0) {
            // Read Blob Request
            if (offset > characteristic.getValue().length) {
                mProfileServer.sendErrorResponse(device, requestId,
                        BluetoothGatt.GATT_INVALID_OFFSET);
                return;
            }
        }

        dispatchCharacteristicReadRequest(device, requestId, offset, characteristic);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
            BluetoothGattCharacteristic characteristic, boolean preparedWrite,
            boolean responseNeeded, int offset, byte[] value) {
        if (mProfileServer == null) {
            Log.w(TAG, "mProfileServer is null");
            return;
        }

        if (offset != 0 && !preparedWrite) {
            if (responseNeeded) {
                mProfileServer.sendErrorResponse(device, requestId,
                        BluetoothGatt.GATT_INVALID_OFFSET);
            }
            return;
        }

        dispatchCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                responseNeeded, offset, value);
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
            BluetoothGattDescriptor descriptor) {
        if (mProfileServer == null) {
            Log.w(TAG, "mProfileServer is null");
            return;
        }

        if (offset != 0) {
            mProfileServer.sendErrorResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET);
            return;
        }

        dispatchDescriptorReadRequest(device, requestId, offset, descriptor);
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
            BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
            int offset, byte[] value) {
        if (mProfileServer == null) {
            Log.w(TAG, "mProfileServer is null");
            return;
        }

        if (offset != 0) {
            if (responseNeeded) {
                mProfileServer.sendErrorResponse(device, requestId,
                        BluetoothGatt.GATT_INVALID_OFFSET);
            }
            return;
        }

        if (value.length != descriptor.getValue().length) {
            if (responseNeeded) {
                mProfileServer.sendErrorResponse(device, requestId,
                        BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH);
            }
            return;
        }

        dispatchDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                responseNeeded, offset, value);
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        if (mProfileServer == null) {
            Log.w(TAG, "mProfileServer is null");
            return;
        }

        mProfileServer.executeWrite(device, execute);
        mProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
    }

    /**
     * Callback invoked when the GATT server is ready for use or there is something
     * error during opening server.
     *
     * @param status {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} if the operation was
     *               successful
     * @param gattServer The GATT server of a given profile. This value might be null
     *                   if the status is not {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS}
     */
    public void onServerReady(int status, BluetoothGattServer gattServer) {
    }


    void dispatchCharacteristicReadRequest(BluetoothDevice device, int requestId,
            int offset, BluetoothGattCharacteristic characteristic) {
    }

    void dispatchCharacteristicWriteRequest(BluetoothDevice device, int requestId,
            BluetoothGattCharacteristic characteristic, boolean preparedWrite,
            boolean responseNeeded, int offset, byte[] value){
    }

    void dispatchDescriptorReadRequest(BluetoothDevice device, int requestId,
            int offset, BluetoothGattDescriptor descriptor) {
    }

    void dispatchDescriptorWriteRequest(BluetoothDevice device, int requestId,
            BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
            int offset, byte[] value) {
    }

    protected void sendResponse(BluetoothDevice device, int requestId, int offset, byte[] value) {
        if (mProfileServer == null) {
            return;
        }
        mProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
    }

    protected void sendErrorResponse(BluetoothDevice device, int requestId, int status) {
        if (mProfileServer == null) {
            return;
        }
        mProfileServer.sendResponse(device, requestId, status, 0, null);
    }

    protected void prepareWrite(BluetoothDevice device, Object obj, int offset, byte[] value,
            boolean isDescriptor) {
        if (mProfileServer == null) {
            return;
        }
        mProfileServer.prepareWrite(device, obj, offset, value, isDescriptor);
    }

    protected boolean updateCccd(BluetoothDevice device, BluetoothGattDescriptor descriptor,
            byte[] value) {
        if (mProfileServer == null) {
            return false;
        }
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        return mProfileServer.updateCccd(device, srvcUuid, charUuid, value);
    }

    protected byte[] getCccd(BluetoothDevice device, CharacteristicBase characteristicBase) {
        return getCccd(device, characteristicBase.getCharacteristic());
    }

    protected byte[] getCccd(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        if (mProfileServer == null) {
            return BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        return mProfileServer.getCccd(device, srvcUuid, charUuid);
    }
}
