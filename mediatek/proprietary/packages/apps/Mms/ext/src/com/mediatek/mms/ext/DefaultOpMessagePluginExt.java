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

package com.mediatek.mms.ext;

import com.mediatek.mms.ext.DefaultOpCBMessageReceiverServiceExt;
import com.mediatek.mms.ext.DefaultOpComposeExt;
import com.mediatek.mms.ext.DefaultOpMmsDraftDataExt;

import android.content.Context;

import com.mediatek.mms.callback.IMessageUtilsCallback;

public class DefaultOpMessagePluginExt implements IOpMessagePluginExt {
    protected Context mContext;

    public DefaultOpMessagePluginExt(Context context) {
        mContext = context;
    }

    @Override
    public IOpConversationExt getOpConversationExt() {
        return new DefaultOpConversationExt(mContext);
    }

    @Override
    public IOpDialogModeActivityExt getOpDialogModeActivityExt() {
        return new DefaultOpDialogModeActivityExt(mContext);
    }

    @Override
    public IOpManageSimMessagesExt getOpManageSimMessagesExt() {
        return new DefaultOpManageSimMessagesExt(mContext);
    }

    @Override
    public IOpMessageListItemExt getOpMessageListItemExt() {
        return new DefaultOpMessageListItemExt(mContext);
    }

    @Override
    public IOpMessageUtilsExt getOpMessageUtilsExt() {
        return new DefaultOpMessageUtilsExt(mContext);
    }

    @Override
    public IOpMessagingNotificationExt getOpMessagingNotificationExt() {
        return new DefaultOpMessagingNotificationExt(mContext);
    }

    @Override
    public IOpMmsAppExt getOpMmsAppExt() {
        return new DefaultOpMmsAppExt(mContext);
    }

    @Override
    public IOpMmsPlayerActivityAdapterExt getOpMmsPlayerActivityAdapterExt() {
        return new DefaultOpMmsPlayerActivityAdapterExt(mContext);
    }

    @Override
    public IOpMmsWidgetServiceExt getOpMmsWidgetServiceExt() {
        return new DefaultOpMmsWidgetServiceExt(mContext);
    }

    @Override
    public IOpPushReceiverExt getOpPushReceiverExt() {
        return new DefaultOpPushReceiverExt();
    }

    @Override
    public IOpRecipientsEditorExt getOpRecipientsEditorExt() {
        return new DefaultOpRecipientsEditorExt(mContext);
    }

    @Override
    public IOpSlideViewExt getOpSlideViewExt() {
        return new DefaultOpSlideViewExt(mContext);
    }

    @Override
    public IOpSlideshowEditActivityExt getOpSlideshowEditActivityExt() {
        return new DefaultOpSlideshowEditActivityExt(mContext);
    }

    @Override
    public IOpTransactionServiceExt getOpTransactionServiceExt() {
        return new DefaultOpTransactionServiceExt(mContext);
    }

    @Override
    public IOpWappushMessagingNotificationExt getOpWappushMessagingNotificationExt() {
        return new DefaultOpWappushMessagingNotificationExt(mContext);
    }
    @Override
    public IOpComposeExt getOpComposeExt() {
        return new DefaultOpComposeExt(mContext);
    }
    @Override
    public IOpConversationListExt getOpConversationListExt() {
        return new DefaultOpConversationListExt(mContext);
    }
    @Override
    public IOpMultiDeleteActivityExt getOpMultiDeleteActivityExt() {
        return new DefaultOpMultiDeleteActivityExt(mContext);
    }

    @Override
    public IOpFolderModeSmsViewerExt getOpFolderModeSmsViewerExt() {
        return new DefaultOpFolderModeSmsViewerExt(mContext);
    }

    @Override
    public IOpGeneralPreferenceActivityExt getOpGeneralPreferenceActivityExt() {
        return new DefaultOpGeneralPreferenceActivityExt(mContext);
    }

    @Override
    public IOpMessageListAdapterExt getOpMessageListAdapterExt() {
        return new DefaultOpMessageListAdapterExt(mContext);
    }

    @Override
    public IOpMmsPlayerActivityExt getOpMmsPlayerActivityExt() {
        return new DefaultOpMmsPlayerActivityExt(mContext);
    }

    @Override
    public IOpMmsPreferenceActivityExt getOpMmsPreferenceActivityExt() {
        return new DefaultOpMmsPreferenceActivityExt(mContext);
    }

    @Override
    public IOpNotificationTransactionExt getOpNotificationTransactionExt() {
        return new DefaultOpNotificationTransactionExt(mContext);
    }

    @Override
    public IOpRetrieveTransactionExt getOpRetrieveTransactionExt() {
        return new DefaultOpRetrieveTransactionExt(mContext);
    }

    @Override
    public IOpSlideEditorActivityExt getOpSlideEditorActivityExt() {
        return new DefaultOpSlideEditorActivityExt(mContext);
    }

    @Override
    public IOpSmsReceiverServiceExt getOpSmsReceiverServiceExt() {
        return new DefaultOpSmsReceiverServiceExt(mContext);
    }

    @Override
    public IOpStatusBarSelectorCreatorExt getOpStatusBarSelectorCreatorExt() {
        return new DefaultOpStatusBarSelectorCreatorExt(mContext);
    }

    @Override
    public IOpSubSelectActivityExt getOpSubSelectActivityExt() {
        return new DefaultOpSubSelectActivityExt();
    }

    @Override
    public IOpWPMessageActivityExt getOpWPMessageActivityExt() {
        return new DefaultOpWPMessageActivityExt(mContext);
    }

    @Override
    public void setOpMessageUtilsCallback(IMessageUtilsCallback callback) {
    }

    @Override
    public IOpMmsSystemEventReceiverExt getOpMmsSystemEventReceiverExt() {
        return new DefaultOpMmsSystemEventReceiverExt(mContext);
    }

    @Override
    public IOpSmsReceiverExt getOpSmsReceiverExt() {
        return new DefaultOpSmsReceiverExt(mContext);
    }

    @Override
    public IOpSmsRejectedReceiverExt getOpSmsRejectedReceiverExt() {
        return new DefaultOpSmsRejectedReceiverExt(mContext);
    }

    @Override
    public IOpWorkingMessageExt getOpWorkingMessageExt() {
        return new DefaultOpWorkingMessageExt();
    }

    @Override
    public IOpSlideshowModelExt getOpSlideshowModelExt() {
        return new DefaultOpSlideshowModelExt();
    }

    @Override
    public IOpMmsConfigExt getOpMmsConfigExt() {
        return new DefaultOpMmsConfigExt();
    }

    @Override
    public IOpDefaultRetrySchemeExt getOpDefaultRetrySchemeExt() {
        return new DefaultOpDefaultRetrySchemeExt();
    }

    @Override
    public IOpSmsMessageSenderExt getOpSmsMessageSenderExt() {
        return new DefaultOpSmsMessageSenderExt();
    }

    @Override
    public IOpRetrySchedulerExt getOpRetrySchedulerExt() {
        return new DefaultOpRetrySchedulerExt(mContext);
    }

    @Override
    public IOpBootActivityExt getOpBootActivityExt() {
        return new DefaultOpBootActivityExt();
    }

    @Override
    public IOpFileAttachmentModelExt getOpFileAttachmentModelExt() {
        return new DefaultOpFileAttachmentModelExt();
    }


    @Override
    public IOpMmsDraftDataExt getOpMmsDraftDataExt() {
        return new DefaultOpMmsDraftDataExt();
    }

    @Override
    public IOpLoadReqExt getOpLoadReqExt() {
        return new DefaultOpLoadReqExt();
    }

    @Override
    public IOpDeliveryReportExt getOpDeliveryReportExt() {
        return new DefaultOpDeliveryReportExt(mContext);
    }

    @Override
    public IOpAudioModelExt getOpAudioModelExt() {
        return new DefaultOpAudioModelExt();
    }

    @Override
    public IOpAttachmentEditorExt getOpAttachmentEditorExt() {
        return new DefaultOpAttachmentEditorExt();
    }

    @Override
    public IOpMessageItemExt getOpMessageItemExt() {
        return new DefaultOpMessageItemExt();
    }

    @Override
    public IOpMmsPushReceiveServiceExt getOpMmsPushReceiveServiceExt() {
        return new DefaultOpMmsPushReceiveServiceExt();
    }

    @Override
    public IOpSmsSingleRecipientSenderExt getOpSmsSingleRecipientSenderExt() {
        return new DefaultOpSmsSingleRecipientSenderExt();
    }

    @Override
    public IOpClassZeroActivityExt getOpClassZeroActivityExt() {
        return new DefaultOpClassZeroActivityExt();
    }

    @Override
    public IOpConversationListItemExt getOpConversationListItemExt() {
        return new DefaultOpConversationListItemExt(mContext);
    }

    @Override
    public IOpMmsThumbnailPresenterExt getOpMmsThumbnailPresenterExt() {
        return new DefaultOpMmsThumbnailPresenterExt();
    }

    @Override
    public IOpMultiSaveActivityExt getOpMultiSaveActivityExt() {
        return new DefaultOpMultiSaveActivityExt();
    }

    @Override
    public IOpRecipientListActivityExt getOpRecipientListActivityExt() {
        return new DefaultOpRecipientListActivityExt();
    }

    @Override
    public IOpSearchActivityExt getOpSearchActivityExt() {
        return new DefaultOpSearchActivityExt();
    }

    @Override
    public IOpSlideshowPresenterExt getOpSlideshowPresenterExt() {
        return new DefaultOpSlideshowPresenterExt();
    }

    @Override
    public IOpFileAttachmentUtilsExt getOpFileAttachmentUtilsExt() {
        return new DefaultOpFileAttachmentUtilsExt();
    }

    @Override
    public IOpSmsPreferenceActivityExt getOpSmsPreferenceActivityExt() {
        return new DefaultOpSmsPreferenceActivityExt();
    }

    @Override
    public IOpSiManagerExt getOpSiManagerExt() {
        return new DefaultOpSiManagerExt();
    }

    @Override
    public IOpWPMessageListItemExt getOpWPMessageListItemExt() {
        return new DefaultOpWPMessageListItemExt();
    }

    @Override
    public IOpWapPushReceiverServiceExt getOpWapPushReceiverServiceExt() {
        return new DefaultOpWapPushReceiverServiceExt();
    }

    @Override
    public IOpCBMessageReceiverServiceExt getOpCBMessageReceiverServiceExt() {
        return new DefaultOpCBMessageReceiverServiceExt();
    }

    @Override
    public IOpSlideshowActivityExt getOpSlideshowActivityExt() {
        return new DefaultOpSlideshowActivityExt();
    }


























}

