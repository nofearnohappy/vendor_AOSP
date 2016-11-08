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
#define LOG_TAG "PipielineContextTest"
//
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
#ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly - USING_FAKE_SENSOR
#include <drv/src/isp/mt6797/iopipe/CamIO/FakeSensor.h>
#endif
//
//#include <../../BaseNode.h>
#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>
//
#include <mtk_platform_metadata_tag.h>
//[workaround]
#include <sensor_hal.h>
//#include <mtkcam/iopipe/CamIO/INormalPipe.h>
//using namespace NSCam::NSIoPipe;
#include <imageio/ispio_utility.h>
//
#include <metadata/ITemplateRequest.h>
#include <metadata/IMetadataProvider.h>
#include <hardware/camera3.h> // for template
//
#include <CamIO/INormalPipe.h>
//#include <CamIO/IStatisticPipe.h>
#include <CamIO/PortMap.h>

using namespace NSCam;
using namespace v3;
using namespace NSCam::v3::Utils;
using namespace android;
using namespace NSCam::v3::NSPipelineContext;

/******************************************************************************
 *
 ******************************************************************************/
#if 0
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#else
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);\
                                    printf("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);\
                                    printf("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);\
                                    printf("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);\
                                    printf("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);\
                                    printf("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg);
#endif
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
//
#define TEST(cond, result)          do { if ( (cond) == (result) ) { printf("Pass\n"); } else { printf("Failed\n"); } }while(0)
#define FUNCTION_IN     MY_LOGD_IF(1, "+");

#define P2_SUPPORT 0


/******************************************************************************
 *
 ******************************************************************************/
void help()
{
    printf("Pipeline <test>\n");
}

static
sp<ImageStreamInfo> createRawImageStreamInfo(
        char const*         streamName,
        StreamId_T          streamId,
        MUINT32             streamType,
        size_t              maxBufNum,
        size_t              minInitBufNum,
        MUINT               usageForAllocator,
        MINT                imgFormat,
        MSize const&        imgSize,
        size_t const        stride
        );

static
sp<ImageStreamInfo> createImageStreamInfo(
        char const*         streamName,
        StreamId_T          streamId,
        MUINT32             streamType,
        size_t              maxBufNum,
        size_t              minInitBufNum,
        MUINT               usageForAllocator,
        MINT                imgFormat,
        MSize const&        imgSize,
        MUINT32             transform = 0
        );
/******************************************************************************
 *
 ******************************************************************************/

namespace {

    enum STREAM_ID{
        STREAM_ID_RAW1 = 1,
        STREAM_ID_RAW2,
        STREAM_ID_YUV1,
        STREAM_ID_YUV2,
        //
        STREAM_ID_METADATA_CONTROL_APP,
        STREAM_ID_METADATA_CONTROL_HAL,
        STREAM_ID_METADATA_RESULT_P1_APP,
        STREAM_ID_METADATA_RESULT_P1_HAL,
        STREAM_ID_METADATA_RESULT_P2_APP,
        STREAM_ID_METADATA_RESULT_P2_HAL,
        //STREAM_ID_APPMETADATA2,
        //STREAM_ID_HALMETADATA1
    };

    enum NODE_ID{
        NODE_ID_NODE1 = 1,
        NODE_ID_NODE2,
        NODE_ID_FAKE
    };
    //
    IHalSensor* mpSensorHalObj = NULL;
    //
    static MUINT32 gSensorId = 0;
    static MUINT32 requestTemplate = CAMERA3_TEMPLATE_PREVIEW;
    //static bool test_full = true;
    //static bool test_resize = true;

    P1Node::SensorParams        gSensorParam;
    P1Node::ConfigParams        gP1ConfigParam;
    P2Node::ConfigParams        gP2ConfigParam;
    //
    MSize                       gRrzoSize;
    const MINT                  gRrzoFormat = eImgFmt_FG_BAYER10;
    size_t                      gRrzoStride;
    //
    MSize                       gImgoSize;
    const MINT                  gImgoFormat = eImgFmt_BAYER10;
    size_t                      gImgoStride;
    //
    android::sp<PipelineContext> gContext;
    //
    // StreamInfos
    sp<IMetaStreamInfo>         gControlMeta_App;
    sp<IMetaStreamInfo>         gControlMeta_Hal;
    sp<IMetaStreamInfo>         gResultMeta_P1_App;
    sp<IMetaStreamInfo>         gResultMeta_P1_Hal;
    sp<IMetaStreamInfo>         gResultMeta_P2_App;
    sp<IMetaStreamInfo>         gResultMeta_P2_Hal;
    //
    sp<IImageStreamInfo>        gImage_RrzoRaw;
    sp<IImageStreamInfo>        gImage_ImgoRaw;
    sp<IImageStreamInfo>        gImage_Yuv;
}; // namespace

int testRequestCnt = 1; // set for request count
int testEndingSec = 0;  // set for the waiting time of ending in second
int testIntervalMs = 0; // set for the time interval between requests in MS
int testZoom10X = 10;   // set for zoom in 10X expression (ex: 2-multiple is 20)
int testCropAlign = 0;  // set for crop alignment (0:center 1:LT 2:RT 3:LB 4:RB)

/******************************************************************************
 *
 ******************************************************************************/
void clear_global_var()
{
}

// crop test
#ifdef P1_REQ_CROP_TAG
#undef P1_REQ_CROP_TAG
#endif
#define P1_REQ_CROP_TAG (MTK_P1NODE_SCALAR_CROP_REGION) // [FIXME] sync correct tag

static MSize checkCropSize(NSCam::NSIoPipe::PortID portId,
    EImageFormat fmt, MSize size)
{
    NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo qry;
    MSize nSize = size;

    NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
        portId.index,
        NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_X_PIX,
        fmt, nSize.w, qry);

    nSize.w = qry.x_pix;

    return nSize;
}

static MPoint checkCropStart(NSCam::NSIoPipe::PortID portId,
    MSize in, MSize crop, MINT32 fmt, MUINT32 option)
{
    NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo qry;
    MINT32 width = 0;
    MINT32 height = 0;

    switch (option) {

        case 1: // L-T
            width = 0;
            height = 0;
            break;

        case 2: // R-T
            width = (in.w-crop.w);
            height = 0;
            break;

        case 3: // L-B
            width = 0;
            height = (in.h-crop.h);
            break;

        case 4: // R-B
            width = (in.w-crop.w);
            height = (in.h-crop.h);
            break;

        case 0: // Center
        default:
            width = (in.w-crop.w)/2;
            height = (in.h-crop.h)/2;
            break;
    }
    if (width < 0) {
        MY_LOGE("Wrong crop width %d of %d", crop.w, in.w);
        width = 0;
    }
    if (height < 0) {
        MY_LOGE("Wrong crop height %d of %d", crop.h, in.h);
        height = 0;
    }

    NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
        portId.index,
        NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_CROP_START_X,
        (EImageFormat)fmt, width, qry);

    return MPoint(qry.crop_x, height);
}


/******************************************************************************
 *
 ******************************************************************************/
void setupMetaStreamInfo()
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


void setupImageStreamInfo()
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
                6, 4,
                usage, format, size, stride
                );
    }
    {
        MSize const& size = gImgoSize;
        MINT const format = gImgoFormat;
        size_t const stride = gImgoStride;
        MUINT const usage = eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE ;
        gImage_ImgoRaw = createRawImageStreamInfo(
                "Hal:Image:Fullraw",
                STREAM_ID_RAW2,
                eSTREAMTYPE_IMAGE_INOUT,
                6, 4,
                usage, format, size, stride
                );
    }
    {
        MSize const& size = MSize(640,480);
        MINT const format = eImgFmt_YUY2;
        size_t const stride = 1280;
        MUINT const usage = eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE ;//0;
        gImage_Yuv = createImageStreamInfo(
                "Hal:Image:yuv",
                STREAM_ID_YUV1,
                eSTREAMTYPE_IMAGE_INOUT,
                5, 1,
                usage, format, size
                );
    }
}
/******************************************************************************
 *
 ******************************************************************************/
void prepareSensor()
{
    #ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly - USING_FAKE_SENSOR
    IHalSensorList* const pHalSensorList = TS_FakeSensorList::getTestModel();
    MUINT num = pHalSensorList->searchSensors();
    //HalSensorList::buildSensorMetadata();
    #else
    IHalSensorList* const pHalSensorList = IHalSensorList::get();
    MUINT num = pHalSensorList->searchSensors();
    #endif
    MY_LOGD("searchSensors (%d)\n", num);

    mpSensorHalObj = pHalSensorList->createSensor("tester", gSensorId);
    if( ! mpSensorHalObj ) {
        MY_LOGE("create sensor failed");
        exit(1);
        return;
    }
    MUINT32    sensorArray[1] = {gSensorId};
    mpSensorHalObj->powerOn("tester", 1, &sensorArray[0]);

    MUINT32 sensorDev = pHalSensorList->querySensorDevIdx(gSensorId);
    SensorStaticInfo sensorStaticInfo;
    memset(&sensorStaticInfo, 0, sizeof(SensorStaticInfo));
    pHalSensorList->querySensorStaticInfo(sensorDev, &sensorStaticInfo);
    //
    gSensorParam.mode = SENSOR_SCENARIO_ID_NORMAL_CAPTURE;//SENSOR_SCENARIO_ID_NORMAL_PREVIEW;
    gSensorParam.size = MSize(sensorStaticInfo.captureWidth, sensorStaticInfo.captureHeight);//(sensorStaticInfo.previewWidth, sensorStaticInfo.previewHeight);
    #ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly - USING_FAKE_SENSOR
    gSensorParam.fps = 1;
    #else
    gSensorParam.fps = sensorStaticInfo.captureFrameRate;//previewFrameRate;
    #endif
    gSensorParam.pixelMode = 0;
    //
    mpSensorHalObj->sendCommand(
            pHalSensorList->querySensorDevIdx(gSensorId),
            SENSOR_CMD_GET_SENSOR_PIXELMODE,
            (MUINTPTR)(&gSensorParam.mode),
            (MUINTPTR)(&gSensorParam.fps),
            (MUINTPTR)(&gSensorParam.pixelMode));
    //
    MY_LOGD("sensor params mode %d, size %dx%d, fps %d, pixelmode %d\n",
            gSensorParam.mode,
            gSensorParam.size.w, gSensorParam.size.h,
            gSensorParam.fps,
            gSensorParam.pixelMode);
    //exit(1);
}

/******************************************************************************
 *
 ******************************************************************************/
void closeSensor()
{
    MUINT32    sensorArray[1] = {gSensorId};
    mpSensorHalObj->powerOff("tester", 1, &sensorArray[0]);
}


/******************************************************************************
 *
 ******************************************************************************/
void prepareConfiguration()
{
#define ALIGN_2(x)     (((x) + 1) & (~1))
    //
    {
        MY_LOGD("pMetadataProvider ...\n");
        sp<IMetadataProvider> pMetadataProvider = IMetadataProvider::create(gSensorId);
        if (pMetadataProvider.get() != NULL)
            MY_LOGD("pMetadataProvider (%p) +++\n", pMetadataProvider.get());
        NSMetadataProviderManager::add(gSensorId, pMetadataProvider.get());
        if (pMetadataProvider.get() != NULL)
            MY_LOGD("pMetadataProvider (%p) ---\n", pMetadataProvider.get());
    }
    {
        ITemplateRequest* obj = NSTemplateRequestManager::valueFor(gSensorId);
        if(obj == NULL) {
            obj = ITemplateRequest::getInstance(gSensorId);
            NSTemplateRequestManager::add(gSensorId, obj);
        }
    }
    //
    MSize rrzoSize = MSize( ALIGN_2(gSensorParam.size.w / 2), ALIGN_2(gSensorParam.size.h / 2) );
    //
    #if 0
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
    #else
    NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo qry_rrzo;
    NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
        NSCam::NSIoPipe::PORT_RRZO.index,
        NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_X_PIX|NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_STRIDE_BYTE,
        (EImageFormat)gRrzoFormat, rrzoSize.w, qry_rrzo);
    rrzoSize.w = qry_rrzo.x_pix;
    gRrzoSize = MSize(rrzoSize.w, rrzoSize.h);
    gRrzoStride = qry_rrzo.stride_byte;
    #endif
    MY_LOGD("rrzo size %dx%d, stride %d\n", gRrzoSize.w, gRrzoSize.h, (int)gRrzoStride);
    //
    //
    MSize imgoSize = MSize( gSensorParam.size.w, gSensorParam.size.h );
    //
    #if 0
    NSImageio::NSIspio::ISP_QUERY_RST queryRstF;
    //
    NSImageio::NSIspio::ISP_QuerySize(
            NSImageio::NSIspio::EPortIndex_IMGO,
            NSImageio::NSIspio::ISP_QUERY_X_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_BYTE,
            (EImageFormat)gImgoFormat,
            imgoSize.w,
            queryRstF,
            gSensorParam.pixelMode  == 0 ?
            NSImageio::NSIspio::ISP_QUERY_1_PIX_MODE :
            NSImageio::NSIspio::ISP_QUERY_2_PIX_MODE
            );
    imgoSize.w = queryRstF.x_pix;
    gImgoSize = MSize(imgoSize.w, imgoSize.h);
    gImgoStride = queryRstF.stride_byte;
    #else
    NSCam::NSIoPipe::NSCamIOPipe::NormalPipe_QueryInfo qry_imgo;
    NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::query(
        NSCam::NSIoPipe::PORT_IMGO.index,
        NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_X_PIX|NSCam::NSIoPipe::NSCamIOPipe::ENPipeQueryCmd_STRIDE_BYTE,
        (EImageFormat)gImgoFormat, imgoSize.w, qry_imgo);
    imgoSize.w = qry_imgo.x_pix;
    gImgoSize = MSize(imgoSize.w, imgoSize.h);
    gImgoStride = qry_imgo.stride_byte;
    #endif
    MY_LOGD("imgo size %dx%d, stride %d\n", gImgoSize.w, gImgoSize.h, (int)gImgoStride);
    //
    //exit(1);
}
/******************************************************************************
 *
 ******************************************************************************/
void setupPipelineContext()
{
    gContext = PipelineContext::create("test");
    if( !gContext.get() ) {
        MY_LOGE("cannot create context\n");
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
        // Image
        StreamBuilder(eStreamType_IMG_HAL_POOL, gImage_RrzoRaw)
            .build(gContext);
        StreamBuilder(eStreamType_IMG_HAL_POOL, gImage_ImgoRaw)
            .build(gContext);
        StreamBuilder(eStreamType_IMG_HAL_POOL, gImage_Yuv)
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
        MY_LOGD("Nodebuilder p1 +\n");
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
            cfgParam.pvOutImage_full.push_back(gImage_ImgoRaw); //N/A
            cfgParam.sensorParams = gSensorParam;
            #if 1 // test the NULL pool parameter
            cfgParam.pStreamPool_resizer = NULL;
            cfgParam.pStreamPool_full = NULL;
            #else
            cfgParam.pStreamPool_resizer = gContext->queryImageStreamPool(STREAM_ID_RAW1);
            cfgParam.pStreamPool_full = gContext->queryImageStreamPool(STREAM_ID_RAW2);
            #endif
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
                    //.add(STREAM_ID_RAW1)
                    .add(STREAM_ID_METADATA_CONTROL_APP)
                    .add(STREAM_ID_METADATA_CONTROL_HAL)
                    )
            .addStream(
                    NodeBuilder::eDirection_OUT,
                    StreamSet()
                    .add(STREAM_ID_RAW1)
                    .add(STREAM_ID_RAW2)
                    .add(STREAM_ID_METADATA_RESULT_P1_APP)
                    .add(STREAM_ID_METADATA_RESULT_P1_HAL)
                    )
            .setImageStreamUsage(
                    STREAM_ID_RAW1,
                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE
                    )
            .setImageStreamUsage(
                    STREAM_ID_RAW2,
                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE
                    )
            .build(gContext);
        MY_LOGD("Nodebuilder p1 -\n");

        if( ret != OK ) {
            MY_LOGE("build p1 node error\n");
            return;
        }
    }

    {
        typedef P2Node                  NodeT;
        typedef NodeActor< NodeT >      MyNodeActorT;
        //
        MY_LOGD("Nodebuilder p2 +\n");
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
        MY_LOGD("Nodebuilder p2 -\n");

        if( ret != OK ) {
            MY_LOGE("build p2 node error\n");
            return;
        }
    }
    //
    // 3. Pipeline **************
    //
    {
        MERROR ret = PipelineBuilder()
            .setRootNode(
                    NodeSet().add(eNODEID_P1Node)
                    )
            .setNodeEdges(
                    NodeEdgeSet()
                    .addEdge(eNODEID_P1Node, eNODEID_P2Node)
                    )
            .build(gContext);
        if( ret != OK ) {
            MY_LOGE("build pipeline error\n");
            return;
        }
    }
    //
    gContext->endConfigure();
}


/******************************************************************************
 *
 ******************************************************************************/
IMetaStreamBuffer*
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
sp<IMetaStreamBuffer>
get_default_request()
{
    sp<IMetaStreamBuffer> pSBuffer;

    ITemplateRequest* obj = NSTemplateRequestManager::valueFor(gSensorId);
    if(obj == NULL) {
        obj = ITemplateRequest::getInstance(gSensorId);
        NSTemplateRequestManager::add(gSensorId, obj);
    }
    IMetadata meta = obj->getMtkData(requestTemplate);
    //
    pSBuffer = createMetaStreamBuffer(gControlMeta_App, meta, false);
    //
    return pSBuffer;
}

/******************************************************************************
 *
 ******************************************************************************/
void processRequest()
{
    static int cnt = 0;
    int current_cnt = cnt++;
    //
    MY_LOGD("request %d +\n", current_cnt);
    //
    sp<IPipelineFrame> pFrame;
    //
    sp<IMetaStreamBuffer> pAppMetaControlSB = get_default_request();
    sp<HalMetaStreamBuffer> pHalMetaControlSB =
        HalMetaStreamBuffer::Allocator(gControlMeta_Hal.get())();
    {
        // modify hal control metadata
        IMetadata* pMetadata = pHalMetaControlSB->tryWriteLock(LOG_TAG);
        //
        {
            IMetadata::IEntry entry(MTK_HAL_REQUEST_SENSOR_SIZE);
            entry.push_back(gSensorParam.size, Type2Type< MSize >());
            pMetadata->update(entry.tag(), entry);
        }
        // crop test
        {
            MPoint zoomStart = MPoint(0, 0);
            MSize zoomSize = gSensorParam.size;
            zoomSize.w = zoomSize.w * 10 / testZoom10X;
            zoomSize.h = zoomSize.h * 10 / testZoom10X;
            zoomSize = checkCropSize(NSCam::NSIoPipe::PORT_RRZO,
                        (EImageFormat)gRrzoFormat, zoomSize);
            zoomStart = checkCropStart(NSCam::NSIoPipe::PORT_RRZO,
                        gSensorParam.size, zoomSize,
                        (EImageFormat)gRrzoFormat, testCropAlign);
            MY_LOGD("sensor(%d,%d)@zoom(%d.%dX) = crop(%d,%d)(%dx%d)[%d]\n",
                gSensorParam.size.w, gSensorParam.size.h,
                testZoom10X/10, testZoom10X%10,
                zoomStart.x, zoomStart.y, zoomSize.w, zoomSize.h,
                testCropAlign);
            MRect rect = MRect(zoomStart, zoomSize);
            IMetadata::IEntry entry(P1_REQ_CROP_TAG);
            entry.push_back(rect, Type2Type< MRect>());
            pMetadata->update(entry.tag(), entry);
        }
        pHalMetaControlSB->unlock(LOG_TAG, pMetadata);
    }
    {
        pFrame = RequestBuilder()
            .setIOMap(
                    eNODEID_P1Node,
                    IOMapSet().add(
                        IOMap()
                        .addOut(STREAM_ID_RAW1)
                        .addOut(STREAM_ID_RAW2)
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
            .build(current_cnt, gContext);
        if( ! pFrame.get() ) {
            MY_LOGE("build request failed\n");
        }
    }
    //
    if( pFrame.get() )
    {
        if( OK != gContext->queue(pFrame) ) {
            MY_LOGE("queue pFrame failed\n");
        }
    }

    MY_LOGD("request %d -\n", current_cnt);
}


/******************************************************************************
 *
 ******************************************************************************/
void finishPipelineContext()
{
    MY_LOGD("waitUntilDrained ...\n");
    gContext->waitUntilDrained();
    MY_LOGD("waitUntilDrained OK\n");
    gContext = NULL;
    MY_LOGD("waitUntilDrained END\n");
}

void test()
{
    exit(1);
    return;
}

/******************************************************************************
 *
 ******************************************************************************/
int main(int argc, char** argv)
{
    //test();
    MY_LOGD("[PipelineContext] start to test\n");

    if(argc >= 2) {
        testRequestCnt = atoi(argv[1]);
        MY_LOGD("[PipelineContext] set RequestCnt: %d\n", testRequestCnt);
    }

    if(argc >= 3) {
        testEndingSec = atoi(argv[2]);
        MY_LOGD("[PipelineContext] set EndingSec : %d\n", testEndingSec);
    }

    if(argc >= 4) {
        testIntervalMs = atoi(argv[3]);
        MY_LOGD("[PipelineContext] set IntervalMs : %d\n", testIntervalMs);
    }

    if(argc >= 5) {
        testZoom10X = atoi(argv[4]);
        MY_LOGD("[PipelineContext] set Zoom10X : %d\n", testZoom10X);
    }

    if(argc >= 6) {
        testCropAlign = atoi(argv[5]);
        MY_LOGD("[PipelineContext] set CropAlign : %d\n", testCropAlign);
    }

    MY_LOGD("[PipelineContext] : "
            "RequestCnt(%d) EndingSec(%d) "
            "IntervalMs(%d) Zoom10X(%d) CropAlign(%d)\n",
            testRequestCnt, testEndingSec, testIntervalMs,
            testZoom10X, testCropAlign);

    //sensor
    MY_LOGD("[PipelineContext] prepareSensor ...\n");
    prepareSensor();

    MY_LOGD("[PipelineContext] prepareConfiguration ...\n");
    prepareConfiguration();

    MY_LOGD("[PipelineContext] setupMetaStreamInfo ...\n");
    setupMetaStreamInfo();

    MY_LOGD("[PipelineContext] setupImageStreamInfo ...\n");
    setupImageStreamInfo();

    MY_LOGD("[PipelineContext] setupPipelineContext ...\n");
    setupPipelineContext();

    int c = 0;
    while( c++ < testRequestCnt )
    {
        MY_LOGD("[PipelineContext] processRequest (%d) +++\n", c);
        processRequest();
        MY_LOGD("[PipelineContext] processRequest (%d) ---\n", c);
        if (testIntervalMs > 0) {
            usleep(testIntervalMs * 1000);
        }
    };

    if (testEndingSec > 0)
    {
        int sleep_loop = testEndingSec; //sec
        int i = 0;
        for (i = 0; i < sleep_loop; i++) {
            MY_LOGD("[PipelineContext] usleep(1000000)*(%d) ...\n", i);
            usleep(1000000);
        }
        MY_LOGD("[PipelineContext] usleep(1000000)*(%d) END\n", i);
    }

    MY_LOGD("[PipelineContext] finishPipelineContext ...\n");
    finishPipelineContext();

    MY_LOGD("[PipelineContext] closeSensor ...\n");
    closeSensor();
    clear_global_var();

    MY_LOGD("[PipelineContext] end of test\n");
    return 0;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<ImageStreamInfo>
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
            MY_LOGE("format not support yet 0x%x \n", imgFormat);
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
        MY_LOGE("create ImageStream failed, %s, %ld \n",
                streamName, streamId);
    }

    return pStreamInfo;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<ImageStreamInfo>
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
            MY_LOGE("format not support yet 0x%x \n", imgFormat);
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
        MY_LOGE("create ImageStream failed, %s, %ld \n",
                streamName, streamId);
    }

    return pStreamInfo;
}

