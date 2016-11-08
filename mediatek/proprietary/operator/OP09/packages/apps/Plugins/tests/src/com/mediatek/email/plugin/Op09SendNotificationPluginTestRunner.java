package com.mediatek.email.plugin;

import junit.framework.TestSuite;
import android.test.InstrumentationTestRunner;

public class Op09SendNotificationPluginTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getTestSuite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(Op09SendNotificationPluginTest.class);
        return suite;
    }
}
