package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable.Callback;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.mms.callback.ITransactionServiceCallback;
import com.mediatek.mms.ext.DefaultOpTransactionServiceExt;

public class Op09TransactionServiceExt extends DefaultOpTransactionServiceExt {

    private static String TAG = "Op09TransactionServiceExt";

    public static final String ACTION_ONALARM = "android.intent.action.ACTION_ONALARM";

    public static final String ACTION_ENABLE_AUTO_RETRIEVE =
                                        "android.intent.action.ACTION_ENABLE_AUTO_RETRIEVE";

    int STATE_COMPLETE = 0x03;

    int CANCEL_DOWNLOAD = 5;

    private static  String sTransactionBundleUri = "bundle_uri";

    Op09MmsCancelDownloadExt mCancelDownloadExt = null;

    public Op09TransactionServiceExt(Context base) {
        super(base);
        mCancelDownloadExt = Op09MmsCancelDownloadExt.getIntance(base);
    }

    public Op09TransactionServiceExt(Context base, ITransactionServiceCallback cdh) {
        super(base);
        mCancelDownloadExt = new Op09MmsCancelDownloadExt(base, cdh);
        // TODO Auto-generated constructor stub
    }

    public void init(ITransactionServiceCallback host) {
        Log.d(TAG, "Op09TransactionServiceExt host: "+host +" mCancelDownloadExt: " +
                          mCancelDownloadExt);
        if(mCancelDownloadExt != null) {
            mCancelDownloadExt.setHostCallback(host);
        }
    }
    @Override
    public void onOpNewIntent(Intent intent) {
        String action = intent.getAction();
        int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        boolean isValidSubscriptionId = SubscriptionManager.isValidSubscriptionId(subId);
        if (ACTION_ONALARM.equals(action) || ACTION_ENABLE_AUTO_RETRIEVE.equals(action)
                || (intent.getExtras() == null)) {
        } else {
            if (!isValidSubscriptionId) {
                /// M: For OP09: Cancel mms download
                mCancelDownloadExt.markStateExt(
                        Uri.parse(intent.getStringExtra(sTransactionBundleUri)), STATE_COMPLETE);
            }
        }
    }

    @Override
    public boolean processTransaction(boolean isCancelling) {
        /// M: For OP09, check if cancel download requested. @{
        if (MessageUtils.isCancelDownloadEnable() && isCancelling) {
            Log.d(TAG, "***Canceling download in processTransaction!");
            return true;
        }
        /// @}
        return false;
    }

    @Override
    public boolean handleTransactionProcessed(ITransactionServiceCallback callback, int result,
            Object transaction, Intent intent) {
        String uri = intent.getStringExtra(sTransactionBundleUri);
        // / M: For OP09: Cancel mms download
        mCancelDownloadExt.markStateExt(Uri.parse(uri), STATE_COMPLETE);
        return false;
    }

    @Override
    public void handleOpTransactionProcessed(Intent intent, boolean isReadRecTransaction,
                            boolean subDisabled) {
        String uri = intent.getStringExtra(sTransactionBundleUri);
        int result = intent.getIntExtra("result", SmsManager.MMS_ERROR_UNSPECIFIED);
        if (result == Activity.RESULT_OK || isReadRecTransaction) {
        } else if (subDisabled) {
            /// M: For OP09: Cancel mms download
            mCancelDownloadExt.markStateExt(Uri.parse(uri), STATE_COMPLETE);
        } else {
            /// M: For OP09: Cancel mms download
            mCancelDownloadExt.markStateExt(Uri.parse(uri), STATE_COMPLETE);
        }
    }

    @Override
    public void cancelTransaction(Context context, Uri uri) {
//        Op09MmsServiceFailedNotifyExt notifyExt = new Op09MmsServiceFailedNotifyExt(context);
//        notifyExt.popupToast(CANCEL_DOWNLOAD, null);

        mCancelDownloadExt.markStateExt(uri, STATE_COMPLETE);
    }

    @Override
    public boolean cancelandRemoveTransaction() {
        return mCancelDownloadExt.getWaitingDataCnxn();
    }

    @Override
    public void markStateComplete(Uri uri) {
        mCancelDownloadExt.markStateExt(uri, STATE_COMPLETE);
    }
}
