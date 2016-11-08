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
#define LOG_TAG "MtkCam/JPGSBHdl"

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
#include <v1/PipeDataInfo.h>
using namespace NSMtkPipeDataInfo;
//
#include <v3/utils/streambuf/IStreamBufferProvider.h>
#include <ImgBufProvidersManager.h>
using namespace NSCam::v3;
using namespace NSCam::v3::Utils;
using namespace android;
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>
#include <v1/StreamBufferProviders/JpgStreamBufHandler.h>
//

#include <v1/StreamIDs.h>
using namespace NSMtkStreamId;
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
#define FUNC_START  MY_LOGD("+")
#define FUNC_END    MY_LOGD("-")
#define FUNC_NAME   MY_LOGD("")
//
/******************************************************************************
*
*******************************************************************************/
#define GET_IIMAGEBUFFERHEAP_PLANE_BITS_PER_PIXCEL(pHaep, plane)    (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getPlaneBitsPerPixel(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_VA(pHaep, plane)                       (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getBufVA(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_SIZE(pHaep, plane)                     (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getBufSizeInBytes(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pHaep, plane)           (pHaep->getPlaneCount() >= (plane+1)) ? (pHaep->getBufStridesInBytes(plane)) : 0
#define GET_IIMAGEBUFFERHEAP_ID(pHaep, inxex)                       (pHaep->getHeapIDCount() >= (inxex+1)) ? (pHaep->getHeapID(inxex)) : 0



/*******************************************************************************
*
********************************************************************************/
namespace NSCamStreamBufProvider {

class JpgStreamBufHandlerImpl : public JpgStreamBufHandler
{
    public:
        JpgStreamBufHandlerImpl(MUINT32 openId, const char* userName);
        ~JpgStreamBufHandlerImpl();

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
    virtual MBOOL   init();
    virtual MBOOL   uninit();
    virtual MVOID   setCallbacks(PipeDataCallback_t data_cb,
                                 MVOID* user);



    //
    protected:
        typedef struct
        {
            MUINT iReqNum;
            android::sp<HalImageStreamBuffer> halImageStreamBuffer;
            android::sp<IIonImageBufferHeap>  imageBufferHeap;
        }BUF_INFO;
        //
        MUINT32 getOpenId() {return mOpenId;}
        const char* getName() const {return msName;}
        //
    private:
        MUINT32                     mOpenId;
        mutable Mutex               mLock;
        const char* const           msName;
        vector<BUF_INFO>            mvJpgBufInfo;
        MVOID*                      mpUser;
        PipeDataCallback_t          mDataCb;

    private:
        MVOID   handleDataCallback(MUINT32 const msg,
                               MUINTPTR const ext1,
                               MUINTPTR const ext2,
                               android::sp<IIonImageBufferHeap> const pImgBuf);

};


/*******************************************************************************
*
********************************************************************************/
JpgStreamBufHandler*
JpgStreamBufHandler::
createInstance(
    MUINT32     openId,
    const char* userName)
{
    return new JpgStreamBufHandlerImpl(openId, userName);
}


/*******************************************************************************
*
********************************************************************************/
MVOID
JpgStreamBufHandler::
destroyInstance()
{
    //delete this;
}


/*******************************************************************************
*
********************************************************************************/
JpgStreamBufHandler::
JpgStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
JpgStreamBufHandler::
~JpgStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
JpgStreamBufHandlerImpl::
JpgStreamBufHandlerImpl(
    MUINT32     openId,
    const char* userName)
    : JpgStreamBufHandler()
    , mOpenId(openId)
    , msName(userName)
    , mpUser(NULL)
    , mDataCb(NULL)
{
    FUNC_NAME;
}


/*******************************************************************************
*
********************************************************************************/
JpgStreamBufHandlerImpl::
~JpgStreamBufHandlerImpl()
{
    FUNC_NAME;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
JpgStreamBufHandlerImpl::
init()
{
    FUNC_START;
    //
    mvJpgBufInfo.clear();
    //
    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
JpgStreamBufHandlerImpl::
uninit()
{
    FUNC_START;
    //
    mvJpgBufInfo.clear();
    //
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MERROR
JpgStreamBufHandlerImpl::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> &rpStreamBuffer)

{
    Mutex::Autolock _l(mLock);
    // Must implement BufferHeap creation in this interface
    // for each kind of buffer with enque/deque ops
    //
    // 1. deque/allocate ImageBufferHeap
    IImageStreamInfo::BufPlanes_t const& bufPlanes = pStreamInfo->getBufPlanes();
    size_t bufStridesInBytes[3] = {0};
    size_t bufBoundaryInBytes[3]= {0};
    for (size_t i = 0; i < bufPlanes.size(); i++) {
        bufStridesInBytes[i] = bufPlanes[i].rowStrideInBytes;
    }

    //one-time allocation
    android::sp<IIonImageBufferHeap> pImageBufferHeap = NULL;
    if ( eImgFmt_JPEG == pStreamInfo->getImgFormat() )
    {
        IImageBufferAllocator::ImgParam imgParam(
                pStreamInfo->getImgSize(),
                (*bufStridesInBytes),
                (*bufBoundaryInBytes));
        imgParam.imgFormat = eImgFmt_BLOB;
        MY_LOGD("eImgFmt_JPEG -> eImgFmt_BLOB");
        pImageBufferHeap = IIonImageBufferHeap::create(
                                pStreamInfo->getStreamName(),
                                imgParam,
                                IIonImageBufferHeap::AllocExtraParam(),
                                MFALSE
                            );
    }
    else
    {
        IImageBufferAllocator::ImgParam imgParam(
            pStreamInfo->getImgFormat(),
            pStreamInfo->getImgSize(),
            bufStridesInBytes, bufBoundaryInBytes,
            bufPlanes.size()
            );
        MY_LOGD("format:%x, size:(%d,%d), stride:%d, boundary:%d, planes:%d", pStreamInfo->getImgFormat(), pStreamInfo->getImgSize().w, pStreamInfo->getImgSize().h, bufStridesInBytes[0], bufBoundaryInBytes[0], bufPlanes.size());
        pImageBufferHeap = IIonImageBufferHeap::create(
                                pStreamInfo->getStreamName(),
                                imgParam,
                                IIonImageBufferHeap::AllocExtraParam(),
                                MFALSE
                            );
    }
    //
    // 2. set ImageBufferHeap into StreamBuffer structure
    android::sp<HalImageStreamBufferProvider>
    pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, pImageBufferHeap, this);
    rpStreamBuffer = pStreamBufferProvider;
    MY_LOGD("rpStreamBuffer = 0x%x, pImageBufferHeap = 0x%x", rpStreamBuffer.get(), pImageBufferHeap.get());
    if ( rpStreamBuffer == 0 || pImageBufferHeap == 0 ) {
        MY_LOGD("Set buffer as NULL by empty buffer container");
        return NO_MEMORY;
    }
    BUF_INFO tempJpgBufInfo;
    tempJpgBufInfo.iReqNum = iRequestNo;
    tempJpgBufInfo.halImageStreamBuffer = rpStreamBuffer;
    tempJpgBufInfo.imageBufferHeap = pImageBufferHeap;
    mvJpgBufInfo.push_back(tempJpgBufInfo);

    pImageBufferHeap->lockBuf(LOG_TAG, eBUFFER_USAGE_SW_MASK);
    MSize imgSize = pImageBufferHeap->getImgSize();
    MY_LOGD("Req(%d),StrBuf(%p),Heap(%p),VA(%p/%p/%p),BS(%d=%d+%d+%d),Id(%d/%d/%d),F(0x%08X),S(%dx%d),Str(%d,%d,%d)",
            iRequestNo,
            rpStreamBuffer.get(),
            pImageBufferHeap.get(),
            GET_IIMAGEBUFFERHEAP_VA(pImageBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_VA(pImageBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_VA(pImageBufferHeap,2),
            ( GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,0)+
              GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,1)+
              GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,2)
            ),
            GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_SIZE(pImageBufferHeap,2),
            GET_IIMAGEBUFFERHEAP_ID(pImageBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_ID(pImageBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_ID(pImageBufferHeap,2),
            pImageBufferHeap->getImgFormat(),
            imgSize.w,
            imgSize.h,
            GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pImageBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pImageBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(pImageBufferHeap,2)
            );
    pImageBufferHeap->unlockBuf(LOG_TAG);
    //
    return OK;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
JpgStreamBufHandlerImpl::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32  bBufStatus)

{
    Mutex::Autolock _l(mLock);
    // find if the add of rpStreamBuffer is found in mvJpgBufInfo
    MBOOL isFound = MFALSE;
    MUINT reqNum;
    android::sp<IIonImageBufferHeap>  jpgBufferHeap;
    vector<BUF_INFO>::iterator iterJpgBuf;
    for(iterJpgBuf = mvJpgBufInfo.begin(); iterJpgBuf != mvJpgBufInfo.end(); iterJpgBuf++)
    {
        if((*iterJpgBuf).halImageStreamBuffer == rpStreamBuffer)
        {
            isFound = MTRUE;
            reqNum = (*iterJpgBuf).iReqNum;
            jpgBufferHeap = (*iterJpgBuf).imageBufferHeap;
            mvJpgBufInfo.erase(iterJpgBuf);
            break;
        }
    }
    if(isFound != MTRUE)
    {
        MY_LOGE("BufAddr is not in queue (%p)",rpStreamBuffer.get());
        return BAD_VALUE;
    }
    jpgBufferHeap->lockBuf(LOG_TAG, eBUFFER_USAGE_SW_MASK);
    MSize imgSize = jpgBufferHeap->getImgSize();
    MY_LOGD("Req(%d),StrBuf(%p),Heap(%p),VA(%p/%p/%p),BS(%d=%d+%d+%d),Id(%d/%d/%d),F(0x%08X),S(%dx%d),Str(%d,%d,%d)",
            reqNum,
            rpStreamBuffer.get(),
            jpgBufferHeap.get(),
            GET_IIMAGEBUFFERHEAP_VA(jpgBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_VA(jpgBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_VA(jpgBufferHeap,2),
            ( GET_IIMAGEBUFFERHEAP_SIZE(jpgBufferHeap,0)+
              GET_IIMAGEBUFFERHEAP_SIZE(jpgBufferHeap,1)+
              GET_IIMAGEBUFFERHEAP_SIZE(jpgBufferHeap,2)
            ),
            GET_IIMAGEBUFFERHEAP_SIZE(jpgBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_SIZE(jpgBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_SIZE(jpgBufferHeap,2),
            GET_IIMAGEBUFFERHEAP_ID(jpgBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_ID(jpgBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_ID(jpgBufferHeap,2),
            jpgBufferHeap->getImgFormat(),
            imgSize.w,
            imgSize.h,
            GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(jpgBufferHeap,0),
            GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(jpgBufferHeap,1),
            GET_IIMAGEBUFFERHEAP_STRIDE_IN_BYTE(jpgBufferHeap,2)
            );
    jpgBufferHeap->unlockBuf(LOG_TAG);
    handleDataCallback(0,0,0,jpgBufferHeap);
    return OK;
}
/*******************************************************************************
*
********************************************************************************/

MVOID
JpgStreamBufHandlerImpl::
setCallbacks(PipeDataCallback_t data_cb, MVOID* user)
{
    Mutex::Autolock _l(mLock);
    mpUser    = user;
    mDataCb   = data_cb;
}
/*******************************************************************************
*
********************************************************************************/
MVOID
JpgStreamBufHandlerImpl::
handleDataCallback(MUINT32 const msg, MUINTPTR const ext1, MUINTPTR const ext2, android::sp<IIonImageBufferHeap> const pImgBuf)
{
    if( mDataCb == NULL )
    {
        MY_LOGE("dataCallback is not set");
        return;
    }

    PipeDataInfo datainfo(msg, ext1, ext2, pImgBuf);
    mDataCb(mpUser, datainfo);
}


}; // namespace NSCamStreamBufProvider

