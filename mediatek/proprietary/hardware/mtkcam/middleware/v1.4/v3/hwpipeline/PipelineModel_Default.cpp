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

#define LOG_TAG "MtkCam/HwPipeline"
//
#include "MyUtils.h"
//
#include "PipelineModel_Default.h"
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>
//
#include <IHalSensor.h>
//
#include <v3/hwnode/P1Node.h>
#include <v3/hwnode/P2Node.h>
#include <v3/hwnode/FDNode.h>
#include <v3/hwnode/JpegNode.h>
#include <v3/hwnode/RAW16Node.h>
#include <v3/pipeline/PipelineContext.h>
//
#include <mtk_platform_metadata_tag.h>
//
#include <iopipe/CamIO/INormalPipe.h>
using namespace NSCam::NSIoPipe;
#include <imageio/ispio_utility.h>
//
#include "ScenarioControl.h"
#warning "FIXME: pip not ready yet"
#if 0
// PIP
#include <hwutils/CamManager.h>
using namespace NSCam::Utils;
#include <featureio/pip_hal.h>
#endif

using namespace android;
using namespace NSCam;
using namespace NSCam::v3;
using NSCam::v3::Utils::MetaStreamInfo;
using NSCam::v3::Utils::ImageStreamInfo;
using namespace NSCam::v3::NSPipelineContext;
using NSCam::v3::Utils::HalMetaStreamBuffer;
using NSCam::v3::Utils::HalImageStreamBuffer;

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

#define CHECK_ERROR(_err_)                                \
    do {                                                  \
        MERROR const err = (_err_);                       \
        if( err != OK ) {                                 \
            MY_LOGE("err:%d(%s)", err, ::strerror(-err)); \
            return err;                                   \
        }                                                 \
    } while(0)

/******************************************************************************
 *
 ******************************************************************************/
typedef HalMetaStreamBuffer::       Allocator
                                    HalMetaStreamBufferAllocatorT;
/******************************************************************************
 *
 ******************************************************************************/
#define SUPPORT_RECONFIGURE         (0)

#define ALIGN_2(x)     (((x) + 1) & (~1))

static inline
MBOOL
isStream(sp<IStreamInfo> pStreamInfo, StreamId_T streamId )
{
    return pStreamInfo.get() && pStreamInfo->getStreamId() == streamId;
}
// utility functions
static
sp<ImageStreamInfo>     createImageStreamInfo(
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


MBOOL                   getSensorOutputFmt(
                            SensorStaticInfo const& sensorInfo,
                            MUINT32 bitDepth,
                            MBOOL isFull,
                            MINT* pFmt
                        );


MERROR                  alignPass1HwLimitation(
                            MUINT32 const pixelMode,
                            MINT const imgFormat,
                            MSize& size,
                            MBOOL isFull,
                            size_t& stride
                        );


/******************************************************************************
 *
 ******************************************************************************/


/******************************************************************************
 *
 ******************************************************************************/
namespace {
class PipelineDefaultImp
    : public PipelineModel_Default
    , public IPipelineBufferSetFrameControl::IAppCallback
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Definitions.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////

    struct MyConfigParams
    {
        PipeConfigParams            configParams;
        //
        // internal setting
        MBOOL                       skipJpeg;
    };
    //
    class MyProcessedParams
    {
    public:
        MINT32 const                mOpenId;
        /*  input params  */
        PipeConfigParams            mConfigParams;
        // internal setting
        MBOOL                       mSkipJpeg;
        //
        /* static info  */
        MBOOL                       mbSensorInfoQueried;
        SensorStaticInfo            mSensorStaticInfo;
        //
        /*  processed params  */
        // Stream infos
        MBOOL                       mbHasRecording;
        MBOOL                       mbHasRaw;
        MBOOL                       mbHasJpeg;
        MSize                       mMaxStreamSize;
        //
        MBOOL                       mbUseP1Node;
        MBOOL                       mbUseP2Node;
        MBOOL                       mbUseP2VSSNode;
        MBOOL                       mbUseFDNode;
        MBOOL                       mbUseJpegNode;
        MBOOL                       mbUseRaw16Node;
        //
        // sensor related
        MUINT                       mSensorMode;
        MSize                       mSensorSize;
        MUINT                       mSensorFps;
        //
        // p1 related
        MUINT32                     mPixelMode;
        MINT                        mFullrawFormat;
        MSize                       mFullrawSize;
        size_t                      mFullrawStride;
        MINT                        mResizedrawFormat;
        MSize                       mResizedrawSize;
        size_t                      mResizedrawStride;
        //
    public:
                                    MyProcessedParams(MINT32 const openId);
        MERROR                      update(
                                        PipeConfigParams const& rParams,
                                        MBOOL skipJpeg
                                        )
                                    {
                                        // update config params
                                        mConfigParams = rParams;
                                        mSkipJpeg     = skipJpeg; //TODO: use this?
                                        //
                                        MERROR err;
                                        if( OK != (err = querySensorStatics())  ||
                                            OK != (err = preprocess())          ||
                                            OK != (err = decideSensor())        ||
                                            OK != (err = decideP1())
                                          )
                                            return err;
                                        return OK;
                                    }
        //
    protected:
        MERROR                      querySensorStatics();
        MERROR                      preprocess();
        MERROR                      decideSensor();
        MERROR                      decideP1();
    };

    struct parsedAppRequest
    {
        // original AppRequest
        AppRequest* const           pRequest;
        //
        IMetadata*                  pAppMetaControl;
        // in
        KeyedVector< StreamId_T, sp<IImageStreamInfo> >
                                    vIImageInfos_Raw;
        KeyedVector< StreamId_T, sp<IImageStreamInfo> >
                                    vIImageInfos_Yuv;
        // out
        KeyedVector< StreamId_T, sp<IImageStreamInfo> >
                                    vOImageInfos_Raw;
        KeyedVector< StreamId_T, sp<IImageStreamInfo> >
                                    vOImageInfos_Yuv;
        KeyedVector< StreamId_T, sp<IImageStreamInfo> >
                                    vOImageInfos_Jpeg;
        //
                                    parsedAppRequest(AppRequest* pRequest)
                                        : pRequest(pRequest)
                                    {}
                                    ~parsedAppRequest() {
                                        if( pAppMetaControl )
                                            pRequest->vIMetaBuffers[0]->unlock(
                                                    "parsedAppRequest", pAppMetaControl
                                                    );
                                    }
        //
        MERROR                      parse();
    };

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:  ////                    Data Members.
    MINT32 const                    mOpenId;
    android::String8 const          mName;
    android::wp<IPipelineModelMgr::IAppCallback>const
                                    mpAppCallback;
    //
    MyProcessedParams               mParams;
    // meta: hal
    android::sp<IMetaStreamInfo>    mpHalMeta_Control;
    android::sp<IMetaStreamInfo>    mpHalMeta_DynamicP1;
    android::sp<IMetaStreamInfo>    mpHalMeta_DynamicP2;
    // meta: app
    android::sp<IMetaStreamInfo>    mpAppMeta_DynamicP1;
    android::sp<IMetaStreamInfo>    mpAppMeta_DynamicP2;
    android::sp<IMetaStreamInfo>    mpAppMeta_DynamicFD;
    android::sp<IMetaStreamInfo>    mpAppMeta_DynamicJpeg;
    android::sp<IMetaStreamInfo>    mpAppMeta_Control;
    // image: hal
    android::sp<IImageStreamInfo>   mpHalImage_P1_Raw;
    android::sp<IImageStreamInfo>   mpHalImage_P1_ResizerRaw;
    android::sp<IImageStreamInfo>   mpHalImage_FD_YUV;
    android::sp<IImageStreamInfo>   mpHalImage_Jpeg_YUV;
    android::sp<IImageStreamInfo>   mpHalImage_Thumbnail_YUV;

    // image: app
    android::KeyedVector <
            StreamId_T, android::sp<IImageStreamInfo>
                    >               mvAppYuvImage;
    android::sp<IImageStreamInfo>   mpAppImage_Jpeg;
    android::sp<IImageStreamInfo>   mpAppImage_RAW16;

    //// raw/yuv stream mapping
    StreamSet                       mvYuvStreams_Fullraw;
    StreamSet                       mvYuvStreams_Resizedraw;
    //android::Vector<StreamId_T>     mvYuvStreams_Fullraw;
    //android::Vector<StreamId_T>     mvYuvStreams_Resizedraw;

    android::KeyedVector < StreamId_T, MINT64 >
                                    mvStreamDurations;

protected:
    android::RWLock                 mRWLock;
    //
    sp<IScenarioControl>            mpScenarioCtrl;
    sp<PipelineContext>             mpPipelineContext;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IPipelineModel Interfaces.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.
    virtual char const*             getName() const { return mName.string(); }
    virtual MINT32                  getOpenId() const { return mOpenId; }

    virtual MERROR                  submitRequest(AppRequest& request);

    virtual MERROR                  beginFlush();

    virtual MVOID                   endFlush();

    virtual MVOID                   endRequesting() {}

    virtual MVOID                   waitDrained();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  PipelineModel_Default Interfaces.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.
    virtual MERROR                  configure(
                                        PipeConfigParams const& rConfigParams
                                    );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Instantiation.
                                    ~PipelineDefaultImp();
                                    PipelineDefaultImp(
                                        MINT32 const openId,
                                        android::String8 const& name,
                                        wp<IPipelineModelMgr::IAppCallback> pAppCallback
                                    );

private:    ////                    Operations.

    MVOID                           evaluatePreviewSize(
                                        PipeConfigParams const& rConfigParams,
                                        MSize &rSize
                                    );

    MERROR                          setupAppStreamsLocked(
                                        PipeConfigParams const& rConfigParams
                                    );

    MERROR                          setupHalStreamsLocked(
                                        PipeConfigParams const& rConfigParams
                                    );

    MERROR                          configContextLocked_Streams(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_Nodes(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_Pipeline(sp<PipelineContext> pContext);
    //
    MERROR                          configContextLocked_P1Node(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_P2Node(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_P2VSSNode(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_FdNode(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_JpegNode(sp<PipelineContext> pContext);
    MERROR                          configContextLocked_Raw16Node(sp<PipelineContext> pContext);
    //
    MERROR                          configRequestRulesLocked();
    struct evaluateRequestResult
    {
        DefaultKeyedVector<
            StreamId_T,
            sp<IImageStreamInfo>
                >                               vUpdatedImageInfos;
        //
        NodeSet                                 roots;
        NodeEdgeSet                             edges;
        //
        DefaultKeyedVector<NodeId_T, IOMapSet>  nodeIOMapImage;
        DefaultKeyedVector<NodeId_T, IOMapSet>  nodeIOMapMeta;
        //
        DefaultKeyedVector<StreamId_T, sp<IImageStreamBuffer> >
                                                vAppImageBuffers;
        DefaultKeyedVector<StreamId_T, sp<HalImageStreamBuffer> >
                                                vHalImageBuffers;
        DefaultKeyedVector<StreamId_T, sp<IMetaStreamBuffer> >
                                                vAppMetaBuffers;
        DefaultKeyedVector<StreamId_T, sp<HalMetaStreamBuffer> >
                                                vHalMetaBuffers;
    };

    MERROR                          evaluateRequestLocked(
                                        parsedAppRequest const& request,
                                        evaluateRequestResult& result
                                    );

    MERROR                          refineRequestMetaStreamBuffersLocked(
                                        evaluateRequestResult& result
                                    );

    MERROR                          createStreamInfoLocked_Thumbnail_YUV(
                                        IMetadata const* pMetadata,
                                        android::sp<IImageStreamInfo>& rpStreamInfo
                                    ) const;

    MSize                           calcThumbnailYuvSize(
                                        MSize const rPicSize,
                                        MSize const rThumbnailsize
                                    ) const;

    MBOOL                           isFdEnable(
                                        IMetadata const* pMetadata
                                    );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IPipelineBufferSetFrameControl::IAppCallback Interfaces.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.
    virtual MVOID                   updateFrame(
                                        MUINT32 const frameNo,
                                        MINTPTR const userId,
                                        Result const& result
                                    );

private:
    // utility functions
    sp<ImageStreamInfo>             createRawImageStreamInfo(
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

    MBOOL                           skipStream(
                                        MBOOL skipJpeg,
                                        IImageStreamInfo* pStreamInfo
                                    ) const;
};
};  //namespace


/******************************************************************************
 *
 ******************************************************************************/
PipelineModel_Default*
PipelineModel_Default::
create(MINT32 const openId, wp<IPipelineModelMgr::IAppCallback> pAppCallback)
{
    String8 const name = String8::format("%s:%d", magicName(), openId);
    PipelineModel_Default* pPipelineModel = new PipelineDefaultImp(openId, name, pAppCallback);
    if  ( ! pPipelineModel ) {
        MY_LOGE("fail to new an instance");
        return NULL;
    }
    //
    return pPipelineModel;
}


/******************************************************************************
 *
 ******************************************************************************/
PipelineDefaultImp::MyProcessedParams::
MyProcessedParams(MINT32 const openId)
    : mOpenId(openId)
    //
    , mConfigParams()
    , mSkipJpeg(MFALSE)
    //
    , mbSensorInfoQueried(MFALSE)
{
    memset(&mSensorStaticInfo, 0, sizeof(SensorStaticInfo));
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::MyProcessedParams::
querySensorStatics()
{
    if( mbSensorInfoQueried ) return OK;
    //
    IHalSensorList* pSensorList = IHalSensorList::get();
    if( pSensorList == NULL ) {
        MY_LOGE("pSensorList == NULL");
        return DEAD_OBJECT;
    }
    //
    MUINT32 sensorDev = pSensorList->querySensorDevIdx(mOpenId);
    pSensorList->querySensorStaticInfo(sensorDev, &mSensorStaticInfo);
    //
    mbSensorInfoQueried = MTRUE;
    //
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::MyProcessedParams::
preprocess()
{
#define hasUsage(flag, usage) ((flag & usage) == usage)
    MBOOL hasVRConsumer = MFALSE;
    for (size_t i = 0; i < mConfigParams.vImage_Yuv_NonStall.size(); i++) {
        if  ( hasUsage(
                    mConfigParams.vImage_Yuv_NonStall[i]->getUsageForConsumer(),
                    GRALLOC_USAGE_HW_VIDEO_ENCODER
                    ) )
        {
            hasVRConsumer = MTRUE;
            break;
        }
    }
#undef hasUsage
    //
    MSize maxStreamSize;
    {
        struct Log
        {
            static String8
                skippedStream(IImageStreamInfo* pStreamInfo)
                {
                    return String8::format(
                            "skipped stream - format:0x%x type:%x size:%dx%d",
                            pStreamInfo->getImgFormat(), pStreamInfo->getStreamType(),
                            pStreamInfo->getImgSize().w, pStreamInfo->getImgSize().h
                            );
                }

            static String8
                candidateStream(IImageStreamInfo* pStreamInfo)
                {
                    return String8::format(
                            "candidate stream - format:0x%x type:%x size:%dx%d",
                            pStreamInfo->getImgFormat(), pStreamInfo->getStreamType(),
                            pStreamInfo->getImgSize().w, pStreamInfo->getImgSize().h
                            );
                }
        };
        //
        if  ( IImageStreamInfo* pStreamInfo = mConfigParams.pImage_Raw.get() ) {
            if  ( pStreamInfo->getStreamType() == eSTREAMTYPE_IMAGE_IN ) {
                MY_LOGD("%s", Log::skippedStream(pStreamInfo).string());
            }
            else {
                MY_LOGD("%s", Log::candidateStream(pStreamInfo).string());
                maxStreamSize = pStreamInfo->getImgSize();
            }
        }
        //
        if  ( IImageStreamInfo* pStreamInfo = mConfigParams.pImage_Jpeg_Stall.get() ) {
            MY_LOGD("%s", Log::candidateStream(pStreamInfo).string());
            if  ( maxStreamSize.size() <= pStreamInfo->getImgSize().size() ) {
                maxStreamSize = pStreamInfo->getImgSize();
            }
        }
        //
        for (size_t i = 0; i < mConfigParams.vImage_Yuv_NonStall.size(); i++) {
            if  ( IImageStreamInfo* pStreamInfo = mConfigParams.vImage_Yuv_NonStall[i].get()) {
                MY_LOGD("%s", Log::candidateStream(pStreamInfo).string());
                if  ( maxStreamSize.size() <= pStreamInfo->getImgSize().size()) {
                    maxStreamSize = pStreamInfo->getImgSize();
                }
            }
        }
    }
    //
    // update processed params
    mbHasRecording = hasVRConsumer;
    mbHasRaw       = mConfigParams.pImage_Raw.get() ? MTRUE : MFALSE;
    mbHasJpeg      = mConfigParams.pImage_Jpeg_Stall.get() ? MTRUE : MFALSE;
    mMaxStreamSize = maxStreamSize;
    MY_LOGD("max stream(%d, %d), raw(%d), jpeg(%d), hasRecording(%d)",
            mMaxStreamSize.w, mMaxStreamSize.h, mbHasRaw, mbHasJpeg, mbHasRecording);
    //
    mbUseP1Node    = MTRUE;
    mbUseP2Node    = MTRUE;
    mbUseP2VSSNode = hasVRConsumer && mbHasJpeg;
#warning "[FIXME] fd not ready"
    mbUseFDNode    = MFALSE;//MTRUE;
    mbUseJpegNode  = mbHasJpeg;
    mbUseRaw16Node = mbHasRaw && (eImgFmt_RAW16 == mConfigParams.pImage_Raw->getImgFormat());
    //
    return OK;
};


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::MyProcessedParams::
decideSensor()
{
    struct sensormodeHelper
    {
        // use enum to select sensor mode if have preferred sensor mode.
        enum
        {
            eNORMAL_PREVIEW = 0,
            eNORMAL_VIDEO,
            eNORMAL_CAPTURE,
            eNUM_SENSOR_MODE,
        };
        //
                    sensormodeHelper(
                            SensorStaticInfo const& staticInfo,
                            MyProcessedParams* rpParams
                            )
                        : selectIdx(-1)
                        , pParams(rpParams)
                    {
                    #define addMode(idx, _scenarioId_, _key_)                                           \
                        do {                                                                            \
                            sensorSize[idx] = MSize(staticInfo._key_##Width, staticInfo._key_##Height); \
                            sensorFps [idx] = staticInfo._key_##FrameRate/10;                           \
                            sensorMode[idx] = _scenarioId_;                                             \
                        } while(0)
                        addMode(eNORMAL_PREVIEW, SENSOR_SCENARIO_ID_NORMAL_PREVIEW, preview);
                        addMode(eNORMAL_VIDEO  , SENSOR_SCENARIO_ID_NORMAL_VIDEO  , video);
                        addMode(eNORMAL_CAPTURE, SENSOR_SCENARIO_ID_NORMAL_CAPTURE, capture);
                    #undef addMode
                    };
                    ~sensormodeHelper()
                    {
                        if( selectIdx != -1 ) {
                            pParams->mSensorMode = sensorMode[selectIdx];
                            pParams->mSensorSize = sensorSize[selectIdx];
                            pParams->mSensorFps  = sensorFps [selectIdx];
                            //
                            refineFps_MultiOpen(pParams->mSensorMode, pParams->mSensorFps);
                            //
                            MY_LOGD("select mode %d, size(%d, %d)@%d",
                                    pParams->mSensorMode,
                                    pParams->mSensorSize.w, pParams->mSensorSize.h,
                                    pParams->mSensorFps
                                   );
                        } else {
                            MY_LOGW("sensor mode is not selected!");
                            for( int i = 0; i < eNUM_SENSOR_MODE; i++ ) {
                                MY_LOGD("mode %d, size(%d, %d)@%d",
                                        sensorMode[i],
                                        sensorSize[i].w, sensorSize[i].h,
                                        sensorFps[i]
                                       );
                            }
                        }
                    }
        MVOID       refineFps_MultiOpen(MUINT const mode, MUINT& fps)
                    {
#warning "FIXME: pip not ready yet"
#if 0
                        if( CamManager::getInstance()->isMultiDevice() )
                        {
                            MUINT32 prvMaxFR = 30, capMaxFR = 30;
                            {
                                PipHal* pPip = PipHal::createInstance(pParams->mOpenId);
                                if( !pPip->Init() ||
                                        !pPip->GetMaxFrameRate(capMaxFR, prvMaxFR)
                                  )
                                {
                                    MY_LOGE("error in query framerates from pip hal");
                                }
                                pPip->Uninit();
                                pPip->destroyInstance(LOG_TAG);
                            }
                            //
                            switch (mode)
                            {
                                case SENSOR_SCENARIO_ID_NORMAL_PREVIEW:
                                    if( fps > prvMaxFR ) {
                                        fps = prvMaxFR;
                                        MY_LOGD("multi open mode %d, limit fps to %d", mode, fps);
                                    }
                                    break;
                                case SENSOR_SCENARIO_ID_NORMAL_CAPTURE:
                                    if( fps > capMaxFR ) {
                                        fps = capMaxFR;
                                        MY_LOGD("multi open mode %d, limit fps to %d", mode, fps);
                                    }
                                default:
                                    MY_LOGE("PIP not implenmented yet!");
                                    break;
                            };
                        };
#endif
                    }
                    //
        MSize                       sensorSize[eNUM_SENSOR_MODE];
        MUINT                       sensorFps [eNUM_SENSOR_MODE];
        MUINT                       sensorMode[eNUM_SENSOR_MODE];
        //
        int                         selectIdx;
        MyProcessedParams* const    pParams;
    } aHelper(mSensorStaticInfo, this);
    //
    // 1. Raw stream configured: find sensor mode with raw size.
    if  ( IImageStreamInfo* pStreamInfo = mConfigParams.pImage_Raw.get() ) {
        bool hit = false;
        for (int i = 0; i < sensormodeHelper::eNUM_SENSOR_MODE; i++) {
            if  (pStreamInfo->getImgSize() == aHelper.sensorSize[i]) {
                aHelper.selectIdx = i;
                hit = true;
                break;
            }
        }
        if( !hit ) {
            MY_LOGE("Can't find sesnor size that equals to raw size");
            return UNKNOWN_ERROR;
        }
    } else
    // 2. if has VR consumer: sensor video mode is preferred
    if  ( mbHasRecording ) {
        aHelper.selectIdx = sensormodeHelper::eNORMAL_VIDEO;
    }
    else {
        //policy:
        //    find the smallest size that is "larger" than max of stream size
        //    (not the smallest difference)
        bool hit = false;
        for (int i = 0; i < sensormodeHelper::eNUM_SENSOR_MODE; i++) {
            if  ( mMaxStreamSize.w <= aHelper.sensorSize[i].w &&
                  mMaxStreamSize.h <= aHelper.sensorSize[i].h )
            {
                aHelper.selectIdx = i;
                hit = true;
                break;
            }
        }
        if( !hit ) {
            // pick largest one
            MY_LOGW("select capture mode");
            aHelper.selectIdx = sensormodeHelper::eNORMAL_CAPTURE;
        }
    }
    //
    return OK;
};


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::MyProcessedParams::
decideP1()
{
    struct refine
    {
        static
            MVOID       not_larger_than(MSize& size, MSize const& limit) {
                if( size.w > limit.w ) size.w = limit.w;
                if( size.h > limit.h ) size.h = limit.h;
            }
        static
            MVOID       not_smaller_than(MSize& size, MSize const& limit) {
                if( size.w < limit.w ) size.w = limit.w;
                if( size.h < limit.h ) size.h = limit.h;
            }
        static
            MSize       align_2(MSize const& size) {
#define align2(x)  ((x+1) & (~1))
                return MSize(align2(size.w), align2(size.h));
#undef align2
            }
        static
            MSize       scale_roundup(MSize const& size, int mul, int div) {
                return MSize(((size.w + div-1)* mul) / div, ((size.h + div-1)* mul) / div);
            }
    };
    //
    struct update_pixel_mode
    {
        MERROR  operator() (MyProcessedParams* pParams) {
            MINT32 const openId    = pParams->mOpenId;
            MUINT const sensorMode = pParams->mSensorMode;
            MUINT const sensorFps  = pParams->mSensorFps;
            MUINT32 pixelMode;
            //
            IHalSensor* pSensorHalObj = NULL;
            IHalSensorList* const pHalSensorList = IHalSensorList::get();
            //
            if( !pHalSensorList ) { MY_LOGE("pHalSensorList == NULL"); return DEAD_OBJECT; }

            pSensorHalObj = pHalSensorList->createSensor(LOG_TAG, openId);
            if( pSensorHalObj == NULL ) { MY_LOGE("pSensorHalObj is NULL"); return DEAD_OBJECT; }

            pSensorHalObj->sendCommand(
                    pHalSensorList->querySensorDevIdx(openId),
                    SENSOR_CMD_GET_SENSOR_PIXELMODE,
                    (MUINTPTR)(&sensorMode),
                    (MUINTPTR)(&sensorFps),
                    (MUINTPTR)(&pixelMode));

            pSensorHalObj->destroyInstance(LOG_TAG);

            if( pixelMode != 0 && pixelMode != 1 ) {
                MY_LOGE("Un-supported pixel mode %d", pixelMode);
                return BAD_VALUE;
            }
            //
            pParams->mPixelMode = pixelMode;
            return OK;
        }
    };
    //
    if( OK != update_pixel_mode()(this) )
        return UNKNOWN_ERROR;
    //
    MSize const sensorSize = mSensorSize;
    //
#define MAX_PREVIEW_W           (1920)
#define MAX_RRZO_HW_W           (2304) // = 1920*1.2
#define MIN_RRZO_EIS_W          (1280)
#define EIS_RATIO_10X           (12)  // src/dst = 1.2
#define RRZO_SCALE_RATIO_10X    (4)
    // estimate preview yuv max size
    MSize const max_preview_size = refine::align_2(
            MSize(MAX_PREVIEW_W, MAX_PREVIEW_W * sensorSize.h / sensorSize.w));
    //
    MSize maxYuvStreamSize;
    MBOOL bLargeYuvStream = MFALSE;
    for (size_t i = 0; i < mConfigParams.vImage_Yuv_NonStall.size(); i++ )
    {
        MSize const streamSize = mConfigParams.vImage_Yuv_NonStall[i]->getImgSize();
        // if stream's size is suitable to use rrzo
        if( streamSize.w <= max_preview_size.w && streamSize.h <= max_preview_size.h )
            refine::not_smaller_than(maxYuvStreamSize, streamSize);
        else
            bLargeYuvStream = MTRUE;
    }
    MY_LOGD_IF( !!maxYuvStreamSize, "max yuv stream size(%dx%d)",
            maxYuvStreamSize.w, maxYuvStreamSize.h);
    //
    // use resized raw if
    // 1. raw sensor
    // 2. some streams need this
    if( mSensorStaticInfo.sensorType == SENSOR_TYPE_RAW )
    {
        //
        // currently, should always enable resized raw due to some reasons...
        //
        // initial value
        MSize target_rrzo_size = maxYuvStreamSize;
        // apply limitations
        //  1. lower bounds
        {
            MSize const min_rrzo_eis_size = refine::align_2(
                    MSize(MIN_RRZO_EIS_W, MIN_RRZO_EIS_W * sensorSize.h / sensorSize.w));
            MSize const min_rrzo_sensor_scale = refine::align_2(
                    refine::scale_roundup(sensorSize, RRZO_SCALE_RATIO_10X, 10)
                    );
            //
            refine::not_smaller_than(target_rrzo_size, min_rrzo_eis_size);
            target_rrzo_size = refine::align_2(
                    refine::scale_roundup(target_rrzo_size, EIS_RATIO_10X, 10)
                    );
            refine::not_smaller_than(target_rrzo_size, min_rrzo_sensor_scale);
        }
        //  2. upper bounds
        {
            MSize const max_rrzo_hw_size = refine::align_2(
                    MSize(MAX_RRZO_HW_W, MAX_RRZO_HW_W * sensorSize.h / sensorSize.w));
            //
            refine::not_larger_than(target_rrzo_size, max_rrzo_hw_size);
            refine::not_larger_than(target_rrzo_size, sensorSize);
        }
        //
        MY_LOGD_IF(1, "rrzo size(%dx%d)", target_rrzo_size.w, target_rrzo_size.h);
        //
        mResizedrawSize = target_rrzo_size;
        getSensorOutputFmt(mSensorStaticInfo, 10, MFALSE, &mResizedrawFormat);
        // check hw limitation with pixel mode & stride
        if( OK != alignPass1HwLimitation(
                    mPixelMode,
                    mResizedrawFormat,
                    mResizedrawSize,
                    MFALSE,
                    mResizedrawStride
                    )
          )
        {
            MY_LOGE("cannot align to hw limitation: resize");
            return BAD_VALUE;
        }
        MY_LOGD_IF(1, "rrzo size(%dx%d) stride %d",
                mResizedrawSize.w, mResizedrawSize.h, mResizedrawStride);
    }
    //
    // use full raw, if
    // 1. jpeg stream (&& not met BW limit)
    // 2. raw stream
    // 3. or stream's size is beyond rrzo's limit
    MBOOL useImgo =
        (mbHasJpeg && ! mbHasRecording) ||
        mbHasRaw ||
        bLargeYuvStream;
    if( useImgo )
    {
        mFullrawSize = sensorSize;
        getSensorOutputFmt(mSensorStaticInfo, 10, MTRUE, &mFullrawFormat);
        // check hw limitation with pixel mode & stride
        if( OK != alignPass1HwLimitation(
                    mPixelMode,
                    mFullrawFormat,
                    mFullrawSize,
                    MTRUE,
                    mFullrawStride
                    )
          )
        {
            MY_LOGE("cannot align to hw limitation: full");
            return BAD_VALUE;
        }
        MY_LOGD_IF(1, "imgo size(%dx%d) stride %d",
                mFullrawSize.w, mFullrawSize.h, mFullrawStride);
    }
    else
    {
        mFullrawSize = MSize(0,0);
    }
    return OK;
};


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::parsedAppRequest::
parse()
{
    struct categorize_img_stream
    {
        typedef KeyedVector< StreamId_T, sp<IImageStreamBuffer> >       IImageSBMapT;
        typedef KeyedVector< StreamId_T, sp<IImageStreamInfo> >         IImageInfoMapT;
        MERROR      operator()(
                IImageSBMapT& map,
                IImageInfoMapT* pMapRaw,
                IImageInfoMapT* pMapYuv,
                IImageInfoMapT* pMapJpeg
                )
        {
            for(size_t i = 0; i < map.size(); i++ )
            {
                sp<IImageStreamBuffer> buf = map.valueAt(i);
                if ( IImageStreamInfo const* pStreamInfo = buf->getStreamInfo() )
                {
                    IImageInfoMapT* pTargetMap = NULL;
                    switch( pStreamInfo->getImgFormat() )
                    {
                        //case eImgFmt_BAYER10: //TODO: not supported yet
                        //case eImgFmt_BAYER12:
                        //case eImgFmt_BAYER14:
                        case eImgFmt_RAW16:
                            pTargetMap = pMapRaw;
                            break;
                            //
                        case eImgFmt_BLOB:
                            pTargetMap = pMapJpeg;
                            break;
                            //
                        case eImgFmt_YV12:
                        case eImgFmt_NV21:
                        case eImgFmt_YUY2:
                        case eImgFmt_Y8:
                        case eImgFmt_Y16:
                            pTargetMap = pMapYuv;
                            break;
                            //
                        default:
                            MY_LOGE("Unsupported format:0x%x", pStreamInfo->getImgFormat());
                            break;
                    }
                    if( pTargetMap == NULL ) {
                        MY_LOGE("cannot get target map");
                        return UNKNOWN_ERROR;
                    }
                    //
                    pTargetMap->add(
                            pStreamInfo->getStreamId(),
                            const_cast<IImageStreamInfo*>(pStreamInfo)
                            );
                }
            }
            return OK;
        }
    };
    //
    pAppMetaControl = pRequest->vIMetaBuffers[0]->tryReadLock("parsedAppRequest");
    if( ! pAppMetaControl ) {
        MY_LOGE("cannot get control meta");
        return UNKNOWN_ERROR;
    }
    //
    CHECK_ERROR(
            categorize_img_stream() (
                pRequest->vIImageBuffers,
                &vIImageInfos_Raw, &vIImageInfos_Yuv, NULL
                )
            );
    CHECK_ERROR(
            categorize_img_stream() (
                pRequest->vOImageBuffers,
                &vOImageInfos_Raw, &vOImageInfos_Yuv, &vOImageInfos_Jpeg
                )
            );
    //
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
PipelineDefaultImp::
PipelineDefaultImp(
    MINT32 const openId,
    android::String8 const& name,
    wp<IPipelineModelMgr::IAppCallback> pAppCallback
)
    : mOpenId(openId)
    , mName(name)
    , mpAppCallback(pAppCallback)
    //
    , mParams(openId)
    //
    , mpHalMeta_DynamicP1()
    , mpAppMeta_DynamicP1()
    , mpAppMeta_DynamicP2()
    , mpAppMeta_DynamicFD()
    , mpAppMeta_DynamicJpeg()
    , mpAppMeta_Control()
    //
    , mpHalImage_P1_Raw()
    , mpHalImage_P1_ResizerRaw()
    , mpHalImage_FD_YUV()
    , mpHalImage_Jpeg_YUV()
    , mpHalImage_Thumbnail_YUV()
    //
    , mvAppYuvImage()
    , mpAppImage_Jpeg()
    , mpAppImage_RAW16()
    //
    , mpScenarioCtrl()
{
}


/******************************************************************************
 *
 ******************************************************************************/
PipelineDefaultImp::
~PipelineDefaultImp()
{
    MY_LOGD("default pipeline destroyed");
}


/******************************************************************************
 *
 ******************************************************************************/
/**
 * Given:
 *      App input meta streams
 *      App in/out image streams
 *
 * Action:
 *      Determine CONFIG stream set
 *      Determine I/O streams of each node
 *      Prepare Hal stream pools
 *      Configure each node (with their streams)
 *
 */
MERROR
PipelineDefaultImp::
configure(
    PipeConfigParams const& rConfigParams
)
{
    FUNC_START;
    RWLock::AutoWLock _l(mRWLock);
    //
    mParams.update(rConfigParams, MFALSE);
    //
    MY_LOGE_IF(mpPipelineContext.get(), "strong count %d", mpPipelineContext->getStrongCount());
    mpPipelineContext = PipelineContext::create("DefaultPipeline");
    //
    CHECK_ERROR(mpPipelineContext->beginConfigure());
    //
    // create IStreamInfos
    CHECK_ERROR(setupAppStreamsLocked(rConfigParams));
    CHECK_ERROR(setupHalStreamsLocked(rConfigParams));
    //
    // config stream
    CHECK_ERROR(configContextLocked_Streams(mpPipelineContext));
    // config node
    CHECK_ERROR(configContextLocked_Nodes(mpPipelineContext));
    // config pipeline
    CHECK_ERROR(configContextLocked_Pipeline(mpPipelineContext));
    //
    CHECK_ERROR(mpPipelineContext->endConfigure());
    //
    CHECK_ERROR(configRequestRulesLocked());
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
submitRequest(AppRequest& request)
{
    parsedAppRequest aParsedRequest(&request);
    CHECK_ERROR( aParsedRequest.parse() );
    //
    RWLock::AutoRLock _l(mRWLock);
    //
    RequestBuilder builder;
    //
    evaluateRequestResult evaluateResult;
    //
    CHECK_ERROR( evaluateRequestLocked(aParsedRequest, evaluateResult) );
    //
    CHECK_ERROR( refineRequestMetaStreamBuffersLocked(evaluateResult) );
    //
    builder.setRootNode( evaluateResult.roots );
    builder.setNodeEdges( evaluateResult.edges );
    //
    for( size_t i = 0; i < evaluateResult.vUpdatedImageInfos.size(); i++ )
    {
        builder.replaceStreamInfo(
                evaluateResult.vUpdatedImageInfos.keyAt(i),
                evaluateResult.vUpdatedImageInfos.valueAt(i)
                );
    }
    //
#define try_setIOMap(_nodeId_)                                                        \
    do {                                                                              \
        ssize_t idx_image = evaluateResult.nodeIOMapImage.indexOfKey(_nodeId_);       \
        ssize_t idx_meta  = evaluateResult.nodeIOMapMeta.indexOfKey(_nodeId_);        \
        builder.setIOMap(                                                             \
                _nodeId_,                                                             \
                (0 <= idx_image ) ?                                                   \
                evaluateResult.nodeIOMapImage.valueAt(idx_image) : IOMapSet::empty(), \
                (0 <= idx_meta ) ?                                                    \
                evaluateResult.nodeIOMapMeta.valueAt(idx_meta) : IOMapSet::empty()    \
                );                                                                    \
    } while(0)
    //
    try_setIOMap(eNODEID_P1Node);
    try_setIOMap(eNODEID_P2Node);
    try_setIOMap(eNODEID_P2Node_VSS);
    try_setIOMap(eNODEID_RAW16Out);
    try_setIOMap(eNODEID_FDNode);
    try_setIOMap(eNODEID_JpegNode);
    //
#undef try_setIOMap
    //
#define setStreamBuffers(_sb_type_, _type_, _vStreamBuffer_, _builder_)    \
    do {                                                                   \
        for (size_t i = 0; i < _vStreamBuffer_.size(); i++ )               \
        {                                                                  \
            StreamId_T streamId                = _vStreamBuffer_.keyAt(i); \
            sp<_sb_type_> buffer = _vStreamBuffer_.valueAt(i);             \
            _builder_.set##_type_##StreamBuffer(streamId, buffer);         \
        }                                                                  \
    } while(0)
    //
    setStreamBuffers(IImageStreamBuffer  , Image, evaluateResult.vAppImageBuffers, builder);
    setStreamBuffers(HalImageStreamBuffer, Image, evaluateResult.vHalImageBuffers, builder);
    setStreamBuffers(IMetaStreamBuffer   , Meta , evaluateResult.vAppMetaBuffers , builder);
    setStreamBuffers(HalMetaStreamBuffer , Meta , evaluateResult.vHalMetaBuffers , builder);
#undef setStreamBuffers
    //
    //
    sp<IPipelineFrame> pFrame = builder
        .updateFrameCallback(this)
        .build(request.requestNo, mpPipelineContext);
    if( ! pFrame.get() )
        return UNKNOWN_ERROR;
    //
    return mpPipelineContext->queue(pFrame);
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
beginFlush()
{
    FUNC_START;
    //
    if( mpPipelineContext.get() )
        mpPipelineContext->flush();
    else
        MY_LOGW("no context");
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipelineDefaultImp::
endFlush()
{
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipelineDefaultImp::
waitDrained()
{
    FUNC_START;
    if( mpPipelineContext.get() )
        mpPipelineContext->waitUntilDrained();
    FUNC_END;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
PipelineDefaultImp::
skipStream(
    MBOOL skipJpeg,
    IImageStreamInfo* pStreamInfo
) const
{
    if  (
            skipJpeg
        &&  pStreamInfo->getImgFormat() == HAL_PIXEL_FORMAT_BLOB
        &&  pStreamInfo->getImgSize().size() >= 1920*1080
        )
    {
 //&& limited mode
        return MTRUE;
    }

    return MFALSE;
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
        MY_LOGE("create ImageStream failed, %s, %#" PRIxPTR,
                streamName, streamId);
    }

    return pStreamInfo;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipelineDefaultImp::
evaluatePreviewSize(
    PipeConfigParams const& rConfigParams,
    MSize &rSize
)
{
    sp<IImageStreamInfo> pStreamInfo;
    int consumer_usage = 0;
    int allocate_usage = 0;
    int maxheight = rSize.h;
    int prevwidth = 0;
    int prevheight = 0;
    for (size_t i = 0; i < rConfigParams.vImage_Yuv_NonStall.size(); i++) {
        if  ( (pStreamInfo = rConfigParams.vImage_Yuv_NonStall[i]) != 0 ) {
            consumer_usage = pStreamInfo->getUsageForConsumer();
            allocate_usage = pStreamInfo->getUsageForAllocator();
            MY_LOGD("consumer : %X, allocate : %X", consumer_usage, allocate_usage);
            if(consumer_usage & GRALLOC_USAGE_HW_TEXTURE) {
                prevwidth = pStreamInfo->getImgSize().w;
                prevheight = pStreamInfo->getImgSize().h;
                break;
            }
            if(consumer_usage & GRALLOC_USAGE_HW_VIDEO_ENCODER) {
                continue;
            }
            prevwidth = pStreamInfo->getImgSize().w;
            prevheight = pStreamInfo->getImgSize().h;
        }
    }
    if(prevwidth == 0 || prevheight == 0)
        return ;
    rSize.h = prevheight * rSize.w / prevwidth;
    if(maxheight < rSize.h) {
        MY_LOGW("Warning!!,  scaled preview height(%d) is larger than max height(%d)", rSize.h, maxheight);
        rSize.h = maxheight;
    }
    MY_LOGD("evaluate preview size : %dx%d", prevwidth, prevheight);
    MY_LOGD("FD buffer size : %dx%d", rSize.w, rSize.h);
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
setupAppStreamsLocked(
    PipeConfigParams const& rConfigParams
)
{
    mvStreamDurations.clear();

    //App:Meta:Control
    {
        sp<IMetaStreamInfo> pStreamInfo;
        if  ( (pStreamInfo = rConfigParams.pMeta_Control) != 0 )
        {
            mpAppMeta_Control = pStreamInfo;
            pStreamInfo->setMaxBufNum(10);
        }
    }

    //App:dynamic
    if( mParams.mbUseP1Node )
    {
        //App:Meta:01
        //   pass1 result meta
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "App:Meta:DynamicP1",
                    eSTREAMID_META_APP_DYNAMIC_01,
                    eSTREAMTYPE_META_OUT,
                    10, 1
                    );
        mpAppMeta_DynamicP1 = pStreamInfo;
    }

    if( mParams.mbUseP2Node )
    {
        //App:Meta:P2
        //   pass2 result meta
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "App:Meta:DynamicP2",
                    eSTREAMID_META_APP_DYNAMIC_02,
                    eSTREAMTYPE_META_OUT,
                    10, 1
                    );
        mpAppMeta_DynamicP2 = pStreamInfo;
    }


    if( mParams.mbUseFDNode )
    {
        //App:Meta:FD
        //   FD result meta
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "App:Meta:FD",
                    eSTREAMID_META_APP_DYNAMIC_FD,
                    eSTREAMTYPE_META_OUT,
                    10, 1
                    );
        mpAppMeta_DynamicFD = pStreamInfo;
    }

    if( mParams.mbUseJpegNode )
    {
        //App:Meta:Jpeg
        //   Jpeg result meta
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "App:Meta:Jpeg",
                    eSTREAMID_META_APP_DYNAMIC_JPEG,
                    eSTREAMTYPE_META_OUT,
                    10, 1
                    );
        mpAppMeta_DynamicJpeg = pStreamInfo;
    }


    //App:Image
    {
        sp<IImageStreamInfo> pStreamInfo;
        //
        //App:Image:Raw
        if  ( (pStreamInfo = rConfigParams.pImage_Raw) != 0
                 &&  eImgFmt_RAW16 == pStreamInfo->getImgFormat()
        ) {
            // RAW16 = rConfigParams.pImage_Raw set this stream
            mpAppImage_RAW16 = pStreamInfo;
            pStreamInfo->setMaxBufNum(1);
            //
            mvStreamDurations.add(
                    pStreamInfo->getStreamId(),
                    rConfigParams.mImage_Raw_Duration.minDuration
                    );
        }
        //
        //App:Image:Jpeg:Stall
        if  ( (pStreamInfo = rConfigParams.pImage_Jpeg_Stall) != 0 ) {
            mpAppImage_Jpeg = pStreamInfo;
            pStreamInfo->setMaxBufNum(1);
            //
            mvStreamDurations.add(
                    pStreamInfo->getStreamId(),
                    rConfigParams.mImage_Jpeg_Duration.minDuration
                    );
        }
        //
        //App:Image:Yuv:NotStall
        for (size_t i = 0; i < rConfigParams.vImage_Yuv_NonStall.size(); i++) {
            if  ( (pStreamInfo = rConfigParams.vImage_Yuv_NonStall[i]) != 0 ) {
                mvAppYuvImage.add(pStreamInfo->getStreamId(), pStreamInfo);
                pStreamInfo->setMaxBufNum(8);
                //
                if( i >= rConfigParams.vImage_Yuv_Duration.size() ) {
                    MY_LOGE("not enough yuv duration for streams");
                    continue;
                }
                mvStreamDurations.add(
                        pStreamInfo->getStreamId(),
                        rConfigParams.vImage_Yuv_Duration[i].minDuration
                        );
            }
        }
        //
        // dump durations
        String8 durations = String8("durations:");
        for( size_t i = 0; i < mvStreamDurations.size(); i++) {
            durations += String8::format("(stream %#" PRIxPTR ": %lld) ",
                    mvStreamDurations.keyAt(i), (long long int)mvStreamDurations.valueAt(i));
        }
        MY_LOGD("%s", durations.string());
    }

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
setupHalStreamsLocked(
    PipeConfigParams const& rConfigParams
)
{
    //Hal:Meta
    //
    if( 1 )
    {
        //Hal:Meta:Control
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "Hal:Meta:Control",
                    eSTREAMID_META_PIPE_CONTROL,
                    eSTREAMTYPE_META_IN,
                    10, 1
                    );
        mpHalMeta_Control = pStreamInfo;
    }
    //
    if( mParams.mbUseP1Node )
    {
        //Hal:Meta:01
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "Hal:Meta:P1:Dynamic",
                    eSTREAMID_META_PIPE_DYNAMIC_01,
                    eSTREAMTYPE_META_INOUT,
                    10, 1
                    );
        mpHalMeta_DynamicP1 = pStreamInfo;
    }
    //
    if( mParams.mbUseP2Node )
    {
        //Hal:Meta:01
        sp<IMetaStreamInfo> pStreamInfo =
            new MetaStreamInfo(
                    "Hal:Meta:P2:Dynamic",
                    eSTREAMID_META_PIPE_DYNAMIC_02,
                    eSTREAMTYPE_META_INOUT,
                    10, 1
                    );
        mpHalMeta_DynamicP2 = pStreamInfo;
    }
    //Hal:Image
    if ( mParams.mbUseP1Node &&
            !! mParams.mFullrawSize.size() )
    {
        // p1: fullsize
        MSize const& size = mParams.mFullrawSize;
        MINT const format = mParams.mFullrawFormat;
        size_t const stride = mParams.mFullrawStride;
        MUINT const usage = 0;//eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READWRITE;
        sp<IImageStreamInfo>
            pStreamInfo = createRawImageStreamInfo(
                    "Hal:Image:P1:Fullraw",
                    eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00,
                    eSTREAMTYPE_IMAGE_INOUT,
                    6, 4,
                    usage, format, size, stride
            );
        if( pStreamInfo == NULL ) {
            return BAD_VALUE;
        }
        //
        mpHalImage_P1_Raw = pStreamInfo;
    }

    //if (
    //        mConfigProfile.mbConfigP1 &&
    //        !! mConfigProfile.mResizedSize
    if ( mParams.mbUseP1Node &&
            !! mParams.mResizedrawSize.size() )
    {
        // p1: resize
        MSize const& size = mParams.mResizedrawSize;
        MINT const format = mParams.mResizedrawFormat;
        size_t const stride = mParams.mResizedrawStride;
        MUINT const usage = 0;
        //
        sp<IImageStreamInfo>
            pStreamInfo = createRawImageStreamInfo(
                    "Hal:Image:P1:Resizeraw",
                    eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00,
                    eSTREAMTYPE_IMAGE_INOUT,
                    6, 4,
                    usage, format, size, stride
            );
        if( pStreamInfo == NULL ) {
            return BAD_VALUE;
        }
        //
        mpHalImage_P1_ResizerRaw = pStreamInfo;
    }

    //Hal:Image:FD
    if ( mParams.mbUseFDNode )
    {
        //MSize const size(640, 480); //FIXME: hard-code here?
        MSize size(640, 480);
        // evaluate preview size
        evaluatePreviewSize(rConfigParams, size);

        MY_LOGD("evaluate FD buffer size : %dx%d", size.w, size.h);

        MINT const format = eImgFmt_YUY2;//eImgFmt_YV12;
        MUINT const usage = 0;

        sp<ImageStreamInfo>
            pStreamInfo = createImageStreamInfo(
                "Hal:Image:FD",
                eSTREAMID_IMAGE_FD,
                eSTREAMTYPE_IMAGE_INOUT,
                5, 1,
                usage, format, size
            );
        if( pStreamInfo == NULL ) {
            return BAD_VALUE;
        }
        //
        mpHalImage_FD_YUV = pStreamInfo;
    }

    //Hal:Image:YUY2 for jpeg & thumbnail
    if ( mParams.mbUseJpegNode )
    {
        //Hal:Image:YUY2 for jpeg
        {
            MSize const& size = rConfigParams.pImage_Jpeg_Stall->getImgSize();
            MINT const format = eImgFmt_YUY2;
            MUINT const usage = 0;
            sp<ImageStreamInfo>
                pStreamInfo = createImageStreamInfo(
                    "Hal:Image:YuvJpeg",
                    eSTREAMID_IMAGE_PIPE_YUV_JPEG_00,
                    eSTREAMTYPE_IMAGE_INOUT,
                    1, 1,
                    usage, format, size
                );
            if( pStreamInfo == NULL ) {
                return BAD_VALUE;
            }
            //
            mpHalImage_Jpeg_YUV = pStreamInfo;
        }
        //
        //Hal:Image:YUY2 for thumbnail
        {
            MSize const size(-1L, -1L); //unknown now
            MINT const format = eImgFmt_YUY2;
            MUINT const usage = 0;
            sp<ImageStreamInfo>
                pStreamInfo = createImageStreamInfo(
                    "Hal:Image:YuvThumbnail",
                    eSTREAMID_IMAGE_PIPE_YUV_THUMBNAIL_00,
                    eSTREAMTYPE_IMAGE_INOUT,
                    1, 1,
                    usage, format, size
                );
            if( pStreamInfo == NULL ) {
                return BAD_VALUE;
            }
            //
            mpHalImage_Thumbnail_YUV = pStreamInfo;
            MY_LOGD("streamId:%#" PRIxPTR " %s %p", pStreamInfo->getStreamId(), pStreamInfo->getStreamName(), pStreamInfo.get());
        }
    }

    return OK;

}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_Streams(sp<PipelineContext> pContext)
{
#define BuildStream(_type_, _IStreamInfo_)                                     \
    do {                                                                       \
        if( _IStreamInfo_.get() ) {                                            \
            MERROR err;                                                        \
            if ( OK != (err = StreamBuilder(_type_, _IStreamInfo_)             \
                    .build(pContext)) )                                        \
            {                                                                  \
                MY_LOGE("StreamBuilder fail stream %# " PRIxPTR " of type %d", \
                    #_type_, _IStreamInfo_->getStreamId());                    \
                return err;                                                    \
            }                                                                  \
        }                                                                      \
    } while(0)
    BuildStream(eStreamType_META_HAL, mpHalMeta_Control);
    BuildStream(eStreamType_META_HAL, mpHalMeta_DynamicP1);
    BuildStream(eStreamType_META_HAL, mpHalMeta_DynamicP2);
    //
    BuildStream(eStreamType_META_APP, mpAppMeta_DynamicP1);
    BuildStream(eStreamType_META_APP, mpAppMeta_DynamicP2);
    BuildStream(eStreamType_META_APP, mpAppMeta_DynamicFD);
    BuildStream(eStreamType_META_APP, mpAppMeta_DynamicJpeg);
    BuildStream(eStreamType_META_APP, mpAppMeta_Control);
    //
    BuildStream(eStreamType_IMG_HAL_POOL   , mpHalImage_P1_Raw);
    BuildStream(eStreamType_IMG_HAL_POOL   , mpHalImage_P1_ResizerRaw);
    BuildStream(eStreamType_IMG_HAL_POOL   , mpHalImage_FD_YUV);
    BuildStream(eStreamType_IMG_HAL_POOL   , mpHalImage_Jpeg_YUV);
    BuildStream(eStreamType_IMG_HAL_RUNTIME, mpHalImage_Thumbnail_YUV);
    //
    for (size_t i = 0; i < mvAppYuvImage.size(); i++ )
    {
        BuildStream(eStreamType_IMG_APP, mvAppYuvImage[i]);
    }
    BuildStream(eStreamType_IMG_APP, mpAppImage_Jpeg);
    BuildStream(eStreamType_IMG_APP, mpAppImage_RAW16);
    //
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_Nodes(sp<PipelineContext> pContext)
{
    if( mParams.mbUseP1Node )
        CHECK_ERROR( configContextLocked_P1Node(pContext) );
    if( mParams.mbUseP2Node )
        CHECK_ERROR( configContextLocked_P2Node(pContext) );
    if( mParams.mbUseP2VSSNode )
        CHECK_ERROR( configContextLocked_P2VSSNode(pContext) );
    if( mParams.mbUseFDNode )
        CHECK_ERROR( configContextLocked_FdNode(pContext) );
    if( mParams.mbUseJpegNode )
        CHECK_ERROR( configContextLocked_JpegNode(pContext) );
    if( mParams.mbUseRaw16Node )
        CHECK_ERROR( configContextLocked_Raw16Node(pContext) );
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_Pipeline(sp<PipelineContext> pContext)
{
    NodeSet roots;
    {
        roots.add(eNODEID_P1Node);
    }
    NodeEdgeSet edges;
    {
        if( mParams.mbUseP1Node && mParams.mbUseP2Node )
            edges.addEdge(eNODEID_P1Node, eNODEID_P2Node);
        if( mParams.mbUseP1Node && mParams.mbUseP2VSSNode )
            edges.addEdge(eNODEID_P1Node, eNODEID_P2Node_VSS);
        if( mParams.mbUseP2Node && mParams.mbUseFDNode )
            edges.addEdge(eNODEID_P2Node, eNODEID_FDNode);
        if( ! mParams.mbUseP2VSSNode ) {
            if( mParams.mbUseP2Node && mParams.mbUseJpegNode )
                edges.addEdge(eNODEID_P2Node, eNODEID_JpegNode);
        }
        else {
            if( mParams.mbUseP2VSSNode && mParams.mbUseJpegNode )
                edges.addEdge(eNODEID_P2Node_VSS, eNODEID_JpegNode);
        }
        if( mParams.mbUseP1Node && mParams.mbUseRaw16Node )
            edges.addEdge(eNODEID_P1Node, eNODEID_RAW16Out);
    }
    //
    CHECK_ERROR(
            PipelineBuilder()
            .setRootNode(roots)
            .setNodeEdges(edges)
            .build(pContext)
            );
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
#define add_stream_to_set(_set_, _IStreamInfo_)                                \
    do {                                                                       \
        if( _IStreamInfo_.get() ) { _set_.add(_IStreamInfo_->getStreamId()); } \
    } while(0)
//
#define setImageUsage( _IStreamInfo_, _usg_ )                                   \
    do {                                                                        \
        if( _IStreamInfo_.get() ) {                                             \
            builder.setImageStreamUsage( _IStreamInfo_->getStreamId(), _usg_ ); \
        }                                                                       \
    } while(0)
/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_P1Node(sp<PipelineContext> pContext)
{
    typedef P1Node                  NodeT;
    typedef NodeActor< NodeT >      NodeActorT;
    //
    NodeId_T const nodeId = eNODEID_P1Node;
    //
    NodeT::InitParams initParam;
    {
        initParam.openId = getOpenId();
        initParam.nodeId = nodeId;
        initParam.nodeName = "P1Node";
    }
    NodeT::ConfigParams cfgParam;
    {
        NodeT::SensorParams sensorParam = {
        mode     : mParams.mSensorMode,
        size     : mParams.mSensorSize,
        fps      : mParams.mSensorFps,
        pixelMode: mParams.mPixelMode,
        };
        //
        cfgParam.pInAppMeta        = mpAppMeta_Control;
        cfgParam.pInHalMeta        = mpHalMeta_Control;
        cfgParam.pOutAppMeta       = mpAppMeta_DynamicP1;
        cfgParam.pOutHalMeta       = mpHalMeta_DynamicP1;
        cfgParam.pOutImage_resizer = mpHalImage_P1_ResizerRaw;
        if( mpHalImage_P1_Raw.get() )
            cfgParam.pvOutImage_full.push_back(mpHalImage_P1_Raw);
        cfgParam.sensorParams      = sensorParam;
        cfgParam.pStreamPool_resizer = mpHalImage_P1_ResizerRaw.get() ?
            pContext->queryImageStreamPool(eSTREAMID_IMAGE_PIPE_RAW_RESIZER_00) : NULL;
        cfgParam.pStreamPool_full = mpHalImage_P1_Raw.get() ?
            pContext->queryImageStreamPool(eSTREAMID_IMAGE_PIPE_RAW_OPAQUE_00) : NULL;
    }
    //
    sp<NodeActorT> pNode = new NodeActorT( NodeT::createInstance() );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(cfgParam);
    //
    StreamSet inStreamSet;
    StreamSet outStreamSet;
    //
    add_stream_to_set(inStreamSet, mpAppMeta_Control);
    add_stream_to_set(inStreamSet, mpHalMeta_Control);
    //
    add_stream_to_set(outStreamSet, mpHalImage_P1_Raw);
    add_stream_to_set(outStreamSet, mpHalImage_P1_ResizerRaw);
    add_stream_to_set(outStreamSet, mpAppMeta_DynamicP1);
    add_stream_to_set(outStreamSet, mpHalMeta_DynamicP1);
    //
    NodeBuilder builder(nodeId, pNode);
    builder
        .addStream(NodeBuilder::eDirection_IN, inStreamSet)
        .addStream(NodeBuilder::eDirection_OUT, outStreamSet);
    //
    setImageUsage(mpHalImage_P1_Raw        , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    setImageUsage(mpHalImage_P1_ResizerRaw , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    //
    MERROR err = builder.build(pContext);
    if( err != OK )
        MY_LOGE("build node %#" PRIxPTR " failed", nodeId);
    return err;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_P2Node(sp<PipelineContext> pContext)
{
    typedef P2Node                  NodeT;
    typedef NodeActor< NodeT >      NodeActorT;
    //
    NodeId_T const nodeId = eNODEID_P2Node;
    //
    NodeT::InitParams initParam;
    {
        initParam.openId = getOpenId();
        initParam.nodeId = nodeId;
        initParam.nodeName = "P2Node";
    }
    NodeT::ConfigParams cfgParam;
    {
        cfgParam.pInAppMeta    = mpAppMeta_Control;
        cfgParam.pInHalMeta    = mpHalMeta_DynamicP1;
        cfgParam.pOutAppMeta   = mpAppMeta_DynamicP2;
        cfgParam.pOutHalMeta   = (!mParams.mbUseP2VSSNode) ? mpHalMeta_DynamicP2 : NULL;
        //
        if( mpHalImage_P1_Raw.get() )
            cfgParam.pvInFullRaw.push_back(mpHalImage_P1_Raw);
        //
        cfgParam.pInResizedRaw = mpHalImage_P1_ResizerRaw;
        //
        for (size_t i = 0; i < mvAppYuvImage.size(); i++)
            cfgParam.vOutImage.push_back(mvAppYuvImage[i]);
        //
        if( !mParams.mbUseP2VSSNode ) {
            if( mpHalImage_Jpeg_YUV.get() )
                cfgParam.vOutImage.push_back(mpHalImage_Jpeg_YUV);
            if( mpHalImage_Jpeg_YUV.get() )
                cfgParam.vOutImage.push_back(mpHalImage_Thumbnail_YUV);
        }
        //
        cfgParam.pOutFDImage = mpHalImage_FD_YUV;
    }
    //
    sp<NodeActorT> pNode = new NodeActorT( NodeT::createInstance(P2Node::PASS2_STREAM) );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(cfgParam);
    //
    StreamSet inStreamSet;
    StreamSet outStreamSet;
    //
    add_stream_to_set(inStreamSet, mpAppMeta_Control);
    add_stream_to_set(inStreamSet, mpHalMeta_DynamicP1);
    add_stream_to_set(inStreamSet, mpHalImage_P1_Raw);
    add_stream_to_set(inStreamSet, mpHalImage_P1_ResizerRaw);
    //
    add_stream_to_set(outStreamSet, mpAppMeta_DynamicP2);
    if( !mParams.mbUseP2VSSNode ) {
        add_stream_to_set(outStreamSet, mpHalMeta_DynamicP2);
        add_stream_to_set(outStreamSet, mpHalImage_Jpeg_YUV);
        add_stream_to_set(outStreamSet, mpHalImage_Thumbnail_YUV);
    }
    //
    for (size_t i = 0; i < mvAppYuvImage.size(); i++)
        add_stream_to_set(outStreamSet, mvAppYuvImage[i]);
    //
    add_stream_to_set(outStreamSet, mpHalImage_FD_YUV);
    //
    NodeBuilder builder(nodeId, pNode);
    builder
        .addStream(NodeBuilder::eDirection_IN, inStreamSet)
        .addStream(NodeBuilder::eDirection_OUT, outStreamSet);
    //
    setImageUsage(mpHalImage_P1_Raw        , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    setImageUsage(mpHalImage_P1_ResizerRaw , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    //
    for (size_t i = 0; i < mvAppYuvImage.size(); i++)
        setImageUsage(mvAppYuvImage[i], eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    //
    setImageUsage(mpHalImage_Jpeg_YUV      , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    setImageUsage(mpHalImage_Thumbnail_YUV , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    setImageUsage(mpHalImage_FD_YUV        , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    //
    MERROR err = builder.build(pContext);
    if( err != OK )
        MY_LOGE("build node %#" PRIxPTR " failed", nodeId);
    return err;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_P2VSSNode(sp<PipelineContext> pContext)
{
    typedef P2Node                  NodeT;
    typedef NodeActor< NodeT >      NodeActorT;
    //
    NodeId_T const nodeId = eNODEID_P2Node_VSS;
    //
    NodeT::InitParams initParam;
    {
        initParam.openId = getOpenId();
        initParam.nodeId = nodeId;
        initParam.nodeName = "P2Node_VSS";
    }
    NodeT::ConfigParams cfgParam;
    {
        cfgParam.pInAppMeta    = mpAppMeta_Control;
        cfgParam.pInHalMeta    = mpHalMeta_DynamicP1;
        cfgParam.pOutAppMeta   = NULL;
        cfgParam.pOutHalMeta   = mpHalMeta_DynamicP2;
        //
        if( mpHalImage_P1_Raw.get() ) {
            cfgParam.pvInFullRaw.push_back(mpHalImage_P1_Raw);
            cfgParam.pInResizedRaw = NULL;
        }
        else
            cfgParam.pInResizedRaw = mpHalImage_P1_ResizerRaw;
        //
        for (size_t i = 0; i < mvAppYuvImage.size(); i++)
            cfgParam.vOutImage.push_back(mvAppYuvImage[i]);
        //
        if( !mParams.mbUseP2VSSNode ) {
            if( mpHalImage_Jpeg_YUV.get() )
                cfgParam.vOutImage.push_back(mpHalImage_Jpeg_YUV);
            if( mpHalImage_Jpeg_YUV.get() )
                cfgParam.vOutImage.push_back(mpHalImage_Thumbnail_YUV);
        }
    }
    //
    sp<NodeActorT> pNode = new NodeActorT( NodeT::createInstance(P2Node::PASS2_TIMESHARING) );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(cfgParam);
    //
    StreamSet inStreamSet;
    StreamSet outStreamSet;
    //
    add_stream_to_set(inStreamSet, mpAppMeta_Control);
    add_stream_to_set(inStreamSet, mpHalMeta_DynamicP1);
    add_stream_to_set(inStreamSet, mpHalImage_P1_Raw);
    add_stream_to_set(inStreamSet, mpHalImage_P1_ResizerRaw);
    //
    add_stream_to_set(outStreamSet, mpHalMeta_DynamicP2);
    add_stream_to_set(outStreamSet, mpHalImage_Jpeg_YUV);
    add_stream_to_set(outStreamSet, mpHalImage_Thumbnail_YUV);
    //
    //
    NodeBuilder builder(nodeId, pNode);
    builder
        .addStream(NodeBuilder::eDirection_IN, inStreamSet)
        .addStream(NodeBuilder::eDirection_OUT, outStreamSet);
    //
    setImageUsage(mpHalImage_P1_Raw        , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    setImageUsage(mpHalImage_P1_ResizerRaw , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    //
    setImageUsage(mpHalImage_Jpeg_YUV      , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    setImageUsage(mpHalImage_Thumbnail_YUV , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    //
    MERROR err = builder.build(pContext);
    if( err != OK )
        MY_LOGE("build node %#" PRIxPTR " failed", nodeId);
    return err;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_FdNode(sp<PipelineContext> pContext)
{
#warning "[FIXME] fd not ready"
#if 0
    typedef FdNode                  NodeT;
    typedef NodeActor< NodeT >      NodeActorT;
    //
    NodeId_T const nodeId = eNODEID_FDNode;
    //
    NodeT::InitParams initParam;
    {
        initParam.openId = getOpenId();
        initParam.nodeId = nodeId;
        initParam.nodeName = "FDNode";
    }
    NodeT::ConfigParams cfgParam;
    {
        cfgParam.pInAppMeta    = mpAppMeta_Control;
        cfgParam.pOutAppMeta   = mpAppMeta_DynamicFD;
        cfgParam.vInImage      = mpHalImage_FD_YUV;
    }
    //
    sp<NodeActorT> pNode = new NodeActorT( NodeT::createInstance() );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(cfgParam);
    //
    StreamSet inStreamSet;
    StreamSet outStreamSet;
    //
    add_stream_to_set(inStreamSet, mpAppMeta_Control);
    add_stream_to_set(inStreamSet, mpHalImage_FD_YUV);
    //
    add_stream_to_set(outStreamSet, mpAppMeta_DynamicFD);
    //
    NodeBuilder builder(nodeId, pNode);
    builder
        .addStream(NodeBuilder::eDirection_IN, inStreamSet)
        .addStream(NodeBuilder::eDirection_OUT, outStreamSet);
    //
    setImageUsage(mpHalImage_FD_YUV , eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    //
    MERROR err = builder.build(pContext);
    if( err != OK )
        MY_LOGE("build node %#" PRIxPTR " failed", nodeId);
    return err;
#endif
    MY_LOGE("fd not ready yet!");
    return UNKNOWN_ERROR;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_JpegNode(sp<PipelineContext> pContext)
{
    typedef JpegNode                NodeT;
    typedef NodeActor< NodeT >      NodeActorT;
    //
    NodeId_T const nodeId = eNODEID_JpegNode;
    //
    NodeT::InitParams initParam;
    {
        initParam.openId = getOpenId();
        initParam.nodeId = nodeId;
        initParam.nodeName = "JpegNode";
    }
    NodeT::ConfigParams cfgParam;
    {
        cfgParam.pInAppMeta        = mpAppMeta_Control;
        cfgParam.pInHalMeta        = mpHalMeta_DynamicP2;
        cfgParam.pOutAppMeta       = mpAppMeta_DynamicJpeg;
        cfgParam.pInYuv_Main       = mpHalImage_Jpeg_YUV;
        cfgParam.pInYuv_Thumbnail  = mpHalImage_Thumbnail_YUV;
        cfgParam.pOutJpeg          = mpAppImage_Jpeg;
    }
    //
    sp<NodeActorT> pNode = new NodeActorT( NodeT::createInstance() );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(cfgParam);
    //
    StreamSet inStreamSet;
    StreamSet outStreamSet;
    //
    add_stream_to_set(inStreamSet, mpAppMeta_Control);
    add_stream_to_set(inStreamSet, mpHalMeta_DynamicP2);
    add_stream_to_set(inStreamSet, mpHalImage_Jpeg_YUV);
    add_stream_to_set(inStreamSet, mpHalImage_Thumbnail_YUV);
    //
    add_stream_to_set(outStreamSet, mpAppMeta_DynamicJpeg);
    add_stream_to_set(outStreamSet, mpAppImage_Jpeg);
    //
    NodeBuilder builder(nodeId, pNode);
    builder
        .addStream(NodeBuilder::eDirection_IN, inStreamSet)
        .addStream(NodeBuilder::eDirection_OUT, outStreamSet);
    //
    setImageUsage(mpHalImage_Jpeg_YUV, eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    setImageUsage(mpHalImage_Thumbnail_YUV, eBUFFER_USAGE_SW_READ_OFTEN | eBUFFER_USAGE_HW_CAMERA_READ);
    setImageUsage(mpAppImage_Jpeg, eBUFFER_USAGE_SW_WRITE_OFTEN | eBUFFER_USAGE_HW_CAMERA_WRITE);
    //
    MERROR err = builder.build(pContext);
    if( err != OK )
        MY_LOGE("build node %#" PRIxPTR " failed", nodeId);
    return err;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configContextLocked_Raw16Node(sp<PipelineContext> pContext)
{
    typedef RAW16Node               NodeT;
    typedef NodeActor< NodeT >      NodeActorT;
    //
    NodeId_T const nodeId = eNODEID_RAW16Out;
    //
    NodeT::InitParams initParam;
    {
        initParam.openId = getOpenId();
        initParam.nodeId = nodeId;
        initParam.nodeName = "Raw16Node";
    }
    NodeT::ConfigParams cfgParam;
    {
    }
    //
    sp<NodeActorT> pNode = new NodeActorT( NodeT::createInstance() );
    pNode->setInitParam(initParam);
    pNode->setConfigParam(cfgParam);
    //
    StreamSet inStreamSet;
    StreamSet outStreamSet;
    //
    add_stream_to_set(inStreamSet, mpHalImage_P1_Raw);
    //
    add_stream_to_set(outStreamSet, mpAppImage_RAW16);
    //
    NodeBuilder builder(nodeId, pNode);
    builder
        .addStream(NodeBuilder::eDirection_IN, inStreamSet)
        .addStream(NodeBuilder::eDirection_OUT, outStreamSet);
    //
    setImageUsage(mpHalImage_P1_Raw, eBUFFER_USAGE_SW_READ_OFTEN);
    setImageUsage(mpAppImage_RAW16, eBUFFER_USAGE_SW_WRITE_OFTEN);
    //
    MERROR err = builder.build(pContext);
    if( err != OK )
        MY_LOGE("build node %#" PRIxPTR " failed", nodeId);
    return err;

}
/******************************************************************************
 *
 ******************************************************************************/
#undef add_stream_to_set
#undef setImageUsage


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
configRequestRulesLocked()
{
    struct categorize_yuv_stream
    {
        MVOID   operator() (
                    sp<const IImageStreamInfo> const pInfo,
                    MSize const& thres,
                    StreamSet& vLarge, StreamSet& vSmall
                )
                {
                    if( ! pInfo.get() ) return;
                    //
                    MSize const size = pInfo->getImgSize();
                    if ( size.w > thres.w || size.h > thres.h )
                        vLarge.add(pInfo->getStreamId());
                    else
                        vSmall.add(pInfo->getStreamId());
                }
    };
    //
    mvYuvStreams_Fullraw.clear();
    mvYuvStreams_Resizedraw.clear();
    //
    if( ! mpHalImage_P1_Raw.get() && ! mpHalImage_P1_ResizerRaw.get() ) {
        MY_LOGE("no available raw stream");
        return UNKNOWN_ERROR;
    }
    //
    MSize const threshold =
        mpHalImage_P1_ResizerRaw.get() ? mpHalImage_P1_ResizerRaw->getImgSize() : MSize(0,0);
    //
    StreamSet& vLarge =
        mpHalImage_P1_Raw.get() ? mvYuvStreams_Fullraw : mvYuvStreams_Resizedraw;
    StreamSet& vSmall =
        mpHalImage_P1_ResizerRaw.get() ? mvYuvStreams_Resizedraw : mvYuvStreams_Fullraw;
    //
    bool haveFullraw = mpHalImage_P1_Raw.get();
    //
    for( size_t i = 0; i < mvAppYuvImage.size(); i++ ) {
        sp<const IImageStreamInfo> pStreamInfo = mvAppYuvImage.valueAt(i);
        categorize_yuv_stream()(pStreamInfo, threshold, vLarge, vSmall);
    }
    categorize_yuv_stream()(mpHalImage_FD_YUV, threshold, vLarge, vSmall);
    //
    categorize_yuv_stream()(mpHalImage_Jpeg_YUV, MSize(0,0), vLarge, vSmall);
    categorize_yuv_stream()(mpHalImage_Thumbnail_YUV, MSize(0,0), vLarge, vSmall);
    //
#if 1
    // dump raw stream dispatch rule
    for( size_t i = 0; i < mvYuvStreams_Fullraw.size(); i++ ) {
        MY_LOGD("full raw streamId:%#" PRIxPTR " -> yuv streamId:%#" PRIxPTR,
                mpHalImage_P1_Raw->getStreamId(), mvYuvStreams_Fullraw[i]);
    }
    for( size_t i = 0; i < mvYuvStreams_Resizedraw.size(); i++ ) {
        MY_LOGD("resized raw streamId:%#" PRIxPTR " -> yuv streamId:%#" PRIxPTR,
                mpHalImage_P1_ResizerRaw->getStreamId(), mvYuvStreams_Resizedraw[i]);
    }
#endif
    //
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
evaluateRequestLocked(parsedAppRequest const& request, evaluateRequestResult& result)
{
    enum ePath
    {
        eImagePathP1,
        eImagePathP2Resized,
        eImagePathP2Full,
        eImagePathP2VSS,
        eImagePathRaw16,
        eImagePathJpeg,
        eImagePathFD,
        //
        eMetaPathP1,
        eMetaPathP2,
        eMetaPathP2VSS,
        eMetaPathRaw16,
        eMetaPathJpeg,
        eMetaPathFD,
        //
        ePathCount,
    };
    //
    struct
    {
        IOMap                       maps[ePathCount];
        DefaultKeyedVector<
            StreamId_T,
            sp<IImageStreamInfo>
                >                   vUpdatedImageInfos;
        //
        IOMap&                      editIOMap(ePath path) { return maps[path]; }
        MVOID                       addIn(ePath path, sp<IStreamInfo> pInfo) {
                                        editIOMap(path).addIn(pInfo->getStreamId());
                                    }
        MVOID                       addOut(ePath path, sp<IStreamInfo> pInfo) {
                                        editIOMap(path).addOut(pInfo->getStreamId());
                                    }
        MBOOL                       isConfigured(ePath path) {
                                        return editIOMap(path).vIn.size() ||
                                            editIOMap(path).vOut.size();
                                    }
        MVOID                       updateStreamInfo(sp<IImageStreamInfo> pInfo) {
                                        vUpdatedImageInfos.add(pInfo->getStreamId(), pInfo);
                                    }
    } aCollector;
    NodeEdgeSet& aEdges = result.edges;
    NodeSet& aRoot      = result.roots;

#define FUNC_ASSERT(exp, msg) \
    do{ if(!(exp)) { MY_LOGE("%s", msg); return INVALID_OPERATION; } } while(0)
    //
    FUNC_ASSERT( request.vIImageInfos_Raw.size() == 0, "[TODO] not supported yet!" );
    FUNC_ASSERT( request.vIImageInfos_Yuv.size() == 0, "[TODO] not supported yet!" );
    FUNC_ASSERT( request.vOImageInfos_Raw.size() <= 1, "[TODO] not supported yet!" );
    FUNC_ASSERT( request.vOImageInfos_Jpeg.size() <= 1, "[TODO] not supported yet!" );
    //
    // set root node
    aRoot.add(eNODEID_P1Node);
    //
    if ( request.vOImageInfos_Raw.size() )
    {
        IImageStreamInfo const* pStreamInfo = request.vOImageInfos_Raw[0].get();
        if ( isStream( mpAppImage_RAW16, pStreamInfo->getStreamId() ) )
        {
            // Raw16: full-size raw -> raw16
            //
            FUNC_ASSERT(
                    mParams.mbUseRaw16Node && mpHalImage_P1_Raw.get() && mpAppImage_RAW16.get(),
                    "not properly configured");
            //
            aCollector.addIn(eImagePathRaw16, mpHalImage_P1_Raw);
            aCollector.addOut(eImagePathRaw16, mpAppImage_RAW16);
            aEdges.addEdge(eNODEID_P1Node, eNODEID_RAW16Out);
        }
        else
        {
            MY_LOGE("not supported raw output stream %#" PRIxPTR ,
                    pStreamInfo->getStreamId());
            return INVALID_OPERATION;
        }
    }
    //
    if( request.vOImageInfos_Yuv.size() ) {
        //
        FUNC_ASSERT(
                mvYuvStreams_Fullraw.size() == 0 || mpHalImage_P1_Raw.get(),
                "wrong fullraw config");
        FUNC_ASSERT(
                mvYuvStreams_Resizedraw.size() == 0 || mpHalImage_P1_ResizerRaw.get(),
                "wrong resizedraw config");
        //
        bool useFull = false;
        bool useResized = false;
        for( size_t i = 0; i < request.vOImageInfos_Yuv.size(); i++ )
        {
            sp<IImageStreamInfo> pInfo = request.vOImageInfos_Yuv.valueAt(i);
            //
            StreamId_T const streamId = pInfo->getStreamId();
            if( 0 <= mvYuvStreams_Fullraw.indexOf(streamId) )
            {
                aCollector.addOut(eImagePathP2Full, pInfo);
                useFull = MTRUE;
            } else if( 0 <= mvYuvStreams_Resizedraw.indexOf(streamId) )
            {
                aCollector.addOut(eImagePathP2Resized, pInfo);
                useResized = MTRUE;
            }
            else
            {
                MY_LOGE("cannot find propery raw for stream %s(%#" PRIxPTR")",
                        pInfo->getStreamName(),streamId);
                return UNKNOWN_ERROR;
            }
        }
        //
        if ( useFull ) {
            aCollector.addOut(eImagePathP1, mpHalImage_P1_Raw);
            //
            aCollector.addIn(eImagePathP2Full, mpHalImage_P1_Raw);
        }
        if ( useResized ) {
            aCollector.addOut(eImagePathP1, mpHalImage_P1_ResizerRaw);
            //
            aCollector.addIn(eImagePathP2Resized, mpHalImage_P1_ResizerRaw);
        }
        //
        aEdges.addEdge(eNODEID_P1Node, eNODEID_P2Node);
    }
    //
    if( request.vOImageInfos_Jpeg.size() ) {
        //
        sp<IImageStreamInfo> pHalImage_Thumbnail_YUV;
        createStreamInfoLocked_Thumbnail_YUV(request.pAppMetaControl, pHalImage_Thumbnail_YUV);
        //
        if( pHalImage_Thumbnail_YUV.get() )
            aCollector.updateStreamInfo(pHalImage_Thumbnail_YUV);
        //
        sp<IImageStreamInfo> pSourceRaw =
            ( 0 <= mvYuvStreams_Fullraw.indexOf(mpHalImage_Jpeg_YUV->getStreamId())) ?
            mpHalImage_P1_Raw : mpHalImage_P1_ResizerRaw;
        bool const isVss = mParams.mbUseP2VSSNode;
        // p2
        if( ! isVss ) {
            aCollector.addOut(eImagePathP1, pSourceRaw);
            //
            aCollector.addIn(eImagePathP2Full, pSourceRaw);
            aCollector.addOut(eImagePathP2Full, mpHalImage_Jpeg_YUV);
            if( pHalImage_Thumbnail_YUV.get() )
                aCollector.addOut(eImagePathP2Full, pHalImage_Thumbnail_YUV);
            //
            aEdges.addEdge(eNODEID_P1Node, eNODEID_P2Node);
            aEdges.addEdge(eNODEID_P2Node, eNODEID_JpegNode);
        }
        else {
            aCollector.addOut(eImagePathP1, pSourceRaw);
            //
            aCollector.addIn(eImagePathP2VSS, pSourceRaw);
            aCollector.addOut(eImagePathP2VSS, mpHalImage_Jpeg_YUV);
            if( pHalImage_Thumbnail_YUV.get() )
                aCollector.addOut(eImagePathP2VSS, pHalImage_Thumbnail_YUV);
            //
            aEdges.addEdge(eNODEID_P1Node, eNODEID_P2Node_VSS);
            aEdges.addEdge(eNODEID_P2Node_VSS, eNODEID_JpegNode);
        }
        // jpeg
        aCollector.addIn(eImagePathJpeg, mpHalImage_Jpeg_YUV);
        if( pHalImage_Thumbnail_YUV.get() )
            aCollector.addIn(eImagePathJpeg, pHalImage_Thumbnail_YUV);
        aCollector.addOut(eImagePathJpeg, mpAppImage_Jpeg);
        //
    }
    //
    {
        // workaround: if p1node is used, config both raw if exists
        if( aCollector.isConfigured(eImagePathP1) ) {
            if( mpHalImage_P1_Raw.get() )
                aCollector.addOut(eImagePathP1, mpHalImage_P1_Raw);
            if( mpHalImage_P1_ResizerRaw.get() )
                aCollector.addOut(eImagePathP1, mpHalImage_P1_ResizerRaw);
        }
    }
    //
    if( isFdEnable(request.pAppMetaControl) )
    {
        FUNC_ASSERT(
                mpHalImage_FD_YUV.get(),
                "wrong fd yuv config");
        if( 0 <= mvYuvStreams_Resizedraw.indexOf(mpHalImage_FD_YUV->getStreamId()) )
        {
            aCollector.addOut(eImagePathP1, mpHalImage_P1_ResizerRaw);
            aCollector.addIn(eImagePathP2Resized, mpHalImage_P1_ResizerRaw);
            aCollector.addOut(eImagePathP2Resized, mpHalImage_FD_YUV);
        }
        else
        {
            aCollector.addOut(eImagePathP1, mpHalImage_P1_Raw);
            aCollector.addIn(eImagePathP2Full, mpHalImage_P1_Raw);
            aCollector.addOut(eImagePathP2Full, mpHalImage_FD_YUV);
        }
        //
        aCollector.addIn(eImagePathFD, mpHalImage_FD_YUV);
        //
        aEdges.addEdge(eNODEID_P1Node, eNODEID_P2Node);
        aEdges.addEdge(eNODEID_P2Node, eNODEID_FDNode);
    }
    //
    // update meta
    if( aCollector.isConfigured(eImagePathP1) )
    {
        aCollector.addIn(eMetaPathP1, mpAppMeta_Control);
        aCollector.addIn(eMetaPathP1, mpHalMeta_Control);
        aCollector.addOut(eMetaPathP1, mpAppMeta_DynamicP1);
        aCollector.addOut(eMetaPathP1, mpHalMeta_DynamicP1);
    }
    if( aCollector.isConfigured(eImagePathP2Full) ||
        aCollector.isConfigured(eImagePathP2Resized))
    {
        aCollector.addIn(eMetaPathP2, mpAppMeta_Control);
        aCollector.addIn(eMetaPathP2, mpHalMeta_DynamicP1);
        aCollector.addOut(eMetaPathP2, mpAppMeta_DynamicP2);
        if( ! mParams.mbUseP2VSSNode /*aCollector.isConfigured(eImagePathP2VSS)*/ )
            aCollector.addOut(eMetaPathP2, mpHalMeta_DynamicP2);
        //
    }
    //
    if( aCollector.isConfigured(eImagePathFD) )
    {
        aCollector.addIn(eMetaPathFD, mpAppMeta_Control);
        aCollector.addOut(eMetaPathFD, mpAppMeta_DynamicFD);
    }
    //
    if( aCollector.isConfigured(eImagePathP2VSS) )
    {
        aCollector.addIn(eMetaPathP2VSS, mpAppMeta_Control);
        aCollector.addIn(eMetaPathP2VSS, mpHalMeta_DynamicP1);
        aCollector.addOut(eMetaPathP2VSS, mpHalMeta_DynamicP2);
    }
    //
    if( aCollector.isConfigured(eImagePathJpeg) )
    {
        aCollector.addIn(eMetaPathJpeg, mpAppMeta_Control);
        aCollector.addIn(eMetaPathJpeg, mpHalMeta_DynamicP2);
        aCollector.addOut(eMetaPathJpeg, mpAppMeta_DynamicJpeg);
    }
    //
    // update to result
    for( size_t i = 0 ; i < aCollector.vUpdatedImageInfos.size(); i++ )
    {
        result.vUpdatedImageInfos.add(
                aCollector.vUpdatedImageInfos.keyAt(i),
                aCollector.vUpdatedImageInfos.valueAt(i)
                );
    }
    //
#define updateIOMap(_type_, _nodeId_, _path_ )                          \
    do{                                                                 \
        if( aCollector.isConfigured(_path_) ) {                    \
            result.nodeIOMap##_type_.add(                               \
                    _nodeId_,                                           \
                    IOMapSet().add(aCollector.editIOMap(_path_))); \
        }                                                               \
    } while(0)

    updateIOMap(Image, eNODEID_P1Node, eImagePathP1);
    {
        IOMapSet iomaps;
        if( aCollector.isConfigured(eImagePathP2Full) )
            iomaps.add(aCollector.editIOMap(eImagePathP2Full));
        if( aCollector.isConfigured(eImagePathP2Resized) )
            iomaps.add(aCollector.editIOMap(eImagePathP2Resized));
        result.nodeIOMapImage.add(eNODEID_P2Node, iomaps);
    }
    updateIOMap(Image, eNODEID_P2Node_VSS, eImagePathP2VSS);
    updateIOMap(Image, eNODEID_RAW16Out  , eImagePathRaw16);
    updateIOMap(Image, eNODEID_FDNode    , eImagePathFD);
    updateIOMap(Image, eNODEID_JpegNode  , eImagePathJpeg);
    //
    updateIOMap(Meta , eNODEID_P1Node    , eMetaPathP1);
    updateIOMap(Meta , eNODEID_P2Node    , eMetaPathP2);
    updateIOMap(Meta , eNODEID_P2Node_VSS, eMetaPathP2VSS);
    updateIOMap(Meta , eNODEID_RAW16Out  , eMetaPathRaw16);
    updateIOMap(Meta , eNODEID_FDNode    , eMetaPathFD);
    updateIOMap(Meta , eNODEID_JpegNode  , eMetaPathJpeg);
    //
#undef updateIOMap
    //
    { // app image
        result.vAppImageBuffers.setCapacity(
                request.pRequest->vIImageBuffers.size() +
                request.pRequest->vOImageBuffers.size()
                );
        for( size_t i = 0; i < request.pRequest->vIImageBuffers.size(); i++ )
            result.vAppImageBuffers.add(
                    request.pRequest->vIImageBuffers.keyAt(i),
                    request.pRequest->vIImageBuffers.valueAt(i)
                    );
        for( size_t i = 0; i < request.pRequest->vOImageBuffers.size(); i++ )
            result.vAppImageBuffers.add(
                    request.pRequest->vOImageBuffers.keyAt(i),
                    request.pRequest->vOImageBuffers.valueAt(i)
                    );
    }
    //
    { // hal image
        result.vHalImageBuffers.clear();
    }
    //
    { // app meta
        result.vAppMetaBuffers.setCapacity(request.pRequest->vIMetaBuffers.size());
        for( size_t i = 0; i < request.pRequest->vIMetaBuffers.size(); i++ )
            result.vAppMetaBuffers.add(
                    request.pRequest->vIMetaBuffers.keyAt(i),
                    request.pRequest->vIMetaBuffers.valueAt(i)
                    );
    }
    //
    { // hal meta
        result.vHalMetaBuffers.setCapacity(1);
        sp<HalMetaStreamBuffer> pBuffer =
            HalMetaStreamBufferAllocatorT(mpHalMeta_Control.get())();
        result.vHalMetaBuffers.add(mpHalMeta_Control->getStreamId(), pBuffer);
    }
    //
    return OK;
#undef FUNC_ASSERT
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
refineRequestMetaStreamBuffersLocked(evaluateRequestResult& result)
{
    if( ! mpHalMeta_Control.get() ) {
        MY_LOGE("should config hal control meta");
        return UNKNOWN_ERROR;
    }
    //
    {
        sp<IMetaStreamBuffer> pBuf = result.vHalMetaBuffers.valueFor(mpHalMeta_Control->getStreamId());
        if( pBuf.get() )
        {
            IMetadata* pMetadata = pBuf->tryWriteLock(LOG_TAG);

            // update sensor size
            {
                IMetadata::IEntry entry(MTK_HAL_REQUEST_SENSOR_SIZE);
                entry.push_back(mParams.mSensorSize, Type2Type< MSize >());
                pMetadata->update(entry.tag(), entry);
            }

            if ( mpAppImage_Jpeg.get() &&
                    0 <= result.vAppImageBuffers.indexOfKey(mpAppImage_Jpeg->getStreamId()) )
            {
                MY_LOGD_IF(1, "set MTK_HAL_REQUEST_REQUIRE_EXIF = 1");
                IMetadata::IEntry entry(MTK_HAL_REQUEST_REQUIRE_EXIF);
                entry.push_back(1, Type2Type<MUINT8>());
                pMetadata->update(entry.tag(), entry);
            }

            // set "the largest frame duration of streams" as "minimum frame duration"
            {
                MINT64 iMinFrmDuration = 0;
                for ( size_t i=0; i<result.vAppImageBuffers.size(); i++ ) {
                    StreamId_T const streamId = result.vAppImageBuffers.keyAt(i);
                    if( mvStreamDurations.indexOfKey(streamId) < 0 ) {
                        MY_LOGE("Request App stream %#" PRIxPTR "have not configured yet", streamId);
                        continue;
                    }
                    iMinFrmDuration = ( mvStreamDurations.valueFor(streamId) > iMinFrmDuration)?
                        mvStreamDurations.valueFor(streamId) : iMinFrmDuration;
                }
                MY_LOGD_IF(0, "The min frame duration is %lld", iMinFrmDuration);
                IMetadata::IEntry entry(MTK_P1NODE_MIN_FRM_DURATION);
                entry.push_back(iMinFrmDuration, Type2Type<MINT64>());
                pMetadata->update(entry.tag(), entry);
            }

            //
            pBuf->unlock(LOG_TAG, pMetadata);
        }
        else
        {
            MY_LOGE("cannot get hal control meta sb.");
            return UNKNOWN_ERROR;
        }
    }
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineDefaultImp::
createStreamInfoLocked_Thumbnail_YUV(
    IMetadata const* pMetadata,
    android::sp<IImageStreamInfo>& rpStreamInfo
) const
{
    if  ( mpHalImage_Thumbnail_YUV == 0 ) {
        MY_LOGW("No config stream: Thumbnail_YUV");
        return NO_INIT;
    }
    //
    IMetadata::IEntry const& entryThumbnailSize = pMetadata->entryFor(MTK_JPEG_THUMBNAIL_SIZE);
    if  ( entryThumbnailSize.isEmpty() ) {
        MY_LOGW("No tag: MTK_JPEG_THUMBNAIL_SIZE");
        return NAME_NOT_FOUND;
    }
    MSize const& thumbnailSize = entryThumbnailSize.itemAt(0, Type2Type<MSize>());
    if  ( ! thumbnailSize ) {
        MY_LOGW("Bad thumbnail size: %dx%d", thumbnailSize.w, thumbnailSize.h);
        return NOT_ENOUGH_DATA;
    }
    MY_LOGD_IF( 0, "thumbnail size from metadata: %dx%d", thumbnailSize.w, thumbnailSize.h);
    //
    //
    IMetadata::IEntry const& entryJpegOrientation = pMetadata->entryFor(MTK_JPEG_ORIENTATION);
    if  ( entryJpegOrientation.isEmpty() ) {
        MY_LOGW("No tag: MTK_JPEG_ORIENTATION");
        return NAME_NOT_FOUND;
    }
    //
    MSize const yuvthumbnailsize = calcThumbnailYuvSize(
                                        mpHalImage_Jpeg_YUV->getImgSize(),
                                        thumbnailSize
                                        );
    //
    MINT32  jpegOrientation = 0;
    MUINT32 jpegTransform   = 0;
    MSize   thunmbSize      = yuvthumbnailsize; // default thumbnail size
    //
#if 0
    if ( mJpegRotationEnable ) {
        jpegOrientation = entryJpegOrientation.itemAt(0, Type2Type<MINT32>());
        if ( 0==jpegOrientation )
            jpegTransform = 0;
        else if ( 90==jpegOrientation )
            jpegTransform = eTransform_ROT_90;
        else if ( 180==jpegOrientation )
            jpegTransform = eTransform_ROT_180;
        else if ( 270==jpegOrientation )
            jpegTransform = eTransform_ROT_270;
        else
             MY_LOGW("Invalid Jpeg Orientation value: %d", jpegOrientation);
        //
        thunmbSize = yuvthumbnailsize;
        if ( jpegTransform & eTransform_ROT_90 )
            thunmbSize = MSize(yuvthumbnailsize.h, yuvthumbnailsize.w);
    }
#endif
    //
    MINT const format = mpHalImage_Thumbnail_YUV->getImgFormat();
    IImageStreamInfo::BufPlanes_t bufPlanes;
    switch (format)
    {
    case eImgFmt_YUY2:{
        IImageStreamInfo::BufPlane bufPlane;
        bufPlane.rowStrideInBytes = (yuvthumbnailsize.w << 1);
        bufPlane.sizeInBytes = bufPlane.rowStrideInBytes * yuvthumbnailsize.h;
        bufPlanes.push_back(bufPlane);
        }break;
    default:
        MY_LOGE("not supported format: %#x", format);
        break;
    }
    //
    rpStreamInfo = new ImageStreamInfo(
        mpHalImage_Thumbnail_YUV->getStreamName(),
        mpHalImage_Thumbnail_YUV->getStreamId(),
        mpHalImage_Thumbnail_YUV->getStreamType(),
        mpHalImage_Thumbnail_YUV->getMaxBufNum(),
        mpHalImage_Thumbnail_YUV->getMinInitBufNum(),
        mpHalImage_Thumbnail_YUV->getUsageForAllocator(),
        format,
        thunmbSize,
        bufPlanes,
        jpegTransform
    );
    if  ( rpStreamInfo == 0 ) {
        MY_LOGE(
            "fail to new ImageStreamInfo: %s %#" PRIxPTR,
            mpHalImage_Thumbnail_YUV->getStreamName(),
            mpHalImage_Thumbnail_YUV->getStreamId()
        );
        return NO_MEMORY;
    }
    //
    MY_LOGD_IF(
        1,
        "streamId:%#" PRIxPTR " %s %p %p yuvthumbnailsize:%dx%d jpegOrientation:%d",
        rpStreamInfo->getStreamId(),
        rpStreamInfo->getStreamName(),
        rpStreamInfo.get(),
        mpHalImage_Thumbnail_YUV.get(),
        thunmbSize.w, thunmbSize.h, jpegOrientation
    );
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MSize
PipelineDefaultImp::
calcThumbnailYuvSize(
    MSize const rPicSize,
    MSize const rThumbnailsize
) const
{
#define align2(x) (((x) + 1) & (~0x1))
    MSize size;
    MUINT32 const val0 = rPicSize.w * rThumbnailsize.h;
    MUINT32 const val1 = rPicSize.h * rThumbnailsize.w;
    if( val0 > val1 ) {
        size.w = align2(val0/rPicSize.h);
        size.h = rThumbnailsize.h;
    }
    else if( val0 < val1 ) {
        size.w = rThumbnailsize.w;
        size.h = align2(val1/rPicSize.w);
    }
    else {
        size = rThumbnailsize;
    }
#undef align2
    MY_LOGD_IF(1, "thumb %dx%d, pic %dx%d -> yuv for thumb %dx%d",
            rThumbnailsize.w, rThumbnailsize.h,
            rPicSize.w, rPicSize.h,
            size.w, size.h
            );
    return size;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
PipelineDefaultImp::
isFdEnable(
    IMetadata const* pMetadata
)
{
    #if 1 //[FIXME] always return false for temporary
    #warning "[FIXME] isFdEnable() always return false for temporary"
    return false;
    #endif

    //  If Face detection is not OFF or scene mode is face priority,
    //  add App:Meta:FD_result stream to Output App Meta Streams.
    IMetadata::IEntry const& entryFdMode = pMetadata->entryFor(MTK_STATISTICS_FACE_DETECT_MODE);
    IMetadata::IEntry const& entryfaceScene = pMetadata->entryFor(MTK_CONTROL_SCENE_MODE);
    IMetadata::IEntry const& entryGdMode = pMetadata->entryFor(MTK_FACE_FEATURE_GESTURE_MODE);
    IMetadata::IEntry const& entrySdMode = pMetadata->entryFor(MTK_FACE_FEATURE_SMILE_DETECT_MODE);
    IMetadata::IEntry const& entryAsdMode = pMetadata->entryFor(MTK_FACE_FEATURE_ASD_MODE);
    //
    return  //(0 != mDebugFdMode) ||
             ( !entryFdMode.isEmpty() && MTK_STATISTICS_FACE_DETECT_MODE_OFF != entryFdMode.itemAt(0, Type2Type<MUINT8>())) ||
             ( !entryfaceScene.isEmpty() && MTK_CONTROL_SCENE_MODE_FACE_PRIORITY == entryfaceScene.itemAt(0, Type2Type<MUINT8>())) ||
             ( !entryGdMode.isEmpty() && MTK_FACE_FEATURE_GESTURE_MODE_OFF != entryGdMode.itemAt(0, Type2Type<MINT32>())) ||
             ( !entrySdMode.isEmpty() && MTK_FACE_FEATURE_SMILE_DETECT_MODE_OFF != entrySdMode.itemAt(0, Type2Type<MINT32>())) ||
             ( !entryAsdMode.isEmpty() && MTK_FACE_FEATURE_ASD_MODE_OFF != entryAsdMode.itemAt(0, Type2Type<MINT32>()));

}


/******************************************************************************
 *  IPipelineBufferSetFrameControl::IAppCallback Interfaces.
 ******************************************************************************/
MVOID
PipelineDefaultImp::
updateFrame(
    MUINT32 const frameNo,
    MINTPTR const userId,
    Result const& result
)
{
    MY_LOGD("frameNo %d, user %#" PRIxPTR ", AppLeft %d, appMeta %d, HalLeft %d, halMeta %d",
            frameNo, userId,
            result.nAppOutMetaLeft, result.vAppOutMeta.size(),
            result.nHalOutMetaLeft, result.vHalOutMeta.size()
           );
    sp<IPipelineModelMgr::IAppCallback> pAppCallback;
    pAppCallback = mpAppCallback.promote();
    if ( ! pAppCallback.get() ) {
        MY_LOGE("Have not set callback to device");
        FUNC_END;
        return;
    }
    pAppCallback->updateFrame(frameNo, userId, result.nAppOutMetaLeft, result.vAppOutMeta);
}


/******************************************************************************
 *
 ******************************************************************************/
sp<ImageStreamInfo>
PipelineDefaultImp::
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
        MY_LOGE("create ImageStream failed, %s, %#" PRIxPTR,
                streamName, streamId);
    }

    return pStreamInfo;
}


/*******************************************************************************
 *
 ********************************************************************************/
MBOOL
getSensorOutputFmt(
    SensorStaticInfo const& sensorInfo,
    MUINT32 bitDepth,
    MBOOL isFull,
    MINT* pFmt
)
{
    MBOOL ret = MFALSE;
    // sensor fmt
#define case_Format( order_bit, mappedfmt, pFmt) \
        case order_bit:                          \
            (*(pFmt)) = mappedfmt;               \
        break;

    if( sensorInfo.sensorType == SENSOR_TYPE_YUV )
    {
        switch( sensorInfo.sensorFormatOrder )
        {
            case_Format( SENSOR_FORMAT_ORDER_UYVY, eImgFmt_UYVY, pFmt);
            case_Format( SENSOR_FORMAT_ORDER_VYUY, eImgFmt_VYUY, pFmt);
            case_Format( SENSOR_FORMAT_ORDER_YUYV, eImgFmt_YUY2, pFmt);
            case_Format( SENSOR_FORMAT_ORDER_YVYU, eImgFmt_YVYU, pFmt);
            default:
            MY_LOGE("formatOrder not supported, 0x%x", sensorInfo.sensorFormatOrder);
            goto lbExit;
            break;
        }
        MY_LOGD("sensortype:(0x%x), fmt(0x%x)", sensorInfo.sensorType, *pFmt);
    }
    else if( sensorInfo.sensorType == SENSOR_TYPE_RAW )
    {
        if( isFull ) //imgo
        {
            switch( bitDepth )
            {
                case_Format(  8, eImgFmt_BAYER8 , pFmt);
                case_Format( 10, eImgFmt_BAYER10, pFmt);
                case_Format( 12, eImgFmt_BAYER12, pFmt);
                case_Format( 14, eImgFmt_BAYER14, pFmt);
                default:
                MY_LOGE("bitdepth not supported, 0x%x", bitDepth);
                goto lbExit;
                break;
            }
        }
        else // rrzo
        {
            switch( bitDepth )
            {
                case_Format(  8, eImgFmt_FG_BAYER8 , pFmt);
                case_Format( 10, eImgFmt_FG_BAYER10, pFmt);
                case_Format( 12, eImgFmt_FG_BAYER12, pFmt);
                case_Format( 14, eImgFmt_FG_BAYER14, pFmt);
                default:
                MY_LOGE("bitdepth not supported, 0x%x", bitDepth);
                goto lbExit;
                break;
            }
        }
        MY_LOGD("sensortype: 0x%x, full(%d), fmt(0x%x), order(%d)",
                sensorInfo.sensorType, isFull, *pFmt, sensorInfo.sensorFormatOrder);
    }
    else
    {
        MY_LOGE("sensorType not supported yet(0x%x)", sensorInfo.sensorType);
        goto lbExit;
    }
    ret = MTRUE;
#undef case_Format

lbExit:
    return ret;
}


/*******************************************************************************
 *
 ********************************************************************************/
MERROR
alignPass1HwLimitation(
    MUINT32 const pixelMode,
    MINT const imgFormat,
    MSize& size,
    MBOOL isFull,
    size_t& stride
)
{
    NSImageio::NSIspio::ISP_QUERY_RST queryRst;
    NSImageio::NSIspio::ISP_QuerySize(
            isFull ?
            NSImageio::NSIspio::EPortIndex_IMGO:
            NSImageio::NSIspio::EPortIndex_RRZO,
            NSImageio::NSIspio::ISP_QUERY_X_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_PIX|
            NSImageio::NSIspio::ISP_QUERY_STRIDE_BYTE,
            (EImageFormat)imgFormat,
            size.w,
            queryRst,
            pixelMode == 0 ?
            NSImageio::NSIspio::ISP_QUERY_1_PIX_MODE :
            NSImageio::NSIspio::ISP_QUERY_2_PIX_MODE
            );
    size.w = queryRst.x_pix;
    stride = queryRst.stride_byte;
    return OK;
}
