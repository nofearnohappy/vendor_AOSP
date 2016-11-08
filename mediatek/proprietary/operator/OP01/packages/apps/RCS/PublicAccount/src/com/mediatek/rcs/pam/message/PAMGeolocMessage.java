package com.mediatek.rcs.pam.message;

import android.content.Context;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MessageContent;

import org.gsma.joyn.JoynServiceException;


public class PAMGeolocMessage extends PAMFileMessage {

    private static final String TAG = "PAM/PAMGeolocMessage";

    public PAMGeolocMessage(Context context, long token, long accountId,
            String data, IPAMMessageHelper helper) {

        super(context, token, Constants.MEDIA_TYPE_GEOLOC,
                accountId, null, null, Constants.INVALID, helper);

        mData = data;
        saveMsgInDB();

        Log.d(TAG, "dump:" + dumpToString(true));
    }

    public PAMGeolocMessage(Context context, long token,
            MessageContent messageContent, IPAMMessageHelper helper) {

        super(context,
                token,
                Constants.MEDIA_TYPE_GEOLOC,
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

        mOriginalId = storeDataToMediaFile(mData, mType, ".xml");
        mMsgId = storeMessage();

        return mMsgId;
    }

    @Override
    public void send() throws JoynServiceException {
        Log.d(TAG, "sendGeoloc(" + mFilePath + ")");
        sendInternal(null, 0);
    }

}
