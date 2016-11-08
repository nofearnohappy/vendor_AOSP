package com.mediatek.rcs.message.cloudbackup.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupDataFileType;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Chat;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.GroupChatMember;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Message;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.RcsMessage;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Vmsg;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FavoriteRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.GroupNumberRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import android.provider.Telephony.Threads;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;
import android.util.Xml;

public class FileUtils {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FileUtils";

    public class ModulePath {
        public static final String FOLDER_MMS = "Mms";
        public static final String FOLDER_SMS = "Sms";
        public static final String FOLDER_IPMSG = "IpMessage";
        public static final String FOLDER_FAVORITE = "Favorite";
        public static final String FILE_EXT_PDU = ".pdu";

        public static final String SMS_VMSG = "sms.vmsg";
        public static final String MMS_XML = "mms_backup.xml";
        public static final String FT_FILE_PATHRECEIVE = "/storage/emulated/0/joyn/";
        public static final String FT_FILE_PATHSEND = "/storage/emulated/0/.Rcse/";
        public static final String BACKUP_DATA_FOLDER = "/storage/sdcard0/cloud/";
        public static final String RESTORE_BACKUP_FOLDER = "/storage/sdcard0/cloudTemp/";
        public static final String FAVORITE_FOLDER = "/storage/sdcard0/cloudFav/";
    }

    public static final int NUMBER_IMPORT_MMS_EACH = 10;
    public static final int NUMBER_IMPORT_SMS_EACH = 40;

    public static final String MESSAGE_BOX_TYPE_INBOX = "1";
    public static final String MESSAGE_BOX_TYPE_SENT = "2";
    public static final String MESSAGE_BOX_TYPE_DRAFT = "3";
    public static final String MESSAGE_BOX_TYPE_OUTBOX = "4";

//    public static final int CHAT_MESSAGE = 0x02;
//    public static final int MMS = 0x04;
//    public static final int SMS = 0x08;

    /**
     * copy pdu file.
     *
     * @param srcPduPath
     * @param desPduPath
     */
    public static void copyPduFile(String srcPduPath, String desPduPath) {
        File desPduFile = new File(desPduPath);
        try {
            desPduFile.createNewFile();
            FileInputStream fis = new FileInputStream(new File(srcPduPath));
            FileOutputStream fos = new FileOutputStream(desPduFile);
            byte[] pduBuff = new byte[2048];

            int len = 0;
            while ((len = fis.read(pduBuff)) > 0) {
                fos.write(pduBuff, 0, len);
            }

            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * create folders with the given folder path.
     * Delete the original folder if the folders has existed before create.
     * @param folderPath
     * @return
     */
    public static boolean createFolder(String folderPath) {
        Log.d(CLASS_TAG, "createFolder folderPath = " + folderPath);
        if (folderPath == null) {
            return false;
        }
        File folder = new File(folderPath);
        if (folder != null && folder.exists()) {
             boolean deleteResult = deleteFileOrFolder(folder);
             if (!deleteResult) {
                 Log.e(CLASS_TAG, "createFolder, deleteResult = " + deleteResult);
                 return false;
             }
        }
        Log.d(CLASS_TAG, "createFolder folder.mkdirs");
        return folder.mkdirs();
    }

    /**
     * get file type from file name.
     * @param file
     * @return
     */
    public static int anysisFileType(File file) {
        Log.d(CLASS_TAG, "anysisFileType");
        InputStream instream = null;
        try {
            instream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        Log.d(CLASS_TAG, "anysisFileType file = " + file.getAbsolutePath());
        InputStreamReader inreader = new InputStreamReader(instream);
        BufferedReader buffreader = new BufferedReader(inreader);
        String line = null;

        try {
            while (((line = buffreader.readLine()) != null)) {
                if (!line.isEmpty()) {
                    Log.d(CLASS_TAG, "!line.isEmpty( = " + line);
                    break;
                }
                Log.d(CLASS_TAG, "line = " + line);
            }
            if (buffreader != null) {
                buffreader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (line != null) {
            Log.d(CLASS_TAG, " the first not empty line = " + line);
            if (line.contains(BackupConstant.VMSG)) {
                Log.d(CLASS_TAG, "this file is a vmsg");
                return BackupDataFileType.VMSG;
            } else if (line.contains("From") || line.contains("from")) {
                Log.d(CLASS_TAG, "this file is a ipmsg");
                return BackupDataFileType.IPMSG;
            } else if (line.contains("xml version") && line.contains("standalone")) {
                Log.d(CLASS_TAG, "this file is a MMS XML");
                return BackupDataFileType.MMS_XML;
            } else {
                Log.d(CLASS_TAG, "this file is a pdu");
                return BackupDataFileType.PDU;
            }
        }
        return -1;
    }

    /**
     * persist root info from group msg(ft and ip text msg) backup data.
     * @param
     * @return
     */
    public static int persistRootData(String content, RootRecord mRootRecord) {
        if (mRootRecord == null) {
            Log.e(CLASS_TAG, "persistRootData mRootRecord = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        StringBufferInputStream is = new StringBufferInputStream(content);
        String participants = null;
        String fileTransferType = null; // ft msg use.
        String sessionType = null; // chat group msg use only.
        String memberNo = null;
        String name = null;
        String stateStr = null;

        ArrayList<GroupNumberRecord> groupNumbers = null;
        GroupNumberRecord gnr = null;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(is, "UTF-8");
            int eventCode;
            eventCode = parser.getEventType();
            while (eventCode != XmlPullParser.END_DOCUMENT) {
                switch (eventCode) {
                case XmlPullParser.START_DOCUMENT:
                    Log.d(CLASS_TAG, "stat document");
                    break;

                case XmlPullParser.START_TAG:
                    Log.d(CLASS_TAG, "XmlPullParser.START_TAG");
                    if (BackupConstant.FILE_TRANSFER_TYPE.equals(parser.getName())) {
                        fileTransferType = parser.nextText();
                        Log.d(CLASS_TAG, " FILE_TRANSFER_TYPE = " + fileTransferType);
                        parser.next();
                    } else if (BackupConstant.SESSION_TYPE.equals(parser.getName())) {
                        sessionType = parser.nextText();
                        Log.d(CLASS_TAG, " SESSION_TYPE = " + sessionType);
                        parser.next();
                    } else if (BackupConstant.PARTICIPANTS.equals(parser.getName())) {
                        participants = parser.nextText();
                        Log.d(CLASS_TAG, " PARTICIPANTS = " + participants);
                        if (parser.next() == XmlPullParser.END_DOCUMENT) {
                            Log.d(CLASS_TAG, "end document, break");
                            break;
                        }
                    } else if (BackupConstant.MTK_MEMBER_INFO.equals(parser.getName())) {
                        if (parser.next() == XmlPullParser.END_DOCUMENT) {
                            Log.d(CLASS_TAG, "end document, break");
                            break;
                        } else {
                            if (groupNumbers == null) {
                                groupNumbers = new ArrayList<GroupNumberRecord>();
                            }
                            gnr = new GroupNumberRecord();
                        }
                    } else if (BackupConstant.MTK_MEMBER_NO.equals(parser.getName())) {
                        memberNo = parser.nextText();
                        Log.d(CLASS_TAG, "MTK_MEMBER_NO memberNo = " + memberNo);
                        parser.next();
                    } else if (BackupConstant.MTK_MEMBER_STATE.equals(parser.getName())) {
                        stateStr = parser.nextText();
                        Log.d(CLASS_TAG, "MTK_MEMBER_STATE = " + stateStr);
                        parser.next();
                    } else if (BackupConstant.MTK_MEMBER_NAME.equals(parser.getName())) {
                        name = parser.nextText();
                        Log.d(CLASS_TAG, "MTK_MEMBER_NAME name = " + name);
                        parser.next();
                        gnr.setNumber(memberNo);
                        gnr.setName(name);
                        gnr.setState(Integer.parseInt(stateStr));
                        groupNumbers.add(gnr);
                        gnr = null;
                    }
                    continue;

                case XmlPullParser.END_TAG:
                    Log.d(CLASS_TAG, "XmlPullParser.END_TAG");
                    if (parser.next() == XmlPullParser.END_DOCUMENT) {
                        Log.d(CLASS_TAG, "end document, break");
                        break;
                    }

                case XmlPullParser.END_DOCUMENT:
                    Log.d(CLASS_TAG, "end document, break");
                    break;
                }
                eventCode = parser.next();
            }
            is.close();
        } catch (XmlPullParserException e) {
            Log.e(CLASS_TAG, "XmlPullParserException");
            e.printStackTrace();
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        } catch (IOException e) {
            Log.e(CLASS_TAG, "IOException");
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }
        if (participants != null) {
            mRootRecord.setParticipants(participants);
        }
        if (fileTransferType != null) {
            mRootRecord.setSessionType(fileTransferType);
        }
        if (sessionType != null) {
            mRootRecord.setSessionType(sessionType);
        }
        if (groupNumbers != null) {
            mRootRecord.setNumberInfo(groupNumbers);
        }
        Log.d(CLASS_TAG, "persistRootData end");
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * insert chat group munbers info to groupnumber table of rcsmessage databases.
     * @param mChatRecord
     * @param mRootRecord
     * @param mContentResolver
     * @return
     */
    public static int insertgroupNumberRecord(ChatRecord mChatRecord, RootRecord mRootRecord,
            ContentResolver mContentResolver) {
        String chatId = mChatRecord.getChatId();
        String conversitonId = mChatRecord.getConversionId();
        ArrayList<GroupNumberRecord> numberInfos = mRootRecord.getNumberInfo();
        if (numberInfos == null) {
            Log.d(CLASS_TAG, "insertgroupNumberRecord no data");
            return CloudBrUtils.ResultCode.OK;
        }
        for (GroupNumberRecord numberInfo : numberInfos) {
            ContentValues groupNumberCv = new ContentValues();
            groupNumberCv.put(GroupChatMember.COLUMN_CHAT_ID, chatId);
            groupNumberCv.put(GroupChatMember.COLUMN_CONTACT_NAME, numberInfo.getName());
            groupNumberCv.put(GroupChatMember.COLUMN_CONTACT_NUMBER, numberInfo.getNumber());
            groupNumberCv.put(GroupChatMember.COLUMN_STATE, numberInfo.getState());
            Uri uri = mContentResolver.insert(CloudBrUtils.GROUP_MEMBER_URI, groupNumberCv);
            if (uri == null) {
                Log.d(CLASS_TAG, "iinsertgroupNumberRecord error, return");
                return CloudBrUtils.ResultCode.DB_EXCEPTION;
            }
        }
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * insert chatRecord to chat table of chat.db.
     *
     * @param mChatRecord
     * @param mContentResolver
     * @return
     */
    public static int insertChatRecord(ChatRecord mChatRecord, ContentResolver mContentResolver) {
        ContentValues chatCv = new ContentValues();
        chatCv.put(Chat.CHAT_ID, mChatRecord.getChatId());
        chatCv.put(Chat.CHAIRMAN, mChatRecord.getChairman());
        chatCv.put(Chat.CONVERSATION_ID, mChatRecord.getConversionId());
        chatCv.put(Chat.DIRECTION, mChatRecord.getDirection());
        chatCv.put(Chat.PARTICIPANTS_LIST, mChatRecord.getParticipants());
        chatCv.put(Chat.REJOIN_ID, mChatRecord.getRejoinId());
        chatCv.put(Chat.STATE, mChatRecord.getState());
        chatCv.put(Chat.SUBJECT, mChatRecord.getSubject());
        chatCv.put(Chat.TIMESTAMP, mChatRecord.getTimeStamp());
        Uri uri = mContentResolver.insert(CloudBrUtils.CHAT_CHAT_URI, chatCv);
        if (uri == null) {
            Log.d(CLASS_TAG, "insertChatDb error, return");
            return CloudBrUtils.ResultCode.DB_EXCEPTION;
        }
        return CloudBrUtils.ResultCode.OK;
    }

    /**
     * insert favoriteRecord info into favorite table of rcsmessage databases.
     * @param mContentResolver
     * @param favoriteRecord
     * @return
     */
    public static Uri insertFavoriteDb(ContentResolver mContentResolver,
            FavoriteRecord favoriteRecord) {
        ContentValues favCv = new ContentValues();
        favCv.put(Favorite.COLUMN_CONTACT_NUB, favoriteRecord.getContactNum());
        favCv.put(Favorite.COLUMN_BODY, favoriteRecord.getBody());
        favCv.put(Favorite.COLUMN_DA_MIME_TYPE, favoriteRecord.getMimeType());
        favCv.put(Favorite.COLUMN_DATE, favoriteRecord.getDate());
        favCv.put(Favorite.COLUMN_PATH, favoriteRecord.getPath());
        favCv.put(Favorite.COLUMN_SIZE, favoriteRecord.getSize());
        favCv.put(Favorite.COLUMN_DA_TYPE, favoriteRecord.getType());
        favCv.put(Favorite.COLUMN_CHATID, favoriteRecord.getChatId());
        favCv.put(Favorite.COLUMN_DA_ID, favoriteRecord.getMsgId());
        favCv.put(Favorite.COLUMN_DA_TIMESTAMP, favoriteRecord.getDataSent());
        favCv.put(Favorite.COLUMN_DA_MESSAGE_STATUS, favoriteRecord.getStatus());
        favCv.put(Favorite.COLUMN_DA_DIRECTION, favoriteRecord.getDirection());
        favCv.put(Favorite.COLUMN_DA_FLAG, favoriteRecord.getFlag());
        favCv.put(Favorite.COLUMN_DA_ICON, favoriteRecord.getIcon());

        Uri uri = mContentResolver.insert(CloudBrUtils.FAVOTIRE_URI, favCv);
        return uri;
    }

    /**
     * get the ft message file path of favorite.
     * @return
     */
    public static String getFavFtFilePath(Context context) {
        String path = RcsMessageUtils.getFavoritePath(context, "favorite_message");
        return path;
    }

    /**
     * create a threadID for group chat when restore data to database.
     * @param chatRecord
     * @return
     */
    public static long createGroupThread(Context context, ChatRecord chatRecord) {
        Log.d(CLASS_TAG, "createGroupThread(ChatRecord chatRecord)");
        String chatId = chatRecord.getChatId();
        String rejoinId = chatRecord.getRejoinId();
        List<Participant> participants = new ArrayList<Participant>();
        String numbers = chatRecord.getParticipants();
        Log.d(CLASS_TAG , "createGroupThread numbers = " + numbers);
        if (numbers != null) {
            String[] numbersArray = numbers.split(",");
            if (numbersArray != null) {
                for (int index = 0; index < numbersArray.length; index++) {
                    participants.add(new Participant(numbersArray[index], null));
                }
            }
        }

        String subject = chatRecord.getSubject();
        String chairmen = chatRecord.getChairman();
        long timestamp = chatRecord.getTimeStamp();
        int status = chatRecord.getThreadMapStatus();
        Log.d(CLASS_TAG, "createGroupThread status = " + status);
        Log.d(CLASS_TAG, "createGroupThread chatid = " + chatId);
        Log.d(CLASS_TAG, "createGroupThread rejoinId = " + rejoinId);
        // insert record to threadMap database and rejoin database.
        long threadId = RCSDataBaseUtils.createGroupThreadId(context, chatId, rejoinId,
                participants, subject, chairmen, timestamp, status);
        return threadId;
    }

    /**
     * get one to one and one to Many message threadId.
     */
    public static long getOrCreate1TNThreadId(Context context, String contactNumber) {
        Log.d(CLASS_TAG, "getOrCreate1TNThreadId contactNumber = " + contactNumber);
        String[] contactsArray = contactNumber.split(";");
        Set<String> recipients = null;
        if (contactsArray != null && contactsArray.length > 0) {
            Log.d(CLASS_TAG, "getOrCreate1TNThreadId contactsArray LENGHT = " + contactsArray.length);
            recipients = new HashSet<String>();
            for(int index = 0; index < contactsArray.length; index++) {
                recipients.add(contactsArray[index]);
            }
        }
        long threadId = 0;
        if (recipients != null) {
            threadId = Threads.getOrCreateThreadId(context, recipients);
        }
        return threadId;
    }
    /**
     * delete file or folder.
     * @param file
     * @return
     */
    public static boolean deleteFileOrFolder(File file) {
        boolean result = true;
        if (file == null || !file.exists()) {
            return result;
        }
        if (file.isFile()) {
            return file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!deleteFileOrFolder(f)) {
                        result = false;
                    }
                }
            }
            if (!file.delete()) {
                result = false;
            }
        }
        return result;
    }

    /**
    * Encode a long date to string value in Z format (see RFC 3339)
    *
    * @param date Date in milliseconds
    * @return String
    */
    public static String encodeDate(long date) {
        Time t = new Time(TimeZone.getTimeZone("UTC").getID());
        t.set(date);
        return t.format3339(false);
    }

    /**
     * Converte string date to long.
     * @param date
     * @return
     */
    public static long decodeDate(String date) {
        Time t = new Time(TimeZone.getTimeZone("UTC").getID());
        t.parse3339(date);
        return t.toMillis(true);
    }

    public static boolean isEmptyFolder(File folderName) {
        boolean ret = true;

        if (folderName != null && folderName.exists()) {
            if (folderName.isFile()) {
                ret = false;
            } else {
                File[] files = folderName.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!isEmptyFolder(file)) {
                            ret = false;
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public static String combineVmsg(String timeStamp, String readByte, String boxType,
            String mSlotid, String locked, String smsAddress, String body, String mseen) {
        StringBuilder mBuilder = new StringBuilder();
        mBuilder.append(Vmsg.BEGIN_VMSG);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.VERSION);
        mBuilder.append("1.1");
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.BEGIN_VCARD);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.FROMTEL);
        mBuilder.append(smsAddress);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.END_VCARD);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.BEGIN_VBODY);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.XBOX);
        mBuilder.append(boxType);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.XREAD);
        mBuilder.append(readByte);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.XSEEN);
        mBuilder.append(mseen);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.XSIMID);
        mBuilder.append(mSlotid);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.XLOCKED);
        mBuilder.append(locked);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.XTYPE);
        mBuilder.append("SMS");
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.DATE);
        mBuilder.append(timeStamp);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.SUBJECT);
        mBuilder.append(Vmsg.ENCODING);
        mBuilder.append(Vmsg.QUOTED);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_SEMICOLON);
        mBuilder.append(Vmsg.CHARSET);
        mBuilder.append(Vmsg.UTF);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_COLON);
        mBuilder.append(body);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.END_VBODY);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);
        mBuilder.append(Vmsg.END_VMSG);
        mBuilder.append(Vmsg.VMESSAGE_END_OF_LINE);

        return mBuilder.toString();
    }

    /**
     **get message info from rcsmessage table.
     * @param rcsMsgCursor cursor of rcs message table
     * @param rcsMsgRecord
     * @return
     */
    public static int getRcsMessageInfo(Cursor rcsMsgCursor,
            RcsMsgRecord rcsMsgRecord) {
        rcsMsgRecord.setBody(rcsMsgCursor.getString(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_BODY)));
        rcsMsgRecord.setContactNum(rcsMsgCursor.getString(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_CONTACT_NUMBER)));
        rcsMsgRecord.setDataSent(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_DATE_SENT)));
        rcsMsgRecord.setDirection(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_DIRECTION)));
        rcsMsgRecord.setFilePath(rcsMsgCursor.getString(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_FILE_PATH)));
        rcsMsgRecord.setFlag(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_FLAG)));
        rcsMsgRecord.setIsBlocked(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_ISBLOCKED)));
        rcsMsgRecord.setLocked(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_LOCKED)));
        rcsMsgRecord.setMimeType(rcsMsgCursor.getString(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_MIME_TYPE)));
        rcsMsgRecord.setMsgClass(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_MSG_CLASS)));
        rcsMsgRecord.setSeen(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_SEEN)));
        rcsMsgRecord.setStatus(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_MESSAGE_STATUS)));
        rcsMsgRecord.setSubID(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_SUB_ID)));
        rcsMsgRecord.setTimestamp(rcsMsgCursor.getLong(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_TIMESTAMP)));
        rcsMsgRecord.setType(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_TYPE)));
        rcsMsgRecord.setIpmsgId(rcsMsgCursor.getInt(rcsMsgCursor
                .getColumnIndex(RcsMessage.MESSAGE_COLUMN_IPMSG_ID)));
        return CloudBrUtils.ResultCode.OK;
    }

    public static void getChatMessageInfo(Cursor msgCs,
            MessageRecord msgRecord) {
        msgRecord.setStatus(msgCs.getInt(msgCs.getColumnIndex(Message.MESSAGE_STATUS)));
        msgRecord.setType(msgCs.getInt(msgCs.getColumnIndex(Message.MESSAGE_TYPE)));
        msgRecord.setConversationId(msgCs.getString(msgCs.getColumnIndex(Message.CONVERSATION_ID)));
    }

    public static boolean isNeedBackup(int msgStatus, int isBlock, int msgClass) {
        if ((msgStatus == RcsLog.MessageStatus.READ
                || msgStatus == RcsLog.MessageStatus.UNREAD
                || msgStatus == RcsLog.MessageStatus.SENT
                || msgStatus == RcsLog.MessageStatus.DELIVERED) &&
                isBlock != 1 &&
                (msgClass == RcsLog.Class.NORMAL
                || msgClass == RcsLog.Class.EMOTICON
                || msgClass == RcsLog.Class.CLOUD)
                 ) {//block is 1 means this msg is spam
                    return true;
        }
        return false;
    }
}
