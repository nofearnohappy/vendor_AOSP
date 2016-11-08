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
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.FormatUtils;
import com.mediatek.bluetoothgatt.service.ServiceBase;
import com.mediatek.bluetoothgatt.service.ServiceCfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Public APIs for the GATT-based profiles (Server).
 */
public abstract class ServerBase {
    private static final boolean DBG = true;
    private static final String TAG = "ServerBase";

    private final Context mContext;
    private final Object mAddServiceLock = new Object();
    private static final long ADD_SERVICE_TIMEOUT = 500; // 0.5 seconds
    private static final ReentrantLock mLock = new ReentrantLock(true);

    /**
     * List of connected devices.
     */
    private final List<BluetoothDevice> mClients = new ArrayList<>();

    /**
     * Client Characteristic Configuration Descriptor settings.
     */
    protected ClientCharacteristicConfiguration mCccd = null;

    protected ServerBaseCallback mCallback;

    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (DBG) {
                Log.d(TAG, "onConnectionStateChange() - device=" + device.getAddress() +
                        ", status=" + status +
                        ", newState=" + newState);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                synchronized (mClients) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (!mClients.contains(device)) {
                            mClients.add(device);
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (mClients.contains(device)) {
                            mClients.remove(device);
                        }
                        try {
                            mCccd.removeDevice(device);
                        } catch (NullPointerException ex) {
                            Log.w(TAG, "CCCD list is null");
                        }
                    }
                }
            }

            try {
                mCallback.onConnectionStateChange(device, status, newState);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onConnectionStateChange() - NullPointerException " + ex);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (DBG) {
                Log.d(TAG, "onServiceAdded() - service=" + service.getUuid() +
                        ", status=" + status);
            }

            try {
                mCallback.onServiceAdded(status, service);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onServiceAdded() - NullPointerException " + ex);
            }

            synchronized (mAddServiceLock) {
                Log.d(TAG, "notifyAll()");
                mAddServiceLock.notifyAll();
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                int offset, BluetoothGattCharacteristic characteristic) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicReadRequest() - UUID=" + characteristic.getUuid() +
                        ", device=" + device.getAddress() +
                        ", requestId=" + requestId +
                        ", offset=" + offset);
            }

            try {
                mCallback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onCharacteristicReadRequest() - NullPointerException " + ex);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                boolean responseNeeded, int offset, byte[] value) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicWriteRequest() - UUID=" + characteristic.getUuid() +
                        ", device=" + device.getAddress() +
                        ", requestId=" + requestId +
                        ", preparedWrite=" + preparedWrite +
                        ", responseNeeded=" + responseNeeded +
                        ", offset=" + offset);
            }

            try {
                mCallback.onCharacteristicWriteRequest(device, requestId, characteristic,
                        preparedWrite, responseNeeded, offset, value);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onCharacteristicWriteRequest() - NullPointerException " + ex);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattDescriptor descriptor) {
            if (DBG) {
                Log.d(TAG, "onDescriptorReadRequest() - UUID=" + descriptor.getUuid() +
                        ", device=" + device.getAddress() +
                        ", requestId=" + requestId +
                        ", offset=" + offset);
            }

            final UUID descrUuid = descriptor.getUuid();
            if (descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                final UUID charUuid = descriptor.getCharacteristic().getUuid();
                final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
                descriptor.setValue(mCccd.getValue(device, srvcUuid, charUuid));
            }

            try {
                mCallback.onDescriptorReadRequest(device, requestId, offset, descriptor);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onDescriptorReadRequest() - NullPointerException " + ex);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                int offset, byte[] value) {
            if (DBG) {
                Log.d(TAG, "onDescriptorWriteRequest() - UUID=" + descriptor.getUuid() +
                        ", device=" + device.getAddress() +
                        ", requestId=" + requestId +
                        ", preparedWrite=" + preparedWrite +
                        ", responseNeeded=" + responseNeeded +
                        ", offset=" + offset);
            }

            final UUID descrUuid = descriptor.getUuid();
            if (descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                final UUID charUuid = descriptor.getCharacteristic().getUuid();
                final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
                descriptor.setValue(mCccd.getValue(device, srvcUuid, charUuid));
            }

            try {
                mCallback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                        responseNeeded, offset, value);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onDescriptorWriteRequest() - NullPointerException " + ex);
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            if (DBG) {
                Log.d(TAG, "onExecuteWrite() - device=" + device.getAddress() +
                        ", requestId=" + requestId +
                        ", execute=" + execute);
            }

            try {
                mCallback.onExecuteWrite(device, requestId, execute);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onExecuteWrite() - NullPointerException " + ex);
            }
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            if (DBG) {
                Log.d(TAG, "onNotificationSent() - device=" + device.getAddress() +
                        ", status=" + status);
            }

            try {
                mCallback.onNotificationSent(device, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onNotificationSent() - NullPointerException " + ex);
            }
        }
    };

    /**
     * Create a GATT server base object.
     *
     * @param context Context
     */
    public ServerBase(Context context) {
        mContext = context;
        loadServicesConfig();
    }

    private final List<ServiceBase> mServices = new ArrayList<>();

    void addService(ServiceBase service) {
        mServices.add(service);
    }

    /**
     * Returns a Service configuration from the list of services configuration.
     *
     * @param srvcUuid Bluetooth GATT service UUID
     * @return Service configuration if supported, or null if the requested service
     *         is not offered by the GATT profile server.
     */
    public ServiceCfg cfgService(UUID srvcUuid) {
        for (ServiceBase service : mServices) {
            if (service.getUuid().equals(srvcUuid)) {
                return service.getServiceCfg();
            }
        }
        return null;
    }

    /**
     * Returns a Characteristic configuration entry.
     *
     * @param srvcUuid Bluetooth GATT service UUID
     * @param charUuid Bluetooth GATT characteristic UUID
     * @return Characteristic configuration entry if supported, or null if the requested
     *         characteristic is not offered by the GATT profile server.
     */
    public ServiceCfg.Entry cfgCharacteristic(UUID srvcUuid, UUID charUuid) {
        if (cfgService(srvcUuid) != null) {
            return cfgService(srvcUuid).cfgCharacteristic(charUuid);
        }
        return null;
    }

    /**
     * Returns a Descriptor configuration entry.
     *
     * @param srvcUuid Bluetooth GATT service UUID
     * @param charUuid Bluetooth GATT characteristic UUID
     * @param descrUuid Bluetooth GATT descriptor UUID
     * @return Descriptor configuration entry if supported, or null if the requested
     *         descriptor is not offered by the GATT profile server.
     */
    public ServiceCfg.Entry cfgDescriptor(UUID srvcUuid, UUID charUuid, UUID descrUuid) {
        if (cfgService(srvcUuid) != null) {
            if (cfgService(srvcUuid).cfgCharacteristic(charUuid) != null) {
                return cfgService(srvcUuid).cfgDescriptor(charUuid, descrUuid);
            }
        }
        return null;
    }

    /**
     * Open a GATT Server.
     *
     * @param callback GATT server callback handler that will receive asynchronous callbacks.
     */
    public void openServer(ServerBaseCallback callback) {
        if (callback == null) {
            Log.e(TAG, "openServer() - callback is null");
            return;
        }

        mLock.lock();
        if (mGattServer != null) {
            Log.w(TAG, "openServer() - Already opened");
            return;
        }

        BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        mGattServer = bluetoothManager.openGattServer(mContext, mGattCallback);
        if (mGattServer == null) {
            Log.e(TAG, "openServer() - Open GATT server fail!");
            return;
        }

        mCallback = callback;
        mCallback.setProfileServer(this);
        mCccd = new ClientCharacteristicConfiguration();
        addGattServerService();
        mCallback.onServerReady(BluetoothGatt.GATT_SUCCESS, mGattServer);
        mLock.unlock();
    }

    /**
     * Close GATT server.
     */
    public void closeServer() {
        mLock.lock();
        if (mGattServer == null) {
            Log.w(TAG, "closeServer() - Not opened yet");
            return;
        }
        mGattServer.clearServices();
        mGattServer.close();
        mGattServer = null;
        mCallback.setProfileServer(null);
        mCallback = null;
        mCccd = null;
        mLock.unlock();
    }

    private void addGattServerService() {
        for (ServiceBase service : mServices) {
            BluetoothGattService gattSrvc = service.getService();
            if (gattSrvc != null) {
                mGattServer.addService(gattSrvc);
                cfgCccd(service.getServiceCfg());
                waitForServiceAdded();
            }
        }
    }

    private void waitForServiceAdded() {
        synchronized (mAddServiceLock) {
            try {
                Log.d(TAG, "wait()");
                mAddServiceLock.wait(ADD_SERVICE_TIMEOUT);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void cfgCccd(ServiceCfg cfg) {
        UUID srvcUuid = cfg.getUuid();
        UUID charUuid = null;
        for (ServiceCfg.Entry entry : cfg.getEntries()) {
            if (entry.getType() == ServiceCfg.TYPE_CHARACTERISTIC) {
                if (entry.getSupport()) {
                    charUuid = entry.getUuid();
                } else {
                    charUuid = null;
                }
            } else if (entry.getType() == ServiceCfg.TYPE_DESCRIPTOR) {
                if (entry.getUuid() == GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION &&
                        entry.getSupport() && charUuid != null) {
                    mCccd.add(srvcUuid, charUuid);
                }
            } else {
                charUuid = null;
            }
        }
    }

    /**
     * Returns a GATT server object.
     *
     * @return GATT server object
     */
    public BluetoothGattServer getGattServer() {
        return mGattServer;
    }

    /**
     * Returns a service with a given UUID out of the list of
     * services for this GATT server.
     *
     * @param srvcUuid Bluetooth GATT service UUID
     * @return GATT service object or null if no service with the given UUID was found.
     */
    public BluetoothGattService getGattService(UUID srvcUuid) {
        if (mGattServer != null) {
            return mGattServer.getService(srvcUuid);
        }
        return null;
    }

    /**
     * Returns a service by UUID and instance.
     *
     * @param srvcUuid Bluetooth GATT service UUID
     * @param instanceId Bluetooth GATT service instance id
     * @return GATT service object or null if no service with the given UUID was found.
     */
    public BluetoothGattService getGattService(UUID srvcUuid, int instanceId) {
        if (mGattServer != null) {
            if (mGattServer.getService(srvcUuid).getInstanceId() == instanceId) {
                return mGattServer.getService(srvcUuid);
            }
        }
        return null;
    }

    /**
     * Returns a characteristic with a given UUID out of the list of
     * characteristics for this service.
     *
     * @param srvcUuid Bluetooth GATT service UUID
     * @param charUuid Bluetooth GATT characteristic UUID
     * @return GATT characteristic object or null if no characteristic with the
     *         given UUID was found.
     */
    public BluetoothGattCharacteristic getGattCharacteristic(UUID srvcUuid, UUID charUuid) {
        if (getGattService(srvcUuid) != null) {
            return getGattService(srvcUuid).getCharacteristic(charUuid);
        }
        return null;
    }

    /**
     * Returns a descriptor with a given UUID out of the list of
     * descriptors for this characteristic.
     *
     * @param srvcUuid  Bluetooth GATT service UUID
     * @param charUuid  Bluetooth GATT characteristic UUID
     * @param descrUuid Bluetooth GATT descriptor UUID
     * @return GATT descriptor object or null if no descriptor with the given UUID was found.
     */
    public BluetoothGattDescriptor getGattDescriptor(UUID srvcUuid, UUID charUuid, UUID descrUuid) {
        if (getGattCharacteristic(srvcUuid, charUuid) != null) {
            return getGattCharacteristic(srvcUuid, charUuid).getDescriptor(descrUuid);
        }
        return null;
    }

    /**
     * Set Client-Characteristic-Configuration descriptor value.
     *
     * @param device Remote device
     * @param srvcUuid Bluetooth GATT service UUID
     * @param charUuid Bluetooth GATT characteristic UUID
     * @param value DISABLE_NOTIFICATION_VALUE or ENABLE_NOTIFICATION_VALUE or
     *              ENABLE_INDICATION_VALUE
     * @return True if Client-Characteristic-Configuration descriptor is exist.
     */
    public boolean updateCccd(BluetoothDevice device, UUID srvcUuid, UUID charUuid, byte[] value) {
        int prop = 0;

        if (getGattCharacteristic(srvcUuid, charUuid) != null) {
            prop = getGattCharacteristic(srvcUuid, charUuid).getProperties();
        } else {
            Log.e(TAG, "updateCccd() - Can't get characteristic properties! srvcUuid=" + srvcUuid +
                    ", charUuid=" + charUuid);
            return false;
        }

        if (DBG) {
            Log.d(TAG, "updateCccd() - srvcUuid=" + srvcUuid +
                    ", charUuid=" + charUuid +
                    ", charProp=0x" + String.format("%02X ", prop) +
                    ", value=" + FormatUtils.toHexString(value));
        }

        if (!Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
            if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                if ((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                    Log.w(TAG, "Characteristic don't support NOTIFY!");
                    return false;
                }
            } else if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                if ((prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
                    Log.w(TAG, "Characteristic don't support INDICATE!");
                    return false;
                }
            } else {
                return false;
            }
        }
        mCccd.setValue(device, srvcUuid, charUuid, value);
        return true;
    }

    /**
     * Get Client-Characteristic-Configuration descriptor value.
     *
     * @param device Remote device
     * @param srvcUuid Bluetooth GATT service UUID
     * @param charUuid Bluetooth GATT characteristic UUID
     * @return Client-Characteristic-Configuration descriptor value
     */
    public byte[] getCccd(BluetoothDevice device, UUID srvcUuid, UUID charUuid) {
        return mCccd.getValue(device, srvcUuid, charUuid);
    }

    /**
     * Updates the locally stored value of this characteristic.
     *
     * @param srvcUuid  Bluetooth GATT service UUID
     * @param charUuid  Bluetooth GATT characteristic UUID
     * @param value New value for this characteristic
     */
    public void setValue(UUID srvcUuid, UUID charUuid, CharacteristicBase value) {
        setValue(srvcUuid, charUuid, value.getValue());
    }

    /**
     * Updates the locally stored value of this characteristic.
     *
     * @param srvcUuid  Bluetooth GATT service UUID
     * @param charUuid  Bluetooth GATT characteristic UUID
     * @param value New value for this characteristic
     */
    public void setValue(UUID srvcUuid, UUID charUuid, byte[] value) {
        if (mGattServer.getService(srvcUuid) == null) {
            Log.e(TAG, "setValue() - Service not found! srvcUuid=" + srvcUuid);
            return;
        }
        if (mGattServer.getService(srvcUuid).getCharacteristic(charUuid) == null) {
            Log.e(TAG, "setValue() - Characteristic not found! charUuid=" + charUuid);
            return;
        }
        mGattServer.getService(srvcUuid).getCharacteristic(charUuid).setValue(value);
    }

    /**
     * Send a notification or indication that a local characteristic has been
     * updated.
     *
     * @param srvcUuid  Bluetooth GATT service UUID
     * @param charUuid  Bluetooth GATT characteristic UUID
     * @param value Value to notify or indicate
     */
    public void notify(UUID srvcUuid, UUID charUuid, CharacteristicBase value) {
        notify(srvcUuid, charUuid, value.getValue());
    }

    /**
     * Send a notification or indication that a local characteristic has been
     * updated.
     *
     * @param srvcUuid  Bluetooth GATT service UUID
     * @param charUuid  Bluetooth GATT characteristic UUID
     * @param value Value to notify or indicate
     */
    public void notify(UUID srvcUuid, UUID charUuid, byte[] value) {
        if (DBG) {
            Log.d(TAG, "notify() - srvcUuid=" + srvcUuid + ", charUuid=" + charUuid +
                    ", value=" + FormatUtils.toHexString(value));
        }
        BluetoothGattCharacteristic characteristic;

        try {
            characteristic = mGattServer.getService(srvcUuid).getCharacteristic(charUuid);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException in notify : e = " + e);
            return;
        }

        if (characteristic == null) {
            Log.w(TAG, "notify() - can't find characteristic, srvcUuid=" + srvcUuid +
                    ", charUuid=" + charUuid);
            return;
        }

        characteristic.setValue(value);

        Map<BluetoothDevice, byte[]> devices = mCccd.getDevices(srvcUuid, charUuid);
        if (devices == null) {
            Log.w(TAG, "notify() - No remote device need to notify");
            return;
        }

        for (BluetoothDevice device : devices.keySet()) {
            if (Arrays.equals(devices.get(device),
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                Log.d(TAG, "notify() - Send notification to device=" + device.getAddress());
                mGattServer.notifyCharacteristicChanged(device, characteristic, false);
            } else if (Arrays.equals(devices.get(device),
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                Log.d(TAG, "notify() - Send indication to device=" + device.getAddress());
                mGattServer.notifyCharacteristicChanged(device, characteristic, true);
            }
        }
    }

    /**
     * Send a response to a read or write request to a remote device.
     *
     * @param device The remote device to send this response to
     * @param requestId The ID of the request that was received with the callback
     * @param status The status of the request to be sent to the remote devices
     * @param offset Value offset for partial read/write response
     * @param value The value of the attribute that was read/written (optional)
     */
    public void sendResponse(BluetoothDevice device, int requestId, int status, int offset,
            byte[] value) {
        if (DBG) {
            Log.d(TAG, "sendResponse() - requestId=" + requestId +
                    ", status=" + status +
                    ", offset=" + offset +
                    ", value=" + FormatUtils.toHexString(value));
        }

        if (mGattServer == null) {
            Log.w(TAG, "GattServer is null, return");
            return;
        }
        mGattServer.sendResponse(device, requestId, status, offset, value);
    }

    /**
     * Send a error response to a read or write request to a remote device.
     *
     * @param device The remote device to send this response to
     * @param requestId The ID of the request that was received with the callback
     * @param status The error status of the request to be sent to the remote devices
     */
    public void sendErrorResponse(BluetoothDevice device, int requestId, int status) {
        if (DBG) {
            Log.d(TAG, "sendErrorResponse() - requestId=" + requestId + ", status=" + status);
        }

        if (mGattServer == null) {
            Log.w(TAG, "GattServer is null, return");
            return;
        }
        mGattServer.sendResponse(device, requestId, status, 0, null);
    }

    private final Map<BluetoothDevice, PrepareQueue> mPrepareQueue = new HashMap<>();

    /**
     * Add prepare write request into per device prepare queue.
     *
     * @param device The remote device to send this response to.
     * @param obj CharacteristicBase or BluetoothGattDescriptor object
     * @param offset Value offset
     * @param value Value to write
     * @param isDescriptor True, if obj is BluetoothGattDescriptor object
     */
    public void prepareWrite(BluetoothDevice device, Object obj, int offset, byte[] value,
            boolean isDescriptor) {
        if (DBG) {
            Log.d(TAG, "prepareWrite() - offset=" + offset +
                    ", value=" + FormatUtils.toHexString(value) +
                    ", isDescriptor=" + isDescriptor);
        }

        PrepareQueue queue;
        if (mPrepareQueue.containsKey(device)) {
            queue = mPrepareQueue.get(device);
        } else {
            queue = new PrepareQueue();
            mPrepareQueue.put(device, queue);
        }

        queue.addPrepareWrite(obj, offset, value, isDescriptor);
    }

    /**
     * Execute pending writes.
     *
     * @param device The remote device to send this response to.
     * @param execute True if execute pending writes. False if abort pending writes.
     */
    public void executeWrite(BluetoothDevice device, boolean execute) {
        if (DBG) {
            Log.d(TAG, "executeWrite() - execute=" + execute);
        }

        if (mPrepareQueue.containsKey(device)) {
            PrepareQueue queue = mPrepareQueue.get(device);
            if (execute) {
                List<PrepareQueue.Entry> entries = queue.getEntries();
                for (PrepareQueue.Entry entry : entries) {
                    if (entry.isDescriptor) {
                        byte newValue[] = ((BluetoothGattDescriptor) entry.obj).getValue();
                        System.arraycopy(
                                entry.value, 0, newValue, entry.offset, entry.value.length);
                        ((BluetoothGattDescriptor) entry.obj).setValue(newValue);
                    } else {
                        byte newValue[] = ((CharacteristicBase) entry.obj).getValue();
                        System.arraycopy(
                                entry.value, 0, newValue, entry.offset, entry.value.length);
                        ((CharacteristicBase) entry.obj).setValue(newValue);
                    }
                }
            }
            queue.clear();
            mPrepareQueue.remove(device);
        }
    }

    protected abstract void loadServicesConfig();
}
