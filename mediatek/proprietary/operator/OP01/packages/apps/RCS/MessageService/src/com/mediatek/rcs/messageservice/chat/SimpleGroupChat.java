/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */

package com.mediatek.rcs.messageservice.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ConferenceEventData.ConferenceUser;
import org.gsma.joyn.chat.GeolocMessage;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.GroupChatListener;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.provider.GroupChatData;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.ReceiveMessageStruct;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.service.GroupParticipant;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.messageservice.utils.Logger;
import com.mediatek.rcs.messageservice.chat.PendingMessageManager.PendingMessage;


/**
 * The implement of group chat. It provides session management, session event handling, and message
 * sending.
 */

public class SimpleGroupChat extends BaseChatImpl implements ISimpleGroupChat {
    public final String TAG = this.toString().substring(this.toString().lastIndexOf('.') + 1);
    public static HandlerThread sGroupConfigurationThread = null;

    private SessionStateMachine mGroupSession = null;
    private GroupConfigHandler mGroupConfigHandler = null;
    private Object mConfigurationPara = null;

    private SimpleGroupChatData mGroupData = new SimpleGroupChatData();

    private GroupChatListener mGroupListener = null;

    static {
        sGroupConfigurationThread = new HandlerThread("GroupConfig");
        sGroupConfigurationThread.start();
    }

    /**
     * @param tag
     * @param subject
     * @param chatId
     * @param chatWindow
     * @param participants
     */
    public SimpleGroupChat(
            RCSChatServiceBinder servcie, String subject, List<Participant> contacts) {
        super(servcie, null);
        Logger.v(TAG, "Constructor entry");
        mGroupData.invitingParticipants = contacts;
        setSubject(subject);

        mGroupSession = new SessionStateMachine("groupchatState");
        mGroupListener = new SimpleGroupChatListener();

        mGroupConfigHandler = new GroupConfigHandler(sGroupConfigurationThread.getLooper());
        Logger.v(TAG, "Constructor exit");
    }

    public SimpleGroupChat(RCSChatServiceBinder service, String chatId) {
        super(service, chatId);
        Logger.v(TAG, "Constructor entry");
        mGroupData.init(chatId);

        mGroupSession = new SessionStateMachine("groupchatState");
        mGroupListener = new SimpleGroupChatListener();

        mGroupConfigHandler = new GroupConfigHandler(sGroupConfigurationThread.getLooper());
        Logger.v(TAG, "Constructor exit");
    }

    /**
     * @param mChatId
     */
    @Override
    public void setChatId(String chatId) {
        Logger.v(TAG, "GroupChat Id: ##" + chatId + "##");
        mGroupData.chatId = chatId;
        super.setTag(chatId);
    }

    /**
     * @return
     */
    @Override
    public String getChatId() {
        return mGroupData.chatId;
    }

    @Override
    public String getSubject() {
        return mGroupData.subject;
    }

    @Override
    public void setSubject(String subject) {
        Logger.v(TAG, "GroupChat Subject: ##" + subject + "##");
        mGroupData.subject = subject;
    }

    /**
     * @return
     */
    @Override
    public Handler getGroupConfigHandler() {
        return mGroupConfigHandler;
    }

    /**
     * @return
     */
    @Override
    public List<String> getParticipants() {
        List<Participant> participants = mGroupData.getParticipants();
        List<String> contacts = new ArrayList<String>();
        for (Participant participant : participants) {
            contacts.add(participant.getContact());
        }
        return contacts;
    }

    @Override
    public synchronized void startGroup() {
        Logger.d(TAG, "startGroup() entry");
        mGroupSession.sendMessage(OP_GROUP_START);
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param exist
     */
    @Override
    public void handleInvitation(String rejoinId) {
        Logger.d(TAG, "handleInvitation() entry: " + rejoinId);
        mGroupSession.sendMessage(OP_GROUP_INVITATION, rejoinId);
    }

    /**
     *
     */
    @Override
    public void invitationAccepted() {
        Logger.d(TAG, "invitationAccepted() entry");
        mGroupSession.sendMessage(OP_GROUP_INVITATION_ACCEPT);
    }

    /**
     *
     */
    @Override
    public void invitationRejected() {
        Logger.d(TAG, "invitationRejected() entry");
        mGroupSession.sendMessage(OP_GROUP_INVITATION_REJECT);
    }

    @Override
    public void sendChatMessage(final int type, final long msgIdInSMS, final String content) {
        Logger.d(TAG, "sendChatMessage() entry, content: " + content);
        if (RCSUtils.in2GCall(GsmaManager.getContext())) {
            notifyMessageSendFail(msgIdInSMS);
            return;
        }
        // for retry.
        if (!GsmaManager.isServiceAvailable()) {
            Logger.w(TAG, "sendChatMessage() msg save in pending list: " + msgIdInSMS);
            Message sendMsgRst = mWorkHandler.obtainMessage(BASE_OP_SEND_MESSAGE_RST);
            PendingMessage pendingMsg = new PendingMessage(msgIdInSMS, content, type);
            pendingMsg.setChat(PendingMessage.GROUP);
            pendingMsg.setChatId(getChatId());
            pendingMsg.setPendingStartTime(System.currentTimeMillis());

            mService.getPendingMessageManager().addPendingMessage(pendingMsg);
            sendMsgRst.obj = pendingMsg;
            mWorkHandler.sendMessageDelayed(sendMsgRst, MAX_MSG_PENDING_TIME);
            return;
        }
        Message msg = mGroupSession.obtainMessage(OP_SEND_CHATMESSAGE);
        Bundle bundle = new Bundle();
        bundle.putLong("smsmsgid", msgIdInSMS);
        bundle.putString("content", content);
        bundle.putInt("type", type);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    /**
     * @param messageId
     */
    @Override
    public void resendChatMessage(final String messageId) {
        Logger.d(TAG, "resendChatMessage() entry, messageId: " + messageId);

        if (RCSUtils.in2GCall(GsmaManager.getContext())) {
            onReceiveMessageDeliveryStatus(messageId, "failed");
            return;
        }

        if (!GsmaManager.isServiceAvailable()) {
            if (mService.getPendingMessageManager().rcsMsgContains(messageId) == null) {
                Message sendMsgRst = mWorkHandler.obtainMessage(BASE_OP_SEND_MESSAGE_RST);
                PendingMessage pendingMsg = new PendingMessage(messageId);
                pendingMsg.setChat(PendingMessage.GROUP);
                pendingMsg.setChatId(getChatId());
                pendingMsg.setPendingStartTime(System.currentTimeMillis());

                mService.getPendingMessageManager().addPendingMessage(pendingMsg);
                sendMsgRst.obj = pendingMsg;
                mWorkHandler.sendMessageDelayed(sendMsgRst, MAX_MSG_PENDING_TIME);
            }
            Message msg = mWorkHandler.obtainMessage(BASE_OP_RESEND_MESSAGE);
            msg.obj = messageId;
            mWorkHandler.sendMessageDelayed(msg, 5 * 1000);
            return;
        }

        Message msg = mGroupSession.obtainMessage(OP_RESEND_CHATMESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString("resendid", messageId);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    /**
     * @param filePath
     * @param type
     * @param fileTag
     */
    @Override
    public void sendFile(final long msgId, final String filePath) {
        Logger.d(TAG, "sendFile() entry, msgId: " + msgId + " , filePath: " + filePath);
        Message message = mGroupSession.obtainMessage(OP_SEND_FILE);
        Bundle bundle = new Bundle();
        bundle.putLong("msgId", msgId);
        bundle.putString("filepath", filePath);
        message.setData(bundle);
        mGroupSession.sendMessage(message);
    }

    @Override
    public void resendFile(final long msgId) {
        Logger.d(TAG, "sendFile() entry, msgId: " + msgId);
        Message message = mGroupSession.obtainMessage(OP_RESEND_FILE);
        Bundle bundle = new Bundle();
        bundle.putLong("msgId", msgId);
        message.setData(bundle);
        mGroupSession.sendMessage(message);
    }

    @Override
    public void downloadFile(String fileTag) {
        Logger.d(TAG, "downloadFile() entry, #fileTag: " + fileTag);
        Message message = mGroupSession.obtainMessage(OP_DOWNLOAD_FILE);
        Bundle bundle = new Bundle();
        bundle.putString("filetag", fileTag);
        message.setData(bundle);
        mGroupSession.sendMessage(message);
    }

    @Override
    public void redownloadFile(String fileTransferTag) {
        Message message = mGroupSession.obtainMessage(OP_REDOWNLOAD_FILE);
        Bundle bundle = new Bundle();
        bundle.putString("filetag", fileTransferTag);
        message.setData(bundle);
        mGroupSession.sendMessage(message);
    }

    @Override
    public void handleFTInvitation(Intent intent) {
        mGroupSession.sendMessage(OP_FT_INVITATION, intent);
    }

    private void onReceiveChatMessage(ChatMessage message) {
        Logger.d(TAG, "onReceiveChatMessage() entry, messageId: " + message.getId());
        int messageType = message.isEmoticonMessage() ?
                Class.EMOTICON : Class.NORMAL;
        MessageStruct struct = new ReceiveMessageStruct(mContext, getChatId(), message.getContact(),
                message.getMessage(), message.getId(), 0, messageType);
        long msgId = struct.saveMessage();
        mService.getListener().onNewGroupMessage(getChatId(), msgId, message.getContact());
    }

    @Override
    public void notifyMessageSendFail(final long msgId) {
        mService.getListener().onSendGroupMessageFailed(msgId);
    }

    /**
     * @param status
     */
    @Override
    public void onStatusChanged(boolean status) {
        Logger.d(TAG, "onStatusChanged() entry, status: " + status);
        if (status) {
            mGroupSession.sendMessage(NOTIFICATION_REGISTRATION_STATUS_TRUE);
            if (!mGroupData.updateDone) {
                mGroupSession.sendMessage(OP_GROUP_UPDATE_STATUS);
            }
        } else {
            mGroupSession.sendMessage(NOTIFICATION_REGISTRATION_STATUS_FALSE);
        }
    }

    /**
     *
     */
    @Override
    public void onCoreServiceDown() {
        Logger.d(TAG, "onCoreServiceDown() entry");
        // the groupChat object is unavialable
        mGroupSession.sendMessage(NOTIFICATION_CORESERVICE_DOWN);
    }

    /**
     *
     */
    public void updateGroupStatus() {
        Logger.d(TAG, "updateGroupStatus() entry");
        mGroupSession.sendMessage(OP_GROUP_UPDATE_STATUS);
    }

    public void onDestroy() {
        Logger.d(TAG, "onDestroy() entry");
        mGroupSession.destory();
        mGroupData.clear();
        mService.removeGroupChat(getChatId());
    }

    /**
     * @author mtk80881
     *
     */
    private final class GroupConfigHandler extends Handler {
        public final String TAG = SimpleGroupChat.this.TAG + "$GroupConfig";

        public GroupConfigHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case OP_ADD_PARTICIPANT:
                List<Participant> participantList = (List<Participant>) msg.obj;
                mGroupSession.sendMessage(OP_ADD_PARTICIPANT, participantList);
                break;
            case OP_REMOVE_PARTICIPANT:
                List<Participant> participants = (List<Participant>) msg.obj;
                mGroupSession.sendMessage(OP_REMOVE_PARTICIPANT, participants);
                break;
            case OP_MODIFY_SUBJECT:
                String newSubject = (String) msg.obj;
                mGroupSession.sendMessage(OP_MODIFY_SUBJECT, newSubject);
                break;
            case OP_MODIFY_MYNICKNAME:
                String nickname = (String) msg.obj;
                mGroupSession.sendMessage(OP_MODIFY_MYNICKNAME, nickname);
                break;
            case OP_TRANSFER_CHAIRMAN:
                Participant participant = (Participant) msg.obj;
                mGroupSession.sendMessage(OP_TRANSFER_CHAIRMAN, participant);
                break;
            case OP_QUIT_GROUP:
                mGroupSession.sendMessage(OP_QUIT_GROUP);
                break;
            case OP_ABORT_GROUP:
                mGroupSession.sendMessage(OP_ABORT_GROUP);
                break;
            default:
                break;
            }
            // wait for the response;
            lockforResponse();
        }

        public void lockforResponse() {
            synchronized (this) {
                try {
                    Logger.d(TAG, "----LOCKED----");
                    wait();
                } catch (InterruptedException e) {
                    Logger.d(TAG, "InterruptedException()");
                    e.printStackTrace();
                }
            }
        }

        public void unlock() {
            synchronized (this) {
                Logger.d(TAG, "----UNLOCKED----");
                notifyAll();
            }
        }
    }

    private final class SessionStateMachine extends StateMachine {
        private GroupChat mGroupChat = null;

        // private List<Message> mStoredMessage = new ArrayList<Message>();

        public SessionStateMachine(String name) {
            super(name);
            Logger.d(TAG, "SessionStateMachine() Constructor");
            addState(mUnAvailableState, null);
            addState(mInitingState, mUnAvailableState);
            addState(mInvitingState, mUnAvailableState);
            addState(mAcceptedState, mUnAvailableState);
            addState(mRejectedState, mUnAvailableState);
            addState(mAutoAcceptedState, mUnAvailableState);
            addState(mActivatedState, mUnAvailableState);
            addState(mDeactivatedState, mUnAvailableState);
            addState(mRejoiningState, mUnAvailableState);
            addState(mQuitingState, mUnAvailableState);
            setInitialState(mUnAvailableState);
            start();
        }

        public void destory() {
            Logger.d(TAG, "SessionStateMachine() Destory");
            quit();
        }

        // Default state for GroupChat
        class UnavailableState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#UnavailableState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
                if (getChatId() == null) {
                    return;
                }
                // mGroupChat = mChatService.getChat(getChatId());
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case OP_SEND_CHATMESSAGE:
                case OP_RESEND_CHATMESSAGE:
                case OP_SEND_FILE:
                case OP_RESEND_FILE:
                case OP_DOWNLOAD_FILE:
                case OP_REDOWNLOAD_FILE:
                case OP_MODIFY_SUBJECT:
                case OP_MODIFY_MYNICKNAME:
                case OP_ADD_PARTICIPANT:
                case OP_REMOVE_PARTICIPANT:
                case OP_TRANSFER_CHAIRMAN:
                    // Quit/Abort both need activate group first.
                case OP_QUIT_GROUP:
                case OP_ABORT_GROUP:
                    deferMessage(msg);
                    removeGroupListener();
                    mGroupChat = rejoinGroup(getChatId());
                    if (mGroupChat == null) {
                        Logger.d(TAG, "Transfer: --> QuitingState");
                        transitionTo(mQuitingState);
                        sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    } else {
                        Logger.d(TAG, "Transfer: --> Rejoining");
                        transitionTo(mRejoiningState);
                    }
                    break;
                case OP_GROUP_UPDATE_STATUS://not used now.
                    try {
                        mGroupChat = GsmaManager.getInstance().getChatApi()
                                .getGroupChat(getChatId());
                        if (mGroupChat != null) {
                            // check if need add a listener.
                            mGroupChat.addEventListener(mGroupListener);
                            int state = mGroupChat.getState();
                            Logger.d(TAG, "OP_GROUP_UPDATE_STATUS mGroupChat: " + mGroupChat
                                    + " state: " + state);
                            switch (state) {
                            case GroupChat.State.INITIATED:
                                transitionTo(mInitingState);
                                break;
                            case GroupChat.State.INVITED:
                                // TODO
                                break;
                            case GroupChat.State.STARTED:
                                transitionTo(mActivatedState);
                                break;
                            case GroupChat.State.ABORTED:
                            case GroupChat.State.FAILED:
                            case GroupChat.State.CLOSED_BY_USER:
                            case GroupChat.State.TERMINATED:
                                transitionTo(mDeactivatedState);
                                break;
                            }
                            // sync from DB directly
                            mGroupData.syncParticipant();
                            mGroupData.syncSubject();
                        } else {
                            Logger.d(TAG, "OP_GROUP_UPDATE_STATUS group rejoin");
                            removeGroupListener();
                            mGroupChat = rejoinGroup(getChatId());
                            transitionTo(mRejoiningState);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case OP_GROUP_START:
                    mGroupChat = startGroup();
                    synchronized (SimpleGroupChat.this) {
                        SimpleGroupChat.this.notifyAll();
                    }
                    transitionTo(mInitingState);
                    Logger.d(TAG, "Transfer: --> Inviting");
                    break;
                case OP_GROUP_INVITATION:
                    if (!mGroupData.newGroup()) {
                        deferMessage(msg);
                        transitionTo(mDeactivatedState);
                        break;
                    }
                    String rejoinId = (String) msg.obj;
                    mGroupData.addRejoinId(rejoinId);
                    try {
                        mGroupChat = GsmaManager.getInstance().getChatApi()
                                .getGroupChat(getChatId());
                        int state = mGroupChat.getState();
                        Logger.d(TAG, "INVITATION mGroupChat: " + mGroupChat + " state: " + state);

                        if (state != GroupChat.State.INVITED) {
                            onDestroy();
                            break;
                        }

                        mGroupChat.addEventListener(mGroupListener);

                        final Participant participant = mGroupData.invitingParticipants.get(0);
                        // report UI
                        mService.getListener().onNewInvite(participant, mGroupData.subject,
                                mGroupData.chatId);
                        transitionTo(mInvitingState);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case OP_GROUP_INVITATION_ACCEPT:
                    try {
                        removeGroupListener();
                        mGroupChat = GsmaManager.getInstance().getChatApi()
                                .rejoinGroupChatId(getChatId(), mGroupData.getRejoinId());
                        Logger.d(TAG, "Accept saved invitation: " + mGroupChat);
                        if (mGroupChat != null) {
                            mGroupChat.addEventListener(mGroupListener);
                        }
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                    RCSDataBaseUtils.addGroupParticipant(
                            mContext,
                            mGroupData.chatId,
                            GsmaManager.getInstance().getMSISDN(),
                            null,
                            ConferenceUser.Status.CONNECTED);
                    mService.getListener().onAcceptInvitationResult(getChatId(), true);
                    transitionTo(mAcceptedState);
                    break;
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_GROUP_ERROR:
                case NOTIFICATION_CORESERVICE_DOWN:
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                case NOTIFICATION_REGISTRATION_STATUS_TRUE:
                    break;
                //For the Notification, Maybe should not care about the groupState.
                //Example:AutoAccept a group, session started is after a new message
                //the thread read a MSRP msg is faster than send a empty Chunk.
                case NOTIFICATION_BEEN_KICKED_OUT:
                    String contact = (String) msg.obj;
                    handleBeenKickedOut(contact);
                    break;
                case NOTIFICATION_GROUP_DISSOLVED:
                    handleGroupDissolved();
                    break;
                case NOTIFICATION_GROUP_ABORT_BY_CHAIRMAN:
                    handleGroupAbortByChairman();
                    break;
                case NOTIFICATION_CHATMESSAGE_SENT:
                    String msgId = (String) msg.obj;
                    int resultSent = msg.arg1;
                    handleChatMessageSent(resultSent, msgId);
                    break;
                case NOTIFICATION_GROUP_QUIT_RST:
                    int errorType = msg.arg1;
                    handleGroupQuitResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_GROUP_ABORT_RST:
                    errorType = msg.arg1;
                    handleGroupAbortResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_ADD_PARTICIPANT_RST:
                    errorType = msg.arg1;
                    handleAddParticipantResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_REMOVE_PARTICIPANT_RST:
                    errorType = msg.arg1;
                    handleRemoveParticipantResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_PARTICIPANT_JOINED:
                    Bundle bundle = msg.getData();
                    contact = bundle.getString("contact");
                    String displayName = bundle.getString("displayName");
                    handleParticipantJoined(contact, displayName);
                    break;
                case NOTIFICATION_PARTICIPANT_LEFT:
                    contact = (String) msg.obj;
                    handleParticipantLeft(contact, null);
                    break;
                case NOTIFICATION_PARTICIPANT_BEEN_KICKED_OUT:
                    contact = (String) msg.obj;
                    handleParticipantBeenKickOut(contact);
                    break;
                case NOTIFICATION_PARTICIPANT_DISCONNECTED:
                    contact = (String) msg.obj;
                    handleParticipantDisconnected(contact);
                    break;
                case NOTIFICATION_MODIFY_SUBJECT_RST:
                    errorType = msg.arg1;
                    handleModifySubjectResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_SUBJECT_CHANGED:
                    String subject = (String) msg.obj;
                    handleSubjectChanged(subject);
                    break;
                case NOTIFICATION_TRANSFER_CHAIRMAN_RST:
                    errorType = msg.arg1;
                    handleTransferChairmanResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_CHAIRMAN_TRANSFERED:
                    String chairman = (String) msg.obj;
                    handleChairmanTransfered(chairman);
                    break;
                case NOTIFICATION_MODIFY_MYNICKNAME_RST:
                    errorType = msg.arg1;
                    handleModifyMyNickNameResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_NICKNAME_CHANGED:
                    Participant participant = (Participant) msg.obj;
                    handleNickNameChanged(participant);
                    break;
                case NOTIFICATION_RECEIVE_MESSAGE:
                    ChatMessage message = (ChatMessage) msg.obj;
                    handleReceiveChatMessage(message);
                    break;
                default:
                    result = NOT_HANDLED;
                    break;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private UnavailableState mUnAvailableState = new UnavailableState();

        // User Create a GroupChat and is sending Invite request.
        class InitingState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#InitingState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case NOTIFICATION_GROUP_STARTED:
                    handleStartGroupResult(true);
                    Logger.d(TAG, "Transfer: --> Activated");
                    transitionTo(mActivatedState);
                    break;
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_GROUP_ERROR:
                    // not need to remove the listener from mGroupChat.
                    handleStartGroupResult(false);
                    break;
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                case NOTIFICATION_CORESERVICE_DOWN:
                    // When just send the invite to server and then registration
                    // state
                    // change to false, maybe should just report init false.
                    handleStartGroupResult(false);
                    break;
                case OP_QUIT_GROUP:
                    // can not happened.
                    result = NOT_HANDLED;
                    break;
                default:
                    result = NOT_HANDLED;
                    break;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private InitingState mInitingState = new InitingState();

        // Recevie a invite request and waiting user accept/reject.
        class InvitingState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#InvitingState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
                if (getChatId() == null) {
                    return;
                }
                // mGroupChat = mChatService.getChat(getChatId());
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                // user not click accept/reject, timeout.
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_CORESERVICE_DOWN:
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                    mService.getListener().onInvitationTimeout(getChatId());
                    removeGroupListener();
                    mGroupChat = null;
                    Logger.d(TAG, "--> No response for invite");
                    onDestroy();
                    break;
                case OP_GROUP_INVITATION_ACCEPT:
                    try {
                        if (mGroupChat.getState() == GroupChat.State.ABORTED) {
                            //has been already canceled
                        }
                        mGroupChat.acceptInvitation();
                    } catch (JoynServiceException e) {
                        Logger.d(TAG, "JoynServiceException");
                        e.printStackTrace();
                    }
                    RCSDataBaseUtils.addGroupParticipant(
                            mContext,
                            mGroupData.chatId, GsmaManager.getInstance().getMSISDN(),
                            null,
                            ConferenceUser.Status.CONNECTED);
                    mService.getListener().onAcceptInvitationResult(getChatId(), true);
                    transitionTo(mAcceptedState);
                    break;
                case OP_GROUP_INVITATION_REJECT:
                    try {
                        mGroupChat.rejectInvitation();
                    } catch (JoynServiceException e) {
                        Logger.d(TAG, "JoynServiceException");
                        e.printStackTrace();
                    }
                    transitionTo(mRejectedState);
                    break;
                default:
                    result = NOT_HANDLED;
                    break;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private InvitingState mInvitingState = new InvitingState();

        class AcceptedState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#AcceptedState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case OP_SEND_CHATMESSAGE:
                case OP_RESEND_CHATMESSAGE:
                case OP_SEND_FILE:
                case OP_RESEND_FILE:
                case OP_DOWNLOAD_FILE:
                case OP_REDOWNLOAD_FILE:
                case OP_MODIFY_SUBJECT:
                case OP_MODIFY_MYNICKNAME:
                case OP_ADD_PARTICIPANT:
                case OP_REMOVE_PARTICIPANT:
                case OP_TRANSFER_CHAIRMAN:
                    // Quit/Abort both need activate group first.
                case OP_QUIT_GROUP:
                case OP_ABORT_GROUP:
                    deferMessage(msg);
                    break;
                case NOTIFICATION_GROUP_STARTED:
                    // mService.getListener().onAcceptInvitationResult(getChatId(), true);
                    Logger.d(TAG, "Transfer: --> Activated");
                    transitionTo(mActivatedState);
                    break;
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                case NOTIFICATION_CORESERVICE_DOWN:
                    // mService.getListener().onAcceptInvitationResult(getChatId(), false);
                    // mGroupData.removeGroupParticipant(mGroupData.chatId, GsmaManager
                    // .getInstance().getMSISDN());
                    // mGroupChat = null;
                    // onDestroy();
                    transitionTo(mQuitingState);
                    sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    break;
                case NOTIFICATION_GROUP_ERROR:
                    int error = msg.arg1;
                    transitionTo(mQuitingState);
                    if (error == GroupChat.Error.CHAT_NOT_FOUND) {
                        sendMessage(OP_TRANSFER_GROUP_STATE, "Quit");
                    } else {
                        sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    }
                    break;
                default:
                    result = NOT_HANDLED;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private AcceptedState mAcceptedState = new AcceptedState();

        class RejectedState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#RejectedState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case NOTIFICATION_GROUP_ABORT:
                    mService.getListener().onRejectInvitationResult(getChatId(), true);
                    // reject result false not happened.
                    RCSDataBaseUtils.removeGroupParticipant(
                            mContext,
                            mGroupData.chatId,
                            GsmaManager.getInstance().getMSISDN());
                    removeGroupListener();
                    mGroupChat = null;
                    onDestroy();
                    break;
                case NOTIFICATION_GROUP_ERROR:
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                case NOTIFICATION_CORESERVICE_DOWN:
                    mService.getListener().onRejectInvitationResult(getChatId(), false);
                    // reject result false not happened.
                    RCSDataBaseUtils.removeGroupParticipant(
                            mContext,
                            mGroupData.chatId,
                            GsmaManager.getInstance().getMSISDN());
                    removeGroupListener();
                    mGroupChat = null;
                    onDestroy();
                    break;
                default:
                    result = NOT_HANDLED;
                    break;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private RejectedState mRejectedState = new RejectedState();

        // Recevie a invite request and waiting user accept/reject.
        class AutoAcceptedState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#AutoAcceptState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case NOTIFICATION_GROUP_STARTED:
                    Logger.d(TAG, "Transfer: --> Activated");
                    transitionTo(mActivatedState);
                    break;
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_GROUP_ERROR:
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                case NOTIFICATION_CORESERVICE_DOWN:
                    Logger.d(TAG, "Transfer: --> DeactivatedState");
                    transitionTo(mQuitingState);
                    sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    break;
                case OP_SEND_CHATMESSAGE:
                case OP_RESEND_CHATMESSAGE:
                case OP_SEND_FILE:
                case OP_RESEND_FILE:
                case OP_DOWNLOAD_FILE:
                case OP_REDOWNLOAD_FILE:
                case OP_FT_INVITATION:
                case OP_MODIFY_SUBJECT:
                case OP_MODIFY_MYNICKNAME:
                case OP_ADD_PARTICIPANT:
                case OP_REMOVE_PARTICIPANT:
                case OP_TRANSFER_CHAIRMAN:
                    // Quit/Abort both need activate group first.
                case OP_QUIT_GROUP:
                case OP_ABORT_GROUP:
                    deferMessage(msg);
                    break;
                default:
                    result = NOT_HANDLED;
                    break;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private AutoAcceptedState mAutoAcceptedState = new AutoAcceptedState();

        // GroupChat activate state
        class ActivatedState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#ActivatedState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
                /*
                 * Logger.d(TAG, "ActivatedState storedMessage size:" mStoredMessage.size()); for
                 * (Message msg : mStoredMessage) { sendMessage(msg); } mStoredMessage.clear();
                 */
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case OP_SEND_CHATMESSAGE:
                    Bundle bundle = msg.getData();
                    long smsMsgId = bundle.getLong("smsmsgid");
                    String msgContent = bundle.getString("content");
                    int type = bundle.getInt("type");
                    sendOrResendChatMsg(0, smsMsgId, msgContent, type);
                    break;
                case OP_RESEND_CHATMESSAGE:
                    bundle = msg.getData();
                    String msgResendId = bundle.getString("resendid");
                    sendOrResendChatMsg(1, 0, msgResendId, 0);
                    break;
                case OP_SEND_FILE:
                    bundle = msg.getData();
                    String filePath = bundle.getString("filepath");
                    long ftmsgId = bundle.getLong("msgId");
                    String chatSessionId = null;
                    try {
                        chatSessionId = mGroupChat.getChatSessionId();
                    } catch (JoynServiceException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Logger.d(TAG, "sendFile chatSessionId: " + chatSessionId);
                    FileTransferManager.getInstance(mService).handleSendFileTransferInvitation(
                            getParticipants(), filePath, getChatId(), chatSessionId, ftmsgId);
                    break;
                case OP_RESEND_FILE:
                    bundle = msg.getData();
                    ftmsgId = bundle.getLong("msgId");
                    chatSessionId = null;
                    try {
                        chatSessionId = mGroupChat.getChatSessionId();
                    } catch (JoynServiceException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    FileTransferManager.getInstance(mService).handleResendFileTransfer(ftmsgId,
                            getChatId(), chatSessionId);
                    break;
                case OP_DOWNLOAD_FILE:
                    bundle = msg.getData();
                    String fileTag = bundle.getString("filetag");
                    FileTransferManager.getInstance(mService).handleAcceptFileTransferInGroup(
                            fileTag);
                    break;
                case OP_REDOWNLOAD_FILE:
                    bundle = msg.getData();
                    fileTag = bundle.getString("filetag");
                    chatSessionId = null;
                    try {
                        chatSessionId = mGroupChat.getChatSessionId();
                    } catch (JoynServiceException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    FileTransferManager.getInstance(mService).handleReAcceptFileTransferInGroup(
                            fileTag, chatSessionId);
                    break;
                case OP_FT_INVITATION:
                    Intent intent = (Intent) msg.obj;
                    FileTransferManager.getInstance(mService)
                            .handleRecevieFileTransferInvitationInGroup(intent);
                    break;
                case OP_TRANSFER_CHAIRMAN:
                    Participant participant = (Participant) msg.obj;
                    transferChairman(participant);
                    break;
                case OP_MODIFY_SUBJECT:
                    String newSubject = (String) msg.obj;
                    modifySubject(newSubject);
                    break;
                case OP_MODIFY_MYNICKNAME:
                    String nickName = (String) msg.obj;
                    modifyNickName(nickName);
                    break;
                case OP_ADD_PARTICIPANT:
                    List<Participant> participants = (List<Participant>) msg.obj;
                    if (!addParticipants(participants)) {
                        // ...
                    }
                    break;
                case OP_REMOVE_PARTICIPANT:
                    List<Participant> participantList = (List<Participant>) msg.obj;
                    if (!removeParticipants(participantList)) {
                        // ...
                    }
                    break;
                case OP_QUIT_GROUP:
                    if (!quitGroup()) {
                        // ...
                    }
                    break;
                case OP_ABORT_GROUP:
                    if (!abortGroup()) {
                    }
                    break;
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_GROUP_ERROR:
                    removeGroupListener();
                    mGroupChat = null;
                    Logger.d(TAG, "Transfer: --> Deactivated");
                    transitionTo(mDeactivatedState);
                    break;
                case NOTIFICATION_BEEN_KICKED_OUT:
                    String contact = (String) msg.obj;
                    handleBeenKickedOut(contact);
                    break;
                case NOTIFICATION_GROUP_DISSOLVED:
                    handleGroupDissolved();
                    break;
                case NOTIFICATION_GROUP_ABORT_BY_CHAIRMAN:
                    handleGroupAbortByChairman();
                    break;
                case NOTIFICATION_CHATMESSAGE_SENT:
                    String msgId = (String) msg.obj;
                    int resultSent = msg.arg1;
                    handleChatMessageSent(resultSent, msgId);
                    break;
                case NOTIFICATION_GROUP_QUIT_RST:
                    int errorType = msg.arg1;
                    handleGroupQuitResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_GROUP_ABORT_RST:
                    errorType = msg.arg1;
                    handleGroupAbortResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_ADD_PARTICIPANT_RST:
                    errorType = msg.arg1;
                    handleAddParticipantResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_REMOVE_PARTICIPANT_RST:
                    errorType = msg.arg1;
                    handleRemoveParticipantResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_PARTICIPANT_JOINED:
                    bundle = msg.getData();
                    contact = bundle.getString("contact");
                    String displayName = bundle.getString("displayName");
                    handleParticipantJoined(contact, displayName);
                    break;
                case NOTIFICATION_PARTICIPANT_LEFT:
                    contact = (String) msg.obj;
                    handleParticipantLeft(contact, null);
                    break;
                case NOTIFICATION_PARTICIPANT_BEEN_KICKED_OUT:
                    contact = (String) msg.obj;
                    handleParticipantBeenKickOut(contact);
                    break;
                case NOTIFICATION_PARTICIPANT_DISCONNECTED:
                    contact = (String) msg.obj;
                    handleParticipantDisconnected(contact);
                    break;
                case NOTIFICATION_MODIFY_SUBJECT_RST:
                    errorType = msg.arg1;
                    handleModifySubjectResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_SUBJECT_CHANGED:
                    String subject = (String) msg.obj;
                    handleSubjectChanged(subject);
                    break;
                case NOTIFICATION_TRANSFER_CHAIRMAN_RST:
                    errorType = msg.arg1;
                    handleTransferChairmanResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_CHAIRMAN_TRANSFERED:
                    String chairman = (String) msg.obj;
                    handleChairmanTransfered(chairman);
                    break;
                case NOTIFICATION_MODIFY_MYNICKNAME_RST:
                    errorType = msg.arg1;
                    handleModifyMyNickNameResult(errorType);
                    mGroupConfigHandler.unlock();
                    break;
                case NOTIFICATION_NICKNAME_CHANGED:
                    participant = (Participant) msg.obj;
                    handleNickNameChanged(participant);
                    break;
                case NOTIFICATION_RECEIVE_MESSAGE:
                    ChatMessage message = (ChatMessage) msg.obj;
                    handleReceiveChatMessage(message);
                    break;
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                    transitionTo(mDeactivatedState);
                    break;
                case NOTIFICATION_CORESERVICE_DOWN:
                    transitionTo(mDeactivatedState);
                default:
                    result = NOT_HANDLED;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private ActivatedState mActivatedState = new ActivatedState();

        // GroupChat deactivate
        class DeactivatedState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#DeactivatedState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case NOTIFICATION_GROUP_STARTED:
                    Logger.d(TAG, "Warning/Error!");
                case NOTIFICATION_GROUP_ABORT:
                case NOTIFICATION_GROUP_ERROR:
                    result = NOT_HANDLED;
                    break;
                case OP_SEND_CHATMESSAGE:
                case OP_RESEND_CHATMESSAGE:
                case OP_SEND_FILE:
                case OP_RESEND_FILE:
                case OP_DOWNLOAD_FILE:
                case OP_REDOWNLOAD_FILE:
                    // FT invitation need check, server handle Group Session
                    // activate.
                case OP_FT_INVITATION:
                case OP_MODIFY_SUBJECT:
                case OP_MODIFY_MYNICKNAME:
                case OP_ADD_PARTICIPANT:
                case OP_REMOVE_PARTICIPANT:
                case OP_TRANSFER_CHAIRMAN:
                    // Quit/Abort both need activate group first.
                case OP_QUIT_GROUP:
                case OP_ABORT_GROUP:
                    deferMessage(msg);
                    removeGroupListener();
                    mGroupChat = rejoinGroup(getChatId());
                    if (mGroupChat == null) {
                        Logger.d(TAG, "Transfer: --> Quiting");
                        transitionTo(mQuitingState);
                        sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    } else {
                        Logger.d(TAG, "Transfer: --> Rejoining");
                        transitionTo(mRejoiningState);
                    }
                    break;
                case OP_GROUP_INVITATION:
                    try {
                        mGroupChat = GsmaManager.getInstance().getChatApi()
                                .getGroupChat(getChatId());
                        int state = mGroupChat.getState();
                        Logger.d(TAG, "INVITATION mGroupChat: " + mGroupChat + " state: " + state);
                        if (state != GroupChat.State.INVITED) {
                            Logger.d(TAG, "Already Canceled so Transfer: --> Quiting");
                            transitionTo(mQuitingState);
                            sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                            break;
                        }

                        mGroupChat.addEventListener(mGroupListener);
                        mGroupChat.acceptInvitation();
                        transitionTo(mAutoAcceptedState);
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                    break;
                case OP_GROUP_UPDATE_STATUS://not used now.
                    removeGroupListener();
                    mGroupChat = rejoinGroup(getChatId());
                    transitionTo(mRejoiningState);
                    break;
                default:
                    result = NOT_HANDLED;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private DeactivatedState mDeactivatedState = new DeactivatedState();

        // GroupChat rejoining.
        class RejoiningState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#RejoiningState";
            int rejoinCount;

            @Override
            public void enter() {
                rejoinCount = 1;
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                if (!GsmaManager.isServiceAvailable()
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_FALSE
                        && msg.what != NOTIFICATION_REGISTRATION_STATUS_TRUE
                        && msg.what != NOTIFICATION_CORESERVICE_DOWN) {
                    handleMsgOutOfService(this, msg);
                    return HANDLED;
                }
                boolean result = HANDLED;
                switch (msg.what) {
                case NOTIFICATION_GROUP_STARTED:
                    transitionTo(mActivatedState);
                    break;
                case NOTIFICATION_GROUP_ABORT:
                    transitionTo(mQuitingState);
                    sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    break;
                case NOTIFICATION_GROUP_ERROR:
                    int error = msg.arg1;
                    transitionTo(mQuitingState);
                    if (error == GroupChat.Error.CHAT_NOT_FOUND) {
                        sendMessage(OP_TRANSFER_GROUP_STATE, "Quit");
                    } else {
                        sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    }
                    break;
                case NOTIFICATION_REGISTRATION_STATUS_FALSE:
                case NOTIFICATION_CORESERVICE_DOWN:
                    Logger.d(TAG, "Transfer: --> Quiting");
                    transitionTo(mQuitingState);
                    sendMessage(OP_TRANSFER_GROUP_STATE, "Deactivated");
                    break;
                case OP_SEND_CHATMESSAGE:
                case OP_RESEND_CHATMESSAGE:
                case OP_SEND_FILE:
                case OP_RESEND_FILE:
                case OP_DOWNLOAD_FILE:
                case OP_REDOWNLOAD_FILE:
                case OP_FT_INVITATION:
                case OP_MODIFY_SUBJECT:
                case OP_MODIFY_MYNICKNAME:
                case OP_ADD_PARTICIPANT:
                case OP_REMOVE_PARTICIPANT:
                case OP_TRANSFER_CHAIRMAN:
                case OP_QUIT_GROUP:
                case OP_ABORT_GROUP:
                    deferMessage(msg);
                    break;
                default:
                    result = NOT_HANDLED;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private RejoiningState mRejoiningState = new RejoiningState();

        // GroupChat can not be used.
        class QuitingState extends State {
            private final String TAG = SimpleGroupChat.this.TAG + "#QuitingState";

            @Override
            public void enter() {
                Logger.d(TAG, "enter()");
            }

            @Override
            public boolean processMessage(Message msg) {
                Logger.d(TAG, "processMessage():" + msgToString(msg.what));
                // if (!GsmaManager.isServiceAvailable()) {
                // handleMsgOutOfService(msg);
                // return HANDLED;
                // }
                boolean result = HANDLED;
                switch (msg.what) {
                case OP_TRANSFER_GROUP_STATE:
                    String state = (String) msg.obj;
                    Logger.d(TAG, "Transfer: --> " + state);
                    if (state.equalsIgnoreCase("Deactivated")) {
                        transitionTo(mDeactivatedState);
                    } else if (state.equalsIgnoreCase("Quit")) {
                        handleGroupDissolved();
                    }
                    break;
                case OP_SEND_CHATMESSAGE:
                    Bundle bundle = msg.getData();
                    long smsMsgId = bundle.getLong("smsmsgid");
                    handleChatMessageSendFail(smsMsgId);
                    break;
                case OP_RESEND_CHATMESSAGE:
                    bundle = msg.getData();
                    String msgResendId = bundle.getString("resendid");
                    handleChatMessageSent(-1, msgResendId);
                    break;
                case OP_SEND_FILE:
                case OP_RESEND_FILE:
                case OP_DOWNLOAD_FILE:
                case OP_REDOWNLOAD_FILE:
                case OP_FT_INVITATION:
                    break;
                case OP_MODIFY_SUBJECT:
                    handleModifySubjectResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                    mGroupConfigHandler.unlock();
                    break;
                case OP_MODIFY_MYNICKNAME:
                    handleModifyMyNickNameResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                    mGroupConfigHandler.unlock();
                    break;
                case OP_ADD_PARTICIPANT:
                    handleAddParticipantResult(GroupChat.ParticipantStatus.FAIL);
                    mGroupConfigHandler.unlock();
                    break;
                case OP_REMOVE_PARTICIPANT:
                    handleRemoveParticipantResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                    mGroupConfigHandler.unlock();
                    break;
                case OP_TRANSFER_CHAIRMAN:
                    handleTransferChairmanResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                    mGroupConfigHandler.unlock();
                    break;
                case OP_QUIT_GROUP:
                    handleGroupQuitResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                    mGroupConfigHandler.unlock();
                    break;
                case OP_ABORT_GROUP:
                    handleGroupAbortResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                    mGroupConfigHandler.unlock();
                    break;
                default:
                    result = NOT_HANDLED;
                }
                return result;
            }

            @Override
            public void exit() {
                Logger.d(TAG, "exit()");
            }
        }

        private QuitingState mQuitingState = new QuitingState();

        private GroupChat startGroup() {
            Logger.d(TAG, "startGroup() entry");
            Set<String> groupContactSet = new HashSet<String>();
            List<Participant> participants = mGroupData.invitingParticipants;
            for (Participant participant : participants) {
                groupContactSet.add(participant.getContact());
            }

            int inviteSize = groupContactSet.size();
            Logger.d(TAG, "startGroup() inviteSize: " + inviteSize);
            if (inviteSize > 0) {
                try {
                    GroupChat groupChat = GsmaManager.getInstance().getChatApi()
                            .initiateGroupChat(groupContactSet, mGroupData.subject, mGroupListener);
                    if (groupChat == null) {
                        return null;
                    }
                    setChatId(groupChat.getChatId());
                    RCSDataBaseUtils.addGroupParticipant(
                            mContext,
                            mGroupData.chatId,
                            GsmaManager.getInstance().getMSISDN(),
                            null,
                            ConferenceUser.Status.CONNECTED);
                    return groupChat;
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    // TODo call API to mark messages as failed.
                }
            }
            return null;
        }

        private GroupChat rejoinGroup(String chatId) {
            Logger.d(TAG, "rejoinGroup() entry, rejoinId: " + chatId);
            if (chatId == null) {
                return null;
            }
            try {
                GroupChat groupChat = GsmaManager.getInstance().getChatApi()
                        .rejoinGroupChat(chatId);
                if (groupChat != null) {
                    groupChat.addEventListener(mGroupListener);
                    Logger.d(TAG, "rejoinGroup() success");
                    return groupChat;
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
            return null;
        }

        private GroupChat restartGroup(String chatId) {
            Logger.d(TAG, "restartGroup() entry, chatId: " + chatId);
            if (chatId == null) {
                return null;
            }
            try {
                GroupChat groupChat = GsmaManager.getInstance().getChatApi()
                        .restartGroupChat(chatId);
                Logger.d(TAG, "restartGroup() groupChat: " + groupChat);

                if (groupChat != null) {
                    groupChat.addEventListener(mGroupListener);
                    Logger.d(TAG, "restartGroup() success");
                    return groupChat;
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
            return null;
        }

        private boolean quitGroup() {
            Logger.d(TAG, "quitGroup() entry2");
            try {
                mGroupChat.quitConversation();
                return true;
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean abortGroup() {
            Logger.d(TAG, "abortGroup() entry2");
            try {
                mGroupChat.abortConversation();
                return true;
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean sendOrResendChatMsg(int resend, long smsMsgId, String para, int type) {
            Logger.d(TAG, "sendOrResendChatMessage() smsMsgId: " + smsMsgId + " para: " + para);
            if (para == null || para.equals(""))
                return false;
            try {
                if (resend == 0) {
                    String messageId = null;
                    if (type == Class.EMOTICON) {
                        messageId = mGroupChat.sendEmoticonsMessage(para);
                    } else {
                        messageId = mGroupChat.sendMessage(para);
                    }
                    // update the sms database for connect the stack msgid
                    RCSDataBaseUtils.combineMsgId(mContext, smsMsgId, messageId, MessageType.IM);
                    // update the rcs msg id in Pending List.
                    PendingMessage resendingMessage = mService.getPendingMessageManager()
                            .smsMsgContains(smsMsgId);
                    if (resendingMessage != null) {
                        Logger.d(TAG, "sendOrResendChatMsg() update rcsmsgid in Pending List");
                        resendingMessage.rcsMsgId = messageId;
                    }
                } else {
                    mGroupChat.resendMessage(para);
                }
                return true;
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean addParticipants(List<Participant> participantList) {
            Logger.d(TAG, "addParticipants() ParticipantList: " + participantList);
            Set<String> contactSet = new HashSet<String>();
            for (Participant participant : participantList) {
                if (!mGroupData.containParticipant(participant)) {
                    contactSet.add(participant.getContact());
                }
            }
            Logger.d(TAG, "addParticipants() " + contactSet);
            if (contactSet.size() <= 0) {
                handleAddParticipantResult(GroupChat.ParticipantStatus.SUCCESS);
                mGroupConfigHandler.unlock();
                return false;
            }

            mConfigurationPara = contactSet;
            try {
                mGroupChat.addParticipants(contactSet);
            } catch (JoynServiceException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        private boolean removeParticipants(List<Participant> participantList) {
            Logger.d(TAG, "removeParticipants() list:" + participantList);
            Set<String> contactSet = new HashSet<String>();
            for (Participant participant : participantList) {
                if (mGroupData.containParticipant(participant)) {
                    contactSet.add(participant.getContact());
                }
            }
            Logger.d(TAG, "removeParticipants() " + contactSet);
            if (contactSet.size() <= 0) {
                mGroupConfigHandler.unlock();
                return false;
            }

            mConfigurationPara = contactSet;
            try {
                mGroupChat.removeParticipants(new ArrayList<String>(contactSet));
                // why add participants arg is Set<String>, and remove is List.
            } catch (JoynServiceException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        private boolean transferChairman(Participant participant) {
            // maybe should check chairman privilege earlier for async call.
            boolean newChairmamInGroup = false;
            List<GroupParticipant> groupParticipants = mGroupData.groupParticipants;
            for (GroupParticipant groupParticipant : groupParticipants) {
                if (groupParticipant.getParticipant().equals(participant)) {
                    newChairmamInGroup = true;
                    break;
                }
            }
            if (newChairmamInGroup) {
                try {
                    Logger.d(TAG, "transferChairman() entry2");
                    mGroupChat.transferChairman(participant.getContact());
                    return true;
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                }
            } else {
                mService.getListener().onTransferChairmenResult(getChatId(), false);
            }
            return false;
        }

        private boolean modifySubject(String subject) {
            try {
                mGroupChat.modifySubject(subject);
            } catch (JoynServiceException e) {
                e.printStackTrace();
                return false;
                // ...
            }
            return true;
        }

        private boolean modifyNickName(String nickName) {
            try {
                mConfigurationPara = nickName;
                mGroupChat.modifyMyNickName(nickName);
            } catch (JoynServiceException e) {
                e.printStackTrace();
                return false;
                // ...
            }
            return true;
        }

        private void handleStartGroupResult(boolean result) {
            if (result) {
                mService.getListener().onInitGroupResult(true, getChatId());
            } else {
                mGroupSession.removeGroupListener();
                mGroupChat = null;
                RCSDataBaseUtils.removeGroupParticipant(
                        mContext,
                        mGroupData.chatId,
                        GsmaManager.getInstance().getMSISDN());
                mService.getListener().onInitGroupResult(false, getChatId());
                onDestroy();
            }
        }

        private void handleChatMessageSendFail(final long smsMsgId) {
            mService.getPendingMessageManager().removePendingMessage(smsMsgId);
            notifyMessageSendFail(smsMsgId);
        }

        private void handleChatMessageSent(int result, String msgId) {
            if (result > 0) {
                mService.getPendingMessageManager().removePendingMessage(msgId);
                onReceiveMessageDeliveryStatus(msgId, "sent");
            } else {
                Message msg = mWorkHandler.obtainMessage(BASE_OP_RESEND_MESSAGE);
                msg.obj = msgId;
                mWorkHandler.sendMessageDelayed(msg, 5 * 1000);
                // onReceiveMessageDeliveryStatus(msgId, "failed");
            }
        }

        private void handleAddParticipantResult(int result) {
            if (result == GroupChat.ParticipantStatus.SUCCESS) {
                @SuppressWarnings("unchecked")
                Set<String> contactSet = (Set<String>) mConfigurationPara;
                for (String contact : contactSet) {
                    //mGroupData.addGroupParticipant(getChatId(), contact, null,
                    //        ConferenceUser.Status.PENDING);
                }
                mService.getListener().onAddParticipantsResult(getChatId(), true);
            } else if (result == GroupChat.ParticipantStatus.FAIL) {
                mService.getListener().onAddParticipantsResult(getChatId(), false);
            }
        }

        private void handleRemoveParticipantResult(int result) {
            if (result == GroupChat.ReasonCode.SUCCESSFUL) {
                Set<String> contactSet = (Set<String>) mConfigurationPara;
                for (String contact : contactSet) {
                    RCSDataBaseUtils.removeGroupParticipant(mContext, getChatId(), contact);
                }
                mService.getListener().onRemoveParticipantsResult(getChatId(), true);
            } else if (result == GroupChat.ReasonCode.INTERNAL_ERROR) {
                // this will not happen.
                mService.getListener().onRemoveParticipantsResult(getChatId(), false);
            }
        }

        private void handleParticipantJoined(String contact, String displayName) {
            Participant participant = new Participant(contact, displayName);
            mGroupData.syncParticipant();
            mService.getListener().onParticipantJoined(getChatId(), participant);
        }

        private void handleParticipantLeft(String contact, String displayName) {
            final Participant participant = new Participant(contact, displayName);
            mGroupData.syncParticipant();
            mService.getListener().onParticipantLeft(getChatId(), participant);
        }

        private void handleParticipantBeenKickOut(String contact) {
            Logger.d(TAG, "handleParticipantBeenKickOut() contact: " + contact);
            final Participant participant = new Participant(contact, null);
            //mGroupData.syncParticipant();
            //mService.getListener().onParticipantRemoved(getChatId(), participant);
        }

        private void handleParticipantDisconnected(String contact) {
            Logger.d(TAG, "handleParticipantDisconnected() contact: " + contact);
            final Participant participant = new Participant(contact, null);
            //mGroupData.syncParticipant();
            // ////
        }

        private void handleModifySubjectResult(int result) {
            if (result == GroupChat.ReasonCode.SUCCESSFUL) {
                // mSubject = ////TODO:
                mService.getListener().onSubjectModifiedResult(getChatId(), true);
            } else if (result == GroupChat.ReasonCode.INTERNAL_ERROR) {
                mService.getListener().onSubjectModifiedResult(getChatId(), false);
            }
        }

        private void handleSubjectChanged(final String subject) {
            mGroupData.syncSubject();
            mService.getListener().onSubjectModified(getChatId(), subject);
        }

        private void handleTransferChairmanResult(int result) {
            if (result == GroupChat.ReasonCode.SUCCESSFUL) {
                mService.getListener().onTransferChairmenResult(getChatId(), true);
            } else if (result == GroupChat.ReasonCode.INTERNAL_ERROR) {
                mService.getListener().onTransferChairmenResult(getChatId(), false);
            }
        }

        private void handleChairmanTransfered(final String chairman) {
            mService.getListener().onChairmenChanged(getChatId(), new Participant(chairman, null),
                    false);
        }

        private void handleModifyMyNickNameResult(int result) {
            if (result == GroupChat.ReasonCode.SUCCESSFUL) {
                RCSDataBaseUtils.updateGroupParticipant(
                        mContext,
                        getChatId(),
                        GsmaManager.getInstance().getMSISDN(),
                        (String) mConfigurationPara,
                        null);
                mService.getListener().onMyNickNameModifiedResult(getChatId(), true);
            } else if (result == GroupChat.ReasonCode.INTERNAL_ERROR) {
                mService.getListener().onMyNickNameModifiedResult(getChatId(), false);
            }
        }

        private void handleNickNameChanged(Participant participant) {
            // TODO
        }

        private void handleBeenKickedOut(final String contact) {
            mGroupSession.removeGroupListener();
            mGroupChat = null;
            mService.getListener().onMeRemoved(getChatId(), contact);
            onDestroy();
            return;
        }

        private void handleGroupDissolved() {
            mGroupSession.removeGroupListener();
            mGroupChat = null;
            mService.getListener().onAbort(getChatId());
            onDestroy();
            return;
        }

        private void handleGroupAbortByChairman() {
            mGroupSession.removeGroupListener();
            mGroupChat = null;
            mService.getListener().onAbort(getChatId());
            onDestroy();
            return;
        }

        private void handleGroupQuitResult(int result) {
            if (result == GroupChat.ReasonCode.SUCCESSFUL) {
                mGroupSession.removeGroupListener();
                mGroupChat = null;
                mService.getListener().onQuitConversationResult(getChatId(), true);
                onDestroy();
            } else if (result == GroupChat.ReasonCode.INTERNAL_ERROR) {
                Logger.d(TAG, "Cann't quit!:(");
                mService.getListener().onQuitConversationResult(getChatId(), false);
            }
        }

        private void handleGroupAbortResult(int result) {
            if (result == GroupChat.ReasonCode.SUCCESSFUL) {
                mGroupSession.removeGroupListener();
                mGroupChat = null;
                mService.getListener().onAbortResult(getChatId(), true);
                onDestroy();
            } else if (result == GroupChat.ReasonCode.INTERNAL_ERROR) {
                Logger.d(TAG, "Cann't abort!:(");
                mService.getListener().onAbortResult(getChatId(), false);
            }
        }

        private void handleReceiveChatMessage(final ChatMessage message) {
            onReceiveChatMessage(message);
            if (message.isDisplayedReportRequested()) {
                //try {
                //First: CMCC not need display report; Second: When receive a msg
                //the group state may not be activated
                //mGroupChat.sendDisplayedDeliveryReport(message.getId());
                //} catch (JoynServiceException e) {
                //e.printStackTrace();
                //}
            }
        }

        private void handleMsgOutOfService(State state, Message msg) {
            int OP = msg.what;
            Logger.d(TAG, " - handleMsgOutOfService: " + msgToString(OP));
            switch (OP) {
            case OP_GROUP_START:
                handleStartGroupResult(false);
                break;
            case OP_GROUP_UPDATE_STATUS:
                break;
            case OP_SEND_CHATMESSAGE:
                Bundle bundle = msg.getData();
                long smsMsgId = bundle.getLong("smsmsgid");
                handleChatMessageSendFail(smsMsgId);
                break;
            case OP_RESEND_CHATMESSAGE:
                bundle = msg.getData();
                String msgResendId = bundle.getString("resendid");
                handleChatMessageSent(-1, msgResendId);
                break;
            case OP_SEND_FILE:
                bundle = msg.getData();
                String filePath = bundle.getString("filepath");
                long ftmsgId = bundle.getLong("msgId");
                String chatSessionId = null;
                Logger.d(TAG, "handleMsgOutOfService, sendFile chatSessionId: " + chatSessionId);
                FileTransferManager.getInstance(mService).handleSendFileTransferInvitation(
                        getParticipants(), filePath, getChatId(), chatSessionId, ftmsgId);
                break;
            case OP_RESEND_FILE:
            case OP_DOWNLOAD_FILE:
            case OP_REDOWNLOAD_FILE:
            case OP_FT_INVITATION:
                break;
            case OP_ADD_PARTICIPANT:
                handleAddParticipantResult(GroupChat.ParticipantStatus.FAIL);
                mGroupConfigHandler.unlock();
                break;
            case OP_REMOVE_PARTICIPANT:
                handleRemoveParticipantResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                mGroupConfigHandler.unlock();
                break;
            case OP_TRANSFER_CHAIRMAN:
                handleTransferChairmanResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                mGroupConfigHandler.unlock();
                break;
            case OP_MODIFY_SUBJECT:
                handleModifySubjectResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                mGroupConfigHandler.unlock();
                break;
            case OP_MODIFY_MYNICKNAME:
                handleModifyMyNickNameResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                mGroupConfigHandler.unlock();
                break;
            case OP_QUIT_GROUP:
                handleGroupQuitResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                mGroupConfigHandler.unlock();
                break;
            case OP_ABORT_GROUP:
                handleGroupAbortResult(GroupChat.ReasonCode.INTERNAL_ERROR);
                mGroupConfigHandler.unlock();
                break;
            case OP_GROUP_INVITATION:
                if (state == mUnAvailableState)
                    onDestroy();
                break;
            case OP_GROUP_INVITATION_ACCEPT:
                mService.getListener().onAcceptInvitationResult(getChatId(), false);
                break;
            case OP_GROUP_INVITATION_REJECT:
                mService.getListener().onRejectInvitationResult(getChatId(), false);
                break;
            }
        }

        /**
         * Called when message wasn't handled
         *
         * @param msg
         *            that couldn't be handled.
         */
        @Override
        protected void unhandledMessage(Message msg) {
            Logger.d(TAG, " - unhandledMessage: msg.what=" + msg.what);
        }

        //This is to avoid memory leak, trigger unlinkToDeath be called.
        private void removeGroupListener() {
            if (mGroupChat != null) {
                try {
                    mGroupChat.removeEventListener(mGroupListener);
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class SimpleGroupChatListener extends GroupChatListener {
        public final String TAG = SimpleGroupChat.this.TAG + "#ChatListener";

        public SimpleGroupChatListener() {
            Logger.d(TAG, "Constructor()");
        }

        /**
         * Callback called when the session is well established and messages may be exchanged with
         * the group of participants
         */
        @Override
        public void onSessionStarted() {
            Logger.d(TAG, "onSessionStarted() entry");
            mGroupSession.sendMessage(NOTIFICATION_GROUP_STARTED);
        }

        /**
         * Callback called when the session has been aborted or terminated
         */
        @Override
        public void onSessionAborted() {
            Logger.d(TAG, "onSessionAborted() entry");
            mGroupSession.sendMessage(NOTIFICATION_GROUP_ABORT);
        }

        /**
         * Callback called when the session has failed
         *
         * @param error
         *            Error
         * @see GroupChat.Error
         */
        @Override
        public void onSessionError(int error) {
            Logger.d(TAG, "onSessionError() entry, error: " + error);
            mGroupSession.sendMessage(NOTIFICATION_GROUP_ERROR, error);
        }

        /**
         * Callback called when a new message has been received
         *
         * @param message
         *            New chat message
         * @see ChatMessage
         */
        @Override
        public void onNewMessage(ChatMessage message) {
            Logger.d(TAG, "onNewMessage() entry Contact: " + message.getContact() + " MsgContent: "
                    + message.getMessage());
            mGroupSession.sendMessage(NOTIFICATION_RECEIVE_MESSAGE, message);
        }

        /**
         * Callback called when a new geoloc has been received
         *
         * @param message
         *            Geoloc message
         * @see GeolocMessage
         */
        @Override
        public void onNewGeoloc(GeolocMessage message) {
            Logger.d(TAG, "onNewGeoloc() entry");
        }

        /**
         * Callback called when a message has been delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageDeliveredContact(String msgId, String contact) {
            Logger.d(TAG, "onReportMessageDelivered() entry, msgId: " + msgId + "contact: "
                    + contact);
        }

        /**
         * Callback called when a message has been displayed by the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageDisplayedContact(String msgId, String contact) {
            Logger.d(TAG, "onReportMessageDisplayedContact() entry, msgId: " + msgId + "contact: "
                    + contact);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageFailedContact(String msgId, String contact) {
            Logger.d(TAG, "onReportMessageFailedContact() entry, msgId: " + msgId + "contact: "
                    + contact);
            mGroupSession.sendMessage(NOTIFICATION_CHATMESSAGE_SENT, 0, 0, msgId);
            return;
        }

        /**
         * Callback called when a message has been delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageDelivered(String msgId) {
            Logger.d(TAG, "onReportMessageDelivered() entry, msgId: " + msgId);
        }

        /**
         * Callback called when a message has been displayed by the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageDisplayed(String msgId) {
            Logger.d(TAG, "onReportMessageDisplayed() entry, msgId: " + msgId);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageFailed(String msgId) {
            Logger.d(TAG, "onReportMessageFailed() entry, msgId: " + msgId);
            //mGroupSession.sendMessage(NOTIFICATION_CHATMESSAGE_SENT, 0, 0, msgId);
            return;
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportFailedMessage(String msgId, int errtype, String statusCode) {
            Logger.d(TAG, "onReportFailedMessage() entry, msgId: " + msgId + " errtype: " + errtype
                    + " statusCode: " + statusCode);
            //mGroupSession.sendMessage(NOTIFICATION_CHATMESSAGE_SENT, 0, 0, msgId);
            return;
        }

        /**
         * Callback called when a message has been sent to remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportSentMessage(String msgId) {
            Logger.d(TAG, "onReportSentMessage() entry, msgId: " + msgId);
            mGroupSession.sendMessage(NOTIFICATION_CHATMESSAGE_SENT, 1, 0, msgId);
        }

        /**
         * Callback called when a group chat is dissolved
         *
         */
        @Override
        public void onGroupChatDissolved() {
            // base on the spec SIP Bye reason 410
            // means participants count is less than the MIN number
            // or the chairman abort the group. **Maybe CMCC Server not support**
            Logger.d(TAG, "onGroupChatDissolved() entry!");
            mGroupSession.sendMessage(NOTIFICATION_GROUP_DISSOLVED);
        }

        /**
         * Callback called to inform the result of invite participants
         *
         */
        @Override
        public void onInviteParticipantsResult(int errType, String statusCode) {
            Logger.d(TAG, "onInviteParticipantsResult() entry, errType: " + errType
                    + " statusCode: " + statusCode);
            mGroupSession.sendMessage(NOTIFICATION_ADD_PARTICIPANT_RST, errType);
        }

        /**
         * Callback called when an Is-composing event has been received. If the remote is typing a
         * message the status is set to true, else it is false.
         *
         * @param contact
         *            Contact
         * @param status
         *            Is-composing status
         */
        @Override
        public void onComposingEvent(String contact, boolean status) {
            Logger.d(TAG, "onComposingEvent() contact: " + contact + " status: " + status);
        }

        /**
         * Callback called when a new participant has joined the group chat
         *
         * @param contact
         *            Contact
         * @param contactDisplayname
         *            Contact displayname
         */
        @Override
        public void onParticipantJoined(String contact, String contactDisplayname) {
            Logger.d(TAG, "onParticipantJoined() contact:" + contact + " contactDisplayname: "
                    + contactDisplayname);
            Message msg = mGroupSession.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("contact", contact);
            bundle.putString("displayName", contactDisplayname);
            msg.what = NOTIFICATION_PARTICIPANT_JOINED;
            msg.setData(bundle);
            // SessionStateMachine.this.sendMessage(msg);
        }

        /**
         * Callback called when a participant has left voluntary the group chat
         *
         * @param contact
         *            Contact
         */
        @Override
        public void onParticipantLeft(String contact) {
            Logger.d(TAG, "onParticipantLeft() contact: " + contact);
            // mGroupSession.sendMessage(NOTIFICATION_PARTICIPANT_LEFT, contact);
        }

        /**
         * Callback called when a participant is disconnected from the group chat
         *
         * @param contact
         *            Contact
         */
        @Override
        public void onParticipantDisconnected(String contact) {
            Logger.d(TAG, "onParticipantDisconnected() contact: " + contact);
            // mGroupSession.sendMessage(NOTIFICATION_PARTICIPANT_DISCONNECTED, contact);
        }

        /**
         * Callback called when new chairman is successfully changed by current chairman (Callback
         * only received by chairman)
         *
         * @param errType
         *            errorType
         */
        @Override
        public void onSetChairmanResult(int errType, int statusCode) {
            Logger.d(TAG, "onSetChairmanResult() errType:"
                    + errType + " statusCode: " + statusCode);
            mGroupSession.sendMessage(NOTIFICATION_TRANSFER_CHAIRMAN_RST, errType);
        }

        /**
         * Callback called when chairman is changed by current chairman (Callback received by every
         * user of group)
         *
         * @param newChairman
         *            new chairman
         */
        @Override
        public void onChairmanChanged(String newChairman) {
            Logger.d(TAG, "onChairmanChanged() newChairman:" + newChairman);
            mGroupSession.sendMessage(NOTIFICATION_CHAIRMAN_TRANSFERED, newChairman);
        }

        /**
         * Callback called when subject is modified (Callback only received by chairman)
         *
         * @param errType
         *            errorType
         */
        @Override
        public void onModifySubjectResult(int errType, int statusCode) {
            Logger.d(TAG, "onModifySubjectResult() errType:" + errType + " statusCode: "
                    + statusCode);
            mGroupSession.sendMessage(NOTIFICATION_MODIFY_SUBJECT_RST, errType);
        }

        /**
         * Callback called when subject is changed (Callback received by every user of group)
         *
         * @param newSubject
         *            new subject
         */
        @Override
        public void onSubjectChanged(String newSubject) {
            Logger.d(TAG, "onSubjectChanged() newSubject:" + newSubject);
            mGroupSession.sendMessage(NOTIFICATION_SUBJECT_CHANGED, newSubject);
        }

        /**
         * Callback called when participants are removed (Callback only received by chairman)
         *
         * @param errType
         *            errorType
         * @param statusCode
         *            status Code
         */
        @Override
        public void onRemoveParticipantResult(int errType, int statusCode, String participant) {
            Logger.d(TAG, "onInviteParticipantsResult() entry, errType: " + errType
                    + " statusCode: " + statusCode + " participant: " + participant);
            mGroupSession.sendMessage(NOTIFICATION_REMOVE_PARTICIPANT_RST, errType);
        }

        /**
         * Callback called participant is kicked out(removed) by chairman (Callback received by
         * removed participant)
         *
         * @param from
         *            who kicked out
         */
        @Override
        public void onReportMeKickedOut(String from) {
            Logger.d(TAG, "onReportMeKickedOut() from:" + from);
            mGroupSession.sendMessage(NOTIFICATION_BEEN_KICKED_OUT, from);
        }

        /**
         * Callback called chairman has successfully aborted the group (Callback only received by
         * chairman)
         *
         * @param errType
         *            errorType
         */
        @Override
        public void onAbortConversationResult(int errType, int statusCode) {
            Logger.d(TAG, "onAbortConversationResult() errType:" + errType + " statusCode: "
                    + statusCode);
            mGroupSession.sendMessage(NOTIFICATION_GROUP_ABORT_RST, errType);
        }

        /**
         * Callback called user has left the group successfully (Callback received by user who left
         * the group)
         *
         * @param errType
         *            errorType
         */
        @Override
        public void onQuitConversationResult(int errType, int statusCode) {
            Logger.d(TAG, "onQuitConversationResult() errType:" + errType + " statusCode: "
                    + statusCode);
            mGroupSession.sendMessage(NOTIFICATION_GROUP_QUIT_RST, errType);
        }

        @Override
        public void onReportParticipantKickedOut(String contact) {
            Logger.d(TAG, "onReportParticipantKickedOut() contact:" + contact);
            // SessionStateMachine.this.sendMessage(NOTIFICATION_PARTICIPANT_BEEN_KICKED_OUT,
            // contact);
        }

        @Override
        public void onModifyNickNameResult(int errType, int statusCode) {
            Logger.d(TAG, "onModifyNickNameResult() errType:" + errType + " statusCode: "
                    + statusCode);
            mGroupSession.sendMessage(NOTIFICATION_MODIFY_MYNICKNAME_RST, errType);
        }

        @Override
        public void onNickNameChanged(String contact, String newNickName) {
            Logger.d(TAG, "onModifyNickNameResult() errType:" + contact + " statusCode: "
                    + newNickName);
            Participant participant = new Participant(contact, newNickName);
            mGroupSession.sendMessage(NOTIFICATION_NICKNAME_CHANGED, participant);
        }

        @Override
        public void onSessionAbortedbyChairman() {
            Logger.d(TAG, "onSessionAbortedbyChairman()");
            mGroupSession.sendMessage(NOTIFICATION_GROUP_ABORT_BY_CHAIRMAN);
        }

        @Override
        public void onConferenceNotify(String confState,
                List<org.gsma.joyn.chat.ConferenceEventData.ConferenceUser> users) {
            mGroupData.handleConferenceNotify(confState, users);
        }
    }

    private class SimpleGroupChatData {
        private boolean updateDone = true;
        private String subject = "";
        private String chatId = null;

        private List<Participant> invitingParticipants;
        private CopyOnWriteArrayList<GroupParticipant> groupParticipants =
                new CopyOnWriteArrayList<GroupParticipant>();

        private ContentResolver resolver = GsmaManager.getContext().getContentResolver();
        private Uri groupMemberUri = GroupMemberData.CONTENT_URI;
        private Uri groupRejoinUri = GroupChatData.CONTENT_URI;

        void init(String chatId) {
            Logger.v(TAG, "initGroupData from DB #chatId: " + chatId);
            if (chatId == null) {
                return;
            }
            this.chatId = chatId;
            subject = RCSDataBaseUtils.getGroupSubject(mContext, chatId);
            Logger.v(TAG, "initGroupData from DB #subject: " + subject);
            List<Participant> participants =
                    RCSDataBaseUtils.getGroupParticipants(mContext, chatId);
            for (Participant participant : participants)
                groupParticipants.add(new GroupParticipant(participant, null));
        }

        public List<Participant> getParticipants() {
            List<Participant> participants = new ArrayList<Participant>();
            for (GroupParticipant participant : groupParticipants) {
                participants.add(participant.getParticipant());
            }
            return participants;
        }

        public boolean containParticipant(Participant participant) {
            if (RCSDataBaseUtils.getSingleGroupParticipant(mContext,
                    chatId, participant.getContact()) != null)
                return true;
            return false;
        }

        void handleConferenceNotify(String confState, List<ConferenceUser> users) {
            // just for logs remove later.
            for (ConferenceUser user : users) {
                Logger.v(TAG,"handleConferenceNotify #contact:" + user.getEntity() + ", #role:"
                                + user.getRole() + ", #state:" + user.getState() + ", #status:"
                                + user.getStatus() + ", #displayName: " + user.getDisplayName());
            }
            // just for logs
            if (confState.equalsIgnoreCase(GroupChat.ConfState.FULL)) {
                updateDone = true;
                handleFullConferenceNotify(users);
            } else if (confState.equalsIgnoreCase(GroupChat.ConfState.PARTIAL)) {
                handlePartialConferenceNotify(users);
            }
            syncParticipant();
        }

        void handleFullConferenceNotify(List<ConferenceUser> users) {
            List<Participant> currentUsers =
                    RCSDataBaseUtils.getGroupParticipants(mContext, chatId);
            for (Participant contact : currentUsers) {
                boolean left = true;
                for (ConferenceUser user : users) {
                    if (RCSUtils.extractNumberFromUri(user.getEntity()).
                            equalsIgnoreCase(contact.getContact())) {
                        left = false;
                        break;
                    }
                }
                if (left == true) {
                    RCSDataBaseUtils.removeGroupParticipant(mContext, chatId, contact.getContact());
                    handleParticipantLeft(contact.getContact(), null);
                }
            }
            ConferenceUser chairman = null;
            boolean reportUI = true;
            for (ConferenceUser user : users) {
                if (user.getRole().equalsIgnoreCase(ConferenceUser.Role.CHAIRMAN)) {
                    chairman = user;
                    break;
                }
            }

            String me = GsmaManager.getInstance().getMSISDN();
            Logger.v(TAG, "handleFullConferenceNotify ME:" + me);
            String chairmanNum = null;
            if (chairman != null)
                chairmanNum = RCSUtils.extractNumberFromUri(chairman.getEntity());
            Logger.v(TAG, "handleFullConferenceNotify chairmanNum:" + chairmanNum);

            if (currentUsers.size() == 1 && currentUsers.get(0).getContact().equalsIgnoreCase(me)
                    && !me.equalsIgnoreCase(chairmanNum)) {
                Logger.v(TAG, "handleFullConferenceNotify reportUI to false");
                reportUI = false;
            }

            for (ConferenceUser user : users) {
                String contact = RCSUtils.extractNumberFromUri(user.getEntity());
                String role = user.getRole();
                String state = user.getState();
                String status = user.getStatus();
                String displayName = user.getDisplayName();

                GroupParticipant groupParticipant =
                        RCSDataBaseUtils.getSingleGroupParticipant(mContext, chatId, contact);
                if (groupParticipant == null) {
                    RCSDataBaseUtils.addGroupParticipant(
                            mContext, chatId, contact, displayName, status);
                    if (reportUI) {
                        if (status.equalsIgnoreCase(ConferenceUser.Status.PENDING)) {
                            // report pending state
                        } else {
                            handleParticipantJoined(contact, displayName);
                        }
                    }
                } else {
                    RCSDataBaseUtils.updateGroupParticipant(
                            mContext, chatId, contact, displayName, status);
                    if (groupParticipant.getState().equalsIgnoreCase(status)) {
                        continue;
                    }
                    if (status.equalsIgnoreCase(ConferenceUser.Status.PENDING) && reportUI) {
                        RCSDataBaseUtils.updateGroupParticipant(
                                mContext, chatId, contact, displayName,
                                ConferenceUser.Status.CONNECTED);
                        // handleParticipantLeft(contact, displayName);
                        // report pending ?
                        // Server modified. After 2015.04.20,
                        // server do not report the pending status user
                    } else if (status.equalsIgnoreCase(ConferenceUser.Status.CONNECTED)) {
                        if (groupParticipant.getState().equalsIgnoreCase(
                                ConferenceUser.Status.PENDING)
                                && reportUI) {
                            //handleParticipantJoined(contact, displayName);
                        }
                    } else if (status.equalsIgnoreCase(ConferenceUser.Status.DISCONNECTED)) {
                        if (groupParticipant.getState().equals(ConferenceUser.Status.PENDING)
                                && reportUI) {
                            //handleParticipantJoined(contact, displayName);
                        }
                    }
                }
            }
        }

        void handlePartialConferenceNotify(List<ConferenceUser> users) {
            for (ConferenceUser user : users) {
                String contact = RCSUtils.extractNumberFromUri(user.getEntity());
                String role = user.getRole();
                String state = user.getState();
                String status = user.getStatus();
                String displayName = user.getDisplayName();

                GroupParticipant groupParticipant =
                        RCSDataBaseUtils.getSingleGroupParticipant(mContext, chatId, contact);
                if (groupParticipant == null) {
                    Logger.v(TAG, "handlePartialConferenceNotify User not in DB");
                    if (state.equalsIgnoreCase(ConferenceUser.State.DELETED)) {
                        //not happened.
                    } else {
                        RCSDataBaseUtils.addGroupParticipant(
                                mContext, chatId, contact, displayName, status);
                        if (status.equalsIgnoreCase(ConferenceUser.Status.PENDING)) {
                            //Pending state report.
                        } else {
                            handleParticipantJoined(contact, displayName);
                        }
                    }
                } else {
                    if (state.equalsIgnoreCase(ConferenceUser.State.DELETED)) {
                        RCSDataBaseUtils.removeGroupParticipant(mContext, chatId, contact);

                        if (status.equalsIgnoreCase(ConferenceUser.Status.DISCONNECTED)) {
                            String method = user.getDisconnectMethod();
                            if (method.equalsIgnoreCase(ConferenceUser.Method.BOOTED)) {
                                handleParticipantBeenKickOut(contact, displayName);
                            } else if (method.equalsIgnoreCase(ConferenceUser.Method.DEPARTED)) {
                                handleParticipantLeft(contact, displayName);
                            }
                        }
                    } else {
                        RCSDataBaseUtils.updateGroupParticipant(
                                mContext, chatId, contact, displayName, status);
                        if (groupParticipant.getState().equalsIgnoreCase(status)) {
                            continue;
                        }
                        if (status.equalsIgnoreCase(ConferenceUser.Status.CONNECTED)) {
                            if (groupParticipant.getState().equalsIgnoreCase(
                                    ConferenceUser.Status.PENDING)) {
                                //handleParticipantJoined(contact, displayName);
                            }
                        } else if (status.equalsIgnoreCase(ConferenceUser.Status.DISCONNECTED)) {
                            // do noting when old state is connected, and pending will not happened
                        } else if (status.equalsIgnoreCase(ConferenceUser.Status.PENDING)) {
                            // report left then pending.?
                        }
                    }
                }
            }
        }

        private void handleParticipantJoined(String contact, String displayName) {
            Participant participant = new Participant(contact, displayName);
            mService.getListener().onParticipantJoined(getChatId(), participant);
        }

        private void handleParticipantLeft(String contact, String displayName) {
            final Participant participant = new Participant(contact, displayName);
            mService.getListener().onParticipantLeft(getChatId(), participant);
        }

        private void handleParticipantBeenKickOut(String contact, String displayName) {
            Logger.d(TAG, "handleParticipantBeenKickOut() contact: " + contact);
            final Participant participant = new Participant(contact, displayName);
            mService.getListener().onParticipantRemoved(getChatId(), participant);
        }

        public void syncParticipant() {
            if (chatId == null) {
                Logger.v(TAG, "syncParticipant error, No ChatId");
            }

            groupParticipants.clear();
            String[] projection = { GroupMemberData.COLUMN_CONTACT_NUMBER,
                    GroupMemberData.COLUMN_CONTACT_NAME, GroupMemberData.COLUMN_STATE, };
            String selection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
            Cursor cursor =
                    resolver.query(GroupMemberData.CONTENT_URI, projection, selection, null, null);
            if (cursor == null)
                return;
            while (cursor.moveToNext()) {
                String contact = cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER));
                String displayName = cursor.getString(cursor
                        .getColumnIndex(GroupMemberData.COLUMN_CONTACT_NAME));
                int state = cursor.getInt(cursor.getColumnIndex(GroupMemberData.COLUMN_STATE));
                Logger.v(TAG, "syncParticipant from DB #contact: " + contact + ",#displayName:"
                        + displayName + ",#state:" + state);
                groupParticipants.add(new GroupParticipant(new Participant(contact, displayName),
                        RCSDataBaseUtils.transferMemberStatusToString(state)));
            }
            cursor.close();
        }

        public void syncSubject() {
            subject = RCSDataBaseUtils.getGroupSubject(mContext, chatId);
        }

        public void clear() {
            groupParticipants.clear();
            Logger.v(TAG, "clear() from DB #chatId: " + chatId);
            int deletedRows = resolver.delete(
                    GroupMemberData.CONTENT_URI,
                    GroupMemberData.COLUMN_CHAT_ID + " = '" + chatId + "'",
                    null);
            Logger.v(TAG, "clear() from DB #deletedRows: " + deletedRows);
        }

        public boolean newGroup() {
            boolean result = true;
            Cursor cursor = resolver.query(GroupMemberData.CONTENT_URI,
                    new String[] { GroupMemberData.COLUMN_CONTACT_NUMBER, }, "("
                            + GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "')", null, null);
            if (cursor == null)
                return true;
            if (cursor.getCount() > 0) {
                result = false;
            }
            cursor.close();
            return result;
        }

        public void addRejoinId(String rejoinId) {
            Logger.v(TAG, "addRejoinId To DB #chatId: " + chatId + ",#rejoinId:" + rejoinId);
            RCSDataBaseUtils.addRejoinId(mContext, chatId, rejoinId);
        }

        public String getRejoinId() {
            String[] projection = { GroupChatData.KEY_REJOIN_ID, };
            String selection = GroupChatData.KEY_CHAT_ID + "='" + chatId + "'";
            Cursor cursor = resolver.query(groupRejoinUri, projection, selection, null, null);

            String rejoinId = null;
            if (cursor == null)
                return null;
            if (cursor.moveToNext()) {
                rejoinId = cursor.getString(cursor.getColumnIndex(GroupChatData.KEY_REJOIN_ID));
            }
            cursor.close();
            Logger.v(TAG, "getRejoinId from DB #chatId: " + chatId + ",#rejoinId:" + rejoinId);
            return rejoinId;
        }

        // copy from PhoneUtils.java TODO: add International prefix code
        public String extractNumberFromUri(String uri) {
            if (uri == null) {
                return null;
            }

            try {
                // Extract URI from address
                int index0 = uri.indexOf("<");
                if (index0 != -1) {
                    uri = uri.substring(index0 + 1, uri.indexOf(">", index0));
                }

                // Extract a Tel-URI
                int index1 = uri.indexOf("tel:");
                if (index1 != -1) {
                    uri = uri.substring(index1 + 4);
                }

                // Extract a SIP-URI
                index1 = uri.indexOf("sip:");
                if (index1 != -1) {
                    int index2 = uri.indexOf("@", index1);
                    uri = uri.substring(index1 + 4, index2);
                }

                // Remove URI parameters
                int index2 = uri.indexOf(";");
                if (index2 != -1) {
                    uri = uri.substring(0, index2);
                }
                return uri;

                // Format the extracted number (username part of the URI)
                // return formatNumberToInternational(uri);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * @param msg
     * @return
     */
    @Override
    public String msgToString(int msg) {
        String result = "";
        switch (msg) {
        case OP_GROUP_START:
            result = "OP_GROUP_START";
            break;
        case OP_GROUP_UPDATE_STATUS:
            result = "OP_GROUP_UPDATE_STATUS";
            break;
        case OP_SEND_CHATMESSAGE:
            result = "OP_SEND_CHATMESSAGE";
            break;
        case OP_RESEND_CHATMESSAGE:
            result = "OP_RESEND_CHATMESSAGE";
            break;
        case OP_ADD_PARTICIPANT:
            result = "OP_ADD_PARTICIPANT";
            break;
        case OP_REMOVE_PARTICIPANT:
            result = "OP_REMOVE_PARTICIPANT";
            break;
        case OP_TRANSFER_CHAIRMAN:
            result = "OP_TRANSFER_CHAIRMAN";
            break;
        case OP_MODIFY_SUBJECT:
            result = "OP_MODIFY_SUBJECT";
            break;
        case OP_MODIFY_MYNICKNAME:
            result = "OP_MODIFY_MYNICKNAME";
            break;
        case OP_QUIT_GROUP:
            result = "OP_QUIT_GROUP";
            break;
        case OP_ABORT_GROUP:
            result = "OP_ABORT_GROUP";
            break;
        case OP_GROUP_INVITATION:
            result = "OP_GROUP_INVITATION";
            break;
        case OP_GROUP_INVITATION_ACCEPT:
            result = "OP_GROUP_INVITATION_ACCEPT";
            break;
        case OP_GROUP_INVITATION_REJECT:
            result = "OP_GROUP_INVITATION_REJECT";
            break;
        case OP_TRANSFER_GROUP_STATE:
            result = "OP_TRANSFER_GROUP_STATE";
            break;
        case OP_SEND_FILE:
            result = "OP_SEND_FILE";
            break;
        case OP_RESEND_FILE:
            result = "OP_RESEND_FILE";
            break;
        case OP_DOWNLOAD_FILE:
            result = "OP_DOWNLOAD_FILE";
            break;
        case OP_REDOWNLOAD_FILE:
            result = "OP_REDOWNLOAD_FILE";
            break;
        case OP_FT_INVITATION:
            result = "OP_FT_INVITATION";
            break;
        case NOTIFICATION_GROUP_STARTED:
            result = "NOTIFICATION_GROUP_STARTED";
            break;
        case NOTIFICATION_GROUP_ABORT:
            result = "NOTIFICATION_GROUP_ABORT";
            break;
        case NOTIFICATION_GROUP_ERROR:
            result = "NOTIFICATION_GROUP_ERROR";
            break;
        case NOTIFICATION_CHATMESSAGE_SENT:
            result = "NOTIFICATION_CHATMESSAGE_SENT";
            break;
        case NOTIFICATION_PARTICIPANT_JOINED:
            result = "NOTIFICATION_PARTICIPANT_JOINED";
            break;
        case NOTIFICATION_PARTICIPANT_LEFT:
            result = "NOTIFICATION_PARTICIPANT_LEFT";
            break;
        case NOTIFICATION_PARTICIPANT_DISCONNECTED:
            result = "NOTIFICATION_PARTICIPANT_DISCONNECTED";
            break;
        case NOTIFICATION_GROUP_QUIT_RST:
            result = "NOTIFICATION_GROUP_QUIT_RST";
            break;
        case NOTIFICATION_GROUP_ABORT_RST:
            result = "NOTIFICATION_GROUP_ABORT_RST";
            break;
        case NOTIFICATION_ADD_PARTICIPANT_RST:
            result = "NOTIFICATION_ADD_PARTICIPANT_RST";
            break;
        case NOTIFICATION_REMOVE_PARTICIPANT_RST:
            result = "NOTIFICATION_REMOVE_PARTICIPANT_RST";
            break;
        case NOTIFICATION_BEEN_KICKED_OUT:
            result = "NOTIFICATION_BEEN_KICKED_OUT";
            break;
        case NOTIFICATION_MODIFY_SUBJECT_RST:
            result = "NOTIFICATION_MODIFY_SUBJECT_RST";
            break;
        case NOTIFICATION_SUBJECT_CHANGED:
            result = "NOTIFICATION_SUBJECT_CHANGED";
            break;
        case NOTIFICATION_TRANSFER_CHAIRMAN_RST:
            result = "NOTIFICATION_TRANSFER_CHAIRMAN_RST";
            break;
        case NOTIFICATION_CHAIRMAN_TRANSFERED:
            result = "NOTIFICATION_CHAIRMAN_TRANSFERED";
            break;
        case NOTIFICATION_MODIFY_MYNICKNAME_RST:
            result = "NOTIFICATION_MODIFY_MYNICKNAME_RST";
            break;
        case NOTIFICATION_NICKNAME_CHANGED:
            result = "NOTIFICATION_NICKNAME_CHANGED";
            break;
        case NOTIFICATION_RECEIVE_MESSAGE:
            result = "NOTIFICATION_RECEIVE_MESSAGE";
            break;
        case NOTIFICATION_GROUP_DISSOLVED:
            result = "NOTIFICATION_GROUP_DISSOLVED";
            break;
        case NOTIFICATION_GROUP_ABORT_BY_CHAIRMAN:
            result = "NOTIFICATION_GROUP_ABORT_BY_CHAIRMAN";
            break;
        case NOTIFICATION_REGISTRATION_STATUS_FALSE:
            result = "NOTIFICATION_REGISTRATION_STATUS_FALSE";
            break;
        case NOTIFICATION_REGISTRATION_STATUS_TRUE:
            result = "NOTIFICATION_REGISTRATION_STATUS_TRUE";
            break;
        case NOTIFICATION_CORESERVICE_DOWN:
            result = "NOTIFICIATION_CORESERVICE_DOWN";
            break;
        default:
            result = "OP_NOT_DEFINE";
            break;
        }
        return result;
    }
}
