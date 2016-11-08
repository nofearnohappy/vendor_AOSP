package com.hesine.nmsg.business.dao;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.common.EnumConstants;

public class Config {

    public Config() {
    }

    public Config(String prefsName) {
    }

    protected static SharedPreferences getSharedPrefs() {
        return Application.getInstance().getSharedPreferences(EnumConstants.SHARED_PREFERENCE_NAME,
                0);
    }

    private String isActivatedKey = "isActivated";
    private String uuidKey = "uuid";
    private String deactiveUuidKey = "deactivateUuid";
    private String pnTokenKey = "pnToken";
    private String uploadPNTokenFlagKey = "uploadPNTokenFlag";
    private String lastUpgradeTimeKey = "lastUpgradeTime";
    private String newVersionKey = "newVersion";
    private String newClientUrlKey = "newClientUrl";
    private String imsiKey = "imsi";
    private String latestWifiMsgIdsKey = "latestWifiMsgIds";
    private String isWifiCheckedKey = "isWifiChecked";
    private String wifiConnectedKey = "wifiConnected";
    private String ipKey = "ip";
    private String ipDeactivationKey = "deactivationIp";
    private String redirectIpKey = "redirectIp";
    private String locationKey = "location";
    private String uploadLocFlagKey = "uploadLocFlag";

    public static Config config;

    public static Config getInstance() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    public static void saveIsActivated(boolean activate) {
        Config conf = Config.getInstance();
        saveBoolean(conf.isActivatedKey, activate);
    }

    public static boolean getIsActivated() {
        Config conf = Config.getInstance();
        return getBoolean(conf.isActivatedKey, false);
    }

    public static void saveUuid(String uuid) {
        Config conf = Config.getInstance();
        saveString(conf.uuidKey, uuid);
    }

    public static String getUuid() {
        Config conf = Config.getInstance();
        return getString(conf.uuidKey, null);
    }

    public static void saveDeactiveUuid(String uuid) {
        Config conf = Config.getInstance();
        saveString(conf.deactiveUuidKey, uuid);
    }

    public static String getDeactiveUuid() {
        Config conf = Config.getInstance();
        return getString(conf.deactiveUuidKey, null);
    }

    public static void savePnToken(String pnToken) {
        Config conf = Config.getInstance();
        saveString(conf.pnTokenKey, pnToken);
    }

    public static String getPnToken() {
        Config conf = Config.getInstance();
        return getString(conf.pnTokenKey, null);
    }

    public static void saveUploadPNTokenFlag(boolean uploadPNTokenFlag) {
        Config conf = Config.getInstance();
        saveBoolean(conf.uploadPNTokenFlagKey, uploadPNTokenFlag);
    }

    public static boolean getUploadPNTokenFlag() {
        Config conf = Config.getInstance();
        return getBoolean(conf.uploadPNTokenFlagKey, false);
    }

    public static void saveLastUpgradeTime(long time) {
        Config conf = Config.getInstance();
        saveLong(conf.lastUpgradeTimeKey, time);
    }

    public static long getLastUpgradeTime() {
        Config conf = Config.getInstance();
        return getLong(conf.lastUpgradeTimeKey, 0);
    }

    public static void saveNewVersion(String version) {
        Config conf = Config.getInstance();
        saveString(conf.newVersionKey, version);
    }

    public static String getNewVersion() {
        Config conf = Config.getInstance();
        return getString(conf.newVersionKey, null);
    }

    public static void saveNewClientUrl(String url) {
        Config conf = Config.getInstance();
        saveString(conf.newClientUrlKey, url);
    }

    public static String getNewClientUrl() {
        Config conf = Config.getInstance();
        return getString(conf.newClientUrlKey, null);
    }

    public static void saveImsi(String imsi) {
        Config conf = Config.getInstance();
        saveString(conf.imsiKey, imsi);
    }

    public static String getImsi() {
        Config conf = Config.getInstance();
        return getString(conf.imsiKey, null);
    }

    public static void saveLatestWifiMsgIds(String latestWifiMsgIds) {
        Config conf = Config.getInstance();
        saveString(conf.latestWifiMsgIdsKey, latestWifiMsgIds);
    }

    public static String getLatestWifiMsgIds() {
        Config conf = Config.getInstance();
        return getString(conf.latestWifiMsgIdsKey, null);
    }

    public static void saveIsWifiChecked(boolean saveIsWifiChecked) {
        Config conf = Config.getInstance();
        saveBoolean(conf.isWifiCheckedKey, saveIsWifiChecked);
    }

    public static boolean getIsWifiChecked() {
        Config conf = Config.getInstance();
        return getBoolean(conf.isWifiCheckedKey, false);
    }

    public static void saveWifiConnected(boolean wifiConnected) {
        Config conf = Config.getInstance();
        saveBoolean(conf.wifiConnectedKey, wifiConnected);
    }

    public static boolean getWifiConnected() {
        Config conf = Config.getInstance();
        return getBoolean(conf.wifiConnectedKey, false);
    }

    public static void saveIp(String ip) {
        Config conf = Config.getInstance();
        saveString(conf.ipKey, ip);
    }

    public static String getIp() {
        Config conf = Config.getInstance();
        return getString(conf.ipKey, null);
    }

    public static void saveDeactivatationIp(String ip) {
        Config conf = Config.getInstance();
        saveString(conf.ipDeactivationKey, ip);
    }

    public static String getDeactivatationIp() {
        Config conf = Config.getInstance();
        return getString(conf.ipDeactivationKey, null);
    }

    public static void saveRedirectIp(String redirectIp) {
        Config conf = Config.getInstance();
        saveString(conf.redirectIpKey, redirectIp);
    }

    public static String getRedirectIp() {
        Config conf = Config.getInstance();
        return getString(conf.redirectIpKey, null);
    }

    public static void saveLocation(String location) {
        Config conf = Config.getInstance();
        saveString(conf.locationKey, location);
    }

    public static String getLocation() {
        Config conf = Config.getInstance();
        return getString(conf.locationKey, null);
    }

    public static void saveUploadLocFlag(boolean uploadLocFlag) {
        Config conf = Config.getInstance();
        saveBoolean(conf.uploadLocFlagKey, uploadLocFlag);
    }

    public static boolean getUploadLocFlag() {
        Config conf = Config.getInstance();
        return getBoolean(conf.uploadLocFlagKey, false);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences prefs = getSharedPrefs();
        return prefs.getBoolean(key, defaultValue);
    }

    public static void saveBoolean(String key, boolean value) {
        SharedPreferences prefs = getSharedPrefs();
        Editor edit = prefs.edit();
        edit.putBoolean(key, value);
        edit.commit();
    }

    public static String getString(String key, String defaultValue) {
        SharedPreferences prefs = getSharedPrefs();
        return prefs.getString(key, defaultValue);
    }

    public static void saveString(String key, String value) {
        SharedPreferences prefs = getSharedPrefs();
        Editor edit = prefs.edit();
        edit.putString(key, value);
        edit.commit();
    }

    public static long getLong(String key, long defaultValue) {
        SharedPreferences prefs = getSharedPrefs();
        return prefs.getLong(key, defaultValue);
    }

    public static void saveLong(String key, long value) {
        SharedPreferences prefs = getSharedPrefs();
        Editor edit = prefs.edit();
        edit.putLong(key, value);
        edit.commit();
    }
}

