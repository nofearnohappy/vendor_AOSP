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
#warning "FIX me mNumberOfWaitingMeta"
CStreamBufferProviderImp::
CStreamBufferProviderImp()
    : mNumberOfWaitingMeta(1)
{}

/******************************************************************************
 *
 ******************************************************************************/
void
CStreamBufferProviderImp::
onResultReceived(
    MUINT32         const requestNo,
    StreamId_T      const streamId,
    MBOOL           const errorResult,
    IMetadata*      const result
)
{
    // p1 result
    if ( errorResult ) {
        if ( mpRule != 0 ) mpRule->errorResult(requestNo);
        clearSet( requestNo, *result );
    } else {
        queueToSet( requestNo, *result );
    }
}

/******************************************************************************
 *
 ******************************************************************************/
android::String8
CStreamBufferProviderImp::
getUserName()
{
    return String8::format("Cathy");
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
CStreamBufferProviderImp::
dequeStreamBuffer(
    MUINT32 const                       iRequestNo,
    android::sp<IImageStreamInfo> const rpStreamInfo,
    android::sp<HalImageStreamBuffer>&  rpStreamBuffer
)
{
    // get from list
    sp<IImageBufferHeap> pHeap;
    {
        Mutex::Autolock _l(mQListLock);
        //
        if ( mQueryList.empty() ) {
            MY_LOGW("User does get metadata first.");

            IMetadata ResultMeta;
            if ( mpRule->getResultSet(ResultMeta, pHeap) != OK ) {
                MY_LOGE("Cannot select result from user's rule.");
                return UNKNOWN_ERROR;
            }
        } else {
            List< sp<IImageBufferHeap> >::iterator it = mQueryList.begin();
            pHeap = *it;
            mQueryList.erase(it);
        }

        rpStreamBuffer = new HalImageStreamBufferProvider(mpImageStreamInfo, pHeap, static_cast< sp<IStreamBufferProvider> >(this));
    }

    {
        Mutex::Autolock _l(mBMapLock);
        mBufferMap.add(rpStreamBuffer.get(), Buffer_T{iRequestNo, pHeap});
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
CStreamBufferProviderImp::
enqueStreamBuffer(
    android::sp<IImageStreamInfo> const rpStreamInfo,
    android::sp<HalImageStreamBuffer>   rpStreamBuffer,
    MUINT32                             bBufStatus
)
{
    // back to pool
    if ( mpBufferPool == 0 ) {
        MY_LOGE("Buffer pool does not exist.");
        return UNKNOWN_ERROR;
    }
    //
    if ( rpStreamBuffer == 0 ) {
        MY_LOGE("return NULL buffer");
        return UNKNOWN_ERROR;
    }

    //
    Buffer_T buf;
    {
        Mutex::Autolock _l(mBMapLock);
        buf = mBufferMap.editValueFor( rpStreamBuffer.get() );
        mBufferMap.removeItem( rpStreamBuffer.get() );
    }

    // handle callback before return to pool
    sp< IImageCallback > cb = mpImageCallback.promote();
    if (   cb != 0
        && !(bBufStatus & STREAM_BUFFER_STATUS::ERROR)
    ) {
        sp<IImageBuffer> pBuf = buf.heap->createImageBuffer();
        cb->onResultReceived(buf.requestNo, pBuf);
    }

#warning "FIXME"
    mpBufferPool->releaseToPool(LOG_TAG, buf.heap, 0, true);

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
CStreamBufferProviderImp::
returnBuffer(
    MINT32  rRequestNo
)
{
    // user release buffer from selector
    Mutex::Autolock _l(mSKBMapLock);

    sp<IImageBufferHeap> pHeap = mSelectorKeepBufferMap.editValueFor( rRequestNo );
    mSelectorKeepBufferMap.removeItem(rRequestNo);

#warning "FIXME"
    mpBufferPool->releaseToPool(LOG_TAG, pHeap, 0, true);

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
CStreamBufferProviderImp::
queueToSet(
    MUINT32                       rRequestNo,
    android::sp<IImageBufferHeap> rpHeap
)
{

    Mutex::Autolock _l(mResultSetLock);

    ssize_t const index = mResultSetMap.indexOfKey(rRequestNo);
    if ( index < 0 ) {
        // rRequestNo does not exist
        Vector<IMetadata*> v;
        mResultSetMap.add(rRequestNo, ResultSet_T{rRequestNo, rpHeap, v});
    } else {
        // in previous result
        ResultSet_T* result = &mResultSetMap.editValueAt(index);

        // check returnable
        if ( result->resultMeta.size() >= mNumberOfWaitingMeta ) {
            // check if need to send to selector
            Mutex::Autolock _l(mSKBMapLock);

            if (   mpRule != 0
                && mpRule->selectResult( rRequestNo, *result->resultMeta[0], rpHeap) ) {
                // selector keep the buffer
                mSelectorKeepBufferMap.add(rRequestNo, rpHeap);
                mResultSetMap.removeItem(rRequestNo);
            } else {
                // selector does not keep the buffer. return to pool
                for ( size_t i = 0; i < result->resultMeta.size(); ++i) {
                    MINT64 timestamp;
                    if ( !tryGetMetadata<MINT64>(result->resultMeta[i], MTK_SENSOR_TIMESTAMP, timestamp) ) {
                        mpBufferPool->releaseToPool(LOG_TAG, rpHeap, timestamp, false);
                        mResultSetMap.removeItem(rRequestNo);
                        return OK;
                    }
                }
            }
        }

        result->heap = rpHeap;
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
CStreamBufferProviderImp::
queueToSet(
    MUINT32                       rRequestNo,
    IMetadata                     rResultMeta
)
{
    Mutex::Autolock _l(mResultSetLock);

    ssize_t const index = mResultSetMap.indexOfKey(rRequestNo);
    if ( index < 0 ) {
        // rRequestNo does not exist
        Vector<IMetadata*> v;
        v.push_back(&rResultMeta);
        mResultSetMap.add(rRequestNo, ResultSet_T{rRequestNo, NULL, v});
    } else {
        // in previous result
        ResultSet_T* result = &mResultSetMap.editValueAt(index);

        // check returnable
        if (    result->resultMeta.size() + 1 >= mNumberOfWaitingMeta
             && result->heap != NULL) {
            // check if need to send to selector
            Mutex::Autolock _l(mSKBMapLock);

            if (   mpRule != 0
                && mpRule->selectResult( rRequestNo, *result->resultMeta[0], result->heap) ) {
                // selector keep the buffer
                mSelectorKeepBufferMap.add(rRequestNo, result->heap);
                mResultSetMap.removeItem(rRequestNo);
            } else {
                // selector does not keep the buffer. return to pool
                for ( size_t i = 0; i < result->resultMeta.size(); ++i) {
                    MINT64 timestamp;
                    if ( !tryGetMetadata<MINT64>(result->resultMeta[i], MTK_SENSOR_TIMESTAMP, timestamp) ) {
                        mpBufferPool->releaseToPool(LOG_TAG, result->heap, timestamp, false);
                        mResultSetMap.removeItem(rRequestNo);
                        return OK;
                    }
                }
            }
        }

        result->resultMeta.push_back(&rResultMeta);
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
CStreamBufferProviderImp::
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
MERROR
CStreamBufferProviderImp::
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
MERROR
CStreamBufferProviderImp::
getResultBufferFromProvider(
    MUINT32              rRequestNo,
    sp<IImageBufferHeap> rpHep,
    MUINT32              rbBufStatus
)
{
    // result from provider
    if ( rbBufStatus & STREAM_BUFFER_STATUS::ERROR ) {
        if ( mpRule != 0) mpRule->errorResult(rRequestNo);
        clearSet( rRequestNo, rpHep );
        //  remove meta from set queue if exist
    } else {
        // send to set queue and check if need to send to user
        queueToSet( rRequestNo, rpHep );
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
CStreamBufferProviderImp::
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
CStreamBufferProviderImp::
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
CStreamBufferProviderImp::
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
CStreamBufferProviderImp::
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
CStreamBufferProviderImp::
setSelector(
    android::sp< ISelector > pRule
)
{
    Mutex::Autolock _l(mRuleLock);

    if ( mpRule != 0 ) {
        mpRule->flush();
        {
            Mutex::Autolock _l(mSKBMapLock);
            if ( mSelectorKeepBufferMap.size() > 0 ) {
                for ( size_t i = 0; i < mSelectorKeepBufferMap.size(); ++i ) {
                    MY_LOGE("[%s] buffer %p not return to pool.", "cathy"/*mpRule->name()*/, mSelectorKeepBufferMap[i].get() );
                }
                mSelectorKeepBufferMap.clear();
            }
        }
    }

    mpRule = pRule;
    mpRule->setPool(this);

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
CStreamBufferProviderImp::
isComsumer()
{
    return true;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
CStreamBufferProviderImp::
getMetadata(
    IMetadata&  rResultMeta
)
{
    sp<IImageBufferHeap>    pHeap;
    {
        // query from user
        Mutex::Autolock _l(mRuleLock);

        if ( mpRule->getResultSet(rResultMeta, pHeap) != OK ) {
            MY_LOGE("Cannot select result from user's rule.");
            return UNKNOWN_ERROR;
        }
    }

    {
        // push heap to list
        Mutex::Autolock _l(mQListLock);

        mQueryList.push_back(pHeap);
    }

    return OK;
}