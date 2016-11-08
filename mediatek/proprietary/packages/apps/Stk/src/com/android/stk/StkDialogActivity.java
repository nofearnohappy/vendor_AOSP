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

import com.android.internal.telephony.cat.TextMessage;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneFactory;

//Stk modification for TDD data only
import android.database.ContentObserver;
import android.net.Uri;

import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.telephony.TelephonyManagerEx;
/**
 * AlretDialog used for DISPLAY TEXT commands.
 *
 */
public class StkDialogActivity extends Activity implements View.OnClickListener {
    // members
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);
    //keys) for saving the state of the dialog in the icicle
    private static final String TEXT = "text";
    TextMessage mTextMsg = null;
    protected boolean mIsResponseSent = false;
    private int mSlotId = -1;
    private String mStkSource = null;
    private Context mContext = null;
    private StkAppService appService = StkAppService.getInstance();
    // Utilize AlarmManager for real-time countdown
    private PendingIntent mTimeoutIntent = null;
    private AlarmManager mAlarmManager = null;
    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;
    private boolean mIsRegisterReceiverDone = false;
    // buttons id
    public static final int OK_BUTTON = R.id.button_ok;
    public static final int CANCEL_BUTTON = R.id.button_cancel;
    protected static final int MIN_LENGTH = 6;
    protected static final int MIN_WIDTH = 170;

    private HandlerThread mTimeoutThread = null;
    private TimeoutHandler mTimeoutHandler = null;
    private TimeoutReceiver mTimeoutReceiver = null;
    private String sOperatorSpec = SystemProperties.get("ro.operator.optr", "OM");
    private static final String ALARM_TIMEOUT = "android.stkDialog.TIMEOUT";

    //Stk modification for TDD data only
    private ContentObserver m4GDataModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            CatLog.d(this, "4G data only mode changed!");
            int subId[] = SubscriptionManager.getSubId(mSlotId);
            CatLog.d(this, "subId: " + subId[0] + " for slotId: " + mSlotId);
            int patternLteDataOnly = Settings.Global.getInt(mContext.getContentResolver(),
                                    TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId[0]),
                                    //Settings.Global.LTE_ON_CDMA_RAT_MODE,
                                    TelephonyManagerEx.SVLTE_RAT_MODE_4G);
            CatLog.d(this, "patternLteDataOnly = " + patternLteDataOnly);
            if (TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY == patternLteDataOnly) {
                CatLog.d(this, "StkMenuActivity.finish()");
                cancelTimeOut();
                finish();
            }
        }
    };

    private final IntentFilter mSIMStateChangeFilter =
            new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);

    private final BroadcastReceiver mSIMStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);

                CatLog.d(LOG_TAG, "mSIMStateChangeReceiver() - slotId[" + slotId +
                        "]  state[" + simState + "], mSlotId: " + mSlotId);
                if ((slotId == mSlotId) &&
                    ((IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) ||
                    (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)))) {
                    if (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)) {
                        showTextToast(getApplicationContext(),
                        getString(R.string.lable_sim_not_ready));
                    }
                    cancelTimeOut();
                    mIsResponseSent = true;
                    finish();
                }
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
            CatLog.d(LOG_TAG, "mAirplaneModeReceiver AIRPLANE_MODE_CHANGED: " +
            airplaneModeEnabled);
            if (airplaneModeEnabled) {
                mIsResponseSent = true;
                cancelTimeOut();
                finish();
            }
        }
    };

    //Add for Stk modification IR case
    private final IntentFilter mIRStateChangeFilter = new IntentFilter();

    private final BroadcastReceiver mIRStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((SvlteRatController.INTENT_ACTION_START_SWITCH_ROAMING_MODE.
                    equals(intent.getAction())) ||
                (SvlteRatController.INTENT_ACTION_FINISH_SWITCH_ROAMING_MODE.
                        equals(intent.getAction()))) {
                CatLog.d(this, "received IR state changed broadcast");
                int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                if (SubscriptionManager.getSlotId(subId) != mSlotId) {
                    CatLog.d(this, "Ignore switch roaming mode intent.");
                    return;
                }
                //Query RAT Mode
                LteDcPhoneProxy lteDcPhoneProxy = (LteDcPhoneProxy) PhoneFactory.getPhone(mSlotId);
                SvlteRatController lteRatController = null;
                if (lteDcPhoneProxy != null) {
                    lteRatController = lteDcPhoneProxy.getSvlteRatController();
                }
                RoamingMode sRoamingMode = null;
                if (lteRatController != null) {
                    sRoamingMode = lteRatController.getRoamingMode();
                    CatLog.d(this, "mIRStateChangeReceiver sRoamingMode = " + sRoamingMode);
                    if (RoamingMode.ROAMING_MODE_NORMAL_ROAMING != sRoamingMode) {
                        cancelTimeOut();
                        finish();
                    }
                }
            }
        }
    };

    private boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        CatLog.d(LOG_TAG, "onCreate");
        initFromIntent(getIntent());
        if (mTextMsg == null) {
            mIsRegisterReceiverDone = false;
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        Window window = getWindow();

        setContentView(R.layout.stk_msg_dialog);
        TextView mMessageView = (TextView) window
                .findViewById(R.id.dialog_message);

        Button okButton = (Button) findViewById(R.id.button_ok);
        Button cancelButton = (Button) findViewById(R.id.button_cancel);
        mContext = getBaseContext();

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        setTitle(mTextMsg.title);
        if (!(mTextMsg.iconSelfExplanatory && mTextMsg.icon != null)) {
            if ((mTextMsg.text == null) || (mTextMsg.text.length() < MIN_LENGTH)) {
                mMessageView.setMinWidth(MIN_WIDTH);
            }
            mMessageView.setText(mTextMsg.text);
        }

        if (mTextMsg.icon == null) {
            CatLog.d(LOG_TAG, "onCreate icon is null");
            window.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                    com.android.internal.R.drawable.stat_notify_sim_toolkit);
        } else {
            window.setFeatureDrawable(Window.FEATURE_LEFT_ICON,
                    new BitmapDrawable(mTextMsg.icon));
        }
        //clear optionmenu in stkDialog activity
        // L-MR1
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.needsMenuKey = WindowManager.LayoutParams.NEEDS_MENU_SET_FALSE;
        getWindow().setAttributes(lp);
        //window.setNeedsMenuKey(WindowManager.LayoutParams.NEEDS_MENU_SET_FALSE);
        registerReceiver(mSIMStateChangeReceiver, mSIMStateChangeFilter);
        registerReceiver(mAirplaneModeReceiver, mAirplaneModeFilter);

        if (mAlarmManager == null) {
            if (mContext != null) {
                CatLog.d(LOG_TAG, "get mAlarmManager");
                mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            } else {
                CatLog.d(LOG_TAG, "mContext is null");
            }
        }
        if (mTimeoutReceiver == null) {
            CatLog.d(LOG_TAG, "new TimeoutReceiver");
            mTimeoutReceiver = new TimeoutReceiver();
        }
        IntentFilter filter = new IntentFilter(ALARM_TIMEOUT);
        CatLog.d(LOG_TAG, "registerReceiver");
        registerReceiver(mTimeoutReceiver, filter);
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            CatLog.d(this, "register 4G data only observer");
            register4GDataModeObserver();
            //Stk modification for IR
            mIRStateChangeFilter.addAction(SvlteRatController.
                    INTENT_ACTION_START_SWITCH_ROAMING_MODE);
            mIRStateChangeFilter.addAction(SvlteRatController.
                    INTENT_ACTION_FINISH_SWITCH_ROAMING_MODE);
            registerReceiver(mIRStateChangeReceiver, mIRStateChangeFilter);
        }

        mIsRegisterReceiverDone = true;

        // Set a new task description to change icon
        if (sOperatorSpec.equals("OP02") && PhoneConstants.SIM_ID_1 < mSlotId) {
            setTaskDescription(new ActivityManager.TaskDescription(null,
            BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_launcher_sim2_toolkit)));
        }
    }

    public void onClick(View v) {
        String input = null;

        switch (v.getId()) {
        case OK_BUTTON:
            if (appService != null) {
                CatLog.d(LOG_TAG, "OK Clicked! isCurCmdSetupCall[" +
                        appService.isCurCmdSetupCall(mSlotId) + "], mSlotId: " + mSlotId);
            }
            if ((appService != null) && appService.isCurCmdSetupCall(mSlotId)) {
                CatLog.d(LOG_TAG, "dailStkCall");
                appService.dailStkCall(mSlotId);
            }
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_CONFIRM, true);
            break;
        case CANCEL_BUTTON:
            CatLog.d(LOG_TAG, "Cancel Clicked!, mSlotId: " + mSlotId);
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_CONFIRM, false);
            break;
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        CatLog.d(LOG_TAG, "onNewIntent - mIsResponseSent[" + mIsResponseSent + "]" + ", mSlotId: " + mSlotId);
        initFromIntent(intent);
        if (mTextMsg == null) {
            finish();
            return;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CatLog.d(LOG_TAG, "onKeyDown - keyCode:" + keyCode);    
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            CatLog.d(LOG_TAG, "onKeyDown - KEYCODE_BACK");
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_BACKWARD);
            finish();
            break;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        CatLog.d(LOG_TAG, "onResume - mIsResponseSent[" + mIsResponseSent +
                "] mTextMsg.responseNeeded: " + mTextMsg.responseNeeded +
                " sim id: " + mSlotId);
        //For performance auto test case, do not delete this log.
        CatLog.d(LOG_TAG, "Stk_Performance time: " + SystemClock.elapsedRealtime());
        if (appService != null) {
            appService.indicateDialogVisibility(true, mSlotId);
        } else {
           CatLog.d(LOG_TAG, "onPause, appService is null.");
           mIsResponseSent = true;//Skip TR since this is not a real activity triggered from sim.
           showTextToast(getApplicationContext(), getString(R.string.lable_not_available));
           finish();
           return;
        }

        // For Immediate Response case
        if (false == mTextMsg.responseNeeded && null != appService) {
            appService.getStkContext(mSlotId).setPendingDialogInstance(this);
        }
        /*
         * When another activity takes the foreground, we do not want the Terminal
         * Response timer to be restarted when our activity resumes. Hence we will
         * check if there is an existing timer, and resume it. In this way we will
         * inform the SIM in correct time when there is no response from the User
         * to a dialog.
         */
        if (mTimeoutIntent != null) {
            CatLog.d(LOG_TAG, "Pending Alarm! Let it finish counting down...");
        } else {
            CatLog.d(LOG_TAG, "No Pending Alarm! OK to start timer...");
            startTimeOut(mTextMsg.userClear);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CatLog.d(LOG_TAG, "onPause, sim id: " + mSlotId);
        if (appService != null) {
            appService.indicateDialogVisibility(false, mSlotId);
        }
        /* For operator lab test, cancelTimeOut() should be removed.
           When HOME key pressed, the timer should be counted continually for sending TR.*/
        /*
         * do not cancel the timer here cancelTimeOut(). If any higher/lower
         * priority events such as incoming call, new sms, screen off intent,
         * notification alerts, user actions such as 'User moving to another activtiy'
         * etc.. occur during Display Text ongoing session,
         * this activity would receive 'onPause()' event resulting in
         * cancellation of the timer. As a result no terminal response is
         * sent to the card.
         */
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsResponseSent = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        CatLog.d(LOG_TAG, "onStop - before Send CONFIRM false mIsResponseSent[" +
                mIsResponseSent + "], sim id: " + mSlotId);
        if (null == appService) {
            CatLog.d(LOG_TAG, "null appService");
            return;
        }
        if (null == appService.getStkContext(mSlotId)) {
            CatLog.d(LOG_TAG, "null stk context");
            return;
        }

        // Display Text with Immediate Response has 3 cases to be clean.
        // In these 3 cases, we don't want setPendingDialogInstance(this) to be
        // called in onStop(). Below are 3 cases:
        // 1. Clean by user click button, mIsResponseSent is set as true in sendResponse().
        // 2. Clean by timer expiration, mIsResponseSent is set as true in sendResponse().
        // 3. Clean by "dialog.finish()" in StkAppService, mIsResponseSent is flase.
        if (!mIsResponseSent && true == mTextMsg.responseNeeded) {
            appService.getStkContext(mSlotId).setPendingDialogInstance(this);
            //sendResponse(StkAppService.RES_ID_CONFIRM, false);
            //finish();
        } else {
            CatLog.d(LOG_TAG, "finish.");
            appService.getStkContext(mSlotId).setPendingDialogInstance(null);
            cancelTimeOut();
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CatLog.d(LOG_TAG, "onDestroy - before Send CONFIRM false mIsResponseSent[" + mIsResponseSent +
                "], sim id: " + mSlotId);
        if (!mIsResponseSent) {
            if (null == appService) {
                //To get instance again, if stkappservice has created before onDestroy.
                appService = StkAppService.getInstance();
            }
            if (null != appService) {
                if (!appService.isDialogPending(mSlotId)) {
                    CatLog.d(LOG_TAG, "handleDestroy - Send false confirm.");
                    sendResponse(StkAppService.RES_ID_CONFIRM, false);
                }
            }
        }
        if (appService != null) {
            appService.indicateDialogVisibility(false, mSlotId);
        }
        cancelTimeOut();
        if (mIsRegisterReceiverDone) {
            unregisterReceiver(mTimeoutReceiver);
            unregisterReceiver(mSIMStateChangeReceiver);
            unregisterReceiver(mAirplaneModeReceiver);
            if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
                unregister4GDataModeObserver();
                //Stk modification for IR
                unregisterReceiver(mIRStateChangeReceiver);
            }
        }
        mTimeoutReceiver = null;
        mAlarmManager = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        CatLog.d(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(outState);
        outState.putParcelable(TEXT, mTextMsg);
        outState.putBoolean("RESPONSE_SENT", mIsResponseSent);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTextMsg = savedInstanceState.getParcelable(TEXT);
        CatLog.d(LOG_TAG, "onRestoreInstanceState - [" + mTextMsg + "]");
        mIsResponseSent = savedInstanceState.getBoolean("RESPONSE_SENT");
    }

    private void sendResponse(int resId) {
        sendResponse(resId, true);
    }

    private void initFromIntent(Intent intent) {

        if (intent != null) {
            mTextMsg = intent.getParcelableExtra("TEXT");
            mSlotId = intent.getIntExtra(StkAppService.SLOT_ID, -1);
            mStkSource = intent.getStringExtra(StkAppService.STK_SOURCE_KEY);
            if (appService != null) {
                if (!appService.isValidStkSourceKey(mStkSource)) {
                    mIsResponseSent = true;
                    if (!(TelephonyManager.getDefault().hasIccCard(mSlotId))) {
                        showTextToast(getApplicationContext(),
                                getString(R.string.no_sim_card_inserted));
                    }
                    finish();
                    appService.restoreCurrentCmd(mSlotId);
                    return;
                }
            } else {
                 CatLog.d(LOG_TAG, "appService is null!");
                 mIsResponseSent = true;
                 finish();
                 return;
            }
        } else {
            finish();
        }

        CatLog.d(LOG_TAG, "initFromIntent - [" + mTextMsg + "], sim id: " + mSlotId);
    }
    private void cancelTimeOut() {
        CatLog.d(LOG_TAG, "cancelTimeOut: " + mSlotId);
        if (mTimeoutIntent != null) {
            CatLog.d(LOG_TAG, "mAlarmManager cancel");
            if (null !=  mAlarmManager) {
                mAlarmManager.cancel(mTimeoutIntent);
            }
        }
    }

    private void startTimeOut(boolean waitForUserToClear) {
        // Reset timeout.
        cancelTimeOut();
        int dialogDuration = StkApp.calculateDurationInMilis(mTextMsg.duration);
        // case 1  userClear = true & responseNeeded = false,
        // Dialog always exists.
        if (mTextMsg.userClear == true && mTextMsg.responseNeeded == false &&
            0 == dialogDuration) {
            return;
        } else {
            // userClear = false. will dissapear after a while.
            if (dialogDuration == 0) {
                if (waitForUserToClear) {
                    dialogDuration = StkApp.DISP_TEXT_WAIT_FOR_USER_TIMEOUT;
                } else {
                    dialogDuration = StkApp.DISP_TEXT_CLEAR_AFTER_DELAY_TIMEOUT;
                }
            }
            Intent mAlarmIntent = new Intent(ALARM_TIMEOUT, null);
            mAlarmIntent.putExtra(StkAppService.SLOT_ID, mSlotId);
            mTimeoutIntent = PendingIntent.getBroadcast(mContext, 0, mAlarmIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // Try to use a more stringent timer not affected by system sleep.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + dialogDuration, mTimeoutIntent);
            } else {
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + dialogDuration, mTimeoutIntent);
            }
            CatLog.d(LOG_TAG, "startTimeOut: " + mSlotId);
        }
    }
    private void sendResponse(int resId, boolean confirmed) {
        if (mSlotId == -1) {
            CatLog.d(LOG_TAG, "sim id is invalid");
            return;
        }

        if (StkAppService.getInstance() == null) {
            CatLog.d(LOG_TAG, "Ignore response: id is " + resId);
            return;
        }

        CatLog.d(LOG_TAG, "sendResponse resID[" + resId + "] confirmed[" + confirmed + "]");

        mIsResponseSent = true;
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_RESPONSE);
        args.putInt(StkAppService.SLOT_ID, mSlotId);
        args.putInt(StkAppService.RES_ID, resId);
        args.putBoolean(StkAppService.CONFIRMATION, confirmed);
        startService(new Intent(this, StkAppService.class).putExtras(args));
    }

    /*Handler base on timeoutThread Looper.*/
    private class TimeoutHandler extends Handler {
        public TimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_ID_TIMEOUT:
                CatLog.d(LOG_TAG, "MSG_ID_TIMEOUT finish.");
                sendResponse(StkAppService.RES_ID_TIMEOUT);
                finish();
                break;
            }
        }
    }
    private class TimeoutReceiver extends BroadcastReceiver {
    
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int slotID = intent.getIntExtra(StkAppService.SLOT_ID, 0);
            CatLog.d(LOG_TAG, "TimeoutReceiver onReceive");
            if (action == null || slotID != mSlotId) {
                return;
            }
            CatLog.d(LOG_TAG, "onReceive, action=" + action + ", sim id: " + slotID);
            if (action.equals(ALARM_TIMEOUT)) {
                CatLog.d(LOG_TAG, "ALARM_TIMEOUT rcvd");
                mTimeoutIntent = null;
                sendResponse(StkAppService.RES_ID_TIMEOUT);
                finish();
            }
        }
    }

    void showTextToast(Context context, String msg) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
    //Stk modification for TDD data only
    private void register4GDataModeObserver() {
        CatLog.d(this, "register 4G data only observer");
        int subId[] = SubscriptionManager.getSubId(mSlotId);
        CatLog.d(this, "subId: " + subId[0] + " for slotId: " + mSlotId);
        Uri uri = Settings.Global.getUriFor(
            TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId[0]));
        mContext.getContentResolver().registerContentObserver(
            uri, true, m4GDataModeObserver);
    }

    private void unregister4GDataModeObserver() {
        CatLog.d(this, "unregister 4G data only observer");
        mContext.getContentResolver().unregisterContentObserver(
                m4GDataModeObserver);
    }
}
