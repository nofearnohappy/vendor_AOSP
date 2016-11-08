package com.mediatek.settings.plugin;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.settings.ext.DefaultStatusExt;

/**
 * For Add LTE information.
 * network type and signal strength.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IStatusExt")
public class SimStatusExtImp extends DefaultStatusExt {
    private static final String TAG = "OP09SimStatusExtImp";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private Context mContext;
    private ServiceState mState;

    /**
     * Constructor method.
     * @param context Context
     */
    public SimStatusExtImp(Context context) {
        mContext = context;
    }

    //@Override
    public void customizeSignalItem(PreferenceScreen preferenceScreen, String key,
            SignalStrength strength, int subId) {
        Log.d(TAG, "customizeSignalStrengthItem");
        int slotId = SubscriptionManager.getSlotId(subId);
        if (Utils.isCTCardType() && (PhoneConstants.SIM_ID_1 == slotId)) {
            Preference preference = preferenceScreen.findPreference(key);
            if (preference != null && isRegisterInLteNetwork(subId, mState)) {
                int lteSignalDbm = 0;
                int lteSignalAsu = 0;
                lteSignalDbm = strength.getLteDbm();
                lteSignalAsu = strength.getLteAsuLevel();
                Log.d(TAG, "updateSignalStrength, LTE SignalStrength is " + lteSignalDbm + " dbm , "
                        + lteSignalAsu + " asu");
                lteSignalDbm = (-1 == lteSignalDbm) ? 0 : lteSignalDbm;
                lteSignalAsu = (-1 == lteSignalAsu) ? 0 : lteSignalAsu;
                String summary = preference.getSummary().toString();
                preference.setSummary("CDMA " + summary + " \nLTE " + String.valueOf(lteSignalDbm)
                        + " " + mContext.getString(R.string.radioInfo_display_dbm) + " "
                        + String.valueOf(lteSignalAsu) + " "
                        + mContext.getString(R.string.radioInfo_display_asu));
            }
        }
    }

    //@Override
    public void customizeSimNetworkTypeItem(PreferenceScreen preferenceScreen, String key,
            ServiceState state, int subId) {
        mState = state;
        Preference preference = preferenceScreen.findPreference(key);
        if (preference != null && isRegisterInLteNetwork(subId, state)) {
            TelephonyManager telManager = TelephonyManager.getDefault();
            String netWorkTypeName = telManager.getNetworkTypeName(state.getVoiceNetworkType());
            netWorkTypeName = CurrentNetworkInfoStatus
                    .renameNetworkTypeNameForCTSpec(netWorkTypeName);
            String lteNetWorkTypeName = telManager.getNetworkTypeName(state.getDataNetworkType());
            Log.d(TAG, "netWorkTypeName = " + netWorkTypeName + " lteNetWorkTypeName = "
                    + lteNetWorkTypeName);
            String summary = null;
            if (TextUtils.isEmpty(netWorkTypeName) && TextUtils.isEmpty(lteNetWorkTypeName)) {
                summary = mContext.getString(R.string.device_info_default);
            } else {
                summary = netWorkTypeName + "," + lteNetWorkTypeName;
            }
            Log.d(TAG, "summary = " + summary);
            preference.setSummary(summary);
        }
    }

    private boolean isRegisterInLteNetwork(int subId, ServiceState serviceState) {
        boolean isLteNetwork = false;
        int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType(subId);
        if (activePhoneType == TelephonyManager.PHONE_TYPE_CDMA && serviceState != null
                && serviceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE
                && serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE) {
            isLteNetwork = true;
        }
        Log.d(TAG, "activePhoneType = " + activePhoneType + "\nservice state = " + serviceState
                + "\nisLteNetwork = " + isLteNetwork);
        return isLteNetwork;
    }
}
