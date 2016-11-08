package com.mediatek.selfregister.test;

import android.test.InstrumentationTestRunner;

import junit.framework.TestSuite;

/**
 * TestRunner for SelfRegister.
 */
public class TestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(RegisterServiceTest.class);
        return suite;
    }

}
