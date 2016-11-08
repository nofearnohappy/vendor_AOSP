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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Sample code implementing client interface for Find Me Profile.
 * <p>
 * The APIs expose FMP-specific functions to applications
 * 
 * @hide
 */
public class BleFindMeProfile extends BleProfile {
    private static final String TAG = "BleFindMeProfile";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /**
     * Indicates the alert level is no alert.
     *
     * @internal
     */
    public static final int ALERT_LEVEL_NO = 0;
    /**
     * Indicates the alert level is 1.
     *
     * @internal
     */
    public static final int ALERT_LEVEL_MILD = 1;
    /**
     * Indicates the alert level is 2.
     *
     * @internal
     */
    public static final int ALERT_LEVEL_HIGH = 2;

    private final int mProfileID = BleProfile.FMP;
    private final BluetoothDevice mDevice;
    private final IBleGatt mGattClientIf;
    private ProfileCallback mProfileCallback;

    private static final int ALERT_LEVEL_OFFSET = 0;
    private static final int LEVEL_MIN = ALERT_LEVEL_NO;
    private static final int LEVEL_MAX = ALERT_LEVEL_HIGH;

    /**
     * Define a callback for GATT characteristic/descriptor related operations Callback.
     * It's profile-specific.
     */
    private IBleGattCallback mCallback = new IBleGattCallback() {

        @Override
        public void onCharacteristicRead(final IBleGatt gatt,
                final BluetoothGattCharacteristic characteristic, final int status) {
            if (VDBG) Log.v(TAG, "Characteristic read is not permitted");
        }

        @Override
        public void onCharacteristicWrite(final IBleGatt gatt,
                final BluetoothGattCharacteristic characteristic, final int status) {
            // /If the correct characteristic is written, notify app
            if (DBG) Log.d(TAG, "onCharacteristicWrite(): status = " + status);

            if (mProfileCallback == null) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mProfileCallback.onTargetAlerted(gatt.getDevice(), true);
            } else {
                mProfileCallback.onTargetAlerted(gatt.getDevice(), false);
            }
        }

        @Override
        public void onCharacteristicChanged(final IBleGatt gatt,
                final BluetoothGattCharacteristic characteristic) {
            if (VDBG) Log.v(TAG, "Characteristic changed");
        }

        @Override
        public void onDescriptorRead(final IBleGatt gatt,
                final BluetoothGattDescriptor descriptor, final int status) {
            if (VDBG) Log.v(TAG, "No descriptor");
        }

        @Override
        public void onDescriptorWrite(final IBleGatt gatt,
                final BluetoothGattDescriptor descriptor, final int status) {
            if (VDBG) Log.v(TAG, "No descriptor");
        }

        @Override
        public void onReliableWriteCompleted(final IBleGatt gatt, final int status) {
            if (VDBG) Log.v(TAG, "onReliableWriteCompleted: status = " + status);
        }

        @Override
        public void onReadRemoteRssi(final IBleGatt gatt, final int rssi, final int status) {
            if (VDBG) Log.v(TAG, "onReadRemoteRssi: rssi = " + rssi + ", status = " + status);
        }
    };

    /**
     * Create a BleFindMeProfile proxy object.
     *
     * @param device BLE the device want to communicate with
     * @param gattClient proxy of BleGattClientImpl for GATT operation
     */
    /* package */BleFindMeProfile(final BluetoothDevice device, final IBleGatt gattClient) {
        this.mDevice = device;
        this.mGattClientIf = gattClient;
    }

    @Override
    public final BluetoothDevice getDevice() {
        return mDevice;
    }

    @Override
    final void open() {
        // Register a callback for the profile
        if (VDBG) Log.v(TAG, "open()");
        this.mGattClientIf.registerClientCallback(BleProfile.FMP, mCallback);
    }

    @Override
    final void close() {
        // Unregister the callback
        if (VDBG) Log.v(TAG, "close()");
        this.mGattClientIf.unregisterClientCallback(BleProfile.FMP);
    }

    /**
     * Registering Find Me Profile callback.
     *
     * <p>Clients must implement
     * {@link #ProfileCallback} to get notified of
     * whether the alert command is sent successfully.
     *
     * @param callback FMP callback that will receive asynchronous result
     * @return true on success, false on error
     */
    public final boolean registerProfileCallback(final BleFindMeProfile.ProfileCallback callback) {
        this.mProfileCallback = callback;
        return true;
    }

    /**
     * A public API implementation for find target.
     *
     * <p>For getting FMP profile interface, BleDeviceManager need to be bound through
     * BleManager: {@link BleManager#getDeviceManager}. After bound with BleDeviceManager,
     * use it to create BleGattDevice: {@link BleDeviceManager#createGattDevice}.
     * Then get BleFindMeProfile through BLeGattDevice: {@link BleGattDevice#asProfileInterface}.
     *
     * <p>Valid level values are:
     * {@link #LEVEL_NO},
     * {@link #LEVEL_MILD},
     * {@link #LEVEL_HIGH}.
     *
     * @param level the alert level
     * @return true on immediate success, false on immediate error
     *
     * @internal
     */
    public final boolean findTarget(final int level) {
        if (DBG) Log.d(TAG, "findTarget: this.mGattClientIf = " + this.mGattClientIf
                + ", this.mProfileID = " + this.mProfileID + ", level = " + level);
        if (this.mGattClientIf == null || this.mProfileID < 0) {
            return false;
        } else if (LEVEL_MIN > level || LEVEL_MAX < level) {
            Log.e(TAG, "Invalid level");
            return false;
        }

        final BluetoothGattService gattService = this.mGattClientIf
                .getService(BleGattUuid.Service.IMMEDIATE_ALERT);
        if (gattService == null) {
            return false;
        }

        final BluetoothGattCharacteristic alertLevel = gattService
                .getCharacteristic(BleGattUuid.Char.ALERT_LEVEL);
        if (alertLevel == null) {
            return false;
        }

        if (VDBG) Log.v(TAG, "-> Service:" + alertLevel.getService().getInstanceId());

        try {
            final Method method = alertLevel.getService().getClass().getDeclaredMethod("getDevice");
            method.setAccessible(true);
            final BluetoothDevice device = (BluetoothDevice) method.invoke(alertLevel.getService());
            if (DBG) Log.d(TAG, "-> Device:" + device);
        } catch (final Exception e) {
            Log.e(TAG, "" + e);
        }

        if (VDBG) Log.v(TAG, "-> Char InstanceID:" + alertLevel.getInstanceId());
        if (VDBG) Log.v(TAG, "-> Char Properties:" + alertLevel.getProperties());
        if (VDBG) Log.v(TAG, "-> Char Permissions:" + alertLevel.getPermissions());

        alertLevel.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        alertLevel.setValue(level, BluetoothGattCharacteristic.FORMAT_UINT8, ALERT_LEVEL_OFFSET);

        return this.mGattClientIf.writeCharacteristic(this.mProfileID, alertLevel);
    }

    /**
     * Define a callback for Find Me Profile operations.
     */
    public interface ProfileCallback extends BleProfile.BleProfileCallback {
        /**
         * Define a callback for Find Me Profile operations.
         *
         * @param device indicate the result is from which device
         * @param isSuccess it only means whether the alert command is sent successfully.
         */
        void onTargetAlerted(BluetoothDevice device, boolean isSuccess);
    };
}
