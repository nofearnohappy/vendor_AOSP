package com.mediatek.mms.plugin;

import android.content.Context;
import android.util.Log;

import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.NotifyRespInd;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.mms.ext.DefaultOpNotificationTransactionExt;

/**
 * Op01NotificationTransactionExt.
 *
 */
public class Op01NotificationTransactionExt extends
        DefaultOpNotificationTransactionExt {
    private static final String TAG = "Op01NotificationTransactionExt";

    /**
     * Construction.
     * @param context Context.
     */
    public Op01NotificationTransactionExt(Context context) {
        super(context);
    }

    @Override
    public void sendNotifyRespInd(Context context, int subId, NotifyRespInd notifyRespInd) {
        // X-Mms-Report-Allowed Optional
        boolean reportAllowed = Op01MmsUtils.isEnableSendDeliveryReport(context, subId);
        Log.d(TAG, "reportAllowed: " + reportAllowed);

        try {
            notifyRespInd.setReportAllowed(reportAllowed ? PduHeaders.VALUE_YES
                    : PduHeaders.VALUE_NO);
        } catch (InvalidHeaderValueException ihve) {
            // do nothing here
            Log.e(TAG, "notifyRespInd.setReportAllowed Failed !!");
        }
    }

}
