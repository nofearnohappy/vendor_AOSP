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

package com.mediatek.smsreg;

import android.telephony.TelephonyManager;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;

public class SmsRegConst {

    public static final int GEMINI_SIM_1 = PhoneConstants.SIM_ID_1;
    public static final int GEMINI_SIM_2 = PhoneConstants.SIM_ID_2;
    public static final int GEMINI_SIM_3 = PhoneConstants.SIM_ID_3;
    public static final int GEMINI_SIM_4 = PhoneConstants.SIM_ID_4;
    public static final int[] GEMSIM = { GEMINI_SIM_1, GEMINI_SIM_2, GEMINI_SIM_3, GEMINI_SIM_4};

    public static final int SIM_STATE_READY = TelephonyManager.SIM_STATE_READY;

    public static final String KEY_ICC_STATE = IccCardConstants.INTENT_KEY_ICC_STATE;
    public static final String VALUE_ICC_LOADED = IccCardConstants.INTENT_VALUE_ICC_LOADED;

    /**
     * Actions
     */
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

    public static final String ACTION_RETRY_SEND_SMS = "com.mediatek.smsreg.RETRY_SEND_SMS";
    public static final String ACTION_FINISH_SEND_SMS = "com.mediatek.smsreg.FINISH_SEND_SMS";

    public static final String ACTION_DISPLAY_DIALOG = "com.mediatek.smsreg.DISPLAY_CONFIRM_DIALOG";
    public static final String ACTION_RESPONSE_DIALOG = "com.mediatek.smsreg.RESPONSE_CONFIRM_DIALOG";

    /**
     * Extras
     */
    public static final String EXTRA_IMSI = "extra_imsi";
    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_IS_NEED_SEND = "extra_is_need_send";

    // Reset CollectSetPermission file of MediatekDM, if a registration occurs
    public static final String DM_SMSREG_MESSAGE_NEW = "com.mediatek.mediatekdm.smsreg.new";
}
