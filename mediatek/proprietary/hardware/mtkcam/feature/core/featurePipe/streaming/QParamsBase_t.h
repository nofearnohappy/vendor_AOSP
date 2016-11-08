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

#ifndef _MTK_CAMERA_STREAMING_FEATURE_PIPE_QPARAMS_BASE_T_H_
#define _MTK_CAMERA_STREAMING_FEATURE_PIPE_QPARAMS_BASE_T_H_

#include "MtkHeader.h"
//#include <mtkcam/iopipe/PostProc/IHalPostProcPipe.h>

#include <utils/RefBase.h>
#include <utils/Mutex.h>

#include <queue>

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

template <typename T>
class QParamsBase : public virtual android::RefBase
{
public:
  struct BACKUP_DATA_TYPE
  {
    BACKUP_DATA_TYPE()
    {
    }

    BACKUP_DATA_TYPE(void *qCookie, NSCam::NSIoPipe::NSPostProc::QParams::PFN_CALLBACK_T qCB, T data)
      : mQParamsCookie(qCookie)
      , mQParamsCB(qCB)
      , mData(data)
    {
    }

    void *mQParamsCookie;
    NSCam::NSIoPipe::NSPostProc::QParams::PFN_CALLBACK_T mQParamsCB;
    T mData;
  };

  QParamsBase();
  virtual ~QParamsBase();

protected:
  virtual MBOOL onQParamsCB(NSCam::NSIoPipe::NSPostProc::QParams &param, T &data) = 0;

public:
  static void staticCBFunc(NSCam::NSIoPipe::NSPostProc::QParams &param);

protected:
  MBOOL enqueQParams(NSCam::NSIoPipe::NSPostProc::INormalStream *stream, NSCam::NSIoPipe::NSPostProc::QParams &param, T &data);

private:
  MBOOL retrieveQParamsData(BACKUP_DATA_TYPE &data);

private:
  android::Mutex mMutex;
  std::queue<BACKUP_DATA_TYPE> mQueue;
};

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam

#endif // _MTK_CAMERA_STREAMING_FEATURE_PIPE_QPARAMS_BASE_T_H_
