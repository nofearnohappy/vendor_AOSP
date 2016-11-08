package com.mediatek.rcs.pam.ui.messageitem;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.ui.PAIpMsgContentShowActivity;
import com.mediatek.rcs.pam.util.Utils;

public class VideoItem extends MessageListItem {
    private static final String TAG = Constants.TAG_PREFIX + "VideoItem";

    private View mIpImageView;
    private GifImageView mImageContent;
    private View mIpImageSizeBg;
    private TextView mContentSize;
    private TextView mVideoDur;
    private ImageView mMediaPlayView;

    public VideoItem(ViewGroup layout) {
        super(layout);

        mIpImageView = (View) mLayout.findViewById(R.id.ip_image);
        mImageContent = (GifImageView) mLayout.findViewById(R.id.image_content);
        mIpImageSizeBg = (View) mLayout.findViewById(R.id.image_size_bg);
        mContentSize = (TextView) mLayout.findViewById(R.id.content_size);
        mVideoDur = (TextView) mLayout.findViewById(R.id.video_dur);
        mMediaPlayView = (ImageView) mLayout
                .findViewById(R.id.video_media_paly);

        mIpImageView.setVisibility(View.VISIBLE);
        mMediaPlayView.setVisibility(View.VISIBLE);
        mImageContent.setVisibility(View.VISIBLE);
        mIpImageSizeBg.setVisibility(View.VISIBLE);
        mVideoDur.setVisibility(View.VISIBLE);
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        if (mMessageData.getMessageContent().basicMedia == null) {
            Log.e(TAG, "mMediaBasic is null !");
            return;
        }

        initImageVideoItem(mImageContent, mIpImageSizeBg);

        try {
            int size = Integer
                    .parseInt(mMessageData.getMessageContent().basicMedia.fileSize);
            String strSize = Utils.formatFileSize(size, 2);
            mContentSize.setText(strSize);
        } catch (NumberFormatException e) {
            Log.e(TAG,
                    "Parse image size failed:"
                            + mMessageData.getMessageContent().basicMedia.fileSize);
            mContentSize.setText("");
        }

        try {
            int duration = Integer
                    .parseInt(mMessageData.getMessageContent().basicMedia.duration);
            String strDur = Utils.formatAudioTime(duration);
            mVideoDur.setText(strDur);
        } catch (NumberFormatException e) {
            Log.e(TAG,
                    "Parse image size failed:"
                            + mMessageData.getMessageContent().basicMedia.fileSize);
            mVideoDur.setText("");
        }

        String path = mMessageData.getMessageContent().basicMedia.thumbnailPath;
        if (Utils.isExistsFile(path)) {
            drawThumbnail(mImageContent, mIpImageSizeBg, 0, path,
                    DRAW_TYPE_NORMAL);
        } else if (mMessageData.getMessageContent().direction
                == Constants.MESSAGE_DIRECTION_INCOMING) {
            String url = mMessageData.getMessageContent().basicMedia.thumbnailUrl;

            sendDownloadReq(url, Constants.THUMBNAIL_TYPE, 0);
        }
    }

    @Override
    public void unbind() {
        mImageContent.setImageBitmap(null);
        mIpImageView.setVisibility(View.GONE);
        mVideoDur.setText("");
        mVideoDur.setVisibility(View.GONE);
        super.unbind();
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {
        mMessageData.getMessageContent().basicMedia.thumbnailPath = path;
        drawThumbnail(mImageContent, mIpImageSizeBg, 0, path, DRAW_TYPE_NORMAL);
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
        Intent vidoIntent = new Intent(mLayout.getContext(),
                PAIpMsgContentShowActivity.class);
        vidoIntent.putExtra("type", Constants.MEDIA_TYPE_VIDEO);
        vidoIntent.putExtra("thumbnail_url", mediaBasic.thumbnailUrl);
        vidoIntent.putExtra("thumbnail_path", mediaBasic.thumbnailPath);
        vidoIntent.putExtra("original_url", mediaBasic.originalUrl);
        vidoIntent.putExtra("original_path", mediaBasic.originalPath);
        vidoIntent.putExtra("forwardable", false);
        mLayout.getContext().startActivity(vidoIntent);
    }
}
