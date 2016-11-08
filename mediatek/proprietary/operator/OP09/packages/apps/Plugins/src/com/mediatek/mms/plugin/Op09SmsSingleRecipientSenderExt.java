package com.mediatek.mms.plugin;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.mediatek.telephony.SmsManagerEx;
import com.mediatek.mms.ext.DefaultOpSmsSingleRecipientSenderExt;

public class Op09SmsSingleRecipientSenderExt extends DefaultOpSmsSingleRecipientSenderExt {

    private static String TAG = "Op09SmsSingleRecipientSenderExt";

    private static final String PREF_SMS_PRIORITY_KEY = "pref_key_sms_priority";
    private static final String BUNDLE_SMS_PRIORITY_KEY = "priority";

    private static final String SMS_PRIORITY_NORMAL = "Normal";
    private static final String SMS_PRIORITY_INTERACTIVE = "Interactive";
    private static final String SMS_PRIORITY_URGENT = "Urgent";
    private static final String SMS_PRIORITY_EMERGENCY = "Emergency";

    @Override
    public boolean sendMessage(Context context, int subId, String dest, String serviceCenter,
            ArrayList<String> messages, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        /// M: OP09 Feature, Send SMS with priority. @{
        if (MessageUtils.isSmsPriorityEnable()
                && Op09MmsUtils.getInstance().isCDMAType(context, subId)
                && !MessageUtils.isAllowDRWhenRoaming(context, subId)) {
            sendSMSWithPriority(context, dest, serviceCenter, messages,
                    subId, sentIntents, deliveryIntents);
            return true;
        /// @}
        }
        return false;
    }

    @Override
    public boolean sendOpMessage(Context context, int subId,
                        ArrayList<PendingIntent> deliveryIntents) {
       /// M: For OP09 feature, do not request delivery report when in international roaming status.
        if (!MessageUtils.isAllowDRWhenRoaming(context, (int) subId)) {
            Log.d(TAG, "Disable DR request when roaming!");
            deliveryIntents.add(null);
            return true;
        }
        return false;
    }

    @Override
    public int sendMessagePrepare(Context context, int codingType) {
        if (MessageUtils.isEnableSmsEncodingType()) {
            return MessageUtils.getSmsEncodingType(context);
        }
        return codingType;
    }

    private void sendSMSWithPriority(Context context, String destAddr, String scAddr,
            ArrayList<String> parts, int subId, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(context);
        String priority = spref.getString(PREF_SMS_PRIORITY_KEY, SMS_PRIORITY_NORMAL);
        Log.d(TAG, "sendSMSWithPriority(), priority: " + priority);
        Bundle extraParams = new Bundle();

        if (priority.equals(SMS_PRIORITY_NORMAL)) {
            extraParams.putInt(BUNDLE_SMS_PRIORITY_KEY, 0);
        } else if (priority.equals(SMS_PRIORITY_INTERACTIVE)) {
            extraParams.putInt(BUNDLE_SMS_PRIORITY_KEY, 1);
        } else if (priority.equals(SMS_PRIORITY_URGENT)) {
            extraParams.putInt(BUNDLE_SMS_PRIORITY_KEY, 2);
        } else if (priority.equals(SMS_PRIORITY_EMERGENCY)) {
            extraParams.putInt(BUNDLE_SMS_PRIORITY_KEY, 3);
        }
        /// M: NOTICE: the method is an old method.
        /// It may be deprecated from the message framework in the future.
        SmsManagerEx.getDefault().sendMultipartTextMessageWithExtraParams(destAddr, scAddr, parts,
            extraParams, sentIntents, deliveryIntents,
            MessageUtils.getSimInfoBySubId(context, subId).getSimSlotIndex());
    }
}
