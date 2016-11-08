package com.mediatek.systemui.plugin;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;
import com.mediatek.systemui.ext.ISignalClusterExt;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.SvLteController;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * M: OP09 implementation of Plug-in definition of Status bar.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IStatusBarPlugin")
public class Op09StatusBarPlugin extends DefaultStatusBarPlugin {

    private static final String TAG = "Op09StatusBarPlugin";
    private static final boolean DEBUG = true;

    private int mSlotCount = 0;

    /**
     * Constructs a new Op09StatusBarPlugin instance with Context.
     * @param context A Context object
     */
    public Op09StatusBarPlugin(Context context) {
        super(context);
        this.mSlotCount = SIMHelper.getSlotCount();
    }

    @Override
    public BehaviorSet customizeBehaviorSet() {
        return BehaviorSet.OP09_BS;
    }

    @Override
    public boolean customizeHspaDistinguishable(boolean distinguishable) {
        if (DEBUG) {
            Log.d(TAG, "customizeHspaDistinguishable, HspaDistinguishable = true");
        }

        return true;
    }

    @Override
    public void customizeSignalStrengthNullIcon(int slotId, IconIdWrapper icon) {
        if (DEBUG) {
            Log.d(TAG, "customizeSignalStrengthNullIcon, slotId = " + slotId);
        }

        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.SIGNAL_STRENGTH_NULL);
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
            Log.d(TAG, "customizeDataTypeIcon, roaming = " + roaming + ", dataType = " + dataType);
        }

        icon.setResources(this.getResources());
        icon.setIconId(TelephonyIcons.getDataTypeIconId(dataType));
    }

    @Override
    public void customizeDataNetworkTypeIcon(IconIdWrapper icon, boolean roaming,
            NetworkType networkType, SvLteController svLteController) {
        if (DEBUG) {
            Log.d(TAG, "customizeDataNetworkTypeIcon, networkType = "
                    + networkType + " roaming=" + roaming);
        }
        if (roaming) {
            switch (networkType) {
            case Type_G:
            case Type_E:
            case Type_1X:
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.NETWORK_TYPE_2G);
                break;
            case Type_3G:
            case Type_1X3G:
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.NETWORK_TYPE_3G);
                break;
            case Type_4G:
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.NETWORK_TYPE_ONLY_4G);
                break;
            default:
                break;
            }
        } else {
            switch (networkType) {
            case Type_G:
            case Type_E:
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.NETWORK_TYPE_2G);
                break;
            case Type_1X:
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.NETWORK_TYPE_2G);
                break;
            case Type_1X3G:
                icon.setResources(this.getResources());
                icon.setIconId(TelephonyIcons.NETWORK_TYPE_3G_2G);
                break;
            case Type_4G:
                icon.setResources(this.getResources());
                if (svLteController != null && svLteController.isShow4GDataOnlyForLTE()) {
                    icon.setIconId(TelephonyIcons.NETWORK_TYPE_ONLY_4G);
                } else {
                    icon.setIconId(TelephonyIcons.NETWORK_TYPE_4G_2G);
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    public void customizeDataActivityIcon(IconIdWrapper icon, int dataActivity) {
        if (DEBUG) {
            Log.d(TAG, "customizeDataActivityIcon, dataActivity = " + dataActivity);
        }

        if (dataActivity >= 0 && dataActivity < TelephonyIcons.DATA_ACTIVITY.length) {
            switch (dataActivity) {
                case TelephonyManager.DATA_ACTIVITY_IN:
                case TelephonyManager.DATA_ACTIVITY_OUT:
                case TelephonyManager.DATA_ACTIVITY_INOUT:
                    icon.setResources(this.getResources());
                    icon.setIconId(TelephonyIcons.DATA_ACTIVITY[dataActivity]);
                    break;
                default:
                    icon.setResources(this.getResources());
                    icon.setIconId(0);
                    break;
            }
        }
    }

    @Override
    public boolean customizeHasNoSims(boolean orgHasNoSims) {
        return false;
    }

    @Override
    public ISignalClusterExt customizeSignalCluster() {
        if (DEBUG) {
            Log.d(TAG, "customizeSignalCluster, class = Op09SignalClusterExt");
        }

        return new Op09SignalClusterExt(this.getBaseContext(), this);
    }

    private boolean isMultiSlot() {
        return mSlotCount > 1;
    }
}
