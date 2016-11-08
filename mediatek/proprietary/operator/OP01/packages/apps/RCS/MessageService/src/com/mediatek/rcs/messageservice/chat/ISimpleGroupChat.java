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

package com.mediatek.rcs.messageservice.chat;

import java.util.List;

import android.content.Intent;
import android.os.Handler;

public interface ISimpleGroupChat {
    public static final int OP_BASE = BaseChatImpl.BASECHAT_OP_BASE + 1000;
    public static final int OP_GROUP_START = OP_BASE + 1;

    public static final int OP_SEND_CHATMESSAGE = OP_BASE + 2;
    public static final int OP_RESEND_CHATMESSAGE = OP_BASE + 3;
    public static final int OP_ADD_PARTICIPANT = OP_BASE + 4;
    public static final int OP_REMOVE_PARTICIPANT = OP_BASE + 5;
    public static final int OP_TRANSFER_CHAIRMAN = OP_BASE + 6;
    public static final int OP_MODIFY_SUBJECT = OP_BASE + 7;
    public static final int OP_MODIFY_MYNICKNAME = OP_BASE + 8;

    public static final int OP_QUIT_GROUP = OP_BASE + 9;
    public static final int OP_ABORT_GROUP = OP_BASE + 10;
    public static final int OP_GROUP_INVITATION = OP_BASE + 11;
    public static final int OP_GROUP_INVITATION_ACCEPT = OP_BASE + 12;
    public static final int OP_GROUP_INVITATION_REJECT = OP_BASE + 13;

    // start for file transfer.
    public static final int OP_SEND_FILE = OP_BASE + 14;
    public static final int OP_RESEND_FILE = OP_BASE + 15;
    public static final int OP_DOWNLOAD_FILE = OP_BASE + 16;
    public static final int OP_REDOWNLOAD_FILE = OP_BASE + 17;
    public static final int OP_FT_INVITATION = OP_BASE + 18;
    // end for file transfer.
    public static final int OP_GROUP_UPDATE_STATUS = OP_BASE + 19;
    public static final int OP_QUIT_NOW = OP_BASE + 99;
    public static final int OP_TRANSFER_GROUP_STATE = OP_BASE + 100;

    public static final int NOTIFICATION_BASE = BaseChatImpl.BASECHAT_NOTIFICATION_BASE + 1000;
    public static final int NOTIFICATION_GROUP_STARTED = NOTIFICATION_BASE + 1;
    public static final int NOTIFICATION_GROUP_ABORT = NOTIFICATION_BASE + 2;
    public static final int NOTIFICATION_GROUP_ERROR = NOTIFICATION_BASE + 3;
    public static final int NOTIFICATION_CHATMESSAGE_SENT = NOTIFICATION_BASE + 4;
    public static final int NOTIFICATION_PARTICIPANT_JOINED = NOTIFICATION_BASE + 5;
    public static final int NOTIFICATION_PARTICIPANT_LEFT = NOTIFICATION_BASE + 6;
    public static final int NOTIFICATION_PARTICIPANT_BEEN_KICKED_OUT = NOTIFICATION_BASE + 7;
    public static final int NOTIFICATION_PARTICIPANT_DISCONNECTED = NOTIFICATION_BASE + 8;
    public static final int NOTIFICATION_GROUP_QUIT_RST = NOTIFICATION_BASE + 9;
    public static final int NOTIFICATION_GROUP_ABORT_RST = NOTIFICATION_BASE + 10;
    public static final int NOTIFICATION_ADD_PARTICIPANT_RST = NOTIFICATION_BASE + 11;
    public static final int NOTIFICATION_REMOVE_PARTICIPANT_RST = NOTIFICATION_BASE + 12;
    public static final int NOTIFICATION_BEEN_KICKED_OUT = NOTIFICATION_BASE + 13;
    public static final int NOTIFICATION_MODIFY_SUBJECT_RST = NOTIFICATION_BASE + 14;
    public static final int NOTIFICATION_SUBJECT_CHANGED = NOTIFICATION_BASE + 15;
    public static final int NOTIFICATION_TRANSFER_CHAIRMAN_RST = NOTIFICATION_BASE + 16;
    public static final int NOTIFICATION_CHAIRMAN_TRANSFERED = NOTIFICATION_BASE + 17;
    public static final int NOTIFICATION_MODIFY_MYNICKNAME_RST = NOTIFICATION_BASE + 18;
    public static final int NOTIFICATION_RECEIVE_MESSAGE = NOTIFICATION_BASE + 19;
    public static final int NOTIFICATION_GROUP_DISSOLVED = NOTIFICATION_BASE + 20;
    public static final int NOTIFICATION_NICKNAME_CHANGED = NOTIFICATION_BASE + 21;
    public static final int NOTIFICATION_GROUP_ABORT_BY_CHAIRMAN = NOTIFICATION_BASE + 25;

    public String msgToString(int msg);

    public void setChatId(String chatId);

    public String getChatId();

    public String getSubject();

    public void setSubject(String subject);

    public List<String> getParticipants();

    public void startGroup();

    public void handleInvitation(String rejoinId);

    public void invitationAccepted();

    public void invitationRejected();

    public void sendChatMessage(final int type, final long msgIdInSMS, final String content);

    public void resendChatMessage(final String messageId);

    public void sendFile(final long msgId, final String filePath);

    public void resendFile(final long msgId);

    public void downloadFile(String fileTag);

    public void redownloadFile(String fileTransferTag);

    public void handleFTInvitation(Intent intent);

    public Handler getGroupConfigHandler();

    public void onStatusChanged(boolean status);

    public void onCoreServiceDown();
}
