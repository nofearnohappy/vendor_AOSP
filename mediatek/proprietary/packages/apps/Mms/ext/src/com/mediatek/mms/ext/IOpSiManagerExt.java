package com.mediatek.mms.ext;

import android.content.ContentValues;
import android.content.Context;

public interface IOpSiManagerExt {

    /**
     * @internal
     */
    void handleIncoming(Context context, ContentValues values, String uri);

}
