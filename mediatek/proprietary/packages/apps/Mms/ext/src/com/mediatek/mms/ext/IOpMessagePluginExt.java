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

import com.mediatek.mms.ext.IOpLoadReqExt;
import com.mediatek.mms.ext.IOpMmsDraftDataExt;

import com.mediatek.mms.callback.IMessageUtilsCallback;

public interface IOpMessagePluginExt {

    /**
     * get IOpComposeExt from plugin or default.
     *
     * @return IOpComposeExt
     * @internal
     */
    IOpComposeExt getOpComposeExt();

    /**
     * @internal
     */
    IOpWorkingMessageExt getOpWorkingMessageExt();

    /**
     * @internal
     */
    IOpMessagingNotificationExt getOpMessagingNotificationExt();

    /**
     * @internal
     */
    IOpMessageListItemExt getOpMessageListItemExt();

    /**
     * @internal
     */
    IOpMmsPlayerActivityExt getOpMmsPlayerActivityExt();

    /**
     * @internal
     */
    IOpMmsPlayerActivityAdapterExt getOpMmsPlayerActivityAdapterExt();

    /**
     * @internal
     */
    IOpSlideViewExt getOpSlideViewExt();

    /**
     * @internal
     */
    IOpRecipientsEditorExt getOpRecipientsEditorExt();

    /**
     * @internal
     */
    IOpConversationExt getOpConversationExt();

    /**
     * @internal
     */
    IOpTransactionServiceExt getOpTransactionServiceExt();

    /**
     * @internal
     */
    IOpDialogModeActivityExt getOpDialogModeActivityExt();

    /**
     * @internal
     */
    IOpWappushMessagingNotificationExt getOpWappushMessagingNotificationExt();

    /**
     * @internal
     */
    IOpPushReceiverExt getOpPushReceiverExt();

    /**
     * @internal
     */
    IOpManageSimMessagesExt getOpManageSimMessagesExt();

    /**
     * @internal
     */
    IOpSlideshowEditActivityExt getOpSlideshowEditActivityExt();

    /**
     * @internal
     */
    IOpMessageUtilsExt getOpMessageUtilsExt();

    /**
     * @internal
     */
    IOpMmsWidgetServiceExt getOpMmsWidgetServiceExt();

    /**
     * @internal
     */
    IOpMmsAppExt getOpMmsAppExt();

    /**
     * @internal
     */
    IOpConversationListExt getOpConversationListExt();

    /**
     * @internal
     */
    IOpMultiDeleteActivityExt getOpMultiDeleteActivityExt();

    /**
     * @internal
     */
    IOpMessageListAdapterExt getOpMessageListAdapterExt();

    /**
     * @internal
     */
    IOpNotificationTransactionExt getOpNotificationTransactionExt();

    /**
     * @internal
     */
    IOpRetrieveTransactionExt getOpRetrieveTransactionExt();

    /**
     * @internal
     */
    IOpFolderModeSmsViewerExt getOpFolderModeSmsViewerExt();

    /**
     * @internal
     */
    IOpGeneralPreferenceActivityExt getOpGeneralPreferenceActivityExt();

    /**
     * @internal
     */
    IOpMmsPreferenceActivityExt getOpMmsPreferenceActivityExt();

    /**
     * @internal
     */
    IOpSubSelectActivityExt getOpSubSelectActivityExt();

    /**
     * @internal
     */
    IOpStatusBarSelectorCreatorExt getOpStatusBarSelectorCreatorExt();

    /**
     * @internal
     */
    IOpSlideEditorActivityExt getOpSlideEditorActivityExt();

    /**
     * @internal
     */
    IOpWPMessageActivityExt getOpWPMessageActivityExt();

    /**
     * @internal
     */
    IOpSmsReceiverServiceExt getOpSmsReceiverServiceExt();

    /**
     * @internal
     */
    void setOpMessageUtilsCallback(IMessageUtilsCallback callback);

    /**
     * @internal
     */
    IOpSmsReceiverExt getOpSmsReceiverExt();

    /**
     * @internal
     */
    IOpSmsRejectedReceiverExt getOpSmsRejectedReceiverExt();

    /**
     * @internal
     */
    IOpMmsSystemEventReceiverExt getOpMmsSystemEventReceiverExt();

    /**
     * @internal
     */
    IOpSlideshowModelExt getOpSlideshowModelExt();

    /**
     * @internal
     */
    IOpMmsConfigExt getOpMmsConfigExt();

    /**
     * @internal
     */
    IOpDefaultRetrySchemeExt getOpDefaultRetrySchemeExt();

    /**
     * @internal
     */
    IOpSmsMessageSenderExt getOpSmsMessageSenderExt();

    /**
     * @internal
     */
    IOpRetrySchedulerExt getOpRetrySchedulerExt();

    /**
     * @internal
     */
    IOpBootActivityExt getOpBootActivityExt();

    /**
     * @internal
     */
    IOpFileAttachmentModelExt getOpFileAttachmentModelExt();

    /**
     * @internal
     */
    IOpMmsDraftDataExt getOpMmsDraftDataExt();

    /**
     * @internal
     */
    IOpLoadReqExt getOpLoadReqExt();

    /**
     * @internal
     */
    IOpDeliveryReportExt getOpDeliveryReportExt();

    /**
     * @internal
     */
    IOpAudioModelExt getOpAudioModelExt();

    /**
     * @internal
     */
    IOpAttachmentEditorExt getOpAttachmentEditorExt();

    /**
     * @internal
     */
    IOpMessageItemExt getOpMessageItemExt();

    /**
     * @internal
     */
    IOpMmsPushReceiveServiceExt getOpMmsPushReceiveServiceExt();

    /**
     * @internal
     */
    IOpSmsSingleRecipientSenderExt getOpSmsSingleRecipientSenderExt();

    /**
     * @internal
     */
    IOpClassZeroActivityExt getOpClassZeroActivityExt();

    /**
     * @internal
     */
    IOpConversationListItemExt getOpConversationListItemExt();

    /**
     * @internal
     */
    IOpMmsThumbnailPresenterExt getOpMmsThumbnailPresenterExt();

    /**
     * @internal
     */
    IOpMultiSaveActivityExt getOpMultiSaveActivityExt();

    /**
     * @internal
     */
    IOpRecipientListActivityExt getOpRecipientListActivityExt();

    /**
     * @internal
     */
    IOpSearchActivityExt getOpSearchActivityExt();

    /**
     * @internal
     */
    IOpSlideshowPresenterExt getOpSlideshowPresenterExt();

    /**
     * @internal
     */
    IOpSlideshowActivityExt getOpSlideshowActivityExt();

    /**
     * @internal
     */
    IOpFileAttachmentUtilsExt getOpFileAttachmentUtilsExt();

    /**
     * @internal
     */
    IOpSmsPreferenceActivityExt getOpSmsPreferenceActivityExt();

    /**
     * @internal
     */
    IOpSiManagerExt getOpSiManagerExt();

    /**
     * @internal
     */
    IOpWPMessageListItemExt getOpWPMessageListItemExt();

    /**
     * @internal
     */
    IOpWapPushReceiverServiceExt getOpWapPushReceiverServiceExt();

    /**
     * @internal
     */
    IOpCBMessageReceiverServiceExt getOpCBMessageReceiverServiceExt();


























}

