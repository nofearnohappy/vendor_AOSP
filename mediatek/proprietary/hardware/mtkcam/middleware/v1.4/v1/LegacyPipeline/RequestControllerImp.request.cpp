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

#define LOG_TAG "MtkCam/RequestControllerImp"
//
#include "MyUtils.h"

#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>

//
using namespace android;
#include <v1/converter/RequestSettingBuilder.h>
#include "RequestControllerImp.h"
#include <LegacyPipeline/ILegacyPipeline.h>


using namespace NSCam;
using namespace NSCam::v1::NSLegacyPipeline;



/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s] " fmt, __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

#if 1
#define FUNC_START     MY_LOGD("+")
#define FUNC_END       MY_LOGD("-")
#else
#define FUNC_START
#define FUNC_END
#endif

typedef NSCam::v3::Utils::HalMetaStreamBuffer       HalMetaStreamBuffer;
typedef NSCam::v3::Utils::HalImageStreamBuffer      HalImageStreamBuffer;
typedef HalMetaStreamBuffer::Allocator              HalMetaStreamBufferAllocatorT;

/******************************************************************************
 *
 ******************************************************************************/
RequestControllerImp::RequestThread::
RequestThread(
    wp< RequestControllerImp > parent
)
    : mpRequestControllerImp(parent)
    //
    , mpPipeline(NULL)
    //
    , mRequestExit(false)
    , mStart(false)
{
}

/******************************************************************************
 *
 ******************************************************************************/
bool
RequestControllerImp::RequestThread::
threadLoop()
{
    if ( !exitPending() &&
         !mRequestExit
    ) {
        if ( !mStart ) return true;

        sp<IMetaStreamBuffer> pAppMetaStreamBuffer;
        sp<HalMetaStreamBuffer> pHalMetaStreamBuffer;
        if ( waitForNextRequest(pAppMetaStreamBuffer, pHalMetaStreamBuffer) != OK ) {
            MY_LOGE("waitForNextRequest fail.");
            return false;
        }
        //
        sp< ILegacyPipeline > pPipeline = mpPipeline.promote();
        if ( pPipeline == NULL) {
            MY_LOGE("LegacyPipeline promote fail.");
            return false;
        }
#warning "FIXME workaround"
        sp< IRequestUpdater > pRequestUpdater = mpRequestUpdater.promote();
        if ( pRequestUpdater == NULL) {
            MY_LOGE("RequestUpdater promote fail.");
            return false;
        }
        pPipeline->submitRequest(
                    pRequestUpdater->constructPipelineFrame(
                                        mRequestNumber,
                                        pAppMetaStreamBuffer,
                                        pHalMetaStreamBuffer
                                    ));
        return true;
    }

    MY_LOGD("RequestThread exit.");
    return false;
}

/******************************************************************************
 *
 ******************************************************************************/
IMetaStreamBuffer*
RequestControllerImp::RequestThread::
createMetaStreamBuffer(
    const IMetadata&    pSetting,
    sp<IMetaStreamInfo> pStreamInfo
)
{
    // allocate buffer
    IMetaStreamBuffer* pStreamBuffer
         = HalMetaStreamBufferAllocatorT(pStreamInfo.get())(pSetting);

    //
    return pStreamBuffer;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::RequestThread::
start(
    wp< ILegacyPipeline >& rpPipeline,
    wp< IRequestUpdater >  rpRequestUpdater,
    MINT32               aStartRequestNumber,
    MINT32               aEndRequestNumber
)
{
    FUNC_START;

    Mutex::Autolock _l(mLock);

    mRequestNumberMin = aStartRequestNumber;
    mRequestNumberMax = aEndRequestNumber;

    mpPipeline = rpPipeline;
    mpRequestUpdater = rpRequestUpdater;
    mStart = true;

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::RequestThread::
stop()
{
    FUNC_START;
    //
    Mutex::Autolock _l(mLock);
    mStart = false;

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::RequestThread::
waitForNextRequest(
    sp<IMetaStreamBuffer>& aAppMetaStreamBuffer,
    sp<HalMetaStreamBuffer>& aHalMetaStreamBuffer
)
{
    FUNC_START;
    //
    if ( mRequestNumber > mRequestNumberMax ) mRequestNumber = mRequestNumberMin;

    sp< RequestControllerImp > pRequestControllerImp = mpRequestControllerImp.promote();
    if ( pRequestControllerImp == NULL) {
        MY_LOGE("RequestController promote fail.");
        return BAD_VALUE;
    }
    //
    // construct request setting
    IMetadata settingAppMeta;
    IMetadata settingHalMeta;
#warning "FIXME shoule not use default request"
    //pRequestControllerImp->getRequestSettingBuilder()->getRequest(mRequestNumber++, settingAppMeta);
    {
        mRequestNumber++;
        pRequestControllerImp->getRequestSettingBuilder()->createDefaultRequest(1, settingAppMeta);// preview->1
    }

    sp< IRequestUpdater > pRequestUpdater = mpRequestUpdater.promote();
    if ( pRequestUpdater == NULL) {
        MY_LOGE("RequestUpdater promote fail.");
        return BAD_VALUE;
    }

    pRequestUpdater->updateAppSetting(&settingAppMeta);
    pRequestUpdater->updateHalSetting(&settingHalMeta);
    //
    aAppMetaStreamBuffer =
        createMetaStreamBuffer(
            settingAppMeta,
            pRequestControllerImp->queryControlAppStreamInfo()
        );

#warning "TODO"
    aHalMetaStreamBuffer
         = HalMetaStreamBufferAllocatorT(
                pRequestControllerImp->queryControlHalStreamInfo().get()
           )(settingHalMeta);
    /*aHalMetaStreamBuffer =
        createMetaStreamBuffer(
            settingHalMeta,
            pRequestControllerImp->queryControlHalStreamInfo()
        );*/

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::RequestThread::
submitRequest(
    Vector< IMetadata* >       settings
) const
{
#warning "TODO for feature"
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
void
RequestControllerImp::RequestThread::
onLastStrongRef(const void* /*id*/)
{
    requestExit();
}

/******************************************************************************
 *
 ******************************************************************************/
void
RequestControllerImp::RequestThread::
requestExit()
{
    //let deque thread back
    Thread::requestExit();

    join();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
RequestControllerImp::RequestThread::
readyToRun()
{
    // set name
    ::prctl(PR_SET_NAME, (unsigned long)REQUESTCONTROLLER_NAME, 0, 0, 0);

    // set normal
    struct sched_param sched_p;
    sched_p.sched_priority = 0;
    ::sched_setscheduler(0, REQUESTCONTROLLER_POLICY, &sched_p);
    ::setpriority(PRIO_PROCESS, 0, REQUESTCONTROLLER_PRIORITY);
    //
    ::sched_getparam(0, &sched_p);

    MY_LOGD(
        "tid(%d) policy(%d) priority(%d)"
        , ::gettid(), ::sched_getscheduler(0)
        , sched_p.sched_priority
    );

    //
    return OK;
}
