package com.mediatek.rcs.pam.message;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.MediaFolder;
import com.mediatek.rcs.pam.PAServiceImpl.PAFTListener;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.util.Utils;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.FileTransferService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public abstract class PAMFileMessage extends PAMBaseMessage {
    private static final String TAG = "PAM/PAMFileMessage";

    public static final Map<String, String> FILE_TYPES = new HashMap<String, String>();
    static {
        FILE_TYPES.put(".mp3",   "audio/mp3");
        FILE_TYPES.put(".wav",   "audio/wav");
        FILE_TYPES.put(".amr",   "audio/amr");

        FILE_TYPES.put(".jpeg",  "image/jpeg");
        FILE_TYPES.put(".jpg",   "image/jpeg");
        FILE_TYPES.put(".png",   "image/png");
        FILE_TYPES.put(".bmp",   "image/bmp");
        FILE_TYPES.put(".gif",   "image/gif");

        FILE_TYPES.put(".3gp",   "video/3gpp");
        FILE_TYPES.put(".mp4",   "video/mp4");
        FILE_TYPES.put(".mp4a",  "video/mp4");
        FILE_TYPES.put(".mpeg4", "video/mp4");
        FILE_TYPES.put(".mpg",   "video/mpeg");
        FILE_TYPES.put(".mpeg",  "video/mpeg");

        FILE_TYPES.put(".xml",   "application/vnd.gsma.rcspushlocation+xml");
        FILE_TYPES.put(".vcf",   "text/vcard");
    }

    protected FileTransfer mFt;
    protected PAFTListener mFtListener;

    protected long mMediaId;

    protected String mFilePath;
    protected long mOriginalId = Constants.INVALID;

    protected String mThumbPath;                    //For Video
    protected long mThumbId = Constants.INVALID;    //For Video

    protected int mDuration;                        //For Audio&Video
    protected String mData;

    PAMFileMessage(
            Context context,
            long token,
            int type,
            long accountId,
            String path,
            String thumbPath,
            int duration,
            IPAMMessageHelper helper) {

        super(context,
                token,
                type,
                accountId,
                Constants.TABLE_FT,
                com.cmcc.ccs.chat.ChatService.FT,
                helper);

        mFilePath = path;
        mThumbPath = thumbPath;
        mDuration = duration;
    }

    @Override
    public boolean readyForSend() throws JoynServiceException {
        boolean ret = false;

        FileTransferService service = mMessageHelper.getFileTransferService();
        if (service != null &&
            service.isServiceConnected() &&
            service.isServiceRegistered()) {
            ret = true;
        }

        return ret;
    }

    @Override
    public void onSendOver() {
        Log.d(TAG, "onSendOver(). msgId=" + mMsgId + ", status=" + mState);
        try {
            mFt.removeEventListener((FileTransferListener) mFtListener);
        } catch (JoynServiceException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }


    @Override
    public final void resend() throws JoynServiceException {
        if (null != mSourceId) {
            resendInternal(mDuration);
        } else {
            send();
        }
    }

    public void sendInternal(String extPath, int duration) throws JoynServiceException {

        PAFTListener listener = (PAFTListener)mMessageHelper.getFileTransferListener(mMsgId);

        FileTransfer ft = mMessageHelper.getFileTransferService().transferPublicChatFile(
                Constants.SIP_PREFIX + mUuid,
                mFilePath,
                extPath,
                listener,
                duration);
        if (ft != null) {
            String newSourceId = ft.getTransferId();
            Log.d(TAG, "FT sourceId is " + newSourceId);
            listener.setTransferId(newSourceId);
            mFt = ft;
            mFtListener = listener;
            updateSourceId(newSourceId);
        }
    }

    public void resendInternal(int duration) throws JoynServiceException {

        PAFTListener listener = (PAFTListener)mMessageHelper.getFileTransferListener(mMsgId);
        FileTransfer newFT = mMessageHelper.getFileTransferService().
                resumePublicFileTransfer(mSourceId, listener, duration);
        if (null == newFT) {
            Log.e(TAG, "resumePublicFileTransfer but newFT is null");
            return;
        }
        String newTransferId = newFT.getTransferId();
        Log.d(TAG, "New transfer ID is " + newTransferId);
        listener.setTransferId(newTransferId);

        //clear&update old ft info
        if (null != mFt) {
            try {
                mFt.removeEventListener(mFtListener);
            } catch (JoynServiceException e) {
                Log.e(TAG, "Failed to remove FTListener. Ignore.");
                e.printStackTrace();
            }
        }
        mFt = newFT;
        mFtListener = listener;
        updateSourceId(newTransferId);

    }

    @Override
    public void complain() throws JoynServiceException {
        mMessageHelper.getFileTransferService().addFileSpamReportListener(
                mMessageHelper.getFileSpamReportListener(mToken, mSourceId, mMsgId));
        mMessageHelper.getFileTransferService().initiateFileSpamReport(
                SPAM_MESSAGE_REPORT_RECEIVER, mSourceId);
    }

    protected final Pair<Long, String> copyAndStoreMediaFile(String path, int type) {

        File file = new File(path);

        ContentValues cv = new ContentValues();
        cv.put(MediaColumns.TYPE, type);
        cv.put(MediaColumns.TIMESTAMP, file.lastModified());
        cv.put(MediaColumns.PATH, file.getAbsolutePath());
        cv.put(MediaColumns.REF_COUNT, 1);

        long mediaId = Constants.INVALID;

        ContentResolver cr = mContext.getContentResolver();
        Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
        mediaId = Long.parseLong(uri.getLastPathSegment());

        String newPath = MediaFolder.generateMediaFileName(
                mediaId,
                type,
                MediaFolder.extractExtension(file.getName()));
        if (null == newPath) {
            Log.e(TAG, "Failed to rename file for media:" + mediaId);
            return new Pair<Long, String>(
                    Long.valueOf(mediaId), file.getAbsolutePath());
        }

        try {
            Utils.copyFile(path, newPath);
        } catch (IOException e) {
            throw new Error(e);
        }

        cv = new ContentValues();
        cv.put(MediaColumns.PATH, newPath);
        cr.update(
                MediaColumns.CONTENT_URI,
                cv,
                MediaColumns.ID + "=?",
                new String[]{Long.toString(mediaId)});
        //mFilePath = newPath;
        Log.d(TAG, "storeMediaFile: " + newPath + " as " + mediaId);
        return new Pair<Long, String>(Long.valueOf(mediaId), newPath);
    }

    protected final Pair<Long, MediaBasic> storeMediaBasic() {
        File file = new File(mFilePath);
        MediaBasic mb = new MediaBasic();
        mb.accountId = mAccountId;
        String extensionString = MediaFolder.extractExtension(file.getName());
        mb.fileType = FILE_TYPES.get(extensionString);
        if (mDuration > Constants.INVALID) {
            mb.duration = Integer.toString(mDuration);
        }
        mb.title = file.getName();
        mb.fileSize = Long.toString(file.length());
        mb.createTime = file.lastModified() / 1000 * 1000;
        mb.publicAccountUuid = mUuid;
        mb.originalId = mOriginalId;
        if (mThumbId > Constants.INVALID) {
            mb.thumbnailId = mThumbId;
        }
        long mediaId = storeMediaBasic(mb);

        mMimeType = mb.fileType;

        return new Pair<Long, MediaBasic>(mediaId, mb);
    }

    protected final long storeDataToMediaFile(String data, int type, String ext) {

        String filename = MediaFolder.generateMediaFileName(Constants.INVALID, type, ext);
        if (filename == null) {
            return Constants.INVALID;
        }

        File file = new File(filename);
        try {
            Utils.storeToFile(data, filename);
        } catch (IOException e) {
            throw new Error(e);
        }
        // insert to media table
        ContentValues cv = new ContentValues();
        cv.put(MediaColumns.TYPE, type);
        cv.put(MediaColumns.TIMESTAMP, file.lastModified());
        cv.put(MediaColumns.PATH, file.getAbsolutePath());
        cv.put(MediaColumns.REF_COUNT, 1);

        ContentResolver cr = mContext.getContentResolver();
        Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
        long mediaId = Long.parseLong(uri.getLastPathSegment());

        mFilePath = MediaFolder.generateMediaFileName(
                mediaId,
                type,
                MediaFolder.extractExtension(file.getName()));
        cv = new ContentValues();
        cv.put(MediaColumns.PATH, mFilePath);
        cr.update(uri, cv, null, null);
        file.renameTo(new File(mFilePath));
        Log.d(TAG, "storeMediaFile: " + file.getAbsolutePath() + " as " + mediaId);

        mMimeType = FILE_TYPES.get(ext);

        return mediaId;
    }

    /* for Audio, Video, Image */
    protected final long storeMessage(MediaBasic mediaBasic) {

        MessageContent messageContent = generateMessageContent(mType);
        messageContent.basicMedia = mediaBasic;
        messageContent.sourceTable = Constants.TABLE_FT;
        messageContent.generateSmsDigest(mContext);
        /* for CMCC */
        messageContent.chatType = mChatType;
        messageContent.mimeType = messageContent.basicMedia.fileType;

        return storeMessageContent(messageContent);
    }

    /* for VCard, Geoloc */
    protected final long storeMessage() {
        MessageContent messageContent = generateMessageContent(mType);
        messageContent.sourceTable = Constants.TABLE_FT;
        messageContent.text = mData;
        messageContent.mediaId = mOriginalId;
        messageContent.mediaPath = mFilePath;
        messageContent.generateSmsDigest(mContext);
        /* for CMCC */
        messageContent.chatType = mChatType;
        messageContent.mimeType = mMimeType;

        return storeMessageContent(messageContent);
    }

    protected long storeMessageContent(MessageContent messageContent) {
        ContentValues cv = new ContentValues();
        messageContent.storeToContentValues(cv);

        Uri uri = mContext.getContentResolver().insert(
                PAContract.MessageColumns.CONTENT_URI, cv);
        long msgId = Long.parseLong(uri.getLastPathSegment());
        Log.d(TAG, "storeMessage() id=" + msgId);
        return msgId;
    }

}
