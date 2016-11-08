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

#define LOG_TAG "MtkCam/PipeMgr/ZsdCap"

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
#include <v1/PipeDataInfo.h>
using namespace NSMtkPipeDataInfo;
#include <v1/StreamBufferProviders/CamClientStreamBufHandler.h>
#include <v1/StreamBufferProviders/ZsdPrvStreamBufHandler.h>
#include <v1/StreamBufferProviders/ZsdShotStreamBufHandler.h>
#include <v1/StreamBufferProviders/JpgStreamBufHandler.h>
using namespace NSCamStreamBufProvider;
#include <v1/converter/RequestSettingBuilder.h>
#include <v1/PipeMgr/PipeMgr.h>
using namespace NSMtkPipeMgr;
//
#include "PipeMgrImp.h"
//
#include "PipeMgrImp.ZsdCap.h"
using namespace NSMtkPipeMgrImp;
//
/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImpZsdCap::
PipeMgrImpZsdCap(
    MUINT32     openId,
    EPipeScen   pipeScenario)
    : PipeMgrImp(openId, pipeScenario)
    , mRrzoSize(MSize(0,0))
    , mRrzoFormat(eImgFmt_FG_BAYER10)
    , mRrzoStride(0)
    , mImgoSize(MSize(4176,3088))//TBD
    , mImgoFormat(eImgFmt_BAYER10)
    , mImgoStride(5220)
    , mspCamClientStreamBufHandler(NULL)
    , mspZsdShotStreamBufHandler(NULL)
    , mspJpgStreamBufHandler(NULL)
{
    FUNC_NAME;
}


/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImpZsdCap::
~PipeMgrImpZsdCap()
{
    FUNC_NAME;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
setupStreamBufferProvider()
{
    mspZsdShotStreamBufHandler = ZsdShotStreamBufHandler::createInstance(
                                        mOpenId,
                                        LOG_TAG);
    if(mspZsdShotStreamBufHandler == NULL)
    {
        MY_LOGE("mspZsdShotStreamBufHandler is NULL");
        return;
    }
    mspZsdShotStreamBufHandler->init(mPipeMgrParams.pBufMgr);

    mspJpgStreamBufHandler = JpgStreamBufHandler::createInstance(
                                        mOpenId,
                                        LOG_TAG);
    if(mspJpgStreamBufHandler == NULL)
    {
        MY_LOGE("mspJpgStreamBufHandler is NULL");
        return;
    }
    mspJpgStreamBufHandler->init();
    mspJpgStreamBufHandler->setCallbacks(mPipeMgrParams.pipeImgCb, mPipeMgrParams.pipeImgCbUser);
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
updateStreamBufferProvider()
{
#if 0
    ZsdPrvStreamBufHandler::CONFIG_INFO configInfo;
    configInfo.pBufMgr = mPipeMgrParams.pBufMgr;
    configInfo.uiKeepBufCount = 0;
    configInfo.pStreamInfo = NULL;
    //mspZsdShotStreamBufHandler->setConfig(configInfo);//TBD
#endif
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
setupPipelineStreamInfo()
{
    //meta streams
    mAllMetaStreamInfo[EMetaInfo_Control_App] =
        new MetaStreamInfo( "App:Meta:Control",
                            STREAM_ID_METADATA_CONTROL_APP,
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
    mAllMetaStreamInfo[EMetaInfo_Jpg_App] =
        new MetaStreamInfo( "App:Meta:ResultJpg",
                            STREAM_ID_METADATA_RESULT_JPG_APP,
                            eSTREAMTYPE_META_OUT,
                            0);
    //image streams
    mAllImageStreamInfo[EImageInfo_FullRaw] =
        createRawImageStreamInfo(   "Hal:Image:Fullraw",
                                    STREAM_ID_PASS1_FULLSIZE,
                                    eSTREAMTYPE_IMAGE_INOUT,
                                    1,
                                    0,
                                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                                    mImgoFormat,//should be replaced when making IPipelineFrame
                                    mImgoSize,//should be replaced when making IPipelineFrame
                                    0);//should be replaced when making IPipelineFrame
    mAllImageStreamInfo[EImageInfo_PictureYuv] =
        createImageStreamInfo(  "Hal:Image:PictureYuv",
                                STREAM_ID_PASS2_PICTUREYUV,
                                eSTREAMTYPE_IMAGE_INOUT,
                                1,
                                0,
                                eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                                eImgFmt_YUY2,
                                MSize(2304,4096),//TBD:picture size
                                eTransform_ROT_90);
    mAllImageStreamInfo[EImageInfo_ThumbnailYuv] =
        createImageStreamInfo(  "Hal:Image:ThumbnailYuv",
                                STREAM_ID_PASS2_THUMBNAILYUV,
                                eSTREAMTYPE_IMAGE_INOUT,
                                1,
                                0,
                                eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                                eImgFmt_YUY2,
                                MSize(160,128),//TBD:thumb size
                                0);
    mAllImageStreamInfo[EImageInfo_Jpg] =
        createImageStreamInfo(  "Hal:Image:Jpg",
                                STREAM_ID_JPG,
                                eSTREAMTYPE_IMAGE_INOUT,
                                1,
                                0,
                                eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE | eBUFFER_USAGE_SW_WRITE_OFTEN,
                                eImgFmt_JPEG,
                                MSize(2304,4096),//TBD:picture size
                                0);

    if( mbDumpInfo )
    {
        dumpPipelineStreamInfo();
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
setupPipelineStreamConfig()
{
    //mvPipeStreamConfig.push_back(
    //  PipeStreamConfig(   eNode,
    //                      ePort,
    //                      streamInfoId,
    //                      streamId,
    //                      streamType,
    //                      usage));
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
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutAppMeta,
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
                            EP2InFullRaw,
                            EImageInfo_FullRaw,
                            STREAM_ID_PASS1_FULLSIZE,
                            eStreamType_IMG_HAL_PROVIDER,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutImage,
                            EImageInfo_PictureYuv,
                            STREAM_ID_PASS2_PICTUREYUV,
                            eStreamType_IMG_HAL_POOL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P2Node,
                            EP2OutImage,
                            EImageInfo_ThumbnailYuv,
                            STREAM_ID_PASS2_THUMBNAILYUV,
                            eStreamType_IMG_HAL_POOL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    // JPG:Meta
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_JpegNode,
                            EJpgInAppMeta,
                            EMetaInfo_Control_App,
                            STREAM_ID_METADATA_CONTROL_APP,
                            eStreamType_META_APP,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_JpegNode,
                            EJpgInHalMeta,
                            EMetaInfo_P2_Hal,
                            STREAM_ID_METADATA_RESULT_P2_HAL,
                            eStreamType_META_HAL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_JpegNode,
                            EJpgOutAppMeta,
                            EMetaInfo_Jpg_App,
                            STREAM_ID_METADATA_RESULT_JPG_APP,
                            eStreamType_META_APP,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    // JPG:Image
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_JpegNode,
                            EJpgInPictureYuv,
                            EImageInfo_PictureYuv,
                            STREAM_ID_PASS2_PICTUREYUV,
                            eStreamType_IMG_HAL_POOL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_JpegNode,
                            EJpgInThumbnailYuv,
                            EImageInfo_ThumbnailYuv,
                            STREAM_ID_PASS2_THUMBNAILYUV,
                            eStreamType_IMG_HAL_POOL,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_JpegNode,
                            EJpgOutJpg,
                            EImageInfo_Jpg,
                            STREAM_ID_JPG,
                            eStreamType_IMG_HAL_PROVIDER,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE | eBUFFER_USAGE_SW_WRITE_OFTEN});

    if( mbDumpInfo )
    {
        dumpPipelineStreamConfig();
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
setupPipelineStream()
{
    Vector<PipeStreamConfig>::iterator iter;
    for( iter = mvPipeStreamConfig.begin() ; iter != mvPipeStreamConfig.end() ; iter++ )
    {
        switch( iter->streamType )
        {
            case eStreamType_IMG_HAL_POOL:
                StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId]).build(mContext);
                MY_LOGD_IF((mLogLevel>=2),"build stream type=0x%x, imageStreamInfoId=%d",iter->streamType,iter->streamInfoId);
                break;
            case eStreamType_IMG_HAL_PROVIDER:
                if( iter->ePort == EP2InFullRaw )
                {
                    StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId])
                        .setProvider((sp<IStreamBufferProvider>)mspZsdShotStreamBufHandler)
                        .build(mContext);
                    MY_LOGD_IF((mLogLevel>=2),"build stream type=0x%x, imageStreamInfoId=%d, provider=ZsdShot", iter->streamType, iter->streamInfoId);
                }
                else if( iter->ePort == EJpgOutJpg )
                {
                    StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId])
                        .setProvider((sp<IStreamBufferProvider>)mspJpgStreamBufHandler)
                        .build(mContext);
                    MY_LOGD_IF((mLogLevel>=2),"build stream type=0x%x, imageStreamInfoId=%d, provider=Jpg", iter->streamType, iter->streamInfoId);
                }
                else
                {
                    MY_LOGW("unsupport stream port %d",iter->ePort);
                }
                break;
            case eStreamType_META_APP:
            case eStreamType_META_HAL:
                StreamBuilder(iter->streamType, mAllMetaStreamInfo[iter->streamInfoId]).build(mContext);
                MY_LOGD_IF((mLogLevel>=2),"build stream type=0x%x, metaStreamInfoId=%d",iter->streamType,iter->streamInfoId);
                break;
            case eStreamType_IMG_APP:
            case eStreamType_IMG_HAL_RUNTIME:
            default:
                MY_LOGW("unsupport stream type %d",iter->streamType);
                    break;
        }
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
setupPipelineNode()
{
    setupP2Node(eNODEID_P2Node, "zsd capture node p2");
    if( mbDumpInfo )
    {
        dumpPipelineP2Config();
    }
    setupJpgNode(eNODEID_JpegNode, "node jpg");
    if( mbDumpInfo)
    {
        dumpPipelineJpgConfig();
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdCap::
setupPipelineFlow()
{
    MERROR ret = PipelineBuilder()
            .setRootNode(
                NodeSet().add(eNODEID_P2Node)
                )
            .setNodeEdges(
                NodeEdgeSet()
                .addEdge(eNODEID_P2Node, eNODEID_JpegNode)
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
PipeMgrImpZsdCap::
finishStreamBufferProvider()
{
    if( mspZsdShotStreamBufHandler!= NULL )
    {
        mspZsdShotStreamBufHandler->uninit();
        mspZsdShotStreamBufHandler->destroyInstance();
        mspZsdShotStreamBufHandler = NULL;
    }
    if( mspJpgStreamBufHandler!= NULL )
    {
        mspJpgStreamBufHandler->uninit();
        mspJpgStreamBufHandler->destroyInstance();
        mspJpgStreamBufHandler = NULL;
    }
    return;
}

#if 1
/******************************************************************************
 *
 ******************************************************************************/
template <typename T>
inline MBOOL
tryGetMetadata(
    IMetadata const* const pMetadata,
    MUINT32 const tag,
    T & rVal
)
{
    if( pMetadata == NULL ) {
        //MY_LOGD("pMetadata == NULL");
        return MFALSE;
    }

    IMetadata::IEntry entry = pMetadata->entryFor(tag);
    if( !entry.isEmpty() ) {
        rVal = entry.itemAt(0, Type2Type<T>());
        return MTRUE;
    }
    return MFALSE;
}
#endif

/******************************************************************************
 *
 ******************************************************************************/
sp<IPipelineFrame>
PipeMgrImpZsdCap::
getPipelineFrame()
{
    MY_LOGD_IF((mLogLevel>=2),"+");

    mspZsdShotStreamBufHandler->prepareBufAndMeta();

    //App Meta
    IMetadata appMetadata;
    mPipeMgrParams.spRequestSettingBuilder->getRequest(mRequestCnt, appMetadata);
    mpAppMetaControlSB = createMetaStreamBuffer(
                            mAllMetaStreamInfo[EMetaInfo_Control_App],
                            appMetadata,
                            false);
    //Hal Meta
#if 1
    IMetadata halMetadata;
    MY_LOGD("mspZsdShotStreamBufHandler->getMetadata(&halMetadata) +");
    mspZsdShotStreamBufHandler->getMetadata(&halMetadata);
    MY_LOGD("mspZsdShotStreamBufHandler->getMetadata(&halMetadata) -");

    MY_LOGD("try halMetadata.count()");
    MY_LOGD("try halMetadata.count() = %d", halMetadata.count());

    // get hal meta info
    MSize testSize;
    MRect testRect;
    if( tryGetMetadata<MRect>(&halMetadata, MTK_P1NODE_SCALAR_CROP_REGION, testRect) )
    {
        MY_LOGW("get MTK_P1NODE_SCALAR_CROP_REGION fail");
    }
    else
    {
        MY_LOGD("get MTK_P1NODE_SCALAR_CROP_REGION, (p.x,p.y,s.w,s.h)=(%d,%d,%d,%d)", testRect.p.x, testRect.p.y, testRect.s.w, testRect.s.h);
    }
    if( tryGetMetadata<MSize>(&halMetadata, MTK_P1NODE_RESIZER_SIZE      , testSize) )
    {
        MY_LOGW("get MTK_P1NODE_RESIZER_SIZE fail");
    }
    else
    {
        MY_LOGD("get MTK_P1NODE_RESIZER_SIZE, (w,h)=(%d,%d)", testSize.w, testSize.h);
    }
    if( tryGetMetadata<MRect>(&halMetadata, MTK_P1NODE_DMA_CROP_REGION   , testRect) )
    {
        MY_LOGW("get MTK_P1NODE_DMA_CROP_REGION fail");
    }
    else
    {
        MY_LOGD("get MTK_P1NODE_DMA_CROP_REGION, (p.x,p.y,s.w,s.h)=(%d,%d,%d,%d)", testRect.p.x, testRect.p.y, testRect.s.w, testRect.s.h);
    }

    MY_LOGD("try again halMetadata.count()");
    MY_LOGD("try again halMetadata.count() = %d", halMetadata.count());

    MY_LOGD("mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_P1_Hal].get())(halMetadata) +");
    mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_P1_Hal].get())(halMetadata);
    MY_LOGD("mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_P1_Hal].get())(halMetadata) -");
    //IMetadata* pMetadata = mpHalMetaControlSB->tryWriteLock(LOG_TAG);
    //{
    //    IMetadata::IEntry entry(MTK_HAL_REQUEST_SENSOR_SIZE);
    //    entry.push_back(mImgoSize, Type2Type< MSize >());//TBD
    //    pMetadata->update(entry.tag(), entry);
    //}
    //mpHalMetaControlSB->unlock(LOG_TAG, pMetadata);
#else
    IMetadata halMetadata;
    MY_LOGD("mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_P1_Hal].get())(&pHalMetadata) +");
    mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_P1_Hal].get())(halMetadata);
    MY_LOGD("mpHalMetaControlSB = HalMetaStreamBuffer::Allocator(mAllMetaStreamInfo[EMetaInfo_P1_Hal].get())(&pHalMetadata) -");
    {
        IMetadata* pMetadata = mpHalMetaControlSB->tryWriteLock(LOG_TAG);
        {
            IMetadata::IEntry entry(MTK_HAL_REQUEST_SENSOR_SIZE);
            entry.push_back(mImgoSize, Type2Type< MSize >());
            pMetadata->update(entry.tag(), entry);
        }
        mpHalMetaControlSB->unlock(LOG_TAG, pMetadata);
    }
#endif

    sp<IPipelineFrame> pFrame;
    //
#if 0


    mspZsdShotStreamBufHandler.getImageInfo( format, size, stride);
    mAllImageStreamInfo[EImageInfo_FullRaw] =
        createRawImageStreamInfo(   "Hal:Image:Fullraw",
                                    STREAM_ID_PASS1_FULLSIZE,
                                    eSTREAMTYPE_IMAGE_INOUT,
                                    1,
                                    0,
                                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                                    format,
                                    size,
                                    stride);
#endif
    //
    pFrame = RequestBuilder()
        .setIOMap(
                eNODEID_P2Node,
                IOMapSet()
                    .add(
                        IOMap()
                        .addIn(STREAM_ID_PASS1_FULLSIZE)
                        .addOut(STREAM_ID_PASS2_PICTUREYUV)
                        .addOut(STREAM_ID_PASS2_THUMBNAILYUV)
                        ),
                IOMapSet().add(
                    IOMap()
                    .addIn(STREAM_ID_METADATA_CONTROL_APP)
                    .addIn(STREAM_ID_METADATA_RESULT_P1_HAL)
                    .addOut(STREAM_ID_METADATA_RESULT_P2_APP)
                    .addOut(STREAM_ID_METADATA_RESULT_P2_HAL)
                    )
                )
        .setIOMap(
                eNODEID_JpegNode,
                IOMapSet().add(
                    IOMap()
                    .addIn(STREAM_ID_PASS2_PICTUREYUV)
                    .addIn(STREAM_ID_PASS2_THUMBNAILYUV)
                    .addOut(STREAM_ID_JPG)
                    ),
                IOMapSet().add(
                    IOMap()
                    .addIn(STREAM_ID_METADATA_CONTROL_APP)
                    .addIn(STREAM_ID_METADATA_RESULT_P2_HAL)
                    .addOut(STREAM_ID_METADATA_RESULT_JPG_APP)
                    )
                )
        .setRootNode(
                NodeSet().add(eNODEID_P2Node)
                )
        .setNodeEdges(
                NodeEdgeSet()
                .addEdge(eNODEID_P2Node, eNODEID_JpegNode)
                )
        //.replaceStreamInfo(
        //        STREAM_ID_PASS1_FULLSIZE,
        //        mAllImageStreamInfo[EImageInfo_FullRaw]
        //        )
        .setMetaStreamBuffer(
                STREAM_ID_METADATA_CONTROL_APP,
                mpAppMetaControlSB
                )
        .setMetaStreamBuffer(
                STREAM_ID_METADATA_RESULT_P1_HAL,
                mpHalMetaControlSB
                )
        //.updateFrameCallback(mPipeMgrParams.spResultProcessor)
        .build(mRequestCnt, mContext);
    if(!pFrame.get())
    {
        MY_LOGE("build request failed");
    }
    //
    MY_LOGD_IF((mLogLevel>=2),"- mRequestCnt(%d)", mRequestCnt);
    return pFrame;
}


