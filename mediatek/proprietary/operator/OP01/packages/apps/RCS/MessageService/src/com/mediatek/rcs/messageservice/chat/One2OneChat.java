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

import java.io.UnsupportedEncodingException;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.GeolocMessage;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.ReceiveMessageStruct;
import com.mediatek.rcs.common.provider.SpamMsgData;
import com.mediatek.rcs.common.provider.SpamMsgUtils;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.messageservice.utils.Logger;
import com.mediatek.rcs.messageservice.chat.PendingMessageManager.PendingMessage;

/**
 * This class is the implementation of a 1-2-1 chat model
 */
public class One2OneChat extends BaseChatImpl {
    public final String TAG = this.toString().substring(this.toString().lastIndexOf('.') + 1);

    private Participant mParticipant = null;
    private Chat mChatImpl = null;
    private ChatListener mListener = null;

    public One2OneChat(RCSChatServiceBinder service, Object tag, String contact) {
        super(service, tag);

        mParticipant = new Participant(contact, null);
        mListener = new One2OneChatListener();
        Logger.d(TAG, "Constructor contact: " + contact);
    }

    /**
     * Get participant of this chat.
     *
     * @return participant of this chat.
     */
    public Participant getParticipant() {
        return mParticipant;
    }

    /**
     * Judge whether participants is duplicated.
     *
     * @param participants
     *            The participants to be compared.
     * @return True, if participants is duplicated, else false.
     */
    public boolean isDuplicated(Participant participant) {
        return mParticipant.equals(participant);
    }

    public String sendMessage(final int type, final String content) {
        Chat o2oChatImpl = null;
        String messageId = null;
        ChatService chatService = null;
        try {
            Logger.d(TAG, "sendMessage() to stack. remote: " + mParticipant.getContact());
            chatService = mGsmaManager.getChatApi();
            o2oChatImpl = chatService.getChat(mParticipant.getContact());
            // if null, create one.
            if (null == o2oChatImpl) {
                Logger.d(TAG, "sendMessage() stack ChatImpl not found, create one");
                o2oChatImpl = chatService.openSingleChat(mParticipant.getContact(), mListener);
                if (o2oChatImpl == null) {
                    //stack error, timing issue.
                    return null;
                }
                if (mChatImpl != null) {
                    try {
                        mChatImpl.removeEventListener(mListener);
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                mChatImpl = o2oChatImpl;
            } else {
                //OMG need check later.//ALPS02042192
                try {
                    o2oChatImpl.removeEventListener(mListener);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    o2oChatImpl.addEventListener(mListener);
                }
            }
            //This is so...
            int byteLength = content.getBytes("UTF-8").length;

            Logger.d(TAG, "sendMessage() MessageType: " +
                        (type == Class.NORMAL ? "Normal" :
                            (type == Class.EMOTICON ? "Emoticon" : "Burn")));
            Logger.d(TAG, "sendMessage() MessageLength: " + byteLength);
            if (type == Class.NORMAL) {
                if (byteLength > MAX_PAGER_MODE_MSG_LENGTH)
                    messageId = o2oChatImpl.sendMessageByLargeMode(content);
                else
                    messageId = o2oChatImpl.sendMessageByPagerMode(content);
            } else if (type == Class.EMOTICON) {
                messageId = o2oChatImpl.sendEmoticonShopMessage(content);
            } else if (type == Class.BURN) {// burning message;
                if (byteLength > MAX_PAGER_MODE_MSG_LENGTH)
                    messageId = o2oChatImpl.sendLargeModeBurnMessage(content);
                else
                    messageId = o2oChatImpl.sendPagerModeBurnMessage(content);
            }
            Logger.d(TAG, "sendMessage() messageId: " + messageId);
        } catch (JoynServiceException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return messageId;
    }

    public void sendChatMessage(final long msgIdInSMS, final String content, final int type) {
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                if (RCSUtils.in2GCall(GsmaManager.getContext())) {
                    notifyMessageSendFail(msgIdInSMS);
                    return;
                }
                Message sendMsgRst = mWorkHandler.obtainMessage(BASE_OP_SEND_MESSAGE_RST);
                PendingMessage pendingMsg;
                String rcsMsgId = null;
                if (!GsmaManager.isServiceAvailable()) {
                    Logger.w(TAG, "sendChatMessage() msg save in pending list: " + msgIdInSMS);
                    pendingMsg = new PendingMessage(msgIdInSMS, content, type);
                } else {
                    rcsMsgId = sendMessage(type, content);
                    Logger.w(TAG, "sendChatMessage() msgId in Stack DB: " + rcsMsgId);
                    // connect sms db and stack db
                    if (rcsMsgId == null) {
                        pendingMsg = new PendingMessage(msgIdInSMS, content, type);
                    } else {
                        RCSDataBaseUtils.combineMsgId(mContext, msgIdInSMS, rcsMsgId,
                                            MessageType.IM);
                        pendingMsg = new PendingMessage(rcsMsgId);
                    }
                }
                pendingMsg.setChat(PendingMessage.ONE2ONE);
                pendingMsg.setChatId(mParticipant.getContact());
                pendingMsg.setPendingStartTime(System.currentTimeMillis());

                PendingMessage resendingMessage = mService.getPendingMessageManager()
                        .smsMsgContains(msgIdInSMS);
                if (resendingMessage == null) {
                    mService.getPendingMessageManager().addPendingMessage(pendingMsg);
                    sendMsgRst.obj = pendingMsg;
                    mWorkHandler.sendMessageDelayed(sendMsgRst, MAX_MSG_PENDING_TIME);
                } else {
                    resendingMessage.rcsMsgId = rcsMsgId;
                }
            }
        };
        Logger.w(TAG, "sendChatMessage() post to worker thread");
        mWorkHandler.post(worker);
    }

    public void sendBurnDeliveryReport(String msgId) {
        if (TextUtils.isEmpty(msgId)) {
            Logger.d(TAG, "sendBurnDeliveryReport, null msgId");
            return;
        }
        Chat o2oChatImpl = null;
        ChatService chatService = null;
        try {
            Logger.d(TAG, "sendBurnDeliveryReport() to stack. remote: " +
                    mParticipant.getContact());
            chatService = mGsmaManager.getChatApi();
            o2oChatImpl = chatService.getChat(mParticipant.getContact());
            // if null, create one.
            if (null == o2oChatImpl) {
                Logger.d(TAG, "Can't Get ChatImpl, Create One");
                o2oChatImpl = chatService.openSingleChat(mParticipant.getContact(), mListener);
                if (o2oChatImpl == null) {
                    return;
                }
                if (mChatImpl != null) {
                    mChatImpl.removeEventListener(mListener);
                }
                mChatImpl = o2oChatImpl;
            }
            Logger.d(TAG, "sendBurnDeliveryReport() messageId: " + msgId);
            o2oChatImpl.sendBurnDeliveryReport(msgId);
        } catch (JoynServiceException e1) {
            e1.printStackTrace();
        }
    }

    public void resendChatMessage(String msgId) {
        Chat o2oChatImpl = null;
        ChatService chatService = null;
        if (RCSUtils.in2GCall(GsmaManager.getContext())) {
            onReceiveMessageDeliveryStatus(msgId, "failed");
            return;
        }
        if (!GsmaManager.isServiceAvailable()) {
            if (mService.getPendingMessageManager().rcsMsgContains(msgId) == null) {
                Message sendMsgRst = mWorkHandler.obtainMessage(BASE_OP_SEND_MESSAGE_RST);
                PendingMessage pendingMsg = new PendingMessage(msgId);
                pendingMsg.setChat(PendingMessage.ONE2ONE);
                pendingMsg.setChatId(mParticipant.getContact());
                pendingMsg.setPendingStartTime(System.currentTimeMillis());

                mService.getPendingMessageManager().addPendingMessage(pendingMsg);
                sendMsgRst.obj = pendingMsg;
                mWorkHandler.sendMessageDelayed(sendMsgRst, MAX_MSG_PENDING_TIME);
            }
            Message msg = mWorkHandler.obtainMessage(BASE_OP_RESEND_MESSAGE);
            msg.obj = msgId;
            mWorkHandler.sendMessageDelayed(msg, 5 * 1000);
            return;
        }
        try {
            Logger.d(TAG, "resendChatMessage() to stack. remote: " + mParticipant.getContact());
            chatService = mGsmaManager.getChatApi();
            o2oChatImpl = chatService.getChat(mParticipant.getContact());
            // if null, create one.
            if (null == o2oChatImpl) {
                Logger.d(TAG, "Can't Get ChatImpl, Create One");
                o2oChatImpl = chatService.openSingleChat(mParticipant.getContact(), mListener);
                if (o2oChatImpl == null) {
                    //we can not get the exception from stack.
                    //just return wait for timeout then send fail.
                    return;
                }
                if (mChatImpl != null) {
                    mChatImpl.removeEventListener(mListener);
                }
                mChatImpl = o2oChatImpl;
            }
            Logger.d(TAG, "resendChatMessage() messageId: " + msgId);
            o2oChatImpl.resendMessage(msgId);
        } catch (JoynServiceException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void notifyMessageSendFail(final long msgId) {
        mService.getListener().onSendO2OMessageFailed(msgId);
    }

    public void onReceiveChatMessage(ChatMessage message) {
        long ipmsgId = RCSDataBaseUtils.getStackMessageId(
                mContext, message.getId(), ChatLog.Message.Direction.INCOMING);
        if (RCSUtils.isIpSpamMessage(mService.getContext(), message.getContact())) {
            Logger.d(TAG, "onReceiveChatMessage, spam msg, contact=" + message.getContact());
            int type = SpamMsgData.Type.TYPE_IP_TEXT_MSG;
            if (message.isBurnMessage()) {
                type = SpamMsgData.Type.TYPE_IP_BURN_TEXT_MSG;
            } else if (message.isCloudMessage()) {
                type = SpamMsgData.Type.TYPE_CLOUD_MSG;
            } else if (message.isEmoticonMessage()) {
                type = SpamMsgData.Type.TYPE_EMOTICON_MSG;
            }
            SpamMsgUtils.getInstance(mService.getContext()).insertSpamMessage(
                    message.getMessage(),
                    message.getContact(),
                    RCSUtils.getRCSSubId(),
                    type,
                    ipmsgId,
                    message.getId());
            return;
        }
        int messageType = Class.NORMAL;
        if (message.isBurnMessage()) {
            messageType = Class.BURN;
        } else if (message.isEmoticonMessage()) {
            messageType = Class.EMOTICON;
        }
        MessageStruct struct = new ReceiveMessageStruct(mContext, message.getContact(),
                message.getMessage(), message.getId(), ipmsgId, messageType);
        long msgId = struct.saveMessage();
        // report UI
        mService.getListener().onNewMessage(msgId);

        // check if need display report. CMCC not need, can remove.
        try {
            ChatService chatService = mGsmaManager.getChatApi();
            Chat o2oChatImpl = chatService.getChat(mParticipant.getContact());
            if (o2oChatImpl == null) {
                Logger.d(TAG, "Can't Get ChatImpl, Create One");
                o2oChatImpl = chatService.openSingleChat(mParticipant.getContact(), mListener);
                if (mChatImpl != null) {
                    mChatImpl.removeEventListener(mListener);
                }
                mChatImpl = o2oChatImpl;
            }
            if (message.isDisplayedReportRequested()) {
                Logger.v(TAG, "sendDisplayedDeliveryReport");
                //CMCC not need
                //o2oChatImpl.sendDisplayedDeliveryReport(message.getId());
            }
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    private void deleteBurnedMsg(final String msgId) {
        Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg()  ");
        ContentResolver resolver = mService.getContext().getContentResolver();
        String[] projection = { ChatLog.Message.MESSAGE_TYPE, ChatLog.Message.ID };
        Cursor cursor = resolver.query(RCSUtils.RCS_URI_MESSAGE, projection, "msg_id='" + msgId
                + "'", null, null);
        try {
            if (cursor == null) {
                Log.d(TAG, " drawDeleteBARMsgIndicator cursor == null");
            }
            if (cursor != null && cursor.moveToFirst()) {
                boolean isBurned = cursor.getInt(cursor.getColumnIndex(
                        ChatLog.Message.MESSAGE_TYPE)) == ChatLog.Message.Type.BURN ? true : false;
                Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg, isBurned=" + isBurned);
                if (isBurned) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg, run");
                                Thread.sleep(5100);
                                RCSDataBaseUtils.deleteBurnMessage(
                                    mContext,
                                    msgId,
                                    Direction.OUTGOING);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return;
    }

    public void handleCapabilityChanged(Capabilities capability) {
        final boolean burnMessageEnable = capability.isBurnAfterRead();
        Logger.d(TAG, "handleCapabilityChanged #burnMessageEnable: " + burnMessageEnable);
        mService.getListener().onRequestBurnMessageCapabilityResult(mParticipant.getContact(),
                burnMessageEnable);
    }

    public void onStatusChanged(boolean status) {
        Logger.w(TAG, "onStatusChanged the status is " + status);
    }

    private class One2OneChatListener extends ChatListener {

        private final String TAG = One2OneChat.this.TAG + "#ChatListener";

        public One2OneChatListener() {
        }

        /**
         * Callback called when a new message has been received
         *
         * @param message
         *            Chat message
         * @see ChatMessage
         */
        @Override
        public void onNewMessage(final ChatMessage message) {
            Logger.d(TAG, "onNewMessage()   message id:" + message.getId() + " ,message text:"
                    + message.getMessage());
            onReceiveChatMessage(message);
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
            // not use in CMCC RCS, the geoloc message for CMCC is just file
            // transfer
            // encode&decode file at app.
        }

        /**
         * Callback called when a message has been delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageDelivered(final String msgId) {
            Logger.d(TAG, "onReportMessageDelivered()  msgId:" + msgId);
            onReceiveMessageDeliveryStatus(msgId, "delivered");
        }

        /**
         * Callback called when a message has been displayed by the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageDisplayed(final String msgId) {
            Logger.d(TAG, "onReportMessageDisplayed()  msgId:" + msgId);
        }

        /**
         * Callback called when a message has failed to be delivered to the remote
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onReportMessageFailed(final String msgId) {
            Logger.d(TAG, "onReportMessageFailed()  msgId:" + msgId);
            Message msg = mWorkHandler.obtainMessage(BASE_OP_RESEND_MESSAGE);
            msg.obj = msgId;
            mWorkHandler.sendMessageDelayed(msg, 5 * 1000);
        }

        /**
         * Callback called when a message has been sent to the server
         *
         * @CMCC RCS spec
         *
         * @param msgId
         *            Message ID
         */

        @Override
        public void onReportSentMessage(final String msgId) {
            Logger.d(TAG, "onReportSentMessage()  msgId:" + msgId);
            mService.getPendingMessageManager().removePendingMessage(msgId);
            onReceiveMessageDeliveryStatus(msgId, "sent");
            deleteBurnedMsg(msgId);
        }

        /**
         * Callback called when a message has been delivered to the remote contact.
         *
         * @CMCC RCS spec
         *
         * @param msgId
         *            Message ID
         */

        @Override
        public void onReportDeliveredMessage(final String msgId) {
            Logger.d(TAG, "onReportDeliveredMessage()  msgId:" + msgId);
            onReceiveMessageDeliveryStatus(msgId, "delivered");
        }

        /**
         * Callback called when a new burn message arrived.
         *
         * @CMCC RCS spec
         *
         * @param msgId
         *            Message ID
         */
        @Override
        public void onNewBurnMessageArrived(final ChatMessage msg) {
            Logger.d(TAG, "onNewBurnMessageArrived()   message id:" + msg.getId()
                    + " message text:" + msg.getMessage());
            onReceiveChatMessage(msg);
        }

        /**
         * Callback called when a message send unsuccessfully.
         *
         * @CMCC RCS spec
         *
         * @param msgId
         *            Message ID
         */

        @Override
        public void onReportFailedMessage(final String msgId, int errtype, String statusCode) {
            Logger.d(TAG, "onReportFailedMessage()  msgId:" + msgId + " ,errorType: " + errtype
                    + " ,statusCode: " + statusCode);
            // Message msg = mWorkHandler.obtainMessage(BASE_OP_RESEND_MESSAGE);
            // msg.obj = msgId;
            // mWorkHandler.sendMessageDelayed(msg, 5 * 1000);
        }

        /**
         * Callback called when an Is-composing event has been received. If the remote is typing a
         * message the status is set to true, else it is false.
         *
         * @param status
         *            Is-composing status
         */
        @Override
        public void onComposingEvent(final boolean status) {
            Logger.d(TAG, "onComposingEvent()  session: the status is " + status);
        }
    }
}
