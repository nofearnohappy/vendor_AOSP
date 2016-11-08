/*
 * Copyright Statement:
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/*
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediatek.mms.op;

import android.app.ActionBar;
import android.app.ActionBar.TabListener;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.R;
import com.android.mms.ui.MessageUtils;

import java.util.List;

/**
 * M: tage setting activity for op09.
 */
public class MessageTabSettingActivity extends Activity implements TabListener {
    private static final String TAG = "MessageTabSettingActivity";

    private static final int MENU_RESTORE_DEFAULT = 1;

    private ActionBar mActionBar = null;

    // when there is only one sim card inserted.
    private int mSlotId = 0;

    private int mTabCount = 0;

    private boolean isSimChanged = false;

    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.message_tab_setting);
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(getResources().getString(R.string.menu_preferences));
        IntentFilter intentFilter = new IntentFilter(
            TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mSimReceiver, intentFilter);
        addFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSimChanged) {
            addFragment();
            isSimChanged = false;
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        if (mSimReceiver != null) {
            unregisterReceiver(mSimReceiver);
        }
        super.onDestroy();
    }

    /**
     * M: add fragment for frame.
     */
    public void addFragment() {
        Log.d(TAG, "addFragment");
        int simCount = SubscriptionManager.from(getApplicationContext())
                .getActiveSubscriptionInfoCount();
        Log.d(TAG, "simCount = " + simCount);
        ClassifyGeneralFragment generalFragment = ClassifyGeneralFragment.newInstance();
        switch (simCount) {
        case 0:
            if (mActionBar != null) {
                mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                mActionBar.removeAllTabs();
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.tab, generalFragment);
            ft.commit();
            break;
        case 1:
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mActionBar.removeAllTabs();
            SubscriptionInfo sir = SubscriptionManager.from(getApplicationContext())
                    .getActiveSubscriptionInfoList().get(0);
            mSlotId = sir.getSimSlotIndex();
            String singleSlot = sir.getDisplayName().toString();
            if (singleSlot != null) {
                String slotStr = getResources().getString(R.string.slot);
                String sim = mSlotId == 0 ? String.format(slotStr, 1)
                        + ": " : String.format(slotStr, 2) + ": ";
                addTabToActionBar(sim + singleSlot);
            }
            setInitialTab();
            break;
        case 2:
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mActionBar.removeAllTabs();
            String firstSlot = SubscriptionManager.from(getApplicationContext())
                    .getActiveSubscriptionInfoForSimSlotIndex(0).getDisplayName().toString();
            String secondSlot = SubscriptionManager.from(getApplicationContext())
                    .getActiveSubscriptionInfoForSimSlotIndex(1).getDisplayName().toString();
            String slotStr = getResources().getString(R.string.slot);
            if (firstSlot != null) {
                    addTabToActionBar(String.format(slotStr, 1) + ": "
                        + firstSlot);
            }
            if (secondSlot != null) {
                    addTabToActionBar(String.format(slotStr, 2) + ": "
                        + secondSlot);
            }
            setInitialTab();
            break;
        default:
            break;
        }
        mTabCount = mActionBar.getTabCount();
    }

    /**
     * M: add tab for actionbar.
     * @param displayName the tab name.
     * @return the tab.
     */
    private Tab addTabToActionBar(String displayName) {
        Log.d(TAG, "addTabToActionBar : " + displayName);
        Tab tab = mActionBar.newTab();
        tab.setText(displayName);
        tab.setTabListener(this);
        mActionBar.addTab(tab);
        return tab;
    }

    /**
     * M: init tab.
     */
    private void setInitialTab() {
        ClassifyGeneralFragment generalFragment = ClassifyGeneralFragment.newInstance();
        Tab generalTab = addTabToActionBar(getResources().getString(
            R.string.pref_setting_general));
        mActionBar.selectTab(generalTab);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.tab, generalFragment);
        ft.commit();
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Log.d(TAG, "onTabSelected");
        ClassifyGeneralFragment generalFragment = ClassifyGeneralFragment.newInstance();
        switch (mTabCount) {
        case 0:
            break;
        case 1:
            break;
        case 2:
            if (tab.getPosition() == 0) {
                ClassifySlotFragment singleSlotFragement = ClassifySlotFragment
                        .newInstance(mSlotId);
                ft.replace(R.id.tab, singleSlotFragement);
            } else if (tab.getPosition() == 1) {
                ft.replace(R.id.tab, generalFragment);
            }
            break;
        case 3:
            if (tab.getPosition() == 0) {
                ClassifySlotFragment slot1Fragement = ClassifySlotFragment
                        .newInstance(0);
                ft.replace(R.id.tab, slot1Fragement);
            } else if (tab.getPosition() == 1) {
                ClassifySlotFragment slot2Fragement = ClassifySlotFragment
                        .newInstance(1);
                ft.replace(R.id.tab, slot2Fragement);
            } else if (tab.getPosition() == 2) {
                ft.replace(R.id.tab, generalFragment);
            }
            break;
        default:
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULT, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case MENU_RESTORE_DEFAULT:
            restoreDefault();
            addFragment();
            break;
        default:
            break;
        }
        return false;
    }

    /**
     * M: restore defualt data.
     */
    private void restoreDefault() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
            MessageTabSettingActivity.this).edit();
        List<SubscriptionInfo> listSimInfo = SubscriptionManager.from(getApplicationContext())
                .getActiveSubscriptionInfoList();
        if (listSimInfo != null) {
            int simCount = listSimInfo.size();
            if (simCount > 0) {
                for (int i = 0; i < simCount; i++) {
                    int subId = listSimInfo.get(i).getSubscriptionId();
                    // sms default.
                    editor.putBoolean(Long.toString(subId) + "_"
                        + ClassifySlotFragment.SMS_DELIVERY_REPORT_MODE, false);

                    if (!getResources().getBoolean(R.bool.isTablet)) {
                        editor.putString(Long.toString(subId) + "_"
                            + ClassifySlotFragment.SMS_SAVE_LOCATION,
                            ClassifySlotFragment.SETTING_SAVE_LOCATION);
                    } else {
                        editor.putString(Long.toString(subId) + "_"
                            + ClassifySlotFragment.SMS_SAVE_LOCATION,
                            ClassifySlotFragment.SETTING_SAVE_LOCATION_TABLET);
                    }
                    // mms default.
                    editor.putBoolean(Long.toString(subId) + "_"
                        + ClassifySlotFragment.MMS_DELIVERY_REPORT_MODE, false);
                    if (!MessageUtils.isUSimType(subId)) {
                        editor.putBoolean(Long.toString(subId) + "_"
                            + ClassifySlotFragment.READ_REPORT_MODE, false);
                        editor.putBoolean(Long.toString(subId) + "_"
                            + ClassifySlotFragment.READ_REPORT_AUTO_REPLY, false);
                    }
                    editor.putBoolean(Long.toString(subId) + "_"
                        + ClassifySlotFragment.AUTO_RETRIEVAL, true);
                    editor.putBoolean(Long.toString(subId) + "_"
                        + ClassifySlotFragment.RETRIEVAL_DURING_ROAMING, false);
                }
            }
        }

        // sms priority
        editor.putString(ClassifySlotFragment.SMS_PRIORITY,
            ClassifyGeneralFragment.PRIORITY_NORMAL);
        // mms default.
        editor.putString(ClassifyGeneralFragment.CREATION_MODE,
            ClassifyGeneralFragment.CREATION_MODE_FREE);
        editor.putString(ClassifyGeneralFragment.MMS_SIZE_LIMIT,
            ClassifyGeneralFragment.SIZE_LIMIT_300);
        editor.putString(ClassifyGeneralFragment.PRIORITY,
            ClassifyGeneralFragment.PRIORITY_NORMAL);
        editor.putBoolean(ClassifyGeneralFragment.GROUP_MMS_MODE, false);
        // notification default.
        editor.putBoolean(ClassifyGeneralFragment.NOTIFICATION_ENABLED, true);
        editor.putString(ClassifyGeneralFragment.NOTIFICATION_MUTE, Integer.toString(0));
        editor.putString(ClassifyGeneralFragment.NOTIFICATION_RINGTONE,
            ClassifyGeneralFragment.DEFAULT_RINGTONE);
        editor.putString(ClassifyGeneralFragment.NOTIFICATION_RINGTONE,
                ClassifyGeneralFragment.DEFAULT_RINGTONE);
        editor.apply();
        editor.putBoolean(ClassifyGeneralFragment.NOTIFICATION_VIBRATE, true);
        editor.putBoolean(ClassifyGeneralFragment.POPUP_NOTIFICATION, true);
        // general default.
        editor.putInt(ClassifyGeneralFragment.FONT_SIZE_SETTING, 0);
        String[] fontSizeValues = getResources().getStringArray(
            R.array.pref_message_font_size_values);
        editor.putFloat(ClassifyGeneralFragment.TEXT_SIZE, Float.parseFloat(fontSizeValues[0]));
        editor.putBoolean(ClassifyGeneralFragment.AUTO_DELETE, false);
        editor.putInt(ClassifyGeneralFragment.MAX_SMS_PER_THREAD,
            ClassifyGeneralFragment.SMS_SIZE_LIMIT_DEFAULT);
        editor.putInt(ClassifyGeneralFragment.MAX_MMS_PER_THREAD,
            ClassifyGeneralFragment.MMS_SIZE_LIMIT_DEFAULT);
        editor.putBoolean(ClassifyGeneralFragment.WAPPUSH_ENABLED, true);
        editor.putBoolean(ClassifyGeneralFragment.SHOW_EMAIL_ADDRESS, true);
        final ClassifyGeneralFragment generalFragment = ClassifyGeneralFragment.newInstance();
        new Thread() {
            public void run() {
                generalFragment.clearWallpaperAll(MessageTabSettingActivity.this);
            }
        } .start();
        editor.apply();
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    // update sim state dynamically.
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isFirst) {
                isFirst = false;
                return;
            }
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                Log.d(TAG, "receive subinfo changed action = " + action);
                if (MessageTabSettingActivity.this.hasWindowFocus()) {
                    addFragment();
                } else {
                    isSimChanged = true;
                }
            }
        }
    };
}
