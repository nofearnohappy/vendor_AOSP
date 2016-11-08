package com.mediatek.rcs.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.RcsLog.ThreadFlag;
import com.mediatek.rcs.common.RcsLog.ThreadsColumn;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.binder.RCSServiceManager.INotifyListener;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

import org.gsma.joyn.chat.ChatLog;

public class RCSGroup implements INotifyListener {

    private static final String TAG = "RCSGroup";
    private String mChatId;
    private String mSubject;
    private List<Participant> mParticipants;
    private String mChairmen;
    private String mGroupNickName;
//    private boolean mIsMeChairmen;
    private String mMyNickName;

    private Long mThreadId;
    private int mNotificationEnable = 1;

    private Context mContext;

    private IRCSChatService mService;

    private int mReferenceCount = 0;

    private List<IGroupActionListener> mListener = new CopyOnWriteArrayList<IGroupActionListener>();

    public interface IGroupActionListener {
        void onParticipantAdded(Participant participant);
        void onParticipantLeft(Participant participant);
        void onParticipantRemoved(Participant participant);
        void onChairmenTransferred(Participant newChairmen);
        void onSubjectModified(String newSubject);
        void onNickNameModified(String newNickName);
        void onSelfNickNameModified(String newSelfNickName);
        void onMeRemoved();
        void onGroupAborted();

        void onAddParticipantFail(Participant participant);
        void onAddParticipantsResult(int result);
        void onRemoveParticipantResult(int result);
        void onTransferChairmenResult(int result);
        void onModifySubjectResult(String subject, int result);
        void onModifyNickNameResult(int result);
        void onModifySelfNickNameResult(String selfNickName, int result);
        void onExitGroupResult(int result);
        void onDestroyGroupResult(int result);
    }

    public void addActionListener(IGroupActionListener listener) {
        mListener.add(listener);
    }

    public void removeActionListener(IGroupActionListener listener) {
        mListener.remove(listener);
    }

    public boolean releaseGroup() {
        if (--mReferenceCount <= 0) {
            RCSServiceManager.getInstance().unregistNotifyListener(this);
            return true;
        }
        return false;
    }

    public RCSGroup(Context context, String chatId, String subject, String chairmen,
            String groupNickName, List<Participant> participants, String myNickName) {
        mContext = context;
        RCSServiceManager.getInstance().registNotifyListener(this);
        mService = RCSServiceManager.getInstance().getChatService();
        mChatId = chatId;
        mSubject = subject;
        if (TextUtils.isEmpty(chairmen)) {
            mChairmen = "";
        } else {
            mChairmen = chairmen;
        }
        mGroupNickName = groupNickName;
        mMyNickName = myNickName;
        mParticipants = participants;
        Cursor cursor = getGroupCursor(mChatId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                mThreadId = cursor.getLong(cursor.getColumnIndex(ThreadsColumn.ID));
                mNotificationEnable = cursor.getInt(cursor.getColumnIndex(
                        Telephony.ThreadSettings.NOTIFICATION_ENABLE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mReferenceCount = 1;
        Logger.d(TAG, "new RCSGroup, chatId=" + chatId + ", subject=" + subject
                + ", threadId=" + mThreadId);
    }

    public boolean addParticipants(List<String> participants) {
        Logger.d(TAG, "addParticipants, entry");
        List<Participant> contacts = new ArrayList<Participant>();
        for (String participant : participants) {
            contacts.add(new Participant(participant, null));
        }
        boolean result = true;
        try {
            mService.addParticipants(mChatId, contacts);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean removeParticipants(List<String> participants) {
        Logger.d(TAG, "removeParticipants, entry");
        List<Participant> contacts = new ArrayList<Participant>();
        for (String participant : participants) {
            contacts.add(new Participant(participant, null));
        }
        boolean result = true;
        try {
            mService.removeParticipants(mChatId, contacts);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean modifySubject(String subject) {
        Logger.d(TAG, "modifySubject, subject=" + subject);
        boolean result = true;
        try {
            mService.modifySubject(mChatId, subject);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean modifyNickName(String nickName) {
        Logger.d(TAG, "modifyNickName, nickName=" + nickName);
        boolean result = true;
        try {
            mService.modifyNickName(mChatId, nickName);
            if (result) {
                mGroupNickName = nickName;
                GroupChatCache.getInstance().updateNickNameByChatId(mChatId, nickName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean modifySelfNickName(String selfNickName) {
        Logger.d(TAG, "modifySelfNickName, selfNickName=" + selfNickName);
        boolean result = true;
        try {
            mService.modifyRemoteAlias(mChatId, selfNickName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean transferChairmen(String newChairmen) {
        Logger.d(TAG, "transferChairmen, newChairmen=" + newChairmen);
        boolean result = true;
        try {
            mService.transferChairman(mChatId, newChairmen);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean exitGroup() {
        Logger.d(TAG, "exitGroup, threadId=" + mThreadId);
        boolean result = true;
        try {
            mService.quit(mChatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean destroyGroup() {
        Logger.d(TAG, "destroyGroup, threadId=" + mThreadId);
        boolean result = true;
        try {
            mService.abort(mChatId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Subscribe group conference to get the last info.
     * @return true if subscribe successfully.
     */
    public boolean sendGroupConferenceSubscription() {
        Logger.d(TAG, "sendGroupConferenceSubscription, threadId=" + mThreadId);
        boolean result = true;
//        try {
//            mService.sendGroupConferenceSubscription(mChatId);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//            result = false;
//        }
        return result;
    }

    public String getGroupNickName() {
        return mGroupNickName;
    }

    public String getMyNickName() {
        return mMyNickName;
    }

    public String getChairmen() {
        if (TextUtils.isEmpty(mChairmen)) {
            String selection = ChatLog.GroupChat.CHAT_ID + "='" + mChatId + "'";
            String[] projection = new String[] {ChatLog.GroupChat.CHAIRMAN};
            Cursor cursor = mContext.getContentResolver()
                            .query(RCSUtils.RCS_URI_GROUP_CHAT, projection, selection, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String chairmen = cursor.getString(0);
                        if (TextUtils.isEmpty(chairmen)) {
                            mChairmen = chairmen;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return mChairmen;
    }

    public List<Participant> getParticipants() {
        return mParticipants;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getChatId() {
        return mChatId;
    }

    public boolean isMeChairmen() {
        String myNumber = RCSServiceManager.getInstance().getMyNumber();
        if (!TextUtils.isEmpty(myNumber) && (PhoneNumberUtils.compare(mChairmen, myNumber)
                || (mChairmen != null && mChairmen.contains(myNumber)))) {
            return true;
        }
        return false;
    }

    public int getNotificationEnable() {
        return mNotificationEnable;
    }

    public int addReferenceCount() {
        return ++mReferenceCount;
    }

    public void setNotificationEnable(final int enable) {
        mNotificationEnable = enable;
        Logger.d(TAG, "setNotificationEnable, enable=" + enable);
        boolean block = false;
        if (IpMessageConsts.GroupNotificationType.NOTIFICATION_REJECT == enable) {
            block = true;
        }
        try {
            mService.blockMessages(mChatId, block);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {

            public void run() {
                RCSUtils.setNotificationEnable(mThreadId, enable);
            }
        }).start();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof RCSGroup)) {
            return false;
        }
        RCSGroup group = (RCSGroup) obj;
        if (this.mChatId.equals(group.getChatId())) {
            return true;
        }
        return false;
    }

    public void notificationsReceived(Intent intent) {
        Log.d(TAG, "notificationsReceived: " + intent.getAction());
        String action = intent.getAction();
        // TODO
        String chatId = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_CHAT_ID);
        if (chatId == null || !chatId.equals(mChatId)) {
            Logger.d(TAG, "notificationsReceived: not my chatId=" + chatId);
            return;
        }
        if (action.equals(IpMessageConsts.GroupActionList.ACTION_GROUP_OPERATION_RESULT)) {
            int result = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_RESULT, -1);
            long threadId = intent.getLongExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, 0);
            for (IGroupActionListener listener : mListener) {
                switch (intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, 0)) {
                    case IpMessageConsts.GroupActionList.VALUE_ADD_PARTICIPANTS:
                        mParticipants = RCSDataBaseUtils.getGroupParticipants(mContext, mChatId);
                        listener.onAddParticipantsResult(result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_REMOVE_PARTICIPANTS:
                        listener.onRemoveParticipantResult(result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_TRANSFER_CHAIRMEN:
                        listener.onTransferChairmenResult(result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_MODIFY_NICK_NAME:
                        listener.onModifyNickNameResult(result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_MODIFY_SELF_NICK_NAME:
                        String selfNickName = intent.getStringExtra(
                                IpMessageConsts.GroupActionList.KEY_SELF_NICK_NAME);
                        mMyNickName = selfNickName;
                        listener.onModifySelfNickNameResult(selfNickName, result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_MODIFY_SUBJECT:
                        String subject = intent.getStringExtra(
                                IpMessageConsts.GroupActionList.KEY_SUBJECT);
                        mSubject = subject;
                        listener.onModifySubjectResult(subject, result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_EXIT_GROUP:
                        listener.onExitGroupResult(result);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_DESTROY_GROUP:
                        listener.onDestroyGroupResult(result);
                        break;
                    default:
                        break;
                }
            }
        } else if (action.equals(IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY)) {
            Participant participant = intent.getParcelableExtra(
                    IpMessageConsts.GroupActionList.KEY_PARTICIPANT);
            for (IGroupActionListener listener : mListener) {
                switch (intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, 0)) {
                    case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_JOIN:
                        int index = mParticipants.indexOf(participant);
                        if (index > -1) {
                            Participant part = mParticipants.get(index);
                            part.setState(GroupMemberData.STATE.STATE_CONNECTED);
                        } else {
                            participant.setState(GroupMemberData.STATE.STATE_CONNECTED);
                            mParticipants.add(participant);
                        }
                        listener.onParticipantAdded(participant);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_LEFT:
                        if (mParticipants.contains(participant)) {
                            mParticipants.remove(participant);
                        }
                        listener.onParticipantLeft(participant);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_REMOVED:
                        if (mParticipants.contains(participant)) {
                            mParticipants.remove(participant);
                        }
                        listener.onParticipantRemoved(participant);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_CHAIRMEN_CHANGED:
                        mChairmen = participant.getContact();
                        listener.onChairmenTransferred(participant);
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_SUBJECT_MODIFIED:
                        String newSubject = intent.getStringExtra(
                                IpMessageConsts.GroupActionList.KEY_SUBJECT);
                        if (!mSubject.equals(newSubject)) {
                            mSubject = newSubject;
                            listener.onSubjectModified(newSubject);
                        }
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_ME_REMOVED:
                        listener.onMeRemoved();
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_GROUP_ABORTED:
                        listener.onGroupAborted();
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_NICKNAME_MODIFIED:
                        String contact = intent.getStringExtra(
                                IpMessageConsts.GroupActionList.KEY_CONTACT_NUMBER);
                        String nickName = intent.getStringExtra(
                                IpMessageConsts.GroupActionList.KEY_NICK_NAME);
                        Participant rParti = null;
                        for (Participant parti : mParticipants) {
                            if (PhoneNumberUtils.compare(contact, parti.getContact())) {
                                rParti = parti;
                                break;
                            }
                        }
                        mParticipants.remove(rParti);
                        mParticipants.add(new Participant(contact, nickName));
                        break;
                    case IpMessageConsts.GroupActionList.VALUE_ADD_PARTICIPANT_FAIL:
                        if (mParticipants.contains(participant)) {
                            mParticipants.remove(participant);
                        }
                        listener.onAddParticipantFail(participant);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void finalize() {
        try {
//            RCSServiceManager.getInstance().unregistNotifyListener(this);
            super.finalize();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Cursor getGroupCursor(String chatId) {
        String selection = ThreadsColumn.RECIPIENTS + "='" + chatId + "' AND " +
                ThreadsColumn.FLAG + "=" + ThreadFlag.MTM;
        String[] projection = {ThreadsColumn.ID, ThreadsColumn.RECIPIENTS,
                Telephony.ThreadSettings.NOTIFICATION_ENABLE};
        return ContextCacher.getHostContext().getContentResolver().query(ThreadsColumn.CONTENT_URI,
                projection, selection, null, null);
    }

    public static class SimpleGroupActionListener implements IGroupActionListener {

        @Override
        public void onParticipantAdded(Participant participant) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onParticipantLeft(Participant participant) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onChairmenTransferred(Participant newChairmen) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSubjectModified(String newSubject) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onNickNameModified(String newNickName) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSelfNickNameModified(String newSelfNickName) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onMeRemoved() {
            // TODO Auto-generated method stub
        }

        @Override
        public void onGroupAborted() {
            // TODO Auto-generated method stub
        }

        @Override
        public void onAddParticipantsResult(int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onRemoveParticipantResult(int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onTransferChairmenResult(int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onModifySubjectResult(String subject, int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onModifyNickNameResult(int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onModifySelfNickNameResult(String selfNickName, int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onExitGroupResult(int result) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onDestroyGroupResult(int result) {
            // TODO Auto-generated method stub
        }

        public void onParticipantRemoved(Participant participant) {
            // TODO Auto-generated method stub
        }

        public void onAddParticipantFail(Participant participant) {
            // TODO Auto-generated method stub
        }
    }
}