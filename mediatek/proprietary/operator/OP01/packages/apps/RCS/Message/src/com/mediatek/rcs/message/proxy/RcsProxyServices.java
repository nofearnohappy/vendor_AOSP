package com.mediatek.rcs.message.proxy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.mms.ipmessage.DefaultIpEmptyServiceExt;

/**
 * RcsProxyServices.
 *
 */
public class RcsProxyServices extends DefaultIpEmptyServiceExt {
    private final static String TAG = "mtk80999";
    private static final String SERVICE_ACTION = "com.mediatek.rcs.EmptyService";
    private static final String SERVICE_PACKAGE_NAME = "com.android.mms";
    private static final String TAG_SERVICE = "service";
    private static final String SERVICE_MESSAGE_SENDER = "servcie_IMessageSender";

    private Service mService;
    private RcsMessageSender mRcsMessageSender;


    @Override
    public void onCreate(Service service) {
        mService = service;
        Log.d(TAG, "onCreate: " + service);
//        mService = service;
        mRcsMessageSender = RcsMessageSender.getInstance(service);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return 0;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        String service = intent.getStringExtra(TAG_SERVICE);
        Log.d(TAG, "onBind: intent service = " + service);
        if (service == null) {
            return null;
        }
        if (service.equals(SERVICE_MESSAGE_SENDER)) {
            return mRcsMessageSender;
        } else {
            return null;
        }
    }
}
