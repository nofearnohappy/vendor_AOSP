package com.mediatek.rcs.pam.ui.messageitem;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryHandler;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.util.PaVcardData;
import com.mediatek.rcs.pam.util.PaVcardParserResult;
import com.mediatek.rcs.pam.util.PaVcardUtils;

public class MessageData {
    private MessageContent mMessageContent;
    private Context mContext;

    public int mContextMenuIndex = 0;
    public boolean mLocked; // locked to prevent auto-deletion

    private List<SoftReference<Bitmap>> mBitmapCaches = new ArrayList<SoftReference<Bitmap>>();
    private int[] mCacheBitmapWidths = new int[5];
    private int[] mCacheBitmapHeights = new int[5];

    // add for vCard
    int mVcardCount;
    String mVcardName;
    String mVcardNumber;
    Bitmap mVcardBitmap;
    private PaVcardEntryhandler mVcardEntryHandler;

    private class PaVcardEntryhandler implements VCardEntryHandler {

        @Override
        public void onEnd() {

        }

        @Override
        public void onEntryCreated(VCardEntry entry) {
            PaVcardParserResult result = PaVcardUtils.ParserRcsVcardEntry(
                    entry, mContext);

            mVcardName = result.getName();
            byte[] pic = result.getPhoto();
            if (pic != null) {
                mVcardBitmap = BitmapFactory
                        .decodeByteArray(pic, 0, pic.length);
            }
            List<PaVcardData> numbers = result.getNumber();
            if (numbers != null && numbers.size() > 0) {
                Log.d("PAM/zzy", "size = " + numbers.size());
                for (PaVcardData number : numbers) {
                    if (!number.getData().isEmpty()) {
                        mVcardNumber = number.getData();
                        Log.d("PAM/zzy", "mVcardNumber = " + mVcardNumber);
                        break;
                    }
                }
            }

        }

        @Override
        public void onStart() {

        }
    };

    public MessageData(MessageContent messageContent, Context context) {
        super();

        mMessageContent = messageContent;
        mContext = context;

        for (int i = 0; i < 5; i++) {
            SoftReference<Bitmap> bitmapCache = new SoftReference<Bitmap>(null);
            mBitmapCaches.add(bitmapCache);
        }
        initMedia();
    }

    public MessageContent getMessageContent() {
        return mMessageContent;
    }

    public Bitmap getMessageBitmap(int index) {
        return mBitmapCaches.get(index).get();
    }

    public void setMessageBitmapCache(Bitmap bitmap, int index) {
        if (null != bitmap) {
            SoftReference<Bitmap> bitmapCache = new SoftReference<Bitmap>(
                    bitmap);
            mBitmapCaches.add(index, bitmapCache);
        }
    }

    public void setMessageBitmapSize(int width, int height, int index) {
        mCacheBitmapWidths[index] = width;
        mCacheBitmapHeights[index] = height;
    }

    public int getMessageBitmapWidth(int index) {
        return mCacheBitmapWidths[index];
    }

    public int getMessageBitmapHeight(int index) {
        return mCacheBitmapHeights[index];
    }

    private void initMedia() {
        switch (mMessageContent.mediaType) {
        case Constants.MEDIA_TYPE_VCARD:
            mVcardCount = PaVcardUtils
                    .getVcardEntryCount(mMessageContent.mediaPath);
            if (mVcardCount == 1) {
                mVcardEntryHandler = new PaVcardEntryhandler();
                PaVcardUtils.parseVcard(mMessageContent.mediaPath,
                        mVcardEntryHandler);
            }
            break;
        default:
            break;
        }

    }
}
