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

#include "GPUNode.h"

#define PIPE_CLASS_TAG "GPUNode"
#define PIPE_TRACE TRACE_GPU_NODE
#include <featurePipe/core/include/PipeLog.h>

#if NUM_DEFAULT_BUFFER > MAX_NUM_GPU_WARP_BUFFER
#error NUM_DEFAULT_BUFFER > MAX_NUM_GPU_WARP_BUFFER
#endif

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

GPUNode::GPUNode(const char *name)
  : StreamingFeatureNode(name)
{
  TRACE_FUNC_ENTER();
  this->addWaitQueue(&mWarpMapDatas);
  this->addWaitQueue(&mFullImgDatas);
  TRACE_FUNC_EXIT();
}

GPUNode::~GPUNode()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MVOID GPUNode::setInputBufferPool(android::sp<GraphicBufferPool> pool)
{
  TRACE_FUNC_ENTER();
  mInputBufferPool = pool;
  TRACE_FUNC_EXIT();
}

MVOID GPUNode::setOutputBufferPool(android::sp<GraphicBufferPool> pool)
{
  TRACE_FUNC_ENTER();
  mOutputBufferPool = pool;
  TRACE_FUNC_EXIT();
}

MBOOL GPUNode::onData(DataID id, const ImgBufferData &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data.mRequest->mRequestNo, ID2Name(id));
  MBOOL ret = MFALSE;
  if( id == ID_VFB_TO_GPU_WARP ||
      id == ID_EIS_TO_GPU_WARP )
  {
    this->mWarpMapDatas.enque(data);
    ret = MTRUE;
  }
  else if( id == ID_P2A_TO_GPU_FULLIMG )
  {
    this->mFullImgDatas.enque(data);
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL GPUNode::onInit()
{
  TRACE_FUNC_ENTER();
  StreamingFeatureNode::onInit();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL GPUNode::onUninit()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL GPUNode::onThreadStart()
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  if( mInputBufferPool != NULL &&
      mOutputBufferPool != NULL )
  {
    ret = mGpuWarp.init(MAX_FULL_SIZE, MAX_WARP_SIZE);
    if( ret )
    {
      if( mInputBufferPool->peakPoolSize() > MAX_NUM_GPU_WARP_BUFFER ||
          mOutputBufferPool->peakPoolSize() > MAX_NUM_GPU_WARP_BUFFER )
      {
        MY_LOGE("GpuWarp not ready for more than 5 buffers");
        ret = MFALSE;
      }
      else
      {
        MUINT32 feature = GpuWarpBase::getDefaultFeature();
        feature = GpuWarpBase::toggleVFB(feature, MTRUE);
        feature = GpuWarpBase::toggleEIS(feature, MTRUE);
        GraphicBufferPool::CONTAINER_TYPE in, out;
        GpuWarpBase::GB_PTR_ARRAY inAddr, outAddr;

        in = mInputBufferPool->getPoolContents();
        out = mOutputBufferPool->getPoolContents();
        inAddr.resize(in.size());
        outAddr.resize(out.size());

        for( unsigned i = 0; i < in.size(); ++i )
        {
          inAddr[i] = &(in[i]->mGraphicBuffer);
        }
        for( unsigned i = 0; i < out.size(); ++i )
        {
          outAddr[i] = &(out[i]->mGraphicBuffer);
        }

        ret = mGpuWarp.config(inAddr, outAddr, feature);
        if( !ret )
        {
          MY_LOGE("GpuWar config failed");
        }
      }
    }
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL GPUNode::onThreadStop()
{
  TRACE_FUNC_ENTER();
  mGpuWarp.uninit();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL GPUNode::onThreadLoop()
{
  TRACE_FUNC("Waitloop");
  MUINT32 requestNo;
  RequestPtr request;
  ImgBufferData warpMap;
  ImgBufferData inBuffer;

  if( !waitAllQueueSync(requestNo) )
  {
    return MFALSE;
  }
  if( !mWarpMapDatas.deque(requestNo, warpMap) )
  {
    MY_LOGE("WarpMapData deque out of sync: %d", requestNo);
    return MFALSE;
  }
  else if( !mFullImgDatas.deque(requestNo, inBuffer) )
  {
    MY_LOGE("FullImgData deque out of sync: %d", requestNo);
    return MFALSE;
  }
  if( warpMap.mRequest == NULL ||
      warpMap.mRequest->mRequestNo != requestNo ||
      warpMap.mRequest != inBuffer.mRequest )
  {
    MY_LOGE("Request out of sync");
    return MFALSE;
  }
  TRACE_FUNC_ENTER();
  request = warpMap.mRequest;
  request->mTimer.startGPU();
  TRACE_FUNC("Frame %d in GPU", request->mRequestNo);
  processGPU(request, warpMap.mData, inBuffer.mData);
  request->mTimer.stopGPU();
  return MTRUE;
}

MBOOL GPUNode::processGPU(const RequestPtr &request, const ImgBuffer &warpMap, const ImgBuffer &fullImg)
{
  TRACE_FUNC_ENTER();
  ImgBufferData outBuffer;
  void *inGBAddr = NULL, *outGBAddr = NULL;
  MSize fullImgSize;

  fullImgSize = fullImg->getImageBuffer()->getImgSize();

  outBuffer.mRequest = request;
  outBuffer.mData = mOutputBufferPool->requestIIBuffer();
  outBuffer.mData->getImageBuffer()->setExtParam(fullImgSize);

  inGBAddr = fullImg->getGraphicBufferAddr();
  outGBAddr = outBuffer.mData->getGraphicBufferAddr();

  MBOOL warpResult = MFALSE;
  if( inGBAddr && outGBAddr )
  {
    sp<GraphicBuffer> *inPtr = (sp<GraphicBuffer>*)inGBAddr;
    sp<GraphicBuffer> *outPtr = (sp<GraphicBuffer>*)outGBAddr;
    IImageBuffer *warpPtr = warpMap->getImageBufferPtr();
    MSize inSize = fullImgSize;
    MSize outSize = fullImgSize;

    request->mTimer.startWarpGPU();
    warpResult = mGpuWarp.process(inPtr, outPtr, warpPtr,
                                  inSize, outSize);
    request->mTimer.stopWarpGPU();
  }
  if( !warpResult )
  {
    MY_LOGE("Frame %d failed GpuWarp", request->mRequestNo);
  }
  request->updateResult(warpResult);

  handleData(ID_GPU_TO_MDP_FULLIMG, outBuffer);
  TRACE_FUNC_EXIT();
  return MTRUE;
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
