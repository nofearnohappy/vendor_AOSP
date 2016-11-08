package com.mediatek.rpm.ext;

import android.content.Context;
import android.util.Log;

public class DefaultSettingsExt implements ISettingsExt {
    private static final String TAG = "RegionalPhoneExt";

    @Override
    public void updateConfiguration(Context context) {
        Log.d("@M_" + TAG, "DefaultSettingsExt; No plugin found");
    }
}