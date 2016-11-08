package com.mediatek.mms.op09;

import android.app.Instrumentation;
import android.content.Context;
import android.provider.Telephony.SIMInfo;
import android.test.InstrumentationTestCase;
import android.test.InstrumentationTestRunner;

import junit.framework.TestSuite;

public class Op09MmsPluginTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getTestSuite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(DisplayClassZeroMessageTest.class);
        suite.addTestSuite(MissedSmsReceiverTest.class);
        suite.addTestSuite(MmsCancelDownloadTest.class);
        suite.addTestSuite(ResendSmsTest.class);
        suite.addTestSuite(MmsFailedNotifyTest.class);
        suite.addTestSuite(SmsReceiverTest.class);
        suite.addTestSuite(MmsComposeTest.class);
        suite.addTestSuite(MmsMessageListItemTest.class);
        suite.addTestSuite(MmsUtilsTest.class);
        suite.addTestSuite(MmsMultiDeleteTest.class);
        suite.addTestSuite(MmsSmsMessageSenderTest.class);
        suite.addTestSuite(MmsConfigTest.class);
        suite.addTestSuite(MmsConversationListItemTest.class);
        suite.addTestSuite(MmsDialogModeTest.class);
        suite.addTestSuite(StringReplacementTest.class);
        suite.addTestSuite(ManageSimMessageTest.class);
        suite.addTestSuite(MmsAdvanceSearchTest.class);
        suite.addTestSuite(UnreadMessageNumberTest.class);
        suite.addTestSuite(MmsPreferenceTest.class);
        suite.addTestSuite(StorageLowTest.class);
        return suite;
    }

    public static class BasicCase extends InstrumentationTestCase {
        protected static final long NEW_THREAD_ID = 0;
        protected static final String TEST_ADDRESS = "15313706371";
        protected static final String SMS_CONTENT = "Test message.";
        protected static final long SEND_DURATION_LIMIT = 20 * 1000;
        protected static final long DELAY_TIME = 1000;
        protected Instrumentation mInstrumentation;
        protected Context mContext;
        protected int mSimIdCdma;
        protected int mSimIdGsm;

        @Override
        protected void setUp() throws Exception {
            super.setUp();

            mInstrumentation = getInstrumentation();
            mContext = mInstrumentation.getContext();
            mSimIdCdma = (int) SIMInfo.getIdBySlot(mContext, 0);
            mSimIdGsm = (int) SIMInfo.getIdBySlot(mContext, 1);
        }

        protected static void delay(long time) throws InterruptedException {
            Thread.sleep(time);
        }

        protected boolean checkSims() {
            return SIMInfo.getInsertedSIMCount(mContext) == 2;
        }

    }

}

