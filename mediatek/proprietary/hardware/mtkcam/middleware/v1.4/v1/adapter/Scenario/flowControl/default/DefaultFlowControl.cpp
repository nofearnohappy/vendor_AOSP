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

#define LOG_TAG "MtkCam/DefaultFlowControl"
//
#include "../MyUtils.h"
//
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>

#include <Hal3/mtk_platform_metadata_tag.h>

#include <v1/camutils/CamInfo.h>
#include <v1/IParamsManager.h>
#include <LegacyPipeline/IRequestController.h>
#include <LegacyPipeline/ILegacyPipeline.h>
#include <v1/converter/RequestSettingBuilder.h>
#include "DefaultFlowControl.h"
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
DefaultFlowControl::
DefaultFlowControl(
    char const* pcszName,
    MINT32 const i4OpenId,
    sp<IParamsManager> pParamsManager,
    sp<ImgBufProvidersManager> pImgBufProvidersManager
)
    : mpParamsManager(pParamsManager)
    , mpImgBufProvidersMgr(pImgBufProvidersManager)
    , mOpenId(i4OpenId)
    , mName(const_cast<char*>(pcszName))
{
#warning "shoud not do this."
    MY_LOGD("open id %d.", i4OpenId);
    mOpenId = 0;

    mpRequestController = IRequestController::createInstance(
                                                    pcszName,
                                                    NULL,
                                                    pParamsManager
                                                );
}

/******************************************************************************
 *
 ******************************************************************************/
char const*
DefaultFlowControl::
getName()   const
{
    return mName;
}

/******************************************************************************
 *
 ******************************************************************************/
int32_t
DefaultFlowControl::
getOpenId() const
{
    return mOpenId;
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
startPreview()
{
    wp<ILegacyPipeline> wpPipeline;

#warning "fixme. how to get different preview scenario from parameter manager"
    constructNormalPreviewPipeline(wpPipeline, mPipelineId);
    //constructZsdPreviewPipeline(wpPipeline, mPipelineId);
    //constructRecordingPreviewPipeline(wpPipeline, mPipelineId);

    sp<ILegacyPipeline> pPipeline = wpPipeline.promote();
    if ( pPipeline == 0 ) {
        MY_LOGE("Cannot get pipeline. start preview fail.");
        return BAD_VALUE;
    }
    //
#warning "fixme start & end request number"
    return mpRequestController->startPipeline(
                                    mOpenId,
                                    0,/*start*/
                                    1000, /*end*/
                                    wpPipeline,
                                    this
                                );
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
stopPreview()
{
    mpRequestController->stopPipeline();
    gContext->flush();
    gContext->waitUntilDrained();
    gContext = NULL;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
startRecording()
{
    wp<ILegacyPipeline> wpPipeline;

#warning "fixme in some condition, no need to create new recording pipeline"
    if ( 1 ) {
        constructNormalPreviewPipeline(wpPipeline, mPipelineId);
        //constructRecordingPreviewPipeline(wpPipeline, mPipelineId);
    } else {
        // sp<LegacyPipelineManager> pPipelineManager = LegacyPipelineManager::getInstance(mOpenId);
        // wpPipeline = pPipelineManager->getLegacyPipeline(mPipelineId);
    }


    sp<ILegacyPipeline> pPipeline = wpPipeline.promote();

    if ( pPipeline == 0 ) {
        MY_LOGE("Cannot get pipeline. start preview fail.");
        return BAD_VALUE;
    }
    //
#warning "fixme start & end request number"
    return mpRequestController->startPipeline(
                                    mOpenId,
                                    0,/*start*/
                                    1000, /*end*/
                                    wpPipeline,
                                    this
                                );
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
stopRecording()
{
    return mpRequestController->stopPipeline();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
autoFocus()
{
    return mpRequestController->autoFocus();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
cancelAutoFocus()
{
    return mpRequestController->cancelAutoFocus();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
precapture()
{
    return mpRequestController->precapture();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
setParameters()
{
    return mpRequestController->setParameters();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
sendCommand(
    int32_t cmd,
    int32_t arg1,
    int32_t arg2
)
{
    return mpRequestController->sendCommand( cmd, arg1, arg2 );
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
dump(
    int fd,
    Vector<String8>const& args
)
{
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
updateAppSetting( IMetadata* setting )
{
#warning " todo"

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
updateHalSetting( IMetadata* setting )
{
#warning " todo"
    IMetadata::IEntry entry(MTK_HAL_REQUEST_SENSOR_SIZE);
    entry.push_back(gSensorParam.size, Type2Type< MSize >());
    setting->update(entry.tag(), entry);

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
notifySettingRequestNo(
    MINT32    rRequestNo,
    IMetadata* setting
)
{
#warning "TODO"
    // for feature.
    // notify request number for specific setting.
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
DefaultFlowControl::
queryCurrentPipelineId(int& id)
{
#warning "realy need it?"
    id = mPipelineId;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
submitRequest(
    Vector< IMetadata* >      rSettings,
    sp<StreamBufferProvider>& resultBuffer
) const
{
    // for feature
#warning "send callback"
    return mpRequestController->submitRequest( rSettings, resultBuffer );
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
DefaultFlowControl::
onLastStrongRef( const void* /*id*/)
{
    mpRequestController = NULL;
    if ( gContext != 0 ) {
        gContext->flush();
        gContext->waitUntilDrained();
        gContext = NULL;
    }
}