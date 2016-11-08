package com.mediatek.hotknotbeam;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import com.mediatek.hotknotbeam.FileUploadTask.FileUploadTaskListener;
import com.mediatek.hotknotbeam.HotKnotFileServer.HotKnotFileServerCb;
import com.mediatek.hotknotbeam.IHotKnotBeamService;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;


public class HotKnotBeamService extends Service implements Handler.Callback {
    static protected final String TAG = "HotKnotBeamService";

    static final int LOCAL_NETWORK_ID = 99;

    static final String TEST_INTENT = "com.mediatek.hotknot.upload";
    static final String SERVER_INTENT = "com.mediatek.hotknot.server";
    static final String HOTKNOT_BEAMING = "com.mediatek.howknot.beaming";
    static final String HOTKNOT_CANCEL_BEAMING = "com.mediatek.howknot.cance.beaming";
    static final String HOTKNOT_SEND = "com.mediatek.hotknotbeam.sendfile";
    static final String HOTKNOT_RECV = "com.mediatek.hotknotbeam.recvfile";
    static final String HOTKNOTBEAM_START = "com.mediatek.hotknotbeam.START";
    static final String HOTKNOT_FINISH = "com.mediatek.hotknotbeam.FINISH";
    static final String HOTKNOT_DL_COMPLETE = "com.mediatek.hotknotbeam.DL.COMPLETE";

    static final String HOTKNOT_EXTRA_BEAM_ID = "com.mediatek.howknot.beam.id";
    static final String HOTKNOT_EXTRA_BEAM_ITEM = "com.mediatek.howknot.beam.item";
    static final String HOTKNOT_EXTRA_BEAM_IP = "ip";
    static final String HOTKNOT_EXTRA_BEAM_URIS = "uris";
    static final String HOTKNOT_EXTRA_BEAM_PATH = "path";
    static final String HOTKNOT_EXTRA_BEAM_DEVICENAME = "devicename";

    static final String HOTKNOT_EXTRA_APP_INTENT = "appintent";
    static final String HOTKNOT_EXTRA_APP_URI = "uri";
    static final String HOTKNOT_EXTRA_APP_MIMETYPE = "miemtype";
    static final String HOTKNOT_EXTRA_APP_ISCHECK = "ischeck";


    static protected final int SERVICE_PORT = HotKnotBeamConstants.SERVICE_PORT;

    static private final int MSG_SENDER_REQ      = 0;
    static private final int MSG_RECEIVER_REQ    = 1;
    static private final int MSG_POLLING         = 2;
    static private final int MSG_CANCEL_REQ      = 3;
    static private final int MSG_CLIENT_DONE     = 4;
    static private final int MSG_CLIENT_END      = 5;
    static private final int MSG_SERVER_END      = 6;
    static private final int MSG_UPDATE_UI       = 7;
    static private final int MSG_CLIENT_CANCEL_REQ  = 8;
    static private final int MSG_START_UI_ACTIVITY  = 9;

    static private final int MSG_TEST_CLIENT_REQ      = 0x10;

    private Handler mHandler;
    private Context mContext;
    private HotKnotFileServer mHotKnotServer = null;
    private DownloadNotifier mDownloadNotifier = null;
    private UploadNotifier mUploadNotifier = null;
    private Object mPollLock = new Object();
    private Object mServerLock = new Object();
    private String mServerIP = "127.0.0.1";
    private INetworkManagementService mNMService;
    private boolean mIsP2pConnected;
    static private int mIdleRxCounter = 0;
    static private int sFileCount = 0;
    static private boolean sIsUlExternal = false;
    static private boolean sIsDlExternal = false;

    private final IHotKnotBeamService.Stub mBinder = new IHotKnotBeamService.Stub() {
        @Override
        public void sendUris(Uri[] uris, String ipAddress, int flag) {
            Log.i(TAG, "sendUris");

            if (ipAddress != null && !ipAddress.isEmpty()) {
                mServerIP = ipAddress;
                Log.d(TAG, "Server address:" + mServerIP);
            } else {
                Log.d(TAG, "Server address:" + mServerIP);
            }

            final int uriCount = uris != null ? uris.length : 0;

            if (uriCount == 0) {
                Log.e(TAG, "No Uris");
                return;
            }

            Message msg = Message.obtain();
            msg.what = MSG_SENDER_REQ;
            msg.obj = (Object) uris;
            mHandler.sendMessage(msg);

        }

        @Override
        public void prepareReceive(int flag) {
            Log.i(TAG, "prepareReceive");
            mHandler.sendEmptyMessage(MSG_RECEIVER_REQ);
        }
    };

    // LinkList to queue the upload task
    private LinkedList<FileUploadTask> mUploadTaskList = new LinkedList<FileUploadTask>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mIsP2pConnected = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction(TEST_INTENT);
        filter.addAction(SERVER_INTENT);
        filter.addAction(HOTKNOT_BEAMING);
        filter.addAction(HOTKNOT_CANCEL_BEAMING);
        filter.addAction(HOTKNOT_SEND);
        filter.addAction(HOTKNOT_RECV);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);

        IntentFilter sysFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        sysFilter.addDataScheme("file");
        registerReceiver(mSysReceiver, sysFilter);

        mHandler = new Handler(this);
        mContext = this;

        mDownloadNotifier = new DownloadNotifier(this);
        mUploadNotifier = new UploadNotifier(this);

        //Toast.makeText(this, "service is started", Toast.LENGTH_SHORT).show();
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.d(TAG, "The service is ended");
        //Toast.makeText(this, "service is stopped", Toast.LENGTH_SHORT).show();
        System.exit(0);
    }

    private BroadcastReceiver mSysReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action:" + action);
            if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                Log.d(TAG, "ACTION_MEDIA_EJECT");

                if (!sIsUlExternal && !sIsDlExternal) {
                    Log.d(TAG, "Ingore this event");
                    return;
                }

                synchronized (mPollLock) {
                    if (mHandler.hasMessages(MSG_POLLING)) {
                        mHandler.removeMessages(MSG_POLLING);
                    }
                    cancelUploadAction();
                    cancelDownloadAction();
                }
            }
        }

    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = Message.obtain();

            String action = intent.getAction();
            Log.d(TAG, "action:" + action);

            if (TEST_INTENT.equals(action)) {
                msg.what = MSG_TEST_CLIENT_REQ;
                msg.obj = intent.getStringExtra(HOTKNOT_EXTRA_BEAM_PATH);

                if (msg.obj != null) {
                    Log.d(TAG, "Test Path:" + msg.obj);
                }

                mHandler.sendMessage(msg);
            } else if (SERVER_INTENT.equals(action)) {
                msg.what = MSG_RECEIVER_REQ;
                msg.obj = intent.getStringExtra(HOTKNOT_EXTRA_BEAM_DEVICENAME);

                if (msg.obj != null) {
                    Log.d(TAG, "Device name:" + msg.obj);
                }

                mHandler.sendMessage(msg);
            } else if (HOTKNOT_CANCEL_BEAMING.equals(action)) {
                msg.what = MSG_CLIENT_CANCEL_REQ;
                msg.arg1  = intent.getIntExtra(HOTKNOT_EXTRA_BEAM_ID, 0);
                mHandler.sendMessageAtFrontOfQueue(msg);
            } else if (HOTKNOT_BEAMING.equals(action)) {
                msg.what = MSG_CANCEL_REQ;
                msg.arg1  = intent.getIntExtra(HOTKNOT_EXTRA_BEAM_ID, 0);
                mHandler.sendMessageAtFrontOfQueue(msg);
            } else if (HOTKNOT_SEND.equals(action)) {
                String ipAddress = intent.getStringExtra(HOTKNOT_EXTRA_BEAM_IP);

                if (ipAddress != null && !ipAddress.isEmpty()) {
                    mServerIP = ipAddress;
                    Log.d(TAG, "Server address:" + mServerIP);
                } else {
                    Log.d(TAG, "Server address:" + mServerIP);
                }

                final Parcelable[] rawUris = intent.getParcelableArrayExtra(HOTKNOT_EXTRA_BEAM_URIS);
                final int uriCount = rawUris != null ? rawUris.length : 0;

                if (uriCount == 0) {
                    Log.e(TAG, "No Uris");
                    return;
                }

                Uri[] uris = new Uri[uriCount];

                for (int i = 0; i < uriCount; i++) {
                    uris[i] = (Uri) rawUris[i];
                }

                msg.what = MSG_SENDER_REQ;
                msg.obj = (Object) uris;
                mHandler.sendMessage(msg);
            } else if (HOTKNOT_RECV.equals(action)) {
                mHandler.sendEmptyMessage(MSG_RECEIVER_REQ);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_NETWORK_INFO);
                mIsP2pConnected = networkInfo.isConnected();
                Log.d(TAG, "P2P connect staus " + mIsP2pConnected);
                if (mIsP2pConnected) {
                    Network network = getConnectivityManager().getNetworkForType(
                                        ConnectivityManager.TYPE_VPN);
                    if (network != null) {
                        try {
                            Log.d(TAG, "Confgiure VPN");
                            mNMService.allowProtect(Binder.getCallingUid());
                            NetworkUtils.bindProcessToNetwork(LOCAL_NETWORK_ID);
                            //99 means local netork for P2P.
                        } catch (RemoteException re) {
                            Log.e(TAG, "remote exception in nw API");
                        }
                    }
                }
            }
        }
    };

    // We can't do this once in the Tethering() constructor and cache the value, because the
    // CONNECTIVITY_SERVICE is registered only after the Tethering() constructor has completed.
    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private void bindNetwork() {
        ConnectivityManager cm = getConnectivityManager();
        if (!mIsP2pConnected) {
            boolean isWifiConnected = false;
            for (Network network : cm.getAllNetworks()) {
                NetworkInfo nwInfo = cm.getNetworkInfo(network);
                if (nwInfo != null
                    && nwInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (nwInfo.isConnected()) {
                        try {
                            Log.d(TAG, "Bind network with Wi-Fi:" + network.netId);
                            cm.bindProcessToNetwork(network);
                            isWifiConnected = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
            if (!isWifiConnected) { //This is AP0 interface.
                Log.d(TAG, "Bind network with AP0");
                NetworkUtils.bindProcessToNetwork(LOCAL_NETWORK_ID);
            }
        } else {
            cm.bindProcessToNetwork(null);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {

        if (msg.what != MSG_POLLING) {
            Log.d(TAG, "msg.what:" + msg.what);
        }

        switch (msg.what) {
            case MSG_SENDER_REQ: {
                Uri[] uris = (Uri[]) msg.obj;

                bindNetwork();

                if (uris != null) {
                    runUploadTask(uris);
                } else {
                    Log.e(TAG, "The uri array is null");
                }

                synchronized (mPollLock) {
                    if (!mHandler.hasMessages(MSG_POLLING)) {
                        mHandler.sendEmptyMessageDelayed(MSG_POLLING,
                                HotKnotBeamConstants.FILE_PROGRESS_POLL);
                    }
                }

                break;
            }
            case MSG_RECEIVER_REQ: {
                mIdleRxCounter = HotKnotBeamConstants.MAX_FIRST_WAIT_COUNTER;
                String deviceName = (String) msg.obj;
                if (deviceName ==  null) {
                    deviceName = HotKnotBeamConstants.DEFAULT_DEVICE_NAME;
                }
                bindNetwork();

                if (mHotKnotServer == null) {
                    mHotKnotServer = new HotKnotFileServer(SERVICE_PORT, this, deviceName);

                    mHotKnotServer.setHotKnotFileServerCb(new HotKnotFileServerCb() {
                        public void onHotKnotFileServerFinish(int status) {
                            Message msg = Message.obtain(mHandler, MSG_UPDATE_UI);
                            mHandler.sendMessageAtFrontOfQueue(msg);

                            synchronized (HotKnotBeamService.this) {
                                if (!mHandler.hasMessages(MSG_SERVER_END)) {
                                    mHandler.sendEmptyMessage(MSG_SERVER_END);
                                }
                            }
                            mIdleRxCounter = 0;
                        }

                        public void onUpdateNotification() {
                            Message msg = Message.obtain(mHandler, MSG_UPDATE_UI);
                            mHandler.sendMessageAtFrontOfQueue(msg);
                        }

                        public void onSetExternalPath(boolean isExternal) {
                            Log.i(TAG, "onSetExternalPath:" + isExternal);
                            sIsDlExternal = isExternal;
                        }

                        public void onStartUiActivity(int id) {
                            Message msg = Message.obtain(mHandler, MSG_START_UI_ACTIVITY);
                            msg.arg1 = id;
                            mHandler.sendMessageDelayed(msg,
                                        HotKnotBeamConstants.START_UI_DELAY
                                    );
                        }
                    });
                    mHotKnotServer.execute();
                }

                synchronized (mPollLock) {
                    if (!mHandler.hasMessages(MSG_POLLING)) {
                        mHandler.sendEmptyMessageDelayed(MSG_POLLING,
                                HotKnotBeamConstants.FILE_PROGRESS_POLL);
                    }
                }

                break;
            }
            case MSG_POLLING: {
                if (mIdleRxCounter >= 0) {
                    Log.d(TAG, "Polling msg:" + mIdleRxCounter);
                }

                synchronized (mPollLock) {
                    if (!mHandler.hasMessages(MSG_POLLING)) {
                        mHandler.sendEmptyMessageDelayed(MSG_POLLING,
                            HotKnotBeamConstants.FILE_PROGRESS_POLL);
                    }
                }

                updateClientNotification();

                if (!updateServerNotification()) {
                    mIdleRxCounter++;

                    if (mIdleRxCounter > HotKnotBeamConstants.MAX_IDLE_COUNTER) {
                        Log.d(TAG, "There is no incoming client; stop server");
                        updateServerNotification();

                        synchronized (HotKnotBeamService.this) {
                            if (!mHandler.hasMessages(MSG_SERVER_END)) {

                                if (mHotKnotServer != null) {
                                    mHotKnotServer.stop();
                                }

                                mHandler.sendEmptyMessage(MSG_SERVER_END);
                            }
                        }

                        return true;
                    }
                } else {
                    mIdleRxCounter = 0;
                }

                break;
            }
            case MSG_CANCEL_REQ: {
                int id = (int) msg.arg1;
                Log.d(TAG, "Cancel dowdload job:" + id);

                if (id != -1 && mHotKnotServer != null) {
                    mHotKnotServer.cancel(id);
                }

                break;
            }
            case MSG_CLIENT_CANCEL_REQ: {
                int id = (int) msg.arg1;
                Log.d(TAG, "Cancel upload job:" + id);

                synchronized (mUploadTaskList) {
                    if (mUploadTaskList.size() != 0) {
                        for (int i = 0; i < mUploadTaskList.size(); i++) {
                            FileUploadTask task = mUploadTaskList.get(i);

                            if (id == task.getTaskId()) {
                                task.cancel(true);
                                break;
                            }
                        }
                    }
                }

                break;
            }
            case MSG_CLIENT_DONE: {

                synchronized (mUploadTaskList) {
                    FileUploadTask task = (FileUploadTask) msg.obj;
                    mUploadTaskList.remove(task);

                    if (mUploadTaskList.size() == 0) {
                        mHandler.sendEmptyMessage(MSG_CLIENT_END);
                    }
                }

                break;
            }
            case MSG_CLIENT_END: {
                Log.d(TAG, "Finish client procedure");

                if (mHotKnotServer == null) {
                    sendGenericIntent(HOTKNOT_FINISH);

                    if (mHandler.hasMessages(MSG_POLLING)) {
                        mHandler.removeMessages(MSG_POLLING);
                    }
                }

                break;
            }
            case MSG_SERVER_END: {
                mHotKnotServer = null;

                synchronized (mUploadTaskList) {
                    boolean isClientStopped = (mUploadTaskList.size() == 0) ? true : false;

                    if (isClientStopped) {
                        sendGenericIntent(HOTKNOT_FINISH);

                        if (mHandler.hasMessages(MSG_POLLING)) {
                            mHandler.removeMessages(MSG_POLLING);
                        }
                    }
                }

                break;
            }
            case MSG_TEST_CLIENT_REQ: {
                Intent intent = new Intent(HOTKNOT_SEND);
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING |
                                Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                Uri[] uris = new Uri[1];
                String path = (String) msg.obj;

                if (path ==  null) {
                    uris = new Uri[4];
                    uris[0] = Uri.parse("file://mnt/sdcard/1.jpg?format=raw");
                    uris[1] = Uri.parse("file://mnt/sdcard/1.zip?format=raw");
                    uris[2] = Uri.parse("file://mnt/sdcard/1.3gp?format=raw");
                    uris[3] = Uri.parse("file://mnt/sdcard/1.mp3?format=raw");

                    Log.i(TAG, "configure zip uris");
                } else {
                    path = Uri.encode(path);
                    uris[0] = Uri.parse("file:/" + path);
                }

                intent.putExtra(HOTKNOT_EXTRA_BEAM_URIS, uris);
                final long ident = Binder.clearCallingIdentity();

                try {
                    mContext.sendBroadcast(intent);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }

                break;
            }
            case MSG_UPDATE_UI:
                updateServerNotification();
                break;
            case MSG_START_UI_ACTIVITY:
                MimeUtilsEx.startRxUiActivity(mContext, msg.arg1);
                break;
            default:

                break;
        }

        return true;
    }

    private void sendGenericIntent(String action) {
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING |
                        Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);

        final long ident = Binder.clearCallingIdentity();

        try {
            Log.i(TAG, "sendGenericIntent:" + action);
            // mContext.sendBroadcast(intent);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean updateServerNotification() {

        if (mHotKnotServer == null) {
            Log.e(TAG, "mHotKnotServer is null");
            return false;
        }

        Collection<DownloadInfo> c = mHotKnotServer.getDownloadInfos();

        if (c != null) {
            mDownloadNotifier.updateWith(c);
            return true;
        }

        return false;
    }

    private void cancelDownloadAction() {
        if (mHotKnotServer != null) {
            mHotKnotServer.resetNotificaiton();
        }
        updateServerNotification();
    }

    private void cancelUploadAction() {
        synchronized (mUploadTaskList) {
            for (int i = 0; i < mUploadTaskList.size(); i++) {
                FileUploadTask task = mUploadTaskList.get(i);
                task.resetNotificaiton();
            }
        }
        updateClientNotification();
        mUploadTaskList.clear();
    }

    private boolean updateClientNotification() {

        synchronized (mUploadTaskList) {
            for (int i = 0; i < mUploadTaskList.size(); i++) {
                FileUploadTask task = mUploadTaskList.get(i);
                Collection<UploadInfo> c = task.getUploadInfos();

                if (c != null) {
                    mUploadNotifier.updateWith(c);
                    return true;
                }
            }
        }

        return false;
    }

    private void runUploadTask(Uri[] uris) {
        FileUploadTask uploadTask = new FileUploadTask(mServerIP, SERVICE_PORT, this);

        synchronized (mUploadTaskList) {
            mUploadTaskList.add(uploadTask);
            Log.i(TAG, "The upload counter is " + mUploadTaskList.size());
        }

        uploadTask.setOnPostExecute(new FileUploadTaskListener() {

            public void onSetExternalPath(boolean isExternal) {
                Log.i(TAG, "onSetExternalPath:" + isExternal);
                sIsUlExternal = isExternal;
            }

            public void onPostExecute(Void result, FileUploadTask task) {
                Message msg = Message.obtain();
                msg.what = MSG_CLIENT_DONE;
                msg.obj = task;
                mHandler.sendMessage(msg);
                updateClientNotification();
            }
        });

        if (uris.length > 1) {
            String queryString = uris[0].getQuery();

            if (queryString != null && queryString.indexOf(HotKnotBeamConstants.QUERY_ZIP
                        + "=" + HotKnotBeamConstants.QUERY_VALUE_YES) != -1) {
                Uri uri = createZipFile(uris, uploadTask);

                if (uri != null) {
                    uploadTask.execute(uri);
                }

                return;
            }
        }

        uploadTask.execute(uris);
    }

    private Uri createZipFile(Uri[] uris, FileUploadTask uploadTask) {
        Uri uri = null;

        NotificationManager notifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(mContext);

        try {
            /*
            builder.setContentTitle("File is preparing").setContentText("" + uris[0])
                .setSmallIcon(R.drawable.stat_sys_upload).setProgress(0, 0, true);
            notifyManager.notify(0, builder.build());
            Log.i(TAG, "ZIP file is here");
            */
            String zipFilePath =  MimeUtilsEx.getSaveRootPath(mContext,
                        HotKnotBeamConstants.MAX_HOTKNOT_BEAM_TEMP_ZIP) + sFileCount;
            File tmpFile = new File(zipFilePath);
            sFileCount++;

            File baseFile = MimeUtilsEx.getFilePathFromUri(uris[0], mContext);
            String zipeName = ZipFileUtils.zipUris(uris, tmpFile, baseFile.getParentFile(), mContext);
            uploadTask.setUploadFileName(zipeName);
            uri = Uri.fromFile(tmpFile).buildUpon().appendQueryParameter(
                        HotKnotBeamConstants.QUERY_ZIP,
                        HotKnotBeamConstants.QUERY_VALUE_YES).build();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //notifyManager.cancel(0);
            Log.i(TAG, "File ZIP is done");
        }

        return uri;
    }

}