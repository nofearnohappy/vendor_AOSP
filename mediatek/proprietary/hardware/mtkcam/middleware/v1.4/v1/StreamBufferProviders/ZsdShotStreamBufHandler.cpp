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
#define LOG_TAG "MtkCam/ZSDSHOTBHdl"

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
#include <v1/StreamBufferProviders/BufMgr.h>
using namespace NSMtkBufMgr;
#include <v3/utils/streambuf/IStreamBufferProvider.h>
#include <ImgBufProvidersManager.h>
using namespace NSCam::v3;
using namespace NSCam::v3::Utils;
using namespace android;
#include <v1/StreamBufferProviders/HalImageStreamBufferProvider.h>
#include <v1/StreamBufferProviders/ZsdShotStreamBufHandler.h>
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

class ZsdShotStreamBufHandlerImpl : public ZsdShotStreamBufHandler
{
    public:
        ZsdShotStreamBufHandlerImpl(MUINT32 openId, const char* userName);
        ~ZsdShotStreamBufHandlerImpl();

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
    virtual MBOOL   init(BufMgr* bufMgr);
    virtual MBOOL   uninit();
    virtual MBOOL   prepareBufAndMeta();
    virtual MBOOL   getMetadata(IMetadata* metadata);

    //
    protected:
        //
        MUINT32 getOpenId() {return mOpenId;}
        const char* getName() const {return msName;}
        //
    private:
        MUINT32                             mOpenId;
        mutable Mutex                       mLock;
        const char* const                   msName;
        BufMgr*                             mpBufMgr;
        IMetadata                           mStoredMetadata;
        sp<IImageBufferHeap>                mStoredImageBufferHeap;
        sp<HalImageStreamBuffer>            mLastRequestFrom;

};


/*******************************************************************************
*
********************************************************************************/
ZsdShotStreamBufHandler*
ZsdShotStreamBufHandler::
createInstance(
    MUINT32     openId,
    const char* userName)
{
    return new ZsdShotStreamBufHandlerImpl(openId, userName);
}


/*******************************************************************************
*
********************************************************************************/
MVOID
ZsdShotStreamBufHandler::
destroyInstance()
{

}


/*******************************************************************************
*
********************************************************************************/
ZsdShotStreamBufHandler::
ZsdShotStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
ZsdShotStreamBufHandler::
~ZsdShotStreamBufHandler()
{
}


/*******************************************************************************
*
********************************************************************************/
ZsdShotStreamBufHandlerImpl::
ZsdShotStreamBufHandlerImpl(
    MUINT32     openId,
    const char* userName)
    : ZsdShotStreamBufHandler()
    , mOpenId(openId)
    , msName(userName)
    , mStoredImageBufferHeap(NULL)
    , mLastRequestFrom(NULL)
{
    FUNC_NAME;
}


/*******************************************************************************
*
********************************************************************************/
ZsdShotStreamBufHandlerImpl::
~ZsdShotStreamBufHandlerImpl()
{
    FUNC_NAME;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
ZsdShotStreamBufHandlerImpl::
init(BufMgr* bufMgr)
{
    FUNC_START;
    //
    mpBufMgr = bufMgr;
    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
ZsdShotStreamBufHandlerImpl::
uninit()
{
    FUNC_START;
    //

    //
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
ZsdShotStreamBufHandlerImpl::
prepareBufAndMeta()
{
    FUNC_START;
    // 1. get buf (mStoredImageBufferHeap) and meta (mStoredMetadata) from BufMgr.deque()
    mpBufMgr->dequeBuf(mStoredImageBufferHeap,&mStoredMetadata);
    FUNC_END;
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
ZsdShotStreamBufHandlerImpl::
getMetadata(IMetadata* metadata)
{
    FUNC_START;

    (*metadata) = mStoredMetadata;

    FUNC_END;
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
ZsdShotStreamBufHandlerImpl::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> &rpStreamBuffer)

{
    if(mStoredImageBufferHeap == NULL)
    {
        MY_LOGW("Image buffer is NULL");
        return BAD_VALUE;
    }
    android::sp<HalImageStreamBufferProvider>
    pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, mStoredImageBufferHeap, this);
    rpStreamBuffer = pStreamBufferProvider;
    if ( rpStreamBuffer == 0)
    {
        MY_LOGD("Set buffer as NULL by empty buffer container");
        return NO_MEMORY;
    }
    mLastRequestFrom = rpStreamBuffer;
    //
    return OK;
}


/*******************************************************************************
*
********************************************************************************/
MERROR
ZsdShotStreamBufHandlerImpl::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32  bBufStatus)
{
    if(mLastRequestFrom != rpStreamBuffer)
    {
        MY_LOGW("LastRequestFrom != rpStreamBuffer");
        return BAD_VALUE;
    }
    if(mStoredImageBufferHeap == NULL)
    {
        MY_LOGW("mStoredImageBufferHeap == NULL");
        return BAD_VALUE;
    }
    // 1. enque mStoredImageBufferHeap to BufMgr
    mpBufMgr->enqueBuf(mStoredImageBufferHeap);
    mLastRequestFrom = NULL;
    mStoredImageBufferHeap = NULL;
    return OK;
}



}; // namespace NSCamStreamBufProvider

