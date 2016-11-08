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
 * Copyright (C) 2008 The Android Open Source Project
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
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.test.AndroidTestCase;


public class WifiInfoTest extends AndroidTestCase {
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
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
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
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

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

    public void testWifiInfoProperties() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        // this test case should in Wifi environment
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        assertNotNull(wifiInfo);
        assertNotNull(wifiInfo.toString());
        SupplicantState.isValidState(wifiInfo.getSupplicantState());
        WifiInfo.getDetailedStateOf(SupplicantState.DISCONNECTED);
        String ssid = wifiInfo.getSSID();
        if (ssid.startsWith("0x") == false) {
            // Non-hex string should be quoted
            assertTrue(ssid.charAt(0) == '"');
            assertTrue(ssid.charAt(ssid.length() - 1) == '"');
        }

        wifiInfo.getBSSID();
        wifiInfo.getIpAddress();
        wifiInfo.getLinkSpeed();
        wifiInfo.getRssi();
        wifiInfo.getHiddenSSID();
        wifiInfo.getMacAddress();
        setWifiEnabled(false);
        ///M: can't not use pollCheck
        Thread.sleep(DURATION);
        wifiInfo = mWifiManager.getConnectionInfo();
        assertEquals(-1, wifiInfo.getNetworkId());
        assertEquals(WifiManager.WIFI_STATE_DISABLED, mWifiManager.getWifiState());

        // M: Add for test
        assertNotNull(wifiInfo.toString());
        WifiInfo testInfo = new WifiInfo(wifiInfo);
        testInfo.setMeteredHint(true);
        assertTrue(testInfo.getMeteredHint());
        //testInfo.setHiddenSSID(true);
//        assertTrue(testInfo.getHiddenSSID());
        assertNull(WifiInfo.removeDoubleQuotes(null));
        assertNotNull(WifiInfo.removeDoubleQuotes("TEST"));
        assertNotNull(WifiInfo.removeDoubleQuotes("\"TEST\""));
    }

}
