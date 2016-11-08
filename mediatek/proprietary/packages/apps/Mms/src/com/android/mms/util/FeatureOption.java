
package com.android.mms.util;

import android.os.SystemProperties;

public final class FeatureOption {

    /**
     * check if GEMINI is turned on or not
     */
    public static final boolean MTK_GEMINI_SUPPORT = SystemProperties.get("ro.mtk_gemini_support").equals("1");

    /**
     * check if MTK_WAPPUSH_SUPPORT is turned on or not
     */
    public static final boolean MTK_WAPPUSH_SUPPORT = SystemProperties.get("ro.mtk_wappush_support").equals("1");

    /**
     * check if MTK_DRM_APP is turned on or not
     */
    public static final boolean MTK_DRM_APP = SystemProperties.get("ro.mtk_oma_drm_support").equals("1");

    /**
     * check if MTK_GEMINI_3G_SWITCH is turned on or not
     */
    public static final boolean MTK_GEMINI_3G_SWITCH = SystemProperties.get("ro.mtk_gemini_3g_switch").equals("1");

    /**
     * check if MTK_BRAZIL_CUSTOMIZATION_CLARO is turned on or not
     */
    public static final boolean MTK_BRAZIL_CUSTOMIZATION_CLARO = SystemProperties.get("ro.brazil_cust_claro").equals("1");

    public static final boolean MTK_SEND_RR_SUPPORT = SystemProperties.get("ro.mtk_send_rr_support").equals("1");

    public static final boolean MTK_C2K_SUPPORT = SystemProperties.get("ro.mtk_c2k_support")
            .equals("1");

    public static final boolean MTK_ONLY_OWNER_SIM_SUPPORT = SystemProperties.get("ro.mtk_owner_sim_support").equals("1");

    /**
     * check if slim project or not
     */
    public static final boolean MTK_GMO_ROM_OPTIMIZE = SystemProperties.get("ro.mtk_gmo_rom_optimize").equals("1");

    /**
     * VoLTE feature option
     */
    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get("ro.mtk_ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get("ro.mtk_volte_support").equals("1");
    public static final boolean MTK_MWI_SUPPORT = MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;

    /**
     * check if use MTK enhancement in onBackPressed() of ConversationList.
     */
    public static final boolean MTK_PERF_RESPONSE_TIME = SystemProperties.get("ro.mtk_perf_response_time").equals("1");
}
