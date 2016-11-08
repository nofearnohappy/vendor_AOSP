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

import android.app.ListActivity;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneFactory;

import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;

import android.view.Gravity;
import android.widget.Toast;
import java.lang.System;

//Stk modification for TDD data only
import android.database.ContentObserver;
import android.net.Uri;

import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
import com.mediatek.telephony.TelephonyManagerEx;
/**
 * ListActivity used for displaying STK menus. These can be SET UP MENU and
 * SELECT ITEM menus. This activity is started multiple times with different
 * menu content.
 *
 */
public class StkMenuActivity extends ListActivity {
    private TextView mTitleTextView = null;
    private ImageView mTitleIconView = null;
    private ProgressBar mProgressView = null;
    private String mOptr = SystemProperties.get("ro.operator.optr", "NONE");
    private final String className = this.toString();
    private final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);
    private Menu mStkMenu = null;
    private int mState = STATE_MAIN;
    private boolean mAcceptUsersInput = true;
    private Context mContext = null;
    private int mSlotId = -1;
    private String mStkSource = null;
    private StkAppService appService = StkAppService.getInstance();
    private boolean mIsResponseSent = false;
    Activity mInstance = null;
    // Internal state values
    static final int STATE_INIT = 0;
    static final int STATE_MAIN = 1;
    static final int STATE_SECONDARY = 2;

    // Finish result
    static final int FINISH_CAUSE_NORMAL = 1;
    static final int FINISH_CAUSE_FLIGHT_MODE = 2;
    static final int FINISH_CAUSE_NULL_SERVICE = 3;
    static final int FINISH_CAUSE_NULL_MENU = 4;
    static final int FINISH_CAUSE_NOT_AVAILABLE = 5;
    static final int FINISH_CAUSE_SIM_REMOVED = 6;

    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;
    private boolean mIsRegisterReceiverDone = false;

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_ID_TIMEOUT:
                    CatLog.d(LOG_TAG, "MSG_ID_TIMEOUT mState: " + mState);
                    mAcceptUsersInput = false;
                    if (mState == STATE_SECONDARY) {
                        appService.getStkContext(mSlotId).setPendingActivityInstance(mInstance);
                    }
                    sendResponse(StkAppService.RES_ID_TIMEOUT);
                    //finish();//We wait the following commands to trigger onStop of this activity.
                    break;
            }
        }
    };

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

                CatLog.d(LOG_TAG, "mSIMStateChangeReceiver() - slotId[" +
                        slotId + "]  state[" + simState + "]");
                if ((slotId == mSlotId) &&
                    ((IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) ||
                     (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)))) {
                        if (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState) &&
                            (appService != null &&
                             appService.getStkContext(slotId).mMenuIsVisible)) {
                            showTextToast(getApplicationContext(),
                            getString(R.string.lable_sim_not_ready));
                        }
                    CatLog.d(LOG_TAG, "mIsResponseSent: " + mIsResponseSent);
                    mIsResponseSent = true;
                    cancelTimeOut();
                    finish();
                }
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

    private final IntentFilter mMainMenuAccessFilter = new IntentFilter(
        StkAppService.RESET_MAIN_MENU_ACCESS);

    private BroadcastReceiver mMainMenuAccessReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CatLog.d(LOG_TAG, "RESET_MAIN_MENU_ACCESS, mState: " + mState);
            if (STATE_MAIN == mState) {
                mAcceptUsersInput = true;
            }
        }
    };

    private boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        CatLog.d(LOG_TAG, "onCreate+");
        //To enable 3 dots(overflow) menu.
        getApplicationInfo().targetSdkVersion = 10;
        // Remove the default title, customized one is used.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set the layout for this activity.
        setContentView(R.layout.stk_menu_list);
        mInstance = this;
        mTitleTextView = (TextView) findViewById(R.id.title_text);
        mTitleIconView = (ImageView) findViewById(R.id.title_icon);
        mProgressView = (ProgressBar) findViewById(R.id.progress_bar);
        mContext = getBaseContext();
        mAcceptUsersInput = true;
        getListView().setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        int result = initFromIntent(getIntent());
        if (StkAppService.STK_VALID_SOURCE != result) {
            CatLog.d(LOG_TAG, "finish!");
            //If it is launched from recent app for the last power on.
            if (StkAppService.STK_INVALID_SOURCE == result) {
                mIsResponseSent = true; //Skip to send TR.
            }
            mIsRegisterReceiverDone = false;
            if (!(TelephonyManager.getDefault().hasIccCard(mSlotId))) {
                showTextToast(getApplicationContext(), getString(R.string.no_sim_card_inserted));
            } else if (null != appService && null != appService.getStkContext(mSlotId) &&
                null == appService.getStkContext(mSlotId).mCurrentCmd) {
                showTextToast(getApplicationContext(), getString(R.string.lable_sim_not_ready));
            }
            finish();
            if (null != appService) {
                if (null == appService.getStkContext(mSlotId)) {
                    CatLog.d(LOG_TAG, "Null stk context!");
                    return;
                }
                // To avoid the previous onDestroy to reset the Main menu instance of this one,
                // and than the end session arrived, the main menu will be created again.
                appService.getStkContext(mSlotId).mRestoreMainMenu = true;
                appService.restoreCurrentCmd(mSlotId);
            }
            return;
        }

        // Set a new task description to change icon
        if (mOptr.equals("OP02") && PhoneConstants.SIM_ID_1 < mSlotId) {
            setTaskDescription(new ActivityManager.TaskDescription(null,
            BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_launcher_sim2_toolkit)));
        }
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
        registerReceiver(mSIMStateChangeReceiver, mSIMStateChangeFilter);
        registerReceiver(mAirplaneModeReceiver, mAirplaneModeFilter);
        registerReceiver(mMainMenuAccessReceiver, mMainMenuAccessFilter);
        if (null != appService) {
            appService.indicateMenuAlive(this, true, mSlotId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        CatLog.d(LOG_TAG, "onNewIntent");
        mAcceptUsersInput = true;
        mIsResponseSent = false;
        if (StkAppService.STK_VALID_SOURCE != initFromIntent(intent)) {
            CatLog.d(LOG_TAG, "finish!");
            finish();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return;
        }

        Item item = getSelectedItem(position);
        if (item == null) {
            CatLog.d(LOG_TAG, "Item is null");
            return;
        }

        CatLog.d(LOG_TAG, "onListItemClick Id: " + item.id + ", mState: " + mState);
        // ONLY set SECONDARY menu. It will be finished when the following command is comming.
        if (mState == STATE_SECONDARY) {
            appService.getStkContext(mSlotId).setPendingActivityInstance(this);
        }
        //else { //STATE_MAIN
        //    appService.getStkContext(mSlotId).setMainActivityInstance(this);
        //}
        cancelTimeOut();
        sendResponse(StkAppService.RES_ID_MENU_SELECTION, item.id, false);
        mAcceptUsersInput = false;
        mProgressView.setVisibility(View.VISIBLE);
        mProgressView.setIndeterminate(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CatLog.d(LOG_TAG, "mAcceptUsersInput: " + mAcceptUsersInput);
        if (!mAcceptUsersInput) {
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                CatLog.d(LOG_TAG, "KEYCODE_BACK - mState[" + mState + "]");
                switch (mState) {
                    case STATE_SECONDARY:
                        CatLog.d(LOG_TAG, "STATE_SECONDARY");
                        cancelTimeOut();
                        mAcceptUsersInput = false;
                        appService.getStkContext(mSlotId).setPendingActivityInstance(this);
                        sendResponse(StkAppService.RES_ID_BACKWARD);
                        return true;
                    case STATE_MAIN:
                        CatLog.d(LOG_TAG, "STATE_MAIN");
                        appService.getStkContext(mSlotId).setMainActivityInstance(null);
                        //We send TR normally for the main menu, but the TR wiil be rejected in StkAppService.
                        //This is used to let StkAppService to know the user exit from main.
                        //sendResponse(StkAppService.RES_ID_BACKWARD);
                        cancelTimeOut();
                        finish();
                        return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onRestart() {
         super.onRestart();
         CatLog.d(LOG_TAG, "onRestart, slot id: " + mSlotId);
    }
    @Override
    public void onResume() {
        super.onResume();

        CatLog.d(LOG_TAG, "onResume, slot id: " + mSlotId + "," + mState);
        int res = FINISH_CAUSE_NORMAL;

        do {
            res = onResumePreConditionCheck(mSlotId);
            if (res != FINISH_CAUSE_NORMAL) {
                CatLog.d(LOG_TAG, "onResume get fail cause: " + res);
                break;
            }
            appService.indicateMenuVisibility(true, mSlotId);
            if (mState == STATE_MAIN) {
                mStkMenu = appService.getMainMenu(mSlotId);
            } else {
                mStkMenu = appService.getMenu(mSlotId);
            }
            if (mStkMenu == null) {
                res = FINISH_CAUSE_NULL_MENU;
                break;
            }
            //Set main menu instance here for clean up stack by other SIMs
            //when receiving OP_LAUNCH_APP.
            if (mState == STATE_MAIN) {
                CatLog.d(LOG_TAG, "set main menu instance.");
                appService.getStkContext(mSlotId).setMainActivityInstance(this);
            }
            displayMenu(mTitleIconView, mTitleTextView, this);
            startTimeOut();
            // whenever this activity is resumed after a sub activity was invoked
            // (Browser, In call screen) switch back to main state and enable
            // user's input;
            if (STATE_MAIN == mState && !mAcceptUsersInput) {
                mAcceptUsersInput = true;
            }

            // make sure the progress bar is not shown.
            mProgressView.setIndeterminate(false);
            mProgressView.setVisibility(View.GONE);
        } while(false);

        CatLog.d(LOG_TAG, "handleResume, result: " + res);

        switch(res) {
            case FINISH_CAUSE_FLIGHT_MODE:
                showTextToast(getApplicationContext(), getString(R.string.lable_on_flight_mode));
                cancelTimeOut();
                finish();
                break;
            case FINISH_CAUSE_NOT_AVAILABLE:
                showTextToast(getApplicationContext(), getString(R.string.lable_sim_not_ready));
                cancelTimeOut();
                finish();
                break;
            case FINISH_CAUSE_SIM_REMOVED:
                showTextToast(getApplicationContext(), getString(R.string.no_sim_card_inserted));
                cancelTimeOut();
                finish();
                break;
            case FINISH_CAUSE_NULL_MENU:
                CatLog.d(LOG_TAG, "menu is null");
                showTextToast(getApplicationContext(), getString(R.string.main_menu_not_initialized));
                cancelTimeOut();
                finish();
                break;
            case FINISH_CAUSE_NULL_SERVICE:
                CatLog.d(LOG_TAG, "app service is null");
                cancelTimeOut();
                finish();
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CatLog.d(LOG_TAG, "onPause, slot id: " + mSlotId + "," + mState);
        //If activity is finished in onResume and it reaults from null appService.
        if (appService != null) {
            appService.indicateMenuVisibility(false, mSlotId);
        } else {
            CatLog.d(LOG_TAG, "onPause: null appService.");
        }

        /*
         * do not cancel the timer here cancelTimeOut(). If any higher/lower
         * priority events such as incoming call, new sms, screen off intent,
         * notification alerts, user actions such as 'User moving to another activtiy'
         * etc.. occur during SELECT ITEM ongoing session,
         * this activity would receive 'onPause()' event resulting in
         * cancellation of the timer. As a result no terminal response is
         * sent to the card.
         */

    }

    @Override
    public void onStop() {
        super.onStop();
        CatLog.d(LOG_TAG, "onStop, slot id: " + mSlotId + "," + mIsResponseSent + "," + mState);
        if (null == appService) {
            CatLog.d(LOG_TAG, "null appService");
            return;
        }
        if (null == appService.getStkContext(mSlotId)) {
            CatLog.d(LOG_TAG, "null stk context");
            return;
        }
        //The menu should stay in background, if
        //1. the dialog is pop up in the screen, but the user does not response to the dialog.
        //2. the menu activity enters Stop state (e.g pressing HOME key) but mIsResponseSent is false.
        if (mIsResponseSent) {
            // ONLY finish SECONDARY menu. MAIN menu should always stay in the root of stack.
            if (mState == STATE_SECONDARY) {
                if (!appService.isStkDialogActivated(mContext)) {
                    CatLog.d(LOG_TAG, "STATE_SECONDARY finish.");
                    cancelTimeOut();//To avoid the timer time out and send TR again.
                    finish();
                } else {
                     appService.getStkContext(mSlotId).setPendingActivityInstance(this);
                }
            }
        } else {
            appService.getStkContext(mSlotId).setPendingActivityInstance(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CatLog.d(LOG_TAG, "onDestroy" + " , " + mState + " , " + mIsResponseSent);
        if (null != appService) {
            appService.indicateMenuAlive(this, false, mSlotId);
        }
        //isMenuPending: if menu act is finish by stkappservice when OP_LAUNCH_APP again,
        //we can not send TR here, since the menu cmd is waiting user to process.
        cancelTimeOut();
        if (!mIsResponseSent) {
            if (null == appService) {
                //To get instance again, if stkappservice has created before onDestroy.
                appService = StkAppService.getInstance();
            }
            if (null != appService) {
                if (!appService.isMenuPending(mSlotId)) {
                    CatLog.d(LOG_TAG, "handleDestroy - Send End Session");
                    sendResponse(StkAppService.RES_ID_END_SESSION);
                }
            }
        }
        if (mState == STATE_MAIN) {
            if (appService != null) {
                if (null != appService.getStkContext(mSlotId)) {
                    CatLog.d(LOG_TAG, "mRestoreMainMenu: " +
                            appService.getStkContext(mSlotId).mRestoreMainMenu);
                    if (false == appService.getStkContext(mSlotId).mRestoreMainMenu) {
                        appService.getStkContext(mSlotId).setMainActivityInstance(null);
                } else {
                        appService.getStkContext(mSlotId).mRestoreMainMenu = false;
                    }
                } else {
                    CatLog.d(LOG_TAG, "onDestroy: null stkcontext.");
                }
            } else {
                CatLog.d(LOG_TAG, "onDestroy: null appService.");
            }
        }
        if (mIsRegisterReceiverDone) {
            unregisterReceiver(mSIMStateChangeReceiver);
            unregisterReceiver(mAirplaneModeReceiver);
            unregisterReceiver(mMainMenuAccessReceiver);
            if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
                unregister4GDataModeObserver();
                //Stk modification for IR
                unregisterReceiver(mIRStateChangeReceiver);
            }
        }
    }

    // For long click menu
    private final OnCreateContextMenuListener mOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            boolean helpVisible = false;
            if (mStkMenu != null) {
                helpVisible = mStkMenu.helpAvailable;
            }
            CatLog.d(LOG_TAG, "OnCreateContextMenuListener, helpVisible: " + helpVisible);
            if (helpVisible == true) {
                menu.add(0, StkApp.MENU_ID_HELP, 0, R.string.help);
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info =
            (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case StkApp.MENU_ID_HELP:
            cancelTimeOut();
            mAcceptUsersInput = false;
            Item stkItem = getSelectedItem(info.position);
            if (stkItem == null) {
                break;
            }
            appService.getStkContext(mSlotId).setPendingActivityInstance(this);
            // send help needed response.
            sendResponse(StkAppService.RES_ID_MENU_SELECTION, stkItem.id, true);
            return true;
        default:
            break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        CatLog.d(LOG_TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu);
        menu.add(0, StkApp.MENU_ID_END_SESSION, 1, R.string.menu_end_session);
        menu.add(0, StkApp.MENU_ID_HELP, 2, R.string.help);
        menu.add(0, StkApp.MENU_ID_DEFAULT_ITEM, 3, R.string.help);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        CatLog.d(LOG_TAG, "onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
        boolean helpVisible = false;
        boolean mainVisible = false;

        if (mState == STATE_SECONDARY) {
            mainVisible = true;
        }
        if (mStkMenu != null) {
            helpVisible = mStkMenu.helpAvailable;
        }
        CatLog.d(LOG_TAG, "onPrepareOptionsMenu, mainVisible: " + mainVisible +
                ", helpVisible: " + helpVisible);
        menu.findItem(StkApp.MENU_ID_END_SESSION).setVisible(mainVisible);
        menu.findItem(StkApp.MENU_ID_HELP).setVisible(helpVisible);
        // for defaut item
        if (mStkMenu != null) {
            Item item = mStkMenu.items.get(mStkMenu.defaultItem);
            if (item != null) {
                CatLog.d(LOG_TAG, "item: " + item);
            }
            if(item == null || item.text == null || item.text.length() == 0 ) {
                CatLog.d(LOG_TAG, "Set visible of default item to false.");
                menu.findItem(StkApp.MENU_ID_DEFAULT_ITEM).setVisible(false);
            } else {
                CatLog.d(LOG_TAG, "Set visible of default item to true.");
                menu.findItem(StkApp.MENU_ID_DEFAULT_ITEM).setTitle(item.text);
                menu.findItem(StkApp.MENU_ID_DEFAULT_ITEM).setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        CatLog.d(LOG_TAG, "onOptionsItemSelected");
        if (!mAcceptUsersInput) {
            return true;
        }
        CatLog.d(LOG_TAG, "onOptionsItemSelected, item " + item.getItemId() +
                ", slot " + mSlotId);
        switch (item.getItemId()) {
            case StkApp.MENU_ID_END_SESSION:
                cancelTimeOut();
                mAcceptUsersInput = false;
                // send session end response.
                sendResponse(StkAppService.RES_ID_END_SESSION);
                cancelTimeOut();
                finish();
                return true;
            case StkApp.MENU_ID_HELP:
                cancelTimeOut();
                mAcceptUsersInput = false;
                int position = getSelectedItemPosition();
                Item stkItem = getSelectedItem(position);
                if (stkItem == null) {
                    break;
                }
                if (mState == STATE_SECONDARY) {
                    appService.getStkContext(mSlotId).setPendingActivityInstance(this);
                }
                // send help needed response.
                sendResponse(StkAppService.RES_ID_MENU_SELECTION, stkItem.id, true);
                return true;
            case StkApp.MENU_ID_DEFAULT_ITEM:
                if (mStkMenu != null) {
                    Item defaultItem = mStkMenu.items.get(mStkMenu.defaultItem);
                    if (defaultItem == null) {
                        return true;
                    }
                    if (mState == STATE_SECONDARY) {
                        appService.getStkContext(mSlotId).setPendingActivityInstance(this);
                    }
                    sendResponse(StkAppService.RES_ID_MENU_SELECTION, defaultItem.id,
                            false);
                    mAcceptUsersInput = false;
                    mProgressView.setVisibility(View.VISIBLE);
                    mProgressView.setIndeterminate(true);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        CatLog.d(LOG_TAG, "onSaveInstanceState: " + mSlotId);
        outState.putInt("STATE", mState);
        outState.putParcelable("MENU", mStkMenu);
        outState.putBoolean("ACCEPT_USERS_INPUT", mAcceptUsersInput);
        outState.putBoolean("RESPONSE_SENT", mIsResponseSent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        CatLog.d(LOG_TAG, "onRestoreInstanceState: " + mSlotId);
        mState = savedInstanceState.getInt("STATE");
        mStkMenu = savedInstanceState.getParcelable("MENU");
        mAcceptUsersInput = savedInstanceState.getBoolean("ACCEPT_USERS_INPUT");
        mIsResponseSent = savedInstanceState.getBoolean("RESPONSE_SENT");
    }

    private void cancelTimeOut() {
        CatLog.d(LOG_TAG, "cancelTimeOut: " + mSlotId);
        mTimeoutHandler.removeMessages(MSG_ID_TIMEOUT);
    }

    private void startTimeOut() {
        if (mState == STATE_SECONDARY) {
            // Reset timeout.
            cancelTimeOut();
            CatLog.d(LOG_TAG, "startTimeOut: " + mSlotId);
            mTimeoutHandler.sendMessageDelayed(mTimeoutHandler
                    .obtainMessage(MSG_ID_TIMEOUT), StkApp.UI_TIMEOUT);
        }
    }

    // Bind list adapter to the items list.
    private void displayMenu(ImageView iconView, TextView textView, ListActivity list) {
        int simCount = TelephonyManager.from(mContext).getSimCount();

        if (mStkMenu != null) {
            // Display title & title icon
            if (mStkMenu.titleIcon != null) {
                iconView.setImageBitmap(mStkMenu.titleIcon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
            if (!mStkMenu.titleIconSelfExplanatory) {
                textView.setVisibility(View.VISIBLE);
                if (mStkMenu.title == null) {
                    int resId = R.string.app_name;
                    if (1 < simCount) {
                        if (mSlotId == PhoneConstants.SIM_ID_1) {
                            resId = R.string.appI_name;
                        } else if (mSlotId == PhoneConstants.SIM_ID_2) {
                            resId = R.string.appII_name;
                        } else if (mSlotId == PhoneConstants.SIM_ID_3) {
                            resId = R.string.appIII_name;
                        } else {
                            resId = R.string.appIV_name;
                        }
                    } else {
                        resId = R.string.app_name;
                    }
                    textView.setText(resId);
                } else {
                    textView.setText(mStkMenu.title);
                }
            } else {
                textView.setVisibility(View.INVISIBLE);
            }
            // create an array adapter for the menu list
            int i = 0;
            for (i = 0; i < mStkMenu.items.size();) {
                if (mStkMenu.items.get(i) == null) {
                    mStkMenu.items.remove(i);
                    CatLog.d(LOG_TAG, "Remove null item from menu.items");
                    continue;
                }
                ++i;
            }
            if (mStkMenu.items.size() == 0) {
                CatLog.d(LOG_TAG, "should not display the SET_UP_MENU because no item");
            } else {
                StkMenuAdapter adapter = new StkMenuAdapter(this,
                        mStkMenu.items, mStkMenu.nextActionIndicator, mStkMenu.itemsIconSelfExplanatory);
                // Bind menu list to the new adapter.
                list.setListAdapter(adapter);
                // Set default item
                list.setSelection(mStkMenu.defaultItem);
            }
        }
    }

    private int initFromIntent(Intent intent) {
        int result = StkAppService.STK_INVALID_SOURCE;

        if (intent != null) {
            mState = intent.getIntExtra("STATE", STATE_MAIN);
            mSlotId = intent.getIntExtra(StkAppService.SLOT_ID, -1);
            mStkSource = intent.getStringExtra(StkAppService.STK_SOURCE_KEY);
            CatLog.d(LOG_TAG, "slot id: " + mSlotId + ", state: " + mState +
                    ", mStkSource: " + mStkSource);
            if (appService != null) {
                if (mStkSource != null) {
                    if (appService.isValidStkSourceKey(mStkSource)) {
                        result = StkAppService.STK_VALID_SOURCE;
                    }
                } else {
                    CatLog.d(LOG_TAG, "mStkSource is null!");
                }
            } else {
                CatLog.d(LOG_TAG, "appService is null!");
            }
        } else {
            result = StkAppService.STK_INVALID_PARAMTER;
        }
        return result;
    }

    private Item getSelectedItem(int position) {
        Item item = null;
        if (mStkMenu != null) {
            try {
                item = mStkMenu.items.get(position);
            } catch (IndexOutOfBoundsException e) {
                if (StkApp.DBG) {
                    CatLog.d(LOG_TAG, "IOOBE Invalid menu: " + position);
                }
            } catch (NullPointerException e) {
                if (StkApp.DBG) {
                    CatLog.d(LOG_TAG, "NPE Invalid menu");
                }
            }
        }
        return item;
    }

    private void sendResponse(int resId) {
        sendResponse(resId, 0, false);
    }

    private void sendResponse(int resId, int itemId, boolean help) {
        CatLog.d(LOG_TAG, "sendResponse resID[" + resId + "] itemId[" + itemId +
            "] help[" + help + "]");

        if (mSlotId == -1) {
            /* In EMMA test case, it may come here */
            CatLog.d(LOG_TAG, "sim id is invalid");
            return;
        }

        if ((STATE_SECONDARY != mState) && (StkAppService.RES_ID_END_SESSION == resId)) {
            CatLog.d(LOG_TAG, "Ignore response of End Session in mState[" + mState + "]");
            return;
        }
        mIsResponseSent = true;
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_RESPONSE);
        args.putInt(StkAppService.SLOT_ID, mSlotId);
        args.putInt(StkAppService.RES_ID, resId);
        args.putInt(StkAppService.MENU_SELECTION, itemId);
        args.putBoolean(StkAppService.HELP, help);
        mContext.startService(new Intent(mContext, StkAppService.class)
                .putExtras(args));
    }

    private int onResumePreConditionCheck(int slodId) {
        int result = FINISH_CAUSE_NORMAL;
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);

        if (isOnFlightMode() == true) {
            CatLog.d(LOG_TAG, "flight mode - don't make stk be visible");
            result = FINISH_CAUSE_FLIGHT_MODE;
        } else if (tm.hasIccCard(slodId) == false) {
            CatLog.d(LOG_TAG, "SIM card was removed");
            result = FINISH_CAUSE_SIM_REMOVED;
        } else if (isOnLockMode(slodId) == true || isRadioOnState(slodId) == false) {
            CatLog.d(LOG_TAG, "radio off - don't make stk be visible");
            result = FINISH_CAUSE_NOT_AVAILABLE;
        } else if (appService == null) {
            CatLog.d(LOG_TAG, "can not launch stk menu 'cause null StkAppService");
            result = FINISH_CAUSE_NULL_SERVICE;
        }
        return result;
    }

    boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            CatLog.d(LOG_TAG, "fail to get airlane mode");
            mode = 0;
        }

        CatLog.d(LOG_TAG, "airlane mode is " + mode);
        return (mode != 0);
    }

    boolean isOnLockMode(int slotId) {
        int simState = TelephonyManager.getDefault().getSimState(slotId);
        CatLog.d(LOG_TAG, "lock mode is " + simState);
        if (TelephonyManager.SIM_STATE_PIN_REQUIRED == simState ||
                TelephonyManager.SIM_STATE_PUK_REQUIRED == simState ||
                TelephonyManager.SIM_STATE_NETWORK_LOCKED == simState) {
            return true;
        } else {
            return false;
        }
    }
    boolean isRadioOnState(int slotId) {
        boolean radioOn = true;
        CatLog.d(LOG_TAG, "isRadioOnState check = " + slotId);

        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (phone != null) {
                int subId[] = SubscriptionManager.getSubId(slotId);
                radioOn = phone.isRadioOnForSubscriber(subId[0],
                    getApplicationContext().getOpPackageName());
            }
            CatLog.d(LOG_TAG, "isRadioOnState - radio_on[" + radioOn + "]");
        } catch (RemoteException e) {
            e.printStackTrace();
            CatLog.d(LOG_TAG, "isRadioOnState - Exception happen ====");
        }
        return radioOn;
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
