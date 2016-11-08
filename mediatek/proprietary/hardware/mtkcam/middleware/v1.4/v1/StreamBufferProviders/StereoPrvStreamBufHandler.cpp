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
#define LOG_TAG "MtkCam/STEREOPRVBHdl"

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
//using namespace NSCam::v3;
//using namespace NSCam::v3::Utils;

//
#include <cutils/properties.h>
//
#include <list>
#include <vector>
using namespace std;
//
#include <iopipe/Port.h>
//
#include <utils/include/ImageBufferHeap.h>
//
#include <v3/utils/streambuf/IStreamBufferProvider.h>
#include <metadata/IMetadata.h>
#include <v1/StreamBufferProviders/StereoBufMgr.h>
using namespace NSMtkBufMgr;
using namespace NSCam::v3;
using namespace NSCam::v3::Utils;
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>
#include <ImgBufProvidersManager.h>
#include <v1/StreamBufferProviders/StereoPrvStreamBufHandler.h>




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
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)
//
#define FUNC_START  MY_LOGD("+")
#define FUNC_END    MY_LOGD("-")
#define FUNC_NAME   MY_LOGD("")
//
/*******************************************************************************
*
********************************************************************************/
namespace NSCamStreamBufProvider {

class StereoPrvStreamBufHandlerImpl : public StereoPrvStreamBufHandler
{
    public:
        StereoPrvStreamBufHandlerImpl(MUINT32 openId, const char* userName);
        ~StereoPrvStreamBufHandlerImpl();

    //DECLARE_ICAMBUFHANDLER_INTERFACE();
    MERROR                              dequeStreamBuffer(
                                            MUINT32 const iRequestNo,
                                            android::sp<IImageStreamInfo> const pStreamInfo,
                                            android::sp<HalImageStreamBuffer> &rpStreamBuffer);

    MERROR                              enqueStreamBuffer(
                                            android::sp<IImageStreamInfo> const pStreamInfo,
                                            android::sp<HalImageStreamBuffer> rpStreamBuffer,
                                            MUINT32  bBufStatus);
    //
    virtual MBOOL   setConfig(CONFIG_INFO configInfo);
    virtual MBOOL   setProviderWrapper(sp<IStreamBufferProvider> wrapper);
    virtual MBOOL   init();
    virtual MBOOL   uninit();

    typedef struct
    {
        MUINT32 requestNum;
        android::sp<IImageBufferHeap> imageBufferHeapAddr;
        android::sp<HalImageStreamBuffer> halImageStreamBufferAddr;
        MBOOL pushed;
    }CAP_BUF_STRUCT;
    //
    protected:
        //
        MUINT32 getOpenId() {return mOpenId;}
        const char* getName() const {return msName;}

    private:
        MUINT32                     mOpenId;
        mutable Mutex               mLock;
        const char* const           msName;
        list<CAP_BUF_STRUCT>        mCapBuffers;
        MUINT32                     mKeepBufCount; // ex. zsd->3, vss->1
        StereoBufMgr*               mpBufMgr;
        sp<IImageStreamInfo>        mspBufStreamInfo;
        list< android::sp<IImageBufferHeap> >        mlspInitBuffers;
        sp<IStreamBufferProvider>   mspProviderWrapper;

};


/*******************************************************************************
*
********************************************************************************/
StereoPrvStreamBufHandler*
StereoPrvStreamBufHandler::
createInstance(
    MUINT32     openId,
    const char* userName)
{
    return new StereoPrvStreamBufHandlerImpl(openId, userName);
}


/*******************************************************************************
*
********************************************************************************/
MVOID
StereoPrvStreamBufHandler::
destroyInstance()
{
    //delete this;
}


/*******************************************************************************
*
********************************************************************************/
StereoPrvStreamBufHandler::
StereoPrvStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
StereoPrvStreamBufHandler::
~StereoPrvStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
StereoPrvStreamBufHandlerImpl::
StereoPrvStreamBufHandlerImpl(
    MUINT32     openId,
    const char* userName)
    : StereoPrvStreamBufHandler()
    , mOpenId(openId)
    , msName(userName)
    , mKeepBufCount(0)
    , mpBufMgr(NULL)
    , mspBufStreamInfo(NULL)
    , mspProviderWrapper(NULL)
{
    FUNC_NAME;

}


/*******************************************************************************
*
********************************************************************************/
StereoPrvStreamBufHandlerImpl::
~StereoPrvStreamBufHandlerImpl()
{
    FUNC_NAME;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoPrvStreamBufHandlerImpl::
init()
{
    FUNC_START;
    if (mspBufStreamInfo == NULL || mpBufMgr == NULL)
    {
        MY_LOGE("mspBufStreamInfo or mpBufMgr not ready");
        return MFALSE;
    }
    //
    mCapBuffers.clear();
    mlspInitBuffers.clear();

    // 1. create N+3 buffer, and prepare mapping table for N+3 buffers
    IImageStreamInfo::BufPlanes_t const& bufPlanes = mspBufStreamInfo->getBufPlanes();
    size_t bufStridesInBytes[3] = {0};
    size_t bufBoundaryInBytes[3]= {0};
    for (size_t i = 0; i < bufPlanes.size(); i++) {
        bufStridesInBytes[i] = bufPlanes[i].rowStrideInBytes;
    }

    IImageBufferAllocator::ImgParam const imgParam(
        mspBufStreamInfo->getImgFormat(),
        mspBufStreamInfo->getImgSize(),
        bufStridesInBytes, bufBoundaryInBytes,
        bufPlanes.size()
    );
    //

    CAP_BUF_STRUCT tempBufInfo;
    for (int i=0; i < mKeepBufCount+3; i++)
    {
        android::sp<IIonImageBufferHeap> pImageBufferHeap =
        IIonImageBufferHeap::create(
            mspBufStreamInfo->getStreamName(),
            imgParam,
            IIonImageBufferHeap::AllocExtraParam(),
            MFALSE
        );

        tempBufInfo.requestNum = 0;
        tempBufInfo.imageBufferHeapAddr = pImageBufferHeap; // push generated imageBufferHeap addr
        tempBufInfo.halImageStreamBufferAddr = NULL;
        tempBufInfo.pushed = MFALSE;
        mCapBuffers.push_back(tempBufInfo);
        MY_LOGD("push to mCapBuffers = %p", pImageBufferHeap.get());
        // all created buffer are put in init buffers initially
        mlspInitBuffers.push_back(pImageBufferHeap);
    }

    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoPrvStreamBufHandlerImpl::
uninit()
{
    FUNC_START;
    //
    // 1. pop all buffer from CapBufMgr
    // 2. release all poped buffer
    // 3. clear mapping table
    mCapBuffers.clear();
    mlspInitBuffers.clear();
    //
    mspProviderWrapper = NULL;
    //
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoPrvStreamBufHandlerImpl::
setConfig(CONFIG_INFO configInfo)
{
    FUNC_START;
    //
    if(configInfo.pBufMgr == NULL)
    {
        MY_LOGE("ZSD config BufMgr is NULL");
        return MFALSE;
    }
    if(configInfo.pStreamInfo == NULL)
    {
        MY_LOGE("ZSD config StreamInfo is NULL");
        return MFALSE;
    }
    mKeepBufCount = configInfo.uiKeepBufCount;
    mpBufMgr = configInfo.pBufMgr;
    mspBufStreamInfo = configInfo.pStreamInfo;
    //
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoPrvStreamBufHandlerImpl::
setProviderWrapper(sp<IStreamBufferProvider> wrapper)
{
    FUNC_START;

    mspProviderWrapper = wrapper;

    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MERROR
StereoPrvStreamBufHandlerImpl::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> &rpStreamBuffer)

{
    Mutex::Autolock _l(mLock);
    FUNC_START;
    MBOOL ret = MFALSE;

    android::sp<IImageBufferHeap> bufForDeque = NULL;
    // if buffer in init buf are all used, get buf from buf mgr
    if (mlspInitBuffers.size() == 0)
    {
        ret = mpBufMgr->popBuf(bufForDeque);
        if (!ret)
        {
            MY_LOGW("- popBuf fail");
            return BAD_VALUE;
        }
    }
    else // get buf rom init buffers
    {
        list< android::sp<IImageBufferHeap> >::iterator it;
        it = mlspInitBuffers.begin();
        bufForDeque = (*it);
        MY_LOGD("bufForDeque = %p", bufForDeque.get());
        mlspInitBuffers.erase(mlspInitBuffers.begin());
    }


    // 3. Provided buff address to rpStreamBuffer
    android::sp<HalImageStreamBufferProvider> pStreamBufferProvider = NULL;
    if(mspProviderWrapper == NULL){
        pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, bufForDeque, this);
    }else{
        pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, bufForDeque, mspProviderWrapper);
    }
    rpStreamBuffer = pStreamBufferProvider;
    //
    if ( rpStreamBuffer == 0)
    {
        MY_LOGD("Set buffer as NULL by empty buffer container");
        return NO_MEMORY;
    }

    // update mCapBuffers information
    list<CAP_BUF_STRUCT>::iterator it;
    MBOOL isFound = MFALSE;
    for(it = mCapBuffers.begin(); it != mCapBuffers.end(); it++)
    {
        if ((*it).imageBufferHeapAddr.get() == bufForDeque.get())
        {
            (*it).requestNum = iRequestNo;
            (*it).halImageStreamBufferAddr = rpStreamBuffer;
            (*it).pushed = MFALSE;
            isFound = MTRUE;
            MY_LOGD("found in mCapBuffers Req(%d), rpStreamBuffer=%p, imageBufferHeapAddr=%p",iRequestNo,rpStreamBuffer.get(), (*it).imageBufferHeapAddr.get());
            break;
        }
    }
    if (!isFound)
    {
        MY_LOGE("not found in mCapBuffers");
        return UNKNOWN_ERROR;
    }

    FUNC_END;
    return OK;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
StereoPrvStreamBufHandlerImpl::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32  bBufStatus)

{
    Mutex::Autolock _l(mLock);
    FUNC_START;
    MBOOL isMatched = MFALSE;
    // 1. use mapping table with rpStreamBuffer address to find buff address and iRequestNo

    list<CAP_BUF_STRUCT>::iterator it;
    MUINT32 requestNum;
    android::sp<IImageBufferHeap> matchedImageBufferHeap;
    MY_LOGD("rpStreamBuffer = %p", rpStreamBuffer.get());
    for(it = mCapBuffers.begin(); it != mCapBuffers.end(); it++)
    {
        if ((*it).halImageStreamBufferAddr.get() == rpStreamBuffer.get())
        {
            requestNum = (*it).requestNum;
            matchedImageBufferHeap = (*it).imageBufferHeapAddr;
            isMatched = MTRUE;
            MY_LOGD("found in mCapBuffers Req(%d), rpStreamBuffer=%p, matchedImageBufferHeap=%p",requestNum,rpStreamBuffer.get(),matchedImageBufferHeap.get());
            break;
        }
    }
    if(isMatched != MTRUE)
    {
        MY_LOGW("- isMatched != MTRUE");
        return BAD_VALUE;
    }
    // 2. push to buf mgr
    MBOOL usable;
    if(bBufStatus == STREAM_BUFFER_STATUS::ERROR){
        // If the buf status is not good, marked as used, which means ZsdShotStreamBufHandler can not get this buffer.
        MY_LOGD("bBufStatus == STREAM_BUFFER_STATUS::ERROR, usable -> MFALSE");
        usable = MFALSE;
    }else{
        // Otherwise, marked as usable
        usable = MTRUE;
    }
    mpBufMgr->pushBuf(matchedImageBufferHeap, requestNum, usable);

    // 3. mark the mapping table as used
    (*it).pushed = MTRUE;
    FUNC_END;
    return OK;
}



}; // namespace NSCamStreamBufProvider

