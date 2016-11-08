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

#include "EISNode.h"

#include "GpuWarpBase.h"

#define PIPE_CLASS_TAG "EISNode"
#define PIPE_TRACE TRACE_EIS_NODE
#include <featurePipe/core/include/PipeLog.h>

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

EISNode::EISNode(const char *name)
  : StreamingFeatureNode(name)
  , mQueueFuture(MFALSE)
{
  TRACE_FUNC_ENTER();
  this->addWaitQueue(&mRequests);
  this->addWaitQueue(&mConfigDatas);
  TRACE_FUNC_EXIT();
}

EISNode::~EISNode()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MBOOL EISNode::onData(DataID id, const RequestPtr &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data->mRequestNo, ID2Name(id));
  MBOOL ret = MFALSE;
  if( id == ID_P2A_TO_EIS_P2DONE )
  {
    mRequests.enque(data);
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL EISNode::onData(DataID id, const EisConfigData &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data.mRequest->mRequestNo, ID2Name(id));
  MBOOL ret = MFALSE;
  if( id == ID_P2A_TO_EIS_CONFIG )
  {
    mConfigDatas.enque(data);
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL EISNode::onInit()
{
  TRACE_FUNC_ENTER();
  StreamingFeatureNode::onInit();
  int w = 80, h = 80;
  mWarpMapBufferPool = ImageBufferPool::create("EISNode", w, h, eImgFmt_BAYER8, ImageBufferPool::USAGE_SW);

  int EIS_WARP_MAP_NUM = 3;
  mWarpMapBufferPool->allocate(EIS_WARP_MAP_NUM);
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL EISNode::onUninit()
{
  TRACE_FUNC_ENTER();
  ImageBufferPool::destroy(mWarpMapBufferPool);
  mWarpMapBufferPool = NULL;
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL EISNode::onThreadLoop()
{
  TRACE_FUNC("Waitloop");
  RequestPtr request;
  EisConfigData configData;

  if( !waitAllQueue() )
  {
    return MFALSE;
  }
  if( !mRequests.deque(request) )
  {
    MY_LOGE("P2A done deque out of sync");
    return MFALSE;
  }
  if( !mConfigDatas.deque(configData) )
  {
    MY_LOGE("ConfigData deque out of sync");
    return MFALSE;
  }
  if( request == NULL ||
      request != configData.mRequest )
  {
    MY_LOGE("Request out of sync");
    return MFALSE;
  }
  TRACE_FUNC_ENTER();
  request->mTimer.startEIS();
  TRACE_FUNC("Frame %d in EIS", request->mRequestNo);
  processEIS(request, configData.mData);
  request->mTimer.stopEIS();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL EISNode::processEIS(const RequestPtr &request, const EIS_HAL_CONFIG_DATA &config)
{
  TRACE_FUNC_ENTER();
  unsigned holdingSize = 0;
  if( request->needFutureEIS() )
  {
    // holdingSize = 25;
    holdingSize = 3;
  }
  mQueue.push(request);
  while( mQueue.size() > holdingSize )
  {
    ImgBuffer warpMap;
    RequestPtr last;
    MSize fullSize;

    last = mQueue.front();
    mQueue.pop();
    warpMap = mWarpMapBufferPool->requestIIBuffer();
    fullSize = request->getMaxOutSize();
    GpuWarpBase::makePassThroughWarp(warpMap->getImageBuffer(), fullSize);
    handleWarpData(warpMap, last);
    warpMap = NULL;
  }
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL EISNode::handleWarpData(const ImgBuffer &warp, const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  StreamingFeatureDataID next;
  next = request->needVFB() ? ID_EIS_TO_VFB_WARP : ID_EIS_TO_GPU_WARP;
  ret = handleData(next, ImgBufferData(warp, request));
  TRACE_FUNC_EXIT();
  return ret;
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
