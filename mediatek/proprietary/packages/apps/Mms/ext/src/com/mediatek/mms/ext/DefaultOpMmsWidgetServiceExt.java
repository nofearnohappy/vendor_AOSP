package com.mediatek.mms.ext;

import com.mediatek.mms.callback.IConversationCallback;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

public class DefaultOpMmsWidgetServiceExt extends ContextWrapper implements
        IOpMmsWidgetServiceExt {

    public DefaultOpMmsWidgetServiceExt(Context base) {
        super(base);
    }

    @Override
    public boolean getViewAt(Intent intent, boolean dirMode, IConversationCallback convCallback) {
        return false;
    }

    public boolean getViewMoreConversationsView(Intent intent, boolean dirMode) {
        return false;
    }
}
