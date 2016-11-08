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

#include <gtest/gtest.h>
#include <featurePipe/core/include/CamThread.h>
#include "TestTool.h"

#define PIPE_TRACE 1
#define PIPE_MODULE_TAG "FeaturePipeTest"
#define PIPE_CLASS_TAG "TestThread"
#include <featurePipe/core/include/PipeLog.h>

namespace NSCam{
namespace NSCamFeature{
namespace NSFeaturePipe{

class TestThread : public CamThread
{
public:
  TestThread() : CamThread("TestThread")
  {
    mInitCounter = 0;
    mUninitCounter = 0;
    mUpdateCounter = 0;
    mTrigger = 0;
  }

  ~TestThread()
  {
    this->stopThread();
  }

  const char* getName() const
  {
    return "TestThreadName";
  }

  virtual MBOOL onThreadLoop()
  {
    TRACE_FUNC_ENTER();
    if( this->waitCondition(TestThread::haveTriggerBind, this) )
    {
      if( !this->dequeTrigger() )
      {
        MY_LOGE("trigger out of sync");
        return MFALSE;
      }

      ++mUpdateCounter;
      SimulateDelayNS(10*NS_PER_MS);
    }
    TRACE_FUNC_EXIT();
    return MTRUE;
  }

  virtual MBOOL onThreadStart()
  {
    TRACE_FUNC_ENTER();
    mTrigger = 0;
    ++mInitCounter;
    TRACE_FUNC_EXIT();
    return MTRUE;
  }

  virtual MBOOL onThreadStop()
  {
    TRACE_FUNC_ENTER();
    ++mUninitCounter;
    TRACE_FUNC_EXIT();
    return MTRUE;
  }

  int mInitCounter;
  int mUninitCounter;
  int mUpdateCounter;

  android::Mutex mTriggerMutex;
  int mTrigger;

  MVOID trigger()
  {
    TRACE_FUNC_ENTER();
    android::Mutex::Autolock lock(mTriggerMutex);
    ++mTrigger;
    this->signalEnque();
    TRACE_FUNC_EXIT();
  }

  MBOOL dequeTrigger()
  {
    TRACE_FUNC_ENTER();
    MBOOL ret;
    android::Mutex::Autolock lock(mTriggerMutex);
    ret = mTrigger;
    if( mTrigger )
    {
      --mTrigger;
    }
    TRACE_FUNC_EXIT();
    return ret;
  }

  MBOOL haveTrigger()
  {
    TRACE_FUNC_ENTER();
    android::Mutex::Autolock lock(mTriggerMutex);
    MY_LOGD("trigger=%d", mTrigger);
    TRACE_FUNC_EXIT();
    return mTrigger;
  }

  static MBOOL haveTriggerBind(TestThread *testThread)
  {
    TRACE_FUNC_ENTER();
    MBOOL ret;
    ret = testThread->haveTrigger();
    MY_LOGD("ret=%d", ret);
    TRACE_FUNC_EXIT();
    return ret;
  }

};

TEST(CamThread, Init)
{
  TestThread thread;
  android::sp<WaitNotifyCB> waiter;
  int i;

  EXPECT_EQ(thread.mInitCounter, 0);
  EXPECT_EQ(thread.mUninitCounter, 0);
  EXPECT_EQ(thread.mUpdateCounter, 0);

  EXPECT_TRUE(thread.startThread());
  thread.waitIdle();
  EXPECT_EQ(thread.mInitCounter, 1);
  EXPECT_EQ(thread.mUninitCounter, 0);
  EXPECT_EQ(thread.mUpdateCounter, 0);
  thread.trigger();
  waiter = new WaitNotifyCB();
  EXPECT_TRUE(thread.insertIdleCB(waiter));
  waiter->wait();
  EXPECT_EQ(thread.mInitCounter, 1);
  EXPECT_EQ(thread.mUninitCounter, 0);
  EXPECT_EQ(thread.mUpdateCounter, 1);

  EXPECT_FALSE(thread.startThread());
  SimulateDelayNS(20*NS_PER_MS);
  EXPECT_EQ(thread.mInitCounter, 1);
  EXPECT_EQ(thread.mUninitCounter, 0);
  EXPECT_EQ(thread.mUpdateCounter, 1);

  EXPECT_TRUE(thread.stopThread());
  EXPECT_EQ(thread.mInitCounter, 1);
  EXPECT_EQ(thread.mUninitCounter, 1);
  EXPECT_EQ(thread.mUpdateCounter, 1);

  EXPECT_FALSE(thread.stopThread());
  SimulateDelayNS(20*NS_PER_MS);
  EXPECT_EQ(thread.mInitCounter, 1);
  EXPECT_EQ(thread.mUninitCounter, 1);
  EXPECT_EQ(thread.mUpdateCounter, 1);

  EXPECT_TRUE(thread.startThread());
  SimulateDelayNS(20*NS_PER_MS);
  EXPECT_EQ(thread.mInitCounter, 2);
  EXPECT_EQ(thread.mUninitCounter, 1);
  EXPECT_EQ(thread.mUpdateCounter, 1);
  EXPECT_TRUE(thread.stopThread());
  SimulateDelayNS(20*NS_PER_MS);
  EXPECT_EQ(thread.mInitCounter, 2);
  EXPECT_EQ(thread.mUninitCounter, 2);
  EXPECT_EQ(thread.mUpdateCounter, 1);
}

TEST(CamThread, Stop)
{
  TestThread thread;
  thread.startThread();
}

#define TEST_TRIGGER_COUNT 10
TEST(CamThread, LegacyFlow)
{
  TestThread thread;
  android::sp<WaitNotifyCB> waiter;
  int i, trigger;

  MY_LOGD("Start CamThread.LegacyFlow test");

  trigger = 0;

  EXPECT_TRUE(thread.startThread());
  thread.waitIdle();

  for( i = 0; i < TEST_TRIGGER_COUNT; ++i )
  {
    thread.trigger();
    ++trigger;
  }

  EXPECT_LT(thread.mUpdateCounter, trigger);

  thread.waitIdle();
  EXPECT_EQ(thread.mUpdateCounter, trigger);

  for( i = 0; i < TEST_TRIGGER_COUNT; ++i )
  {
    thread.trigger();
    ++trigger;
  }
  waiter = new WaitNotifyCB();
  EXPECT_TRUE(thread.insertIdleCB(waiter));
  waiter->wait();
  EXPECT_TRUE(thread.stopThread());

  EXPECT_EQ(thread.mUpdateCounter, trigger);
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
