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

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.content.SharedPreferences;
import android.content.Intent;
import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultStatusBarPlmnDisplayExt;

@PluginImpl(interfaceName="com.mediatek.settings.ext.IStatusBarPlmnDisplayExt")
public class PlmnDisplaySettingsExt extends DefaultStatusBarPlmnDisplayExt {
    private static final String TAG = "PlmnDisplaySettingsExt";
    private Context mContext = null;
    private static final int INDEX = 2;
    private static final int mID = 1;
    static final String PREFERENCES = "PlmnDisplaySettings";
    static final String TRUE = "true";
    static final String FALSE = "false";
    private boolean mCheckBoxStatus = false;
    private LinearLayout mLinearLayout;
    private CheckBox mHideDevice;
    private CheckBoxPreference mcheckboxPref = null;
    private static boolean mPlmnDisplaySetting = false;
    public static String STRPLMN = "displayplmnSetting";
    public static String PLMNSETTING = "plmnSetting";
    public static String PLMNSETTINGCHECK = "plmnSettingCheck";
    public static final String ACTION_PLMN_CHANGED =
        "com.mediatek.settings.plugin.PLMN_TEXT_CHANGED";
    private final String PLMN_SETTING_STATE = "plmn_setting_state";
    SharedPreferences mPref = null;
    private final String prefName = "OP03SettingsPreference";
    private final String MSHOWTEXT = "mshowText";

    public PlmnDisplaySettingsExt(Context context) {
        super(context);
        mContext = context;
    }

    public void createCheckBox(PreferenceScreen pref, int order) {
        CheckBoxPreference mcheckboxPref = new CheckBoxPreference(pref.getContext());
        Log.d("@M_" + TAG, "into createCheckBox");
        mcheckboxPref.setKey("plmn_display");
        mcheckboxPref.setTitle("Display operator name");
        mcheckboxPref.setSummary("Show operator name on lock screen and status bar");
        mcheckboxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            mPlmnDisplaySetting = (Boolean) newValue;
            Log.d("@M_" + TAG, "set value for boolean =" + (Boolean) newValue);
            //s.setShowNotification(show_num, b, getApplicationContext());
            SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(STRPLMN, mPlmnDisplaySetting);
            editor.commit();
            Intent intent = new Intent(PLMNSETTING);
            if (mPlmnDisplaySetting)
                intent.putExtra(PLMNSETTINGCHECK, TRUE);
            else
              intent.putExtra(PLMNSETTINGCHECK, FALSE);

            intent.setAction(ACTION_PLMN_CHANGED);
            sendBroadcast(intent);
            return true;
        }
    });
        Log.d("@M_" + TAG, "add the mcheckboxPref to preference");
        Log.d("@M_" + TAG, "order =" + order);
        mcheckboxPref.setOrder(order++);
        pref.addPreference(mcheckboxPref);
        //check here whether need to turn check box on or off
        boolean check = getShowTextParameter();
        mcheckboxPref.setChecked(check);
    }

    public void addCheckBox(Context context, View mView, int id) {

    }

   public static boolean getPlmnSetting() {
   Log.d("@M_" + TAG, "return mPlmnDisplaySetting " + mPlmnDisplaySetting);
   return mPlmnDisplaySetting;
   }

   public boolean getShowTextParameter() {
       int showText = android.provider.Settings.System.getInt(mContext.getContentResolver(), MSHOWTEXT, -1);
       Log.d("@M_" + TAG, "getShowTextParameter" + showText);
       if (showText == 1)
        return true;
       else
        return false;

   }
}
