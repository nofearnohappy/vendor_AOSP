/**
 *
 */
package com.mediatek.dm.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.mediatek.dm.DmPLLogger;

import com.redbend.android.VdmLogLevel;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * @author MTK80987
 *
 */
public class DmPlLoggerTest extends AndroidTestCase {
    private static final String TAG = "[DmPlLoggerTest]";
    private static final String MSG = "test";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testLogMsg() {
        Log.d(TAG, "test logMsg begin");

        Object obj;
        try {
            obj = Class.forName(DmPLLogger.class.getName()).newInstance();
            Class<?> cls = obj.getClass();
            Constructor con = cls.getDeclaredConstructor(Context.class);
            DmPLLogger logger = (DmPLLogger) con.newInstance(this.getContext());

            logger.logMsg(VdmLogLevel.DEBUG, MSG);
            logger.logMsg(VdmLogLevel.ERROR, MSG);
            logger.logMsg(VdmLogLevel.INFO, MSG);
            logger.logMsg(VdmLogLevel.NOTICE, MSG);
            logger.logMsg(VdmLogLevel.WARNING, MSG);

        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
