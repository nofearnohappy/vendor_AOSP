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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.bluetoothle.pxp.IProximityProfileService;
import com.mediatek.bluetoothle.pxp.IProximityProfileServiceCallback;

/**
 * Provides interfaces for operations in BLE Proximity Profile background service
 * 
 * @hide
 */

public class BleProximityProfileService extends BleProfileService {
    private static final String TAG = "BleProximityProfileService";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private Context mContext;
    private ProfileServiceListener mServiceListener;
    private IProximityProfileService mService;
    
    // custimizable values
    private static final int RANGE_ALERT_THRESH_NEAR = 60;
    private static final int RANGE_ALERT_THRESH_MIDDLE = 65;
    private static final int RANGE_ALERT_THRESH_FAR = 70;

    public static final int INVALID_PATH_LOSS = -999;

    /**
     * Indicates the alert type is in-range alert.
     *
     * @internal
     */
    public static final int RANGE_ALERT_TYPE_IN = 0;

    /**
     * Indicates the alert type is out-range alert.
     *
     * @internal
     */
    public static final int RANGE_ALERT_TYPE_OUT = 1;

    /**
     * Indicates the alert range is near.
     *
     * @internal
     */
    public static final int RANGE_ALERT_RANGE_NEAR = 0;

    /**
     * Indicates the alert range is middle.
     *
     * @internal
     */
    public static final int RANGE_ALERT_RANGE_MIDDLE = 1;

    /**
     * Indicates the alert range is far.
     *
     * @internal
     */
    public static final int RANGE_ALERT_RANGE_FAR = 2;

    /**
     * Intent used to broadcast the change in alert state of remote device.
     *
     * <p>This intent will have 2 extras:
     * {@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * {@link #EXTRA_STATE}- The alert state.
     *
     * @internal
     */
    public static final String ACTION_ALERT_STATE_CHANGED =
            "com.mediatek.bluetooth.action.ALERT_STATE_CHANGED";

    /**
     * Used as an int extra field in {@link #ACTION_ALERT_STATE_CHANGED}
     * intents to request the alert state. Possible values are:
     * {@link #STATE_NO_ALERT},
     * {@link #STATE_DISCONNECTED_ALERT},
     * {@link #STATE_IN_RANGE_ALERT},
     * {@link #STATE_OUT_RANGE_ALERT},
     *
     * @internal
     */
    public static final String EXTRA_STATE = "state";

    /**
     * Indicates the remote device is in no-alert state.
     *
     * @internal
     */
    public static final int STATE_NO_ALERT = 0;

    /**
     * Indicates the remote device is in disconnected alert state.
     *
     * @internal
     */
    public static final int STATE_DISCONNECTED_ALERT = 1;

    /**
     * Indicates the remote device is in in-range-alert state.
     *
     * @internal
     */
    public static final int STATE_IN_RANGE_ALERT = 2;

    /**
     * Indicates the remote device is in out-range-alert state.
     *
     * @internal
     */
    public static final int STATE_OUT_RANGE_ALERT = 3;

    /**
     * Get range alert threshold
     *
     * @param alertRange int to query
     * @return return threshold value. If the parameter is invalid, return INVALID_PATH_LOSS
     *
     * @internal
     */
    public static int getRangeAlertThreshold(int alertRange) {
        if (VDBG) Log.v(TAG, "getRangeAlertThreshold: alertRange = " + alertRange);
        switch (alertRange) {
            case RANGE_ALERT_RANGE_NEAR:
                return RANGE_ALERT_THRESH_NEAR;

            case RANGE_ALERT_RANGE_MIDDLE:
                return RANGE_ALERT_THRESH_MIDDLE;

            case RANGE_ALERT_RANGE_FAR:
                return RANGE_ALERT_THRESH_FAR;

            default:
                Log.e(TAG, "Invalid range! Return INVALID_PATH_LOSS");
                return INVALID_PATH_LOSS;
        }
    }

    public class DevicePxpParams {
        private final int mAlertEnabler;
        private final int mRangeAlertEnabler;
        private final int mRangeType;
        private final int mRangeValue;
        private final int mDisconnEnabler;

        DevicePxpParams(int alertEnabler, int rangeAlertEnabler, int rangeType,
                int rangeValue, int disconnEnabler) {
            mAlertEnabler = alertEnabler;
            mRangeAlertEnabler = rangeAlertEnabler;
            mRangeType = rangeType;
            mRangeValue = rangeValue;
            mDisconnEnabler = disconnEnabler;
        }

        /**
          * @return alert enabler
          *
          * @internal
          */    
        public int getAlertEnabler() {
            if (DBG) Log.d(TAG, "getAlertEnabler: " + mAlertEnabler);
            return mAlertEnabler;
        }

        /**
          * @return range alert enabler
          *
          * @internal
          */    
        public int getRangeAlertEnabler() {
            if (DBG) Log.d(TAG, "getRangeAlertEnabler: " + mRangeAlertEnabler);
            return mRangeAlertEnabler;
        }

        /**
          * @return range type
          *
          * @internal
          */    
        public int getRangeType() {
            if (DBG) Log.d(TAG, "getRangeType: " + mRangeType);
            return mRangeType;
        }

        /**
          * @return range value
          *
          * @internal
          */    
        public int getRangeValue() {
            if (DBG) Log.d(TAG, "getRangeValue: " + mRangeValue);
            return mRangeValue;
        }

        /**
          * @return range disconnection enabler
          *
          * @internal
          */    
        public int getDisconnEnabler() {
            if (DBG) Log.d(TAG, "getDisconnEnabler: " + mDisconnEnabler);
            return mDisconnEnabler;
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IProximityProfileService.Stub.asInterface(service);

            if (DBG) Log.d(TAG, "Proxy object connected: " + mService +
                    ", proxy:" + BleProximityProfileService.this);

            if (null != mServiceListener) {
                mServiceListener.onServiceConnected(BleProfile.PXP,
                        BleProximityProfileService.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) Log.d(TAG, "Proxy object disconnected");

            mService = null;
            if (null != mServiceListener) {
                mServiceListener.onServiceDisconnected(BleProfile.PXP);
            }
        }
    };

    BleProximityProfileService(Context ctxt, ProfileServiceListener listener) {
        mContext = ctxt;
        mServiceListener = listener;
        doBind();
    }

    /* package */void close() {
        if (DBG) Log.d(TAG, "close: " + mService + ", proxy:" + BleProximityProfileService.this);

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
        if (DBG) Log.d(TAG, "doBind");
        Intent intent = new Intent(IProximityProfileService.class.getName());
        intent.setClassName("com.mediatek.bluetoothle", "com.mediatek.bluetoothle"
                + ".pxp.ProximityProfileService");
        if (!mContext.bindService(intent, mConnection, 0)) {
            Log.e(TAG, "Could not bind to ProximityProfileService with " + intent);
            return false;
        }
        return true;
    }

    /**
     * Get current path loss value
     *
     * @param device BluetoothDevice to query
     * @return TX Power value - current RSSI value in db
     *
     * @internal
     */
    public int getPathLoss(BluetoothDevice device) {
        int pathLoss = INVALID_PATH_LOSS;

        if (VDBG) Log.v(TAG, "getPathLoss: " + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "getPathLoss: mService is null");
            return pathLoss;
        }

        try {
            if (VDBG) Log.v(TAG, "getPathLoss: " + device);
            pathLoss = mService.getPathLoss(device);
            if (VDBG) Log.v(TAG, "Path loss = " + pathLoss);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return pathLoss;
    }

    /**
     * Query if service asks remote device to alert
     *
     * @param device BluetoothDevice to query
     * @return return true if positive, otherwise return false
     *
     * @internal
     */
    public boolean isAlertOn(BluetoothDevice device) {
        boolean isAlertOn = false;

        if (VDBG) Log.v(TAG, "isAlertOn:" + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "isAlertOn: mService is null");
            return isAlertOn;
        }

        try {
            if (VDBG) Log.v(TAG, "isAlertOn: " + device);
            isAlertOn = mService.isAlertOn(device);
            if (VDBG) Log.v(TAG, "alerting level" + isAlertOn);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }
        return isAlertOn;
    }

    /**
     * Request to stop remote device from alerting
     *
     * @param device BluetoothDevice to query
     * @return return true if successfully pass the request to service, otherwise return false
     *
     * @internal
     */
    public boolean stopRemoteAlert(BluetoothDevice device) {
        boolean success = false;

        if (VDBG) Log.v(TAG, "stopRemoteAlert:" + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "stopRemoteAlert: mService is null");
            return success;
        }

        try {
            if (VDBG) Log.v(TAG, "stopRemoteAlert:" + device);
            success = mService.stopRemoteAlert(device);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        return success;

    }

    /**
      * @internal
      */
    public boolean setPxpParameters(BluetoothDevice device, int alertEnabler,
            int rangeAlertEnabler, int rangeType, int rangeValue, int disconnectEnabler) {
        boolean success = false;

        if (VDBG) Log.v(TAG, "setPxpParameters:" + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "setPxpParameters: mService is null");
            return success;
        }

        try {
            if (VDBG) Log.v(TAG, "setPxpParameters:" + device);
            success = mService.setPxpParameters(device, alertEnabler, rangeAlertEnabler, rangeType,
                    rangeValue, disconnectEnabler);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        return success;
    }

    /**
      * @internal
      */
    public DevicePxpParams getPxpParameters(BluetoothDevice device) {
        boolean success = false;
        DevicePxpParams params = null;
        int[] alertEnabler = new int[1];
        int[] rangeAlertEnabler = new int[1];
        int[] rangeType = new int[1];
        int[] rangeValue = new int[1];
        int[] disconnectEnabler = new int[1];

        if (VDBG) Log.v(TAG, "getPxpParameters:" + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "getPxpParameters: mService is null");
            return params;
        }

        try {
            if (VDBG) Log.v(TAG, "getPxpParameters:" + device);
            success = mService.getPxpParameters(device, alertEnabler, rangeAlertEnabler, rangeType,
                    rangeValue, disconnectEnabler);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        if (success == true) {
            params = new DevicePxpParams(alertEnabler[0], rangeAlertEnabler[0], rangeType[0],
                    rangeValue[0], disconnectEnabler[0]);
        }

        return params;
    }

    /**
     * Register status change callback to specific device
     *
     * @param device BluetoothDevice to query
     * @param callback instance that implements IProximityProfileServiceCallback interface
     * @return return true if successfully register callback to service, otherwise return false
     */
    public boolean registerStatusChangeCallback(BluetoothDevice device,
            IProximityProfileServiceCallback callback) {
        boolean result = false;

        if (VDBG) Log.v(TAG, "registerStatusChangeCallback:" + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "registerStatusChangeCallback: mService is null");
            return result;
        }

        try {
            result = mService.registerStatusChangeCallback(device, callback);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        if (VDBG) Log.v(TAG, "registerStatusChangeCallback:" + device + ",callback:" + callback +
                ", result:" + result);
        return result;
    }

    /**
     * Unregister status change callback to specific device
     *
     * @param device BluetoothDevice to query
     * @param callback instance that implements IProximityProfileServiceCallback interface
     * @return return true if successfully unregister callback to service, otherwise return false
     */
    public boolean unregisterStatusChangeCallback(BluetoothDevice device,
            IProximityProfileServiceCallback callback) {
        boolean result = false;

        if (VDBG) Log.v(TAG, "unregisterStatusChangeCallback:" + BleProximityProfileService.this);

        if (mService == null) {
            Log.w(TAG, "unregisterStatusChangeCallback: mService is null");
            return result;
        }

        try {
            result = mService.unregisterStatusChangeCallback(device, callback);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

        if (VDBG) Log.v(TAG, "unregisterStatusChangeCallback:" + device + ",callback:" +
            callback + ", result:" + result);
        return result;
    }
}
