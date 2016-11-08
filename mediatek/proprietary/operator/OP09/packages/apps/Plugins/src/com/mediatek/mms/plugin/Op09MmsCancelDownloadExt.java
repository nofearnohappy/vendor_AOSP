/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import java.util.HashMap;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.telephony.SmsManager;
import android.util.Log;

import com.mediatek.mms.callback.ITransactionServiceCallback;


/**
 * M: Plugin implemention for OP09 mms cancel download.
 */
public class Op09MmsCancelDownloadExt {
    private static final String TAG = "Mms/Op09MmsCancelDownloadExt";
    private static final String STATUS_EXT = "st_ext";
    public static int STATE_UNKNOWN = 0x00;
    public static int STATE_DOWNLOADING = 0x01;
    public static int STATE_CANCELLING = 0x02;
    public static int STATE_COMPLETE = 0x03;
    public static int STATE_ABORTED = 0x04;

    private Context mContext;
    private HashMap<String, Uri> mClientMap;
    private boolean mWaitingCnxn;
    private ITransactionServiceCallback mCdh;

    private static Op09MmsCancelDownloadExt sOp09MmsCancelDownloadExt;

    public static Op09MmsCancelDownloadExt getIntance(Context context) {
        if (sOp09MmsCancelDownloadExt == null) {
            sOp09MmsCancelDownloadExt = new Op09MmsCancelDownloadExt(context);
        }
        return sOp09MmsCancelDownloadExt;
    }

    public Op09MmsCancelDownloadExt(Context context) {
        mContext = context;
        mClientMap = new HashMap<String, Uri>();
        mWaitingCnxn = false;
    }
    /**
     * M: Constructor.
     * @param context the Context.
     */
    public Op09MmsCancelDownloadExt(Context context, ITransactionServiceCallback cdh) {
        mContext = context;
        mClientMap = new HashMap<String, Uri>();
        mCdh = cdh;
        mWaitingCnxn = false;
    }

    public void setHostCallback(ITransactionServiceCallback cdh){

        mCdh = cdh;
        Log.d(TAG, "Op09MmsCancelDownloadExt mCdh: " +mCdh);
    }

    public void addHttpClient(String url, Uri client) {
        Log.d("@M_" + TAG, "setHttpClient(): url = " + url);

        mClientMap.put(url, client);
    }


    public void cancelDownload(final Uri uri) {
        Log.d("@M_" + TAG, "MmsCancelDownloadExt: cancelDownload()");
        if (uri == null) {
            Log.d("@M_" + TAG, "cancelDownload(): uri is null!");
            return;
        }

        // Update the download status
        markStateExt(uri, STATE_CANCELLING);

        Thread thread = new Thread(new Runnable() {
            String mContentUrl = null;


            public void run() {
                mContentUrl = getContentLocation(uri);

                if (!mClientMap.containsKey(mContentUrl)) {
                    setCancelDownloadState(uri, true);
                } else {
                    abortMmsHttp(mContentUrl, uri);
                }
            }
        });

        thread.start();
    }


    public void removeHttpClient(String url) {
        Log.d("@M_" + TAG, "removeHttpClient(): url = " + url);

        mClientMap.remove(url);
    }

    public void markStateExt(Uri uri, int state) {
        Log.d("@M_" + TAG, "markStateExt: state = " + state + " uri = " + uri);

        // Use the STATUS field to store the state of downloading process
        ContentValues values = new ContentValues(1);
        values.put(STATUS_EXT, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    uri, values, null, null);
    }


    public int getStateExt(Uri uri) {
        Log.d("@M_" + TAG, "getStateExt: uri = " + uri);
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            uri, new String[] {STATUS_EXT}, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return STATE_UNKNOWN;
    }


    public int getStateExt(String url) {
        Log.d("@M_" + TAG, "getStateExt: url = " + url);

        String where = Mms.CONTENT_LOCATION + " = ?";
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                Mms.CONTENT_URI, new String[] {STATUS_EXT}, where, new String[] {url}, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return STATE_UNKNOWN;
    }

    /**
     * M: set mms cancel dowanload state.
     * @param uri the mms uri.
     * @param isCancelling true: is in cancelling; false: not.
     */
    private void setCancelDownloadState(Uri uri, boolean isCancelling) {
        Log.d("@M_" + TAG, "setCancelDownloadState()...");
        if (mCdh != null) {
            mCdh.setCancelDownloadState(uri, isCancelling);
        }
    }

    /**
     * M: get contentLocation for mms.
     * @param uri the mms uri.
     * @return the mms contentLocaion.
     */
    private String getContentLocation(final Uri uri) {
        String contentUrl = null;

        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
            uri, new String[]{Mms.CONTENT_LOCATION}, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    contentUrl = cursor.getString(0);
                    Log.d("@M_" + TAG, "getContentLocation(): contentUrl = " + contentUrl);
                }
            } finally {
                cursor.close();
            }
        }

        return contentUrl;
    }

    /**
     * M: abort mms http connection.
     * @param contentUrl the mms contentUrl.
     * @param uri the mms uri.
     */
    private void abortMmsHttp(String contentUrl, Uri uri) {
        Log.d("@M_" + TAG, "[abortMmsHttp], contentUrl:" + contentUrl + " uri:" + uri);
        mClientMap.remove(contentUrl);
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), uri,
            new String[] {Mms.SUBSCRIPTION_ID}, null, null, null);
        try {
            if (cursor != null) {
                if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                    int subId = cursor.getInt(0);
                    SmsManager manager = SmsManager.getSmsManagerForSubscriptionId(subId);
                    uri = uri.buildUpon().appendQueryParameter("cancel", "1").build();
                    manager.downloadMultimediaMessage(mContext, contentUrl, uri, null, null);
                }
            }
        } catch (SQLiteException e) {
            Log.e("@M_" + TAG, "[abortMmsHttp] failed, as " + e.getMessage());
        }

    }

    public void setWaitingDataCnxn(boolean isWaiting) {
        Log.d(TAG, "setWaitingDataCnxn(): mWaitingCnxn = " + isWaiting);
        mWaitingCnxn = isWaiting;
    }

    public boolean getWaitingDataCnxn() {
        Log.d(TAG, "getWaitingDataCnxn(): mWaitingCnxn = " + mWaitingCnxn);
        return mWaitingCnxn;
    }

}
