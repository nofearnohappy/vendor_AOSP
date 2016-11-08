package com.mediatek.systemui.plugin;

import com.mediatek.op09.plugin.R;
import com.mediatek.systemui.statusbar.extcb.DataType;

/**
 * M: This class define the OP09 constants of telephony icons.
 */
final class TelephonyIcons {

    /** Data type icons. @{ */
    static final int[] DATA_TYPE = {
        R.drawable.stat_sys_data_fully_connected_2g,
        R.drawable.stat_sys_data_fully_connected_3g,
        R.drawable.stat_sys_data_fully_connected_4g,
        R.drawable.stat_sys_data_fully_connected_e,
        R.drawable.stat_sys_data_fully_connected_g,
        R.drawable.stat_sys_data_fully_connected_h,
        R.drawable.stat_sys_data_fully_connected_h_plus
    };

    static final int DATA_TYPE_2G = R.drawable.stat_sys_data_fully_connected_2g;
    static final int DATA_TYPE_G = R.drawable.stat_sys_data_fully_connected_g;
    static final int DATA_TYPE_E = R.drawable.stat_sys_data_fully_connected_e;
    static final int DATA_TYPE_3G = R.drawable.stat_sys_data_fully_connected_3g;
    static final int DATA_TYPE_H = R.drawable.stat_sys_data_fully_connected_h;
    static final int DATA_TYPE_H_PLUS = R.drawable.stat_sys_data_fully_connected_h_plus;
    static final int DATA_TYPE_4G = R.drawable.stat_sys_data_fully_connected_4g;
    /// M: Support CA 4G+ icon
    static final int DATA_TYPE_4G_PLUS = R.drawable.stat_sys_data_fully_connected_4ga;

    /** Data type icons. @} */

    /** Data activity type icons. @{ */

    static final int[] DATA_ACTIVITY = {
            R.drawable.stat_sys_signal_not_inout,
            R.drawable.stat_sys_signal_in,
            R.drawable.stat_sys_signal_out,
            R.drawable.stat_sys_signal_inout
    };

    /** Data activity type icons. @} */

    /** Roaming icons. @{ */

    static final int DATA_ROAMING_INDICATOR = R.drawable.stat_sys_data_fully_connected_roam;

    /** Roaming icons. @} */

    /** Network type icons. @{ */

    static final int NETWORK_TYPE_2G = R.drawable.stat_sys_network_type_2g;
    static final int NETWORK_TYPE_3G_2G = R.drawable.stat_sys_network_type_3g_2g;
    static final int NETWORK_TYPE_3G = R.drawable.stat_sys_network_type_3g;
    static final int NETWORK_TYPE_4G_2G = R.drawable.stat_sys_network_type_4g_2g;
    static final int NETWORK_TYPE_4G = R.drawable.stat_sys_network_type_4g;
    static final int NETWORK_TYPE_ONLY_4G = R.drawable.stat_sys_network_type_4g_data_only;

    /** Network type icons. @} */

    /** Signal strength null icons. @{ */

    static final int SIGNAL_STRENGTH_NULL = R.drawable.stat_sys_signal_null;

    /** Signal strength null icons. @} */

    /** Signal strength offline icons. @{ */
    static final int SIGNAL_STRENGTH_OFFLINE = R.drawable.stat_sys_signal_radio_off;
    /** Signal strength offline icons. @{ */

    public static final int[] TELEPHONY_SIGNAL_STRENGTH_UP = {
         /// M: fix alps01968282. @{
         R.drawable.stat_sys_signal_up_1, //R.drawable.stat_sys_signal_up_0,
         /// @}
         R.drawable.stat_sys_signal_up_1,
         R.drawable.stat_sys_signal_up_2,
         R.drawable.stat_sys_signal_up_3,
         R.drawable.stat_sys_signal_up_4
    };

    public static final int[] TELEPHONY_SIGNAL_STRENGTH_DOWN = {
        /// M: fix alps01968282. @{
        R.drawable.stat_sys_signal_down_1, //R.drawable.stat_sys_signal_down_0,
        /// @}
        R.drawable.stat_sys_signal_down_1,
        R.drawable.stat_sys_signal_down_2,
        R.drawable.stat_sys_signal_down_3,
        R.drawable.stat_sys_signal_down_4
    };

    // For Tdd 4G only mode.
    public static final int[] TELEPHONY_SIGNAL_STRENGTH_SINGLE = {
        /// M :fix alps02044308. @{
        R.drawable.stat_sys_signal_1_fully, //R.drawable.stat_sys_signal_0_fully,
        /// @}
        R.drawable.stat_sys_signal_1_fully,
        R.drawable.stat_sys_signal_2_fully,
        R.drawable.stat_sys_signal_3_fully,
        R.drawable.stat_sys_signal_4_fully
    };
    /// @}

    static final int SIGNAL_SINGLE = 0;
    static final int SIGNAL_UP = 1;
    static final int SIGNAL_DOWN = 2;

    public static int getSignalStrengthIcon(int signalFlag, int level) {
        if (SIGNAL_UP == signalFlag) {
            return TELEPHONY_SIGNAL_STRENGTH_UP[level];
        } else if (SIGNAL_DOWN == signalFlag) {
            return TELEPHONY_SIGNAL_STRENGTH_DOWN[level];
        } else if (SIGNAL_SINGLE == signalFlag) {
            return TELEPHONY_SIGNAL_STRENGTH_SINGLE[level];
        }
        return 0;
    }

    /**
     * Get data type icon id.
     *
     * @param dataType DataType.
     * @return data type icon id.
     */
    static final int getDataTypeIconId(DataType dataType) {
        int dataTypeIconId = 0;

        if (dataType != null) {
            switch (dataType) {
                case Type_E:
                case Type_G:
                case Type_1X:
                    dataTypeIconId = DATA_TYPE_2G;
                    break;
                case Type_3G:
                case Type_H:
                case Type_H_PLUS:
                    dataTypeIconId = DATA_TYPE_3G;
                    break;
                case Type_4G:
                    dataTypeIconId = DATA_TYPE_4G;
                    break;
                case Type_4G_PLUS:
                    dataTypeIconId = DATA_TYPE_4G_PLUS;
                    break;
                default:
                    break;
            }
        }

        return dataTypeIconId;
    }

    /// For QuickSettings customization @{.
    static final int QS_MOBILE_DISABLE = R.drawable.ic_qs_mobile_disable;
    static final int QS_MOBILE_ENABLE = R.drawable.ic_qs_mobile_enable;

    static final int QS_DUAL_SIM_SETTINGS_DISABLE = R.drawable.ic_qs_dual_sim_settings_disable;
    static final int QS_DUAL_SIM_SETTINGS_ENABLE = R.drawable.ic_qs_dual_sim_settings_enable;

    static final int QS_APN_SETTINGS_DISABLE = R.drawable.ic_qs_apn_settings_disable;
    static final int QS_APN_SETTINGS_ENABLE = R.drawable.ic_qs_apn_settings_enable;

    /// Add for SIM Conn.
    public static final int[] IC_SIM_CONNECT_INDICATOR = {
        R.drawable.ic_qs_mobile_sim1_enable_disable,
        R.drawable.ic_qs_mobile_sim1_enable_enable,
        R.drawable.ic_qs_mobile_sim1_disable_disable,
        R.drawable.ic_qs_mobile_sim1_disable_enable,
        R.drawable.ic_qs_mobile_sim2_enable_disable,
        R.drawable.ic_qs_mobile_sim2_enable_enable,
        R.drawable.ic_qs_mobile_sim2_disable_disable,
        R.drawable.ic_qs_mobile_sim2_disable_enable,
        R.drawable.ic_qs_mobile_all_disable_disable,
        R.drawable.ic_qs_mobile_sim1_enable_off,
        R.drawable.ic_qs_mobile_sim1_disable_off,
        R.drawable.ic_qs_mobile_sim2_enable_off,
        R.drawable.ic_qs_mobile_sim2_disable_off
    };
    /// For QuickSettings customization @}.

}
