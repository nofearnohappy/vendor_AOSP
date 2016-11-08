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

#define LOG_TAG "MtkCam/StreamBufferProvider"
//
#include <Log.h>
#include <imagebuf/IIonImageBufferHeap.h>
#include <v3/utils/streambuf/StreamBufferProvider.h>

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



/******************************************************************************
 *
 ******************************************************************************/
namespace NSCam {
namespace v3 {
namespace Utils {


#if 0
/******************************************************************************
 *
 ******************************************************************************/
StreamBufferProvider::
StreamBufferProvider()
    : mLock()
{}

/******************************************************************************
 *
 ******************************************************************************/
StreamBufferProvider::
~StreamBufferProvider()
{}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
StreamBufferProvider::
dequeStreamBuffer(
    MUINT32 const iRequestNo,
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> &rpStreamBuffer
)
{
//    android::Mutex::Autolock _l(mLock);
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
    IImageBufferAllocator::ImgParam const imgParam(
        pStreamInfo->getImgFormat(),
        pStreamInfo->getImgSize(),
        bufStridesInBytes, bufBoundaryInBytes,
        bufPlanes.size()
    );
    // PortBufInfo_v1 portBufInfo = PortBufInfo_v1(...) @ DefaultBufHandler
    //android::sp<ImageBufferHeap> pHeap = ImageBufferHeap::create(...)
    //
    //workaround: one-time allocation
    android::sp<IIonImageBufferHeap> pImageBufferHeap =
    IIonImageBufferHeap::create(
        pStreamInfo->getStreamName(),
        imgParam,
        IIonImageBufferHeap::AllocExtraParam(),
        MFALSE
    );
    //workaround: one-time allocation

    // 2. set ImageBufferHeap into StreamBuffer structure
    android::sp<HalImageStreamBufferProvider>
    pStreamBufferProvider = new HalImageStreamBufferProvider(pStreamInfo, pImageBufferHeap, this);
    rpStreamBuffer = pStreamBufferProvider;
    if ( rpStreamBuffer == 0 || pImageBufferHeap == 0 ) {
        MY_LOGD("Set buffer as NULL by empty buffer container");
        return NO_MEMORY;
    }
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
StreamBufferProvider::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const pStreamInfo,
    android::sp<HalImageStreamBuffer> rpStreamBuffer,
    MUINT32 bBufStatus
)
{
//    android::Mutex::Autolock _l(mLock);
    //
    if ( rpStreamBuffer==0 )
        MY_LOGD("Null buffer release to provider: %d", bBufStatus);
    else {
        // TODO:
        // implementation should keep the mapping between StreamBuffer & Heap.
        //
        MY_LOGD("Non-null buffer release back to provider: %d", bBufStatus);
    }

    return OK;

}
#endif


/******************************************************************************
 *
 ******************************************************************************/
HalImageStreamBufferProvider::
HalImageStreamBufferProvider(
    android::sp<IImageStreamInfo> pStreamInfo,
    android::sp<IImageBufferHeap> pImageBufferHeap,
    android::sp<IStreamBufferProvider> pProvider
)
    : HalImageStreamBuffer(pStreamInfo, NULL, pImageBufferHeap)
    , mpProvider(pProvider)
{
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
HalImageStreamBufferProvider::
releaseBuffer()
{
    //
    if ( mpProvider.get() ) {
        //  Reset buffer before returning to poolProvider
        resetBuffer();
        //
        //  Release to Provider
        mpProvider->enqueStreamBuffer(mStreamInfo, this, this->mBufStatus);
    }
}


/******************************************************************************
 *
 ******************************************************************************/
};  //namespace Utils
};  //namespace v3
};  //namespace NSCam

