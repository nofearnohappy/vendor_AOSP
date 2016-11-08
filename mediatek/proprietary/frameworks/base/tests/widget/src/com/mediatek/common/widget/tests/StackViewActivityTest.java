package com.mediatek.common.widget.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.StackView;


public class StackViewActivityTest extends ActivityInstrumentationTestCase2<StackViewActivity> {

    StackViewActivity mActivity;
    StackView mStackView;

    public StackViewActivityTest() {
        super("com.mediatek.common.widget.tests", StackViewActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mStackView = mActivity.getStackView();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();


    }

    public void testPreconditions() {
        assertTrue(mActivity != null);
        assertTrue(mStackView != null);
    }


}