package com.mediatek.nlpservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.os.IBinder;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mediatek.lbsutils.LbsUtils;

import static com.mediatek.nlpservice.DataCoder.getInt;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class NlpService extends Service {
    private static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);

    // Messages for internal handler
    public final static int NLPS_MSG_GPS_STARTED = 0;
    public final static int NLPS_MSG_GPS_STOPPED = 1;
    public final static int NLPS_MSG_GPS_NIJ_REQ = 2;
    public final static int NLPS_MSG_NLP_UPDATED = 3;

    // Commands from Server Instance Socket
    public final static int NLPS_CMD_QUIT = 100;
    public final static int NLPS_CMD_GPS_NIJ_REQ = 101;

    public final static int NLPS_MAX_CLIENTS = 2;

    public final static boolean NIJ_ON_GPS_START_DEFAULT = false;

    protected final static String SOCKET_ADDRESS = "com.mediatek.nlpservice.NlpService";

    private boolean mEnabled;
    private SharedPreferences mSettings;
    private LocationManager mLocationManager;
    private NlpsMsgHandler mHandler;
    private Thread mServerThread;
    private boolean mIsNlpRequested = false;
    private volatile boolean mIsStopping = false;
    private AtomicInteger mClientCount = new AtomicInteger();
    private LocalServerSocket mNlpServerSocket = null;
    private Context mContext;


    public NlpService() {
        if (DEBUG) log("[service] NlpService constructor");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) log("[service] onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) log("[service] onCreate");
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        HandlerThread handlerThread = new HandlerThread("[NlpService]");
        handlerThread.start();
        mHandler = new NlpsMsgHandler(handlerThread.getLooper());
        //log("[service] onCreate: handlerThread:" + handlerThread);
        //log("[service] onCreate: mHandler:" + mHandler);

        mLocationManager.addGpsStatusListener(mStatusListener);
        mServerThread = new Thread() {
            public void run() {
                log("[service] mServerThread.run()");
                doServerTask();
            }
        };
        //log("[service] onCreate: mServerThread:" + mServerThread);
        mServerThread.start();
        //log("[service] onCreate: mServerThread: start");

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mediatek.nlpservice.ENABLE");
        filter.addAction("com.mediatek.nlpservice.DISABLE");
        filter.addAction("com.mediatek.nlpservice.GET_STATUS");
        registerReceiver(receiver, filter);
        mSettings = getSharedPreferences("settings", 0);
        mEnabled = mSettings.getBoolean("enable", NIJ_ON_GPS_START_DEFAULT);

        mContext = getApplicationContext();
        LbsUtils lbsUtils = LbsUtils.getInstance(mContext);
        Resources resources = mContext.getResources();
        String[] gmsLpPkgs = resources.getStringArray(
                com.android.internal.R.array.config_locationProviderPackageNames);
        lbsUtils.listenPhoneState(gmsLpPkgs);

        log("[service] onCreate: mEnabled: " + mEnabled);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (DEBUG) log("[service] onStart");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) log("[service] onDestroy");
        mIsStopping = true;
        mLocationManager.removeGpsStatusListener(mStatusListener);
        closeServerSocket();
        releaseNlp();
        unregisterReceiver(receiver);
    }

    private boolean isNlpEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void startNlpQuery() {
        if (mIsNlpRequested == true) {
            return;
        }
        boolean isNlpEnabled = isNlpEnabled();
        log("[service] startNlpQuery isNlpEnabled=" + isNlpEnabled + " ver=1.00");
        if (isNlpEnabled) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 0,
                    mLocationListener);
            mIsNlpRequested = true;
        }
    }

    private void stopNlpQuery() {
        if (mIsNlpRequested == false) {
            return;
        }
        mLocationManager.removeUpdates(mLocationListener);
        mIsNlpRequested = false;
    }

    private GpsStatus.Listener mStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_STARTED) {
                if (mEnabled) {
                    //log("[service] GPS_EVENT_STARTED");
                    sendCommand(NLPS_MSG_GPS_STARTED);
                } else {
                    log("[service] GPS_EVENT_STARTED: ignored");
                }
            }
            if (event == GpsStatus.GPS_EVENT_STOPPED) {
                //log("[service] GPS_EVENT_STOPPED");
                sendCommand(NLPS_MSG_GPS_STOPPED);
            }
        }
    };

    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            //log("[service] onLocationChanged location=" + location);
            sendCommand(NLPS_MSG_NLP_UPDATED);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.mediatek.nlpservice.ENABLE")) {
                log("[service] ENABLE");
                mEnabled = true;
                mSettings.edit().putBoolean("enable", true).commit();
            } else if (action.equals("com.mediatek.nlpservice.DISABLE")) {
                log("[service] DISABLE");
                mEnabled = false;
                mSettings.edit().putBoolean("enable", false).commit();
            } else if (action.equals("com.mediatek.nlpservice.GET_STATUS")) {
                Intent in = new Intent("com.mediatek.nlpservice.UPDATE_STATUS");
                in.putExtra("enable", mEnabled);
                sendBroadcast(in);
            }
        }
    };

    public static void log(String msg) {
        Log.d("nlp_service", msg);
    }

    private static void close(LocalServerSocket lss) {
        try {
            lss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void close(LocalSocket ls) {
        try {
            ls.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void requestNlp() {
        try {
            startNlpQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void releaseNlp() {
        try {
            stopNlpQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeServerSocket() {
        if (mNlpServerSocket == null) {
            return;
        }
        close(mNlpServerSocket);
        mNlpServerSocket = null;
    }

    private void doServerTask() {
        try {
            if (DEBUG) log("NlpServerSocket+");
            synchronized(this) {
                mNlpServerSocket = new LocalServerSocket(SOCKET_ADDRESS);
                log("NlpServerSocket: " + mNlpServerSocket);
            }

            while (mIsStopping != true) {
                if (DEBUG) log("NlpServerSocket, wait client");
                LocalSocket instanceSocket = mNlpServerSocket.accept();
                if (DEBUG) log("NlpServerSocket, instance: " + instanceSocket);
                if (mIsStopping != true) {
                    if (mClientCount.get() < NLPS_MAX_CLIENTS) {
                        new ServerInstanceThread(instanceSocket).start();
                    } else {
                        log("no resource, client count: " + mClientCount.get());
                        close(instanceSocket);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeServerSocket();
        if (DEBUG) log("NlpServerSocket-");
    }

    private class ServerInstanceThread extends Thread {
        LocalSocket mSocket;

        public ServerInstanceThread(LocalSocket instanceSocket) {
            mSocket = instanceSocket;
            mClientCount.getAndIncrement();
            log("client count+: " + mClientCount.get());
        }

        public void run() {
            try {
                if (DEBUG) log("NlpInstanceSocket+");
                DataInputStream dins = new DataInputStream(mSocket.getInputStream());
                while (mIsStopping != true) {
                    int cmd = getInt(dins);
                    //<< For future use...
                    int data1 = getInt(dins);
                    int data2 = getInt(dins);
                    int data3 = getInt(dins);
                    //>>
                    //log("cmd=" + cmd + "," + Integer.toHexString(data1) + "," + Integer.toHexString(data2) + "," + Integer.toHexString(data3));
                    if (cmd == NLPS_CMD_GPS_NIJ_REQ) {
                        sendCommand(NLPS_MSG_GPS_NIJ_REQ);
                        log("ClientCmd: NLP_INJECT_REQ");
                    } else if (cmd == NLPS_CMD_QUIT) {
                        if (DEBUG) log("ClientCmd: QUIT");
                        break;
                    } else {
                        log("ClientCmd, unknown: " + cmd);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeInstanceSocket();
            if (DEBUG) log("NlpInstanceSocket-");
        }

        private void closeInstanceSocket() {
            close(mSocket);
            mSocket = null;
            mClientCount.getAndDecrement();
            log("client count-: " + mClientCount.get());
        }
    }

    private void sendCommand(int cmd) {
        Message msg = Message.obtain();
        msg.what = cmd;
        mHandler.sendMessage(msg);
    }

    private class NlpsMsgHandler extends Handler {
        public NlpsMsgHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NLPS_MSG_GPS_STARTED:
                    if (DEBUG) log("NLPS_MSG_GPS_STARTED");
                    requestNlp();
                    break;
                case NLPS_MSG_GPS_STOPPED:
                    if (DEBUG) log("NLPS_MSG_GPS_STOPPED");
                    releaseNlp();
                    break;
                case NLPS_MSG_GPS_NIJ_REQ:
                    if (DEBUG) log("NLPS_MSG_GPS_NIJ_REQ");
                    requestNlp();
                    break;
                case NLPS_MSG_NLP_UPDATED:
                    if (DEBUG) log("NLPS_MSG_NLP_UPDATED");
                    releaseNlp();
                    break;
                default:
                    log("Undefined message");
            }
        }
    }
}
