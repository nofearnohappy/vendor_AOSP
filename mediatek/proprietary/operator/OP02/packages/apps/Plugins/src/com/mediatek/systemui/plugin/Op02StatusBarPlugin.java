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
import com.mediatek.systemui.statusbar.util.SIMHelper;

/**
 * M: OP02 implementation of Plug-in definition of Status bar.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IStatusBarPlugin")
public class Op02StatusBarPlugin extends DefaultStatusBarPlugin {

    private static final String TAG = "Op02StatusBarPlugin";
    private static final boolean DEBUG = true;

    private int mSlotCount = 0;

    /**
     * Constructs a new Op02StatusBarPlugin instance with Context.
     * @param context A Context object
     */
    public Op02StatusBarPlugin(Context context) {
        super(context);
        this.mSlotCount = SIMHelper.getSlotCount();
    }

    @Override
    public BehaviorSet customizeBehaviorSet() {
        return BehaviorSet.OP02_BS;
    }

    @Override
    public boolean customizeHspaDistinguishable(boolean distinguishable) {
        if (DEBUG) {
            Log.d(TAG, "customizeHspaDistinguishable, HspaDistinguishable = true");
        }

        return true;
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
    public void customizeSignalStrengthNullIcon(int slotId, IconIdWrapper icon) {
        if (isMultiSlot()) {
            if (slotId >= 0 && slotId < TelephonyIcons.SIGNAL_STRENGTH_NULLS.length) {
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.SIGNAL_STRENGTH_NULLS[slotId]);
            }
        } else {
            icon.setResources(this.getResources());
            icon.setIconId(TelephonyIcons.SIGNAL_STRENGTH_NULL);
        }

        if (DEBUG) {
            Log.d(TAG, "customizeSignalStrengthNullIcon, slotId = " + slotId
                    + ", mSlotCount = " + mSlotCount + ", NullIcon = " + icon);
        }
    }

    @Override
    public void customizeSignalIndicatorIcon(int slotId, IconIdWrapper icon) {
        if (slotId >= 0 && slotId < TelephonyIcons.SIGNAL_INDICATOR.length) {
            if (isMultiSlot()) {
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.SIGNAL_INDICATOR[slotId]);
            }
        }
    }

    @Override
    public void customizeDataTypeIcon(IconIdWrapper icon, boolean roaming, DataType dataType) {
        if (DEBUG) {
            Log.d(TAG, "customizeDataTypeIcon, roaming = " + roaming + ", dataType = " + dataType);
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
        if (DEBUG) {
            Log.d(TAG, "customizeDataActivityIcon, dataActivity = " + dataActivity);
        }

        if (dataActivity >= 0 && dataActivity < TelephonyIcons.DATA_ACTIVITY.length) {
            icon.setResources(this.getResources());
            icon.setIconId(TelephonyIcons.DATA_ACTIVITY[dataActivity]);
        }
    }

    @Override
    public boolean customizeHasNoSims(boolean orgHasNoSims) {
        return false;
    }

    /// HD Voice
    @Override
    public void customizeHDVoiceIcon(IconIdWrapper icon) {
        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.HD_VOICE_ICON);
    }

    @Override
    public ISignalClusterExt customizeSignalCluster() {
        if (DEBUG) {
            Log.d(TAG, "customizeSignalCluster, class = Op02SignalClusterExt");
        }
        return new Op02SignalClusterExt(this.getBaseContext(), this);
    }

    private boolean isMultiSlot() {
        return mSlotCount > 1;
    }
}
