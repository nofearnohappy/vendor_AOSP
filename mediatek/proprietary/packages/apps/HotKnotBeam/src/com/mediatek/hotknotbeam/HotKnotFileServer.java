package com.mediatek.hotknotbeam;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.mediatek.hotknot.HotKnotAdapter;

import com.mediatek.hotknotbeam.HotKnotBeamConstants.FailureReason;
import com.mediatek.hotknotbeam.HotKnotBeamConstants.State;
import com.mediatek.storage.StorageManagerEx;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class HotKnotFileServer {
    private final static String TAG = HotKnotBeamService.TAG;

    private final static String CRLF = "\r\n";
    private final static String CONTENT_LEN = "content-length";
    private final static String CONTENT_TYPE = "content-type";

    private final static int MAX_BUFFER_SIZE = 16 * 1024;

    private static boolean mIsServerRunning = false;
    private static int mPort = HotKnotBeamService.SERVICE_PORT;

    // LinkList to queue the download request
    private LinkedList<DownloadInfo> mDownloadList = new LinkedList<DownloadInfo>();

    private Context             mContext = null;
    private ServerSocket        mServerSocket = null;
    private Thread              mServerThread = null;
    private HotKnotFileServerCb mHotKnotFileServerCb = null;
    private HotKnotFileServerCb mHotKnotFileServerUiCb = null;
    private String              mDeviceName;

    private static HotKnotFileServer mHotKnotFileServer = null;

    /**
      * Interface callback for HotKnotFileServer.
      *
      */
    interface HotKnotFileServerCb {
        void onHotKnotFileServerFinish(int status);
        void onUpdateNotification();
        void onSetExternalPath(boolean isExternal);
        void onStartUiActivity(int id);
    }

    public static HotKnotFileServer getInstance() {
        return mHotKnotFileServer;
    }

    public HotKnotFileServer(int port, Context context, String deviceName) {
        mPort = port;
        mContext = context;
        mIsServerRunning = false;
        mDeviceName = deviceName;
        mHotKnotFileServer = this;
    }

    public void execute() {
        mIsServerRunning = true;

        try {
            mServerSocket = new ServerSocket(mPort);
            mServerThread = new Thread(new ServerThread());
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            mIsServerRunning = false;
        }
    }

    public void cancel(int id) {
        synchronized (mDownloadList) {
            for (DownloadInfo info : mDownloadList) {
                if (info.mId == id) {
                    if (info.mState != State.COMPLETE) {
                        CommunicationThread cmThread = info.getClientThread();
                        Log.d(TAG, "interrupt thread");
                        info.setFailReason(FailureReason.USER_CANCEL_RX);
                        cmThread.close();
                        cmThread.interrupt();
                    }

                    break;
                }
            }
        }
    }

    public void resetNotificaiton() {
        Log.d(TAG, "resetNotificaiton in DL");
        synchronized (mDownloadList) {
            for (DownloadInfo info : mDownloadList) {
                if (info.mState != State.COMPLETE) {
                    info.setFailReason(FailureReason.UNKNOWN_ERROR);
                }
            }
        }
    }

    public void stop() {
        mIsServerRunning = false;

        try {
            mServerThread.interrupt();
            mServerSocket.close();
        } catch (IOException ioe) {
            Log.e(TAG, "stop in server thread:" + ioe.getMessage());
        } catch (NullPointerException ne) {
            Log.e(TAG, "stop in server thread:" + ne.getMessage());
        }
    }

    public void setHotKnotFileServerCb(HotKnotFileServerCb cb) {
        mHotKnotFileServerCb = cb;
    }

    public void setHotKnotFileServerUiCb(HotKnotFileServerCb cb) {
        mHotKnotFileServerUiCb = cb;
    }

    public Collection<DownloadInfo> getDownloadInfos() {

        synchronized (mDownloadList) {
            if (mDownloadList.size() > 0) {
                return mDownloadList;
            }
        }

        return null;
    }

    class ServerThread extends Thread {

        public void run() {
            Socket socket = null;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Log.d(TAG, "Server Listen");
                    socket = mServerSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    Log.e(TAG, "accept error:" + e.getMessage());
                }
            }

            if (!mIsServerRunning) {
                Log.d(TAG, "Notify server thread is ended");
                mHotKnotFileServerCb.onHotKnotFileServerFinish(0);
            }
        }
    }

    protected class CommunicationThread extends Thread {
        private Socket mClientSocket;
        private InputStream input;
        private BufferedOutputStream  outBody;
        DownloadInfo mInfo;
        private int mOrder = -1;
        private boolean mIsOpenApp = false;

        public CommunicationThread(Socket clientSocket) {
            mClientSocket = clientSocket;

            try {
                mClientSocket.setSoTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
                mClientSocket.setSoSndTimeout(HotKnotBeamConstants.MAX_TIMEOUT_VALUE);
                mClientSocket.setReceiveBufferSize(MAX_BUFFER_SIZE);
                mClientSocket.setSoLinger(false, 0);
                input = mClientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.shutdownOutput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "error:" + e.getMessage());
            }
        }

        public void run() {
            try {
                Log.d(TAG, "Client Connect:" + mClientSocket.getRemoteSocketAddress());
                String rootPath = MimeUtilsEx.getSaveRootPath(mContext,
                    HotKnotBeamConstants.MAX_HOTKNOT_BEAM_FOLDER);
                Log.d(TAG, "Save root path:" + rootPath);
                boolean isReadSuccess = doDownload(rootPath);
            } catch (IOException e) {
                Log.e(TAG, "error:" + e.getMessage());
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }

                    if (mClientSocket != null) {
                        mClientSocket.close();
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "finally:" + ex.getMessage());
                }
            }

            Log.d(TAG, "CommunicationThread is finished");
        }

        private boolean doDownload(String rootPath) throws IOException {
            String line = "";
            String fileName = "";
            String extInfo = "";
            int    fileSize = 0;
            mIsOpenApp = false;

            //Get FileName
            line = readAsciiLine(input);
            fileName = Uri.decode(parseFileName(line));
            extInfo  = parseExtInfo(line);
            Log.d(TAG, "File info:" + fileName + ":" + extInfo);

            if (fileName.length() == 0) {
                Log.e(TAG, "can't get file name");
                return false;
            }

            if (fileName != null && fileName.equals(HotKnotBeamConstants.BEAM_FINISH_COMMAND)) {
                Log.d(TAG, "Terminate server thread");
                mHotKnotFileServer.resetNotificaiton();

                if (mHotKnotFileServerUiCb != null) {
                    mHotKnotFileServerUiCb.onUpdateNotification();
                }
                if (mHotKnotFileServerCb != null) {
                    mHotKnotFileServerCb.onUpdateNotification();
                }

                try {
                    outBody = new BufferedOutputStream(mClientSocket.getOutputStream());
                    byte[] response = getResponse(HttpURLConnection.HTTP_OK);
                    outBody.write(response, 0, response.length);
                    outBody.flush();

                    try {
                        //wait for client thread in remote side to hanlde finish procedure
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "sleep:" + e.getMessage());
                    }

                    //ALPS02379228 Keep file server running
                    //mHotKnotFileServer.stop();
                } catch (IOException e) {
                    Log.e(TAG, "[mHotKnotFileServer]" + e.getMessage());
                }
                return true;
            }

            do {
                line = readAsciiLine(input);
                line = line.toLowerCase();

                //Get File Size
                if (line.indexOf(CONTENT_LEN) != -1) {
                    String contentLen = line.substring(line.indexOf(":") + 1);
                    fileSize = Integer.parseInt(contentLen.trim());
                } else if (line.indexOf(CONTENT_TYPE) != -1) {

                }
            } while (line.length() != 0);

            //Handle contact exchange
            String appIntent = getAppIntent(extInfo);
            if (fileName.equals(HotKnotBeamConstants.CONTACT_FILE_NAME) ||
                  appIntent.equals(HotKnotBeamConstants.CONTACT_INTENT)) {
                Log.d(TAG, "Receive contact info");
                outBody = new BufferedOutputStream(mClientSocket.getOutputStream());
                handleContactInfo(fileSize, extInfo);
                return true;
            }

            int groupId = getGroupId(extInfo);
            boolean isFirstItem = true;
            Log.d(TAG, "File info:" + fileName + ":" + fileSize + ":" + groupId);

            //Create one donwload info
            synchronized (mDownloadList) {
                if (groupId == HotKnotBeamConstants.NON_GROUP_ID) {
                    mInfo = new DownloadInfo(rootPath, fileName, fileSize,
                                        groupId, mDeviceName, this, mContext);

                    if (extInfo != null) {
                        mInfo.setExtInfo(extInfo);
                        Log.d(TAG, "set extInfo:" + extInfo);
                    }
                } else {
                    mInfo = getGroupInfo(groupId);

                    if (mInfo == null) {
                        mInfo = new DownloadInfo(rootPath, fileName, fileSize, groupId,
                                                mDeviceName, this, mContext);
                        int order = getIntInfoFromExt(extInfo,
                                                HotKnotBeamConstants.QUERY_ORDER, 0);

                        if (order != 0) {
                            //Log.e(TAG, "[Error] No sequence file transfer");
                            //throw new IOException();
                        }

                        if (extInfo != null) {
                            mInfo.setExtInfo(extInfo);
                            Log.d(TAG, "set extInfo:" + extInfo);
                        }
                    } else {
                        //Group item, only update order & title
                        isFirstItem = false;
                        mInfo.mOrder = getIntInfoFromExt(extInfo,
                                                    HotKnotBeamConstants.QUERY_ORDER, 0) + 1;
                        mInfo.setTotalBytes(fileSize);
                        mInfo.setCurrentBytes(0);
                        mInfo.setTitle(fileName);
                        mInfo.setFileName(fileName);
                        mInfo.setSaveFolder(getInfoFromExt(extInfo,
                                                HotKnotBeamConstants.QUERY_FOLDER));
                        mInfo.setCommunicationThread(this);
                    }
                }

                mOrder = mInfo.mOrder;
            }

            rootPath = mInfo.getSaveFolder();

            Log.i(TAG, "Start UI activity:" + isFirstItem);

            //Prepare UI display & Notification
            synchronized (mDownloadList) {
                if (mInfo.isShowNotification() && isFirstItem) {
                    mDownloadList.add(mInfo);

                    //Lanuch UI activity
                    if (mInfo.isShowUiApp()) {
                        PowerManager powerManager = (PowerManager)
                            mContext.getSystemService(Context.POWER_SERVICE);
                        if (powerManager.isInteractive()) {
                            MimeUtilsEx.startRxUiActivity(mContext, mInfo.mId);
                        } else {
                            Log.i(TAG, "Start UI activity in service thread");
                            mHotKnotFileServerCb.onStartUiActivity(mInfo.mId);
                        }
                    } else {
                        mIsOpenApp = true;
                    }
                }
            }

            mHotKnotFileServerCb.onUpdateNotification();

            if (mHotKnotFileServerUiCb != null) {
                mHotKnotFileServerUiCb.onUpdateNotification();
            }

            boolean isRename = mInfo.isRenameFile();

            if (mInfo.isCompressed()) {
                SimpleDateFormat tmp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
                fileName = tmp.format(new Date());
                isRename = false;
            }

            File outFile = null;
            FileOutputStream fout = null;
            int recvRemainBytes = 0;

            try {
                outFile = prepareFile(new File(rootPath), fileName, isRename, isFirstItem);
                mHotKnotFileServerCb.onSetExternalPath(
                        Environment.isExternalStorageRemovable(outFile));
                fout = new FileOutputStream(outFile);

                recvRemainBytes = fileSize;
                int recvBytes = 0, bufferSize = 0;
                byte[] buffer = new byte[MAX_BUFFER_SIZE];

                mInfo.setState(HotKnotBeamConstants.State.RUNNING);

                outBody = new BufferedOutputStream(mClientSocket.getOutputStream());
                bufferSize = Math.min(fileSize, MAX_BUFFER_SIZE);

                do {
                    recvBytes = input.read(buffer, 0, bufferSize);

                    if (recvBytes <= 0) {
                        Log.d(TAG, "remote ends the stream:" + recvBytes);
                        break;
                    }

                    recvRemainBytes -= recvBytes;
                    fout.write(buffer, 0, recvBytes);
                    bufferSize = Math.min(recvRemainBytes, MAX_BUFFER_SIZE);
                    mInfo.setCurrentBytes(fileSize - recvRemainBytes);
                } while (recvRemainBytes > 0);

                Log.d(TAG, "Transfer done");

                //Succesesfully transfer done
                if (recvRemainBytes == 0) {

                    if (mInfo.isCompressed()) {
                        fout.close();
                        fout = null;
                        ZipFileUtils.unzip(outFile, new File(rootPath), mContext);
                        outFile.delete();
                        Log.d(TAG, "Unzip successfully");
                        updateItemDone();
                    }

                    byte[] response = getResponse(HttpURLConnection.HTTP_OK);
                    outBody.write(response, 0, response.length);
                    outBody.flush();
                }
            } catch (IOException ioe) {
                String msg = ioe.getMessage();
                Log.e(TAG, "error in ioe:" + msg);

                if (msg != null && (msg.indexOf("ENOSPC") != -1 || msg.indexOf("EACCES") != -1)) {
                    mInfo.setFailReason(FailureReason.LOW_STORAGE);
                } else {
                    if (mInfo.getFailReason() == FailureReason.NONE) {
                        mInfo.setFailReason(FailureReason.CONNECTION_ISSUE);
                    }
                }
            } finally {
                try {
                    //Hanle failure case firstly
                    if (recvRemainBytes > 0 || mInfo.getFailReason() != FailureReason.NONE) {
                        Log.e(TAG, "Transfer failed");
                        outFile.delete();

                        if (mInfo.getFailReason() == FailureReason.NONE) {
                            mInfo.setFailReason(FailureReason.UNKNOWN_ERROR);
                        }

                        mInfo.setResult(false);
                        mInfo.setState(HotKnotBeamConstants.State.COMPLETE);
                        mHotKnotFileServerCb.onUpdateNotification();

                        if (mHotKnotFileServerUiCb != null) {
                            mHotKnotFileServerUiCb.onUpdateNotification();
                        }
                    } else if (!mInfo.isCompressed()) {
                        //Update the gallery database
                        String[] paths = new String[1];
                        Log.d(TAG, "path:" + outFile.getCanonicalPath());
                        paths[0] = outFile.getCanonicalPath();

                        if (mInfo.isGroup()) {
                            mInfo.setDoneItem(mInfo.mOrder);
                        }

                        MediaScannerConnection.scanFile(mContext, paths, null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.d(TAG, "onScanCompleted:" + path + ":" + uri);
                                        updateItemDone();
                                    }
                                });
                    }

                    if (fout != null) {
                        fout.close();
                        fout = null;
                    }

                    if (outBody != null) {
                        outBody.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream: " + e.getMessage());
                }

            }

            return true;
        }

        private void updateItemDone() {
            //Demo purpose
            if (mIsOpenApp) {
                openUiActivity(mInfo, mContext);
            }

            synchronized (mDownloadList) {
                Log.d(TAG, "order:" + mOrder + ":" + mInfo.mOrder);

                if (mOrder == mInfo.mOrder) {
                    mInfo.setResult(true);

                    if ((mInfo.isGroup() && !mInfo.isLastOne())) {
                        mInfo.setState(HotKnotBeamConstants.State.RUNNING);
                    } else {
                        mInfo.setState(HotKnotBeamConstants.State.COMPLETE);
                    }
                }
            }

            mHotKnotFileServerCb.onUpdateNotification();

            if (mHotKnotFileServerUiCb != null) {
                mHotKnotFileServerUiCb.onUpdateNotification();
            }
        }


        private String readAsciiLine(InputStream in) throws IOException {
            StringBuilder result = new StringBuilder(80);

            while (true) {
                int c = in.read();

                if (c == -1) {
                    throw new EOFException();
                } else if (c == '\n') {
                    break;
                }

                result.append((char) c);
            }

            int length = result.length();

            if (length > 0 && result.charAt(length - 1) == '\r') {
                result.setLength(length - 1);
            }

            return result.toString();
        }

        private String parseFileName(String line) {
            try {
                int dotPos = line.indexOf('/');
                int dotPos2 = 0;

                if (line.indexOf('?') != -1) {
                    dotPos2 = line.indexOf('?');
                } else {
                    dotPos2 = line.lastIndexOf("HTTP/1.1") - 1;
                }

                return line.substring(dotPos + 1, dotPos2).trim();
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

            return "";
        }

        private File prepareFile(File fileDir, String fileName, boolean isRename,
                    boolean isFirstItem) {
            int pos = fileName.indexOf('?');

            if (pos != -1) {
                fileName = fileName.substring(0, pos - 1);
            }

            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            File file = new File(fileDir, fileName);

            if (file.exists() && isRename) {
                String subFileName = "";
                StringBuilder fileNameBuilder = new StringBuilder();

                int dot = fileName.indexOf(".");

                if (dot != -1) {
                    subFileName = fileName.substring(dot);
                    fileNameBuilder.append(fileName.substring(0, dot));
                } else {
                    fileNameBuilder.append(fileName);
                }

                int fileMainNameLen = fileNameBuilder.length();
                int i = 1;
                File newFile = null;

                while (true) {
                    fileNameBuilder.append("(").append(i++).append(")")
                    .append(subFileName);

                    newFile = new File(fileDir, fileNameBuilder.toString());

                    if (!newFile.exists()) {
                        if (isFirstItem) {
                            mInfo.setFileName(fileNameBuilder.toString());
                        }

                        return newFile;
                    }

                    fileNameBuilder.setLength(fileMainNameLen);
                }
            } else {
                return file;
            }
        }

        private byte[] getResponse(int status) {
            String response = "HTTP/1.1 200 OK\r\n\r\n";
            return response.getBytes();
        }

        private String parseExtInfo(String line) {
            String extInfo = null;
            int pos = line.indexOf('?');
            int pos2 = line.lastIndexOf("HTTP/1.1") - 1;

            if (pos == -1) {
                return extInfo; //No support query string
            }

            extInfo = line.substring(pos + 1, pos2);

            try {
                Uri.decode(extInfo);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "decode:" + e.getMessage());
            }

            return extInfo;
        }

        private void handleContactInfo(int fileSize, String extInfo) {
            int recvRemainBytes = fileSize;
            byte[] dataBuffer = new byte[fileSize];
            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            int recvBytes = 0, bufferSize = 0;

            try {
                bufferSize = Math.min(fileSize, MAX_BUFFER_SIZE);

                do {
                    recvBytes = input.read(buffer, 0, bufferSize);

                    if (recvBytes == 0) {
                        Log.d(TAG, "remote ends the stream");
                        break;
                    }

                    System.arraycopy(buffer, 0, dataBuffer, fileSize - recvRemainBytes, recvBytes);
                    recvRemainBytes -= recvBytes;
                    bufferSize = Math.min(recvRemainBytes, MAX_BUFFER_SIZE);
                } while (recvRemainBytes > 0);

                byte[] response = getResponse(HttpURLConnection.HTTP_OK);
                outBody.write(response, 0, response.length);
                outBody.flush();

                Log.i(TAG, "The file size is " + fileSize);
                if (fileSize < IBinder.MAX_IPC_SIZE) {
                    String appIntent = getAppIntent(extInfo);
                    Log.i(TAG, "The intent of contact is " + appIntent);
                    Intent intent = new Intent(appIntent);
                    intent.putExtra(HotKnotAdapter.EXTRA_DATA, dataBuffer);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.startActivity(intent);
                } else {
                    String root = StorageManagerEx.getDefaultPath();
                    File vcardFile = new File(root + "/" + HotKnotBeamConstants.CONTACT_FILE_NAME);
                    FileOutputStream output = new FileOutputStream(vcardFile);
                    output.write(dataBuffer);
                    output.close();
                    Uri path = Uri.fromFile(vcardFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndTypeAndNormalize(path, HotKnotBeamConstants.MIME_TYPE_VCARD);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outBody != null) {
                        outBody.close();
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    public DownloadInfo getDownloadItem(int id) {
        synchronized (mDownloadList) {
            for (DownloadInfo info : mDownloadList) {
                if (info.mId == id) {
                    return info;
                }
            }
        }

        return null;
    }

    private String getAppIntent(String ext) {
        return getInfoFromExt(ext, HotKnotBeamConstants.QUERY_INTENT);
    }

    private int getGroupId(String ext) {
        return getIntInfoFromExt(ext, HotKnotBeamConstants.QUERY_GROUPID,
                                    HotKnotBeamConstants.NON_GROUP_ID);
    }

    private int getIntInfoFromExt(String ext, String fieldName, int defaultValue) {
        int fieldValue = defaultValue;
        String tmpValue = getInfoFromExt(ext, fieldName);

        if (tmpValue == null || tmpValue.length() == 0) {
            return HotKnotBeamConstants.NON_GROUP_ID;
        }

        try {
            fieldValue = Integer.parseInt(tmpValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return fieldValue;
    }

    private String getInfoFromExt(String ext, String fieldName) {
        if (ext == null) {
            return "";
        }

        int start = ext.indexOf(fieldName);

        if (start == -1) {
            return "";
        }

        int separator = ext.indexOf('=', start);

        if (separator == -1) {
            return "";
        }

        int next = ext.indexOf('&', separator);
        int end = (next == -1) ? ext.length() : next;

        return ext.substring(separator + 1, end);
    }

    private DownloadInfo getGroupInfo(int groupId) {
        synchronized (mDownloadList) {
            for (DownloadInfo info : mDownloadList) {
                if (info.mGroupId == groupId) {
                    return info;
                }
            }
        }

        return null;
    }

    protected static void openUiActivity(DownloadInfo info, Context context) {
        String mimeType = info.getMimeType();
        String appIntent = info.getAppIntent();
        boolean isCheck = info.isMimeTypeCheck();
        Uri uri = info.getUri();

        final Intent intent = new Intent(HotKnotBeamService.HOTKNOT_DL_COMPLETE,
                                        null, context, HotKnotBeamReceiver.class);
        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_INTENT, appIntent);
        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_URI, uri);
        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_MIMETYPE, mimeType);
        intent.putExtra(HotKnotBeamService.HOTKNOT_EXTRA_APP_ISCHECK, isCheck);
        context.sendBroadcast(intent);
    }
}