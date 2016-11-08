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

#define LOG_TAG "MtkCam/PipeMgr/ZsdPrv"

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
#include <v1/StreamBufferProviders/ZsdPrvStreamBufHandler.h>
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
#include "PipeMgrImp.ZsdPrv.h"
using namespace NSMtkPipeMgrImp;
//
#define ZSD_ROLLBACK_NUM    (3)
//
/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImpZsdPrv::
PipeMgrImpZsdPrv(
    MUINT32     openId,
    EPipeScen   pipeScenario)
    : PipeMgrImpNorPrv(openId, pipeScenario)
    , mspZsdPrvStreamBufHandler(NULL)
{
    FUNC_NAME;
    mSensorParam.mode = SENSOR_SCENARIO_ID_NORMAL_CAPTURE;
}


/******************************************************************************
 *
 ******************************************************************************/
PipeMgrImpZsdPrv::
~PipeMgrImpZsdPrv()
{
    FUNC_NAME;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdPrv::
prepareConfiguration()
{
    PipeMgrImpNorPrv::prepareConfiguration();

    mImgoFormat = eImgFmt_BAYER10;
    NSImageio::NSIspio::ISP_QUERY_RST queryRst;
    NSImageio::NSIspio::ISP_QuerySize(
            NSImageio::NSIspio::EPortIndex_IMGO,
            NSImageio::NSIspio::ISP_QUERY_X_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_BYTE,
            (EImageFormat)mImgoFormat,
            mSensorParam.size.w,
            queryRst,
            mSensorParam.pixelMode  == 0 ?
            NSImageio::NSIspio::ISP_QUERY_1_PIX_MODE :
            NSImageio::NSIspio::ISP_QUERY_2_PIX_MODE);

    mImgoSize = MSize(mSensorParam.size.w, mSensorParam.size.h);
    mImgoStride = queryRst.stride_byte;
    MY_LOGD1("imgo format 0x%x size %dx%d, stride %d",
            mImgoFormat,
            mImgoSize.w,
            mImgoSize.h,
            mImgoStride);

    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdPrv::
setupStreamBufferProvider()
{
    PipeMgrImpNorPrv::setupStreamBufferProvider();
    //
    mspZsdPrvStreamBufHandler = ZsdPrvStreamBufHandler::createInstance(getOpenId(),LOG_TAG);
    if(mspZsdPrvStreamBufHandler == NULL)
    {
        MY_LOGE("mspZsdPrvStreamBufHandler is NULL");
        return;
    }
    //
    mAllImageStreamInfo[EImageInfo_FullRaw] =
        createRawImageStreamInfo(   "Hal:Image:Fullraw",
                                    STREAM_ID_PASS1_FULLSIZE,
                                    eSTREAMTYPE_IMAGE_INOUT,
                                    6,//TBD
                                    4,//TBD
                                    eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                                    mImgoFormat,
                                    mImgoSize,
                                    mImgoStride);
    //
    ZsdPrvStreamBufHandler::CONFIG_INFO configInfo;
    configInfo.pBufMgr = mPipeMgrParams.pBufMgr;
    configInfo.uiKeepBufCount = ZSD_ROLLBACK_NUM;
    configInfo.pStreamInfo = mAllImageStreamInfo[EImageInfo_FullRaw];
    //
    mspZsdPrvStreamBufHandler->setConfig(configInfo);
    mspZsdPrvStreamBufHandler->init();
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdPrv::
setupPipelineStreamConfig()
{
    PipeMgrImpNorPrv::setupPipelineStreamConfig();
    //
    mvPipeStreamConfig.push_back(
        (PipeStreamConfig){ eNODEID_P1Node,
                            EP1OutImageFull,
                            EImageInfo_FullRaw,
                            STREAM_ID_PASS1_FULLSIZE,
                            eStreamType_IMG_HAL_PROVIDER,
                            eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE});
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdPrv::
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
                if( iter->ePort == EP1OutImageFull )
                {
                    StreamBuilder(iter->streamType, mAllImageStreamInfo[iter->streamInfoId])
                        .setProvider((sp<IStreamBufferProvider>)mspZsdPrvStreamBufHandler)
                        .build(mContext);
                    MY_LOGD2("build stream type=0x%x, imageStreamInfoId=%d, provider=ZsdPrv", iter->streamType, iter->streamInfoId);
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
PipeMgrImpZsdPrv::
finishStreamBufferProvider()
{
    PipeMgrImpNorPrv::finishStreamBufferProvider();
    //
    if(mspZsdPrvStreamBufHandler != NULL)
    {
        mspZsdPrvStreamBufHandler->uninit();
        mspZsdPrvStreamBufHandler->destroyInstance();
        mspZsdPrvStreamBufHandler = NULL;
    }
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<IPipelineFrame>
PipeMgrImpZsdPrv::
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
                    .addOut(STREAM_ID_PASS1_FULLSIZE)
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
PipeMgrImpZsdPrv::
sendMetadata(
    MINT32              requestNumber,
    StreamId_T const    streamId,
    IMetadata*          pMetadata)
{
    PipeMgrImpNorPrv::sendMetadata(
                        requestNumber,
                        streamId,
                        pMetadata);

    if( mPipeMgrParams.pBufMgr != NULL &&
        streamId == STREAM_ID_METADATA_RESULT_P1_HAL)
    {
        mPipeMgrParams.pBufMgr->setMetadata(requestNumber, pMetadata);
    }

    return true;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipeMgrImpZsdPrv::
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

