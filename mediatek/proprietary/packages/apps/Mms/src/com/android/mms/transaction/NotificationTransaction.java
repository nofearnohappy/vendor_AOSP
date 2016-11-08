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
import static com.android.mms.transaction.TransactionState.INITIALIZED;
import static com.android.mms.transaction.TransactionState.SUCCESS;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
import static com.google.android.mms.pdu.PduHeaders.STATUS_DEFERRED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_RETRIEVED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_UNRECOGNIZED;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.provider.Telephony.Threads;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.mediatek.mms.ext.IOpNotificationTransactionExt;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.MmsLog;
import com.android.mms.util.Recycler;
import com.android.mms.widget.MmsWidgetProvider;
import com.mediatek.setting.MmsPreferenceActivity;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.NotifyRespInd;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
/// M: Add MmsService configure param @{
import com.android.mms.MmsConfig;

/**
 * The NotificationTransaction is responsible for handling multimedia
 * message notifications (M-Notification.ind).  It:
 *
 * <ul>
 * <li>Composes the notification response (M-NotifyResp.ind).
 * <li>Sends the notification response to the MMSC server.
 * <li>Stores the notification indication.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 *
 * NOTE: This MMS client handles all notifications with a <b>deferred
 * retrieval</b> response.  The transaction service, upon succesful
 * completion of this transaction, will trigger a retrieve transaction
 * in case the client is in immediate retrieve mode.
 */
public class NotificationTransaction extends Transaction implements Runnable {
    private static final String TAG = LogTag.TAG;
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;
    private static final String NOTIFY_RESP_NAME = "NotifyResp_noti";
    private static final String RETRIEVE_RESULT_NAME = "RetrieveResult_noti";

    private NotificationInd mNotificationInd;
    private String mContentLocation;

    private IOpNotificationTransactionExt mOpNotificationTransactionExt;

    public NotificationTransaction(
            Context context,
            int serviceId,
            String uriString,
            int subId) {
        super(context, serviceId, subId);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        mContentLocation = new String(mNotificationInd.getContentLocation());
        mId = mContentLocation;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));

        mOpNotificationTransactionExt = OpMessageUtils.getOpMessagePlugin()
                .getOpNotificationTransactionExt();
        mOpNotificationTransactionExt.init(DownloadManager.getInstance());
    }

    /**
     * This constructor is only used for test purposes.
     */
    public NotificationTransaction(
            Context context, int serviceId,
            NotificationInd ind,
            int subId) {
        super(context, serviceId, subId);

        try {
            // Save the pdu. If we can start downloading the real pdu immediately, don't allow
            // persist() to create a thread for the notificationInd because it causes UI jank.
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, Inbox.CONTENT_URI, !allowAutoDownload(context, subId),
                        MmsPreferenceActivity.getIsGroupMmsEnabled(context), null);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        mId = new String(mNotificationInd.getContentLocation());

        mOpNotificationTransactionExt = OpMessageUtils.getOpMessagePlugin()
                .getOpNotificationTransactionExt();
        mOpNotificationTransactionExt.init(DownloadManager.getInstance());
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.mms.pdu.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this, "NotificationTransaction").start();
    }

    public static boolean allowAutoDownload(Context context, int subId) {
        DownloadManager downloadManager = DownloadManager.getInstance();
        boolean autoDownload = downloadManager.isAuto(subId);
        boolean dataSuspended = false;/*(MmsApp.getApplication().getTelephonyManager().getDataState() ==
                TelephonyManager.DATA_SUSPENDED);
                */
        return autoDownload && !dataSuspended;
    }

    public void run() {
    	MmsLog.d(MmsApp.TXN_TAG, "NotificationTransaction: run");
        DownloadManager downloadManager = DownloadManager.getInstance();
        boolean autoDownload = allowAutoDownload(mContext, mSubId);
        try {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Notification transaction launched: " + this);
            }

            // By default, we set status to STATUS_DEFERRED because we
            // should response MMSC with STATUS_DEFERRED when we cannot
            // download a MM immediately.
            int status = STATUS_DEFERRED;
            // Don't try to download when data is suspended, as it will fail, so defer download
            if (!autoDownload) {
                // M: change API for ALPS01889178, use sub id.
                downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED, mSubId);
                sendNotifyRespInd(status);
                getState().setState(SUCCESS);
                getState().setContentUri(mUri);
                notifyObservers();
                return;
            }

            // M: change API for ALPS01889178, use sub id.
            downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING, mSubId);

            if (mOpNotificationTransactionExt.run(
                    mIsCancelling, mUri, mContext, getUri(), mContentLocation)) {
                mTransactionState.setState(TransactionState.SUCCESS);
                mTransactionState.setContentUri(mUri);
                mIsCancelling = false;
                return;
            }

            if (LOCAL_LOGV) {
                Log.v(TAG, "Content-Location: " + mContentLocation);
            }
            File pduFile = createPduFile(null, RETRIEVE_RESULT_NAME + mUri.getLastPathSegment());
            pduFile.setWritable(true, false);
            Intent intent = new Intent(TransactionService.ACTION_TRANSACION_PROCESSED);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubId);
            intent.putExtra(TransactionBundle.URI, mUri.toString());
            PendingIntent downloadedIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(mSubId);
            Log.d(MmsApp.TXN_TAG, "download MMS with param, mContentLocation = " + mContentLocation
                    + ", mUri = " + mUri + ", subId" + mSubId);

            /// M: Add MmsService configure param @{
            manager.downloadMultimediaMessage(mContext, mContentLocation, Uri.fromFile(pduFile),
                    MmsConfig.getMmsServiceConfig(), downloadedIntent);
			/// @}

            // sendNotifyRespInd(status);

            // Make sure this thread isn't over the limits in message count.
            Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
            MmsWidgetProvider.notifyDatasetChanged(mContext);
        } catch (Throwable t) {
            getState().setState(FAILED);
            getState().setContentUri(mUri);
            notifyObservers();
            Log.e(TAG, Log.getStackTraceString(t));
        }
    }

    public void sendNotifyRespInd(int status) {
    	MmsLog.v(MmsApp.TXN_TAG, "NotificationTransaction: sendNotifyRespInd()");
        // Create the M-NotifyResp.ind
        NotifyRespInd notifyRespInd = null;
        try {
            notifyRespInd = new NotifyRespInd(PduHeaders.CURRENT_MMS_VERSION,
                    mNotificationInd.getTransactionId(), status);
        } catch (InvalidHeaderValueException ex) {
            ex.printStackTrace();
            return;
        }

        /// M:Code analyze 014, this paragraph below is using for judging if it is allowed
        /// to send delivery report,at present,we don't support delivery report in MMS @{
        mOpNotificationTransactionExt.sendNotifyRespInd(mContext,mSubId, notifyRespInd);

        byte[] datas = new PduComposer(mContext, notifyRespInd).make();
        File pduFile = createPduFile(datas, NOTIFY_RESP_NAME + mUri.getLastPathSegment());
        if (pduFile == null) {
            return;
        }

        SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(mSubId);
        /*
        Intent intent = new Intent(TransactionService.ACTION_TRANSACION_PROCESSED);
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubId);
        // intent.putExtra(TransactionBundle.URI, mUri.toString());
        PendingIntent sentIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
                */
        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            manager.sendMultimediaMessage(mContext, Uri.fromFile(pduFile), mContentLocation,
                    null, null);
        } else {
            manager.sendMultimediaMessage(mContext, Uri.fromFile(pduFile), null, null, null);
        }

    }

    public int checkPduResult() {
        return checkResultPduValid(RETRIEVE_RESULT_NAME + mUri.getLastPathSegment());
    }

    @Override
    public int getType() {
        return NOTIFICATION_TRANSACTION;
    }
}
