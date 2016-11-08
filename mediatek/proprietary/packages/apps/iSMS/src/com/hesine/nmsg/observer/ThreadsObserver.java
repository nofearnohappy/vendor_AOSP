package com.hesine.nmsg.observer;

import java.util.HashSet;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.business.dao.NmsMtkBinderApi;
import com.hesine.nmsg.thirdparty.Statistics;
import com.hesine.nmsg.ui.NmsgNotification;

public class ThreadsObserver extends ContentObserver {

    public static final Uri MSG_QUERY_URI = Uri
            .parse("content://mms-sms/conversations?simple=true");

    public ThreadsObserver() {
        super(null);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        new Thread(new Runnable() {

            @Override
            public void run() {
                processThreadsChange();
            }
        }).start();
    }

    public synchronized void processThreadsChange() {
        HashSet<Long> threads = DBUtils.getMessageThreads();
        String selection = null;

        for (Long thread : threads) {
            selection = "_id = '" + thread + "'";
            Cursor cr = NmsMtkBinderApi.getInstance().query(MSG_QUERY_URI, null, selection, null,
                    null);
            if (!(null != cr && cr.getCount() > 0 && cr.moveToFirst())) {
                String accountName = DBUtils.getSenderViaThreadId(thread);
                DBUtils.deleteMsgViaThreadId(thread);
                NmsgNotification.getInstance(Application.getInstance()).removeNotification(
                        accountName);
                Statistics.getInstance().threadsDelete(accountName, Statistics.ThreadDeleteType.CV);
            }

            if (cr != null) {
                cr.close();
            }
        }
    }
}