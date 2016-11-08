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

#define LOG_TAG "MtkCam/LegacyPipeline_Fake"

#include <common.h>
using namespace android;
using namespace NSCam;

#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/stream/IStreamInfo.h>
using namespace NSCam::v3;

#include <v3/pipeline/PipelineContext.h>
using namespace NSCam::v3::NSPipelineContext;

#include <v1/Processor/ResultProcessor.h>
#include <LegacyPipeline/StreamBufferProvider.h>
using namespace NSCam::v1;

#include <LegacyPipeline/ILegacyPipeline.h>
using namespace NSCam::v1::NSLegacyPipeline;
/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)

#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

//#define MY_LOGD1(...)               MY_LOGD_IF((mLogLevel>=1),__VA_ARGS__)
//#define MY_LOGD2(...)               MY_LOGD_IF((mLogLevel>=2),__VA_ARGS__)
//#define MY_LOGD3(...)               MY_LOGD_IF((mLogLevel>=3),__VA_ARGS__)
#define MY_LOGD1(...)               MY_LOGD(__VA_ARGS__)
#define MY_LOGD2(...)               MY_LOGD(__VA_ARGS__)
#define MY_LOGD3(...)               MY_LOGD(__VA_ARGS__)

#define FUNC_START                  MY_LOGD2("+")
#define FUNC_END                    MY_LOGD2("-")

/******************************************************************************
*
*******************************************************************************/
class LegacyPipeline_Fake
    : public ILegacyPipeline
{
public:
    LegacyPipeline_Fake();
    ~LegacyPipeline_Fake() {};

public:     ////                    Interface for destroy.
    /**
     * destroy LegacyPipeline.
     *
     */
    virtual MVOID                       destroyInstance();

    virtual MVOID                       setPipelineContext( sp<PipelineContext> const spPipelineContext );

    virtual MERROR                      flush() { return OK;};

    virtual MERROR                      waitUntilDrained() { return OK;};

    virtual wp<ResultProcessor>         getResultProcessor();

    virtual MERROR                      submitRequest( sp<IPipelineFrame>);

    virtual MERROR                      setMetaStreamInfo(sp<IMetaStreamInfo> app, sp<IMetaStreamInfo> hal);

    virtual sp<IMetaStreamInfo>         queryControlAppStreamInfo() { return mspAppMeta; };

    virtual sp<IMetaStreamInfo>         queryControlHalStreamInfo() { return mspHalMeta; };

protected: //// Data Member
    sp<PipelineContext>                 mspPipelineContext;
    sp<ResultProcessor>                 mspResultProcessor;
    RequestBuilder*                     mpRequestBuilder;
    MINT32                              mPipelineContextId;

protected:
    sp<IMetaStreamInfo>                 mspAppMeta;
    sp<IMetaStreamInfo>                 mspHalMeta;
};


/******************************************************************************
*
*******************************************************************************/
sp<ILegacyPipeline>
ILegacyPipeline::
createFakePipeline()
{
    return new LegacyPipeline_Fake();
}

/******************************************************************************
*
*******************************************************************************/
wp<ResultProcessor>
LegacyPipeline_Fake::
getResultProcessor()
{
    return mspResultProcessor;
}

/******************************************************************************
*
*******************************************************************************/
LegacyPipeline_Fake::
LegacyPipeline_Fake()
    : mPipelineContextId(0)
    , mspPipelineContext(NULL)
    , mspResultProcessor(ResultProcessor::createInstance())
    , mpRequestBuilder(new RequestBuilder())
{
    //mspvStreamBufferProvider.clear();
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipeline_Fake::
destroyInstance()
{
    mspPipelineContext->waitUntilDrained();
    mspPipelineContext = NULL;
    mspResultProcessor = NULL;
    //mspvStreamBufferProvider.clear();
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipeline_Fake::
setPipelineContext(sp<PipelineContext> const spPipelineContext)
{
    mspPipelineContext = spPipelineContext;
    return;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline_Fake::
submitRequest(sp<IPipelineFrame> rpFrame)
{
    if ( rpFrame != 0 ) {
        if( OK != mspPipelineContext->queue(rpFrame) ) {
            MY_LOGE("queue pFrame failed");
        }
    }

    return OK;
}

/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline_Fake::
setMetaStreamInfo(
    sp<IMetaStreamInfo> app,
    sp<IMetaStreamInfo> hal
)
{
    mspAppMeta = app;
    mspHalMeta = hal;
    return OK;
}