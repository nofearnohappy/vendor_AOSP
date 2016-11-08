package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings.System;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.settings.ext.DefaultMdmPermControlExt;

/**
 * OP01 plugin implementation of MediatekDM feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IMdmPermissionControlExt")
public class CMCCMdmPermControlExt extends DefaultMdmPermControlExt {
    private static final String TAG = "CMCCMdmPermControlExt";
    private static final int DM_BOOT_START_ENABLE_FALG = 1;
    private static final int DM_BOOT_START_DISABLE_FALG = 0;
    private static final int DEF_DM_BOOT_START_ENABLE_VALUE = 1;
    private CheckBoxPreference mPreference;
    private final Context mContext;
    public CMCCMdmPermControlExt(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        Log.i("@M_" + TAG, "CMCCMdmPermControlExt...");
        mContext = context;
        mContext.setTheme(com.android.internal.R.style.Theme_Material_Settings);
        //initial CheckBoxPreference
        mPreference = new CheckBoxPreference(mContext);
        mPreference.setEnabled(true);
        mPreference.setTitle(R.string.cmcc_dm_settings_title);
        mPreference.setChecked(getMdmPermCtrlSwitchFromDB());
        //initial summary
        if (getMdmPermCtrlSwitchFromDB()) {
            mPreference.setSummary(R.string.cmcc_dm_settings_enable_summary);
        } else {
            mPreference.setSummary(R.string.cmcc_dm_settings_disable_summary);
        }
        //add check listener
        mPreference.setOnPreferenceChangeListener(mPreferenceChangeListener);
    }
    public void addMdmPermCtrlPrf(PreferenceGroup prefGroup) {
        Log.i("@M_" + TAG, "addMdmPermCtrlPrf.");
        boolean mdmOption = SystemProperties.get("ro.mtk_mdm_app").equals("1");
        Log.i("@M_" + TAG, "mdmOption: " + mdmOption);
        if (mdmOption) {
            if (prefGroup instanceof PreferenceGroup) {
                prefGroup.addPreference(mPreference);
            }
        }
    }

    private OnPreferenceChangeListener mPreferenceChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean checked = ((Boolean) newValue).booleanValue();
            Log.i("@M_" + TAG, "CMCCMdmPermControlExt checked :" + checked);
            if (checked) {
                mPreference.setSummary(R.string.cmcc_dm_settings_enable_summary);
                updateMdmPermCtrlSwitchToDB(DM_BOOT_START_ENABLE_FALG);
            } else {
                mPreference.setSummary(R.string.cmcc_dm_settings_disable_summary);
                updateMdmPermCtrlSwitchToDB(DM_BOOT_START_DISABLE_FALG);
            }
            getMdmPermCtrlSwitchFromDB();
            return true;
        }
    };

    private boolean getMdmPermCtrlSwitchFromDB() {
        int value = System.getInt(mContext.getContentResolver(), System.DM_BOOT_START_ENABLE_KEY, DEF_DM_BOOT_START_ENABLE_VALUE);
        Log.i("@M_" + TAG, "Read from DB System.DM_BOOT_START_ENABLE_KEY is:" + value);
        if (value == 1) {
            return true;
        } else {
            return false;
        }
    }

    private void updateMdmPermCtrlSwitchToDB(int value) {
        Log.i("@M_" + TAG, "Put DB System.DM_BOOT_START_ENABLE_KEY is:" + value);
        boolean isSuccess = false;
        if (value == 1) {
            isSuccess =  System.putInt(mContext.getContentResolver(), System.DM_BOOT_START_ENABLE_KEY, DM_BOOT_START_ENABLE_FALG);
            mPreference.setSummary(R.string.cmcc_dm_settings_enable_summary);
        } else {
            isSuccess =  System.putInt(mContext.getContentResolver(), System.DM_BOOT_START_ENABLE_KEY, DM_BOOT_START_DISABLE_FALG);
            mPreference.setSummary(R.string.cmcc_dm_settings_disable_summary);
        }
        Log.i("@M_" + TAG, "Put DB System.DM_BOOT_START_ENABLE_KEY isSuccess:" + isSuccess);
    }
}
