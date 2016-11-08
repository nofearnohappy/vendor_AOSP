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

#include "FDNode.h"

#define PIPE_CLASS_TAG "FDNode"
#define PIPE_TRACE TRACE_FD_NODE
#include <featurePipe/core/include/PipeLog.h>

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

FDNode::FDNode(const char *name)
  : StreamingFeatureNode(name)
{
  TRACE_FUNC_ENTER();
  this->addWaitQueue(&mDsImgDatas);
  TRACE_FUNC_EXIT();
}

FDNode::~FDNode()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

MBOOL FDNode::onData(DataID id, const ImgBufferData &data)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC("Frame %d: %s arrived", data.mRequest->mRequestNo, ID2Name(id));
  MBOOL ret = MFALSE;
  if( id == ID_P2A_TO_FD_DSIMG )
  {
    mDsImgDatas.enque(data);
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL FDNode::onInit()
{
  TRACE_FUNC_ENTER();
  StreamingFeatureNode::onInit();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL FDNode::onUninit()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL FDNode::onThreadStart()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL FDNode::onThreadStop()
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL FDNode::onThreadLoop()
{
  TRACE_FUNC("Waitloop");
  RequestPtr request;
  ImgBufferData dsImgData;
  if( !waitAllQueue() )
  {
    return MFALSE;
  }
  if( !mDsImgDatas.deque(dsImgData) )
  {
    MY_LOGE("DsImgData deque out of sync");
    return MFALSE;
  }
  if( dsImgData.mRequest == NULL )
  {
    MY_LOGE("Request out of sync");
    return MFALSE;
  }
  TRACE_FUNC_ENTER();
  request = dsImgData.mRequest;
  request->mTimer.startFD();
  TRACE_FUNC("Frame %d in FD", request->mRequestNo);
  processFD(request, dsImgData.mData);
  request->mTimer.stopFD();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL FDNode::processFD(const RequestPtr &request, const ImgBuffer &dsImg)
{
  TRACE_FUNC_ENTER();
  MtkCameraFaceMetadata face;
  handleData(ID_FD_TO_VFB_FACE, FaceData(face, request));
  request->doExtCallback(FeaturePipeParam::MSG_FD_DONE);
  TRACE_FUNC_EXIT();
  return MTRUE;
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
