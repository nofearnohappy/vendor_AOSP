/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.bootcheck;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmService;
import com.mediatek.dm.conn.DmDataConnection;
import com.mediatek.dm.data.IDmPersistentValues;
import com.mediatek.dm.data.PersistentContext;
import com.mediatek.dm.fumo.DmReport;
import com.mediatek.dm.option.Options;
import com.mediatek.dm.polling.PollingScheduler;
import com.mediatek.dm.util.FileLogger;

import java.util.Timer;
import java.util.TimerTask;

public class BootService extends Service {
    private static final String CLASS_TAG = "DM/BootService";

    private static final String LOCK_NAME_STATIC = "com.mediatek.dm.boot_service";
    private static final int MSG_TIMER_CONN = 0;
    private static final int MSG_TIMER_POLLING = 1;
    private static volatile PowerManager.WakeLock sLock;

    private boolean mTriggered;

    private boolean mPolling;
    private boolean mUpdate;
    private boolean mNia;

    private Timer mConnTimer;
    private Timer mPollingTimer;

    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
        BootService.this.registerReceiver(mTimeChangeReceiver, filter);
        Log.d(CLASS_TAG, "==[onCreate] TimeChange Receiver registered.==");

    }

    public void onDestroy() {
        Log.d(CLASS_TAG, "[onDestroy]");

        unregisterTimeChangeReceiver();

        if (mConnTimer != null) {
            mConnTimer.cancel();
        }
        if (mPollingTimer != null) {
            mPollingTimer.cancel();
        }

        /// M: Unregister
        DmDataConnection.getInstance(BootService.this).setUserHandler(null);

        super.onDestroy();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(CLASS_TAG, "service onStartCommand()");
        getLock(getApplicationContext()).acquire();
        FileLogger.getInstance(this).logMsg("+++wake lock acquired.+++");

        if (intent == null) {
            Log.w(CLASS_TAG, "---[onStartCommand]intent is null.---");
            mUpdate = BootReceiver.CheckReboot.checkUpdateStatus(this);
            mNia = BootReceiver.CheckReboot.checkUnproceedNia();
            mPolling = BootReceiver.CheckReboot.checkPolling(this);

        } else {
            Bundle data = intent.getExtras();
            mUpdate = data.getBoolean(DmService.INTENT_EXTRA_UPDATE, false);
            mNia = data.getBoolean(DmService.INTENT_EXTRA_NIA, false);
            mPolling = data.getBoolean(DmService.INTENT_EXTRA_POLLING, false);

            PersistentContext pc = PersistentContext.getInstance(this);
            if (IDmPersistentValues.STATE_UPDATE_RUNNING == pc.getDLSessionStatus()) {

                Log.v(CLASS_TAG, "---[onStartCommand]STATE_UPDATE_RUNNING---");
                boolean isUpdateSuccessfull = DmService.checkUpdateResult();
                Intent activityIntent = new Intent(this, DmReport.class);
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityIntent.putExtra(DmReport.EXTRA_RESULT, isUpdateSuccessfull);
                startActivity(activityIntent);

                Log.v(CLASS_TAG, "---[onStartCommand]set to STATE_UPDATE_COMPLETE---");
                pc.setDLSessionStatus(IDmPersistentValues.STATE_UPDATE_COMPLETE);
            }
        }

        try {
            FileLogger.getInstance(this).logMsg("====alarming.===");

            Log.d(CLASS_TAG, "[onStartCommand]mUpdate || mNia || mPolling = " + mUpdate + " || "
                    + mNia + " || " + mPolling);

            if (mUpdate || mNia || mPolling) {
                startConnTimer();
            } else {
                stopSelf();
            }

        } finally {
            getLock(getApplicationContext()).release();
            FileLogger.getInstance(this).logMsg("---wake lock released.---");
            FileLogger.getInstance(this).logMsg(
                    "wake lock isHeld=" + getLock(this.getApplicationContext()).isHeld());
            Log.d(CLASS_TAG, "wake lock released");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void triggerDmService() {
        Log.d(CLASS_TAG, "== [triggerDmService] begin ==");
        if (mUpdate || mNia) {
            triggerNiaAndUpdate();

            if (mPolling) {
                startPollingTimer();
            } else {
                stopSelf();
            }

        } else {
            if (mPolling) {
                triggerPolling();
            }
            stopSelf();
        }
    }

    private void triggerNiaAndUpdate() {
        Log.d(CLASS_TAG, "[[[[ trigger Nia&Update ]]]]");
        Bundle bundle = new Bundle();
        bundle.putBoolean(DmService.INTENT_EXTRA_UPDATE, mUpdate);
        bundle.putBoolean(DmService.INTENT_EXTRA_NIA, mNia);

        Intent intent = new Intent();
        intent.setAction(DmConst.IntentAction.ACTION_REBOOT_CHECK);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    private void triggerPolling() {
        Log.d(CLASS_TAG, "[[[[ trigger FUMO polling ]]]]");
        FileLogger.getInstance(this).logMsg("[[[[ trigger FUMO polling ]]]]");
        Intent fumoIntent = new Intent();
        fumoIntent.setAction(DmConst.IntentAction.ACTION_FUMO_CI);

        sendBroadcast(fumoIntent);

        // set next polling alarm
        PollingScheduler.getInstance(this).setNextAlarm();
    }

    private void startConnTimer() {
        Log.d(CLASS_TAG, "[startConnTimer] start");

        mConnTimer = new Timer(false);
        mConnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(CLASS_TAG, "[startConnTimer] send message MSG_TIMER_CONN");
                mHandler.sendEmptyMessage(MSG_TIMER_CONN);
            }
        }, 30000);

    }

    private void startPollingTimer() {
        Log.d(CLASS_TAG, "[startPollingTimer] start");

        mPollingTimer = new Timer(false);
        mPollingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(CLASS_TAG, "[startPollingTimer] send message MSG_TIMER_POLLING");
                mHandler.sendEmptyMessage(MSG_TIMER_POLLING);
            }
        }, 60000);
    }

    private void unregisterTimeChangeReceiver() {
        if (mTimeChangeReceiver != null) {
            this.unregisterReceiver(mTimeChangeReceiver);
            Log.d(CLASS_TAG, "--------TimeChange receiver unregistered.--------");
        }
    }

    private synchronized boolean aquireTrigger() {
        if (!mTriggered) {
            mTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    private static synchronized PowerManager.WakeLock getLock(Context context) {
        if (sLock == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            sLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);

            // reference counted Wake lock, to supply multi-requests.
            sLock.setReferenceCounted(true);
        }

        return (sLock);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS:
            /// M: Fall through
            case MSG_TIMER_CONN:

                Log.d(CLASS_TAG, "== [MSG_TIMER_CONN]reset mConnTimer ==");
                if (mConnTimer != null) {
                    mConnTimer.cancel();
                    mConnTimer = null;
                }

                Log.d(CLASS_TAG, "==[MSG_TIMER_CONN]check connetivity.==");
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connMgr != null) {
                    NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
                    if (netInfo != null && netInfo.isConnected()) {
                        Log.d(CLASS_TAG, "==[MSG_TIMER_CONN] connetivity ok, trigger DmService.==");

                        /// M: Unregister
                        DmDataConnection.getInstance(BootService.this).setUserHandler(null);

                        triggerDmService();

                        return;
                    }
                }

                // otherwise, wait for network ready
                FileLogger.getInstance(BootService.this).logMsg(">>wait for network ready...<<");
                mTriggered = false;

                /// M: Register handler in DmDataConnection, so this handler will be notified when network is OK .
                DmDataConnection dmConnection = DmDataConnection.getInstance(BootService.this);
                dmConnection.setUserHandler(this);
                if (!Options.USE_DIRECT_INTERNET) {
                    dmConnection.startDmDataConnectivity();
                }

                break;

            case MSG_TIMER_POLLING:
                Log.d(CLASS_TAG, "== [MSG_TIMER_CONN]reset mPollingTimer ==");
                mPollingTimer.cancel();
                mPollingTimer = null;

                Log.d(CLASS_TAG, "== [MSG_TIMER_CONN]start Service to trigger Polling ==");
                Intent intent = new Intent(BootService.this, BootService.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean(DmService.INTENT_EXTRA_POLLING, true);
                intent.putExtras(bundle);
                BootService.this.startService(intent);
                Log.d(CLASS_TAG, "== [MSG_TIMER_CONN]cService started.==");

                break;
            default:
                break;
            }
            super.handleMessage(msg);
        }
    };

    private BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(CLASS_TAG, "[TimeChangeReceiver] onReceive");

            if (context == null || intent == null) {
                return;
            }

            String action = intent.getAction();

            if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                Log.d(CLASS_TAG, "[TimeChangeReceiver] ACTION_TIME_CHANGED");
                if (mConnTimer != null) {
                    Log.d(CLASS_TAG, "[TimeChangeReceiver] restart mConnTimer");
                    mConnTimer.cancel();
                    startConnTimer();
                }

                if (mPollingTimer != null) {
                    Log.d(CLASS_TAG, "[TimeChangeReceiver] restart mPollingTimer");
                    mPollingTimer.cancel();
                    startPollingTimer();
                }
            }
        }

    };

}
