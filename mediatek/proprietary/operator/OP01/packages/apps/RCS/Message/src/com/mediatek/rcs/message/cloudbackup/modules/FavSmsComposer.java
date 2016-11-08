package com.mediatek.rcs.message.cloudbackup.modules;

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
import java.util.ArrayList;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.Favorite;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;

class FavSmsComposer {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "FavSmsComposer";

    private static final String VMESSAGE_DEFAULT_EXPORT_CHARSET = "UTF-8";

    Writer mWriter = null;
    protected Context mContext;
    protected String mFavBackupFolder;
    private ArrayList<Integer> mIds;
    private boolean mCancel = false;
    private ContentResolver mContentResolver;

    FavSmsComposer(String filePath, ContentResolver contentResolver, ArrayList<Integer> ids) {
        mFavBackupFolder = filePath;
        mIds = ids;
        mContentResolver = contentResolver;
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
        if (mIds.size() <= 0) {
            Log.d(CLASS_TAG, "sms is no data, return ok");
            return CloudBrUtils.ResultCode.OK;
        }
        int result = init();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
            return result;
        }

        result = implementComposeEntity();
        if (result != CloudBrUtils.ResultCode.OK) {
            Log.e(CLASS_TAG, "backupChatGroupMsg error result " + result);
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
        if (mIds.size() > 0) {
            File path = new File(mFavBackupFolder);
            if (!path.exists()) {
                path.mkdirs();
            }

            File file = new File(path.getAbsolutePath() + File.separator
                    + FileUtils.ModulePath.SMS_VMSG);
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
     * Describe <code>implementComposeEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    private int implementComposeEntity() {
        int result = CloudBrUtils.ResultCode.OTHER_EXCEPTION;
        for (int id : mIds) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
                return CloudBrUtils.ResultCode.SERVICE_CANCELED;
            }

            String selection = CloudBrUtils.ID + " = " + id;
            Log.d(CLASS_TAG, "backup1To1ChatMsg selection = " + selection);
            Cursor favCursor = mContentResolver.query(CloudBrUtils.FAVOTIRE_URI, null, selection,
                    null, null);
            favCursor.moveToFirst();
            long mtime = favCursor.getLong(favCursor.getColumnIndex(Favorite.COLUMN_DATE));
            Log.e(CLASS_TAG, "mtime =" + mtime);
            String timeStamp = formatTimeStampString(mContext, mtime);

            String readByte = "READ";
            String seen = "1";
            String boxType = "INBOX";
            String locked = "UNLOCKED";
            String smsAddress = favCursor.getString(favCursor
                    .getColumnIndex(Favorite.COLUMN_CONTACT_NUB));
            if (smsAddress == null) {
                smsAddress = CloudBrUtils.getMyNumber();
            }
            String mSlotid = "0";

            String body = favCursor.getString(favCursor.getColumnIndex(Favorite.COLUMN_BODY));
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
                    + ",locked =" + locked + ",smsAddress =" + smsAddress);
            try {
                if (mWriter != null) {
                    mWriter.write(FileUtils.combineVmsg(timeStamp, readByte, boxType, mSlotid, locked,
                            smsAddress, body, seen));
                    result = CloudBrUtils.ResultCode.OK;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return CloudBrUtils.ResultCode.OK;
            }
            if (favCursor != null) {
                favCursor.close();
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
        if (mCancel) {
            Log.d(CLASS_TAG, "backup1To1ChatMsg() service canceled");
            result = CloudBrUtils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    private String formatTimeStampString(Context context, long when) {
        CharSequence formattor = DateFormat.format("yyyy/MM/dd kk:mm:ss", when);
        return formattor.toString();
    }
}
