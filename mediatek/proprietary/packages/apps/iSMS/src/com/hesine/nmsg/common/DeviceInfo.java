package com.hesine.nmsg.common;

import java.util.Locale;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class DeviceInfo {
    public static final String INVALID_CODE = "000000000000000";

    public static String getIMEI(Context context) {
        String imei = "";

        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (mTelephonyMgr != null) {
            imei = mTelephonyMgr.getDeviceId();
        }

        imei = imei + "";
        MLog.info("imei: " + imei);
        return imei;
    }

    public static String getIMSI(Context context) {
        String imsi = "";

        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (mTelephonyMgr != null) {
            imsi = mTelephonyMgr.getSubscriberId();
        }

        if (TextUtils.isEmpty(imsi)) {
            imsi = INVALID_CODE;
        }
        MLog.info("imsi: " + imsi);
        return imsi;
    }

    public static String getPhonenum(Context context) {
        String number = "";

        TelephonyManager mTelephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (mTelephonyMgr != null) {
            number = mTelephonyMgr.getLine1Number();
        }

        MLog.info("number: " + number);
        return number;
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    public static String getSystemVersion() {
        return Build.VERSION.RELEASE + "";
    }

    public static String getLanuage(Context context) {
        return Locale.getDefault().getLanguage();
    }

    public static boolean isNetworkReady(Context context) {

        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null == connManager) {
            MLog.error("connectivity manager is null when checking active network");
            return false;
        }

        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info == null) {
            MLog.error("no active network when checking active network");
            return false;
        }

        if (!info.isConnected()) {
            MLog.error("current network is not connected when checking active network");
            return false;
        }

        if (!info.isAvailable()) {
            MLog.error("current network is not available when checking active network");
            return false;
        }
        return true;
    }

    public static boolean isWifiNetwork(Context context) {

        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null == connManager) {
            MLog.error("connectivity manager is null when checking active network");
            return false;
        }

        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info == null) {
            MLog.error("no active network when checking active network");
            return false;
        }

        if (!info.isConnected()) {
            MLog.error("current network is not connected when checking active network");
            return false;
        }

        if (!info.isAvailable()) {
            MLog.error("current network is not available when checking active network");
            return false;
        }

        if (ConnectivityManager.TYPE_WIFI == info.getType()) {
            return true;
        }
        return false;
    }

}
