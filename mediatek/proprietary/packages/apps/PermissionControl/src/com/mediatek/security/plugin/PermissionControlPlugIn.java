package com.mediatek.security.plugin;


import android.provider.SearchIndexableData;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;

import com.mediatek.common.mom.MobileManagerUtils;
import com.mediatek.common.PluginImpl;
import com.mediatek.security.R;
import com.mediatek.security.service.PermControlUtils;
import com.mediatek.settings.ext.DefaultPermissionControlExt;


import java.util.ArrayList;
import java.util.List;

@PluginImpl(interfaceName="com.mediatek.settings.ext.IPermissionControlExt")
public class PermissionControlPlugIn extends DefaultPermissionControlExt {
    private static final String TAG = "PermControlPlugIn";

    private CustomizedSwitchPreference mSwitchPrf;
    private PermissionControlEnabler mEnabler;

    private Preference mAutoBootPrf;

    private Context mContext;

    public PermissionControlPlugIn(Context context) {
        super(context);

        mContext = context;
        mContext.setTheme(com.android.internal.R.style.Theme_Material_Settings);

        mSwitchPrf = new CustomizedSwitchPreference(mContext);
        mSwitchPrf.setTitle(R.string.manage_permission_app_label);
        mSwitchPrf.setSummary(R.string.manage_permission_summary);
        Intent intent = new Intent();
        intent.setAction("com.mediatek.security.PERMISSION_CONTROL");
        /*
         * must add the flag , or will have the exception: Calling
         * startActivity() from outside of an Activity context requires the
         * FLAG_ACTIVITY_NEW_TASK flag.
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mSwitchPrf.setIntent(intent);
        mEnabler = new PermissionControlEnabler(mContext, mSwitchPrf);

        /// new auto boot preference instance
        mAutoBootPrf = new Preference(mContext);
        mAutoBootPrf.setTitle(R.string.auto_boot_pref_title);
        mAutoBootPrf.setSummary(R.string.auto_boot_pref_summary);
        Intent intent2 = new Intent();
        intent2.setAction("com.mediatek.security.AUTO_BOOT");
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mAutoBootPrf.setIntent(intent2);
    }

    public void addPermSwitchPrf(PreferenceGroup prefGroup) {
        if (!MobileManagerUtils.isSupported()) {
            return ;
        }
        if (prefGroup instanceof PreferenceGroup) {
            prefGroup.addPreference(mSwitchPrf);
            mSwitchPrf.setEnabled(PermControlUtils.isInHouseEnabled(mContext));
        }
    }

    public void addAutoBootPrf(PreferenceGroup prefGroup) {
        if (!MobileManagerUtils.isSupported()) {
            return ;
        }
        if (prefGroup instanceof PreferenceGroup) {
            prefGroup.addPreference(mAutoBootPrf);
        }
    }

    public void enablerResume() {
        if (mEnabler != null) {
            mEnabler.resume();
        }
    }

    public void enablerPause() {
        if (mEnabler != null) {
            mEnabler.pause();
        }
    }

    public List<SearchIndexableData> getRawDataToIndex(boolean enabled) {
        List<SearchIndexableData> indexables = new ArrayList<SearchIndexableData>();

        Log.d(TAG, "loading search info");
        SearchIndexableRaw indexable = new SearchIndexableRaw(mContext);
        indexable.title =  mContext.getString(R.string.manage_permission_app_label);
        indexable.intentAction = "com.mediatek.security.PERMISSION_CONTROL";
        indexables.add((SearchIndexableData) indexable);
        return indexables;
    }
}
