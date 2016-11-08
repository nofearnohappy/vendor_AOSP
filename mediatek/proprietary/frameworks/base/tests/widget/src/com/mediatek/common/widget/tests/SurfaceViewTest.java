package com.mediatek.common.widget.tests;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.test.ActivityInstrumentationTestCase2;


@SuppressLint("NewApi")
@TargetApi(15)
public class SurfaceViewTest extends ActivityInstrumentationTestCase2<SurfaceViewActivity> {
    private SurfaceViewActivity mActivity;

    private ActionBarUtils mActionBarUtils;

    public SurfaceViewTest() {
        super("com.mediatek.common.widget.tests", SurfaceViewActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = (SurfaceViewActivity) getActivity();
        mActionBarUtils = new ActionBarUtils(mActivity, getInstrumentation());
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
        System.out.println("tearDown");
    }

    public void testPreConditions() {
        assertTrue(mActivity != null);
    }

    public void testActionProvider() {
        ActionBar actionBar = mActivity.getActionBar();

        assertTrue("", true);
    }
}
