package com.mediatek.mms.ext;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.mediatek.mms.callback.ITransactionServiceCallback;

public interface IOpTransactionServiceExt {
    static final int FAILE_TYPE_PERMANENT = 1;
    static final int FAILE_TYPE_TEMPORARY = 2;
    static final int FAILE_TYPE_RESTAIN_RETRY = 3;

    /**
     * @internal
     */
    boolean handleTransactionProcessed(ITransactionServiceCallback callback,
            int result, Object transaction, Intent intent);

    /**
     * @internal
     */
    void setTransactionFail(Context context, int failType, Cursor cursor);

    /**
     * @internal
     */
    boolean onNewIntent(Uri pduUri, int failureType);

    /**
     * @internal
     */
    void onOpNewIntent(Intent intent);

    /**
     * @internal
     */
    boolean processTransaction(boolean isCancelling);

    /**
     * @internal
     */
    void handleOpTransactionProcessed(Intent intent, boolean isReadRecTransaction,
            boolean subDisabled);

    /**
     * @internal
     */
    void cancelTransaction(Context context, Uri uri);

    /**
     * @internal
     */
    boolean cancelandRemoveTransaction();

    /**
     * @internal
     */
    void markStateComplete(Uri uri);

    /**
     * @internal
     */
    void init(ITransactionServiceCallback transactionCallback);
}
