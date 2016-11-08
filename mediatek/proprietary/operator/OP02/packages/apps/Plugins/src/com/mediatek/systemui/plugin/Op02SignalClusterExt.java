package com.mediatek.systemui.plugin;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mediatek.common.PluginImpl;
import com.mediatek.op02.plugin.R;
import com.mediatek.systemui.ext.DefaultSignalClusterExt;
import com.mediatek.systemui.ext.IStatusBarPlugin;
import com.mediatek.systemui.statusbar.util.SIMHelper;

/**
 * M: OP02 ISignalClusterExt implements for SystemUI.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.ISignalClusterExt")
public class Op02SignalClusterExt extends DefaultSignalClusterExt {
    private static final String TAG = "Op02SignalClusterExt";

    /**
     * Constructs a new Op02SignalClusterExt instance.
     *
     * @param context A Context object
     * @param statusBarPlugin The interface for Plug-in definition of Status bar.
     */
    public Op02SignalClusterExt(Context context, IStatusBarPlugin statusBarPlugin) {
        super(context, statusBarPlugin);
    }

    @Override
    public void onAttachedToWindow(LinearLayout mobileSignalGroup, ImageView noSimsView) {
        if (DEBUG) {
            Log.d(TAG, "onAttachedToWindow(), mobileSignalGroup = " + mobileSignalGroup);
        }

        mMobileSignalGroup = mobileSignalGroup;
        mNoSimsView = noSimsView;

        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.removeAllViews();

            // add PhoneState to ViewGroup
            for (int i = SIMHelper.SLOT_INDEX_DEFAULT; i < mSlotCount; i++) {
                if (DEBUG) {
                    Log.d(TAG, "onAttachedToWindow(), mPhoneStates[" + i + "] =" + mPhoneStates[i]);
                }
                if (mPhoneStates[i] == null) {
                    mPhoneStates[i] = createDefaultPhoneState(i);
                }
                mPhoneStates[i].addToSignalGroup();
            }
        }
    }

    @Override
    protected BasePhoneStateExt createDefaultPhoneState(int slotId) {
        final int subId = getSubId(slotId);
        if (DEBUG) {
            Log.d(TAG, "createDefaultPhoneState(), slotId = " + slotId + ", subId = " + subId);
        }

        final ViewGroup signalClusterCombo = (ViewGroup) LayoutInflater.from(mContext)
                .inflate(R.layout.mobile_signal_group, null);

        final ImageView mobileNetworkType = (ImageView) signalClusterCombo
                .findViewById(R.id.network_type);

        final ViewGroup mobileGroup = (ViewGroup) signalClusterCombo
                .findViewById(R.id.mobile_combo);
        final ImageView mobileStrength = (ImageView) signalClusterCombo
                .findViewById(R.id.mobile_signal);
        final ImageView mobileType = (ImageView) signalClusterCombo.findViewById(R.id.mobile_type);

        final Op02PhoneStateExt state = new Op02PhoneStateExt(slotId, subId);
        state.setViews(signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength,
                mobileType);

        return state;
    }

    @Override
    protected BasePhoneStateExt createPhoneState(int slotId, int subId,
            ViewGroup signalClusterCombo, ImageView mobileNetworkType,
            ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
        final Op02PhoneStateExt state = new Op02PhoneStateExt(slotId, subId);
        state.setViews(signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength,
                mobileType);
        return state;
    }

    /**
     * OP02 PhoneStateExt implements for SystemUI.
     */
    private class Op02PhoneStateExt extends BasePhoneStateExt {

        public Op02PhoneStateExt(int slotId, int subId) {
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

            // Slot Indicator
            mMobileSlotIndicator = new ImageView(mContext);
            if (isMultiSlot()) {
                mMobileGroup.addView(mMobileSlotIndicator, generateLayoutParams());
            }

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

        /**
         * Apply Signal strength to view.
         */
        @Override
        protected void applyMobileSignalStrength() {
            if (DEBUG) {
                Log.d(TAG, "applyMobileSignalStrength(), mSlotId = " + mSlotId
                        + ", mSubId = " + mSubId);
            }

            if (mMobileStrength != null) {
                if (!mIsSimAvailable || isSignalStrengthNullIcon()) {
                    if (DEBUG) {
                        Log.d(TAG, "No SIM inserted/Service or Signal Strength Null: " +
                                "Show empty signal icon");
                    }

                    // Signal strength null
                    setImage(mMobileStrength, mMobileStrengthNullIconId);

                    // Show signal strength icon
                    mMobileStrength.setVisibility(View.VISIBLE);

                    // Show signal icon's parent
                    if (mMobileGroup != null) {
                        mMobileGroup.setVisibility(View.VISIBLE);

                        // Show mMobileSignalGroup
                        if (mMobileSignalGroup != null) {
                            mMobileSignalGroup.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    setImage(mMobileStrength, mMobileStrengthIconId);
                }
            }
        }

        /**
         * Apply Slot Indicator to view.
         */
        @Override
        protected void applyMobileSlotIndicator() {
            if (mMobileSlotIndicator != null) {
                mMobileSlotIndicator.setPaddingRelative(
                        mIsMobileTypeIconWide ? mWideTypeIconStartPadding : 0, 0, 0, 0);
                if (isMultiSlot() && isNormalVisible() && !isSignalStrengthNullIcon()) {
                    mMobileSlotIndicator.setVisibility(View.VISIBLE);
                } else {
                    mMobileSlotIndicator.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
