package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.ContentType;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Ft;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FileObject;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.FtRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.GroupNumberRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

//import com.mediatek.rcs.common.utils.RCSUtils;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;
import android.util.Xml;

class FtMsgRestoreParser {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FtMsgRestoreParser";
    private IinitFileName mGetFtFilePathClass;

    FtMsgRestoreParser(IinitFileName intFileName) {
        mGetFtFilePathClass = intFileName;
    }

    interface IinitFileName {
        String initFtFilePath(String name);
    }

    private FtRecord mFtRecord;
    private ChatRecord mChatRecord;
    private RootRecord mRootRecord;
    // private FileObject mFileObject;
    private RcsMsgRecord mRcsMsgRecord;
    private BufferedReader mReader;

    protected int parseFtMsg(File file) {
        InputStream instream = null;
        try {
            instream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        InputStreamReader inreader = new InputStreamReader(instream);
        mReader = new BufferedReader(inreader);
        Log.d(CLASS_TAG, "parseFtMsg begin file name is " + file.getAbsolutePath());
        int result = parseFtMsgImple(file);
        if (mReader != null) {
            try {
                mReader.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                return CloudBrUtils.ResultCode.IO_EXCEPTION;
            }
        }
        Log.d(CLASS_TAG, "parseFtMsg end result = " + result);
        return result;
    }

    private int parseFtMsgImple(File file) {
        if (mFtRecord == null) {
            Log.e(CLASS_TAG, "parseFtMsg mFtRecord = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        if (mChatRecord == null) {
            Log.e(CLASS_TAG, "parseFtMsg mChatRecord = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        if (mRcsMsgRecord == null) {
            Log.e(CLASS_TAG, "parseFtMsg mRcsMsgRecord = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        long beginTime = System.currentTimeMillis();
        Log.d(CLASS_TAG + "time", "begin parseFtMsg time = " + beginTime);

        String line = null;
        int result = parseFtHeader();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.d(CLASS_TAG, "parseHeader fail, return");
            return result;
        }
        Log.d(CLASS_TAG, "parse ft hander finish.");

        StringBuilder rootContent = new StringBuilder();
        StringBuilder fileObject = new StringBuilder();
        try {
            while (((line = mReader.readLine()) != null)) {
                if (line.isEmpty()) {
                    Log.d(CLASS_TAG, "parse root begin");
                    break;
                }
            }

            while (((line = mReader.readLine()) != null)) {
                if (line.isEmpty()) {
                    Log.d(CLASS_TAG, "parse file_object begin");
                    break;
                }
                if (line.contains("file-object")) {
                    fileObject.append(line);
                    break;
                }
                rootContent.append(line);
            }

            while (((line = mReader.readLine()) != null)) {
                if (line.startsWith(BackupConstant.BOUNDDARY_CPM)) {
                    Log.d(CLASS_TAG, "parse file_object finish");
                    break;
                }
                if (!line.isEmpty() && !line.equals(BackupConstant.ROOT_CONTENT_TYPE)) {
                    fileObject.append(line);
                }
            }

            while (((line = mReader.readLine()) != null)) {
                if (line.startsWith("Content-Transfer-Encoding")) {
                    Log.d(CLASS_TAG, "file body begin");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }
        Log.d(CLASS_TAG, "root body = " + rootContent.toString());

        result = FileUtils.persistRootData(rootContent.toString(), mRootRecord);
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.d(CLASS_TAG, "persistRootData fail, return");
            return result;
        }
        Log.d(CLASS_TAG, "parse ft persistRootData finish.");

        result = persistFileObject(fileObject.toString());
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.d(CLASS_TAG, "persistRootData fail, return");
            return result;
        }

        String type = mFtRecord.geMimeType();
        if (type == null || type.isEmpty()) {
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        }
        Log.d(CLASS_TAG, "msg type = " + type);

        long parseEndTime = System.currentTimeMillis();
        Log.d(CLASS_TAG + "time", "parseEndTime time = " + parseEndTime);
        Log.d(CLASS_TAG + "time", "parse file time = " + (parseEndTime - beginTime));

        if (!mRcsMsgRecord.isIsHasDirection()) {
            if (mRcsMsgRecord.getFrom().equals(CloudBrUtils.getMyNumber())) {
                mRcsMsgRecord.setDirection(Ft.Direction.OUTGOING);
            } else {
                mRcsMsgRecord.setDirection(Ft.Direction.INCOMING);
            }
        }
        // final file path;
        String fileName = mFtRecord.getFileName();
        fileName = mGetFtFilePathClass.initFtFilePath(fileName);
        Log.d(CLASS_TAG, "fileName = " + fileName);

        File ftFile = new File(fileName);
        try {
            ftFile.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        if (type.equals(ContentType.LOCATION_TYPE) || type.equals(ContentType.VCARD_TYPE)) {
            OutputStreamWriter osw;
            try {
                boolean isBodyBegin = false;
                osw = new OutputStreamWriter(new FileOutputStream(new File(fileName)));
                while (((line = mReader.readLine()) != null)) {
                    if (line.startsWith(BackupConstant.BOUNDDARY_CPM_END)) {
                        break;
                    } else if (line.isEmpty() && !isBodyBegin) {
                        Log.d(CLASS_TAG, "skip begin empty line.");
                    } else {
                        if (!isBodyBegin) {
                            isBodyBegin = true;
                        }
                        osw.write(line);
                        osw.write(BackupConstant.LINE_BREAK);
                    }
                }
                osw.close();
            } catch (IOException e) {
                e.printStackTrace();
                return CloudBrUtils.ResultCode.IO_EXCEPTION;
            }
            Log.d(CLASS_TAG, "gelocation msg resotore end successed, return.");
            return CloudBrUtils.ResultCode.OK;
        }

        String parseFilePath = file.getAbsolutePath();
        Log.d(CLASS_TAG, "this msg need decode, begin decode");
        String encodePath = parseFilePath.substring(0, parseFilePath.lastIndexOf(File.separator))
                + "tempft" + ".txt";
        File encodeFile = new File(encodePath);

        long getBodyTime = 0l;
        long decodeTime = 0L;
        try {
            encodeFile.createNewFile();
            FileWriter fw = new FileWriter(encodeFile);
            char[] buffer = new char[2048];
            int length = 0;

            while ((length = mReader.read(buffer)) > 0) {
                if (length == 2048) {
                    fw.write(buffer, 0, length);
                    fw.flush();
                } else {
                    fw.write(buffer, 0, length - 7);
                }
            }
            fw.close();
            mReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        getBodyTime = System.currentTimeMillis();
        Log.d(CLASS_TAG + "time", "get file body time = " + (getBodyTime - parseEndTime));
        decodeFile(fileName, encodeFile);

        decodeTime = System.currentTimeMillis();
        Log.d(CLASS_TAG + "time", "decodeTime time = " + (decodeTime - getBodyTime));
        Log.d(CLASS_TAG, "parser this ft msg end, success!");
        return CloudBrUtils.ResultCode.OK;
    }

    private int parseFtHeader() {
        try {
            String line;
            while (((line = mReader.readLine()) != null)) {
                // begin parse chat header and ft header info.
                String content;
                if (line.startsWith(BackupConstant.FROM)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setFrom(content);
                    continue;
                }

                if (line.startsWith(BackupConstant.TO)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setTo(content);
                    mChatRecord.setRejoinId(content);
                    continue;
                }

                if (line.startsWith(BackupConstant.SUBJECT)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    if (!content.isEmpty()) {
                        mChatRecord.setSubject(content);
                    }
                    Log.d(CLASS_TAG, "startsWith(SUBJECT)");
                    continue;
                }

                if (line.startsWith(BackupConstant.CONVERST_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setConversionId(content);
                    Log.d(CLASS_TAG, "startsWith(CONVERST_ID)");
                    continue;
                }

                if (line.startsWith(BackupConstant.DATE_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setTimeStamp(FileUtils.decodeDate(content));
                    Log.d(CLASS_TAG, "startsWith(DATE_TIME)");
                    continue;
                }

                if (line.startsWith(BackupConstant.DATE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setTimestamp(FileUtils.decodeDate(content));
                    Log.d(CLASS_TAG, "startsWith(DATE) time = " + content);
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_CHAT_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setChatId(content);
                    mRcsMsgRecord.setChatId(content);
                    Log.d(CLASS_TAG, "startsWith(MTK_CHAT_ID)");
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_CHAT_STATE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setState(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith(MTK_CHAT_STATE)");
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_CHAT_STATUS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setThreadMapStatus(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith(MTK_CHAT_STATUS)");
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_CHAT_CHAIRMAN)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setChairman(content);
                    continue;
                }


                if (line.startsWith(BackupConstant.MTK_SEND_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setDataSent((int) FileUtils.decodeDate(content));
                    Log.d(CLASS_TAG, "startsWith Ft.TIMESTAMP_SENT");
                    continue;
                }

                /*if (line.startsWith(BackupConstant.MTK_DELIVERED_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mFtRecord.setDeliveredTimestamp(FileUtils.decodeDate(content));
                    Log.d(CLASS_TAG, "startsWith Ft.TIMESTAMP_DELIVERED");
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_DISPLAYED_TIME)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mFtRecord.setDisplayedTimestamp(FileUtils.decodeDate(content));
                    Log.d(CLASS_TAG, "startsWith Ft.TIMESTAMP_DISPLAYED");
                    continue;
                }*/

                if (line.startsWith(BackupConstant.MTK_DIRECTION)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    Log.d(CLASS_TAG, "startsWith Ft.DIRECTION = " + content);
                    mRcsMsgRecord.setDirection(Integer.parseInt(content));
                    continue;
                }

                //ft db ft talbe state
                if (line.startsWith(BackupConstant.MTK_STATUS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mFtRecord.setStatus(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith FT.MTK_STATUS");
                    continue;
                }

                //rcsmessage table status
                if (line.startsWith(BackupConstant.MTK_MSG_STATUS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setStatus(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith rcs message table status = " + content);
                    continue;
                }

                //ft db ft talbe session_type
                if (line.startsWith(BackupConstant.MTK_SESSION_TYPE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mFtRecord.setSessionType(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith ft db ft talbe session_type = " + content);
                    continue;
                }
                 //rcs message table class
                if (line.startsWith(BackupConstant.MTK_CLASS)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setMsgClass(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith rcs message table msg_type = " + content);
                    continue;
                }

                //rcs message table CHATMESSAGE_TYPE
                if (line.startsWith(BackupConstant.MTK_CHAT_TYPE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mRcsMsgRecord.setType(Integer.parseInt(content));
                    Log.d(CLASS_TAG, "startsWith rcsmessage table CHATMESSAGE_TYPE = " + content);
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_FTID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mFtRecord.setFtId(content);
                    Log.d(CLASS_TAG, "startsWith FT.MTK_FTID");
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_DURATION)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mFtRecord.setDuration(Long.parseLong(content));
                    Log.d(CLASS_TAG, "startsWith FT.MTK_DURATION");
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

                if (line.startsWith(BackupConstant.BOUNDDARY_CPM)) {
                    Log.d(CLASS_TAG, "parse hander finish");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }
        return CloudBrUtils.ResultCode.OK;
    }

    private int decodeFile(String fileName, File srcFile) {
        Log.d(CLASS_TAG, "decodeFile fileName = " + fileName);
        Base64InputStream base64is = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        File encodeFile = srcFile;
        try {
            fis = new FileInputStream(encodeFile);
            fos = new FileOutputStream(new File(fileName));
            base64is = new Base64InputStream(fis, Base64.DEFAULT);
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = base64is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                fos.flush();
            }
            fos.close();
            base64is.close();
            if (encodeFile != null && encodeFile.exists()) {
                encodeFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }
        Log.d(CLASS_TAG, "decodeFile end fileName = " + fileName);
        return CloudBrUtils.ResultCode.OK;
    }

    private int persistFileObject(String content) {
        StringBufferInputStream is = new StringBufferInputStream(content);
        XmlPullParser parser = Xml.newPullParser();
        String ftId = null;
        String sdp = null;
        try {
            parser.setInput(is, "UTF-8");
            int eventCode;
            eventCode = parser.getEventType();
            while (eventCode != XmlPullParser.END_DOCUMENT) {
                switch (eventCode) {
                case XmlPullParser.START_DOCUMENT:
                    break;

                case XmlPullParser.START_TAG:
                    if ("cid".equals(parser.getName())) {
                        ftId = parser.nextText();
                        Log.d(CLASS_TAG, "persistFileObject ftId = " + ftId);
                    } else if ("sdp".equals(parser.getName())) {
                        sdp = parser.nextText();
                        Log.d(CLASS_TAG, "persistFileObject sdp = " + sdp);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    break;
                }
                eventCode = parser.next();
            }
            if (is != null) {
                is.close();
            }
        } catch (XmlPullParserException e) {
            Log.e(CLASS_TAG, "XmlPullParserException");
            e.printStackTrace();
            return CloudBrUtils.ResultCode.PARSE_XML_ERROR;
        } catch (IOException e) {
            Log.e(CLASS_TAG, "IOException");
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        Log.d(CLASS_TAG, "persistFileObject xml parse end, begin get info");
        // ftCv.put(Ft.FT_ID, ftId);
        mFtRecord.setFtId(ftId);
        String[] filePre = sdp.split("=");
        int length = filePre.length;
        Log.d(CLASS_TAG, "filePre.length " + filePre.length);
        String fileSelector = null;
        String fileDate = null;
        for (int index = 0; index < length; index++) {
            if (filePre[index].startsWith("file-selector")) {
                fileSelector = filePre[index];
            }
            if (filePre[index].startsWith("file-date")) {
                fileDate = filePre[index];
            }
        }

        String name = null;
        String type = null;
        Long size = 0L;
        Long time = 0L;

        int nameIndex = fileSelector.indexOf("name");
        int typeIndex = fileSelector.indexOf("type");
        int sizeIndex = fileSelector.indexOf("size");
        if (nameIndex != -1 && typeIndex != -1) {
           Log.d(CLASS_TAG, "nameIndex = " + nameIndex);
           Log.d(CLASS_TAG, "typeIndex = " + typeIndex);
           name = fileSelector.substring(nameIndex + 6, typeIndex -1);
           if (name.contains("\"")) {
             name = name.substring(0, name.indexOf("\""));
           }
           name.trim();
        }

        if (typeIndex != -1 && sizeIndex != -1) {
            type = fileSelector.substring(typeIndex + 5, sizeIndex -1);
            type.trim();
            size = Long.parseLong(fileSelector.substring(sizeIndex + 5, fileSelector.length() - 2).trim());
        }

        if (fileDate != null) {
            Log.d(CLASS_TAG, "persistFileObject file Date = " + fileDate);
//            time = FileUtils.decodeDate(fileDate.substring(BackupConstant.FILE_CREATE_DATE.length() - 1)
//                    .trim());
        }

        Log.d(CLASS_TAG, "persistFileObject xml parse end, get info end");
        Log.d(CLASS_TAG, "persistFileObject file type = " + type);
        Log.d(CLASS_TAG, "persistFileObject file size = " + size);
        Log.d(CLASS_TAG, "persistFileObject file time = " + time);
        Log.d(CLASS_TAG, "persistFileObject file name = " + name);

        if (name != null && name.endsWith("vcf")) {
            type = ContentType.VCARD_TYPE;
        }
        if (name != null && name.endsWith("xml")) {
            type = ContentType.LOCATION_TYPE;
        }

        mFtRecord.seMimeType(type);
        mFtRecord.setSize(size);
        mFtRecord.setFileName(name);
        if (name == null || type == null) {
            return CloudBrUtils.ResultCode.BACKUP_FILE_ERROR;
        } else {
            return CloudBrUtils.ResultCode.OK;
        }
    }

    protected void setFtRecord(FtRecord ftRecord) {
        this.mFtRecord = ftRecord;
    }

    protected void setChatRecord(ChatRecord chatRecord) {
        this.mChatRecord = chatRecord;
    }

    protected void setRootRecord(RootRecord rootRecord) {
        this.mRootRecord = rootRecord;
    }

    protected void setRcsMsgRecord(RcsMsgRecord rcsMsgRecord) {
        this.mRcsMsgRecord = rcsMsgRecord;
    }
}
