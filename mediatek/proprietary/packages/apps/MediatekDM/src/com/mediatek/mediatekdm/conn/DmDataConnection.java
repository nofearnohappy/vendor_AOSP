/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.mediatekdm.conn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DmDataConnection implements SessionStateObserver {
    public static final int MOBILE_DATA_ENABLE = 0;
    public static final int MOBILE_DATA_DISABLE = -1;

    private ConnectivityReceiver mConnectivityReceiver = null;
    private ConnectivityManager mConnMgr;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private Context mContext;
    private DmDatabase mDmDatabase;

    private long mSubId = -1;
    private boolean mIsStealDataConn = false;
    private boolean mIsConnected = false;

    private static DmDataConnection sInstance = null;

    private Set<DataConnectionListener> mListeners = new HashSet<DataConnectionListener>();

    private DmDataConnection(Context context) {
        mContext = context;
        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mConnectivityReceiver = new ConnectivityReceiver();
        mDmDatabase = new DmDatabase(context);

        IntentFilter intent = new IntentFilter();
        intent.addAction(DmConst.IntentAction.NET_DETECT_TIMEOUT);
        mContext.registerReceiver(mConnectivityReceiver, intent);
    }

    public static DmDataConnection getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DmDataConnection(context);
            Log.i(TAG.CONNECTION, "sInstance is null, new it.");
        }
        return sInstance;
    }

    public int startDmDataConnectivity() throws IOException {
        Log.i(TAG.CONNECTION, "startDmDataConnectivity");

        mSubId = PlatformManager.getInstance().getRegisteredSubId();
        if (mSubId == -1) {
            Log.e(TAG.CONNECTION, "Get Register SIM ID error in start data connection");
            return MOBILE_DATA_DISABLE;
        }

        if (!mDmDatabase.prepareDmApn(mSubId)) {
            Log.e(TAG.CONNECTION, "Dm apn mTable is not ready!");
            return MOBILE_DATA_DISABLE;
        }

        // Below condition block, is only for Lawmo unlock flow
        if (DmApplication.getInstance().forceSilentMode()) {
            Log.i(TAG.CONNECTION, "Force silent mode, enable mobile data if need.");

            if (!PlatformManager.getInstance().isMobileDataEnabled()) {
                mIsStealDataConn = true;
                MdmEngine.getInstance().registerSessionStateObserver(this);

                PlatformManager.getInstance().enableMoibleData();
            }
        }

        NetworkInfo networkInfo = mConnMgr.getNetworkInfo(PlatformManager.TYPE_MOBILE_DM);

        if (mIsConnected || networkInfo.isConnected()) {
            Log.i(TAG.CONNECTION, "Network ready.");
            notifyHandlers(IServiceMessage.MSG_WAP_CONNECTION_SUCCESS);
            return PlatformManager.APN_ALREADY_ACTIVE;
        } else {
            Log.i(TAG.CONNECTION, "Network not ready, connect.");
            beginDmDataConnectivity();
            return PlatformManager.APN_TYPE_NOT_AVAILABLE;
        }
    }

    public void stopDmDataConnectivity() {
        Log.v(TAG.CONNECTION, "stopDmDataConnectivity");

        endDmDataConnectivity();
        PlatformManager.getInstance().releaseWakeLock(mContext);
        PlatformManager.getInstance().enableKeyguard(mContext);
    }

    public int enableMobileDataForWapPush() {
        Log.i(TAG.CONNECTION, "enableMobileDataForWapPush");

        mSubId = PlatformManager.getInstance().getRegisteredSubId();
        if (mSubId == -1) {
            Log.e(TAG.CONNECTION, "Get Register SIM ID error in start data connection");
            return MOBILE_DATA_DISABLE;
        }

        if (!mDmDatabase.prepareDmApn(mSubId)) {
            Log.e(TAG.CONNECTION, "Dm apn mTable is not ready!");
            return MOBILE_DATA_DISABLE;
        }

        if (DmConfig.getInstance().getCollectSetMsgPermission()) {
            Log.i(TAG.CONNECTION, "Make ensure the mobile is connected");

            if (!PlatformManager.getInstance().isMobileDataEnabled()) {
                PlatformManager.getInstance().enableMoibleData();
            }
        }

        return MOBILE_DATA_ENABLE;
    }

    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null || intent.getAction() == null) {
                return;
            }

            // Send from DmClient (FUMO module)
            if (intent.getAction().equalsIgnoreCase(DmConst.IntentAction.NET_DETECT_TIMEOUT)) {
                Log.i(TAG.CONNECTION,
                        "[dm-conn]->action == com.mediatek.mediatekdm.NETDETECTTIMEOUT");
                Log.i(TAG.CONNECTION, ">>>sending msg WAP_CONN_TIMEOUT");
                notifyHandlers(IServiceMessage.MSG_WAP_CONNECTION_TIMEOUT);
            }
        }
    }

    private void beginDmDataConnectivity() throws IOException {
        Log.v(TAG.CONNECTION, "beginDmDataConnectivity");
        if (mNetworkCallback == null) {
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.i(TAG.CONNECTION, "[dm-conn]->state == Available.");
                    Log.i(TAG.CONNECTION, "[dm-conn]->subId == " + mSubId);
                    mIsConnected = true;

                    PlatformManager.getInstance().releaseWakeLock(mContext);
                    PlatformManager.getInstance().acquirePartialWakelock(mContext);

                    try {
                        ensureRouteToHost(network);
                        notifyHandlers(IServiceMessage.MSG_WAP_CONNECTION_SUCCESS);
                    } catch (IOException ex) {
                        Log.e(TAG.CONNECTION, "[dm-conn]->ensureRouteToHost() failed:", ex);
                    }
                }

                @Override
                public void onLost(Network network) {
                    Log.i(TAG.CONNECTION, "[dm-conn]->state == Lost");
                    mIsConnected = false;
                }

                @Override
                public void onUnavailable() {
                    Log.i(TAG.CONNECTION, "[dm-conn]->state == Unavailable");
                    mIsConnected = false;
                }
            };
        }

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_DM)
                .setNetworkSpecifier(Long.toString(mSubId)).build();

        Log.i(TAG.CONNECTION, "requestNetwork from sub " + mSubId);

        // When timeout, invoke onUnavailable() callback
        mConnMgr.requestNetwork(networkRequest, mNetworkCallback,
                PlatformManager.DELAY_REQUEST_NETWORK);
    }

    private void endDmDataConnectivity() {
        mIsConnected = false;

        if (mConnMgr != null && mNetworkCallback != null) {
            mConnMgr.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
    }

    private void ensureRouteToHost(Network network) throws IOException {
        Log.v(TAG.CONNECTION, "ensureRouteToHost");
        ConnectivityManager.setProcessDefaultNetwork(network);
    }

    public interface DataConnectionListener {
        void notifyStatus(int status);
    }

    public void registerListener(DataConnectionListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(DataConnectionListener listener) {
        mListeners.remove(listener);
    }

    private void notifyHandlers(int msgCode) {
        for (DataConnectionListener l : mListeners) {
            l.notifyStatus(msgCode);
        }
    }

    public void destroyInstance() {
        try {
            mContext.unregisterReceiver(mConnectivityReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        mContext = null;
        sInstance = null;
        Log.i(TAG.CONNECTION, "-- destroyInstance.");
    }

    @Override
    public void notify(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator) {
        Log.i(TAG.CONNECTION, "DmConnSessionStateObserver");
        Log.i(TAG.CONNECTION, "SessionType = " + type);
        Log.i(TAG.CONNECTION, "SessionState = " + state);
        Log.i(TAG.CONNECTION, "lastError = " + lastError);

        if (type == SessionType.DM && state == SessionState.COMPLETE && mIsStealDataConn
                && !DmApplication.getInstance().forceSilentMode()) {
            Log.i(TAG.CONNECTION, "dm enable mobile data privately, now we should close it");

            PlatformManager.getInstance().disableMoibleData();
            mIsStealDataConn = false;
            MdmEngine.getInstance().unregisterSessionStateObserver(this);
        }
    }
}
