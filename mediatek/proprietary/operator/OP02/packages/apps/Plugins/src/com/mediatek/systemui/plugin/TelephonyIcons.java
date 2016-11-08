package com.mediatek.systemui.plugin;

import com.mediatek.op02.plugin.R;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.NetworkType;

/**
 * M: This class define the OP02 constants of telephony icons.
 */
final class TelephonyIcons {

    /** Signal strength icons. @{ */
    static final int[] TELEPHONY_SIGNAL_STRENGTH_FULL = {
            R.drawable.stat_sys_signal_0_fully,
            R.drawable.stat_sys_signal_1_fully,
            R.drawable.stat_sys_signal_2_fully,
            R.drawable.stat_sys_signal_3_fully,
            R.drawable.stat_sys_signal_4_fully,
    };
    static final int[] TELEPHONY_SIGNAL_STRENGTH_ROAMING_FULL = {
            R.drawable.stat_sys_signal_0_fully,
            R.drawable.stat_sys_signal_1_fully,
            R.drawable.stat_sys_signal_2_fully,
            R.drawable.stat_sys_signal_3_fully,
            R.drawable.stat_sys_signal_4_fully,
    };
    /** Signal strength icons. @} */

    /** Data type icons. @{ */
    static final int[] DATA_TYPE = {
        R.drawable.stat_sys_data_fully_connected_1x,
        R.drawable.stat_sys_data_fully_connected_3g,
        R.drawable.stat_sys_data_fully_connected_4g,
        R.drawable.stat_sys_data_fully_connected_e,
        R.drawable.stat_sys_data_fully_connected_g,
        R.drawable.stat_sys_data_fully_connected_h,
        R.drawable.stat_sys_data_fully_connected_h_plus
    };

    static final int DATA_TYPE_1X = R.drawable.stat_sys_data_fully_connected_1x;
    static final int DATA_TYPE_G = R.drawable.stat_sys_data_fully_connected_g;
    static final int DATA_TYPE_E = R.drawable.stat_sys_data_fully_connected_e;
    static final int DATA_TYPE_3G = R.drawable.stat_sys_data_fully_connected_3g;
    static final int DATA_TYPE_H = R.drawable.stat_sys_data_fully_connected_h;
    static final int DATA_TYPE_H_PLUS = R.drawable.stat_sys_data_fully_connected_h_plus;
    static final int DATA_TYPE_4G = R.drawable.stat_sys_data_fully_connected_4g;
    /// M: Support CA 4G+ icon
    static final int DATA_TYPE_4G_PLUS = R.drawable.stat_sys_data_fully_connected_4ga;

    /** Data type icons. @} */

    /** HD Voice icon. @{ */
    static final int HD_VOICE_ICON = R.drawable.stat_sys_hd_voice_call;
    /** HD Voice icon. @} */

    /** Roaming icons. @{ */
    static final int DATA_ROAMING_INDICATOR = R.drawable.stat_sys_data_fully_connected_roam;
    /** Roaming icons. @} */

    /** Network type icons. @{ */

    static final int[] NETWORK_TYPE = {
            R.drawable.stat_sys_network_type_g,
            R.drawable.stat_sys_network_type_3g,
            R.drawable.stat_sys_network_type_4g
    };

    static final int NETWORK_TYPE_G = NETWORK_TYPE[0];

    static final int NETWORK_TYPE_3G = NETWORK_TYPE[1];

    static final int NETWORK_TYPE_4G = NETWORK_TYPE[2];

    /** Network type icons. @} */

    /** Data activity type icons. @{ */

    static final int[] DATA_ACTIVITY = {
            R.drawable.stat_sys_signal_not_inout,
            R.drawable.stat_sys_signal_in,
            R.drawable.stat_sys_signal_out,
            R.drawable.stat_sys_signal_inout
    };

    /** Data activity type icons. @} */

    /** Signal strength null icons. @{ */

    static final int[] SIGNAL_STRENGTH_NULLS = {
            R.drawable.stat_sys_signal_null_sim1,
            R.drawable.stat_sys_signal_null_sim2,
            R.drawable.stat_sys_signal_null_sim3,
            R.drawable.stat_sys_signal_null_sim4
    };

    static final int SIGNAL_STRENGTH_NULL = R.drawable.stat_sys_signal_null;

    /** Signal strength null icons. @} */

    /** Signal indicator icons. @{ */

    static final int[] SIGNAL_INDICATOR = {
            R.drawable.stat_sys_signal_indicator_sim1,
            R.drawable.stat_sys_signal_indicator_sim2,
            R.drawable.stat_sys_signal_indicator_sim3,
            R.drawable.stat_sys_signal_indicator_sim4,
    };

    /** Signal indicator icons. @} */

    /**
     * Customize Signal strength icon.
     * @param level telephony signal strength leve.
     * @param roaming roaming Whether at roaming state.
     * @return Signal strength icon id.
     */
    static final int getSignalStrengthIcon(int level, boolean roaming) {
        if (level >= 0 && level < TELEPHONY_SIGNAL_STRENGTH_FULL.length) {
            if (roaming) {
                return TELEPHONY_SIGNAL_STRENGTH_ROAMING_FULL[level];
            } else {
                return TELEPHONY_SIGNAL_STRENGTH_FULL[level];
            }
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
                case Type_1X:
                    dataTypeIconId = DATA_TYPE_1X;
                    break;
                case Type_3G:
                    dataTypeIconId = DATA_TYPE_3G;
                    break;
                case Type_4G:
                    dataTypeIconId = DATA_TYPE_4G;
                    break;
                case Type_4G_PLUS:
                    dataTypeIconId = DATA_TYPE_4G_PLUS;
                    break;
                case Type_E:
                    dataTypeIconId = DATA_TYPE_E;
                    break;
                case Type_G:
                    dataTypeIconId = DATA_TYPE_G;
                    break;
                case Type_H:
                    dataTypeIconId = DATA_TYPE_H;
                    break;
                case Type_H_PLUS:
                    dataTypeIconId = DATA_TYPE_H_PLUS;
                    break;
                default:
                    break;
            }
        }

        return dataTypeIconId;
    }

    /**
     * Get network type icon id.
     *
     * @param networkType NetworkType.
     * @return network type icon id.
     */
    static final int getNetworkTypeIconId(NetworkType networkType) {
        int networkTypeIconId = 0;

        if (networkType != null) {
            switch (networkType) {
                case Type_E:
                case Type_G:
                    networkTypeIconId = TelephonyIcons.NETWORK_TYPE_G;
                    break;
                case Type_3G:
                    networkTypeIconId = TelephonyIcons.NETWORK_TYPE_3G;
                    break;
                case Type_4G:
                    networkTypeIconId = TelephonyIcons.NETWORK_TYPE_4G;
                    break;
                default:
                    break;
            }
        }

        return networkTypeIconId;
    }
}
