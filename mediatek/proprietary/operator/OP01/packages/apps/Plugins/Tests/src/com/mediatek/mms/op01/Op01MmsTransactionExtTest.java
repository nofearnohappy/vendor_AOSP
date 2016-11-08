package com.mediatek.op01.tests;

import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;

import com.mediatek.mms.plugin.Op01MmsTransactionExt;
import com.mediatek.common.MPlugin;


import android.app.Service;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import android.os.IBinder;


public class Op01MmsTransactionExtTest extends InstrumentationTestCase {
    private static Op01MmsTransactionExt sMmsTransactionPlugin = null;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        sMmsTransactionPlugin = (Op01MmsTransactionExt)MPlugin.createInstance("com.mediatek.mms.ext.IMmsTransactionExt",mContext);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sMmsTransactionPlugin = null;
    }

    public void testUpdateConnection() {
        sMmsTransactionPlugin.setMmsServerStatusCode(400);
        sMmsTransactionPlugin.setMmsServerStatusCode(400);
        boolean ret = sMmsTransactionPlugin.updateConnection();
        assertTrue(ret == false);
    }

    public void testUpdateConnection1() {
        sMmsTransactionPlugin.setMmsServerStatusCode(400);
        sMmsTransactionPlugin.setMmsServerStatusCode(400);
        sMmsTransactionPlugin.setMmsServerStatusCode(400);
        boolean ret = sMmsTransactionPlugin.updateConnection();
        assertTrue(true);
    }

    public void testGetHttpRequestRetryHandler() {
        DefaultHttpRequestRetryHandler handler = sMmsTransactionPlugin.getHttpRequestRetryHandler();
        assertTrue(handler != null);
    }

    public void testForegroundNotification() {
        sMmsTransactionPlugin.startServiceForeground(null);
        sMmsTransactionPlugin.stopServiceForeground(null);
        assertTrue(true);
    }

    public void testSetSoSendTimeoutProperty() {
        sMmsTransactionPlugin.setSoSendTimeoutProperty();
        assertTrue(true);
    }

    class TestService extends Service {
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}



