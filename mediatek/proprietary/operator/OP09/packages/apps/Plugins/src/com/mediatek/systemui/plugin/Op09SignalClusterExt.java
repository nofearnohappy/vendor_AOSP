
package com.mediatek.systemui.plugin;

import static com.mediatek.systemui.statusbar.extcb.NetworkType.Type_1X3G;
import static com.mediatek.systemui.statusbar.extcb.NetworkType.Type_4G;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.systemui.ext.DefaultSignalClusterExt;
import com.mediatek.systemui.ext.IStatusBarPlugin;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.SvLteController;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * M: OP09 ISignalClusterExt implements for SystemUI.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.ISignalClusterExt")
public class Op09SignalClusterExt extends DefaultSignalClusterExt {
    private static final String TAG = "Op09SignalClusterExt";

    // Save the Tdd 4G data only mode.
    private boolean mTdd4GDataOnlyMode;

    /**
     * Constructs a new Op09SignalClusterExt instance.
     *
     * @param context A Context object
     * @param statusBarPlugin The interface for Plug-in definition of Status bar.
     */
    public Op09SignalClusterExt(Context context, IStatusBarPlugin statusBarPlugin) {
        super(context, statusBarPlugin);

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.LTE_ON_CDMA_RAT_MODE),
                true, mMobileDataPatternObserver);
        mTdd4GDataOnlyMode = is4GDataOnlyMode();
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

        final Op09PhoneStateExt state = new Op09PhoneStateExt(slotId, subId);
        state.setViews(signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength,
                mobileType);

        return state;
    }

    @Override
    protected BasePhoneStateExt createPhoneState(int slotId, int subId,
            ViewGroup signalClusterCombo, ImageView mobileNetworkType,
            ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
        final Op09PhoneStateExt state = new Op09PhoneStateExt(slotId, subId);
        state.setViews(signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength,
                mobileType);
        return state;
    }

    /**
     * M: OP09 BasePhoneStateExt implements.
     */
    private class Op09PhoneStateExt extends BasePhoneStateExt {

        // TELEPHONY_SIGNAL_STRENGTH_UP
        private ImageView mMobileSignalStrengthUp;
        private IconIdWrapper mMobileStrengthIconIdUp = new IconIdWrapper();

        public Op09PhoneStateExt(int slotId, int subId) {
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
            // DataType & DataActivity container
            mMobileNetworkDataGroup.setLayoutParams(generateLayoutParams());

            // 1. DataType
            if (mMobileType.getParent() != null) {
                ((ViewGroup) mMobileType.getParent()).removeView(mMobileType);
            }
            mMobileNetworkDataGroup.addView(mMobileType, generateLayoutParams());

            // 2. DataActivity
            mMobileDataActivity = new ImageView(mContext);
            mMobileNetworkDataGroup.addView(mMobileDataActivity,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER));

            // SignalStrengthUp
            if (SvLteController.isSvlteSlot(mSlotId)) {
                if (DEBUG) {
                    Log.d(TAG, "setViews, addView(mMobileSignalStrengthUp");
                }
                mMobileSignalStrengthUp = new ImageView(mContext);
                mMobileGroup.addView(mMobileSignalStrengthUp,
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.RIGHT));
            }

            // Roaming Indicator
            mMobileRoamingIndicator = new ImageView(mContext);
            mMobileGroup.addView(mMobileRoamingIndicator, generateLayoutParams());

            // Add views to SignalClusterCombo
            final int addViewIndex = mSignalClusterCombo.indexOfChild(mMobileNetworkType);
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
                if (isSignalStrengthNull()) {
                    // No SIM inserted/Service or Signal Strength Null
                    if (DEBUG) {
                        Log.d(TAG, "applyMobileSignalStrength, Show empty signal icon"
                                + ", mIsSimInserted = " + mIsSimInserted
                                + ", mHasSimService = " + mHasSimService
                                + ", mIsSimOffline = " + mIsSimOffline);
                    }

                    // signal strength null or OFFLINE
                    if (!mIsSimInserted) {
                        if (DEBUG) {
                            Log.d(TAG, "Set signal strength NULL icon.");
                        }
                        setImage(mMobileStrength, mMobileStrengthNullIconId);
                    } else if (mIsSimOffline || !mHasSimService) {
                        if (DEBUG) {
                            Log.d(TAG, "Set signal strength OFFLINE icon.");
                        }
                        setImage(mMobileStrength, mMobileStrengthOfflineIconId);
                    }

                    // Show signal icon1 and Hide signal icon2
                    if (DEBUG) {
                        Log.d(TAG, "Show signal icon1 and Hide signal icon2");
                    }
                    mMobileStrength.setVisibility(View.VISIBLE);
                    if (mMobileSignalStrengthUp != null) {
                        mMobileSignalStrengthUp.setVisibility(View.GONE);
                    }
                } else {
                    final SvLteController svLteController = mNetworkControllerExt
                            .getSvLteController(mSubId);
                    if (svLteController != null && SvLteController.isSvlteSlot(mSlotId)) {
                        final int[] iconLevelTower = {
                                0, 0
                        };
                        if (isShowSignalStrengthTower(iconLevelTower)) {
                            mMobileStrengthIconId.setResources(mContext.getResources());
                            mMobileStrengthIconId.setIconId(
                                TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_DOWN[iconLevelTower[0]]);

                            mMobileStrengthIconIdUp.setResources(mContext.getResources());
                            mMobileStrengthIconIdUp.setIconId(
                                    TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_UP[iconLevelTower[1]]);

                            setImage(mMobileStrength, mMobileStrengthIconId);
                            setImage(mMobileSignalStrengthUp, mMobileStrengthIconIdUp);

                            mMobileStrength.setVisibility(View.VISIBLE);
                            mMobileSignalStrengthUp.setVisibility(View.VISIBLE);
                        } else {
                            mMobileStrengthIconId.setResources(mContext.getResources());
                            mMobileStrengthIconId.setIconId(
                                TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_SINGLE[iconLevelTower[0]]);
                            setImage(mMobileStrength, mMobileStrengthIconId);
                            mMobileStrength.setVisibility(View.VISIBLE);
                            mMobileSignalStrengthUp.setVisibility(View.GONE);
                        }
                    }
                }

                // Show signal icon's parent
                if (mMobileGroup != null) {
                    if (mMobileGroup.getVisibility() != View.VISIBLE) {
                        mMobileGroup.setVisibility(View.VISIBLE);
                    }

                    // Show mMobileSignalGroup
                    if (mMobileSignalGroup != null) {
                        if (mMobileSignalGroup.getVisibility() != View.VISIBLE) {
                            mMobileSignalGroup.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }

        private boolean isShowSignalStrengthTower(final int[] iconLevelTower) {
            SvLteController svLteController = mNetworkControllerExt.getSvLteController(mSubId);
            final SignalStrength signalStrength = svLteController.getSignalStrength();
            final boolean showTower = !mRoaming
                    && (mNetworkType == Type_1X3G
                    || (mNetworkType == Type_4G && !svLteController.isShow4GDataOnlyForLTE()));

            if (signalStrength != null) {
                Log.d(TAG, "isShowSignalStrengthTower, slotId="
                        + mSlotId
                        + " getEvdoLevel()= "
                        + signalStrength.getEvdoLevel()
                        + " getLteLevel()= "
                        + signalStrength.getLteLevel()
                        + " getGsmLevel()= "
                        + signalStrength.getGsmLevel()
                        + " getCdmaLevel()= "
                        + signalStrength.getCdmaLevel()
                        + " getLevel() = "
                        + signalStrength.getLevel());

                if (showTower) {
                    iconLevelTower[0] = signalStrength.getCdmaLevel();
                    if (mNetworkType == Type_1X3G) {
                        iconLevelTower[1] = signalStrength.getEvdoLevel();
                    } else if (mNetworkType == Type_4G) {
                        iconLevelTower[1] = signalStrength.getLteLevel();
                    }
                } else {
                    if (mNetworkType == Type_4G) {
                        iconLevelTower[0] = signalStrength.getLteLevel();
                    } else {
                        if (SvLteController.isMediatekSVLteDcSupport()
                                && signalStrength.getGsmLevel() != 0) {
                            iconLevelTower[0] = signalStrength.getGsmLevel();
                        } else {
                            iconLevelTower[0] = signalStrength.getLevel();
                        }
                    }
                }
            }

            Log.d(TAG, "isShowSignalStrengthTower(), slotId=" + mSlotId
                    + " iconLevelTower[0]=" + iconLevelTower[0]
                    + " iconLevelTower[1]=" + iconLevelTower[1]
                    + " isShowSignalStrengthTower=" + showTower
                    + " mTdd4GDataOnlyMode=" + mTdd4GDataOnlyMode
                    + " mNetworkType=" + mNetworkType
                    + " mRoaming= " + mRoaming);

            return showTower;
        }

        @Override
        protected void applyNetworkDataSwitch() {
        }

        @Override
        protected void applyNetworkDataType() {
            if (DEBUG) {
                Log.d(TAG, "applyNetworkDataType(), slotId=" + mSlotId
                        + ", mDataConnectioned= " + mDataConnectioned);
            }

            if (isSignalStrengthNullIcon()) {
                if (DEBUG) {
                    Log.d(TAG, "applyDataNetworkType(), "
                            + "No SIM inserted/Service or Signal Strength Null or sim offline: "
                            + "Hide network type icon and data icon");
                }

                mMobileNetworkDataGroup.setVisibility(View.GONE);
                mMobileNetworkType.setVisibility(View.GONE);
                mMobileType.setVisibility(View.GONE);
            } else {
                mMobileNetworkType.setVisibility(View.VISIBLE);

                if (mDataConnectioned) {
                    if (mMobileDataTypeIconId.getIconId() <= 0) {
                        final IconIdWrapper dataTypeIcon = new IconIdWrapper();
                        dataTypeIcon.setResources(mContext.getResources());
                        dataTypeIcon.setIconId(TelephonyIcons.getDataTypeIconId(mDataType));
                        setImage(mMobileType, dataTypeIcon);
                    } else {
                        setImage(mMobileType, mMobileDataTypeIconId);
                    }
                    mMobileType.setVisibility(View.VISIBLE);
                } else {
                    mMobileType.setVisibility(View.GONE);
                }

                mMobileNetworkDataGroup.setVisibility(View.VISIBLE);
            }
            if (DEBUG) {
                Log.d(TAG, "applyDataNetworkType(), slotId=" + mSlotId
                        + ", mSignalNetworkTypesImageViews isVisible: "
                        + (mMobileNetworkType.getVisibility() == View.VISIBLE)
                        + ", mMobileDataType isVisible: "
                        + (mMobileType.getVisibility() == View.VISIBLE));
            }
        }

        @Override
        protected boolean isSignalStrengthNullIcon() {
            boolean isNullIcon = isSignalStrengthNull();
            if (DEBUG) {
                Log.d(TAG, "isSignalStrengthNullIcon, slotId=" + mSlotId
                        + " isNullIcon=" + isNullIcon);
            }
            return isNullIcon;
        }

        @Override
        protected boolean shouldShowOffline() {
            boolean showOffline = false;
            int mNeedShowOfflineSimId = -1;
            if (mSlotCount == 2 && mNetworkControllerExt.isRoamingGGMode()) {
                final SubscriptionInfo subInfo1 = SIMHelper.getSubInfoBySlot(mContext,
                        PhoneConstants.SIM_ID_1);
                final SubscriptionInfo subInfo2 = SIMHelper.getSubInfoBySlot(mContext,
                        PhoneConstants.SIM_ID_2);
                if (subInfo1 != null && subInfo2 != null) {
                    final int callState1 = TelephonyManager.getDefault().getCallState(
                            subInfo1.getSubscriptionId());
                    final int callState2 = TelephonyManager.getDefault().getCallState(
                            subInfo2.getSubscriptionId());
                    if (callState1 != TelephonyManager.CALL_STATE_IDLE
                            || callState2 != TelephonyManager.CALL_STATE_IDLE) {
                        showOffline = true;
                        if (callState1 != TelephonyManager.CALL_STATE_IDLE) {
                            mNeedShowOfflineSimId = PhoneConstants.SIM_ID_2;
                        } else {
                            mNeedShowOfflineSimId = PhoneConstants.SIM_ID_1;
                        }
                    }
                }

                Log.d(TAG, "shouldShowOffline()"
                        + ", subInfo1 = " + subInfo1 + ", subInfo2 = " + subInfo2
                        + ", showOffline = " + showOffline
                        + ", mNeedShowOfflineSimId = " + mNeedShowOfflineSimId);
            }

            return showOffline && mSlotId == mNeedShowOfflineSimId;
        }

        private final boolean isSignalStrengthNull() {
            // !mIsSimInserted || !mHasSimService || mIsSimOffline
            return !mIsSimAvailable || mIsSimOffline;
        }
    }

    private boolean is4GDataOnlyMode() {
        int svlteRatMode = Settings.Global.getInt(
                mContext.getContentResolver(),
                TelephonyManagerEx.getDefault().getCdmaRatModeKey(
                    getSubId(SvLteController.getSvlteSlot())),
                TelephonyManagerEx.SVLTE_RAT_MODE_4G);
        if (DEBUG) {
            Log.d(TAG, "is4GDataOnlyMode(), svlteRatMode = " + svlteRatMode);
        }
        return svlteRatMode == TelephonyManagerEx.SVLTE_RAT_MODE_4G_DATA_ONLY;
    }

    private final ContentObserver mMobileDataPatternObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) {
                Log.d(TAG, "LTE_ON_CDMA_RAT_MODE onChange selfChange=" + selfChange);
            }
            if (!selfChange) {
                final boolean b4GDataOnly = is4GDataOnlyMode();
                if (b4GDataOnly != mTdd4GDataOnlyMode) {
                    mTdd4GDataOnlyMode = b4GDataOnly;
                    final BasePhoneStateExt state = getState(SvLteController.getSvlteSlot());
                    if (state != null) {
                        state.apply();
                    }
                }
            }
        }
    };

}
