/**
 *
 */
package com.mediatek.dm.test.util;

import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.util.NiaDecoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author MTK80987
 *
 */
public class NiaDecoderTest extends AndroidTestCase {
    private static final String TAG = "[NiaDecoderTest]";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testDecode() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        Log.d(TAG, "test decode begin");
        Object obj = Class.forName(NiaDecoder.class.getName()).newInstance();
        Class<?> cls = obj.getClass();
        Method m = cls.getDeclaredMethod("decode", byte[].class);
        m.setAccessible(true);
        byte [] arr = new byte[200];
        m.invoke(null, arr);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
