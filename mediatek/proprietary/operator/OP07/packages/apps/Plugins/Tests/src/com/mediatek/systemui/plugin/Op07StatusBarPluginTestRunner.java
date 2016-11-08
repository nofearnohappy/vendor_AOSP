package com.mediatek.systemui.plugin;

import junit.framework.TestSuite;
import android.test.InstrumentationTestRunner;


public class Op07StatusBarPluginTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getTestSuite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(Op07StatusBarPluginTest.class);
        return suite;
    }

}
