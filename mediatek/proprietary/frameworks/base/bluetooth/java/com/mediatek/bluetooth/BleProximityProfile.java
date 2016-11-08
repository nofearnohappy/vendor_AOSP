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

/**
 * client interface for Proximity Profile.
 * <p>
 * The APIs expose PXP-specific functions to applications / services.
 * 
 * @hide
 */
public class BleProximityProfile extends BleProfile {
    private static final String TAG = "BleProximityProfile";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private int mProfileID = BleProfile.PXP;
    private BluetoothDevice mDevice;
    private IBleGatt mGattClientIf;
    private ProfileCallback mProfileCallback;

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

    IBleGattCallback mCallback = new IBleGattCallback() {

        @Override
        public void onCharacteristicRead(IBleGatt gatt, BluetoothGattCharacteristic characteristic,
                int status) {
            // currently only work for Tx Power read

            if (mProfileCallback == null) {
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                int txPowerVal = 0;
                Integer txPower = characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_SINT8, 0);
                if (txPower != null) {
                    txPowerVal = txPower.intValue();
                }
                if (VDBG) Log.v(TAG, "onCharacteristicRead: TxPower=" + txPowerVal);
                mProfileCallback.onTxPowerRead(status, txPowerVal, gatt.getDevice());
            } else {
                return;
            }
        }

        @Override
        public void onCharacteristicWrite(IBleGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            // currently only work for Link Lost Alert Level write

            if (mProfileCallback == null) {
                return;
            }

            mProfileCallback.onLinkLostAlertLevelSet(status, gatt.getDevice());
        }

        @Override
        public void onCharacteristicChanged(IBleGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            return;
        }

        @Override
        public void onDescriptorRead(IBleGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
            return;

        }

        @Override
        public void onDescriptorWrite(IBleGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
            return;

        }

        @Override
        public void onReliableWriteCompleted(IBleGatt gatt, int status) {
            return;

        }

        @Override
        public void onReadRemoteRssi(IBleGatt gatt, int rssi, int status) {
            if (mProfileCallback == null) {
                return;
            }

            mProfileCallback.onRssiRead(status, rssi, gatt.getDevice());

        }
    };

    /* package */BleProximityProfile(BluetoothDevice device, IBleGatt gattClient) {
        this.mDevice = device;
        this.mGattClientIf = gattClient;
    }

    @Override
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    @Override
    void open() {
        // Register a callback for the profile
        if (DBG) Log.d(TAG, "open()");
        this.mGattClientIf.registerClientCallback(BleProfile.PXP, mCallback);
    }

    @Override
    void close() {
        // Unregister the callback
        if (DBG) Log.d(TAG, "close()");
        this.mGattClientIf.unregisterClientCallback(BleProfile.PXP);
    }

    /**
     * Register callback for proximity profile gatt requests.
     *
     * @internal
     */
    public boolean registerProfileCallback(BleProximityProfile.ProfileCallback callback) {
        this.mProfileCallback = callback;
        return true;
    }

    /**
     * Read current device's rssi value.
     *
     * @internal
     */
    public boolean readRssi() {
        if (this.mGattClientIf == null) {
            return false;
        }

        return this.mGattClientIf.readRemoteRssi(this.mProfileID);
    }

    /**
     * Set link lost alert level to remote device.
     *
     * @internal
     */
    public boolean setLinkLostAlertLevel(int level) {
        if (VDBG) Log.v(TAG, "setLinkLostAlertLevel:" + level);

        if (this.mGattClientIf == null) {
            return false;
        }
        if (level < ALERT_LEVEL_NO || level > ALERT_LEVEL_HIGH) {
            return false;
        }

        BluetoothGattService gattService = null;

        gattService = this.mGattClientIf.getService(BleGattUuid.Service.LINK_LOST);

        if (gattService == null) {
            if (DBG) Log.d(TAG, "setLinkLostAlertLevel: gattService is null");
            return false;
        }

        BluetoothGattCharacteristic alertLevel = gattService
                .getCharacteristic(BleGattUuid.Char.ALERT_LEVEL);

        if (alertLevel == null) {
            if (DBG) Log.d(TAG, "setLinkLostAlertLevel: alertLevel is null");
            return false;
        }

        alertLevel.setValue(new byte[] {
                (byte) level
        });

        return this.mGattClientIf.writeCharacteristic(this.mProfileID, alertLevel);
    }

    /**
     * Read current device's TX Power value.
     *
     * @internal
     */
    public boolean readTxPower() {
        if (VDBG) Log.v(TAG, "readTxPower");

        if (this.mGattClientIf == null) {
            return false;
        }

        BluetoothGattService gattService = null;

        gattService = this.mGattClientIf.getService(BleGattUuid.Service.TX_POWER);

        if (gattService == null) {
            Log.w(TAG, "readTxPower: gattService is null");
            return false;
        }

        BluetoothGattCharacteristic txPowerLevel = gattService
                .getCharacteristic(BleGattUuid.Char.TX_POWER_LEVEL);

        if (txPowerLevel == null) {
            Log.w(TAG, "readTxPower: TxPower level is null");
            return false;
        }

        return this.mGattClientIf.readCharacteristic(this.mProfileID, txPowerLevel);
    }

    /**
     * Define callback for Proximity Profile operations.
     */
    public interface ProfileCallback extends BleProfile.BleProfileCallback {
        void onRssiRead(int status, int rssi, BluetoothDevice device);

        void onLinkLostAlertLevelSet(int status, BluetoothDevice device);

        void onTxPowerRead(int status, int txPower, BluetoothDevice device);
    };

}
