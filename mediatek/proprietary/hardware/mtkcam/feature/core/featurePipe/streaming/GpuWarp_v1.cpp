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

#include "GpuWarp_v1.h"

#include "DebugControl.h"
#define PIPE_CLASS_TAG "GpuWarp"
#define PIPE_TRACE TRACE_GPU_WARP
#include <featurePipe/core/include/PipeLog.h>

using android::GraphicBuffer;
using android::Mutex;
using android::sp;

namespace NSCam {
namespace NSCamFeature {

GpuWarp_v1::GpuWarp_v1()
  : mpGpuWarp(NULL)
  , mWorkBufSize(0)
  , mWorkBuf(NULL)
{
  TRACE_FUNC_ENTER();
  TRACE_FUNC_EXIT();
}

GpuWarp_v1::~GpuWarp_v1()
{
  TRACE_FUNC_ENTER();
  this->uninit();
  if( mpGpuWarp || mWorkBufSize || mWorkBuf )
  {
    MY_LOGE("uninit() failed to call onReset()");
  }
  TRACE_FUNC_EXIT();
}

MBOOL GpuWarp_v1::onConfig(MUINT32 feature, const GB_PTR_ARRAY &in, const GB_PTR_ARRAY &out, const MSize &maxImage, const MSize &maxWarp)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  MY_LOGD("feature=0x%x in(%d)/out(%d) maxImage(%dx%d) maxWarp(%dx%d)",
          feature, in.size(), out.size(),
          maxImage.w, maxImage.h, maxWarp.w, maxWarp.h);

  cleanUp();
  ret = initWarp(feature, in, out, maxImage, maxWarp) && initWorkBuffer();
  if( !ret )
  {
    cleanUp();
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL GpuWarp_v1::onReset()
{
  TRACE_FUNC_ENTER();
  cleanUp();
  TRACE_FUNC_EXIT();
  return MTRUE;
}

MBOOL GpuWarp_v1::onProcess(GB_PTR in, GB_PTR out, IImageBuffer *warpMap, const MSize &inSize, const MSize &outSize, MBOOL passThrough)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  struct WarpImageExtInfo warpInfo;
  GpuTuning gpuTuning;
  MUINT32 passWarp[2][4];

  warpInfo.pTuningPara = &gpuTuning;
  // Use version 3 for D3 patch
  // warpInfo.pTuningPara->GLESVersion = 2;
  warpInfo.pTuningPara->GLESVersion = 3;
  warpInfo.pTuningPara->Demo = 0;

  warpInfo.Width = inSize.w;
  warpInfo.Height = inSize.h;
  warpInfo.ClipWidth = outSize.w;
  warpInfo.ClipHeight = outSize.h;

  warpInfo.WarpLevel = 0;

  warpInfo.WarpMatrixNum = 0;

  warpInfo.WarpMapNum = 1;
  if( passThrough )
  {
    passWarp[0][0] = 0;
    passWarp[0][1] = (inSize.w-1) * 16;
    passWarp[0][2] = 0;
    passWarp[0][3] = (inSize.w-1) * 16;
    passWarp[1][0] = 0;
    passWarp[1][1] = 0;
    passWarp[1][2] = (inSize.h-1) * 16;
    passWarp[1][3] = (inSize.h-1) * 16;
    warpInfo.WarpMapSize[0][0] = 2;
    warpInfo.WarpMapSize[0][1] = 2;
    warpInfo.WarpMapAddr[0][0] = passWarp[0];
    warpInfo.WarpMapAddr[0][1] = passWarp[1];
  }
  else
  {
    warpInfo.WarpMapSize[0][0] = warpMap->getImgSize().w / 4;
    warpInfo.WarpMapSize[0][1] = warpMap->getImgSize().h / 2;
    warpInfo.WarpMapAddr[0][0] = (MUINT32*)(warpMap->getBufVA(0));
    warpInfo.WarpMapAddr[0][1] = (MUINT32*)(warpMap->getBufVA(0) + warpMap->getBufSizeInBytes(0) / 2);
  }

  warpInfo.SrcGraphicBuffer = (void*)(in);
  warpInfo.DstGraphicBuffer = (void*)(out);

  char *gfxBuf = NULL;
  (*out)->unlock();
  ret = doWarp(warpInfo);
  (*out)->lock(GRALLOC_USAGE_SW_WRITE_OFTEN | GRALLOC_USAGE_SW_READ_OFTEN, (void**)(&gfxBuf));
  TRACE_FUNC_EXIT();
  return ret;
}

MBOOL GpuWarp_v1::initWarp(MUINT32 feature, const GB_PTR_ARRAY &inputBuffers, const GB_PTR_ARRAY &outputBuffers, const MSize &maxImage, const MSize &maxWarp)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  struct WarpImageExtInfo warpInfo;
  GpuTuning gpuTuning;
  sp<GraphicBuffer>** inputArray;
  sp<GraphicBuffer>** outputArray;

  inputArray = new sp<GraphicBuffer>* [inputBuffers.size()];
  outputArray = new sp<GraphicBuffer>* [outputBuffers.size()];

  if( prepareBufferArray(inputArray, outputArray, inputBuffers, outputBuffers) )
  {
    configInitWarpInfo(&warpInfo, &gpuTuning, feature, maxImage, maxWarp);
    warpInfo.SrcGraphicBuffer = (void*) inputArray;
    warpInfo.DstGraphicBuffer = (void*) outputArray;
    warpInfo.InputGBNum = inputBuffers.size();
    warpInfo.OutputGBNum = outputBuffers.size();

    ret = createWarpObj() && initWarpObj(warpInfo);
  }

  delete [] inputArray;
  delete [] outputArray;
  inputArray = NULL;
  outputArray = NULL;
  TRACE_FUNC_EXIT();
  return ret;
}

MVOID GpuWarp_v1::uninitWarp()
{
  TRACE_FUNC_ENTER();
  if( mpGpuWarp )
  {
    mpGpuWarp->WarpReset();
    mpGpuWarp->destroyInstance(mpGpuWarp);
    mpGpuWarp = NULL;
  }
  TRACE_FUNC_EXIT();
}

MBOOL GpuWarp_v1::prepareBufferArray(GB_PTR *inputArray, GB_PTR *outputArray, const GB_PTR_ARRAY &inputBuffers, const GB_PTR_ARRAY &outputBuffers)
{
  TRACE_FUNC_ENTER();

  MBOOL ret = MFALSE;
  MUINT32 i, size;
  if( inputArray == NULL || outputArray == NULL )
  {
    MY_LOGE("OOM: cannot create buffer array holder");
  }
  else if( inputBuffers.size() == 0 ||
             outputBuffers.size() == 0 )
  {
    MY_LOGE("Invalid number of input(%d)/output(%d) buffers provided",
            inputBuffers.size(), outputBuffers.size());
  }
  else
  {
    ret = MTRUE;
    for( i = 0, size = inputBuffers.size(); i < size; ++i )
    {
      if( inputBuffers[i] == NULL )
      {
        MY_LOGE("Invalid input buffer[%d]", i);
        ret = MFALSE;
      }
      inputArray[i] = inputBuffers[i];
    }
    for( i = 0, size = outputBuffers.size(); i < size; ++i )
    {
      if( outputBuffers[i] == NULL )
      {
        MY_LOGE("Invalid output buffer[%d]", i);
        ret = MFALSE;
      }
      outputArray[i] = outputBuffers[i];
    }
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MVOID GpuWarp_v1::configInitWarpInfo(struct WarpImageExtInfo *warpInfo, GpuTuning *gpuTuning, MUINT32 feature, const MSize &maxImage, const MSize &maxWarp)
{
  TRACE_FUNC_ENTER();
  warpInfo->pTuningPara = gpuTuning;
  warpInfo->pTuningPara->GLESVersion = 2;
  warpInfo->pTuningPara->Demo = 0;

  warpInfo->Features = 0;
  if( this->hasVFB(feature) )
  {
    MY_LOGD("Enable VFB BIT");
    ADD_FEATURE(warpInfo->Features, MTK_VFB_BIT);
  }
  if( this->hasEIS(feature) )
  {
    MY_LOGD("Enable EIS2 BIT");
    ADD_FEATURE(warpInfo->Features, MTK_EIS2_BIT);
  }

  warpInfo->ImgFmt = WARP_IMAGE_YV12;
  warpInfo->OutImgFmt = WARP_IMAGE_RGBA8888;

  warpInfo->Width = maxImage.w;
  warpInfo->Height = maxImage.h;

  warpInfo->WarpMatrixNum = 0;
  warpInfo->WarpMapNum = 1;
  warpInfo->MaxWarpMapSize[0] = maxWarp.w;
  warpInfo->MaxWarpMapSize[1] = maxWarp.h;
  //warpInfo->demo = 0;
  TRACE_FUNC_EXIT();
}

MBOOL GpuWarp_v1::createWarpObj()
{
  TRACE_FUNC_ENTER();
  if( (mpGpuWarp = MTKWarp::createInstance(DRV_WARP_OBJ_GLES)) == NULL )
  {
    MY_LOGE("MTKWarp::createInstance failed");
  }
  TRACE_FUNC_EXIT();
  return mpGpuWarp != NULL;
}

MBOOL GpuWarp_v1::initWarpObj(const struct WarpImageExtInfo &warpInfo)
{
  TRACE_FUNC_ENTER();
  MRESULT mret;
  if( (mret = mpGpuWarp->WarpInit((MUINT32*) &warpInfo, NULL)) != S_WARP_OK )
  {
    MY_LOGE("MTKWarp init failed(%d).", mret);
  }
  TRACE_FUNC_EXIT();
  return (mret == S_WARP_OK);
}

MBOOL GpuWarp_v1::initWorkBuffer()
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  MRESULT mret;
  if( !mpGpuWarp )
  {
    MY_LOGE("Invalid GpuWarp object state");
  }
  else if( (mret = mpGpuWarp->WarpFeatureCtrl(WARP_FEATURE_GET_WORKBUF_SIZE, NULL, &mWorkBufSize)) != S_WARP_OK )
  {
    MY_LOGE("MTKWarp WARP_FEATURE_GET_WORKBUF_ADDR failed! (%d)", mret);
  }
  else if( (mWorkBuf = (MUINT8*)malloc(mWorkBufSize)) == NULL )
  {
    MY_LOGE("OOM: Warp working buffer allocation failed!");
  }
  else if( (mret = mpGpuWarp->WarpFeatureCtrl(WARP_FEATURE_SET_WORKBUF_ADDR, &mWorkBuf, NULL)) != S_WARP_OK)
  {
    MY_LOGE("MTKWarp WARP_FEATURE_SET_WORKBUF_ADDR failed! (%d)", mret);
  }
  else
  {
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MVOID GpuWarp_v1::uninitWorkBuffer()
{
  TRACE_FUNC_ENTER();
  mWorkBufSize = 0;
  free(mWorkBuf);
  mWorkBuf = NULL;
  TRACE_FUNC_EXIT();
}

MBOOL GpuWarp_v1::doWarp(struct WarpImageExtInfo &warpInfo)
{
  TRACE_FUNC_ENTER();
  MBOOL ret = MFALSE;
  MRESULT mret;
  if( (mret = mpGpuWarp->WarpFeatureCtrl(WARP_FEATURE_ADD_IMAGE, &warpInfo, NULL)) != S_WARP_OK )
  {
    MY_LOGE("MTKWarp WARP_FEATURE_ADD_IMAGE failed! (%d)", mret);
  }
  else if( (mret = mpGpuWarp->WarpMain()) != S_WARP_OK )
  {
    MY_LOGE("MTKWarp WarpMain failed! (%d)", mret);
  }
  else
  {
    ret = MTRUE;
  }
  TRACE_FUNC_EXIT();
  return ret;
}

MVOID GpuWarp_v1::cleanUp()
{
  TRACE_FUNC_ENTER();
  uninitWarp();
  uninitWorkBuffer();
  TRACE_FUNC_EXIT();
}

} // namespace NSCamFeature
} // namespace NSCam
