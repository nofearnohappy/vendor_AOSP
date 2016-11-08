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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetoothle.fmp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.mediatek.bluetoothle.IBleProfileServer;
import com.mediatek.bluetoothle.bleservice.BleSingleProfileServerService;

import java.util.Arrays;

/**
 * Provides a service which implements Find Me Profile.
 * This class is a subclass of Android Service, and communicates with GATT interface.
 */
public class FmpServerService extends BleSingleProfileServerService {
    private static final String TAG = "FmpServerService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    static final int LEVEL_NO = 0;
    static final int LEVEL_MILD = 1;
    static final int LEVEL_HIGH = 2;
    private static final int ALERTER_TYPE_DEFAULT = 0;
    private static final int ALERT_LEVEL_OFFSET = 0;

    private IAlerter mAlerter;

    private final BluetoothGattServerCallback mCallback = new BluetoothGattServerCallback() {
        @Override
        public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId,
                final int offset, final BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            final IBleProfileServer bleProfileServer = FmpServerService.this.getBleProfileServer();

            if (DBG) {
                Log.d(TAG, "onCharacteristicReadRequest - incoming request: " + device.getName());
                Log.d(TAG, "onCharacteristicReadRequest -        requestId: " + requestId);
                Log.d(TAG, "onCharacteristicReadRequest -           offset: " + offset);
                Log.d(TAG, "onCharacteristicReadRequest -             uuid: "
                        + characteristic.getUuid().toString());
            }

            bleProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    Arrays.copyOfRange(data, offset, data.length));
        }

        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId,
                final BluetoothGattCharacteristic characteristic, final boolean preparedWrite,
                final boolean responseNeeded, final int offset, final byte[] value) {
            byte[] newValue = null;
            final byte[] oldValue = characteristic.getValue();
            final IBleProfileServer bleProfileServer = FmpServerService.this.getBleProfileServer();

            if (DBG) Log.d(TAG, "onCharacteristicWriteRequest - offset:" + offset + " "
                    + "value.length:" + value.length + " "
                    + "preparedWrite:" + preparedWrite + " "
                    + "responseNeeded:" + responseNeeded);

            if (null != oldValue && oldValue.length >= offset + value.length) {
                newValue = new byte[offset + value.length];
                System.arraycopy(oldValue, 0, newValue, 0, offset);
                System.arraycopy(value, 0, newValue, offset, value.length);
            } else {
                newValue = new byte[offset + value.length];
                if (null != oldValue) {
                    System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
                }
                System.arraycopy(value, 0, newValue, offset, value.length);
            }

            if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest- preparedWrite:" + preparedWrite);

            if (preparedWrite) {
                if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest - preparedWrite write\n");
            } else {
                if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest - a normal write\n");
                if (null != characteristic) {
                    characteristic.setValue(newValue);
                    final Integer level = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, ALERT_LEVEL_OFFSET);
                    if (null != level) {
                        if (DBG) Log.d(TAG, "level = " + level.intValue()
                                + ", mAlerter = " + mAlerter);
                        mAlerter.alert(level.intValue());
                    }
                }
            }

            if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest- responseNeeded:"
                    + responseNeeded);
            if (responseNeeded) {
                bleProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset, value);
            }

        }

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status,
                final int newState) {
            if (VDBG) Log.v(TAG, "onConnectionStateChange- device:" + device + " status:" + status
                    + " newState:" + newState);
        }

        @Override
        public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId,
                final int offset, final BluetoothGattDescriptor descriptor) {
            final byte[] data = descriptor.getValue();
            final IBleProfileServer bleProfileServer = FmpServerService.this.getBleProfileServer();

            if (VDBG) {
                Log.v(TAG, "onDescriptorReadRequest - incoming request: " + device.getName());
                Log.v(TAG, "onDescriptorReadRequest -        requestId: " + requestId);
                Log.v(TAG, "onDescriptorReadRequest -           offset: " + offset);
                Log.v(TAG, "onDescriptorReadRequest -             uuid: "
                        + descriptor.getUuid().toString());
            }

            bleProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    Arrays.copyOfRange(data, offset, data.length));

        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, final int requestId,
                final BluetoothGattDescriptor descriptor, final boolean preparedWrite,
                final boolean responseNeeded, final int offset, final byte[] value) {

            byte[] newValue = null;
            final byte[] oldValue = descriptor.getValue();
            final IBleProfileServer bleProfileServer = FmpServerService.this.getBleProfileServer();

            if (VDBG) Log.v(TAG, "onDescriptorWriteRequest - offset:" + offset + " "
                    + "value.length:" + value.length + " " + "preparedWrite:" + preparedWrite + " "
                    + "responseNeeded:" + responseNeeded);

            if (oldValue.length >= offset + value.length) {
                newValue = new byte[offset + value.length];
                System.arraycopy(oldValue, 0, newValue, 0, offset);
                System.arraycopy(value, 0, newValue, offset, value.length);
            } else {
                newValue = new byte[offset + value.length];
                System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
                System.arraycopy(value, 0, newValue, offset, value.length);
            }

            if (preparedWrite) {
                if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest - preparedWrite write\n");
            } else {
                if (VDBG) Log.v(TAG, "onDescriptorWriteRequest - a normal write\n");
                descriptor.setValue(newValue);
            }

            if (responseNeeded) {
                bleProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset, value);
            }
        }

        @Override
        public void onExecuteWrite(final BluetoothDevice device, final int requestId,
                final boolean execute) {
            if (VDBG) Log.v(TAG, "onExecuteWrite- device:" + device + " requestId:" + requestId
                    + " execute:" + execute);
        }

        @Override
        public void onServiceAdded(final int status, final BluetoothGattService service) {
            if (VDBG) Log.v(TAG, "onServiceAdded- status:" + status + " service:" + service);
        }
    };

    @Override
    public final void onCreate() {
        super.onCreate();
        if (VDBG) Log.v(TAG, "onCreate: FmpServerService");
        mAlerter = makeAlerter(ALERTER_TYPE_DEFAULT);
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        if (VDBG) Log.v(TAG, "onDestroy: FmpServerService");
        mAlerter.uninit();
    }

    @Override
    protected final int getProfileId() {
        return IBleProfileServer.FMP;
    }

    @Override
    protected final BluetoothGattServerCallback getDefaultBleProfileServerHandler() {
        return mCallback;
    }

    @Override
    protected final IProfileServiceBinder initBinder() {
        if (VDBG) Log.v(TAG, "initBinder: SDK is not provided, return null");
        return null;
    }

    private IAlerter makeAlerter(final int alerterType) {
        if (DBG) Log.d(TAG, "makeAlerter: alerterType = " + alerterType);
        switch (alerterType) {
            case ALERTER_TYPE_DEFAULT:
                return new DefaultAlerter(this);
            default:
                Log.e(TAG, "Invalid alerter type, return null");
                return null;
        }
    }
}
