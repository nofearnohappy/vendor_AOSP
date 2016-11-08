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

package com.mediatek.dm.ext;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.IBinder;
import android.os.ServiceManager;
import android.provider.Telephony.Carriers;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.dm.DmAgent;

public final class MTKPhone {
    public static final int NETWORK_PRECHECK = ConnectivityManager.CALLBACK_PRECHECK; //1
    public static final int NETWORK_AVAILABLE = ConnectivityManager.CALLBACK_AVAILABLE; //2
    public static final int NETWORK_LOST = ConnectivityManager.CALLBACK_LOST; //4
    public static final int NETWORK_UNAVAILABLE = ConnectivityManager.CALLBACK_UNAVAIL; //5

    public static final int TRANSPORT_TYPE_CELLULAR = NetworkCapabilities.TRANSPORT_CELLULAR;
    public static final int TRANSPORT_TYPE_WIFI = NetworkCapabilities.TRANSPORT_WIFI;
    public static final int NET_CAPABILITY_DM = NetworkCapabilities.NET_CAPABILITY_DM;
    public static final int NET_CAPABILITY_INTERNET = NetworkCapabilities.NET_CAPABILITY_INTERNET;

    public static final Uri CONTENT_URI_DM = Carriers.CONTENT_URI_DM;

    public static final String SUBSCRIPTION_KEY = PhoneConstants.SUBSCRIPTION_KEY;

    public static DmAgent getDmAgent() {
        IBinder binder = ServiceManager.getService("DmAgent");
        if (binder == null) {
            Log.e("MTKPhone", "ServiceManager.getService(DmAgent) failed.");
            return null;
        }
        DmAgent agent = DmAgent.Stub.asInterface(binder);
        return agent;
    }
}
