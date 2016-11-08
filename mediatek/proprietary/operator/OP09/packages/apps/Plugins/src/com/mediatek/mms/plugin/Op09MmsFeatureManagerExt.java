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
import android.util.Log;

/**
 * M: OP09 Mms Feature Manager implemention.
 */
public class Op09MmsFeatureManagerExt {
    private static final String TAG = "Mms/Op09MmsFeatureManagerExt";


    // define featue name index for Op09, index from 901 to 1000.

    /**
     * Add OP09 base index.
     */
    public static final int OP09_BASE_INDEX = 900;

    /**
     * Add comment for this feature.
     */
    public static final int STRING_REPLACE_MANAGEMENT = OP09_BASE_INDEX + 1;
    // / M: OP09Feature: for show dual time for received message item.
    public static final int SHOW_DUAL_TIME_FOR_MESSAGE_ITEM = OP09_BASE_INDEX + 2;
    // / M: OP09Feature: for show tab setting for MMS setting.
    public static final int MMS_TAB_SETTING = OP09_BASE_INDEX + 3;
    // / M: OP09Feature: for show dual send button in compose.
    public static final int MMS_DUAL_SEND_BUTTON = OP09_BASE_INDEX + 4;
    // / M: OP09Feature: for preview VCard in MMS compose.
    public static final int MMS_VCARD_PREVIEW = OP09_BASE_INDEX + 5;
    // / M: OP09Feature: change the legnthRequired MMS to SMS;
    public static final int CHANGE_LENGTH_REQUIRED_MMS_TO_SMS = OP09_BASE_INDEX + 6;
    // / M: OP09Feature: mass text msg: there is only one message item which be
    // show , when send a text msg to more than
    // / one recipient in one conversation.
    public static final int MASS_TEXT_MSG = OP09_BASE_INDEX + 7;
    // / M: OP09Feature: It is allowed to multiCOmpose exist .
    public static final int MMS_MULTI_COMPOSE = OP09_BASE_INDEX + 8;
    // / M: OP09Feature: can cancel the downloading MMS which has already start
    // download.
    public static final int MMS_CANCEL_DOWNLOAD = OP09_BASE_INDEX + 9;
    // / M: OP09Feature: splice missed sms which is a long sms which contains
    // more than one short messages.
    public static final int SPLICE_MISSED_SMS = OP09_BASE_INDEX + 10;
    // / M: OP09Feature: new class_zero model: when user received more than one
    // class_zero msg. the device should always
    // / show the latest class_zero msg.
    public static final int CLASS_ZERO_NEW_MODEL_SHOW_LATEST = OP09_BASE_INDEX + 11;
    // / M: OP09Feature: when device memory has low than 5% memory, device show
    // show Notification for user.
    public static final int MMS_LOW_MEMORY = OP09_BASE_INDEX + 12;
    // / M: OP09Feature: wake up screen when the device has inserted headSet
    // when receive new msg.
    public static final int WAKE_UP_SCREEN_WHEN_RECEIVE_MSG = OP09_BASE_INDEX + 13;
    // / M: OP09Feature: AdvanceSearchView: add time search condition.
    public static final int ADVANCE_SEARCH_VIEW = OP09_BASE_INDEX + 14;
    // / M: OP09Feature: When the device is at roaming status, MMS cannot allow
    // to set delivery report.
    public static final int DELIEVEEY_REPORT_IN_ROAMING = OP09_BASE_INDEX + 15;
    // / M: OP09Feature: show number location
    public static final int MMS_NUMBER_LOCATION = OP09_BASE_INDEX + 16;
    // / M: OP09Feature: format date and time stamp for op09;
    public static final int FORMAT_DATE_AND_TIME = OP09_BASE_INDEX + 17;
    // / M: OP09Feature: format notification content for adding expire date.
    public static final int FORMAT_NOTIFICATION_CONTENT = OP09_BASE_INDEX + 18;
    // / M: OP09Feature: read SMS from dual model UIM;
    public static final int READ_SMS_FROM_DUAL_MODEL_UIM = OP09_BASE_INDEX + 19;
    // / M: OP09Feature: show sent date and sorted by received date
    public static final int SHOW_DATE_MANAGEMENT = OP09_BASE_INDEX + 20;
    // / M: OP09Feature: there is more strict validation for SMS address.
    public static final int MORE_STRICT_VALIDATION_FOR_SMS_ADDRESS = OP09_BASE_INDEX + 21;
    // / M: OP09Feature: show preview for recipient.
    public static final int SHOW_PREVIEW_FOR_RECIPIENT = OP09_BASE_INDEX + 22;
    // / M: OP09Feature: when receive SI message, show the dialog to user.
    public static final int SHOW_DIALOG_FOR_NEW_SI_MSG = OP09_BASE_INDEX + 23;
    // / M: OP09Feature: When MMS transaction failed, show toast to notify user
    // the specific reason.
    public static final int MMS_TRANSACTION_FAILED_NOTIFY = OP09_BASE_INDEX + 24;
    // / M: OP09Feature: Modify MMS retry time interval.
    public static final int MMS_RETRY_SCHEDULER = OP09_BASE_INDEX + 25;
    // / M: OP09Feature: can add cc recipients into mms.
    public static final int MMS_CC_RECIPIENTS = OP09_BASE_INDEX + 26;
    // / M: OP09Feature: Set priority when sending SMS.
    public static final int SMS_PRIORITY = OP09_BASE_INDEX + 27;
    // / M: OP09Feature: turn page after fling screen left or right;
    public static final int MMS_PLAY_FILING_TURNPAGE = OP09_BASE_INDEX + 28;
    // / M: OP09Feature: unsupported files;
    public static final int MMS_UNSUPPORTED_FILES = OP09_BASE_INDEX + 29;

    public static final int SMS_ENABLE_CONCATENATE_LONG_SIM_SMS = 19;

    public static final int ENABLED_SMS_ENCODING_TYPE = 300 + 3;

    static public boolean isFeatureEnabled(int featureNameIndex) {
        Log.d(TAG, "[isFeatureEnabled]:\t" + featureNameIndex);
        switch (featureNameIndex) {
            case STRING_REPLACE_MANAGEMENT:
              return true;
            case SHOW_DUAL_TIME_FOR_MESSAGE_ITEM:
              return true;
            case MMS_TAB_SETTING:
              return true;
            case MMS_DUAL_SEND_BUTTON:
              return true;
            case MMS_VCARD_PREVIEW:
              return true;
            case CHANGE_LENGTH_REQUIRED_MMS_TO_SMS:
              return true;
            case MASS_TEXT_MSG:
              return true;
            case MMS_MULTI_COMPOSE:
              return false;
            case MMS_CANCEL_DOWNLOAD:
              return true;
            case SPLICE_MISSED_SMS:
              return true;
            case CLASS_ZERO_NEW_MODEL_SHOW_LATEST:
              return true;
            case MMS_LOW_MEMORY:
              return true;
            case WAKE_UP_SCREEN_WHEN_RECEIVE_MSG:
              return true;
            case ADVANCE_SEARCH_VIEW:
              return true;
            case DELIEVEEY_REPORT_IN_ROAMING:
              return true;
            case MMS_NUMBER_LOCATION:
              return true;
            case FORMAT_DATE_AND_TIME:
              return true;
            case FORMAT_NOTIFICATION_CONTENT:
              return true;
            case READ_SMS_FROM_DUAL_MODEL_UIM:
              return true;
            case SHOW_DATE_MANAGEMENT:
              return true;
            case MORE_STRICT_VALIDATION_FOR_SMS_ADDRESS:
              return true;
            case SHOW_PREVIEW_FOR_RECIPIENT:
              return true;
            case SHOW_DIALOG_FOR_NEW_SI_MSG:
              return true;
            case MMS_TRANSACTION_FAILED_NOTIFY:
                return true;
            case MMS_RETRY_SCHEDULER:
                return true;
            case SMS_PRIORITY:
                return true;
            case MMS_CC_RECIPIENTS:
                return true;
            case MMS_PLAY_FILING_TURNPAGE:
                return true;
            case MMS_UNSUPPORTED_FILES:
                Log.d(TAG, "MMS_UNSUPPORTED_FILES return true");
                return true;
            case SMS_ENABLE_CONCATENATE_LONG_SIM_SMS:
                return false;
            default:
                return false;
        }
    }
}
