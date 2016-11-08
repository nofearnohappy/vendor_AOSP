package com.mediatek.rcs.message.cloudbackup.modules;

import android.os.SystemProperties;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage.SubmitPdu;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

class SmsComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "SmsComposer";
    private static final String TRICKY_TO_GET_DRAFT_SMS_ADDRESS = "canonical_addresses.address from sms,threads,"
            + "canonical_addresses where sms.thread_id=threads._id and threads.recipient_ids="
            + "canonical_addresses._id and sms.thread_id =";
    private static final String COLUMN_NAME_DATE = "date";
    private static final String COLUMN_NAME_READ = "read";
    private static final String COLUMN_NAME_SEEN = "seen";
    private static final String COLUMN_NAME_TYPE = "type";
    private static final String COLUMN_NAME_SUB_ID = "sub_id";
    private static final String COLUMN_NAME_LOCKED = "locked";
    private static final String COLUMN_NAME_THREAD_ID = "thread_id";
    private static final String COLUMN_NAME_ADDRESS = "address";
    private static final String COLUMN_NAME_SC = "service_center";
    private static final String COLUMN_NAME_BODY = "body";

    private static final String VMESSAGE_DEFAULT_EXPORT_CHARSET = "UTF-8";

    private static final Uri[] SMSURIARRAY = { Sms.Inbox.CONTENT_URI, Sms.Sent.CONTENT_URI,
    // Sms.Outbox.CONTENT_URI,
    // Sms.Draft.CONTENT_URI
    };
    private Cursor[] mSmsCursorArray = { null, null };
    private Writer mWriter = null;
    private Context mContext;
    protected String mParentFolderPath;
    private boolean mCancel = false;

    SmsComposer(String parentFolderPath, Context context) {
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

    int backupSmsData() {
        int result = init();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupSmsData() init error result " + result);
            return result;
        }
        if (getCount() <= 0) {
            Log.d(CLASS_TAG, "sms is no data, return ok");
            for (Cursor cur : mSmsCursorArray) {
                if (cur != null) {
                    cur.close();
                    cur = null;
                }
            }
            return CloudBrUtils.ResultCode.OK;
        }

        result = implementComposeEntity();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupSmsData() implementComposeEntity error result " + result);
            return result;
        }

        result = onEnd();

        if (mCancel) {
            Log.d(CLASS_TAG, "backupSmsData() service canceled");
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
        for (int i = 0; i < SMSURIARRAY.length; ++i) {
            String selection = "ipmsg_id is 0";
            mSmsCursorArray[i] = mContext.getContentResolver().query(SMSURIARRAY[i], null,
                    selection, null, "date ASC");
            if (mSmsCursorArray[i] != null) {
                mSmsCursorArray[i].moveToFirst();
            }
        }

        if (getCount() > 0) {
            File file = new File(mParentFolderPath + File.separator + FileUtils.ModulePath.SMS_VMSG);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    mWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
                } catch (java.io.IOException e) {
                    Log.e(CLASS_TAG, "file:" + file.getAbsolutePath());
                    Log.e(CLASS_TAG, "create file failed");
                    e.printStackTrace();
                    result = CloudBrUtils.ResultCode.IO_EXCEPTION;
                }
            }
        }

        Log.d(CLASS_TAG, "init():" + result + ",count:");
        return result;
    }

    /**
     * Describe <code>getCount</code> method here.
     *
     * @return an <code>int</code> value
     */
    private int getCount() {
        int count = 0;
        for (Cursor cur : mSmsCursorArray) {
            if (cur != null && cur.getCount() > 0) {
                count += cur.getCount();
            }
        }

        Log.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    /**
     * Describe <code>implementComposeEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    private int implementComposeEntity() {
        int result = CloudBrUtils.ResultCode.OK;

        for (int i = 0; i < mSmsCursorArray.length; ++i) {
            if (mSmsCursorArray[i] == null) {
                continue;
            }
            Cursor tmpCur = mSmsCursorArray[i];
            while (!tmpCur.isAfterLast()) {
                if (mCancel) {
                    Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
                    return CloudBrUtils.ResultCode.SERVICE_CANCELED;
                }

                long mtime = tmpCur.getLong(tmpCur.getColumnIndex(COLUMN_NAME_DATE));
                Log.e(CLASS_TAG, "mtime =" + mtime);

                String timeStamp = formatTimeStampString(mtime);

                int read = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_READ));
                String readByte = (read == 0 ? "UNREAD" : "READ");

                String seen = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_SEEN));

                int box = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_TYPE));
                String boxType = null;
                switch (box) {
                case 1:
                    boxType = "INBOX";
                    break;

                case 2:
                    boxType = "SENDBOX";
                    break;

                default:
                    boxType = "INBOX";
                    break;
                }

                int subid = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_SUB_ID));
                String mSlotid = "0";
                if (SystemProperties.getBoolean("ro.mediatek.gemini_support", false) && subid >= 0) {
                    SubscriptionInfo simInfo = new SubscriptionManager(mContext)
                            .getActiveSubscriptionInfo(subid);
                    int slot = -1;
                    if (simInfo != null) {
                        slot = simInfo.getSimSlotIndex();
                    }
                    mSlotid = String.valueOf(slot + 1);

                }
                Log.d(CLASS_TAG, "is gemini = "
                        + SystemProperties.get("ro.mtk_gemini_support").equals("1") + ",subid:"
                        + subid + ",mSlotid:" + mSlotid);

                int lock = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_LOCKED));
                String locked = (lock == 1 ? "LOCKED" : "UNLOCKED");

                String smsAddress = null;
                if (i == 3) {
                    String threadId = tmpCur
                            .getString(tmpCur.getColumnIndex(COLUMN_NAME_THREAD_ID));
                    Cursor draftCursor = mContext.getContentResolver().query(
                            Uri.parse("content://sms"),
                            new String[] { TRICKY_TO_GET_DRAFT_SMS_ADDRESS + threadId + " --" },
                            null, null, null);

                    if (draftCursor != null) {
                        if (draftCursor.moveToFirst()) {
                            smsAddress = draftCursor.getString(draftCursor
                                    .getColumnIndex(COLUMN_NAME_ADDRESS));
                        }
                        draftCursor.close();
                    }
                } else {
                    smsAddress = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_ADDRESS));
                }

                if (smsAddress == null) {
                    smsAddress = "";
                }

                String sc = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_SC));

                String body = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_BODY));
                Log.d(CLASS_TAG, "implementComposeOneEntity body =" + body);
                StringBuffer sbf = null;
                if (body != null) {
                    sbf = new StringBuffer(body);
                    int num = 0;
                    num = sbf.indexOf("END:VBODY");
                    do {
                        if (num >= 0) {
                            sbf.insert(num, "/");
                        } else {
                            break;
                        }
                    } while ((num = sbf.indexOf("END:VBODY", num + 1 + "END:VBODY".length())) >= 0);
                    body = sbf.toString();
                }
                body = encodeQuotedPrintable(body);

                Log.e(CLASS_TAG, "combineVmsg:timeStamp=" + timeStamp + ",readByte=" + readByte
                        + ",locked =" + locked + ",body =" + body);
                try {
                    if (mWriter != null) {
                        mWriter.write(FileUtils.combineVmsg(timeStamp, readByte, boxType, mSlotid, locked,
                                smsAddress, body, seen));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return CloudBrUtils.ResultCode.IO_EXCEPTION;
                }
                tmpCur.moveToNext();
            }
        }

        return result;
    }

    private String encodeQuotedPrintable(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        int index = 0;
        int lineCount = 0;
        byte[] strArray = null;

        try {
            strArray = str.getBytes(VMESSAGE_DEFAULT_EXPORT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            Log.e(CLASS_TAG, " cannot be used. " + "Try default charset");
            strArray = str.getBytes();
        }
        while (index < strArray.length) {
            builder.append(String.format("=%02X", strArray[index]));
            index += 1;
            lineCount += 3;

            if (lineCount >= 67) {
                // Specification requires CRLF must be inserted before the
                // length of the line
                // becomes more than 76.
                // Assuming that the next character is a multi-byte character,
                // it will become
                // 6 bytes.
                // 76 - 6 - 3 = 67
                builder.append("=\r\n");
                lineCount = 0;
            }
        }

        return builder.toString();
    }

    protected void setParentFolderPath(String path) {
        mParentFolderPath = path;
    }

    /**
     * Describe <code>onEnd</code> method here.
     *
     */
    private int onEnd() {
        int result = CloudBrUtils.ResultCode.OK;
        try {
            Log.e(CLASS_TAG, "SmsBackupComposer onEnd");
            if (mWriter != null) {
                Log.e(CLASS_TAG, "mWriter.close()");
                mWriter.close();
            }
        } catch (IOException e) {
            Log.e(CLASS_TAG, "mWriter.close() failed");
            return CloudBrUtils.ResultCode.IO_EXCEPTION;
        }

        for (Cursor cur : mSmsCursorArray) {
            if (cur != null) {
                cur.close();
                cur = null;
            }
        }
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    private String formatTimeStampString(long when) {
        CharSequence formattor = DateFormat.format("yyyy/MM/dd kk:mm:ss", when);
        return formattor.toString();
    }
}
