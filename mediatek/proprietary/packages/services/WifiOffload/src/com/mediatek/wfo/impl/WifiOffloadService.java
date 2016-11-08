/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.wfo.impl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;

import android.util.Log;

import com.android.ims.ImsManager;
import com.mediatek.wfo.DisconnectCause;
import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.IWifiOffloadListener;

public class WifiOffloadService extends IWifiOffloadService.Stub {
    static {
        System.loadLibrary("wfo_jni");
    }

    private native void nativeInit();

    private native int nativeGetRatType();

    private native DisconnectCause nativeGetDisconnectCause();

    private native void nativeSetWosProfile(boolean volteEnabled, boolean wfcEnabled,
            boolean wifiEnabled, int wfcMode);

    private native void nativeSetWifiStatus(boolean wifiConnected, String ifNmae, String ipv4, String ipv6, String mac);

    static final String TAG = "WifiOffloadService";

    private Set<IWifiOffloadListener> mListeners = new CopyOnWriteArraySet<IWifiOffloadListener>();

    private Context mContext;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    // from user settings
    private boolean mIsVolteEnabled;
    private boolean mIsWfcEnabled;
    private int mWfcMode;
    private boolean mIsWifiEnabled;

    // wifi state
    private boolean mIsWifiConnected = false;
    private String mWifiApMac = "";
    private String mWifiIpv4Address = "";
    private String mWifiIpv6Address = "";
    private String mIfName ="";

    private SettingsObserver mSettingsObserver = new SettingsObserver(null);

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive action:" + intent.getAction());
            boolean changed = getWifiState(false);
            if (changed) {
                notifyMalWifiState();
            }
        }
    };

    private class SettingsObserver extends ContentObserver {
        private final Uri VOLTE_ENABLED_URI = Settings.Global
                .getUriFor(android.provider.Settings.Global.ENHANCED_4G_MODE_ENABLED);
        private final Uri WFC_ENABLED_URI = Settings.Global
                .getUriFor(android.provider.Settings.Global.WFC_IMS_ENABLED);
        private final Uri WFC_MODE_URI = Settings.Global
                .getUriFor(android.provider.Settings.Global.WFC_IMS_MODE);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        private void register() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(VOLTE_ENABLED_URI, false, this);
            resolver.registerContentObserver(WFC_ENABLED_URI, false, this);
            resolver.registerContentObserver(WFC_MODE_URI, false, this);
        }

        private void unregister() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (VOLTE_ENABLED_URI.equals(uri)) {
                mIsVolteEnabled = ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext);
            }
            if (WFC_ENABLED_URI.equals(uri)) {
                mIsWfcEnabled = ImsManager.isWfcEnabledByUser(mContext);
            }
            if (WFC_MODE_URI.equals(uri)) {
                mWfcMode = ImsManager.getWfcMode(mContext);
            }
            notifyMalUserProfile();
        }
    }

    public WifiOffloadService(Context context) {
        nativeInit();
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = ConnectivityManager.from(mContext);

        mIsVolteEnabled = ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext);
        mIsWfcEnabled = ImsManager.isWfcEnabledByUser(mContext);
        mWfcMode = ImsManager.getWfcMode(mContext);

        // init Wifi state
        getWifiState(true);
        notifyMalWifiState();

        mSettingsObserver.register();
        registerForBroadcast();
    }

    @Override
    public void registerForHandoverEvent(IWifiOffloadListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterForHandoverEvent(IWifiOffloadListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public int getRatType() {
        int result = nativeGetRatType();
        Log.d(TAG, "rat type is " + result);
        return result;
    }

    @Override
    public DisconnectCause getDisconnectCause() {
        DisconnectCause result = nativeGetDisconnectCause();
        Log.d(TAG, "disconnect cause is " + result);
        return result;
    }

    private void registerForBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, filter);
    }

    private boolean getWifiState(boolean forceNotifyProfile) {
        boolean changed = false, isWifiEnabled = false, isWifiConnected = false;
        isWifiEnabled = mWifiManager.isWifiEnabled();
        // check if wifi connected
        for (Network nw : mConnectivityManager.getAllNetworks()) {
            LinkProperties prop = mConnectivityManager.getLinkProperties(nw);
            // MAL only care about wlan
            if (prop == null || prop.getInterfaceName() == null
                    || !prop.getInterfaceName().startsWith("wlan")) {
                continue;
            }
            NetworkInfo nwInfo = mConnectivityManager.getNetworkInfo(nw);
            if (nwInfo != null) {
                isWifiConnected = nwInfo.isConnected();
            }
        }
        if (forceNotifyProfile || (isWifiEnabled != mIsWifiEnabled)) {
            mIsWifiEnabled = isWifiEnabled;
            notifyMalUserProfile();
        }
        if (isWifiConnected != mIsWifiConnected) {
            mIsWifiConnected = isWifiConnected;
            changed = true;
        }
        return changed |= getWifiConnectedInfo();
    }

    private boolean getWifiConnectedInfo() {
        if (mIsWifiConnected == false) {
            mWifiApMac = "";
            mWifiIpv4Address = "";
            mWifiIpv6Address = "";
            mIfName ="";
            return false;
        }
        boolean changed = false;
        String wifiApMac = "", ipv4Address = "", ipv6Address = "", ifName = "";
        // get MAC address of the current access point
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            wifiApMac = wifiInfo.getBSSID();
            if (!mWifiApMac.equals(wifiApMac)) {
                mWifiApMac = (wifiApMac == null ? "" : wifiApMac);
                changed = true;
            }
        }
        // get ip
        for (Network nw : mConnectivityManager.getAllNetworks()) {
            LinkProperties prop = mConnectivityManager.getLinkProperties(nw);
            // MAL only care about wlan
            if (prop == null || prop.getInterfaceName() == null
                    || !prop.getInterfaceName().startsWith("wlan")) {
                continue;
            }
            for (InetAddress address : prop.getAddresses()) {
                if (address instanceof Inet4Address) {
                    ipv4Address = address.getHostAddress();
                } else if (address instanceof Inet6Address && !address.isLinkLocalAddress()) {
                    // Filters out link-local address. If cannot find non-link-local address,
                    // pass empty string to MAL.
                    ipv6Address = address.getHostAddress();
                }
            }
            // get interface name
            ifName = prop.getInterfaceName();
        }
        if (!mWifiIpv4Address.equals(ipv4Address)) {
            mWifiIpv4Address = (ipv4Address == null ? "" : ipv4Address);
            changed = true;
        }
        if (!mWifiIpv6Address.equals(ipv6Address)) {
            mWifiIpv6Address = (ipv6Address == null ? "" : ipv6Address);
            changed = true;
        }
        if (!mIfName.equals(ifName)) {
            mIfName = (ifName == null ? "" : ifName);
            changed = true;
        }
        return changed;
    }

    private void notifyMalUserProfile() {
        Log.d(TAG, "notifyMalUserProfile mIsVolteEnabled:" + mIsVolteEnabled + " mIsWfcEnabled:"
                + mIsWfcEnabled + " mIsWifiEnabled:" + mIsWifiEnabled + " mWfcMode:"
                + mWfcMode);
        nativeSetWosProfile(mIsVolteEnabled, mIsWfcEnabled, mIsWifiEnabled, mWfcMode);
    }

    private void notifyMalWifiState() {
        Log.d(TAG, "notifyMalWifiState mIsWifiConnected:" + mIsWifiConnected + " mIfaceName:"
                + mIfName + " mWifiIpv4Address:" + mWifiIpv4Address + " mWifiIpv6Address:"
                + mWifiIpv6Address + " mWifiApMac:" + mWifiApMac);
        nativeSetWifiStatus(mIsWifiConnected, mIfName, mWifiIpv4Address, mWifiIpv6Address, mWifiApMac);
    }

    /**
     * callback from MAL when IMS PDN handover
     * @param stage
     */
    private void onHandover(int stage, int ratType) {
        for (IWifiOffloadListener listener : mListeners) {
            if (listener != null) {
                try {
                    listener.onHandover(stage, ratType);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException occurs!");
                }
            }
        }
    }

    /**
     * callback from MAL when IMS PDN is lost
     */
    private void onLost() {
        // TODO: broadcast
    }

    /**
     * callback from MAL when IMS PDN is unavailable
     */
    private void onUnavailable() {
        // TODO: broadcast
    }
}
