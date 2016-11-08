package com.mediatek.op.telephony;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.WindowManager;

import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.UiccController;

import com.mediatek.internal.R;
import com.mediatek.internal.telephony.ltedc.svlte.SvltePhoneProxy;

/**
 * Customization from CT for data when roaming.
 * Popup reminder dialog when roaming first time.
 */
public class DataRoamingCustomization extends Handler {
    private static final String TAG = "DataRoamingCustomization";

    private static final int EVENT_DATA_SETTING_CHANGED = 1;
    private static final int EVENT_GSM_SERVICE_STATE_CHANGED = 2;
    private static final int EVENT_CDMA_SERVICE_STATE_CHANGED = 3;
    private static final int EVENT_DATA_ROAMING_SWITCH_CHANGED = 4;

    private static final int OP09_SIM_SLOT = 0;
    private static final String OPERATOR_OP09 = "OP09";
    private static final String SEGDEFAULT = "SEGDEFAULT";
    // CT C 6M support
    public static final String MTK_CT6M_SUPPORT = "ro.ct6m_support";

    // Feature support.
    public static final String SUPPORT_YES = "1";

    private static final String CHINA_MCC = "460";
    private static final int MCC_LENGTH = 3;

    private static final String PREFERENCE_NAME = "roaming_customization";
    private static final String FIRST_ROAMING_KEY = "first_roaming";
    private static final String LAST_REG_STATE_KEY = "last_reg_state";
    private static final String LAST_OPERATOR_NUMERIC_KEY = "last_operator_numeric";

    private Context mContext;
    private ContentResolver mResolver;
    private PhoneBase mGsmPhone;
    private PhoneBase mCdmaPhone;
    private SvltePhoneProxy mSvltePhoneProxy;

    private String mUri = Settings.Global.MOBILE_DATA;
    private String mRoamingUri = Settings.Global.DATA_ROAMING;
    private String mFirstRoamingKey = FIRST_ROAMING_KEY;
    private String mLastRegStateKey = LAST_REG_STATE_KEY;
    private String mLastOperatorNumericKey = LAST_OPERATOR_NUMERIC_KEY;

    private int mLastRilRegState = ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING;
    private String mLastOpNumeric = "00000";

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            logd("Observer Onchange");
            removeMessages(EVENT_DATA_SETTING_CHANGED);
            sendEmptyMessage(EVENT_DATA_SETTING_CHANGED);
        }
    };

    private ContentObserver mRoamingSwitchObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            logd("Data roaming switch changed.");
            removeMessages(EVENT_DATA_ROAMING_SWITCH_CHANGED);
            sendEmptyMessage(EVENT_DATA_ROAMING_SWITCH_CHANGED);
        }
    };

    /**
     * Construct DataRoamingCustomization with context and phone.
     * @param svltePhoneProxy SvltePhoneProxy
     */
    public DataRoamingCustomization(SvltePhoneProxy svltePhoneProxy) {
        mSvltePhoneProxy = svltePhoneProxy;
        mContext = svltePhoneProxy.getContext();
        mGsmPhone = svltePhoneProxy.getLtePhone();
        mCdmaPhone = svltePhoneProxy.getNLtePhone();
        mResolver = mContext.getContentResolver();

        mGsmPhone.registerForServiceStateChanged(
                this, EVENT_GSM_SERVICE_STATE_CHANGED, null);
        mCdmaPhone.registerForServiceStateChanged(
                this, EVENT_CDMA_SERVICE_STATE_CHANGED, null);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mContext.registerReceiver(mIntentReceiver, filter);
        logd("DataRoamingCustomization constructor for OP09");
    }

    @Override
    public void handleMessage(Message msg) {
        int msgId = msg.what;
        logd("handleMessage: " + msgIdToString(msgId) + "(id=" + msgId + ")");
        switch (msgId) {
        case EVENT_DATA_SETTING_CHANGED:
        case EVENT_DATA_ROAMING_SWITCH_CHANGED:
            checkFirstRoaming();
            break;
        case EVENT_GSM_SERVICE_STATE_CHANGED:
        case EVENT_CDMA_SERVICE_STATE_CHANGED:
            AsyncResult ar = (AsyncResult) msg.obj;
            ServiceState serviceState = (ServiceState) ar.result;
            logd("serviceState = " + serviceState.toString());
            final int dataRegState = serviceState.getDataRegState();
            logd("dataRegState = " + dataRegState);
            if (dataRegState == ServiceState.STATE_IN_SERVICE) {
                final int rilDataRegState = serviceState.getRilDataRegState();
                final String operatorNumeric = serviceState.getOperatorNumeric();
                logd("rilDataRegState = " + rilDataRegState + ",operatorNumeric = " +
                        operatorNumeric + ",mLastRilRegState = " + mLastRilRegState +
                        ",mLastOpNumeric = " + mLastOpNumeric);
                if (isMccInvalid(operatorNumeric)) {
                    return;
                }
                if (rilDataRegState != mLastRilRegState ||
                        (mLastOpNumeric != null && operatorNumeric != null &&
                        !mLastOpNumeric.equals(operatorNumeric))) {
                    saveLastRegInfo(rilDataRegState, operatorNumeric);
                    if (rilDataRegState == ServiceState.REGISTRATION_STATE_ROAMING) {
                        checkFirstRoaming();
                    } else if (rilDataRegState == ServiceState.REGISTRATION_STATE_HOME_NETWORK) {
                        setFirstRoamingFlag(true);
                    }
                }
            }
            break;
        default:
            break;
        }
    }

    private boolean isMccInvalid(String opNumeric) {
        if (TextUtils.isEmpty(opNumeric)) {
            logd("isMccInvalid, opNumeric is empty");
            return false;
        }
        String mcc = opNumeric.substring(0, MCC_LENGTH);
        logd("isMccInvalid, mcc=" + mcc);
        return TextUtils.isEmpty(mcc) || mcc.equals("000") || mcc.equals("N/A");
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("onReceive: action=" + action);
            if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                onSubInfoReady();
            }
        }
    };

    private void onSubInfoReady() {
        logd("onSubInfoReady");
        int subId = SubscriptionManager.getSubIdUsingPhoneId(mSvltePhoneProxy.getPhoneId());
        logd("onSubInfoReady: subId:" + subId);

        if (SubscriptionManager.isValidSubscriptionId(subId)) {

            // For Dual SIM phones, need to change URI.
            if (TelephonyManager.getDefault().getSimCount() > 1) {
                mUri = Settings.Global.MOBILE_DATA + subId;
                mRoamingUri = Settings.Global.DATA_ROAMING + subId;
            }

            logd("onSubInfoReady(), mUri=" + mUri + " mRoamingUri=" + mRoamingUri);

            // M: Register for mobile data enabled.
            if (mUri != null && !mUri.equals(Settings.Global.MOBILE_DATA)) {
                mResolver.unregisterContentObserver(mObserver);
            }
            mResolver.registerContentObserver(Settings.Global.getUriFor(mUri), false, mObserver);

            // M: Register for data roaming enabled with subId
            if (mRoamingUri != null && !mRoamingUri.equals(Settings.Global.DATA_ROAMING)) {
                mResolver.unregisterContentObserver(mRoamingSwitchObserver);
            }
            mResolver.registerContentObserver(
                    Settings.Global.getUriFor(mRoamingUri), false, mRoamingSwitchObserver);

            //Get roaming info from preference
            mFirstRoamingKey = FIRST_ROAMING_KEY + subId;
            mLastRegStateKey = LAST_REG_STATE_KEY + subId;
            mLastOperatorNumericKey = LAST_OPERATOR_NUMERIC_KEY + subId;
            SharedPreferences roamingPreferences = mContext.getSharedPreferences(
                    PREFERENCE_NAME, 0);
            mLastRilRegState = roamingPreferences.getInt(mLastRegStateKey,
                    ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING);
            mLastOpNumeric = roamingPreferences.getString(mLastOperatorNumericKey, "00000");

            // Trigger a self change to check whether need to popup prompt
            // dialog, in case the sub info ready is later than network
            // registered.
            mObserver.onChange(true);
            mRoamingSwitchObserver.onChange(true);
        }
    }

    private boolean isCdmaCard() {
        int phoneId = mSvltePhoneProxy.getPhoneId();
        logd("isCdmaCard phoneId=" + phoneId);
        if (isOP09ASupport()) {
            logd("isOP09ASupport ");
            if (phoneId == OP09_SIM_SLOT) {
                return true;
            }
        } else if (isOP09CSupport()) {
            logd("isOP09CSupport ");
            int[] cardType = UiccController.getInstance().getC2KWPCardType();
            if ((cardType[phoneId] & UiccController.CARD_TYPE_RUIM) > 0
                || (cardType[phoneId] & UiccController.CARD_TYPE_CSIM) > 0) {
                return true;
            }
        }
        return false;
    }

    private void checkFirstRoaming() {
        if (!isCdmaCard()) {
            logd("checkFirstRoaming, is not cdma card");
            return;
        }

        boolean userDataEnabled = Settings.Global.getInt(mResolver, mUri, 1) == 1;

        /// M: OP09-A don't have data roaming enabled switch.
        boolean dataRoamingEnabled = isOP09ASupport()
                || Settings.Global.getInt(mResolver, mRoamingUri, 1) == 1;
        boolean isRoaming = mLastRilRegState == ServiceState.REGISTRATION_STATE_ROAMING;
        SharedPreferences roamingPreferences = mContext.getSharedPreferences(PREFERENCE_NAME, 0);
        boolean firstRoaming = roamingPreferences.getBoolean(mFirstRoamingKey, true);

        logd("checkFirstRoaming, userDataEnabled=" + userDataEnabled
                + ", dataRoamingEnabled=" + dataRoamingEnabled
                + ",isRoaming=" + isRoaming + ",firstRoaming=" + firstRoaming);
        if (userDataEnabled && isRoaming && firstRoaming && dataRoamingEnabled) {
            popupDialog();
            setFirstRoamingFlag(false);
        }
    }

    private void setFirstRoamingFlag(boolean first) {
        logd("setFirstRoamingFlag, first=" + first);
        SharedPreferences roamingPreferences = mContext.getSharedPreferences(PREFERENCE_NAME, 0);
        Editor roamingEditor = roamingPreferences.edit();
        roamingEditor.putBoolean(mFirstRoamingKey, first);
        roamingEditor.commit();
    }

    private void saveLastRegInfo(int regState, String operatorNumeric) {
        logd("saveLastRegInfo, regState=" + regState + ",operatorNumeric=" + operatorNumeric);
        mLastRilRegState = regState;
        mLastOpNumeric = operatorNumeric;
        SharedPreferences roamingPreferences = mContext.getSharedPreferences(PREFERENCE_NAME, 0);
        Editor roamingEditor = roamingPreferences.edit();
        roamingEditor.putInt(mLastRegStateKey, regState);
        roamingEditor.putString(mLastOperatorNumericKey, operatorNumeric);
        roamingEditor.commit();
    }

    /**
     * Unregister from all events it registered for.
     */
    public void dispose() {
        mResolver.unregisterContentObserver(mObserver);
        mGsmPhone.unregisterForServiceStateChanged(this);
        mCdmaPhone.unregisterForServiceStateChanged(this);
        mContext.unregisterReceiver(mIntentReceiver);
    }

    private void popupDialog() {
        logd("popupDialog for data enabled on roaming network.");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.roaming_message);
        builder.setPositiveButton(R.string.known, null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        dialog.show();
    }

    private boolean isOP09CSupport() {
        return SUPPORT_YES.equals(SystemProperties.get(MTK_CT6M_SUPPORT, "0"));
    }

    private boolean isOP09ASupport() {
        return OPERATOR_OP09.equals(SystemProperties.get("ro.operator.optr", ""))
                && SEGDEFAULT.equals(SystemProperties.get("ro.operator.seg", ""));
    }

    private String msgIdToString(int id) {
        switch (id) {
        case EVENT_DATA_SETTING_CHANGED:
            return "EVENT_DATA_SETTING_CHANGED";
        case EVENT_GSM_SERVICE_STATE_CHANGED:
            return "EVENT_GSM_SERVICE_STATE_CHANGED";
        case EVENT_CDMA_SERVICE_STATE_CHANGED:
            return "EVENT_CDMA_SERVICE_STATE_CHANGED";
        case EVENT_DATA_ROAMING_SWITCH_CHANGED:
            return "EVENT_DATA_ROAMING_SWITCH_CHANGED";
        default:
            return "unknown event";
        }
    }

    private void logd(String s) {
        Rlog.d(TAG + "[" + mSvltePhoneProxy.getPhoneId() + "]", s);
    }
}
