package com.android.mms.util;

import java.util.HashMap;
import java.util.Map;

import com.mediatek.mms.callback.IMuteCacheCallback;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;


public class MuteCache implements IMuteCacheCallback {

    private final Context mContext;
    private static MuteCache sInstance;
    private static final String TAG = "Mms/MuteCache";
    private static Map<Long, MuteEntry> mMuteCache;
    private static final String SILENT_SELECTION = "(notification_enable = 0) OR " +
            "(mute > 0 AND mute_start > 0)";
    private static final Uri THREAD_SETTING_URI = Uri.parse("content://mms-sms/thread_settings/");

    private MuteCache(Context context) {

        mContext = context;
    }

    public static void init(final Context context) {
        /// M:
        sInstance = new MuteCache(context);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                cacheMuteThreads(context);
            }
        }, "FolderView.muteinit");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public static MuteCache getInstance() {
        return sInstance;
    }

    private static void cacheMuteThreads(Context context) {

        mMuteCache = new HashMap<Long, MuteEntry>();

        Cursor c = context.getContentResolver().query(THREAD_SETTING_URI,
            new String[] {"thread_id", "notification_enable", "mute",
                "mute_start"},
                SILENT_SELECTION, null, null);
        try {
            if (c != null) {
                /// M:
                long mthreadId = 0L;
                long mthreadMute = 0;
                long mthreadMuteStart = 0;
                boolean mthreadNotificationEnabled = true;
                while (c.moveToNext()) {
                    mthreadId = c.getLong(0);
                    mthreadMute = c.getLong(1);
                    mthreadMuteStart = c.getLong(2);
                    mthreadNotificationEnabled = c.getLong(3) == 0 ? false : true;

                    MuteEntry mcache = new MuteEntry(mthreadMute, mthreadMuteStart, mthreadNotificationEnabled);
                    mMuteCache.put(mthreadId, mcache);
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static boolean needMute(long threadId) {
        return mMuteCache.containsKey(threadId);
    }

    public static MuteEntry getMuteEntry(long threadId) {
        if (mMuteCache.containsKey(threadId)) {
            return mMuteCache.get(threadId);
        } else {
            return null;
        }
    }

    public long getMute(long threadId) {
        if (mMuteCache.containsKey(threadId)) {
            return mMuteCache.get(threadId).threadMute;
        } else {
            return 0;
        }
    }

    public long getMuteStart(long threadId) {
        if (mMuteCache.containsKey(threadId)) {
            return mMuteCache.get(threadId).threadMuteStart;
        } else {
            return 0;
        }
    }

    public boolean getMuteEnable(long threadId) {
        if (mMuteCache.containsKey(threadId)) {
            return mMuteCache.get(threadId).threadNotificationEnabled;
        } else {
            return true;
        }
    }

    public static void setMuteCache(long lthreadId, long lthreadMute, long lthreadMuteStart,
            boolean lthreadNotificationEnabled) {
        if (lthreadNotificationEnabled == false || (lthreadMute > 0 && lthreadMuteStart > 0)) {
            MmsLog.d(TAG, "setMuteCache add lthreadId =" + lthreadId);
            if (mMuteCache.containsKey(lthreadId)) {
                mMuteCache.remove(lthreadId);
            }
            MuteEntry mtcache = new MuteEntry(lthreadMute, lthreadMuteStart, lthreadNotificationEnabled);
            mMuteCache.put(lthreadId, mtcache);
        } else {
            MmsLog.d(TAG, "setMuteCache remove lthreadId =" + lthreadId);
            if (mMuteCache.containsKey(lthreadId)) {
                mMuteCache.remove(lthreadId);
            }
        }
    }

    /// M: callback @{
    public void setMuteCacheCallback(long lthreadId, long lthreadMute, long lthreadMuteStart,
            boolean lthreadNotificationEnabled) {
        setMuteCache(lthreadId, lthreadMute, lthreadMuteStart, lthreadNotificationEnabled);
    }

    public void initCallback(Context context) {
        init(context);
    }
    /// @}
}
class MuteEntry {
    long threadMute = 0;
    long threadMuteStart = 0;
    boolean threadNotificationEnabled = true;
    public MuteEntry(long lthreadMute, long lthreadMuteStart, boolean lthreadNotificationEnabled) {
        super();
        threadMute = lthreadMute;
        threadMuteStart = lthreadMuteStart;
        threadNotificationEnabled = lthreadNotificationEnabled;
    }
}
