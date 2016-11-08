/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.ITelephony;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;

import android.view.Gravity;
import android.widget.Toast;

/**
 * Launcher class. Serve as the app's MAIN activity, send an intent to the
 * StkAppService and finish.
 *
 */
public class StkLauncherActivityIII extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StkAppService service = StkAppService.getInstance();

        if (service != null && service.StkQueryAvailable(PhoneConstants.SIM_ID_3) != StkAppService.STK_AVAIL_AVAILABLE)
        {
            CatLog.d("Stk-LAIII", "Not available");
            int resId = R.string.lable_sim_not_ready;

            int simState = TelephonyManager.getDefault().getSimState(PhoneConstants.SIM_ID_3);

            CatLog.d("Stk-LAIII", "Not available simState:" + simState);
            if (true == isOnFlightMode()) { //OP02 case
                resId = R.string.lable_on_flight_mode;
            } else if (true == isOnLockMode(PhoneConstants.SIM_ID_3) ||
                    false == isRadioOnState(PhoneConstants.SIM_ID_3)) {
                CatLog.d("Stk-LAIII", "Stk is on lock or radio off.");
            }
            Toast toast = Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
            finish();
            return;
        }

        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_LAUNCH_APP);
        args.putInt(StkAppService.SLOT_ID, PhoneConstants.SIM_ID_3);
        startService(new Intent(this, StkAppService.class).putExtras(args));

        finish();
    }
    boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(getApplicationContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            CatLog.d("Stk-LAIII", "fail to get airlane mode");
            mode = 0;
        }

        CatLog.d("Stk-LAIII", "airlane mode is " + mode);
        return (mode != 0);
    }
    boolean isOnLockMode(int slotId) {
        int simState = TelephonyManager.getDefault().getSimState(slotId);
        CatLog.d("Stk-LAIII", "lock mode is " + simState);
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
        CatLog.d("Stk-LAIII", "isRadioOnState check = " + slotId);

        try {
            ITelephony phone = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (phone != null) {
                int subId[] = SubscriptionManager.getSubId(slotId);
                radioOn = phone.isRadioOnForSubscriber(subId[0],
                    getApplicationContext().getOpPackageName());
            }
            CatLog.d("Stk-LAIII", "isRadioOnState - radio_on[" + radioOn + "]");
        } catch (RemoteException e) {
            e.printStackTrace();
            CatLog.d("Stk-LAIII", "isRadioOnState - Exception happen ====");
        }
        return radioOn;
    }
}
