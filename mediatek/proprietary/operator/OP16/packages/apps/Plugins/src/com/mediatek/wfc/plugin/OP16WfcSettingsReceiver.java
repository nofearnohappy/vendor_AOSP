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

package com.mediatek.wfc.plugin;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;

/**
 * OP16 Wfc settings static receiver.
 */
public class OP16WfcSettingsReceiver extends BroadcastReceiver {

    private static final String TAG = "OP16WfcSettingsReceiver";
    private int mCallState;


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            int phoneType = intent.getIntExtra(PhoneConstants.PHONE_TYPE_KEY,
                    RILConstants.NO_PHONE);
            Log.d(TAG, "phone state:" + state);
            Log.d(TAG, "phone type:" + phoneType);
            // TODO: need this checking due to intents with wrong phone_type received
            if (phoneType == RILConstants.IMS_PHONE) {
                if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                        || TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                    mCallState = WfcSettings.CALL_STATE_PS;
                    Settings.System.putInt(context.getContentResolver(),
                            WfcSettings.CALL_STATE, WfcSettings.CALL_STATE_PS);
                } else {
                    mCallState = WfcSettings.CALL_STATE_IDLE;
                    Settings.System.putInt(context.getContentResolver(),
                            WfcSettings.CALL_STATE, WfcSettings.CALL_STATE_IDLE);
                }
                sendCallStateChangeIntent(context, mCallState);
            } else if (phoneType == RILConstants.GSM_PHONE) {
                if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                        || TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                    mCallState = WfcSettings.CALL_STATE_CS;
                    Settings.System.putInt(context.getContentResolver(),
                            WfcSettings.CALL_STATE, WfcSettings.CALL_STATE_CS);
                } else {
                    mCallState = WfcSettings.CALL_STATE_IDLE;
                    Settings.System.putInt(context.getContentResolver(),
                            WfcSettings.CALL_STATE, WfcSettings.CALL_STATE_IDLE);
                }
                sendCallStateChangeIntent(context, mCallState);
            }
        }
    }

    private void sendCallStateChangeIntent(Context context, int callState) {
        Log.d(TAG, "broadcasting call_state:" + callState);
        Intent i = new Intent(WfcSettings.NOTIFY_CALL_STATE);
        i.putExtra(WfcSettings.CALL_STATE, callState);
        context.sendBroadcast(i);
    }
}
