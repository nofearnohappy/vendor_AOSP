/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.mediatek.ims;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.SystemClock;
//import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Manages the XCAP mobile data network connectivity.
 */
public class XcapMobileDataNetworkManager {
    private static final String LOG_TAG = "XcapMobileDataNetworkManager";

    // Timeout used to call ConnectivityManager.requestNetwork
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 10 * 1000; // previous: 8 * 1000
    // Wait timeout for this class, a little bit longer than the above timeout
    // to make sure we don't bail prematurely
    private static final int NETWORK_ACQUIRE_TIMEOUT_MILLIS =
            NETWORK_REQUEST_TIMEOUT_MILLIS + (2 * 1000);

    private Context mContext;
    // The requested XCAP mobile data network {@link android.net.Network} we are holding
    // We need this when we unbind from it. This is also used to indicate if the
    // XCAP mobile data network network is available.
    private Network mNetwork;
    // The current count of XCAP mobile data network requests that require the network
    // If mXcapMobileDataNetworkRequestCount is 0, we should release the XCAP mobile data network.
    private int mXcapMobileDataNetworkRequestCount;

    // The callback to register when we request XCAP mobile data network
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    private ConnectivityManager mConnectivityManager;

    // TODO: we need to re-architect this when we support MSIM, like maybe one manager for each SIM?
    /**
     * Manages the XCAP mobile data network connectivity.
     * @param context context
     */
    public XcapMobileDataNetworkManager(Context context) {
        mContext = context;
        mNetworkCallback = null;
        mNetwork = null;
        mXcapMobileDataNetworkRequestCount = 0;
        mConnectivityManager = null;
    }

    /**
     * Get the network acquired.
     * @return mNetwork
     */
    public Network getNetwork() {
        synchronized (this) {
            return mNetwork;
        }
    }

    /**
     * Acquire the XCAP mobile data network.
     * @param phoneId the phone index
     * @return the acquired network
     */
    public Network acquireNetwork(int phoneId) {
        Rlog.d(LOG_TAG, "acquireNetwork(): phoneId = " + phoneId);
        if (inAirplaneMode()) {
            // Fast fail airplane mode
            return null;
        }
        if (!(MMTelSSUtils.isOp01IccCard(phoneId))
                && !(MMTelSSUtils.isOp03IccCard(phoneId))
                && !(MMTelSSUtils.isOp05IccCard(phoneId))
                && !(MMTelSSUtils.isOp06IccCard(phoneId))
                && !(MMTelSSUtils.isOp07IccCard(phoneId))
                && !(MMTelSSUtils.isOp15IccCard(phoneId))
                && !(MMTelSSUtils.isOp18IccCard(phoneId))
//                && !(SystemProperties.getInt("ro.mtk_epdg_support", 0) == 1)
                ) {
            Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: acquireNetwork not supported");
            return null;
        }
        Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: acquireNetwork start");
        synchronized (this) {
            mXcapMobileDataNetworkRequestCount += 1;
            if (mNetwork != null) {
                // Already available
                Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: already available");
                return mNetwork;
            }
            Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: start new network request");
            // Not available, so start a new request
            newRequest(phoneId);
            final long shouldEnd = SystemClock.elapsedRealtime()
                    + NETWORK_ACQUIRE_TIMEOUT_MILLIS;
            long waitTime = NETWORK_ACQUIRE_TIMEOUT_MILLIS;
            while (waitTime > 0) {
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: acquire network wait "
                            + "interrupted");
                }
                if (mNetwork != null) {
                    // Success
                    return mNetwork;
                }
                // Calculate remaining waiting time to make sure we wait the full timeout period
                waitTime = shouldEnd - SystemClock.elapsedRealtime();
            }
            // Timed out, so release the request and fail
            Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: timed out");
            releaseRequest(mNetworkCallback);
            resetLocked();
        }

        return null;
    }

    /**
     * Release the XCAP mobile data network when nobody is holding on to it.
     */
    public void releaseNetwork() {
        synchronized (this) {
            if (mXcapMobileDataNetworkRequestCount > 0) {
                mXcapMobileDataNetworkRequestCount -= 1;
                Rlog.d(LOG_TAG, "XcapMobileDataNetworkManager: release, count="
                        + mXcapMobileDataNetworkRequestCount);
                if (mXcapMobileDataNetworkRequestCount < 1) {
                    releaseRequest(mNetworkCallback);
                    resetLocked();
                }
            }
        }
    }

    /**
     * Start a new {@link android.net.NetworkRequest} for XCAP mobile data network.
     */
    private void newRequest(int phoneId) {
        final ConnectivityManager connectivityManager = getConnectivityManager();
        int subId = (int) SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Rlog.d(LOG_TAG, "NetworkCallbackListener.onAvailable: network=" + network);
                synchronized (XcapMobileDataNetworkManager.this) {
                    mNetwork = network;
                    XcapMobileDataNetworkManager.this.notifyAll();
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Rlog.d(LOG_TAG, "NetworkCallbackListener.onLost: network=" + network);
                synchronized (XcapMobileDataNetworkManager.this) {
                    releaseRequest(this);
                    if (mNetworkCallback == this) {
                        resetLocked();
                    }
                    XcapMobileDataNetworkManager.this.notifyAll();
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Rlog.d(LOG_TAG, "NetworkCallbackListener.onUnavailable");
                synchronized (XcapMobileDataNetworkManager.this) {
                    releaseRequest(this);
                    if (mNetworkCallback == this) {
                        resetLocked();
                    }
                    XcapMobileDataNetworkManager.this.notifyAll();
                }
            }
        };
        Rlog.d(LOG_TAG, "newRequest, subId = " + subId);
        NetworkRequest.Builder networkBuilder = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .setNetworkSpecifier(Integer.toString(subId));

        //if (SystemProperties.getInt("ro.mtk_epdg_support", 0) == 1) {
        //    networkBuilder.addTransportType(NetworkCapabilities.TRANSPORT_EPDG);
        //}

        if ((MMTelSSUtils.isOp01IccCard(phoneId))
                || (MMTelSSUtils.isOp03IccCard(phoneId))
                || (MMTelSSUtils.isOp05IccCard(phoneId))
                || (MMTelSSUtils.isOp06IccCard(phoneId))
                || (MMTelSSUtils.isOp07IccCard(phoneId))
                || (MMTelSSUtils.isOp15IccCard(phoneId))
                || (MMTelSSUtils.isOp18IccCard(phoneId))) {
            networkBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
        }
        NetworkRequest networkRequest = networkBuilder.build();
        connectivityManager.requestNetwork(
                networkRequest, mNetworkCallback, NETWORK_REQUEST_TIMEOUT_MILLIS);
    }

    /**
     * Release the current {@link android.net.NetworkRequest} for XCAP mobile data network.
     *
     * @param callback the {@link android.net.ConnectivityManager.NetworkCallback} to unregister
     */
    private void releaseRequest(ConnectivityManager.NetworkCallback callback) {
        if (callback != null) {
            final ConnectivityManager connectivityManager = getConnectivityManager();
            connectivityManager.unregisterNetworkCallback(callback);
        }
    }

    /**
     * Reset the state.
     */
    private void resetLocked() {
        mNetworkCallback = null;
        mNetwork = null;
        mXcapMobileDataNetworkRequestCount = 0;
    }

    /**
     * Uset the acquired network to route to the XCAP root URI.
     * @param network the acquired network
     * @param xcapRootUri XCAP root URI
     * @param phoneId phone index
     */
    public void useAcquiredNetwork(Network network, String xcapRootUri, int phoneId) {
        String xcapSrvHostName = null;
        if (xcapRootUri != null) {
            if (xcapRootUri.startsWith("http://")) {
                xcapSrvHostName = xcapRootUri.substring(7, xcapRootUri.lastIndexOf("/"));
            } else if (xcapRootUri.startsWith("https://")) {
                xcapSrvHostName = xcapRootUri.substring(8, xcapRootUri.lastIndexOf("/"));
            } else {
                xcapSrvHostName = xcapRootUri.substring(0, xcapRootUri.lastIndexOf("/"));
            }
        }

        if (xcapSrvHostName != null) {
            int portStartIndex = xcapSrvHostName.lastIndexOf(":");
            if (portStartIndex != -1) {
                xcapSrvHostName = xcapSrvHostName.substring(0, portStartIndex);
            }
        }

        Rlog.d(LOG_TAG, "useAcquiredNetwork(): xcapRootUri = " + xcapRootUri
                + ", xcapSrvHostName=" + xcapSrvHostName);

        if (xcapSrvHostName != null) {
            int networkType = ConnectivityManager.TYPE_MOBILE_XCAP;
            //if (SystemProperties.getInt("ro.mtk_epdg_support", 0) == 1) {
            //    networkType = ConnectivityManager.TYPE_WIFI;
            //}
            try {
                for (final InetAddress address : network.getAllByName(xcapSrvHostName)) {
                    if (!getConnectivityManager().requestRouteToHostAddress(
                            networkType, address)) {
                        Rlog.e(LOG_TAG, "useAcquiredNetwork(): requestRouteToHostAddress() failed");
                        return;
                    }
                }
            } catch (UnknownHostException ex) {
                Rlog.e(LOG_TAG, "useAcquiredNetwork(): UnknownHostException");
            }
        }
    }

    private ConnectivityManager getConnectivityManager() {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        }
        return mConnectivityManager;
    }

    private boolean inAirplaneMode() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
}
