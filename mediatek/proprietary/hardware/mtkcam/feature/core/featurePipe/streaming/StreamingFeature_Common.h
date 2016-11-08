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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

#ifndef _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_COMMON_H_
#define _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_COMMON_H_

#include "MtkHeader.h"
//#include <mtkcam/feature/featurePipe/IStreamingFeaturePipe.h>
//#include <mtkcam/imageio/ispio_pipe_ports.h>
//#include <mtkcam/iopipe/PostProc/IHalPostProcPipe.h>
//#include <mtkcam/iopipe/PostProc/IFeatureStream.h>
//#include <mtkcam/iopipe/PostProc/INormalStream.h>

#include <featurePipe/core/include/ImageBufferPool.h>
#include <featurePipe/core/include/GraphicBufferPool.h>

#include "StreamingFeatureData.h"
#include "DebugControl.h"

#define SUPPORT_HAL3

#ifdef SUPPORT_HAL3
#define BOOL_SUPPORT_HAL3 (true)
#else
#define BOOL_SUPPORT_HAL3 (false)
#endif

#define MAX_FULL_WIDTH    2304
#define MAX_FULL_HEIGHT   4096
#define DS_IMAGE_WIDTH    320
#define DS_IMAGE_HEIGHT   320
#define MAX_WARP_WIDTH    320
#define MAX_WARP_HEIGHT   320

#define MAX_FULL_SIZE (MSize(MAX_FULL_WIDTH, MAX_FULL_HEIGHT))
#define DS_IMAGE_SIZE (MSize(DS_IMAGE_WIDTH, DS_IMAGE_HEIGHT))
#define MAX_WARP_SIZE (MSize(MAX_WARP_WIDTH, MAX_WARP_HEIGHT))

// GPU warp support up to 5 now
#define NUM_DEFAULT_BUFFER 5

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

MBOOL useMDPHardware();

IImageBuffer* findOutBuffer(const NSCam::NSIoPipe::NSPostProc::QParams &qparam, unsigned skip);

MBOOL copyImageBuffer(IImageBuffer *src, IImageBuffer *dst);

void* getGraphicBufferAddr(IImageBuffer *imageBuffer);

MSize calcDsImgSize(const MSize &src);

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam

#endif // _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_COMMON_H_
