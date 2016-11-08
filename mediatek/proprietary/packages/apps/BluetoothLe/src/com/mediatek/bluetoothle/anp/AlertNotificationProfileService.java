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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.bluetoothle.anp;

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
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import com.mediatek.bluetooth.BleAlertNotificationProfileService;
import com.mediatek.bluetoothle.IBleProfileServer;
import com.mediatek.bluetoothle.anp.data.GattAnsAttributes;
import com.mediatek.bluetoothle.anp.detector.CalllogAlertDetector;
import com.mediatek.bluetoothle.anp.detector.EmailAlertDetector;
import com.mediatek.bluetoothle.anp.detector.IncommingCallAlertDetector;
import com.mediatek.bluetoothle.anp.detector.MessageAlertDetector;
import com.mediatek.bluetoothle.bleservice.BleSingleProfileServerService;
import com.mediatek.bluetoothle.ext.BLEExtentionManager;
import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;
import com.mediatek.bluetoothle.ext.IBluetoothLeAnsExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

public class AlertNotificationProfileService extends BleSingleProfileServerService {
    private static final String TAG = "[BLE][BluetoothAns]ProfileService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private HashMap<Integer, List<Pair<Object, byte[]>>> mHashReliableData;

    private NotificationController mNotificationController;
    private AlertNotifier mAlertNotifier;
    private ControlCommandChecker mCommandChecker;
    private BluetoothAnsCategoryManager mDetectorManager;

    private IBleProfileServer mBluetoothGattServer = null;

    private BluetoothGattServerCallback mCallback = new BluetoothGattServerCallback() {

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (DBG) {
                Log.v(TAG, "onServiceAdded - status:" + status + " service:" + service);
                Log.v(TAG, "onServiceAdded - service  uuid=" + service.getUuid());
            }

            if (BluetoothGatt.GATT_SUCCESS == status) {
                // init characteristics
                BluetoothGattCharacteristic character;
                BluetoothGattDescriptor descriptor;

                byte[] twoBytesZero = {
                        (byte) 0x00, (byte) 0x00
                };
                byte[] oneByteZero = {
                        (byte) 0x00
                };
                byte[] supportedCategoryBytes = {
                        (byte) 0x00, (byte) 0x00
                };
                if (mNotificationController != null) {
                    supportedCategoryBytes = mNotificationController.getSupportedCategory();
                } else {
                    Log.e(TAG, "mNotificationController is null in onServiceAdded");
                }
                try {
                    character = service.getCharacteristic(GattAnsAttributes.NEW_ALERT_UUID);
                    character.setValue(twoBytesZero);
                    descriptor = character
                            .getDescriptor(GattAnsAttributes.CLIENT_CHARACTERISTIC_CONFIG_DES_UUID);
                    descriptor.setValue(oneByteZero);

                    character = service
                            .getCharacteristic(GattAnsAttributes.SUPPORTED_NEW_ALERT_CATEGORY_UUID);
                    character.setValue(supportedCategoryBytes);

                    character = service.getCharacteristic(
                            GattAnsAttributes.SUPPORTED_UNREAD_ALERT_CATEGORY_UUID);
                    character.setValue(supportedCategoryBytes);

                    character = service.getCharacteristic(GattAnsAttributes.UNREAD_ALERT_STATUS);
                    character.setValue(twoBytesZero);
                    descriptor = character
                            .getDescriptor(GattAnsAttributes.CLIENT_CHARACTERISTIC_CONFIG_DES_UUID);
                    descriptor.setValue(oneByteZero);

                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }

                mBluetoothGattServer = AlertNotificationProfileService.this.getBleProfileServer();
                if (mBluetoothGattServer == null) {
                    Log.e(TAG, "mBluetoothGattServer = null");
                }

                if (mAlertNotifier != null) {
                    mAlertNotifier.setGattServer(mBluetoothGattServer);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

            if (VDBG) {
                Log.v(TAG, "onConnectionStateChange - device:" + device + " " + "status:" + status
                        + " " + "newState:" + newState);
            }

            if (BluetoothGatt.GATT_SUCCESS == status) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mNotificationController.initDeviceSetting(device);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mNotificationController.removeDeviceSetting(device);
                } else {
                    if (VDBG) {
                        Log.v(TAG, "onConnectionStateChange - ignore state:" + newState);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattCharacteristic characteristic) {

            byte[] data = characteristic.getValue();
            if (VDBG) {
                Log.v(TAG, "onCharacteristicReadRequest - incoming request: " + device.getName());
                Log.v(TAG, "onCharacteristicReadRequest -        requestId: " + requestId);
                Log.v(TAG, "onCharacteristicReadRequest -           offset: " + offset);
                Log.v(TAG, "onCharacteristicReadRequest -             uuid: "
                        + characteristic.getUuid().toString());
                Log.v(TAG, "onCharacteristicReadRequest -             data: " + data[0] + " ,"
                        + data[1]);
            }

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, Arrays.copyOfRange(data, offset, data.length));
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattDescriptor descriptor) {

            String address = device.getAddress();
            int result = 0;
            if (GattAnsAttributes.NEW_ALERT_UUID.equals(descriptor.getCharacteristic().getUuid())) {
                result = mNotificationController.getClientAlertConfig(address,
                        NotificationController.CATEGORY_ENABLED_NEW);
            } else if (GattAnsAttributes.UNREAD_ALERT_STATUS.equals(descriptor.getCharacteristic()
                    .getUuid())) {
                result = mNotificationController.getClientAlertConfig(address,
                        NotificationController.CATEGORY_ENABLED_UNREAD);
            }
            byte[] data = {
                    (byte) (result & 0xFF), (byte) ((result >> 8) & 0xFF)
            };
            if (VDBG) {
                Log.v(TAG, "onDescriptorReadRequest - incoming request: " + device.getName());
                Log.v(TAG, "onDescriptorReadRequest -        requestId: " + requestId);
                Log.v(TAG, "onDescriptorReadRequest -           offset: " + offset);
                Log.v(TAG, "onDescriptorReadRequest -             uuid: "
                        + descriptor.getUuid().toString());
            }

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, Arrays.copyOfRange(data, offset, data.length));
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                boolean responseNeeded, int offset, byte[] value) {
            if (VDBG) {
                Log.v(TAG, "onCharacteristicWriteRequest - offset:" + offset + " value.length:"
                        + value.length + " preparedWrite:" + preparedWrite + " responseNeeded:"
                        + responseNeeded);
            }
            if (!characteristic.getUuid().equals(
                    GattAnsAttributes.ALERT_NOTIFICATION_CONTROL_POINT_UUID)) {
                if (VDBG) {
                    Log.v(TAG, "onCharacteristicWriteRequest ANS return , for unexpected uuid "
                            + characteristic.getUuid());
                }
                return;
            }

            byte[] oldValue = characteristic.getValue();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < value.length; i++) {
                builder.append(value[i] + ",");
            }
            if (VDBG) {
                Log.v(TAG, "value " + builder.toString());
            }

            if (preparedWrite) {
                if (VDBG) {
                    Log.v(TAG, "onCharacteristicWriteRequest - a prepare write (pending data)\n"
                            + "onCharacteristicWriteRequest - requestId=" + requestId + "\n");
                }

                List<Pair<Object, byte[]>> listPendingData = null;
                listPendingData = mHashReliableData.get(requestId);
                if (null == listPendingData) {
                    if (VDBG) {
                        Log.v(TAG, "onCharacteristicWriteRequest - a new listPendingData");
                    }
                    listPendingData = new ArrayList<Pair<Object, byte[]>>();
                    mHashReliableData.put(requestId, listPendingData);
                }
                listPendingData.add(new Pair<Object, byte[]>(characteristic, value));
            } else {
                if (VDBG) {
                    Log.v(TAG, "onCharacteristicWriteRequest - a normal write\n");
                }
                characteristic.setValue(value);
            }
            if (VDBG) {
                Log.v(TAG, "onCharacteristicWriteRequest ANS preparedWrite ok");
            }

            boolean result = true;
            if (!preparedWrite) {
                result = mCommandChecker.newControlCommand(value, device);
            }
            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId,
                        result ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE, offset,
                        value);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                int offset, byte[] value) {
            if (VDBG) {
                Log.v(TAG, "onDescriptorWriteRequest - offset:" + offset + " value.length:"
                        + value.length + " preparedWrite:" + preparedWrite + " responseNeeded:"
                        + responseNeeded);
            }
            UUID characteristicUuid = descriptor.getCharacteristic().getUuid();
            UUID descriptorUuid = descriptor.getUuid();
            if (VDBG) {
                Log.v(TAG, "onDescriptorWriteRequest - descriptorUuid " + descriptorUuid
                        + " characteristicUuid " + characteristicUuid);
            }

            if ((!descriptorUuid.equals(GattAnsAttributes.CLIENT_CHARACTERISTIC_CONFIG_DES_UUID))
                    || ((!characteristicUuid.equals(GattAnsAttributes.UNREAD_ALERT_STATUS))
                            && (!characteristicUuid.equals(GattAnsAttributes.NEW_ALERT_UUID)))) {
                if (VDBG) {
                    Log.v(TAG, "onDescriptorWriteRequest - ANS unexpect UUID:");
                }
                return;
            }

            byte[] newValue = value;

            if (preparedWrite) {
                if (VDBG) {
                    Log.v(TAG, "onDescriptorWriteRequest - a prepare write (pending data)\n"
                            + "onDescriptorWriteRequest - requestId=" + requestId + "\n");
                }

                List<Pair<Object, byte[]>> listPendingData = null;
                listPendingData = mHashReliableData.get(requestId);
                if (null == listPendingData) {
                    if (VDBG) {
                        Log.v(TAG, "onDescriptorWriteRequest - a new listPendingData");
                    }
                    listPendingData = new ArrayList<Pair<Object, byte[]>>();
                    mHashReliableData.put(requestId, listPendingData);
                }
                listPendingData.add(new Pair<Object, byte[]>(descriptor, newValue));
            } else {
                if (VDBG) {
                    Log.v(TAG, "onDescriptorWriteRequest - a normal write\n");
                }
                descriptor.setValue(newValue);
            }

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset, value);
            }
            int finalValue = 0;
            for (int i = 0; i < newValue.length; i++) {
                finalValue |= ((int) newValue[i]) << (8 * i);
            }
            if (VDBG) {
                Log.v(TAG, "onDescriptorWriteRequest - broadcastUpdateData() newValue "
                        + finalValue);
            }
            if (!preparedWrite) {
                writeDescription(device, descriptor, newValue);
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            if (VDBG) {
                Log.v(TAG, "onExecuteWrite - device:" + device + " requestId:" + requestId
                        + " execute:" + execute);
            }

            ArrayList<byte[]> commandData = new ArrayList<byte[]>();
            if (execute) {
                if (VDBG) {
                    Log.v(TAG, "onExecuteWrite - execute write");
                }

                List<Pair<Object, byte[]>> listPendingData = null;
                listPendingData = mHashReliableData.get(requestId);

                if (listPendingData != null) {
                    for (Pair<Object, byte[]> pair : listPendingData) {
                        if (pair.first instanceof BluetoothGattCharacteristic) {
                            BluetoothGattCharacteristic characteristic =
                                    (BluetoothGattCharacteristic) pair.first;
                            characteristic.setValue(pair.second);
                            commandData.add(pair.second);
                            if (VDBG) {
                                Log.v(TAG, "onExecuteWrite - characteristic:" + characteristic);
                            }
                        } else if (pair.first instanceof BluetoothGattDescriptor) {
                            BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) pair
                                    .first;
                            descriptor.setValue(pair.second);
                            writeDescription(device, descriptor, pair.second);
                            if (VDBG) {
                                Log.v(TAG, "onExecuteWrite - descriptor:" + descriptor);
                            }
                        } else {
                            if (VDBG) {
                                Log.v(TAG, "onExecuteWrite - unexpect object:" + pair.first);
                            }
                        }
                    }
                }
            } else {
                if (VDBG) {
                    Log.v(TAG, "onExecuteWrite - execute cancel");
                }
                mHashReliableData.remove(requestId);
            }
            boolean result = true;
            for (byte[] command : commandData) {
                if (!mCommandChecker.newControlCommand(command, device)) {
                    result = false;
                }
            }
            mBluetoothGattServer.sendResponse(device, requestId,
                    result ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE, 0, null);
        }

    };

    @Override
    public void onDestroy() {
        if (mDetectorManager != null) {
            mDetectorManager.removeAllDetectors();
        }
        if (mNotificationController != null) {
            mNotificationController.clearAll();
        }
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        //super.onCreate();
        if (VDBG) {
            Log.v(TAG, "-- onCreate");
        }
        initialize();
        super.onCreate();
    }

    private boolean initialize() {
        BluetoothManager bluetoothManager = null;
        BluetoothAdapter bluetoothAdapter = null;
        IBluetoothLeAnsExtension ansExtention = (IBluetoothLeAnsExtension) BLEExtentionManager
                .getExtentionObject(BLEExtentionManager.BLE_ANS_EXTENTION,
                        AlertNotificationProfileService.this);

        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (mAlertNotifier == null) {
            mAlertNotifier = new AlertNotifier();
        }

        ArrayList<BluetoothAnsDetector> detectorList = createAllDetector();
        TreeSet<Byte> defaultCategoryIdSet = getDefaultCategoryId(detectorList);
        ArrayList<BluetoothAnsDetector> extraDetectorList = null;
        TreeSet<Byte> extraCategoryIdSet = null;
        if (ansExtention != null) {
            extraDetectorList = ansExtention.getDetectorArray(AlertNotificationProfileService.this);
            extraCategoryIdSet = ansExtention.getExtraCategoryId();
        }

        if (mNotificationController == null) {
            mNotificationController = new NotificationController(
                    AlertNotificationProfileService.this, bluetoothAdapter);
            mNotificationController.addSupportedCategory(defaultCategoryIdSet);
            mNotificationController.addSupportedCategory(extraCategoryIdSet);
            mNotificationController.initSupportedCategory();
        }
        if (mHashReliableData == null) {
            mHashReliableData = new HashMap<Integer, List<Pair<Object, byte[]>>>();
        }

        if (mDetectorManager == null) {
            if (VDBG) {
                Log.v(TAG, "mDetectorManager initialized");
            }
            mDetectorManager = new BluetoothAnsCategoryManager(mNotificationController,
                    mAlertNotifier);
        }

        mDetectorManager.addDetectors(detectorList);
        mDetectorManager.addDetectors(extraDetectorList);

        if (mCommandChecker == null) {
            mCommandChecker = new ControlCommandChecker(mNotificationController, mDetectorManager);
        }

        return true;
    }

    @Override
    protected int getProfileId() {
        return IBleProfileServer.ANP;
    }

    @Override
    protected BluetoothGattServerCallback getDefaultBleProfileServerHandler() {
        return mCallback;
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new AlertNotificationServiceBinder(mNotificationController);
    }

    private void writeDescription(BluetoothDevice device, BluetoothGattDescriptor descriptor,
            byte[] newValue) {
        UUID characUuid = descriptor.getCharacteristic().getUuid();
        int type = 0;
        int finalValue = 0;
        for (int i = 0; i < newValue.length; i++) {
            finalValue |= (int) newValue[i] << (8 * i);
        }
        if (GattAnsAttributes.NEW_ALERT_UUID.equals(characUuid)) {
            type = NotificationController.CATEGORY_ENABLED_NEW;
        } else if (GattAnsAttributes.UNREAD_ALERT_STATUS.equals(characUuid)) {
            type = NotificationController.CATEGORY_ENABLED_UNREAD;
        } else {
            Log.e(TAG, "writeDescription() error, the characUuid is not correct" + characUuid);
            return;
        }
        if (DBG) {
            Log.d(TAG, "writeDescription()-" + device.getAddress() + ", type:" + type + "value:"
                    + finalValue);
        }
        mNotificationController.setClientAlertConfig(device.getAddress(), type, finalValue);
    }

    private ArrayList<BluetoothAnsDetector> createAllDetector() {
        ArrayList<BluetoothAnsDetector> detectorList = new ArrayList<BluetoothAnsDetector>();
        detectorList.add(new CalllogAlertDetector(this));
        detectorList.add(new EmailAlertDetector(this));
        detectorList.add(new IncommingCallAlertDetector(this));
        detectorList.add(new MessageAlertDetector(this));
        return detectorList;
    }

    private TreeSet<Byte> getDefaultCategoryId(ArrayList<BluetoothAnsDetector> detectorList) {
        TreeSet<Byte> defaultSupportedIdSet = new TreeSet<Byte>();
        for (BluetoothAnsDetector detector : detectorList) {
            defaultSupportedIdSet.add(detector.getDetectorCategory());
        }
        return defaultSupportedIdSet;
    }

    private class AlertNotificationServiceBinder extends IAlertNotificationProfileService.Stub
            implements IProfileServiceBinder {

        private NotificationController mController = null;

        AlertNotificationServiceBinder(NotificationController controller) {
            mController = controller;
        }

        @Override
        public int[] getDeviceSettings(String address, int[] categoryArray) {
            if (categoryArray != null) {
                ArrayList<Integer> categorylist = new ArrayList<Integer>();
                SparseIntArray result = null;
                for (int category : categoryArray) {
                    categorylist.add(category);
                }
                if (mController != null) {
                    result = mController.getDeviceSetting(address, categorylist, false);
                } else {
                    Log.e(TAG, "getDeviceSettings (mController == null) ");
                    return null;
                }
                if (result != null) {
                    int[] resultArray = new int[categoryArray.length];
                    int i = 0;
                    for (int categoryId : categoryArray) {
                        resultArray[i] = result.get(categoryId,
                                BleAlertNotificationProfileService.CATEGORY_ERROR_VALUE);
                        i++;
                    }
                    return resultArray;
                } else {
                    Log.e(TAG, "getDeviceSettings, result is null");
                    return null;
                }
            } else {
                Log.e(TAG, "getDeviceSettings, categoryArray == null");
                return null;
            }
        }

        @Override
        public int[] getRemoteSettings(String address, int[] categoryArray) {
            if (categoryArray != null) {
                ArrayList<Integer> categorylist = new ArrayList<Integer>();
                SparseIntArray result = null;
                for (int category : categoryArray) {
                    categorylist.add(category);
                }
                if (mController != null) {
                    result = mController.getDeviceSetting(address, categorylist, true);
                } else {
                    Log.e(TAG, "getRemoteSettings (mController == null) ");
                    return null;
                }
                if (result != null) {
                    int[] resultArray = new int[categoryArray.length];
                    int i = 0;
                    for (int categoryId : categoryArray) {
                        resultArray[i] = result.get(categoryId,
                                BleAlertNotificationProfileService.CATEGORY_ERROR_VALUE);
                        i++;
                    }
                    return resultArray;
                } else {
                    Log.e(TAG, "getRemoteSettings, result is null");
                    return null;
                }
            } else {
                Log.e(TAG, "getRemoteSettings, categoryArray == null");
                return null;
            }
        }

        @Override
        public boolean updateDeviceSettings(String address, int[] categoryArray, int[] valueArray) {
            if (mController != null) {
                SparseIntArray categoryIdValues = new SparseIntArray();
                if (categoryArray != null && valueArray != null
                        && categoryArray.length == valueArray.length) {
                    for (int i = 0; i < categoryArray.length; i++) {
                        categoryIdValues.put(categoryArray[i], valueArray[i]);
                    }
                    return mController.updateHostSetting(address, categoryIdValues);
                } else {
                    Log.e(TAG, "updateDeviceSettings, categoryArray==null?"
                            + (categoryArray != null) + "valueArray==null?" + (valueArray != null));
                }
            } else {
                Log.e(TAG, "updateDeviceSettings (mController == null) ");
            }
            return false;

        }

        @Override
        public boolean cleanup() {
            return false;
        }
    }

}
