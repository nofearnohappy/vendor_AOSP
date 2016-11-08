package com.mediatek.launcher.ext;

import android.content.Context;
import android.test.InstrumentationTestCase;

/**
 * Op01Launcher TestCase.
 */
public class Op01LauncherTest extends InstrumentationTestCase {

    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = this.getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
