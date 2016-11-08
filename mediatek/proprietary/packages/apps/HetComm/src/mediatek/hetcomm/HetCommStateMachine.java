package com.mediatek.hetcomm;

import static android.net.ConnectivityManager.ACTION_TETHER_STATE_CHANGED;
import static android.telephony.PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
import static android.telephony.PhoneStateListener.LISTEN_SERVICE_STATE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.app.ActivityManager;

import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RemoteViews;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.util.ArrayList;

public class HetCommStateMachine extends StateMachine {
    private static final String TAG = "HetCommStateMachine";
    private static final boolean SAMPLE_DBG = true;

    private static final String WIFI_INTERFACE = "wlan0";

    public static final int EVENT_WIFI_CONNECTED = 1;
    public static final int EVENT_WIFI_DISCONNECTED = 2;
    public static final int EVENT_MOBILE_CONNECTED = 3;
    public static final int EVENT_MOBILE_DISCONNECTED = 4;
    public static final int EVENT_SWITCH_ENABLE = 5;
    public static final int EVENT_SWITCH_DISABLE = 6;
    public static final int EVENT_VPN_ON = 7;
    //public static final int EVENT_VPN_OFF = 8;
    public static final int EVENT_TETHERING_ON = 9;
    //public static final int EVENT_TETHERING_OFF = 10;
    public static final int EVENT_BEYOND_3G = 11;
    public static final int EVENT_POLL_NOTIFY_SPEED = 12;
    public static final int EVENT_ROAMING_ON = 13;
    public static final int EVENT_USER_SWITCH = 14;

    private static final int NO_NETWORK = -1;

    private static final int POOL_SPEED_INTERVAL = 1;
    private static final int CONSTANT_MB = 1024 * 1024; // 1 Mbps
    private static final int SPEED_THRESHOLD = 3 * CONSTANT_MB; // 1 Mbps
    private static final int IDLE_RESET_THRESHOLD = 120; //times POOL_SPEED_INTERVAL
    private static final int PROGRESS_MAX = 50;//MBps
    private static final double INITIAL_WLAN_THETA = 0.5;//MBps
    private static final double IDLE_RESET_WLAN_THETA = 0.7;//MBps
    private static final int UI_UPDATE_INTERVAL = 2;
    private static final int MIM_UI_SPEED = 1;

    private final TelephonyManager mTeleManager;
    private ConnectivityManager mConnMgr;
    private WifiManager mWifiManager;
    private INetworkManagementService mNetd;
    private Context mContext;
    private Handler mTarget;
    private View mDebugWindnowView;
    private TextView mDebugTextView;
    private boolean mIsAvailabled = false;

    private static final NetworkRequest VPN_REQUEST = new NetworkRequest.Builder()
    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
    .build();

    /* Default parent state */
    private State mDefaultState = new DefaultState();

    /* Available state */
    private State mAvailableState = new AvailableState();

    /* UnAvailable state */
    private State mUnAvailableState = new UnAvailableState();

    /* Start state */
    private State mRunningState = new RunningState();

    /* Stop state */
    private State mStopState = new StopState();

    private int mCurrentVpnNetworkId = NO_NETWORK;
    private Object  mNotificationSync;
    private Notification mHetCommNotification;
    private RemoteViews mNotificationRemoteView;
    private Resources mResource;

    private String mWifiInterface;
    private String mMobileInterface;

    private long mWlanRxBytes;
    private long mMobileRxBytes;
    private long mWlanTxBytes;
    private long mMobileTxBytes;
    private double mAvgWlanRate;
    private double mAvgMobileRate;
    private double mWlanTheta;
    private int mLastWifiSpeed;
    private int mLastMobileSpeed;
    private int mIdleCounter;
    private boolean mIsDebug = false;
    private int mTestMode;
    private int mShowNotification;
    private boolean mIsWifiConnected = false;
    private boolean mIsMobileConnected = false;
    private boolean mIsVpnOn = false;
    private boolean mIsTetheringOn = false;
    private boolean mIsBeyond3G = false;
    private boolean mIsRoaming = false;

    HetCommStateMachine(Handler handler, Context context, ConnectivityManager conn) {
        super(TAG, handler.getLooper());
        mTarget = handler;
        mContext = context;
        mConnMgr = conn;
        addState(mDefaultState);
        addState(mAvailableState, mDefaultState);
        addState(mUnAvailableState, mDefaultState);
        addState(mRunningState, mAvailableState);
        addState(mStopState, mAvailableState);

        setInitialState(mUnAvailableState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ACTION_TETHER_STATE_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        mContext.registerReceiver(mIntentReceiver, filter);

        mTeleManager = TelephonyManager.getDefault();

        mNotificationSync = new Object();
        mResource = context.getResources();

        mWlanTheta = INITIAL_WLAN_THETA;

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mWifiInterface = WIFI_INTERFACE;

        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNetd = INetworkManagementService.Stub.asInterface(b);

        mIdleCounter = 0;

        if (SystemProperties.get("persist.sys.hetcomm.debug").equals("1")
                || SystemProperties.get("persist.sys.hetcomm.debug").equals("true")) {
            mIsDebug = true;
        }

        mTestMode = SystemProperties.getInt("persist.sys.hetcomm.testmode", 0);
        Log.d(TAG, "Debug wnd:" + mIsDebug + ":" + mTestMode);
    }

    public void regiterCallback() {
        mConnMgr.registerNetworkCallback(VPN_REQUEST, mNetworkCallback);
    }

    public void stop() {
        Log.d(TAG, "stop");
        hideDebugScreen();
        clearHetCommNotification();
        mContext.unregisterReceiver(mIntentReceiver);
        mConnMgr.unregisterNetworkCallback(mNetworkCallback);
        quitNow();
    }

    class DefaultState extends State {

        @Override
        public void enter() {
            Log.d(TAG, "[DefaultState] enter");
            init();
            setDebugMsg("[DefaultState] enter");
            checkStatusTransition();
        }

        @Override
        public boolean processMessage(Message message) {
            Log.i(TAG, "[DefaultState] what=" + getLogRecString(message));

            switch (message.what) {
            case EVENT_WIFI_CONNECTED:
                mIsWifiConnected = true;
                break;
            case EVENT_WIFI_DISCONNECTED:
                mIsWifiConnected = false;
                break;
            case EVENT_MOBILE_CONNECTED:
                mIsMobileConnected = true;
                break;
            case EVENT_MOBILE_DISCONNECTED:
                mIsMobileConnected = false;
                break;
            case EVENT_ROAMING_ON:
                mTarget.sendMessage(mTarget.obtainMessage(EVENT_ROAMING_ON));
                break;
            case EVENT_VPN_ON:
                mIsVpnOn = true;
                mTarget.sendMessage(mTarget.obtainMessage(EVENT_VPN_ON));
                break;
            case EVENT_TETHERING_ON:
                mIsTetheringOn = true;
                mTarget.sendMessage(mTarget.obtainMessage(EVENT_TETHERING_ON));
                break;
            }

            checkStatusTransition();
            return HANDLED;
        }

        private void init() {
            NetworkInfo info = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI);
            mIsMobileConnected = (info != null) ? info.isConnected() : false;
            mIsRoaming = (info != null) ? info.isRoaming() : false;
            info = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            mIsWifiConnected = (info != null) ? info.isConnected() : false;

            if (mIsDebug) {
                showDebugScreen();
            }

/*
            mIsBeyond3G = (mTeleManager.getDataNetworkType()
                           >= TelephonyManager.NETWORK_TYPE_UMTS) ? false : true;
            Log.d(TAG, "init mIsBeyond3G:" + mTeleManager.getDataNetworkType());
*/

            mLastWifiSpeed = -1;
            mLastMobileSpeed = -1;

            //TODO: add read tethering and vpn status
            //mIsVpnOn =
            //mIsTetheringOn
        }

        private void checkStatusTransition() {
            Log.d(TAG, "mIsWifiConnected:" + mIsWifiConnected +
                  " mIsMobileConnected:" + mIsMobileConnected +
                  " mTestMode:" + mTestMode);

            Log.d(TAG, "mIsVpnOn:" + mIsVpnOn +
                  " mIsTetheringOn:" + mIsTetheringOn +
                  " mIsRoaming:" + mIsRoaming);

            Log.d(TAG, "mIsBeyond3G:" + mIsBeyond3G);

            if (mTestMode == 1) {
                mMobileInterface = "wlan0";
                transitionTo(mAvailableState);
                return;
            }

            if (mIsWifiConnected && mIsMobileConnected &&
                    !mIsVpnOn && !mIsTetheringOn &&
                    !mIsRoaming) {
                transitionTo(mAvailableState);
            } else {
                transitionTo(mUnAvailableState);
            }
        }
    }

    class AvailableState extends State {
        @Override
        public void enter() {
            Log.d(TAG, "[AvailableState] enter");

            try {
                if (mMobileInterface == null || mMobileInterface.length() == 0) {
                    Log.e(TAG, "The interface name is wrong:" + mMobileInterface);
                    transitionTo(mUnAvailableState);
                    return;
                }

                mNetd.setHetCommInterface(mWifiInterface, mMobileInterface,
                                          mWlanTheta, 1 - mWlanTheta);
                mIsAvailabled = true;
            } catch (Exception e) {
                Log.d(TAG, "setHetCommInterface e:" + e);
                transitionTo(mUnAvailableState);
                return;
            }

            transitionTo(mRunningState);
        }

        @Override
        public boolean processMessage(Message message) {
            Log.i(TAG, "[AvailableState] what=" + getLogRecString(message));

            switch (message.what) {
            case EVENT_WIFI_CONNECTED:
            case EVENT_MOBILE_CONNECTED:
                return HANDLED;
            }

            return NOT_HANDLED;
        }
    }

    class UnAvailableState extends State {
        @Override
        public void enter() {
            Log.d(TAG, "[UnAvailableState] enter");

            if (mIsAvailabled) {
                mIsAvailabled = false;
            }
        }

        @Override
        public boolean processMessage(Message message) {
            Log.i(TAG, "[UnAvailableState] what=" + getLogRecString(message));
            boolean retValue = HANDLED;

            switch (message.what) {
            case EVENT_WIFI_DISCONNECTED:
                mIsWifiConnected = false;
                break;
            case EVENT_MOBILE_DISCONNECTED:
                mIsMobileConnected = false;
                break;

            default:
                retValue = NOT_HANDLED;
                break;
            }

            return retValue;
        }
    }

    class RunningState extends State {
        @Override
        public void enter() {
            Log.d(TAG, "[RunningState] enter");
            startMonitoring();
        }

        @Override
        public boolean processMessage(Message message) {
            Log.i(TAG, "[RunningState] what=" + getLogRecString(message));
            boolean retValue = HANDLED;

            switch (message.what) {
            case EVENT_WIFI_CONNECTED:
            case EVENT_MOBILE_CONNECTED:
                break;
            case EVENT_POLL_NOTIFY_SPEED:
                onPollNetworkSpeed();
                break;
            default:
                retValue = NOT_HANDLED;
                break;
            }

            return retValue;
        }

        @Override
        public void exit() {
            Log.i(TAG, "[RunningState] exit");
            stopMonitoring();

            try {
                mNetd.removeHetCommInterface();
            } catch (Exception e) {
                Log.d(TAG, "removeHetCommInterface e:" + e);
            }
        }
    }

    class StopState extends State {
        @Override
        public void enter() {
            Log.d(TAG, "[StopState] enter");
        }

        @Override
        public boolean processMessage(Message message) {
            Log.i(TAG, "[StopState] what=" + getLogRecString(message));
            return NOT_HANDLED;
        }
    }

    private String getInterfaceName(int nwType) {
        String interfaceName = null;

        LinkProperties linkProp = mConnMgr.getLinkProperties(nwType);

        if (linkProp != null) {
            interfaceName = linkProp.getInterfaceName();
        }

        if (mTestMode == 1 && interfaceName == null) {
            interfaceName = "wlan0";
        }

        Log.e(TAG, "getInterfaceName:" + interfaceName);
        return interfaceName;
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, "onReceive: " + action);

            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                final NetworkInfo networkInfo = (NetworkInfo)
                                                intent.getParcelableExtra(
                                                    ConnectivityManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "networkInfo: " + networkInfo);

                if (networkInfo != null) {
                    int type = networkInfo.getType();

                    if (type == ConnectivityManager.TYPE_MOBILE_HIPRI) {
                        if (networkInfo.isConnected()) {
                            //Get interface name firstly and process message later.
                            mMobileInterface = getInterfaceName(
                                                   ConnectivityManager.TYPE_MOBILE_HIPRI);
                            mIsRoaming = networkInfo.isRoaming();
                            if (mIsRoaming) {
                                sendMessage(EVENT_ROAMING_ON);
                            } else {
                                sendMessage(EVENT_MOBILE_CONNECTED);
                            }
                        } else {
                            sendMessage(EVENT_MOBILE_DISCONNECTED);
                        }
                    } else if (type == ConnectivityManager.TYPE_WIFI) {
                        if (networkInfo.isConnected()) {
                            sendMessage(EVENT_WIFI_CONNECTED);
                        } else {
                            sendMessage(EVENT_WIFI_DISCONNECTED);
                        }
                    }
                }
            } else if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                ArrayList<String> available = intent.getStringArrayListExtra(
                                                  ConnectivityManager.EXTRA_AVAILABLE_TETHER);

                ArrayList<String> tethered = intent.getStringArrayListExtra(
                                                 ConnectivityManager.EXTRA_ACTIVE_TETHER);

                if (tethered != null && tethered.size() > 0) {
                    Log.d(TAG, "tethered_iface:");
                    sendMessage(EVENT_TETHERING_ON);
                }
            } else if (action.equals(Intent.ACTION_USER_SWITCHED)) {
                mTarget.sendMessage(mTarget.obtainMessage(EVENT_USER_SWITCH));
            } else if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                if (ActivityManager.getCurrentUser() != 0) {
                    mTarget.sendMessage(mTarget.obtainMessage(EVENT_USER_SWITCH));
                }
            }
        }
    };


    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            NetworkCapabilities networkCapabilities =
                mConnMgr.getNetworkCapabilities(network);

            Log.d(TAG, "onAvailable " + network.netId + " : " + networkCapabilities);

            if (networkCapabilities == null) {
                Log.e(TAG, "The connection could be disconnected:" + network);
                return;
            }

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                if (network.netId != mCurrentVpnNetworkId) {
                    sendMessage(EVENT_VPN_ON);
                    mCurrentVpnNetworkId = network.netId;
                }
            }
        };

        // TODO Find another way to receive VPN lost.  This may be delayed depending on
        // how long the VPN connection is held on to.
        @Override
        public void onLost(Network network) {
            Log.d(TAG, "onLost " + network.netId);

            if (mCurrentVpnNetworkId == network.netId) {
                mCurrentVpnNetworkId = NO_NETWORK;
            }
        };
    };

    private void startMonitoring() {
        Log.d(TAG, "startMonitoring");

        mWlanTheta = INITIAL_WLAN_THETA;

        if (mMobileInterface == null || mMobileInterface.length() == 0) {
            Log.e(TAG, "null interface error!");
            return;
        }

        mWlanRxBytes = TrafficStats.getRxBytes(mWifiInterface);
        mMobileRxBytes = TrafficStats.getRxBytes(mMobileInterface);
        mWlanTxBytes = TrafficStats.getTxBytes(mWifiInterface);
        mMobileTxBytes = TrafficStats.getTxBytes(mMobileInterface);
        mAvgWlanRate = 0;
        mAvgMobileRate = 0;
        mShowNotification = 0;

        stopMonitoring();
        showHetCommNotification((int)mAvgWlanRate, (int)mAvgMobileRate);
        sendMessage(EVENT_POLL_NOTIFY_SPEED);
    }

    private void stopMonitoring() {
        Log.d(TAG, "stopMonitoring");

        clearHetCommNotification();
        removeMessages(EVENT_POLL_NOTIFY_SPEED);
    }

    private void showToast(int id) {
        final Resources r = mContext.getResources();
        Toast.makeText(mContext, r.getString(id), Toast.LENGTH_SHORT).show();
    }

    private void onPollNetworkSpeed() {
        //Calculate the traffic
        long latestWlanRxBytes = 0;
        long latestMobileRxBytes = 0;
        long deltaWlanRxBytes = 0;
        long deltaMobileRxBytes = 0;
        boolean isWlanRx = false;
        boolean isMobileRx = false;
        int wlanRate;
        int mobileRate;

        if (mMobileInterface == null || mMobileInterface.length() == 0) {
            Log.e(TAG, "The interface name is wrong:" + mMobileInterface);
            transitionTo(mUnAvailableState);
            return;
        }

        latestWlanRxBytes = TrafficStats.getRxBytes(mWifiInterface);
        latestMobileRxBytes = TrafficStats.getRxBytes(mMobileInterface);

        if (SAMPLE_DBG) {
            Log.d(TAG, "Wi-Fi:" + latestWlanRxBytes);
            Log.d(TAG, "Mobile:" + latestMobileRxBytes);
        }

        deltaWlanRxBytes = latestWlanRxBytes - mWlanRxBytes;
        deltaMobileRxBytes = latestMobileRxBytes - mMobileRxBytes;

        wlanRate = calcuateRate(deltaWlanRxBytes);
        mobileRate = calcuateRate(deltaMobileRxBytes);

        Log.d(TAG, "WifiSpeed Rx Mbps:" + wlanRate);
        Log.d(TAG, "MobileSpeed Rx Mbps:" + mobileRate);

        // Check Wi-Fi speed should be larger than 1 Mbps
        if (wlanRate > SPEED_THRESHOLD) {
            isWlanRx = true;

            // The Wi-Fi speed = 0.8 * old speed + 0.2 * new speed
            mAvgWlanRate = mAvgWlanRate * 0.8 + wlanRate * 0.2;

            Log.i(TAG, "[HetComm] WlanRxByte: " + deltaWlanRxBytes
                  + ", mAvgWlanRate: " + mAvgWlanRate);
        }


        // Check Mobile speed should be larger than 1 MBps
        if (mobileRate > SPEED_THRESHOLD) {
            isMobileRx = true;

            // The mobile speed = 0.8 * old speed + 0.2 * new speed
            mAvgMobileRate = mAvgMobileRate * 0.8 + mobileRate * 0.2;

            Log.i(TAG, "[HetComm] MobileRxByte: " + deltaMobileRxBytes
                  + ", mAvgMobileRate: " + mAvgMobileRate);
        }

        Log.d(TAG, "isMobileRx:" + isMobileRx + " ,isWlanRx:" + isWlanRx
              + " ,mWlanTheta:" + mWlanTheta);

        if (!isMobileRx && !isWlanRx &&
                (mWlanTheta >= IDLE_RESET_WLAN_THETA || mWlanTheta <= 1 - IDLE_RESET_WLAN_THETA)) {
            mIdleCounter++;

            if ( mIdleCounter > 2 * IDLE_RESET_THRESHOLD ) {
                mIdleCounter = mIdleCounter - IDLE_RESET_THRESHOLD;
            }

            if (mIdleCounter > IDLE_RESET_THRESHOLD && mWlanTheta != IDLE_RESET_WLAN_THETA ) {
                mWlanTheta = IDLE_RESET_WLAN_THETA;
                Log.d(TAG, "[HetComm] IDLE_RESET happens. setHetCommRatio: [" + mWlanTheta + "]");

                showToast(R.string.ratio_is_back);
                try {
                    mNetd.setHetCommRatio(mWlanTheta, 1 - mWlanTheta );
                    showDbgSpeed();
                } catch (Exception e) {
                    Log.d(TAG, "setHetCommRatio e:" + e);
                }
            }

            //} else if (isMobileRx | isWlanRx) {
        } else {

            mIdleCounter = 0;

            if (mAvgWlanRate != 0 && mAvgMobileRate != 0) {
                double latestWlanTheta = (mAvgWlanRate / (mAvgWlanRate + mAvgMobileRate)) * 10.0;

                if (latestWlanTheta > 7) latestWlanTheta = 10;

                if (latestWlanTheta < 3) latestWlanTheta = 0;

                int intTheta = (int) (latestWlanTheta);

                if (isWlanRx || isMobileRx) {
                    Log.i(TAG, "[HetComm] mWlanRxBytes: " + deltaWlanRxBytes +
                          ", mMobileRxBytes: " + deltaMobileRxBytes +
                          ", latestWlanTheta: " + latestWlanTheta +
                          ", intTheta: " + intTheta);
                    Log.i(TAG, "[HetComm] avg wlan rx rate: " + mAvgWlanRate
                            + ", avg mobile rx rate: " + mAvgMobileRate);
                }

                if (mWlanTheta != ((double)(intTheta)) / 10.0) {

                    mWlanTheta = ((double)(intTheta)) / 10.0;

                    Log.i(TAG, "[HetComm] new mWlanTheta: " + mWlanTheta);

                    Log.d(TAG, "[HetComm] update setHetCommRatio: [" + mWlanTheta + "]");

                    if (mWlanTheta == 1 || mWlanTheta == 0) {
                        showToast(R.string.ratio_is_single);
                    }

                    try {
                        mNetd.setHetCommRatio(mWlanTheta, 1 - mWlanTheta);
                        showDbgSpeed();
                    } catch (Exception e) {
                        Log.d(TAG, "setHetCommRatio e:" + e);
                    }
                }
            }
        }

        mWlanRxBytes = latestWlanRxBytes;
        mMobileRxBytes = latestMobileRxBytes;

        if (deltaWlanRxBytes > 0 || deltaMobileRxBytes > 0) {
            if (wlanRate < MIM_UI_SPEED && wlanRate > 0) {
                wlanRate = MIM_UI_SPEED;
            }

            if (mobileRate < MIM_UI_SPEED && mobileRate > 0) {
                mobileRate = MIM_UI_SPEED;
            }

            mShowNotification++;
            if (mShowNotification >= UI_UPDATE_INTERVAL) {
                showHetCommNotification(wlanRate, mobileRate);
                mShowNotification = 0;
            }
        }

        sendMessageDelayed(EVENT_POLL_NOTIFY_SPEED, POOL_SPEED_INTERVAL * 1000);
    }

    private int calcuateRate(long deltaRxBytes) {
        return Math.round((float) (deltaRxBytes * 8.0 / CONSTANT_MB) / POOL_SPEED_INTERVAL);
    }

    private void showHetCommNotification(int speed_wifi, int speed_mobile) {
        Log.i(TAG, "showHetCommNotification speed_wifi:"
              + speed_wifi + ", speed_mobile:" + speed_mobile);

        if (mLastWifiSpeed == speed_wifi
                && mLastMobileSpeed == speed_mobile && mHetCommNotification != null) {
            return;
        }

        mLastWifiSpeed = speed_wifi;
        mLastMobileSpeed = speed_mobile;

        synchronized (this.mNotificationSync) {
            NotificationManager notificationManager =
                (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                return;
            }

            Intent intent = new Intent();
            intent.setClassName("com.mediatek.hetcomm", "com.mediatek.hetcomm.HetCommActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            PendingIntent pi = PendingIntent.getActivityAsUser(mContext, 0, intent, 0,
                               null, UserHandle.CURRENT);

            CharSequence title = mResource.getText(R.string.notify_top_title);
            CharSequence app_name = mResource.getText(R.string.app_name);

            if (mNotificationRemoteView == null) {
                mNotificationRemoteView = new RemoteViews(mContext.getPackageName(),
                        R.layout.hetcomm_custom_notification);
            }

            Notification.Builder builder = new Notification.Builder(mContext)
            .setSmallIcon(R.drawable.ic_status_bar_hetcomm)
            .setOngoing (true)
            .setTicker(app_name)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContent(mNotificationRemoteView)
            .setContentIntent(pi)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setPriority(Notification.PRIORITY_MAX);

            mHetCommNotification = builder.build();

            mHetCommNotification.contentView.setImageViewResource(R.id.hetcomm_notification_icon,
                    R.drawable.ic_notification_hetcomm);
            mHetCommNotification.contentView.setTextViewText(R.id.notify_top_speed_text,
                    String.valueOf(speed_wifi + speed_mobile) );
            mHetCommNotification.contentView.setProgressBar(R.id.notify_speed_progress1,
                    PROGRESS_MAX, speed_wifi, false);
            mHetCommNotification.contentView.setProgressBar(R.id.notify_speed_progress2,
                    PROGRESS_MAX, speed_mobile, false);
            mHetCommNotification.contentView.setTextViewText(R.id.notify_network_speed_text1,
                    String.valueOf(speed_wifi));
            mHetCommNotification.contentView.setTextViewText(R.id.notify_network_speed_text2,
                    String.valueOf(speed_mobile));

            notificationManager.notifyAsUser(null, mHetCommNotification.icon,
                                             mHetCommNotification, UserHandle.ALL);
        }

        showDbgSpeed();
    }

    private void clearHetCommNotification() {
        Log.i(TAG, "clearHetCommNotification");

        synchronized (this.mNotificationSync) {
            NotificationManager notificationManager =
                (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null && mHetCommNotification != null) {
                notificationManager.cancelAsUser(null, mHetCommNotification.icon,
                                                 UserHandle.ALL);
                mHetCommNotification = null;
                mLastWifiSpeed = -1;
                mLastMobileSpeed = -1;
            }
        }
    }

    protected String getLogRecString(Message msg) {
        StringBuilder sb = new StringBuilder();

        switch (msg.what) {
        case EVENT_WIFI_CONNECTED:
            sb.append("EVENT_WIFI_CONNECTED");
            break;

        case EVENT_WIFI_DISCONNECTED:
            sb.append("EVENT_WIFI_DISCONNECTED");
            break;

        case EVENT_MOBILE_CONNECTED:
            sb.append("EVENT_MOBILE_CONNECTED");
            break;

        case EVENT_MOBILE_DISCONNECTED:
            sb.append("EVENT_MOBILE_DISCONNECTED");
            break;

        case EVENT_SWITCH_ENABLE:
            sb.append("EVENT_SWITCH_ENABLE");
            break;

        case EVENT_SWITCH_DISABLE:
            sb.append("EVENT_SWITCH_DISABLE");
            break;

        case EVENT_VPN_ON:
            sb.append("EVENT_VPN_ON");
            break;

        case EVENT_TETHERING_ON:
            sb.append("EVENT_TETHERING_ON");
            break;

        case EVENT_ROAMING_ON:
            sb.append("EVENT_ROAMING_ON");
            break;

        case EVENT_BEYOND_3G:
            sb.append("EVENT_BEYOND_3G");
            break;

        case EVENT_POLL_NOTIFY_SPEED:
            sb.append("EVENT_POLL_NOTIFY_SPEED");
            break;
        }

        return sb.toString();
    }

    private void showDebugScreen() {
        Log.v(TAG, "showDebugScreen");

        if (null == mDebugWindnowView) {
            try {
                LayoutInflater adbInflater = LayoutInflater.from(mContext);
                mDebugWindnowView = adbInflater.inflate(R.layout.hetcomm_debugwindow, null);

                // text view
                mDebugTextView = (TextView) mDebugWindnowView.findViewById(R.id.bodyText);
                mDebugTextView.setTextColor(0xffffffff);

                // layout param
                WindowManager.LayoutParams layoutParams;
                layoutParams = new WindowManager.LayoutParams();

                layoutParams.type = WindowManager.LayoutParams.TYPE_TOP_MOST;
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                layoutParams.alpha = 0.7f;

                // add view to window manager
                WindowManager windowManager = (WindowManager)
                                              mContext.getSystemService(Context.WINDOW_SERVICE);

                windowManager.addView(mDebugWindnowView, layoutParams);
            } catch (Exception e) {
                Log.e(TAG, "err in dbg:" + e);
            }
        }
    }

    private void hideDebugScreen() {
        Log.v(TAG, "hideDebugScreen");

        if (mDebugWindnowView != null) {

            try {
                // remove view to window manager
                WindowManager windowManager = (WindowManager)
                                              mContext.getSystemService(Context.WINDOW_SERVICE);

                windowManager.removeView(mDebugWindnowView);
                mDebugWindnowView = null;
            } catch (Exception e) {
                Log.e(TAG, "err in dbg2:" + e);
            }
        }

        mDebugTextView = null;
    }

    private void setDebugMsg(String msg) {
        if (mDebugTextView != null) {
            try {
                mDebugTextView.setText(msg);
            } catch (Exception e) {
                Log.e(TAG, "err in dbg3:" + e);
            }
        }
    }

    private void showDbgSpeed() {
        if (mIsDebug) {
            StringBuilder msg = new StringBuilder();
            msg.append("Data Rate:" + mLastWifiSpeed + ":" + mLastMobileSpeed + "\n");
            msg.append("Data Ratio:" + mWlanTheta + ":" + (1 - mWlanTheta));
            setDebugMsg(msg.toString());
        }
    }
}
