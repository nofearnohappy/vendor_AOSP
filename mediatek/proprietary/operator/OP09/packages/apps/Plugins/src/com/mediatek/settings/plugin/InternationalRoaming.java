/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.settings.plugin;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.op09.plugin.R;

/**
 * The class <code>InternationalRoaming</code> is main class of International
 * Roaming about China Telecom.
 */
public class InternationalRoaming extends Activity implements TabListener {

    private static final String TAG = "InternationalRoaming";

    private static final String KEY_CURRENT_SLOT = "current_slot";
    private int mCurSlot = 0;

    private SlotSettingsFragment mSlot1Fragement = new SlotSettingsFragment();
    private SlotSettingsFragment mSlot2Fragement = new SlotSettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSlot1Fragement.setSlotId(PhoneConstants.SIM_ID_1);
        mSlot2Fragement.setSlotId(PhoneConstants.SIM_ID_2);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.international_roaming_settings);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
            Context.TELEPHONY_SERVICE);
        if (telephonyManager != null && telephonyManager.getSimCount() == 1) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.tab, mSlot1Fragement);
            transaction.commitAllowingStateLoss();
            return;
        }
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        Tab simSlot1 = actionBar.newTab();
        simSlot1.setText(R.string.sim_slot_1);
        simSlot1.setTabListener(this);
        actionBar.addTab(simSlot1);

        Tab simSlot2 = actionBar.newTab();
        simSlot2.setText(R.string.sim_slot_2);
        simSlot2.setTabListener(this);
        actionBar.addTab(simSlot2);

        // Get current slot and select the corresponding tab if created from
        // relaunching.
        if (savedInstanceState != null) {
            mCurSlot = savedInstanceState.getInt(KEY_CURRENT_SLOT);
        }

        Log.d("@M_" + TAG, "onCreate: savedInstanceState = " + savedInstanceState + ",mSlot1Fragement = "
                + mSlot1Fragement + ", mSlot2Fragement = " + mSlot2Fragement + ",mCurSlot = "
                + mCurSlot);
        if (mCurSlot == PhoneConstants.SIM_ID_1) {
            actionBar.selectTab(simSlot1);
        } else if (mCurSlot == PhoneConstants.SIM_ID_2) {
            actionBar.selectTab(simSlot2);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_SLOT, mCurSlot);
        super.onSaveInstanceState(outState);
        Log.d("@M_" + TAG, "onSaveInstanceState: outState = " + outState + ", mCurSlot = " + mCurSlot);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mCurSlot = state.getInt(KEY_CURRENT_SLOT);
        Log.d("@M_" + TAG, "onRestoreInstanceState: state = " + state + ", mCurSlot = " + mCurSlot);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mCurSlot = tab.getPosition();
        Log.d("@M_" + TAG, "onTabSelected: tab.getPosition() = " + tab.getPosition() + ", mCurSlot = "
                + mCurSlot);

        if (mCurSlot == PhoneConstants.SIM_ID_1) {
            ft.replace(R.id.tab, mSlot1Fragement);
        } else if (mCurSlot == PhoneConstants.SIM_ID_2) {
            ft.replace(R.id.tab, mSlot2Fragement);
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }
}