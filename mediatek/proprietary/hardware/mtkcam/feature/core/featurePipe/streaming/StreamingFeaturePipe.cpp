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

#include "StreamingFeaturePipe.h"

#define PIPE_CLASS_TAG "Pipe"
#define PIPE_TRACE TRACE_STREAMING_FEATURE_PIPE
#include <featurePipe/core/include/PipeLog.h>

using namespace NSCam::NSIoPipe::NSPostProc;

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

StreamingFeaturePipe::StreamingFeaturePipe(MUINT32 sensorIndex)
  : CamPipe<StreamingFeatureNode>("StreamingFeaturePipe")
  , mForceOnMask(0)
  , mForceOffMask(~0)
  , mSensorIndex(sensorIndex)
  , mCounter(0)
  , mP2A("fpipe.p2a")
  , mP2B("fpipe.p2b")
  , mGPU("fpipe.gpu")
  , mFD("fpipe.fd")
  , mMDP("fpipe.mdp")
  , mVFB("fpipe.vfb")
  , mEIS("fpipe.eis")
  , mNormalStream(NULL)
{
  TRACE_FUNC_ENTER();
  mNodes.push_back(&mP2A);
  mNodes.push_back(&mP2B);
  mNodes.push_back(&mGPU);
  mNodes.push_back(&mFD);
  mNodes.push_back(&mMDP);
  mNodes.push_back(&mVFB);
  mNodes.push_back(&mEIS);
  TRACE_FUNC_EXIT();
}

StreamingFeaturePipe::~StreamingFeaturePipe()
{
  TRACE_FUNC_ENTER();
  // must call dispose to free CamGraph
  this->dispose();
  TRACE_FUNC_EXIT();
}

void StreamingFeaturePipe::setSensorIndex(MUINT32 sensorIndex)
{
  TRACE_FUNC_ENTER();
  this->mSensorIndex = sensorIndex;
  TRACE_FUNC_EXIT();
}

MBOOL StreamingFeaturePipe::init()
{
  TRACE_FUNC_ENTER();
  MBOOL ret;
  ret = PARENT_PIPE::init();
  mCounter = 0;
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL StreamingFeaturePipe::uninit()
{
  TRACE_FUNC_ENTER();
  MBOOL ret;
  ret = PARENT_PIPE::uninit();
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL StreamingFeaturePipe::enque(const FeaturePipeParam &param)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  RequestPtr request;
  request = new StreamingFeatureRequest(param, ++mCounter);
  if(request == NULL)
  {
    MY_LOGE("OOM: Cannot allocate StreamingFeatureRequest");
  }
  else
  {
    #if defined(DEV_PER_FRAME_PROPERTY) && (DEV_PER_FRAME_PROPERTY == 1)
    {
      this->prepareDebugSetting();
    }
    #endif

    this->applyMaskOverride(request);
    this->applyVarMapOverride(request);
    ret = CamPipe::enque(ID_ROOT_ENQUE, request);
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL StreamingFeaturePipe::onInit()
{
  TRACE_FUNC_ENTER();
  MBOOL ret;
  ret = this->prepareDebugSetting() &&
        this->prepareGeneralPipe() &&
        this->prepareNodeSetting() &&
        this->prepareNodeConnection() &&
        this->prepareBuffer();

  TRACE_FUNC_EXIT();
  return ret;
}

MVOID StreamingFeaturePipe::onUninit()
{
  TRACE_FUNC_ENTER();
  this->releaseBuffer();
  this->releaseNodeSetting();
  this->releaseGeneralPipe();
  TRACE_FUNC_EXIT();
}

MBOOL StreamingFeaturePipe::onData(DataID, const RequestPtr &)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL StreamingFeaturePipe::prepareDebugSetting()
{
  TRACE_FUNC_ENTER();
  MINT32 forceEIS;
  MINT32 forceEIS_FU;
  MINT32 forceVFB;
  MINT32 forceVFB_EX;
  MINT32 force3DNR;
  MINT32 forceVHDR;
  MINT32 prop;

  forceEIS = getPropertyValue(KEY_OVERRIDE_EIS, DEFAULT_EIS_OVERRIDE);
  forceEIS_FU = getPropertyValue(KEY_OVERRIDE_EIS_FU, DEFAULT_EIS_FU_OVERRIDE);
  forceVFB = getPropertyValue(KEY_OVERRIDE_VFB, DEFAULT_VFB_OVERRIDE);
  forceVFB_EX = getPropertyValue(KEY_OVERRIDE_VFB_EX, DEFAULT_VFB_EX_OVERRIDE);
  force3DNR = getPropertyValue(KEY_OVERRIDE_3DNR, DEFAULT_3DNR_OVERRIDE);
  forceVHDR = getPropertyValue(KEY_OVERRIDE_VHDR, DEFAULT_VHDR_OVERRIDE);

  mForceOnMask = 0;
  mForceOffMask = ~0;

  if( forceEIS == FORCE_ON )    ENABLE_EIS(mForceOnMask);
  if( forceEIS_FU == FORCE_ON ) ENABLE_EIS_FU(mForceOnMask);
  if( forceVFB == FORCE_ON )    ENABLE_VFB(mForceOnMask);
  if( forceVFB_EX == FORCE_ON ) ENABLE_VFB_EX(mForceOnMask);
  if( force3DNR == FORCE_ON )   ENABLE_3DNR(mForceOnMask);
  if( forceVHDR == FORCE_ON )   ENABLE_VHDR(mForceOnMask);

  if( forceEIS == FORCE_OFF)    DISABLE_EIS(mForceOffMask);
  if( forceEIS_FU == FORCE_OFF) DISABLE_EIS_FU(mForceOffMask);
  if( forceVFB == FORCE_OFF)    DISABLE_VFB(mForceOffMask);
  if( forceVFB_EX == FORCE_OFF) DISABLE_VFB_EX(mForceOffMask);
  if( force3DNR == FORCE_OFF)   DISABLE_3DNR(mForceOffMask);
  if( forceVHDR == FORCE_OFF)   DISABLE_VHDR(mForceOffMask);

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL StreamingFeaturePipe::prepareGeneralPipe()
{
  TRACE_FUNC_ENTER();
  mNormalStream = INormalStream::createInstance(mSensorIndex);
  if( mNormalStream != NULL )
  {
    mNormalStream->init();
  }
  mP2A.setNormalStream(mNormalStream);
  TRACE_FUNC_EXIT();
  return (mNormalStream != NULL);
}

MBOOL StreamingFeaturePipe::prepareNodeSetting()
{
  TRACE_FUNC_ENTER();
  NODE_LIST::iterator it, end;
  for( it = mNodes.begin(), end = mNodes.end(); it != end; ++it )
  {
    (*it)->setSensorIndex(mSensorIndex);
  }

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL StreamingFeaturePipe::prepareNodeConnection()
{
  TRACE_FUNC_ENTER();

  this->connectData(ID_P2A_TO_GPU_FULLIMG, mP2A, mGPU);
  this->connectData(ID_GPU_TO_MDP_FULLIMG, mGPU, mMDP);

  // VFB nodes
  this->connectData(ID_P2A_TO_FD_DSIMG, mP2A, mFD);
  this->connectData(ID_P2A_TO_VFB_DSIMG, mP2A, mVFB);
  this->connectData(ID_P2A_TO_P2B_FULLIMG, mP2A, mP2B);
  this->connectData(ID_FD_TO_VFB_FACE, mFD, mVFB);
  this->connectData(ID_VFB_TO_P2B, mVFB, mP2B);
  this->connectData(ID_VFB_TO_GPU_WARP, mVFB, mGPU);
  this->connectData(ID_MDP_TO_P2B_FULLIMG, mMDP, mP2B);

  // EIS nodes
  this->connectData(ID_P2A_TO_EIS_CONFIG, mP2A, mEIS);
  this->connectData(ID_P2A_TO_EIS_P2DONE, mP2A, mEIS);
  this->connectData(ID_EIS_TO_GPU_WARP, mEIS, mGPU);

  // EIS + VFB
  this->connectData(ID_EIS_TO_VFB_WARP, mEIS, mVFB);

  this->setRootNode(&mP2A);
  mP2A.registerInputDataID(ID_ROOT_ENQUE);

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL StreamingFeaturePipe::prepareBuffer()
{
  TRACE_FUNC_ENTER();
  mDsImgPool = ImageBufferPool::create("SFPipe.dsImg", DS_IMAGE_WIDTH, DS_IMAGE_HEIGHT, eImgFmt_YUY2, ImageBufferPool::USAGE_HW );
  mFullImgPool = GraphicBufferPool::create("SFPipe.fullImg", MAX_FULL_WIDTH, MAX_FULL_HEIGHT, HAL_PIXEL_FORMAT_YV12, GraphicBufferPool::USAGE_HW_TEXTURE);
  mGpuOutputPool = GraphicBufferPool::create("SFPipe.gpuOut", MAX_FULL_WIDTH, MAX_FULL_HEIGHT, android::PIXEL_FORMAT_RGBA_8888, GraphicBufferPool::USAGE_HW_RENDER);

  mDsImgPool->allocate(NUM_DEFAULT_BUFFER);
  mFullImgPool->allocate(NUM_DEFAULT_BUFFER);
  mGpuOutputPool->allocate(NUM_DEFAULT_BUFFER);

  mP2A.setDsImgPool(mDsImgPool);
  mP2A.setFullImgPool(mFullImgPool);
  mGPU.setInputBufferPool(mFullImgPool);
  mGPU.setOutputBufferPool(mGpuOutputPool);

  TRACE_FUNC_EXIT();
  return MTRUE;
}

MVOID StreamingFeaturePipe::releaseNodeSetting()
{
  TRACE_FUNC_ENTER();
  this->disconnect();
  TRACE_FUNC_EXIT();
}

MVOID StreamingFeaturePipe::releaseGeneralPipe()
{
  TRACE_FUNC_ENTER();
  mP2A.setNormalStream(NULL);
  if( mNormalStream )
  {
    mNormalStream->uninit();
    mNormalStream->destroyInstance("StreamingFeaturePipe");
  }
  TRACE_FUNC_EXIT();
}

MVOID StreamingFeaturePipe::releaseBuffer()
{
  TRACE_FUNC_ENTER();

  mP2A.setDsImgPool(NULL);
  mP2A.setFullImgPool(NULL);
  mGPU.setInputBufferPool(NULL);
  mGPU.setOutputBufferPool(NULL);

  ImageBufferPool::destroy(mDsImgPool);
  GraphicBufferPool::destroy(mFullImgPool);
  GraphicBufferPool::destroy(mGpuOutputPool);

  TRACE_FUNC_EXIT();
}

MVOID StreamingFeaturePipe::applyMaskOverride(const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  request->mFeatureMask |= mForceOnMask;
  request->mFeatureMask &= mForceOffMask;
  TRACE_FUNC_EXIT();
}

MVOID StreamingFeaturePipe::applyVarMapOverride(const RequestPtr &request)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

} // NSFeaturePipe
} // NSCamFeature
} // NSCam
