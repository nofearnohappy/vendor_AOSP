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

#define LOG_TAG "MtkCam/PipeBuilder"

#include <common.h>
using namespace android;
using namespace NSCam;

#include <vector>
using namespace std;

#include <v1/camutils/IBuffer.h>
#include <v1/camutils/IImgBufQueue.h>
#include <v1/camutils/ImgBufQueue.h>
using namespace android::MtkCamUtils;

#include <v3/hwpipeline/NodeId.h>
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/stream/IStreamInfo.h>
using namespace NSCam::v3;

#include <v3/hwpipeline/StreamId.h>
#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>
#include <v3/hwnode/JpegNode.h>
#include <v3/pipeline/PipelineContext.h>
#include <v3/pipeline/PipelineContextImpl.h>
using namespace NSCam::v3::NSPipelineContext;

#include <v1/Processor/ResultProcessor.h>
#include <v1/BufferProvider/StreamBufferProvider.h>
#include <v1/BufferProvider/StreamBufferProviderFactory.h>
using namespace NSCam::v1;

#include <LegacyPipeline/NodePortDefine.h>
#include <LegacyPipeline/ILegacyPipeline.h>
#include <LegacyPipeline/LegacyPipeline.h>
#include <LegacyPipeline/LegacyPipelineManager.h>
#include <LegacyPipeline/LegacyPipelineBuilder.h>
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
LegacyPipelineBuilder::
LegacyPipelineBuilder(char* const pipeName)
    : mPipeName(pipeName)
    , mSensorId(-1)
    , mRootNodeId(0)
    , mspScenarioControl(NULL)
{
    mspvAppMetaStreamInfo.clear();
    mspvHalMetaStreamInfo.clear();
    mvImageStreamUsage.clear();
    mvNodeSetting.clear();
    mvNodeEdge.clear();
}


/******************************************************************************
*
*******************************************************************************/
LegacyPipelineBuilder::
~LegacyPipelineBuilder()
{
    mspScenarioControl = NULL;
    mspvAppMetaStreamInfo.clear();
    mspvHalMetaStreamInfo.clear();
    mvImageStreamUsage.clear();
    mvNodeSetting.clear();
    mvNodeEdge.clear();
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
setSensor(
    MINT32 const sensroId,
    PipelineSensorParam* const pSensorParam)
{
    if( pSensorParam == NULL )
    {
        MY_LOGW("pSensorParam is NULL, set sensor failed.");
        return BAD_VALUE;
    }
    mSensorId = sensroId;
    mSensorParam = (*pSensorParam);
    MY_LOGD2("id:%d, mode:%d, type:%d, (w,h):(%d,%d), fps:%d, pixel mode:%d", mSensorId,
                                                                              mSensorParam.mode,
                                                                              mSensorParam.rawType,
                                                                              mSensorParam.size.w,
                                                                              mSensorParam.size.h,
                                                                              mSensorParam.fps,
                                                                              mSensorParam.pixelmode);
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
addMetaStream(
    vector<sp<IMetaStreamInfo> > const appMetaStreamInfos,
    vector<sp<IMetaStreamInfo> > const halMetaStreamInfos)
{
    vector<sp<IMetaStreamInfo> >::const_iterator paramIter;
    vector<sp<IMetaStreamInfo> >::iterator memberIter;

    //update meta stream infos
    for( paramIter = appMetaStreamInfos.begin() ; paramIter != appMetaStreamInfos.end() ; paramIter++ )
    {
        for( memberIter = mspvAppMetaStreamInfo.begin() ; memberIter != mspvAppMetaStreamInfo.end() ; memberIter++ )
        {
            if( (*paramIter)->getStreamId() == (*memberIter)->getStreamId() )
            {
                MY_LOGW("overwrite stream id:%d", (*paramIter)->getStreamId());
                mspvAppMetaStreamInfo.erase(memberIter);
                break;
            }
        }
        mspvAppMetaStreamInfo.push_back(*paramIter);
    }
    for( paramIter = halMetaStreamInfos.begin() ; paramIter != halMetaStreamInfos.end() ; paramIter++ )
    {
        for( memberIter = mspvHalMetaStreamInfo.begin() ; memberIter != mspvHalMetaStreamInfo.end() ; memberIter++ )
        {
            if( (*paramIter)->getStreamId() == (*memberIter)->getStreamId() )
            {
                MY_LOGW("overwrite stream id:%d", (*paramIter)->getStreamId());
                mspvHalMetaStreamInfo.erase(memberIter);
                break;
            }
        }
        mspvHalMetaStreamInfo.push_back(*paramIter);
    }

    //dump meta stream infos
    int i = 0;
    for( memberIter = mspvAppMetaStreamInfo.begin() ; memberIter != mspvAppMetaStreamInfo.end() ; memberIter++ )
    {
        MY_LOGD2("App-i:%d, name:%s, id:%d, type:%d, max:%d, min:%d", i,
                                                                      (*memberIter)->getStreamName(),
                                                                      (*memberIter)->getStreamId(),
                                                                      (*memberIter)->getStreamType(),
                                                                      (*memberIter)->getMaxBufNum(),
                                                                      (*memberIter)->getMinInitBufNum());
        i++;
    }
    i = 0;
    for( memberIter = mspvHalMetaStreamInfo.begin() ; memberIter != mspvHalMetaStreamInfo.end() ; memberIter++ )
    {
        MY_LOGD2("Hal-i:%d, name:%s, id:%d, type:%d, max:%d, min:%d", i,
                                                                      (*memberIter)->getStreamName(),
                                                                      (*memberIter)->getStreamId(),
                                                                      (*memberIter)->getStreamType(),
                                                                      (*memberIter)->getMaxBufNum(),
                                                                      (*memberIter)->getMinInitBufNum());
        i++;
    }
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
addImageStream(vector<ImageStreamUsage> const vImageStreamUsage)
{
    vector<ImageStreamUsage>::const_iterator paramIter;
    vector<ImageStreamUsage>::iterator memberIter;

    //update image stream infos
    for( paramIter = vImageStreamUsage.begin() ; paramIter != vImageStreamUsage.end() ; paramIter++ )
    {
        for( memberIter = mvImageStreamUsage.begin() ; memberIter != mvImageStreamUsage.end() ; memberIter++ )
        {
            if( (*paramIter).spImageStreamInfo->getStreamId() == (*memberIter).spImageStreamInfo->getStreamId() )
            {
                MY_LOGW("overwrite stream id:%d", (*paramIter).spImageStreamInfo->getStreamId());
                mvImageStreamUsage.erase(memberIter);
                break;
            }
        }
        mvImageStreamUsage.push_back(*paramIter);
    }

    //dump image stream infos
    //TBD

    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
addNode(
    IPipelineNode::NodeId_T const nodeId,
    char* const nodeName,
    vector<NodeStreamUsage> const vNodeStreamUsage)
{
    vector<NodeSetting>::iterator memberIter;

    //update node setting
    for( memberIter = mvNodeSetting.begin() ; memberIter != mvNodeSetting.end() ; memberIter++ )
    {
        if( nodeId == (*memberIter).nodeId )
        {
            MY_LOGW("overwrite node id:%d", (*memberIter).nodeId);
            mvNodeSetting.erase(memberIter);
            break;
        }
    }
    NodeSetting setting;
    setting.nodeId = nodeId;
    setting.nodeName = nodeName;
    setting.vNodeStreamUsage.clear();
    setting.vNodeStreamUsage = vNodeStreamUsage;
    mvNodeSetting.push_back(setting);

    //dump node setting
    //TBD

    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
setFlow(
    IPipelineNode::NodeId_T const rootNodeId,
    vector<NodeEdge> const vNodeEdge)
{
    //update flow setting
    mRootNodeId = rootNodeId;
    mvNodeEdge.clear();
    mvNodeEdge = vNodeEdge;

    //dump flow setting
    MY_LOGD2("root node id:%d", mRootNodeId);
    vector<NodeEdge>::iterator memberIter;
    for( memberIter = mvNodeEdge.begin() ; memberIter != mvNodeEdge.end() ; memberIter++ )
    {
        MY_LOGD2("node edge:(%d,%d)", (*memberIter).startNodeId, (*memberIter).endNodeId);
    }
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
setScenarioControl(android::sp<IScenarioControl> const pControl)
{
    if( pControl == NULL )
    {
        MY_LOGW("pControl is NULL, set scenario control failed.");
        return BAD_VALUE;
    }
    mspScenarioControl = pControl;
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
create(
    sp<LegacyPipelineManager> const mgr,
    MINT32& pipelineId,
    wp<ILegacyPipeline>& legacyPipeline)
{
    //check setting
    MERROR err = checkSetting();
    if( err != OK )
    {
        MY_LOGW("check setting fail, LegacyPipeline not created");
        return err;
    }

    //create LegacyPipeline
    sp<ILegacyPipeline> pipe = new LegacyPipeline();
    if( pipe == NULL )
    {
        MY_LOGW("create LegacyPipeline fail, LegacyPipeline not created");
        return UNKNOWN_ERROR;
    }

    //build result processor
    sp<ResultProcessor> spResultProcessor = ResultProcessor::createInstance();
    if( spResultProcessor.get() == NULL )
    {
        MY_LOGW("create ResultProcessor fail, LegacyPipeline not created");
        return UNKNOWN_ERROR;
    }

    //build stream providers
    vector<sp<StreamBufferProvider> > spvBufferProvider;
    {
        vector<ImageStreamUsage>::iterator streamIter;
        sp<StreamBufferProvider> spBufferProvider;
        for( streamIter = mvImageStreamUsage.begin() ; streamIter != mvImageStreamUsage.end() ; streamIter++ )
        {
            spBufferProvider = NULL;
            if( (*streamIter).useImgHalPool )
            {
                MY_LOGD2("use image hal pool:%d", (*streamIter).spImageStreamInfo->getStreamId());
                continue;
            }
            if( (*streamIter).isConsumer )
            {
                spBufferProvider = mgr->getConsumerBufferProvider((*streamIter).providerStreamId);
                if( spBufferProvider == NULL )
                {
                    MY_LOGE("cannot get consumer stream buffer provider, provider stream id:%d", (*streamIter).providerStreamId);
                    return UNKNOWN_ERROR;
                }
                spBufferProvider->setImageStreamInfo((*streamIter).spImageStreamInfo);
                if( (*streamIter).spCallback != NULL )
                {
                    spBufferProvider->setImageCallback((*streamIter).spCallback);
                }
                spvBufferProvider.push_back(spBufferProvider);
                continue;
            }

            sp<StreamBufferProviderFactory> providerFactory = StreamBufferProviderFactory::createInstance();
            providerFactory->setImageStreamInfo((*streamIter).spImageStreamInfo);
            if( (*streamIter).spProvider != NULL )
            {
                providerFactory->setCamClient((*streamIter).spProvider);
            }
            if( (*streamIter).spCallback != NULL )
            {
                providerFactory->setBufferCallback((*streamIter).spCallback);
            }
            vector<sp<IImageBuffer> >::iterator bufIter;
            for( bufIter = (*streamIter).spvBuffers.begin() ; bufIter != (*streamIter).spvBuffers.end() ; bufIter++ )
            {
                //providerFactory->setUsersBuffer(*bufIter); //need StreamBufferProviderFactory to change API
            }
            if( (*streamIter).bufferQueueDepth > 0 )
            {
                providerFactory->setBufferQueueDepth((*streamIter).bufferQueueDepth);
            }
            Vector<sp<StreamBufferProvider> > vNewProvider;
            providerFactory->create(vNewProvider);
            vector<sp<StreamBufferProvider> >::iterator newProviderIter;
            int i = 0;
            for( newProviderIter = vNewProvider.begin() ; newProviderIter != vNewProvider.end() ; newProviderIter++ )
            {
                MY_LOGD2("create stream buffer provider:%d, stream id:%d, isConsumer:%d",
                            i,
                            (*streamIter).spImageStreamInfo->getStreamId(),
                            (*newProviderIter)->isComsumer()
                        );
                if( (*newProviderIter)->isComsumer() )
                {
                    mgr->registerConsumerBufferProvider(*newProviderIter);
                }
                else
                {
                    spvBufferProvider.push_back(*newProviderIter);
                }
                i++;
            }
        }
    }

    //build pipeline context
    //0. create instance
    sp<PipelineContext>spPipelineContext = PipelineContext::create(mPipeName);
    if( spPipelineContext.get() == NULL )
    {
        MY_LOGW("create PipelineContext fail, LegacyPipeline not created");
        return UNKNOWN_ERROR;
    }
    spPipelineContext->beginConfigure();

    //1.config stream
    {
        vector<sp<IMetaStreamInfo> >::iterator streamIter;
        for( streamIter = mspvAppMetaStreamInfo.begin() ; streamIter != mspvAppMetaStreamInfo.end() ; streamIter++ )
        {
            StreamBuilder(eStreamType_META_APP, *streamIter)
                .build(spPipelineContext);
        }
    }
    {
        vector<sp<IMetaStreamInfo> >::iterator streamIter;
        for( streamIter = mspvHalMetaStreamInfo.begin() ; streamIter != mspvHalMetaStreamInfo.end() ; streamIter++ )
        {
            StreamBuilder(eStreamType_META_HAL, *streamIter)
                .build(spPipelineContext);
        }
    }
    {
        vector<ImageStreamUsage>::iterator streamIter;
        for( streamIter = mvImageStreamUsage.begin() ; streamIter != mvImageStreamUsage.end() ; streamIter++ )
        {
            if( (*streamIter).useImgHalPool )
            {
                StreamBuilder(eStreamType_IMG_HAL_POOL, (*streamIter).spImageStreamInfo)
                    .build(spPipelineContext);
            }
            else
            {
                sp<StreamBufferProvider> bufferProvider;
                bufferProvider = getStreamBufferProvider((*streamIter).spImageStreamInfo->getStreamId(), spvBufferProvider);
                StreamBuilder(eStreamType_IMG_HAL_PROVIDER, (*streamIter).spImageStreamInfo)
                    .setProvider((sp<IStreamBufferProvider>)bufferProvider)
                    .build(spPipelineContext);
            }
        }
    }

    //2.config node
    vector<NodeSetting>::iterator nodeSettingIter;
    for( nodeSettingIter = mvNodeSetting.begin() ; nodeSettingIter < mvNodeSetting.end() ; nodeSettingIter++ )
    {
        switch( (*nodeSettingIter).nodeId )
        {
            case eNODEID_P1Node:
            {
                P1Node::ConfigParams cfgParam;
                buildP1NodeConfig(*nodeSettingIter, spPipelineContext, &cfgParam);
                buildNode<P1Node>(*nodeSettingIter, &cfgParam, spPipelineContext);
                break;
            }
            case eNODEID_P2Node:
            {
                P2Node::ConfigParams cfgParam;
                buildP2NodeConfig(*nodeSettingIter, &cfgParam);
                buildNode<P2Node>(*nodeSettingIter, &cfgParam, spPipelineContext);
                break;
            }
            case eNODEID_JpegNode:
            {
                JpegNode::ConfigParams cfgParam;
                buildJpegNodeConfig(*nodeSettingIter, &cfgParam);
                buildNode<JpegNode>(*nodeSettingIter, &cfgParam, spPipelineContext);
                break;
            }
            default:
                MY_LOGE("un-support node id:%d", (*nodeSettingIter).nodeId);
        }
    }

    //3.config pipe
    PipelineBuilder pipeBuilder = PipelineBuilder();
    pipeBuilder = pipeBuilder.setRootNode(
                                  NodeSet().add(mRootNodeId)
                              );
    vector<NodeEdge>::iterator nodeEdgeIter;
    for( nodeEdgeIter = mvNodeEdge.begin() ; nodeEdgeIter < mvNodeEdge.end() ; nodeEdgeIter++ )
    {
        pipeBuilder = pipeBuilder.setNodeEdges(
                                      NodeEdgeSet()
                                          .addEdge((*nodeEdgeIter).startNodeId, (*nodeEdgeIter).endNodeId)
                                  );
    }
    err = pipeBuilder.build(spPipelineContext);
    if( err != OK ) {
        MY_LOGE("build pipeline error");
        return err;
    }

    //4.config end
    spPipelineContext->endConfigure();
    spPipelineContext->setScenarioControl(mspScenarioControl);

    //set to LegacyPipeline
    pipe->setResultProcessor(spResultProcessor);
    pipe->setBufferProvider(spvBufferProvider);
    pipe->setPipelineContext(spPipelineContext);

    //register to LegacyPipelineManager
    pipelineId = mgr->registerLegacyPipeline(legacyPipeline.promote());

    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MERROR
LegacyPipelineBuilder::
checkSetting()
{
    //check all nodes in flow exist
    //TBD

    //check all streams in node setting exist
    //TBD

    return OK;
}


/******************************************************************************
*
*******************************************************************************/
template <class _Node_>
MERROR
LegacyPipelineBuilder::
buildNode(NodeSetting const setting, typename _Node_::ConfigParams* const cfgParam, sp<PipelineContext> spPipelineContext)
{
    MY_LOGD1("Nodebuilder id:%d, name:%s, +", setting.nodeId, setting.nodeName);

    typedef _Node_                  NodeT;
    typedef NodeActor< NodeT >      MyNodeActorT;

    typename NodeT::InitParams initParam;
    initParam.openId = mSensorId;
    initParam.nodeId = setting.nodeId;
    initParam.nodeName = setting.nodeName;
    MY_LOGD2("jpeg initParam, openId=%d, nodeId=%d, name=%s", initParam.openId,
                                                              initParam.nodeId,
                                                              initParam.nodeName);

    sp<MyNodeActorT> pNode = new MyNodeActorT( NodeT::createInstance() );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(*cfgParam);

    //TBD: need to review meta/image streams
    //which stream id is APP meta in??
    StreamSet streamSetIn = StreamSet();
    StreamSet streamSetOut = StreamSet();
    vector<NodeStreamUsage>::const_iterator iter;
    for( iter = setting.vNodeStreamUsage.begin() ; iter != setting.vNodeStreamUsage.end() ; iter++ )
    {
        switch( setting.nodeId )
        {
            case eNODEID_P1Node:
                switch( (*iter).streamId )
                {
                    #if 0
                    case xxx:
                        MY_LOGD2("node id:%d, name:%s, streamSetIn add %d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        streamSetIn = streamSetIn.add((*iter).streamId);
                        break;
                    #endif
                    case eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00:
                    case eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00:
                    case eSTREAMID_META_PIPE_DYNAMIC_01:
                    case eSTREAMID_META_APP_DYNAMIC_01:
                        MY_LOGD2("node id:%d, name:%s, streamSetOut add %d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        streamSetOut = streamSetOut.add((*iter).streamId);
                    default:
                        MY_LOGW("stream IO not set:%d", (*iter).streamId);
                        break;
                }
                break;
            case eNODEID_P2Node:
                switch( (*iter).streamId )
                {
                    case eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00:
                    case eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00:
                    case eSTREAMID_META_PIPE_DYNAMIC_01:
                        MY_LOGD2("node id:%d, name:%s, streamSetIn add %d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        streamSetIn = streamSetIn.add((*iter).streamId);
                        break;
                    case eSTREAMID_IMAGE_PIPE_YUV_JPEG_00:
                    case eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00:
                    case eSTREAMID_META_PIPE_DYNAMIC_02:
                    case eSTREAMID_META_APP_DYNAMIC_02:
                        MY_LOGD2("node id:%d, name:%s, streamSetOut add %d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        streamSetOut = streamSetOut.add((*iter).streamId);
                        break;
                    default:
                        MY_LOGW("stream IO not set:%d", (*iter).streamId);
                        break;
                }
                break;
            case eNODEID_JpegNode:
                switch( (*iter).streamId )
                {
                    case eSTREAMID_IMAGE_PIPE_YUV_JPEG_00:
                    case eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00:
                    case eSTREAMID_META_PIPE_DYNAMIC_02:
                        MY_LOGD2("node id:%d, name:%s, streamSetIn add %d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        streamSetIn = streamSetIn.add((*iter).streamId);
                        break;
                    case eSTREAMID_IMAGE_JPEG:
                    case eSTREAMID_META_APP_DYNAMIC_JPEG:
                        MY_LOGD2("node id:%d, name:%s, streamSetOut add %d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        streamSetOut = streamSetOut.add((*iter).streamId);
                        break;
                    default:
                        MY_LOGW("node id:%d, name:%s, stream IO not set:%d", setting.nodeId, setting.nodeName, (*iter).streamId);
                        break;
                }
                break;
            default:
                MY_LOGE("un-support node:%d", setting.nodeId);
        }
    }
    NodeBuilder nodeBuilder = NodeBuilder( setting.nodeId, pNode)
                                .addStream( NodeBuilder::eDirection_IN, streamSetIn)
                                .addStream( NodeBuilder::eDirection_OUT, streamSetOut);

    //TBD: need to review image streams
    for( iter = setting.vNodeStreamUsage.begin() ; iter != setting.vNodeStreamUsage.end() ; iter++ )
    {
        switch( setting.nodeId )
        {
            case eNODEID_P1Node:
                switch( (*iter).streamId )
                {
                    case eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00:
                    case eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00:
                        MY_LOGD2("jpeg setImageStreamUsage %d-0x%x", (*iter).streamId, (*iter).memoryUsage);
                        nodeBuilder = nodeBuilder.setImageStreamUsage((*iter).streamId, (*iter).memoryUsage);
                        break;
                    default:
                        MY_LOGD("un-support id:%d", (*iter).streamId);
                        break;
                }
                break;
            case eNODEID_P2Node:
                switch( (*iter).streamId )
                {
                    case eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00:
                    case eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00:
                    case eSTREAMID_IMAGE_PIPE_YUV_JPEG_00:
                    case eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00:
                    case eSTREAMID_IMAGE_FD:
                        MY_LOGD2("jpeg setImageStreamUsage %d-0x%x", (*iter).streamId, (*iter).memoryUsage);
                        nodeBuilder = nodeBuilder.setImageStreamUsage((*iter).streamId, (*iter).memoryUsage);
                        break;
                    default:
                        MY_LOGD("un-support id:%d", (*iter).streamId);
                        break;
                }
                break;
            case eNODEID_JpegNode:
                switch( (*iter).streamId )
                {
                    case eSTREAMID_IMAGE_PIPE_YUV_JPEG_00:
                    case eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00:
                    case eSTREAMID_IMAGE_JPEG:
                        MY_LOGD2("jpeg setImageStreamUsage %d-0x%x", (*iter).streamId, (*iter).memoryUsage);
                        nodeBuilder = nodeBuilder.setImageStreamUsage((*iter).streamId, (*iter).memoryUsage);
                        break;
                    default:
                        MY_LOGD("un-support id:%d", (*iter).streamId);
                        break;
                }
                break;
            default:
                MY_LOGE("un-support node:%d", setting.nodeId);
        }
    }
    MERROR ret = nodeBuilder.build(spPipelineContext);
    MY_LOGD1("Nodebuilder id:%d, name:%s, -", setting.nodeId, setting.nodeName);

    if( ret != OK ) {
        MY_LOGE("build node id:%d, name:%s error", setting.nodeId, setting.nodeName);
        return ret;
    }

    return OK;
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipelineBuilder::
buildP1NodeConfig(NodeSetting const setting, sp<PipelineContext> const spPipelineContext, P1Node::ConfigParams* cfgParam)
{
    MY_LOGD1("build P1Node Config +");

    (*cfgParam).pvOutImage_full.clear();
    (*cfgParam).pOutImage_resizer = NULL;
    (*cfgParam).pStreamPool_full = NULL;
    (*cfgParam).pStreamPool_resizer = NULL;

    P1Node::SensorParams sensorParam;
    sensorParam.mode = mSensorParam.mode;
    sensorParam.size.w = mSensorParam.size.w;
    sensorParam.size.h = mSensorParam.size.h;
    sensorParam.fps = mSensorParam.fps;
    sensorParam.pixelMode = mSensorParam.pixelmode;
    (*cfgParam).sensorParams = sensorParam;

    //TBD: need to review in meta stream id
    vector<NodeStreamUsage>::const_iterator iter;
    for( iter = setting.vNodeStreamUsage.begin() ; iter != setting.vNodeStreamUsage.end() ; iter++ )
    {
        switch( (*iter).streamId )
        {
            #if 0
            case xxx:
                (*cfgParam).pInAppMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case xxx:
                (*cfgParam).pInHalMeta = getMetaStreamInfo((*iter).streamId);
                break;
            #endif
            case eSTREAMID_META_APP_DYNAMIC_01:
                (*cfgParam).pOutAppMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_META_PIPE_DYNAMIC_01:
                (*cfgParam).pOutHalMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00:
                (*cfgParam).pvOutImage_full.push_back(getImageStreamInfo((*iter).streamId));
                if( getImageStreamType((*iter).streamId) == eStreamType_IMG_HAL_POOL )
                {
                    (*cfgParam).pStreamPool_full = spPipelineContext->queryImageStreamPool((*iter).streamId);
                }
                break;
            case eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00:
                (*cfgParam).pOutImage_resizer = getImageStreamInfo((*iter).streamId);
                if( getImageStreamType((*iter).streamId) == eStreamType_IMG_HAL_POOL )
                {
                    (*cfgParam).pStreamPool_resizer = spPipelineContext->queryImageStreamPool((*iter).streamId);
                }
                break;
            default:
                MY_LOGW("un-support id:%d", (*iter).streamId);
                break;
        }
    }
    MY_LOGD2("p1 cfgParam, InAppMetaId=%d, InHalMetaId=%d, OutAppMetaId=%d, OutHalMetaId=%d",
                    (*cfgParam).pInAppMeta->getStreamId(),
                    (*cfgParam).pInHalMeta->getStreamId(),
                    (*cfgParam).pOutAppMeta->getStreamId(),
                    (*cfgParam).pOutHalMeta->getStreamId());
    if((*cfgParam).pOutImage_resizer != NULL)
    {
        MY_LOGD1("p1 cfgParam, OutRrzoImageId=%d",
                (*cfgParam).pOutImage_resizer->getStreamId());
    }
    if((*cfgParam).pvOutImage_full.size() > 0)
    {
        MY_LOGD1("p1 cfgParam, OutImgoSize=%d, 0:OutImgoImageId=%d",
                (*cfgParam).pvOutImage_full.size(),
                ((*cfgParam).pvOutImage_full[0])->getStreamId());
    }
    MY_LOGD1("Nodebuilder p1 -");

    return;
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipelineBuilder::
buildP2NodeConfig(NodeSetting const setting, P2Node::ConfigParams* cfgParam)
{
    MY_LOGD1("Nodebuilder p2 +");

    typedef P2Node                  NodeT;

    (*cfgParam).pvInFullRaw.clear();
    (*cfgParam).pInResizedRaw = NULL;
    (*cfgParam).vOutImage.clear();
    (*cfgParam).pOutFDImage = NULL;

    //TBD: need to review in meta stream id
    vector<NodeStreamUsage>::const_iterator iter;
    for( iter = setting.vNodeStreamUsage.begin() ; iter != setting.vNodeStreamUsage.end() ; iter++ )
    {
        switch( (*iter).streamId )
        {
            #if 0
            case xxx:
                (*cfgParam).pInAppMeta = getMetaStreamInfo((*iter).streamId);
                break;
            #endif
            case eSTREAMID_META_PIPE_DYNAMIC_01:
                (*cfgParam).pInHalMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_META_APP_DYNAMIC_02:
                (*cfgParam).pOutAppMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_META_PIPE_DYNAMIC_02:
                (*cfgParam).pOutHalMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00:
                (*cfgParam).pvInFullRaw.push_back(getImageStreamInfo((*iter).streamId));
                break;
            case eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00:
                (*cfgParam).pInResizedRaw = getImageStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_IMAGE_PIPE_YUV_JPEG_00:
            case eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00:
                (*cfgParam).vOutImage.push_back(getImageStreamInfo((*iter).streamId));
                break;
            case eSTREAMID_IMAGE_FD:
                (*cfgParam).pOutFDImage = getImageStreamInfo((*iter).streamId);
                break;
            default:
                MY_LOGW("un-support id:%d", (*iter).streamId);
                break;
        }
    }
    MY_LOGD2("p2 cfgParam, InAppMetaId=%d, InHalMetaId=%d, OutAppMetaId=%d, OutHalMetaId=%d",
                     (*cfgParam).pInAppMeta->getStreamId(),
                     (*cfgParam).pInHalMeta->getStreamId(),
                     (*cfgParam).pOutAppMeta->getStreamId(),
                     (*cfgParam).pOutHalMeta->getStreamId());
    if((*cfgParam).pInResizedRaw != NULL)
    {
        MY_LOGD1("p2 cfgParam, InRrzoImageId=%d",
                 (*cfgParam).pInResizedRaw->getStreamId());
    }
    if((*cfgParam).pvInFullRaw.size() > 0)
    {
        MY_LOGD1("p2 cfgParam, InImgoSize=%d, 0:InImgoImageId=%d",
                 (*cfgParam).pvInFullRaw.size(),
                 ((*cfgParam).pvInFullRaw[0])->getStreamId());
    }
    if((*cfgParam).pOutFDImage != NULL)
    {
        MY_LOGD1("p2 cfgParam, OutFDImageId=%d",
                 (*cfgParam).pOutFDImage->getStreamId());
    }
    {
        int i = 0;
        vector<sp<IImageStreamInfo> >::iterator iter;
        for( iter = (*cfgParam).vOutImage.begin() ; iter < (*cfgParam).vOutImage.end() ; iter++ )
        {
            MY_LOGD2("p2 cfgParam, OutImage=%d, Id=%d", i, (*iter)->getStreamId());
            i++;
        }
    }
    MY_LOGD1("Nodebuilder p2 -");

    return;
}


/******************************************************************************
*
*******************************************************************************/
MVOID
LegacyPipelineBuilder::
buildJpegNodeConfig(NodeSetting const setting, JpegNode::ConfigParams* cfgParam)
{
    MY_LOGD1("Nodebuilder jpeg +");

    typedef JpegNode                NodeT;

    (*cfgParam).pInYuv_Main= NULL;
    (*cfgParam).pInYuv_Thumbnail= NULL;
    (*cfgParam).pOutJpeg = NULL;

    //TBD: need to review in meta stream id
    vector<NodeStreamUsage>::const_iterator iter;
    for( iter = setting.vNodeStreamUsage.begin() ; iter != setting.vNodeStreamUsage.end() ; iter++ )
    {
        switch( (*iter).streamId )
        {
            #if 0
            case xxx:
                (*cfgParam).pInAppMeta = getMetaStreamInfo((*iter).streamId);
                break;
            #endif
            case eSTREAMID_META_PIPE_DYNAMIC_02:
                (*cfgParam).pInHalMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_META_APP_DYNAMIC_JPEG:
                (*cfgParam).pOutAppMeta = getMetaStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_IMAGE_PIPE_YUV_JPEG_00:
                (*cfgParam).pInYuv_Main = getImageStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00:
                (*cfgParam).pInYuv_Thumbnail = getImageStreamInfo((*iter).streamId);
                break;
            case eSTREAMID_IMAGE_JPEG:
                (*cfgParam).pOutJpeg = getImageStreamInfo((*iter).streamId);
                break;
            default:
                MY_LOGW("un-support id:%d", (*iter).streamId);
                break;
        }
    }
    MY_LOGD2("jpeg cfgParam, InAppMetaId=%d, InHalMetaId=%d, OutAppMetaId=%d",
                (*cfgParam).pInAppMeta->getStreamId(),
                (*cfgParam).pInHalMeta->getStreamId(),
                (*cfgParam).pOutAppMeta->getStreamId());
    MY_LOGD2("jpeg cfgParam, InMainYuvId=%d, InThumbYuvId=%d, OutJpegId=%d",
                (*cfgParam).pInYuv_Main->getStreamId(),
                (*cfgParam).pInYuv_Thumbnail->getStreamId(),
                (*cfgParam).pOutJpeg->getStreamId());
    MY_LOGD1("Nodebuilder jpeg -");

    return;
}


/******************************************************************************
*
*******************************************************************************/
sp<IMetaStreamInfo>
LegacyPipelineBuilder::
getMetaStreamInfo(StreamId_T const streamId)
{
    vector<sp<IMetaStreamInfo> >::iterator memberIter;

    for( memberIter = mspvAppMetaStreamInfo.begin() ; memberIter != mspvAppMetaStreamInfo.end() ; memberIter++ )
    {
        if( streamId == (*memberIter)->getStreamId() )
        {
            return *memberIter;
        }
    }
    for( memberIter = mspvHalMetaStreamInfo.begin() ; memberIter != mspvHalMetaStreamInfo.end() ; memberIter++ )
    {
        if( streamId == (*memberIter)->getStreamId() )
        {
            return *memberIter;
        }
    }
    MY_LOGW("meta stream info not found:%d", streamId);
    return NULL;
}


/******************************************************************************
*
*******************************************************************************/
sp<IImageStreamInfo>
LegacyPipelineBuilder::
getImageStreamInfo(StreamId_T const streamId)
{
    vector<ImageStreamUsage>::iterator memberIter;

    for( memberIter = mvImageStreamUsage.begin() ; memberIter != mvImageStreamUsage.end() ; memberIter++ )
    {
        if( streamId == (*memberIter).spImageStreamInfo->getStreamId() )
        {
             return (*memberIter).spImageStreamInfo;
        }
    }
    MY_LOGW("image stream info not found:%d", streamId);
    return NULL;
}


/******************************************************************************
*
*******************************************************************************/
sp<StreamBufferProvider>
LegacyPipelineBuilder::
getStreamBufferProvider(StreamId_T const streamId, vector<sp<StreamBufferProvider> > const spvStreamBufferProvider)
{
    vector<sp<StreamBufferProvider> >::const_iterator providerIter;
    sp<IImageStreamInfo> imageStreamInfo;

    for( providerIter = spvStreamBufferProvider.begin() ; providerIter != spvStreamBufferProvider.end() ; providerIter++ )
    {
        (*providerIter)->queryImageStreamInfo(imageStreamInfo);
        if( streamId == imageStreamInfo->getStreamId() )
        {
             return *providerIter;
        }
    }
    MY_LOGW("stream buffer provider not found:%d", streamId);
    return NULL;
}


/******************************************************************************
*
*******************************************************************************/
MINT32
LegacyPipelineBuilder::
getImageStreamType(StreamId_T const streamId)
{
    vector<ImageStreamUsage>::iterator memberIter;

    for( memberIter = mvImageStreamUsage.begin() ; memberIter != mvImageStreamUsage.end() ; memberIter++ )
    {
        if( streamId == (*memberIter).spImageStreamInfo->getStreamId() )
        {
            if( (*memberIter).useImgHalPool )
            {
                 return eStreamType_IMG_HAL_POOL;
            }
            else
            {
                return eStreamType_IMG_HAL_PROVIDER;
            }
        }
    }
    MY_LOGW("image stream info not found:%d", streamId);
    return -1;
}

