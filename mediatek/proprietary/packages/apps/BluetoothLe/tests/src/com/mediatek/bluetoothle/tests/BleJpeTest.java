package com.mediatek.bluetoothle.tests;


import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.bluetoothle.bleservice.BleReceiver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test Case For BleJpe
 */
public class BleJpeTest extends AndroidTestCase {
    private static final String TAG = "BleJpeTest";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.v(TAG, " setUp JPE Test Case ");
        Log.v(TAG, " setUp NativeCheck = " + com.mediatek.common.jpe.a.b);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Log.v(TAG, " tearDown JPE Test Case ");
        Log.v(TAG, " tearDown NativeCheck = " + com.mediatek.common.jpe.a.b);
    }

    /**
     * Test JPE works or not
     */
    public void testJPEwork() {
        try {
            final Method method = BleReceiver.class
                    .getMethod("sendIntentToBleProfileManagerService");
            method.setAccessible(true);
            method.invoke(null);
        } catch (final NoSuchMethodException e) {
            Log.v(TAG, "" + e);
        } catch (final IllegalAccessException e) {
            Log.v(TAG, "" + e);
        } catch (final InvocationTargetException e) {
            Log.v(TAG, "" + e);
        }
        Log.v(TAG, "testJPEwork NativeCheck = " + com.mediatek.common.jpe.a.b);
        assertTrue("testJPEwork is not JPE checked", (com.mediatek.common.jpe.a.b));
    }
}
