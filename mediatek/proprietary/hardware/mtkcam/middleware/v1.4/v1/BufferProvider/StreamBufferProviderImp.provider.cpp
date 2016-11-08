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

#define LOG_TAG "MtkCam/StreamBufferProviderImp"
//
#include "MyUtils.h"
//
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>
//
#include "StreamBufferProviderImp.h"

using namespace android;
using namespace NSCam;
using namespace NSCam::v1;

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
template <typename T>
inline MBOOL
tryGetMetadata(
    IMetadata* pMetadata,
    MUINT32 const tag,
    T & rVal
)
{
    if( pMetadata == NULL ) {
        MY_LOGW("pMetadata == NULL");
        return MFALSE;
    }

    IMetadata::IEntry entry = pMetadata->entryFor(tag);
    if( !entry.isEmpty() ) {
        rVal = entry.itemAt(0, Type2Type<T>());
        return MTRUE;
    }
    return MFALSE;
}

/******************************************************************************
 *
 ******************************************************************************/
PStreamBufferProviderImp::
PStreamBufferProviderImp()
    : mNumberOfWaitingMeta(1)
{}

/******************************************************************************
 *
 ******************************************************************************/
void
PStreamBufferProviderImp::
onResultReceived(
    MUINT32         const requestNo,
    StreamId_T      const streamId,
    MBOOL           const errorResult,
    IMetadata*      const result
)
{
    MY_LOGD("[request:%d][streamId:%d] error:%d meta:%d", requestNo, streamId, errorResult, result->count());
    // provider only receive timestamp
    if ( errorResult ) {
        // clear queue set
        clearSet( requestNo, *result );
    } else {
        queueToSet( requestNo, *result );
    }
}

/******************************************************************************
 *
 ******************************************************************************/
android::String8
PStreamBufferProviderImp::
getUserName()
{
    return String8::format("Cathy");
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
PStreamBufferProviderImp::
dequeStreamBuffer(
    MUINT32 const                       iRequestNo,
    android::sp<IImageStreamInfo> const rpStreamInfo,
    android::sp<HalImageStreamBuffer>&  rpStreamBuffer
)
{
    FUNC_START;

    sp<IImageBufferHeap> pHeap;
    {
        Mutex::Autolock _l(mPoolLock);

        if ( mpBufferPool == 0 ) {
            MY_LOGE("Buffer pool does not exist.");
            return UNKNOWN_ERROR;
        }
        //
        if ( mpBufferPool->acquireFromPool(LOG_TAG, pHeap) != OK ) {
            MY_LOGE("Cannot acquire from pool. Something wrong... SOS");
            return NO_MEMORY;
        }
    }

    rpStreamBuffer = new HalImageStreamBufferProvider(mpImageStreamInfo, pHeap, static_cast< sp<IStreamBufferProvider> >(this));

    if ( rpStreamBuffer == 0) {
        MY_LOGE("Cannot new HalImageStreamBuffer. Something wrong...");
        return NO_MEMORY;
    }

    {
        Mutex::Autolock _l(mBufferLock);
        mBufferMap.add(rpStreamBuffer.get(), Buffer_T{iRequestNo, pHeap});
    }

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
PStreamBufferProviderImp::
setConsumer(
    sp< CStreamBufferProviderImp > pConsumer
)
{
    FUNC_START;

    mpConsumer = pConsumer;

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
PStreamBufferProviderImp::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const rpStreamInfo,
    android::sp<HalImageStreamBuffer>   rpStreamBuffer,
    MUINT32                             bBufStatus
)
{
    FUNC_START;

    Mutex::Autolock _l(mBufferLock);

    if ( mpBufferPool == 0 ) {
        MY_LOGE("Buffer pool does not exist.");
        return UNKNOWN_ERROR;
    }
    //
    if ( rpStreamBuffer == 0 ) {
        MY_LOGE("return NULL buffer");
        return UNKNOWN_ERROR;
    }
    // handle callback before return to pool
    //
    Buffer_T buf = mBufferMap.editValueFor( rpStreamBuffer.get() );
    sp< IImageCallback > cb = mpImageCallback.promote();
    if (   cb != 0
        && !(bBufStatus & STREAM_BUFFER_STATUS::ERROR)
    ) {
        sp<IImageBuffer> pBuf = buf.heap->createImageBuffer();
        cb->onResultReceived(buf.requestNo, pBuf);
    }

    mBufferMap.removeItem(rpStreamBuffer.get());

    MY_LOGD("request:%d bBufStatus:%d.", buf.requestNo, bBufStatus);

    // if consumer exist, send to consumer
    // else collect & return to pool
    sp< CStreamBufferProviderImp > pConsumer = mpConsumer.promote();
    if ( pConsumer != 0 ) {
        // send all buffer to consumer including error buffer
        pConsumer->getResultBufferFromProvider(buf.requestNo, buf.heap, bBufStatus);
    } else {
        // send to set queue & release to pool
        if ( bBufStatus & STREAM_BUFFER_STATUS::ERROR ) {
            clearSet( buf.requestNo, buf.heap );
        } else {
            queueToSet( buf.requestNo, buf.heap );
        }
    }

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
queueToSet(
    MUINT32 const        iRequestNo,
    sp<IImageBufferHeap> rpHeap
)
{
    MY_LOGD("[%d] rpHeap:%p", iRequestNo, rpHeap.get());
    Mutex::Autolock _l(mResultSetLock);

    ssize_t const index = mResultSetMap.indexOfKey(iRequestNo);
    if ( index < 0 ) {
        // iRequestNo does not exist
        Vector<IMetadata*> v;
        mResultSetMap.add(iRequestNo, ResultSet_T{iRequestNo, rpHeap, v});
    } else {
        // in previous result
        ResultSet_T* result = &mResultSetMap.editValueAt(index);

        // check returnable
        if ( result->resultMeta.size() >= mNumberOfWaitingMeta ) {
            MINT64 timestamp = 0;
#warning "FIXME"
#if 1
            Mutex::Autolock _l(mPoolLock);
            mpBufferPool->releaseToPool(LOG_TAG, rpHeap, timestamp, false);
            return OK;
#else
            if ( !tryGetMetadata<MINT64>(result->resultMeta[0], MTK_SENSOR_TIMESTAMP, timestamp) ) {
                {
                    Mutex::Autolock _l(mPoolLock);
                    mpBufferPool->releaseToPool(LOG_TAG, rpHeap, timestamp, false);
                }
                return OK;
            } else {
                MY_LOGE("No timestamp information in result metadata.");
            }
#endif
        }
        result->heap = rpHeap;
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
queueToSet(
    MUINT32 const        iRequestNo,
    IMetadata            rResultMeta
)
{
    MY_LOGD("[%d] rResultMeta:%p", iRequestNo, &rResultMeta);
    // producer will only collect timestamp
    Mutex::Autolock _l(mResultSetLock);

    ssize_t const index = mResultSetMap.indexOfKey(iRequestNo);
    if ( index < 0 ) {
        // iRequestNo does not exist
        Vector<IMetadata*> v;
        v.push_back(&rResultMeta);
        mResultSetMap.add(iRequestNo, ResultSet_T{iRequestNo, NULL, v});
    } else {
        // in previous result
        ResultSet_T* result = &mResultSetMap.editValueAt(index);

        // check returnable
        if (    result->resultMeta.size() + 1 >= mNumberOfWaitingMeta
             && result->heap != NULL) {
            MINT64 timestamp;
            if ( !tryGetMetadata<MINT64>(&rResultMeta, MTK_SENSOR_TIMESTAMP, timestamp) ) {
                {
                    Mutex::Autolock _l(mPoolLock);
                    mpBufferPool->releaseToPool(LOG_TAG, result->heap, timestamp, false);
                }
                mResultSetMap.removeItem(iRequestNo);
                return OK;
            } else {
                MY_LOGE("No timestamp information in result metadat.");
            }
        }

        result->resultMeta.push_back(&rResultMeta);
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
clearSet(
    MUINT32 const        iRequestNo,
    sp<IImageBufferHeap> rpHeap
)
{
    ssize_t const index = mResultSetMap.indexOfKey(iRequestNo);
    if ( index >= 0 ) {
        mResultSetMap.removeItem(iRequestNo);
    }

    {
        Mutex::Autolock _l(mPoolLock);
        mpBufferPool->releaseToPool(LOG_TAG, rpHeap, 0, true);
    }
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
clearSet(
    MUINT32 const        iRequestNo,
    IMetadata            rResultMeta
)
{
    ssize_t const index = mResultSetMap.indexOfKey(iRequestNo);
    if ( index >= 0 ) {
        mResultSetMap.removeItem(iRequestNo);
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
setImageStreamInfo(
    android::sp<IImageStreamInfo> pStreamInfo
)
{
    mpImageStreamInfo = pStreamInfo;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
queryImageStreamInfo(
    android::sp<IImageStreamInfo>& pStreamInfo
)
{
    pStreamInfo = mpImageStreamInfo;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
setImageCallback(
    android::wp< IImageCallback > cb
)
{
    mpImageCallback = cb;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
setBufferPool(
    android::sp< IBufferPool >    pBufProvider
)
{
    Mutex::Autolock _l(mPoolLock);
    mpBufferPool = pBufProvider;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
setSelector(
    android::sp< ISelector > pRule
)
{
    MY_LOGE("Producer does not support.");
    return UNKNOWN_ERROR;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
PStreamBufferProviderImp::
isComsumer()
{
    return false;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
PStreamBufferProviderImp::
getMetadata(
    IMetadata&  rResultMeta
)
{
    MY_LOGE("Producer does not support.");
    return UNKNOWN_ERROR;
}