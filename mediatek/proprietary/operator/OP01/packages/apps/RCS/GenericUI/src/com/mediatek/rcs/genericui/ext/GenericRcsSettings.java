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
package com.mediatek.rcs.genericui.ext;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.android.internal.telephony.SmsApplication;
import com.mediatek.common.PluginImpl;
import com.mediatek.rcs.genericui.R;
import com.mediatek.settings.ext.DefaultRCSSettings;

import java.util.List;


/**
 * Rcs plugin implementation of RCS settings feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IRCSSettings")
public class GenericRcsSettings extends DefaultRCSSettings {
    private static final String TAG = "GenericRcsSettings";
    private static final String KEY_RCS_SETTINGS = "rcs_settings";

    private Context mContext;

    public GenericRcsSettings(Context context) {
        super();
        mContext = context;
        mContext.setTheme(R.style.SettingsPluginBase);
        Log.d(TAG, "GenericRcsSettings()");
    }

    public void addRCSPreference(Activity activity, PreferenceScreen screen) {
        Preference rcsPref = new Preference(mContext);
        rcsPref.setTitle(R.string.rcs_setting_title);
        rcsPref.setKey(KEY_RCS_SETTINGS);
        rcsPref.setOnPreferenceClickListener(mPreferenceClickListener);
        screen.addPreference(rcsPref);
    }

    private Preference.OnPreferenceClickListener mPreferenceClickListener =
            new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            Log.d(TAG, "key=" + key);
            if (KEY_RCS_SETTINGS.equals(key)) {
                Intent start = new Intent("com.mediatek.rcs.genericui.RcsSettingsActivity");
                start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(start);
            }
            return true;
        }
    };

    public boolean isNeedAskFirstItemForSms() {
        Log.d(TAG, "isNeedAskFirstItemForSms:");
        return false;
    }

    public int getDefaultSmsClickContentExt(final List<SubscriptionInfo> subInfoList,
            int value, int subId) {
        Log.d(TAG, "getDefaultSmsClickContent:");
        if (value >= 0 && value < subInfoList.size()) {
            subId = subInfoList.get(value).getSubscriptionId();
        } else if (value >= subInfoList.size()) {
            subId = (int) Settings.System.SMS_SIM_SETTING_AUTO;
        } else {
            Log.d(TAG, "value<0");
        }
        return subId;
    }

    @Override
    public void setDefaultSmsApplication(String packageName, Context context) {
        ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(context, true);

        Log.d("@M_" + TAG, "setDefaultSmsApplication():" + packageName);

        if (!"com.android.mms".equals(packageName)
            && (oldSmsComponent != null)
            && "com.android.mms".equals(oldSmsComponent.getPackageName())) {
            Intent start = new Intent("com.mediatek.rcs.genericui.SmsApplicationDialog");
            start.putExtra("packageName", packageName);
            context.startActivity(start);
        } else {
            SmsApplication.setDefaultApplication(packageName, context);
        }
    }

}


