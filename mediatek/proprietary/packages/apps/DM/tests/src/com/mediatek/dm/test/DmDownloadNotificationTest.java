/**
 *
 */
package com.mediatek.dm.test;

import java.lang.reflect.*;


import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mediatek.dm.DmDownloadNotification;

/**
 * @author MTK80987
 *
 */
public class DmDownloadNotificationTest extends AndroidTestCase {

    private static final String TAG = "[DmDownloadNotificationTest]";
    public static final int NOTIFICATION_USERMODE_VISIBLE = 5;
    private Context context;
    private DmDownloadNotification notify;

    protected void setUp() throws Exception {
        super.setUp();

        context = getContext();
        notify = new DmDownloadNotification(context);
    }

    public void testShowNiaNotification() {
        Log.d(TAG, "test showNiaNotification begin");

        notify.showNiaNotification();
    }

    public void testShowUserNodeNotification() {
        Log.d(TAG, "test showUserNodeNotification begin");
        notify.showUserModeNotification(NOTIFICATION_USERMODE_VISIBLE);
    }

    public void testShowNewVersionNotification() {
        Log.d(TAG, "test showNewVersionNotification begin");
        notify.showNewVersionNotification();
    }

    public void testShowDownloadCompletedNotification() {
        Log.d(TAG, "test showDownloadCompletedNotification begin");
        notify.showDownloadCompletedNotification();
    }

    public void testClearDownloadNotification() {
        Log.d(TAG, "test clearDownloadNotification begin");
        notify.clearDownloadNotification();

    }


    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
