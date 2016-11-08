package com.mediatek.rcs.messageservice.modules;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Telephony.Mms;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.GenericPdu;

import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_SEND_REQ;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MmsDecomposer {
    private static final String CLASS_TAG = Utils.MODULE_TAG + "MmsDecomposer";
    private Context mContext;
    private boolean mCancel = false;

    public MmsDecomposer(Context context) {
        mContext = context;
    }

    public int retoreData(File pduFile) {
        if (pduFile == null || !pduFile.exists()) {
            Log.d(CLASS_TAG, "backup file error. return");
            return Utils.ResultCode.BACKUP_FILE_ERROR;
        }
        int result = implementComposeEntity(pduFile);
        onEnd();
        if (mCancel) {
            Log.d(CLASS_TAG, "restoreMmsData() service canceled");
            result = Utils.ResultCode.SERVICE_CANCELED;
        }
        return result;
    }

    protected int implementComposeEntity(File pduFile) {
        int result = Utils.ResultCode.OTHER_EXCEPTION;
        if (mCancel) {
            Log.d(CLASS_TAG, "backupMmsData() service canceled");
            result = Utils.ResultCode.SERVICE_CANCELED;
        }

        GenericPdu genericPdu = null;
        Log.d(CLASS_TAG, "backupPduPath:" + pduFile.getAbsolutePath());
        byte[] pduByteArray = readFileContent(pduFile.getAbsolutePath());
        if (pduByteArray != null) {
            result = Utils.ResultCode.OK;
        }
        int mmsType = 0;
        if (result == Utils.ResultCode.OK) {
            genericPdu = new PduParser(pduByteArray, false).parse(true);
        }
        if (genericPdu != null) {
            mmsType = genericPdu.getMessageType();
        } else {
            Log.d(CLASS_TAG, "genericPdu is null return");
            return Utils.ResultCode.BACKUP_FILE_ERROR;
        }
        Uri msgUri = null;
        if (mmsType == MESSAGE_TYPE_SEND_REQ) {
            Log.d(CLASS_TAG, "send box pdu");
            msgUri = Mms.Sent.CONTENT_URI;
        } else {
            Log.d(CLASS_TAG, "inbox pdu");
            msgUri = Mms.Inbox.CONTENT_URI;
        }

        int subId = new SubscriptionManager(mContext).getDefaultDataSubId();

        HashMap<String, String> msgInfo = new HashMap<String, String>();
        msgInfo.put("locked", "0");
        msgInfo.put("read", "1");
        msgInfo.put("sub_id", Integer.toString(subId));
        msgInfo.put("m_size", Integer.toString(pduByteArray.length));
        msgInfo.put("index", "0");

        if (genericPdu != null) {
            Log.d(CLASS_TAG, "genericPdu != null begin insert databases");
            Uri tmpUri = null;
            try {
                PduPersister persister = PduPersister.getPduPersister(mContext);
                tmpUri = persister.persistEx(genericPdu, msgUri, true, msgInfo);
                Log.d(CLASS_TAG, "MmsRestoreThread persist finish mms Uri : " + tmpUri);
            } catch (MmsException e) {
                Log.d(CLASS_TAG, "MmsRestoreThread MmsException");
                e.printStackTrace();
            } catch (Exception e) {
                Log.d(CLASS_TAG, "MmsRestoreThread Exception");
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Describe <code>onEnd</code> method here.
     *
     */
    protected boolean onEnd() {
        Log.d(CLASS_TAG, "onEnd()");
        return true;
    }


    /**
     * Describe <code>readFileContent</code> method here.
     *
     * @param fileName
     *            a <code>String</code> value
     * @return a <code>byte[]</code> value
     */
    private byte[] readFileContent(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }

            is.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This method will be called when backup service be cancel.
     *
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
