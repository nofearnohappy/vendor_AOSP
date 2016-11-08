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

import com.mediatek.bluetoothle.tip.ITimeProfileService;

/**
 * Implement BleProfileService for time profile.
 * 
 * @hide
 */
public class BleTimeProfileService extends BleProfileService {
    private static final String TAG = "BleTimeProfileService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private final Context mContext;
    private ProfileServiceListener mServiceListener;
    private ITimeProfileService mService;

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback
            = new IBluetoothStateChangeCallback.Stub() {
        public void onBluetoothStateChange(final boolean up) {
            if (DBG) Log.d(TAG, "onBluetoothStateChange: up = " + up);
            if (up) {
                synchronized (mConnection) {
                    try {
                        if (mService == null) {
                            if (VDBG) Log.v(TAG, "Binding service...");
                            doBind();
                        }
                    } catch (final Exception re) {
                        Log.e(TAG, "", re);
                    }
                }
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            if (VDBG) Log.v(TAG, "Proxy object connected");
            mService = ITimeProfileService.Stub.asInterface(service);
            if (null != mServiceListener) {
                mServiceListener.onServiceConnected(BleProfile.TIP,
                        BleTimeProfileService.this);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (VDBG) Log.v(TAG, "Proxy object disconnected");
            mService = null;
            if (null != mServiceListener) {
                mServiceListener.onServiceDisconnected(BleProfile.TIP);
            }
        }
    };

    /* package */BleTimeProfileService(final Context ctxt, final ProfileServiceListener listener) {
        mContext = ctxt;
        mServiceListener = listener;

        final IBinder mBinder = ServiceManager
                .getService(BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE);
        if (null != mBinder) {
            final IBluetoothManager mgr = IBluetoothManager.Stub.asInterface(mBinder);
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (final RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        doBind();
    }

    /* package */ final void close() {
        final IBinder mBinder = ServiceManager
                .getService(BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE);
        if (null != mBinder) {
            final IBluetoothManager mgr = IBluetoothManager.Stub.asInterface(mBinder);
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (final RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        synchronized (mConnection) {
            if (mService != null) {
                try {
                    mService = null;
                    mContext.unbindService(mConnection);
                } catch (final Exception e) {
                    Log.e(TAG, "", e);
                }
            }
        }
        mServiceListener = null;
    }

    final boolean doBind() {
        if (VDBG) Log.v(TAG, "doBind");
        final Intent intent = new Intent(ITimeProfileService.class.getName());
        intent.setClassName("com.mediatek.bluetoothle",
                "com.mediatek.bluetoothle" + ".tip.TipServerService");
        if (!mContext.bindService(intent, mConnection, 0)) {
            Log.e(TAG, "Could not bind to TipServerService with " + intent);
            return false;
        }
        return true;
    }

    /**
     * Public API for notify time to remote GATT device.
     *
     * @param time the time we want to notify to remote GATT device
     */
    public final void notifyTime(final long time) {
        try {
            if (DBG) Log.d(TAG, "notifyTime: time = " + time);
            mService.notifyTime(time);
        } catch (final RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }
}
