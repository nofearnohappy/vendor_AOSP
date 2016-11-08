package com.mediatek.rcs.pam.ui.messagehistory;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageHistorySummaryColumns;
import com.mediatek.rcs.pam.provider.PAContract.SearchColumns;
import com.mediatek.rcs.pam.ui.FileDownloader;

import java.io.File;

public class MessageHistoryItem {
    private final Context mContext;
    public long id = Constants.INVALID;
    public String accountUuid;
    public String accountName;
    public long logoId = Constants.INVALID;
    public String logoPath;
    public String logoUrl;
    public Bitmap logoBitmap = null;
    public long lastMessageId = Constants.INVALID;
    public String lastMessageSummary;
    public long lastMessageTimestamp;

    public MessageHistoryItemView view;

    public MessageHistoryItem(Context context, Cursor cursor, boolean search) {
        mContext = context;
        loadFromCursor(cursor, search);
    }

    private String getUuidFromAccountId(long accountId) {
        String uuid = null;
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(AccountColumns.CONTENT_URI,
                    new String[] { AccountColumns.UUID }, AccountColumns.ID + "=?",
                    new String[] { Long.toString(accountId) }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                uuid = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return uuid;
    }

    public void loadFromCursor(Cursor cursor, boolean search) {
        if (search) {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_ID));
            accountName = cursor
                    .getString(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_NAME));
            accountUuid = getUuidFromAccountId(id);
            logoId = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_LOGO_ID));
            logoPath = cursor.getString(cursor
                    .getColumnIndexOrThrow(SearchColumns.ACCOUNT_LOGO_PATH));
            logoUrl = cursor
                    .getString(cursor.getColumnIndexOrThrow(SearchColumns.ACCOUNT_LOGO_URL));
            lastMessageId = cursor.getLong(cursor.getColumnIndexOrThrow(SearchColumns.MESSAGE_ID));
            lastMessageSummary = cursor.getString(cursor
                    .getColumnIndexOrThrow(SearchColumns.MESSAGE_SUMMARY));
            lastMessageTimestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(SearchColumns.MESSAGE_TIMESTAMP));

        } else {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MessageHistorySummaryColumns.ID));
            accountUuid = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.UUID));
            accountName = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.NAME));
            logoId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LOGO_ID));
            logoPath = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LOGO_PATH));
            logoUrl = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LOGO_URL));
            lastMessageId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LAST_MESSAGE_ID));
            lastMessageSummary = cursor.getString(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LAST_MESSAGE_SUMMARY));
            lastMessageTimestamp = cursor.getLong(cursor
                    .getColumnIndexOrThrow(MessageHistorySummaryColumns.LAST_MESSAGE_TIMESTAMP));

        }
        // load image
        if (logoPath == null) {
            downloadLogo();
        } else {
            File file = new File(logoPath);
            if (file.exists()) {
                logoBitmap = BitmapFactory.decodeFile(logoPath);
            } else {
                downloadLogo();
            }
        }
    }

    private void downloadLogo() {

        FileDownloader.getInstance().sendDownloadRequest(logoUrl, Constants.MEDIA_TYPE_PICTURE, -1,
                0, new FileDownloader.DownloadListener() {
                    @Override
                    public void reportDownloadResult(int resultCode, final String path,
                            long mediaId, long msgId, int index) {
                        if (resultCode == ResultCode.SUCCESS) {
                            logoPath = path;
                            logoBitmap = BitmapFactory.decodeFile(logoPath);
                            if (view != null) {
                                view.setLogo(logoBitmap);
                            }
                        }
                    }

                    @Override
                    public void reportDownloadProgress(long msgId, int index, int percentage) {

                    }
                });
    }
}
