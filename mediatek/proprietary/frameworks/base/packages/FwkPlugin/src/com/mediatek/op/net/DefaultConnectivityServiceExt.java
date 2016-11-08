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

package com.mediatek.op.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.mediatek.common.MPlugin;
import com.mediatek.common.PluginImpl;
import com.mediatek.common.net.IConnectivityServiceExt;
import com.mediatek.common.telephony.ILteDataOnlyController;
/**
 * Interface that defines all methos which are implemented in ConnectivityService
 */

 /** {@hide} */
@PluginImpl(interfaceName = "com.mediatek.common.net.IConnectivityServiceExt")
public class DefaultConnectivityServiceExt implements IConnectivityServiceExt
{
    private static final String TAG = "CDS/IConnectivityServiceExt";
    protected Context mContext;
    private ILteDataOnlyController mLteDataOnlyControllerExt;

    public void init(Context context) {
        log("init in default");
        mContext = context;
        mLteDataOnlyControllerExt = MPlugin.createInstance(ILteDataOnlyController.class.getName(),
                context);
    }

    public void UserPrompt() {
        log("default UserPrompt");
    }

    public boolean isControlBySetting(int netType, int radio) {
         log("isControlBySetting: netType=" + netType + " readio=" + radio);
        if (radio == ConnectivityManager.TYPE_MOBILE
             /*&& (netType != ConnectivityManager.TYPE_MOBILE_DM)*/
             && (netType != ConnectivityManager.TYPE_MOBILE_MMS)) {
             // TODO L migration marked first
             return true;
        }

        return false;
    }

    @Override
    public boolean ignoreRequest(Object networkCapabilities) {
        log("ignoreRequest: enter");
        NetworkCapabilities nc = (NetworkCapabilities) networkCapabilities;
        if (nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                && nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (mLteDataOnlyControllerExt != null) {
                int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                try {
                    subId = Integer.parseInt(nc.getNetworkSpecifier());
                } catch (NumberFormatException e) {
                    log("ignoreRequest(), NumberFormatException!");
                }

                log("ignoreRequest(), subId: " + subId);
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    return (!mLteDataOnlyControllerExt.checkPermission(subId));
                } else {
                    return (!mLteDataOnlyControllerExt.checkPermission());
                }
            }
        }
        log("ignoreRequest: return false");
        return false;
    }

    private void log(String s) {
        Log.d(TAG, s);
    }
}
