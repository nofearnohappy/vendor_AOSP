/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.stk;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.FontSize;
import com.android.internal.telephony.cat.Input;
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
 * Display a request for a text input a long with a text edit form.
 */
public class StkInputActivity extends Activity implements View.OnClickListener,
        TextWatcher {

    // Members
    private EditText mTextIn = null;
    private TextView mPromptView = null;
    private View mYesNoLayout = null;
    private View mNormalLayout = null;
    private String mOptr = SystemProperties.get("ro.operator.optr", "NONE");

    // Constants
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);

    private int mState;
    private Context mContext;
    private Input mStkInput = null;
    private boolean mAcceptUsersInput = true;
    private String mStkSource = null;
    private boolean mIsRegisterReceiverDone = false;

    // Constants
    private static final int STATE_TEXT = 1;
    private static final int STATE_YES_NO = 2;

    static final String YES_STR_RESPONSE = "YES";
    static final String NO_STR_RESPONSE = "NO";

    // Font size factor values.
    static final float NORMAL_FONT_FACTOR = 1;
    static final float LARGE_FONT_FACTOR = 2;
    static final float SMALL_FONT_FACTOR = (1 / 2);

    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;

    private static final int DELAY_TIME = 300;
    private StkAppService appService = StkAppService.getInstance();

    private boolean mIsResponseSent = false;
    private int mSlotId = -1;
    Activity mInstance = null;

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_ID_TIMEOUT:
                CatLog.d(LOG_TAG, "Msg timeout.");
                mAcceptUsersInput = false;
                appService.getStkContext(mSlotId).setPendingActivityInstance(mInstance);
                sendResponse(StkAppService.RES_ID_TIMEOUT);
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

                CatLog.d(LOG_TAG, "mSIMStateChangeReceiver() - slotId[" + slotId + "]  state[" +
                        simState + "]");
                if ((slotId == mSlotId) &&
                    ((IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) ||
                    (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)))) {
                    if (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)) {
                        showTextToast(getApplicationContext(),
                        getString(R.string.lable_sim_not_ready));
                    }
                    cancelTimeOut();
                    CatLog.d(LOG_TAG, "mSIMStateChangeReceiver, mState: " + mState);
                    mIsResponseSent = true;
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

    private boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    // Click listener to handle buttons press..
    public void onClick(View v) {
        String input = null;
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return;
        }

        switch (v.getId()) {
            case R.id.button_ok:
                // Check that text entered is valid .
                if (!verfiyTypedText(mTextIn)) {
                    CatLog.d(LOG_TAG, "handleClick, invalid text");
                    return;
                }
                mAcceptUsersInput = false;
                input = mTextIn.getText().toString();
                break;
            // Yes/No layout buttons.
            case R.id.button_yes:
                mAcceptUsersInput = false;
                input = YES_STR_RESPONSE;
                break;
            case R.id.button_no:
                mAcceptUsersInput = false;
                input = NO_STR_RESPONSE;
                break;
        }
        CatLog.d(LOG_TAG, "handleClick, ready to response");
        appService.getStkContext(mSlotId).setPendingActivityInstance(this);
        sendResponse(StkAppService.RES_ID_INPUT, input, false);
        cancelTimeOut();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        CatLog.d(LOG_TAG, "onCreate - mIsResponseSent[" + mIsResponseSent + "]");

        // Set the layout for this activity.
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.stk_input);

        // Initialize members
        mTextIn = (EditText) this.findViewById(R.id.in_text);
        mPromptView = (TextView) this.findViewById(R.id.prompt);
        mInstance = this;
        // Set buttons listeners.
        Button okButton = (Button) findViewById(R.id.button_ok);
        Button yesButton = (Button) findViewById(R.id.button_yes);
        Button noButton = (Button) findViewById(R.id.button_no);

        okButton.setOnClickListener(this);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        mYesNoLayout = findViewById(R.id.yes_no_layout);
        mNormalLayout = findViewById(R.id.normal_layout);

        initFromIntent(getIntent());

        int simId = mSlotId + 1;
        setTitle(getString(R.string.app_name) + " " + simId);

        mContext = getBaseContext();
        mAcceptUsersInput = true;

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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        CatLog.d(LOG_TAG, "onNewIntent");
        initFromIntent(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mTextIn.addTextChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        CatLog.d(LOG_TAG, "onResume - mIsResponseSent[" + mIsResponseSent +
                "], slot id: " + mSlotId);
        if (appService != null) {
            appService.indicateInputVisibility(true, mSlotId);
        }
        startTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
        CatLog.d(LOG_TAG, "onPause - mIsResponseSent[" + mIsResponseSent + "]");
        if (appService != null) {
            appService.indicateInputVisibility(false, mSlotId);
        }
        /* Remove cancelTimeOut() to fix no terminal response when activity was moved to background. */
    }

    @Override
    public void onStop() {
        super.onStop();
        CatLog.d(LOG_TAG, "onStop - mIsResponseSent[" + mIsResponseSent + "]");
        if (mIsResponseSent) {
            cancelTimeOut();
            finish();
        } else {
            if (null == appService) {
                CatLog.d(LOG_TAG, "null appService");
                return;
            }
            if (null == appService.getStkContext(mSlotId)) {
                CatLog.d(LOG_TAG, "null stk context");
                return;
            }
            appService.getStkContext(mSlotId).setPendingActivityInstance(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CatLog.d(LOG_TAG, "onDestroy - before Send End Session mIsResponseSent[" +
                mIsResponseSent + " , " + mSlotId + "]");
        //isInputFinishBySrv: if input act is finish by stkappservice when OP_LAUNCH_APP again,
        //we can not send TR here, since the input cmd is waiting user to process.
        if (!mIsResponseSent) {
            if (null == appService) {
             //To get instance again, if stkappservice has created before onDestroy.
             appService = StkAppService.getInstance();
            }
            if (null != appService) {
                if (!appService.isInputPending(mSlotId)) {
                    CatLog.d(LOG_TAG, "handleDestroy - Send End Session");
                    sendResponse(StkAppService.RES_ID_END_SESSION);
                }
            }
        }
        cancelTimeOut();
        if (mIsRegisterReceiverDone) {
            unregisterReceiver(mSIMStateChangeReceiver);
            unregisterReceiver(mAirplaneModeReceiver);
            if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
                unregister4GDataModeObserver();
                //Stk modification for IR
                unregisterReceiver(mIRStateChangeReceiver);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                CatLog.d(LOG_TAG, "onKeyDown - KEYCODE_BACK");
                mAcceptUsersInput = false;
                cancelTimeOut();
                appService.getStkContext(mSlotId).setPendingActivityInstance(this);
                sendResponse(StkAppService.RES_ID_BACKWARD, null, false);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(android.view.Menu.NONE, StkApp.MENU_ID_END_SESSION, 1,
                R.string.menu_end_session);
        menu.add(0, StkApp.MENU_ID_HELP, 2, R.string.help);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(StkApp.MENU_ID_END_SESSION).setVisible(true);
        menu.findItem(StkApp.MENU_ID_HELP).setVisible(mStkInput.helpAvailable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return true;
        }
        switch (item.getItemId()) {
        case StkApp.MENU_ID_END_SESSION:
            mAcceptUsersInput = false;
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_END_SESSION);
            finish();
            return true;
        case StkApp.MENU_ID_HELP:
            mAcceptUsersInput = false;
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_INPUT, "", true);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        CatLog.d(LOG_TAG, "onSaveInstanceState: " + mSlotId);
        outState.putBoolean("ACCEPT_USERS_INPUT", mAcceptUsersInput);
        outState.putBoolean("RESPONSE_SENT", mIsResponseSent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        CatLog.d(LOG_TAG, "onRestoreInstanceState: " + mSlotId);
        mAcceptUsersInput = savedInstanceState.getBoolean("ACCEPT_USERS_INPUT");
        mIsResponseSent = savedInstanceState.getBoolean("RESPONSE_SENT");
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        handleBeforeTextChanged(s, start, count, after);
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Reset timeout.
        handleTextChanged(s, start, before, count);
    }

    public void afterTextChanged(Editable s) {
        handleAfterTextChanged(s, mTextIn);
    }

    private void configInputDisplay() {
        TextView numOfCharsView = (TextView) findViewById(R.id.num_of_chars);
        TextView inTypeView = (TextView) findViewById(R.id.input_type);

        if (mStkInput.icon != null) {
            setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(
                    mStkInput.icon));
        }

        handleConfigInputDisplay(
                mPromptView, mTextIn, numOfCharsView, inTypeView, mYesNoLayout, mNormalLayout);
    }
    private void initFromIntent(Intent intent) {
        // Get the calling intent type: text/key, and setup the
        // display parameters.
        CatLog.d(LOG_TAG, "initFromIntent - slot id: " + mSlotId);
        if (intent != null) {
            mStkInput = intent.getParcelableExtra("INPUT");
            mSlotId = intent.getIntExtra(StkAppService.SLOT_ID, -1);
            mStkSource = intent.getStringExtra(StkAppService.STK_SOURCE_KEY);
            CatLog.d(LOG_TAG, "onCreate - slot id: " + mSlotId);
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
            if (mStkInput == null) {
                finish();
            } else {
                mState = mStkInput.yesNo ? STATE_YES_NO :
                        STATE_TEXT;
                configInputDisplay();
            }
        } else {
            finish();
        }
    }
    void sendResponse(int resId) {
        sendResponse(resId, null, false);
    }

    void sendResponse(int resId, String input, boolean help) {
        if (mSlotId == -1) {
            CatLog.d(LOG_TAG, "slot id is invalid");
            return;
        }

        if (StkAppService.getInstance() == null) {
            CatLog.d(LOG_TAG, "StkAppService is null, Ignore response: id is " + resId);
            return;
        }

        CatLog.d(LOG_TAG, "sendResponse resID[" + resId + "] input[" + input +
                "] help[" + help + "]");
        mIsResponseSent = true;
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_RESPONSE);
        args.putInt(StkAppService.SLOT_ID, mSlotId);
        args.putInt(StkAppService.RES_ID, resId);
        if (input != null) {
            args.putString(StkAppService.INPUT, input);
        }
        args.putBoolean(StkAppService.HELP, help);
        mContext.startService(new Intent(mContext, StkAppService.class)
                .putExtras(args));
    }

    void handleBeforeTextChanged(CharSequence s, int start, int count,
            int after) {
    }

    void handleTextChanged(CharSequence s, int start, int before, int count) {
        // Reset timeout.
        startTimeOut();
    }

    void handleAfterTextChanged(Editable s, EditText edit) {

        int iStart = edit.getSelectionStart();
        int iEnd = edit.getSelectionEnd();
        if(mStkInput.ucs2 == true){
            if(mStkInput.maxLen > 239 / 2)
                mStkInput.maxLen = 239 / 2;
        }
        if (s.length() > mStkInput.maxLen){
            s.delete(mStkInput.maxLen, s.length());
            edit.setText(s);
            int temp = 0;
            if (iStart > 0){
                temp = iStart > (mStkInput.maxLen)? mStkInput.maxLen:(iStart -1);
            }
            edit.setSelection(temp);
        }
    }

    private boolean verfiyTypedText(EditText edit) {
        // If not enough input was typed in stay on the edit screen.
        if (edit.getText().length() < mStkInput.minLen) {
            return false;
        }

        return true;
    }

    void cancelTimeOut() {
        mTimeoutHandler.removeMessages(MSG_ID_TIMEOUT);
    }

    void startTimeOut() {
        int duration = StkApp.calculateDurationInMilis(mStkInput.duration);

        if (duration <= 0) {
            duration = StkApp.UI_TIMEOUT;
        }
        cancelTimeOut();
        mTimeoutHandler.sendMessageDelayed(mTimeoutHandler
                .obtainMessage(MSG_ID_TIMEOUT), duration);
    }

    void handleConfigInputDisplay(TextView prompt, EditText edit, TextView numOfCharsView
            , TextView inTypeView, View YN, View Normal) {
        int inTypeId = R.string.alphabet;

        prompt.setText(mStkInput.text);

        // Handle specific global and text attributes.
        switch (mState) {
        case STATE_TEXT:
            int maxLen = mStkInput.maxLen;
            int minLen = mStkInput.minLen;

            if (mStkInput.ucs2 == true) {
                if(mStkInput.maxLen > 239 / 2)
                    maxLen = mStkInput.maxLen = 239 / 2;
            }

            // Set number of chars info.
            String lengthLimit = String.valueOf(minLen);
            if (maxLen != minLen) {
                lengthLimit = minLen + " - " + maxLen;
            }
            numOfCharsView.setText(lengthLimit);

            if (!mStkInput.echo) {
                edit.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Set default text if present.
            if (mStkInput.defaultText != null) {
                edit.setText(mStkInput.defaultText);
            } else {
                // make sure the text is cleared
                edit.setText("", BufferType.EDITABLE);
            }

            break;
        case STATE_YES_NO:
            // Set display mode - normal / yes-no layout
            YN.setVisibility(View.VISIBLE);
            Normal.setVisibility(View.GONE);
            break;
        }

        // Set input type (alphabet/digit) info close to the InText form.
        if (mStkInput.digitOnly) {
            edit.setKeyListener(StkDigitsKeyListener.getInstance());
            inTypeId = R.string.digits;
        }
        inTypeView.setText(inTypeId);
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
