package com.mediatek.mms.plugin;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import com.mediatek.mms.ext.DefaultOpSmsSingleRecipientSenderExt;

public class Op01SmsSingleRecipientSenderExt extends DefaultOpSmsSingleRecipientSenderExt {

    private static final String TAG = "Mms/Op01SmsSingleRecipientSenderExt";
    private static final boolean MTK_GEMINI_SUPPORT =
            SystemProperties.get("ro.mtk_gemini_support").equals("1");

    @Override
    public boolean sendMessage(Context context, int subId, String dest, String serviceCenter,
            ArrayList<String> messages, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        Bundle extra = getSmsValidityParamBundleWhenSend(context, subId);
        if (extra != null) {
            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
            smsManager.sendMultipartTextMessageWithExtraParams(dest, serviceCenter, messages,
                    extra, sentIntents, deliveryIntents);
            return true;
        }
        return false;
    }

    /**
     * getSmsValidityParamBundleWhenSend.
     * @param context Context
     * @param subId subId
     * @return Bundle
     */
    private Bundle getSmsValidityParamBundleWhenSend(Context context, int subId) {
        Log.d(TAG, "getSmsValidityParamBundleWhenSend, subId = " + subId);
        if (MTK_GEMINI_SUPPORT) {
            Bundle extra = new Bundle();
//            int slotid = SubscriptionManager.getSlotId(subId);
            int valid = getSmsValiditybySubId(context, subId);
//            extra.putInt(SmsManager.EXTRA_PARAMS_VALIDITY_PERIOD, valid);
            extra.putInt("validity_period", valid);
            return extra;
        } else {
            return null;
        }
    }

    private int getSmsValiditybySubId(Context context, int subId) {

        final int[] validities = {SmsManager.VALIDITY_PERIOD_NO_DURATION,
                                    SmsManager.VALIDITY_PERIOD_ONE_HOUR,
                                    SmsManager.VALIDITY_PERIOD_SIX_HOURS,
                                    SmsManager.VALIDITY_PERIOD_TWELVE_HOURS,
                                    SmsManager.VALIDITY_PERIOD_ONE_DAY,
                                    SmsManager.VALIDITY_PERIOD_MAX_DURATION};

//        final int[] validities = {-1,
//                                    11,
//                                    71,
//                                    143,
//                                    167,
//                                    255};
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(context);
        final String validityKey = getSmsValidityKeyBySubId(subId);
        int index = spref.getInt(validityKey, 0);
        if (index > validities.length) {
            index = 0;
        }
        Log.d(TAG, "getSmsValiditybysubId: result index = " + index +
                                                ", validity =" + validities[index]);
        return validities[index];
    }

    private String getSmsValidityKeyBySubId(int subId) {
        return Integer.toString(subId) + "_" + Op01MmsPreference.SMS_VALIDITY_PERIOD_PREFERENCE_KEY;
    }
}