package com.mediatek.hotknotbeam;


import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.mediatek.hotknotbeam.HotKnotBeamConstants.FailureReason;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.HotKnotFileType;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.State;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;


public class FileUploadTask extends AsyncTask<Uri, Integer, Void> {
    private final static String TAG = HotKnotBeamService.TAG;

    private final static String LINE_END    = "\r\n";
    private final static String TWO_HYPHENS = "--";
    private final static String BOUNDARY    = "*****";

    private final static int OK = 0;
    private final static int RETRY = 1;
    private final static int FAILED = 2;
    private final static int CANCELED = 3;

    private final static int MAX_BUFFER_SIZE  = 32 * 1024;

    private static String           sUploadServer = "";
    private int                     mPort = HotKnotBeamService.SERVICE_PORT;
    private HttpURLConnection       mConnection = null;
    private Context                 mContext = null;
    private FileUploadTaskListener  mPostExecuteCB = null;
    private String                  mFileName = null;
    private int                     mTaskId;

    // LinkList to queue the download request
    private LinkedList<UploadInfo> mUploadList = new LinkedList<UploadInfo>();

    /**
      * Interface callback for HotKnotFileServer.
      *
      */
    interface FileUploadTaskListener {
        void onSetExternalPath(boolean isExternal);
        void onPostExecute(Void result, FileUploadTask task);
    }

    private FileUploadTask() {
        super();
    }

    public FileUploadTask(String server, int port, Context context) {
        mContext = context;
        mPort = port;
        sUploadServer = "http://" + server + ":" + mPort;
        mTaskId = -1;

        Properties pro = System.getProperties();
        pro.remove("http.proxyHost");
        pro.remove("http.proxyPort");
    }

    public void setOnPostExecute(FileUploadTaskListener callback) {
        mPostExecuteCB = callback;
    }

    public void setUploadFileName(String filename) {
        mFileName = filename;
    }

    public int getTaskId() {
        return mTaskId;
    }

    public static void sendClientFinishNotify() {

        Thread t = new Thread() {

            @Override
            public void run() {
                OutputStream outStream = null;
                InputStream  inStream = null;
                HttpURLConnection hConnection = null;

                try {
                    URL url = new URL(sUploadServer + "/" + HotKnotBeamConstants.BEAM_FINISH_COMMAND);
                    hConnection = (HttpURLConnection) url.openConnection();
                    hConnection.setDoInput(true);
                    hConnection.setDoOutput(true);
                    hConnection.setUseCaches(false);
                    hConnection.setRequestMethod("POST");

                    hConnection.setConnectTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
                    hConnection.setReadTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
                    outStream = hConnection.getOutputStream();
                    inStream = hConnection.getInputStream();

                    String buffer = LINE_END + LINE_END;
                    outStream.write(buffer.getBytes(), 0, buffer.length());
                    outStream.flush();

                    if (outStream  != null) {
                        outStream.close();
                        outStream = null;
                    }

                    if (inStream  != null) {
                        inStream.close();
                        inStream = null;
                    }

                    hConnection.disconnect();
                    hConnection = null;
                } catch (SocketException ee) {
                    Log.e(TAG, "error in sendClientFinishNotify:" + ee.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "error in sendClientFinishNotify:" + e.getMessage());
                } finally {
                    try {
                        if (inStream  != null) {
                            inStream.close();
                            inStream = null;
                        }

                        if (outStream  != null) {
                            outStream.close();
                            outStream = null;
                        }

                        if (hConnection != null) {
                            hConnection.disconnect();
                            hConnection = null;
                        }
                    } catch (IOException ioe) {
                        Log.e(TAG, "close:" + ioe.getMessage());
                    }
                }
            }
        };
        t.start();
    }

    private int sendDataFile(File inFile, Uri uri, int groupId, int order, int count, UploadInfo item) {
        int result = OK;
        byte[] buffer;
        int fileOffset = 0;
        OutputStream outputStream = null;
        FileInputStream fileInputStream = null;
        int bytesRead, bytesAvailable, bufferSize;

        try {
            String filename = inFile.getName();
            URL sUrl = null;

            if (mFileName != null) {
                filename = mFileName;
            }

            filename = Uri.encode(filename);

            if (uri.getQuery() != null) {
                sUrl = new URL(sUploadServer + "/" + filename + "?" + uri.getEncodedQuery());
            } else {
                sUrl = new URL(sUploadServer + "/" + filename);
            }

            mConnection = (HttpURLConnection) sUrl.openConnection();

            mConnection.setDoInput(true);
            mConnection.setDoOutput(true);
            mConnection.setUseCaches(false);
            mConnection.setFixedLengthStreamingMode((int) inFile.length());

            mConnection.setRequestMethod("POST");
            mConnection.setConnectTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
            mConnection.setReadTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);

            outputStream = mConnection.getOutputStream();
            fileInputStream = new FileInputStream(inFile);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);

            Log.d(TAG, "buffer size:"  + bufferSize);

            if (inFile.length() < bufferSize) {
                bufferSize = (int) inFile.length();
            }

            buffer = new byte[bufferSize];

            item.setState(HotKnotBeamConstants.State.RUNNING);

            //Start to read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0 && !isCancelled()) {
                //Log.d(TAG, "[client]write:"  + bytesAvailable + "/" + bufferSize);
                outputStream.write(buffer, 0, bufferSize);
                fileOffset += bufferSize;

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                item.setCurrentBytes(fileOffset);
            }

            if (!isCancelled()) {
                int serverResponseCode = mConnection.getResponseCode();
                String serverResponseMessage = mConnection.getResponseMessage();
                Log.i(TAG, "Http Response :" + serverResponseCode + ":" + serverResponseMessage);
                item.setResult(true);
            } else {
                item.setFailReason(FailureReason.USER_CANCEL_TX);
            }

            synchronized (mUploadList) {
                if ((item.isGroup() && !item.isLastOne() && item.getResult())) {
                    item.setState(HotKnotBeamConstants.State.RUNNING);
                } else {
                    item.setState(HotKnotBeamConstants.State.COMPLETE);
                }
            }

            //outputStream.writeBytes(LINE_END + TWO_HYPHENS + BOUNDARY + LINE_END);
            outputStream.flush();

            if (outputStream  != null) {
                try {
                    outputStream.close();
                } catch (IOException pe) {
                    pe.printStackTrace();
                    Log.e(TAG, "error in ioe:" + pe.getMessage());
                } finally {
                    outputStream = null;
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Log.e(TAG, "error in ioe:" + ioe.getMessage());
            //Check the IOException is triggerred by cancel procedure,
            //such as java.io.InterruptedIOException
            if (isCancelled()) {
                item.setFailReason(FailureReason.USER_CANCEL_TX);
                result = OK;
            } else {
                result = FAILED;
            }

            if (ioe.getMessage() != null) {
                String msg = ioe.getMessage();

                if (msg.indexOf("ECONNREFUSED") != -1 || msg.indexOf("EHOSTUNREACH") != -1) {
                    result = RETRY;
                }
            }
        } catch (IllegalArgumentException ee) {
            ee.printStackTrace();
            result = FAILED;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }

                if (outputStream  != null) {
                    outputStream.close();
                }

                if (mConnection != null) {
                    mConnection.disconnect();
                    mConnection = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "finally:" + e.getMessage());
            }
        }

        return result;
    }

    private int sendRawData(Uri uri, String filename, int filesize,
                            int order, int count,
                            HotKnotFileType mimeType) {
        int result = OK;
        byte[] buffer;
        int fileOffset = 0;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        int bytesRead, bytesAvailable, bufferSize;
        UploadInfo item = null;

        Log.d(TAG, "sendRawData:"  + uri);

        if (mimeType == HotKnotFileType.RAW) {
            synchronized (mUploadList) {
                item = new UploadInfo(filename, filesize,
                                     -1, order, count, this, mContext);
                mUploadList.add(item);
                mTaskId = item.getId();
            }
        }

        try {
            URL sUrl = null;

            inputStream = mContext.getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                throw new FileNotFoundException("Failed to open " + uri);
            }

            filename = Uri.encode(filename);

            if (uri.getQuery() != null) {
                sUrl = new URL(sUploadServer + "/" + filename + "?" + uri.getEncodedQuery());
            } else {
                if (mimeType == HotKnotFileType.CONTACT) {
                    sUrl = new URL(sUploadServer + "/" + filename +
                    "?intent=com.mediatek.hotknot.action.VCARD_DISCOVERD&mimetype=text%2Fvcard");
                } else {
                    sUrl = new URL(sUploadServer + "/" + filename);
                }
            }

            mConnection = (HttpURLConnection) sUrl.openConnection();

            mConnection.setDoInput(true);
            mConnection.setDoOutput(true);
            mConnection.setUseCaches(false);
            mConnection.setFixedLengthStreamingMode(inputStream.available());

            mConnection.setRequestMethod("POST");
            mConnection.setConnectTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
            mConnection.setReadTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
            mConnection.setWriteTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);

            outputStream = mConnection.getOutputStream();


            bytesAvailable = inputStream.available();
            bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);

            Log.d(TAG, "buffer size:"  + bufferSize);
            buffer = new byte[bufferSize];

            //Start to read file
            bytesRead = inputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0 && !isCancelled()) {
                //Log.d(TAG, "[client]write:"  + bytesAvailable + "/" + bufferSize);
                outputStream.write(buffer, 0, bufferSize);
                fileOffset += bufferSize;

                bytesAvailable = inputStream.available();
                bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);
                bytesRead = inputStream.read(buffer, 0, bufferSize);
            }

            //outputStream.writeBytes(LINE_END + TWO_HYPHENS + BOUNDARY + LINE_END);
            outputStream.flush();

            if (outputStream  != null) {
                outputStream.close();
                outputStream = null;
            }

            if (mimeType == HotKnotFileType.RAW) {
                synchronized (mUploadList) {
                    if (!isCancelled()) {
                        item.setResult(true);
                        item.setState(HotKnotBeamConstants.State.COMPLETE);
                    } else {
                        item.setFailReason(FailureReason.USER_CANCEL_TX);
                    }
                }
            }
            int serverResponseCode = mConnection.getResponseCode();
            String serverResponseMessage = mConnection.getResponseMessage();
            Log.i(TAG, "Http Response :" + serverResponseCode + ":" + serverResponseMessage);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Log.e(TAG, "error in ioe:" + ioe.getMessage());

            if (mimeType == HotKnotFileType.RAW) {
                synchronized (mUploadList) {
                    if (isCancelled()) {
                        item.setFailReason(FailureReason.USER_CANCEL_TX);
                    } else {

                        item.setFailReason(FailureReason.CONNECTION_ISSUE);
                    }
                }
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream  != null) {
                    outputStream.close();
                }

                if (mConnection != null) {
                    mConnection.disconnect();
                    mConnection = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "finally:" + e.getMessage());
            }
        }

        return result;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onCancelled() {
        Log.i(TAG, "onCancelled");
        Log.d(TAG, "Finish upload action");

        if (mPostExecuteCB != null) {
            mPostExecuteCB.onPostExecute(null, this);
        }
    }

    @Override
    protected Void doInBackground(Uri... uris) {
        File[] fileList = null;
        File inFile = null;
        int i = 0, j = 0;
        int groupId = -1;
        int sendResult = OK;
        int totalSize = 0;

        //Check phone contact firstly
        if (uris.length >= 1) {
            if (MimeUtilsEx.isContactUri(uris[0])) {
                for (i = 0; i < uris.length; i++) {
                    if (MimeUtilsEx.isContactUri(uris[i])) {
                        try {
                            sendRawData(uris[i], HotKnotBeamConstants.CONTACT_FILE_NAME, 0,
                                    i, uris.length, HotKnotFileType.CONTACT);
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "err:" + e);
                            return null;
                        }
                    } else {
                        Log.i(TAG, "Not vcard mime type:" + uris[i]);
                    }
                }
                Log.i(TAG, "done in sendRawData");
                return null;
            } else if (MimeUtilsEx.isRawUri(uris[0])) {
                for (i = 0; i < uris.length; i++) {
                    if (MimeUtilsEx.isRawUri(uris[i])) {
                        String filename = MimeUtilsEx.getRawFilename(mContext, uris[i]);
                        int filesize = MimeUtilsEx.getRawFilesize(mContext, uris[i]);
                        if (filename != null && filesize > -1) {
                            Log.i(TAG, "Filename:" + filename);
                            sendRawData(uris[i], filename, filesize,
                                        i, uris.length,
                                        HotKnotFileType.RAW);
                        }
                    } else {
                        Log.i(TAG, "Not mime type:" + uris[i]);
                    }
                }
                Log.i(TAG, "done in sendRawData");
                return null;
            }
        }

        if (uris.length != 1) {
            groupId = Utils.getGroupId();
        }

        fileList = new File[uris.length];

        for (i = 0; i < uris.length; i++) {
            Uri uri = uris[i];
            fileList[i] = MimeUtilsEx.getFilePathFromUri(uri, mContext);

            if (fileList[i] == null) {
                Log.e(TAG, "The file is not existed:" + fileList[i]);
                return null;
            }

            if (mPostExecuteCB != null) {
                mPostExecuteCB.onSetExternalPath(
                    Environment.isExternalStorageRemovable(fileList[i]));
            }

            totalSize += fileList[i].length();
        }

        Log.i(TAG, "The uris is :" + uris.length + " groupID:" + groupId + ":" + totalSize);

        UploadInfo item = null;

        for (i = 0; i < uris.length; i++) {
            Uri uri = uris[i];

            Log.i(TAG, "process[" + i + "] " + uri);

            inFile = fileList[i];

            try {
                Log.i(TAG, "Transfer File:" + inFile.getName() + ":" + inFile.length());

                Log.e(TAG, "max file size:" + HotKnotBeamConstants.MAX_FILE_UPLOAD_SIZE);

                if (inFile.length() > HotKnotBeamConstants.MAX_FILE_UPLOAD_SIZE) {
                    Log.e(TAG, "The file size is not allowed");
                    return null;
                }

            } catch (NullPointerException e) {
                Log.e(TAG, "transfer file:" + e.getMessage());
            }

            if (groupId != HotKnotBeamConstants.NON_GROUP_ID) {
                Uri.Builder tmpBuilder = uri.buildUpon().appendQueryParameter(HotKnotBeamConstants.QUERY_GROUPID, String.valueOf(groupId));
                tmpBuilder.appendQueryParameter(HotKnotBeamConstants.QUERY_ORDER, String.valueOf(i));
                tmpBuilder.appendQueryParameter(HotKnotBeamConstants.QUERY_NUM, String.valueOf(uris.length));
                tmpBuilder.appendQueryParameter(HotKnotBeamConstants.QUERY_TOTAL_SIZE, String.valueOf(totalSize));
                uri = tmpBuilder.build();
            }

            //Add notificaiton item
            synchronized (mUploadList) {
                String filename = inFile.getName();

                if (mFileName != null) {
                    filename = mFileName;
                }

                if (groupId == HotKnotBeamConstants.NON_GROUP_ID) {
                    item = new UploadInfo(filename, (int) inFile.length(), groupId, i, uris.length, this, mContext);
                    mUploadList.add(item);
                } else {
                    item = getGroupInfo(groupId);

                    if (item == null) {
                        item = new UploadInfo(filename, (int) inFile.length(), groupId, i, uris.length, this, mContext);
                        mUploadList.add(item);
                    } else {
                        item.mOrder = (i + 1);
                        item.setTotalBytes((int) inFile.length());
                        item.setCurrentBytes(0);
                        item.setTitle(filename);
                        item.setFileUploadTask(this);
                    }
                }

                mTaskId = item.getId();
            }

            for (j = 0; j < HotKnotBeamConstants.MAX_RETRY_COUNT; j++) {
                try {
                    if (j != 0) {
                        Thread.sleep(HotKnotBeamConstants.RETRY_SLEEP_TIMER * j);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException");
                }

                sendResult = sendDataFile(inFile, uri, groupId, i, uris.length, item);
                Log.i(TAG, "send result:" + sendResult);

                if (sendResult == OK) { //Check retry or not
                    Log.i(TAG, "[OK]send finished");
                    break;
                } else if (sendResult == FAILED) {
                    Log.i(TAG, "[FAILED]send finished");
                    break;
                }
            }

            synchronized (mUploadList) {
                if ((item.isGroup() && !item.isLastOne() && item.getResult())) {
                    item.setState(HotKnotBeamConstants.State.RUNNING);
                } else {
                    item.setState(HotKnotBeamConstants.State.COMPLETE);
                }
            }

            if (sendResult == FAILED || j == HotKnotBeamConstants.MAX_RETRY_COUNT) {
                Log.e(TAG, "Failed to send. Cancel this upload");
                item.setFailReason(FailureReason.CONNECTION_ISSUE);
                if (sendResult == FAILED) {
                    sendClientFinishNotify();
                }
                break;
            } else if (sendResult == OK && item.getFailReason() == FailureReason.USER_CANCEL_TX) {
                Log.i(TAG, "Stop to upload, due to user cancel");
                sendClientFinishNotify();
                break;
            }

            if (item.isGroup()) {
                Log.i(TAG, "item:" + item.mOrder);
                item.setDoneItem(item.mOrder);
            }
        }

        if (uris.length == 1) {
            String queryString = uris[0].getQuery();
            if (queryString != null && queryString.indexOf(HotKnotBeamConstants.QUERY_ZIP
                        + "=" + HotKnotBeamConstants.QUERY_VALUE_YES) != -1) {
                Log.d(TAG, "Remove zip file");
                if (inFile != null) {
                    inFile.delete();
                }
            }
        }

        fileList = null;

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d(TAG, "Finish upload action");

        if (mPostExecuteCB != null) {
            mPostExecuteCB.onPostExecute(result, this);
        }
    }


    public Collection<UploadInfo> getUploadInfos() {

        synchronized (mUploadList) {
            if (mUploadList.size() > 0) {
                return mUploadList;
            }
        }

        return null;
    }

    private UploadInfo getGroupInfo(int groupId) {
        synchronized (mUploadList) {
            for (UploadInfo info : mUploadList) {
                if (info.mGroupId == groupId) {
                    return info;
                }
            }
        }

        return null;
    }

    public void resetNotificaiton() {
        Log.d(TAG, "resetNotificaiton in UL");
        synchronized (mUploadList) {
            for (UploadInfo info : mUploadList) {
                if (info.mState != State.COMPLETE) {
                    info.setFailReason(FailureReason.UNKNOWN_ERROR);
                }
            }
        }
    }

}