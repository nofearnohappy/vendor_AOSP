package com.mediatek.rcs.pam.message;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.PublicAccountChat;

import java.lang.ref.WeakReference;

public class PAMTextMessage extends PAMBaseMessage {
    private static final String TAG = "PAM/PAMTextMessage";

    private boolean mIsSystem;
    private WeakReference<String> mMessageRef;


    public PAMTextMessage(Context context, long token, long accountId,
            boolean system, String msg, IPAMMessageHelper helper) {

        super(context,
                token,
                Constants.MEDIA_TYPE_TEXT,
                accountId,
                Constants.TABLE_MESSAGE,
                com.cmcc.ccs.chat.ChatService.IM,
                helper);

        mIsSystem = system;
        mMessageRef = new WeakReference<String>(msg);
        mMimeType = "text/plain";
        saveMsgInDB(msg);

        Log.d(TAG, "dump:" + dumpToString(true));
    }

    public PAMTextMessage(Context context, long token,
            MessageContent messageContent, IPAMMessageHelper helper) {

        super(context,
                token,
                Constants.MEDIA_TYPE_TEXT,
                messageContent.accountId,
                Constants.TABLE_MESSAGE,
                com.cmcc.ccs.chat.ChatService.IM,
                helper);

        mIsSystem = messageContent.system >= 1 ? true : false;
        mMsgId = messageContent.id;
        mMessageRef = new WeakReference<String>(messageContent.text);
    }


    private long saveMsgInDB(String msg) {

        MessageContent messageContent = generateMessageContent(mType);
        messageContent.sourceTable = Constants.TABLE_MESSAGE;
        messageContent.text = msg;
        messageContent.system = mIsSystem ? 1 : 0;
        messageContent.generateSmsDigest(mContext);
        /* for cmcc provider*/
        messageContent.chatType = mChatType;
        messageContent.body = messageContent.text;
        messageContent.mimeType = mMimeType;

        mMsgId = messageContent.storeToProvider(mContext.getContentResolver(), mContext);
        Log.d(TAG, "generateMessage() id=" + mMsgId);
        return mMsgId;
    }

    private String sendInternal(PublicAccountChat chat, String msg) throws JoynServiceException {
        String sourceId;
        if (msg.length() <= Constants.LARGE_MESSAGE_THRESHOLD) {
            Log.d(TAG, "Send in Pager Mode Start");
            sourceId = chat.sendPublicAccountMessageByPagerMode(msg);
            Log.d(TAG, "Send in Pager Mode End. sourceId=" + sourceId);
        } else {
            Log.d(TAG, "Send in Large Mode Start");
            sourceId = chat.sendPublicAccountMessageByLargeMode(msg);
            Log.d(TAG, "Send in Pager Mode End. sourceId=" + sourceId);
        }
        return sourceId;
    }

    @Override
    public boolean readyForSend() throws JoynServiceException {
        boolean ret = false;

        ChatService service = mMessageHelper.getChatService();
        if (service != null &&
            service.isServiceConnected() &&
            service.isServiceRegistered()) {
            ret = true;
        }

        return ret;
    }

    private PublicAccountChat getChat() {
        PublicAccountChat chat = mMessageHelper.getChatCache(mToken, mUuid);
        if (null == chat) {
            chat = createNewChat();
            if (null != chat) {
                mMessageHelper.updateChatCache(mToken, mUuid, chat);
            } else {
                Log.e(TAG, "failed to create PublicAccountChat");
            }
        }
        return chat;
    }

    @Override
    public void send() throws JoynServiceException {
        String msg = getMessage();
        if (TextUtils.isEmpty(msg)) {
            Log.d(TAG, "fail to send due to empty text message!");
            return;
        }

        PublicAccountChat chat = getChat();
        if (null != chat) {
            String newSourceId = null;
            try {
                newSourceId = sendInternal(chat, msg);
            } catch (JoynServiceException e) {
                chat = createNewChat();
                mMessageHelper.updateChatCache(mToken, mUuid, chat);
                newSourceId = sendInternal(chat, msg);
            }
            if (!TextUtils.isEmpty(newSourceId)) {
                updateSourceId(newSourceId);
                updateStatus(Constants.MESSAGE_STATUS_SENDING);
            }
        }
    }

    @Override
    public void resend() throws JoynServiceException {

        if (null != mSourceId) {
            PublicAccountChat chat = getChat();
            if (null != chat) {
                chat.resendMessage(mSourceId);
            }
        } else {
            send();
        }
    }

    @Override
    public void onSendOver() {
        //Do nothing
    }

    public boolean getIsSystem() {
        return mIsSystem;
    }

    @Override
    String dumpToString(boolean isFull) {
        StringBuffer sb = new StringBuffer("mIsSystem=" + mIsSystem + ", ");
        sb.append(super.dumpToString(isFull));
        if (isFull) {
            sb.append("message=" + mMessageRef.get());
        }
        return sb.toString();
    }

    private String getMessage() {
        String msg = mMessageRef.get();
        if (null == msg) {
            msg = queryMessageFromDB();
        }
        return msg;
    }

    private PublicAccountChat createNewChat() {
        Log.d(TAG, "createNewChat()");
        PublicAccountChat chat = null;
        try {
            chat = mMessageHelper.getChatService().initPublicAccountChat(
                        Constants.SIP_PREFIX + mUuid,
                        mMessageHelper.getChatServiceListener(mMsgId));
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
        return chat;
    }

    private String queryMessageFromDB() {
        String text = null;
        Cursor c = null;

        Uri uri = MessageColumns.CONTENT_URI
                    .buildUpon()
                    .appendQueryParameter(
                            PAContract.MESSAGES_PARAM_INCLUDING_SYSTEM,
                            Integer.toString(Constants.IS_SYSTEM_YES))
                    .build();

        c = mContext.getContentResolver().query(
                    uri,
                    new String[] {MessageColumns.TEXT},
                    MessageColumns.ID + "=?",
                    new String[] {Long.toString(mMsgId)},
                    null);
        if (null != c) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                text = c.getString(c.getColumnIndexOrThrow(MessageColumns.TEXT));
            }
            c.close();
        }
        return text;
    }

    @Override
    public void complain() throws JoynServiceException {
        mMessageHelper.getChatService().addSpamReportListener(
                mMessageHelper.getSpamReportListener(mToken, mSourceId, mMsgId));
        mMessageHelper.getChatService().initiateSpamReport(
                SPAM_MESSAGE_REPORT_RECEIVER, mSourceId);
    }

}
