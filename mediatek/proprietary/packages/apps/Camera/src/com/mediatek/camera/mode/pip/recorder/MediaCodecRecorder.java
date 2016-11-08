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

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.ConditionVariable;
import android.util.Log;
import android.view.Surface;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecRecorder implements AudioEncoder.DrainAudioBufferListener,
        VideoEncoder.DrainVideoBufferListener {
    private static final String                 TAG = "MediaCodecRecorder";
    
    private int                                 mTotalTrackNum = 0;
    private int                                 mNumTrackAdded = 0;
    private static Object                       sSyncChangeNumEOS = new Object();
    private int                                 mNumEOSNum = 0;
    private boolean                             mMuxerStarted = false;
    private Long                                mTotalSize    = 0l; 
    private boolean                             mMaxSizedReached = false;
    private boolean                             mIsFirstVideoFrameArrived = false;
    // common
    private VideoEncoder mVideoEncoder;
    private AudioEncoder mAudioEncoder;
    private Surface mVideoInputSurface;
    private ConditionVariable mStopConditionVariable = new ConditionVariable();
    private boolean mIsFirstVideoFrameReady = false;
    private OnInfoListener mOnInfoListener;
    
    private int mFileFormat;
    private Long                                mMaxFileSize;
    private String mOutputFilePath;
    private int mRecordingOrientation;
    private long mLatitude = 0l;
    private long mLongitude = 0l;
    private int mVideoBufferCount = 0;
    // video
    private TrackInfo mVideoTrackInfo;
    private int mVideoFrameRate;
    private int mVideoFrameWidth;
    private int mVideoFrameHeight;
    private int mVideoBitRate;
    // audio
    private TrackInfo mAudioTrackInfo;
    private int mAudioBitRate;
    private int mAudioChannels;
    private int mAudioSamplingRate;
    private int mAudioSource;
    
    private MediaMuxer mMediaMuxer = null;
    
    private class TrackInfo {
        private int index = -1;
    }
    
    public MediaCodecRecorder() {
    }
    
    public AudioEncoder getAudioEncoder() {
        return mAudioEncoder;
    }
    
    public VideoEncoder getVideoEncoder() {
        return mVideoEncoder;
    }
    
    // common
    // current media muxer only support MPEG-4
    public void setOutputFormat(int fileFormat) {
        Log.i(TAG, "Initialize >>> setOutputFormat = " + fileFormat);
        mFileFormat = fileFormat;
    }
    
    public void setMaxFileSize(Long maxFileSize) {
        Log.i(TAG, "Initialize >>> setMaxFileSize = " + maxFileSize);
        mMaxFileSize = maxFileSize;
    }
    
    public void setOutputFile(String path) {
        Log.i(TAG, "Initialize >>> setOutputFile = " + path);
        mOutputFilePath = path;
    }
    
    public void setOrientationHint(int degrees) {
        Log.i(TAG, "Initialize >>> setOrientationHint = " + degrees);
        mRecordingOrientation = degrees;
    }
    
    public void setLocation(long latitude, long longitude) {
        Log.i(TAG, "Initialize >>> setLocation latitude = " + latitude + " , longitude = " + longitude);
        mLatitude = latitude;
        mLongitude = longitude;
    }
    
    // video
    public void setVideoEncoder(VideoEncoder videoEncorder) {
        Log.i(TAG, "Initialize >>> setVideoEncoder = " + videoEncorder);
        mVideoEncoder = videoEncorder;
        mVideoEncoder.setDrainVideoBufferListener(this);
        mVideoTrackInfo = new TrackInfo();
    }

    public Surface getSurface() {
        Log.i(TAG, "getSurface surface = " + mVideoInputSurface);
        return mVideoInputSurface;
    }

    public void notifyFirstVideoFrameReady(long videoPts) {
        if (mAudioEncoder != null) {
            mAudioEncoder.notifyFirstVideoFrameReady(videoPts);
        }
    }
    
    public void dropVideoFrame(long presentationTimeUs) {
        if (mVideoEncoder != null) {
            mVideoEncoder.dropVideoFrame(presentationTimeUs);
        }
    }
    
    
    public long getVideoPausedDurationUs() {
        if (mVideoEncoder != null) {
            return mVideoEncoder.getPausedDurationUs();
        }
        return 0l;
    }
    
    public void setVideoFrameRate(int videoFrameRate) {
        Log.i(TAG, "Initialize >>> setVideoFrameRate = " + videoFrameRate);
        mVideoFrameRate = videoFrameRate;
    }
    
    public void setVideoSize(int videoFrameWidth, int videoFrameHeight) {
        Log.i(TAG, "Initialize >>> videoFrameWidth = " + videoFrameWidth + " videoFrameHeight = " + videoFrameHeight);
        mVideoFrameWidth = videoFrameWidth;
        mVideoFrameHeight = videoFrameHeight;
    }
    
    public void setVideoEncodingBitRate(int bitRate) {
        Log.i(TAG, "Initialize >>> setVideoEncodingBitRate = " + bitRate);
        mVideoBitRate = bitRate;
    }
    
    // audio
    public void setAudioEncoder(AudioEncoder audioEncorder) {
        Log.i(TAG, "Initialize >>> setAudioEncoder = " + audioEncorder);
        mAudioEncoder = audioEncorder;
        mAudioEncoder.setDrainAudioBufferListener(this);
        mAudioTrackInfo = new TrackInfo();
    }
    
    public void setAudioSource(int audioSource) {
        Log.i(TAG, "Initialize >>> setAudioSource = " + audioSource);
        mAudioSource = audioSource;
    }
    
    public void setAudioEncodingBitRate(int bitRate) {
        Log.i(TAG, "Initialize >>> setAudioEncodingBitRate = " + bitRate);
        mAudioBitRate = bitRate;
    }
    
    public void setAudioChannels(int numChannels) {
        Log.i(TAG, "Initialize >>> setAudioChannels = " + numChannels);
        mAudioChannels = numChannels;
    }
    
    public void setAudioSamplingRate(int samplingRate) {
        Log.i(TAG, "Initialize >>> setAudioSamplingRate = " + samplingRate);
        mAudioSamplingRate = samplingRate;
    }
    
    // life cycle methods
    public void prepare() {
        Log.i(TAG, "prepare begin mRecordingOrientation = " + mRecordingOrientation);
        mNumEOSNum = 0;
        mNumTrackAdded = 0;
        mTotalTrackNum = 0;
        mMaxSizedReached = false;
        try {
            if (mVideoEncoder != null) {
                mVideoEncoder.prepareVideoEncoder("video/avc", mVideoFrameRate, mVideoBitRate,
                        mVideoFrameWidth, mVideoFrameHeight);
                mTotalTrackNum++;
                mVideoInputSurface = mVideoEncoder.getInputSurface();
            }
            if (mAudioEncoder != null) {
                mAudioEncoder.prepareAudioEncoder(null, mAudioSource, mAudioSamplingRate, mAudioChannels, mAudioBitRate);
                mTotalTrackNum++;
            }
            mMediaMuxer = new MediaMuxer(mOutputFilePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMediaMuxer.setOrientationHint(mRecordingOrientation);
            mMuxerStarted = false;
            mMediaMuxer.setLocation(mLatitude, mLongitude);
        } catch (IOException e) {
            Log.e(TAG, "prepare exception e:" + e.toString());
            e.printStackTrace();
            release();
        }
        Log.i(TAG, "prepare end");
    }
    
    /**
     * Interface definition for a callback to be invoked while recording.
     */
    public interface OnInfoListener {
        void onMediaMuxerStarted();
        void onMaxFileSizeReached();
        void onFirstVideoFrameRecorded();
    }

    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public void start() {
        if (mVideoEncoder != null) {
            mIsFirstVideoFrameArrived = false;
            mVideoEncoder.startVideoRecording();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.startAudioRecording();
        }
        // we can't start the MediaMuxer here, because our MediaFormat doesn't
        // have the Magic Goodies,
        // These can only be obtained from the encoder after it has started
        // processing data.
        // mMediaMuxer.start();
    }

    public void pause() {
        Log.i(TAG, "pause");
        if (mAudioEncoder != null) {
            mAudioEncoder.pauseAudioRecording();
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.pauseVideoRecording();
        }
        Log.i(TAG, "pause end");
    }

    public void resume() {
        Log.i(TAG, "resume");
        if (mAudioEncoder != null) {
            mAudioEncoder.resumeAudioRecording();
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.resumeVideoRecording();
        }
        Log.i(TAG, "resume end");
    }
    
    public void stop() {
        Log.i(TAG, "stop begin");
        if (mAudioEncoder != null) {
            mAudioEncoder.stopAudioRecording();
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.stopVideoRecording();
        }
        mStopConditionVariable.block();
        Log.i(TAG, "stop end");
    }
    
    public void release() {
        Log.i(TAG, "release");
        mVideoInputSurface = null;
        if (mVideoEncoder != null) {
            mVideoEncoder.releaseVideoEncoder();
        }
    }
    
    @Override
    public long drainAudioBuffer(BufferInfo bufferInfo, boolean endOfStream) {
        Log.v(TAG, "drainAudioBuffer mAudioEncoder = " + mAudioEncoder);
        if (mAudioEncoder != null) {
            return drainEncoder(mAudioEncoder, bufferInfo, mAudioTrackInfo, endOfStream);
        }
        return 0l;
    }
    
    @Override
    public long drainVideoBuffer(BufferInfo bufferInfo, boolean endOfStream) {
        Log.v(TAG, "drainVideoBuffer");
        if (mVideoEncoder != null) {
            return drainEncoder(mVideoEncoder, bufferInfo, mVideoTrackInfo, endOfStream);
        }
        return 0l;
    }
    
    /**
     * Do encoding by using MediaCodec encoder, then extracts all pending data
     * from the encoder and forwards it to the MediaMuxer.
     * <p>
     * If endOfStream is false, this returns when there is no more data to
     * output. If it is true, we send EOS to the encoder, and then iterate until
     * we see EOS on the output. Calling this with notifyEndOfStream set should
     * be done once, before stopping the MediaMuxer.
     * </p>
     * <p>
     * We're just using the MediaMuxer to get a .mp4 file and audio is not
     * included here.
     * </p>
     */
    private long drainEncoder(Encoder encoder, BufferInfo bufferInfo, TrackInfo trackInfo,
            boolean endOfStream) {
        Log.v(TAG, "drainEncoder begin at thread name : " + Thread.currentThread().getName()
                + " thread id : " + Thread.currentThread().getId() + " endOfStream " + endOfStream);
        long drainPresentationTimeUs = 0l;
        MediaCodec codec = encoder.getMediaCodec();
        if (codec == null) {
            throw new RuntimeException("why codec is null ?");
        }
        // video encoder is started, but muxer is not started, should wait
        if (!mMuxerStarted && (encoder instanceof VideoEncoder) && (trackInfo.index != -1)) {
            return 0l;
        }
        
        if (endOfStream && (encoder instanceof VideoEncoder)) {
            Log.i(TAG, "signal endOfInputStream to Video Encoder");
            codec.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = codec.getOutputBuffers();
        while (true) {
            int encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 0);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    /**
                     * Break out of the while loop because the encoder is not
                     * ready to output anything yet.
                     */
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // generic case for MediaCodec, not likely occurs for encoder.
                encoderOutputBuffers = codec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    /**
                     * should happen before receiving buffers, and should only
                     * happen once
                     */
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = codec.getOutputFormat();
                trackInfo.index = mMediaMuxer.addTrack(newFormat);
                mNumTrackAdded++;
                if (mNumTrackAdded == mTotalTrackNum) {
                    Log.i(TAG, "mMediaMuxer.start();");
                    mMediaMuxer.start();
                    mOnInfoListener.onMediaMuxerStarted();
                    mMuxerStarted = true;
                }
            } else {
                // Normal flow: get output encoded buffer, send to MediaMuxer.
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    /**
                     * The MediaCodec's config data was pulled out and fed to
                     * the MediaMuxer when we got the INFO_OUTPUT_FORMAT_CHANGED
                     * status. Ignore it.
                     */
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        Log.i(TAG, "Muxer is not started! Drop frame: "
                                + ((encoder instanceof VideoEncoder) ? " video" : " audio")
                                + "endOfStream = " + endOfStream + " bufferInfo.flags = "
                                + bufferInfo.flags);
                    } else if ((encoder instanceof VideoEncoder) && (encoder.getEncodeState() == Encoder.ENCODE_STATE_PAUSED)) {
                        dropVideoFrame(bufferInfo.presentationTimeUs);
                        codec.releaseOutputBuffer(encoderStatus, false);
                    } else {
                        if ((encoder instanceof VideoEncoder)) {
                            long presentationTimeUs = bufferInfo.presentationTimeUs - getVideoPausedDurationUs();
                            if (presentationTimeUs <= encoder.getLatestPresentationTimeUs()) {
                                // drop invalid time stamp's video frame
                                Log.i(TAG, "drop invalid time stamp's video frame!!!" 
                                         + " bufferInfo.presentationTimeUs =  " + bufferInfo.presentationTimeUs 
                                         + " getVideoLatestPresentationTimeUs = " + encoder.getLatestPresentationTimeUs());
                                return encoder.getLatestPresentationTimeUs();
                            }
                            bufferInfo.presentationTimeUs = presentationTimeUs;
                        }
                        /**
                         * It's usually necessary to adjust the ByteBuffer
                         * values to match BufferInfo.
                         */
                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        boolean calc_time = false;
                        Log.v(TAG, ((encoder instanceof VideoEncoder) ? " video" : " audio")
                                + " writeSampleData presentation time : "
                                + bufferInfo.presentationTimeUs);
                        drainPresentationTimeUs = bufferInfo.presentationTimeUs;
                        if ((encoder instanceof VideoEncoder) 
                                && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0)
                                && !mIsFirstVideoFrameArrived) {
                            // first video frame arrived
                            notifyFirstVideoFrameReady(bufferInfo.presentationTimeUs * 1000);
                            mIsFirstVideoFrameArrived = true;
                        }
                        if (calc_time) {
                            long t0 = System.currentTimeMillis();
                            mMediaMuxer.writeSampleData(trackInfo.index, encodedData, bufferInfo);
                            mTotalSize += bufferInfo.size;
                            long dt = System.currentTimeMillis() - t0;
                            if (dt > 50)
                                Log.e("DEBUG", String.format("XXX: dt=%d, size=%.2f", dt,
                                        (float) mTotalSize / 1024 / 1024));
                        } else {
                            if (encoder instanceof VideoEncoder) {
                                mVideoBufferCount++;
                            }
                            mMediaMuxer.writeSampleData(trackInfo.index, encodedData, bufferInfo);
                            mTotalSize += bufferInfo.size;
                            synchronized (sSyncChangeNumEOS) {
                                //keep 2M for video header
                                if(((mTotalSize + 2*1024*1024) >= mMaxFileSize) && mOnInfoListener != null && !mMaxSizedReached) {
                                    Log.i(TAG, "PIP Recording Max size reached!");
                                    mMaxSizedReached = true;
                                    mOnInfoListener.onMaxFileSizeReached();
                                }
                            }
                        }
                        codec.releaseOutputBuffer(encoderStatus, false);
                        if ((encoder instanceof VideoEncoder) && !mIsFirstVideoFrameReady) {
                            notifyFirstVideoFrameReady(bufferInfo.presentationTimeUs * 1000);
                            mOnInfoListener.onFirstVideoFrameRecorded();
                            mIsFirstVideoFrameReady = true;
                        }
                    }
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    synchronized (sSyncChangeNumEOS) {
                        if (endOfStream) {
                            mNumEOSNum++;
                            Log.i(TAG, "mNumEOSNum = " + mNumEOSNum + "mTotalTrackNum = "
                                    + mTotalTrackNum + " mMediaMuxer = " + mMediaMuxer);
                            if (mNumEOSNum == mTotalTrackNum && (mMediaMuxer != null)) {
                                mMediaMuxer.stop();
                                mMediaMuxer.release();
                                mAudioEncoder = null;
                                mVideoEncoder = null;
                                mMediaMuxer = null;
                                mMuxerStarted = false;
                                mStopConditionVariable.open();
                                Log.i(TAG,
                                        "mMediaMuxer stop and release !!!!!!!!! muxer video total count = "
                                                + mVideoBufferCount);
                            }
                        }
                    }
                    break;
                }
            }
        }
        Log.v(TAG, "drainEncoder end " + ((encoder instanceof VideoEncoder) ? " video" : " audio")
                + " end drainPresentationTimeUs = " + drainPresentationTimeUs);
        return drainPresentationTimeUs;
    }
}
