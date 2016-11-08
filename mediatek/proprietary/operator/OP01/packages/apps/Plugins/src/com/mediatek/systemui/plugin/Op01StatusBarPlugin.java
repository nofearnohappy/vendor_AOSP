package com.mediatek.systemui.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;
import com.mediatek.systemui.ext.ISignalClusterExt;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;

/**
 * M: OP01 implementation of Plug-in definition of Status bar.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IStatusBarPlugin")
public class Op01StatusBarPlugin extends DefaultStatusBarPlugin {

    private static final String TAG = "Op01StatusBarPlugin";
    private static final boolean DEBUG = true;

    /**
     * Constructs a new Op01StatusBarPlugin instance with Context.
     * @param context A Context object
     */
    public Op01StatusBarPlugin(Context context) {
        super(context);
    }

    @Override
    public BehaviorSet customizeBehaviorSet() {
        return BehaviorSet.OP01_BS;
    }

    @Override
    public boolean customizeHspaDistinguishable(boolean distinguishable) {
        return false;
    }

    @Override
    public void customizeSignalStrengthIcon(int level, boolean roaming, IconIdWrapper icon) {
        if (DEBUG) {
            Log.d(TAG, "customizeSignalStrengthIcon, level = " + level + ", roaming = " + roaming);
        }

        if (level >= 0 && level < TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_FULL.length) {
            icon.setResources(this.getResources());
            icon.setIconId(TelephonyIcons.getSignalStrengthIcon(level, roaming));
        }
    }

    @Override
    public void customizeSignalStrengthOfflineIcon(int slotId, IconIdWrapper icon) {
        if (DEBUG) {
            Log.d(TAG, "customizeSignalStrengthOfflineIcon, slotId = " + slotId);
        }

        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.SIGNAL_STRENGTH_OFFLINE);
    }

    @Override
    public void customizeDataTypeIcon(IconIdWrapper icon, boolean roaming, DataType dataType) {
        if (DEBUG) {
            Log.d(TAG, "customizeDataTypeIcon, dataType = " + dataType);
        }

        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.getDataTypeIconId(dataType));
    }

    @Override
    public void customizeDataNetworkTypeIcon(IconIdWrapper icon,
            boolean roaming, NetworkType networkType) {
        if (DEBUG) {
            Log.d(TAG, "customizeDataNetworkTypeIcon, roaming = " + roaming
                    + ", networkType = " + networkType);
        }

        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.getNetworkTypeIconId(networkType));
    }

    @Override
    public void customizeDataActivityIcon(IconIdWrapper icon, int dataActivity) {
        if (dataActivity >= 0 && dataActivity < TelephonyIcons.DATA_ACTIVITY.length) {
            icon.setResources(this.getResources());
            icon.setIconId(TelephonyIcons.DATA_ACTIVITY[dataActivity]);
        }
    }

    @Override
    public ISignalClusterExt customizeSignalCluster() {
        return new Op01SignalClusterExt(this.getBaseContext(), this);
    }
}
