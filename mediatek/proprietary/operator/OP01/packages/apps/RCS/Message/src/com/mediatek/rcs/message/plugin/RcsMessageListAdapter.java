package com.mediatek.rcs.message.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.mms.ui.MessageListAdapter.ColumnsMap;

import com.mediatek.mms.callback.IColumnsMapCallback;
import com.mediatek.mms.ipmessage.DefaultIpColumnsMapExt;
import com.mediatek.mms.ipmessage.DefaultIpMessageListAdapterExt;
import com.mediatek.mms.ipmessage.IIpColumnsMapExt;
import com.mediatek.mms.ipmessage.IIpMessageListItemExt;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RcsLog.ThreadsColumn;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.ui.MsgListItem;


/**
 * Plugin implements. response MessageListAdapter.java in MMS host.
 *
 */
public class RcsMessageListAdapter extends DefaultIpMessageListAdapterExt {
    private static String TAG = "RcseMessageListAdapter";
    private static final String SMS_IP_MESSAGE_ID = Telephony.Sms.IPMSG_ID;
    static final String RCS_TYPE = "rcs";

    /// M: message type
    private static final int INCOMING_ITEM_TYPE = 0;
    private static final int OUTCOMING_ITEM_TYPE = 1;
    private static final int INCOMING_ITEM_TYPE_IPMSG = 2;
    private static final int OUTGOING_ITEM_TYPE_IPMSG = 3;
    private static final int SYSTEM_EVENT_ITEM_TYPE = 4;
    private static final int UNKNOWN_ITEM_TYPE = -1;

    /// M: query item culumns
    static final int COLUMN_MSG_TYPE            = 0;
    static final int COLUMN_THREAD_ID           = 2;
    static final int COLUMN_SMS_ADDRESS         = 3;
    static final int COLUMN_SMS_BODY            = 4;
    static final int COLUMN_SMS_DATE            = 5;
    static final int COLUMN_SMS_DATE_SENT       = 6;
    static final int COLUMN_SMS_TYPE            = 8;
    static final int COLUMN_SMS_STATUS          = 9;
    static final int COLUMN_MMS_MESSAGE_BOX     = 18;
    static final int COLUMN_SMS_SUBID           = 24;
    static final int COLUMN_MMS_SUBID           = 25;
    static final int COLUMN_SMS_IP_MESSAGE_ID   = 28;
//    static final int COLUMN_SMS_SPAM            = 29;

    static final int COLUMN_SIM_MESSAGE_COLUMN_MAX = 20;

    static final int CHAT_TYPE_ONE2ONE = 1;
    static final int CHAT_TYPE_ONE2MULTI = 2;
    static final int CHAT_TYPE_GROUP = 3;

    static final int IP_VIEW_TYPE_COUNT = 5;
    private Cursor mCursor;
    private long mThreadId;
    private Context mHostContext;
    private Context mPluginContext;
    private Context mLayoutContext = null;
    private boolean misChatActive = true;
    private int mChatType;
    private boolean mIsSimMessage;
    private Handler mMsgListItemHandler;
    private HashSet<OnMessageListChangedListener> mListChangedListeners
                            = new HashSet<OnMessageListChangedListener>(2);
    RCSColumnsMap mRcsColumnsMap;

    public static final String[] RCS_MESSAGE_PROJECTION_EXTENDS = {
        MessageColumn.IPMSG_ID,
        MessageColumn.STATE,
        MessageColumn.CLASS,
        MessageColumn.FILE_PATH,
        MessageColumn.MESSAGE_ID,
        MessageColumn.CHAT_ID,
        MessageColumn.CONTACT_NUMBER,
        MessageColumn.BODY,
        MessageColumn.TIMESTAMP,
        MessageColumn.MESSAGE_STATUS,
        MessageColumn.TYPE,
        MessageColumn.DIRECTION,
        MessageColumn.MIME_TYPE,
        MessageColumn.SUB_ID
    };

    public RcsMessageListAdapter(Context pluginContext) {
        mPluginContext = pluginContext;
    }

    public void onCreate(Context context, IIpColumnsMapExt columnsMap) {
        mHostContext = context;
        mRcsColumnsMap = (RCSColumnsMap)columnsMap;
    }

    @Override
    public View onIpNewView(LayoutInflater inflater, Cursor cursor, ViewGroup parent) {
        // / M: add for ipmessage
        int columnCount = cursor.getColumnCount();
        Log.d(TAG, "onIpNewView, columnCount = " + columnCount);
        if (columnCount < COLUMN_SIM_MESSAGE_COLUMN_MAX) {
            mIsSimMessage = true;
            return null;
        }
        if (mLayoutContext == null) {
            //create plugin context and use the theme that is same as host
            try {
                mLayoutContext = inflater.getContext().createPackageContext(
                        "com.mediatek.rcs.message",
                        Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                mLayoutContext.setTheme(R.style.MmsTheme);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "[onIpNewView]e = " + e);
            }
        }
        mThreadId = cursor.getLong(COLUMN_THREAD_ID);
        Log.d(TAG, "onIpNewView(): threadId = " + mThreadId);

        LayoutInflater pInflater = LayoutInflater.from(mLayoutContext);
        View retView = null;
        switch (getIpItemViewType(cursor)) {
            case INCOMING_ITEM_TYPE:
                retView = pInflater.inflate(R.layout.message_list_item_recv, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_INCOMING);
                break;
            case OUTCOMING_ITEM_TYPE:
                retView = pInflater.inflate(R.layout.message_list_item_send, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_OUTGOING);
                break;
            case INCOMING_ITEM_TYPE_IPMSG:
                retView = pInflater.inflate(R.layout.message_list_item_recv_ipmsg, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_INCOMING);
                break;
            case OUTGOING_ITEM_TYPE_IPMSG:
                retView = pInflater.inflate(R.layout.message_list_item_send_ipmsg, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE, RcsMessageListItem.TYPE_OUTGOING);
                break;
            case SYSTEM_EVENT_ITEM_TYPE:
                retView = pInflater.inflate(R.layout.message_list_item_recv_ipmsg, parent, false);
                retView.setTag(RcsMessageListItem.TAG_ITEM_TYPE,
                                    RcsMessageListItem.TYPE_SYSTEM_EVENT);
                break;
            default:
                return null;
        }
        if (retView != null) {
            retView.setTag(RcsMessageListItem.TAG_THREAD_ID, mThreadId);
        }
        return retView;
    }

    @Override
    public View onIpBindView(IIpMessageListItemExt mListItem, Context context, Cursor cursor) {
        Log.d(TAG, "onIpBindView()");
        if (mIsSimMessage) {
            return null;
        }
        mListItem.setIpMessageListItemAdapter(this);
        RcsMessageListItem item = (RcsMessageListItem) mListItem;
        item.setMsgListItemHandler(mMsgListItemHandler);
        if (cursor.isLast()) {
            item.setIsLastItem(true);
        }
        return null;
    }

    /**
     * set message list item handler.
     * @param handler
     */
    public void setMsgListItemHandler(Handler handler) {
        mMsgListItemHandler = handler;
    }

    @Override
    public int getIpItemViewType(Cursor cursor) {
        /// M: add for ipmessage
        if (mIsSimMessage) {
            return  -1;
        }
        String type = cursor.getString(mRcsColumnsMap.getColumnMsgType());
        Log.d(TAG, "getIpItemViewType(): message type = " + type);

        int boxId;
        if ("sms".equals(type)) {
            /// M: check sim sms and set box id
            long status = cursor.getLong(COLUMN_SMS_STATUS);
            boolean isSimMsg = false;
            if (status == SmsManager.STATUS_ON_ICC_SENT
                    || status == SmsManager.STATUS_ON_ICC_UNSENT) {
                isSimMsg = true;
                boxId = Sms.MESSAGE_TYPE_SENT;
            } else if (status == SmsManager.STATUS_ON_ICC_READ
                    || status == SmsManager.STATUS_ON_ICC_UNREAD) {
                isSimMsg = true;
                boxId = Sms.MESSAGE_TYPE_INBOX;
            } else {
                boxId = cursor.getInt(mRcsColumnsMap.getColumnSmsType());
            }
        } else if ("mms".equals(type)) {
            boxId = cursor.getInt(COLUMN_MMS_MESSAGE_BOX);
        } else if ("rcs".equals(type)) {
            int rcsMessageClass = cursor.getInt(mRcsColumnsMap.mColumnRcsMessageClass);
            int rcsDirection = cursor.getInt(mRcsColumnsMap.mColumnRcsMessageDirection);
            Log.d(TAG, "getItemViewType(): rcsMessageType = " + rcsMessageClass);
            if (rcsMessageClass >= Class.SYSTEM) {
                return SYSTEM_EVENT_ITEM_TYPE;
            } else {
                if (rcsDirection == Direction.INCOMING) {
                    return INCOMING_ITEM_TYPE_IPMSG;
                } else {
                    return OUTGOING_ITEM_TYPE_IPMSG;
                }
            }
        } else {
            return UNKNOWN_ITEM_TYPE;
        }
        return boxId == Mms.MESSAGE_BOX_INBOX ? INCOMING_ITEM_TYPE : OUTCOMING_ITEM_TYPE;
    }

    @Override
    public int getIpViewTypeCount() {
        Log.d(TAG, "getIpViewTypeCount(): " + IP_VIEW_TYPE_COUNT);
        if (mIsSimMessage) {
            return -1;
        } else {
            return IP_VIEW_TYPE_COUNT;
        }
    }

    @Override
    public boolean isEnabled(Cursor cursor, int position) {
        if (!mIsSimMessage) {
            int curPosition = cursor.getPosition();
            cursor.moveToPosition(position);
            int type = getIpItemViewType(cursor);
            if (type == SYSTEM_EVENT_ITEM_TYPE) {
                return false;
            }
        }
        return super.isEnabled(cursor, position);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        mCursor = cursor;
        for (OnMessageListChangedListener l : mListChangedListeners) {
            l.onChanged();
        }
    }

    /**
     * set chat active state.
     * @param active
     */
    public void setChatActive(boolean active) {
        misChatActive = active;
    }

    /**
     * Is Chat Active, mainly used for group chat.
     * @return true if active, else return false;
     */
    public boolean isChatActive() {
        return misChatActive;
    }

    /**
     * Get select sub id. When used for multi sub id and the message select sub is AUTO.
     * @return The last sub id of message. If sub id in setting is not auto, return -1;
     */
    public int getAutoSelectSubId() {
        int subId = -1;
        boolean isValid = false;
        List<SubscriptionInfo> mSubInfoList =  SubscriptionManager.from(mPluginContext).
                                                    getActiveSubscriptionInfoList();
        int mSubCount = mSubInfoList.isEmpty() ? 0 : mSubInfoList.size();
        long subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
        if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
            if (mCursor != null && mCursor.moveToLast()) {
                subId = getSubIdFromCursor(mCursor);
            }
            for (int i = 0; i < mSubCount; i++) {
                if ((int) mSubInfoList.get(i).getSubscriptionId() == subId) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                subId = -1;
            }
        }
        Log.d(TAG, "getAutoSelectSubId subId = " + subId);
        return subId;
    }

    private int getSubIdFromCursor(Cursor c) {
        int subId = -1;
        try {
            String type = c.getString(COLUMN_MSG_TYPE);
            if (type.equals("mms")) {
                subId = c.getInt(COLUMN_MMS_SUBID);
            } else if (type.equals("sms")) {
                subId = c.getInt(COLUMN_SMS_SUBID);
            }
        } catch (Exception e) {
            Log.d(TAG, "getSimId error happens, please check!");
        } finally {
            Log.d(TAG, "getSimId id = " + subId);
            return subId;
        }
    }

    /**
     * Set Chat Type. The type is {@link #CHAT_TYPE_ONE2ONE},{@link #CHAT_TYPE_ONE2MULTI},
     * {@link #CHAT_TYPE_GROUP}.
     * @param type
     */
    public void setChatType(int type) {
        mChatType = type;
    }

    /**
     * Get Chat Type.
     * @return The type is {@link #CHAT_TYPE_ONE2ONE},{@link #CHAT_TYPE_ONE2MULTI},
     * {@link #CHAT_TYPE_GROUP}.
     */
    public int getChatType() {
        return mChatType;
    }

    /**
     * Whether the message list only has group system msg. Only used for group chat.
     * @return
     */
    public boolean isOnlyHasSystemMsg() {
        if (mCursor != null && !mCursor.isClosed()) {
            if (mCursor.moveToFirst()) {
                do {
                    String type = mCursor.getString(COLUMN_MSG_TYPE);
                    if (!type.equals(RCS_TYPE)) {
                        return false;
                    }
                } while(mCursor.moveToNext());
            }
        }
        return true;
    }

    /**
     * addOnMessageListChangedListener
     * @param l OnMessageListChangedListener
     * @return true if add success, else false
     */
    public boolean addOnMessageListChangedListener(OnMessageListChangedListener l) {
        return mListChangedListeners.add(l);
    }

    /**
     * removeOnMessageListChangedListener
     * @param l OnMessageListChangedListener
     * @return true if remove success, else false
     */
    public boolean removeOnMessageListChangedListener(OnMessageListChangedListener l) {
        return mListChangedListeners.remove(l);
    }

    /**
     * OnMessageListChangedListener. when message list is changed, will call onChanged();
     *
     */
    public interface OnMessageListChangedListener {
        public void onChanged();
    }

    public static class RCSColumnsMap extends DefaultIpColumnsMapExt
                            implements IColumnsMapCallback {
        private IColumnsMapCallback mColumnCallback;
        private ColumnsMap mHostColumnsMap;
        public int mColumnRcsMessageIpMsgId;
        public int mColumnRcsMessageState;
        public int mColumnRcsMessageClass;
        public int mColumnRcsMessageFilePath;
        public int mColumnRcsMessageId;
        public int mColumnRcsMessageChatId;
        public int mColumnRcsMessageAddress;
        public int mColumnRcsMessageBody;
        public int mColumnRcsMessageTimeStamp;
        public int mColumnRcsMessageStatus;
        public int mColumnRcsMessageType; //IM:1; FT:2
        public int mColumnRcsMessageDirection;
        public int mColumnRcsMessageFTMimeType;
        public int mColumnRcsMessageSubId;
        @Override
        public void onCreate(int maxColumnValue, IColumnsMapCallback callback) {
            mColumnCallback = callback;
            mColumnRcsMessageIpMsgId = maxColumnValue + 1;
            mColumnRcsMessageState = maxColumnValue + 2;
            mColumnRcsMessageClass = maxColumnValue + 3;
            mColumnRcsMessageFilePath = maxColumnValue + 4;
            mColumnRcsMessageId = maxColumnValue + 5;
            mColumnRcsMessageChatId = maxColumnValue + 6;
            mColumnRcsMessageAddress = maxColumnValue + 7;
            mColumnRcsMessageBody = maxColumnValue + 8;
            mColumnRcsMessageTimeStamp = maxColumnValue + 9;
            mColumnRcsMessageStatus = maxColumnValue + 10;
            mColumnRcsMessageType = maxColumnValue + 11;
            mColumnRcsMessageDirection = maxColumnValue + 12;
            mColumnRcsMessageFTMimeType = maxColumnValue + 13;
            mColumnRcsMessageSubId = maxColumnValue + 14;
        }

        @Override
        public void onCreate(Cursor cursor, IColumnsMapCallback callback) {
            mColumnCallback = callback;
            mColumnRcsMessageIpMsgId = getColumnIndexByCursor(cursor, MessageColumn.IPMSG_ID);
            mColumnRcsMessageState = getColumnIndexByCursor(cursor, MessageColumn.STATE);
            mColumnRcsMessageClass = getColumnIndexByCursor(cursor, MessageColumn.CLASS);
            mColumnRcsMessageFilePath = getColumnIndexByCursor(cursor, MessageColumn.FILE_PATH);
            mColumnRcsMessageId = getColumnIndexByCursor(cursor, MessageColumn.MESSAGE_ID);
            mColumnRcsMessageChatId = getColumnIndexByCursor(cursor, MessageColumn.CHAT_ID);
            mColumnRcsMessageAddress = getColumnIndexByCursor(cursor, MessageColumn.CONTACT_NUMBER);
            mColumnRcsMessageBody = getColumnIndexByCursor(cursor, MessageColumn.BODY);
            mColumnRcsMessageTimeStamp = getColumnIndexByCursor(cursor, MessageColumn.TIMESTAMP);
            mColumnRcsMessageStatus = getColumnIndexByCursor(cursor, MessageColumn.MESSAGE_STATUS);
            mColumnRcsMessageType = getColumnIndexByCursor(cursor, MessageColumn.TYPE);
            mColumnRcsMessageDirection = getColumnIndexByCursor(cursor, MessageColumn.DIRECTION);
            mColumnRcsMessageFTMimeType = getColumnIndexByCursor(cursor, MessageColumn.MIME_TYPE);
            mColumnRcsMessageSubId = getColumnIndexByCursor(cursor, MessageColumn.SUB_ID);
        }

        private static int getColumnIndexByCursor(Cursor c, String columnName) {
            int columnIndex = -1;
            try {
                columnIndex = c.getColumnIndexOrThrow(columnName);
            } catch (IllegalArgumentException e) {
                Log.w("RCSColumnsMap", e.getMessage());
            }
            return columnIndex;
        }

        @Override
        public int getColumnMmsCc() {
            return mColumnCallback.getColumnMmsCc();
        }

        @Override
        public int getColumnMmsCcEncoding() {
            return mColumnCallback.getColumnMmsCcEncoding();
        }

        @Override
        public int getColumnMmsSubId() {
            return mColumnCallback.getColumnMmsSubId();
        }

        @Override
        public int getColumnMsgId() {
            return mColumnCallback.getColumnMsgId();
        }

        @Override
        public int getColumnMsgType() {
            return mColumnCallback.getColumnMsgType();
        }

        @Override
        public int getColumnSmsAddress() {
            return mColumnCallback.getColumnSmsAddress();
        }

        @Override
        public int getColumnSmsBody() {
            return mColumnCallback.getColumnSmsBody();
        }

        @Override
        public int getColumnSmsIpMessageId() {
            return mColumnCallback.getColumnSmsIpMessageId();
        }

        @Override
        public int getColumnSmsSubId() {
            return mColumnCallback.getColumnSmsSubId();
        }

        @Override
        public int getColumnSmsType() {
            return mColumnCallback.getColumnSmsType();
        }

        /**
         * Get column index of thread id.
         * @param c Cursor
         * @return if not exist this column, return -1;
         */
        public static int getColumnThreadId(Cursor c) {
            return getColumnIndexByCursor(c, "thread_id");
        }

        /**
         * Get column index of SMS date.
         * @param c Cursor
         * @return if not exist this column, return -1
         */
        public static int getColumnSmsDate(Cursor c) {
            return getColumnIndexByCursor(c, Sms.DATE);
        }

        /**
         * Get column index of SMS Sent date.
         * @param c Cursor
         * @return if not exist this column, return -1
         */
        public static int getColumnSmsSentDate(Cursor c) {
            return getColumnIndexByCursor(c, Sms.DATE_SENT);
        }

        public static int getColumnSmsLocked(Cursor c) {
            return getColumnIndexByCursor(c, Sms.LOCKED);
        }

        public static int getColumnMmsBoxId(Cursor c) {
            return getColumnIndexByCursor(c, Mms.MESSAGE_BOX);
        }

        public static int getColumnMmsMessageType(Cursor c) {
            return getColumnIndexByCursor(c, Mms.MESSAGE_TYPE);
        }
    }

    private static long RCS_BASE_KEY = Integer.MAX_VALUE / 2;
    public static long getKey(String type, long msgId) {
        if (type.equals("rcs")) {
            return msgId + RCS_BASE_KEY;
        }
        return 0;
    }

    public static long getRcsMsgIdByKey(long key) {
        if (key > RCS_BASE_KEY) {
            return key - RCS_BASE_KEY;
        }
        return key;
    }

    public static boolean isRcsKey(long key) {
        if (key > RCS_BASE_KEY) {
            return true;
        }
        return false;
    }

    public Cursor getCursor() {
        return mCursor;
    }
}
