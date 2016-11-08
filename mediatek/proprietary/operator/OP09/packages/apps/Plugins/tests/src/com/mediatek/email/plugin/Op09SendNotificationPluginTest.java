package com.mediatek.email.plugin;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mediatek.email.ext.ISendNotification;
import com.mediatek.pluginmanager.PluginManager;

import java.lang.reflect.Field;

public class Op09SendNotificationPluginTest extends InstrumentationTestCase {
    private final String TAG = "Op09SendNotificationPluginTest";
    // The SendNotificationPlugin object which under test
    private SendNotificationPlugin mSendNotificationPlugin;
    // The context of the target
    private Context mContext;
    private final static String METANAME_SN = "sendnotification";

    // Event types definition
    public static final int SEND_MAIL = 0;
    public static final int SEND_FAILED = 1;
    public static final int SEND_COMPLETE = 2;

    // The length of the whole progress bar
    private static final int BAR_LENGTH = 10000;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = this.getInstrumentation().getTargetContext();
        mSendNotificationPlugin = (SendNotificationPlugin) PluginManager.createPluginObject(mContext,
                ISendNotification.class.getName(), METANAME_SN);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mSendNotificationPlugin = null;
    }

    public void testShowSendingNotification() throws Exception {
        try {
            long value = -1;
            // Get the fields for testing
            Field fieldSendCount = mSendNotificationPlugin.getClass().getDeclaredField("mSendCount");
            fieldSendCount.setAccessible(true);
            Field fieldFailedAccountId = mSendNotificationPlugin.getClass().getDeclaredField("mFailedAccountId");
            fieldFailedAccountId.setAccessible(true);
            Field fieldFailedCount = mSendNotificationPlugin.getClass().getDeclaredField("mFailedCount");
            fieldFailedCount.setAccessible(true);
            Field fieldCompletedCount = mSendNotificationPlugin.getClass().getDeclaredField("mCompletedCount");
            fieldCompletedCount.setAccessible(true);
            Field fieldMailProgress = mSendNotificationPlugin.getClass().getDeclaredField("mMailProgress");
            fieldMailProgress.setAccessible(true);

            // Send 1 mail for account 1
            mSendNotificationPlugin.showSendingNotification(mContext, 1, SEND_MAIL, 1);
            value = fieldSendCount.getInt(mSendNotificationPlugin);
            assertEquals(1, value);
            value = fieldMailProgress.getInt(mSendNotificationPlugin);
            assertEquals(BAR_LENGTH, value);

            // The mail sent successfully
            mSendNotificationPlugin.showSendingNotification(mContext, 1, SEND_COMPLETE, 1);
            value = fieldSendCount.getInt(mSendNotificationPlugin);
            assertEquals(0, value);

            // Send 2 mail for account 1
            mSendNotificationPlugin.showSendingNotification(mContext, 1, SEND_MAIL, 2);
            value = fieldSendCount.getInt(mSendNotificationPlugin);
            assertEquals(2, value);
            value = fieldMailProgress.getInt(mSendNotificationPlugin);
            assertEquals(BAR_LENGTH / 2, value);

            // 1 mail sent failed
            mSendNotificationPlugin.showSendingNotification(mContext, 1, SEND_FAILED, 1);
            value = fieldFailedAccountId.getLong(mSendNotificationPlugin);
            assertEquals(1, value);
            value = fieldFailedCount.getInt(mSendNotificationPlugin);
            assertEquals(1, value);

            mSendNotificationPlugin.cancelSendingNotification();

            // 2 accounts send mails respectively
            mSendNotificationPlugin.showSendingNotification(mContext, 1, SEND_MAIL, 2);
            value = fieldFailedAccountId.getLong(mSendNotificationPlugin);
            assertEquals(-1, value);
            mSendNotificationPlugin.showSendingNotification(mContext, 2, SEND_MAIL, 1);
            value = fieldSendCount.getInt(mSendNotificationPlugin);
            assertEquals(3, value);
            mSendNotificationPlugin.showSendingNotification(mContext, 1, SEND_COMPLETE, 1);
            value = fieldCompletedCount.getInt(mSendNotificationPlugin);
            assertEquals(1, value);
            mSendNotificationPlugin.showSendingNotification(mContext, 2, SEND_FAILED, 1);
            value = fieldFailedAccountId.getLong(mSendNotificationPlugin);
            assertEquals(2, value);
            mSendNotificationPlugin.cancelSendingNotification();
        } catch (Exception e) {
            Log.d(TAG, "Exception occurs:" + e.getMessage());
            throw e;
        }
    }
}
