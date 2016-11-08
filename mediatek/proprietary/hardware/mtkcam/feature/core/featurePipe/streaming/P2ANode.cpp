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

#include "P2ANode.h"

#define PIPE_CLASS_TAG "P2ANode"
#define PIPE_TRACE TRACE_P2A_NODE
#include <featurePipe/core/include/PipeLog.h>

using namespace NSCam::NSIoPipe::NSPostProc;
using NSCam::NSIoPipe::PortID;
using NSCam::NSIoPipe::EPortType_Memory;
using NSImageio::NSIspio::EPortIndex_LCEI;
using NSImageio::NSIspio::EPortIndex_IMG3O;
using NSImageio::NSIspio::EPortIndex_WDMAO;
using NSImageio::NSIspio::EPortIndex_VIPI;

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

P2ANode::P2ANode(const char *name)
  : StreamingFeatureNode(name)
  , mNormalStream(NULL)
{
  TRACE_FUNC_ENTER();
  this->addWaitQueue(&mRequests);
  TRACE_FUNC_EXIT();
}

P2ANode::~P2ANode()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MVOID P2ANode::setNormalStream(INormalStream *stream)
{
  TRACE_FUNC_ENTER();
  mNormalStream = stream;
  TRACE_FUNC_EXIT();
}

MVOID P2ANode::setDsImgPool(android::sp<ImageBufferPool> pool)
{
  TRACE_FUNC_ENTER();
  mDsImgPool = pool;
  TRACE_FUNC_EXIT();
}

MVOID P2ANode::setFullImgPool(android::sp<GraphicBufferPool> pool)
{
  TRACE_FUNC_ENTER();
  mFullImgPool = pool;
  TRACE_FUNC_EXIT();
}

MBOOL P2ANode::onInit()
{
  TRACE_FUNC_ENTER();

  StreamingFeatureNode::onInit();

  init3DNR();

  initVHDR();

  initP2();

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::onUninit()
{
  TRACE_FUNC_ENTER();

  uninit3DNR();

  uninitVHDR();

  uninitP2();

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::onThreadStart()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::onThreadStop()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::onData(DataID id, const RequestPtr &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data->mRequestNo, ID2Name(id));
  MBOOL ret;

  switch( id )
  {
  case ID_ROOT_ENQUE:
    mRequests.enque(data);
    ret = MTRUE;
    break;
  default:
    ret = MFALSE;
    break;
  }

  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL P2ANode::onThreadLoop()
{
  TRACE_FUNC("Waitloop");
  RequestPtr request;

  if( !waitAllQueue() )
  {
    return MFALSE;
  }
  if( !mRequests.deque(request) )
  {
    MY_LOGE("Request deque out of sync");
    return MFALSE;
  }
  else if( request == NULL )
  {
    MY_LOGE("Request out of sync");
    return MFALSE;
  }
  TRACE_FUNC_ENTER();

  request->mTimer.startP2A();
  MY_LOGD("Frame %d in P2A, feature=0x%04x", request->mRequestNo, request->mFeatureMask);

  processP2A(request);
  request->mTimer.stopP2A();

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::processP2A(const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  P2AEnqueData data;
  QParams param;
  MSize postCropSize;

  data.mRequest = request;

  postCropSize = calcPostCropSize(request);

  prepareQParams(param, request, postCropSize);

  prepare3A(param, request);

  if( request->need3DNR() )
  {
    prepare3DNR(param, request);
  }
  if( request->needVHDR() )
  {
    prepareVHDR(param, request);
  }
  if( request->needEIS() )
  {
    prepareEIS(param, request);
  }

  prepareIO(param, request, data, postCropSize);
  enqueFeatureStream(param, data);

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MSize P2ANode::calcPostCropSize(const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  MSize postCropSize;
  postCropSize = request->getMaxOutSize();
  TRACE_FUNC_EXIT();
  return postCropSize;
}

MBOOL P2ANode::onQParamsCB(QParams &params, P2AEnqueData &data)
{
  TRACE_FUNC_ENTER();

  RequestPtr request;
  request = data.mRequest;
  if( request == NULL )
  {
    MY_LOGE("Missing request");
    return MFALSE;
  }

  request->mTimer.stopEnqueP2A();
  MY_LOGD("Frame %d enque done in %d ms", request->mRequestNo, request->mTimer.getElapsedEnqueP2A());

  request->mP2QParams = params;

  if( request->isLastNodeP2A() )
  {
    request->updateResult(params.mDequeSuccess);
    request->doExtCallback(FeaturePipeParam::MSG_FRAME_DONE);
  }
  else if( request->needGPU() )
  {
    handleData(ID_P2A_TO_GPU_FULLIMG, ImgBufferData(data.mFullImg, request));
  }
  else if( request->needVFB() )
  {
    handleData(ID_P2A_TO_P2B_FULLIMG, ImgBufferData(data.mFullImg, request));
  }
  else
  {
    MY_LOGE("Why are you here?");
    return MFALSE;
  }

  if( request->need3DNR() )
  {
    mPrevFullImg = data.mFullImg;
  }

  if( request->needVFB() )
  {
    handleData(ID_P2A_TO_FD_DSIMG, ImgBufferData(data.mDsImg, request));
    handleData(ID_P2A_TO_VFB_DSIMG, ImgBufferData(data.mDsImg, request));
  }

  if( request->needEIS() )
  {
    handleData(ID_P2A_TO_EIS_P2DONE, request);
  }

  request->mTimer.stopP2A();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::initP2()
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  if( mNormalStream != NULL )
  {
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MVOID P2ANode::uninitP2()
{
  TRACE_FUNC_ENTER();

  mNormalStream = NULL;
  mPrevFullImg = NULL;
  TRACE_FUNC_EXIT();
}

MBOOL P2ANode::prepareQParams(QParams &params, const RequestPtr &request, MSize postCropSize)
{
  TRACE_FUNC_ENTER();
  params = request->mQParams;
  prepareCropInfo(params, request, postCropSize);
  prepareStreamTag(params, request);
  TRACE_FUNC_EXIT();
  return MFALSE;
}

MBOOL P2ANode::prepareCropInfo(QParams &params, const RequestPtr &request, MSize postCropSize)
{
  TRACE_FUNC_ENTER();

  if( request->needDsImg() )
  {
    MCrpRsInfo crop;
    crop.mGroupID                 = 2;
    crop.mCropRect.s              = postCropSize;
    crop.mCropRect.p_integral.x   = 0;
    crop.mCropRect.p_integral.y   = 0;
    crop.mCropRect.p_fractional.x = 0;
    crop.mCropRect.p_fractional.y = 0;
    crop.mResizeDst               = MSize(0, 0);
    params.mvCropRsInfo.clear();
    params.mvCropRsInfo.push_back(crop);
  }

  // TODO: side effect for non VFB feature
  {
    int cropIndex = -1;
    for( unsigned i = 0, n = request->mQParams.mvCropRsInfo.size(); i < n; ++i )
    {
      if( request->mQParams.mvCropRsInfo[i].mGroupID == 2 )
      {
        cropIndex = i;
        break;
      }
      else if( request->mQParams.mvCropRsInfo[i].mGroupID == 3 )
      {
        cropIndex = i;
      }
    }
    if( cropIndex >= 0 )
    {
      MCrpRsInfo crop = request->mQParams.mvCropRsInfo[cropIndex];
      crop.mGroupID = 1; // IMGO crz module
      crop.mResizeDst = postCropSize;
      params.mvCropRsInfo.push_back(crop);
    }
  }

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::prepareStreamTag(QParams &params, const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  if( request->needVFB() || request->needEIS() )
  {
    params.mvStreamTag.clear();
    params.mvStreamTag.push_back(EFeatureStreamTag_vFB_Stream);
  }
  else if( request->need3DNR() )
  {
    params.mvStreamTag.clear();
    params.mvStreamTag.push_back(EFeatureStreamTag_Stream);
  }
  else
  {
    //params.mvStreamTag.clear();
    //params.mvStreamTag.push_back(ENormalStreamTag_Normal);
  }
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::prepareIO(QParams &params, const RequestPtr &request, P2AEnqueData &data, MSize postCropSize)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MTRUE;
  printRequestIO(request);
  params.mvIn = request->mQParams.mvIn;
  params.mvOut.clear();

  if( request->isLastNodeP2A() )
  {
    params.mvOut = request->mQParams.mvOut;
  }
  if( request->needIMG3O() )
  {
    data.mFullImg = mFullImgPool->requestIIBuffer();
    data.mFullImg->getImageBuffer()->setExtParam(postCropSize);
    Output output;
    output.mPortID = PortID(EPortType_Memory, EPortIndex_IMG3O, 1);
    output.mBuffer = data.mFullImg->getImageBufferPtr();
    output.mBuffer->setTimestamp(request->mQParams.mvIn[0].mBuffer->getTimestamp());
    params.mvOut.push_back(output);
  }
  if( request->needDsImg() )
  {
    MSize inputSize, dsSize;

    data.mDsImg = mDsImgPool->requestIIBuffer();
    inputSize = request->mQParams.mvIn[0].mBuffer->getImgSize();
    dsSize = calcDsImgSize(inputSize);
    MY_LOGD("DsImg size from %dx%d to %dx%d", inputSize.w, inputSize.h, dsSize.w, dsSize.h);
    data.mDsImg->getImageBuffer()->setExtParam(dsSize);
    Output output;
    output.mPortID = PortID(EPortType_Memory, EPortIndex_WDMAO, 1);
    output.mBuffer = data.mDsImg->getImageBufferPtr();
    params.mvOut.push_back(output);
  }
  if( request->need3DNR() )
  {
    if( mPrevFullImg != NULL )
    {
      Input input;
      input.mPortID = PortID(EPortType_Memory, EPortIndex_VIPI, 0);
      input.mBuffer = mPrevFullImg->getImageBufferPtr();
#if DEV_3DNR_READY
      params.mvIn.push_back(input);
#endif
      data.mPrevFullImg = mPrevFullImg;
    }
  }

  TRACE_FUNC_EXIT();
  return ret;
}

MVOID P2ANode::enqueFeatureStream(NSCam::NSIoPipe::NSPostProc::QParams &params, P2AEnqueData &data)
{
  TRACE_FUNC_ENTER();
  MBOOL ret;
  MY_LOGD("Frame %d enque start", data.mRequest->mRequestNo);
  data.mRequest->mTimer.startEnqueP2A();
  ret = this->enqueQParams(mNormalStream, params, data);
  if( !ret )
  {
    MY_LOGE("Frame %d enque failed", data.mRequest->mRequestNo);
    data.mRequest->updateResult(MFALSE);
    P2ANode::onQParamsCB(params, data);
  }
  TRACE_FUNC_EXIT();
}

MVOID P2ANode::printRequestIO(const RequestPtr &request)
{
  for( unsigned i = 0, n = request->mQParams.mvIn.size(); i < n; ++i )
  {
    unsigned index = request->mQParams.mvIn[i].mPortID.index;
    MSize size = request->mQParams.mvIn[i].mBuffer->getImgSize();
    MY_LOGD("Frame %d mvIn[%d] idx=%d size=(%d,%d)", request->mRequestNo, i, index, size.w, size.h);
  }
  for( unsigned i = 0, n = request->mQParams.mvOut.size(); i < n; ++i )
  {
    unsigned index = request->mQParams.mvOut[i].mPortID.index;
    MSize size = request->mQParams.mvOut[i].mBuffer->getImgSize();
    MBOOL isGraphic = (getGraphicBufferAddr(request->mQParams.mvOut[i].mBuffer) != NULL);
    MINT fmt = request->mQParams.mvOut[i].mBuffer->getImgFormat();
    MY_LOGD("Frame %d mvOut[%d] idx=%d size=(%d,%d) fmt=%d, isGraphic=%d", request->mRequestNo, i, index, size.w, size.h, fmt, isGraphic);
  }
}

MBOOL P2ANode::init3A()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MVOID P2ANode::uninit3A()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MBOOL P2ANode::prepare3A(NSCam::NSIoPipe::NSPostProc::QParams &params, const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  (void)params;
  (void)request;
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::initEIS()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MVOID P2ANode::uninitEIS()
{
}

MBOOL P2ANode::prepareEIS(NSCam::NSIoPipe::NSPostProc::QParams &params, const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  (void)params;
  EIS_HAL_CONFIG_DATA eisHalConfigData;
  handleData(ID_P2A_TO_EIS_CONFIG, EisConfigData(eisHalConfigData, request));
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::init3DNR()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MVOID P2ANode::uninit3DNR()
{
  TRACE_FUNC_ENTER();
  mPrevFullImg = NULL;
  TRACE_FUNC_EXIT();
}

MBOOL P2ANode::prepare3DNR(NSCam::NSIoPipe::NSPostProc::QParams &params, const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  (void)params;
  (void)request;
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL P2ANode::initVHDR()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MVOID P2ANode::uninitVHDR()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MBOOL P2ANode::prepareVHDR(NSCam::NSIoPipe::NSPostProc::QParams &params, const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  (void)params;
  (void)request;
  TRACE_FUNC_EXIT();
  return MTRUE;
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
