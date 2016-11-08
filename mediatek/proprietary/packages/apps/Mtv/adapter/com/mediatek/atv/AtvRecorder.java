/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.atv;

import java.io.IOException;
import android.view.Surface;

import com.mediatek.mtvbase.Recorder;
import android.media.MediaRecorder;
import android.util.Log;

public class AtvRecorder implements Recorder, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    private static final String TAG = "ATV/AtvRecorder";
    private AtvService mService;
    private int mState = MTV_RECORDER_IDLE;
    private MediaRecorder mMediaRecorder;
    private static AtvService.EventCallback mEventCallback;
    public AtvRecorder(AtvService.EventCallback e, AtvService s) {
        mService = s;
        mEventCallback = e;
        mMediaRecorder = new MediaRecorder();
    }

    /*Releases resources associated with this MtvRecorder object.*/
    public void  release() {
        Log.d("@M_" + TAG, "release mState = " + mState);
        if (mState == MTV_RECORDER_RELEASED) {
            return;
        } else if (mState != MTV_RECORDER_IDLE) {
            mMediaRecorder.reset();
        }

        mMediaRecorder.release();
        mService.lock();
        try {
            mService.reconnect();
        } catch (IOException e) {
            e.printStackTrace();
            //ignore
        }
        mState = MTV_RECORDER_RELEASED;
        mMediaRecorder = null;
    }

    /*Starts or resumes playback.*/
    public void  start(int output_format, String path, long maxFileSize
                , Surface s, int encoder, int width, int height) throws IllegalStateException, IOException {
        Log.d("@M_" + TAG, "start mState = " + mState);
        if (mState == MTV_RECORDER_IDLE) {
            mService.unlock();
            mService.setRecorder(mMediaRecorder);

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MATV);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            mMediaRecorder.setOutputFormat(output_format);
            mMediaRecorder.setOutputFile(path);
            try {
                mMediaRecorder.setMaxFileSize(maxFileSize);
            } catch (IllegalArgumentException exception) {
                // We are going to ignore failure of setMaxFileSize here, as
                // a) The composer selected may simply not support it, or
                // b) The underlying media framework may not handle 64-bit range
                // on the size restriction.
                Log.d("@M_" + TAG, "start() - setMaxFileSize() faile :" + exception);
            }

            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncoder(encoder);
            mMediaRecorder.setPreviewDisplay(s);
            mMediaRecorder.setVideoSize(width, height);
            mMediaRecorder.setVideoEncodingBitRate(500 * 1024);
            mMediaRecorder.prepare();

            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.setOnInfoListener(this);
            mMediaRecorder.start();
            mState = MTV_RECORDER_RECORDING;
        } else {
            release();
        }
    }

    /*Stops playback after playback has been stopped or paused. */
    public void  stop() {
        Log.d("@M_" + TAG, "stop mState = " + mState);
        if (mState == MTV_RECORDER_RECORDING) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setOnInfoListener(null);
            mMediaRecorder.stop();
            mState = MTV_RECORDER_IDLE;
        } else {
            release();
        }
    }

    /*   the MtvRecorder that encountered the error
     * what  the type of error that has occurred:
        MTV_ERROR_UNKNOWN
     * extra  an extra code, specific to the error type
    */
    public void onError(MediaRecorder mr, int what, int extra) {
        if (mState != MTV_RECORDER_IDLE) {
            release();
            mEventCallback.callOnEvent(what | 0xf0004000, extra, 0, mr);
        } else {
            throw new IllegalStateException();
        }
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        mEventCallback.callOnEvent(what | 0xf0004000, extra, 0, mr);
    }

}
