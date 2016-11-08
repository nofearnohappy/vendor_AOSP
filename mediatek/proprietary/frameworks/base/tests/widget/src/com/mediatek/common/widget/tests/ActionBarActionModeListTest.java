package com.mediatek.common.widget.tests;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.test.ActivityInstrumentationTestCase2;

@SuppressLint("NewApi")
@TargetApi(15)
public class ActionBarActionModeListTest extends ActivityInstrumentationTestCase2<ActionBarActionModeListActivity> {
    private ActionBarActionModeListActivity mActivity;

    private ActionBarUtils mActionBarUtils;

    public ActionBarActionModeListTest() {
        super("com.mediatek.common.widget.tests", ActionBarActionModeListActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = (ActionBarActionModeListActivity) getActivity();
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
