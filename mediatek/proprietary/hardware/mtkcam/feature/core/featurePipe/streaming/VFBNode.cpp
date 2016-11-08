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

#include "VFBNode.h"
#include "GpuWarpBase.h"

#define PIPE_CLASS_TAG "VFBNode"
#define PIPE_TRACE TRACE_VFB_NODE
#include <featurePipe/core/include/PipeLog.h>

#define VFB_WARP_WIDTH        320
#define VFB_WARP_HEIGHT       320
#define VFB_WARP_FORMAT       eImgFmt_BAYER8

#define VFB_DSIMG_WIDTH       320
#define VFB_DSIMG_HEIGHT      320
#define VFB_DSIMG_FORMAT      eImgFmt_YUY2

#define VFB_ALPHA_CL_WIDTH    320
#define VFB_ALPHA_CL_HEIGHT   320
#define VFB_ALPHA_CL_FORMAT   eImgFmt_BAYER8

#define VFB_ALPHA_NR_WIDTH    320
#define VFB_ALPHA_NR_HEIGHT   320
#define VFB_ALPHA_NR_FORMAT   eImgFmt_BAYER8

#define VFB_PCA_WIDTH         160
#define VFB_PCA_HEIGHT        27
#define VFB_PCA_FORMAT        eImgFmt_BAYER8

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

VFBNode::VFBNode(const char *name)
  : StreamingFeatureNode(name)
{
  TRACE_FUNC_ENTER();
  this->addWaitQueue(&mP2ADatas);
  this->addWaitQueue(&mFaceDatas);
  this->addWaitQueue(&mExtWarpDatas);
  TRACE_FUNC_EXIT();
}

VFBNode::~VFBNode()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MBOOL VFBNode::onData(DataID id, const ImgBufferData &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data.mRequest->mRequestNo, ID2Name(id));
  MBOOL ret = MFALSE;
  if( id == ID_P2A_TO_VFB_DSIMG )
  {
    mP2ADatas.enque(data);
    // enque dummy warp in case of no EIS
    if( !data.mRequest->needEIS() )
    {
      mExtWarpDatas.enque(ImgBufferData(ImgBuffer(), data.mRequest));
    }
    ret = MTRUE;
  }
  else if( id == ID_EIS_TO_VFB_WARP )
  {
    mExtWarpDatas.enque(data);
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL VFBNode::onData(DataID id, const FaceData &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data.mRequest->mRequestNo, ID2Name(id));
  MBOOL ret = MFALSE;
  if( id == ID_FD_TO_VFB_FACE )
  {
    mFaceDatas.enque(data);
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL VFBNode::onInit()
{
  TRACE_FUNC_ENTER();
  StreamingFeatureNode::onInit();

  mWarpBufferPool = ImageBufferPool::create("VFB/warp",
                      VFB_WARP_WIDTH * 4, VFB_WARP_HEIGHT * 2,
                      VFB_WARP_FORMAT, ImageBufferPool::USAGE_SW);
  mDsImgBufferPool = ImageBufferPool::create("VFB/DS",
                      VFB_DSIMG_WIDTH, VFB_DSIMG_HEIGHT,
                      VFB_DSIMG_FORMAT, ImageBufferPool::USAGE_HW);
  mAlphaCLBufferPool = ImageBufferPool::create("VFB/CL",
                      VFB_ALPHA_CL_WIDTH, VFB_ALPHA_CL_HEIGHT,
                      VFB_ALPHA_CL_FORMAT, ImageBufferPool::USAGE_HW);
  mAlphaNRBufferPool = ImageBufferPool::create("VFB/NR",
                      VFB_ALPHA_NR_WIDTH, VFB_ALPHA_NR_HEIGHT,
                      VFB_ALPHA_NR_FORMAT, ImageBufferPool::USAGE_HW);
  mPCABufferPool = ImageBufferPool::create("VFB/PCA",
                      VFB_PCA_WIDTH, VFB_PCA_HEIGHT,
                      VFB_PCA_FORMAT, ImageBufferPool::USAGE_HW);

  mWarpBufferPool->allocate(3);
  mDsImgBufferPool->allocate(3);
  mAlphaCLBufferPool->allocate(3);
  mAlphaNRBufferPool->allocate(3);
  mPCABufferPool->allocate(3);

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL VFBNode::onUninit()
{
  TRACE_FUNC_ENTER();
  ImageBufferPool::destroy(mWarpBufferPool);
  ImageBufferPool::destroy(mDsImgBufferPool);
  ImageBufferPool::destroy(mAlphaCLBufferPool);
  ImageBufferPool::destroy(mAlphaNRBufferPool);
  ImageBufferPool::destroy(mPCABufferPool);
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL VFBNode::onThreadStart()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL VFBNode::onThreadStop()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL VFBNode::onThreadLoop()
{
  TRACE_FUNC("Waitloop");
  MUINT32 requestNo;
  RequestPtr request;
  ImgBufferData p2a;
  FaceData faceData;
  ImgBufferData extWarp;

  if( !waitAllQueueSync(requestNo) )
  {
    return MFALSE;
  }
  if( !mP2ADatas.deque(requestNo, p2a) )
  {
    MY_LOGE("P2AData deque out of sync: %d", requestNo);
    return MFALSE;
  }
  if( !mFaceDatas.deque(requestNo, faceData) )
  {
    MY_LOGE("FaceData deque out of sync: %d", requestNo);
    return MFALSE;
  }
  if( !mExtWarpDatas.deque(requestNo, extWarp) )
  {
    MY_LOGE("ExtWarpData deque out of sync: %d", requestNo);
    return MFALSE;
  }
  if( p2a.mRequest == NULL ||
      p2a.mRequest->mRequestNo != requestNo ||
      p2a.mRequest != faceData.mRequest ||
      p2a.mRequest != extWarp.mRequest )
  {
    MY_LOGE("Request out of sync");
    return MFALSE;
  }
  TRACE_FUNC_ENTER();

  request = p2a.mRequest;
  request->mTimer.startVFB();
  TRACE_FUNC("Frame %d in VFB", request->mRequestNo);
  processVFB(request, p2a.mData, faceData.mData, extWarp.mData);
  request->mTimer.stopVFB();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL VFBNode::processVFB(const RequestPtr &request, const ImgBuffer &p2aImg, const MtkCameraFaceMetadata &face, const ImgBuffer &extWarp)
{
  TRACE_FUNC_ENTER();
  ImgBuffer warp;
  ImgBuffer dsImg;
  ImgBuffer alphaCL;
  ImgBuffer alphaNR;
  ImgBuffer pca;

  warp = mWarpBufferPool->requestIIBuffer();
  {
    MSize fullSize = request->getMaxOutSize();
    GpuWarpBase::makePassThroughWarp(warp->getImageBuffer(), fullSize);
  }

  dsImg = mDsImgBufferPool->requestIIBuffer();
  alphaCL = mAlphaCLBufferPool->requestIIBuffer();
  alphaNR = mAlphaNRBufferPool->requestIIBuffer();
  pca = mPCABufferPool->requestIIBuffer();

  if( request->needExVFB() )
  {
  }

  // process here
  if( request->needGPU() )
  {
    handleData(ID_VFB_TO_GPU_WARP, ImgBufferData(warp, request));
  }
  handleData(ID_VFB_TO_P2B, VFBData(VFBResult(dsImg, alphaCL, alphaNR, pca), request));

  TRACE_FUNC_EXIT();
  return MTRUE;
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
