package com.mediatek.mms.plugin;

import android.content.Context;
import com.mediatek.mms.ext.DefaultOpPushReceiverExt;


public class Op09PushReceiverExt extends DefaultOpPushReceiverExt {

    @Override
    public void doInBackground(Context context) {
        // TODO Auto-generated method stub
        if (MessageUtils.isLowMemoryNotifEnable()) {
            MessageUtils.dealCTDeviceLowNotification(context);
        }
    }
}
