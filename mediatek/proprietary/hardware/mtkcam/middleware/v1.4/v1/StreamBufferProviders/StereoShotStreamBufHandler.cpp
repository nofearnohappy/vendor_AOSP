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
#define LOG_TAG "MtkCam/STEREOSHOTBHdl"

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

#include <metadata/IMetadata.h>
#include <v1/StreamBufferProviders/StereoBufMgr.h>
using namespace NSMtkBufMgr;
#include <v3/utils/streambuf/IStreamBufferProvider.h>
#include <ImgBufProvidersManager.h>
using namespace NSCam::v3;
using namespace NSCam::v3::Utils;
using namespace android;
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>
#include <v1/StreamBufferProviders/StereoShotStreamBufHandler.h>
//
//
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
#define MY_LOGD1(...)               MY_LOGD_IF(1<=mLogLevel, __VA_ARGS__)
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

class StereoShotStreamBufHandlerImpl : public StereoShotStreamBufHandler
{
    public:
        StereoShotStreamBufHandlerImpl(MUINT32 openId, const char* userName);
        ~StereoShotStreamBufHandlerImpl();

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
    virtual MBOOL   init(StereoBufMgr* bufMgr);
    virtual MBOOL   uninit();
    virtual MBOOL   prepareBufAndMeta();
    virtual MBOOL   getMetadata(IMetadata* metadata);
    virtual MBOOL   stopDequeBufMgr();

    typedef struct
    {
        android::sp<IImageBufferHeap>       imageBufferHeap;
        IMetadata                           metadata;
        android::sp<HalImageStreamBuffer>   rpStreamBuffer;
        MBOOL                               isDequed;
    }DEQ_BUF_STRUCT;

    //
    protected:
        //
        MUINT32 getOpenId() {return mOpenId;}
        const char* getName() const {return msName;}
        //
    private:
        MUINT32                                     mOpenId;
        MINT32                                      mLogLevel;
        mutable Mutex                               mLock;
        const char* const                           msName;
        StereoBufMgr*                               mpStereoBufMgr;
        list<DEQ_BUF_STRUCT>                        mDequedBuf;


};


/*******************************************************************************
*
********************************************************************************/
StereoShotStreamBufHandler*
StereoShotStreamBufHandler::
createInstance(
    MUINT32     openId,
    const char* userName)
{
    return new StereoShotStreamBufHandlerImpl(openId, userName);
}


/*******************************************************************************
*
********************************************************************************/
MVOID
StereoShotStreamBufHandler::
destroyInstance()
{

}


/*******************************************************************************
*
********************************************************************************/
StereoShotStreamBufHandler::
StereoShotStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
StereoShotStreamBufHandler::
~StereoShotStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
StereoShotStreamBufHandlerImpl::
StereoShotStreamBufHandlerImpl(
    MUINT32     openId,
    const char* userName)
    : StereoShotStreamBufHandler()
    , mOpenId(openId)
    , msName(userName)
{
    FUNC_NAME;

    char cLogLevel[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "0");
    mLogLevel = ::atoi(cLogLevel);
}


/*******************************************************************************
*
********************************************************************************/
StereoShotStreamBufHandlerImpl::
~StereoShotStreamBufHandlerImpl()
{
    FUNC_NAME;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShotStreamBufHandlerImpl::
init(StereoBufMgr* bufMgr)
{
    FUNC_START;
    //
    mpStereoBufMgr = bufMgr;
    mDequedBuf.clear();
    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShotStreamBufHandlerImpl::
uninit()
{
    FUNC_START;
    //
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShotStreamBufHandlerImpl::
prepareBufAndMeta()
{
    FUNC_START;
    Mutex::Autolock _l(mLock);

    sp<IImageBufferHeap> newDequedBufHeap = NULL;
    IMetadata metadata;
    MBOOL ret = mpStereoBufMgr->dequeBuf(newDequedBufHeap,&metadata);

    MY_LOGD1("dequeBuf = %p", newDequedBufHeap.get());

    if(ret == MTRUE){
        DEQ_BUF_STRUCT newDequedBuf = {newDequedBufHeap, metadata, NULL, MFALSE};
        mDequedBuf.push_back(newDequedBuf);
    }else{
        MY_LOGD1("deque is stopped.");
    }

    FUNC_END;
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShotStreamBufHandlerImpl::
getMetadata(IMetadata* metadata)
{
    FUNC_START;
    Mutex::Autolock _l(mLock);

    if(mDequedBuf.size() > 0){
        DEQ_BUF_STRUCT temp = mDequedBuf.front();
        (*metadata) = temp.metadata;
    }else{
        MY_LOGE("getMetadata when mDequedBuf.size(%d) <= 0, should not have happened!", mDequedBuf.size());
        return MFALSE;
    }

    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
StereoShotStreamBufHandlerImpl::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> &rpStreamBuffer)

{
    FUNC_START;
    Mutex::Autolock _l(mLock);

    MY_LOGD1("req(%d), dequeueStreamBuffer %#" PRIxPTR " + ", iRequestNo, pStreamInfo->getStreamId());
    MY_LOGD1("mDequedBuf.size=%d", mDequedBuf.size());

    // First we try to find a buffer that is not been dequed
    list<DEQ_BUF_STRUCT>::iterator it;
    for(it = mDequedBuf.begin(); it != mDequedBuf.end(); it++)
    {
        MY_LOGD1("(*it).imageBufferHeap=%p, (*it).isDequed=%d", (*it).imageBufferHeap.get(), (*it).isDequed);

        if ((*it).isDequed == MFALSE){
            MY_LOGD1("found in mDequedBuf");
            break;
        }
    }
    if(it == mDequedBuf.end()){
        MY_LOGE("no usable buf in mDequedBuf, should not have happened!");
        return UNKNOWN_ERROR;
    }

    // then we create a halImageStreamBuffer with it
    android::sp<HalImageStreamBufferProvider> pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, (*it).imageBufferHeap, this);
    rpStreamBuffer = pStreamBufferProvider;
    if ( rpStreamBuffer == 0)
    {
        MY_LOGE("Set buffer as NULL by empty buffer container");
        return NO_MEMORY;
    }
    (*it).isDequed = MTRUE;
    (*it).rpStreamBuffer = rpStreamBuffer;

    MY_LOGD("req(%d), dequeueStreamBuffer %#" PRIxPTR ", bufferHeap=%p -", iRequestNo, pStreamInfo->getStreamId(), (*it).imageBufferHeap.get());

    FUNC_END;
    return OK;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
StereoShotStreamBufHandlerImpl::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32  bBufStatus)
{
    FUNC_START;
    Mutex::Autolock _l(mLock);

    MY_LOGD("enque rpStreamBuffer=%p", rpStreamBuffer.get());

    list<DEQ_BUF_STRUCT>::iterator it;
    for(it = mDequedBuf.begin(); it != mDequedBuf.end(); it++)
    {
        if ((*it).rpStreamBuffer.get() == rpStreamBuffer.get()){
            MY_LOGD1("found in mDequedBuf");
            break;
        }
    }
    if(it == mDequedBuf.end()){
        MY_LOGE("can not be found in mDequedBuf, should not have happened!");
        return UNKNOWN_ERROR;
    }

    mpStereoBufMgr->enqueBuf((*it).imageBufferHeap);
    mDequedBuf.erase(it);

    FUNC_END;
    return OK;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShotStreamBufHandlerImpl::
stopDequeBufMgr(){
    FUNC_START;

    if(mpStereoBufMgr != NULL){
        mpStereoBufMgr->stopDequeBuf();
    }

    FUNC_END;
    return MTRUE;
}

}; // namespace NSCamStreamBufProvider

