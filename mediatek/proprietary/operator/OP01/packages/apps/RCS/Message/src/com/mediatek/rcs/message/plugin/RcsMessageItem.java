package com.mediatek.rcs.message.plugin;

import java.lang.ref.SoftReference;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import android.provider.Telephony.Sms;

import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.MessageItem;
import com.mediatek.mms.callback.IColumnsMapCallback;
import com.mediatek.mms.callback.IMessageItemCallback;
import com.mediatek.mms.ipmessage.DefaultIpMessageItemExt;
import com.mediatek.mms.ipmessage.IIpColumnsMapExt;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.plugin.RcsMessageListAdapter.RCSColumnsMap;
import com.mediatek.rcs.message.utils.BackgroundLoader;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * class RcsMessageItem, plugin implements response MessageItem.
 *
 */
public class RcsMessageItem extends DefaultIpMessageItemExt {
    private static String TAG = "RcseMessageItem";

    IpMessage mIpMessage = null;
    Context mContext;
    Context mResourceContext;
    long mThreadId;
    boolean mIsGroupItem = false;
    long mMsgId;
    int mSubId;
    String mType;
    String mAddress;
    int mBoxId;
    String mSubject;
    int mMessageType;
    int mBurnedMsgTimerNum = 5;
    String mBody;
    long mSentDate;
    long mDate;

    //rcs message only
    long mIpMessageId;
    int mDeliveredState;
    int mRcsMsgClass;
    String mRcsMessageId;
    String mRcsChatId;
    int mRcsStatus;
    int mRcsMsgType; //IM or FT
    int mRcsDirection;
    String mRcsFtFilePath;
    String mRcsFtMimeType;
//    Handler mHandler;

    private SoftReference<Bitmap> mBitmapCache = new SoftReference<Bitmap>(null);
    private int mCacheBitmapWidth;
    private int mCacheBitmapHeight;

    public RcsMessageItem(Context resourceContext) {
        mResourceContext = resourceContext;
    }

    public Bitmap getIpMessageBitmap() {
        return mBitmapCache.get();
    }

    public void setIpMessageBitmapCache(Bitmap bitmap) {
        if (null != bitmap) {
            mBitmapCache = new SoftReference<Bitmap>(bitmap);
        }
    }

    // / M: these 3 methods is matched with setIpMessageBitmapCache.
    public void setIpMessageBitmapSize(int width, int height) {
        mCacheBitmapWidth = width;
        mCacheBitmapHeight = height;
    }

    public int getIpMessageBitmapWidth() {
        return mCacheBitmapWidth;
    }

    public int getIpMessageBitmapHeight() {
        return mCacheBitmapHeight;
    }

    public boolean isReceivedBurnedMessage() {
        Log.d(TAG, " [BurnedMsg]: isReceivedBurnedMessage()");
        IpMessage ipMsg = getIpMessage();
        if (ipMsg == null) {
            Log.d(TAG, " [BurnedMsg]: BurnedMessage: IpMessage is null");
            return false;
        }
        Log.d(TAG, " [BurnedMsg]: BurnedMessage: isReceivedBurnedMessage  getStatus = "
        + ipMsg.getStatus());
        if (ipMsg.getStatus() == MessageStatus.READ
                || ipMsg.getStatus() == MessageStatus.UNREAD) {
            return false;
        }
        boolean isBurned = false;
        if (ipMsg.getType() == IpMessageType.TEXT) {
            isBurned =  ipMsg.getBurnedMessage();
        } else if (ipMsg != null && (ipMsg.getType() == IpMessageType.VIDEO ||
                ipMsg.getType() == IpMessageType.PICTURE ||
                ipMsg.getType() == IpMessageType.VOICE)) {
            isBurned = ((IpAttachMessage) ipMsg).getBurnedMessage();
        }
        Log.d(TAG, " [BurnedMsg]: BurnedMessage: isReceivedBurnedMessage  ipMsg.getType() = "
        + ipMsg.getType()+ " isBurned = "+isBurned);
        return isBurned;
    }

    /**
     * Is sent burned message.
     * @return
     */
    boolean isSentBurnedMessage() {
        return (mRcsDirection == Direction.OUTGOING && mRcsMsgClass == RcsLog.Class.BURN
                    && mRcsStatus == MessageStatus.SENT);
    }

    boolean isBurnedMessage() {
        return mRcsMsgClass == RcsLog.Class.BURN;
    }

    /**
     * for sms and rcs message.
     */
    @Override
    public boolean onIpCreateMessageItem(Context context, Cursor cursor, long msgId, String type,
            int subId, IIpColumnsMapExt ipColumnsMap, IMessageItemCallback msgItemCallback,
            boolean isDrawTimeDivider) {
        /// M: add for ipmessage
        RCSColumnsMap rcsColumnsMap = (RCSColumnsMap) ipColumnsMap;
        mContext = context;
        mMsgId = msgId;
        mType = type;
        int threadIdColumn = RCSColumnsMap.getColumnThreadId(cursor);
        if (threadIdColumn != -1) {
            mThreadId = cursor.getLong(threadIdColumn);
        }
        int columnCount = cursor.getColumnCount();
        if (columnCount <= RcsMessageListAdapter.COLUMN_SIM_MESSAGE_COLUMN_MAX) {
            Log.d(TAG, "onIpCreateMessageItem: columnCount = " + columnCount);
            mIpMessageId = 0;
            return false;
        }
        Log.d(TAG, "[onIpCreateMessageItem]: mType = " + mType + ", mMsgId = " + mMsgId +
                "mThreadId = " + mThreadId);

        if (mType.equals("sms")) {
            mSubId = msgItemCallback.getSubId();
            mAddress = msgItemCallback.getAddress();

            mBody = msgItemCallback.getBody();
            int smsDateColumn = RCSColumnsMap.getColumnSmsDate(cursor);
            if (smsDateColumn != -1) {
                mDate = cursor.getLong(smsDateColumn);
            }
            int smsDateSentColumn = RCSColumnsMap.getColumnSmsSentDate(cursor);
            if (smsDateColumn != -1) {
                mSentDate = cursor.getLong(smsDateColumn);
            } else {
                mSentDate = mDate;
            }
            mBoxId = msgItemCallback.getBoxIdCallback();
        } else if (mType.equals("rcs")) {
            mIpMessageId = cursor.getLong(rcsColumnsMap.mColumnRcsMessageIpMsgId);
            mDeliveredState = cursor.getInt(rcsColumnsMap.mColumnRcsMessageState);
            mRcsMsgClass = cursor.getInt(rcsColumnsMap.mColumnRcsMessageClass);
            mRcsFtFilePath = cursor.getString(rcsColumnsMap.mColumnRcsMessageFilePath);
            mRcsMessageId = cursor.getString(rcsColumnsMap.mColumnRcsMessageId);
            mRcsChatId = cursor.getString(rcsColumnsMap.mColumnRcsMessageChatId);
            mAddress = cursor.getString(rcsColumnsMap.mColumnRcsMessageAddress);
            mBody = cursor.getString(rcsColumnsMap.mColumnRcsMessageBody);
            mDate = cursor.getLong(rcsColumnsMap.mColumnRcsMessageTimeStamp);
            mRcsStatus = cursor.getInt(rcsColumnsMap.mColumnRcsMessageStatus);
            mRcsMsgType = cursor.getInt(rcsColumnsMap.mColumnRcsMessageType);
            mRcsDirection = cursor.getInt(rcsColumnsMap.mColumnRcsMessageDirection);
            mRcsFtMimeType = cursor.getString(rcsColumnsMap.mColumnRcsMessageFTMimeType);
            mSubId = cursor.getInt(rcsColumnsMap.mColumnRcsMessageSubId);
            if (!TextUtils.isEmpty(mRcsChatId)) {
                mIsGroupItem = true;
            }
            if (msgItemCallback != null) {
                msgItemCallback.setAddress(mAddress);
                msgItemCallback.setSubId(mSubId);
                msgItemCallback.setBody(mBody);
                msgItemCallback.setUri(
                        ContentUris.withAppendedId(MessageColumn.CONTENT_URI, mMsgId));
                String timeStamp = MessageUtils.getShortTimeString(context, mDate);
                msgItemCallback.setTimeStamp(timeStamp);
                boolean locked = cursor.getInt(rcsColumnsMap.getColumnSmsLocked(cursor)) != 0;
                msgItemCallback.setLocked(locked);
                msgItemCallback.setDeliveryStatus(MessageItem.DeliveryStatus.NONE);
            }
            if (mRcsMsgType == MessageType.IM && mRcsMsgClass == RcsLog.Class.NORMAL) {
                //normal text content no need other information
                IpTextMessage msg = new IpTextMessage(mMsgId, mRcsMessageId, mRcsDirection);
                msg.setSimId(mSubId);
                msg.setBody(mBody);
                mIpMessage = msg;
            } else if (mRcsMsgClass < RcsLog.Class.SYSTEM) {
                loadIpMessage(msgId);
            }
            if (mIpMessage == null) {
                Log.d(TAG, "MessageItem.init(): ip message is null!");
            }
            return true;
        } else if (mType.endsWith("mms")) {

        } else {
            throw new RuntimeException("unkown type : " + mType + "In RcsMessageItem");
        }
        return false;
    }

    /**
     * add for mms
     */
    public boolean onIpCreateMessageItem(Context context, long msgId, String type, int subId,
            int boxId, String subject, int messageType, String address, long date) {
        mContext = context;
        mIpMessageId = 0;
        mMsgId = msgId;
        mType = type;
        mSubId = subId;
        //for mms
        mBoxId = boxId;
        mSubject = subject;
        mMessageType = messageType;
        mAddress = address;
        mDate = date;
        return false;
    }

    /**
     * add for scrolling
     * @param context
     * @param cursor
     * @return true if handled
     */
    @Override
    public boolean onIpCreateMessageItem(Context context, Cursor cursor, IIpColumnsMapExt map) {
        RCSColumnsMap rcsColumnsMap = (RCSColumnsMap) map;
        mContext = context;
        String type = cursor.getString(rcsColumnsMap.getColumnMsgType());
        if (type.equals("rcs")) {
            //system event
//            mIsSystemEvent = true;
            mRcsMsgClass = cursor.getInt(rcsColumnsMap.mColumnRcsMessageClass);
            mAddress = cursor.getString(rcsColumnsMap.mColumnRcsMessageAddress);
            mBody = cursor.getString(rcsColumnsMap.mColumnRcsMessageBody);
            mRcsMsgType = cursor.getInt(rcsColumnsMap.mColumnRcsMessageType);
            return true;
        }
        return false;
    }

    public boolean isSystemEvent() {
        boolean ret = mRcsMsgClass >= Class.SYSTEM;
        Log.d(TAG, "[isSystemEvent]: ret = " + ret + ", msgId = " + mMsgId);
        return ret;
    }

    public synchronized IpMessage getIpMessage() {
        Log.e(TAG, "[getIpMessage]: ipMessage is " + mIpMessage);
//        Log.e(TAG, "getIpMessage: this is " + this);
        return mIpMessage;
    }

    private synchronized void updateIpMessage(IpMessage msg) {
        mIpMessage = msg;
        Log.e(TAG, "[updateIpMessage]: msg is " + mLoadIpMsgFinishedMessage);
//        Log.e(TAG, "updateIpMessage: this is " + this);
        if (mLoadIpMsgFinishedMessage != null) {
            mLoadIpMsgFinishedMessage.sendToTarget();
        }
    }

    private Message mLoadIpMsgFinishedMessage = null;
    public synchronized void setGetIpMessageFinishMessage(Message msg) {
        Log.d(TAG, "[setGetIpMessageFinishMessage]:  msg = " + msg + ", ipMessage = " + mIpMessage);
        mLoadIpMsgFinishedMessage = msg;
        if (mIpMessage != null && msg != null) {
            msg.sendToTarget();
        }
    }

    public void onItemDetachedFromWindow() {
    }
    Runnable mLoadMsgRunnable;

    private void loadIpMessage(final long msgId) {
        final Object object = new Object();

        mLoadMsgRunnable = new Runnable() {
            @Override
            public void run() {
                IpMessage ipMsg  = RCSMessageManager.getInstance().getRCSMessageInfo(msgId);
//                try {
//                    //TODO test ,must delete when IT
//                    int waitTime = 4000;
                    /// @}
//                    Thread.sleep(waitTime);
//                } catch (InterruptedException ex) {
//                    Log.e(TAG, "wait has been intrrupted", ex);
 //               }

                Log.d(TAG, "async load ipmessage loaded, ipMsg = " + ipMsg);
                updateIpMessage(ipMsg);
                synchronized (object) {
                    object.notifyAll();
                }
            }
        };
        getBackgroundMessageLoader().pushTask(mLoadMsgRunnable);
        synchronized (object) {
            try {
                /// M: Fix ALPS00391886, avoid waiting too long time when many uncached messages
                int waitTime = 400;
                /// @}
                object.wait(waitTime);
            } catch (InterruptedException ex) {
                Log.e(TAG, "wait has been intrrupted", ex);
            }
        }
    }

    public void detached() {
        if (getIpMessage() == null && mLoadMsgRunnable != null) {
            getBackgroundMessageLoader().cacelTask(mLoadMsgRunnable);
        }
    }

    private static BackgroundLoader sBackgroundLoader;
    private static BackgroundLoader getBackgroundMessageLoader() {
        if (sBackgroundLoader == null) {
            sBackgroundLoader = new BackgroundLoader();
        }
        return sBackgroundLoader;
    }

    /**
     * Resend failed message. now support ipmessage only.
     * @return return true if start resend, also return false;
     */
    public boolean resend() {
        Log.d(TAG, "[resend]: mThreadId = " + mThreadId + ", mType = " + mType +
                ", address = " + mAddress + ",mMsgId = " + mMsgId);
        if (!mType.equals("rcs")) {
            Log.e(TAG, "mType is wrong");
            return false;
        }
        if (!RCSServiceManager.getInstance().serviceIsReady()) {
            Toast.makeText(mContext, mResourceContext.getString(R.string.rcs_not_availble),
                                Toast.LENGTH_SHORT).show();
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                RCSMessageManager msgManager = RCSMessageManager.getInstance();
                msgManager.resendRCSMessage(mMsgId);
            }
        }).start();
        return true;
    }


    @Override
    public boolean isOutgoingMessage() {
        if (mType.equals("rcs")) {
            boolean isOutgoing = mRcsStatus == RcsLog.MessageStatus.SENDING
                                || mRcsStatus == RcsLog.MessageStatus.TO_SEND
                                || mRcsStatus == RcsLog.MessageStatus.FAILED;
            Log.d(TAG, "isOutgoingMessage: isOutgoing = " + isOutgoing);
            return isOutgoing;
        }
        return super.isOutgoingMessage();
    }

    @Override
    public boolean isFailedMessage() {
        if (mType.equals("rcs")) {
            boolean failed = mRcsStatus == MessageStatus.FAILED;
            Log.d(TAG, "isFailedMessage: failed = " + failed);
            return failed;
        }
        return super.isFailedMessage();
    }

    boolean isNormalTextMessage() {
         return (mRcsMsgType == RcsLog.MessageType.IM && mRcsMsgClass == RcsLog.Class.NORMAL);
    }

    boolean isRcs() {
        return mType.equals("rcs");
    }

    boolean isSms() {
        return mType.equals("sms");
    }

    boolean isMms() {
        return mType.equals("mms");
    }
}
