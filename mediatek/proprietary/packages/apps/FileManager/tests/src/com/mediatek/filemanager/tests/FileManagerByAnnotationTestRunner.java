package com.mediatek.filemanager.tests;

import android.test.InstrumentationTestRunner;

import junit.framework.TestSuite;

public class FileManagerByAnnotationTestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(FileManagerTestByAnnotation.class);
        suite.addTestSuite(FileManagerCMCCTest.class);
        return suite;
    }
}
