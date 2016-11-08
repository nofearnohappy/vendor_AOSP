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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetoothle.bleservice;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.mediatek.bluetoothle.R;
import com.mediatek.bluetoothle.anp.AlertNotificationProfileService;
import com.mediatek.bluetoothle.fmp.FmpServerService;
import com.mediatek.bluetoothle.pasp.PaspServerService;
import com.mediatek.bluetoothle.pxp.ProximityProfileService;
import com.mediatek.bluetoothle.tip.TipServerService;

import java.util.ArrayList;

/**
 * Config class for setting Ble profiles
 */
public class Config {
    private static final boolean DBG = true;
    private static final String TAG = "BleConfig";

    /**
     * List of profile services.
     */
    @SuppressWarnings("rawtypes")
    private static final Class[] PROFILE_SERVICES = {
            AlertNotificationProfileService.class,
            FmpServerService.class, ProximityProfileService.class, PaspServerService.class,
            TipServerService.class
    };

    private static final int[] PROFILE_SERVICES_FLAG = {
            R.bool.profile_supported_anp,
            R.bool.profile_supported_fmp, R.bool.profile_supported_pxp,
            R.bool.profile_supported_pasp, R.bool.profile_supported_tip
    };

    @SuppressWarnings("rawtypes")
    private static Class[] sSupportProfiles = new Class[0];

    @SuppressWarnings("rawtypes")
    static void init(final Context ctx) {
        if (ctx == null) {
            return;
        }
        final Resources resources = ctx.getResources();
        if (resources == null) {
            return;
        }
        final ArrayList<Class> profiles = new ArrayList<Class>(PROFILE_SERVICES.length);
        for (int i = 0; i < PROFILE_SERVICES_FLAG.length; i++) {
            final boolean supported = resources.getBoolean(PROFILE_SERVICES_FLAG[i]);
            if (supported) {
                if (DBG) {
                    Log.d(TAG, "Adding " + PROFILE_SERVICES[i].getSimpleName());
                }
                profiles.add(PROFILE_SERVICES[i]);
            }
        }
        final int totalProfiles = profiles.size();
        sSupportProfiles = new Class[totalProfiles];
        profiles.toArray(sSupportProfiles);
    }

    @SuppressWarnings("rawtypes")
    static Class[] getSupportedProfiles() {
        return sSupportProfiles;
    }
}
