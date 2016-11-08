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
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.mediatek.blemanager.provider.BleConstants;

import java.util.Set;

/**
 * LocalBluetoothAdapter provides an interface between the Settings app and the
 * functionality of the local {@link BluetoothAdapter}, specifically those
 * related to state transitions of the adapter itself.
 * 
 * <p>
 * Connection and bonding state changes affecting specific devices are handled
 * by {@link CachedBluetoothDeviceManager}, {@link BleEventManager}, and
 * {@link LocalBluetoothProfileManager}.
 */
final class LocalBluetoothAdapter {
    private static final String TAG = BleConstants.COMMON_TAG + "[LocalBluetoothAdapter]";

    /** This class does not allow direct access to the BluetoothAdapter. */
    private final BluetoothAdapter mBluetoothAdapter;

    private static LocalBluetoothAdapter sInstance;

    private int mState = BluetoothAdapter.ERROR;

    private static final int SCAN_EXPIRATION_MS = 5 * 60 * 1000; // 5 mins

    private long mLastScan;

    private LocalBluetoothAdapter(BluetoothAdapter adapter) {
        mBluetoothAdapter = adapter;
    }

    /**
     * Get the singleton instance of the LocalBluetoothAdapter. If this device
     * doesn't support Bluetooth, then null will be returned. Callers must be
     * prepared to handle a null return value.
     * 
     * @return the LocalBluetoothAdapter object, or null if not supported
     */
    static synchronized LocalBluetoothAdapter getInstance() {
        if (sInstance == null) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                sInstance = new LocalBluetoothAdapter(adapter);
            }
        }
        return sInstance;
    }

    // Pass-through BluetoothAdapter methods that we can intercept if necessary

    public void cancelDiscovery() {
        Log.d(TAG, "[cancelDiscovery]...");
        mBluetoothAdapter.cancelDiscovery();
    }

    boolean enable() {
        Log.d(TAG, "[enable]...");
        return mBluetoothAdapter.enable();
    }

    boolean disable() {
        Log.d(TAG, "[disable]...");
        return mBluetoothAdapter.disable();
    }

    void getProfileProxy(Context context, BluetoothProfile.ServiceListener listener, int profile) {
        mBluetoothAdapter.getProfileProxy(context, listener, profile);
    }

    Set<BluetoothDevice> getBondedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    String getName() {
        return mBluetoothAdapter.getName();
    }

    int getScanMode() {
        return mBluetoothAdapter.getScanMode();
    }

    int getState() {
        return mBluetoothAdapter.getState();
    }

    ParcelUuid[] getUuids() {
        return mBluetoothAdapter.getUuids();
    }

    boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    void setDiscoverableTimeout(int timeout) {
        Log.d(TAG, "[setDiscoverableTimeout] timeout : " + timeout);
        mBluetoothAdapter.setDiscoverableTimeout(timeout);
    }

    void setName(String name) {
        Log.d(TAG, "[setName] name : " + name);
        mBluetoothAdapter.setName(name);
    }

    void setScanMode(int mode) {
        Log.d(TAG, "[setScanMode] mode : " + mode);
        mBluetoothAdapter.setScanMode(mode);
    }

    boolean setScanMode(int mode, int duration) {
        Log.d(TAG, "[setScanMode] mode : " + mode + ",duration:" + duration);
        return mBluetoothAdapter.setScanMode(mode, duration);
    }

    void startScanning(boolean force, BluetoothAdapter.LeScanCallback callback) {
        // Only start if we're not already scanning
        Log.i(TAG, "[startScanning] force : " + force);
        if (callback == null) {
            Log.w(TAG, "[startScanning] callback is null");
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (!mBluetoothAdapter.isDiscovering()) {
            if (!force) {
                // Don't scan more than frequently than SCAN_EXPIRATION_MS,
                // unless forced
                if (mLastScan + SCAN_EXPIRATION_MS > System.currentTimeMillis()) {
                    Log.w(TAG, "[startScanning]mLastScan + SCAN_EXPIRATION_MS too big,return,");
                    return;
                }
            }
            Log.i(TAG, "[startScanning] start le scan ");
            mBluetoothAdapter.startLeScan(callback);
        }
    }

    public void stopScanning(BluetoothAdapter.LeScanCallback callback) {
        if (callback == null) {
            Log.w(TAG, "[stopScanning] callback is null,return.");
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        Log.i(TAG, "[stopScanning] stop le scan.");
        mBluetoothAdapter.stopLeScan(callback);
    }

    synchronized int getBluetoothState() {
        // Always sync state, in case it changed while paused
        syncBluetoothState();
        return mState;
    }

    synchronized void setBluetoothState(int state) {
        mState = state;
    }

    // Returns true if the state changed; false otherwise.
    boolean syncBluetoothState() {
        int currentState = mBluetoothAdapter.getState();
        if (currentState != mState) {
            setBluetoothState(mBluetoothAdapter.getState());
            return true;
        }
        return false;
    }

    void setBluetoothEnabled(boolean enabled) {
        boolean success = enabled ? mBluetoothAdapter.enable() : mBluetoothAdapter.disable();
        Log.d(TAG, "[setBluetoothEnabled] enabled : " + enabled + ",success = " + success);
        if (success) {
            setBluetoothState(enabled ? BluetoothAdapter.STATE_TURNING_ON
                    : BluetoothAdapter.STATE_TURNING_OFF);
            Log.d(TAG, "[setBluetoothEnabled] mState : " + mState);
        } else {
            syncBluetoothState();
            Log.d(TAG, "[setBluetoothEnabled] mState1 : " + mState);
        }
    }
}
