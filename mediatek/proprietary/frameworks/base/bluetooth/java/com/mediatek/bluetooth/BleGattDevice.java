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

package com.mediatek.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IInterface;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.bluetooth.parcel.ParcelBluetoothGattCharacteristic;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattDescriptor;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Public API for the Bluetooth GATT Profile.
 *
 * <p>
 * This class is used to interact with a remote GATT capable device, The class provides
 * profile-specific interfaces for the ease of use.
 *
 * @hide
 */

public class BleGattDevice {

    /**
      * The profile is in disconnected state
      *
      * @internal
      */
    public static final int STATE_DISCONNECTED = BluetoothGatt.STATE_DISCONNECTED;

    /**
      * The profile is in connecting state
      *
      * @internal
      */
    public static final int STATE_CONNECTING = BluetoothGatt.STATE_CONNECTING;

    /**
      * The profile is in connected state
      *
      * @internal
      */
    public static final int STATE_CONNECTED = BluetoothGatt.STATE_CONNECTED;

    /**
      * The profile is in disconnecting state
      *
      * @internal
      */
    public static final int STATE_DISCONNECTING = BluetoothGatt.STATE_DISCONNECTING;

    /**
      * A GATT operation completed successfully
      *
      * @internal
      */
    public static final int GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS;

    /** GATT read operation is not permitted */
    public static final int GATT_READ_NOT_PERMITTED = BluetoothGatt.GATT_READ_NOT_PERMITTED;

    /** GATT write operation is not permitted */
    public static final int GATT_WRITE_NOT_PERMITTED = BluetoothGatt.GATT_WRITE_NOT_PERMITTED;

    /** Insufficient authentication for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHENTICATION =
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;

    /** The given request is not supported */
    public static final int GATT_REQUEST_NOT_SUPPORTED = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;

    /** Insufficient encryption for a given operation */
    public static final int GATT_INSUFFICIENT_ENCRYPTION =
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION;

    /** A read or write operation was requested with an invalid offset */
    public static final int GATT_INVALID_OFFSET = BluetoothGatt.GATT_INVALID_OFFSET;

    /** A write operation exceeds the maximum length of the attribute */
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH =
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;

    /**
      * A GATT operation failed, errors other than the above
      *
      * @internal
      */
    public static final int GATT_FAILURE = BluetoothGatt.GATT_FAILURE;

    private static final String TAG = "BleGattDevice";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private Map<Integer, BleProfile> mProfileRegistry;

    private final Object mDeviceOpenLock = new Object();
    private Context mContext; // Not used for now
    private int mClientId = 0;
    private BluetoothDevice mDevice;

    // Death receipient
    private IBinder.DeathRecipient mDeathRecipient;

    // Cross-profile callback for the mDevice
    private BleGattDeviceCallback mClientCB;

    // Common client interface for all profiles to interact with remote device via GATT profile
    private BleGattClientImpl mClientWrapper;

    private IBleDeviceManager mDeviceManager;

    // Common callback to receive all GATT client responses/notifications
    private IBleDeviceManagerCallback mCallback = new IBleDeviceManagerCallback.Stub() {

        // Device operation callback methods
        @Override
        public void onConnectionStateChange(String address, int status, int newState)
                throws RemoteException {
            if (DBG) Log.d(TAG, "onConnectionStateChange");

            if (mClientCB == null) {
                Log.w(TAG, "mClientCB is null");
                return;
            }

            mClientCB.onConnectionStateChange(BleGattDevice.this, status, newState);
        }

        @Override
        public void onServicesChanged(String address, int status) throws RemoteException {
            if (DBG) Log.d(TAG, "onServicesChanged");

            if (mClientCB == null) {
                Log.w(TAG, "mClientCB is null");
                return;
            }

            mClientCB.onServicesDiscovered(BleGattDevice.this, status);
        }

        // GATT operation callback methods
        @Override
        public void onCharacteristicRead(String address, int profileID,
                ParcelBluetoothGattCharacteristic characteristic, int status)
                throws RemoteException {
            BluetoothGattCharacteristic gattChar = characteristic.unpack();
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onCharacteristicRead(mClientWrapper, gattChar, status);
        }

        @Override
        public void onCharacteristicWrite(String address, int profileID,
                ParcelBluetoothGattCharacteristic characteristic, int status)
                throws RemoteException {
            BluetoothGattCharacteristic gattChar = characteristic.unpack();
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onCharacteristicWrite(mClientWrapper, gattChar, status);
        }

        @Override
        public void onCharacteristicChanged(String address, int profileID,
                ParcelBluetoothGattCharacteristic characteristic) throws RemoteException {
            BluetoothGattCharacteristic gattChar = characteristic.unpack();
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onCharacteristicChanged(mClientWrapper, gattChar);
        }

        @Override
        public void onDescriptorRead(String address, int profileID,
                ParcelBluetoothGattDescriptor descriptor, int status) throws RemoteException {
            BluetoothGattDescriptor gattDesc = descriptor.unpack();
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onDescriptorRead(mClientWrapper, gattDesc, status);
        }

        @Override
        public void onDescriptorWrite(String address, int profileID,
                ParcelBluetoothGattDescriptor descriptor, int status) throws RemoteException {
            BluetoothGattDescriptor gattDesc = descriptor.unpack();
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onDescriptorWrite(mClientWrapper, gattDesc, status);
        }

        @Override
        public void onReliableWriteCompleted(String address, int profileID, int status)
                throws RemoteException {
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onReliableWriteCompleted(mClientWrapper, status);
        }

        @Override
        public void onReadRemoteRssi(String address, int profileID, int rssi, int status)
                throws RemoteException {
            IBleGattCallback gattCb = mClientWrapper.getProfileCallback(profileID);

            if (gattCb == null) {
                Log.w(TAG, "IBleGattCallback is null");
                return;
            }

            gattCb.onReadRemoteRssi(mClientWrapper, rssi, status);
        }
    };

    /**
     * Create a BleGattDevice proxy object
     */
    BleGattDevice(Context context, IBleDeviceManager deviceManager, BluetoothDevice device,
            BleGattDeviceCallback clientCB) {
        if (VDBG) Log.v(TAG, "BleGattDevice created. instance = " + this);

        linkToDeath((IInterface) deviceManager, new DeathRecipient() {
            @Override
            public void binderDied() {
                if (DBG) Log.d(TAG, "BluetoothLe process died");
                // BleGattDevice.this.mDeviceManager = null;
            }
        });

        this.mContext = context;
        this.mDevice = device;
        this.mClientCB = clientCB;
        this.mProfileRegistry = new HashMap<Integer, BleProfile>();
        this.mDeviceManager = deviceManager;
        this.mClientWrapper = new BleGattClientImpl(deviceManager);

        openProfileClients();
    }

    /**
     * Link death recipient
     */
    private void linkToDeath(IInterface binderItf, IBinder.DeathRecipient deathRecipient) {
        try {
            IBinder binder = binderItf.asBinder();
            binder.linkToDeath(deathRecipient, 0);
            mDeathRecipient = deathRecipient;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to link deathRecipient for app");
        }
    }

    /**
     * Unlink death recipient
     */
    void unlinkToDeath(IInterface binderItf) {
        if (mDeathRecipient != null) {
            try {
                IBinder binder = binderItf.asBinder();
                binder.unlinkToDeath(mDeathRecipient, 0);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Unable to unlink deathRecipient");
            }
        }
    }

    /**
     * Register a callback
     */
    /* package */boolean startListen() {
        if (DBG) Log.d(TAG, "startListen");

        synchronized (mDeviceOpenLock) {

            if (mClientId != 0) {
                return false;
            }

            try {
                UUID uuid = UUID.randomUUID();

                mClientId =
                        mDeviceManager.registerClient(new ParcelUuid(uuid), this.mDevice,
                                this.mCallback);

            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        return true;
    }

    private void openProfileClients() {
        this.mProfileRegistry.put(BleProfile.FMP, new BleFindMeProfile(this.mDevice,
                mClientWrapper));
        this.mProfileRegistry.put(BleProfile.PXP, new BleProximityProfile(this.mDevice,
                mClientWrapper));

        Collection<BleProfile> profileClients = this.mProfileRegistry.values();

        for (BleProfile profile : profileClients) {
            profile.open();
        }

    }

    private void closeProfileClients() {
        Collection<BleProfile> profileClients = this.mProfileRegistry.values();

        for (BleProfile profile : profileClients) {
            profile.close();
        }

    }

    /**
     * Return the connection state of this particular remote bluetooth device
     *
     * @return connection state
     *
     * @internal
     */
    public int getState() {
        if (DBG) Log.d(TAG, "getState:" + this.mDevice.getAddress());

        if (this.mDeviceManager == null || this.mClientId == 0) {
            return STATE_DISCONNECTED;
        }

        try {
            return mDeviceManager.getState(mClientId, mDevice);
        } catch (RemoteException e) {
            return STATE_DISCONNECTED;
        }
    }

    /**
     * Return the remote bluetooth device this GATT client targets to
     *
     * @return remote bluetooth device
     *
     * @internal
     */
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * Connect to remote device. DeviceManagerService will decide to use direct or background
     * connection
     *
     * @return true, if the connection attempt was initiated successfully
     *
     * @internal
     */
    public boolean connect() {
        if (DBG) Log.d(TAG, "connect:" + this.mDevice.getAddress());

        if (this.mDeviceManager == null || this.mClientId == 0) {
            return false;
        }

        try {
            return this.mDeviceManager.connectDevice(this.mClientId, this.mDevice);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    /**
     * Disconnects an established connection, or cancels a connection attempt currently in progress.
     *
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @internal
     */
    public void disconnect() {
        if (DBG) Log.d(TAG, "disconnect");

        if (this.mDeviceManager == null || this.mClientId == 0) {
            return;
        }

        try {
            this.mDeviceManager.disconnectDevice(this.mClientId, this.mDevice);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Close this Bluetooth GATT client.
     *
     * Application should call this method as early as possible after it is done with this GATT
     * client.
     *
     * @internal
     */
    public void close() {
        if (DBG) Log.d(TAG, "close");

        if (this.mDeviceManager == null || this.mClientId == 0) {
            return;
        }

        try {
            this.mDeviceManager.unregisterClient(this.mClientId);

            unlinkToDeath((IInterface) this.mDeviceManager);

        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }

        // Clear all profile related data as well, e.g. BleFIndMeProfile.close()
        closeProfileClients();

        this.mClientCB = null;

        this.mClientId = 0;

    }

    /**
     * Discovers services offered by a remote device as well as their characteristics and
     * descriptors.
     *
     * <p>
     * This is an asynchronous operation. Once service discovery is completed, the
     * {@link BluetoothGattCallback#onServicesDiscovered} callback is triggered. If the discovery
     * was successful, the remote services can be retrieved using the {@link #getServices} function.
     *
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the remote service discovery has been started
     *
     * @internal
     */
    public boolean discoverServices() {
        if (DBG) Log.d(TAG, "discoverServices");

        if (this.mDeviceManager == null || this.mClientId == 0) {
            return false;
        }

        try {

            return this.mDeviceManager.discoverServices(this.mClientId, this.mDevice);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    /**
     * Returns a list of GATT services offered by the remote device.
     *
     * <p>
     * This function requires that service discovery has been completed for the given device.
     *
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return List of services on the remote device. Returns an empty list if service discovery has
     *         not yet been performed.
     *
     * @internal
     */
    public List<BluetoothGattService> getServices() {
        if (DBG) Log.d(TAG, "getServices");

        List<BluetoothGattService> srvs = new ArrayList<BluetoothGattService>(0);

        try {
            List<ParcelBluetoothGattService> services =
                    this.mDeviceManager.getServices(this.mDevice);

            if (services != null) {
                for (ParcelBluetoothGattService srv : services) {
                    srvs.add(srv.unpack());
                }
            }

        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }

        return srvs;

    }

    /**
     * Returns a {@link BluetoothGattService}, if the requested UUID is supported by the remote
     * device.
     *
     * <p>
     * This function requires that service discovery has been completed for the given device.
     *
     * <p>
     * If multiple instances of the same service (as identified by UUID) exist, the first instance
     * of the service is returned.
     *
     * <p>
     * Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param uuid
     *            UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested service is not offered by
     *         the remote device.
     *
     * @internal
     */
    public BluetoothGattService getService(UUID uuid) {
        if (DBG) Log.d(TAG, "getService");

        try {
            ParcelBluetoothGattService service =
                    this.mDeviceManager.getService(this.mDevice, new ParcelUuid(uuid));

            if (service != null) {
                return service.unpack();
            }

        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }

        return null;
    }

    /**
     * Return a profile-specific interface
     * <p>
     * The interface exposes simpler profile-specific APIs
     *
     * @return BleProfile a profile-specific interface
     *
     * @internal
     */
    public BleProfile asProfileInterface(int profileId) {
        if (VDBG) Log.v(TAG, "asProfileInterface");

        if (this.mDeviceManager == null || this.mClientId == 0) {
            return null;
        }

        // Return null if there are no GATT services for a profile

        // Make each profile interface singleton for a device

        if (profileId == BleProfile.FMP) {
            BleFindMeProfile profileClient =
                    (BleFindMeProfile) this.mProfileRegistry.get(profileId);
            return profileClient;
        }

        if (profileId == BleProfile.PXP) {
            BleProximityProfile profileClient =
                    (BleProximityProfile) this.mProfileRegistry.get(profileId);
            return profileClient;
        }

        return null;

    }

    /**
     * Define connection/service discovery callback
     *
     * <p>
     * The callback is responsible for cross-profile operations for a GATT device
     *
     */
    public interface BleGattDeviceCallback {

        void onConnectionStateChange(BleGattDevice gattDevice, int status, int newState);

        void onServicesDiscovered(BleGattDevice gattDevice, int status);
    }

    /**
     * Internal implementation for IBleGatt
     *
     * <p>
     * It provide common interface for all profile clients, and keeps a profile request / response
     * queue to make sure response is dispatched to correct requester
     *
     */
    private class BleGattClientImpl implements IBleGatt {

        private static final String TAG = "BleGattClientImpl";

        // Store callback for all profiles
        private Map<Integer, IBleGattCallback> mProfileCallbacks =
                new HashMap<Integer, IBleGattCallback>();

        private IBleDeviceManager mGattManager;

        /* package */BleGattClientImpl(IBleDeviceManager gattManager) {
            this.mGattManager = gattManager;
        }

        public IBleGattCallback getProfileCallback(int profileId) {
            return this.mProfileCallbacks.get(profileId);
        }

        @Override
        public void registerClientCallback(int profileId, IBleGattCallback callback) {
            // Save the callback for the profile
            mProfileCallbacks.put(profileId, callback);
        }

        @Override
        public void unregisterClientCallback(int profileId) {
            mProfileCallbacks.remove(profileId);
        }

        // Device-specific operations
        @Override
        public BluetoothDevice getDevice() {
            return BleGattDevice.this.getDevice();
        }

        @Override
        public List<BluetoothGattService> getServices() {
            return BleGattDevice.this.getServices();
        }

        @Override
        public BluetoothGattService getService(UUID uuid) {
            return BleGattDevice.this.getService(uuid);
        }

        // GATT Characteristic/Descriptor operations

        @Override
        public boolean readCharacteristic(int profileId,
                BluetoothGattCharacteristic characteristic) {
            if (DBG) Log.d(TAG, "readCharacteristic");

            ParcelBluetoothGattCharacteristic parcelChar =
                    ParcelBluetoothGattCharacteristic.from(characteristic);

            try {
                return this.mGattManager.readCharacteristic(mClientId, profileId, mDevice,
                        parcelChar);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        @Override
        public boolean writeCharacteristic(int profileId,
                BluetoothGattCharacteristic characteristic) {
            if (DBG) Log.d(TAG, "writeCharacteristic");

            ParcelBluetoothGattCharacteristic parcelChar =
                    ParcelBluetoothGattCharacteristic.from(characteristic);

            try {
                return this.mGattManager.writeCharacteristic(mClientId, profileId, mDevice,
                        parcelChar);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        @Override
        public boolean readDescriptor(int profileId, BluetoothGattDescriptor descriptor) {
            if (DBG) Log.d(TAG, "readDescriptor");

            ParcelBluetoothGattDescriptor parcelDesc =
                    ParcelBluetoothGattDescriptor.from(descriptor);

            try {
                return this.mGattManager.readDescriptor(mClientId, profileId, mDevice, parcelDesc);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        @Override
        public boolean writeDescriptor(int profileId, BluetoothGattDescriptor descriptor) {
            if (DBG) Log.d(TAG, "writeDescriptor");

            ParcelBluetoothGattDescriptor parcelDesc =
                    ParcelBluetoothGattDescriptor.from(descriptor);
            try {
                return this.mGattManager.writeDescriptor(mClientId, profileId, mDevice, parcelDesc);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        @Override
        public boolean readRemoteRssi(int profileId) {
            if (DBG) Log.d(TAG, "readRemoteRssi");

            try {
                return this.mGattManager.readRemoteRssi(mClientId, profileId, mDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        // Reliable write operations

        @Override
        public boolean beginReliableWrite(int profileId) {
            if (DBG) Log.d(TAG, "beginReliableWrite");

            // TODO Save the current reliable write profileID

            try {
                return this.mGattManager.beginReliableWrite(mClientId, profileId, mDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        @Override
        public boolean executeReliableWrite(int profileId) {
            if (DBG) Log.d(TAG, "executeReliableWrite");

            // TODO: Queue the profile request for the callback: onReliableWriteCompleted

            try {
                return this.mGattManager.executeReliableWrite(mClientId, profileId, mDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }
        }

        @Override
        public void abortReliableWrite(int profileId) {
            if (DBG) Log.d(TAG, "abortReliableWrite");

            // TODO: Queue the profile request for the callback: onReliableWriteCompleted

            try {
                this.mGattManager.abortReliableWrite(mClientId, profileId, mDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }

        @Override
        public boolean setCharacteristicNotification(int profileId,
                BluetoothGattCharacteristic characteristic, boolean enable) {
            // TODO: Keep a map char_instance_id -> list of registered profiles
            // TODO: If the profileID is already registered, no need to enable again

            if (DBG) Log.d(TAG, "setCharacteristicNotification");

            ParcelBluetoothGattCharacteristic parcelChar =
                    ParcelBluetoothGattCharacteristic.from(characteristic);

            try {
                this.mGattManager.setCharacteristicNotification(mClientId, profileId, mDevice,
                        parcelChar, enable);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                return false;
            }

            return true;
        }
    }
}
