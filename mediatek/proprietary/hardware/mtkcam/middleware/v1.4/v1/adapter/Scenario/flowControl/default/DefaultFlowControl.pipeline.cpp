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
#include "MyUtils.h"
//
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>

#include <v1/camutils/CamInfo.h>
#include <v1/IParamsManager.h>
#include <LegacyPipeline/IRequestController.h>
#include <LegacyPipeline/ILegacyPipeline.h>
#include <Scenario/IFlowControl.h>
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
#if 0
MERROR
DefaultFlowControl::
constructNormalPreviewPipeline(
    wp<ILegacyPipeline>& rpPipeline,
    MINT32&              rPipelinId
)
{
    sp<LegacyPipelineBuilder> pBuilder = LegacyPipelineBuilder("NormalPreview");

    SensorStaticInfo            aSensorStaticInfo;
    IHalSensorList* pHalSensorList = IHalSensorList::get();
    if( pHalSensorList == NULL ) {
        MY_LOGE("pHalSensorList == NULL");
        return DEAD_OBJECT;
    }
    //
    MUINT32 sensorDev = pHalSensorList->querySensorDevIdx(mOpenId);
    pHalSensorList->querySensorStaticInfo(sensorDev, aSensorStaticInfo);
    //
    IHalSensor* pSensorHalObj = NULL;
    //
    if( !pHalSensorList ) { MY_LOGE("pHalSensorList == NULL"); return DEAD_OBJECT; }

    MUINT const sensorMode = 0;
    MUINT const sensorFps  = aSensorStaticInfo.previewFrameRate;
    MUINT32 pixelMode;
    pSensorHalObj = pHalSensorList->createSensor(LOG_TAG, mOpenId);
    if( pSensorHalObj == NULL ) { MY_LOGE("pSensorHalObj is NULL"); return DEAD_OBJECT; }
    pSensorHalObj->sendCommand(
                    pHalSensorList->querySensorDevIdx(mOpenId),
                    SENSOR_CMD_GET_SENSOR_PIXELMODE,
                    (MUINTPTR)(&sensorMode),
                    (MUINTPTR)(&sensorFps),
                    (MUINTPTR)(&pixelMode));

    pSensorHalObj->destroyInstance(LOG_TAG);

    PipelineSensorParam SensorParam{
        mOpenId,
        sensorMode, // preview
        aSensorStaticInfo.rawFmtType,
        MSize(aSensorStaticInfo.previewWidth, aSensorStaticInfo.previewHeight),
        sensorFps,
        pixelMode
    };
    pBuilder->setSensor(mOpenId, &SensorParam);

    vector<sp<IMetaStreamInfo> > appMetaStreamInfos;
    appMetaStreamInfos.push_back(
        new MetaStreamInfo( "App:Meta:Control",
                            STREAM_ID_METADATA_CONTROL_APP,
                            eSTREAMTYPE_META_IN,
                            0);
    );
    //
    appMetaStreamInfos.push_back(
        new MetaStreamInfo( "App:Meta:ResultP1",
                            STREAM_ID_METADATA_RESULT_P1_APP,
                            eSTREAMTYPE_META_OUT,
                            0);
    );
    //
    appMetaStreamInfos.push_back(
        new MetaStreamInfo( "App:Meta:ResultP2",
                            STREAM_ID_METADATA_RESULT_P2_APP,
                            eSTREAMTYPE_META_OUT,
                            0);
    );
    vector<sp<IMetaStreamInfo> > halMetaStreamInfos;
    halMetaStreamInfos.push_back(
        new MetaStreamInfo( "Hal:Meta:Control",
                            STREAM_ID_METADATA_CONTROL_HAL,
                            eSTREAMTYPE_META_IN,
                            0);
    );
    //
    halMetaStreamInfos.push_back(
        new MetaStreamInfo( "Hal:Meta:ResultP1",
                            STREAM_ID_METADATA_RESULT_P1_HAL,
                            eSTREAMTYPE_META_INOUT,
                            0);
    );
    //
    halMetaStreamInfos.push_back(
        new MetaStreamInfo( "Hal:Meta:ResultP2",
                            STREAM_ID_METADATA_RESULT_P2_HAL,
                            eSTREAMTYPE_META_INOUT,
                            0);
    );
    pBuilder->addMetaStream(appMetaStreamInfos, halMetaStreamInfos);

#define addBufPlane(planes, height, stride)                                      \
        do{                                                                      \
            size_t _height = (size_t)(height);                                   \
            size_t _stride = (size_t)(stride);                                   \
            IImageStreamInfo::BufPlane bufPlane= { _height * _stride, _stride }; \
            planes.push_back(bufPlane);                                          \
        }while(0)
    vector<sp<IImageBuffer> > temp;
    vector<ImageStreamUsage> vImageStreamUsage;
    IImageStreamInfo::BufPlanes_t bufPlanes1;
    vImageStreamUsage.push_back(
        ImageStreamUsage{
            new ImageStreamInfo(
                "Hal:Image:Resiedraw",
                STREAM_ID_PASS1_RESIZE,
                eSTREAMTYPE_IMAGE_INOUT,
                6, 4,
                eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                eImgFmt_FG_BAYER10, MSize(1920, 1080), IImageStreamInfo::BufPlane{}
                ),
            false,
            false,
            STREAM_ID_PASS1_RESIZE,
            0,
            spProvider,
            NULL,
            temp
        }
    );
    //
    IImageStreamInfo::BufPlanes_t bufPlanes2;
    addBufPlane(bufPlanes2, 480, 640 << 1)
    vImageStreamUsage.push_back(
        ImageStreamUsage{
            new ImageStreamInfo(
                "Hal:Image:p2Port1",
                STREAM_ID_PASS2_OUT1,
                eSTREAMTYPE_IMAGE_INOUT,
                5, 1,
                eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE,
                eImgFmt_YUY2, MSize(640, 480), bufPlanes2
                ),
            false,
            false,
            STREAM_ID_PASS2_OUT1,
            0,
            spProvider,
            NULL,
            temp
        }
    );
#undef  addBufPlane

    struct ImageStreamUsage{
        sp<IImageStreamInfo>        spImageStreamInfo;

        //for buffer provider
        MBOOL                       useImgHalPool;
        MBOOL                       isConsumer;
        StreamId_T                  providerStreamId;
        MINT32                      bufferQueueDepth;
        sp<IImgBufProvider>         spProvider;
        sp<IImageCallback>          spCallback;
        vector<sp<IImageBuffer> >   spvBuffers;
    };
    pBuilder->addImageStream(vImageStreamUsage);

    pBuilder->addNode(IPipelineNode::NodeId_T const nodeId, char* const nodeName, vector<NodeStreamUsage> const vNodeStreamUsage);

    pBuilder->setFlow(IPipelineNode::NodeId_T const rootNodeId, vector<NodeEdge> const vNodeEdge);

    pBuilder->create(
                LegacyPipelineManager::getInstance(mOpenId),
                rPipelinId,
                rpPipeline
            );

    return OK;
}
#endif


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
constructZsdPreviewPipeline(
    wp<ILegacyPipeline>& rpPipeline,
    MINT32&              rPipelinId
)
{
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DefaultFlowControl::
constructRecordingPipeline(
    wp<ILegacyPipeline>& rpPipeline,
    MINT32&              rPipelinId
)
{
    return OK;
}
