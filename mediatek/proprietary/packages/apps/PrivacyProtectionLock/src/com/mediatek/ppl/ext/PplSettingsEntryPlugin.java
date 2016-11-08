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

package com.mediatek.ppl.ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.ppl.IPplAgent;
import com.mediatek.ppl.ControlData;
import com.mediatek.ppl.R;
import com.mediatek.ppl.SimTracker;
import com.mediatek.ppl.ui.LoginPplActivity;
import com.mediatek.ppl.ui.LaunchPplActivity;
import com.mediatek.ppl.ui.SetupPasswordActivity;
import com.mediatek.settings.ext.IPplSettingsEntryExt;

@PluginImpl(interfaceName="com.mediatek.settings.ext.IPplSettingsEntryExt")
public class PplSettingsEntryPlugin extends ContextWrapper implements IPplSettingsEntryExt {

    private Preference mPreference;
    private final IPplAgent mAgent;
    private final Context mContext;
    private SimTracker mSimTracker;
    private EventReceiver mReceiver;
    private OnPreferenceClickListener mNoSimListener;
    private OnPreferenceClickListener mSimNotReadyListener;
    private OnPreferenceClickListener mEnabledListener;
    private OnPreferenceClickListener mProvisionedListener;
    private OnPreferenceClickListener mNotProvisionedListener;

    private static final String TAG = "PPL/PplSettingsEntryPlugin";

    private class EventReceiver extends BroadcastReceiver {
        // com.android.internal.telephony.TelephonyIntents.ACTION_SIM_STATE_CHANGED
        public static final String ACTION_SIM_STATE_CHANGED = TelephonyIntents.ACTION_SIM_STATE_CHANGED;

        public void initialize() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SIM_STATE_CHANGED);
            registerReceiver(this, filter);
        }

        public void destroy() {
            unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (action.equals(ACTION_SIM_STATE_CHANGED)) {
                mSimTracker.takeSnapshot();
                updateUI();
            }
        }
    }

    public PplSettingsEntryPlugin(Context context) {
        super(context);
        mContext = context;
        mContext.setTheme(com.android.internal.R.style.Theme_Material_Settings);
        
        IBinder binder = ServiceManager.getService("PPLAgent");
        if (binder == null) {
            throw new Error("Failed to get PPLAgent");
        }
        mAgent = IPplAgent.Stub.asInterface(binder);
        if (mAgent == null) {
            throw new Error("mAgent is null!");
        }
        TelephonyManager telephonyManager = new TelephonyManager(context);
        int sim_number = telephonyManager.getSimCount();
        Log.i(TAG, "init sim number is " + sim_number);

        mPreference = new Preference(mContext);
        mPreference.setTitle(R.string.app_name);
        mPreference.setSummary(R.string.status_pending);
        mSimTracker = new SimTracker(sim_number, mContext);
        mNoSimListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast toast = Toast.makeText(mContext, R.string.toast_no_sim, Toast.LENGTH_SHORT);
                toast.show();
                return true;
            }
        };
        mSimNotReadyListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast toast = Toast.makeText(mContext, R.string.toast_sim_not_ready, Toast.LENGTH_SHORT);
                toast.show();
                return true;
            }
        };
        mEnabledListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // enter access lock
                Intent intent = new Intent();
                intent.setClass(mContext, LoginPplActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        };
        mProvisionedListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setClass(mContext, LaunchPplActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        };
        mNotProvisionedListener = new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setClass(mContext, SetupPasswordActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        };
    }

    public void addPplPrf(PreferenceGroup prefGroup) {
        if (prefGroup instanceof PreferenceGroup) {
            prefGroup.addPreference(mPreference);
        }
    }

    @Override
    public void enablerResume() {
        mReceiver = new EventReceiver();
        mReceiver.initialize();
        mSimTracker.takeSnapshot();
        updateUI();
    }

    @Override
    public void enablerPause() {
        if (mReceiver != null) {
            mReceiver.destroy();
            mReceiver = null;
        }
    }

    private void updateUI() {
        Log.i(TAG, "mSimTracker is " + mSimTracker);
        if (mSimTracker.getInsertedSim().length == 0) {
            mPreference.setOnPreferenceClickListener(mNoSimListener);
            mPreference.setSummary(R.string.status_pending);
        } else if (!mSimTracker.isAllSimReady()) {
            mPreference.setOnPreferenceClickListener(mSimNotReadyListener);
            mPreference.setSummary(R.string.status_pending);
        } else {
            ControlData controlData = null;
            try {
                controlData = ControlData.buildControlData(mAgent.readControlData());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if (controlData == null) {
                mPreference.setEnabled(false);
                return;
            }
            if (controlData.isEnabled()) {
                mPreference.setSummary(R.string.status_enabled);
                mPreference.setOnPreferenceClickListener(mEnabledListener);
            } else if (controlData.isProvisioned()) {
                mPreference.setSummary(R.string.status_provisioned);
                mPreference.setOnPreferenceClickListener(mProvisionedListener);
            } else {
                mPreference.setSummary(R.string.status_unprovisioned);
                mPreference.setOnPreferenceClickListener(mNotProvisionedListener);
            }
        }
    }
}
