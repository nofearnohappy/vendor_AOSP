package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

public class DefaultOpSmsReceiverExt extends ContextWrapper implements
        IOpSmsReceiverExt {

    public DefaultOpSmsReceiverExt(Context base) {
        super(base);
    }

    @Override
    public void onReceiveWithPrivilege(Intent intent, String action) {

    }

}
