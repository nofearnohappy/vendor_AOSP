package com.mediatek.rcs.pam.message;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.model.MessageContent;

import org.gsma.joyn.JoynServiceException;


public class PAMAudioMessage extends PAMFileMessage {

    private static final String TAG = "PAM/PAMAudioMessage";

    public PAMAudioMessage(Context context, long token, long accountId,
            String path, int duration, IPAMMessageHelper helper) {

        super(context, token, Constants.MEDIA_TYPE_AUDIO,
                accountId, path, null, duration, helper);

        saveMsgInDB();

        Log.d(TAG, "dump:" + dumpToString(true));

    }

    public PAMAudioMessage(Context context, long token,
            MessageContent messageContent, IPAMMessageHelper helper) {

        super(context,
                token,
                Constants.MEDIA_TYPE_AUDIO,
                messageContent.accountId,
                messageContent.basicMedia.originalPath,
                null,
                Integer.parseInt(messageContent.basicMedia.duration),
                helper);

        mMsgId = messageContent.id;
        mOriginalId = messageContent.basicMedia.originalId;
    }

    private long saveMsgInDB() {

        Pair<Long, String> mediaResult = copyAndStoreMediaFile(mFilePath, mType);
        mOriginalId = mediaResult.first;
        mFilePath = mediaResult.second;

        Pair<Long, MediaBasic> mediaBasicResult = storeMediaBasic();
        mMediaId = mediaBasicResult.first;
        MediaBasic mb = mediaBasicResult.second;
        mMsgId = storeMessage(mb);

        return mMsgId;
    }

    @Override
    public void send() throws JoynServiceException {
        Log.d(TAG, "sendAudio(" + mFilePath + ")");
        sendInternal(null, mDuration);
    }

}
