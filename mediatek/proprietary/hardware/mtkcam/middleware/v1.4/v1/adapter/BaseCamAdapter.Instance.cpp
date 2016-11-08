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

#define LOG_TAG "MtkCam/CamAdapter"
//
#include <cutils/properties.h>
//
#include "inc/CamUtils.h"
using namespace android;
using namespace MtkCamUtils;
//
#include "inc/ImgBufProvidersManager.h"
//
#include <v1/IParamsManager.h>
#include <v1/ICamAdapter.h>
#include "inc/BaseCamAdapter.h"
//


/******************************************************************************
*   Function Prototype.
*******************************************************************************/
#if '1'==MTKCAM_HAVE_MTKDEFAULT
sp<ICamAdapter> createDefaultAdapter(String8 const& rName, int32_t const i4OpenId, sp<IParamsManager> pParamsMgr);
#endif
#if '1'==MTKCAM_HAVE_MTKSTEREO
//#include <CamAdapter/inc/MtkCamAdapter.Stereo.h>
sp<ICamAdapter> createMtkStereoCamAdapter(String8 const& rName, int32_t const i4OpenId, sp<IParamsManager> pParamsMgr);
#endif

/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%s)[BaseCamAdapter::%s] " fmt, getName(), __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)


/******************************************************************************
*
*******************************************************************************/
sp<ICamAdapter>
ICamAdapter::
createInstance(
    String8 const&      rName,
    int32_t const       i4OpenId,
    sp<IParamsManager>  pParamsMgr
)
{
    String8 const s8AppMode = PARAMSMANAGER_MAP_INST(eMapAppMode)->stringFor(pParamsMgr->getHalAppMode());
    //
    MINT32 stereoMwEnable = 0;
    {
        char value[PROPERTY_VALUE_MAX] = {'\0'};

        property_get( "debug.everest.stereo.enable", value, "0");
        stereoMwEnable = atoi(value);
        CAM_LOGD("stereoMwEnable(%d)", stereoMwEnable);
    }
    //
    // stereo temp dev
    if(stereoMwEnable)
    {
        if( s8AppMode == MtkCameraParameters::APP_MODE_NAME_DEFAULT ||
            s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_PHOTO ||
            s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_VIDEO ||
            s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_ZSD ||
            s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_VT)
        {
            #if ('1'==MTKCAM_HAVE_MTKDEFAULT || '1'==MTKCAM_HAVE_MTKVT)
            CAM_LOGD("stereo temp dev => hard code createMtkStereoCamAdapter");
            return  createMtkStereoCamAdapter(s8AppMode, i4OpenId, pParamsMgr);
            #endif
        }
    }
    //
    if( s8AppMode == MtkCameraParameters::APP_MODE_NAME_DEFAULT ||
        s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_PHOTO ||
        s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_VIDEO ||
        s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_ZSD ||
        s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_VT ||
        s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_ENG)

    {
        #if ('1'==MTKCAM_HAVE_MTKDEFAULT || '1'==MTKCAM_HAVE_MTKVT)
        return  createDefaultAdapter(s8AppMode, i4OpenId, pParamsMgr);
        #endif
    }
    //
    if( s8AppMode == MtkCameraParameters::APP_MODE_NAME_MTK_STEREO )
    {
        #if '1'==MTKCAM_HAVE_MTKSTEREO
        return  createMtkStereoCamAdapter(s8AppMode, i4OpenId, pParamsMgr);
        #endif
    }
    //
    //
    CAM_LOGE("[ICamAdapter::%s, %s] NOT IMPLEMENT YET !", rName.string(), __FUNCTION__);
    return  NULL;
}


/******************************************************************************
*
*******************************************************************************/
bool
ICamAdapter::
isValidInstance(sp<ICamAdapter>const& rpCamAdapter)
{
    if  ( rpCamAdapter == 0 )
    {
        CAM_LOGW("[ICamAdapter::%s] NULL rpCamAdapter", __FUNCTION__);
        return false;
    }
    //
    String8 const s8AppMode = PARAMSMANAGER_MAP_INST(eMapAppMode)->stringFor(rpCamAdapter->getParamsManager()->getHalAppMode());
    //
    char const*const pszName    = rpCamAdapter->getName();
    int32_t   const i4OpenId    = rpCamAdapter->getOpenId();
    //
    //
    CAM_LOGD(
        "[ICamAdapter::%s] OpenId(%d) current(%s)/expect(%s)",
        __FUNCTION__, i4OpenId, pszName, s8AppMode.string()
    );
    //
    return  (s8AppMode == pszName);
}

