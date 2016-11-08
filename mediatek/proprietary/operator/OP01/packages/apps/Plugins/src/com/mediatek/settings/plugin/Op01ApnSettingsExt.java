package com.mediatek.settings.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.settings.ext.DefaultApnSettingsExt;


/**
 * APN info plugin.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IApnSettingsExt")
public class Op01ApnSettingsExt extends DefaultApnSettingsExt {

    private static final String TAG = "OP01ApnSettingsExt";
    private Context mContext;

    /**
     * Init context.
     * @param context The Context
     */
    public Op01ApnSettingsExt(Context context) {
        super();
        mContext = context;
    }

    @Override
    public String[] getApnTypeArray(String[] defaultApnArray, Context context, String apnType) {
        Log.d("@M_" + TAG, "getApnTypeArray : cmcc array");
        return mContext.getResources().getStringArray(R.array.apn_type_cmcc);
    }
}

