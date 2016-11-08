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

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

/**
 * When user press <code>FreeService</code>,
 * the number "+86 18918910000" will be dialed directly.
 * Note:
 * Even the default SIM for "Voice call" is another non-CDMA SIM,
 * this free service number is always dialed out by CDMA SIM card.
 */
public class FreeService extends IntentService {

    private static final String TAG = "FreeService";

    public static final int NO_SIM_ERROR = 0;
    public static final int ONE_CDMA = 1;
    public static final int ONE_GSM = 2;
    public static final int TWO_SIM = 3;
    public static final String SIM_INFO = "SIM_INFO";
    public static final String EXTRA_SLOT_ID = "com.android.phone.extra.slot";

    private static final String FREE_SERVICE_URI = "tel:+8618918910000";

    /**
     * Creates a new <code>FreeService</code> instance.
     *
     */
    public FreeService() {
        super("FreeService");
        Log.v("@M_" + TAG, "FreeService Constructor is called.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.v("@M_" + TAG, "onHandleIntent method is called.");
        Intent newIntent = new Intent(Intent.ACTION_CALL, Uri.parse(FREE_SERVICE_URI));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int simStatus = intent.getIntExtra(SIM_INFO, NO_SIM_ERROR);
        Log.v("@M_" + TAG, "dialUsingAccount, simStatus=" + simStatus);
        if (simStatus == TWO_SIM) {
            // When two SIM inserted, using the account to dial to neglect the sim choice dialog.
            dialUsingAccount(PhoneConstants.SIM_ID_1, newIntent);
        } else if (simStatus == ONE_CDMA) {
            // It's normal status, calling by SIM1 CDMA
            newIntent.putExtra(EXTRA_SLOT_ID, PhoneConstants.SIM_ID_1);
        } else if (simStatus == ONE_GSM) {
            // Only GSM Sim inserted into Slot2, calling by SIM2 GSM
            newIntent.putExtra(EXTRA_SLOT_ID, PhoneConstants.SIM_ID_2);
        } else { // NO_SIM_ERROR
            Log.v("@M_" + TAG, "ERROR! No sim detected!");
        }

        startActivity(newIntent);
        Log.v("@M_" + TAG, "Dialing Successfully!");
    }

    /**
     * Using the account to fill the intent for neglecting the sim choice dialog when dialing.
     * @param slotId the slot id.
     * @param intent the Intent.
     */
    private void dialUsingAccount(int slotId, Intent intent) {
        final Context context = getApplicationContext();
        final int [] subIds = SubscriptionManager.getSubId(slotId);
        Log.v("@M_" + TAG, "dialUsingAccount, slotId = " + slotId + ",subIds = " + subIds);
        if (subIds != null && subIds.length != 0) {
            int subId = subIds[0];
            Log.v("@M_" + TAG, "dialUsingAccount, slotId = " + slotId + ",subId = " + subId);
            if (subId != -1) {
                ComponentName pstnConnectionServiceName = new ComponentName("com.android.phone",
                        "com.android.services.telephony.TelephonyConnectionService");
                //String id = "" + String.valueOf(subId);
                SubscriptionInfo subInfo = SubscriptionManager.from(context)
                        .getActiveSubscriptionInfo(subId);
                String id = subInfo.getIccId();
                PhoneAccountHandle phoneAccountHandle =
                    new PhoneAccountHandle(pstnConnectionServiceName, id);
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
            }
        }
    }
}
