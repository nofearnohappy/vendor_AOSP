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

import com.mediatek.mms.callback.IMessageUtilsCallback;
import com.mediatek.mms.ext.DefaultOpMessagePluginExt;
import com.mediatek.mms.ext.IOpAttachmentEditorExt;
import com.mediatek.mms.ext.IOpAudioModelExt;
import com.mediatek.mms.ext.IOpCBMessageReceiverServiceExt;
import com.mediatek.mms.ext.IOpClassZeroActivityExt;
import com.mediatek.mms.ext.IOpComposeExt;
import com.mediatek.mms.ext.IOpConversationExt;
import com.mediatek.mms.ext.IOpConversationListExt;
import com.mediatek.mms.ext.IOpConversationListItemExt;
import com.mediatek.mms.ext.IOpDefaultRetrySchemeExt;
import com.mediatek.mms.ext.IOpDeliveryReportExt;
import com.mediatek.mms.ext.IOpDialogModeActivityExt;
import com.mediatek.mms.ext.IOpFileAttachmentModelExt;
import com.mediatek.mms.ext.IOpFileAttachmentUtilsExt;
import com.mediatek.mms.ext.IOpLoadReqExt;
import com.mediatek.mms.ext.IOpManageSimMessagesExt;
import com.mediatek.mms.ext.IOpMessageItemExt;
import com.mediatek.mms.ext.IOpMessageListAdapterExt;
import com.mediatek.mms.ext.IOpMessageListItemExt;
import com.mediatek.mms.ext.IOpMessageUtilsExt;
import com.mediatek.mms.ext.IOpMessagingNotificationExt;
import com.mediatek.mms.ext.IOpMmsConfigExt;
import com.mediatek.mms.ext.IOpMmsDraftDataExt;
import com.mediatek.mms.ext.IOpMmsPlayerActivityAdapterExt;
import com.mediatek.mms.ext.IOpMmsPlayerActivityExt;
import com.mediatek.mms.ext.IOpMmsPushReceiveServiceExt;
import com.mediatek.mms.ext.IOpMmsSystemEventReceiverExt;
import com.mediatek.mms.ext.IOpMmsThumbnailPresenterExt;
import com.mediatek.mms.ext.IOpMultiDeleteActivityExt;
import com.mediatek.mms.ext.IOpNotificationTransactionExt;
import com.mediatek.mms.ext.IOpRetrieveTransactionExt;
import com.mediatek.mms.ext.IOpPushReceiverExt;
import com.mediatek.mms.ext.IOpRecipientListActivityExt;
import com.mediatek.mms.ext.IOpRecipientsEditorExt;
import com.mediatek.mms.ext.IOpRetrySchedulerExt;
import com.mediatek.mms.ext.IOpSearchActivityExt;
import com.mediatek.mms.ext.IOpSiManagerExt;
import com.mediatek.mms.ext.IOpSlideEditorActivityExt;
import com.mediatek.mms.ext.IOpSlideViewExt;
import com.mediatek.mms.ext.IOpSlideshowActivityExt;
import com.mediatek.mms.ext.IOpSlideshowPresenterExt;
import com.mediatek.mms.ext.IOpSmsMessageSenderExt;
import com.mediatek.mms.ext.IOpSmsReceiverExt;
import com.mediatek.mms.ext.IOpSmsReceiverServiceExt;
import com.mediatek.mms.ext.IOpSmsRejectedReceiverExt;
import com.mediatek.mms.ext.IOpSmsSingleRecipientSenderExt;
import com.mediatek.mms.ext.IOpStatusBarSelectorCreatorExt;
import com.mediatek.mms.ext.IOpSubSelectActivityExt;
import com.mediatek.mms.ext.IOpTransactionServiceExt;
import com.mediatek.mms.ext.IOpWPMessageListItemExt;
import com.mediatek.mms.ext.IOpWapPushReceiverServiceExt;
import com.mediatek.mms.ext.IOpWorkingMessageExt;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpMessagePluginExt")
public class Op09MessagePluginExt extends DefaultOpMessagePluginExt {

    public Op09MessagePluginExt(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public static IMessageUtilsCallback sCallback;

    @Override
    public void setOpMessageUtilsCallback(IMessageUtilsCallback callback) {
        sCallback = callback;
    }

    @Override
    public IOpMmsConfigExt getOpMmsConfigExt() {
        return new Op09MmsConfigExt();
    }
    @Override
    public IOpConversationExt getOpConversationExt() {
        return new OP09ConversationExt(mContext);
    }

    @Override
    public IOpDialogModeActivityExt getOpDialogModeActivityExt() {
        return new Op09DialogModeActivityExt(mContext);
    }

    @Override
    public IOpManageSimMessagesExt getOpManageSimMessagesExt() {
        return new Op09ManageSimMessagesExt(mContext);
    }

    @Override
    public IOpMessageListItemExt getOpMessageListItemExt() {
        return new Op09MessageListItemExt(mContext);
    }

    @Override
    public IOpMessageUtilsExt getOpMessageUtilsExt() {
        return new Op09MessageUtilsExt(mContext);
    }

    @Override
    public IOpMessagingNotificationExt getOpMessagingNotificationExt() {
        return new OP09MessagingNotificationExt(mContext);
    }

    @Override
    public IOpMmsPlayerActivityAdapterExt getOpMmsPlayerActivityAdapterExt() {
        return new Op09MmsPlayerActivityAdapterExt(mContext);
    }

    @Override
    public IOpPushReceiverExt getOpPushReceiverExt() {
        return new Op09PushReceiverExt();
    }

    @Override
    public IOpRecipientsEditorExt getOpRecipientsEditorExt() {
        return new Op09RecipientsEditorExt(mContext);
    }

    @Override
    public IOpSlideViewExt getOpSlideViewExt() {
        return new Op09SlideViewExt(mContext);
    }

    @Override
    public IOpTransactionServiceExt getOpTransactionServiceExt() {
        return new Op09TransactionServiceExt(mContext);
    }

    @Override
    public IOpComposeExt getOpComposeExt() {
        return new Op09ComposeActivityExt(mContext);
    }

    @Override
    public IOpConversationListExt getOpConversationListExt() {
        return new Op09ConversationListExt(mContext);
    }

    @Override
    public IOpMultiDeleteActivityExt getOpMultiDeleteActivityExt() {
        return new Op09MultiDeleteActivityExt(mContext);
    }

    @Override
    public IOpNotificationTransactionExt getOpNotificationTransactionExt() {
        return new Op09NotificationTransactionExt(mContext);
    }

    @Override
    public IOpRetrieveTransactionExt getOpRetrieveTransactionExt() {
        return new Op09RetrieveTransactionExt(mContext);
    }

    @Override
    public IOpSmsReceiverServiceExt getOpSmsReceiverServiceExt() {
        return new Op09SmsReceiverServiceExt(mContext);
    }

    @Override
    public IOpStatusBarSelectorCreatorExt getOpStatusBarSelectorCreatorExt() {
        return new Op09StatusBarSelectorCreatorExt(mContext);
    }

    @Override
    public IOpSubSelectActivityExt getOpSubSelectActivityExt() {
        return new Op09SubSelectActivityExt();
    }

    @Override
    public IOpMmsSystemEventReceiverExt getOpMmsSystemEventReceiverExt() {
        return new Op09MmsSystemEventReceiverExt(mContext);
    }

    @Override
    public IOpSmsRejectedReceiverExt getOpSmsRejectedReceiverExt() {
        return new Op09SmsRejectedReceiverExt(mContext);
    }

    @Override
    public IOpWorkingMessageExt getOpWorkingMessageExt() {
        return new Op09WorkingMessageExt();
    }

    @Override
    public IOpDefaultRetrySchemeExt getOpDefaultRetrySchemeExt() {
        return new Op09DefaultRetrySchemeExt();
    }

    @Override
    public IOpSmsMessageSenderExt getOpSmsMessageSenderExt() {
        return new Op09SmsMessageSenderExt(mContext);
    }

    @Override
    public IOpMmsDraftDataExt getOpMmsDraftDataExt() {
        return new Op09MmsDraftDataExt();
    }

    @Override
    public IOpLoadReqExt getOpLoadReqExt() {
        return new Op09LoadReqExt();
    }

    @Override
    public IOpDeliveryReportExt getOpDeliveryReportExt() {
        return new Op09DeliveryReportExt(mContext);
    }

    @Override
    public IOpAudioModelExt getOpAudioModelExt() {
        return new Op09AudioModelExt();
    }

    @Override
    public IOpAttachmentEditorExt getOpAttachmentEditorExt() {
        return new Op09AttachmentEditorExt(mContext);
    }

    @Override
    public IOpMessageItemExt getOpMessageItemExt() {
        return new Op09MessageItemExt();
    }

    @Override
    public IOpMmsPushReceiveServiceExt getOpMmsPushReceiveServiceExt() {
        return new Op09MmsPushReceiveServiceExt();
    }

    @Override
    public IOpSmsSingleRecipientSenderExt getOpSmsSingleRecipientSenderExt() {
        return new Op09SmsSingleRecipientSenderExt();
    }

    @Override
    public IOpClassZeroActivityExt getOpClassZeroActivityExt() {
        return new Op09ClassZeroActivityExt();
    }

    @Override
    public IOpConversationListItemExt getOpConversationListItemExt() {
        return new Op09ConversationListItemExt(mContext);
    }

    @Override
    public IOpMmsThumbnailPresenterExt getOpMmsThumbnailPresenterExt() {
        return new Op09MmsThumbnailPresenterExt(mContext);
    }

    @Override
    public IOpRecipientListActivityExt getOpRecipientListActivityExt() {
        return new Op09RecipientListActivityExt();
    }

    @Override
    public IOpSearchActivityExt getOpSearchActivityExt() {
        return new Op09SearchActivityExt(mContext);
    }

    @Override
    public IOpSlideshowPresenterExt getOpSlideshowPresenterExt() {
        return new Op09SlideshowPresenterExt();
    }

    @Override
    public IOpFileAttachmentUtilsExt getOpFileAttachmentUtilsExt() {
        return new Op09FileAttachmentUtilsExt();
    }

    @Override
    public IOpSiManagerExt getOpSiManagerExt() {
        return new Op09SiManagerExt();
    }

    @Override
    public IOpWPMessageListItemExt getOpWPMessageListItemExt() {
        return new Op09WPMessageListItemExt();
    }

    @Override
    public IOpWapPushReceiverServiceExt getOpWapPushReceiverServiceExt() {
        return new Op09WapPushReceiverServiceExt();
    }

    @Override
    public IOpCBMessageReceiverServiceExt getOpCBMessageReceiverServiceExt() {
        return new Op09CBMessageReceiverServiceExt();
    }

    @Override
    public IOpFileAttachmentModelExt getOpFileAttachmentModelExt() {
        return new Op09FileAttachmentModelExt();
    }

    public IOpSmsReceiverExt getOpSmsReceiverExt() {
        return new Op09SmsReceiverExt(mContext);
    }

    @Override
    public IOpRetrySchedulerExt getOpRetrySchedulerExt() {
        return new Op09RetrySchedulerExt(mContext);
    }

    @Override
    public IOpSlideshowActivityExt getOpSlideshowActivityExt() {
        return new Op09SlideShowActivityExt(mContext);
    }

    public IOpMmsPlayerActivityExt getOpMmsPlayerActivityExt() {
        return new Op09MmsPlayerActivityExt(mContext);
    }

    public IOpSlideEditorActivityExt getOpSlideEditorActivityExt() {
        return new Op09SlideEditorActivityExt(mContext);
    }
}
