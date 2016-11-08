
package com.mediatek.bluetoothle.tests;

import android.os.Handler;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mediatek.bluetoothle.BleProfileServerObjectPool;
import com.mediatek.bluetoothle.bleservice.BleProfileManagerService;
import com.mediatek.bluetoothle.bleservice.BleProfileManagerState;
import com.mediatek.bluetoothle.bleservice.TestUtil;

import org.mockito.Mockito;

/**
 * Test Case For BleProfileManagerState
 */
public class BleProfileManagerStateTest extends InstrumentationTestCase {
    private static final String TAG = "BleProfileManagerStateTest";
    private BleProfileManagerService mManager;
    private BleProfileServerObjectPool mPool;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected void setUp() throws Exception {
        // Workaround1: Space for Mock classes generation
        System.setProperty("dexmaker.dexcache", this.getInstrumentation()
                .getTargetContext().getCacheDir().getPath());
        // Workaround2: For sharedUserId
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // Prepare Mock Object
        mManager = Mockito.mock(BleProfileManagerService.class);
        mPool = Mockito.mock(BleProfileServerObjectPool.class);
        // Check Pre-Condition
        assertNotNull(mManager);
        assertNotNull(mPool);
    }

    private void waitFor(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Log.e(TAG, "" + e) ;
        }
    }

    /**
     * Test the normal flow for enable and disable services
     */
    public void test01EnableDisableNormalFlow() {
        // mock protected method is broken @L, and we skip it
        if(!isSupportProtectedMock(mManager)) return;
        // prepare mock object
        Mockito.when(mManager.getApplicationContext())
                .thenReturn(getInstrumentation().getTargetContext());
        TestUtil.mockitoStubStartProfileServices(mManager);
        TestUtil.mockitoStubStopProfileServices(mManager);
        TestUtil.mockitoStubShutdownProfileServices(mManager);
        Mockito.when(mPool.isAllObjectReleased()).thenReturn(true);
        // start simulation
        final BleProfileManagerState sm = TestUtil.makeBleProfileManagerState(mManager, mPool);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        waitFor(100);
        // verify result
        TestUtil.mockitoVerifyStartProfileServices(mManager, 1);
        sm.sendMessage(BleProfileManagerState.PROFILES_STARTED);
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        waitFor(100);
        TestUtil.mockitoVerifyStopProfileServices(mManager, 1);
        sm.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
        // wait for shutdown
        waitFor(6000);
        Mockito.verify(mPool, Mockito.times(1)).isAllObjectReleased();
        TestUtil.mockitoVerifyShutdownProfileServices(mManager, 1);
        // all messages must be handled
        final Handler hdlr = sm.getHandler();
        assertTrue(!hdlr.hasMessages(BleProfileManagerState.START_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STARTED)
                && !hdlr.hasMessages(BleProfileManagerState.STOP_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STOPPED)
                && !hdlr.hasMessages(BleProfileManagerState.SHUTDOWN));
    }

    /**
     * Test enable/disable ble services twice quickly. It should be pending in
     * the queue, and will be handled one by one. Only has one shutdown message
     * at the same time.
     */
    public void test02EnableDisableMultipleFlow() {
        // mock protected method is broken @L, and we skip it
        if(!isSupportProtectedMock(mManager)) return;
        // prepare mock object
        TestUtil.mockitoStubStartProfileServices(mManager);
        TestUtil.mockitoStubStopProfileServices(mManager);
        TestUtil.mockitoStubShutdownProfileServices(mManager);
        Mockito.when(mPool.isAllObjectReleased()).thenReturn(true);
        // start simulation
        final BleProfileManagerState sm = TestUtil.makeBleProfileManagerState(mManager, mPool);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        sm.sendMessage(BleProfileManagerState.PROFILES_STARTED);
        sm.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
        sm.sendMessage(BleProfileManagerState.PROFILES_STARTED);
        sm.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
        // wait for shutdown
        waitFor(6000);
        // verify result
        TestUtil.mockitoVerifyStopProfileServices(mManager, 2);
        TestUtil.mockitoVerifyStartProfileServices(mManager, 2);
        Mockito.verify(mPool, Mockito.times(1)).isAllObjectReleased();
        TestUtil.mockitoVerifyShutdownProfileServices(mManager, 1);
        // all messages must be handled
        final Handler hdlr = sm.getHandler();
        assertTrue(!hdlr.hasMessages(BleProfileManagerState.START_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STARTED)
                && !hdlr.hasMessages(BleProfileManagerState.STOP_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STOPPED)
                && !hdlr.hasMessages(BleProfileManagerState.SHUTDOWN));
    }

    /**
     * Test if the state machine will drop message or not
     */
    public void test03EnableDisableAbortFlow() {
        // mock protected method is broken @L, and we skip it
        if(!isSupportProtectedMock(mManager)) return;
        // prepare mock object
        TestUtil.mockitoStubStartProfileServices(mManager);
        TestUtil.mockitoStubStopProfileServices(mManager);
        TestUtil.mockitoStubShutdownProfileServices(mManager);
        Mockito.when(mPool.isAllObjectReleased()).thenReturn(true);
        // start simulation
        final BleProfileManagerState sm = TestUtil.makeBleProfileManagerState(mManager, mPool);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        sm.sendMessage(BleProfileManagerState.PROFILES_STARTED);
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        waitFor(100);
        sm.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
        // wait for shutdown
        waitFor(6000);
        // verify result
        TestUtil.mockitoVerifyStopProfileServices(mManager, 1);
        TestUtil.mockitoVerifyStartProfileServices(mManager, 1);
        Mockito.verify(mPool, Mockito.times(1)).isAllObjectReleased();
        TestUtil.mockitoVerifyShutdownProfileServices(mManager, 1);
        // all messages must be handled
        final Handler hdlr = sm.getHandler();
        assertTrue(!hdlr.hasMessages(BleProfileManagerState.START_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STARTED)
                && !hdlr.hasMessages(BleProfileManagerState.STOP_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STOPPED)
                && !hdlr.hasMessages(BleProfileManagerState.SHUTDOWN));
    }

    /**
     * Test if some Ble services are timeout when enabling.
     */
    public void test04EnableTimeoutFlow() {
        // mock protected method is broken @L, and we skip it
        if(!isSupportProtectedMock(mManager)) return;
        // prepare mock object
        TestUtil.mockitoStubStartProfileServices(mManager);
        TestUtil.mockitoStubStopProfileServices(mManager);
        TestUtil.mockitoStubShutdownProfileServices(mManager);
        Mockito.when(mPool.isAllObjectReleased()).thenReturn(true);
        // start simulation
        final BleProfileManagerState sm = TestUtil.makeBleProfileManagerState(mManager, mPool);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        waitFor(100);
        // verify result
        TestUtil.mockitoVerifyStartProfileServices(mManager, 1);
        // / wait for timeout
        waitFor(6000);
        // / message PROFILES_STARTED should be dropped
        sm.sendMessage(BleProfileManagerState.PROFILES_STARTED);
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        waitFor(100);
        TestUtil.mockitoVerifyStopProfileServices(mManager, 1);
        sm.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
        // wait for shutdown
        waitFor(6000);
        Mockito.verify(mPool, Mockito.times(1)).isAllObjectReleased();
        TestUtil.mockitoVerifyShutdownProfileServices(mManager, 1);
        // all messages must be handled
        final Handler hdlr = sm.getHandler();
        assertTrue(!hdlr.hasMessages(BleProfileManagerState.START_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STARTED)
                && !hdlr.hasMessages(BleProfileManagerState.STOP_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STOPPED)
                && !hdlr.hasMessages(BleProfileManagerState.SHUTDOWN));
    }

    /**
     * Test if some Ble services are timeout when disabling.
     */
    public void test05DisableTimeoutFlow() {
        // mock protected method is broken @L, and we skip it
        if(!isSupportProtectedMock(mManager)) return;
        // prepare mock object
        TestUtil.mockitoStubStartProfileServices(mManager);
        TestUtil.mockitoStubStopProfileServices(mManager);
        TestUtil.mockitoStubShutdownProfileServices(mManager);
        Mockito.when(mPool.isAllObjectReleased()).thenReturn(true);
        // start simulation
        final BleProfileManagerState sm = TestUtil.makeBleProfileManagerState(mManager, mPool);
        sm.sendMessage(BleProfileManagerState.START_PROFILES);
        // verify result
        waitFor(100);
        TestUtil.mockitoVerifyStartProfileServices(mManager, 1);
        sm.sendMessage(BleProfileManagerState.PROFILES_STARTED);
        // stop
        sm.sendMessage(BleProfileManagerState.STOP_PROFILES);
        waitFor(100);
        TestUtil.mockitoVerifyStopProfileServices(mManager, 1);
        waitFor(6000);
        // message PROFILES_STOPPED will be dropped
        sm.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
        // wait for shutdown
        waitFor(6000);
        // no shutdown for stop timeout
        Mockito.verify(mPool).isAllObjectReleased();
        TestUtil.mockitoVerifyShutdownProfileServices(mManager, 1);
        // all messages must be handled
        final Handler hdlr = sm.getHandler();
        assertTrue(!hdlr.hasMessages(BleProfileManagerState.START_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STARTED)
                && !hdlr.hasMessages(BleProfileManagerState.STOP_PROFILES)
                && !hdlr.hasMessages(BleProfileManagerState.PROFILES_STOPPED)
                && !hdlr.hasMessages(BleProfileManagerState.SHUTDOWN));
    }

    private static boolean isSupportProtectedMock(BleProfileManagerService manager) {
        boolean bSupport = true;
        try {
            TestUtil.mockitoStubStartProfileServices(manager);
            TestUtil.mockitoStubStopProfileServices(manager);
            TestUtil.mockitoStubShutdownProfileServices(manager);
        } catch (Exception e) {
            bSupport = false;
        }
        return bSupport;
    }
}
