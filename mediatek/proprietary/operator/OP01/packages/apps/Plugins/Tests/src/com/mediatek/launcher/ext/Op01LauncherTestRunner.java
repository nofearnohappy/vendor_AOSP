package com.mediatek.launcher.ext;

import android.test.InstrumentationTestRunner;

import junit.framework.TestSuite;

/**
 * Op01Launcher test runner.
 */
public class Op01LauncherTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getTestSuite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(Op01LauncherTest.class);
        return suite;
    }

}
