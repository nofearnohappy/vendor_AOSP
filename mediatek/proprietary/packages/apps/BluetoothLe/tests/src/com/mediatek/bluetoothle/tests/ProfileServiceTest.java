
package com.mediatek.bluetoothle.tests;

import android.bluetooth.BluetoothAdapter;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mediatek.bluetooth.BleManager;
import com.mediatek.bluetooth.BleProfileServiceManager;

/**
 * Test Case For ProfileService
 */
public class ProfileServiceTest extends InstrumentationTestCase {
    private static final String TAG = "ProfileServiceTest";
    private static final long WIAT_TIME = 4000;
    private BleManager mBleManager;
    private BleProfileServiceManager mProfileServiceManager;
    private final BleProfileServiceManager.ProfileServiceManagerListener mListener =
            new BleProfileServiceManager.ProfileServiceManagerListener() {
                @Override
                public void onServiceConnected(final BleProfileServiceManager proxy) {
                    mProfileServiceManager = proxy;
                }

                @Override
                public void onServiceDisconnected(final BleProfileServiceManager proxy) {
                    if (proxy == mProfileServiceManager) {
                        mProfileServiceManager = null;
                    }
                }
            };

    @Override
    protected void setUp() throws Exception {
        mBleManager = BleManager.getDefaultBleProfileManager();
        mBleManager.getProfileServiceManager(getInstrumentation().getContext(), mListener);
    }

    @Override
    protected void tearDown() throws Exception {
        if (null != mProfileServiceManager) {
            mBleManager.closeProfileServiceManager(mProfileServiceManager);
        }
        mBleManager = null;
        mProfileServiceManager = null;
    }

    /**
     * Test get current supported id
     */
    public void testCase1() {
        checkProfileServiceManager();
        mProfileServiceManager.getCurSupportedServerProfiles();
    }

    /**
     * Test get profile status
     */
    public void testCase2() {
        checkProfileServiceManager();

        final int[] ids = mProfileServiceManager.getCurSupportedServerProfiles();
        for (final int id : ids) {
            mProfileServiceManager.getProfileServerState(id);
        }
    }

    /**
     * Test turn off BT and query current supported profile
     */
    public void testCase3() {
        checkProfileServiceManager();

        BluetoothAdapter.getDefaultAdapter().disable();
        waitForBT();

        final int[] ids = mProfileServiceManager.getCurSupportedServerProfiles();
        for (final int id : ids) {
            Log.v(TAG,
                    "testCase3: profile=" + id + " state="
                            + mProfileServiceManager.getProfileServerState(id));
            assertTrue(BleProfileServiceManager.STATE_SERVER_IDLE == mProfileServiceManager
                    .getProfileServerState(id));
        }
    }

    /**
     * Test turn on BT and query current supported profile
     */
    public void testCase4() {
        checkProfileServiceManager();

        mProfileServiceManager.setBackgroundMode(true);
        BluetoothAdapter.getDefaultAdapter().enable();
        waitForBT();

        final int[] ids = mProfileServiceManager.getCurSupportedServerProfiles();
        for (final int id : ids) {
            Log.v(TAG,
                    "testCase4: profile=" + id + " state="
                            + mProfileServiceManager.getProfileServerState(id));
            waitForServiceReady();
            assertTrue(BleProfileServiceManager.STATE_SERVER_REGISTERED == mProfileServiceManager
                    .getProfileServerState(id));
        }
        mProfileServiceManager.setBackgroundMode(false);
    }

    /**
     * Test setBackgroundMode/getBackgroundMode
     */
    public void testCase5() {
        checkProfileServiceManager();

        assertTrue(mProfileServiceManager.setBackgroundMode(true));
        assertTrue(mProfileServiceManager.getBackgroundMode()
                == BleProfileServiceManager.STATUS_ENABLED);

        assertTrue(mProfileServiceManager.setBackgroundMode(false));
        assertTrue(mProfileServiceManager.getBackgroundMode()
                == BleProfileServiceManager.STATUS_DISABLED);
    }

    /**
     * Test launch Services in non-background mode
     */
    public void testCase6() {
        checkProfileServiceManager();

        mProfileServiceManager.setBackgroundMode(false);

        BluetoothAdapter.getDefaultAdapter().disable();
        waitForBT();
        BluetoothAdapter.getDefaultAdapter().enable();
        waitForBT();

        assertTrue(mProfileServiceManager.launchServices());
        waitForBT();

        // check the status
        final int[] ids = mProfileServiceManager.getCurSupportedServerProfiles();
        for (final int id : ids) {
            Log.v(TAG,
                    "testCase6: profile=" + id + " state="
                            + mProfileServiceManager.getProfileServerState(id));
            waitForServiceReady();
            assertTrue(BleProfileServiceManager.STATE_SERVER_REGISTERED
                == mProfileServiceManager.getProfileServerState(id));
        }
    }

    /**
     * Test shutdown Services in non-background mode
     */
    public void testCase7() {
        checkProfileServiceManager();

        assertTrue(mProfileServiceManager.shutdownServices());
        waitForBT();

        // check the status
        final int[] ids = mProfileServiceManager.getCurSupportedServerProfiles();
        for (final int id : ids) {
            Log.v(TAG,
                    "testCase7: profile=" + id + " state="
                            + mProfileServiceManager.getProfileServerState(id));
            waitForServiceReady();
            assertTrue(BleProfileServiceManager.STATE_SERVER_IDLE
                == mProfileServiceManager.getProfileServerState(id));
        }
    }

    private void waitForProfileServiceManagerReady() {
        waitFor(WIAT_TIME);
    }

    private void waitForBT() {
        waitFor(WIAT_TIME * 2);
    }

    private void waitForServiceReady() {
        waitFor(WIAT_TIME / 2);
    }

    private void waitFor(final long time) {
        try {
            Thread.sleep(time);
        } catch (final InterruptedException e) {
            Log.e(TAG, "" + e);
        }
    }

    private void checkProfileServiceManager() {
        if (null == mProfileServiceManager) {
            waitForProfileServiceManagerReady();
        }

        assertNotNull(mProfileServiceManager);
    }
}
