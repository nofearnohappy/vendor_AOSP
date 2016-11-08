package com.mediatek.rcs.message.cloudbackup.modules;

import android.util.Log;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

/**
 * Parse a 1 to 1 backup file.
 * @author mtk81368
 *
 */
public class Chat1To1RestoreParser {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "Chat1To1RestoreParser";

    private MessageRecord mMessageRecord;
    private RcsMsgRecord mRcsMsgRecord;

    protected void setRcsMsgRecord(RcsMsgRecord rcsMsgRecord) {
        this.mRcsMsgRecord = rcsMsgRecord;
    }

    protected void setMessageRecord(MessageRecord messageRecord) {
        this.mMessageRecord = messageRecord;
    }

    protected int parseOneToOneMsg(File file) {
        if (file == null || !file.exists()) {
            Log.d(CLASS_TAG, "parseOneToOneMsg file error. return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        Log.d(CLASS_TAG, "parseOneToOneMsg begin, filename = " + file.getName());
        InputStream instream = null;
        try {
            instream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        InputStreamReader inreader = new InputStreamReader(instream);
        BufferedReader buffreader = new BufferedReader(inreader);
        int result = parseMessageBodyImple(null, 0, buffreader);
        if (buffreader != null) {
            try {
                buffreader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected int persistMessageBody(String chatId, long threadId, String msgContent) {
        Log.d(CLASS_TAG, "persistMessageBody begin, msgContent = " + msgContent);
        StringReader sr = new StringReader(msgContent);
        BufferedReader buffreader = new BufferedReader(sr);
        int result = parseMessageBodyImple(chatId, threadId, buffreader);
        if (buffreader != null) {
            try {
                buffreader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private int parseMessageBodyImple(String chatId, long threadId, BufferedReader buffreader) {
        Log.d(CLASS_TAG, "parseMessageBodyImple begin");
        String line = null;
        String content = null;

        String from = null;
        String to = null;
        String contentType = null;
        try {
            while ((line = buffreader.readLine()) != null) {
                if (line.startsWith(BackupConstant.FROM)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    from = content;
                    mRcsMsgRecord.setFrom(content);
                    Log.d(CLASS_TAG, "startsWith(FROM) = " + content);
                    continue;
                }

                if (line.startsWith(BackupConstant.TO)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    to = content;
                    mRcsMsgRecord.setTo(content);
                    Log.d(CLASS_TAG, "startsWith(TO)");
                    continue;
                }

                if (line.startsWith(BackupConstant.DATE_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "startsWith(DATE_TIME) = " + content);
                    mRcsMsgRecord.setTimestamp(FileUtils.decodeDate(content));
                    continue;
                }

                if (line.startsWith(BackupConstant.DATE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "startsWith(DATE) = " + content);
                    mRcsMsgRecord.setTimestamp(FileUtils.decodeDate(content));
                    continue;
                }

                if ((line.startsWith("Content-type") || line.startsWith("Content-Type"))
                        && !line.contains(BackupConstant.CPIM_MESSAGE_TYPE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    contentType = content;
                    mRcsMsgRecord.setMimeType(content);
                    Log.d(CLASS_TAG, "startsWith(CONTENT_TYPE) = " + content);
                    break;
                }

                if (line.startsWith(BackupConstant.CONVERST_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "startsWith(CONVERST_ID) = " + content);
                    mMessageRecord.setConversationId(content);
                    continue;
                }

                if (line.startsWith(BackupConstant.IMDN_MSG_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "startsWith(BackupConstant.IMDN_MSG_ID) = " + content);
                    mMessageRecord.setMsgId(content);
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_SEND_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "startsWith(MTK_SEND_TIME) = " + content);
                    mRcsMsgRecord.setDataSent((int) FileUtils.decodeDate(content));
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_DELIVERED_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "MTK_DELIVERED_TIME =" + content);
                    mMessageRecord.setDeliveredTimestamp(FileUtils.decodeDate(content));
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_DISPLAYED_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "MTK_DISPLAYED_TIME = " + content);
                    mMessageRecord.setDisplayTimestamp(FileUtils.decodeDate(content));
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_DIRECTION)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "MTK_DIRECTIONE = " + content);
                    mRcsMsgRecord.setDirection(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "MTK_DIRECTIONE = " + Integer.parseInt(content));
                    continue;
                }

                //chat db message talbe status
                if (line.startsWith(BackupConstant.MTK_STATUS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "chat db message talbe Message.MESSAGE_STATUS = " + content);
                    if (content != null) {
                        mMessageRecord.setStatus(Integer.parseInt(content));
                    }
                    continue;
                }
                //rcs message table status
                if (line.startsWith(BackupConstant.MTK_MSG_STATUS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "rcsmessage status Message.MTK_MSG_STATUS = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setStatus(Integer.parseInt(content));
                    }
                    continue;
                }

               //chat db message talbe type
                if (line.startsWith(BackupConstant.MTK_CHAT_TYPE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "chat db message talbe type MTK_MSG_TYPE = " + content);
                    if (content != null) {
                        mMessageRecord.setType(Integer.parseInt(content));
                    }
                    continue;
                }

              //rcs message table msg_type
                if (line.startsWith(BackupConstant.MTK_TYPE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "rcsmessage table msg_type MTK_MSG_TYPE = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setType(Integer.parseInt(content));
                    }
                    continue;
                }

                //rcs message table class
                if (line.startsWith(BackupConstant.MTK_CLASS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "rcsmessage CHATMESSAGE_TYPE MTK_MSG_TYPE = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setMsgClass(Integer.parseInt(content));
                    }
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_SUB_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, " sub id = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setSubID(Integer.parseInt(content));
                    }
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_LOCK)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, " lock = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setLocked(Integer.parseInt(content));
                    }
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_SEEN)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, " MTK_SEEN = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setSeen(Integer.parseInt(content));
                    }
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_FAV_FLAG)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, " MTK_FAV_FLAG = " + content);
                    if (content != null) {
                        mRcsMsgRecord.setFlag(Integer.parseInt(content));
                    }
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        // assign value to direction
        if (!mRcsMsgRecord.isIsHasDirection()) {
            if (mRcsMsgRecord.getFrom().equals(CloudBrUtils.getMyNumber())) {
                mRcsMsgRecord.setDirection(CloudBrUtils.Message.Direction.OUTGOING);
            } else {
                mRcsMsgRecord.setDirection(CloudBrUtils.Message.Direction.INCOMING);
            }
        }

        if (from == null || to == null) {
            Log.e(CLASS_TAG, "this msg no from no to info, return error");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }

        if (chatId != null) {
            Log.d(CLASS_TAG, "chat id fill with contactnumber");
            mRcsMsgRecord.setChatId(chatId);
        }

        mRcsMsgRecord.setConversation((int) threadId);

        // sms table column need set default value if the backup file haven't
        // contain the info.

        Log.d(CLASS_TAG, "persist string finish, begine insert db");

        StringBuilder msgBody = new StringBuilder();
        try {
            while ((line = buffreader.readLine()) != null) {
                if (!line.isEmpty()) {
                    msgBody.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        if (contentType != null) {
            int result = parserMsgBody(contentType, msgBody.toString());
            if (result != CloudBrUtils.ResultCode.OK) {
                Log.e(CLASS_TAG, "parserMsgBody error, result = " + result);
                return result;
            }
        } else {
            Log.d(CLASS_TAG, "this file has not content type, return");
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        return CloudBrUtils.ResultCode.OK;
    }

    private int parserMsgBody(String contentType, String content) {
        if (contentType.endsWith(CloudBrUtils.ContentType.TEXT_TYPE)) {
            return parseTextMsg(content);
        } else if (contentType.endsWith(CloudBrUtils.ContentType.VEMOTION_TYPE)) {
            mRcsMsgRecord.setBody(content);
            mMessageRecord.setBody(content.getBytes());
        } else if (contentType.endsWith(CloudBrUtils.ContentType.CLOUDFILE_TYPE)) {
            mRcsMsgRecord.setBody(content);
            mMessageRecord.setBody(content.getBytes());
        } else {
            Log.e(CLASS_TAG, "parserMsgBody contentType is error " + contentType);
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        return CloudBrUtils.ResultCode.OK;
    }

    private int parseTextMsg(String msgBody) {
        Log.d(CLASS_TAG, "parseTextMsg msgbody = " + msgBody);
        mRcsMsgRecord.setBody(msgBody.toString());
        mMessageRecord.setBody(msgBody.toString().getBytes());
        return CloudBrUtils.ResultCode.OK;
    }
}
