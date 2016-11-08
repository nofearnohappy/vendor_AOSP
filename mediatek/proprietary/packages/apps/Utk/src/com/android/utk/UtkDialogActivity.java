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

import com.android.internal.telephony.cdma.utk.TextMessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.telephony.cdma.utk.UtkLog;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

//Utk modification for TDD data only
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;
/**
 * AlretDialog used for DISPLAY TEXT commands.
 *
 */
public class UtkDialogActivity extends Activity implements View.OnClickListener {
    // members
    TextMessage mTextMsg;

    private static final int MIN_LENGTH = 6;
    private static final int MIN_WIDTH = 170;

    UtkAppService appService = UtkAppService.getInstance();

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_ID_TIMEOUT:
                if (!mTextMsg.userClear) {
                    UtkLog.d(this, "handleMessage user clear false");
                    sendResponse(UtkAppService.RES_ID_CONFIRM);
                }
                finish();
                break;
            }
        }
    };

    //keys) for saving the state of the dialog in the icicle
    private static final String TEXT = "text";

    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;

    // buttons id
    public static final int OK_BUTTON = R.id.button_ok;
    public static final int CANCEL_BUTTON = R.id.button_cancel;

    private int mPatternLteDataOnly = -1;
    private boolean mIrSwitchflag = false;
    private int mPhoneId = -1;

    //Utk modification for TDD data only
    private ContentObserver m4GDataModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            UtkLog.d(this, "4G data only mode changed!");
            int subId = SubscriptionManager.getSubIdUsingPhoneId(mPhoneId);
            UtkLog.d(this, "subId = " + subId);
            mPatternLteDataOnly = Settings.Global.getInt(getContentResolver(),
                                    TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId),
                                    TelephonyManagerEx.SVLTE_RAT_MODE_4G);
            UtkLog.d(this, "mPatternLteDataOnly = " + mPatternLteDataOnly);
            if (TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY == mPatternLteDataOnly) {
                UtkLog.d(this, "UtkDialogActivity.finish()");
                UtkDialogActivity.this.cancelTimeOut();
                UtkDialogActivity.this.finish();
            }
        }
    };

    //Add for Utk IR case
    private final IntentFilter mIRStateChangeFilter = new IntentFilter();

    private final BroadcastReceiver mIRStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((SvlteRatController.INTENT_ACTION_START_SWITCH_ROAMING_MODE.equals(
                                                                            intent.getAction())) ||
                (SvlteRatController.INTENT_ACTION_FINISH_SWITCH_ROAMING_MODE.equals(
                                                                            intent.getAction()))) {
                //Query RAT Mode
                //SvlteRatController mLteRatController = SvlteRatController.getInstance();
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                UtkLog.d(this, "received IR state changed broadcast at phone " + phoneId);
                if (phoneId == mPhoneId) {
                    LteDcPhoneProxy lteDcPhoneProxy =
                            (LteDcPhoneProxy) PhoneFactory.getPhone(mPhoneId);
                    SvlteRatController mLteRatController = null;
                    if (lteDcPhoneProxy != null) {
                        mLteRatController = lteDcPhoneProxy.getSvlteRatController();
                    }
                    RoamingMode mRoamingMode = null;
                    if (mLteRatController != null) {
                        mRoamingMode = mLteRatController.getRoamingMode();
                        UtkLog.d(this, "mIRStateChangeReceiver mRoamingMode = " + mRoamingMode);
                        if (RoamingMode.ROAMING_MODE_NORMAL_ROAMING == mRoamingMode) {
                            mIrSwitchflag = true;
                            UtkDialogActivity.this.cancelTimeOut();
                            UtkDialogActivity.this.finish();
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        initFromIntent(getIntent());
        if (mTextMsg == null) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        Window window = getWindow();

        setContentView(R.layout.utk_msg_dialog);
        window.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.utk_msg_dialog_title);
        TextView titleTv = (TextView)findViewById(R.id.title);
        TextView mMessageView = (TextView) window
                .findViewById(R.id.dialog_message);

        Button okButton = (Button) findViewById(R.id.button_ok);
        Button cancelButton = (Button) findViewById(R.id.button_cancel);

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        titleTv.setText(mTextMsg.title);
        if (!(mTextMsg.iconSelfExplanatory && mTextMsg.icon != null)) {
            if ((mTextMsg.text==null) || (mTextMsg.text.length() < MIN_LENGTH) ) {
            	mMessageView.setMinWidth(MIN_WIDTH);
            }
            mMessageView.setText(mTextMsg.text);
        }

        ImageView icon = (ImageView)findViewById(R.id.icon);
        if (mTextMsg.icon == null) {
            icon.setImageResource(com.android.internal.R.drawable.stat_notify_sim_toolkit);
        } else {
            icon.setImageDrawable(new BitmapDrawable(mTextMsg.icon));
        }

        //Utk modification for TDD data only
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            //UtkLog.d(this, "register 4G data only observer");
            mPatternLteDataOnly = -1;
            register4GDataModeObserver();

            //Utk modification for IR
            mIrSwitchflag = false;
            mIRStateChangeFilter.addAction(
                                SvlteRatController.INTENT_ACTION_START_SWITCH_ROAMING_MODE);
            mIRStateChangeFilter.addAction(
                                SvlteRatController.INTENT_ACTION_FINISH_SWITCH_ROAMING_MODE);
            registerReceiver(mIRStateChangeReceiver, mIRStateChangeFilter);
        }
    }

    public void onClick(View v) {
        String input = null;

        switch (v.getId()) {
        case OK_BUTTON:
            UtkLog.d(this, "OK Clicked! isCurCmdSetupCall[" + appService.isCurCmdSetupCall() + "]");
            if (appService.isCurCmdSetupCall()) {
                UtkLog.d(this, "stk call sendBroadcast(STKCALL_REGISTER_SPEECH_INFO)");
                Intent intent = new Intent("com.android.stk.STKCALL_REGISTER_SPEECH_INFO");
                sendBroadcast(intent);
            }

            sendResponse(UtkAppService.RES_ID_CONFIRM, true);
            finish();
            break;
        case CANCEL_BUTTON:
            UtkLog.d(this, "mPhoneId:" + mPhoneId + "  Cancel button");
            sendResponse(UtkAppService.RES_ID_CONFIRM, false);
            finish();
            break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            sendResponse(UtkAppService.RES_ID_BACKWARD);
            finish();
            break;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimeOut();
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimeOut();
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimeOut();

        //Utk modification for TDD data only
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            UtkLog.d(this, "mPhoneId:" + mPhoneId + "  onDestroy");
            unregister4GDataModeObserver();
            //Utk modification for IR
            unregisterReceiver(mIRStateChangeReceiver);

            if ((TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY == mPatternLteDataOnly) ||
                (mIrSwitchflag)) {
                sendResponse(UtkAppService.RES_ID_END_SESSION);
            }
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(TEXT, mTextMsg);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mTextMsg = savedInstanceState.getParcelable(TEXT);
    }

    private void sendResponse(int resId, boolean confirmed) {
        Bundle args = new Bundle();
        args.putInt(UtkAppService.OPCODE, UtkAppService.OP_RESPONSE);
        args.putInt(UtkAppService.PHONE_ID, mPhoneId);
        args.putInt(UtkAppService.RES_ID, resId);
        args.putBoolean(UtkAppService.CONFIRMATION, confirmed);
        startService(new Intent(this, UtkAppService.class).putExtras(args));
    }

    private void sendResponse(int resId) {
        sendResponse(resId, true);
    }

    private void initFromIntent(Intent intent) {

        if (intent != null) {
            mTextMsg = intent.getParcelableExtra("TEXT");
            mPhoneId = intent.getIntExtra(UtkAppService.PHONE_ID, -1);
            UtkLog.d(this, "mPhoneId :" + mPhoneId);
        } else {
            finish();
        }
    }

    private void cancelTimeOut() {
        mTimeoutHandler.removeMessages(MSG_ID_TIMEOUT);
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  cancelTimeOut");
    }

    private void startTimeOut() {
        if(!mTextMsg.userClear)
        {
            // Reset timeout.
            cancelTimeOut();
            int dialogDuration = UtkApp.calculateDurationInMilis(mTextMsg.duration);
            UtkLog.d(this, "==========>   dialogDuration = " + dialogDuration);
            if (dialogDuration == 0) {
                dialogDuration = UtkApp.DEFAULT_DURATION_TIMEOUT;
            }
            mTimeoutHandler.sendMessageDelayed(mTimeoutHandler
                    .obtainMessage(MSG_ID_TIMEOUT), dialogDuration);
        }
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  startTimeOut");
    }

    //Utk modification for TDD data only
    private void register4GDataModeObserver() {
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  register 4G data only observer");
        int subId = SubscriptionManager.getSubIdUsingPhoneId(mPhoneId);
        UtkLog.d(this, "subId = " + subId);
        Uri uri = Settings.Global.getUriFor(
            TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId));
        getContentResolver().registerContentObserver(
            uri, true, m4GDataModeObserver);
    }

    private void unregister4GDataModeObserver() {
        UtkLog.d(this, "mPhoneId:" + mPhoneId + "  unregister 4G data only observer");
        getContentResolver().unregisterContentObserver(
                m4GDataModeObserver);
    }
}
