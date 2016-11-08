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
package com.mediatek.blemanager.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;

import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleAlertNotificationProfileService;

import java.util.HashMap;
import java.util.Map;

/**
 * BluetoothEventManager receives broadcasts and callbacks from the Bluetooth
 * API and dispatches the event on the UI thread to the right class in the
 * Settings.
 */
final class BleEventManager {
    private static final String TAG = BleConstants.COMMON_TAG + "[BleEventManager]";

    private final IntentFilter mAdapterIntentFilter;
    private final Map<String, Handler> mHandlerMap;
    private Context mContext;
    private LocalBleManager mLocalBleManager;
    private CachedBleDeviceManager mCachedBleDeviceManager;

    private static final String[] ANS_CONFIGURATION_PROJECTION = {
            BleConstants.ANS_CONFIGURATION.ANS_HOST_CALL_ALERT,
            BleConstants.ANS_CONFIGURATION.ANS_HOST_MISSED_CALL_ALERT,
            BleConstants.ANS_CONFIGURATION.ANS_HOST_SMSMMS_ALERT,
            BleConstants.ANS_CONFIGURATION.ANS_HOST_EMAIL_ALERT,

            BleConstants.ANS_CONFIGURATION.ANS_REMOTE_CALL_ALERT,
            BleConstants.ANS_CONFIGURATION.ANS_REMOTE_EMAIL_ALERT,
            BleConstants.ANS_CONFIGURATION.ANS_REMOTE_MISSED_CALL_ALERT,
            BleConstants.ANS_CONFIGURATION.ANS_REMOTE_SMSMMS_ALERT
    };

    interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice device);
    }

    BleEventManager(Context context, LocalBleManager manager,
            CachedBleDeviceManager deviceManager) {
        Log.i(TAG, "[BluetoothLEEventManager]...");
        mAdapterIntentFilter = new IntentFilter();
        mHandlerMap = new HashMap<String, Handler>();
        mContext = context;
        mLocalBleManager = manager;
        mCachedBleDeviceManager = deviceManager;

        // Bluetooth on/off broadcasts
        addHandler(BluetoothAdapter.ACTION_STATE_CHANGED, new AdapterStateChangedHandler());
        // Discovery broadcasts
        addHandler(BluetoothAdapter.ACTION_DISCOVERY_STARTED, new ScanningStateChangedHandler(true));
        addHandler(BluetoothAdapter.ACTION_DISCOVERY_FINISHED, new ScanningStateChangedHandler(
                false));
        addHandler(BluetoothDevice.ACTION_NAME_CHANGED, new NameChangedHandler());

        // Pairing broadcasts
        addHandler(BluetoothDevice.ACTION_BOND_STATE_CHANGED, new BondStateChangedHandler());
        addHandler(BleAlertNotificationProfileService.ACTION_REMOTE_CHANGE,
                new AnsRemoteDataChangedHandler());
    }

    public void registerBroadcastReceiver() {
        mContext.registerReceiver(mBroadcastReceiver, mAdapterIntentFilter);
    }

    public void unregisterBroadCastReceiver() {
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
        } catch (Exception ex) {
            Log.e(TAG, "[unregisterBroadCastReceiver]ex = " + ex);
        }
    }
    
    private void addHandler(String action, Handler handler) {
        Log.d(TAG, "[addHandler]action = " + action);
        mHandlerMap.put(action, handler);
        mAdapterIntentFilter.addAction(action);
    }
    
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "[onReceive]action = " + action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            Handler handler = mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, device);
            }
        }
    };

    private class AdapterStateChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.d(TAG, "[onReceive]AdapterStateChangedHandler,state : " + state);
            mLocalBleManager.onAdapterStateChanged(state);
        }
    }

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;

        ScanningStateChangedHandler(boolean started) {
            mStarted = started;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            mLocalBleManager.onAdapterScanningStateChanged(mStarted);
            Log.d(TAG, "[onReceive]scanning state change to " + mStarted);
        }
    }

    private class NameChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            Log.d(TAG, "[NameChangedHandler]name : " + name);
            if (mCachedBleDeviceManager.findDevice(device) == null) {
                Log.d(TAG, "[NameChangedHandler] device : " + device + ", name : " + name);
                mLocalBleManager.onScannedDeviceNameChanged(device, name);
            } else {
                mCachedBleDeviceManager.onDeviceNameChanged(device, name);
            }
        }
    }

    private class BondStateChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            //TODO:
            if (device == null) {
                Log.e(TAG, "[onReceive]ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);
            Log.d(TAG, "[BondStateChangedHandler] bondState : " + bondState);
            Log.d(TAG, "[BondStateChangedHandler] device : " + device.getAddress());
            // if (bondState == BluetoothDevice.BOND_NONE) {
            // Log.d(TAG,
            // "[BondStateChangedHandler] start to disconnect device");
            // mManager.disconnectGattDevice(device);
            // }
        }
    }

    private class AnsRemoteDataChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            Log.w(TAG, "[AnsRemoteDataChangedHandler][onReceive]...");
            if (device == null) {
                Log.w(TAG, "[AnsRemoteDataChangedHandler] device is null");
                return;
            }

            CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "[AnsRemoteDataChangedHandler] bdevice is contained in cacher");
                return;
            }
            mLocalBleManager.updateAnpData(cachedDevice);
        }
    }

    private void updateAnsSettings(final BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "[updateAnsSettings] device is null");
            return;
        }
        final CachedBleDevice cachedDevice = this.mCachedBleDeviceManager.findDevice(device);
        if (cachedDevice == null) {
            Log.w(TAG, "[updateAnsSettings] cachedDevice is null");
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String selection = BleConstants.COLUMN_BT_ADDRESS + "='" + device.getAddress()
                        + "'";
                Cursor cursor = mContext.getContentResolver().query(BleConstants.TABLE_ANS_URI,
                        ANS_CONFIGURATION_PROJECTION, selection, null, null);
                if (cursor == null) {
                    Log.w(TAG, "[updateAnsSettings] cursor is null.");
                    return;
                }
                if (cursor.getCount() == 0) {
                    cursor.close();
                    Log.w(TAG, "[updateAnsSettings] cursor count is 0.");
                    return;
                }
                cursor.moveToFirst();
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_HOST_CALL_ALERT))));
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_HOST_MISSED_CALL_ALERT))));
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_HOST_SMSMMS_ALERT))));
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_HOST_EMAIL_ALERT))));

                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_REMOTE_INCOMING_CALL_FLAGE,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_REMOTE_CALL_ALERT))));
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_REMOTE_MISSED_CALL_FLAGE,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_REMOTE_MISSED_CALL_ALERT))));
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_REMOTE_NEW_EMAIL_FLAGE,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_REMOTE_EMAIL_ALERT))));
                cachedDevice
                        .setBooleanAttribute(
                                CachedBleDevice.DEVICE_REMOTE_NEW_MESSAGE_FLAGE,
                                getBoolean(cursor.getInt(cursor
                                        .getColumnIndex(BleConstants.ANS_CONFIGURATION.ANS_REMOTE_SMSMMS_ALERT))));

                cursor.close();
            }

        };
        new Thread(r).start();
    }

    private boolean getBoolean(int it) {
        if (it == 0) {
            return false;
        }
        return true;
    }
}
