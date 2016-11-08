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

#include "TestNodeD.h"
#include "TestPipeRule.h"

#define PIPE_MODULE_TAG "FeaturePipeTest"
#define PIPE_CLASS_TAG "TestNodeD"
#include <featurePipe/core/include/PipeLog.h>

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

TestNodeD::TestNodeD(const char *name, Graph_T *graph)
  : TestNode(name, graph)
{
  this->addWaitQueue(&mBQueue);
  this->addWaitQueue(&mCQueue);
}

TestNodeD::~TestNodeD()
{
}

MBOOL TestNodeD::onInit()
{
  return MTRUE;
}

MBOOL TestNodeD::onUninit()
{
  return MTRUE;
}

MBOOL TestNodeD::onThreadStart()
{
  return MTRUE;
}

MBOOL TestNodeD::onThreadStop()
{
  return MTRUE;
}

MBOOL TestNodeD::onData(DataID data, TestBuffer &buffer)
{
  TRACE_FUNC_ENTER();

  MBOOL ret;
  switch(data)
  {
  case B2D:
    mBQueue.enque(buffer);
    ret = MTRUE;
    break;
  case C2D:
    mCQueue.enque(buffer);
    ret = MTRUE;
    break;
  default:
    ret = MFALSE;
    break;
  }

  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL TestNodeD::onThreadLoop()
{
  int result;
  TestBuffer bufferB, bufferC;
  android::sp<TestRequest> request;

  MY_LOGD("prepare waitAllQueue()");
  if( !waitAllQueue() )
  {
    MY_LOGD("waitAllQueue failed");
    return MFALSE;
  }

  MY_LOGD("prepare mBQueue.deque() && mCQueue.deque()");
  if( !mBQueue.deque(bufferB) || !mCQueue.deque(bufferC) )
  {
    MY_LOGD("mBQueue.deque() && mCQueue.deque() failed");
    return MFALSE;
  }

  this->simulateDelay();

  result = Func3(bufferB.mData, bufferC.mData);
  if( (request = bufferB.mRequest) != NULL )
  {
    request->mOutD = result;
  }
  handleData(D_OUT, request);
  return MTRUE;
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
