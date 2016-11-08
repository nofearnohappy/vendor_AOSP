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

package com.mediatek.bluetoothle.anp.detector;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.bluetoothle.anp.NotificationController;
import com.mediatek.bluetoothle.anp.support.ContactsUtil;
import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;

public class IncommingCallAlertDetector extends BluetoothAnsDetector {

    private static final String TAG = "[BluetoothAns]IncommingCallAlertDetector";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private TelephonyManager mTelephonyManager;

    public IncommingCallAlertDetector(Context context) {
        super(context);
        mCategoryId = NotificationController.CATEGORY_ID_INCOMING_CALL;
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.i(TAG, "PhoneStateListener,new state=" + state);
            if (mTelephonyManager != null) {
                int currPhoneCallState = mTelephonyManager.getCallState();
                if (currPhoneCallState == TelephonyManager.CALL_STATE_RINGING) {
                    if (incomingNumber != null) {
                        incomingNumber = ContactsUtil.getNameFromAddress(mContext, incomingNumber);
                    }
                    Log.i(TAG, "onCallStateChanged, number = " + incomingNumber);
                    mNewCount = 1;
                    setNewAlertText(incomingNumber);
                    onAlertNotify(null, NotificationController.CATEGORY_ENABLED_NEW);
                } else if (currPhoneCallState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    mNewCount = 0;
                    setNewAlertText(null);
                }
            }
        }
    };

    @Override
    public void initializeAll() {
        initNewDetector();
    }

    @Override
    public void clearAll() {
        clearNewDetector();
    }
    private void initNewDetector() {
        if (DBG) {
            Log.d(TAG, "initNewDetector");
        }
        mTelephonyManager =
                (TelephonyManager) (mContext.getSystemService(Context.TELEPHONY_SERVICE));
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void clearNewDetector() {
        if (DBG) {
            Log.d(TAG, "clearNewDetector");
        }
        mTelephonyManager =
                (TelephonyManager) (mContext.getSystemService(Context.TELEPHONY_SERVICE));
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }
}
