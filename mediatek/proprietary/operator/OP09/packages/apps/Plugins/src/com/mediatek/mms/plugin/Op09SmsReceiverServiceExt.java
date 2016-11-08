package com.mediatek.mms.plugin;

import com.mediatek.common.sms.IConcatenatedSmsFwkExt;
import com.mediatek.mms.callback.ISmsReceiverServiceCallback;
import com.mediatek.mms.ext.DefaultOpSmsReceiverServiceExt;

import android.net.Uri;
import android.telephony.SmsMessage;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;

public class Op09SmsReceiverServiceExt extends DefaultOpSmsReceiverServiceExt {
    /**
     * Updated segments to dispatch flag type.
     *
     * @internal
     */
    public static final int UPLOAD_FLAG_UPDATE = 2;

    private static String TAG = "Op09SmsReceiverServiceExt";

    /**
     * Updated segments tag to put on the intent extra value.
     *
     * @internal
     */
    public static final String UPLOAD_FLAG_TAG = "upload_flag";
    /**
     * New segments to dispatch flag type.
     *
     * @internal
     */
    public static final int UPLOAD_FLAG_NEW = 1;
    private ISmsReceiverServiceCallback mCallback;

    public Op09SmsReceiverServiceExt(Context base) {
        super(base);
        // TODO Auto-generated constructor stub
    }
    @Override
    public void onCreate(Service service, ISmsReceiverServiceCallback callback) {
        mCallback = callback;
    }

    @Override
    public boolean storeMessage(SmsMessage[] msgs, SmsMessage sms, ContentValues values) {
        Op09SmsReceiverExt.getIntance(this).extractSmsBody(msgs, sms, values);
        return true;
    }

    @Override
    public Uri handleSmsReceived(Context context, SmsMessage[] msgs, Intent intent, int error) {
        Uri messageUri = null;
        /// M: For OP09 Storage Low @{
        if (MessageUtils.isLowMemoryNotifEnable()) {
            MessageUtils.dealCTDeviceLowNotification(context);
        }
        /// M: @}

        /// M: OP09 Feature, receive long SMS. @{
        int uploadFlag = UPLOAD_FLAG_NEW;
        if (MessageUtils.isMissedSmsReceiverEnable()) {
            uploadFlag = intent.getIntExtra(UPLOAD_FLAG_TAG,
                UPLOAD_FLAG_NEW);
            Log.d(TAG, "UPLOAD_FLAG_TAG: " + uploadFlag);
        }
        /// @}

        /// M: For OP09 Feature, receive missing part of concatenated Sms. @{
        if (msgs != null && msgs[0].getMessageClass() != SmsMessage.MessageClass.CLASS_0
            && !msgs[0].isReplace() && MessageUtils.isMissedSmsReceiverEnable()
            && uploadFlag == UPLOAD_FLAG_UPDATE) {
            messageUri = Op09MissedSmsReceiverExt.getIntance(context).updateMissedSms(
                    this, msgs, error, mCallback);
        /// @}
        }
        return messageUri;
        /// @}
    }

    @Override
    public Intent displayClassZeroMessage(Intent intent) {
        /// M: add for OP09 @{
        if (MessageUtils.isClassZeroModelShowLatestEnable()) {
            return Op09DisplayClassZeroMessageExt.getIntance(getApplicationContext())
                                                 .setLaunchMode(intent);
        }
        /// @}
        return intent;
    }

}
















