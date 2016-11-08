package com.mediatek.rcs.messageservice.modules;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri; //import android.provider.Telephony;
import android.os.SystemProperties;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class SmsRestoreParser {
    private static final String CLASS_TAG = Utils.MODULE_TAG + "SmsRestoreParser";

    private ArrayList<SmsRestoreEntry> mVmessageList;

    int parseVmsg(File file) {
        Log.d(CLASS_TAG, "parseVmsg file = " + file.getName());

        int result = Utils.ResultCode.OK;
        getSmsRestoreEntry(file);
        Log.d(CLASS_TAG, "result = " + result + ", count:" + mVmessageList.size());
        return result;
    }

    void setVmessageList(ArrayList<SmsRestoreEntry> vmessageList) {
        this.mVmessageList = vmessageList;
    }

    private String decodeQuotedPrintable(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int length = bytes.length;
        for (int j = length - 1; j > 0; j--) {
            if (bytes[j] == ESCAPE_CHAR || '\r' == (char) bytes[j] || '\n' == (char) bytes[j]) {
                // Log.d(CLASS_TAG,
                // "decodeQuotedPrintable:end with'\r','\n'or'=' ");
            } else {
                length = j + 1;
                break;
            }
        }
        for (int i = 0; i < length; i++) {
            int b = bytes[i];
            if (b == ESCAPE_CHAR && i + 2 < length) {
                try {
                    if (i + 2 < length && '\r' == (char) bytes[i + 1]
                            && '\n' == (char) bytes[i + 2]) {
                        i += 2;
                        continue;
                    }
                    if (i + 1 < length && ('\r' == (char) bytes[i + 1])) {
                        i += 1;
                        continue;
                    }
                    if (i + 1 < length && ('\n' == (char) bytes[i + 1])) {
                        i += 1;
                        continue;
                    }
                    int u = Character.digit((char) bytes[++i], 16);
                    int l = Character.digit((char) bytes[++i], 16);
                    if (u == -1 || l == -1) {
                        return null;
                    }
                    buffer.write((char) ((u << 4) + l));

                } catch (ArrayIndexOutOfBoundsException e) {
                    String ssssBody = new String(buffer.toByteArray());
                    // Log.d(CLASS_TAG,
                    // "ArrayIndexOutOfBoundsException ssssBody =" + ssssBody);
                    return null;
                }
            } else {
                buffer.write(b);
            }
        }
        String sBody = new String(buffer.toByteArray());
        return sBody;
    }

    private static final String VMESSAGE_END_OF_LINE = "\r\n";
    private static final String BEGIN_VMSG = "BEGIN:VMSG";
    private static final String END_VMSG = "END:VMSG";
    private static final String VERSION = "VERSION:";
    private static final String BEGIN_VCARD = "BEGIN:VCARD";
    private static final String END_VCARD = "END:VCARD";
    private static final String BEGIN_VBODY = "BEGIN:VBODY";
    private static final String END_VBODY = "END:VBODY";
    private static final String FROMTEL = "TEL:";
    private static final String XBOX = "X-BOX:";
    private static final String XREAD = "X-READ:";
    private static final String XSEEN = "X-SEEN:";
    private static final String XSIMID = "X-SIMID:";
    private static final String XLOCKED = "X-LOCKED:";
    private static final String XTYPE = "X-TYPE:";
    private static final String DATE = "Date:";
    private static final String SUBJECT = "Subject";

    private static final String ESCAPE_STR = "=";
    private static final String REPLACED_STR = "==";
    private static final byte ESCAPE_CHAR = '=';

    private static final String VMESSAGE_END_OF_COLON = ":";

    protected class SmsRestoreEntry {
        private String mTimeStamp;

        private String mReadByte;

        private String mSeen;

        private String mBoxType;

        private String mSimCardid;

        private String mLocked;

        private String mSmsAddress;

        private String mBody;

        public String getTimeStamp() {
            return mTimeStamp;
        }

        public void setTimeStamp(String timeStamp) {
            this.mTimeStamp = timeStamp;
        }

        public String getReadByte() {
            return mReadByte == null ? "READ" : mReadByte;
        }

        public void setReadByte(String readByte) {
            this.mReadByte = readByte;
        }

        public String getSeen() {
            return mSeen == null ? "1" : mSeen;
        }

        public void setSeen(String seen) {
            this.mSeen = seen;
        }

        public String getBoxType() {
            return mBoxType;
        }

        public void setBoxType(String boxType) {
            this.mBoxType = boxType;
        }

        public String getSimCardid() {
            return mSimCardid;
        }

        public void setSimCardid(String simCardid) {
            this.mSimCardid = simCardid;
        }

        public String getLocked() {
            return mLocked;

        }

        public void setLocked(String locked) {
            this.mLocked = locked;
        }

        public String getSmsAddress() {
            return mSmsAddress;
        }

        public void setSmsAddress(String smsAddress) {
            this.mSmsAddress = smsAddress;
        }

        public String getBody() {
            return mBody;
        }

        public void setBody(String body) {
            this.mBody = body;
        }
    }

    protected ArrayList<SmsRestoreEntry> getSmsRestoreEntry(File file) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss");
        try {
            InputStream instream = new FileInputStream(file);
            InputStreamReader inreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inreader);
            String line = null;
            StringBuffer tmpbody = new StringBuffer();
            boolean appendbody = false;
            SmsRestoreEntry smsentry = null;
            while ((line = buffreader.readLine()) != null) {
                /*
                 * if (isCancel()) { smsEntryList.clear(); break; }
                 */

                if (line.startsWith(BEGIN_VMSG) && !appendbody) {
                    smsentry = new SmsRestoreEntry();
                    continue;
                }

                if (line.startsWith(FROMTEL) && !appendbody && smsentry != null) {
                    smsentry.setSmsAddress(line.substring(FROMTEL.length()));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(TEL)");
                }
                if (line.startsWith(XBOX) && !appendbody && smsentry != null) {
                    smsentry.setBoxType(line.substring(XBOX.length()));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(XBOX)");
                }
                if (line.startsWith(XREAD) && !appendbody && smsentry != null) {
                    smsentry.setReadByte(line.substring(XREAD.length()));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(XREAD)");
                }
                if (line.startsWith(XSEEN) && !appendbody && smsentry != null) {
                    smsentry.setSeen(line.substring(XSEEN.length()));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(XSEEN)");
                }
                if (line.startsWith(XSIMID) && !appendbody && smsentry != null) {
                    smsentry.setSimCardid(line.substring(XSIMID.length()));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(XSIMID)");
                }
                if (line.startsWith(XLOCKED) && !appendbody && smsentry != null) {
                    smsentry.setLocked(line.substring(XLOCKED.length()));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(XLOCKED)");
                }
                // if (line.startsWith(XTYPE)) {
                // smsentry.set(line.substring(XTYPE.length()));
                // Log.d(CLASS_TAG, "startsWith(XTYPE)
                // line.substring(XTYPE.length()) =
                // "+line.substring(XTYPE.length()));
                // }
                if (line.startsWith(DATE) && !appendbody && smsentry != null) {
                    long result = sd.parse(line.substring(DATE.length())).getTime();
                    smsentry.setTimeStamp(String.valueOf(result));
                    continue;
                    // Log.d(CLASS_TAG, "startsWith(DATE)");
                }

                if (line.startsWith(SUBJECT) && !appendbody) {
                    String bodySlash = line.substring(line.indexOf(VMESSAGE_END_OF_COLON) + 1);

                    tmpbody.append(bodySlash);
                    appendbody = true;
                    // Log.d(CLASS_TAG, "startsWith(SUBJECT) bodySlash=" +
                    // bodySlash);
                    continue;
                }
                if (line.startsWith(END_VBODY) && smsentry != null) {
                    appendbody = false;
                    String body = decodeBody(tmpbody.toString());
                    smsentry.setBody(body);
                    mVmessageList.add(smsentry);
                    tmpbody.setLength(0);
                    // Log.d(CLASS_TAG, "startsWith(END_VBODY)");
                    continue;
                }
                if (appendbody) {
                    if (tmpbody.toString().endsWith(ESCAPE_STR)) {
                        tmpbody.delete(tmpbody.lastIndexOf(ESCAPE_STR), tmpbody.length());
                    }
                    tmpbody.append(line);
                    // Log.d(CLASS_TAG, "appendbody=true,tmpbody=" +
                    // tmpbody.toString());
                }
            }
            instream.close();
        } catch (Exception e) {
            Log.e(CLASS_TAG, "init failed");
        }

        return mVmessageList;
    }

    private String decodeBody(String decodeBody) {
        boolean mInd = decodeBody.endsWith(ESCAPE_STR);
        Log.d(CLASS_TAG, "parseVmessage mInd = " + mInd);
        if (mInd) {
            decodeBody = decodeBody.substring(0, decodeBody.lastIndexOf(ESCAPE_STR));
            // Log.d(CLASS_TAG, "decodeBody = " + decodeBody);
        }
        decodeBody = decodeQuotedPrintable(decodeBody.getBytes());
        // Log.d(CLASS_TAG, "decodeBody2 = " + decodeBody);
        if (decodeBody == null) {
            return null;
        }
        int m = decodeBody.indexOf("END:VBODY");
        if (m > 0) {
            StringBuffer tempssb = new StringBuffer(decodeBody);
            do {
                if (m > 0) {
                    tempssb.deleteCharAt(m - 1);
                } else {
                    break;
                }
            } while ((m = tempssb.indexOf("END:VBODY", m + "END:VBODY".length())) > 0);
            decodeBody = tempssb.toString();
        }
        return decodeBody;
    }
}
