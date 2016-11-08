package com.mediatek.rcs.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gsma.joyn.chat.ChatLog;

import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

public class GroupManager {

    private static final String TAG = "GroupManager";
    private static GroupManager sInstance;
    private Context mContext;
    private Context mPluginContext;

    private Map<String, RCSGroup> mGroupList = new ConcurrentHashMap<String, RCSGroup>();
    private List<IInitGroupListener> mListener = new CopyOnWriteArrayList<IInitGroupListener>();
    private Map<String, GroupInfo> mInvitingGroup = new ConcurrentHashMap<String, GroupInfo>();

    public interface IInitGroupListener {

        void onInitGroupResult(int result, long threadId, String chatId);
        void onAcceptGroupInvitationResult(int result, long threadId, String chatId);
        void onRejectGroupInvitationResult(int result, long threadId, String chatId);
    }

    private GroupManager(Context context) {
        Logger.d(TAG, "GroupManager constructor");
        mContext = context;
        mPluginContext = ContextCacher.getPluginContext();
    }

    public static GroupManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("need call createManager to create instance");
        }
        return sInstance;
    }

    public static void createInstance(Context context) {
        Logger.d(TAG, "createInstance");
        if (sInstance == null) {
            sInstance = new GroupManager(context);
        }
    }

    public void addGroupListener(IInitGroupListener listener) {
        mListener.add(listener);
    }

    public void removeGroupListener(IInitGroupListener listener) {
        mListener.remove(listener);
    }

    public List<IInitGroupListener> getAllListeners() {
        return mListener;
    }

    /**
     *Record inviting Group.
     * @param chatId
     * @param subject
     * @param participants
     */
    public void recordInvitingGroup(String chatId, String subject, List<String> participants) {
        mInvitingGroup.put(chatId, new GroupInfo(chatId, subject, participants));
    }

    /**
     * Get inviting group.
     * @param chatId
     * @return
     */
    public GroupInfo getInvitingGroup(String chatId) {
        return mInvitingGroup.get(chatId);
    }

    /**
     * Remove inviting group.
     * @param chatId
     */
    public void removeInvitingGroup(String chatId) {
        mInvitingGroup.remove(chatId);
    }

    /**
     *  Class GroupInfo.
     * @author
     *
     */
    public class GroupInfo {
        private List<String> mParticipants;
        private String mSubject;
        private String mChatId;

        public GroupInfo(String chatId, String subject, List<String> participants) {
            mParticipants = participants;
            mSubject = subject;
            mChatId = chatId;
        }

        public String getSubject() {
            return mSubject;
        }

        public List<String> getParticipants() {
            return mParticipants;
        }
    }

    /**
     * Get Group Info, should not call this function in main thread
     * @param chatId
     * @return
     */
    public RCSGroup getRCSGroup(String chatId) {
        Logger.d(TAG, "getRCSGroup, chatId=" + chatId);
        RCSGroup groupInfo = mGroupList.get(chatId);
        if (groupInfo != null) {
            groupInfo.addReferenceCount();
            return groupInfo;
        }
        String chatSelection = ChatLog.GroupChat.CHAT_ID + "='" + chatId + "'";
        Cursor chatCursor = null;
        Cursor participantCursor = null;
        try {
            chatCursor = mContext.getContentResolver().query(
                    RCSUtils.RCS_URI_GROUP_CHAT, RCSUtils.PROJECTION_GROUP_INFO, chatSelection,
                        null, null);
            if (chatCursor.moveToFirst()) {
                List<Participant> participants = new ArrayList<Participant>();
                String memberSelection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
//                memberSelection  = memberSelection + " AND " + GroupMemberData.COLUMN_STATE +
//                  "<>" + GroupMemberData.STATE.STATE_PENDING;
                participantCursor = mContext.getContentResolver().query(
                        RCSUtils.RCS_URI_GROUP_MEMBER, RCSUtils.PROJECTION_GROUP_MEMBER,
                        memberSelection, null, null);
                String myNickName = null;
                String myNumber = RCSServiceManager.getInstance().getMyNumber();
                while (participantCursor.moveToNext()) {
                    String number = participantCursor.getString(participantCursor.getColumnIndex(
                            GroupMemberData.COLUMN_CONTACT_NUMBER));
                    String name = participantCursor.getString(participantCursor.getColumnIndex(
                            GroupMemberData.COLUMN_CONTACT_NAME));
                    Participant participant = new Participant(number, name);
                    participant.setState(participantCursor.getInt(participantCursor.getColumnIndex(
                            GroupMemberData.COLUMN_STATE)));
                    participants.add(participant);
                    if (PhoneNumberUtils.compare(number, myNumber)) {
                        myNickName = name;
                    }
                }
                groupInfo = new RCSGroup(mContext
                    , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.CHAT_ID))
                    , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.SUBJECT))
                    , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.CHAIRMAN))
                    , chatCursor.getString(chatCursor.getColumnIndex(ChatLog.GroupChat.NICKNAME))
                    , participants
                    , myNickName
                    );
            }
        } finally {
            if (chatCursor != null) {
                chatCursor.close();
            }
            if (participantCursor != null) {
                participantCursor.close();
            }
        }
        mGroupList.put(chatId, groupInfo);
        return groupInfo;
    }

    public void releaseRCSGroup(String chatId) {
        Logger.d(TAG, "releaseRCSGroup, chatId=" + chatId);
        RCSGroup groupInfo = mGroupList.get(chatId);
        if (groupInfo != null && groupInfo.releaseGroup()) {
            mGroupList.remove(chatId);
        }
    }

    public String initGroupChat(HashSet<String> participants, String subject) {
        Logger.d(TAG, "init GroupChat, subject = " + subject);
        List<String> contacts = new ArrayList<String>(participants);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        String chatId = null;
        try {
            chatId = service.initGroupChat(subject, contacts);
            if (!TextUtils.isEmpty(chatId)) {
                recordInvitingGroup(chatId, subject, contacts);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return chatId;
    }

    public boolean acceptGroupInvitation(String chatId) {
        Logger.d(TAG, "acceptGroupInvitation, chatId=" + chatId);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.acceptGroupChat(chatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean rejectGroupInvitation(String chatId) {
        Logger.d(TAG, "rejectGroupInvitation, chatId=" + chatId);
        IRCSChatService service = RCSServiceManager.getInstance().getChatService();
        try {
            service.rejectGroupChat(chatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }
}
