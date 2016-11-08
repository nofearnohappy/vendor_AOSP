package com.mediatek.dm.test.fumo;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

//import com.android.internal.telephony.Phone;

import com.mediatek.dm.DmMmiProgress;
import com.mediatek.dm.DmService;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.fumo.DmClient;
import com.mediatek.dm.fumo.DmFumoHandler;
import com.mediatek.dm.session.DmSessionStateObserver;
import com.mediatek.dm.test.DmTestHelper;

//import com.jayway.android.robotium.solo.Solo;
import com.redbend.android.RbException.VdmError;
import com.redbend.vdm.DownloadDescriptor;
import com.redbend.vdm.SessionInitiator;
import com.redbend.vdm.SessionStateObserver.SessionState;
import com.redbend.vdm.SessionStateObserver.SessionType;

import junit.framework.Assert;

public class Fumo03FlowTests extends ActivityInstrumentationTestCase2<DmClient> {

    private static final String TAG = "ClientTests";
    private static final int CURRENT = 805737;
    static final int TOTAL = 8057371;

    private static final String FOTA_PACKAGE_PATH = "/data/data/com.mediatek.dm/files/delta.zip";

    /**
     * ALPS.JB.FPB.p9-p7
     */
    static final String[] FIELD = {
            "8057371", // DD0_SIZE
            "http://218.206.176.97:7001/dlserver/get?at=f&pkgId=17066&d=bin&msisdn=15110246357&imei=IMEI:864855010012278&taskId=18249299&taskSource=1&sessionId=60837&cid=1", // DD1_OBJECT_URI
            "application/octet-stream", // DD2_TYPE
            "update.zip", // DD3_NAME
            "1.0", // DD4_VERSION
            "CMCC", // DD5_VENDOR
            "ALPS.JB.FPB.p12 back to ALPS.JB.FPB.p9", // DD6_DESCRIPTION
            "http://218.206.176.97:7001/dlserver/get?at=s&msisdn=15110246357&imei=IMEI:864855010012278&taskId=18249299&taskSource=1&sessionId=60837&cid=1", // DD7_INSTALL_NOTIFY_URI
            "http://dm.monternet.com:7001/dlserver/", // DD8_NEXT_URL
            "http://dm.monternet.com:7001/dlserver/", // DD9_INFO_URL
            "http://dm.monternet.com:7001/dlserver/", // DD10_ICON_URI
            "estimatedDownloadTimeInSecs=300;estimatedInstallTimeInSecs=300"// DD11_INSTALL_PARAM
    };

    DmClient mActivity;
    Context mContext;
    Instrumentation mInstrumentation;
    PersistentContext mPersistentContext;

    // private Solo mSolo;

    public Fumo03FlowTests() {
        super(DmClient.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = this.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mPersistentContext = PersistentContext.getInstance(mContext);
//        setActivityInitialTouchMode(false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test01NetworkConnection() throws Exception {
        Log.v(TAG, "test01NetworkConnection");
        mPersistentContext.deleteDeltaPackage();
        mInstrumentation.waitForIdleSync();

        DmTestHelper.setNetworkConnection(mContext, true);
        mInstrumentation.waitForIdleSync();
        Thread.sleep(1000);
        Log.v(TAG, "++++++++++ test01NetworkConnection set network true ++++++++++");

        Intent service = new Intent(mContext, DmService.class);
        mContext.startService(service);
        mInstrumentation.waitForIdleSync();
        Log.v(TAG, "++++++++++ test01NetworkConnection start DmService ++++++++++");

        mActivity = this.getActivity();
        Log.v(TAG, "++++++++++ test01NetworkConnection get activity ++++++++++");
        mInstrumentation.waitForIdleSync();

        if (!DmTestHelper.checkNetwork(mContext)) {
            Log.v(TAG, "++++++++++ test01NetworkConnection, wait for network connecting ++++++++++");
            Thread.sleep(40 * 1000);
        }
        Log.v(TAG, "++++++++++ test01NetworkConnection, assert ++++++++++");
        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

    }

    public void test02DmSessionComplete() throws Exception {
        mPersistentContext.setDMSessionStatus(PersistentContext.STATE_DM_NIA_START);
        mInstrumentation.waitForIdleSync();
        Log.d(TAG, "++++++++++++ setDMSessionStatus STATE_DM_NIA_START++++++++++++"
                + PersistentContext.STATE_DM_NIA_START);

        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();
        Log.d(TAG, "++++++++++++ mActivity started ++++++++++++");

        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        DmSessionStateObserver observer = new DmSessionStateObserver();
        SessionInitiator initiator = new MockFumoSessionInitiator();
        observer.notify(SessionType.DM, SessionState.COMPLETE, VdmError.OK.val, initiator);
        mInstrumentation.waitForIdleSync();
        Log.d(TAG, "++++++++++++ DM COMPLETE ++++++++++++");

        assertEquals(PersistentContext.STATE_DM_NIA_COMPLETE,
                mPersistentContext.getDMSessionStatus());
    }

    public void test03QueryNewVersion() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_NOT_DOWNLOAD);
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();
        if (!DmTestHelper.checkNetwork(mContext)) {
            Thread.sleep(30 * 1000);
        }
        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        // query new verion
        assertEquals(PersistentContext.STATE_QUERY_NEW_VERSION,
                mPersistentContext.getDLSessionStatus());

        assertEquals(IDmPersistentValues.CLIENT_PULL, DmService.getInstance().getSessionInitor());
        assertNotNull(DmService.getInstance());
        assertTrue(DmService.getInstance().isInitDmController());

        // query done, no new version
        Thread.sleep(60 * 1000);
        mInstrumentation.waitForIdleSync();
        assertEquals(PersistentContext.STATE_NOT_DOWNLOAD, mPersistentContext.getDLSessionStatus());
    }

    public void test04NewVersionDeteched() {
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        DmFumoHandler fumoHandler = new DmFumoHandler();
        DownloadDescriptor dd = new DownloadDescriptor();
        dd.field = FIELD;
        dd.size = TOTAL;
        fumoHandler.confirmDownload(dd, null);
        mInstrumentation.waitForIdleSync();

        Assert.assertEquals(PersistentContext.STATE_NEW_VERSION_DETECTED,
                mPersistentContext.getDLSessionStatus());
        Assert.assertEquals(TOTAL, mPersistentContext.getSize());
    }

    public void test05DownloadSessionStart() throws Exception {
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();
        Log.d(TAG, "++++++++++++ mActivity started ++++++++++++");
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_RESUME_DOWNLOAD);
        mInstrumentation.waitForIdleSync();
        Log.d(TAG, "++++++++++++ setDLSessionStatus STATE_RESUME_DOWNLOAD++++++++++++"
                + PersistentContext.STATE_RESUME_DOWNLOAD);

        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        DmSessionStateObserver observer = new DmSessionStateObserver();
        SessionInitiator initiator = new MockFumoSessionInitiator();
        observer.notify(SessionType.DL, SessionState.STARTED, VdmError.OK.val, initiator);
        mInstrumentation.waitForIdleSync();
        Log.d(TAG, "++++++++++++ DM COMPLETE, and wait for 1s ++++++++++++");
        Thread.sleep(1000);

        mInstrumentation.waitForIdleSync();
        Assert.assertEquals(PersistentContext.STATE_START_TO_DOWNLOAD,
                mPersistentContext.getDLSessionStatus());
    }

    public void test06DownloadAbortedBadDD() {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_DOWNLOADING);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        mPersistentContext.setDMSessionStatus(PersistentContext.STATE_DM_NIA_COMPLETE);
        DmSessionStateObserver observer = new DmSessionStateObserver();
        SessionInitiator initiator = new MockFumoSessionInitiator();
        observer.notify(SessionType.DL, SessionState.ABORTED, VdmError.BAD_DD.val, initiator);
        mInstrumentation.waitForIdleSync();

        Assert.assertEquals(PersistentContext.STATE_DM_NIA_CANCLE,
                mPersistentContext.getDMSessionStatus());
    }

    /**
     * DM crashes before user confirm download, then resume it
     *
     * @throws Exception
     */
    public void test07ResumeDownload() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_NEW_VERSION_DETECTED);
//        mPersistentContext.setIsNeedResumeDLSession(true);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        if (!DmTestHelper.checkNetwork(mContext)) {
            Thread.sleep(30 * 1000);
        }
        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        // mSolo = new Solo(mInstrumentation, mActivity);
        // mSolo.clickOnButton(mActivity.getString(R.string.download));

        // assertEquals(PersistentContext.STATE_RESUME_DOWNLOAD,
        // mPersistentContext.getDLSessionStatus());
    }

    public void test08Downloading() {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_NEW_VERSION_DETECTED);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_START_TO_DOWNLOAD);
        mInstrumentation.waitForIdleSync();
        DmMmiProgress progress = new DmMmiProgress();
        DmService.sSessionType = DmService.SESSION_TYPE_FUMO;
        progress.update(CURRENT, TOTAL);
        mInstrumentation.waitForIdleSync();

        assertEquals(PersistentContext.STATE_DOWNLOADING, mPersistentContext.getDLSessionStatus());
        assertEquals(CURRENT, mPersistentContext.getDownloadedSize());
    }

    // TODO
    public void test09PauseDownload() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_DOWNLOADING);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        if (!DmTestHelper.checkNetwork(mContext)) {
            Thread.sleep(30 * 1000);
        }
        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        // mSolo = new Solo(mInstrumentation, mActivity);
        // mSolo.clickOnButton(mActivity.getString(R.string.suspend));

        // assertEquals(PersistentContext.STATE_PAUSE_DOWNLOAD,
        // mPersistentContext.getDLSessionStatus());
    }

    /**
     * DM paused, then resume it
     *
     * @throws Exception
     */
    // TODO
    public void test10ResumeDownload() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_PAUSE_DOWNLOAD);
//        mPersistentContext.setIsNeedResumeDLSession(false);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        if (!DmTestHelper.checkNetwork(mContext)) {
            Thread.sleep(30 * 1000);
        }
        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        assertEquals(PersistentContext.STATE_RESUME_DOWNLOAD,
                mPersistentContext.getDLSessionStatus());
    }

    public void test11DownloadComplete() {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_DOWNLOADING);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        DmFumoHandler fumoHandler = new DmFumoHandler();
        fumoHandler.confirmUpdate(null);
        mInstrumentation.waitForIdleSync();

        Assert.assertEquals(PersistentContext.STATE_DL_PKG_COMPLETE,
                mPersistentContext.getDLSessionStatus());
    }

    public void test12CancelDownload() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_DOWNLOADING);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        if (!DmTestHelper.checkNetwork(mContext)) {
            Thread.sleep(30 * 1000);
        }
        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));

        // mSolo = new Solo(mInstrumentation, mActivity);
        // mSolo.clickOnButton(mActivity.getString(R.string.cancel));
        // mInstrumentation.waitForIdleSync();
        // assertEquals(PersistentContext.STATE_PAUSE_DOWNLOAD,
        // mPersistentContext.getDLSessionStatus());
        //
        // mSolo.clickOnButton(mActivity.getString(R.string.ok));
        // mInstrumentation.waitForIdleSync();

        // assertEquals(PersistentContext.STATE_CANCEL_DOWNLOAD,
        // mPersistentContext.getDLSessionStatus());
    }

    /*public void test13Report() throws Exception {
        ActivityMonitor monitor = mInstrumentation.addMonitor("com.mediatek.dm.fumo.DmReport",
                null, false);
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_UPDATE_RUNNING);
        Intent intent = new Intent();
        intent.setAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        intent.putExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, Phone.SIM_INDICATOR_NORMAL);
        mContext.sendBroadcast(intent);

        mInstrumentation.waitForIdleSync();
        // Wait for check update result in another thread.
        Thread.sleep(200);
        File fotaFile = new File(FOTA_PACKAGE_PATH);
        Assert.assertFalse(fotaFile.exists());
        mInstrumentation.waitForIdleSync();
        Assert.assertTrue(monitor.getHits() > 0);
    }*/

    /**
     * DL Abort for Socket error, but data connection resume within 5min, so Resume DL again
     *
     * @throws Exception
     */
    public void test14DownloadAbortedSocketError1() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_DOWNLOADING);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();

        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));
        DmSessionStateObserver observer = new DmSessionStateObserver();
        SessionInitiator initiator = new MockFumoSessionInitiator();
        observer.notify(SessionType.DL, SessionState.ABORTED, VdmError.COMMS_SOCKET_ERROR.val,
                initiator);
        mInstrumentation.waitForIdleSync();

        Assert.assertEquals(PersistentContext.STATE_DOWNLOADING,
                mPersistentContext.getDLSessionStatus());
    }

    /**
     * DL Abort for Socket error, and data connection not resume within 5min, Cancel the session
     *
     * @throws Exception
     */
    public void test15DownloadAbortedSocketError2() throws Exception {
        mPersistentContext.setDLSessionStatus(PersistentContext.STATE_DOWNLOADING);
        mInstrumentation.waitForIdleSync();
        mActivity = this.getActivity();
        mInstrumentation.waitForIdleSync();
        DmTestHelper.setNetworkConnection(mContext, false);

        Assert.assertTrue("network not connected ", DmTestHelper.checkNetwork(mContext));
        mPersistentContext.setDMSessionStatus(PersistentContext.STATE_DM_NIA_COMPLETE);
        DmSessionStateObserver observer = new DmSessionStateObserver();
        SessionInitiator initiator = new MockFumoSessionInitiator();
        observer.notify(SessionType.DL, SessionState.ABORTED, VdmError.COMMS_SOCKET_ERROR.val,
                initiator);
        mInstrumentation.waitForIdleSync();
        // will wait 5 min for socket timeout
        Thread.sleep(5 * 60 * 1000);

        DmTestHelper.setNetworkConnection(mContext, true);
        Assert.assertEquals(PersistentContext.STATE_DM_NIA_CANCLE,
                mPersistentContext.getDMSessionStatus());

        mPersistentContext.deleteDeltaPackage();
        mInstrumentation.waitForIdleSync();
    }

    public class MockFumoSessionInitiator implements SessionInitiator {

        @Override
        public String getId() {
            return "VDM_FUMO";
        }

    }
}
