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
package com.mediatek.phone.callrejection;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;

import com.mediatek.op01.plugin.R;
//import com.mediatek.common.featureoption.FeatureOption;

public class CallRejectSettingActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String LOG_TAG = "CallRejectSettingActivity";
    private static final boolean DBG = true;

    private static final String CALL_REJECT_MODE_KEY = "call_reject_mode_key";
    private static final String CALL_REJECT_LIST_KEY = "call_reject_list_key";
    private static final String CALL_REJECT_TARGET_CLASS = "com.mediatek.settings.CallRejectListSetting";
    private static final int CALL_ALL_NUMBERS = 100;

    private String[] mCallRejectModeArray;
    private ListPreference mRejectSetting;
    private Preference mRejectList;

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.call_reject_setting);
        mCallRejectModeArray = getResources().getStringArray(R.array.call_reject_mode_entries);

        mRejectSetting = (ListPreference) findPreference(CALL_REJECT_MODE_KEY);
        mRejectSetting.setOnPreferenceChangeListener(this);

        mRejectList = (Preference) findPreference(CALL_REJECT_LIST_KEY);
        mRejectList.setOnPreferenceChangeListener(this);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int rejectMode = Settings.System.getInt(getContentResolver(), Settings.System.VOICE_CALL_REJECT_MODE, 0);
        mRejectSetting.setValueIndex(rejectMode);
        mRejectSetting.setSummary(mCallRejectModeArray[rejectMode]);
        mRejectList.setEnabled(rejectMode == 2);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        log("Enter onPreferenceTreeClick function.");
        if (preference == mRejectList) {
            Intent intent = new Intent(this, CallRejectActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        log("Enter onPreferenceChange function.");
        final String key = arg0.getKey();
        int value = Integer.parseInt(String.valueOf(arg1));
        if (CALL_REJECT_MODE_KEY.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOICE_CALL_REJECT_MODE, value);
            mRejectSetting.setSummary(mCallRejectModeArray[value]);
            mRejectList.setEnabled(value == 2);
            if (value == 1) {
                showDialog(CALL_ALL_NUMBERS);
            }
        }
        return true;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        AlertDialog alertDlg;
        switch(id) {
        case CALL_ALL_NUMBERS:
            builder.setMessage(getResources().getString(R.string.call_all_numbers));
            break;
        default:
            break;
        }
        builder.setPositiveButton(android.R.string.yes, null);
        alertDlg = builder.create();
        return alertDlg;
    }
}
