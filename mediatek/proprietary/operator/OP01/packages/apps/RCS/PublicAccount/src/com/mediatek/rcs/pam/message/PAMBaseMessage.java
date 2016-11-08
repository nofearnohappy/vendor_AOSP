package com.mediatek.rcs.pam.message;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.util.Utils;

import org.gsma.joyn.JoynServiceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


public abstract class PAMBaseMessage {

    private static final String TAG = "PAM/PAMBaseMessage";

    // FIXME use value from CMCC's response
    static final String SPAM_MESSAGE_REPORT_RECEIVER = "";

    public static final long RECOVER_TIMEOUT = 45 * 1000; // 45s
    public static final long RECOVER_INTERVAL = 5 * 1000; // 5s

    protected long      mMsgId = Constants.INVALID;
    protected int       mType;
    protected long      mToken;
    protected long      mAccountId;
    protected int       mSourceTable;
    protected String    mSourceId;
    protected String    mUuid;
    protected long      mStartTimeStamp;
    protected Context   mContext;
    protected int       mState;
    /* For CMCC */
    protected int       mChatType;
    protected String    mMimeType;

    IPAMMessageHelper mMessageHelper;

    @SuppressLint("UseSparseArrays")
    private static Map<Integer, Class<? extends PAMBaseMessage>> sClassMap =
            new HashMap<Integer, Class<? extends PAMBaseMessage>>();
    static {
        Log.d(TAG, "initialize createHandlerMap.");
        sClassMap.put(Constants.MEDIA_TYPE_TEXT,    PAMTextMessage.class);
        sClassMap.put(Constants.MEDIA_TYPE_AUDIO,   PAMAudioMessage.class);
        sClassMap.put(Constants.MEDIA_TYPE_VIDEO,   PAMVideoMessage.class);
        sClassMap.put(Constants.MEDIA_TYPE_PICTURE, PAMImageMessage.class);
        sClassMap.put(Constants.MEDIA_TYPE_GEOLOC,  PAMGeolocMessage.class);
        sClassMap.put(Constants.MEDIA_TYPE_VCARD,   PAMVcardMessage.class);
    }

    PAMBaseMessage(
            Context context,
            long token,
            int type,
            long accountId,
            int sourceTable,
            int chatType,
            IPAMMessageHelper helper) {

        mContext = context;
        mToken = token;
        mType = type;
        mAccountId = accountId;
        mSourceTable = sourceTable;
        mMessageHelper = helper;
        mUuid = PublicAccount.queryAccountUuid(context, accountId);
        /* For CMCC*/
        mChatType = chatType;
    }

    public static PAMBaseMessage generateMessageFromDB(
            Context context, long token, long msgId, IPAMMessageHelper helper) {

        PAMBaseMessage msg = null;
        MessageContent messageContent = MessageContent.loadFromProvider(
                msgId, context.getContentResolver());

        if (sClassMap.containsKey(messageContent.mediaType)) {
            Class<? extends PAMBaseMessage> clazz =
                    sClassMap.get(messageContent.mediaType);
            try {
                Constructor<? extends PAMBaseMessage> constructor =
                        clazz.getConstructor(new Class[]{Context.class, long.class,
                                MessageContent.class, IPAMMessageHelper.class});

                msg = constructor.newInstance(context, token, messageContent, helper);
            } catch (NoSuchMethodException | InstantiationException |
                    IllegalAccessException | IllegalArgumentException |
                    InvocationTargetException e) {
                e.printStackTrace();
                Log.d(TAG, "create new message fail for type:" + messageContent.mediaType);
            }
        }

        return msg;
    }

    public int getType() {
        return mType;
    }

    public long getToken() {
        return mToken;
    }

    public String getUuid() {
        return mUuid;
    }

    public long getMessageId() {
        return mMsgId;
    }

    public long getAccountId() {
        return mAccountId;
    }

    public int getState() {
        return mState;
    }

    public long getStartTimeStamp() {
        return mStartTimeStamp;
    }

    MessageContent generateMessageContent(int type) {

        MessageContent messageContent = new MessageContent();
        messageContent.accountId = mAccountId;
        messageContent.mediaType = type;
        messageContent.createTime = Utils.currentTimestamp();
        messageContent.timestamp = messageContent.createTime;
        messageContent.publicAccountUuid = mUuid;
        messageContent.direction = Constants.MESSAGE_DIRECTION_OUTGOING;
        messageContent.status = Constants.MESSAGE_STATUS_TO_SEND;
        messageContent.forwardable = Constants.MESSAGE_FORWARDABLE_YES;

        return messageContent;
    }

    long storeMediaBasic(MediaBasic mediaBasic) {
        ContentValues cv = new ContentValues();
        mediaBasic.storeToContentValues(cv);
        Uri uri = mContext.getContentResolver().insert(MediaBasicColumns.CONTENT_URI, cv);
        mediaBasic.id = Long.parseLong(uri.getLastPathSegment());
        return mediaBasic.id;
    }

    public void updateSourceId(String sourceId) {
        if (!sourceId.equals(mSourceId)) {
            mSourceId = sourceId;

            ContentValues cv = new ContentValues();
            cv.put(MessageColumns.SOURCE_ID, mSourceId);
            cv.put(MessageColumns.SOURCE_TABLE, mSourceTable);
            if (Constants.TABLE_FT == mSourceTable) {
                cv.put(MessageColumns.BODY, mSourceId);
            }
            int result = mContext.getContentResolver().update(
                    MessageColumns.CONTENT_URI,
                    cv,
                    MessageColumns.ID + "=?",
                    new String[]{Long.toString(mMsgId)});
            Log.d(TAG, "updateSourceId(" + mMsgId + ", " + mSourceTable
                    + ", " + mSourceId + ") returns " + result);
        }
    }

    public void updateStatus(int state) {

        if (state != mState) {
            mState = state;

            ContentValues cv = new ContentValues();
            cv.put(PAContract.MessageColumns.STATUS, state);
            mContext.getContentResolver().update(
                    PAContract.MessageColumns.CONTENT_URI,
                    cv,
                    PAContract.MessageColumns.SOURCE_ID + "=? AND "
                    + PAContract.MessageColumns.SOURCE_TABLE + "=?",
                    new String[]{mSourceId, Integer.toString(mSourceTable)});
        }
    }

    public abstract boolean readyForSend() throws JoynServiceException;

    public abstract void send() throws JoynServiceException;
    public abstract void resend() throws JoynServiceException;

    public abstract void onSendOver();

    public boolean shouldRetry() {
        boolean shouldRetry = false;
        if (mStartTimeStamp == Constants.INVALID) {
            mStartTimeStamp = System.currentTimeMillis();
            shouldRetry = true;
        } else {
            final long currentTimestamp = System.currentTimeMillis();
            long timespan = currentTimestamp - mStartTimeStamp;
            if (timespan > 0 && timespan < RECOVER_TIMEOUT) {
                shouldRetry = true;
            }
        }
        Log.d(TAG, "shouldRetry() is " + shouldRetry);
        return shouldRetry;
    }

    String dumpToString(boolean isFull) {
        StringBuffer sb = new StringBuffer(
                "mToken="     + mToken + ", " +
                "mAccountId=" + mAccountId + ", " +
                "mMessageHelper=" + mMessageHelper + ", ");
        if (isFull) {
            sb.append("mMsgId="     + mMsgId + ", " +
                      "mType="      + mType + ", " +
                      "mSourceId="  + mSourceId + ", " +
                      "mUuid="      + mUuid + ", " +
                      "mStartTimeStamp=" + mStartTimeStamp + ", ");
        }
        return sb.toString();
    }

    public abstract void complain() throws JoynServiceException;

    public int setFavorite(int indx) {
        return ResultCode.SUCCESS;
    }
}
