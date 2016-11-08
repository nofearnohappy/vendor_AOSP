/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.util;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduPersister;

import android.database.sqlite.SqliteWrapper;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;
import android.telephony.SubscriptionManager;
/// M:
import android.telephony.TelephonyManager;

import com.android.mms.MmsPluginManager;
import com.android.mms.MmsApp;
import com.mediatek.mms.callback.IDownloadManagerCallback;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.setting.MmsPreferenceActivity;


public class DownloadManager implements IDownloadManagerCallback{
    private static final String TAG = "DownloadManager";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    public static final int DEFERRED_MASK           = 0x04;

    public static final int STATE_UNKNOWN           = 0x00;
    public static final int STATE_UNSTARTED         = 0x80;
    public static final int STATE_DOWNLOADING       = 0x81;
    public static final int STATE_TRANSIENT_FAILURE = 0x82;
    public static final int STATE_PERMANENT_FAILURE = 0x87;
    public static final int STATE_PRE_DOWNLOADING   = 0x88;
    // TransactionService will skip downloading Mms if auto-download is off
    public static final int STATE_SKIP_RETRYING = 0x82;// M:[ALPS01772380] 0x89;

    private final Context mContext;
    private final Handler mHandler;
    private final SharedPreferences mPreferences;
    private boolean mAutoDownload;

    private final OnSharedPreferenceChangeListener mPreferencesChangeListener =
        new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (MmsPreferenceActivity.AUTO_RETRIEVAL.equals(key)
                    || MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING.equals(key)) {
                if (LOCAL_LOGV) {
                    Log.v(TAG, "Preferences updated.");
                }

                synchronized (sInstance) {
                    mAutoDownload = getAutoDownloadState(prefs);
                    if (LOCAL_LOGV) {
                        Log.v(TAG, "mAutoDownload ------> " + mAutoDownload);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mRoamingStateListener =
        new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
              //fix me, the intent will add two other extra value
                if (LOCAL_LOGV) {
                    Log.v(TAG, "Service state changed: " + intent.getExtras());
                }

                ServiceState state = ServiceState.newFromBundle(intent.getExtras());
                boolean isRoaming = state.getRoaming();
                synchronized (sInstance) {
                    int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                    mAutoDownload = getAutoDownloadState(mPreferences, isRoaming, subId);

                }
            }
        }
    };

    private static DownloadManager sInstance;

    private DownloadManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);

        context.registerReceiver(
                mRoamingStateListener,
                new IntentFilter(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED));

        mAutoDownload = getAutoDownloadState(mPreferences);
        if (LOCAL_LOGV) {
            Log.v(TAG, "mAutoDownload ------> " + mAutoDownload);
        }
    }

    public boolean isAuto() {
        return mAutoDownload;
    }

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "DownloadManager.init()");
        }

        if (sInstance != null) {
            Log.w(TAG, "Already initialized.");
        }
        sInstance = new DownloadManager(context);
    }

    public static DownloadManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Uninitialized.");
        }
        return sInstance;
    }

    static boolean getAutoDownloadState(SharedPreferences prefs) {
        return getAutoDownloadState(prefs, isRoaming());
    }

    static boolean getAutoDownloadState(SharedPreferences prefs, boolean roaming) {
        boolean autoDownload = prefs.getBoolean(
                MmsPreferenceActivity.AUTO_RETRIEVAL, true);

        if (LOCAL_LOGV) {
            Log.v(TAG, "auto download without roaming -> " + autoDownload);
        }

        if (autoDownload) {
            boolean alwaysAuto = prefs.getBoolean(
                    MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING, false);

            if (LOCAL_LOGV) {
                Log.v(TAG, "auto download during roaming -> " + alwaysAuto);
            }

            if (!roaming || alwaysAuto) {
                return true;
            }
        }
        return false;
    }

    static boolean isRoaming() {
        return isRoaming(SubscriptionManager.getDefaultSmsSubId());
    }

    static boolean isRoaming(int subId) {
        TelephonyManager teleMgr = TelephonyManager.getDefault();
        if (teleMgr == null) {
            return false;
        }
        return teleMgr.isNetworkRoaming(subId);
    }

    public void markState(final Uri uri, int state) {
        // Notify user if the message has expired.
        try {
            ///M: modify for ALPS00437262 &{
            Log.e(TAG, "markState: uri = " + uri + ", state = " + state);
            GenericPdu pdu = PduPersister.getPduPersister(mContext).load(uri);
            if (!(pdu instanceof NotificationInd)) {
                Log.e(TAG, "markState: pdu type is not NotificationInd.");
                return;
            }
            NotificationInd nInd = (NotificationInd) pdu;
            /// @}
            if ((nInd.getExpiry() < System.currentTimeMillis() / 1000L)
                    && (state == STATE_DOWNLOADING || state == STATE_PRE_DOWNLOADING)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, R.string.service_message_not_found,
                                Toast.LENGTH_LONG).show();
                    }
                });
                SqliteWrapper.delete(mContext, mContext.getContentResolver(), uri, null, null);
                return;
            }
        } catch(MmsException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        // Notify user if downloading permanently failed.
        // change for op
        if (!mAutoDownload) {
            state |= DEFERRED_MASK;
        }
        // Use the STATUS field to store the state of downloading process
        // because it's useless for M-Notification.ind.
        ContentValues values = new ContentValues(1);
        values.put(Mms.STATUS, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    uri, values, null, null);
    }

    public void showErrorCodeToast(int errorStr) {
        final int errStr = errorStr;
        mHandler.post(new Runnable() {
            public void run() {
                try {
                    Toast.makeText(mContext, errStr, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG,"Caught an exception in showErrorCodeToast");
                }
            }
        });
    }

    private String getMessage(Uri uri) throws MmsException {
        NotificationInd ind = (NotificationInd) PduPersister
                .getPduPersister(mContext).load(uri);

        EncodedStringValue v = ind.getSubject();
        String subject = (v != null) ? v.getString()
                : mContext.getString(R.string.no_subject);

        v = ind.getFrom();
        String from = (v != null)
                ? Contact.get(v.getString(), false).getName()
                : mContext.getString(R.string.unknown_sender);

        return mContext.getString(R.string.dl_failure_notification, subject, from);
    }

    public int getState(Uri uri) {
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            uri, new String[] {Mms.STATUS}, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0) & ~DEFERRED_MASK;
                }
            } finally {
                cursor.close();
            }
        }
        return STATE_UNSTARTED;
    }

    /// M:Code analyze 002,New methods,add for folder mode use to download @{
    public  void setState(final Uri uri, int state) {
        // Notify user if the message has expired.
        try {
            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(mContext)
                    .load(uri);
            if ((nInd.getExpiry() < System.currentTimeMillis() / 1000L)
                && (state == STATE_DOWNLOADING)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, R.string.dl_expired_notification,
                                Toast.LENGTH_LONG).show();
                    }
                });
                SqliteWrapper.delete(mContext, mContext.getContentResolver(), uri, null, null);
                return;
            }
        } catch (MmsException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        // Use the STATUS field to store the state of downloading process
        // because it's useless for M-Notification.ind.
        ContentValues values = new ContentValues(1);
        values.put(Mms.STATUS, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    uri, values, null, null);
    }
    /// @}

    /// M:Code analyze 003, add for msim enhancement,judge if it is auto download mode @{
    private boolean getAutoDownloadState(SharedPreferences prefs, int subId) {
        return getAutoDownloadState(prefs, isRoaming(subId), subId);
    }

    private boolean getAutoDownloadState(SharedPreferences prefs, boolean roaming, int subId) {
        /// M:ALPS00717041, Single SIM no SIM id prefix @{
        boolean autoDownload = true;
        autoDownload = prefs.getBoolean(Long.toString(subId) + "_" +
        MmsPreferenceActivity.AUTO_RETRIEVAL, true);
        MmsLog.d(MmsApp.TXN_TAG, "getAutoDownloadState, roaming=" + roaming);
        /// @}
        MmsLog.d(MmsApp.TXN_TAG, "subId " + subId + " auto download without roaming -> "
                + autoDownload);
        if (autoDownload) {
            boolean alwaysAuto = prefs.getBoolean(Long.toString(subId) + "_" +
                    MmsPreferenceActivity.RETRIEVAL_DURING_ROAMING, false);
            MmsLog.d(MmsApp.TXN_TAG, "subId " + subId + " auto download during roaming -> "
                    + alwaysAuto);
            if (!roaming || alwaysAuto) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuto(int subId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return getAutoDownloadState(preferences, subId);
    }

    /// @}

    /// M:Code analyze 004,add for msim,mark download state,and update it into database @{
    public void markState(final Uri uri, int state, int subId) {
        // Notify user if the message has expired.
        try {
            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(mContext)
                    .load(uri);
            if ((nInd.getExpiry() < System.currentTimeMillis() / 1000L)
                && (state == STATE_DOWNLOADING)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, R.string.service_message_not_found,
                                Toast.LENGTH_LONG).show();
                    }
                });
                SqliteWrapper.delete(mContext, mContext.getContentResolver(), uri, null, null);
                return;
            }
        } catch (MmsException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        // Notify user if downloading permanently failed.
        if (state == STATE_PERMANENT_FAILURE) {
            mHandler.post(new Runnable() {
                public void run() {
                    try {
                        Toast.makeText(mContext, getMessage(uri),
                                Toast.LENGTH_LONG).show();
                    } catch (MmsException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });
        } else if (!isAuto(subId)) {
            state |= DEFERRED_MASK;
        }

        // Use the STATUS field to store the state of downloading process
        // because it's useless for M-Notification.ind.
        ContentValues values = new ContentValues(1);
        values.put(Mms.STATUS, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    uri, values, null, null);
    }
    /// @}

    @Override
    public void markStateCallback(Uri uri, int state) {
        // TODO Auto-generated method stub
        markState(uri, state);
    }
}
