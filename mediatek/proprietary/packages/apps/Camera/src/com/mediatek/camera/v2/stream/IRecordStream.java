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

package com.mediatek.camera.v2.stream;

import java.io.FileDescriptor;
import java.util.List;
import android.media.CamcorderProfile;
import android.view.Surface;

/**
 *
 */
public interface IRecordStream {
    public static final int MEDIA_RECORDER_INFO_MAX_DURATION_REACHED = 800;
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED = 801;
    public static final int MEDIA_RECORDER_INFO_RECORDING_SIZE = 895;
    public static final int MEDIA_RECORDER_INFO_FPS_ADJUSTED = 897;
    public static final int MEDIA_RECORDER_INFO_BITRATE_ADJUSTED = 898;
    public static final int MEDIA_RECORDER_INFO_WRITE_SLOW = 899;
    public static final int MEDIA_RECORDER_INFO_START_TIMER = 1998;
    public static final int MEDIA_RECORDER_ENCODER_ERROR = -1103;
    public static final int MEDIA_RECORDER_INFO_CAMERA_RELEASE = 1999;

    public static final String RECORDER_INFO_SUFFIX = "media-recorder-info=";


    public final class HDRecordMode {
        private HDRecordMode() {
        }

        public static final int UNSUPPORT = -1;
        /** Normal mode */
        public static final int NORMAL = 0;
        /** Indoor mode */
        public static final int INDOOR = 1;
        /** Outdoor mode */
        public static final int OUTDOOR = 2;
    }

    public interface RecordStreamStatus {
        public void onRecordingStarted();
        public void onRecordingStoped(boolean video_saved);
        public void onInfo(int what, int extra);
        public void onError(int what, int extra);
    }

    // status observer
    public void registerRecordingObserver(RecordStreamStatus status);
    public void unregisterCaptureObserver(RecordStreamStatus status);
    // common part
    public void setRecordingProfile(CamcorderProfile profile);
    /**
     * @param @param max_duration_ms the maximum duration in ms
     * (if zero or negative, disables the duration limit)
     */
    public void setMaxDuration(int max_duration_ms);
    public void setMaxFileSize(long max_filesize_bytes);
    public void setOutputFile(String path);
    public void setOutputFile(FileDescriptor fd);
    public void setCaptureRate(double fps);
    public void setLocation(float latitude, float longitude);
    public void setOrientationHint(int degrees);
    public void setMediaRecorderParameters(List<String> paramters);
    // audio part
    public void enalbeAudioRecording(boolean enable_audio);
    public void setAudioSource(int audio_source);
    /**
     * Sets up the HD record mode to be used for recording.
     * @param mode mode HD record mode to be used
     */
    public void setHDRecordMode(String mode);
    // video part
    public void setVideoSource(int video_source);
    // life cycle
    public void prepareRecord();
    public void startRecord();
    public void pauseRecord();
    public void resumeRecord();
    /**
     *
     * @param need_save_video, true will save the video, false will delete the video.
     */
    public void stopRecord(boolean need_save_video);
    /**
     * Delete the video file, file path is defined by {@link #setOutputFile(String)}.
     * @return true for delete success, false for delete fail.
     */
    public boolean deleteVideoFile();
    // input surface
    public Surface getRecordInputSurface();
    public void releaseRecordStream();
}
