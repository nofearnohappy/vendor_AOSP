/**
 *
 */
package com.mediatek.dm.test.util;

import java.lang.reflect.Constructor;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmPLLogger;
import com.mediatek.dm.util.FileLogger;

import com.redbend.android.VdmLogLevel;

/**
 * @author MTK80987
 *
 */
public class FileLoggerTest extends AndroidTestCase {
    private static final String TAG = "[FileLggerTest]";
    static final String MESSAGE_STR = "this is a test";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testLogMsg() {
        Log.d(TAG, "test logMsg begin");
        try {
            Object obj = Class.forName(FileLogger.class.getName()).newInstance();
            Class<?> cls = obj.getClass();

            Constructor<?> con = cls.getDeclaredConstructor(Context.class);
            DmPLLogger logger = (DmPLLogger) con.newInstance(this.getContext());

            logger.init(this.getContext());
            logger.logMsg(VdmLogLevel.DEBUG, MESSAGE_STR);
            logger.logMsg(VdmLogLevel.ERROR, MESSAGE_STR);
            logger.logMsg(VdmLogLevel.INFO, MESSAGE_STR);
            logger.logMsg(VdmLogLevel.NOTICE, MESSAGE_STR);
            logger.logMsg(VdmLogLevel.WARNING, MESSAGE_STR);
        } catch (Exception e) {
            Log.d(TAG, "test logMsg fail");
            e.printStackTrace();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
