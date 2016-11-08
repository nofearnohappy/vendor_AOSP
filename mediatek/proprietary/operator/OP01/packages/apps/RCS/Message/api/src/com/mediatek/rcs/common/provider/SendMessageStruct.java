package com.mediatek.rcs.common.provider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.text.TextUtils;

import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RcsLog.ThreadFlag;
import com.mediatek.rcs.common.utils.RCSUtils;

public class SendMessageStruct extends MessageStruct {

    public SendMessageStruct(Context context) {
        mContext = context;
        mBody = null;
        mDirection = Direction.OUTGOING;
        mTimestamp = System.currentTimeMillis();
        mSeen = 1;
        mMessageStatus = MessageStatus.SENDING;
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

    private SendMessageStruct(Context context, String contact, String content) {
        this(context);
        mBody = content;
        mContact = contact;
        Set<String> contacts = new HashSet<String>();
        if (!TextUtils.isEmpty(contact)) {
            contacts.add(contact);
        }
        mContactSet = contacts;
        mFlag = ThreadFlag.OTO;
    }

    private SendMessageStruct(Context context, List<String> contacts, String content) {
        this(context);
        mBody = content;
        Set<String> recipients = new HashSet<String>();
        StringBuffer contact = new StringBuffer();
        for (String con : contacts){
            recipients.add(con);
            contact.append(con);
            contact.append(';');
        }
        mContactSet = recipients;
        mContact = contact.toString();
        mContact = mContact.substring(0, mContact.length() - 1);
        mFlag = ThreadFlag.OTM;
    }

    public SendMessageStruct(Context context, String contact, String content, int msgType) {
        this(context, contact, content);
        mFlag = ThreadFlag.OTO;
        mClass = msgType;
    }

    public SendMessageStruct(Context context, List<String> contacts, String content, int msgType) {
        this(context, contacts, content);
        mFlag = ThreadFlag.OTM;
        mClass = msgType;
    }

//    private SendMessageStruct(Context context, String chatId, String contact, String content) {
//        this(context, contact, content);
//        mFlag = ThreadFlag.MTM;
//        mChatId = chatId;
//    }

    public SendMessageStruct(Context context, String chatId, String contact,
            String content, int msgType) {
        this(context, contact, content, msgType);
        mFlag = ThreadFlag.MTM;
        mChatId = chatId;
    }

    public SendMessageStruct(String contact, Context context, int msgType, String filePath,
            long ipmsgId) {
        this(context);
        mType = MessageType.FT;
        mContact = contact;
        mContactSet = new HashSet<String>();
        if (!TextUtils.isEmpty(contact)) {
            mContactSet.add(contact);
        }
        mClass = msgType;
        mFilePath = filePath;
        mFlag = ThreadFlag.OTO;
        mIpmsgId = ipmsgId;
        mMimeType = getMimeType(filePath);
    }

    public SendMessageStruct(Set<String> contacts, Context context, String filePath, long ipmsgId) {
        this(null, context, Class.NORMAL, filePath, ipmsgId);
        mFlag = ThreadFlag.OTM;
        mContactSet = contacts;
        StringBuffer contact = new StringBuffer();
        for (String con : contacts){
            contact.append(con);
            contact.append(';');
        }
        mContact = contact.toString();
        mContact = mContact.substring(0, mContact.length() - 1);
    }

    public SendMessageStruct(String chatId, Context context, String filePath, long ipmsgId) {
        this(null, context, Class.NORMAL, filePath, ipmsgId);
        mFlag = ThreadFlag.MTM;
        mChatId = chatId;
    }

}
