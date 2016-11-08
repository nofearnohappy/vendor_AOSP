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

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.cdma.utk.Item;
import com.android.internal.telephony.cdma.utk.Menu;
import com.android.internal.telephony.cdma.utk.UtkLog;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;


//Utk modification for TDD data only
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.telephony.TelephonyManagerEx;

import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

/**
 * ListActivity used for displaying UTK menus. These can be SET UP MENU and
 * SELECT ITEM menus. This activity is started multiple times with different
 * menu content.
 *
 */
public class UtkMenuActivity extends ListActivity {
    private Context mContext;
    private Menu mUtkMenu = null;
    private int mState = STATE_MAIN;
    private int mPhoneId = -1;
    private boolean mAcceptUsersInput = true;

    private TextView mTitleTextView = null;
    private ImageView mTitleIconView = null;
    private ProgressBar mProgressView = null;

    UtkAppService appService = UtkAppService.getInstance();

    private final IntentFilter mSIMStateChangeFilter = new IntentFilter(
            TelephonyIntents.ACTION_SIM_STATE_CHANGED);

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
                if ((simId == mPhoneId) &&
                        ((IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)) ||
                        (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)))) {
                    UtkLog.d(this, "UtkMenuActivity.finish()");
                    UtkMenuActivity.this.cancelTimeOut();
                    UtkMenuActivity.this.finish();
                    ActivityManager activityManager = null;
                    int taskId = -1;
                    activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    if (activityManager == null) {
                        UtkLog.d(this, "mSIMStateChangeReceiver(), get ActivityManager fail");
                        return;
                    }
                    taskId = UtkMenuActivity.this.getTaskId();
                    if (taskId == -1) {
                        UtkLog.d(this, "mSIMStateChangeReceiver(), taskId = -1, fail");
                        return;
                    }
                    try {
                        activityManager.removeTask(taskId);
                        UtkLog.d(this, "mSIMStateChangeReceiver(), remove task(" + taskId
                                + ") done");
                    } catch (SecurityException e) {
                        UtkLog.d(this, "mSIMStateChangeReceiver(), remove task(" + taskId
                                + ") fail");
                        UtkLog.d(this, "mSIMStateChangeReceiver():" + e.toString());
                    }

                }
            }
        }
    };

    //Add for Utk IR case
    private final IntentFilter mIRStateChangeFilter = new IntentFilter();

    private final BroadcastReceiver mIRStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((SvlteRatController.INTENT_ACTION_START_SWITCH_ROAMING_MODE.
                    equals(intent.getAction())) ||
                (SvlteRatController.INTENT_ACTION_FINISH_SWITCH_ROAMING_MODE.
                        equals(intent.getAction()))) {
                //Query RAT Mode
                //SvlteRatController sLteRatController = SvlteRatController.getInstance();
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                UtkLog.d(this, "received IR state changed broadcast at phone " + phoneId);
                if (phoneId == mPhoneId) {
                    LteDcPhoneProxy lteDcPhoneProxy =
                            (LteDcPhoneProxy) PhoneFactory.getPhone(mPhoneId);
                    SvlteRatController sLteRatController = null;
                    if (lteDcPhoneProxy != null) {
                        sLteRatController = lteDcPhoneProxy.getSvlteRatController();
                    }
                    RoamingMode sRoamingMode = null;
                    if (sLteRatController != null) {
                        sRoamingMode = sLteRatController.getRoamingMode();
                        UtkLog.d(this, "mIRStateChangeReceiver sRoamingMode = " + sRoamingMode);
                        if (RoamingMode.ROAMING_MODE_NORMAL_ROAMING == sRoamingMode) {
                            UtkMenuActivity.this.cancelTimeOut();
                            UtkMenuActivity.this.finish();
                        }
                    }
                }
            }
        }
    };
    private final IntentFilter mRadioOffFilter = new IntentFilter(
            "android.intent.action.utk.radio_off");

    private final BroadcastReceiver mRadioOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.utk.radio_off".equals(intent.getAction())) {
                UtkLog.d(this, "received radio off broadcast");
                ActivityManager activityManager = null;
                int taskId = -1;
                UtkMenuActivity.this.cancelTimeOut();
                activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager == null) {
                    UtkLog.d(this, "received radio off broadcast, get ActivityManager fail");
                    return;
                }
                taskId = UtkMenuActivity.this.getTaskId();
                if (taskId == -1) {
                    UtkLog.d(this, "received radio off broadcast, taskId = -1, fail");
                    return;
                }
                try {
                    activityManager.removeTask(taskId);
                    UtkLog.d(this, "received radio off, remove task(" + taskId + ") done");
                } catch (SecurityException e) {
                    UtkLog.d(this, "received radio off, remove task(" + taskId + ") fail");
                    UtkLog.d(this, "received radio off:" + e.toString());
                }
            }
        }
    };

    // Internal state values
    static final int STATE_MAIN = 1;
    static final int STATE_SECONDARY = 2;

    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_ID_TIMEOUT:
                mAcceptUsersInput = false;
                sendResponse(UtkAppService.RES_ID_TIMEOUT);
                break;
            }
        }
    };

    //Utk modification for TDD data only
    private ContentObserver m4GDataModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            UtkLog.d(this, "4G data only mode changed!");
            int subId = SubscriptionManager.getSubIdUsingPhoneId(mPhoneId);
            UtkLog.d(this, "subId = " + subId);
            int patternLteDataOnly = Settings.Global.getInt(mContext.getContentResolver(),
                                    TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId),
                                    TelephonyManagerEx.SVLTE_RAT_MODE_4G);
            UtkLog.d(this, "patternLteDataOnly = " + patternLteDataOnly);
            if (TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY == patternLteDataOnly) {
                UtkLog.d(this, "UtkMenuActivity.finish()");
                UtkMenuActivity.this.cancelTimeOut();
                UtkMenuActivity.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        UtkLog.d(this, "onCreate");
        // Remove the default title, customized one is used.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set the layout for this activity.
        setContentView(R.layout.utk_menu_list);

        mTitleTextView = (TextView) findViewById(R.id.title_text);
        mTitleIconView = (ImageView) findViewById(R.id.title_icon);
        mProgressView = (ProgressBar) findViewById(R.id.progress_bar);
        mContext = getBaseContext();

        initFromIntent(getIntent());
        mAcceptUsersInput = true;

        registerReceiver(mSIMStateChangeReceiver, mSIMStateChangeFilter);

        //Utk modification for TDD data only
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            //UtkLog.d(this, "register 4G data only observer");
            register4GDataModeObserver();

            //Utk modification for IR
            mIRStateChangeFilter.addAction(
                                SvlteRatController.INTENT_ACTION_START_SWITCH_ROAMING_MODE);
            mIRStateChangeFilter.addAction(
                                SvlteRatController.INTENT_ACTION_FINISH_SWITCH_ROAMING_MODE);
            registerReceiver(mIRStateChangeReceiver, mIRStateChangeFilter);
        }

        registerReceiver(mRadioOffReceiver, mRadioOffFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        UtkLog.d(this, "onNewIntent");
        initFromIntent(intent);
        mAcceptUsersInput = true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onListItemClick");
        UtkLog.d(this, "mState :" + mState + " ,mAcceptUsersInput:" + mAcceptUsersInput);
        if (!mAcceptUsersInput) {
            return;
        }

        Item item = getSelectedItem(position);
        UtkLog.d(this, "item :" + item + " ,position:" + position);
        if (item == null) {
            return;
        }
        sendResponse(UtkAppService.RES_ID_MENU_SELECTION, item.id, false);
        mAcceptUsersInput = false;
        mProgressView.setVisibility(View.VISIBLE);
        mProgressView.setIndeterminate(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onKeyDown");
        UtkLog.d(this, "mState :" + mState + " ,mAcceptUsersInput:" + mAcceptUsersInput);
        if (!mAcceptUsersInput) {
            return true;
        }
        UtkLog.d(this, "keyCode:" + keyCode + " ,event:" + event);
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            switch (mState) {
            case STATE_SECONDARY:
                cancelTimeOut();
                mAcceptUsersInput = false;
                sendResponse(UtkAppService.RES_ID_BACKWARD);
                return true;
            case STATE_MAIN:
                break;
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();

        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onResume");
        if (isOnFlightMode() == true) {
            UtkLog.d(this, "Utk can't be launched in flight mode");
            showTextToast(getString(R.string.lable_on_flight_mode));

            finish();
            return;
        }
        UtkLog.d(this, "mState :"+mState + "mAcceptUsersInput:" + mAcceptUsersInput);
        appService.indicateMenuVisibility(true);
        mUtkMenu = appService.getMenu();
        if (mUtkMenu == null) {
            UtkLog.d(this, "Utk can't be launched because Menu is null");
            finish();
            return;
        }
        displayMenu();
        startTimeOut();
        // whenever this activity is resumed after a sub activity was invoked
        // (Browser, In call screen) switch back to main state and enable
        // user's input;
        if (!mAcceptUsersInput) {
            mState = STATE_MAIN;
            mAcceptUsersInput = true;
        }
        // make sure the progress bar is not shown.
        mProgressView.setIndeterminate(false);
        mProgressView.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onPause");
        appService.indicateMenuVisibility(false);
        cancelTimeOut();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSIMStateChangeReceiver);
        unregisterReceiver(mRadioOffReceiver);
        //Utk modification for TDD data only
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            UtkLog.d(this, "UtkMenuActivity onDestory: mAcceptUsersInput = " +
                                    mAcceptUsersInput + " mState = " + mState);
            unregister4GDataModeObserver();

            //Utk modification for IR
            unregisterReceiver(mIRStateChangeReceiver);

            if (mAcceptUsersInput && (mState != STATE_MAIN)) {
            UtkLog.d(this, "handleDestroy - Send End Session");
            sendResponse(UtkAppService.RES_ID_END_SESSION);
            }
        }
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, UtkApp.MENU_ID_END_SESSION, 1, R.string.menu_end_session);
        menu.add(0, UtkApp.MENU_ID_HELP, 2, R.string.help);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean helpVisible = false;
        boolean mainVisible = false;

        if (mState == STATE_SECONDARY) {
            mainVisible = true;
        }
        if (mUtkMenu != null) {
            helpVisible = mUtkMenu.helpAvailable;
        }

        menu.findItem(UtkApp.MENU_ID_END_SESSION).setVisible(mainVisible);
        menu.findItem(UtkApp.MENU_ID_HELP).setVisible(helpVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mAcceptUsersInput) {
            return true;
        }
        switch (item.getItemId()) {
        case UtkApp.MENU_ID_END_SESSION:
            cancelTimeOut();
            mAcceptUsersInput = false;
            // send session end response.
            sendResponse(UtkAppService.RES_ID_END_SESSION);
            return true;
        case UtkApp.MENU_ID_HELP:
            cancelTimeOut();
            mAcceptUsersInput = false;
            int position = getSelectedItemPosition();
            Item utkItem = getSelectedItem(position);
            if (utkItem == null) {
                break;
            }
            // send help needed response.
            sendResponse(UtkAppService.RES_ID_MENU_SELECTION, utkItem.id, true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onSaveInstanceState");
        UtkLog.d(this, "mState :"+mState + "mAcceptUsersInput:" + mAcceptUsersInput);        
        outState.putInt("STATE", mState);
        outState.putParcelable("MENU", mUtkMenu);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onRestoreInstanceState");
        UtkLog.d(this, "mState :"+mState + "mAcceptUsersInput:" + mAcceptUsersInput);                
        mState = savedInstanceState.getInt("STATE");
        mUtkMenu = savedInstanceState.getParcelable("MENU");
    }

    private void cancelTimeOut() {
        mTimeoutHandler.removeMessages(MSG_ID_TIMEOUT);
    }

    private void startTimeOut() {
        if (mState == STATE_SECONDARY) {
            // Reset timeout.
            cancelTimeOut();
            mTimeoutHandler.sendMessageDelayed(mTimeoutHandler
                    .obtainMessage(MSG_ID_TIMEOUT), UtkApp.UI_TIMEOUT);
            UtkLog.d(this, "mPhoneId:" + mPhoneId + "  startTimeOut ");
        }
    }

    // Bind list adapter to the items list.
    private void displayMenu() {

        if (mUtkMenu != null) {
            // Display title & title icon
            if (mUtkMenu.titleIcon != null) {
                mTitleIconView.setImageBitmap(mUtkMenu.titleIcon);
                mTitleIconView.setVisibility(View.VISIBLE);
            } else {
                mTitleIconView.setVisibility(View.GONE);
            }
            if (!mUtkMenu.titleIconSelfExplanatory) {
                mTitleTextView.setVisibility(View.VISIBLE);
                if (mUtkMenu.title == null) {
                    mTitleTextView.setText(R.string.app_name);
                } else {
                    mTitleTextView.setText(mUtkMenu.title);
                }
            } else {
                mTitleTextView.setVisibility(View.INVISIBLE);
            }
            // create an array adapter for the menu list
            UtkMenuAdapter adapter = new UtkMenuAdapter(this,
                    mUtkMenu.items, mUtkMenu.itemsIconSelfExplanatory);
            // Bind menu list to the new adapter.
            setListAdapter(adapter);
            // Set default item
            setSelection(mUtkMenu.defaultItem);
        }
    }

    private void initFromIntent(Intent intent) {
        UtkLog.d (this, "initFromIntent");
        if (intent != null) {
            mState = intent.getIntExtra("STATE", STATE_MAIN);
            UtkLog.d(this, "mState :" + mState + "mAcceptUsersInput:" + mAcceptUsersInput);
            mPhoneId = intent.getIntExtra(UtkAppService.PHONE_ID, -1);
            UtkLog.d(this, "mPhoneId :" + mPhoneId);
        } else {
            UtkLog.d (this, "initFromIntent null");        
            finish();
        }
    }

    private Item getSelectedItem(int position) {
        Item item = null;
        if (mUtkMenu != null) {
            try {
                item = mUtkMenu.items.get(position);
            } catch (IndexOutOfBoundsException e) {
                if (UtkApp.DBG) {
                    UtkLog.d(this, "Invalid menu");
                }
            } catch (NullPointerException e) {
                if (UtkApp.DBG) {
                    UtkLog.d(this, "Invalid menu");
                }
            }
        }
        return item;
    }

    private void sendResponse(int resId) {
        sendResponse(resId, 0, false);
    }

    private void sendResponse(int resId, int itemId, boolean help) {
        Bundle args = new Bundle();
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  sendResponse resId:" + resId);
        if (mPhoneId == -1) {
            UtkLog.d(this, "phone id is invalid");
            return;
        }
        args.putInt(UtkAppService.OPCODE, UtkAppService.OP_RESPONSE);
        args.putInt(UtkAppService.PHONE_ID, mPhoneId);
        args.putInt(UtkAppService.RES_ID, resId);
        args.putInt(UtkAppService.MENU_SELECTION, itemId);
        args.putBoolean(UtkAppService.HELP, help);
        mContext.startService(new Intent(mContext, UtkAppService.class)
                .putExtras(args));
    }

    boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
        } catch(SettingNotFoundException e) {
            UtkLog.d(this, "fail to get airlane mode");
            mode = 0;
        }
        
        UtkLog.d(this, "airlane mode is " + mode);
        return (mode != 0);
    }

    void showTextToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    //Utk modification for TDD data only
    private void register4GDataModeObserver() {
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  register 4G data only observer");
        int subId = SubscriptionManager.getSubIdUsingPhoneId(mPhoneId);
        UtkLog.d(this, "subId = " + subId);
        Uri uri = Settings.Global.getUriFor(
            TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId));
        mContext.getContentResolver().registerContentObserver(
            uri, true, m4GDataModeObserver);
    }

    private void unregister4GDataModeObserver() {
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  unregister 4G data only observer");
        mContext.getContentResolver().unregisterContentObserver(
                m4GDataModeObserver);
    }
}
