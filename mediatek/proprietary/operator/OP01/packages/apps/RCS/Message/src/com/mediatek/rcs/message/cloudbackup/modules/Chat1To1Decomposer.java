package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.File;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Message;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.RcsMessage;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Restore one one to one wrapped file.
 */
class Chat1To1Decomposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "Chat1To1Decomposer";
    private ContentResolver mContentResolver;
    private Chat1To1RestoreParser mChat1To1RestoreParser;
    private MessageRecord mMessageRecord;
    private RcsMsgRecord mRcsMsgRecord;
    private Context mContext;
    private boolean mCancel = false;


    Chat1To1Decomposer(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mChat1To1RestoreParser = new Chat1To1RestoreParser();
    }

    protected int parseOneToOneMsg(File file) {
        Log.d(CLASS_TAG, "parseOneToOneMsg begin, filename = " + file.getName());
        initParse();
        int result = mChat1To1RestoreParser.parseOneToOneMsg(file);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "parseOneToOneMsg result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "parseOneToOneMsg end, result = ok");
        result = restructureRecord();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "restructureRecord result is not ok, return");
            return result;
        }

        result = insertDataBase();

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    private void initParse() {
        mMessageRecord = null;
        mRcsMsgRecord = null;
        mRcsMsgRecord = new RcsMsgRecord();
        mMessageRecord = new MessageRecord();
        mChat1To1RestoreParser.setMessageRecord(mMessageRecord);
        mChat1To1RestoreParser.setRcsMsgRecord(mRcsMsgRecord);
    }

    protected int persistMessageBody(String chatId, long threadId, String msgContent) {
        Log.d(CLASS_TAG, "persistMessageBody begin, msgContent = " + msgContent);
        initParse();

        int result = mChat1To1RestoreParser.persistMessageBody(chatId, threadId, msgContent);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "persistMessageBody result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "persistMessageBody end, result = ok");

        if (mCancel) {
            Log.d(CLASS_TAG, "persistMessageBody service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        result = restructureRecord();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "restructureRecord result is not ok, return");
            return result;
        }
        result = insertDataBase();
        return result;
    }

    private int restructureRecord() {
        String contactNumber;
        String to = mRcsMsgRecord.getTo();
        String from = mRcsMsgRecord.getFrom();
        // assgin value to contactNumber
        if (to.equals(BackupConstant.ANONYMOUS)) {
            // group message to is anonymous
            if (from.equals(CloudBrUtils.getMyNumber())) {
                contactNumber = mRcsMsgRecord.getChatId();
            } else {
                contactNumber = from;
            }
        } else {
            if (from.equals(CloudBrUtils.getMyNumber())) {
                contactNumber = to;
            } else {
                contactNumber = from;
            }
        }
        Log.d(CLASS_TAG, "contact number is = " + contactNumber);
        mRcsMsgRecord.setContactNum(contactNumber);
        mMessageRecord.setContactNumber(contactNumber);

        String chatId = mRcsMsgRecord.getChatId();
        if (chatId == null) {
            Log.d(CLASS_TAG, "chat id fill with contactnumber");
            mMessageRecord.setChatId(contactNumber);
            mRcsMsgRecord.setChatId(contactNumber);
        }

        // if getThreadId is o, this msg is not a group message.
        Log.d(CLASS_TAG, "contactNumber = " + contactNumber);
        if (mRcsMsgRecord.getConversation() == 0) {
           long threadId = FileUtils.getOrCreate1TNThreadId(mContext, contactNumber);
           Log.d(CLASS_TAG, "restructureRecord() this is not group message");
           Log.d(CLASS_TAG, "restructureRecord() thradid = " + threadId);
           mRcsMsgRecord.setConversation((int) threadId);
        }
        mRcsMsgRecord.setIsBlocked(0);//0 is normal msg, 1 is spam msg
        mRcsMsgRecord.setState(1);//0 is not delivered, 1 deliverd

        mMessageRecord.setDirection(mRcsMsgRecord.getDirection());
        mMessageRecord.setMimeType(mRcsMsgRecord.getMimeType());
        mMessageRecord.setTimestamp(mRcsMsgRecord.getTimestamp());

        // boolean isSysInfo = isSystemMsg(mMessageRecord);
        // if (isSysInfo) {
        // Log.d(CLASS_TAG,
        // "this msg is a sys info, so need't insert sms table");
        // mMessageRecord.setMimeType(null);
        // mSmsRecord = null;
        // }
        return CloudBrUtils.ResultCode.OK;
    }

    // private boolean isSystemMsg(MessageRecord messageRecord) {
    // int direction = messageRecord.getDirection();
    // String type = messageRecord.getMimeType();
    // if (direction == Message.Direction.IRRELEVANT &&
    // type.equals(CloudBrUtils.ContentType.TEXT_TYPE)) {
    // Log.d(CLASS_TAG, "isSystemMsg is true");
    // return true;
    // }
    // return false;
    // }

    private int insertDataBase() {
        ContentValues messageCv = new ContentValues();
        combineMessageRecord(messageCv);
        ContentValues rcsMsgCv = null;
        if (mRcsMsgRecord != null) {
            rcsMsgCv = new ContentValues();
            combineRcsMsgRecord(rcsMsgCv);
        }

        Uri uri = mContentResolver.insert(CloudBrUtils.CHAT_CONTENT_URI, messageCv);
        if (uri == null) {
            Log.e(CLASS_TAG, "insert chat message db fail uri = null");
            return CloudBrUtils.ResultCode.INSERTDB_EXCEPTION;
        }
        if (rcsMsgCv != null) {
            long id = ContentUris.parseId(uri);
            rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_IPMSG_ID, id);
            rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MESSAGE_ID, id);
            Log.e(CLASS_TAG, "smsCv.put(Sms.IPMSG_ID, id) id = " + id);
            Uri smsUri = mContentResolver.insert(CloudBrUtils.RCS_URI, rcsMsgCv);
            Log.d(CLASS_TAG, "smsUri = " + smsUri);
        }

        return CloudBrUtils.ResultCode.OK;
    }

    private void combineRcsMsgRecord(ContentValues rcsMsgCv) {
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_BODY, mRcsMsgRecord.getBody());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_CHAT_ID, mRcsMsgRecord.getChatId());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_CONTACT_NUMBER, mRcsMsgRecord.getContactNum());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_CONVERSATION, mRcsMsgRecord.getConversation());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_DATE_SENT, mRcsMsgRecord.getDataSent());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_DIRECTION, mRcsMsgRecord.getDirection());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_FLAG, mRcsMsgRecord.getFlag());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_ISBLOCKED, mRcsMsgRecord.getIsBlocked());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_LOCKED, mRcsMsgRecord.getLocked());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS, mRcsMsgRecord.getStatus());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MIME_TYPE, mRcsMsgRecord.getMimeType());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_MSG_CLASS, mRcsMsgRecord.getMsgClass());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_SEEN, mRcsMsgRecord.getSeen());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_SUB_ID, mRcsMsgRecord.getSubID());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_TIMESTAMP, mRcsMsgRecord.getTimestamp());
        rcsMsgCv.put(RcsMessage.MESSAGE_COLUMN_TYPE, mRcsMsgRecord.getType());
    }

    private void combineMessageRecord(ContentValues messageCv) {
        messageCv.put(Message.BODY, mMessageRecord.getBody());
        messageCv.put(Message.CHAT_ID, mMessageRecord.getChatId());
        messageCv.put(Message.CONTACT_NUMBER, mMessageRecord.getContactNumber());
        messageCv.put(Message.CONVERSATION_ID, mMessageRecord.getConversationId());
        messageCv.put(Message.DIRECTION, mMessageRecord.getDirection());
        messageCv.put(Message.MESSAGE_STATUS, mMessageRecord.getStatus());
        messageCv.put(Message.MIME_TYPE, mMessageRecord.getMimeType());
        messageCv.put(Message.MESSAGE_TYPE, mMessageRecord.getType());
        messageCv.put(Message.TIMESTAMP, mMessageRecord.getTimestamp());
        messageCv.put(Message.TIMESTAMP_DELIVERED, mMessageRecord.getDeliveredTimestamp());
        messageCv.put(Message.TIMESTAMP_DISPLAYED, mMessageRecord.getDisplayTimestamp());
        messageCv.put(Message.TIMESTAMP_SENT, mMessageRecord.getDeliveredTimestamp());
        messageCv.put(Message.MESSAGE_ID, mMessageRecord.getMsgId());
    }

}
