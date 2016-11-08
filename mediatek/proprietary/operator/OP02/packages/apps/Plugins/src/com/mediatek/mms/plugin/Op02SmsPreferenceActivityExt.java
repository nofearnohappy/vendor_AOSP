package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

import com.mediatek.mms.ext.DefaultOpSmsPreferenceActivityExt;

public class Op02SmsPreferenceActivityExt extends DefaultOpSmsPreferenceActivityExt {

    private PreferenceActivity mActivity;
    private PreferenceCategory mSmsCategory;
    private Preference mManageSimPref;

//    public Op02SmsPreferenceActivityExt(Context context) {
//        super(context);
//    }

    @Override
    public void onCreate(PreferenceActivity activity) {
        mActivity = activity;
    }

    @Override
    public void setMessagePreferences(Activity hostActivity, PreferenceCategory pC, int simCount) {
        init();
        if (simCount <= 1 && mManageSimPref != null) {
            mSmsCategory.removePreference(mManageSimPref);
        }
    }

    @Override
    public void changeSingleCardKeyToSimRelated() {
        init();
        if (mManageSimPref != null) {
            mSmsCategory.removePreference(mManageSimPref);
        }
    }

    @Override
    public void setMultiCardPreference() {
        init();
        Preference manageSimPrefMultiSim = mActivity.findPreference(SMS_MANAGE_SIM_MESSAGES);
        if (manageSimPrefMultiSim != null) {
            mSmsCategory.removePreference(manageSimPrefMultiSim);
        }
    }

    private void init() {
        android.util.Log.d("Op02SmsPreferenceActivityExt", "init");
        mSmsCategory = (PreferenceCategory) mActivity.findPreference(SMS_SETTINGS);
        mManageSimPref = mActivity.findPreference(SMS_MANAGE_SIM_MESSAGES);
    }
}
