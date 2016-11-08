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

package com.mediatek.rcs.contacts.networkcontacts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Get network status.
 * @author MTK81350
 *
 */
public class NetworkStatusManager {
    private static final String TAG = "NetworkContacts::NetworkStatusManager";
    private Context mContext = null;
    private ConnectivityManager mConnectivityManager = null;

    /**
     * @param context context
     */
    public NetworkStatusManager(Context context) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * get State of wifi Network,is connected? NetworkInfo.State: CONNECTING,
     * CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, UNKNOWN
     *
     * @return boolean true:CONNECTED; false: other States
     */
    public boolean isWiFiConnected() {
        NetworkInfo netInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (netInfo != null) {
            Log.i(TAG, "WiFi NetworkInfo.State:" + netInfo.getState());
            return netInfo.getState() == NetworkInfo.State.CONNECTED;
        } else {
            return false;
        }
    }

    /**
     * get State of Mobile Network,is connected? NetworkInfo.State: CONNECTING,
     * CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, UNKNOWN
     *
     * @return boolean true:CONNECTED;false: other States
     */
    public boolean isMobileConnected() {
        NetworkInfo netInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (netInfo != null) {
            Log.i(TAG, "Mobile NetworkInfo.State:" + netInfo.getState());
            return netInfo.getState() == NetworkInfo.State.CONNECTED;
        } else {
            return false;
        }
    }

    /**
     * get State of Mobile or wifi Network,is connected? NetworkInfo.State:
     * CONNECTING, CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, UNKNOWN
     *
     * @return boolean true:CONNECTED;false: other States
     */
    public boolean isConnected() {
        return isWiFiConnected() || isMobileConnected();
    }

}
