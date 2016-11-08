package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.util.Utils;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;

public class MediaEntry {
    private static final String TAG = "PAM/MediaEntry";

    private static final Object DB_LOCK = new Object();

    public long id;
    public int type;
    private int refCount;
    public String url;
    public String path;
    public long timestamp;

    public static String[] sFullProjection = {
        MediaColumns.ID,
        MediaColumns.TYPE,
        MediaColumns.TIMESTAMP,
        MediaColumns.PATH,
        MediaColumns.URL,
        MediaColumns.REF_COUNT
    };

    public MediaEntry() {

    }

    public MediaEntry(long id, int type, String url, String path, long timestamp) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.path = path;
        this.timestamp = timestamp;
    }

    public static MediaEntry loadFromProvider(long mediaId, ContentResolver cr) {
        MediaEntry result = null;
        Cursor c = null;
        try {
            synchronized (MediaEntry.class) {
                c = cr.query(
                    MediaColumns.CONTENT_URI,
                    sFullProjection,
                    MediaColumns.ID + "=?",
                    new String[]{Long.toString(mediaId)},
                    null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    result = new MediaEntry();
                    result.id = mediaId;
                    result.type = c.getInt(c.getColumnIndexOrThrow(MediaColumns.TYPE));
                    result.url = c.getString(c.getColumnIndexOrThrow(MediaColumns.URL));
                    result.path = c.getString(c.getColumnIndexOrThrow(MediaColumns.PATH));
                    result.timestamp = c.getLong(c.getColumnIndexOrThrow(MediaColumns.TIMESTAMP));
                    result.refCount = c.getInt(c.getColumnIndexOrThrow(MediaColumns.REF_COUNT));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    private ContentValues storeToContentValues() {
        synchronized (MediaEntry.class) {
            ContentValues cv = new ContentValues();
            if (id != Constants.INVALID) {
                cv.put(MediaColumns.ID, id);
            }
            cv.put(MediaColumns.TYPE, type);
            cv.put(MediaColumns.TIMESTAMP, timestamp);
            cv.put(MediaColumns.PATH, path);
            cv.put(MediaColumns.URL, url);
            cv.put(MediaColumns.REF_COUNT, refCount);
            return cv;
        }
    }

    public long storeToProvider(ContentResolver cr, boolean strongRef) {
        Cursor c = null;

        if (TextUtils.isEmpty(url)) {
            if (!strongRef) {
                synchronized (MediaEntry.class) {
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, storeToContentValues());
                    id = Long.parseLong(uri.getLastPathSegment());
                }
            } else {
                Log.d(TAG, "storeToProvider(). strong reference but url is null !");
            }
            return id;
        }

        try {
            synchronized (MediaEntry.class) {
                c = cr.query(
                        MediaColumns.CONTENT_URI,
                        sFullProjection,
                        MediaColumns.URL + "=?",
                        new String[] {url},
                        null);
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    id = c.getLong(c.getColumnIndexOrThrow(MediaColumns.ID));
                    refCount = c.getInt(c.getColumnIndexOrThrow(MediaColumns.REF_COUNT));
                    if (strongRef) {
                        refCount++;
                        Log.d(TAG, "update media and add count++ : " + refCount);
                    }
                    if (TextUtils.isEmpty(path)) {
                        path = c.getString(c.getColumnIndexOrThrow(MediaColumns.PATH));
                        if (!TextUtils.isEmpty(path) && 0 == timestamp) {
                            timestamp = c.getLong(c.getColumnIndexOrThrow(MediaColumns.TIMESTAMP));
                        }
                    }
                    int result = cr.update(
                            MediaColumns.CONTENT_URI,
                            storeToContentValues(),
                            MediaColumns.URL + "=?",
                            new String[] {url});
                    if (result != 1) {
                        Log.e(TAG, "Failed to update media item with URL: " + url);
                    }
                } else {
                    if (strongRef) {
                        refCount = 1;
                        Log.d(TAG, "insert new media and add count=1");
                    }
                    timestamp = Utils.currentTimestamp();
                    Uri uri = cr.insert(MediaColumns.CONTENT_URI, storeToContentValues());
                    id = Long.parseLong(uri.getLastPathSegment());
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return id;
    }

    public void deleteFromProvider(ContentResolver cr) {
        Log.d(TAG, "deleteMediaMessage");

        if (id == Constants.INVALID) {
            Log.e(TAG, "deleteMediaMessage but mediaId is -1");
            return;
        }

        synchronized (MediaEntry.class) {
            Cursor c = cr.query(
                    MediaColumns.CONTENT_URI,
                    new String[] {MediaColumns.PATH, MediaColumns.REF_COUNT},
                    MediaColumns.ID + "=?",
                    new String[] {Long.toString(id)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                path = c.getString(c.getColumnIndexOrThrow(MediaColumns.PATH));
                refCount = c.getInt(c.getColumnIndexOrThrow(MediaColumns.REF_COUNT));
                Log.d(TAG, "deleteMediaMessage: query filePath="
                        + path + ". refCount=" + refCount);
                if (refCount > 1) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaColumns.REF_COUNT, --refCount);
                    int result = cr.update(
                            MediaColumns.CONTENT_URI,
                            cv,
                            MediaColumns.ID + "=?",
                            new String[]{Long.toString(id)});
                    Log.d(TAG, "deleteMessageMessge with refCount-- =" + refCount);
                } else {
                    cr.delete(
                            MediaColumns.CONTENT_URI,
                            MediaColumns.ID + "=?",
                            new String[]{Long.toString(id)});
                    if (!TextUtils.isEmpty(path)) {
                        Log.d(TAG, "deleteMediaMessage. delete file:" + path);
                        Utils.deleteFile(path);
                    } else {
                        Log.w(TAG, "path of media is empty: " + id);
                    }
                }
            }
        }
    }
}
