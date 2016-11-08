package com.hesine.nmsg.business.dao;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hesine.nmsg.common.MLog;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

public final class NmsCreateSmsThread {

    private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");
    public static final Uri CONTENT_URI = Uri.withAppendedPath(Uri.parse("content://mms-sms/"),
            "conversations");
    public static final Uri OBSOLETE_THREADS_URI = Uri.withAppendedPath(CONTENT_URI, "obsolete");
    public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern
            .compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    public static final int COMMON_THREAD = 0;
    public static final int BROADCAST_THREAD = 1;

    // No one should construct an instance of this class.
    private NmsCreateSmsThread() {
    }

    public static long getOrCreateThreadId(Context context, String recipient) {
        Set<String> recipients = new HashSet<String>();
        if (!NmsSendMessage.getInstance().isAddressLegal(recipient, recipients)) {
            return -1;
        }
        return getOrCreateThreadId(context, recipients);
    }

    /**
     * Given the recipients list and subject of an unsaved message, return its
     * thread ID. If the message starts a new thread, allocate a new thread ID.
     * Otherwise, use the appropriate existing thread ID.
     * 
     * Find the thread ID of the same set of recipients (in any order, without
     * any additions). If one is found, return it. Otherwise, return a unique
     * thread ID.
     */
    public static long getOrCreateThreadId(Context context, Set<String> recipients) {
        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

        for (String recipient : recipients) {
            if (isEmailAddress(recipient)) {
                recipient = extractAddrSpec(recipient);
            }

            uriBuilder.appendQueryParameter("recipient", recipient);
        }

        Uri uri = uriBuilder.build();

        return NmsMtkBinderApi.getInstance().getOrCreateThreadId(uri);
    }

    public static long getThreadIdByAddress(Context context, String address) {

        String selection = null;
        String recipientIds = null;
        Cursor cursor = null;
        long threadId = 0;

        if (address != null) {
            try {
                Uri addressesUrl = Uri.parse("content://mms-sms/canonical-addresses");
                selection = "address = \"" + address + "\"";

                cursor = NmsContentResolver.query(context.getContentResolver(), addressesUrl,
                        new String[] { "_id" }, selection, null, null);

                if (cursor != null && cursor.moveToNext()) {
                    recipientIds = cursor.getString(0);
                }
            } catch (NullPointerException e) {
                MLog.error(MLog.getStactTrace(e));
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        if (recipientIds != null) {
            Uri conversationUri = CONTENT_URI.buildUpon().appendQueryParameter("simple", "true")
                    .build();
            selection = "recipient_ids = ?";

            cursor = NmsContentResolver.query(context.getContentResolver(), conversationUri,
                    new String[] { "_id" }, selection, new String[] { recipientIds }, null);

            if (cursor != null && cursor.moveToNext()) {
                threadId = cursor.getLong(0);
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                cursor = null;
            }
        }
        return threadId;
    }

    public static String extractAddrSpec(String address) {
        Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }

    /**
     * Returns true if the address is an email address
     * 
     * @param address
     *            the input address to be tested
     * @return true if address is an email address
     */
    public static boolean isEmailAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        String s = extractAddrSpec(address);
        Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
        return match.matches();
    }
}
