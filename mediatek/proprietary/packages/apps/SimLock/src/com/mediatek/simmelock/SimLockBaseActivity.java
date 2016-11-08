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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class SimLockBaseActivity extends Activity implements DialogInterface.OnKeyListener {
    private static final String TAG = "SIMMELOCK";
    private static final String SLOTID = "SlotId";
    private static final String LOCKCATEGORY = "LockCategory";

    protected int mSlotId = 0;
    protected int mSubId = 0;
    protected int mLockCategory = -1;

    protected View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return true;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            mLockCategory = bundle.getInt(LOCKCATEGORY, -1);
            mSlotId = bundle.getInt(SLOTID, 0);
            int[] subId = SubscriptionManager.getSubId(mSlotId);
            if (subId != null && subId.length > 0) {
                mSubId = subId[0];
            }
        }
        if (mLockCategory == -1) {
            finish();
            return;
        }

        // set the title
        String lockName = getLockName(mLockCategory);
        this.setTitle(lockName);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
        if (arg2.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            arg0.dismiss();
            finish();
            return true;
        }
        return false;
    }

    protected String getLockName(final int locktype) {
        switch (locktype) {
        case 0:
            return getString(R.string.strLockNameNetwork);
        case 1:
            return getString(R.string.strLockNameNetworkSub);
        case 2:
            return getString(R.string.strLockNameService);
        case 3:
            return getString(R.string.strLockNameCorporate);
        case 4:
            return getString(R.string.strLockNameSIM);
        default:
            return getString(R.string.simmelock_name);
        }
    }

    protected boolean isSimReady() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (TelephonyManager.SIM_STATE_ABSENT == telephonyManager.getSimState(mSlotId)) {
            return false;
        }
        return true;
    }

    protected void setIccNetworkLockEnabled(int category, int op, String passwd,
        String mcc, String gid1, String gid2, Message callback) {
        Phone phone = PhoneFactory.getPhone(mSlotId);
        if (phone != null) {
            phone.getIccCard().setIccNetworkLockEnabled(category, op, passwd,
                    mcc, gid1, gid2, callback);
        }
    }

    protected void log(String str) {
        Log.i(TAG, "[SIM" + (mSlotId + 1) + "] " + str);
    }
}
