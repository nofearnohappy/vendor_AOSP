package com.mediatek.settings.plugin;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
//import android.telephony.PhoneRatFamily;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Switch;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.op09.plugin.R;

import java.util.List;

/**
 * For CT IRAT feature: User can enable data and choose the sim.
 */
public class DataConnectionSettings extends PreferenceActivity {

    private static final String TAG = "DataConnectionSettings";

    private static final String SIM_SLOT_1_KEY = "data_connection_sim_slot_1";
    private static final String SIM_SLOT_2_KEY = "data_connection_sim_slot_2";

    private static final int PROGRESS_DIALOG = 1000;

    private ConnectivityManager mConnectivityManager;
    private boolean mIsDataConnectActing = false;
    private IntentFilter mIntentFilter;
    private RadioPreference mSlot1Preference;
    private RadioPreference mSlot2Preference;
    private int mProDlgMsgId = -1;
    private DataConnectionEnabler mDataConnectionEnabler;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private ITelephony mTelephony;
    private StatusBarManager mStatusBarManager;
    private int mResetSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action=" + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                updateScreen();
            } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                updateScreen();
            } else if (isSimSwitchAction(action)) {
                handleSimSwitchBroadcast(intent);
            }
        }
    };

    private ContentObserver mDataConnectionSlotObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "mDataConnectionSlotObserver onChange selfChange=" + selfChange);
            if (!selfChange) {
                updateScreen();
            }
        }
    };

    private ContentObserver mDataConnectionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "mDataConnectionObserver onChange selfChange=" + selfChange);
            if (!selfChange) {
                hideProgressDlg();
                updateScreen();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.data_connection_settings);
        this.getListView().setBackgroundColor(
                getResources().getColor(R.color.dashboard_background_color));
        mSlot1Preference = (RadioPreference) findPreference(SIM_SLOT_1_KEY);
        mSlot2Preference = (RadioPreference) findPreference(SIM_SLOT_2_KEY);
        if (TelephonyManager.from(this).getSimCount() == 1) {
            Log.d(TAG, "Single SIM project only show one item");
            getPreferenceScreen().removePreference(mSlot2Preference);
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SET_PHONE_RAT_FAMILY_DONE);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SET_PHONE_RAT_FAMILY_FAILED);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = TelephonyManager.from(this);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));

        LayoutInflater inflater =
            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Switch actionBarSwitch = new Switch(inflater.getContext());
        final int padding = getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
        actionBarSwitch.setPadding(0, 0, padding, 0);
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(
                actionBarSwitch,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));
        mDataConnectionEnabler = new DataConnectionEnabler(this, actionBarSwitch);
        // TODO: Fix switch data JE issue: assign a default value for mProDlgMsgId
        // to avoid string(-1) not found issue.
        mProDlgMsgId = R.string.gemini_data_connection_progress_message;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        registerPhoneStateListener();
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.MOBILE_DATA),
                true, mDataConnectionObserver);
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SETTING),
                true, mDataConnectionSlotObserver);
        updateScreen();
        if (mDataConnectionEnabler != null) {
            mDataConnectionEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterPhoneStateListener();
        unregisterReceiver(mReceiver);
        getContentResolver().unregisterContentObserver(mDataConnectionObserver);
        getContentResolver().unregisterContentObserver(mDataConnectionSlotObserver);
        if (mDataConnectionEnabler != null) {
            mDataConnectionEnabler.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDlg();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.equals(mSlot1Preference)) {
            handleDataConnectionChange(PhoneConstants.SIM_ID_1);
            return true;
        } else if (preference.equals(mSlot2Preference)) {
            handleDataConnectionChange(PhoneConstants.SIM_ID_2);
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    private void registerPhoneStateListener() {
        mPhoneStateListener = getPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);
        Log.d(TAG, "Register registerPhoneStateListener");
    }

    private void unregisterPhoneStateListener() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
        Log.d(TAG, "Register unregisterPhoneStateListener");
    }

    private PhoneStateListener getPhoneStateListener() {
        return new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState state) {
                Log.d(TAG, "PhoneStateListener: onServiceStateChanged");
                updateScreen();
            }
        };
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case PROGRESS_DIALOG:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(mProDlgMsgId));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        default:
            return null;
        }
    }

    private void updateScreen() {
        boolean enabled = mTelephonyManager.getDataEnabled() && !mIsDataConnectActing
                && DataConnectionEnabler.isGPRSEnable(this);
        boolean slot1RadioOn = DataConnectionEnabler.isTargetSimRadioOn(PhoneConstants.SIM_ID_1);
        boolean slot2RadioOn = DataConnectionEnabler.isTargetSimRadioOn(PhoneConstants.SIM_ID_2);
        boolean slot1NotLocked = slot1RadioOn
                && DataConnectionEnabler.isSimStateReady(this, PhoneConstants.SIM_ID_1);
        boolean slot2NotLocked = slot2RadioOn
                && DataConnectionEnabler.isSimStateReady(this, PhoneConstants.SIM_ID_2);
        int dataConnectionId = SubscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "updateSwitcherState enalbed=" + enabled
                + ", mIsDataConnectActing=" + mIsDataConnectActing
                + ", slot1RadioOn=" + slot1RadioOn + ", slot2RadioOn=" + slot2RadioOn
                + ", slot1NotLocked=" + slot1NotLocked + ", slot2NotLocked=" + slot2NotLocked
                + ", dataConnectionId=" + dataConnectionId);

        mSlot1Preference.setEnabled(slot1RadioOn && slot1NotLocked);
        mSlot2Preference.setEnabled(slot2RadioOn && slot2NotLocked);
        if (SubscriptionManager.getSlotId(dataConnectionId) == PhoneConstants.SIM_ID_1) {
            mSlot1Preference.setChecked(true);
            mSlot2Preference.setChecked(false);
        } else if (SubscriptionManager.getSlotId(dataConnectionId) == PhoneConstants.SIM_ID_2) {
            mSlot1Preference.setChecked(false);
            mSlot2Preference.setChecked(true);
        } else {
            mSlot1Preference.setChecked(false);
            mSlot2Preference.setChecked(false);
        }
    }

    private void handleDataConnectionChange(int newSlot) {
        Log.d(TAG, "handleDataConnectionChange newSlot=" + newSlot);
        if (SubscriptionManager.getSlotId(SubscriptionManager.getDefaultDataSubId()) != newSlot) {
            List<SubscriptionInfo> si =
                    SubscriptionManager.from(this).getActiveSubscriptionInfoList();
            if (si != null && si.size() > 0) {
                for (int i = 0; i < si.size(); i++) {
                    SubscriptionInfo subInfoRecord = si.get(i);
                    if (newSlot == subInfoRecord.getSimSlotIndex()) {
                        int subId = subInfoRecord.getSubscriptionId();
                        Log.d(TAG, "handleDataConnectionChange newSlot = "
                                + newSlot + " subId = " + subId);
                        switchDataDefaultSub(subId);
                        return;
                    }
                }
            }
        }
    }

    private void showProgressDlg(int dialogMsg) {
        Log.d(TAG, "showProgressDlg() with dialogMsg = " + dialogMsg);
        mProDlgMsgId = dialogMsg;
        mIsDataConnectActing = true;
        showDialog(PROGRESS_DIALOG);
    }

    private void hideProgressDlg() {
        Log.d(TAG, "hideProgressDlg()");
        if (mIsDataConnectActing) {
            Log.d(TAG, "real hideProgressDlg()");
            mIsDataConnectActing = false;
            dismissDialog(PROGRESS_DIALOG);
        }
    }

    private static int getDataConnectionSlotId(Context context) {
        ///M: Data framework defined that this id is actual (slotId + 1),
        // we need to minus 1 before return
        return (int) Settings.System.getInt(context.getContentResolver(),
                Settings.System.GPRS_CONNECTION_SETTING,
                Settings.System.GPRS_CONNECTION_SETTING_DEFAULT) - 1;
    }

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    /**
     * switch data connection default SIM.
     *
     * @param value: sim id of the new default SIM
     */
    private void switchDataDefaultSub(int subId) {
        boolean enableBefore = mTelephonyManager.getDataEnabled();
        mResetSubId = SubscriptionManager.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId) &&
            subId != mResetSubId) {
            SubscriptionManager.from(this).setDefaultDataSubId(subId);
            if (enableBefore) {
                mTelephonyManager.setDataEnabled(subId, true);
                mTelephonyManager.setDataEnabled(mResetSubId, false);
            }
        }
        if (handleSimSwitch(subId)) {
            showProgressDlg(R.string.gemini_data_connection_progress_message);
        }
    }

    private boolean handleSimSwitch(int subId) {
        boolean ret = false;
        /*int count = mTelephonyManager.getPhoneCount();
        PhoneRatFamily[] rats = new PhoneRatFamily[count];
        Log.d(TAG, "handleSimSwitch()... " + count);
        for (int i = 0; i < rats.length; i++) {
            if (SubscriptionManager.getPhoneId(subId) == i) {
                if (isLteSupport()) {
                    rats[i] = new PhoneRatFamily(i, PhoneRatFamily.PHONE_RAT_FAMILY_3G |
                            PhoneRatFamily.PHONE_RAT_FAMILY_4G);
                } else {
                    rats[i] = new PhoneRatFamily(i, PhoneRatFamily.PHONE_RAT_FAMILY_3G);
                }
            } else {
                rats[i] = new PhoneRatFamily(i, PhoneRatFamily.PHONE_RAT_FAMILY_2G);

            }
            Log.d(TAG, "handleSimSwitch()... rat[" + i + "]: " + rats[i]);
        }
        try {
            setStatusBarEnableStatus(false);
            ret = mTelephony.setPhoneRat(rats);
        } catch (RemoteException e) {
            Log.d(TAG, "RemoteException, handleSimSwitch fail");
            e.printStackTrace();
            ret = false;
        }
        if (!ret) {
            Log.d(TAG, "setPhoneRat fail.");
            resetDefaultDataSubId();
            setStatusBarEnableStatus(true);
        }*/
        Log.d(TAG, "handleSimSwitch()... ret: " + ret);
        return ret;
    }

    private void resetDefaultDataSubId() {
        Log.d(TAG, "resetDefaultDataSubId()... + resetSubId: " + mResetSubId);
        SubscriptionManager.from(this).setDefaultDataSubId(mResetSubId);
    }

    private void setStatusBarEnableStatus(boolean enabled) {
        Log.i(TAG, "setStatusBarEnableStatus(" + enabled + ")");
        if (mStatusBarManager == null) {
            mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        }
        if (mStatusBarManager != null) {
            if (enabled) {
                mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
            } else {
                mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND |
                        StatusBarManager.DISABLE_RECENT |
                        StatusBarManager.DISABLE_HOME);
            }
        } else {
            Log.e(TAG, "Fail to get status bar instance");
        }
    }

    private boolean isSimSwitchAction(String action) {
        return action.equals(TelephonyIntents.ACTION_SET_PHONE_RAT_FAMILY_DONE) ||
               action.equals(TelephonyIntents.ACTION_SET_PHONE_RAT_FAMILY_FAILED);
    }

    private void handleSimSwitchBroadcast(Intent intent) {
        setStatusBarEnableStatus(true);
        if (mIsDataConnectActing) {
            hideProgressDlg();
        }
        updateScreen();
        //If switch fail, rollback default data value
        if (TelephonyIntents.ACTION_SET_PHONE_RAT_FAMILY_FAILED.equals(intent.getAction())) {
            resetDefaultDataSubId();
        }
    }

    public static boolean isLteSupport() {
        return SystemProperties.get("ro.mtk_lte_support").equals("1");
    }
}
