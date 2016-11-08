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

import com.mediatek.bluetoothle.bleservice.IProfileServiceManager;
import com.mediatek.bluetoothle.bleservice.IProfileServiceManagerCallback;

/**
 * Ble profile service manager
 *
 * It provides several methods to query status of each profile service
 *
 * @hide
 */
public class BleProfileServiceManager {
    private static final boolean DBG = true;
    private static final String TAG = "BleProfileServiceManager";
    private ProfileServiceManagerListener mServiceListener;
    private IProfileServiceManager mService;
    private final Context mContext;

    /**
     * Intent used to broadcast the change in server state of the Ble profile
     * It's the action for the intent and following has 3 extra:
     *
     * @internal
     */
    public static final String ACTION_SERVER_STATE_CHANGED =
            "com.mediatek.bluetooth.bleprofileservermanager.state.changed";
    /**
     * EXTRA_STATE - The current state of the ble server profile
     *
     * @internal
     */
    public static final String EXTRA_STATE =
            "com.mediatek.bluetooth.bleprofileservermanager.extra.STATE";
    /**
     * EXTRA_PREVIOUS_STATE - The previous state of the ble server profile
     *
     * @internal
     */
    public static final String EXTRA_PREVIOUS_STATE =
            "com.mediatek.bluetooth.bleprofileservermanager.extra.PREVIOUS_STATE";
    /**
     * EXTRA_PROFILE - The id for the ble server profile
     *
     * @internal
     */
    public static final String EXTRA_PROFILE =
            "com.mediatek.bluetooth.bleprofileservermanager.extra.PROFILE";

    /**
     * The following constants used to represent the state of the profile server
     *
     * STATE_SERVER_IDLE means server's in idle state
     *
     * @internal
     **/
    public static final int STATE_SERVER_IDLE = 0X00;
    /**
     * STATE_SERVER_REGISTERING means server's in registering state
     *
     * @internal
     */
    public static final int STATE_SERVER_REGISTERING = 0x01;
    /**
     * STATE_SERVER_REGISTERED means server's in registered state
     *
     * @internal
     */
    public static final int STATE_SERVER_REGISTERED = 0X02;
    /**
     * STATE_SERVER_UNKNOWN means server's in unknown
     */
    public static final int STATE_SERVER_UNKNOWN = 0X04;
    /**
     * STATUS_ENABLED means background mode is enabled
     *
     * @internal
     */
    public static final int STATUS_ENABLED = 1;
    /**
     * STATUS_DISABLED means background mode is disabled
     *
     * @internal
     */
    public static final int STATUS_DISABLED = 0;
    /**
     * STATUS_DISABLED means background mode is unknown
     *
     * @internal
     */
    public static final int STATUS_UNKNOWN = -1;
    /**
     * Interface for ProfileServiceManagerListener
     */
    public interface ProfileServiceManagerListener {
        /**
         * Called to notify the client when the proxy object has been connected
         * to the service.
         *
         * @param proxy the proxy of the BleProfileServiceManager
         */
        void onServiceConnected(BleProfileServiceManager proxy);

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the service.
         *
         * @param proxy the proxy of the BleProfileServiceManager
         */
        void onServiceDisconnected(BleProfileServiceManager proxy);
    }

    private IProfileServiceManagerCallback mCallback =
            new IProfileServiceManagerCallback.Stub() { };

    private void registerCallback() {
        if (null != mService) {
            try {
                mService.registerCallback(mCallback);
            } catch (final RemoteException e) {
                Log.e(TAG, "registerCallback:" + e.toString());
            }
        }
    }

    private void unregisterCallback() {
        if (null != mService) {
            try {
                mService.unregisterCallback(mCallback);
            } catch (final RemoteException e) {
                Log.e(TAG, "unregisterCallback:" + e.toString());
            }
        }
    }

    /**
     * Get all supported Ble profile servers in the platform
     *
     * @return array of id in the BleProfile.java
     */
    public int[] getCurSupportedServerProfiles() {
        int[] ids = new int[] {};

        if (null == mService) {
            if (DBG) {
                Log.d(TAG, "getCurSupportedServerProfiles: null == mService");
            }
            return ids;
        }

        try {
            if (DBG) {
                Log.d(TAG, "getCurSupportedServerProfiles");
            }
            ids = mService.getCurSupportedServerProfiles();
        } catch (final RemoteException e) {
            Log.e(TAG, e.toString());
        }

        return ids;
    }

    /**
     * Query the status of the server by using profile id defined in the
     * BleProfile.java
     *
     * @param profile the profile id of BLE
     *
     * @return current state of the profile server
     */
    public int getProfileServerState(final int profile) {
        int state = BleProfileServiceManager.STATE_SERVER_IDLE;

        if (null == mService) {
            if (DBG) {
                Log.d(TAG, "getProfileServerState: null == mService");
            }
            return BleProfileServiceManager.STATE_SERVER_UNKNOWN;
        }

        try {
            if (DBG) {
                Log.d(TAG, "getProfileServerState: profile=" + profile);
            }
            state = mService.getProfileServerState(profile);
        } catch (final RemoteException e) {
            state = BleProfileServiceManager.STATE_SERVER_UNKNOWN;
            Log.e(TAG, e.toString());
        }

        return state;
    }

    /**
     * Enable the background mode for BLE services
     *
     * @param bEnabled true for enabling the background mode
     *                  false for disabling the background mode
     *
     * @return true, if it's success
     *         false,it it failed
     *
     * @internal
     */
    public boolean setBackgroundMode(final boolean bEnabled) {
        boolean bSuccess = false;

        if (null == mService) {
            if (DBG) {
                Log.d(TAG, "setBackgroundMode: null == mService");
            }
            return false;
        }

        try {
            bSuccess = mService.setBackgroundMode(bEnabled);
        } catch (final RemoteException e) {
            bSuccess = false;
            Log.e(TAG, e.toString());
        }

        if (DBG) {
            Log.d(TAG, "setBackgroundMode: bEnabled="
                    + bEnabled + ",bSuccess:" + bSuccess);
        }
        return bSuccess;
    }

    /**
     * Query the status of the background mode
     *
     *
     * @return STATUS_ENABLED, if background mode is on
     *         STATUS_DISABLED,if background mode is off
     *         STATUS_UNKNOWN, if some errors occur
     *
     * @internal
     */
    public int getBackgroundMode() {
        int nRet = STATUS_ENABLED;

        if (null == mService) {
            if (DBG) {
                Log.d(TAG, "getBackgroundMode: null == mService");
            }
            return STATUS_UNKNOWN;
        }

        try {
            nRet = (mService.isBackgroundModeEnabled() ? STATUS_ENABLED
                    : STATUS_DISABLED);
        } catch (final RemoteException e) {
            nRet = STATUS_UNKNOWN;
            Log.e(TAG, e.toString());
        }

        if (DBG) {
            Log.d(TAG, "getBackgroundMode: nRet=" + nRet);
        }
        return nRet;
    }

    /**
     * Launch the BLE services when the background mode is off
     *
     *
     * @return true, if this operation starts
     *         false,if this operation fails
     *
     * @internal
     */
    public boolean launchServices() {
        boolean isExec = false;

        if (null == mService) {
            if (DBG) {
                Log.d(TAG, "launchServices: null == mService");
            }
            return isExec;
        }

        try {
            isExec = mService.launchServices();
        } catch (final RemoteException e) {
            isExec = false;
            Log.e(TAG, e.toString());
        }

        if (DBG) {
            Log.d(TAG, "launchServices: isExec=" + isExec);
        }
        return isExec;
    }

    /**
     * Shutdown the BLE services when the background mode is off
     *
     *
     * @return true, if this operation starts
     *         false,if this operation fails
     *
     * @internal
     */
    public boolean shutdownServices() {
        boolean isExec = false;

        if (null == mService) {
            if (DBG) {
                Log.d(TAG, "shutdownServices: null == mService");
            }
            return isExec;
        }

        try {
            isExec = mService.shutdownServices();
        } catch (final RemoteException e) {
            isExec = false;
            Log.e(TAG, e.toString());
        }

        if (DBG) {
            Log.d(TAG, "shutdownServices: isExec=" + isExec);
        }
        return isExec;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            if (DBG) {
                Log.d(TAG, "Proxy object connected");
            }

            mService = IProfileServiceManager.Stub.asInterface(service);
            registerCallback();
            if (null != mServiceListener) {
                mServiceListener
                        .onServiceConnected(BleProfileServiceManager.this);
            }

        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (DBG) {
                Log.d(TAG, "Proxy object disconnected");
            }

            mService = null;
            if (null != mServiceListener) {
                mServiceListener
                        .onServiceDisconnected(BleProfileServiceManager.this);
            }

        }
    };

    /* package */BleProfileServiceManager(final Context ctxt,
            final ProfileServiceManagerListener listener) {
        mContext = ctxt;
        mServiceListener = listener;
        doBind();
    }

    /* package */void close() {
        synchronized (mConnection) {
            if (mService != null) {
                try {
                    unregisterCallback();
                    mService = null;
                    mContext.unbindService(mConnection);
                } catch (final NullPointerException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
        mServiceListener = null;
    }

    boolean doBind() {
        if (DBG) {
            Log.d(TAG, "doBind");
        }
        final Intent intent = new Intent(IProfileServiceManager.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to BleProfileManagerService with "
                       + intent);
            return false;
        }
        return true;
    }

}
