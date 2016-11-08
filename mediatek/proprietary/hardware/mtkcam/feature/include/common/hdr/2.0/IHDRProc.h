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

#ifndef _IHDRPROC_H_
#define _IHDRPROC_H_

#include <utils/Vector.h>
#include "mtkcam/IImageBuffer.h"
#include "HDRDefs.h"

using namespace android;
using namespace NSCam;

typedef MBOOL (*HDRProcCompleteCallback_t)(MVOID* user, MBOOL ret);

// ---------------------------------------------------------------------------

class IHDRProc
{
public:
    static IHDRProc* createInstance();

    // init() initializes HDRProc
    // openID is an index to indicate which camera device to open
    virtual MBOOL   init(MINT32 openID)= 0;
    virtual MBOOL   uninit()= 0;

    virtual MBOOL   setParam(
            MUINT32 paramId, MUINTPTR iArg1, MUINTPTR iArg2 ) = 0;

    virtual MBOOL   getParam(
            MUINT32 paramId, MUINT32 & rArg1, MUINT32 & rArg2 ) = 0;

    virtual MBOOL   setShotParam(
            MSize& pictureSize, MSize& postviewSize,
            MRect& cropRegion, MINT32 transform, MINT32 zoomratio) = 0;

    virtual MBOOL   setJpegParam(
            MSize& jpegSize, MSize& thumbnailSize,
            MINT32 jpegQuality, MINT32 thumbnailQuality) = 0;

    virtual MBOOL   prepare() = 0;

    virtual MBOOL   addInputFrame(
            MINT32 frameIndex, const sp<IImageBuffer>& inBuffer) = 0;
    virtual MBOOL   addOutputFrame(
            HDROutputType type, sp<IImageBuffer>& outBuffer) = 0;

    virtual MBOOL   start() = 0;
    virtual MBOOL   release() = 0;

    // get HDR capture information
    // the vector size is equal to u4FrameNum
    virtual MBOOL   getHDRCapInfo(MINT32& i4FrameNum,
            android::Vector<MUINT32>& vu4Eposuretime,
            android::Vector<MUINT32>& vu4SensorGain,
            android::Vector<MUINT32>& vu4FlareOffset) = 0;

    virtual MVOID   setCompleteCallback(HDRProcCompleteCallback_t completeCB,
            MVOID* user) = 0;

protected:
    virtual ~IHDRProc() {}
};

#endif  // _IHDRPROC_H_
