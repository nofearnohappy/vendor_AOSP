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

#define LOG_TAG "MtkCam/DualImageTransformNode"
//
#include <mtkcam/Log.h>
#include <mtkcam/v3/hwnode/DualImageTransformNode.h>
#include <mtkcam/v3/stream/IStreamInfo.h>
#include <mtkcam/v3/stream/IStreamBuffer.h>
#include "BaseNode.h"
//
#include <utils/RWLock.h>
#include <utils/Thread.h>
//
#include <sys/prctl.h>
#include <sys/resource.h>

using namespace android;
using namespace NSCam;
using namespace NSCam::v3;
using namespace NSCam::Utils::Sync;
//
#include <mtkcam/iopipe/SImager/IImageTransform.h>
using namespace NSCam::NSIoPipe::NSSImager;

/******************************************************************************
 *
 ******************************************************************************/
#define DUALIMAGETRANSFORMTHREAD_NAME       ("Cam@DualImageTransform")
#define DUALIMAGETRANSFORMTHREAD_POLICY     (SCHED_OTHER)
#define DUALIMAGETRANSFORMTHREAD_PRIORITY   (0)

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

#define FUNC_START     MY_LOGD("+")
#define FUNC_END       MY_LOGD("-")

// temp design
#define Key_Main1_Crop  MTK_STEREO_JPS_MAIN1_CROP
#define Key_Main2_Crop  MTK_STEREO_JPS_MAIN2_CROP

/******************************************************************************
 *
 ******************************************************************************/
static inline
MBOOL
isStream(sp<IStreamInfo> pStreamInfo, StreamId_T streamId )
{
    return pStreamInfo.get() && pStreamInfo->getStreamId() == streamId;
}

/******************************************************************************
 *
 ******************************************************************************/
template <typename T>
inline MBOOL
tryGetMetadata(
    IMetadata const* const pMetadata,
    MUINT32 const tag,
    T & rVal
)
{
    if( pMetadata == NULL ) {
        MY_LOGE("pMetadata == NULL");
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
class DualImageTransformNodeImp
    : public BaseNode
    , public DualImageTransformNode
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                                            Definitions.
    typedef android::sp<IPipelineFrame>                     QueNode_T;
    typedef android::List<QueNode_T>                        Que_T;

protected:
    class TransformThread
        : public Thread
    {

    public:

                                    TransformThread(DualImageTransformNodeImp* pNodeImp)
                                        : mpNodeImp(pNodeImp)
                                    {}

                                    ~TransformThread()
                                    {}

    public:

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

    private:

                    DualImageTransformNodeImp*       mpNodeImp;

    };

private:
    class process_frame
    {
        public:
            sp<IPipelineFrame> const    mpFrame;

            sp<IImageBuffer>            mpJpeg_Main_buf;
            sp<IImageBuffer>            mpJpeg_Main2_buf;
            sp<IImageStreamBuffer>      mpInImgStreamBuffer_Main1;
            sp<IImageStreamBuffer>      mpInImgStreamBuffer_Main2;

            sp<IImageBuffer>            mpJps_Output_buf;
            sp<IImageStreamBuffer>      mpOutImgStreamBuffer;//

            IMetadata*                  mpInMetadata;
            sp<IMetaStreamBuffer>       mpInMetaStream_hal;
            // crop info
            //
            process_frame(
                        sp<IPipelineFrame> const pFrame,
                        NodeId_T nodeId,
                        char const* nodeName
                        ):
                        mpFrame(pFrame),
                        mNodeId(nodeId),
                        mNodeName(nodeName),
                        mpJpeg_Main_buf(NULL),
                        mpJpeg_Main2_buf(NULL),
                        mpInImgStreamBuffer_Main1(NULL),
                        mpInImgStreamBuffer_Main2(NULL),
                        mpJps_Output_buf(NULL),
                        mpOutImgStreamBuffer(NULL),
                        mpInMetadata(NULL),
                        mpInMetaStream_hal(NULL)
            {
            }

            MBOOL                       init();
            MBOOL                       uninit(MBOOL process_state);
            MRect                       getMain1Crop() { return mMain1Crop; }
            MRect                       getMain2Crop() { return mMain2Crop; }
        private:
            MRect                       mMain1Crop;
            MRect                       mMain2Crop;
            NodeId_T                    mNodeId;
            char const*                 mNodeName;
    };

    MVOID                           errorMetaHandle(
                                        process_frame*& rpProcessFrame,
                                        sp<IMetaStreamBuffer>& rpStreamBuffer,
                                        IMetadata*& rpMetadata
                                    );
    //
public:     ////                    Operations.

                                    DualImageTransformNodeImp();

                                    ~DualImageTransformNodeImp();

    virtual MERROR                  config(ConfigParams const& rParams);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IPipelineNode Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.

    virtual MERROR                  init(InitParams const& rParams);

    virtual MERROR                  uninit();

    virtual MERROR                  flush();

    virtual MERROR                  queue(
                                        android::sp<IPipelineFrame> pFrame
                                    );

protected:  ////                    Operations.

    MERROR                          onDequeRequest( //TODO: check frameNo
                                        android::sp<IPipelineFrame>& rpFrame
                                    );

    MVOID                           onProcessFrame(
                                        android::sp<IPipelineFrame> const& pFrame
                                    );

    MVOID                           waitForRequestDrained();


    MERROR                          getImageBufferAndLock(
                                        android::sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        sp<IImageStreamBuffer>& rpStreamBuffer,
                                        sp<IImageBuffer>& rpImageBuffer,
                                        MBOOL const isInStream
                                    );

    MERROR                          getMetadataAndLock(
                                        android::sp<IPipelineFrame> const& pFrame,
                                        StreamId_T const streamId,
                                        sp<IMetaStreamBuffer>& rpStreamBuffer,
                                        IMetadata*& rpOutMetadataResult
                                    );

    MBOOL                           doImageTransform(
                                        sp<IImageBuffer> srcBuf,
                                        sp<IImageBuffer> dstBuf,
                                        MRect crop,
                                        MBOOL const isRightBuf
                                    );

protected:

    MERROR                          threadSetting();

    MERROR                          verifyConfigParams(
                                        ConfigParams const & rParams
                                    ) const;

    MBOOL                           isInMetaStream(
                                        StreamId_T const streamId
                                    ) const;

protected:  ////                    Data Members. (Request Queue)
    mutable RWLock                  mConfigRWLock;
    // image
    sp<IImageStreamInfo>            mpInHalImageJpsMain1;
    sp<IImageStreamInfo>            mpInHalImageJpsMain2;

    sp<IImageStreamInfo>            mpOutHalImageJPS;

    // meta data
    sp<IMetaStreamInfo>             mpInHalMetadata;

    mutable Mutex                   mRequestQueueLock;
    Condition                       mRequestQueueCond;
    Que_T                           mRequestQueue;
    MBOOL                           mbRequestDrained;
    Condition                       mbRequestDrainedCond;
    MBOOL                           mbRequestExit;

private:   ////                     Threads
    sp<TransformThread>             mpTransformThread;

    IImageTransform*                mpImgTransform;
    MBOOL                           metaInLock;
};


/******************************************************************************
 *
 ******************************************************************************/
android::sp<DualImageTransformNode>
DualImageTransformNode::
createInstance()
{
    MY_LOGD("createInstance");
    return new DualImageTransformNodeImp();
}


/******************************************************************************
 *
 ******************************************************************************/
DualImageTransformNodeImp::
DualImageTransformNodeImp()
    : BaseNode()
    , DualImageTransformNode()
    //
    , mConfigRWLock()
    , mpInHalImageJpsMain1(NULL)
    , mpInHalImageJpsMain2(NULL)
    , mpOutHalImageJPS(NULL)
    , mpInHalMetadata(NULL)
    , mbRequestDrained(MFALSE)
    , mbRequestExit(MFALSE)
    //
    , mpTransformThread(NULL)
    //
    , mpImgTransform(NULL)
    , metaInLock(MFALSE)
{
}


/******************************************************************************
 *
 ******************************************************************************/
DualImageTransformNodeImp::
~DualImageTransformNodeImp()
{
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
init(InitParams const& rParams)
{
    FUNC_START;
    MERROR ret = UNKNOWN_ERROR;
    //
    mOpenId = rParams.openId;
    mNodeId = rParams.nodeId;
    mNodeName = rParams.nodeName;
    //
    MY_LOGD("OpenId %d, nodeId %d, name %s",
            getOpenId(), getNodeId(), getNodeName() );
    //
    mpTransformThread = new TransformThread(this);
    if( mpTransformThread->run(DUALIMAGETRANSFORMTHREAD_NAME) != OK )
    {
        MY_LOGE("run thread failed.");
        goto lbExit;
    }

    mpImgTransform = IImageTransform::createInstance();
    if(!mpImgTransform)
    {
        MY_LOGE("imageTransform create failed.");
        goto lbExit;
    }

    ret = OK;

lbExit:
    FUNC_END;
    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
config(ConfigParams const& rParams)
{
    FUNC_START;
    {
        MERROR const err = verifyConfigParams(rParams);
        if(err != OK)
        {
            MY_LOGE("verifyConfigParams failed, err = %d", err);
            return err;
        }
    }

    flush();

    {
        // image
        mpInHalImageJpsMain1 = rParams.pInHalImageJpsMain1;
        mpInHalImageJpsMain2 = rParams.pInHalImageJpsMain2;
        mpOutHalImageJPS = rParams.pOutHalImageJPS;
        // metadata
        mpInHalMetadata = rParams.pInHalMetadata;
    }
    MY_LOGD("mpInHalImageJpsMain1:%dx%d mpInHalImageJpsMain2:%dx%d",
        mpInHalImageJpsMain1->getImgSize().w, mpInHalImageJpsMain1->getImgSize().h,
        mpInHalImageJpsMain2->getImgSize().w, mpInHalImageJpsMain2->getImgSize().h);

    MY_LOGD("mpOutHalImageJPS:%dx%d",
        mpOutHalImageJPS->getImgSize().w, mpOutHalImageJPS->getImgSize().h);

    MY_LOGD("mpInHalMetadata:%#"PRIxPTR,
        mpInHalMetadata->getStreamId());

    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
uninit()
{
    FUNC_START;
    //
    if ( OK != flush() )
        MY_LOGE("flush failed");
    //
    // exit threads
    mpTransformThread->requestExit();
    // join
    mpTransformThread->join();
    //
    mpTransformThread = NULL;
    //
    if( mpImgTransform )
    {
        mpImgTransform->destroyInstance();
        mpImgTransform = NULL;
    }
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
flush()
{
    FUNC_START;
    //
    // 1. clear requests
    {
        Mutex::Autolock _l(mRequestQueueLock);
        //
        Que_T::iterator it = mRequestQueue.begin();
        while ( it != mRequestQueue.end() ) {
            BaseNode::flush(*it);
            it = mRequestQueue.erase(it);
        }
    }
    //
    // 2. wait enque thread
    waitForRequestDrained();
    //

    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
queue(android::sp<IPipelineFrame> pFrame)
{
    FUNC_START;
    //
    if( ! pFrame.get() ) {
        MY_LOGE("Null frame");
        return BAD_VALUE;
    }

    Mutex::Autolock _l(mRequestQueueLock);

    //  Make sure the request with a smaller frame number has a higher priority.
    Que_T::iterator it = mRequestQueue.end();
    for (; it != mRequestQueue.begin(); ) {
        --it;
        if  ( 0 <= (MINT32)(pFrame->getFrameNo() - (*it)->getFrameNo()) ) {
            ++it;   //insert(): insert before the current node
            break;
        }
    }

    mRequestQueue.insert(it, pFrame);

    mRequestQueueCond.signal();
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
onDequeRequest(
    android::sp<IPipelineFrame>& rpFrame
)
{
    FUNC_START;
    //
    Mutex::Autolock _l(mRequestQueueLock);
    //
    //  Wait until the queue is not empty or not going exit
    while ( mRequestQueue.empty() && ! mbRequestExit )
    {
        // set dained flag
        mbRequestDrained = MTRUE;
        mbRequestDrainedCond.signal();
        //
        status_t status = mRequestQueueCond.wait(mRequestQueueLock);
        if  ( OK != status ) {
            MY_LOGW(
                "wait status:%d:%s, mRequestQueue.size:%zu",
                status, ::strerror(-status), mRequestQueue.size()
            );
        }
    }
    //
    if  ( mbRequestExit ) {
        MY_LOGW_IF(!mRequestQueue.empty(), "[flush] mRequestQueue.size:%zu", mRequestQueue.size());
        return DEAD_OBJECT;
    }
    //
    //  Here the queue is not empty, take the first request from the queue.
    mbRequestDrained = MFALSE;
    rpFrame = *mRequestQueue.begin();
    mRequestQueue.erase(mRequestQueue.begin());
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
DualImageTransformNodeImp::
onProcessFrame(
    android::sp<IPipelineFrame> const& pFrame
)
{
    FUNC_START;
    //
    IPipelineFrame::InfoIOMapSet IOMapSet;

    if(
            OK != pFrame->queryInfoIOMapSet( getNodeId(), IOMapSet )
            || IOMapSet.mImageInfoIOMapSet.size() != 1
       ) {
        MY_LOGE("queryInfoIOMap failed, IOMap img/meta: %d/%d",
                IOMapSet.mImageInfoIOMapSet.size(),
                IOMapSet.mMetaInfoIOMapSet.size()
                );
        return;
    }

    process_frame* pProcessFrame = NULL;
    {
        IPipelineFrame::ImageInfoIOMap const& imageIOMap = IOMapSet.mImageInfoIOMapSet[0];

        pProcessFrame = new process_frame(pFrame, getNodeId(), getNodeName());

        // 1. get metadata stream buf and metadata.
        IMetadata* pInMeta_Hal = NULL;
        sp<IMetaStreamBuffer> pInMetaStream_Hal = pProcessFrame->mpInMetaStream_hal;
        MERROR const err = getMetadataAndLock(
                pFrame,
                mpInHalMetadata->getStreamId(),
                pInMetaStream_Hal,
                pInMeta_Hal
        );

        if( err != OK )
        {
            MY_LOGE("getMetadataAndLock err = %d", err);
            errorMetaHandle(pProcessFrame, pInMetaStream_Hal, pInMeta_Hal);
            delete pProcessFrame;
            pProcessFrame = NULL;
            return;
        }else{
            pProcessFrame->mpInMetadata = pInMeta_Hal;
        }



        // 2. get image stream buf and image buf.
        for( size_t i=0; i<imageIOMap.vIn.size() ; ++i )
        {
            StreamId_T const streamId = imageIOMap.vIn.keyAt(i);
            if( isStream(mpInHalImageJpsMain1, streamId) )
            {
                // Main1 stream.
                MERROR const err = getImageBufferAndLock(
                pFrame,
                mpInHalImageJpsMain1->getStreamId(),
                pProcessFrame->mpInImgStreamBuffer_Main1,
                pProcessFrame->mpJpeg_Main_buf,
                MTRUE
                );

                if( err != OK ) {
                    MY_LOGE("[main1] getImageBufferAndLock err = %d", err);
                    return;
                }
            }

            if( isStream(mpInHalImageJpsMain2, streamId) )
            {
                // Main2 stream.
                MERROR const err = getImageBufferAndLock(
                pFrame,
                mpInHalImageJpsMain2->getStreamId(),
                pProcessFrame->mpInImgStreamBuffer_Main2,
                pProcessFrame->mpJpeg_Main2_buf,
                MTRUE
                );

                if( err != OK ) {
                    MY_LOGE("[main2] getImageBufferAndLock err = %d", err);
                    return;
                }
            }
        }

        {
            // 3. get JPS output stream
            StreamId_T const streamId_out = mpOutHalImageJPS->getStreamId();

            MERROR const err = getImageBufferAndLock(
                    pFrame,
                    mpOutHalImageJPS->getStreamId(),
                    pProcessFrame->mpOutImgStreamBuffer,
                    pProcessFrame->mpJps_Output_buf,
                    MFALSE
                    );

            if( err != OK ) {
                MY_LOGE("getImageBufferAndLock err = %d", err);
                return;
            }

            // we need to unlock JPS buf here because doTransform() will try to lock this buffer later
            pProcessFrame->mpJps_Output_buf->unlockBuf(mNodeName);
        }
    }

    {
        pProcessFrame->init();
        // do some processing
        MBOOL transform_result = MTRUE;
        MRect crop;
        transform_result = doImageTransform(pProcessFrame->mpJpeg_Main_buf,
                                            pProcessFrame->mpJps_Output_buf,
                                            pProcessFrame->getMain1Crop(),
                                            MTRUE) &&
                           doImageTransform(pProcessFrame->mpJpeg_Main2_buf,
                                            pProcessFrame->mpJps_Output_buf,
                                            pProcessFrame->getMain2Crop(),
                                            MFALSE);
        //

        pProcessFrame->uninit(transform_result);
        delete pProcessFrame;
        pProcessFrame = NULL;
    }
    onDispatchFrame(pFrame);

    FUNC_END;
    return;
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
DualImageTransformNodeImp::
waitForRequestDrained()
{
    FUNC_START;
    //
    Mutex::Autolock _l(mRequestQueueLock);
    if( !mbRequestDrained ) {
        MY_LOGD("wait for request drained");
        mbRequestDrainedCond.wait(mRequestQueueLock);
    }
    //
    FUNC_END;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
getImageBufferAndLock(
    android::sp<IPipelineFrame> const& pFrame,
    StreamId_T const streamId,
    sp<IImageStreamBuffer>& rpStreamBuffer,
    sp<IImageBuffer>& rpImageBuffer,
    MBOOL const isInStream
)
{
    IStreamBufferSet&      rStreamBufferSet = pFrame->getStreamBufferSet();
    sp<IImageBufferHeap>   pImageBufferHeap = NULL;
    MERROR const err = ensureImageBufferAvailable_(
            pFrame->getFrameNo(),
            streamId,
            rStreamBufferSet,
            rpStreamBuffer
            );

    if( err != OK )
        return err;
    //
    //  Query the group usage.
    MUINT const groupUsage = rpStreamBuffer->queryGroupUsage(getNodeId());

    if(isInStream){
        pImageBufferHeap = rpStreamBuffer->tryReadLock(getNodeName());
    }
    else{
        pImageBufferHeap = rpStreamBuffer->tryWriteLock(getNodeName());
    }

    if (pImageBufferHeap == NULL) {
        MY_LOGE("pImageBufferHeap == NULL");
        return BAD_VALUE;
    }
    MY_LOGD("@pImageBufferHeap->getBufSizeInBytes(0) = %d", pImageBufferHeap->getBufSizeInBytes(0));
    rpImageBuffer = pImageBufferHeap->createImageBuffer();

    if (rpImageBuffer == NULL) {
        MY_LOGE("rpImageBuffer == NULL");
        return BAD_VALUE;
    }
    rpImageBuffer->lockBuf(getNodeName(), groupUsage);

    MY_LOGD("stream buffer: (%p) %p, heap: %p, buffer: %p, usage: %p",
        streamId, rpStreamBuffer.get(), pImageBufferHeap.get(), rpImageBuffer.get(), groupUsage);

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
getMetadataAndLock(
    android::sp<IPipelineFrame> const& pFrame,
    StreamId_T const streamId,
    sp<IMetaStreamBuffer>& rpStreamBuffer,
    IMetadata*& rpMetadata
)
{
    IStreamBufferSet&      rStreamBufferSet = pFrame->getStreamBufferSet();
    MY_LOGD("nodeID %d streamID %d",getNodeId(), streamId);
    MERROR const err = ensureMetaBufferAvailable_(
            pFrame->getFrameNo(),
            streamId,
            rStreamBufferSet,
            rpStreamBuffer
            );
    MY_LOGD_IF(rpStreamBuffer==NULL," rpStreamBuffer==NULL");
    metaInLock = MFALSE;

    if( err != OK )
        return err;

    rpMetadata = isInMetaStream(streamId) ?
        rpStreamBuffer->tryReadLock(getNodeName()) :
        rpStreamBuffer->tryWriteLock(getNodeName());

    metaInLock = MTRUE;
    if( rpMetadata == NULL ) {
        MY_LOGE("[frame:%u node:%d][stream buffer:%s] cannot get metadata",
                pFrame->getFrameNo(), getNodeId(), rpStreamBuffer->getName());
        return BAD_VALUE;
    }

    MY_LOGD_IF(1,"stream %#"PRIxPTR": stream buffer %p, metadata: %p",
        streamId, rpStreamBuffer.get(), rpMetadata);

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
DualImageTransformNodeImp::
doImageTransform(
    sp<IImageBuffer> srcBuf,
    sp<IImageBuffer> dstBuf,
    MRect crop,
    MBOOL const isRightBuf
)
{
    MBOOL ret = MTRUE;
    //
    if(dstBuf == NULL)
    {
        MY_LOGE("dstBuf is null.");
        return MFALSE;
    }
    //
    if(srcBuf == NULL)
    {
        MY_LOGE("srcBuf is null.");
        return MFALSE;
    }
    //
    sp<IImageBuffer> pDstBuf_SBS = NULL;

    // this value may get from stereo_hal.
    MUINT32 const trans = 0; // eTransform_None, if this value is not exist, it need to add to ImageFormat.h.

    pDstBuf_SBS = dstBuf->getImageBufferHeap()->createImageBuffer_SideBySide(isRightBuf);
    if(pDstBuf_SBS != NULL)
    {
        if(!pDstBuf_SBS->lockBuf(LOG_TAG, eBUFFER_USAGE_SW_MASK | eBUFFER_USAGE_HW_MASK))
        {
            MY_LOGE("lock buffer failed");
            ret = MFALSE;
        }
        pDstBuf_SBS->setTimestamp(srcBuf->getTimestamp());

        // Watch Out! this may cause image size reset!
        // EX: [IonHeap::setExtParam] update imgSize(1344x1984 -> 1920x1080), offset(0->0) @0-plane
        pDstBuf_SBS->setExtParam(crop.s);
    }
    else
    {
        MY_LOGE("pDstBuf_SBS is NULL.");
    }

    //
    MY_LOGD("src:(%p),S(%dx%d),stride(%d),F(%d),TS(%lld)", srcBuf.get(), srcBuf->getImgSize().w, srcBuf->getImgSize().h,
            srcBuf->getBufStridesInBytes(0), srcBuf->getImgFormat(), srcBuf->getTimestamp() );
    MY_LOGD_IF( pDstBuf_SBS != 0, "SBS:(%p),isR(%d),S(%dx%d),stride(%d),F(%d),TS(%lld),crop(%d,%d,%d,%d)", pDstBuf_SBS.get(), isRightBuf,
            pDstBuf_SBS->getImgSize().w, pDstBuf_SBS->getImgSize().h,
            pDstBuf_SBS->getBufStridesInBytes(0), pDstBuf_SBS->getImgFormat(), pDstBuf_SBS->getTimestamp(),
            crop.p.x, crop.p.y, crop.s.w, crop.s.h);

    // log information need query from stereo_hal.
    MY_LOGD_IF(0, "Stereo_Profile: mpImgTransform->execute +");
    mpImgTransform->execute(srcBuf.get(), NULL, pDstBuf_SBS.get(), crop, trans, 0xFFFFFFFF);
    MY_LOGD_IF(0, "Stereo_Profile: mpImgTransform->execute -");

    if(!pDstBuf_SBS->unlockBuf(LOG_TAG))
    {
        MY_LOGE("unlock buffer failed.");
        ret = MFALSE;
    }
    return ret;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
threadSetting()
{
    //
    //  thread policy & priority
    //  Notes:
    //      Even if pthread_create() with SCHED_OTHER policy, a newly-created thread
    //      may inherit the non-SCHED_OTHER policy & priority of the thread creator.
    //      And thus, we must set the expected policy & priority after a thread creation.
    MINT tid;
    struct sched_param sched_p;
    ::sched_getparam(0, &sched_p);
    if (DUALIMAGETRANSFORMTHREAD_POLICY == SCHED_OTHER) {
        sched_p.sched_priority = 0;
        ::sched_setscheduler(0, DUALIMAGETRANSFORMTHREAD_POLICY, &sched_p);
        ::setpriority(PRIO_PROCESS, 0, DUALIMAGETRANSFORMTHREAD_PRIORITY);   //  Note: "priority" is nice value.
    } else {
        sched_p.sched_priority = DUALIMAGETRANSFORMTHREAD_PRIORITY;          //  Note: "priority" is real-time priority.
        ::sched_setscheduler(0, DUALIMAGETRANSFORMTHREAD_POLICY, &sched_p);
    }

    MY_LOGD("tid(%d) policy(%d) priority(%d)", ::gettid(), DUALIMAGETRANSFORMTHREAD_POLICY, DUALIMAGETRANSFORMTHREAD_PRIORITY);

    return OK;

}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
verifyConfigParams(ConfigParams const & rParams) const
{
    if( !rParams.pInHalMetadata.get())
    {
        MY_LOGE("no pInHalMetadata");
        return BAD_VALUE;
    }
    if( !rParams.pInHalImageJpsMain1.get())
    {
        MY_LOGE("no InHalImageJpsMain1");
        return BAD_VALUE;
    }

    if( !rParams.pInHalImageJpsMain2.get())
    {
        MY_LOGE("no pInHalImageJpsMain2");
        return BAD_VALUE;
    }

    if( !rParams.pOutHalImageJPS.get())
    {
        MY_LOGE("no pOutHalImageJPS");
        return BAD_VALUE;
    }

    MY_LOGD_IF( rParams.pInHalMetadata.get(),
            "stream: [Meta] in hal DynamicDepth %#"PRIxPTR,
            rParams.pInHalMetadata->getStreamId()
            );
    MY_LOGD_IF( rParams.pInHalImageJpsMain1.get(),
            "stream: [Image] in hal JpsMain1 %#"PRIxPTR,
            rParams.pInHalImageJpsMain1->getStreamId()
            );

    MY_LOGD_IF( rParams.pInHalImageJpsMain2.get(),
            "stream: [Image] in hal JpsMain2 %#"PRIxPTR,
            rParams.pInHalImageJpsMain2->getStreamId()
            );

    MY_LOGD_IF( rParams.pOutHalImageJPS.get(),
            "stream: [Image] out hal ImageJPS %#"PRIxPTR,
            rParams.pOutHalImageJPS->getStreamId()
            );

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MERROR
DualImageTransformNodeImp::
isInMetaStream(
    StreamId_T const streamId
) const
{
    RWLock::AutoRLock _l(mConfigRWLock);
    return isStream(mpInHalMetadata, streamId);
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
DualImageTransformNodeImp::
errorMetaHandle(
    process_frame*& rpEncodeFrame,
    sp<IMetaStreamBuffer>& rpStreamBuffer,
    IMetadata*& rpMetadata
)
{
    FUNC_START;
    IStreamBufferSet& streamBufferSet = rpEncodeFrame->mpFrame->getStreamBufferSet();
    //in meta
    {
    if( metaInLock )
        rpStreamBuffer->unlock(getNodeName(), rpMetadata);

    //  Mark this buffer as USED by this user.
    //  Mark this buffer as RELEASE by this user.
    streamBufferSet.markUserStatus(
            mpInHalMetadata->getStreamId(), getNodeId(),
            IUsersManager::UserStatus::RELEASE
            );
    }
    // out jps image
    {
    streamBufferSet.markUserStatus(
            mpOutHalImageJPS->getStreamId(),
            getNodeId(),
            IUsersManager::UserStatus::RELEASE
            );
    }
    //in image main1
    {
         streamBufferSet.markUserStatus(
                         mpInHalImageJpsMain1->getStreamId(),
                         getNodeId(),
                         IUsersManager::UserStatus::RELEASE
                         );


    }
    //in image main2
    {
         streamBufferSet.markUserStatus(
                         mpInHalImageJpsMain2->getStreamId(),
                         getNodeId(),
                         IUsersManager::UserStatus::RELEASE
                         );


    }
    streamBufferSet.applyRelease(getNodeId());
    onDispatchFrame(rpEncodeFrame->mpFrame);
    FUNC_END;
}
/******************************************************************************
 *
 ******************************************************************************/
void
DualImageTransformNodeImp::TransformThread::
requestExit()
{
    //TODO: refine this
    Mutex::Autolock _l(mpNodeImp->mRequestQueueLock);
    mpNodeImp->mbRequestExit = MTRUE;
    mpNodeImp->mRequestQueueCond.signal();
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
DualImageTransformNodeImp::TransformThread::
readyToRun()
{
    return mpNodeImp->threadSetting();
}


/******************************************************************************
 *
 ******************************************************************************/
bool
DualImageTransformNodeImp::TransformThread::
threadLoop()
{
    sp<IPipelineFrame> pFrame;
    if  (
            !exitPending()
        &&  OK == mpNodeImp->onDequeRequest(pFrame)
        &&  pFrame != 0
        )
    {
        mpNodeImp->onProcessFrame(pFrame);

        return true;
    }

    MY_LOGD("exit unpack thread");
    return  false;

}

/******************************************************************************
 *
 ******************************************************************************/
MBOOL
DualImageTransformNodeImp::process_frame::
init()
{
    MBOOL ret = MFALSE;
    if(!mpInMetadata)
    {
        MY_LOGE("mpInMetadata is null.");
        goto lbExit;
    }

    // get main1 crop info.
    if(mpInMetadata)
    {
        MY_LOGD("temp use fixed main1 & main2 crop");
        mMain1Crop = MRect(2176, 1152); // JPS sbs size
        mMain2Crop = MRect(228, 128);
        // if(!tryGetMetadata<MRect>(mpInMetadata, Key_Main1_Crop, mMain1Crop))
        // {
        //     MY_LOGE("no tag: MTK_HAL_JPS_MAIN1_Crop_Info");
        //     mMain1Crop = MRect(MPoint(), mpJpeg_Main_buf->getImgSize());
        // }
        // if(!tryGetMetadata<MRect>(mpInMetadata, Key_Main2_Crop, mMain2Crop))
        // {
        //     MY_LOGE("no tag: MTK_HAL_JPS_MAIN2_Crop_Info");
        //     mMain2Crop = MRect(MPoint(), mpJpeg_Main2_buf->getImgSize());
        // }
    }else{
        MY_LOGE("mMain1Crop & mMain2Crop not set!");
        goto lbExit;
    }

    MY_LOGD("JPS main1Crop:(%d,%d,%d,%d)  main2Crop:(%d,%d,%d,%d)",
        mMain1Crop.p.x,
        mMain1Crop.p.y,
        mMain1Crop.s.w,
        mMain1Crop.s.h,
        mMain2Crop.p.x,
        mMain2Crop.p.y,
        mMain2Crop.s.w,
        mMain2Crop.s.h
    );

    ret = MTRUE;
lbExit:
    return ret;
}

/******************************************************************************
 *
 ******************************************************************************/
MBOOL
DualImageTransformNodeImp::process_frame::
uninit(MBOOL process_state)
{
    MBOOL ret = MTRUE;

    IStreamBufferSet& streamBufferSet = mpFrame->getStreamBufferSet();

    // unlock and mark buffer state to main1 inStream.
    mpJpeg_Main_buf->unlockBuf(mNodeName);
    mpInImgStreamBuffer_Main1->unlock(mNodeName, mpJpeg_Main_buf->getImageBufferHeap());
    streamBufferSet.markUserStatus(
                mpInImgStreamBuffer_Main1->getStreamInfo()->getStreamId(),
                mNodeId,
                IUsersManager::UserStatus::USED |
                IUsersManager::UserStatus::RELEASE
                );

    // unlock and mark buffer state to main2 inStream.
    mpJpeg_Main2_buf->unlockBuf(mNodeName);
    mpInImgStreamBuffer_Main2->unlock(mNodeName, mpJpeg_Main2_buf->getImageBufferHeap());
    streamBufferSet.markUserStatus(
                mpInImgStreamBuffer_Main2->getStreamInfo()->getStreamId(),
                mNodeId,
                IUsersManager::UserStatus::USED |
                IUsersManager::UserStatus::RELEASE
                );

    //unlock and mark buffer state to outputstream
    mpJps_Output_buf->unlockBuf(mNodeName);
    mpOutImgStreamBuffer->unlock(mNodeName, mpJps_Output_buf->getImageBufferHeap());
    mpOutImgStreamBuffer->markStatus(
            process_state ?
            STREAM_BUFFER_STATUS::WRITE_OK : STREAM_BUFFER_STATUS::WRITE_ERROR
            );

    streamBufferSet.markUserStatus(
            mpOutImgStreamBuffer->getStreamInfo()->getStreamId(),
            mNodeId,
            IUsersManager::UserStatus::USED |
            IUsersManager::UserStatus::RELEASE
            );

    streamBufferSet.applyRelease(mNodeId);
    return ret;
}