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

package com.mediatek.rcs.pam.util;

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
import android.util.Log;
import android.widget.Toast;

public class DownloadManager {
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
    public static final int STATE_SKIP_RETRYING     = 0x89;

    private final Context mContext;
    private final Handler mHandler;
    private final SharedPreferences mPreferences;
    private boolean mAutoDownload;

    private final OnSharedPreferenceChangeListener mPreferencesChangeListener =
        new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        	
        }
    };

    private final BroadcastReceiver mRoamingStateListener =
        new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        }
    };

    private static DownloadManager sInstance;

    private DownloadManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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

        return false;
    }

    static boolean isRoaming() {
        return false;
    }

    public void markState(final Uri uri, int state) {
    	
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

    private String getMessage(Uri uri) {

        return null;
    }

    public int getState(Uri uri) {
        return STATE_UNSTARTED;
    }
}
