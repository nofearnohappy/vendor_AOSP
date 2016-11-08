package com.mediatek.settings.plugin;

import android.os.SystemProperties;

/**
 * Use Utils replace MTK feature option.
 */
public class Utils {
    private static final String TAG = "Utils";

    /**
     * Check if mtk 3g switch feature is enabled.
     * @return true if support 3GSwitch
     */
    public static boolean is3GSwitchSupport() {
        return SystemProperties.getInt("ro.mtk_gemini_3g_switch", 0) == 1;
    }
}
