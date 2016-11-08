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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.blemanager.common.CachedBleDevice;
import com.mediatek.blemanager.common.CachedBleDeviceManager;
import com.mediatek.blemanager.common.LocalBleManager;
import com.mediatek.blemanager.provider.BleConstants;

public class ConnectAction {
    private static final String TAG = BleConstants.COMMON_TAG + "[ConnectAction]";

    private static final int CONNECT_TIMEOUT = 30 * 1000; // 30s

    private static final int CONNECT_ACTION = 1;
    private static final int DISCONNECT_ACTION = 2;
    private static final int CONNECT_TIMEOUT_OVER = 3;
    private static final int DIALOG_DISMISS = 4;

    private boolean mShowDialog = true;

    private Activity mActivity;
    private LocalBleManager mLocalBleManager;
    private CachedBleDeviceManager mCachedBleDeviceManager;

    public ConnectAction(Activity activity) {
        Log.i(TAG, "[ConnectAction]new...");
        if (activity == null) {
            throw new IllegalArgumentException("[ConnectAction]activity is null");
        }
        mActivity = activity;
        mLocalBleManager = LocalBleManager.getInstance(mActivity);
        mCachedBleDeviceManager = CachedBleDeviceManager.getInstance();

    }

    public void connect(BluetoothDevice device, int localIndex, boolean showDialog) {
        if (device == null) {
            Log.w(TAG, "[connec] device is null");
            return;
        }
        if (localIndex < 0) {
            Log.w(TAG, "[connec] localIndex < 0,localIndex = " + localIndex);
            return;
        }
        
        Log.w(TAG, "[connec]...");
        mHandler.removeMessages(CONNECT_ACTION);
        mHandler.removeMessages(DISCONNECT_ACTION);
        mHandler.removeMessages(CONNECT_TIMEOUT_OVER);

        mShowDialog = showDialog;

        Message msg = mHandler.obtainMessage();
        msg.what = CONNECT_ACTION;
        msg.obj = device;
        msg.arg2 = localIndex;
        mHandler.sendMessage(msg);
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
            case CONNECT_ACTION:
                Log.d(TAG, "[handleMessage] handle CONNECT_ACTION");
                BluetoothDevice device1 = (BluetoothDevice) msg.obj;
                doConnectAction(device1, msg.arg2);
                break;

            case DISCONNECT_ACTION:
                Log.d(TAG, "[handleMessage] handle DISCONNECT_ACTION");
                BluetoothDevice device2 = (BluetoothDevice) msg.obj;
                doDisconnectAction(device2);
                mHandler.removeMessages(CONNECT_TIMEOUT_OVER);
                break;

            case CONNECT_TIMEOUT_OVER:
                Log.d(TAG, "[handleMessage] handle CONNECT_TIMEOUT_OVER");
                BluetoothDevice device3 = (BluetoothDevice) msg.obj;
                doDisconnectAction(device3);
                // do show a toast to notify user connect failed
                String devicename = null;
                CachedBleDevice cachedDevice = mCachedBleDeviceManager
                        .findDevice(device3);
                if (cachedDevice == null) {
                    devicename = device3.getName();
                } else {
                    devicename = cachedDevice.getDeviceName();
                }
                if (devicename != null) {
                    //Toast.makeText(mActivity, "Failed to connect " + devicename, Toast.LENGTH_SHORT)
                    //        .show();
                }
                break;

            case DIALOG_DISMISS:
                Log.d(TAG, "[handleMessage] handle DIALOG_DISMISS");
                ConnectProgressAlertDialog.dismiss();
                break;

            default:
                Log.w(TAG, "[handleMessage] unknown id");
                return;
            }
        }

    };

    private LocalBleManager.DeviceConnectionChangeListener mConnectionListener = 
            new LocalBleManager.DeviceConnectionChangeListener() {

        @Override
        public void onDeviceConnectionStateChange(BluetoothDevice device, int state) {
            if (device == null) {
                Log.w(TAG, "[onDeviceConnectionStateChange] device is null");
                return;
            }
            Log.d(TAG, "[onDeviceConnectionStateChange] state : " + state);
            if (state == BluetoothGatt.STATE_CONNECTED || state == BluetoothGatt.STATE_DISCONNECTED) {
                Message msg = mHandler.obtainMessage();
                msg.what = DIALOG_DISMISS;
                mHandler.sendMessage(msg);
                mHandler.removeMessages(CONNECT_TIMEOUT_OVER);
                Log.d(TAG, "[onDeviceConnectionStateChange] unregister.");
                mLocalBleManager.unregisterConnectionStateChangeListener(this);
            }
        }

    };

    private void doConnectAction(final BluetoothDevice device, final int localIndex) {
        Log.d(TAG, "[doConnectAction] mLocationIndex : " + localIndex);
        if (localIndex < 0) {
            return;
        }
        mLocalBleManager.registerConnectionStateChangeListener(mConnectionListener);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "[doConnectAction] start to connect gatt device");
                mLocalBleManager.connectGattDevice(device, false, localIndex);
            }
        };
        new Thread(r).start();

        if (mShowDialog) {
            ConnectProgressAlertDialog.show(mActivity, /* "connecting...", */
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            Log.d(TAG, "[doConnectAction],onClick.");
                            Message msg = mHandler.obtainMessage();
                            msg.what = DISCONNECT_ACTION;
                            msg.obj = device;
                            mHandler.sendMessageDelayed(msg, 0);
                        }
                    });
        }

        Message msg = mHandler.obtainMessage();
        msg.what = CONNECT_TIMEOUT_OVER;
        msg.obj = device;
        mHandler.sendMessageDelayed(msg, CONNECT_TIMEOUT);
    }

    private void doDisconnectAction(final BluetoothDevice device) {
        if (mShowDialog) {
            ConnectProgressAlertDialog.dismiss();
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "[doDisconnectAction] start to disconnect gatt device");
                mLocalBleManager.disconnectGattDevice(device);
            }
        };
        new Thread(r).start();
        mHandler.removeMessages(CONNECT_TIMEOUT_OVER);
    }
}
