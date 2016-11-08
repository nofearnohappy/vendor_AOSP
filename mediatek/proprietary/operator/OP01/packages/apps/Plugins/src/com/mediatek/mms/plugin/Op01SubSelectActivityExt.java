package com.mediatek.mms.plugin;

import android.app.Activity;
import android.content.Context;

import com.mediatek.mms.ext.DefaultOpSubSelectActivityExt;

/**
 * Op01SubSelectActivityExt.
 *
 */
public class Op01SubSelectActivityExt extends DefaultOpSubSelectActivityExt {

    private Op01MmsPreference mMmsPreferenceExt = null;

    /**
     * Construction.
     * @param context Context
     */
    public Op01SubSelectActivityExt(Context context) {
        mMmsPreferenceExt = new Op01MmsPreference(context);
    }

    @Override
    public void onCreate(Activity hostActivity) {
        mMmsPreferenceExt.configSelectCardPreferenceTitle(hostActivity);
    }

    @Override
    public boolean onListItemClick(Activity hostActivity, final int subId) {
        return mMmsPreferenceExt.handleSelectCardPreferenceTreeClick(hostActivity, subId);
    }
}
