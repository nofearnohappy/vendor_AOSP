package com.mediatek.rcs.messageservice.modules;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri; //import android.provider.Telephony;
import android.os.SystemProperties;
import android.provider.Telephony.Sms;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
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

import com.mediatek.rcs.messageservice.modules.SmsRestoreParser.SmsRestoreEntry;

public class SmsDecomposer {
    private static final String CLASS_TAG = Utils.MODULE_TAG + "SmsDecomposer";
    private static final Uri[] SMSURIARRAY = { Sms.Inbox.CONTENT_URI, Sms.Sent.CONTENT_URI
    // Sms.Draft.CONTENT_URI,
    // Sms.Outbox.CONTENT_URI
    };

    private ArrayList<ContentProviderOperation> mOperationList;
    private ArrayList<SmsRestoreEntry> mVmessageList;
    private Context mContext;
    private SmsRestoreParser mSmsRestoreParser;
    private boolean mCancel = false;

    public SmsDecomposer(Context context) {
        mContext = context;
        mSmsRestoreParser = new SmsRestoreParser();
    }

    public int retoreData(File file) {
        if (file == null || !file.exists()) {
            // if (file == null || !file.exists() ||
            // !file.getName().endsWith("vmsg")) {
            Log.d(CLASS_TAG, "backup file error. return");
            return Utils.ResultCode.BACKUP_FILE_ERROR;
        }

        if (mSmsRestoreParser == null) {
            Log.e(CLASS_TAG, "retoreData mRestoreSmsImpl = null, return");
            return Utils.ResultCode.OTHER_EXCEPTION;
        }
        initRestore();

        if (mCancel) {
            Log.d(CLASS_TAG, "retoreData() service canceled");
            return Utils.ResultCode.SERVICE_CANCELED;
        }

        int result = mSmsRestoreParser.parseVmsg(file);
        if (result != Utils.ResultCode.OK) {
            Log.e(CLASS_TAG, "mRestoreSmsImpl.parseVmsg result is not ok, return");
            return result;
        }
        Log.d(CLASS_TAG, "mSmsRestoreParser.parseVmsg success!");

        result = implementComposeEntity();
        if (result != Utils.ResultCode.OK) {
            Log.e(CLASS_TAG, "implementComposeEntity() result is not ok, return");
            return result;
        }
        onEnd();
        return Utils.ResultCode.OK;
    }

    private void initRestore() {
        mVmessageList = null;
        mVmessageList = new ArrayList<SmsRestoreEntry>();
        mSmsRestoreParser.setVmessageList(mVmessageList);
        mOperationList = new ArrayList<ContentProviderOperation>();
    }

    private int implementComposeEntity() {
        int result = Utils.ResultCode.OK;
        Log.d(CLASS_TAG, "implementComposeEntity mVmessageList.size() = " + mVmessageList.size());
        for (int index = 0; index < mVmessageList.size(); index++) {
            if (mCancel) {
                Log.d(CLASS_TAG, "backupData() service canceled");
                return Utils.ResultCode.SERVICE_CANCELED;
            }

            SmsRestoreEntry vMsgFileEntry = mVmessageList.get(index);

            if (vMsgFileEntry != null) {
                ContentValues values = combineContentValue(vMsgFileEntry);
                if (values == null) {
                    Log.d(CLASS_TAG, "combineContentValue():values=null");
                } else {
                    Log.d(CLASS_TAG, "begin restore:" + System.currentTimeMillis());
                    int mboxType = vMsgFileEntry.getBoxType().equals("INBOX") ? 1 : 2;
                    Log.d(CLASS_TAG, "mboxType:" + mboxType);
                    ContentProviderOperation.Builder builder = ContentProviderOperation
                            .newInsert(SMSURIARRAY[mboxType - 1]);
                    builder.withValues(values);
                    mOperationList.add(builder.build());
                    if ((index % Utils.NUMBER_IMPORT_SMS_EACH != 0)
                            && index != (mVmessageList.size() - 1)) {
                    }

                    if (mOperationList.size() > 0) {
                        try {
                            mContext.getContentResolver().applyBatch("sms", mOperationList);
                        } catch (android.os.RemoteException e) {
                            e.printStackTrace();
                            return Utils.ResultCode.DB_EXCEPTION;
                        } catch (android.content.OperationApplicationException e) {
                            e.printStackTrace();
                            return Utils.ResultCode.DB_EXCEPTION;
                        } finally {
                            mOperationList.clear();
                        }
                    }

                    Log.d(CLASS_TAG, "end restore:" + System.currentTimeMillis());
                }
            } else {
                Log.d(CLASS_TAG, "vMsgFileEntry == null");
            }
        }

        Log.d(CLASS_TAG, "implementComposeEntity end");
        return result;
    }

    private ContentValues combineContentValue(SmsRestoreEntry pdu) {

        ContentValues values = new ContentValues();

        values.put(Sms.ADDRESS, pdu.getSmsAddress());
        String body = pdu.getBody();
        values.put(Sms.BODY, body);
        // Log.d(CLASS_TAG, "readorunread :" + pdu.getReadByte());

        values.put(Sms.READ, (pdu.getReadByte().equals("UNREAD") ? 0 : 1));
        values.put(Sms.SEEN, pdu.getSeen());
        values.put(Sms.LOCKED, (pdu.getLocked().equals("LOCKED") ? "1" : "0"));
        String subId = "-1";
        int soltid = Integer.parseInt(pdu.getSimCardid());
        Log.d(CLASS_TAG, " debug 1: from pdu get SimID and soltid: " + soltid);
        if (soltid > 0) {
            if (SystemProperties.getBoolean("ro.mediatek.gemini_support", false)) {
                soltid = soltid - 1;
            } else {
                soltid = 0;
            }
        } else {
            soltid = 0;
            Log.d(CLASS_TAG, "Warnning : soltid set 0 !");
        }

        Log.d(CLASS_TAG, " debug 2: after counted soltid: " + soltid);
        SubscriptionInfo si = new SubscriptionManager(mContext)
                .getActiveSubscriptionInfoForSimSlotIndex(soltid);
        if (si != null) {
            subId = String.valueOf(si.getSubscriptionId());
        }
        Log.d(CLASS_TAG, "we will put subId into MessageDb and the subId : " + subId);
        values.put(Sms.SUBSCRIPTION_ID, subId);
        values.put(Sms.DATE, pdu.getTimeStamp());
        values.put(Sms.TYPE, (pdu.getBoxType().equals("INBOX") ? 1 : 2));
        // values.put(COLUMN_NAME_IMPORT_SMS, true);

        return values;
    }

    private boolean onEnd() {
        if (mVmessageList != null) {
            mVmessageList.clear();
        }

        if (mOperationList != null) {
            mOperationList = null;
        }

        Log.d(CLASS_TAG, "onEnd()");
        Log.d(CLASS_TAG, "smsRestore end:" + System.currentTimeMillis());
        return true;
    }

    /**
     * This method will be called when restore service be cancel.
     *
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        mCancel = cancel;
    }
}
