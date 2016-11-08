package com.mediatek.settings.plugin;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.op09.plugin.R;
import com.mediatek.telephony.TelephonyManagerEx;


/**
 * CT feature for add swith for LET data enable or not.
 */
public class UseLteDataSettings extends Activity {

    private static final String TAG = "UseLteDataSettings";

    private static final String INTENT_ACTION_FINISH_SWITCH_SVLTE_RAT_MODE =
            "com.mediatek.intent.action.FINISH_SWITCH_SVLTE_RAT_MODE";
    private static final String INTENT_ACTION_CARD_TYPE =
        "android.intent.action.CDMA_CARD_TYPE";
    static final String INTENT_ACTION_DISMISS_DIALOG =
        "com.mediatek.settings.plugin.DISMISS_DIALOG";
    private static final String MODE_STATUS = "mode_status";
    private static final String LTETDD_CDMA = "ltetdd_cdma";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    private static final int PROGRESS_DIALOG = 1000;

    private IntentFilter mIntentFilter;
    private Switch mSwitchBar;
    private boolean mSwitching;
    private boolean mSwitchBarChecked;
    private boolean mIsActiveWindow;
    private boolean mIsDialogShowing;
    private boolean mIsLteCardType;
    private AlertDialog mDialog = null;
    static boolean sSet4GMode = false;
    private boolean mBound = false;
    private ChangeRatModeService mService = null;
    private TelephonyManagerEx mTelephonyManagerEx = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "on receive broadcast action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                Log.i(TAG, "action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED) udpateSwitch");
                updateSwitch();
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
                if (slotId == PhoneConstants.SIM_ID_1) {
                    Log.i(TAG, "TelephonyIntents.ACTION_SIM_STATE_CHANGED udpateSwitch");
                    updateSwitch();
                }
            } else if (INTENT_ACTION_FINISH_SWITCH_SVLTE_RAT_MODE.equals(action)) {
                mSwitching = false;
                updateSwitch();
                hideProgressDlg();
            } else if (TelephonyIntents.ACTION_CDMA_CARD_TYPE.equals(intent.getAction())) {
                CardType cardType = (CardType)
                intent.getExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
                if (cardType.equals(IccCardConstants.CardType.CT_4G_UICC_CARD)) {
                    mIsLteCardType = true;
                } else {
                    mIsLteCardType = false;
                }
                Log.i(TAG, "intent cardType = " + cardType.toString()
                        + ", mIsLteCardType " + mIsLteCardType);
                updateSwitch();
            } else if (action.equals(INTENT_ACTION_DISMISS_DIALOG)) {
                Log.i(TAG, "receive INTENT_ACTION_DISMISS_DIALOG.");
                if (mDialog != null && mDialog.isShowing()) {
                    Log.i(TAG, "data service is done, dismiss dialog.");
                    mDialog.dismiss();
                }
            }
        }
    };

    private ContentObserver mDataConnectionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange selfChange=" + selfChange);
            if (!selfChange) {
                updateSwitch();
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            ChangeRatModeService.SetRatMode setRatMode = (ChangeRatModeService.SetRatMode) binder;
            mService = setRatMode.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.use_lte_data_settings);
        initialize();
    }
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ChangeRatModeService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    private void initialize() {
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mSwitchBar = new Switch(inflater.getContext());
        int padding = getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
        mSwitchBar.setPadding(0, 0, padding, 0);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(mSwitchBar,
              new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                      ActionBar.LayoutParams.WRAP_CONTENT,
                      Gravity.CENTER_VERTICAL | Gravity.END));
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mIntentFilter.addAction(INTENT_ACTION_FINISH_SWITCH_SVLTE_RAT_MODE);
        mIntentFilter.addAction(INTENT_ACTION_CARD_TYPE);
        mIntentFilter.addAction(INTENT_ACTION_DISMISS_DIALOG);
        this.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1))),
                true, mDataConnectionObserver);
        int pattern = Settings.Global.getInt(this.getContentResolver(),
            mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                TelephonyManagerEx.SVLTE_RAT_MODE_4G);
        if (pattern == TelephonyManagerEx.SVLTE_RAT_MODE_3G) {
            mSwitchBarChecked = false;
        } else {
            mSwitchBarChecked = true;
        }
        if (sSet4GMode) {
            mSwitchBarChecked = true;
        }
        mSwitchBar.setChecked(mSwitchBarChecked);
        mSwitchBar.setOnCheckedChangeListener(mSwitchBarListener);
        createWarningDialog();
    }
    private void createWarningDialog() {
        mDialog = new AlertDialog.Builder(this)
            .setMessage(R.string.whether_enable_4G)
            .setPositiveButton(R.string.lte_only_dialog_button_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        Log.i(TAG, "User select yes!");
                        sSet4GMode = false;
                        switchSvlteRatMode(true);
                    }
                 })
            .setNegativeButton(R.string.lte_only_dialog_button_no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        Log.i(TAG, "User select no!");
                        openSwitcher();
                        sSet4GMode = true;
                    }
                 })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                 public void onCancel(DialogInterface dialog) {
                     Log.i(TAG, "User didn't select, dialog cancel!");
                     sSet4GMode = true;
                 }
             })
            .create();

    }
    private void showWarningDialog() {
        int pattern = Settings.Global.getInt(this.getContentResolver(),
            mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
            TelephonyManagerEx.SVLTE_RAT_MODE_4G);
        if (mDialog != null && !mDialog.isShowing()
                && pattern != TelephonyManagerEx.SVLTE_RAT_MODE_4G) {
            sSet4GMode = true;
            mDialog.show();
        }
    }

    private void openSwitcher() {
        boolean simInserted = isSIMInserted(PhoneConstants.SIM_ID_1);
        boolean airPlaneMode = isAirPlaneMode();
        boolean radioOn = isTargetSimRadioOn(PhoneConstants.SIM_ID_1);
        boolean callStateIdle = isCallStateIDLE();
        boolean simStateIsReady = isSimStateReady(UseLteDataSettings.this, PhoneConstants.SIM_ID_1);

        if ((simInserted && !airPlaneMode && radioOn
                && callStateIdle && !mSwitching
                && simStateIsReady && mIsLteCardType)
                || (CdmaFeatureOptionUtils.isCTLteTddTestSupport()))  {
            mSwitchBar.setEnabled(true);
            if (!mSwitchBar.isChecked()) {
                Log.i(TAG, "if user select no, open the switcher!");
                mSwitchBar.setChecked(true);
            }
        } else {
            mSwitchBar.setEnabled(false);
        }
    }

    private OnCheckedChangeListener mSwitchBarListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            TelephonyManager telephony = (TelephonyManager) UseLteDataSettings.this
                .getSystemService(Context.TELEPHONY_SERVICE);
            int dataFlag = TelephonyManager.DATA_ACTIVITY_NONE;
            dataFlag = telephony.getDataActivity();
            Log.i(TAG, "isChecked = " + isChecked + " dataFlag = " + dataFlag);
            if ((dataFlag == TelephonyManager.DATA_ACTIVITY_IN
                || dataFlag == TelephonyManager.DATA_ACTIVITY_OUT
                || dataFlag == TelephonyManager.DATA_ACTIVITY_INOUT) && isChecked) {
                showWarningDialog();
            } else {
                if (!isChecked) {
                    int lastMode = Settings.Global.
                                        getInt(UseLteDataSettings.this.getContentResolver(),
                                        mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                                            .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                                            TelephonyManagerEx.SVLTE_RAT_MODE_4G);
                    if (lastMode == TelephonyManagerEx.SVLTE_RAT_MODE_3G) {
                        Log.d(TAG, "close switch in the 3G mode!");
                        sSet4GMode = false;
                        return;
                    }
                }
                switchSvlteRatMode(isChecked);
                sSet4GMode = false;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mIsActiveWindow = true;
        mSwitching = false;
        updateSwitch();
        registerReceiver(mReceiver, mIntentFilter);
        TelephonyManager telephonyManager =
            (TelephonyManager) UseLteDataSettings.this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d(TAG, "onCallStateChanged, new state = " + state);
            updateSwitch();
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.d(TAG, "onServiceStateChanged, new state = " + serviceState);
            updateSwitch();
        }
    };

    private void updateSwitch() {
        boolean simInserted = isSIMInserted(PhoneConstants.SIM_ID_1);
        boolean airPlaneMode = isAirPlaneMode();
        boolean radioOn = isTargetSimRadioOn(PhoneConstants.SIM_ID_1);
        boolean callStateIdle = isCallStateIDLE();
        boolean simStateIsReady = isSimStateReady(UseLteDataSettings.this, PhoneConstants.SIM_ID_1);

        if ((simInserted && !airPlaneMode && radioOn
             && callStateIdle && !mSwitching
             && simStateIsReady && mIsLteCardType)
             || (CdmaFeatureOptionUtils.isCTLteTddTestSupport())) {
            mSwitchBar.setEnabled(true);
        } else {
            mSwitchBar.setEnabled(false);
        }
        int pattern = Settings.Global.getInt(this.getContentResolver(),
                            mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                                .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                            TelephonyManagerEx.SVLTE_RAT_MODE_4G);
        if (pattern == TelephonyManagerEx.SVLTE_RAT_MODE_3G) {
            mSwitchBarChecked = false;
        } else {
            mSwitchBarChecked = true;
        }
        if (sSet4GMode) {
            mSwitchBarChecked = true;
        }
        Log.d(TAG, "updateSwitch() simInserted=" + simInserted
                + ", airPlaneMode=" + airPlaneMode
                + ", radioOn=" + radioOn
                + ", callStateIdle=" + callStateIdle
                + ", mSwitching=" + mSwitching
                + ", mSwitchBarChecked =" + mSwitchBarChecked
                + ", simStateIsReady = " + simStateIsReady
                + ", mIsLteCardType = " + mIsLteCardType
                + ", isChecked = " + mSwitchBar.isChecked());
        if ((mSwitchBar.isChecked() && mSwitchBarChecked == false)
            || (!mSwitchBar.isChecked() && mSwitchBarChecked == true)) {
            Log.i(TAG, "update the switcher");
            mSwitchBar.setChecked(mSwitchBarChecked);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActiveWindow = false;
        unregisterReceiver(mReceiver);
        TelephonyManager.getDefault().listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private void switchSvlteRatMode(boolean isChecked) {
        if (mIsActiveWindow && mService != null) {
            try {
                showProgressDlg();
                mService.saveData(isChecked);
                mService.switchSvlte(isChecked);
            } catch (RemoteException e) {
                hideProgressDlg();
                Log.i(TAG, "telephony.switchSvlteRatMode() has exception");
            }
            mSwitching = true;
            updateSwitch();
        }
    }

    private void showProgressDlg() {
        Log.d(TAG, "showProgressDlg() with dialogMsg");
        mIsDialogShowing = true;
        showDialog(PROGRESS_DIALOG);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case PROGRESS_DIALOG:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(
                    R.string.gemini_data_connection_progress_message));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        default:
            return null;
        }
    }

    private void hideProgressDlg() {
        Log.d(TAG, "hideProgressDlg()");
        if (mIsDialogShowing) {
            dismissDialog(PROGRESS_DIALOG);
            mIsDialogShowing = false;
        }
    }

    ///return true if air plane mode on
    private boolean isAirPlaneMode() {
        return Settings.System.getInt(this.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1;
    }

    ///return true if sim one is insert
    private boolean isSIMInserted(int slotId) {
        try {
            ITelephony tmex = ITelephony.Stub.asInterface(android.os.ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            Log.i(TAG, "isSIMInserted = " + (tmex != null && tmex.hasIccCardUsingSlotId(slotId)));
            return (tmex != null && tmex.hasIccCardUsingSlotId(slotId));
        } catch (RemoteException e) {
            Log.i(TAG, "isSIMInserted = false because RemoteException");
            return false;
        }
    }

    ///one sim can has one or more slot id
    private boolean isTargetSimRadioOn(int simId) {
        int[] targetSubId = SubscriptionManager.getSubId(simId);
        if (targetSubId != null && targetSubId.length > 0) {
            for (int i = 0; i < targetSubId.length; i++) {
               if (isTargetSlotRadioOn(targetSubId[i])) {
                   Log.i(TAG, "isTargetSimRadioOn true simId = " + simId);
                   return true;
               }
            }
            Log.i(TAG, "isTargetSimRadioOn false simId = " + simId);
            return false;
        } else {
            Log.i(TAG, "isTargetSimRadioOn false because" +
                    " targetSubId[] = null or targetSubId[].length is 0  simId =" + simId);
            return false;
        }
    }

    ///return true if sim one is Radio on
    static boolean isTargetSlotRadioOn(int subId) {
        boolean radioOn = true;

        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (null == iTel) {
                Log.i(TAG, "isTargetSlotRadioOn = false because iTel = null");
                return false;
            }
            Log.i(TAG, "isTargetSlotRadioOn = " + iTel.isRadioOnForSubscriber(subId, PACKAGE_NAME));
            radioOn = iTel.isRadioOnForSubscriber(subId, PACKAGE_NAME);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Log.i(TAG, "isTargetSlotRadioOn radioOn = " + radioOn);
        return radioOn;
    }

    ///return true if sim one SIM_STATE_READY
    /**
     * judge if sim state is ready.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    private boolean isSimStateReady(int simId) {
        TelephonyManager telephonyManager = TelephonyManager.from(this);
        Log.i(TAG, "isSimStateReady = " + telephonyManager.getSimState(simId)
                + ", sim_state_ready == 5");
        return telephonyManager.getSimState(simId) == TelephonyManager.SIM_STATE_READY;
    }

    private boolean isCallStateIDLE() {
        TelephonyManager telephonyManager =
            (TelephonyManager) UseLteDataSettings.this.getSystemService(Context.TELEPHONY_SERVICE);
        int currPhoneCallState = telephonyManager.getCallState();
        Log.i(TAG, "use lte isCallStateIDLE = " +
                (currPhoneCallState == TelephonyManager.CALL_STATE_IDLE));
        return currPhoneCallState == TelephonyManager.CALL_STATE_IDLE;
    }

    /**
     * judge if sim state is ready.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    static boolean isSimStateReady(Context context, int simId) {
        int simState = TelephonyManager.from(context).getSimState(simId);
        Log.i(TAG, "isSimStateReady simState=" + simState);
        return simState == TelephonyManager.SIM_STATE_READY;
    }
}
