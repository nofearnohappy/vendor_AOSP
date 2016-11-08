package com.mediatek.phone.plugin;

import android.content.Context;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;

import com.mediatek.common.PluginImpl;
import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;


@PluginImpl(interfaceName="com.mediatek.phone.ext.ICallFeaturesSettingExt")
public class OP06CallFeaturesSettingExt extends DefaultCallFeaturesSettingExt {
    private static final String TAG = "OP06CallFeaturesSettingExt";
    Context mContext;

    public OP06CallFeaturesSettingExt(Context context) {
        Log.d(TAG, "OP06 Constructor call");
        mContext = context;
    }

   /**
    * Get whether the IMS is IN_SERVICE.
    * @param subId the sub which one user selected.
    * @return true if the ImsPhone is IN_SERVICE, else false.
    */
    private static boolean isImsServiceAvailable(Context context, int subId) {
        boolean isImsReg = false;
        boolean isImsEnabled = ImsManager.isVolteEnabledByPlatform(context);
        Log.d(TAG, "[isImsServiceAvailable] isImsEnabled : " + isImsEnabled);
        if (isImsEnabled) {
            isImsReg = ((TelephonyManager)context
                    .getSystemService(Context.TELEPHONY_SERVICE)).isVolteEnabled();
            Log.d(TAG, "[isImsServiceAvailable] isImsReg = " + isImsReg);
        }
        return isImsReg;
    }

    @Override
   /**
     * Get whether the IMS is IN_SERVICE.
     * @param subId the sub which one user selected.
     * @return true if the ImsPhone is IN_SERVICE, else false.
     */
   public boolean needShowOpenMobileDataDialog(Context context, int subId) {
        Log.d(TAG, "ImsService Not Available, plugin returns true else false");
        return !isImsServiceAvailable(context, subId);
    }

    @Override
    /**
      * Keep the preference enabled after the preference is SET
      * @param subId the sub which one user selected.
      */
    public void onError(Preference preference){
        preference.setEnabled(true);
        Log.d(TAG, "onError, set preference true even after error");
    }
}
