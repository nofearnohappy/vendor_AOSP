package com.mediatek.mms.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public interface IOpMmsSystemEventReceiverExt {
    /**
     * @internal
     */
    void onReceive(Context context, Intent intent, final int tempFileNameLen);
    /**
     * @internal
     */
    void setNotificationIndUnstarted(ContentValues values, int status);
}
