package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceCategory;

import com.mediatek.mms.ext.DefaultOpSmsPreferenceActivityExt;

/**
 * Op01SmsPreferenceActivityExt.
 *
 */
public class Op01SmsPreferenceActivityExt extends
        DefaultOpSmsPreferenceActivityExt {

    private Op01MmsPreference mMmsPreferenceExt;

    /**
     * Construction.
     * @param context Context
     */
    public Op01SmsPreferenceActivityExt(Context context) {
        mMmsPreferenceExt = new Op01MmsPreference(context);
    }

    @Override
    public void setMessagePreferences(Activity hostActivity,
            PreferenceCategory pC, int simCount) {
        mMmsPreferenceExt.configSmsPreference(hostActivity, pC, simCount);
    }

    @Override
    public void restoreDefaultPreferences(Activity hostActivity, SharedPreferences.Editor editor) {
        mMmsPreferenceExt.configSmsPreferenceEditorWhenRestore(hostActivity, editor);
    }
}
