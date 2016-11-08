package com.mediatek.mms.plugin;

import android.content.Context;
import android.util.Log;

import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.AcknowledgeInd;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.mms.ext.DefaultOpRetrieveTransactionExt;

/**
 * Op01RetrieveTransactionExt.
 *
 */
public class Op01RetrieveTransactionExt extends DefaultOpRetrieveTransactionExt {
    private static final String TAG = "Op01RetrieveTransactionExt";

    /**
     * Construction.
     * @param context Context
     */
    public Op01RetrieveTransactionExt(Context context) {
        super(context);
    }

    @Override
    public void sendAcknowledgeInd(Context context, int subId, AcknowledgeInd acknowledgeInd) {
        // X-Mms-Report-Allowed Optional
        boolean reportAllowed = Op01MmsUtils.isEnableSendDeliveryReport(context, subId);
        Log.d(TAG, "reportAllowed: " + reportAllowed);

        try {
            acknowledgeInd.setReportAllowed(
                                    reportAllowed ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
        } catch (InvalidHeaderValueException ihve) {
            Log.e(TAG, "acknowledgeInd.setReportAllowed Failed !!");
        }
    }
}
