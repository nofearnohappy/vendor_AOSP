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

#define DEBUG_LOG_TAG "PROC"

#include "HDR.h"
#include "HDRProc.h"
#include "Platform.h"

#include <mtkcam/v3/hal/aaa_hal_common.h>

#include <common/hdr/2.0/utils/Debug.h>

using namespace NSCam;
using namespace NS3Av3;

// ---------------------------------------------------------------------------

IHDRProc* IHDRProc::createInstance()
{
    return &HDRProc::getInstance();
}

// ---------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(HDRProc);

HDRProc::HDRProc()
{
    mpHDR = NULL;
}

HDRProc::~HDRProc()
{
    uninitLocked();
}

MBOOL HDRProc::init(MINT32 openID)
{
    AutoMutex l(mHDRlock);

    if (mpHDR == NULL)
	{
        mpHDR = new NSCam::HDR("hdr", 0, openID);
        if (mpHDR == NULL)
        {
            HDR_LOGE("[HDRProcInit] init HDRProc failed");
            return MFALSE;
        }
    }

    HDR_LOGD("[HDRProcInit] init HDRProc done for camera(%d)", openID);

    return MTRUE;
}

MBOOL HDRProc::uninit()
{
    AutoMutex l(mHDRlock);

    // exit performance mode
    Platform::getInstance().exitPerfMode(mSceneHandle);

    return uninitLocked();
}

MBOOL HDRProc::uninitLocked()
{
    if (mpHDR == NULL)
        return MTRUE;

    delete mpHDR;
    mpHDR = NULL;

    return MTRUE;
}

MBOOL HDRProc::setParam(MUINT32 paramId, MUINTPTR iArg1, MUINTPTR iArg2)
{
    if ((paramId <= HDRProcParam_Begin) || (paramId >= HDRProcParam_Num))
    {
        HDR_LOGE("[HDRProc::setParam] invalid paramId:%d", paramId);
        return MFALSE;
    }

    AutoMutex l(mHDRlock);

    return mpHDR->setParam(paramId, iArg1, iArg2);
}

MBOOL HDRProc::getParam(MUINT32 paramId, MUINT32 & rArg1, MUINT32 & rArg2)
{
    if ((paramId <= HDRProcParam_Begin) || (paramId >= HDRProcParam_Num))
    {
        HDR_LOGE("[HDRProc::getParam] invalid paramId:%d", paramId);
        return MFALSE;
    }

    AutoMutex l(mHDRlock);

    return mpHDR->getParam(paramId, rArg1, rArg2);
}

MBOOL HDRProc::setShotParam(
        MSize& pictureSize, MSize& postviewSize,
        MRect& cropRegion, MINT32 transform, MINT32 zoomratio)
{
    MBOOL ret = MTRUE;

    HDRProc_ShotParam param = { pictureSize, postviewSize, cropRegion, transform };

    HDR_LOGD("[HDRProc::setShotParam] size(%dx%d) postview(%dx%d) crop(%d,%d,%dx%d)",
            pictureSize.w, pictureSize.h,
            // TODO: check if postivew setting can be removed
            postviewSize.w, postviewSize.h,
            cropRegion.leftTop().x, cropRegion.leftTop().y,
            cropRegion.width(), cropRegion.height());

    {
        AutoMutex l(mHDRlock);
        if (!mpHDR->setShotParam(&param))
        {
            HDR_LOGE("[HDRProc::setShotParam] setShotParam failed");
            ret = MFALSE;
        }
    }

    return ret;
}

MBOOL HDRProc::setJpegParam(
        MSize& jpegSize, MSize& thumbnailSize,
        MINT32 jpegQuality, MINT32 thumbnailQuality)
{
    MBOOL ret = MTRUE;

    HDRProc_JpegParam param = { jpegSize, thumbnailSize, jpegQuality, thumbnailQuality };

    HDR_LOGD("[HDRProc::setJpegParam] jpeg(%dx%d) thumbnail(%dx%d) " \
            "quality(%d) thumbnailQuality(%d)",
            jpegSize.w, jpegSize.h,
            thumbnailSize.w, thumbnailSize.h,
            jpegQuality, thumbnailQuality);

    {
        AutoMutex l(mHDRlock);
        if (!mpHDR->setJpegParam(&param))
        {
            HDR_LOGE("[HDRProcSetJpegParam] setJpegParam failed");
            ret = MFALSE;
        }
    }

    return ret;
}

MBOOL HDRProc::prepare()
{
    AutoMutex l(mHDRlock);

    MBOOL ret = MTRUE;

    // enter performance mode
    mSceneHandle = Platform::getInstance().enterPerfMode();

    ret = mpHDR->updateInfo_cam3();
    if (MTRUE != ret)
    {
        HDR_LOGE("[HDRProcPrepare] updateInfo_cam3 failed");
        goto lbExit;
    }

    ret = mpHDR->EVBracketCapture_cam3();
    if (MTRUE != ret)
    {
        HDR_LOGE("[HDRProcPrepare] createSourceAndSmallImg_cam3 fail");
    }

lbExit:
    return ret;
}

MBOOL HDRProc::addInputFrame(
        MINT32 frameIndex, const sp<IImageBuffer>& inBuffer)
{
    AutoMutex l(mHDRlock);

    return mpHDR->addInputFrame_cam3(frameIndex, inBuffer);
}

MBOOL HDRProc::addOutputFrame(
        HDROutputType type, sp<IImageBuffer>& outBuffer)
{
    AutoMutex l(mHDRlock);

    return mpHDR->addOutputFrame_cam3(type, outBuffer);
}

MBOOL HDRProc::start()
{
    AutoMutex l(mHDRlock);

    MBOOL ret = MTRUE;

    ret = mpHDR->process_cam3();
    if (MTRUE != ret)
    {
        HDR_LOGE("[HDRProcStart] process_cam3 failed");
    }

    return ret;
}

MBOOL HDRProc::release()
{
    AutoMutex l(mHDRlock);

    MBOOL ret = MTRUE;

    ret = mpHDR->release_cam3();
    if (MTRUE != ret)
    {
        HDR_LOGE("[HDRProcRelease] release_cam3 failed");
    }

    return ret;
}

MBOOL HDRProc::getHDRCapInfo(
        MINT32& i4FrameNum,
        Vector<MUINT32>& vu4Eposuretime,
        Vector<MUINT32>& vu4SensorGain,
        Vector<MUINT32>& vu4FlareOffset)
{
    MBOOL ret = MTRUE;
    Vector<NS3Av3::CaptureParam_T> rCap3AParam;

    {
        AutoMutex l(mHDRlock);
        mpHDR->getCaptureInfo_cam3(rCap3AParam, i4FrameNum);
    }

    HDR_LOGD("[HDRProc::getHDRCapInfo] hdrFrameNum(%d)", i4FrameNum);

    Vector<NS3Av3::CaptureParam_T>::iterator it = rCap3AParam.begin();
    while (it != rCap3AParam.end())
    {
        HDR_LOGD("=================\n" \
                "[HDRProc::getHDRCapInfo] u4Eposuretime(%d) " \
                " u4AfeGain(%d) u4IspGain(%d) u4RealISO(%d) u4FlareOffset(%d)",
                it->u4Eposuretime, it->u4AfeGain, it->u4IspGain,
                it->u4RealISO, it->u4FlareOffset);

        vu4Eposuretime.push_back(it->u4Eposuretime);
        vu4SensorGain.push_back(it->u4AfeGain);
        vu4FlareOffset.push_back(it->u4FlareOffset);

        it++;
    }

    return ret;
}

MVOID HDRProc::setCompleteCallback(
        HDRProcCompleteCallback_t completeCB, MVOID* user)
{
    AutoMutex l(mHDRlock);

    return mpHDR->setCompleteCallback(completeCB, user);
}
