package com.mediatek.dm.test.scomo;

import android.content.Intent;
import android.test.AndroidTestCase;
import android.util.Log;
import android.content.Context;

import com.mediatek.dm.DmService;
import com.mediatek.dm.scomo.DmScomoHandler;
import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDp;


import junit.framework.Assert;

public class ScomoHandlerTest extends AndroidTestCase {
    private static final String TAG = "[ScomoHandlerTest]";
    private static final String SCOMO_ROOT = "./SCOMO";
    private Context mContext;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        if (DmService.getInstance() == null) {
            startDmService();
            Thread.sleep(1000);
        }
    }

    protected void startDmService() {
        Intent dmIntent = new Intent();
        dmIntent.setAction(Intent.ACTION_DEFAULT);
        dmIntent.setClass(mContext, DmService.class);
        mContext.startService(dmIntent);
    }

    public void testGetInstance() {
        Log.d(TAG, "test getInstance begin");
        DmScomoHandler handler = DmScomoHandler.getInstance();
        Assert.assertNotNull(handler);
    }

    public void testNewDpAdded() {
        Log.d(TAG, "test newDpAdded begin");
        try {
            DmScomoHandler handler = DmScomoHandler.getInstance();
            VdmScomoDp dp = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDps().get(0);
            //handler.newDpAdded(dp.getName());
        } catch (Exception e) {
            Log.d(TAG, "test newdpAdded fail");
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
