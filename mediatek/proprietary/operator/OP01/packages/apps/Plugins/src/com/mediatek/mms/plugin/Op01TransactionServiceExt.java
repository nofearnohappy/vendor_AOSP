package com.mediatek.mms.plugin;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.mms.callback.ITransactionServiceCallback;
import com.mediatek.mms.ext.DefaultOpTransactionServiceExt;

/**
 * Op01TransactionServiceExt.
 *
 */
public class Op01TransactionServiceExt extends DefaultOpTransactionServiceExt {
    private static final String TAG = "Op01TransactionServiceExt";
    private Context mContext = null;
    private Op01MmsTransaction mMmsTransaction;

    /**
     * Construction.
     * @param context Context
     */
    public Op01TransactionServiceExt(Context context) {
        super(context);
        mContext = context;
        mMmsTransaction = new Op01MmsTransaction(mContext);
    }

    @Override
    public boolean handleTransactionProcessed(ITransactionServiceCallback callback,
            int result, Object transaction, Intent intent) {
        switch (result) {
        case SmsManager.MMS_ERROR_INVALID_APN:
            callback.setTransactionFailCallback(transaction, FAILE_TYPE_TEMPORARY);
            return true;
        case SmsManager.MMS_ERROR_HTTP_FAILURE:
        case SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS:
        case SmsManager.MMS_ERROR_CONFIGURATION_ERROR:
            if (callback.isSendOrRetrieveTransaction(transaction)) {
                int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                boolean incall = isDuringCallForCurrentSim(subId);
                Log.d(TAG, "incall? " + incall);
                if (incall) {
                    callback.setTransactionFailCallback(transaction,
                            FAILE_TYPE_RESTAIN_RETRY);
                } else {
                    callback.setTransactionFailCallback(transaction,
                            FAILE_TYPE_TEMPORARY);
                }
            } else {
                callback.setTransactionFailCallback(transaction,
                        FAILE_TYPE_TEMPORARY);
            }
            return true;
        default:
            break;
        }
        return false;
    }

    @Override
    public void setTransactionFail(Context context, int failType, Cursor cursor) {
        if (FAILE_TYPE_RESTAIN_RETRY == failType) {
            int retryIndex = cursor.getInt(cursor
                    .getColumnIndexOrThrow(PendingMessages.RETRY_INDEX)); // Count this time.
            if (retryIndex > 0) {
                retryIndex--;
            }
            Log.d(TAG, "failType = 3, retryIndex = " + retryIndex);

            ContentValues values = new ContentValues(1);
            values.put(PendingMessages.RETRY_INDEX, retryIndex);
            int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
            long id = cursor.getLong(columnIndex);
            SqliteWrapper.update(context, context.getContentResolver(),
                    PendingMessages.CONTENT_URI, values,
                    PendingMessages._ID + "=" + id, null);
        }
    }

    /* M:Code analyze 004,add for ALPS00081452,check whether the request data connection fail
     * is caused by calling going on. @{
     */
    private boolean isDuringCallForCurrentSim(int subId) {
        TelephonyManager teleManager =
                                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int phoneState = TelephonyManager.CALL_STATE_IDLE;
        if (teleManager != null) {
            phoneState = teleManager.getCallState(subId);
        }
        return phoneState != TelephonyManager.CALL_STATE_IDLE;
    }
    /// @}

    @Override
    public boolean onNewIntent(Uri pduUri, int failureType) {
        return !(new Op01MmsTransaction(mContext).isPendingMmsNeedRestart(pduUri, failureType));
    }

    @Override
    public void handleOpTransactionProcessed(Intent intent, boolean isReadRecTransaction,
            boolean subDisabled) {
        mMmsTransaction.updateConnection();
    }
}
