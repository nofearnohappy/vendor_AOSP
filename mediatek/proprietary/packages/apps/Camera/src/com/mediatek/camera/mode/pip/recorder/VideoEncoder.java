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
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class VideoEncoder extends Encoder {
    private static final String TAG = "VideoEncoder";
    
    private static final String MIME_TYPE = "video/avc"; // H.264 AVC encoding
    private static final int IFRAME_INTERVAL = 1; // 5 seconds between I-frames
    
    private MediaCodec mMediaCodec;
    private Surface mSurface;
    private MediaFormat mMediaFormat;
    private MediaCodec.BufferInfo mBufferInfo;
    
    private DrainVideoBufferListener mVideoBufferListener;
    private Thread mRecordingThread;
    
    public interface DrainVideoBufferListener {
        long drainVideoBuffer(MediaCodec.BufferInfo bufferInfo, boolean endOfStream);
    }
    
    public void setDrainVideoBufferListener(DrainVideoBufferListener listener) {
        mVideoBufferListener = listener;
    }
    
    public VideoEncoder() {
        
    }
    
    public Surface getInputSurface() {
        Log.i(TAG, "getInputSurface");
        return mSurface;
    }
    
    @Override
    public MediaCodec getMediaCodec() {
        return mMediaCodec;
    }
    
    public void prepareVideoEncoder(String mimeType, int frameRate, int bitRate,
            int videoFrameWidth, int vieoFrameHeight) {
        Log.i(TAG, "prepareVideoEncorder " + " mimeType = " + mimeType + " frameRate = "
                + frameRate + " bitRate = " + bitRate + " videoFrameWidth = " + videoFrameWidth
                + " vieoFrameHeight = " + vieoFrameHeight);
        checkMimeTypeSupported(mimeType);
        if (mMediaCodec != null || getEncodeState() != ENCODE_STATE_IDLE) {
            throw new RuntimeException("prepareVideoEncorder called twice?");
        }
        // info
        mBufferInfo = new MediaCodec.BufferInfo();
        // format
        mMediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, videoFrameWidth, vieoFrameHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        // codec
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch(IOException e) {
            Log.d(TAG,"[prepareVideoEncoder] createEncoderByType exception");
        }
        
        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        setEncodeState(ENCODE_STATE_CONFIGURED);
        mSurface = mMediaCodec.createInputSurface();
        Log.i(TAG, "prepareVideoEncoder end");
    }

    public void startVideoRecording() {
        Log.i(TAG, "startVideoRecording");
        if (mMediaCodec != null) {
            try {
                mMediaCodec.start();
            } catch (IllegalStateException e) {
                releaseVideoEncoder();
                throw e;
            }
        }
        setEncodeState(ENCODE_STATE_RECORDING);
        /**
         * Start video recording asynchronously. we need a loop to handle output
         * data for each frame.
         */
        mRecordingThread = new Thread("VideoEncoder") {
            @Override
            public void run() {
                Log.v(TAG, "Recording thread starts");
                while (getEncodeState() == ENCODE_STATE_RECORDING
                        || getEncodeState() == ENCODE_STATE_PAUSED) {
                    // Feed encoder output into the MediaMuxer until recording
                    // stops or pauses.
                    if (mVideoBufferListener != null) {
                       long framePts =  mVideoBufferListener.drainVideoBuffer(mBufferInfo, false);
                       if (framePts != 0l && (framePts >= getLatestPresentationTimeUs())) {
                           setLatestPresentationTimeUs(framePts);
                       }
                    }
                }
                Log.v(TAG, "Video Recording thread completes");
                return;
            }
        };
        mRecordingThread.start();
        Log.i(TAG, "startVideoRecording end");
    }

    public void pauseVideoRecording() {
        Log.i(TAG, "pauseVideoRecording");
        setEncodeState(ENCODE_STATE_PAUSED);
        Log.i(TAG, "pauseVideoRecording end");
    }

    public void dropVideoFrame(long presentationTimeUs) {
        Log.d(TAG, "dropVideoFrame presentationTimeUs = " + presentationTimeUs
                + " getEncodeState = " + getEncodeState());
        if (getEncodeState() == ENCODE_STATE_PAUSED) {
            setPausedDurationUs(presentationTimeUs - getLatestPresentationTimeUs());
        }
    }
    
    public void resumeVideoRecording() {
        Log.i(TAG, "resumeVideoRecording");
        setEncodeState(ENCODE_STATE_RECORDING);
        Log.i(TAG, "resumeVideoRecording end");
    }
    
    public void stopVideoRecording() {
        Log.i(TAG, "stopVideoRecording");
        if (getEncodeState() != ENCODE_STATE_RECORDING && getEncodeState() != ENCODE_STATE_PAUSED) {
            Log.w(TAG, "Recording stream is not started yet");
            return;
        }
        setEncodeState(ENCODE_STATE_IDLE);
        // Wait until recording thread stop
        try {
            mRecordingThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Stop recording failed", e);
        }
        if (mVideoBufferListener != null) {
            mVideoBufferListener.drainVideoBuffer(mBufferInfo, true);
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
        }
        Log.i(TAG, "stopVideoRecording end");
    }
    
    public void releaseVideoEncoder() {
        Log.i(TAG, "releaseVideoEncoder");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mMediaCodec != null) {
            mMediaCodec.release();
            mMediaCodec = null;
        }
        mVideoBufferListener = null;
        Log.i(TAG, "releaseVideoEncoder end");
    }
    
    private void checkMimeTypeSupported(String mimeType) {
        Log.i(TAG, "checkMimeTypeSupported mimeType = " + mimeType);
        int codec_num = MediaCodecList.getCodecCount();
        String mime = null;
        for (int i = 0; i < codec_num; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (info.isEncoder()) {
                Log.d(TAG, "Codec: " + info.getName());
                String[] mimes = info.getSupportedTypes();
                for (String m : mimes) {
                    Log.i(TAG, "MIME: " + m);
                    if (mimeType.equals(m)) {
                        mime = m;
                    }
                }
            }
        }
        if (mime == null) {
            throw new UnsupportedOperationException(String.format("Not support MIME: %s", mimeType));
        }
    }
}
