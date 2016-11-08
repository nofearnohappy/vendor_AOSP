/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mediatek.gba;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.util.Log;



import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.gba.cache.GbaKeysCache;
import com.mediatek.gba.element.NafId;
import com.mediatek.gba.telephony.TelephonyUtils;

/**
 * implementation for GbaService.
 *
 * @hide
 */
public class GbaService extends Service {
    public static final String TAG = "GbaService";

    private Context mContext;
    private GbaKeysCache mGbaKeysCache = null;
    private int mGbaType = GbaConstant.GBA_NONE;

    private static final int EVENT_SIM_STATE_CHANGED = 0;
    private static final String ACTION_SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";

    @Override
    public void onCreate() {
        super.onCreate();

        ServiceManager.addService(TAG, mBinder);

        mContext = this.getBaseContext();

        if (mGbaKeysCache == null) {
            mGbaKeysCache = new GbaKeysCache();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SHUTDOWN_IPO);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(ACTION_SHUTDOWN_IPO)) {
                    Log.d(TAG, "ACTION_SHUTDOWN_IPO received");
                    mGbaKeysCache = new GbaKeysCache();
                }
            }
        };
        mContext.registerReceiver(receiver, intentFilter);

/*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                    String iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    Log.d(TAG, "iccState:" + iccState);

                    if (!iccState.equals(IccCardConstants.INTENT_VALUE_ICC_LOADED)) {
                        return;
                    }

                    mHandler.sendEmptyMessage(EVENT_SIM_STATE_CHANGED);
                }
            }
        };
        mContext.registerReceiver(receiver, intentFilter);
*/

        Log.d(TAG, "Add service for GbaService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_STICKY;
        }

        String action = intent.getAction();
        Log.d(TAG, "Service starting for intent " + action);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;    // clients can't bind to this service
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service is onDestroy");
    }


    private IBinder mBinder = new IGbaService.Stub() {

        public int getGbaSupported() {
            return TelephonyUtils.getGbaSupported(mContext, SubscriptionManager.getDefaultSubId());
        }

        public int getGbaSupportedForSubscriber(int subId) {
            return TelephonyUtils.getGbaSupported(mContext, subId);
        }


        public boolean isGbaKeyExpired(String nafFqdn, byte[] nafSecurProtocolId) {
            boolean bIsKeyExpired = true;

            return isGbaKeyExpiredForSubscriber(nafFqdn, nafSecurProtocolId,
                SubscriptionManager.getDefaultSubId());
        }

        public boolean isGbaKeyExpiredForSubscriber(
            String nafFqdn, byte[] nafSecurProtocolId, int subId) {
            boolean bIsKeyExpired = true;

            NafId nafId = NafId.createFromNafFqdnAndProtocolId(nafFqdn, nafSecurProtocolId);
            bIsKeyExpired = mGbaKeysCache.isExpiredKey(nafId, subId);

            return bIsKeyExpired;
        }

        public void setNetwork(Network network) {
            if (network != null) {
                ConnectivityManager connMgr =
                        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

                connMgr.setProcessDefaultNetwork(network);
            }
        }

        public NafSessionKey runGbaAuthentication(String nafFqdn, byte[] nafSecurProtocolId,
                boolean forceRun) {
            return runGbaAuthenticationForSubscriber(nafFqdn, nafSecurProtocolId, forceRun,
                   SubscriptionManager.getDefaultSubId());
        }

        public NafSessionKey runGbaAuthenticationForSubscriber(
            String nafFqdn, byte[] nafSecurProtocolId, boolean forceRun, int subId) {
            GbaDebugParam gbaDebugParam = GbaDebugParam.getInstance();
            gbaDebugParam.load();
            Log.d(TAG, "Gba debug params: \n" + gbaDebugParam.toString());

            NafSessionKey nafSessionKey = null;
            NafId nafId = NafId.createFromNafFqdnAndProtocolId(nafFqdn, nafSecurProtocolId);
            boolean isExpiredKey = mGbaKeysCache.isExpiredKey(nafId, subId);

            boolean needForce = gbaDebugParam.getEnableGbaForceRun();

            if (needForce || TelephonyUtils.getTestSIM(subId) == 1) {
                Log.i(TAG, "Need force");
                forceRun = true;
            }

            if (!isExpiredKey && !forceRun) {
                nafSessionKey = mGbaKeysCache.getKeys(nafId, subId);
            } else {
                int gbaType = TelephonyUtils.getGbaSupported(mContext, subId);
                GbaBsfProcedure gbaProcedure = new GbaBsfProcedure(gbaType, subId, mContext);
                nafSessionKey = gbaProcedure.perform(nafId);
                if (nafSessionKey != null) {
                    Log.i(TAG, "nafSessionKey:" + nafSessionKey);
                    mGbaKeysCache.putKeys(nafId, subId, nafSessionKey);
                }
            }

            ConnectivityManager connMgr =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            connMgr.setProcessDefaultNetwork(null);
            return nafSessionKey;
        }


    };

/*
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SIM_STATE_CHANGED:

                    break;
                default:
                    break;
            }
        }
    };
*/
}
