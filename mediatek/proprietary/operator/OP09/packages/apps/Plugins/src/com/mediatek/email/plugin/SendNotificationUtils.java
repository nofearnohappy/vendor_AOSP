package com.mediatek.email.plugin;

import android.content.Intent;
import android.net.Uri;

/**
 * Utility for the sending notification plugin, re-defines some
 * variables and methods defined in emailcommon in order to use
 * the same utilities without including emailcommon module.
 */
public class SendNotificationUtils {
    public static final Uri FROM_ACCOUNT_AND_TYPE_URI =
        Uri.parse("content://com.android.email.provider/mailboxIdFromAccountAndType");
    public static final int TYPE_OUTBOX = 4;
    public static final String RECORD_ID = "_id";
    public static final String[] ID_PROJECTION = new String[] {
        RECORD_ID
    };
    public static final int ID_PROJECTION_COLUMN = 0;
    public static final long NO_MAILBOX = -1;

    private static final String VIEW_MAILBOX_INTENT_URL_PATH = "/view/mailbox";
    private static final String ACTIVITY_INTENT_SCHEME = "content";
    private static final String ACTIVITY_INTENT_HOST = "ui.email.android.com";

    private static final String ACCOUNT_ID_PARAM = "ACCOUNT_ID";
    private static final String MAILBOX_ID_PARAM = "MAILBOX_ID";

    public static Intent createOpenMailboxIntent(long accountId,
            long mailboxId) {
        final Uri.Builder b = createActivityIntentUrlBuilder(
                VIEW_MAILBOX_INTENT_URL_PATH);
        setAccountId(b, accountId);
        setMailboxId(b, mailboxId);
        return createRestartAppIntent(b.build());
    }

    private static Uri.Builder createActivityIntentUrlBuilder(String path) {
        final Uri.Builder b = new Uri.Builder();
        b.scheme(ACTIVITY_INTENT_SCHEME);
        b.authority(ACTIVITY_INTENT_HOST);
        b.path(path);
        return b;
    }

    /**
     * Add the account ID parameter.
     */
    private static void setAccountId(Uri.Builder b, long accountId) {
        if (accountId != -1) {
            b.appendQueryParameter(ACCOUNT_ID_PARAM, Long.toString(accountId));
        }
    }

    /**
     * Add the mailbox ID parameter.
     */
    private static void setMailboxId(Uri.Builder b, long mailboxId) {
        if (mailboxId != -1) {
            b.appendQueryParameter(MAILBOX_ID_PARAM, Long.toString(mailboxId));
        }
    }

    /**
     * Create an {@link Intent} to launch an activity as the main entry point.  Existing activities
     * will all be closed.
     */
    private static Intent createRestartAppIntent(Uri data) {
        Intent i = new Intent(Intent.ACTION_MAIN, data);
        prepareRestartAppIntent(i);
        return i;
    }

    private static void prepareRestartAppIntent(Intent i) {
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
