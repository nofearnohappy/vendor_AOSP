package com.mediatek.mms.plugin;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.mms.ContentType;
import com.mediatek.mms.ext.DefaultOpFileAttachmentModelExt;

public class Op09FileAttachmentModelExt extends DefaultOpFileAttachmentModelExt {

    protected Context mContext;
    protected String mFileName;
    protected String mContentType;
    protected Uri mUri;
    protected int mSize;
    private static final String TAG = "Mms/Op09FileAttachmentModelExt";

    //they are only for OP09 VCard
    private String mDisplayName;
    private int mContactCount;

    @Override
    public void init(Context context, Uri uri, String fileName, String contentType, int size) {
        mContext = context;
        mFileName = fileName;
        mContentType = contentType;
        mUri = uri;
        mSize = size;

        if (MessageUtils.isSupportVCardPreview() &&
            mContentType !=null && mContentType.equalsIgnoreCase(ContentType.TEXT_VCARD)) {
            mDisplayName = VCardUtils.getVCardFirstContactName(context, uri);
            mContactCount = VCardUtils.getVCardContactsCount(context, uri);
            Log.d(TAG, "initDisplayName(): displayName = " + mDisplayName + ", mContactCount = "
                    + mContactCount);
        }
    }

    public boolean isVCard() {
        if (mContentType == null) {
            return mFileName.toLowerCase().endsWith(".vcf");
        }
        return mContentType.equalsIgnoreCase(ContentType.TEXT_VCARD);
    }

    public boolean isVCalendar() {
        if (mContentType == null) {
            return mFileName.toLowerCase().endsWith(".vcs");
        }
        return mContentType.equalsIgnoreCase(ContentType.TEXT_VCALENDAR);
    }

    public String getSrc() {
        return mFileName;
    }

    public int getAttachSize() {
        return mSize;
    }
    public Uri getUri() {
        return mUri;
    }

    public String getContentType() {
        return mContentType;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public int getContactCount() {
        return mContactCount;
    }
}
