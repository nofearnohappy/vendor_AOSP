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

#define LOG_TAG "MtkCam/HalSensorList"
//
#include "MyUtils.h"
//
#include <dlfcn.h>
//
/******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s] " fmt, __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s] " fmt, __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

extern IMetadata  gSensorMetadata[3];

/******************************************************************************
 *
 ******************************************************************************/
static
MBOOL
impBuildStaticInfo_by_SymbolName(
    String8 const&  s8Symbol,
    Info const&     rInfo,
    IMetadata&      rMetadata
)
{
typedef MBOOL (*PFN_T)(
        Info const& rInfo,
        IMetadata&  rMetadata
    );
    //
    PFN_T pfn = (PFN_T)::dlsym(RTLD_DEFAULT, s8Symbol.string());
#if 0
    if  ( ! pfn ) {
        MY_LOGW("%s not found", s8Symbol.string());
        return  MFALSE;
    }
#endif
    //
    MBOOL const ret = pfn(rInfo, rMetadata);
    MY_LOGW_IF(!ret, "%s returns failure", s8Symbol.string());
    //
    return  ret;
}


/******************************************************************************
 *
 ******************************************************************************/
static
char const*const
kStaticMetadataTypeNames[] = { "0", "1", NULL };
//
static
MBOOL
impBuildStaticInfo(
    Info const&     rInfo,
    IMetadata&      rMetadata
)
{
    MBOOL ret = MFALSE;
    //
    {
        char const*const pTypeName = "SETUP";
        //
        String8 const s8Symbol_Common = String8::format("%s_%s_%s", PREFIX_FUNCTION_STATIC_METADATA, pTypeName, "COMMON");
        ret = impBuildStaticInfo_by_SymbolName(s8Symbol_Common, rInfo, rMetadata);
        if  ( ! ret ) {
            MY_LOGE_IF(1, "Fail for %s", s8Symbol_Common.string());
            return  ret;
        }
    }
    //
    //
    for (int i = 0; NULL != kStaticMetadataTypeNames[i]; i++)
    {
        char const*const pTypeName = kStaticMetadataTypeNames[i];
        //
        String8 const s8Symbol_Sensor = String8::format("%s_%s_%s", PREFIX_FUNCTION_STATIC_METADATA, pTypeName, rInfo.sensorDrvName().string());
        ret = impBuildStaticInfo_by_SymbolName(s8Symbol_Sensor, rInfo, rMetadata);
        if  ( ret ) {
            continue;
        }
        //
        String8 const s8Symbol_Common = String8::format(
            "%s_%s_%s", PREFIX_FUNCTION_STATIC_METADATA, pTypeName,
            (rInfo.sensorType()==NSSensorType::eRAW ? "COMMON_RAW" : "COMMON_YUV")
        );
        ret = impBuildStaticInfo_by_SymbolName(s8Symbol_Common, rInfo, rMetadata);
        if  ( ret ) {
            continue;
        }
        //
        MY_LOGE_IF(1, "Fail for both %s & %s", s8Symbol_Sensor.string(), s8Symbol_Common.string());
        break;
    }
    return  ret;
}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
HalSensorList::
buildStaticInfo(Info const& rInfo, IMetadata& rMetadata) const
{

    MINT32 idx;
    idx = rInfo.sensorIndex()>>1;

    if  ( ! impBuildStaticInfo(rInfo, gSensorMetadata[idx]) )
    {
        MY_LOGE(
            "Fail to build static info for %s index:%d type:%d",
            rInfo.sensorDrvName().string(), rInfo.sensorIndex(), rInfo.sensorType()
        );
        return  MFALSE;
    }
    //
    gSensorMetadata[idx].sort();
    rMetadata = gSensorMetadata[idx];
    //
    return  MTRUE;
}

