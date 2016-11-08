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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.mediatek.bluetooth.BleAlertNotificationProfileService;
import com.mediatek.bluetoothle.provider.BLEConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class NotificationController {
    private static final String TAG = "[BluetoothAns]NotificationController";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    public static final byte CATEGORY_ID_SIMPLE = 0;
    public static final byte CATEGORY_ID_EMAIL = 1;
    public static final byte CATEGORY_ID_NEWS = 2;
    public static final byte CATEGORY_ID_INCOMING_CALL = 3;
    public static final byte CATEGORY_ID_MISSED_CALL = 4;
    public static final byte CATEGORY_ID_SMS = 5;
    public static final byte CATEGORY_ID_VOICE_MAIL = 6;
    public static final byte CATEGORY_ID_SCHEDULE = 7;
    public static final byte CATEGORY_ID_HIGH_PRIORITIZED = 8;
    public static final byte CATEGORY_ID_INSTANT_MESSAGE = 9;

    public static final byte CATEGORY_ID_ALL_CATEGORY = (byte) 0xFF;

    public static final int CATEGORY_ENABLED =
            BleAlertNotificationProfileService.CATEGORY_VALUE_ALL_ALERT_ENABLED;
    public static final int CATEGORY_DISABLED =
            BleAlertNotificationProfileService.CATEGORY_VAULE_ALL_ALERT_DISABLED;

    public static final int CATEGORY_ENABLED_NEW =
            BleAlertNotificationProfileService.CATEGORY_VALUE_NEW_ALERT_ENABLED;
    public static final int CATEGORY_ENABLED_UNREAD =
            BleAlertNotificationProfileService.CATEGORY_VALUE_UNREAD_ALERT_ENABLED;

    private static final int CONFIG_DEFAULT_VALUE = 0x0000;

    private static final String HOST_DB_COLUMN[] = {
            BLEConstants.ANS.ANS_HOST_SIMPLE_ALERT,
            BLEConstants.ANS.ANS_HOST_EMAIL_ALERT,
            BLEConstants.ANS.ANS_HOST_NEWS_ALERT,
            BLEConstants.ANS.ANS_HOST_CALL_ALERT,
            BLEConstants.ANS.ANS_HOST_MISSED_CALL_ALERT,
            BLEConstants.ANS.ANS_HOST_SMSMMS_ALERT,
            BLEConstants.ANS.ANS_HOST_VOICE_MAIL_ALERT,
            BLEConstants.ANS.ANS_HOST_SCHEDULE_ALERT,
            BLEConstants.ANS.ANS_HOST_HIGH_PRIORITIZED_ALERT,
            BLEConstants.ANS.ANS_HOST_INSTANT_MESSAGE_ALERT,
    };

    private static final String REMOTE_DB_COLUMN[] = {
            BLEConstants.ANS.ANS_REMOTE_SIMPLE_ALERT,
            BLEConstants.ANS.ANS_REMOTE_EMAIL_ALERT,
            BLEConstants.ANS.ANS_REMOTE_NEWS_ALERT,
            BLEConstants.ANS.ANS_REMOTE_CALL_ALERT,
            BLEConstants.ANS.ANS_REMOTE_MISSED_CALL_ALERT,
            BLEConstants.ANS.ANS_REMOTE_SMSMMS_ALERT,
            BLEConstants.ANS.ANS_REMOTE_VOICE_MAIL_ALERT,
            BLEConstants.ANS.ANS_REMOTE_SECHEDULE_ALERT,
            BLEConstants.ANS.ANS_REMOTE_HIGH_PRIORITIZED_ALERT,
            BLEConstants.ANS.ANS_REMOTE_INSTANT_MESSAGE_ALERT,
    };

    private Context mContext;
    private BluetoothAdapter mAdapter;
    private HashMap<String, DeviceSettingRegister> mDeviceRegisterMap;
    private TreeSet<Byte> mCategoryIdSet = new TreeSet<Byte>();
    private byte[] mSupportedCategory = null;
    private static final Uri ANS_URI = Uri.parse(BLEConstants.HEADER + BLEConstants.AUTORITY + "/"
            + BLEConstants.ANS.TABLE_ANS);
    private static final String ANS_ADDRESS_SELECTION = BLEConstants.COLUMN_BT_ADDRESS + "='";
    private static final String ANS_ADDRESS_SELECTION_END = "'";

    public NotificationController(Context context, BluetoothAdapter bluetoothAdapter) {
        mContext = context;
        mAdapter = bluetoothAdapter;
        if (mDeviceRegisterMap == null) {
            mDeviceRegisterMap = new HashMap<String, DeviceSettingRegister>();
        }
    }

    public void initDeviceSetting(BluetoothDevice device) {
        String deviceAddress = device.getAddress();
        if (!mDeviceRegisterMap.containsKey(deviceAddress)) {
            if (DBG) {
                Log.d(TAG, "addDevice:" + device);
            }
            DeviceSettingRegister settingRegister = new DeviceSettingRegister();
            mDeviceRegisterMap.put(deviceAddress, settingRegister);
            if (initHostSettingFromDataBase(deviceAddress)) {
                initNewDefaultSetingInDatabase(deviceAddress);
            }
            notifyRemoteStatusChanged(deviceAddress);
        }
    }

    public void addSupportedCategory(Set<Byte> supportedIdSet) {
        if (supportedIdSet != null) {
            mCategoryIdSet.addAll(supportedIdSet);
        }
    }

    public void initSupportedCategory() {
        int minGroupSize = 2;
        if (mCategoryIdSet != null) {
            long categoryValue = 0;
            for (byte id : mCategoryIdSet) {
                categoryValue += (long) 1 << id;
            }
            ArrayList<Byte> list = new ArrayList<Byte>();
            while (categoryValue > 0) {
                list.add((byte) (categoryValue & 0xFF));
                categoryValue = categoryValue >> Byte.SIZE;
            }
            int groupSize = list.size();
            byte[] result;
            if (groupSize <= minGroupSize) {
                result = new byte[] {
                        0x00, 0x00
                };

            } else {
                result = new byte[groupSize];
            }
            for (int i = 0; i < groupSize; i++) {
                result[i] = list.get(i);
            }
            mSupportedCategory = result;
        } else {
            mSupportedCategory = null;
        }
    }

    public byte[] getSupportedCategory() {
        if (mSupportedCategory != null) {
            return mSupportedCategory;
        } else {
            Log.e(TAG, "mSupportedCategory is null!!!");
            return new byte[] {
                    0x00, 0x00
            };
        }
    }

    public void clearAll() {
        mDeviceRegisterMap.clear();
    }

    public void removeDeviceSetting(BluetoothDevice device) {
        String deviceAddress = device.getAddress();
        if (mDeviceRegisterMap != null) {
            mDeviceRegisterMap.remove(deviceAddress);
        }
    }

    public void deleteDevice(String address) {
        if (mDeviceRegisterMap != null && mDeviceRegisterMap.containsKey(address)) {
            mDeviceRegisterMap.remove(address);
        }
        mContext.getContentResolver().delete(ANS_URI,
                ANS_ADDRESS_SELECTION + address + ANS_ADDRESS_SELECTION_END, null);
    }

    public boolean updateHostSetting(String address, SparseIntArray categoryIdValues) {
        if (mCategoryIdSet != null && categoryIdValues != null) {
            int size = categoryIdValues.size();
            ContentValues values = new ContentValues();
            DeviceSettingRegister register = mDeviceRegisterMap.get(address);
            if (register == null) {
                Log.e(TAG, "updateHostSetting, register is null");
                return false;
            }
            for (int i = 0; i < size; i++) {
                byte categoryId = (byte) categoryIdValues.keyAt(i);
                int value = categoryIdValues.valueAt(i);
                if (mCategoryIdSet.contains(categoryId)) {
                    register.setHostCategoryStatus(categoryId, value);
                    if (categoryId <= CATEGORY_ID_INSTANT_MESSAGE) {
                        values.put(HOST_DB_COLUMN[CATEGORY_ID_INSTANT_MESSAGE], value);
                    } else {
                        values.put(BLEConstants.ANS.ANS_HOST_EXTRA_CATEGORY + categoryId, value);
                    }
                } else {
                    Log.e(TAG, "updateHostSetting, not supported category" + categoryId);
                }
            }
            if (values.size() > 0) {
                mContext.getContentResolver().update(ANS_URI, values,
                        ANS_ADDRESS_SELECTION + address + ANS_ADDRESS_SELECTION_END, null);
                return true;
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "updateHostSetting, address or categoryIdValues is null: " + address);
            return false;
        }
    }

    public SparseIntArray getDeviceSetting(String address, ArrayList<Integer> categorylist,
            boolean remoteSetting) {
        if (address != null && categorylist != null) {
            DeviceSettingRegister register = mDeviceRegisterMap.get(address);
            if (register == null) {
                Log.e(TAG, "getDeviceSetting, register is null");
                return null;
            }
            SparseIntArray resultArray = new SparseIntArray();
            for (int categoryId : categorylist) {
                if (mCategoryIdSet.contains((byte) categoryId)) {
                    if (remoteSetting) {
                        resultArray.put(categoryId,
                                register.getRemoteCategoryStatus((byte) categoryId));
                    } else {
                        resultArray.put(categoryId,
                                register.getHostCategoryStatus((byte) categoryId));
                    }
                } else {
                    Log.e(TAG, "getDeviceSetting, not supported category" + categoryId);
                }
            }
            return resultArray;
        } else {
            Log.e(TAG, "getDeviceSetting, address or categorylist is null: " + address);
            return null;
        }
    }

    private boolean initHostSettingFromDataBase(String address) {
        if (DBG) {
            Log.d(TAG, "initHostSettingFromDataBase, " + address);
        }
        DeviceSettingRegister register = null;
        boolean isNewDevice = true;
        if (mDeviceRegisterMap != null) {
            register = mDeviceRegisterMap.get(address);
        }
        if (register == null) {
            return isNewDevice;
        }
        Cursor cursor = mContext.getContentResolver().query(ANS_URI, null,
                ANS_ADDRESS_SELECTION + address + ANS_ADDRESS_SELECTION_END, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    if (DBG) {
                        Log.d(TAG, "initHostSettingFromDataBase, have a cursor");
                    }
                    register.updateHostSetting(cursor);
                    isNewDevice = false;
                }
            } finally {
                cursor.close();
            }
        }
        return isNewDevice;
    }

    private void initNewDefaultSetingInDatabase(String address) {
        if (DBG) {
            Log.d(TAG, "initNewDefaultSetingInDatabase, " + address);
        }
        ContentValues values = new ContentValues();
        for (byte categoryId : mCategoryIdSet) {
            values.put(BLEConstants.COLUMN_BT_ADDRESS, address);
            if (categoryId <= CATEGORY_ID_INSTANT_MESSAGE) {
                values.put(HOST_DB_COLUMN[categoryId], CATEGORY_ENABLED);
                values.put(REMOTE_DB_COLUMN[categoryId], CATEGORY_DISABLED);
            } else {
                values.put(BLEConstants.ANS.ANS_HOST_EXTRA_CATEGORY + categoryId, CATEGORY_ENABLED);
                values.put(BLEConstants.ANS.ANS_REMOTE_EXTRA_CATEGORY + categoryId,
                        CATEGORY_DISABLED);
            }
        }
        Uri uri = mContext.getContentResolver().insert(ANS_URI, values);
        if (DBG) {
            Log.d(TAG, "initNewDefaultSetingInDatabase, uri = " + uri);
        }
    }

    private void setAlertStatus(String address, int type, byte categoryId, boolean enabled) {
        int oldStatus = getCurrentCategoryRemoteStatus(address, categoryId);
        int newStatus = 0;
        if (enabled) {
            newStatus = oldStatus | type;
        } else {
            newStatus = oldStatus & ~type;
        }
        setCategoryRemoteInRegister(address, categoryId, newStatus);
    }

    public void setAlertEnabled(String address, int type, byte categoryId, boolean enabled) {
        if (categoryId == CATEGORY_ID_ALL_CATEGORY) {
            for (byte id : mCategoryIdSet) {
                setAlertStatus(address, type, id, enabled);
            }
        } else if (mCategoryIdSet.contains(categoryId)) {
            setAlertStatus(address, type, categoryId, enabled);
        } else {
            Log.w(TAG, "setAlertEnabled undefined categoryId = " + categoryId);
        }
        setRemoteSettingInDatabase(address);
        notifyRemoteStatusChanged(address);
    }

    public int getCurrentCategoryRemoteStatus(String address, byte categoryId) {
        int status = 0;
        DeviceSettingRegister register = mDeviceRegisterMap.get(address);
        if (register != null) {
            status = register.getRemoteCategoryStatus(categoryId);
        }
        return status;

    }

    public void setClientAlertConfig(String address, int type, int value) {
        DeviceSettingRegister register = mDeviceRegisterMap.get(address);
        if (register != null) {
            if (DBG) {
                Log.d(TAG, "setClientAlertConfig()-" + address + ", type:" + type + "value:"
                        + value);
            }
            register.setClientConfigStatus(type, value);
        } else {
            if (DBG) {
                Log.e(TAG, "No register for this device, error");
            }
        }
    }

    public int getClientAlertConfig(String address, int type) {
        DeviceSettingRegister register = mDeviceRegisterMap.get(address);
        if (register != null) {
            return register.getClientConfigStatus(type);
        } else {
            return CONFIG_DEFAULT_VALUE;
        }
    }

    public ArrayList<BluetoothDevice> getNofiyableDevices(String deviceAddress, byte categoryId,
            int type) {
        ArrayList<BluetoothDevice> devicesList = new ArrayList<BluetoothDevice>();
        if (deviceAddress == null) {
            Iterator<Entry<String, DeviceSettingRegister>> interator = mDeviceRegisterMap
                    .entrySet().iterator();
            Entry<String, DeviceSettingRegister> entry;
            DeviceSettingRegister register;
            while (interator.hasNext()) {
                entry = interator.next();
                register = entry.getValue();
                if (isNotifyEnabled(categoryId, register, type)) {
                    BluetoothDevice activeDevice = mAdapter.getRemoteDevice(entry.getKey());
                    if (activeDevice != null) {
                        if (DBG) {
                            Log.d(TAG,
                                "getNofiyableDevices() get a active device from adapter"
                                        + entry.getKey());
                        }
                        devicesList.add(activeDevice);
                    } else {
                        Log.w(TAG,
                                "getNofiyableDevices() get a null device from adapter"
                                        + entry.getKey());
                    }
                }
            }
        } else {
            DeviceSettingRegister register = mDeviceRegisterMap.get(deviceAddress);
            if (register != null) {
                if (isNotifyEnabled(categoryId, register, type)) {
                    BluetoothDevice activeDevice = mAdapter.getRemoteDevice(deviceAddress);
                    devicesList.add(activeDevice);
                }
            }
        }
        if (!devicesList.isEmpty()) {
            return devicesList;
        } else {
            return null;
        }
    }

    private boolean isNotifyEnabled(byte categoryId, DeviceSettingRegister register, int type) {
        int hostStatus = register.getHostCategoryStatus(categoryId);
        int remoteStatus = register.getRemoteCategoryStatus(categoryId);
        int clientConfig = register.getClientConfigStatus(type);
        if (DBG) {
            Log.d(TAG, "isNotifyEnabled(), clientConfig:" + clientConfig + ", hostStatus:"
                    + hostStatus + ", remoteStatus:" + remoteStatus + ", type:" + type);
        }
        return (clientConfig != 0 && (hostStatus & type) != 0 && (remoteStatus & type) != 0);
    }

    private void setCategoryRemoteInRegister(String address, byte categoryId, int status) {
        if (DBG) {
            Log.d(TAG, "setCategoryRemoteInRegister(), address:" + address + ", categoryId:"
                    + categoryId + ", status:" + status);
        }
        DeviceSettingRegister register = mDeviceRegisterMap.get(address);
        if (register != null) {
            register.setRemoteCategoryStatus(categoryId, status);
        }
    }

    private void setRemoteSettingInDatabase(String address) {
        DeviceSettingRegister register = mDeviceRegisterMap.get(address);
        if (register != null) {
            ContentValues values = new ContentValues();
            for (byte categoryId : mCategoryIdSet) {
                if (categoryId <= CATEGORY_ID_INSTANT_MESSAGE) {
                    values.put(REMOTE_DB_COLUMN[categoryId],
                            register.getRemoteCategoryStatus(categoryId));
                } else {
                    values.put(BLEConstants.ANS.ANS_REMOTE_EXTRA_CATEGORY + categoryId,
                            register.getRemoteCategoryStatus(categoryId));
                }
            }
            mContext.getContentResolver().update(ANS_URI, values,
                    ANS_ADDRESS_SELECTION + address + ANS_ADDRESS_SELECTION_END, null);
        }
    }

    private void notifyRemoteStatusChanged(String address) {
        Intent intent = new Intent(BleAlertNotificationProfileService.ACTION_REMOTE_CHANGE);
        BluetoothDevice device = mAdapter.getRemoteDevice(address);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        mContext.sendBroadcast(intent);
    }

    private class DeviceSettingRegister {
        private SparseArray<Integer> mCurrentHostSettingArray = new SparseArray<Integer>();
        private SparseArray<Integer> mCurrentRemoteSettingArray = new SparseArray<Integer>();
        private int mNewAlertClientConfig = 0x0001;
        private int mUnreadAlertClientConfig = 0x0001;

        // initialize settings
        public DeviceSettingRegister() {
            for (byte i : mCategoryIdSet) {
                mCurrentHostSettingArray.append(i, CATEGORY_ENABLED);
                mCurrentRemoteSettingArray.append(i, CATEGORY_DISABLED);
            }
        }

        public int getRemoteCategoryStatus(byte categoryId) {
            Integer status = mCurrentRemoteSettingArray.get(categoryId);
            if (status != null) {
                return status;
            } else {
                Log.e(TAG, "getRemoteCategoryStatus: status = null");
                return 0;
            }
        }

        public void setRemoteCategoryStatus(byte categoryId, int status) {
            if (mCategoryIdSet.contains(categoryId)) {
                if (DBG) {
                    Log.d(TAG, "setRemoteCategoryStatus: " + categoryId + " : " + status);
                }
                mCurrentRemoteSettingArray.put(categoryId, status);
            }
        }

        public void setClientConfigStatus(int type, int value) {
            switch (type) {
                case CATEGORY_ENABLED_NEW:
                    mNewAlertClientConfig = value;
                    break;
                case CATEGORY_ENABLED_UNREAD:
                    mUnreadAlertClientConfig = value;
                    break;
                default:
                    break;
            }
        }

        public int getClientConfigStatus(int type) {
            switch (type) {
                case CATEGORY_ENABLED_NEW:
                    return mNewAlertClientConfig;
                case CATEGORY_ENABLED_UNREAD:
                    return mUnreadAlertClientConfig;
                default:
                    return CONFIG_DEFAULT_VALUE;
            }
        }

        public void setHostCategoryStatus(byte categoryId, int status) {
            mCurrentHostSettingArray.put(categoryId, status);
        }

        public int getHostCategoryStatus(byte categoryId) {
            Integer status = mCurrentHostSettingArray.get(categoryId);
            if (status != null) {
                return status;
            } else {
                return 0;
            }
        }

        public void updateHostSetting(Cursor cursor) {
            if (DBG) {
                Log.d(TAG, "updateHostSetting()");
            }
            int hostStartColumn = cursor.getColumnIndexOrThrow(HOST_DB_COLUMN[0]);
            for (byte categoryId : mCategoryIdSet) {
                int newCategoryStartColumn = hostStartColumn + CATEGORY_ID_INSTANT_MESSAGE + 1;
                if (categoryId <= CATEGORY_ID_INSTANT_MESSAGE) {
                    int hostStatus = cursor.getInt(categoryId + hostStartColumn);
                    mCurrentHostSettingArray.put(categoryId, hostStatus);
                } else {
                    int hostStatus = cursor.getInt(newCategoryStartColumn);
                    mCurrentHostSettingArray.put(categoryId, hostStatus);
                }
            }
        }
    }
}
