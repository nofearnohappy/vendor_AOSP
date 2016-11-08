package com.mediatek.rcs.messageservice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.provider.Telephony.Mms;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;

import com.mediatek.rcs.messageservice.modules.MmsDecomposer;
import com.mediatek.rcs.messageservice.modules.SmsDecomposer;

import java.util.HashMap;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.rcs.common.service.IRcsMessageRestoreService;
import com.mediatek.rcs.common.service.IMsgRestoreListener;

public class RcsMessageRestoreService extends Service {

    private final String CLASS_TAG = "com.mediatek.rcs.messageService/RcsMessageRestoreService";

    private RcsMessageBinder mRcsMessageBinder;
    private IMsgRestoreListener mIMsgRestoreListener;
    private Context mContext;
    private WorkThread mWorkThread;

    @Override
    public void onCreate() {
        super.onCreate();
        mRcsMessageBinder = new RcsMessageBinder();
        mContext = this;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mRcsMessageBinder;
    }

    public class RcsMessageBinder extends IRcsMessageRestoreService.Stub {

        @Override
        public boolean restoreSms(String vmsgPath) throws RemoteException {
            Log.d(CLASS_TAG, "restoreSms vmsgPath = " + vmsgPath);
            if (vmsgPath == null) {
                Log.e(CLASS_TAG, "restoreSms vmsgPath is null,return fail");
                return false;
            }
            File vmsgFile = new File(vmsgPath);
            if (vmsgFile == null) {
                Log.e(CLASS_TAG,
                        "restoreSms vmsgFile is null,return fail");
                return false;
            }
            if (!vmsgFile.exists()) {
                Log.e(CLASS_TAG,
                        "restoreSms vmsgFile  not existed,return fail");
                return false;
            }

            if (mIMsgRestoreListener == null) {
                Log.e(CLASS_TAG, "mIExcuteListener == null,return fail");
                return false;
            }
            if (mWorkThread == null) {
                mWorkThread = new WorkThread();
            }
            mWorkThread.setRestoreParam(vmsgFile, Module.RESTORE_MODULE_SMS);
            mWorkThread.start();
            return true;
        }

        @Override
        public boolean restoreMms(String pduPath) throws RemoteException {
            Log.d(CLASS_TAG, "restoreMms pduPath = " + pduPath);
            if (pduPath == null) {
                Log.e(CLASS_TAG, "restoremms pduPath is null,return fail");
                return false;
            }
            File pduFile = new File(pduPath);
            if (pduFile == null || !pduFile.exists()) {
                Log.e(CLASS_TAG,
                        "restoreSms pduFile is null or not existed,return fail");
                return false;
            }

            if (mIMsgRestoreListener == null) {
                Log.e(CLASS_TAG, "mIExcuteListener == null,return fail");
                return false;
            }
            if (mWorkThread == null) {
                mWorkThread = new WorkThread();
            }
            mWorkThread.setRestoreParam(pduFile, Module.RESTORE_MODULE_MMS);
            mWorkThread.start();
            return true;
        }

        @Override
        public void setListener(IMsgRestoreListener excuteListener)
                throws android.os.RemoteException {
            mIMsgRestoreListener = excuteListener;
        }

        @Override
        public int delMsg(Uri uri) throws android.os.RemoteException {
            return mContext.getContentResolver().delete(uri, null, null);
        }

        @Override
        public void setCancel(boolean isCancel)
                throws android.os.RemoteException {
            if (mWorkThread != null) {
                mWorkThread.setCancel(isCancel);
            }
        }

        @Override
        public Uri insertPdu(String pduPath) {
            Log.d(CLASS_TAG, "insertPdu pduPath = " + pduPath);
            return insertMmsFromPdu(pduPath);
        }
    }

    private class WorkThread extends Thread {
        private File mFile;
        private int mMode;
        private SmsDecomposer mSmsDecomposer;
        private MmsDecomposer mMmsDecomposer;
        private int mResult;
        private int mState;

        WorkThread() {
            mState = ThreadState.INIT;
        }

        public void setRestoreParam(File file, int mode) {
            mFile = file;
            mMode = mode;
        }

        public void setCancel(boolean cancel) {
            if (mSmsDecomposer != null) {
                mSmsDecomposer.setCancel(cancel);
            }

            if (mMmsDecomposer != null) {
                mMmsDecomposer.setCancel(cancel);
            }
        }

        @Override
        public void run() {
            if (mState == ThreadState.INIT) {
                mState = ThreadState.RUNNINT;
            } else {
                Log.e(CLASS_TAG, "WorkThread is busy mState = " + mState);
                try {
                    mIMsgRestoreListener.onWorkResult(-2);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (mMode == Module.RESTORE_MODULE_SMS) {
                Log.d(CLASS_TAG, "WorkThread restore sms, mFile path = "
                        + mFile.getAbsolutePath());
                if (mSmsDecomposer == null) {
                    mSmsDecomposer = new SmsDecomposer(mContext);
                }
                mResult = mSmsDecomposer.retoreData(mFile);
                Log.d(CLASS_TAG, "restore sms end result = " + mResult);
                mSmsDecomposer = null;
            } else {
                Log.d(CLASS_TAG, "WorkThread restore mms, mFile path = "
                        + mFile.getAbsolutePath());
                if (mMmsDecomposer == null) {
                    mMmsDecomposer = new MmsDecomposer(mContext);
                }
                mResult = mMmsDecomposer.retoreData(mFile);
                Log.d(CLASS_TAG, "restore mms end result = " + mResult);
                mMmsDecomposer = null;
            }
            try {
                mIMsgRestoreListener.onWorkResult(mResult);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            mFile = null;
            mIMsgRestoreListener = null;// note*******************
            mState = ThreadState.INIT;
        }
    }

    private class Module {
        public static final int RESTORE_MODULE_SMS = 0X1001;
        public static final int RESTORE_MODULE_MMS = 0X1002;
    }

    private class ThreadState {
        public static final int RUNNINT = 0X01;
        public static final int INIT = 0X00;
    }

    @Override
    public void onDestroy() {
        Log.d(CLASS_TAG, "onDestroy");
        super.onDestroy();
        if (mWorkThread != null) {
            mWorkThread = null;
        }
        if (mIMsgRestoreListener != null) {
            mIMsgRestoreListener = null;
        }
    }

    private Uri insertMmsFromPdu(String pduPath) {
        Log.d(CLASS_TAG, "insertMmsFromPdu");
        Uri ret = null;
        byte[] pduByteArray = readFileContent(pduPath);
        if (pduByteArray == null) {
            Log.e(CLASS_TAG, "insertMmsFromPdu, pduByteArray == null, return null");
            return null;
        }
        SendReq sendReq = new SendReq();
        MultimediaMessagePdu mmsPdu = (MultimediaMessagePdu) new PduParser(pduByteArray, false)
                .parse();
        if (mmsPdu.getSubject() != null) {
            sendReq.setSubject(mmsPdu.getSubject());
        }
        sendReq.setBody(mmsPdu.getBody());
        PduPersister persister = PduPersister.getPduPersister(this);
        try {
            ret = persister.persist(sendReq, Mms.Draft.CONTENT_URI, true, false, null);
        } catch (MmsException e) {
            e.printStackTrace();
        }
        return ret;
    }

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
}
