package com.mediatek.mms.ext;

import android.content.Context;
import android.net.Uri;

import com.google.android.mms.pdu.AcknowledgeInd;

import com.mediatek.mms.callback.IDownloadManagerCallback;

public interface IOpRetrieveTransactionExt {
    /**
     * @internal
     */
    void sendAcknowledgeInd(Context context, int subId, AcknowledgeInd acknowledgeInd);
    /**
     * @internal
     */
    boolean run(boolean isCancelling, Uri uri, Context context, Uri trxnUri, String contentLocation);
    /**
     * @internal
     */
    void init(IDownloadManagerCallback downloadManager);
}
