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
//
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>

#include <v1/camutils/CamInfo.h>
#include <v1/IParamsManager.h>
#include <LegacyPipeline/IRequestController.h>
#include <LegacyPipeline/ILegacyPipeline.h>
#include <Scenario/IFlowControl.h>
#include <v1/converter/RequestSettingBuilder.h>
#include "RequestControllerImp.h"
#include <v1/Processor/StreamingProcessor.h>

using namespace NSCam;
using namespace NSCam::v1;
using namespace NSCam::v1::NSLegacyPipeline;
using namespace android;
using namespace NSCam::v3;

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

/******************************************************************************
 *
 ******************************************************************************/
sp< IRequestController >
IRequestController::
createInstance(
    char                 const *name,
    sp< CamMsgCbInfo >   const &rpCamMsgCbInfo,
    sp< IParamsManager > const &rpParamsManager
)
{
    return new RequestControllerImp(
                    name,
                    rpCamMsgCbInfo,
                    rpParamsManager
               );
}

/******************************************************************************
 *
 ******************************************************************************/
RequestControllerImp::
RequestControllerImp(
    char                 const *name,
    sp< CamMsgCbInfo >   const &rpCamMsgCbInfo,
    sp< IParamsManager > const &rpParamsManager
)
    : mName(name)
    , mpCamMsgCbInfo(rpCamMsgCbInfo)
    , mpParamsManager(rpParamsManager)
{
    mpRequestThread = new RequestThread(this);
    if( mpRequestThread->run(REQUESTCONTROLLER_NAME) != OK ) {
        MY_LOGE("Thread init fail.");
    }
}

/******************************************************************************
 *
 ******************************************************************************/
char const*
RequestControllerImp::
getName() const
{
    return mName;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
startPipeline(
    MINT32               aCameraId,
    MINT32               aStartRequestNumber,
    MINT32               aEndRequestNumber,
    wp<ILegacyPipeline>  apPipeline,
    wp<IRequestUpdater>  apRequestUpdater
)
{
    FUNC_START;

    sp<ILegacyPipeline> pPipeline = apPipeline.promote();
    if ( pPipeline == 0 ) {
        MY_LOGE("invalid LegacyPipeline.");
        return BAD_VALUE;
    }
#warning "workaround"
    setControlAppStreamInfo(pPipeline->queryControlAppStreamInfo());
    setControlHalStreamInfo(pPipeline->queryControlHalStreamInfo());
    //
    if ( mpCamMsgCbInfo != 0 ) {
        mpStreamingProcessor = StreamingProcessor::createInstance(
                                                mpCamMsgCbInfo,
                                                mpRequestSettingBuilder,
                                                mpParamsManager
                                            );
    } else {
        MY_LOGW("Streaming processor does not create.");
    }
    //
#warning "111"
    sp<ResultProcessor> pProcessor = pPipeline->getResultProcessor().promote();
    if ( pProcessor != 0 ) {
        pProcessor->registerListener(
                    aStartRequestNumber,
                    aEndRequestNumber,
                    true,
                    mpStreamingProcessor
                );
    } else {
        MY_LOGE("Cannot get result processor.");
    }

    //
    mCameraId = aCameraId;
    mpPipeline = apPipeline;
    //
    mpRequestThread->start(
                        apPipeline,
                        apRequestUpdater,
                        aStartRequestNumber,
                        aEndRequestNumber
                    );
    //
    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
stopPipeline()
{
    FUNC_START;

    mpRequestThread->stop();

    mpRequestSettingBuilder = NULL;
    mpStreamingProcessor = NULL;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
autoFocus()
{
    if ( mpStreamingProcessor != 0 ) {
        return mpStreamingProcessor->startAutoFocus();
    }

#warning "FIXME"
    MY_LOGE("Streaming processor does not set.");
    //return UNKNOWN_ERROR;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
cancelAutoFocus()
{
    if ( mpStreamingProcessor != 0 ) {
        return mpStreamingProcessor->cancelAutoFocus();
    }
#warning "FIXME"
    MY_LOGE("Streaming processor does not set.");
    //return UNKNOWN_ERROR;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
precapture()
{
    if ( mpStreamingProcessor != 0 ) {
        return mpStreamingProcessor->preCapture();
    }

    MY_LOGE("Streaming processor does not set.");
    return UNKNOWN_ERROR;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
setParameters()
{
#warning "TODO"
    FUNC_START;

    if ( mpRequestSettingBuilder == NULL ) {
        MY_LOGD("Create RequestSettingBuilder.");
        mpRequestSettingBuilder = RequestSettingBuilder::createInstance(
                                            0, //mCameraId,
                                            mpParamsManager
                                        );
    }
    //
    if ( mpParamsManagerV3 == NULL ) {
        MY_LOGD("Create IParamsManagerV3.");
        mpParamsManagerV3 = IParamsManagerV3::createInstance(
                                            String8::format(mName),
                                            0, //mCameraId,
                                            mpParamsManager
                                        );
    }
    //
    IMetadata setting;
    mpParamsManagerV3->updateRequest(&setting);
    mpRequestSettingBuilder->setStreamingRequest(setting);

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
sendCommand(
    int32_t cmd,
    int32_t arg1,
    int32_t arg2
)
{
#warning "TODO"
    MY_LOGD("cmd(0x%08X),arg1(0x%08X),arg2(0x%08X)",cmd,arg1,arg2);

    if ( mpStreamingProcessor == 0 ) {
        MY_LOGW("Streaming processor does not exist.");
        return OK;
    }
    //
    switch (cmd)
    {
        case CAMERA_CMD_START_SMOOTH_ZOOM:
        {
            mpStreamingProcessor->startSmoothZoom(arg1);
        }
        case CAMERA_CMD_STOP_SMOOTH_ZOOM:
        {
            mpStreamingProcessor->stopSmoothZoom();
        }
        case CAMERA_CMD_ENABLE_FOCUS_MOVE_MSG:
        {
            //MY_LOGD("[sendCommand] CAMERA_CMD_ENABLE_FOCUS_MOVE_MSG (%d)\n", arg1);
            //enableAFMove(arg1);
        }
        default:
        {
            MY_LOGE("unsupported cmd(0x%08X),arg1(0x%08X),arg2(0x%08X)",cmd,arg1,arg2);
            break;
        }
    };
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
setRequestType( int type )
{
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
RequestControllerImp::
onLastStrongRef( const void* /*id*/)
{
    mpParamsManagerV3       = NULL;
    mpRequestSettingBuilder = NULL;
    mpStreamingProcessor    = NULL;
    mpRequestThread         = NULL;
    //
    mpParamsManager         = NULL;
    mpCamMsgCbInfo          = NULL;

}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
RequestControllerImp::
dump()
{
#warning "TODO"
}

/******************************************************************************
 *
 ******************************************************************************/
sp< RequestSettingBuilder >
RequestControllerImp::
getRequestSettingBuilder()
{
    return mpRequestSettingBuilder;
}
/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
setControlAppStreamInfo(
    sp<IMetaStreamInfo>    apControlAppStreamInfo
)
{
    Mutex::Autolock _l(mStreamInfoLock);

    mpControlAppStreamInfo = apControlAppStreamInfo;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
sp<IMetaStreamInfo>
RequestControllerImp::
queryControlAppStreamInfo()
{
    Mutex::Autolock _l(mStreamInfoLock);

    return mpControlAppStreamInfo;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
setControlHalStreamInfo(
    sp<IMetaStreamInfo>    apControlHalStreamInfo
)
{
    Mutex::Autolock _l(mStreamInfoLock);

    mpControlHalStreamInfo = apControlHalStreamInfo;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
sp<IMetaStreamInfo>
RequestControllerImp::
queryControlHalStreamInfo()
{
    Mutex::Autolock _l(mStreamInfoLock);

    return mpControlHalStreamInfo;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
RequestControllerImp::
submitRequest(
    Vector< IMetadata* >      /*settings*/,
    sp<StreamBufferProvider>& /*resultBuffer*/
) const
{
#warning "for feature TODO"
    return OK;
}
