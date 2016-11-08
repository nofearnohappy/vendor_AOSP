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

package com.mediatek.camera.v2.mode.normal;

import android.content.Intent;
import android.provider.MediaStore;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import com.android.camera.R;

import com.android.camera.v2.util.Storage;

import com.mediatek.camcorder.CamcorderProfileEx;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.setting.ISettingServant;
import com.mediatek.camera.v2.setting.SettingCtrl;
import com.mediatek.camera.v2.util.SettingKeys;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoHelper {
    private static String TAG = "VideoHelper";

    private static final Long  VIDEO_4G_SIZE = 4 * 1024 * 1024 * 1024L;
    private static final int NOT_FAT_FILE_SYSTEM = 0;
    private static final long INVALID_DURATION = -1L;
    private static final long FILE_ERROR = -2L;

    private boolean mIsCaptureIntent;

    private String mCurrentCameraId;
    private StringBuffer mVideoTempPath = new StringBuffer();

    private Intent mIntent;

    private SettingCtrl mSettingCtroller;
    private ISettingServant mSettingServant;


    public VideoHelper(Intent intent, boolean isCaptureIntent, SettingCtrl settingCtroller) {
        mIntent = intent;
        mIsCaptureIntent = isCaptureIntent;
        mSettingCtroller = settingCtroller;
    }

    public int getRecordingQuality(int cameraId) {
        mSettingServant = mSettingCtroller.getSettingServant(String.valueOf(cameraId));
        int videoQualityValue = Integer.valueOf(
                mSettingServant.getSettingValue(SettingKeys.KEY_VIDEO_QUALITY));
        Intent intent = mIntent;
        boolean userLimitQuality = intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY);
        if (userLimitQuality) {
            int extraVideoQuality = intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            if (extraVideoQuality > 0) {
                if (CamcorderProfile.hasProfile(cameraId, extraVideoQuality)) {
                    videoQualityValue = extraVideoQuality;
                } else {
                    if (CamcorderProfile.hasProfile(cameraId, CamcorderProfileEx.QUALITY_MEDIUM)) {
                        videoQualityValue = CamcorderProfileEx.QUALITY_MEDIUM;
                    } else {
                        videoQualityValue = CamcorderProfileEx.QUALITY_HIGH;
                    }
                }
            } else {
                videoQualityValue = CamcorderProfile.QUALITY_LOW;
            }
        }
        Log.i(TAG, "[getRecordingQuality] videoQualityValue = " + videoQualityValue);
        return videoQualityValue;
    }


    public CamcorderProfile fetchProfile(int quality, int cameraId) {
        Log.i(TAG, "[fetchProfile](" + quality + ", " + " cameraId = "
                + cameraId + ")");
        CamcorderProfile camcorderProfile = CamcorderProfileEx.getProfile(cameraId, quality);
        if (camcorderProfile != null) {
            Log.i(TAG, "[fetchProfile()] mProfile.videoFrameRate="
                    + camcorderProfile.videoFrameRate
                    + ", mProfile.videoFrameWidth="
                    + camcorderProfile.videoFrameWidth
                    + ", mProfile.videoFrameHeight="
                    + camcorderProfile.videoFrameHeight
                    + ", mProfile.audioBitRate="
                    + camcorderProfile.audioBitRate
                    + ", mProfile.videoBitRate="
                    + camcorderProfile.videoBitRate
                    + ", mProfile.quality=" + camcorderProfile.quality
                    + ", mProfile.duration=" + camcorderProfile.duration);
        }
        return camcorderProfile;
    }
    public long getRequestSizeLimit(CamcorderProfile profile,
            boolean needUpdateValue, boolean isCaptureIntent, Intent intent) {
        long size = 0;
        if (isCaptureIntent) {
            size = intent.getLongExtra(MediaStore.EXTRA_SIZE_LIMIT, 0L);
        }
        // M: enlarge recording video size nearby to limit size in MMS
        // in case of low quality and files size below 2M.
        // Why is 0.95????? [Need Check]
        if (needUpdateValue && profile != null && CamcorderProfile.QUALITY_LOW == profile.quality
                && (size < 2 * 1024 * 1024)) {
            size = (long) ((double) size / 0.95);
        }

        return size;
    }

    public void deleteVideoFile(String fileName) {
        File f = new File(fileName);
        if (!f.delete()) {
            Log.i(TAG, "[deleteVideoFile] Could not delete " + fileName);
        }
    }

    public long getRecorderMaxSize(long limitSize) {
        // Set maximum file size.
        // fat file system only support single file max size 4G,so if
        // max file size is larger than 4g set it to 4g
        // Storage.getStorageCapbility() return 4294967295L means fat file
        // system 0L means not fat

        long maxFileSize = Storage.getAvailableSpace() - Storage.RECORD_LOW_STORAGE_THRESHOLD;
        if (limitSize > 0 && limitSize < maxFileSize) {
            maxFileSize = limitSize;
        } else if (maxFileSize >= VIDEO_4G_SIZE
                && NOT_FAT_FILE_SYSTEM != Storage.getStorageCapbility()) {
            maxFileSize = VIDEO_4G_SIZE;
        }
        return maxFileSize;
    }
   /**
    * compute file duration.
    * @param fileName the file want know long.
    * @return return duration
    */
    public long getDuration(String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(fileName);
            return Long.valueOf(retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (IllegalArgumentException e) {
            return INVALID_DURATION;
        } catch (RuntimeException e) {
            return FILE_ERROR;
        } finally {
            retriever.release();
        }
    }

    public void closeVideoFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "[closeVideoFileDescriptor] Fail to close fd", e);
            }
        }
    }

    public String createFileTitle(long dateTaken, AppController appController) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                appController.getActivity().getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }

    public String generateVideoFileName(int outputFileFormat, String suffix) {
        if (!Storage.isStorageReady()) {
            Storage.initializeStorageState();
        }
        mVideoTempPath.delete(0, mVideoTempPath.length());
        mVideoTempPath.append(Storage.getFileDirectory());
        mVideoTempPath.append("/videorecorder");
        mVideoTempPath.append(convertOutputFormatToFileExt(outputFileFormat));
        if (suffix != null) {
            mVideoTempPath.append("_");
            mVideoTempPath.append(suffix);
        }
        mVideoTempPath.append(".tmp");
        Log.i(TAG, "[generateVideoFilename] mVideoFilename = " + mVideoTempPath.toString());
        return mVideoTempPath.toString();
    }

    public String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    public String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            Log.i(TAG, "[convertOutputFormatToFileExt] return .mp4");
            return ".mp4";
        }
        Log.i(TAG, "[convertOutputFormatToFileExt] return .3gp");
        return ".3gp";
    }



}
