package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.provider.PAContract.MediaArticleColumns;
import com.mediatek.rcs.pam.util.Utils;


public class MediaArticle implements SanityCheck {
    private static final String TAG = "PAM/MediaArticle";
    public String title;
    public String author;
    public String thumbnailUrl;
    public String originalUrl;
    public String sourceUrl;
    public String bodyUrl;
    public String mainText;
    public String mediaUuid;
    public String fileType;

    // Android Specific
    public long id = Constants.INVALID;
    public long thumbnailId = Constants.INVALID;
    public long originalId = Constants.INVALID;
    public String thumbnailPath;
    public String originalPath; // FIXME support this

    public static String[] sFullProjection = {
        MediaArticleColumns.ID,
        MediaArticleColumns.TITLE,
        MediaArticleColumns.AUTHOR,
        MediaArticleColumns.THUMBNAIL_ID,
        MediaArticleColumns.ORIGINAL_ID,
        MediaArticleColumns.SOURCE_URL,
        MediaArticleColumns.BODY_URL,
        MediaArticleColumns.TEXT,
        MediaArticleColumns.FILE_TYPE,
        MediaArticleColumns.MEDIA_UUID,
    };

    @Override
    public void checkSanity() throws PAMException {
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(title) ||
                TextUtils.isEmpty(thumbnailUrl) ||
                TextUtils.isEmpty(originalUrl) ||
                TextUtils.isEmpty(bodyUrl) ||
//                TextUtils.isEmpty(sourceUrl) ||
                TextUtils.isEmpty(mediaUuid)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"MediaArticle\", title:\"")
        .append(title)
        .append("\", author:\"")
        .append(author)
        .append("\", thumbnailUrl:\"")
        .append(thumbnailUrl)
        .append("\", originalUrl:\"")
        .append(originalUrl)
        .append("\", sourceUrl:\"")
        .append(sourceUrl)
        .append("\", bodyUrl:\"")
        .append(bodyUrl)
        .append("\", mainText:\"")
        .append(mainText)
        .append("\", mediaUuid:\"")
        .append(mediaUuid)
        .append("\", fileType:\"")
        .append(fileType)
        .append("\", thumbnailPath:\"")
        .append(thumbnailPath)
        .append("\", originalPath:\"")
        .append(originalPath)
        .append("\"}");
        return sb.toString();
    }

    public void storeToContentValues(ContentValues cv) {
        if (id != Constants.INVALID) {
            cv.put(MediaArticleColumns.ID, id);
        }
        cv.put(MediaArticleColumns.TITLE, title);
        cv.put(MediaArticleColumns.AUTHOR, author);
        cv.put(MediaArticleColumns.THUMBNAIL_ID, thumbnailId);
        cv.put(MediaArticleColumns.ORIGINAL_ID, originalId);
        cv.put(MediaArticleColumns.SOURCE_URL, sourceUrl);
        cv.put(MediaArticleColumns.BODY_URL, bodyUrl);
        cv.put(MediaArticleColumns.TEXT, mainText);
        cv.put(MediaArticleColumns.MEDIA_UUID, mediaUuid);
    }

    public static MediaArticle loadFromProvider(long articleId, ContentResolver cr) {
        MediaArticle result = null;
        Cursor c = null;
        try {
            c = cr.query(
                    MediaArticleColumns.CONTENT_URI,
                    sFullProjection,
                    MediaArticleColumns.ID + "=?",
                    new String[]{Long.toString(articleId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = new MediaArticle();
                result.id = articleId;
                result.title = c.getString(c.getColumnIndexOrThrow(MediaArticleColumns.TITLE));
                result.author = c.getString(c.getColumnIndexOrThrow(MediaArticleColumns.AUTHOR));
                result.sourceUrl = c.getString(
                        c.getColumnIndexOrThrow(MediaArticleColumns.SOURCE_URL));
                result.bodyUrl = c.getString(
                        c.getColumnIndexOrThrow(MediaArticleColumns.BODY_URL));
                result.mainText = c.getString(
                        c.getColumnIndexOrThrow(MediaArticleColumns.TEXT));
                result.mediaUuid = c.getString(
                        c.getColumnIndexOrThrow(MediaArticleColumns.MEDIA_UUID));
                result.fileType = c.getString(
                        c.getColumnIndexOrThrow(MediaArticleColumns.FILE_TYPE));
                result.thumbnailId = c.getLong(
                        c.getColumnIndexOrThrow(MediaArticleColumns.THUMBNAIL_ID));
                if (result.thumbnailId != Constants.INVALID) {
                    MediaEntry me = MediaEntry.loadFromProvider(result.thumbnailId, cr);
                    if (me != null) {
                        result.thumbnailUrl = me.url;
                        result.thumbnailPath = me.path;
                    } else {
                        Log.w(TAG, "Failed to load thumbnail MediaEntry from provider: "
                                + result.thumbnailId);
                    }
                }
                result.originalId = c.getLong(
                        c.getColumnIndexOrThrow(MediaArticleColumns.ORIGINAL_ID));
                if (result.originalId != Constants.INVALID) {
                    MediaEntry me = MediaEntry.loadFromProvider(result.originalId, cr);
                    if (me != null) {
                        result.originalUrl = me.url;
                        result.originalPath = me.path;
                    } else {
                        Log.w(TAG, "Failed to load original MediaEntry from provider: "
                                + result.originalId);
                    }
                }
            } else {
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
         }
        return result;
    }

    public long storeToProvider(ContentResolver cr) {
        if (originalUrl != null) {
            MediaEntry me = new MediaEntry(
                    originalId,
                    Constants.MEDIA_TYPE_PICTURE,
                    originalUrl,
                    originalPath,
                    0);
            originalId = me.storeToProvider(cr, true);
        }

        if (thumbnailUrl != null) {
            MediaEntry me = new MediaEntry(
                    thumbnailId,
                    Constants.MEDIA_TYPE_PICTURE,
                    thumbnailUrl,
                    thumbnailPath,
                    0);
            thumbnailId = me.storeToProvider(cr, true);
        }

        ContentValues cv = new ContentValues();
        storeToContentValues(cv);

        Cursor c = null;
        try {
            Uri uri = cr.insert(MediaArticleColumns.CONTENT_URI, cv);
            id = Long.parseLong(uri.getLastPathSegment());
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return id;
    }
}
