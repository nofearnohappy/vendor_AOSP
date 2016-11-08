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

import org.gsma.joyn.ft.FileTransfer;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;
import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.ReceiveMessageStruct;
import com.mediatek.rcs.common.provider.SendMessageStruct;
import com.mediatek.rcs.common.provider.SpamMsgData;
import com.mediatek.rcs.common.provider.SpamMsgUtils;
import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;

/**
 * Plugin ChatWindow file transfer
 */
public class FileTransferChatWindow {

    private static final String TAG = "FileTransferChatWindow";

    public static final int STARTING = 0;
    public static final int DOWNLOADING = 1;
    public static final int DONE = 2;

    private FileStruct mFileTransfer;

    public static final int ONE2ONE = 0;
    public static final int ONE2MULTI = 1;
    public static final int GROUP = 2;

    private boolean mBurn = false;
    private int mChatType;

    private long mIpMsgId; // it is +
    private String mFileTransferTag;

    private String mChatId;
    private long mSmsId = -1;

    private RCSChatServiceBinder mService = null;

    public FileTransferChatWindow(FileStruct fileTransfer, int chatType, String chatId,
            RCSChatServiceBinder service, int direction) {
        mService = service;
        mChatId = chatId;
        mFileTransfer = fileTransfer;
        mChatType = chatType;
        mBurn = fileTransfer.mSessionType;
        mFileTransferTag = fileTransfer.mFileTransferTag;
        mIpMsgId = RCSDataBaseUtils.getStackFTMessageId(mService.getContext(),
                mFileTransferTag, direction);
        if (mIpMsgId == 0) {
            Log.d(TAG, "FileTransferChatWindow, mIpMsgId == 0 !");
            mIpMsgId = Long.valueOf(mFileTransfer.mFileTransferTag);
        }
        Log.d(TAG, "FileTransferChatWindow, mIpMsgId = " + mIpMsgId);
        Log.d(TAG, "FileTransferChatWindow, mFileTransferTag = " + mFileTransferTag);
    }

    public FileTransferChatWindow(FileStruct fileTransfer, int chatType,
            RCSChatServiceBinder service, int direction) {
        mService = service;
        mFileTransfer = fileTransfer;
        mChatType = chatType;
        mBurn = fileTransfer.mSessionType;
        mFileTransferTag = fileTransfer.mFileTransferTag;
        mIpMsgId = RCSDataBaseUtils.getStackFTMessageId(mService.getContext(),
                mFileTransferTag, direction);
        if (mIpMsgId == 0) {
            Log.d(TAG, "FileTransferChatWindow, mIpMsgId == 0 !");
            mIpMsgId = Long.valueOf(mFileTransfer.mFileTransferTag);
        }
        Log.d(TAG, "FileTransferChatWindow, mIpMsgId = " + mIpMsgId);
        Log.d(TAG, "FileTransferChatWindow, mFileTransferTag = " + mFileTransferTag);
    }

    public long saveSendFileTransferToSmsDB() {
        MessageStruct struct = null;
        if (mChatType == ONE2ONE) {
            int msgType = mBurn ? Class.BURN : Class.NORMAL;
            struct = new SendMessageStruct(mFileTransfer.mRemote, mService.getContext(), msgType,
                    mFileTransfer.mFilePath, Long.valueOf(mFileTransfer.mFileTransferTag));
        } else if (mChatType == ONE2MULTI) {
            struct = new SendMessageStruct(mFileTransfer.mRemotes, mService.getContext(),
                    mFileTransfer.mFilePath, Long.valueOf(mFileTransfer.mFileTransferTag));
        }
        mSmsId = struct.saveMessage();

        return mSmsId;
    }

    public long saveReceiveFileTransferToSmsDB() {
        mIpMsgId = RCSDataBaseUtils.getStackFTMessageId(mService.getContext(),
                mFileTransferTag, FileTransfer.Direction.INCOMING);

        if (mChatType == ONE2ONE) {
            if (RCSUtils.isIpSpamMessage(mService.getContext(), mFileTransfer.mRemote)) {
                Log.d(TAG, "onReceiveChatMessage, spam msg, contact=" + mFileTransfer.mRemote);
                int type = mBurn ? SpamMsgData.Type.TYPE_IP_BURN_FT_MSG
                        : SpamMsgData.Type.TYPE_IP_FT_MSG;
                SpamMsgUtils.getInstance(mService.getContext()).insertSpamMessage(
                        mFileTransfer.mFilePath, mFileTransfer.mRemote, RCSUtils.getRCSSubId(),
                        type, mIpMsgId, mFileTransferTag);
                return -1;
            }
        }
        if (mChatType == ONE2ONE) {
            int msgType = mBurn ? Class.BURN : Class.NORMAL;
            MessageStruct struct = new ReceiveMessageStruct(mFileTransfer.mRemote,
                    mService.getContext(), mFileTransfer.mFilePath, mFileTransferTag,
                    mIpMsgId, msgType);
            mSmsId = struct.saveMessage();
            mService.getListener().onNewMessage(mSmsId);
        } else if (mChatType == GROUP) {
            MessageStruct struct = new ReceiveMessageStruct(mChatId, mFileTransfer.mRemote,
                    mService.getContext(), mFileTransfer.mFilePath, mFileTransferTag,
                    mIpMsgId);
            mSmsId = struct.saveMessage();
            mService.getListener().onNewGroupMessage(mChatId, mSmsId, mFileTransfer.mRemote);
        }
        return mSmsId;
    }

    public long updateInfo(String newFileTransferTag, String oldFileTransferTag) {
        Log.d(TAG, "updateInfo enter, newFileTransferTag = " + newFileTransferTag
                + "oldFileTransferTag" + oldFileTransferTag);
        mFileTransfer.mFileTransferTag = newFileTransferTag;
        mFileTransferTag = newFileTransferTag;

        mSmsId = getSmsId(oldFileTransferTag);
        mIpMsgId = RCSDataBaseUtils.getStackFTMessageId(mService.getContext(),
                newFileTransferTag, FileTransfer.Direction.OUTGOING);
        RCSDataBaseUtils.combineMsgId(mService.getContext(), mSmsId, newFileTransferTag,
                MessageType.FT);
        return mIpMsgId;
    }

    public void setSendFail(String fileTransferTag) {
        if (mSmsId == -1) {
            mSmsId = getSmsId(fileTransferTag);
        }
        RCSDataBaseUtils.updateMessageStatus(mService.getContext(), mSmsId,
                MessageStatus.FAILED);
    }

    private long getSmsId(String fileTransferTag) {
        return RCSDataBaseUtils.getFTMessageId(mService.getContext().getContentResolver(),
                Long.valueOf(fileTransferTag));
    }

    public void updateInfo(String newFileTransferTag) {
        mFileTransferTag = newFileTransferTag;
    }

    public void updateFTStatus(Status status) {
        // Notify app
        Log.d(TAG, "updateFTStatus enter, Status = " + status);
        updateStatus(mIpMsgId, status);
    }

    private void updateStatus(long ipMsgId, Status status) {

        long smsId = -1;
        int type = -1;

        Log.d(TAG, "updateStatus enter, Status = " + status + " ipMsgId = " + ipMsgId);

        Context context = mService.getContext();
        if (context != null) {
            smsId = RCSDataBaseUtils.getFTMessageId(context.getContentResolver(), ipMsgId);
            type = RCSDataBaseUtils.getMessageStatus(context, smsId);
            Log.d(TAG, "smsId = " + smsId + " type " + type);
        }
        if (smsId > 0) {
            switch (status) {
            case FINISHED:
                Log.d(TAG, " drawDeleteBARMsgIndicator FT smsId = " + smsId + ",type = " + type);
                if (type == MessageStatus.SENDING || type == MessageStatus.FAILED) {

                    int stat = RCSUtils.getIntStatus(status);
                    mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                            MessageStatus.SENT);

                    RCSDataBaseUtils.updateMessageStatus(mService.getContext(), smsId,
                            MessageStatus.SENT);
                    deleteBurnedMsg(smsId);
                } else {
                    int stat = RCSUtils.getIntStatus(status);
                    mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat, type);
                }
                break;
            case TRANSFERING:
                if (type == MessageStatus.FAILED) {
                    Log.d(TAG, " updateStatus TRANSFERING ");
                    RCSDataBaseUtils.updateMessageStatus(mService.getContext(), smsId,
                            MessageStatus.SENDING);
                }
                break;
            case WAITING:
                break;
            case CANCEL:
                break;
            case FAILED:
                if (type == MessageStatus.SENDING) {

                    int stat = RCSUtils.getIntStatus(status);
                    mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                            MessageStatus.FAILED);
                    RCSDataBaseUtils.updateMessageStatus(mService.getContext(), smsId,
                            MessageStatus.FAILED);
                } else if (type == MessageStatus.UNREAD || type == MessageStatus.READ) {
                    int stat = RCSUtils.getIntStatus(status);
                    mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat, type);
                    Log.d(TAG, "download file fail !");
                }
                break;
            case TIMEOUT:
                break;
            case REJECTED:
                if (type == MessageStatus.SENDING) {

                    int stat = RCSUtils.getIntStatus(status);
                    mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat,
                            MessageStatus.FAILED);
                    RCSDataBaseUtils.updateMessageStatus(mService.getContext(), smsId,
                            MessageStatus.FAILED);
                } else {
                    Log.d(TAG, "download file fail !");
                    int stat = RCSUtils.getIntStatus(status);
                    mService.getListener().onUpdateFileTransferStatus(mIpMsgId, stat, type);
                }
                break;
            case PENDING:
                break;
            default:
                break;
            }
        }
    }

    private void deleteBurnedMsg(final long msgId) {
        Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg()");
        final Context context = mService.getContext();
        Cursor cursor = RCSDataBaseUtils.getMessage(context, msgId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int msgType = cursor.getInt(cursor.getColumnIndex(MessageColumn.CLASS));
                final String messageId = cursor.getString(cursor
                        .getColumnIndex(MessageColumn.MESSAGE_ID));
                Log.d(TAG, " drawDeleteBARMsgIndicator deleteBurnedMsg, sessionType=" + msgType
                        + " ipMsgId = " + msgId);
                if (msgType == Class.BURN) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(5100);
                                RCSDataBaseUtils.deleteBurnMessage(context, messageId,
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

    public void setFilePath(String filePath) {
        Log.d(TAG, "setFilePath() entry! filePath = " + filePath);
        RCSDataBaseUtils.updateFTMsgFilePath(mService.getContext(), filePath, mIpMsgId);
        mService.getListener().setFilePath(mIpMsgId, filePath);
    }

    public static void onReceiveMessageDeliveryStatus(Context ctx, String fid, String status) {
        Log.d(TAG, "onReceiveMessageDeliveryStatus() entry! fid = " + fid + "status = " + status);
        if (status.equalsIgnoreCase("delivered")) {
            Log.d(TAG, "send filetransfer delivered !!");

            RCSDataBaseUtils.updateMessageStatus(ctx, fid,
                    MessageStatus.DELIVERED);
        }
    }

}
