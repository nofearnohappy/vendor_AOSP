package com.mediatek.rcs.pam.ui;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.IPAServiceCallback.Stub;
import com.mediatek.rcs.pam.model.ResultCode;

import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;

public class FileDownloader {
    private static String TAG = Constants.TAG_PREFIX + "FileDownloader";
    private static FileDownloader mInstance = null;
    private static long sUuidCounter = 0;

    private PAService mPAService;
    private long mToken = Constants.INVALID;

    private LongSparseArray<TaskInfo> mDownloadMap = new LongSparseArray<TaskInfo>();

    private FileDownloader() {
        initPAService();
    }

    public static FileDownloader getInstance() {
        if (mInstance == null) {
            synchronized (FileDownloader.class) {
                if (mInstance == null) {
                    mInstance = new FileDownloader();
                }
            }
        }
        return mInstance;
    }

    private static synchronized long generateUuid() {
        sUuidCounter += 1;
        return sUuidCounter;
    }

    public long sendDownloadRequest(String url, int type, long msgId,
            int index, DownloadListener listener) {
        long requestId = generateUuid();
        mDownloadMap.put(requestId, new TaskInfo(url, type, msgId, index,
                listener));
        Log.d(TAG, "sendDownloadRequest(). url=" + url + ". type=" + type
                + ". msgId=" + msgId + ". requestId=" + requestId);
        long cancelId = mPAService.downloadObject(mToken, requestId, url, type);

        return cancelId;
    }

    public void canelDownload(long cancelId) {
        mPAService.cancelDownload(mToken, cancelId);
    }

    private void initPAService() {
        Stub callback = new SimpleServiceCallback() {

            @Override
            public void reportDownloadResult(final long requestId,
                    final int resultCode, final String path, final long mediaId)
                    throws RemoteException {
                Log.i(FileDownloader.TAG, "reportDownloadResult msg id = "
                        + requestId + ", result" + resultCode
                        + ", file path = " + path);
                final TaskInfo downloadInfo = mDownloadMap.get(requestId);

                downloadInfo.mListener.reportDownloadResult(resultCode, path,
                        mediaId, downloadInfo.mMsgId, downloadInfo.mIndex);
                mDownloadMap.remove(requestId);
            }

            @Override
            public void updateDownloadProgress(final long requestId,
                    final int percentage) throws RemoteException {
                super.updateDownloadProgress(requestId, percentage);
                final TaskInfo downloadInfo = mDownloadMap.get(requestId);
                if (downloadInfo != null) {
                    downloadInfo.mListener.reportDownloadProgress(
                            downloadInfo.mMsgId, downloadInfo.mIndex,
                            percentage);
                }
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.i(FileDownloader.TAG, "onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(final int reason)
                    throws RemoteException {
                Log.i(FileDownloader.TAG, "onServiceDisconnected, reason="
                        + reason);
                if (reason == PAService.INTERNAL_ERROR) {
                    mInstance = null;
                    mPAService = null;
                    for (int i = 0; i < mDownloadMap.size(); i++) {
                        final TaskInfo downloadInfo = mDownloadMap.valueAt(i);
                        downloadInfo.mListener.reportDownloadResult(
                                ResultCode.SYSEM_ERROR_UNKNOWN, null,
                                Constants.INVALID, downloadInfo.mMsgId,
                                downloadInfo.mIndex);

                    }
                }
            }
        };

        mPAService = PAService.getInstance();
        mToken = mPAService.registerCallback(callback, true);
        mPAService.registerAck(mToken);
    }

    private class TaskInfo {
        public final String mUrl;
        public final int mType;
        public final long mMsgId;
        private DownloadListener mListener;
        public Integer mIndex;

        public TaskInfo(String url, int type, long msgId, int index,
                DownloadListener listener) {
            mUrl = url;
            mType = type;
            mMsgId = msgId;
            mListener = listener;
            mIndex = index;
        }
    }

    public interface DownloadListener {
        void reportDownloadResult(int resultCode, String path, long mediaId,
                long msgId, int index);

        void reportDownloadProgress(long msgId, int index, int percentage);
    }
}
