package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;

import com.google.android.mms.pdu.AcknowledgeInd;

import com.mediatek.mms.callback.IDownloadManagerCallback;

public class DefaultOpRetrieveTransactionExt extends ContextWrapper implements
        IOpRetrieveTransactionExt {

    public DefaultOpRetrieveTransactionExt(Context base) {
        super(base);
    }

    @Override
    public void sendAcknowledgeInd(Context context, int subId,
            AcknowledgeInd acknowledgeInd) {

    }
    
    @Override
    public boolean run(boolean isCancelling, Uri uri, Context context, Uri trxnUri,
            String contentLocation) {
        return false;
    }

    @Override
    public void init(IDownloadManagerCallback downloadManager) {

    }
}
