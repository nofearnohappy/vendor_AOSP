package com.mediatek.systemui.plugin;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.ext.DefaultSignalClusterExt;
import com.mediatek.systemui.ext.IStatusBarPlugin;

/**
 * M: OP01 ISignalClusterExt implements for SystemUI.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.ISignalClusterExt")
public class Op01SignalClusterExt extends DefaultSignalClusterExt {
    private static final String TAG = "Op01SignalClusterExt";

    /**
     * Constructs a new Op01SignalClusterExt instance.
     *
     * @param context A Context object
     * @param statusBarPlugin The interface for Plug-in definition of Status bar.
     */
    public Op01SignalClusterExt(Context context, IStatusBarPlugin statusBarPlugin) {
        super(context, statusBarPlugin);
    }

    @Override
    protected BasePhoneStateExt createPhoneState(int slotId, int subId,
            ViewGroup signalClusterCombo, ImageView mobileNetworkType,
            ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
        final Op01PhoneStateExt state = new Op01PhoneStateExt(slotId, subId);
        state.setViews(signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength,
                mobileType);
        return state;
    }

    /**
     * OP01 PhoneStateExt implements for SystemUI.
     */
    class Op01PhoneStateExt extends BasePhoneStateExt {

        public Op01PhoneStateExt(int slotId, int subId) {
            super(slotId, subId);
        }

        @Override
        public void setViews(ViewGroup signalClusterCombo, ImageView mobileNetworkType,
                ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
            super.setViews(signalClusterCombo, mobileNetworkType,
                    mobileGroup, mobileStrength, mobileType);
            setViews();
        }

        private final void setViews() {
            // NetworkType & DataType & DataActivity container
            mMobileNetworkDataGroup.setLayoutParams(generateLayoutParams());

            // 1. NetworkType
            if (mMobileNetworkType.getParent() != null) {
                ((ViewGroup) mMobileNetworkType.getParent()).removeView(mMobileNetworkType);
            }
            mMobileNetworkDataGroup.addView(mMobileNetworkType, generateLayoutParams());

            // 2. DataType
            if (mMobileType.getParent() != null) {
                ((ViewGroup) mMobileType.getParent()).removeView(mMobileType);
            }
            mMobileNetworkDataGroup.addView(mMobileType, generateLayoutParams());

            // 3. DataActivity
            mMobileDataActivity = new ImageView(mContext);
            mMobileNetworkDataGroup.addView(mMobileDataActivity,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER));

            // Roaming Indicator
            mMobileRoamingIndicator = new ImageView(mContext);
            mMobileGroup.addView(mMobileRoamingIndicator, generateLayoutParams());

            // Add views to SignalClusterCombo
            final int addViewIndex = mSignalClusterCombo.indexOfChild(mMobileGroup);
            if (addViewIndex >= 0) {
                mSignalClusterCombo.addView(mMobileNetworkDataGroup, addViewIndex);
            }

            // Add SignalClusterCombo to MobileSignalGroup
            if (mMobileSignalGroup != null && mSignalClusterCombo.getParent() == null) {
                mMobileSignalGroup.addView(mSignalClusterCombo);
            }
        }

        @Override
        protected void applyMobileSignalStrength() {
            if (DEBUG) {
                Log.d(TAG, "applyMobileSignalStrength(), mSlotId = " + mSlotId
                        + ", mSubId = " + mSubId);
            }

            if (mMobileStrength != null) {
                if (!mHasSimService) {
                    if (DEBUG) {
                        Log.d(TAG, "Set signal strength OFFLINE icon.");
                    }
                    setImage(mMobileStrength, mMobileStrengthOfflineIconId);
                    mMobileStrength.setVisibility(View.VISIBLE);
                    mMobileGroup.setVisibility(View.VISIBLE);
                } else if (isSignalStrengthNullIcon()) {
                    if (DEBUG) {
                        Log.d(TAG, "Set signal strength null icon.");
                    }
                    setImage(mMobileStrength, mDefaultSignalNullIconId);
                    mMobileStrength.setVisibility(View.VISIBLE);
                    mMobileGroup.setVisibility(View.VISIBLE);
                } else {
                    setImage(mMobileStrength, mMobileStrengthIconId);
                    mMobileStrength.setVisibility(View.VISIBLE);
                    mMobileGroup.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
