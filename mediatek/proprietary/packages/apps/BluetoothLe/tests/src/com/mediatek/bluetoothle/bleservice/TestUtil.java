
package com.mediatek.bluetoothle.bleservice;

import com.android.internal.util.IState;
import com.mediatek.bluetoothle.BleProfileServerObjectPool;

import org.mockito.Mockito;

/**
 * Util for access package member
 */
public class TestUtil {

    /**
     * Utility for call BleProfileManagerState's make method
     *
     * @param manager the instance of BleProfileManagerService
     * @param pool the instance of pool
     * @return instance of BleProfileManagerState
     */
    public static BleProfileManagerState makeBleProfileManagerState(
            final BleProfileManagerService manager,
            final BleProfileServerObjectPool pool) {
        return BleProfileManagerState.make(manager, pool);
    }

    /**
     * Wrapper method for package method
     *
     * @param manager parameter
     */
    public static void mockitoStubStartProfileServices(final BleProfileManagerService manager) {
        Mockito.doNothing().when(manager).startProfileServices();
    }

    /**
     * Wrapper method for package method
     *
     * @param manager parameter
     */
    public static void mockitoStubStopProfileServices(final BleProfileManagerService manager) {
        Mockito.when(manager.stopProfileServices()).thenReturn(true);
    }

    /**
     * Wrapper method for package method
     *
     * @param manager parameter
     */
    public static void mockitoStubShutdownProfileServices(final BleProfileManagerService manager) {
        Mockito.doNothing().when(manager).shutdown();
    }

    /**
     * Wrapper method for package method
     *
     * @param manager parameter
     * @param times parameter
     */
    public static void mockitoVerifyStopProfileServices(final BleProfileManagerService manager,
            final int times) {
        Mockito.verify(manager, Mockito.times(times)).stopProfileServices();
    }

    /**
     * Wrapper method for package method
     *
     * @param manager parameter
     * @param times parameter
     */
    public static void mockitoVerifyStartProfileServices(final BleProfileManagerService manager,
            final int times) {
        Mockito.verify(manager, Mockito.times(times)).startProfileServices();
    }

    /**
     * Wrapper method for package method
     *
     * @param manager parameter
     * @param times parameter
     */
    public static void mockitoVerifyShutdownProfileServices(final BleProfileManagerService manager,
            final int times) {
        Mockito.verify(manager, Mockito.times(times)).shutdown();
    }

    /**
     * Wrapper method for package method
     *
     * @return array of class
     */
    @SuppressWarnings("rawtypes")
    public static Class[] getSupportedProfiles() {
        return Config.getSupportedProfiles();
    }

    /**
     * Wrapper method for package method
     *
     * @param service parameter
     * @param name parameter
     * @param state parameter
     */
    public static void notifyProfileStateServiceStateChange(final BleProfileManagerService service,
            final String name, final int state) {
        service.onProfileServiceStateChanged(name, state);
    }

    /**
     * Wrapper method for package method
     *
     * @param sm parameter
     *
     * @return IState
     */
    public static IState getCurrentState(final BleProfileManagerState sm) {
        return sm.getCurtate();
    }
}
