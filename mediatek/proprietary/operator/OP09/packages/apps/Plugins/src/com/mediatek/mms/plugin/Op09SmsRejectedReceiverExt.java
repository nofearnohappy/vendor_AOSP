package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.ext.DefaultOpSmsRejectedReceiverExt;

public class Op09SmsRejectedReceiverExt extends DefaultOpSmsRejectedReceiverExt {

    public Op09SmsRejectedReceiverExt(Context base) {
        super(base);
    }

    public void onReceive(Context context) {
        MessageUtils.cancelCTDeviceLowNotification(context);
    }
}
