package com.mediatek.smartmotion.enabler;

import android.content.Context;
import android.content.SharedPreferences;


public class Preferences {
    public static final String PREFERENCES_FILE = "MediatekSmartMotion.Main";

    private static final String ENABLE_QUICK_ANSWER = "enableQuickAnswer";
    private static final String ENABLE_EASY_REJECT = "enableEasyReject";
    private static final String ENABLE_SMART_SILENT = "enableSmartSilent";
    private static final String ENABLE_IN_POCKET= "enableInPocket";
    private static final String ENABLE_PEDOMETER = "enablePedometer";
    private static final String ENABLE_USER_ACTIVITY = "enableUserActivity";
    private static final String ENABLE_SIGNIFICANT_SMD = "enableSignificantMotion";
    private static final String DEMO_MODE = "demo mode";

    private static Preferences sPreferences;
    private final SharedPreferences mSharedPreferences;

    private Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public static synchronized Preferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = new Preferences(context);
        }
        return sPreferences;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return getPreferences(context).mSharedPreferences;
    }

    public void setQuickAnswer(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_QUICK_ANSWER, value).apply();
    }

    public boolean getQuickAnswer() {
        return mSharedPreferences.getBoolean(ENABLE_QUICK_ANSWER, false);
    }

    public void setEasyReject(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_EASY_REJECT, value).apply();
    }

    public boolean getEasyReject() {
        return mSharedPreferences.getBoolean(ENABLE_EASY_REJECT, false);
    }

    public void setInPocket(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_IN_POCKET, value).apply();
    }

    public boolean getInPocket() {
        return mSharedPreferences.getBoolean(ENABLE_IN_POCKET, false);
    }


    public void setSmartSilent(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_SMART_SILENT, value).apply();
    }

    public boolean getSmartSilent() {
        return mSharedPreferences.getBoolean(ENABLE_SMART_SILENT, false);
    }

    public void setPedometer(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_PEDOMETER, value).apply();
    }

    public boolean getPedometer() {
        return mSharedPreferences.getBoolean(ENABLE_PEDOMETER, false);
    }

    public void setUserActivity(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_USER_ACTIVITY, value).apply();
    }

    public boolean getUserActivity() {
        return mSharedPreferences.getBoolean(ENABLE_USER_ACTIVITY, false);
    }

    public void setSignificantMotion(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_SIGNIFICANT_SMD, value).apply();
    }

    public boolean getSignificantMotion() {
        return mSharedPreferences.getBoolean(ENABLE_SIGNIFICANT_SMD, false);
    }

    public void setDemoMode(boolean value) {
        mSharedPreferences.edit().putBoolean(DEMO_MODE, value).apply();
    }

    public boolean getDemoMode() {
        return mSharedPreferences.getBoolean(DEMO_MODE, true);
    }
}
