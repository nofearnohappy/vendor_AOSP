/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.transaction;

import static com.android.mms.transaction.TransactionState.FAILED;

import java.util.Arrays;

import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Sent;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.RateController;
import com.android.mms.util.SendingProgressTokenManager;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendConf;
import com.google.android.mms.pdu.SendReq;
/// M: Add MmsService configure param @{
import com.android.mms.MmsConfig;

import java.util.Arrays;

/**
 * The SendTransaction is responsible for sending multimedia messages
 * (M-Send.req) to the MMSC server.  It:
 *
 * <ul>
 * <li>Loads the multimedia message from storage (Outbox).
 * <li>Packs M-Send.req and sends it.
 * <li>Retrieves confirmation data from the server  (M-Send.conf).
 * <li>Parses confirmation message and handles it.
 * <li>Moves sent multimedia message from Outbox to Sent.
 * <li>Notifies the TransactionService about successful completion.
 * </ul>
 */
public class SendTransaction extends Transaction implements Runnable {
    private static final String TAG = LogTag.TAG;

    private Thread mThread;
    private String mMmscUrl;

    public SendTransaction(Context context, int transId, String mmscUrl, String uri, int subId) {
        super(context, transId, subId);
        mUri = Uri.parse(uri);
        mMmscUrl = mmscUrl;
        mId = uri;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.Transaction#process()
     */
    @Override
    public void process() {
        mThread = new Thread(this, "SendTransaction");
        mThread.start();
    }

    public void run() {
    	Log.d(MmsApp.TXN_TAG, "SendTransaction: run");
        RateController rateCtlr = RateController.getInstance();
        if (rateCtlr.isLimitSurpassed() && !rateCtlr.isAllowedByUser()) {
            Log.e(TAG, "Sending rate limit surpassed.");
            return;
        }

        try {
            Intent intent = new Intent(TransactionService.ACTION_TRANSACION_PROCESSED);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubId);
            intent.putExtra(TransactionBundle.URI, mUri.toString());
            PendingIntent sentIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Log.d(MmsApp.TXN_TAG, "send MMS with param, mUri = " + mUri + ", subId = " + mSubId);
            /// M: Add MmsService configure param @{
            SmsManager.getSmsManagerForSubscriptionId(mSubId).sendStoredMultimediaMessage(mUri,
                    MmsConfig.getMmsServiceConfig(),
                    sentIntent);
	    /// @}
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
            getState().setState(FAILED);
            getState().setContentUri(mUri);
            notifyObservers();
        }
    }

    @Override
    public int getType() {
        return SEND_TRANSACTION;
    }
}
