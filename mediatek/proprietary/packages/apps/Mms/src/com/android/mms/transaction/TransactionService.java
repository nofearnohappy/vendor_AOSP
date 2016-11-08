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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Mms.Sent;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.mediatek.internal.telephony.CellConnMgr;
import com.mediatek.mms.callback.ITransactionServiceCallback;
import com.mediatek.mms.ext.IOpTransactionServiceExt;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.MmsLog;
import com.android.mms.util.RateController;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.mms.util.MmsDialogNotifyUtils;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.setting.NotificationPreferenceActivity;

/**
 * The TransactionService of the MMS Client is responsible for handling requests
 * to initiate client-transactions sent from:
 * <ul>
 * <li>The Proxy-Relay (Through Push messages)</li>
 * <li>The composer/viewer activities of the MMS Client (Through intents)</li>
 * </ul>
 * The TransactionService runs locally in the same process as the application.
 * It contains a HandlerThread to which messages are posted from the
 * intent-receivers of this application.
 * <p/>
 * <b>IMPORTANT</b>: This is currently the only instance in the system in
 * which simultaneous connectivity to both the mobile data network and
 * a Wi-Fi network is allowed. This makes the code for handling network
 * connectivity somewhat different than it is in other applications. In
 * particular, we want to be able to send or receive MMS messages when
 * a Wi-Fi connection is active (which implies that there is no connection
 * to the mobile data network). This has two main consequences:
 * <ul>
 * <li>Testing for current network connectivity ({@link android.net.NetworkInfo#isConnected()} is
 * not sufficient. Instead, the correct test is for network availability
 * ({@link android.net.NetworkInfo#isAvailable()}).</li>
 * <li>If the mobile data network is not in the connected state, but it is available,
 * we must initiate setup of the mobile data connection, and defer handling
 * the MMS transaction until the connection is established.</li>
 * </ul>
 */
public class TransactionService extends Service implements Observer, ITransactionServiceCallback {
    private static final String TAG = LogTag.TAG;

    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is completed.
     */
    public static final String TRANSACTION_COMPLETED_ACTION =
            "android.intent.action.TRANSACTION_COMPLETED_ACTION";

    /**
     * Action for the Intent which is sent by Alarm service to launch
     * TransactionService.
     */
    public static final String ACTION_ONALARM = "android.intent.action.ACTION_ONALARM";

    /**
     * Action for the Intent which is sent when the user turns on the auto-retrieve setting.
     * This service gets started to auto-retrieve any undownloaded messages.
     */
    public static final String ACTION_ENABLE_AUTO_RETRIEVE
            = "android.intent.action.ACTION_ENABLE_AUTO_RETRIEVE";

    /**
     * Action for Mms Service process a transaction finished. The action will be set into
     * a transaction request. After sending/downloading finished, Mms service will send it back.
     */
    public static final String ACTION_TRANSACION_PROCESSED
            = "com.android.mms.transaction.TRANSACION_PROCESSED";

    /**
     * Used as extra key in notification intents broadcasted by the TransactionService
     * when a Transaction is completed (TRANSACTION_COMPLETED_ACTION intents).
     * Allowed values for this key are: TransactionState.INITIALIZED,
     * TransactionState.SUCCESS, TransactionState.FAILED.
     */
    public static final String STATE = "state";

    /**
     * Used as extra key in notification intents broadcasted by the TransactionService
     * when a Transaction is completed (TRANSACTION_COMPLETED_ACTION intents).
     * Allowed values for this key are any valid content uri.
     */
    public static final String STATE_URI = "uri";

    private static final int EVENT_TRANSACTION_REQUEST = 1;
    private static final int EVENT_TRANSACTION_PROCESSED = 2;
    private static final int EVENT_HANDLE_NEXT_PENDING_TRANSACTION = 4;
    private static final int EVENT_NEW_INTENT = 5;
    private static final int EVENT_PROCESS_TIME_OUT = 6;
    private static final int EVENT_QUIT = 100;

    private static final int TOAST_MSG_QUEUED = 1;
    private static final int TOAST_DOWNLOAD_LATER = 2;
    private static final int TOAST_NO_APN = 3;
    private static final int TOAST_NONE = -1;

    // How often to extend the use of the MMS APN while a transaction
    // is still being processed.
    private static final int APN_EXTENSION_WAIT = 30 * 1000;
    private static final long TRANSACTION_PROCESS_TIME_OUT = 10L * 60 * 1000;

    // Timeout used to call ConnectivityManager.requestNetwork
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 3 * 60 * 1000;

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private final ArrayList<Transaction> mProcessing  = new ArrayList<Transaction>();
    private final ArrayList<Transaction> mPending  = new ArrayList<Transaction>();
    private ConnectivityManager mConnMgr;

    private PowerManager.WakeLock mWakeLock;
    private static final int FAILE_TYPE_PERMANENT = 1;
    private static final int FAILE_TYPE_TEMPORARY = 2;

    private IOpTransactionServiceExt mOpTransactionServiceExt;

    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = null;

            if (msg.what == TOAST_MSG_QUEUED) {
                str = getString(R.string.message_queued);
            } else if (msg.what == TOAST_DOWNLOAD_LATER) {
                str = getString(R.string.download_later);
            } else if (msg.what == TOAST_NO_APN) {
                str = getString(R.string.no_apn);
            }

            if (str != null) {
                Toast.makeText(TransactionService.this, str,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate() {
        MmsLog.d(MmsApp.TXN_TAG, "Creating TransactionService");
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("TransactionService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mOpTransactionServiceExt = OpMessageUtils.getOpMessagePlugin()
                .getOpTransactionServiceExt();
        mOpTransactionServiceExt.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Message msg = null;
            MmsLog.d(MmsApp.TXN_TAG, "onStartCommand action = " + intent.getAction());
            if (ACTION_TRANSACION_PROCESSED.equals(intent.getAction())) {
                if (mServiceHandler.hasMessages(EVENT_PROCESS_TIME_OUT)) {
                    mServiceHandler.removeMessages(EVENT_PROCESS_TIME_OUT);
                }
                msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_PROCESSED);
            } else {
                msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
            }
            msg.arg1 = startId;
            msg.obj = intent;
            mServiceHandler.sendMessage(msg);
        }
        return Service.START_NOT_STICKY;
    }

    public void onNewIntent(Intent intent, int serviceId) {
        if (!MmsConfig.isSmsEnabled(this)) {
            Log.d(TAG, "TransactionService: is not the default sms app");
            stopSelf(serviceId);
            return;
        }

        if (!PermissionCheckUtil.checkRequiredPermissions(this)) {
            MmsLog.v(MmsApp.TXN_TAG, "onNewIntent no permissions: serviceId: "
                    + serviceId + ": " + intent.getExtras() + " intent=" + intent);
            return;
        }

        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnMgr == null //|| !SubStatusResolver.isMobileDataEnabledOnAnySub(getApplicationContext())
                || !MmsConfig.isSmsEnabled(getApplicationContext())) {
            stopSelf(serviceId);
            Log.e(TAG, "onNewIntent(), not support SMS");
            return;
        }
        NetworkInfo ni = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
        boolean noNetwork = false; // ni == null || !ni.isAvailable();

        MmsLog.v(MmsApp.TXN_TAG, "onNewIntent: serviceId: " + serviceId + ": " + intent.getExtras()
                +
                    " intent=" + intent);
        MmsLog.v(MmsApp.TXN_TAG, "    networkAvailable=" + !noNetwork);

        String action = intent.getAction();
        if (ACTION_ONALARM.equals(action) || ACTION_ENABLE_AUTO_RETRIEVE.equals(action) ||
                (intent.getExtras() == null)) {
            // Scan database to find all pending operations.
            Cursor cursor = PduPersister.getPduPersister(this).getPendingMessages(
                    System.currentTimeMillis());
            if (cursor != null) {
                try {
                    int count = cursor.getCount();

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "onNewIntent: cursor.count=" + count + " action=" + action);
                    }

                    if (count == 0) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "onNewIntent: no pending messages. Stopping service.");
                        }
                        RetryScheduler.setRetryAlarm(this);
                        stopSelfIfIdle(serviceId);
                        return;
                    }

                    int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
                    int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_TYPE);
                    int columnIndexOfSubIndex = cursor
                            .getColumnIndexOrThrow(PendingMessages.SUBSCRIPTION_ID);

                    while (cursor.moveToNext()) {
                        int msgType = cursor.getInt(columnIndexOfMsgType);
                        int transactionType = getTransactionType(msgType);
                        int subId = cursor.getInt(columnIndexOfSubIndex);
                        MmsLog.d(MmsApp.TXN_TAG, "onNewIntent subId = " + subId);
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "onNewIntent: msgType=" + msgType + " transactionType=" +
                                    transactionType);
                        }
                        if (noNetwork) {
                            onNetworkUnavailable(serviceId, transactionType);
                            return;
                        }
                        switch (transactionType) {
                            case -1:
                                break;
                            case Transaction.RETRIEVE_TRANSACTION:
                                // If it's a transiently failed transaction,
                                // we should retry it in spite of current
                                // downloading mode. If the user just turned on the auto-retrieve
                                // option, we also retry those messages that don't have any errors.
                                int failureType = cursor.getInt(
                                        cursor.getColumnIndexOrThrow(
                                                PendingMessages.ERROR_TYPE));
                                DownloadManager downloadManager = DownloadManager.getInstance();
                                boolean autoDownload = downloadManager.isAuto(subId);
                                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                    Log.v(TAG, "onNewIntent: failureType=" + failureType +
                                            " action=" + action + " isTransientFailure:" +
                                            isTransientFailure(failureType) + " autoDownload=" +
                                            autoDownload + "for subId " + subId);
                                }
                                if (!autoDownload) {
                                    // If autodownload is turned off, don't process the
                                    // transaction.
                                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                        Log.v(TAG, "onNewIntent: skipping - autodownload off");
                                    }
                                    // Re-enable "download" button if auto-download is off
                                    Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI,
                                            cursor.getLong(columnIndexOfMsgId));
                                    int mmsStatus = downloadManager.getState(uri);
                                    Log.v(TAG, "onNewIntent: downloadManager.getState  = " + mmsStatus);
                                    if (mmsStatus != DownloadManager.STATE_DOWNLOADING) {
                                        downloadManager.markState(uri,
                                                DownloadManager.STATE_SKIP_RETRYING);
                                    }
                                    break;
                                }
                                // Logic is twisty. If there's no failure or the failure
                                // is a non-permanent failure, we want to process the transaction.
                                // Otherwise, break out and skip processing this transaction.
                                if (!(failureType == MmsSms.NO_ERROR ||
                                        isTransientFailure(failureType))) {
                                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                        Log.v(TAG, "onNewIntent: skipping - permanent error");
                                    }
                                    break;
                                }
                                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                    Log.v(TAG, "onNewIntent: falling through and processing");
                                }
                                
                                /// M: ALPS00545779, for FT, restart pending receiving mms @ {
                                if (mOpTransactionServiceExt.onNewIntent(
                                        ContentUris.withAppendedId(Mms.CONTENT_URI,
                                            cursor.getLong(columnIndexOfMsgId)), failureType)) {
                                    MmsLog.d(MmsApp.TXN_TAG, cursor.getLong(columnIndexOfMsgId)
                                            + "this RETRIEVE not transient failure");
                                    break;
                                } else if (!isTransientFailure(failureType)) {
                                    MmsLog.d(MmsApp.TXN_TAG, cursor.getLong(columnIndexOfMsgId)
                                            + "this RETRIEVE not transient failure");
                                    break;
                                }
                                /// @}

                               // fall-through
                            default:
                                Uri uri = ContentUris.withAppendedId(
                                        Mms.CONTENT_URI,
                                        cursor.getLong(columnIndexOfMsgId));
                                TransactionBundle args = new TransactionBundle(
                                        transactionType, uri.toString());
                                //We use the same startId for all MMs.
                                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                    Log.v(TAG, "onNewIntent: launchTransaction uri=" + uri);
                                }
                                if (!SubscriptionManager.isValidSubscriptionId(subId) || (subId == 0)) {
                                    MmsLog.e(MmsApp.TXN_TAG, "onNewIntent invalid subId = " + subId);
                                } else {
                                    launchTransaction(serviceId, args, false, subId);
                                }
                                break;
                        }
                    }
                } finally {
                    cursor.close();
                }
            } else {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "onNewIntent: no pending messages. Stopping service.");
                }
                RetryScheduler.setRetryAlarm(this);
                stopSelfIfIdle(serviceId);
            }
        } else {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "onNewIntent: launch transaction...");
            }
            // For launching NotificationTransaction and test purpose.
            TransactionBundle args = new TransactionBundle(intent.getIntExtra(
                    TransactionBundle.TRANSACTION_TYPE, Transaction.READREC_TRANSACTION),
                    intent.getStringExtra(TransactionBundle.URI));
            int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            MmsLog.d(MmsApp.TXN_TAG, "onNewIntent ACTION else subId = " + subId);
            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                MmsLog.e(MmsApp.TXN_TAG, "onNewIntent subId error, " + args.toString()
                        + ", subId = " + subId);
            } else {
                launchTransaction(serviceId, args, noNetwork, subId);
            }
        }

        mOpTransactionServiceExt.onOpNewIntent(intent);
    }

    private void stopSelfIfIdle(int startId) {
        synchronized (mProcessing) {
            if (mProcessing.isEmpty() && mPending.isEmpty()) {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "stopSelfIfIdle: STOP!");
                }

                stopSelf(startId);
            }
        }
    }

    private static boolean isTransientFailure(int type) {
        return type > MmsSms.NO_ERROR && type < MmsSms.ERR_TYPE_GENERIC_PERMANENT;
    }

    private int getTransactionType(int msgType) {
        switch (msgType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                return Transaction.RETRIEVE_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
                return Transaction.READREC_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                return Transaction.SEND_TRANSACTION;
            default:
                Log.w(TAG, "Unrecognized MESSAGE_TYPE: " + msgType);
                return -1;
        }
    }

    private void launchTransaction(int serviceId, TransactionBundle txnBundle, boolean noNetwork,
            int subId) {
        if (noNetwork) {
            Log.w(TAG, "launchTransaction: no network error!");
            onNetworkUnavailable(serviceId, txnBundle.getTransactionType());
            return;
        }
        Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
        msg.arg1 = serviceId;
        MmsLog.d(MmsApp.TXN_TAG, "launchTransaction subId = " + subId);
        if (subId <= Long.MAX_VALUE) {
            msg.arg2 = (int) subId & 0xFFFFFFFF;
        } else {
            Log.e(TAG, "launchTransaction: subId is too large, impossible!!!");
        }
        msg.obj = txnBundle;

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "launchTransaction: sending message " + msg);
        }
        mServiceHandler.sendMessage(msg);
    }

    private void onNetworkUnavailable(int serviceId, int transactionType) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "onNetworkUnavailable: sid=" + serviceId + ", type=" + transactionType);
        }

        int toastType = TOAST_NONE;
        if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
            toastType = TOAST_DOWNLOAD_LATER;
        } else if (transactionType == Transaction.SEND_TRANSACTION) {
            toastType = TOAST_MSG_QUEUED;
        }
        if (toastType != TOAST_NONE) {
            mToastHandler.sendEmptyMessage(toastType);
        }
        stopSelf(serviceId);
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(LogTag.TRANSACTION, "Destroying TransactionService");
        }
        if (!mPending.isEmpty()) {
            Log.w(TAG, "TransactionService exiting with transaction still pending");
        }

        releaseWakeLock();

        mServiceHandler.sendEmptyMessage(EVENT_QUIT);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handle status change of Transaction (The Observable).
     */
    public void update(Observable observable) {
        Transaction transaction = (Transaction) observable;
        int serviceId = transaction.getServiceId();

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "update transaction " + serviceId);
        }

        try {
            synchronized (mProcessing) {
                mProcessing.remove(transaction);
                if (mPending.size() > 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "update: handle next pending transaction...");
                    }
                    Message msg = mServiceHandler.obtainMessage(
                            EVENT_HANDLE_NEXT_PENDING_TRANSACTION);
                    mServiceHandler.sendMessage(msg);
                }
            }

            Intent intent = new Intent(TRANSACTION_COMPLETED_ACTION);
            TransactionState state = transaction.getState();
            int result = state.getState();
            intent.putExtra(STATE, result);

            switch (result) {
                case TransactionState.SUCCESS:
                	MmsLog.d(MmsApp.TXN_TAG, "update: result=SUCCESS");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction complete: " + serviceId);
                    }

                    intent.putExtra(STATE_URI, state.getContentUri());

                    // Notify user in the system-wide notification area.
                    switch (transaction.getType()) {
                        case Transaction.NOTIFICATION_TRANSACTION:
                        case Transaction.RETRIEVE_TRANSACTION:
                            // We're already in a non-UI thread called from
                            // NotificationTransacation.run(), so ok to block here.
                            long threadId = MessagingNotification.getThreadId(
                                    this, state.getContentUri());
                            MessagingNotification.blockingUpdateNewMessageIndicator(this,
                                    threadId,
                                    false,
                                    null);
                            MessagingNotification.updateDownloadFailedNotification(this);
                            if (NotificationPreferenceActivity.isPopupNotificationEnable()) {
                                new MmsDialogNotifyUtils(this)
                                        .notifyNewSmsDialog(state.getContentUri());
                            }
                            MmsWidgetProvider.notifyDatasetChanged(this);
                            break;
                        case Transaction.SEND_TRANSACTION:
                            RateController.getInstance().update();
                            break;
                    }
                    break;
                case TransactionState.FAILED:
                	MmsLog.d(MmsApp.TXN_TAG, "update: result=FAILED");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction failed: " + serviceId);
                    }
                    break;
                default:
                	MmsLog.d(MmsApp.TXN_TAG, "update: result=default");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction state unknown: " +
                                serviceId + " " + result);
                    }
                    break;
            }

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "update: broadcast transaction result " + result);
            }
            // Broadcast the result of the transaction.
            sendBroadcast(intent);
        } finally {
            transaction.detach(this);
            stopSelfIfIdle(serviceId);
            String uri = getTransactionId(transaction);
            removeTransactionById(uri);
        }
    }

    private synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
            mWakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        Log.v(TAG, "mms acquireWakeLock");
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            Log.v(TAG, "mms releaseWakeLock");
            mWakeLock.release();
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        private String decodeMessage(Message msg) {
            if (msg.what == EVENT_QUIT) {
                return "EVENT_QUIT";
            } else if (msg.what == EVENT_TRANSACTION_PROCESSED) {
                return "EVENT_TRANSACTION_PROCESSED";
            } else if (msg.what == EVENT_TRANSACTION_REQUEST) {
                return "EVENT_TRANSACTION_REQUEST";
            } else if (msg.what == EVENT_HANDLE_NEXT_PENDING_TRANSACTION) {
                return "EVENT_HANDLE_NEXT_PENDING_TRANSACTION";
            } else if (msg.what == EVENT_NEW_INTENT) {
                return "EVENT_NEW_INTENT";
            }
            return "unknown message.what";
        }

        private String decodeTransactionType(int transactionType) {
            if (transactionType == Transaction.NOTIFICATION_TRANSACTION) {
                return "NOTIFICATION_TRANSACTION";
            } else if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
                return "RETRIEVE_TRANSACTION";
            } else if (transactionType == Transaction.SEND_TRANSACTION) {
                return "SEND_TRANSACTION";
            } else if (transactionType == Transaction.READREC_TRANSACTION) {
                return "READREC_TRANSACTION";
            }
            return "invalid transaction type";
        }

        /**
         * Handle incoming transaction requests.
         * The incoming requests are initiated by the MMSC Server or by the
         * MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "Handling incoming message: " + msg + " = " + decodeMessage(msg));
            }

            Transaction transaction = null;

            switch (msg.what) {
                case EVENT_NEW_INTENT:
                    onNewIntent((Intent)msg.obj, msg.arg1);
                    break;

                case EVENT_QUIT:
                    getLooper().quit();
                    return;

                case EVENT_TRANSACTION_PROCESSED:
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_TRANSACTION_PROCESSED");
                    handleTransactionProcessed((Intent) msg.obj, msg.arg1);
                    break;

                case EVENT_PROCESS_TIME_OUT:
                    handleTransactionTimeout((String)msg.obj);

                case EVENT_TRANSACTION_REQUEST:
                	MmsLog.d(MmsApp.TXN_TAG, "EVENT_TRANSACTION_REQUEST");
                    int serviceId = msg.arg1;
                    int subId = msg.arg2;
                    try {
                        TransactionBundle args = (TransactionBundle) msg.obj;
                        MmsLog.d(MmsApp.TXN_TAG, "EVENT_TRANSACTION_REQUEST MmscUrl=" +
                                args.getMmscUrl() + " proxy port: " + args.getProxyAddress() +
                                " subId = " + subId);

                        // Set the connection settings for this transaction.
                        // If these have not been set in args, load the default settings.
                        String mmsc = args.getMmscUrl();

                        int transactionType = args.getTransactionType();

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "handle EVENT_TRANSACTION_REQUEST: transactionType=" +
                                    transactionType + " " + decodeTransactionType(transactionType));
                        }

                        // Create appropriate transaction
                        switch (transactionType) {
                            case Transaction.NOTIFICATION_TRANSACTION:
                                String uri = args.getUri();
                                MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: NOTIFICATION_TRANSACTION, uri="+uri);
                                if (uri != null) {
                                    transaction = new NotificationTransaction(
                                            TransactionService.this, serviceId,
                                            uri, subId);
                                } else {
                                    // Now it's only used for test purpose.
                                    byte[] pushData = args.getPushData();
                                    PduParser parser = new PduParser(
                                            pushData,
                                            PduParserUtil.shouldParseContentDisposition(subId));
                                    GenericPdu ind = parser.parse();

                                    int type = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
                                    if ((ind != null) && (ind.getMessageType() == type)) {
                                        transaction = new NotificationTransaction(
                                                TransactionService.this, serviceId,
                                                (NotificationInd) ind, subId);
                                    } else {
                                        Log.e(TAG, "Invalid PUSH data.");
                                        transaction = null;
                                        return;
                                    }
                                }
                                break;
                            case Transaction.RETRIEVE_TRANSACTION:
                                MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: RETRIEVE_TRANSACTION uri=" + args.getUri());
                                transaction = new RetrieveTransaction(
                                        TransactionService.this, serviceId,
                                        args.getUri(), subId);
                                break;
                            case Transaction.SEND_TRANSACTION:
                            	MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: SEND_TRANSACTION");
                                transaction = new SendTransaction(
                                        TransactionService.this, serviceId,
                                        mmsc, args.getUri(), subId);
                                break;
                            case Transaction.READREC_TRANSACTION:
                            	MmsLog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: READREC_TRANSACTION");
                                transaction = new ReadRecTransaction(
                                        TransactionService.this, serviceId,
                                        mmsc, args.getUri(), subId);
                                break;
                            default:
                                Log.w(TAG, "Invalid transaction type: " + serviceId);
                                transaction = null;
                                return;
                        }

                        if (!processTransaction(transaction)) {
                            transaction = null;
                            return;
                        }

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Started processing of incoming message: " + msg);
                        }
                    } catch (Exception ex) {
                        Log.w(TAG, "Exception occurred while handling message: " + msg, ex);

                        if (transaction != null) {
                            try {
                                transaction.detach(TransactionService.this);
                                if (mProcessing.contains(transaction)) {
                                    synchronized (mProcessing) {
                                        mProcessing.remove(transaction);
                                    }
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "Unexpected Throwable.", t);
                            } finally {
                                // Set transaction to null to allow stopping the
                                // transaction service.
                                transaction = null;
                            }
                        }
                    } finally {
                        if (transaction == null) {
                            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                Log.v(TAG, "Transaction was null. Stopping self: " + serviceId);
                            }
                            stopSelf(serviceId);
                        }
                    }
                    return;
                case EVENT_HANDLE_NEXT_PENDING_TRANSACTION:
                    processPendingTransaction(transaction);
                    return;
                default:
                    Log.w(TAG, "what=" + msg.what);
                    return;
            }
        }

        public void processPendingTransaction(Transaction transaction) {

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "processPendingTxn: transaction=" + transaction);
            }

            int numProcessTransaction = 0;
            synchronized (mProcessing) {
                if (mPending.size() != 0) {
                    transaction = mPending.remove(0);
                }
                numProcessTransaction = mProcessing.size();
            }

            if (transaction != null) {

                /*
                 * Process deferred transaction
                 */
                try {
                    int serviceId = transaction.getServiceId();

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processPendingTxn: process " + serviceId);
                    }

                    if (processTransaction(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Started deferred processing of transaction  "
                                    + transaction);
                        }
                    } else {
                        transaction = null;
                        stopSelf(serviceId);
                    }
                } catch (IOException e) {
                    Log.w(TAG, e.getMessage(), e);
                }
            }
        }

        /**
         * Internal method to begin processing a transaction.
         * @param transaction the transaction. Must not be {@code null}.
         * @return {@code true} if process has begun or will begin. {@code false}
         * if the transaction should be discarded.
         * @throws IOException if connectivity for MMS traffic could not be
         * established.
         */
        private boolean processTransaction(Transaction transaction)
                throws IOException {
        	MmsLog.v(MmsApp.TXN_TAG, "process Transaction");
            // Check if transaction already processing
            synchronized (mProcessing) {
                for (Transaction t : mPending) {
                    if (t.isEquivalent(transaction)) {
                        MmsLog.v(MmsApp.TXN_TAG,
                                "Transaction already pending: " + transaction.getServiceId());
                        return true;
                    }
                }
                for (Transaction t : mProcessing) {
                    if (t.isEquivalent(transaction)) {
                        MmsLog.v(MmsApp.TXN_TAG,
                                "Duplicated transaction: " + transaction.getServiceId());
                        return true;
                    }
                }
                MmsLog.v(MmsApp.TXN_TAG, "Adding transaction to 'mProcessing' list: "
                        + transaction);
                mProcessing.add(transaction);
            }

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "processTransaction: starting transaction " + transaction);
            }

            if (mOpTransactionServiceExt.processTransaction(transaction.mIsCancelling)) {
                cancelTransaction(transaction);
                return false;
            }

            // Attach to transaction and process it
            transaction.attach(TransactionService.this);
            transaction.process();
            if (mServiceHandler.hasMessages(EVENT_PROCESS_TIME_OUT)) {
                mServiceHandler.removeMessages(EVENT_PROCESS_TIME_OUT);
            }

            setTransactionById(transaction);
            Message msg = mServiceHandler.obtainMessage(EVENT_PROCESS_TIME_OUT);
            msg.obj = getTransactionId(transaction);
            mServiceHandler.sendMessageDelayed(msg, TRANSACTION_PROCESS_TIME_OUT);
            return true;
        }
    }

    public Map<String, Transaction> sProcessingTxn = new ConcurrentHashMap<String, Transaction>();
    private void setTransactionById(Transaction transaction) {
        String uri = getTransactionId(transaction);
        if (uri != null) {
            synchronized (sProcessingTxn) {
                if ( uri != null && !sProcessingTxn.containsKey(uri)) {
                    sProcessingTxn.put(uri, transaction);
                }
            }
            MmsLog.d(MmsApp.TXN_TAG, "----setTransaction() enter, uri: " + uri + "  sProcessingTxn: "+sProcessingTxn);
        }
    }

    private Transaction getTransactionById(String uri) {
        MmsLog.d(MmsApp.TXN_TAG, "----getTransaction() enter uri: "+uri
                + " sProcessingTxn: "+sProcessingTxn);

        synchronized (sProcessingTxn) {
            if (sProcessingTxn.containsKey(uri)) {
                return sProcessingTxn.get(uri);
            }
        }
        return null;
    }

    private void removeTransactionById(String uri) {
        MmsLog.d(MmsApp.TXN_TAG, "----removeTransaction() enter, uri = " + uri);
        synchronized (sProcessingTxn) {
            if ( uri != null && sProcessingTxn.containsKey(uri)) {
                sProcessingTxn.remove(uri);
            }
        }
        if (sProcessingTxn != null) {
            MmsLog.d(MmsApp.TXN_TAG, "----removeTransaction() sProcessingTxn: "+sProcessingTxn);
        }
    }

    private String getTransactionId(Transaction transaction) {
        if (transaction == null) {
            MmsLog.d(MmsApp.TXN_TAG, "----getTransactionUri() transaction null ");
            return null;
        }
        String uri = null;

        if (transaction instanceof ReadRecTransaction){
            uri = ((ReadRecTransaction) transaction).getReadRecUrl();
        } else {
            uri = transaction.getId();
        }
        if (uri != null) {
            MmsLog.d(MmsApp.TXN_TAG, "----getTransactionUri() uri: "+uri + " transaction: "+transaction);
        }
        return uri;
    }

    private Transaction getLastTransaction() {
        Transaction lastTransaction = null;
        synchronized (mProcessing) {
            if (!mProcessing.isEmpty()) {
                lastTransaction = mProcessing.get(0);
            }
        }
        if (lastTransaction == null && !mPending.isEmpty()) {
            lastTransaction = mPending.get(0);
        }
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "getLastTransaction, transaction = " + lastTransaction);
        }
        return lastTransaction;
    }

    private void handleTransactionTimeout(String uri) {
        if (!mProcessing.isEmpty()) {
            MmsLog.d(MmsApp.TXN_TAG, "----handleTransactionTimeout(),  uri = " + uri);
            Transaction transaction = getTransactionById(uri);
            checkTransactionState(transaction);
        }
    }

    private void handleTransactionProcessed(Intent intent, int serviceId) {
        if (!mProcessing.isEmpty()) {
            String bundleUri = intent.getStringExtra(TransactionBundle.URI);
            String oriUri = intent.getStringExtra("oriuri");
            String contentUri = intent.getStringExtra("uri");
            Transaction transaction = getTransactionById(oriUri);
            MmsLog.d(MmsApp.TXN_TAG, "---handleTransactionProcessed(), oriUri = " + oriUri
                    + " content uri = " + contentUri + " transaction = " + transaction);
            int result = intent.getIntExtra("result", SmsManager.MMS_ERROR_UNSPECIFIED);
            MmsLog.d(MmsApp.TXN_TAG, "handleTransactionProcessed(), error type = " + result
                    + ", bundle uri = " + bundleUri);
            if (transaction != null && transaction.getUri() != null) {
                transaction.getState().setContentUri(transaction.getUri());
                if (result == Activity.RESULT_OK) {
                    if (transaction instanceof NotificationTransaction) {
                        result = ((NotificationTransaction) transaction).checkPduResult();
                    } else if (transaction instanceof RetrieveTransaction) {
                        result = ((RetrieveTransaction) transaction).checkPduResult();
                    }
                }
                int slotId = SubscriptionManager.getSlotId(transaction.mSubId);
                boolean isSubInserted = slotId >= 0
                        && slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX;
                final CellConnMgr cellConnMgr = new CellConnMgr(getApplicationContext());
                final int state = cellConnMgr.getCurrentState(transaction.mSubId,
                        CellConnMgr.STATE_FLIGHT_MODE | CellConnMgr.STATE_SIM_LOCKED
                                | CellConnMgr.STATE_RADIO_OFF);
                boolean subDisabled = (!isSubInserted)
                        || ((state & CellConnMgr.STATE_FLIGHT_MODE) == CellConnMgr.STATE_FLIGHT_MODE)
                        || ((state & CellConnMgr.STATE_RADIO_OFF) == CellConnMgr.STATE_RADIO_OFF)
                        || ((state & CellConnMgr.STATE_SIM_LOCKED) == CellConnMgr.STATE_SIM_LOCKED);

                if (result == Activity.RESULT_OK || transaction instanceof ReadRecTransaction) {
                    transaction.getState().setState(TransactionState.SUCCESS);
                    setTransactionStatus(intent, transaction);
                } else if (subDisabled) {
                    setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                } else {
                    if (!mOpTransactionServiceExt
                            .handleTransactionProcessed(this, result, transaction, intent)) {
                        switch (result) {
                        case SmsManager.MMS_ERROR_UNSPECIFIED:
                            setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                            break;
                        case SmsManager.MMS_ERROR_INVALID_APN:
                            setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                            break;
                        case SmsManager.MMS_ERROR_IO_ERROR:
                            setTransactionFail(transaction, FAILE_TYPE_TEMPORARY);
                            break;
                        case SmsManager.MMS_ERROR_HTTP_FAILURE:
                        case SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS:
                        case SmsManager.MMS_ERROR_CONFIGURATION_ERROR:
                               //add for sync
                               setTransactionFail(transaction, FAILE_TYPE_TEMPORARY);
                            break;
                        default:
                            MmsLog.e(MmsApp.TXN_TAG, "Unknown Error type");
                            setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                            break;
                        }
                    }
                }

                mOpTransactionServiceExt.handleOpTransactionProcessed(intent,
                        transaction instanceof ReadRecTransaction, subDisabled);
                transaction.notifyObservers();
            } else {
                MmsLog.e(MmsApp.TXN_TAG, "handleTransactionProcessed(), uri not match!");
            }
        }
    }

    private void setTransactionStatus(Intent intent, Transaction txn) {
        if (txn instanceof NotificationTransaction || txn instanceof RetrieveTransaction) {
            String newUri = intent.getStringExtra("uri");
            int result = intent.getIntExtra("result", SmsManager.MMS_ERROR_UNSPECIFIED);
            if (newUri != null) {
                txn.getState().setContentUri(Uri.parse(newUri));
            }

            if (txn instanceof NotificationTransaction) {
                if (result == Activity.RESULT_OK) {
                    ((NotificationTransaction) txn).sendNotifyRespInd(PduHeaders.STATUS_RETRIEVED);
                } else {
                    ((NotificationTransaction) txn).sendNotifyRespInd(PduHeaders.STATUS_DEFERRED);
                }
            } else if (txn instanceof RetrieveTransaction) {
                if ((result == Activity.RESULT_OK)
                        && newUri != null) {
                    ((RetrieveTransaction) txn).sendAcknowledgeInd(newUri);
                }
            }

        }
    }

    private void checkTransactionState(Transaction txn) {
        Uri uri = null;
        Intent intent = new Intent(ACTION_TRANSACION_PROCESSED);
        int result = SmsManager.MMS_ERROR_UNSPECIFIED;
        if (txn instanceof SendTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "checkTransactionState. :Send");
            uri = ((SendTransaction) txn).getUri();
            Cursor cursor = null;
            try {
                cursor = SqliteWrapper.query(getApplicationContext(), getApplicationContext()
                        .getContentResolver(), uri, new String[] { Mms.MESSAGE_BOX },
                        null, null, null);
                if (cursor != null && cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    int msgBox = cursor.getInt(0);
                    if (msgBox == Mms.MESSAGE_BOX_SENT) {
                        result = Activity.RESULT_OK;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

        } else if (txn instanceof NotificationTransaction || txn instanceof RetrieveTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "checkTransactionState. :Notification/Retrieve");
            if (txn instanceof NotificationTransaction) {
                uri = ((NotificationTransaction) txn).getUri();
            } else {
                uri = ((RetrieveTransaction) txn).getUri();
            }
            Cursor cursor = null;
            try {
                cursor = SqliteWrapper.query(getApplicationContext(), getApplicationContext()
                        .getContentResolver(), uri, new String[] { Mms.MESSAGE_BOX },
                        null, null, null);
                if (cursor == null || cursor.getCount() == 0) {
                    result = Activity.RESULT_OK;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (txn instanceof ReadRecTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :ReadRec");
            uri = ((ReadRecTransaction) txn).getUri();
            result = Activity.RESULT_OK;
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "checkTransactionState. type cann't be recognised");
            return;
        }
        intent.putExtra(TransactionBundle.URI, uri.toString());
        intent.putExtra("result", result);
        handleTransactionProcessed(intent, 0);
    }

    private void setTransactionFail(Transaction txn, int failType) {
        MmsLog.v(MmsApp.TXN_TAG, "set Transaction Fail. fail Type=" + failType);

        long msgId = 0;
        Uri uri = null;
        if (txn instanceof SendTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Send");
            uri = ((SendTransaction) txn).getUri();
        } else if (txn instanceof NotificationTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Notification");
            uri = ((NotificationTransaction) txn).getUri();
        } else if (txn instanceof RetrieveTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Retrieve");
            uri = ((RetrieveTransaction) txn).getUri();
        } else if (txn instanceof ReadRecTransaction) {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. :ReadRec");
            uri = ((ReadRecTransaction) txn).getUri();
            // add this for read report.
            // if the read report is failed to open connection.mark it
            // sent(129).i.e. only try to send once.
            // [or mark 128, this is another policy, this will resend next time
            // into UI and out.]
            ContentValues values = new ContentValues(1);
            values.put(Mms.READ_REPORT, 129);
            SqliteWrapper.update(getApplicationContext(), getApplicationContext()
                    .getContentResolver(), uri, values, null, null);
            txn.mTransactionState.setState(TransactionState.FAILED);
            txn.mTransactionState.setContentUri(uri);
            return;
        } else {
            MmsLog.d(MmsApp.TXN_TAG, "set Transaction Fail. type cann't be recognised");
        }

        if (null != uri) {
            txn.mTransactionState.setContentUri(uri);
            msgId = ContentUris.parseId(uri);
        } else {
            MmsLog.e(MmsApp.TXN_TAG, "set Transaction Fail. uri is null.");
            return;
        }

        if (txn instanceof NotificationTransaction) {
            DownloadManager downloadManager = DownloadManager.getInstance();
            boolean autoDownload = false;
            autoDownload = downloadManager.isAuto(txn.mSubId);

            if (!autoDownload) {
                txn.mTransactionState.setState(TransactionState.SUCCESS);
            } else {
                txn.mTransactionState.setState(TransactionState.FAILED);
            }
        } else {
            txn.mTransactionState.setState(TransactionState.FAILED);
        }

        Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(getApplicationContext(), getApplicationContext()
                .getContentResolver(), uriBuilder.build(), null, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    DefaultRetryScheme scheme = new DefaultRetryScheme(getApplicationContext(), 100);

                    ContentValues values = null;
                    if (FAILE_TYPE_PERMANENT == failType) {
                        values = new ContentValues(2);
                        values.put(PendingMessages.ERROR_TYPE, MmsSms.ERR_TYPE_GENERIC_PERMANENT);
                        values.put(PendingMessages.RETRY_INDEX, scheme.getRetryLimit());

                        int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                        long id = cursor.getLong(columnIndex);

                        SqliteWrapper.update(getApplicationContext(), getApplicationContext()
                                .getContentResolver(), PendingMessages.CONTENT_URI, values,
                                PendingMessages._ID + "=" + id, null);
                    }
                    mOpTransactionServiceExt.setTransactionFail(this, failType, cursor);
                }
            } finally {
                cursor.close();
            }
        }

    }

    private boolean isDuringCall() {
        int[] subIds = SubscriptionManager.from(MmsApp.getApplication()).getActiveSubscriptionIdList();
        TelephonyManager telephonyManager = MmsApp.getApplication().getTelephonyManager();
        for (int subId : subIds) {
            if (!(telephonyManager.getCallState(subId) == TelephonyManager.CALL_STATE_IDLE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * M: cancel download mms.
     */
    private void cancelTransaction(Transaction transaction) {
        setTransactionFail(transaction, FAILE_TYPE_PERMANENT);

        transaction.mIsCancelling = false;

        Uri trxnUri = null;
        if (transaction.getType() == Transaction.RETRIEVE_TRANSACTION) {
            trxnUri = ((RetrieveTransaction) transaction).getUri();
        } else if (transaction.getType() == Transaction.NOTIFICATION_TRANSACTION) {
            trxnUri = ((NotificationTransaction) transaction).getUri();
        }

        DownloadManager.getInstance().markState(trxnUri, DownloadManager.STATE_UNSTARTED);

        mOpTransactionServiceExt.cancelTransaction(getApplicationContext(), trxnUri);
    }

    /**
     * M: Op callback. Set a label, according to which, we can know whether user need to
     * cancel the transaction.
     *
     * @param uri the mms's uri
     * @param isCancelling true: is in cancell; false: is not in cancell.
     */
    public void setCancelDownloadState(Uri uri, boolean isCancelling) {
        MmsLog.d(MmsApp.TXN_TAG, "setCancelDownloadState: isCancelling = " + isCancelling
                + " Uri = " + uri);

        /// M: Construct uri which contains "inbox".
        Uri inboxUri = Uri.parse("content://" + uri.getAuthority() + "/inbox/"
            + uri.getLastPathSegment());
        Uri uriInList = null;

        for (int i = 0; i < 2; i++) {
            synchronized (mProcessing) {
                for (Transaction t : mPending) {
                    MmsLog.d(MmsApp.TXN_TAG, "setCancelDownloadState: search in mPending");
                    if (t.getType() == Transaction.RETRIEVE_TRANSACTION) {
                        uriInList = ((RetrieveTransaction) t).getUri();
                        MmsLog.d(MmsApp.TXN_TAG, "    uriInList == " + uriInList);
                    } else if (t.getType() == Transaction.NOTIFICATION_TRANSACTION) {
                        uriInList = ((NotificationTransaction) t).getUri();
                        MmsLog.d(MmsApp.TXN_TAG, "    uriInList == " + uriInList);
                    }

                    /// M: Compare inboxUri above is for DialogModeActivity case.
                    if (uriInList != null && (uriInList.equals(uri)
                            || uriInList.equals(inboxUri))) {
                        /// M: If any other transaction is in processing,
                        /// just move this from mPending.
                        if (mProcessing.size() > 0) {
                            mPending.remove(t);
                            cancelTransaction(t);
                            MmsLog.d(MmsApp.TXN_TAG, "***Cancel download when mProcessing > 0!");
                            return;
                        }

                        if (isCancelling && mOpTransactionServiceExt.cancelandRemoveTransaction()) {
                            mPending.remove(t);
                            cancelTransaction(t);
                            MmsLog.d(MmsApp.TXN_TAG, "***Cancel download when waiting connection!");
                        } else {
                            t.mIsCancelling = isCancelling;
                        }
                        MmsLog.d(MmsApp.TXN_TAG, "setCancelDownloadState: find in mPending");
                        return;
                    }
                }

                for (Transaction t : mProcessing) {
                    MmsLog.d(MmsApp.TXN_TAG, "setCancelDownloadState: search in mProcessing");
                    if (t.getType() == Transaction.RETRIEVE_TRANSACTION) {
                        uriInList = ((RetrieveTransaction) t).getUri();
                        MmsLog.d(MmsApp.TXN_TAG, "    uriInList == " + uriInList);
                    } else if (t.getType() == Transaction.NOTIFICATION_TRANSACTION) {
                        uriInList = ((NotificationTransaction) t).getUri();
                        MmsLog.d(MmsApp.TXN_TAG, "    uriInList == " + uriInList);
                    }

                    if (uriInList != null && (uriInList.equals(uri)
                            || uriInList.equals(inboxUri))) {
                        t.mIsCancelling = isCancelling;
                        if (mProcessing.size() > 0) {
                            mProcessing.remove(t);
                        }
                        MmsLog.d(MmsApp.TXN_TAG, "setCancelDownloadState: find in mProcessing");
                        return;
                    }
                }

                uriInList = null;
            }
            /// M: If the transacton not found both in mPending and mProcessing, wait and try again.
            SystemClock.sleep(500);
        }
        mOpTransactionServiceExt.markStateComplete(uri);
        MmsLog.e(MmsApp.TXN_TAG, "setCancelDownloadState: No transaction to be canceled!");
    }

    public void setTransactionFailCallback(Object transaction, int failType) {
        setTransactionFail((Transaction) transaction, failType);
    }

    public boolean isSendOrRetrieveTransaction(Object transaction) {
        return transaction instanceof SendTransaction || transaction instanceof RetrieveTransaction;
    }
}
