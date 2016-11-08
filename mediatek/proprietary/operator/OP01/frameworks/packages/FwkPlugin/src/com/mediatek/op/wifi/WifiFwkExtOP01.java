package com.mediatek.op.wifi;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.common.wifi.IWifiFwkExt;

import java.util.List;

@PluginImpl(interfaceName="com.mediatek.common.wifi.IWifiFwkExt")
public class WifiFwkExtOP01 extends DefaultWifiFwkExt {
    private static final String TAG = "WifiFwkExtOP01";

    private ConnectTypeObserver mConnectTypeObserver;
    private ReminderTypeObserver mReminderTypeObserver;
    private long mSwitchSuspendTime = 0;
    private long mReselectSuspendTime = 0;
    private int mCellToWiFiPolicy = Settings.System.WIFI_CONNECT_TYPE_AUTO;
    private int mReminderType = IWifiFwkExt.WIFI_CONNECT_REMINDER_ALWAYS;
    private ConnectivityManager mCm;
    private ActivityManager mAm;

    public WifiFwkExtOP01(Context context) {
        super(context);
    }

    public void init() {
        super.init();
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mCellToWiFiPolicy = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
        mReminderType = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_REMINDER,
                IWifiFwkExt.WIFI_CONNECT_REMINDER_ALWAYS);
        mConnectTypeObserver = new ConnectTypeObserver(new Handler(), mContext);
        mReminderTypeObserver = new ReminderTypeObserver(new Handler(), mContext);
    }

    public boolean hasCustomizedAutoConnect() {
        return true;
    }

    public boolean shouldAutoConnect() {
        if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO) {
            Log.d("@M_" + TAG, "Should auto connect.");
            return true;
        } else {
            Log.d("@M_" + TAG, "Shouldn't auto connect.");
            return false;
        }
    }

    public boolean isWifiConnecting(int connectingNetworkId, List<Integer> disconnectNetworks) {
        Log.d("@M_" + TAG, "isWifiConnecting, mCellToWiFiPolicy:" + mCellToWiFiPolicy
            + ", connectingNetworkId:" + connectingNetworkId);
        boolean isConnecting = false;
        boolean autoConnect = false;
        NetworkInfo info = mCm.getActiveNetworkInfo();
        if (null == info) {
            Log.d("@M_" + TAG, "No active network.");
        } else {
            Log.d("@M_" + TAG, "Active network type:" + info.getTypeName());
        }
        String highestPriorityNetworkSSID = null;
        int highestPriority = -1;
        int highestPriorityNetworkId = -1;
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        if (null != networks && null != scanResults) {
            for (WifiConfiguration network : networks) {
                for (ScanResult scanresult : scanResults) {
                    //Xlog.d(TAG, "network.SSID=" + network.SSID + ", scanresult.SSID=" + scanresult.SSID);
                    //Xlog.d(TAG, "network.security=" + getSecurity(network)
                    //      + ", scanresult.security=" + getSecurity(scanresult));
                    if ((network.SSID != null) && (scanresult.SSID != null)
                        && network.SSID.equals("\"" + scanresult.SSID + "\"")
                        && (getSecurity(network) == getSecurity(scanresult))) {
                        if (network.priority > highestPriority) {
                            highestPriority = network.priority;
                            highestPriorityNetworkId = network.networkId;
                            highestPriorityNetworkSSID = network.SSID;
                        }
                        if (network.networkId == connectingNetworkId) {
                            isConnecting = true;
                        }
                    }
                }
            }
        }
        Log.d("@M_" + TAG, "highestPriorityNetworkId:" + highestPriorityNetworkId
            + ", highestPriorityNetworkSSID:" + highestPriorityNetworkSSID
            + ", highestPriority:" + highestPriority
            + ", currentTimeMillis:" + System.currentTimeMillis()
            + ", mSwitchSuspendTime:" + mSwitchSuspendTime
            + ", mReminderType:" + mReminderType);
        if (!isConnecting) {
            if (null != info && info.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_ASK) {
                    if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)
                        && (System.currentTimeMillis() - mSwitchSuspendTime >
                            IWifiFwkExt.SUSPEND_NOTIFICATION_DURATION)
                        && mReminderType == IWifiFwkExt.WIFI_CONNECT_REMINDER_ALWAYS) {
                        Intent intent = new Intent(IWifiFwkExt.WIFI_NOTIFICATION_ACTION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(IWifiFwkExt.EXTRA_NOTIFICATION_SSID, highestPriorityNetworkSSID);
                        intent.putExtra(IWifiFwkExt.EXTRA_NOTIFICATION_NETWORKID, highestPriorityNetworkId);
                        mContext.startActivity(intent);
                    }
                } else if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO) {
                    highestPriorityNetworkSSID = null;
                    highestPriority = -1;
                    highestPriorityNetworkId = -1;
                    if (null != networks && null != scanResults) {
                        for (WifiConfiguration network : networks) {
                            if (!disconnectNetworks.contains(network.networkId)
                                && !isDisableForFailure(network.disableReason)) {
                                for (ScanResult scanresult : scanResults) {
                                    //Xlog.d(TAG, "network.SSID=" + network.SSID
                                    //      + ", scanresult.SSID=" + scanresult.SSID);
                                    //Xlog.d(TAG, "network.security=" + getSecurity(network)
                                    //      + ", scanresult.security=" + getSecurity(scanresult));
                                    if ((network.SSID != null) && (scanresult.SSID != null)
                                        && network.SSID.equals("\"" + scanresult.SSID + "\"")
                                        && (getSecurity(network) == getSecurity(scanresult))) {
                                        if (network.priority > highestPriority) {
                                            highestPriority = network.priority;
                                            highestPriorityNetworkId = network.networkId;
                                            highestPriorityNetworkSSID = network.SSID;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.d("@M_" + TAG, "Mobile connected, highestPriorityNetworkId:" + highestPriorityNetworkId
                        + ", highestPriorityNetworkSSID:" + highestPriorityNetworkSSID
                        + ", highestPriority:" + highestPriority);
                    if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)) {
                        Log.d("@M_" + TAG, "Enable all networks for mobile is connected.");
                        //sendEnableAllNetworksBroadcast();
                        autoConnect = true;
                    }
                } else if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_MANUL) {
                    ComponentName cn = null;
                    String classname = null;
                    List<RunningTaskInfo> runningTasks = mAm.getRunningTasks(1);
                    if (runningTasks != null && runningTasks.size() > 0 && runningTasks.get(0) != null) {
                        cn = runningTasks.get(0).topActivity;
                    }
                    if (cn != null) {
                        classname = cn.getClassName();
                        Log.d("@M_" + TAG, "Class Name:" + classname);
                    } else {
                        Log.e("@M_" + TAG, "ComponentName is null!");
                    }
                    if (!IWifiFwkExt.WIFISETTINGS_CLASSNAME.equals(classname)
                        && highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)
                        && (System.currentTimeMillis() - mSwitchSuspendTime >
                            IWifiFwkExt.SUSPEND_NOTIFICATION_DURATION)
                        && mReminderType == IWifiFwkExt.WIFI_CONNECT_REMINDER_ALWAYS) {
                        Intent intent = new Intent(IWifiFwkExt.WIFI_NOTIFICATION_ACTION);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(IWifiFwkExt.EXTRA_NOTIFICATION_SSID, highestPriorityNetworkSSID);
                        intent.putExtra(IWifiFwkExt.EXTRA_NOTIFICATION_NETWORKID, highestPriorityNetworkId);
                        mContext.startActivity(intent);
                    }
                }
            } else {
                if (mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO) {
                    highestPriorityNetworkSSID = null;
                    highestPriority = -1;
                    highestPriorityNetworkId = -1;
                    if (null != networks && null != scanResults) {
                        for (WifiConfiguration network : networks) {
                            if (!disconnectNetworks.contains(network.networkId)
                                && !isDisableForFailure(network.disableReason)) {
                                for (ScanResult scanresult : scanResults) {
                                    //Xlog.d(TAG, "network.SSID=" + network.SSID
                                    //      + ", scanresult.SSID=" + scanresult.SSID);
                                    //Xlog.d(TAG, "network.security=" + getSecurity(network)
                                    //      + ", scanresult.security=" + getSecurity(scanresult));
                                    if ((network.SSID != null) && (scanresult.SSID != null)
                                        && network.SSID.equals("\"" + scanresult.SSID + "\"")
                                        && (getSecurity(network) == getSecurity(scanresult))) {
                                        if (network.priority > highestPriority) {
                                            highestPriority = network.priority;
                                            highestPriorityNetworkId = network.networkId;
                                            highestPriorityNetworkSSID = network.SSID;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.d("@M_" + TAG, "Mobile isn't connected, highestPriorityNetworkId:" + highestPriorityNetworkId
                        + ", highestPriorityNetworkSSID:" + highestPriorityNetworkSSID
                        + ", highestPriority:" + highestPriority);
                    if (highestPriorityNetworkId != -1 && !TextUtils.isEmpty(highestPriorityNetworkSSID)) {
                        Log.d("@M_" + TAG, "Enable all networks for mobile is not connected.");
                        //sendEnableAllNetworksBroadcast();
                        autoConnect = true;
                    }
                }
            }
        }
        Log.d("@M_" + TAG, "isWifiConnecting, isConnecting:" + isConnecting + ", autoConnect:" + autoConnect);
        return (isConnecting || autoConnect);
    }

    public boolean hasConnectableAp() {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            Log.d("@M_" + TAG, "Scan for checking connectable AP.");
            mWifiManager.startScan();
            return true;
        } else {
            return false;
        }
    }

    public void suspendNotification(int type) {
        if (IWifiFwkExt.NOTIFY_TYPE_SWITCH == type) {
            mSwitchSuspendTime = System.currentTimeMillis();
        } else if (IWifiFwkExt.NOTIFY_TYPE_RESELECT == type) {
            mReselectSuspendTime = System.currentTimeMillis();
        }
        Log.d("@M_" + TAG, "suspendNotification, mSwitchSuspendTime:" + mSwitchSuspendTime
            + ", mReselectSuspendTime:" + mReselectSuspendTime + ", type:" + type);
    }

    public int defaultFrameworkScanIntervalMs() {
        return IWifiFwkExt.DEFAULT_FRAMEWORK_SCAN_INTERVAL_MS;
    }

    public String getApDefaultSsid() {
        return mContext.getString(com.mediatek.internal.R.string.wifi_tether_configure_ssid_default_for_cmcc);
    }

    public boolean handleNetworkReselection() {
        Log.d("@M_" + TAG, "handleNetworkReselection, currentTimeMillis:" + System.currentTimeMillis()
            + ", mReselectSuspendTime:" + mReselectSuspendTime
            + ", mReminderType:" + mReminderType);
        if (System.currentTimeMillis() - mReselectSuspendTime >
                IWifiFwkExt.SUSPEND_NOTIFICATION_DURATION
            && mReminderType == IWifiFwkExt.WIFI_CONNECT_REMINDER_ALWAYS) {
            ComponentName cn = null;
            String classname = null;
            List<RunningTaskInfo> runningTasks = mAm.getRunningTasks(1);
            if (runningTasks != null && runningTasks.size() > 0 && runningTasks.get(0) != null) {
                cn = runningTasks.get(0).topActivity;
            }
            if (cn != null) {
                classname = cn.getClassName();
                Log.d("@M_" + TAG, "Class Name:" + classname);
            } else {
                Log.e("@M_" + TAG, "ComponentName is null!");
            }
            if (!IWifiFwkExt.RESELECT_DIALOG_CLASSNAME.equals(classname)) {
                Intent intent = new Intent(IWifiFwkExt.ACTION_RESELECTION_AP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private void sendUpdateSettingsBroadcast() {
        Intent intent = new Intent(IWifiFwkExt.AUTOCONNECT_SETTINGS_CHANGE);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendEnableAllNetworksBroadcast() {
        Intent intent = new Intent(IWifiFwkExt.AUTOCONNECT_ENABLE_ALL_NETWORKS);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isDisableForFailure(int reason) {
        return reason == WifiConfiguration.DISABLED_DNS_FAILURE
                || reason == WifiConfiguration.DISABLED_DHCP_FAILURE
                || reason == WifiConfiguration.DISABLED_AUTH_FAILURE
                || reason == WifiConfiguration.DISABLED_ASSOCIATION_REJECT;
    }

    /**
     * Has network selection or not.
     * @return the operator value
     */
    public int hasNetworkSelection() {
        return IWifiFwkExt.OP_01;
    }

    private class ConnectTypeObserver extends ContentObserver {
        private Context mMyContext;
        public ConnectTypeObserver(Handler handler, Context context) {
            super(handler);
            mMyContext = context;
            ContentResolver cr = mMyContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.WIFI_CONNECT_TYPE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mCellToWiFiPolicy = Settings.System.getInt(mMyContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
            mSwitchSuspendTime = 0;
            mReselectSuspendTime = 0;
            Log.d("@M_" + TAG, "ConnectTypeObserver, mCellToWiFiPolicy:" + mCellToWiFiPolicy);
            sendUpdateSettingsBroadcast();
        }
    }

    /**
     * Reminder type observer.
     */
    private class ReminderTypeObserver extends ContentObserver {
        private Context mMyContext;
        public ReminderTypeObserver(Handler handler, Context context) {
            super(handler);
            mMyContext = context;
            ContentResolver cr = mMyContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.WIFI_CONNECT_REMINDER), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mReminderType = Settings.System.getInt(mMyContext.getContentResolver(),
                Settings.System.WIFI_CONNECT_REMINDER,
                IWifiFwkExt.WIFI_CONNECT_REMINDER_ALWAYS);
            Log.d("@M_" + TAG, "ReminderTypeObserver, mReminderType:" + mReminderType);
        }
    }
}
