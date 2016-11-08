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

#include "StreamingFeatureTimer.h"

#include "DebugControl.h"
#define PIPE_CLASS_TAG "Timer"
#define PIPE_TRACE TRACE_STREAMING_FEATURE_TIMER
#include <featurePipe/core/include/PipeLog.h>

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

MVOID StreamingFeatureTimer::print(MUINT32 requestNo)
{
  MUINT32 total       = getElapsed();
  MUINT32 p2a         = getElapsedP2A();
  MUINT32 p2aEnque    = getElapsedEnqueP2A();
  MUINT32 eis         = getElapsedEIS();
  MUINT32 fd          = getElapsedFD();
  MUINT32 vfb         = getElapsedVFB();
  MUINT32 gpu         = getElapsedGPU();
  MUINT32 gpuWarp     = getElapsedWarpGPU();
  MUINT32 mdp         = getElapsedMDP();
  MUINT32 p2b         = getElapsedP2B();
  MUINT32 p2bEnque    = getElapsedEnqueP2B();

  MY_LOGD("Frame timer [#%5d][t%4d][a%4d/%4d][e%4d][f%4d][v%4d][g%4d/%4d][m%4d][b%4d/%4d]",
          requestNo, total, p2aEnque, p2a, eis, fd, vfb, gpuWarp, gpu, mdp, p2bEnque, p2b);

#if defined(DEBUG_TIMER) && (DEBUT_TIMER == 1)
  {
    MUINT32 t1 = getElapsedT1();
    MUINT32 t2 = getElapsedT2();
    MUINT32 t3 = getElapsedT3();
    MUINT32 t4 = getElapsedT4();
    MY_LOGD("Frame [t1%5d][t2%5d][t3%5d][t4%5d]",
            requestNo, t1, t2, t3, t4);
  }
#endif
}

} // namespace NSFeaturePipe
} // namespace NSCamFeature
} // namespace NSCam
