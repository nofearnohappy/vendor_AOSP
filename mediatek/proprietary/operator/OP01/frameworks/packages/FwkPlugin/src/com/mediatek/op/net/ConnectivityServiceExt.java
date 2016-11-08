
package com.mediatek.common.op.net;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.net.ConnectivityManager;
import android.net.NetworkUtils;
import android.net.wifi.IWifiManager;

import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;

import android.provider.Settings;

import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.os.SystemProperties;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.wifi.IWifiFwkExt;
import com.mediatek.op.net.DefaultConnectivityServiceExt;

import java.util.List;

/**
 * Interface that defines all methos which are implemented in ConnectivityService
 */

 /** {@hide} */
@PluginImpl(interfaceName="com.mediatek.common.net.IConnectivityServiceExt")
public class ConnectivityServiceExt extends DefaultConnectivityServiceExt
{
    private static final String MTK_TAG =
        "CDS/ConnectivityServiceExt";

    private BroadcastReceiver mReceiver;
    private Object mSynchronizedObject;
    private static final int WIFI_CONNECT_REMINDER_ALWAYS = 0;
    private static final String ACTION_PS_STATE_RESUMED =
        "com.mtk.ACTION_PS_STATE_RESUMED";
    private static final String ACTION_CMCC_MUSIC_RETRY =
        "android.intent.action.EMMRRS_PS_RESUME";
    //[RB release workaround]
    public static final String ACTION_SET_PACKETS_FLUSH =
        "com.android.internal.ACTION_SET_PACKETS_FLUSH";
    //[RB release workaround]
    public static final String ACTION_SUBLIST_UPDATED =
        TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED;

    private long[] mSubIdList = null;
    private DataStateListener[] mDataStateListeners = null;
    private int mPsNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private Context mContext;

    private boolean mIsRebooting = false;
    private static final String ACTION_SHUTDOWN_IPO =
        "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final String ACTION_PREBOOT_IPO =
        "android.intent.action.ACTION_PREBOOT_IPO";

    public void init(Context context) {
        mContext = context;

        IntentFilter filter =
            new IntentFilter(ACTION_PS_STATE_RESUMED);
        filter.addAction(ACTION_CMCC_MUSIC_RETRY);
        //[RB release workaround]
        filter.addAction(ACTION_SET_PACKETS_FLUSH);
        //[RB release workaround]

        filter.addAction(ACTION_SHUTDOWN_IPO);
        filter.addAction(ACTION_PREBOOT_IPO);

        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);

        mReceiver = new ConnectivityServiceReceiver();
        Intent intent = mContext.registerReceiver(mReceiver, filter);

        mSynchronizedObject = new Object();

        Log.d("@M_" + MTK_TAG, "Init done in ConnectivityServiceExt");
    }

    private void turnOffDataConnection() {
        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (telMgr == null) {
            return;
        }

        // Remember last status on(1) or off(-1)
        Settings.System.putLong(mContext.getContentResolver(),
            Settings.System.LAST_SIMID_BEFORE_WIFI_DISCONNECTED,
            telMgr.getDataEnabled() ? 1 : -1);

        telMgr.setDataEnabled(false);
    }

    public void UserPrompt() {
        boolean skipDataDialog = SystemProperties.get("ro.op01_compatible").equals("1");
        if (skipDataDialog) {
            Log.d("@M_" + MTK_TAG, "skip DataDialog, no datadialog");
            return;
        }

        int isAsking = Settings.System.getInt(mContext.getContentResolver(), 
                                Settings.System.WIFI_CONNECT_REMINDER,
                                WIFI_CONNECT_REMINDER_ALWAYS);
        if (isAsking != WIFI_CONNECT_REMINDER_ALWAYS) {
            // not asking mode
            Log.d("@M_" + MTK_TAG, "Not ask mode");
            return;
        }

        if (mIsRebooting) {
            Log.d("@M_" + MTK_TAG, "IPO rebooting, skip datadialog");
            return;
        }

        boolean dataAvailable = isPsDataAvailable();
        Log.d("@M_" + MTK_TAG, "dataAvailable: " + dataAvailable);
        if (!dataAvailable) {
            return;
        }

        // Close data connection switch here.
        turnOffDataConnection();

        IBinder binder = ServiceManager.getService(Context.WIFI_SERVICE);
        final IWifiManager wifiService = IWifiManager.Stub.asInterface(binder);
        boolean hasConnectableAP = false;
        try {
            if (wifiService != null) {
                hasConnectableAP = wifiService.hasConnectableAp();
            }
        } catch (RemoteException e) {
            Log.d("@M_" + MTK_TAG, "hasConnectableAp failed!");
        }
        Log.d("@M_" + MTK_TAG, "hasConnectableAP: " + hasConnectableAP);

        if (!hasConnectableAP) {
            // Show the Data Dialog here
            Intent i = new Intent(IWifiFwkExt.ACTION_WIFI_FAILOVER_GPRS_DIALOG);
            i.putExtra("simId", 1);
            mContext.sendBroadcast(i);
            Log.d("@M_" + MTK_TAG, "Send ACTION_WIFI_FAILOVER_GPRS_DIALOG intent");
        }

        // WIFI module will setup DIALOG later
    }

    public boolean isControlBySetting(int netType, int radio) {
        Log.d("@M_" + MTK_TAG, "isControlBySetting: netType=" + netType + " readio=" + radio);
        if (radio == ConnectivityManager.TYPE_MOBILE
             && (netType != ConnectivityManager.TYPE_MOBILE_MMS)) {
             return true;
        }

        return false;
    }

    private boolean isPsDataAvailable() {

        // Check SIM ready
        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(
            Context.TELEPHONY_SERVICE);
        if (telMgr == null) {
            Log.v("@M_" + MTK_TAG, "TelephonyManager is null");
            return false;
        }

        boolean isSIMReady = false;
        int i = 0;
        int n = telMgr.getSimCount();
        for (i = 0; i < n; i++) {
            if (telMgr.getSimState(i) == TelephonyManager.SIM_STATE_READY) {
                isSIMReady = true;
                break;
            }
        }

        Log.v("@M_" + MTK_TAG, "isSIMReady: " + isSIMReady);
        if (!isSIMReady) {
            return false;
        }

        // check radio on
        ITelephony iTel = ITelephony.Stub.asInterface(
            ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.v("@M_" + MTK_TAG, "ITelephony is null");
            return false;
        }

        SubscriptionManager subMgr = SubscriptionManager.from(mContext);
        if (subMgr == null) {
            Log.v("@M_" + MTK_TAG, "SubscriptionManager is null");
            return false;
        }
                
        int[] subIdList = subMgr.getActiveSubscriptionIdList();
        n = 0;
        if (subIdList != null) {
            n = subIdList.length;
        }

        boolean isRadioOn = false;
        for (i = 0; i < n; i++) {
            try {
                isRadioOn = iTel.isRadioOnForSubscriber(
                    subIdList[i],
                    mContext.getPackageName());
                if (isRadioOn) {
                    break;
                }
            } catch (RemoteException e) {
                Log.v("@M_" + MTK_TAG, "isRadioOnForSubscriber RemoteException");
                isRadioOn = false;
            }
        }
        if (!isRadioOn) {
            Log.v("@M_" + MTK_TAG, "All sub Radio OFF");
            return false;
        }

        // Check flight mode
        int airplanMode = Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0);
        Log.v("@M_" + MTK_TAG, "airplanMode:" + airplanMode);
        if (airplanMode == 1) {
            return false;
        }

        return true;
    }

    //[RB release workaround]
    private void retryMms() {
        Log.d("@M_" + MTK_TAG, "retryMms()");

        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
        if (processInfoList == null) {
            return;
        }

        for (RunningAppProcessInfo info : processInfoList) {
            if (info.processName.equals("com.android.mms")) {
                Log.d("@M_" + MTK_TAG, "retryMms(),info.uid:" + info.uid);
                //NetworkUtils.resetConnectionByUidErrNum(info.uid, 0);

                String rbReleaseSetting = android.os.SystemProperties.get("debug.rb.release", "true");
                if (rbReleaseSetting == null || "true".equals(rbReleaseSetting)) {
                    Log.d("@M_" + MTK_TAG, "retryMms(),info.uid:" + info.uid +
                           ",rbReleaseSetting:" + rbReleaseSetting);
                    NetworkUtils.resetConnectionByUidErrNum(info.uid, 0);
                }
                break;
            }
        }
    }
    //[RB release workaround]

    private class ConnectivityServiceReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            Log.d("@M_" + MTK_TAG, "received intent ==> " + action);

            synchronized (mSynchronizedObject) {
              if (ACTION_SET_PACKETS_FLUSH.equals(action)) {
                  //[RB release workaround]
                  retryMms();
              } else if (ACTION_SHUTDOWN_IPO.equals(action)) {
                  mIsRebooting = true;
              } else if (ACTION_PREBOOT_IPO.equals(action)) {
                  mIsRebooting = false;
              } else if (ACTION_SUBLIST_UPDATED.equals(action)) {
                  onSubInfoUpdated();
              }
            }
        }
    }

    private void onSubInfoUpdated() {
        int i = 0;

        TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);

        // un-register all sub
        if (mDataStateListeners != null) {
            for (i = 0; i < mDataStateListeners.length; i++) {
                mDataStateListeners[i].setSubId(
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                telMgr.listen(mDataStateListeners[i],
                    PhoneStateListener.LISTEN_NONE);
            }
        }
        mDataStateListeners = null;

        SubscriptionManager subMgr = SubscriptionManager.from(mContext);
        if (subMgr == null) {
            Log.v("@M_" + MTK_TAG, "SubscriptionManager is null");
            return;
        }

        int[] subIdList = subMgr.getActiveSubscriptionIdList();
        int count = 0;

        if (subIdList != null) {
            count = subIdList.length;
        }

        // register all inserted sub
        if (count > 0) {
            mDataStateListeners = new DataStateListener[count];
            for (i = 0; i < subIdList.length; i++) {
                mDataStateListeners[i] = new DataStateListener(subIdList[i]);
                telMgr.listen(mDataStateListeners[i],
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            }
        }
    }

    private class DataStateListener extends PhoneStateListener {
        protected int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public DataStateListener(int subId) {
            super(subId);
            mSubId = subId;
        }

        public void setSubId(int subId) {
            this.mSubId = subId;
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.d("@M_" + MTK_TAG, "data state:" + state + ":"
                    + " nw type:" + networkType + "/" + mPsNetworkType
                    + " subId:" + mSubId);

            if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return;
            }

            // Only handle the active data connection
            if (state == TelephonyManager.DATA_CONNECTED) {
               if ((mPsNetworkType > TelephonyManager.NETWORK_TYPE_EDGE
                        && networkType < TelephonyManager.NETWORK_TYPE_UMTS
                        && networkType > TelephonyManager.NETWORK_TYPE_UNKNOWN)
                    ||
                   (mPsNetworkType != TelephonyManager.NETWORK_TYPE_UNKNOWN
                        && mPsNetworkType < TelephonyManager.NETWORK_TYPE_UMTS
                        && networkType > TelephonyManager.NETWORK_TYPE_EDGE)) {

                    Log.d("@M_" + MTK_TAG, "Send ps resumed from connectivityservice");
                    Intent intent = new Intent(ACTION_PS_STATE_RESUMED);
                    mContext.sendBroadcast(intent);
               }
               mPsNetworkType = networkType;
            }
        }
    }
}
