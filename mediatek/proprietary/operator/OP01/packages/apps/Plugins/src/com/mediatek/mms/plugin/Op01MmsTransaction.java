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

import android.app.Notification;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.telephony.TelephonyManager;
import android.util.Log;

/// M: ALPS00452618, set special HTTP retry handler for CMCC FT @
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
/// @}


/**
 * Op01MmsTransaction.
 *
 */
public class Op01MmsTransaction extends ContextWrapper {
    private static final String TAG = "Mms/Op01MmsTransactionExt";

    private static final int sMaxFailTime = 3;
    private static final int sSC504 = 504;

    private Context mContext = null;
    private int mServerFailCount = 0;
    private long mLastSimId = -1;

    /**
     * Construction.
     * @param context Context.
     */
    public Op01MmsTransaction(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * setMmsServerStatusCode.
     * @param code int
     */
    public synchronized void setMmsServerStatusCode(int code) {
        Log.d(TAG, "setMmsServerStatusCode, code=" + code);
        updateServerFailRecord(code);
    }

    private void updateServerFailRecord(int code) {
        Log.d(TAG, "updateServerFailRecord, code=" + code + ", count=" + mServerFailCount);
        if (!isConcernErrorCode(code)) {
            mServerFailCount = 0;
            return;
        }

        mServerFailCount++;
    }

    private boolean isConcernErrorCode(int code) {
        if (code >= 400 && code < 600) {
            return true;
        }
        return false;
    }

    /**
     * updateConnection.
     * @return true if update successful.
     */
    public synchronized boolean updateConnection() {
        Log.d(TAG, "updateConnection");

        boolean ret = false;

        if (mServerFailCount >= sMaxFailTime) {
            ret = doUpdateConnection();
            mServerFailCount = 0;
        }
        Log.d(TAG, "updateConnection ret=" + ret);
        return ret;
    }

    private boolean doUpdateConnection() {
        Log.d(TAG, "doUpdateConnection");
        if (isDataConnectionEnabled()) {
            mLastSimId = getLastDataSimId();
            closeDataConnection();

            final Object object = new Object();
            synchronized (object) {
                try {
                    Log.d(TAG, "before wait");
                    object.wait(500);
                    Log.d(TAG, "after wait");
                } catch (InterruptedException ex) {
                    Log.e(TAG, "wait has been intrrupted", ex);
                }
            }

            openDataConnection();
            return true;
        }
        return false;
    }

    private boolean isDataConnectionEnabled() {
        Log.d(TAG, "isDataConnectionEnabled");

        boolean enabled = false;
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            telephony.getDataEnabled();
        }
//        }

        Log.d(TAG, "enabled=" + enabled);
        return enabled;
    }

    private long getLastDataSimId() {
        Log.d(TAG, "getLastDataSimId");

        long simId = -1;

        if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
            simId = Settings.System.getLong(mContext.getContentResolver(),
                        Settings.System.GPRS_CONNECTION_SIM_SETTING,
                        Settings.System.DEFAULT_SIM_NOT_SET);
            Log.d(TAG, "simId=" + simId);
        }
        return simId;
    }

    private void closeDataConnection() {
        Log.d(TAG, "closeDataConnection");
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            telephony.setDataEnabled(false);
        }
    }

    private void openDataConnection() {
        Log.d(TAG, "openDataConnection, mLastSimId=" + mLastSimId);
        if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
            Log.d(TAG, "gemini");
            Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
            intent.putExtra("simid", mLastSimId);
            mContext.sendBroadcast(intent);
        } else {
            TelephonyManager telephony =
                        (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephony != null) {
                telephony.setDataEnabled(true);
            } else {
                Log.d(TAG, "cm null");
            }
        }
    }

    /// M: ALPS00452618, set special HTTP retry handler for CMCC FT @
    /**
     * getHttpRequestRetryHandler.
     * @return DefaultHttpRequestRetryHandler
     */
    public DefaultHttpRequestRetryHandler getHttpRequestRetryHandler() {
        Log.d(TAG, "getHttpRequestRetryHandler");
        return new Op01HttpRequestRetryHandler(mContext, 1, true);
    }
    /// @}

    /// M: ALPS00440523, set service to foreground @
    /**
     * Set service to foreground.
     *
     * @param service         Service that need to be foreground
     */
    public void startServiceForeground(Service service) {
        Log.d(TAG, "startServiceForeground");
        ///M: remove for ALPS01241119. notification will show on statusbar even icon id = 0. @{
        Notification noti = new Notification(0, null, System.currentTimeMillis());
        noti.flags |= Notification.FLAG_NO_CLEAR;
        noti.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        if (service != null) {
            service.startForeground(1, noti);
        }
        /// @}
    }

    /**
     * Set service to foreground.
     *
     * @param service         Service that need stop to be foreground
     */
    public void stopServiceForeground(Service service) {
        Log.d(TAG, "stopServiceForeground");
        ///M: remove for ALPS01241119.  @{
        if (service != null) {
            service.stopForeground(true);
        }
        /// @}
    }
    /// @}

    /// M: ALPS00440523, set property @ {
    /**
     * setSoSendTimeoutProperty.
     */
    public void setSoSendTimeoutProperty() {
        Log.d(TAG, "setSoSendTimeoutProperty");
        System.setProperty("SO_SND_TIMEOUT", "1");
    }
    /// @}

    /// M: ALPS00545779, for FT, restart pending receiving mms @ {
    /**
     * isPendingMmsNeedRestart.
     * @param pduUri Uri
     * @param failureType int
     * @return true if restart.
     */
    public boolean isPendingMmsNeedRestart(Uri pduUri, int failureType) {
        Log.d(TAG, "isPendingMmsNeedRestart, uri=" + pduUri);

        final int pduColumnStatus = 2;
        final String[] pduProjection = new String[] {
            Mms.MESSAGE_BOX,
            Mms.MESSAGE_ID,
            Mms.STATUS,
        };
        Cursor c = null;
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            c = contentResolver.query(pduUri, pduProjection, null, null, null);

            if ((c == null) || (c.getCount() != 1) || !c.moveToFirst()) {
                Log.d(TAG, "Bad uri");
                return true;
            }

            int status = c.getInt(pduColumnStatus);
            Log.v(TAG, "status" + status);

            /* This notification is not processed yet, so need restart*/
            if (status == 0) {
                return true;
            }
            /* DEFERRED_MASK is not set, it is auto download*/
            if ((status & 0x04) == 0) {
                return isTransientFailure(failureType);
            }
            /* Reach here means it is manully download*/
            return false;
        } catch (SQLiteException e) {
            Log.e(TAG, "Catch a SQLiteException when query: ", e);
            return isTransientFailure(failureType);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static boolean isTransientFailure(int type) {
        Log.d(TAG, "isTransientFailure, type=" + type);
        return (type > MmsSms.NO_ERROR && type < MmsSms.ERR_TYPE_GENERIC_PERMANENT);
    }
    /// @}
}


