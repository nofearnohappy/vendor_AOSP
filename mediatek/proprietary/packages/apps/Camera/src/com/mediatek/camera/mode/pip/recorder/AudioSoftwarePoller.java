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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;

/*
 * This class polls audio from the microphone and feeds an
 * AudioEncoder. Audio buffers are recycled between this class and the AudioEncoder
 *
 * Usage:
 *
 * 1. AudioSoftwarePoller recorder = new AudioSoftwarePoller();
 * 1a (optional): recorder.setSamplesPerFrame(NUM_SAMPLES_PER_CODEC_FRAME)
 * 2. recorder.setAudioEncoder(myAudioEncoder)
 * 2. recorder.startPolling();
 * 3. recorder.stopPolling();
 */
public class AudioSoftwarePoller {
    private static final String TAG = "AudioSoftwarePoller";
    
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int FRAMES_PER_BUFFER = 24; // 1 sec @ 1024
                                                     // samples/frame (aac)
    private static long US_PER_FRAME = 0;
    private static boolean mIsRecording = false;
    private RecorderTask recorderTask = new RecorderTask();
    private AudioEncoder mAudioEncoder;
    private int mSampleRate = SAMPLE_RATE;
    
    public AudioSoftwarePoller(AudioEncoder avcEncoder) {
        mAudioEncoder = avcEncoder;
    }
    
    /**
     * Set the number of samples per frame (Default is 1024). Call this before
     * startPolling(). The output of emptyBuffer() will be equal to, or a
     * multiple of, this value.
     * 
     * @param samples_per_frame
     *            The desired audio frame size in samples.
     */
    public void setSamplesPerFrame(int samples_per_frame) {
        if (!mIsRecording)
            recorderTask.samples_per_frame = samples_per_frame;
    }
    
    /**
     * Return the number of microseconds represented by each audio frame
     * calculated with the sampling rate and samples per frame
     */
    public long getMicroSecondsPerFrame() {
        if (US_PER_FRAME == 0) {
            US_PER_FRAME = (mSampleRate / recorderTask.samples_per_frame) * 1000000;
        }
        return US_PER_FRAME;
    }
    
    public void recycleInputBuffer(byte[] buffer) {
        if (mIsRecording) {
            recorderTask.data_buffer.offer(buffer);
        }
    }
    
    /**
     * Begin polling audio and transferring it to the buffer. Call this before
     * emptyBuffer().
     */
    public void startPolling(int sampleRate) {
        if (sampleRate > 0) {
            mSampleRate = sampleRate;
        }
        new Thread(recorderTask).start();
    }
    
    /**
     * Stop polling audio.
     */
    public void stopPolling() {
        mIsRecording = false; // will stop recording after next sample received
    }
    
    public class RecorderTask implements Runnable {
        private int buffer_size;
        private int samples_per_frame = 2048 * 2; // codec-specific
        private int buffer_write_index = 0; // last buffer index written to
        private ArrayBlockingQueue<byte[]> data_buffer = new ArrayBlockingQueue<byte[]>(50);
        private int read_result = 0;
        private long mSamplesPerFrameMs = 0l;
        
        public void run() {
            int min_buffer_size = AudioRecord.getMinBufferSize(mSampleRate, CHANNEL_CONFIG,
                    AUDIO_FORMAT);
            buffer_size = samples_per_frame * FRAMES_PER_BUFFER;
            // Ensure buffer is adequately sized for the AudioRecord object to
            // initialize
            if (buffer_size < min_buffer_size)
                buffer_size = ((min_buffer_size / samples_per_frame) + 1) * samples_per_frame * 2;
            // data_buffer = new byte[samples_per_frame]; // filled directly by
            // hardware
            for (int x = 0; x < 25; x++) {
                data_buffer.add(new byte[samples_per_frame]);
            }
            AudioRecord audioRecord;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, // source
                    mSampleRate, // sample rate, hz
                    CHANNEL_CONFIG, // channels
                    AUDIO_FORMAT, // audio format
                    buffer_size); // buffer size (bytes)
            audioRecord.startRecording();
            mIsRecording = true;
            Log.i(TAG, "SW recording begin mSamplesPerFrameMs = " + mSamplesPerFrameMs);
            long audioPresentationTimeNs = 0l;
            while (mIsRecording) {
                byte[] this_buffer;
                if (data_buffer.isEmpty()) {
                    this_buffer = new byte[samples_per_frame];
                    Log.w(TAG, "Audio buffer empty. added new buffer");
                } else {
                    this_buffer = data_buffer.poll();
                }
                read_result = audioRecord.read(this_buffer, 0, samples_per_frame);
                // per frame consume time = buffer size / (channelcount *
                // byte/bit) / samplerate * 1000000000
                audioPresentationTimeNs += ((long) (((read_result / (1 * 2)) / (float) mSampleRate) * 1000000000));
                if (read_result == AudioRecord.ERROR_BAD_VALUE
                        || read_result == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Read error read_result = " + read_result);
                }
                if (mAudioEncoder != null && read_result > 0) {
                    mAudioEncoder.offerAudioEncoder(this_buffer, audioPresentationTimeNs);
                }
            }
            if (audioRecord != null) {
                audioRecord.setRecordPositionUpdateListener(null);
                audioRecord.release();
                audioRecord = null;
                data_buffer.clear();
                Log.i(TAG, "stopped audio record");
            }
        }
    }
    
}