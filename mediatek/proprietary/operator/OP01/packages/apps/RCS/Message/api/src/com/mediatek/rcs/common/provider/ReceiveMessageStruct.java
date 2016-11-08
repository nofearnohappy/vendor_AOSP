package com.mediatek.rcs.common.provider;

import java.util.HashSet;
import java.util.Set;

import org.gsma.joyn.chat.ChatLog;

import android.content.Context;
import android.text.TextUtils;

import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RcsLog.ThreadFlag;
import com.mediatek.rcs.common.utils.RCSUtils;

public class ReceiveMessageStruct extends MessageStruct {

    public ReceiveMessageStruct(Context context) {
        mContext = context;
        mBody = null;
        mDirection = Direction.INCOMING;
        mTimestamp = System.currentTimeMillis();
        mSeen = 0;
        mMessageStatus = MessageStatus.UNREAD;
        mSubId = RCSUtils.getRCSSubId();
        mIsBlocked = 0;
        mIpmsgId = Integer.MAX_VALUE;
        mLocked = 0;
        mClass = Class.NORMAL;
        mType = MessageType.IM;
        mFlag = ThreadFlag.OTO;
        mDateSent = 0;
        mConversationId = 0;
        mContact = null;
        mChatId = null;
        mMessageId = null;
        mMimeType = null;
        mContactSet = null;
        mFilePath = null;
    }

    /**
     * constructor for normol IM
     * @param context
     * @param contact
     * @param content
     */
    private ReceiveMessageStruct(Context context, String contact, String content) {
        this(context);
        mBody = content;
        mContact = contact;
        Set<String> contacts = new HashSet<String>();
        if (!TextUtils.isEmpty(contact)) {
            contacts.add(contact);
        }
        mContactSet = contacts;
    }

    /**
     * constructor for burn IM and emoticon/cloud
     * @param context
     * @param contact
     * @param content
     * @param msgId
     * @param ipmsgId
     * @param msgType
     */
    public ReceiveMessageStruct(Context context, String contact, String content,
            String msgId, long ipmsgId, int msgType) {
        this(context, contact, content);
        if (ipmsgId > 0) {
            mIpmsgId = ipmsgId;
        } else {
            mIpmsgId = RCSDataBaseUtils.getStackMessageId(context, msgId,
                    ChatLog.Message.Direction.INCOMING);
        }
        mMessageId = msgId;
        mFlag = ThreadFlag.OTO;
        mClass = msgType;
    }

    /**
     * contructor for group IM
     * @param context
     * @param chatId
     * @param contact
     * @param content
     * @param msgId
     * @param ipmsgId
     * @param msgType
     */
    public ReceiveMessageStruct(Context context, String chatId, String contact, String content,
            String msgId, long ipmsgId, int msgType) {
        this(context, contact, content, msgId, ipmsgId, msgType);
        mFlag = ThreadFlag.MTM;
        mChatId = chatId;
        if (msgType >= Class.SYSTEM) {
            mMessageStatus = MessageStatus.READ;
        }
    }

    /**
     * constructor for file transfer
     * @param contact
     * @param context
     * @param filePath
     * @param msgId
     * @param ipmsgId
     * @param msgType
     */
    public ReceiveMessageStruct(String contact, Context context, String filePath,
            String msgId, long ipmsgId, int msgType) {
        this(context);
        mContact = contact;
        Set<String> contacts = new HashSet<String>();
        if (!TextUtils.isEmpty(contact)) {
            contacts.add(contact);
        }
        mContactSet = contacts;
        mMessageId = msgId;
        mBody = msgId;
        mIpmsgId = ipmsgId;
        mFilePath = filePath;
        mType = MessageType.FT;
        mMimeType = getMimeType(filePath);
    }

    /**
     * constructor for group file transfer
     * @param chatId
     * @param contact
     * @param context
     * @param filePath
     * @param msgId
     * @param ipmsgId
     */
    public ReceiveMessageStruct(String chatId, String contact, Context context, String filePath,
            String msgId, long ipmsgId) {
        this(contact, context, filePath, msgId, ipmsgId, Class.NORMAL);
        mFlag = ThreadFlag.MTM;
        mChatId = chatId;
    }

}
