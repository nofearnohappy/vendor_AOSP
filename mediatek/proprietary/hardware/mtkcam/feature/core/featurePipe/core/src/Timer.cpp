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

#include "../include/Timer.h"

#include "../include/DebugControl.h"
#define PIPE_TRACE TRACE_TIMER
#define PIPE_CLASS_TAG "Timer"
#include "../include/PipeLog.h"

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

Timer::Timer()
  : mStart()
  , mStop()
{
}

MVOID Timer::start() const
{
  TRACE_FUNC_ENTER();
  clock_gettime(CLOCK_MONOTONIC, &mStart);
  TRACE_FUNC_EXIT();
}

MVOID Timer::stop() const
{
  TRACE_FUNC_ENTER();
  clock_gettime(CLOCK_MONOTONIC, &mStop);
  TRACE_FUNC_EXIT();
}

timespec Timer::getStart() const
{
  return mStart;
}

timespec Timer::getStop() const
{
  return mStop;
}

MUINT32 Timer::getElapsed() const
{
  return diff(mStart, mStop);
}

MUINT32 Timer::diff(const timespec &from, const timespec &to)
{
  TRACE_FUNC_ENTER();
  MUINT32 diff = 0;
  if( to.tv_sec && to.tv_nsec &&
      from.tv_sec && from.tv_nsec )
  {
    diff = ((to.tv_sec - from.tv_sec) * 1000) +
           ((to.tv_nsec - from.tv_nsec) / 1000000);
  }
  TRACE_FUNC_EXIT();
  return diff;
}

}; // namespace NSFeaturePipe
}; // namespace NSCamFeature
}; // namespace NSCam
