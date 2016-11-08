package com.mediatek.rcs.pam.ui.messageitem;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.ui.FileDownloader;
import com.mediatek.rcs.pam.util.Utils;

public abstract class MessageListItem {
    private static String TAG = Constants.TAG_PREFIX + "MessageListItem";

    protected static final int DRAW_TYPE_NORMAL = 0;
    protected static final int DRAW_TYPE_SMALL = 1;

    protected MessageData mMessageData;
    protected ViewGroup mLayout;

    public MessageListItem(ViewGroup layout) {
        mLayout = layout;
    }

    public MessageData getMessageData() {
        return mMessageData;
    }

    public void setBodyTextSize(float size) {
    }

    public void bind(MessageData messageData) {
        Log.i(TAG, "MessageListItem() id = "
                + messageData.getMessageContent().id + ", type = "
                + messageData.getMessageContent().mediaType);
        mMessageData = messageData;
    }

    public void unbind() {
        if (null == mMessageData) {
            Log.e(TAG, "unbind() but mMessageData is null.");
            return;
        }
        Log.e(TAG, "unbind: msgItemId=" + mMessageData.getMessageContent().id);

        mMessageData = null;
    }

    abstract protected void updateAfterDownload(final int index, String path);

    protected void updateDownloadProgress(final int index, int percentage) {

    }

    abstract public void onMessageListItemClick();

    public static MessageListItem generateItem(MessageData message,
            ViewGroup layout) {
        MessageListItem item = null;
        switch (message.getMessageContent().mediaType) {
        case Constants.MEDIA_TYPE_TEXT:
        case Constants.MEDIA_TYPE_SMS:
            item = new TextItem(layout);
            break;
        case Constants.MEDIA_TYPE_PICTURE:
            item = new PictureItem(
                    (ViewGroup) layout.findViewById(R.id.ip_image));
            break;
        case Constants.MEDIA_TYPE_VIDEO:
            item = new VideoItem((ViewGroup) layout.findViewById(R.id.ip_image));
            break;
        case Constants.MEDIA_TYPE_AUDIO:
            item = new AudioItem((ViewGroup) layout.findViewById(R.id.ip_audio));
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            item = new SingleArticleItem(
                    (ViewGroup) layout.findViewById(R.id.ip_single_mix));
            break;
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            item = new MultipleArticleItem(
                    (ViewGroup) layout.findViewById(R.id.ip_multi_mix));
            break;
        case Constants.MEDIA_TYPE_VCARD:
            item = new VCardItem((ViewGroup) layout.findViewById(R.id.ip_vcard));
            break;
        case Constants.MEDIA_TYPE_GEOLOC:
            item = new GeolocItem(
                    (ViewGroup) layout.findViewById(R.id.ip_geoloc));
            break;
        default:
            throw new IllegalArgumentException();
        }
        return item;
    }

    protected void initImageVideoItem(ImageView imageView, View bgView) {
        FrameLayout.LayoutParams para = new FrameLayout.LayoutParams(352, 288);
        imageView.setLayoutParams(para);
        imageView.setImageDrawable(new ColorDrawable(
                android.R.color.transparent));
        adjustTextWidth(352, imageView);
    }

    private void adjustTextWidth(int width, View view) {
        if ((mMessageData.getMessageContent().mediaType != Constants.MEDIA_TYPE_PICTURE)
                && (mMessageData.getMessageContent().mediaType != Constants.MEDIA_TYPE_VIDEO)) {
            return;
        }

        if (view != null && (view.getVisibility() == View.VISIBLE)) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = width;
            view.setLayoutParams(lp);
            Log.d(TAG, "adjustTextWidth=" + lp.width);
        }
    }

    protected void sendDownloadReq(String url, int type, int index) {
        Log.d(TAG, "sendDownloadReq():" + mMessageData.getMessageContent().id
                + "|" + index);
        FileDownloader.getInstance().sendDownloadRequest(url, type,
                mMessageData.getMessageContent().id, index,
                new FileDownloader.DownloadListener() {

                    @Override
                    public void reportDownloadResult(int resultCode,
                            final String path, long mediaId, long msgId,
                            final int indexz) {
                        Log.d(TAG, "resultCode=" + resultCode + ", path = "
                                + path + ", msgId = " + msgId + ", indexz = "
                                + indexz);
                        if (resultCode == ResultCode.SUCCESS) {
                            MessageListItem.this.CheckAndpost(msgId,
                                    new Runnable() {

                                        @Override
                                        public void run() {
                                            updateAfterDownload(indexz, path);
                                        }
                                    });
                        }
                    }

                    public void reportDownloadProgress(long msgId,
                            final int index, final int percentage) {
                        MessageListItem.this.CheckAndpost(msgId,
                                new Runnable() {

                                    @Override
                                    public void run() {
                                        updateDownloadProgress(index,
                                                percentage);
                                    }
                                });
                    }
                });
    }

    protected boolean drawThumbnail(ImageView iv, View bgView, int index,
            String path, int type) {
        Log.d(TAG, "drawThumbnail path=" + path);

        Bitmap bitmap = mMessageData.getMessageBitmap(index);
        if (path == null || path.isEmpty()) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            Log.d(TAG, "drawThumbnail() but file not exist");
            return false;
        }

        if (null == bitmap) {
            Log.d(TAG, "drawThumbnail() bitmap cache is null, do redraw");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            bitmap = BitmapFactory.decodeFile(path, options);
            Size size = new Size(200, 200);

            if (DRAW_TYPE_NORMAL == type) {
                size = Utils.getFineImageSize(new Size(options.outWidth,
                        options.outHeight), mLayout.getContext());
            } else if (type == DRAW_TYPE_SMALL) {
                size = new Size(200, 200);
            }

            bitmap = Utils.getBitmapByPath(path, options, size.getWidth(),
                    size.getHeight());
            mMessageData.setMessageBitmapSize(size.getWidth(),
                    size.getHeight(), index);
            mMessageData.setMessageBitmapCache(bitmap, index);
        }

        if (null != bitmap) {
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) iv
                    .getLayoutParams();
            params.height = mMessageData.getMessageBitmapHeight(index);
            params.width = mMessageData.getMessageBitmapWidth(index);
            iv.setLayoutParams(params);
            iv.setImageBitmap(bitmap);
            Log.d(TAG, "drawThumbnail done. size=" + params.width + ","
                    + params.height);
            adjustTextWidth(params.width, bgView);
            return true;
        }
        return false;
    }

    private boolean CheckAndpost(final long token, final Runnable runnable) {
        if (null == mMessageData) {
            Log.d(TAG, "post() but mMessageData is null.");
            return false;
        }

        // if this view is not attached, wait not more than 1 second
        int timeCount = 0;
        while (mLayout.getHandler() == null && timeCount < 10) {
            SystemClock.sleep(100);
            timeCount++;
        }
        if (mLayout.getHandler() == null) {
            Log.d(TAG, "post() but view is not attached!");
            return false;
        }

        Runnable realrun = new Runnable() {

            @Override
            public void run() {
                if (mMessageData != null
                        && token == mMessageData.getMessageContent().id) {
                    runnable.run();
                }

            }
        };
        return mLayout.post(realrun);
    }

}
