/**
 *
 */
package com.mediatek.dm.test.scomo;

import com.mediatek.dm.DmService;
import com.mediatek.dm.scomo.DmScomoNotification;
import com.mediatek.dm.scomo.DmScomoState;
import com.redbend.vdm.SessionInitiator;

import android.content.Intent;
import android.test.AndroidTestCase;
import android.util.Log;


/**
 * @author MTK80987
 *
 */
public class DmScomoNotificationTest extends AndroidTestCase {
    private static final String TAG = "[DmScomoNotificationTest]";
    private DmScomoState mState;
    protected void setUp() throws Exception {
        super.setUp();
        if (DmService.getInstance() == null) {
            startDmService();
            Thread.sleep(1000);
        }
        mState = DmScomoState.getInstance(this.getContext());
    }

    protected void startDmService() {
        Intent dmIntent = new Intent();
        dmIntent.setAction(Intent.ACTION_DEFAULT);
        dmIntent.setClass(mContext, DmService.class);
        mContext.startService(dmIntent);
    }

    public void testOnUpdate() throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InterruptedException {
        Log.d(TAG, "test onUpdate begin");

        DmScomoNotification notify = new DmScomoNotification(this.getContext());

        setDmScomoState(DmScomoState.DOWNLOADING);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.DOWNLOADING_STARTED);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.PAUSED);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.CONFIRM_UPDATE);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.DOWNLOAD_FAILED);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.INSTALL_FAILED);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.INSTALL_OK);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.CONFIRM_DOWNLOAD);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.CONFIRM_INSTALL);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.IDLE);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.ABORTED);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.GENERIC_ERROR);
        notify.onScomoUpdated();
        Thread.sleep(1000);

        setDmScomoState(DmScomoState.INSTALLING);
        notify.onScomoUpdated();
        Thread.sleep(1000);
    }

    private void setDmScomoState(int state) throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        mState.mState = state;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public class MockScomoSessionInitiator implements SessionInitiator {

        @Override
        public String getId() {
            return "VDM_SCOMO";
        }
    }
}
