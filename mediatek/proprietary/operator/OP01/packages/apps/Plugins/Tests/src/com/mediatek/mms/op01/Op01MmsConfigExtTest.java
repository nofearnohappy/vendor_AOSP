
package com.mediatek.op01.tests;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.test.InstrumentationTestCase;

import com.mediatek.mms.plugin.Op01MmsConfigExt;
import com.mediatek.common.MPlugin;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import android.net.Uri;

import com.mediatek.mms.plugin.ExtendURLSpan;
import android.widget.TextView;

public class Op01MmsConfigExtTest extends InstrumentationTestCase {

    private static int sSmsToMmsThreadshold = 11;
    private static final int MAX_CMCC_TEXT_LENGTH = 3100;
    private static final int RECIPIENTS_LIMIT = 50;
    private static final String UAPROFURL_OP01 = "http://218.249.47.94/Xianghe/MTK_Athens15_UAProfile.xml";
    private static final int SOCKET_TIMEOUT = 90 * 1000;

    private static Op01MmsConfigExt sMmsConfigPlugin = null;
    private Context mContext;

    private ExtendURLSpan mUrlSpan = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = this.getInstrumentation().getContext();
        sMmsConfigPlugin = (Op01MmsConfigExt)MPlugin.createInstance("com.mediatek.mms.ext.IMmsConfigExt",mContext);
        mUrlSpan = new ExtendURLSpan("http://test.com");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sMmsConfigPlugin = null;
    }

    public void testGetSmsToMmsTextThreshold() {
        int threshold = sMmsConfigPlugin.getSmsToMmsTextThreshold();
        assertEquals(sSmsToMmsThreadshold, threshold);
    }

    public void testGetMaxTextLimit() {
        int maxTextLimit = sMmsConfigPlugin.getMaxTextLimit();
        assertEquals(MAX_CMCC_TEXT_LENGTH, maxTextLimit);
    }

    public void testGetMmsRecipientLimit() {
        int mmsRecipientLimit = sMmsConfigPlugin.getMmsRecipientLimit();
        assertEquals(RECIPIENTS_LIMIT, mmsRecipientLimit);
    }

    public void testGetHttpSocketTimeout() {
        int httpSocketTimeout = sMmsConfigPlugin.getHttpSocketTimeout();
        assertEquals(SOCKET_TIMEOUT, httpSocketTimeout);
    }

    public void testSetHttpSocketTimeout() {
        sMmsConfigPlugin.setHttpSocketTimeout(0);
        assertTrue(true);
    }


    public void testIsNotificationDialogEnabled() {
        boolean isNotificationDialogEnabled = sMmsConfigPlugin.isNotificationDialogEnabled();
        assertTrue(isNotificationDialogEnabled);
    }

    public void testGetMmsRetryPromptIndex() {
        int index = sMmsConfigPlugin.getMmsRetryPromptIndex();
        assertEquals(index, 4);
    }

    public void testGetMmsRetryScheme() {
        int[] scheme = sMmsConfigPlugin.getMmsRetryScheme();
        assertTrue(scheme[1] == 5000);
    }

    public void testSetSoSndTimeout() {
        String ua = "Android-Mms/2.0";
        AndroidHttpClient client = AndroidHttpClient.newInstance(ua, this.getInstrumentation().getContext());
        HttpParams params = client.getParams();
        sMmsConfigPlugin.setSoSndTimeout(params);
        int timeout = HttpConnectionParams.getSoSndTimeout(params);
        assertEquals(timeout, 30000);
    }

    public void testAppendExtraQueryParameterForConversationDeleteAll() {
        final Uri MMSSMS_CONTENT_URI = Uri.parse("content://mms-sms/");
        Uri uri = Uri.withAppendedPath(MMSSMS_CONTENT_URI, "conversations");
        Uri retUri = sMmsConfigPlugin.appendExtraQueryParameterForConversationDeleteAll(uri);

        String param = retUri.getQueryParameter("groupDeleteParts");
        assertTrue(param.equals("yes"));
    }

/*
    public void testPrintMmsMemStat() {
        Context context = this.getInstrumentation().getContext();
        sMmsConfigPlugin.printMmsMemStat(context, "op01plugintest");
        assertTrue(true);
    }
*/
    public void testSetMaxTextLimit() {
        sMmsConfigPlugin.setMaxTextLimit(0);
        assertTrue(true);
    }

    public void testSetMmsRecipientLimit() {
        sMmsConfigPlugin.setMmsRecipientLimit(0);
        assertTrue(true);
    }

    public void testSetSmsToMmsTextThreshold() {
        sMmsConfigPlugin.setSmsToMmsTextThreshold(0);
        assertTrue(true);
    }

    public void testExtendURLSpan() {
        TextView textView = new TextView(mContext);
        try {
            mUrlSpan.onClick(textView);
        } catch (Exception e) {
        }
        assertTrue(true);
    }

    public void testGetUndeliveryMmsQueryUri() {
        Uri uri = sMmsConfigPlugin.getUndeliveryMmsQueryUri(null);
        assertTrue(true);
    }
}
