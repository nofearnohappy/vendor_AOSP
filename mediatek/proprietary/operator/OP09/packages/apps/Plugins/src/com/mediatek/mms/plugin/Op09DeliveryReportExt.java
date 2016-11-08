package com.mediatek.mms.plugin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.provider.Telephony.Sms;

import com.mediatek.mms.ext.DefaultOpDeliveryReportExt;

public class Op09DeliveryReportExt extends DefaultOpDeliveryReportExt {

    static String MASS_TEXT_MESSAGE_GROUP_ID = "mass_txt_msg_group_id";

    public Op09DeliveryReportExt(Context context) {
        super(context);
    }

    @Override
    public Cursor getSmsReportItems(Intent intent, String[] projection, String selection) {
        long groupId = getGroupIdFromIntent(intent);
        if (groupId >= -1) {
            return null;
        }
        return SqliteWrapper.query(this, getContentResolver(), Sms.CONTENT_URI, projection,
                "ipmsg_id = ?", new String[] { groupId + "" }, null);
    }

    public long getGroupIdFromIntent(Intent intent) {
        if (intent == null) {
            return -1L;
        }
        return intent.getLongExtra(MASS_TEXT_MESSAGE_GROUP_ID, -1L);
    }
}
