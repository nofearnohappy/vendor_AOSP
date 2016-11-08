/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.providers.drm;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import com.mediatek.drm.OmaDrmClient;

/**
 * Using service to sync time, and start with foreground to avoid low memory kill.
 * 1. sync secure timer
 * 2. update time offset
 */
public class DrmSyncTimeService extends Service {
    private static final String TAG = "DRM/DrmSyncTimeService";
    // Message which handle by DrmSyncTimeHandler
    private static final int MSG_SYNC_SECURE_TIMER = 1;
    private static final int MSG_UPDATE_TIME_OFFSET = 2;
    // Timeout used to call ConnectivityManager.requestNetwork
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 3 * 1000;
    private DrmSyncTimeHandler mDrmSyncTimeHandler = null;
    private Context mContext;

    // Sync with these SNTP host servers for different countries.
    private static String[] sHostList = new String[] {
        "hshh.org",
        "t1.hshh.org",
        "t2.hshh.org",
        "t3.hshh.org",
        "clock.via.net",
        "pool.ntp.org",
        "asia.pool.ntp.org",
        "europe.pool.ntp.org",
        "north-america.pool.ntp.org",
        "oceania.pool.ntp.org",
        "south-america.pool.ntp.org"
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mContext = getApplicationContext();
        // Set as foreground process to avoid LMK when sync secure timer
        // use 0 does not show notification
        Notification notification = new Notification.Builder(mContext).build();
        startForeground(0, notification);

        // start sub thread to access network
        HandlerThread handlerThread = new HandlerThread("DrmSyncTimeThread");
        handlerThread.start();
        mDrmSyncTimeHandler = new DrmSyncTimeHandler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: intent = " + intent + ", startId = " + startId);
        // Intent is null, return not stick to avoid service restart again.
        if (intent == null) {
            // Just stop start id, because there may be some new start command coming when we
            // call stopself() to stop current service.
            stopSelf(startId);
            Log.d(TAG, "onStartCommand with null intent, stop service and avoid restart again");
            return START_NOT_STICKY;
        }
        String action = intent.getStringExtra("action");
        int what = -1;
        if ("sync_secure_timer".equals(action)) {
            what = MSG_SYNC_SECURE_TIMER;
        } else if ("update_time_offset".equals(action)) {
            what = MSG_UPDATE_TIME_OFFSET;
        }
        mDrmSyncTimeHandler.removeMessages(what);
        Message msg = mDrmSyncTimeHandler.obtainMessage(what, startId, -1);
        msg.sendToTarget();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        // Set process as background and stop handler thread
        stopForeground(true);
        mDrmSyncTimeHandler.getLooper().quit();
        mDrmSyncTimeHandler = null;
        mContext = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Use handler thread to access network to avoid blocking main thread.
     */
    private class DrmSyncTimeHandler extends Handler {

        DrmSyncTimeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SYNC_SECURE_TIMER:
                    syncSecureTimer();
                    break;

                case MSG_UPDATE_TIME_OFFSET:
                    updateTimeOffset();
                    break;

                default:
                    break;
            }
            // stop with start id of you self, so that service will be stopped only when
            // all job finish
            Log.d(TAG, "Stop DrmSyncTimeService with startId = " + msg.arg1);
            stopSelf(msg.arg1);
        }
    }

    private void syncSecureTimer() {
        // If test on CT case, disable to sync secure timer to avoid send ntp package
        if (OmaDrmHelper.isRequestDisableSyncSecureTimer()) {
            Log.d(TAG, "workaround for OP case, disable sync secure timer");
            return;
        }
        // Request network to check which host server available, if request failed,
        // stop sync secure timer.
        NetworkRequest request = new NetworkRequest.Builder().addCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback();
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        connManager.requestNetwork(request, callback, NETWORK_REQUEST_TIMEOUT_MILLIS);

        // Update device id at first, so that OMA DRM can work normal
        OmaDrmClient client = new OmaDrmClient(mContext);
        OmaDrmHelper.updateDeviceId(mContext, client);
        for (String serverName: sHostList) {
            // 1. If current server not available, check next server
            if (!isServerAvailable(serverName)) {
                Log.d(TAG, "syncSecureTimer with server not available for " + serverName);
                continue;
            }

            // 2. If sync offset is invalid, check next server
            int offset = Ntp.sync(serverName);
            if (Ntp.INVALID_OFFSET == offset) {
                Log.d(TAG, "syncSecureTimer invalid offset with host server " + serverName);
                continue;
            }

            // 3. If secure timer is not valid after update clock with offset, check next server
            OmaDrmHelper.updateClock(client, offset);
            String secureTimerSec = OmaDrmHelper.getSecureTimeInSeconds(client);
            if ("invalid".equals(secureTimerSec) || "".equals(secureTimerSec)
                    || "invalid-need-synchronization".equals(secureTimerSec)) {
                Log.d(TAG, "syncSecureTimer invalid time with host server " + serverName);
                continue;
            }

            // 4. If phone time is not synchronized with server, reset secure timer
            // and sync with next server
            int sendTimeSec = (int) (Ntp.getSentTime() / 1000);
            if (Math.abs(sendTimeSec + offset - Integer.valueOf(secureTimerSec)) > 60) {
                Log.d(TAG, "syncSecureTimer with time not sync between phone and host server "
                        + serverName);
                OmaDrmHelper.updateClock(client, Ntp.INVALID_OFFSET);
                continue;
            }
            // Success sync secure timer, break loop
            Log.d(TAG, "syncSecureTimer success with host server " + serverName);
            break;

        }
        // release OmaDrmClient if not use it again
        client.release();
        // Release network after finish sync
        connManager.unregisterNetworkCallback(callback);
    }

    private boolean isServerAvailable(String serverName) {
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(serverName);
        } catch (UnknownHostException e) {
            Log.w(TAG, "isServerAvailable with " + e);
        }
        Log.d(TAG, "isServerAvailable: serverName = " + serverName + ", addr = " + addr);
        return addr != null;
    }

    private void updateTimeOffset() {
        OmaDrmClient client = new OmaDrmClient(mContext);
        OmaDrmHelper.updateOffset(client);
        client.release();
        client = null;
    }
}
