package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpMessagePluginExt;
import com.mediatek.mms.ext.IOpConversationListExt;
import com.mediatek.mms.ext.IOpMmsConfigExt;
import com.mediatek.mms.ext.IOpSmsPreferenceActivityExt;

@PluginImpl(interfaceName = "com.mediatek.mms.ext.IOpMessagePluginExt")
public class Op02MessagePluginExt extends DefaultOpMessagePluginExt {

    private Context mContext;
    public Op02MessagePluginExt(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public IOpMmsConfigExt getOpMmsConfigExt() {
        return new Op02MmsConfigExt();
    }

    @Override
    public IOpConversationListExt getOpConversationListExt() {
        return new Op02ConversationListExt(mContext);
    }

    @Override
    public IOpSmsPreferenceActivityExt getOpSmsPreferenceActivityExt() {
        return new Op02SmsPreferenceActivityExt();
    }
}
