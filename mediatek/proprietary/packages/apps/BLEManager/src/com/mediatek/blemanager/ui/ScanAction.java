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
package com.mediatek.blemanager.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.mediatek.blemanager.R;
import com.mediatek.blemanager.common.BluetoothCallback;
import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;

import java.util.ArrayList;

public class ScanAction {
    private static final String TAG = BleConstants.COMMON_TAG + "[ScanAction]";

    public static final int DEVICE_TYPE_LE = 2;

    private static final int SCANNING_DEVICE_OVERTIME = 60 * 1000; // 60s
    //private static final int CONNECT_DEVICE_OVERTIME = 30 * 1000; // 30s

    private static final int START_SCANNING_DEVICE_FLAG = 10;
    private static final int STOP_SCANNING_DEVICE_FLAG = 20;
    private static final int SCANNING_DEVICE_OVER_DELAY_FLAG = 30;
    private static final int CONNECT_DEVICE_FLAG = 40;
    // private static final int CONNECT_DEVICE_OVER_DELAY_FLAG = 50;
    // private static final int DISCONNECT_DEVICE_FLAG = 60;
    private static final int SCAN_DEVICE_ADD_FLAG = 70;
    private static final int SCAN_DEVICE_REMOVE_FLAG = 80;

    private static final int UPDATE_UI_FLAG = 100;
    private static final int DISMISS_DELETE_DEVICE_FLAG = 110;
    private static final int DELETE_DEVICE_FLAG = 120;

    private int mMaxiumConnectedCount = 20; // for2D
    private int mLocationIndex;
    private int mCount = 0;

    private static final String PERSIST_KEY = "persist.bt.lemaxdevice";

    private ArrayList<BluetoothDevice> mScannedDeviceList;
    private ArrayList<BluetoothDevice> mConnectedDeviceList;

    private LocalBleManager mLocalBleManager;
    private CachedBleDeviceManager mCachedBleDeviceManager;
    private BluetoothManager mBluetoothManager;

    private Activity mActivity;
    private ConnectAction mConnectAction;

    public ScanAction(Activity activity) {
        Log.i(TAG, "[ScanAction] new...");
        if (activity == null) {
            throw new IllegalArgumentException("[ScanAction] activity is null");
        }
        mActivity = activity;
        mScannedDeviceList = new ArrayList<BluetoothDevice>();
        mConnectedDeviceList = new ArrayList<BluetoothDevice>();

        mConnectAction = new ConnectAction(mActivity);

        mLocalBleManager = LocalBleManager.getInstance(mActivity);
        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) (mActivity
                    .getSystemService(Context.BLUETOOTH_SERVICE));
        }

        // mLocalManager.registerBluetoothLEScanStateCallback(mScannedCallback);
        // mLocalManager.registerNameChangeListener(mNameChangeListener);

    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "[handleMessage] msg.what = " +  msg.what);
            switch ( msg.what) {
            case START_SCANNING_DEVICE_FLAG:
                showScanDialog();
                doStartScanAction();
                sendHandlerMessage(SCANNING_DEVICE_OVER_DELAY_FLAG, SCANNING_DEVICE_OVERTIME);
                break;

            case STOP_SCANNING_DEVICE_FLAG:
                doStopScanAction();
                mHandler.removeMessages(SCANNING_DEVICE_OVER_DELAY_FLAG);
                ScanDeviceAlertDialog.dismiss();
                break;

            case SCANNING_DEVICE_OVER_DELAY_FLAG:
                doStopScanAction();
                if (mScannedDeviceList.size() == 0) {
                    mScannedDeviceList.clear();
                    ScanDeviceAlertDialog.notifyUi();
                    ScanDeviceAlertDialog.dismiss();
                } else {
                    ScanDeviceAlertDialog.hideProgressBar();
                }
                mHandler.removeMessages(SCANNING_DEVICE_OVER_DELAY_FLAG);
                break;

            case CONNECT_DEVICE_FLAG:
                mHandler.removeMessages(SCANNING_DEVICE_OVER_DELAY_FLAG);
                BluetoothDevice device = (BluetoothDevice) msg.obj;
                doConnectAction(device);
                break;

            case UPDATE_UI_FLAG:
                break;

            case SCAN_DEVICE_ADD_FLAG:
                break;

            case SCAN_DEVICE_REMOVE_FLAG:
                break;

            case DISMISS_DELETE_DEVICE_FLAG:
                ScanDeviceAlertDialog.dismiss();
                break;

            case DELETE_DEVICE_FLAG:
                BluetoothDevice deleteDevice = (BluetoothDevice) msg.obj;
                if (deleteDevice != null) {
                    Log.d(TAG, "[handleMessage]from delete dialog,enter delete operation");
                    mLocalBleManager.disconnectGattDevice(deleteDevice);
                    deleteDevice.removeBond();
                    CachedBleDevice cacheDevice = mCachedBleDeviceManager.findDevice(deleteDevice);
                    if (cacheDevice != null) {
                        Log.d(TAG, "[handleMessage]remove delete device from cache");
                        mCachedBleDeviceManager.removeDevice(cacheDevice);
                    }
                }
                break;

            default:
                break;
            }
        }

    };

    private void showScanDialog() {
        Log.d(TAG, "[showScanDialog]...");
        mScannedDeviceList.clear();
        mCount = 0;

        ArrayList<BluetoothDevice> deviceListInManager = new ArrayList<BluetoothDevice>();
        for (CachedBleDevice cachedevice : mCachedBleDeviceManager.getCachedDevicesCopy()) {
            if (cachedevice != null) {
                deviceListInManager.add(cachedevice.getDevice());
            }
        }
        for (BluetoothDevice connectedDevice : mBluetoothManager
                .getConnectedDevices(BluetoothProfile.GATT)) {
            if ((deviceListInManager != null) && (!deviceListInManager.contains(connectedDevice))) {
                mScannedDeviceList.add(connectedDevice);
                updateScanDialog(UPDATE_UI_FLAG, connectedDevice);
            }
            mCount++;
        }
        Log.d(TAG, "[showScanDialog]count=" + mCount);
        for (BluetoothDevice bondedDevice : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if ((!mScannedDeviceList.contains(bondedDevice))
                    && (bondedDevice.getType() == DEVICE_TYPE_LE)) {
                mScannedDeviceList.add(bondedDevice);
                updateScanDialog(UPDATE_UI_FLAG, bondedDevice);
            }
        }
        ScanDeviceAlertDialog.show(ScanDeviceAlertDialog.SCAN, mActivity,
                mScannedDeviceList, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendHandlerMessage(STOP_SCANNING_DEVICE_FLAG, 0);
                    }
                }, new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        // do connect action
                        BluetoothDevice device = mScannedDeviceList.get(arg2);
                        Log.d(TAG, "[onItemClick] device name: " + device.getName());
                        Message msg = mHandler.obtainMessage();
                        if (isNeedConnect(device)) {
                            Log.d(TAG, "[showScanDialog]isNeedConnect is true ");
                            msg.what = CONNECT_DEVICE_FLAG;
                            msg.obj = device;
                            mHandler.sendMessage(msg);
                            mHandler.removeMessages(SCANNING_DEVICE_OVER_DELAY_FLAG);
                        } else {
                            showDeleteDeviceDialog();
                        }
                    }
                });
    }

    private void showDeleteDeviceDialog() {
        Log.d(TAG, "[showDeleteDeviceDialog]...");
        mConnectedDeviceList.clear();

        for (BluetoothDevice connectedDevice : mBluetoothManager
                .getConnectedDevices(BluetoothProfile.GATT)) {
            mConnectedDeviceList.add(connectedDevice);
            updateScanDialog(UPDATE_UI_FLAG, connectedDevice);
        }

        ScanDeviceAlertDialog.show(ScanDeviceAlertDialog.DELETE, mActivity,
                mConnectedDeviceList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendHandlerMessage(DISMISS_DELETE_DEVICE_FLAG, 0);
                    }
                }, new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        BluetoothDevice device = mConnectedDeviceList.get(arg2);
                        Log.d(TAG, "[onItemClick] device name: " + device.getName());
                        final Message msg = mHandler.obtainMessage();
                        msg.what = DELETE_DEVICE_FLAG;
                        msg.obj = device;
                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                        builder.setTitle(R.string.delete_text);
                        builder.setMessage(R.string.device_delete_dialog_message);
                        builder.setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mHandler.sendMessage(msg);
                                        sendHandlerMessage(DISMISS_DELETE_DEVICE_FLAG, 0);
                                    }
                                });
                        builder.setNegativeButton(R.string.no, null);
                        builder.create().show();

                    }
                });

    }

    private boolean isNeedConnect(BluetoothDevice device) {
        int receriveCount = SystemProperties.getInt(PERSIST_KEY, 20); // for2D
        //if (receriveCount > 0 && receriveCount < 5) {
        mMaxiumConnectedCount = receriveCount;
        //}
        if ((mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED)
                || (mCount < mMaxiumConnectedCount)) {
            Log.d(TAG, "[isNeedConnect]mMaxiumConnectedCount=" + mMaxiumConnectedCount + ",mCount = " + mCount);
            return true;
        }
        return false;
    }

    private void doStartScanAction() {
        mLocalBleManager.registerBleDeviceScannedStateCallback(mScannedCallback);
        mLocalBleManager.registerNameChangeListener(mNameChangeListener);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mLocalBleManager.startLEScan(0);
            }
        };
        new Thread(r).start();
    }

    private void doStopScanAction() {
        mLocalBleManager.unregisterScannedStateCallback(mScannedCallback);
        mLocalBleManager.unregisterNameChangeListener(mNameChangeListener);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mLocalBleManager.stopLEScan();
            }
        };
        new Thread(r).start();
    }

    private void doConnectAction(final BluetoothDevice device) {
        ScanDeviceAlertDialog.dismiss();
        sendHandlerMessage(STOP_SCANNING_DEVICE_FLAG, 0);
        mConnectAction.connect(device, mLocationIndex, true);
    }

    private void sendHandlerMessage(int what, long delayTime) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        mHandler.sendMessageDelayed(msg, delayTime);
    }

    private void updateScanDialog(final int id, final BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "[updateScanDialog] device is null,return.");
            return;
        }
        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "[updateScanDialog]run,id = " + id);
                switch (id) {
                case UPDATE_UI_FLAG:
                    break;

                case SCAN_DEVICE_ADD_FLAG:
                    if (!mScannedDeviceList.contains(device)) {
                        if (mCachedBleDeviceManager.findDevice(device) == null) {
                            Log.d(TAG, "[updateScanDialog] call to add device");
                            mScannedDeviceList.add(device);
                        }
                    }
                    break;

                case SCAN_DEVICE_REMOVE_FLAG:
                    if (!mScannedDeviceList.contains(device)) {
                        Log.d(TAG, "[updateScanDialog] NOT contained in the list");
                        return;
                    }
                    mScannedDeviceList.remove(device);
                    break;

                default:
                    Log.d(TAG, "[updateScanDialog] unrecongnized id");
                    return;
                }
                Log.d(TAG, "[updateScanDialog] call to notify scan dialog");
                ScanDeviceAlertDialog.notifyUi();
            }

        });
    }

    private BluetoothCallback.BleDeviceScanned mScannedCallback = new BluetoothCallback.BleDeviceScanned() {

        @Override
        public void onScannedBleDeviceRemoved(BluetoothDevice device) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onScannedBleDeviceRemoved enter");
            // Message msg = mHandler.obtainMessage();
            // msg.what = SCAN_DEVICE_REMOVE_FLAG;
            // msg.obj = device;
            // mHandler.sendMessage(msg);
            updateScanDialog(SCAN_DEVICE_REMOVE_FLAG, device);
        }

        @Override
        public void onScannedBleDeviceAdded(BluetoothDevice device) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onScannedBleDeviceAdded enter");
            // Message msg = mHandler.obtainMessage();
            // msg.what = SCAN_DEVICE_ADD_FLAG;
            // msg.obj = device;
            // mHandler.sendMessage(msg);
            updateScanDialog(SCAN_DEVICE_ADD_FLAG, device);
        }
    };

    private LocalBleManager.DeviceNameChangeListener mNameChangeListener = new LocalBleManager.DeviceNameChangeListener() {
        @Override
        public void onDeviceNameChange(BluetoothDevice device, String name) {
            // TODO Auto-generated method stub
            Log.d(TAG, "[onDeviceNameChange] device : " + device.getAddress() + ", name : " + name);
            // sendHandlerMessage(UPDATE_UI_FLAG, 0);
            updateScanDialog(UPDATE_UI_FLAG, device);
        }
    };

    public void doScanAction(int localIndex) {
        mLocationIndex = localIndex;
        sendHandlerMessage(START_SCANNING_DEVICE_FLAG, 0);
    }

}
