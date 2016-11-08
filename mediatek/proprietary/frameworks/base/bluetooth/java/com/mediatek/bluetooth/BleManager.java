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

import android.content.Context;
import android.util.Log;

/**
 * Public API for the GATT-based BLE Profiles
 *
 * <p>
 * This class provides access to background BLE profile services & Bluetooth Smart devices
 *
 * @hide
 */

public class BleManager {

    private static final String TAG = "BleManager";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static BleManager sBleManager = new BleManager();

    private BleManager() {
        // TODO: Auto-generated constructor stub
    }

    /**
     * Get a handle to the default BleManager.
     *
     * @return the default BleManager
     *
     * @internal
     */
    public static BleManager getDefaultBleProfileManager() {
        return sBleManager;
    }

    /**
     * Get the proxy object associated with the Device Manager Service.
     *
     * @return true on success, false on error
     *
     * @internal
     */
    public boolean getDeviceManager(Context ctx,
            BleDeviceManager.DeviceManagerListener proxyListener) {
        if (DBG) Log.d(TAG, "getDeviceManager");

        if (ctx == null || proxyListener == null) {
            return false;
        }

        new BleDeviceManager(ctx, proxyListener);

        return true;
    }

    /**
     * Close the connection of the proxy to the Device Manager Service.
     *
     * <p>
     * Clients should call this when they are no longer using the proxy
     *
     * @param proxy
     *            Device Manager Service proxy object
     *
     * @internal
     */
    public void closeDeviceManager(BleDeviceManager proxy) {
        if (DBG) Log.d(TAG, "closeDeviceManager");

        if (null == proxy) {
            return;
        }

        proxy.close();

        return;
    }

    /**
     * Get the profile proxy object associated with a specified profile service.
     *
     * @return true on success, false on error
     *
     * @internal
     */
    public boolean getProfileServiceProxy(Context ctxt, int profile,
            BleProfileService.ProfileServiceListener listener) {
        if (DBG) Log.d(TAG, "getProfileService");

        if (null == ctxt || null == listener) {
            return false;
        }

        if (BleProfile.TIP == profile) {
            if (VDBG) Log.v(TAG, "getProfileService: BLE_PROFILE_TIP");
            new BleTimeProfileService(ctxt, listener);
            return true;
        } else if (BleProfile.PXP == profile) {
            if (VDBG) Log.v(TAG, "getProfileService: BLE_PROFILE_PXP");
            new BleProximityProfileService(ctxt, listener);
            return true;
        } else if (BleProfile.ANP == profile) {
            if (VDBG) {
                Log.v(TAG, "getProfileService: BLE_PROFILE_ANP");
            }
            new BleAlertNotificationProfileService(ctxt, listener);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close the connection of the proxy to a profile service.
     *
     * <p>
     * Clients should call this when they are no longer using the proxy
     *
     * @param profile
     * @param proxy
     *            Profile Service proxy object
     *
     * @internal
     */
    public void closeProfileServiceProxy(int profile, BleProfileService proxy) {
        if (DBG) Log.d(TAG, "closeProfileServiceProxy: profile=" + profile + " proxy=" + proxy);

        if (null == proxy) {
            return;
        }

        if (BleProfile.TIP == profile && proxy instanceof BleTimeProfileService) {
            ((BleTimeProfileService) proxy).close();
        } else if (BleProfile.PXP == profile
                && proxy instanceof BleProximityProfileService) {
            ((BleProximityProfileService) proxy).close();
        } else if (BleProfile.ANP == profile
                && proxy instanceof BleAlertNotificationProfileService) {
            ((BleAlertNotificationProfileService) proxy).close();
        } else {
            Log.w(TAG, "getProfileService: not match!");
        }
    }

    /**
     * Get the proxy object associated with the Profile Service Manager.
     *
     * @return true on success, false on error
     *
     * @internal
     */
    public boolean getProfileServiceManager(Context ctxt,
            BleProfileServiceManager.ProfileServiceManagerListener listener) {
        if (DBG) Log.d(TAG, "getProfileServiceManager");

        if (null != ctxt && null != listener) {
            new BleProfileServiceManager(ctxt, listener);
            return true;
        }
        return false;
    }

    /**
     * Close the connection of the proxy to the Profile Service Manager.
     *
     * <p>
     * Applications should call this when they are no longer using the proxy
     *
     * @param proxy
     *            Profile Service Manager proxy object
     *
     * @internal
     */
    public void closeProfileServiceManager(BleProfileServiceManager proxy) {
        if (DBG) Log.d(TAG, "closeProfileServiceManager: proxy=" + proxy);

        if (null != proxy) {
            proxy.close();
        }
    }
}
