package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;

public class DefaultOpSmsRejectedReceiverExt extends ContextWrapper implements
        IOpSmsRejectedReceiverExt {

    public DefaultOpSmsRejectedReceiverExt(Context base) {
        super(base);
    }

    @Override
    public void onReceive(Context context) {

    }

}
