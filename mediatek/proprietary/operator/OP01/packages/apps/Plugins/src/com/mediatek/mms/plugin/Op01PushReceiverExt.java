package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;

import com.mediatek.mms.ext.DefaultOpPushReceiverExt;

/**
 * Op01PushReceiverExt.
 *
 */
public class Op01PushReceiverExt extends DefaultOpPushReceiverExt {

    @Override
    public boolean onReceive(Context context, Intent intent, Class<?> cls) {
        intent.setClass(context, cls);
        context.startService(intent);
        return true;
    }
}
