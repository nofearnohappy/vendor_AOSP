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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseIntArray;

import com.mediatek.blemanager.provider.BleConstants;
import com.mediatek.bluetooth.BleAlertNotificationProfileService;
import com.mediatek.bluetooth.BleDeviceManager;
import com.mediatek.bluetooth.BleFindMeProfile;
import com.mediatek.bluetooth.BleGattDevice;
import com.mediatek.bluetooth.BleGattUuid;
import com.mediatek.bluetooth.BleManager;
import com.mediatek.bluetooth.BleProfile;
import com.mediatek.bluetooth.BleProfileService;
import com.mediatek.bluetooth.BleProfileServiceManager;
import com.mediatek.bluetooth.BleProximityProfileService;
//import com.mediatek.bluetoothle.bleservice.BleDeviceManagerService;
import com.mediatek.bluetoothle.pxp.IProximityProfileServiceCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalBleManager {
    private static final String TAG = BleConstants.COMMON_TAG + "[LocalBleManager]";

    private static final int CATEGORY_ID_EMAIL = 1;
    private static final int CATEGORY_ID_INCOMING_CALL = 3;
    private static final int CATEGORY_ID_MISSED_CALL = 4;
    private static final int CATEGORY_ID_SMS = 5;

    public static final int PROFILE_ANP_ID = 100;
    public static final int PROFILE_PXP_ID = 200;
    public static final int PROFILE_DEVICE_MANAGER_SERVICE_ID = 300;

    public static final int PROFILE_CONNECTED = 10;
    public static final int PROFILE_DISCONNECTED = 11;

    private static final ArrayList<Integer> CATEGORY_IDS = new ArrayList<Integer>();

    private static LocalBleManager sInstance;
    private LocalBluetoothAdapter mLocalBluetoothAdapter;
    private BleEventManager mBleEventManager;
    private CachedBleDeviceManager mCachedBleDeviceManager;
    private ConcurrentHashMap<BluetoothDevice, Integer> mConnectionHashMap;
    private Context mContext;

    private BleManager mBleManager;

    private BleDeviceManager mBleDeviceManager;
    private BleProximityProfileService mBlePxpService;
    private BleAlertNotificationProfileService mBleAnpService;

    private BleProfileServiceManager mBleProfileServiceManager;

    private CachedBleGattDevice mCachedBleGattDevice;
    private ProximityProfileServiceCallback mProximityProfileServiceCallback;

    private CopyOnWriteArrayList<BluetoothCallback.BluetoothAdapterState> mAdapterStateCallbacksList;
    private CopyOnWriteArrayList<BluetoothCallback.BleDeviceScanned> mScannedCallbacksList;
    private CopyOnWriteArrayList<BluetoothDevice> mConnectingDevicesList;

    private ArrayList<DeviceNameChangeListener> mScannedDeviceNameChangeListenersList;
    private ArrayList<DeviceConnectionChangeListener> mDeviceConnectionChangeListenersList;;
    private ArrayList<ServiceConnectionListener> mServiceConnectionListeners;

    private ConcurrentHashMap<BluetoothDevice, ProximityProfileServiceCallback> mPxpCallbackHashMap;

    private boolean mIsFwkInited = false;
    private boolean mIsFwkClosed = false;

    private boolean mIsDeviceManagerServiceConnected = false;
    private boolean mIsPxpServiceConnected = false;
    private boolean mIsAnpServiceConnected = false;

    /**
     * constructor
     * 
     * used to initialize the local parameters.
     * 
     * @param context
     */
    private LocalBleManager(Context context) {
        Log.i(TAG, "[LocalBluetoothLEManager]new...");
        mContext = context;
        mLocalBluetoothAdapter = LocalBluetoothAdapter.getInstance();

        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        mCachedBleDeviceManager.registerDeviceListChangedListener(mCachedDeviceListChangedListener);

        mBleEventManager = new BleEventManager(context, this, mCachedBleDeviceManager);

        mAdapterStateCallbacksList = new CopyOnWriteArrayList<BluetoothCallback.BluetoothAdapterState>();
        mScannedCallbacksList = new CopyOnWriteArrayList<BluetoothCallback.BleDeviceScanned>();
        mScannedDeviceNameChangeListenersList = new ArrayList<DeviceNameChangeListener>();
        mDeviceConnectionChangeListenersList = new ArrayList<DeviceConnectionChangeListener>();

        mServiceConnectionListeners = new ArrayList<ServiceConnectionListener>();

        mConnectionHashMap = new ConcurrentHashMap<BluetoothDevice, Integer>();

        mConnectingDevicesList = new CopyOnWriteArrayList<BluetoothDevice>();

        mProximityProfileServiceCallback = new ProximityProfileServiceCallback();

        mPxpCallbackHashMap = new ConcurrentHashMap<BluetoothDevice, ProximityProfileServiceCallback>();

        CATEGORY_IDS.add(CATEGORY_ID_EMAIL);
        CATEGORY_IDS.add(CATEGORY_ID_INCOMING_CALL);
        CATEGORY_IDS.add(CATEGORY_ID_MISSED_CALL);
        CATEGORY_IDS.add(CATEGORY_ID_SMS);

    }

    /**
     * sigletone instance, get instance from this method.
     * 
     * @param context
     * @return
     */
    public static synchronized LocalBleManager getInstance(Context context) {
        if (context == null) {
            Log.w(TAG, "[getInstance]context is null!!");
            return null;
        }
        if (sInstance == null) {
            LocalBluetoothAdapter adapter = LocalBluetoothAdapter.getInstance();
            if (adapter == null) {
                Log.w(TAG, "[getInstance]no bluetooth supported!!");
                return null;
            }
            sInstance = new LocalBleManager(context);
        }
        return sInstance;
    }

    public void intialize() {
        Log.i(TAG, "[intialize]...");
        Intent intent = new Intent(mContext, TaskDetectService.class);
        mContext.startService(intent);

        mBleEventManager.registerBroadcastReceiver();

        mBleManager = BleManager.getDefaultBleProfileManager();
        // get ble profile service manager which can launch service and shut
        // down services
        mBleManager.getProfileServiceManager(mContext, mServiceManagerListener);

        if (mLocalBluetoothAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
            Log.i(TAG, "[intialize] BT is on, call to init FWK");
            initFwk();
        }
    }

    /**
     *
     */
    public void close() {
        Log.i(TAG, "[close]mIsFwkClosed = " + mIsFwkClosed);
        ArrayList<CachedBleDevice> cacheDevices = mCachedBleDeviceManager.getCachedDevicesCopy();
        if (cacheDevices.size() > 0 && mCachedBleGattDevice != null) {
            Log.d(TAG, "[close] unregister the FWK gatt callback");
            for (CachedBleDevice device : cacheDevices) {
                BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(
                        mBleDeviceManager, device.getDevice(), mGattDeviceCallback);
                if (gattDevice != null) {
                    gattDevice.close();
                }
            }
        }
        
        if (!mIsFwkClosed) {
            mIsFwkInited = false;
            mIsFwkClosed = true;
            if (mBleProfileServiceManager != null) {
                Log.i(TAG, "[close] do shutdownService action");
                mBleProfileServiceManager.shutdownServices();
            }
            if (mBleManager != null) {
                if (mBlePxpService != null) {
                    Log.i(TAG, "[close]close mBlePxpService");
                    mBleManager.closeProfileServiceProxy(BleProfile.PXP, mBlePxpService);
                    mBlePxpService = null;
                    mIsPxpServiceConnected = false;
                    notifyServiceConnectionChanged(PROFILE_PXP_ID, PROFILE_DISCONNECTED);
                }
                if (mBleAnpService != null) {
                    Log.i(TAG, "[close]close mBleAnpService");
                    mBleManager.closeProfileServiceProxy(BleProfile.ANP, mBleAnpService);
                    mBleAnpService = null;
                    mIsAnpServiceConnected = false;
                    notifyServiceConnectionChanged(PROFILE_ANP_ID, PROFILE_DISCONNECTED);
                }
                if (mBleDeviceManager != null) {
                    Log.i(TAG, "[close]close mBleDeviceManager");
                    mBleManager.closeDeviceManager(mBleDeviceManager);
                    mBleDeviceManager = null;
                    mIsDeviceManagerServiceConnected = false;
                    notifyServiceConnectionChanged(PROFILE_DEVICE_MANAGER_SERVICE_ID,
                            PROFILE_DISCONNECTED);
                }
                if (mBleProfileServiceManager != null) {
                    Log.i(TAG, "[close]close mBleProfileServiceManager");
                    mBleManager.closeProfileServiceManager(mBleProfileServiceManager);
                    mBleProfileServiceManager = null;
                }
            }
        }
        if (mConnectionHashMap != null) {
            mConnectionHashMap.clear();
        }
        if (this.mPxpCallbackHashMap != null) {
            mPxpCallbackHashMap.clear();
        }
        if (mAdapterStateCallbacksList != null) {
            mAdapterStateCallbacksList.clear();
        }
        if (mScannedCallbacksList != null) {
            mScannedCallbacksList.clear();
        }
        if (mScannedDeviceNameChangeListenersList != null) {
            mScannedDeviceNameChangeListenersList.clear();
        }
        if (mCachedBleGattDevice != null) {
            mCachedBleGattDevice.clearData();
            mCachedBleGattDevice = null;
        }
        if (mDeviceConnectionChangeListenersList != null) {
            mDeviceConnectionChangeListenersList.clear();
        }
        if (mServiceConnectionListeners != null) {
            mServiceConnectionListeners.clear();
        }
        // sInstance = null;
        mBleEventManager.unregisterBroadCastReceiver();
        Intent intent = new Intent(mContext, TaskDetectService.class);
        mContext.stopService(intent);
        mIsFwkInited = false;
    }

    private void initFwk() {
        Log.d(TAG, "[initFwk]mIsFwkInited = " + mIsFwkInited);
        if (!mIsFwkInited) {
            Log.d(TAG, "[initFwk] mIsFwkInited is false, do init action");
            // mBleManager = BleManager.getDefaultBleProfileManager();
            // //get ble profile service manager which can launch service and
            // shut down services
            // mBleManager.getProfileServiceManager(mContext,
            // mServiceManagerListener);

            // get ble device manager, which will get device manager
            // in mDeviceManagerListener.onServiceConnected method
            mBleManager.getDeviceManager(mContext, mDeviceManagerListener);
            // get BleProximityProfileService, which will get it from
            // mProfileServiceListener.onServiceConnected method
            mBleManager.getProfileServiceProxy(mContext, BleProfile.PXP, mProfileServiceListener);
            mBleManager.getProfileServiceProxy(mContext, BleProfile.ANP, mProfileServiceListener);
            mIsFwkInited = true;
            mIsFwkClosed = false;
        }
    }

    public boolean getServiceConnectionState(int profileId) {
        if (profileId == PROFILE_DEVICE_MANAGER_SERVICE_ID) {
            return this.mIsDeviceManagerServiceConnected;
        } else if (profileId == this.PROFILE_PXP_ID) {
            return this.mIsPxpServiceConnected;
        } else if (profileId == this.PROFILE_ANP_ID) {
            return this.mIsAnpServiceConnected;
        }
        return false;
    }

    /**
     * 
     * @param callback
     */
    public void registerBluetoothAdapterStateCallback(
            BluetoothCallback.BluetoothAdapterState callback) {
        if (callback == null) {
            Log.w(TAG, "[registerBluetoothAdapterStateCallback] callback is null");
            return;
        }
        if (mAdapterStateCallbacksList.contains(callback)) {
            Log.d(TAG, "[registerBluetoothAdapterStateCallback]"
                    + " callback has been contained in list");
            return;
        }
        Log.i(TAG, "[registerBluetoothAdapterStateCallback]add.");
        mAdapterStateCallbacksList.add(callback);
    }

    /**
     * 
     * @param callback
     */
    public void registerBleDeviceScannedStateCallback(BluetoothCallback.BleDeviceScanned callback) {
        if (callback == null) {
            Log.w(TAG, "[registerBleDeviceScannedStateCallback] callback is null");
            return;
        }
        if (mScannedCallbacksList.contains(callback)) {
            Log.d(TAG, "[registerBleDeviceScannedStateCallback]"
                    + " callback has been contained in list");
            return;
        }
        Log.i(TAG, "[registerBleDeviceScannedStateCallback]add.");
        mScannedCallbacksList.add(callback);
    }

    /**
     * 
     * @param callback
     */
    public void unregisterAdaterStateCallback(BluetoothCallback.BluetoothAdapterState callback) {
        if (callback == null) {
            Log.w(TAG, "[unregisterAdaterStateCallback] callback is null");
            return;
        }
        if (!mAdapterStateCallbacksList.contains(callback)) {
            Log.w(TAG, "[unregisterAdaterStateCallback] callback not contained in list");
            return;
        }
        Log.i(TAG, "[unregisterAdaterStateCallback]remove.");
        mAdapterStateCallbacksList.remove(callback);
    }

    /**
     * 
     * @param callback
     */
    public void unregisterScannedStateCallback(BluetoothCallback.BleDeviceScanned callback) {
        if (callback == null) {
            Log.w(TAG, "[unregisterScannedStateCallback] callback is null");
            return;
        }
        if (!mScannedCallbacksList.contains(callback)) {
            Log.w(TAG, "[unregisterScannedStateCallback] callback not contained in list");
            return;
        }
        Log.i(TAG, "[unregisterScannedStateCallback]remove.");
        mScannedCallbacksList.remove(callback);
    }

    /**
     * scanned device name has been changed listener
     * 
     */
    public interface DeviceNameChangeListener {
        void onDeviceNameChange(BluetoothDevice device, String name);
    }

    public interface DeviceConnectionChangeListener {
        void onDeviceConnectionStateChange(BluetoothDevice device, int state);
    }

    public interface ServiceConnectionListener {
        void onServiceConnectionChange(int profileService, int connection);
    }

    public void registerNameChangeListener(DeviceNameChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "[registerNameChangeListener] listener is null");
            return;
        }
        Log.i(TAG, "[registerNameChangeListener]add.");
        mScannedDeviceNameChangeListenersList.add(listener);
    }

    public void unregisterNameChangeListener(DeviceNameChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "[unregisterNameChangeListener] listener is null");
            return;
        }
        Log.i(TAG, "[unregisterNameChangeListener]remove.");
        mScannedDeviceNameChangeListenersList.remove(listener);
    }

    public void registerConnectionStateChangeListener(DeviceConnectionChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "[registerConnectionStateChangeListener] listener is null");
            return;
        }
        Log.i(TAG, "[registerConnectionStateChangeListener]add.");
        mDeviceConnectionChangeListenersList.add(listener);
    }

    public void unregisterConnectionStateChangeListener(DeviceConnectionChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "[unregisterConnectionStateChangeListener] listener is null");
            return;
        }
        Log.i(TAG, "[unregisterConnectionStateChangeListener]remove.");
        mDeviceConnectionChangeListenersList.remove(listener);
    }

    public void registerServiceConnectionListener(ServiceConnectionListener listener) {
        if (listener == null) {
            Log.w(TAG, "[registerServiceConnectionListener] listener is null");
            return;
        }
        Log.i(TAG, "[registerServiceConnectionListener]add.");
        mServiceConnectionListeners.add(listener);
    }

    public void unregisterServiceConnectionListener(ServiceConnectionListener listener) {
        if (listener == null) {
            Log.w(TAG, "[unregisterServiceConnectionListener] listener is null");
            return;
        }
        Log.i(TAG, "[unregisterServiceConnectionListener]remove.");
        mServiceConnectionListeners.remove(listener);
    }

    private void notifyConnectionStateChanged(BluetoothDevice device, int state) {
        if (mDeviceConnectionChangeListenersList.size() == 0) {
            Log.d(TAG, "[notifyConnectionStateChanged] size is 0, return");
            return;
        }
        for (DeviceConnectionChangeListener listener : mDeviceConnectionChangeListenersList) {
            listener.onDeviceConnectionStateChange(device, state);
        }
    }

    void onScannedDeviceNameChanged(BluetoothDevice device, String name) {
        Log.d(TAG, "[onScannedDeviceNameChanged] name : " + name);

        for (DeviceNameChangeListener listener : mScannedDeviceNameChangeListenersList) {
            listener.onDeviceNameChange(device, name);
        }
    }

    private void notifyServiceConnectionChanged(int profile, int connection) {
        for (ServiceConnectionListener listener : mServiceConnectionListeners) {
            listener.onServiceConnectionChange(profile, connection);
        }
    }

    public void setBackgroundMode(boolean isBackground) {
        boolean success = false;
        if (mBleProfileServiceManager != null) {
            success = mBleProfileServiceManager.setBackgroundMode(isBackground);
            Log.d(TAG, "[setBackgroundMode]isBackground : " + isBackground + ",success = "
                    + success);
        } else {
            Log.w(TAG, "[setBackgroundMode] mProfileServiceManager is null");
        }
    }

    public int getBackgroundMode() {
        int mode = BleProfileServiceManager.STATUS_ENABLED;
        if (mBleProfileServiceManager != null) {
            mode = mBleProfileServiceManager.getBackgroundMode();
        } else {
            Log.w(TAG, "[getBackgroundMode] mProfileServiceManager is null");
        }
        return mode;
    }

    /**
     * used to turn on bluetooth
     */
    public void turnOnBluetooth() {
        mLocalBluetoothAdapter.setBluetoothEnabled(true);
    }

    /**
     * used to turn off bluetooth
     */
    public void turnOffBluetooth() {
        mLocalBluetoothAdapter.setBluetoothEnabled(false);
    }

    /**
     * start to ble device scan action
     * 
     * @param order
     */
    public void startLEScan(int order) {
        Log.i(TAG, "[startLEScan]order = " + order);
        // mScanOrder = order;
        mLocalBluetoothAdapter.startScanning(true, mLeCallback);
    }

    /**
     * stop ble device scan action
     */
    public void stopLEScan() {
        Log.i(TAG, "[stopLEScan]...");
        mLocalBluetoothAdapter.stopScanning(mLeCallback);
    }

    /**
     * Get bluetooth adapter current state
     * 
     * @return
     */
    public int getCurrentState() {
        return mLocalBluetoothAdapter.getBluetoothState();
    }

    /**
     * used to connect gatt device.
     * 
     * first should get {@link BleGattDevice} from {@link CachedBleGattDevice}
     * if the device is not in CachedBleGattDevice, just create BleGattDevice
     * from {@link BleGattDeviceManager.createGattDevce} method. if the device
     * in CachedBleGattDevice, just return the BleGattDevice from the cached
     * map.
     * 
     * @param device
     *            {@link BluetoothDevice} remote BluetoothDevice which will do
     *            connect action
     * @param autoConnect
     *            whether the device can do auto-connect or not.
     * @param locationIndex
     *            the locationIndex which will be showed in 3D view.
     */
    public void connectGattDevice(BluetoothDevice device, boolean autoConnect, int locationIndex) {
        Log.i(TAG, "[connectGattDevice]autoConnect = " + autoConnect + ",locationIndex = "
                + locationIndex);
        if (device == null) {
            throw new IllegalArgumentException("connectGattDevice device is null");
        }

        CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
        if (cachedDevice != null) {
            if (cachedDevice.getConnectionState() == BluetoothGatt.STATE_CONNECTING) {
                Log.i(TAG, "[connectGattDevice] cached device is connecting,"
                        + " no need do connect again");
                return;
            }
        } else {
            Log.d(TAG, "[connectGattDevice] call to add gatt device in FWK");
            addGattDevice(device);
        }

        if (mCachedBleGattDevice != null) {
            // get BleGattDevice from CachedBleGattDevice
            BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(mBleDeviceManager,
                    device, mGattDeviceCallback);
            if (gattDevice != null) {
                Log.d(TAG, "[connectGattDevice] start to connect gatt device");
                gattDevice.connect();
                // to make sure the device can be updated
                if (mConnectionHashMap.containsKey(device)) {
                    mConnectionHashMap.remove(device);
                }
                mConnectionHashMap.put(device, locationIndex);
                mCachedBleDeviceManager.onDeviceConnectionStateChanged(device,
                        BluetoothGatt.STATE_CONNECTING);
            } else {
                Log.w(TAG, "[connectGattDevice] gattDevice is null");
            }
        } else {
            Log.d(TAG, "[connectGattDevice] mCachedBleGattDevice is null");
            Log.d(TAG, "[connectGattDevice] mCachedBleGattDevice is null, add to pending list");
            if (mConnectingDevicesList.contains(device)) {
                mConnectingDevicesList.remove(device);
            }
            mConnectingDevicesList.add(device);
            if (mConnectionHashMap.containsKey(device)) {
                mConnectionHashMap.remove(device);
            }
            mConnectionHashMap.put(device, locationIndex);
            mCachedBleDeviceManager.onDeviceConnectionStateChanged(device,
                    BluetoothGatt.STATE_CONNECTING);
        }
    }

    /**
     * disconnect the gatt device
     * 
     * @param device
     */
    public void disconnectGattDevice(BluetoothDevice device) {
        Log.i(TAG, "[disconnectGattDevice]...");
        if (device == null) {
            throw new IllegalArgumentException("disconnectGattDevice device is null");
        }

        if (mCachedBleGattDevice != null) {
            BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(mBleDeviceManager,
                    device, mGattDeviceCallback);
            if (gattDevice != null) {
                Log.d(TAG, "[disconnectGattDevice] start to disconnect gatt device");
                gattDevice.disconnect();
                mConnectionHashMap.remove(device);
            } else {
                Log.w(TAG, "[disconnectGattDevice] gattDevice is null");
            }
        } else {
            Log.d(TAG, "[disconnectGattDevice] mCachedBleGattDevice is null");
        }
        if (mConnectingDevicesList.contains(device)) {
            mConnectingDevicesList.remove(device);
        }
    }

    /**
     * 
     * @param state
     */
    void onAdapterStateChanged(int state) {
        for (BluetoothCallback.BluetoothAdapterState callback : mAdapterStateCallbacksList) {
            callback.onBluetoothStateChanged(state);
        }
        mLocalBluetoothAdapter.syncBluetoothState();
        if (mLocalBluetoothAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
            Log.d(TAG, "[onAdapterStateChanged] adapter state changed to STATE_ON");
            initFwk();
            if (mBleProfileServiceManager != null) {
                Log.d(TAG, "[onAdapterStateChanged] call launch service");
                mBleProfileServiceManager.launchServices();
            } else {
                Log.d(TAG, "[onAdapterStateChanged] STATE_ON mProfileServiceManager is null");
            }
        } else if (mLocalBluetoothAdapter.getBluetoothState() == BluetoothAdapter.STATE_OFF) {
            Log.d(TAG, "[onAdapterStateChanged] adapter state changed to STATE_OFF,mIsFwkClosed = " + mIsFwkClosed);
            mIsFwkInited = false;
            if (!mIsFwkClosed) {
                mIsFwkClosed = true;
                if (mBleManager != null) {
                    if (mBlePxpService != null) {
                        mBleManager.closeProfileServiceProxy(BleProfile.PXP, mBlePxpService);
                        mBlePxpService = null;
                    }
                    if (mBleAnpService != null) {
                        mBleManager.closeProfileServiceProxy(BleProfile.ANP, mBleAnpService);
                        mBleAnpService = null;
                    }
                    if (mBleDeviceManager != null) {
                        mBleManager.closeDeviceManager(mBleDeviceManager);
                        mBleDeviceManager = null;
                    }
                    // if (mProfileServiceManager != null) {
                    // mBleManager.closeProfileServiceManager(mProfileServiceManager);
                    // }
                }
            }

            if (mBleProfileServiceManager != null) {
                Log.i(TAG, "[onAdapterStateChanged] call shutdown service");
                mBleProfileServiceManager.shutdownServices();
            } else {
                Log.w(TAG, "[onAdapterStateChanged] STATE_OFF mProfileServiceManager is null");
            }
            if (mCachedBleGattDevice != null) {
                mCachedBleGattDevice.clearData();
                mCachedBleGattDevice = null;
            }
        }
    }

    /**
     * 
     * @param started
     */
    void onAdapterScanningStateChanged(boolean started) {
        Log.i(TAG, "[onAdapterScanningStateChanged]started = " + started);
        for (BluetoothCallback.BluetoothAdapterState callback : mAdapterStateCallbacksList) {
            callback.onBluetoothScanningStateChanged(started);
        }
    }

    /**
     * When remote device scanned, add the device to the list view which will
     * show in alert dialog
     * 
     * @param device
     */
    private void onDeviceAdded(BluetoothDevice device) {
        Log.i(TAG, "[onDeviceAdded]...");
        for (BluetoothCallback.BleDeviceScanned callback : mScannedCallbacksList) {
            callback.onScannedBleDeviceAdded(device);
        }
    }

    /**
     * When remote device disappeared, delete the device from the list view
     * which will show in alert dialog
     * 
     * @param device
     */
    void onDeviceDeleted(BluetoothDevice device) {
        Log.i(TAG, "[onDeviceDeleted]...");
        for (BluetoothCallback.BleDeviceScanned callback : mScannedCallbacksList) {
            callback.onScannedBleDeviceRemoved(device);
        }
    }

    /**
     * initialization FMP & PXP profile interface which used to register a
     * callback to receive profile action.
     * 
     * @param device
     */
    private void initProfileInterfaces(BleGattDevice device) {
        Log.d(TAG, "[initProfileInterfaces] ...");
        if (device == null) {
            Log.w(TAG, "[initProfileInterfaces] device is null");
            return;
        }
        if (device.getService(BleGattUuid.Service.IMMEDIATE_ALERT) != null) {
            BleFindMeProfile fmpProfile = (BleFindMeProfile) device
                    .asProfileInterface(BleProfile.FMP);
            if (fmpProfile == null) {
                Log.d(TAG, "[initProfileInterfaces] fmpProfile is null");
                return;
            }
            // TODO:
            // fmpProfile.registerProfileCallback(mFmpProfileCallback);
        }
    }

    /**
     * Find target device according to {@link BluetoothDevice}.
     * 
     * if the device is not in cached device manager, cann't find the target
     * device if the device is not support FMP profile, cann't find the target
     * device
     * 
     * if the device is alerted, {@link mFmpProfileCallback.onTargetAlerted}
     * will be called
     * 
     * @param level
     *            should only be
     *            BleFindMeProfile.ALERT_LEVEL_NO,BleFindMeProfile
     *            .ALERT_LEVEL_MIDDLE, BleFindMeProfile.ALERT_LEVEL_HIGH.
     * 
     * @param device
     *            {@link BluetoothDevice}
     */
    public void findTargetDevice(int level, BluetoothDevice device) {
        if (level != BleFindMeProfile.ALERT_LEVEL_NO && level != BleFindMeProfile.ALERT_LEVEL_MILD
                && level != BleFindMeProfile.ALERT_LEVEL_HIGH) {
            Log.w(TAG, "[findTargetDevice] level is wrong defination");
            return;
        }
        if (device == null) {
            Log.w(TAG, "[findTargetDevice] device is null");
            return;
        }
        CachedBleDevice cachedDevice = this.mCachedBleDeviceManager.findDevice(device);
        if (cachedDevice == null) {
            Log.w(TAG, "[findTargetDevice] cachedDevice is null");
            return;
        }
        // TODO :this maybe has a error, if the service list is empty
        // or the service has not been found
        if (!cachedDevice.isSupportFmp()) {
            Log.w(TAG, "[findTargetDevice] cachedDevice is not support FMP");
            return;
        }
        if (cachedDevice.getConnectionState() != BleGattDevice.STATE_CONNECTED) {
            Log.d(TAG, "[findTargetDevice] cachedDevice is not in connected state, return");
            return;
        }
        BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(mBleDeviceManager, device,
                mGattDeviceCallback);
        if (gattDevice == null) {
            Log.w(TAG, "[findTargetDevice] gattDevice is null");
            return;
        }
        BleFindMeProfile fmpProfile = (BleFindMeProfile) gattDevice
                .asProfileInterface(BleProfile.FMP);
        if (fmpProfile == null) {
            Log.w(TAG, "[findTargetDevice] fmpProfile is null");
            return;
        }
        Log.d(TAG, "[findTargetDevice] start to find gatt device,level = " + level);
        if (level == BleFindMeProfile.ALERT_LEVEL_HIGH) {
            Log.d(TAG, "[findTargetDevice] level is ALERT_LEVEL_HIGH");
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG, true);
        } else if (level == BleFindMeProfile.ALERT_LEVEL_NO) {
            Log.d(TAG, "[findTargetDevice] level is ALERT_LEVEL_NO");
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG, false);
        }
        fmpProfile.findTarget(level);
    }

    /**
     * Which used to stop remote device alert, which will be used in PXP service
     * 
     * @param device
     */
    public void stopRemoteDeviceAlert(BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "[stopRemoteDeviceAlert] device is null");
            return;
        }
        if (mBlePxpService == null) {
            Log.w(TAG, "[stopRemoteDeviceAlert] mProximityService is null");
            return;
        }
        boolean b = mBlePxpService.stopRemoteAlert(device);
        Log.i(TAG, "[stopRemoteDeviceAlert] return result : " + b);
    }

    private void updateDevicePxpState(CachedBleDevice cachedDevice) {
        if (cachedDevice == null) {
            Log.w(TAG, "[updateDevicePxpState] cachedDevice is null");
            return;
        }
        if (mBlePxpService == null) {
            Log.w(TAG, "[updateDevicePxpState] mBleProximityProfileService is null");
            return;
        }
        if (cachedDevice.getConnectionState() == BluetoothGatt.STATE_CONNECTED) {
            if (cachedDevice.isSupportPxpOptional()) {
                cachedDevice.onDevicePxpAlertStateChange(mBlePxpService.isAlertOn(cachedDevice
                        .getDevice()));
                cachedDevice.setIntAttribute(CachedBleDevice.DEVICE_CURRENT_TX_POWER_FLAG,
                        mBlePxpService.getPathLoss(cachedDevice.getDevice()));

                if (!mPxpCallbackHashMap.containsKey(cachedDevice.getDevice())) {
                    boolean b = mBlePxpService.registerStatusChangeCallback(
                            cachedDevice.getDevice(), mProximityProfileServiceCallback);
                    if (b) {
                        Log.d(TAG, "[updateDevicePxpState] add to hashmap");
                        mPxpCallbackHashMap.put(cachedDevice.getDevice(),
                                mProximityProfileServiceCallback);
                    }
                }
            }
        }

        BleProximityProfileService.DevicePxpParams param = mBlePxpService
                .getPxpParameters(cachedDevice.getDevice());
        if (param != null) {
            Log.d(TAG, "[updateDevicePxpState]...!");
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_ALERT_SWITCH_ENABLER_FLAG,
                    param.getAlertEnabler() == 0 ? false : true);
            cachedDevice.setBooleanAttribute(
                    CachedBleDevice.DEVICE_DISCONNECTION_WARNING_EANBLER_FLAG,
                    param.getDisconnEnabler() == 0 ? false : true);
            cachedDevice.setIntAttribute(CachedBleDevice.DEVICE_IN_OUT_RANGE_ALERT_FLAG,
                    param.getRangeType());
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_RANGE_ALERT_ENABLER_FLAG,
                    param.getRangeAlertEnabler() == 0 ? false : true);
            cachedDevice.setIntAttribute(CachedBleDevice.DEVICE_RANGE_VALUE_FLAG,
                    param.getRangeValue());
        } else {
            Log.w(TAG, "[updateDevicePxpState]param is null!");
        }
    }

    // public boolean isPxpProfileServiceReady() {
    // if (mProximityService == null) {
    // return false;
    // }
    // return true;
    // }
    //
    // public boolean isAnpProfileServiceReady() {
    // if (mAlertProfileService == null) {
    // return false;
    // }
    // return true;
    // }

    void updateAnpData(CachedBleDevice device) {
        if (device == null) {
            Log.w(TAG, "[updateAnpData] device is null");
            return;
        }
        if (mBleAnpService == null) {
            Log.w(TAG, "[updateAnpData] mBleAlertNotificationProfileService is null");
            return;
        }
        SparseIntArray hostSparce = mBleAnpService.getDeviceSettings(device.getDevice()
                .getAddress(), CATEGORY_IDS);
        if (hostSparce == null) {
            Log.w(TAG, "[updateAnpData] hostSparce return null");
            return;
        }
        if (hostSparce.size() == 0) {
            Log.w(TAG, "[updateAnpData] hostSparce return size is 0");
            return;
        }
        Log.d(TAG, "[updateAnpData] updater anp host data");
        device.setBooleanAttribute(CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG,
                hostSparce.get(CATEGORY_ID_INCOMING_CALL) == 0 ? false : true);
        device.setBooleanAttribute(CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG,
                hostSparce.get(CATEGORY_ID_MISSED_CALL) == 0 ? false : true);
        device.setBooleanAttribute(CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG,
                hostSparce.get(CATEGORY_ID_SMS) == 0 ? false : true);
        device.setBooleanAttribute(CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG,
                hostSparce.get(CATEGORY_ID_EMAIL) == 0 ? false : true);

        SparseIntArray remoteSparce = mBleAnpService.getRemoteSettings(device.getDevice()
                .getAddress(), CATEGORY_IDS);
        if (remoteSparce == null) {
            Log.w(TAG, "[updateAnpData] remoteSparce return null");
            return;
        }
        if (remoteSparce.size() == 0) {
            Log.w(TAG, "[updateAnpData] getRemoteSettings return size is 0");
            return;
        }
        Log.d(TAG, "[updateAnpData] update anp remote data");
        device.setBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_INCOMING_CALL_FLAGE,
                remoteSparce.get(CATEGORY_ID_INCOMING_CALL) == 0 ? false : true);
        device.setBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_MISSED_CALL_FLAGE,
                remoteSparce.get(CATEGORY_ID_MISSED_CALL) == 0 ? false : true);
        device.setBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_NEW_MESSAGE_FLAGE,
                remoteSparce.get(CATEGORY_ID_SMS) == 0 ? false : true);
        device.setBooleanAttribute(CachedBleDevice.DEVICE_REMOTE_NEW_EMAIL_FLAGE,
                remoteSparce.get(CATEGORY_ID_EMAIL) == 0 ? false : true);

    }

    /**
     * used to update UX configuration to ans table
     */
    private void updateAnpDataToClientTable(CachedBleDevice cachedDevice, int whichAttribute) {
        if (cachedDevice == null) {
            Log.w(TAG, "[updateAnpDataToClientTable] cachedDevice is null");
            return;
        }
        if (cachedDevice.getDevice() == null) {
            Log.w(TAG, "[updateAnpDataToClientTable] cachedDevice.getDevice is null");
            return;
        }
        if (mBleAnpService == null) {
            Log.w(TAG, "[updateAnpDataToClientTable]mBleAlertNotificationProfileService is null");
            return;
        }

        int whichCategory = -1;
        int value = 0;
        Log.d(TAG, "[updateAnpDataToClientTable]whichAttribute = " + whichAttribute);
        switch (whichAttribute) {
        case CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG:
            whichCategory = CATEGORY_ID_INCOMING_CALL;
            value = cachedDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG) ? 3 : 0;
            break;

        case CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG:
            whichCategory = CATEGORY_ID_MISSED_CALL;
            value = cachedDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG) ? 3 : 0;
            break;

        case CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG:
            whichCategory = CATEGORY_ID_SMS;
            value = cachedDevice
                    .getBooleanAttribute(CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG) ? 3 : 0;
            break;

        case CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG:
            whichCategory = CATEGORY_ID_EMAIL;
            value = cachedDevice.getBooleanAttribute(CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG) ? 3
                    : 0;
            break;

        default:
            break;
        }
        if (whichCategory == -1) {
            Log.w(TAG, "[updateAnpDataToClientTable] whichCategory is -1, not recognize");
            return;
        }
        SparseIntArray s = new SparseIntArray();
        s.put(whichCategory, value);

        mBleAnpService.updateDeviceSettings(cachedDevice.getDevice().getAddress(), s);
    }

    /**
     * {@link CachedBleDeviceManager.CachedDeviceListChangedListener} which used
     * to update the device connection state while the app is first to load if
     * the {@link CachedBleGattDevice} is initialized before than
     * CachedBluetoothLEDeviceManager to add the device, the device state should
     * be update from this callback, which is from
     * {@link BleDeviceManagerService} which is running all the time in the
     * background.
     * 
     * while all cached device connection state has been updated, unregister
     * this callback to release resource.
     * 
     */
    private CachedBleDeviceManager.CachedDeviceListChangedListener mCachedDeviceListChangedListener = new CachedBleDeviceManager.CachedDeviceListChangedListener() {

        @Override
        public void onDeviceRemoved(CachedBleDevice device) {
            Log.i(TAG, "[onDeviceRemoved]...");
            if (device == null) {
                Log.w(TAG, "[onDeviceRemoved] device is null");
                return;
            }
            if (mBlePxpService != null) {
                mBlePxpService.unregisterStatusChangeCallback(device.getDevice(),
                        mProximityProfileServiceCallback);
                mPxpCallbackHashMap.remove(device.getDevice());
            }
            removeGattDevice(device.getDevice());
            device.unregisterAttributeChangeListener(mDeviceAttributeListener);
        }

        @Override
        public void onDeviceAdded(CachedBleDevice device) {
            Log.i(TAG, "[onDeviceAdded]...");
            if (device == null) {
                Log.w(TAG, "[onDeviceAdded] device is null");
                return;
            }
            device.registerAttributeChangeListener(mDeviceAttributeListener);
            // update device connection state and device service list
            // while add device happened after device manager service connected
            if (mCachedBleGattDevice != null) {
                BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(mBleDeviceManager,
                        device.getDevice(), mGattDeviceCallback);
                if (gattDevice != null) {
                    int state = gattDevice.getState();
                    Log.d(TAG, "[onDeviceAdded] state : " + state);
                    device.onConnectionStateChanged(state);
                    if (device.getServiceList().size() == 0) {
                        device.onServiceDiscovered(gattDevice.getServices());
                    }
                    initProfileInterfaces(gattDevice);
                } else {
                    Log.e(TAG, "[onDeviceAdded] gattDevice is null");
                }
            } else {
                Log.w(TAG, "[onDeviceAdded] mCachedBleGattDevice is null,"
                        + " cann't update cached device connect state");
            }

            // update device state alert state & distance value from
            // proximity profile service
            // while get proximity profile service is before add device.
            // TODO if device is support pxp or not, maybe change to TxPower and
            // IAS
            updateDevicePxpState(device);
            updateAnpData(device);
        }
    };

    /**
     * which used to bind {@link BleDeviceManagerService} which is running in
     * the background. after bind success, the {@link onServiceConnected} will
     * be called, then initialize the mBleDeviceManager and
     * {@link CachedBleGattDevice}.
     * 
     * if the mCachedDeviceManager.addDevice is called before this callback.
     * after this callback returned, should update all the cached device
     * connection state.
     * 
     */
    private BleDeviceManager.DeviceManagerListener mDeviceManagerListener = new BleDeviceManager.DeviceManagerListener() {

        public void onServiceConnected(BleDeviceManager proxy) {
            if (proxy == null) {
                Log.w(TAG, "[onServiceConnected] proxy is null");
                return;
            }
            Log.i(TAG, "[onServiceConnected] set proxy");
            mBleDeviceManager = proxy;
            mCachedBleGattDevice = new CachedBleGattDevice(mContext);
            mIsDeviceManagerServiceConnected = true;
            notifyServiceConnectionChanged(PROFILE_DEVICE_MANAGER_SERVICE_ID, PROFILE_CONNECTED);

            if (mCachedBleDeviceManager.getCachedDevicesCopy().size() != 0) {
                for (CachedBleDevice device : mCachedBleDeviceManager.getCachedDevicesCopy()) {
                    BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(
                            mBleDeviceManager, device.getDevice(), mGattDeviceCallback);
                    if (gattDevice != null) {
                        int state = gattDevice.getState();
                        Log.d(TAG, "[onServiceConnected] update cached device"
                                + " connection state : " + state);
                        device.onConnectionStateChanged(state);
                        if (device.getServiceList().size() == 0) {
                            device.onServiceDiscovered(gattDevice.getServices());
                        }
                        initProfileInterfaces(gattDevice);
                    } else {
                        Log.e(TAG, "[onServiceConnected] gattDevice is null");
                    }
                }
            }
            if (mConnectingDevicesList.size() != 0) {
                Log.d(TAG, "[mDeviceManagerListener] start to connect pending devices");
                for (BluetoothDevice device : mConnectingDevicesList) {
                    BleGattDevice gattDevice = mCachedBleGattDevice.getBleGattDevice(
                            mBleDeviceManager, device, mGattDeviceCallback);
                    if (gattDevice != null) {
                        gattDevice.connect();
                    } else {
                        Log.e(TAG, "[mDeviceManagerListener] pending devices gattDevice is null");
                    }
                }
                mConnectingDevicesList.clear();
            }
        }

        public void onServiceDisconnected() {
            Log.i(TAG, "[onServiceDisconnected] reset proxy");
            mBleDeviceManager = null;
            if (mCachedBleGattDevice != null) {
                mCachedBleGattDevice.clearData();
                mCachedBleGattDevice = null;
            }
            mIsDeviceManagerServiceConnected = false;
            notifyServiceConnectionChanged(PROFILE_DEVICE_MANAGER_SERVICE_ID, PROFILE_DISCONNECTED);
        }
    };

    /**
     * which used to communicate with BleProximityProfie background service
     * 
     * while service connected, call to query distance state, and alert state
     */
    private BleProfileService.ProfileServiceListener mProfileServiceListener = new BleProfileService.ProfileServiceListener() {

        public void onServiceConnected(int profile, BleProfileService proxy) {
            if (proxy == null) {
                Log.w(TAG, "[mProfileServiceListener] onServiceConnected, proxy is null");
                return;
            } else if (mIsFwkClosed) {
                Log.d(TAG, "[mProfileServiceListener] onServiceConnected, mIsFwkClosed TRUE,"
                        + "close profile service");
                if (null == mBleManager) {
                    mBleManager = BleManager.getDefaultBleProfileManager();
                }
                mBleManager.closeProfileServiceProxy(profile, proxy);
                return;
            }
            Log.d(TAG, "[mProfileServiceListener] onServiceConnected, profile : " + profile);
            if (profile == BleProfile.PXP) {
                if (proxy instanceof BleProximityProfileService) {
                    mIsPxpServiceConnected = true;
                    notifyServiceConnectionChanged(PROFILE_PXP_ID, PROFILE_CONNECTED);
                    Log.d(TAG,
                            "[mProfileServiceListener] onServiceConnected, init mProximityService");
                    mBlePxpService = (BleProximityProfileService) proxy;

                    // if cached device manager is not empty, try to update
                    // device
                    // distance & alert state from pxp service
                    if (mCachedBleDeviceManager.getCachedDevicesCopy().size() != 0) {
                        Log.d(TAG, "[mProfileServiceListener] onServiceConnected do"
                                + " pxp data update");
                        for (CachedBleDevice cachedDevice : mCachedBleDeviceManager
                                .getCachedDevicesCopy()) {
                            // check device support pxp or not
                            // TODO maybe change to TxPower, and IAS
                            updateDevicePxpState(cachedDevice);
                        }
                    } else {
                        Log.i(TAG, "[mProfileServiceListener] onServiceConnected,"
                                + " cached device manager is empty");
                    }
                } else {
                    Log.w(TAG, "[mProfileServiceListener] onServiceConnected,"
                            + " proxy is not match BleProximityProfileService");
                }
            } else if (profile == BleProfile.ANP) {
                if (proxy instanceof BleAlertNotificationProfileService) {
                    mIsAnpServiceConnected = true;
                    mBleAnpService = (BleAlertNotificationProfileService) proxy;

                    notifyServiceConnectionChanged(PROFILE_ANP_ID, PROFILE_CONNECTED);
                    if (mCachedBleDeviceManager.getCachedDevicesCopy().size() != 0) {
                        Log.d(TAG, "[mProfileServiceListener] onServiceConnected do"
                                + " anp data initialization");
                        for (CachedBleDevice cachedDevice : mCachedBleDeviceManager
                                .getCachedDevicesCopy()) {
                            updateAnpData(cachedDevice);
                        }
                    } else {
                        Log.i(TAG, "[mProfileServiceListener] onServiceConnected, "
                                + "cached device is 0");
                    }
                } else {
                    Log.i(TAG, "[mProfileServiceListener] onServiceConnected,"
                            + " proxy is not match BleAlertNotificationProfileService");
                }
            } else {
                Log.w(TAG, "[mProfileServiceListener] onServiceConnected,"
                        + "profile not match PXP & ANP");
            }
        }

        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "[mProfileServiceListener] onServiceDisconnected,profile = " + profile);
            if (profile == BleProfile.PXP) {
                mIsPxpServiceConnected = false;
                notifyServiceConnectionChanged(PROFILE_PXP_ID, PROFILE_DISCONNECTED);
                mBlePxpService = null;
                mPxpCallbackHashMap.clear();
            } else if (profile == BleProfile.ANP) {
                mIsAnpServiceConnected = false;
                notifyServiceConnectionChanged(PROFILE_ANP_ID, PROFILE_DISCONNECTED);
                mBleAnpService = null;
            }
        }
    };

    /**
     * mServiceManagerListener which to used to make mProfileServiceManager to
     * be proxy while the service is connected, do launch service action. while
     * the service is disconnected, so shutdown service action.
     */
    private BleProfileServiceManager.ProfileServiceManagerListener mServiceManagerListener = new BleProfileServiceManager.ProfileServiceManagerListener() {
        public void onServiceConnected(BleProfileServiceManager proxy) {
            if (proxy == null) {
                Log.e(TAG, "[mServiceManagerListener] onServiceConnected service"
                        + " manager proxy is null");
                return;
            }
            Log.d(TAG, "[mServiceManagerListener] onServiceConnected set service manager proxy");
            mBleProfileServiceManager = proxy;

            if (mLocalBluetoothAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
                Log.d(TAG, "[mServiceManagerListener] onServiceConnected"
                        + " do launch service action");
                mBleProfileServiceManager.launchServices();
            } else {
                Log.d(TAG, "[mServiceManagerListener] BT is off, do not launch service");
            }
        }

        public void onServiceDisconnected(BleProfileServiceManager proxy) {
            Log.i(TAG, "[mServiceManagerListener] onServiceDisconnected set"
                    + " service manager to be null");
            mBleProfileServiceManager = null;
        }
    };

    private void addGattDevice(BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "[addGattDevice] device is null");
            return;
        }
        if (this.mBleDeviceManager != null) {
            Log.d(TAG, "[addGattDevice] call to add device");
            mBleDeviceManager.addGattDevice(device);
        }
    }

    private void removeGattDevice(BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "[removeGattDevice] device is null");
            return;
        }
        if (this.mBleDeviceManager != null) {
            Log.d(TAG, "[removeGattDevice] call to delete device");
            mBleDeviceManager.deleteGattDevice(device);
        }
    }

    public void updateClientPxpData(BluetoothDevice device, boolean alertEnabler,
            boolean rangeAlertEnabler, boolean disconnectWarningEnabler, int rangeType,
            int rangeValue) {
        if (device == null) {
            Log.w(TAG, "[updateClientPxpData] device is null");
            return;
        }
        Log.d(TAG, "[updateClientPxpData] alertEnabler : " + alertEnabler
                + ", rangeAlertEnabler : " + rangeAlertEnabler + ", disconnectWarningEnabler : "
                + disconnectWarningEnabler + ", rangeType : " + rangeType + ", rangeValue : "
                + rangeValue);
        if (mBlePxpService != null) {
            mBlePxpService.setPxpParameters(device, alertEnabler ? 1 : 0,
                    rangeAlertEnabler ? 1 : 0, rangeType, rangeValue, disconnectWarningEnabler ? 1
                            : 0);
        } else {
            Log.d(TAG, "[updateClientPxpData] mProximityService is null, cann't update pxp data");
        }
    }

    /**
     * ble gatt device callback used to update device connection state & service
     * discovered callback while connection state has been changed, if the
     * device is not in cache, should add the device to cache, and update state
     * to be actual connection state
     * 
     * if the device is in cache, should only update the device connection
     * state.
     * 
     * service discovery callback, while the connection state is connected,
     * begin to discovery the device services, after discovered, the callback
     * {@link onServicesDiscovered} will be called. and then update the services
     * to be the cached device.
     * 
     */
    private BleGattDevice.BleGattDeviceCallback mGattDeviceCallback = new BleGattDevice.BleGattDeviceCallback() {

        /**
         * device connection state change callback.
         * 
         * @param gattDevice
         * @param status
         * @param newState
         */
        public void onConnectionStateChange(BleGattDevice gattDevice, int status, int newState) {
            if (gattDevice == null) {
                Log.w(TAG, "[mGattDeviceCallback.onConnectionStateChange] gattDevice is null");
                return;
            }
            Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange] device address : "
                    + gattDevice.getDevice());
            Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange] status : " + status
                    + ", newState : " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange] status is GATT_SUCCESS");
                BluetoothDevice device = gattDevice.getDevice();
                if (device == null) {
                    Log.w(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                            + " gattDevice is null!!");
                    return;
                }

                if (newState == BleGattDevice.STATE_CONNECTED) {
                    Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                            + " new state is STATE_CONNECTED");
                    if (mCachedBleDeviceManager.findDevice(device) == null) {
                        if (mConnectionHashMap.containsKey(device)) {
                            Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                                    + " call to add device to cacher");
                            mCachedBleDeviceManager.addDevice(device,
                                    mConnectionHashMap.get(device));
                        } else {
                            Log.e(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                                    + " device is not in map");
                            return;
                        }
                    }
                    mCachedBleDeviceManager.onDeviceConnectionStateChanged(device,
                            BleGattDevice.STATE_CONNECTED);
                    Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                            + " start to discover device services");
                    gattDevice.discoverServices();
                    CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
                    if (cachedDevice != null) {
                        cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_AUTO_CONNECT_FLAG,
                                true);
                        updateDevicePxpState(cachedDevice);
                    }
                } else if (newState == BleGattDevice.STATE_DISCONNECTED) {
                    Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                            + " new state is STATE_DISCONNECTED");
                    CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
                    if (cachedDevice == null) {
                        Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                                + " call to remove gatt device in FWK");
                        removeGattDevice(device);
                        // While the device not in cache, close it.
                        gattDevice.close();
                        if (mCachedBleGattDevice != null) {
                            mCachedBleGattDevice.removeDevice(device);
                        }
                    } else {
                        Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                                + " update cache device connection state to be STATE_DISCONNECTED");
                        mCachedBleDeviceManager.onDeviceConnectionStateChanged(device,
                                BleGattDevice.STATE_DISCONNECTED);
                        if (mBlePxpService != null) {
                            mBlePxpService.unregisterStatusChangeCallback(device,
                                    mProximityProfileServiceCallback);
                            mPxpCallbackHashMap.remove(device);
                        }
                    }
                    if (mConnectionHashMap.containsKey(device)) {
                        mConnectionHashMap.remove(device);
                    }
//                    if (gattDevice != null) {
//                        Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
//                                + " call to close gatt device");
//                        gattDevice.close();
//                        if (mCachedBleGattDevice != null) {
//                            mCachedBleGattDevice.removeDevice(device);
//                        }
//                    }
                } else {
                    mCachedBleDeviceManager.onDeviceConnectionStateChanged(device, newState);
                }
                notifyConnectionStateChanged(device, newState);
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d(TAG, "[mGattDeviceCallback.onConnectionStateChange]"
                        + " status return GATT_FAILURE!!");
            }
        }

        /**
         * service discovery callback
         * 
         * @param gattDevice
         * @param status
         */
        public void onServicesDiscovered(BleGattDevice gattDevice, int status) {
            Log.i(TAG, "[mGattDeviceCallback.onServicesDiscovered] status : " + status);
            if (gattDevice == null) {
                Log.w(TAG, "[mGattDeviceCallback.onServicesDiscovered] gatt device is null");
                return;
            }
            BluetoothDevice device = gattDevice.getDevice();
            if (device == null) {
                Log.w(TAG, "[mGattDeviceCallback.onServicesDiscovered] device is null");
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gattDevice.getServices();
                if (services == null || services.size() == 0) {
                    Log.w(TAG, "[mGattDeviceCallback.onServicesDiscovered]"
                            + " service list is null or empty");
                    return;
                }
                mCachedBleDeviceManager.onDeviceServiceDiscoveried(device, services);
                CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
                updateDevicePxpState(cachedDevice);
                initProfileInterfaces(gattDevice);
            } else {
                Log.i(TAG, "[mGattDeviceCallback.onServicesDiscovered] status : GATT_FAILURE");
            }
        }
    };

    private class ProximityProfileServiceCallback extends IProximityProfileServiceCallback.Stub {

        public void onAlertStatusChange(String address, boolean isAlert) {
            if (address == null || address.trim().length() == 0) {
                Log.w(TAG, "[onDistanceValueChange] address is null or empty");
                return;
            }
            Log.d(TAG, "[onAlertStatusChange] address : " + address + ", isAlert : " + isAlert);
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                Log.w(TAG, "[onAlertStatusChange] adapter is null");
                return;
            }
            BluetoothDevice device = adapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "[onAlertStatusChange] device is null");
                return;
            }
            CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "[onAlertStatusChange] cachedDevice is null");
                return;
            }
            cachedDevice.onDevicePxpAlertStateChange(isAlert);
        }

        public void onDistanceValueChange(String address, int value) {
            if (address == null || address.trim().length() == 0) {
                Log.w(TAG, "[onDistanceValueChange] address is null or empty");
                return;
            }
            Log.d(TAG, "[onDistanceValueChange] address : " + address + ", value : " + value);
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                Log.w(TAG, "[onDistanceValueChange] adapter is null");
                return;
            }
            BluetoothDevice device = adapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "[onDistanceValueChange] device is null");
                return;
            }
            CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "[onDistanceValueChange] cachedDevice is null");
                return;
            }
            cachedDevice.setIntAttribute(CachedBleDevice.DEVICE_CURRENT_TX_POWER_FLAG, value);
        }
    };

    /**
     * find me profile callback, if the device is alerted should update the
     * cached device fmp state
     */
    private BleFindMeProfile.ProfileCallback mFmpProfileCallback = new BleFindMeProfile.ProfileCallback() {
        public void onTargetAlerted(BluetoothDevice device, boolean isSuccess) {
            Log.d(TAG, "[onTargetAlerted] isSuccess : " + isSuccess);
            if (device == null) {
                Log.w(TAG, "[onTargetAlerted] device is null");
                return;
            }
            CachedBleDevice cachedDevice = mCachedBleDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "[onTargetAlerted] cachedDevice is null");
                return;
            }
            cachedDevice.setBooleanAttribute(CachedBleDevice.DEVICE_FMP_STATE_FLAG, isSuccess);
        }
    };

    private CachedBleDevice.DeviceAttributeChangeListener mDeviceAttributeListener = new CachedBleDevice.DeviceAttributeChangeListener() {

        @Override
        public void onDeviceAttributeChange(CachedBleDevice device, int which) {
            if (which != CachedBleDevice.DEVICE_INCOMING_CALL_ENABLER_FLAG
                    && which != CachedBleDevice.DEVICE_MISSED_CALL_ENABLER_FLAG
                    && which != CachedBleDevice.DEVICE_NEW_MESSAGE_ENABLER_FLAG
                    && which != CachedBleDevice.DEVICE_NEW_EMAIL_ENABLER_FLAG) {
                Log.d(TAG, "[mDeviceAttributeListener] which is no need to update");
                return;
            }
            Log.d(TAG, "[mDeviceAttributeListener] update anp configuration data");
            updateAnpDataToClientTable(device, which);
        }

    };

    /**
     * used to get scan le device callback.while ble device scanned, the
     * callback will be called.
     */
    private BluetoothAdapter.LeScanCallback mLeCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
            Log.d(TAG, "[onLeScan] address : " + device.getAddress() + ",name = " + name);
            onDeviceAdded(device);
        }
    };
}
