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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatIntent;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.ChatServiceConfiguration;
import org.gsma.joyn.chat.ConferenceEventData;
import org.gsma.joyn.chat.ConferenceEventData.ConferenceUser;
import org.gsma.joyn.chat.GeolocMessage;
import org.gsma.joyn.chat.GroupChatIntent;
import org.gsma.joyn.chat.SpamReportListener;
import org.gsma.joyn.ft.FileSpamReportListener;
import org.gsma.joyn.ft.FileTransferIntent;
import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.ThreadFlag;
import com.mediatek.rcs.common.provider.GroupChatUtils;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.SendMessageStruct;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.service.GroupParticipant;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.messageservice.utils.Logger;

public class RCSChatServiceBinder extends IRCSChatService.Stub {
    public final String TAG = "RCSChatServiceImpl";

    // The map retains Object&IChatBase
    private final Map<String, One2OneChat> mO2OChatMap =
            new ConcurrentHashMap<String, One2OneChat>();
    private final Map<String, SimpleGroupChat> mGroupChatMap =
            new ConcurrentHashMap<String, SimpleGroupChat>();
    private final Map<String, One2MultiChat> mO2MChatMap =
            new ConcurrentHashMap<String, One2MultiChat>();

    private RCSChatManagerReceiver mReceiver = null;
    private RCSChatServiceListenerWrapper mListener = null;
    private Service mService = null;

    private FileTransferManager mFTManager = null; //for filetransfer

    private PendingMessageManager mPendingMsgManager = null;

    private boolean mGroupInitComplete = true;

    private Handler mWorkHandler = null;
    private Handler mNotifyHandler = null;

    RcsSpamReportListener mSpamReportListener = null;
    RcsPagerModeSpamReportListener mPagerModeSpamReportListener = null;
    RcsFileSpamReportListener mFileSpamReportListener = null;
    ChatListener listener = null;

    public RCSChatServiceBinder(Service service) {
        Logger.d(TAG, "RCSChatServiceImpl #Constructor");
        mService = service;
        GsmaManager.initialize(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ChatIntent.ACTION_NEW_CHAT);
        intentFilter.addAction(ChatIntent.ACTION_DELIVERY_STATUS);
        intentFilter.addAction(GroupChatIntent.ACTION_NEW_INVITATION);
        intentFilter.addAction(FileTransferIntent.ACTION_NEW_INVITATION);
        intentFilter.addAction(FileTransferIntent.ACTION_DELIVERY_STATUS);

        mReceiver = new RCSChatManagerReceiver();
        mService.registerReceiver(mReceiver, intentFilter);

        mPendingMsgManager = new PendingMessageManager(this);
        mFTManager = FileTransferManager.getInstance(this);

        HandlerThread thread = new HandlerThread("RCSChatServiceImplWorker");
        thread.start();
        mWorkHandler = new WorkHandler(thread.getLooper());

        thread = new HandlerThread("RCSChatServiceImplNotifyer");
        thread.start();
        mNotifyHandler = new Handler(thread.getLooper());
        mListener = new RCSChatServiceListenerWrapper(service, mNotifyHandler);

        mSpamReportListener = new RcsSpamReportListener();
        mPagerModeSpamReportListener = new RcsPagerModeSpamReportListener();
        listener = new SpamReportChatListener();
        mFileSpamReportListener = new RcsFileSpamReportListener();
    }

    public void onDestroy() {
        GsmaManager.unInitialize();
        mService.unregisterReceiver(mReceiver);
    }

    public Handler getWorkHandler() {
        return mWorkHandler;
    }

    public Handler getNotifyHandler() {
        return mNotifyHandler;
    }

    public PendingMessageManager getPendingMessageManager() {
        return mPendingMsgManager;
    }

    public Context getContext() {
        return mService;
    }

    public RCSChatServiceListenerWrapper getListener() {
        return mListener;
    }

    public One2OneChat getOne2OneChat(String contact) {
        One2OneChat chat = mO2OChatMap.get(contact);
        if (chat == null) {
            chat = new One2OneChat(this, contact, contact);
            mO2OChatMap.put(contact, chat);
        }
        return chat;
    }

    public SimpleGroupChat getGroupChat(String chatId) {
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            Logger.v(TAG, "getGroupChat NULL Warningg!!!");
        }
        return groupChat;
    }

    public SimpleGroupChat getOrCreateGroupChat(String chatId) {
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            Logger.v(TAG, "getOrCreateGroupChat Create One!!!");
            groupChat = new SimpleGroupChat(this, chatId);
            mGroupChatMap.put(chatId, groupChat);
        }
        return groupChat;
    }

    public void removeGroupChat(String chatId) {
        mGroupChatMap.remove(chatId);
    }

    public One2MultiChat getO2MChat(String chatId) {
        One2MultiChat o2mChat = mO2MChatMap.get(chatId);
        if (o2mChat == null) {
            Logger.v(TAG, "getO2MChat NULL!!!");
        }
        return o2mChat;
    }

    private void handleO2OInvitation(Intent intent) {
        Logger.v(TAG, "handleOne2OneChatReceivedMessage() entry");
        String contactNumber = intent.getStringExtra(ChatIntent.EXTRA_CONTACT);
        String displayName = intent.getStringExtra(ChatIntent.EXTRA_DISPLAY_NAME);
        ChatMessage chatMessage = intent.getParcelableExtra(ChatIntent.EXTRA_MESSAGE);
        Logger.v(TAG, "handleOne2OneChatReceivedMessage contact:" + contactNumber);

        if (chatMessage == null) {
            Logger.v(TAG, "Just a invitation for larger mode.");
            return;
        } else if (chatMessage.isPublicMessage()) {
            return;
        }
        Logger.v(TAG, "handleOne2OneChatReceivedMessage MessageId: " + chatMessage.getId());
        One2OneChat chat = getOne2OneChat(contactNumber);
        chat.onReceiveChatMessage(chatMessage);
    }

    private void handleMessageDeliveryStatus(Intent intent) {
        // ChatIntent.ACTION_DELIVERY_STATUS
        String remoteContact = intent.getStringExtra(ChatIntent.EXTRA_CONTACT);
        String msgId = intent.getStringExtra("msgId");
        String status = intent.getStringExtra("status");
        Logger.v(TAG, "handleMessageDeliveryStatus() from broadcast, msgId: " + msgId
                + ", status: " + status);
        One2OneChat chat = getOne2OneChat(remoteContact);
        chat.onReceiveMessageDeliveryStatus(msgId, status);
        return;
    }

    private void handleGroupInvitation(Intent intent) {
        Logger.v(TAG, "handleNewGroupInvitation() entry");
        String contact = intent.getStringExtra(GroupChatIntent.EXTRA_CONTACT);
        String displayName = intent.getStringExtra(GroupChatIntent.EXTRA_DISPLAY_NAME);
        String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
        String subject = intent.getStringExtra(GroupChatIntent.EXTRA_SUBJECT);
        // boolean isGroupExist = intent.getBooleanExtra("isGroupChatExist", false);
        String rejoinId = intent.getStringExtra(GroupChatIntent.EXTRA_SESSION_IDENTITY);
        Logger.v(TAG, "handleNewGroupInvitation contact:" + contact + " displayName: "
                + displayName + " chatId: " + chatId + " subject: " + subject + " rejoinId: "
                + rejoinId);
        List<Participant> participantList = new ArrayList<Participant>();
        participantList.add(new Participant(contact, displayName));
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            groupChat = new SimpleGroupChat(this, subject, participantList);
            groupChat.setChatId(chatId);
            mGroupChatMap.put(chatId, groupChat);
        }
        groupChat.handleInvitation(rejoinId);
    }

    private void handleGroupFTInvitation(Intent intent) {
        Logger.v(TAG, "handleGroupFTInvitation() entry");
        String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
        SimpleGroupChat groupChat = mGroupChatMap.get(chatId);
        if (groupChat == null) {
            Logger.v(TAG, "handleGroupFTInvitation() groupChat == null");
            return;
        }
        groupChat.handleFTInvitation(intent);
    }

    public void handleRegistrationStatusChanged(boolean status) {
        for (SimpleGroupChat groupChat : mGroupChatMap.values()) {
            groupChat.onStatusChanged(status);
        }
        for (One2OneChat o2oChat : mO2OChatMap.values()) {
            o2oChat.onStatusChanged(status);
        }
        mFTManager.onStatusChanged(status);
        mPendingMsgManager.handleRegistrationChanged(status);
    }

    public void handleCoreServiceDown() {
        for (SimpleGroupChat groupChat : mGroupChatMap.values()) {
            groupChat.onCoreServiceDown();
        }
    }

    public void handleCapabilityChanged(String contact, Capabilities capability) {
        Logger.d(TAG, "handleCapabilityChanged #contact: " + contact);
        One2OneChat chat = getOne2OneChat(contact);
        chat.handleCapabilityChanged(capability);
    }

    public void handleGroupConferenceNotify(String chatId, ConferenceEventData data) {
        if (!data.getState().equalsIgnoreCase(ConferenceEventData.State.FULL)) {
            Logger.d(TAG, "handleGroupConferenceNotify() Partial return");
            return;
        }
        String me = GsmaManager.getInstance().getMSISDN();
        int isMeChairmen = PhoneNumberUtils.compare(data.getChairman(), me) ? 1 : 0;
        GroupChatUtils.getInstance(mService).insertGroupChatData(chatId, data.getSubject(),
                RCSUtils.getRCSSubId(), isMeChairmen);
        List<ConferenceUser> users = data.getUsers();

        Logger.d(TAG, "handleGroupConferenceNotify() #chatId:" + chatId);

        List<Participant> currentUsers = RCSDataBaseUtils.getGroupParticipants(mService, chatId);
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
                RCSDataBaseUtils.removeGroupParticipant(mService, chatId, contact.getContact());
                getListener().onParticipantLeft(chatId, contact);
            }
        }
        boolean reportUI = true;
        Logger.v(TAG, "handleFullConferenceNotify ME:" + me + ", chairmen: " + data.getChairman());

        if (currentUsers.size() == 1 && currentUsers.get(0).getContact().equalsIgnoreCase(me)
                && !me.equalsIgnoreCase(data.getChairman())) {
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
                    RCSDataBaseUtils.getSingleGroupParticipant(mService, chatId, contact);
            if (groupParticipant == null) {
                RCSDataBaseUtils.addGroupParticipant(
                        mService, chatId, contact, displayName, status);
                if (reportUI) {
                    if (status.equalsIgnoreCase(ConferenceUser.Status.PENDING)) {
                        // report pending state
                    } else {
                        getListener().onParticipantJoined(
                                chatId,
                                new Participant(contact, displayName));
                    }
                }
            } else {
                RCSDataBaseUtils.updateGroupParticipant(
                        mService, chatId, contact, displayName, status);
            }
        }
    }


    @Override
    public boolean getRCSStatus() throws RemoteException {
        return GsmaManager.getInstance().getRCSStatus();
    }

    @Override
    public boolean getConfigurationStatus() throws RemoteException {
        return GsmaManager.getInstance().getConfigurationStatus();
    }

    @Override
    public boolean getRegistrationStatus() throws RemoteException {
        return GsmaManager.getInstance().getRegistrationStatus();
    }

    @Override
    public void getBurnMessageCapability(String contact) throws RemoteException {
        try {
            CapabilityService capabilityService = GsmaManager.getInstance().getCapabilityApi();
            Capabilities capability = capabilityService.getContactCapabilities(contact);
            if (capability != null) {
                One2OneChat chat = getOne2OneChat(contact);
                chat.handleCapabilityChanged(capability);
            }
            capabilityService.requestContactCapabilities(contact);
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getMSISDN() throws RemoteException {
        return GsmaManager.getInstance().getMSISDN();
    }

    @Override
    public void sendOne2OneMessage(String contact, String content, int type)
                                                    throws RemoteException {
        Logger.d(TAG, "sendOne2OneMessage #contact: " + contact + ", content: " + content);
        One2OneChat chat = getOne2OneChat(contact);
        MessageStruct struct = new SendMessageStruct(mService, contact, content, type);
        long msgIdInSMS = struct.saveMessage();
        chat.sendChatMessage(msgIdInSMS, content, type);
    }

    @Override
    public void sendOne2MultiMessage(List<String> contacts, String content, int type)
                                                                    throws RemoteException {
        Logger.d(TAG, "sendOne2MultiMessage #contacts: " + contacts + ", content: " + content);
        Collections.sort(contacts);
        MessageStruct struct = new SendMessageStruct(mService, contacts, content, type);
        long smsId = struct.saveMessage();
        String key = struct.mContact;
        One2MultiChat o2mChat = getO2MChat(key);
        if (o2mChat == null) {
            o2mChat = new One2MultiChat(this, key, contacts);
            mO2MChatMap.put(key, o2mChat);
        }
        o2mChat.sendChatMessage(smsId, content, type);
    }

//    @Override
//    public void sendBurnMessage(String contact, String content) throws RemoteException {
//        Logger.d(TAG, "sendOne2OneMessage #contact: " + contact + ", content: " + content);
//        One2OneChat chat = getOne2OneChat(contact);
//        MessageStruct struct = new SendMessageStruct(mService, contact, content, Class.BURN);
//        long msgIdInSMS = struct.saveMessage();
//        chat.sendChatMessage(msgIdInSMS, content, Class.BURN);
//    }

    @Override
    public void sendBurnDeliveryReport(String contact, String msgId) throws RemoteException {
        Logger.d(TAG, "sendBurnDeliveryReport #contact: " + contact + ", msgId: " + msgId);
        One2OneChat chat = getOne2OneChat(contact);
        chat.sendBurnDeliveryReport(msgId);
    }

    @Override
    public void blockMessages(String chatId, boolean block) throws RemoteException {
        Logger.d(TAG, "blockMessages #chatId: " + chatId + ", block=" + block);
        try {
            ChatService chatService = GsmaManager.getInstance().getChatApi();
            if (chatService != null) {
                chatService.blockGroupMessages(chatId, block);
            }
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendOne2OneFileTransfer(String contact, String filePath) throws RemoteException {
        Log.d(TAG, "sendOne2OneFileTransfer() enter, contact = " + contact + "filePath = "
                + filePath);
        boolean isBurn = false;
        mFTManager.handleSendFileTransferInvitation(contact, filePath, isBurn);
    }

    @Override
    public void sendOne2OneBurnFileTransfer(String contact, String filePath)
            throws RemoteException {
        Log.d(TAG, "sendOne2OneBurnFileTransfer() enter, contact = " + contact + "filePath = "
                + filePath);
        boolean isBurn = true;
        mFTManager.handleSendFileTransferInvitation(contact, filePath, isBurn);
    }

    @Override
    public void sendOne2MultiFileTransfer(List<String> contacts, String filePath)
            throws RemoteException {
        Log.d(TAG, "sendOne2MultiFileTransfer() enter, contacts = " + contacts + "filePath = "
                + filePath);
        mFTManager.handleSendFileTransferInvitation(contacts, filePath);
    }

    @Override
    public void sendGroupFileTransfer(String chatId, String filePath) throws RemoteException {
        Log.d(TAG, "sendGroupChatFileTransfer() enter, filePath = " + filePath);
        // save db;
        Random RANDOM = new Random();
        int messageTag = RANDOM.nextInt(1000) + 1;
        messageTag = Integer.MAX_VALUE - messageTag;
        int dummyId = messageTag;
        long dummIpMsgId = Long.valueOf(dummyId);

        MessageStruct struct = new SendMessageStruct(chatId, mService, filePath, dummIpMsgId);
        long msgIdInSMS = struct.saveMessage();

        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.sendFile(dummIpMsgId, filePath);
    }

//    @Deprecated
//    @Override
//    public void resendGroupFileTransfer(String chatId, long msgId) throws RemoteException {
//        Log.d(TAG, "sendGroupChatFileTransfer() enter, msgId = " + msgId);
//        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
//        groupChat.resendFile(msgId);
//    }
//
//    @Deprecated
//    @Override
//    public void resendFileTransfer(long ipMsgId) throws RemoteException {
//        // Log.d(TAG, "resendFileTransfer() enter, contact = " + contact + "filePath = " +
//        // filePath);
//       //Log.d(TAG, "resendFileTransfer() enter, fid = " + fid + "isBurn = " + isBurn);
//       Log.d(TAG, "resendFileTransfer() enter,ipMsgId = " +ipMsgId);
//       mFTManager.handleResendFileTransfer(ipMsgId);
//    }
//
//    @Deprecated
//    @Override
//    public void resendOne2MultiFileTransfer(long index) throws RemoteException {
//        // TODO Auto-generated method stub
//    }

    @Override
    public void acceptFileTransfer(String fileTransferTag) throws RemoteException {
        Log.d(TAG, "acceptFileTransfer() enter,  fileTransferTag = " + fileTransferTag);
        mFTManager.handleAcceptFileTransfer(fileTransferTag);
    }

    @Override
    public void acceptGroupFileTransfer(String chatId, String fileTransferTag)
            throws RemoteException {
        Log.d(TAG, "acceptGroupFileTransfer() enter,  #chatId = " + chatId + ", #fileTransferTag"
                + fileTransferTag);
        getOrCreateGroupChat(chatId).downloadFile(fileTransferTag);
    }

    @Override
    public void reAcceptFileTransfer(String fileTransferTag)
            throws RemoteException {
        Log.d(TAG, "reacceptFileTransfer() enter,  fileTransferTag = " + fileTransferTag);
        mFTManager.handleReAcceptFileTransfer(fileTransferTag);
    }

    @Override
    public void reAcceptGroupFileTransfer(String chatId, String fileTransferTag)
            throws RemoteException {
        Log.d(TAG, "reacceptGroupFileTransfer() enter,  #chatId = " + chatId + ", #fileTransferTag"
                + fileTransferTag);
        getOrCreateGroupChat(chatId).redownloadFile(fileTransferTag);
    }

    @Override
    public void resumeFileTransfer() throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void pauseFileTransfer(String fileTransferTag)
        throws RemoteException {
        Log.d(TAG, "pauseFileTransfer() enter,  fileTransferTag = " + fileTransferTag);
        mFTManager.handlePauseFileTransfer(fileTransferTag);
    }

    // add by Feng.
    @Override
    public long getRcsFileTransferMaxSize() throws RemoteException {
        FileTransferService fileTransferService = null;
        FileTransferServiceConfiguration fileTransferConfig = null;
        GsmaManager instance = GsmaManager.getInstance();
        long maxSize = 0;

        if (instance != null) {
            try {
                fileTransferService = instance.getFileTransferApi();
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        } else {
            return 0;
        }

        if (fileTransferService != null) {
            try {
                fileTransferConfig = fileTransferService.getConfiguration();
            } catch (JoynServiceException e) {
                Logger.e(TAG,"getfileTransferConfig error ");
            }
        }

        if (fileTransferConfig != null) {
            maxSize = fileTransferConfig.getMaxSize();
        }
        return maxSize;
    }

    @Override
    public void resendRCSMessage(long index) {
        Cursor cursor = RCSDataBaseUtils.getMessage(mService, index);
        if (cursor == null) {
            return;
        }
        try {
            if (!cursor.moveToFirst()) {
                Logger.d(TAG, "resend fail, no record in db");
                return;
            }
            String addr = cursor.getString(cursor.getColumnIndex(MessageColumn.CONTACT_NUMBER));
            int flag = cursor.getInt(cursor.getColumnIndex(MessageColumn.FLAG));
            int type = cursor.getInt(cursor.getColumnIndex(MessageColumn.TYPE));
            long ipMsgId = cursor.getLong(cursor.getColumnIndex(MessageColumn.IPMSG_ID));
            String msgId = cursor.getString(cursor.getColumnIndex(MessageColumn.MESSAGE_ID));
            String body = cursor.getString(cursor.getColumnIndex(MessageColumn.BODY));
            int msgType = cursor.getInt(cursor.getColumnIndex(MessageColumn.CLASS));
            switch (flag) {
                case ThreadFlag.OTO: {
                    if (TextUtils.isEmpty(addr)) {
                        return;
                    }
                    One2OneChat chat = getOne2OneChat(addr);
                    if (type == MessageType.FT) {
                        mFTManager.handleResendFileTransfer(ipMsgId);
                    } else if (type == MessageType.IM) {
                        if (ipMsgId != Integer.MAX_VALUE) {
                            chat.resendChatMessage(msgId);
                        } else {
                            chat.sendChatMessage(index, body, msgType);
                        }
                    }
                    break;
                }
                case ThreadFlag.OTM: {
                    if (TextUtils.isEmpty(addr)) {
                        return;
                    }
                    One2MultiChat chat = getO2MChat(addr);
                    if (type == MessageType.FT) {
                        mFTManager.handleResendFileTransfer(ipMsgId);
                    } else if (type == MessageType.IM) {
                        if (ipMsgId != Integer.MAX_VALUE) {
                            chat.resendChatMessage(msgId);
                        } else {
                            chat.sendChatMessage(index, body, msgType);
                        }
                    }
                    break;
                }
                case ThreadFlag.MTM: {
                    String chatId = cursor.getString(cursor.getColumnIndex(MessageColumn.CHAT_ID));
                    if (TextUtils.isEmpty(chatId)) {
                        return;
                    }
                    SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
                    if (type == MessageType.FT) {
                        groupChat.resendFile(ipMsgId);
                    } else if (type == MessageType.IM) {
                        if (ipMsgId != Integer.MAX_VALUE) {
                            groupChat.resendChatMessage(msgId);
                        } else {
                            groupChat.sendChatMessage(msgType, index, body);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public synchronized void startGroups(List<String> chatIds) throws RemoteException {
        if (mGroupInitComplete) {
            Logger.d(TAG, "startGroups #mGroupInitComplete true");
            return;
        }
        Logger.d(TAG, "startGroups #group count: " + chatIds.size());
        for (String chatId : chatIds) {
            Logger.d(TAG, "startGroups #chatId: " + chatId);
            if (mGroupChatMap.containsKey(chatId)) {
                Logger.d(TAG, "startGroups #chatId: " + chatId + " exist~");
                continue;
            }
            SimpleGroupChat groupChat = new SimpleGroupChat(this, chatId);
            mGroupChatMap.put(chatId, groupChat);
            groupChat.updateGroupStatus();
        }
        mGroupInitComplete = true;
    }

    @Override
    public String initGroupChat(String subject, List<String> contacts) throws RemoteException {
        Logger.d(TAG, "initGroupChat #contacts: " + contacts + ", subject: " + subject);
        List<Participant> participants = new ArrayList<Participant>();
        for (String contact : contacts) {
            participants.add(new Participant(contact, null));
        }
        SimpleGroupChat groupChat = new SimpleGroupChat(this, subject, participants);
        groupChat.startGroup();
        Logger.d(TAG, "initGroupChat #groupChatId: " + groupChat.getChatId());
        if (groupChat.getChatId() == null)
            return null;
        mGroupChatMap.put(groupChat.getChatId(), groupChat);
        return groupChat.getChatId();
    }

    @Override
    public void acceptGroupChat(String chatId) throws RemoteException {
        Logger.d(TAG, "acceptGroupChat #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.invitationAccepted();
    }

    @Override
    public void rejectGroupChat(String chatId) throws RemoteException {
        Logger.d(TAG, "rejectGroupChat #chatId: " + chatId);
        SimpleGroupChat groupChat = getGroupChat(chatId);
        if (groupChat == null) {
            getListener().onRejectInvitationResult(chatId, false);
            return;
        }
        groupChat.invitationRejected();
    }

    @Override
    public void initiateSpamReport(String contact, String msgId, boolean isPagerMode)
            throws RemoteException {
        Logger.d(TAG, " [spam-report] initiateSpamReport contact : " + contact +
                ", msgId= " + msgId + " isPagerMode:"+isPagerMode);
        try {
            Chat o2oChatImpl = null;
            ChatService chatService = GsmaManager.getInstance().getChatApi();
            if (isPagerMode) {
                if (chatService != null) {
                    o2oChatImpl = chatService.getChat(contact);
                    // if null, create one.
                    if (null == o2oChatImpl) {
                        o2oChatImpl = chatService.openSingleChat(contact, listener);
                    }
                }
                if (null != o2oChatImpl) {
                    o2oChatImpl.sendSpamMessageByPagerMode(contact, msgId);
                    Log.d(TAG, "[spam-report]: pagermode mSpamReportListener: " +
                            mSpamReportListener);
                    o2oChatImpl.addSpamReportListener(mPagerModeSpamReportListener);
                    o2oChatImpl.removeEventListener(listener);
                }
            } else {
                if (chatService != null) {
                    chatService.initiateSpamReport(contact, msgId);
                    Log.d(TAG, "[spam-report]:  largemode mSpamReportListener: " +
                            mSpamReportListener);
                    chatService.addSpamReportListener(mSpamReportListener);
                }
            }

        } catch (JoynServiceException e) {
            Log.d(TAG, "[spam-report]:  JoynServiceException: " + e);
            e.printStackTrace();
        }catch (Exception e) {
            Log.d(TAG, "[spam-report]: Exception: " + e);
        }
    }

    private class RcsPagerModeSpamReportListener extends SpamReportListener {
        @Override
        public void onSpamReportSuccess(String contact, String msgId) {
            Log.d(TAG, " [spam-report] RcsSpamReportListener onSpamReportSuccess  contact: "
                    + contact + " msgId: " + msgId);
            //define for report success
            try {
                Chat o2oChatImpl = null;
                ChatService chatService = GsmaManager.getInstance().getChatApi();
                if (chatService != null) {
                    o2oChatImpl = chatService.getChat(contact);
                    // if null, create one.
                    if (null == o2oChatImpl) {
                        o2oChatImpl = chatService.openSingleChat(contact, listener);
                    }
                    if (null != o2oChatImpl) {
                        o2oChatImpl.removeSpamReportListener(mPagerModeSpamReportListener);
                    }
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

            int errorCode = 200;
            getListener().handleSpamReportResult(contact,msgId,errorCode);
        }

        @Override
        public void onSpamReportFailed(String contact, String msgId, int errorCode) {
            Log.d(TAG, " [spam-report] RcsSpamReportListener onSpamReportFailed  contact: "
                    + contact + " msgId: " + msgId + " errorCode: "+errorCode);
            try {
                Chat o2oChatImpl = null;
                ChatService chatService = GsmaManager.getInstance().getChatApi();
                if (chatService != null) {
                    o2oChatImpl = chatService.getChat(contact);
                    // if null, create one.
                    if (null == o2oChatImpl) {
                        o2oChatImpl = chatService.openSingleChat(contact, listener);
                    }
                    if (null != o2oChatImpl) {
                        o2oChatImpl.removeSpamReportListener(mPagerModeSpamReportListener);
                    }
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

            getListener().handleSpamReportResult(contact,msgId,errorCode);
        }
    }

    private class SpamReportChatListener extends ChatListener {

        private final String TAG = "#ChatListener";

        public SpamReportChatListener() {
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

    @Override
    public void initiateFileSpamReport(String contact, String msgId) throws RemoteException {

        Logger.d(TAG, "[spam-report] initiateFileSpamReport contact : " +
                contact + ", msgId= " + msgId);
        try {
            FileTransferService fileTransferService =
                    GsmaManager.getInstance().getFileTransferApi();
            if (fileTransferService != null) {
                fileTransferService.initiateFileSpamReport(contact, msgId);
            }
            if (fileTransferService != null) {
                Log.d(TAG, "[spam-report]:  mFileSpamReportListener: " + mFileSpamReportListener);
                fileTransferService.addFileSpamReportListener(mFileSpamReportListener);
            }
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }
    }

    private class RcsSpamReportListener extends SpamReportListener {
        @Override
        public void onSpamReportSuccess(String contact, String msgId) {
            Log.d(TAG, " [spam-report] RcsSpamReportListener onSpamReportSuccess  contact: "
                    + contact + " msgId: " + msgId);
            //define for report success
            try {
                ChatService chatApi = GsmaManager.getInstance().getChatApi();
                if (chatApi != null) {
                    chatApi.removeSpamReportListener(mSpamReportListener);
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

            int errorCode = 200;
            getListener().handleSpamReportResult(contact,msgId,errorCode);
        }

        @Override
        public void onSpamReportFailed(String contact, String msgId, int errorCode) {
            Log.d(TAG, " [spam-report] RcsSpamReportListener onSpamReportFailed  contact: "
                    + contact + " msgId: " + msgId + " errorCode: "+errorCode);
            try {
                ChatService chatApi = GsmaManager.getInstance().getChatApi();
                if (chatApi != null) {
                    chatApi.removeSpamReportListener(mSpamReportListener);
                }
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }

            getListener().handleSpamReportResult(contact,msgId,errorCode);
        }
    }

    private class RcsFileSpamReportListener extends FileSpamReportListener {
        @Override
        public void onFileSpamReportSuccess(String contact, String ftId) {
            Log.d(TAG, "[spam-report] onFileSpamReportSuccess  contact: "
                    + contact + " ftId: " + ftId);
            //define for report success
            int errorCode = 200;
            try {
                FileTransferService fileTransferService =
                        GsmaManager.getInstance().getFileTransferApi();
                if (fileTransferService != null) {
                    fileTransferService.removeFileSpamReportListener(mFileSpamReportListener);
                }

                getListener().handleFileSpamReportResult(contact,ftId,errorCode);
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFileSpamReportFailed(String contact, String ftId, int errorCode) {
            Log.d(TAG, "[spam-report] onFileSpamReportFailed  contact: "
                    + contact + " ftId: " + ftId + " errorCode: "+errorCode);
            try {
                FileTransferService fileTransferService =
                        GsmaManager.getInstance().getFileTransferApi();
                if (fileTransferService != null) {
                    fileTransferService.removeFileSpamReportListener(mFileSpamReportListener);
                }

                getListener().handleFileSpamReportResult(contact,ftId,errorCode);
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendGroupMessage(String chatId, String content, int type) throws RemoteException {
        // if (!getRegistrationStatus())
        // return false;
        Logger.d(TAG, "sendGroupMessage #chatId: " + chatId + ", #content: " + content);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        MessageStruct struct = new SendMessageStruct(mService, chatId, null, content, type);
        long msgIdInSMS = struct.saveMessage();
        groupChat.sendChatMessage(type, msgIdInSMS, content);
    }

    @Override
    public void addParticipants(String chatId, List<Participant> participants)
            throws RemoteException {
        Logger.d(TAG, "addParticipants #chatId: " + chatId + ", #participants: " + participants);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_ADD_PARTICIPANT, participants).sendToTarget();
    }

    @Override
    public void removeParticipants(String chatId, List<Participant> participants)
            throws RemoteException {
        Logger.d(TAG, "removeParticipants #chatId: " + chatId + ", #participants: " + participants);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_REMOVE_PARTICIPANT, participants).sendToTarget();
    }

    @Override
    public void modifySubject(String chatId, String subject) throws RemoteException {
        Logger.d(TAG, "modifySubject #chatId: " + chatId + ", #subject: " + subject);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_MODIFY_SUBJECT, subject).sendToTarget();
    }

    @Override
    public void modifyNickName(String chatId, String nickName) throws RemoteException {
        Logger.d(TAG, "modifyNickName #chatId: " + chatId + ", #nickName: " + nickName);
        // need change to local nick name, just modify the database.
        RCSDataBaseUtils.modifyGroupNickName(mService, chatId, nickName);
    }

    @Override
    public void modifyRemoteAlias(String chatId, String alias) throws RemoteException {
        Logger.d(TAG, "modifyRemoteAlias #chatId: " + chatId + ", #alias: " + alias);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_MODIFY_MYNICKNAME, alias).sendToTarget();
    }

    @Override
    public void transferChairman(String chatId, String contact) throws RemoteException {
        Logger.d(TAG, "transferChairman #chatId: " + chatId + ", #contact: " + contact);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        Participant participant = new Participant(contact, null);
        groupChat.getGroupConfigHandler()
                .obtainMessage(ISimpleGroupChat.OP_TRANSFER_CHAIRMAN, participant).sendToTarget();
    }

    @Override
    public void quit(String chatId) throws RemoteException {
        Logger.d(TAG, "quit #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler().obtainMessage(ISimpleGroupChat.OP_QUIT_GROUP)
                .sendToTarget();
    }

    @Override
    public void abort(String chatId) throws RemoteException {
        Logger.d(TAG, "abort #chatId: " + chatId);
        SimpleGroupChat groupChat = getOrCreateGroupChat(chatId);
        groupChat.getGroupConfigHandler().obtainMessage(ISimpleGroupChat.OP_ABORT_GROUP)
                .sendToTarget();
    }

    @Override
    public void sendGroupConferenceSubscription(String chatId) throws RemoteException {
        Logger.d(TAG, "sendGroupConferenceSubscription #chatId: " + chatId);
        GsmaManager.getInstance().sendGroupConferenceSubscription(chatId);
    }

    @Override
    public void syncAllGroupChats() {
        Logger.d(TAG, "sync all group chats");
        GsmaManager.getInstance().syncAllGroupChats();
    }

    @Override
    public void addRCSChatServiceListener(IRCSChatServiceListener listener) throws RemoteException {
        mListener.addListener(listener);
        IBinder binder = listener.asBinder();
        binder.linkToDeath(mListener, 0);
    }

    @Override
    public void removeRCSChatServiceListener(IRCSChatServiceListener listener)
            throws RemoteException {
        mListener.removeListener(listener);
    }

    private class RCSChatManagerReceiver extends BroadcastReceiver {
        public final String TAG = this.toString().substring(this.toString().lastIndexOf('.') + 1);

        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Logger.v(TAG, "onReceive() action: " + intent.getAction());
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    asyncOnReceive(context, intent);
                    return null;
                }
            }.execute();
        }

        private void asyncOnReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.v(TAG, "asyncOnReceive() entry, the action is " + action);
            if (ChatIntent.ACTION_NEW_CHAT.equalsIgnoreCase(action)) {
                handleO2OInvitation(intent);
            } else if (GroupChatIntent.ACTION_NEW_INVITATION.equalsIgnoreCase(action)) {
                handleGroupInvitation(intent);
            } else if (ChatIntent.ACTION_DELIVERY_STATUS.equalsIgnoreCase(action)) {
                handleMessageDeliveryStatus(intent);
            } else if (FileTransferIntent.ACTION_NEW_INVITATION.equalsIgnoreCase(action)) {
                // add by feng
                boolean isGroupChat = intent.getBooleanExtra("isGroupTransfer", false);
                Log.v(TAG, "asyncOnReceive() entry, is Group" + isGroupChat);
                if (isGroupChat) {
                    handleGroupFTInvitation(intent);
                } else {
                    mFTManager.handleRecevieFileTransferInvitation(intent);
                }
            } else if (FileTransferIntent.ACTION_DELIVERY_STATUS.equalsIgnoreCase(action)) {
                mFTManager.handleFileTransferDeliveryStatus(intent);
            }
        }
    }

    private class WorkHandler extends Handler {
        static final String TAG = "WorkHandler";

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.d(TAG, "handleMessage():" + msg.what);
            switch (msg.what) {
                case BaseChatImpl.BASE_OP_RESEND_MESSAGE:
                case BaseChatImpl.BASE_OP_SEND_MESSAGE_RST:
                    mPendingMsgManager.handlePendingMessage(msg);
                    break;
            }
        }
    }

    @Override
    public int getGroupChatMaxParticipantsNumber() {
        GsmaManager gsmaManager = GsmaManager.getInstance();
        if (gsmaManager == null) {
            Logger.e(TAG, "[getGroupChatMaxParticipantsNumber], GsmaManager is null");
            return 0;
        }
        ChatServiceConfiguration chatConfig = gsmaManager.getChatServiceConfiguration();
        if (chatConfig == null) {
            Logger.e(TAG, "[getGroupChatMaxParticipantsNumber], chatConfig is null");
            return 0;
        }
        return chatConfig.getGroupChatMaxParticipantsNumber();
    }
}
