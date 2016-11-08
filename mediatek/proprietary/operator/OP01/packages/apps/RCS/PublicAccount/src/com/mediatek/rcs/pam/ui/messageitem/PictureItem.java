package com.mediatek.rcs.pam.ui.messageitem;

import java.io.File;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.ui.PAIpMsgContentShowActivity;
import com.mediatek.rcs.pam.util.Utils;

public class PictureItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX + "PictureItem";
    private static final int THUMBNAIL = 0;
    private static final int GIF = 1;

    private View mIpImageView;
    private GifImageView mImageContent;
    private View mIpImageSizeBg;
    private TextView mContentSize;
    private TextView mVideoDur;
    private ImageView mMediaPlayView;
    private ProgressBar mProgressBar;
    private TextView mProgressText;

    private boolean mIsGif;

    public PictureItem(ViewGroup layout) {
        super(layout);
        mIsGif = false;

        mIpImageView = (View) mLayout.findViewById(R.id.ip_image);
        mImageContent = (GifImageView) mLayout.findViewById(R.id.image_content);
        mIpImageSizeBg = (View) mLayout.findViewById(R.id.image_size_bg);
        mContentSize = (TextView) mLayout.findViewById(R.id.content_size);
        mVideoDur = (TextView) mLayout.findViewById(R.id.video_dur);
        mMediaPlayView = (ImageView) mLayout
                .findViewById(R.id.video_media_paly);
        mProgressText = (TextView) mLayout.findViewById(R.id.progress_text);
        mProgressBar = (ProgressBar) mLayout.findViewById(R.id.progress_large);

        mIpImageView.setVisibility(View.VISIBLE);
        mImageContent.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.VISIBLE);
        mVideoDur.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mProgressText.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        if (mMessageData.getMessageContent().basicMedia == null) {
            Log.e(TAG, "mMediaBasic is null !");
            return;
        }

        initImageVideoItem(mImageContent, mIpImageSizeBg);

        String imageUrl = mMessageData.getMessageContent().basicMedia.originalUrl;
        String ext = Utils.getFileExtension(imageUrl);
        if ("gif".equalsIgnoreCase(ext)) {
            mIsGif = true;
        } else {
            mIsGif = false;
        }

        try {
            int size = Integer
                    .parseInt(mMessageData.getMessageContent().basicMedia.fileSize);
            mContentSize.setText(Utils.formatFileSize(size, 2));
        } catch (NumberFormatException e) {
            Log.e(TAG,
                    "Parse image size failed:"
                            + mMessageData.getMessageContent().basicMedia.fileSize);
            mContentSize.setText("");
        }

        if (mMessageData.getMessageContent().direction == Constants.MESSAGE_DIRECTION_OUTGOING) {
            String imagePath = mMessageData.getMessageContent().basicMedia.originalPath;
            if (Utils.isExistsFile(imagePath)) {
                if (mIsGif) {
                    drawGif(mImageContent, 0, imagePath);
                } else {
                    drawThumbnail(mImageContent, mIpImageSizeBg, 0, imagePath,
                            DRAW_TYPE_NORMAL);
                }
            }
        } else {
            if (mIsGif) {
                String imagePath = mMessageData.getMessageContent().basicMedia.originalPath;
                if (Utils.isExistsFile(imagePath)) {
                    drawGif(mImageContent, 0, imagePath);
                    return;
                } else {
                    mMediaPlayView.setVisibility(View.VISIBLE);
                }
            }
            String path = mMessageData.getMessageContent().basicMedia.thumbnailPath;
            if (Utils.isExistsFile(path)) {
                drawThumbnail(mImageContent, mIpImageSizeBg, 0, path,
                        DRAW_TYPE_NORMAL);
            } else {
                String url = mMessageData.getMessageContent().basicMedia.thumbnailUrl;
                sendDownloadReq(url, Constants.THUMBNAIL_TYPE, THUMBNAIL);
            }
        }
    }

    @Override
    public void unbind() {
        mImageContent.setImageBitmap(null);
        mImageContent.unbind();
        mIpImageView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mProgressText.setVisibility(View.GONE);
        mMediaPlayView.setVisibility(View.GONE);
        super.unbind();
    }

    @Override
    protected void updateDownloadProgress(final int index, final int percentage) {
        if (index == GIF) {
            mProgressText.setText(percentage + "%");
        }
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {
        if (index == GIF) {
            mProgressBar.setVisibility(View.GONE);
            mProgressText.setVisibility(View.GONE);
            mMessageData.getMessageContent().basicMedia.originalPath = path;
            drawGif(mImageContent, 0, path);
        } else if (index == THUMBNAIL) {
            mMessageData.getMessageContent().basicMedia.thumbnailPath = path;
            drawThumbnail(mImageContent, mIpImageSizeBg, 0, path,
                    DRAW_TYPE_NORMAL);
        } else {
            Log.e(TAG, "updateAfterDownload index = " + index);
        }
    }

    @Override
    public void onMessageListItemClick() {
        if (mMessageData == null) {
            Log.d(TAG, "onMessageListItemClick():Message item is null !");
            return;
        }
        MediaBasic mediaBasic = mMessageData.getMessageContent().basicMedia;
        if (mediaBasic == null) {
            Toast.makeText(mLayout.getContext(), "mediaBasic is null",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (mIsGif == true && mediaBasic.originalPath == null) {
            mMediaPlayView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressText.setVisibility(View.VISIBLE);
            sendDownloadReq(mediaBasic.originalUrl,
                    Constants.MEDIA_TYPE_PICTURE, GIF);
        } else {
            Intent picIntent = new Intent(mLayout.getContext(),
                    PAIpMsgContentShowActivity.class);
            picIntent.putExtra("type", Constants.MEDIA_TYPE_PICTURE);
            picIntent.putExtra("thumbnail_url", mediaBasic.thumbnailUrl);
            picIntent.putExtra("thumbnail_path", mediaBasic.thumbnailPath);
            picIntent.putExtra("original_url", mediaBasic.originalUrl);
            picIntent.putExtra("original_path", mediaBasic.originalPath);
            picIntent.putExtra("forwardable", false);
            mLayout.getContext().startActivity(picIntent);
        }

    }

    private boolean drawGif(GifImageView iv, int index, String path) {
        Log.d(TAG, "drawGif path=" + path);

        if (path == null || path.isEmpty()) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            Log.d(TAG, "drawThumbnail() but file not exist");
            return false;
        }

        iv.drawGif(path);

        return true;
    }
}
