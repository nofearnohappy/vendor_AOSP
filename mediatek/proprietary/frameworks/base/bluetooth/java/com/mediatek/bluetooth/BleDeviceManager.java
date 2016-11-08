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

package com.mediatek.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.bluetooth.BleGattDevice.BleGattDeviceCallback;

/**
 * Public API for the GATT-based BLE Profiles
 *
 * <p>
 * This class is used to do device-specific operations with Bluetooth Smart devices
 *
 * @hide
 */

public class BleDeviceManager {

    private static final String TAG = "BleDeviceManager";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private Context mContext;
    private DeviceManagerListener mDeviceManagerListener;
    private IBleDeviceManager mService;

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
                @Override
                public void onBluetoothStateChange(boolean up) {
                    if (DBG) Log.d(TAG, "onBluetoothStateChange: up=" + up);

                    if (up) {
                        synchronized (mConnection) {
                            try {
                                if (mService == null) {
                                    if (VDBG) Log.v(TAG, "Binding service...");
                                    doBind();
                                }
                            } catch (SecurityException re) {
                                Log.e(TAG, "", re);
                            }
                        }
                    }
                }
            };

    // Callback for BleClientManager
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) Log.d(TAG, "Proxy object connected");

            mService = IBleDeviceManager.Stub.asInterface(service);

            if (mDeviceManagerListener != null) {
                mDeviceManagerListener.onServiceConnected(BleDeviceManager.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) Log.d(TAG, "Proxy object disconnected");

            mService = null;

            if (mDeviceManagerListener != null) {
                mDeviceManagerListener.onServiceDisconnected();
            }
        }
    };

    /* package */BleDeviceManager(Context context, DeviceManagerListener l) {
        if (VDBG) Log.v(TAG, "BleDeviceManager created. instance = " + this);

        this.mContext = context;
        this.mDeviceManagerListener = l;

        // Register for BT on/off
        IBinder mgrBinder = ServiceManager.getService(BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE);

        if (mgrBinder != null) {
            IBluetoothManager managerService = IBluetoothManager.Stub.asInterface(mgrBinder);
            try {
                managerService.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }

        doBind();
    }

    boolean doBind() {
        // Try to bind the BleClientManagerService to communicate with the remote device
        Intent intent = new Intent(IBleDeviceManager.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindService(intent, mConnection, 0)) {
            Log.e(TAG, "Could not bind to BleDeviceManagerService with " + intent);
            return false;
        }
        return true;
    }

    /**
     * Create and return a BLE GATT device
     * <p>
     * Callers need to invoke {@link BleGattDevice#open} explicitly to start the connection
     *
     * @return BleGattDevice
     *
     * @internal
     */
    public BleGattDevice createGattDevice(Context ctx, BluetoothDevice device,
            BleGattDeviceCallback clientCB) {
        if (VDBG) Log.v(TAG, "createGattDevice() instance = " + this);

        if (ctx == null) {
            return null;
        }

        if (mService == null) {
            Log.w(TAG, "Proxy not attached to service");
            return null;
        }

        BleGattDevice gattDevice = new BleGattDevice(ctx, this.mService, device, clientCB);

        if (!gattDevice.startListen()) {
            return null;
        }

        return gattDevice;
    }

    /**
     * @internal
     */
    public void addGattDevice(BluetoothDevice device) {
        if (mService == null) return;

        try {
            mService.addGattDevice(device);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "", e);
        }
    }

    /**
     * @internal
     */
    public void deleteGattDevice(BluetoothDevice device) {
        if (mService == null) return;

        try {
            mService.deleteGattDevice(device);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "", e);
        }
    }

    /* package */void close() {
        if (DBG) Log.d(TAG, "close()");

        synchronized (mConnection) {
            if (mService != null) {
                try {
                    mService = null;
                    mContext.unbindService(mConnection);
                } catch (Exception re) {
                    Log.e(TAG, "", re);
                }
            }
        }

        this.mDeviceManagerListener = null;
    }

    /**
     * An interface for notifying BleDeviceManager IPC clients when they have been connected or
     * disconnected to the service.
     */
    public interface DeviceManagerListener {
        /**
         * Called to notify the client when the proxy object has been connected to the service.
         */
        void onServiceConnected(BleDeviceManager proxy);

        /**
         * Called to notify the client that this proxy object has been disconnected from the
         * service.
         */
        void onServiceDisconnected();
    }
}
