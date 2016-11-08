package com.mediatek.systemui.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.op02.plugin.R;
import com.mediatek.systemui.ext.DefaultQuickSettingsPlugin;
import com.mediatek.systemui.statusbar.util.SIMHelper;

/**
 * Customize carrier text.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IQuickSettingsPlugin")
public class OP02QuickSettingsPlugin extends DefaultQuickSettingsPlugin {
    public static final String TAG = "OP02QuickSettingsPlugin";

    /**
     * Constructs a new OP02QuickSettingsPlugin instance with Context.
     * @param context A Context object
     */
    public OP02QuickSettingsPlugin(Context context) {
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
}
