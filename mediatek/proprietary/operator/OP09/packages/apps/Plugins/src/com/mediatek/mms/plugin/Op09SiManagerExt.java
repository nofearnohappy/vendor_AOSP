package com.mediatek.mms.plugin;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.WapPush;

import com.mediatek.mms.ext.DefaultOpSiManagerExt;

public class Op09SiManagerExt extends DefaultOpSiManagerExt {
    @Override
    public void handleIncoming(Context context, ContentValues values, String uri) {
        if (MessageUtils.isShowDialogForNewSIMsg()) {
            Intent intent = new Intent();
            intent.setAction("com.mediatek.action.WPMessageDialogActivity");
            intent.setPackage("com.android.mms");
            intent.putExtra(WapPush.ADDR, values.getAsString(WapPush.ADDR));
            intent.putExtra(WapPush.SERVICE_ADDR, values.getAsString(WapPush.SERVICE_ADDR));
            intent.putExtra(WapPush.SUBSCRIPTION_ID, values.getAsString(WapPush.SUBSCRIPTION_ID));
            intent.putExtra(WapPush.URL, values.getAsString(WapPush.URL));
            intent.putExtra(WapPush.SIID, values.getAsString(WapPush.SIID));
            intent.putExtra(WapPush.ACTION, values.getAsInteger(WapPush.ACTION));
            intent.putExtra(WapPush.CREATE, values.getAsInteger(WapPush.CREATE));
            intent.putExtra(WapPush.EXPIRATION, values.getAsLong(WapPush.EXPIRATION));
            intent.putExtra(WapPush.TEXT, values.getAsString(WapPush.TEXT));
            intent.putExtra(WapPush.DATE, System.currentTimeMillis());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("uri", uri.toString());
            context.startActivity(intent);
        }
    }
}
