package com.mediatek.op.telephony;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.internal.telephony.CommandsInterface;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.SSLContext;

/**
 * The OPLMN updating controller.
 *
 */
public class OplmnUpdateCenter extends Handler {
    private static final String PROPERTY_OPLMN_UPDATE_URL =
            "https://roam.radiosky.com.cn/cdma/ud/index";
    private static final String PREF_NW_OPLMN_INFO = "mediatek_oplmn_info";
    private static final String KEY_OPLMN_UPDATE_TIME = "com.mediatek.oplmn.update.time";
    private static final String KEY_OPLMN_UPDATE_VERSION = "com.mediatek.oplmn.update.version";
    private static final String ACTION_OPLMN_UPDATE =
            "com.mediatek.intent.action.ACTION_OPLMN_UPDATE";

    private static final long UPDATE_INTERVAL = 10 * 24 * 60 * 60 * 1000;
    private static final long UPDATE_INTERVAL_DELAY = 60 * 1000;

    private static final int EVENT_SEND_OPLMN_LIST_DONE = 1;
    private static final int EVENT_RETRY_OPLMN_UPDATE = 2;

    private Context mContext;
    private CommandsInterface mCi;
    private URL mUrl;
    private SSLContext mSslContext;
    private String mOplmnInfo;
    private boolean mWaitingForNet;

    private SharedPreferences mSharedPreferences;

    private ConnectivityManager mConnectivityManager;

    private Runnable mOplmnUpdateRunnable = new Runnable() {
        public void run() {
            mSslContext = OplmnUpdateUtils.getSslContext();
            logd("Get the SslContext = " + mSslContext);
            if (mSslContext == null) {
                return;
            }
            OplmnUpdateUtils.OplmnInfo oplmnInfo = OplmnUpdateUtils.getOplmnInfo(mSslContext, mUrl);
            if (oplmnInfo != null) {
                String currentVersion = mSharedPreferences.getString(KEY_OPLMN_UPDATE_VERSION,
                        "0.0");
                logd("The current version of oplmn file:" + currentVersion);

                if (OplmnUpdateUtils.compareVersion(oplmnInfo.getVersion(), currentVersion) > 0) {
                    byte[] rawData = OplmnUpdateUtils.downloadOplmnFile(mSslContext, oplmnInfo);
                    if (rawData != null) {
                        // Update the last oplmn update time and the file version
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putString(KEY_OPLMN_UPDATE_TIME,
                                String.valueOf(System.currentTimeMillis()));
                        editor.putString(KEY_OPLMN_UPDATE_VERSION, oplmnInfo.getVersion());
                        editor.commit();
                        // Send the oplmn file to modem
                        sendOplmnFile(oplmnInfo.getVersion(), rawData);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            logd("received broadcast, action = " + action + " at time "
                    + DateFormat.format("yyyy-MM-dd, kk:mm:ss", System.currentTimeMillis()));
            final NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
            boolean connected = info != null && info.isConnected()
                    && info.getType() == ConnectivityManager.TYPE_MOBILE;

            if (ACTION_OPLMN_UPDATE.equals(action)) {
                logd("Update oplmn, data connected = " + connected);
                if (connected) {
                    new Thread(mOplmnUpdateRunnable).start();
                } else {
                    mWaitingForNet = true;
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (connected && mWaitingForNet) {
                    logd("Retry to update the oplmn file");
                    new Thread(mOplmnUpdateRunnable).start();
                    mWaitingForNet = false;
                }
            }
        }
    };

    /**
     * Start the OPLMN updating task.
     * @param context : The context object.
     * @param ci : The CommandsInterface object.
     */
    public void startOplmnUpdater(Context context, Object ci) {
        mContext = context;
        mCi = (CommandsInterface) ci;
        mCi.registerForAvailable(this, EVENT_RETRY_OPLMN_UPDATE, null);

        mSharedPreferences = mContext.getSharedPreferences(PREF_NW_OPLMN_INFO, 0);
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        try {
            mUrl = new URL(PROPERTY_OPLMN_UPDATE_URL);
        } catch (MalformedURLException e) {
            logd("Initialize the url fail:" + e.getMessage());
        }

        long nextUpdateTime = getNextUpdateTime();

        logd("nextUpdateTime = " + nextUpdateTime + ", "
                + DateFormat.format("yyyy-MM-dd, kk:mm:ss", nextUpdateTime));

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPLMN_UPDATE);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mReceiver, filter);

        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_OPLMN_UPDATE);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, nextUpdateTime, UPDATE_INTERVAL, sender);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SEND_OPLMN_LIST_DONE:
                logd("handle EVENT_SEND_OPLMN_LIST_DONE");
                AsyncResult ar;
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    logd("Send oplmn file fail");
                } else {
                    logd("Send oplmn file success, delete the oplmn file");
                    File oplmnFile = OplmnUpdateUtils.getOplmnFile();
                    if (oplmnFile != null) {
                        oplmnFile.delete();
                    }
                }
                break;
            case EVENT_RETRY_OPLMN_UPDATE:
                logd("handle EVENT_RETRY_OPLMN_UPDATE");
                if (mSslContext == null) {
                    return;
                }
                File oplmnFile = OplmnUpdateUtils.getOplmnFile();
                if (oplmnFile != null) {
                    byte[] cacheOplmn = OplmnUpdateUtils.readOplmnFile(oplmnFile);
                    if (cacheOplmn != null) {
                        String ver = mSharedPreferences.getString(KEY_OPLMN_UPDATE_VERSION, "0.0");
                        sendOplmnFile(ver, cacheOplmn);
                    }
                }
                break;
            default:
                break;
        }
    }

    private long getNextUpdateTime() {
        String time = mSharedPreferences.getString(KEY_OPLMN_UPDATE_TIME, "0");
        long lastTime = 0;
        if (!TextUtils.isEmpty(time)) {
            lastTime = Long.valueOf(time);
        }
        logd("lastUpdateTime = " + lastTime + ", "
                + (String) DateFormat.format("yyyy-MM-dd, kk:mm:ss", lastTime));

        if ((lastTime + UPDATE_INTERVAL) <= System.currentTimeMillis()) {
            return System.currentTimeMillis() + UPDATE_INTERVAL_DELAY;
        } else {
            return lastTime + UPDATE_INTERVAL;
        }
    }

    private void sendOplmnFile(String version, byte[] cacheOplmn) {
        mOplmnInfo = OplmnUpdateUtils.parseOplmnAsModemFormat(version, cacheOplmn);
        if (!TextUtils.isEmpty(mOplmnInfo)) {
            mCi.setOplmn(mOplmnInfo, obtainMessage(EVENT_SEND_OPLMN_LIST_DONE));
            OplmnUpdateUtils.respondResult(mContext, mSslContext, mUrl, version, true);
        }
    }

    private void logd(String msg) {
        Log.d("@M_" + OplmnUpdateUtils.LOG_TAG_PHONE, OplmnUpdateUtils.LOG_TAG + " " + msg);
    }
}