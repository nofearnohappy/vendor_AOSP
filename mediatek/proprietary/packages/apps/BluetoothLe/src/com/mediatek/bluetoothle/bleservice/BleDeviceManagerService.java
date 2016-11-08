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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.mediatek.bluetooth.BleGattDevice;
import com.mediatek.bluetooth.IBleDeviceManager;
import com.mediatek.bluetooth.IBleDeviceManagerCallback;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattCharacteristic;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattDescriptor;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattService;
import com.mediatek.bluetoothle.provider.DeviceParameterRecorder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a background service to communicate with remote Bluetooth Smart devices. The operations
 * supported include: GATT service discovery, characteristic/descriptor read/write etc.
 */

public class BleDeviceManagerService extends Service {

    private volatile int mClientCounter = 0;
    private static final String TAG = "BleDeviceManagerService";
    private static final String REQ = "[REQ]";
    private static final String RSP = "[RSP]";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private static BleDeviceManagerService sInstance;

    // Synchronized Locks
    private final Object mConnLock = new Object();
    private final Object mConnStateLock = new Object();
    private final Object mServiceDiscoveryLock = new Object();
    private final Object mClientReqLock = new Object();

    private BluetoothManager mBtManager;

    private final ClientReqQueue mClientOperationQueue = new ClientReqQueue();
    private final ConcurrentHashMap<BluetoothDevice, Integer> mDeviceStates =
            new ConcurrentHashMap<BluetoothDevice, Integer>();
    private final ClientDeviceMap mDeviceMap = new ClientDeviceMap();

    private final ClientCallbackMap mClientCallbacks = new ClientCallbackMap();

    private final HashSet<BluetoothDevice> mDiscoveringDevices = new HashSet<BluetoothDevice>();

    private final HandlerThread mGattWorker = new HandlerThread("GATT Worker");
    private Handler mGattServiceHandler;

    private BleDeviceManagerServiceBinder mBinder;

    // API methods
    public static synchronized BleDeviceManagerService getDeviceManagerService() {
        return sInstance;
    }

    private static synchronized void setDeviceManagerService(final BleDeviceManagerService srv) {
        sInstance = srv;
    }

    private List<BluetoothDevice> getAutoConnectDevices() {
        final List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

        final List<String>  addrList = DeviceParameterRecorder.getAutoConnectDeviceAddresses(this);

        if (addrList == null) {
            if (DBG) Log.d(TAG, " No auto-connect devices ");
            return deviceList;
        }

        final BluetoothAdapter adapter = mBtManager.getAdapter();

        for (String addr : addrList) {
            if (addr == null) continue;

            final BluetoothDevice device = adapter.getRemoteDevice(addr);

            if (device != null) {
                deviceList.add(device);
                if (DBG) Log.d(TAG, " Auto-connect device : " + addr);
            }
        }

        return deviceList;
    }

    private void autoConnect() throws RemoteException {
        if (VDBG) Log.v(TAG, "Try auto connect all devices");
        final List<BluetoothDevice> autoConnDevices = getAutoConnectDevices();

        if (autoConnDevices == null || autoConnDevices.size() == 0) {
            return;
        }

        // TODO: Remove the delay when stack can handle consecutive connection requests well

        final ClientDeviceConnector connector = ClientDeviceConnector.getInstance();

        int count = 0;

        for (final BluetoothDevice device : autoConnDevices) {
            if (device != null) {
                connector.connectDevice(device, count * 6000);
            }
            count++;
        }
    }

    private void disconnectAll() {
        if (VDBG) Log.v(TAG, "Disconnect all devices");
        final List<BluetoothGatt> gattClientList = this.mDeviceMap.getDeviceClientList();
        for (BluetoothGatt gattClient : gattClientList) {
            gattClient.close();
        }
    }

    // Return the cached GATT services for a device
    private List<BluetoothGattService> getCachedGattServices(BluetoothDevice device) {
        final GattCallbackImpl callback =
                (GattCallbackImpl) mDeviceMap.getDeviceClientCallback(device);

        if (callback == null) {
            Log.w(TAG, "No GattCallbackImpl for the device: " + device.getAddress());
            return null;
        } else {
            return callback.mGattServices;
        }
    }

    // Search the characteristic based on current cached list
    private BluetoothGattCharacteristic findGattCharacteristic(
            final List<BluetoothGattService> gattSrvList,
            final ParcelBluetoothGattService parcelSrv,
            final ParcelBluetoothGattCharacteristic parcelChar) {
        final UUID targetSrvUuid = parcelSrv.getUuid().getUuid();
        final int targetSrvInstance = parcelSrv.getInstanceId();

        BluetoothGattService parentSrv = null;

        if (gattSrvList == null) {
            Log.w(TAG, "findGattCharacteristic: No GATT services");
            return null;
        }

        for (final BluetoothGattService srv : gattSrvList) {
            final int srvId = srv.getInstanceId();
            final UUID srvUuid = srv.getUuid();
            if (srvId == targetSrvInstance && srvUuid.equals(targetSrvUuid)) {
                parentSrv = srv;
                if (VDBG) Log.v(TAG, "GATT Service Found: ID = " + srvId + ", UUID = " + srvUuid);
            }
        }

        if (parentSrv == null) {
            return null;
        }

        final List<BluetoothGattCharacteristic> gattCharList = parentSrv.getCharacteristics();

        for (final BluetoothGattCharacteristic gattChar : gattCharList) {
            final int charId = gattChar.getInstanceId();
            final UUID charUuid = gattChar.getUuid();
            if (charId == parcelChar.getInstanceId() && charUuid.equals(gattChar.getUuid())) {
                if (VDBG) Log.v(TAG, "GATT Char Found: ID = " + charId + ", UUID = " + charUuid);
                return gattChar;
            }
        }

        return null;
    }

    private void dumpGattDesc(BluetoothGattDescriptor gattDesc) {
        if (VDBG) {
            Log.v(TAG, "Dump Gatt Descriptor");
            Log.v(TAG, ">UUID:" + gattDesc.getUuid());
            Log.v(TAG, ">Perm:" + gattDesc.getPermissions());
        }
    }

    private void dumpGattChar(BluetoothGattCharacteristic gattChar) {
        if (VDBG) {
            Log.v(TAG, "Dump Gatt Characteristic");
            Log.v(TAG, ">InstanceID:" + gattChar.getInstanceId());
            Log.v(TAG, ">UUID:" + gattChar.getUuid());
            Log.v(TAG, ">Perm:" + gattChar.getPermissions());
            Log.v(TAG, ">Prop:" + gattChar.getProperties());
            Log.v(TAG, ">WriteType:" + gattChar.getWriteType());
        }
    }

    // Binder Implementation
    private class BleDeviceManagerServiceBinder extends IBleDeviceManager.Stub {

        private BleDeviceManagerService mDeviceManagerService;

        public BleDeviceManagerServiceBinder(final BleDeviceManagerService service) {
            mDeviceManagerService = service;
        }

        private BleDeviceManagerService getService() {
            return mDeviceManagerService;
        }

        public boolean cleanup() {
            mDeviceManagerService = null;
            return true;
        }

        // Client Callback Registration APIs
        @Override
        public int registerClient(final ParcelUuid uuid, final BluetoothDevice device,
                final IBleDeviceManagerCallback callback) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return 0;
            }

            return service.registerClient(uuid, device, callback);
        }

        @Override
        public void unregisterClient(final int clientId) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return;
            }

            service.unregisterClient(clientId);
        }

        // Device-specific APIs

        @Override
        public int getState(final int clientId, final BluetoothDevice device)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return BleGattDevice.STATE_DISCONNECTED;
            }

            return service.getState(clientId, device);
        }

        @Override
        public boolean connectDevice(final int clientId, final BluetoothDevice device)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.connectDevice(clientId, device);
        }

        @Override
        public boolean disconnectDevice(final int clientId, final BluetoothDevice device)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.disconnectDevice(clientId, device);
        }

        @Override
        public boolean discoverServices(final int clientId, final BluetoothDevice device)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.discoverServices(clientId, device);
        }

        @Override
        public ParcelBluetoothGattService getService(final BluetoothDevice device,
                final ParcelUuid serviceId) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return null;
            }

            return service.getService(device, serviceId);
        }

        @Override
        public List<ParcelBluetoothGattService> getServices(final BluetoothDevice device)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return null;
            }

            return service.getServices(device);
        }

        // GATT APIs
        @Override
        public boolean readCharacteristic(final int clientId, final int profileId,
                final BluetoothDevice device, final ParcelBluetoothGattCharacteristic parcelChar)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.readCharacteristic(clientId, profileId, device, parcelChar);
        }

        @Override
        public boolean readDescriptor(final int clientId, final int profileId,
                final BluetoothDevice device, final ParcelBluetoothGattDescriptor parcelDesc)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.readDescriptor(clientId, profileId, device, parcelDesc);
        }

        @Override
        public boolean readRemoteRssi(final int clientId, final int profileId,
                final BluetoothDevice device) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.readRemoteRssi(clientId, profileId, device);
        }

        @Override
        public boolean setCharacteristicNotification(final int clientId, final int profileId,
                final BluetoothDevice device, final ParcelBluetoothGattCharacteristic parcelChar,
                final boolean enable) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.setCharacteristicNotification(clientId, profileId, device, parcelChar,
                    enable);
        }

        @Override
        public boolean writeCharacteristic(final int clientId, final int profileId,
                final BluetoothDevice device, final ParcelBluetoothGattCharacteristic parcelChar)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.writeCharacteristic(clientId, profileId, device, parcelChar);
        }

        @Override
        public boolean writeDescriptor(final int clientId, final int profileId,
                final BluetoothDevice device, final ParcelBluetoothGattDescriptor parcelDesc)
                throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.writeDescriptor(clientId, profileId, device, parcelDesc);
        }

        @Override
        public boolean beginReliableWrite(final int clientId, final int profileId,
                final BluetoothDevice device) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.beginReliableWrite(clientId, profileId, device);
        }

        @Override
        public void abortReliableWrite(final int clientId, final int profileId,
                final BluetoothDevice device) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return;
            }

            service.abortReliableWrite(clientId, profileId, device);
        }

        @Override
        public boolean executeReliableWrite(final int clientId, final int profileId,
                final BluetoothDevice device) throws RemoteException {
            final BleDeviceManagerService service = getService();

            if (service == null) {
                return false;
            }

            return service.executeReliableWrite(clientId, profileId, device);
        }

        @Override
        public void addGattDevice(final BluetoothDevice device) {
            // insert device into BLEClient table
            if (device == null) return;

            final BleDeviceManagerService service = getService();
            if (service == null) {
                return ;
            }

            service.addGattDevice(device.getAddress());
        }

        @Override
        public void deleteGattDevice(final BluetoothDevice device) {
            // delete device from BleClient table
            if (device == null) return;

            final BleDeviceManagerService service = getService();
            if (service == null) {
                return ;
            }
            service.deleteGattDevice(device.getAddress());
        }

    };

    // Service APIs

    // Client Callback Registration APIs
    int registerClient(final ParcelUuid uuid, final BluetoothDevice device,
            final IBleDeviceManagerCallback callback) throws RemoteException {
        final int clientId = ++mClientCounter;

        if (DBG) Log.d(TAG, "registerClient: clientID = " + clientId);

        mClientCallbacks.register(clientId, device, callback);

        return clientId;
    }

    void unregisterClient(final int clientId) throws RemoteException {
        if (DBG) Log.d(TAG, "unregisterClient: clientID = " + clientId);

        mClientCallbacks.unregister(clientId);

    }

    // Device-specific APIs
    int getState(final int clientId, final BluetoothDevice device) throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "getState: device = " + device);

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            return BleGattDevice.STATE_DISCONNECTED;
        }

        Integer connState = BleGattDevice.STATE_DISCONNECTED;

        synchronized (mConnStateLock) {
            connState = mDeviceStates.get(device);
            if (connState == null) {
                connState = BluetoothProfile.STATE_DISCONNECTED;
            }
        }

        if (DBG) Log.d(TAG, REQ + "getState: state = " + connState);
        return connState;
    }

    boolean connectDevice(final int clientId, final BluetoothDevice device) throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "connectDevice: device = " + device);

        synchronized (mConnLock) {
            // Check BluetoothGatt
            final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

            if (VDBG) Log.v(TAG, "BleDeviceManagerService instance:" + this);
            if (VDBG) Log.v(TAG, "mDeviceStates:" + this.mDeviceStates);

            synchronized (mConnStateLock) {
                Integer deviceState = mDeviceStates.get(device);
                if (deviceState == null) {
                    deviceState = BluetoothProfile.STATE_DISCONNECTED;
                }

                if (BluetoothProfile.STATE_CONNECTED == deviceState
                        || BluetoothProfile.STATE_CONNECTING == deviceState) {
                    if (DBG) Log.d(TAG, "The device is already connecting or connected");
                    return false;
                }

                mDeviceStates.put(device, BluetoothProfile.STATE_CONNECTING);
            }

            // Not exist, direct connect
            if (clientInstance == null) {
                if (DBG) Log.d(TAG, "[New Device] Direct Connect");

                final BluetoothGattCallback cb = new GattCallbackImpl();

                final BluetoothGatt gattClient =
                        device.connectGatt(BleDeviceManagerService.this, false, cb);

                mDeviceMap.setDeviceClientData(device, gattClient, cb);
            } else {
                // Known device, background connect
                if (DBG) Log.d(TAG, "[Known Device] Background Connect");

                clientInstance.connect();
            }

            return true;
        }
    }

    boolean disconnectDevice(final int clientId, final BluetoothDevice device)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "disconnectDevice: device = " + device);

        synchronized (mConnLock) {
            // Check BluetoothGatt
            final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);
            final BluetoothGattCallback cb = mDeviceMap.getDeviceClientCallback(device);

            if (clientInstance == null) {
                Log.w(TAG, "clientInstance is null");
                return false;
            }

            final String deviceAddr = device.getAddress();

            this.mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (DBG) Log.d(TAG, REQ + "Simulate BluetoothGatt.STATE_DISCONNECTING");
                    cb.onConnectionStateChange(clientInstance, BluetoothGatt.GATT_SUCCESS,
                            BluetoothProfile.STATE_DISCONNECTING);

                    clientInstance.disconnect();
                    DeviceParameterRecorder.setAutoConnectFlag(
                        BleDeviceManagerService.getDeviceManagerService(),
                        deviceAddr, 0);
                }
            });

            return true;
        }
    }

    boolean discoverServices(final int clientId, final BluetoothDevice device)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "discoverServices: device = " + device);

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        synchronized (mServiceDiscoveryLock) {
            // The device is already discovering GATT services
            if (mDiscoveringDevices.contains(device)) {
                if (DBG) Log.d(TAG, "Already discovering the device = " + device);
                return false;
            }

            final boolean result = clientInstance.discoverServices();

            if (result) {
                mDiscoveringDevices.add(device);
            }

            return result;
        }
    }

    ParcelBluetoothGattService getService(final BluetoothDevice device, final ParcelUuid serviceId)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "getService: device = " + device + " ,uuid = " + serviceId);

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);

        if (cachedGattServices == null) {
            return null;
        }

        for (final BluetoothGattService service : cachedGattServices) {
            if (service.getUuid().equals(serviceId.getUuid())) {
                return ParcelBluetoothGattService.from(service, true);
            }
        }

        return null;
    }

    List<ParcelBluetoothGattService> getServices(final BluetoothDevice device)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "getServices device = " + device);

        final List<ParcelBluetoothGattService> result =
                new ArrayList<ParcelBluetoothGattService>(0);

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);

        if (cachedGattServices == null) {
            return null;
        }

        for (final BluetoothGattService srv : cachedGattServices) {
            result.add(ParcelBluetoothGattService.from(srv, true));
        }

        return result;
    }

    // GATT APIs
    boolean readCharacteristic(final int clientId, final int profileId,
            final BluetoothDevice device, final ParcelBluetoothGattCharacteristic parcelChar)
            throws RemoteException {
        if (parcelChar == null) {
            Log.w(TAG, "Invalid inputs");
            return false;
        }

        if (DBG) {
            Log.d(TAG, REQ + "readCharacteristic:" + " device = " + device + " ,uuid = "
                    + parcelChar.getUuid() + " ,instance = " + parcelChar.getInstanceId());
        }

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        if (VDBG) Log.v(TAG, parcelChar.toString());

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);
        final BluetoothGattCharacteristic outGattChar =
                findGattCharacteristic(cachedGattServices, parcelChar.getService(), parcelChar);

        if (outGattChar == null) {
            Log.w(TAG, "find Gatt Characteristic failed");
            return false;
        }

        dumpGattChar(outGattChar);

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_READ_CHAR, clientId, profileId);
            return clientInstance.readCharacteristic(outGattChar);
        }
    }

    boolean readDescriptor(final int clientId, final int profileId, final BluetoothDevice device,
            final ParcelBluetoothGattDescriptor parcelDesc) throws RemoteException {
        if (parcelDesc == null) {
            Log.w(TAG, "Invalid inputs");
            return false;
        }

        if (DBG) {
            Log.d(TAG,
                    REQ + "readDescriptor:" + " device = " + device + " ,uuid = "
                            + parcelDesc.getUuid() + " ,instance = " + parcelDesc.getInstanceId());
        }

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        if (VDBG) Log.v(TAG, parcelDesc.toString());

        ParcelBluetoothGattCharacteristic parentParcelChar = parcelDesc.getCharacteristic();
        ParcelBluetoothGattService parentParcelSrv = parentParcelChar.getService();

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);
        final BluetoothGattCharacteristic gattChar =
                findGattCharacteristic(cachedGattServices, parentParcelSrv, parentParcelChar);

        if (gattChar == null) {
            Log.w(TAG, "find Gatt Characteristic failed");
            return false;
        }

        dumpGattChar(gattChar);

        final BluetoothGattDescriptor outGattDesc =
                gattChar.getDescriptor(parcelDesc.getUuid().getUuid());

        if (outGattDesc == null) {
            Log.w(TAG, "find Gatt Descriptor failed");
            return false;
        }

        dumpGattDesc(outGattDesc);

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_READ_DESC, clientId, profileId);
            return clientInstance.readDescriptor(outGattDesc);
        }
    }

    boolean readRemoteRssi(final int clientId, final int profileId, final BluetoothDevice device)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "readRemoteRssi: device = " + device);

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_READ_RSSI, clientId, profileId);
            return clientInstance.readRemoteRssi();
        }
    }

    boolean setCharacteristicNotification(final int clientId, final int profileId,
            final BluetoothDevice device, final ParcelBluetoothGattCharacteristic parcelChar,
            final boolean enable) throws RemoteException {
        if (parcelChar == null) {
            Log.w(TAG, "Invalid inputs");
            return false;
        }

        if (DBG) {
            Log.d(TAG, REQ + "setCharacteristicNotification:" + " device = " + device + " ,uuid = "
                    + parcelChar.getUuid() + " ,instance = " + parcelChar.getInstanceId());
        }

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        if (VDBG) Log.v(TAG, parcelChar.toString());

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);
        final BluetoothGattCharacteristic outGattChar =
                findGattCharacteristic(cachedGattServices, parcelChar.getService(), parcelChar);

        if (outGattChar == null) {
            Log.w(TAG, "find Gatt Characteristic failed");
            return false;
        }

        dumpGattChar(outGattChar);

        synchronized (mClientReqLock) {
            // Put in waiting queue only when app wants to enable the
            // notification, onCharacteristicChange will be called
            if (enable) {
                mClientOperationQueue.onClientReq(ClientReqQueue.REQ_SET_CHAR_NOTIFY, clientId,
                        profileId);
            }

            return clientInstance.setCharacteristicNotification(outGattChar, enable);
        }
    }

    boolean writeCharacteristic(final int clientId, final int profileId,
            final BluetoothDevice device, final ParcelBluetoothGattCharacteristic parcelChar)
            throws RemoteException {
        if (parcelChar == null) {
            Log.w(TAG, "Invalid inputs");
            return false;
        }

        if (DBG) {
            Log.d(TAG, REQ + "writeCharacteristic:" + " device = " + device + " ,uuid = "
                    + parcelChar.getUuid() + " ,instance = " + parcelChar.getInstanceId());
        }

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        if (VDBG) Log.v(TAG, parcelChar.toString());

        // Get data from parcel
        final byte[] charValue = parcelChar.getValue();
        final int writeType = parcelChar.getWriteType();

        if (charValue != null) {
            for (int i = 0; i < charValue.length; i++) {
                if (DBG) Log.d(TAG, "-> Parcel Char Values:" + charValue[i]);
            }
        }

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);
        final BluetoothGattCharacteristic outGattChar =
                findGattCharacteristic(cachedGattServices, parcelChar.getService(), parcelChar);

        if (outGattChar == null) {
            Log.w(TAG, "find Gatt Characteristic failed");
            return false;
        }

        outGattChar.setValue(charValue);
        outGattChar.setWriteType(writeType);

        dumpGattChar(outGattChar);

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_WRITE_CHAR, clientId, profileId);
            return clientInstance.writeCharacteristic(outGattChar);
        }
    }

    boolean writeDescriptor(final int clientId, final int profileId, final BluetoothDevice device,
            final ParcelBluetoothGattDescriptor parcelDesc) throws RemoteException {
        if (parcelDesc == null) {
            Log.w(TAG, "Invalid inputs");
            return false;
        }

        if (DBG) {
            Log.d(TAG,
                    REQ + "writeDescriptor:" + " device = " + device + " ,uuid = "
                            + parcelDesc.getUuid() + " ,instance = " + parcelDesc.getInstanceId());
        }

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        // Get data from parcel
        final byte[] descValue = parcelDesc.getValue();

        if (descValue != null) {
            for (int i = 0; i < descValue.length; i++) {
                if (DBG) Log.d(TAG, "-> Parcel Desc Values:" + descValue[i]);
            }
        }

        if (VDBG) Log.v(TAG, parcelDesc.toString());

        ParcelBluetoothGattCharacteristic parentParcelChar = parcelDesc.getCharacteristic();
        ParcelBluetoothGattService parentParcelSrv = parentParcelChar.getService();

        final List<BluetoothGattService> cachedGattServices = this.getCachedGattServices(device);
        final BluetoothGattCharacteristic gattChar =
                findGattCharacteristic(cachedGattServices, parentParcelSrv, parentParcelChar);

        if (gattChar == null) {
            Log.w(TAG, "find Gatt Characteristic failed");
            return false;
        }

        dumpGattChar(gattChar);

        final BluetoothGattDescriptor outGattDesc =
                gattChar.getDescriptor(parcelDesc.getUuid().getUuid());

        if (outGattDesc == null) {
            Log.w(TAG, "Gatt Descriptor does not exist");
            return false;
        }

        outGattDesc.setValue(descValue);

        dumpGattDesc(outGattDesc);

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_WRITE_DESC, clientId, profileId);
            return clientInstance.writeDescriptor(outGattDesc);
        }
    }

    boolean beginReliableWrite(final int clientId, final int profileId, final BluetoothDevice device)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "beginReliableWrite: device = " + device);

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        return clientInstance.beginReliableWrite();
    }

    void abortReliableWrite(final int clientId, final int profileId, final BluetoothDevice device)
            throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "abortReliableWrite: device = " + device);

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return;
        }

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_RELIABLE_WRITE, clientId,
                    profileId);
            clientInstance.abortReliableWrite(device);
        }
    }

    boolean executeReliableWrite(final int clientId, final int profileId,
            final BluetoothDevice device) throws RemoteException {
        if (DBG) Log.d(TAG, REQ + "executeReliableWrite: device = " + device);

        // Check BluetoothGatt
        final BluetoothGatt clientInstance = mDeviceMap.getDeviceClient(device);

        if (clientInstance == null) {
            Log.w(TAG, "clientInstance is null");
            return false;
        }

        synchronized (mClientReqLock) {
            mClientOperationQueue.onClientReq(ClientReqQueue.REQ_RELIABLE_WRITE, clientId,
                    profileId);
            return clientInstance.executeReliableWrite();
        }
    }

    public void addGattDevice(final String deviceAddr) {
        DeviceParameterRecorder.insertNewDevice(BleDeviceManagerService.this, deviceAddr);
        DeviceParameterRecorder.setAutoConnectFlag(BleDeviceManagerService.this, deviceAddr, 1);
    }

    public void deleteGattDevice(final String deviceAddr) {
        DeviceParameterRecorder.deleteDevice(this, deviceAddr);
    }

    @Override
    public void onCreate() {
        if (VDBG) Log.v(TAG, "onCreate");

        if (VDBG) Log.v(TAG, "BleDeviceManagerService instance:" + this);
        if (VDBG) Log.v(TAG, "mDeviceStates:" + this.mDeviceStates);

        mBinder = new BleDeviceManagerServiceBinder(this);

        mBtManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);

        if (VDBG) Log.v(TAG, "mBtManager:" + mBtManager);

        // Prepare a thread to execute remote command in sequence
        mGattWorker.start();
        mGattServiceHandler = new Handler(mGattWorker.getLooper());
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (VDBG) {
            Log.v(TAG, "onStartCommand: intent=" + intent + " flags=" + flags + " startId="
                    + startId);
        }

        final String action = intent.getStringExtra(BleApp.EXTRA_ACTION);

        if (BleApp.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
            final int state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            if (state == BluetoothAdapter.STATE_OFF) {
                if (DBG) Log.d(TAG, "Stop Service");

                // Disconnect all devices
                disconnectAll();

                setDeviceManagerService(null);

                this.stopSelf();
            } else if (state == BluetoothAdapter.STATE_ON) {
                if (DBG) Log.d(TAG, "Start Service");

                setDeviceManagerService(this);

                // Auto-connect devices
                try {
                    autoConnect();
                } catch (final RemoteException e) {
                    Log.e(TAG, e + "");
                }
            }

        } else {
            // ignore
            Log.w(TAG, "Received unknown itent:" + intent);
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        if (VDBG) Log.v(TAG, "onBind");

        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        if (VDBG) Log.v(TAG, "onUnbind");

        mBinder.cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (VDBG) Log.v(TAG, "onDestroy");

        if (VDBG) Log.v(TAG, "BleDeviceManagerService instance:" + this);
        if (VDBG) Log.v(TAG, "mDeviceStates:" + this.mDeviceStates);

        mClientCallbacks.kill();
        mGattServiceHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                if (DBG) Log.d(TAG, "quit mGattWorker");
                mGattWorker.quit();
            }
        });
        mBtManager = null;
    }

    private class GattCallbackImpl extends BluetoothGattCallback {

        // Cache GATT Services
        private List<BluetoothGattService> mGattServices = null;

        // Broadcast Response
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status,
                final int newState) {
            if (DBG) Log.d(TAG, RSP + "onConnectionStateChange");

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int connStatus = status;
            final int connNewState = newState;

            // Update connection state
            if (status == BluetoothGatt.GATT_SUCCESS) {
                synchronized (mConnStateLock) {
                    mDeviceStates.put(targetDevice, newState);
                }
            }

            // Do some cleanup work when the device is disconnected
            if (BluetoothProfile.STATE_DISCONNECTED == newState) {
                // Clear service discovery state
                synchronized (mServiceDiscoveryLock) {
                    mDiscoveringDevices.remove(targetDevice);
                }
                // Clear services cache
                mGattServices = null;
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        if (mClientCallbacks.isValidCallback(i, targetDevice)) {
                            if (DBG) {
                                Log.d(TAG, "Invoke: onConnectionStateChange:" + " status = 0x"
                                        + Integer.toHexString(connStatus) + " ,newState = "
                                        + connNewState);
                            }

                            try {
                                mClientCallbacks.getBroadcastCallback(i).onConnectionStateChange(
                                        targetDevice.getAddress(), connStatus, connNewState);
                            } catch (final RemoteException e) {
                                Log.e(TAG, "" + e);
                            }
                        }
                    }

                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (DBG) Log.d(TAG, RSP + "onServicesDiscovered");

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int connStatus = status;

            // Cache GATT services within DeviceManagerService until new
            // services are discovered
            mGattServices = gatt.getServices();

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        if (mClientCallbacks.isValidCallback(i, targetDevice)) {
                            if (DBG) {
                                Log.d(TAG, "Invoke: onServicesDiscovered" + " status = 0x"
                                        + Integer.toHexString(connStatus));
                            }

                            try {
                                mClientCallbacks.getBroadcastCallback(i).onServicesChanged(
                                        targetDevice.getAddress(), connStatus);
                            } catch (final RemoteException e) {
                                Log.e(TAG, "" + e);
                            }
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");

                    // Not discovering
                    synchronized (mServiceDiscoveryLock) {
                        mDiscoveringDevices.remove(targetDevice);
                    }
                }
            });
        }

        // 1 to 1 Response
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt,
                final BluetoothGattCharacteristic characteristic) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_SET_CHAR_NOTIFY);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final BluetoothGattCharacteristic gattChar = characteristic;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onCharacteristicChanged: clientID = " + targetClientId
                        + ", profileID = " + targetProfileId);
            }

            final byte[] charValue = characteristic.getValue();

            if (charValue != null) {
                for (int i = 0; i < charValue.length; i++) {
                    if (DBG) Log.d(TAG, "-> Char Values:" + charValue[i]);
                }
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        try {
                            final int clientId = mClientCallbacks.getBroadcastClientId(i);
                            if (DBG) Log.d(TAG, "onCharacteristicChanged ClientID =" + clientId);

                            if (clientId == targetClientId) {
                                if (DBG) Log.d(TAG, "Invoke: onCharacteristicChanged");

                                mClientCallbacks.getBroadcastCallback(i).onCharacteristicChanged(
                                        targetDevice.getAddress(), targetProfileId,
                                        ParcelBluetoothGattCharacteristic.from(gattChar, true));
                            }
                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });

        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                final BluetoothGattCharacteristic characteristic, final int status) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_READ_CHAR);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int gattStatus = status;
            final BluetoothGattCharacteristic gattChar = characteristic;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onCharacteristicRead: clientID = " + targetClientId
                        + ", profileID = " + targetProfileId);
            }

            final byte[] charValue = characteristic.getValue();

            if (charValue != null) {
                for (int i = 0; i < charValue.length; i++) {
                    if (DBG) Log.d(TAG, "-> Char Values:" + charValue[i]);
                }
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        try {
                            final int clientId = mClientCallbacks.getBroadcastClientId(i);
                            if (DBG) Log.d(TAG, "onCharacteristicRead ClientID =" + clientId);

                            if (clientId == targetClientId) {
                                if (DBG) {
                                    Log.d(TAG, "Invoke: onCharacteristicRead" + " status = 0x"
                                            + Integer.toHexString(gattStatus));
                                }

                                mClientCallbacks.getBroadcastCallback(i).onCharacteristicRead(
                                        targetDevice.getAddress(), targetProfileId,
                                        ParcelBluetoothGattCharacteristic.from(gattChar, true),
                                        gattStatus);
                            }
                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });

        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt,
                final BluetoothGattCharacteristic characteristic, final int status) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_WRITE_CHAR);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int gattStatus = status;
            final BluetoothGattCharacteristic gattChar = characteristic;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onCharacteristicWrite: clientID = " + targetClientId
                        + ",profileID = " + targetProfileId);
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        try {
                            final int clientId = mClientCallbacks.getBroadcastClientId(i);
                            if (DBG) Log.d(TAG, "onCharacteristicWrite ClientID =" + clientId);

                            if (clientId == targetClientId) {
                                if (DBG) {
                                    Log.d(TAG, "Invoke: onCharacteristicWrite" + " status = 0x"
                                            + Integer.toHexString(gattStatus));
                                }

                                mClientCallbacks.getBroadcastCallback(i).onCharacteristicWrite(
                                        targetDevice.getAddress(), targetProfileId,
                                        ParcelBluetoothGattCharacteristic.from(gattChar, true),
                                        gattStatus);
                            }

                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });

        }

        @Override
        public void onDescriptorRead(final BluetoothGatt gatt,
                final BluetoothGattDescriptor descriptor, final int status) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_READ_DESC);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int gattStatus = status;
            final BluetoothGattDescriptor gattDesc = descriptor;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onDescriptorRead: clientID = " + targetClientId + ",profileID = "
                        + targetProfileId);
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        try {
                            final int clientId = mClientCallbacks.getBroadcastClientId(i);
                            if (DBG) Log.d(TAG, "onDescriptorRead ClientID =" + clientId);

                            if (clientId == targetClientId) {
                                if (DBG) {
                                    Log.d(TAG, "Invoke: onDescriptorRead" + " status = 0x"
                                            + Integer.toHexString(gattStatus));
                                }

                                mClientCallbacks.getBroadcastCallback(i).onDescriptorRead(
                                        targetDevice.getAddress(), targetProfileId,
                                        ParcelBluetoothGattDescriptor.from(gattDesc), gattStatus);
                            }

                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });

        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt,
                final BluetoothGattDescriptor descriptor, final int status) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_WRITE_DESC);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int gattStatus = status;
            final BluetoothGattDescriptor gattDesc = descriptor;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onDescriptorWrite: clientID = " + targetClientId
                        + ",profileID = " + targetProfileId);
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        try {
                            final int clientId = mClientCallbacks.getBroadcastClientId(i);
                            if (DBG) Log.d(TAG, "onDescriptorWrite ClientID =" + clientId);

                            if (clientId == targetClientId) {
                                if (DBG) {
                                    Log.d(TAG, "Invoke: onDescriptorWrite" + " status = 0x"
                                            + Integer.toHexString(gattStatus));
                                }

                                mClientCallbacks.getBroadcastCallback(i).onDescriptorWrite(
                                        targetDevice.getAddress(), targetProfileId,
                                        ParcelBluetoothGattDescriptor.from(gattDesc), gattStatus);
                            }

                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });

        }

        @Override
        public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_READ_RSSI);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int gattStatus = status;
            final int gattRssi = rssi;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onReadRemoteRssi: clientID = " + targetClientId + ",profileID = "
                        + targetProfileId);
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        final int clientId = mClientCallbacks.getBroadcastClientId(i);
                        if (DBG) Log.d(TAG, "onReadRemoteRssi ClientID =" + clientId);

                        try {
                            if (clientId == targetClientId) {
                                if (DBG) {
                                    Log.d(TAG, "Invoke: onReadRemoteRssi" + " status = 0x"
                                            + Integer.toHexString(gattStatus));
                                }

                                mClientCallbacks.getBroadcastCallback(i).onReadRemoteRssi(
                                        targetDevice.getAddress(), targetProfileId, gattRssi,
                                        gattStatus);

                            }

                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });

        }

        @Override
        public void onReliableWriteCompleted(final BluetoothGatt gatt, final int status) {
            // Get waiting client
            final Pair<Integer, Integer> compositeId =
                    mClientOperationQueue.onClientRsp(ClientReqQueue.REQ_RELIABLE_WRITE);

            final BluetoothDevice targetDevice = gatt.getDevice();
            final int gattStatus = status;

            final int targetClientId = compositeId.first;
            final int targetProfileId = compositeId.second;

            if (DBG) {
                Log.d(TAG, RSP + "onReliableWriteCompleted: clientID = " + targetClientId
                        + ",profileID = " + targetProfileId);
            }

            // Send to command queue
            mGattServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int count = mClientCallbacks.beginBroadcast();
                    if (VDBG) Log.v(TAG, "Broadcast Callback Count:" + count);

                    for (int i = 0; i < count; i++) {
                        try {
                            final int clientId = mClientCallbacks.getBroadcastClientId(i);
                            if (DBG) Log.d(TAG, "onReliableWriteCompleted ClientID =" + clientId);

                            if (clientId == targetClientId) {
                                if (DBG) {
                                    Log.d(TAG, "Invoke: onReliableWriteCompleted" + " status = 0x"
                                            + Integer.toHexString(gattStatus));
                                }

                                mClientCallbacks.getBroadcastCallback(i).onReliableWriteCompleted(
                                        targetDevice.getAddress(), targetProfileId, gattStatus);
                            }

                        } catch (final RemoteException e) {
                            Log.e(TAG, "" + e);
                        }
                    }
                    mClientCallbacks.finishBroadcast();
                    if (VDBG) Log.v(TAG, "Finish Callback");
                }
            });
        }
    }
}
