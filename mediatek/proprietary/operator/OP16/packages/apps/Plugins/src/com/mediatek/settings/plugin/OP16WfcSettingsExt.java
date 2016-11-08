package com.mediatek.settings.plugin;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;

import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.ims.ImsManager;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

import com.mediatek.common.PluginImpl;
import com.mediatek.ims.WfcReasonInfo;
import com.mediatek.op16.plugin.R;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.wfc.plugin.WfcSettings;

/**
 * Plugin implementation for WFC Settings plugin
 */

@PluginImpl(interfaceName = "com.mediatek.settings.ext.IWfcSettingsExt")
public class OP16WfcSettingsExt extends DefaultWfcSettingsExt {

    private static final String TAG = "OP16WfcSettingsExt";
    private static final String CALL_STATE = "call_state";
    private static final String PHONE_TYPE = "phone_type";

    private Context mContext;
    private Context mAppContext;
    private SwitchPreference mWfcSwitch = null;
    private WfcSettings  mWfcSettings = null;
    ImsManager mImsManager;
    private SwitchBar mHotspotSwitchBar;

    /** Constructor.
     * @param context context
     */
    public OP16WfcSettingsExt(Context context) {
        super();
        mContext = context;
    }

    @Override
     /** Show tethering alert dialog.
     * @param context context
     * @return boolean
     */
    public boolean showWfcTetheringAlertDialog(Context context) {
        // Show alert only if WFC is registered
        mImsManager = ImsManager.getInstance(context, SubscriptionManager.getDefaultVoiceSubId());
        Log.d(TAG, "wfc status:" + mImsManager.getWfcStatusCode());
        if (mImsManager.getWfcStatusCode() != WfcReasonInfo.CODE_WFC_SUCCESS) {
            return false;
        }

        mHotspotSwitchBar = ((SettingsActivity) context).getSwitchBar();
        if (mHotspotSwitchBar != null) {
            mHotspotSwitchBar.setChecked(false);
        }

        new AlertDialog.Builder(context)
            .setCancelable(true)
            .setTitle(mContext.getText(R.string.wfc_wifi_hotspot_alert_title))
            .setMessage(mContext.getText(R.string.wfc_wifi_hotspot_alert_message))
            .setPositiveButton(mContext.getText(android.R.string.ok), new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.d(TAG, "Enabling hotspot");
                    // Turn on Wifi Hotspot
                    final ContentResolver cr = mContext.getContentResolver();

                    // Disable Wifi if enabling tethering
                    WifiManager wifiManager = (WifiManager) mContext
                            .getSystemService(Context.WIFI_SERVICE);
                    int wifiState = wifiManager.getWifiState();
                    if ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                            (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
                        wifiManager.setWifiEnabled(false);
                        android.provider.Settings.Global.putInt(cr, android.provider.Settings
                                .Global.WIFI_SAVED_STATE, 1);
                    }

                    if (wifiManager.setWifiApEnabled(null, true)) {
                        mHotspotSwitchBar.setEnabled(false);
                    } else {
                        mHotspotSwitchBar.setEnabled(true);
                    }
                }
            })
            .setNegativeButton(mContext.getText(android.R.string.cancel), new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            })
            .show();
        Log.d(TAG, "Alert shown");
        return true;
    }

    @Override
    /**
     * @param context Context
     * @param preferenceScreen preferenceScreen
     * @return
     * Customize wfc preference
     */
    public void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen) {
        mAppContext = context;
        mWfcSettings = WfcSettings.getInstance(context);
        mWfcSettings.customizedWfcPreference(context, preferenceScreen);
    }
}

