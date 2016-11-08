package com.mediatek.mms.ext;

import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.net.Uri;

public interface IOpConversationExt {
    /**
     * init IOpConversationExt from cursor
     * @internal
     */
    void fillFromCursor(Cursor cursor, int recipSize, boolean hasDraft);

    /**
     * @internal
     */
    boolean startQuery(AsyncQueryHandler handler, int token, String selection);
}
