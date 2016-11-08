/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.genericui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.MenuItem;

import org.gsma.joyn.JoynServiceConfiguration;

/**
 * RcsSettingsActivity for rcs settings.
 */
public class RcsSettingsActivity extends PreferenceActivity {

    private static final String TAG = "RcsSettingsState";
    private static final String RCS_SET_MODE = "rcs_set_mode";
    private static final String RCS_CORE_STATE = "rcs_core_state";
    private static final String RCS_SMS_MODE = "rcs_sms_mode";
    private static final String MODE_ENABLED = "enable";

    private static final int ILLEGAL_STATE = -1;
    private final static int RCS_CORE_LOADED = 0;
    private final static int RCS_CORE_STOPPED = 3;
    private final static int RCS_CORE_IMS_TRY_CONNECTION = 5;
    private final static int RCS_CORE_NOT_LOADED = 10;
    /** Key for preference rcs on-off "Mode". */
    private static final String KEY_RCS_ON_OFF = "rcs_on_off";
    /** Key for preference sms setting "Mode". */
    private static final String KEY_SMS_MODE = "rcs_sms_mode";
    /** Intent for open RCS. */
    private static final String INTENT_RCS_ON = "com.mediatek.intent.rcs.stack.LaunchService";
    /** Intent for close RCS. */
    private static final String INTENT_RCS_OFF = "com.mediatek.intent.rcs.stack.StopService";
    private static final String RCS_SWITCH_ENABLE = "com.mediatek.rcs.genericui.RCS_SWITCH_ENABLE";

    private SwitchPreference mRcsSwitchPref;
    private SwitchPreference mSmsSwitchPref;
    private Context mContext;
    private RcsTimer mRcsTimer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rcs_settings);
        mRcsSwitchPref = (SwitchPreference) findPreference(KEY_RCS_ON_OFF);
        mSmsSwitchPref = (SwitchPreference) findPreference(KEY_SMS_MODE);
        mContext = this;
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroy() {
        if (mRcsTimer != null) {
            Log.d(TAG, " onDestroy cancel RcsTimer");
            mRcsTimer.cancel();
            mRcsTimer = null;
        }
        super.onDestroy();
    }

    /**
     * define one internal timer class.
     */
    class RcsTimer extends CountDownTimer {
        public RcsTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            Log.d(TAG, " RcsTimer onFinish()");
            mRcsSwitchPref.setEnabled(true);
            mRcsTimer = null;
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean enable = intent.getBooleanExtra(MODE_ENABLED, true);
            Log.d(TAG, "onReceive() rcs switch enable:" + enable);
            if (mRcsTimer != null) {
                mRcsTimer.cancel();
                Log.d(TAG, " onReceive cancel RcsTimer");
            }
            if (enable) {
                mRcsTimer = new RcsTimer(2000, 2000);
                mRcsTimer.start();
            } else {
                mRcsSwitchPref.setEnabled(false);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        IntentFilter filter = new IntentFilter();
        filter.addAction(RCS_SWITCH_ENABLE);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);
        super.onPause();
    }

    /**
     * get rcs on-off status.
     */
    private boolean getRcsState() {
        return JoynServiceConfiguration.isServiceActivated(mContext);
    }

    /**
     * load switch preference status.
     */
    private void createPreferenceHierarchy() {
        boolean isEnable = getRcsState();
        Log.d(TAG, "enter rcs state:" + isEnable);
        mRcsSwitchPref.setChecked(isEnable);
        mRcsSwitchPref.setOnPreferenceChangeListener(mPreferenceChangeListener);
        SharedPreferences rcsSh = mContext.getSharedPreferences(
                                RCS_SET_MODE,
                                Context.MODE_WORLD_READABLE);
        if (rcsSh.getBoolean(MODE_ENABLED, true)) {
            mRcsSwitchPref.setEnabled(true);
        } else {
            mRcsSwitchPref.setEnabled(false);
        }

        SharedPreferences sh = mContext.getSharedPreferences(
                                RCS_SMS_MODE,
                                Context.MODE_WORLD_READABLE);
        mSmsSwitchPref.setChecked(sh.getBoolean(MODE_ENABLED, true));
        mSmsSwitchPref.setEnabled(isEnable);
        mSmsSwitchPref.setOnPreferenceChangeListener(mPreferenceChangeListener);
    }

    /**
     * swich preference listener.
     */
    private Preference.OnPreferenceChangeListener mPreferenceChangeListener =
            new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            boolean checked = ((Boolean) newValue).booleanValue();
            Log.d(TAG, "key=" + key + ", enable=" + checked);
            if (KEY_RCS_ON_OFF.equals(key)) {

                SharedPreferences sh = mContext.getSharedPreferences(
                    RCS_SET_MODE,
                    Context.MODE_WORLD_READABLE);
                SharedPreferences.Editor editor = sh.edit();
                int rcsCoreState = sh.getInt(RCS_CORE_STATE, ILLEGAL_STATE);
                int subId = SubscriptionManager.getDefaultDataSubId();
                Log.d(TAG, "rcsCoreState=" + rcsCoreState + ",subId:" + subId);

                if (checked) {
                    if ((subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                        && rcsCoreState != ILLEGAL_STATE
                        && rcsCoreState != RCS_CORE_LOADED
                        && rcsCoreState != RCS_CORE_NOT_LOADED) {
                        mRcsSwitchPref.setEnabled(false);

                        editor.putBoolean(MODE_ENABLED, false);
                        editor.commit();
                    }

                    mSmsSwitchPref.setEnabled(true);
                    Intent intent = new Intent(INTENT_RCS_ON);
                    mContext.sendBroadcast(intent);
                } else {
                    if (rcsCoreState == RCS_CORE_IMS_TRY_CONNECTION) {
                        mRcsSwitchPref.setEnabled(false);
                        return false;
                    }

                    if (rcsCoreState == RCS_CORE_STOPPED) {
                        editor.putInt(RCS_CORE_STATE, RCS_CORE_NOT_LOADED);
                        editor.commit();
                    } else if ((subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                        && rcsCoreState != ILLEGAL_STATE
                        && rcsCoreState != RCS_CORE_NOT_LOADED) {
                        mRcsSwitchPref.setEnabled(false);
                        editor.putBoolean(MODE_ENABLED, false);
                        editor.commit();
                    }

                    mSmsSwitchPref.setEnabled(false);

                    Intent intent = new Intent(INTENT_RCS_OFF);
                    mContext.sendBroadcast(intent);
                }
            } else if (KEY_SMS_MODE.equals(key)) {
                SharedPreferences sh = mContext.getSharedPreferences(
                    RCS_SMS_MODE,
                    Context.MODE_WORLD_READABLE);
                SharedPreferences.Editor editor = sh.edit();
                editor.putBoolean(MODE_ENABLED, checked);
                editor.commit();
            }
            return true;
        }
    };

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 finish();
                 return true;
             default:
                 break;
         }
         return super.onOptionsItemSelected(item);
     }

 /*
    Context packageContext;
    try {
        packageContext = context.createPackageContext(
                "com.mediatek.rcs.genericui", Context.CONTEXT_IGNORE_SECURITY);
    } catch (PackageManager.NameNotFoundException e) {
        Log.e(this, e, "Cannot find package com.mediatek.rcs.genericui");
        return null;
    }
    SharedPreferences sh = mContext.getSharedPreferences(
                        RCS_SMS_MODE,
                        Context.MODE_WORLD_READABLE);
    boolean isEnable = sh.getBoolean("enable", true)
*/

}

