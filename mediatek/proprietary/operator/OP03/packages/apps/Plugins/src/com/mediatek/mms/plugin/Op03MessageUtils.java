package com.mediatek.mms.plugin;

import com.mediatek.mms.ext.IOpSmsPreferenceActivityExt;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;

public class Op03MessageUtils {

    public static int getSmsEncodingType(Context context) {
        int type = SmsMessage.ENCODING_UNKNOWN;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String encodingType = null;
        encodingType = prefs.getString(IOpSmsPreferenceActivityExt.SMS_INPUT_MODE, "Automatic");

        if ("Unicode".equals(encodingType)) {
            type = SmsMessage.ENCODING_16BIT;
        } else if ("GSM alphabet".equals(encodingType)) {
            type = SmsMessage.ENCODING_7BIT;
        }
        return type;
    }
}
