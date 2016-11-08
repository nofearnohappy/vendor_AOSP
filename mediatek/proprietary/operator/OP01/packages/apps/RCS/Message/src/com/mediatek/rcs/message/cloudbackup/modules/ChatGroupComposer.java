package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.xmlpull.v1.XmlSerializer;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Chat;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.GroupChat;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Message;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.GroupNumberRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.util.Xml;

/**
 * Restore group message file.
 * @author mtk81368
 *
 */
class ChatGroupComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "ChatGroupComposer";
    private boolean mCancel = false;
    private Chat1To1BackupBuilder mChat1To1BackupBuilder;
    private ContentResolver mContentResolver;
    private HashMap<String, ArrayList<Integer>> mGroupNumberMap;
    private String mIpBackupFolder;
    private HashMap<String, ArrayList<Integer>> mChatGroupMap;

    ChatGroupComposer(String filePath, ContentResolver contentResolver) {
        mIpBackupFolder = filePath;
        mContentResolver = contentResolver;
        mChat1To1BackupBuilder = new Chat1To1BackupBuilder();
    }

    protected void setBackupParam(HashMap<String, ArrayList<Integer>> groupNumberMap,
            HashMap<String, ArrayList<Integer>> chatGroupMap) {
        mGroupNumberMap = groupNumberMap;
        mChatGroupMap = chatGroupMap;
    }

    /**
     * Backup all group chat message.
     * @return
     */
    protected int backupChatGroupMsg() {
        int result = CloudBrUtils.ResultCode.OK;

        Iterator it = mChatGroupMap.keySet().iterator();
        while (it.hasNext()) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backupChatGroupMsg() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String chatId = (String) it.next();
            if (chatId == null) {
                Log.e(CLASS_TAG, "backupChatGroupMsg() chatId = null, continue");
                continue;
            }
            Log.d(CLASS_TAG, "backupChatGroupMsg chatId = " + chatId);
            ArrayList<Integer> idChatList = mChatGroupMap.get(chatId);
            if (idChatList == null || idChatList.size() <= 0) {
                Log.e(CLASS_TAG, "backupChatGroupMsg() idChatList = null or size = 0, continue");
                continue;
            }

            result = backupOneChatMsg(chatId, idChatList);
            if (result != CloudBrUtils.ResultCode.OK) {
                Log.d(CLASS_TAG, "backupChatGroupMsg error. result = " + result);
                return result;
            }
        }

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        Log.d(CLASS_TAG, "group msg backup end");
        return CloudBrUtils.ResultCode.OK;
    }

    private final String[] chatProjection = new String[] { CloudBrUtils.Chat.CHAIRMAN,
            CloudBrUtils.Chat.TIMESTAMP, CloudBrUtils.Chat.SUBJECT,
            CloudBrUtils.Chat.CONVERSATION_ID, CloudBrUtils.Chat.PARTICIPANTS_LIST,
            CloudBrUtils.Chat.REJOIN_ID, CloudBrUtils.Chat.STATE, CloudBrUtils.CHAT_ID };

    /**
     * backup one group chat message. each 500 message wrap a file.
     * @param chatId
     *            This group chat id.
     * @param idChatList
     *            Every text message id contained in this group.
     * @return backup result.
     */
    protected int backupOneChatMsg(String chatId, ArrayList<Integer> idChatList) {
        // get Groupchat info to compose CPM header part.
        String selection = CloudBrUtils.CHAT_ID + " is \"" + chatId + "\"";
        Log.d(CLASS_TAG, "selection = " + selection);
        Cursor threadMapCursor = mContentResolver.query(CloudBrUtils.GROUP_CHAT_URI, null,
                selection, null, null);
        if (threadMapCursor == null || threadMapCursor.getCount() == 0) {
            Log.e(CLASS_TAG, "threadMapCursor is null or count is 0, skip this chat return ok, " +
                    "treadmap no this chat id info, no data chat id = " + chatId);
            return CloudBrUtils.ResultCode.OK;
        }
        threadMapCursor.moveToFirst();
        int localChatStatus = threadMapCursor.getInt(threadMapCursor.getColumnIndex(
                GroupChat.STATUS));
        if (threadMapCursor != null) {
            threadMapCursor.close();
        }

        Cursor chatCursor = mContentResolver.query(CloudBrUtils.CHAT_CHAT_URI, chatProjection,
                selection, null, null);
        if (chatCursor == null) {
            Log.e(CLASS_TAG, "backupData chatCursor is null,return false");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        chatCursor.moveToFirst();
        if (chatCursor.getCount() <= 0) {
            Log.d(CLASS_TAG, "chatCursor.getCount() <= 0");
            return CloudBrUtils.ResultCode.OK;
        }
        String participants = chatCursor.getString(chatCursor
                .getColumnIndex(CloudBrUtils.Chat.PARTICIPANTS_LIST));

        ChatRecord chatRecord = new ChatRecord();
        RootRecord rootRecord = new RootRecord();
        int result = EntryRecord.getChatInfo(chatCursor, chatRecord);
        chatRecord.setThreadMapStatus(localChatStatus);
        // get root info
        ArrayList<Integer> numberIdList = mGroupNumberMap.get(chatId);
        EntryRecord.getRootNumbersInfo(mContentResolver, numberIdList, rootRecord);
        rootRecord.setParticipants(participants);
        rootRecord.setSessionType(CloudBrUtils.FileTransferType.GROUP);
        String rootInfo = buildRootRecord(rootRecord);

        String cpmHander = buildChatGroupHeader(chatRecord);
        chatCursor.close();

        int index = 0;
        Writer chatWriter = null;
        File file = null;
        for (int id : idChatList) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backupData() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String rcsMsgSel = CloudBrUtils.ID + " = " + id;
            Log.d(CLASS_TAG, "backupOneChatMsg selection = " + selection);
            Cursor rcsMsgCursor = mContentResolver.query(CloudBrUtils.RCS_URI, null,
                    rcsMsgSel, null, null);
            rcsMsgCursor.moveToFirst();
            RcsMsgRecord rcsMsgRecord = null;

            int ipMsgId = rcsMsgCursor.getInt(rcsMsgCursor.getColumnIndex
                    (CloudBrUtils.RcsMessage.MESSAGE_COLUMN_IPMSG_ID));
            Cursor msgCursor = null;

            String msgSel = CloudBrUtils.ID + " = " + ipMsgId;
            msgCursor = mContentResolver
                    .query(CloudBrUtils.CHAT_CONTENT_URI, null, msgSel, null, null);
            if (msgCursor == null || msgCursor.getCount() <= 0) {
                Log.d(CLASS_TAG, "chat message table havnt this record ,smsSelection = " + msgSel);
                if (msgCursor != null) {
                    msgCursor.close();
                }
                if (rcsMsgCursor != null) {
                    rcsMsgCursor.close();
                }
                continue;
            }
            msgCursor.moveToFirst();

            // backup in one file 500 msg.
            if (index % 500 == 0) {
                if (index / 500 != 0) {
                    try {
                        chatWriter.write(CloudBrUtils.BackupConstant.BOUNDDARY_CPM_END);
                        chatWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return CloudBrUtils.ResultCode.IO_EXCEPTION;
                    }
                }

                file = new File(mIpBackupFolder + File.separator + chatId + index / 500
                        + "chat.txt");
                try {
                    file.createNewFile();
                    chatWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                            file)));
                    chatWriter.write(cpmHander);
                    chatWriter.write(rootInfo);
                } catch (IOException e) {
                    Log.e(CLASS_TAG, "create file fail chat id = " + chatId);
                    e.printStackTrace();
                    return CloudBrUtils.ResultCode.IO_EXCEPTION;
                }
            }
            rcsMsgRecord = new RcsMsgRecord();

            MessageRecord msgRecord = new MessageRecord();
            FileUtils.getRcsMessageInfo(rcsMsgCursor, rcsMsgRecord);
            FileUtils.getChatMessageInfo(msgCursor, msgRecord);
            int direction = rcsMsgRecord.getDirection();
            String contactNum = rcsMsgRecord.getContactNum();
            String from;
            String to;

            if (direction == CloudBrUtils.Message.Direction.INCOMING) {
                from = contactNum;
                to = CloudBrUtils.BackupConstant.ANONYMOUS;
            } else if (direction == CloudBrUtils.Message.Direction.OUTGOING) {
                from = CloudBrUtils.getMyNumber();
                to = CloudBrUtils.BackupConstant.ANONYMOUS;
            } else {
                Log.d(CLASS_TAG, "Irrelevant or not applicable (e.g. for a system message)");
                from = contactNum;
                to = CloudBrUtils.BackupConstant.ANONYMOUS;
            }
            rcsMsgRecord.setFrom(from);
            rcsMsgRecord.setTo(to);
            String msgBody = mChat1To1BackupBuilder.buildOneChatMsg(msgRecord, rcsMsgRecord);

            String msgHeader = buildEachMsgHeader();
            try {
                chatWriter.write(msgHeader);
                chatWriter.write(msgBody);
            } catch (IOException e) {
                e.printStackTrace();
                return CloudBrUtils.ResultCode.IO_EXCEPTION;
            }

            if (rcsMsgCursor != null) {
                rcsMsgCursor.close();
            }
            if (msgCursor != null) {
                msgCursor.close();
            }
            index++;
        }
        if (chatWriter != null) {
            try {
                chatWriter.write(CloudBrUtils.BackupConstant.BOUNDDARY_CPM_END);
                chatWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
                return CloudBrUtils.ResultCode.IO_EXCEPTION;
            }
        }
        Log.d(CLASS_TAG, "backupGroupMsg idChatList size = " + idChatList.size());
        Log.d(CLASS_TAG, "backupGroupMsg chatGroupBackupCount = " + index);
        return CloudBrUtils.ResultCode.OK;
    }

    // private boolean isSystemMsg(Cursor msgCs) {
    // int direction = msgCs.getInt(msgCs.getColumnIndex(Message.DIRECTION));
    // if (direction == Message.Direction.IRRELEVANT) {
    // Log.d(CLASS_TAG, "this is a system msg, backup");
    // return true;
    // }
    // return false;
    // }

    private String buildEachMsgHeader() {
        StringBuilder header = new StringBuilder();
        header.append(BackupConstant.LINE_BREAK);
        header.append(BackupConstant.BOUNDDARY_CPM);
        header.append(BackupConstant.LINE_BREAK);
        header.append("Content-type: Message/CPIM");
        header.append(BackupConstant.LINE_BREAK);
        return header.toString();
    }

    private static String buildChatGroupHeader(ChatRecord chatRecord) {
        StringBuilder header = new StringBuilder();
        header.append(BackupConstant.FROM + " ");
        header.append(chatRecord.getRejoinId());
        header.append(BackupConstant.LINE_BREAK);

        String subject = chatRecord.getSubject();
        if (subject != null) {
            header.append(BackupConstant.SUBJECT + " ");
            header.append(subject);
            header.append(BackupConstant.LINE_BREAK);
        }

        header.append(BackupConstant.CONVERST_ID + " ");
        header.append(chatRecord.getConversionId());
        header.append(BackupConstant.LINE_BREAK);

        header.append(BackupConstant.MTK_CHAT_ID + " ");
        header.append(chatRecord.getChatId());
        header.append(BackupConstant.LINE_BREAK);

        header.append(BackupConstant.MTK_CHAT_STATE + " ");
        header.append(chatRecord.getState());
        header.append(BackupConstant.LINE_BREAK);

        header.append(BackupConstant.MTK_CHAT_STATUS + " ");
        header.append(chatRecord.getThreadMapStatus());
        header.append(BackupConstant.LINE_BREAK);

        header.append(BackupConstant.MTK_CHAT_CHAIRMAN + " ");
        header.append(chatRecord.getChairman());
        header.append(BackupConstant.LINE_BREAK);
        header.append(BackupConstant.LINE_BREAK);

        header.append(BackupConstant.DATE + " ");
        header.append(FileUtils.encodeDate(chatRecord.getTimeStamp()));
        header.append(BackupConstant.LINE_BREAK);

        header.append("Content-type: multipart/related;boundary=cpm;type=\"Application/X-CPM-Session\"");

        header.append(BackupConstant.BOUNDDARY);
        header.append(BackupConstant.CONTENT_TYPE + " ");
        header.append("Application/X-CPM-Session");
        header.append(BackupConstant.LINE_BREAK);
        header.append(BackupConstant.LINE_BREAK);
        return header.toString();
    }

    protected String buildRootRecord(RootRecord rootRecord) {
        Log.d(CLASS_TAG, "buildRootRecord begin");
        String resultStr = null;
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter mStringWriter = new StringWriter();
        try {
            serializer.setOutput(mStringWriter);
            serializer.startTag(null, BackupConstant.SESSION_TYPE);
            serializer.text(rootRecord.getSessionType());
            serializer.endTag(null, BackupConstant.SESSION_TYPE);

            serializer.startTag(null, BackupConstant.PARTICIPANTS);
            serializer.text(rootRecord.getParticipants());
            serializer.endTag(null, BackupConstant.PARTICIPANTS);

            ArrayList<GroupNumberRecord> numbersInfo = rootRecord.getNumberInfo();
            if (numbersInfo == null || numbersInfo.size() <= 0) {
                Log.d(CLASS_TAG, "buildRootRecord no membertable info.");
                Log.d(CLASS_TAG, mStringWriter.toString());
                serializer.endDocument();
                resultStr = mStringWriter.toString() + BackupConstant.LINE_BREAK;
                mStringWriter.close();
                return resultStr;
            }

            for (GroupNumberRecord record : numbersInfo) {
                String number = record.getNumber();
                String state = Integer.toString(record.getState());
                String name = record.getName();
                if (number != null) {
                    serializer.startTag(null, BackupConstant.MTK_MEMBER_INFO);
                    serializer.startTag(null, BackupConstant.MTK_MEMBER_NO);
                    serializer.text(number);
                    serializer.endTag(null, BackupConstant.MTK_MEMBER_NO);

                    serializer.startTag(null, BackupConstant.MTK_MEMBER_STATE);
                    serializer.text(state);
                    serializer.endTag(null, BackupConstant.MTK_MEMBER_STATE);

                    if (name != null) {
                        serializer.startTag(null, BackupConstant.MTK_MEMBER_NAME);
                        serializer.text(name);
                        serializer.endTag(null, BackupConstant.MTK_MEMBER_NAME);
                    }
                    serializer.endTag(null, BackupConstant.MTK_MEMBER_INFO);
                }
            }
            serializer.endDocument();
            resultStr = mStringWriter.toString() + BackupConstant.LINE_BREAK;
            mStringWriter.close();
        } catch (IllegalArgumentException e) {
            Log.e(CLASS_TAG, "IllegalArgumentException");
            e.printStackTrace();
            resultStr = null;
        } catch (IllegalStateException e) {
            Log.e(CLASS_TAG, "IllegalStateException");
            e.printStackTrace();
            resultStr = null;
        } catch (IOException e) {
            Log.e(CLASS_TAG, "IOException");
            e.printStackTrace();
            resultStr = null;
        }
        Log.d(CLASS_TAG, mStringWriter.toString());
        return resultStr;
    }

    /**
     * This method will be called when backup service be cancel.
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
