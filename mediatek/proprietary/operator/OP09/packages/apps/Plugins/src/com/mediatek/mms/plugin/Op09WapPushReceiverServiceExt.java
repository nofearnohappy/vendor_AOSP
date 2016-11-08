package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.ext.DefaultOpWapPushReceiverServiceExt;

public class Op09WapPushReceiverServiceExt extends DefaultOpWapPushReceiverServiceExt {

    @Override
    public void handleWapPushReceived(Context context) {
        if (MessageUtils.isLowMemoryNotifEnable()) {
            MessageUtils.dealCTDeviceLowNotification(context);
        }
    }

}
