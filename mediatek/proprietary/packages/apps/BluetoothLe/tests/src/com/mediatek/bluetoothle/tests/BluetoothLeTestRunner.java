package com.mediatek.bluetoothle.tests;

import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

import junit.framework.TestSuite;

/**
 * Test Runner for BLEManager's Test
 */
public class BluetoothLeTestRunner  extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {

        final TestSuite suite = new InstrumentationTestSuite(this);
        //suite.addTestSuite(BleJpeTest.class);

        // Profile Service Tests
        //suite.addTestSuite(ProfileServiceTest.class);

        // Profile Service Object Pool Tests
        //suite.addTestSuite(BleProfileServiceObjectPoolTest.class);

        // BleProfileManagerState Tests (Mocked)
        //suite.addTestSuite(BleProfileManagerStateTest.class);

        // BleProfileManagerService Tests (Mocked)
        //suite.addTestSuite(BleProfileManagerService.class);

        ///Framework Client Tests (Mocked)
        //suite.addTestSuite(BluetoothLeTest.class);

        // TIP State Machine Tests
        //suite.addTestSuite(BluetoothLeTipTest.class);

        return suite;
    }

}
