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

import java.io.IOException;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Sent;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.MmsLog;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadRecInd;
import com.google.android.mms.util.SqliteWrapper;
import com.mediatek.setting.MmsPreferenceActivity;

/**
 * The ReadRecTransaction is responsible for sending read report
 * notifications (M-read-rec.ind) to clients that have requested them.
 * It:
 *
 * <ul>
 * <li>Loads the read report indication from storage (Outbox).
 * <li>Packs M-read-rec.ind and sends it.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 */
public class ReadRecTransaction extends Transaction implements Runnable{
    private static final String TAG = LogTag.TAG;
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private Thread mThread;
    private String mMmscUrl;
    private String mReadRecUrl;

    public ReadRecTransaction(Context context,
            int transId,
            String mmscUrl,
            String uri,
            int subId) {
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
        mThread = new Thread(this, "ReadRecTransaction");
        mThread.start();
    }

    public void run() {
        MmsLog.d(MmsApp.TXN_TAG, "ReadRecTransaction: process()");
        // prepare for ReadRec
        int readReportState = 0;
        String messageId = null;
        long msgId = 0;
        EncodedStringValue[] sender = new EncodedStringValue[1];
        Cursor cursor = null;
        cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), mUri,
                new String[] {
                        Mms.MESSAGE_ID, Mms.READ_REPORT, Mms._ID
                }, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    messageId = cursor.getString(0);
                    readReportState = cursor.getInt(1);
                    msgId = cursor.getLong(2);
                }
                // if curosr==null, this means the mms is deleted during
                // processing.
                // exception will happened. catched by out catch clause.
                // so do not catch exception here.
            } finally {
                cursor.close();
            }
        }
        MmsLog.d(MmsApp.TXN_TAG, "messageid:" + messageId + ",and readreport flag:"
                + readReportState + ", mSubId = " + mSubId);

        cursor = null;
        cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                Uri.parse("content://mms/" + msgId + "/addr"), new String[] {
                        Mms.Addr.ADDRESS, Mms.Addr.CHARSET
                }, Mms.Addr.TYPE + " = " + PduHeaders.FROM, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String address = cursor.getString(0);
                    int charSet = cursor.getInt(1);
                    MmsLog.d(MmsApp.TXN_TAG, "find address:" + address + ",charset:" + charSet);
                    sender[0] = new EncodedStringValue(charSet, PduPersister.getBytes(address));
                }
                // if cursor == null exception will catched by out catch clause.
            } finally {
                cursor.close();
            }
        }
        try {
            ReadRecInd readRecInd = new ReadRecInd(new EncodedStringValue(
                    PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes()), messageId.getBytes(),
                    PduHeaders.CURRENT_MMS_VERSION, PduHeaders.READ_STATUS_READ,// always
                                                                                // set
                                                                                // read.
                    sender);
            readRecInd.setDate(System.currentTimeMillis() / 1000);
            Uri uri = PduPersister.getPduPersister(mContext).persist(readRecInd,
                    Mms.Outbox.CONTENT_URI, true,
                    MmsPreferenceActivity.getIsGroupMmsEnabled(mContext), null);
            Intent intent = new Intent(TransactionService.ACTION_TRANSACION_PROCESSED);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubId);
            intent.putExtra(TransactionBundle.URI, mUri.toString());
            PendingIntent sentIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            SmsManager.getSmsManagerForSubscriptionId(mSubId).sendStoredMultimediaMessage(uri,
                    null,
                    sentIntent);
        } catch (InvalidHeaderValueException e) {
            Log.e(TAG, "Invalide header value", e);
            getState().setState(FAILED);
            getState().setContentUri(mUri);
            notifyObservers();
        } catch (MmsException e) {
            Log.e(TAG, "Persist message failed", e);
            getState().setState(FAILED);
            getState().setContentUri(mUri);
            notifyObservers();
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
            getState().setState(FAILED);
            getState().setContentUri(mUri);
            notifyObservers();
        }
    }

    @Override
    public int getType() {
        return READREC_TRANSACTION;
    }

    public String getReadRecUrl() {
        return mReadRecUrl;
    }
}
