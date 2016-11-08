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

package com.mediatek.bluetoothle.tip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.bluetooth.BleGattUuid;
import com.mediatek.bluetoothle.IBleProfileServer;
import com.mediatek.bluetoothle.bleservice.BleSingleProfileServerService;
import com.mediatek.bluetoothle.provider.TipDbOperator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Provides a service which implements TIme Profile.
 * This class is a subclass of Android Service, and communicates with GATT interface.
 */
public class TipServerService extends BleSingleProfileServerService {
    private static final String TAG = "TipServerService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private TipService mCurrentTimeService;
    private TipService mNDSTCService;
    private TipService mRTUService;

    static final String CASIO_GB6900AB = "CASIO GB-6900A*";
    private static final String[] IOT_DEVICE_LIST = new String[] {
            CASIO_GB6900AB
    };

    private final Map<String, IotHandler> mIotHandlers = new HashMap<String, IotHandler>();
    private final Set<BluetoothDevice> mNotifiedDevices = new HashSet<BluetoothDevice>();
    private final BluetoothGattServerCallback mCallback = new BluetoothGattServerCallback() {
        @Override
        public void onCharacteristicReadRequest(final BluetoothDevice device,
                final int requestId, final int offset,
                final BluetoothGattCharacteristic characteristic) {
            final TipService service = pickService(characteristic);
            if (null == service) {
                Log.e(TAG, "service is null");
                return;
            }
            service.onReadCharacteristic(characteristic, device);

            // Handle IOT
            final String name = device.getName();
            final IotHandler iotHandler = mIotHandlers.get(name);
            if (null == iotHandler) {
                if (DBG) Log.d(TAG, "No need to handle IOT");
            } else {
                if (DBG) Log.d(TAG, "Handle IOT for device = " + device);
                // iotHandler.handleCharRead(characteristic);
            }

            final byte[] data = characteristic.getValue();
            //
            for (int i = 0; i < data.length; i++) {
                if (VDBG) Log.v(TAG, "data[" + i + "] = " + data[i]);
            }
            //
            final IBleProfileServer bleProfileServer = TipServerService.this.getBleProfileServer();

            if (DBG) {
                Log.d(TAG, "onCharacteristicReadRequest - incoming request: " + device.getName());
                Log.d(TAG, "onCharacteristicReadRequest -        requestId: " + requestId);
                Log.d(TAG, "onCharacteristicReadRequest -           offset: " + offset);
                Log.d(TAG, "onCharacteristicReadRequest -             uuid: "
                        + characteristic.getUuid().toString());
            }

            bleProfileServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    Arrays.copyOfRange(data, offset, data.length));
        }

        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device,
                final int requestId, final BluetoothGattCharacteristic characteristic,
                final boolean preparedWrite, final boolean responseNeeded, final int offset,
                final byte[] value) {
            byte[] newValue = null;
            final byte[] oldValue = characteristic.getValue();
            final IBleProfileServer bleProfileServer = TipServerService.this.getBleProfileServer();

            if (DBG) Log.d(TAG, "onCharacteristicWriteRequest - offset:" + offset + " "
                    + "value.length:" + value.length + " "
                    + "preparedWrite:" + preparedWrite + " "
                    + "responseNeeded:" + responseNeeded);

            if (null != oldValue
                    && oldValue.length >= offset + value.length) {
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

            if (DBG) Log.d(TAG, "onCharacteristicWriteRequest- preparedWrite:" + preparedWrite);
            if (preparedWrite) {
                // / do nothing
                if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest - preparedWrite write\n");
            } else {
                if (VDBG) Log.v(TAG, "onCharacteristicWriteRequest - a normal write\n");
                characteristic.setValue(newValue);
            }

            final TipService service = pickService(characteristic);
            if (null == service) {
                Log.e(TAG, "service is null");
                return;
            }
            service.onWriteCharateristic(characteristic, device, value);

            if (DBG) Log.d(TAG, "onCharacteristicWriteRequest- responseNeeded:" + responseNeeded);
            if (responseNeeded) {
                bleProfileServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value);
            }

        }

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status,
                final int newState) {
            if (DBG) Log.d(TAG, "onConnectionStateChange- device:" + device
                    + " status:" + status
                    + " newState:" + newState);
            final String name = device.getName();
            final IotHandler iotHandler = mIotHandlers.get(name);
            if (null == iotHandler) {
                if (DBG) Log.d(TAG, "No need to handle IOT");
            } else {
                if (DBG) Log.d(TAG, "Handle IOT for device = " + device);
                iotHandler.handleConnStateChange(device, status, newState);
                return;
            }
        }

        @Override
        public void onDescriptorReadRequest(final BluetoothDevice device,
                final int requestId, final int offset, final BluetoothGattDescriptor descriptor) {

            final TipService service = pickService(descriptor);
            if (null == service) {
                Log.e(TAG, "service is null");
                return;
            }
            service.onReadDescriptor(descriptor, device);
            if (mNotifiedDevices.contains(device)) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            final byte[] data = descriptor.getValue();
            //
            for (int i = 0; i < data.length; i++) {
                if (VDBG) Log.v(TAG, "data[" + i + "] = " + data[i]);
            }
            //
            final IBleProfileServer bleProfileServer = TipServerService.this.getBleProfileServer();
            if (DBG) {
                Log.d(TAG, "onDescriptorReadRequest - incoming request: " + device.getName());
                Log.d(TAG, "onDescriptorReadRequest -        requestId: " + requestId);
                Log.d(TAG, "onDescriptorReadRequest -           offset: " + offset);
                Log.d(TAG, "onDescriptorReadRequest -             uuid: "
                        + descriptor.getUuid().toString());
            }
            bleProfileServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    Arrays.copyOfRange(data, offset, data.length));

        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device,
                final int requestId, final BluetoothGattDescriptor descriptor,
                final boolean preparedWrite, final boolean responseNeeded, final int offset,
                final byte[] value) {

            byte[] newValue = null;
            final byte[] oldValue = descriptor.getValue();
            final IBleProfileServer bleProfileServer = TipServerService.this.getBleProfileServer();

            if (DBG) Log.d(TAG, "onDescriptorWriteRequest - offset:" + offset + " "
                    + "value.length:" + value.length + " "
                    + "preparedWrite:" + preparedWrite + " "
                    + "responseNeeded:" + responseNeeded);

            if (null != oldValue
                    && oldValue.length >= offset + value.length) {
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

            if (DBG) Log.d(TAG, "onDescriptorWriteRequest- preparedWrite:" + preparedWrite);
            if (preparedWrite) {
                // / do nothing
                if (VDBG) Log.v(TAG, "onDescriptorWriteRequest - preparedWrite write\n");
            } else {
                if (VDBG) Log.v(TAG, "onDescriptorWriteRequest - a normal write\n");
                descriptor.setValue(newValue);
            }

            final TipService service = pickService(descriptor);
            if (null == service) {
                Log.e(TAG, "service is null");
                return;
            }
            service.onWriteDescriptor(descriptor, device, value);

            for (int i = 0; i < newValue.length; i++) {
                if (VDBG) Log.v(TAG, "newValue[" + i + "] = " + newValue[i]);
            }

            if (responseNeeded) {
                bleProfileServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value);
            }
        }

        @Override
        public void onExecuteWrite(final BluetoothDevice device, final int requestId,
                final boolean execute) {
            if (DBG) Log.d(TAG, "onExecuteWrite- device:" + device + " requestId:" + requestId
                    + " execute:" + execute);
        }

        @Override
        public void onServiceAdded(final int status, final BluetoothGattService service) {
            if (DBG) Log.d(TAG, "onServiceAdded- status:" + status + " service:" + service);
        }
    };

    @Override
    public final void onCreate() {
        super.onCreate();
        init();
        if (VDBG) Log.v(TAG, "onCreate: TipServerService");
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        if (VDBG) Log.v(TAG, "onDestroy: TipServerService");
        ((CurrentTimeService) mCurrentTimeService).uninit();
        ((ReferenceTimeUpdateService) mRTUService).uninit();
    }

    @Override
    protected final int getProfileId() {
        return IBleProfileServer.TIP;
    }

    @Override
    protected final BluetoothGattServerCallback getDefaultBleProfileServerHandler() {
        return mCallback;
    }

    @Override
    protected final IProfileServiceBinder initBinder() {
        if (VDBG) Log.v(TAG, "initBinder");
        return new TipServerServiceBinder(this);
    }

    static int getTypeLen(final int formatType) {
        return formatType & 0xF;
    }

    final boolean init() {
        if (VDBG) Log.v(TAG, "init");
        mCurrentTimeService = new CurrentTimeService(this);
        mNDSTCService = new NextDSTChangeService();
        mRTUService = new ReferenceTimeUpdateService(this);
        initNotifiedDevices();
        initIotHandler();
        return true;
    }

    final void onNotifyChanged(final BluetoothDevice device, final boolean notify) {
        if (DBG) Log.d(TAG, "onNotifyChanged: device = " + device + ", notify = " + notify);
        if (notify) {
            addNotifiedDevices(device);
        } else {
            removeNotifiedDevices(device);
        }
    }

    final void onTimeUpdated(final long time) {
        if (DBG) Log.d(TAG, "onTimeUpdated: time = " + time);
        ((CurrentTimeService) mCurrentTimeService).onTimeUpdated(time);
    }

    final BluetoothGattCharacteristic getCharacteristic(final UUID charUuid) {
        if (DBG) Log.d(TAG, "getCharacteristic: uuid = " + charUuid);
        final IBleProfileServer bleProfileServer = TipServerService.this.getBleProfileServer();
        final BluetoothGattService service = bleProfileServer
                .getService(BleGattUuid.Service.CURRENT_TIME);
        return service.getCharacteristic(charUuid);
    }

    final void notifyCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (VDBG) Log.v(TAG, "notifyCharacteristic");

        final IBleProfileServer bleProfileServer = TipServerService.this.getBleProfileServer();
        final Iterator<BluetoothDevice> it = mNotifiedDevices.iterator();
        final BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        while (it.hasNext()) {
            final BluetoothDevice device = it.next();
            if (BluetoothProfile.STATE_CONNECTED == bm.getConnectionState(device,
                    BluetoothProfile.GATT_SERVER)) {
                if (DBG) Log.d(TAG, "Notify device: " + device);
                // Handle IOT
                final String name = device.getName();
                final IotHandler iotHandler = mIotHandlers.get(name);
                if (null == iotHandler) {
                    if (DBG) Log.d(TAG, "No need to handle IOT");
                } else {
                    if (DBG) Log.d(TAG, "Handle IOT for device = " + device);
                    // iotHandler.handleNotify(device, characteristic, false);
                    // continue;
                }
                bleProfileServer.notifyCharacteristicChanged(device, characteristic, false);
            }
        }
    }

    private IotHandler makeIotHandler(final String name) {
        if (DBG) Log.d(TAG, "makeIotHandler: name = " + name);
        IotHandler handler = null;
        if (CASIO_GB6900AB == name) {
            handler = new Casio6900abHandler(this);
        } else {
            if (VDBG) Log.v(TAG, "No IOT Handler");
        }
        return handler;
    }

    private void initIotHandler() {
        if (VDBG) Log.v(TAG, "initIotHandler");
        for (int i = 0; i < IOT_DEVICE_LIST.length; i++) {
            final String name = IOT_DEVICE_LIST[i];
            if (DBG) Log.d(TAG, "name = " + name);
            mIotHandlers.put(name, makeIotHandler(name));
        }
    }

    private TipService pickService(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGattService service = characteristic.getService();
        final UUID serviceUuid = service.getUuid();
        if (DBG) Log.d(TAG, "pickService: serviceUuid = " + serviceUuid);
        return selectService(serviceUuid);
    }

    private TipService pickService(final BluetoothGattDescriptor descriptor) {
        if (VDBG) Log.v(TAG, "pickService");
        final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        final UUID serviceUuid = characteristic.getService().getUuid();
        return selectService(serviceUuid);
    }

    private TipService selectService(final UUID serviceUuid) {
        if (DBG) Log.d(TAG, "selectService: serviceUuid = " + serviceUuid);
        if (serviceUuid.equals(BleGattUuid.Service.CURRENT_TIME)) {
            return mCurrentTimeService;
        } else if (serviceUuid.equals(BleGattUuid.Service.NEXT_DST_CHANGE)) {
            return mNDSTCService;
        } else if (serviceUuid.equals(BleGattUuid.Service.REFERENCE_TIME_UPDATE)) {
            return mRTUService;
        } else {
            Log.e(TAG, "Unsupported Service: serviceUuid = " + serviceUuid);
            return null;
        }
    }

    private void initNotifiedDevices() {
        if (VDBG) Log.v(TAG, "initNotifiedDevices");
        // Init mNotifiedDevices from database
        final Set<String> addresses = TipDbOperator.getNotifyDeviceAddresses(this, true);
        if (null == addresses) {
            if (DBG) Log.d(TAG, "Addresses got from database is null");
            return;
        }
        final Iterator<String> it = addresses.iterator();
        while (it.hasNext()) {
            final String addr = it.next();
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            final BluetoothDevice device = adapter.getRemoteDevice(addr);
            if (DBG) Log.d(TAG, "Add device: " + device);
            mNotifiedDevices.add(device);
        }
    }

    private void addNotifiedDevices(final BluetoothDevice device) {
        if (DBG) Log.d(TAG, "addNotifiedDevices: device = " + device);
        mNotifiedDevices.add(device);
        // Add notified device to database
        TipDbOperator.insertData(this, device.getAddress(), true);
    }

    private void removeNotifiedDevices(final BluetoothDevice device) {
        if (DBG) Log.d(TAG, "removeNotifiedDevices: device = " + device);
        mNotifiedDevices.remove(device);
        // Remove notified device to database
        TipDbOperator.deleteData(this, device.getAddress());
    }

    private class TipServerServiceBinder extends ITimeProfileService.Stub implements
            IProfileServiceBinder {

        TipServerServiceBinder(final TipServerService tipService) {
            if (DBG) Log.d(TAG, "TipServerServiceBinder: tipService = " + tipService);
        }

        @Override
        public void notifyTime(final long time) throws RemoteException {
            ((CurrentTimeService) mCurrentTimeService).notifyTime(time);
        }

        @Override
        public boolean cleanup() {
            return false;
        }
    }
}
