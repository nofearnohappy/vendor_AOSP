package com.mediatek.rcs.pam;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;

import com.mediatek.rcs.pam.client.PAMClient;
import com.mediatek.rcs.pam.message.IPAMMessageHelper;
import com.mediatek.rcs.pam.message.PAMAudioMessage;
import com.mediatek.rcs.pam.message.PAMBaseMessage;
import com.mediatek.rcs.pam.message.PAMGeolocMessage;
import com.mediatek.rcs.pam.message.PAMImageMessage;
import com.mediatek.rcs.pam.message.PAMTextMessage;
import com.mediatek.rcs.pam.message.PAMVcardMessage;
import com.mediatek.rcs.pam.message.PAMVideoMessage;
import com.mediatek.rcs.pam.model.MediaArticle;
import com.mediatek.rcs.pam.model.MediaEntry;
import com.mediatek.rcs.pam.model.MenuInfo;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.FavouriteContract;
import com.mediatek.rcs.pam.provider.FavouriteContract.FavouriteColumns;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.provider.PAContract.StateColumns;
import com.mediatek.rcs.pam.provider.PAProvider;
import com.mediatek.rcs.pam.provider.RcseProviderContract;
import com.mediatek.rcs.pam.util.DownloadService;
import com.mediatek.rcs.pam.util.Triplet;
import com.mediatek.rcs.pam.util.Utils;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.GeolocMessage;
import org.gsma.joyn.chat.PublicAccountChat;
import org.gsma.joyn.chat.SpamReportListener;
import org.gsma.joyn.ft.FileSpamReportListener;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PAServiceImpl extends Service {
    public static final String ACTION_RESTART = "com.mediatek.pam.RESTART";

    public static final String ACTION_KEY_ACCOUNT = "contact";
    public static final String ACTION_KEY_CHAT_MESSAGE = "firstMessage";
    public static final String ACTION_KEY_FT_ID = "transferId";

    private static final String TAG = "PAM/PAServiceImpl";
    private static long sUuidCounter = 0;
    private static long sTokenCounter = 0;
    private boolean mIsGettingSubscribedList = false;

    private static final long SERVICE_TOKEN = -1000;

    private PAMClient mClient;
    private ExecutorService mExecutorService;
    private DownloadService mDownloadService;
    private ConcurrentHashMap<Long, CancellableTask> mWorkingTasks;
    private ConcurrentHashMap<String, DownloadTask> mDownloadingTasks;
    private Handler mHandler;
    private PASBinder mPASBinder;

    private ChatService mRcseChatService;
    private FileTransferService mRcseFTService;

    private ConcurrentHashMap<Long, PASCallbackWrapper> mClientListeners;
    private LruCache<String, PublicAccountChat> mChats;

    private Map<Long, PAMBaseMessage> mMessagePool;

    private static synchronized long generateUuid() {
        sUuidCounter += 1;
        return sUuidCounter;
    }

    private static synchronized long generateToken() {
        sTokenCounter += 1;
        return sTokenCounter;
    }

    /****************************************************
     * Listeners
     *****************************************************/
    class PAMMessageHelper implements IPAMMessageHelper {

        @Override
        public ChatService getChatService() {
            return mRcseChatService;
        }

        @Override
        public FileTransferService getFileTransferService() {
            return mRcseFTService;
        }

        @Override
        public ChatListener getChatServiceListener(long msgId) {
            return new PAChatListener(msgId);
        }

        @Override
        public FileTransferListener getFileTransferListener(long msgId) {
            return new PAFTListener(msgId);
        }

        @Override
        public PublicAccountChat getChatCache(long token, String uuid) {
            return mChats.get(String.valueOf(token) + uuid);
        }

        @Override
        public void updateChatCache(long token, String uuid, PublicAccountChat chat) {
            mChats.put(String.valueOf(token) + uuid, chat);
        }

        @Override
        public SpamReportListener getSpamReportListener(
                long token, String sourceId, long msgId) {
            return new PASpamReportListener(token, sourceId, msgId);
        }

        @Override
        public FileSpamReportListener getFileSpamReportListener(
                long token, String sourceId, long msgId) {
            return new PAFileSpamReportListener(token, sourceId, msgId);
        }
    }

    class PAMJoynRegistrationListener extends JoynServiceRegistrationListener {
        String mTag;

        PAMJoynRegistrationListener(String tag) {
            mTag = tag;
        }

        @Override
        public void onServiceUnregistered() {
            Log.d(TAG, mTag + " is unregistered");
            notifyListenersOfServiceUnregisteredEvent();
        }

        @Override
        public void onServiceRegistered() {
            try {
                Log.d(TAG, mTag + " is registered");
                if (mRcseFTService.isServiceConnected() &&
                    mRcseFTService.isServiceRegistered() &&
                    mRcseChatService.isServiceConnected() &&
                    mRcseChatService.isServiceRegistered()) {
                    Log.d(TAG, "both services are registered");
                    notifyListenersOfServiceRegisteredEvent();
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception happened in registration. Ignore.");
            }
        }
    }

    class PAMJoynChatServiceListener implements JoynServiceListener {
        @Override
        public void onServiceDisconnected(int reason) {
            Log.d(TAG, "mRcseChatService is disconnected");
            notifyListenersOfServiceDisconnectedEvent(reason);
        }

        @Override
        public void onServiceConnected() {
            Log.d(TAG, "mRcseChatService is connected");
            try {
                mRcseChatService.addServiceRegistrationListener(
                        new PAMJoynRegistrationListener("mRcseChatService"));
            } catch (JoynServiceException e) {
                Log.e(TAG, "Maybe RCS service is offline again.");
                e.printStackTrace();
                return;
            }
            if (mRcseFTService.isServiceConnected()) {
                Log.d(TAG, "mRcseFTService is also connected");
                notifyListenersOfServiceConnectedEvent();
            }
        }
    }

    class PAMJoynFTServiceListener implements JoynServiceListener {
        @Override
        public void onServiceDisconnected(int reason) {
            Log.d(TAG, "mRcseFTService is disconnected");
            notifyListenersOfServiceDisconnectedEvent(reason);
        }

        @Override
        public void onServiceConnected() {
            Log.d(TAG, "mRcseFTService is connected");
            try {
                mRcseFTService.addServiceRegistrationListener(
                        new PAMJoynRegistrationListener("mRcseFTService"));
            } catch (JoynServiceException e) {
                Log.e(TAG, "Maybe RCS service is offline again.");
                e.printStackTrace();
                return;
            }
            if (mRcseChatService.isServiceConnected()) {
                Log.d(TAG, "mRcseChatService is also connected");
                notifyListenersOfServiceConnectedEvent();
            }
        }

    }

    public class PAChatListener extends ChatListener {
        private static final String TAG = Constants.TAG_PREFIX + "PAChatListener";
        private long mToken = Constants.INVALID;
        private long mMsgId;

        public PAChatListener(long msgId) {
            mMsgId = msgId;
            PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "Failed to find chat messag " + mMsgId);
                return;
            }
            mToken = msg.getToken();
        }

        @Override
        public void onReportMessageFailed(String msgId) {
            Log.d(TAG, "onReportMessageFailed(" + msgId + ")");
            reportMessageFailed(msgId);
        }

        @Override
        public void onReportFailedMessage(String msgId, int errtype, String statusCode) {
            Log.d(TAG, "onReportFailedMessage(" + msgId + "," + errtype + ","  + statusCode + ")");
            reportMessageFailed(msgId);
        }

        private void reportMessageFailed(final String sourceId) {
            Log.d(TAG, "reportMessageFailed:" + sourceId);
            final long msgId = getMessageIdFromSourceId(sourceId);
            final PAMTextMessage msg = (PAMTextMessage)mMessagePool.get(msgId);
            if (null == msg) {
                Log.e(TAG, "reportMessageFailed() can't find msg, msgId=" + msgId);
                return;
            }
            if (msg.shouldRetry()) {
                // try to recover in 5s
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "Auto recover message: " + msgId);
                            resendMessageInternal(msgId);
                        } catch (JoynServiceException e) {
                            e.printStackTrace();
                            updateSendStatus(msgId, Constants.MESSAGE_STATUS_FAILED);
                            reportSendFailed(msgId);
                        }
                    }
                }, PAMBaseMessage.RECOVER_INTERVAL);
            } else {
                updateSendStatus(msgId, Constants.MESSAGE_STATUS_FAILED);
                reportSendFailed(msgId);
            }
        }

        @Override
        public void onReportMessageDisplayed(String sourceId) {
            Log.d(TAG, "onReportMessageDisplayed(" + sourceId + ")");
            try {
                long msgId = getMessageIdFromSourceId(sourceId);
                PAMTextMessage msg = (PAMTextMessage)mMessagePool.get(msgId);
                if (null == msg) {
                    Log.e(TAG, "onReportMessageDisplayed() failed to find msg:" + msgId);
                    return;
                }
                updateSendStatus(msgId, Constants.MESSAGE_STATUS_READ);
                if (mToken == SERVICE_TOKEN) {
                    for (IPAServiceCallback listener : mClientListeners.values()) {
                        listener.onReportMessageDisplayed(msgId);
                    }
                } else {
                    IPAServiceCallback callback = mClientListeners.get(mToken);
                    if (null != callback) {
                        callback.onReportMessageDisplayed(msgId);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback " + mToken);
                mClientListeners.remove(mToken);
            }
        }

        @Override
        public void onReportMessageDelivered(String msgId) {
            Log.d(TAG, "onReportMessageDelivered(" + msgId + ")");
            reportMessageDelivered(msgId);
        }

        @Override
        public void onReportDeliveredMessage(String msgId) {
            Log.d(TAG, "onReportDeliveredMessage(" + msgId + ")");
            reportMessageDelivered(msgId);
        }

        @Override
        public void onReportSentMessage(String msgId) {
            Log.d(TAG, "onReportSentMessage(" + msgId + ")");
            reportMessageDelivered(msgId);
        }

        private void reportMessageDelivered(String sourceId) {

            long msgId = getMessageIdFromSourceId(sourceId);
            PAMTextMessage msg = (PAMTextMessage)mMessagePool.get(msgId);

            if (null == msg) {
                Log.e(TAG, "reportMessageDelivered() but failed to find msg:" + msgId);
                return;
            }

            updateSendStatus(msgId, Constants.MESSAGE_STATUS_SENT);
            reportSendSuccess(msgId);
            if (msg.getIsSystem()) {
                deleteMessageInRCSe(sourceId);
                deleteMessage(msgId);
            }
        }

        @Override
        public void onComposingEvent(boolean status) {
            Log.d(TAG, "onComposingEvent(" + status + ")");
        }

        @Override
        public void onNewMessage(ChatMessage message) {
            Log.d(TAG, "onNewMessage(" + message.getId() + ") not supported.");
        }

        @Override
        public void onNewBurnMessageArrived(ChatMessage message) {
            Log.d(TAG, "onNewBurnMessageArrived(" + message.getId() + ") not supported.");
        }

        @Override
        public void onNewGeoloc(GeolocMessage message) {
            Log.d(TAG, "onNewGeoloc(" + message.getId() + ") not supported.");
        }
    }

    public class PAFTListener extends FileTransferListener {

        private String mTransferId;
        private final long mMsgId;
        private long mToken = Constants.INVALID;

        public PAFTListener(long msgId) {
            super();
            Log.d(TAG, "Create FTListener(msgId=" + msgId + ")");
            mMsgId = msgId;
            PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "PAFTListener() failed to find msg:" + msgId);
                return;
            }
            mToken = msg.getToken();
        }

        public void setTransferId(String transferId) {
            Log.d(TAG, "Set transfer id to " + transferId +
                    " for message " + mMsgId + ", token " + mToken);
            mTransferId = transferId;
        }

        @Override
        public void onTransferStarted() {
            Log.d(TAG, "FTListener.onTransferStarted()" + ", transferId="
                    + mTransferId + ", msgId=" + mMsgId);
            PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "onTransferStarted() failed to find msg:" + mMsgId);
                return;
            }
            try {
                updateSendStatus(mMsgId, Constants.MESSAGE_STATUS_SENDING);
                IPAServiceCallback callback = mClientListeners.get(mToken);
                if (callback != null) {
                    callback.onTransferProgress(mMsgId, 0, 0);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback " + mToken);
                mClientListeners.remove(mToken);
            }
        }

        @Override
        public void onTransferProgress(long currentSize, long totalSize) {
            Log.d(TAG,
                  "FTListener.onTransferProgress(" + currentSize + ", " + totalSize + ")"
                  + ", transferId=" + mTransferId + ", messageId=" + mMsgId);

            // broadcast progress for PaComposeActivity
            for (PASCallbackWrapper c : mClientListeners.values()) {
                try {
                    c.onTransferProgress(mMsgId, currentSize, totalSize);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback " + c.getToken());
                    mClientListeners.remove(c.getToken());
                }
            }
        }

        @Override
        public void onTransferError(int error) {
            Log.d(TAG, "FTListener.onTransferError(" + error + ")" +
                    ", route it to onTransferAborted");
            onTransferAborted();
        }

        @Override
        public void onTransferAborted() {
            Log.d(TAG, "FTListener.onTransferAborted" +
                    ", transferId=" + mTransferId + ", msgId=" + mMsgId);

            final PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "failed to find msg:" + mMsgId);
                return;
            }

            if (msg.getState() == Constants.MESSAGE_STATUS_FAILED) {
                Log.d(TAG, "Message " + mMsgId + " record is already in failed state. Ignore.");
                return;
            }

            if (msg.shouldRetry()) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "Auto recover FT: " + mTransferId + ", msgId " + mMsgId);
                            resendMessageInternal(mMsgId);
                        } catch (JoynServiceException e) {
                            Log.w(TAG, "Failed to trigger resendMessageInternal with message "
                                    + mMsgId);
                            e.printStackTrace();
                            updateSendStatus(mMsgId, Constants.MESSAGE_STATUS_FAILED);
                            reportSendFailed(mMsgId);
                        }
                    }
                }, PAMBaseMessage.RECOVER_INTERVAL);
            } else {
                updateSendStatus(mMsgId, Constants.MESSAGE_STATUS_FAILED);
                reportSendFailed(mMsgId);
            }
        }

        @Override
        public void onFileTransferred(String filename) {
            Log.d(TAG, "FTListener.onFileTransferred(" + filename + ")"
                    + ", transferId=" + mTransferId + ", msgId=" + mMsgId);

            PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "onFileTransferred() failed to find msg in pool, msgId=" + mMsgId);
                return;
            }
            updateSendStatus(mMsgId, Constants.MESSAGE_STATUS_SENT);
            reportSendSuccess(mMsgId);
        }

        @Override
        public void onTransferPaused() {
            // do nothing
            Log.d(TAG, "FTListener.onTransferPaused()"
                    + ", transferId=" + mTransferId + ", msgId=" + mMsgId);
        }

        @Override
        public void onTransferResumed(String oldTransferId, String newTransferId) {
            Log.d(TAG,
                  "FTListener.onTransferResumed(" + oldTransferId + ", " + newTransferId
                  + ")" + ", transferId=" + mTransferId + ", msgId=" + mMsgId);
            PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "onTransferResumed() failed to find msg:" + mMsgId);
                return;
            }
            msg.updateSourceId(newTransferId);
            updateSendStatus(mMsgId, Constants.MESSAGE_STATUS_SENDING);
        }
    }

    public class PASpamReportListener extends SpamReportListener {
        private long mToken;
        private String mSourceId;
        private long mMsgId;

        PASpamReportListener(long token, String sourceId, long msgId) {
            mToken = token;
            mSourceId = sourceId;
            mMsgId = msgId;
        }

        @Override
        public void onSpamReportSuccess(String contact, String sourceId) {
            try {
                if (mSourceId.equals(sourceId)) {
                    mRcseChatService.removeSpamReportListener(this);
                    IPAServiceCallback callback = mClientListeners.get(mToken);
                    if (null != callback) {
                        callback.reportComplainSpamSuccess(mMsgId);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback " + mToken);
                mClientListeners.remove(mToken);
            } catch (JoynServiceException e) {
                throw new Error(e);
            }
        }

        @Override
        public void onSpamReportFailed(String contact, String sourceId, int errorCode) {
            try {
                if (mSourceId.equals(sourceId)) {
                    mRcseChatService.removeSpamReportListener(this);
                    IPAServiceCallback callback = mClientListeners.get(mToken);
                    if (null != callback) {
                        callback.reportComplainSpamFailed(mMsgId, errorCode);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback " + mToken);
                mClientListeners.remove(mToken);
            } catch (JoynServiceException e) {
                throw new Error(e);
            }
        }
    }


    public class PAFileSpamReportListener extends FileSpamReportListener {

        private long mToken;
        private String mSourceId;
        private long mMsgId;

        PAFileSpamReportListener(long token, String sourceId, long msgId) {
            mToken = token;
            mSourceId = sourceId;
            mMsgId = msgId;
        }
        @Override
        public void onFileSpamReportSuccess(String contact, String sourceId) {
            try {
                if (mSourceId.equals(sourceId)) {
                    mRcseFTService.removeFileSpamReportListener(this);
                    IPAServiceCallback callback = mClientListeners.get(mToken);
                    if (null != callback) {
                        callback.reportComplainSpamSuccess(mMsgId);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback " + mToken);
                mClientListeners.remove(mToken);
            } catch (JoynServiceException e) {
                throw new Error(e);
            }
        }

        @Override
        public void onFileSpamReportFailed(String contact, String sourceId, int errorCode) {
            try {
                if (mSourceId.equals(sourceId)) {
                    mRcseFTService.removeFileSpamReportListener(this);
                    IPAServiceCallback callback = mClientListeners.get(mToken);
                    if (null != callback) {
                        callback.reportComplainSpamFailed(mMsgId, errorCode);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback " + mToken);
                mClientListeners.remove(mToken);
            } catch (JoynServiceException e) {
                throw new Error(e);
            }
        }
    };

    final class DownloadTask {
        private DownloadService.DownloadInfo mDownloadInfo;
        private final int mType;
        private final String mUrlString;
        ArrayList<Triplet<Long, Long, Long>> mCallbackList =
            new ArrayList<Triplet<Long, Long, Long>>();

        public DownloadTask(long cancelId, long requestId,
                String url, int type, PASCallbackWrapper callback) {
            mCallbackList.add(new Triplet<Long, Long, Long>(
                callback.getToken(), cancelId, requestId));
            mUrlString = url;
            mType = type;
        }

        public void addCallback(long token, long cancelId, long requestId) {
            boolean isDup = false;

            for (Triplet<Long, Long, Long> triplet : mCallbackList) {
                if (null != triplet && triplet.first == token
                    && triplet.second == cancelId) {
                    isDup = true;
                }
            }
            if (!isDup) {
                Log.d(TAG, "addCallback for: " + token + ", " + cancelId);
                mCallbackList.add(new Triplet<Long, Long, Long>(token,
                    cancelId, requestId));
            }
        }

        public boolean contains(long cancelId) {
            for (Triplet<Long, Long, Long> pair : mCallbackList) {
                if (null != pair && pair.second == cancelId) {
                    return true;
                }
            }
            return false;
        }

        public void cancel(long cancelId) {
            for (Triplet<Long, Long, Long> triplet : mCallbackList) {
                if (null != triplet && triplet.second == cancelId) {
                    mCallbackList.remove(triplet);
                    break;
                }
            }

            if (0 == mCallbackList.size()) {
                cancel();
            }
        }

        public void cancel() {
            mDownloadInfo.cancel();
        }

        private void notifyDownloadProgress(int percentage) {
            List<Triplet<Long, Long, Long>> tempList = new ArrayList<Triplet<Long,Long,Long>>();
            tempList.addAll(mCallbackList);

            for (Triplet<Long, Long, Long> triplet : tempList) {
                if (null != triplet && triplet.first >= 0) {
                    PASCallbackWrapper callback = mClientListeners.get(triplet.first);
                    if (callback != null) {
                        try {
                            callback.updateDownloadProgress(triplet.third,
                                percentage);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e(TAG,
                                "RemoteException happened, remove callback "
                                + triplet.first);
                            mClientListeners.remove(triplet.first);
                        }
                    }
                }
            }
        }

        private void notifyDownloadResult(int result, String filePath, long mediaId) {
            for (Triplet<Long, Long, Long> triplet : mCallbackList) {
                if (null != triplet) {
                    PASCallbackWrapper callback = mClientListeners.get(triplet.first);
                    if (callback != null) {
                        try {
                            Log.d(TAG, "Task ID: " + triplet.second
                                    + ", notifyDownloadResult");
                             callback.reportDownloadResult(triplet.third,
                                result, filePath, mediaId);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e(TAG, "RemoteException, remove callback "
                                + triplet.first);
                            mClientListeners.remove(triplet.first);
                        }
                    }
                }
            }
        }

        public void doDownload() {
            Log.d(TAG, " downloadObject(" + mUrlString + ")");

            try {
                mDownloadInfo = new DownloadService.DownloadInfo(mUrlString, mType,
                    new DownloadService.DownloadCallback() {

                        @Override
                        public void updateDownloadProgress(int percentage) {
                            notifyDownloadProgress(percentage);
                        }

                        @Override
                        public void reportDownloadResult(int resultCode,
                                String filePath) {
                            long mediaId = Constants.INVALID;
                            synchronized (mDownloadingTasks) {
                                mDownloadingTasks.remove(mUrlString);
                            }
                            if (resultCode == ResultCode.SUCCESS
                                    && (!mDownloadInfo.isCancelled())) {
                            MediaEntry me = new MediaEntry(
                                        Constants.INVALID,
                                        mType,
                                        mUrlString,
                                        filePath,
                                        System.currentTimeMillis());
                            mediaId = me.storeToProvider(getContentResolver(), false);
                        }
                        notifyDownloadResult(resultCode, filePath, mediaId);
                    }
                });

                mDownloadService.startDownload(mDownloadInfo);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                notifyDownloadResult(ResultCode.PARAM_ERROR_INVALID_FORMAT
                    , null, Constants.INVALID);
            }
        }
    }

    final class SubscribeTask extends CancellableTask {

        private String mUuid;
        private int mStatus;

        public SubscribeTask(long requestId, String id,
                int status, PASCallbackWrapper callback) {
            super(requestId, callback);
            mUuid = id;
            mStatus = status;
        }

        @Override
        public void doRun() {
            Log.d(TAG, "subscribe(" + mId + ", " + mUuid + ", " + mStatus + ")");

            int result = ResultCode.SUCCESS;
            Cursor c = null;

            try {
                if (Constants.SUBSCRIPTION_STATUS_YES == mStatus) {
                    mClient.subscribe(mUuid);
                } else {
                    mClient.unsubscribe(mUuid);
                }

                ContentResolver cr = getContentResolver();
                c = cr.query(
                        AccountColumns.CONTENT_URI,
                        new String[] {
                                AccountColumns.NAME,
                                AccountColumns.RECOMMEND_LEVEL,
                                AccountColumns.LOGO_URL
                        },
                        AccountColumns.UUID + "=?",
                        new String[]{mUuid},
                        null);
                if (c == null || c.getCount() == 0) {
                    result = ResultCode.SERVICE_ERROR_SUBSCRIPTION_PUBLIC_ACCOUNT_DOES_NOT_EXIST;
                } else {
                    // update
                    c.moveToFirst();
                    ContentValues accountValues = new ContentValues();
                    accountValues.put(AccountColumns.SUBSCRIPTION_STATUS, mStatus);
                    cr.update(
                            AccountColumns.CONTENT_URI,
                            accountValues,
                            AccountColumns.UUID + "=?",
                            new String[]{mUuid});
                    }
            } catch (PAMException e) {
                result = e.resultCode;
            } finally {
                if (null != c) {
                    c.close();
                }
            }

            if (mCallback != null) {
                try {
                    if (Constants.SUBSCRIPTION_STATUS_YES == mStatus) {
                        mCallback.reportSubscribeResult(mId, result);
                    } else {
                        mCallback.reportUnsubscribeResult(mId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback " + mCallback.getToken());
                    mClientListeners.remove(mCallback.getToken());
                }
            }
            mWorkingTasks.remove(mId);
        }
    }

    class GetSubscribedListTask extends CancellableTask {
        public static final int DEFAULT_BATCH_SIZE = 50;

        private int mOrder;
        private int mBatchSize;
        private int mPageNumber;
        private boolean mIsAll;

        public GetSubscribedListTask(
                long requestId,
                int order,
                int batchSize,
                int pageNumber,
                boolean all,
                boolean isBackground,
                PASCallbackWrapper callback) {
            super(requestId, callback, isBackground);
            mOrder = order;
            mBatchSize = batchSize;
            mIsAll = all;
            if (mIsAll) {
                mPageNumber = 1;
            } else {
                mPageNumber = pageNumber;
            }
        }

        @Override
        protected void doRun() {
            Log.d(TAG, "getSubscribedInBackground");
            int result = ResultCode.SUCCESS;
            List<PublicAccount> accounts = null;
            ArrayList<Long> ids = new ArrayList<Long>();
            do {
                try {
                    accounts = mClient.getSubscribedList(mOrder, mBatchSize, mPageNumber);
                    // TODO test edge cases
                    for (PublicAccount account : accounts) {
                        Log.d(TAG, "Store account: " + account.uuid);
                        Log.d(TAG, "Subscription status: " + account.subscribeStatus);
                        long accountid = account.insertOrUpdateAccount(getContentResolver(), false);
                        ids.add(accountid);
                    }
                } catch (PAMException e) {
                    e.printStackTrace();
                    result = e.resultCode;
                }
                mPageNumber += 1;
                if (!mIsAll) {
                    break;
                }
            } while (accounts.size() == mBatchSize);

            mWorkingTasks.remove(mId);
            if (mIsBackground) {
                if (ResultCode.SUCCESS == result) {
                    setInitialized(true);
                } else {
                    setInitialized(false);
                }
                mIsGettingSubscribedList = false;
            }

            if (mCallback != null) {
                try {
                    long[] accountIds = new long[ids.size()];
                    for (int i = 0; i < ids.size() ; ++i) {
                        accountIds[i] = ids.get(i).longValue();
                    }
                    mCallback.reportGetSubscribedResult(mId, result, accountIds);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback " + mCallback.getToken());
                    mClientListeners.remove(mCallback.getToken());
                }
            }
        }

    }

    final class GetDetailsTask extends CancellableTask {
        private final String mUuid;
        private final String mTimestamp;
        public GetDetailsTask(long requestId, String uuid,
                String timestamp, PASCallbackWrapper callback) {
            super(requestId, callback);
            mUuid = uuid;
            mTimestamp = timestamp;
        }

        @Override
        public void doRun() {
            Log.d(TAG, "getDetails(" + mId + ", " + mUuid + ", " + mTimestamp + ")");

            int result = ResultCode.SUCCESS;
            long accountId = Constants.INVALID;

            try {
                PublicAccount details = mClient.getDetails(mUuid, mTimestamp);
                accountId = details.insertOrUpdateAccount(getContentResolver(), true);
            } catch (PAMException e) {
                result = e.resultCode;
            }
            if (mCallback != null) {
                try {
                    mCallback.reportGetDetailsResult(mId, result, accountId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback " + mCallback.getToken());
                    mClientListeners.remove(mCallback.getToken());
                }
            }
            mWorkingTasks.remove(mId);
        }
    }

    final class GetMenuTask extends CancellableTask {
        private final String mUuid;
        private final String mTimestamp;
        public GetMenuTask(
                long requestId,
                String uuid, String
                timestamp,
                PASCallbackWrapper callback) {
            super(requestId, callback);
            mUuid = uuid;
            mTimestamp = timestamp;
        }

        @Override
        public void doRun() {
            Log.d(TAG, "getMenu(" + mId + ", " + mUuid + ", " + mTimestamp + ")");
            int result = ResultCode.SUCCESS;
            try {
                MenuInfo info = mClient.getMenu(mUuid, mTimestamp);
                ContentResolver cr = getContentResolver();
                cr.update(
                        AccountColumns.CONTENT_URI,
                        MenuInfo.storeToContentValues(info),
                        AccountColumns.UUID + "=?",
                        new String[]{info.uuid});
            } catch (PAMException e) {
                result = e.resultCode;
            }
            if (mCallback != null) {
                try {
                    mCallback.reportGetMenuResult(mId, result);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback " + mCallback.getToken());
                    mClientListeners.remove(mCallback.getToken());
                }
            }
            mWorkingTasks.remove(mId);
        }
    }

    final class SetFavTask extends CancellableTask {
        private final long mMsgId;
        private final int mIndex;

        public SetFavTask(
                long requestId,
                long msgId,
                int index,
                final PASCallbackWrapper callback) {
            super(requestId, callback);
            mMsgId = msgId;
            mIndex = index;
        }

        @Override
        public void doRun() {
            Log.d(TAG, "setFavourite(" + mId + ", " + mMsgId + ")");
            MessageContent message = MessageContent.loadFromProvider(mMsgId, getContentResolver());

            if (message != null) {
                Cursor c = null;
                try {
                    c = getContentResolver().query(
                            AccountColumns.CONTENT_URI,
                            new String[]{ AccountColumns.UUID },
                            AccountColumns.ID + "=?",
                            new String[]{ Long.toString(message.accountId) },
                            null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        String accountUuid = c.getString(
                                c.getColumnIndexOrThrow(AccountColumns.UUID));

                        ContentValues cv = new ContentValues();
                        cv.put(FavouriteContract.FavouriteColumns.DATE, System.currentTimeMillis());
                        cv.put(FavouriteContract.FavouriteColumns.ADDRESS, accountUuid);
                        if (message.direction == Constants.MESSAGE_DIRECTION_INCOMING) {
                            cv.put(FavouriteContract.FavouriteColumns.ADDRESS, accountUuid);
                        } else if (message.direction != Constants.MESSAGE_DIRECTION_OUTGOING) {
                            throw new Error("Bad message direction: " + message.direction);
                        } /* else { address = null } */
                        /* chatid = null */
                        cv.put(FavouriteContract.FavouriteColumns.MESSAGE_ID, message.sourceId);
                        cv.put(FavouriteContract.FavouriteColumns.CONTACT_NUMBER,
                               Constants.SIP_PREFIX + accountUuid);
                        cv.put(FavouriteContract.FavouriteColumns.TIMESTAMP, message.timestamp);
                        cv.put(FavouriteContract.FavouriteColumns.MESSAGE_STATUS, message.status);
                        cv.put(FavouriteContract.FavouriteColumns.DIRECTION, message.direction);
                        cv.put(FavouriteContract.FavouriteColumns.FLAG,
                                com.cmcc.ccs.chat.ChatMessage.PUBLIC);

                        File file = null;
                        if (message.mediaType == Constants.MEDIA_TYPE_TEXT) {
                            cv.put(FavouriteContract.FavouriteColumns.SUBJECT, message.text);
                            /* path = null */
                            /* size = null */
                            cv.put(FavouriteContract.FavouriteColumns.BODY, message.text);
                            cv.put(FavouriteContract.FavouriteColumns.MIME_TYPE, "text/plain");
                            cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                    com.cmcc.ccs.chat.ChatService.IM);
                        } else if (message.mediaType == Constants.MEDIA_TYPE_PICTURE ||
                                   message.mediaType == Constants.MEDIA_TYPE_AUDIO ||
                                   message.mediaType == Constants.MEDIA_TYPE_VIDEO) {
                            /* subject = null */
                            file = new File(message.basicMedia.originalPath);

                            cv.put(FavouriteContract.FavouriteColumns.SIZE,
                                   Integer.parseInt(message.basicMedia.fileSize));
                            if (message.direction == Constants.MESSAGE_DIRECTION_INCOMING) {
                                cv.put(FavouriteContract.FavouriteColumns.BODY, message.text);
                                cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                        com.cmcc.ccs.chat.ChatService.XML);
                            } else {
                                cv.put(FavouriteContract.FavouriteColumns.BODY, message.sourceId);
                                cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                        com.cmcc.ccs.chat.ChatService.FT);
                            }
                            cv.put(FavouriteContract.FavouriteColumns.MIME_TYPE,
                                    message.basicMedia.fileType);
                        } else if (message.mediaType == Constants.MEDIA_TYPE_VCARD) {
                            /* subject = null */
                            file = new File(message.mediaPath);
                            int length = Constants.INVALID;
                            try {
                                length = message.text.getBytes("UTF-8").length;
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            cv.put(FavouriteContract.FavouriteColumns.SIZE, length);
                            if (message.direction == Constants.MESSAGE_DIRECTION_INCOMING) {
                                cv.put(FavouriteContract.FavouriteColumns.BODY, message.text);
                                cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                        com.cmcc.ccs.chat.ChatService.XML);
                            } else {
                                cv.put(FavouriteContract.FavouriteColumns.BODY, message.sourceId);
                                cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                        com.cmcc.ccs.chat.ChatService.FT);
                            }
                            cv.put(FavouriteContract.FavouriteColumns.MIME_TYPE, "text/vcard");
                        } else if (message.mediaType == Constants.MEDIA_TYPE_GEOLOC) {
                            /* subject = null */
                            file = new File(message.mediaPath);
                            int length = Constants.INVALID;
                            try {
                                length = message.text.getBytes("UTF-8").length;
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            cv.put(FavouriteContract.FavouriteColumns.SIZE, length);
                            if (message.direction == Constants.MESSAGE_DIRECTION_INCOMING) {
                                cv.put(FavouriteContract.FavouriteColumns.BODY,
                                        message.text);
                                cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                        com.cmcc.ccs.chat.ChatService.XML);
                            } else {
                                cv.put(FavouriteContract.FavouriteColumns.BODY, message.sourceId);
                                cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                        com.cmcc.ccs.chat.ChatService.FT);
                            }
                            cv.put(FavouriteContract.FavouriteColumns.MIME_TYPE,
                                   "application/vnd.gsma.rcspushlocation+xml");
                        } else if (message.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE ||
                                   message.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
                            if ((message.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                                    && mIndex != 0) ||
                                (message.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE &&
                                    (mIndex >= 5 || mIndex < 0))) {
                                Log.e(TAG, "Invalid index: " + mIndex);
                                try {
                                    mCallback.reportSetFavourite(mId,
                                            ResultCode.PARAM_ERROR_INVALID_FORMAT);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    // ignore
                                }
                                mWorkingTasks.remove(mId);
                                return;
                            }
                            MediaArticle article = message.article.get(mIndex);
                            cv.put(FavouriteContract.FavouriteColumns.SUBJECT, article.title);
                            cv.put(FavouriteContract.FavouriteColumns.PATH, article.bodyUrl);
                            /* size = null */
                            cv.put(FavouriteContract.FavouriteColumns.BODY, message.text);
                            cv.put(FavouriteContract.FavouriteColumns.MIME_TYPE,
                                    "application/article");
                            cv.put(FavouriteContract.FavouriteColumns.TYPE,
                                    com.cmcc.ccs.chat.ChatService.XML);
                        } else {
                            throw new Error("Invalid media type: " + message.mediaType);
                        }

                        if (file != null) {
                            if (!file.isFile()) {
                                Log.e(TAG, "there is no file = " + file.getAbsolutePath());
                                return;
                            }

                            File d = new File(FavouriteContract.MEDIA_FOLDER);
                            if (!d.isDirectory()) {
                                d.mkdirs();
                            }
                            String path = FavouriteContract.MEDIA_FOLDER
                                    + FavouriteContract.PA_PREFIX + file.getName();
                            try {
                                Utils.copyFile(file.getAbsolutePath(), path);
                                cv.put(FavouriteContract.FavouriteColumns.PATH, path);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            }
                        }

                        Uri uri = getContentResolver().insert(FavouriteColumns.CONTENT_URI, cv);
                        if (uri != null) {
                            long resultId = Long.parseLong(uri.getLastPathSegment());
                            Log.d(TAG, "Insert favourite message as " + resultId);
                            try {
                                mCallback.reportSetFavourite(mId, ResultCode.SUCCESS);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                // ignore
                            }
                        } else {
                            try {
                                mCallback.reportSetFavourite(mId, ResultCode.SERVICE_ERROR_UNKNOWN);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                // ignore
                            }
                        }
                    } else {
                        Log.e(TAG, "No such account: " + message.accountId);
                        try {
                            mCallback.reportSetFavourite(mId,
                                    ResultCode.PARAM_ERROR_INVALID_FORMAT);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            // ignore
                        }
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            } else {
                Log.e(TAG, "No such message: " + mMsgId);
                try {
                    mCallback.reportSetFavourite(mId, ResultCode.PARAM_ERROR_INVALID_FORMAT);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    // ignore
                }
            }

            mWorkingTasks.remove(mId);
        }
    }


    final class SendTask extends CancellableTask {

        long mMsgId;

        public SendTask(long requestId,
                long msgId, PASCallbackWrapper callback) {
            super(requestId, callback);

            mMsgId = msgId;
        }

        @Override
        public void doRun() {
            Log.d(TAG, "sendImage(" + mId + ")");

            PAMBaseMessage msg = mMessagePool.get(mMsgId);
            if (null == msg) {
                Log.e(TAG, "can't find msg:" + mMsgId + " in pool");
                return;
            }

            try {
                msg.send();
            } catch (JoynServiceException e) {
                e.printStackTrace();
                updateSendStatus(mMsgId, Constants.MESSAGE_STATUS_FAILED);
                reportSendFailed(mMsgId);
            }
        }
    }


    public class PASBinder extends IPAService.Stub {
        private static final String TAG = Constants.TAG_PREFIX + "PASBinder";

        // Callback Management
        @Override
        public synchronized long registerCallback(
                final IPAServiceCallback callback) throws RemoteException {
            if (callback == null) {
                throw new RemoteException("Illegal parameter: callback is null");
            }
            long token = generateToken();
            Log.d(TAG, "New Token: " + token + " for Callback: " + callback);
            mClientListeners.put(token, new PASCallbackWrapper(callback, token));
            return token;
        }

        @Override
        public synchronized void unregisterCallback(long token) throws RemoteException {
            mClientListeners.remove(token);
        }

        // Service States
        @Override
        public boolean isServiceConnected() throws RemoteException {
            return mRcseChatService.isServiceConnected() && mRcseFTService.isServiceConnected();
        }

        @Override
        public boolean isServiceRegistered() throws RemoteException {
            try {
                return mRcseChatService.isServiceRegistered()
                        && mRcseFTService.isServiceRegistered();
            } catch (JoynServiceException e) {
                throw new RemoteException(e.getLocalizedMessage());
            }
        }

        // Account Management
        @Override
        public synchronized long subscribe(long token, String id) throws RemoteException {
            long requestId = generateUuid();

            CancellableTask task = new SubscribeTask(
                    requestId,
                    id,
                    Constants.SUBSCRIPTION_STATUS_YES,
                    mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
            return requestId;
        }

        @Override
        public synchronized long unsubscribe(
                long token, String id) throws RemoteException {
            long requestId = generateUuid();
            CancellableTask task = new SubscribeTask(
                    requestId,
                    id,
                    Constants.SUBSCRIPTION_STATUS_NO,
                    mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
            return requestId;
        }

        @Override
        public synchronized long getSubscribedList(
                long token,
                int order,
                int pageSize,
                int pageNumber) throws RemoteException {
            long requestId = generateUuid();

            CancellableTask task = new GetSubscribedListTask(
                    requestId,
                    order,
                    pageSize,
                    pageNumber,
                    false,
                    false,
                    mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public synchronized long getDetails(long token, String uuid,
                String timestamp) throws RemoteException {
            long requestId = generateUuid();

            CancellableTask task = new GetDetailsTask(
                    requestId,
                    uuid,
                    timestamp,
                    mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public synchronized long getMenu(long token,
                String uuid, String timestamp) throws RemoteException {
            long requestId = generateUuid();

            CancellableTask task = new GetMenuTask(
                    requestId, uuid, timestamp, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public synchronized long downloadObject(long token, long requestId,
                String url, int type) throws RemoteException {

            Log.d(TAG, "downloadObject(): token=" + token + ". requestId="
                + requestId + ". url=" + url + ". type=" + type);
            long cancelId = generateUuid();
            synchronized (mDownloadingTasks) {
                DownloadTask task = mDownloadingTasks.get(url);
                if (task != null) {
                    Log.d(TAG, "add callback to exist download task");
                    task.addCallback(token, cancelId, requestId);
                    return cancelId;
                }
            }

            Log.d(TAG, "add a new downloadTask.");
            DownloadTask task = new DownloadTask(
                cancelId, requestId, url, type, mClientListeners.get(token));
            mDownloadingTasks.put(url, task);
            task.doDownload();
            return cancelId;
        }

        @Override
        public synchronized void cancelDownload(long cancelId) throws RemoteException {
            Log.d(TAG, "cancelDownload(" + cancelId + ")");

            synchronized (mDownloadingTasks) {
                Iterator<Entry<String, DownloadTask>> iter = mDownloadingTasks
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, DownloadTask> entry = (Map.Entry<String, DownloadTask>) iter
                        .next();
                    DownloadTask task = entry.getValue();
                    if (task.contains(cancelId)) {
                        Log.d(TAG, "cancelDownload() cancelId=" + cancelId
                            + ". url=" + entry.getKey());
                        task.cancel(cancelId);
                        break;
                    }
                }
            }
        }

        // Messaging
        @Override
        public long sendMessage(long token, long accountId,
                String message, boolean system) throws RemoteException {
            Log.d(TAG, "sendMessage(" + accountId + ", " + message + ", " + system + ")");

            PAMTextMessage msg = new PAMTextMessage(PAServiceImpl.this,
                    token, accountId, system, message, new PAMMessageHelper());
            long msgId = msg.getMessageId();
            if (msgId > Constants.INVALID) {
                mMessagePool.put(msg.getMessageId(), msg);
                try {
                    msg.send();
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                    updateSendStatus(msgId, Constants.MESSAGE_STATUS_FAILED);
                    reportSendFailed(msgId);
                }
            }
            return msgId;
        }

        public long sendImage(final long token, long accountId,
                String path, String thumbnailPath) throws RemoteException {

            long requestId = generateUuid();

            final PAMImageMessage msg = new PAMImageMessage(PAServiceImpl.this,
                    token, accountId, path, thumbnailPath, new PAMMessageHelper());

            final long msgId = msg.getMessageId();
            if (msgId > Constants.INVALID) {
                mMessagePool.put(msgId, msg);
            }

            SendTask task = new SendTask(requestId, msgId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        public long sendAudio(final long token, long accountId,
                String path, int duration) throws RemoteException {

            long requestId = generateUuid();

            final PAMAudioMessage msg = new PAMAudioMessage(PAServiceImpl.this,
                    token, accountId, path, duration, new PAMMessageHelper());

            final long msgId = msg.getMessageId();
            if (msgId > Constants.INVALID) {
                mMessagePool.put(msgId, msg);
            }

            SendTask task = new SendTask(requestId, msgId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        public long sendVideo(final long token, long accountId, String path,
                String thumbnailPath, int duration) throws RemoteException {

            long requestId = generateUuid();

            final PAMVideoMessage msg = new PAMVideoMessage(PAServiceImpl.this, token,
                    accountId, path, thumbnailPath, duration, new PAMMessageHelper());

            final long msgId = msg.getMessageId();
            if (msgId > Constants.INVALID) {
                mMessagePool.put(msgId, msg);
            }

            SendTask task = new SendTask(requestId, msgId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long sendVcard(final long token, long accountId,
                String data) throws RemoteException {

            long requestId = generateUuid();

            final PAMVcardMessage msg = new PAMVcardMessage(PAServiceImpl.this, token,
                    accountId, data, new PAMMessageHelper());

            final long msgId = msg.getMessageId();
            if (msgId > Constants.INVALID) {
                mMessagePool.put(msgId, msg);
            }

            SendTask task = new SendTask(requestId, msgId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long sendGeoLoc(final long token, long accountId,
                String data) throws RemoteException {

            long requestId = generateUuid();

            final PAMGeolocMessage msg = new PAMGeolocMessage(PAServiceImpl.this, token,
                    accountId, data, new PAMMessageHelper());

            final long msgId = msg.getMessageId();
            if (msgId > Constants.INVALID) {
                mMessagePool.put(msgId, msg);
            }

            SendTask task = new SendTask(requestId, msgId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public void complainSpamMessage(
                final long token, final long messageId) throws RemoteException {

            PAMBaseMessage msg = PAMBaseMessage.generateMessageFromDB(
                        PAServiceImpl.this, token, messageId, new PAMMessageHelper());

            try {
                msg.complain();
            } catch (JoynServiceException e) {
                // FIXME check for errors
                e.printStackTrace();
                throw new RemoteException(e.getLocalizedMessage());
            }
        }

        @Override
        public long setAcceptStatus(
                long token,
                String uuid,
                int acceptStatus) throws RemoteException {

            long requestId = generateUuid();
            final class SetAcceptStatusTask extends CancellableTask {
                private final String mUuid;
                private final int mAcceptStatus;

                public SetAcceptStatusTask(long requestId, String uuid,
                        int acceptStatus, PASCallbackWrapper callback) {
                    super(requestId, callback);
                    mUuid = uuid;
                    mAcceptStatus = acceptStatus;
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "setAcceptStatus(" + mId + ", "
                            + mUuid + ", " + mAcceptStatus + ")");
                    int result = ResultCode.SUCCESS;
                    try {
                        result = mClient.setAcceptStatus(mUuid, mAcceptStatus);
                        if (result == ResultCode.SUCCESS) {
                            ContentResolver cr = getContentResolver();
                            ContentValues cv = new ContentValues();
                            cv.put(AccountColumns.ACCEPT_STATUS, mAcceptStatus);
                            cr.update(
                                    AccountColumns.CONTENT_URI,
                                    cv,
                                    AccountColumns.UUID + "=?",
                                    new String[]{mUuid});
                        }
                    } catch (PAMException e) {
                        result = e.resultCode;
                    }
                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportSetAcceptStatusResult(mId, result);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e(TAG, "remove callback " + mCallback.getToken());
                            mClientListeners.remove(mCallback.getToken());
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            SetAcceptStatusTask task = new SetAcceptStatusTask(
                    requestId, uuid, acceptStatus, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public void resendMessage(long token, long messageId) throws RemoteException {
            Log.d(TAG, "resendMessage(" + messageId + ")");
            PAMBaseMessage msg = mMessagePool.get(messageId);
            if (null == msg) {
                //todo construct message
                msg = PAMBaseMessage.generateMessageFromDB(
                        PAServiceImpl.this, token, messageId, new PAMMessageHelper());
                mMessagePool.put(messageId, msg);
            }
            try {
                resendMessageInternal(messageId);
            } catch (JoynServiceException e) {
                updateSendStatus(messageId, Constants.MESSAGE_STATUS_FAILED);
                reportSendFailed(messageId);
            }
        }

        @Override
        public boolean deleteMessage(long token, long messageId) throws RemoteException {
            Log.d(TAG, "deleteMessage(" + messageId + ")");
            // Mark deleted
            ContentValues cv = new ContentValues();
            cv.put(MessageColumns.DELETED, Constants.DELETED_YES);
            int updateCount = getContentResolver().update(
                    MessageColumns.CONTENT_URI,
                    cv,
                    MessageColumns.ID + "=?",
                    new String[]{Long.toString(messageId)});
            deleteMessagesInBackground(new long[] {messageId});
            return (updateCount == 1);
        }

        @Override
        public long deleteMessageByAccount(
                long token, final long accountId) throws RemoteException {
            Log.d(TAG, "deleteMessageByAccount(" + accountId + ")");
            long requestId = generateUuid();
            final class BatchDeleteTask extends CancellableTask {
                public BatchDeleteTask(long requestId, PASCallbackWrapper callback) {
                    super(requestId, callback);
                }

                @Override
                public void doRun() {
                    Log.d(TAG, "deleteMessageByAccount(" + mId + ", " + accountId + ")");
                    int result = ResultCode.SUCCESS;
                    Cursor c = null;
                    try {
                        c = getContentResolver().query(
                                MessageColumns.CONTENT_URI,
                                new String[]{MessageColumns.ID},
                                MessageColumns.ACCOUNT_ID + "=?",
                                new String[]{Long.toString(accountId)},
                                null);
                        if (c != null && c.getCount() > 0) {
                            // Collect message IDs
                            long[] messageIds = new long[c.getCount()];
                            c.moveToFirst();
                            int index = c.getColumnIndexOrThrow(MessageColumns.ID);
                            for (int i = 0; i < c.getCount(); ++i) {
                                messageIds[i] = c.getLong(index);
                                c.moveToNext();
                            }
                            // Mark deleted
                            ContentValues cv = new ContentValues();
                            cv.put(MessageColumns.DELETED, Constants.DELETED_YES);
                            int updateCount = getContentResolver().update(
                                    MessageColumns.CONTENT_URI,
                                    cv,
                                    MessageColumns.ACCOUNT_ID + "=? AND " +
                                            MessageColumns.DELETED + "!=" + Constants.DELETED_YES,
                                    new String[]{Long.toString(accountId)});
                            // Sanity check
                            if (updateCount != messageIds.length) {
                                throw new Error(
                                        "Failed to mark deleted flags: " + messageIds.length +
                                            " to mark, " + updateCount + " done");
                            }
                            // Delete in background
                            deleteMessagesInBackground(messageIds);
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    if (mCallback != null) {
                        if (isCancelled()) {
                            result = getCancelReason();
                        }
                        try {
                            mCallback.reportDeleteMessageResult(mId, result);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e(TAG, "remove callback " + mCallback.getToken());
                            mClientListeners.remove(mCallback.getToken());
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            CancellableTask task = new BatchDeleteTask(requestId, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
            return requestId;
        }

        @Override
        public void registerAck(long token) throws RemoteException {
            Log.d(TAG, "registerAck(" + token + ")");
            final PASCallbackWrapper callback = mClientListeners.get(token);
            if (callback == null) {
                Log.e(TAG, "Callback " + token + " not found");
                return;
            }
            callback.setActive();
            if (mRcseChatService.isServiceConnected() && mRcseFTService.isServiceConnected()) {
                Log.d(TAG, "Instantly connected");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onServiceConnected();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.e(TAG, "remove callback " + callback.getToken());
                            mClientListeners.remove(callback.getToken());
                        }
                    }
                });
                try {
                    if (mRcseChatService.isServiceRegistered()
                            && mRcseFTService.isServiceRegistered()) {
                        Log.d(TAG, "Instantly registered");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    callback.onServiceRegistered();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "remove callback " + callback.getToken());
                                    mClientListeners.remove(callback.getToken());
                                }
                            }
                        });
                    }
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                    throw new RemoteException(e.getLocalizedMessage());
                }
            }

        }

        @Override
        public long setFavourite(long token, long messageId, int index) throws RemoteException {
            long requestId = generateUuid();
            SetFavTask task = new SetFavTask(
                    requestId, messageId, index, mClientListeners.get(token));
            mWorkingTasks.put(requestId, task);
            mExecutorService.submit(task);
            return requestId;
        }

        @Override
        public long getMaxFileTransferSize() throws RemoteException {
            long maxSize = 0;
            try {
                FileTransferServiceConfiguration ftConfig = mRcseFTService.getConfiguration();
                maxSize = ftConfig.getMaxSize();
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
            return maxSize;
        }
    }

    private void updateSendStatus(long msgId, int status) {
        Log.d(TAG, "updateSendStatus. id=" + msgId + ", status=" + status);
        mMessagePool.get(msgId).updateStatus(status);
        //if (PlatformManager.getInstance().supportCcs()) {
        //    updateCcsMessageStatus(msgId,  status);
        //}
    }

    private void reportSendSuccess(long msgId) {
        Log.e(TAG, "reportSendSuccess()  msgId=" + msgId);
        PAMBaseMessage msg = mMessagePool.get(msgId);
        if (null == msg) {
            Log.e(TAG, "failed to find msg in MessagePool. msgId=" + msgId);
            return;
        }
        mMessagePool.remove(msgId);
        long token = msg.getToken();
        msg.onSendOver();
        try {
            if (token == SERVICE_TOKEN) {
                for (IPAServiceCallback listener : mClientListeners.values()) {
                    listener.onReportMessageDelivered(msgId);
                }
            } else if (null != mClientListeners.get(token)) {
                mClientListeners.get(token).onReportMessageDelivered(msgId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e(TAG, "remove callback " + token);
            mClientListeners.remove(token);
        }
    }

    private void reportSendFailed(long msgId) {
        Log.d(TAG, "reportSendFailed() msgId=" + msgId);
        PAMBaseMessage msg = mMessagePool.get(msgId);
        if (null == msg) {
            Log.e(TAG, "failed to find msg in MessagePool. msgId=" + msgId);
            return;
        }
        mMessagePool.remove(msgId);
        long token = msg.getToken();
        msg.onSendOver();
        try {
            if (SERVICE_TOKEN == token) {
                for (IPAServiceCallback listener : mClientListeners.values()) {
                    listener.onReportMessageFailed(msgId);
                }
            } else if (null != mClientListeners.get(token)) {
                mClientListeners.get(token).onReportMessageFailed(msgId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e(TAG, "remove callback " + token);
            mClientListeners.remove(token);
        }
    }

    private void deleteMessagesInBackground(final long[] messageIds) {
       class DeleteMessageTask extends CancellableTask {
            private static final int BATCH_SIZE = 5;

            public DeleteMessageTask(long requestId, PASCallbackWrapper callback) {
                super(requestId, callback, true);
            }

            @Override
            protected void doRun() {
                StringBuilder sb = new StringBuilder("deleteMessagesInBackground(");
                for (long id : messageIds) {
                    sb.append(id).append(", ");
                }
                sb.append(")");
                Log.d(TAG, sb.toString());
                for (int i = 0; i < messageIds.length; ++i) {
                    deleteFullMessageContent(messageIds[i]);
                    if ((i % BATCH_SIZE) == 0) {
                        Thread.yield();
                    }
                }
                mWorkingTasks.remove(mId);
            }
        }
        long requestId = generateUuid();
        DeleteMessageTask task = new DeleteMessageTask(requestId, null);
        mWorkingTasks.put(requestId, task);
        mExecutorService.submit(task);
    }

    private void markSendingMessageAsFailed() {
        Log.d(TAG, "markSendingMessageAsFailed");

        ContentValues cv = new ContentValues();
        cv.put(MessageColumns.STATUS, Constants.MESSAGE_STATUS_FAILED);
        int count = getContentResolver().update(
                MessageColumns.CONTENT_URI,
                cv,
                MessageColumns.STATUS + "=? OR " + MessageColumns.STATUS + "=?",
                new String[] {
                        Integer.toString(Constants.MESSAGE_STATUS_TO_SEND),
                        Integer.toString(Constants.MESSAGE_STATUS_SENDING)});
        Log.d(TAG, "Total " + count + " messages are marked as failed.");
        //if (PlatformManager.getInstance().supportCcs()) {
        //    markCcsSendingMessageAsFailed();
        //}
    }

    /*
    private void markCcsSendingMessageAsFailed() {
        ContentValues cv = new ContentValues();
        cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, com.cmcc.ccs.chat.ChatMessage.FAILED);
        int count = getContentResolver().update(
                CCS_MESSAGE_CONTENT_URI,
                cv,
                com.cmcc.ccs.chat.ChatMessage.FLAG + "=? AND (" +
                        com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS +
                        "=? OR " + com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS + "=?)",
                new String[] {
                        Integer.toString(com.cmcc.ccs.chat.ChatMessage.PUBLIC),
                        Integer.toString(com.cmcc.ccs.chat.ChatMessage.TO_SEND),
                        Integer.toString(com.cmcc.ccs.chat.ChatMessage.SENDING)});
        Log.d(TAG, "Mark " + count + " ccs messages as failed.");
    }
    */

    private void deleteFullMessageContent(long messageId) {
        Log.d(TAG, "deleteFullMessageContent(" + messageId + ")");

        ContentResolver cr = getContentResolver();
        MessageContent messageContent = MessageContent.loadFromProvider(messageId, cr, true);
        //if (PlatformManager.getInstance().supportCcs()) {
        //     deleteFromCcsMessageProvider(messageContent);
        //}
        messageContent.deleteFromProvider(getContentResolver());
    }

    private void resendMessageInternal(final long messageId) throws JoynServiceException {

        final PAMBaseMessage msg = mMessagePool.get(messageId);
        if (null == msg) {
            Log.e(TAG, "resendMessageInternal() but no such message:" + messageId);
            return;
        }

        if (msg.readyForSend()) {
            msg.resend();
        } else {
            long timespan = System.currentTimeMillis() - msg.getStartTimeStamp();
            if (timespan > 0 && timespan < PAMBaseMessage.RECOVER_TIMEOUT) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "Auto recover msgId: " + messageId);
                            resendMessageInternal(messageId);
                        } catch (JoynServiceException e) {
                            e.printStackTrace();
                            updateSendStatus(messageId, Constants.MESSAGE_STATUS_FAILED);
                            reportSendFailed(messageId);
                        }
                    }
                }, PAMBaseMessage.RECOVER_INTERVAL);
            }  else {
                Log.e(TAG, "Fail to resend msg " + messageId + " for timeout");
                updateSendStatus(messageId, Constants.MESSAGE_STATUS_FAILED);
                reportSendFailed(messageId);
            }
        }
    }

    private void deleteMessage(long messageId) {
        ContentResolver cr = getContentResolver();
        cr.delete(
                PAContract.MessageColumns.CONTENT_URI,
                PAContract.MessageColumns.ID + "=?",
                new String[]{Long.toString(messageId)});
    }

    private void deleteMessageInRCSe(String sourceId) {
        ContentResolver cr = getContentResolver();
        cr.delete(
                RcseProviderContract.MessageColumns.CONTENT_URI,
                RcseProviderContract.MessageColumns.MESSAGE_ID + "=?",
                new String[]{sourceId});
    }

    private long copyMessageFromChatProvider(String sourceId) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        long msgId = Constants.INVALID;
        try {
            c = cr.query(
                    RcseProviderContract.MessageColumns.CONTENT_URI,
                    RcseProviderContract.MESSAGE_FULL_PROJECTION,
                    RcseProviderContract.MessageColumns.MESSAGE_ID + "=?",
                    new String[]{sourceId},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                MessageContent message = MessageContent.
                        buildFromRcseMessageProviderCursor(this, c);
                if (message.mediaType > Constants.MEDIA_TYPE_SMS) {
                    handleCommandMessage(message);
                } else {
                    msgId = message.storeToProvider(cr, PAServiceImpl.this);
                    Log.d(TAG, "copyMessageFromChatProvider() with new msgId:" + msgId);
                }

            } else {
                Log.e(TAG, "copyMessageFromChatProvider() but No message: " + sourceId);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return msgId;
    }

    private void handleCommandMessage(MessageContent message) {
        switch(message.mediaType) {
        case Constants.MEDIA_TYPE_SUBSCRIBED_LIST_CHANGED:
            getSubscribedInBackground();
            break;
        case Constants.MEDIA_TYPE_ACCOUNT_DETAIL_CHANGED:
            String uuid = PublicAccount.queryAccountUuid(this, message.accountId);
            try {
                mPASBinder.getDetails(Constants.INVALID, uuid, null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            break;
        default:
            throw new IllegalStateException("unknow media type");

        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        mHandler = new Handler();
        mPASBinder = new PASBinder();
        mClientListeners = new ConcurrentHashMap<Long, PASCallbackWrapper>();
        mChats = new LruCache<String, PublicAccountChat>(10);

        mMessagePool = new ConcurrentHashMap<Long, PAMBaseMessage>();
        if (null == mRcseChatService) {
            mRcseChatService = new ChatService(this, new PAMJoynChatServiceListener());
        }
        if (null == mRcseFTService) {
            mRcseFTService = new FileTransferService(this, new PAMJoynFTServiceListener());
        }

        PlatformManager pm = PlatformManager.getInstance();
        mClient = new PAMClient(
                pm.getTransmitter(this),
                this);
        mExecutorService = Executors.newFixedThreadPool(15);
        mDownloadService = DownloadService.getInstance(this);
        mWorkingTasks = new ConcurrentHashMap<Long, CancellableTask>();
        mDownloadingTasks = new ConcurrentHashMap<String, DownloadTask>();
    }

    private void notifyListenersOfServiceConnectedEvent() {
        Log.d(TAG, "notifyListenersOfServiceConnectedEvent");
        for (PASCallbackWrapper listener : mClientListeners.values()) {
            try {
                listener.onServiceConnected();
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback" + listener.getToken());
                mClientListeners.remove(listener.getToken());
            }
        }
        try {
            if (mRcseChatService.isServiceRegistered() && mRcseFTService.isServiceRegistered()) {
                Log.d(TAG, "RCSe Chat and FT are both already registered." +
                        " Send instant notification.");
                notifyListenersOfServiceRegisteredEvent();
            }
        } catch (JoynServiceException e) {
            throw new Error(e);
        }
    }

    private void notifyListenersOfServiceDisconnectedEvent(int reason) {
        for (PASCallbackWrapper listener : mClientListeners.values()) {
            try {
                listener.onServiceDisconnected(reason);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback" + listener.getToken());
                mClientListeners.remove(listener.getToken());
            }
        }
        if (!PlatformManager.getInstance().isRcsServiceActivated(this)) {
            ActivityManager ams = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = ams.getAppTasks();
            for (ActivityManager.AppTask task : tasks) {
                task.finishAndRemoveTask();
            }
        }
    }

    private void notifyListenersOfServiceRegisteredEvent() {
        Log.d(TAG, "notifyListenersOfServiceRegisteredEvent");
        for (PASCallbackWrapper listener : mClientListeners.values()) {
            try {
                listener.onServiceRegistered();
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback" + listener.getToken());
                mClientListeners.remove(listener.getToken());
            }
        }
        if (!isInitialized()) {
            getSubscribedInBackground();
        }
    }

    private void notifyListenersOfServiceUnregisteredEvent() {
        for (PASCallbackWrapper listener : mClientListeners.values()) {
            try {
                listener.onServiceUnregistered();
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "remove callback" + listener.getToken());
                mClientListeners.remove(listener.getToken());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Check the intents and permissions.
        Log.d(TAG, "PAServiceImpl onBind()");
        if (!mRcseChatService.isServiceConnected()) {
            mRcseChatService.connect();
        }
        if (!mRcseFTService.isServiceConnected()) {
            mRcseFTService.connect();
        }

        return mPASBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        final String action = intent.getAction();

        Log.d(TAG, "onStartCommand() action=" + action);

        if (PAMReceiver.ACTION_NEW_MESSAGE.equals(action)) {
            Bundle extras = intent.getExtras();
            Set<String> keys = extras.keySet();
            for (String key:keys) {
                Log.i(TAG, "Key: " + key);
            }
            String accountUuid = intent.getStringExtra(ACTION_KEY_ACCOUNT);
            accountUuid = Utils.extractUuidFromSipUri(accountUuid);
            ChatMessage message = (ChatMessage)intent.getParcelableExtra(ACTION_KEY_CHAT_MESSAGE);
            if (null == message) {
                Log.d(TAG, "Message is null. Ignore.");
                return START_STICKY;
            }
            if (!message.isPublicMessage()) {
                Log.d(TAG, "Not a public message. Ignore.");
                return START_STICKY;
            }
            Log.d(TAG, "receive new message content: " + message.getMessage());
            onReceiveNewMessage(accountUuid, message);

        } else if (PAMReceiver.ACTION_RCS_ACCOUNT_CHANGED.equals(action)) {
            // FIXME
            // 0. Cancel all tasks and tell clients the reason
            //(this part should be done by each task)
            for (CancellableTask task : mWorkingTasks.values()) {
                task.cancel();
            }
            // 1. Ask clients to quit
            for (PASCallbackWrapper listener : mClientListeners.values()) {
                try {
                    listener.onAccountChanged(PlatformManager.getInstance().getIdentity(this));
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback" + listener.getToken());
                    mClientListeners.remove(listener.getToken());
                }
            }
            // 2. Clean state
            // Reset PA
            Log.d(TAG, "Account Changed. Delete all data and exit.");
            deleteAllData();
            // relaunch in 5s
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(ACTION_RESTART);
            i.setClass(this, PAServiceImpl.class);
            PendingIntent pi = PendingIntent.getService(
                    this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);
            Process.killProcessQuiet(Process.myPid());
        } else if (PAMReceiver.ACTION_JOYN_UP.equals(action)) {
            Log.d(TAG, "Joyn Service is up. We get up!");
            if (mRcseChatService != null && !mRcseChatService.isServiceConnected()) {
                mRcseChatService.connect();
            }
            if (mRcseFTService != null && !mRcseFTService.isServiceConnected()) {
                mRcseFTService.connect();
            }
            markSendingMessageAsFailed();

            return START_STICKY;
        } else if (ACTION_RESTART.equals(action)) {
            Log.d(TAG, "PAService is relaunched.");
            mRcseChatService.connect();
            mRcseFTService.connect();
            markSendingMessageAsFailed();

            return START_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestory()");
        if (mRcseChatService != null) {
            mRcseChatService.disconnect();
        }
        if (mRcseFTService != null) {
            mRcseFTService.disconnect();
        }
        if (mDownloadService !=null) {
            mDownloadService.stop();
        }
    }

    private void onReceiveNewMessage(String uuid, ChatMessage message) {
        long accountId = PublicAccount.queryAccountId(PAServiceImpl.this, uuid, true);
        // All incoming message saved in Chat DB, FT DB is no use.
        long messageId = copyMessageFromChatProvider(message.getId());
        if (messageId != Constants.INVALID) {
            for (PASCallbackWrapper listener : mClientListeners.values()) {
                try {
                    listener.onNewMessage(accountId, messageId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "remove callback" + listener.getToken());
                    mClientListeners.remove(listener.getToken());
                }
            }
        }
    }

    private void deleteAllData() {
        File dbFile = getDatabasePath(PAProvider.DATABASE_NAME);
        File dbJournalFile = getDatabasePath(PAProvider.DATABASE_NAME + "-journal");
        dbFile.delete();
        dbJournalFile.delete();
    }

    private long getMessageIdFromSourceId(String sourcdId) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    Uri.parse(
                            MessageColumns.CONTENT_URI_STRING + "?" +
                            PAContract.MESSAGES_PARAM_INCLUDING_SYSTEM +
                            "=" + Constants.IS_SYSTEM_YES),
                    new String[] {MessageColumns.ID},
                    MessageColumns.SOURCE_ID + "=?",
                    new String[]{sourcdId},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getLong(c.getColumnIndexOrThrow(MessageColumns.ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return Constants.INVALID;
    }

    private boolean isInitialized() {
        boolean result = false;
        Cursor c = null;
        try {
            c = getContentResolver().query(StateColumns.CONTENT_URI, null, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int value = c.getInt(c.getColumnIndexOrThrow(StateColumns.INITIALIZED));
                result = (value == Constants.INIT_YES);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    private void setInitialized(boolean flag) {
        Log.d(TAG, "setInitialized(" + flag + ")");
        ContentValues cv = new ContentValues();
        cv.put(StateColumns.INITIALIZED, flag ? Constants.INIT_YES : Constants.INIT_NO);
        int result = getContentResolver().update(StateColumns.CONTENT_URI, cv, null, null);
        Log.d(TAG, "update result is " + result);
    }

    private void getSubscribedInBackground() {
        if (mIsGettingSubscribedList) {
            Log.d(TAG, "Duplicate getSubscribed. Ignore.");
            return;
        }
        mIsGettingSubscribedList = true;
        long requestId = generateUuid();
        CancellableTask task = new GetSubscribedListTask(
                requestId,
                Constants.ORDER_BY_NAME,
                GetSubscribedListTask.DEFAULT_BATCH_SIZE,
                1,
                true,
                true,
                null);
        mWorkingTasks.put(requestId, task);
        mExecutorService.submit(task);
    }

    /*
    //Ruoyao: duplicate func
    private String getUuidFromAccountId(long accountId) {
        String uuid = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    new String[]{AccountColumns.UUID},
                    AccountColumns.ID + "=?",
                    new String[]{Long.toString(accountId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                uuid = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return uuid;
    }

    private static final Uri CCS_MESSAGE_CONTENT_URI = Uri.parse("content://com.cmcc.ccs.message");

    private void updateCcsMessageProvider(MessageContent messageContent) {
        ContentValues cv = new ContentValues();
        cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID, messageContent.sourceId);
        cv.put(com.cmcc.ccs.chat.ChatMessage.FLAG, com.cmcc.ccs.chat.ChatMessage.PUBLIC);
        cv.put(com.cmcc.ccs.chat.ChatMessage.CONTACT_NUMBER,
                getUuidFromAccountId(messageContent.accountId));
        cv.put(com.cmcc.ccs.chat.ChatMessage.TIMESTAMP, messageContent.timestamp);
        cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, messageContent.status);
        cv.put(com.cmcc.ccs.chat.ChatMessage.DIRECTION,
               (messageContent.direction == Constants.MESSAGE_DIRECTION_INCOMING ?
                        com.cmcc.ccs.chat.ChatMessage.INCOMING :
                        com.cmcc.ccs.chat.ChatMessage.OUTCOMING));

        // Set type, mime_type and body according to mediaType
        if (messageContent.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE ||
            messageContent.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.XML);
            cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.buildMediaArticleString());
            cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, "application/xml");
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_TEXT) {
            cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.IM);
            cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.text);
            cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, "text/plain");
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_AUDIO ||
                   messageContent.mediaType == Constants.MEDIA_TYPE_PICTURE ||
                   messageContent.mediaType == Constants.MEDIA_TYPE_VIDEO) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.FT);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.sourceId);
                cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, messageContent.basicMedia.fileType);
            } else {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.XML);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.basicMedia.originalUrl);
                cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE, messageContent.basicMedia.fileType);
            }
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_GEOLOC ||
                   messageContent.mediaType == Constants.MEDIA_TYPE_VCARD) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.FT);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.sourceId);
            } else {
                cv.put(com.cmcc.ccs.chat.ChatMessage.TYPE, com.cmcc.ccs.chat.ChatService.XML);
                cv.put(com.cmcc.ccs.chat.ChatMessage.BODY, messageContent.text);
            }
            cv.put(com.cmcc.ccs.chat.ChatMessage.MIME_TYPE,
                   (messageContent.mediaType == Constants.MEDIA_TYPE_GEOLOC ?
                           "application/vnd.gsma.rcspushlocation+xml" :
                           "text/vcard"));
        }
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    CCS_MESSAGE_CONTENT_URI,
                    new String[] {
                            com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID,
                            com.cmcc.ccs.chat.ChatMessage.TYPE},
                    com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND "
                            + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                    new String[] {
                        cv.getAsString(com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID),
                        Integer.toString(cv.getAsInteger(com.cmcc.ccs.chat.ChatMessage.TYPE))},
                    null);
            if (c != null && c.getCount() > 0) {
                // update
                getContentResolver().update(
                        CCS_MESSAGE_CONTENT_URI,
                        cv,
                        com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND "
                        + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                        new String[] {
                                cv.getAsString(com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID),
                                Integer.toString(cv.getAsInteger(
                                        com.cmcc.ccs.chat.ChatMessage.TYPE))});
            } else {
                // insert
                getContentResolver().insert(CCS_MESSAGE_CONTENT_URI, cv);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void updateCcsMessageStatus(long messageId, int status) {
        Pair<String, Integer> pair = getCombinedKeyForCcsMessage(messageId);
        if (pair != null) {
            ContentValues cv = new ContentValues();
            cv.put(com.cmcc.ccs.chat.ChatMessage.MESSAGE_STATUS, status);
            getContentResolver().update(
                    CCS_MESSAGE_CONTENT_URI,
                    cv,
                    com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND "
                    + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                    new String[] { pair.first, Integer.toString(pair.second)});
        }
    }

    private void deleteFromCcsMessageProvider(MessageContent messageContent) {
        Pair<String, Integer> pair = getCombinedKeyForCcsMessage(messageContent);
        getContentResolver().delete(
                CCS_MESSAGE_CONTENT_URI,
                com.cmcc.ccs.chat.ChatMessage.MESSAGE_ID + "=? AND "
                + com.cmcc.ccs.chat.ChatMessage.TYPE + "=?",
                new String[] { pair.first, Integer.toString(pair.second) });
    }

    private Pair<String, Integer> getCombinedKeyForCcsMessage(MessageContent messageContent) {
        int type = -1;
        if (messageContent.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                || messageContent.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            type = com.cmcc.ccs.chat.ChatService.XML;
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_TEXT) {
            type = com.cmcc.ccs.chat.ChatService.IM;
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_AUDIO
                || messageContent.mediaType == Constants.MEDIA_TYPE_PICTURE
                || messageContent.mediaType == Constants.MEDIA_TYPE_VIDEO) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        } else if (messageContent.mediaType == Constants.MEDIA_TYPE_GEOLOC
                || messageContent.mediaType == Constants.MEDIA_TYPE_VCARD) {
            if (messageContent.direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        }
        return new Pair<String, Integer>(messageContent.sourceId, type);
    }

    private Pair<String, Integer> getCombinedKeyForCcsMessage(long messageId) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    MessageColumns.CONTENT_URI,
                    new String[] {
                        MessageColumns.SOURCE_ID,
                        MessageColumns.TYPE,
                        MessageColumns.DIRECTION,
                    },
                    MessageColumns.ID + "=?",
                    new String[] {Long.toString(messageId)},
                    null);
            if (c != null && c.getCount() > 0) {
                return getCombinedKeyForCcsMessage(c);
            } else {
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private Pair<String, Integer> getCombinedKeyForCcsMessage(Cursor c) {
        String sourceId = c.getString(c.getColumnIndexOrThrow(MessageColumns.SOURCE_ID));
        int mediaType = c.getInt(c.getColumnIndexOrThrow(MessageColumns.TYPE));
        int direction = c.getInt(c.getColumnIndexOrThrow(MessageColumns.DIRECTION));
        int type = -1;
        if (mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                || mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            type = com.cmcc.ccs.chat.ChatService.XML;
        } else if (mediaType == Constants.MEDIA_TYPE_TEXT) {
            type = com.cmcc.ccs.chat.ChatService.IM;
        } else if (mediaType == Constants.MEDIA_TYPE_AUDIO
                || mediaType == Constants.MEDIA_TYPE_PICTURE
                || mediaType == Constants.MEDIA_TYPE_VIDEO) {
            if (direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        } else if (mediaType == Constants.MEDIA_TYPE_GEOLOC
                || mediaType == Constants.MEDIA_TYPE_VCARD) {
            if (direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
                type = com.cmcc.ccs.chat.ChatService.FT;
            } else {
                type = com.cmcc.ccs.chat.ChatService.XML;
            }
        }
        return new Pair<String, Integer>(sourceId, type);
    }
*/
}
