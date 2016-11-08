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

#define LOG_TAG "MtkCam/LegacyPipeline"

#include <common.h>
using namespace android;
using namespace NSCam;

#include <vector>
using namespace std;

#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/stream/IStreamInfo.h>
using namespace NSCam::v3;

#include <v3/pipeline/PipelineContext.h>
#include <v3/pipeline/PipelineContextImpl.h>
using namespace NSCam::v3::NSPipelineContext;

#include <v1/Processor/ResultProcessor.h>
#include <v1/BufferProvider/StreamBufferProvider.h>
using namespace NSCam::v1;

#include <LegacyPipeline/ILegacyPipeline.h>
#include <LegacyPipeline/LegacyPipeline.h>
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
LegacyPipeline::
LegacyPipeline()
    : mPipelineContextId(-1)
    , mspPipelineContext(NULL)
    , mspResultProcessor(NULL)
    , mpRequestBuilder(new RequestBuilder())
{
    mspvStreamBufferProvider.clear();
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipeline::
destroyInstance()
{
    mspPipelineContext->waitUntilDrained();
    mspPipelineContext = NULL;
    mspResultProcessor = NULL;
    mspvStreamBufferProvider.clear();
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipeline::
setBufferProvider(vector<sp<StreamBufferProvider> > const vBufferProvider)
{
    mspvStreamBufferProvider = vBufferProvider;
    return;
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipeline::
setResultProcessor(sp<ResultProcessor> const spResultProcessor)
{
    mspResultProcessor = spResultProcessor;
    return;
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipeline::
setPipelineContext(sp<PipelineContext> const spPipelineContext)
{
    mspPipelineContext = spPipelineContext;
    return;
}


/******************************************************************************
*
*******************************************************************************/
wp<StreamBufferProvider>
LegacyPipeline::
getBufferProvider(StreamId_T const streamId)
{
    vector<sp<StreamBufferProvider> >::iterator providerIter;
    sp<IImageStreamInfo> spImageStreamInfo;
    for( providerIter = mspvStreamBufferProvider.begin() ; providerIter != mspvStreamBufferProvider.end() ; providerIter++ )
    {
        (*providerIter)->queryImageStreamInfo(spImageStreamInfo);
        if( streamId == spImageStreamInfo->getStreamId() )
        {
            break;
        }
    }

    if( providerIter != mspvStreamBufferProvider.end() )
    {
        MY_LOGE("stream buffer provider of streamId:%d not found.", streamId);
    }
    return (*providerIter).get();
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
resetFrame()
{
    mpRequestBuilder = new RequestBuilder();
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
setFrameRoot(MINT32 const nodeId)
{
    mpRequestBuilder->setRootNode(
                        NodeSet().add(nodeId)
                      );
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
setFrameEdge(vector<NodeEdge> const vNodeEdge)
{
    vector<NodeEdge>::const_iterator edgeIter;
    for( edgeIter = vNodeEdge.begin() ; edgeIter != vNodeEdge.end() ; edgeIter++ )
    {
        mpRequestBuilder->setNodeEdges(
                              NodeEdgeSet().addEdge(
                                                (*edgeIter).startNodeId,
                                                (*edgeIter).endNodeId
                                            )
                          );
    }
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
setFrameIOMap(vector<FrameIOMap> const vFrameIOMap)
{
    vector<FrameIOMap>::const_iterator mapIter;
    for( mapIter = vFrameIOMap.begin() ; mapIter != vFrameIOMap.end() ; mapIter++ )
    {
        IOMapSet imageIOMapSet = IOMapSet();
        IOMapSet metaIOMapSet = IOMapSet();

        vector<IOSet>::const_iterator setIter;
        for( setIter = (*mapIter).vImageIOSet.begin() ; setIter != (*mapIter).vImageIOSet.end() ; setIter++ )
        {
            IOMap imageIOMap = IOMap();
            vector<StreamId_T>::const_iterator idIter;
            for( idIter = (*setIter).vInStreamId.begin() ; idIter != (*setIter).vInStreamId.end() ; idIter++ )
            {
                imageIOMap = imageIOMap.addIn(*idIter);
            }
            for( idIter = (*setIter).vOutStreamId.begin() ; idIter != (*setIter).vOutStreamId.end() ; idIter++ )
            {
                imageIOMap = imageIOMap.addOut(*idIter);
            }
            imageIOMapSet = imageIOMapSet.add(imageIOMap);
        }
        for( setIter = (*mapIter).vMetaIOSet.begin() ; setIter != (*mapIter).vMetaIOSet.end() ; setIter++ )
        {
            IOMap metaIOMap = IOMap();
            vector<StreamId_T>::const_iterator idIter;
            for( idIter = (*setIter).vInStreamId.begin() ; idIter != (*setIter).vInStreamId.end() ; idIter++ )
            {
                metaIOMap = metaIOMap.addIn(*idIter);
            }
            for( idIter = (*setIter).vOutStreamId.begin() ; idIter != (*setIter).vOutStreamId.end() ; idIter++ )
            {
                metaIOMap = metaIOMap.addOut(*idIter);
            }
            metaIOMapSet = metaIOMapSet.add(metaIOMap);
        }

        mpRequestBuilder->setIOMap(
                                (*mapIter).nodeId,
                                imageIOMapSet,
                                metaIOMapSet
                          );
    }
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
replaceStreamInfo(StreamId_T const streamId, sp<IImageStreamInfo> const pStreamInfo)
{
    mpRequestBuilder->replaceStreamInfo(streamId, pStreamInfo);
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
updateFrameCallback(wp<AppCallbackT> const pCallback)
{
    mpRequestBuilder->updateFrameCallback(pCallback);
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipeline::
queue(
    MUINT32             const frameNumber,
    StreamId_T          const appMetaStreamId,
    sp<IMetaStreamInfo> const appMetaStreamInfo,
    IMetadata*          const appMetadata,
    StreamId_T          const halMetaStreamId,
    sp<IMetaStreamInfo> const halMetaStreamInfo,
    IMetadata*          const halMetadata)
{
    sp<IMetaStreamBuffer> pAppMetaControlSB = createMetaStreamBuffer(
                                                    appMetaStreamInfo,
                                                    *appMetadata,
                                                    false);
    sp<HalMetaStreamBuffer> pHalMetaControlSB =
        HalMetaStreamBuffer::Allocator(halMetaStreamInfo.get())(*halMetadata);

    mpRequestBuilder->setMetaStreamBuffer(appMetaStreamId, pAppMetaControlSB);
    mpRequestBuilder->setMetaStreamBuffer(halMetaStreamId, pHalMetaControlSB);

    sp<IPipelineFrame> pFrame = mpRequestBuilder->build(frameNumber, mspPipelineContext);

    if( ! pFrame.get() ) {
        MY_LOGE("build request failed");
        return UNKNOWN_ERROR;
    }

    if( OK != mspPipelineContext->queue(pFrame) ) {
        MY_LOGE("queue pFrame failed");
        return UNKNOWN_ERROR;
    }
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
sp<IMetaStreamBuffer>
LegacyPipeline::
createMetaStreamBuffer(
    sp<IMetaStreamInfo> const pStreamInfo,
    IMetadata const& rSettings,
    MBOOL const repeating)
{
    wp<HalMetaStreamBuffer> pStreamBuffer = HalMetaStreamBuffer::Allocator(pStreamInfo.get())(rSettings);
    sp<HalMetaStreamBuffer> spStreamBuffer = pStreamBuffer.promote();
    if( spStreamBuffer == NULL )
    {
        MY_LOGE("create Meta Stream Buffer failed");
        return NULL;
    }

    spStreamBuffer->setRepeating(repeating);
    return spStreamBuffer;
}
