
package com.mediatek.bluetoothle.tests;



import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mediatek.bluetoothle.BleProfileServerObjectPool;
import com.mediatek.bluetoothle.bleservice.BleApp;
import com.mediatek.bluetoothle.bleservice.BleProfileManagerService;
import com.mediatek.bluetoothle.bleservice.BleProfileManagerState;
import com.mediatek.bluetoothle.bleservice.TestUtil;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * BleProfileManagerServiceTest
 */
public class BleProfileManagerServiceTest extends InstrumentationTestCase {
    private static final String TAG = "BleProfileManagerServiceTest";
    private BleProfileManagerService mService;

    @Override
    protected void setUp() throws Exception {
        // Workaround1: Space for Mock classes generation
        System.setProperty("dexmaker.dexcache", getInstrumentation()
                .getTargetContext().getCacheDir().getPath());
        // Workaround2: For sharedUserId
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // init
        BleProfileServerObjectPool.getInstance().init(mock(Context.class));
    }

    /**
     * Test start  profile service for normal flow
     */
    @SuppressWarnings("unchecked")
    public void test01StartProfileServicesNormalFlow() {
        final Context mockCtxt = mock(Context.class);

        // reset mockCtxt
        Log.i(TAG, "test01StartProfileServicesNormalFlow: reset mockCtxt");
        resetMockCtxt(mockCtxt, "test01StartProfileServicesNormalFlow");

        // new service
        Log.i(TAG, "test01StartProfileServicesNormalFlow: new BleProfileManagerService()");
        mService = new BleProfileManagerService();

        // inject mock object
        Log.i(TAG, "test01StartProfileServicesNormalFlow: inject mock context");
        assertTrue(setField(mService.getClass(), mService, "mContext", mockCtxt));

        // invoke onCreate
        Log.i(TAG, "test01StartProfileServicesNormalFlow: mService.onCreate()");
        mService.onCreate();

        // get field mProfileServicesState
        Log.i(TAG, "test01StartProfileServicesNormalFlow: get field mProfileServicesState");
        HashMap<String, Integer> profileServicesState = null;
        profileServicesState = (HashMap<String, Integer>) getField(mService.getClass(),
                mService, "mProfileServicesState");
        assertNotNull(profileServicesState);

        // start turn on
        Log.i(TAG, "test01StartProfileServicesNormalFlow: start turn on");
        mService.onStartCommand(createIntent4StartService(BluetoothAdapter.STATE_ON),
                Service.START_STICKY, 0);
        verify(mockCtxt, timeout(100).times(TestUtil.getSupportedProfiles().length)).startService(
                any(Intent.class));
        Log.i(TAG, "test01StartProfileServicesNormalFlow: verify pass");

        // check the final state
        Log.i(TAG, "test01StartProfileServicesNormalFlow: check the final state");
        final Iterator<Map.Entry<String, Integer>> i =
                profileServicesState.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Integer> entry = i.next();
            assertTrue(entry.getValue() == BluetoothAdapter.STATE_TURNING_ON);
        }
    }

    /**
     * Test start  profile service for timeout flow
     */
    @SuppressWarnings("unchecked")
    public void test02StartProfileServicesTimeoutFlow() {
        final Context mockCtxt = mock(Context.class);

        // reset mockCtxt
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: reset mockCtxt");
        resetMockCtxt(mockCtxt, "test02StartProfileServicesTimeoutFlow");

        // new service
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: new BleProfileManagerService()");
        mService = new BleProfileManagerService();

        // inject mock object
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: inject mock context");
        assertTrue(setField(mService.getClass(), mService, "mContext", mockCtxt));

        // invoke onCreate
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: mService.onCreate()");
        mService.onCreate();

        // get field mProfileServicesState
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: get field mProfileServicesState");
        HashMap<String, Integer> profileServicesState = null;
        profileServicesState = (HashMap<String, Integer>) getField(mService.getClass(),
                mService, "mProfileServicesState");
        assertNotNull(profileServicesState);

        // let a service's state as abnormal
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: let a service's state as abnormal");
        profileServicesState.put(TestUtil.getSupportedProfiles()[0].getName(),
                BluetoothAdapter.STATE_TURNING_OFF);

        // start turn on
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: start turn on");
        mService.onStartCommand(createIntent4StartService(BluetoothAdapter.STATE_ON),
                Service.START_STICKY, 0);
        verify(mockCtxt, timeout(100).times(TestUtil.getSupportedProfiles().length - 1)).startService(
                any(Intent.class));
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: verify pass");

        // check the final state
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: check the final state");
        final Iterator<Map.Entry<String, Integer>> i =
                profileServicesState.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Integer> entry = i.next();
            if (entry.getKey() == TestUtil.getSupportedProfiles()[0].getName()) {
                assertTrue(entry.getValue() == BluetoothAdapter.STATE_TURNING_OFF);
            } else {
                assertTrue(entry.getValue() == BluetoothAdapter.STATE_TURNING_ON);
            }
        }

        // wait for timeout message
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: wait for timeout");
        waitFor(6000);

        // check the final state
        final BleProfileManagerState sm = (BleProfileManagerState) getField(mService.getClass(),
                mService, "mStateMachine");
        assertNotNull(sm);
        final String strCurState = TestUtil.getCurrentState(sm).getName();
        Log.i(TAG, "test02StartProfileServicesTimeoutFlow: final state "
                + strCurState);
        assertTrue("StartState".equals(strCurState));
    }

    /**
     * Test start then stop profile service for normal flow
     */
    @SuppressWarnings("unchecked")
    public void test03StopProfileServicesNormalFlow() {
        final Context mockCtxt = mock(Context.class);
        final Class<?>[] supportedProfiles = TestUtil.getSupportedProfiles();
        final int profileNum = supportedProfiles.length;

        // reset mockCtxt
        Log.i(TAG, "test03StopProfileServicesNormalFlow: reset mockCtxt");
        resetMockCtxt(mockCtxt, "test03StopProfileServicesNormalFlow");

        // new service
        Log.i(TAG, "test03StopProfileServicesNormalFlow: new BleProfileManagerService()");
        mService = new BleProfileManagerService();

        // inject mock object
        Log.i(TAG, "test03StopProfileServicesNormalFlow: inject mock context");
        assertTrue(setField(mService.getClass(), mService, "mContext", mockCtxt));

        // invoke onCreate
        Log.i(TAG, "test03StopProfileServicesNormalFlow: mService.onCreate()");
        mService.onCreate();

        // get field mProfileServicesState
        Log.i(TAG, "test03StopProfileServicesNormalFlow: get field mProfileServicesState");
        HashMap<String, Integer> profileServicesState = null;
        profileServicesState = (HashMap<String, Integer>) getField(mService.getClass(),
                mService, "mProfileServicesState");
        assertNotNull(profileServicesState);

        // start turn on
        Log.i(TAG, "test03StopProfileServicesNormalFlow: start turn on");
        mService.onStartCommand(createIntent4StartService(BluetoothAdapter.STATE_ON),
                Service.START_STICKY, 0);
        verify(mockCtxt, timeout(100).times(profileNum)).startService(any(Intent.class));
        Log.i(TAG, "test03StopProfileServicesNormalFlow: verify pass");

        // reset mockCtxt
        Log.i(TAG, "test03StopProfileServicesNormalFlow: reset mockCtxt");
        resetMockCtxt(mockCtxt, "test03StopProfileServicesNormalFlow");

        // notify turn on ok
        Log.i(TAG, "test03StopProfileServicesNormalFlow: notify turn on ok");
        notifyAllProfileServicesStateChanged(mService, BluetoothAdapter.STATE_ON);

        // start turn off
        Log.i(TAG, "test03StopProfileServicesNormalFlow: start turn off");
        mService.onStartCommand(createIntent4StartService(BluetoothAdapter.STATE_OFF),
                Service.START_STICKY, 0);
        verify(mockCtxt, timeout(100).times(profileNum)).startService(any(Intent.class));
        Log.i(TAG, "test03StopProfileServicesNormalFlow: verify pass");

        // notify turn off ok
        Log.i(TAG, "test03StopProfileServicesNormalFlow: notify turn off ok");
        notifyAllProfileServicesStateChanged(mService, BluetoothAdapter.STATE_OFF);

        // check the final state
        Log.i(TAG, "test03StopProfileServicesNormalFlow: check the final state");
        final Iterator<Map.Entry<String, Integer>> i =
                profileServicesState.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Integer> entry = i.next();
            assertTrue(entry.getValue() == BluetoothAdapter.STATE_OFF);
        }
    }

    /**
     * Test start then stop profile service for timeout flow
     */
    @SuppressWarnings("unchecked")
    public void test04StopProfileServicesTimeoutFlow() {
        final Context mockCtxt = mock(Context.class);
        final Class<?>[] supportedProfiles = TestUtil.getSupportedProfiles();
        final int profileNum = supportedProfiles.length;

        // reset mockCtxt
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: reset mockCtxt");
        resetMockCtxt(mockCtxt, "test04StopProfileServicesTimeoutFlow");

        // new service
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: new BleProfileManagerService()");
        mService = new BleProfileManagerService();

        // inject mock object
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: inject mock context");
        assertTrue(setField(mService.getClass(), mService, "mContext", mockCtxt));

        // invoke onCreate
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: mService.onCreate()");
        mService.onCreate();

        // get field mProfileServicesState
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: get field mProfileServicesState");
        HashMap<String, Integer> profileServicesState = null;
        profileServicesState = (HashMap<String, Integer>) getField(mService.getClass(),
                mService, "mProfileServicesState");
        assertNotNull(profileServicesState);

        // let a service's state as abnormal
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: let a service's state as abnormal");
        profileServicesState.put(supportedProfiles[0].getName(),
                BluetoothAdapter.STATE_TURNING_OFF);

        // start turn on
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: start turn on");
        mService.onStartCommand(createIntent4StartService(BluetoothAdapter.STATE_ON),
                Service.START_STICKY, 0);
        verify(mockCtxt, timeout(100).times(profileNum - 1)).startService(any(Intent.class));
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: verify pass");

        // reset mockCtxt
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: reset mockCtxt");
        resetMockCtxt(mockCtxt, "test04StopProfileServicesTimeoutFlow");

        // notify turn on ok except the abnormal one
        Log.i(TAG,
                "test04StopProfileServicesTimeoutFlow: notify turn on ok except the abnormal one");
        notifyProfileServicesStateChanged(mService,
                Arrays.copyOfRange(supportedProfiles, 1, supportedProfiles.length),
                BluetoothAdapter.STATE_ON);

        // wait for timeout
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: wait for start timeout");
        waitFor(6000);

        // check the current state
        final BleProfileManagerState sm = (BleProfileManagerState) getField(mService.getClass(),
                mService, "mStateMachine");
        assertNotNull(sm);
        String strCurState = TestUtil.getCurrentState(sm).getName();
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: current state "
                + strCurState);
        assertTrue("StartState".equals(strCurState));

        // start turn off
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: start turn off");
        mService.onStartCommand(createIntent4StartService(BluetoothAdapter.STATE_OFF),
                Service.START_STICKY, 0);
        verify(mockCtxt, timeout(100).times(profileNum - 1)).startService(any(Intent.class));
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: verify pass");

        // notify turn on ok except the abnormal one
        Log.i(TAG,
                "test04StopProfileServicesTimeoutFlow: notify turn on ok except the abnormal one");
        notifyProfileServicesStateChanged(mService,
                Arrays.copyOfRange(supportedProfiles, 1, supportedProfiles.length),
                BluetoothAdapter.STATE_OFF);

        // wait for timeout
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: wait for stop timeout");
        waitFor(6000);

        // check the final state
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: check the final state");
        final Iterator<Map.Entry<String, Integer>> i =
                profileServicesState.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<String, Integer> entry = i.next();
            if (entry.getKey() == supportedProfiles[0].getName()) {
                assertTrue(entry.getValue() == BluetoothAdapter.STATE_TURNING_OFF);
            } else {
                assertTrue(entry.getValue() == BluetoothAdapter.STATE_OFF);
            }
        }

        strCurState = TestUtil.getCurrentState(sm).getName();
        Log.i(TAG, "test04StopProfileServicesTimeoutFlow: final state "
                + strCurState);
        assertTrue("StopState".equals(strCurState));
    }

    private static Object getField(final Class<?> clazz, final Object obj, final String fieldName) {
        Object value = null;
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            value = field.get(obj);
        } catch (final IllegalAccessException e) {
            Log.v(TAG, "getField: " + e);
        } catch (final NoSuchFieldException e) {
            Log.v(TAG, "getField: " + e);
        }
        return value;
    }

    private static boolean setField(final Class<?> clazz,
            final Object obj, final String fieldName, final Object initValue) {
        boolean bSuccess = false;
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, initValue);
            bSuccess = true;
        } catch (final NoSuchFieldException e) {
            Log.v(TAG, "setField: " + e);
        } catch (final IllegalAccessException e) {
            Log.v(TAG, "setField: " + e);
        }
        return bSuccess;
    }

    @SuppressWarnings("rawtypes")
    private static void notifyAllProfileServicesStateChanged(
            final BleProfileManagerService service,
            final int state) {
        final Class[] supportedProfileServices = TestUtil.getSupportedProfiles();
        notifyProfileServicesStateChanged(service, supportedProfileServices, state);
    }

    @SuppressWarnings("rawtypes")
    private static void notifyProfileServicesStateChanged(final BleProfileManagerService services,
            final Class[] supportedProfileServices, final int state) {
        for (int i = 0; i < supportedProfileServices.length; i++) {
            TestUtil.notifyProfileStateServiceStateChange(services,
                    supportedProfileServices[i].getName(), state);
        }
    }

    private static Intent createIntent4StartService(final int state) {
        final Intent intent = new Intent();
        intent.putExtra(BleApp.EXTRA_ACTION, BleApp.ACTION_SERVICE_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, state);
        return intent;
    }

    private static void waitFor(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Log.e(TAG, "" + e);
        }
    }

    private void resetMockCtxt(final Context ctxt, final String testCaseName) {
        reset(ctxt);
        // add logging mechanism
        when(ctxt.startService(any(Intent.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Log.v(TAG, "test04StopProfileServicesTimeoutFlow: startService is invoked");
                return null;
            }
        });

        // mock applicationInfo for Notification (in L)
        Mockito.when(ctxt.getApplicationInfo())
                .thenReturn(getInstrumentation().getTargetContext().getApplicationInfo());
        // mock resources for Notification (in L)
        Mockito.when(ctxt.getResources())
                .thenReturn(getInstrumentation().getTargetContext().getResources());
    }
}
