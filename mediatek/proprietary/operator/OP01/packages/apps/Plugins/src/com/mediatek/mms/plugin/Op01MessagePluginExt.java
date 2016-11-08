package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.callback.IMessageUtilsCallback;
import com.mediatek.mms.ext.DefaultOpMessagePluginExt;
import com.mediatek.mms.ext.IOpAttachmentEditorExt;
import com.mediatek.mms.ext.IOpBootActivityExt;
import com.mediatek.mms.ext.IOpComposeExt;
import com.mediatek.mms.ext.IOpConversationListExt;
import com.mediatek.mms.ext.IOpConversationListItemExt;
import com.mediatek.mms.ext.IOpDefaultRetrySchemeExt;
import com.mediatek.mms.ext.IOpDialogModeActivityExt;
import com.mediatek.mms.ext.IOpFileAttachmentModelExt;
import com.mediatek.mms.ext.IOpFileAttachmentUtilsExt;
import com.mediatek.mms.ext.IOpFolderModeSmsViewerExt;
import com.mediatek.mms.ext.IOpGeneralPreferenceActivityExt;
import com.mediatek.mms.ext.IOpManageSimMessagesExt;
import com.mediatek.mms.ext.IOpMessageListAdapterExt;
import com.mediatek.mms.ext.IOpMessageListItemExt;
import com.mediatek.mms.ext.IOpMessageUtilsExt;
import com.mediatek.mms.ext.IOpMessagingNotificationExt;
import com.mediatek.mms.ext.IOpMmsAppExt;
import com.mediatek.mms.ext.IOpMmsConfigExt;
import com.mediatek.mms.ext.IOpMmsPlayerActivityAdapterExt;
import com.mediatek.mms.ext.IOpMmsPlayerActivityExt;
import com.mediatek.mms.ext.IOpMmsPreferenceActivityExt;
import com.mediatek.mms.ext.IOpMmsWidgetServiceExt;
import com.mediatek.mms.ext.IOpMultiDeleteActivityExt;
import com.mediatek.mms.ext.IOpMultiSaveActivityExt;
import com.mediatek.mms.ext.IOpNotificationTransactionExt;
import com.mediatek.mms.ext.IOpPushReceiverExt;
import com.mediatek.mms.ext.IOpRecipientsEditorExt;
import com.mediatek.mms.ext.IOpRetrieveTransactionExt;
import com.mediatek.mms.ext.IOpSearchActivityExt;
import com.mediatek.mms.ext.IOpSlideEditorActivityExt;
import com.mediatek.mms.ext.IOpSlideViewExt;
import com.mediatek.mms.ext.IOpSlideshowEditActivityExt;
import com.mediatek.mms.ext.IOpSlideshowModelExt;
import com.mediatek.mms.ext.IOpSmsPreferenceActivityExt;
import com.mediatek.mms.ext.IOpSmsReceiverServiceExt;
import com.mediatek.mms.ext.IOpSmsSingleRecipientSenderExt;
import com.mediatek.mms.ext.IOpStatusBarSelectorCreatorExt;
import com.mediatek.mms.ext.IOpSubSelectActivityExt;
import com.mediatek.mms.ext.IOpTransactionServiceExt;
import com.mediatek.mms.ext.IOpWPMessageActivityExt;
import com.mediatek.mms.ext.IOpWappushMessagingNotificationExt;
import com.mediatek.mms.ext.IOpWorkingMessageExt;

/**
 * Op01MessagePluginExt. Used to get pluin instances.
 *
 */
@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpMessagePluginExt")
public class Op01MessagePluginExt extends DefaultOpMessagePluginExt {
    public static IMessageUtilsCallback sMessageUtilsCallback = null;

    /**
     * Construction.
     * @param context Context
     */
    public Op01MessagePluginExt(Context context) {
        super(context);
    }

    public IOpDialogModeActivityExt getOpDialogModeActivityExt() {
        return new Op01DialogModeActivityExt(mContext);
    }

    public IOpManageSimMessagesExt getOpManageSimMessagesExt() {
        return new Op01ManageSimMessagesExt(mContext);
    }

    public IOpMessageListItemExt getOpMessageListItemExt() {
        return new Op01MessageListItemExt(mContext);
    }

    public IOpMessageUtilsExt getOpMessageUtilsExt() {
        return new Op01MessageUtilsExt(mContext);
    }

    public IOpMessagingNotificationExt getOpMessagingNotificationExt() {
        return new Op01MessagingNotificationExt(mContext);
    }

    public IOpMmsAppExt getOpMmsAppExt() {
        return new Op01MmsAppExt(mContext);
    }

    public IOpMmsPlayerActivityAdapterExt getOpMmsPlayerActivityAdapterExt() {
        return new Op01MmsPlayerActivityAdapterExt(mContext);
    }

    public IOpMmsWidgetServiceExt getOpMmsWidgetServiceExt() {
        return new Op01MmsWidgetServiceExt(mContext);
    }

    public IOpPushReceiverExt getOpPushReceiverExt() {
        return new Op01PushReceiverExt();
    }

    public IOpRecipientsEditorExt getOpRecipientsEditorExt() {
        return new Op01RecipientsEditorExt(mContext);
    }

    public IOpSlideViewExt getOpSlideViewExt() {
        return new Op01SlideViewExt(mContext);
    }

    public IOpSlideshowEditActivityExt getOpSlideshowEditActivityExt() {
        return new Op01SlideshowEditActivityExt(mContext);
    }

    public IOpTransactionServiceExt getOpTransactionServiceExt() {
        return new Op01TransactionServiceExt(mContext);
    }

    public IOpWappushMessagingNotificationExt getOpWappushMessagingNotificationExt() {
        return new Op01WappushMessagingNotificationExt(mContext);
    }

    public IOpComposeExt getOpComposeExt() {
        return new Op01ComposeExt(mContext);
    }

    public IOpConversationListExt getOpConversationListExt() {
        return new Op01ConversationListExt(mContext);
    }

    public IOpConversationListItemExt getOpConversationListItemExt() {
        return new Op01ConversationListItemExt(mContext);
    }

    public IOpMultiDeleteActivityExt getOpMultiDeleteActivityExt() {
        return new Op01MultiDeleteActivityExt(mContext);
    }

    public IOpFolderModeSmsViewerExt getOpFolderModeSmsViewerExt() {
        return new Op01FolderModeSmsViewerExt(mContext);
    }

    public IOpGeneralPreferenceActivityExt getOpGeneralPreferenceActivityExt() {
        return new Op01GeneralPreferenceActivityExt(mContext);
    }

    public IOpMessageListAdapterExt getOpMessageListAdapterExt() {
        return new Op01MessageListAdapterExt(mContext);
    }

    public IOpMmsPlayerActivityExt getOpMmsPlayerActivityExt() {
        return new Op01MmsPlayerActivityExt(mContext);
    }

    public IOpMmsPreferenceActivityExt getOpMmsPreferenceActivityExt() {
        return new Op01MmsPreferenceActivityExt(mContext);
    }

    public IOpNotificationTransactionExt getOpNotificationTransactionExt() {
        return new Op01NotificationTransactionExt(mContext);
    }

    public IOpRetrieveTransactionExt getOpRetrieveTransactionExt() {
        return new Op01RetrieveTransactionExt(mContext);
    }

    public IOpSlideEditorActivityExt getOpSlideEditorActivityExt() {
        return new Op01SlideEditorActivityExt(mContext);
    }

    public IOpSmsReceiverServiceExt getOpSmsReceiverServiceExt() {
        return new Op01SmsReceiverServiceExt(mContext);
    }

    public IOpStatusBarSelectorCreatorExt getOpStatusBarSelectorCreatorExt() {
        return new Op01StatusBarSelectorCreatorExt(mContext);
    }

    public IOpSubSelectActivityExt getOpSubSelectActivityExt() {
        return new Op01SubSelectActivityExt(mContext);
    }

    public IOpWPMessageActivityExt getOpWPMessageActivityExt() {
        return new Op01WPMessageActivityExt(mContext);
    }

    public void setOpMessageUtilsCallback(IMessageUtilsCallback callback) {
        sMessageUtilsCallback = callback;
    }

    public IOpWorkingMessageExt getOpWorkingMessageExt() {
        return new Op01WorkingMessageExt();
    }

    public IOpSlideshowModelExt getOpSlideshowModelExt() {
        return new Op01SlideshowModelExt();
    }

    public IOpMmsConfigExt getOpMmsConfigExt() {
        return new Op01MmsConfigExt();
    }

    public IOpDefaultRetrySchemeExt getOpDefaultRetrySchemeExt() {
        return new Op01DefaultRetrySchemeExt();
    }

    public IOpBootActivityExt getOpBootActivityExt() {
        return new Op01BootActivityExt();
    }

    public IOpFileAttachmentModelExt getOpFileAttachmentModelExt() {
        return new Op01FileAttachmentModelExt();
    }

    public IOpFileAttachmentUtilsExt getOpFileAttachmentUtilsExt() {
        return new Op01FileAttachmentUtilsExt(mContext);
    }

    @Override
    public IOpAttachmentEditorExt getOpAttachmentEditorExt() {
        return new Op01AttachmentEditorExt();
    }

    @Override
    public IOpSmsPreferenceActivityExt getOpSmsPreferenceActivityExt() {
        return new Op01SmsPreferenceActivityExt(mContext);
    }

    public IOpMultiSaveActivityExt getOpMultiSaveActivityExt() {
        return new Op01MultiSaveActivityExt();
    }

    public IOpSearchActivityExt getOpSearchActivityExt() {
        return new Op01SearchActivityExt();
    }

    public IOpSmsSingleRecipientSenderExt getOpSmsSingleRecipientSenderExt() {
        return new Op01SmsSingleRecipientSenderExt();
    }
}
