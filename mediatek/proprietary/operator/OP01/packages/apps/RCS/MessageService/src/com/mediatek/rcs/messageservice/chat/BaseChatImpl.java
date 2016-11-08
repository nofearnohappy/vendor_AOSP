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

/**
 * It's a implementation of IChat, it indicates a specify chat model.
 */

package com.mediatek.rcs.messageservice.chat;

import java.util.LinkedList;
import java.util.List;

import org.gsma.joyn.chat.ChatMessage;

import android.content.Context;
import android.os.Handler;

import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.utils.Logger;

public abstract class BaseChatImpl {
    private static final String TAG = "BaseChatImpl";

    public static final int MAX_PAGER_MODE_MSG_LENGTH = 900;
    // 60s
    public static final long MAX_MSG_PENDING_TIME = 45 * 1000;

    public static final int BASECHAT_OP_BASE = 0;
    public static final int BASE_OP_SEND_MESSAGE = BASECHAT_OP_BASE + 1;
    public static final int BASE_OP_RESEND_MESSAGE = BASECHAT_OP_BASE + 2;
    public static final int BASE_OP_SEND_MESSAGE_RST = BASECHAT_OP_BASE + 3;

    public static final int BASECHAT_NOTIFICATION_BASE = 10000;
    public static final int NOTIFICATION_REGISTRATION_STATUS_FALSE = BASECHAT_NOTIFICATION_BASE + 1;
    public static final int NOTIFICATION_REGISTRATION_STATUS_TRUE = BASECHAT_NOTIFICATION_BASE + 2;
    public static final int NOTIFICATION_CORESERVICE_DOWN = BASECHAT_NOTIFICATION_BASE + 3;
    // Chat message list, not used.
    private final List<ChatMessage> mMessageList = new LinkedList<ChatMessage>();

    protected Object mTag = null;

    protected RCSChatServiceBinder mService = null;
    protected Context mContext = null;
    protected GsmaManager mGsmaManager = GsmaManager.getInstance();
    protected Handler mWorkHandler = null;

    protected BaseChatImpl(RCSChatServiceBinder service, Object tag) {
        mService = service;
        mTag = tag;
        mContext = mService.getContext();
        mWorkHandler = mService.getWorkHandler();
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public void onReceiveMessageDeliveryStatus(final String msgId, final String status) {
        // status: ImdnDocument.java "sent" "delivered" "displayed" "failed" "error"
        // "display_burned"
        Logger.d(TAG, "onReceiveMessageDeliveryStatus() msgId: " + msgId + ", status: " + status);
        if (status.equalsIgnoreCase("sent")) {
            RCSDataBaseUtils.updateMessageStatus(mContext, msgId, MessageStatus.SENT);
        } else if (status.equalsIgnoreCase("delivered")) {
            RCSDataBaseUtils.updateMessageStatus(mContext, msgId, MessageStatus.DELIVERED);
        } else if (status.equalsIgnoreCase("failed")) {
            long smsId = RCSDataBaseUtils.getRcsMessageId(mContext, msgId, Direction.OUTGOING);
            notifyMessageSendFail(smsId);
        } else {
            // do nothing.
        }
    }

    public void notifyMessageSendFail(final long msgId) {
        Logger.d(TAG, "notifyMessageSendFail() msgId: " + msgId);
    }
}
