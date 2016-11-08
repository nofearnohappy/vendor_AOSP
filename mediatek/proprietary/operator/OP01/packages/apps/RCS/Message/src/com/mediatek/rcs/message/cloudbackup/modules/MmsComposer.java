package com.mediatek.rcs.message.cloudbackup.modules;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Telephony.Mms;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.Xml;

import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MmsXmlInfo;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils.ModulePath;

import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import org.xmlpull.v1.XmlSerializer;

class MmsComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "MmsComposer";
    protected Context mContext;
    private static final String[] MMS_EXCLUDE_TYPE = { "134", "130" };
    private static final String COLUMN_NAME_ID = "_id";
    private static final String COLUMN_NAME_TYPE = "m_type";
    private static final String COLUMN_NAME_DATE = "date";
    private static final String COLUMN_NAME_MESSAGE_BOX = "msg_box";
    private static final String COLUMN_NAME_READ = "read";
    // private static final String COLUMN_NAME_ST = "st";
    private static final String COLUMN_NAME_SUBID = "sub_id";
    private static final String COLUMN_NAME_LOCKED = "locked";

    private static final Uri[] MMSURIARRAY = { Mms.Sent.CONTENT_URI,
            // Mms.Outbox.CONTENT_URI,
            // Mms.Draft.CONTENT_URI,
            Mms.Inbox.CONTENT_URI };
    private Cursor[] mMmsCursorArray = { null, null };

    private int mMmsIndex;
    private MmsXmlComposer mXmlComposer;
    private Object mLock = new Object();
    private ArrayList<MmsBackupContent> mPduList = null;
    private ArrayList<MmsBackupContent> mTempPduList = null;
    private static final String STORAGEPATH = "mms";
    private boolean mCancel = false;
    protected String mParentFolderPath;

    MmsComposer(String parentFolderPath, Context context) {
        mContext = context;
        mParentFolderPath = parentFolderPath;
    }

    /**
     * This method will be called when backup service be cancel.
     *
     * @param cancel
     */
    protected void setCancel(boolean cancel) {
        mCancel = cancel;
    }

    public int backupMmsData() {
        int result = init();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
            return result;
        }
        if (getCount() <= 0) {
            Log.d(CLASS_TAG, "mms is no data, return ok");
            for (Cursor cur : mMmsCursorArray) {
                if (cur != null) {
                    cur.close();
                    cur = null;
                }
            }
            return CloudBrUtils.ResultCode.OK;
        }

        result = implementComposeEntity();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
            return result;
        }
        result = onEnd();

        if (mCancel) {
            Log.d(CLASS_TAG, "backupMmsData() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    /**
     * Describe <code>init</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    private int init() {
        int result = CloudBrUtils.ResultCode.OK;
        mTempPduList = new ArrayList<MmsBackupContent>();
        for (int i = 0; i < MMSURIARRAY.length; ++i) {
            if (MMSURIARRAY[i] == Mms.Inbox.CONTENT_URI) {
                mMmsCursorArray[i] = mContext.getContentResolver().query(MMSURIARRAY[i], null,
                        "m_type <> ? AND m_type <> ?", MMS_EXCLUDE_TYPE, null);
            } else {
                mMmsCursorArray[i] = mContext.getContentResolver().query(MMSURIARRAY[i], null,
                        null, null, null);
            }
            if (mMmsCursorArray[i] != null) {
                mMmsCursorArray[i].moveToFirst();
            }
        }
        if (getCount() > 0) {
            mXmlComposer = new MmsXmlComposer();
            if (mXmlComposer != null) {
                mXmlComposer.startCompose();
            }
        }

        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }

        Log.d(CLASS_TAG, "init():" + result + " count:" + getCount());
        return result;
    }

    /**
     * Describe <code>getCount</code> method here.
     *
     * @return an <code>int</code> value
     */
    private int getCount() {
        int count = 0;
        for (Cursor cur : mMmsCursorArray) {
            if (cur != null && cur.getCount() > 0) {
                count += cur.getCount();
            }
        }

        Log.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    /**
     * Describe <code>implementComposeOneEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public int implementComposeEntity() {
        int result = CloudBrUtils.ResultCode.OK;
        byte[] pduMid;

        for (int i = 0; i < mMmsCursorArray.length; i++) {
            if (mMmsCursorArray[i] == null) {
                continue;
            }

            while (!mMmsCursorArray[i].isAfterLast()) {
                if (mCancel) {
                    Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
                    return CloudBrUtils.ResultCode.SERVICE_CANCELED;
                }

                int id = mMmsCursorArray[i].getInt(mMmsCursorArray[i]
                        .getColumnIndex(COLUMN_NAME_ID));
                Uri realUri = ContentUris.withAppendedId(MMSURIARRAY[i], id);
                Log.d(CLASS_TAG, "id:" + id + ",realUri:" + realUri);

                PduPersister p = PduPersister.getPduPersister(mContext);
                try {
                    if (MMSURIARRAY[i] == Mms.Inbox.CONTENT_URI) {
                        int type = mMmsCursorArray[i].getInt(mMmsCursorArray[i]
                                .getColumnIndex(COLUMN_NAME_TYPE));
                        Log.d(CLASS_TAG, "inbox, m_type:" + type);
                        if (type == MESSAGE_TYPE_NOTIFICATION_IND) {
                            NotificationInd nPdu = (NotificationInd) p.load(realUri);
                            pduMid = new PduComposer(mContext, nPdu).make(true);
                        } else if (type == MESSAGE_TYPE_RETRIEVE_CONF) {
                            RetrieveConf rPdu = (RetrieveConf) p.load(realUri, true);
                            pduMid = new PduComposer(mContext, rPdu).make(true);
                        } else {
                            pduMid = null;
                            Log.d(CLASS_TAG, "pduMid ==null");
                        }
                    } else {
                        SendReq sPdu = (SendReq) p.load(realUri);
                        pduMid = new PduComposer(mContext, sPdu).make();
                    }

                    if (pduMid != null) {
                        String fileName = Integer.toString(mMmsIndex++) + ModulePath.FILE_EXT_PDU;
                        String isRead = mMmsCursorArray[i].getString(mMmsCursorArray[i]
                                .getColumnIndex(COLUMN_NAME_READ));
                        String msgBox = mMmsCursorArray[i].getString(mMmsCursorArray[i]
                                .getColumnIndex(COLUMN_NAME_MESSAGE_BOX));
                        String date = mMmsCursorArray[i].getString(mMmsCursorArray[i]
                                .getColumnIndex(COLUMN_NAME_DATE));
                        int subId = mMmsCursorArray[i].getInt(mMmsCursorArray[i]
                                .getColumnIndex(COLUMN_NAME_SUBID));
                        Log.d(CLASS_TAG, "Get subId From Mms_db :" + subId);
                        String slotId = "0";
                        if (SystemProperties.get("ro.mtk_gemini_support").equals("1") && subId >= 0) {
                            SubscriptionInfo simInfo = new SubscriptionManager(mContext)
                                    .getActiveSubscriptionInfo(subId);
                            if (simInfo != null) {
                                int slot = simInfo.getSimSlotIndex();
                                slotId = String.valueOf(slot + 1);
                            } else {
                                Log.d(CLASS_TAG, "WARMING : simInfo is null and stop backup MMS");
                            }
                        }
                        String isLocked = mMmsCursorArray[i].getString(mMmsCursorArray[i]
                                .getColumnIndex(COLUMN_NAME_LOCKED));
                        MmsXmlInfo mRecord = new MmsXmlInfo();
                        mRecord.setID(fileName);
                        mRecord.setIsRead(isRead);

                        mRecord.setMsgBox(msgBox);
                        mRecord.setDate(date);
                        mRecord.setSize(Integer.toString(pduMid.length));
                        mRecord.setSimId(slotId);
                        mRecord.setIsLocked(isLocked);
                        MmsBackupContent tmpContent = new MmsBackupContent();
                        tmpContent.pduMid = pduMid;
                        tmpContent.fileName = fileName;
                        tmpContent.mRecord = mRecord;
                        mTempPduList.add(tmpContent);
                    }

                    if (mMmsIndex % FileUtils.NUMBER_IMPORT_MMS_EACH == 0
                            || mMmsIndex >= (getCount() - 1)) {
                        if (mPduList != null) {
                            synchronized (mLock) {
                                try {
                                    Log.d(CLASS_TAG, "wait for WriteFileThread:");
                                    mLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        mPduList = mTempPduList;
                        new WriteFileThread().start();
                        if (!isAfterLast()) {
                            mTempPduList = new ArrayList<MmsBackupContent>();
                        }
                    }

                } catch (InvalidHeaderValueException e) {
                    e.printStackTrace();
                    Log.e(CLASS_TAG, "InvalidHeaderValueException");
                    return CloudBrUtils.ResultCode.DB_EXCEPTION;
                } catch (MmsException e) {
                    e.printStackTrace();
                    Log.e(CLASS_TAG, "MmsException");
                    return CloudBrUtils.ResultCode.DB_EXCEPTION;
                }

                mMmsCursorArray[i].moveToNext();
            }
        }

        Log.d(CLASS_TAG, "implementComposeOneEntity result :" + result);
        return result;
    }

    /**
     * Describe <code>isAfterLast</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    private boolean isAfterLast() {
        boolean result = true;
        for (Cursor cur : mMmsCursorArray) {
            if (cur != null && !cur.isAfterLast()) {
                result = false;
                break;
            }
        }

        Log.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    /**
     * Describe class <code>MmsBackupContent</code> here.
     *
     */
    private class MmsBackupContent {
        public byte[] pduMid;
        public String fileName;
        MmsXmlInfo mRecord;
    }

    /**
     * Describe <code>writeToFile</code> method here.
     *
     * @param fileName
     *            a <code>String</code> value
     * @param buf
     *            a <code>byte</code> value
     * @exception IOException
     *                if an error occurs
     */
    private void writeToFile(String fileName, byte[] buf) {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class WriteFileThread extends Thread {
        @Override
        public void run() {
            Log.e(CLASS_TAG, "WriteFileThread run");
            String path = mParentFolderPath + File.separator;
            int length = mPduList.size();
            Log.d(CLASS_TAG, "WriteFileThread mPduList.size() = " + length);
            for (int j = 0; (mPduList != null) && (j < length); ++j) {
                MmsBackupContent mmsContent = mPduList.get(j);
                byte[] pduByteArray = mmsContent.pduMid;
                String fileName = mmsContent.fileName;
                if (pduByteArray != null) {
                    Log.d(CLASS_TAG, "WriteFileThread() pduMid.length:" + pduByteArray.length);
                    writeToFile(path + fileName, pduByteArray);
                    if (mXmlComposer != null) {
                        mXmlComposer.addOneMmsRecord(mmsContent.mRecord);
                    }
                    Log.d(CLASS_TAG, "WriteFileThread() addFile:" + fileName + " success");
                } else {
                    Log.e(CLASS_TAG, "WriteFileThread pduByteArray == null");
                }
            }

            synchronized (mLock) {
                mPduList = null;
                mLock.notifyAll();
            }
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     *
     */
    private int onEnd() {
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            return CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        if (mPduList != null) {
            synchronized (mLock) {
                try {
                    Log.d(CLASS_TAG, "wait for WriteFileThread:");
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mXmlComposer != null) {
            mXmlComposer.endCompose();
            String msgXmlInfo = mXmlComposer.getXmlInfo();
            if (getCount() > 0 && msgXmlInfo != null) {
                writeToFile(mParentFolderPath + File.separator + FileUtils.ModulePath.MMS_XML,
                        msgXmlInfo.getBytes());

            }
        }

        for (Cursor cur : mMmsCursorArray) {
            if (cur != null) {
                cur.close();
                cur = null;
            }
        }
        return CloudBrUtils.ResultCode.OK;
    }
}
