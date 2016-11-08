package com.mediatek.systemui.plugin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.ext.DefaultStatusBarPlmnPlugin;

/**
 * M: OP09 IStatusBarPlmnPlugin implements for SystemUI.
 */
@PluginImpl(interfaceName = "com.mediatek.systemui.ext.IStatusBarPlmnPlugin")
public class Op09StatusBarPlmnPlugin extends DefaultStatusBarPlmnPlugin {
    private static final String TAG = "Op09StatusBarPlmnPlugin";
    private static final boolean DEBUG = true;

    private TextView mPlmnTextView = null;
    private String mPlmn = null;
    private Context mContext = null;

    private Op09CarrierText mCarrierText;
    private boolean mVisible = false;

    private boolean mIsLockedCard = false;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    /**
     * Constructs a new Op09CarrierTextPlugin instance with Context.
     * @param context A Context object.
     */
    public Op09StatusBarPlmnPlugin(Context context) {
        super(context);
        this.mContext = context;

        final IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_CDMA_CARD_TYPE);
        final Intent intent = mContext.registerReceiver(mReceiver, intentFilter);
        updateCardType(intent);
    }

    @Override
    public boolean supportCustomizeCarrierLabel() {
        return true;
    }

    @Override
    public View customizeCarrierLabel(ViewGroup parentView, View orgCarrierLabel) {
        Log.d(TAG, "customizeCarrierLabel, orgCarrierLabel=" + orgCarrierLabel);

        if (mCarrierText == null) {
            mCarrierText = new Op09CarrierText(mContext);
        }

        if (parentView.indexOfChild(mCarrierText.mCarrierLayout) < 0) {
            Log.d(TAG, "customizeCarrierLabel, set the org to gone.");
            if (orgCarrierLabel != null) {
                orgCarrierLabel.setVisibility(View.GONE);
            }

            int index = parentView.indexOfChild(orgCarrierLabel);
            if (index < 0) {
                index = 0;
            }
            parentView.addView(mCarrierText.mCarrierLayout, index);
        }

        return mCarrierText.mCarrierLayout;
    }

    @Override
    public void updateCarrierLabelVisibility(boolean force, boolean makeVisible) {
        if (DEBUG) {
            Log.d(TAG, "updateCarrierLabelVisibility(), force = " + force
                    + "making carrier label " + (makeVisible ? "visible" : "invisible"));
        }

        if (force || mVisible != makeVisible) {
            mVisible = makeVisible;
            final View mCustomizeCarrierLabel = mCarrierText.mCarrierLayout;
            mCustomizeCarrierLabel.animate().cancel();
            if (makeVisible) {
                mCustomizeCarrierLabel.setVisibility(View.VISIBLE);
            }
            mCustomizeCarrierLabel.animate()
                    .alpha(makeVisible ? 1f : 0f)
                    .setDuration(150)
                    .setListener(makeVisible ? null : new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!mVisible) { // race
                                mCustomizeCarrierLabel.setVisibility(View.INVISIBLE);
                                mCustomizeCarrierLabel.setAlpha(0f);
                            }
                        }
                    })
                    .start();
        }
    }

    @Override
    public void updateCarrierLabel(int slotId, boolean isSimInserted, boolean isHasSimService,
            String[] networkNames) {
        if (DEBUG) {
            Log.d(TAG, "into updateCarrierLabel, slotId=" + slotId
                    + ", isSimInserted=" + isSimInserted
                    + ", isHasSimService=" + isHasSimService
                    + ", mIsLockedCard=" + mIsLockedCard
                    + ", mCarrierText == null " + (mCarrierText == null));
            for (int i = 0; i < networkNames.length; i++) {
                Log.d(TAG, "into updateCarrierLabel, networkNames[" + i + "] : "
                        + networkNames[i]);
            }
        }

        updateCarrierTextOnUiThread(slotId, isSimInserted, isHasSimService,
                mIsLockedCard, networkNames);
    }

    public void updateCarrierTextOnUiThread(final int slotId, final boolean isSimInserted,
            final boolean isHasSimService, final boolean isLockedCard,
            final String[] networkNames) {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCarrierText != null) {
                    mCarrierText.updateCarrierText(slotId, isSimInserted, isHasSimService,
                            isLockedCard, networkNames);
                }
            }
        });
    }

    private final void updateCardType(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_CDMA_CARD_TYPE.equals(action)) {
                final CardType cardType = (CardType)
                        intent.getExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);

                mIsLockedCard = CardType.LOCKED_CARD == cardType;
                if (DEBUG) {
                    Log.d(TAG, "updateCardType(), intent cardType = " + cardType
                            + ", mIsLockedCard = " + mIsLockedCard);
                }
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                Log.d(TAG, "onReceive(), intent = " + intent.getAction());
            }
            updateCardType(intent);
        }
    };
};
