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

package com.android.mms;

import java.util.HashMap;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.location.Country;
import android.location.CountryDetector;
import android.location.CountryListener;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.layout.LayoutManager;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.MmsSystemEventReceiver;
import com.mediatek.mms.ui.DialogModeActivity;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.DraftCache;
import com.android.mms.util.MuteCache;
import com.android.mms.util.PduLoaderManager;
import com.android.mms.util.RateController;
import com.android.mms.util.ThumbnailManager;


/// M:
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.Toast;

import com.android.mms.util.MmsLog;
import com.mediatek.drm.OmaDrmClient;

import java.util.Locale;

import android.provider.Telephony;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.nmsg.util.IpMessageNmsgUtil;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.opmsg.util.OpMessageUtils;


///M: add for ALPS00444082 @{
/// @}
/// M: ALPS00440523, set service to foreground @ {
import android.content.Intent;

import com.android.mms.transaction.TransactionService;
/// @}
import com.mediatek.mms.callback.IMmsAppCallback;
import com.mediatek.telephony.TelephonyManagerEx;

public class MmsApp extends Application implements IMmsAppCallback {
    public static final String LOG_TAG = "Mms";

    private SearchRecentSuggestions mRecentSuggestions;
    private TelephonyManager mTelephonyManager;
    private CountryDetector mCountryDetector;
    private CountryListener mCountryListener;
    private String mCountryIso;
    private static MmsApp sMmsApp = null;
    private PduLoaderManager mPduLoaderManager;
    private ThumbnailManager mThumbnailManager;
    private OmaDrmClient mDrmManagerClient;
    /// M: fix bug ALPS00987075, Optimize first launch time @{
    private Context mContext;
    /// @}

    /// M: for toast thread
    public static final String TXN_TAG = "Mms/Txn";
    public static final int MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL = 2;
    public static final int MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION = 4;
    public static final int MSG_MMS_TOO_BIG_TO_DOWNLOAD = 6;
    public static final int MSG_MMS_CAN_NOT_SAVE = 8;
    public static final int MSG_MMS_CAN_NOT_OPEN = 10;
    public static final int MSG_DONE = 12;
    public static final int EVENT_QUIT = 100;
    private static HandlerThread mToastthread = null;
    private static ToastHandler mToastHandler = null;

    private final BroadcastReceiver mLocaleChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                MmsLog.d(MmsApp.TXN_TAG, "MmsApp  ACTION_LOCALE_CHANGED received");
                if (MmsConfig.getInitQuickText()) {
                    return;
                }
                MmsConfig.getQuicktexts().clear();
                MmsConfig.getQuicktextsId().clear();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        PDebug.Start("MmsApp onCreate");
        /// M: ALPS00440523, when run mms ap will scan and restart pending mms @{
        MmsLog.d(MmsApp.TXN_TAG, "MmsApp.onCreate");
        /// @}

        if (Log.isLoggable(LogTag.STRICT_MODE_TAG, Log.DEBUG)) {
            // Log tag for enabling/disabling StrictMode violation log. This will dump a stack
            // in the log that shows the StrictMode violator.
            // To enable: adb shell setprop log.tag.Mms:strictmode DEBUG
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        }

        sMmsApp = this;
        // add for ipmessage
        IpMessageUtils.onIpMmsCreate(this);

        // Load the default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Figure out the country *before* loading contacts and formatting numbers
        mCountryDetector = (CountryDetector) getSystemService(Context.COUNTRY_DETECTOR);
        mCountryListener = new CountryListener() {
            @Override
            public synchronized void onCountryDetected(Country country) {
                mCountryIso = country.getCountryIso();
            }
        };
        mCountryDetector.addCountryListener(mCountryListener, getMainLooper());
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        mContext = getApplicationContext();
        /// @}
        OpMessageUtils.init(this);
        OpMessageUtils.getOpMessagePlugin().getOpMmsAppExt().onCreate(this);
        MmsConfig.init(this);
        MessageUtils.init(this);
        MmsPluginManager.initPlugins(this);
        if (PermissionCheckUtil.checkRequiredPermissions(this)) {
            Contact.init(this);
            DraftCache.init(this);
        }
        /// M: comment this
        Conversation.init(this);
        DownloadManager.init(this);
        RateController.init(this);
        LayoutManager.init(this);
        MessagingNotification.init(this);
        /// M: @{
        InitToastThread();
        /// @}

        /// M: ALPS00440523, when run mms ap will scan and restart pending mms @{
        MmsSystemEventReceiver.delayWakeupService(this);
        /// @}

        IpMessageNmsgUtil.nmsgCheckService();
        registerLocaleChangedReceiver();
        PDebug.End("MmsApp onCreate");
    }

    synchronized public static MmsApp getApplication() {
        return sMmsApp;
    }

    @Override
    public void onTerminate() {
        MmsLog.d(LOG_TAG, "MmsApp#onTerminate");
        unregisterLocaleChangedReceiver();
        mCountryDetector.removeCountryListener(mCountryListener);
    }

    @Override
    public void onLowMemory() {
        MmsLog.d(LOG_TAG, "MmsApp#onLowMemory");
        super.onLowMemory();
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        if (mPduLoaderManager != null) {
            mPduLoaderManager.onLowMemory();
        }
        if (mThumbnailManager != null) {
            mThumbnailManager.onLowMemory();
        }
        /// @}
    }

    public PduLoaderManager getPduLoaderManager() {
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        if (mPduLoaderManager == null) {
            mPduLoaderManager = new PduLoaderManager(mContext);
        }
        /// @}
        return mPduLoaderManager;
    }

    public ThumbnailManager getThumbnailManager() {
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        if (mThumbnailManager == null) {
            mThumbnailManager = new ThumbnailManager(mContext);
        }
        /// @}
        return mThumbnailManager;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LayoutManager.getInstance().onConfigurationChanged(newConfig);
    }

    /**
     * @return Returns the TelephonyManager.
     */
    public TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager)getApplicationContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }

    /**
     * Returns the content provider wrapper that allows access to recent searches.
     * @return Returns the content provider wrapper that allows access to recent searches.
     */
    public SearchRecentSuggestions getRecentSuggestions() {
        /*
        if (mRecentSuggestions == null) {
            mRecentSuggestions = new SearchRecentSuggestions(this,
                    SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
        }
        */
        return mRecentSuggestions;
    }

    /// Google JB MR1.1 patch. This function CAN return null.
    public String getCurrentCountryIso() {
        if (mCountryIso == null) {
            Country country = mCountryDetector.detectCountry();

            if (country == null) {
                // Fallback to Locale if there are issues with CountryDetector
                return Locale.getDefault().getCountry();
            }

            mCountryIso = country.getCountryIso();
        }
        return mCountryIso;
    }

    public OmaDrmClient getDrmManagerClient() {
        if (mDrmManagerClient == null) {
            mDrmManagerClient = new OmaDrmClient(getApplicationContext());
        }
        return mDrmManagerClient;
    }

    /// M: a handler belong to UI thread.
    private void InitToastThread() {
        if (null == mToastHandler) {
            mToastHandler = new ToastHandler();
        }
    }

    public static ToastHandler getToastHandler() {
        return mToastHandler;
    }

    public final class ToastHandler extends Handler {
        public ToastHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            MmsLog.d(MmsApp.TXN_TAG, "Toast Handler handleMessage :" + msg);

            switch (msg.what) {
                case EVENT_QUIT: {
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_QUIT");
                    getLooper().quit();
                    return;
                }

                case MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL: {
                    Toast.makeText(sMmsApp, R.string.download_failed_due_to_full_memory, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION: {
                    Toast.makeText(sMmsApp, R.string.transmission_transiently_failed, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_TOO_BIG_TO_DOWNLOAD: {
                    Toast.makeText(sMmsApp, R.string.mms_too_big_to_download, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_CAN_NOT_SAVE: {
                    Toast.makeText(sMmsApp, R.string.cannot_save_message, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_CAN_NOT_OPEN: {
                    String str = sMmsApp.getResources().getString(R.string.unsupported_media_format, (String) msg.obj);
                    Toast.makeText(sMmsApp, str, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_DONE: {
                    Toast.makeText(sMmsApp, R.string.finish, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    /// APARTODO
    /// M: new feature for regional phone.
    /// modify values about creation mode and sms service center. @{
    String mCreationMode = "";
    String mSmsServiceCenter = "";
    public void setSmsValues(HashMap<String, String> values) {
        mCreationMode = values.get("creationmode");
        setCreactionMode(mCreationMode);
        mSmsServiceCenter = values.get("servicecenter");
        setSmsServiceCenter(0, mSmsServiceCenter);
    }

    private void setCreactionMode(String mode) {
        MmsLog.d(MmsApp.TXN_TAG, "setCreactionMode, mode=" + mode);
        Context context = getBaseContext();
        if (!TextUtils.isEmpty(mode)) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString("pref_key_mms_creation_mode", mode);
            editor.commit();
        }
    }

    private void setSmsServiceCenter(final int subId, final String number) {
        MmsLog.d(MmsApp.TXN_TAG, "setSmsCenter,  subId=" + subId + ", number=" + number);
        new Thread(new Runnable() {
            public void run() {
               TelephonyManagerEx.getDefault().setScAddress(subId, number);
            }
        }).start();
    }

    private void registerLocaleChangedReceiver() {
        IntentFilter localeChangedFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mLocaleChangedReceiver, localeChangedFilter);
    }

    private void unregisterLocaleChangedReceiver() {
        unregisterReceiver(mLocaleChangedReceiver);
    }

    public void registerSmsStateReceiver() {
        // IntentFilter intentFilter = new
        // IntentFilter(Telephony.Sms.Intents.SMS_STATE_CHANGED_ACTION);
        // this.registerReceiver(mSmsStateReceiver, intentFilter);
    }

    // private BroadcastReceiver mSmsStateReceiver = new BroadcastReceiver() {
    // @Override
    // public void onReceive(Context context, Intent intent) {
    // String action = intent.getAction();
    // if (action.equals(Telephony.Sms.Intents.SMS_STATE_CHANGED_ACTION)) {
    // boolean isReady = intent.getBooleanExtra("ready", false);
    // if (isReady) {
    // int subId = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
    // if (subId >= 1) {
    // if (mMmsSettingsPlugin != null && TextUtils.isEmpty(mSmsServiceCenter)) {
    // mSmsServiceCenter = mMmsSettingsPlugin.getSmsServiceCenter();
    // }
    // MmsLog.d(MmsApp.TXN_TAG,
    // "mSmsStateReceiver#onReceive, mSmsServiceCenter=" + mSmsServiceCenter);
    // if (!TextUtils.isEmpty(mSmsServiceCenter)) {
    // setSmsServiceCenter(subId, mSmsServiceCenter);
    // }
    // }
    // MmsApp.this.unregisterReceiver(mSmsStateReceiver);
    // }
    // }
    // }
    // };
    /// @}

    public void initMuteCache() {
        MuteCache.init(this);
    }

    public void onRequestPermissionsResult() {
        Contact.init(this);
        DraftCache.init(this);
    }
}
