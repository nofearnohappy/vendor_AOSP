package com.mediatek.wfc.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

import com.mediatek.common.PluginImpl;
import com.mediatek.op08.plugin.R;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;


/**
 * Plugin implementation for WFC Settings plugin
 */

@PluginImpl(interfaceName = "com.mediatek.settings.ext.IWfcSettingsExt")

public class OP08WfcSettingsExt extends DefaultWfcSettingsExt {

    private static final String TAG = "OP08WfcSettingsExt";
    private static final String KEY_WFC_SETTINGS = "wifi_calling_settings";
    private static final String TUTORIALS = "Tutorials";
    private static final String TOP_QUESTIONS = "Top_questions";
    private static final String WFC_MODE = "Wfc_mode";
    private static final String AOSP_BUTTON_WFC_MODE = "wifi_calling_mode";
    private static final String TUTORIAL_ACTION = "mediatek.settings.WFC_TUTORIALS";
    private static final String QA_ACTION = "mediatek.settings.WFC_QA";
    private static final String WFC_MODE_DIALOG_ACTION = "mediatek.settings.SHOW_WFC_MODE_DIALOG";

    private Context mContext;
    private PreferenceFragment mPreferenceFragment;
    private PreferenceScreen mWfcPreferenceScreen;
    private WfcSummary mWfcSummary;
    private Preference mWfcModePreference;
    private SwitchBar mSwitchBar;

    private ContentObserver mWfcModeChangeObserver;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("@M_" + TAG, "onReceive:" + action);
            if (action.equals(WfcSummary.ACTION_WFC_SUMMARY_CHANGE)) {
                if (mWfcPreferenceScreen != null) {
                    mWfcPreferenceScreen.setSummary(intent
                            .getStringExtra(WfcSummary.EXTRA_SUMMARY));
                }
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // TODO Auto-generated method stub
            Log.d(TAG, "Phone state:" + state);
            switch(state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mSwitchBar != null) {
                        mSwitchBar.setEnabled(true);
                    }
                    /* Enable preference, only if wfc switch is ON */
                    mWfcModePreference.setEnabled(mSwitchBar.isChecked() ? true : false);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mSwitchBar != null) {
                        mSwitchBar.setEnabled(false);
                    }
                    mWfcModePreference.setEnabled(false);
                    break;
                default:
                    break;
            }
        }
    };


    /** Constructor.
     * @param context default summary res id
     */
    public OP08WfcSettingsExt(Context context) {
        super();
        mContext = context;
        mWfcSummary = new WfcSummary(context);
    }

    @Override
     /** Initialize plugin with essential values.
     * @param
     * @return
     */
    public void initPlugin(PreferenceFragment pf) {
        mPreferenceFragment = pf;
    }

    @Override
    /** get operator specific customized summary for WFC button.
     * Used in WirelessSettings
     * @param context context
     * @param defaultSummaryResId default summary res id
     * @return String
     */
    public String getWfcSummary(Context context, int defaultSummaryResId) {
        ImsManager imsManager = ImsManager.getInstance(mContext,
                SubscriptionManager.getDefaultVoiceSubId());
        return mWfcSummary.getWfcSummaryText(imsManager.getWfcStatusCode());
    }


    //@Override
     /** Called from onPause.
     * Used in WirelessSettings
     * @param event event happened
     * @return
     */
    public void onWirelessSettingsEvent(int event) {
        Log.d("@M_" + TAG, "Wireless setting event:" + event);
        switch(event) {
            case DefaultWfcSettingsExt.RESUME: {
                    PreferenceScreen wfcPrefScreen = (PreferenceScreen) mPreferenceFragment
                            .findPreference(KEY_WFC_SETTINGS);
                    Log.d("@M_" + TAG, "onResume, wfcPreferenceScreen:" + wfcPrefScreen);
                    if (wfcPrefScreen == null) {
                        return;
                    }
                    IntentFilter filter = new IntentFilter(WfcSummary.ACTION_WFC_SUMMARY_CHANGE);
                    mContext.registerReceiver(mReceiver, filter);
                    mWfcSummary.onResume();

                    ImsManager imsManager = ImsManager.getInstance(mContext,
                    SubscriptionManager.getDefaultVoiceSubId());
                    wfcPrefScreen.setSummary(mWfcSummary
                            .getWfcSummaryText(imsManager.getWfcStatusCode()));
                }
                break;

            case DefaultWfcSettingsExt.PAUSE: {
                    PreferenceScreen wfcPrefScreen = (PreferenceScreen) mPreferenceFragment
                            .findPreference(KEY_WFC_SETTINGS);
                    Log.d("@M_" + TAG, "onPause, wfcPreferenceScreen:" + wfcPrefScreen);
                    if (wfcPrefScreen == null) {
                        return;
                    }
                    mContext.unregisterReceiver(mReceiver);
                    mWfcSummary.onPause();
                }
                break;
            default:
                break;
        }
    }

    /*******************        ************************/

    /***********    Plugin for  WifiCallingSettings     *******/

    //@Override
    /** Called from onPause/onResume.
     * @param event event happened
     * @return
     */
    public void onWfcSettingsEvent(int event) {
        Log.d("@M_" + TAG, "WfcSeting event:" + event);
        switch(event) {
            case DefaultWfcSettingsExt.RESUME:
                SettingsActivity settingsActivity = (SettingsActivity) mPreferenceFragment
                        .getActivity();
                mSwitchBar = settingsActivity.getSwitchBar();
                ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE))
                        .listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                mContext.getContentResolver().registerContentObserver(android.provider
                        .Settings.Global.getUriFor(android.provider
                                .Settings.Global.WFC_IMS_MODE),
                        false, mWfcModeChangeObserver);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE))
                        .listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                mContext.getContentResolver().unregisterContentObserver(mWfcModeChangeObserver);
                break;
            default:
                break;
        }
    }

    //@Override
    /** Add WFC tutorial prefernce, if any.
     * @param
     * @return
     */
    public void addOtherCustomPreference() {
        PreferenceScreen ps = mPreferenceFragment.getPreferenceScreen();
        /* remove AOSP wfc mode preference */
        ps.removePreference(mPreferenceFragment.findPreference(AOSP_BUTTON_WFC_MODE));

        /* Add cutomized wfc mode preference */
        mWfcModePreference = new Preference(ps.getContext());
        mWfcModePreference.setKey(WFC_MODE);
        mWfcModePreference.setTitle(mContext.getText(R.string.wfc_mode_preference_title));
        mWfcModePreference.setSummary(WfcUtils.getWfcModeSummary(ImsManager.getWfcMode(mContext)));
        mWfcModePreference.setIntent(new Intent(WFC_MODE_DIALOG_ACTION));
        ps.addPreference(mWfcModePreference);
        /* Register content observer on Wfc Mode to change summary of this pref on mode change */
        registerForWfcModeChange(new Handler());

        Preference wfcTutorialPreference = new Preference(ps.getContext());
        wfcTutorialPreference.setKey(TUTORIALS);
        wfcTutorialPreference.setTitle(mContext.getText(R.string.Tutorials));
        wfcTutorialPreference.setIntent(new Intent(TUTORIAL_ACTION));
        ps.addPreference(wfcTutorialPreference);

        Preference wfcQAPreference = new Preference(ps.getContext());
        wfcQAPreference.setKey(TOP_QUESTIONS);
        wfcQAPreference.setTitle(mContext.getText(R.string.Top_questions));
        wfcQAPreference.setIntent(new Intent(QA_ACTION));
        ps.addPreference(wfcQAPreference);
    }

    @Override
    /** Takes operator specific action on wfc list preference on switch change:
     * On Switch OFF, disable wfcModePref.
     * @param root
     * @param wfcModePref
     * @return
     */
    public void updateWfcModePreference(PreferenceScreen root, ListPreference wfcModePref,
            boolean wfcEnabled, int wfcMode) {
        Log.d("@M_" + TAG, "wfc_enabled:" + wfcEnabled + " wfcMode:" + wfcMode);
        mWfcModePreference.setSummary(WfcUtils.getWfcModeSummary(wfcMode));
        mWfcModePreference.setEnabled(wfcEnabled);
        updateRadioStatus(wfcEnabled, wfcMode);
    }

    /** Takes operator specific action on device's radio condition:
     * If wfcmode is Wifi-only & wfc enabled, switch off radio.
     * @param wfcEnabled wfcEnabled
     * @param wfcMode wfcMode
     */
    public void updateRadioStatus(boolean wfcEnabled, int wfcMode) {
        /* Turn OFF radio power, on turning WFC ON, if preference selected is WIFI_ONLY */
        if (wfcEnabled && ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY == wfcMode) {
            Log.d("@M_" + TAG, "Turn OFF radio, as wfc is getting ON & mode is wifi_only");
            WfcUtils.sendWifiOnlyModeIntent(mContext, true);
        } else if (!wfcEnabled && ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY == wfcMode) {
            /* Turn ON radio power, on turning WFC OFF, if preference selected is WIFI_ONLY */
            Log.d("@M_" + TAG, "Turn ON radio, as wfc is getting OFF & pref was wifi_only");
            WfcUtils.sendWifiOnlyModeIntent(mContext, false);
        }
    }

    /*
    * Observes WFC mode changes to change summary of preference.
    */
    private void registerForWfcModeChange(Handler handler) {
        mWfcModeChangeObserver = new ContentObserver(handler) {

            @Override
            public void onChange(boolean selfChange) {
                this.onChange(selfChange,
                        android.provider.Settings.Global
                        .getUriFor(android.provider.Settings.Global.WFC_IMS_MODE));
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Uri i = android.provider.Settings.Global
                        .getUriFor(android.provider.Settings.Global.WFC_IMS_MODE);
                Log.d("@M_" + TAG, "wfc mode:" + ImsManager.getWfcMode(mContext));
                if (i != null && i.equals(uri)) {
                    /* set summary */
                    mWfcModePreference.setSummary(WfcUtils
                            .getWfcModeSummary(ImsManager.getWfcMode(mContext)));
                }
            }
        };
        mContext.getContentResolver().registerContentObserver(
                android.provider.Settings.Global.getUriFor(android.provider
                        .Settings.Global.WFC_IMS_MODE),
                false, mWfcModeChangeObserver);
    }
    /***************************        *********************/
}

