package com.mediatek.settings.plugin;

import android.content.Context;
import android.preference.PreferenceGroup;
import android.util.AttributeSet;


/**
 * Used in ManualNetworkSelection to show slot1 GSM Preference.
 *
 */
public class NormalPreferenceGroup extends PreferenceGroup {
    /**
     * Constructor with Context.
     * @param context Input Context
     * @param attrs Input AttributeSet
     * @param defStyle Input default Style
     */
    public NormalPreferenceGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    /**
     * Constructor with Context, AttributeSet.
     * @param context Input Context
     * @param attrs Input AttributeSet
     */
    public NormalPreferenceGroup(Context context, AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.preferenceStyle);
    }
    /**
     * Constructor with Context.
     * @param context Input Context
     */
    public NormalPreferenceGroup(Context context) {
        this(context, null);
    }
}
