package com.mediatek.rcs.messageservice.chat;

import java.util.ArrayList;
import java.util.List;

import android.os.Message;

import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.utils.Logger;

public class PendingMessageManager {
    public static final String TAG = "PendingMessageManager";
    private RCSChatServiceBinder mService = null;
    private List<PendingMessage> mPendingMessages = new ArrayList<PendingMessage>();

    PendingMessageManager(RCSChatServiceBinder service) {
        mService = service;
    }

    synchronized void addPendingMessage(PendingMessage message) {
        Logger.d(TAG, "addPendingMessage()  msgId: " + message);
        if (message.rcsMsgId != null && rcsMsgContains(message.rcsMsgId) != null)
            return;
        if (message.smsMsgId != -1 && smsMsgContains(message.smsMsgId) != null)
            return;
        mPendingMessages.add(message);
    }

    synchronized void removePendingMessage(PendingMessage message) {
        mPendingMessages.remove(message);
    }

    synchronized void removePendingMessage(String msgId) {
        Logger.d(TAG, "removePendingMessage()  msgId: " + msgId);
        for (PendingMessage message : mPendingMessages) {
            if (msgId.equalsIgnoreCase(message.rcsMsgId)) {
                mPendingMessages.remove(message);
                break;
            }
        }
    }

    synchronized void removePendingMessage(long smsMsgId) {
        Logger.d(TAG, "removePendingMessage()  smsMsgId: " + smsMsgId);
        for (PendingMessage message : mPendingMessages) {
            if (message.smsMsgId == smsMsgId) {
                mPendingMessages.remove(message);
                break;
            }
        }
    }

    PendingMessage rcsMsgContains(String rcsMsgId) {
        for (PendingMessage message : mPendingMessages) {
            if (message.rcsMsgId != null && message.rcsMsgId.equalsIgnoreCase(rcsMsgId)) {
                return message;
            }
        }
        return null;
    }

    PendingMessage smsMsgContains(long smsMsgId) {
        for (PendingMessage message : mPendingMessages) {
            if (message.smsMsgId == smsMsgId) {
                return message;
            }
        }
        return null;
    }

    PendingMessage contains(PendingMessage pendingMsg) {
        for (PendingMessage message : mPendingMessages) {
            if (message == pendingMsg) {
                return message;
            }
        }
        return null;
    }

    void handlePendingMessage(Message msg) {
        Logger.d(TAG, "handleMessage():" + msg.what);
        switch (msg.what) {
            case BaseChatImpl.BASE_OP_RESEND_MESSAGE:
                PendingMessage pendingMsg = rcsMsgContains((String) msg.obj);
                Logger.d(TAG, "handlePendingMessage() rcsMsgContains:" + pendingMsg);
                if (pendingMsg == null) {
                    break;
                } else {
                    handleResendChatMessage(pendingMsg);
                }
                break;
            case BaseChatImpl.BASE_OP_SEND_MESSAGE_RST:
                pendingMsg = (PendingMessage) msg.obj;
                Logger.d(TAG, "handlePendingMessage()  msgId: " + pendingMsg);
                if (contains(pendingMsg) != null) {
                    Logger.d(TAG, "handlePendingMessage()  msgId contains: " + pendingMsg);
                    removePendingMessage(pendingMsg);
                    handlePendingMessageRst(pendingMsg);
                }
                break;
        }
    }

    void handleRegistrationChanged(boolean status) {
        Logger.d(TAG, "handleRegistrationChanged(): " + status);
        if (status) {
            for (PendingMessage message : mPendingMessages) {
                int type = message.chat;
                Logger.d(TAG, "handleRegistrationChanged(): message: " + message);
                switch (type) {
                    case PendingMessage.ONE2ONE:
                         One2OneChat o2oChat = mService.getOne2OneChat(message.chatId);
                        if (message.rcsMsgId == null) {
                            o2oChat.sendChatMessage(message.smsMsgId, message.content,
                                    message.msgType);
                        } else {
                            //o2oChat.resendChatMessage(message.rcsMsgId);
                        }
                        break;
                    case PendingMessage.ONE2MULTI:
                        One2MultiChat o2mChat = mService.getO2MChat(message.chatId);
                        if (message.rcsMsgId == null) {
                            o2mChat.sendChatMessage(message.smsMsgId, message.content,
                                                        Class.NORMAL);
                        }else{
                            //o2mChat.resendChatMessage(message.rcsMsgId);
                        }
                        break;
                    case PendingMessage.GROUP:
                        SimpleGroupChat groupChat = mService.getGroupChat(message.chatId);
                        if (message.rcsMsgId == null) {
                            groupChat.sendChatMessage(message.msgType, message.smsMsgId,
                                                message.content);
                        }else{
                        }
                        break;
                }
            }
        }
    }

    void handleResendChatMessage(PendingMessage pendingMsg) {
        int type = pendingMsg.chat;
        switch (type) {
            case PendingMessage.ONE2ONE:
                One2OneChat o2oChat = mService.getOne2OneChat(pendingMsg.chatId);
                o2oChat.resendChatMessage(pendingMsg.rcsMsgId);
                break;
            case PendingMessage.ONE2MULTI:
                One2MultiChat o2mChat = mService.getO2MChat(pendingMsg.chatId);
                o2mChat.resendChatMessage(pendingMsg.rcsMsgId);
                break;
            case PendingMessage.GROUP:
                SimpleGroupChat groupChat = mService.getGroupChat(pendingMsg.chatId);
                groupChat.resendChatMessage(pendingMsg.rcsMsgId);
                break;
        }
    }

    void handlePendingMessageRst(PendingMessage pendingMsg) {
        int type = pendingMsg.chat;
        switch (type) {
            case PendingMessage.ONE2ONE:
                One2OneChat o2oChat = mService.getOne2OneChat(pendingMsg.chatId);
                if (pendingMsg.rcsMsgId == null) {
                    o2oChat.notifyMessageSendFail(pendingMsg.smsMsgId);
                } else {
                    o2oChat.onReceiveMessageDeliveryStatus(pendingMsg.rcsMsgId, "failed");
                }
                break;
            case PendingMessage.ONE2MULTI:
                One2MultiChat o2mChat = mService.getO2MChat(pendingMsg.chatId);
                if (pendingMsg.rcsMsgId == null) {
                    o2mChat.notifyMessageSendFail(pendingMsg.smsMsgId);
                } else {
                    o2mChat.onReceiveMessageDeliveryStatus(pendingMsg.rcsMsgId, "failed");
                }
                break;
            case PendingMessage.GROUP:
                SimpleGroupChat groupChat = mService.getGroupChat(pendingMsg.chatId);
                if (pendingMsg.rcsMsgId == null) {
                    groupChat.notifyMessageSendFail(pendingMsg.smsMsgId);
                } else {
                    groupChat.onReceiveMessageDeliveryStatus(pendingMsg.rcsMsgId, "failed");
                }
                break;
        }
    }

    public static class PendingMessage {
        public static final int ONE2ONE = 0;
        public static final int ONE2MULTI = 1;
        public static final int GROUP = 2;

        public long smsMsgId = -1;
        public int msgType = -1;
        public String content = null;
        public String rcsMsgId = null;

        public int chat;
        public String chatId = null;

        public int pendingCnts = 3;
        public long pendingStartTime = 0;

        // for msg not send to stack
        PendingMessage(long smsMsgId, String content, int msgType) {
            this.msgType = msgType;
            this.smsMsgId = smsMsgId;
            this.content = content;
        }

        // for groupchat
        PendingMessage(long smsMsgId, String content) {
            this(smsMsgId, content, -1);
        }

        // for msg has been send to stack
        PendingMessage(String rcsMsgId) {
            this.rcsMsgId = rcsMsgId;
        }

        public void setChat(int type) {
            chat = type;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

        public void setPendingStartTime(long time) {
            pendingStartTime = time;
        }

        public String toString() {
            if (rcsMsgId == null)
                return "#smsMsgId: " + smsMsgId;
            else
                return "#rcsMsgId: " + rcsMsgId;
        }
    }
}
