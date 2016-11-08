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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class LockList extends PreferenceActivity {
    private static final String TAG = "SIMMELOCK";
    private static final String SLOTID = "SlotId";
    private static final String LOCKCATEGORY = "LockCategory";
    private static final int NPLOCKTYPE = 0;
    private static final int NSPLOCKTYPE = 1;
    private static final int SPLOCKTYPE = 2;
    private static final int CPLOCKTYPE = 3;
    private static final int SIMPLOCKTYPE = 4;
    private static final int CATEGORY_TOTAL = 5;
    private static final int QUERY_ICC_SML_COMPLETE = 120;

    private int mSlotId = 0;
    private int mSubId = 0;
    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                finish();
            }
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == QUERY_ICC_SML_COMPLETE) {
                Log.i(TAG, "QUERY_ICC_SML_COMPLETE");
                AsyncResult ar = (AsyncResult) msg.obj;
                int[] lockState = (int[]) ar.result;
                if (lockState == null) {
                    LockList.this.getPreferenceScreen().setEnabled(false);
                } else if (lockState[2] == 0) {
                    // Retry count is 0
                    LockList.this.getPreferenceScreen()
                        .getPreference(lockState[0]).setEnabled(false);
                } else {
                    // lockState[1] == 4: Disabled
                    LockList.this.getPreferenceScreen().getPreference(lockState[0])
                            .setEnabled(lockState[1] != 4);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.locklist);
        mTelephonyManager = TelephonyManager.getDefault();
        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(TELEPHONY_SERVICE));

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            mSlotId = bundle.getInt(SLOTID, 0);
            int[] subId = SubscriptionManager.getSubId(mSlotId);
            if (subId != null && subId.length > 0) {
                mSubId = subId[0];
            }
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isSimInsert = mTelephonyManager.hasIccCard(mSlotId);
        boolean isRadioOn = false;
        try {
            isRadioOn = PhoneFactory.getPhone(mSlotId)
                    .getServiceState().getState() != ServiceState.STATE_POWER_OFF;
        } catch (NullPointerException e) {
            isRadioOn = false;
        }

        Log.i(TAG, "isSimInsert: " + isSimInsert);
        Log.i(TAG, "isRadioOn: " + isRadioOn);
        if (!isSimInsert || !isRadioOn) {
            getPreferenceScreen().setEnabled(false);
            return;
        } else {
            getPreferenceScreen().setEnabled(true);
        }

        for (int category = 0; category < CATEGORY_TOTAL; category++) {
            Message callback = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
            queryIccNetworkLock(category, callback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferencescreen, Preference preference) {
        Bundle bundle = new Bundle();
        bundle.putInt(SLOTID, mSlotId);

        if (this.getPreferenceScreen().findPreference("nplock") == preference) {
            Intent intent = new Intent(LockList.this, ActionList.class);
            bundle.putInt(LOCKCATEGORY, NPLOCKTYPE);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("nsplock") == preference) {
            Intent intent = new Intent(LockList.this, ActionList.class);
            bundle.putInt(LOCKCATEGORY, NSPLOCKTYPE);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("splock") == preference) {
            Intent intent = new Intent(LockList.this, ActionList.class);
            bundle.putInt(LOCKCATEGORY, SPLOCKTYPE);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("cplock") == preference) {
            Intent intent = new Intent(LockList.this, ActionList.class);
            bundle.putInt(LOCKCATEGORY, CPLOCKTYPE);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("simplock") == preference) {
            Intent intent = new Intent(LockList.this, ActionList.class);
            bundle.putInt(LOCKCATEGORY, SIMPLOCKTYPE);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void queryIccNetworkLock(int lockCategory, Message callback) {
        Phone phone = PhoneFactory.getPhone(mSlotId);
        if (phone != null) {
            IccCard iccCard = phone.getIccCard();
            if (iccCard != null) {
                iccCard.queryIccNetworkLock(lockCategory, callback);
            }
        }
    }
}
