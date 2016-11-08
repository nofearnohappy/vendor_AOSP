package com.mediatek.systemui.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.systemui.ext.DefaultQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.util.SIMHelper;

/**
 * Customize carrier text.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IQuickSettingsPlugin")
public class Op09QuickSettingsPlugin extends DefaultQuickSettingsPlugin {
    public static final String TAG = "Op09QuickSettingsPlugin";

    private static final int DATA_DISCONNECT = 0;
    private static final int DATA_CONNECT = 1;
    private static final int AIRPLANE_DATA_CONNECT = 2;
    private static final int DATA_CONNECT_DISABLE = 3;
    private static final int DATA_RADIO_OFF = 4;

    /**
     * Constructs a new OP02QuickSettingsPlugin instance with Context.
     * @param context A Context object
     */
    public Op09QuickSettingsPlugin(Context context) {
        super(context);
    }

    @Override
    public boolean customizeDisplayDataUsage(boolean isDisplay) {
        Log.i(TAG, "customizeDisplayDataUsage, " + " return true");
        return true;
    }

    @Override
    public String customizeQuickSettingsTileOrder(String defaultString) {
        if (SIMHelper.getSlotCount() <= 1) {
            return mContext.getString(R.string.quick_settings_tiles_Single_Sim_default);
        } else {
            return mContext.getString(R.string.quick_settings_tiles_Dual_Sim_default);
        }
    }

    @Override
    public Object customizeAddQSTile(Object qsTile) {
        return qsTile;
    }

    @Override
    public String customizeDataConnectionTile(int dataState, IconIdWrapper icon,
            String orgLabelStr) {
        Log.d(TAG, "customizeDataConnectionTile dataState=" + dataState + "icon= " + icon);
        icon.setResources(this.getResources());
        int iconId = TelephonyIcons.QS_MOBILE_DISABLE;

        switch(dataState) {
            case DATA_DISCONNECT:
                iconId = TelephonyIcons.QS_MOBILE_DISABLE;
                break;
            case DATA_RADIO_OFF:
            case DATA_CONNECT_DISABLE:
            case AIRPLANE_DATA_CONNECT:
                iconId = TelephonyIcons.QS_MOBILE_DISABLE;
                break;
            case DATA_CONNECT:
                iconId = TelephonyIcons.QS_MOBILE_ENABLE;
                break;
            default :
                iconId = TelephonyIcons.QS_MOBILE_DISABLE;
                break;
        }

        icon.setIconId(iconId);

        return mContext.getString(R.string.data_connection_summary_title);
    }

    @Override
    public String customizeDualSimSettingsTile(boolean enable, IconIdWrapper icon,
            String labelStr) {
        Log.i(TAG, "customizeDualSimSettingsTile, enable = " + enable + " icon=" + icon);
        icon.setResources(this.getResources());
        if (enable) {
            icon.setIconId(TelephonyIcons.QS_DUAL_SIM_SETTINGS_ENABLE);
        } else {
            icon.setIconId(TelephonyIcons.QS_DUAL_SIM_SETTINGS_DISABLE);
        }
        return mContext.getString(R.string.dual_sim_settings);
    }

    @Override
    public void customizeSimDataConnectionTile(int state, IconIdWrapper icon) {
        Log.i(TAG, "customizeSimDataConnectionTile, state = " + state + " icon=" + icon);
        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.IC_SIM_CONNECT_INDICATOR[state]);
    }

    @Override
    public String customizeApnSettingsTile(boolean enable, IconIdWrapper icon, String orgLabelStr) {
        Log.i(TAG, "customizeApnSettingsTile, enable = " + enable + " icon=" + icon);
        icon.setResources(this.getResources());
        if (enable) {
            icon.setIconId(TelephonyIcons.QS_APN_SETTINGS_ENABLE);
        } else {
            icon.setIconId(TelephonyIcons.QS_APN_SETTINGS_DISABLE);
        }

        return mContext.getString(R.string.apn_apn);
    }

}
