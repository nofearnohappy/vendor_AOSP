package com.mediatek.mediatekdm;

import android.os.SystemProperties;

public final class DmFeatureSwitch {
    public static final boolean DM_FUMO = "1".equals(SystemProperties.get("ro.mtk_mdm_fumo"));
    public static final boolean DM_SCOMO = "1".equals(SystemProperties.get("ro.mtk_mdm_scomo"));
    public static final boolean DM_LAWMO = "1".equals(SystemProperties.get("ro.mtk_mdm_lawmo"));
    public static final boolean DM_VOLTE = false; // FeatureOption.MTK_MDM_VOLTE;
    public static final boolean DM_WFHS = false; // "1".equals(SystemProperties.get("ro.mtk_passpoint_r2_support"));
    public static final boolean DM_ANDSF = false; // FeatureOption.MTK_MDM_ANDSF;
    public static final boolean CMCC_SPECIFIC = true;

    public static final boolean MTK_GEMINI_SUPPORT = "1".equals(SystemProperties
            .get("ro.mtk_gemini_support"));
}
