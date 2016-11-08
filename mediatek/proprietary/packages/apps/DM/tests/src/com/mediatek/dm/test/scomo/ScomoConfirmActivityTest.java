package com.mediatek.dm.test.scomo;

import com.mediatek.dm.scomo.DmScomoConfirmActivity;
import com.mediatek.dm.scomo.DmScomoState;

import android.test.ActivityInstrumentationTestCase2;
import android.content.Intent;
import android.util.Log;

public class ScomoConfirmActivityTest extends
        ActivityInstrumentationTestCase2<DmScomoConfirmActivity> {
    private static final String TAG = "[ScomoConfirmActivityTest]";
    private static final String EXTRA_ACTION = "action";
    private static final int EXTRA_TYPE_INSTALL_OK = 26;
    private static final int EXTRA_TYPE_INSTALL_FAILED = 25;
    private static final int EXTRA_TYPE_DWONLOAD_FAILED = 5;
    private static final int EXTRA_TEYP_GENERIC_ERROR = 27;

    public ScomoConfirmActivityTest() {
        super(DmScomoConfirmActivity.class);
        // TODO Auto-generated constructor stub
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testOnConfrimInstall() {
        Log.d(TAG, "test onConfirmInstall begin");
        try {
            Intent startIntent = new Intent();
            startIntent.putExtra(EXTRA_ACTION, DmScomoState.CONFIRM_INSTALL);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(startIntent);
            this.getActivity();
            Thread.sleep(1000);
            this.getActivity().finish();
        } catch (Exception e) {
            Log.d(TAG, "test onGenericError fail");
        }
    }

    public void testOnConfirmDownload() {
        Log.d(TAG, "test onConfirmDownload begin");
        try {
            Intent startIntent = new Intent();

            startIntent.putExtra(EXTRA_ACTION, DmScomoState.CONFIRM_DOWNLOAD);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(startIntent);
            this.getActivity();
            Thread.sleep(1000);
            this.getActivity().finish();
        } catch (Exception e) {
            Log.d(TAG, "test onGenericError fail");
        }
    }

    public void testOnGenericError() {
        Log.d(TAG, "test onGenericError begin");
        try {
            Intent startIntent = new Intent();

            startIntent.putExtra(EXTRA_ACTION, EXTRA_TEYP_GENERIC_ERROR);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(startIntent);
            this.getActivity();
            Thread.sleep(1000);
            this.getActivity().finish();
        } catch (Exception e) {
            Log.d(TAG, "test onGenericError fail");
        }
    }

    public void testOnDownloadFailed() {
        Log.d(TAG, "test onDownLoadFail begin");
        try {
            Intent startIntent = new Intent();

            startIntent.putExtra(EXTRA_ACTION, DmScomoState.DOWNLOAD_FAILED);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(startIntent);

            this.getActivity();
            Thread.sleep(1000);
            this.getActivity().finish();
        } catch (Exception e) {
            Log.d(TAG, "test onDownloadFailed fail");
        }
    }

    public void testOnInstallOk() {
        Log.d(TAG, "test onInstallOk begin");
        try {
            Intent startIntent = new Intent();

            startIntent.putExtra(EXTRA_ACTION, EXTRA_TYPE_DWONLOAD_FAILED);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(startIntent);

            this.getActivity();
            Thread.sleep(1000);
            this.getActivity().finish();
        } catch (Exception e) {
            Log.d(TAG, "test onInstallOk fail");
        }
    }

    public void testOnInstallFailed() {
        Log.d(TAG, "test onInstallFailed begin");

        try {
            Intent startIntent = new Intent();

            startIntent.putExtra(EXTRA_ACTION, DmScomoState.INSTALL_FAILED);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setActivityIntent(startIntent);

            this.getActivity();
            Thread.sleep(1000);
            this.getActivity().finish();
        } catch (Exception e) {
            Log.d(TAG, "test onInstallFaild fail");
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
