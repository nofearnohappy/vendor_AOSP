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

#define LOG_TAG "MtkCam/BufferPoolImp"
//
#include "MyUtils.h"
//
#include <utils/RWLock.h>
#include <utils/Thread.h>
//
#include <sys/prctl.h>
#include <sys/resource.h>
//
#include <utils/include/ImageBufferHeap.h>
#include <v3/utils/streaminfo/MetaStreamInfo.h>
#include <v3/utils/streaminfo/ImageStreamInfo.h>
#include <v3/utils/streambuf/StreamBuffers.h>
#include <v3/stream/IStreamInfo.h>
//
#include <v1/BufferProvider/StreamBufferProviderFactory.h>
#include <utils/include/Format.h>

using namespace android;
using namespace android::MtkCamUtils;
using namespace NSCam;
using namespace NSCam::v1;
using namespace NSCam::Utils;

#include <ImgBufProvidersManager.h>

using namespace NSCam::v3::Utils;
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
#define BUFFERPOOL_NAME       ("Cam@v1BufferPool")
#define BUFFERPOOL_POLICY     (SCHED_OTHER)
#define BUFFERPOOL_PRIORITY   (0)

/******************************************************************************
 *
 ******************************************************************************/
class BufferPoolImp
    : public IBufferPool
    , public Thread
{
public:
                                        BufferPoolImp();

                                        ~BufferPoolImp() {};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IBufferPool Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:   //// producer's operations.

    virtual MERROR                      acquireFromPool(
                                            char const*                    szCallerName,
                                            sp<IImageBufferHeap>& rpBuffer
                                        );

    virtual MERROR                      releaseToPool(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap> pBuffer,
                                            MUINT64              rTimeStamp,
                                            bool                 rErrorResult
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:    //// debug

    virtual char const*                 poolName() const;

    virtual MVOID                       dumpPool() const;

public:    //// set stream info & buffer source

    virtual MERROR                      setImageStreamInfo(
                                            char const* szCallerName,
                                            android::sp<IImageStreamInfo> pStreamInfo
                                        );

    virtual MERROR                      allocateBuffer(
                                            char const* szCallerName,
                                            size_t maxNumberOfBuffers,
                                            size_t minNumberOfInitialCommittedBuffers
                                        );

    /*virtual MERROR                      setCamClient(
                                            char const* szCallerName,
                                            android::sp<IImgBufProvider> pSource
                                        );*/

    virtual MERROR                      setCamClient(
                                            char const*                         szCallerName,
                                            android::sp<ImgBufProvidersManager> pSource,
                                            MINT32                              rMode
                                        );

    virtual MERROR                      setUsersBuffer(
                                            char const* szCallerName,
                                            List<android::sp<IImageBuffer> > pSource
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:   //// operations.

    virtual MVOID                       uninitPool(
                                            char const* szCallerName
                                        );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    enum poolMode_T {
        POOLMODE_ALLOCATE,
        POOLMODE_USER_PROVIDE,
        POOLMODE_CAMCLIENT
    };

protected:   //// acquire buffer
    MERROR                              getBufferFromPool(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap>& rpBuffer
                                        );

    MERROR                              getBufferFromBufferQueue(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap>& rpBuffer
                                        );

    MERROR                              getBufferFromAvailableList(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap>& rpBuffer
                                        );

    MERROR                              getBufferFromClient(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap>& rpBuffer
                                        );

protected:
    MERROR                              returnBufferToPool(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap> rpBuffer
                                        );

    MERROR                              returnBufferToClient(
                                            char const* szCallerName,
                                            sp<IImageBufferHeap> rpBuffer,
                                            MUINT64              rTimeStamp,
                                            bool                 rErrorResult
                                        );

protected:
    MERROR                              do_construct(sp<IImageBufferHeap>&);

    MERROR                              tryGetClient();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  RefBase Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    void                                onLastStrongRef(const void* /*id*/);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Thread Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    // Ask this object's thread to exit. This function is asynchronous, when the
    // function returns the thread might still be running. Of course, this
    // function can be called from a different thread.
    virtual void        requestExit();

    // Good place to do one-time initializations
    virtual status_t    readyToRun();

private:
    // Derived class must implement threadLoop(). The thread starts its life
    // here. There are two ways of using the Thread object:
    // 1) loop: if threadLoop() returns true, it will be called again if
    //          requestExit() wasn't called.
    // 2) once: if threadLoop() returns false, the thread will exit upon return.
    virtual bool        threadLoop();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    MINT32                              mLogLevel;

protected:
    poolMode_T                          mPoolMode;
    sp<IImgBufProvider>                 mpClient;
    sp<ImgBufProvidersManager>          mpSourceManager;
    MINT32                              mClientMode;
    sp<IImageStreamInfo>                mpStreamInfo;

protected:
    MINT32                              mResultQueueDepth;
    List< sp<IImageBufferHeap> >        mAvailableBuf;
    List< sp<IImageBufferHeap> >        mInUseBuf;
    KeyedVector<IImageBufferHeap*, ImgBufQueNode >
                                        mClientBufferMap;
    //List< Condition* >                  mWaitingList;

protected:
    MINT32                              mMaxBuffer;
    MINT32                              mMinBuffer;
};


/******************************************************************************
 *
 ******************************************************************************/
sp<IBufferPool>
IBufferPool::
createInstance()
{
    return new BufferPoolImp();
}

/******************************************************************************
 *
 ******************************************************************************/
BufferPoolImp::
BufferPoolImp()
    : mLogLevel(1)
{}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
acquireFromPool(
    char const*           szCallerName,
    sp<IImageBufferHeap>& rpBuffer
)
{
    MY_LOGD_IF( mLogLevel >= 1, "%s", szCallerName);

    //Mutex::Autolock _l(mLock);
    switch( mPoolMode ) {
        case POOLMODE_ALLOCATE:
        case POOLMODE_USER_PROVIDE:
            return getBufferFromPool(
                      szCallerName,
                      rpBuffer
                   );
        break;
        /*case POOLMODE_USER_PROVIDE:
            return getBufferFromAvailableList(
                      szCallerName,
                      rpBuffer
                   );
        break;*/
        case POOLMODE_CAMCLIENT:
            return getBufferFromClient(
                      szCallerName,
                      rpBuffer
                   );
        break;
        default:
            MY_LOGE("Not support pool type %d", mPoolMode);
            return BAD_VALUE;
        break;
    };

}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
tryGetClient()
{
#warning "WHP TBD"
    switch(mClientMode)
    {
        case IImgBufProvider::eID_DISPLAY: {
            MY_LOGD("Try get display client");
            mpClient = mpSourceManager->getDisplayPvdr();
            break;
        }
        case IImgBufProvider::eID_REC_CB: {
            mpClient = mpSourceManager->getRecCBPvdr();
            break;
        }
        default: {
            MY_LOGE("Unsupport client", mClientMode);
            return UNKNOWN_ERROR;
        }
    }

    if ( mpClient != 0 ) {
        IImageStreamInfo::BufPlanes_t bufPlanes;
#define addBufPlane(planes, height, stride)                                      \
        do{                                                                      \
            size_t _height = (size_t)(height);                                   \
            size_t _stride = (size_t)(stride);                                   \
            IImageStreamInfo::BufPlane bufPlane= { _height * _stride, _stride }; \
            planes.push_back(bufPlane);                                          \
        }while(0)
#if 0
        ImgBufQueNode rNode;
        if ( mpClient->queryProvider(rNode) ) {
            MINT  imgFormat = Format::queryImageFormat(rNode.getImgBuf()->getImgFormat().string());
            MSize imgSize = MSize( rNode.getImgBuf()->getImgWidth(), rNode.getImgBuf()->getImgHeight() );
#warning "FIXME"
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

            sp<IImageStreamInfo>
                pStreamInfo = new ImageStreamInfo(
                        mpStreamInfo->getStreamName(),
                        mpStreamInfo->getStreamId(),
                        mpStreamInfo->getStreamType(),
                        mpStreamInfo->getMaxBufNum(), mpStreamInfo->getMinInitBufNum(),
                        mpStreamInfo->getUsageForAllocator(),
                        imgFormat, imgSize,
                        bufPlanes
                    );
            mpStreamInfo = pStreamInfo;
            MY_LOGD("CamClient stream info: size(%d,%d) format:0x%x(%s)",
                rNode.getImgBuf()->getImgWidth(), rNode.getImgBuf()->getImgHeight(),
                Format::queryImageFormat(rNode.getImgBuf()->getImgFormat().string()), rNode.getImgBuf()->getImgFormat().string()
            );
        }
#endif
        MY_LOGD("mpClient:%p", mpClient.get());
    }
#undef  addBufPlane
    if ( mpClient == 0 ) return UNKNOWN_ERROR;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
getBufferFromClient(
    char const*           szCallerName,
    sp<IImageBufferHeap>& rpBuffer
)
{
    FUNC_START;

    if ( mpClient == 0 ) {
        MY_LOGW("No CamClient.");
        if ( OK != tryGetClient() ) return UNKNOWN_ERROR;
    }

    ImgBufQueNode node;
    if( ! mpClient->dequeProvider(node) ) {
        MY_LOGE("Cannot dequeProvider.");
        return UNKNOWN_ERROR;
    }
#define GET_IIMGBUF_IMG_STRIDE_IN_BYTE(pBuf, plane) (Format::queryPlaneCount(Format::queryImageFormat(pBuf->getImgFormat().string())) >= (plane+1)) ? \
((pBuf->getImgWidthStride(plane)*Format::queryPlaneBitsPerPixel(Format::queryImageFormat(pBuf->getImgFormat().string()),plane)))>>3 : 0

#warning "WHP"
    size_t bufStridesInBytes[] = { GET_IIMGBUF_IMG_STRIDE_IN_BYTE(node.getImgBuf(), 0),
                                    GET_IIMGBUF_IMG_STRIDE_IN_BYTE(node.getImgBuf(), 1),
                                    GET_IIMGBUF_IMG_STRIDE_IN_BYTE(node.getImgBuf(), 2)};
    size_t bufBoundaryInBytes[] = {0,0,0};
    IImageBufferAllocator::ImgParam imgParam = IImageBufferAllocator::ImgParam(
                                                    Format::queryImageFormat(node.getImgBuf()->getImgFormat().string()),
                                                    MSize(
                                                        node.getImgBuf()->getImgWidth(),
                                                        node.getImgBuf()->getImgHeight()
                                                    ),
                                                    bufStridesInBytes,
                                                    bufBoundaryInBytes,
                                                    Format::queryPlaneCount(Format::queryImageFormat(node.getImgBuf()->getImgFormat().string()))
                                                );
    PortBufInfo_v1 portBufInfo = PortBufInfo_v1(
                                    node.getImgBuf()->getIonFd(),
                                    (MUINTPTR)node.getImgBuf()->getVirAddr(),
                                    0,
                                    node.getImgBuf()->getBufSecu(),
                                    node.getImgBuf()->getBufCohe()
                                );
    //
    rpBuffer = ImageBufferHeap::create(
                                    LOG_TAG,
                                    imgParam,
                                    portBufInfo,
                                    true //mbEenableIImageBufferLog
                                );
    if(rpBuffer == 0) {
        MY_LOGE("rpBuffer is NULL");
        return NO_MEMORY;
    }

    mClientBufferMap.add(rpBuffer.get(), node);
    MY_LOGD( "rpBuffer:%p", rpBuffer.get() );

    FUNC_END;
#undef GET_IIMGBUF_IMG_STRIDE_IN_BYTE
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
getBufferFromPool(
    char const*           szCallerName,
    sp<IImageBufferHeap>& rpBuffer
)
{
    FUNC_START;

    MY_LOGD("[%s] mAvailableBuf:%d mInUseBuf:%d", mAvailableBuf.size(), mInUseBuf.size());
    if( !mAvailableBuf.empty() )
    {
        typename List< sp<IImageBufferHeap> >::iterator iter = mAvailableBuf.begin();
        mInUseBuf.push_back(*iter);
        rpBuffer = *iter;
        mAvailableBuf.erase(iter);
        //
        FUNC_END;
        return OK;
    }

    FUNC_END;
    /*Condition cond;
    mWaitingList.push_back(&cond);

    //wait for buffer
    MY_LOGD("acquireFromPoolImpl waiting %lld ns", nsTimeout);
    cond.waitRelative(mLock, nsTimeout);

    android::List<android::Condition*>::iterator pCond = mWaitingList.begin();
    while( pCond != mWaitingList.end() ) {
        if( (*pCond) == &cond ) {
            mWaitingList.erase(pCond);
            break;
        }
        pCond++;
    }

    if( !mAvailableBuf.empty() )
    {
        typename android::List<MUINT32>::iterator iter = mAvailableBuf.begin();
        mInUseBuf.push_back(*iter);
        rpBuffer = *iter;
        mAvailableBuf.erase(iter);
        //
        return OK;
    }*/

    /*MY_LOGW("mPoolName timeout: buffer available %d, toAlloc %d",
             mAvailableBuf.size(), muToAllocCnt );*/
    return TIMED_OUT;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
releaseToPool(
    char const*          szCallerName,
    sp<IImageBufferHeap> rpBuffer,
    MUINT64              rTimeStamp,
    bool                 rErrorResult
)
{
    MY_LOGD_IF( mLogLevel >= 1, "%s", szCallerName);

    FUNC_START;

    //Mutex::Autolock _l(mLock);
    switch( mPoolMode ) {
        case POOLMODE_ALLOCATE:
        case POOLMODE_USER_PROVIDE:
            return returnBufferToPool(
                      szCallerName,
                      rpBuffer
                   );
        break;
        case POOLMODE_CAMCLIENT:
            return returnBufferToClient(
                      szCallerName,
                      rpBuffer,
                      rTimeStamp,
                      rErrorResult
                   );
        break;
        default:
            return BAD_VALUE;
        break;
    };

    FUNC_END;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
returnBufferToPool(
    char const* szCallerName,
    sp<IImageBufferHeap> rpBuffer
)
{
    FUNC_START;
    typename List< sp<IImageBufferHeap> >::iterator iter = mInUseBuf.begin();
    while( iter != mInUseBuf.end() ) {
        if ( rpBuffer == (*iter) ) {
            mAvailableBuf.push_back(*iter);
            mInUseBuf.erase(iter);

            MY_LOGD("[%s] mAvailableBuf:%d mInUseBuf:%d", mAvailableBuf.size(), mInUseBuf.size());
            FUNC_END;
            return OK;
        }
        iter++;
    }

    MY_LOGE("[%s] Cannot find buffer %p.", szCallerName, rpBuffer.get());

    FUNC_END;

    return UNKNOWN_ERROR;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
returnBufferToClient(
    char const*          szCallerName,
    sp<IImageBufferHeap> rpBuffer,
    MUINT64              rTimeStamp,
    bool                 rErrorResult
)
{
    FUNC_START;
    MY_LOGD("[%s] rpBuffer:%p rTimeStamp:%lld error:%d.", szCallerName, rpBuffer.get(), rTimeStamp, rErrorResult);
#warning "WHP QQ"
    ssize_t const index = mClientBufferMap.indexOfKey(rpBuffer.get());
    if ( index < 0 ) {
        MY_LOGE("[%s] rpBuffer:%p not found.", szCallerName, rpBuffer.get());
        return UNKNOWN_ERROR;
    }

    ImgBufQueNode node = mClientBufferMap.editValueFor(rpBuffer.get());

    if ( rErrorResult ) {
        const_cast<ImgBufQueNode*>(&(node))->setStatus(ImgBufQueNode::eSTATUS_CANCEL);
    } else {
        const_cast<ImgBufQueNode*>(&(node))->setStatus(ImgBufQueNode::eSTATUS_DONE);
        const_cast<ImgBufQueNode*>(&(node))->getImgBuf()->setTimestamp(rTimeStamp);  // time stamp
    }
    //

    mpClient->enqueProvider(node);

    MY_LOGD("[%s] mClientBufferMap size:%d.", szCallerName, mClientBufferMap.size());

    FUNC_END;

    return OK;
}
/******************************************************************************
 *
 ******************************************************************************/
char const*
BufferPoolImp::
poolName() const
{
    return "Cathy";
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
BufferPoolImp::
dumpPool() const
{
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
setImageStreamInfo(
    char const* szCallerName,
    android::sp<IImageStreamInfo> pStreamInfo
)
{
    //MY_LOGD( "[%s] format: size(,) (min,max):()", szCallerName);
    mpStreamInfo = pStreamInfo;

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
allocateBuffer(
    char const* szCallerName,
    size_t maxNumberOfBuffers,
    size_t minNumberOfInitialCommittedBuffers
)
{
    FUNC_START;

    mMaxBuffer = maxNumberOfBuffers;
    mMinBuffer = minNumberOfInitialCommittedBuffers;

    if ( mMinBuffer > mMaxBuffer) {
        MY_LOGE("mMinBuffer:%d > mMaxBuffer:%d", mMinBuffer, mMaxBuffer);
        return UNKNOWN_ERROR;
    }

    status_t status = run();
    if  ( OK != status ) {
        MY_LOGE("Fail to run the thread - status:%d(%s)", status, ::strerror(-status));
        return UNKNOWN_ERROR;
    }

    mPoolMode = POOLMODE_ALLOCATE;
    MY_LOGD("mPoolMode:%d (min,max):(%d,%d)", mPoolMode, minNumberOfInitialCommittedBuffers, maxNumberOfBuffers);

    FUNC_END;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
setCamClient(
    char const*                szCallerName,
    sp<ImgBufProvidersManager> pSource,
    MINT32                     rMode
)
{
    FUNC_START;

    //mpClient = pSource;
    mpSourceManager = pSource;
    mClientMode = rMode;
    mPoolMode = POOLMODE_CAMCLIENT;

    MY_LOGD("mPoolMode:%d mpSourceManager:%p", mPoolMode, mpSourceManager.get());
    FUNC_END;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
setUsersBuffer(
    char const* szCallerName,
    List<android::sp<IImageBuffer> > pSource
)
{
    mPoolMode = POOLMODE_USER_PROVIDE;
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
BufferPoolImp::
uninitPool(
    char const* szCallerName
)
{
    FUNC_START;

    mpClient = NULL;
    mAvailableBuf.clear();

    if ( mInUseBuf.size() > 0 ) {
        typename List< sp<IImageBufferHeap> >::iterator iter = mInUseBuf.begin();
        while( iter != mInUseBuf.end() ) {
            MY_LOGW("[%s] buffer %p not return to pool.", szCallerName, (*iter).get());
            iter++;
        }
    }

    mInUseBuf.clear();
    mClientBufferMap.clear();

    FUNC_END;
}

/******************************************************************************
 *
 ******************************************************************************/
void
BufferPoolImp::
onLastStrongRef(const void* /*id*/)
{
    requestExit();
    uninitPool("test");
}

/******************************************************************************
 *
 ******************************************************************************/
void
BufferPoolImp::
requestExit()
{
    //let allocate thread back
    Thread::requestExit();

    join();
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
BufferPoolImp::
readyToRun()
{
    // set name
    ::prctl(PR_SET_NAME, (unsigned long)BUFFERPOOL_NAME, 0, 0, 0);

    // set normal
    struct sched_param sched_p;
    sched_p.sched_priority = 0;
    ::sched_setscheduler(0, BUFFERPOOL_POLICY, &sched_p);
    ::setpriority(PRIO_PROCESS, 0, BUFFERPOOL_PRIORITY);
    //
    ::sched_getparam(0, &sched_p);

    MY_LOGD(
        "tid(%d) policy(%d) priority(%d)"
        , ::gettid(), ::sched_getscheduler(0)
        , sched_p.sched_priority
    );

    //
    return OK;

}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
BufferPoolImp::
do_construct(
    sp<IImageBufferHeap>& pImageBufferHeap
)
{
    IImageStreamInfo::BufPlanes_t const& bufPlanes = mpStreamInfo->getBufPlanes();
    size_t bufStridesInBytes[3] = {0};
    size_t bufBoundaryInBytes[3]= {0};
    for (size_t i = 0; i < bufPlanes.size(); i++) {
        bufStridesInBytes[i] = bufPlanes[i].rowStrideInBytes;
    }

    if ( eImgFmt_JPEG == mpStreamInfo->getImgFormat() )
    {
        IImageBufferAllocator::ImgParam imgParam(
                mpStreamInfo->getImgSize(),
                (*bufStridesInBytes),
                (*bufBoundaryInBytes));
        imgParam.imgFormat = eImgFmt_BLOB;
        MY_LOGD("eImgFmt_JPEG -> eImgFmt_BLOB");
        pImageBufferHeap = IIonImageBufferHeap::create(
                                mpStreamInfo->getStreamName(),
                                imgParam,
                                IIonImageBufferHeap::AllocExtraParam(),
                                MFALSE
                            );
    }
    else
    {
        IImageBufferAllocator::ImgParam imgParam(
            mpStreamInfo->getImgFormat(),
            mpStreamInfo->getImgSize(),
            bufStridesInBytes, bufBoundaryInBytes,
            bufPlanes.size()
            );
        MY_LOGD("format:%x, size:(%d,%d), stride:%d, boundary:%d, planes:%d", mpStreamInfo->getImgFormat(), mpStreamInfo->getImgSize().w, mpStreamInfo->getImgSize().h, bufStridesInBytes[0], bufBoundaryInBytes[0], bufPlanes.size());
        pImageBufferHeap = IIonImageBufferHeap::create(
                                mpStreamInfo->getStreamName(),
                                imgParam,
                                IIonImageBufferHeap::AllocExtraParam(),
                                MFALSE
                            );
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
bool
BufferPoolImp::
threadLoop()
{
    bool next = false;
    sp<IImageBufferHeap> pHeap;
    if( do_construct(pHeap) == NO_MEMORY ) {
        MY_LOGE("do_construct allocate buffer failed");
        return true;
    }

    {
        //android::Mutex::Autolock _l(mLock);
        mAvailableBuf.push_back(pHeap);

        next = (mAvailableBuf.size() + mInUseBuf.size() ) < mMaxBuffer;
        //signalUserLocked();
    }
    return next;
}
