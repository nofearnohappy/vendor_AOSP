/**
 *
 */
package com.mediatek.dm.test;

import junit.framework.Assert;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;
import com.mediatek.dm.DmController;
import com.mediatek.dm.DmService;
import com.mediatek.dm.session.DmSessionStateObserver;

import com.redbend.vdm.fumo.FumoState;

import android.util.Log;



/**
 * @author MTK80987
 *
 */
public class DmControllerTest extends AndroidTestCase {
    private static final String TAG = "[DmControllerTest]";
    private static final int TIME_OUT = 1000;
    private Context mContext;
    private DmController mController;

    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();
        Intent dmIntent = new Intent();
        dmIntent.setAction(Intent.ACTION_DEFAULT);
        dmIntent.setClass(mContext, DmService.class);
        mContext.startService(dmIntent);
        Thread.sleep(1000);

        mController = new DmController(mContext);
        mController.createEngine(mContext);
        mController.startEngine();
    }

    public void testStop() {
        Log.d(TAG, "test stop begin");
        mController.stop();

        Log.d(TAG, "test stop end");
    }

    /*public void testProceedDLSession()
    {
        Log.d(TAG, "test proceedDLSession begin");
        try {
            mController.proceedDLSession();
        }
        catch (Exception e) {
            Log.d(TAG, "test proceedDLSession fail");
        }
    }*/

    public void testCancelDLSession() {
        Log.d(TAG, "test cancelDlSession begin");

        try {
            mController.cancelDLSession();
        }
        catch (Exception e) {
            Log.d(TAG, "test cancelDLSession fail");
        }
    }

    public void testResumeDLSession() {
        Log.d(TAG, "test resumeDLSession begin");

        try {
            mController.resumeDLSession();
        }
        catch (Exception e) {
            Log.d(TAG, "test resumelDLSession fail");
        }
    }

    public void testGetDmAction() {
        Log.d(TAG, "test getDmAction begin");

        DmSessionStateObserver.DmAction  action = mController.getDmAction();
        Assert.assertNotNull(action);
    }

    public void testTriggerLawmoReportSession() {
        Log.d(TAG, "test triggerLawmoReportSession begin");
        try {
            mController.triggerLawmoReportSession(null, null);
        }
        catch (Exception e) {
            Log.d(TAG, "test triggerLawmoReportSession fail");
        }

    }

    public void testGetFumoState() {
        Log.d(TAG, "test getFumoState begin");

        try {
            FumoState state = mController.getFumoState();
            Assert.assertNotNull(state);
        }
        catch (Exception e) {
            Log.d(TAG, "test getFumoState fail");
        }
    }

    public void testIsIdle() {
        Log.d(TAG, "test isIdle begin");
        Assert.assertNotNull(mController.isIdle());
    }

    public void testSetTimeout() {
        Log.d(TAG, "test setTime begin");

        try {
            mController.setTimeout(TIME_OUT);
        }
        catch (Exception e) {
            Log.d(TAG, "tst setTimeout fail");
        }
    }

    public void testQuerySessionActions() {
        Log.d(TAG, "test querySessionxxx begin");

        mController.queryFumoSessionActions();
        mController.queryLawmoSessionActions();
        mController.queryScomoSessionActions();
    }

    public void testTeminateSessionActions() {
        Log.d(TAG, "test teminate session actions begin");

        try {
            mController.terminateFumo();
            mController.terminateLawmo();
            mController.terminateScomo();
        }
        catch (Exception e) {
            Log.d(TAG, "test teminate session action fail");
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
