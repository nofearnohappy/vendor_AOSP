package com.mediatek.mms.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

public class DefaultOpMmsSystemEventReceiverExt extends ContextWrapper
        implements IOpMmsSystemEventReceiverExt {

    public DefaultOpMmsSystemEventReceiverExt(Context base) {
        super(base);
    }

    @Override
    public void onReceive(Context context, Intent intent, final int tempFileNameLen) {

    }

    @Override
    public void setNotificationIndUnstarted(ContentValues values, int status) {

    }

}
