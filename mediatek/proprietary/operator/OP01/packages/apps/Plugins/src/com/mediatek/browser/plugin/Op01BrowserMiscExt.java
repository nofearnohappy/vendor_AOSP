package com.mediatek.browser.plugin;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.WebView;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.browser.ext.DefaultBrowserMiscExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserMiscExt")
public class Op01BrowserMiscExt extends DefaultBrowserMiscExt {

    private static final String TAG = "Op01BrowserMiscExt";

    private static final String ACTION_DATA_CONNECTION = "com.mediatek.browser.Op01BrowserDataConnectionDialog";

    public static final String FILEMANAGER_EXTRA_NAME = "download path";

    public static final int RESULT_CODE_START_FILEMANAGER = 1000;
    private static final int RESULT_CODE_WLAN_PREFER = 2000;

    private static boolean sOp01NoNetworkShouldNotify = true;

    public void processNetworkNotify(WebView view, Activity activity, boolean isNetworkUp) {
        Log.i("@M_" + TAG, "Enter: " + "handleOp01NoNetworkNotify" + " --OP01 implement");

        if (!sOp01NoNetworkShouldNotify) {
            if (!isNetworkUp) {
                view.setNetworkAvailable(false);
            }
            return;
        }

        ConnectivityManager conMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiMgr = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        Log.d("@M_" + TAG, "wifiMgr.isWifiEnabled(): " + wifiMgr.isWifiEnabled());
        if (wifiMgr.isWifiEnabled()) {
            if (conMgr.getActiveNetworkInfo() == null ||
                    (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().getType()
                            != ConnectivityManager.TYPE_WIFI)) {
                List<ScanResult> list = wifiMgr.getScanResults();
                if (list != null && list.size() == 0) {
                    Log.d("@M_" + TAG, "handleOp01NoNetworkNotify(): For OP01 wlan test case 7.3.4.");
                    SharedPreferences sh = activity.getSharedPreferences("data_connection", Activity.MODE_WORLD_READABLE);
                    boolean needShow = !sh.getBoolean("pref_not_remind", false);
                    boolean airplane = System.getInt(activity.getContentResolver(),
                        System.AIRPLANE_MODE_ON, 0) != 0;
                    boolean hasSim = false;
                    if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
                        TelephonyManagerEx teleMgrEx = TelephonyManagerEx.getDefault();
                        hasSim = teleMgrEx.hasIccCard(PhoneConstants.SIM_ID_1) ||
                            teleMgrEx.hasIccCard(PhoneConstants.SIM_ID_2) ||
                            teleMgrEx.hasIccCard(PhoneConstants.SIM_ID_3) ||
                            teleMgrEx.hasIccCard(PhoneConstants.SIM_ID_4);
                    } else {
                        TelephonyManager teleMgr = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                        hasSim = teleMgr.hasIccCard();
                    }
                    if (needShow && !airplane && hasSim) {
                        Intent dlg = new Intent(ACTION_DATA_CONNECTION);
                        activity.startActivityForResult(dlg, RESULT_CODE_WLAN_PREFER);
                    }
                } else {
                    if (ActivityManagerNative.isSystemReady()) {
                        Log.d("@M_" + TAG, "handleOp01NoNetworkNotify(): For OP01 wlan test case 7.3.2.");
                        Intent intent = new Intent("android.net.wifi.PICK_WIFI_NETWORK_AND_GPRS");
                        intent.putExtra("access_points_and_gprs", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } else {
                        Log.e("@M_" + TAG, "handleOp01NoNetworkNotify(): Error, ActivityManagerNative.isSystemReady return false.");
                    }
                }
                sOp01NoNetworkShouldNotify = false;
            }
        } else {
            if (!isNetworkUp) {
                view.setNetworkAvailable(false);
                Log.v("@M_" + TAG, "handleOp01NoNetworkNotify(): WiFi is not enabled.");
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data, Object obj) {
        Log.i("@M_" + TAG, "Enter: " + "onActivityResult" + " --OP01 implement");
        if (requestCode == RESULT_CODE_START_FILEMANAGER) {
            PreferenceFragment prefFrag = (PreferenceFragment) obj;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    String downloadPath = extras.getString(FILEMANAGER_EXTRA_NAME);
                    if (downloadPath != null) {
                        Preference downloadPref = prefFrag.findPreference(Op01BrowserSettingExt.PREF_DOWNLOAD_DIRECTORY_SETTING);
                        Editor ed = downloadPref.getEditor();
                        ed.putString(Op01BrowserSettingExt.PREF_DOWNLOAD_DIRECTORY_SETTING, downloadPath);
                        ed.commit();
                        downloadPref.setSummary(downloadPath);
                    }
                }
            }
        } else if (requestCode == RESULT_CODE_WLAN_PREFER) {
            Context context = (Context) obj;
            if (resultCode == Activity.RESULT_OK) {
                SharedPreferences sh = context.getSharedPreferences("data_connection", Activity.MODE_WORLD_READABLE);
                Editor ed = sh.edit();
                ed.putBoolean(Op01BrowserSettingExt.PREF_NOT_REMIND, true);
                ed.commit();
            }
        }
    }
}
