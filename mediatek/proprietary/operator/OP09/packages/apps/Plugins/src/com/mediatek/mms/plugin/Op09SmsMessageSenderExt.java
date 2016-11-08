package com.mediatek.mms.plugin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.provider.Telephony.Sms;

import com.mediatek.mms.ext.DefaultOpSmsMessageSenderExt;

public class Op09SmsMessageSenderExt extends DefaultOpSmsMessageSenderExt {

    private static final String TAG = "Mms/Op09SmsMessageSenderExt";
    /**
     * Constructor.
     * @param context the Context.
     */
    public Op09SmsMessageSenderExt(Context context) {
        super();
    }

    @Override
    public Uri queueMessage(int numberOfDests, ContentResolver resolver, String address,
            String body, Long date, boolean deliveryReport, long threadId, long subId,
            long ipmsgId) {
        Uri smsUri = null;
        if (numberOfDests > 1) {
            smsUri = addMessageToUri(resolver,
                    Uri.parse("content://sms/queued"), address, body, null, date, true,
                    deliveryReport, threadId, subId, ipmsgId);
        }
        return smsUri;
    }

    private Uri addMessageToUri(ContentResolver resolver, Uri uri, String address, String body,
            String subject, Long date, boolean read, boolean deliveryReport, long threadId,
            long subId, long ipmsgId) {
        Log.d(TAG, "uri:" + uri + " ipmsgId:" + ipmsgId);
        ContentValues values = new ContentValues(10);

        values.put(Sms.ADDRESS, address);
        values.put(Sms.READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
        values.put(Sms.SUBJECT, subject);
        values.put(Sms.BODY, body);
        values.put(Sms.SEEN, read ? Integer.valueOf(1) : Integer.valueOf(0));

        if (date != null) {
            values.put(Sms.DATE, date);
        }

        if (deliveryReport) {
            values.put(Sms.STATUS, Sms.STATUS_PENDING);
        }
        if (threadId != -1L) {
            values.put(Sms.THREAD_ID, threadId);
        }

        if (subId != -1) {
            values.put(Sms.SUBSCRIPTION_ID, subId);
        }
        values.put(Sms.IPMSG_ID, ipmsgId);
        return resolver.insert(uri, values);
    }
}
