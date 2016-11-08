package com.mediatek.quicksearchbox.utility;

import android.os.SystemProperties;

/**
 * M: Add FeatureOption class.
 */
public class FeatureOption {
    /** mtk owner sim support */
    public static final boolean MTK_ONLY_OWNER_SIM_SUPPORT =
            SystemProperties.get("ro.mtk_owner_sim_support").equals("1");
}
