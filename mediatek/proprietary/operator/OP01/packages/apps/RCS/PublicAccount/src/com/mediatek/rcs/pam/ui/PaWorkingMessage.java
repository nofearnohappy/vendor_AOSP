package com.mediatek.rcs.pam.ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.ui.conversation.PaComposeActivity;
import com.mediatek.rcs.pam.util.GeoLocUtils;
import com.mediatek.rcs.pam.util.RCSVCardAttachment;
import com.mediatek.rcs.pam.util.Utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Audio;
import android.text.TextUtils;
import android.util.Log;

public class PaWorkingMessage {
    private static final String TAG = "PA/PaWorkingMessage";

    // Database access stuff
    private final Activity mActivity;
    private final ContentResolver mContentResolver;

    // Attachment types
    public static final int TEXT = 0;
    public static final int IMAGE = 1;
    public static final int VIDEO = 2;
    public static final int AUDIO = 3;
    public static final int VCARD = 4;
    public static final int GEOLOC = 5;

    public static final int OK = 0;
    public static final int ERR_UNKOWN = -100;
    public static final int ERR_AUDIO_TOO_LONG = -1;
    public static final int ERR_VIDEO_TOO_LARGE = -2;
    public static final int ERR_IMAGE_TOO_LARGE = -3;
    public static final int ERR_UNSUPPORT_TYPE = -4;
    public static final int ERR_PATH_NOT_EXIST = -5;
    public static final int ERR_FILE_NOT_FIND = -6;
    public static final int ERR_IMAGE_RESIZE_FAIL = -7;
    public static final int ERR_GET_CONTACT_FAIL = -8;

    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    public static final long MAX_VIDEO_SIZE = 500 * 1000 * 1000;

    // Current attachment type of the message; one of the above values.
    private int mAttachmentType;

    // Set to true if this message has been discarded.
    private boolean mDiscarded = false;

    private long mAccountId = 0;

    private volatile boolean mHasTextDraft;

    // Text of the message.
    private CharSequence mText;
    private String mMimeType;
    private Uri mUri;
    private String mPath;
    private String mZoomInPath;
    private String mThumbPath;
    private int mSize;
    private int mDuration;
    private int mWidth;
    private int mHeight;
    private long mFTMaxSize;
    private String mContent;

    final String[] mAudioSelectColumn = { Audio.Media.DATA };

    final String[] mVideoSelectColumn = { "_data", "duration" };

    final String[] mImageSelectColumn = { "_data", "_size", "width", "height",
            "mime_type" };

    public interface MessageStatusListener {
        /**
         * Called when the protocol for sending the message changes from SMS to
         * MMS, and vice versa.
         *
         * @param mms If true, it changed to MMS. If false, to SMS.
         */
        void onProtocolChanged(boolean mms);

        /**
         * Called when an attachment on the message has changed.
         */
        void onAttachmentChanged();

        /**
         * Called just before the process of sending a message.
         */
        void onPreMessageSent();

        /**
         * Called once the process of sending a message, triggered by
         * {@link send} has completed. This doesn't mean the send succeeded,
         * just that it has been dispatched to the network.
         */
        void onMessageSent();

        /**
         * Called if there are too many unsent messages in the queue and we're
         * not allowing any more Mms's to be sent.
         */
        void onMaxPendingMessagesReached();

        /**
         * Called if there's an attachment error while resizing the images just
         * before sending.
         */
        void onAttachmentError(int error);
    }

    private PaWorkingMessage(PaComposeActivity activity) {
        mActivity = activity;
        mContentResolver = mActivity.getContentResolver();
        mAttachmentType = 0;
        mText = "";
        clearAttachmentInfo();
    }

    /**
     * Creates a new working message.
     */
    public static PaWorkingMessage createEmpty(PaComposeActivity activity) {
        // Make a new empty working message.
        PaWorkingMessage msg = new PaWorkingMessage(activity);
        return msg;
    }

    /**
     * Load the draft message for the specified conversation, or a new empty
     * message if none exists.
     */
    public static PaWorkingMessage loadDraft(PaComposeActivity activity,
            final long accountId, final Runnable onDraftLoaded) {
        Log.d(TAG, "loadDraft. accountId=" + accountId);

        final PaWorkingMessage msg = createEmpty(activity);
        if (accountId <= 0) {
            if (onDraftLoaded != null) {
                onDraftLoaded.run();
            }
            return msg;
        }

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... none) {
                // Look for an SMS draft first.
                String draftText = msg.readDraftTextMessage(accountId);

                return draftText;
            }

            @Override
            protected void onPostExecute(String result) {
                if (!TextUtils.isEmpty(result)) {
                    msg.mHasTextDraft = true;
                    msg.setText(result);
                }
                if (onDraftLoaded != null) {
                    onDraftLoaded.run();
                }
            }
        }.execute();

        return msg;
    }

    public void setText(CharSequence s) {
        mText = s;
    }

    /**
     * Returns the current message text.
     */
    public CharSequence getText() {
        return mText;
    }

    /**
     * @return True if the message has any text. A message with just whitespace
     *         is not considered to have text.
     */
    public boolean hasText() {
        return mText != null && !TextUtils.isEmpty(mText);
    }

    /**
     * Adds an attachment to the message, replacing an old one if it existed.
     *
     * @param type Type of this attachment, such as {@link IMAGE}
     * @param dataUri Uri containing the attachment data (or null for
     *            {@link TEXT})
     * @param append true if we should add the attachment to a new slide
     * @return An error code such as {@link UNKNOWN_ERROR} or {@link OK} if
     *         successful
     */
    public int setAttachment(int type, Uri dataUri, long maxSize) {
        Log.d(TAG, "setAttachment() type=" + type + ". maxSize=" + maxSize
                + ". uri=" + dataUri);

        if (null == dataUri) {
            return ERR_UNKOWN;
        }
        clearAttachmentInfo();

        mAttachmentType = type;
        mUri = dataUri;
        int result = ERR_UNKOWN;

        switch (type) {
        case IMAGE:
            mFTMaxSize = maxSize > MAX_IMAGE_SIZE ? MAX_IMAGE_SIZE : maxSize;
            result = parseImageInfo();
            break;
        case VIDEO:
            mFTMaxSize = maxSize > MAX_VIDEO_SIZE ? MAX_VIDEO_SIZE : maxSize;
            result = parseVideoInfo();
            break;
        case AUDIO:
            mFTMaxSize = maxSize;
            result = parseAudioInfo();
            break;
        default:
            break;
        }
        return result;
    }

    public int setVCardAttachment(long[] ids) {

        int result = ERR_UNKOWN;

        clearAttachmentInfo();

        if (null == ids || ids.length == 0) {
            return result;
        }

        mAttachmentType = VCARD;

        RCSVCardAttachment va = new RCSVCardAttachment(mActivity);
        mPath = va.getVCardFileNameByContactsId(ids, true);
        if (mPath != null && mPath.isEmpty()) {
            result = ERR_GET_CONTACT_FAIL;
        } else {
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(mPath));
                String line = "";
                StringBuffer buffer = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    buffer.append(line).append("\n");
                }
                mContent = buffer.toString();
                result = OK;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int setLocAttachment(Location loc) {
        int result = ERR_UNKOWN;

        clearAttachmentInfo();

        if (loc == null) {
            return result;
        }

        mAttachmentType = GEOLOC;

        mContent = GeoLocUtils.buildGeoLocXml(loc);

        if (mContent != null && !mContent.isEmpty()) {
            result = OK;
        }

        return OK;
    }

    private void clearAttachmentInfo() {
        mAttachmentType = 0;
        mMimeType = null;
        mUri = null;
        mPath = null;
        mZoomInPath = null;
        mThumbPath = null;
        mContent = null;
        mFTMaxSize = -1;
        mSize = -1;
        mDuration = -1;
        mWidth = -1;
        mHeight = -1;
    }

    private int parseImageInfo() {

        Cursor cursor = null;

        cursor = mActivity.getContentResolver().query(mUri, mImageSelectColumn,
                null, null, null);
        if (null == cursor) {
            mPath = mUri.getEncodedPath();
        } else if (0 == cursor.getCount()) {
            Log.e(TAG, "parseImageInfo(): cursor count is 0");
        } else {
            cursor.moveToFirst();
            mPath = cursor.getString(cursor.getColumnIndex("_data"));
            mWidth = cursor.getInt(cursor.getColumnIndex("width"));
            mHeight = cursor.getInt(cursor.getColumnIndex("height"));
            mSize = cursor.getInt(cursor.getColumnIndex("_size"));
            mMimeType = cursor.getString(cursor.getColumnIndex("mime_type"));
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        if (null == mPath) {
            Log.d(TAG, "parseImageInfo() get file path fail");
            return ERR_PATH_NOT_EXIST;
        }
        if (!Utils.isPic(mPath)) {
            Log.d(TAG, "parseImageInfo() unsupport type");
            return ERR_UNSUPPORT_TYPE;
        }
        // get file size
        mSize = Utils.getFileSize(mPath);
        if (mSize < 0) {
            Log.d(TAG, "parseImageInfo() get image size fail");
            return ERR_FILE_NOT_FIND;
        } else if (mSize > mFTMaxSize) {
            Log.d(TAG, "parseVideoInfo() get image size too large");
            return ERR_IMAGE_TOO_LARGE;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inSampleSize = 1;
            BitmapFactory.decodeFile(mPath, options);
            mWidth = options.outWidth;
            mHeight = options.outHeight;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "parseVideoInfo() out of memory:" + e);
            return ERR_IMAGE_TOO_LARGE;
        }

        Log.d(TAG, "parseVideoInfo() resolution:" + mWidth + "x" + mHeight);
        if (mWidth > 4096 || mHeight > 4096) {
            Log.d(TAG, "parseVideoInfo() get image resolution too large");
            return ERR_IMAGE_TOO_LARGE;
        }
        return OK;
    }

    private int parseVideoInfo() {

        Cursor cursor = null;
        // parse path
        cursor = mActivity.getContentResolver().query(mUri, mVideoSelectColumn,
                null, null, null);
        if (null == cursor) {
            mPath = mUri.getEncodedPath();
        } else if (0 == cursor.getCount()) {
            Log.e(TAG, "parseVideoInfo(): cursor count is 0");
        } else {
            cursor.moveToFirst();
            mPath = cursor.getString(cursor.getColumnIndex("_data"));
            mDuration = cursor.getInt(cursor.getColumnIndex("duration"));
            mDuration = mDuration / 1000 == 0 ? 1 : mDuration / 1000;
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
        if (null == mPath) {
            Log.d(TAG, "parseVideoInfo() get file path fail");
            return ERR_PATH_NOT_EXIST;
        }

        // get file size
        mSize = Utils.getFileSize(mPath);
        if (mSize < 0) {
            Log.d(TAG, "parseVideoInfo() getVideo size fail");
            return ERR_FILE_NOT_FIND;
        } else if (mSize > mFTMaxSize) {
            Log.d(TAG, "parseVideoInfo() getVideo too large");
            return ERR_VIDEO_TOO_LARGE;
        }

        // parse duration
        if (mDuration <= 0) {
            mDuration = getFileDuration(mPath);
        }
        if (mDuration < 0) {
            Log.d(TAG, "parseVideoInfo() getDuration fail");
            return ERR_UNSUPPORT_TYPE;
        }

        Log.d(TAG, "parseVideoInfo() OK");
        return OK;
    }

    private int parseAudioInfo() {

        final String scheme = mUri.getScheme();
        if (scheme.equals("file")) {
            mPath = mUri.getEncodedPath();
        } else {
            Cursor c = mActivity.getContentResolver().query(mUri,
                    mAudioSelectColumn, null, null, null);
            c.moveToFirst();
            mPath = c.getString(c.getColumnIndexOrThrow(Audio.Media.DATA));
            c.close();
        }
        if (mPath == null || mPath.isEmpty()) {
            return ERR_UNKOWN;
        }
        Log.d(TAG, "parseAudioInfo() path=" + mPath);

        mSize = Utils.getFileSize(mPath);
        if (mSize < 0) {
            Log.d(TAG, "parseAudioInfo() getVideo size fail");
            return ERR_FILE_NOT_FIND;
        } else if (mSize > mFTMaxSize && mFTMaxSize > 0) {
            Log.d(TAG, "parseAudioInfo() getAudio too large");
            return ERR_AUDIO_TOO_LONG;
        }

        mDuration = getFileDuration(mPath);
        if (mDuration < 0) {
            return ERR_UNSUPPORT_TYPE;
        } else if (mDuration > 180) {
            return ERR_AUDIO_TOO_LONG;
        } else {
            return OK;
        }
    }

    private int getFileDuration(String path) {
        int duration = -1;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(path);
            String dur = mmr
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                duration = Integer.parseInt(dur);
                duration = duration / 1000 == 0 ? 1 : duration / 1000;
            }
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        } finally {
            mmr.release();
        }

        Log.d(TAG, "getAudioDuration()=" + duration);
        return duration;
    }

    public int generateThumbnail() {
        if (mAttachmentType == VIDEO) {
            return generateVideoThumbnail();
        } else {
            return generateImageThumbnail();
        }
    }

    private int generateImageThumbnail() {
        String path = mZoomInPath == null ? mPath : mZoomInPath;
        Log.d(TAG, "generateImageThumbnail(). original path = " + path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        int w = options.outWidth;
        int h = options.outHeight;
        bitmap = Utils.getBitmapByPath(path, options, w, h);

        Bitmap newBitmap = Utils.resizeImage(bitmap, 352, 288, true);
        String thumbPath = Utils.getTempFilePath(Utils.PA_FILE_PREFIX_IMG,
                Utils.PA_FILE_SUFFIX_JPG);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(thumbPath);
            newBitmap.compress(CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mThumbPath = thumbPath;
        return OK;
    }

    private int generateVideoThumbnail() {
        Log.d(TAG, "generateVideoThumbnail:" + mPath);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(mPath);
            Bitmap bitmap = mmr.getFrameAtTime();

            String path = Utils.getTempFilePath(Utils.PA_FILE_PREFIX_VDO,
                    Utils.PA_FILE_SUFFIX_JPG);

            FileOutputStream fos = null;
            fos = new FileOutputStream(path);
            bitmap.compress(CompressFormat.JPEG, 100, fos);
            fos.close();
            mThumbPath = path;
        } catch (IOException e) {
            e.printStackTrace();
            return ERR_UNKOWN;
        }
        mmr.release();
        return OK;
    }

    public int zoomInImage() {
        Log.d(TAG, "zoomInImage");

        if (mPath == null || mPath.isEmpty()) {
            return ERR_PATH_NOT_EXIST;
        }

        if (!Utils.isExistsFile(mPath)) {
            return ERR_FILE_NOT_FIND;
        }
        if (!Utils.isPic(mPath)) {
            Log.d(TAG, "zoomInImage() unsupport type");
            return ERR_UNSUPPORT_TYPE;
        }
        long maxLen = 1024;
        byte[] img = Utils.resizeImg(mPath, maxLen);

        if (null == img) {
            return ERR_IMAGE_RESIZE_FAIL;
        }
        mZoomInPath = Utils.getTempFilePath(Utils.PA_FILE_PREFIX_IMG,
                Utils.PA_FILE_SUFFIX_JPG);
        try {
            Utils.nmsStream2File(img, mZoomInPath);
        } catch (IOException e) {
            e.printStackTrace();
            return ERR_IMAGE_RESIZE_FAIL;
        }
        return OK;
    }

    public int getAttachmentType() {
        return mAttachmentType;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getPath() {
        return mPath;
    }

    public String getThumbnail() {
        return mThumbPath;
    }

    public String getZoomInImage() {
        return mZoomInPath;
    }

    public String getVCardContent() {
        return mContent;
    }

    public String getGeolocContent() {
        Log.d(TAG, "getGeolocContent()=" + mContent);
        return mContent;
    }

    public int getSize() {
        return mSize;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public boolean isGif() {
        if (mMimeType != null) {
            if (mMimeType.equalsIgnoreCase("image/gif")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this message contains anything worth saving.
     */
    public boolean isWorthSaving() {
        // If it actually contains anything, it's of course not empty.
        if (hasText()) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the message has an attachment.
     */
    public boolean hasAttachment() {
        return (mAttachmentType > 0);
    }

    /**
     * Save this message as a draft in the conversation previously specified to
     * {@link setConversation}.
     */
    public void saveDraft(final boolean isStopping) {
        Log.d(TAG, "saveDraft. isStopping:" + isStopping);

        // If we have discarded the message, just bail out.
        if (mDiscarded) {
            Log.d(TAG, "saveDraft mDiscarded: true mAccountId: " + mAccountId
                    + " skipping saving draft and bailing");
            return;
        }

        // Make sure setConversation was called.
        if (mAccountId < 0) {
            throw new IllegalStateException(
                    "saveDraft() called with no account id");
        }

        String content = mText.toString();

        // bug 2169583: don't bother creating a thread id only to delete the
        // thread
        // because the content is empty. When we delete the thread in
        // updateDraftSmsMessage,
        // we didn't nullify conv.mThreadId, causing a temperary situation where
        // conv
        // is holding onto a thread id that isn't in the database. If a new
        // message arrives
        // and takes that thread id (because it's the next thread id to be
        // assigned), the
        // new message will be merged with the draft message thread, causing
        // confusion!
        if (!TextUtils.isEmpty(content)) {
            asyncUpdateDraftSmsMessage(mAccountId, content, isStopping);
            mHasTextDraft = true;
        }
    }

    public synchronized void discard() {
        Log.d(TAG, "PaWorkingMessage discard");

        if (mDiscarded) {
            return;
        }

        // Mark this message as discarded in order to make saveDraft() no-op.
        mDiscarded = true;

        if (mHasTextDraft) {
            asyncDeleteDraftTextMessage(mAccountId);
        }
    }

    public void unDiscard() {
        Log.d(TAG, "PaWorkingMessage unDiscard");
        mDiscarded = false;
    }

    /**
     * Returns true if discard() has been called on this message.
     */
    public boolean isDiscarded() {
        return mDiscarded;
    }

    /**
     * Set the conversation associated with this message.
     */
    public void setAccountId(long accountId) {
        Log.d(TAG, "setAccountId = " + accountId);
        mAccountId = accountId;
    }

    public long getAccountId() {
        return mAccountId;
    }

    /**
     * Reads a draft message for the given thread ID from the database, if there
     * is one, deletes it from the database, and returns it.
     *
     * @return The draft message or an empty string.
     */
    private String readDraftTextMessage(long accountId) {
        Log.d(TAG, "readDraftTextMessage accountid: " + accountId);
        // If it's an invalid thread or we know there's no draft, don't bother.
        if (accountId <= 0) {
            return "";
        }
        String body = "";
        ContentResolver cr = mActivity.getContentResolver();

        Cursor c = cr.query(
                PAContract.MessageColumns.CONTENT_URI,
                new String[] { PAContract.MessageColumns.TEXT },
                PAContract.MessageColumns.ACCOUNT_ID + "=? AND "
                        + PAContract.MessageColumns.STATUS + "=?",
                new String[] { Long.toString(accountId),
                        Long.toString(Constants.MESSAGE_STATUS_DRAFT) }, null);

        boolean haveDraft = false;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    body = c.getString(c
                            .getColumnIndex(PAContract.MessageColumns.TEXT));
                    haveDraft = true;
                }
            } finally {
                c.close();
            }
        }

        // We found a draft, and if there are no messages in the conversation,
        // that means we deleted the thread, too. Must reset the thread id
        // so we'll eventually create a new thread.
        // if (haveDraft && conv.getMessageCount() == 0) {
        // asyncDeleteDraftSmsMessage(conv);

        // Clean out drafts for this thread -- if the recipient set changes,
        // we will lose track of the original draft and be unable to delete
        // it later. The message will be re-saved if necessary upon exit of
        // the activity.
        // clearConversation(conv, true);
        // }
        Log.d(TAG, "readDraftSmsMessage haveDraft: " + body);

        return body;
    }

    private void asyncUpdateDraftSmsMessage(final long accountId,
            final String contents, final boolean isStopping) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateDraftTextMessage(accountId, contents);
            }
        }, "PaWorkingMessage.asyncUpdateDraftSmsMessage").start();
    }

    private void updateDraftTextMessage(final long accountId, String contents) {

        Log.d(TAG, "updateDraftSmsMessage accountId=" + accountId
                + ", contents=" + contents);

        // If we don't have a valid thread, there's nothing to do.
        if (accountId <= 0) {
            return;
        }

        ContentResolver cr = mActivity.getContentResolver();
        ContentValues cv = new ContentValues();
        Cursor c = null;
        long msgId = -1;
        String uuid = "";

        try {
            c = cr.query(
                    PAContract.MessageColumns.CONTENT_URI,
                    new String[] { PAContract.MessageColumns.ID,
                            MessageColumns.UUID },
                    PAContract.MessageColumns.ACCOUNT_ID + "=? AND "
                            + PAContract.MessageColumns.STATUS + "=?",
                    new String[] { Long.toString(accountId),
                            Long.toString(Constants.MESSAGE_STATUS_DRAFT) },
                    null);

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                msgId = c.getLong(c
                        .getColumnIndex(PAContract.AccountColumns.ID));
                uuid = c.getString(c
                        .getColumnIndex(PAContract.AccountColumns.UUID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (msgId >= 0) {
            cv.put(MessageColumns.TEXT, contents);
            cr.update(PAContract.MessageColumns.CONTENT_URI, cv,
                    PAContract.MessageColumns.ID + "=?",
                    new String[] { Long.toString(msgId) });
        } else {
            cv.put(MessageColumns.ACCOUNT_ID, accountId);
            cv.put(MessageColumns.TYPE, Constants.MEDIA_TYPE_TEXT);
            cv.put(MessageColumns.UUID, uuid);
            cv.put(MessageColumns.TEXT, contents);
            cv.put(MessageColumns.DIRECTION,
                    Constants.MESSAGE_DIRECTION_OUTGOING);
            cv.put(MessageColumns.STATUS, Constants.MESSAGE_STATUS_DRAFT);
            cv.put(MessageColumns.FORWARDABLE, 0);
            Uri uri = cr.insert(PAContract.MessageColumns.CONTENT_URI, cv);
            msgId = Long.parseLong(uri.getLastPathSegment());
            Log.d(TAG, "insert new draft. megId=" + msgId);
        }
    }

    public void asyncDeleteDraftTextMessage(final long accountId) {
        mHasTextDraft = false;

        if (accountId > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    deleteDraftTextMessage(accountId);
                }
            }, "WorkingMessage.asyncDeleteDraftTextMessage").start();
        }
    }

    private void deleteDraftTextMessage(long accountId) {
        Cursor c = null;
        long msgId = -1;

        try {
            c = mContentResolver.query(
                    PAContract.MessageColumns.CONTENT_URI,
                    new String[] { PAContract.MessageColumns.ID },
                    PAContract.MessageColumns.ACCOUNT_ID + "=? AND "
                            + PAContract.MessageColumns.STATUS + "=?",
                    new String[] { Long.toString(accountId),
                            Long.toString(Constants.MESSAGE_STATUS_DRAFT) },
                    null);

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                msgId = c.getLong(c
                        .getColumnIndex(PAContract.AccountColumns.ID));
                mContentResolver.delete(PAContract.MessageColumns.CONTENT_URI,
                        PAContract.MessageColumns.ID + "=?",
                        new String[] { Long.toString(msgId) });
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
