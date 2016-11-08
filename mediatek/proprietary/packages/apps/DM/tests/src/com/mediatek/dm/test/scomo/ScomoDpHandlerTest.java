package com.mediatek.dm.test.scomo;

import android.test.AndroidTestCase;
import android.util.Log;
import android.content.Context;
import android.content.Intent;

import com.mediatek.dm.DmService;
import com.mediatek.dm.scomo.DmScomoDpHandler;
import com.mediatek.dm.scomo.DmScomoHandler;

import com.redbend.vdm.scomo.VdmScomo;
import com.redbend.vdm.scomo.VdmScomoDp;
import com.redbend.vdm.scomo.ScomoOperationResult;


import junit.framework.Assert;

public class ScomoDpHandlerTest extends AndroidTestCase {
    private static final String TAG = "[ScomoDpHandlerTest]";
    private static final String PACKAGE_PATH = "/data/data/com.mediatek.dm/files/scomo.zip";
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

    public void testConfirmDownload() {
        Log.d(TAG, "test confirmDownload begin");
        try {
            VdmScomoDp dp = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDps().get(0);
            Assert.assertNotNull(dp);
        } catch (Exception e) {
            Log.d(TAG, "test confirmDwonload fail");
        }
    }

    public void testConfirmInstall() {
        Log.d(TAG, "test confirmInstall begin");
        try {
            VdmScomoDp dp = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDps().get(3);
            DmScomoDpHandler dpHandler = new DmScomoDpHandler();
            DmScomoDpHandler.getInstance().confirmInstall(dp);
        } catch (Exception e) {
            Log.e(TAG, "test confirmInstall fail");
        }
    }

    public void testExecuteInstall() {
        Log.i(TAG, "test executeInstall begin");
        try {
            VdmScomoDp dp = VdmScomo.getInstance(SCOMO_ROOT, DmScomoHandler.getInstance()).getDps().get(0);
            ScomoOperationResult result = DmScomoDpHandler.getInstance().executeInstall(dp, PACKAGE_PATH, true);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            Log.e(TAG, "test executeInsatll fail");
        }
    }

    protected void teardown() throws Exception {
        super.tearDown();
    }
}
