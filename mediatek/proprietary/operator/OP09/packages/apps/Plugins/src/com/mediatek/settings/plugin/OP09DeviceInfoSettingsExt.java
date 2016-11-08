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
package com.mediatek.settings.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.settings.ext.DefaultDeviceInfoSettingsExt;
import com.mediatek.custom.CustomProperties;

/**
 * CT feature about E push.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IDeviceInfoSettingsExt")
public class OP09DeviceInfoSettingsExt extends DefaultDeviceInfoSettingsExt {

    private static final String TAG = "OP09DeviceInfoSettingsExt";
    private static final String E_PUSH_KEY = "cdma_epush";
    private static final String KEY_STATUS_INFO = "status_info";
    private static final String KEY_BUILD_NUMBER = "build_number";
        
    private Context mContext;

    /**
     * Construct method.
     * @param context Context
     */
    public OP09DeviceInfoSettingsExt(Context context) {
        mContext = context;
    }

    /**
     *
     * @return E push xml perference
     */
    private Preference getEpushLayout(Context context) {
        PreferenceScreen pref = new PreferenceScreen(context, null);
        pref.setKey(E_PUSH_KEY);
        pref.setTitle(mContext.getResources().getString(R.string.cdma_e_push_title));
        pref.setSummary(mContext.getResources().getString(R.string.cdma_e_push_summary));
        return pref;
    }

    /**
     * Enable or disable the epush preference.
     * when the 3part app(com.ctc.epush) install,
     * enable the epush preference,or disable epush preference
     * @param preferenceScreen,is epush preference
     */
    private void setEpushEnabledOrNot(Preference preferenceScreen) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        ComponentName cn = new ComponentName("com.ctc.epush", "com.ctc.epush.IndexActivity");
        intent.setComponent(cn);
        if (mContext.getPackageManager().resolveActivity(intent, 0) == null) {
            Log.i(TAG, "no com.ctc.epush.IndexActivity");
            if (preferenceScreen != null) {
                preferenceScreen.setEnabled(false);
            }
        }
    }
    
    @Override
    public void updateSummary(Preference preference, String value, String defaultValue) {
        Log.i(TAG, "updateSummary value=" + value);
        Log.i(TAG, "updateSummary defaultValue=" + defaultValue);
        String pref_key_t = null;
        
        pref_key_t = preference.getKey();
        Log.i(TAG, "updateSummary pref_key_t=" + pref_key_t);
        if (preference.getKey().equals(KEY_BUILD_NUMBER)){            
            /// M: Get from custom.conf.
            String update_value = CustomProperties.getString(CustomProperties.MODULE_DM,
                "SoftwareVersion", defaultValue);            
            Log.i(TAG, "updateSummary update_value=" + update_value);
            preference.setSummary(update_value);            
        }
    }    
}
