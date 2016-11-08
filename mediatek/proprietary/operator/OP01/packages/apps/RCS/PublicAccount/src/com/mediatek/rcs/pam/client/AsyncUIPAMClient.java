package com.mediatek.rcs.pam.client;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.PlatformManager;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is a facade of PAClient for UI components such as activities. By using this
 * object, activities can retrieve non-persistent data from server without bothering
 * PAService and/or content provider.
 *
 * The APIs here are sync method invocations. So watch out for ANRs.
 */
public class AsyncUIPAMClient {
    private static final String TAG = "PAM/AsyncUIPAMClient";

    private PAMClient mClient;
    private Callback mCallback = null;
    private long mUuidCounter = 0;
    private ExecutorService mExecutorService;
    private ConcurrentHashMap<Long, RequestedTask> mWorkingTasks;

    public interface Callback {
        void reportSearchResult(long requestId, int resultCode, List<PublicAccount> results);
        void reportGetRecommendsResult(long requestId, int resultCode, List<PublicAccount> results);
        void reportGetMessageHistoryResult(
                long requestId, int resultCode, List<MessageContent> results);
        void reportComplainResult(long requestId, int resultCode);
    }

    public static class SimpleCallback implements Callback {

        @Override
        public void reportSearchResult(
                long requestId, int resultCode, List<PublicAccount> results) {}

        @Override
        public void reportGetRecommendsResult(
                long requestId, int resultCode, List<PublicAccount> results) {}

        @Override
        public void reportGetMessageHistoryResult(
                long requestId, int resultCode, List<MessageContent> results) {}

        @Override
        public void reportComplainResult(long requestId, int resultCode) {}

    }

    private static class CallbackWrapper implements Callback {
        private Callback mCallback;
        private final Handler mHandler;

        public CallbackWrapper(Callback callback, Context context) {
            mCallback = callback;
            mHandler = new Handler(context.getMainLooper());
        }

        @Override
        public void reportSearchResult(
                final long requestId, final int resultCode, final List<PublicAccount> results) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.reportSearchResult(requestId, resultCode, results);
                }
            });
        }

        @Override
        public void reportGetRecommendsResult(
                final long requestId,
                final int resultCode,
                final List<PublicAccount> results) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.reportGetRecommendsResult(requestId, resultCode, results);
                }
            });
        }

        @Override
        public void reportGetMessageHistoryResult(
                final long requestId,
                final int resultCode,
                final List<MessageContent> results) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.reportGetMessageHistoryResult(requestId, resultCode, results);
                }
            });
        }

        @Override
        public void reportComplainResult(final long requestId, final int resultCode) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.reportComplainResult(requestId, resultCode);
                }
            });
        }
    }

    private abstract static class RequestedTask implements Runnable {
        protected final long mId;
        protected final Callback mCallback;
        protected boolean mCancelling = false;

        public RequestedTask(long requestId, Callback callback) {
            mId = requestId;
            mCallback = callback;
        }
    }

    public AsyncUIPAMClient(Context context, Callback callback) {
        PlatformManager pm = PlatformManager.getInstance();
        mClient = new PAMClient(
                pm.getTransmitter(context),
                context);
        mCallback = new CallbackWrapper(callback, context);
        mExecutorService = Executors.newFixedThreadPool(15);
        mWorkingTasks = new ConcurrentHashMap<Long, RequestedTask>();
    }

    public long search(String keyword, int order, int pageSize, int pageNumber) {
        long requestId = generateUuid();
        final class SearchTask extends RequestedTask {
            private final String mKeyword;
            private final int mOrder;
            private final int mPageSize;
            private final int mPageNumber;

            public SearchTask(long requestId, String keyword,
                    int order, int pageSize, int pageNumber, Callback callback) {
                super(requestId, callback);
                mKeyword = keyword;
                mOrder = order;
                mPageSize = pageSize;
                mPageNumber = pageNumber;
            }

            @Override
            public void run() {
                Log.d(TAG, "search(" + mId + ", " + mKeyword +
                        ", " + mOrder + ", " + mPageSize + ", " + mPageNumber + ")");
                int result = ResultCode.SUCCESS;
                List<PublicAccount> accounts = null;
                try {
                    accounts = mClient.search(mKeyword, mOrder, mPageSize, mPageNumber);
                } catch (PAMException e) {
                    result = e.resultCode;
                }
                if (mCallback != null) {
                    mCallback.reportSearchResult(mId, result, accounts);
                }
                mWorkingTasks.remove(mId);
            }
        }
        SearchTask task = new SearchTask(requestId,
                keyword, order, pageSize, pageNumber, mCallback);
        mWorkingTasks.put(requestId, task);
        mExecutorService.execute(task);
        return requestId;
    }

    public long getRecommends(int type, int pageSize, int pageNumber) {
        long requestId = generateUuid();
        final class GetRecommendsTask extends RequestedTask {
            private final int mType;
            private final int mPageSize;
            private final int mPageNumber;

            public GetRecommendsTask(long requestId,
                    int type, int pageSize, int pageNumber, Callback callback) {
                super(requestId, callback);
                mType = type;
                mPageSize = pageSize;
                mPageNumber = pageNumber;
            }

            @Override
            public void run() {
                Log.d(TAG, "getRecommends(" + mId + ", "
                        + mType + ", " + mPageSize + ", " + mPageNumber + ")");
                int result = ResultCode.SUCCESS;
                List<PublicAccount> accounts = null;
                try {
                    accounts = mClient.getRecommends(mType, mPageSize, mPageNumber);
                } catch (PAMException e) {
                    result = e.resultCode;
                }
                if (mCallback != null) {
                    mCallback.reportGetRecommendsResult(mId, result, accounts);
                }
                mWorkingTasks.remove(mId);
            }
        }
        GetRecommendsTask task = new GetRecommendsTask(
                requestId, type, pageSize, pageNumber, mCallback);
        mWorkingTasks.put(requestId, task);
        mExecutorService.execute(task);
        return requestId;
    }

    public long getMessageHistory(String uuid,
            String timestamp, int order, int pageSize, int pageNumber) {
        long requestId = generateUuid();
        final class GetMessageHistoryTask extends RequestedTask {
            private final String mUuid;
            private final String mTimestamp;
            private final int mOrder;
            private final int mPageSize;
            private final int mPageNumber;

            public GetMessageHistoryTask(
                    long requestId,
                    String uuid,
                    String timestamp,
                    int order,
                    int pageSize,
                    int pageNumber,
                    Callback callback) {
                super(requestId, callback);
                mUuid = uuid;
                mTimestamp = timestamp;
                mOrder = order;
                mPageSize = pageSize;
                mPageNumber = pageNumber;
            }

            @Override
            public void run() {
                Log.d(TAG,
                      "getMessageHistory(" + mId + ", " + mUuid + ", " +
                      mTimestamp + ", " + mOrder + ", " + mPageSize + ", " + mPageNumber + ")");
                int result = ResultCode.SUCCESS;
                List<MessageContent> messages = null;
                try {
                    messages = mClient.getMessageHistory(
                            mUuid, mTimestamp, mOrder, mPageSize, mPageNumber);
                } catch (PAMException e) {
                    result = e.resultCode;
                }
                if (mCallback != null) {
                    mCallback.reportGetMessageHistoryResult(mId, result, messages);
                }
                mWorkingTasks.remove(mId);
            }
        }
        GetMessageHistoryTask task = new GetMessageHistoryTask(
                requestId,
                uuid,
                timestamp,
                order,
                pageSize,
                pageNumber,
                mCallback);
        mWorkingTasks.put(requestId, task);
        mExecutorService.execute(task);
        return requestId;
    }

    public long complain(String uuid, int type, String reason, String data, String description) {
        long requestId = generateUuid();
        final class ComplainTask extends RequestedTask {
            private String mUuid;
            private int mType;
            private String mReason;
            private String mData;
            private String mDescription;

            public ComplainTask(
                    long requestId,
                    String uuid,
                    int type,
                    String reason,
                    String data,
                    String description,
                    Callback callback) {
                super(requestId, callback);
                mUuid = uuid;
                mType = type;
                mReason = reason;
                mData = data;
                mDescription = description;
            }

            @Override
            public void run() {
                Log.d(TAG, "complain(" + mId + ", " + mUuid +
                        ", " + mType + ", " + mReason + ", " + mDescription + ")");
                int result = ResultCode.SUCCESS;
                try {
                    result = mClient.complain(mUuid, mType, mReason, mData, mDescription);
                } catch (PAMException e) {
                    result = e.resultCode;
                }
                if (mCallback != null) {
                    mCallback.reportComplainResult(mId, result);
                }
                mWorkingTasks.remove(mId);
            }
        }
        ComplainTask task = new ComplainTask(
                requestId, uuid, type, reason, data, description, mCallback);
        mWorkingTasks.put(requestId, task);
        mExecutorService.execute(task);
        return requestId;
    }

    private synchronized long generateUuid() {
        mUuidCounter += 1;
        return mUuidCounter;
    }
}
