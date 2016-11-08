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

#define LOG_TAG "MtkCam/PipeMgr/NorPrv"

#include <list>
#include "MyUtils.h"
#include <v1/IParamsManager.h>

#include <metadata/client/mtk_metadata_tag.h>

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

#include <Hal3/mtk_platform_metadata_tag.h>

#include <IHalSensor.h>

#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>

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
#include <v1/StreamBufferProviders/BufMgr.h>
using namespace NSMtkBufMgr;
#include <v1/StreamBufferProviders/CamClientStreamBufHandler.h>
using namespace NSCamStreamBufProvider;
#include <v1/PipeDataInfo.h>
using namespace NSMtkPipeDataInfo;
#include <v1/converter/RequestSettingBuilder.h>
#include <v1/PipeMgr/PipeMgr.h>
using namespace NSMtkPipeMgr;
//
#include "PipeMgrImp.h"
//
#include "PipeMgrImp.NorPrv.h"
using namespace NSMtkPipeMgrImp;
//
/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImpNorPrv::
PipeMgrImpNorPrv(
    MUINT32     openId,
    EPipeScen   pipeScenario)
    : PipeMgrImp(openId, pipeScenario)
    , mRrzoSize(MSize(0,0))
    , mRrzoFormat(eImgFmt_FG_BAYER10)
    , mRrzoStride(0)
    , mImgoSize(MSize(0,0))
    , mImgoFormat(eImgFmt_BAYER10)
    , mImgoStride(0)
    , mspCamClientStreamBufHandler(NULL)
{
    FUNC_NAME;
    mSensorParam.mode = SENSOR_SCENARIO_ID_NORMAL_PREVIEW;
}


/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImpNorPrv::
~PipeMgrImpNorPrv()
{
    FUNC_NAME;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
prepareSensor()
{
    IHalSensorList* const pHalSensorList = IHalSensorList::get();
    pHalSensorList->searchSensors();
    mpSensorHalObj = pHalSensorList->createSensor(LOG_TAG, mOpenId);
    if( ! mpSensorHalObj ) {
        MY_LOGE("create sensor failed");
        return;
    }
    //MUINT32    sensorArray[1] = {mOpenId};
    //mpSensorHalObj->powerOn(LOG_TAG, 1, &sensorArray[0]);

    MUINT32 sensorDev = pHalSensorList->querySensorDevIdx(mOpenId);
    SensorStaticInfo sensorStaticInfo;
    memset(&sensorStaticInfo, 0, sizeof(SensorStaticInfo));
    pHalSensorList->querySensorStaticInfo(sensorDev, &sensorStaticInfo);

    switch(mSensorParam.mode)
    {
        case SENSOR_SCENARIO_ID_NORMAL_PREVIEW:
        {
            mSensorParam.size = MSize(sensorStaticInfo.previewWidth, sensorStaticInfo.previewHeight);
            break;
        }
        case SENSOR_SCENARIO_ID_NORMAL_CAPTURE:
        {
            mSensorParam.size = MSize(sensorStaticInfo.captureWidth, sensorStaticInfo.captureHeight);
            break;
        }
        case SENSOR_SCENARIO_ID_NORMAL_VIDEO:
        {
            mSensorParam.size = MSize(sensorStaticInfo.videoWidth, sensorStaticInfo.videoHeight);
            break;
        }
        case SENSOR_SCENARIO_ID_SLIM_VIDEO1:
        {
            mSensorParam.size = MSize(sensorStaticInfo.video1Width, sensorStaticInfo.video1Width);
            break;
        }
        case SENSOR_SCENARIO_ID_SLIM_VIDEO2:
        {
            mSensorParam.size = MSize(sensorStaticInfo.video2Width, sensorStaticInfo.video2Width);
            break;
        }
        default:
        {
            MY_LOGE("un-supported scenarioId(%d)",mSensorParam.mode);
            mSensorParam.mode = SENSOR_SCENARIO_ID_NORMAL_PREVIEW;
            mSensorParam.size = MSize(sensorStaticInfo.previewWidth, sensorStaticInfo.previewHeight);
            break;
        }
    }

    mpSensorHalObj->sendCommand(
                        pHalSensorList->querySensorDevIdx(mOpenId),
                        SENSOR_CMD_GET_DEFAULT_FRAME_RATE_BY_SCENARIO,
                        (MINTPTR)(&mSensorParam.mode),
                        (MINTPTR)(&mSensorParam.fps),//get fps here
                        0);

    mSensorParam.pixelMode = 0;
    mpSensorHalObj->sendCommand(
            pHalSensorList->querySensorDevIdx(mOpenId),
            SENSOR_CMD_GET_SENSOR_PIXELMODE,
            (MUINTPTR)(&mSensorParam.mode),
            (MUINTPTR)(&mSensorParam.fps),
            (MUINTPTR)(&mSensorParam.pixelMode));//get pixel mode here

    MY_LOGD1("sensor params mode %d, size %dx%d, fps %d, pixelmode %d",
            mSensorParam.mode,
            mSensorParam.size.w, mSensorParam.size.h,
            mSensorParam.fps,
            mSensorParam.pixelMode);

    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
closeSensor()
{
    //MUINT32    sensorArray[1] = {mOpenId};
    //mpSensorHalObj->powerOff(LOG_TAG, 1, &sensorArray[0]);
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
prepareConfiguration()
{
#define ALIGN_2(x)     (((x) + 1) & (~1))

    {//???
        sp<IMetadataProvider> pMetadataProvider = IMetadataProvider::create(mOpenId);
        NSMetadataProviderManager::add(mOpenId, pMetadataProvider.get());
    }
    {//???
        ITemplateRequest* obj = NSTemplateRequestManager::valueFor(mOpenId);
        if(obj == NULL) {
            obj = ITemplateRequest::getInstance(mOpenId);
            NSTemplateRequestManager::add(mOpenId, obj);
        }
    }

    MINT initRrzoW = 0, initRrzoH = 0;
    MRect rrzoCrop;
    MSize rrzoSize;

    getInitRrzoSize(initRrzoW, initRrzoH);
    calRrzoSize(
        rrzoCrop,
        rrzoSize,
        100,
        MSize(initRrzoW, initRrzoH));

    MY_LOGD1("initRrzo size(%dx%d),RRZO:crop(%d,%d,%dx%d),size(%dx%d)",
            initRrzoW,
            initRrzoH,
            rrzoCrop.p.x,
            rrzoCrop.p.y,
            rrzoCrop.s.w,
            rrzoCrop.s.h,
            rrzoSize.w,
            rrzoSize.h);

    mRrzoFormat = eImgFmt_FG_BAYER10;
    NSImageio::NSIspio::ISP_QUERY_RST queryRst;
    NSImageio::NSIspio::ISP_QuerySize(
            NSImageio::NSIspio::EPortIndex_RRZO,
            NSImageio::NSIspio::ISP_QUERY_X_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_BYTE,
            (EImageFormat)mRrzoFormat,
            rrzoSize.w,
            queryRst,
            mSensorParam.pixelMode  == 0 ?
            NSImageio::NSIspio::ISP_QUERY_1_PIX_MODE :
            NSImageio::NSIspio::ISP_QUERY_2_PIX_MODE
            );
    rrzoSize.w = queryRst.x_pix;
    mRrzoSize = MSize(rrzoSize.w, rrzoSize.h);
    mRrzoStride = queryRst.stride_byte;
    MY_LOGD1("rrzo format 0x%x size %dx%d, stride %d",
            mRrzoFormat,
            mRrzoSize.w,
            mRrzoSize.h,
            mRrzoStride);

    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
setupStreamBufferProvider()
{
    mspCamClientStreamBufHandler = CamClientStreamBufHandler::createInstance(
                                        mOpenId,
                                        LOG_TAG,
                                        mPipeMgrParams.spImgBufProvidersMgr
                                        /*,mPipeMgrParams.pBufMgr
                                          ,pipeImageCallback*/);
    if(mspCamClientStreamBufHandler == NULL)
    {
        MY_LOGE("mspCamClientStreamBufHandler is NULL");
        return;
    }
    mspCamClientStreamBufHandler->init();
    mspCamClientStreamBufHandler->mapPort(CamClientStreamBufHandler::eBuf_Disp,     STREAM_ID_PASS2_OUT1);
    mspCamClientStreamBufHandler->mapPort(CamClientStreamBufHandler::eBuf_Rec,      STREAM_ID_PASS2_OUT2);
    mspCamClientStreamBufHandler->mapPort(CamClientStreamBufHandler::eBuf_AP,       STREAM_ID_PASS2_OUT2);
    mspCamClientStreamBufHandler->mapPort(CamClientStreamBufHandler::eBuf_Generic,  STREAM_ID_PASS2_OUT2);
    mspCamClientStreamBufHandler->mapPort(CamClientStreamBufHandler::eBuf_FD,       STREAM_ID_PASS2_OUT3);
    mspCamClientStreamBufHandler->mapPort(CamClientStreamBufHandler::eBuf_OT,       STREAM_ID_PASS2_OUT3);
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
setupPipelineStreamInfo()
{
    //meta streams
    mAllMetaStreamInfo[EMetaInfo_Control_App] =
        new MetaStreamInfo( "App:Meta:Control",
                            STREAM_ID_METADATA_CONTROL_APP,
                            eSTREAMTYPE_META_IN,
                            0);
    mAllMetaStreamInfo[EMetaInfo_Control_Hal] =
        new MetaStreamInfo( "Hal:Meta:Control",
                            STREAM_ID_METADATA_CONTROL_HAL,
                            eSTREAMTYPE_META_IN,
                            0);
    mAllMetaStreamInfo[EMetaInfo_P1_App] =
        new MetaStreamInfo( "App:Meta:ResultP1",
                            STREAM_ID_METADATA_RESULT_P1_APP,
                            eSTREAMTYPE_META_OUT,
                            0);
    mAllMetaStreamInfo[EMetaInfo_P1_Hal] =
        new MetaStreamInfo( "Hal:Meta:ResultP1",
                            STREAM_ID_METADATA_RESULT_P1_HAL,
                            eSTREAMTYPE_META_INOUT,
                            0);
    mAllMetaStreamInfo[EMetaInfo_P2_App] =
        new MetaStreamInfo( "App:Meta:ResultP2",
                            STREAM_ID_METADATA_RESULT_P2_APP,
                            eSTREAMTYPE_META_OUT,
                            0);
    mAllMetaStreamInfo[EMetaInfo_P2_Hal] =
        new MetaStreamInfo( "Hal:Meta:ResultP2",
                            STREAM_ID_METADATA_RESULT_P2_HAL,
                            eSTREAMTYPE_META_INOUT,
                            0);
    //image streams
    mAllImageStreamInfo[EImageInfo_ResizedRaw] =
        createRawImageStreamInfo(   "Hal:Image:Resiedraw",
                                    STREAM_ID_PASS1_RESIZE,
                                    eSTREAMTYPE_IMAGE_INOUT,
                                    6,
                                    4,
                                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                                    mRrzoFormat,
                                    mRrzoSize,
                                    mRrzoStride);
    mAllImageStreamInfo[EImageInfo_Port1] =
        createImageStreamInfo(  "Hal:Image:Port1",
                                STREAM_ID_PASS2_OUT1,
                                eSTREAMTYPE_IMAGE_INOUT,
                                5,
                                1,
                                0,
                                eImgFmt_YUY2,
                                MSize(640,480),
                                0);
    mAllImageStreamInfo[EImageInfo_Port2] =
        createImageStreamInfo(  "Hal:Image:Port2",
                                STREAM_ID_PASS2_OUT2,
                                eSTREAMTYPE_IMAGE_INOUT,
                                5,
                                1,
                                0,
                                eImgFmt_YUY2,
                                MSize(640,480),
                                0);

    mAllImageStreamInfo[EImageInfo_Port3] =
        createImageStreamInfo(  "Hal:Image:Port3",
                                STREAM_ID_PASS2_OUT3,
                                eSTREAMTYPE_IMAGE_INOUT,
                                5,
                                1,
                                0,
                                eImgFmt_YUY2,
                                MSize(640,480),
                                0);
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
setupPipelineStreamConfig()
{
    //mvPipeStreamConfig.push_back(
    //  PipeStreamConfig(   eNode,
    //                      ePort,
    //                      streamInfoId,
    //                      streamId,
    //                      streamType,
    //                      usage));
    // P1:Meta
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P1Node, EP1InAppMeta,
                            EMetaInfo_Control_App,
                            STREAM_ID_METADATA_CONTROL_APP,
                            eStreamType_META_APP,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P1Node, EP1InHalMeta,
                            EMetaInfo_Control_Hal,
                            STREAM_ID_METADATA_CONTROL_HAL,
                            eStreamType_META_HAL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P1Node, EP1OutAppMeta,
                            EMetaInfo_P1_App,
                            STREAM_ID_METADATA_RESULT_P1_APP,
                            eStreamType_META_APP,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P1Node, EP1OutHalMeta,
                            EMetaInfo_P1_Hal,
                            STREAM_ID_METADATA_RESULT_P1_HAL,
                            eStreamType_META_HAL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    // P1:Image
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P1Node,
                            EP1OutImageResizer,
                            EImageInfo_ResizedRaw,
                            STREAM_ID_PASS1_RESIZE,
                            eStreamType_IMG_HAL_POOL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    // P2:Meta
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2InAppMeta,
                            EMetaInfo_Control_App,
                            STREAM_ID_METADATA_CONTROL_APP,
                            eStreamType_META_APP,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2InHalMeta,
                            EMetaInfo_P1_Hal,
                            STREAM_ID_METADATA_RESULT_P1_HAL,
                            eStreamType_META_HAL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node, EP2OutAppMeta,
                            EMetaInfo_P2_App,
                            STREAM_ID_METADATA_RESULT_P2_APP ,
                            eStreamType_META_APP,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutHalMeta,
                            EMetaInfo_P2_Hal,
                            STREAM_ID_METADATA_RESULT_P2_HAL,
                            eStreamType_META_HAL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    // P2:Image
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2InResizedRaw,
                            EImageInfo_ResizedRaw,
                            STREAM_ID_PASS1_RESIZE,
                            eStreamType_IMG_HAL_POOL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutImage,
                            EImageInfo_Port1,
                            STREAM_ID_PASS2_OUT1,
                            eStreamType_IMG_HAL_PROVIDER,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    #if CAM_CLIENT_TEST
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutImage,
                            EImageInfo_Port2,
                            STREAM_ID_PASS2_OUT2,
                            eStreamType_IMG_HAL_PROVIDER,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutFDImage,
                            EImageInfo_Port3,
                            STREAM_ID_PASS2_OUT3,
                            eStreamType_IMG_HAL_PROVIDER,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    #endif
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
setupPipelineStream()
{
    Vector<PipeStreamConfig>::iterator iter;
    for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
    {
        switch( iter->streamType )
        {
            case eStreamType_IMG_HAL_POOL:
            {
                StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId])
                            .build(mContext);
                MY_LOGD2("build stream type=0x%x, imageStreamInfoId=%d",
                            iter->streamType,
                            iter->streamInfoId);
                break;
            }
            case eStreamType_IMG_HAL_PROVIDER:
            {
                if( iter->ePort == EP2OutImage ||
                    iter->ePort == EP2OutFDImage)
                {
                    StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId])
                                .setProvider((sp<IStreamBufferProvider>)mspCamClientStreamBufHandler)
                                .build(mContext);
                    MY_LOGD2("build stream type=0x%x, imageStreamInfoId=%d, provider=CamClient",
                                iter->streamType,
                                iter->streamInfoId);
                }
                else
                {
                    MY_LOGW("unsupport stream port %d",iter->ePort);
                }
                break;
            }
            case eStreamType_META_APP:
            case eStreamType_META_HAL:
            {
                StreamBuilder(iter->streamType, mAllMetaStreamInfo[iter->streamInfoId])
                            .build(mContext);
                MY_LOGD2("build stream type=0x%x, metaStreamInfoId=%d",
                            iter->streamType,
                            iter->streamInfoId);
                break;
            }
            case eStreamType_IMG_APP:
            case eStreamType_IMG_HAL_RUNTIME:
            default:
            {
                MY_LOGW("unsupport stream type %d",iter->streamType);
                break;
            }
        }
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
setupPipelineNode()
{
    setupP1Node(eNODEID_P1Node, "node p1");
    if( mbDumpInfo )
    {
        dumpPipelineP1Config();
    }
    setupP2Node(eNODEID_P2Node, "node p2");
    if( mbDumpInfo )
    {
        dumpPipelineP2Config();
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
setupPipelineFlow()
{
    MERROR ret = PipelineBuilder()
            .setRootNode(
                NodeSet().add(eNODEID_P1Node)
                )
            .setNodeEdges(
                NodeEdgeSet()
                .addEdge(eNODEID_P1Node, eNODEID_P2Node)
                )
            .build(mContext);
    if( ret != OK ) {
        MY_LOGE("build pipeline error");
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
finishStreamBufferProvider()
{
    if( mspCamClientStreamBufHandler!= NULL )
    {
        mspCamClientStreamBufHandler->unmapPort(CamClientStreamBufHandler::eBuf_Disp);
        mspCamClientStreamBufHandler->unmapPort(CamClientStreamBufHandler::eBuf_Rec);
        mspCamClientStreamBufHandler->unmapPort(CamClientStreamBufHandler::eBuf_AP);
        mspCamClientStreamBufHandler->unmapPort(CamClientStreamBufHandler::eBuf_Generic);
        mspCamClientStreamBufHandler->unmapPort(CamClientStreamBufHandler::eBuf_FD);
        mspCamClientStreamBufHandler->unmapPort(CamClientStreamBufHandler::eBuf_OT);
        mspCamClientStreamBufHandler->uninit();
        mspCamClientStreamBufHandler->destroyInstance();
        mspCamClientStreamBufHandler = NULL;
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<IPipelineFrame>
PipeMgrImpNorPrv::
getPipelineFrame()
{
    MY_LOGD2("+");
    //
    IMetadata metadata;
    mPipeMgrParams.spRequestSettingBuilder->getRequest(mRequestCnt, metadata);
    mpAppMetaControlSB = createMetaStreamBuffer(
                            mAllMetaStreamInfo[EMetaInfo_Control_App],
                            metadata,
                            false);
    mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_Control_Hal].get())();

    sp<IPipelineFrame> pFrame;
    //
    {
        IMetadata* pMetadata = mpHalMetaControlSB->tryWriteLock(LOG_TAG);
        {
            IMetadata::IEntry entry(MTK_HAL_REQUEST_SENSOR_SIZE);
            entry.push_back(mSensorParam.size, Type2Type< MSize >());
            pMetadata->update(entry.tag(), entry);
        }
        mpHalMetaControlSB->unlock(LOG_TAG, pMetadata);
    }
    //
    pFrame = RequestBuilder()
        .setIOMap(
                eNODEID_P1Node,
                IOMapSet().add(
                    IOMap()
                    .addOut(STREAM_ID_PASS1_RESIZE)
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
                    .addIn(STREAM_ID_PASS1_RESIZE)
                    .addOut(STREAM_ID_PASS2_OUT1)
                    #if CAM_CLIENT_TEST
                    .addOut(STREAM_ID_PASS2_OUT2)
                    .addOut(STREAM_ID_PASS2_OUT3)
                    #endif
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
                mpAppMetaControlSB
                )
        .setMetaStreamBuffer(
                STREAM_ID_METADATA_CONTROL_HAL,
                mpHalMetaControlSB
                )
        .updateFrameCallback(mPipeMgrParams.spResultProcessor)
        .build(mRequestCnt, mContext);
    if(!pFrame.get())
    {
        MY_LOGE("build request failed");
    }
    //
    MY_LOGD2("- mRequestCnt(%d)", mRequestCnt);
    return pFrame;
}


/******************************************************************************
*
*******************************************************************************/
MBOOL
PipeMgrImpNorPrv::
sendMetadata(
    MINT32              requestNumber,
    StreamId_T const    streamId,
    IMetadata*          pMetadata)
{
    if( mspCamClientStreamBufHandler != NULL &&
        streamId == STREAM_ID_METADATA_RESULT_P1_APP)
    {
        mspCamClientStreamBufHandler->setMetadata(requestNumber, pMetadata);
    }
    return true;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpNorPrv::
getInitRrzoSize(
    MINT&   width,
    MINT&   height)
{
    if(mPipeMgrParams.spParamsMgr != NULL)
    {
        mPipeMgrParams.spParamsMgr->getPreviewSize(&width, &height);
    }
    else
    {
        MY_LOGW("mPipeMgrParams.spParamsMgr == NULL");
    }
}


