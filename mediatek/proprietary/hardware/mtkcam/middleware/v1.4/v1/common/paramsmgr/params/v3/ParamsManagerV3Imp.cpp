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
#define LOG_TAG "MtkCam/ParamsManagerV3"


#include <utils/Vector.h>

#include <Log.h>
#include <cutils/properties.h>  // For property_get().

#include <Hal3/mtk_platform_metadata_tag.h>
#include <metadata/IMetadataProvider.h>
#include <metadata/IMetadata.h>
using namespace NSCam;
using namespace android;

#include <metadata/client/mtk_metadata_tag.h>
#include "StaticMetaTable.h"

#include <v1/IParamsManager.h>

#include <v1/IParamsManagerV3.h>
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

#define FUNC_START     MY_LOGD("+")
#define FUNC_END       MY_LOGD("-")

 /******************************************************************************
 *
 ******************************************************************************/
#define MY_LOGD1(...)           MY_LOGD_IF(1 <= mLogLevel, __VA_ARGS__)
#define MY_LOGD2(...)           MY_LOGD_IF(2 <= mLogLevel, __VA_ARGS__)
/******************************************************************************
 *
 ******************************************************************************/
class ParamsManagerV3Imp
        : public IParamsManagerV3
{

public:     ////                    Interface.
    //
    virtual bool                       init();
    //
    virtual bool                       uninit();


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Attributes .
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    virtual char const*                getName()    const {return mName.string();}
    virtual int32_t                    getOpenId()   const {return mi4OpenId;}


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Operation.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:     ////     Operations .
    virtual status_t                updateRequest(IMetadata *request)    ;
    virtual status_t                updateRequestJpeg(IMetadata *request)   const;
    virtual status_t                updateRequestEng(IMetadata *request)      const;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Instance
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

public:
                                    ParamsManagerV3Imp(
                                        String8 const& rName,
                                        MINT32 const cameraId,
                                        sp<IParamsManager> pParamsMgr
                                    );
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:
    virtual                         ~ParamsManagerV3Imp(){};

    int                             wbModeStringToEnum(const char *wbMode);

    int                             effectModeStringToEnum(const char *effectMode);

    int                             abModeStringToEnum(const char *abMode);

    int                             sceneModeStringToEnum(const char *sceneMode);

    enum flashMode_t {
        FLASH_MODE_OFF = 0,
        FLASH_MODE_AUTO,
        FLASH_MODE_ON,
        FLASH_MODE_TORCH,
        FLASH_MODE_RED_EYE,
        FLASH_MODE_INVALID = -1
    };

    flashMode_t                     flashModeStringToEnum(const char *flashMode);


    enum focusMode_t {
        FOCUS_MODE_AUTO = MTK_CONTROL_AF_MODE_AUTO,
        FOCUS_MODE_MACRO = MTK_CONTROL_AF_MODE_MACRO,
        FOCUS_MODE_CONTINUOUS_VIDEO = MTK_CONTROL_AF_MODE_CONTINUOUS_VIDEO,
        FOCUS_MODE_CONTINUOUS_PICTURE = MTK_CONTROL_AF_MODE_CONTINUOUS_PICTURE,
        FOCUS_MODE_EDOF = MTK_CONTROL_AF_MODE_EDOF,
        FOCUS_MODE_INFINITY,
        FOCUS_MODE_FIXED,
        FOCUS_MODE_INVALID = -1
    };

    focusMode_t                     focusModeStringToEnum(const char *focusMode);

    bool                            boolFromString(const char *boolStr);

    MRect                           calculateCropRegion() const;

    struct my_params
    {
        MINT32                          mWbMode;
        MUINT8                          mEffectMode;
        MUINT8                          mAntibandingMode;
        MINT32                          mSceneMode;
        MINT32                          mPreviewFpsRange[2];
        flashMode_t                     mParamFlashMode;
        focusMode_t                     mParamFocusMode;
        bool                            mUseParmsFlashMode;
        bool                            mUseParmsFocusMode;

        MINT32                          mFlashMode;
        MINT32                          mFocusMode;

        bool                            mVideoStabilization;

        MINT32                          mExposureCompensation;
        bool                            mAutoExposureLock;
        bool                            mAutoWhiteBalanceLock;
        int                             mZoom;
        Vector<int>                     mRatio;

        // jpeg related
        MINT32                          mJpegThumbSize[2];
        MUINT8                          mJpegQuality, mJpegThumbQuality;
        MINT32                          mJpegRotation;

        bool                            mGgpsEnabled;
        MDOUBLE                         mGpsCoordinates[3];
        MINT64                          mGpsTimestamp;
        String8                         mGpsProcessingMethod;
    };


    virtual MERROR                  set(/*MtkCameraParameters const& paramString, */my_params& params);

    // Update passed-in request for common parameters
    virtual MERROR                  updateRequest(IMetadata *request, my_params const& params) const;

    // Add/update JPEG entries in metadata
    virtual MERROR                  updateRequestJpeg(IMetadata *request, my_params const& params) const;
    //static const unsigned int       NUM_ZOOM_STEPS = 100;


private:
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    String8 const                   mName;
    MINT32                          mLogLevel;
    MINT32                          mi4OpenId;

    //IMetadata::IEntry               staticInfo(MUINT32 tag) const;

    // static info
    MRect                           mActiveArray;
    IMetadata::IEntry               mEntryAvailableScene;
    IMetadata::IEntry               mEntrySceneModeOverrides;
    IMetadata::IEntry               maxDigitalZoom;

    // ConvertParamImp   params;
    sp<android::IParamsManager>              mpParamsMgr;
    //int                             previewWidth, previewHeight;

    //bool                            recordingHint;
    //int                             previewFormat;
    //int                             pictureWidth, pictureHeight;
    struct                          OverrideModes {
                                        MUINT8 flashMode;
                                        MUINT8 wbMode;
                                        MUINT8 focusMode;
                                    };
    DefaultKeyedVector<MUINT8, OverrideModes> mSceneModeOverrides;



};

/******************************************************************************
 *
 ******************************************************************************/
static int parse(const char *str, int *first/*, char delim*/, char **endptr = NULL)
{
    // Find the first integer.
    char *end;
    int w = (int)strtol(str, &end, 10);

    *first = w;

    if (endptr) {
        *endptr = end;
    }

    return 0;
}

/******************************************************************************
 *
 ******************************************************************************/
static void parseSizesList(const char *sizesStr, Vector<int> &values)
{
    MY_LOGD_IF(0,"%s\n", sizesStr);
    if (sizesStr == 0) {
        return;
    }

    char *sizeStartPtr = (char *)sizesStr;

    while (true) {
        int ivalue;
        int success = parse(sizeStartPtr, &ivalue, &sizeStartPtr);
        if (success == -1 || (*sizeStartPtr != ',' && *sizeStartPtr != '\0')) {
            MY_LOGE("Picture sizes string \"%s\" contains invalid character.", sizesStr);
            return;
        }
        values.push(ivalue);

        if (*sizeStartPtr == '\0') {
            return;
        }
        sizeStartPtr++;
    }
}


/******************************************************************************
 *
 ******************************************************************************/
ParamsManagerV3Imp::
ParamsManagerV3Imp(
    String8 const& rName,
    MINT32 cameraId,
    sp<android::IParamsManager> pParamsMgr
)
    : IParamsManagerV3()
    , mName(rName)
    , mi4OpenId(cameraId)
    , mpParamsMgr(pParamsMgr)
{
    char cLogLevel[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "1");
    mLogLevel = ::atoi(cLogLevel);
    //
    sp<IMetadataProvider> pMetadataProvider = NSMetadataProviderManager::valueFor(mi4OpenId);
    if( ! pMetadataProvider.get() ) {
            MY_LOGE("pMetadataProvider.get() is NULL");
    }
    IMetadata static_meta = pMetadataProvider->geMtktStaticCharacteristics();
    // static meta
    /*mEntryAvailableScene = static_meta.entryFor(MTK_CONTROL_AVAILABLE_SCENE_MODES);
    if(mEntryAvailableScene.isEmpty()) MY_LOGW("no tag: MTK_CONTROL_AVAILABLE_SCENE_MODES");

    mEntrySceneModeOverrides = static_meta.entryFor(MTK_CONTROL_SCENE_MODE_OVERRIDES);
    if(mEntrySceneModeOverrides.isEmpty()) MY_LOGW("no tag: MTK_CONTROL_SCENE_MODE_OVERRIDES");*/

    //maxDigitalZoom = static_meta.entryFor(MTK_SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
    //if(maxDigitalZoom.isEmpty()) MY_LOGW("no tag: MTK_SCALER_AVAILABLE_MAX_DIGITAL_ZOOM");

    IMetadata::IEntry activeA = static_meta.entryFor(MTK_SENSOR_INFO_ACTIVE_ARRAY_REGION);
    if( !activeA.isEmpty() ) {
        mActiveArray = activeA.itemAt(0, Type2Type<MRect>());
    }

    MUINT32 array_sizes = sizeof(CONVERTParam::scenModes) / sizeof(CONVERTParam::scenModes[0]);
    for (MUINT32 i = 0; i < array_sizes; i++)
    {
        MUINT8 availableSceneModes = CONVERTParam::scenModes[i];
        //if (overrideValue.size() >= 3*i)
        {
            // override
            OverrideModes rMode3A;
            rMode3A.flashMode  = CONVERTParam::overrideValue[3*i];
            rMode3A.wbMode     = CONVERTParam::overrideValue[3*i+1];
            rMode3A.focusMode  = CONVERTParam::overrideValue[3*i+2];
            mSceneModeOverrides.add(availableSceneModes, rMode3A);
            MY_LOGD1("Scene mode(%d) overrides AE(%d), AWB(%d), AF(%d)", i, rMode3A.flashMode, rMode3A.wbMode, rMode3A.focusMode);
        }
    }
}

/******************************************************************************
 *
 ******************************************************************************/
sp<IParamsManagerV3>
IParamsManagerV3::
createInstance(
    String8 const& rName,
    MINT32 cameraId,
    sp<android::IParamsManager> pParamsMgr
)
{
    return new ParamsManagerV3Imp(rName, cameraId, pParamsMgr);
}
/******************************************************************************
 *
 ******************************************************************************/
template <typename T>
inline MVOID
updateEntry(
    IMetadata* pMetadata,
    MUINT32 const tag,
    T const& val
)
{
    if( pMetadata == NULL ) {
        MY_LOGE("pMetadata == NULL");
        return;
    }

    IMetadata::IEntry entry(tag);
    entry.push_back(val, Type2Type<T>());
    pMetadata->update(tag, entry);
}
/******************************************************************************
 *
 ******************************************************************************/
template<typename T>
inline MVOID
updateEntryArray(
    IMetadata* pMetadata,
    MUINT32 const tag,
    const T* array,
    MUINT32 size
)
{
    IMetadata::IEntry entry(tag);
    for (MUINT32 i = size; i != 0; i--)
    {
        entry.push_back(*array++, Type2Type< T >());
    }
    pMetadata->update(tag, entry);
}

/******************************************************************************
 *
 ******************************************************************************/
bool
ParamsManagerV3Imp::
init()
{
#warning [TODO]
    return true;

}

/******************************************************************************
 *
 ******************************************************************************/
bool
ParamsManagerV3Imp::
uninit()
{
#warning [TODO]
    return true;

}


/******************************************************************************
 *
 ******************************************************************************/
status_t
ParamsManagerV3Imp::
updateRequestJpeg(
    IMetadata *request
) const
{
#warning [TODO]
    return OK;
}

/******************************************************************************
 *
 ******************************************************************************/
status_t
ParamsManagerV3Imp::
updateRequestEng(
    IMetadata *request
) const
{
#warning [TODO]
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
status_t
ParamsManagerV3Imp::
updateRequest(
   IMetadata *request
)
{
    my_params params;
    set(params);
    updateRequest(request, params);
    updateRequestJpeg(request, params);

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
ParamsManagerV3Imp::
set(/*MtkCameraParameters const& paramString, */my_params& params) {

    MtkCameraParameters newParams;// = paramString;

    newParams.unflatten(mpParamsMgr->flatten());

    //ConvertParamImp validatedParams(*this);

    // PREVIEW_SIZE
    //newParams.getPreviewSize(&validatedParams.previewWidth,
    //        &validatedParams.previewHeight);
    // RECORDING_HINT (always supported)
    //validatedParams.recordingHint = boolFromString(
    //    newParams.get(CameraParameters::KEY_RECORDING_HINT) );
    // PREVIEW_FPS_RANGE
    newParams.getPreviewFpsRange(&params.mPreviewFpsRange[0],
            &params.mPreviewFpsRange[1]);
    const int kFpsToApiScale = 1000;
    params.mPreviewFpsRange[0] /= kFpsToApiScale;
    params.mPreviewFpsRange[1] /= kFpsToApiScale;

    // PREVIEW_FORMAT
    //validatedParams.previewFormat =
    //        formatStringToEnum(newParams.getPreviewFormat());

    // PICTURE_SIZE
    //newParams.getPictureSize(&validatedParams.pictureWidth,
    //        &validatedParams.pictureHeight);
    // JPEG_THUMBNAIL_WIDTH/HEIGHT
    params.mJpegThumbSize[0] =
            newParams.getInt(CameraParameters::KEY_JPEG_THUMBNAIL_WIDTH);
    params.mJpegThumbSize[1] =
            newParams.getInt(CameraParameters::KEY_JPEG_THUMBNAIL_HEIGHT);

    // JPEG_THUMBNAIL_QUALITY
    int quality = newParams.getInt(CameraParameters::KEY_JPEG_THUMBNAIL_QUALITY);
    // also makes sure quality fits in uint8_t
    if (quality < 0 || quality > 100) {
        MY_LOGE("Requested JPEG thumbnail quality %d is not supported",
                 quality);
    }
    params.mJpegThumbQuality = quality;

    // JPEG_QUALITY
    quality = newParams.getInt(CameraParameters::KEY_JPEG_QUALITY);
    // also makes sure quality fits in uint8_t
    if (quality < 0 || quality > 100) {
        MY_LOGE("Requested JPEG quality %d is not supported",
                    quality);
    }
    params.mJpegQuality = quality;

    // ROTATION
    params.mJpegRotation =
            newParams.getInt(CameraParameters::KEY_ROTATION);
    if (params.mJpegRotation != 0 &&
            params.mJpegRotation != 90 &&
            params.mJpegRotation != 180 &&
            params.mJpegRotation != 270) {
        MY_LOGE("Requested picture rotation angle %d is not supported",
                params.mJpegRotation);
        return BAD_VALUE;
    }

    // GPS
    const char *gpsLatStr =
            newParams.get(CameraParameters::KEY_GPS_LATITUDE);
    if (gpsLatStr != NULL) {
        const char *gpsLongStr =
                newParams.get(CameraParameters::KEY_GPS_LONGITUDE);
        const char *gpsAltitudeStr =
                newParams.get(CameraParameters::KEY_GPS_ALTITUDE);
        const char *gpsTimeStr =
                newParams.get(CameraParameters::KEY_GPS_TIMESTAMP);
        const char *gpsProcMethodStr =
                newParams.get(CameraParameters::KEY_GPS_PROCESSING_METHOD);
        if (gpsLongStr == NULL ||
                gpsAltitudeStr == NULL ||
                gpsTimeStr == NULL ||
                gpsProcMethodStr == NULL) {
            MY_LOGE("Incomplete set of GPS parameters provided");
            return BAD_VALUE;
        }
        char *endPtr;
        params.mGpsCoordinates[0] = strtod(gpsLatStr, &endPtr);
        if (endPtr == gpsLatStr) {
            MY_LOGE("Malformed GPS latitude: %s", gpsLatStr);
            return BAD_VALUE;
        }

        params.mGpsCoordinates[1] = strtod(gpsLongStr, &endPtr);
        if (endPtr == gpsLongStr) {
            ALOGE("Malformed GPS longitude: %s", gpsLongStr);
            return BAD_VALUE;
        }

        params.mGpsCoordinates[2] = strtod(gpsAltitudeStr, &endPtr);
        if (endPtr == gpsAltitudeStr) {
            ALOGE("Malformed GPS altitude: %s", gpsAltitudeStr);
            return BAD_VALUE;
        }

        params.mGpsTimestamp = strtoll(gpsTimeStr, &endPtr, 10);
        if (endPtr == gpsTimeStr) {
            ALOGE("Malformed GPS timestamp: %s", gpsTimeStr);
            return BAD_VALUE;
        }
        params.mGpsProcessingMethod = gpsProcMethodStr;
        /*printf("gps method:");
        for(size_t i = 0; i < validatedParams.mGpsProcessingMethod.size(); i++ ) {
            printf("%c ", *(validatedParams.mGpsProcessingMethod+i));
        }
        printf("\n");*/
        params.mGgpsEnabled = true;
    } else {
        params.mGgpsEnabled = false;
    }


    // EFFECT
    params.mEffectMode = effectModeStringToEnum(
        newParams.get(CameraParameters::KEY_EFFECT) );
    MY_LOGD1("mEffectMode = %d", params.mEffectMode);

    // ANTIBANDING
    params.mAntibandingMode = abModeStringToEnum(
        newParams.get(CameraParameters::KEY_ANTIBANDING) );
    MY_LOGD1("mAntibandingMode = %d", params.mAntibandingMode);

    // SCENE_MODE
    params.mSceneMode = sceneModeStringToEnum(
        newParams.get(CameraParameters::KEY_SCENE_MODE) );
    MY_LOGD1("Scene mode =%s", newParams.get(CameraParameters::KEY_SCENE_MODE));
    MY_LOGD1("Scene mode =%d", params.mSceneMode);

    //Scene mode override
    //const IMetadata::IEntry& entryAvailableScene = staticInfo(MTK_CONTROL_AVAILABLE_SCENE_MODES);

    /*if (mEntryAvailableScene.isEmpty())
    {
        MY_LOGW("MTK_CONTROL_AVAILABLE_SCENE_MODES are not defined");
    }else{
        if (mSceneModeOverrides.isEmpty())
        {
            MY_LOGD1("mSceneModeOverrides is empty");

            //const IMetadata::IEntry& mEntrySceneModeOverrides  =
            //            staticInfo(MTK_CONTROL_SCENE_MODE_OVERRIDES);
            //const IMetadata::IEntry& mEntrySceneModeOverrides  = static_meta.entryFor(MTK_CONTROL_SCENE_MODE_OVERRIDES);
            if (mEntrySceneModeOverrides.isEmpty())
            {
                MY_LOGE("MTK_CONTROL_SCENE_MODE_OVERRIDES are not defined");
                return BAD_VALUE;
            }
            MUINT32 i;
            for (i = 0; i < mEntryAvailableScene.count(); i++)
            {
                MUINT8 availableSceneModes = mEntryAvailableScene.itemAt(i, Type2Type< MUINT8 >());
                if (mEntrySceneModeOverrides.count() >= 3*i)
                {
                    // override
                    OverrideModes rMode3A;
                    rMode3A.flashMode  = mEntrySceneModeOverrides.itemAt(3*i,   Type2Type< MUINT8 >());
                    rMode3A.wbMode     = mEntrySceneModeOverrides.itemAt(3*i+1, Type2Type< MUINT8 >());
                    rMode3A.focusMode  = mEntrySceneModeOverrides.itemAt(3*i+2, Type2Type< MUINT8 >());
                    mSceneModeOverrides.add(availableSceneModes, rMode3A);
                    MY_LOGD1("Scene mode(%d) overrides AE(%d), AWB(%d), AF(%d)", i, rMode3A.flashMode, rMode3A.wbMode, rMode3A.focusMode);
                }
            }
        }
    }*/


    bool sceneModeSet =
            params.mSceneMode != MTK_CONTROL_SCENE_MODE_DISABLED;
    MY_LOGD1("sceneModeSet = %d", sceneModeSet);
    // FLASH_MODE
    if (sceneModeSet) {
        params.mUseParmsFlashMode = false;
        params.mFlashMode =
                mSceneModeOverrides.
                        valueFor(params.mSceneMode).flashMode;
        MY_LOGD1("mFlashMode = %d", params.mFlashMode);
    } else {
        params.mUseParmsFlashMode = true;
        params.mParamFlashMode = flashModeStringToEnum(newParams.get(CameraParameters::KEY_FLASH_MODE));
        params.mFlashMode = MTK_CONTROL_AE_MODE_ON_AUTO_FLASH;
    }
    //MY_LOGD("sceneModeSet = %d mUseParmsFlashMode = %d\n", sceneModeSet, validatedParams.mUseParmsFlashMode);

    // WHITE_BALANCE
    if (sceneModeSet) {
        params.mWbMode =
                mSceneModeOverrides.
                        valueFor(params.mSceneMode).wbMode;
        MY_LOGD1("mWbMode = %d", params.mWbMode);
    } else {
        params.mWbMode = MTK_CONTROL_AWB_MODE_OFF;
    }
    if (params.mWbMode == MTK_CONTROL_AWB_MODE_OFF) {
        params.mWbMode = wbModeStringToEnum(
            newParams.get(CameraParameters::KEY_WHITE_BALANCE) );
    }

    // FOCUS_MODE
    if (sceneModeSet) {
        params.mUseParmsFocusMode = false;
        params.mFocusMode =
                mSceneModeOverrides.
                        valueFor(params.mSceneMode).focusMode;
        MY_LOGD1("mFocusMode = %d", params.mFocusMode);
    } else {
        params.mUseParmsFocusMode = true;
        params.mParamFocusMode = focusModeStringToEnum(newParams.get(CameraParameters::KEY_FOCUS_MODE));
        params.mFocusMode = MTK_CONTROL_AF_MODE_OFF;
    }

    // FOCUS_AREAS
    //res = parseAreas(newParams.get(CameraParameters::KEY_FOCUS_AREAS),
    //        &validatedParams.focusingAreas);

    // EXPOSURE_COMPENSATION
    params.mExposureCompensation =
        newParams.getInt(CameraParameters::KEY_EXPOSURE_COMPENSATION);

    // AUTO_EXPOSURE_LOCK (always supported)
    params.mAutoExposureLock = boolFromString(
        newParams.get(CameraParameters::KEY_AUTO_EXPOSURE_LOCK));

    // AUTO_WHITEBALANCE_LOCK (always supported)
    params.mAutoWhiteBalanceLock = boolFromString(
        newParams.get(CameraParameters::KEY_AUTO_WHITEBALANCE_LOCK));

    // METERING_AREAS
    // res = parseAreas(newParams.get(CameraParameters::KEY_METERING_AREAS),
    //        &validatedParams.meteringAreas);
    // ZOOM
    params.mZoom = newParams.getInt(CameraParameters::KEY_ZOOM);
    MY_LOGD1("mZoom = %d", params.mZoom);

    // ZOOM
    const char *zoomRatios = newParams.get(CameraParameters::KEY_ZOOM_RATIOS);
    //validatedParams.mRatio = newParams.get(CameraParameters::KEY_ZOOM_RATIOS);
    parseSizesList(zoomRatios, params.mRatio);
    MY_LOGD1("mRatio = %s ", zoomRatios);
    MY_LOGD1("mRatio size = %d ", params.mRatio.size());
    for(size_t ii = 0; ii < params.mRatio.size(); ii++) {
        MY_LOGD1("%d, ", params.mRatio[ii]);
    }
    //MY_LOGD("mRatio = %d", validatedParams.mRatio);
    // VIDEO_SIZE
    //newParams.getVideoSize(&validatedParams.videoWidth,
    //        &validatedParams.videoHeight);

    // VIDEO_STABILIZATION
    params.mVideoStabilization = boolFromString(
        newParams.get(CameraParameters::KEY_VIDEO_STABILIZATION) );

    // LIGHTFX
    //validatedParams.lightFx = lightFxStringToEnum(
    //    newParams.get(CameraParameters::KEY_LIGHTFX));
    //*this = validatedParams;
    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
ParamsManagerV3Imp::
updateRequest(IMetadata *request, my_params const& params) const
{
    IMetadata::IEntry entry =
            request->entryFor(MTK_CONTROL_CAPTURE_INTENT);
    if (entry.isEmpty()) return BAD_VALUE;

    if (entry.itemAt(0, Type2Type<MUINT8>()) != MTK_CONTROL_CAPTURE_INTENT_STILL_CAPTURE) {
        updateEntryArray<MINT32>(request, MTK_CONTROL_AE_TARGET_FPS_RANGE, params.mPreviewFpsRange, 2);
    }

    MUINT8 reqWbLock = params.mAutoWhiteBalanceLock ?
            MTK_CONTROL_AWB_LOCK_ON : MTK_CONTROL_AWB_LOCK_OFF;
    updateEntry<MUINT8>(request , MTK_CONTROL_AWB_LOCK , reqWbLock);

    updateEntry<MUINT8>(request , MTK_CONTROL_EFFECT_MODE , params.mEffectMode);

    updateEntry<MUINT8>(request , MTK_CONTROL_AE_ANTIBANDING_MODE , params.mAntibandingMode);

    bool sceneModeActive =
            params.mSceneMode != (MUINT8)MTK_CONTROL_SCENE_MODE_DISABLED;
    MUINT8 reqControlMode = MTK_CONTROL_MODE_AUTO;
    if (sceneModeActive) {
        reqControlMode = MTK_CONTROL_MODE_USE_SCENE_MODE;
    }
    updateEntry<MUINT8>(request , MTK_CONTROL_MODE , reqControlMode);

    MUINT8 reqSceneMode =
            sceneModeActive ? params.mSceneMode :
            (MUINT8)MTK_CONTROL_SCENE_MODE_DISABLED;
    updateEntry<MUINT8>(request , MTK_CONTROL_SCENE_MODE , reqSceneMode);

    MUINT8 reqFlashMode = MTK_FLASH_MODE_OFF;
    MUINT8 reqAeMode = params.mFlashMode;
    if(params.mUseParmsFlashMode) {
        switch (params.mParamFlashMode) {
            case ParamsManagerV3Imp::FLASH_MODE_OFF:
                reqAeMode = MTK_CONTROL_AE_MODE_ON; break;
            case ParamsManagerV3Imp::FLASH_MODE_AUTO:
                reqAeMode = MTK_CONTROL_AE_MODE_ON_AUTO_FLASH; break;
            case ParamsManagerV3Imp::FLASH_MODE_ON:
                reqAeMode = MTK_CONTROL_AE_MODE_ON_ALWAYS_FLASH; break;
            case ParamsManagerV3Imp::FLASH_MODE_TORCH:
                reqAeMode = MTK_CONTROL_AE_MODE_ON;
                reqFlashMode = MTK_FLASH_MODE_TORCH;
                break;
            case ParamsManagerV3Imp::FLASH_MODE_RED_EYE:
                reqAeMode = MTK_CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE; break;
            default:
                MY_LOGE("Unknown flash mode %d", params.mParamFlashMode);
                return BAD_VALUE;
        }
    }
    updateEntry<MUINT8>(request , MTK_FLASH_MODE , reqFlashMode);

    updateEntry<MUINT8>(request , MTK_CONTROL_AE_MODE , reqAeMode);

    MUINT8 reqAeLock = params.mAutoExposureLock ?
            MTK_CONTROL_AE_LOCK_ON : MTK_CONTROL_AE_LOCK_OFF;
    updateEntry<MUINT8>(request , MTK_CONTROL_AE_LOCK , reqAeLock);

    updateEntry<MUINT8>(request , MTK_CONTROL_AWB_MODE , params.mWbMode);

    float reqFocusDistance = 0; // infinity focus in diopters
    MUINT8 reqFocusMode = params.mFocusMode;
    if(params.mUseParmsFocusMode) {
        switch (params.mParamFocusMode) {
            case ParamsManagerV3Imp::FOCUS_MODE_AUTO:
            case ParamsManagerV3Imp::FOCUS_MODE_MACRO:
            case ParamsManagerV3Imp::FOCUS_MODE_CONTINUOUS_VIDEO:
            case ParamsManagerV3Imp::FOCUS_MODE_CONTINUOUS_PICTURE:
            case ParamsManagerV3Imp::FOCUS_MODE_EDOF:
                reqFocusMode = params.mParamFocusMode;
                break;
            case ParamsManagerV3Imp::FOCUS_MODE_INFINITY:
            case ParamsManagerV3Imp::FOCUS_MODE_FIXED:
                reqFocusMode = MTK_CONTROL_AF_MODE_OFF;
                break;
        default:
                MY_LOGE("Unknown mParamFocusMode mode %d", params.mParamFocusMode);
                return BAD_VALUE;
        }
    }
    updateEntry<MFLOAT>(request , MTK_LENS_FOCUS_DISTANCE , reqFocusDistance);

    updateEntry<MUINT8>(request , MTK_CONTROL_AF_MODE , reqFocusMode);

    updateEntry<MINT32>(request , MTK_CONTROL_AE_EXPOSURE_COMPENSATION , params.mExposureCompensation);

    MRect reqCropRegion = calculateCropRegion();
    updateEntry<MRect>(request , MTK_SCALER_CROP_REGION , reqCropRegion);

    uint8_t reqVstabMode = params.mVideoStabilization ?
            MTK_CONTROL_VIDEO_STABILIZATION_MODE_ON :
            MTK_CONTROL_VIDEO_STABILIZATION_MODE_OFF;
    updateEntry<MUINT8>(request , MTK_CONTROL_VIDEO_STABILIZATION_MODE , reqVstabMode);

    return OK;
}


/******************************************************************************
 *
 ******************************************************************************/
MERROR
ParamsManagerV3Imp::
updateRequestJpeg(IMetadata *request, my_params const& params) const
{
    status_t res = OK;

    //IMetadata::IEntry thumbSizeEntry(MTK_JPEG_THUMBNAIL_SIZE);
    //thumbSizeEntry.push_back(MSize(mJpegThumbSize[0],mJpegThumbSize[1]), Type2Type< MSize >());
    updateEntry<MSize>(request , MTK_JPEG_THUMBNAIL_SIZE , MSize(params.mJpegThumbSize[0], params.mJpegThumbSize[1]));
    //request->update(MTK_JPEG_THUMBNAIL_SIZE, thumbSizeEntry);

    updateEntry<MUINT8>(request , MTK_JPEG_THUMBNAIL_QUALITY , params.mJpegThumbQuality);

    updateEntry<MUINT8>(request , MTK_JPEG_QUALITY , params.mJpegQuality);

    updateEntry<MINT32>(request , MTK_JPEG_ORIENTATION , params.mJpegRotation);

    if (params.mGgpsEnabled) {
        updateEntryArray(request, MTK_JPEG_GPS_COORDINATES, params.mGpsCoordinates, 3);

        updateEntry<MINT64>(request , MTK_JPEG_GPS_TIMESTAMP , params.mGpsTimestamp);

        MUINT8 uGPSProcessingMethod[64]={0};
        for( size_t i = 0; i < params.mGpsProcessingMethod.size(); i++ ) {
            uGPSProcessingMethod[i] = *(params.mGpsProcessingMethod+i);
        }
        //uGPSProcessingMethod[63] = '\0'; //null-terminating
        updateEntryArray<MUINT8>(request , MTK_JPEG_GPS_PROCESSING_METHOD , uGPSProcessingMethod, 64);

    } /*else {
        res = request->remove(MTK_JPEG_GPS_COORDINATES);
        if (res != OK) return res;
        res = request->remove(MTK_JPEG_GPS_TIMESTAMP);
        if (res != OK) return res;
        res = request->remove(MTK_JPEG_GPS_PROCESSING_METHOD);
        if (res != OK) return res;
    }*/
    return res;
}


/******************************************************************************
 *
 ******************************************************************************/
#if 0
IMetadata::IEntry ParamsManagerV3Imp::staticInfo(uint32_t tag) const {

    if( ! mpMetadataProvider.get() ) {
            MY_LOGE(" ! mpMetadataProvider.get() ");
            return DEAD_OBJECT;
        }
    IMetadata static_meta = mpMetadataProvider->geMtktStaticCharacteristics();
    IMetadata::IEntry entry = static_meta.entryFor(tag);
    return entry;
}
#endif
/******************************************************************************
 *
 ******************************************************************************/
ParamsManagerV3Imp::ParamsManagerV3Imp::flashMode_t ParamsManagerV3Imp::flashModeStringToEnum(
        const char *flashMode) {
    return
        !flashMode ?
            ParamsManagerV3Imp::FLASH_MODE_OFF :
        !strcmp(flashMode, CameraParameters::FLASH_MODE_OFF) ?
            ParamsManagerV3Imp::FLASH_MODE_OFF :
        !strcmp(flashMode, CameraParameters::FLASH_MODE_AUTO) ?
            ParamsManagerV3Imp::FLASH_MODE_AUTO :
        !strcmp(flashMode, CameraParameters::FLASH_MODE_ON) ?
            ParamsManagerV3Imp::FLASH_MODE_ON :
        !strcmp(flashMode, CameraParameters::FLASH_MODE_RED_EYE) ?
            ParamsManagerV3Imp::FLASH_MODE_RED_EYE :
        !strcmp(flashMode, CameraParameters::FLASH_MODE_TORCH) ?
            ParamsManagerV3Imp::FLASH_MODE_TORCH :
        ParamsManagerV3Imp::FLASH_MODE_INVALID;
}

/******************************************************************************
 *
 ******************************************************************************/
ParamsManagerV3Imp::ParamsManagerV3Imp::focusMode_t ParamsManagerV3Imp::focusModeStringToEnum(
        const char *focusMode) {
    return
        !focusMode ?
            ParamsManagerV3Imp::FOCUS_MODE_INVALID :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_AUTO) ?
            ParamsManagerV3Imp::FOCUS_MODE_AUTO :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_INFINITY) ?
            ParamsManagerV3Imp::FOCUS_MODE_INFINITY :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_MACRO) ?
            ParamsManagerV3Imp::FOCUS_MODE_MACRO :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_FIXED) ?
            ParamsManagerV3Imp::FOCUS_MODE_FIXED :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_EDOF) ?
            ParamsManagerV3Imp::FOCUS_MODE_EDOF :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_CONTINUOUS_VIDEO) ?
            ParamsManagerV3Imp::FOCUS_MODE_CONTINUOUS_VIDEO :
        !strcmp(focusMode, CameraParameters::FOCUS_MODE_CONTINUOUS_PICTURE) ?
            ParamsManagerV3Imp::FOCUS_MODE_CONTINUOUS_PICTURE :
        ParamsManagerV3Imp::FOCUS_MODE_INVALID;
}


/******************************************************************************
 *
 ******************************************************************************/
int ParamsManagerV3Imp::wbModeStringToEnum(const char *wbMode) {
    return
        !wbMode ?
            MTK_CONTROL_AWB_MODE_AUTO :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_AUTO) ?
            MTK_CONTROL_AWB_MODE_AUTO :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_INCANDESCENT) ?
            MTK_CONTROL_AWB_MODE_INCANDESCENT :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_FLUORESCENT) ?
            MTK_CONTROL_AWB_MODE_FLUORESCENT :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_WARM_FLUORESCENT) ?
            MTK_CONTROL_AWB_MODE_WARM_FLUORESCENT :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_DAYLIGHT) ?
            MTK_CONTROL_AWB_MODE_DAYLIGHT :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_CLOUDY_DAYLIGHT) ?
            MTK_CONTROL_AWB_MODE_CLOUDY_DAYLIGHT :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_TWILIGHT) ?
            MTK_CONTROL_AWB_MODE_TWILIGHT :
        !strcmp(wbMode, CameraParameters::WHITE_BALANCE_SHADE) ?
            MTK_CONTROL_AWB_MODE_SHADE :
        -1;
}

/******************************************************************************
 *
 ******************************************************************************/
bool ParamsManagerV3Imp::boolFromString(const char *boolStr) {
    return !boolStr ? false :
        !strcmp(boolStr, CameraParameters::TRUE) ? true :
        false;
}


/******************************************************************************
 *
 ******************************************************************************/
int ParamsManagerV3Imp::effectModeStringToEnum(const char *effectMode) {
    return
        !effectMode ?
            MTK_CONTROL_EFFECT_MODE_OFF :
        !strcmp(effectMode, CameraParameters::EFFECT_NONE) ?
            MTK_CONTROL_EFFECT_MODE_OFF :
        !strcmp(effectMode, CameraParameters::EFFECT_MONO) ?
            MTK_CONTROL_EFFECT_MODE_MONO :
        !strcmp(effectMode, CameraParameters::EFFECT_NEGATIVE) ?
            MTK_CONTROL_EFFECT_MODE_NEGATIVE :
        !strcmp(effectMode, CameraParameters::EFFECT_SOLARIZE) ?
            MTK_CONTROL_EFFECT_MODE_SOLARIZE :
        !strcmp(effectMode, CameraParameters::EFFECT_SEPIA) ?
            MTK_CONTROL_EFFECT_MODE_SEPIA :
        !strcmp(effectMode, CameraParameters::EFFECT_POSTERIZE) ?
            MTK_CONTROL_EFFECT_MODE_POSTERIZE :
        !strcmp(effectMode, CameraParameters::EFFECT_WHITEBOARD) ?
            MTK_CONTROL_EFFECT_MODE_WHITEBOARD :
        !strcmp(effectMode, CameraParameters::EFFECT_BLACKBOARD) ?
            MTK_CONTROL_EFFECT_MODE_BLACKBOARD :
        !strcmp(effectMode, CameraParameters::EFFECT_AQUA) ?
            MTK_CONTROL_EFFECT_MODE_AQUA :
        !strcmp(effectMode, MtkCameraParameters::EFFECT_SEPIA_BLUE) ?
            MTK_CONTROL_EFFECT_MODE_SEPIABLUE :
        !strcmp(effectMode, MtkCameraParameters::EFFECT_SEPIA_GREEN) ?
            MTK_CONTROL_EFFECT_MODE_SEPIAGREEN :
        -1;
}


/******************************************************************************
 *
 ******************************************************************************/
MINT32 ParamsManagerV3Imp::abModeStringToEnum(const char *abMode) {
    return
        !abMode ?
            MTK_CONTROL_AE_ANTIBANDING_MODE_AUTO :
        !strcmp(abMode, CameraParameters::ANTIBANDING_AUTO) ?
            MTK_CONTROL_AE_ANTIBANDING_MODE_AUTO :
        !strcmp(abMode, CameraParameters::ANTIBANDING_OFF) ?
            MTK_CONTROL_AE_ANTIBANDING_MODE_OFF :
        !strcmp(abMode, CameraParameters::ANTIBANDING_50HZ) ?
            MTK_CONTROL_AE_ANTIBANDING_MODE_50HZ :
        !strcmp(abMode, CameraParameters::ANTIBANDING_60HZ) ?
            MTK_CONTROL_AE_ANTIBANDING_MODE_60HZ :
        -1;
}


/******************************************************************************
 *
 ******************************************************************************/
MINT32 ParamsManagerV3Imp::sceneModeStringToEnum(const char *sceneMode) {
    return
        !sceneMode ?
            MTK_CONTROL_SCENE_MODE_DISABLED :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_AUTO) ?
            MTK_CONTROL_SCENE_MODE_DISABLED :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_ACTION) ?
            MTK_CONTROL_SCENE_MODE_ACTION :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_PORTRAIT) ?
            MTK_CONTROL_SCENE_MODE_PORTRAIT :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_LANDSCAPE) ?
            MTK_CONTROL_SCENE_MODE_LANDSCAPE :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_NIGHT) ?
            MTK_CONTROL_SCENE_MODE_NIGHT :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_NIGHT_PORTRAIT) ?
            MTK_CONTROL_SCENE_MODE_NIGHT_PORTRAIT :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_THEATRE) ?
            MTK_CONTROL_SCENE_MODE_THEATRE :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_BEACH) ?
            MTK_CONTROL_SCENE_MODE_BEACH :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_SNOW) ?
            MTK_CONTROL_SCENE_MODE_SNOW :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_SUNSET) ?
            MTK_CONTROL_SCENE_MODE_SUNSET :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_STEADYPHOTO) ?
            MTK_CONTROL_SCENE_MODE_STEADYPHOTO :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_FIREWORKS) ?
            MTK_CONTROL_SCENE_MODE_FIREWORKS :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_SPORTS) ?
            MTK_CONTROL_SCENE_MODE_SPORTS :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_PARTY) ?
            MTK_CONTROL_SCENE_MODE_PARTY :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_CANDLELIGHT) ?
            MTK_CONTROL_SCENE_MODE_CANDLELIGHT :
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_BARCODE) ?
            MTK_CONTROL_SCENE_MODE_BARCODE:
        !strcmp(sceneMode, CameraParameters::SCENE_MODE_HDR) ?
            MTK_CONTROL_SCENE_MODE_HDR:
        !strcmp(sceneMode, MtkCameraParameters::SCENE_MODE_NORMAL) ?
            MTK_CONTROL_SCENE_MODE_NORMAL:
        -1;
}

/******************************************************************************
 *
 ******************************************************************************/
MRect ParamsManagerV3Imp::calculateCropRegion() const {

    float zoomLeft, zoomTop, zoomWidth, zoomHeight;
    uint32_t r = mpParamsMgr->getZoomRatio();

    float zoomRatio = (r / 100.0) ;
    //float zoomRatio = ((float)mRatio[mZoom] / 100.0) ;
    //MY_LOGD_IF(1,"@@@mpParamsMgr->getZoomRatio(%d) mpParamsMgr->getZoomRatioByIndex=(%d)",r,mpParamsMgr->getZoomRatioByIndex(mZoom));
    MY_LOGD_IF(1,"@@@mpParamsMgr->getInt(CameraParameters::KEY_ZOOM) = %d",mpParamsMgr->getInt(CameraParameters::KEY_ZOOM));
    MY_LOGD1("ratio=%f, activeWidth=%d, activeHeight=%d",
          zoomRatio, mActiveArray.s.w, mActiveArray.s.h);

    zoomWidth = mActiveArray.s.w / zoomRatio;
    zoomHeight = mActiveArray.s.h / zoomRatio;

    zoomLeft = (mActiveArray.s.w - zoomWidth) / 2;
    zoomTop = (mActiveArray.s.h - zoomHeight) / 2;

    MY_LOGD1("@@@Crop region calculated (x=%d,y=%d,w=%f,h=%f)",
        (int32_t)zoomLeft, (int32_t)zoomTop, zoomWidth, zoomHeight);

    return MRect(MPoint(zoomLeft, zoomTop),MSize(zoomWidth, zoomHeight));
}
