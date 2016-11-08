/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.tedongle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Slog;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.net.DhcpInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.WorkSource;
import android.os.Messenger;
import android.os.Bundle;
import android.net.LinkProperties;
//import android.net.LinkCapabilities;
import android.net.ConnectivityManager;
//import android.net.NetworkStateTracker;

import android.tedongle.TelephonyManager;
import com.android.internal.tedongle.TelephonyProperties;
import com.android.internal.tedongle.DctConstants;
import com.android.internal.tedongle.ITedongle;
import com.android.internal.tedongle.PhoneConstants;
import com.android.internal.tedongle.TelephonyIntents;
import com.android.internal.tedongle.ITedongleStateListener;
import android.tedongle.TedongleStateListener;
import com.android.internal.util.AsyncChannel;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.List;

import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.os.SystemProperties;
import android.os.AsyncResult;


/**
 * This class provides the primary API for managing all aspects of tedongle
 * connectivity. Get an instance of this class by calling
 */
public class TedongleManager {

    private static final String TAG = "3GD-TedongleManager";
    // Supplicant error codes:
    /**
     * The error code if there was a problem connect.
     */
    public static final int CONNECT_ERROR = 1;

    /**
     * Broadcast intent action indicating that TEDONGLE has been enabled, disabled,
     * enabling, disabling, or unknown. One extra provides this state as an int.
     * Another extra provides the previous state, if available.
     *
     */
    ///@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String TEDONGLE_RADIO_STATE_CHANGED_ACTION =
        "android.tedongle.TEDONGLE_RADIO_STATE_CHANGED";
    /**
     * The lookup key for an int that indicates whether dongle radio is enabled,
     * disabled, enabling, disabling, or unknown.
     */
    public static final String TEDONGLE_RADIO_STATE = "tedongle_radio_state";
    /**
     * The previous tedongle state.
     *
     */
    public static final String EXTRA_PREVIOUS_TEDONGLE_RADIO_STATE = "previous_tedongle_radio_state";


    public static final int TEDONGLE_RADIO_STATE_DISABLING = 0;
    /**
     * tedongle is disabled.
     *
     */
    public static final int TEDONGLE_RADIO_STATE_DISABLED = 1;

    public static final int TEDONGLE_RADIO_STATE_ENABLING = 2;

    public static final int TEDONGLE_RADIO_STATE_ENABLED = 3;

    public static final int TEDONGLE_RADIO_STATE_UNKNOWN = 4;


    /**
     * Broadcast intent action indicating that the state of tedongle connectivity
     * has changed. One extra provides the new state
     * in the form of a {@link android.net.NetworkInfo} object. If the new
     * state is CONNECTED
     */
    //@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.tedongle.STATE_CHANGE";
    /**
     * The lookup key for a {@link android.net.NetworkInfo} object associated with the
     *  network. Retrieve with
     * {@link android.content.Intent#getParcelableExtra(String)}.
     */
    public static final String EXTRA_NETWORK_INFO = "networkInfo";


    /** List of asyncronous notifications
     * @hide
     */
    public static final int DATA_ACTIVITY_NOTIFICATION = 1;

    //Lowest bit indicates data reception and the second lowest
    //bit indicates data transmitted
    /** @hide */
    public static final int DATA_ACTIVITY_NONE         = 0x00;
    /** @hide */
    public static final int DATA_ACTIVITY_IN           = 0x01;
    /** @hide */
    public static final int DATA_ACTIVITY_OUT          = 0x02;
    /** @hide */
    public static final int DATA_ACTIVITY_INOUT        = 0x03;

    /* Maximum number of active locks we allow.
     * This limit was added to prevent apps from creating a ridiculous number
     * of locks and crashing the system by overflowing the global ref table.
     */
    private static final int MAX_ACTIVE_LOCKS = 50;

    /* Number of currently active tedongleLocks and MulticastLocks */
    private int mActiveLockCount;

    //3gdongle add+++
    /** Network type is unknown */
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    /** Current network is GPRS */
    public static final int NETWORK_TYPE_GPRS = 1;
    /** Current network is EDGE */
    public static final int NETWORK_TYPE_EDGE = 2;
    /** Current network is UMTS */
    public static final int NETWORK_TYPE_UMTS = 3;
    /** Current network is CDMA: Either IS95A or IS95B*/
    public static final int NETWORK_TYPE_CDMA = 4;
    /** Current network is EVDO revision 0*/
    public static final int NETWORK_TYPE_EVDO_0 = 5;
    /** Current network is EVDO revision A*/
    public static final int NETWORK_TYPE_EVDO_A = 6;
    /** Current network is 1xRTT*/
    public static final int NETWORK_TYPE_1xRTT = 7;
    /** Current network is HSDPA */
    public static final int NETWORK_TYPE_HSDPA = 8;
    /** Current network is HSUPA */
    public static final int NETWORK_TYPE_HSUPA = 9;
    /** Current network is HSPA */
    public static final int NETWORK_TYPE_HSPA = 10;
    /** Current network is iDen */
    public static final int NETWORK_TYPE_IDEN = 11;
    /** Current network is EVDO revision B*/
    public static final int NETWORK_TYPE_EVDO_B = 12;
    /** Current network is LTE */
    public static final int NETWORK_TYPE_LTE = 13;
    /** Current network is eHRPD */
    public static final int NETWORK_TYPE_EHRPD = 14;
    /** Current network is HSPA+ */
    public static final int NETWORK_TYPE_HSPAP = 15;
    //3gdonle add --

    /////@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String TEDONGLE_CLEAR_NOTIFICATION_SHOW_FLAG_ACTION =
        "android.net.tedongle.TEDONGLE_CLEAR_NOTIFICATION_SHOW_FLAG_ACTION";

    /**
     * The lookup key for a boolean that indicates whether the pick network activity
     * is triggered by the notification.
     * Retrieve with {@link android.content.Intent#getBooleanExtra(String,boolean)}.
     * @hide
     */
    public static final String EXTRA_TRIGGERED_BY_NOTIFICATION = "notification";

    /**
     * Activity Action: Confirm with user if they want to connect to an AP.
     * @hide
     */
    ////@SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String TEDONGLE_NOTIFICATION_ACTION = "android.net.tedongle.TEDONGLE_NOTIFICATION";


    //
    //
    // SIM Card
    //
    //

    /** SIM card state: Unknown. Signifies that the SIM is in transition
     *  between states. For example, when the user inputs the SIM pin
     *  under PIN_REQUIRED state, a query for sim status returns
     *  this state before turning to SIM_STATE_READY. */
    public static final int SIM_STATE_UNKNOWN = 0;
    /** SIM card state: no SIM card is available in the device */
    public static final int SIM_STATE_ABSENT = 1;
    /** SIM card state: Locked: requires the user's SIM PIN to unlock */
    public static final int SIM_STATE_PIN_REQUIRED = 2;
    /** SIM card state: Locked: requires the user's SIM PUK to unlock */
    public static final int SIM_STATE_PUK_REQUIRED = 3;
    /** SIM card state: Locked: requries a network PIN to unlock */
    public static final int SIM_STATE_NETWORK_LOCKED = 4;
    /** SIM card state: Ready */
    public static final int SIM_STATE_READY = 5;
    /** SIM card state: Not Ready */
    public static final int SIM_STATE_NOT_READY = 6;

    //3gdongle
    protected static final int EVENT_TEDONGLE_MESSENGER_TEST                    = 104;

    private Context mContext;
    private static ITedongle mTedongleService;

    private static final int INVALID_KEY = 0;
    private static int mListenerKey = 1;
    private static final SparseArray mListenerMap = new SparseArray();
    private static final Object mListenerMapLock = new Object();

    private static AsyncChannel sAsyncChannel;
    private ServiceHandler sHandler;
    private Messenger mTedongleServiceMessenger;

    private static Object sThreadRefLock = new Object();
    private static int sThreadRefCount;
    private static HandlerThread sHandlerThread;

    private Messenger mTedongleManagerMSG;

    private TedongleManager() {
        Log.d(TAG, "TedongleManager default...");
        initDefault();
        init();
    }

    private static TedongleManager sInstance = new TedongleManager();

    /** @hide
    /* @deprecated - use getSystemService as described above */
    public static TedongleManager getDefault() {
        return sInstance;
    }


    /**
     * Create a new TedongleManager instance.
     * Applications will almost always want to use
     * @param context the application context
     */
    public TedongleManager(Context context) {
        Log.d(TAG, "TedongleManager...");
        if (mContext == null) {
            Context appContext = context.getApplicationContext();
            if (appContext != null) {
                mContext = appContext;
            } else {
                mContext = context;
            }
            init();
        }
    }


    public boolean setRadio(boolean turnon) {
        try {
            return mTedongleService.setRadio(turnon);
        } catch (RemoteException e) {
            return false;
        }
    }

	public boolean isDonglePluged() {
	    try {
            return mTedongleService.isDonglePluged();
        } catch (RemoteException e) {
            return false;
        }
    }
    //mark by xiaolei for compiling...
    /*public int enableApnType(String type) {
	    try {
            return mTedongleService.enableApnType(type);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int disableApnType(String type) {
	    try {
            return mTedongleService.disableApnType(type);
        } catch (RemoteException e) {
            return 0;
        }
    }*/

	public int getDataState() {
		try {
            return mTedongleService.getDataState();
        } catch (RemoteException e) {
            return TEDONGLE_RADIO_STATE_UNKNOWN;
        }
    }

    public int getDataActivity() {
		try {
            return mTedongleService.getDataActivity();
        } catch (RemoteException e) {
            return 0;
        }
    }

	public int getActivePhoneType() {
		try {
            return mTedongleService.getActivePhoneType();
        } catch (RemoteException e) {
            return 0;
        }
    }

	public int getNetworkType() {
		try {
            return mTedongleService.getNetworkType();
        } catch (RemoteException e) {
            return 0;
        }
    }
    //add for 3gdongle +++

    public String getLine1Number() {
		try {
            return mTedongleService.getLine1Number();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getSubscriberId() {
        try {
            return mTedongleService.getSubscriberId();
        } catch (RemoteException e) {
            return null;
        }
    }
    public ServiceState getServiceState() {
    	try {
            return mTedongleService.getServiceState();
        } catch (RemoteException e) {
            return null;
        }
    }

    public SignalStrength getSignalStrength() {
        try {
            return mTedongleService.getSignalStrength();
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean isSimReady() {
        try {
            return mTedongleService.isSimReady();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean supplyPin(String pin) {
        try {
            return mTedongleService.supplyPin(pin);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean supplyPuk(String puk, String pin) {
        try {
            return mTedongleService.supplyPuk(puk, pin);
        } catch (RemoteException e) {
            return false;
        }
    }


	/*public int getSimIndicateState() {

        try {
            return mTedongleService.getSimIndicateState();
        } catch (RemoteException e) {
            return 0;
        }

    }*/

    /**
     * Returns a constant indicating the state of the
     * device SIM card.
     *
     * @see #SIM_STATE_UNKNOWN
     * @see #SIM_STATE_ABSENT
     * @see #SIM_STATE_PIN_REQUIRED
     * @see #SIM_STATE_PUK_REQUIRED
     * @see #SIM_STATE_NETWORK_LOCKED
     * @see #SIM_STATE_READY
     */
    public int getSimState() {
        String prop = SystemProperties.get(TelephonyProperties.PROPERTY_SIM_STATE);
        Log.d(TAG, "getSimState: " + prop);

        if ("ABSENT".equals(prop)) {
            return SIM_STATE_ABSENT;
        }
        else if ("PIN_REQUIRED".equals(prop)) {
            return SIM_STATE_PIN_REQUIRED;
        }
        else if ("PUK_REQUIRED".equals(prop)) {
            return SIM_STATE_PUK_REQUIRED;
        }
        else if ("NETWORK_LOCKED".equals(prop)) {
            return SIM_STATE_NETWORK_LOCKED;
        }
        else if ("READY".equals(prop)) {
            return SIM_STATE_READY;
        }
        else if ("NOT_READY".equals(prop)) {
            return SIM_STATE_NOT_READY;
        }
        else {
            return SIM_STATE_UNKNOWN;
        }
    }

    //
    //
    // TedongleStateListener
    //
    //

    /**
     * Registers a listener object to receive notification of changes
     * in specified tedongle states.
     * <p>
     * To register a listener, pass a {@link TedongleStateListener}
     * and specify at least one telephony state of interest in
     * the events argument.
     *
     * At registration, and when a specified tedongle state
     * changes, the tedongle manager invokes the appropriate
     * callback method on the listener object and passes the
     * current (udpated) values.
     * <p>
     * To unregister a listener, pass the listener object and set the
     * events argument to
     * {@link TedongleStateListener#LISTEN_NONE LISTEN_NONE} (0).
     *
     * @param listener The {@link TedongleStateListener} object to register
     *                 (or unregister)
     * @param events The tedongle state(s) of interest to the listener,
     *               as a bitwise-OR combination of {@link TedongleStateListener}
     *               LISTEN_ flags.
     */
    public void listen(TedongleStateListener listener, int events) {
        //String pkgForDebug = mContext != null ? mContext.getPackageName() : "<unknown>";
        try {
            //Boolean notifyNow = (mTedongleService != null);
            mTedongleService.listen(listener.callback, events);
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }

    /**
     * Returns a string representation of the radio technology (network type)
     * currently in use on the device.
     * @return the name of the radio technology
     *
     * @hide pending API council review
     */
    public String getNetworkTypeName() {
        return getNetworkTypeName(getNetworkType());
    }

    public String getNetworkTypeName(int type) {
        switch (type) {
            case NETWORK_TYPE_GPRS:
                return "GPRS";
            case NETWORK_TYPE_EDGE:
                return "EDGE";
            case NETWORK_TYPE_UMTS:
                return "UMTS";
            case NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case NETWORK_TYPE_HSPA:
                return "HSPA";
            case NETWORK_TYPE_CDMA:
                return "CDMA";
            case NETWORK_TYPE_EVDO_0:
                return "CDMA - EvDo rev. 0";
            case NETWORK_TYPE_EVDO_A:
                return "CDMA - EvDo rev. A";
            case NETWORK_TYPE_EVDO_B:
                return "CDMA - EvDo rev. B";
            case NETWORK_TYPE_1xRTT:
                return "CDMA - 1xRTT";
            case NETWORK_TYPE_LTE:
                return "LTE";
            case NETWORK_TYPE_EHRPD:
                return "CDMA - eHRPD";
            case NETWORK_TYPE_IDEN:
                return "iDEN";
            case NETWORK_TYPE_HSPAP:
                return "HSPA+";
            default:
                return "UNKNOWN";
        }
    }

    public boolean getIccLockEnabled() {
        try {
            return mTedongleService.getIccLockEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
            //3gdongle
            Log.d(TAG, "setIccLockEnabled :" + " enabled:"+enabled+ " password:"+password
                + " message:"+onComplete);
            mSimSetLock = onComplete;
            Message msg = new Message();
            msg.what = MSG_ENABLE_ICC_PIN_COMPLETE_SERVICE;
            int enable = enabled ? 1 : 0;
            msg.arg1 = enable;
            // msg.arg2 =  Integer.parseInt(password);
            Bundle bundle = new Bundle();
            bundle.putString("password", password);
            msg.setData(bundle);
            msg.replyTo = mTedongleManagerMSG;
        try {
            mTedongleServiceMessenger.send(msg);
            //mTedongleService.setIccLockEnabled(enabled, password, onComplete);
        } catch (RemoteException e) {
            //return false;
        }
    }

    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        Log.d(TAG, "changeIccLockPassword :" + " oldPassword:"+oldPassword+ " newPassword:"+newPassword
            + " message:"+onComplete);

        mSimChangePin = onComplete;
        Message msg = new Message();
        msg.what = MSG_CHANGE_ICC_PIN_COMPLETE_SERVICE;
        // msg.arg1 = Integer.parseInt(oldPassword);
        //msg.arg2 = Integer.parseInt(newPassword);
        Bundle bundle = new Bundle();
        bundle.putString("oldpassword", oldPassword);
        bundle.putString("newpassword", newPassword);
        msg.setData(bundle);
        msg.replyTo = mTedongleManagerMSG;
        try {
            mTedongleServiceMessenger.send(msg);
            //mTedongleService.changeIccLockPassword(oldPassword, newPassword, onComplete);
        } catch (RemoteException e) {
            //return false;
        }
    }

    //add for 3gdongle ---


    /**
     * Calculates the level of the signal. This should be used any time a signal
     * is being shown.
     *
     * @param rssi The power of the signal measured in RSSI.
     * @param numLevels The number of levels to consider in the calculated
     *            level.
     * @return A level of the signal, given in the range of 0 to numLevels-1
     *         (both inclusive).
     */
    /*public static int calculateSignalLevel(int rssi, int numLevels) {
        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return numLevels - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (numLevels - 1);
            return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
        }
    }*/


    /** Interface for callback invocation on an application action {@hide} */
    public interface ActionListener {
        /** The operation succeeded */
        public void onSuccess();
        /**
         * The operation failed
         * @param reason The reason for failure could be one of
         * {@link #ERROR}, {@link #IN_PROGRESS} or {@link #BUSY}
         */
        public void onFailure(int reason);
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "ServiceHandler" + " MESSAGE:" + message.what);

            Object listener = removeListener(message.arg2);
            switch (message.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (message.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        sAsyncChannel.sendMessage(AsyncChannel.CMD_CHANNEL_FULL_CONNECTION);
                    } else {
                        Log.e(TAG, "Failed to set up channel connection");
                        // This will cause all further async API calls on the tedongleManager
                        // to fail and throw an exception
                        if (sAsyncChannel != null) {
                            Log.d(TAG, "Disconnect sAsyncChannel for failed to set up!");
                            sAsyncChannel.disconnect();
                            sAsyncChannel = null;
                        } else {
                            Log.d(TAG, "sAsyncChannel is null when failed to set up!");
                        }
                    }
                    break;
                case AsyncChannel.CMD_CHANNEL_FULLY_CONNECTED:
                    // Ignore
                    break;
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                    Log.e(TAG, "Channel connection lost");
                    // This will cause all further async API calls on the tedongleManager
                    // to fail and throw an exception
                    if (sAsyncChannel != null) {
                        Log.d(TAG, "Disconnect sAsyncChannel for channel connection lost!");
                        sAsyncChannel.disconnect();
                        sAsyncChannel = null;
                    } else {
                        Log.d(TAG, "sAsyncChannel is null when channel connection lost!");
                    }
                    getLooper().quit();
                    break;
                case MSG_ENABLE_ICC_PIN_COMPLETE:
                    Log.d(TAG, "3344MSG_ENABLE_ICC_PIN_COMPLETE:"+message);
                    //mSimSetLock.obj = (AsyncResult)message.obj;
                    //AsyncResult.forMessage(mSimSetLock).exception = ((AsyncResult)message.obj).exception;
                   //((Message)ar.userObj).sendToTarget();
                    mSimSetLock.arg1 = message.arg1;
                    mSimSetLock.sendToTarget();
                    break;
                case MSG_CHANGE_ICC_PIN_COMPLETE:
                    Log.d(TAG, "3344MSG_CHANGE_ICC_PIN_COMPLETE:"+message);
                    mSimChangePin.arg1 = message.arg1;
                    mSimChangePin.sendToTarget();
                    break;
                default:
                    //ignore
                    break;
            }
        }
    }

    private int putListener(Object listener) {
        if (listener == null) return INVALID_KEY;
        int key;
        synchronized (mListenerMapLock) {
            do {
                key = mListenerKey++;
            } while (key == INVALID_KEY);
            mListenerMap.put(key, listener);
        }
        return key;
    }

    private Object removeListener(int key) {
        if (key == INVALID_KEY) return null;
        synchronized (mListenerMapLock) {
            Object listener = mListenerMap.get(key);
            mListenerMap.remove(key);
            return listener;
        }
    }

    private void getTedongleService() {
        if ((mTedongleService == null)) {
            mTedongleService = ITedongle.Stub.asInterface(ServiceManager.getService("tedongleservice"));
        }
    }

    private void init() {
        Log.d(TAG, "Enter init, sThreadRefCount:" + sThreadRefCount);
        getTedongleService();
        mTedongleServiceMessenger = getTedongleServiceMessenger();
        if (mTedongleServiceMessenger == null) {
            sAsyncChannel = null;
            Log.e(TAG, "mTedongleServiceMessenger == null");
            return;
        }

        synchronized (sThreadRefLock) {
            if (++sThreadRefCount == 1) {
                sHandlerThread = new HandlerThread("TedongleManager");
                Log.d(TAG, "Create TedongleManager handlerthread");
                sHandlerThread.start();
                sHandler = new ServiceHandler(sHandlerThread.getLooper());
                sAsyncChannel = new AsyncChannel();
                sAsyncChannel.connect(mContext, sHandler, mTedongleServiceMessenger);
            }
        }

        //3gdongle
        //sendMessager = new Messenger(sHandler);
        //TestMessenger();
        mTedongleManagerMSG = new Messenger(sHandler);
        Log.d(TAG, "init end +++");
    }

    //3gdongle
    private Messenger sendMessager;
    private Message mSimSetLock;
    private Message mSimChangePin;
    private static final int EVENT_TEDONGLE_MANAGER = 120;

    	// For async handler to identify request type
	private static final int MSG_ENABLE_ICC_PIN_COMPLETE = 121;
	private static final int MSG_CHANGE_ICC_PIN_COMPLETE = 122;
    private static final int MSG_ENABLE_ICC_PIN_COMPLETE_SERVICE = 123;
	private static final int MSG_CHANGE_ICC_PIN_COMPLETE_SERVICE = 124;
	private static final int MSG_SIM_STATE_CHANGED = 125;

    private void TestMessenger() {
        Message msg = new Message();
        msg.what = EVENT_TEDONGLE_MESSENGER_TEST;
        msg.replyTo = sendMessager;
        try {
            mTedongleServiceMessenger.send(msg);
            Log.d(TAG, "3344TestMessenger++");
        } catch (RemoteException e) {
            //
        }
    }

    private void initDefault() {
        Log.d(TAG, "Enter initDefault()");
        getTedongleService();
    }
    /**
     * Get a reference to TedongleService handler. This is used by a client to establish
     * an AsyncChannel communication with TedongleService
     *
     * @return Messenger pointing to the TedongleService handler
     * @hide
     */
    public Messenger getTedongleServiceMessenger() {
        try {
            return mTedongleService.getTedongleServiceMessenger();
        } catch (RemoteException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }
    }


}
