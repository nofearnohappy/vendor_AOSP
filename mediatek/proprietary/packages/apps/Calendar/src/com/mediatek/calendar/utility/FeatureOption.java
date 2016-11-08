package com.mediatek.calendar.utility;

import android.os.SystemProperties;

/**
 * M: Add FeatureOption class.
 */
public class FeatureOption {

    public static final boolean MTK_HOTKNOT_SUPPORT =
            SystemProperties.get("ro.mtk_hotknot_support").equals("1");

    public static final boolean MTK_BEAM_PLUS_SUPPORT =
            SystemProperties.get("ro.mtk_beam_plus_support").equals("1");
}
