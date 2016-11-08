package com.mediatek.rcs.messageservice.modules;

public class Utils {
    public static final String MODULE_TAG = "com.mediatek.rcs.messageService/";

    /**
     * Result code during backup and restore.
     * result dialog will according to it to show result.
     */
    public static class ResultCode {
        public static final int IO_EXCEPTION = -1;
        public static final int OK = 0;
        public static final int INSERTDB_EXCEPTION = -2;
        public static final int DB_EXCEPTION = -3;
        public static final int BACKUP_FILE_ERROR = -4;
        public static final int PARSE_XML_ERROR = -5;
        public static final int SERVICE_CANCELED = -6;
        public static final int NETWORK_ERROR = -7;
        public static final int BACKUP_BEFOR_RESTORE_EXCEPTION = -8;
        public static final int BACKUP_FOLDER_EMPTY = -9;
        public static final int OTHER_EXCEPTION = -10;
    }

    public static class MmsXml {
        public static final String ROOT = "mms";
        public static final String RECORD = "record";
        public static final String ID = "_id";
        public static final String ISREAD = "isread";
        public static final String MSGBOX = "msg_box";
        public static final String DATE = "date";
        public static final String SIZE = "m_size";
        public static final String SIMID = "sim_id";
        public static final String ISLOCKED = "islocked";
    }

    public static final int NUMBER_IMPORT_MMS_EACH = 10;
    public static final int NUMBER_IMPORT_SMS_EACH = 40;

    /**
     * Mms Xml Info.
     */
    public static class MmsXmlInfo {
        private String mId;
        private String mIsRead;
        private String mMsgBox;
        private String mDate;
        private String mSize;
        private String mSimId;
        private String mIsLocked;

        public void setID(String id) {
            mId = id;
        }

        public String getID() {
            return (mId == null) ? "" : mId;
        }

        public void setIsRead(String isread) {
            mIsRead = isread;
        }

        public String getIsRead() {
            return ((mIsRead == null) || mIsRead.equals("")) ? "1" : mIsRead;
        }

        public void setMsgBox(String msgBox) {
            mMsgBox = msgBox;
        }

        public String getMsgBox() {
            return ((mMsgBox == null) || mMsgBox.equals("")) ? "1" : mMsgBox;
        }

        public void setDate(String date) {
            mDate = date;
        }

        public String getDate() {
            return (mDate == null) ? "" : mDate;
        }

        public void setSize(String size) {
            mSize = size;
        }

        public String getSize() {
            return ((mSize == null) || mSize.equals("")) ? "0" : mSize;
        }

        public void setSimId(String simId) {
            mSimId = simId;
        }

        public String getSimId() {
            return ((mSimId == null) || mSimId.equals("")) ? "0" : mSimId;
        }

        public void setIsLocked(String islocked) {
            mIsLocked = islocked;
        }

        public String getIsLocked() {
            return ((mIsLocked == null) || mIsLocked.equals("")) ? "0" : mIsLocked;
        }
    }

}
