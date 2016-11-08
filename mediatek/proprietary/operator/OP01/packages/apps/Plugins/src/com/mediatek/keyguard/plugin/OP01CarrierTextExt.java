package com.mediatek.keyguard.plugin;

import com.mediatek.common.PluginImpl;
import com.mediatek.keyguard.ext.DefaultCarrierTextExt;

/**
 * Customize the carrier text for OP01.
 */
@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.ICarrierTextExt")
public class OP01CarrierTextExt extends DefaultCarrierTextExt {

    /**
     * The customized divider of carrier text.
     *
     * @param divider the current carrier text divider string.
     *
     * @return the customized carrier text divider string.
     */
    @Override
    public String customizeCarrierTextDivider(String divider) {
        String carrierDivider = " | ";
        return carrierDivider;
    }
}
