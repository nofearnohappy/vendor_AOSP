/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.phone.plugin;


import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.os.Handler;
import android.widget.EditText;


import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultMmiCodeExt;

@PluginImpl(interfaceName="com.mediatek.phone.ext.IMmiCodeExt")
public class OP03MmiCodeExt extends DefaultMmiCodeExt {
    private static final String LOG_TAG = "MmiCodeOP03";
    private final Context mContext;
    private static final long TIME_DELAY = 300000;
    private static final int USSD_DIALOG_REQUEST = 1;
    private boolean mUserInit = false;
    static final int MAX_USSD_LEN = 160;

   public OP03MmiCodeExt(Context context) {
        mContext = context;
    }

    public void onMmiDailogShow(Message buttonCallbackMessage) {
        if(buttonCallbackMessage == null){
            Log.d("@M_" + LOG_TAG, "ussd does not dimiss as buttonCallbackMessage is null");
            return;
        }
        Handler mHandler = buttonCallbackMessage.getTarget();
        mHandler.removeMessages(buttonCallbackMessage.what);
        mHandler.sendMessageDelayed(buttonCallbackMessage, TIME_DELAY);
        Log.d("@M_" + LOG_TAG, "ussd dismisses in 5 minutes if network does not dismiss it onMmiDailogShow");
    }
    public boolean showUssdInteractionDialog(Phone phone , EditText inputText) {
        if (inputText.length() <= MAX_USSD_LEN) {
            phone.sendUssdResponse(inputText.getText().toString());
            Log.d("@M_" + LOG_TAG, "send response to network when input length is less than MAX_USSD_LEN");
            return true;
        }
        else {
        Log.d("@M_" + LOG_TAG, "raise toast when input length is greater than MAX_USSD_LEN");
        return false;
    }
    }

    public void configBeforeMmiDialogShow(MmiCode mmiCode) {
        mUserInit = mmiCode.getUserInitiatedMMI();
        Log.d("@M_" + LOG_TAG, "ussd mUserInit state =" + mUserInit);
    }

    public boolean skipPlayingUssdTone() {
        Log.d("@M_" + LOG_TAG, "ussd beeps when mUserInit is 0 if 1 it is Userinitiated and does not beep " + mUserInit);
        return mUserInit;
   }
}
