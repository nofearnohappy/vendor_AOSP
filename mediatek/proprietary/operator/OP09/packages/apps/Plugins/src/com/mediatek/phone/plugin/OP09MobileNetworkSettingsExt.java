package com.mediatek.phone.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController;
import com.mediatek.internal.telephony.ltedc.svlte.SvlteRatController.RoamingMode;

import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.op09.plugin.R;
import com.mediatek.phone.ext.DefaultMobileNetworkSettingsExt;
import com.mediatek.telephony.TelephonyManagerEx;


import java.util.List;

/**
 * REMOVE ALL,just only TDD data only and APN.
 */
@PluginImpl(interfaceName = "com.mediatek.phone.ext.IMobileNetworkSettingsExt")
public class OP09MobileNetworkSettingsExt extends DefaultMobileNetworkSettingsExt {

    private static final String TAG = "OP09MobileNetworkSettingsExt";
    private static final String CT_APN_KEY = "pref_ct_apn_key";
    private static final String SINGLE_LTE_DATA = "single_lte_data";
    private static final String INTENT_ACTION_FINISH_SWITCH_SVLTE_RAT_MODE =
            "com.mediatek.intent.action.FINISH_SWITCH_SVLTE_RAT_MODE";
    private static final String INTENT_ACTION_CARD_TYPE =
        "android.intent.action.CDMA_CARD_TYPE";
    private static final String PACKAGE_NAME = "com.mediatek.op09.plugin";
    private Context mOP09Context;
    private SwitchPreference mSwitchPreference;
    private IntentFilter mIntentFilter;
    private boolean mSwitching;
    private boolean mIsLteCardType;
    private TelephonyManagerEx mTelephonyManagerEx = null;

    // TODO:: Work around for ALPS01976671
    private boolean mIsRegister = false;
    /**
     * Constructor method.
     * @param context Context
     */
    public OP09MobileNetworkSettingsExt(Context context) {
        mOP09Context = context;
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.i(TAG, "OP09 onPreferenceTreeClick preference = " + preference);

        if (null != (PreferenceScreen) preferenceScreen.findPreference(CT_APN_KEY)
                && preference == (PreferenceScreen) preferenceScreen.findPreference(CT_APN_KEY)) {
            preference.getContext().startActivity(preference.getIntent());
            Log.i(TAG, "OP09 onPreferenceTreeClick preference key = CT_APN");
            return true;
        } else if ((preference == mSwitchPreference) ||
                    preference.getKey().equals(SINGLE_LTE_DATA)) {
            if (preference != mSwitchPreference) {
                Log.i(TAG, "SINGLE_LTE_DATA!!!!!");
            }

            Log.i(TAG, "OP09 onPreferenceTreeClick preference key = singleltedata");
            SwitchPreference swp = (SwitchPreference) preference;
            boolean isChecked = swp.isChecked();
            Log.i(TAG, "onPreferenceTreeClick isChecked = " + isChecked);
            try {
                Settings.Global.putInt(mOP09Context.getContentResolver(),
                    mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                        .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                        (isChecked ? TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY
                                : TelephonyManagerEx.SVLTE_RAT_MODE_4G));
                switchSvlteRatMode(isChecked);
            } catch (RemoteException e) {
                Log.i(TAG, "op09 only lte data onPreferenceTreeClick RemoteException");
            }
            mSwitching = true;
            updateSwitch();
            return true;
        }
        return false;
    }

    private void switchSvlteRatMode(boolean isChecked) throws RemoteException {
        ITelephonyEx telephony = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        if (null != telephony) {
            telephony.switchRadioTechnology(isChecked ?
                  SvlteRatController.RAT_MODE_SVLTE_4G :
                  (SvlteRatController.RAT_MODE_SVLTE_2G
                   | SvlteRatController.RAT_MODE_SVLTE_3G
                   | SvlteRatController.RAT_MODE_SVLTE_4G));
            Log.i(TAG, "switchSvlteRatMode isChecked = " + isChecked +
                    " set value is " + (isChecked ?
                    SvlteRatController.RAT_MODE_SVLTE_4G :
                    (SvlteRatController.RAT_MODE_SVLTE_2G
                     | SvlteRatController.RAT_MODE_SVLTE_3G
                     | SvlteRatController.RAT_MODE_SVLTE_4G)));
        }
    }

    /*
    public void initOtherMobileNetworkSettings(PreferenceActivity activity) {
        Log.d(TAG, "initOtherMobileNetworkSettings remove all");
        PreferenceScreen prefScreen = activity.getPreferenceScreen();
        prefScreen.removeAll();
        mIsRegister = true;
        /// add TDD data only feature
        Log.d(TAG, "initOtherMobileNetworkSettings add TDD data only feature" +
                   " with mIsRegister = " + mIsRegister);
        mSwitchPreference = new SwitchPreference(activity);
        mSwitchPreference.setTitle(mOP09Context.getString(R.string.only_use_LTE_data));
        mSwitchPreference.setKey(SINGLE_LTE_DATA);
        mSwitchPreference.setSummaryOn(mOP09Context.getString(R.string.only_use_LTE_data_summary));
        mSwitchPreference.setSummaryOff(mOP09Context.getString(R.string.only_use_LTE_data_summary));
        prefScreen.addPreference(mSwitchPreference);

        /// add CT APN feature
        Log.d(TAG, "initOtherMobileNetworkSettings add CT APN");
        PreferenceScreen prefAPN = new PreferenceScreen(activity, null);
        prefAPN.setKey(CT_APN_KEY);
        prefAPN.setTitle(mOP09Context.getResources().getText(R.string.apn_settings));
        prefAPN.setPersistent(false);
        Intent targetIntent = new Intent(Settings.ACTION_APN_SETTINGS);
        prefAPN.setIntent(targetIntent);
        prefScreen.addPreference(prefAPN);
        updateSwitch();
    }
    */
    @Override
    public void initOtherMobileNetworkSettings(PreferenceActivity activity, int subId) {
        Log.d(TAG, "initOtherMobileNetworkSettings remove all");
        PreferenceScreen prefScreen = activity.getPreferenceScreen();
        prefScreen.removeAll();
        if (!mIsRegister) {
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mIntentFilter.addAction(INTENT_ACTION_FINISH_SWITCH_SVLTE_RAT_MODE);
            mIntentFilter.addAction(INTENT_ACTION_CARD_TYPE);
            mOP09Context.registerReceiver(mReceiver, mIntentFilter);
            mOP09Context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(mTelephonyManagerEx
                                              .getCdmaRatModeKey(SubscriptionManager
                                                 .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1))),
                    true, mDataConnectionObserver);

            List<SubscriptionInfo> si =  SubscriptionManager.from(mOP09Context)
                .getActiveSubscriptionInfoList();

            if (si != null && si.size() > 0) {
                for (int i = 0; i < si.size(); i++) {
                    SubscriptionInfo subInfo = si.get(i);
                    subId = subInfo.getSubscriptionId();
                    mOP09Context.getContentResolver().registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId),
                        true, mDataConnectionStateObserver);
                }
            }

        TelephonyManager telephonyManager =
            (TelephonyManager) mOP09Context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        telephonyManager.listen(mPhoneStateListener,
            PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
            mIsRegister = true;
        }
        /// add TDD data only feature
        int slotId = SubscriptionManager.getSlotId(subId);
        if ((slotId == PhoneConstants.SIM_ID_1)
            || (CdmaFeatureOptionUtils.isCTLteTddTestSupport())) {
            Log.d(TAG, "initOtherMobileNetworkSettings add TDD data only feature" +
                    " with mIsRegister = " + mIsRegister);
            mSwitchPreference = new SwitchPreference(activity);
            mSwitchPreference.setTitle(mOP09Context.getString(R.string.only_use_LTE_data));
            mSwitchPreference.setKey(SINGLE_LTE_DATA);
            mSwitchPreference.setSummaryOn(
                mOP09Context.getString(R.string.only_use_LTE_data_summary));
            mSwitchPreference.setSummaryOff(
                mOP09Context.getString(R.string.only_use_LTE_data_summary));
            prefScreen.addPreference(mSwitchPreference);
        }


        /// add CT APN feature
        Log.d(TAG, "initOtherMobileNetworkSettings add CT APN");
        PreferenceScreen prefAPN = new PreferenceScreen(activity, null);
        prefAPN.setKey(CT_APN_KEY);
        prefAPN.setTitle(mOP09Context.getResources().getText(R.string.apn_settings));
        prefAPN.setPersistent(false);
        Intent targetIntent = new Intent(Settings.ACTION_APN_SETTINGS);
        targetIntent.putExtra("sub_id", subId);
        prefAPN.setIntent(targetIntent);
        prefScreen.addPreference(prefAPN);
        updateSwitch();
    }

    //TODO: fix build error, should add API in plugin interface @Override
    @Override
    public void unRegister() {
        Log.d(TAG, "unRegister mIsRegister = " + mIsRegister);
        if (mIsRegister) {
            mOP09Context.unregisterReceiver(mReceiver);
            mOP09Context.getContentResolver().unregisterContentObserver(mDataConnectionObserver);
            mOP09Context.getContentResolver().unregisterContentObserver(
                mDataConnectionStateObserver);
            TelephonyManager.getDefault().listen(
                mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mIsRegister = false;
        }
    }

    /**
     * Resume the plugin.
     * @return void.
     */
    public void onResume() {
        mSwitching = false;
        updateSwitch();

    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d(TAG, "PhoneStateListener, onCallStateChanged new state=" + state);
            updateSwitch();
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            // TODO Auto-generated method stub
            super.onDataConnectionStateChanged(state);
            Log.d(TAG, "PhoneStateListener, onDataConnectionStateChanged new state=" + state);
            updateSwitch();
        }

    };

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

    private ContentObserver mDataConnectionStateObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "mDataConnectionState onChange selfChange=" + selfChange);
            if (!selfChange) {
                updateSwitch();
            }
        }
    };

    private void updateSwitch() {
        if (mSwitchPreference != null) {
            int patternLteDataOnly = Settings.Global.getInt(
                    mOP09Context.getContentResolver(),
                    mTelephonyManagerEx.getCdmaRatModeKey(SubscriptionManager
                        .getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1)),
                    TelephonyManagerEx.SVLTE_RAT_MODE_4G);
            mSwitchPreference.setChecked(
                    patternLteDataOnly == TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY);
            boolean simInserted = isSIMInserted(PhoneConstants.SIM_ID_1);
            boolean airPlaneMode = isAirPlaneMode();
            boolean callStateIdle = isCallStateIDLE();
            boolean simStateIsReady = isSimStateReady(mOP09Context, PhoneConstants.SIM_ID_1);
            /// if data close && default data is not sim one, tdd data only can not choose
            boolean dataEnable = TelephonyManager.from(mOP09Context).getDataEnabled();
            int slotId = SubscriptionManager.getSlotId(SubscriptionManager.getDefaultDataSubId());
            boolean isCTEnableData = (PhoneConstants.SIM_ID_1 == slotId) && dataEnable;
            RoamingMode roamingMode = SvlteRatController.getInstance().getRoamingMode();
            boolean roamingEnable  = (roamingMode != RoamingMode.ROAMING_MODE_HOME);
            Log.d(TAG, "updateSwitch() simInserted=" + simInserted + ", airPlaneMode="
                    + airPlaneMode
                    + ", callStateIdle=" + callStateIdle + ", dataEnable = " + dataEnable
                    + ", slotId = " + slotId + " isCTEnableData = " + isCTEnableData
                    + ", patternLteDataOnly = " + patternLteDataOnly
                    + ", mIsLteCardType = " + mIsLteCardType
                    + ", simStateIsReady = " + simStateIsReady
                    + ", roamingEnable = " + roamingEnable
                    + ", mSwitching = " + mSwitching
                    + ",  SVLTE_RAT_MODE_4G = 0,SVLTE_RAT_MODE_3G = 1, "
                    + "SVLTE_RAT_MODE_4G_DATA_ONLY = 2");
            Log.d(TAG, "updata information ");
            if ((simInserted && !isAirPlaneMode() && mIsLteCardType
                && isCallStateIDLE() && isCTEnableData && simStateIsReady
                && (patternLteDataOnly != TelephonyManagerEx.SVLTE_RAT_MODE_3G)
                && !roamingEnable) || CdmaFeatureOptionUtils.isCTLteTddTestSupport()) {
                mSwitchPreference.setEnabled(true);
            } else {
                mSwitchPreference.setEnabled(false);
            }
        }
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

    ///return true if air plane mode on
    private boolean isAirPlaneMode() {
        Log.i(TAG, "isAirPlaneMode = " + (Settings.System.getInt(mOP09Context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1));
        return Settings.System.getInt(mOP09Context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1;
    }

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
            Log.i(TAG, "isTargetSimRadioOn false because  targetSubId[] = null" +
                " or targetSubId[].length is 0  simId = " + simId);
            return false;
        }
    }

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
            radioOn = false;
            Log.i(TAG, "isTargetSlotRadioOn radioOn = false because catch RemoteException");
        }
        Log.i(TAG, "isTargetSlotRadioOn radioOn = " + radioOn);
        return radioOn;
    }

    /**
     * judge if sim state is ready,use in updateSwitch.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    private boolean isSimStateReady(int simId) {
        TelephonyManager mTelephonyManager = TelephonyManager.from(mOP09Context);
        Log.i(TAG, "isSimStateReady = " +
                mTelephonyManager.getSimState(simId) +
                ", sim_state_ready == 5");
        return mTelephonyManager.getSimState(simId) == TelephonyManager.SIM_STATE_READY;
    }

    private boolean isCallStateIDLE() {
        TelephonyManager telephonyManager =
            (TelephonyManager) mOP09Context.getSystemService(Context.TELEPHONY_SERVICE);
        int currPhoneCallState = telephonyManager.getCallState();
        Log.i(TAG, "op09 mobile isCallStateIDLE = " +
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

    /**
     * Judge if it is CT Plugin.
     * @return true if it it CT Plugin.
     */
    @Override
    public boolean isCtPlugin() {
        return true;
    }
}
