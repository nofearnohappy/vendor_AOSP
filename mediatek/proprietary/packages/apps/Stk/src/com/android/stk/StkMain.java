/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.stk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;

import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import android.view.Gravity;
import android.widget.Toast;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;

/**
 * Launcher class. Serve as the app's MAIN activity, send an intent to the
 * StkAppService and finish.
 *
 */
 public class StkMain extends Activity {
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);
    private int mSingleSimId = -1;
    private Context mContext = null;
    private TelephonyManager mTm = null;
    private static final String PACKAGE_NAME = "com.android.stk";
    private static final String STK_LAUNCHER_ACTIVITY_NAME = PACKAGE_NAME + ".StkLauncherActivity";
    private static String mStkAppSourceKey = new String();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        CatLog.d(LOG_TAG, "onCreate+");
        mContext = getBaseContext();
        if (ActivityManager.getCurrentUser() != UserHandle.USER_OWNER) {
            CatLog.d(LOG_TAG, "CurrentUser:" + ActivityManager.getCurrentUser() +
            " is not USER_OWNER:" + UserHandle.USER_OWNER + " !!!");
            int UnsupportResId = R.string.lable_sim_not_ready;
            showTextToast(mContext, UnsupportResId);
            finish();
            return;
        }
        mStkAppSourceKey = LOG_TAG + System.currentTimeMillis();
        mTm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        //Check if needs to show the meun list.
        if (isShowSTKListMenu()) {
            Intent newIntent = new Intent(Intent.ACTION_VIEW);
            newIntent.putExtra(StkAppService.STK_SOURCE_KEY, mStkAppSourceKey);
            newIntent.setClassName(PACKAGE_NAME, STK_LAUNCHER_ACTIVITY_NAME);
            startActivity(newIntent);
        } else {
            //launch stk menu activity for the SIM.
            if (mSingleSimId < 0) {
                showTextToast(mContext, R.string.no_sim_card_inserted);
            } else {
                launchSTKMainMenu(mSingleSimId);
            }
        }
        finish();
    }

    private boolean isShowSTKListMenu() {
        int simCount = TelephonyManager.from(mContext).getSimCount();
        int simInsertedCount = 0;
        int insertedSlotId = -1;

        CatLog.d(LOG_TAG, "simCount: " + simCount);
        for (int i = 0; i < simCount; i++) {
            //Check if the card is inserted.
            if (mTm.hasIccCard(i)) {
                CatLog.d(LOG_TAG, "SIM " + i + " is inserted.");
                int subId[] = SubscriptionManager.getSubId(i);
                Phone phone = getPhoneUsingSubId(subId[0]);
                String cardType = phone.getIccCard().getIccCardType();
                CatLog.d(LOG_TAG, "cardType: " + cardType);
                if (cardType.contains("RUIM") || cardType.contains("CSIM")) {
                    if (RoamingMode.ROAMING_MODE_NORMAL_ROAMING == getRoamingState(i)) {
                        simInsertedCount++;
                        mSingleSimId = i;
                    }
                } else {
                    simInsertedCount++;
                    mSingleSimId = i;
                }
            } else {
                CatLog.d(LOG_TAG, "SIM " + i + " is not inserted.");
            }
        }
        if (simInsertedCount > 1) {
            return true;
        } else {
            //No card or only one card.
            CatLog.d(LOG_TAG, "do not show stk list menu.");
            return false;
        }
    }

    private void launchSTKMainMenu(int slotId) {
        Bundle args = new Bundle();
        CatLog.d(LOG_TAG, "launchSTKMainMenu.");

        if (!isStkAvailable(slotId)) {
            CatLog.d(LOG_TAG, "Stk " + slotId + " is not available.");
            finish();
            return;
        }
        args.putInt(StkAppService.OPCODE, StkAppService.OP_LAUNCH_APP);
        args.putInt(StkAppService.SLOT_ID
                , PhoneConstants.SIM_ID_1 + slotId);
        startService(new Intent(this, StkAppService.class)
                .putExtras(args));
    }

    private boolean isStkAvailable(int slotId) {
        StkAppService service = StkAppService.getInstance();
        if (service != null && service.StkQueryAvailable(slotId) !=
                StkAppService.STK_AVAIL_AVAILABLE) {
            CatLog.d(LOG_TAG, "Not available");
            int resId = R.string.lable_sim_not_ready;
            int simState = TelephonyManager.getDefault().getSimState(slotId);

            CatLog.d(LOG_TAG, "Not available simState:" + simState);
            if (true == isOnFlightMode()) {
                resId = R.string.lable_on_flight_mode;
            } else if (true == isOnLockMode(slotId) ||
                    false == isRadioOnState(slotId)) {
                CatLog.d(LOG_TAG, "isOnLockMode or not onRadioOnState");
            }
            showTextToast(mContext, resId);
            return false;
        }
        return true;
    }

    private boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            CatLog.d(LOG_TAG, "fail to get airlane mode");
            mode = 0;
        }

        CatLog.d(LOG_TAG, "airlane mode is " + mode);
        return (mode != 0);
    }

    boolean isOnLockMode(int slotId) {
        int simState = TelephonyManager.getDefault().getSimState(slotId);
        CatLog.d(LOG_TAG, "lock mode is " + simState);
        if (TelephonyManager.SIM_STATE_PIN_REQUIRED == simState ||
                TelephonyManager.SIM_STATE_PUK_REQUIRED == simState ||
                TelephonyManager.SIM_STATE_NETWORK_LOCKED == simState) {
            return true;
        } else {
            return false;
        }
    }

    boolean isRadioOnState(int slotId) {
        boolean radioOn = true;
        CatLog.d(LOG_TAG, "isRadioOnState check = " + slotId);

        try {
            ITelephony phone = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (phone != null) {
                int subId[] = SubscriptionManager.getSubId(slotId);
                radioOn = phone.isRadioOnForSubscriber(subId[0],
                    getApplicationContext().getOpPackageName());
            }
            CatLog.d(LOG_TAG, "isRadioOnState - radio_on[" + radioOn + "]");
        } catch (RemoteException e) {
            e.printStackTrace();
            CatLog.d(LOG_TAG, "isRadioOnState - Exception happen ====");
        }
        return radioOn;
    }

    private void showTextToast(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    public static boolean isValidStkSourceKey(String strKey) {
        CatLog.d(LOG_TAG, "isValidStkSourceKey key: " + strKey + " , " + mStkAppSourceKey);
        return (mStkAppSourceKey == null) ? false : mStkAppSourceKey.equals(strKey);
    }

    private Phone getPhoneUsingSubId(int subId) {
        CatLog.d(this, "getPhoneUsingSubId subId:" + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    private RoamingMode getRoamingState(int slotId) {
        LteDcPhoneProxy lteDcPhoneProxy = (LteDcPhoneProxy) PhoneFactory.getPhone(slotId);
        SvlteRatController lteRatController = null;
        RoamingMode rm = RoamingMode.ROAMING_MODE_UNKNOWN;
        if (null != lteDcPhoneProxy) {
            lteRatController = lteDcPhoneProxy.getSvlteRatController();
            if (null != lteRatController) {
                rm = lteRatController.getRoamingMode();
            }
        } else {
            CatLog.d(this, "lteDcPhoneProxy: null");
        }
        CatLog.d(this, "getRoamingState: " + rm);
        return rm;
    }
}
