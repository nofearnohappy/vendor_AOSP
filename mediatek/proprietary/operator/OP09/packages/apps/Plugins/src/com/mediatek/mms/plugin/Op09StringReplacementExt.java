/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.mediatek.op09.plugin.R;

/**
 * M: Op09StringReplacementExt.
 */
public class Op09StringReplacementExt {
    private static final String TAG = "Mms/OP09StringReplacementExt";
    private Resources mResources = null;

    private static Op09StringReplacementExt sOp09StringReplacementExt;

    public static final int SAVE_MSG_TO_CARD = 1;
    public static final int SELECT_CARD = 2;
    public static final int MANAGE_CARD_MSG_TITLE = 3;
    public static final int MANAGE_CARD_MSG_SUMMARY = 4;
    public static final int MANAGE_UIM_MESSAGE = 5;
    public static final int UIM_EMPTY = 6;
    public static final int GET_CAPACITY_FAILED = 7;
    public static final int CONFIRM_DELETE_MSG = 8;
    public static final int UIM_FULL_TITLE = 9;
    public static final int MESSAGE_CANNOT_BE_OPERATED = 10;
    public static final int CONFIRM_DELETE_SELECTED_MESSAGES = 11;
    public static final int CAPACITY_SIM_CARD = 12;
    public static final int CHANGE_MMS_TO_SMS = 13;

    /**
     * The Constructor.
     * @param context The Context.
     */
    public static Op09StringReplacementExt getInstance(Context context) {
        if (sOp09StringReplacementExt == null) {
            sOp09StringReplacementExt = new Op09StringReplacementExt(context);
        }
        return sOp09StringReplacementExt;
    }

    public Op09StringReplacementExt(Context context) {
        mResources = context.getResources();
    }


    public String[] getSaveLocationString() {
        Log.d("@M_" + TAG, "OP09StringReplacementExt.getSaveLocationString()");
        return mResources.getStringArray(R.array.ct_sms_save_location_array);
    }


    public String getStrings(int type) {
        Log.d("@M_" + TAG, "OP09StringReplacementExt.getStrings: type = " + type);
        switch(type) {
        case SAVE_MSG_TO_CARD:
            return mResources.getString(R.string.save_msg_to_card);

        case SELECT_CARD:
            return mResources.getString(R.string.select_card);

        case MANAGE_CARD_MSG_TITLE:
            return mResources.getString(R.string.manage_card_msg_title);

        case MANAGE_CARD_MSG_SUMMARY:
            return mResources.getString(R.string.manage_card_msg_summary);

        case MANAGE_UIM_MESSAGE:
            return mResources.getString(R.string.manage_uim_messages_title);

        case UIM_EMPTY:
            return mResources.getString(R.string.uim_empty);

        case GET_CAPACITY_FAILED:
            return mResources.getString(R.string.get_uim_capacity_failed);

        case CONFIRM_DELETE_MSG:
            return mResources.getString(R.string.confirm_delete_uim_msg);

        case UIM_FULL_TITLE:
            return mResources.getString(R.string.uim_full_title);

        case MESSAGE_CANNOT_BE_OPERATED:
            return mResources.getString(R.string.message_cannot_be_operated);

        case CONFIRM_DELETE_SELECTED_MESSAGES:
            return mResources.getString(R.string.confirm_delete_selected_messages);

        case CAPACITY_SIM_CARD:
            return mResources.getString(R.string.capacity_sim_card_due_mode);

        case CHANGE_MMS_TO_SMS:
            return mResources.getString(R.string.change_mms_to_sms);

        default:
            return null;
        }
    }

}
