/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.camera.mode.pip.recorder;

import java.io.IOException;

import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import com.mediatek.media.MediaRecorderEx;
/**
 * This class is used to wrap MediaRecorder or MediaCodec video recording.
 * <p>
 * When MediaRecorder.getSurface is enable, we will go MediaRecorder path,
 * otherwise we will choose MediaCodec path.
 *
 */
public class MediaRecorderWrapper implements MediaCodecRecorder.OnInfoListener,
        MediaRecorder.OnInfoListener {
    private static final String        TAG = "MediaRecorderWrapper";
    private MediaRecorder              mMediaRecorder;
    private MediaCodecRecorder         mMediaCodecRecorder;
    private boolean                    mIsUseMediaCodec = true;
    private OnInfoListener             mOnInfoListener;
    /**
     * Unspecified media recorder error.
     */
    public static final int MEDIA_RECORDER_INFO_UNKNOWN               = 1;
    /**
     * A maximum duration had been setup and has now been reached.
     */
    public static final int MEDIA_RECORDER_INFO_MAX_DURATION_REACHED  = 800;
    /**
     * A maximum file size had been setup and has now been reached.
     */
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED  = 801;
    public static final int MEDIA_RECORDER_INFO_RECORDED_SIZE         = 895;
    public static final int MEDIA_RECORDER_INFO_SESSIONID             = 896;
    public static final int MEDIA_RECORDER_INFO_FPS_ADJUSTED          = 897;
    public static final int MEDIA_RECORDER_INFO_BITRATE_ADJUSTED      = 898;
    public static final int MEDIA_RECORDER_INFO_WRITE_SLOW            = 899;
    public static final int MEDIA_RECORDER_INFO_START_TIMER           = 1998;
    public static final int MEDIA_RECORDER_INFO_CAMERA_RELEASE        = 1999;
    public static final String RECORDER_INFO_SUFFIX = "media-recorder-info=";
    /**
     * Interface definition for a callback to be invoked while recording.
     */
    public interface OnInfoListener {
        /**
         * Called when an informational event occurs while recording
         *
         * @param mr the MediaRecorderWrapper that encountered the event
         * @param what    the type of event that has occurred:
         * @param extra   an extra code, specific to the event type
         */
        void onInfo(MediaRecorderWrapper mr, int what, int extra);
    }


    @Override
    public void onMediaMuxerStarted() {

    }

    @Override
    public void onMaxFileSizeReached() {
        if (mOnInfoListener != null) {
            mOnInfoListener.onInfo(this, MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED, 0);
        }
    }

    @Override
    public void onFirstVideoFrameRecorded() {
        if (mOnInfoListener != null) {
            mOnInfoListener.onInfo(this, MEDIA_RECORDER_INFO_START_TIMER, 0);
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (mOnInfoListener != null) {
            mOnInfoListener.onInfo(this, what, extra);
        }
    }

    public MediaRecorderWrapper(boolean useMediaCodec) {
        Log.i(TAG, "MediaRecorderWrapper useMediaCodec = " + useMediaCodec);
        mIsUseMediaCodec = useMediaCodec;
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder = new MediaCodecRecorder();
        } else {
            mMediaRecorder = new MediaRecorder();
        }
    }

    /*****************************************common part ****************************************/
    public void setOutputFormat(int fileFormat) {
        Log.i(TAG, "Initialize >>> setOutputFormat = " + fileFormat);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setOutputFormat(fileFormat);
        } else {
            mMediaRecorder.setOutputFormat(fileFormat);
        }
    }

    public void setMaxFileSize(long maxFileSize) {
        Log.i(TAG, "Initialize >>> setMaxFileSize = " + maxFileSize);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setMaxFileSize(maxFileSize);
        } else {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        }
    }

    public void setOutputFile(String path) {
        Log.i(TAG, "Initialize >>> setOutputFile = " + path);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setOutputFile(path);
        } else {
            mMediaRecorder.setOutputFile(path);
        }
    }

    public void setOrientationHint(int degrees) {
        Log.i(TAG, "Initialize >>> setOrientationHint = " + degrees);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setOrientationHint(degrees);
        } else {
            mMediaRecorder.setOrientationHint(degrees);
        }
    }

    public void setLocation(long latitude, long longitude) {
        Log.i(TAG, "Initialize >>> setLocation latitude = " + latitude
                    + " longitude = " + longitude);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setLocation(latitude, longitude);
        } else {
            mMediaRecorder.setLocation(latitude, longitude);
        }
    }

    /*****************************************video part *****************************************/
    public void setVideoEncoder(VideoEncoder videoEncorder) throws IllegalArgumentException {
        Log.i(TAG, "Initialize >>> setVideoEncoder videoEncorder =  " + videoEncorder);
        if (!mIsUseMediaCodec) {
            throw new IllegalArgumentException("setVideoEncoder(VideoEncoder videoEncorder)" +
                    "must be use MediaCodec recording!");
        }
        mMediaCodecRecorder.setVideoEncoder(videoEncorder);
    }

    public void setVideoEncoder(int video_encoder) throws IllegalStateException,
            IllegalArgumentException {
        Log.i(TAG, "Initialize >>> setVideoEncoder video_encoder =  " + video_encoder);
        if (mIsUseMediaCodec) {
            throw new IllegalArgumentException("setVideoEncoder(int video_encoder)" +
                    "must be use MediaRecorder recording!");
        }
        mMediaRecorder.setVideoEncoder(video_encoder);
    }

    public void setVideoSource(int video_source) {
        Log.i(TAG, "Initialize >>> setVideoSource video_source =  " + video_source);
        if (mIsUseMediaCodec) {
            throw new IllegalArgumentException("setVideoEncoder(int video_encoder)" +
                    "must be use MediaRecorder recording!");
        }
        mMediaRecorder.setVideoSource(video_source);
    }

    public void setVideoFrameRate(int videoFrameRate) {
        Log.i(TAG, "Initialize >>> setVideoFrameRate videoFrameRate =  " + videoFrameRate);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setVideoFrameRate(videoFrameRate);
        } else {
            mMediaRecorder.setVideoFrameRate(videoFrameRate);
        }
    }

    public void setVideoSize(int videoFrameWidth, int videoFrameHeight) {
        Log.i(TAG, "Initialize >>> setVideoSize videoFrameWidth =  " + videoFrameWidth
                   + " videoFrameHeight = " + videoFrameHeight);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setVideoSize(videoFrameWidth, videoFrameHeight);
        } else {
            mMediaRecorder.setVideoSize(videoFrameWidth, videoFrameHeight);
        }
    }

    public void setVideoEncodingBitRate(int bitRate) {
        Log.i(TAG, "Initialize >>> setVideoEncodingBitRate bitRate =  " + bitRate);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setVideoEncodingBitRate(bitRate);
        } else {
            mMediaRecorder.setVideoEncodingBitRate(bitRate);
        }
    }

    /*****************************************audio part *****************************************/
    public void setAudioEncoder(AudioEncoder audioEncorder) throws IllegalArgumentException {
        Log.i(TAG, "Initialize >>> setAudioEncoder audioEncorder =  " + audioEncorder);
        if (!mIsUseMediaCodec) {
            throw new IllegalArgumentException("setAudioEncoder(AudioEncoder audioEncorder) " +
                    "must be use MediaCodec recording!");
        }
        mMediaCodecRecorder.setAudioEncoder(audioEncorder);
    }

    public void setAudioEncoder(int audio_encoder) throws IllegalStateException,
            IllegalArgumentException {
        Log.i(TAG, "Initialize >>> setAudioEncoder audio_encoder =  " + audio_encoder);
        if (mIsUseMediaCodec) {
            throw new IllegalArgumentException("setAudioEncoder(int audio_encoder) " +
                    "must be use MediaRecorder recording!");
        }
        mMediaRecorder.setAudioEncoder(audio_encoder);
    }

    public void setAudioSource(int audioSource) {
        Log.i(TAG, "Initialize >>> setAudioSource audioSource =  " + audioSource);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setAudioSource(audioSource);
        } else {
            mMediaRecorder.setAudioSource(audioSource);
        }
    }

    public void setAudioEncodingBitRate(int bitRate) {
        Log.i(TAG, "Initialize >>> setAudioEncodingBitRate bitRate =  " + bitRate);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setAudioEncodingBitRate(bitRate);
        } else {
            mMediaRecorder.setAudioEncodingBitRate(bitRate);
        }
    }

    public void setAudioChannels(int numChannels) {
        Log.i(TAG, "Initialize >>> setAudioChannels numChannels =  " + numChannels);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setAudioChannels(numChannels);
        } else {
            mMediaRecorder.setAudioChannels(numChannels);
        }
    }

    public void setAudioSamplingRate(int samplingRate) {
        Log.i(TAG, "Initialize >>> setAudioSamplingRate samplingRate =  " + samplingRate);
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setAudioSamplingRate(samplingRate);
        } else {
            mMediaRecorder.setAudioSamplingRate(samplingRate);
        }
    }

    public void setParametersExtra() {
        if (!mIsUseMediaCodec) {
            setMediaRecorderParameters(mMediaRecorder);
        }
    }

    /***************************************life cycle part **************************************/
    /**
     *  @throws IOException
     *  @throws IllegalStateException
     */
    public void prepare() throws IllegalStateException, IOException {
        Log.i(TAG, "prepare begin");
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.prepare();
        } else {
            mMediaRecorder.prepare();
        }
        Log.i(TAG, "prepare end");
    }

    public Surface getSurface() {
        if (mIsUseMediaCodec) {
            return mMediaCodecRecorder.getSurface();
        } else {
            return mMediaRecorder.getSurface();
        }
    }

    public void start() {
        Log.i(TAG, "start begin");
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.start();
        } else {
            mMediaRecorder.start();
        }
        Log.i(TAG, "start end");
    }

    /**
      * Pauses the recording.
      * Call this method after MediaRecorderWrapper.start().
      * In addition, call MediaRecorderWrapper.
      * start() to resume the recorder after this method is called.
      *
      * @param recorder Recorder used to record video
      * @throws IllegalStateException If it is not called after MediaRecorder.start()
      */
     public void pause(MediaRecorderWrapper recorder) throws IllegalStateException {
         Log.i(TAG, "pause begin");
         if (recorder == null) {
             Log.e(TAG, "Null MediaRecorderWrapper!");
             return;
         }
         if (mIsUseMediaCodec) {
             mMediaCodecRecorder.pause();
         } else {
             MediaRecorderEx.pause(mMediaRecorder);
         }
         Log.i(TAG, "pause end");
     }

    public void resume(MediaRecorderWrapper recorder) {
        Log.i(TAG, "resume begin");
        if (recorder == null) {
            Log.e(TAG, "[resume]Null MediaRecorderWrapper!");
            return;
        }
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.resume();
        } else {
            mMediaRecorder.start();
        }
        Log.i(TAG, "resume end");
    }

    public void stop() {
        Log.i(TAG, "stop begin");
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.stop();
        } else {
            mMediaRecorder.stop();
        }
        Log.i(TAG, "stop end");
    }

    public void release() {
        Log.i(TAG, "release begin");
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.release();
        } else {
            mMediaRecorder.release();
        }
        Log.i(TAG, "release end");
    }

    /***************************************listener part** **************************************/
    /**
     * Register a callback to be invoked when an informational event occurs while
     * recording.
     *
     * @param listener the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener) {
        Log.i(TAG, "setOnInfoListener listener = " + listener);
        mOnInfoListener = listener;
        if (mIsUseMediaCodec) {
            mMediaCodecRecorder.setOnInfoListener(this);
        } else {
            mMediaRecorder.setOnInfoListener(this);
        }
    }

    private void setMediaRecorderParameters(MediaRecorder mediaRecorder) {
        mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                + MEDIA_RECORDER_INFO_BITRATE_ADJUSTED);
        mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                + MEDIA_RECORDER_INFO_FPS_ADJUSTED);
        mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                + MEDIA_RECORDER_INFO_START_TIMER);
        mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                + MEDIA_RECORDER_INFO_WRITE_SLOW);
        mediaRecorder.setParametersExtra(RECORDER_INFO_SUFFIX
                + MEDIA_RECORDER_INFO_CAMERA_RELEASE);
    }
}
