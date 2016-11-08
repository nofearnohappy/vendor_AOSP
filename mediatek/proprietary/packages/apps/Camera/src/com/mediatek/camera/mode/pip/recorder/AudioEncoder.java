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
import android.media.MediaFormat;
import android.os.ConditionVariable;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioEncoder extends Encoder {
    private static final String TAG = "AudioEncoder";

    private static final String       AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private boolean                   mEosSentToAudioEncoder = false;
    private boolean                   mStopCommandReceived = false;
    private boolean                   mEosAudioArrived = false;
    private int                       mTotalInputAudioFrameCount = 0; // testing
    private int                       mTotalOutputAudioFrameCount = 0;
    private int                       mEncodingServiceQueueLength = 0;
    private MediaFormat               mAudioFormat;
    private MediaCodec                mMediaCodec;
    private MediaCodec.BufferInfo     mAudioBufferInfo;
    // re-use mEncodingService
    private ExecutorService           mEncodingService = Executors.newSingleThreadExecutor();
    private AudioSoftwarePoller       mAudioSoftwarePoller;
    private boolean                   mIsVideoStarted = false;
    private boolean                   mIsAudioStarted = false;
    private long                      mVideoStartTime;
    private long                      mAVSyncDuration = 0L;
    private long                      mSendToEncoderLastTimeStampUs = 0L;
    private DrainAudioBufferListener  mAudioBufferListener;

    private String                    mAudioMIMEType;
    private int                       mAudioSampleRate;
    private int                       mAudioBitRate;
    private int                       mAudioChannelCount;
    private ConditionVariable         mStopConditionVariable = new ConditionVariable();
    private ConditionVariable         mEOSConditionVariable = new ConditionVariable();
    // input buffer container, get from media codec, only get once
    private ByteBuffer[]              mInputBuffers = null;

    public AudioEncoder() {
        mAudioSoftwarePoller = new AudioSoftwarePoller(this);
    }

    public void notifyFirstVideoFrameReady(long videoPts) {
        Log.v(TAG, "notifyFirstVideoFrameReady videoPts = " + videoPts);
        if (!mIsVideoStarted && mAudioSoftwarePoller != null) {
            mVideoStartTime = videoPts;
            mIsVideoStarted = true;
        }
    }

    @Override
    public MediaCodec getMediaCodec() {
        return mMediaCodec;
    }

    public interface DrainAudioBufferListener {
        long drainAudioBuffer(MediaCodec.BufferInfo bufferInfo, boolean endOfStream);
    }

    public void setDrainAudioBufferListener(DrainAudioBufferListener listener) {
        mAudioBufferListener = listener;
    }

    public void prepareAudioEncoder(String mimeType, int audioSource, int sampleRate,
            int channelCount, int bitRate) {
        Log.i(TAG, "prepareAudioEncoder :" +
                " mimeType = " + mimeType +
                " audioSource = " + audioSource +
                " sampleRate = " + sampleRate +
                " channelCount = " + channelCount +
                " bitRate = " + bitRate);
        mEosSentToAudioEncoder = false;
        mStopCommandReceived = false;
        mEosAudioArrived = false;
        mAudioBufferInfo = new MediaCodec.BufferInfo();
        mAudioFormat = new MediaFormat();

        mAudioMIMEType = (mimeType == null) ? AUDIO_MIME_TYPE : mimeType;
        mAudioSampleRate = sampleRate;
        mAudioBitRate = bitRate;
        mAudioChannelCount = channelCount;

        mAudioFormat.setString(MediaFormat.KEY_MIME, mAudioMIMEType);
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mAudioSampleRate);
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mAudioChannelCount);
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mAudioBitRate);
        mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        try {
            mMediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            Log.d(TAG, "[prepareAudioEncoder] createEncoderByType exception");
        }
        mMediaCodec.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void startAudioRecording() {
        Log.i(TAG, "startAudioRecording");
        mMediaCodec.start();
        mAudioSoftwarePoller.startPolling(mAudioSampleRate);
        setEncodeState(ENCODE_STATE_RECORDING);
        Thread mRecordingThread = new Thread("AudioEncoder") {
            @Override
            public void run() {
                while (!mEosSentToAudioEncoder) {
                    // Feed encoder output into the MediaMuxer until recording stops or pauses.
                    if (mAudioBufferListener != null) {
                        mAudioBufferListener.drainAudioBuffer(mAudioBufferInfo, false);
                    }
                }
                Log.i(TAG, "AudioEncoder thread complete");
                //when eos arrived, should wait previous buffer handled,
                //and then send EOS to media codec
                mEOSConditionVariable.open();
                return;
            }
        };
        mRecordingThread.start();
    }

    public void stopAudioRecording() {
        Log.i(TAG, "stopAudioRecording");
        // resume state to allow receive audio buffer
        setEncodeState(ENCODE_STATE_RECORDING);
        stop();
        // wait audio really stop
        mStopConditionVariable.block();
        // stop audio poller
        setEncodeState(ENCODE_STATE_STOPPED);
        mAudioSoftwarePoller.stopPolling();
        Log.i(TAG, "AudioEncoder stopAudioRecording end");
    }

    public void pauseAudioRecording() {
        Log.i(TAG, "pauseAudioRecording");
        setEncodeState(ENCODE_STATE_PAUSED);
    }

    public void resumeAudioRecording() {
        Log.i(TAG, "resumeAudioRecording");
        setEncodeState(ENCODE_STATE_RECORDING);
    }

    private void stop() {
        if (!mEncodingService.isShutdown())
            mEncodingService.submit(new EncoderTask(this, EncoderTaskType.FINALIZE_ENCODER));
            mStopCommandReceived = true;
    }

    /**
     * Called from mEncodingService
     */
    public void _stop() {
        Log.i(TAG, "_stop");
        mEosAudioArrived = true;
        logStatistics();
    }

    public void closeEncoder(MediaCodec.BufferInfo bufferInfo) {
        Log.i(TAG, "closeEncoder mMediaCodec = " +
                mMediaCodec + "mEncodingService status = " +
                mEncodingService.isShutdown());
        if (mMediaCodec != null) {
            mAudioBufferListener.drainAudioBuffer(bufferInfo, true);
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (IllegalStateException e) {
                Log.i(TAG, " closeEncoder " + e);
                e.printStackTrace();
            }
        }
    }

    public void offerAudioEncoder(byte[] input, long presentationTimeStampNs) {
        Log.v(TAG, "offerAudioEncoder getEncodeState = " + getEncodeState() + " mIsVideoStarted = "
                + mIsVideoStarted);
        int encodeState = getEncodeState();
        if (!mIsVideoStarted || (encodeState == ENCODE_STATE_PAUSED)
                || (encodeState == ENCODE_STATE_STOPPED) || input == null) {
            // in paused state, should keep paused duration
            if (encodeState == ENCODE_STATE_PAUSED) {
                setPausedDurationUs((presentationTimeStampNs - mAVSyncDuration) / 1000
                        - getLatestPresentationTimeUs());
            }
            if (mAudioSoftwarePoller != null) {
                mAudioSoftwarePoller.recycleInputBuffer(input);
            }
            Log.i(TAG, "offerAudioEncoder getEncodeState = " + getEncodeState()
                    + " mIsVideoStarted = " + mIsVideoStarted + " input = " + input);
            return;
        }
        if (!mIsAudioStarted) {
            mIsAudioStarted = true;
            mAVSyncDuration = presentationTimeStampNs - mVideoStartTime;
        }
        presentationTimeStampNs = presentationTimeStampNs - mAVSyncDuration - getPausedDurationUs()
                * 1000;
        if ((presentationTimeStampNs / 1000) <= getLatestPresentationTimeUs()) {
            // drop invalid time stamp audio frame
            Log.i(TAG, "presentationTimeStampNs = " + presentationTimeStampNs
                    + " mAVSyncDuration = " + mAVSyncDuration + " getPausedDurationUs = "
                    + getPausedDurationUs() + " getLatestPresentationTimeUs = "
                    + getLatestPresentationTimeUs() + " mEncodingServiceQueueLength = "
                    + mEncodingServiceQueueLength);
            if (mAudioSoftwarePoller != null) {
                mAudioSoftwarePoller.recycleInputBuffer(input);
            }
            return;
        }
        if (!mEncodingService.isShutdown()) {
            mEncodingServiceQueueLength++;
            if (mStopCommandReceived) {
                mStopConditionVariable.open();
            }
            mEncodingService.submit(new EncoderTask(this, input, presentationTimeStampNs));
            setLatestPresentationTimeUs(presentationTimeStampNs > 0 ? presentationTimeStampNs / 1000
                    : getLatestPresentationTimeUs());
        }
    }

    private void _offerAudioEncoder(byte[] input, long presentationTimeNs) {
        Log.v(TAG, "_offerAudioEncoder  mEosSentToAudioEncoder = " + mEosSentToAudioEncoder
                + " mStopCommandReceived = " + mStopCommandReceived +
                " mEosAudioArrived = " + mEosAudioArrived + " input = " + input);
        mTotalInputAudioFrameCount++;
        if (mEosSentToAudioEncoder && mEosAudioArrived || input == null) {
            return;
        }
        try {
            Log.v(TAG, "getInputBuffers");
            if (mInputBuffers == null) {
                mInputBuffers = mMediaCodec.getInputBuffers();
            }
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mInputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                if (mAudioSoftwarePoller != null) {
                    mAudioSoftwarePoller.recycleInputBuffer(input);
                }
                long presentationTimeUs = (presentationTimeNs) / 1000;
                if (mEosAudioArrived) {
                    mEosSentToAudioEncoder = true;
                    mEOSConditionVariable.block();
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    closeEncoder(mAudioBufferInfo);
                    mEncodingService.shutdownNow();
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,
                            presentationTimeUs, 0);
                }
            }
        } catch (Throwable t) {
            Log.i(TAG, "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

    private void logStatistics() {
        Log.v(TAG + "-Stats", "audio frames input: " + mTotalInputAudioFrameCount + " output: "
                + mTotalOutputAudioFrameCount);
    }

    enum EncoderTaskType {
        ENCODE_FRAME, FINALIZE_ENCODER;
    }

    private class EncoderTask implements Runnable {
        private boolean           isInitialized = false;
        private long              presentationTimeNs;
        private AudioEncoder      encoder;
        private EncoderTaskType   type;
        private byte[]            audio_data;

        public EncoderTask(AudioEncoder encoder, EncoderTaskType encoderTaskType) {
            setEncoder(encoder);
            type = encoderTaskType;
            switch (type) {
                case FINALIZE_ENCODER:
                    setFinalizeEncoderParams();
                    break;
            }
        }

        public EncoderTask(AudioEncoder encoder, byte[] audio_data, long pts) {
            setEncoder(encoder);
            setEncodeFrameParams(audio_data, pts);
        }

        private void setEncoder(AudioEncoder encoder) {
            this.encoder = encoder;
        }

        private void setFinalizeEncoderParams() {
            isInitialized = true;
        }

        private void setEncodeFrameParams(byte[] audio_data, long pts) {
            this.audio_data = audio_data;
            this.presentationTimeNs = pts;

            isInitialized = true;
            this.type = EncoderTaskType.ENCODE_FRAME;
        }

        private void encodeFrame() {
            if (encoder == null || audio_data == null
                    || (presentationTimeNs / 1000 <= mSendToEncoderLastTimeStampUs)) {
                Log.i(TAG, " mSendToEncoderLastTimeStampUs = " + mSendToEncoderLastTimeStampUs);
                return;
            }
            mSendToEncoderLastTimeStampUs = presentationTimeNs / 1000;
            encoder._offerAudioEncoder(audio_data, presentationTimeNs);
            audio_data = null;
        }

        private void finalizeEncoder() {
            Log.i(TAG, "finalizeEncoder");
            encoder._stop();
        }

        @Override
        public void run() {
            if (Thread.MAX_PRIORITY != Thread.currentThread().getPriority()) {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            }
            if (isInitialized) {
                switch (type) {
                    case ENCODE_FRAME:
                        encodeFrame();
                        break;
                    case FINALIZE_ENCODER:
                        finalizeEncoder();
                        break;
                }
                // prevent multiple execution of same task
                isInitialized = false;
                mEncodingServiceQueueLength -= 1;
                Log.v(TAG, "mEncodingService Queue length: " + mEncodingServiceQueueLength);
            } else {
                Log.e(TAG, "run() called but EncoderTask not initialized");
            }
        }
    }
}
