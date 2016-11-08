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

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.util.Log;

import android.test.AndroidTestCase;

/**
 * Test Wifi soft AP configuration
 */
public class WifiSoftAPTest extends AndroidTestCase {

    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private final String TAG = "WifiSoftAPTest";
    private final int DURATION = 10000;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            Thread.sleep(3000);
        }
        assertTrue(mWifiManager.setWifiApEnabled(null, true));
        Thread.sleep(5000);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        if (mWifiConfig != null) {
            Log.d(TAG, "mWifiConfig is " + mWifiConfig.toString());
        } else {
            Log.d(TAG, "mWifiConfig is null.");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG, "turn off wifi tethering");
        mWifiManager.setWifiApEnabled(null, false);
        Thread.sleep(3000);
        super.tearDown();
    }

    public void testWifiSoftAP() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "abcdefghijklmnopqrstuvwxyz";
        config.allowedKeyManagement.set(KeyMgmt.NONE);
        mWifiConfig = config;
        assertTrue(mWifiManager.setWifiApConfiguration(mWifiConfig));
        assertTrue(mWifiManager.setWifiApEnabled(mWifiConfig, true));
        try {
            Thread.sleep(DURATION);
        } catch (InterruptedException e) {
            Log.e(TAG, "exception " + e.getStackTrace());
            assertFalse(true);
        }
        assertTrue(mWifiManager.isWifiApEnabled());
        assertNotNull(mWifiManager.getWifiApConfiguration());
        assertEquals("wifi AP state is enabled", WifiManager.WIFI_AP_STATE_ENABLED,
                     mWifiManager.getWifiApState());
    }
}
