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

#define LOG_TAG "MtkCam/ppl_context"
//
#include "MyUtils.h"
#include "PipelineContextImpl.h"
//#include <v3/pipeline/PipelineContext.h>
//#include <v3/pipeline/IPipelineNodeMapControl.h>
//#include <v3/pipeline/IPipelineFrameNumberGenerator.h>
//#include <utils/KeyedVectr.h>
//#include <utils/RWLock.h>
//
using namespace android;
using namespace NSCam;
using namespace NSCam::v3;
using namespace NSCam::v3::NSPipelineContext;


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
const IOMapSet&
IOMapSet::
empty()
{
    static IOMapSet o;
    return o;
}


/******************************************************************************
 *
 ******************************************************************************/
sp<PipelineContext>
PipelineContext::
create(char const* name)
{
    return new PipelineContext(name);
}


/******************************************************************************
 *
 ******************************************************************************/
PipelineContext::
PipelineContext(char const* name)
    : mpImpl( new PipelineContextImpl(name) )
{
    MY_LOGD("ctor(%p)", this);
}


/******************************************************************************
 *
 ******************************************************************************/
PipelineContext::
~PipelineContext()
{
    MY_LOGD("dtor(%p)", this);
}


/******************************************************************************
 *
 ******************************************************************************/
char const*
PipelineContext::
getName() const
{
    return mpImpl->getName();
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
beginConfigure(
    android::sp<PipelineContext> oldContext
)
{
    // if old == current
    if( oldContext.get() == this ) {
        MY_LOGW("context: old == this");
        return INVALID_OPERATION;
    }
    //
    waitUntilDrained();
    //
    if( oldContext.get() ) {
        String8 name(getName());
        MY_LOGD("context: old %s, current %s", oldContext->getName(), getName() );
    }
    //
    mpOldContext = oldContext;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
endConfigure()
{
    MERROR err;
    //
    if(1) dump();
    // TODO: when to release old context?
    // 1. release old context
    mpOldContext = NULL;
    //
    if( OK != (err = getImpl()->config(mpOldContext.get() ? mpOldContext->getImpl() : NULL)) )
        return err;
    //
    // TODO: add status check!
    return OK;
}


#if 0
/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
beginReuseConfigure(
    android::sp<PipelineContext> oldContext
)
{
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
endReuseConfigure()
{
    return OK;
}
#endif


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
queue(
    android::sp<IPipelineFrame> const &pFrame
)
{
    return getImpl()->queue(pFrame);
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
flush()
{
    FUNC_START;
    // begin flush
    getImpl()->beginFlush();
    //
    // wait until drained
    getImpl()->waitUntilDrained();
    //
    // end flush
    getImpl()->endFlush();
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
waitUntilDrained()
{
    FUNC_START;
    //
    getImpl()->waitUntilDrained();
    //
    FUNC_END;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
setScenarioControl(
    android::sp<IScenarioControl> pControl
)
{
    return getImpl()->setScenarioControl(pControl);
}


/******************************************************************************
 *
 ******************************************************************************/
android::sp<IScenarioControl>
PipelineContext::
getScenarioControl() const
{
    return getImpl()->getScenarioControl();
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
PipelineContext::
setDispatcher(
    android::wp<IDispatcher> pDispatcher
)
{
    return getImpl()->setDispatcher(pDispatcher);
}


/******************************************************************************
 *
 ******************************************************************************/
sp<HalImageStreamBufferPoolT>
PipelineContext::
queryImageStreamPool(
    StreamId_T const streamId
) const
{
    return getImpl()->queryImageStreamPool(streamId);
}


/******************************************************************************
 *
 ******************************************************************************/
sp<INodeActor>
PipelineContext::
queryINodeActor(NodeId_T const nodeId) const
{
    return getImpl()->queryNode(nodeId);
}


/******************************************************************************
 *
 ******************************************************************************/
MVOID
PipelineContext::
dump()
{
    MY_LOGD("dump pipeline(%s) begin", getName());
    mpImpl->dump();
    MY_LOGD("dump pipeline(%s) end", getName());
}


/******************************************************************************
 *
 ******************************************************************************/
PipelineContext::PipelineContextImpl*
PipelineContext::
getImpl() const
{
    return mpImpl.get();
}


#if 0
/******************************************************************************
 *
 ******************************************************************************/
wp<IImgStreamHandle>
PipelineContext::
getStreamHandle(
    sp<IImageStreamInfo> pStreamInfo
) const
{
    return NULL;
}
#endif


/******************************************************************************
 *
 ******************************************************************************/
void
INodeActor::
onLastStrongRef(const void* /*id*/)
{
    Mutex::Autolock _l(mLock);
    //
    if( muStatus >= eNodeState_Init) {
        onUninit();
    }
}


/******************************************************************************
 *
 ******************************************************************************/
MUINT32
INodeActor::
getStatus() const
{
    Mutex::Autolock _l(mLock); return muStatus;
}


/******************************************************************************
 *
 ******************************************************************************/
#define PRECHECK_STATUS( _status_, _skip_st_, _expected_ )                 \
    do {                                                                   \
        if( _status_ >= _skip_st_ )                                        \
        {                                                                  \
            MY_LOGD("%s already in state %d",                              \
                    getNode()->getNodeName(), _status_);                   \
            return OK;                                                     \
        } else if ( _status_ != _expected_ ) {                             \
            MY_LOGE("%s wrong status %d, expected %d",                     \
                    getNode()->getNodeName(), _status_, _expected_);       \
            return INVALID_OPERATION;                                      \
        }                                                                  \
    } while(0)

#define UPDATE_STATUS_IF_OK( _ret_, _status_var_, _newstatus_ ) \
    do {                                                        \
        if( _ret_ == OK ) {                                     \
            _status_var_ = _newstatus_;                         \
        } else {                                                \
            MY_LOGE("%s ret = %d",                              \
                    getNode()->getNodeName(), _ret_);           \
        }                                                       \
    } while(0)
/******************************************************************************
 *
 ******************************************************************************/
MERROR
INodeActor::
init()
{
    Mutex::Autolock _l(mLock);
    PRECHECK_STATUS( muStatus, eNodeState_Init, eNodeState_Create);
    MERROR const ret = onInit();
    UPDATE_STATUS_IF_OK( ret, muStatus, eNodeState_Init);
    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
INodeActor::
config()
{
    Mutex::Autolock _l(mLock);
    PRECHECK_STATUS( muStatus, eNodeState_Config, eNodeState_Init);
    MERROR const ret = onConfig();
    UPDATE_STATUS_IF_OK( ret, muStatus, eNodeState_Config);
    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
INodeActor::
uninit()
{
    Mutex::Autolock _l(mLock);
    if( muStatus < eNodeState_Init ) {
        MY_LOGD("already uninit or not init");
        return OK;
    }
    //
    MERROR const ret = onUninit();
    if( ret != OK ) {
        MY_LOGE("uninit failed");
    }
    // always update
    muStatus = eNodeState_Create;
    return ret;

}


/******************************************************************************
 *
 ******************************************************************************/
ContextNode::
ContextNode(
        NodeId_T const nodeId,
        sp<INodeActor> pNode
        )
    : mNodeId(nodeId)
    , mpNode(pNode)
    //
    , mpInStreams(NULL)
    , mpOutStreams(NULL)
{}


/******************************************************************************
 *
 ******************************************************************************/
ContextNode::
~ContextNode()
{
}


/******************************************************************************
 *
 ******************************************************************************/
