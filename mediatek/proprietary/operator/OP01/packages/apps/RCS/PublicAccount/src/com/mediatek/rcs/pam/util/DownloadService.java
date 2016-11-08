package com.mediatek.rcs.pam.util;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.MediaFolder;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * this class do the real download work
 * add download requests will process in one same thread.
 */
public class DownloadService {
    private static final String TAG = Constants.TAG_PREFIX + "DownloadService";
    private static DownloadService sInstance = null;
    private boolean mIsRunning;

    private final long mTimeout = 45 * 1000;
    private Context mContext = null;
    private Selector mSelector = null;
    private List<DownloadInfo> mWaitingTask = null;
    private long mLastCheckTime = 0;
    private Runnable mSelectThread = new Runnable() {
        public void run() {
            while (true) {
                try {
                    mSelector.select(mTimeout + 1000);
                    if (!mIsRunning) {
                        stopSelector();
                        return;
                    }

                    addNewTask();

                    Iterator<SelectionKey> ite = mSelector.selectedKeys()
                            .iterator();
                    while (ite.hasNext()) {
                        SelectionKey key = ite.next();
                        ite.remove();

                        DownloadInfo info = (DownloadInfo) key.attachment();
                        try {
                            if (info.isCancelled()) {
                                closeAndDelete(info, key, info.mCancelReason);
                            } else if (key.isConnectable()) {
                                connect(key);
                            } else if (key.isReadable()) {
                                read(key);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            closeAndDelete(info, key,
                                    ResultCode.SYSTEM_ERROR_NETWORK);
                        }

                    }
                    checkAndRemoveBlockingChannel();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void stopSelector() throws IOException {
        Set<SelectionKey> keys = mSelector.keys();
        for (SelectionKey key : keys) {
            DownloadInfo info = (DownloadInfo) key.attachment();
            closeAndDelete(info, key, ResultCode.SYSEM_ERROR_UNKNOWN);
        }
        for (DownloadInfo info : mWaitingTask) {
            info.mCallback.reportDownloadResult(ResultCode.SYSEM_ERROR_UNKNOWN,
                    null);
        }
        mSelector.close();
    }

    private void addNewTask() throws IOException {
        List<DownloadInfo> tempQueue = new ArrayList<DownloadInfo>();
        if (mWaitingTask.size() != 0) {
            synchronized (mWaitingTask) {
                tempQueue.addAll(mWaitingTask);
                mWaitingTask.clear();
            }
        }

        // we do this here because we must handle mSelector.keys() in one thread
        // to avoid ConcurrentModicationException
        for (DownloadInfo info : tempQueue) {
            Log.d(TAG, info.mUrl.toString() + "runnable");
            if (!hasDonwloaded(info)) {
                InetSocketAddress socketAddress = new InetSocketAddress(
                        info.mHost, info.mPort);
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(socketAddress);

                socketChannel
                        .register(mSelector, SelectionKey.OP_CONNECT, info);
            }
        }
    }

    private boolean checkAndRemoveBlockingChannel() {
        boolean isRemoved = false;
        long currentTime = System.currentTimeMillis();
        if (mLastCheckTime == 0) {
            mLastCheckTime = currentTime;
        }
        if ((currentTime - mLastCheckTime > mTimeout)) {
            Iterator<SelectionKey> ite = mSelector.keys().iterator();
            Log.i(TAG, "clear time out counts = " + mSelector.keys().size());
            while (ite.hasNext()) {
                SelectionKey key = ite.next();
                DownloadInfo info = (DownloadInfo) key.attachment();
                long time = currentTime - info.mLastCallTime;
                if ((info.mLastCallTime != 0) && (time > mTimeout)) {
                    closeAndDelete(info, key, ResultCode.SYSTEM_ERROR_TIMEOUT);
                    isRemoved = true;
                } else if (info.mLastCallTime == 0) {
                    info.mLastCallTime = currentTime;
                }
            }

            mLastCheckTime = currentTime;
        }
        return isRemoved;
    }

    private DownloadService(Context context) {
        mWaitingTask = new ArrayList<DownloadInfo>();
        mContext = context;
        mIsRunning = true;
        try {
            mSelector = Selector.open();
            Thread thread = new Thread(mSelectThread);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        DownloadInfo info = (DownloadInfo) key.attachment();
        Log.i(TAG, "download connected! file = " + info.mContent);

        if (channel.isConnectionPending()) {
            if (channel.finishConnect()) {
                // channel.configureBlocking(false);
                info.mLastCallTime = System.currentTimeMillis();
                String request = "GET " + info.mContent + " HTTP/1.1\r\nHost: "
                        + info.mHost + ":" + info.mPort
                        + "\r\nConnection: close\r\n\r\n";
                Charset charset = Charset.forName("UTF-8");
                ByteBuffer buffer = charset.encode(request);
                int limit = buffer.limit();
                int writeBytes = 0;
                while (writeBytes != limit) {
                    writeBytes += channel.write(buffer);
                }
                channel.register(mSelector, SelectionKey.OP_READ, info);
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        DownloadInfo info = (DownloadInfo) key.attachment();
        if (info.mHeaders == null) {
            RawHeaders headers = new RawHeaders();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int headerBytes = readHeaders(channel, headers, buffer);
            int responseCode = headers.getResponseCode();
            Log.i(TAG, "download read! file = " + info.mContent
                    + ", response = " + responseCode);
            if (responseCode == 100) {
                return;
            } else if (responseCode == 302 || responseCode == 301) {
                String location = headers.get("Location");
                Log.i(TAG,"response = " + responseCode +
                        ", location = " + location);
                info.setUrl(location);
                String request = "GET " + info.mContent + " HTTP/1.1\r\nHost: "
                        + info.mHost + ":" + info.mPort
                        + "\r\nConnection: close\r\n\r\n";
                Charset charset = Charset.forName("UTF-8");
                buffer = charset.encode(request);
                int limit = buffer.limit();
                int writeBytes = 0;
                while (writeBytes != limit) {
                    writeBytes += channel.write(buffer);
                }
                return;
            } else if(responseCode != 200) {
                closeAndDelete(info, key, responseCode);
                return;
            }

            info.mHeaders = headers;
            info.mFileSize = Integer.parseInt(headers.get("Content-Length"));
            String ext = MediaFolder.getExtensionFromMimeType((headers
                    .get("Content-Type")));
            info.mFilePath = MediaFolder.generateMediaFileName(
                    Constants.INVALID, info.mType, ext);
            File file = new File(info.mFilePath);
            info.mFileStream = new FileOutputStream(file);
            info.mFileChannel = info.mFileStream.getChannel();
            buffer.flip();
            buffer.position(headerBytes);
            info.mOffset = info.mFileChannel.write(buffer);
        }

        long size = 0;
        final int maxRead = (1 * 1024 * 1024) > info.mFileSize ? info.mFileSize
                : (1 * 1024 * 1024);
        ByteBuffer buffer = ByteBuffer.allocate(maxRead);
        do {
            size = channel.read(buffer);
            if (size <= 0) {
                break;
            }
            buffer.flip();
            info.mFileChannel.write(buffer, info.mOffset);
            info.mOffset += size;
            buffer.clear();
            info.mLastCallTime = System.currentTimeMillis();
        } while (size == maxRead);
        if (info.mOffset != info.mFileSize) {
            int percentage = info.mOffset * 100 / info.mFileSize;
            info.mCallback.updateDownloadProgress(percentage);
        } else {
            info.mFileChannel.close();
            info.mFileStream.close();
            channel.close();
            key.cancel();
            info.mCallback.reportDownloadResult(ResultCode.SUCCESS,
                    info.mFilePath);
        }
    }

    private void closeAndDelete(DownloadInfo info, SelectionKey key, int reason) {
        Log.i(TAG, "download failed, file = " + info.mFilePath + ", reason = "
                + reason);
        try {
            if (info.mFileChannel != null && info.mFileStream != null) {
                info.mFileChannel.close();
                info.mFileStream.close();
                File file = new File(info.mFilePath);
                if (file.exists()) {
                    file.delete();
                }
            }

            SocketChannel channel = (SocketChannel) key.channel();
            channel.close();
            key.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            info.mCallback.reportDownloadResult(reason, null);
        }
    }

    private boolean hasDonwloaded(DownloadInfo info) {
        // check for exist file
        Cursor c = mContext.getContentResolver().query(
                MediaColumns.CONTENT_URI,
                new String[] { MediaColumns.ID, MediaColumns.PATH },
                MediaColumns.URL + "=?", new String[] { info.mUrl.toString() },
                null);
        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                long mediaId = c.getLong(c
                        .getColumnIndexOrThrow(MediaColumns.ID));
                String filePath = c.getString(c
                        .getColumnIndexOrThrow(MediaColumns.PATH));
                if (!TextUtils.isEmpty(filePath)) {
                    Log.d(TAG,
                            "file has downloaded, callback directly. mediaId="
                                    + mediaId);
                    File file = new File(filePath);
                    if (file.exists()) {
                        info.mCallback.reportDownloadResult(ResultCode.SUCCESS,
                                filePath);
                        return true;
                    } else {
                        Log.d(TAG, "can't file file even it exist in DB.");
                    }
                }
            }
        } finally {
            c.close();
        }

        return false;
    }

    private int readHeaders(SocketChannel in, RawHeaders headers,
            ByteBuffer buffer) throws IOException {
        StringBuilder result = new StringBuilder(80);
        int lenght = in.read(buffer);
        int pos = 0;
        int line = 0;
        while (pos < lenght) {
            byte c = buffer.get(pos);
            result.append((char) c);
            pos++;
            if (c == -1) {
                throw new EOFException();
            } else if (c == '\n') {
                int length = result.length();
                if (length > 0 && result.charAt(length - 2) == '\r') {
                    if (length == 2) {
                        return pos;
                    }
                    result.setLength(length - 2);
                }
                if (line == 0) {
                    headers.setStatusLine(result.toString());
                } else {
                    headers.addLine(result.toString());
                }
                result.delete(0, result.length());
                line++;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * get this singletion's instance.
     *
     * @param context a Context, to get ContentResolve to access the database
     *
     * @return instance of this singletion class
     */
    public static DownloadService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DownloadService.class) {
                if (sInstance == null) {
                    sInstance = new DownloadService(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * stop download thread, this will notice all callbacks and release resource.
     */
    public void stop() {
        Log.i(TAG, "stop");
        mIsRunning = false;
        mSelector.wakeup();
        sInstance = null;
    }

    /**
     * download files from url in the.
     *
     * @param info include download info
     */
    public void startDownload(DownloadInfo info) {
        synchronized (mWaitingTask) {
            mWaitingTask.add(info);
            Log.d(TAG, info.mUrl.toString());
        }
        mSelector.wakeup();

    }

    /**
     * callback to notice download result and progress.
     */
    public static interface DownloadCallback {
        /**
         * notice the download progress.
         *
         * @param percentage between 0-100
         */
        void updateDownloadProgress(int percentage);

        /**
         * notice the download result.
         *
         * @param resultCode resutl of this download, defined in {@link ResultCode}
         * @param filePath path of the download file if success, null if failed
         */
        void reportDownloadResult(int resultCode, String filePath);
    }

    /**
     * class include download info and can cancel the download file.
     */
    public static class DownloadInfo {
        /**
         * @param surl a String, url of download file
         * @param type a int, type of the file, define in {@link Constants}
         * @param callback a {@link DownloadCallback} to notice the download status
         *
         * @throws MalformedURLException if the something is wrong with the
         * <strong>surl</strong>
         *
         * @see Constants
         */
        public DownloadInfo(String surl, int type, DownloadCallback callback)
                throws MalformedURLException {
            this.mCallback = callback;
            this.mType = type;

            setUrl(surl);
        }

        public void setUrl(String surl)throws MalformedURLException {
            this.mUrl = new URL(surl);
            mHost = mUrl.getHost();
            mContent = mUrl.getPath();
            mPort = mUrl.getPort() > 0 ? mUrl.getPort() : mUrl.getDefaultPort();
        }

        /**
         * cancel this download and notice the callback.
         */
        public void cancel() {
            cancel(ResultCode.USER_CANCELLED);
        }

        /**
         * cancel this download and notice the callback.
         *
         * @param reason result to notice the callback, see {@link ResultCode} for more info
         *
         * @see ResultCode
         */
        public void cancel(int reason) {
            if (reason == Constants.INVALID) {
                throw new Error("Invalid cancel reason");
            }
            mCancelReason = reason;
        }

        /**
         * check whether this download is been cancelled.
         *
         * @return whether this download is been cancelled
         */
        public boolean isCancelled() {
            return mCancelReason != Constants.INVALID;
        }

        RawHeaders mHeaders;
        FileChannel mFileChannel = null;
        FileOutputStream mFileStream = null;
        URL mUrl = null;
        String mFilePath = null;
        DownloadCallback mCallback = null;
        int mType;

        int mFileSize = -1;
        int mOffset = 0;

        String mHost;
        String mContent;
        int mPort;
        int mCancelReason = Constants.INVALID;
        long mLastCallTime = 0;
    }
}