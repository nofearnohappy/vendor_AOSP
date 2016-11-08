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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.mediatek.blemanager.provider.ActivityDbOperator;
import com.mediatek.blemanager.provider.BleConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CachedBluetoothDeviceManager manages the set of remote Bluetooth devices.
 */
public final class CachedBleDeviceManager {
    private static final String TAG = BleConstants.COMMON_TAG + "[CachedBleDeviceManager]";

    private static CachedBleDeviceManager sInstance;
    private CopyOnWriteArrayList<CachedBleDevice> mCachedDevicesList =
            new CopyOnWriteArrayList<CachedBleDevice>();

    private CopyOnWriteArrayList<CachedDeviceListChangedListener> mChangedListenersList =
            new CopyOnWriteArrayList<CachedDeviceListChangedListener>();

    private CachedBleDeviceManager() {

    }

    public static CachedBleDeviceManager getInstance() {
        if (sInstance == null) {
            sInstance = new CachedBleDeviceManager();
        }
        return sInstance;
    }

    /**
     * on cached device list changed listener such as add, remove
     *
     */
    public interface CachedDeviceListChangedListener {
        void onDeviceAdded(CachedBleDevice device);
        void onDeviceRemoved(CachedBleDevice device);
    }

    /**
     * register changed callback
     * @param listener
     */
    public void registerDeviceListChangedListener(CachedDeviceListChangedListener listener) {
        if (listener == null) {
            return;
        }
        if (!mChangedListenersList.contains(listener)) {
            mChangedListenersList.add(listener);
        }
    }

    /**
     * unregister changed callback;
     * @param listener
     */
    public void unregisterDeviceListChangedListener(CachedDeviceListChangedListener listener) {
        if (listener == null) {
            return;
        }
        if (mChangedListenersList.contains(listener)) {
            mChangedListenersList.remove(listener);
        }
    }

    /**
     * notify device has been add to the list
     * @param newDevice
     */
    private void notifyDeviceAdded(CachedBleDevice newDevice) {
        if (mChangedListenersList != null && mChangedListenersList.size() != 0) {
            for (CachedDeviceListChangedListener listener : mChangedListenersList) {
                listener.onDeviceAdded(newDevice);
            }
        }
    }

    /**
     * notify device has been removed from the list
     * @param device
     */
    private void notifyDeviceRemoved(CachedBleDevice device) {
        if (mChangedListenersList != null && mChangedListenersList.size() != 0) {
            for (CachedDeviceListChangedListener listener : mChangedListenersList) {
                listener.onDeviceRemoved(device);
            }
        }
        if (device != null && device.getDevice() != null) {
            ActivityDbOperator.getInstance().deleteDevice(device);
        }
    }

    /**
     *
     * @return
     */
    public synchronized ArrayList<CachedBleDevice> getCachedDevicesCopy() {
        return new ArrayList<CachedBleDevice>(mCachedDevicesList);
    }

    /**
     *
     * @param device
     * @return
     */
    public CachedBleDevice findDevice(BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "[findDevice] device is null!!");
            return null;
        }
        if (mCachedDevicesList.size() == 0) {
            Log.w(TAG, "[findDevice] device list is empty!!");
            return null;
        }
        for (CachedBleDevice cachedDevice : mCachedDevicesList) {
            if (cachedDevice.getDevice().equals(device)) {
                return cachedDevice;
            }
        }
        return null;
    }

    /**
     *
     * @param disOrder
     * @return
     */
    public CachedBleDevice getCachedDeviceFromDisOrder(int disOrder) {
        if (mCachedDevicesList.size() == 0) {
            Log.w(TAG, "[getCachedDeviceFromDisOrder] device list is empty!!");
            return null;
        }
        for (CachedBleDevice cachedDevice : mCachedDevicesList) {
            if (cachedDevice.getDeviceLocationIndex() == disOrder) {
                return cachedDevice;
            }
        }
        return null;
    }

    /**
     *
     * @param device
     * @param locationIndex
     * @return
     */
    public CachedBleDevice addDevice(BluetoothDevice device, int locationIndex) {
        //for2D
        //if (mCachedDevicesList.size() == 4) {
        //    Log.w(TAG, "[addDevice] device list is full!!");
        //    return null;
        //}
        if (mCachedDevicesList == null) {
            Log.w(TAG, "[addDevice]mCachedDevicesList is null!!");
            return null;
        }

        CachedBleDevice cachedDevice = findDevice(device);
        if (cachedDevice != null) {
            Log.d(TAG, "[addDevice] device has been added to de manager," +
                    " just return the cached device");
            return cachedDevice;
        }
        CachedBleDevice newDevice = new CachedBleDevice(device, locationIndex);
        mCachedDevicesList.add(newDevice);
        notifyDeviceAdded(newDevice);
        return newDevice;
    }

    /**
     *
     * @param device
     * @return
     */
    public boolean removeDevice(CachedBleDevice device) {
        if (device == null || mCachedDevicesList == null) {
            Log.w(TAG, "[removeDevice] device is null,device = " + device);
            return false;
        }
        if (mCachedDevicesList.size() == 0) {
            Log.w(TAG, "[removeDevice] mCachedDevices size is 0!!");
            return false;
        }
        CachedBleDevice cachedDevice = findDevice(device.getDevice());
        if (cachedDevice == null) {
            Log.w(TAG, "[removeDevice] not exist in device list!!");
            return false;
        }
        boolean ret = mCachedDevicesList.remove(cachedDevice);
        notifyDeviceRemoved(cachedDevice);
        return ret;
    }

    public CachedBleDevice removeDevice(int disOrder) {
        CachedBleDevice device = this.getCachedDeviceFromDisOrder(disOrder);
        if (device == null || mCachedDevicesList == null ) {
            Log.w(TAG, "[removeDevice]device is null,device= " + device);
            return null;
        }
        mCachedDevicesList.remove(device);
        notifyDeviceRemoved(device);
        return device;
    }

    /**
     * Get cached device which supported fmp
     * @return
     */
    public ArrayList<CachedBleDevice> getFmpDevices() {
        ArrayList<CachedBleDevice> retValue = new ArrayList<CachedBleDevice>();
        for (CachedBleDevice cachedDevice : mCachedDevicesList) {
            if (cachedDevice.isSupportFmp()) {
                retValue.add(cachedDevice);
            }
        }
        return retValue;
    }

    /**
     * Get cached device which supported pxp
     * @return
     */
    public ArrayList<CachedBleDevice> getPxpDevices() {
        ArrayList<CachedBleDevice> retValue = new ArrayList<CachedBleDevice>();
        for (CachedBleDevice cachedDevice : mCachedDevicesList) {
            if (cachedDevice.isSupportPxpOptional()) {
                retValue.add(cachedDevice);
            }
        }
        return retValue;
    }

    /**
     * get cached device which supported ans
     * @return
     */
//    public ArrayList<CachedBluetoothLEDevice> getAnsDevices() {
//        ArrayList<CachedBluetoothLEDevice> retValue = new ArrayList<CachedBluetoothLEDevice>();
//        for(CachedBluetoothLEDevice cachedDevice : mCachedDevices) {
//            if (cachedDevice.isSupportAns()) {
//                retValue.add(cachedDevice);
//            }
//        }
//        return retValue;
//    }

    /**
     *
     * @param device
     * @param state
     */
    void onDeviceConnectionStateChanged(BluetoothDevice device, int state) {
        if (device == null) {
            Log.w(TAG, "[onDeviceConnectionStateChanged] device is null");
            return;
        }
        CachedBleDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) {
            Log.w(TAG, "[onDeviceConnectionStateChanged] cachedDevice is null");
            return;
        }
        Log.d(TAG, "[onDeviceConnectionStateChanged] connect state : " + state);
        cachedDevice.onConnectionStateChanged(state);
    }

    /**
     * When remote device name has been changed, and receiver received the name changed
     * broadcast, then will call this method, and update the device name.
     * @param device
     */
    void onDeviceNameChanged(BluetoothDevice device, String name) {
        CachedBleDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) {
            Log.w(TAG, "[onDeviceNameChanged] cachedDevice is null. ");
            return;
        }
        Log.d(TAG, "[onDeviceNameChanged] name : " + name);
        cachedDevice.setDeviceName(name);
    }

    /**
     *
     * @param device
     */
    void onDeviceUuidChanged(BluetoothDevice device) {

    }

    /**
     * When remote device services changed, then update the service list
     * @param device
     */
    void onDeviceServiceDiscoveried(BluetoothDevice device, List<BluetoothGattService> services) {
        CachedBleDevice cachedDevice = findDevice(device);
        if (cachedDevice == null) {
            Log.w(TAG, "[onDeviceServiceDiscoveried] cachedDevice is null. ");
            return;
        }
        // used to update device service list;
        ArrayList<BluetoothGattService> serList = new ArrayList<BluetoothGattService>(services);
        cachedDevice.onServiceDiscovered(serList);
    }

}
