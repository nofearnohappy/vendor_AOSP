package com.mediatek.calendar.features;

import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.utility.FeatureOption;
import com.mediatek.common.mom.MobileManagerUtils;

import java.util.ArrayList;

/**
 * A class to manage the features and operators.
 * TODO: to Change this class to some small classes, to get a better performance and comprehension.
 */
public class Features {

    private static final int FEATURE_CLEAR_ALL_EVENTS = 3;
    private static final int FEATURE_THEME_MANAGER = 4;
    private static final int FEATURE_BEAM_PLUS = 5;

    private static final ArrayList<Integer> COMMON_FEATURES = new ArrayList<Integer>();
    private static final String TAG = "CalFeature";

    static {
        if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
            COMMON_FEATURES.add(FEATURE_BEAM_PLUS);
        }
    }

    public static boolean isThemeManagerEnabled() {
        return isFeatureEnabled(FEATURE_THEME_MANAGER);
    }

    /**
     * is the given feature name is needed by current operator
     * @param feature
     * @return
     */
    private static boolean isFeatureEnabled(int feature) {
        return COMMON_FEATURES.contains(feature);
    }

    /**
     * In fact, NFC(in beam plus) is a common feature.
     * @return
     */
    public static boolean isBeamPlusEnabled() {
        return isFeatureEnabled(FEATURE_BEAM_PLUS);
    }

    /// M: added for HotKnot @{
    public static boolean isHotKnotSupported() {
        // TODO Auto-generated method stub
        return FeatureOption.MTK_HOTKNOT_SUPPORT;
    }
    /// @}

    /**
     * check does it support CTA feature.
     */
    public static boolean isCtaSupported() {
        boolean support = MobileManagerUtils.isSupported();
        LogUtil.d(TAG, "isCtaSupported?" + support);
        return support;
    }
}
