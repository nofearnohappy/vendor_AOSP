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

#define LOG_TAG "MtkCam/DefaultFlowControl_pipeline_workaround"
#include <Log.h>
//
#include <stdlib.h>
#include <utils/Errors.h>
#include <utils/List.h>
#include <utils/RefBase.h>
//
#include <metadata/client/mtk_metadata_tag.h>
//
#include <v3/pipeline/IPipelineDAG.h>
#include <v3/pipeline/IPipelineNode.h>
#include <v3/pipeline/IPipelineNodeMapControl.h>
#include <v3/pipeline/IPipelineFrameControl.h>
#include <v3/pipeline/PipelineContext.h>
//
#include <v3/utils/streambuf/StreamBufferPool.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
//
#include <mmutils/include/IGrallocHelper.h>
//
#include <utils/include/imagebuf/IIonImageBufferHeap.h>
//
#include <IHalSensor.h>
//
#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>
//
#include <Hal3/mtk_platform_metadata_tag.h>
//[workaround]
#include <sensor_hal.h>
#include <imageio/ispio_utility.h>
//
#include <metadata/ITemplateRequest.h>
#include <metadata/IMetadataProvider.h>
#include <hardware/camera3.h> // for template
//
#include "DefaultFlowControl.h"
#include <v1/BufferProvider/StreamBufferProviderFactory.h>


using namespace NSCam;
using namespace v3;
using namespace NSCam::v3::Utils;
using namespace android;
using namespace NSCam::v3::NSPipelineContext;
using namespace NSCam::v1::NSLegacyPipeline;
//#include <ImgBufProvidersManager.h>
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
void
DefaultFlowControl::
prepareSensor()
{
    IHalSensorList* const pHalSensorList = IHalSensorList::get();
    mpSensorHalObj = pHalSensorList->createSensor("tester", gSensorId);
    if( ! mpSensorHalObj ) {
        MY_LOGE("create sensor failed");
        return;
    }

    MUINT32 sensorDev = pHalSensorList->querySensorDevIdx(gSensorId);
    SensorStaticInfo sensorStaticInfo;
    memset(&sensorStaticInfo, 0, sizeof(SensorStaticInfo));
    pHalSensorList->querySensorStaticInfo(sensorDev, &sensorStaticInfo);
    //
    gSensorParam.mode = SENSOR_SCENARIO_ID_NORMAL_PREVIEW;
    gSensorParam.size = MSize(sensorStaticInfo.previewWidth, sensorStaticInfo.previewHeight);
    gSensorParam.fps = sensorStaticInfo.previewFrameRate / 10;
    gSensorParam.pixelMode = 0;
    //
    mpSensorHalObj->sendCommand(
            pHalSensorList->querySensorDevIdx(gSensorId),
            SENSOR_CMD_GET_SENSOR_PIXELMODE,
            (MUINTPTR)(&gSensorParam.mode),
            (MUINTPTR)(&gSensorParam.fps),
            (MUINTPTR)(&gSensorParam.pixelMode));
    //
    MY_LOGD("sensor params mode %d, size %dx%d, fps %d, pixelmode %d",
            gSensorParam.mode,
            gSensorParam.size.w, gSensorParam.size.h,
            gSensorParam.fps,
            gSensorParam.pixelMode);
}

/******************************************************************************
 *
 ******************************************************************************/
IMetaStreamBuffer*
DefaultFlowControl::
createMetaStreamBuffer(
    android::sp<IMetaStreamInfo> pStreamInfo,
    IMetadata const& rSettings,
    MBOOL const repeating
)
{
    HalMetaStreamBuffer*
    pStreamBuffer =
    HalMetaStreamBuffer::Allocator(pStreamInfo.get())(rSettings);
    //
    pStreamBuffer->setRepeating(repeating);
    //
    return pStreamBuffer;
}

/******************************************************************************
 *
 ******************************************************************************/
sp<IImageStreamInfo>
DefaultFlowControl::
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

    if( pStreamInfo == NULL ) {
        MY_LOGE("create ImageStream failed, %s, %#"PRIxPTR,
                streamName, streamId);
    }

    return pStreamInfo;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<IImageStreamInfo>
DefaultFlowControl::
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
void
DefaultFlowControl::
prepareConfiguration()
{
#define ALIGN_2(x)     (((x) + 1) & (~1))
    //
    {
        gSensorId = 0;
        //sp<IMetadataProvider> pMetadataProvider = IMetadataProvider::create(gSensorId);
        //NSMetadataProviderManager::add(gSensorId, pMetadataProvider.get());
    }
    {
        /*ITemplateRequest* obj = NSTemplateRequestManager::valueFor(gSensorId);
        if(obj == NULL) {
            obj = ITemplateRequest::getInstance(gSensorId);
            NSTemplateRequestManager::add(gSensorId, obj);
        }*/
    }
    //
    MSize rrzoSize = MSize( ALIGN_2(gSensorParam.size.w / 2), ALIGN_2(gSensorParam.size.h / 2) );
    //
    NSImageio::NSIspio::ISP_QUERY_RST queryRst;
    //
    NSImageio::NSIspio::ISP_QuerySize(
            NSImageio::NSIspio::EPortIndex_RRZO,
            NSImageio::NSIspio::ISP_QUERY_X_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_BYTE,
            (EImageFormat)gRrzoFormat,
            rrzoSize.w,
            queryRst,
            gSensorParam.pixelMode  == 0 ?
            NSImageio::NSIspio::ISP_QUERY_1_PIX_MODE :
            NSImageio::NSIspio::ISP_QUERY_2_PIX_MODE
            );
    rrzoSize.w = queryRst.x_pix;
    gRrzoSize = MSize(rrzoSize.w, rrzoSize.h);
    gRrzoStride = queryRst.stride_byte;
    MY_LOGD("rrzo size %dx%d, stride %d", gRrzoSize.w, gRrzoSize.h, gRrzoStride);
    //
#undef  ALIGN_2
}

/******************************************************************************
 *
 ******************************************************************************/
void
DefaultFlowControl::
setupPipelineContext()
{
    gContext = PipelineContext::create("test");
    if( !gContext.get() ) {
        MY_LOGE("cannot create context");
        return;
    }
    //
    gContext->beginConfigure();
    //
    // 1. Streams ***************
    //
    // 1.a. check if stream exist
    // 1.b. setup streams
    {
        // Meta
        StreamBuilder(eStreamType_META_APP, gControlMeta_App)
            .build(gContext);
        StreamBuilder(eStreamType_META_HAL, gControlMeta_Hal)
            .build(gContext);
        StreamBuilder(eStreamType_META_APP, gResultMeta_P1_App)
            .build(gContext);
        StreamBuilder(eStreamType_META_HAL, gResultMeta_P1_Hal)
            .build(gContext);
        StreamBuilder(eStreamType_META_APP, gResultMeta_P2_App)
            .build(gContext);
        StreamBuilder(eStreamType_META_HAL, gResultMeta_P2_Hal)
            .build(gContext);

        {
            sp<StreamBufferProviderFactory> pFactory = StreamBufferProviderFactory::createInstance("display_client");
            pFactory->setImageStreamInfo(gImage_Yuv);

            pFactory->setCamClient(mpImgBufProvidersMgr, IImgBufProvider::eID_DISPLAY);
            Vector< sp<StreamBufferProvider> > pProviders;
            pFactory->create(pProviders);
            pCamClientProvider = pProviders[0];
        }
        //
        {
            /*sp<StreamBufferProviderFactory> pFactory = StreamBufferProviderFactory::createInstance("rrzo");
            pFactory->setImageStreamInfo(gImage_RrzoRaw);

            Vector< sp<StreamBufferProvider> > pProviders;
            pFactory->create(pProviders);
            pRawProvider = pProviders[0];*/
        }

        // Image
        StreamBuilder(eStreamType_IMG_HAL_POOL, gImage_RrzoRaw)
            .build(gContext);
        StreamBuilder(eStreamType_IMG_HAL_PROVIDER, gImage_Yuv)
            .setProvider(pCamClientProvider)
            .build(gContext);
    }
    //
    // 2. Nodes   ***************
    //
    // 2.a. check if node exist
    // 2.b. setup nodes
    //
    {
        typedef P1Node                  NodeT;
        typedef NodeActor< NodeT >      MyNodeActorT;
        //
        MY_LOGD("Nodebuilder p1 +");
        NodeT::InitParams initParam;
        {
            initParam.openId = gSensorId;
            initParam.nodeId = eNODEID_P1Node;
            initParam.nodeName = "P1Node";
        }
        NodeT::ConfigParams cfgParam;
        {
            cfgParam.pInAppMeta = gControlMeta_App;
            cfgParam.pInHalMeta = gControlMeta_Hal;
            cfgParam.pOutAppMeta = gResultMeta_P1_App;
            cfgParam.pOutHalMeta = gResultMeta_P1_Hal;
            cfgParam.pOutImage_resizer = gImage_RrzoRaw;
            cfgParam.pvOutImage_full; //N/A
            cfgParam.sensorParams = gSensorParam;
            cfgParam.pStreamPool_resizer = gContext->queryImageStreamPool(STREAM_ID_RAW1);
            cfgParam.pStreamPool_full;
        }
        //
        sp<MyNodeActorT> pNode = new MyNodeActorT( NodeT::createInstance() );
        pNode->setInitParam(initParam);
        pNode->setConfigParam(cfgParam);
        //
        MERROR ret =
            NodeBuilder(
                eNODEID_P1Node,
                pNode
                )
            .addStream(
                    NodeBuilder::eDirection_IN,
                    StreamSet()
                    .add(STREAM_ID_METADATA_CONTROL_APP)
                    .add(STREAM_ID_METADATA_CONTROL_HAL)
                    )
            .addStream(
                    NodeBuilder::eDirection_OUT,
                    StreamSet()
                    .add(STREAM_ID_RAW1)
                    .add(STREAM_ID_METADATA_RESULT_P1_APP)
                    .add(STREAM_ID_METADATA_RESULT_P1_HAL)
                    )
            .setImageStreamUsage(
                    STREAM_ID_RAW1,
                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE
                    )
            .build(gContext);
        MY_LOGD("Nodebuilder p1 -");

        if( ret != OK ) {
            MY_LOGE("build p1 node error");
            return;
        }
    }


#if !TEST_SINGLENODE
    {
        typedef P2Node                  NodeT;
        typedef NodeActor< NodeT >      MyNodeActorT;
        //
        MY_LOGD("Nodebuilder p2 +");
        NodeT::InitParams initParam;
        {
            initParam.openId = gSensorId;
            initParam.nodeId = eNODEID_P2Node;
            initParam.nodeName = "P2Node";
        }
        NodeT::ConfigParams cfgParam;
        {
            cfgParam.pInAppMeta = gControlMeta_App;
            cfgParam.pInHalMeta = gResultMeta_P1_Hal;
            cfgParam.pOutAppMeta = gResultMeta_P2_App;
            cfgParam.pOutHalMeta = gResultMeta_P2_Hal;
            cfgParam.pvInFullRaw; // N/A
            cfgParam.pInResizedRaw = gImage_RrzoRaw;
            cfgParam.vOutImage.push_back(gImage_Yuv);
            cfgParam.pOutFDImage; // N/A
        }
        //
        sp<MyNodeActorT> pNode = new MyNodeActorT( NodeT::createInstance() );
        pNode->setInitParam(initParam);
        pNode->setConfigParam(cfgParam);
        //
        MERROR ret =
            NodeBuilder(
                eNODEID_P2Node,
                pNode
                )
            .addStream(
                    NodeBuilder::eDirection_IN,
                    StreamSet()
                    .add(STREAM_ID_RAW1)
                    .add(STREAM_ID_METADATA_CONTROL_APP)
                    .add(STREAM_ID_METADATA_RESULT_P1_HAL)
                    )
            .addStream(
                    NodeBuilder::eDirection_OUT,
                    StreamSet()
                    .add(STREAM_ID_YUV1)
                    .add(STREAM_ID_METADATA_RESULT_P2_APP)
                    .add(STREAM_ID_METADATA_RESULT_P2_HAL)
                    )
            .setImageStreamUsage(
                    STREAM_ID_RAW1,
                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ
                    )
            .setImageStreamUsage(
                    STREAM_ID_YUV1,
                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE
                    )
            .build(gContext);
        MY_LOGD("Nodebuilder p2 -");

        if( ret != OK ) {
            MY_LOGE("build p2 node error");
            return;
        }
    }
#endif
    //
    // 3. Pipeline **************
    //
    {
        MERROR ret = PipelineBuilder()
            .setRootNode(
                    NodeSet().add(eNODEID_P1Node)
                    )
#if !TEST_SINGLENODE
            .setNodeEdges(
                    NodeEdgeSet()
                    .addEdge(eNODEID_P1Node, eNODEID_P2Node)
                    )
#endif
            .build(gContext);
        if( ret != OK ) {
            MY_LOGE("build pipeline error");
            return;
        }
    }
    //
    gContext->endConfigure();
}

/******************************************************************************
 *
 ******************************************************************************/
void
DefaultFlowControl::
setupImageStreamInfo()
{
    {
        MSize const& size = gRrzoSize;
        MINT const format = gRrzoFormat;
        size_t const stride = gRrzoStride;
        MUINT const usage = eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE ;
        gImage_RrzoRaw = createRawImageStreamInfo(
                "Hal:Image:Resiedraw",
                STREAM_ID_RAW1,
                eSTREAMTYPE_IMAGE_INOUT,
                8, 4,
                usage, format, size, stride
                );
    }
    {
        MSize const& size = MSize(640,480);
        MINT const format = eImgFmt_YUY2;
        size_t const stride = 1280;
        MUINT const usage = 0;
        gImage_Yuv = createImageStreamInfo(
                "Hal:Image:yuv",
                STREAM_ID_YUV1,
                eSTREAMTYPE_IMAGE_INOUT,
                5, 1,
                usage, format, size, false
                );
    }
}

/******************************************************************************
 *
 ******************************************************************************/
void
DefaultFlowControl::
setupMetaStreamInfo()
{
    gControlMeta_App =
        new MetaStreamInfo(
                "App:Meta:Control",
                STREAM_ID_METADATA_CONTROL_APP,
                eSTREAMTYPE_META_IN,
                0
                );
    gControlMeta_Hal =
        new MetaStreamInfo(
                "Hal:Meta:Control",
                STREAM_ID_METADATA_CONTROL_HAL,
                eSTREAMTYPE_META_IN,
                0
                );
    gResultMeta_P1_App =
        new MetaStreamInfo(
                "App:Meta:ResultP1",
                STREAM_ID_METADATA_RESULT_P1_APP,
                eSTREAMTYPE_META_OUT,
                0
                );
    gResultMeta_P1_Hal =
        new MetaStreamInfo(
                "Hal:Meta:ResultP1",
                STREAM_ID_METADATA_RESULT_P1_HAL,
                eSTREAMTYPE_META_INOUT,
                0
                );
    gResultMeta_P2_App =
        new MetaStreamInfo(
                "App:Meta:ResultP2",
                STREAM_ID_METADATA_RESULT_P2_APP,
                eSTREAMTYPE_META_OUT,
                0
                );
    gResultMeta_P2_Hal =
        new MetaStreamInfo(
                "Hal:Meta:ResultP2",
                STREAM_ID_METADATA_RESULT_P2_HAL,
                eSTREAMTYPE_META_INOUT,
                0
                );
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
constructNormalPreviewPipeline(
    wp<ILegacyPipeline>& rpPipeline,
    MINT32&              rPipelinId
)
{
    prepareSensor();

    prepareConfiguration();

    setupMetaStreamInfo();

    setupImageStreamInfo();

    setupPipelineContext();

    mpLegacyPipeline = ILegacyPipeline::createFakePipeline();
    mpLegacyPipeline->setPipelineContext(gContext);

    mpLegacyPipeline->setMetaStreamInfo(gControlMeta_App, gControlMeta_Hal);

    sp<ResultProcessor> pProcessor = mpLegacyPipeline->getResultProcessor().promote();
    if ( pProcessor == 0 ) {
        MY_LOGE("No ResultProcessor");
    } else {
        pProcessor->registerListener(STREAM_ID_METADATA_RESULT_P1_APP, pCamClientProvider);
    }

    rpPipeline = mpLegacyPipeline;
    rPipelinId = 0;
    //finishPipelineContext();
    return OK;
}

#warning "FIXME"
/******************************************************************************
 *
 ******************************************************************************/
sp<IPipelineFrame>
DefaultFlowControl::
constructPipelineFrame(
    MINT32  requestNo,
    sp<IMetaStreamBuffer>   pAppMetaControlSB,
    sp<HalMetaStreamBuffer> pHalMetaControlSB
)
{
    {
        /*IMetadata* pMetadata = pAppMetaControlSB->tryReadLock(LOG_TAG);
        MY_LOGD("pAppMetaControlSB size:%d", pMetadata->count());
        pAppMetaControlSB->unlock(LOG_TAG, pMetadata);

        IMetadata* pMetadata1 = pHalMetaControlSB->tryReadLock(LOG_TAG);
        MY_LOGD("pHalMetaControlSB size:%d", pMetadata1->count());
        pHalMetaControlSB->unlock(LOG_TAG, pMetadata1);*/
    }
    sp<IPipelineFrame> pFrame;
        pFrame = RequestBuilder()
            .setIOMap(
                    eNODEID_P1Node,
                    IOMapSet().add(
                        IOMap()
                        .addOut(STREAM_ID_RAW1)
                        ),
                    IOMapSet().add(
                        IOMap()
                        .addIn(STREAM_ID_METADATA_CONTROL_APP)
                        .addIn(STREAM_ID_METADATA_CONTROL_HAL)
                        .addOut(STREAM_ID_METADATA_RESULT_P1_APP)
                        .addOut(STREAM_ID_METADATA_RESULT_P1_HAL)
                        )
                    )
            .setIOMap(
                    eNODEID_P2Node,
                    IOMapSet().add(
                        IOMap()
                        .addIn(STREAM_ID_RAW1)
                        .addOut(STREAM_ID_YUV1)
                        ),
                    IOMapSet().add(
                        IOMap()
                        .addIn(STREAM_ID_METADATA_CONTROL_APP)
                        .addIn(STREAM_ID_METADATA_RESULT_P1_HAL)
                        .addOut(STREAM_ID_METADATA_RESULT_P2_APP)
                        .addOut(STREAM_ID_METADATA_RESULT_P2_HAL)
                        )
                    )
            .setRootNode(
                    NodeSet().add(eNODEID_P1Node)
                    )
            .setNodeEdges(
                    NodeEdgeSet().addEdge(eNODEID_P1Node, eNODEID_P2Node)
                    )
            .setMetaStreamBuffer(
                    STREAM_ID_METADATA_CONTROL_APP,
                    pAppMetaControlSB
                    )
            .setMetaStreamBuffer(
                    STREAM_ID_METADATA_CONTROL_HAL,
                    pHalMetaControlSB
                    )
            .updateFrameCallback(mpLegacyPipeline->getResultProcessor().promote())
            .build(requestNo, gContext);
        if( ! pFrame.get() ) {
            MY_LOGE("build request failed");
        }
    //
    return pFrame;
    /*if( pFrame.get() )
    {
        if( OK != gContext->queue(pFrame) ) {
            MY_LOGE("queue pFrame failed");
        }
    }*/
}
