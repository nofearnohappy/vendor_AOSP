package com.mediatek.datatransfer;


import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;


public class Test3MainActivityTest extends ActivityInstrumentationTestCase2<BootActivity> {
    private static final String TAG = "MainActivitytest";
    private Solo mSolo = null;
    BootActivity activity = null;
    /**
     * Creates a new <code>BackupRestoreTest</code> instance.
     *
     */
    public Test3MainActivityTest() {
        super(BootActivity.class);
    }

    /**
     * Describe <code>setUp</code> method here.
     *
     * @exception Exception if an error occurs
     */
    public final void setUp() throws Exception {
        super.setUp();
        mSolo = new Solo(getInstrumentation(), getActivity());
        activity = getActivity();
        Log.d(TAG, "setUp");
    }

    /**
     * Describe <code>tearDown</code> method here.
     *
     * @exception Exception if an error occurs
     */
    public final void tearDown() throws Exception {
        //
        try {
            mSolo.finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (activity != null) {
             Log.d(TAG, "tearDown : activity = " + activity);
            activity.finish();
            activity = null;
        }
        super.tearDown();
        Log.d(TAG, "tearDown");
        sleep(5000);
    }

    public void testMount() {
        sleep(6000);
        Intent mountIntent = new Intent("com.mediatek.autotest.unmount");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "MainActivity unmount SDCard");
        sleep(6000);
        mountIntent = new Intent("com.mediatek.autotest.mount");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "MainActivity mount SDCard");
        sleep(6000);
        mountIntent = new Intent("com.mediatek.SD_SWAP");
        getActivity().sendBroadcast(mountIntent);
        Log.d(TAG, "MainActivity SD_SWAP");
        sleep(6000);
        mSolo.finishOpenedActivities();
    }

    /**
     * Describe <code>sleep</code> method here.
     *
     * @param time a <code>int</code> value
     */
    public void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

}
