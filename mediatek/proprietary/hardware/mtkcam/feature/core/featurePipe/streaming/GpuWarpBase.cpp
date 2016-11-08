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

#include "GpuWarpBase.h"

#include "DebugControl.h"
#define PIPE_CLASS_TAG "GpuWarpBase"
#define PIPE_TRACE TRACE_GPU_WARP
#include <featurePipe/core/include/PipeLog.h>

using android::GraphicBuffer;
using android::Mutex;
using android::sp;

namespace NSCam {
namespace NSCamFeature {

GpuWarpBase::GpuWarpBase()
  : mStage(STAGE_IDLE)
  , mFeature(0)
  , mMaxImage(0, 0)
  , mMaxWarp(0, 0)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

GpuWarpBase::~GpuWarpBase()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MBOOL GpuWarpBase::init(const MSize &maxImageSize, const MSize &maxWarpSize, MUINT32 feature)
{
  TRACE_FUNC_ENTER();
  Mutex::Autolock lock(mMutex);

  if( mStage != STAGE_IDLE )
  {
    MY_LOGE("Invalid state(%d)", mStage);
  }
  else if( !(maxImageSize.w && maxImageSize.h &&
             maxWarpSize.w && maxWarpSize.h) )
  {
    MY_LOGE("Invalid config: feature=0x%x maxImage=(%d,%d), maxWarp=(%d,%d)", feature, maxImageSize.w, maxImageSize.h, maxWarpSize.w, maxWarpSize.h);
  }
  else
  {
    mMaxImage = maxImageSize;
    mMaxWarp = maxWarpSize;
    mStage = STAGE_INIT;
  }
  TRACE_FUNC_EXIT();
  return (mStage == STAGE_INIT);
}

MBOOL GpuWarpBase::uninit()
{
  TRACE_FUNC_ENTER();
  Mutex::Autolock lock(mMutex);
  if( mStage == STAGE_CONFIG )
  {
    updateConfig(GB_PTR_ARRAY(), GB_PTR_ARRAY(), DEFAULT_FEATURE);
    this->onReset();
  }
  if( mStage == STAGE_INIT || mStage == STAGE_CONFIG )
  {
    mFeature = 0;
    mMaxImage.w = mMaxImage.h = 0;
    mMaxWarp.w = mMaxWarp.h = 0;
    mStage = STAGE_IDLE;
  }
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL GpuWarpBase::config(const GB_PTR_ARRAY &in, const GB_PTR_ARRAY &out, MUINT32 feature)
{
  TRACE_FUNC_ENTER();
  Mutex::Autolock lock(mMutex);
  if( mStage != STAGE_INIT && mStage != STAGE_CONFIG )
  {
    MY_LOGW("Invalid state(%d)", mStage);
  }
  else if( needConfig(in, out, feature) )
  {
    this->updateConfig(in, out, feature);
    if( this->onConfig(mFeature, mInGB, mOutGB, mMaxImage, mMaxWarp) )
    {
      mStage = STAGE_CONFIG;
    }
    else
    {
      updateConfig(GB_PTR_ARRAY(), GB_PTR_ARRAY(), DEFAULT_FEATURE);
      mStage = STAGE_INIT;
    }
  }
  TRACE_FUNC_EXIT();
  return (mStage == STAGE_CONFIG);
}

MBOOL GpuWarpBase::reset()
{
  TRACE_FUNC_ENTER();
  Mutex::Autolock lock(mMutex);
  if( mStage == STAGE_CONFIG )
  {
    updateConfig(GB_PTR_ARRAY(), GB_PTR_ARRAY(), DEFAULT_FEATURE);
    this->onReset();
    mStage = STAGE_INIT;
  }
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL GpuWarpBase::needConfig(const GB_PTR_ARRAY &in, const GB_PTR_ARRAY &out, MUINT32 feature)
{
  TRACE_FUNC_ENTER();
  MBOOL changed = MFALSE;
  changed = changed || (mFeature != feature);
  changed = changed || !isSubset(mInGB, in);
  changed = changed || !isSubset(mOutGB, out);
  TRACE_FUNC_EXIT();
  return changed;
}

MVOID GpuWarpBase::updateConfig(const GB_PTR_ARRAY &in, const GB_PTR_ARRAY &out, MUINT32 feature)
{
  TRACE_FUNC_ENTER();
  mFeature = feature;
  mInGB = in;
  mOutGB = out;
  TRACE_FUNC_EXIT();
}

MBOOL GpuWarpBase::isSubset(const GB_PTR_ARRAY &whole, const GB_PTR_ARRAY &part)
{
  TRACE_FUNC_ENTER();
  MBOOL subset = MTRUE;
  GB_PTR_ARRAY::const_iterator wholeBegin, wholeEnd, wholeIt;
  GB_PTR_ARRAY::const_iterator partBegin, partEnd, partIt;
  wholeBegin = whole.begin();
  wholeEnd = whole.end();
  partBegin = part.begin();
  partEnd = part.end();

  for( partIt = partBegin; partIt != partEnd; ++partIt )
  {
    for( wholeIt = wholeBegin; wholeIt != wholeEnd; ++wholeIt )
    {
      if( *wholeIt == *partIt )
      {
        break;
      }
    }
    if( partIt == partEnd )
    {
      subset = MFALSE;
      break;
    }
  }

  TRACE_FUNC_EXIT();
  return subset;
}

MBOOL GpuWarpBase::process(GB_PTR in, GB_PTR out, IImageBuffer *warpMap, const MSize &inSize, const MSize &outSize, MBOOL passThrough)
{
  TRACE_FUNC_ENTER();
  Mutex::Autolock lock(mMutex);
  MBOOL ret = MFALSE;
  if( mStage == STAGE_CONFIG )
  {
    if( checkProcessParam(in, out, warpMap, inSize, outSize) )
    {
      ret = this->onProcess(in, out, warpMap, inSize, outSize, passThrough);
    }
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MUINT32 GpuWarpBase::getDefaultFeature()
{
  TRACE_FUNC_ENTER();
  MUINT32 feature = DEFAULT_FEATURE;
  TRACE_FUNC_EXIT();
  return feature;
}

MUINT32 GpuWarpBase::toggleVFB(MUINT32 ori, MBOOL value)
{
  TRACE_FUNC_ENTER();
  if( value )
  {
    ori |= FEATURE_VFB;
  }
  else
  {
    ori &= ~FEATURE_VFB;
  }
  TRACE_FUNC_EXIT();
  return ori;
}

MBOOL GpuWarpBase::hasVFB(MUINT32 feature)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = feature & FEATURE_VFB;
  TRACE_FUNC_EXIT();
  return ret;
}

MUINT32 GpuWarpBase::toggleEIS(MUINT32 ori, MBOOL value)
{
  TRACE_FUNC_ENTER();
  if( value )
  {
    ori |= FEATURE_EIS2;
  }
  else
  {
    ori &= ~FEATURE_EIS2;
  }
  TRACE_FUNC_EXIT();
  return ori;
}

MBOOL GpuWarpBase::hasEIS(MUINT32 feature)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = feature & FEATURE_EIS2;
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL GpuWarpBase::makePassThroughWarp(const sp<IImageBuffer> &buffer, MSize size)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  if( buffer != NULL )
  {
    char *va = NULL;
    MUINT32 *ptr = NULL;
    buffer->setExtParam(MSize(2*4, 2*2));
    va = (char*)buffer->getBufVA(0);
    ptr = (MUINT32*)va;
    ptr[0] = 0;
    ptr[1] = (size.w-1) * 16;
    ptr[2] = 0;
    ptr[3] = (size.w-1) * 16;
    va += buffer->getBufSizeInBytes(0)/2;
    ptr = (MUINT32*)va;
    ptr[0] = 0;
    ptr[1] = 0;
    ptr[2] = (size.h-1) * 16;
    ptr[3] = (size.h-1) * 16;
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL GpuWarpBase::checkProcessParam(GB_PTR in, GB_PTR out, IImageBuffer *warpMap, const MSize &inSize, const MSize &outSize)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  if( in == NULL )
  {
    MY_LOGE("invalid input buffer handle addr");
  }
  else if( in->get() == NULL )
  {
    MY_LOGE("invalid input buffer handle value");
  }
  else if( out == NULL )
  {
    MY_LOGE("invalid output buffer handle addr");
  }
  else if( out->get() == NULL )
  {
    MY_LOGE("invalid output buffer handle value");
  }
  else if( warpMap == NULL )
  {
    MY_LOGE("invalid warpMap handle");
  }
  else if( inSize.w <= 0 || inSize.h <= 0 ||
           inSize.w > mMaxImage.w || inSize.h > mMaxImage.h )
  {
    MY_LOGE("invalid input size");
  }
  else if( outSize.w <= 0 || outSize.h <= 0 ||
           outSize.w > mMaxImage.w || outSize.h > mMaxImage.h )
  {
    MY_LOGE("invalid output size");
  }
  else
  {
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

} // namespace NSCamFeature
} // namespace NSCam
