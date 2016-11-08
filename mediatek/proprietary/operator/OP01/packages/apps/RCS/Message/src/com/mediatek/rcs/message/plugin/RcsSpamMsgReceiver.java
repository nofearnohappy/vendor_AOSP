package com.mediatek.rcs.message.plugin;

import static android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.WapPush;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.mms.ipmessage.DefaultIpSpamMsgReceiverExt;
import com.mediatek.rcs.common.provider.SpamMsgUtils;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.android.internal.telephony.PhoneConstants;

/**
 * Plugin implements. response SpamMsgReceiver.java in MMS host.
 *
 */
public class RcsSpamMsgReceiver extends DefaultIpSpamMsgReceiverExt {
    private static final String TAG = "RcsSpamMsgReceiver";
    private Context mContext;

//    private static final String BLACK_LIST_URI = "content://com.cmcc.ccs.black_list/black_list";
//    private static final String[] BLACK_LIST_PROJECTION = {
//        "PHONE_NUMBER"
//    };

   /*
      * check if is spam ipmessage, if yes return true, if no return false
      */
    @Override
    public boolean onIpReceiveSpamMsg(Context context, Intent intent, boolean isMmsPush) {
        Log.d(TAG, "onIpReceiveSpamMsg. intent = " + intent + ", isMmsPush = " + isMmsPush);
        mContext = context;

        if (intent != null) {
            // sms
            if (!isMmsPush) {
                String action = intent.getAction();
                if (SMS_DELIVER_ACTION.equals(action)) {
                    return onIphandleSmsSpamMsg(context, intent);
                }
            } else { // mms push
                return onIphandlePushSpamMsg(context, intent);
            }
        }
        return false;
    }

    private boolean onIphandleSmsSpamMsg(Context context, Intent intent) {
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        if (msgs == null) {
            return false;
        }
        SmsMessage sms = msgs[0];
        String address = sms.getDisplayOriginatingAddress();

        if (!TextUtils.isEmpty(address) && RCSUtils.isIpSpamMessage(mContext, address)) {
            Log.d(TAG, "received spam sms message, intent="+ intent);
            String body = sms.getMessageBody();
            long subId = sms.getSubId();

            // store the message into ipmsg db
            SpamMsgUtils spamUtils = SpamMsgUtils.getInstance(context);
            Uri spamUri = spamUtils.insertSpamSms(body, address, subId);
            if (spamUri != null) {
                // nothing to do
            }
            return true;
        }
        return false;
    }

    private boolean onIphandlePushSpamMsg(Context context, Intent intent) {
        byte[] pushData = intent.getByteArrayExtra("data");
        long subId = intent.getLongExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        PduParser parser = new PduParser(pushData, false);
        GenericPdu pdu = parser.parse();
        int type = pdu.getMessageType();
        EncodedStringValue from = pdu.getFrom();
        String contact = "";

        if (from != null) {
            contact = from.getString();
        }
        Log.d(TAG, "contact = " + contact + ", type = " + type);

        if ((type == MESSAGE_TYPE_NOTIFICATION_IND) &&
                                RCSUtils.isIpSpamMessage(mContext, contact)) {
            Log.d(TAG, "received spam push message, intent="+ intent);
            SpamMsgUtils spamUtils = SpamMsgUtils.getInstance(context);

            Uri spamUri = spamUtils.insertSpamMmsPush(pushData, contact, subId);
            if (spamUri != null) {
                //
            }
            return true;
        }
        return false;
    }

//   /*
//    * TODO:
//    * If is spam message, return true, else return false.
//    */
//    private boolean isIpSpamMessage(String number) {
//        Cursor cursor = mContext.getContentResolver().query(Uri.parse(BLACK_LIST_URI),
//                BLACK_LIST_PROJECTION, null, null, null);
//        if (cursor == null) {
//            Log.d(TAG, "isIpSpamMessage, cursor is null...");
//            return false;
//        }
//        String blockNumber;
//        boolean result = false;
//        cursor.moveToFirst();
//        while (!cursor.isAfterLast()) {
//            blockNumber = cursor.getString(0);
//            if (PhoneNumberUtils.compare(number, blockNumber)) {
//                result = true;
//                break;
//            }
//            cursor.moveToNext();
//        }
//        cursor.close();
//        Log.d(TAG, "isIpSpamMessage, number=" + number + ", result=" + result);
//        return result;
//    }
//
}