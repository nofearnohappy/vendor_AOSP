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

package com.android.utk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;


import com.android.internal.telephony.cdma.utk.AppInterface;
import com.android.internal.telephony.cdma.utk.Item;
import com.android.internal.telephony.cdma.utk.Input;
import com.android.internal.telephony.cdma.utk.Menu;
import com.android.internal.telephony.cdma.utk.ResultCode;
import com.android.internal.telephony.cdma.utk.TextMessage;
import com.android.internal.telephony.cdma.utk.UtkCmdMessage;
import com.android.internal.telephony.cdma.utk.UtkCmdMessage.BrowserSettings;
import com.android.internal.telephony.cdma.utk.UtkLog;
import com.android.internal.telephony.cdma.utk.UtkResponseMessage;


import com.android.internal.telephony.IccCardConstants;

import com.android.internal.telephony.Phone;
//import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.ILteDataOnlyController;

import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;

import java.util.LinkedList;

/**
 * UIM toolkit application level service. Interacts with Telephopny messages,
 * application's launch and user input from UTK UI elements.
 *
 */
public class UtkAppService extends Service implements Runnable {

    // members
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private AppInterface mUtkService;
    private Context mContext = null;
    private UtkCmdMessage mMainCmd = null;
    private UtkCmdMessage mCurrentCmd = null;
    private Menu mCurrentMenu = null;
    private String lastSelectedItem = null;
    private boolean mMenuIsVisibile = false;
    private boolean mCanLaunchStkMenuActivity = true;
    private boolean responseNeeded = true;
    private boolean mCmdInProgress = false;
    private NotificationManager mNotificationManager = null;
    private LinkedList<DelayedCmd> mCmdsQ = null;
    private boolean launchBrowser = false;
    private BrowserSettings mBrowserSettings = null;
    static UtkAppService sInstance = null;
    //public Phone mPhone;
    private Phone[] mPhone = null;
    // Used for setting FLAG_ACTIVITY_NO_USER_ACTION when
    // creating an intent.
    private enum InitiatedByUserAction {
        yes,            // The action was started via a user initiated action
        unknown,        // Not known for sure if user initated the action
    }
    //Add for utk IR case
    private RoamingMode mRoamingMode = RoamingMode.ROAMING_MODE_HOME;
    // constants
    static final String OPCODE = "op";
    static final String CMD_MSG = "cmd message";
    static final String RES_ID = "response id";
    static final String MENU_SELECTION = "menu selection";
    static final String INPUT = "input";
    static final String HELP = "help";
    static final String CONFIRMATION = "confirm";
    static final String CHOICE = "choice";
    static final String PHONE_ID = "PHONE_ID";
//    static final String EXTRA_DIAPLAYNAME = "diapplayname";
    // operations ids for different service functionality.
    static final int OP_CMD = 1;
    static final int OP_RESPONSE = 2;
    static final int OP_LAUNCH_APP = 3;
    static final int OP_END_SESSION = 4;
    static final int OP_BOOT_COMPLETED = 5;
    private static final int OP_DELAYED_MSG = 6;

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

    static final int YES = 1;
    static final int NO = 0;

    private static final String PACKAGE_NAME = "com.android.utk";
    private static final String MENU_ACTIVITY_NAME =
                                        PACKAGE_NAME + ".UtkMenuActivity";
    private static final String INPUT_ACTIVITY_NAME =
                                        PACKAGE_NAME + ".UtkInputActivity";

    // Notification id used to display Idle Mode text in NotificationManager.
    private static final int UTK_NOTIFICATION_ID = 333;
    private static final int PHONE_STATE_CHANGED = 101;
    private static final int SUPP_SERVICE_FAILED = 102;

    private static final int miSIMid = 0;  // Gemini SIM1

    //Utk changes on L
    private int mSimCount = 0;
    //WFS2 modification
    private int mActiveUtkId = -1;
    //Utk modification for TDD data only
    private ILteDataOnlyController mLteDataOnlyController;

    // Inner class used for queuing telephony messages (proactive commands,
    // session end) while the service is busy processing a previous message.
    private class DelayedCmd {
        // members
        int id;
        UtkCmdMessage msg;

        DelayedCmd(int id, UtkCmdMessage msg) {
            this.id = id;
            this.msg = msg;
        }
    }

    @Override
    public void onCreate() {
        // Initialize members
        // This can return null if UtkService is not yet instantiated, but it's ok
        // If this is null we will do getInstance before we need to use this
        mUtkService = com.android.internal.telephony.cdma.utk.UtkService
                .getInstance();

        mCmdsQ = new LinkedList<DelayedCmd>();
        Thread serviceThread = new Thread(null, this, "Utk App Service");
        serviceThread.start();
        mContext = getBaseContext();
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        sInstance = this;
        // Modify the method of getting Phones on L
        //mPhone = PhoneFactory.getDefaultPhone();
        mSimCount = TelephonyManager.from(mContext).getSimCount();
        UtkLog.d(this, "mSimCount is " + mSimCount);
        mPhone = new Phone[mSimCount];
        int i = 0;

        for (i = 0; i < mSimCount; i++) {
            UtkLog.d(this, "slotId is " + i);

            if (TelephonyManager.getDefault().hasIccCard(i)) {
                UtkLog.d(this, "insert sim is " + i);

                int subId[] = SubscriptionManager.getSubId(i);
                if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                    int phoneId = SubscriptionManager.getPhoneId(subId[0]);
                    if (SubscriptionManager.isValidPhoneId(phoneId)) {
                        UtkLog.d(this, "sim: " + i + ", " + subId[0] + ", " + phoneId);
                        mPhone[i] = PhoneFactory.getPhone(phoneId);
                    } else {
                        mPhone[i] = null;
                        UtkLog.d(this, "invalid phone id.");
                    }
                } else {
                    mPhone[i] = null;
                    UtkLog.d(this, "invalid sub id.");
                }
            } else {
                UtkLog.d(this, "no insert sim: " + i);
                mPhone[i] = null;
            }
        }

        IntentFilter mSIMStateChangeFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mSIMStateChangeReceiver, mSIMStateChangeFilter);
        //Utk modification for TDD data only
        mLteDataOnlyController =
            MPlugin.createInstance(ILteDataOnlyController.class.getName(), mContext);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        waitForLooper();

        // onStart() method can be passed a null intent
        // TODO: replace onStart() with onStartCommand()
        if (intent == null) {
            return;
        }

        Bundle args = intent.getExtras();

        if (args == null) {
            return;
        }

        Message msg = mServiceHandler.obtainMessage();
        int mPhoneId = -1;
        msg.arg1 = args.getInt(OPCODE);
        if ((OP_CMD == msg.arg1) || (OP_RESPONSE == msg.arg1) || (OP_END_SESSION == msg.arg1)) {
            mPhoneId = args.getInt(PHONE_ID);
            UtkLog.d(this, " mActiveUtkId = " + mActiveUtkId + ", mPhoneId = " + mPhoneId);
        }
        switch(msg.arg1) {
        case OP_CMD:
            UtkLog.d(this, " UTK msg.arg1: OP_CMD");
            if (mPhoneId != mActiveUtkId) {
                mActiveUtkId = mPhoneId;
                mCanLaunchStkMenuActivity = true;
                UtkLog.d(this, " update mUtkService obj in UtkAppService");
                mUtkService = com.android.internal.telephony.cdma.utk.UtkService.
                        getInstance(mPhoneId);
            }
            msg.obj = args.getParcelable(CMD_MSG);
            break;
        case OP_RESPONSE:
            UtkLog.d(this, " UTK msg.arg1: OP_RESPONSE");
            msg.arg2 = mPhoneId;
            msg.obj = args;
            break;
        case OP_END_SESSION:
            msg.arg2 = mPhoneId;
            /* falls through */
        case OP_LAUNCH_APP:
        case OP_BOOT_COMPLETED:
            break;
        default:
            return;
        }
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mSIMStateChangeReceiver);
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
     * Package api used by UtkMenuActivity to indicate if its on the foreground.
     */
    void indicateMenuVisibility(boolean visibility) {
        mMenuIsVisibile = visibility;
    }

    /*
     * Package api used by UtkMenuActivity to get its Menu parameter.
     */
    Menu getMenu() {
        return mCurrentMenu;
    }

    boolean isCurCmdSetupCall() {
        if (mCurrentCmd == null) {
            UtkLog.d(this, "[isCurCmdSetupCall][mCurrentCmd]:null");
            return false;
        } else if (mCurrentCmd.getCmdType() == null) {
            UtkLog.d(this, "[isCurCmdSetupCall][mCurrentCmd.getCmdType()]:null");
            return false;
        } else {
            UtkLog.d(this, "SET UP CALL Cmd Check["  + mCurrentCmd.getCmdType().value() + "]");
            return (AppInterface.CommandType.SET_UP_CALL.value() == mCurrentCmd.getCmdType().value());
        }
     }

    /*
     * Package api used by UI Activities and Dialogs to communicate directly
     * with the service to deliver state information and parameters.
     */
    static UtkAppService getInstance() {
        return sInstance;
    }

    private void waitForLooper() {
        while (mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    //Follow Google original source code and need not handle this exception.
                    //But maybe changed in future
                }
            }
        }
    }

    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int opcode = msg.arg1;

            UtkLog.d(this, "ServiceHandler " + opcode);

            switch (opcode) {
            case OP_LAUNCH_APP:
                //no UIM card insert
                // when gsm network, the utk fw is disposed and the instance will be null.
                mUtkService = com.android.internal.telephony.cdma.utk.UtkService.getInstance();

                if (mUtkService == null || mMainCmd == null || mCurrentMenu == null) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                                                    getString(R.string.activity_not_found),
                                                        Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                    UtkLog.d(this, "UTK mMainCmd == null");
                    return;
                }

                if (isOnFlightMode() == true) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                                                    getString(R.string.lable_on_flight_mode),
                                                        Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                    UtkLog.d(this, "Utk can't be launched in flight mode");
                    return;
                }

                //Utk modification for TDD data only
                if ((mLteDataOnlyController != null) &&
                    (!mLteDataOnlyController.checkPermission())) {
                    UtkLog.d(this, "Utk can't be launched in TDD data only mode");
                    return;
                }

                //Utk modification for IR
                if (getRoamingState(mActiveUtkId) != RoamingMode.ROAMING_MODE_HOME) {
                    //Todo:Utk maybe need another toast to remind user
                    Toast toast = Toast.makeText(getApplicationContext(),
                                                    getString(R.string.activity_not_found),
                                                        Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                    UtkLog.d(this, "Utk can't be launched in Roaming State");
                    return;
                }

                //when radio off sim card from sim manager
                if ((mCanLaunchStkMenuActivity == false) || !(mUtkService.isRadioOn())) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                                                    getString(R.string.lable_on_radio_off),
                                                        Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                    UtkLog.d(this, "Utk can't be launched on radio off");
                    return;
                }

                if (isBusyOnCall() == true) {
                    Toast toast = Toast.makeText(mContext.getApplicationContext(), R.string.lable_busy_on_call, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                    return;
                }
                launchMenuActivity(null);
                break;
            case OP_CMD:
                UtkCmdMessage cmdMsg = (UtkCmdMessage) msg.obj;
                // There are two types of commands:
                // 1. Interactive - user's response is required.
                // 2. Informative - display a message, no interaction with the user.
                //
                // Informative commands can be handled immediately without any delay.
                // Interactive commands can't override each other. So if a command
                // is already in progress, we need to queue the next command until
                // the user has responded or a timeout expired.
                if (!isCmdInteractive(cmdMsg)) {
                    handleCmd(cmdMsg);
                } else {
                    if (!mCmdInProgress) {
                        mCmdInProgress = true;
                        handleCmd((UtkCmdMessage) msg.obj);
                    } else {
                        mCmdsQ.addLast(new DelayedCmd(OP_CMD,
                                (UtkCmdMessage) msg.obj));
                    }
                }
                break;
            case OP_RESPONSE:
                if (responseNeeded) {
                    handleCmdResponse((Bundle) msg.obj, msg.arg2);
                }
                // call delayed commands if needed.
                if (mCmdsQ.size() != 0) {
                    callDelayedMsg(msg.arg2);
                } else {
                    mCmdInProgress = false;
                }
                // reset response needed state var to its original value.
                responseNeeded = true;
                break;
            case OP_END_SESSION:
                if (!mCmdInProgress) {
                    mCmdInProgress = true;
                    handleSessionEnd(msg.arg2);
                } else {
                    mCmdsQ.addLast(new DelayedCmd(OP_END_SESSION, null));
                }
                break;
            case OP_BOOT_COMPLETED:
                UtkLog.d(this, "OP_BOOT_COMPLETED");
                if (mMainCmd == null) {
                    //UtkAppInstaller.unInstall(mContext);
                }
                break;
            case OP_DELAYED_MSG:
                int mPhoneId = msg.arg2;
                handleDelayedCmd(mPhoneId);
                break;
            }
        }
    }

    private boolean isCmdInteractive(UtkCmdMessage cmd) {
        switch (cmd.getCmdType()) {
        case SEND_DTMF:
        case SEND_SMS:
        case SEND_SS:
        case SEND_USSD:
        case SET_UP_IDLE_MODE_TEXT:
        case SET_UP_MENU:
        //case SET_UP_CALL:
            return false;
        }

        return true;
    }

    private void handleDelayedCmd(int phoneId) {
        if (mCmdsQ.size() != 0) {
            DelayedCmd cmd = mCmdsQ.poll();
            switch (cmd.id) {
            case OP_CMD:
                handleCmd(cmd.msg);
                break;
            case OP_END_SESSION:
                handleSessionEnd(phoneId);
                break;
            }
        }
    }

    private void callDelayedMsg(int phoneId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = OP_DELAYED_MSG;
        msg.arg2 = phoneId;
        mServiceHandler.sendMessage(msg);
    }

    private void handleSessionEnd(int phoneId) {
        UtkLog.d(this, "handleSessionEnd for phoneId: " + phoneId);
        mCurrentCmd = mMainCmd;
        lastSelectedItem = null;
        // In case of SET UP MENU command which removed the app, don't
        // update the current menu member.
        if (mCurrentMenu != null && mMainCmd != null) {
            mCurrentMenu = mMainCmd.getMenu();
        }
        if (mMenuIsVisibile) {
            launchMenuActivity(null);
        }
        if (mCmdsQ.size() != 0) {
            callDelayedMsg(phoneId);
        } else {
            mCmdInProgress = false;
        }
        // In case a launch browser command was just confirmed, launch that url.
        if (launchBrowser) {
            launchBrowser = false;
            launchBrowser(mBrowserSettings);
        }
    }

    private void handleCmd(UtkCmdMessage cmdMsg) {
        if (cmdMsg == null) {
            return;
        }
        // save local reference for state tracking.
        mCurrentCmd = cmdMsg;
        boolean waitForUsersResponse = true;
        byte[] additionalInfo = null;

        UtkLog.d(this, cmdMsg.getCmdType().name());

        if (mUtkService == null) {
            mUtkService = com.android.internal.telephony.cdma.utk.UtkService.getInstance();
            if (mUtkService == null) {
                // This should never happen (we should be responding only to a message
                // that arrived from UtkService). It has to exist by this time
                UtkLog.d(this, "mUtkService is null when we need to" +
                        "send response in handleCmd");
                return;
            }
        }

        switch (cmdMsg.getCmdType()) {
        case DISPLAY_TEXT:
            if (isBusyOnCall() == true) {
                UtkLog.d(this, "[Handle Command][DISPLAY_TEXT][Can not handle currently]");
                UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                mUtkService.onCmdResponse(resMsg);
                return;
            }

            TextMessage msg = cmdMsg.geTextMessage();
            responseNeeded = msg.responseNeeded;
            if (lastSelectedItem != null) {
                msg.title = lastSelectedItem;
            } else if (mMainCmd != null) {
                msg.title = mMainCmd.getMenu().title;
            } else {
                // TODO: get the carrier name from the RUIM
                msg.title = "";
            }
            launchTextDialog();
            break;
        case SELECT_ITEM:
            mCurrentMenu = cmdMsg.getMenu();

            if (isBusyOnCall() == true) {
                UtkLog.d(this, "[Handle Command][SELECT_ITEM][Can not handle currently]");
                UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                mUtkService.onCmdResponse(resMsg);
                return;
            }

            launchMenuActivity(cmdMsg.getMenu());
            break;
        case SET_UP_MENU:
            mMainCmd = mCurrentCmd;
            mCurrentMenu = cmdMsg.getMenu();
            if (mCurrentMenu == null) {
                UtkLog.d(this, "mCurrentMenu == null");
            }
            if (removeMenu()) {
                UtkLog.d(this, "Uninstall App");
                mCurrentMenu = null;
                //UtkAppInstaller.unInstall(mContext);
            } else {
                UtkLog.d(this, "Install App");
                UtkAppInstaller.install(mContext);
            }
            if (mMenuIsVisibile) {
                launchMenuActivity(null);
            }
            break;
        case GET_INPUT:
        case GET_INKEY:
            if (isBusyOnCall() == true) {
                UtkLog.d(this, "[Handle Command][GET_INPUT][Can not handle currently]");
                UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                mUtkService.onCmdResponse(resMsg);
                return;
            }
            launchInputActivity();
            break;
        case SET_UP_IDLE_MODE_TEXT:
            waitForUsersResponse = false;
            launchIdleText();
            break;
        case SEND_SMS:
            if (mCurrentCmd.geTextMessage().text == null)
            {
                mCurrentCmd.setTextMessage(mContext.getString(R.string.utk_send_sms));
            }
        case SEND_DTMF:
        case SEND_SS:
        case SEND_USSD:
            waitForUsersResponse = false;
            launchEventMessage();
            break;
        case LAUNCH_BROWSER:
            launchConfirmationDialog(mCurrentCmd.geTextMessage());
            break;
        case SET_UP_CALL:
            if (mCurrentCmd.getCallSettings().confirmMsg.text != null) {
               UtkLog.d(this, "handleCmd SET_UP_CALL confirm text not null");
               launchConfirmationDialog(mCurrentCmd.getCallSettings().confirmMsg);
             }
             else {
               UtkLog.d(this, "handleCmd SET_UP_CALL confirm text is null, launchCallMsg");
               launchCallMsg();

                UtkLog.d(this, "OK Clicked! isCurCmdSetupCall[" + isCurCmdSetupCall() + "]");
                if (isCurCmdSetupCall()) {
                    UtkLog.d(this, "Utk call sendBroadcast(STKCALL_REGISTER_SPEECH_INFO)");
                    Intent intent = new Intent("com.android.stk.STKCALL_REGISTER_SPEECH_INFO");
                    sendBroadcast(intent);
                }
                mCmdInProgress = false;
                //UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);
                //resMsg.setResultCode(ResultCode.OK);
                //resMsg.setConfirmation(true);
                //mUtkService.onCmdResponse(resMsg);
             }
            break;
        case PLAY_TONE:
            launchToneDialog();
            break;
        case OPEN_CHANNEL:
            if (isBusyOnCall() == true) {
                UtkLog.d(this, "isBusyOnCall");
                UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);
                resMsg.setResultCode(ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS);
                mUtkService.onCmdResponse(resMsg);
                mCmdInProgress = false;
                return;
            }

/*            if ((mCurrentCmd.geTextMessage().text != null) &&
                (mCurrentCmd.geTextMessage().text.length() != 0)){
                UtkLog.d(this, "handleCmd OPEN_CHANNEL confirm text not null");
                launchConfirmationDialog(mCurrentCmd.geTextMessage());
            }else*/ {
                //UtkLog.d(this, "handleCmd OPEN_CHANNEL confirm text is null, launchDataCallMsg");
                waitForUsersResponse = false;

                UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);
                resMsg.setResultCode(ResultCode.OK);
                resMsg.setConfirmation(true);
                mUtkService.onCmdResponse(resMsg);
            }
            break;
        case CLOSE_CHANNEL:
        case RECEIVE_DATA:
        case SEND_DATA:
            if (mCurrentCmd.geTextMessage().text != null
                        && mCurrentCmd.geTextMessage().text.length() != 0) {
                launchDataCallMsg(mCurrentCmd.geTextMessage().text.subSequence(0,
                        mCurrentCmd.geTextMessage().text.length()));
            } else {
                UtkLog.d(this, "handleCmd,text is null no need toast");
            }
            waitForUsersResponse = false;
            break;
        }

        if (!waitForUsersResponse) {
            if (mCmdsQ.size() != 0) {
                callDelayedMsg(mActiveUtkId);
            } else {
                mCmdInProgress = false;
            }
        }
    }

    private void handleCmdResponse(Bundle args, int phoneId) {
        if (mCurrentCmd == null) {
            return;
        }

        mUtkService = com.android.internal.telephony.cdma.utk.UtkService.getInstance(phoneId);
        if (mUtkService == null) {
            // This should never happen (we should be responding only to a message
            // that arrived from UtkService). It has to exist by this time
            UtkLog.d(this, "mUtkService is null when we need to send " +
                "response in handleCmdResponse");
            return;
        }


        UtkResponseMessage resMsg = new UtkResponseMessage(mCurrentCmd);

        UtkLog.d(this, "handleCmdResponse, phoneId: " + phoneId);

        // set result code
        boolean helpRequired = args.getBoolean(HELP, false);

        switch(args.getInt(RES_ID)) {
        case RES_ID_MENU_SELECTION:
            UtkLog.d(this, "RES_ID_MENU_SELECTION");
            int menuSelection = args.getInt(MENU_SELECTION);
            switch(mCurrentCmd.getCmdType()) {
            case SET_UP_MENU:
            case SELECT_ITEM:
                lastSelectedItem = getItemName(menuSelection);
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
            UtkLog.d(this, "RES_ID_INPUT");
            String input = args.getString(INPUT);
            Input cmdInput = mCurrentCmd.geInput();
            if (cmdInput != null && cmdInput.yesNo) {
                boolean yesNoSelection = input
                        .equals(UtkInputActivity.YES_STR_RESPONSE);
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
            UtkLog.d(this, "RES_ID_CONFIRM");
            boolean confirmed = args.getBoolean(CONFIRMATION);
            UtkLog.d(this, "confirmed: " + confirmed + " mCurrentCmd.getCmdType(): " + mCurrentCmd.getCmdType());
            switch (mCurrentCmd.getCmdType()) {
            case DISPLAY_TEXT:
                resMsg.setResultCode(confirmed ? ResultCode.OK
                        : ResultCode.UICC_SESSION_TERM_BY_USER);
                break;
            case LAUNCH_BROWSER:
                resMsg.setResultCode(confirmed ? ResultCode.OK
                        : ResultCode.UICC_SESSION_TERM_BY_USER);
                if (confirmed) {
                    launchBrowser = true;
                    mBrowserSettings = mCurrentCmd.getBrowserSettings();
                }
                break;
            case SET_UP_CALL:
                UtkLog.d(this, "SET_UP_CALL");
                resMsg.setResultCode(ResultCode.OK);
                resMsg.setConfirmation(confirmed);
                if (confirmed) {
                    launchCallMsg();
                }
                break;
            case OPEN_CHANNEL:
                UtkLog.d(this, "OPEN_CHANNEL");
                resMsg.setResultCode(ResultCode.OK);
                resMsg.setConfirmation(confirmed);
                break;
            }
            break;
        case RES_ID_DONE:
            resMsg.setResultCode(ResultCode.OK);
            break;
        case RES_ID_BACKWARD:
            UtkLog.d(this, "RES_ID_BACKWARD");
            resMsg.setResultCode(ResultCode.BACKWARD_MOVE_BY_USER);
            break;
        case RES_ID_END_SESSION:
            UtkLog.d(this, "RES_ID_END_SESSION");
            resMsg.setResultCode(ResultCode.UICC_SESSION_TERM_BY_USER);
            break;
        case RES_ID_TIMEOUT:
            UtkLog.d(this, "RES_ID_TIMEOUT");
            // GCF test-case 27.22.4.1.1 Expected Sequence 1.5 (DISPLAY TEXT,
            // Clear message after delay, successful) expects result code OK.
            // If the command qualifier specifies no user response is required
            // then send OK instead of NO_RESPONSE_FROM_USER
            if ((mCurrentCmd.getCmdType().value() == AppInterface.CommandType.DISPLAY_TEXT
                    .value())
                    && (mCurrentCmd.geTextMessage().userClear == false)) {
                resMsg.setResultCode(ResultCode.OK);
            } else {
                resMsg.setResultCode(ResultCode.NO_RESPONSE_FROM_USER);
            }
            break;
        default:
            UtkLog.d(this, "Unknown result id");
            return;
        }
        mUtkService.onCmdResponse(resMsg);
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
    private int getFlagActivityNoUserAction(InitiatedByUserAction userAction) {
        return ((userAction == InitiatedByUserAction.yes) | mMenuIsVisibile) ?
                                                    0 : Intent.FLAG_ACTIVITY_NO_USER_ACTION;
    }

    private void launchMenuActivity(Menu menu) {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setClassName(PACKAGE_NAME, MENU_ACTIVITY_NAME);
        int intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP;
        if (menu == null) {
            // We assume this was initiated by the user pressing the tool kit icon
            intentFlags |= getFlagActivityNoUserAction(InitiatedByUserAction.yes);

            newIntent.putExtra("STATE", UtkMenuActivity.STATE_MAIN);
        } else {
            // We don't know and we'll let getFlagActivityNoUserAction decide.
            intentFlags |= getFlagActivityNoUserAction(InitiatedByUserAction.unknown);

            newIntent.putExtra("STATE", UtkMenuActivity.STATE_SECONDARY);
        }
        newIntent.setFlags(intentFlags);
        newIntent.putExtra(PHONE_ID, mActiveUtkId);
        mContext.startActivity(newIntent);
    }

    private void launchInputActivity() {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.setClassName(PACKAGE_NAME, INPUT_ACTIVITY_NAME);
        newIntent.putExtra("INPUT", mCurrentCmd.geInput());
        newIntent.putExtra(PHONE_ID, mActiveUtkId);
        mContext.startActivity(newIntent);
    }

    private void launchTextDialog() {
        Intent newIntent = new Intent(this, UtkDialogActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.putExtra("TEXT", mCurrentCmd.geTextMessage());
        newIntent.putExtra(PHONE_ID, mActiveUtkId);
        startActivity(newIntent);
    }

    private void launchEventMessage() {
        TextMessage msg = mCurrentCmd.geTextMessage();
        if (msg == null || msg.text == null) {
            return;
        }
        Toast toast = new Toast(mContext.getApplicationContext());
        LayoutInflater inflate = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.utk_event_msg, null);
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
            tv.setText(msg.text);
        }

        toast.setView(v);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    private void launchConfirmationDialog(TextMessage msg) {
        msg.title = lastSelectedItem;
        Intent newIntent = new Intent(this, UtkDialogActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.putExtra("TEXT", msg);
        startActivity(newIntent);
    }

    private void launchBrowser(BrowserSettings settings) {
        if (settings == null) {
            return;
        }
        // Set browser launch mode
        Intent intent = new Intent();
        intent.setClassName("com.android.browser",
                "com.android.browser.BrowserActivity");

        // to launch home page, make sure that data Uri is null.
        Uri data = null;
        if (settings.url != null) {
            UtkLog.d(this, "settings.url = " + settings.url);
            if ((settings.url.startsWith("http://") || (settings.url.startsWith("https://")))) {
                data = Uri.parse(settings.url);
            } else {
                String modifiedUrl = "http://" + settings.url;
                UtkLog.d(this, "modifiedUrl = " + modifiedUrl);
                data = Uri.parse(modifiedUrl);
            }
        } else {
            // If no URL specified, just bring up the "home page".
            //
            // (Note we need to specify *something* in the intent's data field
            // here, since if you fire off a VIEW intent with no data at all
            // you'll get an activity chooser rather than the browser.  There's
            // no specific URI that means "use the default home page", so
            // instead let's just explicitly bring up http://google.com.)
            data = Uri.parse("http://google.com/");
        }
        intent.setData(data);

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
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            break;
        }
        // start browser activity
        startActivity(intent);
        // a small delay, let the browser start, before processing the next command.
        // this is good for scenarios where a related DISPLAY TEXT command is
        // followed immediately.
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            //Follow Google original source code and need not handle this exception.
            //But maybe changed in future
        }
    }

    private void launchCallMsg() {
        Toast toast = Toast.makeText(mContext.getApplicationContext(), "Calling",
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    private void launchDataCallMsg(CharSequence txt) {
        Toast toast = Toast.makeText(mContext.getApplicationContext(), txt,
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    private void launchIdleText() {
        TextMessage msg = mCurrentCmd.geTextMessage();

        if (msg == null) {
            UtkLog.d(this, "mCurrent.getTextMessage is NULL");
            mNotificationManager.cancel(UTK_NOTIFICATION_ID);
            return;
        }
        if (msg.text == null) {
            mNotificationManager.cancel(UTK_NOTIFICATION_ID);
        } else {
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
                contentView.setImageViewBitmap(com.android.internal.R.id.icon,
                        msg.icon);
            } else {
                contentView
                        .setImageViewResource(
                                com.android.internal.R.id.icon,
                                com.android.internal.R.drawable.stat_notify_sim_toolkit);
            }
            notification.contentView = contentView;
            notification.contentIntent = PendingIntent.getService(mContext, 0,
                    new Intent(mContext, UtkAppService.class), 0);

            mNotificationManager.notify(UTK_NOTIFICATION_ID, notification);
        }
    }

    private void launchToneDialog() {
        Intent newIntent = new Intent(this, ToneDialog.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | getFlagActivityNoUserAction(InitiatedByUserAction.unknown));
        newIntent.putExtra("TEXT", mCurrentCmd.geTextMessage());
        newIntent.putExtra("TONE", mCurrentCmd.getToneSettings());
        startActivity(newIntent);
    }

    private String getItemName(int itemId) {
        Menu menu = mCurrentCmd.getMenu();
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

    private boolean removeMenu() {
        try {
            if (mCurrentMenu.items.size() == 1 &&
                mCurrentMenu.items.get(0) == null) {
                UtkLog.d(this, "mCurrentMenu.items.size() == 1 or items.get(0) == null");
                return true;
            }
        } catch (NullPointerException e) {
            UtkLog.d(this, "Unable to get Menu's items size");
            return true;
        }
        return false;
    }

    private boolean isBusyOnCall() {
        PhoneConstants.State s = getCallState();
        //Temp solution in order to build pass
        //s = PhoneConstants.State.IDLE;
        //if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
        //    s = ((GeminiPhone) mPhone).getState();
        //} else {
        //    s = mPhone.getState();
        //}

        UtkLog.d(this, "isBusyOnCall: " + s);
        return (s == PhoneConstants.State.RINGING);
    }

    private Phone getPhone(int slotId) {
        UtkLog.d(this, "getPhone slotId: " + slotId);
        if (mPhone[slotId] != null) {
            return mPhone[slotId];
        }

        int subId[] = SubscriptionManager.getSubId(slotId);
        int phoneId = 0;
        if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
            phoneId = SubscriptionManager.getPhoneId(subId[0]);
            UtkLog.d(this, "subId[0] " + subId[0] + " phoneId: " + phoneId);
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                UtkLog.d(this, "ERROR: invalid phone id.");
                return null;
            }
        } else {
            //FIXME
            if (subId == null) {
                UtkLog.d(this, "ERROR: sub array is null.");
            } else {
                UtkLog.d(this, "ERROR: sub id is invalid. sub id: " + subId[0]);
            }
            return null; //should return null and caller should handle null object case.
        }
        UtkLog.d(this, "getPhone done.");
        return PhoneFactory.getPhone(phoneId);
    }
    private PhoneConstants.State getCallState() {
        Phone phone = null;
        for (int i = 0; i < mSimCount; i++) {
            phone = getPhone(i);
            if (phone == null) {
                UtkLog.d(this, "Phone is null.");
                continue;
            }
            PhoneConstants.State ps = phone.getState();
            UtkLog.d(this, "Phone " + i + " state: " + ps);
            if (ps == PhoneConstants.State.RINGING) {
                return PhoneConstants.State.RINGING;
            }
        }
        return PhoneConstants.State.IDLE;
    }

    boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            UtkLog.d(this, "fail to get airlane mode");
            mode = 0;
        }

        UtkLog.d(this, "airlane mode is " + mode);
        return (mode != 0);
    }
    
    private RoamingMode getRoamingState(int phoneId) {
        SvlteRatController sInstance = null;
        try {
            //sInstance = SvlteRatController.getInstance();
            LteDcPhoneProxy lteDcPhoneProxy = (LteDcPhoneProxy) PhoneFactory.getPhone(phoneId);
            if (lteDcPhoneProxy != null) {
                sInstance = lteDcPhoneProxy.getSvlteRatController();
            }
        } catch (RuntimeException rex) {
            UtkLog.d(this, "fail to get SvlteRatController instance: RuntimeException");
            sInstance = null;
        } catch (Exception ex) {
            UtkLog.d(this, "fail to get SvlteRatController instance: Exception");
            sInstance = null;
        }  
        if (null != sInstance) {
            mRoamingMode = sInstance.getRoamingMode();
            UtkLog.d(this, "getRoamingState: mRoamingMode = " + mRoamingMode);
        }
        return mRoamingMode;
    }

    private final BroadcastReceiver mSIMStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int simId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                //int simId = intent.getIntExtra(
                //        com.android.internal.telephony.PhoneConstants.GEMINI_SIM_ID_KEY, -1);

                UtkLog.d(this, "mSIMStateChangeReceiver() - simId[" + simId + "]  state["
                        + simState + "]");
                if (simId == mActiveUtkId) {
                    if ((IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)) ||
                        (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState))) {
                        UtkLog.d(this, "Utk launch MenuActivity finish!");
                        mCanLaunchStkMenuActivity = false;
                        mCurrentMenu = null;
                        //hot swap, when plug out UIM card, reset mMainCmd into null.
                        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) {
                            mMainCmd = null;
                        }
                    } else {
                        mCanLaunchStkMenuActivity = true;
                    }
                }
            }
        }
    };
}
