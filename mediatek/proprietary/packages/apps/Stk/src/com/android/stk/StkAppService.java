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

package com.android.stk;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.ServiceState;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Input;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.CatCmdMessage.BrowserSettings;
import com.android.internal.telephony.cat.CatCmdMessage.SetupEventListSettings;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatResponseMessage;
import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.TextMessage;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.cat.CatCmdMessage.BrowserSettings;
import com.android.internal.telephony.cat.CatCmdMessage.CallSettings;
import com.android.internal.telephony.cat.CatService;
//import com.android.internal.telephony.cat.BearerDesc;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;

//import com.mediatek.op.telephony.cat.CatOpAppInterfaceImp;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.cat.ICatServiceExt;

import android.provider.Settings;
import android.telecom.TelecomManager;

import android.database.ContentObserver;

import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

import java.util.LinkedList;
import java.lang.System;
import java.util.List;
import java.util.Locale;

import android.text.TextUtils;
import static com.android.internal.telephony.PhoneConstants.SUBSCRIPTION_KEY;

import static com.android.internal.telephony.cat.CatCmdMessage.
                   SetupEventListConstants.IDLE_SCREEN_AVAILABLE_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.
                   SetupEventListConstants.LANGUAGE_SELECTION_EVENT;

/**
 * SIM toolkit application level service. Interacts with Telephopny messages,
 * application's launch and user input from STK UI elements.
 *
 */
public class StkAppService extends Service implements Runnable {

    // members
    protected class StkContext {
        protected CatCmdMessage mMainCmd = null;
        protected CatCmdMessage mCurrentCmd = null;
        protected CatCmdMessage mCurrentMenuCmd = null;
        protected Menu mCurrentMenu = null;
        protected String lastSelectedItem = null;
        protected boolean mMenuIsVisible = false;
        protected boolean mInputIsVisible = false;
        protected boolean mDialogIsVisible = false;
        protected boolean mIsInputPending = false;
        protected boolean mIsMenuPending = false;
        protected boolean mIsDialogPending = false;
        protected boolean responseNeeded = true;
        protected boolean launchBrowser = false;
        protected BrowserSettings mBrowserSettings = null;
        protected boolean mSetupMenuCalled = false;
        protected boolean mSetUpMenuHandled = false;
        protected boolean mNotified = false;
        protected boolean isUserAccessed = false;
        protected boolean mSetupCallInProcess = false; // true means in process.
        protected int mAvailable = STK_AVAIL_INIT;
        protected LinkedList<DelayedCmd> mCmdsQ = null;
        protected boolean mCmdInProgress = false;
        protected int mStkServiceState = STATE_UNKNOWN;
        protected int mSetupMenuState = STATE_UNKNOWN;
        protected int mMenuState = StkMenuActivity.STATE_INIT;
        protected int mOpCode = -1;
        private Activity mActivityInstance = null;
        private Activity mDialogInstance = null;
        private Activity mMainActivityInstance = null;
        private boolean mBackGroundTRSent = false;
        private int mSlotId = 0;
        private boolean mRemovePendingInputCmd = false;
        private boolean mIsLaunchDelayRetunTimer = false;
        protected int mDelayToCheckTime = 0;
        protected int mIccCardState = -1;
        protected boolean mRestoreMainMenu = false;
        protected boolean mMenuIsAlive = false;
        protected LinkedList<Activity> mMenuActivityInstanceHistory = null;
        private SetupEventListSettings mSetupEventListSettings = null;
        private boolean mClearSelectItem = false;
        private boolean mDisplayTextDlgIsVisibile = false;
        private CatCmdMessage mCurrentSetupEventCmd = null;
        private CatCmdMessage mIdleModeTextCmd = null;
        protected boolean mIsPendingCallDisconnectEvent = false;
        /*
         * This method is used to set pending activity instance by slot id.
         */
        final synchronized void setPendingActivityInstance(Activity act) {
            CatLog.d(this, "setPendingActivityInstance act : " + mSlotId + ", " + act);
            callSetActivityInstMsg(OP_SET_ACT_INST, mSlotId, act);
        }
        final synchronized Activity getPendingActivityInstance() {
            CatLog.d(this, "getPendingActivityInstance act : " + mSlotId + ", " +
                    mActivityInstance);
            return mActivityInstance;
        }
        final synchronized void setPendingDialogInstance(Activity act) {
            CatLog.d(this, "setPendingDialogInstance act : " + mSlotId + ", " + act);
            callSetActivityInstMsg(OP_SET_DAL_INST, mSlotId, act);
        }
        final synchronized Activity getPendingDialogInstance() {
            CatLog.d(this, "getPendingDialogInstance act : " + mSlotId + ", " +
                    mDialogInstance);
            return mDialogInstance;
        }
        final synchronized void setMainActivityInstance(Activity act) {
            CatLog.d(this, "setMainActivityInstance act : " + mSlotId + ", " + act);
            callSetActivityInstMsg(OP_SET_MAINACT_INST, mSlotId, act);
        }
        final synchronized Activity getMainActivityInstance() {
            CatLog.d(this, "getMainActivityInstance act : " + mSlotId + ", " +
                    mMainActivityInstance);
            return mMainActivityInstance;
        }
    }

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private Context mContext = null;
    private int mSimCount = 0;
    private PowerManager mPowerManager = null;
    private StkCmdReceiver mStkCmdReceiver = null;

    static int STK_GEMINI_SIM_NUM = 4;
    static int STK_GEMINI_BROADCAST_ALL = 99;
    private NotificationManager mNotificationManager = null;
    private int mEvdlCallObj = 0;
    private LinkedList<Integer> mEvdlCallObjQ = new LinkedList();

    static StkAppService sInstance = null;
    private AppInterface[] mStkService = null;
    private StkContext[] mStkContext = null;
    private Phone[] mPhone = null;
    // Used for setting FLAG_ACTIVITY_NO_USER_ACTION when
    // creating an intent.
    private Call.State mPreCallState = Call.State.IDLE;
    private Call.State mPreCallState2 = Call.State.IDLE;
    private Call.State mPreCallState3 = Call.State.IDLE;
    private Call.State mPreCallState4 = Call.State.IDLE;
    private Call.State mPreBgCallState = Call.State.IDLE;
    private Call.State mPreBgCallState2 = Call.State.IDLE;
    private Call.State mPreBgCallState3 = Call.State.IDLE;
    private Call.State mPreBgCallState4 = Call.State.IDLE;
    private enum InitiatedByUserAction {
        yes,            // The action was started via a user initiated action
        unknown,        // Not known for sure if user initated the action
    }
    //Add for stk IR case
    private RoamingMode mRoamingMode = RoamingMode.ROAMING_MODE_HOME;
    // constants
    static final String OPCODE = "op";
    static final String CMD_MSG = "cmd message";
    static final String RES_ID = "response id";
    static final String EVDL_ID = "downLoad event id";
    static final String MENU_SELECTION = "menu selection";
    static final String INPUT = "input";
    static final String HELP = "help";
    static final String CONFIRMATION = "confirm";
    static final String CHOICE = "choice";
    static final String SLOT_ID = "SLOT_ID";
    static final String STK_SOURCE_KEY = "STK_SOURCE";
    static final String STK_SOURCE_APP = "STK_APP";
    static final String STK_CMD = "STK CMD";
    static final String ACT_INST = "ACT INST";
    static final String DAL_INST = "DAL INST";
    static final String STK_DIALOG_URI = "stk://com.android.stk/dialog/";
    static final String STK_MENU_URI = "stk://com.android.stk/menu/";
    static final String STK_INPUT_URI = "stk://com.android.stk/input/";
    static final String STK_TONE_URI = "stk://com.android.stk/tone/";

    // These below constants are used for SETUP_EVENT_LIST
    static final String SETUP_EVENT_TYPE = "event";
    static final String SETUP_EVENT_CAUSE = "cause";

    // operations ids for different service functionality.
    static final int OP_CMD = 1;
    static final int OP_RESPONSE = 2;
    static final int OP_LAUNCH_APP = 3;
    static final int OP_END_SESSION = 4;
    static final int OP_BOOT_COMPLETED = 5;
    private static final int OP_DELAYED_MSG = 6;
    static final int OP_EVENT_DOWNLOAD = 7;
    private static final int OP_RESPONSE_IDLE_TEXT = 8;
    static final int OP_REMOVE_STM = 9;
    private static final int OP_EVDL_CALL_DISCONN_TIMEOUT = 10;
    private static final int OP_DELAY_TO_CHECK_IDLE = 11;
    private static final int OP_DELAY_TO_CHECK_ICCID = 12;
    static final int OP_CARD_STATUS_CHANGED = 13;
    static final int MSG_ID_ACCEPT_USER_INPUT = 15;
    static final int OP_SET_ACT_INST = 16;
    static final int OP_SET_DAL_INST = 17;
    static final int OP_SET_MAINACT_INST = 18;
    static final int OP_RETURN_INPUT_CMD = 19;
    static final int OP_RESTORE_CURRENT_CMD = 20;
    static final int OP_LAUNCH_DB_SETUP_MENU = 21;
    static final int OP_LOCALE_CHANGED = 22;
    static final int OP_ALPHA_NOTIFY = 23;
    static final int OP_IDLE_SCREEN = 24;
    static final int OP_CANCEL_TOAST_MSG = 25;

    //Invalid SetupEvent
    static final int INVALID_SETUP_EVENT = 0xFF;

    // Response ids
    static final int RES_ID_MENU_SELECTION = 11;
    static final int RES_ID_INPUT = 12;
    static final int RES_ID_CONFIRM = 13;
    static final int RES_ID_DONE = 14;
    static final int RES_ID_CHOICE = 15;

    static final int RES_ID_TIMEOUT = 20;
    static final int RES_ID_BACKWARD = 21;
    static final int RES_ID_END_SESSION = 22;
    static final int RES_ID_EXIT = 23;

    // DownLoad event ids
    static final int EVDL_ID_CALL_CONNECTED = 0X01;
    static final int EVDL_ID_CALL_DISCONNECTED = 0X02;
    static final int EVDL_ID_USER_ACTIVITY = 0x04;
    static final int EVDL_ID_IDLE_SCREEN_AVAILABLE = 0x05;
    static final int EVDL_ID_LANGUAGE_SELECT = 0x07;
    static final int EVDL_ID_BROWSER_TERMINATION = 0x08;
    static final int EVDL_ID_BROWSING_STATUS = 0x0F;

    static final int DEV_ID_KEYPAD = 0x01;
    static final int DEV_ID_DISPLAY = 0x02;
    static final int DEV_ID_EARPIECE = 0x03;
    static final int DEV_ID_UICC = 0x81;
    static final int DEV_ID_TERMINAL = 0x82;
    static final int DEV_ID_NETWORK = 0x83;

    static final int SETUP_CALL_NO_CALL_1 = 0x00;
    static final int SETUP_CALL_NO_CALL_2 = 0x01;
    static final int SETUP_CALL_HOLD_CALL_1 = 0x02;
    static final int SETUP_CALL_HOLD_CALL_2 = 0x03;
    static final int SETUP_CALL_END_CALL_1 = 0x04;
    static final int SETUP_CALL_END_CALL_2 = 0x05;

    static final int STK_VALID_SOURCE = 0;
    static final int STK_INVALID_PARAMTER = 1;
    static final int STK_INVALID_SOURCE = 2;

    static final int YES = 1;
    static final int NO = 0;

    static final int STATE_UNKNOWN = -1;
    static final int STATE_NOT_EXIST = 0;
    static final int STATE_EXIST = 1;

    private static final String PACKAGE_NAME = "com.android.stk";
    private static final String STK_MENU_ACTIVITY_NAME = PACKAGE_NAME + ".StkMenuActivity";
    private static final String STK_INPUT_ACTIVITY_NAME = PACKAGE_NAME + ".StkInputActivity";
    private static final String STK_DIALOG_ACTIVITY_NAME = PACKAGE_NAME + ".StkDialogActivity";
    // Notification id used to display Idle Mode text in NotificationManager.
    private static final int STK_NOTIFICATION_ID = 333;
    private static final String className = new Object() { } .getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);

    private static final int PHONE_STATE_CHANGED = 101;
    private static final int SUPP_SERVICE_FAILED = 102;

    private static final int PHONE_DISCONNECT = 1001;
    private static final int PHONE_DISCONNECT2 = 1002;
    private static final int PHONE_DISCONNECT3 = 1003;
    private static final int PHONE_DISCONNECT4 = 1004;
    private static final int[] PHONE_DISCONNECT_GEMINI = new int[]
            { PHONE_DISCONNECT, PHONE_DISCONNECT2,
              PHONE_DISCONNECT3, PHONE_DISCONNECT4 };

    public static final int STK_AVAIL_INIT = -1;
    public static final int STK_AVAIL_NOT_AVAILABLE = 0;
    public static final int STK_AVAIL_AVAILABLE = 1;

    private static boolean mPhoneStateChangeReg[] = {false, false, false, false};
    private static final int AP_EVDL_TIMEOUT = 8 * 1000;
    private static final int DELAY_TO_CHECK_IDLE_TIME = 3 * 1000;
    private static final int DELAY_TO_RETURN_INPUT_CMD_TIME = 20 * 1000;
    private static final int DELAY_TO_CANCEL_TOAST_TIMEOUT = 4 * 1000;
    private static final int DELAY_TO_CHECK_NUM = 2;
    private static final String ICCID_STRING_FOR_NO_SIM = "N/A";
    private String[] mIccId = new String[STK_GEMINI_SIM_NUM];
    private static final int[] INITIAL_RETRY_TIMER = {1,1,1,1,1,2,2,2,2,2,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5,5,5}; //90 secs total
    private int mInitializeWaitCounter = 0;
    private static final String[] PROPERTY_ICCID_SIM = {
        "ril.iccid.sim1",
        "ril.iccid.sim2",
        "ril.iccid.sim3",
        "ril.iccid.sim4",
    };

    static final String[] INSTALL_STK_IF_FLIGHT_MODE_ON_PROPERTY  = {
        "service.cat.install.on",
        "service.cat.install.on.2",
        "service.cat.install.on.3",
        "service.cat.install.on.4"
    };
    static final String RESET_MAIN_MENU_ACCESS = "android.intent.action.RESET_MAIN_ACCESS";
    //static final String PROPERTY_LAUNCHER_ACCEPT_INPUT = "stk.launcher.acceptUserInput";
    static final String NORMAL_SHUTDOWN_PROPERTY = "persist.service.stk.shutdown";
    static boolean mIsLauncherAcceptInput = false;
    private String mStkAppSourceKey = new String();
    Thread serviceThread = null;
    private Toast mToast = null;
    boolean mInCallUIState = false; // true: foreground, false: background
    String mLaunchBrowserUrl = null;
    int mLaunchBrowserUrlType = UNKNOWN_URL; // -1: unknown, 1:default url, 2:dedicated url
    private static final int UNKNOWN_URL = -1;
    private static final int DEFAULT_URL = 1;
    private static final int DEDICATED_URL = 2;
    // Inner class used for queuing telephony messages (proactive commands,
    // session end) while the service is busy processing a previous message.

    private class DelayedCmd {
        // members
        int id;
        CatCmdMessage msg;
        int slotId;

        DelayedCmd(int id, CatCmdMessage msg, int slotId) {
            this.id = id;
            this.msg = msg;
            this.slotId = slotId;
        }
    }

    static boolean isSetupMenuCalled(int SIMID) {
        CatLog.d("StkAppService", "isSetupMenuCalled, sim id: " + SIMID + ",[" + sInstance + "]");
        if (sInstance != null && SubscriptionManager.isValidSlotId(SIMID)) {
            CatLog.d("StkAppService", "isSetupMenuCalled, Stk context: " + sInstance.mStkContext[SIMID]);
            if (sInstance.mStkContext[SIMID] != null) {
                CatLog.d("StkAppService", "isSetupMenuCalled, removeMenu: "
                        + sInstance.mStkContext[SIMID].mSetupMenuCalled + "," + sInstance.removeMenu(SIMID));
                if (sInstance.mStkContext[SIMID].mSetupMenuCalled && !(sInstance.removeMenu(SIMID))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    static int getIccCardState(int SIMID) {
        CatLog.d("StkAppService", "getIccCardState, sim id: " + SIMID + ",[" + sInstance + "]");
        if (sInstance != null && SubscriptionManager.isValidSlotId(SIMID)) {
            if (sInstance.mStkContext[SIMID] != null) {
                CatLog.d("StkAppService", "mIccCardState: " +
                        sInstance.mStkContext[SIMID].mIccCardState);
                return sInstance.mStkContext[SIMID].mIccCardState;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public void onCreate() {
        CatLog.d(LOG_TAG, "onCreate()+");

        if (ActivityManager.getCurrentUser() != UserHandle.USER_OWNER) {
            CatLog.d(LOG_TAG, "onCreate: CurrentUser:" + ActivityManager.getCurrentUser() +
            " is not USER_OWNER:" + UserHandle.USER_OWNER + " !!!");
            return;
        }
        // Initialize members
        int i = 0;
        mContext = getBaseContext();
        mSimCount = TelephonyManager.from(mContext).getSimCount();
        CatLog.d(LOG_TAG, "simCount: " + mSimCount);
        mStkService = new AppInterface[mSimCount];
        mStkContext = new StkContext[mSimCount];
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // L-MR1
        mStkCmdReceiver = new StkCmdReceiver();
        registerReceiver(mStkCmdReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        StkApp.mPLMN = new String[mSimCount];
        StkApp.mIdleMessage = new String[mSimCount];
        mPhone = new Phone[mSimCount];
        mStkAppSourceKey = LOG_TAG + System.currentTimeMillis();

        for (i = 0; i < mSimCount; i++) {
            CatLog.d(LOG_TAG, "slotId: " + i);
            mStkService[i] = com.android.internal.telephony.cat.CatService.getInstance(i);
            mStkContext[i] = new StkContext();
            mStkContext[i].mSlotId = i;
            mStkContext[i].mAvailable = STK_AVAIL_INIT;
            mStkContext[i].mCmdsQ = new LinkedList<DelayedCmd>();
            mStkContext[i].mMenuActivityInstanceHistory = new LinkedList<Activity>();
            if (TelephonyManager.getDefault().hasIccCard(i)) {
                CatLog.d(LOG_TAG, "insert sim: " + i);

                int subId[] = SubscriptionManager.getSubId(i);
                if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                    int phoneId = SubscriptionManager.getPhoneId(subId[0]);
                    if (SubscriptionManager.isValidPhoneId(phoneId)) {
                        CatLog.d(LOG_TAG, "sim: " + i + ", " + subId[0] + ", " + phoneId);
                        try {
                            mPhone[i] = PhoneFactory.getPhone(phoneId);
                        } catch (IllegalStateException e) {
                            // Exception thrown by getPhone() when default phone is not made
                            CatLog.e(LOG_TAG, "IllegalStateException, get phone fail.");
                            e.printStackTrace();
                        }
                    } else {
                        mPhone[i] = null;
                        CatLog.d(LOG_TAG, "invalid phone id.");
                    }
                } else {
                    mPhone[i] = null;
                    CatLog.d(LOG_TAG, "invalid sub id.");
                }
            } else {
                CatLog.d(LOG_TAG, "no insert sim: " + i);
                mPhone[i] = null;
            }
        }
        CatLog.d(LOG_TAG, "new serviceThread()");

        serviceThread = new Thread(null, this, "Stk App Service");
        serviceThread.start();
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        sInstance = this;
        initNotify();

        IntentFilter mSIMStateChangeFilter =
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSIMStateChangeFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        mSIMStateChangeFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        mSIMStateChangeFilter.addAction(TelephonyIntents.ACTION_REMOVE_IDLE_TEXT);
        mSIMStateChangeFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mSIMStateChangeReceiver, mSIMStateChangeFilter);
        if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            registerReceiver(mEventDownloadCallReceiver, mEventDownloadCallFilter);
            registerReceiver(mBrowsingStatusReceiver, mBrowsingStatusFilter);
        }
        registerReceiver(mStkCallReceiver, mStkCallFilter);
        registerReceiver(mAirplaneModeReceiver, mAirplaneModeFilter);
        registerMSIMModeObserver();
        startPollingIccId();
        CatLog.d(LOG_TAG, " onCreate()-");
    }

    /**
     * @param intent The intent with action {@link TelephonyIntents#SPN_STRINGS_UPDATED_ACTION}
     * @return The string to use for the plmn, or null if it should not be shown.
     */
    private String getTelephonyPlmnFrom(Intent intent) {
        if (intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false)) {
            final String plmn = intent.getStringExtra(TelephonyIntents.EXTRA_PLMN);
            if (plmn != null) {
                return plmn;
            }
        }
        return getDefaultPlmn();
    }

    /**
     * @return The default plmn (no service)
     */
    private String getDefaultPlmn() {
        return getResources().getString(
                com.android.internal.R.string.lockscreen_carrier_default);
    }

    public void initNotify() {
        int i = 0;
        for (i = 0; i < mSimCount; i++) {
            StkApp.mPLMN[i] = getDefaultPlmn();
        }
        final IntentFilter filter = new IntentFilter();

        filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                    int simId = intent.getIntExtra(PhoneConstants.SLOT_KEY, PhoneConstants.SIM_ID_1);
                    if (simId >= PhoneConstants.SIM_ID_1 && simId < mSimCount) {
                        StkApp.mPLMN[simId] = getTelephonyPlmnFrom(intent);
                    }
                }
            }
        }, filter);
        return;
    }

    @Override
    public void onStart(Intent intent, int startId) {

        if (ActivityManager.getCurrentUser() != UserHandle.USER_OWNER) {
            CatLog.d(LOG_TAG, "onStart: CurrentUser:" + ActivityManager.getCurrentUser() +
            " is not USER_OWNER:" + UserHandle.USER_OWNER + " !!!");
            return;
        }

        if (intent == null) {
            CatLog.d(LOG_TAG, "StkAppService onStart intent is null so return");
            return;
        }

        Bundle args = intent.getExtras();
        if (args == null) {
            CatLog.d(LOG_TAG, "StkAppService onStart args is null so return");
            return;
        }

        int op = args.getInt(OPCODE);
        int slotId = -1;
        int i = 0;
        if (op != OP_BOOT_COMPLETED) {
            slotId = args.getInt(SLOT_ID);
        }
        CatLog.d(LOG_TAG, "StkAppService onStart sim id: " + slotId + ", op: " + op + ", " + args);
        if ((slotId >= 0 && slotId < mSimCount) && mStkService[slotId] == null) {
            mStkService[slotId] = com.android.internal.telephony.cat.CatService.getInstance(slotId);
            if (mStkService[slotId] == null) {
                CatLog.d(LOG_TAG, "onStart mStkService is: " + mStkContext[slotId].mStkServiceState);
                mStkContext[slotId].mStkServiceState = STATE_NOT_EXIST;
                //Check other StkService state.
                //If all StkServices are not available, stop itself and uninstall apk.
                for (i = PhoneConstants.SIM_ID_1; i < mSimCount; i++) {
                    if (i != slotId
                            && (mStkContext[i].mStkServiceState == STATE_UNKNOWN
               	            || mStkContext[i].mStkServiceState == STATE_EXIST)) {
      	               break;
                   }
                }
            } else {
                mStkContext[slotId].mStkServiceState = STATE_EXIST;
            }
            StkAppInstaller appInstaller = StkAppInstaller.getInstance();
            if (i == mSimCount) {
                CatLog.d(LOG_TAG, "All StkService are not available. stopSelf and uninstall.");
                stopSelf();
                //appInstaller.unInstall(mContext);
                return;
            }
        }
        if (slotId >= PhoneConstants.SIM_ID_1 && slotId < mSimCount && mPhone[slotId] == null) {
            mPhone[slotId] = getPhone(slotId);
        }
        if (slotId >= PhoneConstants.SIM_ID_1 && slotId < mSimCount) {
            if (mPhone[slotId] != null) {
            CatLog.d(LOG_TAG, "StkAppService onStart mPhone: " + mPhone[slotId] + ", mPhoneStateChangeReg: " + mPhoneStateChangeReg[slotId]);
                if (mPhoneStateChangeReg[slotId] == false) {
                    registerForCallState(slotId);
                }
            } else {
                CatLog.d(LOG_TAG, "mPhone " + slotId + " is null.");
            }
        }

        waitForLooper();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = op;
        msg.arg2 = slotId;
        switch(msg.arg1) {
        case OP_CMD:
            msg.obj = args.getParcelable(CMD_MSG);
            break;
        case OP_EVENT_DOWNLOAD:
            msg.obj = args;
            break;
        case OP_RESPONSE:
        case OP_CARD_STATUS_CHANGED:
        case OP_LOCALE_CHANGED:
        case OP_ALPHA_NOTIFY:
        case OP_IDLE_SCREEN:
            msg.obj = args;
            /* falls through */
        case OP_LAUNCH_APP:
        case OP_END_SESSION:
        case OP_BOOT_COMPLETED:
        case OP_REMOVE_STM:
        case OP_LAUNCH_DB_SETUP_MENU:
            break;
        case OP_SET_ACT_INST:
            msg.obj = args.getParcelable(ACT_INST);
            break;
        case OP_SET_DAL_INST:
            msg.obj = args.getParcelable(DAL_INST);
            break;
        default:
            return;
        }
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        CatLog.d(LOG_TAG, "onDestroy()");
        if (mStkCmdReceiver != null) {
            unregisterReceiver(mStkCmdReceiver);
            mStkCmdReceiver = null;
        }
        if (null != mToast) {
            mToast.cancel();
        }
        mPowerManager = null;
        unregisterReceiver(mSIMStateChangeReceiver);
        if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            unregisterReceiver(mEventDownloadCallReceiver);
            unregisterReceiver(mBrowsingStatusReceiver);
        }
        unregisterReceiver(mStkCallReceiver);
        unregisterReceiver(mAirplaneModeReceiver);
        unRegisterMSIMModeObserver();
        for (int i = 0; i < mSimCount; i++) {
            if (mPhone[i] == null) {
                mPhone[i] = getPhone(i);
            }
            CatLog.d(this, "onDestroy() mPhone: " + mPhone[i] + ", mPhoneStateChangeReg: " + mPhoneStateChangeReg[i]);
            if (mPhoneStateChangeReg[i] == true && mPhone[i] != null) {
                unregisterForCallState(i);
            } else {
                CatLog.d(LOG_TAG, "mPhone is null so don't need to unregister");
            }
        }
        waitForLooper();
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void run() {
        Looper.prepare();

        mServiceLooper = Looper.myLooper();
        mServiceHandler = new ServiceHandler();

        Looper.loop();
    }

    /*
     * Package api used by StkMenuActivity to indicate if its on the foreground.
     */
    void indicateMenuVisibility(boolean visibility, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            mStkContext[slotId].mMenuIsVisible = visibility;
        }
    }

    /*
     * Package api used by StkMenuActivity to indicate if its on the foreground.
     */
    void indicateMenuAlive(Activity act, boolean alive, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            if (alive) {
                mStkContext[slotId].mMenuActivityInstanceHistory.add(act);
            } else {
                mStkContext[slotId].mMenuActivityInstanceHistory.remove(act);
            }
            CatLog.d(LOG_TAG, "indicateMenuAlive: " +
                    mStkContext[slotId].mMenuActivityInstanceHistory.size());
            if (mStkContext[slotId].mMenuActivityInstanceHistory.size() > 0) {
                mStkContext[slotId].mMenuIsAlive = true;
            } else {
                mStkContext[slotId].mMenuIsAlive = false;
            }
        }
    }

    /*
     * Package api used by StkInputActivity to indicate if its on the foreground.
     */
    void indicateInputVisibility(boolean visibility, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            mStkContext[slotId].mInputIsVisible = visibility;
        }
    }

    /*
     * Package api used by StkDialogActivity to indicate if its on the foreground.
     */
    void indicateDialogVisibility(boolean visibility, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            mStkContext[slotId].mDialogIsVisible = visibility;
        }
    }

    /*
     * Package api used by StkDialogActivity to indicate if its on the foreground.
     */
    void setDisplayTextDlgVisibility(boolean visibility, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            mStkContext[slotId].mDisplayTextDlgIsVisibile = visibility;
        }
    }

    void indicatePendingInput(boolean isInputPending, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            CatLog.d(LOG_TAG, "indicatePendingInput: " + isInputPending);
            mStkContext[slotId].mIsInputPending = isInputPending;
        }
    }

    boolean isInputPending(int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            CatLog.d(LOG_TAG, "isInputFinishBySrv: " + mStkContext[slotId].mIsInputPending);
            return mStkContext[slotId].mIsInputPending;
        }
        return false;
    }

    void indicatePendingMenu(boolean isMenuPending, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            CatLog.d(LOG_TAG, "indicatePendingMenu: " + isMenuPending);
            mStkContext[slotId].mIsMenuPending = isMenuPending;
        }
    }

    boolean isMenuPending(int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            CatLog.d(LOG_TAG, "isMenuPending: " + mStkContext[slotId].mIsMenuPending);
            return mStkContext[slotId].mIsMenuPending;
        }
        return false;
    }

    void indicatePendingDialog(boolean isDialogPending, int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            CatLog.d(LOG_TAG, "indicatePendingDialog: " + isDialogPending);
            mStkContext[slotId].mIsDialogPending = isDialogPending;
        }
    }

    boolean isDialogPending(int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
            CatLog.d(LOG_TAG, "isDialogPending: " + mStkContext[slotId].mIsDialogPending);
            return mStkContext[slotId].mIsDialogPending;
        }
        return false;
    }

    /*
     * Package api used by StkMenuActivity to get its Menu parameter.
     */
    Menu getMenu(int slotId) {
        CatLog.d(LOG_TAG, "StkAppService, getMenu, sim id: " + slotId);
        if (slotId >=0 && slotId < mSimCount) {
            return mStkContext[slotId].mCurrentMenu;
        } else {
            return null;
        }
    }

    /*
     * Package api used by StkMenuActivity to get its Main Menu parameter.
     */
    Menu getMainMenu(int slotId) {
        CatLog.d(LOG_TAG, "StkAppService, getMainMenu, sim id: " + slotId);
        if (slotId >=0 && slotId < mSimCount) {
            if (mStkContext[slotId].mMainCmd != null) {
                return mStkContext[slotId].mMainCmd.getMenu();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    boolean isCurCmdSetupCall(int slotId) {
        if (slotId < 0 || slotId >= mSimCount) {
            CatLog.d(LOG_TAG, "[isCurCmdSetupCall] sim id is out of range");
            return false;
        }
        else if (mStkContext[slotId].mCurrentCmd == null) {
            CatLog.d(LOG_TAG, "[isCurCmdSetupCall][mCurrentCmd]:null");
            return false;
        } else if (mStkContext[slotId].mCurrentCmd.getCmdType() == null) {
            CatLog.d(LOG_TAG, "[isCurCmdSetupCall][mCurrentCmd.getCmdType()]:null");
            return false;
        } else {
            CatLog.d(LOG_TAG, "SET UP CALL Cmd Check["  + mStkContext[slotId].mCurrentCmd.getCmdType().value() + "]");
            return (AppInterface.CommandType.SET_UP_CALL.value() == mStkContext[slotId].mCurrentCmd.getCmdType().value());
        }
     }

    /*
     * Package api used by UI Activities and Dialogs to communicate directly
     * with the service to deliver state information and parameters.
     */
    static StkAppService getInstance() {
        return sInstance;
    }

    private void waitForLooper() {
        while (mServiceHandler == null) {
            if (serviceThread == null || serviceThread.isAlive() == false) {
                CatLog.d(LOG_TAG, "do re-init");
                init();
            }
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    CatLog.d(LOG_TAG, "[waitForLooper] InterruptedException");
                    e.printStackTrace();
                }
            }
        }
    }

    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(null == msg) {
                CatLog.d(LOG_TAG, "ServiceHandler handleMessage msg is null");
                return;
            }
            int opcode = msg.arg1;
            int slotId = msg.arg2;
            StkAppInstaller appInstaller = StkAppInstaller.getInstance();

            CatLog.d(LOG_TAG, "handleMessage opcode[" + opcode + "], sim id[" + slotId + "]");
            if (opcode == OP_CMD && msg.obj != null &&
                    ((CatCmdMessage)msg.obj).getCmdType()!= null) {
                CatLog.d(LOG_TAG, "cmdName[" + ((CatCmdMessage)msg.obj).getCmdType().name() + "]");
            }
            if (slotId >= PhoneConstants.SIM_ID_1 && slotId < mSimCount) {
                mStkContext[slotId].mOpCode = opcode;
            }
            switch (opcode) {
            case OP_LAUNCH_APP:
                if (null != mToast) {
                    mToast.cancel();
                }
                if (mStkContext[slotId].mMainCmd == null) {
                    CatLog.d(LOG_TAG, "mMainCmd is null");
                    // nothing todo when no SET UP MENU command didn't arrive.
                    mToast = Toast.makeText(mContext.getApplicationContext(),
                            R.string.main_menu_not_initialized, Toast.LENGTH_LONG);
                    mToast.setGravity(Gravity.BOTTOM, 0, 0);
                    mToast.show();
                    StkAppService.mIsLauncherAcceptInput = true;
                    //Workaround for the toast is not canceled sometimes.
                    Message msg1 = mServiceHandler.obtainMessage(OP_CANCEL_TOAST_MSG);
                    msg1.arg1 = OP_CANCEL_TOAST_MSG;
                    msg1.arg2 = slotId;
                    mServiceHandler.sendMessageDelayed(msg1, DELAY_TO_CANCEL_TOAST_TIMEOUT);
                    return;
                }
                if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
                    int subId[] = SubscriptionManager.getSubId(slotId);
                    Phone phone = getPhoneUsingSubId(subId[0]);
                    String cardType = phone.getIccCard().getIccCardType();
                    CatLog.d(LOG_TAG, "cardType: " + cardType);
                    if ((cardType.contains("RUIM") || cardType.contains("CSIM"))
                            && getRoamingState(slotId) != RoamingMode.ROAMING_MODE_NORMAL_ROAMING) {
                        CatLog.d(LOG_TAG, "Now is not normal roaming.");
                        mToast = Toast.makeText(mContext.getApplicationContext(),
                                R.string.main_menu_not_initialized, Toast.LENGTH_LONG);
                        mToast.setGravity(Gravity.BOTTOM, 0, 0);
                        mToast.show();
                        StkAppService.mIsLauncherAcceptInput = true;
                        return;
                    }
                }
/* This may not happend here.
                if (isBusyOnCall() == true) {
                    Toast toast = Toast.makeText(mContext.getApplicationContext(), R.string.lable_busy_on_call, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                    StkAppService.mIsLauncherAcceptInput = true;
                    return;
                }
*/

                if (mStkContext[slotId].mAvailable != STK_AVAIL_AVAILABLE) {
                    mToast = Toast.makeText(mContext.getApplicationContext(),
                            R.string.lable_not_available, Toast.LENGTH_LONG);
                    mToast.setGravity(Gravity.BOTTOM, 0, 0);
                    mToast.show();
                    StkAppService.mIsLauncherAcceptInput = true;
                    //Workaround for the toast is not canceled sometimes.
                    Message msg1 = mServiceHandler.obtainMessage(OP_CANCEL_TOAST_MSG);
                    msg1.arg1 = OP_CANCEL_TOAST_MSG;
                    msg1.arg2 = slotId;
                    mServiceHandler.sendMessageDelayed(msg1, DELAY_TO_CANCEL_TOAST_TIMEOUT);
                    return;
                }
                CatLog.d(LOG_TAG, "handleMessage OP_LAUNCH_APP - mCmdInProgress[" +
                        mStkContext[slotId].mCmdInProgress + "]");

                //If there is a pending activity for the slot id,
                //just finish it and create a new one to handle the pending command.
                cleanUpInstanceStackBySlot(slotId);

                //Clean up all other activities in stack except current 'slotId'.
                for (int i = 0; i < mSimCount; i++) {
                    if (i != slotId && mStkContext[i].mCurrentCmd != null) {
                        Activity otherAct = mStkContext[i].getPendingActivityInstance();
                        Activity otherDal = mStkContext[i].getPendingDialogInstance();
                        Activity otherMainMenu = mStkContext[i].getMainActivityInstance();
                        if (otherAct != null) {
                            CatLog.d(LOG_TAG, "finish pending otherAct and send SE. slot: " + i);
                            //Send end session for the pending proactive command of slot i.
                            //Set mBackGroundTRSent to true for ignoring the following end session event.
                            mStkContext[i].mBackGroundTRSent = true;
                            otherAct.finish();
                            mStkContext[i].mActivityInstance = null;
                        }
                        if (otherDal != null) {
                            CatLog.d(LOG_TAG, "finish pending otherDal.");
                            if (isDialogStyleCommand(i)) {
                                CatLog.d(LOG_TAG, "send TR for the other dialog.");
                                //Send negative confirmation for the pending dialog command of slot i.
                                mStkContext[i].mBackGroundTRSent = true;
                            }
                            otherDal.finish();
                            mStkContext[i].mDialogInstance = null;
                        }
                        if (otherMainMenu != null) {
                            CatLog.d(LOG_TAG, "finish pending otherMainMenu.");
                            otherMainMenu.finish();
                            mStkContext[i].mMainActivityInstance = null;
                        }
                    }
                }
                CatLog.d(LOG_TAG, "Current cmd type: " +
                        mStkContext[slotId].mCurrentCmd.getCmdType());
                //Restore the last command from stack by slot id.
                restoreInstanceFromStackBySlot(slotId);
                break;
            case OP_CMD:
                CatLog.d(LOG_TAG, "[OP_CMD]");
                CatCmdMessage cmdMsg = (CatCmdMessage) msg.obj;
                // There are two types of commands:
                // 1. Interactive - user's response is required.
                // 2. Informative - display a message, no interaction with the user.
                //
                // Informative commands can be handled immediately without any delay.
                // Interactive commands can't override each other. So if a command
                // is already in progress, we need to queue the next command until
                // the user has responded or a timeout expired.
                if (cmdMsg == null) {
                    /* In EMMA test case, cmdMsg may be null */
                    return;
                }
                if (!isCmdInteractive(cmdMsg)) {
                    CatLog.d(LOG_TAG, "[OP_CMD][Normal][Not Interactive]");
                    handleCmd(cmdMsg, slotId);
                } else {
                    TextMessage textmsg = cmdMsg.geTextMessage();
                    Input input         = cmdMsg.geInput();
                    CallSettings callsetting = cmdMsg.getCallSettings();
                    Activity dialog = mStkContext[slotId].getPendingDialogInstance();

                    CatLog.d(LOG_TAG, "[OP_CMD][Normal][Interactive]");
                    if (!mStkContext[slotId].mCmdInProgress) {
                        CatLog.d(LOG_TAG, "[OP_CMD][Normal][Interactive][not in progress]");
                        mStkContext[slotId].mCmdInProgress = true;
                        handleCmd((CatCmdMessage) msg.obj, slotId);
                    } else if (false == mStkContext[slotId].responseNeeded &&
                               true == mStkContext[slotId].mDialogIsVisible &&
                               null != dialog &&
                               ((null != textmsg && null != textmsg.text) ||
                                (null != input && null != input.text) ||
                                (null != callsetting && null != callsetting.confirmMsg &&
                                 null != callsetting.confirmMsg.text))) {
                        // ETSI TS 102 223: The terminal shall continue to display the text
                        // which includes an immediate response object until a subsequent
                        // proactive command is received containing display data;
                        CatLog.d(LOG_TAG, "Clean Immediate Response Display Text Dialog");
                        // First queue receiving command
                        mStkContext[slotId].mCmdsQ.addLast(new DelayedCmd(OP_CMD,
                                (CatCmdMessage) msg.obj, slotId));
                        // Clean the Immediate Response Display Text Dialog
                        dialog.finish();
                        mStkContext[slotId].mDialogInstance = null;
                    } else {
                        CatLog.d(LOG_TAG, "[Interactive][in progress]");
                        mStkContext[slotId].mCmdsQ.addLast(new DelayedCmd(OP_CMD,
                                (CatCmdMessage) msg.obj, slotId));
                    }
                }
                break;
            case OP_RESPONSE:
                CatLog.d(LOG_TAG, "[OP_RESPONSE][responseNeeded]: " +
                    mStkContext[slotId].responseNeeded + " mCmdsQ.size(): " +
                    mStkContext[slotId].mCmdsQ.size());
                // L-MR1
                if (mStkContext[slotId].responseNeeded) {
                    handleCmdResponse((Bundle) msg.obj, slotId);
                }
                // call delayed commands if needed.
                if (mStkContext[slotId].mCmdsQ.size() != 0) {
                    callDelayedMsg(slotId);
                } else {
                    mStkContext[slotId].mCmdInProgress = false;
                }
                // reset response needed state var to its original value.
                mStkContext[slotId].responseNeeded = true;
                break;
            case OP_END_SESSION:
                CatLog.d(LOG_TAG, "OP_END_SESSION: mCmdInProgress: " +
                    mStkContext[slotId].mCmdInProgress);
                if (!mStkContext[slotId].mCmdInProgress) {
                    mStkContext[slotId].mCmdInProgress = true;
                    handleSessionEnd(slotId);
                } else {
                    mStkContext[slotId].mCmdsQ.addLast(
                            new DelayedCmd(OP_END_SESSION, null, slotId));
                }
                break;
            case OP_BOOT_COMPLETED:
                CatLog.d(LOG_TAG, "OP_BOOT_COMPLETED");
/*
                int i = 0;
                for (i = PhoneConstants.SIM_ID_1; i < mSimCount; i++) {
                    if (mStkContext[i].mMainCmd != null) {
                        break;
                    }
                }
                if (i == mSimCount) {
                    appInstaller.unInstall(mContext);
                }
*/
                break;
            case OP_REMOVE_STM:
                CatLog.d(LOG_TAG, "OP_REMOVE_STM");
                StkAvailable(slotId, STK_AVAIL_NOT_AVAILABLE);
                setUserAccessState(false, slotId);
                if (mStkContext[slotId] != null) {
                    mStkContext[slotId].mCurrentMenu = null;
                    mStkContext[slotId].mSetupMenuCalled = false;
                }
                appInstaller.unInstall(mContext, slotId);
                if (mStkService[slotId] != null) {
                    mStkService[slotId].onDBHandler(slotId);
                }
                break;
            case OP_EVENT_DOWNLOAD:
                CatLog.d(LOG_TAG, "OP_EVENT_DOWNLOAD");
                handleEventDownload((Bundle) msg.obj, slotId);
                break;
            case OP_DELAYED_MSG:
                handleDelayedCmd(slotId);
                break;
            case OP_RESPONSE_IDLE_TEXT:
                handleIdleTextResponse(slotId);
                // End the process.
                mStkContext[slotId].mCmdInProgress = false;
                break;
            case OP_EVDL_CALL_DISCONN_TIMEOUT:
                if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    CatLog.d(LOG_TAG, "OP_EVDL_CALL_DISCONN_TIMEOUT() No disconn intent received.");
                    if (mStkService[slotId] != null && true == mStkService[slotId].isCallDisConnReceived()) {
                        SendEventDownloadMsg(EVDL_ID_CALL_DISCONNECTED, slotId);
                    }
                }
                break;
            case OP_DELAY_TO_CHECK_IDLE:
                launchTextDialog(slotId);
                break;
            case OP_DELAY_TO_CHECK_ICCID:
                ++mInitializeWaitCounter;
                startPollingIccId();
                break;
            case OP_LAUNCH_DB_SETUP_MENU:
                if (mStkService[slotId] != null) {
                    mStkService[slotId].onLaunchCachedSetupMenu();
                }
                break;
            case OP_CARD_STATUS_CHANGED:
                CatLog.d(LOG_TAG, "Card/Icc Status change received");
                handleCardStatusChangeAndIccRefresh((Bundle) msg.obj, slotId);
                break;
            case OP_SET_ACT_INST:
                Activity act = new Activity();
                act = (Activity) msg.obj;
                CatLog.d(LOG_TAG, "Set activity instance. " + act);
                mStkContext[slotId].mActivityInstance = act;
                break;
            case OP_SET_DAL_INST:
                Activity dal = new Activity();
                CatLog.d(LOG_TAG, "Set dialog instance. " + dal);
                dal = (Activity) msg.obj;
                mStkContext[slotId].mDialogInstance = dal;
                break;
            case OP_SET_MAINACT_INST:
                Activity mainAct = new Activity();
                mainAct = (Activity) msg.obj;
                CatLog.d(LOG_TAG, "Set activity instance. " + mainAct);
                mStkContext[slotId].mMainActivityInstance = mainAct;
                break;
            case OP_RETURN_INPUT_CMD:
                CatLog.d(LOG_TAG, "OP_RETURN_INPUT_CMD");
                mStkContext[slotId].mIsLaunchDelayRetunTimer = false;//reset timer flag
                if (mStkContext[slotId].mRemovePendingInputCmd) {
                    CatLog.d(LOG_TAG, "pending input timer canceled.");
                    return;
                } else {
                    CatLog.d(LOG_TAG, "send TR for the pending input.");
                    Bundle args = new Bundle();
                    args.putInt(StkAppService.RES_ID, RES_ID_END_SESSION);
                    args.putBoolean(StkAppService.HELP, false);
                    handleCmdResponse(args, slotId);
                    mStkContext[slotId].mCurrentCmd = mStkContext[slotId].mMainCmd;
                    mStkContext[slotId].mCurrentMenuCmd = mStkContext[slotId].mMainCmd;
                }
                break;
            case OP_RESTORE_CURRENT_CMD:
                CatLog.d(LOG_TAG, "OP_RESTORE_CURRENT_CMD");
                restoreInstanceFromStackBySlot(slotId);
                break;
            case OP_ALPHA_NOTIFY:
                handleAlphaNotify((Bundle) msg.obj);
                break;
            case OP_IDLE_SCREEN:
               for (int slot = 0; slot < mSimCount; slot++) {
                    if (mStkContext[slot] != null) {
                        handleIdleScreen(slot);
                    }
                }
                break;
            case OP_CANCEL_TOAST_MSG:
                CatLog.d(LOG_TAG, "OP_CANCEL_TOAST_MSG");
                if (null != mToast) {
                    mToast.cancel();
                }
                break;
            }
        }

        private void handleCardStatusChangeAndIccRefresh(Bundle args, int slotId) {
            boolean cardStatus = args.getBoolean(AppInterface.CARD_STATUS);
            StkAppInstaller appInstaller = StkAppInstaller.getInstance();
            CatLog.d(LOG_TAG, "CardStatus: " + cardStatus + " , slotId: " + slotId);
            if (cardStatus == false) {
                CatLog.d(LOG_TAG, "CARD is ABSENT");
                // Uninstall STKAPP, Clear Idle text, Stop StkAppService
                mNotificationManager.cancel(getNotificationId(slotId));
                boolean airPlane = false;
                String optr = SystemProperties.get("ro.operator.optr");
                if (optr != null && "OP02".equals(optr)) {
                    airPlane = isAirplaneModeOn(mContext);
                }
                if (!airPlane) {
                    appInstaller.unInstall(mContext, slotId);
                }
                //StkAvailable(slotId, false);
                if (mStkContext[slotId] != null) {
                    mStkContext[slotId].mCurrentMenu = null;
                    mStkContext[slotId].mCurrentMenuCmd = null;
                    mStkContext[slotId].mMainCmd = null;
                    mStkContext[slotId].mSetupMenuCalled = false;
                    if (mStkContext[slotId].mCmdsQ != null &&
                        mStkContext[slotId].mCmdsQ.size() != 0) {
                        CatLog.d(LOG_TAG, "There are commands in queue. size: " +
                        mStkContext[slotId].mCmdsQ.size());
                        mStkContext[slotId].mCmdsQ.clear();
                    }
                    mStkContext[slotId].mSetupMenuState = STATE_UNKNOWN;
                    mStkContext[slotId].mStkServiceState = STATE_UNKNOWN;
                    mStkContext[slotId].mDelayToCheckTime = 0;
                }
                if (mServiceHandler != null) {
                    mServiceHandler.removeMessages(OP_DELAY_TO_CHECK_ICCID);
                    mServiceHandler.removeMessages(OP_DELAY_TO_CHECK_IDLE);
                }
                //Reset CatService instance.
                mStkService[slotId] = null;
                if (isAllOtherCardsAbsent(slotId)) {
                    CatLog.d(LOG_TAG, "All CARDs are ABSENT. stopSelf");
                    //appInstaller.unInstall(mContext);
                    stopSelf();
                }
            } else {
                IccRefreshResponse state = new IccRefreshResponse();
                state.refreshResult = args.getInt(AppInterface.REFRESH_RESULT);

                CatLog.d(LOG_TAG, "Icc Refresh Result: "+ state.refreshResult);
                if ((state.refreshResult == IccRefreshResponse.REFRESH_RESULT_INIT) ||
                    (state.refreshResult == IccRefreshResponse.REFRESH_RESULT_RESET)) {
                    // Clear Idle Text
                    mNotificationManager.cancel(getNotificationId(slotId));
                }

                if (state.refreshResult == IccRefreshResponse.REFRESH_RESULT_RESET) {
                    // Uninstall STkmenu
                    //if (isAllOtherCardsAbsent(slotId)) {
                    //    appInstaller.unInstall(mContext);
                    //}
                    mStkContext[slotId].mCurrentMenu = null;
                    mStkContext[slotId].mMainCmd = null;
                }
            }
        }
    }
    /*
     * Check if all SIMs are absent except the id of slot equals "slotId".
     */
    private boolean isAllOtherCardsAbsent(int slotId) {
        TelephonyManager mTm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        int i = 0;

        for (i = 0; i < mSimCount; i++) {
            if (i != slotId && mTm.hasIccCard(i)) {
                break;
            }
        }
        if (i == mSimCount) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * If the device is not in an interactive state, we can assume
     * that the screen is idle.
     */
    private boolean isScreenIdle() {
        return (!mPowerManager.isInteractive());
    }

    private void handleIdleScreen(int slotId) {

        // If the idle screen event is present in the list need to send the
        // response to SIM.
        CatLog.d(this, "Need to send IDLE SCREEN Available event to SIM");
        //checkForSetupEvent(IDLE_SCREEN_AVAILABLE_EVENT, null, slotId);

        if (mStkContext[slotId].mIdleModeTextCmd != null) {
           launchIdleText(slotId);
        }
    }

    private void sendScreenBusyResponse(int slotId) {
        if (mStkContext[slotId].mCurrentCmd == null) {
            return;
        }
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        CatLog.d(this, "SCREEN_BUSY");
        resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
        mStkService[slotId].onCmdResponse(resMsg);
        if (mStkContext[slotId].mCmdsQ.size() != 0) {
            callDelayedMsg(slotId);
        } else {
            mStkContext[slotId].mCmdInProgress = false;
        }
    }

    private void sendResponse(int resId, int slotId, boolean confirm) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = OP_RESPONSE;
        Bundle args = new Bundle();
        args.putInt(StkAppService.RES_ID, resId);
        args.putInt(SLOT_ID, slotId);
        args.putBoolean(StkAppService.CONFIRMATION, confirm);
        msg.obj = args;
        mServiceHandler.sendMessage(msg);
    }

    private boolean isCmdInteractive(CatCmdMessage cmd) {
        switch (cmd.getCmdType()) {
        case SEND_DTMF:
        case SEND_SMS:
        case SEND_SS:
        case SEND_USSD:
        case SET_UP_IDLE_MODE_TEXT:
        case SET_UP_MENU:
        case CLOSE_CHANNEL:
        case RECEIVE_DATA:
        case SEND_DATA:
        case SET_UP_EVENT_LIST:
            return false;
        }

        return true;
    }

    private void handleDelayedCmd(int slotId) {
        CatLog.d(LOG_TAG, "handleDelayedCmd, mCmdsQ.size(): " +
        mStkContext[slotId].mCmdsQ.size() + " slotId: " + slotId);
        if (mStkContext[slotId].mCmdsQ.size() != 0) {
            DelayedCmd cmd = mStkContext[slotId].mCmdsQ.poll();
            if (cmd != null) {
                CatLog.d(LOG_TAG, "handleDelayedCmd, cmd.id: " +
                cmd.id + " cmd.slotId: " + cmd.slotId);
                switch (cmd.id) {
                case OP_CMD:
                    handleCmd(cmd.msg, cmd.slotId);
                    break;
                case OP_END_SESSION:
                    handleSessionEnd(cmd.slotId);
                    break;
                }
            }
        }
    }

    private void callDelayedMsg(int slotId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = OP_DELAYED_MSG;
        msg.arg2 = slotId;
        mServiceHandler.sendMessage(msg);
    }

    private void callSetActivityInstMsg(int inst_type, int slotId, Object obj) {
        Message msg = mServiceHandler.obtainMessage();
        msg.obj = obj;
        msg.arg1 = inst_type;
        msg.arg2 = slotId;
        mServiceHandler.sendMessage(msg);
    }

    private void handleSessionEnd(int slotId) {
        // mMenuState would be 'STATE_INIT' if the proactive command is coming alone
        // but not triggering from Stk main menu. So, the pending activity instance
        // should be finished.
        // We should finish all pending activity if receiving END SESSION command.
        cleanUpInstanceStackBySlot(slotId);

        mStkContext[slotId].mCurrentCmd = mStkContext[slotId].mMainCmd;
        CatLog.d(LOG_TAG, "[handleSessionEnd] - mCurrentCmd changed to mMainCmd!.");
        mStkContext[slotId].mCurrentMenuCmd = mStkContext[slotId].mMainCmd;
        CatLog.d(LOG_TAG, "slotId: " + slotId + ", mMenuState: " +
                mStkContext[slotId].mMenuState);

        indicatePendingInput(false, slotId);
        indicatePendingMenu(false, slotId);
        indicatePendingDialog(false, slotId);

        Intent intent = new Intent(RESET_MAIN_MENU_ACCESS);
        mContext.sendBroadcast(intent);

        if (mStkContext[slotId].mMainCmd == null) {
            CatLog.d(LOG_TAG, "[handleSessionEnd][mMainCmd is null!]");
        }
        mStkContext[slotId].lastSelectedItem = null;
        // In case of SET UP MENU command which removed the app, don't
        // update the current menu member.
        if (mStkContext[slotId].mCurrentMenu != null && mStkContext[slotId].mMainCmd != null) {
            mStkContext[slotId].mCurrentMenu = mStkContext[slotId].mMainCmd.getMenu();
        }
        CatLog.d(LOG_TAG, "[handleSessionEnd][mMenuIsAlive]: " +
        mStkContext[slotId].mMenuIsAlive);
        // In mutiple instance architecture, the main menu for slotId will be finished
        // when user goes to the Stk menu of other SIM.So, we should launch a new main
        // menu if the main menu instance has been finished.
        if (mStkContext[slotId].mMenuIsAlive) {
            CatLog.d(LOG_TAG, "[handleSessionEnd][mMenuState]: " +
            mStkContext[slotId].mMenuState);
            // If the current menu is secondary menu, we should launch main menu.
            if (StkMenuActivity.STATE_SECONDARY == mStkContext[slotId].mMenuState) {
                launchMenuActivity(null, slotId);
            }
/* We do not need to finish menu activity here, since menu activity will be finished itself.
            else {
                CatLog.d(LOG_TAG, "[handleSessionEnd][To finish menu activity]");
                finishMenuActivity(slotId);
            }
*/
        } else {
            //Make sure the menu state is STATE_MAIN when current command is back to setup menu.
            //Fix ALPS01943936
            mStkContext[slotId].mMenuState = StkMenuActivity.STATE_MAIN;
        }
        if (mStkContext[slotId].mCmdsQ.size() != 0) {
            callDelayedMsg(slotId);
        } else {
            mStkContext[slotId].mCmdInProgress = false;
        }
        // In case a launch browser command was just confirmed, launch that url.
        if (mStkContext[slotId].launchBrowser) {
            mStkContext[slotId].launchBrowser = false;
            launchBrowser(mStkContext[slotId].mBrowserSettings);
        }
    }

    // returns true if any Stk related activity already has focus on the screen
    private boolean isTopOfStack() {
        ActivityManager mAcivityManager = (ActivityManager) mContext
                .getSystemService(ACTIVITY_SERVICE);
        String currentPackageName = mAcivityManager.getRunningTasks(1).get(0).topActivity
                .getPackageName();
        if (null != currentPackageName) {
            return currentPackageName.equals(PACKAGE_NAME);
        }

        return false;
    }

    private void handleCmd(CatCmdMessage cmdMsg, int slotId) {
        StkAppInstaller appInstaller = StkAppInstaller.getInstance();
        if (cmdMsg == null) {
            return;
        }
        // save local reference for state tracking.
        mStkContext[slotId].mCurrentCmd = cmdMsg;
        boolean waitForUsersResponse = true;

        indicatePendingInput(false, slotId);
        indicatePendingMenu(false, slotId);
        indicatePendingDialog(false, slotId);

        byte[] additionalInfo = null;

        if (cmdMsg.getCmdType() != null) {
            CatLog.d(LOG_TAG, "handleCmd cmdName[" + cmdMsg.getCmdType().name() + "]  mCurrentCmd = cmdMsg");
        }

        CatLog.d(LOG_TAG,"[handleCmd]" + cmdMsg.getCmdType().name());
        switch (cmdMsg.getCmdType()) {
        case DISPLAY_TEXT:
            CatLog.d(LOG_TAG, "[handleCmd][DISPLAY_TEXT] +");
            if (isBusyOnCall() == true) {
                CatLog.d(LOG_TAG, "[Handle Command][DISPLAY_TEXT][Can not handle currently]");
                CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                additionalInfo = new byte[1];
                additionalInfo[0] = (byte) 0x02;
                resMsg.setAdditionalInfo(additionalInfo);
                mStkService[slotId].onCmdResponse(resMsg);
                return;
            }
            TextMessage msg = cmdMsg.geTextMessage();
            mStkContext[slotId].responseNeeded = msg.responseNeeded;
            if (mStkContext[slotId].responseNeeded == false) {
                //Immediate response
                CatLog.d(LOG_TAG, "[Handle Command][DISPLAY_TEXT][Should immediatly response]");
                CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
                  resMsg.setResultCode(ResultCode.OK);
                mStkService[slotId].onCmdResponse(resMsg);
            } else {
            }
            // TODO: get the carrier name from the SIM
            msg.title = "";
            String optr = SystemProperties.get("ro.operator.optr");
            if (optr != null && "OP02".equals(optr)) {
                 int resId = R.string.app_name;
                 if (slotId == PhoneConstants.SIM_ID_1) {
                     if (SystemProperties.get("ro.mtk_gemini_support").equals("1") == true) {
                         /* GEMINI or GEMINI+ project */
                         resId = R.string.appI_name;
                     } else {
                         /* Single card project */
                         resId = R.string.app_name;
                     }
                 } else if (slotId == PhoneConstants.SIM_ID_2){
                     resId = R.string.appII_name;
                 }
                 msg.title = getResources().getString(resId);
            }
            byte[] target = {0x0d, 0x0a};
            String strTarget = new String(target);
            String strLine = System.getProperty("line.separator");
            String strText = msg.text.replaceAll(strTarget, strLine);
            msg.text = strText;

            launchTextDialog(slotId);
            break;
        case SELECT_ITEM:
            CatLog.d(LOG_TAG, "SELECT_ITEM +");
            mStkContext[slotId].mCurrentMenuCmd = mStkContext[slotId].mCurrentCmd;
            mStkContext[slotId].mCurrentMenu = cmdMsg.getMenu();
            if (isBusyOnCall() == true) {
                CatLog.d(LOG_TAG, "[Handle Command][SELECT_ITEM][Can not handle currently]");
                CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                additionalInfo = new byte[1];
                additionalInfo[0] = (byte) 0x02;
                resMsg.setAdditionalInfo(additionalInfo);
                mStkService[slotId].onCmdResponse(resMsg);
                return;
            }
            //clean up previous activity instance.
            //e.g. when stk menu times out and select item is coming later.
            cleanUpInstanceStackBySlot(slotId);

            final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (iTel != null) {
                try {
                    int subId[] = SubscriptionManager.getSubId(slotId);
                    if(iTel.isRadioOnForSubscriber(subId[0], mContext.getOpPackageName()) == true) {
                        launchMenuActivity(cmdMsg.getMenu(), slotId);
                    } else {
                        CatLog.d(LOG_TAG, "radio off, send TR directly.");
                        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
                        resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                        if(null != mStkService[slotId])
                            mStkService[slotId].onCmdResponse(resMsg);
                    }
                } catch(RemoteException ex) {
                        ex.getMessage();
                }
            } else {
                CatLog.e(LOG_TAG, "ITelephonyEx is null.");
            }
            break;
        case SET_UP_MENU:
            CatLog.d(LOG_TAG, "[handleCmd][SET_UP_MENU] +, from modem: " + cmdMsg.getMenu().getSetUpMenuFlag());
            if (cmdMsg.getMenu().getSetUpMenuFlag() == 1) {
                CatLog.d(LOG_TAG, "Got SET_UP_MENU from modem");
                if (mStkContext[slotId].mCmdsQ.size() != 0) {
                    CatLog.d(LOG_TAG, "Command queue size is not 0 so to remove all items in the queue, size: " + mStkContext[slotId].mCmdsQ.size());
                    mStkContext[slotId].mCmdsQ.clear();
                }
                mStkContext[slotId].mCmdInProgress = false;
            }
            mStkContext[slotId].mSetupMenuCalled = true;
            mStkContext[slotId].mMainCmd = mStkContext[slotId].mCurrentCmd;
            mStkContext[slotId].mCurrentMenuCmd = mStkContext[slotId].mCurrentCmd;
            mStkContext[slotId].mCurrentMenu = cmdMsg.getMenu();
            CatLog.d(LOG_TAG, "SET_UP_MENU [" + removeMenu(slotId) + "]");
            appInstaller = StkAppInstaller.getInstance();

            boolean radio_on = true;
            boolean sim_locked = false;
            optr = SystemProperties.get("ro.operator.optr");

            radio_on = checkSimRadioState(mContext, slotId);
            CatLog.d(LOG_TAG, "StkAppService - SET_UP_MENU radio_on[" + radio_on + "]");
            /*For OP02 spec v4.1 start*/
            if (optr != null && "OP02".equals(optr)) {
                int simState = TelephonyManager.getDefault().getSimState(slotId);
                CatLog.d(LOG_TAG, " checkSimRadioState: " + checkSimRadioState(mContext, slotId));
                sim_locked = (TelephonyManager.SIM_STATE_PIN_REQUIRED == simState || TelephonyManager.SIM_STATE_PUK_REQUIRED == simState) ? true : false;
                CatLog.d(LOG_TAG, "StkAppService - simState[" + simState + "][" + sim_locked + "]");
                if (cmdMsg.getMenu().getSetUpMenuFlag() == 1) {
                    if (!removeMenu(slotId)) {
                        CatLog.d(LOG_TAG, "Set property : service.cat.install.on");
                        SystemProperties.set(
                            INSTALL_STK_IF_FLIGHT_MODE_ON_PROPERTY[slotId], "1");
                    }
                }
            }/*For OP02 spec v4.1 end*/

            if (removeMenu(slotId)) {
                int i = 0;
                CatLog.d(LOG_TAG, "removeMenu() - Uninstall App");
                mStkContext[slotId].mCurrentMenu = null;
                mStkContext[slotId].mMainCmd = null;
                mStkContext[slotId].mSetupMenuCalled = false;
                appInstaller.unInstall(mContext, slotId);
                StkAvailable(slotId, STK_AVAIL_NOT_AVAILABLE);
                //Check other setup menu state. If all setup menu are removed, uninstall apk.
                for (i = PhoneConstants.SIM_ID_1; i < mSimCount; i++) {
                    CatLog.d(LOG_TAG, "slot: " + i + " , setup menu state: " +
                            mStkContext[slotId].mSetupMenuState);
                    if (i != slotId
                            && (mStkContext[slotId].mSetupMenuState == STATE_UNKNOWN
                            || mStkContext[slotId].mSetupMenuState == STATE_EXIST)) {
                        CatLog.d(LOG_TAG, "Do not uninstall App.");
                        break;
                    }
                }
                //Uninstall stk menu list launcher.
                if (i == mSimCount) {
                    CatLog.d(LOG_TAG, "All Stk menu are removed.");
                    //appInstaller.unInstall(mContext);
                }
            } else if (!radio_on || sim_locked) {
                CatLog.d(LOG_TAG, "StkAppService - SET_UP_MENU - install App - radio_on[" + radio_on + "]");
                appInstaller.unInstall(mContext, slotId);
                StkAvailable(slotId, STK_AVAIL_NOT_AVAILABLE);
            } else {
                CatLog.d(LOG_TAG, "install App");
                appInstaller.install(mContext, slotId);
                StkAvailable(slotId, STK_AVAIL_AVAILABLE);
            }
            if (mStkContext[slotId].mMenuIsVisible) {
                launchMenuActivity(null, slotId);
            }
            // MTK_OP03_PROTECT_START
            if (slotId == PhoneConstants.SIM_ID_1) {
                ICatServiceExt catServiceExt = null;
                try {
                    catServiceExt = MPlugin.createInstance(ICatServiceExt.class.getName(), mContext);
                } catch (Exception e) {
                    CatLog.e(LOG_TAG, "ICatServiceExt: Fail to create plug-in");
                    e.printStackTrace();
                }
                CatLog.d(LOG_TAG, "updateMenuTitleFromEf catServiceExt: " + catServiceExt +
                        " , " + mStkContext[slotId].mCurrentMenu);
                if (catServiceExt != null && mStkContext[slotId].mCurrentMenu != null) {
                    CatLog.d(LOG_TAG, "updateMenuTitleFromEf init");
                    catServiceExt.init(slotId);
                    catServiceExt.updateMenuTitleFromEf(mStkContext[slotId].mCurrentMenu.title);
                }
            }
            // MTK_OP03_PROTECT_END
            break;
        case GET_INPUT:
        case GET_INKEY:
            if (isBusyOnCall() == true) {
                CatLog.d(LOG_TAG, "[Handle Command][GET_INPUT][Can not handle currently]");
                CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                additionalInfo = new byte[1];
                additionalInfo[0] = (byte) 0x02;
                resMsg.setAdditionalInfo(additionalInfo);
                mStkService[slotId].onCmdResponse(resMsg);
                return;
            }
            //clean up previous activity instance.
            //e.g. when stk input times out and get input is coming later.
            cleanUpInstanceStackBySlot(slotId);
            launchInputActivity(slotId);
            break;
        case SET_UP_IDLE_MODE_TEXT:
            waitForUsersResponse = false;
            mStkContext[slotId].mIdleModeTextCmd = mStkContext[slotId].mCurrentCmd;
            TextMessage idleModeText = mStkContext[slotId].mCurrentCmd.geTextMessage();
            if (idleModeText == null) {
                launchIdleText(slotId);
                mStkContext[slotId].mIdleModeTextCmd = null;
            }
            CatLog.d(this, "isScreenIdle: " + isScreenIdle());
            mStkContext[slotId].mCurrentCmd = mStkContext[slotId].mMainCmd;
            if ((mStkContext[slotId].mIdleModeTextCmd != null) && !isScreenIdle()) {
                CatLog.d(this, "set up idle mode");
                launchIdleText(slotId);
            }
            break;
        case SEND_DTMF:
        case SEND_SMS:
        case SEND_SS:
        case SEND_USSD:
            waitForUsersResponse = false;
            launchEventMessage(slotId);
            break;
        case LAUNCH_BROWSER:
            CatLog.d(LOG_TAG, "[Handle Command][LAUNCH_BROWSER]");
            mStkContext[slotId].mBrowserSettings = mStkContext[slotId].mCurrentCmd.getBrowserSettings();
            if ((mStkContext[slotId].mBrowserSettings != null)
                && (isBrowserLaunched(getApplicationContext()) == true)) {
                switch(mStkContext[slotId].mBrowserSettings.mode) {
                    case LAUNCH_IF_NOT_ALREADY_LAUNCHED :
                        CatLog.d(LOG_TAG, "[Handle Command][LAUNCH_BROWSER][Should not launch browser]");
                        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
                        mStkContext[slotId].launchBrowser = false;
                        resMsg.setResultCode(ResultCode.LAUNCH_BROWSER_ERROR);
                        mStkService[slotId].onCmdResponse(resMsg);
                        break;
                    default:
                        launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.geTextMessage(), slotId);
                        break;
                }

            } else {
                launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.geTextMessage(), slotId);
            }
            break;
        case SET_UP_CALL:
            processSetupCall(slotId);
            break;
        case PLAY_TONE:
            launchToneDialog(slotId);
            break;
        // TODO: 6573 supported
        case RUN_AT_COMMAND:
            break;
        case OPEN_CHANNEL:
            processOpenChannel(slotId);
            break;
        case CLOSE_CHANNEL:
        case RECEIVE_DATA:
        case SEND_DATA:
        case GET_CHANNEL_STATUS:
            waitForUsersResponse = false;
            launchEventMessage(slotId);
            break;
        case CALLCTRL_RSP_MSG:
            msg = cmdMsg.geTextMessage();
            CatLog.d(LOG_TAG, "[CALLCTRL_RSP_MSG]: text " + msg.text);
            displayAlphaIcon(msg, slotId);
            waitForUsersResponse = false;
            break;
        }

        if (!waitForUsersResponse) {
            if (mStkContext[slotId].mCmdsQ.size() != 0) {
                callDelayedMsg(slotId);
            } else {
                mStkContext[slotId].mCmdInProgress = false;
            }
        }
    }
    private void displayAlphaIcon(TextMessage msg, int slotId) {
        if (msg == null) {
            CatLog.d(LOG_TAG, "[displayAlphaIcon] msg is null");
            return;
        }
        CatLog.d(LOG_TAG, "launchAlphaIcon - IconSelfExplanatory[" + msg.iconSelfExplanatory + "]"
                                          + "icon[" + msg.icon + "]"
                                          + "text[" + msg.text + "]");
        TextMessage dispTxt = msg;
        correctTextMessage(dispTxt, slotId);
        if (msg.iconSelfExplanatory == true) {
            // only display Icon.
            if (msg.icon != null) {
                showIconToast(msg);
            } else {
                // do nothing.
                CatLog.d(LOG_TAG, "launchAlphaIcon - null icon!");
                return;
            }
        } else {
            // show text & icon.
            if (msg.icon != null) {
                if (msg.text == null || msg.text.length() == 0) {
                    // show Icon only.
                    showIconToast(msg);
                }
                else {
                    showIconAndTextToast(msg);
                }
            } else {
                if (msg.text == null || msg.text.length() == 0) {
                    // do nothing
                    CatLog.d(LOG_TAG, "launchAlphaIcon - null txt!");
                    return;
                } else {
                    showTextToast(msg, slotId);
                }
            }
        }
    }


    private void processOpenChannel(int slotId) {
        CatLog.d(LOG_TAG, "processOpenChannel()+ " + slotId);

        Call.State callState = Call.State.IDLE;
        TextMessage txtMsg = mStkContext[slotId].mCurrentCmd.geTextMessage();
        int subId[] = SubscriptionManager.getSubId(slotId);
        int phoneId = SubscriptionManager.getPhoneId(subId[0]);
        Phone myPhone = PhoneFactory.getPhone(phoneId);
        //int nt = TelephonyManager.getDefault().getNetworkType(subId[0]);

        if (myPhone == null) {
            CatLog.d("CatService", "myPhone is null");
        } else {
            if (myPhone.getServiceState().getNetworkType() <= TelephonyManager.NETWORK_TYPE_EDGE) {
                callState = getCallState(slotId);
            }
        }

        switch(callState) {
        case IDLE:
        case DISCONNECTED:
            if ((null != txtMsg.text) && (0 != txtMsg.text.length())) {
                /* Alpha identifier with data object */
                launchConfirmationDialog(txtMsg, slotId);
            } else {
                 /* Alpha identifier with null data object
                Chap 6.4.27.1 ME should not give any information to the user or ask for user confirmation */
                processNormalOpenChannelResponse(slotId);
            }
            break;

        default:
            CatLog.d(LOG_TAG, "processOpenChannel() Abnormal OpenChannel Response");
            processAbnormalOpenChannelResponse(slotId);
            break;
        }

        CatLog.d(LOG_TAG, "processOpenChannel()-");
    }


    private void processOpenChannelResponse(int slotId) {
        CatLog.d(LOG_TAG, "processOpenChannelResponse()+ " + slotId);
/*
        int iChannelType = 0;
        BearerDesc iBearerDesc = mStkContext[slotId].mCurrentCmd.getBearerDesc();
        if (iBearerDesc == null) {
            iChannelType = 2;
        } else {
            iChannelType = iBearerDesc.bearerType;
        }
        switch (iChannelType) {
            case 1: //Open Channel related to CS Bearer
                processNormalOpenChannelResponse(slotId);
                break;

            case 2: //Open Channel related to packet data service Bearer
                processNormalOpenChannelResponse(slotId);
                break;

            case 3: //Open Channel related to local Bearer
                processNormalOpenChannelResponse(slotId);
                break;

            case 4: //Open Channel related to default(Network) Bearer
                processNormalOpenChannelResponse(slotId);
                break;

            case 5: //Open Channel related to UICC Server Mode
                processNormalOpenChannelResponse(slotId);
                break;

            default: //Error!
                CatLog.d(LOG_TAG, "processOpenChannelResponse() Error channel type[" +
                        iChannelType + "]");
                processAbnormalOpenChannelResponse(slotId);  // TODO: To check
                break;
        }
*/
        CatLog.d(LOG_TAG, "processOpenChannelResponse()-");

    }

    private void processNormalResponse(int slotId) {
        CatLog.d(LOG_TAG, "Normal Response PROCESS Start, sim id: " + slotId);
        mStkContext[slotId].mCmdInProgress = false;
        if (mStkContext[slotId].mSetupCallInProcess == false) {
            return;
        }
        mStkContext[slotId].mSetupCallInProcess = false;
        if (mStkContext[slotId].mCurrentCmd == null) {
            CatLog.d(LOG_TAG, "Normal Response PROCESS mCurrentCmd changed to null!");
            return;
        }
        if (mStkContext[slotId].mCurrentCmd.getCmdType() != null) {
            CatLog.d(LOG_TAG, "Normal Response PROCESS end! cmdName[" + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        resMsg.setResultCode(ResultCode.OK);
        resMsg.setConfirmation(true);
        launchCallMsg(slotId);
        mStkService[slotId].onCmdResponse(resMsg);
    }

    private void processAbnormalResponse(int slotId) {
        mStkContext[slotId].mCmdInProgress = false;
        CatLog.d(LOG_TAG, "Abnormal Response PROCESS Start");
        if (mStkContext[slotId].mSetupCallInProcess == false) {
            return;
        }
        mStkContext[slotId].mSetupCallInProcess = false;
        CatLog.d(LOG_TAG, "Abnormal Response PROCESS");
        if (mStkContext[slotId].mCurrentCmd == null) {
            return;
        }
        if (mStkContext[slotId].mCurrentCmd.getCmdType() != null) {
            CatLog.d(LOG_TAG, "Abnormal Response PROCESS end! cmdName[" + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        resMsg.setResultCode(ResultCode.NETWORK_CRNTLY_UNABLE_TO_PROCESS);
        mStkService[slotId].onCmdResponse(resMsg);
    }

    private void processAbnormalPhone2BusyResponse(int slotId) {
        mStkContext[slotId].mCmdInProgress = false;
        mStkContext[slotId].mSetupCallInProcess = false;
        CatLog.d(LOG_TAG, "Abnormal No Call Response PROCESS - SIM 2 Call Busy");
        if (mStkContext[slotId].mCurrentCmd == null) {
            return;
        }
        if (mStkContext[slotId].mCurrentCmd.getCmdType() != null) {
            CatLog.d(LOG_TAG, "Abnormal No Call Response PROCESS end - SIM 2 Call Busy! cmdName["
                + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }

        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        resMsg.setResultCode(ResultCode.OK);
        resMsg.setConfirmation(false);
        mStkService[slotId].onCmdResponse(resMsg);
    }

    private void processAbnormalNoCallResponse(int slotId) {
        mStkContext[slotId].mCmdInProgress = false;
        if (mStkContext[slotId].mSetupCallInProcess == false) {
            return;
        }
        mStkContext[slotId].mSetupCallInProcess = false;
        CatLog.d(LOG_TAG, "Abnormal No Call Response PROCESS");
        if (mStkContext[slotId].mCurrentCmd == null) {
            return;
        }
        if (mStkContext[slotId].mCurrentCmd.getCmdType() != null) {
            CatLog.d(LOG_TAG, "Abnormal No Call Response PROCESS end! cmdName[" + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
        mStkService[slotId].onCmdResponse(resMsg);
    }

    private void processNormalOpenChannelResponse(int slotId) {
        CatLog.d(LOG_TAG, "Normal OpenChannel Response PROCESS Start");

        mStkContext[slotId].mCmdInProgress = false;
        if (mStkContext[slotId].mCurrentCmd == null) {
            CatLog.d(LOG_TAG, "Normal OpenChannel Response PROCESS mCurrentCmd changed to null!");
            return;
        }

        TextMessage txtMsg = mStkContext[slotId].mCurrentCmd.geTextMessage();
        if (mStkContext[slotId].mCurrentCmd.getCmdType() != null) {
            CatLog.d(LOG_TAG, "Normal OpenChannel Response PROCESS end! cmdName[" + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        resMsg.setResultCode(ResultCode.OK);
        resMsg.setConfirmation(true);
        displayAlphaIcon(txtMsg, slotId);
        if (mStkService != null && mStkService[slotId] != null) {
            mStkService[slotId].onCmdResponse(resMsg);
        } else {
            CatLog.e(LOG_TAG, "mStkService is null!!");
        }
    }

    private void processAbnormalOpenChannelResponse(int slotId) {
        mStkContext[slotId].mCmdInProgress = false;
        CatLog.d(LOG_TAG, "Abnormal OpenChannel Response PROCESS");
        if (mStkContext[slotId].mCurrentCmd == null) {
            return;
        }
        if (mStkContext[slotId].mCurrentCmd.getCmdType() != null) {
            CatLog.d(LOG_TAG, "Abnormal OpenChannel Response PROCESS end! cmdName[" + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
        if (mStkService != null && mStkService[slotId] != null) {
            mStkService[slotId].onCmdResponse(resMsg);
        } else {
            CatLog.e(LOG_TAG, "mStkService is null!!");
        }
    }

    private void processNormalEndCallResponse(int slotId) {
        CatLog.d(LOG_TAG, "END CALL PROCESS");
        processNormalResponse(slotId);
    }

    private void processNormalHoldCallResponse(int slotId) {
        CatLog.d(LOG_TAG, "HOLD CALL PROCESS");
        processNormalResponse(slotId);
    }

    private void processAbnormalEndCallResponse(int slotId) {
       CatLog.d(LOG_TAG, "End Abnormal CALL PROCESS");
        processAbnormalResponse(slotId);
    }

    private void processAbnormalHoldCallResponse(int slotId) {
        CatLog.d(LOG_TAG, "HOLD Abnormal CALL PROCESS");
        processAbnormalResponse(slotId);
    }

    private boolean isReadyToCallConnected(Call.State state) {
        boolean ret = false;
        if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            switch(state) {
                case IDLE:
                case DIALING:
                case ALERTING:
                case INCOMING:
                case WAITING:
                    ret = true;
                    break;
                default:
                    ret = false;
                    break;
            }
        }
        return ret;
    }
    private void processPhoneStateChanged(int slotId) {
        CatLog.d(LOG_TAG, " PHONE_STATE_CHANGED: " + slotId);
        /* TODO: Gemini and non-Gemini are different begine */
        Phone phone = getPhone(slotId);
        if (phone == null) {
            CatLog.d(LOG_TAG, "processPhoneStateChanged, phone is null.");
            return;
        }

        Call fg = phone.getForegroundCall();
        Call bg = phone.getBackgroundCall();
        Call.State state = null;
        /* TODO: Gemini and non-Gemini are different end */
        if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            Call.State tmpPreCallState = null;
            Call.State tmpPreBgCallState = null;

            if (PhoneConstants.SIM_ID_1 == slotId) {
                tmpPreCallState = mPreCallState;
                tmpPreBgCallState = mPreBgCallState;
            } else if(PhoneConstants.SIM_ID_2 == slotId) {
                tmpPreCallState = mPreCallState2;
                tmpPreBgCallState = mPreBgCallState2;
            } else if(PhoneConstants.SIM_ID_3 == slotId) {
                tmpPreCallState = mPreCallState3;
                tmpPreBgCallState = mPreBgCallState3;
            } else {
                tmpPreCallState = mPreCallState4;
                tmpPreBgCallState = mPreBgCallState4;
            }
            if (fg != null) {
                state = fg.getState();
                CatLog.d(LOG_TAG, "processPhoneStateChanged fg state -> " + tmpPreCallState + "," + state);
                if (Call.State.ACTIVE == state && true == isReadyToCallConnected(tmpPreCallState)) {
                    CatLog.d(LOG_TAG, "IDLE -> ACTIVE");
                    SendEventDownloadMsg(EVDL_ID_CALL_CONNECTED, slotId);
                }
                if (tmpPreCallState != Call.State.DISCONNECTED && Call.State.DISCONNECTED == state) {
                    CatLog.d(LOG_TAG, "FG: mInCallUIState: " + mInCallUIState);
                    /*
                    if (false == mInCallUIState) {
                        mStkContext[slotId].mIsPendingCallDisconnectEvent = false;
                        SendEventDownloadMsg(EVDL_ID_CALL_DISCONNECTED, slotId);
                    } else {
                        mStkContext[slotId].mIsPendingCallDisconnectEvent = true;
                    }*/
                    //Send delay message 8 seconds to wait UI.
                    //Message msg1 = mServiceHandler.obtainMessage(OP_EVDL_CALL_DISCONN_TIMEOUT);
                    //msg1.arg1 = OP_EVDL_CALL_DISCONN_TIMEOUT;
                    //msg1.arg2 = slotId;
                    //mServiceHandler.sendMessageDelayed(msg1,AP_EVDL_TIMEOUT);
                }
                if (PhoneConstants.SIM_ID_1 == slotId) {
                    mPreCallState = state;
                } else if(PhoneConstants.SIM_ID_2 == slotId) {
                    mPreCallState2 = state;
                } else if(PhoneConstants.SIM_ID_3 == slotId) {
                    mPreCallState3 = state;
                } else {
                    mPreCallState4 = state;
                }
            }
            if (bg != null) {
                state = bg.getState();
                CatLog.d(LOG_TAG, "processPhoneStateChanged bg state -> " + mPreBgCallState + "," + state);
                if (mPreBgCallState != Call.State.DISCONNECTED && Call.State.DISCONNECTED == state) {
                    CatLog.d(LOG_TAG, "BG: mInCallUIState: " + mInCallUIState);
                    /*
                    if (false == mInCallUIState) {
                        mStkContext[slotId].mIsPendingCallDisconnectEvent = false;
                        SendEventDownloadMsg(EVDL_ID_CALL_DISCONNECTED, slotId);
                    } else {
                        mStkContext[slotId].mIsPendingCallDisconnectEvent = true;
                    }*/
                    //Send delay message 8 seconds to wait UI.
                    //Message msg1 = mServiceHandler.obtainMessage(OP_EVDL_CALL_DISCONN_TIMEOUT);
                    //msg1.arg1 = OP_EVDL_CALL_DISCONN_TIMEOUT;
                    //msg1.arg2 = slotId;
                    //mServiceHandler.sendMessageDelayed(msg1,AP_EVDL_TIMEOUT);
                }
                if (PhoneConstants.SIM_ID_1 == slotId) {
                    mPreBgCallState = state;
                } else if(PhoneConstants.SIM_ID_2 == slotId) {
                    mPreBgCallState2 = state;
                } else if(PhoneConstants.SIM_ID_3 == slotId) {
                    mPreBgCallState3 = state;
                } else {
                    mPreBgCallState4 = state;
                }
            }
        }
        if (mStkContext[slotId].mSetupCallInProcess == false) {
            CatLog.d(LOG_TAG, " PHONE_STATE_CHANGED: setup in process is false");
            return;
        }
        CatLog.d(LOG_TAG, " PHONE_STATE_CHANGED: setup in process is true");
        // Setup call In Process.
        if (mStkContext[slotId].mCurrentCmd != null) {
            // Set up call
            switch (mStkContext[slotId].mCurrentCmd.getCmdType()) {
            case SET_UP_CALL:
                int cmdQualifier = mStkContext[slotId].mCurrentCmd.getCmdQualifier();
                // Call fg = mPhone.getForegroundCall();
                if (fg != null) {
                    state = fg.getState();
                    CatLog.d(LOG_TAG, " PHONE_STATE_CHANGED to : " + state);
                    switch(state) {
                    case HOLDING:
                        if (cmdQualifier == SETUP_CALL_HOLD_CALL_1 || cmdQualifier == SETUP_CALL_HOLD_CALL_2) {
                            processNormalHoldCallResponse(slotId);
                        }
                        break;
                    case IDLE:
                        if (cmdQualifier == SETUP_CALL_HOLD_CALL_1 ||
                                cmdQualifier == SETUP_CALL_HOLD_CALL_2) {
                            // need process "end call" when hold
                            processNormalHoldCallResponse(slotId);
                        }
                        break;
                    }
                }
                PhoneConstants.State phoneState = PhoneConstants.State.IDLE;
                phoneState = phone.getState();
                CatLog.d(this, "phone state: " + phoneState);
                if (PhoneConstants.State.IDLE == phoneState) {
                    if (cmdQualifier == SETUP_CALL_END_CALL_1 ||
                        cmdQualifier == SETUP_CALL_END_CALL_2) {
                            processNormalEndCallResponse(slotId);
                    }
                }
                break;
            }
        }
        return;
    }

    private void processSuppServiceFailed(AsyncResult r, int slotId) {
        Phone.SuppService service = (Phone.SuppService) r.result;
        CatLog.d(LOG_TAG, "onSuppServiceFailed: " + service + ", sim id: " + slotId);

        int errorMessageResId;
        switch (service) {
            case SWITCH:
                // Attempt to switch foreground and background/incoming calls failed
                // ("Failed to switch calls")
                CatLog.d(LOG_TAG, "Switch failed");
                processAbnormalHoldCallResponse(slotId);
                break;
        }
    }

    private Handler mCallDisConnHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                int slotId = 0;
                CatLog.d(LOG_TAG, "mCallDisConnHandler" + msg.what);
                switch (msg.what) {
                    case PHONE_DISCONNECT:
                        slotId = PhoneConstants.SIM_ID_1;
                        break;
                    case PHONE_DISCONNECT2:
                        slotId = PhoneConstants.SIM_ID_2;
                        break;
                    case PHONE_DISCONNECT3:
                        slotId = PhoneConstants.SIM_ID_3;
                        break;
                    case PHONE_DISCONNECT4:
                        slotId = PhoneConstants.SIM_ID_4;
                        break;
                    default:
                        break;
                }
                CatLog.d(LOG_TAG, "mInCallUIState: " + mInCallUIState);
                if (false == mInCallUIState) {
                    mStkContext[slotId].mIsPendingCallDisconnectEvent = false;
                    SendEventDownloadMsg(EVDL_ID_CALL_DISCONNECTED, slotId);
                } else {
                    mStkContext[slotId].mIsPendingCallDisconnectEvent = true;
                }
                //CatLog.d(LOGTAG, "Send OP_EVDL_CALL_DISCONN_TIMEOUT:" + msg.what);
                //Send delay message 8 seconds to wait UI.
                //Message msg1 = mServiceHandler.obtainMessage(OP_EVDL_CALL_DISCONN_TIMEOUT);
                //msg1.arg1 = OP_EVDL_CALL_DISCONN_TIMEOUT;
                //msg1.arg2 = sim_id;
                //mServiceHandler.sendMessageDelayed(msg1,AP_EVDL_TIMEOUT);
            }
        }
    };

    private Handler mCallHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PHONE_STATE_CHANGED:
                processPhoneStateChanged(PhoneConstants.SIM_ID_1);
                break;
            case SUPP_SERVICE_FAILED:
                processSuppServiceFailed((AsyncResult) msg.obj, PhoneConstants.SIM_ID_1);
                break;
            }
        }
    };

    private Handler mCallHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PHONE_STATE_CHANGED:
                processPhoneStateChanged(PhoneConstants.SIM_ID_2);
                break;
            case SUPP_SERVICE_FAILED:
                processSuppServiceFailed((AsyncResult) msg.obj, PhoneConstants.SIM_ID_2);
                break;
            }
        }
    };

    private Handler mCallHandler3 = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PHONE_STATE_CHANGED:
                processPhoneStateChanged(PhoneConstants.SIM_ID_3);
                break;
            case SUPP_SERVICE_FAILED:
                processSuppServiceFailed((AsyncResult) msg.obj, PhoneConstants.SIM_ID_3);
                break;
            }
        }
    };

    private Handler mCallHandler4 = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PHONE_STATE_CHANGED:
                processPhoneStateChanged(PhoneConstants.SIM_ID_4);
                break;
            case SUPP_SERVICE_FAILED:
                processSuppServiceFailed((AsyncResult) msg.obj, PhoneConstants.SIM_ID_4);
                break;
            }
        }
    };

    private Call.State getCallState(int slotId) {
        // Call fg = mPhone.getForegroundCall();
        CatLog.d(LOG_TAG, "getCallState: " + slotId);
        /* TODO: Gemini and non-Gemini are different begine */
        Phone phone = getPhone(slotId);
        if (phone == null) {
            CatLog.d(LOG_TAG, "getCallState: phone is null.");
            return Call.State.IDLE;
        }
        Call fg = phone.getForegroundCall();
        /* TODO: Gemini and non-Gemini are different end */
        if (fg != null) {
            CatLog.d(LOG_TAG, "ForegroundCall State: " + fg.getState());
            return fg.getState();
        }
        return Call.State.IDLE;
    }

    private Call.State getBackgroundCallState(int slotId) {
        // Call bg = mPhone.getBackgroundCall();
        CatLog.d(LOG_TAG, "getBackgroundCallState: " + slotId);
        /* TODO: Gemini and non-Gemini are different begine */
        Phone phone = getPhone(slotId);
        if (phone == null) {
            CatLog.d(LOG_TAG, "getBackgroundCallState: phone is null.");
            return Call.State.IDLE;
        }
        Call bg = phone.getBackgroundCall();

        /* TODO: Gemini and non-Gemini are different end */
        if (bg != null) {
            CatLog.d(LOG_TAG, "BackgroundCall State: " + bg.getState());
            return bg.getState();
        }
        return Call.State.IDLE;
    }

    private boolean is1A1H(int slotId) {
        Call.State fgState = getCallState(slotId);
        Call.State bgState = getBackgroundCallState(slotId);
        if (fgState != Call.State.IDLE && bgState != Call.State.IDLE) {
            CatLog.d(LOG_TAG, "1A1H");
            return true;
        }
        return false;
    }

    private Call.State isPhoneIdle(int slotId) {
        /* TODO: Gemini and non-Gemini are different begine */
        if (slotId == PhoneConstants.SIM_ID_3 && (SystemProperties.get(
                "ro.mtk_gemini_3sim_support").equals("1") != true || SystemProperties.get(
                "ro.mtk_gemini_4sim_support").equals("1") != true)) {
            CatLog.d(LOG_TAG, "isPhoneIdle(), Does not support SIM3");
            return Call.State.IDLE;
        }
        if (slotId == PhoneConstants.SIM_ID_4 && SystemProperties.get(
                "ro.mtk_gemini_4sim_support").equals("1") != true) {
            CatLog.d(LOG_TAG, "isPhoneIdle(), Does not support SIM4");
            return Call.State.IDLE;
        }
        Phone phone = getPhone(slotId);
        if (phone == null) {
            CatLog.d(LOG_TAG, "isPhoneIdle() : phone is null.");
            return Call.State.IDLE;
        }
        Call fg = phone.getForegroundCall();;
        /* TODO: Gemini and non-Gemini are different end */
        if (fg != null) {
            CatLog.d(LOG_TAG, "isPhoneIdle() Phone" + slotId +
                    " ForegroundCall State: " + fg.getState());
            if ((Call.State.IDLE != fg.getState()) &&
                    (Call.State.DISCONNECTED != fg.getState())) {
                return fg.getState();
            }
        }
        /* TODO: Gemini and non-Gemini are different begine */
        Call bg = phone.getBackgroundCall();
        /* TODO: Gemini and non-Gemini are different end */
        if (bg != null) {
            CatLog.d(LOG_TAG, "isPhoneIdle() Phone" + slotId +
                    " BackgroundCall State: " + bg.getState());
            if (Call.State.IDLE != bg.getState() &&
                    (Call.State.DISCONNECTED != bg.getState())) {
                return bg.getState();
            }
        }
        /* TODO: Gemini and non-Gemini are different begine */
        Call ring = phone.getRingingCall();
        /* TODO: Gemini and non-Gemini are different end */
        if (ring != null) {
            CatLog.d(LOG_TAG, "isPhoneIdle() Phone" + slotId +
                    " RingCall State: " + ring.getState());
            if (Call.State.IDLE != ring.getState() &&
                    (Call.State.DISCONNECTED != ring.getState())) {
                return ring.getState();
            }
        }

        CatLog.d(LOG_TAG, "isPhoneIdle() Phone" + slotId + " State: " + Call.State.IDLE);
        return Call.State.IDLE;
    }

    private void processNoCall(int slotId) {
        // get Call State.
        Call.State callState = getCallState(slotId);
        switch(callState) {
        case IDLE:
        case DISCONNECTED:
            launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.getCallSettings().confirmMsg, slotId);
            break;
        default:
            CatLog.d(LOG_TAG, "Call Abnormal No Call Response");
            processAbnormalNoCallResponse(slotId);
            break;
        }
    }

    private void processHoldCall(int slotId) {
        // Just show the confirm dialog, and add the process when user click OK.
        if (!is1A1H(slotId)) {
            launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.getCallSettings().confirmMsg, slotId);
        } else {
            CatLog.d(LOG_TAG, "Call Abnormal Hold Call Response(has 1A1H calls)");
            processAbnormalNoCallResponse(slotId);
        }
    }

    private void processEndCall(int slotId) {
        // Just show the confirm dialog, and add the process when user click OK.
        launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.getCallSettings().confirmMsg, slotId);
    }

    private void processSetupCall(int slotId) {
        CatLog.d(LOG_TAG, "processSetupCall, sim id: " + slotId);
        int i = 0;
        boolean state_idle = true;
        boolean isDualTalkMode = isSupportDualTalk();
        CatLog.d(LOG_TAG, "isDualTalkMode: " + isDualTalkMode);
        if (true == SystemProperties.get("ro.mtk_gemini_support").equals("1") && !isDualTalkMode) {
            for (i = 0; i < mSimCount; i++) {
                if ((i != slotId) && (Call.State.IDLE != isPhoneIdle(i))) {
                    state_idle = false;
                    processAbnormalPhone2BusyResponse(slotId);
                    CatLog.d(LOG_TAG, "The other sim is not idle, sim id: " + i);
                    break;
                }
            }
        } else {
            CatLog.d(LOG_TAG, "This is dual talk mode");
        }
        if (state_idle) {
            // get callback.
            mStkContext[slotId].mSetupCallInProcess = true;
            int cmdQualifier = mStkContext[slotId].mCurrentCmd.getCmdQualifier();
            CatLog.d(LOG_TAG, "Qualifier code is " + cmdQualifier);
            switch(cmdQualifier) {
            case SETUP_CALL_NO_CALL_1:
            case SETUP_CALL_NO_CALL_2:
                processNoCall(slotId);
                break;
            case SETUP_CALL_HOLD_CALL_1:
            case SETUP_CALL_HOLD_CALL_2:
                processHoldCall(slotId);
                break;
            case SETUP_CALL_END_CALL_1:
            case SETUP_CALL_END_CALL_2:
                processEndCall(slotId);
                break;
            }
        }
    }

    private void processHoldCallResponse(int slotId) {
        // get Call State.
        Call.State callState = getCallState(slotId);
        CatLog.d(LOG_TAG, "processHoldCallResponse callState[" + callState + "], sim id: " + slotId);

        switch(callState) {
        case IDLE:
        case HOLDING:
            processNormalResponse(slotId);
            CatLog.d(LOG_TAG, "processHoldCallResponse in Idle or HOLDING");
            break;
        case ACTIVE:
            CatLog.d(LOG_TAG, "processHoldCallResponse in Active ");
            try {
                CatLog.d(LOG_TAG, "switchHoldingAndActive");
                // mPhone.switchHoldingAndActive();
                /* TODO: Gemini and non-Gemini are different begine */
                Phone phone = getPhone(slotId);
                if (phone != null) {
                    phone.switchHoldingAndActive();
                }
                /* TODO: Gemini and non-Gemini are different end */
            } catch (CallStateException ex) {
                CatLog.d(LOG_TAG, " Error: switchHoldingAndActive: caught " + ex);
                processAbnormalResponse(slotId);
            }
            break;
        default:
            CatLog.d(LOG_TAG, "processHoldCallResponse in other state");
            processAbnormalResponse(slotId);
            break;
        }
        return;
    }

    private void processEndCallResponse(int slotId) {
        // get Call State.
        Call.State callState = getCallState(slotId);
        CatLog.d(LOG_TAG, "call State  = " + callState + " ,sim id" + slotId);
        switch(callState) {
        case IDLE:
            processNormalResponse(slotId);
            break;
            // other state
        default:
            // End call
            CatLog.d(LOG_TAG, "End call");
            // 1A1H call
            if (is1A1H(slotId)) {
                try {
                    // mPhone.hangupAll();
                    /* TODO: Gemini and non-Gemini are different begine */
                    Phone phone = getPhone(slotId);
                    if (phone == null) {
                        CatLog.d(LOG_TAG, "ERROR: phone is null.");
                        break;
                    }
                    phone.hangupAll();
                    /* TODO: Gemini and non-Gemini are different end */
                } catch (Exception ex) {
                    CatLog.d(LOG_TAG, " Error: Call hangup: caught " + ex);
                    processAbnormalResponse(slotId);
                }
            } else {
                // Call fg = mPhone.getForegroundCall();
                /* TODO: Gemini and non-Gemini are different begine */
                Phone phone = getPhone(slotId);
                if (phone == null) {
                    CatLog.d(LOG_TAG, "ERROR: phone is null.");
                    break;
                }
                Call fg = phone.getForegroundCall();
                /* TODO: Gemini and non-Gemini are different end */
                if (fg != null) {
                    try {
                        CatLog.d(LOG_TAG, "End call  " + callState);
                        fg.hangup();
                    } catch (CallStateException ex) {
                        CatLog.d(LOG_TAG, " Error: Call hangup: caught " + ex);
                        // TODO
                        processAbnormalResponse(slotId);
                    }
                }
            }
            CatLog.d(LOG_TAG, "call Not IDLE  = " + callState);
            break;
        }
    }

    private void processSetupCallResponse(int slotId) {
        CatLog.d(LOG_TAG, "processSetupCallResponse(), sim id: " + slotId);
        int cmdQualifier = mStkContext[slotId].mCurrentCmd.getCmdQualifier();
        CatLog.d(LOG_TAG, "processSetupCallResponse() - cmdQualifier[" + cmdQualifier + "]");

        switch (cmdQualifier) {
        case SETUP_CALL_NO_CALL_1:
        case SETUP_CALL_NO_CALL_2:
            //TODO
            processNormalResponse(slotId);
            break;
        case SETUP_CALL_HOLD_CALL_1:
        case SETUP_CALL_HOLD_CALL_2:
            processHoldCallResponse(slotId);
            break;
        case SETUP_CALL_END_CALL_1:
        case SETUP_CALL_END_CALL_2:
            processEndCallResponse(slotId);
            break;
        }
    }
    // End Setup Call

    private void handleEventDownload(Bundle args, int slotId) {
        int eventId = args.getInt(EVDL_ID);
        int sourceId = 0;
        int destinationId = 0;
        byte[] additionalInfo = null;
        byte[] language;
        boolean oneShot = false;
        String languageInfo;

        CatResponseMessage resMsg = new CatResponseMessage(eventId);
        switch(eventId) {
        case EVDL_ID_USER_ACTIVITY:
            sourceId = DEV_ID_TERMINAL;
            destinationId = DEV_ID_UICC;
            oneShot = true;
            break;
        case EVDL_ID_IDLE_SCREEN_AVAILABLE:
            sourceId = DEV_ID_DISPLAY;
            destinationId = DEV_ID_UICC;
            oneShot = true;
            break;
        case EVDL_ID_LANGUAGE_SELECT:
            sourceId = DEV_ID_TERMINAL;
            destinationId = DEV_ID_UICC;
            additionalInfo = new byte[4];
            //language tag
            additionalInfo[0] = (byte) 0xAD;
            //language code, defined in ISO639,coded in GSM 7-bit ex. Emglish -> en -> 0x65 0x6E
            languageInfo = Locale.getDefault().getLanguage();
            additionalInfo[1] = 0x02;
            language = languageInfo.getBytes();
            additionalInfo[2] = language[0];
            additionalInfo[3] = language[1];

            oneShot = false;
            break;
        case EVDL_ID_BROWSER_TERMINATION:
            sourceId = DEV_ID_TERMINAL;
            destinationId = DEV_ID_UICC;
            //browser termination cause tag
            additionalInfo = new byte[3];
            additionalInfo[0] = (byte) 0xB4;
            additionalInfo[1] = 0x01;
            additionalInfo[2] = 0x00;
            oneShot = false;
            break;
        case EVDL_ID_CALL_DISCONNECTED:
            oneShot = false;
            break;
        case EVDL_ID_BROWSING_STATUS:
            sourceId = DEV_ID_TERMINAL;
            destinationId = DEV_ID_UICC;
            //browsing status tag
            additionalInfo = new byte[4];
            additionalInfo[0] = (byte) 0xE4;
            additionalInfo[1] = 0x02;
            //0x0194 = 404, Not found.
            additionalInfo[2] = (byte) 0x01;
            additionalInfo[3] = (byte) 0x94;
            oneShot = false;
            break;
        default:
            break;
        }
        resMsg.setSourceId(sourceId);
        resMsg.setDestinationId(destinationId);
        resMsg.setAdditionalInfo(additionalInfo);
        resMsg.setOneShot(oneShot);
        CatLog.d(LOG_TAG, "onEventDownload - eventId[" + eventId + "], sim id: " + slotId);
        if (slotId >= 0 && slotId < mSimCount) {
            try {
                mStkService[slotId].onEventDownload(resMsg);
            } catch (NullPointerException e) {
                CatLog.d(LOG_TAG, "mStkService is null, sim: " + slotId);
            }
        } else if (slotId == STK_GEMINI_BROADCAST_ALL) {
            int i = 0;
            for (i = 0; i < mSimCount; i++) {
                if (mStkService[i] != null)
                    mStkService[i].onEventDownload(resMsg);
            }
        }
    }
    private void handleCmdResponse(Bundle args, int slotId) {
        CatLog.d(LOG_TAG, "handleCmdResponse, sim id: " + slotId);
        if (mStkContext[slotId].mCurrentCmd == null) {
            return;
        }

        if (mStkService[slotId] == null) {
            mStkService[slotId] = com.android.internal.telephony.cat.CatService.getInstance(slotId);
            if (mStkService[slotId] == null) {
                // This should never happen (we should be responding only to a message
                // that arrived from StkService). It has to exist by this time
                CatLog.e(LOG_TAG, "Exception! mStkService is null when we need to send response.");
                //throw new RuntimeException("mStkService is null when we need to send response");
                return;
            }
        }

        boolean skip_timeout = false;
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);
        if (null != mStkContext[slotId].mCurrentCmd && null != mStkContext[slotId].mCurrentCmd.getCmdType()) {
            CatLog.d(LOG_TAG, "handleCmdResponse+ cmdName[" + mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
            if (mStkContext[slotId].mCurrentCmd.getCmdType() == AppInterface.CommandType.SEND_DATA ||
                mStkContext[slotId].mCurrentCmd.getCmdType() == AppInterface.CommandType.RECEIVE_DATA ||
                mStkContext[slotId].mCurrentCmd.getCmdType() == AppInterface.CommandType.CLOSE_CHANNEL ||
                mStkContext[slotId].mCurrentCmd.getCmdType() == AppInterface.CommandType.SET_UP_MENU) {
                skip_timeout = true;
            }
        }

        // set result code
        boolean helpRequired = args.getBoolean(HELP, false);
        boolean confirmed = false;

        switch(args.getInt(RES_ID)) {
        case RES_ID_MENU_SELECTION:
            if (null == mStkContext[slotId].mCurrentMenuCmd) {
                CatLog.d(LOG_TAG, "mCurrentMenuCmd == null");
                return;
            }
            CatLog.d(LOG_TAG, "MENU_SELECTION = " + mStkContext[slotId].mCurrentMenuCmd.getCmdType());
            if (isBipCommand(mStkContext[slotId].mCurrentCmd)) {
                Toast toast = Toast.makeText(mContext.getApplicationContext(), R.string.lable_busy_on_bip, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 0);
                toast.show();
                return;
            }

            int menuSelection = args.getInt(MENU_SELECTION);
            switch(mStkContext[slotId].mCurrentMenuCmd.getCmdType()) {
            case SET_UP_MENU:
                //have already handled setup menu
                mStkContext[slotId].mSetUpMenuHandled = true;
            case SELECT_ITEM:
                resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentMenuCmd);
                mStkContext[slotId].lastSelectedItem = getItemName(menuSelection, slotId);
                if (helpRequired) {
                    resMsg.setResultCode(ResultCode.HELP_INFO_REQUIRED);
                } else {
                    resMsg.setResultCode(ResultCode.OK);
                }
                resMsg.setMenuSelection(menuSelection);
                break;
            }
            break;
        case RES_ID_INPUT:
            CatLog.d(LOG_TAG, "RES_ID_INPUT");
            String input = args.getString(INPUT);
            if (input != null && (null != mStkContext[slotId].mCurrentCmd.geInput()) &&
                    (mStkContext[slotId].mCurrentCmd.geInput().yesNo)) {
                boolean yesNoSelection = input
                        .equals(StkInputActivity.YES_STR_RESPONSE);
                resMsg.setYesNo(yesNoSelection);
            } else {
                if (helpRequired) {
                    resMsg.setResultCode(ResultCode.HELP_INFO_REQUIRED);
                } else {
                    resMsg.setResultCode(ResultCode.OK);
                    resMsg.setInput(input);
                }
            }
            break;
        case RES_ID_CONFIRM:
            CatLog.d(this, "RES_ID_CONFIRM");
            confirmed = args.getBoolean(CONFIRMATION);
            switch (mStkContext[slotId].mCurrentCmd.getCmdType()) {
            case SET_UP_MENU:
                CatLog.d(LOG_TAG, "RES_ID_CONFIRM SET_UP_MENU");
                return;
            case DISPLAY_TEXT:
                resMsg.setResultCode(confirmed ? ResultCode.OK
                        : ResultCode.UICC_SESSION_TERM_BY_USER);
                break;
            case LAUNCH_BROWSER:
                resMsg.setResultCode(confirmed ? ResultCode.OK
                        : ResultCode.UICC_SESSION_TERM_BY_USER);
                if (confirmed) {
                    mStkContext[slotId].launchBrowser = true;
                    mStkContext[slotId].mBrowserSettings =
                            mStkContext[slotId].mCurrentCmd.getBrowserSettings();
                }
                break;
            case SET_UP_CALL:
                if (confirmed) {
                    processSetupCallResponse(slotId);
                    return;
                }
                // Cancel
                mStkContext[slotId].mSetupCallInProcess = false;
                resMsg.setResultCode(ResultCode.OK);
                resMsg.setConfirmation(confirmed);

/* for L stk call.
                if (!confirmed) {
                    // Cancel
                    mStkContext[slotId].mSetupCallInProcess = false;
                    resMsg.setResultCode(ResultCode.OK);
                    resMsg.setConfirmation(confirmed);
                } else {
                    CatLog.d(LOG_TAG, "Wait for CC confirm.");
                    // May send a timer for the error handling if we do not receive intent from CC.
                    return;
                }
*/
                break;
            case OPEN_CHANNEL:
                if (confirmed) {
                    //We do no need to check bearer here. Move the logic to BisService
                    //if the bearer we do not support for M.
                    //processOpenChannelResponse(slotId);
                    processNormalOpenChannelResponse(slotId);
                    return;
                }
                // Cancel
                resMsg.setResultCode(ResultCode.USER_NOT_ACCEPT);
                resMsg.setConfirmation(confirmed);
                break;
            }
            break;
        case RES_ID_DONE:
            resMsg.setResultCode(ResultCode.OK);
            break;
        case RES_ID_BACKWARD:
            CatLog.d(LOG_TAG, "RES_ID_BACKWARD");
            switch (mStkContext[slotId].mCurrentCmd.getCmdType()) {
                case OPEN_CHANNEL:
                    CatLog.d(LOG_TAG, "RES_ID_BACKWARD - OPEN_CHANNEL");
                    resMsg.setResultCode(ResultCode.UICC_SESSION_TERM_BY_USER);
                    break;

                default:
                    CatLog.d(LOG_TAG, "RES_ID_BACKWARD - not OPEN_CHANNEL");
                    resMsg.setResultCode(ResultCode.BACKWARD_MOVE_BY_USER);
                    break;
            }
            break;
        case RES_ID_END_SESSION:
            CatLog.d(LOG_TAG, "RES_ID_END_SESSION");
            resMsg.setResultCode(ResultCode.UICC_SESSION_TERM_BY_USER);
            break;
        case RES_ID_TIMEOUT:
            CatLog.d(LOG_TAG, "RES_ID_TIMEOUT, skip timout: " + skip_timeout);
            // GCF test-case 27.22.4.1.1 Expected Sequence 1.5 (DISPLAY TEXT,
            // Clear message after delay, successful) expects result code OK.
            // If the command qualifier specifies no user response is required
            // then send OK instead of NO_RESPONSE_FROM_USER
            if (!skip_timeout) {
                if ((mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                        AppInterface.CommandType.DISPLAY_TEXT.value())
                        && (mStkContext[slotId].mCurrentCmd.geTextMessage().userClear == false)) {
                    resMsg.setResultCode(ResultCode.OK);
                } else {
                    resMsg.setResultCode(ResultCode.NO_RESPONSE_FROM_USER);
                }
            } else {
                CatLog.d(LOG_TAG, "Skip timeout because the command is SEND_DATA");
            }
            break;
        case RES_ID_CHOICE:
            int choice = args.getInt(CHOICE);
            CatLog.d(this, "User Choice=" + choice);
            switch (choice) {
                case YES:
                    resMsg.setResultCode(ResultCode.OK);
                    confirmed = true;
                    break;
                case NO:
                    resMsg.setResultCode(ResultCode.USER_NOT_ACCEPT);
                    break;
            }

            if (mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                    AppInterface.CommandType.OPEN_CHANNEL.value()) {
                resMsg.setConfirmation(confirmed);
            }
            break;

        default:
            CatLog.d(LOG_TAG, "Unknown result id");
            return;
        }

        if (null != mStkContext[slotId].mCurrentCmd &&
                null != mStkContext[slotId].mCurrentCmd.getCmdType()) {
            CatLog.d(LOG_TAG, "handleCmdResponse- cmdName[" +
                    mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        mStkService[slotId].onCmdResponse(resMsg);
        //reset current command.
        //mStkContext[slotId].mCurrentCmd = null;
    }

    /**
     * Returns 0 or FLAG_ACTIVITY_NO_USER_ACTION, 0 means the user initiated the action.
     *
     * @param userAction If the userAction is yes then we always return 0 otherwise
     * mMenuIsVisible is used to determine what to return. If mMenuIsVisible is true
     * then we are the foreground app and we'll return 0 as from our perspective a
     * user action did cause. If it's false than we aren't the foreground app and
     * FLAG_ACTIVITY_NO_USER_ACTION is returned.
     *
     * @return 0 or FLAG_ACTIVITY_NO_USER_ACTION
     */
    private int getFlagActivityNoUserAction(InitiatedByUserAction userAction, int slotId) {
        return ((userAction == InitiatedByUserAction.yes) | mStkContext[slotId].mMenuIsVisible)
                ? 0 : Intent.FLAG_ACTIVITY_NO_USER_ACTION;
    }
    /**
     * This method is used for cleaning up pending instances in stack.
     */
    private void cleanUpInstanceStackBySlot(int slotId) {
        Activity activity = mStkContext[slotId].getPendingActivityInstance();
        Activity dialog = mStkContext[slotId].getPendingDialogInstance();
        CatLog.d(LOG_TAG, "cleanUpInstanceStackBySlot slotId: " + slotId);
        if (mStkContext[slotId].mCurrentCmd == null) {
            CatLog.d(LOG_TAG, "current cmd is null.");
            return;
        }
        if (activity != null) {
            CatLog.d(LOG_TAG, "current cmd type: " +
                 mStkContext[slotId].mCurrentCmd.getCmdType());
            if (mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                    AppInterface.CommandType.GET_INPUT.value() ||
                    mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                    AppInterface.CommandType.GET_INKEY.value()) {
                indicatePendingInput(true, slotId);
            } else if (mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                    AppInterface.CommandType.SET_UP_MENU.value() ||
                    mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                    AppInterface.CommandType.SELECT_ITEM.value()) {
                indicatePendingMenu(true, slotId);
            } else {
            }
            CatLog.d(LOG_TAG, "finish pending activity.");
            activity.finish();
            mStkContext[slotId].mActivityInstance = null;
        }
        if (dialog != null) {
            CatLog.d(LOG_TAG, "finish pending dialog.");
            indicatePendingDialog(true, slotId);
            dialog.finish();
            mStkContext[slotId].mDialogInstance = null;
        }
    }
    /**
     * This method is used for restoring pending instances from stack.
     */
    private void restoreInstanceFromStackBySlot(int slotId) {
        if (null == mStkContext || null == mStkContext[slotId].mCurrentCmd) {
            CatLog.d(LOG_TAG, "Null mStkContext / mCurrentCmd : " + mStkContext);
            return;
        }
        AppInterface.CommandType cmdType = mStkContext[slotId].mCurrentCmd.getCmdType();

        CatLog.d(LOG_TAG, "restoreInstanceFromStackBySlot cmdType : " + cmdType);
        switch(cmdType) {
            case GET_INPUT:
            case GET_INKEY:
                launchInputActivity(slotId);
                //Set mMenuIsVisible to true for showing main menu for
                //following session end command.
                mStkContext[slotId].mMenuIsVisible = true;
            break;
            case DISPLAY_TEXT:
                launchTextDialog(slotId);
            break;
            case LAUNCH_BROWSER:
            case OPEN_CHANNEL:
                launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.geTextMessage(), slotId);
            break;
            case SET_UP_CALL:
                launchConfirmationDialog(mStkContext[slotId].mCurrentCmd.getCallSettings().
                        confirmMsg, slotId);
            break;
            case SET_UP_MENU:
            case SELECT_ITEM:
                launchMenuActivity(null, slotId);
            break;
        default:
            break;
        }
    }

    private void launchMenuActivity(Menu menu, int slotId) {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String targetActivity = STK_MENU_ACTIVITY_NAME;
        String uriString = STK_MENU_URI + System.currentTimeMillis();
        //Set unique URI to create a new instance of activity for different slotId.
        Uri uriData = Uri.parse(uriString);

        CatLog.d(LOG_TAG, "launchMenuActivity, slotId: " + slotId + " , " +
                uriData.toString() + " , " + mStkContext[slotId].mOpCode + ", "
                + mStkContext[slotId].mMenuState);
        newIntent.setClassName(PACKAGE_NAME, targetActivity);
        int intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK;

        if (menu == null) {
            // We assume this was initiated by the user pressing the tool kit icon
            intentFlags |= getFlagActivityNoUserAction(InitiatedByUserAction.yes, slotId);
            if (mStkContext[slotId].mOpCode == OP_END_SESSION) {
                CatLog.d(LOG_TAG, "launchMenuActivity, return OP_END_SESSION");
                mStkContext[slotId].mMenuState = StkMenuActivity.STATE_MAIN;
                if (mStkContext[slotId].mMainActivityInstance != null) {
                    CatLog.d(LOG_TAG, "launchMenuActivity, mMainActivityInstance is not null");
                    return;
                }
                if (mStkContext[slotId].mBackGroundTRSent) {
                    CatLog.d(LOG_TAG, "launchMenuActivity, ES is triggered by BG.");
                    mStkContext[slotId].mBackGroundTRSent = false;
                    return;
                }
            }

            //If the last pending menu is secondary menu, "STATE" should be "STATE_SECONDARY".
            //Otherwise, it should be "STATE_MAIN".
            if (mStkContext[slotId].mOpCode == OP_LAUNCH_APP &&
                    mStkContext[slotId].mMenuState == StkMenuActivity.STATE_SECONDARY) {
                CatLog.d(LOG_TAG, "launchMenuActivity, STATE_SECONDARY");
                newIntent.putExtra("STATE", StkMenuActivity.STATE_SECONDARY);
            } else {
                CatLog.d(LOG_TAG, "mMenuState, set to STATE_MAIN : " + slotId);
                newIntent.putExtra("STATE", StkMenuActivity.STATE_MAIN);
                mStkContext[slotId].mMenuState = StkMenuActivity.STATE_MAIN;
            }
        } else {
            // We don't know and we'll let getFlagActivityNoUserAction decide.
            intentFlags |= getFlagActivityNoUserAction(InitiatedByUserAction.unknown, slotId);
            newIntent.putExtra("STATE", StkMenuActivity.STATE_SECONDARY);
            CatLog.d(LOG_TAG, "mMenuState, set to STATE_SECONDARY : " + slotId);
            mStkContext[slotId].mMenuState = StkMenuActivity.STATE_SECONDARY;
        }
        newIntent.putExtra(SLOT_ID, slotId);
        newIntent.putExtra(STK_SOURCE_KEY, mStkAppSourceKey);
        newIntent.setData(uriData);
        newIntent.setFlags(intentFlags);
        mContext.startActivity(newIntent);
    }

    private void launchInputActivity(int slotId) {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String targetActivity = STK_INPUT_ACTIVITY_NAME;
        String uriString = STK_INPUT_URI + System.currentTimeMillis();
        //Set unique URI to create a new instance of activity for different slotId.
        Uri uriData = Uri.parse(uriString);

        CatLog.d(LOG_TAG, "launchInputActivity, slotId: " + slotId);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | getFlagActivityNoUserAction(InitiatedByUserAction.unknown, slotId));
        newIntent.setClassName(PACKAGE_NAME, targetActivity);
        newIntent.putExtra("INPUT", mStkContext[slotId].mCurrentCmd.geInput());
        newIntent.putExtra(SLOT_ID, slotId);
        newIntent.putExtra(STK_SOURCE_KEY, mStkAppSourceKey);
        newIntent.setData(uriData);
        mContext.startActivity(newIntent);
    }

    private void delayToCheckIdle(int slotId) {
        CatLog.d(LOG_TAG, "delayToCheckIdle, sim id: " + slotId);
        Message msg1 = mServiceHandler.obtainMessage(OP_DELAY_TO_CHECK_IDLE);
        msg1.arg1 = OP_DELAY_TO_CHECK_IDLE;
        msg1.arg2 = slotId;
        mServiceHandler.sendMessageDelayed(msg1, DELAY_TO_CHECK_IDLE_TIME);
    }

    private void delayToCheckIccid(int time) {
        CatLog.d(LOG_TAG, "delayToCheckIccid, mServiceHandler: " + mServiceHandler);
        if(mServiceHandler == null) {
            waitForLooper();
        }
        Message msg1 = mServiceHandler.obtainMessage(OP_DELAY_TO_CHECK_ICCID);
        msg1.arg1 = OP_DELAY_TO_CHECK_ICCID;
        msg1.arg2 = 0;
        mServiceHandler.sendMessageDelayed(msg1, time);
    }

    private void delayToReturnInputCmd(int slotId) {
        CatLog.d(LOG_TAG, "delayToReturnInputCmd, mServiceHandler: " + mServiceHandler);
        if(mServiceHandler == null) {
            waitForLooper();
        }
        mStkContext[slotId].mRemovePendingInputCmd = false;
        Message msg1 = mServiceHandler.obtainMessage();
        msg1.arg1 = OP_RETURN_INPUT_CMD;
        msg1.arg2 = slotId;
        mServiceHandler.sendMessageDelayed(msg1, DELAY_TO_RETURN_INPUT_CMD_TIME);
    }

    private void launchTextDialog(int slotId) {
        CatLog.d(LOG_TAG, "launchTextDialog, slotId: " + slotId +
                ", mDelayToCheckTime: " + mStkContext[slotId].mDelayToCheckTime);
        if (canShowTextDialog(mStkContext[slotId].mCurrentCmd.geTextMessage(), slotId) == false) {
            if (0 >= DELAY_TO_CHECK_IDLE_TIME || DELAY_TO_CHECK_NUM <= mStkContext[slotId].mDelayToCheckTime) {
                mStkContext[slotId].mDelayToCheckTime = 0;
                CatLog.d(LOG_TAG, "launchTextDialog responseNeeded: " +
                mStkContext[slotId].responseNeeded);
                if (mStkContext[slotId].responseNeeded) {
                    sendOkMessage(slotId);
                }
                // reset mStkContext[].responseNeeded
                if (!mStkContext[slotId].responseNeeded) {
                    mStkContext[slotId].responseNeeded = true;
                }
                handleDelayedCmd(slotId);
            } else {
                mStkContext[slotId].mDelayToCheckTime++;
                delayToCheckIdle(slotId);
            }
            return;
        }
        mStkContext[slotId].mDelayToCheckTime = 0;

        Intent newIntent = new Intent();
        String targetActivity = STK_DIALOG_ACTIVITY_NAME;
        int action = getFlagActivityNoUserAction(InitiatedByUserAction.unknown, slotId);
        String uriString = STK_DIALOG_URI + System.currentTimeMillis();
        //Set unique URI to create a new instance of activity for different slotId.
        Uri uriData = Uri.parse(uriString);
        if (newIntent != null) {
            newIntent.setClassName(PACKAGE_NAME, targetActivity);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | getFlagActivityNoUserAction(InitiatedByUserAction.unknown, slotId));
            newIntent.setData(uriData);
            newIntent.putExtra("TEXT", mStkContext[slotId].mCurrentCmd.geTextMessage());
            newIntent.putExtra(SLOT_ID, slotId);
            newIntent.putExtra(STK_SOURCE_KEY, mStkAppSourceKey);
            startActivity(newIntent);
        }
    }

    private boolean canShowTextDialog(TextMessage msg, int slotId) {
        // can show whatever screen it is.
        if (msg == null) {
            // using normal flow.
            return true;
        }
        CatLog.d(LOG_TAG, "canShowTextDialog: mMenuIsVisible: " +
        mStkContext[slotId].mMenuIsVisible + " mInputIsVisible: " +
        mStkContext[slotId].mInputIsVisible + " mDialogIsVisible: " +
        mStkContext[slotId].mDialogIsVisible);
        if (msg.isHighPriority == true) {
            return true;
        } else {
            // only show in idle screen.
            if (isIdleScreen(this.mContext) == true)
            {
                return true;
            }
            // if not in Idle Screen, but in Stk screen, will show the message.
            if (mStkContext[slotId].mMenuIsVisible == true || mStkContext[slotId].mInputIsVisible == true || mStkContext[slotId].mDialogIsVisible == true) {
                return true;
            }
        }
        return false;
    }
    /*
     * This method is used to check if the foreground application is HOME .
     */
    public boolean isIdleScreen(Context context) {
        String homePackage = null;
        String homeProcess = null;
        boolean idle = false;

        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RecentTaskInfo> taskInfo = am.getRecentTasks(16, ActivityManager.RECENT_WITH_EXCLUDED);

        if (taskInfo != null) {
            for (RecentTaskInfo task : taskInfo) {
                if (true == task.baseIntent.hasCategory(Intent.CATEGORY_HOME)) {
                    homePackage = task.baseIntent.getComponent().getPackageName();
                    break;
                }
            }
        }

        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(homePackage,0);
            homeProcess = appInfo.processName;
        } catch (NameNotFoundException e) {
            CatLog.d(LOG_TAG, "[isIdleScreen] NameNotFoundException");
            e.printStackTrace();
        }

        CatLog.d(LOG_TAG, "home package: " + homePackage + " home process: " + homeProcess);

        List<RunningAppProcessInfo> runningAppInfo = am.getRunningAppProcesses();
        for (RunningAppProcessInfo app : runningAppInfo) {
            if ( app.processName.equals(homeProcess) &&
                    app.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND ) {
                idle = true;
                break;
            }
        }
        CatLog.d(LOG_TAG, "[isIdleScreen][idle] : " + idle);
        return idle;
    }

    public boolean isStkDialogActivated(Context context) {
        String stkDialogActivity = "com.android.stk.StkDialogActivity";
        boolean activated = false;
        final ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        String topActivity = am.getRunningTasks(1).get(0).topActivity.getClassName();

        CatLog.d(LOG_TAG, "isStkDialogActivated: " + topActivity);
        if (topActivity.equals(stkDialogActivity)) {
            activated = true;
        }
        CatLog.d(LOG_TAG, "activated : " + activated);
        return activated;
    }
    private String createTelUrl(String number) {
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        return "tel:" + number;
    }

    public void dailStkCall(int slotId) {
        int subId[] = SubscriptionManager.getSubId(slotId);
        /*

        String url = createTelUrl("0958533271");
        if (url == null) {
            CatLog.d(LOG_TAG, "null url!");
            return;
        }
        CatLog.d(LOG_TAG, "dailStkCall : " + subId[0]);
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        intent.putExtra(SUBSCRIPTION_KEY, subId[0]);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TelecomManager.EXTRA_SPECIAL_OUTGOING_CALL_SOURCE,
                TelecomManager.SPECIAL_OUTGOING_CALL_STK);
        intent.putExtra(TelecomManager.EXTRA_SPECIAL_OUTGOING_CALL_EXTRAS, subId[0]);
        startActivity(intent);
   */
    }

    static String BROWSER_PACKAGE_NAME = "com.android.browser";
    public boolean isBrowserLaunched(Context context) {
        CatLog.d(LOG_TAG, "[isBrowserLaunched]+");
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        boolean top = false;
        List<RunningAppProcessInfo> runningAppInfo = am.getRunningAppProcesses();
        if (runningAppInfo != null) {
            for (RunningAppProcessInfo app : runningAppInfo) {
                if (app.processName.equals(BROWSER_PACKAGE_NAME) && (app.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
                    top = true;
                    break;
                }
            }
        }

        CatLog.d(LOG_TAG, "[isBrowserLaunched][top] : " + top);
        CatLog.d(LOG_TAG, "[isBrowserLaunched]-");
        return top;
    }

    // just for idle Screen text response
    private void  handleIdleTextResponse(int slotId) {
        CatResponseMessage resMsg = new CatResponseMessage(mStkContext[slotId].mCurrentCmd);

        if (null == mStkService || null == mStkService[slotId]) {
            CatLog.e(LOG_TAG, "mStkService is null.");
            return;
        }
        resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
        byte[] additionalInfo = new byte[1];
        additionalInfo[0] = (byte) 0x01;
        resMsg.setAdditionalInfo(additionalInfo);
        CatLog.d(LOG_TAG, "handleResponseOk ");
        if (null != mStkContext[slotId].mCurrentCmd && null !=
                mStkContext[slotId].mCurrentCmd.getCmdType()) {
            CatLog.d(LOG_TAG, "handleIdleTextResponse cmdName[" +
                    mStkContext[slotId].mCurrentCmd.getCmdType().name() + "]");
        }
        mStkService[slotId].onCmdResponse(resMsg);
    }

    private void sendOkMessage(int slotId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = OP_RESPONSE_IDLE_TEXT;
        msg.arg2 = slotId;
        mServiceHandler.sendMessage(msg);
    }
    private void launchEventMessage(int slotId, TextMessage msg) {
        if (msg == null || (msg.text != null && msg.text.length() == 0)) {
            CatLog.d(LOG_TAG, "launchEventMessage return");
            return;
        }

        // For OP09, remove the default message display for BIP command
        String optr = SystemProperties.get("ro.operator.optr");
        String ct6m_support = SystemProperties.get("ro.ct6m_support");
        AppInterface.CommandType cmdType = mStkContext[slotId].mCurrentCmd.getCmdType();
        if (((optr != null && optr.equals("OP09")) ||
            (ct6m_support != null && ct6m_support.equals("1"))) &&
            !msg.iconSelfExplanatory &&
            msg.text == null &&
            (cmdType == AppInterface.CommandType.CLOSE_CHANNEL ||
             cmdType == AppInterface.CommandType.RECEIVE_DATA  ||
             cmdType == AppInterface.CommandType.SEND_DATA     ||
             cmdType == AppInterface.CommandType.GET_CHANNEL_STATUS)) {
                return;
        }

        Toast toast = new Toast(mContext.getApplicationContext());
        LayoutInflater inflate = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.stk_event_msg, null);
        TextView tv = (TextView) v
                .findViewById(com.android.internal.R.id.message);
        ImageView iv = (ImageView) v
                .findViewById(com.android.internal.R.id.icon);
        if (msg.icon != null) {
            iv.setImageBitmap(msg.icon);
        } else {
            iv.setVisibility(View.GONE);
        }
        if (!msg.iconSelfExplanatory) {
            CatLog.d(LOG_TAG, "aaaaa [msg.iconSelfExplanatory = null] ");
            if (msg.text == null) {
                CatLog.d(LOG_TAG, "aaaaa [msg.text == null] ");
                switch (mStkContext[slotId].mCurrentCmd.getCmdType()) {
                case SEND_DTMF:
                    tv.setText(R.string.lable_send_dtmf);
                    break;
                case SEND_SMS:
                    CatLog.d(LOG_TAG, "aaaaa [SEND_SMS] ");
                    tv.setText(R.string.lable_send_sms);
                    break;
                case SEND_SS:
                    tv.setText(R.string.lable_send_ss);
                    break;
                case SEND_USSD:
                    tv.setText(R.string.lable_send_ussd);
                    break;
                case CLOSE_CHANNEL:
                    tv.setText(R.string.lable_close_channel);
                    break;
                case RECEIVE_DATA:
                    tv.setText(R.string.lable_receive_data);
                    break;
                case SEND_DATA:
                    tv.setText(R.string.lable_send_data);
                    break;
                case GET_CHANNEL_STATUS:
                    tv.setText(R.string.lable_get_channel_status);
                    break;
                }
            }
            else {
                tv.setText(msg.text);
            }
        }

        toast.setView(v);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    private void launchEventMessage(int slotId) {
        launchEventMessage(slotId, mStkContext[slotId].mCurrentCmd.geTextMessage());
    }

    private void launchConfirmationDialog(TextMessage msg, int slotId) {
        msg.title = mStkContext[slotId].lastSelectedItem;
        correctTextMessage(msg, slotId);
        Intent newIntent = new Intent();
        String targetActivity = STK_DIALOG_ACTIVITY_NAME;
        String uriString = STK_DIALOG_URI + System.currentTimeMillis();
        //Set unique URI to create a new instance of activity for different slotId.
        Uri uriData = Uri.parse(uriString);

        if (newIntent != null) {
            newIntent.setClassName(this, targetActivity);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | getFlagActivityNoUserAction(InitiatedByUserAction.unknown, slotId));
            newIntent.putExtra("TEXT", msg);
            newIntent.putExtra(SLOT_ID, slotId);
            newIntent.putExtra(STK_SOURCE_KEY, mStkAppSourceKey);
            newIntent.setData(uriData);
            startActivity(newIntent);
        }
    }

    private void launchBrowser(BrowserSettings settings) {
        if (settings == null) {
            return;
        }
        // Set browser launch mode
        Intent intent = null;

        // to launch home page, make sure that data Uri is null.
        Uri data = null;

        if (settings.url != null) {
            CatLog.d(LOG_TAG, "settings.url = " + settings.url);
            if ((settings.url.startsWith("http://") || (settings.url.startsWith("https://")))) {
                data = Uri.parse(settings.url);
            } else {
                String modifiedUrl = "http://" + settings.url;
                CatLog.d(LOG_TAG, "modifiedUrl = " + modifiedUrl);
                data = Uri.parse(modifiedUrl);
            }
        }
        if (data != null) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(data);
            mLaunchBrowserUrl = data.getAuthority();
            mLaunchBrowserUrlType = DEDICATED_URL;
        } else {
            // if the command did not contain a URL,
            // launch the browser to the default homepage.
            CatLog.d(LOG_TAG, "launch browser with default URL ");
            intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_BROWSER);
            mLaunchBrowserUrlType = DEFAULT_URL;
        }
        //The string of mLaunchBrowserUrl will be the substring of
        //"http://aaa.bbb.ccc" likes "aaa.bbb.ccc"
        CatLog.d(LOG_TAG, "authority of Uri: " + mLaunchBrowserUrl);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (settings.mode) {
        case USE_EXISTING_BROWSER:
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            break;
        case LAUNCH_NEW_BROWSER:
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            break;
        case LAUNCH_IF_NOT_ALREADY_LAUNCHED:
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            break;
        }
        // start browser activity
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            CatLog.e(LOG_TAG, "Browser activity is not found.");
        }
        // a small delay, let the browser start, before processing the next command.
        // this is good for scenarios where a related DISPLAY TEXT command is
        // followed immediately.
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            CatLog.d(LOG_TAG, "[launchBrowser] InterruptedException");
            e.printStackTrace();
        }
    }

    private void showIconToast(TextMessage msg) {
        Toast t = new Toast(this);
        ImageView v = new ImageView(this);
        v.setImageBitmap(msg.icon);
        t.setView(v);
        t.setDuration(Toast.LENGTH_LONG);
        t.show();
    }

    private void showTextToast(TextMessage msg, int slotId) {
        msg.title = mStkContext[slotId].lastSelectedItem;

        Toast toast = Toast.makeText(mContext.getApplicationContext(), msg.text,
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    // TODO should show text and Icon
    private void showIconAndTextToast(TextMessage msg) {
        Toast t = new Toast(this);
        ImageView v = new ImageView(this);
        v.setImageBitmap(msg.icon);
        t.setView(v);
        t.setDuration(Toast.LENGTH_LONG);
        t.show();
    }

    private void launchCallMsg(int slotId) {
        TextMessage msg = mStkContext[slotId].mCurrentCmd.getCallSettings().callMsg;
        if (msg.iconSelfExplanatory == true) {
            // only display Icon.

            if (msg.icon != null) {
                showIconToast(msg);
            } else {
                // do nothing.
                return;
            }
        } else {
            // show text & icon.
            if (msg.icon != null) {
                if (msg.text == null || msg.text.length() == 0) {
                    // show Icon only.
                    showIconToast(msg);
                }
                else {
                    showIconAndTextToast(msg);
                }
            } else {
                if (msg.text == null || msg.text.length() == 0) {
                    // do nothing
                    return;
                } else {
                    showTextToast(msg, slotId);
                }

            }
        }
    }
    private void launchIdleText(int slotId) {
        TextMessage msg = mStkContext[slotId].mIdleModeTextCmd.geTextMessage();
        if (msg == null) {
            CatLog.d(LOG_TAG, "mCurrent.getTextMessage is NULL");
            mNotificationManager.cancel(getNotificationId(slotId));
            return;
        }
        CatLog.d(LOG_TAG, "launchIdleText - text[" + msg.text
                         + "] iconSelfExplanatory[" + msg.iconSelfExplanatory
                         + "] icon[" + msg.icon + "], sim id: " + slotId);

        if (msg.text == null) {
            CatLog.d(LOG_TAG, "cancel IdleMode text");
            mNotificationManager.cancel(getNotificationId(slotId));
        } else {
            CatLog.d(LOG_TAG, "Add IdleMode text");
            mNotificationManager.cancel(getNotificationId(slotId));
            Notification notification = new Notification();
            RemoteViews contentView = new RemoteViews(
                    PACKAGE_NAME,
                    com.android.internal.R.layout.status_bar_latest_event_content);

            notification.flags |= Notification.FLAG_NO_CLEAR;
            notification.icon = com.android.internal.R.drawable.stat_notify_sim_toolkit;
            // Set text and icon for the status bar and notification body.
            if (!msg.iconSelfExplanatory) {
                notification.tickerText = msg.text;
                contentView.setTextViewText(com.android.internal.R.id.text,
                        msg.text);
            }
            if (msg.icon != null) {
                CatLog.d(LOG_TAG, "Idle Mode Text with icon");
                contentView.setImageViewBitmap(com.android.internal.R.id.icon,
                        msg.icon);
            } else {
                CatLog.d(LOG_TAG, "Idle Mode Text without icon");
                contentView
                        .setImageViewResource(
                                com.android.internal.R.id.icon,
                                com.android.internal.R.drawable.stat_notify_sim_toolkit);
            }
            Intent notificationIntent = new Intent(mContext,
                    NotificationAlertActivity.class);
            // use mIdleMessage replace Intent parameter, because the extra seems do not update
            // even create a new notification with same ID.
            StkApp.mIdleMessage[slotId] = msg.text;
            notificationIntent.putExtra(SLOT_ID, slotId);
            contentView.setTextViewText(com.android.internal.R.id.title, StkApp.mPLMN[slotId]);
            notification.contentView = contentView;
            notification.contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
            mNotificationManager.notify(getNotificationId(slotId), notification);
        }
    }

    private void launchToneDialog(int slotId) {
        Intent newIntent = new Intent(this, ToneDialog.class);
        String uriString = STK_TONE_URI + slotId;
        Uri uriData = Uri.parse(uriString);
        //Set unique URI to create a new instance of activity for different slotId.
        CatLog.d(LOG_TAG, "launchToneDialog, slotId: " + slotId);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | getFlagActivityNoUserAction(InitiatedByUserAction.unknown, slotId));
        newIntent.putExtra("TEXT", mStkContext[slotId].mCurrentCmd.geTextMessage());
        newIntent.putExtra("TONE", mStkContext[slotId].mCurrentCmd.getToneSettings());
        newIntent.putExtra(SLOT_ID, slotId);
        newIntent.setData(uriData);
        startActivity(newIntent);
    }

    private void launchOpenChannelDialog(int slotId) {
        TextMessage msg = mStkContext[slotId].mCurrentCmd.geTextMessage();
        if (msg == null) {
            CatLog.d(LOG_TAG, "msg is null, return here");
            return;
        }

        msg.title = getResources().getString(R.string.stk_dialog_title);
        if (msg.text == null) {
            msg.text = getResources().getString(R.string.default_open_channel_msg);
        }

        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(msg.title)
                    .setMessage(msg.text)
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.stk_dialog_accept),
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle args = new Bundle();
                            args.putInt(RES_ID, RES_ID_CHOICE);
                            args.putInt(CHOICE, YES);
                            Message message = mServiceHandler.obtainMessage();
                            message.arg1 = OP_RESPONSE;
                            message.obj = args;
                            mServiceHandler.sendMessage(message);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.stk_dialog_reject),
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle args = new Bundle();
                            args.putInt(RES_ID, RES_ID_CHOICE);
                            args.putInt(CHOICE, NO);
                            Message message = mServiceHandler.obtainMessage();
                            message.arg1 = OP_RESPONSE;
                            message.obj = args;
                            mServiceHandler.sendMessage(message);
                        }
                    })
                    .create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }

        dialog.show();
    }

    private void launchTransientEventMessage(int slotId) {
        TextMessage msg = mStkContext[slotId].mCurrentCmd.geTextMessage();
        if (msg == null) {
            CatLog.d(LOG_TAG, "msg is null, return here");
            return;
        }

        msg.title = getResources().getString(R.string.stk_dialog_title);

        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(msg.title)
                    .setMessage(msg.text)
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(android.R.string.ok),
                                       new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }

        dialog.show();
    }

    private int getNotificationId(int slotId) {
        int notifyId = STK_NOTIFICATION_ID;
        if (slotId >= 0 && slotId < mSimCount) {
            notifyId += slotId;
        } else {
            CatLog.d(LOG_TAG, "invalid slotId: " + slotId);
        }
        CatLog.d(LOG_TAG, "getNotificationId, slotId: " + slotId + ", notifyId: " + notifyId);
        return notifyId;
    }

    private String getItemName(int itemId, int slotId) {
        Menu menu = mStkContext[slotId].mCurrentCmd.getMenu();
        if (menu == null) {
            return null;
        }
        for (Item item : menu.items) {
            if (item.id == itemId) {
                return item.text;
            }
        }
        return null;
    }

    private boolean removeMenu(int slotId) {
        try {
            if (!isMenuValid(mStkContext[slotId].mCurrentMenu)) {
                mStkContext[slotId].mSetupMenuState = STATE_NOT_EXIST;
                return true;
            }
        } catch (NullPointerException e) {
            CatLog.d(LOG_TAG, "Unable to get Menu's items size");
            mStkContext[slotId].mSetupMenuState = STATE_NOT_EXIST;
            return true;
        }
        mStkContext[slotId].mSetupMenuState = STATE_EXIST;
        return false;
    }

    public void setUserAccessState(boolean state, int slotId) {
        CatLog.d(LOG_TAG, "setUserAccessState: state=" + state + ", sim id=" + slotId);
        mStkContext[slotId].isUserAccessed = state;
    }

    /*
     * This method is used to check if the current command is displayed by dialog.
     */
    boolean isDialogStyleCommand(int slotId) {
        int commandList[] = {
                AppInterface.CommandType.DISPLAY_TEXT.value(),
                AppInterface.CommandType.LAUNCH_BROWSER.value(),
                AppInterface.CommandType.SET_UP_CALL.value(),
                //AppInterface.CommandType.PLAY_TONE.value(),
                AppInterface.CommandType.OPEN_CHANNEL.value()};
        CatLog.d(LOG_TAG, "dialog command: " +
                mStkContext[slotId].mCurrentCmd.getCmdType().value());
        for (int i = 0; i < commandList.length; i++) {
            if (mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                    commandList[i]) {
                return true;
            }
        }
        CatLog.d(LOG_TAG, "isDialogStyleCommand false.");
        return false;
    }

    private void correctTextMessage(TextMessage msg, int slotId) {
        switch (mStkContext[slotId].mCurrentCmd.getCmdType()) {
            case OPEN_CHANNEL:
                if (msg.text == null) {
                    msg.text = getDefaultText(slotId);
                }
                break;
            default:
                if (msg.text == null || msg.text.length() == 0) {
                    msg.text = getDefaultText(slotId);
                }
        }
        return;
    }

    private String getDefaultText(int slotId) {
        String str = "";
        switch (mStkContext[slotId].mCurrentCmd.getCmdType()) {
        case LAUNCH_BROWSER:
            str = getResources().getString(R.string.action_launch_browser);
            break;
        case SET_UP_CALL:
            str = getResources().getString(R.string.action_setup_call);
            break;
        case OPEN_CHANNEL:
            str = getResources().getString(R.string.lable_open_channel);
            break;
        }
        return str;
    }

    public boolean haveEndSession(int slotId) {
        CatLog.d(LOG_TAG, "haveEndSession, query by sim id: " + slotId);
        if (mStkContext[slotId].mCmdsQ.size() == 0)
            return false;
        for (int i = 0 ; i < mStkContext[slotId].mCmdsQ.size() ; i++) {
            // if delay message involve OP_END_SESSION, return true;
            if (mStkContext[slotId].mCmdsQ.get(i).id == OP_END_SESSION &&
                    mStkContext[slotId].mCmdsQ.get(i).slotId == slotId) {
                CatLog.d(LOG_TAG, "end Session a delay Message");
                return true;
            }
        }
        return false;
    }

    private static final String ACTION_SEND_ERROR = "com.android.browser.action.SEND_ERROR";
    private static final String EXTRA_ERROR_CODE = "com.android.browser.error_code_key";
    private static final String EXTRA_URL = "com.android.browser.url_key";
    private static final String EXTRA_HOMEPAGE = "com.android.browser.homepage_key";

    private final IntentFilter mBrowsingStatusFilter =
        new IntentFilter(ACTION_SEND_ERROR);

    private final BroadcastReceiver mBrowsingStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String evtAction = intent.getAction();
            int evdl = EVDL_ID_BROWSING_STATUS;

            String url = intent.getStringExtra(EXTRA_URL);
            String defaultUrl = intent.getStringExtra(EXTRA_HOMEPAGE);
            int errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, 0);
            CatLog.d(LOG_TAG, "mBrowsingStatusReceiver() - evtAction["
                    + evtAction + "," + mLaunchBrowserUrl +
                    "," + url + "," + defaultUrl + "," + errorCode + "]");
            int i = 0;
            Uri defaultUri = null;
            String authDefaultUrl = null;

            if (null != url) {
                if (DEDICATED_URL == mLaunchBrowserUrlType &&
                        null != mLaunchBrowserUrl &&
                        url.contains(mLaunchBrowserUrl)) {
                    CatLog.d(LOG_TAG, "contain dedicated url.");
                } else if (DEFAULT_URL == mLaunchBrowserUrlType) {
                    if (null != defaultUrl) {
                        defaultUri = Uri.parse(defaultUrl);
                        if (null == defaultUri) {
                            return;
                        }
                        authDefaultUrl = defaultUri.getAuthority();
                    } else {
                        return;
                    }
                    if (null != authDefaultUrl &&
                        url.contains(authDefaultUrl)) {
                        CatLog.d(LOG_TAG, "contain default url.");
                    } else {
                        return;
                    }
                } else {
                    CatLog.e(LOG_TAG, "unknown url type.");
                    return;
                }
                for (i = 0; i < mSimCount; i++) {
                    SendEventDownloadMsg(evdl, i);
                }
            } else {
                CatLog.e(LOG_TAG, "null url.");
            }
        }
    };

    private final IntentFilter mLocaleChangedFilter =
        new IntentFilter("android.intent.action.LOCALE_CHANGED");

    private final BroadcastReceiver mStkLocaleChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String evtAction = intent.getAction();
            int evdl = EVDL_ID_USER_ACTIVITY;

            CatLog.d(LOG_TAG, "mStkLocaleChangedReceiver() - evtAction[" + evtAction + "]");

            if (evtAction.equals("android.intent.action.LOCALE_CHANGED")) {
                CatLog.d(LOG_TAG, "mStkLocaleChangedReceiver() - Received[LOCALE_CHANGED]");
                evdl = EVDL_ID_LANGUAGE_SELECT;
            } else {
                CatLog.d(LOG_TAG, "mStkLocaleChangedReceiver() - Received needn't handle!");
                return;
            }
            int i = 0;
            for (i = 0; i < mSimCount; i++) {
                SendEventDownloadMsg(evdl, i);
            }
        }
    };

    private final IntentFilter mIdleScreenAvailableFilter =
        new IntentFilter("android.intent.action.stk.IDLE_SCREEN_AVAILABLE");

    private final BroadcastReceiver mStkIdleScreenAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String evtAction = intent.getAction();
            int evdl = EVDL_ID_USER_ACTIVITY;

            CatLog.d(LOG_TAG, "mStkIdleScreenAvailableReceiver() - evtAction[" + evtAction + "]");

            if (evtAction.equals("android.intent.action.stk.IDLE_SCREEN_AVAILABLE")) {
                CatLog.d(LOG_TAG, "mStkIdleScreenAvailableReceiver() - Received[IDLE_SCREEN_AVAILABLE]");
                evdl = EVDL_ID_IDLE_SCREEN_AVAILABLE;
            } else {
                CatLog.d(LOG_TAG, "mStkIdleScreenAvailableReceiver() - Received needn't handle!");
                return;
            }
            int i = 0;
            for (i = 0; i < mSimCount; i++) {
                SendEventDownloadMsg(evdl, i);
            }
        }
    };

    private static final String ACTION_INCALL_SCREEN_STATE_CHANGED =
        "com.mediatek.telecom.action.INCALL_SCREEN_STATE_CHANGED";
    private static final String EXTRA_INCALL_SCREEN_SHOW =
        "com.mediatek.telecom.extra.INCALL_SCREEN_SHOW";

    private final IntentFilter mEventDownloadCallFilter =
        new IntentFilter(ACTION_INCALL_SCREEN_STATE_CHANGED);

    private final BroadcastReceiver mEventDownloadCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                final String evtAction = intent.getAction();
                int i = 0;
                CatLog.d(LOG_TAG, "mEventDownloadCallReceiver() - evtAction[" + evtAction + "]");

                if (evtAction.equals(ACTION_INCALL_SCREEN_STATE_CHANGED)) {
                    mInCallUIState = intent.getBooleanExtra(EXTRA_INCALL_SCREEN_SHOW, false);
                    CatLog.d(LOG_TAG, "[InCall_Screen_State_Changed], mInCallUIState: " +
                            mInCallUIState);
                } else {
                    CatLog.d(LOG_TAG, "Received needn't handle!");
                    return;
                }
                //All calls are disconnected, remove all TIMEOUT message.
                mServiceHandler.removeMessages(OP_EVDL_CALL_DISCONN_TIMEOUT);
                try {
                    if (!mInCallUIState) {
                        for (i = 0; i < mSimCount; i++) {
                            if (mStkContext[i].mIsPendingCallDisconnectEvent) {
                                mStkService[i].setAllCallDisConn(true);
                                SendEventDownloadMsg(EVDL_ID_CALL_DISCONNECTED, i);
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    CatLog.d(LOG_TAG, "mStkService is null, sim: " + i);
                }
            }
        }
    };

    private final BroadcastReceiver mSIMStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int i;
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                CatLog.d(LOG_TAG, "mSIMStateChangeReceiver() - slotId[" + slotId +
                    "]  state[" + simState + "]");
                if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) {
                    if (TelephonyManager.getDefault().hasIccCard(slotId)) {
                        CatLog.d(this, "FALSE AlARM!! Igonre this broadcast message");
                        return;
                    }
                    StkAppInstaller appInstaller = StkAppInstaller.getInstance();
                    if (mStkContext[slotId] != null) {
                        mStkContext[slotId].mCurrentMenu = null;
                        if (mStkContext[slotId].mCmdsQ != null &&
                                mStkContext[slotId].mCmdsQ.size() != 0) {
                            CatLog.d(LOG_TAG, "There are commands in queue SIM absent. size: " +
                                mStkContext[slotId].mCmdsQ.size());
                            //mStkContext[slotId].mCmdsQ.clear();
                        }
                        if (mServiceHandler != null) {
                            mServiceHandler.removeMessages(OP_DELAY_TO_CHECK_ICCID);
                            mServiceHandler.removeMessages(OP_DELAY_TO_CHECK_IDLE);
                        }
                        mStkContext[slotId].mSetupMenuState = STATE_UNKNOWN;
                    }
                    //Reset CatService instance.
                    mStkService[slotId] = null;
                }
            } else if (intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN")) {
                CatLog.d(LOG_TAG, "[NORMAL_SHUTDOWN]");
                SystemProperties.set(NORMAL_SHUTDOWN_PROPERTY, "1");
                StkAppInstaller appInstaller = StkAppInstaller.getInstance();
                //Uninstall all StkLauncherActivity to reset app state for next boot up.
                if (null != appInstaller) {
                    for (i = 0; i < mSimCount; i++) {
                        appInstaller.unInstall(mContext, i);
                        StkAvailable(i, STK_AVAIL_NOT_AVAILABLE);
                    }
                }
                //Clear SET_UP_MENU cache.
                for (i = 0; i < mSimCount; i++) {
                    if (mStkService[i] != null) {
                        mStkService[i].onDBHandler(i);
                    }
                }
            } else if (intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                for (i = 0; i < mSimCount; i++) {
                    CatLog.d(LOG_TAG, "[IPO_SHUTDOWN][initial mMainCmd] : " + mStkContext[i].mMainCmd);
                    mStkContext[i].mMainCmd = null;
                    mStkContext[i].mSetUpMenuHandled = false;
                    mStkContext[i].mSetupMenuCalled = false;
                    CatLog.d(LOG_TAG, "[IPO_SHUTDOWN][mMainCmd] : " + mStkContext[i].mMainCmd);
                }
            } else if (intent.getAction().equals(TelephonyIntents.ACTION_REMOVE_IDLE_TEXT)) {
                int simId = intent.getIntExtra("SIM_ID", -1);
                CatLog.d(LOG_TAG, "remove idle mode text by Refresh command for sim " + (simId + 1));
                mNotificationManager.cancel(getNotificationId(simId));
            } else if (intent.getAction().equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                CatLog.d(LOG_TAG, "get ACTION_SUBINFO_RECORD_UPDATED mSimCount: " + mSimCount);
                if (mPhone == null) {
                    mPhone = new Phone[mSimCount];
                }
                for (i = 0; i < mSimCount; i++) {
                    if (mPhone[i] == null) {
                        mPhone[i] = getPhone(i);
                    }
                    if (mPhone[i] == null) {
                        CatLog.d(LOG_TAG, "mPhone " + i + " is still null.");
                        continue;
                    }
                    if (i >= PhoneConstants.SIM_ID_1 && i < mSimCount) {
                        CatLog.d(LOG_TAG, "ACTION_SUBINFO_RECORD_UPDATED mPhone: " + mPhone[i] +
                                ", mPhoneStateChangeReg: " + mPhoneStateChangeReg[i]);
                        if (mPhoneStateChangeReg[i] == false) {
                            registerForCallState(i);
                        }
                    }
                }
            }
        }
    };
    public static final String ACTION_STK_CALL = "android.intent.action.stk.call";
    public static final String EXTRA_STK_CALL_SUBID = "subid";
    private final IntentFilter mStkCallFilter =
        new IntentFilter(ACTION_STK_CALL);

    private final BroadcastReceiver mStkCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String evtAction = intent.getAction();

            CatLog.d(LOG_TAG, "mStkCallReceiver() - evtAction[" + evtAction + "]");

            if (evtAction.equals(ACTION_STK_CALL)) {
                int subId = intent.getIntExtra(EXTRA_STK_CALL_SUBID, 0);
                CatLog.d(LOG_TAG, "mStkCallReceiver() - Received[" + ACTION_STK_CALL + "]" + subId);
                Bundle args = new Bundle();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    int slotId = SubscriptionManager.getSlotId(subId);
                    if (mStkContext[slotId].mCurrentCmd.getCmdType().value() ==
                            AppInterface.CommandType.SET_UP_CALL.value()) {
                         CatLog.d(LOG_TAG, "mStkCallReceiver() - send confirmed.");
                         processSetupCallResponse(slotId);
/*
                         args.putInt(StkAppService.RES_ID, RES_ID_CONFIRM);
                         args.putBoolean(StkAppService.CONFIRMATION, true);
                         Message message = mServiceHandler.obtainMessage();
                         message.arg1 = OP_RESPONSE;
                         message.arg2 = slotId;
                         message.obj = args;
                         mServiceHandler.sendMessage(message);
*/
                    }
                } else {
                    //create timer for return 'false' confirmed.
                }
            } else {
                CatLog.d(LOG_TAG, "mStkCallReceiver() - Received needn't handle!");
                return;
            }
        }
    };

    private final IntentFilter mAirplaneModeFilter = new IntentFilter(
        Intent.ACTION_AIRPLANE_MODE_CHANGED);

    private BroadcastReceiver mAirplaneModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean airplaneModeEnabled = isAirplaneModeOn(mContext);
            StkAppInstaller appInstaller = StkAppInstaller.getInstance();
            CatLog.d(LOG_TAG, "mAirplaneModeReceiver AIRPLANE_MODE_CHANGED...[" +
                    airplaneModeEnabled + "]");
            if (airplaneModeEnabled) {
                for (int i = PhoneConstants.SIM_ID_1; i < mSimCount; i++) {
                    if (RadioManager.isFlightModePowerOffModemEnabled()) {
                        CatLog.d(LOG_TAG, "FlightModePowerOffModemEnable true");
                        if (null != mStkContext) {
                            //Reset all stk context info, since modem will be reset by flight mode.
                            //To avoid user uses the old or null context after receiving card is
                            //absent that results from Cat service is disposed.
                            CatLog.d(LOG_TAG, "Reset stk context info.");
                            mStkContext[i].mCurrentMenu = null;
                            mStkContext[i].mCurrentMenuCmd = null;
                            mStkContext[i].mMainCmd = null;
                            mStkContext[i].mSetupMenuCalled = false;
                            if (mStkContext[i].mCmdsQ != null &&
                                mStkContext[i].mCmdsQ.size() != 0) {
                                CatLog.d(LOG_TAG, "There are commands in queue. size: " +
                                mStkContext[i].mCmdsQ.size());
                                mStkContext[i].mCmdsQ.clear();
                            }
                        }
                    }
                    String optr = SystemProperties.get("ro.operator.optr");
                    if (optr != null && !("OP02".equals(optr))) {
                        appInstaller.unInstall(mContext, i);
                    }
                    StkAvailable(i, STK_AVAIL_NOT_AVAILABLE);
                }
            } else {
                for (int i = PhoneConstants.SIM_ID_1; i < mSimCount; i++) {
                    if (TelephonyManager.getDefault().hasIccCard(i)) {
                        int subId[] = SubscriptionManager.getSubId(i);
                        Phone phone = getPhoneUsingSubId(subId[0]);
                        String cardType = phone.getIccCard().getIccCardType();
                        CatLog.d(LOG_TAG, "cardType: " + cardType);
                        if (!(cardType.contains("RUIM") || cardType.contains("CSIM"))) {
                            appInstaller.install(mContext, i);
                            StkAvailable(i, StkAppService.STK_AVAIL_AVAILABLE);
                        }
                    }
                }
            }
        }
    };

    private boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void SendEventDownloadMsg(int evdlId, int slotId) {
        CatLog.d(LOG_TAG, "SendEventDownloadMsg() - evdlId[" + evdlId + "], sim id: " + slotId);
        Bundle args = new Bundle();
        args.putInt(OPCODE, OP_EVENT_DOWNLOAD);
        args.putInt(SLOT_ID, slotId);
        args.putInt(EVDL_ID, evdlId);

        Message msg = mServiceHandler.obtainMessage();
        // msg.arg1 = EVDL_ID_IDLE_SCREEN_AVAILABLE;
        msg.arg1 = OP_EVENT_DOWNLOAD;
        msg.arg2 = slotId;
        msg.obj = args;

        mServiceHandler.sendMessage(msg);
    }

    private boolean isBipCommand(CatCmdMessage cmd) {
        switch (cmd.getCmdType()) {
        case OPEN_CHANNEL:
        case CLOSE_CHANNEL:
        case SEND_DATA:
        case RECEIVE_DATA:
        case GET_CHANNEL_STATUS:
            CatLog.d(this, "BIP command");
            return true;
        }

        CatLog.d(this, "non-BIP command");
        return false;
    }

    private boolean isBusyOnCall() {
        PhoneConstants.State s = getCallState();

        CatLog.d(this, "isBusyOnCall: " + s);
        return (s == PhoneConstants.State.RINGING);
    }

    private void init() {
        CatLog.d(LOG_TAG, "init()+ ");

        mContext = getBaseContext();
        mSimCount = TelephonyManager.from(mContext).getSimCount();
        CatLog.d(LOG_TAG, "simCount: " + mSimCount);
        mStkService = new AppInterface[mSimCount];
        mStkContext = new StkContext[mSimCount];
        StkApp.mPLMN = new String[mSimCount];
        StkApp.mIdleMessage = new String[mSimCount];
        mPhone = new Phone[mSimCount];
        mStkAppSourceKey = LOG_TAG + System.currentTimeMillis();

        serviceThread = new Thread(null, this, "Stk App Service");
        serviceThread.start();
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        sInstance = this;

        for (int i = 0; i < mSimCount; i++) {

            mStkService[i] = com.android.internal.telephony.cat.CatService.getInstance(i);
            mStkContext[i] = new StkContext();
            mStkContext[i].mSlotId = i;
            mStkContext[i].mAvailable = STK_AVAIL_INIT;
            mStkContext[i].mCmdsQ = new LinkedList<DelayedCmd>();

            if (TelephonyManager.getDefault().hasIccCard(i)) {
                if (mPhone[i] == null) {
                    mPhone[i] = getPhone(i);
                }
                CatLog.d(this, "init() : mPhoneStateChangeReg: " + mPhoneStateChangeReg[i]);
                if (mPhone[i] != null) {
                    registerForCallState(i);
                } else {
                    CatLog.d(this, "init() : null phone : " + i);
                }
            } else {
                CatLog.d(LOG_TAG, "init() : no insert sim: " + i);
                mPhone[i] = null;
            }
        }
        initNotify();

        IntentFilter mSIMStateChangeFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSIMStateChangeFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        mSIMStateChangeFilter.addAction(TelephonyIntents.ACTION_REMOVE_IDLE_TEXT);
        mSIMStateChangeFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mSIMStateChangeReceiver, mSIMStateChangeFilter);
        if (false == SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            registerReceiver(mEventDownloadCallReceiver, mEventDownloadCallFilter);
            registerReceiver(mBrowsingStatusReceiver, mBrowsingStatusFilter);
        }
        registerReceiver(mStkCallReceiver, mStkCallFilter);
        registerReceiver(mAirplaneModeReceiver, mAirplaneModeFilter);
        registerMSIMModeObserver();
        startPollingIccId();
        CatLog.d(LOG_TAG, " init()-");
    }
    public void sendMessageToServiceHandler(int opCode, Object obj, int slotId) {
        CatLog.d(LOG_TAG, "call sendMessageToServiceHandler: " + opCodeToString(opCode));
        if (mServiceHandler == null) {
            waitForLooper();
        }
        Message msg = mServiceHandler.obtainMessage(0, opCode, slotId, obj);
        mServiceHandler.sendMessage(msg);
    }

    private String opCodeToString(int opCode) {
        switch(opCode) {
            case OP_CMD:                return "OP_CMD";
            case OP_RESPONSE:           return "OP_RESPONSE";
            case OP_LAUNCH_APP:         return "OP_LAUNCH_APP";
            case OP_END_SESSION:        return "OP_END_SESSION";
            case OP_BOOT_COMPLETED:     return "OP_BOOT_COMPLETED";
            case OP_EVENT_DOWNLOAD:     return "OP_EVENT_DOWNLOAD";
            case OP_DELAYED_MSG:        return "OP_DELAYED_MSG";
            case OP_RESPONSE_IDLE_TEXT: return "OP_RESPONSE_IDLE_TEXT";
            default:                    return "unknown op code";
        }
    }

    public void StkAvailable(int slotId, int available) {
        if (mStkContext[slotId] != null) {
            mStkContext[slotId].mAvailable = available;
        }
        CatLog.d(LOG_TAG, "slotId: " + slotId + ", available: " + available + ", StkAvailable: " + ((mStkContext[slotId] != null) ? mStkContext[slotId].mAvailable : -1));
    }

    public int StkQueryAvailable(int slotId) {
        int result = ((mStkContext[slotId] != null) ? mStkContext[slotId].mAvailable : -1);

        CatLog.d(LOG_TAG, "slotId: " + slotId + ", StkQueryAvailable: " + result);
        return result;
    }

    private boolean checkSimRadioState(Context context, int slotId) {
        int dualSimMode = -1;
        boolean result = false;

        /* dualSimMode: 0 => both are off, 1 => SIM1 is on, 2 => SIM2 is on, 3 => both is on */
        dualSimMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);

        CatLog.d(LOG_TAG, "dualSimMode: " + dualSimMode + ", sim id: " + slotId);
        int curRadioOnSim = (dualSimMode & (0x01 << slotId));
        CatLog.d(LOG_TAG, "result: " + curRadioOnSim);
        if (curRadioOnSim != 0) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSupportDualTalk() {
        if (SystemProperties.get("ro.evdo_ir_support").equals("1") == true) {
            return (SystemProperties.getInt("mediatek.evdo.mode.dualtalk", 1) == 1);
        } else {
            return SystemProperties.get("ro.mtk_dt_support").equals("1");
        }
    }

    //This function is used for the case of switching on UE with flight mode power off MD.
    private void startPollingIccId() {
        int simIdx = 0;
        boolean needWait = false;
        CatLog.d(LOG_TAG, "polling start.");
        for(simIdx = PhoneConstants.SIM_ID_1; simIdx < mSimCount; simIdx++) {
            mIccId[simIdx]= SystemProperties.get(PROPERTY_ICCID_SIM[simIdx]);
        }

        for (simIdx = PhoneConstants.SIM_ID_1; simIdx < mSimCount; simIdx++) {
            if ((mIccId[simIdx] == null) || "".equals(mIccId[simIdx])) {
                needWait = true;
                break;
            }
        }

        if (mInitializeWaitCounter < INITIAL_RETRY_TIMER.length && needWait) {
            delayToCheckIccid(INITIAL_RETRY_TIMER[mInitializeWaitCounter] * 1000);
            return;
        }
        CatLog.d(LOG_TAG, "polling end.");
        mInitializeWaitCounter = 0;
        StkAppInstaller appInstaller = StkAppInstaller.getInstance();

        // Install StkMain by default
        if (-1 == appInstaller.getIsInstalled(appInstaller.STK_LAUNCH_ID)) {
            appInstaller.install(mContext, appInstaller.STK_LAUNCH_ID);
            SystemClock.sleep(100);
        }

        for (simIdx = PhoneConstants.SIM_ID_1; simIdx < mSimCount; simIdx++) {
            CatLog.d(LOG_TAG, "mIccId[" + simIdx + "]: " + mIccId[simIdx]);
            if (ICCID_STRING_FOR_NO_SIM.equals(mIccId[simIdx])) {
                appInstaller.unInstall(mContext, simIdx);
                StkAvailable(simIdx, STK_AVAIL_NOT_AVAILABLE);
                mStkContext[simIdx].mIccCardState = 0;
            } else {
                if ((mIccId[simIdx] != null) || false == ("".equals(mIccId[simIdx]))) {
                    // If any mIccId[] is valid, install StkMain
                    if (appInstaller.STK_NOT_INSTALLED ==
                        appInstaller.getIsInstalled(appInstaller.STK_LAUNCH_ID)) {
                        appInstaller.install(mContext, appInstaller.STK_LAUNCH_ID);
                    }
                    mStkContext[simIdx].mIccCardState = 1;
                }
            }
        }
        // Check if all SIMs are absent
        for (simIdx = PhoneConstants.SIM_ID_1; simIdx < mSimCount; simIdx++) {
            if (1 == mStkContext[simIdx].mIccCardState) {
                break;
            }
        }
        if (mSimCount == simIdx) {
            ICatServiceExt catServiceExt = null;
            try {
                catServiceExt = MPlugin.createInstance(ICatServiceExt.class.getName(), mContext);
            } catch (NullPointerException e) {
                CatLog.e(LOG_TAG, "ICatServiceExt: Fail to create plug-in");
                e.printStackTrace();
            }
            if (null != catServiceExt && true == catServiceExt.unInstallIfNoSim()) {
                if (appInstaller.STK_INSTALLED ==
                    appInstaller.getIsInstalled(appInstaller.STK_LAUNCH_ID)) {
                    CatLog.e(LOG_TAG, "All SIMs are absent. unInstall STK icon !!!");
                    appInstaller.unInstall(mContext, appInstaller.STK_LAUNCH_ID);
                }
            }
        }
    }
    private Phone getPhone(int slotId) {
        CatLog.d(LOG_TAG, "getPhone slotId: " + slotId);
        if (mPhone[slotId] != null) {
            return mPhone[slotId];
        }

        int subId[] = SubscriptionManager.getSubId(slotId);
        int phoneId = 0;
        if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
            phoneId = SubscriptionManager.getPhoneId(subId[0]);
            CatLog.d(LOG_TAG, "subId[0] " + subId[0] + " phoneId: " + phoneId);
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                CatLog.d(LOG_TAG, "ERROR: invalid phone id.");
                return null;
            }
        } else {
            //FIXME
            if (subId == null) {
                CatLog.d(LOG_TAG, "ERROR: sub array is null.");
            } else {
                CatLog.d(LOG_TAG, "ERROR: sub id is invalid. sub id: " + subId[0]);
            }
            return null; //should return null and caller should handle null object case.
        }
        try {
            mPhone[slotId] = PhoneFactory.getPhone(phoneId);
        } catch (IllegalStateException e) {
            // Exception thrown by getPhone() when default phone is not made
            CatLog.e(LOG_TAG, "IllegalStateException, get phone fail.");
            e.printStackTrace();
            return null;
        }
        CatLog.d(LOG_TAG, "getPhone done.");
        return mPhone[slotId];
    }
    private PhoneConstants.State getCallState() {
        Phone phone = null;
        for (int i = 0; i < mSimCount; i++) {
            phone = getPhone(i);
            if (phone == null) {
                CatLog.d(LOG_TAG, "Phone is null.");
                continue;
            }
            PhoneConstants.State ps = phone.getState();
            CatLog.d(LOG_TAG, "Phone " + i + " state: " + ps);
            if (ps == PhoneConstants.State.RINGING) {
                return PhoneConstants.State.RINGING;
            }
        }
        return PhoneConstants.State.IDLE;
    }
    StkContext getStkContext(int slotId) {
        if (slotId >= 0 && slotId < mSimCount) {
    	    return mStkContext[slotId];
        } else {
            CatLog.d(LOG_TAG, "invalid slotId: " + slotId);
            return null;
        }
    }

    private void handleAlphaNotify(Bundle args) {
        String alphaString = args.getString(AppInterface.ALPHA_STRING);

        CatLog.d(this, "Alpha string received from card: " + alphaString);
        Toast toast = Toast.makeText(sInstance, alphaString, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }
    private void registerMSIMModeObserver() {
        CatLog.d(this, "call registerMSIMModeObserver");
        Uri uri = Settings.System.getUriFor(Settings.System.MSIM_MODE_SETTING);
        mContext.getContentResolver().registerContentObserver(
                uri, false, mMSIMModeObserver);
    }

    private void unRegisterMSIMModeObserver() {
        CatLog.d(this, "call unRegisterMSIMModeObserver");
        mContext.getContentResolver().unregisterContentObserver(
                mMSIMModeObserver);
    }
    private ContentObserver mMSIMModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            StkAppInstaller appInstaller = StkAppInstaller.getInstance();
            for (int i = PhoneConstants.SIM_ID_1; i < mSimCount; i++) {
                boolean radioOn = checkSimRadioState(mContext, i);
                CatLog.d(this, "mMSIMModeObserver radioOn: " + radioOn);
                if (radioOn) {
                    if (CatService.getSaveNewSetUpMenuFlag(i) &&
                        mStkContext[i].mMainCmd != null &&
                        isMenuValid(mStkContext[i].mMainCmd.getMenu())) {
                        appInstaller.install(mContext, i);
                        StkAvailable(i, STK_AVAIL_AVAILABLE);
                    }
                } else {
                    appInstaller.unInstall(mContext, i);
                    StkAvailable(i, STK_AVAIL_NOT_AVAILABLE);
                }
            }
        }
    };

    private boolean isMenuValid(Menu menu) {
        if (menu == null) {
            CatLog.d(this, "Null menu.");
            return false;
        }
        if (menu.items.size() == 1 &&
            menu.items.get(0) == null) {
            return false;
        }
        return true;
    }
    public boolean isValidStkSourceKey(String strKey) {
        CatLog.d(this, "isValidStkSourceKey key: " + strKey + " , " + mStkAppSourceKey);
        return (mStkAppSourceKey == null) ? false : mStkAppSourceKey.equals(strKey);
    }
    /**
    * This method is used for restoring current command and launch activity.
    *
    * @param slotId get current command by slot id.
    */
    final synchronized public void restoreCurrentCmd(int slotId) {
        CatLog.d(this, "restoreCurrentCmd act : " + slotId);
        if (slotId < 0 || slotId >= mSimCount) {
            return;
        }
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = OP_RESTORE_CURRENT_CMD;
        msg.arg2 = slotId;
        mServiceHandler.sendMessage(msg);
    }

    private RoamingMode getRoamingState(int slotId) {
        LteDcPhoneProxy lteDcPhoneProxy = (LteDcPhoneProxy) PhoneFactory.getPhone(slotId);
        SvlteRatController lteRatController = null;
        if (null != lteDcPhoneProxy) {
            lteRatController = lteDcPhoneProxy.getSvlteRatController();
            if (null != lteRatController) {
                mRoamingMode = lteRatController.getRoamingMode();
                CatLog.d(this, "getRoamingState: mRoamingMode = " + mRoamingMode);
            }
        }
        return mRoamingMode;
    }

    private Phone getPhoneUsingSubId(int subId) {
        CatLog.d(this, "getPhoneUsingSubId subId:" + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    private void registerForCallState(int slotId) {
        if (null == mPhone || null == mPhone[slotId]) {
            CatLog.d(this, "registerForCallState mPhone is null : " + slotId);
            mPhoneStateChangeReg[slotId] = false;
            return;
        }
        if (slotId == PhoneConstants.SIM_ID_1) {
            mPhone[slotId].registerForPreciseCallStateChanged(mCallHandler,
                    PHONE_STATE_CHANGED, null);
            mPhone[slotId].registerForDisconnect(mCallDisConnHandler,
                    PHONE_DISCONNECT, null);
            mPhone[slotId].registerForSuppServiceFailed(mCallHandler,
                    SUPP_SERVICE_FAILED, null);
        } else if (slotId == PhoneConstants.SIM_ID_2) {
            mPhone[slotId].registerForPreciseCallStateChanged(mCallHandler2,
                    PHONE_STATE_CHANGED, null);
            mPhone[slotId].registerForDisconnect(mCallDisConnHandler,
                    PHONE_DISCONNECT2, null);
            mPhone[slotId].registerForSuppServiceFailed(mCallHandler2,
                    SUPP_SERVICE_FAILED, null);
        } else if (slotId == PhoneConstants.SIM_ID_3) {
            mPhone[slotId].registerForPreciseCallStateChanged(mCallHandler3,
                    PHONE_STATE_CHANGED, null);
            mPhone[slotId].registerForDisconnect(mCallDisConnHandler,
                    PHONE_DISCONNECT3, null);
            mPhone[slotId].registerForSuppServiceFailed(mCallHandler3,
                    SUPP_SERVICE_FAILED, null);
        } else {
            mPhone[slotId].registerForPreciseCallStateChanged(mCallHandler4,
                    PHONE_STATE_CHANGED, null);
            mPhone[slotId].registerForDisconnect(mCallDisConnHandler,
                    PHONE_DISCONNECT4, null);
            mPhone[slotId].registerForSuppServiceFailed(mCallHandler4,
                    SUPP_SERVICE_FAILED, null);
        }
        mPhoneStateChangeReg[slotId] = true;
    }

    private void unregisterForCallState(int slotId) {
        if (null == mPhone || null == mPhone[slotId]) {
            CatLog.d(this, "unregisterForCallState mPhone is null : " + slotId);
            return;
        }
        if (slotId == PhoneConstants.SIM_ID_1) {
            mPhone[slotId].unregisterForPreciseCallStateChanged(mCallHandler);
            mPhone[slotId].unregisterForSuppServiceFailed(mCallHandler);
        } else if (slotId == PhoneConstants.SIM_ID_2) {
            mPhone[slotId].unregisterForPreciseCallStateChanged(mCallHandler2);
            mPhone[slotId].unregisterForSuppServiceFailed(mCallHandler2);
        } else if (slotId == PhoneConstants.SIM_ID_3) {
            mPhone[slotId].unregisterForPreciseCallStateChanged(mCallHandler3);
            mPhone[slotId].unregisterForSuppServiceFailed(mCallHandler3);
        } else {
            mPhone[slotId].unregisterForPreciseCallStateChanged(mCallHandler4);
            mPhone[slotId].unregisterForSuppServiceFailed(mCallHandler4);
        }
        mPhone[slotId].unregisterForDisconnect(mCallDisConnHandler);
        mPhoneStateChangeReg[slotId] = false;
    }
}
