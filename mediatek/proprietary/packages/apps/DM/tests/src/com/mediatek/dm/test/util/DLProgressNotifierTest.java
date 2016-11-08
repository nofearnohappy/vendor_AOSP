/**
 *
 */
package com.mediatek.dm.test.util;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.util.DLProgressNotifier;

/**
 * @author MTK80987
 *
 */
public class DLProgressNotifierTest extends AndroidTestCase {

    private static final String TAG = "[DLProgressNotifierTest]";
    private static final int CUR_SIZE = 10;
    private static final int TOTAL_SIZE = 100;

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testOnProgressUpdate() {
        Log.d(TAG, "test onProgessUpdate begin");
        Context context = getContext();
        DLProgressNotifier notifier = new DLProgressNotifier(context, null);
        notifier.onProgressUpdate(CUR_SIZE, TOTAL_SIZE);

        notifier.onFinish();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
