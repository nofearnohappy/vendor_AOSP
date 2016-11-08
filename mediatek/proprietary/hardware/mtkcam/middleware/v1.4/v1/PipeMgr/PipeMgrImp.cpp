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

#define LOG_TAG "MtkCam/PipeMgr"

#include <list>
#include "MyUtils.h"
#include <hwutils/HwMisc.h>

#include <metadata/client/mtk_metadata_tag.h>

#include <v1/config/PriorityDefs.h>
#include <v1/IParamsManager.h>

#include <v3/pipeline/IPipelineDAG.h>
#include <v3/pipeline/IPipelineNode.h>
#include <v3/pipeline/IPipelineNodeMapControl.h>
#include <v3/pipeline/IPipelineFrameControl.h>
#include <v3/pipeline/PipelineContext.h>
#include <v3/pipeline/PipelineContextImpl.h>

#include <v3/utils/streambuf/StreamBufferPool.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>

#include <IHalSensor.h>

#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>
#include <v3/hwnode/JpegNode.h>

#include <imageio/ispio_utility.h>

#include <metadata/ITemplateRequest.h>
#include <metadata/IMetadataProvider.h>

using namespace NSCam;
using namespace v3;
using namespace NSCam::v3::Utils;
using namespace android;
using namespace NSCam::v3::NSPipelineContext;

#include <v1/StreamIDs.h>
using namespace NSMtkStreamId;
//
using namespace MtkCamUtils;
#include <v1/adapter/inc/ImgBufProvidersManager.h>
#include <v3/pipeline/IPipelineBufferSetFrameControl.h>
using namespace NSCam::v3;
#include <utils/include/ImageBufferHeap.h>
#include <metadata/IMetadata.h>
#include <v1/StreamBufferProviders/BufMgr.h>
using namespace NSMtkBufMgr;
#include <v1/PipeDataInfo.h>
using namespace NSMtkPipeDataInfo;
#include <v1/converter/RequestSettingBuilder.h>
#include <v1/PipeMgr/PipeMgr.h>
using namespace NSMtkPipeMgr;
//
#include "PipeMgrImp.h"
using namespace NSMtkPipeMgrImp;
//
/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImp::
PipeMgrImp(
    MUINT32     openId,
    EPipeScen   pipeScenario)
    : mOpenId(openId)
    , mPipeScenario(pipeScenario)
    , mLogLevel(0)
    , mbDumpInfo(MFALSE)
    , mPipeFlow(EPipeFlow_Default)
    , mContext(NULL)
    , mLoopCond()
    , mLoopCondStop()
    , mLoopLock()
    , mRequestCnt(getRequestNumMin())
    , mAllMetaStreamInfo(NULL)
    , mAllImageStreamInfo(NULL)
    , mpSensorHalObj(NULL)
{
    FUNC_NAME;
    //
    char cProperty[PROPERTY_VALUE_MAX] = {'\0'};
    //
    ::property_get("debug.camera.pipemgr.loglevel", cProperty, "2");
    mLogLevel = ::atoi(cProperty);
    MY_LOGD("debug.camera.pipemgr.loglevel=%s", cProperty);
    //
    ::property_get("debug.camera.pipemgr.dumpInfo", cProperty, "1");
    mbDumpInfo = ::atoi(cProperty);
    MY_LOGD1("debug.camera.pipemgr.dumpInfo=%s", cProperty);
    //
    mPipeMgrParams.spParamsMgr = NULL;
    mPipeMgrParams.spImgBufProvidersMgr = NULL;
    mPipeMgrParams.pBufMgr = NULL;
    mPipeMgrParams.pipeImgCb = NULL;
    mPipeMgrParams.pipeImgCbUser = NULL;
    mPipeMgrParams.spResultProcessor = NULL;
    mPipeMgrParams.spRequestSettingBuilder = NULL;
    //
    mlLoopCmd.clear();
    //
    FUNC_END;
}


/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImp::
~PipeMgrImp()
{
    FUNC_NAME;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
destroyInstance()
{
    FUNC_NAME;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
PipeMgrImp::
createPipe(PipeMgrParams* pPipeMgrParams)
{
    FUNC_START;

    mPipeMgrParams.spParamsMgr              = pPipeMgrParams->spParamsMgr;
    mPipeMgrParams.spImgBufProvidersMgr     = pPipeMgrParams->spImgBufProvidersMgr;
    mPipeMgrParams.pBufMgr                  = pPipeMgrParams->pBufMgr;
    mPipeMgrParams.pipeImgCb                = pPipeMgrParams->pipeImgCb;
    mPipeMgrParams.pipeImgCbUser            = pPipeMgrParams->pipeImgCbUser;
    mPipeMgrParams.spResultProcessor        = pPipeMgrParams->spResultProcessor;
    mPipeMgrParams.spRequestSettingBuilder  = pPipeMgrParams->spRequestSettingBuilder;

    mAllMetaStreamInfo = (sp<IMetaStreamInfo>*)malloc(sizeof(sp<IMetaStreamInfo>)*EMetaInfo_Amount);
    memset(mAllMetaStreamInfo, 0, sizeof(sp<IMetaStreamInfo>)*EMetaInfo_Amount);
    mAllImageStreamInfo = (sp<IImageStreamInfo>*)malloc(sizeof(sp<IImageStreamInfo>)*EImageInfo_Amount);
    memset(mAllImageStreamInfo, 0, sizeof(sp<IImageStreamInfo>)*EImageInfo_Amount);

    //directly return if not neccessary
    prepareSensor();//sensorParam, todo : preview size...
    prepareConfiguration();//rrzo size, todo : MetadataProvider , TemplateRequestManager...

    setupStreamBufferProvider();

    setupPipelineContext();

    updateStreamBufferProvider();

    //setupSubmitThread();

    run();

    FUNC_END;
    return MTRUE;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
PipeMgrImp::
destroyPipe()
{
    FUNC_START;
    //finishSubmitThread();

    finishPipelineContext();
    mContext = NULL;

    closeSensor();

    finishStreamBufferProvider();

    free(mAllMetaStreamInfo);
    free(mAllImageStreamInfo);

    {
        Mutex::Autolock _l(mLoopLock);
        mlLoopCmd.push_back(ELoopCmd_Exit);
        requestExit();
        MY_LOGD1("wake up thread to exit");
        mLoopCond.broadcast();
    }
    MY_LOGD1("join");
    join();
    mlLoopCmd.clear();
    FUNC_END;
    return MTRUE;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<ImageStreamInfo>
PipeMgrImp::
createImageStreamInfo(
    char const*         streamName,
    StreamId_T          streamId,
    MUINT32             streamType,
    size_t              maxBufNum,
    size_t              minInitBufNum,
    MUINT               usageForAllocator,
    MINT                imgFormat,
    MSize const&        imgSize,
    MUINT32             transform
)
{
    IImageStreamInfo::BufPlanes_t bufPlanes;
#define addBufPlane(planes, height, stride)                                      \
        do{                                                                      \
            size_t _height = (size_t)(height);                                   \
            size_t _stride = (size_t)(stride);                                   \
            IImageStreamInfo::BufPlane bufPlane= { _height * _stride, _stride }; \
            planes.push_back(bufPlane);                                          \
        }while(0)
    switch( imgFormat ) {
        case eImgFmt_YV12:
            addBufPlane(bufPlanes , imgSize.h      , imgSize.w);
            addBufPlane(bufPlanes , imgSize.h >> 1 , imgSize.w >> 1);
            addBufPlane(bufPlanes , imgSize.h >> 1 , imgSize.w >> 1);
            break;
        case eImgFmt_NV21:
            addBufPlane(bufPlanes , imgSize.h      , imgSize.w);
            addBufPlane(bufPlanes , imgSize.h >> 1 , imgSize.w);
            break;
        case eImgFmt_YUY2:
            addBufPlane(bufPlanes , imgSize.h      , imgSize.w << 1);
            break;
        case eImgFmt_JPEG:
            {
                size_t const thumbnailsize = 160 * 128; //FIXME: temp solution
                size_t bufsize = (size_t)imgSize.w * imgSize.h;
                if( bufsize > thumbnailsize ) // to make sure buffer is large enough for thumbnail
                {
                    bufsize = bufsize * 6 / 5; //jpeg compression ratio
                }
                else
                {
                    bufsize = bufsize * 2;
                }
                IImageStreamInfo::BufPlane blobBufPlane= { bufsize, bufsize};
                bufPlanes.push_back(blobBufPlane);
                MY_LOGD("in createImageStreamInfo (w,h)=(%d,%d), bufsize=%d", imgSize.w, imgSize.h, bufsize);
            }
            break;
        default:
            MY_LOGE("format not support yet %p", imgFormat);
            break;
    }
#undef  addBufPlane

    sp<ImageStreamInfo>
        pStreamInfo = new ImageStreamInfo(
                streamName,
                streamId,
                streamType,
                maxBufNum, minInitBufNum,
                usageForAllocator, imgFormat, imgSize, bufPlanes, transform
                );
    MY_LOGD1("in createImageStreamInfo pStreamInfo : usage:0x%x, size:%d, stride:%d",
            pStreamInfo->getUsageForAllocator(),
            (pStreamInfo->getBufPlanes())[0].sizeInBytes,
            (pStreamInfo->getBufPlanes())[0].rowStrideInBytes);

    if( pStreamInfo == NULL ) {
        MY_LOGE("create ImageStream failed, %s, %#"PRIxPTR,
                streamName, streamId);
    }

    return pStreamInfo;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<ImageStreamInfo>
PipeMgrImp::
createRawImageStreamInfo(
    char const*         streamName,
    StreamId_T          streamId,
    MUINT32             streamType,
    size_t              maxBufNum,
    size_t              minInitBufNum,
    MUINT               usageForAllocator,
    MINT                imgFormat,
    MSize const&        imgSize,
    size_t const        stride
)
{
    IImageStreamInfo::BufPlanes_t bufPlanes;
    //
#define addBufPlane(planes, height, stride)                                      \
        do{                                                                      \
            size_t _height = (size_t)(height);                                   \
            size_t _stride = (size_t)(stride);                                   \
            IImageStreamInfo::BufPlane bufPlane= { _height * _stride, _stride }; \
            planes.push_back(bufPlane);                                          \
        }while(0)
    switch( imgFormat ) {
        case eImgFmt_BAYER10:
        case eImgFmt_FG_BAYER10:
            addBufPlane(bufPlanes , imgSize.h, stride);
            break;
        default:
            MY_LOGE("format not support yet %p", imgFormat);
            break;
    }
#undef  addBufPlane

    sp<ImageStreamInfo>
        pStreamInfo = new ImageStreamInfo(
                streamName,
                streamId,
                streamType,
                maxBufNum, minInitBufNum,
                usageForAllocator, imgFormat, imgSize, bufPlanes
                );

    if( pStreamInfo == NULL ) {
        MY_LOGE("create ImageStream failed, %s, %#"PRIxPTR,
                streamName, streamId);
    }

    return pStreamInfo;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupPipelineContext()
{
    mContext = PipelineContext::create("test");
    if( !mContext.get() ) {
        MY_LOGE("cannot create context");
        return;
    }
    //
    mContext->beginConfigure();

    setupPipelineStreamInfo();
    dumpPipelineStreamInfo();

    setupPipelineStreamConfig();
    dumpPipelineStreamConfig();
    //
    // 1. Streams ***************
    //
    // 1.a. check if stream exist
    // 1.b. setup streams
    setupPipelineStream();
    //
    // 2. Nodes   ***************
    //
    // 2.a. check if node exist
    // 2.b. setup nodes
    //
    setupPipelineNode();
    //
    // 3. Pipeline **************
    //
    setupPipelineFlow();
    //
    mContext->endConfigure();
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
dumpPipelineStreamInfo()
{
    if(!mbDumpInfo)
    {
        return;
    }

    MY_LOGD1("dump meta stream info");
    int i = 0;
    for( i = 0 ; i < EMetaInfo_Amount ; i++ )
    {
        if( mAllMetaStreamInfo[i]  == NULL )
        {
            continue;
        }
        MY_LOGD1("i=%d, name=%s, id=0x%x, type=%d, maxBuf=%d, minInitBuf=%d",
                i,
                mAllMetaStreamInfo[i]->getStreamName(),
                mAllMetaStreamInfo[i]->getStreamId(),
                mAllMetaStreamInfo[i]->getStreamType(),
                mAllMetaStreamInfo[i]->getMaxBufNum(),
                mAllMetaStreamInfo[i]->getMinInitBufNum());
    }
    MY_LOGD1("dump image stream info");
    for( i = 0 ; i < EImageInfo_Amount ; i++ )
    {
        if( mAllImageStreamInfo[i]  == NULL )
        {
            continue;
        }
        MY_LOGD1("i=%d, name=%s, id=0x%x, type=%d, maxBuf=%d, minInitBuf=%d, usageConsumer=%d, usageAlloc=%d",
                i,
                mAllImageStreamInfo[i]->getStreamName(),
                mAllImageStreamInfo[i]->getStreamId(),
                mAllImageStreamInfo[i]->getStreamType(),
                mAllImageStreamInfo[i]->getMaxBufNum(),
                mAllImageStreamInfo[i]->getMinInitBufNum(),
                mAllImageStreamInfo[i]->getUsageForConsumer(),
                mAllImageStreamInfo[i]->getUsageForAllocator());
        MY_LOGD1("i=%d, format=%d, w=%d, h=%d, planes=%d, transform=%d",
                i,
                mAllImageStreamInfo[i]->getImgFormat(),
                mAllImageStreamInfo[i]->getImgSize().w,
                mAllImageStreamInfo[i]->getImgSize().h,
                mAllImageStreamInfo[i]->getBufPlanes().size(),
                mAllImageStreamInfo[i]->getTransform());
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
dumpPipelineStreamConfig()
{
    if(!mbDumpInfo)
    {
        return;
    }

    MY_LOGD1("dump stream config");
    int count = 0;
    Vector<PipeStreamConfig>::iterator iter;
    for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
    {
        MY_LOGD1("count=%d, node=%d, port=%d, infoId=%d, streamId=%d, type=0x%x, usage=0x%x"
                ,count
                ,iter->eNode
                ,iter->ePort
                ,iter->streamInfoId
                ,iter->streamId
                ,iter->streamType
                ,iter->usage);
        count++;
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupP1Node(MINT32 nodeId, char* nodeName)
{
        MY_LOGD1("Nodebuilder p1 +");
        typedef P1Node                  NodeT;
        typedef NodeActor< NodeT >      MyNodeActorT;

        NodeT::InitParams initParam;
        initParam.openId = mOpenId;
        initParam.nodeId = eNODEID_P1Node;//eNODEID_P1Node
        initParam.nodeName = nodeName;//"node p1"
        MY_LOGD2("p1 initParam, openId=%d, nodeId=%d, name=%s",
                initParam.openId,
                initParam.nodeId,
                initParam.nodeName);

        NodeT::ConfigParams cfgParam;
        cfgParam.pOutImage_resizer = NULL;
        cfgParam.pStreamPool_resizer = NULL;
        cfgParam.pStreamPool_full = NULL;
        cfgParam.sensorParams = mSensorParam;
        Vector<PipeStreamConfig>::iterator iter;
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->ePort )
            {
                case EP1InAppMeta:
                    cfgParam.pInAppMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP1InHalMeta:
                    cfgParam.pInHalMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP1OutAppMeta:
                    cfgParam.pOutAppMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP1OutHalMeta:
                    cfgParam.pOutHalMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP1OutImageResizer:
                    cfgParam.pOutImage_resizer = mAllImageStreamInfo[iter->streamInfoId];
                    if( iter->streamType == eStreamType_IMG_HAL_POOL )
                    {
                        cfgParam.pStreamPool_resizer = mContext->queryImageStreamPool(iter->streamId);
                    }
                    break;
                case EP1OutImageFull:
                    cfgParam.pvOutImage_full.push_back(mAllImageStreamInfo[iter->streamInfoId]);
                    if( iter->streamType == eStreamType_IMG_HAL_POOL )
                    {
                        cfgParam.pStreamPool_full = mContext->queryImageStreamPool(iter->streamId);
                    }
                    break;
                default:
                    break;
            }
        }
        MY_LOGD2("p1 cfgParam, InAppMetaId=%d, InHalMetaId=%d, OutAppMetaId=%d, OutHalMetaId=%d",
                    cfgParam.pInAppMeta->getStreamId(),
                    cfgParam.pInHalMeta->getStreamId(),
                    cfgParam.pOutAppMeta->getStreamId(),
                    cfgParam.pOutHalMeta->getStreamId());
        if(cfgParam.pOutImage_resizer != NULL)
        {
            MY_LOGD1("p1 cfgParam, OutRrzoImageId=%d",
                    cfgParam.pOutImage_resizer->getStreamId());
        }
        if(cfgParam.pvOutImage_full.size() > 0)
        {
            MY_LOGD1("p1 cfgParam, OutImgoSize=%d, 0:OutImgoImageId=%d",
                    cfgParam.pvOutImage_full.size(),
                    (cfgParam.pvOutImage_full[0])->getStreamId());
        }

        sp<MyNodeActorT> pNode = new MyNodeActorT( NodeT::createInstance() );
        pNode->setInitParam(initParam);
        pNode->setConfigParam(cfgParam);

        StreamSet streamSetIn = StreamSet();
        StreamSet streamSetOut = StreamSet();
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->ePort & EInOutMask )
            {
                case EIn:
                    MY_LOGD2("p1 streamSetIn add %d", iter->streamId);
                    streamSetIn = streamSetIn.add(iter->streamId);
                    break;
                case EOut:
                    MY_LOGD2("p1 streamSetOut add %d", iter->streamId);
                    streamSetOut = streamSetOut.add(iter->streamId);
                    break;
                default:
                    MY_LOGD2("p1 not set %d", iter->streamId);
                    break;
            }
        }

        NodeBuilder nodeBuilder = NodeBuilder( nodeId, pNode)
                                    .addStream( NodeBuilder::eDirection_IN, streamSetIn)
                                    .addStream( NodeBuilder::eDirection_OUT, streamSetOut);
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->streamType & eCategory_Type_MASK )
            {
                case eType_IMAGE:
                    MY_LOGD2("p1 setImageStreamUsage %d-0x%x", iter->streamId, iter->usage);
                    nodeBuilder = nodeBuilder.setImageStreamUsage(iter->streamId,iter->usage);
                    break;
                case eType_META:
                    MY_LOGD2("no need for meta stream to setImageStreamUsage %d", iter->streamId);
                    break;
                default:
                    MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
            }
        }
        MERROR ret = nodeBuilder.build(mContext);
        MY_LOGD1("Nodebuilder p1 -");

        if( ret != OK ) {
            MY_LOGE("build p1 node error");
            return;
        }
        return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
dumpPipelineP1Config()
{
    //TBD
    return;
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupP2Node(MINT32 nodeId, char* nodeName)
{
        MY_LOGD1("Nodebuilder p2 +");
        typedef P2Node                  NodeT;
        typedef NodeActor< NodeT >      MyNodeActorT;

        NodeT::InitParams initParam;
        initParam.openId = mOpenId;
        initParam.nodeId = nodeId;//eNODEID_P2Node
        initParam.nodeName = nodeName;//"node p2"
        MY_LOGD2("p2 initParam, openId=%d, nodeId=%d, name=%s",
                initParam.openId,
                initParam.nodeId,
                initParam.nodeName);

        NodeT::ConfigParams cfgParam;
        cfgParam.pInResizedRaw = NULL;
        cfgParam.pOutFDImage = NULL;
        Vector<PipeStreamConfig>::iterator iter;
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->ePort )
            {
                case EP2InAppMeta:
                    cfgParam.pInAppMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP2InHalMeta:
                    cfgParam.pInHalMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP2OutAppMeta:
                    cfgParam.pOutAppMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP2OutHalMeta:
                    cfgParam.pOutHalMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EP2InFullRaw:
                    cfgParam.pvInFullRaw.push_back(mAllImageStreamInfo[iter->streamInfoId]);
                    break;
                case EP2InResizedRaw:
                    cfgParam.pInResizedRaw = mAllImageStreamInfo[iter->streamInfoId];
                    break;
                case EP2OutImage:
                    cfgParam.vOutImage.push_back(mAllImageStreamInfo[iter->streamInfoId]);
                    break;
                case EP2OutFDImage:
                    cfgParam.pOutFDImage = mAllImageStreamInfo[iter->streamInfoId];
                    break;
                default:
                    break;
            }
        }
        MY_LOGD2("p2 cfgParam, InAppMetaId=%d, InHalMetaId=%d, OutAppMetaId=%d, OutHalMetaId=%d",
                     cfgParam.pInAppMeta->getStreamId(),
                     cfgParam.pInHalMeta->getStreamId(),
                     cfgParam.pOutAppMeta->getStreamId(),
                     cfgParam.pOutHalMeta->getStreamId());
        if(cfgParam.pInResizedRaw != NULL)
        {
            MY_LOGD1("p2 cfgParam, InRrzoImageId=%d",
                     cfgParam.pInResizedRaw->getStreamId());
        }
        if(cfgParam.pvInFullRaw.size() > 0)
        {
            MY_LOGD1("p2 cfgParam, InImgoSize=%d, 0:InImgoImageId=%d",
                     cfgParam.pvInFullRaw.size(),
                     (cfgParam.pvInFullRaw[0])->getStreamId());
        }
        if(cfgParam.pOutFDImage != NULL)
        {
            MY_LOGD1("p2 cfgParam, OutFDImageId=%d",
                     cfgParam.pOutFDImage->getStreamId());
        }
        {
            int i;
            for( i = 0 ; i < cfgParam.vOutImage.size() ; i++ )
            {
                MY_LOGD2("p2 cfgParam, OutImage=%d, Id=%d", i, (cfgParam.vOutImage[i])->getStreamId());
            }
        }

        sp<MyNodeActorT> pNode = new MyNodeActorT( NodeT::createInstance() );
        pNode->setInitParam(initParam);
        pNode->setConfigParam(cfgParam);

        StreamSet streamSetIn = StreamSet();
        StreamSet streamSetOut = StreamSet();
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->ePort & EInOutMask )
            {
                case EIn:
                    MY_LOGD2("p2 streamSetIn add %d", iter->streamId);
                    streamSetIn = streamSetIn.add(iter->streamId);
                    break;
                case EOut:
                    MY_LOGD2("p2 streamSetOut add %d", iter->streamId);
                    streamSetOut = streamSetOut.add(iter->streamId);
                    break;
                default:
                    MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
            }
        }

        NodeBuilder nodeBuilder = NodeBuilder( nodeId, pNode)
                                    .addStream( NodeBuilder::eDirection_IN, streamSetIn)
                                    .addStream( NodeBuilder::eDirection_OUT, streamSetOut);
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->streamType & eCategory_Type_MASK )
            {
                case eType_IMAGE:
                    MY_LOGD2("p2 setImageStreamUsage %d-0x%x", iter->streamId, iter->usage);
                    nodeBuilder = nodeBuilder.setImageStreamUsage(iter->streamId,iter->usage);
                    break;
                case eType_META:
                    MY_LOGD2("no need for meta stream to setImageStreamUsage %d", iter->streamId);
                    break;
                default:
                    MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
            }
        }
        MERROR ret = nodeBuilder.build(mContext);
        MY_LOGD1("Nodebuilder p2 -");

        if( ret != OK ) {
            MY_LOGE("build p2 node error");
            return;
        }
        return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
dumpPipelineP2Config()
{
    //TBD
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupJpgNode(MINT32 nodeId, char* nodeName)
{
        MY_LOGD1("Nodebuilder jpg +");
        typedef JpegNode                NodeT;
        typedef NodeActor< NodeT >      MyNodeActorT;

        NodeT::InitParams initParam;
        initParam.openId = mOpenId;
        initParam.nodeId = nodeId;//eNODEID_JpegNode
        initParam.nodeName = nodeName;//"node jpg"
        MY_LOGD2("jpg initParam, openId=%d, nodeId=%d, name=%s",
                initParam.openId,
                initParam.nodeId,
                initParam.nodeName);

        NodeT::ConfigParams cfgParam;
        Vector<PipeStreamConfig>::iterator iter;
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->ePort )
            {
                case EJpgInAppMeta:
                    cfgParam.pInAppMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EJpgInHalMeta:
                    cfgParam.pInHalMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EJpgOutAppMeta:
                    cfgParam.pOutAppMeta = mAllMetaStreamInfo[iter->streamInfoId];
                    break;
                case EJpgInPictureYuv:
                    cfgParam.pInYuv_Main = mAllImageStreamInfo[iter->streamInfoId];
                    break;
                case EJpgInThumbnailYuv:
                    cfgParam.pInYuv_Thumbnail = mAllImageStreamInfo[iter->streamInfoId];
                    break;
                case EJpgOutJpg:
                    cfgParam.pOutJpeg = mAllImageStreamInfo[iter->streamInfoId];
                    break;
                default:
                    break;
            }
        }
        MY_LOGD2("jpg cfgParam, InAppMetaId=%d, InHalMetaId=%d, OutAppMetaId=%d",
                cfgParam.pInAppMeta->getStreamId(),
                cfgParam.pInHalMeta->getStreamId(),
                cfgParam.pOutAppMeta->getStreamId());
        MY_LOGD2("jpg cfgParam, InMainYuvId=%d, InThumbYuvId=%d, OutJpgId=%d",
                cfgParam.pInYuv_Main->getStreamId(),
                cfgParam.pInYuv_Thumbnail->getStreamId(),
                cfgParam.pOutJpeg->getStreamId());

        sp<MyNodeActorT> pNode = new MyNodeActorT( NodeT::createInstance() );
        pNode->setInitParam(initParam);
        pNode->setConfigParam(cfgParam);

        StreamSet streamSetIn = StreamSet();
        StreamSet streamSetOut = StreamSet();
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->ePort & EInOutMask )
            {
                case EIn:
                    MY_LOGD2("jpg streamSetIn add %d", iter->streamId);
                    streamSetIn = streamSetIn.add(iter->streamId);
                    break;
                case EOut:
                    MY_LOGD2("jpg streamSetOut add %d", iter->streamId);
                    streamSetOut = streamSetOut.add(iter->streamId);
                    break;
                default:
                    MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
            }
        }

        NodeBuilder nodeBuilder = NodeBuilder( nodeId, pNode)
                                    .addStream( NodeBuilder::eDirection_IN, streamSetIn)
                                    .addStream( NodeBuilder::eDirection_OUT, streamSetOut);
        for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
        {
            if( iter->eNode != nodeId )
            {
                continue;
            }

            switch( iter->streamType & eCategory_Type_MASK )
            {
                case eType_IMAGE:
                    MY_LOGD2("jpg setImageStreamUsage %d-0x%x", iter->streamId, iter->usage);
                    nodeBuilder = nodeBuilder.setImageStreamUsage(iter->streamId,iter->usage);
                    break;
                case eType_META:
                    MY_LOGD2("no need for meta stream to setImageStreamUsage %d", iter->streamId);
                    break;
                default:
                    MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
            }
        }
        MERROR ret = nodeBuilder.build(mContext);
        MY_LOGD1("Nodebuilder jpg -");

        if( ret != OK ) {
            MY_LOGE("build jpg node error");
            return;
        }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
dumpPipelineJpgConfig()
{
    //TBD
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
finishPipelineContext()
{
    FUNC_START;
    //
    if(mContext != NULL)
    {
        mContext->waitUntilDrained();
    }
    else
    {
        MY_LOGW("mContext is NULL");
    }
    //
    FUNC_END;
}


/******************************************************************************
 *
 ******************************************************************************/
IMetaStreamBuffer*
PipeMgrImp::
createMetaStreamBuffer(
    android::sp<IMetaStreamInfo> pStreamInfo,
    IMetadata const& rSettings,
    MBOOL const repeating)
{
    HalMetaStreamBuffer* pStreamBuffer = HalMetaStreamBuffer::Allocator(pStreamInfo.get())(rSettings);
    //
    pStreamBuffer->setRepeating(repeating);
    //
    return pStreamBuffer;
}


/******************************************************************************
*
*******************************************************************************/
status_t
PipeMgrImp::
readyToRun()
{
    ::prctl(PR_SET_NAME, LOG_TAG, 0, 0, 0);
    //
    int const expect_policy     = SCHED_OTHER;
    int const expect_priority   = NICE_CAMERA_PIPEMGR_BASE;
    int policy = 0, priority = 0;
    NSCam::Utils::setThreadPriority(expect_policy, expect_priority);
    NSCam::Utils::getThreadPriority(policy, priority);
    //
    MY_LOGD1("policy:(expect, result)=(%d, %d), priority:(expect, result)=(0x%x, 0x%x)",
            expect_policy,
            policy,
            expect_priority,
            priority);
    return OK;
}


/******************************************************************************
*
*******************************************************************************/
bool
PipeMgrImp::
threadLoop()
{
    FUNC_START;
    while(1)
    {
        ELoopCmd loopCmd = ELoopCmd_Exit;
        list<ELoopCmd>::iterator it;
        MBOOL bEraseCmd = MFALSE;
        {
            Mutex::Autolock _l(mLoopLock);
            //
            if(mlLoopCmd.size() == 0)
            {
                MY_LOGD1("loop idle and broadcast");
                mLoopCondStop.broadcast();
                MY_LOGD1("wait lock E");
                mLoopCond.wait(mLoopLock);
                MY_LOGD1("wait lock X");
            }
            else
            {
                it = mlLoopCmd.begin();
                loopCmd = (*it);
                if(loopCmd == ELoopCmd_StartLoop)
                {
                    if(mlLoopCmd.size() > 1)
                    {
                       bEraseCmd = MTRUE;
                    }
                }
                else
                if(loopCmd == ELoopCmd_StartOne)
                {
                    MY_LOGD1("ELoopCmd_StartOne,size(%d)",mlLoopCmd.size());
                    bEraseCmd = MTRUE;
                }
                else
                {
                    MY_LOGD1("ELoopCmd_Exit,size(%d)",mlLoopCmd.size());
                    mlLoopCmd.erase(it);
                    MY_LOGD1("loop exit");
                    break;
                }
            }
        }
        //
        if(loopCmd != ELoopCmd_Exit)
        {
#if 0
            IMetadata metadata;
            mPipeMgrParams.spRequestSettingBuilder->getRequest(mRequestCnt, metadata);
            //
            mpAppMetaControlSB = createMetaStreamBuffer(
                                    mAllMetaStreamInfo[EMetaInfo_Control_App],
                                    metadata,
                                    false);
            mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_Control_Hal].get())();
#endif
            //
            MY_LOGD1("ReqCnt(%d),Loop:status(%d),size(%d)",
                        mRequestCnt,
                        loopCmd,
                        mlLoopCmd.size());
            //
            sp<IPipelineFrame> pFrame = getPipelineFrame();
            if(pFrame.get())
            {
                MY_LOGD2("queue E");
                if(OK != mContext->queue(pFrame))
                {
                    MY_LOGE("queue pFrame failed");
                }
                MY_LOGD2("queue X");
            }
            //
            if(mRequestCnt == getRequestNumMax())
            {
                mRequestCnt = getRequestNumMin();
            }
            else
            {
                mRequestCnt++;
            }
            //
            if(bEraseCmd)
            {
                MY_LOGD1("erase Cmd(%d)",loopCmd);
                Mutex::Autolock _l(mLoopLock);
                mlLoopCmd.erase(it);
            }
        }
    }
    //
    FUNC_END;
    return true;
}


/******************************************************************************
*
*******************************************************************************/
MBOOL
PipeMgrImp::
startLoop()
{
    FUNC_START;
    //
    Mutex::Autolock _l(mLoopLock);
    //
    mlLoopCmd.push_back(ELoopCmd_StartLoop);
    mLoopCond.broadcast();
    //
    FUNC_END;
    return true;
}


/******************************************************************************
*
*******************************************************************************/
MBOOL
PipeMgrImp::
startOne()
{
    FUNC_START;
    //
    Mutex::Autolock _l(mLoopLock);
    //
    mlLoopCmd.push_back(ELoopCmd_StartOne);
    mLoopCond.broadcast();
    //
    FUNC_END;
    return true;
}


/******************************************************************************
*
*******************************************************************************/
MBOOL
PipeMgrImp::
stop()
{
    FUNC_START;
    //
    Mutex::Autolock _l(mLoopLock);
    //
    if(mlLoopCmd.size() > 0)
    {
        list<ELoopCmd>::iterator it;
        //
        it = mlLoopCmd.begin();
        if((*it) == ELoopCmd_StartLoop)
        {
            MY_LOGD1("Stop loop");
            mlLoopCmd.erase(it);

        }
        mLoopCondStop.wait(mLoopLock);
        MY_LOGD1("Stop");
    }
    //
    finishPipelineContext();
    //
    FUNC_END;
    return true;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
PipeMgrImp::
calRrzoSize(
    MRect&  crop,
    MSize&  size,
    MUINT32 zoomRatio,
    MSize   dstSize)
{
    NSCamHW::Rect SrcRect(0, 0, mSensorParam.size.w, mSensorParam.size.h);
    NSCamHW::Rect DstRect(0, 0, dstSize.w, dstSize.h);
    NSCamHW::Rect CropRect = MtkCamUtils::calCrop(SrcRect, DstRect, zoomRatio);
    //
    crop.p.x = CropRect.x;
    crop.p.y = CropRect.y;
    crop.s.w = CropRect.w;
    crop.s.h = CropRect.h;
    //
    if( crop.s.w <= dstSize.w ||
        crop.s.h <= dstSize.h)
    {
        size.w = crop.s.w;
        size.h = crop.s.h;
    }
    else
    {
        if( crop.s.w * RRZO_SCALE_DOWN_RATIO_MAX > dstSize.w ||
            crop.s.h * RRZO_SCALE_DOWN_RATIO_MAX > dstSize.h)
        {
            size.w = crop.s.w*RRZO_SCALE_DOWN_RATIO_MAX;
            if(size.w < crop.s.w*RRZO_SCALE_DOWN_RATIO_MAX)
            {
                size.w += 1;
            }
            //
            size.h = crop.s.h*RRZO_SCALE_DOWN_RATIO_MAX;
            if(size.h < crop.s.h*RRZO_SCALE_DOWN_RATIO_MAX)
            {
                size.h += 1;
            }
        }
        else
        {
            size.w = dstSize.w;
            size.h = dstSize.h;
        }
    }
    //
    if( ALIGN_UP_SIZE(size.w, 2) <= crop.s.w &&
        ALIGN_UP_SIZE(size.h, 2) <= crop.s.h)
    {
        size.w = ALIGN_UP_SIZE(size.w, 2);
        size.h = ALIGN_UP_SIZE(size.h, 2);
    }
    else
    {
        size.w = ALIGN_DOWN_SIZE(size.w, 2);
        size.h = ALIGN_DOWN_SIZE(size.h, 2);
    }
    //
    MY_LOGV("dstSize(%dx%d),C(%d,%d,%dx%d),S(%dx%d)",
            dstSize.w,
            dstSize.h,
            crop.p.x,
            crop.p.y,
            crop.s.w,
            crop.s.h,
            size.w,
            size.h);
    //
    return MTRUE;
}


//by scenario functions
/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
prepareSensor()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
closeSensor()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
prepareConfiguration()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupStreamBufferProvider()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
updateStreamBufferProvider()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupPipelineStreamInfo()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupPipelineStreamConfig()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupPipelineStream()
{
#if 0
    Vector<PipeStreamConfig>::iterator iter;
    for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
    {
        switch( iter->streamType & eCategory_Type_MASK )
        {
            case eType_IMAGE:
                StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId]).build(mContext);
                break;
            case eType_META:
                StreamBuilder(iter->streamType, mAllMetaStreamInfo[iter->streamInfoId]).build(mContext);
                break;
            default:
                MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
        }
    }
#endif
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupPipelineNode()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
setupPipelineFlow()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
finishStreamBufferProvider()
{
    MY_LOGW("No implementation");
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<IPipelineFrame>
PipeMgrImp::
getPipelineFrame()
{
    MY_LOGW("No implementation");
    return NULL;
}


/******************************************************************************
*
*******************************************************************************/
MBOOL
PipeMgrImp::
sendMetadata(
    MINT32              requestNumber,
    StreamId_T const    streamId,
    IMetadata*          pMetadata)
{
    MY_LOGW("No implementation");
    return true;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImp::
getInitRrzoSize(
    MINT&   width,
    MINT&   height)
{
    MY_LOGW("No implementation");
    return;
}


