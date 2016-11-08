/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.net.cts;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ConnectivityManagerTest extends AndroidTestCase {

    private static final String TAG = ConnectivityManagerTest.class.getSimpleName();

    private static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";

    public static final int TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    private static final int HOST_ADDRESS = 0x7f000001; // represent ip 127.0.0.1

    // device could have only one interface: data, wifi.
    private static final int MIN_NUM_NETWORK_TYPES = 1;

    private ConnectivityManager mCm;
    private WifiManager mWifiManager;
    private PackageManager mPackageManager;
    private final HashMap<Integer, NetworkConfig> mNetworks =
            new HashMap<Integer, NetworkConfig>();
    private final List<Integer>mProtectedNetworks = new ArrayList<Integer>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        mPackageManager = getContext().getPackageManager();

        // Get com.android.internal.R.array.networkAttributes
        int resId = getContext().getResources().getIdentifier("networkAttributes", "array", "android");
        String[] naStrings = getContext().getResources().getStringArray(resId);

        for (String naString : naStrings) {
            try {
                NetworkConfig n = new NetworkConfig(naString);
                mNetworks.put(n.type, n);
            } catch (Exception e) { }
        }

        // Get com.android.internal.R.array.config_protectedNetworks
        resId = getContext().getResources().getIdentifier("config_protectedNetworks", "array", "android");
        int[] protectedNetworks = getContext().getResources().getIntArray(resId);
        for (int p : protectedNetworks) {
            mProtectedNetworks.add(p);
        }
    }



    public void testGetNetworkInfo() {
        for (int type = -1; type <= ConnectivityManager.MAX_NETWORK_TYPE + 1; type++) {
            if (isSupported(type)) {
                NetworkInfo ni = mCm.getNetworkInfo(type);
                assertTrue("Info shouldn't be null for " + type, ni != null);
                State state = ni.getState();
                assertTrue("Bad state for " + type, State.UNKNOWN.ordinal() >= state.ordinal()
                           && state.ordinal() >= State.CONNECTING.ordinal());
                DetailedState ds = ni.getDetailedState();
                assertTrue("Bad detailed state for " + type,
                           DetailedState.FAILED.ordinal() >= ds.ordinal()
                           && ds.ordinal() >= DetailedState.IDLE.ordinal());
            } else {
                assertNull("Info should be null for " + type, mCm.getNetworkInfo(type));
            }
        }
    }

    public void testGetAllNetworkInfo() {
        NetworkInfo[] ni = mCm.getAllNetworkInfo();
        assertTrue(ni.length >= MIN_NUM_NETWORK_TYPES);
        for (int type = 0; type <= ConnectivityManager.MAX_NETWORK_TYPE; type++) {
            int desiredFoundCount = (isSupported(type) ? 1 : 0);
            int foundCount = 0;
            for (NetworkInfo i : ni) {
                if (i.getType() == type) foundCount++;
            }
            if (foundCount != desiredFoundCount) {
                Log.e(TAG, "failure in testGetAllNetworkInfo.  Dump of returned NetworkInfos:");
                for (NetworkInfo networkInfo : ni) Log.e(TAG, "  " + networkInfo);
            }
            assertTrue("Unexpected foundCount of " + foundCount + " for type " + type,
                    foundCount == desiredFoundCount);
        }
    }


    private boolean isSupported(int networkType) {
        // Change-Id I02eb5f22737720095f646f8db5c87fd66da129d6 added VPN support
        // to all devices directly in software, independent of any external
        // configuration.
        return mNetworks.containsKey(networkType) ||
               (networkType == ConnectivityManager.TYPE_VPN);
    }

    // true if only the system can turn it on
    private boolean isNetworkProtected(int networkType) {
        return mProtectedNetworks.contains(networkType);
    }

    public void testIsNetworkSupported() {
        for (int type = -1; type <= ConnectivityManager.MAX_NETWORK_TYPE; type++) {
            boolean supported = mCm.isNetworkSupported(type);
            if (isSupported(type)) {
                assertTrue(supported);
            } else {
                assertFalse(supported);
            }
        }
    }



    /** Receiver that captures the last connectivity change's network type and state. */
    private class ConnectivityActionReceiver extends BroadcastReceiver {

        private final CountDownLatch mReceiveLatch = new CountDownLatch(1);

        private final int mNetworkType;

        ConnectivityActionReceiver(int networkType) {
            mNetworkType = networkType;
        }

        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getExtras()
                    .getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            int networkType = networkInfo.getType();
            State networkState = networkInfo.getState();
            Log.i(TAG, "Network type: " + networkType + " state: " + networkState);
            if (networkType == mNetworkType && networkInfo.getState() == State.CONNECTED) {
                mReceiveLatch.countDown();
            }
        }

        public boolean waitForConnection() throws InterruptedException {
            return mReceiveLatch.await(30, TimeUnit.SECONDS);
        }
    }
}
