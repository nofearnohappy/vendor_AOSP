/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.rcs.contacts.networkcontacts;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.mediatek.rcs.contacts.R;
import com.mediatek.rcs.contacts.networkcontacts.ui.HomeActivity;

import org.gsma.joyn.JoynServiceConfiguration;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

/**
 *
 */
public class SyncService extends Service {
    private static final String TAG = "NetworkContacts::SyncService";

    private SyncScheduler mSyncScheduler = null;
    private SyncBinder mBinder = null;
    private SyncHandler mHandler = null;
    private ContactObserver mContactObserver = null;
    private SyncRequest.SyncNotify mNotify = null;
    private HashSet<SyncRequest.SyncNotify> mAutoListeners = new HashSet<SyncRequest.SyncNotify>();
    private int mLatestId = -1;
    private boolean mImmediateBackupEnabled = true;

    /*
     * message send by immediate backup
     */
    private final static int MSG_AUTO_BACKUP = 0;
    /*
     * arg1 is the new state
     */
    private final static int MSG_STATE_CHANGE = 1;
    /*
     * arg1 is the start id
     */
    private final static int MSG_STOP_SELF = 2;

    /*
     * arg1 is the start id
     */
    private final static int MSG_UNBLOCK_IMM = 3;

    /*
     * notification id of auto backup
     */
    private final static int ID_NOTIFICATION_AUTOBACKUP = 2;

    /**
     * Sync binder.
     *
     */
    public class SyncBinder extends Binder {
        /**
         * @return SyncService
         */
        public SyncService getService() {
            return SyncService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind ");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind ");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mBinder = new SyncBinder();
        mHandler = new SyncHandler(this);
        mNotify = new DefNotify(mHandler);
        mSyncScheduler = new SyncScheduler(this);

        configService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mLatestId = startId;
        handleCommand(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mBinder = null;
        mSyncScheduler = null;
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        unregisterContactsObserver();
        stopForeground(true);
        super.onDestroy();
    }

    private void handleCommand(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "action = " + action);
        if (action == null) {
            return;
        } else if (action.equalsIgnoreCase(Const.Action.AUTO_BACKUP)) {
            SyncRequest request = new SyncRequest(SyncRequest.SYNC_BACKUP,
                    mNotify, true);
            /* check if auto backup is allowed */
            SettingsSharedPreference preference = new SettingsSharedPreference(this);
            NetworkStatusManager nm = new NetworkStatusManager(this);
            if (preference.isWifiBackupOnly() && !nm.isWiFiConnected()) {
                mNotify.onStateChange(SyncRequest.SyncNotify.SYNC_STATE_ERROR, request);
            } else {
                mSyncScheduler.syncStart(request);
            }
        } else if (action
                .equalsIgnoreCase(Const.Action.REGISTER_CONTACTS_OBSERVER)) {
            enableImmediateBackup();
        } else if (action
                .equalsIgnoreCase(Const.Action.UNREGISTER_CONTACTS_OBSERVER)) {
            disableImmediateBackup();
        } else if (action.equalsIgnoreCase(Const.Action.CHECK_RESTORE)) {
            checkRestoreResult();
        } else {
            Log.d(TAG, "unknown action: " + action);
        }
    }

    private void checkRestoreResult() {
        SyncRequest request = new SyncRequest(SyncRequest.CHECK_RESTORE_RESULT,
                mNotify, false);
        mSyncScheduler.syncStart(request);
    }

    /**
     * sync with the request.
     *
     * @param request
     *            sync request
     */
    public void sync(SyncRequest request) {
        mSyncScheduler.syncStart(request);
    }

    /**
     * Add a listener to listen the state of auto backup.
     *
     * @param notify
     *            listener.
     */
    public void addAutoBackupListener(SyncRequest.SyncNotify notify) {
        mAutoListeners.add(notify);

        if (mSyncScheduler.isSyncing()) {
            SyncRequest request = mSyncScheduler.getCurrentRequest();
            if (request != null) {
                if (request.mSyncType == SyncRequest.SYNC_BACKUP) {
                    notify.onStateChange(
                            SyncRequest.SyncNotify.SYNC_STATE_START, request);
                }
            }
        }
    }

    /**
     * Remove a listener to state of auto backup.
     *
     * @param notify
     *            listener.
     */
    public void removeAutoBackupListener(SyncRequest.SyncNotify notify) {
        mAutoListeners.remove(notify);
    }

    /**
     * Block immediate backup temperarily to avoid auto
     * backup triggered by restore.
     */
    public void blockImmBackup() {
        mImmediateBackupEnabled = false;
    }

    /**
     * Unblock the immediate backup be blocked.
     * Delay 30s to avoid restore triggered auto backup.
     */
    public void unblockImmBackup() {
        if (isImmediateBackup()) {
            mHandler.sendEmptyMessageDelayed(MSG_UNBLOCK_IMM, 30000);
        } else {
            mImmediateBackupEnabled = true;
        }
    }

    /**
     * Clear all backup task , if RCS switch is off
     *
     */
    public void stopAutoSync() {
        AutoSyncManager autoSyncManager = new AutoSyncManager(this);
        autoSyncManager.stopAutoSync();
    }

    /**
     * internal handler.
     *
     */
    private static class SyncHandler extends Handler {
        private WeakReference<SyncService> mService;

        public SyncHandler(SyncService service) {
            mService = new WeakReference<SyncService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            SyncService service = mService.get();
            Log.d(TAG, String.format("handleMessage %d", msg.what));

            if (service == null) {
                Log.d(TAG, "service has been deleted!!!");
                return;
            }

            switch (msg.what) {
            case MSG_AUTO_BACKUP:
                SyncRequest request = new SyncRequest(SyncRequest.SYNC_BACKUP,
                        service.mNotify, true);
                service.mSyncScheduler.syncStart(request);
                break;

            case MSG_STATE_CHANGE:
                SyncRequest req = (SyncRequest) msg.obj;
                if (req.mSyncType == SyncRequest.SYNC_BACKUP) {
                    service.showAutoSyncNotification(msg.arg1);
                }
                for (SyncRequest.SyncNotify l : service.mAutoListeners) {
                    l.onStateChange(msg.arg1, (SyncRequest) msg.obj);
                }
                if (msg.arg1 == SyncRequest.SyncNotify.SYNC_STATE_ERROR
                        || msg.arg1 == SyncRequest.SyncNotify.SYNC_STATE_SUCCESS) {
                    // delay a little to avoid then new command arrives in short
                    // time
                    service.mHandler.sendEmptyMessageDelayed(MSG_STOP_SELF,
                            20000);
                }
                break;

            case MSG_STOP_SELF:
                service.stopServiceSelf();
                break;

            case MSG_UNBLOCK_IMM:
                service.mImmediateBackupEnabled = true;
                break;

            default:
                break;
            }
        }

    };

    /**
     * Configure service according to settings.
     */
    private void configService() {
        SettingsSharedPreference sspPreference = new SettingsSharedPreference(
                this);
        if (!JoynServiceConfiguration.isServiceActivated(this)) {
            Log.d(TAG, "RCS is off, clearAllBackupTask!");
            stopAutoSync();
        } else if (sspPreference.isAutoBackup()
                && (sspPreference.getAutoBackupType()
                        == SettingsSharedPreference.BACKUP_TYPE_IMMEDIATELY)) {
            Log.d(TAG, "RCS is on, BACKUP_TYPE_IMMEDIATELY!");
            enableImmediateBackup();
        }
    }

    private void enableImmediateBackup() {
        registerContactsObserver();

        /* stay in foreground */
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        startForeground(1, notification);
    }

    private void disableImmediateBackup() {
        unregisterContactsObserver();
        mHandler.removeMessages(MSG_AUTO_BACKUP);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ID_NOTIFICATION_AUTOBACKUP);
        stopForeground(true);

        /* stop service */
        stopServiceSelf();
    }

    private boolean isImmediateBackup() {
        return mContactObserver != null;
    }

    /**
     * wrapper of stopSelf.
     */
    private void stopServiceSelf() {
        Log.d(TAG, "+stopServiceSelf...");

        if (isImmediateBackup()) {
            Log.d(TAG,
                    "-stopServiceSelf: Cannot stop service as immediate backup enabled");
            return;
        }

        if (mSyncScheduler.isIdle()) {
            Log.d(TAG, "stop service!");
            boolean result = stopSelfResult(mLatestId);
            Log.d(TAG, String.format("stop %d return ", mLatestId) + result);
        }

        Log.d(TAG, "-stopServiceSelf");
    }

    private void registerContactsObserver() {
        if (mContactObserver == null) {
            mContactObserver = new ContactObserver(mHandler);
            getContentResolver().registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI, true,
                    mContactObserver);
        }

    }

    private void unregisterContactsObserver() {
        if (mContactObserver != null) {
            getContentResolver().unregisterContentObserver(mContactObserver);
            ContactsChecker.getInstance(this).cancelCheck();
        }

        mContactObserver = null;
    }

    /**
     * Default state notification. Is used by auto backup or all the services
     * launched by startService.
     *
     */
    private static class DefNotify implements SyncRequest.SyncNotify {

        private Handler mHandler;

        public DefNotify(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void onStateChange(int state, SyncRequest request) {
            Message msg;
            if (request.mSyncType == SyncRequest.CHECK_RESTORE_RESULT) {
                msg = Message.obtain(mHandler, MSG_STOP_SELF, 0, 0);
            } else {
                msg = Message.obtain(mHandler, MSG_STATE_CHANGE, state, 0, request);
            }
            msg.sendToTarget();
        }

    }

    private void showAutoSyncNotification(int state) {
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        Intent mIntent = new Intent(this, HomeActivity.class);
        PendingIntent mPendingIntent = PendingIntent.getActivity(this, 0,
                mIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Builder builder = new Notification.Builder(this)
        .setContentIntent(mPendingIntent).setDefaults(BIND_AUTO_CREATE)
        .setSmallIcon(R.drawable.ic_auto_backup_notify);

        String msg = null;
        boolean autoCancel = true;
        if (state == SyncRequest.SyncNotify.SYNC_STATE_START) {
            msg = getString(R.string.auto_backuping);
            autoCancel = false;
            builder.setOngoing(true);
        } else if (state == SyncRequest.SyncNotify.SYNC_STATE_SUCCESS) {
            msg = getString(R.string.auto_backup_success);
        } else if (state == SyncRequest.SyncNotify.SYNC_STATE_ERROR) {
            msg = getString(R.string.auto_backup_fail);
        } else {
            Log.i(TAG, "unknown state code" + state);
            return;
        }

        builder.setAutoCancel(autoCancel).setContentText(msg);
        builder.setAutoCancel(autoCancel).setContentTitle(getString(R.string.backup_restore_lable));
        notificationManager.notify(ID_NOTIFICATION_AUTOBACKUP, builder.build());
    }

    /**
     * Observe contacts' modification to implements backup on modification.
     *
     */
    private class ContactObserver extends ContentObserver {
        public ContactObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.i(TAG, "ContactObserver " + mImmediateBackupEnabled);
            Log.i(TAG, "uri = " + uri);
            if (mImmediateBackupEnabled) {
                ContactsChecker.getInstance(SyncService.this).checkVersion();
            }
        }
    }

    /**
     * @author MTK80963
     * Check if the content of contacts has any changes from
     * last modification.
     */
    static class ContactsChecker {
        private WeakReference<SyncService> mService;
        private HandlerThread mThread = null;
        private CheckerHandler mHandler = null;
        private static final int MSG_CHECK_VERSION = 100;
        private static final int MSG_UPDATE_VERSION = 101;
        private static final int MSG_CANCEL_CHECK = 102;
        private static ContactsChecker sInstance = null;

        private ContactsChecker(SyncService service) {
            /* Create Worker thread */
            mThread = new HandlerThread("ContactsVersionThread");
            mThread.start();
            mHandler = new CheckerHandler(mThread.getLooper());
            mService = new WeakReference<SyncService>(service);
        }

        /**
         * @param service sync service.
         * @return contacts checker instance.
         */
        public static synchronized ContactsChecker getInstance(SyncService service) {
            if (sInstance == null) {
                sInstance = new ContactsChecker(service);
            } else {
                Log.d(TAG,
                        String.format("%s : %s", service,
                                sInstance.mService.get()));
                if (service != sInstance.mService.get()) {
                    sInstance.mService = new WeakReference<SyncService>(service);
                }
            }

            return sInstance;
        }

        /**
         * Check if contacts changes and start back on changes.
         */
        public void checkVersion() {
            Log.i(TAG, "checkVersion");
            mHandler.removeMessages(MSG_CHECK_VERSION);
            mHandler.sendEmptyMessage(MSG_CANCEL_CHECK);
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_VERSION, 15000);
        }

        /**
         * update current version to preference.
         */
        public void updateVersion() {
            Log.i(TAG, "updateVersion");
            mHandler.removeMessages(MSG_UPDATE_VERSION);
            mHandler.sendEmptyMessage(MSG_UPDATE_VERSION);
        }

        /**
         * Cancel the pending check message.
         */
        public void cancelCheck() {
            mHandler.removeMessages(MSG_CHECK_VERSION);
        }

        private void check() {
            Log.i(TAG, "check");
            SyncService service = mService.get();
            if (service == null) {
                Log.e(TAG, "check: service has been removed.");
                return;
            }

            String mVer = getContactsVersion(true);
            if (mVer == null) {
               Log.e(TAG, "check: compute version error!");
               return;
            }

            String savedVersion = PreferenceManager
                    .getDefaultSharedPreferences(service).getString("verdigest", "init");

            Log.i(TAG, String.format("check:: %s : %s", mVer, savedVersion));

            if (mVer.compareTo(savedVersion) != 0) {
                service.mHandler.sendEmptyMessage(MSG_AUTO_BACKUP);
            } else {
                Log.d(TAG, "new and old version is same.");
            }
        }

        /**
         * save current version to preference.
         */
        public void update() {
            Log.i(TAG, "update");
            SyncService service = mService.get();
            if (service == null) {
                Log.e(TAG, "update: service has been removed.");
                return;
            }
            String ver = getContactsVersion(false);
            if (ver == null) {
                Log.e(TAG, "upate: compute version error");
                return;
            }

            /* save to preference */
            PreferenceManager.getDefaultSharedPreferences(service).edit()
                    .putString("verdigest", ver).commit();
        }

        /**
         * @author MTK80963
         *
         */
        @SuppressLint("HandlerLeak")
        private class CheckerHandler extends Handler {
            public CheckerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case MSG_UPDATE_VERSION:
                    update();
                    break;

                case MSG_CHECK_VERSION:
                    check();
                    break;

                default:
                    break;
                }
            }
        }

        private String stringToMd5(String s) {
            byte[] value = s.getBytes();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(value);
                byte[] temp = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : temp) {
                    sb.append(Integer.toHexString(b & 0xff));
                }
                String md5Version = sb.toString();
                return md5Version;
            } catch (NoSuchAlgorithmException e) {

                e.printStackTrace();
            }
            return null;
        }

        private String getContactsVersion(boolean forCheck) {
            Log.d(TAG, "getContactsVersion+++");
            SyncService service = mService.get();
            ContactsSource contactsSource = new ContactsSource(service);
            String version = null;
            StringBuffer sb = new StringBuffer();
            Cursor raws = null;
            String[] projection = {RawContacts.VERSION};
            String selection = contactsSource.getPhoneContactSelection();
            raws = service.getContentResolver().query(RawContacts.CONTENT_URI, projection,
                    selection, null, null);
            if (raws == null) {
               Log.e(TAG, "getContactsVersion: cursor is null");
               return null;
            }
            Log.d(TAG, "get count:" + raws.getCount());
            if (raws.moveToFirst()) {
                do {
                    version = raws
                            .getString(raws
                                    .getColumnIndex(ContactsContract.RawContacts.VERSION));
                    sb.append(version);

                    if (mHandler.hasMessages(MSG_CANCEL_CHECK) && forCheck) {
                        raws.close();
                        mHandler.removeMessages(MSG_CANCEL_CHECK);
                        return null;
                    }
                } while (raws.moveToNext());

            }

            if (raws != null) {
                raws.close();
            }

            Log.d(TAG, "contact version:" + sb.toString());
            return stringToMd5(sb.toString());
        }

    }
}
