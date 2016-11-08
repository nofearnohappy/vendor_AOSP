package com.mediatek.browser.ext;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class DefaultBrowserRegionalPhoneExt implements IBrowserRegionalPhoneExt {

    private static final String TAG = "DefaultBrowserRegionalPhoneExt";

    @Override
    public String getSearchEngine(SharedPreferences pref, Context context) {
        Log.i("@M_" + TAG, "Enter: " + "updateSearchEngine" + " --default implement");
        return null;
    }

    @Override
    public void updateBookmarks(Context context) {
        Log.i("@M_" + TAG, "Enter: " + "updateBookmarks" + " --default implement");
    }

}

