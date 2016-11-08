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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mediatek.bluetooth.BleProfileServiceManager;
import com.mediatek.bluetoothle.BleProfileServerObjectPool;
import com.mediatek.bluetoothle.IBleProfileServer;

/**
 * BleSingleProfileServerService class
 */
public abstract class BleSingleProfileServerService extends BleProfileServerServiceBase {
    private static final boolean DBG = true;
    private static final String TAG = BleSingleProfileServerService.class.getSimpleName();
    private BleSingleProfileServerHandler mHandler;
    protected IBleProfileServer mBleProfileServer;
    private int mServiceAdded;
    private static final int MESSAGE_REGISTER_SERVICES = 0X01;
    private final BluetoothGattServerCallback mCallbackWrapper = new BluetoothGattServerCallback() {
        @Override
        public void onCharacteristicReadRequest(final BluetoothDevice device, final int requestId,
                final int offset, final BluetoothGattCharacteristic characteristic) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicReadRequest");
            }

            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            if (null != callback) {
                if (BleProfileManagerService.getSavedDevices(BleSingleProfileServerService.this)
                        .contains(device)) {
                    callback.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                } else {
                    mBleProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                            0, null);
                    if (DBG) {
                        Log.d(TAG, "onCharacteristicReadRequest: "
                                + "device is not in the list, reject it");
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device, final int requestId,
                final BluetoothGattCharacteristic characteristic, final boolean preparedWrite,
                final boolean responseNeeded, final int offset, final byte[] value) {
            if (DBG) {
                Log.d(TAG, "onCharacteristicWriteRequest");
            }

            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            if (null != callback) {
                if (BleProfileManagerService.getSavedDevices(BleSingleProfileServerService.this)
                        .contains(device)) {
                    callback.onCharacteristicWriteRequest(device, requestId, characteristic,
                            preparedWrite, responseNeeded, offset, value);
                } else {
                    if (responseNeeded) {
                        mBleProfileServer.sendResponse(device, requestId,
                                BluetoothGatt.GATT_FAILURE, 0, null);
                        if (DBG) {
                            Log.d(TAG, "onCharacteristicWriteRequest: reject it");
                        }
                    }
                    if (DBG) {
                        Log.d(TAG, "onCharacteristicWriteRequest: " + "device is not in the list");
                    }
                }
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status,
                final int newState) {
            if (DBG) {
                Log.d(TAG, "onConnectionStateChange");
            }
            // Update connection state
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (null != BleProfileManagerService.getBleProfileManagerService()) {
                    BleProfileManagerService.getBleProfileManagerService()
                            .onProfileConnectionStateChanged(device, newState);
                } else {
                    if (DBG) {
                        Log.d(TAG, "onConnectionStateChange: null == "
                                + "BleProfileManagerService.getBleProfileManagerService()");
                    }
                }
            }
            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            if (null != callback) {
                callback.onConnectionStateChange(device, status, newState);
            }
        }

        @Override
        public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId,
                final int offset, final BluetoothGattDescriptor descriptor) {
            if (DBG) {
                Log.d(TAG, "onDescriptorReadRequest");
            }

            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            if (null != callback) {
                if (BleProfileManagerService.getSavedDevices(BleSingleProfileServerService.this)
                        .contains(device)) {
                    callback.onDescriptorReadRequest(device, requestId, offset, descriptor);
                } else {
                    mBleProfileServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE,
                            0, null);
                    if (DBG) {
                        Log.d(TAG, "onDescriptorReadRequest: "
                                + "device is not in the list, reject it");
                    }
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, final int requestId,
                final BluetoothGattDescriptor descriptor, final boolean preparedWrite,
                final boolean responseNeeded, final int offset, final byte[] value) {
            if (DBG) {
                Log.d(TAG, "onDescriptorWriteRequest");
            }

            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            if (null != callback) {
                if (BleProfileManagerService.getSavedDevices(BleSingleProfileServerService.this)
                        .contains(device)) {
                    callback.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                            responseNeeded, offset, value);
                } else {
                    if (responseNeeded) {
                        mBleProfileServer.sendResponse(device, requestId,
                                BluetoothGatt.GATT_FAILURE, 0, null);
                        if (DBG) {
                            Log.d(TAG, "onCharacteristicWriteRequest: reject it");
                        }
                    }
                    if (DBG) {
                        Log.d(TAG, "onDescriptorWriteRequest: " + "device is not in the list");
                    }
                }
            }
        }

        @Override
        public void onExecuteWrite(final BluetoothDevice device, final int requestId,
                final boolean execute) {
            if (DBG) {
                Log.d(TAG, "onExecuteWrite");
            }

            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            if (null != callback) {
                if (BleProfileManagerService.getSavedDevices(BleSingleProfileServerService.this)
                        .contains(device)) {
                    callback.onExecuteWrite(device, requestId, execute);
                } else {
                    if (DBG) {
                        Log.d(TAG, "onExecuteWrite: device is not in the list");
                    }
                }
            }
        }

        @Override
        public void onServiceAdded(final int status, final BluetoothGattService service) {
            if (DBG) {
                Log.d(TAG, "onServiceAdded: getBleProfileServer().getServices().size()="
                        + getBleProfileServer().getServices().size()
                        + ",BleSingleProfileServerService.this.mServiceAdded="
                        + BleSingleProfileServerService.this.mServiceAdded + 1);
            }
            final BluetoothGattServerCallback callback = getDefaultBleProfileServerHandler();
            try {
                if (null != callback) {
                    callback.onServiceAdded(status, service);
                }
            } catch (final Exception e) {
                // it's a special case, skip it for check-style
                Log.e(TAG, "onServiceAdded: caller's callback has exception=" + e);
            }

            // notify
            BleSingleProfileServerService.this.mServiceAdded++;
            if (getBleProfileServer().getServices().size() ==
                    BleSingleProfileServerService.this.mServiceAdded) {
                if (DBG) {
                    Log.d(TAG, "BleProfileManagerService.getBleProfileManagerService()"
                            + ".onProfileServerStateChanged");
                }
                broadcastChangedState(getProfileId(),
                        BleProfileServiceManager.STATE_SERVER_REGISTERED);
            } else {
                if (getBleProfileServer().getServices().size() <
                        BleSingleProfileServerService.this.mServiceAdded) {
                    throw new RuntimeException("getBleProfileServer().getServices().size() < "
                            + "BleSingleProfileServerService.this.mServiceAdded");
                }
            }

        }
    };

    private final class BleSingleProfileServerHandler extends Handler {

        public BleSingleProfileServerHandler(final Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            if (DBG) {
                Log.d(TAG, "BleSingleProfileServerHandler msg: " + msg.what);
            }

            switch (msg.what) {
            case MESSAGE_REGISTER_SERVICES:
                if (DBG) {
                    Log.d(TAG, "BleSingleProfileServerHandler: MESSAGE_REGISTER_SERVICES");
                }
                if (DBG) {
                    Log.d(TAG, "current thread=" + Thread.currentThread());
                }
                broadcastChangedState(getProfileId(),
                        BleProfileServiceManager.STATE_SERVER_REGISTERING);
                mBleProfileServer.startProfileServices(mCallbackWrapper);
                break;
            default:
                break;
            }
        }

    };

    /**
     * Get the singleton of IBleProfileServer object
     *
     * @return singleton of IBleProfileServer object
     */
    public IBleProfileServer getBleProfileServer() {
        return mBleProfileServer;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final BleProfileServerObjectPool objectPool = BleProfileServerObjectPool.getInstance();
        objectPool.init(getApplicationContext());
        mServiceAdded = 0;
        mBleProfileServer = objectPool.acquire(getProfileId());
        createThread();
        mHandler.sendEmptyMessage(MESSAGE_REGISTER_SERVICES);
    }

    @Override
    public void onDestroy() {
        destroyThread();
        final BleProfileServerObjectPool objectPool = BleProfileServerObjectPool.getInstance();
        mBleProfileServer.stopProfileServices();
        mBleProfileServer.close();
        broadcastChangedState(getProfileId(), BleProfileServiceManager.STATE_SERVER_IDLE);
        objectPool.release(mBleProfileServer);
        mServiceAdded = 0;
        mBleProfileServer = null;
        super.onDestroy();
    }

    private void broadcastChangedState(final int profile, final int state) {
        Integer i = null;
        final BleProfileManagerService bleProfileMgrService =
                BleProfileManagerService.getBleProfileManagerService();
        if (null != bleProfileMgrService) {
            i = bleProfileMgrService.getProfileServerState(profile);
        } else {
            if (DBG) {
                Log.d(TAG, "1 bleProfileMgrService == null");
            }
        }

        final int prevState = (null == i) ? BleProfileServiceManager.STATE_SERVER_IDLE : i;
        if (DBG) {
            Log.d(TAG, "broadcastChangedState: profile=" + profile + ",preState=" + prevState
                    + " state=" + state);
        }

        final Intent intent = new Intent(BleProfileServiceManager.ACTION_SERVER_STATE_CHANGED);
        intent.putExtra(BleProfileServiceManager.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BleProfileServiceManager.EXTRA_STATE, state);
        intent.putExtra(BleProfileServiceManager.EXTRA_PROFILE, profile);
        sendBroadcast(intent);

        if (null != bleProfileMgrService) {
            bleProfileMgrService.setProfileServerState(profile, state);
        } else {
            if (DBG) {
                Log.d(TAG, "2 bleProfileMgrService == null");
            }
        }
    }

    protected abstract int getProfileId();

    protected abstract BluetoothGattServerCallback getDefaultBleProfileServerHandler();

    @Override
    public int[] getProfileIds() {
        return new int[] {
                getProfileId()
                };
    }

    @Override
    public IBleProfileServer[] getProfileServers() {
        return new IBleProfileServer[] {
                getBleProfileServer()
                };
    }

    private void createThread() {
        final HandlerThread thread = new HandlerThread(TAG + "-2");
        thread.start();
        mHandler = new BleSingleProfileServerHandler(thread.getLooper());
        if (DBG) {
            Log.d(TAG, "createThread: current=" + Thread.currentThread() +
                    ", new thread=" + thread);
        }
    }

    private void destroyThread() {
        if (null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
            final Looper looper = mHandler.getLooper();
            if (null != looper) {
                looper.quit();
            }
        }
    }
}
