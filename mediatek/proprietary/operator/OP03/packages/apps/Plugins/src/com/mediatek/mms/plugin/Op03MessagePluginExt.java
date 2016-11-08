package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpMessagePluginExt;
import com.mediatek.mms.ext.IOpComposeExt;
import com.mediatek.mms.ext.IOpConversationListExt;
import com.mediatek.mms.ext.IOpSmsPreferenceActivityExt;
import com.mediatek.mms.ext.IOpSmsSingleRecipientSenderExt;
import com.mediatek.mms.ext.IOpWorkingMessageExt;

@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpMessagePluginExt")
public class Op03MessagePluginExt extends DefaultOpMessagePluginExt {

    private Context mContext;
    public Op03MessagePluginExt(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public IOpConversationListExt getOpConversationListExt() {
            return new Op03ConversationListExt(mContext);
    }

    @Override
    public IOpWorkingMessageExt getOpWorkingMessageExt() {
        return new Op03WorkingMessageExt();
    }

    @Override
    public IOpComposeExt getOpComposeExt() {
        return new Op03ComposeExt(mContext);
    }

    @Override
    public IOpSmsSingleRecipientSenderExt getOpSmsSingleRecipientSenderExt() {
        return new Op03SmsSingleRecipientSenderExt();
    }

    @Override
    public IOpSmsPreferenceActivityExt getOpSmsPreferenceActivityExt() {
        return new Op03SmsPreferenceActivityExt();
    }
}
