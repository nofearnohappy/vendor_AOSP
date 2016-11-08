/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.cc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.DmService;
import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;

public class DmMailNodeIoHandler implements NodeIoHandler {

    protected Context mContext;
    protected Uri mUri;
    protected String mMccmnc;
    protected String mRecordToWrite;
    private boolean mIsDataReady = true;
//    private String mPushMailStr;
    private String mItemToRead;
    private final Object mLock = new Object();
    private final IntentFilter mFilter = new IntentFilter(
            "android.intent.action.PUSHMAIL_PROFILE");
    private PushMailReceiver mReceiver = new PushMailReceiver();

    private String[] mItem = { "apn", "smtp_server", "smtp_port", "smtp_ssl",
            "pop3_server", "pop3_port", "pop3_ssl", "recv_protocol" };

    static String[] sContent = new String[8];
    static String[] sSetArr = new String[8];

    public DmMailNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NODE_IO_HANDLER, "Mail constructed");

        mContext = ctx;
        mUri = treeUri;
        mMccmnc = mccMnc;
    }

    public int read(int arg0, byte[] arg1) throws VdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODE_IO_HANDLER, "uri: " + uriPath);
        Log.i(TAG.NODE_IO_HANDLER, "arg0: " + arg0);

        if (DmService.sCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.sCCStoredParams.get(uriPath);
            Log.d(TAG.NODE_IO_HANDLER,
                    "get valueToRead from mCCStoredParams, the value is "
                            + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < getItem().length; i++) {
                if (mUri.getPath().contains(getItem()[i])) {
                    if (sContent[i] != null) {
                        recordToRead = sContent[i];
                    } else {
                        mItemToRead = getItem()[i];
                        HandlerThread thread = new HandlerThread("pushmail");
                        thread.start();
                        mContext.registerReceiver(mReceiver, mFilter, null,
                                new Handler(thread.getLooper()));
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.PUSHMAIL_GET_PROFILE");
                        mContext.sendBroadcast(intent);
                        Log.i(TAG.NODE_IO_HANDLER,
                                "[MailNode] send broadcast intent 'PUSHMAIL_GET_PROFILE' ====>");

                        Log.i(TAG.NODE_IO_HANDLER,
                                "[MailNode] blocking here to wait for intent 'PUSHMAIL_PROFILE'...");
                        synchronized (mLock) {
                            while (mIsDataReady) {
                                try {
                                    // FIXME!!!
                                    // CC procedure will hang if the intent has
                                    // no response.
                                    // Do we need set a timeout here?
                                    mLock.wait();
                                    Log.i(TAG.NODE_IO_HANDLER,
                                            "[MailNode] skip waiting when got intent back");
                                    break;
                                } catch (InterruptedException e) {
                                    Log.e(TAG.NODE_IO_HANDLER,
                                            "[MailNode] waiting interrupted.");
                                }
                            }
                        }
                        mIsDataReady = true;
                        recordToRead = sContent[i];
                        mContext.unregisterReceiver(mReceiver);
                    }
                }
            }
            DmService.sCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG.NODE_IO_HANDLER,
                    "put valueToRead to mCCStoredParams, the value is "
                            + recordToRead);
        }
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws VdmException {

        Log.i(TAG.NODE_IO_HANDLER, "uri: " + mUri.getPath());
        Log.i(TAG.NODE_IO_HANDLER, "arg1: " + new String(arg1));
        Log.i(TAG.NODE_IO_HANDLER, "arg0: " + arg0);
        Log.i(TAG.NODE_IO_HANDLER, "arg2: " + arg2);

        if (mRecordToWrite == null) {
            mRecordToWrite = new String();
        }
        // FIXME: why add???
        mRecordToWrite += new String(arg1);
        if (mRecordToWrite.length() == arg2) {
            for (int i = 0; i < getItem().length; i++) {
                if (mUri.getPath().contains(getItem()[i])) {
                    sSetArr[i] = mRecordToWrite;

                    boolean needToBroadcast = true;
                    for (String s : sSetArr) {
                        if (s == null) {
                            needToBroadcast = false;
                        }
                    }

                    if (needToBroadcast) {

                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.PUSHMAIL_SET_PROFILE");
                        for (int k = 0; k < sSetArr.length; k++) {
                            intent.putExtra(getItem()[k], sSetArr[k]);
                            Log.i(TAG.NODE_IO_HANDLER, getItem()[k] + ": "
                                    + sSetArr[k]);
                        }

                        mContext.sendBroadcast(intent);
                        mRecordToWrite = null;
                        for (int j = 0; j < sContent.length; j++) {
                            sContent[j] = null;
                            sSetArr[j] = null;
                        }
                    }
                    break;
                }
            }
        }
    }

    protected String[] getItem() {
        return mItem;
    };

    class PushMailReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction.equals("android.intent.action.PUSHMAIL_PROFILE")) {
                Log.i(TAG.NODE_IO_HANDLER,
                        "[MailNode] received broadcast intent 'PUSHMAIL_PROFILE' <====");
                for (int i = 0; i < sContent.length; i++) {
                    sContent[i] = intent.getStringExtra(mItem[i]);
                    Log.i(TAG.NODE_IO_HANDLER, mItem[i] + ":" + sContent[i]);
                }
                mIsDataReady = true;
                synchronized (mLock) {
                    mLock.notify();
                    Log.i(TAG.NODE_IO_HANDLER,
                            "[MailNode] notifying the wait lock.");
                }
            }
        }

    }
}