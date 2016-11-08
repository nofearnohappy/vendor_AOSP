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

package mediatek.net.wifi.cts;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.Status;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.List;
import android.content.ContentResolver;
import android.provider.Settings;



public class WifiScanModeTest extends AndroidTestCase {
    private static class MySync {
        int expectedState = STATE_NULL;
    }

    private static final int STATE_START_SCAN = 3;


    private WifiManager mWifiManager;
    private static MySync mMySync;
    private List<ScanResult> mScanResult = null;
    private NetworkInfo mNetworkInfo;


    private static final int STATE_NULL = 0;
    private static final int STATE_WIFI_CHANGING = 1;
    private static final int STATE_WIFI_ENABLED = 2;
    private static final int STATE_WIFI_DISABLED = 3;
    private static final int STATE_SCANNING = 4;
    private static final int STATE_SCAN_RESULTS_AVAILABLE = 5;

    private static final String TAG = "WifiManagerTest";
    private static final int TIMEOUT_MSEC = 6000;
    private static final int WAIT_MSEC = 60;
    private static final int DURATION = 10000;
    private IntentFilter mIntentFilter;

    private ContentResolver mContentResolver;
    private static final int SCAN_WAIT_MSEC = 10000;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                synchronized (mMySync) {
                    if (mWifiManager.getScanResults() != null) {
                        mScanResult = mWifiManager.getScanResults();
                        mMySync.expectedState = STATE_SCAN_RESULTS_AVAILABLE;
                        mScanResult = mWifiManager.getScanResults();
                        mMySync.notifyAll();
                    }
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int newState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);
                synchronized (mMySync) {
                    if (newState == WifiManager.WIFI_STATE_ENABLED) {
                        Log.d(TAG, "*** New WiFi state is ENABLED ***");
                        mMySync.expectedState = STATE_WIFI_ENABLED;
                        mMySync.notifyAll();
                    } else if (newState == WifiManager.WIFI_STATE_DISABLED) {
                        Log.d(TAG, "*** New WiFi state is DISABLED ***");
                        mMySync.expectedState = STATE_WIFI_DISABLED;
                        mMySync.notifyAll();
                    }
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                synchronized (mMySync) {
                    mNetworkInfo =
                            (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED)
                        mMySync.notifyAll();
                }
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        mMySync = new MySync();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);

        mContext.registerReceiver(mReceiver, mIntentFilter);
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);

        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        Thread.sleep(DURATION);
        assertTrue(mWifiManager.isWifiEnabled());
        synchronized (mMySync) {
            mMySync.expectedState = STATE_NULL;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            super.tearDown();
            return;
        }
        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        mContext.unregisterReceiver(mReceiver);
        Thread.sleep(DURATION);
        super.tearDown();
    }

    private void setWifiEnabled(boolean enable) throws Exception {
        synchronized (mMySync) {
            assertTrue(mWifiManager.setWifiEnabled(enable));
            if (mWifiManager.isWifiEnabled() != enable) {
                mMySync.expectedState = STATE_WIFI_CHANGING;
                long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
                int expectedState = (enable ? STATE_WIFI_ENABLED : STATE_WIFI_DISABLED);
                while (System.currentTimeMillis() < timeout
                        && mMySync.expectedState != expectedState)
                    mMySync.wait(WAIT_MSEC);
            }
        }
    }

    private void startScan() throws Exception {
        synchronized (mMySync) {
            mMySync.expectedState = STATE_SCANNING;
            assertTrue(mWifiManager.startScan());
            long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
            while (System.currentTimeMillis() < timeout && mMySync.expectedState == STATE_SCANNING)
                mMySync.wait(WAIT_MSEC);
        }
    }

    private void connectWifi() throws Exception {
        synchronized (mMySync) {
            if (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED) return;
            assertTrue(mWifiManager.reconnect());
            long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
            while (System.currentTimeMillis() < timeout
                    && mNetworkInfo.getState() != NetworkInfo.State.CONNECTED)
                mMySync.wait(WAIT_MSEC);
            assertTrue(mNetworkInfo.getState() == NetworkInfo.State.CONNECTED);
        }
    }

    private boolean existSSID(String ssid) {
        for (final WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
            if (w.SSID.equals(ssid))
                return true;
        }
        return false;
    }

    private int findConfiguredNetworks(String SSID, List<WifiConfiguration> networks) {
        for (final WifiConfiguration w : networks) {
            if (w.SSID.equals(SSID))
                return networks.indexOf(w);
        }
        return -1;
    }

    private void assertDisableOthers(WifiConfiguration wifiConfiguration, boolean disableOthers) {
        for (WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
            if ((!w.SSID.equals(wifiConfiguration.SSID)) && w.status != Status.CURRENT) {
                if (disableOthers)
                    assertEquals(Status.DISABLED, w.status);
            }
        }
    }

    public void testScanModeActions() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }

        //1. set WIFI_SCAN_ALWAYS_AVAILABLE=true
        int preViousSetting = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE,
                0);

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        setWifiEnabled(false);
        Thread.sleep(DURATION);
        assertTrue(mWifiManager.pingSupplicant() == mWifiManager.isScanAlwaysAvailable());
        final String TAG = "Test";

        //2. start scan and get scan result
       long timestamp = 0;
        String BSSID = null;


        scanAndWait();
        scanAndWait();
        scanAndWait();

        List<ScanResult> scanResults = mWifiManager.getScanResults();
        for (ScanResult result : scanResults) {
            BSSID = result.BSSID;
            timestamp = result.timestamp;
            assertTrue(timestamp != 0);
            break;
        }

        scanAndWait();
        scanAndWait();
        scanAndWait();

        scanResults = mWifiManager.getScanResults();
        for (ScanResult result : scanResults) {
            if (result.BSSID.equals(BSSID)) {
                long timeDiff = (result.timestamp - timestamp) / 1000;
                assertTrue(timeDiff > 0);
                assertTrue(timeDiff < 6 * SCAN_WAIT_MSEC);
            }
        }
        //restore setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, preViousSetting);
    }


      private void scanAndWait() throws Exception {
          synchronized (mMySync) {
              mMySync.expectedState = STATE_START_SCAN;
              mWifiManager.startScan();
              waitForBroadcast(SCAN_WAIT_MSEC, STATE_SCAN_RESULTS_AVAILABLE);
          }
     }

     private void waitForBroadcast(long timeout, int expectedState) throws Exception {
        long waitTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < waitTime
                && mMySync.expectedState != expectedState)
            mMySync.wait(WAIT_MSEC);
    }
}
