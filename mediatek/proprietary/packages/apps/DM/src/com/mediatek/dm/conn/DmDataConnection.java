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

package com.mediatek.dm.conn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.mediatek.dm.DmApplication;
import com.mediatek.dm.DmCommonFun;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.ext.MTKConnectivity;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.fumo.DmClient;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.scomo.DmScomoDownloadDetailActivity;
import com.mediatek.dm.session.SessionEventQueue;
import com.mediatek.dm.util.ScreenLock;

//import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Assert;

public final class DmDataConnection {
    private ConnectivityManager mConnMgr;
    private Context mContext;
    private DmDatabase mDmDatabase;

    private int mSubId = -1;

    private static Handler sClientHandler;
    private static Handler sScomoHandler;
    private static Handler sServiceHandler;

    private static DmDataConnection sInstance;

//    private NetworkWatcherThread mNetworkThread;
    public static final int CONN_INTERVAL = 5000;

    // Timeout used to call ConnectivityManager.requestNetwork
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 3 * 60 * 1000;

    // extended message handler
    private Handler mUserMsgHandler;

    /// M: DM connectivity result network
    private Network mNetwork = null;

    public void setUserHandler(Handler hd) {
        mUserMsgHandler = hd;
    }

    private DmDataConnection(Context context) {
        mContext = context;
        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        /// M: Register callback to monitor network
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(MTKPhone.NET_CAPABILITY_INTERNET)
                .build();
        mConnMgr.registerNetworkCallback(networkRequest, mNetworkCallback);

        if (!Options.USE_DIRECT_INTERNET) {
            // init DmDatabase
            mDmDatabase = new DmDatabase(context);
        }
    }

    public static synchronized DmDataConnection getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DmDataConnection(context);
        }
        return sInstance;
    }

    /**
     * Start a DM data connection by using a special APN which configured in DmApnInfo.xml.
     * @return int The result of DM data connection.
     */
    public int startDmDataConnectivity() {
        Assert.assertFalse("startDmDataConnectivity MUST NOT be called in direct internet conn.",
                Options.USE_DIRECT_INTERNET);

        NetworkInfo networkInfo = mConnMgr.getNetworkInfo(mNetwork);
        if (networkInfo != null && networkInfo.isConnected()
                && networkInfo.getType() == MTKConnectivity.TYPE_MOBILE) {
            return MTKPhone.NETWORK_AVAILABLE;
        }

        if (Options.USE_SMS_REGISTER) {
            mSubId = DmCommonFun.getRegisterSubID(mContext);
        } else {
            mSubId = DmCommonFun.getPreferredSubID(mContext);
            Log.d(TAG.CONNECTION, "Prefered Subscription ID: " + mSubId);
        }

        if (mSubId == -1) {
            Log.e(TAG.CONNECTION, "Get Register SIM ID error in start data connection");
            return MTKPhone.NETWORK_UNAVAILABLE;
        }

        if (!mDmDatabase.isDmApnReady(mSubId)) {
            Log.e(TAG.CONNECTION, "Dm apn table is not ready!");
            return MTKPhone.NETWORK_UNAVAILABLE;
        }
        beginDmDataConnectivity(mSubId);

        return MTKPhone.NETWORK_PRECHECK;
    }

    public void stopDmDataConnectivity() {
        Assert.assertFalse("stopDmDataConnectivity MUST NOT be called in direct internet conn.",
                Options.USE_DIRECT_INTERNET);
        Log.v(TAG.CONNECTION, "stopDmDataConnectivity");
        try {
            endDmDataConnectivity();

            ScreenLock.releaseWakeLock(mContext);
            ScreenLock.enableKeyguard(mContext);
        } finally {
            Log.v(TAG.CONNECTION, "stopUsingNetworkFeature end");
        }
    }

    ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d(TAG.CONNECTION,
                    "[dm-conn]NetworkCallbackListener.onAvailable: network=" + network);

            NetworkInfo networkInfo = mConnMgr.getNetworkInfo(network);
            if (networkInfo == null) {
                Log.e(TAG.CONNECTION,
                        "[dm-conn]DM type is null or mobile data is turned off, bail");
                return;
            }
            int networkType = networkInfo.getType();

            if (!Options.USE_DIRECT_INTERNET) {
                int slotId = SubscriptionManager.getSlotId(mSubId);
                Log.i(TAG.CONNECTION,
                        new StringBuilder("[dm-conn]->type == ")
                                .append(networkInfo.getTypeName())
                                .append("(").append(networkType).append(")").append("\n")
                                .append("[dm-conn]->subId == ").append(mSubId).append("\n")
                                .append("[dm-conn]->slotId == ").append(slotId)
                                .toString());

                if (networkType != MTKConnectivity.TYPE_MOBILE) {
                    Log.e(TAG.CONNECTION, "[dm-conn]->Connect type is incorrect");
                    return;
                }

            } else {
                Log.i(TAG.CONNECTION,
                        new StringBuilder("[dm-conn]->type == ")
                                .append(networkInfo.getTypeName())
                                .append("(").append(networkType).append(")").toString());
                if (networkType != MTKConnectivity.TYPE_MOBILE
                        && networkType != MTKConnectivity.TYPE_WIFI) {
                    Log.e(TAG.CONNECTION, "connect type is incorrect");
                    return;
                }
            }

            if (networkInfo.isConnected()) { // TODO: swap
                Log.i(TAG.CONNECTION, "[dm-conn]->state == CONNECTED");

                synchronized (mNetworkCallback) {
                    if (!Options.USE_DIRECT_INTERNET) {
                        mConnMgr.setProcessDefaultNetwork(network);
                        mNetwork = network;
                    }

                    // store CONNECTED event.
                    DmApplication.getInstance().queueEvent(SessionEventQueue.EVENT_CONN_CONNECTED);
                    Log.i(TAG.CONNECTION, ">>sending msg WAP_CONN_SUCCESS");
                    notifyHandlers(IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS);
                }
            } else {
                Log.e(TAG.CONNECTION, "[dm-conn]TYPE_MOBILE_DM not connected, state == "
                        + networkInfo.getState());
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            NetworkInfo networkInfo = mConnMgr.getNetworkInfo(network);
            if (networkInfo == null) {
                Log.e(TAG.CONNECTION, "[dm-conn]DM type is null or mobile data is turned off!");
                return;
            }

            int networkType = networkInfo.getType();
            int slotId = SubscriptionManager.getSlotId(mSubId);

            Log.i(TAG.CONNECTION,
                    new StringBuilder("[dm-conn]->type == ")
                            .append(networkInfo.getTypeName())
                            .append("(").append(networkType).append(")").append("\n")
                            .append("[dm-conn]->subId == ").append(mSubId).append("\n")
                            .append("[dm-conn]->slotId == ").append(slotId)
                            .toString());
            Log.i(TAG.CONNECTION, "[dm-conn]->state == " + networkInfo.getState());

            synchronized (mNetworkCallback) {
                if (!Options.USE_DIRECT_INTERNET) {
                    mNetwork = network;
                }

                // store DISCONNECTED event.
                DmApplication.getInstance().queueEvent(SessionEventQueue.EVENT_CONN_DISCONNECTED);
            }
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            Log.e(TAG.CONNECTION, "[dm-conn]->state == Unavailable");

            synchronized (mNetworkCallback) {
                if (!Options.USE_DIRECT_INTERNET) {
                    endDmDataConnectivity();
                }
                notifyHandlers(IDmPersistentValues.MSG_WAP_CONNECTION_TIMEOUT);
            }
        }
    };

//    private class NetworkWatcherThread extends Thread {
//        public void run() {
//        Log.i(TAG.CONNECTION, "start to run watcher thread");
//
//            try {
//                // sleep 30s to wait for the first dm data connnection completed
//                Thread.sleep(CONN_INTERVAL * 6);
//                int times = 12;
//                if (mConnectivityReceiver.bConn) {
//                    Log.i(TAG.CONNECTION, "[dm-conn] already successful, no need to start again");
//                    return;
//                } else {
//                    Log.i(TAG.CONNECTION, "[dm-conn] is not ready after 30s, need to retry");
//                    Log.i(TAG.CONNECTION, "[dm-conn] stop the dm data connectivity before retry");
//                    stopDmDataConnectivity();
//                }
//
//                int result = MTKPhone.APN_REQUEST_FAILED;
//                while (!mConnectivityReceiver.bConn && times > 0) {
//                    Log.i(TAG.CONNECTION, "[dm-conn] begin the " + times + "retry ");
//                    result = startDmDataConnectivity();
//                    if (result == MTKPhone.APN_ALREADY_ACTIVE) {
//                        break;
//                    } else if (result == MTKPhone.APN_REQUEST_STARTED) {
//                        Log.i(TAG.CONNECTION, "[dm-conn] is waiting, sleep 10 seconds");
//                        // sleep 10s to wait for the dm connection complete
//                        Thread.sleep(CONN_INTERVAL * 2);
//                        if (mConnectivityReceiver.bConn) {
//                            break;
//                        }
//                    } else if (result == MTKPhone.APN_REQUEST_FAILED){
//                        Log.i(TAG.CONNECTION, "[dm-conn] has connected failed");
//                    }
//                    Log.i(TAG.CONNECTION, "[dm-conn] should be stoped before another try");
//                    stopDmDataConnectivity();
//                    times--;
//                }
//                if (result != MTKPhone.APN_ALREADY_ACTIVE) {
//                    Log.e(TAG.CONNECTION, "[dm-conn] has tried for 12 times in 2 minute!");
//                } else {
//                    Log.i(TAG.CONNECTION, "[dm-conn] has been connected successully!");
//                }
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

    private void beginDmDataConnectivity(int subId) {
        Log.i(TAG.CONNECTION, "[dm-conn]->requestNetwork: subId = " + subId);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(MTKPhone.TRANSPORT_TYPE_CELLULAR)
                .addCapability(MTKPhone.NET_CAPABILITY_DM)
                .setNetworkSpecifier(String.valueOf(subId))
                .build();

        mConnMgr.requestNetwork(networkRequest, mNetworkCallback, NETWORK_REQUEST_TIMEOUT_MILLIS);
    }

    private void endDmDataConnectivity() {
        try {
            Log.i(TAG.CONNECTION, "[dm-conn]->unregisterNetworkCallback.");

            if (mConnMgr != null) {
                mConnMgr.unregisterNetworkCallback(mNetworkCallback);
            }

            mNetwork = null;
        } finally {
            Log.i(TAG.CONNECTION, "[dm-conn]->unregisterNetworkCallback end");
        }
    }

    public static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8) | (addrBytes[0] & 0xff);
        return addr;
    }

    private void notifyHandlers(int msgCode) {
        sClientHandler = null;
        sScomoHandler = null;
        if (DmClient.getVdmClientInstance() != null) {
            sClientHandler = DmClient.getVdmClientInstance().mApnConnHandler;
        }

        if (DmScomoDownloadDetailActivity.getInstance() != null) {
            sScomoHandler = DmScomoDownloadDetailActivity.getInstance().mApnConnHandler;
        }

        if (sServiceHandler == null) {
            if (DmService.getInstance() != null) {
                sServiceHandler = DmService.getInstance().mHandler;
            }
        }
        if (sClientHandler != null) {
            sClientHandler.sendMessage(sClientHandler.obtainMessage(msgCode));
        }
        if (sScomoHandler != null) {
            sScomoHandler.sendMessage(sScomoHandler.obtainMessage(msgCode));
        }
        if (sServiceHandler != null) {
            sServiceHandler.sendMessage(sServiceHandler.obtainMessage(msgCode));
        }

        // extended message handler
        if (mUserMsgHandler != null) {
            mUserMsgHandler.sendMessage(mUserMsgHandler.obtainMessage(msgCode));
        }
    }

    private void destroyDataConnection() {
        mContext = null;
        if (mConnMgr != null) {
            mConnMgr.unregisterNetworkCallback(mNetworkCallback);
        }
    }

    public static void destroyInstance() {
        if (sInstance != null) {
            sInstance.destroyDataConnection();
            sInstance = null;
        }
    }

}
