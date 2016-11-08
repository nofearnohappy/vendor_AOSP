package com.mediatek.settings.plugin;

import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultApnSettingsExt;

/**
 * CU feature , CU apn settings can not edit.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IApnSettingsExt")
public class OP02ApnSettingsExt extends DefaultApnSettingsExt {

    private static final String TAG = "OP02ApnSettingsExt";
    private static final String CU_NUMERIC_1 = "46001";
    private static final String CU_NUMERIC_2 = "46009";
    /// CU sim in Hongkong
    private static final String CU_NUMERIC_3 = "45407";

    @Override
    public boolean isAllowEditPresetApn(String type, String apn, String numeric, int sourcetype) {
        return (!isCUNumeric(numeric) || sourcetype != 0);
    }

    private boolean isCUNumeric(String numeric) {
        if (numeric != null) {
            return numeric.equals(CU_NUMERIC_1)
                || numeric.equals(CU_NUMERIC_2)
                || numeric.equals(CU_NUMERIC_3);
        } else {
            return false;
        }
    }

    /**
     * if sort APN by the name.
     * @param order sort by the name.
     * @return the sort string.
     */
    @Override
    public String getApnSortOrder(String order) {
        return null;
    }
}

