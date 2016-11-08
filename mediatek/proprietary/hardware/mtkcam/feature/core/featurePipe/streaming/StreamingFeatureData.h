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

#ifndef _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_DATA_H_
#define _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_DATA_H_

#include "MtkHeader.h"
//#include <mtkcam/feature/featurePipe/IStreamingFeaturePipe.h>
//#include <mtkcam/common/faces.h>
//#include <mtkcam/featureio/eis_type.h>

#include <utils/RefBase.h>
#include <utils/Vector.h>

#include <featurePipe/core/include/WaitQueue.h>
#include <featurePipe/core/include/ImageBufferPool.h>
#include <featurePipe/core/include/GraphicBufferPool.h>

#include "StreamingFeatureTimer.h"

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

class StreamingFeatureRequest : public virtual android::RefBase
{
private:
  // must allocate extParam before everything else
  FeaturePipeParam mExtParam;

public:
  MUINT32 mFeatureMask;
  MUINT32 mRequestNo;
  StreamingFeatureTimer mTimer;
  NSCam::NSIoPipe::NSPostProc::QParams mP2QParams;

  // alias members, do not change initialize order
  VarMap &mVarMap;
  NSCam::NSIoPipe::NSPostProc::QParams &mQParams;
  android::Vector<NSCam::NSIoPipe::NSPostProc::Input> &mvIn;
  android::Vector<NSCam::NSIoPipe::NSPostProc::Output> &mvOut;
  android::Vector<NSCam::NSIoPipe::NSPostProc::MCrpRsInfo> &mvCropRsInfo;

public:
  StreamingFeatureRequest(const FeaturePipeParam &extParam, MUINT32 requestNo);
  ~StreamingFeatureRequest();

  MBOOL updateResult(MBOOL result);
  MBOOL doExtCallback(FeaturePipeParam::MSG_TYPE msg);

  MSize getMaxOutSize() const;

  DECLARE_VAR_MAP_INTERFACE(mVarMap, setVar, getVar, tryGetVar);

  MBOOL need3DNR() const;
  MBOOL needVHDR() const;
  MBOOL needVFB() const;
  MBOOL needExVFB() const;
  MBOOL needEIS() const;
  MBOOL needFutureEIS() const;
  MBOOL needGPU() const;
  MBOOL needIMG3O() const;
  MBOOL needDsImg() const;
  MBOOL isLastNodeP2A() const;

private:
  MBOOL mResult;
};
typedef android::sp<StreamingFeatureRequest> RequestPtr;
typedef android::sp<IIBuffer> ImgBuffer;

template <typename T>
class Data
{
public:
  T mData;
  RequestPtr mRequest;

  // lower value will be process first
  MUINT32 mPriority;

  Data() {}

  virtual ~Data() {}

  Data(const T &data, const RequestPtr &request, MINT32 nice = 0)
    : mData(data)
    , mRequest(request)
    , mPriority(request->mRequestNo)
  {
    if( nice > 0 )
    {
      // TODO: watch out for overflow
      mPriority += nice;
    }
  }

  T& operator->()
  {
    return mData;
  }

  const T& operator->() const
  {
    return mData;
  }

  class IndexConverter
  {
  public:
    IWaitQueue::Index operator()(const Data &data) const
    {
      return IWaitQueue::Index(data.mRequest->mRequestNo,
                               data.mPriority);
    }
    static unsigned getID(const Data &data)
    {
      return data.mRequest->mRequestNo;
    }
    static unsigned getPriority(const Data &data)
    {
      return data.mPriority;
    }
  };
};

class VFBResult
{
public:
  ImgBuffer mDsImg;
  ImgBuffer mAlphaCL;
  ImgBuffer mAlphaNR;
  ImgBuffer mPCA;

  VFBResult();
  VFBResult(const ImgBuffer &dsImg, const ImgBuffer &alphaCL, const ImgBuffer &alphaNR, const ImgBuffer &pca);
};

typedef Data<ImgBuffer> ImgBufferData;
typedef Data<EIS_HAL_CONFIG_DATA> EisConfigData;
typedef Data<MtkCameraFaceMetadata> FaceData;
typedef Data<VFBResult> VFBData;

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam

#endif // _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_DATA_H_
