package com.mediatek.rcs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.IIpEmptyReceiverExt;

public class EmptyReceiver extends BroadcastReceiver {

    private IIpEmptyReceiverExt mIpEmptyReceiver;
    
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        // TODO Auto-generated method stub
        if (mIpEmptyReceiver == null) {
            mIpEmptyReceiver = IpMessageUtils.getIpMessagePlugin(arg0).getIpEmptyReceiver();
        }
        mIpEmptyReceiver.onReceive(this, arg0, arg1);
    }
}
