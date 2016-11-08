package com.mediatek.wfc.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
//import android.view.View;
import android.widget.Toast;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.PhoneConstants;

/**
 * Customized WFC Setting implementation.
 */
public class WfcSettings implements OnPreferenceChangeListener {

    private static final String TAG = "OP16WfcSettings";
    private static final String WFC_PREFERENCE = "wfc_pref";
    //private static final String WFC_PREFERENCE_KEY = "wfc_pref_switch";
    public static final String NOTIFY_CALL_STATE = "OP16_call_state_Change";
    public static final String CALL_STATE = "call_state";
    public static final int CALL_STATE_IDLE = 1;
    public static final int CALL_STATE_CS = 2;
    public static final int CALL_STATE_PS = 3;
    static WfcSettings sWfcSettings = null;

    Context mContext;
    Context mAppContext;
    SwitchPreference mWfcSwitch = null;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (NOTIFY_CALL_STATE.equals(intent.getAction())) {
                if (mWfcSwitch != null) {
                    int callState = intent.getIntExtra(CALL_STATE, CALL_STATE_IDLE);
                     Log.v(TAG, "br call_satte: " + callState);
                    mWfcSwitch.setEnabled(callState == CALL_STATE_PS ? false : true);
                }
            }
        }
    };

    private WfcSettings(Context context) {
       mContext = context;
    }

    /** Provides instance of plugin.
     * @param context context
     * @return WfcSettings
     */
    public static WfcSettings getInstance(Context context) {

        if (sWfcSettings == null) {
            sWfcSettings = new WfcSettings(context);
        }
        return sWfcSettings;
    }

    /** Customize wfc setting.
     * @param context context
     * @param preferenceScreen preferenceScreen
     * @return
     */
    public void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen) {
        mAppContext = context;
        Preference wfcSettingsPreference = (Preference) preferenceScreen
                .findPreference(WFC_PREFERENCE);

        if (wfcSettingsPreference != null) {
            preferenceScreen.removePreference(wfcSettingsPreference);
            mWfcSwitch = new SwitchPreference(context);
            mWfcSwitch.setKey(WFC_PREFERENCE); //WFC_PREFERENCE_KEY);
            mWfcSwitch.setTitle(wfcSettingsPreference.getTitle());
            mWfcSwitch.setOrder(wfcSettingsPreference.getOrder());
            mWfcSwitch.setOnPreferenceChangeListener(this);
            // Disable switch if PS call ongoing
            Log.d(TAG, "call_state: " + Settings.System.getInt(context.getContentResolver(),
                            CALL_STATE, CALL_STATE_IDLE));
            mWfcSwitch.setEnabled(Settings.System.getInt(context.getContentResolver(),
                            CALL_STATE, CALL_STATE_IDLE) == CALL_STATE_PS ? false : true);
            preferenceScreen.addPreference(mWfcSwitch);
       }
       if (mWfcSwitch != null) {
        mWfcSwitch.setChecked(ImsManager.isWfcEnabledByUser(mAppContext));
      }
    }



    private boolean isInSwitchProcess() {
        int imsState = PhoneConstants.IMS_STATE_DISABLED;
        try {
         imsState = ImsManager.getInstance(mAppContext, SubscriptionManager
                .getDefaultVoicePhoneId()).getImsState();
        } catch (ImsException e) {
           return false;
        }
        Log.d(TAG, "isInSwitchProcess , imsState = " + imsState);
        return imsState == PhoneConstants.IMS_STATE_DISABLING
                || imsState == PhoneConstants.IMS_STATE_ENABLING;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isChecked = !mWfcSwitch.isChecked();

        if (isInSwitchProcess()) {
            Toast.makeText(mContext, "Operation not allowed", Toast.LENGTH_SHORT)
                .show();
            return false;
        }

        ImsManager.setWfcSetting(mAppContext, isChecked);
        return true;
    }
}
