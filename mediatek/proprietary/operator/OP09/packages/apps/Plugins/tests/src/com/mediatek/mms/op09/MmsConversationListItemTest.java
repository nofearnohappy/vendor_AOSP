package com.mediatek.mms.op09;

import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsConversationListItemExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase; 

public class MmsConversationListItemTest extends BasicCase {
    private static IMmsConversationListItemExt sConversaitonListItem;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sConversaitonListItem = MPlugin.createInstance("com.mediatek.mms.ext.IMmsConversationListItem", mContext);
    }

    public void test001ShowSimType() {
        // Clear the sms table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Sms.CONTENT_URI, null, null);

        long threadId = Threads.getOrCreateThreadId(mContext, TEST_ADDRESS);
        Sms.addMessageToUri(mContext.getContentResolver(), Sms.CONTENT_URI,
                TEST_ADDRESS, SMS_CONTENT, "TEST", System.currentTimeMillis(),
                true, false, threadId, mSimIdGsm);

        sConversaitonListItem.showSimType(mContext, null, null);
        sConversaitonListItem.showSimType(mContext,
                Uri.withAppendedPath(Threads.CONTENT_URI, String.valueOf(threadId)), null);

        TextView textView = new TextView(mContext);
        sConversaitonListItem.showSimType(mContext,
            Uri.withAppendedPath(Threads.CONTENT_URI, String.valueOf(threadId)), textView);
    }

    public void test002SetViewSize() {
        TextView textView = new TextView(mContext);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(lp);

        sConversaitonListItem.setViewSize(null);
        sConversaitonListItem.setViewSize(textView);
    }
}
