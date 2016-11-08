/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.rcse.service.binder;

import android.os.ParcelUuid;
import android.os.RemoteException;

import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.api.Participant;
import com.mediatek.rcse.fragments.GroupChatFragment.ChatEventInformation;
import com.mediatek.rcse.interfaces.ChatView.IChatEventInformation;
import com.mediatek.rcse.interfaces.ChatView.IChatWindow;
import com.mediatek.rcse.interfaces.ChatView.IChatWindowManager;
import com.mediatek.rcse.interfaces.ChatView.IChatWindowMessage;
import com.mediatek.rcse.interfaces.ChatView.IFileTransfer;
import com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow;
import com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow;
import com.mediatek.rcse.interfaces.ChatView.IReceivedChatMessage;
import com.mediatek.rcse.interfaces.ChatView.ISentChatMessage;
import com.mediatek.rcse.interfaces.ChatView.ISentChatMessage.Status;
import com.mediatek.rcse.mvc.ModelImpl.ChatEventStruct;
import com.mediatek.rcse.mvc.ModelImpl.FileStruct;
import com.mediatek.rcse.mvc.ParticipantInfo;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.gsma.joyn.chat.ChatMessage;

/**
 * This class is used for message plugin to manage chat window.
 */
public class ChatWindowManagerAdapter implements IChatWindowManager {
    /**
     * The Constant TAG.
     */
    public static final String TAG = "ChatWindowManagerAdapter";
    /**
     * The m remote chat window manager.
     */
    public IRemoteChatWindowManager mRemoteChatWindowManager = null;
    /**
     * The m remote chat windows.
     */
    private List<BaseChatWindow> mRemoteChatWindows =
            new ArrayList<BaseChatWindow>();

    /**
     * Instantiates a new chat window manager adapter.
     *
     * @param chatWindowManager the chat window manager
     */
    public ChatWindowManagerAdapter(
            IRemoteChatWindowManager chatWindowManager) {
        Logger.d(TAG, "ChatWindowManagerAdapter() entry");
        mRemoteChatWindowManager = chatWindowManager;
    }
    /**
     * Gets the chat window manager.
     *
     * @return the chat window manager
     */
    public IRemoteChatWindowManager getChatWindowManager() {
        Logger.d(TAG, "getChatWindowManager() entry");
        return mRemoteChatWindowManager;
    }
    /* (non-Javadoc)
     * @see com.mediatek.rcse.interfaces.ChatView.IChatWindowManager
     * #addGroupChatWindow(java.lang.Object, java.util.List)
     */
    @Override
    public IGroupChatWindow addGroupChatWindow(Object tag,
            List<ParticipantInfo> participantList) {
        Logger.d(TAG, "addGroupChatWindow() entry! tag = " + tag
                + " participantList = " + participantList);
        try {
            IRemoteGroupChatWindow groupChatWindow = mRemoteChatWindowManager
                    .addGroupChatWindow(tag.toString(),
                            participantList);
            Logger.d(TAG, "addGroupChatWindow() : groupChatWindow = "
                    + groupChatWindow);
            if (null != groupChatWindow) {
                GroupChatWindowAdapter remoteGroupChatWindow = new GroupChatWindowAdapter(
                        groupChatWindow);
                TagTranslater.saveTag(tag);
                mRemoteChatWindows.add(remoteGroupChatWindow);
                return remoteGroupChatWindow;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
    /* (non-Javadoc)
     * @see com.mediatek.rcse.interfaces.ChatView.IChatWindowManager
     * #addOne2OneChatWindow(java.lang.Object, com.mediatek.rcse.api.Participant)
     */
    @Override
    public IOne2OneChatWindow addOne2OneChatWindow(Object tag,
            Participant participant) {
        Logger.d(TAG, "addOne2OneChatWindow() entry! tag = " + tag
                + " participant" + participant);
        try {
            Logger.d(TAG,
                    "addOne2OneChatWindow(), mRemoteChatWindowManager = "
                            + mRemoteChatWindowManager);
            IRemoteOne2OneChatWindow one2OneChatWindow = mRemoteChatWindowManager
                    .addOne2OneChatWindow(tag.toString(), participant);
            One2OneChatWindowAdapter one2OneChatWindowAdapter = new One2OneChatWindowAdapter(
                    one2OneChatWindow);
            TagTranslater.saveTag(tag);
            mRemoteChatWindows.add(one2OneChatWindowAdapter);
            return one2OneChatWindowAdapter;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
    /* (non-Javadoc)
     * @see com.mediatek.rcse.interfaces.ChatView.IChatWindowManager#
     * removeChatWindow(com.mediatek.rcse.interfaces.ChatView.IChatWindow)
     */
    @Override
    public boolean removeChatWindow(IChatWindow chatWindow) {
        Logger.d(TAG, "removeChatWindow() entry! chatWindow = "
                + chatWindow);
        if (mRemoteChatWindows.contains(chatWindow)) {
            Object remoteChatWindow = ((BaseChatWindow) chatWindow)
                    .getChatWindow();
            if (remoteChatWindow instanceof IRemoteGroupChatWindow) {
                try {
                    return mRemoteChatWindowManager
                            .removeGroupChatWindow((IRemoteGroupChatWindow) remoteChatWindow);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    return mRemoteChatWindowManager
                            .removeOne2OneChatWindow((IRemoteOne2OneChatWindow) remoteChatWindow);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    /* (non-Javadoc)
     * @see com.mediatek.rcse.interfaces.ChatView.IChatWindowManager
     * #switchChatWindowByTag(android.os.ParcelUuid)
     */
    @Override
    public void switchChatWindowByTag(ParcelUuid uuidTag) {
        Logger.d(TAG, "switchChatWindowByTag() entry! uuidTag = "
                + uuidTag);
        try {
            mRemoteChatWindowManager.switchChatWindowByTag(uuidTag
                    .toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * The Class BaseChatWindow.
     */
    public abstract class BaseChatWindow {
        /**
         * Gets the chat window.
         *
         * @return the chat window
         */
        public abstract Object getChatWindow();
    }

    /**
     * The Class GroupChatWindowAdapter.
     */
    public class GroupChatWindowAdapter extends BaseChatWindow
            implements IGroupChatWindow {
        /**
         * The m remote group chat window.
         */
        IRemoteGroupChatWindow mRemoteGroupChatWindow = null;

        /**
         * Instantiates a new group chat window adapter.
         *
         * @param groupChatWindow the group chat window
         */
        GroupChatWindowAdapter(IRemoteGroupChatWindow groupChatWindow) {
            Logger.d(TAG, "GroupChatWindowAdapter() entry!");
            mRemoteGroupChatWindow = groupChatWindow;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * addReceivedFileTransfer(com.mediatek.rcse.mvc.ModelImpl.FileStruct, boolean)
         */
        @Override
        public IFileTransfer addReceivedFileTransfer(FileStruct file,
                boolean isAutoAccept, boolean isRead) {
            Logger.d(TAG, "addReceivedFileTransfer() entry! file = "
                    + file);
            FileStructForBinder fileStructForBinder = createFileStructForBinder(file);
            IRemoteFileTransfer fileTransfer = null;
            try {
                fileTransfer = mRemoteGroupChatWindow
                        .addReceivedFileTransfer(fileStructForBinder,
                                isAutoAccept, isRead);
                fileTransfer.setStatus(1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new FileTransferAdapter(fileTransfer);
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * addSentFileTransfer(com.mediatek.rcse.mvc.ModelImpl.FileStruct)
         */
        @Override
        public IFileTransfer addSentFileTransfer(FileStruct file) {
            Logger.d(TAG, "addSentFileTransfer() entry! file = "
                    + file);
            FileStructForBinder fileStructForBinder = createFileStructForBinder(file);
            IRemoteFileTransfer fileTransfer = null;
            try {
                fileTransfer = mRemoteGroupChatWindow
                        .addSentFileTransfer(fileStructForBinder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new FileTransferAdapter(fileTransfer);
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * setFileTransferEnable(int)
         */
        @Override
        public void setFileTransferEnable(int reason) {
            Logger.d(TAG, "setFileTransferEnable() entry! reason = "
                    + reason);
            try {
                mRemoteGroupChatWindow.setFileTransferEnable(reason);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.service.binder.ChatWindowManagerAdapter.
         * BaseChatWindow#getChatWindow()
         */
        @Override
        public Object getChatWindow() {
            Logger.d(TAG, "getChatWindow() entry!");
            return mRemoteGroupChatWindow;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * addChatEventInformation(com.mediatek.rcse.mvc.ModelImpl.ChatEventStruct)
         */
        @Override
        public IChatEventInformation addChatEventInformation(
                ChatEventStruct chatEventStruct) {
            Logger.d(TAG,
                    "addChatEventInformation() entry! chatEventStruct = "
                            + chatEventStruct);
            ChatEventStructForBinder chatEventStructForBinder;
            chatEventStructForBinder = createChatEventStructForBinder(chatEventStruct);
            try {
                Logger.d(TAG,
                        "addChatEventInformation() : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    mRemoteGroupChatWindow
                            .addChatEventInformation(chatEventStructForBinder);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            ChatEventInformation chatEventInformation = new ChatEventInformation(
                    chatEventStruct);
            return chatEventInformation;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * setIsComposing(boolean, com.mediatek.rcse.api.Participant)
         */
        @Override
        public void setIsComposing(boolean isComposing,
                Participant participant) {
            Logger.d(TAG, "setIsComposing() entry! isComposing = "
                    + isComposing + " participant = " + participant);
            try {
                Logger.d(TAG,
                        "setIsComposing() : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    mRemoteGroupChatWindow.setIsComposing(
                            isComposing, participant);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#updateChatStatus(int)
         */
        @Override
        public void updateChatStatus(int status) {
            Logger.d(TAG, "updateChatStatus() entry! status: "
                    + status);
            try {
                Logger.d(TAG,
                        "updateChatStatus() mRemoteGroupChatWindow: "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    mRemoteGroupChatWindow.updateChatStatus(status);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * updateParticipants(java.util.List)
         */
        @Override
        public void updateParticipants(
                List<ParticipantInfo> participants) {
            Logger.d(TAG,
                    "updateParticipants() entry! participants = "
                            + participants);
            try {
                Logger.d(TAG,
                        "updateParticipants() : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    mRemoteGroupChatWindow
                            .updateParticipants(participants);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#addLoadHistoryHeader(boolean)
         */
        @Override
        public void addLoadHistoryHeader(boolean showLoader) {
            Logger.d(TAG,
                    "addLoadHistoryHeader() entry! showLoader = "
                            + showLoader);
            try {
                mRemoteGroupChatWindow
                        .addLoadHistoryHeader(showLoader);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * addReceivedMessage(org.gsma.joyn.chat.ChatMessage, boolean)
         */
        @Override
        public IReceivedChatMessage addReceivedMessage(
                ChatMessage message, boolean isRead) {
            Logger.d(TAG, "addReceivedMessage() entry! message = "
                    + message + " isRead" + isRead);
            IRemoteReceivedChatMessage receivedChatMessage = null;
            try {
                Logger.d(TAG,
                        "updateParticipants() : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    receivedChatMessage = mRemoteGroupChatWindow
                            .addReceivedMessage(message, isRead);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new ReceivedChatMessageAdapter(receivedChatMessage);
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * addSentMessage(org.gsma.joyn.chat.ChatMessage, int)
         */
        @Override
        public ISentChatMessage addSentMessage(ChatMessage message,
                int messageTag) {
            Logger.d(TAG, "addSentMessage() entry! message = "
                    + message + " messageTag = " + messageTag);
            IRemoteSentChatMessage sentChatMessage = null;
            try {
                Logger.d(TAG,
                        "updateParticipants() : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    sentChatMessage = mRemoteGroupChatWindow
                            .addSentMessage(message, messageTag);
                    sentChatMessage.updateStatus(Status.SENDING
                            .toString());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new SentChatMessageAdapter(sentChatMessage);
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * getSentChatMessage(java.lang.String)
         */
        @Override
        public IChatWindowMessage getSentChatMessage(String messageId) {
            Logger.d(TAG, "getSentChatMessage() entry! messageId = "
                    + messageId);
            return null;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#removeAllMessages()
         */
        @Override
        public void removeAllMessages() {
            Logger.d(TAG, "removeAllMessages() entry!");
            try {
                mRemoteGroupChatWindow.removeAllMessages();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#updateAllMsgAsRead()
         */
        @Override
        public void updateAllMsgAsRead() {
            Logger.d(TAG, "updateAllMsgAsRead() entry!");
            try {
                Logger.d(TAG,
                        "updateParticipants() : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    mRemoteGroupChatWindow.updateAllMsgAsRead();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * removeChatMessage(java.lang.String)
         */
        @Override
        public void removeChatMessage(String messageId) {
            // TODO Auto-generated method stub
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * addgroupSubject(java.lang.String)
         */
        @Override
        public void addgroupSubject(String subject) {
            try {
                Logger.d(TAG,
                        "addgroupSubject : mRemoteGroupChatWindow = "
                                + mRemoteGroupChatWindow);
                if (mRemoteGroupChatWindow != null) {
                    mRemoteGroupChatWindow.addgroupSubject(subject);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * updateAllMsgAsReadForContact(com.mediatek.rcse.api.Participant)
         */
        @Override
        public void updateAllMsgAsReadForContact(
                Participant participant) {
            // TODO Auto-generated method stub
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#
         * setmChatID(java.lang.String)
         */
        @Override
        public void setmChatID(String mChatID) {
            // TODO Auto-generated method stub
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#clearExtraMessage()
         */
        @Override
        public void clearExtraMessage() {
            // TODO Auto-generated method stub
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IGroupChatWindow#loadMessageIdList()
         */
        @Override
        public void loadMessageIdList() {
            // TODO Auto-generated method stub
        }
    }

    /**
     * The Class One2OneChatWindowAdapter.
     */
    public class One2OneChatWindowAdapter extends BaseChatWindow
            implements IOne2OneChatWindow {
        /**
         * The m remote one2 one chat window.
         */
        IRemoteOne2OneChatWindow mRemoteOne2OneChatWindow = null;

        /**
         * Instantiates a new one2 one chat window adapter.
         *
         * @param one2OneChatWindow the one2 one chat window
         */
        public One2OneChatWindowAdapter(
                IRemoteOne2OneChatWindow one2OneChatWindow) {
            Logger.d(TAG,
                    "One2OneChatWindowAdapter() entry! one2OneChatWindow = "
                            + one2OneChatWindow);
            mRemoteOne2OneChatWindow = one2OneChatWindow;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.service.binder.ChatWindowManagerAdapter
         * .BaseChatWindow#getChatWindow()
         */
        @Override
        public Object getChatWindow() {
            Logger.d(TAG, "getChatWindow() entry!");
            return mRemoteOne2OneChatWindow;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow#
         * addReceivedFileTransfer(com.mediatek.rcse.mvc.ModelImpl.FileStruct, boolean)
         */
        @Override
        public IFileTransfer addReceivedFileTransfer(FileStruct file,
                boolean isAutoAccept) {
            Logger.d(TAG, "addReceivedFileTransfer() entry! file = "
                    + file);
            FileStructForBinder fileStructForBinder = createFileStructForBinder(file);
            IRemoteFileTransfer fileTransfer = null;
            try {
                fileTransfer = mRemoteOne2OneChatWindow
                        .addReceivedFileTransfer(fileStructForBinder,
                                isAutoAccept);
                if (!isAutoAccept) {
                    fileTransfer.setStatus(1);
                } else {
                    fileTransfer.setStatus(2);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new FileTransferAdapter(fileTransfer);
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow#
         * addSentFileTransfer(com.mediatek.rcse.mvc.ModelImpl.FileStruct)
         */
        @Override
        public IFileTransfer addSentFileTransfer(FileStruct file) {
            Logger.d(TAG, "addSentFileTransfer() entry! file = "
                    + file);
            FileStructForBinder fileStructForBinder = createFileStructForBinder(file);
            IRemoteFileTransfer fileTransfer = null;
            try {
                fileTransfer = mRemoteOne2OneChatWindow
                        .addSentFileTransfer(fileStructForBinder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new FileTransferAdapter(fileTransfer);
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow
         * #setFileTransferEnable(int)
         */
        @Override
        public void setFileTransferEnable(int reason) {
            Logger.d(TAG, "setFileTransferEnable() entry! reason = "
                    + reason);
            try {
                mRemoteOne2OneChatWindow
                        .setFileTransferEnable(reason);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow#
         * setIsComposing(boolean)
         */
        @Override
        public void setIsComposing(boolean isComposing) {
            Logger.d(TAG, "setIsComposing() entry! isComposing = "
                    + isComposing);
            try {
                mRemoteOne2OneChatWindow.setIsComposing(isComposing);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IOne2OneChatWindow#
         * setRemoteOfflineReminder(boolean)
         */
        @Override
        public void setRemoteOfflineReminder(boolean isOffline) {
            Logger.d(TAG,
                    "setRemoteOfflineReminder() entry! isOffline = "
                            + isOffline);
            try {
                mRemoteOne2OneChatWindow
                        .setRemoteOfflineReminder(isOffline);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * addLoadHistoryHeader(boolean)
         */
        @Override
        public void addLoadHistoryHeader(boolean showLoader) {
            Logger.d(TAG,
                    "addLoadHistoryHeader() entry! + showLoader ="
                            + showLoader);
            try {
                mRemoteOne2OneChatWindow
                        .addLoadHistoryHeader(showLoader);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * addReceivedMessage(org.gsma.joyn.chat.ChatMessage, boolean)
         */
        @Override
        public IReceivedChatMessage addReceivedMessage(
                ChatMessage message, boolean isRead) {
            Logger.d(TAG, "addReceivedMessage() entry! message = "
                    + message + " isRead = " + isRead);
            try {
                IRemoteReceivedChatMessage receivedChatMessage = mRemoteOne2OneChatWindow
                        .addReceivedMessage(message, isRead);
                return new ReceivedChatMessageAdapter(
                        receivedChatMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * addSentMessage(org.gsma.joyn.chat.ChatMessage, int)
         */
        @Override
        public ISentChatMessage addSentMessage(ChatMessage message,
                int messageTag) {
            Logger.d(TAG, "addSentMessage() entry! message = "
                    + message + " messageTag = " + messageTag);
            try {
                IRemoteSentChatMessage sentChatMessage = mRemoteOne2OneChatWindow
                        .addSentMessage(message, messageTag);
                return new SentChatMessageAdapter(sentChatMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * getSentChatMessage(java.lang.String)
         */
        @Override
        public IChatWindowMessage getSentChatMessage(String messageId) {
            Logger.d(TAG,
                    "getSentChatMessage() entry! + messageId = "
                            + messageId);
            return null;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#removeAllMessages()
         */
        @Override
        public void removeAllMessages() {
            Logger.d(TAG, "removeAllMessages() entry!");
            try {
                mRemoteOne2OneChatWindow.removeAllMessages();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#updateAllMsgAsRead()
         */
        @Override
        public void updateAllMsgAsRead() {
            Logger.d(TAG, "updateAllMsgAsRead() entry!");
            try {
                mRemoteOne2OneChatWindow.updateAllMsgAsRead();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * removeChatMessage(java.lang.String)
         */
        @Override
        public void removeChatMessage(String messageId) {
            // TODO Auto-generated method stub
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindow#
         * updateAllMsgAsReadForContact(com.mediatek.rcse.api.Participant)
         */
        @Override
        public void updateAllMsgAsReadForContact(
                Participant participant) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * The Class ReceivedChatMessageAdapter.
     */
    public static class ReceivedChatMessageAdapter implements
            IReceivedChatMessage {
        /**
         * The m received chat message.
         */
        IRemoteReceivedChatMessage mReceivedChatMessage = null;

        /**
         * Instantiates a new received chat message adapter.
         *
         * @param receivedChatMessage the received chat message
         */
        public ReceivedChatMessageAdapter(
                IRemoteReceivedChatMessage receivedChatMessage) {
            Logger.d(TAG,
                    "ReceivedChatMessageAdapter() entry! receivedChatMessage = "
                            + receivedChatMessage);
            mReceivedChatMessage = receivedChatMessage;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindowMessage#getId()
         */
        @Override
        public String getId() {
            Logger.d(TAG, "getId() entry!");
            try {
                return mReceivedChatMessage.getId();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * The Class SentChatMessageAdapter.
     */
    public static class SentChatMessageAdapter implements
            ISentChatMessage {
        /**
         * The m sent chat message.
         */
        IRemoteSentChatMessage mSentChatMessage;

        /**
         * Instantiates a new sent chat message adapter.
         *
         * @param sentChatMessage the sent chat message
         */
        public SentChatMessageAdapter(
                IRemoteSentChatMessage sentChatMessage) {
            Logger.d(TAG,
                    "SentChatMessageAdapter() entry! sentChatMessage = "
                            + sentChatMessage);
            mSentChatMessage = sentChatMessage;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.
         * ISentChatMessage#updateDate(java.util.Date)
         */
        @Override
        public void updateDate(Date date) {
            Logger.d(TAG, "updateDate() entry! date = " + date
                    + ", mSentChatMessage = " + mSentChatMessage);
            try {
                if (mSentChatMessage != null) {
                    mSentChatMessage.updateDate(date.getTime());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.ISentChatMessage#
         * updateStatus(com.mediatek.rcse.interfaces.ChatView.ISentChatMessage.Status)
         */
        @Override
        public void updateStatus(Status status) {
            Logger.d(TAG, "updateStatus() entry! status = " + status
                    + ",mSentChatMessage = " + mSentChatMessage);
            try {
                if (mSentChatMessage != null) {
                    mSentChatMessage.updateStatus(status.name());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IChatWindowMessage#getId()
         */
        @Override
        public String getId() {
            Logger.d(TAG, "getId() entry!");
            try {
                return mSentChatMessage.getId();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.ISentChatMessage#
         * updateStatus(com.mediatek.rcse.interfaces.ChatView.
         * ISentChatMessage.Status, java.lang.String)
         */
        @Override
        public void updateStatus(Status status, String contact) {
            Logger.d(TAG, "updateStatus() entry! status = " + status
                    + ",mSentChatMessage = " + mSentChatMessage
                    + ",contact = " + contact);
            try {
                if (mSentChatMessage != null) {
                    mSentChatMessage.updateStatus(status.name());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The Class FileTransferAdapter.
     */
    public static class FileTransferAdapter implements IFileTransfer {
        /**
         * The m file transfer.
         */
        IRemoteFileTransfer mFileTransfer;

        /**
         * Instantiates a new file transfer adapter.
         *
         * @param fileTransfer the file transfer
         */
        public FileTransferAdapter(IRemoteFileTransfer fileTransfer) {
            Logger.d(TAG,
                    "FileTransferAdapter() entry! fileTransfer = "
                            + fileTransfer);
            mFileTransfer = fileTransfer;
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IFileTransfer#setFilePath(java.lang.String)
         */
        @Override
        public void setFilePath(String filePath) {
            Logger.d(TAG, "setFilePath() entry! filePath = "
                    + filePath + ",mFileTransfer = " + mFileTransfer);
            try {
                if (mFileTransfer != null) {
                    mFileTransfer.setFilePath(filePath);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IFileTransfer#setProgress(long)
         */
        @Override
        public void setProgress(long progress) {
            Logger.d(TAG, "setProgress() entry! progress = "
                    + progress + ", mFileTransfer = " + mFileTransfer);
            try {
                if (mFileTransfer != null) {
                    mFileTransfer.setProgress(progress);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IFileTransfer#setStatus
         * (com.mediatek.rcse.interfaces.ChatView.IFileTransfer.Status)
         */
        @Override
        public void setStatus(Status status) {
            Logger.d(TAG, "setStatus() entry! status = " + status
                    + ", mFileTransfer = " + mFileTransfer);
            try {
                if (mFileTransfer != null) {
                    mFileTransfer.setStatus(status.ordinal());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* (non-Javadoc)
         * @see com.mediatek.rcse.interfaces.ChatView.IFileTransfer#updateTag
         * (java.lang.String, long)
         */
        @Override
        public void updateTag(String transferTag, long transferSize) {
            Logger.d(TAG, "updateTag() entry! transferTag = "
                    + transferTag + ", mFileTransfer = "
                    + mFileTransfer);
            try {
                if (mFileTransfer != null) {
                    mFileTransfer
                            .updateTag(transferTag, transferSize);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates the chat event struct for binder.
     *
     * @param chatEventStruct the chat event struct
     * @return the chat event struct for binder
     */
    private ChatEventStructForBinder createChatEventStructForBinder(
            ChatEventStruct chatEventStruct) {
        Logger.d(TAG,
                "createChatEventStructForBinder(), chatEventStruct = "
                        + chatEventStruct);
        int information = chatEventStruct.information.ordinal();
        List<String> relatedInfo = new ArrayList<String>();
        String date = null;
        Logger.d(
                TAG,
                "createChatEventStructForBinder(): chatEventStruct.relatedInformation = "
                        + chatEventStruct.relatedInformation
                        + ", chatEventStruct.date = "
                        + chatEventStruct.date);
        if (chatEventStruct.relatedInformation != null) {
            relatedInfo.add(chatEventStruct.relatedInformation
                    .toString());
        }
        if (chatEventStruct.date != null) {
            date = chatEventStruct.date.toString();
        }
        ChatEventStructForBinder chatEventStructForBinder =
                new ChatEventStructForBinder(
                information, relatedInfo, date);
        return chatEventStructForBinder;
    }
    /**
     * Creates the file struct for binder.
     *
     * @param fileStruct the file struct
     * @return the file struct for binder
     */
    private FileStructForBinder createFileStructForBinder(
            FileStruct fileStruct) {
        Logger.d(TAG,
                "createFileStructForBinder() entry! fileStruct = "
                        + fileStruct);
        FileStructForBinder fileStructForBinder = new FileStructForBinder(
                fileStruct);
        return fileStructForBinder;
    }
}
