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
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.FormatUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Public APIs for the GATT-based profiles (Client).
 */
public abstract class ClientBase {
    private static final boolean DBG = true;
    private static final String TAG = "ClientBase";

    private final Context mContext;
    private BluetoothGatt mGattClient;
    protected ClientBaseCallback mCallback;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mGattClient.discoverServices();
        }
    };
    private final BluetoothGattCallback mClientCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (DBG) {
                Log.d(TAG, "onConnectionStateChange() - status=" + status +
                        ", newState=" + newState);
            }

            if (status == BluetoothGatt.GATT_SUCCESS &&
                    newState == BluetoothProfile.STATE_CONNECTED) {
                mHandler.post(mRunnable);
            }

            try {
                mCallback.onConnectionStateChange(gatt, status, newState);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onConnectionStateChange() - NullPointerException " + ex);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (DBG) {
                Log.d(TAG, "onServicesDiscovered() - status=" + status);
            }

            try {
                mCallback.onServicesDiscovered(gatt, status);

                boolean isVerified = handleServicesDiscovered(gatt, status);
                if (isVerified) {
                    mCallback.onServicesVerified(BluetoothGatt.GATT_SUCCESS);
                } else {
                    mCallback.onServicesVerified(BluetoothGatt.GATT_FAILURE);
                }
            } catch (NullPointerException ex) {
                Log.e(TAG, "onServicesDiscovered() - NullPointerException " + ex);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicRead() - UUID=" + characteristic.getUuid() +
                        ", status=" + status);
            }

            try {
                mCallback.onCharacteristicRead(gatt, characteristic, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onCharacteristicRead() - NullPointerException " + ex);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicWrite() - UUID=" + characteristic.getUuid() +
                        ", status=" + status);
            }

            try {
                mCallback.onCharacteristicWrite(gatt, characteristic, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onCharacteristicWrite() - NullPointerException " + ex);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicChanged() - UUID=" + characteristic.getUuid() +
                        ", value=" + FormatUtils.toHexString(characteristic.getValue()));
            }

            try {
                mCallback.onCharacteristicChanged(gatt, characteristic);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onCharacteristicChanged() - NullPointerException " + ex);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            if (DBG) {
                Log.d(TAG, "onDescriptorRead() - UUID=" + descriptor.getUuid() +
                        ", status=" + status);
            }

            try {
                mCallback.onDescriptorRead(gatt, descriptor, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onDescriptorRead() - NullPointerException " + ex);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor, int status) {
            if (DBG) {
                Log.d(TAG, "onDescriptorWrite() - descriptor=" + descriptor.getUuid() +
                        ", status=" + status);
            }

            try {
                mCallback.onDescriptorWrite(gatt, descriptor, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onDescriptorWrite() - NullPointerException " + ex);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            if (DBG) {
                Log.d(TAG, "onReliableWriteCompleted() - status=" + status);
            }

            try {
                mCallback.onReliableWriteCompleted(gatt, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onReliableWriteCompleted() - NullPointerException " + ex);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (DBG) {
                Log.d(TAG, "onReadRemoteRssi() - rssi=" + rssi + ", status=" + status);
            }

            try {
                mCallback.onReadRemoteRssi(gatt, rssi, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onReadRemoteRssi() - NullPointerException " + ex);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (DBG) {
                Log.d(TAG, "onMtuChanged() - mtu=" + mtu + ", status=" + status);
            }

            try {
                mCallback.onMtuChanged(gatt, mtu, status);
            } catch (NullPointerException ex) {
                Log.e(TAG, "onMtuChanged() - NullPointerException " + ex);
            }
        }
    };

    /**
     * Create a GATT client base object.
     *
     * @param context Context
     */
    public ClientBase(Context context) {
        mContext = context;
    }

    /**
     * Check mandatory service is exist.
     *
     * @param gatt GATT client invoked {@link BluetoothGatt#discoverServices}
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
     *               has been explored successfully.
     * @return True, if profile mandatory service is exist.
     */
    protected abstract boolean handleServicesDiscovered(BluetoothGatt gatt, int status);

    /**
     * Connect to remote GATT server.
     *
     * @param autoConnect Whether to directly connect to the remote device (false)
     *                    or to automatically connect as soon as the remote
     *                    device becomes available (true).
     * @param device Remote GATT server
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @return BluetoothGatt
     */
    public BluetoothGatt connect(boolean autoConnect, BluetoothDevice device,
                                    ClientBaseCallback callback) {
        if (DBG) {
            Log.d(TAG, "connect() - device=" + device.getAddress() +
                    ", autoConnect=" + autoConnect);
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null");
        }

        mCallback = callback;
        mCallback.setProfileClient(this);
        mGattClient = device.connectGatt(mContext, autoConnect, mClientCallback);
       return mGattClient;
    }

    /**
     * Disconnects an established connection.
     */
    public void disconnect() {
        if (DBG) {
            Log.d(TAG, "disconnect()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "Not connected to a device");
            return;
        }
        mHandler.removeCallbacks(mRunnable);
        mGattClient.disconnect();
        mGattClient.close();
        mGattClient = null;
        mCallback.setProfileClient(null);
        mCallback = null;
    }

    /**
     * Return the remote bluetooth device this GATT client targets to.
     *
     * @return remote bluetooth device
     */
    public BluetoothDevice getDevice() {
        if (DBG) {
            Log.d(TAG, "discoverServices()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "discoverServices() - not connected to a device");
            return null;
        }

        return mGattClient.getDevice();
    }

    /**
     * Discovers services offered by a remote device as well as their
     * characteristics and descriptors.
     *
     * @return true, if the remote service discovery has been started
     */
    public boolean discoverServices() {
        if (DBG) {
            Log.d(TAG, "discoverServices()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "discoverServices() - not connected to a device");
            return false;
        }

        return mGattClient.discoverServices();
    }

    /**
     * Returns a list of GATT services offered by the remote device.
     *
     * @return List of services on the remote device. Returns an empty list
     *         if service discovery has not yet been performed.
     */
    public List<BluetoothGattService> getServices() {
        if (DBG) {
            Log.d(TAG, "getServices()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "getServices() - not connected to a device");
            return null;
        }

        return mGattClient.getServices();
    }

    /**
     * Returns a {@link BluetoothGattService}, if the requested UUID is
     * supported by the remote device.
     *
     * @param uuid UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested
     *         service is not offered by the remote device.
     */
    public BluetoothGattService getService(UUID uuid) {
        if (DBG) {
            Log.d(TAG, "getService() - UUID=" + uuid);
        }

        if (mGattClient == null) {
            Log.e(TAG, "getService() - not connected to a device");
            return null;
        }

        return mGattClient.getService(uuid);
    }

    /**
     * Returns a characteristic with a given [Service UUID, Characteristic UUID].
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @return BluetoothGattCharacteristic if supported, or null if the requested
     *         characteristic is not offered by the remote device.
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID srvcUuid, UUID charUuid) {
        BluetoothGattService service = getService(srvcUuid);

        if (service != null) {
            return service.getCharacteristic(charUuid);
        }

        return null;
    }

    /**
     * Returns a characteristic with a given [Service UUID, Characteristic UUID,
     * Descriptor UUID].
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @param descUuid Descriptor UUID
     * @return BluetoothGattDescriptor if supported, or null if the requested
     *         descriptor is not offered by the remote device.
     */
    public BluetoothGattDescriptor getDescriptor(UUID srvcUuid, UUID charUuid, UUID descUuid) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(srvcUuid, charUuid);

        if (characteristic != null) {
            return characteristic.getDescriptor(descUuid);
        }

        return null;
    }

    /**
     * Reads the requested characteristic value from the associated remote device.
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCharacteristic(UUID srvcUuid, UUID charUuid) {
        if (DBG) {
            Log.d(TAG, "readCharacteristic() - srvcUuid=" + srvcUuid + ", charUuid=" + charUuid);
        }

        if (mGattClient == null) {
            Log.e(TAG, "readCharacteristic() - not connected to a device");
            return false;
        }

        BluetoothGattService service = mGattClient.getService(srvcUuid);
        if (service == null) {
            Log.e(TAG, "readCharacteristic() - service " + srvcUuid + " is not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
        if (characteristic == null) {
            Log.e(TAG, "readCharacteristic() - characteristic " + charUuid + " is not found");
            return false;
        }

        return readCharacteristic(characteristic);
    }

    /**
     * Reads the requested characteristic value from the associated remote device.
     *
     * @param characteristic Characteristic to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (DBG) {
            Log.d(TAG, "readCharacteristic() - UUID=" + characteristic.getUuid());
        }

        if (mGattClient == null) {
            Log.e(TAG, "readCharacteristic() - not connected to a device");
            return false;
        }

        return mGattClient.readCharacteristic(characteristic);
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param srvcUuid Service UUID
     * @param charBase Value to write on the remote device
     * @return True, if the write operation was initiated successfully
     */
    public boolean writeCharacteristic(UUID srvcUuid, CharacteristicBase charBase) {
        return writeCharacteristic(srvcUuid, charBase.getUuid(), charBase.getValue());
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @param value Value to write on the remote device
     * @return True, if the write operation was initiated successfully
     */
    public boolean writeCharacteristic(UUID srvcUuid, UUID charUuid, byte[] value) {
        if (DBG) {
            Log.d(TAG, "writeCharacteristic() - srvcUuid=" + srvcUuid + ", charUuid=" + charUuid +
                    ", value=" + FormatUtils.toHexString(value));
        }

        if (mGattClient == null) {
            Log.e(TAG, "writeCharacteristic() - not connected to a device");
            return false;
        }

        BluetoothGattService service = mGattClient.getService(srvcUuid);
        if (service == null) {
            Log.e(TAG, "writeCharacteristic() - service " + srvcUuid + " is not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
        if (characteristic == null) {
            Log.e(TAG, "writeCharacteristic() - characteristic " + charUuid + " is not found");
            return false;
        }
        characteristic.setValue(value);
        return writeCharacteristic(characteristic);
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return True, if the write operation was initiated successfully
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (DBG) {
            Log.d(TAG, "writeCharacteristic() - UUID=" + characteristic.getUuid());
        }

        if (mGattClient == null) {
            Log.e(TAG, "writeCharacteristic() - not connected to a device");
            return false;
        }

        return mGattClient.writeCharacteristic(characteristic);
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @param descUuid Descriptor UUID
     * @return True, if the read operation was initiated successfully
     */
    public boolean readDescriptor(UUID srvcUuid, UUID charUuid, UUID descUuid) {
        if (DBG) {
            Log.d(TAG, "readDescriptor() - srvcUuid=" + srvcUuid + ", charUuid=" + charUuid +
                    ", descUuid=" + descUuid);
        }

        if (mGattClient == null) {
            Log.e(TAG, "readDescriptor() - not connected to a device");
            return false;
        }

        BluetoothGattService service = mGattClient.getService(srvcUuid);
        if (service == null) {
            Log.e(TAG, "readDescriptor() - service " + srvcUuid.toString() + " is not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
        if (characteristic == null) {
            Log.e(TAG, "readDescriptor() - characteristic " + charUuid + " is not found");
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descUuid);
        if (descriptor == null) {
            Log.e(TAG, "readDescriptor() - descriptor " + descUuid + " is not found");
            return false;
        }

        return readDescriptor(descriptor);
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     *
     * @param descriptor Descriptor value to read from the remote device
     * @return True, if the read operation was initiated successfully
     */
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        if (DBG) {
            Log.d(TAG, "readDescriptor() - UUID=" + descriptor.getUuid());
        }

        if (mGattClient == null) {
            Log.e(TAG, "readDescriptor() - not connected to a device");
            return false;
        }

        return mGattClient.readDescriptor(descriptor);
    }

     /**
     * Write the value of a given descriptor to the associated remote device.
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @param descUuid Descriptor UUID
     * @param value Value to write on the remote device
     * @return True, if the write operation was initiated successfully
     */
    public boolean writeDescriptor(UUID srvcUuid, UUID charUuid, UUID descUuid, byte[] value) {
        if (DBG) {
            Log.d(TAG, "writeDescriptor() - srvcUuid=" + srvcUuid + ", charUuid=" + charUuid +
                    ", descUuid=" + descUuid + ", value=" + FormatUtils.toHexString(value));
        }

        if (mGattClient == null) {
            Log.e(TAG, "writeDescriptor() - not connected to a device");
            return false;
        }

        BluetoothGattService service = mGattClient.getService(srvcUuid);
        if (service == null) {
            Log.e(TAG, "writeDescriptor() - service " + srvcUuid + " is not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
        if (characteristic == null) {
            Log.e(TAG, "writeDescriptor() - characteristic " + charUuid + " is not found");
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descUuid);
        if (descriptor == null) {
            Log.e(TAG, "writeDescriptor() - descriptor " + descUuid + " is not found");
            return false;
        }

        descriptor.setValue(value);
        return writeDescriptor(descriptor);
    }

    /**
     * Write the value of a given descriptor to the associated remote device.
     *
     * @param descriptor Descriptor to write to the associated remote device
     * @return True, if the write operation was initiated successfully
     */
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (DBG) {
            Log.d(TAG, "writeDescriptor() - UUID=" + descriptor.getUuid());
        }

        if (mGattClient == null) {
            Log.e(TAG, "writeDescriptor() - not connected to a device");
            return false;
        }

        if (mGattClient.writeDescriptor(descriptor)) {
            UUID descrUuid = descriptor.getUuid();
            byte[] value = descriptor.getValue();
            if (descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

                if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) ||
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    if (DBG) {
                        Log.d(TAG, "writeDescriptor() - setCharacteristicNotification(true)");
                    }
                    mGattClient.setCharacteristicNotification(characteristic, true);
                } else {
                    if (DBG) {
                        Log.d(TAG, "writeDescriptor() - setCharacteristicNotification(false)");
                    }
                    mGattClient.setCharacteristicNotification(characteristic, false);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Initiates a reliable write transaction for a given remote device.
     *
     * @return true, if the reliable write transaction has been initiated
     */
    public boolean beginReliableWrite() {
        if (DBG) {
            Log.d(TAG, "beginReliableWrite()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "beginReliableWrite() - not connected to a device");
            return false;
        }

        return mGattClient.beginReliableWrite();
    }

    /**
     * Executes a reliable write transaction for a given remote device.
     *
     * @return true, if the reliable write transaction has been initiated
     */
    public boolean executeReliableWrite() {
        if (DBG) {
            Log.d(TAG, "executeReliableWrite()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "executeReliableWrite() - not connected to a device");
            return false;
        }

        return mGattClient.executeReliableWrite();
    }

    /**
     * Cancels a reliable write transaction for a given device.
     */
    public void abortReliableWrite() {
        if (DBG) {
            Log.d(TAG, "abortReliableWrite()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "abortReliableWrite() - not connected to a device");
            return;
        }

        mGattClient.abortReliableWrite();
    }

    /**
     * Read the RSSI for a connected remote device.
     *
     * @return true, if the RSSI value has been requested successfully
     */
    public boolean readRemoteRssi() {
        if (DBG) {
            Log.d(TAG, "readRemoteRssi()");
        }

        if (mGattClient == null) {
            Log.e(TAG, "readRemoteRssi() - not connected to a device");
            return false;
        }

        return mGattClient.readRemoteRssi();
    }

    /**
     * Request an MTU size used for a given connection.
     *
     * @param mtu Request MTU
     * @return true, if the new MTU value has been requested successfully
     */
    public boolean requestMtu(int mtu) {
        if (DBG) {
            Log.d(TAG, "requestMtu() - mtu=" + mtu);
        }

        if (mGattClient == null) {
            Log.e(TAG, "requestMtu() - not connected to a device");
            return false;
        }

        return mGattClient.requestMtu(mtu);
    }

    /**
     * Returns BluetoothGatt object.
     *
     * @return BluetoothGatt
     */
    public BluetoothGatt getBluetoothGatt() {
        return mGattClient;
    }

    /**
     * Enable notification of a given characteristic to the associated remote device.
     *
     * @param srvcUuid Service UUID
     * @param charUuid Characteristic UUID
     * @param enable Enable/Disable notification
     * @return true, if the enable notification has been requested successfully
     */
    public boolean enableNotify(UUID srvcUuid, UUID charUuid, boolean enable) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(srvcUuid, charUuid);

        if (characteristic == null) {
            Log.w(TAG, "enableNotify() - Can't find characteristic! srvcUuid=" + srvcUuid +
                    ", charUuid=" + charUuid);
            return false;
        }

        int prop = characteristic.getProperties();

        if (((prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) &&
                ((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)) {
            Log.w(TAG, "enableNotify() - Characteristic don't support notification/indication. " +
                    "srvcUuid=" + srvcUuid + ", charUuid=" + charUuid + ", prop=" + prop);
            return false;
        }

        if (DBG) {
            Log.d(TAG, "enableNotify() - srvcUuid=" + srvcUuid +
                    ", charUuid=" + charUuid +
                    ", prop=" + prop +
                    ", enable=" + enable);
        }

        byte enableValue[];

        if (enable) {
            if ((prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                enableValue = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
            } else {
                enableValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            }
        } else {
            enableValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        }

        return writeDescriptor(srvcUuid, charUuid,
                GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION, enableValue);
    }
}
