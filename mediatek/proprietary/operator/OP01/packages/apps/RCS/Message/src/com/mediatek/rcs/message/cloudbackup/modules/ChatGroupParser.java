package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.GroupChatMember;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.ChatRecord;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RootRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

/**
 * This class is used to parse group msg file that get from service.
 */
class ChatGroupParser {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "ChatGroupParser";
    private ContentResolver mContentResolver;
    private ChatRecord mChatRecord;
    private RootRecord mRootRecord;
    private BufferedReader mReader;
    private IGroupChatMsgParse mIGroupChatMsgParse;
    private Context mContext;

    ChatGroupParser(Context context, IGroupChatMsgParse iGroupChatMsgParse) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mIGroupChatMsgParse = iGroupChatMsgParse;
    }

    protected int parseChatGroupMsg(File file) {
        InputStream instream = null;
        try {
            instream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        InputStreamReader inreader = new InputStreamReader(instream);
        mReader = new BufferedReader(inreader);
        Log.d(CLASS_TAG + "time", "parseChatGroupMsg name = " + file.getAbsolutePath());
        int result = parseChatGroupMsgImple();
        if (mReader != null) {
            try {
                mReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(CLASS_TAG, "parseChatGroupMsg end. result = " + result);
        return result;
    }

    private int parseChatGroupMsgImple() {
        if (mRootRecord == null) {
            Log.e(CLASS_TAG, "parseFtMsg mFtRecord = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }
        if (mChatRecord == null) {
            Log.e(CLASS_TAG, "parseFtMsg mChatRecord = null, return");
            return CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        }

        long beginTime = System.currentTimeMillis();
        Log.d(CLASS_TAG + "time", "begin parseFtMsg time = " + beginTime);

        String line = null;
        int result = parseChatGroupHeader();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.d(CLASS_TAG, "parseChatGroupHeader fail, return");
            return result;
        }
        Log.d(CLASS_TAG, "parseChatGroupHeader chat group hander finish.");

        StringBuilder rootContent = new StringBuilder();
        try {
            while ((line = mReader.readLine()) != null) {
                if (line.startsWith(BackupConstant.BOUNDDARY_CPM)) {
                    Log.d(CLASS_TAG, "parse root finish");
                    break;
                }
                if (!line.isEmpty() && !line.equals(BackupConstant.ROOT_CONTENT_TYPE)) {
                    rootContent.append(line);
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
        Log.d(CLASS_TAG, "parse chat group persistRootData finish.");

        result = mIGroupChatMsgParse.dealChatRecord();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.d(CLASS_TAG, "mIGroupChatMsgParse.insertChatRecord fail, return");
            return result;
        }
        Log.d(CLASS_TAG, "mIGroupChatMsgParse.insertChatRecord finish.");

        String chatId = mChatRecord.getChatId();
        long threadId = mChatRecord.getThreadId();
        try {
            StringBuilder cpmMsg = new StringBuilder();
            while (((line = mReader.readLine()) != null)) {
                if (line.startsWith(BackupConstant.BOUNDDARY_CPM)
                        || line.startsWith(BackupConstant.BOUNDDARY_CPM_END)) {
                    result = mIGroupChatMsgParse.persistMessageBody(chatId, threadId,
                            cpmMsg.toString());
                    if (result != CloudBrUtils.ResultCode.OK) {
                        Log.d(CLASS_TAG, "mIGroupChatMsgParse.persistMessageBody fail, return");
                        return result;
                    }

                    cpmMsg.delete(0, cpmMsg.length()); // clear cmpMsg
                } else if (!line.isEmpty()) {
                    cpmMsg.append(line);
                    cpmMsg.append(BackupConstant.LINE_BREAK);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        return CloudBrUtils.ResultCode.OK;
    }

    private int parseChatGroupHeader() {
        String line = null;
        try {
            while (((line = mReader.readLine()) != null)) {
                String content;
                if (line.startsWith(BackupConstant.FROM)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setRejoinId(content);
                    continue;
                }

                if (line.startsWith(BackupConstant.SUBJECT)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setSubject(content);
                    Log.d(CLASS_TAG, "startsWith(SUBJECT)");
                    continue;
                }

                if (line.startsWith(BackupConstant.CONVERST_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setConversionId(content);
                    Log.d(CLASS_TAG, "startsWith(CONVERST_ID)");
                    continue;
                }

                if (line.startsWith(BackupConstant.DATE)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setTimeStamp(FileUtils.decodeDate(content));
                    Log.d(CLASS_TAG, "startsWith(DATE)");
                    continue;
                }

                if (line.startsWith(BackupConstant.MTK_CHAT_ID)) {
                    content = line.substring(line.indexOf(BackupConstant.SEPRATOR) + 1).trim();
                    mChatRecord.setChatId(content);
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

    private String persistRootData(String content, String chatId, Boolean isChatInfoExited) {
        StringBufferInputStream is = new StringBufferInputStream(content);

        String sessionType = null;
        String participants = null;
        ContentValues cv = null;
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
                    if (BackupConstant.SESSION_TYPE.equals(parser.getName())) {
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
                            cv = new ContentValues();
                        }
                    } else if (BackupConstant.MTK_MEMBER_NO.equals(parser.getName())) {
                        cv.put(GroupChatMember.COLUMN_CONTACT_NUMBER, parser.nextText());
                        parser.next();
                    } else if (BackupConstant.MTK_MEMBER_STATE.equals(parser.getName())) {
                        cv.put(GroupChatMember.COLUMN_STATE, parser.nextText());
                        parser.next();
                    } else if (BackupConstant.MTK_MEMBER_NAME.equals(parser.getName())) {
                        cv.put(GroupChatMember.COLUMN_CONTACT_NAME, parser.nextText());
                        parser.next();
                        cv.put(GroupChatMember.COLUMN_CHAT_ID, chatId);
                        if (!isChatInfoExited) {
                            mContentResolver.insert(CloudBrUtils.GROUP_MEMBER_URI, cv);
                        }
                        cv = null;
                    }
                    continue;

                case XmlPullParser.END_TAG:
                    if (parser.next() == XmlPullParser.END_DOCUMENT) {
                        Log.d(CLASS_TAG, "end document, break");
                        break;
                    }

                case XmlPullParser.END_DOCUMENT:
                    break;
                }
                eventCode = parser.next();
            }
            is.close();
            return participants;
        } catch (XmlPullParserException e) {
            Log.e(CLASS_TAG, "XmlPullParserException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(CLASS_TAG, "IOException");
            e.printStackTrace();
        }
        return null;
    }

    protected void setChatRecord(ChatRecord ChatRecord) {
        this.mChatRecord = ChatRecord;
    }

    protected void setRootRecord(RootRecord RootRecord) {
        this.mRootRecord = RootRecord;
    }

    public interface IGroupChatMsgParse {
        /**
         * how to deal with the chat record.
         *
         * @return
         */
        public int dealChatRecord();

        /**
         * persist method for each msg in this goup file.
         *
         * @param chatId
         *            this group msg chatid.
         * @param msgContent
         *            every msg body.
         * @return
         */
        public int persistMessageBody(String chatId, long threadId, String msgContent);
    }
}
