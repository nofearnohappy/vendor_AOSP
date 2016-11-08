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

#define LOG_TAG "MtkCam/ResultProcessor"
//
#include <utils/Thread.h>
//
#include <sys/prctl.h>
#include <sys/resource.h>
#include <system/thread_defs.h>
//
#include "MyUtils.h"
#include <utils/List.h>
#include <utils/KeyedVector.h>
#include <v1/Processor/ResultProcessor.h>
//
using namespace android;
using namespace NSCam;
using namespace NSCam::v1;
using namespace NSCam::v3;
//
/******************************************************************************
 *
 ******************************************************************************/
#define RESULTPROCESSOR_NAME       ("Cam@ResultProcessor")
#define RESULTPROCESSOR_POLICY     (SCHED_OTHER)
#define RESULTPROCESSOR_PRIORITY   (0)

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

class ResultProcessorImp
    : public ResultProcessor
    , protected Thread
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  ResultProcessor Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:

    virtual status_t                registerListener(
                                        StreamId_T      const streamId,
                                        wp< IListener > const listener
                                    );

    virtual status_t                registerListener(
                                        MUINT32         const startRequestNo,
                                        MUINT32         const endRequestNo,
                                        MBOOL           const needPartial,
                                        wp< IListener > const listener
                                    );

    virtual status_t                removeListener(
                                        StreamId_T      const streamId,
                                        wp< IListener > const listener
                                    );

    virtual status_t                removeListener(
                                        MUINT32         const startRequestNo,
                                        MUINT32         const endRequestNo,
                                        MBOOL           const needPartial,
                                        wp< IListener > const listener
                                    );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IAppCallback Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual void                    updateFrame(
                                        MUINT32 const requestNo,
                                        MINTPTR const userId,
                                        Result const& result
                                    );

    virtual MVOID                   onLastStrongRef( const void* /*id*/);

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
protected: ////                     Structure.

    class ListenerBase : public RefBase
    {
    public:
                                ListenerBase() {};

        virtual                 ~ListenerBase() {};

        virtual bool            canCallback(
                                    MUINT32                      requestNo,
                                    const sp<IMetaStreamBuffer>& pBuf
                                )                                      = 0;

        virtual sp< IListener > getListener()                          = 0;

        virtual bool            equal(
                                    StreamId_T      /*id*/,
                                    wp< IListener > /*rlistener*/
                                 ) {
                                    MY_LOGW("should not happen!");
                                    return false;
                                 }

        virtual bool            equal(
                                    MUINT32         /*start*/,
                                    MUINT32         /*end*/,
                                    wp< IListener > /*rlistener*/
                                ) {
                                    MY_LOGW("should not happen!");
                                    return false;
                                }

    };

    class RangeListener : public ListenerBase
    {
    public:
                                RangeListener(
                                    MUINT32 start,
                                    MUINT32 end,
                                    wp< IListener > rlistener)
                                    : startRequestNo(start)
                                    , endRequestNo(end)
                                    , listener(rlistener)
                                {}

        virtual sp< IListener > getListener() {
                                    return listener.promote();
                                }

        virtual bool            canCallback(
                                    MUINT32                      requestNo,
                                    const sp<IMetaStreamBuffer>& /*pBuf*/
                                ) {
                                    return (requestNo >= startRequestNo
                                         && requestNo <= endRequestNo);
                                }

        virtual bool            equal(
                                    MUINT32         start,
                                    MUINT32         end,
                                    wp< IListener > rlistener
                                ) {
                                    return ( start == startRequestNo
                                            && end == endRequestNo
                                            && rlistener == listener
                                            );
                                }

    protected:
        MUINT32         startRequestNo;
        MUINT32         endRequestNo;
        wp< IListener > listener;
    };

    class StreamListener : public ListenerBase
    {
    public:
                                StreamListener(
                                    StreamId_T id,
                                    wp< IListener > rlistener
                                )
                                    : streamId(id)
                                    , listener(rlistener)
                                {}

        virtual sp< IListener > getListener() {
                                    return listener.promote();
                                }

        virtual bool            canCallback(
                                    MUINT32                      /*requestNo*/,
                                    const sp<IMetaStreamBuffer>& pBuf
                                ) {
                                    return (pBuf->getStreamInfo()->getStreamId() == streamId);
                                }

        virtual bool            equal(
                                    StreamId_T      id,
                                    wp< IListener > rlistener
                                ) {
                                    return ( id == streamId && rlistener == listener );
                                }

    protected:
        StreamId_T      streamId;
        wp< IListener > listener;
    };

    struct BufferBit
    {
        enum
        {
            RETURNED,
            LAST_PARTIAL,
        };
    };

    struct ResultItem : public RefBase
    {
        MUINT32                         requestNo;
        BitSet32                        appBuffer;
        BitSet32                        halBuffer;
        Vector< sp<IMetaStreamBuffer> > outAppBuffer;
        Vector< sp<IMetaStreamBuffer> > outHalBuffer;
    };

    struct Callback : public RefBase
    {
        MUINT32         requestNo;
        StreamId_T      streamId;
        MBOOL           errorResult;
        IMetadata*      buffer;
        sp< IListener > listener;
    };

public:     ////                    Definitions.

    typedef KeyedVector< MUINT32, sp< ResultItem > > BufQue_T;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Implementations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////                    Operations.
                                    ResultProcessorImp();

                                    ~ResultProcessorImp();

protected:

            status_t                dequeResult( BufQue_T& rvResult );

            MVOID                   handleResult(
                                        BufQue_T const&                     rvResult
                                    );

            MVOID                   handlePartialResultCallback(
                                        BufQue_T const& rvResult
                                    );

            MVOID                   handleFullResultCallback();

            MVOID                   handleListener(
                                        sp< ResultItem >&                   pItem,
                                        List< sp<ListenerBase> >&           listenerList,
                                        /*out*/
                                        List< sp<Callback> >&               callbackList
                                    );

            MVOID                   addPartialCallback(
                                        MUINT32                             requestNo,
                                        sp<ListenerBase>&                   item,
                                        Vector< sp<IMetaStreamBuffer> >&    vOutMeta,
                                        /*out*/
                                        List< sp<Callback> >&               callbackList
                                    );

protected:  ////                    Full result callback.
            MVOID                   collectPartialResult(
                                        MUINT32          const requestNo,
                                        sp< ResultItem > const rpItem
                                    );

            MVOID                   addFullCallback(
                                        MUINT32                             requestNo,
                                        sp<ListenerBase>&                   item,
                                        Vector< sp<IMetaStreamBuffer> >&    vOutMeta,
                                        /*out*/
                                        List< sp<Callback> >&               callbackList
                                    );

            bool                    isRemovableLocked(
                                        MUINT32                    requestNo
                                    );

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected: ////                     Data Members. (partial result listener).
    List< sp<ListenerBase> >        mRangeListenerList;
    List< sp<ListenerBase> >        mStreamListenerList;
    mutable Mutex                   mListenerListLock;

protected: ////                     Data Members. (partial result queue).
    BufQue_T                        mResultQueue;
    mutable Mutex                   mResultQueueLock;
    Condition                       mResultQueueCond;

protected: ////                     Data Members. (full result).
    BufQue_T                        mFullResultQueue;
    mutable Mutex                   mFullResultQueueLock;
    List< sp<ListenerBase> >        mFullListenerList;

protected: ////                     Logs.
    MINT32                          mLogLevel;
};


/******************************************************************************
 *
 ******************************************************************************/
sp< ResultProcessor >
ResultProcessor::
createInstance()
{
    return new ResultProcessorImp();
}

/******************************************************************************
 *
 ******************************************************************************/
ResultProcessorImp::
ResultProcessorImp()
    : ResultProcessor()
    , mLogLevel(1)
{
    status_t status = run();
    if  ( OK != status ) {
        MY_LOGE("Fail to run the thread - status:%d(%s)", status, ::strerror(-status));
    }
}


/******************************************************************************
 *
 ******************************************************************************/
ResultProcessorImp::
~ResultProcessorImp()
{
}


/******************************************************************************
 *
 ******************************************************************************/
void
ResultProcessorImp::
onLastStrongRef(const void* /*id*/)
{
    requestExit();
}

/******************************************************************************
 *
 ******************************************************************************/
void
ResultProcessorImp::
requestExit()
{
    //let deque thread back
    Thread::requestExit();
    {
        Mutex::Autolock _l(mListenerListLock);
        mRangeListenerList.clear();
        mFullListenerList.clear();
        mStreamListenerList.clear();
    }
    //
    {
        Mutex::Autolock _l(mResultQueueLock);
        mResultQueue.clear();
        mResultQueueCond.broadcast();
    }
    //
    {
        Mutex::Autolock _l(mFullResultQueueLock);
        mFullResultQueue.clear();
    }
    join();
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
ResultProcessorImp::
readyToRun()
{
    // set name
    ::prctl(PR_SET_NAME, (unsigned long)RESULTPROCESSOR_NAME, 0, 0, 0);

    // set normal
    struct sched_param sched_p;
    sched_p.sched_priority = 0;
    ::sched_setscheduler(0, RESULTPROCESSOR_POLICY, &sched_p);
    ::setpriority(PRIO_PROCESS, 0, RESULTPROCESSOR_PRIORITY);
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
status_t
ResultProcessorImp::
registerListener(
    StreamId_T      const streamId,
    wp< IListener > const listener
)
{
    Mutex::Autolock _l(mListenerListLock);
    //
    List< sp<ListenerBase> >::iterator item = mStreamListenerList.begin();
    while ( item != mStreamListenerList.end() ) {
        if ( (*item)->equal(streamId, listener) ) {
            // already registered, just return
            MY_LOGD_IF(
                1,
                "Attempt to register the same client twice, ignoring"
            );
            return OK;
        }
        item++;
    }
    //
    sp< IListener > l = listener.promote();
    if ( l == 0 ) {
        MY_LOGW("Bad listener. stream:%d", streamId);
    } else {
        sp<ListenerBase> sListener = new StreamListener(streamId, listener);
        mStreamListenerList.push_back(sListener);
        //
        MY_LOGD_IF(
            1,
            "StreamListener:%s(%d) stream:%d",
            l->getUserName().string(), mStreamListenerList.size(), streamId
        );
    }

    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
ResultProcessorImp::
registerListener(
    MUINT32         const startRequestNo,
    MUINT32         const endRequestNo,
    MBOOL           const needPartial,
    wp< IListener > const listener
)
{
    Mutex::Autolock _l(mListenerListLock);
    //
    List< sp<ListenerBase> >* listeners;
    if ( needPartial ) {
        listeners = &mRangeListenerList;
    } else {
        listeners = &mFullListenerList;
    }
    //
    List< sp<ListenerBase> >::iterator item = (*listeners).begin();
    while ( item != (*listeners).end() ) {
        if ( (*item)->equal(startRequestNo, endRequestNo, listener) ) {
            // already registered, just return
            MY_LOGD_IF(
                1,
                "Attempt to register the same client twice, ignoring"
            );
            return OK;
        }
        item++;
    }
    //
    sp< IListener > l = listener.promote();
    if ( l == 0 ) {
        MY_LOGW("Bad listener. range(%d,%d) partial:%d", startRequestNo, endRequestNo, needPartial);
    } else {
        sp<ListenerBase> rListener = new RangeListener(startRequestNo, endRequestNo, listener);
        (*listeners).push_back(rListener);
        //
        MY_LOGD_IF(
            1,
            "RangeListener:%s(%d) range(%d,%d) partial:%d",
            l->getUserName().string(), (*listeners).size(), startRequestNo, endRequestNo, needPartial
        );
    }
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
ResultProcessorImp::
removeListener(
    MUINT32         const startRequestNo,
    MUINT32         const endRequestNo,
    MBOOL           const needPartial,
    wp< IListener > const listener
)
{
    Mutex::Autolock _l(mListenerListLock);
    //
    List< sp<ListenerBase> >* listeners;
    if ( needPartial ) {
        listeners = &mRangeListenerList;
    } else {
        listeners = &mFullListenerList;
    }
    //
    List< sp<ListenerBase> >::iterator item = (*listeners).begin();
    while ( item != (*listeners).end() ) {
        if ( (*item)->equal(startRequestNo, endRequestNo, listener) ) {
            item = (*listeners).erase(item);
            MY_LOGD_IF(
                1,
                "Remove listener for range(%d,%d) partial:%d",
                startRequestNo, endRequestNo, needPartial
            );
        } else {
            item++;
        }
    }
    //
    sp< IListener > l = listener.promote();
    if ( l == 0 ) {
        MY_LOGW("Bad listener. range(%d,%d) partial:%d", startRequestNo, endRequestNo, needPartial);
    } else {
        MY_LOGD_IF(
            1,
            "RangeListener:%s(%d) range(%d,%d) partial:%d",
            l->getUserName().string(), (*listeners).size(), startRequestNo, endRequestNo, needPartial
        );
    }
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
ResultProcessorImp::
removeListener(
    StreamId_T      const streamId,
    wp< IListener > const listener
)
{
    Mutex::Autolock _l(mListenerListLock);
    //
    List< sp<ListenerBase> >::iterator item = mStreamListenerList.begin();
    while ( item != mStreamListenerList.end() ) {
        if ( (*item)->equal(streamId, listener) ) {
            item = mStreamListenerList.erase(item);
            MY_LOGD_IF(
                1,
                "Remove listener for stream id %d",
                streamId
            );
        } else {
            item++;
        }
    }
    //
    sp< IListener > l = listener.promote();
    if ( l == 0 ) {
        MY_LOGW("Bad listener. stream:%d", streamId);
    } else {
        MY_LOGD_IF(
            1,
            "RangeListener:%s(%d) stream:%d",
            l->getUserName().string(), mStreamListenerList.size(), streamId
        );
    }
    //
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
updateFrame(
    MUINT32 const  requestNo,
    MINTPTR const  userId,
    Result  const& result
)
{
    Mutex::Autolock _l(mResultQueueLock);
    //
#if 1
    {
        String8 str = String8::format("requestNo:%u userId:%#" PRIxPTR " appMeta(left:%d) ", requestNo, userId, result.nAppOutMetaLeft);
        for ( size_t i = 0; i < result.vAppOutMeta.size(); ++i)
            str += String8::format("%s ", result.vAppOutMeta[i]->getName());
        str += String8::format("halMeta(left:%d) ", result.nHalOutMetaLeft);
        for ( size_t i = 0; i < result.vHalOutMeta.size(); ++i)
            str += String8::format("%s ", result.vHalOutMeta[i]->getName());
        MY_LOGD_IF(
            mLogLevel >= 1,
            "%s", str.string()
        );
    }
#endif
    //
    if  ( exitPending() ) {
        MY_LOGW("Dead ResultQueue");
        return;
    }
    //
    ssize_t const index = mResultQueue.indexOfKey(requestNo);

    if  ( 0 <= index ) {
        MY_LOGW("requestNo:%u existed @ index:%zd", requestNo, index);
        //
        sp<ResultItem> pItem = mResultQueue.editValueAt(index);
        pItem->outAppBuffer.appendVector(result.vAppOutMeta);
        pItem->outHalBuffer.appendVector(result.vHalOutMeta);
        //
        if ( result.nAppOutMetaLeft == 0 )
            pItem->appBuffer.markBit(BufferBit::LAST_PARTIAL);
        if ( result.nHalOutMetaLeft == 0 )
            pItem->halBuffer.markBit(BufferBit::LAST_PARTIAL);
        //
        mResultQueueCond.broadcast();
    }
    else {
        sp< ResultItem > pItem = new ResultItem;
        pItem->requestNo       = requestNo;
        pItem->outAppBuffer    = result.vAppOutMeta;
        pItem->outHalBuffer    = result.vHalOutMeta;
        //
        if ( result.nAppOutMetaLeft == 0 )
            pItem->appBuffer.markBit(BufferBit::LAST_PARTIAL);
        if ( result.nHalOutMetaLeft == 0 )
            pItem->halBuffer.markBit(BufferBit::LAST_PARTIAL);
        //
        mResultQueue.add(requestNo, pItem);
        mResultQueueCond.broadcast();
    }
}

/******************************************************************************
 *
 ******************************************************************************/
bool
ResultProcessorImp::
threadLoop()
{
    BufQue_T vResult;
    MERROR err = dequeResult(vResult);
    if  ( OK == err && ! vResult.isEmpty() )
    {
        handleResult(vResult);
    }
    //
    return  true;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
ResultProcessorImp::
dequeResult(
    BufQue_T& rvResult
)
{
    status_t err = OK;
    //
    Mutex::Autolock _l(mResultQueueLock);
    //
    while ( ! exitPending() && mResultQueue.isEmpty() )
    {
        err = mResultQueueCond.wait(mResultQueueLock);
        MY_LOGW_IF(
            OK != err,
            "exitPending:%d ResultQueue#:%zu err:%d(%s)",
            exitPending(), mResultQueue.size(), err, ::strerror(-err)
        );
    }
    //
    if  ( mResultQueue.isEmpty() )
    {
        MY_LOGD_IF(
            mLogLevel >= 1,
            "empty queue"
        );
        rvResult.clear();
        err = NOT_ENOUGH_DATA;
    }
    else
    {
        rvResult = mResultQueue;
        mResultQueue.clear();
        err = OK;
    }
    //
    return err;
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
handleResult(
    BufQue_T const& rvResult
)
{
    // handle partial callback
    handlePartialResultCallback( rvResult );
    //
    // handle full result callback
    {
        Mutex::Autolock _l(mFullResultQueueLock);
        //
        handleFullResultCallback();
        //
        // remove full result
        for (size_t i = 0; i < mFullResultQueue.size();) {
            MUINT32 const requestNo = mFullResultQueue.keyAt(i);
            sp< ResultItem > pItem  = mFullResultQueue.valueAt(i);
            if  ( pItem == 0 ) {
                MY_LOGW_IF(
                    mLogLevel >= 1,
                    "requestNo %d NULL ResultItem", requestNo
                );
                continue;
            }
            //
            if (    pItem->appBuffer.hasBit(BufferBit::LAST_PARTIAL)
                 && pItem->halBuffer.hasBit(BufferBit::LAST_PARTIAL) ) {
                MY_LOGW_IF(
                    mLogLevel >= 1,
                    "remove mFullResultQueue[%d]", i
                );
                i = mFullResultQueue.removeItemsAt(i);
                continue;
            }
            //
            ++i;
        }
    }
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
handlePartialResultCallback(
    BufQue_T const& rvResult
)
{
    for (size_t i = 0; i < rvResult.size(); i++) {
        MUINT32 const requestNo = rvResult.keyAt(i);
        sp< ResultItem > pItem  = rvResult.valueAt(i);
        if  ( pItem == 0 ) {
            MY_LOGW_IF(
                mLogLevel >= 1,
                "requestNo %d NULL ResultItem", requestNo
            );
            continue;
        }
        //
        collectPartialResult( requestNo, pItem );
        //
        List< sp<Callback> >  callbackList;
        {
            Mutex::Autolock _l(mListenerListLock);
            handleListener(pItem, mRangeListenerList, callbackList);
            handleListener(pItem, mStreamListenerList, callbackList);
        }
        //
        List< sp<Callback> >::iterator item = callbackList.begin();
        for (; item != callbackList.end(); item++) {
            MY_LOGD_IF(
                mLogLevel >= 1,
                "[partial] %s requestNo:%d streamId:%d error:%d buffer:%p(%d)",
                (*item)->listener->getUserName().string(), (*item)->streamId, (*item)->requestNo, (*item)->errorResult, (*item)->buffer, (*item)->buffer->count()
            );
            //
            (*item)->listener->onResultReceived(
                                    (*item)->requestNo,
                                    (*item)->streamId,
                                    (*item)->errorResult,
                                    (*item)->buffer
                               );
        }
    }
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
handleFullResultCallback()
{
    for (size_t i = 0; i < mFullResultQueue.size(); i++) {
        MUINT32 const requestNo = mFullResultQueue.keyAt(i);
        sp< ResultItem > pItem  = mFullResultQueue.valueAt(i);
        if  ( pItem == 0 ) {
            MY_LOGW_IF(
                mLogLevel >= 1,
                "requestNo %d NULL ResultItem", requestNo
            );
            continue;
        }
        //
        List< sp<Callback> >  callbackList;
        {
            Mutex::Autolock _l(mListenerListLock);
            //
            List< sp<ListenerBase> >::iterator item = mFullListenerList.begin();
            while (item != mFullListenerList.end()) {
                // app metadata
                if (    pItem->appBuffer.hasBit(BufferBit::LAST_PARTIAL)
                    && !pItem->appBuffer.hasBit(BufferBit::RETURNED) ) {
                    addFullCallback(pItem->requestNo, (*item), pItem->outAppBuffer, /*out*/callbackList);
                }
                // hal metadata
                if (    pItem->halBuffer.hasBit(BufferBit::LAST_PARTIAL)
                    && !pItem->halBuffer.hasBit(BufferBit::RETURNED) ) {
                    addFullCallback(pItem->requestNo, (*item), pItem->outHalBuffer, /*out*/callbackList);
                }
                //
                if ( isRemovableLocked( requestNo ) ) {
                    item = mFullListenerList.erase(item);
                } else {
                    item++;
                }
            }
            if ( pItem->appBuffer.hasBit(BufferBit::LAST_PARTIAL) )
                pItem->appBuffer.markBit(BufferBit::RETURNED);
            if ( pItem->halBuffer.hasBit(BufferBit::LAST_PARTIAL) )
                pItem->halBuffer.markBit(BufferBit::RETURNED);
        }
        //
        List< sp<Callback> >::iterator item = callbackList.begin();
        for (; item != callbackList.end(); item++) {
            MY_LOGD_IF(
                mLogLevel >= 1,
                "[full] %s requestNo:%d streamId:%d error%d buffer:%p(%d)",
                (*item)->listener->getUserName().string(), (*item)->streamId, (*item)->requestNo, (*item)->errorResult, (*item)->buffer, (*item)->buffer->count()
            );
            //
            (*item)->listener->onResultReceived(
                                    (*item)->requestNo,
                                    (*item)->streamId,
                                    (*item)->errorResult,
                                    (*item)->buffer
                               );
        }
    }
}
/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
collectPartialResult(
    MUINT32          const requestNo,
    sp< ResultItem > const rpItem
)
{
    Mutex::Autolock _l(mFullResultQueueLock);
    ssize_t const index = mFullResultQueue.indexOfKey(requestNo);

    if  ( 0 <= index ) {
        MY_LOGD_IF(
            mLogLevel >= 1,
            "requestNo:%u existed @ index:%zd", requestNo, index
        );
        //
        sp<ResultItem> pItem = mFullResultQueue.editValueAt(index);
        pItem->outAppBuffer.appendVector(rpItem->outAppBuffer);
        pItem->outHalBuffer.appendVector(rpItem->outHalBuffer);
        //
        if ( rpItem->appBuffer.hasBit(BufferBit::LAST_PARTIAL) )
            pItem->appBuffer.markBit(BufferBit::LAST_PARTIAL);
        if ( rpItem->halBuffer.hasBit(BufferBit::LAST_PARTIAL) )
            pItem->halBuffer.markBit(BufferBit::LAST_PARTIAL);
    }
    else {
        sp< ResultItem > pItem = new ResultItem;
        pItem->requestNo       = requestNo;
        pItem->outAppBuffer    = rpItem->outAppBuffer;
        pItem->outHalBuffer    = rpItem->outHalBuffer;
        //
        if ( rpItem->appBuffer.hasBit(BufferBit::LAST_PARTIAL) )
            pItem->appBuffer.markBit(BufferBit::LAST_PARTIAL);
        if ( rpItem->halBuffer.hasBit(BufferBit::LAST_PARTIAL) )
            pItem->halBuffer.markBit(BufferBit::LAST_PARTIAL);
        //
        mFullResultQueue.add(requestNo, pItem);
    }
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
handleListener(
    sp< ResultItem >&         pItem,
    List< sp<ListenerBase> >& listenerList,
    /*out*/
    List< sp<Callback> >&     callbackList
)
{
    List< sp<ListenerBase> >::iterator item = listenerList.begin();
    while (item != listenerList.end()) {
        addPartialCallback(pItem->requestNo, (*item), pItem->outAppBuffer, /*out*/callbackList);
        addPartialCallback(pItem->requestNo, (*item), pItem->outHalBuffer, /*out*/callbackList);
        item++;
    }
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
addPartialCallback(
    MUINT32                             requestNo,
    sp<ListenerBase>&                   item,
    Vector< sp<IMetaStreamBuffer> >&    vOutMeta,
    /*out*/
    List< sp<Callback> >&               callbackList
)
{
    for ( size_t i = 0; i < vOutMeta.size(); ++i) {
        if ( item->canCallback(requestNo, vOutMeta[i]) ) {
            sp< IListener > listener = item->getListener();
            if (listener == 0) {
                //item = listenerList.erase(item);
                //continue;
                return;
            } else {
                sp< Callback > pCallback = new Callback;
                pCallback->requestNo     = requestNo;
                pCallback->streamId      = vOutMeta[i]->getStreamInfo()->getStreamId();
                pCallback->errorResult   = vOutMeta[i]->hasStatus(STREAM_BUFFER_STATUS::ERROR);
                pCallback->buffer        = vOutMeta[i]->tryReadLock(LOG_TAG);
                pCallback->listener      = listener;
                callbackList.push_back(pCallback);
                //
                vOutMeta[i]->unlock(LOG_TAG, pCallback->buffer);
            }
        }
    }
}

/******************************************************************************
 *
 ******************************************************************************/
MVOID
ResultProcessorImp::
addFullCallback(
    MUINT32                             requestNo,
    sp<ListenerBase>&                   item,
    Vector< sp<IMetaStreamBuffer> >&    vOutMeta,
    /*out*/
    List< sp<Callback> >&               callbackList
)
{
    if ( !item->canCallback(requestNo, NULL) ) return;
    //
    sp< IListener > listener = item->getListener();
    if (listener == 0) {
        //item = listenerList.erase(item);
        return;
    } else {
        IMetadata* result = vOutMeta[0]->tryReadLock(LOG_TAG);
        vOutMeta[0]->unlock(LOG_TAG, result);
        for ( size_t i = 1; i < vOutMeta.size(); ++i) {
            IMetadata* meta = vOutMeta[i]->tryReadLock(LOG_TAG);
            (*result) += (*meta);
            vOutMeta[i]->unlock(LOG_TAG, meta);
        }
        //
        sp< Callback > pCallback = new Callback;
        pCallback->requestNo     = requestNo;
        pCallback->streamId      = 0;
        pCallback->errorResult   = vOutMeta[vOutMeta.size() - 1]->hasStatus(STREAM_BUFFER_STATUS::ERROR);
        pCallback->buffer        = result;
        pCallback->listener      = listener;
        callbackList.push_back(pCallback);
    }
}

/******************************************************************************
 *
 ******************************************************************************/
bool
ResultProcessorImp::
isRemovableLocked(
    MUINT32 requestNo
)
{
    sp< ResultItem > pItem = mFullResultQueue.valueFor(requestNo);
    //
    // both app & hal meta return last partial
    return (   pItem->appBuffer.hasBit(BufferBit::LAST_PARTIAL)
            && pItem->halBuffer.hasBit(BufferBit::LAST_PARTIAL) );
}
