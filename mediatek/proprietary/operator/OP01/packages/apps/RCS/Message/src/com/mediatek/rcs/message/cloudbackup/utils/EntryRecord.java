package com.mediatek.rcs.message.cloudbackup.utils;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.GroupChatMember;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;

/**
 * record defined by the database table and cmcc spec.
 * @author mtk81368
 *
 */
public class EntryRecord {

    /**
     * ft file need wrapped to FileObject, next wrap to cmcc styles or insert database.
     * @author mtk81368
     *
     */
    public static class FileObject {
        private String mCid;
        private String mName;
        private String mType;
        private long mSize;
        private long mDate;

        /**
         * @return
         */
        public String getCid() {
            return mCid;
        }

        /**
         * @param mCid
         */
        public void setCid(String cid) {
            this.mCid = cid;
        }

        /**
         * @return
         */
        public String getName() {
            return mName;
        }

        /**
         * @param mName
         */
        public void setName(String name) {
            this.mName = name;
        }

        /**
         * @return ft file type, eg, image, audio.
         */
        public String getType() {
            return mType;
        }

        /**
         * @param mType
         */
        public void setType(String type) {
            this.mType = type;
        }

        /**
         * @return
         */
        public long getSize() {
            return mSize;
        }

        /**
         * @param mSize
         */
        public void setSize(long size) {
            this.mSize = size;
        }

        /**
         * @return
         */
        public long getDate() {
            return mDate;
        }

        /**
         * @param mDate
         */
        public void setDate(long date) {
            this.mDate = date;
        }
    }

    /**
     * Group chat info.
     * @author mtk81368
     *
     */
    public static class RootRecord {
        /**
         * @return chat type, eg: 1-1, adhoc, 1- many.
         */
        public String getSessionType() {
            return mSessionType;
        }

        /**
         * @param mSessionType
         */
        public void setSessionType(String sessionType) {
            this.mSessionType = sessionType;
        }

        /**
         * @return
         */
        public String getParticipants() {
            return mParticipants;
        }

        /**
         * @param mParticipants
         */
        public void setParticipants(String participants) {
            this.mParticipants = participants;
        }

        /**
         * @return
         */
        public ArrayList<GroupNumberRecord> getNumberInfo() {
            return mNumberInfo;
        }

        /**
         * @param mNumberInfo
         */
        public void setNumberInfo(ArrayList<GroupNumberRecord> numberInfo) {
            this.mNumberInfo = numberInfo;
        }

        private String mSessionType;// file-transfer-type
        private String mParticipants;
        private ArrayList<GroupNumberRecord> mNumberInfo = null;
    }

    /**
     * Describer one group number of group chat.
     * @author mtk81368
     *
     */
    public static class GroupNumberRecord {
        private String mNumber;
        private int mState;
        private String mName;

        /**
         * @return
         */
        public String getNumber() {
            return mNumber;
        }

        /**
         * @param mNumber
         */
        public void setNumber(String number) {
            this.mNumber = number;
        }

        /**
         * @return
         */
        public int getState() {
            return mState;
        }

        /**
         * @param state
         */
        public void setState(int state) {
            this.mState = state;
        }

        /**
         * @return
         */
        public String getName() {
            return mName;
        }

        /**
         * @param mName
         */
        public void setName(String name) {
            this.mName = name;
        }
    }

    /**
     * One ftRecord is a record of Ft table in the ft database.
     * In backup, we get ft record info to FtRecord, then wrap.
     * In restore, we unwrap the file to FtRecord, then insert database.
     * @author mtk81368
     *
     */
    public static class FtRecord {
        private String mFtId;
        private String mMsgId;
        private String mContactNumber;
        private String mFileName;
        private String mChatId;
        private String mMimeType;
        private int mStatus;
        private int mDirection;
        private long mTimestamp;
        private long mSendTimestamp;
        private long mDeliveredTimestamp;
        private long mDisplayedTimestamp;
        private long mSize;
        private long mDuration;
        private int mSessionType = 0;
       // private String mFileIcon;

        private String mFrom;
        private String mTo;
        private boolean mIsHasDirection;
        private String mRejoinId;

        /**
         * @return
         */
        public String getMsgId() {
            return mMsgId;
        }

        /**
         * @param mMsgId
         */
        public void setMsgId(String msgId) {
            this.mMsgId = msgId;
        }

        /**
         * @return
         */
        public String getRejoinId() {
            return mRejoinId;
        }

        /**
         * @param mRejoinId
         */
        public void setRejoinId(String rejoinId) {
            this.mRejoinId = rejoinId;
        }

        /**
         * @return
         */
        public String getFrom() {
            return mFrom;
        }

        /**
         * @param mFrom
         */
        public void setFrom(String from) {
            this.mFrom = from;
        }

        /**
         * @return
         */
        public String getTo() {
            return mTo;
        }

        /**
         * @param mTo
         */
        public void setTo(String to) {
            this.mTo = to;
        }

        /**
         * @return if this record is setted direction.
         * if havnt set, need computer the value by others mumber.
         */
        public boolean isIsHasDirection() {
            return mIsHasDirection;
        }

        /**
         * @return
         */
        public String getFtId() {
            return mFtId;
        }

        /**
         * @param mFtId
         */
        public void setFtId(String ftId) {
            this.mFtId = ftId;
        }

        /**
         * @return
         */
        public String getContactNumber() {
            return mContactNumber;
        }

        /**
         * @param mContactNumber
         */
        public void setContactNumber(String contactNumber) {
            this.mContactNumber = contactNumber;
        }

        /**
         * @return
         */
        public String getFileName() {
            return mFileName;
        }

        /**
         * @param mFileName
         */
        public void setFileName(String fileName) {
            this.mFileName = fileName;
        }

        /**
         * @return
         */
        public String getChatId() {
            return mChatId;
        }

        /**
         * @param mChatId
         */
        public void setChatId(String chatId) {
            this.mChatId = chatId;
        }

        /**
         * @return
         */
        public String geMimeType() {
            return mMimeType;
        }

        /**
         * @param mMimeType
         */
        public void seMimeType(String mimeType) {
            this.mMimeType = mimeType;
        }

        /**
         * @return
         */
        public int getStatus() {
            return mStatus;
        }

        /**
         * @param mStatus
         */
        public void setStatus(int status) {
            this.mStatus = status;
        }

        /**
         * @return
         */
        public int getDirection() {
            return mDirection;
        }

        /**
         * @param mDirection
         */
        public void setDirection(int direction) {
            mIsHasDirection = true;
            this.mDirection = direction;
        }

        /**
         * @return
         */
        public long getTimestamp() {
            Log.d("com.mediatek.rcs.message.cloudbackup/", "ft getTimestamp = " + this.mTimestamp);
            return mTimestamp;
        }

        /**
         * @param mTimestamp
         */
        public void setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
            Log.d("com.mediatek.rcs.message.cloudbackup/", "ft setTimestamp = " + this.mTimestamp);
        }

        /**
         * @return
         */
        public long getSendTimestamp() {
            return mSendTimestamp;
        }

        /**
         * @param mSendTimestamp
         */
        public void setSendTimestamp(long sendTimestamp) {
            this.mSendTimestamp = sendTimestamp;
        }

        /**
         * @return
         */
        public long getDeliveredTimestamp() {
            return mDeliveredTimestamp;
        }

        /**
         * @param mDeliveredTimestamp
         */
        public void setDeliveredTimestamp(long deliveredTimestamp) {
            this.mDeliveredTimestamp = deliveredTimestamp;
        }

        /**
         * @return
         */
        public long getDisplayedTimestamp() {
            return mDisplayedTimestamp;
        }

        /**
         * @param mDisplayedTimestamp
         */
        public void setDisplayedTimestamp(long displayedTimestamp) {
            this.mDisplayedTimestamp = displayedTimestamp;
        }

        /**
         * @return
         */
        public long getSize() {
            return mSize;
        }

        /**
         * @param mSize
         */
        public void setSize(long size) {
            this.mSize = size;
        }

        /**
         * @return
         */
        public long getDuration() {
            return mDuration;
        }

        /**
         * @param mDuration
         */
        public void setDuration(long duration) {
            this.mDuration = duration;
        }

        /**
         * @return
         */
        public int getSessionType() {
            return mSessionType;
        }

        /**
         * @param mSessionType
         */
        public void setSessionType(int sessionType) {
            this.mSessionType = sessionType;
        }

//        /**
//         * @return
//         */
//        public String getFileIcon() {
//            return mFileIcon;
//        }
//
//        /**
//         * @param mFileIcon
//         */
//        public void setFileIcon(String fileIcon) {
//            this.mFileIcon = fileIcon;
//        }
    }

    /**
     * One ChatRecord is a record of chat table in the chat database.
     * In backup, we get chat record info to ChatRecord, then wrap.
     * In restore, we unwrap the file to ChatRecord, then insert chat database.
     */
    public static class ChatRecord {
        private String mChatId;
        private String mRejoinId;
        private String mSubject;
        private String mParticipants;
        private String mChairman;
        private int mState;
        private int mDirection;
        private long mTimeStamp;
        private String mConversionId;

        private long mThreadId;
        private String mTo;
        private int mThreadMapStatus;

        /**
         * @return
         */
        public int getThreadMapStatus() {
            return mThreadMapStatus;
        }

        /**
         * @param mStatus
         */
        public void setThreadMapStatus(int threadMapStatus) {
            this.mThreadMapStatus = threadMapStatus;
        }

        /**
         * @return
         */
        public long getThreadId() {
            return mThreadId;
        }

        /**
         * @param mThreadId
         */
        public void setThreadId(long threadId) {
            this.mThreadId = threadId;
        }

        /**
         * @return
         */
        public String getTo() {
            return mTo;
        }

        /**
         * @param mTo
         */
        public void setTo(String to) {
            this.mTo = to;
        }

        /**
         * @return
         */
        public String getConversionId() {
            return mConversionId;
        }

        /**
         * @param conversionId
         */
        public void setConversionId(String conversionId) {
            this.mConversionId = conversionId;
        }

        /**
         * @return
         */
        public String getChatId() {
            return mChatId;
        }

        /**
         * @param mChatId
         */
        public void setChatId(String chatId) {
            this.mChatId = chatId;
        }

        /**
         * @return
         */
        public String getRejoinId() {
            return mRejoinId;
        }

        /**
         * @param mRejoinId
         */
        public void setRejoinId(String rejoinId) {
            this.mRejoinId = rejoinId;
        }

        /**
         * @return
         */
        public String getSubject() {
            return mSubject;
        }

        /**
         * @param mSubject
         */
        public void setSubject(String subject) {
            this.mSubject = subject;
        }

        /**
         * @return
         */
        public String getParticipants() {
            return mParticipants;
        }

        /**
         * @param mParticipants
         */
        public void setParticipants(String participants) {
            this.mParticipants = participants;
        }

        /**
         * @return
         */
        public String getChairman() {
            return mChairman;
        }

        /**
         * @param mChairman
         */
        public void setChairman(String chairman) {
            this.mChairman = chairman;
        }

        /**
         * @return
         */
        public int getState() {
            return mState;
        }

        /**
         * @param mStatus
         */
        public void setState(int status) {
            this.mState = status;
        }

        /**
         * @return
         */
        public int getDirection() {
            return mDirection;
        }

        /**
         * @param mDirection
         */
        public void setDirection(int direction) {
            this.mDirection = direction;
        }

        /**
         * @return
         */
        public long getTimeStamp() {
            return mTimeStamp;
        }

        /**
         * @param mTimeStamp
         */
        public void setTimeStamp(long timeStamp) {
            this.mTimeStamp = timeStamp;
        }

    }

    /**
     * One MessageRecord is a record of message table in the chat database.
     * In backup, we get message record info to MessageRecord, then wrap.
     * In restore, we unwrap the file to MessageRecord, then insert message database.
     */
    public static class MessageRecord {
        private String mChatId;
        private int mStatus;
        private int mDirection;
        private int mType;
        private byte[] mBody;
        private long mTimestamp;
        private long mSendTimestamp = 0L;
        private long mDeliveredTimestamp = 0L;
        private long mDisplayTimestamp = 0L;
        private String mMimeType;
        private String mDisplayName;
        private String mConversationId;
        private String mContactNumber;
        private String mMsgId;

        private boolean mIsHasDirection = false;
        private String mFrom;
        private String mTo;
        private int mFlag = 0;// only useful to favorite message.

        public int getFlag() {
            return mFlag;
        }

        public void setFlag(int flag) {
            this.mFlag = flag;
        }

        /**
         * @return
         */
        public String getMsgId() {
            return mMsgId;
        }

        /**
         * @param mMsgId
         */
        public void setMsgId(String msgId) {
            this.mMsgId = msgId;
        }

        /**
         * @return
         */
        public String getFrom() {
            return mFrom;
        }

        /**
         * @param mFrom
         */
        public void setFrom(String from) {
            this.mFrom = from;
        }

        /**
         * @return
         */
        public String getTo() {
            return mTo;
        }

        /**
         * @param mTo
         */
        public void setTo(String to) {
            this.mTo = to;
        }

        /**
         * @return
         */
        public boolean isIsHasDirection() {
            return mIsHasDirection;
        }

        /**
         * @return
         */
        public String getContactNumber() {
            return mContactNumber;
        }

        /**
         * @param contactNumber
         */
        public void setContactNumber(String contactNumber) {
            this.mContactNumber = contactNumber;
        }

        /**
         * @return
         */
        public String getChatId() {
            return mChatId;
        }

        /**
         * @param mChatId
         */
        public void setChatId(String chatId) {
            this.mChatId = chatId;
        }

        /**
         * @return
         */
        public int getStatus() {
            return mStatus;
        }

        /**
         * @param mStatus
         */
        public void setStatus(int status) {
            this.mStatus = status;
        }

        /**
         * @return
         */
        public int getDirection() {
            return mDirection;
        }

        /**
         * @param mDirection
         */
        public void setDirection(int direction) {
            mIsHasDirection = true;
            this.mDirection = direction;
        }

        /**
         * @return
         */
        public int getType() {
            return mType;
        }

        /**
         * @param mType
         */
        public void setType(int type) {
            this.mType = type;
        }

        /**
         * @return
         */
        public byte[] getBody() {
            return mBody;
        }

        /**
         * @param mBody
         */
        public void setBody(byte[] body) {
            this.mBody = body;
        }

        /**
         * @return
         */
        public long getTimestamp() {
            return mTimestamp;
        }

        /**
         * @param mTimestamp
         */
        public void setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
        }

        /**
         * @return
         */
        public long getSendTimestamp() {
            Log.d(CLASS_TAG, "getSendTimestamp() = " + mSendTimestamp);
            return mSendTimestamp;
        }

        /**
         * @param mSendTimestamp
         */
        public void setSendTimestamp(long sendTimestamp) {
            this.mSendTimestamp = sendTimestamp;
        }

        /**
         * @return
         */
        public long getDeliveredTimestamp() {
            return mDeliveredTimestamp;
        }

        /**
         * @param mDeliveredTimestamp
         */
        public void setDeliveredTimestamp(long deliveredTimestamp) {
            this.mDeliveredTimestamp = deliveredTimestamp;
        }

        /**
         * @return
         */
        public long getDisplayTimestamp() {
            return mDisplayTimestamp;
        }

        /**
         * @param mDisplayTimestamp
         */
        public void setDisplayTimestamp(long displayTimestamp) {
            this.mDisplayTimestamp = displayTimestamp;
        }

        /**
         * @return
         */
        public String getMimeType() {
            return mMimeType;
        }

        /**
         * @param mMimeType
         */
        public void setMimeType(String mimeType) {
            this.mMimeType = mimeType;
        }

        /**
         * @return
         */
        public String getDisplayName() {
            return mDisplayName;
        }

        /**
         * @param mDisplayName
         */
        public void setDisplayName(String displayName) {
            this.mDisplayName = displayName;
        }

        /**
         * @return
         */
        public String getConversationId() {
            return mConversationId;
        }

        /**
         * @param mConversationId
         */
        public void setConversationId(String conversationId) {
            this.mConversationId = conversationId;
        }

    }

    public static class RcsMsgRecord {
        private int mDataSent;
        private int mSeen = 0;
        private int mLocked;
        private int mSubID;
        private int mIpmsgId; //_id in chat/ft database
        private int mState = 0;
        private int mMsgClass;//msgtype for normal/burn/emoticon/cloud/system
        private String mFilePath;
        private String mMsgStrId; //String messageId in stack db
        private String mChatId;
        private String mContactNum;
        private String mBody;
        private long mTimestamp;
        private int mStatus;  // 0UNREAD/2READ/3SENDING/4SENT/5FAILED/6TO_SEND
        private int mType;   //1 IM/2 FT
        private int mDirection;
        private int mFlag; // 1 OTO/2 OTM/3 MTM
        private int mIsBlocked;
        private int mConversation;
        private String mMimeType;

        private String mFrom;
        private String mTo;
        private boolean mIsHasDirection = false;

        /**
         * @return
         */
        public boolean isIsHasDirection() {
            return mIsHasDirection;
        }

        /**
         * @return
         */
        public String getFrom() {
            return mFrom;
        }

        /**
         * @param mFrom
         */
        public void setFrom(String from) {
            this.mFrom = from;
        }

        /**
         * @return
         */
        public String getTo() {
            return mTo;
        }

        /**
         * @param mTo
         */
        public void setTo(String to) {
            this.mTo = to;
        }

        public int getState() {
            return mState;
        }

        public void setState(int state) {
            this.mState = state;
        }

        public int getDataSent() {
            return mDataSent;
        }
        public void setDataSent(int dataSent) {
            this.mDataSent = dataSent;
        }
        public int getSeen() {
            return mSeen;
        }
        public void setSeen(int seen) {
            this.mSeen = seen;
        }
        public int getLocked() {
            return mLocked;
        }
        public void setLocked(int locked) {
            this.mLocked = locked;
        }
        public int getSubID() {
            return mSubID;
        }
        public void setSubID(int subID) {
            this.mSubID = subID;
        }
        public int getIpmsgId() {
            return mIpmsgId;
        }
        public void setIpmsgId(int ipmsgId) {
            this.mIpmsgId = ipmsgId;
        }
        public int getMsgClass() {
            return mMsgClass;
        }
        public void setMsgClass(int msgClass) {
            this.mMsgClass = msgClass;
        }
        public String getFilePath() {
            return mFilePath;
        }
        public void setFilePath(String filePath) {
            this.mFilePath = filePath;
        }
        public String getMsgStrId() {
            return mMsgStrId;
        }
        public void setMsgStrId(String msgStrId) {
            this.mMsgStrId = msgStrId;
        }
        public String getChatId() {
            return mChatId;
        }
        public void setChatId(String chatId) {
            this.mChatId = chatId;
        }
        public String getContactNum() {
            return mContactNum;
        }
        public void setContactNum(String contactNum) {
            this.mContactNum = contactNum;
        }
        public String getBody() {
            return mBody;
        }
        public void setBody(String body) {
            this.mBody = body;
        }
        public long getTimestamp() {
            return mTimestamp;
        }
        public void setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
        }
        public int getStatus() {
            return mStatus;
        }
        public void setStatus(int status) {
            this.mStatus = status;
        }
        public int getType() {
            return mType;
        }
        public void setType(int type) {
            this.mType = type;
        }
        public int getDirection() {
            return mDirection;
        }
        public void setDirection(int direction) {
            mIsHasDirection = true;
            this.mDirection = direction;
        }
        public int getFlag() {
            return mFlag;
        }
        public void setFlag(int flag) {
            this.mFlag = flag;
        }
        public int getIsBlocked() {
            return mIsBlocked;
        }
        public void setIsBlocked(int isBlocked) {
            this.mIsBlocked = isBlocked;
        }
        public int getConversation() {
            return mConversation;
        }
        public void setConversation(int conversation) {
            this.mConversation = conversation;
        }
        public String getMimeType() {
            return mMimeType;
        }
        public void setMimeType(String mimeType) {
            this.mMimeType = mimeType;
        }
    }
    /**
     * One SmsRecord is a record of sms table.
     * In backup, we get message record info to SmsRecord, then wrap.
     * In restore, we unwrap the file to SmsRecord, then insert mmssms database.
     */
    /*public static class SmsRecord {
        private int mType = 0;
        private long mSubId = 2;
        private int mRead = 1;
        private long mThreadId = 0;
        private String mAddress;
        private long mDate;

        *//**
         * @return data.
         *//*
        public long getDate() {
            return mDate;
        }

        *//**
         * @param date
         *//*
        public void setDate(long date) {
            this.mDate = date;
        }

        *//**
         * @return
         *//*
        public long getThreadId() {
            return mThreadId;
        }

        *//**
         * @param threadId
         *//*
        public void setThreadId(long threadId) {
            this.mThreadId = threadId;
        }

        *//**
         * @return
         *//*
        public String getAddress() {
            return mAddress;
        }

        *//**
         * @param address
         *//*
        public void setAddress(String address) {
            this.mAddress = address;
        }

        protected String mBody;

        *//**
         * @return
         *//*
        public String getBody() {
            return mBody;
        }

        *//**
         * @param mBody
         *//*
        public void setBody(String body) {
            this.mBody = body;
        }

        *//**
         * @return
         *//*
        public int getType() {
            return mType;
        }

        *//**
         * @param mType
         *//*
        public void setType(int type) {
            this.mType = type;
        }

        *//**
         * @return sub id
         *//*
        public long getSubId() {
            return mSubId;
        }

        *//**
         * @param mSubId
         *//*
        public void setSubId(long subId) {
            this.mSubId = subId;
        }

        *//**
         * @return
         *//*
        public int getRead() {
            return mRead;
        }

        *//**
         * @param mRead
         *//*
        public void setRead(int read) {
            this.mRead = read;
        }
    }*/

    /**
     * One FavoriteRecord is a record of favorite table.
     * In backup, we get favorite record info to FavoriteRecord, then wrap.
     * In restore, we unwrap the file to FavoriteRecord,
     * then insert favorite table of rcsmessage database.
     */
    public static class FavoriteRecord {
        private String mContactNum;
        private long mDate;
        private int mDataSent;
        private String mChatId;
        private int mType;
        private String mBody;
        private String mPath;
        private long mSize;
        private int mStatus;
        private int mDirection;
        private String mMimeType;
        private int mFlag; // only use to favorite message.
        private String mIcon;
        private int mMsgId;


        public int getMsgId() {
            return mMsgId;
        }

        public void setMsgId(int msgId) {
            this.mMsgId = msgId;
        }

        public int getFlag() {
            return mFlag;
        }

        public void setFlag(int flag) {
            this.mFlag = flag;
        }

        public String getContactNum() {
            return mContactNum;
        }

        public void setContactNum(String contactNum) {
            this.mContactNum = contactNum;
        }

        public long getDate() {
            return mDate;
        }

        public void setDate(long date) {
            this.mDate = date;
        }

        public int getType() {
            return mType;
        }

        public void setType(int type) {
            this.mType = type;
        }

        public String getBody() {
            return mBody;
        }

        public void setBody(String body) {
            this.mBody = body;
        }

        public int getDataSent() {
            return mDataSent;
        }

        public void setDataSent(int dataSent) {
            this.mDataSent = dataSent;
        }

        public String getChatId() {
            return mChatId;
        }

        public void setChatId(String chatId) {
            this.mChatId = chatId;
        }

        public String getIcon() {
            return mIcon;
        }

        public void setIcon(String icon) {
            this.mIcon = icon;
        }

        public String getPath() {
            return mPath;
        }

        public void setPath(String path) {
            this.mPath = path;
        }

        public long getSize() {
            return mSize;
        }

        public void setSize(long size) {
            this.mSize = size;
        }
        /**
         * @return
         */
        public int getStatus() {
            return mStatus;
        }

        /**
         * @param mStatus
         */
        public void setStatus(int status) {
            this.mStatus = status;
        }

        /**
         * @return
         */
        public int getDirection() {
            return mDirection;
        }

        /**
         * @param mDirection
         */
        public void setDirection(int direction) {
            this.mDirection = direction;
        }

        /**
         * @return
         */
        public String getMimeType() {
            return mMimeType;
        }

        /**
         * @param mMimeType
         */
        public void setMimeType(String mimeType) {
            this.mMimeType = mimeType;
        }
    }

    /**
     * Mms Xml Info.
     */
    public static class MmsXmlInfo {
        private String mId;
        private String mIsRead;
        private String mMsgBox;
        private String mDate;
        private String mSize;
        private String mSimId;
        private String mIsLocked;

        public void setID(String id) {
            mId = id;
        }

        public String getID() {
            return (mId == null) ? "" : mId;
        }

        public void setIsRead(String isread) {
            mIsRead = isread;
        }

        public String getIsRead() {
            return ((mIsRead == null) || mIsRead.equals("")) ? "1" : mIsRead;
        }

        public void setMsgBox(String msgBox) {
            mMsgBox = msgBox;
        }

        public String getMsgBox() {
            return ((mMsgBox == null) || mMsgBox.equals("")) ? "1" : mMsgBox;
        }

        public void setDate(String date) {
            mDate = date;
        }

        public String getDate() {
            return (mDate == null) ? "" : mDate;
        }

        public void setSize(String size) {
            mSize = size;
        }

        public String getSize() {
            return ((mSize == null) || mSize.equals("")) ? "0" : mSize;
        }

        public void setSimId(String simId) {
            mSimId = simId;
        }

        public String getSimId() {
            return ((mSimId == null) || mSimId.equals("")) ? "0" : mSimId;
        }

        public void setIsLocked(String islocked) {
            mIsLocked = islocked;
        }

        public String getIsLocked() {
            return ((mIsLocked == null) || mIsLocked.equals("")) ? "0" : mIsLocked;
        }
    }

    private static final String CLASS_TAG = null;

    /**
     * members info of one group wrapped into GroupNumberRecord arraylist of
     * rootRecord.
     * @param mContentResolver
     * @param numberList
     *            all of member ids in the groupMember table of chat.db.
     * @param rootRecord
     * @return
     */
    public static int getRootNumbersInfo(ContentResolver mContentResolver,
            ArrayList<Integer> numberList, RootRecord rootRecord) {
        ArrayList<GroupNumberRecord> numbersInfo = null;
        if (numberList == null || numberList.size() <= 0) {
           Log.d(CLASS_TAG, "getRootNumbersInfo numberList == null or size = 0 return ok.");
           return CloudBrUtils.ResultCode.OK;
        }

        for (int id : numberList) {
            String numbSelec = CloudBrUtils.ID + " = " + id;
            Cursor numberCr = mContentResolver.query(CloudBrUtils.GROUP_MEMBER_URI, null,
                    numbSelec, null, null);
            if (numberCr != null && numberCr.getCount() > 0) {
                numberCr.moveToFirst();
                String number = numberCr.getString(numberCr
                        .getColumnIndex(GroupChatMember.COLUMN_CONTACT_NUMBER));
                int state = numberCr.getInt(numberCr.getColumnIndex(GroupChatMember.COLUMN_STATE));
                String name = numberCr.getString(numberCr
                        .getColumnIndex(GroupChatMember.COLUMN_CONTACT_NAME));
                if (numbersInfo == null) {
                    numbersInfo = new ArrayList<GroupNumberRecord>();
                }
                GroupNumberRecord gnr = new GroupNumberRecord();
                gnr.setName(name);
                gnr.setNumber(number);
                gnr.setState(state);
                numbersInfo.add(gnr);
                name = null;
                state = 0;
                number = null;
            }
            if (numberCr != null) {
                numberCr.close();
            }
        }
        if (numbersInfo != null) {
            rootRecord.setNumberInfo(numbersInfo);
        }
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * get chat info in the chat table of chat.db into chatRecord.
     * @param chatCursor
     * @param chatRecord
     * @return
     */
    public static int getChatInfo(Cursor chatCursor, ChatRecord chatRecord) {
        chatRecord.setTo(chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.REJOIN_ID)));
        String subject = chatCursor.getString(chatCursor.getColumnIndex(CloudBrUtils.Chat.SUBJECT));
        if (subject != null) {
            chatRecord.setSubject(subject);
        }
        chatRecord.setConversionId(chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.CONVERSATION_ID)));
        chatRecord.setTimeStamp(chatCursor.getLong(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.TIMESTAMP)));
        chatRecord.setChatId(chatCursor.getString(chatCursor.getColumnIndex(CloudBrUtils.CHAT_ID)));
        chatRecord.setState(chatCursor.getInt(chatCursor.getColumnIndex(CloudBrUtils.Chat.STATE)));
        chatRecord.setChairman(chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.CHAIRMAN)));
        chatRecord.setParticipants(chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.PARTICIPANTS_LIST)));
        chatRecord.setRejoinId(chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.REJOIN_ID)));
        return CloudBrUtils.ResultCode.OK;
    }
}
