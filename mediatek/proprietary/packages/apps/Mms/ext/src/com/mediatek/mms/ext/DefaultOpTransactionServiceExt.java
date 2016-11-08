package com.mediatek.mms.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.mediatek.mms.callback.ITransactionServiceCallback;

public class DefaultOpTransactionServiceExt extends ContextWrapper implements
        IOpTransactionServiceExt {

    public DefaultOpTransactionServiceExt(Context base) {
        super(base);
    }

    @Override
    public boolean handleTransactionProcessed(
            ITransactionServiceCallback callback, int result,
            Object transaction, Intent intent) {
        return false;
    }

    @Override
    public void init(ITransactionServiceCallback transactionCallback){

    }

    @Override
    public void setTransactionFail(Context context, int failType, Cursor cursor) {

    }

    @Override
    public boolean onNewIntent(Uri pduUri, int failureType) {
        return false;
    }

    @Override
    public void onOpNewIntent(Intent intent) {
    }

    @Override
    public boolean processTransaction(boolean isCancelling) {
        return false;
    }

    @Override
    public void handleOpTransactionProcessed(Intent intent, boolean isReadRecTransaction,
            boolean subDisabled) {
    }

    @Override
    public void cancelTransaction(Context context, Uri uri) {
    }

    @Override
    public boolean cancelandRemoveTransaction() {
        return false;
    }

    @Override
    public void markStateComplete(Uri uri) {

    }

}

