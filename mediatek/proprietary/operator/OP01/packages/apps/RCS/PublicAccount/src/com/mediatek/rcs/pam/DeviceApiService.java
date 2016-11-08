package com.mediatek.rcs.pam;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.cmcc.ccs.publicaccount.PublicService;
import com.mediatek.rcs.pam.client.PAMClient;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.CcsSearchColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.gsma.joyn.JoynService;

public class DeviceApiService extends Service {
    private static final String TAG = Constants.TAG_PREFIX + "DeviceApiService";

    @Override
    public IBinder onBind(Intent intent) {
        return new DeviceApiBinder();
    }

    private static final int TIMEOUT = 5; // 30s

    private PAMClient mClient;
    private PAServiceImpl.PASBinder mPASBinder;
    private IPAServiceCallback mCallback;

    private long mToken;
    private Map<Long, String> mPendingPARequests;
    private Map<Long, BlockingQueue<Long>> mPendingRequests;
    private LongSparseArray<IDeviceApiServiceCallback> mEventListeners;
    private LongSparseArray<IDeviceApiServiceCallback> mTempListeners;

    private long mUuidCounter = 0;
    private long mTokenCounter = 0;
    private ExecutorService mExecutorService;
    private ConcurrentHashMap<Long, RequestedTask> mWorkingTasks;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected, ComponentName = " + name);
            for (int i = 0; i < mEventListeners.size(); i++) {
                final IDeviceApiServiceCallback callback = mEventListeners.valueAt(i);
                try {
                    callback.onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
                } catch (RemoteException e) {
                    mEventListeners.remove(i);
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected, ComponentName = " + name);

            mPASBinder = (PAServiceImpl.PASBinder) service;
            try {
                mToken = mPASBinder.registerCallback(mCallback);
                mPASBinder.registerAck(mToken);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private abstract class RequestedTask implements Runnable {
        protected final long mId;

        public RequestedTask(long requestId) {
            mId = requestId;
        }
    }

    public static final String READ_PERMISSION = "com.cmcc.ccs.READ_PUBLICACCOUNT";
    public static final String WRITE_PERMISSION = "com.cmcc.ccs.WRITE_PUBLICACCOUNT";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mExecutorService = Executors.newFixedThreadPool(15);
        mWorkingTasks = new ConcurrentHashMap<Long, RequestedTask>();
        mPendingPARequests = new HashMap<Long, String>();
        mPendingRequests = new HashMap<Long, BlockingQueue<Long>>();

        mEventListeners = new LongSparseArray<IDeviceApiServiceCallback>();
        mTempListeners = new LongSparseArray<IDeviceApiServiceCallback>();

        mCallback = new SimpleServiceCallback() {
            @Override
            public void onServiceConnected() throws RemoteException {
                Log.d(TAG, "onServiceConnected");
                for (int i = 0; i < mEventListeners.size(); i++) {
                    final IDeviceApiServiceCallback callback = mEventListeners.valueAt(i);
                    try {
                        callback.onServiceConnected();
                    } catch (RemoteException e) {
                        mEventListeners.remove(i);
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int reason) throws RemoteException {
                Log.d(TAG, "onServiceDisconnected");
                for (int i = 0; i < mEventListeners.size(); i++) {
                    final IDeviceApiServiceCallback callback = mEventListeners.valueAt(i);
                    try {
                        callback.onServiceDisconnected(reason);
                    } catch (RemoteException e) {
                        mEventListeners.remove(i);
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceRegistered() throws RemoteException {
                Log.d(TAG, "onServiceRegistered");
                for (int i = 0; i < mEventListeners.size(); i++) {
                    final IDeviceApiServiceCallback callback = mEventListeners.valueAt(i);
                    try {
                        callback.onServiceRegistered();
                    } catch (RemoteException e) {
                        mEventListeners.remove(i);
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceUnregistered() throws RemoteException {
                Log.d(TAG, "onServiceUnregistered");
                for (int i = 0; i < mEventListeners.size(); i++) {
                    final IDeviceApiServiceCallback callback = mEventListeners.valueAt(i);
                    try {
                        callback.onServiceUnregistered();
                    } catch (RemoteException e) {
                        mEventListeners.remove(i);
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void reportSubscribeResult(long requestId, int resultCode)
                    throws RemoteException {
                Log.d(TAG, "reportSubscribeResult, resultCode = " + resultCode);
                String account = mPendingPARequests.get(requestId);
                mPendingPARequests.remove(requestId);
                if (account != null) {
                    mTempListeners.get(requestId).onFollowPublicAccount(
                            account,
                            resultCode == ResultCode.SUCCESS ? PublicService.OK
                                    : PublicService.INTERNEL_ERROR, null);
                    mTempListeners.remove(requestId);
                }
            }

            @Override
            public void reportUnsubscribeResult(long requestId, int resultCode)
                    throws RemoteException {
                Log.d(TAG, "reportUnsubscribeResult, resultCode = " + resultCode);
                String account = mPendingPARequests.get(requestId);
                mPendingPARequests.remove(requestId);
                if (account != null) {
                    mTempListeners.get(requestId).onUnfollowPublicAccount(
                            account,
                            resultCode == ResultCode.SUCCESS ? PublicService.OK
                                    : PublicService.INTERNEL_ERROR, null);
                    mTempListeners.remove(requestId);
                }
            }

            @Override
            public void reportGetDetailsResult(long requestId, int resultCode,
                    long accountId) throws RemoteException {
                Log.d(TAG, "reportGetDetailsResult, resultCode = " + resultCode);
                String account = mPendingPARequests.get(requestId);
                mPendingPARequests.remove(requestId);
                if (account != null) {
                    // request from get public account info
                    mTempListeners.get(requestId).onGetInfo(
                            account,
                            resultCode == ResultCode.SUCCESS ? PublicService.OK
                                    : PublicService.INTERNEL_ERROR, null);
                    mTempListeners.remove(requestId);
                } else {
                    // request from send message
                    BlockingQueue<Long> queue = mPendingRequests.get(requestId);
                    if (resultCode == ResultCode.SUCCESS) {
                        try {
                            queue.put((long) accountId);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            queue.put((long) Constants.INVALID);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void reportGetMenuResult(long requestId, int resultCode)
                    throws RemoteException {
                Log.d(TAG, "reportGetMenuResult, resultCode = " + resultCode);
                String account = mPendingPARequests.get(requestId);
                mPendingPARequests.remove(requestId);
                if (account != null) {
                    Cursor c = null;
                    String menu = null;
                    try {
                        c = getContentResolver().query(AccountColumns.CONTENT_URI,
                                new String[] { AccountColumns.MENU },
                                AccountColumns.UUID + "=?", new String[] { account },
                                null);
                        if (c != null && c.getCount() > 0) {
                            c.moveToFirst();
                            menu = c.getString(c
                                    .getColumnIndexOrThrow(AccountColumns.MENU));
                        } else {
                            resultCode = ResultCode.SYSEM_ERROR_UNKNOWN;
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                    mTempListeners.get(requestId).onMenuConfigUpdated(
                            account,
                            menu,
                            resultCode == ResultCode.SUCCESS ? PublicService.OK
                                    : PublicService.INTERNEL_ERROR, null);
                    mTempListeners.remove(requestId);
                }
            }
        };

        mClient = new PAMClient(PlatformManager.getInstance().getTransmitter(this), this);
        Intent intent = new Intent();
        intent.setClass(this, PAServiceImpl.class);
        boolean ret = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onCreate end, bindService ret = " + ret);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        try {
            mPASBinder.unregisterCallback(mToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    class DeviceApiBinder extends IDeviceApiService.Stub {

        @Override
        public long addDeviceApiCallback(IDeviceApiServiceCallback listener)
                throws RemoteException {
            Log.d(TAG, "addDeviceApiCallback");
            long token = generateToken();
            mEventListeners.put(token, listener);
            if (mPASBinder != null && mPASBinder.isServiceConnected()) {
                listener.onServiceConnected();
            }
            if (mPASBinder != null && mPASBinder.isServiceRegistered()) {
                listener.onServiceRegistered();
            }
            return token;
        }

        @Override
        public void removeDeviceApiCallback(long token) throws RemoteException {
            Log.d(TAG, "removeDeviceApiCallback token = " + token);
            mEventListeners.remove(token);
        }

        @Override
        public boolean isServiceConnected(long token) throws RemoteException {
            Log.d(TAG, "isServiceConnected token = " + token);
            return (mPASBinder != null && mPASBinder.isServiceConnected());
        }

        @Override
        public String sendMessage(long token, String accountnumber, String message)
                throws RemoteException {
            Log.d(TAG, "sendMessage token = " + token);
            return sendMessageInternal(token, accountnumber, message, false);
        }

        @Override
        public boolean deleteMessage(long token, String msgId) throws RemoteException {
            Log.d(TAG, "deleteMessage token = " + token + ", id = " + msgId);
            boolean ret = false;
            Cursor c = null;
            try {
                c = getContentResolver().query(MessageColumns.CONTENT_URI,
                        new String[] { MessageColumns.ID },
                        MessageColumns.SOURCE_ID + "=?", new String[] { msgId }, null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    long id = c.getLong(c.getColumnIndexOrThrow(MessageColumns.ID));
                    ret = mPASBinder.deleteMessage(mToken, id);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            return ret;
        }

        @Override
        public String sendMenuMessage(long token, String accountnumber, String menuID)
                throws RemoteException {
            Log.d(TAG, "sendMenuMessage token = " + token + ", id = " + menuID);
            return sendMessageInternal(token, accountnumber, menuID, true);
        }

        @Override
        public boolean setMessageRead(long token, String msgId) throws RemoteException {
            Log.d(TAG, "setMessageRead token = " + token + ", id = " + msgId);
            ContentValues cv = new ContentValues();
            cv.put(MessageColumns.STATUS, Constants.MESSAGE_STATUS_READ);
            int updateCount = getContentResolver().update(MessageColumns.CONTENT_URI, cv,
                    MessageColumns.SOURCE_ID + "=?", new String[] { msgId });
            // TODO Should we send read report to the server? Does server care?
            return (updateCount == 1);
        }

        @Override
        public void getPublicAccountInfo(long token, String account)
                throws RemoteException {
            Log.d(TAG, "getPublicAccountInfo token = " + token + ", account = " + account);
            long requestId = mPASBinder.getDetails(mToken, account, null);
            mPendingPARequests.put(requestId, account);
            mTempListeners.put(requestId, mEventListeners.get(token));
        }

        @Override
        public boolean getPublicAccountHistory(long token, String accountnumber,
                String timestamp, int order, int pageno, int pagesize, long id)
                throws RemoteException {
            Log.d(TAG, "getPublicAccountHistory token = " + token);
            List<MessageContent> messages = null;
            try {
                messages = mClient.getMessageHistory(accountnumber, timestamp, order,
                        pagesize, pageno);

                getContentResolver().delete(PAContract.CcsHistoryColumns.CONTENT_URI,
                        PAContract.CcsHistoryColumns.ID + "=?",
                        new String[] { Long.toString(1) });

                for (MessageContent m : messages) {
                    ContentValues cv = new ContentValues();
                    cv.put(PAContract.CcsHistoryColumns.MESSAGE_ID, m.messageUuid);
                    cv.put(PAContract.CcsHistoryColumns.ACCOUNT, m.publicAccountUuid);
                    cv.put(PAContract.CcsHistoryColumns.BODY, m.body);
                    cv.put(PAContract.CcsHistoryColumns.TIMESTAMP, m.createTime);
                    cv.put(PAContract.CcsHistoryColumns.MIME_TYPE, ".xml");
                    cv.put(PAContract.CcsHistoryColumns.MESSAGE_STATUS, -1);
                    cv.put(PAContract.CcsHistoryColumns.DIRECTION,
                            Constants.MESSAGE_DIRECTION_INCOMING);
                    cv.put(PAContract.CcsHistoryColumns.TYPE,
                            com.cmcc.ccs.chat.ChatService.XML);
                    cv.put(PAContract.CcsHistoryColumns.ID, id);

                    Uri uri = getContentResolver().insert(
                            PAContract.CcsHistoryColumns.CONTENT_URI, cv);
                    return uri != null;
                }
            } catch (PAMException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean getFollowedPublicAccount(long token, int pageno, int order,
                int pagesize) throws RemoteException {
            Log.d(TAG, "getFollowedPublicAccount token = " + token);
            List<PublicAccount> accounts = null;
            try {
                accounts = mClient.getSubscribedList(order, pagesize, pageno);
                for (PublicAccount account : accounts) {
                    account.insertOrUpdateAccount(getContentResolver(), false);
                }
            } catch (PAMException e) {
                e.printStackTrace();
            }

            return accounts != null && accounts.size() != 0;
        }

        @Override
        public void followPublicAccount(long token, String accountnumber)
                throws RemoteException {
            Log.d(TAG, "followPublicAccount token = " + token);
            long requestId = mPASBinder.subscribe(mToken, accountnumber);
            mPendingPARequests.put(requestId, accountnumber);
            mTempListeners.put(requestId, mEventListeners.get(token));
        }

        @Override
        public void searchPublicAccount(final long token, final String keyword,
                final int pageNum, final int order, final int pageSize)
                throws RemoteException {
            Log.d(TAG, "searchPublicAccount token = " + token);
            final long requestId = generateUuid();
            final class SearchTask extends RequestedTask {
                public SearchTask(long requestId) {
                    super(requestId);
                }

                @Override
                public void run() {
                    try {
                        List<com.mediatek.rcs.pam.model.PublicAccount> accounts = mClient
                                .search(keyword, order, pageSize, pageNum);
                        ContentResolver cr = getContentResolver();
                        cr.delete(CcsSearchColumns.CONTENT_URI, null, null);
                        boolean errorHappened = false;
                        for (com.mediatek.rcs.pam.model.PublicAccount account : accounts) {
                            long accountId = storeSearchedAccount(account);
                            if (accountId == Constants.INVALID) {
                                Log.e(TAG, "Insert searched account failed: "
                                        + account.uuid);
                                errorHappened = true;
                            }
                        }
                        if (errorHappened) {
                            mEventListeners.get(token).onSearch(
                                    PublicService.INTERNEL_ERROR,
                                    Integer.toString(ResultCode.PARAM_ERROR_UNKNOWN));
                        } else {
                            mEventListeners.get(token).onSearch(PublicService.OK, null);
                        }
                    } catch (PAMException e) {
                        e.printStackTrace();
                        try {
                            mEventListeners.get(token).onSearch(
                                    PublicService.INTERNEL_ERROR,
                                    Integer.toString(ResultCode.PARAM_ERROR_UNKNOWN));
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                            mEventListeners.remove(token);
                        }

                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                        mEventListeners.remove(token);
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            SearchTask task = new SearchTask(requestId);
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
        }

        @Override
        public void unfollowPublicAccount(long token, String accountnumber)
                throws RemoteException {
            Log.d(TAG, "unfollowPublicAccount token = " + token);
            long requestId = mPASBinder.unsubscribe(mToken, accountnumber);
            mPendingPARequests.put(requestId, accountnumber);
            mTempListeners.put(requestId, mEventListeners.get(token));
        }

        @Override
        public boolean getPublicAccountStatus(long token, String accountnumber)
                throws RemoteException {
            Log.d(TAG, "getPublicAccountStatus token = " + token);
            int subscriptionStatus = getSubscriptionStatus(accountnumber);
            if (subscriptionStatus != Constants.INVALID) {
                return (subscriptionStatus == Constants.SUBSCRIPTION_STATUS_YES);
            } else {
                long requestId = mPASBinder.getDetails(mToken, accountnumber, null);
                BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
                mPendingRequests.put(requestId, queue);
                mTempListeners.put(requestId, mEventListeners.get(token));
                long accountId = Constants.INVALID;
                try {
                    accountId = queue.poll(TIMEOUT, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    // time out
                    Log.d(TAG, "Get details time out when sending message");
                    return false;
                } finally {
                    mPendingRequests.remove(queue);
                }
                if (accountId != Constants.INVALID) {
                    return (Constants.SUBSCRIPTION_STATUS_YES
                            == getSubscriptionStatus(accountnumber));
                } else {
                    Log.d(TAG, "No public account: " + accountnumber);
                    return false;
                }
            }
        }

        @Override
        public void reportPublicAccount(final long token, final String account,
                final String reason, final String description, final int type,
                final String data) throws RemoteException {
            Log.d(TAG, "reportPublicAccount token = " + token);
            final long requestId = generateUuid();
            final class ReportTask extends RequestedTask {
                public ReportTask(long requestId) {
                    super(requestId);
                }

                @Override
                public void run() {
                    try {
                        int result = mClient.complain(account, type, reason, data,
                                description);
                        try {
                            mEventListeners.get(token).onReportPublicAccount(
                                    account,
                                    // TODO check this error code
                                    result == ResultCode.SUCCESS ? PublicService.OK
                                            : PublicService.INTERNEL_ERROR,
                                    Integer.toString(result));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            mEventListeners.remove(token);
                        }

                    } catch (PAMException e) {
                        e.printStackTrace();
                        try {
                            mEventListeners.get(token).onReportPublicAccount(account,
                            // TODO check this error code
                                    PublicService.INTERNEL_ERROR, null);
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                            mEventListeners.remove(token);
                        }
                    }
                    mWorkingTasks.remove(mId);
                }
            }
            ReportTask task = new ReportTask(requestId);
            mWorkingTasks.put(requestId, task);
            mExecutorService.execute(task);
        }

        @Override
        public void updateMenuConfig(long token, String accountnumber)
                throws RemoteException {
            Log.d(TAG, "updateMenuConfig token = " + token);
            long requestId = mPASBinder.getMenu(mToken, accountnumber, null);
            mPendingPARequests.put(requestId, accountnumber);
            mTempListeners.put(requestId, mEventListeners.get(token));
        }
    }

    private long storeSearchedAccount(com.mediatek.rcs.pam.model.PublicAccount account) {
        Log.d(TAG, "storeSearchedAccount");
        ContentValues cv = new ContentValues();
        cv.put(CcsSearchColumns.ACCOUNT, account.uuid);
        cv.put(CcsSearchColumns.NAME, account.name);
        cv.put(CcsSearchColumns.PORTRAIT, account.logoUrl);
        String portraitType = null;
        String url = account.logoUrl.toLowerCase(Locale.ENGLISH);
        if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            portraitType = "JPG";
        } else if (url.endsWith(".png")) {
            portraitType = "PNG";
        } else if (url.endsWith(".bmp")) {
            portraitType = "BMP";
        } else if (url.endsWith(".gif")) {
            portraitType = "GIF";
        }
        cv.put(CcsSearchColumns.PORTRAIT_TYPE, portraitType);
        cv.put(CcsSearchColumns.BREIF_INTRODUCTION, account.introduction);
        Uri uri = getContentResolver().insert(CcsSearchColumns.CONTENT_URI, cv);
        Log.d(TAG, "Insert searched account: " + uri);
        long accountId = Constants.INVALID;
        try {
            accountId = Long.parseLong(uri.getLastPathSegment());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return accountId;
    }

    private int getSubscriptionStatus(String account) {
        Cursor c = null;
        try {
            c = getContentResolver().query(AccountColumns.CONTENT_URI,
                    new String[] { AccountColumns.SUBSCRIPTION_STATUS },
                    AccountColumns.UUID + "=?", new String[] { account }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getInt(c
                        .getColumnIndexOrThrow(AccountColumns.SUBSCRIPTION_STATUS));
            } else {
                return Constants.INVALID;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String sendMessageInternal(long token, String accountnumber, String message,
            boolean system) throws RemoteException {
        Cursor c = null;
        try {
            c = getContentResolver().query(AccountColumns.CONTENT_URI,
                    new String[] { AccountColumns.ID }, AccountColumns.UUID + "=?",
                    new String[] { accountnumber }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                final long accountId = c.getLong(c
                        .getColumnIndexOrThrow(AccountColumns.ID));
                return sendMessageToAccountId(accountId, message, system);
            } else {
                long requestId = mPASBinder.getDetails(mToken, accountnumber, null);
                BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
                mPendingRequests.put(requestId, queue);
                mTempListeners.put(requestId, mEventListeners.get(token));
                long accountId = Constants.INVALID;
                try {
                    accountId = queue.poll(TIMEOUT, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    // time out
                    Log.d(TAG, "Get details time out when sending message");
                    return null;
                } finally {
                    mPendingRequests.remove(queue);
                }
                if (accountId != Constants.INVALID) {
                    String result = sendMessageToAccountId(accountId, message, system);
                    return result;
                } else {
                    Log.d(TAG, "No public account: " + accountnumber);
                    return null;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String sendMessageToAccountId(long accountId, String message, boolean system)
            throws RemoteException {
        final long messageId = mPASBinder.sendMessage(mToken, accountId, message, system);
        Log.d(TAG, "sendMessageToAccountId, messageId = " + messageId);
        if (messageId == Constants.INVALID) {
            return null;
        } else {
            Cursor c = null;
            try {
                Uri uri = MessageColumns.CONTENT_URI;
                if (system) {
                    uri = MessageColumns.CONTENT_URI
                            .buildUpon()
                            .appendQueryParameter(
                                    PAContract.MESSAGES_PARAM_INCLUDING_SYSTEM,
                                    Integer.toString(Constants.IS_SYSTEM_YES)).build();
                }
                c = getContentResolver().query(uri,
                        new String[] { MessageColumns.SOURCE_ID },
                        MessageColumns.ID + "=?",
                        new String[] { Long.toString(messageId) }, null);

                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    final String result = c.getString(c
                            .getColumnIndexOrThrow(MessageColumns.SOURCE_ID));
                    return result;
                } else {
                    return null;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    private synchronized long generateUuid() {
        mUuidCounter += 1;
        return mUuidCounter;
    }

    private synchronized long generateToken() {
        mTokenCounter += 1;
        return mTokenCounter;
    }
}
