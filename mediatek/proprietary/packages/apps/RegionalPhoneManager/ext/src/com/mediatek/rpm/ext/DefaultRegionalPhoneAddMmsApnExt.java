package com.mediatek.rpm.ext;

import android.content.Context;
import android.util.Log;

public class DefaultRegionalPhoneAddMmsApnExt implements IRegionalPhoneAddMmsApnExt {
    private static final String TAG = "RegionalPhoneExt";

    @Override
    public boolean addMmsApn(Context context) {
        Log.d("@M_" + TAG, "DefaultRegionalPhoneAddMmsApnExt; No plugin found");
        return false;
    }
}