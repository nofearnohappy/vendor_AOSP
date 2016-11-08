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

package com.mediatek.bluetoothle.pxp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.bluetooth.BleDeviceManager;
import com.mediatek.bluetooth.BleFindMeProfile;
import com.mediatek.bluetooth.BleGattDevice;
import com.mediatek.bluetooth.BleGattDevice.BleGattDeviceCallback;
import com.mediatek.bluetooth.BleGattUuid;
import com.mediatek.bluetooth.BleManager;
import com.mediatek.bluetooth.BleProfile;
import com.mediatek.bluetooth.BleProximityProfile;
import com.mediatek.bluetooth.BleProximityProfileService;
import com.mediatek.bluetoothle.bleservice.BleProfileServiceBase;
import com.mediatek.bluetoothle.provider.DeviceParameterRecorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Creates instances to monitoring distances / dicsonnection and broadcast intents for UX apk to
 * handle the alert, as a service in the BluetoothLe background application
 */

public class ProximityProfileService extends BleProfileServiceBase {

    private static final String TAG = "[BLE][PXP]ProximityProfileService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private static final boolean RSSITEST = true;

    private BleDeviceManager mDeviceManager = null;
    private HashMap<String, ProximityDevice> mDeviceMap = null;
    private HashMap<String, ArrayList<IProximityProfileServiceCallback>> mPreRegisterCallbackMap
            = null;

    private ArrayList<String> mAddressList = null;

    // TODO: shall refine to entry base
    ContentObserver mPxpObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(final boolean selfChange) {

            if (DBG) {
                Log.d(TAG, "PxpObserver: onChange");
            }

            final ArrayList<String> deviceEntries = DeviceParameterRecorder
                    .getDeviceAddresses(ProximityProfileService.this);

            if (deviceEntries != null) {
                for (final String address : deviceEntries) {
                    if (mAddressList.contains(address)) {
                        final ProximityDevice device = mDeviceMap.get(address);
                        if (device != null) {
                            device.updateSetting(ProximityDevice.PXP_SETTING);
                        }
                    } else {
                        if (DBG) {
                            Log.d(TAG, "Create ProximityDevices:" + address);
                        }
                        final ProximityDevice device = new ProximityDevice(
                                ProximityProfileService.this,
                                address);
                        final ArrayList<IProximityProfileServiceCallback> callbackList =
                                mPreRegisterCallbackMap.get(address);
                        if (callbackList != null && !callbackList.isEmpty()) {
                            final Iterator<IProximityProfileServiceCallback> i = callbackList
                                    .iterator();
                            while (i.hasNext()) {
                                final IProximityProfileServiceCallback callback = i.next();
                                device.registerStatusChangeCallback(callback);
                            }
                        }
                        mPreRegisterCallbackMap.remove(address);
                        mDeviceMap.put(address, device);
                        mAddressList.add(address);
                    }
                }
            }

            final Iterator<String> i = mAddressList.iterator();

            final String deletedAddress = null;

            if (deviceEntries != null) {
                while (i.hasNext()) {
                    final String address = i.next();
                    if (!deviceEntries.contains(address)) {
                        if (DBG) {
                            Log.d(TAG, "Delete ProximityDevice:" + address);
                        }
                        i.remove();
                        final ProximityDevice device = mDeviceMap.get(address);
                        if (device != null) {
                            device.deviceDeleted();
                        }
                        mPreRegisterCallbackMap.remove(address);
                        mDeviceMap.remove(address);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            ProximityDevice pxpDevice = null;

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                final BluetoothDevice inDevice = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (inDevice != null) {
                    final String deviceAddress = inDevice.getAddress();

                    if (mAddressList.contains(deviceAddress)) {
                        pxpDevice = mDeviceMap.get(deviceAddress);
                    } else {
                        return;
                    }
                } else {
                    return;
                }

                final int bond = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothAdapter.ERROR);

                // BluetoothDevice.BOND_NONE : 10
                if (VDBG) {
                    Log.v(TAG, "mReceiver: ACTION_BOND_STATE_CHANGED. bond:" + bond);
                }

                if (bond == BluetoothDevice.BOND_NONE && pxpDevice != null) {
                    pxpDevice.setManualDisconnect(true);
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);

                // BluetoothAdapter.STATE_TURNING_OFF : 13
                if (VDBG) {
                    Log.v(TAG, "mReceiver: ACTION_STATE_CHANGED. state:" + state);
                }

                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    for (final String address : mAddressList) {
                        pxpDevice = mDeviceMap.get(address);
                        if (pxpDevice != null) {
                            pxpDevice.setManualDisconnect(true);
                        }
                    }
                }
            }
        }
    };

    private static final int DISCONNECT_DELAY_TIME = 1000;

    @Override
    public void onCreate() {
        if (DBG) {
            Log.d(TAG, "onCreate: get BleDeivceManager");
        }

        BleManager.getDefaultBleProfileManager().getDeviceManager(this,
                new BleDeviceManager.DeviceManagerListener() {

                    @Override
                    public void onServiceConnected(final BleDeviceManager manager) {
                        if (DBG) {
                            Log.d(TAG, "onServiceConnected");
                        }
                        mDeviceManager = manager;
                    }

                    @Override
                    public void onServiceDisconnected() {
                        if (DBG) {
                            Log.d(TAG, "onServiceDisconnected");
                        }
                        BleManager.getDefaultBleProfileManager()
                                .closeDeviceManager(mDeviceManager);
                        mDeviceManager = null;
                    }
                });

        mDeviceMap = new HashMap<String, ProximityDevice>();
        mPreRegisterCallbackMap =
                new HashMap<String, ArrayList<IProximityProfileServiceCallback>>();

        mAddressList = DeviceParameterRecorder.getDeviceAddresses(this);

        if (mAddressList != null) {
            for (final String address : mAddressList) {
                if (VDBG) {
                    Log.v(TAG, "Create ProximityDevices:" + address);
                }
                final ProximityDevice device = new ProximityDevice(this, address);
                mDeviceMap.put(address, device);
            }
        }

        DeviceParameterRecorder.registerRecoderObserver(this, mPxpObserver);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(mReceiver, filter);

        super.onCreate();

    }

    @Override
    protected IProfileServiceBinder initBinder() {
        if (DBG) {
            Log.d(TAG, "initBinder");
        }
        return new ProximityProfileServiceBinder(this);
    }

    @Override
    protected boolean start() {
        // this is called when BT power on
        if (DBG) {
            Log.d(TAG, "start()");
        }
        return super.start();
    }

    @Override
    protected boolean stop() {
        // this is called when BT power off
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        return super.stop();
    }

    @Override
    public void onDestroy() {
        if (DBG) {
            Log.d(TAG, "onDestroy()");
        }
        /// M: ALPS01838657: Quit HandlerThread if the device is deleted or PXP service is stopped. @{
        if (mAddressList != null) {
            for (final String address : mAddressList) {
                if (VDBG) {
                    Log.v(TAG, "Quit the HandlerThread of ProximityDevice: " + address);
                }
                ProximityDevice device = mDeviceMap.get(address);
                if (device != null) {
                    if(device.mDeviceHandler != null) {
                        Log.d(TAG, "Quit the HandlerThread");
                        device.mDeviceHandler.getLooper().quit();
                    }
                }
            }
        }
        /// @}
        DeviceParameterRecorder.unregisterRecorderObserver(this, mPxpObserver);

        unregisterReceiver(mReceiver);

        BleManager.getDefaultBleProfileManager().closeDeviceManager(mDeviceManager);

        super.onDestroy();
    }

    public boolean setPxpParameters(final BluetoothDevice device, final int alertEnabler,
            final int rangeAlertEnabler, final int rangeType, final int rangeValue,
            final int disconnectEnabler) {
        return DeviceParameterRecorder.setPxpClientParam(this, device.getAddress(), alertEnabler,
                rangeAlertEnabler, rangeType, rangeValue, disconnectEnabler);
    }

    public boolean getPxpParameters(final BluetoothDevice device, final int[] alertEnabler,
            final int[] rangeAlertEnabler,
            final int[] rangeType, final int[] rangeValue, final int[] disconnectEnabler)
            throws RemoteException {

        final DeviceParameterRecorder.DevicePxpParams pxpParams = DeviceParameterRecorder
                .getPxpClientParam(this, device.getAddress());

        if (pxpParams == null) {
            return false;
        }

        alertEnabler[0] = pxpParams.mAlertEnabler;
        rangeAlertEnabler[0] = pxpParams.mRangeAlertEnabler;
        rangeType[0] = pxpParams.mRangeType;
        rangeValue[0] = pxpParams.mRangeValue;
        disconnectEnabler[0] = pxpParams.mDisconnEnabler;
        return true;
    }

    private class ProximityProfileServiceBinder extends IProximityProfileService.Stub implements
            IProfileServiceBinder {
        private ProximityProfileService mPxpProfileService = null;

        ProximityProfileServiceBinder(final ProximityProfileService pxpService) {
            mPxpProfileService = pxpService;
        }

        @Override
        public int getPathLoss(final BluetoothDevice device)
                throws RemoteException {
            final ProximityDevice pxpDev = mPxpProfileService.mDeviceMap.get(device.getAddress());

            if (pxpDev != null) {
                final int pathLoss = pxpDev.getPathLoss();
                if (VDBG) {
                    Log.v(TAG, "getPathLoss: " + device.getAddress() + ", pathLoss = "
                            + pathLoss);
                }
                return pathLoss;
            } else {
                if (DBG) {
                    Log.d(TAG, "getPathLoss: " + device.getAddress() + " not available");
                }
                return 0;
            }
        }

        @Override
        public boolean isAlertOn(final BluetoothDevice device)
                throws RemoteException {
            final ProximityDevice pxpDev = mPxpProfileService.mDeviceMap.get(device.getAddress());

            if (pxpDev != null) {
                final boolean isNotify = pxpDev.queryIsNotifyRemote();
                if (VDBG) {
                    Log.v(TAG, "isAlertOn: " + device.getAddress() + ", isNotify:"
                            + isNotify);
                }
                return isNotify;
            } else {
                if (DBG) {
                    Log.d(TAG, "isAlertOn: " + device.getAddress() + " not available");
                }
                return false;
            }
        }

        @Override
        public boolean stopRemoteAlert(final BluetoothDevice device)
                throws RemoteException {
            final ProximityDevice pxpDev = mPxpProfileService.mDeviceMap.get(device.getAddress());

            if (pxpDev != null) {
                final boolean success = pxpDev.stopRemoteAlert();
                if (VDBG) {
                    Log.v(TAG, "stopRemoteAlert: " + device.getAddress() + ", success:" +
                            success);
                }
                return success;
            } else {
                if (DBG) {
                    Log.d(TAG, "stopRemoteAlert: " + device.getAddress() + " not available");
                }
                return false;
            }
        }

        @Override
        public boolean setPxpParameters(final BluetoothDevice device, final int alertEnabler,
                final int rangeAlertEnabler, final int rangeType, final int rangeValue,
                final int disconnectEnabler)
                throws RemoteException {
            if (DBG) {
                Log.d(TAG, "setPxpParameters: " + device.getAddress() + ", alertEnabler:" +
                        alertEnabler + ", rangeAlertEnabler:" + rangeAlertEnabler +
                        ", rangeType:" + rangeType + ", rangeValue:" + rangeValue +
                        ", disconnectEnable:" + disconnectEnabler);
            }

            if (mPxpProfileService == null) {
                Log.w(TAG, "setPxpParameters: pxp service not available");
                return false;
            }

            mPxpProfileService.setPxpParameters(device, alertEnabler, rangeAlertEnabler,
                    rangeType, rangeValue, disconnectEnabler);

            return true;
        }

        @Override
        public boolean getPxpParameters(final BluetoothDevice device, final int[] alertEnabler,
                final int[] rangeAlertEnabler, final int[] rangeType, final int[] rangeValue,
                final int[] disconnectEnabler)
                throws RemoteException {
            if (DBG) {
                Log.d(TAG, "getPxpParameters: " + device.getAddress());
            }

            if (mPxpProfileService == null) {
                Log.w(TAG, "getPxpParameters: pxp service not available");
                return false;
            }

            return mPxpProfileService.getPxpParameters(device, alertEnabler, rangeAlertEnabler,
                    rangeType, rangeValue, disconnectEnabler);
        }

        @Override
        public boolean registerStatusChangeCallback(final BluetoothDevice device,
                final IProximityProfileServiceCallback callback)
                throws RemoteException {
            final ProximityDevice pxpDev = mPxpProfileService.mDeviceMap.get(device.getAddress());

            if (pxpDev != null) {
                final boolean result = pxpDev.registerStatusChangeCallback(callback);
                if (VDBG) {
                    Log.v(TAG, "registerStatusChangeCallback: " + device.getAddress() +
                            ", result:" + result);
                }
                return result;
            } else {
                if (DBG) {
                    Log.d(TAG, "registerStatusChangeCallback: " + device.getAddress()
                            + " not available");
                }
                final ArrayList<IProximityProfileServiceCallback> callbackList =
                        mPxpProfileService.mPreRegisterCallbackMap.get(device.getAddress());

                if (callbackList == null) {
                    final ArrayList<IProximityProfileServiceCallback> createCallbackList =
                            new ArrayList<IProximityProfileServiceCallback>();
                    createCallbackList.add(callback);
                    mPxpProfileService.mPreRegisterCallbackMap.put(device.getAddress(),
                            createCallbackList);

                    if (DBG) {
                        Log.d(TAG, "registerStatusChangeCallback:"
                                + " device callbackList not available, create a new one");
                    }

                } else {
                    if (!callbackList.contains(callback)) {
                        callbackList.add(callback);
                        mPxpProfileService.mPreRegisterCallbackMap.put(device.getAddress(),
                                callbackList);
                    } else {
                        if (DBG) {
                            Log.d(TAG, "registerStatusChangeCallback: callback already existed");
                        }
                        return false;
                    }
                }

                return true;
            }
        }

        @Override
        public boolean unregisterStatusChangeCallback(final BluetoothDevice device,
                final IProximityProfileServiceCallback callback)
                throws RemoteException {
            final ProximityDevice pxpDev = mPxpProfileService.mDeviceMap.get(device.getAddress());

            if (pxpDev != null) {
                final boolean result = pxpDev.unregisterStatusChangeCallback(callback);
                if (VDBG) {
                    Log.v(TAG, "unregisterStatusChangeCallback: " + device.getAddress() +
                            ", result:" + result);
                }
                return result;
            } else {
                if (DBG) {
                    Log.d(TAG, "unregisterStatusChangeCallback: " + device.getAddress() +
                            " not available");
                }
                final ArrayList<IProximityProfileServiceCallback> callbackList =
                        mPxpProfileService.mPreRegisterCallbackMap.get(device.getAddress());

                if (callbackList == null) {
                    if (DBG) {
                        Log.d(TAG, "registerStatusChangeCallback:"
                                + " device callbackList not available");
                    }
                    return false;
                } else {
                    if (callbackList.contains(callback)) {
                        callbackList.remove(callback);
                        if (callbackList.isEmpty()) {
                            mPxpProfileService.mPreRegisterCallbackMap.remove(device.getAddress());
                            if (DBG) {
                                Log.d(TAG, "registerStatusChangeCallback:"
                                        + " callback is empty, remove it");
                            }
                        } else {
                            mPxpProfileService.mPreRegisterCallbackMap.put(device.getAddress(),
                                    callbackList);
                        }
                    } else {
                        if (DBG) {
                            Log.d(TAG, "registerStatusChangeCallback: callback does not existed");
                        }
                        return false;
                    }
                }

                return true;
            }
        }

        @Override
        public boolean cleanup() {
            return false;
        }
    }

    private class ProximityDevice {
        private static final String TAG = "[BLE][PXP]ProximityDevice";

        private final Context mContext;
        private final String mAddress;
        private boolean mInitDone = false;
        private boolean mManualDisconnect = false;
        private int mTxPower = 0;
        private int mRssi = 0;
        private int mPrevRssi = 0;
        private boolean mIsNotifyRemote = false;
        private final String mTagString;

        private final RemoteCallbackList<IProximityProfileServiceCallback> mCallbacks =
                new RemoteCallbackList<IProximityProfileServiceCallback>();

        public DeviceSetting mDeviceSetting;
        public int mAlertStatus = NO_ALERT;

        private final ProximityMessageHandler mDeviceHandler;

        private BleGattDevice mGattDevice = null;

        private boolean mTxPowerRetried = false;

        BleGattDeviceCallback mGattDeviceCB = new BleGattDeviceCallback() {
            @Override
            public void onConnectionStateChange(final BleGattDevice gattDevice,
                    final int status, final int newState) {

                if (DBG) {
                    Log.d(mTagString, "onConnectionStateChange(), status:" + status +
                            ", newState:" + newState + ", mAlertStatus:" + mAlertStatus);
                }
                // /If device is connected
                // 0: STATE_DISCONNECTED
                // 1: STATE_CONNECTING
                // 2: STATE_CONNECTED
                // 3: STATE_DISCONNECTING
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    if (BluetoothGatt.STATE_CONNECTED == newState) {
                        // Connected. Continue to SET_LINK_LOST and later actions
                        if (!mGattDevice.getServices().isEmpty()) {
                            if (mGattDevice.getService(BleGattUuid.Service.LINK_LOST) != null) {
                                if (VDBG) {
                                    Log.v(mTagString, "start SET_LINK_LOST");
                                }
                                Message outMsg = Message.obtain(mDeviceHandler, SET_LINK_LOST);
                                mDeviceHandler.sendMessage(outMsg);
                                setInitDone(true);
                                mManualDisconnect = false;

                                if (mDeviceHandler.hasMessages(DELAY_DISCONNECT_ALERT)) {
                                    mDeviceHandler.removeMessages(DELAY_DISCONNECT_ALERT);
                                }

                                outMsg = Message.obtain(mDeviceHandler,
                                        CHECK_DISCONNECT_ALERT, RECONNECT, 0);
                                mDeviceHandler.sendMessage(outMsg);

                            } else {
                                if (DBG) {
                                    Log.d(mTagString, "onConnectionStateChange()," +
                                            "remote not support link lost");
                                }
                                setInitDone(false);
                            }
                        } else {
                            if (VDBG) {
                                Log.v(mTagString, "onConnectionStateChange()," +
                                        " wait for auto service discover result");
                            }
                        }
                    } else if (BluetoothGatt.STATE_DISCONNECTING == newState) {

                        mManualDisconnect = true;

                    } else if (BluetoothGatt.STATE_DISCONNECTED == newState) {

                        if (mDeviceHandler.hasMessages(READ_RSSI)) {
                            mDeviceHandler.removeMessages(READ_RSSI);
                        }

                        // check if it is manually disconnected, set to NO_Alert instead of
                        // DISCONNECT_ALERT
                        if (getInitDone()) {
                            Message outMsg;
                            outMsg = Message.obtain(mDeviceHandler, DELAY_DISCONNECT_ALERT);
                            mDeviceHandler.sendMessageDelayed(outMsg, DISCONNECT_DELAY_TIME);
                            setInitDone(false);
                        }
                    }
                }
            }

            @Override
            public void onServicesDiscovered(final BleGattDevice gattDevice, final int status) {
                if (DBG) {
                    Log.d(mTagString, "onServicesDiscovered(), status:" + status);
                }

                if (BluetoothGatt.GATT_SUCCESS == status) {
                    // Service discovered. Continue to SET_LINK_LOST and later actions
                    final boolean isLinkLostSupported = (mGattDevice
                            .getService(BleGattUuid.Service.LINK_LOST) != null);

                    if (isLinkLostSupported && !getInitDone()) {
                        if (VDBG) {
                            Log.v(mTagString, "start SET_LINK_LOST");
                        }
                        Message outMsg = Message.obtain(mDeviceHandler, SET_LINK_LOST);
                        mDeviceHandler.sendMessage(outMsg);
                        setInitDone(true);

                        mManualDisconnect = false;

                        if (mDeviceHandler.hasMessages(DELAY_DISCONNECT_ALERT)) {
                            mDeviceHandler.removeMessages(DELAY_DISCONNECT_ALERT);
                        }

                        outMsg = Message.obtain(mDeviceHandler, CHECK_DISCONNECT_ALERT,
                                RECONNECT, 0);
                        mDeviceHandler.sendMessage(outMsg);

                    } else if (!isLinkLostSupported) {
                        Log.e(mTagString, "onServicesDiscovered(), " +
                                "remote not support link lost, shall never happen");
                        setInitDone(false);

                        if (mDeviceHandler.hasMessages(SET_RSSI_AND_CHECK_RANGE_ALERT)) {
                            mDeviceHandler.removeMessages(SET_RSSI_AND_CHECK_RANGE_ALERT);
                        }
                        if (mDeviceHandler.hasMessages(READ_RSSI)) {
                            mDeviceHandler.removeMessages(READ_RSSI);
                        }
                    }
                }
            }

        };

        // RSSITEST
        private int mRssiTotal = 0;
        private int mRssiCount = 0;
        private int mMinRssi = 0;
        private int mMaxRssi = -200;
        // RSSITEST

        BleProximityProfile.ProfileCallback mProximityClientCallback =
                new BleProximityProfile.ProfileCallback() {
                    @Override
                    public void onTxPowerRead(final int status, final int txPower,
                            final BluetoothDevice device) {

                        if (DBG) Log.d(mTagString, "onTxPowerRead: status = " + status +
                                ", txPower = " + txPower + ", device = " + device);

                        if (!getInitDone()) {
                            return;
                        }

                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            if (DBG) {
                                Log.d(mTagString, "mProximityClientCallback:" +
                                        "onTxPowerRead fail, read again");
                            }

                            if (mGattDevice != null && !mTxPowerRetried) {

                                if (mGattDevice.getService(BleGattUuid.Service.IMMEDIATE_ALERT)
                                        != null && mGattDevice
                                                .getService(BleGattUuid.Service.TX_POWER)
                                                        != null) {

                                    final BleProximityProfile pxpClient =
                                            (BleProximityProfile) mGattDevice
                                                    .asProfileInterface(BleProfile.PXP);
                                    if (pxpClient != null) {
                                        pxpClient.readTxPower();
                                    } else {
                                        Log.w(mTagString, "mProximityClientCallback:" +
                                                "onTxPowerRead fail: pxpClient is null");
                                    }
                                    mTxPowerRetried = true;
                                }
                            }
                            return;
                        }

                        if (VDBG) {
                            Log.v(mTagString, "mProximityClientCallback:onTxPowerRead: "
                                    + txPower);
                        }
                        mTxPower = txPower;
                        final Message outMsg = Message.obtain(mDeviceHandler, READ_RSSI);
                        mDeviceHandler.sendMessage(outMsg);
                    }

                    @Override
                    public void onRssiRead(final int status, final int rssi,
                            final BluetoothDevice device) {

                        if (DBG) Log.d(mTagString, "onRssiRead: status = " + status +
                                ", rssi = " + rssi + ", device = " + device);

                        if (!getInitDone()) {
                            return;
                        }

                        Message outMsg = null;

                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            if (DBG) {
                                Log.d(mTagString, "mProximityClientCallback:onRssiRead fail" +
                                        ", read again");
                            }
                            outMsg = Message.obtain(mDeviceHandler, READ_RSSI);
                            mDeviceHandler.sendMessageDelayed(outMsg, RSSI_DEALY_TIME);
                            return;
                        }

                        if (VDBG) {
                            Log.v(mTagString, "mProximityClientCallback:onRssiRead: " + rssi);
                        }

                        // RSSITEST
                        if (RSSITEST) {
                            mRssiTotal = mRssiTotal + rssi;
                            mRssiCount++;

                            if (rssi < mMinRssi) {
                                mMinRssi = rssi;
                            }

                            if (rssi > mMaxRssi) {
                                mMaxRssi = rssi;
                            }

                            if (mRssiCount == 100) {
                                if (VDBG) {
                                    Log.v(mTagString, "[RSSITEST] average:" + mRssiTotal
                                            / mRssiCount
                                            + ", min:" + mMinRssi + ", max:" + mMaxRssi);
                                }
                                mMinRssi = 0;
                                mMaxRssi = -200;
                                mRssiCount = 0;
                                mRssiTotal = 0;
                            }
                        }

                        outMsg = Message.obtain(mDeviceHandler, SET_RSSI_AND_CHECK_RANGE_ALERT,
                                rssi, 0);
                        mDeviceHandler.sendMessage(outMsg);

                        outMsg = Message.obtain(mDeviceHandler, READ_RSSI);
                        mDeviceHandler.sendMessageDelayed(outMsg, RSSI_DEALY_TIME);
                    }

                    @Override
                    public void onLinkLostAlertLevelSet(final int status,
                            final BluetoothDevice device) {

                        if (DBG) Log.d(mTagString, "onLinkLostAlertLevelSet: status = " + status +
                                ", device = " + device);

                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            if (DBG) {
                                Log.d(mTagString, "mProximityClientCallback:" +
                                        "onLinkLostAlertLevelSet fail ");
                            }
                        } else {
                            if (VDBG) {
                                Log.v(mTagString, "mProximityClientCallback:" +
                                        "onLinkLostAlertLevelSet");
                            }
                        }
                        /// M: ALPS01750628: Read Tx Power after Link Lost Alert Level is set @{
                        final Message outMsg = Message.obtain(mDeviceHandler, READ_TX_POWER);
                        mDeviceHandler.sendMessage(outMsg);
                        /// @}
                    }
                };

        // RSSI read dealy time
        private static final int RSSI_DEALY_TIME = 1500;

        // RSSI tolerance
        private static final int RSSI_TOLERANCE = 2;

        // Hander message
        private static final int START_INIT = 0;
        private static final int GET_GATT_DEVICE = 1;
        private static final int SET_LINK_LOST = 2;
        private static final int READ_RSSI = 3;
        /// M: ALPS01750628: Read Tx Power after Link Lost Alert Level is set
        private static final int READ_TX_POWER = 4;
        private static final int SET_RSSI_AND_CHECK_RANGE_ALERT = 5;
        private static final int CHECK_DISCONNECT_ALERT = 6;
        private static final int SET_MANUAL_DISCONNECT = 7;
        private static final int DEVICE_DELETED = 8;
        private static final int UPDATE_SETTING = 9;
        private static final int STOP_REMOTE_ALERT = 10;
        private static final int DELAY_DISCONNECT_ALERT = 11;

        // Alert Status
        private static final int NO_ALERT = BleProximityProfileService.STATE_NO_ALERT;
        private static final int DISCONNECTED_ALERT =
                BleProximityProfileService.STATE_DISCONNECTED_ALERT;
        private static final int IN_RANGE_ALERT =
                BleProximityProfileService.STATE_IN_RANGE_ALERT;
        private static final int OUT_RANGE_ALERT =
                BleProximityProfileService.STATE_OUT_RANGE_ALERT;

        // Connection type
        private static final int MANUAL_DISCONNECT = 0;
        private static final int ABNORMAL_DISCONNECT = 1;
        private static final int RECONNECT = 2;

        // Setting type
        public static final int PXP_SETTING = 0;

        public class DeviceSetting {
            public boolean alertEnabler;

            public boolean rangeAlertEnabler;

            public int rangeAlertInOut; // type

            public int rangeAlertDistance; // value

            public int rangeAlertThreshold;

            public boolean disconnectAlertEnabler;
        }

        public ProximityDevice(final Context context, final String address) {
            mContext = context;
            mAddress = address;

            mTagString = TAG + "(" + mAddress + ")";

            if (DBG) {
                Log.d(mTagString, "ProximityDevice :" + mAddress);
            }

            mDeviceSetting = new DeviceSetting();

            final HandlerThread thread = new HandlerThread("ProximityMessageHandler");
            thread.start();
            final Looper looper = thread.getLooper();
            mDeviceHandler = new ProximityMessageHandler(looper);

            final Message msg = Message.obtain(mDeviceHandler, START_INIT);
            mDeviceHandler.sendMessage(msg);

        }

        private void setInitDone(final boolean done) {
            if (DBG) {
                Log.d(mTagString, "setInitDone: " + mInitDone + " to " + done);
            }
            mInitDone = done;

            if (!mInitDone) {
                mRssi = 0;
                mPrevRssi = 0;
                mTxPower = 0;
                mTxPowerRetried = false;
            }
        }

        private boolean getInitDone() {
            if (DBG) {
                Log.d(mTagString, "getInitDone: " + mInitDone);
            }
            return mInitDone;
        }

        private void broadcastIntentForAlertDisplay() {
            if (DBG) {
                Log.d(mTagString, "broadcastIntentForAlertDisplay: " + mAlertStatus);
            }
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
                    .getRemoteDevice(mAddress);

            final Intent intent = new Intent(BleProximityProfileService.ACTION_ALERT_STATE_CHANGED);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            intent.putExtra(BleProximityProfileService.EXTRA_STATE, mAlertStatus);
            mContext.sendBroadcast(intent);
        }

        private final class ProximityMessageHandler extends Handler {
            private ProximityMessageHandler(final Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(final Message msg) {
                if (DBG) {
                    Log.d(mTagString, "handleMessage:" + msg.what);
                }

                switch (msg.what) {
                    case START_INIT:
                        processStartInit();
                        break;

                    case GET_GATT_DEVICE:
                        processGetGattDevice();
                        break;

                    case SET_LINK_LOST:
                        processSetLinkLost();
                        break;
                    /// M: ALPS01750628: Read Tx Power after Link Lost Alert Level is set @{
                    case READ_TX_POWER:
                        processReadTxPower();
                        break;
                    /// @}
                    case READ_RSSI:
                        processReadRssi();
                        break;

                    case SET_RSSI_AND_CHECK_RANGE_ALERT:
                        processSetRssiAndCheckRangeAlert(msg.arg1);
                        break;

                    case CHECK_DISCONNECT_ALERT:
                        if (DBG) {
                            Log.d(mTagString, "CHECK_DISCONNECT_ALERT");
                        }
                        checkDisconnectAlert(msg.arg1);
                        break;

                    case DELAY_DISCONNECT_ALERT:
                        processDelayDisconnectAlert();
                        break;

                    case SET_MANUAL_DISCONNECT:
                        mManualDisconnect = msg.arg1 == 1 ? true : false;
                        if (DBG) {
                            Log.d(mTagString, "SET_MANUAL_DISCONNECT: " + mManualDisconnect);
                        }
                        break;

                    case DEVICE_DELETED:
                        processDeviceDeleted();
                        break;

                    case UPDATE_SETTING:
                        processUpdateSetting();
                        break;

                    case STOP_REMOTE_ALERT:
                        processStopRemoteAlert();
                        break;

                    default:
                        break;

                }
            }
        }

        private void processStartInit() {
            // read database setting
            updatePxpSetting();
            // acquire pxp client instance
            Message outMsg = null;
            outMsg = Message.obtain(mDeviceHandler, GET_GATT_DEVICE);
            mDeviceHandler.sendMessageDelayed(outMsg, 500);
        }

        private void processGetGattDevice() {
            // get GATT device and wait for connect callback
            final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
                    .getRemoteDevice(mAddress);
            if (mDeviceManager != null) {
                mGattDevice = mDeviceManager.createGattDevice(mContext, device,
                        mGattDeviceCB);
                if (VDBG) {
                    Log.v(mTagString, "processGetGattDevice: get mGattDevice done:" +
                            mGattDevice);
                }
            } else {
                if (VDBG) {
                    Log.v(mTagString,
                            "processGetGattDevice: mDeviceManager not bound yet," +
                                    "start timer to wait");
                }
                Message outMsg = null;
                outMsg = Message.obtain(mDeviceHandler, GET_GATT_DEVICE);
                mDeviceHandler.sendMessageDelayed(outMsg, 500);
                return;
            }
            // we shall always wait for GattDevice to notify device Connected since the
            // auto connection will be done by framework
            if (mGattDevice != null
                    && mGattDevice.getState() == BleGattDevice.STATE_CONNECTED) {
                if (mGattDevice.getService(BleGattUuid.Service.LINK_LOST) != null) {
                    if (VDBG) {
                        Log.v(mTagString, "processGetGattDevice: device connected," +
                                "start SET_LINK_LOST");
                    }
                    Message outMsg = null;
                    outMsg = Message.obtain(mDeviceHandler, SET_LINK_LOST);
                    mDeviceHandler.sendMessage(outMsg);
                    setInitDone(true);
                } else {
                    if (VDBG) {
                        Log.v(mTagString, "processGetGattDevice: discover service");
                    }
                    mGattDevice.discoverServices();
                }
            } else {
                if (VDBG) {
                    Log.v(mTagString, "processGetGattDevice: device not connected," +
                            "wait for framework auto connection");
                }
            }

        }

        private void processSetLinkLost() {
            // Get the PXP interface
            if (mGattDevice != null) {
                final BleProximityProfile pxpClient = (BleProximityProfile) mGattDevice
                        .asProfileInterface(BleProfile.PXP);
                if (pxpClient != null) {
                    if (VDBG) {
                        Log.v(mTagString, "processSetLinkLost: register callback");
                    }
                    pxpClient.registerProfileCallback(mProximityClientCallback);

                    int level = BleProximityProfile.ALERT_LEVEL_NO;
                    synchronized (mDeviceSetting) {
                        if (mDeviceSetting.alertEnabler
                                && mDeviceSetting.disconnectAlertEnabler) {
                            level = BleProximityProfile.ALERT_LEVEL_HIGH;
                        }
                    }
                    // set link lost
                    final boolean result = pxpClient.setLinkLostAlertLevel(level);
                    if (VDBG) {
                        Log.v(mTagString, "processSetLinkLost: set link lost: " +
                                level + ", result:" + result);
                    }
                } else {
                    Log.e(mTagString, "processSetLinkLost: pxpClient is null");
                }

            } else {
                Log.e(mTagString, "processSetLinkLost: mGattDevice is null");
            }

        }
        /// M: ALPS01750628: Read Tx Power after Link Lost Alert Level is set @{
        private void processReadTxPower() {
            if (mGattDevice != null) {
                final BleProximityProfile pxpClient = (BleProximityProfile) mGattDevice
                        .asProfileInterface(BleProfile.PXP);
                if (pxpClient != null) {
                    if (mGattDevice.getService(BleGattUuid.Service.IMMEDIATE_ALERT) != null
                            && mGattDevice.getService(BleGattUuid.Service.TX_POWER)
                                != null) {
                        if (VDBG) {
                            Log.v(mTagString, "processReadTxPower: Tx Power " +
                                    "supported, read Tx Power");
                        }
                        pxpClient.readTxPower();
                        mTxPowerRetried = false;
                    } else {
                        if (DBG) {
                            Log.d(mTagString, "Tx Power or IAS not supported");
                        }
                    }
                } else {
                    Log.e(mTagString, "pxpClient is null");
                }

            } else {
                Log.e(mTagString, "mGattDevice is null");
            }
        }
        /// @}
        private void processReadRssi() {
            // Get the PXP interface
            if (mGattDevice != null) {
                final BleProximityProfile pxpClient = (BleProximityProfile) mGattDevice
                        .asProfileInterface(BleProfile.PXP);
                // read rssi
                if (DBG) {
                    Log.d(mTagString, "processReadRssi");
                }
                if (pxpClient != null) {
                    pxpClient.readRssi();
                } else {
                    Log.e(mTagString, "processReadRssi: pxpClient is null");
                }

            } else {
                Log.e(mTagString, "processReadRssi: mGattDevice is null");
            }
        }

        private void processSetRssiAndCheckRangeAlert(final int rssi) {

            if (!setRssiValue(rssi)) {
                return;
            }

            synchronized (mDeviceSetting) {

                if (DBG) {
                    Log.d(mTagString, "processSetRssiAndCheckRangeAlert: alertEnabler:"
                            + mDeviceSetting.alertEnabler
                            + ", rangeAlertEnabler:" + mDeviceSetting.rangeAlertEnabler
                            + ", rangeAlertInOut:" + mDeviceSetting.rangeAlertInOut
                            + ", rangeAlertDistance:" + mDeviceSetting.rangeAlertDistance);
                }

                if (mDeviceSetting.alertEnabler && mDeviceSetting.rangeAlertEnabler) {
                    // check if need to alert
                    final int pathLoss = mTxPower - mRssi;
                    checkRangeAlert(pathLoss);
                }

            }
        }

        private void processDelayDisconnectAlert() {
            if (DBG) {
                Log.d(mTagString, "processDelayDisconnectAlert: mInitDone:" +
                        mInitDone + ", mManualDisconnect:" + mManualDisconnect);
            }

            if (getInitDone()) {
                // reconnected during delay time
                return;
            }

            Message outMsg = null;

            if (mManualDisconnect) {
                outMsg = Message.obtain(mDeviceHandler, CHECK_DISCONNECT_ALERT,
                        MANUAL_DISCONNECT, 0);
            } else {
                outMsg = Message.obtain(mDeviceHandler, CHECK_DISCONNECT_ALERT,
                        ABNORMAL_DISCONNECT, 0);
            }

            mDeviceHandler.sendMessage(outMsg);

        }

        private void processDeviceDeleted() {
            if (DBG) {
                Log.d(mTagString, "processDeviceDeleted");
            }

            setInitDone(false);

            if (mGattDevice != null) {
                mGattDevice.close();
            }

            if (mAlertStatus != NO_ALERT) {
                mAlertStatus = NO_ALERT;
                broadcastIntentForAlertDisplay();
            }
            /// M: ALPS01838657: Quit HandlerThread if the device is deleted or PXP service is stopped. @{
            if(mDeviceHandler != null) {
                Log.d(mTagString, "Quit the HandlerThread");
                mDeviceHandler.getLooper().quit();
            }
            /// @}
        }

        private void processUpdateSetting() {
            if (DBG) {
                Log.d(mTagString, "processUpdateSetting");
            }

            boolean curLinkLost = true;
            synchronized (mDeviceSetting) {
                curLinkLost = mDeviceSetting.alertEnabler
                        && mDeviceSetting.disconnectAlertEnabler;
            }

            updatePxpSetting();

            synchronized (mDeviceSetting) {
                // set remote link lost setting
                if ((mDeviceSetting.alertEnabler && mDeviceSetting.disconnectAlertEnabler)
                        != curLinkLost) {
                    if (mGattDevice != null) {
                        final BleProximityProfile pxpClient =
                                (BleProximityProfile) mGattDevice
                                        .asProfileInterface(BleProfile.PXP);

                        int level = BleProximityProfile.ALERT_LEVEL_NO;
                        if (mDeviceSetting.alertEnabler
                                && mDeviceSetting.disconnectAlertEnabler) {
                            level = BleProximityProfile.ALERT_LEVEL_HIGH;
                        }
                        // set link lost
                        if (pxpClient != null) {
                            final boolean result = pxpClient.setLinkLostAlertLevel(level);
                            if (VDBG) {
                                Log.v(mTagString, "UPDATE_SETTING: set link lost: " +
                                        level + ", result:" + result);
                            }
                        } else {
                            Log.e(mTagString, "UPDATE_SETTING: set link lost, " +
                                    "pxpClient is null ");
                        }

                    } else {
                        Log.e(mTagString, "UPDATE_SETTING: mGattDevice is null");
                    }
                }

                // updpate local dialog / notification
                boolean statusUpdated = false;

                if (mAlertStatus == DISCONNECTED_ALERT
                        && (!mDeviceSetting.alertEnabler ||
                        !mDeviceSetting.disconnectAlertEnabler)) {
                    if (VDBG) {
                        Log.v(mTagString, "UPDATE_SETTING: DISCONNECTED_ALERT" +
                                "to NO_ALERT");
                    }
                    mAlertStatus = NO_ALERT;
                    statusUpdated = true;
                }

                if ((mAlertStatus == IN_RANGE_ALERT || mAlertStatus == OUT_RANGE_ALERT)
                        && (!mDeviceSetting.alertEnabler ||
                        !mDeviceSetting.rangeAlertEnabler)) {
                    if (VDBG) {
                        Log.v(mTagString, "UPDATE_SETTING: IN/OUT_RANGE_ALERT" +
                                "to NO_ALERT");
                    }
                    mAlertStatus = NO_ALERT;
                    statusUpdated = true;
                }

                if (statusUpdated) {
                    broadcastIntentForAlertDisplay();
                }

            }

        }

        private void processStopRemoteAlert() {
            if (mGattDevice != null && queryIsNotifyRemote()) {
                final BleFindMeProfile fmpClient = (BleFindMeProfile) mGattDevice
                        .asProfileInterface(BleProfile.FMP);
                if (VDBG) {
                    Log.v(mTagString, "STOP_REMOTE_ALERT, notify remote");
                }
                if (fmpClient != null) {
                    fmpClient.findTarget(BleFindMeProfile.ALERT_LEVEL_NO);
                } else {
                    Log.e(mTagString, "STOP_REMOTE_ALERT, pxpClient is null");
                }
                setIsNotifyRemote(false);
            } else {
                if (VDBG) {
                    Log.v(mTagString, "STOP_REMOTE_ALERT: no need to" +
                            "notify remote");
                }
            }
        }

        private void setIsNotifyRemote(final boolean isAlert) {
            if (DBG) {
                Log.d(mTagString, "setIsNotifyRemote:" + isAlert);
            }

            if (mIsNotifyRemote != isAlert) {
                mIsNotifyRemote = isAlert;

                final int count = mCallbacks.beginBroadcast();

                for (int i = 0; i < count; i++) {
                    if (VDBG) {
                        Log.v(mTagString, "Invoke: onAlertStatusChange" + i + ":" +
                                mIsNotifyRemote);
                    }
                    try {
                        mCallbacks.getBroadcastItem(i).onAlertStatusChange(mAddress,
                                mIsNotifyRemote);
                    } catch (final RemoteException e) {
                        Log.e(mTagString, e.toString());
                    }
                }
                mCallbacks.finishBroadcast();
            }
        }

        private boolean setRssiValue(final int rssi) {
            if (DBG) {
                Log.d(mTagString, "setRssiValue: " + mPrevRssi + " to " + rssi +
                        ", currrent Rssi = " + mRssi);
            }

            if (rssi == 0) {
                return false;
            } else if (mRssi == 0) {
                mRssi = rssi;
                mPrevRssi = mRssi;
                return false;
            } else if (mRssi == rssi) {
                return false;
            }

            final int absRssi = Math.abs(mPrevRssi);
            final int diff = mPrevRssi - rssi;
            final int absDiff = Math.abs(diff);

            if (VDBG) {
                Log.v(mTagString, "diff rate:" + (double) absDiff / (double) absRssi);
            }

            if ((double) absDiff / (double) absRssi <= 0.25) {
                mRssi = rssi;
                mPrevRssi = rssi;
                final int count = mCallbacks.beginBroadcast();
                for (int i = 0; i < count; i++) {
                    if (VDBG) {
                        Log.v(mTagString, "Invoke: onDistanceValueChange " + i + ": " +
                                (mTxPower - mRssi));
                    }
                    try {
                        mCallbacks.getBroadcastItem(i).onDistanceValueChange(mAddress,
                                mTxPower - mRssi);
                    } catch (final RemoteException e) {
                        Log.e(mTagString, e.toString());
                    }
                }
                mCallbacks.finishBroadcast();
            } else {
                mPrevRssi = rssi;
                return false;
            }

            return true;
        }

        private void rangeAlertNotifyUxAndInformRemote() {
            broadcastIntentForAlertDisplay();

            // notify remote
            if (mGattDevice != null) {

                BleFindMeProfile fmpClient = null;
                boolean isFmpProcessing = false;

                final BluetoothGattService service = mGattDevice
                        .getService(BleGattUuid.Service.IMMEDIATE_ALERT);

                if (service != null) {
                    final BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(BleGattUuid.Char.ALERT_LEVEL);

                    if (characteristic != null) {

                        final byte[] alertLevel = characteristic.getValue();

                        if (alertLevel != null &&
                                alertLevel[0] != (byte) BleFindMeProfile.ALERT_LEVEL_NO) {
                            isFmpProcessing = true;
                            if (VDBG) {
                                Log.v(mTagString, "rangeAlertNotifyUxAndInformRemote:" +
                                        " FMP is processing");
                            }
                        } else {
                            if (VDBG) {
                                Log.v(mTagString, "rangeAlertNotifyUxAndInformRemote:" +
                                        " FMP is not processing");
                            }
                        }
                    } else {
                        Log.w(mTagString, "rangeAlertNotifyUxAndInformRemote:" +
                                "alert level Char not available");
                    }
                } else {
                    Log.w(mTagString, "rangeAlertNotifyUxAndInformRemote:" +
                            "IAS not available");
                }

                fmpClient = (BleFindMeProfile) mGattDevice
                        .asProfileInterface(BleProfile.FMP);

                if (mAlertStatus == NO_ALERT && queryIsNotifyRemote()) {
                    if (fmpClient != null) {
                        fmpClient.findTarget(BleFindMeProfile.ALERT_LEVEL_NO);
                        setIsNotifyRemote(false);
                    } else {
                        Log.w(mTagString, "rangeAlertNotifyUxAndInformRemote:" +
                                "fmpClient not available");
                    }
                } else if (mAlertStatus != NO_ALERT && !isFmpProcessing) {
                    if (fmpClient != null) {
                        fmpClient.findTarget(BleFindMeProfile.ALERT_LEVEL_HIGH);
                        setIsNotifyRemote(true);
                    } else {
                        Log.w(mTagString, "rangeAlertNotifyUxAndInformRemote:" +
                                "fmpClient not available");
                    }
                }
            } else {
                Log.e(mTagString, "rangeAlertNotifyUxAndInformRemote: mGattDevice is null");
            }

        }

        private void checkRangeAlert(final int pathLoss) {
            if (DBG) {
                Log.d(mTagString, "checkRangeAlert: pathLoss = " + pathLoss + ", threshold = "
                        + mDeviceSetting.rangeAlertThreshold + ", current AlertStatus = "
                        + mAlertStatus);
            }

            boolean statusUpdated = false;

            switch (mDeviceSetting.rangeAlertInOut) {
                case BleProximityProfileService.RANGE_ALERT_TYPE_IN:
                    if (pathLoss <= mDeviceSetting.rangeAlertThreshold - RSSI_TOLERANCE) {
                        if (mAlertStatus != IN_RANGE_ALERT) {
                            mAlertStatus = IN_RANGE_ALERT;
                            statusUpdated = true;
                        }
                    } else if (pathLoss > mDeviceSetting.rangeAlertThreshold + RSSI_TOLERANCE) {
                        if (mAlertStatus != NO_ALERT) {
                            mAlertStatus = NO_ALERT;
                            statusUpdated = true;
                        }
                    }
                    break;

                case BleProximityProfileService.RANGE_ALERT_TYPE_OUT:
                    if (pathLoss >= mDeviceSetting.rangeAlertThreshold + RSSI_TOLERANCE) {
                        if (mAlertStatus != OUT_RANGE_ALERT) {
                            mAlertStatus = OUT_RANGE_ALERT;
                            statusUpdated = true;
                        }
                    } else if (pathLoss < mDeviceSetting.rangeAlertThreshold - RSSI_TOLERANCE) {
                        if (mAlertStatus != NO_ALERT) {
                            mAlertStatus = NO_ALERT;
                            statusUpdated = true;
                        }
                    }
                    break;

                default:
                    break;
            }

            if (VDBG) {
                Log.v(mTagString, "checkRangeAlert, statusUpdated: " + statusUpdated);
            }

            if (statusUpdated) {
                rangeAlertNotifyUxAndInformRemote();
            }

        }

        private void checkDisconnectAlert(final int disconnectType) {
            synchronized (mDeviceSetting) {

                if (DBG) {
                    Log.d(mTagString, "checkDisconnectAlert: type " + disconnectType);
                }

                final int prevAlertStatus = mAlertStatus;

                if (disconnectType == MANUAL_DISCONNECT || disconnectType == RECONNECT) {
                    mAlertStatus = NO_ALERT;
                } else {
                    if (mDeviceSetting.alertEnabler && mDeviceSetting.disconnectAlertEnabler) {
                        mAlertStatus = DISCONNECTED_ALERT;
                    } else {
                        mAlertStatus = NO_ALERT;
                    }
                }

                if (mAlertStatus != prevAlertStatus) {
                    if (VDBG) {
                        Log.v(mTagString, "checkDisconnectAlert: send to update dialog : " +
                                mAddress);
                    }
                    broadcastIntentForAlertDisplay();
                }

                setIsNotifyRemote(false);

            }
        }

        private void updatePxpSetting() {
            if (DBG) {
                Log.d(mTagString, "updatePxpSetting");
            }

            final DeviceParameterRecorder.DevicePxpParams pxpParams =
                    DeviceParameterRecorder.getPxpClientParam(mContext, mAddress);

            if (pxpParams == null) {
                Log.w(mTagString, "updatePxpSetting: failed to get parameters of  :" + mAddress);
                return;
            }

            synchronized (mDeviceSetting) {
                mDeviceSetting.alertEnabler = pxpParams.mAlertEnabler == 1 ? true : false;
                mDeviceSetting.rangeAlertEnabler =
                        pxpParams.mRangeAlertEnabler == 1 ? true : false;
                mDeviceSetting.rangeAlertInOut = pxpParams.mRangeType;
                mDeviceSetting.rangeAlertDistance = pxpParams.mRangeValue;

                final int THRESHOLD_NEAR = BleProximityProfileService.getRangeAlertThreshold
                        (BleProximityProfileService.RANGE_ALERT_RANGE_NEAR);
                final int THRESHOLD_MIDDLE = BleProximityProfileService.getRangeAlertThreshold
                        (BleProximityProfileService.RANGE_ALERT_RANGE_MIDDLE);
                final int THRESHOLD_FAR = BleProximityProfileService.getRangeAlertThreshold
                        (BleProximityProfileService.RANGE_ALERT_RANGE_FAR);

                switch (mDeviceSetting.rangeAlertDistance) {
                    case BleProximityProfileService.RANGE_ALERT_RANGE_NEAR:
                        mDeviceSetting.rangeAlertThreshold = THRESHOLD_NEAR;
                        break;
                    case BleProximityProfileService.RANGE_ALERT_RANGE_MIDDLE:
                        mDeviceSetting.rangeAlertThreshold = THRESHOLD_MIDDLE;
                        break;
                    case BleProximityProfileService.RANGE_ALERT_RANGE_FAR:
                        mDeviceSetting.rangeAlertThreshold = THRESHOLD_FAR;
                        break;
                    default:
                        Log.w(mTagString, "updatePxpSetting: invalid AlertDistance :"
                                + mDeviceSetting.rangeAlertDistance);
                        mDeviceSetting.rangeAlertDistance =
                                BleProximityProfileService.RANGE_ALERT_RANGE_NEAR;
                        mDeviceSetting.rangeAlertThreshold = THRESHOLD_NEAR;
                        break;
                }

                mDeviceSetting.disconnectAlertEnabler =
                        pxpParams.mDisconnEnabler == 1 ? true : false;
            }
        }

        public void setManualDisconnect(final boolean manual) {
            if (DBG) {
                Log.d(mTagString, "setManualDisconnect:" + manual);
            }

            final Message msg = Message.obtain(mDeviceHandler, SET_MANUAL_DISCONNECT,
                    manual ? 1 : 0, 0);
            mDeviceHandler.sendMessage(msg);

            if (mDeviceHandler.hasMessages(READ_RSSI)) {
                mDeviceHandler.removeMessages(READ_RSSI);
            }
        }

        public void deviceDeleted() {
            if (DBG) {
                Log.d(mTagString, "deviceDeleted");
            }
            /// M: ALPS01838657: Quit HandlerThread if the device is deleted or PXP service is stopped. @{
            if (mDeviceHandler.hasMessages(READ_RSSI)) {
                mDeviceHandler.removeMessages(READ_RSSI);
            }

            final Message msg = Message.obtain(mDeviceHandler, DEVICE_DELETED);
            mDeviceHandler.sendMessage(msg);
            /// @}
        }

        public int getPathLoss() {
            if (DBG) {
                Log.d(mTagString, "getPathLoss, TxPower:" + mTxPower + ", Rssi:" + mRssi);
            }
            return mTxPower - mRssi;
        }

        public boolean queryIsNotifyRemote() {
            if (DBG) {
                Log.d(mTagString, "queryIsNotifyRemote:" + mIsNotifyRemote);
            }
            return mIsNotifyRemote;
        }

        public boolean stopRemoteAlert() {
            if (DBG) {
                Log.d(mTagString, "stopRemoteAlert");
            }
            final Message msg = Message.obtain(mDeviceHandler, STOP_REMOTE_ALERT);
            mDeviceHandler.sendMessage(msg);
            return true;
        }

        public boolean registerStatusChangeCallback(final IProximityProfileServiceCallback callback) {
            final boolean result = mCallbacks.register(callback);
            if (DBG) {
                Log.d(mTagString, "registerStatusChangeCallback: " + callback +
                        ", result:" + result);
            }
            return result;
        }

        public boolean unregisterStatusChangeCallback(
                final IProximityProfileServiceCallback callback) {
            final boolean result = mCallbacks.unregister(callback);
            if (DBG) {
                Log.d(mTagString, "unregisterStatusChangeCallback: " + callback +
                        ", result:" + result);
            }
            return result;
        }

        public void updateSetting(final int type) {
            if (DBG) {
                Log.d(mTagString, "updateSetting, type:" + type);
            }

            final Message msg = Message.obtain(mDeviceHandler, UPDATE_SETTING, type, 0);
            mDeviceHandler.sendMessage(msg);
        }

    }

}
