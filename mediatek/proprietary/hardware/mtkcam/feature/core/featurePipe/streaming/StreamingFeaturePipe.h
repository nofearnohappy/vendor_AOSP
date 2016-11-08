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

#ifndef _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_PIPE_H_
#define _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_PIPE_H_

#include <list>

#include <featurePipe/core/include/CamPipe.h>
#include "StreamingFeatureNode.h"

#include "P2ANode.h"
#include "P2BNode.h"
#include "GPUNode.h"
#include "FDNode.h"
#include "MDPNode.h"
#include "VFBNode.h"
#include "EISNode.h"

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

class StreamingFeaturePipe : public CamPipe<StreamingFeatureNode>, public StreamingFeatureNode::Handler_T, public IStreamingFeaturePipe
{
public:
  StreamingFeaturePipe(MUINT32 sensorIndex);
  virtual ~StreamingFeaturePipe();

public:
  // IStreamingFeaturePipe Members
  virtual void setSensorIndex(MUINT32 sensorIndex);
  virtual MBOOL init();
  virtual MBOOL uninit();
  virtual MBOOL enque(const FeaturePipeParam &param);

protected:
  typedef CamPipe<StreamingFeatureNode> PARENT_PIPE;
  virtual MBOOL onInit();
  virtual MVOID onUninit();

protected:
  virtual MBOOL onData(DataID id, const RequestPtr &data);

private:
  MBOOL prepareDebugSetting();
  MBOOL prepareGeneralPipe();
  MBOOL prepareNodeSetting();
  MBOOL prepareNodeConnection();
  MBOOL prepareBuffer();

  MVOID releaseGeneralPipe();
  MVOID releaseNodeSetting();
  MVOID releaseBuffer();

  MVOID applyMaskOverride(const RequestPtr &request);
  MVOID applyVarMapOverride(const RequestPtr &request);

private:
  MUINT32 mForceOnMask;
  MUINT32 mForceOffMask;
  MUINT32 mSensorIndex;
  MUINT32 mCounter;

  P2ANode mP2A;
  P2BNode mP2B;
  GPUNode mGPU;
  FDNode mFD;
  MDPNode mMDP;
  VFBNode mVFB;
  EISNode mEIS;

  android::sp<ImageBufferPool> mDsImgPool;
  android::sp<GraphicBufferPool> mFullImgPool;
  android::sp<GraphicBufferPool> mGpuOutputPool;

  NSCam::NSIoPipe::NSPostProc::INormalStream *mNormalStream;

  typedef std::list<StreamingFeatureNode*> NODE_LIST;
  NODE_LIST mNodes;
};

} // NSFeaturePipe
} // NSCamFeature
} // NSCam

#endif // _MTK_CAMERA_STREAMING_FEATURE_PIPE_STREAMING_FEATURE_PIPE_H_
