package com.mediatek.mms.plugin;

import com.mediatek.mms.callback.ISplitToMmsAndSmsConversationCallback;
import com.mediatek.mms.callback.IConversationCallback;
import com.mediatek.mms.ext.DefaultOpWorkingMessageExt;

public class Op03WorkingMessageExt extends DefaultOpWorkingMessageExt {

    public boolean onCreateSplitToMmsAndSmsConv(
            ISplitToMmsAndSmsConversationCallback splitConv, IConversationCallback conv) {
        splitConv.splitConvCallback(conv);
        return true;
    }
}
