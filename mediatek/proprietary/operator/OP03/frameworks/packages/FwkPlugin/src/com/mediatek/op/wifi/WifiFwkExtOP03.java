package com.mediatek.op.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
//import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.database.ContentObserver;
import android.net.NetworkInfo;
//import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
//import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

//import java.io.FileDescriptor;
//import java.io.PrintWriter;
//import java.util.List;


import com.mediatek.common.PluginImpl;
import com.mediatek.common.wifi.IWifiFwkExt;
import com.mediatek.fwk.plugin.R;

@PluginImpl(interfaceName="com.mediatek.common.wifi.IWifiFwkExt")
public class WifiFwkExtOP03 extends DefaultWifiFwkExt {
    private static final String TAG = "WifiFwkExtOP03";

    /**
     * The icon to show in the 'available networks' notification. This will also
     * be the ID of the Notification given to the NotificationManager.
     */
    private static final int ICON_AUTH_FAILURE = R.drawable.ic_notify_wifi_alert;

    /**
     * The Notification object given to the NotificationManager.
     */
    private Notification mNotification;
    /**
     * Whether the notification is being shown, as set by us. That is, if the
     * user cancels the notification, we will not receive the callback so this
     * will still be true. We only guarantee if this is false, then the
     * notification is not showing.
     */
    private boolean mNotificationShown;
    private Context mContext;
    private NetworkInfo mNetworkInfo;
    private volatile int mWifiState;


    public WifiFwkExtOP03(Context context) {
        super(context);
        wifiNotificationControllerAuth(context);
    }

    public void init() {
         super.init();
    }

    public boolean needRandomSsid() {
        Log.d("@M_" + TAG, "needRandomSsid =yes");
        return true;
    }

    public void setCustomizedWifiSleepPolicy(Context context) {
        int sleepPolicy = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.WIFI_SLEEP_POLICY, Settings.Global.WIFI_SLEEP_POLICY_NEVER);
        Log.d("@M_" + TAG, "Before--> setCustomizedWifiSleepPolicy:" + sleepPolicy);
        if (sleepPolicy == Settings.Global.WIFI_SLEEP_POLICY_NEVER)
        sleepPolicy = Settings.Global.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED;

        Settings.Global.putInt(context.getContentResolver(), Settings.Global.WIFI_SLEEP_POLICY,
                sleepPolicy);
        Log.d("@M_" + TAG, "After--> setCustomizedWifiSleepPolicy is:" + sleepPolicy);
    }

    public int hasNetworkSelection() {
        return IWifiFwkExt.OP_03;
    }

   private void wifiNotificationControllerAuth(Context context) {
        mContext = context;
        mWifiState = WifiManager.WIFI_STATE_UNKNOWN;

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                            mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                    WifiManager.WIFI_STATE_UNKNOWN);
                            if (mWifiState == WifiManager.WIFI_STATE_DISABLED) {
                                setNotificationVisible(false);
                            }
                        } else if (intent.getAction().equals(
                                WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                            mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(
                                    WifiManager.EXTRA_NETWORK_INFO);
                            // reset & clear notification on a network connect & disconnect
                            switch(mNetworkInfo.getDetailedState()) {
                                case CONNECTED:
                                    setNotificationVisible(false);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }, filter);

    }

    /**
     * Display or don't display a notification for wifi authentication problem.
     * @param visible {@code true} if notification should be visible, {@code false} otherwise
     */
    public void setNotificationVisible(boolean visible) {
        Log.d("@M_" + TAG, "setNotificationVisible: " + visible);
        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Message message;
        if (visible) {

            if (mNotification == null) {
                // Cache the Notification object.
                mNotification = new Notification();
                mNotification.when = 0;
                mNotification.icon = ICON_AUTH_FAILURE;
                mNotification.flags = Notification.FLAG_AUTO_CANCEL;
                mNotification.contentIntent = TaskStackBuilder.create(mContext)
                        .addNextIntentWithParentStack(
                                new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
                        .getPendingIntent(0, 0, null, UserHandle.CURRENT);
                Log.d("@M_" + TAG, "mNotification is null");
            }

            CharSequence title = mContext.getResources().getText(R.string.wifi_auth_problem);
            CharSequence details = mContext.getResources().getText(
                    R.string.wifi_auth_problem_detail);
            mNotification.tickerText = title;
            mNotification.color = mContext.getResources().getColor(
                    com.android.internal.R.color.system_notification_accent_color);
            mNotification.setLatestEventInfo(mContext, title, details, mNotification.contentIntent);

            notificationManager.notifyAsUser(null, ICON_AUTH_FAILURE, mNotification,
                    UserHandle.ALL);
            Log.d("@M_" + TAG, "notificationManager notifyAsUser");
        } else {
            notificationManager.cancelAsUser(null, ICON_AUTH_FAILURE, UserHandle.ALL);
            Log.d("@M_" + TAG, "notificationManager cancelAsUser");
        }

        mNotificationShown = visible;
    }
}
