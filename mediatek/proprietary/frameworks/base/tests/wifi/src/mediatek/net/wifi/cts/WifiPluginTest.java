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
 * MediaTek Inc. (C) 2010. All rights reserved.
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.provider.Settings;
import android.test.AndroidTestCase;

import com.mediatek.common.MPlugin;
import com.mediatek.common.wifi.IWifiFwkExt;

public class WifiPluginTest extends AndroidTestCase {
    private IWifiFwkExt mWifiExt;

    private static class MySync {
        int expectedState = STATE_NULL;
    }

    private WifiManager mWifiManager;
    private WifiLock mWifiLock;
    private static MySync mMySync;

    private static final int STATE_NULL = 0;
    private static final int STATE_WIFI_CHANGING = 1;
    private static final int STATE_WIFI_CHANGED = 2;

    private static final String TAG = "WifiInfoTest";
    private static final int TIMEOUT_MSEC = 6000;
    private static final int WAIT_MSEC = 60;
    private static final int DURATION = 10000;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                synchronized (mMySync) {
                    mMySync.expectedState = STATE_WIFI_CHANGED;
                    mMySync.notify();
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
        mWifiLock = mWifiManager.createWifiLock(TAG);
        mWifiLock.acquire();
        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        Thread.sleep(DURATION);
        assertTrue(mWifiManager.isWifiEnabled());
        mMySync.expectedState = STATE_NULL;

        mWifiExt = MPlugin.createInstance(IWifiFwkExt.class.getName(), mContext);
    }

    @Override
    protected void tearDown() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            super.tearDown();
            return;
        }
        mWifiLock.release();
        mContext.unregisterReceiver(mReceiver);
        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        Thread.sleep(DURATION);
        super.tearDown();
    }

    private void setWifiEnabled(boolean enable) throws Exception {
        synchronized (mMySync) {
            mMySync.expectedState = STATE_WIFI_CHANGING;
            assertTrue(mWifiManager.setWifiEnabled(enable));
            long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
            while (System.currentTimeMillis() < timeout
                    && mMySync.expectedState == STATE_WIFI_CHANGING)
                mMySync.wait(WAIT_MSEC);
        }
    }

    public void testWifiPlugin() throws Exception {
        assertTrue(mWifiExt != null);
        mWifiExt.init();
        if (mWifiExt.hasCustomizedAutoConnect()) {
            assertTrue(mWifiExt.hasCustomizedAutoConnect());
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            int connectType = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_AP_TYPE, Settings.System.WIFI_CONNECT_AP_TYPE_AUTO);
            int cellToWlan = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
            if (connectType == Settings.System.WIFI_CONNECT_AP_TYPE_AUTO
                && (info == null
                || (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE && cellToWlan == Settings.System.WIFI_CONNECT_TYPE_AUTO))) {
                assertTrue(mWifiExt.shouldAutoConnect());
            } else {
                assertFalse(mWifiExt.shouldAutoConnect());
            }
            assertTrue(mWifiExt.defaultFrameworkScanIntervalMs() == 15000);
        } else {
            assertFalse(mWifiExt.hasCustomizedAutoConnect());
            assertTrue(mWifiExt.shouldAutoConnect());
            assertTrue(mWifiExt.defaultFrameworkScanIntervalMs() == mContext.getResources().getInteger(com.android.internal.R.integer.config_wifi_framework_scan_interval));
        }
    }
}
