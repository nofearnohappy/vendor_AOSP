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

package com.mediatek.rcs.messageservice.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.sql.Blob;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import android.util.Log;
import android.widget.Toast;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.content.Intent;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaFile;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferIntent;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;
import org.gsma.joyn.ft.NewFileTransferListener;

import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.utils.RCSUtils;
import org.gsma.joyn.ft.MultiFileTransferLog;

import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer;
import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.RcsLog.Class;

import java.util.Random;

import com.mediatek.rcs.common.provider.RCSDataBaseUtils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import com.mediatek.rcs.common.utils.ContextCacher;

public class FileTransferManager {
    public static final String TAG = "RCS/FileTransferManager";

    private SentFileTransferManager mOutGoingFileTransferManager = new SentFileTransferManager();
    FileTransferService mFileTransferService = null;
    private static FileTransferManager INSTANCE = null;
    protected ReceiveFileTransferManager mReceiveFileTransferManager
                                                = new ReceiveFileTransferManager();

    public static final int FILETRANSFER_ENABLE_OK = 0;
    public static final int FILETRANSFER_DISABLE_REASON_NOT_REGISTER = 1;
    public static final int FILETRANSFER_DISABLE_REASON_CAPABILITY_FAILED = 2;
    public static final int FILETRANSFER_DISABLE_REASON_REMOTE = 3;

    private static final String AUTO_ACCEPT = "autoAccept";
    private static final String CHAT_SESSION_ID = "chatSessionId";
    private static final String ISGROUPTRANSFER = "isGroupTransfer";
    private static final String CHAT_ID = "chatId";

    private static final Random RANDOM = new Random();

    // The blank space text
    private static final String BLANK_SPACE = " ";
    // The empty text
    private static final String EMPTY_STRING = "";
    // The seprator text
    private static final String SEPRATOR = ";";

    protected static RCSChatServiceBinder mService = null;

    protected static FTNotificationsManager mNotificationsManager = null;

    private FileTransferManager(RCSChatServiceBinder service) {
        mService = service;
        mNotificationsManager = FTNotificationsManager.getInstance();
    }

    public static FileTransferManager getInstance(RCSChatServiceBinder service) {
        Log.v(TAG, "getInstance() FileTransferManager");
        if (INSTANCE == null) {
            INSTANCE = new FileTransferManager(service);
        }
        return INSTANCE;
    }

    public SentFileTransferManager getSentFileTransferManager() {
        return mOutGoingFileTransferManager;
    }

    public ReceiveFileTransferManager getReceiveFileTransferManager() {
        return mReceiveFileTransferManager;
    }

    public interface INotificationsListener {
        void notificationsReceived(Intent intent);
    }

    /**
     * This class is used to manage the whole sent file transfers and make it
     * work in queue
     */
    private static class SentFileTransferManager implements SentFileTransfer.IOnSendFinishListener,
            SentFileTransfer.IOnResendListener {
        private static final String TAG = "SentFileTransferManager";

        public static final int TO_SEND = 0;
        public static final int TO_RESUME = 1;
        public static final int TO_SEND_AGAIN = 2;

        public static final int MANUAL_RESEND = 0;
        public static final int AUTO_RESEND = 1;

        private static final int MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM = 1;

        /* Pending list for save pending SentFileTransfer */
        private ConcurrentLinkedQueue<SentFileTransfer> mPendingList
                        = new ConcurrentLinkedQueue<SentFileTransfer>();
        /* Active list for save active SentFileTransfer */
        private CopyOnWriteArrayList<SentFileTransfer> mActiveList
                        = new CopyOnWriteArrayList<SentFileTransfer>();
        /* Active list for save resend SentFileTransfer */
        private CopyOnWriteArrayList<SentFileTransfer> mResendableList
                        = new CopyOnWriteArrayList<SentFileTransfer>();

        private synchronized void checkNext() {
            int activatedNum = mActiveList.size();
            if (activatedNum < MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM) {
                Log.w(TAG, "checkNext() current activatedNum is " + activatedNum
                        + " will find next file transfer to send");
                final SentFileTransfer nextFileTransfer = mPendingList.poll();
                if (null != nextFileTransfer) {
                    Log.w(TAG, "checkNext() next file transfer found, just send it!");

                    if (nextFileTransfer.mSentFileTransferType == TO_SEND
                            || nextFileTransfer.mSentFileTransferType == TO_SEND_AGAIN) {
                        Log.d(TAG, "checkNext(), send run");
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                nextFileTransfer.send();
                            }
                        });
                    } else {
                        Log.d(TAG, "checkNext(), resend run");
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                nextFileTransfer.resend();
                            }
                        });
                    }
                    if (nextFileTransfer.mResendType != SentFileTransferManager.AUTO_RESEND
                            || nextFileTransfer.mChatType != nextFileTransfer.GROUP) {
                        mActiveList.add(nextFileTransfer);
                    }
                } else {
                    Log.w(TAG, "checkNext() next file transfer not found, pending list is null");
                }
            } else {
                Log.w(TAG, "checkNext() current activatedNum is " + activatedNum
                        + " MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM is "
                        + MAX_ACTIVATED_SENT_FILE_TRANSFER_NUM
                        + " so no need to find next pending file transfer");
            }
        }

        /**
         * onAddSentFileTransfer, add sentFileTransfer into mPendingList and
         * checknext
         *
         */
        public void onAddSentFileTransfer(SentFileTransfer sentFileTransfer) {
            Log.w(TAG, "onAddSentFileTransfer() entry, sentFileTransfer =  " + sentFileTransfer);
            if (null != sentFileTransfer) {
                Log.w(TAG, "onAddSentFileTransfer() entry, file " + sentFileTransfer.mFileStruct
                        + " is going to be sent");
                sentFileTransfer.mOnSendFinishListener = this;
                sentFileTransfer.mOnResendListener = this;
                mPendingList.add(sentFileTransfer);
                checkNext();
            }
        }

        public void resendFileTransfer(SentFileTransfer sentFileTransfer, boolean haveInstance) {
            Log.d(TAG, "resendFileTransfer enter");
            if (isResendingInstance(sentFileTransfer)) {
                Log.d(TAG, "resendFileTransfer, it is a exist resending instance !");
                return;
            }
            if (haveInstance && sentFileTransfer != null) {
                if (sentFileTransfer.mResendType != SentFileTransferManager.AUTO_RESEND
                        || sentFileTransfer.mChatType != sentFileTransfer.GROUP) {
                    mResendableList.remove(sentFileTransfer);
                }
            }
            if (sentFileTransfer != null) {
                sentFileTransfer.mOnSendFinishListener = this;
                sentFileTransfer.mOnResendListener = this;
                int size = mPendingList.size();
                Log.d(TAG, "resendFileTransfer, mPendingList.size() = " + mPendingList.size());
                ConcurrentLinkedQueue<SentFileTransfer> tmpList =
                                    new ConcurrentLinkedQueue<SentFileTransfer>();
                tmpList.add(sentFileTransfer);
                for (int i = 0; i < size; i++) {
                    tmpList.add(mPendingList.poll());
                }
                mPendingList = tmpList;
                Log.d(TAG,
                        "resendFileTransfer, mPendingList.size() after add = "
                                + mPendingList.size());
                sentFileTransfer.FTChatWindow.updateFTStatus(Status.TRANSFERING);
                checkNext();
            }
        }

        /* delete a sent file transfer */
        public void cancelFileTransfer(String targetFileTransferTag) {
            Log.d(TAG, "cancelFileTransfer() begin to cancel sent file transfer with tag "
                    + targetFileTransferTag);
            SentFileTransfer fileTransfer = findPendingFileTransfer(targetFileTransferTag);
            if (null != fileTransfer) {
                Log.d(TAG, "cancelFileTransfer() the target file transfer with tag "
                        + targetFileTransferTag + " found in pending list");
                // fileTransfer.onCancel();
                fileTransfer.onDestroy();
                mPendingList.remove(fileTransfer);
            } else {
                fileTransfer = findActiveFileTransfer(targetFileTransferTag);

                if (null != fileTransfer) {
                    Log.w(TAG, "cancelFileTransfer() the target file transfer with tag "
                            + targetFileTransferTag + " found in active list");
                    fileTransfer.onDestroy();
                    onSendFinish(fileTransfer, Result.REMOVABLE);
                    return;
                }
            }
            fileTransfer = findResendableFileTransfer(targetFileTransferTag);
            if (null != fileTransfer) {
                Log.w(TAG, "cancelFileTransfer() the target file transfer with tag "
                        + targetFileTransferTag + " found in active list");
                fileTransfer.onDestroy();
                mResendableList.remove(fileTransfer);
            }
            return;
        }

        @Override
        public void onSendFinish(final SentFileTransfer sentFileTransfer, final Result result) {
            Log.w(TAG, "onSendFinish(): sentFileTransfer = " + sentFileTransfer + ", result = "
                    + result);
            if (mActiveList.contains(sentFileTransfer)) {
                // sentFileTransfer.cancelNotification();
                Log.w(TAG, "onSendFinish() file transfer " + sentFileTransfer.mFileStruct
                        + " with " + result + " remove it from activated list");
                switch (result) {
                case RESENDABLE:
                    mResendableList.add(sentFileTransfer);
                    mActiveList.remove(sentFileTransfer);
                    Log.v(TAG, "resend test:  mResendableList.add and  mActiveList.remove");
                    Log.v(TAG, "onSendFinish, mActiveList.size = " + mActiveList.size());
                    break;
                case REMOVABLE:
                    mActiveList.remove(sentFileTransfer);
                    Log.v(TAG, "resend test:  mActiveList.remove");
                    Log.v(TAG, "onSendFinish, mActiveList.size = " + mActiveList.size());
                    break;
                default:
                    break;
                }
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        checkNext();
                    }
                });
            }
        }

        @Override
        public void onReSendFileTransfer(SentFileTransfer sentFileTransfer) {
            Log.d(TAG, "onReSendFileTransfer() enter");
            boolean haveInstance = true;
            sentFileTransfer.mResendType = SentFileTransferManager.AUTO_RESEND;
            resendFileTransfer(sentFileTransfer, haveInstance);
        }

        private boolean isResendingInstance(SentFileTransfer sentFileTransfer) {
            Log.d(TAG, "isResendingInstance() enter");
            if (mPendingList.contains(sentFileTransfer) || mActiveList.contains(sentFileTransfer)) {
                Log.d(TAG, "isResendingInstance() return true");
                return true;
            }
            Log.d(TAG, "isResendingInstance() return false");
            return false;
        }

        private SentFileTransfer findActiveFileTransfer(String targetTag) {
            Log.w(TAG, "findActiveFileTransfer entry, targetTag is " + targetTag);
            return findFileTransferByTag(mActiveList, targetTag);
        }

        private SentFileTransfer findPendingFileTransfer(String targetTag) {
            Log.w(TAG, "findPendingFileTransfer entry, targetTag is " + targetTag);
            return findFileTransferByTag(mPendingList, targetTag);
        }

        private SentFileTransfer findResendableFileTransfer(String targetTag) {
            Log.d(TAG, "findResendableFileTransfer entry, targetTag is " + targetTag);
            return findFileTransferByTag(mResendableList, targetTag);
        }

        private SentFileTransfer findFileTransferByTag(Collection<SentFileTransfer> whereToFind,
                String targetTag) {
            if (null != whereToFind && null != targetTag) {
                for (SentFileTransfer sentFileTransfer : whereToFind) {
                    String fileTransferTag = sentFileTransfer.mFileTransferTag;
                    if (targetTag.equals(fileTransferTag)) {
                        Log.w(TAG, "findFileTransferByTag() the file transfer with targetTag "
                                + targetTag + " found");
                        return sentFileTransfer;
                    }
                }
                Log.w(TAG, "findFileTransferByTag() not found targetTag " + targetTag);
                return null;
            } else {
                Log.e(TAG, "findFileTransferByTag() whereToFind is " + whereToFind
                        + " targetTag is " + targetTag);
                return null;
            }
        }
    }

    /**
     * This class describe one single out-going file transfer, and control the
     * status itself
     */
    public static class SentFileTransfer {

        private static final String TAG = "SentFileTransfer";

        public static final int ONE2ONE = 0;
        public static final int ONE2MULTI = 1;
        public static final int GROUP = 2;

        protected int mDuration = 0;
        protected String mMimeType = null;
        boolean isSendToMulti = false;

        protected String mFileTransferTag = null;
        protected FileStruct mFileStruct = null;

        protected FileTransfer mFileTransferObject = null;
        protected FileTransferListener mFileTransferListener = null;
        protected String mChatSessionId;
        protected boolean mBurnType = false;

        protected int mChatType = ONE2ONE;
        protected String mContact = null;
        protected Set<String> mContacts = null;
        protected String mChatId;

        protected FileTransferChatWindow FTChatWindow = null;
        protected long mSmsId;

        protected int mSentFileTransferType = SentFileTransferManager.TO_SEND;

        protected long mIpMessageId = 0;

        protected int mResendType = SentFileTransferManager.MANUAL_RESEND;

        public class StatusListener implements FileTransferManager.INotificationsListener {
            SentFileTransfer sentFileTransfer = null;

            @Override
            public void notificationsReceived(Intent intent) {
                if (intent.getAction() == FTNotificationsManager.ACTION_STATUS_CHANGE) {
                    boolean status = intent.getBooleanExtra(FTNotificationsManager.KEY_STATUS,
                                            false);
                    if (status == true) {
                        // resend it
                        mOnResendListener.onReSendFileTransfer(sentFileTransfer);
                    }
                }
            }

            StatusListener(SentFileTransfer s) {
                sentFileTransfer = s;
            }
        }

        StatusListener myListener = new StatusListener(this);
        int retryTimes = 0;

        // it is for one2one
        public SentFileTransfer(String contact, String filePath, boolean needSaveDb,
                boolean isBurn, String fid) {
            Log.d(TAG, "SentFileTransfer(), for one2one FT");

            if (fid == null) {
                int dummyId = generateFileTransferTag();
                long dummIpMsgId = Long.valueOf(dummyId);
                mFileTransferTag = (Long.valueOf(dummIpMsgId)).toString();
            } else {
                mFileTransferTag = fid;
            }

            mChatType = ONE2ONE;
            mBurnType = isBurn;
            mDuration = RCSUtils.getDuration(mService.getContext(), filePath); // get
                                                                                        // duration
                                                                                        // form
                                                                                        // filepath
            mContact = contact; // one2one chat, we will save sms db through
                                // contact

            Log.d(TAG, "SentFileTransfer(), mFileTransferTag = " + mFileTransferTag);

            mFileStruct = new FileStruct(filePath, extractFileNameFromPath(filePath),
                    getFileSize(filePath), mFileTransferTag, new Date(), contact,
                    getThumnailFile(filePath), mBurnType, mDuration);

            mMimeType = MediaFile.getMimeTypeForFile(mFileStruct.mName);

            FTChatWindow = new FileTransferChatWindow(mFileStruct, mChatType, mService,
                    FileTransfer.Direction.OUTGOING);

            if (needSaveDb) {
                mSentFileTransferType = SentFileTransferManager.TO_SEND;
                FTChatWindow.saveSendFileTransferToSmsDB();
            } else {
                mSentFileTransferType = SentFileTransferManager.TO_RESUME;
                FTChatWindow.updateFTStatus(IFileTransfer.Status.TRANSFERING);
            }
        }

        // it is for group
        public SentFileTransfer(Set<String> contacts, String filePath, String chatId,
                String chatSessionId, boolean needSaveDb, long dummyId) {
            Log.d(TAG, "SentFileTransfer(), for group FT");

            mFileTransferTag = (Long.valueOf(dummyId)).toString();
            mIpMessageId = dummyId;

            Log.d(TAG, "SentFileTransfer(), mFileTransferTag = " + mFileTransferTag
                    + " mIpMessageId = " + mIpMessageId);
            mChatType = GROUP;
            mBurnType = false;
            mDuration = RCSUtils.getDuration(mService.getContext(), filePath);
            mContacts = contacts;
            mChatId = chatId;
            mChatSessionId = chatSessionId;

            mFileStruct = new FileStruct(filePath, extractFileNameFromPath(filePath),
                    getFileSize(filePath), mFileTransferTag, new Date(), contacts,
                    getThumnailFile(filePath), mDuration);

            mSentFileTransferType = SentFileTransferManager.TO_SEND;

            mMimeType = MediaFile.getMimeTypeForFile(mFileStruct.mName);

            FTChatWindow = new FileTransferChatWindow(mFileStruct, mChatType, chatId, mService,
                    FileTransfer.Direction.OUTGOING);
        }

        // it is for one2multi
        public SentFileTransfer(Set<String> contacts, String filePath, boolean needSaveDb,
                String fid) {
            Log.d(TAG, "SentFileTransfer(), for one2multi FT");
            Log.d(TAG, "SentFileTransfer(), contacts = " + contacts + "filePath = " + filePath
                    + "needSaveDb = " + needSaveDb + "fid = " + fid);

            if (fid == null) {
                int dummyId = generateFileTransferTag();
                long dummIpMsgId = Long.valueOf(dummyId);
                mFileTransferTag = (Long.valueOf(dummIpMsgId)).toString();
            } else {
                mFileTransferTag = fid;
            }
            mChatType = ONE2MULTI;
            mBurnType = false;
            mDuration = RCSUtils.getDuration(mService.getContext(), filePath);
            mContacts = contacts;

            mFileStruct = new FileStruct(filePath, extractFileNameFromPath(filePath),
                    getFileSize(filePath), mFileTransferTag, new Date(), contacts,
                    getThumnailFile(filePath), mDuration);

            mMimeType = MediaFile.getMimeTypeForFile(mFileStruct.mName);

            FTChatWindow = new FileTransferChatWindow(mFileStruct, mChatType, mService,
                    FileTransfer.Direction.OUTGOING);
            if (needSaveDb) {
                mSentFileTransferType = SentFileTransferManager.TO_SEND;
                FTChatWindow.saveSendFileTransferToSmsDB();
            } else {
                mSentFileTransferType = SentFileTransferManager.TO_RESUME;
                FTChatWindow.updateFTStatus(IFileTransfer.Status.TRANSFERING);
            }
        }

        protected void resend() {
            Log.v(TAG, "resend test:  resend() entry! ");

            if (myListener != null) {
                mNotificationsManager.unregisterNotificationsListener(myListener);
            }

            GsmaManager instance = GsmaManager.getInstance();
            FileTransferService fileTransferService = null;
            if (mFileTransferListener == null) {
                mFileTransferListener = new FileTransferSenderListener();
            }
            if (instance != null && (mChatType == ONE2ONE || mChatType == ONE2MULTI)) {
                try {
                    fileTransferService = instance.getFileTransferApi();
                    mFileTransferObject = fileTransferService.resumeFileTransfer(mFileTransferTag,
                            mFileTransferListener);
                } catch (JoynServiceException e) {
                    Log.e(TAG, "Can't get fileTransferService! ");
                }
            } else if (instance != null && mChatType == GROUP) {
                try {
                    if (mResendType == SentFileTransferManager.AUTO_RESEND) {
                        // It is auto resend, so we must call
                        // mService.resendGroupFileTransfer
                        // to active the group chat and get new chatSessionId
                        Log.d(TAG,
                                "Auto resend in group, call mService.resendGroupFileTransfer !!" +
                                "mChatId = " + mChatId + " mIpMessageId = " + mIpMessageId);
                        SimpleGroupChat groupChat = mService.getOrCreateGroupChat(mChatId);
                        groupChat.resendFile(mIpMessageId);
                    } else {
                        fileTransferService = instance.getFileTransferApi();
                        Log.d(TAG, "resumeGroupFileTransfer,  mChatSessionId = " + mChatSessionId);
                        mFileTransferObject = fileTransferService.resumeGroupFileTransfer(
                                mChatSessionId, mFileTransferTag, mFileTransferListener);
                    }
                } catch (JoynServiceException e) {
                    Log.e(TAG, "Can't get fileTransferService! ");
                }
            }
            Log.e(TAG, "resend test --- mFileTransferObject = " + mFileTransferObject);
            Log.e(TAG, "resend test --- old fid = " + mFileTransferTag);
            try {
                if (null != mFileTransferObject) {
                    mFileStruct.mSize = mFileTransferObject.getFileSize();
                    final String fileTransferId = mFileTransferObject.getTransferId();
                    Log.e(TAG, "resend test --- new fid = " + fileTransferId);

                    FTChatWindow.updateInfo(fileTransferId);
                    FTChatWindow.updateFTStatus(Status.TRANSFERING);

                    mFileTransferTag = fileTransferId;
                    mFileStruct.mFileTransferTag = mFileTransferTag;

                } else {
                    // move sentFileTransfer into resendable list
                    onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                    FTChatWindow.updateFTStatus(Status.FAILED);
                    if (retryTimes < 3) {
                        Log.d(TAG, "send fail, and because of network issue, so register listener");
                        retryTimes++;
                        mNotificationsManager.registerNotificationsListener(myListener);
                    }
                }
            } catch (JoynServiceException e) {
                Log.e(TAG, "reAcceptFileTransferInvitation() update fail !");
                onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                FTChatWindow.updateFTStatus(Status.FAILED);
                if (retryTimes < 3) {
                    Log.d(TAG, "send fail, and because of network issue, so register listener");
                    retryTimes++;
                    mNotificationsManager.registerNotificationsListener(myListener);
                }
            }

        }

        protected void send() {
            Log.w(TAG, "checkNext() send new file");
            if (FTChatWindow != null) {
                FTChatWindow.updateFTStatus(IFileTransfer.Status.TRANSFERING);
            }
            GsmaManager instance = GsmaManager.getInstance();
            FileTransferService fileTransferService = null;

            if (myListener != null) {
                mNotificationsManager.unregisterNotificationsListener(myListener);
            }

            if (instance != null) {
                try {
                    fileTransferService = instance.getFileTransferApi();
                } catch (JoynServiceException e) {
                    Log.e(TAG, "Can't get fileTransferService! ");
                }

                if (fileTransferService != null) {
                    try {
                        mFileTransferListener = new FileTransferSenderListener();
                        if (mChatType == ONE2ONE) {
                            // send to one2one
                            if (mBurnType == true) {
                                Log.d(TAG, "Send a burn ft ");
                                mFileTransferObject = fileTransferService.transferBurnFile(
                                        mContact, mFileStruct.mFilePath,
                                        getThumnailFile(mFileStruct.mFilePath),
                                        mFileTransferListener);
                            } else if (mMimeType != null
                                    && (mMimeType.contains("audio")
                                            || mMimeType.contains("video"))) {
                                Log.d(TAG, "Send a audio or video with dur ");
                                mFileTransferObject = fileTransferService.transferMedia(mContact,
                                        mFileStruct.mFilePath,
                                        getThumnailFile(mFileStruct.mFilePath), mDuration,
                                        mFileTransferListener);
                            } else if (mFileStruct.mName.toLowerCase().endsWith(".xml")) {
                                // geolocation
                                Log.d(TAG, "Send a one2one geolocation");
                                mFileTransferObject = fileTransferService.transferGeoLocFile(
                                        mContact, mFileStruct.mFilePath,
                                        getThumnailFile(mFileStruct.mFilePath),
                                        mFileTransferListener);
                            } else {
                                Log.d(TAG, "Send a one2one ft ");
                                mFileTransferObject = fileTransferService.transferFile(mContact,
                                        mFileStruct.mFilePath,
                                        getThumnailFile(mFileStruct.mFilePath),
                                        mFileTransferListener);
                            }
                        } else if (mChatType == GROUP) {
                            // TODO: send to group
                            Log.w(TAG, "checkNext() send new group file thumbnail not supported");
                            if (mResendType == SentFileTransferManager.AUTO_RESEND) {
                                // It is auto resend, so we must call
                                // mService.resendGroupFileTransfer
                                // to active the group chat and get new
                                // chatSessionId
                                Log.d(TAG, "Send, auto group resend ! ");
                                SimpleGroupChat groupChat = mService.getOrCreateGroupChat(mChatId);
                                groupChat.resendFile(mIpMessageId);
                            } else {
                                Log.d(TAG, "Send, call transferFileToGroup and send ft");
                                mFileTransferObject = fileTransferService.transferFileToGroup(
                                        mChatSessionId, mContacts, mFileStruct.mFilePath,
                                        mDuration, mFileTransferListener);
                            }
                        } else if (mChatType == ONE2MULTI) {
                            // send to multi
                            Log.w(TAG, "send a one2multi filetransfer");
                            mFileTransferObject = fileTransferService.transferFileToMultirecepient(
                                    mContacts, mFileStruct.mFilePath, true, mFileTransferListener,
                                    mDuration);
                        }

                        if (null != mFileTransferObject
                                && mFileTransferObject.getTransferId() != null) {

                            String fileTransferId = mFileTransferObject.getTransferId();

                            String oldFileTransferTag = mFileTransferTag;
                            mFileTransferTag = fileTransferId;
                            mFileStruct.mFileTransferTag = fileTransferId;

                            mIpMessageId = FTChatWindow.updateInfo(fileTransferId,
                                    oldFileTransferTag);

                        } else {
                            Log.e(TAG, "send() failed, mFileTransferObject is null, filePath is "
                                    + mFileStruct.mFilePath);

                            FTChatWindow.setSendFail(mFileTransferTag);
                            onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                            mSentFileTransferType = SentFileTransferManager.TO_SEND_AGAIN;

                            // if network cut issue, register listener
                            // boolean serviceReady =
                            // RCSMessageServiceManager.getInstance().serviceIsReady();
                            // boolean serviceReady = false;
                            if (retryTimes < 3) {
                                Log.d(TAG, "send fail, and because of network issue," +
                                        "so register listener");
                                retryTimes++;
                                mNotificationsManager.registerNotificationsListener(myListener);
                            }
                        }
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                        Log.e(TAG, "send() failed, mFileTransferObject is null, filePath is "
                                + mFileStruct.mFilePath);
                        FTChatWindow.setSendFail(mFileTransferTag);
                        onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                        mSentFileTransferType = SentFileTransferManager.TO_SEND_AGAIN;

                        // if network cut issue, register listener
                        // boolean serviceReady =
                        // RCSMessageServiceManager.getInstance().serviceIsReady();
                        // boolean serviceReady = false;
                        if (retryTimes < 3) {
                            Log.d(TAG, "send fail, and because of network issue," +
                                                        "so register listener");
                            retryTimes++;
                            mNotificationsManager.registerNotificationsListener(myListener);
                        }
                    }
                }
            }
        }

        private void onDestroy() {
            Log.d(TAG, "onDestroy() sent file transfer mFilePath "
                    + ((null == mFileStruct) ? null : mFileStruct.mFilePath)
                    + " mFileTransferObject = " + mFileTransferObject
                    + ", mFileTransferListener = " + mFileTransferListener);
            if (myListener != null && mNotificationsManager != null) {
                mNotificationsManager.unregisterNotificationsListener(myListener);
            }
            if (null != mFileTransferObject) {
                try {
                    if (null != mFileTransferListener) {
                        mFileTransferObject.removeEventListener(mFileTransferListener);
                    }
                    mFileTransferObject.abortTransfer();
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onFileTransferFinished(IOnSendFinishListener.Result result) {
            Log.d(TAG, "onFileTransferFinished() mFileStruct = " + mFileStruct + ", file = "
                    + ((null == mFileStruct) ? null : mFileStruct.mFilePath)
                    + ", mOnSendFinishListener = " + mOnSendFinishListener
                    + ", mFileTransferListener = " + mFileTransferListener
                    + ", result = " + result);
            if (null != mOnSendFinishListener) {
                mOnSendFinishListener.onSendFinish(SentFileTransfer.this, result);
                if (null != mFileTransferObject) {
                    try {
                        if (null != mFileTransferListener) {
                            mFileTransferObject.removeEventListener(mFileTransferListener);
                        }
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * File transfer session event listener
         */
        private class FileTransferSenderListener extends FileTransferListener {
            private static final String TAG = "M0CFF FileTransferSenderListener";

            @Override
            public void onTransferPaused() {
                Log.d(TAG, "onTransferPaused() this file is " + mFileStruct.mFilePath);
            }

            /**
             * Callback called when the file transfer is started
             */
            @Override
            public void onTransferStarted() {
                Log.d(TAG, "onTransferStarted() this file is " + mFileStruct.mFilePath);
            }

            /**
             * Callback called when the file transfer has been aborted
             */
            @Override
            public void onTransferAborted() {
                Log.v(TAG, "File transfer onTransferAborted(): mFileTransferTag = "
                        + mFileTransferTag);
                mFileTransferObject = null;
                mSentFileTransferType = SentFileTransferManager.TO_RESUME;
                RCSDataBaseUtils.updateMessageStatus(mService.getContext(),
                        mFileTransferTag, MessageStatus.FAILED);
                onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);

                // if network cut issue, register listener
                // boolean serviceReady =
                // RCSMessageServiceManager.getInstance().serviceIsReady();
                // boolean serviceReady = false;
                if (retryTimes < 3) {
                    retryTimes++;
                    mNotificationsManager.registerNotificationsListener(myListener);
                }
            }

            /**
             * Callback called when the transfer has failed
             *
             * @param error
             *            Error
             * @see FileTransfer.Error
             */
            @Override
            public void onTransferError(int error) {
                Log.v(TAG, "onTransferError(),error = " + error + ", mFileTransferTag ="
                        + mFileTransferTag);

                Log.v(TAG, "resend test:  onTransferError!!!! ");

                switch (error) {
                case FileTransfer.Error.TRANSFER_FAILED:
                    Log.d(TAG, "onTransferError(), the file transfer invitation is failed.");

                    FTChatWindow.updateFTStatus(Status.FAILED);
                    onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                    break;

                case FileTransfer.Error.INVITATION_DECLINED:
                    Log.d(TAG, "onTransferError, your file transfer invitation has been rejected");

                    FTChatWindow.updateFTStatus(Status.FAILED);
                    onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                    break;
                case FileTransfer.Error.SAVING_FAILED:
                    Log.d(TAG, "onTransferError(), saving of file failed");

                    FTChatWindow.updateFTStatus(Status.FAILED);
                    onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                    break;
                default:
                    Log.e(TAG, "onTransferError() unknown error " + error);

                    FTChatWindow.updateFTStatus(Status.FAILED);
                    onFileTransferFinished(IOnSendFinishListener.Result.RESENDABLE);
                    break;
                }
                mSentFileTransferType = SentFileTransferManager.TO_RESUME;

                // if network cut issue, register listener
                // boolean serviceReady =
                // RCSMessageServiceManager.getInstance().serviceIsReady();
                // boolean serviceReady = false;
                if (retryTimes < 3) {
                    retryTimes++;
                    mNotificationsManager.registerNotificationsListener(myListener);
                }

            }

            @Override
            public void onTransferProgress(long currentSize, long totalSize) {

                Log.d(TAG, "onTransferProgress() the file is transferring, currentSize is "
                        + currentSize + " total size is " + totalSize);

            }

            /**
             * Callback called when the file has been transferred
             *
             * @param filename
             *            Filename including the path of the transferred file
             */
            @Override
            public void onFileTransferred(String filename) {
                Log.d(TAG, "onFileTransferred() entry, successfuly, fileName is " + filename);
                FTChatWindow.updateFTStatus(Status.FINISHED);
                mNotificationsManager.unregisterNotificationsListener(myListener);
                myListener = null;
                onFileTransferFinished(IOnSendFinishListener.Result.REMOVABLE);
            }

            @Override
            public void onTransferResumed(String oldFTid, String newFTId) {
                // TODO:
                Log.d(TAG, "onTransferResumed() this file is " + mFileStruct.mFilePath);
            }
        }

        /*
         * private static String buildPercentageLabel(Context context, long
         * totalBytes, long currentBytes) { if (totalBytes <= 0) { return null;
         * } else { final int percent = (int) (100 * currentBytes / totalBytes);
         * return context.getString(R.string.ft_percent, percent); } }
         */

        protected IOnSendFinishListener mOnSendFinishListener = null;

        protected interface IOnSendFinishListener {
            static enum Result {
                REMOVABLE, // This kind of result indicates that this File
                // transfer should be removed from the manager
                RESENDABLE
                // This kind of result indicates that this File transfer will
                // have a chance to be resent in the future
            };

            void onSendFinish(SentFileTransfer sentFileTransfer, Result result);
        }

        protected interface IOnResendListener {
            void onReSendFileTransfer(SentFileTransfer sentFileTransfer);
        }

        protected IOnResendListener mOnResendListener = null;
    }

    /**
     * This class is used to manage the whole receive file transfers
     */
    protected class ReceiveFileTransferManager {
        private static final String TAG = "ReceiveFileTransferManager";

        private CopyOnWriteArrayList<ReceiveFileTransfer> mActiveList =
                                        new CopyOnWriteArrayList<ReceiveFileTransfer>();

        /**
         * Handle a new file transfer invitation (one2one OR group chat)
         */
        public synchronized void addReceiveFileTransfer(FileTransfer fileTransferObject,
                boolean isBurn, String chatId, boolean isGroup, String chatSessionId) {
            Log.d(TAG, "addReceiveFileTransfer() entry, fileTransferObject = "
                                                                + fileTransferObject);
            if (null != fileTransferObject) {
                if (!isValidType(fileTransferObject)) {
                    return;
                }
                ReceiveFileTransfer receiveFileTransfer = new ReceiveFileTransfer(
                        fileTransferObject, isBurn, chatId, isGroup, chatSessionId);
                mActiveList.add(receiveFileTransfer);
            }
        }

        private boolean isValidType(FileTransfer fileTransferObject) {
            String fileName = null;
            try {
                fileName = fileTransferObject.getFileName();
                Log.d(TAG,
                        "isValidType , fileTransferObject.getFileName = "
                                + fileTransferObject.getFileName());
                String mimeType = MediaFile.getMimeTypeForFile(fileName);
                if (mimeType != null) {
                    if (mimeType.contains(RCSUtils.FILE_TYPE_IMAGE)) {
                        return true;
                    } else if (mimeType.contains(RCSUtils.FILE_TYPE_AUDIO)
                            || mimeType.contains("application/ogg")) {
                        return true;
                    } else if (mimeType.contains(RCSUtils.FILE_TYPE_VIDEO)) {
                        return true;
                    }
                }
                if (fileName != null) {
                    if (fileName.toLowerCase().endsWith(".vcf")) {
                        return true;
                    } else if (fileName.toLowerCase().endsWith(".xml")) {
                        return true;
                    }
                }
            } catch (JoynServiceException e) {
            }
            Log.d(TAG, "isValidType , result is false");
            return false;
        }

        public synchronized void addReceiveFileTransfer(String fid, boolean isBurn, String chatId,
                boolean isGroup, String contact, Set<String> contacts) {
            ReceiveFileTransfer receiveFileTransfer = new ReceiveFileTransfer(fid, isBurn, chatId,
                    isGroup, contact, contacts);
            mActiveList.add(receiveFileTransfer);
        }

        public ReceiveFileTransfer getReceiveTransfer() {
            ReceiveFileTransfer resume_file = null;
            Log.d(TAG, "getReceiveTransfer 1");
            try {
                resume_file = mActiveList.get(0);
            } catch (Exception e) {
                Log.d(TAG, "getReceiveTransfer exception");
            }
            return resume_file;
        }

        /**
         * remove receive file transfer from mActiveList and add it to
         * mReDownLoadList
         */
        public synchronized void removeReceiveFileTransfer(
                                                        ReceiveFileTransfer receiveFileTransfer) {
            Log.d(TAG, "removeReceiveFileTransfer() entry, receiveFileTransfer = "
                    + receiveFileTransfer);
            if (null != receiveFileTransfer) {
                mActiveList.remove(receiveFileTransfer);
                Log.d(TAG,
                        "removeReceiveFileTransfer() the file transfer with receiveFileTransfer: "
                                + receiveFileTransfer);
            }
        }

        /**
         * Cancel all the receive file transfers
         */
        public synchronized void cancelReceiveFileTransfer() {
            Log.d(TAG, "cancelReceiveFileTransfer entry");
            ArrayList<ReceiveFileTransfer> tempList = new ArrayList<ReceiveFileTransfer>(
                    mActiveList);

            int size = tempList.size();
            for (int i = 0; i < size; i++) {
                ReceiveFileTransfer receiveFileTransfer = tempList.get(i);
                if (null != receiveFileTransfer) {
                    receiveFileTransfer.cancelFileTransfer();
                }
            }
        }

        public void cancelFileTransfer(String targetFileTransferTag) {
            Log.d(TAG, "cancelFileTransfer, begin to cancel receive file transfer with tag, "
                    + targetFileTransferTag);
            ReceiveFileTransfer receiveFileTransfer = findFileTransferByTagFromActiveList(
                                                            targetFileTransferTag);

            if (receiveFileTransfer != null) {
                receiveFileTransfer.cancelFileTransfer();
                removeReceiveFileTransfer(receiveFileTransfer);
            }
        }

        /**
         * Search an existing file transfer in the receive list form ActiveList
         */
        public synchronized ReceiveFileTransfer findFileTransferByTagFromActiveList(
                                                                            Object targetTag) {
            if (null != mActiveList && null != targetTag) {
                for (ReceiveFileTransfer receiveFileTransfer : mActiveList) {
                    Object fileTransferTag = receiveFileTransfer.mFileTransferTag;
                    if (targetTag.equals(fileTransferTag)) {
                        Log.d(TAG, "findFileTransferByTag() the file transfer with targetTag "
                                + targetTag + " found");
                        return receiveFileTransfer;
                    }
                }
                Log.d(TAG, "findFileTransferByTag() not found targetTag " + targetTag);
                return null;
            } else {
                Log.e(TAG, "findFileTransferByTag(), targetTag is " + targetTag);
                return null;
            }
        }
    }

    /**
     * This class describe one single in-coming file transfer, and control the
     * status itself
     */
    protected class ReceiveFileTransfer {
        private static final String TAG = "ReceiveFileTransfer";

        protected String mFileTransferTag = null;
        protected FileStruct mFileStruct = null;
        protected FileTransfer mFileTransferObject = null;
        protected FileTransferListener mFileTransferListener = null;

        protected static final int ONE2ONE = 0;
        protected static final int GROUP = 2;
        protected String mContact = null;
        protected Set<String> mContacts = null;
        protected long smsId = -1;
        protected int mChatType = ONE2ONE;

        protected String mChatSessionId = null;

        protected long ipMsgId = -1;

        private FileTransferChatWindow FTChatWindow;
        boolean isAutoAccept = false;
        boolean mBurn = false;
        String mChatId;

        public ReceiveFileTransfer(FileTransfer fileTransferObject, boolean isBurn, String chatId,
                boolean isGroup, String chatSessionId) {
            if (null != fileTransferObject) {
                Log.d(TAG, "ReceiveFileTransfer() constructor FileTransfer is "
                        + fileTransferObject);

                handleReceiveFileTransferInvitation(fileTransferObject, chatId, isGroup, isBurn,
                        chatSessionId);
            }
        }

        public ReceiveFileTransfer(String fid, boolean isBurn, String chatId, boolean isGroup,
                String contact, Set<String> contacts) {
            mFileTransferTag = fid;
            mBurn = isBurn;
            mChatId = chatId;

            String filePath = null;
            String fileName = null;
            long fileSize = 0;
            int duration = 0;

            if (isGroup) {
                mChatType = GROUP;
                mContacts = contacts;
                mFileStruct = new FileStruct(filePath, fileName, fileSize, fid, new Date(),
                        mContacts, getThumnailFile(filePath), duration);

            } else {
                mChatType = ONE2ONE;
                mContact = contact;
                mFileStruct = new FileStruct(filePath, fileName, fileSize, fid, new Date(),
                        mContact, getThumnailFile(filePath), isBurn, duration);
            }
            mFileTransferListener = new FileTransferReceiverListener();
            FTChatWindow = new FileTransferChatWindow(mFileStruct, mChatType, chatId, mService,
                    FileTransfer.Direction.INCOMING);
        }

        protected void handleReceiveFileTransferInvitation(FileTransfer fileTransferObject,
                String chatId, boolean isGroup, boolean isBurn, String chatSessionId) {
            Log.d(TAG, "handleFileTransferInvitation() entry ");
            if (fileTransferObject == null) {
                Log.e(TAG, "handleFileTransferInvitation, fileTransferSession is null!");
                return;
            }

            mBurn = isBurn;
            mChatId = chatId; // we need the chatId for saving sms db
            mChatSessionId = chatSessionId;

            if (isGroup) {
                mChatType = GROUP;
            } else {
                mChatType = ONE2ONE;
                try {
                    String number = RCSUtils.extractNumberFromUri(fileTransferObject
                            .getRemoteContact());
                    mContact = number; // we get this contact number for save
                                       // sms db
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                mFileTransferObject = fileTransferObject;
                mFileTransferTag = mFileTransferObject.getTransferId();
                mFileTransferListener = new FileTransferReceiverListener();
                mFileTransferObject.addEventListener(mFileTransferListener);
                mFileStruct = FileStruct.from(mFileTransferObject, isBurn, null);
                FTChatWindow = new FileTransferChatWindow(mFileStruct, mChatType, chatId, mService,
                        FileTransfer.Direction.INCOMING);
            } catch (JoynServiceException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            isAutoAccept = isAutoDownLoad(mFileStruct.mName);

            if (mFileStruct != null && !isAutoAccept) {
                Log.d(TAG, "call saveReceiveFileTransferToSmsDB, isAutoAccept = " + isAutoAccept);
                smsId = FTChatWindow.saveReceiveFileTransferToSmsDB();
            } else if (isAutoAccept) {
                Log.d(TAG, "call acceptFileTransferInvitation, isAutoAccept = " + isAutoAccept);
                acceptFileTransferInvitation();
            }

        }

        private boolean isAutoDownLoad(String filename) {
            String mimeType = MediaFile.getMimeTypeForFile(filename);
            if (mimeType != null) {
                if (mimeType.contains(RCSUtils.FILE_TYPE_VIDEO)) {
                    return false;
                } else if (mimeType.contains(RCSUtils.FILE_TYPE_IMAGE)) {
                    return false;
                }
            }
            return true;
        }

        protected void acceptFileTransferInvitation() {
            if (mFileTransferObject != null) {
                try {
                    // Received file size in byte
                    long receivedFileSize = mFileTransferObject.getFileSize();
                    long currentStorageSize = RCSUtils.getFreeStorageSize();
                    Log.d(TAG, "receivedFileSize = " + receivedFileSize + "/currentStorageSize = "
                            + currentStorageSize);
                    if (currentStorageSize > 0) {
                        if (receivedFileSize <= currentStorageSize) {
                            mFileTransferObject.acceptInvitation();
                            if (!isAutoAccept) {
                                FTChatWindow.updateFTStatus(IFileTransfer.Status.TRANSFERING);
                            }
                        } else {
                            mFileTransferObject.rejectInvitation();

                            FTChatWindow.updateFTStatus(IFileTransfer.Status.REJECTED);
                            Log.d(TAG, "acceptFileTransferInvitation(),fail," +
                                        "because no enough storage to download it");
                        }
                    } else {
                        mFileTransferObject.rejectInvitation();
                        FTChatWindow.updateFTStatus(IFileTransfer.Status.REJECTED);
                        Log.d(TAG, "acceptFileTransferInvitation(),fail," +
                                        "because no enough storage to download it");
                    }
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                FTChatWindow.updateFTStatus(IFileTransfer.Status.FAILED);
                Log.d(TAG, "acceptFileTransferInvitation(), mFileTransferObject is null");
            }
        }

        protected void reAcceptFileTransferInvitation() {
            Log.w(TAG, "reAcceptFileTransferInvitation() entry...");
            GsmaManager instance = GsmaManager.getInstance();
            FileTransferService fileTransferService = null;
            if (instance != null && mChatType == ONE2ONE) {
                Log.w(TAG, "reAcceptFileTransferInvitation in one2one");
                try {
                    fileTransferService = instance.getFileTransferApi();
                    mFileTransferObject = fileTransferService.resumeFileTransfer(mFileTransferTag,
                            mFileTransferListener);
                } catch (JoynServiceException e) {
                    Log.e(TAG, "Can't get fileTransferService! ");
                }
            } else if (instance != null && mChatType == GROUP) {
                Log.w(TAG, "reAcceptFileTransferInvitation in GROUP, mChatSessionId = "
                        + mChatSessionId);
                try {
                    fileTransferService = instance.getFileTransferApi();
                    mFileTransferObject = fileTransferService.resumeGroupFileTransfer(
                            mChatSessionId, mFileTransferTag, mFileTransferListener);
                } catch (JoynServiceException e) {
                    Log.e(TAG, "Can't get fileTransferService! ");
                }
            }
            Log.e(TAG, "reaccept yangfeng test --- old fid = " + mFileTransferTag);
            try {
                if (null != mFileTransferObject) {
                    mFileStruct.mSize = mFileTransferObject.getFileSize();
                    final String fileTransferId = mFileTransferObject.getTransferId();
                    Log.e(TAG, "reaccept yangfeng test --- new fid = " + fileTransferId);

                    // FTChatWindow.updateInfo(fileTransferId,mFileTransferTag,smsId);
                    FTChatWindow.updateFTStatus(Status.TRANSFERING);
                    mFileTransferTag = fileTransferId;
                    mFileStruct.mFileTransferTag = mFileTransferTag;
                }
            } catch (JoynServiceException e) {
                Log.e(TAG, "reAcceptFileTransferInvitation() update fail !");
            }

        }

        protected void abortFileTransfer() {
            Log.w(TAG, "abortFileTransfer() entry...");
            cancelFileTransfer();
        }

        protected void cancelFileTransfer() {
            if (null != mFileTransferObject) {
                try {
                    mFileTransferObject.pauseTransfer();
                    mFileTransferObject.abortTransfer();
                } catch (JoynServiceException e) {
                    e.printStackTrace();
                }
            }
            onFileTransferFailed();
        }

        protected void onFileTransferFailed() {
            // notify file transfer failed on timeout
            FTChatWindow.updateFTStatus(IFileTransfer.Status.FAILED);

            if (mFileTransferListener != null) {
                if (mFileTransferObject != null) {
                    try {
                        mFileTransferObject.removeEventListener(mFileTransferListener);
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                }
            }
            // mReceiveFileTransferManager.removeReceiveFileTransfer(this);
        }

        /**
         * You cancel a file transfer.
         */
        protected void onFileTransferCancel() {

            // notify file transfer cancel
            FTChatWindow.updateFTStatus(IFileTransfer.Status.CANCEL);

            if (mFileTransferListener != null) {
                if (mFileTransferObject != null) {
                    try {
                        mFileTransferObject.removeEventListener(mFileTransferListener);
                    } catch (JoynServiceException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * File transfer session event listener
         */
        private class FileTransferReceiverListener extends FileTransferListener {
            private static final String TAG = "M0CFF FileTransferReceiverListener";
            private long mTotalSize = 0;
            private long mCurrentSize = 0;

            /**
             * Callback called when the file transfer is started
             */
            @Override
            public void onTransferStarted() {
                Log.v(TAG, "onTransferStarted() entry");
            }

            /**
             * Callback called when the file transfer has been aborted
             */
            @Override
            public void onTransferAborted() {
                Log.v(TAG, "onTransferAborted, File transfer onTransferAborted");
                onFileTransferFailed();
            }

            /**
             * Callback called when the transfer has failed
             *
             * @param error
             *            Error
             * @see FileTransfer.Error
             */
            @Override
            public void onTransferError(int error) {
                // TODO:
                switch (error) {
                case FileTransfer.Error.TRANSFER_FAILED:
                    Log.d(TAG, "onTransferError(), the file transfer invitation is failed.");
                    onFileTransferFailed();
                    break;

                default:
                    Log.e(TAG, "onTransferError() unknown error " + error);
                    onFileTransferFailed();
                    break;
                }
            }

            /**
             * Callback called during the transfer progress
             *
             * @param currentSize
             *            Current transferred size in bytes
             * @param totalSize
             *            Total size to transfer in bytes
             */
            @Override
            public void onTransferProgress(long currentSize, long totalSize) {
                Log.d(TAG, "handleTransferProgress() entry: currentSize = " + currentSize
                        + ", totalSize = " + totalSize);
                mTotalSize = totalSize;
                mCurrentSize = currentSize;
            }

            /**
             * Callback called when the file has been transferred
             *
             * @param filename
             *            Filename including the path of the transferred file
             */
            @Override
            public void onFileTransferred(String filename) {
                Log.d(TAG, "onFileTransferred() entry: filename = " + filename);
                if (isAutoAccept) {
                    Log.d(TAG, "is auto download , so saveReceiveFileTransferToSmsDB");
                    FTChatWindow.saveReceiveFileTransferToSmsDB();
                } else {
                    Log.d(TAG, "setFilePath and updateFTStatus");
                    FTChatWindow.setFilePath(filename);
                    FTChatWindow.updateFTStatus(IFileTransfer.Status.FINISHED);
                }

                ReceiveFileTransfer receiveFileTransfer = mReceiveFileTransferManager
                        .findFileTransferByTagFromActiveList(mFileTransferTag);
                if (null != receiveFileTransfer) {
                    mReceiveFileTransferManager.removeReceiveFileTransfer(receiveFileTransfer);
                    return;
                }
            }

            @Override
            public void onTransferResumed(String oldFTid, String newFTId) {
                // TODO:
            }

            @Override
            public void onTransferPaused() {
                // TODO
            }

        }

    }

    /**************************** common API *******************************/

    // new: generate sentfiletransfer for one2one chat
    private SentFileTransfer generateSentFileTransfer(String contact, String filePath,
            boolean needSaveDb, boolean isBurn, String fid) {
        return new SentFileTransfer(contact, filePath, needSaveDb, isBurn, fid);
    }

    // new: generate sentfiletransfer for group chat
    private SentFileTransfer generateSentFileTransfer(Set<String> contacts, String filePath,
            String chatId, String chatSessionId, boolean needSaveDb, long dummyId) {
        return new SentFileTransfer(contacts, filePath, chatId, chatSessionId, needSaveDb, dummyId);
    }

    // new: generate sentfiletransfer for one2multi chat
    private SentFileTransfer generateSentFileTransfer(Set<String> contacts, String filePath,
            boolean needSaveDb, String fid) {
        return new SentFileTransfer(contacts, filePath, needSaveDb, fid);
    }

    // new: send filetransfer in one2one chat
    public void handleSendFileTransferInvitation(String contact, String filePath, boolean isBurn) {
        Log.d(TAG, "handleSendFileTransferInvitation entry, it is for one2one ft");
        boolean needSaveDb = true;
        String fid = null;
        mOutGoingFileTransferManager.onAddSentFileTransfer(generateSentFileTransfer(contact,
                filePath, needSaveDb, isBurn, fid));
    }

    // new: send filetransfer in One2Multi chat
    public void handleSendFileTransferInvitation(List<String> contacts, String filePath) {
        Log.d(TAG, "handleSendFileTransferInvitation entry, it is for one2multi ft");
        Set<String> ctcts = new HashSet<String>();
        ctcts.addAll(contacts);
        boolean needSaveDb = true;
        String fid = null;
        mOutGoingFileTransferManager.onAddSentFileTransfer(generateSentFileTransfer(ctcts,
                filePath, needSaveDb, fid));
    }

    // new: It is for send a group file transfer, called by shuo
    public void handleSendFileTransferInvitation(List<String> contacts, String filePath,
            String chatId, String chatSessionId, long dummyId) {
        Log.d(TAG, "handleSendFileTransferInvitation entry, it is for group chat ft");
        boolean needSaveDb = true;
        Set<String> ctcts = new HashSet<String>();
        ctcts.addAll(contacts);
        mOutGoingFileTransferManager.onAddSentFileTransfer(generateSentFileTransfer(ctcts,
                filePath, chatId, chatSessionId, needSaveDb, dummyId));
    }

    // new: resend in one2one and one2multi
    public void handleResendFileTransfer(long ipMsgId) {
        Log.d(TAG, "handleResendFileTransfer in one2one or one2multi, enter!!");
        FTInfo ftInfo = getIpFTInfo(ipMsgId);
        if (isDummyId(ipMsgId)) {
            // it is a failed message not recorded is stack
            // so we should send it not resume
            Log.d(TAG, "handleResendFileTransfer, isDummyId");
            boolean needSaveDb = false;
            SentFileTransfer sentFileTransfer = null;
            sentFileTransfer = mOutGoingFileTransferManager.findResendableFileTransfer(ftInfo.fid);

            if (sentFileTransfer == null) {
                Log.d(TAG, "No dummy sentfileTransfer in Resendable List, generate it");
                if (ftInfo.contacts == null && ftInfo.contact != null) {
                    // it is for one2one
                    sentFileTransfer = generateSentFileTransfer(ftInfo.contact, ftInfo.filePath,
                            needSaveDb, ftInfo.isBurn, ftInfo.fid);
                } else if (ftInfo.contacts != null && ftInfo.contact == null) {
                    // it is for one2multi
                    sentFileTransfer = generateSentFileTransfer(ftInfo.contacts, ftInfo.filePath,
                            needSaveDb, ftInfo.fid);
                }
            } else {
                Log.d(TAG, "Get dummy sentfileTransfer from Resendable List," +
                            "need remove it from  Resendable List");
                mOutGoingFileTransferManager.cancelFileTransfer(ftInfo.fid);
            }

            if (sentFileTransfer != null) {
                sentFileTransfer.mSentFileTransferType = SentFileTransferManager.TO_SEND_AGAIN;
                mOutGoingFileTransferManager.onAddSentFileTransfer(sentFileTransfer);
            } else {
                Log.d(TAG, "Cannot get dummy sentfileTransfer");
            }
        } else {
            // String fid = RCSUtils.getFTids(ipMsgId);
            Log.d(TAG, "handleResendFileTransfer, is no dummy !");
            SentFileTransfer fileTransfer = mOutGoingFileTransferManager
                    .findResendableFileTransfer(ftInfo.fid);
            boolean needSaveDb = false;
            boolean haveInstance = true;

            if (null != fileTransfer) {
                Log.d(TAG, "handleResendFileTransfer, null != fileTransfer");
                fileTransfer.mSentFileTransferType = SentFileTransferManager.TO_RESUME;
                mOutGoingFileTransferManager.resendFileTransfer(fileTransfer, haveInstance);
            } else {
                // there is no sentFileTransfer object
                Log.d(TAG, "handleResendFileTransfer, null == fileTransfer");

                if (ftInfo.contacts == null && ftInfo.contact != null) {
                    // it is for one2one
                    fileTransfer = generateSentFileTransfer(ftInfo.contact, ftInfo.filePath,
                            needSaveDb, ftInfo.isBurn, ftInfo.fid);
                } else if (ftInfo.contacts != null && ftInfo.contact == null) {
                    // it is for one2multi
                    fileTransfer = generateSentFileTransfer(ftInfo.contacts, ftInfo.filePath,
                            needSaveDb, ftInfo.fid);
                }
                haveInstance = false;
                mOutGoingFileTransferManager.resendFileTransfer(fileTransfer, haveInstance);
            }
        }
    }

    // new: resend in group, it will called by shuo
    public void handleResendFileTransfer(long ipMsgId, String chatId, String chatSessionId) {
        Log.d(TAG, "handleResendFileTransfer in group chat, enter!!");
        FTInfo ftInfo = getIpFTInfo(ipMsgId);

        if (isDummyId(ipMsgId)) {
            Log.d(TAG, "handleResendFileTransfer in group chat, isDummyId");

            boolean needSaveDb = false;
            SentFileTransfer sentFileTransfer = null;
            sentFileTransfer = mOutGoingFileTransferManager.findResendableFileTransfer(ftInfo.fid);

            if (sentFileTransfer == null) {
                Log.d(TAG, "No dummy sentfileTransfer in Resendable List, generate it");
                sentFileTransfer = generateSentFileTransfer(ftInfo.contacts, ftInfo.filePath,
                        chatId, chatSessionId, needSaveDb, ipMsgId);
            } else {
                Log.d(TAG, "Get dummy sentfileTransfer from Resendable List," +
                                            "need remove it from  Resendable List");
                sentFileTransfer.mChatSessionId = chatSessionId;
                mOutGoingFileTransferManager.cancelFileTransfer(ftInfo.fid);
            }

            if (sentFileTransfer != null) {
                sentFileTransfer.mSentFileTransferType = SentFileTransferManager.TO_SEND_AGAIN;
                sentFileTransfer.mResendType = SentFileTransferManager.MANUAL_RESEND;
                mOutGoingFileTransferManager.onAddSentFileTransfer(sentFileTransfer);
            } else {
                Log.d(TAG, "Cannot get dummy sentfileTransfer");
            }
        } else {
            Log.d(TAG, "handleResendFileTransfer in group chat, is NOT DummyId");
            SentFileTransfer fileTransfer = mOutGoingFileTransferManager
                    .findResendableFileTransfer(ftInfo.fid);
            boolean needSaveDb = false;
            boolean haveInstance = true;
            if (null != fileTransfer) {
                Log.d(TAG, "handleResendFileTransfer in group chat, null != fileTransfer");
                fileTransfer.mChatSessionId = chatSessionId;
                fileTransfer.mSentFileTransferType = SentFileTransferManager.TO_RESUME;
                fileTransfer.mResendType = SentFileTransferManager.MANUAL_RESEND;
                mOutGoingFileTransferManager.resendFileTransfer(fileTransfer, haveInstance);
            } else {
                Log.d(TAG, "handleResendFileTransfer in group chat, null == fileTransfer");
            }
        }
    }

    public void handleRecevieFileTransferInvitationInGroup(Intent invitation) {
        handleRecevieFileTransferInvitation(invitation);
    }

    // new: handle receive file transfer invite
    public void handleRecevieFileTransferInvitation(Intent invitation) {
        String fid = invitation.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
        boolean isAutoAccept = invitation.getBooleanExtra(AUTO_ACCEPT, false);
        byte[] thumbNail = invitation.getByteArrayExtra("thumbnail");
        String chatSessionId = invitation.getStringExtra(CHAT_SESSION_ID);
        boolean isGroup = invitation.getBooleanExtra(ISGROUPTRANSFER, false);
        String chatId = invitation.getStringExtra(CHAT_ID);
        boolean isBurn = invitation.getBooleanExtra(FileTransferIntent.EXTRA_BURN, false);
        if (handleFileTransferInvitation(fid, isBurn, chatId, isGroup, chatSessionId)) {
            Log.d(TAG, "Have handleFileTransferInvitation ");
        } else {
            Log.d(TAG, "handleFileTransferInvitation occur some error!");
        }
    }

    // new: handle invitaiton (one2one and group)
    public boolean handleFileTransferInvitation(String fid, boolean isBurn, String chatId,
            boolean isGroup, String chatSessionId) {
        Log.w(TAG, "handleFileTransferInvitation() entry, fid = " + fid + "isBurn = " + isBurn
                + "isGroup = " + isGroup + "chatSessionId = " + chatSessionId);

        GsmaManager instance = GsmaManager.getInstance();
        FileTransferService fileTransferService = null;

        if (instance != null) {
            try {
                fileTransferService = instance.getFileTransferApi();
            } catch (JoynServiceException e) {
                Log.e(TAG, "get fileTransferService fail !");
            }

            if (fileTransferService != null) {
                try {
                    FileTransfer fileTransferObject = fileTransferService.getFileTransfer(fid);
                    if (fileTransferObject == null) {
                        Log.w(TAG,
                                "handleFileTransferInvitation-The getFileTransferSession is null");
                        return false;
                    }
                    mReceiveFileTransferManager.addReceiveFileTransfer(fileTransferObject, isBurn,
                            chatId, isGroup, chatSessionId);
                    return true;
                } catch (JoynServiceException e) {
                    Log.e(TAG, "M0CFF handleFileTransferInvitation-getChatSession fail");
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "M0CFF handleFileTransferInvitation-getParticipants fail");
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // new : download a filetransfer
    public void handleAcceptFileTransfer(String fileTransferTag) {
        Log.e(TAG, "handleAcceptFileTransfer() enter");
        ReceiveFileTransfer receiveFileTransfer = mReceiveFileTransferManager
                .findFileTransferByTagFromActiveList(fileTransferTag);
        if (null != receiveFileTransfer) {
            Log.w(TAG, "handleAcceptFileTransfer, receiveFileTransfer != null");
            receiveFileTransfer.acceptFileTransferInvitation();
        } else {
            Log.w(TAG, "handleAcceptFileTransfer, receiveFileTransfer == null");
            boolean isGroup = false;
            String chatId = null;
            FTInfo ftInfo = getIpFTInfo(fileTransferTag, FileTransfer.Direction.INCOMING);
            mReceiveFileTransferManager.addReceiveFileTransfer(fileTransferTag, ftInfo.isBurn,
                    chatId, isGroup, ftInfo.contact, ftInfo.contacts);
            receiveFileTransfer = mReceiveFileTransferManager
                    .findFileTransferByTagFromActiveList(fileTransferTag);
            if (null != receiveFileTransfer) {
                Log.d(TAG, "handleAcceptFileTransfer(), call acceptFileTransferInvitation()");
                receiveFileTransfer.acceptFileTransferInvitation();
            }
        }
    }

    // new: call by shuo,download a filetransfer from group
    public void handleAcceptFileTransferInGroup(String fileTransferTag) {
        ReceiveFileTransfer receiveFileTransfer = mReceiveFileTransferManager
                .findFileTransferByTagFromActiveList(fileTransferTag);
        if (null != receiveFileTransfer) {
            receiveFileTransfer.acceptFileTransferInvitation();
        } else {
            Log.w(TAG, "handleAcceptFileTransferInGroup, receiveFileTransfer == null");
            boolean isGroup = true;
            String chatId = null;
            FTInfo ftInfo = getIpFTInfo(fileTransferTag, FileTransfer.Direction.INCOMING);
            mReceiveFileTransferManager.addReceiveFileTransfer(fileTransferTag, ftInfo.isBurn,
                    chatId, isGroup, ftInfo.contact, ftInfo.contacts);
            receiveFileTransfer = mReceiveFileTransferManager
                    .findFileTransferByTagFromActiveList(fileTransferTag);
            if (null != receiveFileTransfer) {
                Log.d(TAG, "handleAcceptFileTransfer(), call acceptFileTransferInvitation()");
                receiveFileTransfer.acceptFileTransferInvitation();
            }
        }
    }

    // new: redownload in one2one
    public void handleReAcceptFileTransfer(String fileTransferTag) {
        Log.w(TAG, "handleReAcceptFileTransfer() entry...");
        ReceiveFileTransfer receiveFileTransfer = mReceiveFileTransferManager
                .findFileTransferByTagFromActiveList(fileTransferTag);
        if (null != receiveFileTransfer) {
            Log.w(TAG, "null != receiveFileTransfer, call reAcceptFileTransferInvitation directly");
            receiveFileTransfer.reAcceptFileTransferInvitation();
        } else {
            Log.w(TAG, "receiveFileTransfer == null");
            boolean isGroup = false;
            String chatId = null;
            FTInfo ftInfo = getIpFTInfo(fileTransferTag, RcsLog.Direction.INCOMING);
            mReceiveFileTransferManager.addReceiveFileTransfer(fileTransferTag, ftInfo.isBurn,
                    chatId, isGroup, ftInfo.contact, ftInfo.contacts);
            receiveFileTransfer = mReceiveFileTransferManager
                    .findFileTransferByTagFromActiveList(fileTransferTag);
            if (null != receiveFileTransfer) {
                Log.d(TAG, "handleAcceptFileTransfer() entry, now reAcceptFileTransferInvitation" +
                        "after reboot");
                receiveFileTransfer.reAcceptFileTransferInvitation();
            }
        }
    }

    // new: redownload in group
    public void handleReAcceptFileTransferInGroup(String fileTransferTag, String chatSessionId) {
        Log.w(TAG, "handleReAcceptFileTransferInGroup() entry...");
        Log.w(TAG, "fileTransferTag = " + fileTransferTag + "chatSessionId = " + chatSessionId);
        ReceiveFileTransfer receiveFileTransfer = mReceiveFileTransferManager
                .findFileTransferByTagFromActiveList(fileTransferTag);
        if (null != receiveFileTransfer) {
            Log.w(TAG, "null != receiveFileTransfer, call reAcceptFileTransferInvitation directly");
            receiveFileTransfer.mChatSessionId = chatSessionId;
            receiveFileTransfer.reAcceptFileTransferInvitation();
        } else {
            Log.w(TAG, "receiveFileTransfer == null");

            boolean isGroup = true;
            String chatId = null;
            FTInfo ftInfo = getIpFTInfo(fileTransferTag, RcsLog.Direction.INCOMING);
            mReceiveFileTransferManager.addReceiveFileTransfer(fileTransferTag, ftInfo.isBurn,
                    chatId, isGroup, ftInfo.contact, ftInfo.contacts);

            receiveFileTransfer = mReceiveFileTransferManager
                    .findFileTransferByTagFromActiveList(fileTransferTag);

            if (null != receiveFileTransfer) {
                Log.d(TAG, "handleAcceptFileTransfer() entry," +
                        "now reAcceptFileTransferInvitation after reboot");
                receiveFileTransfer.mChatSessionId = chatSessionId;
                receiveFileTransfer.reAcceptFileTransferInvitation();
            }
        }
    }

    /**
     * this method is for cancel filetransfer obj in manager.
     *
     * @param IpMsgId
     *            , ipmessage id in sms, it should be negative.
     * @return void.
     */
    public void handleCancelFileTransfer(long IpMsgId) {
        Log.d(TAG, "handleCancelFileTransfer() , IpMsgId = " + IpMsgId);
        // TODO
        String fid = RCSDataBaseUtils.findMsgIdInRcsDb(mService.getContext(), IpMsgId);
        handleCancelFileTransfer(fid);
    }

    public void handleCancelFileTransfer(String fileTransferTag) {
        Log.d(TAG, "handleCancelFileTransfer() , fileTransferTag = " + fileTransferTag);
        mOutGoingFileTransferManager.cancelFileTransfer(fileTransferTag);
        mReceiveFileTransferManager.cancelFileTransfer(fileTransferTag);
    }

    public void handlePauseFileTransfer(String fileTransferTag) {
        Log.d(TAG, "handlePauseFileTransfer() , fileTransferTag = " + fileTransferTag);
        ReceiveFileTransfer receiveFileTransfer = mReceiveFileTransferManager
                .findFileTransferByTagFromActiveList(fileTransferTag);
        if (receiveFileTransfer != null) {
            receiveFileTransfer.abortFileTransfer();
        }
    }

    public void handleFileTransferDeliveryStatus(Intent invitation) {
        String fileTransferTag = invitation.getStringExtra("msgId");
        String status = invitation.getStringExtra("status");
        Log.d(TAG, "handleFileTransferDeliveryStatus() from broadcast, msgId: " + fileTransferTag
                + ", status: " + status);
        FileTransferChatWindow.onReceiveMessageDeliveryStatus(
                mService.getContext(), fileTransferTag, status);
        return;
    }

    public void onStatusChanged(boolean status) {
        Log.d(TAG, "onStatusChanged =" + status);
        Intent intent = new Intent();
        intent.setAction(FTNotificationsManager.ACTION_STATUS_CHANGE);
        intent.putExtra(FTNotificationsManager.KEY_STATUS, status);
        mNotificationsManager.notify(intent);
    }

    /**************************** Utils function *******************************/

    public static String extractFileNameFromPath(String filePath) {
        if (null != filePath) {
            int lastDashIndex = filePath.lastIndexOf("/");
            if (-1 != lastDashIndex && lastDashIndex < filePath.length() - 1) {
                String fileName = filePath.substring(lastDashIndex + 1);
                return fileName;
            } else {
                Log.e(TAG, "extractFileNameFromPath() invalid file path:" + filePath);
                return null;
            }
        } else {
            Log.e(TAG, "extractFileNameFromPath() filePath is null");
            return null;
        }
    }

    public static long getFileSize(String fileName) {
        return RCSUtils.getFileSize(fileName);
    }

    public static String getThumnailFile(String fileName) {
        // now stack can handle it
        return null;
    }

    private static int generateFileTransferTag() {
        int messageTag = RANDOM.nextInt(1000) + 1;
        messageTag = Integer.MAX_VALUE - messageTag;
        Log.d(TAG, "generateMessageTag() messageTag: " + messageTag);
        return messageTag;
    }

    private boolean isDummyId(long ipmsgId) {
        if (ipmsgId > Integer.MAX_VALUE - 1001 && ipmsgId < Integer.MAX_VALUE) {
            return true;
        } else {
            return false;
        }
    }

    private class FTInfo {
        FTInfo(String fp, String ct, boolean ib, String ft) {
            filePath = fp;
            contact = ct;
            isBurn = ib;
            fid = ft;
        }

        FTInfo(String fp, Set<String> ct, boolean ib, String ft) {
            filePath = fp;
            contacts = ct;
            isBurn = ib;
            fid = ft;
        }

        String contact = null;
        Set<String> contacts = null;
        String filePath = null;
        boolean isBurn = false;
        String fid = null;
    }

    private FTInfo getIpFTInfo(String fid, int direction) {
        Log.d(TAG, "getIpFTInfo, fid = " + fid);
        String selection = MessageColumn.MESSAGE_ID + "='" + fid + "' AND " +
                MessageColumn.DIRECTION + "=" + direction;
        Cursor cursor = mService.getContext().getContentResolver().query(
                MessageColumn.CONTENT_URI, RCSDataBaseUtils.PROJECTION_MESSAGE,
                selection, null, null);
        return getFTInfo(cursor);
    }

    private FTInfo getIpFTInfo(long ipMsgId) {
        Log.d(TAG, "getIpFTInfo, ipMsgId = " + ipMsgId);
        ContentResolver resolver = mService.getContext().getContentResolver();
        String selection = MessageColumn.IPMSG_ID + "=" + ipMsgId + " AND " + MessageColumn.TYPE
                + "=" + MessageType.FT;
        Cursor cursor = resolver.query(RCSDataBaseUtils.URI_RCS_MESSAGE,
                RCSDataBaseUtils.PROJECTION_MESSAGE, selection, null, null);
        return getFTInfo(cursor);
    }

    private FTInfo getFTInfo(Cursor cursor) {
        FTInfo info = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String filePath = cursor.getString(cursor.getColumnIndex(MessageColumn.FILE_PATH));
                String contact = cursor.getString(
                                        cursor.getColumnIndex(MessageColumn.CONTACT_NUMBER));
                long ipMsgId = cursor.getLong(cursor.getColumnIndex(MessageColumn.IPMSG_ID));
                int msgType = cursor.getInt(cursor.getColumnIndex(MessageColumn.CLASS));
                boolean isBurn = msgType == Class.BURN ? true : false;
                String fid = null;
                if (isDummyId(ipMsgId)) {
                    fid = (Long.valueOf(ipMsgId)).toString();
                    Log.d(TAG, "getIpFTInfo, isDummyId,  fid = " + fid);
                } else {
                    fid = cursor.getString(cursor.getColumnIndex(MessageColumn.MESSAGE_ID));
                }
                if (contact != null && contact.contains(SEPRATOR)) {
                    // it is one2multi
                    String[] contacts;
                    contacts = contact.split(SEPRATOR);
                    Set<String> remotes = new HashSet<String>();
                    for (String recipient : contacts) {
                        remotes.add(recipient);
                    }
                    info = new FTInfo(filePath, remotes, isBurn, fid);
                } else {
                    // it is one2one
                    info = new FTInfo(filePath, contact, isBurn, fid);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return info;
    }
}
