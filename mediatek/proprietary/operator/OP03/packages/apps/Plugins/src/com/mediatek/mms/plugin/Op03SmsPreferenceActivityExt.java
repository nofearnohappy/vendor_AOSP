package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.mediatek.mms.ext.DefaultOpSmsPreferenceActivityExt;

public class Op03SmsPreferenceActivityExt
        extends DefaultOpSmsPreferenceActivityExt
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "Op03SmsPreferenceActivityExt";
    private ListPreference mSmsInputMode = null;
    private PreferenceActivity mActivity;
    private static final String SMS_INPUT_MODE = "pref_key_sms_input_mode";

    public void onCreate(PreferenceActivity activity) {
        mActivity = activity;
    }

    @Override
    public boolean addSmsInputModePreference(Preference.OnPreferenceChangeListener listener) {
        if (mSmsInputMode == null) {
        mSmsInputMode = (ListPreference) mActivity.findPreference(SMS_INPUT_MODE);
        }
        mSmsInputMode.setOnPreferenceChangeListener(listener);
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object arg1) {
        final String key = preference.getKey();
        if (SMS_INPUT_MODE.equals(key)) {
            Log.d(TAG, "Input Mode Changed");
          mSmsInputMode.setValue((String) arg1);
          return true;
        }
        return false;
    }

    @Override
    public void restoreDefaultPreferences(Activity hostActivity, SharedPreferences.Editor editor) {
        editor.putString(SMS_INPUT_MODE, SETTING_INPUT_MODE);
    }
}
