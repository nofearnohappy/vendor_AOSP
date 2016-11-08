package com.mediatek.rcs.common.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.mediatek.rcs.common.provider.GroupChatData;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class GroupChatCache {

    private static final String THREAD_NAME = "RcsGroupChatThread";
    private static final String TAG = "GroupChatCache";
    private static HandlerThread sGroupChatThread = null;
    private GroupChatHandler mHandler = null;
    private static Context mContext = null;
    private static final long LOAD_DATA_DELAY_TIME = 1 * 1000;

    private static GroupChatCache sInstance = null;

    private GroupChatObserver mGroupChatObserver = null;

    private static final Map<String, ChatInfo> sChatInfo =
            new ConcurrentHashMap<String, ChatInfo>();

    private static final int EVENT_LOAD_ALL_GROUPCHAT_DATA = 1;
    private static final int EVENT_UPDATE_SUBJECT_BY_CHATID = 2;
    private static final int EVENT_REMOVE_BY_CHATID = 4;
    private static final int EVENT_ADD_GROUPCHAT_DATA = 6;
    private static final int EVENT_REFRESH_BY_THREADS = 7;
    private static final int EVENT_UPDATE_NICKNAME_BY_CHATID = 8;
    private static final int EVENT_UPDATE_STATUS_BY_CHATID = 11;
    private static final int EVENT_UPDATE_ISMECHAIRMEN_BY_CHATID = 12;
    private static final int EVENT_UPDATE_SUBID_BY_CHATID = 13;
    private static final int EVENT_CLEAR_ALL_DATA = 14;

    private GroupChatCache(Context context) {
        mContext = context;
        if (sGroupChatThread == null) {
            sGroupChatThread = new HandlerThread(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
            sGroupChatThread.start();
        }
        if (mHandler == null) {
            mHandler = new GroupChatHandler(sGroupChatThread.getLooper());
        }
        mGroupChatObserver = new GroupChatObserver(mHandler);
        mContext.getContentResolver().registerContentObserver(GroupChatData.CONTENT_URI,
                true, mGroupChatObserver);
        loadAllGroupChat(mContext);
    }

    public synchronized static void createInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GroupChatCache(context);
        }
    }

    public synchronized static GroupChatCache getInstance() {
        return sInstance;
    }

    public synchronized void clearData() {
        sChatInfo.clear();
        Message msg = new Message();
        msg.what = EVENT_CLEAR_ALL_DATA;
        mHandler.sendMessage(msg);
    }

    public synchronized ChatInfo getInfoByChatId(String chatId) {
        if (sChatInfo == null) {
            return null;
        }
        if (chatId != null && sChatInfo.containsKey(chatId)) {
            return sChatInfo.get(chatId);
        }
        logD("getInfoByChatId, cannot find, not a group chat, chatId = " + chatId);
        return null;
    }

    public synchronized void updateSubId(String chatId, int subId) {
        ChatInfo info = sChatInfo.get(chatId);
        logD("updateSubId subId=" + subId +
                ", info = " + (info != null ? chatId : "null"));
        if (info != null) {
            String subject = info.getSubject();
            String nickName = info.getNickName();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            sChatInfo.remove(chatId);
            sChatInfo.put(chatId,
                    new ChatInfo(chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_SUBID_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(GroupChatData.KEY_CHAT_ID, chatId);
            data.putInt(GroupChatData.KEY_SUB_ID, subId);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    public synchronized void updateSubjectByChatId(String chatId, String subject) {
        ChatInfo info = sChatInfo.get(chatId);
        logD("updateSubjectByChatId subject=" + subject +
                ", info = " + (info != null ? chatId : "null"));
        if (info != null) {
            String nickName = info.getNickName();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            sChatInfo.remove(chatId);
            sChatInfo.put(chatId,
                    new ChatInfo(chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_SUBJECT_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(GroupChatData.KEY_CHAT_ID, chatId);
            data.putString(GroupChatData.KEY_SUBJECT, subject);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    public synchronized void updateStatusByChatId(String chatId, long status) {
        ChatInfo info = sChatInfo.get(chatId);
        logD("updateStatusByChatId status=" + status +
                ", info = " + (info != null ? chatId : "null"));
        if (info != null) {
            String subject = info.getSubject();
            String nickName = info.getNickName();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            sChatInfo.remove(chatId);
            sChatInfo.put(chatId,
                    new ChatInfo(chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_STATUS_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(GroupChatData.KEY_CHAT_ID, chatId);
            data.putLong(GroupChatData.KEY_STATUS, status);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    public synchronized void updateNickNameByChatId(String chatId, String nickName) {
        ChatInfo info = sChatInfo.get(chatId);
        logD("updateNickNameByChatId nickName=" + nickName +
                ", info = " + (info != null ? chatId : "null"));
        if (info != null) {
            String subject = info.getSubject();
            long status = info.getStatus();
            boolean isMeChairmen = info.isMeChairmen();
            int subId = info.getSubId();
            sChatInfo.remove(chatId);
            sChatInfo.put(chatId,
                    new ChatInfo(chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_NICKNAME_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(GroupChatData.KEY_CHAT_ID, chatId);
            data.putString(GroupChatData.KEY_NICKNAME, nickName);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    public synchronized void updateIsMeChairmenByChatId(String chatId, boolean isMeChairmen) {
        ChatInfo info = sChatInfo.get(chatId);
        logD("updateIsMeChairmenByChatId isMeChairmen=" + isMeChairmen +
                ", info = " + (info != null ? chatId : "null"));
        if (info != null && info.isMeChairmen() != isMeChairmen) {
            String subject = info.getSubject();
            long status = info.getStatus();
            String nickName = info.getNickName();
            int subId = info.getSubId();
            logD("updateIsMeChairmenByChatId, find info by given chatId=" + chatId);
            sChatInfo.remove(chatId);
            sChatInfo.put(chatId,
                    new ChatInfo(chatId, subject, nickName, status, isMeChairmen, subId));

            Message msg = new Message();
            msg.what = EVENT_UPDATE_ISMECHAIRMEN_BY_CHATID;
            Bundle data = new Bundle();
            data.putString(GroupChatData.KEY_CHAT_ID, chatId);
            data.putBoolean(GroupChatData.KEY_ISCHAIRMEN, isMeChairmen);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    public synchronized void removeByChatId(String chatId) {
        logD("removeByChatId, chatId=" + chatId);
        sChatInfo.remove(chatId);
        Message msg = new Message();
        msg.what = EVENT_REMOVE_BY_CHATID;
        Bundle data = new Bundle();
        data.putString(GroupChatData.KEY_CHAT_ID, chatId);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public synchronized void addChatInfo(String chatId, String subject, long status,
            boolean isMeChairmen) {
        logD("addChatInfo, chatId=" + chatId + ", subject=" + subject);
        ChatInfo info = sChatInfo.get(chatId);
        if (info != null) {
            sChatInfo.remove(chatId);
        }
        sChatInfo.put(chatId,
                new ChatInfo(chatId, subject, null, status, isMeChairmen, RCSUtils.getRCSSubId()));
        Message msg = new Message();
        msg.what = EVENT_ADD_GROUPCHAT_DATA;
        Bundle data = new Bundle();
        data.putString(GroupChatData.KEY_CHAT_ID, chatId);
        data.putString(GroupChatData.KEY_SUBJECT, subject);
        data.putLong(GroupChatData.KEY_STATUS, status);
        data.putBoolean(GroupChatData.KEY_ISCHAIRMEN, isMeChairmen);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    @Override
    public void finalize() {
        mContext.getContentResolver().unregisterContentObserver(mGroupChatObserver);
    }

    public void refreshByThreadsAfterDelete() {
        mHandler.sendEmptyMessage(EVENT_REFRESH_BY_THREADS);
    }

    private final class GroupChatHandler extends Handler {
        public GroupChatHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case EVENT_LOAD_ALL_GROUPCHAT_DATA:
                loadAllGroupChat(mContext);
                break;
            case EVENT_UPDATE_SUBJECT_BY_CHATID:
                updateSubjectByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_STATUS_BY_CHATID:
                updateStatusByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_ISMECHAIRMEN_BY_CHATID:
                updateIsMeChairmenByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_NICKNAME_BY_CHATID:
                updateNickNameByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_UPDATE_SUBID_BY_CHATID:
                updateSubIdByChatIdInDB(mContext, msg.getData());
                break;
            case EVENT_REMOVE_BY_CHATID:
                removeByChatIdFromDB(mContext, msg.getData());
                break;
            case EVENT_ADD_GROUPCHAT_DATA:
                addChatInfoToDB(mContext, msg.getData());
                break;
            case EVENT_REFRESH_BY_THREADS:
                // refreshByThreadsInternal(mContext);
                break;
            case EVENT_CLEAR_ALL_DATA:
                clearAllData();
                break;
            default:
                break;
            }
        }
    }

    private synchronized void loadAllGroupChat(Context context) {
        Cursor cursor = null;
        try {
            cursor = GroupChatUtils.getInstance(context).getAllGroupChat();
            if (cursor != null) {
                sChatInfo.clear();
                while (cursor.moveToNext()) {
                    ChatInfo info = new ChatInfo(cursor);
                    logD("loadAllGroupChat, chatId="
                            + info.getChatId() + ", subject=" + info.getSubject());
                    sChatInfo.put(info.getChatId(), info);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "[loadAllGroupChat] exception : e = " + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updateSubjectByChatIdInDB(Context context, Bundle data) {
        if (data == null || !data.containsKey(GroupChatData.KEY_CHAT_ID)
                || !data.containsKey(GroupChatData.KEY_SUBJECT)) {
            return;
        }
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        String subject = data.getString(GroupChatData.KEY_SUBJECT);
        GroupChatUtils.getInstance(context).updateSubject(chatId, subject);
    }

    private void updateIsMeChairmenByChatIdInDB(Context context, Bundle data) {
        if (data == null || !data.containsKey(GroupChatData.KEY_CHAT_ID)
                || !data.containsKey(GroupChatData.KEY_ISCHAIRMEN)) {
            return;
        }
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        boolean isMeChairmen = data.getBoolean(GroupChatData.KEY_ISCHAIRMEN);
        GroupChatUtils.getInstance(context).updateChairmen(chatId, isMeChairmen);
    }

    private void updateStatusByChatIdInDB(Context context, Bundle data) {
        if (data == null || !data.containsKey(GroupChatData.KEY_CHAT_ID)
                || !data.containsKey(GroupChatData.KEY_STATUS)) {
            return;
        }
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        long status = data.getLong(GroupChatData.KEY_STATUS);
        GroupChatUtils.getInstance(context).updateStatus(chatId, status);
    }

    private void updateNickNameByChatIdInDB(Context context, Bundle data) {
        if (data == null || !data.containsKey(GroupChatData.KEY_CHAT_ID)
                || !data.containsKey(GroupChatData.KEY_NICKNAME)) {
            return;
        }
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        String nickName = data.getString(GroupChatData.KEY_NICKNAME);
        GroupChatUtils.getInstance(context).updateNickName(chatId, nickName);
    }

    private void updateSubIdByChatIdInDB(Context context, Bundle data) {
        if (data == null || !data.containsKey(GroupChatData.KEY_CHAT_ID)
                || !data.containsKey(GroupChatData.KEY_SUB_ID)) {
            return;
        }
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        int subId = data.getInt(GroupChatData.KEY_SUB_ID);
        GroupChatUtils.getInstance(context).updateSubId(chatId, subId);
    }

    private void removeByChatIdFromDB(Context context, Bundle data) {
        if (data == null || !data.containsKey(GroupChatData.KEY_CHAT_ID)) {
            return;
        }
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        GroupChatUtils.getInstance(context).deleteGroupChatData(chatId);
    }

    private void addChatInfoToDB(Context context, Bundle data) {
        String chatId = data.getString(GroupChatData.KEY_CHAT_ID);
        String subject = data.getString(GroupChatData.KEY_SUBJECT);
        long status = data.getLong(GroupChatData.KEY_STATUS);
        int isMeChairmen = data.getBoolean(GroupChatData.KEY_ISCHAIRMEN) ? 1 : 0;
        GroupChatUtils.getInstance(context).insertGroupChatData(chatId,
                subject, status, isMeChairmen);
    }

    private int clearAllData() {
        return GroupChatUtils.getInstance(mContext).deleteAllData();
    }

    private static void logD(String string) {
        Logger.d(TAG, string);
    }

    class GroupChatObserver extends ContentObserver {

        private Handler mHandler;
        public GroupChatObserver(Handler handler) {
            super(handler);
            mHandler = handler;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mHandler.hasMessages(EVENT_LOAD_ALL_GROUPCHAT_DATA)) {
                mHandler.removeMessages(EVENT_LOAD_ALL_GROUPCHAT_DATA);
            }
            mHandler.sendEmptyMessageDelayed(EVENT_LOAD_ALL_GROUPCHAT_DATA, LOAD_DATA_DELAY_TIME);
        }
    }

    public class ChatInfo {

        private String mChatId;
        private String mSubject;
        private String mNickName;
        private long mStatus;
        private boolean mIsMeChairmen;
        private int mSubId;

        ChatInfo(Cursor cursor) {
            if (cursor != null && cursor.getPosition() > -1) {
                mChatId = cursor.getString(cursor.getColumnIndex(GroupChatData.KEY_CHAT_ID));
                mSubject = cursor.getString(cursor.getColumnIndex(GroupChatData.KEY_SUBJECT));
                mNickName = cursor.getString(cursor.getColumnIndex(GroupChatData.KEY_NICKNAME));
                mStatus = cursor.getLong(cursor.getColumnIndex(GroupChatData.KEY_STATUS));
                mIsMeChairmen = cursor.getInt(cursor.getColumnIndex(
                        GroupChatData.KEY_ISCHAIRMEN)) > 0 ? true : false;
                mSubId = cursor.getInt(cursor.getColumnIndex(GroupChatData.KEY_SUB_ID));
            }
        }

        ChatInfo(String chatId, String subject) {
            mChatId = chatId;
            mSubject = subject;
            mNickName = null;
            mStatus = 0;
            mIsMeChairmen = false;
            mSubId = RCSUtils.getRCSSubId();
        }

        ChatInfo(String chatId, String subject, String nickName, long status, boolean isMeChairmen,
                int subId) {
            mChatId = chatId;
            mSubject = subject;
            mNickName = nickName;
            mStatus = status;
            mIsMeChairmen = isMeChairmen;
            mSubId = subId;
        }

        public String getChatId() {
            return mChatId;
        }

        public String getSubject() {
            return mSubject;
        }

        public String getNickName() {
            return mNickName;
        }

        public long getStatus() {
            return mStatus;
        }

        public boolean isMeChairmen() {
            return mIsMeChairmen;
        }

        public int getSubId() {
            return mSubId;
        }
    }
}
