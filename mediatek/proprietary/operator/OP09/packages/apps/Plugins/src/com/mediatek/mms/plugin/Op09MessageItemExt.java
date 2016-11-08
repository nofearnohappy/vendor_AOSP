package com.mediatek.mms.plugin;

import java.lang.ref.SoftReference;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.util.Log;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduPersister;

import com.mediatek.mms.callback.IColumnsMapCallback;
import com.mediatek.mms.callback.IMessageItemCallback;
import com.mediatek.mms.ext.DefaultOpMessageItemExt;

public class Op09MessageItemExt extends DefaultOpMessageItemExt {
    private static String TAG = "Op09MessageItemExt";

    static final int COLUMN_SMS_IP_MESSAGE_ID = 28;
    public static final int DEFERRED_MASK = 0x04;
    Context mContext;
    long mIpMessageId;
    long mMsgId;
    String mType;
    boolean mIsFailedMessage;
    boolean mIsSending;
    String mMmsCc;
    String mAddress;
    String mSubject;
    String mMsgSizeText;
    String mTimestamp;
    int mMmsStatus;
    int mSubId;
    Uri mMessageUri;
    int mMessageType = 0;
    long mSmsSentDate = 0;
    long mMmsSentDate = 0;
    int mBoxId;
//    int mErrorType;
    boolean mSubMsg = false;

    public enum DeliveryStatus  { NONE, INFO, FAILED, PENDING, RECEIVED }

    DeliveryStatus mDeliveryStatus;

    private IMessageItemCallback mMessageItemCallback;
    private IColumnsMapCallback mColumnMapCallback;

    /**
     * This Item is for MMS.
     */
//    @Override
//    public boolean onOpCreateMessageItem(Context context, long msgId, String type,
//                        IMessageItemCallback callback) {
//        Log.d(TAG, "onOpCreateMessageItem-mms: msgId = " + msgId + ", type = " + type +
//                "callback = " + callback);
//        mContext = context;
//        mMsgId = msgId;
//        mType = type;
//        mMessageItemCallback = callback;
//        mIpMessageId = 0;
//        if (mMessageItemCallback != null) {
//            mBoxId = mMessageItemCallback.getBoxIdCallback();
//            mSubId = mMessageItemCallback.getSubId();
//            mMmsStatus = mMessageItemCallback.getMmsStatus();
//            mMessageType = mMessageItemCallback.getMessageType();
//        }
//        mMessageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
//        return true;
//    }

    /**
     * This Item is for SMS and MMS.
     */
    @Override
    public boolean onOpCreateMessageItem(Context context, Cursor cursor, long msgId, String type,
            IMessageItemCallback callback, IColumnsMapCallback columnMapCallback) {
        Log.d(TAG, "onOpCreateMessageItem-mms: msgId = " + msgId + ", type = " + type +
                "callback = " + callback);
        mMessageItemCallback = callback;
        mColumnMapCallback = columnMapCallback;
        mContext = context;
        mType = type;
        mMsgId = msgId;
        int ipsgColumIndex = cursor.getColumnIndex("ipmsg_id");


        if (type.equals("sms")) {
            Log.d(TAG, "onIpCreateMessageItem ipsgColumIndex!" + ipsgColumIndex);
            if (mMessageItemCallback != null) {
                mSubMsg = mMessageItemCallback.getIsSubMessage();
                Log.d(TAG, "onIpCreateMessageItem mSubMsg = " + mSubMsg);
            }
            if (ipsgColumIndex == -1) {
                return false;
            }
            mIpMessageId = cursor.getLong(columnMapCallback.getColumnSmsIpMessageId());
            if (mMessageItemCallback != null) {
                mBoxId = mMessageItemCallback.getBoxIdCallback();
                mSubId = mMessageItemCallback.getSubId();
                mAddress = mMessageItemCallback.getAddress();
            } else if (mColumnMapCallback != null) {
                mBoxId = cursor.getInt(mColumnMapCallback.getColumnSmsType());
                mSubId = cursor.getInt(mColumnMapCallback.getColumnSmsSubId());
                mAddress = cursor.getString(mColumnMapCallback.getColumnSmsAddress());
            }
            mMessageUri = ContentUris.withAppendedId(Sms.CONTENT_URI, mMsgId);
            if (mMessageItemCallback.isReceivedMessage() && !isSubMsg()) {
                int dateSentColumn = cursor.getColumnIndex(Sms.DATE_SENT);
                if (dateSentColumn == -1) {
                    dateSentColumn = cursor.getColumnIndex(Sms.DATE);
                }
                if (dateSentColumn != -1) {
                    mSmsSentDate = cursor.getLong(dateSentColumn);
                }
            }
        } else if (type.equals("mms")) {
            mIpMessageId = 0;
            if (mMessageItemCallback != null) {
                mBoxId = mMessageItemCallback.getBoxIdCallback();
                mSubId = mMessageItemCallback.getSubId();
                mMmsStatus = mMessageItemCallback.getMmsStatus();
                mMessageType = mMessageItemCallback.getMessageType();
            }
            if (mColumnMapCallback != null) {
                String cc = cursor.getString(mColumnMapCallback.getColumnMmsCc());
                String encoding = cursor.getString(mColumnMapCallback.getColumnMmsCcEncoding());
                mMmsCc = getMmsCcString(cc, encoding);
            }
            mMessageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
            Log.d(TAG, "mMmsCc = " + mMmsCc);
        } else {
            Log.e(TAG, "unKnown type: type = " + type);
        }
        return true;
    }

    /**
     * This Item is forMMS.
     */
    @Override
    public boolean onOpCreateMessageItem(Context context, long msgId, String type,
            IMessageItemCallback callback, long ipMsgId, int boxId, int subId,
            String address, String timeStamp,String cc, String ccEncoding, long mmsSentDate) {
        Log.d(TAG, "onOpCreateMessageItem-mms: msgId = " + msgId + ", type = " + type +
                "callback = " + callback);
        mMessageItemCallback = callback;
        mContext = context;
        mType = type;
        mMsgId = msgId;
        mMmsSentDate = mmsSentDate;

        if (type.equals("sms")) {
            mIpMessageId = ipMsgId;
            if (mMessageItemCallback != null) {
                mBoxId = mMessageItemCallback.getBoxIdCallback();
                mSubId = mMessageItemCallback.getSubId();
                mAddress = mMessageItemCallback.getAddress();
            } else if (mColumnMapCallback != null) {
                mBoxId = boxId;
                mSubId = subId;
                mAddress = address;
            }
            mMessageUri = ContentUris.withAppendedId(Sms.CONTENT_URI, mMsgId);
        } else if (type.equals("mms")) {
            mIpMessageId = 0;
            if (mMessageItemCallback != null) {
                mBoxId = mMessageItemCallback.getBoxIdCallback();
                mSubId = mMessageItemCallback.getSubId();
                mMmsStatus = mMessageItemCallback.getMmsStatus();
                mMessageType = mMessageItemCallback.getMessageType();
            }
            mAddress = address;
            mTimestamp = timeStamp;
            String encoding = ccEncoding;
            mMmsCc = getMmsCcString(cc, encoding);
            mMessageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
            Log.d(TAG, "mMmsCc = " + mMmsCc);
        } else {
            Log.e(TAG, "unKnown type: type = " + type);
        }
        return true;
    }

    public boolean isSubMsg() {
        return mSubMsg;
    }

    public boolean isReceivedMessage() {
        boolean ret = false;
        if (mMessageItemCallback != null) {
            ret = mMessageItemCallback.isReceivedMessage();
        }
        Log.d(TAG, "isReceivedMessage: ret = " + ret);
        return ret;
//        boolean isReceivedMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_INBOX);
//        boolean isReceivedSms = isSms() && (mBoxId == Sms.MESSAGE_TYPE_INBOX);
//        return isReceivedMms || isReceivedSms || (mBoxId == 0 && isSms());
    }


 public int getMmsDownloadStatus() {
        return mMmsStatus & ~DEFERRED_MASK;
    }

    public boolean isMms() {
        return mType.equals("mms");
    }
    public boolean isOutgoingMessage() {
        boolean ret = false;
        if (mMessageItemCallback != null) {
            ret = mMessageItemCallback.isOutgoingMessage();
        }
        Log.d(TAG, "isOutgoingMessage: ret = " + ret);
        return ret;
//        boolean isOutgoingMms = isMms()
//                && (mBoxId == Mms.MESSAGE_BOX_OUTBOX || mBoxId == Mms.MESSAGE_BOX_FAILED);
//        boolean isOutgoingSms = isSms()
//                                    && ((mBoxId == Sms.MESSAGE_TYPE_FAILED)
//                                            || (mBoxId == Sms.MESSAGE_TYPE_OUTBOX)
//                                            || (mBoxId == Sms.MESSAGE_TYPE_QUEUED));
//        return isOutgoingMms || isOutgoingSms;
    }

    public boolean isSending() {
        return !isFailedMessage() && isOutgoingMessage();
    }

    public boolean isFailedMessage() {
        boolean ret = false;
        if (mMessageItemCallback != null) {
            ret = mMessageItemCallback.isFailedMessage();
        }
        return ret;
//        boolean isFailedMms = isMms()
//                            && mErrorType >= MmsSms.ERR_TYPE_GENERIC_PERMANENT;
//        boolean isFailedSms = isSms()
//                            && (mBoxId == Sms.MESSAGE_TYPE_FAILED);
//        return isFailedMms || isFailedSms;
    }


    public boolean isSms() {
        return mType.equals("sms");
    }

    /** M: add for OP09 Feature, mms cc.
     *  the format of this two argument is
     *  cc: 10086,xxx@xx.xx. i.e. comma seperated encoded addresses.
     *  encoding: char-set,char-set. the corresponding encoding of each address,
     *  encoding is comma seperated too.
     *  there is no last comma.
     *  return: <CC: addresses with comma seperated.>
     */
    private String getMmsCcString(String cc, String encoding) {
        String cclist = null;
        if (cc != null && MessageUtils.isSupportSendMmsWithCc()) {
            String[] addresses = null;
            String[] encodings = null;
            if (encoding == null) {
                return cc;
            } else {
                /// M: they should be same length.
                addresses = cc.split(",");
                encodings = encoding.split(",");
                int size = addresses.length;
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    int charset = Integer.parseInt(encodings[i]);
                    EncodedStringValue v =
                            new EncodedStringValue(charset, PduPersister.getBytes(addresses[i]));
                    builder.append(v.getString());
                    if ((i + 1) != size) {
                        builder.append(",");
                    }
                }
                cc = builder.toString();
            }
            return cc;
        }
        return cclist;
    }
}
