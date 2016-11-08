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

#define LOG_TAG "MtkCam/devicemgr"
//
#include "MyUtils.h"
#include "CamDeviceManagerImp.h"
using namespace android;
using namespace NSCam;
//
#include <mtkcam/hwutils/CamManager.h>
using namespace NSCam::Utils;
//
#include <mtkcam/hal/IHalFlash.h>
/******************************************************************************
 *
 ******************************************************************************/
#if '1'==MTKCAM_HAVE_SENSOR_HAL
    #include <mtkcam/hal/IHalSensor.h>
#else
    #warning "[Warn] Not support Sensor Hal"
#endif

#if '1'==MTKCAM_HAVE_METADATA
    #include <mtkcam/metadata/IMetadataProvider.h>
#else
    #warning "[Warn] Not support Metadata"
#endif

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


/******************************************************************************
 *
 ******************************************************************************/
namespace
{
    CamDeviceManagerImp gCamDeviceManager;
}   //namespace


/******************************************************************************
 *
 ******************************************************************************/
namespace NSCam {
ICamDeviceManager*
getCamDeviceManager()
{
    return  &gCamDeviceManager;
}
}


/******************************************************************************
 *
 ******************************************************************************/
CamDeviceManagerImp::
CamDeviceManagerImp()
    : CamDeviceManagerBase()
{
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
CamDeviceManagerImp::
validateOpenLocked(int32_t i4OpenId) const
{
    status_t status = OK;
    //
    status = CamDeviceManagerBase::validateOpenLocked(i4OpenId);
    if  ( OK != status )
    {
        return  status;
    }
    //
    if  ( MAX_SIMUL_CAMERAS_SUPPORTED <= mOpenMap.size() )
    {
        MY_LOGE("Cannot open device %d ...", i4OpenId);
        MY_LOGE("open count(%d) >= maximum count(%d)", mOpenMap.size(), MAX_SIMUL_CAMERAS_SUPPORTED);
        status = NO_MEMORY;
    }
    //
    CamManager* pCamMgr = CamManager::getInstance();
    if ( ! pCamMgr->getPermission() )
    {
        MY_LOGE("Cannot open device %d ... Permission denied", i4OpenId);
        status = NO_MEMORY;
    }
    //
    return  status;
}


/******************************************************************************
 *
 ******************************************************************************/
int32_t
CamDeviceManagerImp::
enumDeviceLocked()
{
    Utils::CamProfile _profile(__FUNCTION__, "CamDeviceManagerImp");
    //
    status_t status = OK;
    int32_t i, i4DeviceNum = 0, iSensorsList = 0;
    //temp
    camera_info camInfo;
    camInfo.device_version = CAMERA_DEVICE_API_VERSION_1_0;
    camInfo.static_camera_characteristics = NULL;
    //
#if '1'==MTKCAM_HAVE_METADATA
        NSMetadataProviderManager::clear();
#endif
    mEnumMap.clear();
//------------------------------------------------------------------------------
#if '1'==MTKCAM_HAVE_SENSOR_HAL

    mEnumMap.clear();
    //
    IHalSensorList* const pHalSensorList = IHalSensorList::get();
    if(!pHalSensorList)
    {
        MY_LOGE("pHalSensorList == NULL");
        return 0;
    }
    //
    pHalSensorList->searchSensors();
    i4DeviceNum = pHalSensorList->queryNumberOfSensors();
    //
    if(i4DeviceNum <= 0)
    {
        MY_LOGE("i4DeviceNum(%d) <= 0",i4DeviceNum);
        return 0;
    }
    //
    for(i = 0; i < i4DeviceNum; i++)
    {
        char szDeviceId[20];
        sprintf(szDeviceId, "%d", i);
        uint32_t curDevIdx;
        SensorStaticInfo curSensorStaticInfo;
        //
        curDevIdx = pHalSensorList->querySensorDevIdx(i);
        iSensorsList |= curDevIdx;
        //
        pHalSensorList->querySensorStaticInfo(
                            curDevIdx,
                            &curSensorStaticInfo);
        //
        MY_LOGD("i(%d),DevIdx(%d),Name(%s),Type(%d)",
                i,
                curDevIdx,
                pHalSensorList->queryDriverName(i),
                pHalSensorList->queryType(i));
        //
        if(curDevIdx == SENSOR_DEV_MAIN_2)
        {
            String8 const s8Main2IdKey = String8("MTK_SENSOR_DEV_MAIN_2");
            String8 const s8Main2IdVal = String8(szDeviceId);
            Utils::Property::set(s8Main2IdKey, s8Main2IdVal);
            //
            MY_LOGI("Stereo 2nd Camera found %d", i);
        }
        else
        if(curDevIdx == SENSOR_DEV_MAIN)
        {
            String8 const s8MainIdKey = String8("MTK_SENSOR_DEV_MAIN");
            String8 const s8MainIdVal = String8(szDeviceId);
            Utils::Property::set(s8MainIdKey, s8MainIdVal);
            //
            int32_t const deviceId = i;
#if '1'==MTKCAM_HAVE_METADATA
            sp<IMetadataProvider> pMetadataProvider = IMetadataProvider::create(deviceId);
            NSMetadataProviderManager::add(deviceId, pMetadataProvider.get());
            MY_LOGD("[0x%02x] IMetadataProvider:%p sensor:%s flash:%d",
                deviceId, pMetadataProvider.get(),
                pHalSensorList->queryDriverName(i), pMetadataProvider->getDeviceHasFlashLight());
#endif
            //
            sp<EnumInfo> pInfo = new EnumInfo;
            pInfo->uDeviceVersion       = CAMERA_DEVICE_API_VERSION_1_0;
            pInfo->iFacing              = curSensorStaticInfo.facingDirection;
            pInfo->iWantedOrientation   = curSensorStaticInfo.orientationAngle;
            pInfo->iSetupOrientation    = curSensorStaticInfo.orientationAngle;
#if '1'==MTKCAM_HAVE_METADATA
            pInfo->pMetadata            = pMetadataProvider->getStaticCharacteristics();
            pInfo->iHasFlashLight       = pMetadataProvider->getDeviceHasFlashLight();
#else
            pInfo->pMetadata            = NULL;
            pInfo->iHasFlashLight       = 0;
#endif
            mEnumMap.add(deviceId, pInfo);
        }
        else
        if(curDevIdx == SENSOR_DEV_SUB)
        {
            String8 const s8SubIdKey = String8("MTK_SENSOR_DEV_SUB");
            String8 const s8SubIdVal = String8(szDeviceId);
            Utils::Property::set(s8SubIdKey, s8SubIdVal);
            //
            int32_t const deviceId = i;
            //
#if '1'==MTKCAM_HAVE_METADATA
            sp<IMetadataProvider> pMetadataProvider = IMetadataProvider::create(deviceId);
            NSMetadataProviderManager::add(deviceId, pMetadataProvider.get());
            MY_LOGD("[0x%02x] IMetadataProvider:%p sensor:%s flash:%d",
                deviceId, pMetadataProvider.get(),
                pHalSensorList->queryDriverName(i), pMetadataProvider->getDeviceHasFlashLight());
#endif
            sp<EnumInfo> pInfo = new EnumInfo;
            pInfo->uDeviceVersion       = CAMERA_DEVICE_API_VERSION_1_0;
            pInfo->iFacing              = curSensorStaticInfo.facingDirection;
            pInfo->iWantedOrientation   = curSensorStaticInfo.orientationAngle;
            pInfo->iSetupOrientation    = curSensorStaticInfo.orientationAngle;
#if '1'==MTKCAM_HAVE_METADATA
            pInfo->pMetadata            = pMetadataProvider->getStaticCharacteristics();
            pInfo->iHasFlashLight       = pMetadataProvider->getDeviceHasFlashLight();
#else
            pInfo->pMetadata            = NULL;
            pInfo->iHasFlashLight       = 0;
#endif
            mEnumMap.add(deviceId, pInfo);
        }
    }
    //
    MY_LOGI("iSensorsList=0x%08X, i4DeviceNum=%d",
            iSensorsList,
            i4DeviceNum);
    if ( iSensorsList | SENSOR_DEV_MAIN_3D )
    {
        String8 const s8StereoKey = String8("MTK_STEREO_FEATURE_SUPPORT");
        Utils::Property::set(s8StereoKey, String8("true"));
    }
    for (size_t i = 0; i < mEnumMap.size(); i++)
    {
        int32_t const deviceId = mEnumMap.keyAt(i);
        sp<EnumInfo> pInfo = mEnumMap.valueAt(i);
        uint32_t const uDeviceVersion   = pInfo->uDeviceVersion;
        camera_metadata const*pMetadata = pInfo->pMetadata;
        int32_t const iFacing           = pInfo->iFacing;
        int32_t const iWantedOrientation= pInfo->iWantedOrientation;
        int32_t const iSetupOrientation = pInfo->iSetupOrientation;
        MY_LOGI(
            "[0x%02x] orientation(wanted/setup)=(%d/%d) facing:%d metadata:%p DeviceVersion:0x%x",
            deviceId,
            iWantedOrientation,
            iSetupOrientation,
            iFacing,
            pMetadata,
            uDeviceVersion);
    }
    i4DeviceNum = mEnumMap.size();


#else   //----------------------------------------------------------------------

    #warning "[WARN] Simulation for CamDeviceManagerImp::enumDeviceLocked()"

    mEnumMap.clear();
    {
        int32_t const deviceId = 0;
        //
        camera_info camInfo;
        camInfo.device_version  = CAMERA_DEVICE_API_VERSION_1_0;
        camInfo.static_camera_characteristics = NULL;
        camInfo.facing      = 0;
        camInfo.orientation = 90;
        //
        sp<EnumInfo> pInfo = new EnumInfo;
        pInfo->uDeviceVersion       = CAMERA_DEVICE_API_VERSION_1_0;
        pInfo->pMetadata            = NULL;
        pInfo->iFacing              = 0;
        pInfo->iWantedOrientation   = 90;
        pInfo->iSetupOrientation    = 90;
        mEnumMap.add(deviceId, pInfo);
    }
    //
    {
        int32_t const deviceId = 0xFF;
        //
        camera_info camInfo;
        camInfo.device_version  = CAMERA_DEVICE_API_VERSION_1_0;
        camInfo.static_camera_characteristics = NULL;
        camInfo.facing      = 0;
        camInfo.orientation = 0;
        //
        sp<EnumInfo> pInfo = new EnumInfo;
        pInfo->uDeviceVersion       = CAMERA_DEVICE_API_VERSION_1_0;
        pInfo->pMetadata            = NULL;
        pInfo->iFacing              = 0;
        pInfo->iWantedOrientation   = 0;
        pInfo->iSetupOrientation    = 0;
        mEnumMap.add(deviceId, pInfo);
    }
    //
    i4DeviceNum = 1;

#endif
//------------------------------------------------------------------------------
    //
    _profile.print("");
    return  i4DeviceNum;
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
CamDeviceManagerImp::
attachDeviceLocked(android::sp<ICamDevice> pDevice, uint32_t device_version)
{
    status_t status = OK;
    //
    status = CamDeviceManagerBase::attachDeviceLocked(pDevice, device_version);
    if  ( OK != status )
    {
        return  status;
    }
    //
    CamManager* pCamMgr = CamManager::getInstance();
    pCamMgr->incDevice();
    //
    return  status;
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
CamDeviceManagerImp::
detachDeviceLocked(android::sp<ICamDevice> pDevice)
{
    status_t status = OK;
    //
    status = CamDeviceManagerBase::detachDeviceLocked(pDevice);
    if  ( OK != status )
    {
        return  status;
    }
    //
    CamManager* pCamMgr = CamManager::getInstance();
    pCamMgr->decDevice();
    //
    return  status;
}

status_t
CamDeviceManagerImp::
setTorchModeLocked(int const deviceId, bool enabled, bool notifyEvent = true)
{
    sp<EnumInfo> pInfo = mEnumMap.size() > deviceId ? mEnumMap.valueAt(deviceId) : NULL;
    if (pInfo.get() && pInfo->iHasFlashLight == 0) {
         MY_LOGW("setTorchAvailableLocked Failed[No Flash]: deviceId:%d",deviceId);
        return -EINVAL;
    }

    IHalFlash*const pHalFlash = IHalFlash::getInstance();
    bool flashStatus = pHalFlash->getTorchStatus(deviceId) == 1;
    MY_LOGD("setTorchModeLocked: deviceId:%d, flashStatus:%d, enable:%d",deviceId ,flashStatus, enabled);
    if (flashStatus != enabled) {
        if (pHalFlash->setTorchOnOff(deviceId, enabled) == OK) {
            if (notifyEvent) {
                String8 cameraId = String8::format("%d", deviceId);
                mpModuleCallbacks->torch_mode_status_change(mpModuleCallbacks, cameraId.string(),
                    enabled ? TORCH_MODE_STATUS_AVAILABLE_ON : TORCH_MODE_STATUS_AVAILABLE_OFF);
            }
        } else {
            MY_LOGW("SetTorchModeLocked Failed: deviceId:%d, enable:%d",deviceId, enabled);
            return -EINVAL;
        }
    }
    return OK;
}


status_t
CamDeviceManagerImp::
setTorchAvailableLocked(int const deviceId, bool available)
{
    sp<EnumInfo> pInfo = mEnumMap.size() > deviceId ? mEnumMap.valueAt(deviceId) : NULL;
    if (pInfo.get() && pInfo->iHasFlashLight == 0) {
        MY_LOGW("setTorchAvailableLocked Failed[No Flash]: deviceId:%d",deviceId);
        return -EINVAL;
    }
    MY_LOGD("setTorchAvailableLocked: deviceId:%d, available:%d",deviceId ,available);
    // notify camera service for locking/unlocking touch mode
    String8 cameraId = String8::format("%d", deviceId);
    if (mpModuleCallbacks != NULL)
        mpModuleCallbacks->torch_mode_status_change(mpModuleCallbacks, cameraId.string(),
            available ? TORCH_MODE_STATUS_AVAILABLE_OFF : TORCH_MODE_STATUS_NOT_AVAILABLE);
    return OK;
}

