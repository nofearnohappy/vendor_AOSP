package com.hesine.nmsg.observer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.os.Process;
import android.provider.ContactsContract.RawContacts;

import com.hesine.nmsg.common.MLog;

public class NmsgService extends Service {

    private boolean isServiceStart = false;
    private ContentObserver mThreadsObserver = null;
    private ContentObserver mContactsObserver = null;
    private static final Uri URI_DIRTY_CONTACTS = RawContacts.CONTENT_URI;
    private static NmsgService mInstance = null;

    public NmsgService() {
        super();
    }

    public static NmsgService getInstance() {
        if (mInstance == null) {
            mInstance = new NmsgService();
        }
        return mInstance;
    }

    public void addThreadObserver() {

        ContentResolver resolver = getContentResolver();
        if (null == mThreadsObserver) {
            mThreadsObserver = new ThreadsObserver();
        }
        resolver.registerContentObserver(Uri.parse("content://mms-sms/threads"), false,
                mThreadsObserver);
    }

    public void removeThreadObserver() {

        if (null == mThreadsObserver) {
            return;
        }
        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mThreadsObserver);
    }

    public void addContactObserver() {
        ContentResolver resolver = getContentResolver();
        if (null == mContactsObserver) {
            mContactsObserver = new ContactObserver();
        }
        resolver.registerContentObserver(URI_DIRTY_CONTACTS, true, mContactsObserver);
    }

    public void removeContactObserver() {
        if (null == mContactsObserver) {
            return;
        }
        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mContactsObserver);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        isServiceStart = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MLog.info("nmsg service onCreate");
        mInstance = this;
        addThreadObserver();
        addContactObserver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MLog.info("nmsg service onDestroy");
        removeThreadObserver();
        removeContactObserver();
        mInstance = null;
        isServiceStart = false;
        //Process.killProcess(Process.myPid());
        super.onDestroy();
    }

    public boolean isServiceStart() {
        return isServiceStart;
    }

}
