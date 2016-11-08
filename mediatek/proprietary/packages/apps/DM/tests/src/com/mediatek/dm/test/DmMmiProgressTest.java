/**
 *
 */
package com.mediatek.dm.test;

import android.content.Intent;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmMmiProgress;
import com.mediatek.dm.DmService;


/**
 * @author MTK80987
 *
 */
public class DmMmiProgressTest extends AndroidTestCase {
    private static final String TAG = "[MmiProgressTest]";

    protected void setUp() throws Exception {
        super.setUp();
        if (DmService.getInstance() == null)
        {
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

    public void testUpdate() {
        Log.d(TAG, "test update begin");
        if (DmService.getInstance() != null) {
            DmService.sSessionType = DmService.SESSION_TYPE_SCOMO;
            DmMmiProgress progress = new DmMmiProgress();
            progress.update(10, 100);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
