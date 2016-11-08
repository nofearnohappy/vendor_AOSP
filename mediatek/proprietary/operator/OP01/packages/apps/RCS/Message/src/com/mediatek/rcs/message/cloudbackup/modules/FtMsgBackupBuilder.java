package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.xmlpull.v1.XmlSerializer;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ContentType;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ResultCode;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FileObject;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FtRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.GroupNumberRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;

import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.Xml;

class FtMsgBackupBuilder {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FtMsgBackupBuilder";
    private String mIpBackupFolder;

    FtMsgBackupBuilder(String filePath) {
        mIpBackupFolder = filePath;
    }

    protected String buildGroupHeader(ChatRecord chatRecord, FtRecord ftRecord,
            RcsMsgRecord rcsMsgRecord) {
        StringBuilder header = new StringBuilder();
        header.append(BackupConstant.FROM + " ");
        header.append(rcsMsgRecord.getFrom());
        header.append(BackupConstant.LINE_BREAK);

        String chatInfo = buildChatInfo(chatRecord);
        header.append(chatInfo);
        String ftInfo = buildOneFtMsgInfo(ftRecord, rcsMsgRecord);
        header.append(ftInfo);
        return header.toString();
    }

    protected String buildFileObject(FileObject fileObjectRecord) {
        Log.d(CLASS_TAG, "buildFileObject begin");
        XmlSerializer serializer = Xml.newSerializer();

        StringWriter mStringWriter = new StringWriter();
        String resultStr = null;
        try {
            serializer.setOutput(mStringWriter);
            serializer.startTag(null, BackupConstant.FILE_OBJECT);

            String ftId = fileObjectRecord.getCid();
            if (ftId != null && ftId.length() > 0) {
                serializer.startTag(null, BackupConstant.CID);
                serializer.text(ftId);
                serializer.endTag(null, BackupConstant.CID);
            }

            serializer.startTag(null, BackupConstant.SDP);
            StringBuilder content = new StringBuilder();
            content.append(BackupConstant.FILE_DESCRIPTION);
            content.append(BackupConstant.FILE_SENDONLY);
            String name = fileObjectRecord.getName();
            String type = fileObjectRecord.getType();
            String size = Long.toString(fileObjectRecord.getSize());
            String selector = BackupConstant.FILE_SELECTOR + "name:\"" + name + "\"" + " type:"
                    + type + " size:" + size;
            content.append(selector);
            serializer.text(content.toString());
            long date = fileObjectRecord.getDate();
            String createDate = BackupConstant.FILE_CREATE_DATE + FileUtils.encodeDate(date);
            serializer.text(createDate);

            serializer.endTag(null, BackupConstant.SDP);
            serializer.endTag(null, BackupConstant.FILE_OBJECT);
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

    protected String build1ToNHeader(FtRecord ftRecord, RcsMsgRecord rcsMsgRecord) {
        StringBuilder header = new StringBuilder();
        header.append(BackupConstant.FROM + " ");
        header.append(rcsMsgRecord.getFrom());
        header.append(BackupConstant.LINE_BREAK);

        header.append(BackupConstant.CONVERST_ID + " ");
        header.append("");
        header.append(BackupConstant.LINE_BREAK);

        String ftInfo = buildOneFtMsgInfo(ftRecord, rcsMsgRecord);
        header.append(ftInfo);
        return header.toString();
    }

    protected String buildRootRecord(RootRecord rootRecord) {
        Log.d(CLASS_TAG, "buildRootRecord begin");
        String resultStr = null;
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter mStringWriter = new StringWriter();
        try {
            serializer.setOutput(mStringWriter);
            serializer.startTag(null, BackupConstant.FILE_TRANSFER_TYPE);
            serializer.text(rootRecord.getSessionType());
            serializer.endTag(null, BackupConstant.FILE_TRANSFER_TYPE);

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
            resultStr = mStringWriter.toString() + BackupConstant.LINE_BREAK
                    + BackupConstant.LINE_BREAK;
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

    private int copyFtFileMsg(Writer chatWriter, String filePath) {
        char[] buf = new char[1024];
        try {
            BufferedReader brd = null;
            brd = new BufferedReader(new FileReader(new File(filePath)));
            int len = 0;
            while ((len = brd.read(buf)) > 0) {
                chatWriter.write(buf, 0, len);
                chatWriter.flush();
            }

            chatWriter.write(BackupConstant.LINE_BREAK);
            chatWriter.write(BackupConstant.BOUNDDARY_CPM_END);
            brd.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return ResultCode.OTHER_EXCEPTION;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return ResultCode.IO_EXCEPTION;
        }
        return ResultCode.OK;
    }

    /**
     * pack one ft msg info that only belong to this one message.
     * Modify in 2015.8.19.
     */
    private String buildOneFtMsgInfo(FtRecord ftRecord, RcsMsgRecord rcsMsgRecord) {
        StringBuilder ftMsgInfo = new StringBuilder();
        ftMsgInfo.append(BackupConstant.IMDN_MSG_ID + " ");
        ftMsgInfo.append(ftRecord.getMsgId());

        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.DATE + " ");
        ftMsgInfo.append(FileUtils.encodeDate(rcsMsgRecord.getTimestamp()));
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

         long sendTime = rcsMsgRecord.getDataSent();
         ftMsgInfo.append(BackupConstant.MTK_SEND_TIME + " ");
         ftMsgInfo.append(FileUtils.encodeDate(sendTime));
         ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.MTK_DIRECTION + " ");
        ftMsgInfo.append(rcsMsgRecord.getDirection());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        //ft db ft talbe state
        ftMsgInfo.append(BackupConstant.MTK_STATUS + " ");
        ftMsgInfo.append(ftRecord.getStatus());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        //rcs message table status
        ftMsgInfo.append(BackupConstant.MTK_MSG_STATUS + " ");
        ftMsgInfo.append(rcsMsgRecord.getStatus());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        //rcs message table Class
        ftMsgInfo.append(BackupConstant.MTK_CLASS + " ");
        ftMsgInfo.append(rcsMsgRecord.getMsgClass());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        //rcs message table CHATMESSAGE_TYPE
        ftMsgInfo.append(BackupConstant.MTK_CHAT_TYPE + " ");
        ftMsgInfo.append(rcsMsgRecord.getType());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.MTK_SUB_ID + " ");
        ftMsgInfo.append(rcsMsgRecord.getSubID());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.MTK_LOCK + " ");
        ftMsgInfo.append(rcsMsgRecord.getLocked());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.MTK_SEEN + " ");
        ftMsgInfo.append(rcsMsgRecord.getSeen());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.MTK_FAV_FLAG + " ");
        ftMsgInfo.append(rcsMsgRecord.getFlag());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        //ft db ft talbe session_type
        ftMsgInfo.append(BackupConstant.MTK_SESSION_TYPE + " ");
        ftMsgInfo.append(ftRecord.getSessionType());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        String fileId = ftRecord.getFtId();
        if (fileId != null) {
            ftMsgInfo.append(BackupConstant.MTK_FTID + " ");
            ftMsgInfo.append(fileId);
            ftMsgInfo.append(BackupConstant.LINE_BREAK);
        }

        ftMsgInfo.append(BackupConstant.MTK_DURATION + " ");
        ftMsgInfo.append(ftRecord.getDuration());
        ftMsgInfo.append(BackupConstant.LINE_BREAK);

        ftMsgInfo.append(BackupConstant.CONTENT_TYPE + " ");
        ftMsgInfo.append(ContentType.GROUP_FT_TYPE);
        ftMsgInfo.append(BackupConstant.BOUNDDARY);
        ftMsgInfo.append(BackupConstant.CONTENT_TYPE + " ");
        ftMsgInfo.append("Application/X-CPM-File-Transfer");
        ftMsgInfo.append(BackupConstant.LINE_BREAK);
        ftMsgInfo.append(BackupConstant.LINE_BREAK);
        return ftMsgInfo.toString();
    }

    private String buildChatInfo(ChatRecord chatRecord) {
        StringBuilder chatInfo = new StringBuilder();

        chatInfo.append(BackupConstant.TO + " ");
        chatInfo.append(chatRecord.getTo());
        chatInfo.append(BackupConstant.LINE_BREAK);

        String subject = chatRecord.getSubject();
        if (subject != null) {
            chatInfo.append(BackupConstant.SUBJECT + " ");
            chatInfo.append(subject);
            chatInfo.append(BackupConstant.LINE_BREAK);
        }

        chatInfo.append(BackupConstant.CONVERST_ID + " ");
        chatInfo.append(chatRecord.getConversionId());
        chatInfo.append(BackupConstant.LINE_BREAK);

        // chatInfo.append(BackupConstant.CONTRIBUT_ID + " ");
        // chatInfo.append(" ");
        // chatInfo.append(BackupConstant.LINE_BREAK);

        chatInfo.append(BackupConstant.DATE_TIME + " "); // chat table time
        chatInfo.append(FileUtils.encodeDate(chatRecord.getTimeStamp()));
        chatInfo.append(BackupConstant.LINE_BREAK);

        chatInfo.append(BackupConstant.MTK_CHAT_ID + " ");
        chatInfo.append(chatRecord.getChatId());
        chatInfo.append(BackupConstant.LINE_BREAK);

        chatInfo.append(BackupConstant.MTK_CHAT_STATE + " ");
        chatInfo.append(chatRecord.getState());
        chatInfo.append(BackupConstant.LINE_BREAK);

        chatInfo.append(BackupConstant.MTK_CHAT_STATUS + " ");
        chatInfo.append(chatRecord.getThreadMapStatus());
        chatInfo.append(BackupConstant.LINE_BREAK);

        chatInfo.append(BackupConstant.MTK_CHAT_CHAIRMAN + " ");
        chatInfo.append(chatRecord.getChairman());
        chatInfo.append(BackupConstant.LINE_BREAK);

        return chatInfo.toString();
    }

    protected int addMsgBody(Writer chatWriter, OutputStream stream, String type,
         String filePath) {
        Log.d(CLASS_TAG, "addMsgBody");
        StringBuilder content = new StringBuilder();
        content.append(BackupConstant.LINE_BREAK);
        content.append(BackupConstant.BOUNDDARY_CPM);
        content.append(BackupConstant.LINE_BREAK);
        content.append(BackupConstant.CONTENT_TYPE + " " + type);
        content.append(BackupConstant.LINE_BREAK);
        if (type.equals(ContentType.LOCATION_TYPE) || type.equals(ContentType.VCARD_TYPE)
                || filePath.endsWith("xml") || filePath.endsWith("vcf")) {
            content.append(BackupConstant.ENCODING_BINARY);
        } else {
            content.append(BackupConstant.ENCODING_BASE64);
        }
        content.append(BackupConstant.LINE_BREAK);
        content.append(BackupConstant.LINE_BREAK);

        try {
            chatWriter.write(content.toString());
            chatWriter.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
            return ResultCode.IO_EXCEPTION;
        }
        int copyResult = ResultCode.OK;
        boolean isNeedEncode = true;
        if (type.equals(ContentType.VCARD_TYPE) || filePath.endsWith("vcf")) {
            Log.d(CLASS_TAG, "buildMsgBody this is vcard file");
            isNeedEncode = false;
        }
        if (type.equals(ContentType.LOCATION_TYPE) || filePath.endsWith("xml")) {
            Log.d(CLASS_TAG, "buildMsgBody this is gelo file");
            isNeedEncode = false;
        }

        if (!isNeedEncode) {
            Log.d(CLASS_TAG, "location msg or vcard, copy directly");
            copyResult = copyFtFileMsg(chatWriter, filePath);
            return copyResult;
        } else {
            Log.d(CLASS_TAG, "base64 encode and copy");
            InputStream is = null;
            Base64OutputStream base64os = null;

            try {
                is = new FileInputStream(new File(filePath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            byte[] buffer = new byte[2048];
            base64os = new Base64OutputStream(stream, Base64.CRLF | Base64.NO_CLOSE);
            int len;
            try {
                while ((len = is.read(buffer)) > 0) {
                    base64os.write(buffer, 0, len);
                    base64os.flush();
                }
                base64os.close();
                is.close();
                chatWriter.write(BackupConstant.LINE_BREAK);
                chatWriter.write(BackupConstant.BOUNDDARY_CPM_END);
            } catch (IOException e) {
                e.printStackTrace();
                return ResultCode.IO_EXCEPTION;
            }
            Log.d(CLASS_TAG, "encode end");
            return ResultCode.OK;
        }
    }
}
