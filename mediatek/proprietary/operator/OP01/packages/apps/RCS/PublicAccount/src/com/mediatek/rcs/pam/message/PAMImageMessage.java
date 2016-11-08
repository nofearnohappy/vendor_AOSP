package com.mediatek.rcs.pam.message;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.model.MessageContent;

import org.gsma.joyn.JoynServiceException;

public class PAMImageMessage extends PAMFileMessage {

    private static final String TAG = "PAM/PAMImageMessage";

    public PAMImageMessage(Context context, long token, long accountId,
            String path, String thumbPath, IPAMMessageHelper helper) {

        super(context, token, Constants.MEDIA_TYPE_PICTURE,
                accountId, path, null, Constants.INVALID, helper);

        saveMsgInDB();

        Log.d(TAG, "dump:" + dumpToString(true));

    }

    public PAMImageMessage(Context context, long token,
            MessageContent messageContent, IPAMMessageHelper helper) {

        super(context,
                token,
                Constants.MEDIA_TYPE_PICTURE,
                messageContent.accountId,
                messageContent.basicMedia.originalPath,
                null,
                Constants.INVALID,
                helper);

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
        Log.d(TAG, "sendImage(" + mFilePath + ")");
        sendInternal(null, 0);
    }

}
