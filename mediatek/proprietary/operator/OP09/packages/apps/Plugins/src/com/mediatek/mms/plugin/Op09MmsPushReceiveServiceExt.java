package com.mediatek.mms.plugin;

import android.content.Context;

import com.mediatek.mms.ext.DefaultOpMmsPushReceiveServiceExt;

public class Op09MmsPushReceiveServiceExt extends DefaultOpMmsPushReceiveServiceExt {

    @Override
    public void doInBackground(Context context) {
        if (MessageUtils.isLowMemoryNotifEnable()) {
            MessageUtils.dealCTDeviceLowNotification(context);
        }

    }

}
