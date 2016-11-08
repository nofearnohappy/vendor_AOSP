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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseIntArray;

import com.mediatek.bluetoothle.anp.IAlertNotificationProfileService;

import java.util.ArrayList;

/**
 * Provides interfaces for operations in BLE Alert Notification Profile background service
 * 
 * @hide
 */

public class BleAlertNotificationProfileService extends BleProfileService {

    private static final String TAG = "BleProximityProfileService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /**
     * @internal
     */
    public static final String ACTION_REMOTE_CHANGE = "com.mediatek.ble.ans.REMOTE_STATE_CHANGED";

    /**
     * @internal
     */
    public static final int CATEGORY_ERROR_VALUE = Integer.MIN_VALUE;

    /**
     * @internal
     */
    public static final int CATEGORY_ID_EMAIL = 1;

    /**
     * @internal
     */
    public static final int CATEGORY_ID_INCOMING_CALL = 3;

    /**
     * @internal
     */
    public static final int CATEGORY_ID_MISSED_CALL = 4;

    /**
     * @internal
     */
    public static final int CATEGORY_ID_SMS = 5;

    /**
     * @internal
     */
    public static final int CATEGORY_VAULE_ALL_ALERT_DISABLED = 0x00;

    /**
     * @internal
     */
    public static final int CATEGORY_VALUE_NEW_ALERT_ENABLED = 0x01;

    /**
     * @internal
     */
    public static final int CATEGORY_VALUE_UNREAD_ALERT_ENABLED = 0x02;

    /**
     * @internal
     */
    public static final int CATEGORY_VALUE_ALL_ALERT_ENABLED = CATEGORY_VALUE_NEW_ALERT_ENABLED
            | CATEGORY_VALUE_UNREAD_ALERT_ENABLED;

    private Context mContext;
    private ProfileServiceListener mServiceListener;
    private IAlertNotificationProfileService mService;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) {
                Log.d(TAG, "Proxy object connected");
            }
            mService = IAlertNotificationProfileService.Stub.asInterface(service);
            if (null != mServiceListener) {
                mServiceListener.onServiceConnected(BleProfile.ANP,
                        BleAlertNotificationProfileService.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) {
                Log.d(TAG, "Proxy object disconnected");
            }
            mService = null;
            if (null != mServiceListener) {
                mServiceListener.onServiceDisconnected(BleProfile.ANP);
            }
        }
    };

    BleAlertNotificationProfileService(Context ctxt, ProfileServiceListener listener) {
        mContext = ctxt;
        mServiceListener = listener;
        doBind();
    }

    /* package */void close() {
        synchronized (mConnection) {
            if (mService != null) {
                try {
                    mService = null;
                    mContext.unbindService(mConnection);
                } catch (Exception e) {
                    Log.e(TAG, "", e);
                }
            }
        }
        mServiceListener = null;
    }

    boolean doBind() {
        if (VDBG) {
            Log.v(TAG, "doBind");
        }
        Intent intent = new Intent(IAlertNotificationProfileService.class.getName());
        intent.setClassName("com.mediatek.bluetoothle", "com.mediatek.bluetoothle"
                + ".anp.AlertNotificationProfileService");
        if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to ProximityProfileService with " + intent);
            return false;
        }
        return true;
    }

    /**
     * get UI category value setting
     *
     * @param address the address of BluetoothDevice need get
     * @param categorylist a list of category ID
     * @return key is category ID, value is the category value get from ANS
     *
     * @internal
     */
    public SparseIntArray getDeviceSettings(String address,
            ArrayList<Integer> categorylist) {

        try {
            if (VDBG) {
                Log.v(TAG, "getDeviceSettings:" + address);
            }
            if (categorylist != null) {
                int listSize = categorylist.size();
                int[] categoryArray = new int[listSize];
                for (int i = 0; i < listSize; i++) {
                    categoryArray[i] = categorylist.get(i);
                }
                int[] resultArray = null;
                if (mService != null) {
                    resultArray = mService.getDeviceSettings(address, categoryArray);
                }
                if (resultArray == null) {
                    return null;
                }
                int arraySize = resultArray.length;
                if (listSize != arraySize) {
                    return null;
                }
                SparseIntArray resultSparseIntArray = new SparseIntArray();
                for (int i = 0; i < listSize; i++) {
                    resultSparseIntArray.put(categorylist.get(i), resultArray[i]);
                }
                return resultSparseIntArray;
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * Get the category settings that set by remote device
     *
     * @param address the address of BluetoothDevice need get
     * @param categorylist a list of category ID
     * @return key is category ID, value is the category value get from ANS
     *
     * @internal
     */
    public SparseIntArray getRemoteSettings(String address,
            ArrayList<Integer> categorylist) {
        try {
            if (VDBG) {
                Log.v(TAG, "getRemoteSettings:" + address);
            }
            if (categorylist != null) {
                int listSize = categorylist.size();
                int[] categoryArray = new int[listSize];
                for (int i = 0; i < listSize; i++) {
                    categoryArray[i] = categorylist.get(i);
                }
                int[] resultArray = null;
                if (mService != null) {
                    resultArray = mService.getRemoteSettings(address, categoryArray);
                }
                if (resultArray == null) {
                    return null;
                }
                int arraySize = resultArray.length;
                if (listSize != arraySize) {
                    return null;
                }
                SparseIntArray resultSparseIntArray = new SparseIntArray();
                for (int i = 0; i < listSize; i++) {
                    resultSparseIntArray.put(categorylist.get(i), resultArray[i]);
                }
                return resultSparseIntArray;
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * update UI category value setting
     *
     * @param address the address of BluetoothDevice need update
     * @param categoryIdValues key is category ID, value is the category value need update to ANS
     * @return updating is successful
     *
     * @internal
     */
    public boolean updateDeviceSettings(String address, SparseIntArray categoryIdValues) {

        try {
            if (VDBG) {
                Log.v(TAG, "updateDeviceSetting: " + address);
            }
            if (categoryIdValues != null) {
                int size = categoryIdValues.size();
                int[] categoryArray = new int[size];
                int[] valueArray = new int[size];
                for (int i = 0; i < size; i++) {
                    categoryArray[i] = categoryIdValues.keyAt(i);
                    valueArray[i] = categoryIdValues.valueAt(i);
                }
                if (mService != null) {
                    return mService.updateDeviceSettings(address, categoryArray, valueArray);
                } else {
                    return false;
                }
            }
        } catch (RemoteException e) {
            Log.v(TAG, e.toString());
        }
        return false;
    }
}
