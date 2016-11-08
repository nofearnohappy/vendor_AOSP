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

#include "../include/DebugUtil.h"
#include <sys/resource.h>
#include <cutils/properties.h>

#include "../include/DebugControl.h"
#define PIPE_TRACE TRACE_DEBUG_UTIL
#define PIPE_CLASS_TAG "DebugUtil"
#include "../include/PipeLog.h"

#define DEFAULT_PROPERTY_VALUE 0

namespace NSCam {
namespace NSCamFeature {
namespace NSFeaturePipe {

MINT32 getPropertyValue(const char *key)
{
  return getPropertyValue(key, DEFAULT_PROPERTY_VALUE);
}

MINT32 getPropertyValue(const char *key, MINT32 defVal)
{
  TRACE_FUNC_ENTER();
  MINT32 value = defVal;
  if( key && *key )
  {
    value = property_get_int32(key, defVal);
    if( value != defVal )
    {
     MY_LOGD("getPropertyValue %s=%d", key, value);
    }
  }
  TRACE_FUNC_EXIT();
  return value;
}

MINT32 getFormattedPropertyValue(const char *fmt, ...)
{
  TRACE_FUNC_ENTER();
  char key[PROPERTY_KEY_MAX*2];
  va_list args;
  int keyLen;
  MINT32 value = DEFAULT_PROPERTY_VALUE;

  if( fmt && *fmt )
  {
    va_start(args, fmt);
    keyLen = vsnprintf(key, sizeof(key), fmt, args);
    va_end(args);

    if( keyLen >= PROPERTY_KEY_MAX )
    {
      MY_LOGE("Property key[%s...] length exceed %d char", key, PROPERTY_KEY_MAX-1);
    }
    if( keyLen > 0 && keyLen < PROPERTY_KEY_MAX )
    {
      value = getPropertyValue(key, DEFAULT_PROPERTY_VALUE);
    }
  }

  TRACE_FUNC_EXIT();
  return value;
}

}; // namespace NSFeaturePipe
}; // namespace NSCamFeature
}; // namespace NSCam
