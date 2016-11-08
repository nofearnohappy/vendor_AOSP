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

package com.mediatek.bluetoothle.anp.detector;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;

import com.mediatek.bluetoothle.anp.NotificationController;
import com.mediatek.bluetoothle.anp.support.ContactsUtil;
import com.mediatek.bluetoothle.anp.support.UnreadSmsCallBroadcastRegister;
import com.mediatek.bluetoothle.anp.support.UnreadSmsCallBroadcastRegister.AlertChangeListener;
import com.mediatek.bluetoothle.ext.BluetoothAnsDetector;

public class MessageAlertDetector extends BluetoothAnsDetector {

    private static final String TAG = "[BluetoothAns]MessageAlertDetector";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static final Uri OBSERVER_URI_SMS = Sms.CONTENT_URI;
    private static final Uri OBSERVER_URI_MMS = Mms.CONTENT_URI;
    private static final Uri OBSERVER_URI_MMS_SMS = MmsSms.CONTENT_URI;

    private static final Uri UNREAD_COUNT_URI = Uri.parse("content://mms-sms/unread_count");
    private static final String[] UNREAD_PROJECTION = {
            MmsSms._ID
    };

    // for get sms new alert
    private static final Uri NEW_COUNT_SMS_URI = Sms.CONTENT_URI;
    private static final String[] NEW_SMS_PROJECTION = {
            Sms.ADDRESS, Sms.DATE, Sms._ID
    };
    private static final String NEW_SMS_SELECTION = "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
            + " AND " + Sms.SEEN + " = 0)";
    private static final String NEW_SMS_ORDER_BY = Sms.DATE + " desc";
    private static final int SMS_ADDRESS_COLUMN = 0;
    private static final int SMS_DATE_COLUMN = 1;
    private static final int SMS_ID_COLUMN = 2;

    // for get mms new alert
    private static final Uri NEW_COUNT_MMS_URI = Mms.CONTENT_URI;
    private static final String[] NEW_MMS_PROJECTION = {
            Mms.DATE, Mms._ID
    };
    private static final String NEW_MMS_SELECTION = "(" + Mms.MESSAGE_BOX + "="
            + Mms.MESSAGE_BOX_INBOX
            + " AND " + Mms.SEEN + "=0"
            + " AND (" + Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND
            + " OR " + Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + "))";
    private static final String NEW_MMS_ORDER_BY = Mms.DATE + " desc";

    private static final int MMS_DATE_COLUMN = 0;
    private static final int MMS_ID_COLUMN = 1;

    private UnreadSmsCallBroadcastRegister mRegister;
    private ComponentName mName;
    private long mLastSmsId = -1;
    private long mLastMmsId = -1;

    private AlertChangeListener mUnreadMmsListener = new AlertChangeListener() {
        public void onAlertChanged(int number) {
            if (DBG) {
                Log.d(TAG, "mUnreadMmsListener, onAlertChanged, number = " + number);
            }
            mUnreadCount = number;
            onAlertNotify(null, NotificationController.CATEGORY_ENABLED_UNREAD);
        }
    };

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (DBG) {
                Log.d(TAG, "mObserver, onChange");
            }
            NewAlertContent smsContent = new NewAlertContent();
            NewAlertContent mmsContent = new NewAlertContent();
            getAndInitNewAlertStatus(smsContent, mmsContent);
            String address;
            String senderName;
            String text;
            // because mms use second, sms use millisecond
            if (mmsContent.getDate() * 1000 > smsContent.getDate()) {
                address = mmsContent.getAddress();
            } else {
                address = smsContent.getAddress();
            }
            if (address != null) {
                senderName = ContactsUtil.getNameFromAddress(mContext, address);
            } else {
                senderName = null;
            }
            // check if now need notify remote devices
            int count = smsContent.getCount() + mmsContent.getCount();
            boolean isNeedNotify = false;
            if ((count > 0) &&
                    (smsContent.getId() > mLastSmsId || mmsContent.getId() > mLastMmsId)) {
                isNeedNotify = true;
            }
            mLastSmsId = smsContent.getId();
            mLastMmsId = mmsContent.getId();
            if (DBG) {
                Log.d(TAG, "mObserver, onChange count = " + count + ", sender = " + senderName
                        + ", needNotify = " + isNeedNotify);
            }
            if (senderName == null) {
                text = address;
            } else {
                text = senderName;
            }
            mNewCount = count;
            setNewAlertText(text);
            if (isNeedNotify) {
                onAlertNotify(null, NotificationController.CATEGORY_ENABLED_NEW);
            }
        };
    };

    public MessageAlertDetector(Context context) {
        super(context);
        mCategoryId = NotificationController.CATEGORY_ID_SMS;
    }

    @Override
    public void initializeAll() {
        initNewAlertStatus();
        initUnreadAlertStatus();
        initNewDetector();
        initUnreadDetector();
    }

    @Override
    public void clearAll() {
        clearNewDetector();
        clearUnreadDetector();
    }

    private void initNewDetector() {
        if (DBG) {
            Log.d(TAG, "initNewDetector");
        }
        ContentResolver contentResolver = mContext.getContentResolver();
        contentResolver.registerContentObserver(OBSERVER_URI_SMS, true, mObserver);
        contentResolver.registerContentObserver(OBSERVER_URI_MMS, true, mObserver);
        contentResolver.registerContentObserver(OBSERVER_URI_MMS_SMS, true, mObserver);
    }

    private void initUnreadDetector() {
        if (DBG) {
            Log.d(TAG, "initUnreadDetector");
        }
        mName = new ComponentName("com.android.mms", "com.android.mms.ui.BootActivity");
        mRegister = UnreadSmsCallBroadcastRegister.getInstance();
        mRegister.registerAlertChangeListener(mContext, mName, mUnreadMmsListener);
    }

    private void initNewAlertStatus() {
        new Thread() {
            public void run() {
                NewAlertContent smsContent = new NewAlertContent();
                NewAlertContent mmsContent = new NewAlertContent();
                getAndInitNewAlertStatus(smsContent, mmsContent);
                int count = smsContent.getCount() + mmsContent.getCount();
                if (DBG) {
                    Log.d(TAG, "initNewAlertStatus, count = " + count);
                }
                mNewCount = count;
            }
        } .start();
    }

    private void initUnreadAlertStatus() {
        new Thread() {
            public void run() {
                Cursor cursor = null;
                try {
                    cursor = mContext.getContentResolver().query(UNREAD_COUNT_URI,
                            UNREAD_PROJECTION, null, null, null);
                    if (cursor != null) {
                        if (DBG) {
                            Log.d(TAG, "initUnreadAlertStatus, count = " + cursor.getCount());
                        }
                        mUnreadCount = cursor.getCount();
                    }
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "initUnreadAlertStatus, fail, not support uri");
                    return;
                } catch (SecurityException ex) {
                    ex.printStackTrace();
                    return;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } .start();
    }

    private void getAndInitNewAlertStatus(
            NewAlertContent smsAlertContent, NewAlertContent mmsAlertContent) {
        getNewSmsAlertContent(smsAlertContent);
        getNewMmsAlertContent(mmsAlertContent);
    }

    private void clearNewDetector() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    private void clearUnreadDetector() {
        mRegister.removeAlertChangeListener(mContext, mName);
    }

    private String getMmsFrom(long msgId) {
        Uri.Builder builder = Mms.CONTENT_URI.buildUpon();
        builder.appendPath(String.valueOf(msgId)).appendPath("addr");
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(builder.build(), new String[] {
                    Mms.Addr.ADDRESS, Mms.Addr.CHARSET
            }, Mms.Addr.TYPE + "=" + PduHeaders.FROM, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String from = cursor.getString(0);
                if (!TextUtils.isEmpty(from)) {
                    byte[] bytes = PduPersister.getBytes(from);
                    int charset = cursor.getInt(1);
                    return new EncodedStringValue(charset, bytes).getString();
                }
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void getNewSmsAlertContent(NewAlertContent alertContent) {

        long smsLastItemId = 0;
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(NEW_COUNT_SMS_URI, NEW_SMS_PROJECTION,
                    NEW_SMS_SELECTION, null, NEW_SMS_ORDER_BY);
            if (cursor != null && cursor.moveToFirst()) {
                if (DBG) {
                    Log.d(TAG, "getNewSmsAlertContent, start");
                }
                alertContent.setCount(cursor.getCount());
                alertContent.setDate(cursor.getLong(SMS_DATE_COLUMN));
                alertContent.setAddress(cursor.getString(SMS_ADDRESS_COLUMN));
                alertContent.setId(cursor.getLong(SMS_ID_COLUMN));
                if (DBG) {
                    Log.d(TAG, "getNewSmsAlertContent, result" + alertContent);
                }
            } else {
                alertContent.setCount(0);
                alertContent.setDate(0);
                alertContent.setAddress(null);
                alertContent.setId(0);
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void getNewMmsAlertContent(NewAlertContent alertContent) {
        if (DBG) {
            Log.d(TAG, "getNewMmsAlertContent, start");
        }
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(NEW_COUNT_MMS_URI, NEW_MMS_PROJECTION,
                    NEW_MMS_SELECTION, null, NEW_MMS_ORDER_BY);
            if (cursor != null && cursor.moveToFirst()) {
                alertContent.setCount(cursor.getCount());
                long msgId = cursor.getLong(MMS_ID_COLUMN);
                alertContent.setId(msgId);
                alertContent.setAddress(getMmsFrom(msgId));
                alertContent.setDate(cursor.getLong(MMS_DATE_COLUMN));
                Log.d(TAG, "getNewMmsAlertContent, result = " + alertContent.toString());
            } else {
                alertContent.setCount(0);
                alertContent.setId(0);
                alertContent.setAddress(null);
                alertContent.setDate(0);
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private class NewAlertContent {
        private long mLastItemId = 0;
        private int mNewAlertCount = 0;
        private String mLastItemAddress = null;
        private long mNewAlertDate = 0;

        public void setId(long id) {
            mLastItemId = id;
        }

        public void setCount(int count) {
            mNewAlertCount = count;
        }

        public void setAddress(String address) {
            mLastItemAddress = address;
        }

        public void setDate(long date) {
            mNewAlertDate = date;
        }

        public long getId() {
            return mLastItemId;
        }

        public int getCount() {
            return mNewAlertCount;
        }

        public String getAddress() {
            return mLastItemAddress;
        }

        public long getDate() {
            return mNewAlertDate;
        }

        public String toString() {
            return "Count = " + mNewAlertCount + ", LastItemId = " + mLastItemId
                    + ", Date = " + mNewAlertDate + ", Address = " + mLastItemAddress;
        }
    }
}
