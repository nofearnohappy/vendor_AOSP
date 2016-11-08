package com.mediatek.keyguard.plugin;

import android.content.Context;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.common.PluginImpl;
import com.mediatek.keyguard.ext.DefaultCarrierTextExt;
import com.mediatek.op09.plugin.R;

/**
 * Customize the carrier text for OP09.
 */
@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.ICarrierTextExt")
public class OP09CarrierTextExt extends DefaultCarrierTextExt {
    private static final String TAG = "OP09CarrierTextExt";

    private static final String CT_PLMN = "china telecom";

    private Context mContext;

    /**
     * The constructor and to save the plugin's context for resource access.
     *
     * @param context the context of plugin.
     */
    public OP09CarrierTextExt(Context context) {
        super();
        mContext = context;
    }

    @Override
    public CharSequence customizeCarrierTextCapital(CharSequence carrierText) {
        if (carrierText.toString().equalsIgnoreCase(CT_PLMN)) {
            return carrierText;
        } else {
            return carrierText.toString().toUpperCase();
        }
    }

    /**
     * For CT, display "No SERVICE" when CDMA card type is locked.
     *
     * @param carrierText
     *          the carrier text before customize.
     *
     * @param context
     *          the context of the application.
     *
     * @param phoneId
     *          the phone ID of the customized carrier text.
     *
     * @param isCardLocked
     *          whether is the card is locked.
     *
     * @return the right carrier text when card is locked.
     */
    @Override
    public CharSequence customizeCarrierTextWhenCardTypeLocked(
            CharSequence carrierText, Context context, int phoneId, boolean isCardLocked) {
        Log.d(TAG, "customizeCarrierTextWhenCardTypeLocked, phoneId = " + phoneId
                + " isCardLocked = " + isCardLocked);
        if (isCardLocked && phoneId == PhoneConstants.SIM_ID_1) {
            Log.d(TAG, "customizeCarrierTextWhenCardTypeLocked, using locktext");
            return context.getResources().
                    getText(com.android.internal.R.string.lockscreen_carrier_default);
        }
        return super.customizeCarrierTextWhenCardTypeLocked(
                        carrierText, context, phoneId, isCardLocked);
    }

    @Override
    public boolean showCarrierTextWhenSimMissing(boolean isSimMissing, int simId) {
        Log.d(TAG, "showCarrierTextWhenSimMissing, simId=" + simId
                + " isSimMissing=" + isSimMissing
                + " return false");
        return false;
    }

    /**
     * The customized carrier text when SIM is missing.
     *
     * @param carrierText the current carrier text string.
     *
     * @return the customized the carrier text.
     */
    @Override
    public CharSequence customizeCarrierTextWhenSimMissing(CharSequence carrierText) {
        return mContext.getResources().
                    getText(R.string.lockscreen_missing_sim_message_short);
    }

    /**
     * The customized divider of carrier text.
     *
     * @param divider the current carrier text divider string.
     *
     * @return the customized carrier text divider string.
     */
    @Override
    public String customizeCarrierTextDivider(String divider) {
        String carrierDivider = " | ";
        return carrierDivider;
    }
}
