/**
 *
 */
package com.mediatek.dm.test.util;

import com.mediatek.dm.util.ScheduledTaskQueue;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * @author MTK80987
 *
 */
public class ScheduleTaskQueueTest extends AndroidTestCase {
    private static final String TAG = "[ScheduleTaskQueueTest]";

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testRemoveAll() {
        Log.d(TAG, "test removeAll begin");
        ScheduledTaskQueue queue = new ScheduledTaskQueue();
        queue.removeAll();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
