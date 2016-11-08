package com.mediatek.setting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.MmsApp;
import com.android.mms.util.MmsLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

/**
 * Used for setting activities, receive SIM state related intents, activities will re-fresh
 * when receive any one of these action:
 * TelephonyIntents.ACTION_SIM_STATE_CHANGED
 * TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED
 * Intent.ACTION_AIRPLANE_MODE_CHANGED
 * TelephonyIntents.ACTION_SERVICE_STATE_CHANGED
 * Telephony.Sms.Intents.SMS_STATE_CHANGED_ACTION
 */
public class SimStateMonitor extends BroadcastReceiver {

    private Set<SimStateListener> mListeners = new HashSet<SimStateMonitor.SimStateListener>();
    private static SimStateMonitor sSimStateMonitor = new SimStateMonitor();
    private List<SubscriptionInfo> mSubInfoList;
    private int mSubCount = 0;
    private boolean mIsAirPlaneMode = false;
    private static final String TAG = "SimStateMonitor";

    public static SimStateMonitor getInstance() {
        return sSimStateMonitor;
    }

    public interface SimStateListener {
        public void onSimStateChanged();
    }

    public SimStateMonitor() {
        refreshData();
    }

    public void addListener(SimStateListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(SimStateListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        MmsLog.d(TAG, "onReceive action : " + action);
        SimStateMonitor.getInstance().refreshData();
        for (SimStateListener listener : SimStateMonitor.getInstance().mListeners) {
            listener.onSimStateChanged();
        }
    }

    private void refreshData() {
        mSubInfoList = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfoList();
        if (mSubInfoList == null) {
            mSubInfoList = new CopyOnWriteArrayList();
        }
        mSubCount = mSubInfoList.size();
        mIsAirPlaneMode = Settings.System.getInt(MmsApp.getApplication().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    public List<SubscriptionInfo> getSubInfoList() {
        return mSubInfoList;
    }

    public int getSubCount() {
        return mSubCount;
    }

    public boolean getAirplaneMode() {
        return mIsAirPlaneMode;
    }

}
