/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2015. All rights reserved.
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

#ifndef _HDRDEFS_H_
#define _HDRDEFS_H_

#include <mtkcam/UITypes.h>

#define L1_CACHE_BYTES 32

enum
{
    HDRProcParam_Begin = 0,

    HDRProcParam_Get_src_main_format,
    HDRProcParam_Get_src_main_size,
    HDRProcParam_Get_src_small_format,
    HDRProcParam_Get_src_small_size,

    HDRProcParam_Set_sensor_size,
    HDRProcParam_Set_sensor_type,

    HDRProcParam_Set_AOEMode,
    HDRProcParam_Set_MaxSensorAnalogGain,
    HDRProcParam_Set_MaxAEExpTimeInUS,
    HDRProcParam_Set_MinAEExpTimeInUS,
    HDRProcParam_Set_ShutterLineTime,
    HDRProcParam_Set_MaxAESensorGain,
    HDRProcParam_Set_MinAESensorGain,
    HDRProcParam_Set_ExpTimeInUS0EV,
    HDRProcParam_Set_SensorGain0EV,
    HDRProcParam_Set_FlareOffset0EV,
    HDRProcParam_Set_GainBase0EV,
    HDRProcParam_Set_LE_LowAvg,
    HDRProcParam_Set_SEDeltaEVx100,
    HDRProcParam_Set_Histogram,

    HDRProcParam_Set_DetectFace,
    HDRProcParam_Set_FlareHistogram,
    HDRProcParam_Set_PLineAETable,

    HDRProcParam_Num
};

struct HDRProc_ShotParam
{
    // The dimensions for captured pictures in pixels (width x height)
    NSCam::MSize  pictureSize;

    // The dimensions for postview in pixels (width x height)
    NSCam::MSize  postviewSize;

    // This control can be used to implement digital zoom
    NSCam::MRect  scalerCropRegion;

    // The transform: includes rotation and flip
    // The rotation angle in degrees relative to the orientation of the camera.
    //
    // For example, suppose the natural orientation of the device is portrait.
    // The device is rotated 270 degrees clockwise, so the device orientation is
    // 270. Suppose a back-facing camera sensor is mounted in landscape and the
    // top side of the camera sensor is aligned with the right edge of the
    // display in natural orientation. So the camera orientation is 90. The
    // rotation should be set to 0 (270 + 90).
    //
    // Flip: horizontally/vertically
    // reference value: mtkcam/ImageFormat.h
    MINT32        transform;
};

struct HDRProc_JpegParam
{
    // The dimensions (in pixels) of the compressed JPEG image
    NSCam::MSize jpegSize;

    // The width (in pixels) of the embedded JPEG thumbnail
    NSCam::MSize thumbnailSize;

    // compression quality of the final JPEG image
    // 1-100; larger is higher quality
    MINT32       jpegQuality;

    // Compression quality of JPEG thumbnail
    // 1-100; larger is higher quality
    MINT32       thumbnailQuality;
};

enum HDROutputType
{
    HDR_OUTPUT_JPEG_YUV,
    HDR_OUTPUT_JPEG_THUMBNAIL_YUV,
//    HDR_OUTPUT_POSTVIEW,
    HDR_OUTPUT_NUM
};

enum HDRBufferType
{
    HDR_BUFFER_SOURCE,
    HDR_BUFFER_SMALL,
    HDR_BUFFER_SE,
    HDR_BUFFER_WORKING,
    HDR_BUFFER_ORI_WEIGHT_MAP,
    HDR_BUFFER_BLURRED_WEIGHT_MAP,
    HDR_BUFFER_DOWNSIZED_WEIGHT_MAP,
    HDR_BUFFER_BLENDING,
//    HDR_BUFFER_POSTVIEW
};

#endif // _HDRDEFS_H_
