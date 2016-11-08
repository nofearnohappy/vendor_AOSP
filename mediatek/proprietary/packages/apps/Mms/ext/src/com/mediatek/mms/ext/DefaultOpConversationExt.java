package com.mediatek.mms.ext;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;

public class DefaultOpConversationExt extends ContextWrapper implements
        IOpConversationExt {

    public DefaultOpConversationExt(Context base) {
        super(base);
    }


    /**
     * init IOpConversationExt from cursor
     */
    @Override
    public void fillFromCursor(Cursor cursor,int recipSize, boolean hasDraft) {
        return;
    }

    @Override
    public boolean startQuery(AsyncQueryHandler handler, int token, String selection) {
        return false;
        // TODO Auto-generated method stub

    }
}
