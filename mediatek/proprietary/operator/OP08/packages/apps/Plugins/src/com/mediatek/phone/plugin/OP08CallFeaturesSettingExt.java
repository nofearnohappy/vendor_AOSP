package com.mediatek.phone.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.ims.ImsManager;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;
import com.mediatek.wfc.plugin.WfcSummary;


/**
 * Plugin implementation for CallfeatureSettings.
 */
@PluginImpl(interfaceName="com.mediatek.phone.ext.ICallFeaturesSettingExt")
public class OP08CallFeaturesSettingExt extends DefaultCallFeaturesSettingExt {
    private static final String TAG = "OP08CallFeaturesSettingExt";
    private static final String SETTINGS_CHANGED_OR_SS_COMPLETE
            = "com.mediatek.op.telephony.SETTINGS_CHANGED_OR_SS_COMPLETE";
    private static final int MESSAGE_SET_SS = 1;

    private Context mContext;
    private Preference mWfcPreference;
    private WfcSummary mWfcSummary;
    private PreferenceActivity mPreferenceActivity;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("@M_" + TAG, "onReceive:" + action);
            if (action.equals(WfcSummary.ACTION_WFC_SUMMARY_CHANGE)) {
                if (mWfcPreference != null) {
                    mWfcPreference.setSummary(intent.getStringExtra(WfcSummary.EXTRA_SUMMARY));
                }
            }
        }
    };

    /** Constructor.
     * @param context context
     */
    public OP08CallFeaturesSettingExt(Context context) {
        super();
        mContext = context;
        mWfcSummary = new WfcSummary(context);
    }

    @Override
    /**
    * Sends to intent to start re registration
    * @param context Context.
    * @param msg Message argument2 if is SET or GET/
    */
    public void resetImsPdnOverSSComplete(Context context, int msg) {
        Log.d(TAG, "resetImsPdnOverSSComplete");
        if (msg == MESSAGE_SET_SS) {
            Intent intent = new Intent(SETTINGS_CHANGED_OR_SS_COMPLETE);
            context.sendBroadcast(intent);
            Log.d(TAG, "Intent Broadcast " + SETTINGS_CHANGED_OR_SS_COMPLETE);
        }
    }

    @Override
    public void initPlugin(PreferenceActivity pa, Preference wfcPreference) {
        mPreferenceActivity = pa;
        if (ImsManager.isWfcEnabledByPlatform(mContext)) {
            mWfcPreference = wfcPreference;
        }
    }

    @Override
    /** Called from onPause
     * @param activity
     * @return
     */
    public void onCallFeatureSettingsEvent(int event) {
        Log.d("@M_" + TAG, "event:" + event);
        Log.d("@M_" + TAG, "wfcPreference:" + mWfcPreference);
        switch(event) {
            case DefaultCallFeaturesSettingExt.RESUME:
                if (ImsManager.isWfcEnabledByPlatform(mContext)) {
                    if (mWfcPreference == null) {
                        return;
                    }
                    IntentFilter filter = new IntentFilter(WfcSummary.ACTION_WFC_SUMMARY_CHANGE);
                    mContext.registerReceiver(mReceiver, filter);
                    mWfcSummary.onResume();

                    ImsManager imsManager = ImsManager.getInstance(mContext,
                            SubscriptionManager.getDefaultVoiceSubId());
                    mWfcPreference.setSummary(mWfcSummary
                            .getWfcSummaryText(imsManager.getWfcStatusCode()));
                }
                break;
            case DefaultCallFeaturesSettingExt.DESTROY:
                if (ImsManager.isWfcEnabledByPlatform(mContext)) {
                    if (mWfcPreference == null) {
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

    @Override
    /** get operator specific customized summary for WFC button.
     * Used in CallFeatureSettings
     * @param defaultSummaryResId default summary res id
     * @return summary string to be displayed
     */
    public String getWfcSummary(Context context, int defaultSummaryResId) {
        if (!ImsManager.isWfcEnabledByPlatform(context)) {
            return context.getResources().getString(defaultSummaryResId);
        }
        ImsManager imsManager = ImsManager.getInstance(mContext,
                SubscriptionManager.getDefaultVoiceSubId());
        return mWfcSummary.getWfcSummaryText(imsManager.getWfcStatusCode());
    }
}
