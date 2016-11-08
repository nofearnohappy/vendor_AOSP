package com.mediatek.mms.plugin;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.util.SqliteWrapper;
import com.mediatek.mms.ext.DefaultOpLoadReqExt;
import com.mediatek.mms.ext.IOpMmsDraftDataExt;

public class Op09LoadReqExt extends DefaultOpLoadReqExt {

    private static final String TAG = "Op09LoadReqExt";
    private static final int MMS_ID_INDEX = 0;

    private static final int MMS_ADDR = 0;
    private static final int MMS_ADDR_CHARSET = 1;

    // Draft message stuff
    private static final String[] MMS_DRAFT_PROJECTION = {
            Mms._ID, // 0
            Mms.SUBJECT, // 1
            Mms.SUBJECT_CHARSET // 2
    };

    // / M: support mms cc feature, OP09 requested.
    private static final String[] MMS_ADDR_PROJECTION = { Mms.Addr.ADDRESS, // 0
            Mms.Addr.CHARSET // 1
    };

    @Override
    public void executeReq(final Context context, final long threadId,
            IOpMmsDraftDataExt opDraftData) {
        StringBuilder cc = readDraftMmsMessage(context, threadId);
        if (cc != null && cc.toString().length() != 0) {
            ((Op09MmsDraftDataExt)opDraftData).setCc(cc.toString());
        }
    }

    public StringBuilder readDraftMmsMessage(final Context context, final long threadId) {
        StringBuilder ccList = new StringBuilder();
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "[readDraftMmsMessage] begin");

        if (threadId <= 0) {
            Log.d(TAG, "[readDraftMmsMessage] threadId <= 0");
            return null;
        }

        Cursor cursor;
        ContentResolver cr = context.getContentResolver();
        final String selection = Mms.THREAD_ID + " = " + threadId;
        cursor = SqliteWrapper.query(context, cr, Mms.Draft.CONTENT_URI,
                MMS_DRAFT_PROJECTION, selection, null, null);

        if (cursor == null) {
            Log.d(TAG, "[readDraftMmsMessage] cursor is null");
            return null;
        }

        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    if (MessageUtils.isSupportSendMmsWithCc()) {
                        Uri addrUri = Uri.parse("content://mms/" + cursor.getLong(MMS_ID_INDEX)
                                + "/addr");
                        final String where = Mms.Addr.TYPE + " = " + PduHeaders.CC;
                        Cursor addr = SqliteWrapper.query(context, cr,
                                    addrUri, MMS_ADDR_PROJECTION,
                                    where, null, null);
                        if (addr != null) {
                            while (addr.moveToNext()) {
                                String number = MessageUtils.extractEncStrFromCursor(addr,
                                        MMS_ADDR, MMS_ADDR_CHARSET);
                                ccList.append(number + ";");
                            }
                            addr.close();
                        }
                    }
                    long costTime = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "[readDraftMmsMessage] end and return uri, and cost : " + costTime);
                    return ccList;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        long costTime1 = System.currentTimeMillis() - startTime;
        Log.d(TAG, "[readDraftMmsMessage] end and return null, and cost : " + costTime1);
        return null;
    }
}
