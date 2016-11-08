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
import android.content.res.Configuration;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class ActionList extends PreferenceActivity {
    private static final int SMLLOCKED = 1;
    private static final int SMLUNLOCKED = 2;
    private static final String LOCKNAME = "LockName";
    private static final String SLOTID = "SlotId";
    private static final String LOCKCATEGORY = "LockCategory";
    private static final String ACTIONNAME = "ActionName";
    private static final String TAG = "SIMMELOCK";
    private static final int QUERY_ICC_SML_COMPLETE = 120;
    private static final int QUERY_ICC_SML_LOCK_STATE = 100;

    private String mLockName = null;
    private int mSlotId = 0;
    private int mLockCategory = -1;
    private boolean mUnlockEnable = true;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                finish();
            }
        }
    };

    /**
     * get the enable status of every Action.
     *
     */
    // notes: wait for framework's interface
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case QUERY_ICC_SML_LOCK_STATE: {
                if (!mUnlockEnable) {
                    return;
                }
                AsyncResult ar = (AsyncResult) msg.obj;
                Log.i(TAG, "ActionList handler QUERY_ICC_SML_LOCK_STATE");
                int[] lockState = (int[]) ar.result;
                if (lockState == null) {
                    ActionList.this.finish();
                } else if (lockState[1] == SMLLOCKED) {
                    Log.i(TAG, "mUnlockEnable = false");
                    mUnlockEnable = false;
                }
                break;
            }
            case QUERY_ICC_SML_COMPLETE: {
                Log.i(TAG, "ActionList handler");
                AsyncResult ar1 = (AsyncResult) msg.obj;
                int[] lockState = (int[]) ar1.result;
                if (lockState == null) {
                    ActionList.this.finish();
                } else if (lockState[2] == 0) {
                    ActionList.this.finish();
                } else if (lockState[1] == 4) {  // Disable
                    enablePreference(false, false, false, false, false);
                } else if (lockState[1] == 2) {  // Not locked
                    if (lockState[4] == 0) {  // Lock number == 0
                        enablePreference(false, false, true, false, true);
                    } else {
                        if (lockState[4] < lockState[5]) {  // Lock number < Max number
                            enablePreference(true, false, true, true, true);
                        } else {  // Lock number == Max number
                            enablePreference(true, false, false, true, true);
                        }
                    }
                } else {  // Locked
                    enablePreference(false, true, false, false, false);
                }
                if (!mUnlockEnable) {
                    ((PreferenceActivity) (ActionList.this)).getPreferenceScreen()
                            .getPreference(1).setEnabled(false);
                }
                break;
            }
            default:
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.actionlist);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        // get the lock name
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            mLockCategory = bundle.getInt(LOCKCATEGORY, -1);
            mSlotId = bundle.getInt(SLOTID);
        }
        if (mLockCategory == -1) {
            finish();
            return;
        }

        // set the title
        mLockName = getLockName(mLockCategory);
        setTitle(mLockName);

        getUnlockEnableState(mLockCategory);
        enablePreference(false, false, false, false, false);
    }

    private String getLockName(final int locktype) {
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

    @Override
    protected void onResume() {
        super.onResume();

        if (mLockCategory == -1) {
            finish();
            return;
        }

        Message callback = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
        queryIccNetworkLock(mLockCategory, callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mLockName = state.getString(LOCKNAME);
        mLockCategory = state.getInt(LOCKCATEGORY, -1);
        mSlotId = state.getInt(SLOTID, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(LOCKNAME, mLockName);
        outState.putInt(LOCKCATEGORY, mLockCategory);
        outState.putInt(SLOTID, mSlotId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferencescreen, Preference preference) {
        Bundle bundle = new Bundle();
        bundle.putInt(LOCKCATEGORY, mLockCategory);
        bundle.putInt(SLOTID, mSlotId);

        if (this.getPreferenceScreen().findPreference("lock") == preference) {
            // To lock
            Log.i(TAG, "Action lock");
            Intent intent = new Intent(ActionList.this, LockSetting.class);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (preference.getKey().equals("unlock")) {
            // to unlock
            Log.i(TAG, "Action unlock");
            Configuration conf = getResources().getConfiguration();
            String locale = conf.locale.getDisplayName(conf.locale);
            Intent intent = new Intent(ActionList.this, UnlockSetting.class);
            bundle.putString("LOCALNAME", locale);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("addlock") == preference) {
            // To add a lock
            Log.i(TAG, "Action addlock");
            Intent intent = null;
            switch (mLockCategory) {
            case 0:
                intent = new Intent(ActionList.this, NPAddLockSetting.class);
                break;
            case 1:
                intent = new Intent(ActionList.this, NSPAddLockSetting.class);
                break;
            case 2:
                intent = new Intent(ActionList.this, SPAddLockSetting.class);
                break;
            case 3:
                intent = new Intent(ActionList.this, CPAddLockSetting.class);
                break;
            case 4:
                intent = new Intent(ActionList.this, SIMPAddLockSetting.class);
                break;
            default:
                return false;
            }
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("removelock") == preference) {
            // To remove a lock
            Log.i(TAG, "Action removelock");
            Intent intent = new Intent(ActionList.this, RemoveSetting.class);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        } else if (this.getPreferenceScreen().findPreference("permanentlyunlock") == preference) {
            // To permanently unlock
            Log.i(TAG, "Action permanentlyunlock");
            Intent intent = new Intent(ActionList.this, PermanUnlockSetting.class);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        }
        Log.i(TAG, "Action null || preference" + preference);
        return false;
    }

    private void enablePreference(boolean lock, boolean unlock, boolean add,
            boolean remove, boolean permanRemove) {
        ((PreferenceActivity) (ActionList.this)).getPreferenceScreen()
                .getPreference(0).setEnabled(lock);
        ((PreferenceActivity) (ActionList.this)).getPreferenceScreen()
                .getPreference(1).setEnabled(unlock);
        ((PreferenceActivity) (ActionList.this)).getPreferenceScreen()
                .getPreference(2).setEnabled(add);
        ((PreferenceActivity) (ActionList.this)).getPreferenceScreen()
                .getPreference(3).setEnabled(remove);
        ((PreferenceActivity) (ActionList.this)).getPreferenceScreen()
                .getPreference(4).setEnabled(permanRemove);
    }

    private void getUnlockEnableState(int category) {
        Log.i(TAG, "[getUnlockEnableState] Current lock category is " + category);
        if (category < 0 || category > 4) {
            return;
        }

        for (int tempCate = 0; tempCate < category; tempCate++) {
            Log.i(TAG, "[getUnlockEnableState] Queried lock category is " + tempCate
                    + " || mUnlockEnable is " + mUnlockEnable);
            if (!mUnlockEnable) {
                break;
            }
            Message unlockCallBack = Message.obtain(mHandler, QUERY_ICC_SML_LOCK_STATE);
            queryIccNetworkLock(tempCate, unlockCallBack);
        }
    }

    private void queryIccNetworkLock(int mLockCategory, Message callback) {
        Phone phone = PhoneFactory.getPhone(mSlotId);
        if (phone != null) {
            IccCard iccCard = phone.getIccCard();
            if (iccCard != null) {
                iccCard.queryIccNetworkLock(mLockCategory, callback);
            }
        }
    }
}
