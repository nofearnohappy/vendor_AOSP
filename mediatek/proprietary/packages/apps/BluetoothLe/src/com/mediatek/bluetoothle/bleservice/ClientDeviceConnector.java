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

package com.mediatek.bluetoothle.bleservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.bluetooth.BleGattDevice;
import com.mediatek.bluetooth.IBleDeviceManagerCallback;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattCharacteristic;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattDescriptor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An internal utility class to help connect Bluetooth Smart devices in background The class will
 * help queue the connection request, and process the request one by one in a separate thread
 */

public class ClientDeviceConnector {

    private static final String TAG = "ClientDeviceConnector";
    private static final boolean DBG = true;

    private static final int MESSAGE_CLIENT_CONNECT = 21;

    private static final int MESSAGE_CLIENT_DISCOVER = 22;

    private static final int MESSAGE_CLIENT_STOP = 23;

    private static ClientDeviceConnector sInstance = new ClientDeviceConnector();

    private Handler mConnHandler;
    private HandlerThread mConnThread;

    private final ConcurrentHashMap<BluetoothDevice, Integer> mDeviceClientMap =
            new ConcurrentHashMap<BluetoothDevice, Integer>();

    // Device Manager Service Callback
    private final IBleDeviceManagerCallback mCallback = new IBleDeviceManagerCallback.Stub() {

        @Override
        public void onConnectionStateChange(final String address, final int status, final int state)
                throws RemoteException {

            if (BluetoothGatt.GATT_FAILURE != status && BleGattDevice.STATE_CONNECTED == state) {

                final BluetoothDevice device =
                        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

                final Message m = mConnHandler.obtainMessage(MESSAGE_CLIENT_DISCOVER);
                m.obj = device;

                mConnHandler.sendMessage(m);
            }
        }

        @Override
        public void onServicesChanged(final String address, final int arg1) throws RemoteException {
            // Services discovered, unregister callback
            final BleDeviceManagerService deviceManager =
                    BleDeviceManagerService.getDeviceManagerService();

            if (deviceManager == null) {
                Log.w(TAG, "BleDeviceManagerService is destroyed.");
                return;
            }

            final BluetoothDevice device =
                    BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

            final Message m = mConnHandler.obtainMessage(MESSAGE_CLIENT_STOP);
            m.obj = device;

            mConnHandler.sendMessage(m);
        }

        @Override
        public void onReliableWriteCompleted(final String arg0, final int arg1, final int arg2) {
            // Not used
        }

        @Override
        public void onReadRemoteRssi(final String arg0, final int arg1, final int arg2,
                final int arg3) throws RemoteException {
            // Not used
        }

        @Override
        public void onDescriptorWrite(final String arg0, final int arg1,
                final ParcelBluetoothGattDescriptor arg2, final int arg3) throws RemoteException {
            // Not used
        }

        @Override
        public void onDescriptorRead(final String arg0, final int arg1,
                final ParcelBluetoothGattDescriptor arg2, final int arg3) throws RemoteException {
            // Not used
        }

        @Override
        public void onCharacteristicWrite(final String arg0, final int arg1,
                final ParcelBluetoothGattCharacteristic arg2, final int arg3)
                throws RemoteException {
            // Not used
        }

        @Override
        public void onCharacteristicRead(final String arg0, final int arg1,
                final ParcelBluetoothGattCharacteristic arg2, final int arg3)
                throws RemoteException {
            // Not used
        }

        @Override
        public void onCharacteristicChanged(final String arg0, final int arg1,
                final ParcelBluetoothGattCharacteristic arg2) throws RemoteException {
            // Not used
        }

    };

    private ClientDeviceConnector() {

        this.start();

    }

    void start() {

        mConnThread = new HandlerThread("ClientDeviceConnector");
        mConnThread.start();

        mConnHandler = new ConnHandler(mConnThread.getLooper());
    }

    public static ClientDeviceConnector getInstance() {
        return sInstance;
    }

    public void connectDevice(final BluetoothDevice device) {

        final Message m = mConnHandler.obtainMessage(MESSAGE_CLIENT_CONNECT);
        m.obj = device;

        mConnHandler.sendMessage(m);
    }

    public void connectDevice(final BluetoothDevice device, final long wait) {

        final Message m = mConnHandler.obtainMessage(MESSAGE_CLIENT_CONNECT);
        m.obj = device;

        mConnHandler.sendMessageDelayed(m, wait);
    }

    class ConnHandler extends Handler {

        public ConnHandler(final Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {

            final int msgId = msg.what;
            final BluetoothDevice targetDevice = (BluetoothDevice) msg.obj;

            final BleDeviceManagerService deviceManager =
                    BleDeviceManagerService.getDeviceManagerService();

            if (deviceManager == null) {
                Log.w(TAG, "BleDeviceManagerService is destroyed.");
                return;
            }

            if (DBG) Log.d(TAG, "ConnHandler: msg = " + msgId);

            switch (msgId) {
            case MESSAGE_CLIENT_CONNECT:
                try {
                    Integer clientId = mDeviceClientMap.get(targetDevice);

                    if (clientId == null) {
                        // No clientID for the device yet

                        clientId =
                                deviceManager.registerClient(new ParcelUuid(UUID.randomUUID()),
                                        targetDevice, mCallback);

                        // Save a clientID for the device
                        mDeviceClientMap.put(targetDevice, clientId);

                    }

                    if (clientId != null) {
                        int state = deviceManager.getState(clientId, targetDevice);

                        if (BluetoothGatt.STATE_CONNECTED != state
                                && BluetoothGatt.STATE_CONNECTING != state) {
                            deviceManager.connectDevice(clientId, targetDevice);
                        } else {
                            if (DBG) Log.d(TAG, "Device is already connecting / connected");
                        }
                    }

                } catch (final RemoteException e) {
                    Log.e(TAG, e + "");
                }
                break;
            case MESSAGE_CLIENT_DISCOVER:
                try {
                    final Integer clientId = mDeviceClientMap.get(targetDevice);

                    if (clientId != null) {
                        deviceManager.discoverServices(clientId, targetDevice);
                    }

                } catch (final RemoteException e) {
                    Log.e(TAG, e + "");
                }
                break;
            case MESSAGE_CLIENT_STOP:
                try {
                    final Integer clientId = mDeviceClientMap.remove(targetDevice);

                    if (clientId != null) {
                        deviceManager.unregisterClient(clientId);
                    }

                } catch (final RemoteException e) {
                    Log.e(TAG, e + "");
                }
                break;
            default:
                Log.w(TAG, "Unknown Message");
                break;
            }

        };

    }
}
