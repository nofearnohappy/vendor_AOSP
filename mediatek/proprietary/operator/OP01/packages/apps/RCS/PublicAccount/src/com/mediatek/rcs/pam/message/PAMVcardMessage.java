package com.mediatek.rcs.pam.message;

import android.content.Context;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MessageContent;

import org.gsma.joyn.JoynServiceException;

public class PAMVcardMessage extends PAMFileMessage {

    private static final String TAG = "PAM/PAMAudioMessage";

    public PAMVcardMessage(Context context, long token, long accountId,
            String data, IPAMMessageHelper helper) {

        super(context, token, Constants.MEDIA_TYPE_VCARD,
                accountId, null, null, Constants.INVALID, helper);

        mData = data;
        saveMsgInDB();

        Log.d(TAG, "dump:" + dumpToString(true));
    }

    public PAMVcardMessage(Context context, long token,
            MessageContent messageContent, IPAMMessageHelper helper) {

        super(context,
                token,
                Constants.MEDIA_TYPE_VCARD,
                messageContent.accountId,
                messageContent.mediaPath,
                null,
                Constants.INVALID,
                helper);

        mMsgId = messageContent.id;
        mOriginalId = messageContent.mediaId;
        mData = messageContent.text;
    }

    private long saveMsgInDB() {

        mOriginalId = storeDataToMediaFile(mData, mType, ".vcf");
        mMsgId = storeMessage();
        return mMsgId;
    }

    @Override
    public void send() throws JoynServiceException {
        Log.d(TAG, "sendVCard(" + mFilePath + ")");
        sendInternal(null, 0);
    }

}
