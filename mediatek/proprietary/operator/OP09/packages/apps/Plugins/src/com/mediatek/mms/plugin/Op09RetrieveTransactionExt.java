package com.mediatek.mms.plugin;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mms.callback.IDownloadManagerCallback;
import com.mediatek.mms.ext.DefaultOpRetrieveTransactionExt;

public class Op09RetrieveTransactionExt extends DefaultOpRetrieveTransactionExt {
    IDownloadManagerCallback mHostCallback;
    public static final int STATE_UNSTARTED = 0x80;

    private static String TAG = "Op09RetrieveTransactionExt";
    int CANCEL_DOWNLOAD = 5;

    public Op09RetrieveTransactionExt(Context base) {
        super(base);
    }

    public void init(IDownloadManagerCallback host) {
        mHostCallback = host;
    }

    @Override
    public boolean run(boolean isCancelling, Uri uri, Context context, Uri trxnUri,
            String contentLocation) {
        /// M: For OP09, check if cancel download requested.@{
        if (MessageUtils.isCancelDownloadEnable() && isCancelling) {
            Log.d(TAG, "***Canceling download in processing!(RetrieveTransaction)");

            if (MessageUtils.isCancelDownloadEnable()) {
                // Op09MmsFailedNotify.getIntance(context).popupToast(CANCEL_DOWNLOAD, null);
            }
            Op09MmsCancelDownloadExt.getIntance(context).markStateExt(trxnUri,
                                                    Op09MmsCancelDownloadExt.STATE_COMPLETE);
            mHostCallback.markStateCallback(trxnUri, STATE_UNSTARTED);
            return true;
        }
        /// @}

        /// OP09 MMS Feature: cancel download Mms @{
        if (MessageUtils.isCancelDownloadEnable()) {
            Op09MmsCancelDownloadExt.getIntance(context).addHttpClient(contentLocation, uri);
        }
        /// @}
        return false;
    }

}
