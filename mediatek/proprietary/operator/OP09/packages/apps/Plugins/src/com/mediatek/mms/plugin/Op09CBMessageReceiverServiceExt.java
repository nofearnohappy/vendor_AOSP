package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.ext.DefaultOpCBMessageReceiverServiceExt;

public class Op09CBMessageReceiverServiceExt extends DefaultOpCBMessageReceiverServiceExt {

    @Override
    public void handleCBMessageReceived(Context context) {
        if (MessageUtils.isLowMemoryNotifEnable()) {
            MessageUtils.dealCTDeviceLowNotification(context);
        }
    }

}
