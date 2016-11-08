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

package com.mediatek.bluetoothle.bleservice;

import android.bluetooth.BluetoothDevice;
import android.os.RemoteCallbackList;
import android.util.Log;
import android.util.SparseArray;

import com.mediatek.bluetooth.IBleDeviceManagerCallback;

/**
 * An internal utility class to help record the callback registered by all the applications
 */

public class ClientCallbackMap {

    private static final String TAG = "ClientCallbackMap";
    private static final boolean DBG = true;

    private boolean mIsDead = false;

    private final Object mCallbackLock = new Object();

    // Client to Callback
    private final SparseArray<IBleDeviceManagerCallback> mClientCallbackMap =
            new SparseArray<IBleDeviceManagerCallback>();

    // Client to BluetoothDevice
    private final SparseArray<BluetoothDevice> mClientDeviceMap =
            new SparseArray<BluetoothDevice>();

    private final RemoteCallbackList<IBleDeviceManagerCallback> mCallbacks =
            new RemoteCallbackList<IBleDeviceManagerCallback>();

    public void kill() {
        synchronized (mCallbackLock) {
            mCallbacks.kill();
            mClientCallbackMap.clear();
            mClientDeviceMap.clear();
            mIsDead = true;
        }
    }

    public boolean register(final int clientId, final BluetoothDevice device,
            final IBleDeviceManagerCallback callback) {
        if (DBG) Log.d(TAG, "register: clientID=" + clientId + ",callback = " + callback);

        synchronized (mCallbackLock) {

            if (mIsDead) {
                return false;
            }

            mClientCallbackMap.put(clientId, callback);
            mClientDeviceMap.put(clientId, device);
            return mCallbacks.register(callback, clientId);
        }
    }

    public boolean unregister(final int clientId) {
        if (DBG) Log.d(TAG, "unregister: clientID=" + clientId);

        synchronized (mCallbackLock) {

            mClientDeviceMap.remove(clientId);
            final IBleDeviceManagerCallback cb = mClientCallbackMap.get(clientId);
            mClientCallbackMap.remove(clientId);

            if (cb != null) {
                return mCallbacks.unregister(cb);
            } else {
                return false;
            }
        }
    }

    public int beginBroadcast() {
        synchronized (mCallbackLock) {
            return mCallbacks.beginBroadcast();
        }
    }

    public void finishBroadcast() {
        synchronized (mCallbackLock) {
            mCallbacks.finishBroadcast();
        }
    }

    // Get a callback object in the Broadcast list
    public IBleDeviceManagerCallback getBroadcastCallback(final int index) {
        final IBleDeviceManagerCallback callback = mCallbacks.getBroadcastItem(index);
        if (DBG) Log.d(TAG, "Callback instance =" + callback);
        return callback;
    }

    // Get a callback object in the Broadcast list
    public int getBroadcastClientId(final int index) {
        final Integer clientId = (Integer) mCallbacks.getBroadcastCookie(index);
        if (DBG) Log.d(TAG, "Client ID =" + clientId);
        return clientId.intValue();
    }

    // Check if a callback object matches the target device
    public boolean isValidCallback(final int index, final BluetoothDevice targetDevice) {
        if (targetDevice == null) {
            return false;
        }

        final int clientID = (Integer) mCallbacks.getBroadcastCookie(index);
        if (DBG) Log.d(TAG, "Callback client =" + clientID);

        final BluetoothDevice device = mClientDeviceMap.get(clientID);

        if (device == null) {
            Log.w(TAG, "Invalid client:" + clientID);
        }

        return targetDevice.equals(device);
    }
}
