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

#include "StreamingFeatureData.h"

using NSCam::NSIoPipe::NSPostProc::QParams;

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

StreamingFeatureRequest::StreamingFeatureRequest(const FeaturePipeParam &extParam, MUINT32 requestNo)
  : mExtParam(extParam)
  , mFeatureMask(extParam.mFeatureMask)
  , mRequestNo(requestNo)
  , mVarMap(mExtParam.mVarMap)
  , mQParams(mExtParam.mQParams)
  , mvIn(mQParams.mvIn)
  , mvOut(mQParams.mvOut)
  , mvCropRsInfo(mQParams.mvCropRsInfo)
  , mResult(MTRUE)
{
  mQParams.mDequeSuccess = MFALSE;
  mTimer.start();
}

StreamingFeatureRequest::~StreamingFeatureRequest()
{
  mTimer.print(mRequestNo);
}

MBOOL StreamingFeatureRequest::updateResult(MBOOL result)
{
  mResult = (mResult && result);
  mQParams.mDequeSuccess = mResult;
  return mResult;
}

MBOOL StreamingFeatureRequest::doExtCallback(FeaturePipeParam::MSG_TYPE msg)
{
  MBOOL ret = MFALSE;
  if( msg == FeaturePipeParam::MSG_FRAME_DONE )
  {
    mTimer.stop();
  }
  if( mExtParam.mCallback )
  {
    ret = mExtParam.mCallback(msg, mExtParam);
  }
  return ret;
}

MSize StreamingFeatureRequest::getMaxOutSize() const
{
  MSize maxSize = MSize(0, 0);
  MUINT32 max = 0;
  for( unsigned i = 0, count = mQParams.mvOut.size(); i < count; ++i )
  {
    MSize size = mQParams.mvOut[i].mBuffer->getImgSize();
    MUINT32 temp = size.w * size.h;
    if( temp > max )
    {
      maxSize = size;
      max = temp;
    }
  }
  return maxSize;
}

MBOOL StreamingFeatureRequest::need3DNR() const
{
  return HAS_3DNR(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needVHDR() const
{
  return HAS_VHDR(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needVFB() const
{
  return HAS_VFB(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needExVFB() const
{
  return HAS_VFB_EX(mFeatureMask) && HAS_VFB(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needEIS() const
{
  return HAS_EIS(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needFutureEIS() const
{
  return HAS_EIS_FU(mFeatureMask) && HAS_EIS(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needGPU() const
{
  return HAS_EIS(mFeatureMask) ||
         (HAS_VFB_EX(mFeatureMask) && HAS_VFB(mFeatureMask));
}

MBOOL StreamingFeatureRequest::needIMG3O() const
{
  return HAS_3DNR(mFeatureMask) || HAS_VFB(mFeatureMask) || HAS_EIS(mFeatureMask);
}

MBOOL StreamingFeatureRequest::needDsImg() const
{
  return HAS_VFB(mFeatureMask);
}

MBOOL StreamingFeatureRequest::isLastNodeP2A() const
{
  return !HAS_EIS(mFeatureMask) && !HAS_VFB(mFeatureMask);
}

VFBResult::VFBResult()
{
}

VFBResult::VFBResult(const ImgBuffer &dsImg, const ImgBuffer &alphaCL, const ImgBuffer &alphaNR, const ImgBuffer &pca)
  : mDsImg(dsImg)
  , mAlphaCL(alphaCL)
  , mAlphaNR(alphaNR)
  , mPCA(pca)
{
}

} // NSFeaturePipe
} // NSCamFeature
} // NSCam
