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

package com.mediatek.bluetoothle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Class for representing a GATT profile server It's only used in the process of
 * BluetoothLe {@hide}
 */
public class BleProfileServer implements IBleProfileServer {
    private static final boolean DBG = true;
    private static final String TAG = "BleProfileServer";
    private final int mProfileId;
    private boolean mProfileStarted;
    private BluetoothGattServer mGattServer;
    private List<BluetoothGattService> mListService;
    private BluetoothGattServerCallback mCallback;
    private final IBleProfileServerObjectPool mObjectPool;

    /**
     * Constructor for BleProfileServer
     *
     * @param pool the server object pool for each BLE profile
     * @param profile the BLE profile
     */
    /* package */BleProfileServer(final IBleProfileServerObjectPool pool, final int profile) {
        mObjectPool = pool;
        // check whether the id is valid or not
        mProfileId = profile;
    }

    /**
     * Query the profile which the BleProfileServer instance bound with
     *
     * @return the id of BLE profile
     */
    @Override
    public int getProfileId() {
        return mProfileId;
    }

    /**
     * Get all the services belongs to the profile
     *
     * @return all the GATT services belongs to the profile
     */
    @Override
    public List<BluetoothGattService> getServices() {
        if (null == mListService) {
            BleProfileServicesFactory.getInstance().init(mObjectPool.getContext());
            mListService = BleProfileServicesFactory.getInstance().constructProfileServices(
                    getProfileId());
        }
        return mListService;
    }

    /**
     * Get a service belongs to the profile based on a uuid
     *
     * @param uuid the uuid for the ble service
     * @return a service belongs to the profile and its UUID is uuid
     */
    @Override
    public BluetoothGattService getService(final UUID uuid) {
        BluetoothGattService service = null;
        final List<BluetoothGattService> listService = getServices();

        for (final BluetoothGattService s : listService) {
            if (s.getUuid().equals(uuid)) {
                service = s;
                break;
            }
        }

        return service;
    }

    /**
     * To add all the services bound with the profile
     *
     * @param callback Callback used to receive incoming request
     * @return RET_SUCCESS, the action to start profile is successfully
     *         RET_PROFILE_ALREADY_STARTED, the profile is already started
     *         RET_FAILURE, the action to start profile is failure
     */
    @Override
    public int startProfileServices(final BluetoothGattServerCallback callback) {
        int retCode = IBleProfileServer.RET_FAILURE;
        if (!mProfileStarted) {
            final Context ctxt = mObjectPool.getContext();
            final List<BluetoothGattService> listService = getServices();
            final BluetoothManager bluetoothManager = (BluetoothManager) ctxt
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            mProfileStarted = true;
            retCode = IBleProfileServer.RET_SUCCESS;
            // those field should be null before assignment
            mCallback = callback;
            mGattServer = bluetoothManager.openGattServer(ctxt, mCallback);
            if (null != mGattServer) {
                try {
                    for (final BluetoothGattService s : listService) {
                        if (!mGattServer.addService(s)) {
                            if (DBG) {
                                Log.d(TAG, "startProfileServices:" + s + " fail");
                            }
                        }
                    }
                } catch (final NullPointerException e) {
                    Log.e(TAG, "startProfileServices: " + e);
                }
            } else {
                retCode = IBleProfileServer.RET_FAILURE;
                if (DBG) {
                    Log.d(TAG, "startProfileServices: BT maybe turn off now!");
                }
            }
        } else {
            retCode = IBleProfileServer.RET_PROFILE_ALREADY_STARTED;
        }
        return retCode;
    }

    /**
     * To remove all the services bound with the profile
     */
    @Override
    public void stopProfileServices() {
        if (mProfileStarted && null != mGattServer) {
            mGattServer.clearServices();
        } else {
            if (DBG) {
                Log.d(TAG, "stopProfileServices: BT already turn off!");
            }
        }
    }

    @Override
    public void close() {
        if (null != mGattServer) {
            try {
                final List<BluetoothGattService> listService = getServices();
                for (final BluetoothGattService s : listService) {
                    mGattServer.removeService(s);
                }
            } catch (final NullPointerException e) {
                Log.e(TAG, "close: " + e);
            }
            mGattServer.close();
        } else {
            // it means BT has already turned off when onCreate is invoked
            if (DBG) {
                Log.d(TAG, "stopProfileServices: BT already turn off!");
            }
        }
        mProfileStarted = false;
        mGattServer = null;
        mCallback = null;
    }

    /**
     * Send a notification or indication that a local characteristic has been
     * updated
     *
     * @param device The device you want to notify
     * @param characteristic The updated characteristic
     * @param confirm true, you need to be confirmed by the client false,you
     *            needn't to be confirmed by the client
     * @return true, false,
     */
    @Override
    public boolean notifyCharacteristicChanged(final BluetoothDevice device,
            final BluetoothGattCharacteristic characteristic, final boolean confirm) {
        if (mProfileStarted) {
            return mGattServer.notifyCharacteristicChanged(device, characteristic, confirm);
        } else {
            if (DBG) {
                Log.d(TAG, "notifyCharacteristicChanged: profile is not started yet!");
            }
            return false;
        }
    }

    /**
     * Send a response to a read or write request to a remote device
     *
     * @param device The remote device to send this response to
     * @param requestId The ID of the request that was received with the
     *            callback
     * @param status The status of the request to be sent to the remote devices
     * @param offset Value offset for partial read/write response
     * @param value The value of the attribute that was read/written (optional)
     * @return true, sendResponse is successful false, it fail to sendResponse
     */
    @Override
    public boolean sendResponse(final BluetoothDevice device, final int requestId,
            final int status, final int offset, final byte[] value) {
        if (mProfileStarted) {
            return mGattServer.sendResponse(device, requestId, status, offset, value);
        } else {
            if (DBG) {
                Log.d(TAG, "sendResponse: profile is not started yet!");
            }
            return false;
        }
    }

    /**
     * Initiate a connection to a Bluetooth GATT capable device
     *
     * @param device The device you want to connect
     * @param autoConnect Whether to directly connect to the remote device
     *            (false) or to automatically connect as soon as the remote
     *            device becomes available (true)
     * @return true, if success. false, if failed.
     */
    @Override
    public boolean connect(final BluetoothDevice device, final boolean autoConnect) {
        if (mProfileStarted) {
            return mGattServer.connect(device, autoConnect);
        } else {
            if (DBG) {
                Log.d(TAG, "connect: profile is not started yet!");
            }
            return false;
        }
    }

    /**
     * Disconnects an established connection, or cancels a connection attempt
     * currently in progress
     *
     * @param device the bluetooth device
     */
    @Override
    public void cancelConnection(final BluetoothDevice device) {
        if (mProfileStarted) {
            mGattServer.cancelConnection(device);
        } else {
            if (DBG) {
                Log.d(TAG, "cancelConnection: profile is not started yet!");
            }
        }
    }

    /**
     * Get callback for this BleProfileServer
     *
     * @return callback of the BluetoothGattServerCallback
     */
    /* package */BluetoothGattServerCallback getProfileCallback() {
        return mCallback;
    }
}
