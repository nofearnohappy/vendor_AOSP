/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
*
* BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
* THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
* RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
* AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
* NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
* SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
* SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
* THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
* THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
* CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
* SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
* STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
* CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
* AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
* The following software/firmware and/or related documentation ("MediaTek Software")
* have been modified by MediaTek Inc. All revisions are subject to any receiver's
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.rcs.incallui.video;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;
import com.mediatek.rcs.incallui.RichCallPanel;
import com.mediatek.rcs.phone.R;

import java.io.IOException;

public class RichCallVideoPanel extends RichCallPanel
    implements MediaPlayer.OnErrorListener,
    MediaPlayer.OnInfoListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnVideoSizeChangedListener {
    private static final String TAG = "RichCallVideoPanel";
    private static int   sVideoWidth = 480;
    private static int   sVideoHeight = 450;

    private RichCallInfo mRichCallInfo;
    private SurfaceHolder     mSurfaceHolder;
    private MediaPlayer       mMediaPlayer;
    private VideoSurfaceView  mVideoSurfaceView;
    private FrameLayout       mVideoLayout;
    private String            mDataPath;

    private ImageView         mCoverImageView;
    private ImageView         mRecordImageView;

    private int               mCurrentMusicVolume;
    private int               mMaxMusicVolume;
    private AudioManager      mAudioManager;
    private boolean mIsMute;
    public RichCallVideoPanel(Context cnx, RCSInCallUIPlugin plugin) {
        super(cnx, plugin);
        mPanelType = RCS_PANEL_VIDEO;
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format,
                int width, int height) {

            Log.d(TAG, "surfaceChanged");
            try {
                mMediaPlayer.setDisplay(holder);
                mMediaPlayer.prepareAsync();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            try {
                mMediaPlayer.setDataSource(mDataPath);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed, need to close video panel");
        }
    };

    @Override
    public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
        Log.d(TAG, "onVideoSizeChanged");
    }

   @Override
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "onPrepared~");
        int width = player.getVideoWidth();
        int height = player.getVideoHeight();
        //Need to mute the video volume.
        player.setVolume(0.0f, 0.0f);
        player.start();
   }

    @Override
    public boolean onInfo(MediaPlayer player, int whatInfo, int extra) {
        Log.d(TAG, "onInfo, info = " + whatInfo);
        switch (whatInfo) {
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                //When the video data is ready, need to dismiss the covered image.
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mCoverImageView.setVisibility(View.GONE);
                                        }
                                    }, 300);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onError(MediaPlayer player, int whatError, int extra) {
        Log.d(TAG, "onError, Error = " + whatError);
        switch (whatError) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                break;
            default:
                break;
        }
        return false;
    }

    public void init() {
        Log.d(TAG, "init");
        mVideoLayout = mRCSInCallUIPlugin.getVideoPanelRect();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        getAudioVolume();
    }

    @Override
    public void openPanel(RichCallInfo info) {
        Log.d(TAG, "openPanel, isPanelOpen = " + mPanelOpen);
        if (mRCSInCallUIPlugin.shouldShowPanel() /*&& !isVideoPanelOpen()*/) {
            removeView();
            initMediaPlayer();

            //Created surface the mediaplayer needed.
            mVideoSurfaceView = new VideoSurfaceView(mContext);
            mVideoSurfaceView.setZOrderMediaOverlay(true);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);

            mVideoSurfaceView.setLayoutParams(layoutParams);
            mVideoSurfaceView.setAspectRatio(sVideoHeight, sVideoWidth);
            mSurfaceHolder = mVideoSurfaceView.getHolder();
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mSurfaceHolder.addCallback(mSurfaceCallback);
            mVideoLayout.addView(mVideoSurfaceView, layoutParams);

            //Need to add cover image on surfaceview, or else when surface is ready but
            //data is not ready, will show backgroud activity. The surface is transparent.
            mCoverImageView = new ImageView(mContext);
            mCoverImageView.setLayoutParams(layoutParams);
            mCoverImageView.setImageResource(R.drawable.default_video_screen);
            mVideoLayout.addView(mCoverImageView, layoutParams);

            //Add record icon for record a phone call.
            FrameLayout.LayoutParams recordParams =
                    new FrameLayout.LayoutParams(
                        dip2dx(mContext, R.dimen.incall_record_icon_size),
                        dip2dx(mContext, R.dimen.incall_record_icon_size));

            recordParams.setMargins(0, dip2dx(mContext, R.dimen.incall_record_icon_margin),
                    dip2dx(mContext, R.dimen.incall_record_icon_margin), 0);

            recordParams.gravity = Gravity.RIGHT;

            mRecordImageView = new ImageView(mContext);
            mRecordImageView.setLayoutParams(recordParams);
            mRecordImageView.setImageResource(R.drawable.voice_record);
            mRecordImageView.setVisibility(View.GONE);
            mVideoLayout.addView(mRecordImageView, recordParams);

            mVideoLayout.setVisibility(View.VISIBLE);

            mPanelOpen = true;
            mDataPath = info.mUri;
        }
    }

    @Override
    public void closePanel() {
        Log.d(TAG, "closePanel, isPanelOpen = " + mPanelOpen);
        if (isPanelOpen()) {
            stopVideo();
            mSurfaceHolder.removeCallback(mSurfaceCallback);
            mVideoLayout.setVisibility(View.INVISIBLE);
            mPanelOpen = false;
            //unmuteVideo();
            releaseMediaPlayer();
        }
    }

    private void getAudioVolume() {
        mCurrentMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mMaxMusicVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "getAudioVolume, mCurrentMusicVolume = " + mCurrentMusicVolume +
                        ", mMaxMusicVolume = " + mMaxMusicVolume);
    }

    private void muteVideo() {
        Log.d(TAG, "muteVideo");
        if (isPanelOpen()) {
            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(0.0f, 0.0f);
            }
        }
    }

    private void unmuteVideo() {
        Log.d(TAG, "unmuteVideo");
        float currentVolume = 0.0f;
        if (isPanelOpen()) {
            if (mMaxMusicVolume != 0) {
                currentVolume = mCurrentMusicVolume / mMaxMusicVolume;
            }
            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(currentVolume, currentVolume);
            }
        }
    }

    private void playVideo() {
        Log.d(TAG, "playVideo");
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    public void stopVideo() {
        Log.d(TAG, "stopVideo");
        if (isPanelOpen()) {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
            }
        }
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        if (isPanelOpen()) {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
            }
        }
    }

    private void removeView() {
        releaseMediaPlayer();
        if (mVideoLayout != null && mVideoSurfaceView != null) {
            mVideoLayout.removeView(mVideoSurfaceView);
            mVideoLayout.removeView(mCoverImageView);
            mVideoLayout.removeView(mRecordImageView);
            mVideoSurfaceView = null;
            mCoverImageView = null;
            mRecordImageView = null;
        }
    }

    @Override
    public void releaseResource() {
        Log.d(TAG, "releaseResource");
        removeView();
        ViewGroup group = mRCSInCallUIPlugin.getHostViewGroup();
        if (group != null) {
            group.removeView(mVideoLayout);
            mVideoLayout = null;
        }
        mRCSInCallUIPlugin = null;
    }

    private void initMediaPlayer() {
        Log.d(TAG, "initMediaPlayer");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
    }

    private void releaseMediaPlayer() {
        Log.d(TAG, "releaseMediaPlayer");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void updateAudioState(boolean visible) {
        //Update audio record state, plugin will trigger this func.
        if (isPanelOpen() && mRecordImageView != null) {
            mRecordImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
            AnimationDrawable ad = (AnimationDrawable) mRecordImageView.getDrawable();
            if (ad != null) {
                if (visible && !ad.isRunning()) {
                    ad.start();
                } else if (!visible && ad.isRunning()) {
                    ad.stop();
                }
            }
        }
    }
}