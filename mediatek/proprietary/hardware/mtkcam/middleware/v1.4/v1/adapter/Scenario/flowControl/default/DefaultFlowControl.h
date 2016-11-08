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

#ifndef _MTK_HARDWARE_MTKCAM_V1_LEGACYPIPELINE_DEFAULT_DEFAULTFLOWCONTROL_H_
#define _MTK_HARDWARE_MTKCAM_V1_LEGACYPIPELINE_DEFAULT_DEFAULTFLOWCONTROL_H_
//
#include <utils/RefBase.h>
#include <utils/StrongPointer.h>
#include <utils/String8.h>
#include <utils/Vector.h>
//
#include <utils/Thread.h>
//
#include <sys/prctl.h>
#include <sys/resource.h>
#include <system/thread_defs.h>
//
#include <metadata/IMetadata.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/utils/streambuf/StreamBufferProvider.h>
#include <v1/Processor/StreamingProcessor.h>
#include <LegacyPipeline/ILegacyPipeline.h>
#include <Scenario/IFlowControl.h>
#include <LegacyPipeline/IRequestController.h>
#include <LegacyPipeline/StreamBufferProvider.h>


typedef NSCam::v3::IMetaStreamInfo          IMetaStreamInfo;
typedef NSCam::v3::IMetaStreamBuffer        IMetaStreamBuffer;
typedef NSCam::IMetadata                    IMetadata;

using namespace android;
#include <v1/IParamsManagerV3.h>

//[workaround]
#include <v3/pipeline/PipelineContext.h>
#include <sensor_hal.h>
#include <IHalSensor.h>
#include <v3/hwpipeline/NodeId.h>
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>
#include <v1/camutils/IBuffer.h>
//#include <v1/camutils/IImgBufQueue.h>

using namespace NSCam::v3;
using namespace NSCam::v3::NSPipelineContext;
using namespace android::MtkCamUtils;
typedef NSCam::v3::Utils::HalMetaStreamBuffer HalMetaStreamBuffer;
//#include <ImgBufProvidersManager.h>
/******************************************************************************
 *
 ******************************************************************************/
//
namespace NSCam {
namespace v1 {

namespace NSLegacyPipeline {


class DefaultFlowControl
    : public IFlowControl
    , public IFeatureFlowControl
    , public IRequestUpdater
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
                                                DefaultFlowControl(
                                                    char const* pcszName,
                                                    MINT32 const i4OpenId,
                                                    sp<IParamsManager> pParamsManager,
                                                    sp<ImgBufProvidersManager> pImgBufProvidersManager
                                                );

    virtual                                     ~DefaultFlowControl() {};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IFlowControl Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    virtual char const*                         getName()   const;

    virtual int32_t                             getOpenId() const;

public:  //// Adapter

    virtual status_t                            startPreview();

    virtual status_t                            stopPreview();

    virtual status_t                            startRecording();

    virtual status_t                            stopRecording();

    virtual status_t                            autoFocus();

    virtual status_t                            cancelAutoFocus();

    virtual status_t                            precapture();

    virtual status_t                            setParameters();

    virtual status_t                            sendCommand(
                                                    int32_t cmd,
                                                    int32_t arg1,
                                                    int32_t arg2
                                                );

public:

    virtual status_t                            dump(
                                                    int fd,
                                                    Vector<String8>const& args
                                                );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IRequestUpdater Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual MERROR                              updateAppSetting(
                                                    IMetadata* setting
                                                );

    virtual MERROR                              updateHalSetting(
                                                    IMetadata* setting
                                                );

    virtual MERROR                              notifySettingRequestNo(
                                                    MINT32    rRequestNo,
                                                    IMetadata* setting
                                                );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IFeatureFlowControl Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual status_t                            queryCurrentPipelineId(int& id);

    virtual MERROR                              submitRequest(
                                                    Vector< IMetadata* >                settings,
                                                    android::sp<StreamBufferProvider>& resultBuffer
                                                ) const;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  RefBase Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual MVOID                               onLastStrongRef( const void* /*id*/);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:  //// pipeline
    MERROR                                      constructNormalPreviewPipeline(
                                                    wp<ILegacyPipeline>& rpPipeline,
                                                    MINT32&              rPipelinId
                                                );

    MERROR                                      constructZsdPreviewPipeline(
                                                    wp<ILegacyPipeline>& rpPipeline,
                                                    MINT32&              rPipelinId
                                                );

    MERROR                                      constructRecordingPipeline(
                                                    wp<ILegacyPipeline>& rpPipeline,
                                                    MINT32&              rPipelinId
                                                );

public:  //// workaround
    virtual sp<IPipelineFrame>                  constructPipelineFrame(
                                                    MINT32  requestNo,
                                                    android::sp<IMetaStreamBuffer> pAppMetaControlSB,
                                                    android::sp<HalMetaStreamBuffer> pHalMetaControlSB
                                                );

    void                                        prepareSensor();

    void                                        prepareConfiguration();

    void                                        setupMetaStreamInfo();

    void                                        setupImageStreamInfo();

    void                                        setupPipelineContext();

    IMetaStreamBuffer*                          createMetaStreamBuffer(
                                                    android::sp<IMetaStreamInfo> pStreamInfo,
                                                    IMetadata const& rSettings,
                                                    MBOOL const repeating
                                                );

    sp<IImageStreamInfo>                        createRawImageStreamInfo(
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

    sp<IImageStreamInfo>                        createImageStreamInfo(
                                                    char const*         streamName,
                                                    StreamId_T          streamId,
                                                    MUINT32             streamType,
                                                    size_t              maxBufNum,
                                                    size_t              minInitBufNum,
                                                    MUINT               usageForAllocator,
                                                    MINT                imgFormat,
                                                    MSize const&        imgSize,
                                                    MUINT32             transform
                                                );

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

protected:
    IHalSensor* mpSensorHalObj;
    //
    MUINT32 gSensorId = 0;
    MUINT32 requestTemplate = CAMERA3_TEMPLATE_PREVIEW;
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
    sp<IImageStreamInfo>        gImage_Yuv;

    sp<ILegacyPipeline>         mpLegacyPipeline;

    sp<StreamBufferProvider>   pCamClientProvider;
    sp<StreamBufferProvider>   pRawProvider;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    MINT32                                      mOpenId;
    MINT32                                      mPipelineId;
    sp<IRequestController>                      mpRequestController;
    sp<IParamsManager>                          mpParamsManager;
    sp<ImgBufProvidersManager>                  mpImgBufProvidersMgr;
    char*                                       mName;
    //sp<CamMsgCbInfo>                            mpCamMsgCbInfo;
};

/******************************************************************************
*
******************************************************************************/
};  //namespace NSPipelineContext
};  //namespace v1
};  //namespace NSCam
#endif  //_MTK_HARDWARE_MTKCAM_V1_LEGACYPIPELINE_DEFAULT_DEFAULTFLOWCONTROL_H_

