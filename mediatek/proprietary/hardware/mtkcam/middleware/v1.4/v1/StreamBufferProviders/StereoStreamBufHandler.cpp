/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#define LOG_TAG "MtkCam/StereoStreamHdl"

#include <Log.h>
#include <common.h>
using namespace NSCam;
//
#include <utils/Mutex.h>
#include <camera/MtkCamera.h>
#include <CamUtils.h>

using namespace android;
using namespace MtkCamUtils;
using namespace NSCam::Utils;
//
#include <cutils/properties.h>
//
#include <list>
#include <map>
#include <set>
using namespace std;
//
#include <iopipe/Port.h>
//
#include <utils/include/ImageBufferHeap.h>
//
#include <v3/utils/streambuf/IStreamBufferProvider.h>
#include <metadata/IMetadata.h>
#include <v1/StreamBufferProviders/StereoBufMgr.h>
#include <ImgBufProvidersManager.h>
using namespace NSMtkBufMgr;
using namespace NSCam::v3;
using namespace NSCam::v3::Utils;
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>
#include <v1/StreamBufferProviders/StereoPrvStreamBufHandler.h>
#include <v1/StreamBufferProviders/StereoStreamBufHandler.h>
#include <v1/StreamIDs.h>
using namespace NSMtkStreamId;
#include <metadata/client/mtk_metadata_tag.h>

#if defined(__func__)
#undef __func__
#endif
#define __func__ __FUNCTION__
//
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%d)[%s] " fmt, getOpenId(), __func__, ##arg)
//
#define MY_LOGD_NO_OPENID(fmt, arg...)  CAM_LOGD("(%d)[%s] " fmt, 3, __func__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

// Temp set LOGD1 to be enabled when mLogLevel == 0
// Will be set to 1 when development is done
#define MY_LOGD1(...)               MY_LOGD_IF(0<=mLogLevel, __VA_ARGS__)
//
#define FUNC_START  MY_LOGD("+")
#define FUNC_END    MY_LOGD("-")
#define FUNC_NAME   MY_LOGD("")
//
/******************************************************************************
*
*******************************************************************************/
#define GET_IIMGBUF_IMG_STRIDE_IN_BYTE(pBuf, plane)         (Format::queryPlaneCount(Format::queryImageFormat(pBuf->getImgFormat().string())) >= (plane+1)) ? \
                                                            ((pBuf->getImgWidthStride(plane)*Format::queryPlaneBitsPerPixel(Format::queryImageFormat(pBuf->getImgFormat().string()),plane)))>>3 : 0
#define GET_IIMAGEBUFFER_BUF_STRIDE_IN_BYTE(pBuf, plane)    (pBuf->getPlaneCount() >= (plane+1)) ? (pBuf->getBufStridesInBytes(plane)) : 0
#define GET_IIMAGEBUFFER_BUF_SIZE(pBuf, plane)              (pBuf->getPlaneCount() >= (plane+1)) ? pBuf->getBufSizeInBytes(plane) : 0
#define GET_IIMAGEBUFFER_BUF_VA(pBuf, plane)                (pBuf->getPlaneCount() >= (plane+1)) ? pBuf->getBufVA(plane) : 0
#define GET_IIMAGEBUFFER_BUF_PA(pBuf, plane)                (pBuf->getPlaneCount() >= (plane+1)) ? pBuf->getBufPA(plane) : 0


/*******************************************************************************
*
********************************************************************************/
namespace NSCamStreamBufProvider {

typedef enum _SYNC_RESULT_
{
    SYNC_RESULT_PAIR_OK       = 0,
    SYNC_RESULT_MAIN1_TOO_OLD = 1,
    SYNC_RESULT_MAIN2_TOO_OLD = 2,
    SYNC_RESULT_PAIR_NOT_SYNC = 3
} SYNC_RESULT;

typedef enum _READY_RESULT_
{
    READY_RESULT_OK           = 0,
    READY_RESULT_EMPTY        = 1,
    READY_RESULT_NO_META      = 2,
    READY_RESULT_NO_BUFFER    = 3
} READY_RESULT;

typedef enum _DEBUG_MODE_
{
    eNONE                      = 0,
    eWORK_AS_SIMPLE_WARPPER    = 1,
    eSKIP_META_MECHANISM       = 2
} DEBUG_MODE;

class StereoStreamBufHandlerImpl : public StereoStreamBufHandler
{
    public:
        StereoStreamBufHandlerImpl(MUINT32 openId, const char* userName);
        ~StereoStreamBufHandlerImpl();

        //DECLARE_ICAMBUFHANDLER_INTERFACE();
        MERROR                              dequeStreamBuffer(
                                                MUINT32 const iRequestNo,
                                                sp<IImageStreamInfo> const pStreamInfo,
                                                sp<HalImageStreamBuffer> &rpStreamBuffer);

        MERROR                              enqueStreamBuffer(
                                                sp<IImageStreamInfo> const pStreamInfo,
                                                sp<HalImageStreamBuffer> rpStreamBuffer,
                                                MUINT32  bBufStatus);

        virtual MBOOL                       setDebugMode(MUINT32 debugMode){mDebugMode = debugMode; return MTRUE;};

        virtual MBOOL   setMetadata(
                                MUINT32 iReqNum,
                                MUINT32 streamId,
                                IMetadata* pMetadata,
                                MBOOL isMain2);
        virtual MBOOL   setConfig(StereoPrvStreamBufHandler::CONFIG_INFO configInfo);

        virtual MBOOL   stopDequeBufMgrs();
        virtual MBOOL   init();
        virtual MBOOL   uninit();

        typedef struct
        {
            MBOOL                   isMetadataSet;
            IMetadata               metadata;
        }META_INFO;

        typedef struct
        {
            MUINT32                             iReqNum;
            sp<IImageStreamInfo>                pStreamInfo;
            sp<HalImageStreamBuffer>            rpStreamBuffer;
            MUINT32                             bBufStatus;
            META_INFO                           pMeta_hal;
            META_INFO                           pMeta_app;
            MBOOL                               isBufferReturned;
        }STREAM_BUF_INFO;

    //
    protected:
        //
        MUINT32         getOpenId() {return mOpenId;}
        const char*     getName() const {return msName;}
        MBOOL           syncRoutine();
        READY_RESULT    isReadyToPushToZsdStreamMgr();
        SYNC_RESULT     isTimeSync();
        MBOOL           enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(StreamId_T streamId, MUINT32 bBufStatus);

    private:
        MUINT32                     mOpenId;
        mutable Mutex               mLock;
        const char* const           msName;
        MINT32                      mLogLevel;
        MINT32                      mNoMetaCount;

        map<StreamId_T, sp<StereoPrvStreamBufHandler> > streamToBufHandler;
        typedef pair<StreamId_T, sp<StereoPrvStreamBufHandler> > PairStreamToBufHandler;

        map<StreamId_T, list<STREAM_BUF_INFO>*> streamToPendingQue;
        typedef pair<StreamId_T, list<STREAM_BUF_INFO>*> PairStreamToPendingQue;

        map<StreamId_T, StereoBufMgr*> streamToBufMgr;
        typedef pair<StreamId_T, StereoBufMgr*> PairStreamToBufMgr;

        sp<StereoPrvStreamBufHandler>  Zsd_ResizedRaw;
        sp<StereoPrvStreamBufHandler>  Zsd_ResizedRaw_main2;
        sp<StereoPrvStreamBufHandler>  Zsd_FullRaw;
        sp<StereoPrvStreamBufHandler>  Zsd_FullRaw_main2;

        StereoBufMgr*               mpBufMgr_Zsd_ResizedRaw;
        StereoBufMgr*               mpBufMgr_Zsd_ResizedRaw_main2;
        StereoBufMgr*               mpBufMgr_Zsd_FullRaw;
        StereoBufMgr*               mpBufMgr_Zsd_FullRaw_main2;

        list<STREAM_BUF_INFO>       pendingQueueResizeRaw;
        list<STREAM_BUF_INFO>       pendingQueueFullRaw;
        list<STREAM_BUF_INFO>       pendingQueueResizeRaw_main2;
        list<STREAM_BUF_INFO>       pendingQueueFullRaw_main2;

        int                         testCounter;
        MUINT32                     mDebugMode;
};


/*******************************************************************************
*
********************************************************************************/
StereoStreamBufHandler*
StereoStreamBufHandler::
createInstance(
    MUINT32     openId,
    const char* userName)
{
    return new StereoStreamBufHandlerImpl(openId, userName);
}


/*******************************************************************************
*
********************************************************************************/
MVOID
StereoStreamBufHandler::
destroyInstance()
{
    MY_LOGD_NO_OPENID("+");

    // delete this;

    MY_LOGD_NO_OPENID("-");
}


/*******************************************************************************
*
********************************************************************************/
StereoStreamBufHandler::
StereoStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
StereoStreamBufHandler::
~StereoStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
StereoStreamBufHandlerImpl::
StereoStreamBufHandlerImpl(
    MUINT32     openId,
    const char* userName)
    : StereoStreamBufHandler()
    , mOpenId(openId)
    , msName(userName)
    , Zsd_ResizedRaw(NULL)
    , Zsd_ResizedRaw_main2(NULL)
    , Zsd_FullRaw(NULL)
    , Zsd_FullRaw_main2(NULL)
    , mpBufMgr_Zsd_ResizedRaw(NULL)
    , mpBufMgr_Zsd_ResizedRaw_main2(NULL)
    , mpBufMgr_Zsd_FullRaw(NULL)
    , mpBufMgr_Zsd_FullRaw_main2(NULL)
    , testCounter(0)
    , mNoMetaCount(0)
    , mDebugMode(0)
{
    FUNC_NAME;
    // maps
    streamToBufHandler.clear();
    streamToPendingQue.clear();
    streamToBufMgr.clear();

    // pending queues
    pendingQueueResizeRaw.clear();
    pendingQueueFullRaw.clear();
    pendingQueueResizeRaw_main2.clear();
    pendingQueueFullRaw_main2.clear();

    char cLogLevel[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "0");
    mLogLevel = ::atoi(cLogLevel);
}


/*******************************************************************************
*
********************************************************************************/
StereoStreamBufHandlerImpl::
~StereoStreamBufHandlerImpl()
{
    FUNC_NAME;

    Zsd_ResizedRaw = NULL;
    Zsd_ResizedRaw_main2 = NULL;
    Zsd_FullRaw = NULL;
    Zsd_FullRaw_main2 = NULL;

    mpBufMgr_Zsd_ResizedRaw = NULL;
    mpBufMgr_Zsd_ResizedRaw_main2 = NULL;
    mpBufMgr_Zsd_FullRaw = NULL;
    mpBufMgr_Zsd_FullRaw_main2 = NULL;

    // todo
    // release or return everything
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoStreamBufHandlerImpl::
init()
{
    FUNC_START;

    // TODO:
    // Check whether necessary streams are set

    for(map<StreamId_T, sp<StereoPrvStreamBufHandler> >::iterator it=streamToBufHandler.begin(); it!=streamToBufHandler.end(); ++it){
        MY_LOGD("StereoPrvStreamBufHandler stream %#" PRIxPTR " init",it->first);
        if(!it->second->init()){
            MY_LOGE("StereoPrvStreamBufHandler stream %#" PRIxPTR " init failed",it->first);
        }
        it->second->setProviderWrapper(this);
    }

    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoStreamBufHandlerImpl::
uninit()
{
    FUNC_START;

    for(map<StreamId_T, sp<StereoPrvStreamBufHandler> >::iterator it=streamToBufHandler.begin(); it!=streamToBufHandler.end(); ++it){
        MY_LOGD("StereoPrvStreamBufHandler stream %#" PRIxPTR " uninit",it->first);
        if(!it->second->uninit()){
            MY_LOGE("StereoPrvStreamBufHandler stream %#" PRIxPTR " uninit failed",it->first);
        }
        MY_LOGD("StereoPrvStreamBufHandler stream %#" PRIxPTR " destroyInstance +",it->first);
        it->second->destroyInstance();
        MY_LOGD("StereoPrvStreamBufHandler stream %#" PRIxPTR " destroyInstance -",it->first);
    }

    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoStreamBufHandlerImpl::
setConfig(StereoPrvStreamBufHandler::CONFIG_INFO configInfo)
{
    FUNC_START;
    MBOOL ret = MTRUE;

    if(configInfo.pStreamInfo == NULL) {
        MY_LOGE("config StreamInfo is NULL");
        return MFALSE;
    }

    switch(configInfo.pStreamInfo->getStreamId()){
        case STREAM_ID_PASS1_RESIZE:
            if(Zsd_ResizedRaw == NULL){
                Zsd_ResizedRaw = StereoPrvStreamBufHandler::createInstance(getOpenId(), "Zsd_ResizedRaw");
                mpBufMgr_Zsd_ResizedRaw = configInfo.pBufMgr;
                streamToBufHandler.insert(PairStreamToBufHandler(STREAM_ID_PASS1_RESIZE, Zsd_ResizedRaw));
                streamToPendingQue.insert(PairStreamToPendingQue(STREAM_ID_PASS1_RESIZE, &pendingQueueResizeRaw));
                streamToBufMgr.insert(PairStreamToBufMgr(STREAM_ID_PASS1_RESIZE, mpBufMgr_Zsd_ResizedRaw));
            }
            if(!Zsd_ResizedRaw->setConfig(configInfo)){
                MY_LOGE("Zsd_ResizedRaw->setConfig failed");
            };
            break;
        case STREAM_ID_PASS1_RESIZE_MAIN2:
            if(Zsd_ResizedRaw_main2 == NULL){
                Zsd_ResizedRaw_main2 = StereoPrvStreamBufHandler::createInstance(getOpenId(), "Zsd_ResizedRaw_main2");
                mpBufMgr_Zsd_ResizedRaw_main2 = configInfo.pBufMgr;
                streamToBufHandler.insert(PairStreamToBufHandler(STREAM_ID_PASS1_RESIZE_MAIN2, Zsd_ResizedRaw_main2));
                streamToPendingQue.insert(PairStreamToPendingQue(STREAM_ID_PASS1_RESIZE_MAIN2, &pendingQueueResizeRaw_main2));
                streamToBufMgr.insert(PairStreamToBufMgr(STREAM_ID_PASS1_RESIZE_MAIN2, mpBufMgr_Zsd_ResizedRaw_main2));
            }
            if(!Zsd_ResizedRaw_main2->setConfig(configInfo)){
                MY_LOGE("Zsd_ResizedRaw_main2->setConfig failed");
            };
            break;
        case STREAM_ID_PASS1_FULLSIZE:
            if(Zsd_FullRaw == NULL){
                Zsd_FullRaw = StereoPrvStreamBufHandler::createInstance(getOpenId(), "Zsd_FullRaw");
                mpBufMgr_Zsd_FullRaw = configInfo.pBufMgr;
                streamToBufHandler.insert(PairStreamToBufHandler(STREAM_ID_PASS1_FULLSIZE, Zsd_FullRaw));
                streamToPendingQue.insert(PairStreamToPendingQue(STREAM_ID_PASS1_FULLSIZE, &pendingQueueFullRaw));
                streamToBufMgr.insert(PairStreamToBufMgr(STREAM_ID_PASS1_FULLSIZE, mpBufMgr_Zsd_FullRaw));
            }
            if(!Zsd_FullRaw->setConfig(configInfo)){
                MY_LOGE("Zsd_FullRaw->setConfig failed");
            };
            break;
        case STREAM_ID_PASS1_FULLSIZE_MAIN2:
            if(Zsd_FullRaw_main2 == NULL){
                Zsd_FullRaw_main2 = StereoPrvStreamBufHandler::createInstance(getOpenId(), "Zsd_FullRaw_main2");
                mpBufMgr_Zsd_FullRaw_main2 = configInfo.pBufMgr;
                streamToBufHandler.insert(PairStreamToBufHandler(STREAM_ID_PASS1_FULLSIZE_MAIN2, Zsd_FullRaw_main2));
                streamToPendingQue.insert(PairStreamToPendingQue(STREAM_ID_PASS1_FULLSIZE_MAIN2, &pendingQueueFullRaw_main2));
                streamToBufMgr.insert(PairStreamToBufMgr(STREAM_ID_PASS1_FULLSIZE_MAIN2, mpBufMgr_Zsd_FullRaw_main2));
            }
            if(!Zsd_FullRaw_main2->setConfig(configInfo)){
                MY_LOGE("Zsd_FullRaw_main2->setConfig failed");
            };
            break;
        default:
            MY_LOGE("Unknown stream %#" PRIxPTR " . Should not have happended!", configInfo.pStreamInfo->getStreamId());
            break;
    }

    FUNC_END;
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MERROR
StereoStreamBufHandlerImpl::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    sp<IImageStreamInfo> const pStreamInfo,
    sp<HalImageStreamBuffer> &rpStreamBuffer)

{
    FUNC_START;
    Mutex::Autolock lock(mLock);

    MY_LOGD("req(%d), dequeueStreamBuffer %#" PRIxPTR ".", iRequestNo, pStreamInfo->getStreamId());

    map<StreamId_T, sp<StereoPrvStreamBufHandler> >::iterator theZsdStreamHandler = streamToBufHandler.find(pStreamInfo->getStreamId());

    if(theZsdStreamHandler == streamToBufHandler.end()){
        MY_LOGE("dequeue stream %#" PRIxPTR " not exist!", pStreamInfo->getStreamId());
        return NAME_NOT_FOUND;
    }

    MERROR ret = theZsdStreamHandler->second->dequeStreamBuffer(iRequestNo, pStreamInfo, rpStreamBuffer);

    if(ret != OK){
        MY_LOGE("dequeue stream %#" PRIxPTR " from zsdStreamBufHandlers failed!", pStreamInfo->getStreamId());
        return ret;
    }

if(mDebugMode == eWORK_AS_SIMPLE_WARPPER){
    MY_LOGD("eWORK_AS_SIMPLE_WARPPER");
    FUNC_END;
    return OK;
}
    IMetadata new_metadata_hal;
    IMetadata new_metadata_app;
    META_INFO new_meta_hal = {MFALSE, new_metadata_hal};
    META_INFO new_meta_app = {MFALSE, new_metadata_app};
    STREAM_BUF_INFO newDequedStream = {
                                        iRequestNo,                  //iReqNum;
                                        pStreamInfo,                 //pStreamInfo
                                        rpStreamBuffer,              //rpStreamBuffer
                                        STREAM_BUFFER_STATUS::WRITE, //bBufStatus
                                        new_meta_hal,                //pMeta_hal
                                        new_meta_app,                //pMeta_app
                                        MFALSE                       //isBufferReturned
                                    };

    if(streamToPendingQue.count(pStreamInfo->getStreamId())){
        streamToPendingQue[pStreamInfo->getStreamId()]->push_back(newDequedStream);
    }else{
        MY_LOGE("undefined streamId%#" PRIxPTR ". should not have happended!", pStreamInfo->getStreamId());
        return NAME_NOT_FOUND;
    }

    FUNC_END;
    return OK;
}

MBOOL
StereoStreamBufHandlerImpl::
enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(StreamId_T streamId, MUINT32 bBufStatus)
{
    MY_LOGD1("streamId%#" PRIxPTR ".", streamId);

    list<STREAM_BUF_INFO>* thePendiningQue = NULL;
    sp<StereoPrvStreamBufHandler> theZsdStreamHandler = NULL;
    StereoBufMgr* theBufMgr = NULL;
    STREAM_BUF_INFO bufferInfo;

    thePendiningQue         = streamToPendingQue[streamId];
    theZsdStreamHandler     = streamToBufHandler[streamId];
    theBufMgr               = streamToBufMgr[streamId];

    if(thePendiningQue->size() > 0){
        bufferInfo = thePendiningQue->front();
        thePendiningQue->pop_front();
    }else{
        return MTRUE;
    }

    if(bufferInfo.isBufferReturned){
        MERROR err = theZsdStreamHandler->enqueStreamBuffer(bufferInfo.pStreamInfo, bufferInfo.rpStreamBuffer, bBufStatus);
        if(err != OK){
            MY_LOGE("UNKNOWN_ERROR doing theZsdStreamHandler->enqueStreamBuffer");
            return MFALSE;
        }
    }

// return here if work as simple wrapper
if(mDebugMode == eWORK_AS_SIMPLE_WARPPER){
    MY_LOGD("eWORK_AS_SIMPLE_WARPPER");
    return MTRUE;
}

    if(bufferInfo.pMeta_hal.isMetadataSet){
        if(theBufMgr == NULL){
            MY_LOGE("theBufMgr is NULL, should not have happended!");
        }
         MBOOL ret = theBufMgr->setMetadata(bufferInfo.iReqNum, &(bufferInfo.pMeta_hal.metadata));
        if(ret != MTRUE){
            MY_LOGE("UNKNOWN_ERROR doing theBufMgr->setMetadata");
            return MFALSE;
        }
    }

    return MTRUE;
}

MBOOL
StereoStreamBufHandlerImpl::
syncRoutine()
{
    FUNC_START;
    READY_RESULT readyResult = isReadyToPushToZsdStreamMgr();
if(mDebugMode == eSKIP_META_MECHANISM){
    MY_LOGD("eSKIP_META_MECHANISM");

    if(readyResult == READY_RESULT_OK){
        if(pendingQueueResizeRaw.size() > 0){
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE,            STREAM_BUFFER_STATUS::WRITE_OK);
        }
        if(pendingQueueResizeRaw_main2.size() > 0){
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE_MAIN2,      STREAM_BUFFER_STATUS::WRITE_OK);
        }
        if(pendingQueueFullRaw.size() > 0){
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE,           STREAM_BUFFER_STATUS::WRITE_OK);
        }
        if(pendingQueueFullRaw_main2.size() > 0){
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE_MAIN2,     STREAM_BUFFER_STATUS::WRITE_OK);
        }
    }else{
        MY_LOGD("readyResult not OK => %d", readyResult);
    }
    FUNC_END;
    return MTRUE;
}

    while(readyResult == READY_RESULT_OK){
        // step 3.
        // check timestamps to decide whether to enque with OK(synchorized) or BAD(not sync) status to zsdStreamBufHandlers.
        mNoMetaCount = 0;
        SYNC_RESULT sync_result = isTimeSync();

        switch(sync_result){
            case SYNC_RESULT_MAIN1_TOO_OLD:
                // Main1 too old
                // enque Main1 buffer with "Bad" status
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE,      STREAM_BUFFER_STATUS::ERROR);
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE,    STREAM_BUFFER_STATUS::ERROR);
                break;
            case SYNC_RESULT_MAIN2_TOO_OLD:
                // Main2 too old
                // enque Main2 buffer with "Bad" status
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE_MAIN2,      STREAM_BUFFER_STATUS::ERROR);
                if(mpBufMgr_Zsd_FullRaw_main2 != NULL){
                    enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE_MAIN2,    STREAM_BUFFER_STATUS::ERROR);
                }
                break;
            case SYNC_RESULT_PAIR_NOT_SYNC:
                // This buffet pair is not sync.
                // enque Both buffer with "Bad" status
                // main1
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE,      STREAM_BUFFER_STATUS::ERROR);
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE,    STREAM_BUFFER_STATUS::ERROR);
                // main2
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE_MAIN2,      STREAM_BUFFER_STATUS::ERROR);
                if(mpBufMgr_Zsd_FullRaw_main2 != NULL){
                    enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE_MAIN2,    STREAM_BUFFER_STATUS::ERROR);
                }
                break;
            default:
                // SYNC_RESULT_OK
                // This buffet pair is synchronized.
                // enque Both buffer with "Good" status
                // main1
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE,      STREAM_BUFFER_STATUS::WRITE_OK);
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE,    STREAM_BUFFER_STATUS::WRITE_OK);
                // main2
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE_MAIN2,      STREAM_BUFFER_STATUS::WRITE_OK);
                if(mpBufMgr_Zsd_FullRaw_main2 != NULL){
                    enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE_MAIN2,    STREAM_BUFFER_STATUS::WRITE_OK);
                }
                break;
        }

        // keep checking if pending queue is ready
        readyResult = isReadyToPushToZsdStreamMgr();
    }

    if(readyResult == READY_RESULT_NO_META){
        mNoMetaCount ++;
        MY_LOGD("mNoMetaCount = %d", mNoMetaCount);
        if(mNoMetaCount > NO_META_TIMEOUT_COUNT){
            MY_LOGD("This buffet pair's meta is not ready for too long");
            // This buffet pair's meta is not ready for too long.
            // enque Both buffer with "Bad" status
            // main1
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE,      STREAM_BUFFER_STATUS::ERROR);
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE,    STREAM_BUFFER_STATUS::ERROR);
            // main2
            enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_RESIZE_MAIN2,      STREAM_BUFFER_STATUS::ERROR);
            if(mpBufMgr_Zsd_FullRaw_main2 != NULL){
                enqueToZsdStreamBufHandlerAndSetMetaToBufMgr(STREAM_ID_PASS1_FULLSIZE_MAIN2,    STREAM_BUFFER_STATUS::ERROR);
            }
            mNoMetaCount = 0;
        }
    }

    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MERROR
StereoStreamBufHandlerImpl::
enqueStreamBuffer(
    sp<IImageStreamInfo> const pStreamInfo,
    sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32  bBufStatus)

{
    FUNC_START;
    Mutex::Autolock lock(mLock);

    MY_LOGD("enqueueStreamBuffer %#" PRIxPTR ".", pStreamInfo->getStreamId());

if(mDebugMode == eWORK_AS_SIMPLE_WARPPER){
    MY_LOGD("eWORK_AS_SIMPLE_WARPPER");
    map<StreamId_T, sp<StereoPrvStreamBufHandler> >::iterator theZsdStreamHandler = streamToBufHandler.find(pStreamInfo->getStreamId());

    if(theZsdStreamHandler == streamToBufHandler.end()){
        MY_LOGE("enque stream %#" PRIxPTR " not exist!", pStreamInfo->getStreamId());
        return NAME_NOT_FOUND;
    }

    bBufStatus = STREAM_BUFFER_STATUS::WRITE_OK;
    // if(testCounter%5 == 0){
    //     MY_LOGD("testCounter=%d, intentionally set STREAM_BUFFER_STATUS::ERROR", testCounter);
    //     bBufStatus = STREAM_BUFFER_STATUS::ERROR;
    // }

    // testCounter++;

    MERROR ret = theZsdStreamHandler->second->enqueStreamBuffer(pStreamInfo, rpStreamBuffer, bBufStatus);

    if(ret != OK){
        MY_LOGE("enque stream %#" PRIxPTR " from zsdStreamBufHandlers failed!", pStreamInfo->getStreamId());
        return ret;
    }
    FUNC_END;
    return OK;
}

    // step 1. find the corresponding buffer and marked it as buffer returned
    if(streamToPendingQue.count(pStreamInfo->getStreamId())){
        list<STREAM_BUF_INFO>* thePendiningQue = streamToPendingQue[pStreamInfo->getStreamId()];
        for (list<STREAM_BUF_INFO>::iterator it=thePendiningQue->begin(); it != thePendiningQue->end();){
            if( (*it).rpStreamBuffer == rpStreamBuffer){
                //
                (*it).isBufferReturned  = MTRUE;
                (*it).bBufStatus        = bBufStatus;
                //
                break;
            }

            it++;

            if(it == thePendiningQue->end()){
                MY_LOGE("undefined streamId%#" PRIxPTR ". stream_addr:%p not in pending queue. should not have happended!", pStreamInfo->getStreamId(),rpStreamBuffer.get());
                return UNKNOWN_ERROR;
            }
        }
    }else{
        MY_LOGE("undefined streamId%#" PRIxPTR ". should not have happended!", pStreamInfo->getStreamId());
        return NAME_NOT_FOUND;
    }

    // step 2.
    // check whether or not buffers from both sensor have arrived, if so, start syncCheck
    syncRoutine();

    FUNC_END;
    return OK;
}

/*******************************************************************************
*
********************************************************************************/
READY_RESULT
StereoStreamBufHandlerImpl::
isReadyToPushToZsdStreamMgr()
{

    FUNC_START;

if(mDebugMode == eSKIP_META_MECHANISM){
    MY_LOGD("eSKIP_META_MECHANISM");
    for(map<StreamId_T, list<STREAM_BUF_INFO>*>::iterator it=streamToPendingQue.begin(); it!=streamToPendingQue.end(); ++it){
        list<STREAM_BUF_INFO>* theQue = it->second;
        MY_LOGD1("streamId%#" PRIxPTR " pending Queue size: %d", it->first, theQue->size());
        // return FALSE when someone is not ready
        if(theQue->empty()){
            return READY_RESULT_EMPTY;
        }
    }
    return  READY_RESULT_OK;
}

    // show sizes
    for(map<StreamId_T, list<STREAM_BUF_INFO>*>::iterator it=streamToPendingQue.begin(); it!=streamToPendingQue.end(); ++it){
        list<STREAM_BUF_INFO>* theQue = it->second;
        MY_LOGD1("streamId%#" PRIxPTR " pending Queue size: %d", it->first, theQue->size());

        if(theQue->size() > 0){
            STREAM_BUF_INFO theBufInfo = theQue->front();
            MY_LOGD1("front element: isBufferReturned=%d, pMeta_app.isMetadataSet=%d, pMeta_hal.isMetadataSet=%d",
                theBufInfo.isBufferReturned,
                theBufInfo.pMeta_app.isMetadataSet,
                theBufInfo.pMeta_hal.isMetadataSet
            );
        }
    }

    for(map<StreamId_T, list<STREAM_BUF_INFO>*>::iterator it=streamToPendingQue.begin(); it!=streamToPendingQue.end(); ++it){
        list<STREAM_BUF_INFO>* theQue = it->second;
        // return FALSE when someone is not ready
        if(theQue->empty()){
            // return FALSE when someone is empty
            MY_LOGD1("readyResult = READY_RESULT_EMPTY");
            return READY_RESULT_EMPTY;
        }else{
            // return FALSE when someone's meta or buffer is not ready
            STREAM_BUF_INFO theBufInfo = theQue->front();
            if( theBufInfo.isBufferReturned == MFALSE){
                MY_LOGD1("readyResult = READY_RESULT_NO_BUFFER");
                return READY_RESULT_NO_BUFFER;
            }
            if( theBufInfo.pMeta_hal.isMetadataSet == MFALSE || theBufInfo.pMeta_app.isMetadataSet == MFALSE){
                MY_LOGD1("readyResult = READY_RESULT_NO_META");
                return READY_RESULT_NO_META;
            }
        }
    }

    MY_LOGD1("readyResult = READY_RESULT_OK");
    FUNC_END;
    return READY_RESULT_OK;
}

/*******************************************************************************
*
********************************************************************************/
SYNC_RESULT
StereoStreamBufHandlerImpl::
isTimeSync()
{
    SYNC_RESULT ret = SYNC_RESULT_PAIR_OK;

    // get oldest meta and check the timestamp
    // we just compare resize raw since the timestamp is same with fullraw

    MY_LOGD1("pendingQueueResizeRaw.size=%d, pendingQueueResizeRaw_main2.size=%d", pendingQueueResizeRaw.size(), pendingQueueResizeRaw_main2.size());

    STREAM_BUF_INFO bufInfo_main1 = pendingQueueResizeRaw.front();
    STREAM_BUF_INFO bufInfo_main2 = pendingQueueResizeRaw_main2.front();

    IMetadata::IEntry const entry_main1 = bufInfo_main1.pMeta_app.metadata.entryFor(MTK_SENSOR_TIMESTAMP);
    IMetadata::IEntry const entry_main2 = bufInfo_main2.pMeta_app.metadata.entryFor(MTK_SENSOR_TIMESTAMP);
    MINT64 timestamp_main1;
    MINT64 timestamp_main2;
    int timestamp_main1_ms;
    int timestamp_main2_ms;
    int timestamp_diff;

    if (
        (!entry_main1.isEmpty()) &&
        (entry_main1.tag() == MTK_SENSOR_TIMESTAMP) &&
        (!entry_main2.isEmpty()) &&
        (entry_main2.tag() == MTK_SENSOR_TIMESTAMP)
    )
    {
        timestamp_main1 = entry_main1.itemAt(0, Type2Type<MINT64>());
        timestamp_main2 = entry_main2.itemAt(0, Type2Type<MINT64>());
        timestamp_main1_ms = timestamp_main1/1000000;
        timestamp_main2_ms = timestamp_main2/1000000;
        timestamp_diff     = abs(timestamp_main1_ms - timestamp_main2_ms);
    }else{
        MY_LOGE("can not get timestamp meta");
        if(entry_main1.isEmpty()){MY_LOGE("entry_main1.isEmpty()");}
        if(entry_main2.isEmpty()){MY_LOGE("entry_main2.isEmpty()");}
        if(entry_main1.tag() != MTK_SENSOR_TIMESTAMP){MY_LOGE("entry_main1.tag() != MTK_SENSOR_TIMESTAMP");}
        if(entry_main2.tag() != MTK_SENSOR_TIMESTAMP){MY_LOGE("entry_main2.tag() != MTK_SENSOR_TIMESTAMP");}
        return SYNC_RESULT_PAIR_NOT_SYNC;
    }

    MY_LOGD1("timestamp main1: %lld (ns) / %d (ms)", timestamp_main1, timestamp_main1_ms);
    MY_LOGD1("timestamp main2: %lld (ns) / %d (ms)", timestamp_main2, timestamp_main2_ms);
    MY_LOGD1("timestamp diff: %d (ms)", timestamp_diff);
    MY_LOGD1("PAIR_THRESHOLD_MS=%d  SYNC_THRESHOLD_MS=%d", PAIR_THRESHOLD_MS, SYNC_THRESHOLD_MS);

    if(timestamp_diff < PAIR_THRESHOLD_MS){
        if(timestamp_diff <= SYNC_THRESHOLD_MS){
            MY_LOGD1("SYNC_RESULT_PAIR_OK");
            return SYNC_RESULT_PAIR_OK;
        }else{
            MY_LOGD1("SYNC_RESULT_PAIR_NOT_SYNC");
            return SYNC_RESULT_PAIR_NOT_SYNC;
        }
    }else{
        if(timestamp_main1 > timestamp_main2){
            MY_LOGD1("SYNC_RESULT_MAIN2_TOO_OLD");
            return SYNC_RESULT_MAIN2_TOO_OLD;
        }else{
            MY_LOGD1("SYNC_RESULT_MAIN1_TOO_OLD");
            return SYNC_RESULT_MAIN1_TOO_OLD;
        }
    }

    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoStreamBufHandlerImpl::
setMetadata(    MUINT32 iReqNum,
                MUINT32 streamId,
                IMetadata* pMetadata,
                MBOOL isMain2)
{
    FUNC_START;
    Mutex::Autolock lock(mLock);

    MY_LOGD1("req(%d), streamId%#" PRIxPTR " isMain2=%d,  pMetadata=%p", iReqNum, streamId, isMain2, pMetadata);

if(mDebugMode == eWORK_AS_SIMPLE_WARPPER){
    MY_LOGD("eWORK_AS_SIMPLE_WARPPER");
    return MTRUE;
}

if(mDebugMode == eSKIP_META_MECHANISM){
    MY_LOGD("eSKIP_META_MECHANISM");
    return MTRUE;
}
    // step 1. find the corresponding meta and update the value
    list<STREAM_BUF_INFO>* thePendiningQue_resized = NULL;
    list<STREAM_BUF_INFO>* thePendiningQue_full = NULL;

    if(isMain2){
        thePendiningQue_resized = &pendingQueueResizeRaw_main2;
        if(mpBufMgr_Zsd_FullRaw_main2 != NULL){
            thePendiningQue_full = &pendingQueueFullRaw_main2;
        }
    }else{
        thePendiningQue_resized = &pendingQueueResizeRaw;
        thePendiningQue_full = &pendingQueueFullRaw;
    }


    // First we update the meta with pending resized raw
    if(thePendiningQue_resized == NULL){
        MY_LOGE("no resizedRaw. should not have happended");
        return MFALSE;
    }

    for (list<STREAM_BUF_INFO>::iterator it=thePendiningQue_resized->begin(); it != thePendiningQue_resized->end();){
        if( (*it).iReqNum == iReqNum){
            //
            if(streamId == STREAM_ID_METADATA_RESULT_P1_HAL || streamId == STREAM_ID_METADATA_RESULT_P1_HAL_MAIN2){
                (*it).pMeta_hal.metadata        = (*pMetadata);
                (*it).pMeta_hal.isMetadataSet   = MTRUE;
            }else{
                (*it).pMeta_app.metadata        = (*pMetadata);
                (*it).pMeta_app.isMetadataSet   = MTRUE;
            }
            //
            break;
        }

        it++;

        if(it == thePendiningQue_resized->end()){
            MY_LOGE("undefined streamId%#" PRIxPTR ". iReqNum:%d not in pending queue. Should not have happended! Or the buffer wait too long that its already pushed into bufMgr", streamId, iReqNum);
            return MFALSE;
        }
    }

    if(thePendiningQue_full != NULL){
        // Then we update the meta with pending full raw
        for (list<STREAM_BUF_INFO>::iterator it=thePendiningQue_full->begin(); it != thePendiningQue_full->end();){
            if( (*it).iReqNum == iReqNum){
                //
                if(streamId == STREAM_ID_METADATA_RESULT_P1_HAL || streamId == STREAM_ID_METADATA_RESULT_P1_HAL_MAIN2){
                    (*it).pMeta_hal.metadata        = (*pMetadata);
                    (*it).pMeta_hal.isMetadataSet   = MTRUE;
                }else{
                    (*it).pMeta_app.metadata        = (*pMetadata);
                    (*it).pMeta_app.isMetadataSet   = MTRUE;
                }
                //
                break;
            }

            it++;

            if(it == thePendiningQue_full->end()){
                MY_LOGE("undefined streamId%#" PRIxPTR ". iReqNum:%d not in pending queue. should not have happended! Or the buffer wait too long that its already pushed into bufMgr", streamId, iReqNum);
                return MFALSE;
            }
        }
    }

    // step 2.
    // check whether or not buffers from both sensor have arrived, if so, start syncCheck
    syncRoutine();

    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoStreamBufHandlerImpl::
stopDequeBufMgrs(){
    FUNC_START;

    for(map<StreamId_T, StereoBufMgr*>::iterator it=streamToBufMgr.begin(); it!=streamToBufMgr.end(); ++it){
        it->second->stopDequeBuf();
    }

    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
}; // namespace NSCamStreamBufProvider
