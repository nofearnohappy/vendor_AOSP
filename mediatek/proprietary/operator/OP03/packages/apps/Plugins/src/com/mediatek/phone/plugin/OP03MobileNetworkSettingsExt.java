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

package com.mediatek.phone.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.mediatek.common.PluginImpl;
import com.mediatek.op03.plugin.R;
import com.mediatek.phone.ext.DefaultMobileNetworkSettingsExt;

/**
 * Plugin implementation for OP03.
 */
@PluginImpl(interfaceName="com.mediatek.phone.ext.IMobileNetworkSettingsExt")
public class OP03MobileNetworkSettingsExt extends DefaultMobileNetworkSettingsExt {

//    private AlertDialog.Builder mDialogBuild;
    private static final String TAG = "OP03MobileNetworkSettingsExt";
    private final static int DATA_ENABLE_ALERT_DIALOG = 100;
    private final static int DATA_DISABLE_ALERT_DIALOG = 200;
    private static final int PROGRESS_DIALOG = 400;
    private static final int DATA_STATE_CHANGE_TIMEOUT = 2001;
    private static final int RESET_DATA_CONNECTION = 2002;
    public static final String BUTTON_NETWORK_MODE_KEY = "gsm_umts_preferred_network_mode_key";
    public static final String BUTTON_NETWORK_MODE_EX_KEY = "button_network_mode_ex_key";
    public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    public static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    public static final String BUTTON_OP03_2G_ONLY = "button_prefer_op03_2g_key";
    public static final String BUTTON_NETWORK_MODE_LTE_KEY = "button_network_mode_LTE_key";

    private final Context mContext;

    /**
     * Plugin implementation for CallfeatureSettings.
     * @param context context
     */
    public OP03MobileNetworkSettingsExt(Context context) {
        mContext = context;
    }

    /*
    @Override
    public void initOtherMobileNetworkSettings(PreferenceActivity activity, int subId) {
        PreferenceScreen prefSet = activity.getPreferenceScreen();
        Preference buttonPreferredNetworkModeEx = prefSet.findPreference(BUTTON_NETWORK_MODE_EX_KEY);
        Preference buttonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        Preference buttonGsmUmtsPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_NETWORK_MODE_KEY);
        Preference buttonLtePreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_NETWORK_MODE_LTE_KEY);
        Preference buttonEnabledNetworks = (ListPreference) prefSet.findPreference(
                BUTTON_ENABLED_NETWORKS_KEY);

        if (buttonPreferredNetworkModeEx != null) {
            prefSet.removePreference(buttonPreferredNetworkModeEx);
        }
        if (buttonPreferredNetworkMode != null) {
            prefSet.removePreference(buttonPreferredNetworkMode);
        }
        if (buttonGsmUmtsPreferredNetworkMode != null) {
            prefSet.removePreference(buttonGsmUmtsPreferredNetworkMode);
        }
        if (buttonLtePreferredNetworkMode != null) {
            prefSet.removePreference(buttonLtePreferredNetworkMode);
        }
        if (buttonEnabledNetworks != null) {
            prefSet.removePreference(buttonEnabledNetworks);
        }

       if (prefSet.findPreference(BUTTON_OP03_2G_ONLY) == null) {
            SwitchPreference buttonPreferredGSMOnly = new Use2GOnlyCheckBoxPreference(mContext, prefSet);
            buttonPreferredGSMOnly.setKey(BUTTON_OP03_2G_ONLY);
            buttonPreferredGSMOnly.setTitle(mContext.getString(R.string.prefer_2g));
            buttonPreferredGSMOnly.setPersistent(false);
            prefSet.addPreference(buttonPreferredGSMOnly);
        }
    } */

    public boolean onPreferenceTreeClick(PreferenceScreen prefSet, Preference preference) {
        Preference buttonPreferredGSMOnly = (SwitchPreference) prefSet.findPreference(BUTTON_OP03_2G_ONLY);
        if (preference == buttonPreferredGSMOnly) {
            return true;
        } else {
            return false;
        }
    }

    public void customizeAlertDialog(Preference preference, AlertDialog.Builder builder) {
            builder.setMessage(mContext.getString(R.string.network_roaming_warning));
            builder.setTitle(mContext.getString(R.string.network_roaming_warning_title));
    }

}

