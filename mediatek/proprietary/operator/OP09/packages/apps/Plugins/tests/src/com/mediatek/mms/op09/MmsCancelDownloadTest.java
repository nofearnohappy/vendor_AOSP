package com.mediatek.mms.op09;

import android.content.ContentValues;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.provider.Telephony.Mms;

import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.common.MPlugin;
import com.mediatek.mms.ext.IMmsCancelDownloadExt;
import com.mediatek.mms.op09.Op09MmsPluginTestRunner.BasicCase;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

public class MmsCancelDownloadTest extends BasicCase {
    private static IMmsCancelDownloadExt sCancelDownload;
    private static final String CONTENT_LOCATION = "http://10.233.3.74:80/3Z0p-0";
    private AndroidHttpClient mClient;
    private Uri mNotificationUri;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sCancelDownload = MPlugin.createInstance("com.mediatek.mms.ext.IMmsCancelDownload", mContext);
    }

    public void test001CancelDownload() throws Exception {
        // Clear the pdu table.
        SqliteWrapper.delete(mContext, mContext.getContentResolver(), Mms.CONTENT_URI, null, null);

        // Insert a Notification.Ind in database.
        ContentValues values = new ContentValues();
        values.put(Mms.THREAD_ID, NEW_THREAD_ID);
        values.put(Mms.CONTENT_LOCATION, CONTENT_LOCATION);
        values.put(Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
        values.put(Mms.STATUS_EXT, IMmsCancelDownloadExt.STATE_DOWNLOADING);
        mNotificationUri = SqliteWrapper.insert(mContext, mContext.getContentResolver(), Mms.CONTENT_URI, values);

        mClient = AndroidHttpClient.newInstance("Android-Mms/0.1", mContext);
        DefaultHttpRequestRetryHandler httpRequestRetryHandler = new DefaultHttpRequestRetryHandler(1, true);

        sCancelDownload.saveDefaultHttpRetryHandler(httpRequestRetryHandler);
        sCancelDownload.addHttpClient(CONTENT_LOCATION, mClient);
        sCancelDownload.cancelDownload(mNotificationUri);
        delay(DELAY_TIME);
        sCancelDownload.removeHttpClient(CONTENT_LOCATION);
        assertEquals(IMmsCancelDownloadExt.STATE_ABORTED, sCancelDownload.getStateExt(CONTENT_LOCATION));
        assertEquals(IMmsCancelDownloadExt.STATE_ABORTED, sCancelDownload.getStateExt(mNotificationUri));
    }

    public void test002OtherAPIs() {
        sCancelDownload.setCancelToastEnabled(true);
        assertTrue(sCancelDownload.getCancelToastEnabled());

        sCancelDownload.setWaitingDataCnxn(true);
        assertTrue(sCancelDownload.getWaitingDataCnxn());
    }
}
