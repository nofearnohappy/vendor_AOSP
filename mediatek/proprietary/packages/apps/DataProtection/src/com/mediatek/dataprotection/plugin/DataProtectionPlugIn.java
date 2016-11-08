package com.mediatek.dataprotection.plugin;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.dataprotection.R;
import com.mediatek.settings.ext.DefaultDataProtectionExt;

/**
 * PluginImpl is for new plugin mechanism.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IDataProtectionExt")
public class DataProtectionPlugIn extends DefaultDataProtectionExt {

    private static final String TAG = "DataProtectionPlugIn";

    private Preference mPreference;

    private Context mContext;

    public DataProtectionPlugIn(Context context) {
        super(context);
        mContext = context;
        mContext.setTheme(com.android.internal.R.style.Theme_Material_Settings);
        mPreference = new Preference(mContext);
        mPreference.setTitle(mContext.getString(R.string.app_name));
        mPreference.setSummary(mContext
                .getString(R.string.data_protection_summary));
        Intent intent = new Intent();
        intent.setAction("com.mediatek.dataprotection.ACTION_START_MAIN");
        /*
         * must add the flag , or will have the exception: Calling
         * startActivity() from outside of an Activity context requires the
         * FLAG_ACTIVITY_NEW_TASK flag.
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mPreference.setIntent(intent);
    }

    public void addDataPrf(PreferenceGroup prefGroup) {
        Log.d(TAG, "addDataPrf for dataprotection plugin");
        if (prefGroup instanceof PreferenceGroup) {
            prefGroup.addPreference(mPreference);
        }
    }
}
