package com.mediatek.rcs.pam.ui.messageitem;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.model.MediaBasic;
import com.mediatek.rcs.pam.ui.messageitem.PAAudioService.IAudioServiceCallBack;
import com.mediatek.rcs.pam.util.Utils;

public class AudioItem extends MessageListItem implements IAudioServiceCallBack {
    private static final String TAG = Constants.TAG_PREFIX + "AudioItem";
    private PAAudioService mAudioService;

    private View mIpAudioView;
    private View mIpAudioPreview;
    private TextView mAudioInfo;
    private TextView mAudioDur;
    private ProgressBar mAudioDownloadBar;
    private View mIpAudioPlay;
    private ProgressBar mAudioPlayBar;
    private TextView mAudioTimePlay;
    private TextView mAudioTimeDuration;

    public AudioItem(ViewGroup layout) {
        super(layout);

        mIpAudioView = (View) mLayout.findViewById(R.id.ip_audio);
        mIpAudioPreview = (View) mLayout.findViewById(R.id.ip_audio_preview);
        mAudioInfo = (TextView) mLayout.findViewById(R.id.ip_audio_info);
        mAudioDur = (TextView) mLayout.findViewById(R.id.ip_audio_dur);
        mAudioDownloadBar = (ProgressBar) mLayout
                .findViewById(R.id.ip_audio_downLoad_bar);
        mIpAudioPlay = (View) mLayout.findViewById(R.id.ip_audio_play);
        mAudioPlayBar = (ProgressBar) mLayout
                .findViewById(R.id.ip_audio_play_bar);
        if (mAudioPlayBar != null) {
            mAudioPlayBar.setMax(100);
        }
        mAudioTimePlay = (TextView) mLayout
                .findViewById(R.id.ip_audio_time_play);
        mAudioTimeDuration = (TextView) mLayout
                .findViewById(R.id.ip_audio_time_duration);

        mIpAudioView.setVisibility(View.VISIBLE);
        mIpAudioPreview.setVisibility(View.VISIBLE);
        mIpAudioPlay.setVisibility(View.GONE);
    }

    @Override
    public void bind(MessageData messageData) {
        super.bind(messageData);

        if (mMessageData.getMessageContent().basicMedia == null) {
            Log.e(TAG, "mMediaBasic is null !");
            return;
        }
        mAudioService = PAAudioService.getService();
        String audioUrl = mMessageData.getMessageContent().basicMedia.originalUrl;
        String audioPath = mMessageData.getMessageContent().basicMedia.originalPath;
        if (audioPath == null || audioPath.isEmpty()) {
            // audio haven't download,start download
            sendDownloadReq(audioUrl,
                    mMessageData.getMessageContent().mediaType, 0);
            showAudioPreview(true);
        } else {
            if (mAudioService.bindAudio(mMessageData.getMessageContent().id,
                    mMessageData.getMessageContent().basicMedia.originalPath,
                    this)) {
                showAudioPlay(false);
            } else {
                showAudioPreview(false);
            }
        }
    }

    @Override
    public void unbind() {
        mAudioInfo.setText("");
        mAudioDur.setText("");
        mIpAudioView.setVisibility(View.GONE);
        if (mAudioService != null) {
            mAudioService.unBindAudio(mMessageData.getMessageContent().id);
        }
        mAudioService = null;
        super.unbind();
    }

    @Override
    protected void updateAfterDownload(final int index, String path) {
        mMessageData.getMessageContent().basicMedia.originalPath = path;
        showAudioPreview(false);
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
        if (mediaBasic.originalPath != null) {
            boolean reset = mAudioService.bindAudio(
                    mMessageData.getMessageContent().id,
                    mediaBasic.originalPath, this);
            boolean ret = mAudioService.touch(
                    mMessageData.getMessageContent().id,
                    mediaBasic.originalPath, this);
            if (ret) {
                showAudioPlay(!reset);
            }
        } else {
            Log.w(TAG, "onMessageListItemClick() Audio is not ready.");
        }
    }

    private void showAudioPreview(boolean isDownloading) {
        mIpAudioPreview.setVisibility(View.VISIBLE);
        mIpAudioPlay.setVisibility(View.GONE);

        String strFileSize = null;
        String strDuration = null;
        int fileSize = 0;
        int duration = 0;

        try {
            fileSize = Integer
                    .parseInt(mMessageData.getMessageContent().basicMedia.fileSize);
            strFileSize = Utils.formatFileSize(fileSize, 2);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG,
                    "Audio size parse error:"
                            + mMessageData.getMessageContent().basicMedia.fileSize);
        }

        try {
            duration = Integer
                    .parseInt(mMessageData.getMessageContent().basicMedia.duration);
            strDuration = Utils.formatAudioTime(duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG,
                    "Audio duration parse error:"
                            + mMessageData.getMessageContent().basicMedia.duration);
        }

        if (null == strFileSize) {
            strFileSize = mMessageData.getMessageContent().basicMedia.fileSize;
        }
        if (null == strDuration) {
            strDuration = mMessageData.getMessageContent().basicMedia.duration;
        }
        if (strFileSize != null) {
            mAudioInfo.setText(strFileSize);
        }
        if (strDuration != null) {
            mAudioDur.setText(strDuration);
        }
        if (isDownloading) {
            mAudioDownloadBar.setVisibility(View.VISIBLE);
        } else {
            mAudioDownloadBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showAudioPlay(boolean reset) {
        mIpAudioPreview.setVisibility(View.GONE);
        mIpAudioPlay.setVisibility(View.VISIBLE);

        mAudioTimeDuration.setText(getTimeFromInt(mAudioService.getDuration()));
        String currentTime = getTimeFromInt(0);
        int persentage = 0;
        if (!reset) {
            int currentPos = mAudioService.getCurrentPosition();
            int duration = mAudioService.getDuration();
            persentage = (int) Math.round(((double) currentPos) * 100
                    / duration);
            currentTime = getTimeFromInt(currentPos);
        }
        mAudioTimePlay.setText(currentTime);
        mAudioPlayBar.setProgress(persentage);
    }

    private String getTimeFromInt(int time) {

        if (time <= 0) {
            return "0:00";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss",
                Locale.getDefault());
        return formatter.format(time);
    }

    @Override
    public void onError(int what, int extra) {
        mLayout.post(new Runnable() {

            @Override
            public void run() {
                showAudioPreview(false);
            }
        });
    }

    @Override
    public void onCompletion() {
        mLayout.post(new Runnable() {

            @Override
            public void run() {
                showAudioPreview(false);
            }
        });
    }

    @Override
    public void onPlayProgress(final int persentage, final int currentTime) {
        mLayout.post(new Runnable() {

            @Override
            public void run() {
                mAudioTimePlay.setText(getTimeFromInt(currentTime));
                mAudioPlayBar.setProgress(persentage);
            }
        });
        Log.d(TAG, "Timer task update: " + persentage);
    }
}
