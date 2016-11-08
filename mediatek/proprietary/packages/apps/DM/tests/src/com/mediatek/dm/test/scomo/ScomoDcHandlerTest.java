package com.mediatek.dm.test.scomo;

import junit.framework.Assert;
import android.util.Log;

import com.mediatek.dm.DmService;
import com.mediatek.dm.scomo.DmScomoDcHandler;
import com.mediatek.dm.scomo.DmScomoHandler;

import android.content.Context;
import android.content.Intent;

import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDc;


import android.test.AndroidTestCase;

public class ScomoDcHandlerTest extends AndroidTestCase {
    private static final String TAG = "[ScomoDcHandlerTest]";
    private static final String SCOMO_ROOT = "./SCOMO";
    private DmService mDmService;
    private Context mContext;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        if (DmService.getInstance() == null) {
            startDmService();
            Thread.sleep(1000); // wait for DM service starting completed
        }
    }

    protected void startDmService() {
        Intent dmIntent = new Intent();
        dmIntent.setAction(Intent.ACTION_DEFAULT);
        dmIntent.setClass(mContext, DmService.class);
        mContext.startService(dmIntent);
    }

    public void testGetInstance() {
        Log.i(TAG, "test getInstance begin");
        DmScomoDcHandler handler = DmScomoDcHandler.getInstance();
        Assert.assertNotNull(handler);
    }

    public void testConfirmActivate() {
        Log.i(TAG, "test confirmActivate begin");
        try {
            DmScomoDcHandler handler = DmScomoDcHandler.getInstance();
            VdmScomoDc dc = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs().get(0);
            Assert.assertFalse(handler.confirmActivate(dc));
        } catch (Exception e) {
            Log.e(TAG, "test confirmActive fail");
        }
    }

    public void testConfirmDeactivate() {
        Log.i(TAG, "test confirmDeactivate begin");
        try {
            DmScomoDcHandler handler = DmScomoDcHandler.getInstance();
            VdmScomoDc dc = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs().get(0);
            Assert.assertFalse(handler.confirmDeactivate(dc));
        } catch (Exception e) {
            Log.e(TAG, "test confirmDeactive fail");
        }
    }

    public void testExecuteActivate() {
        Log.i(TAG, "test executeActivate begin");
        try {
            DmScomoDcHandler handler = DmScomoDcHandler.getInstance();
            VdmScomoDc dc = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs().get(0);

            Assert.assertFalse(handler.confirmDeactivate(dc));
        } catch (Exception e) {
            Log.e(TAG, "test executeActivate fail");
        }
    }

    public void testExecuteDeactivate() {
        Log.i(TAG, "test executeDeactivate begin");
        try {
            DmScomoDcHandler handler = DmScomoDcHandler.getInstance();
            VdmScomoDc dc = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs().get(0);

            Assert.assertFalse(handler.confirmDeactivate(dc));
        } catch (Exception e) {
            Log.e(TAG, "test executeDeactivate fail");
        }
    }

    public void testExcuteRemove() {
        Log.i(TAG, "test excuteRemove begin");
        try {
            DmScomoDcHandler handler = DmScomoDcHandler.getInstance();
            VdmScomoDc dc = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDcs().get(0);
            Assert.assertNull(handler.executeRemove(dc));
        } catch (Exception e) {
            Log.e(TAG, "test executeRemove fail");
        }
    }

    protected void teardown() throws Exception {
        super.tearDown();
    }
}
