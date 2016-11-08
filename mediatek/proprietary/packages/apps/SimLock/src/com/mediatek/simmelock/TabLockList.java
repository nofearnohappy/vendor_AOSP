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

package com.android.simmelock;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ServiceManager;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class TabLockList extends TabActivity implements OnTabChangeListener {
    private static final String TAG = "SIMMELOCK";
    private static final String SLOTID = "SlotId";
    private static final int TAB_SIM_1 = 0;
    private static final int TAB_SIM_2 = 1;

    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        ITelephony iTelephony = ITelephony.Stub.asInterface(
                ServiceManager.getService(TELEPHONY_SERVICE));
        if (iTelephony == null || telephonyManager == null) {
            finish();
            return;
        }

        int simCount = telephonyManager.getSimCount();
        Log.d(TAG, "simCount = " + simCount);
        if (simCount <= 0) {
            finish();
            return;
        }

        boolean[] simState = new boolean[simCount];
        mTabHost = getTabHost();
        mTabHost.setOnTabChangedListener(this);
        for (int i = 0; i < simCount; i++) {
            setupTab(i);
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null) {
                simState[i] = telephonyManager.hasIccCard(i)
                        && phone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
            } else {
                simState[i] = false;
            }
        }
        Log.d(TAG, "simState = " + simState[0] + " " + simState[1]);
        setCurrentTab(simState);
    }

    private void setCurrentTab(boolean[] simState) {
        Log.d(TAG, "setCurrentTab " + simState[0] + " " + simState[1]);
        if (simState.length == 2 && !simState[0] && simState[1]) {
            Log.d(TAG, "setCurrentTab " + TAB_SIM_2);
            mTabHost.setCurrentTab(TAB_SIM_2);
        } else {
            // Gemini+ set current tab only sim1
            Log.d(TAG, "setCurrentTab " + TAB_SIM_1);
            mTabHost.setCurrentTab(TAB_SIM_1);
        }
    }

    private void setupTab(int id) {
        Log.d(TAG, "setupTab " + id);
        String title = "SIM" + (id + 1);
        int icon = id == 0 ? R.drawable.tab_manage_sim1 : R.drawable.tab_manage_sim2;
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(this, LockList.class);
        intent.putExtra(SLOTID, id);
        mTabHost.addTab(mTabHost.newTabSpec(title).setIndicator(title,
                getResources().getDrawable(icon)).setContent(intent));
    }

    @Override
    public void onTabChanged(String tabId) {
        Activity activity = getLocalActivityManager().getActivity(tabId);
        if (activity != null) {
            activity.onWindowFocusChanged(true);
        }
    }
}
