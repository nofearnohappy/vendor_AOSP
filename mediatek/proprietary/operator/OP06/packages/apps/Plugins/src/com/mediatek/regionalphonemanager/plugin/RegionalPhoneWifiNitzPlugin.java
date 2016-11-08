package com.mediatek.regionalphonemanager.plugin;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.rpm.ext.ISettingsExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.common.regionalphone.RegionalPhone;

@PluginImpl(interfaceName="com.mediatek.rpm.ext.ISettingsExt")
public class RegionalPhoneWifiNitzPlugin implements ISettingsExt {
    private static final String TAG = "RegionalPhone";
    private Context mContext;
    private Uri mUri = RegionalPhone.SETTINGS_URI;
    private static final String NITZ_AUTOUPDATE = "NITZAutoUpdate";
    private static final String WIFI_DEFAULT = "wifi";

    public void updateConfiguration(Context context) {
        mContext = context;
        Log.d("@M_" + TAG, "RegionalPhoneWifiNitzPlugin");
        dataUpdate();
    }

    /**
     * update configuration of NITZ and Wi-Fi
     */
    private void dataUpdate() {
        Cursor mCursor = mContext.getContentResolver().query(
                mUri,
                null,
                null,
                null,
                null
                );
        if (null == mCursor) {
            Log.d("@M_" + TAG, "Cursor == null");
        } else if (mCursor.getCount() < 1) {
            Log.d("@M_" + TAG, "No data found");
        } else {
            mCursor.moveToNext();
            int NITZEnabled = mCursor.getInt(mCursor.getColumnIndex(NITZ_AUTOUPDATE));
            int wifiEnabled = mCursor.getInt(mCursor.getColumnIndex(WIFI_DEFAULT));
            setNITZEnabler(NITZEnabled);
            setWifiEnabler(wifiEnabled);
        }
    mCursor.close();
    }

    /**
     * Enable/Disable NITZ function
     * @param enabled
     * 0: Disable
     * 1: Enable
     */
    private void setNITZEnabler(int enabled) {
        Log.d("@M_" + TAG, "setNITZ, new value is: " + enabled);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME, enabled);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_GPS, 0);

    }

    /**
     * Enable/Disable Wi-Fi function
     * @param enabled
     * 0: Disable
     * 1: Enable
     */
    private void setWifiEnabler(int enabled) {
        boolean newEnabled = (enabled != 0 ? true : false);
        WifiManager mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager.setWifiEnabled(newEnabled)) {
            Log.d("@M_" + TAG, "set Wifi success, new value is: " + enabled);
        } else {
            Log.d("@M_" + TAG, "set Wifi fail");
        }
    }

}
