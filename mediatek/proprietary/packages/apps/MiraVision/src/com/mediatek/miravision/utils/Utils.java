package com.mediatek.miravision.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Utils {

    private static final String SHARED_PREFERENCES_NAME = "miravision_status";

    private Context mContext;

    public Utils(Context context) {
        mContext = context;
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public boolean getSharePrefBoolenValue(String key) {
        return getSharedPreferences().getBoolean(key, false);
    }

    public void setSharePrefBoolenValue(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public int getSharePrefIntValue(String key) {
        return getSharedPreferences().getInt(key, 0);
    }

    public void setSharePrefIntValue(String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

}
