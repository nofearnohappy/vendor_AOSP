/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
 *     TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/

#define LOG_TAG "stereo_hal"

#include <stdlib.h>     // for rand()

#include <cutils/properties.h>

#include <mtkcam/Log.h>
#include <cutils/log.h>            // For ALOG*
#include "MTKStereoKernelScenario.h"    // For MTKStereoKernel class/INPUT_FORMAT_ENUM/STEREO_KERNEL_TUNING_PARA_STRUCT. Must be included before stereo_hal_base.h/stereo_hal.h.
#include "MTKStereoKernel.h"    // For MTKStereoKernel class/INPUT_FORMAT_ENUM/STEREO_KERNEL_TUNING_PARA_STRUCT. Must be included before stereo_hal_base.h/stereo_hal.h.
#include "stereo_hal.h"         // For StereoHal class.
#include "pip_hal.h"            // For query max frame rate.

#include "IHalSensor.h"

#include "nvbuf_util.h"
#include "camera_custom_stereo.h"  // For CUST_STEREO_* definitions., e.g. STEREO_BASELINE
#include "camera/MtkCameraParameters.h"
#include <ui/FramebufferNativeWindow.h>
#include <ui/GraphicBuffer.h>
#include "aaa_hal_common.h"     // For DAF_TBL_STRUCT.
#include <math.h>

using namespace NSCam;
using namespace android;
using android::Mutex;           // For android::Mutex in stereo_hal.h.

/**************************************************************************
 *                      D E F I N E S / M A C R O S                       *
 **************************************************************************/
#define ENABLE_GPU                  1

/*******************************************************************************
*
********************************************************************************/
#define STEREO_HAL_DEBUG

#ifdef STEREO_HAL_DEBUG    // Enable debug log.

#undef __func__
#define __func__ __FUNCTION__

#define MY_LOGD(fmt, arg...)    ALOGD("[%s]" fmt, __func__, ##arg)
#define MY_LOGI(fmt, arg...)    ALOGI("[%s]" fmt, __func__, ##arg)
#define MY_LOGW(fmt, arg...)    ALOGW("[%s] WRN(%5d):" fmt, __func__, __LINE__, ##arg)
#define MY_LOGE(fmt, arg...)    ALOGE("[%s] %s ERROR(%5d):" fmt, __func__,__FILE__, __LINE__, ##arg)

#else   // Disable debug log.
#define MY_LOGD(a,...)
#define MY_LOGI(a,...)
#define MY_LOGW(a,...)
#define MY_LOGE(a,...)
#endif  // STEREO_HAL_DEBUG

/**************************************************************************
 *     E N U M / S T R U C T / T Y P E D E F    D E C L A R A T I O N     *
 **************************************************************************/

/**************************************************************************
 *                 E X T E R N A L    R E F E R E N C E S                 *
 **************************************************************************/

/**************************************************************************
 *                         G L O B A L    D A T A                         *
 **************************************************************************/
STEREO_KERNEL_TUNING_PARA_STRUCT        gStereoKernelTuningParaInfo;
static STEREO_KERNEL_SET_ENV_INFO_STRUCT       gStereoKernelInitInfo;
STEREO_KERNEL_SET_WORK_BUF_INFO_STRUCT  gStereoKernelWorkBufInfo;   // Working Buffer information.
STEREO_KERNEL_SET_PROC_INFO_STRUCT      gStereoKernelProcInfo;      // Setprocess information. Mostly addresses.
static STEREO_KERNEL_RESULT_STRUCT             gStereoKernelResultInfo;    // STEREO algorithm results.

// MINT32 gDataArray[MAXIMUM_NVRAM_CAMERA_GEOMETRY_FILE_SIZE];    // STORE the Learning Information / One-Shot Calibration

MFLOAT main1_FOV_horizontal     = 55;//55; //update Sava for FOV
MFLOAT main1_FOV_vertical       = 65;
//MUINT main1_Capture_Width       = 4096;
//MUINT main1_Capture_Height      = 3072;
//MUINT main1_Capture_Offsetx     = 0;
//MUINT main1_Capture_Offsety     = 0;
MFLOAT main2_FOV_horizontal     = 75; //update Sava for FOV
MFLOAT main2_FOV_vertical       = 65;
//MUINT main2_Capture_Width       = 4096;
//MUINT main2_Capture_Height      = 3072;
//MUINT main2_Capture_Offsetx     = 0;
//MUINT main2_Capture_Offsety     = 0;
const MUINT PV_720P_W           = 1280;
const MUINT PV_720P_H           = 720;
const MUINT FE_BLOCK_NUM        = 2;
//MUINT FE_IMAGE_WIDTH            = 1536;
//MUINT FE_IMAGE_HEIGHT           = 864;
//MUINT RGBA_IMAGE_WIDTH          = 160;
//MUINT RGBA_IMAGE_HEIGHT         = 90;
//MUINT SensorIs5M                = 2000;

static MUINT gnRefocusOffsetX  = 256;  //Image width  + gnRefocusOffsetX must be 16-align
static MUINT gnRefocusOffsetY  =  72;  //Image height + gnRefocusOffsetY must be 16-align, 16:9->72, 4:3->64
//
// Stereo ratio
const MFLOAT DEFAULT_CROP_PRERCENT_X    = 0.0f;     // unit: %
const MFLOAT DEFAULT_CROP_PRERCENT_Y    = 0.0f;     // unit: %
const MFLOAT STEREO_FACTOR              = 1.0f;     // unit: %
const MFLOAT RRZ_CAPIBILITY             = 0.4f;
static STEREO_RATIO_E g_seImageRatio   = eRatio_16_9;
static IMAGE_RESOLUTION_INFO_STRUCT g_sResolutionInfo;

//
static DAF_TBL_STRUCT *g_prDafTbl = NULL;
//
/**************************************************************************
 *       P R I V A T E    F U N C T I O N    D E C L A R A T I O N        *
 **************************************************************************/
#define ENABLE_ALGO             1
#define LEARNING_TIMEOUT        5
#define INVALID_QUERY           0xFFFFFFFF

#define STEREO_PROPERTY_PREFIX  "debug.STEREO."
#define PROPERTY_ENABLE_VERIFY  STEREO_PROPERTY_PREFIX"enable_verify"
#define PERPERTY_ENABLE_CC      STEREO_PROPERTY_PREFIX"enable_cc"
#define PROPERTY_ALGO_BEBUG     STEREO_PROPERTY_PREFIX"dbgdump"

/**
 * @param PROPERTY_NAME The property to query, e.g. "debug.STEREO.enable_verify"
 * @param DEFAULT Default value of the property. If not set, it will be 0.
 * @return: -1: property not been set; otherwise the property value
 */
inline const int checkStereoProperty(const char *PROPERTY_NAME, const bool HAS_DEFAULT=true, const int DEFAULT=0)
{
    char val[PROPERTY_VALUE_MAX];
    ::memset(val, 0, sizeof(char)*PROPERTY_VALUE_MAX);

    int len = 0;
    if(HAS_DEFAULT) {
        char strDefault[PROPERTY_VALUE_MAX];
        sprintf(strDefault, "%d", DEFAULT);
        len = property_get(PROPERTY_NAME, val, strDefault);
    } else {
        len = property_get(PROPERTY_NAME, val, NULL);
    }

    if(len <= 0) {
        return -1; //property not exist
    }

    return (!strcmp(val, "1"));
}
/*******************************************************************************
*
********************************************************************************/
StereoHal::
StereoHal()
    : mUsers(0)
    , m_pStereoDrv(NULL)
    , mScenario(STEREO_SCENARIO_UNKNOWN) // default value
    , mAlgoSize()
    , mMainSize()
    , mFEImgSize(MSize(PV_720P_W, PV_720P_H)*STEREO_FACTOR)
    , m_pDebugInforkBuf(NULL)
    , m_uSensorRelativePosition(0)  // L-R 0: main-minor , 1: minor-main
    , m_fPrecropAngleForCapture(10.0f)
    , m_nMain1SensorIdx(0)
    , m_fMinorCamOffsetXRatio(1.0f/2.0f)
    , mpHal3A(NULL)
    , mpStereoDepthHal(NULL)
    , mRefocusMode(MFALSE)
    , mDepthAF(MFALSE)
    , mDistanceMeasure(MFALSE)
    , m_pWorkBuf(NULL)
    , m_uDebugInfotSize(0)
    , m_nCapOrientation(0)
    , m_pVoidGeoData(NULL)
    , m_nMain2SensorIdx(2)
{
    pthread_mutex_init(&mResultInfoLock, NULL);
    pthread_mutex_init(&mInitDestroyLock, NULL);
    pthread_mutex_init(&mRunLock, NULL);

    ::memset(&mFDInfo, 0, sizeof(MtkCameraFace));
}


/*******************************************************************************
*
********************************************************************************/
StereoHal::~StereoHal()
{
    pthread_mutex_destroy(&mResultInfoLock);
    pthread_mutex_destroy(&mInitDestroyLock);
    pthread_mutex_destroy(&mRunLock);
}

StereoHal* StereoHal::
createInstance()
{
    StereoHal *pStereoHal = StereoHal::getInstance();
    pStereoHal->init();
    return pStereoHal;
}

/*******************************************************************************
*
********************************************************************************/
StereoHal* StereoHal::
getInstance()
{
    MY_LOGD("StereoHal getInstance.");
    static StereoHal singleton;
    return &singleton;
}


/*******************************************************************************
*
********************************************************************************/
void
StereoHal::
destroyInstance()
{
    uninit();
}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::init()
{
    MY_LOGD("- E. mUsers: %d.", mUsers);
    MBOOL Result = MTRUE;   // TRUE: no error. FALSE: error.
    MINT32 err = 0;
    Mutex::Autolock lock(mLock);

    //memset((void*)&gStereoKernelTuningParaInfo,0,sizeof(gStereoKernelTuningParaInfo));
    //memset((void*)&gStereoKernelInitInfo,0,sizeof(gStereoKernelInitInfo));
    //memset((void*)&gStereoKernelWorkBufInfo,0,sizeof(gStereoKernelWorkBufInfo));
    //memset((void*)&gStereoKernelProcInfo,0,sizeof(gStereoKernelProcInfo));
    memset((void*)&gStereoKernelResultInfo,0,sizeof(gStereoKernelResultInfo));

    if (mUsers > 0)
    {
        MY_LOGD("StereoHal has already inited.");
        goto lb_Normal_Exit;
    }

    // Create StereoDrv instance.
    m_pStereoDrv = MTKStereoKernel::createInstance();
    if (!m_pStereoDrv)
    {
        MY_LOGE("MTKStereoKernel::createInstance() fail.");
        Result = MFALSE;
        goto lb_Abnormal_Exit;
    }

    // Load learning data from NVRAM.
#if ENABLE_ALGO
    Result = _loadFromNvram();
    MY_LOGD("_saveToNvram before");
    //for(int i=0;i<MTK_STEREO_KERNEL_NVRAM_LENGTH;i++) {
    //    MY_LOGD("%d. ", m_pVoidGeoData->StereoNvramData.StereoData[i]);
    //}
    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_LOAD_NVRAM,
                                                (void*)&m_pVoidGeoData->StereoNvramData.StereoData, NULL);
    if (err)
    {
        MY_LOGE("StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_LOAD_NVRAM) fail. error code: %d.", err);
        Result = MFALSE;
        goto lb_Abnormal_Exit;
    }

    if (!Result)    // Maybe no data in NVRAM, so read from EEPROM.
    {
        MY_LOGD("Load from NVRAM fail (Maybe 1st time so no data in NVRAM yet).");
    }

    MY_LOGD("StereoHal init NVRAM %d. %d. %d. %d. %d."
            , m_pVoidGeoData->StereoNvramData.StereoData[0]
            , m_pVoidGeoData->StereoNvramData.StereoData[1]
            , m_pVoidGeoData->StereoNvramData.StereoData[2]
            , m_pVoidGeoData->StereoNvramData.StereoData[3]
            , m_pVoidGeoData->StereoNvramData.StereoData[4]
            );
#endif
    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_GET_DEFAULT_TUNING, NULL, &gStereoKernelTuningParaInfo);
    if (err)
    {
        MY_LOGE("StereoKernelFeatureCtrl(GET_DEFAULT_TUNING) fail. error code: %d.", err);
        Result = MFALSE;
        goto lb_Abnormal_Exit;
    }

lb_Normal_Exit:
    android_atomic_inc(&mUsers);

    MY_LOGD("- X. Result: %d. mUsers: %d.", Result, mUsers);
    return Result;

lb_Abnormal_Exit:
    // StereoDrv Init failed, destroy StereoDrv instance.
    if (m_pStereoDrv)
    {
        m_pStereoDrv->destroyInstance();
        m_pStereoDrv = NULL;
    }

    MY_LOGD("- X. Result: %d. mUsers: %d.", Result, mUsers);
    return Result;

}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::uninit()
{
    MY_LOGD("uninit mUsers: %d.", mUsers);
    MBOOL Result = MTRUE;
    MINT32 err = 0;
    Mutex::Autolock lock(mLock);

    if (mUsers > 1)  // More than one user, so decrease one mUsers.
    {
        android_atomic_dec(&mUsers);
    }
    else if (mUsers == 1)   // Last user, must do some un-init procedure.
    {
        android_atomic_dec(&mUsers);

        // Save learning data to NVRAM.
#if ENABLE_ALGO

        err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SAVE_NVRAM,
                                                    (void*)&m_pVoidGeoData->StereoNvramData.StereoData, NULL);
        if (err)
        {
            MY_LOGE("StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SAVE_NVRAM) fail. error code: %d.", err);
            Result = MFALSE;
        }
        MY_LOGD("_saveToNvram after");
        //for(int i=0;i<MTK_STEREO_KERNEL_NVRAM_LENGTH;i++) {
        //    MY_LOGD("%d. ", m_pVoidGeoData->StereoNvramData.StereoData[i]);
        //}
        Result = _saveToNvram();
#endif
        if (!Result)
        {
            MY_LOGD("Save to NVRAM fail.");
        }

        // Destroy StereoDrv instance.
        if (m_pStereoDrv)
        {
            m_pStereoDrv->destroyInstance();
            m_pStereoDrv = NULL;
        }
    }
    else // mUsers <= 0. No StereoHal user, do nothing.
    {
        // do nothing.
        MY_LOGW("No StereoHal to un-init.");
    }

    MY_LOGD("- X. Result: %d. mUsers: %d.", Result, mUsers);
    return Result;

}

/*******************************************************************************
*
********************************************************************************/
bool
StereoHalBase::getStereoParams(MINT32 const main_idx, MINT32 const main2_idx, STEREO_PARAMS_T &OutData)
{
    MBOOL Result = MTRUE;
    //MINT32 err = 0; // 0: no error. other value: error.
    //SensorCropWinInfo rSensorCropInfo;
    //MUINT32 sensor_scenario = 2;
    //MUINT32 sensorwidth = 0;
    //MUINT32 sensorheight = 0;
    //int sensorDevIdx = 0;
    //IHalSensorList* sensorlsit;
    //SensorStaticInfo pSensorStaticInfo;
    //sensorlsit = IHalSensorList::get();
    //IHalSensor* pIHalSensor = sensorlsit->createSensor("Stereo hal", main_idx);
    //sensorDevIdx=sensorlsit->querySensorDevIdx(main_idx);
    //err = pIHalSensor->sendCommand(sensorDevIdx,SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,(MINT32)&sensor_scenario,(MINT32)&rSensorCropInfo,0);
    //if(err)
    //{
    //    MY_LOGE("SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO() fail. error code: %d.", err);
    //    Result = MFALSE;
    //    goto lb_Abnormal_Exit;
    //}
    //sensorwidth  = rSensorCropInfo.full_w;
    //sensorheight = rSensorCropInfo.full_h ; // 4:3=>3072, 16:9=>2304
    //MY_LOGD("main1 sensorDevIdx %d. sensorIdx %d sensorwidth %d sensorheight %d",sensorDevIdx,main_idx,sensorwidth,sensorheight);
    //if(sensorwidth>SensorIs5M) //check sensor is below 5M
    //{
    //   OutData.refocusSize     = (char*)"3072x1728";
    //   OutData.refocusSizesStr = (char*)"3072x1728,2560x1440,1920x1080,1280x720";
    //}
    //else
    //{
    //   OutData.refocusSize     = (char*)"2560x1440";
    //   OutData.refocusSizesStr = (char*)"2560x1440,1920x1080,1280x720";
    //}
    //
    //sensorDevIdx=sensorlsit->querySensorDevIdx(main2_idx);
    //err = pIHalSensor->sendCommand(sensorDevIdx,SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,(MINT32)&sensor_scenario,(MINT32)&rSensorCropInfo,0);
    //if(err)
    //{
    //    MY_LOGE("SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO() fail. error code: %d.", err);
    //    Result = MFALSE;
    //    goto lb_Abnormal_Exit;
    //}
    //sensorwidth  = rSensorCropInfo.full_w;
    //sensorheight = rSensorCropInfo.full_h ; // 4:3=>3072, 16:9=>2304
    //MY_LOGD("main2 sensorDevIdx %d. sensorIdx %d sensorwidth %d sensorheight %d",sensorDevIdx,main_idx,sensorwidth,sensorheight);
    //if(sensorwidth>SensorIs5M) //check sensor is 5M
    //{
    //   OutData.stereoSize     = (char*)"4352x1112";
    //   OutData.stereoSizesStr = (char*)"4352x1112,3072x752";
    //}
    //else
    //{
    //   OutData.stereoSize     = (char*)"3072x752";
    //   OutData.stereoSizesStr = (char*)"3072x752";
    //}
    //MY_LOGD("getStereoParams pass out OutData.stereoSize %s OutData.stereoSizesStr %s OutData.refocusSize %s OutData.refocusSizesStr %s",OutData.stereoSize,OutData.stereoSizesStr,OutData.refocusSize,OutData.refocusSizesStr);
    //return Result;
    //lb_Abnormal_Exit:

    // hw limiation
    MUINT32 capHwFps_Main = 0, capHwFps_Main2 = 0;
    MUINT32 prvHwFps_Main = 0, prvHwFps_Main2 = 0;
    // sensor ability
    MUINT32 capFps_Main = 0, capFps_Main2 = 0;
    MUINT32 prvFps_Main = 0, prvFps_Main2 = 0;
    PipHal* pPipHal = PipHal::createInstance( main_idx );

//    pPipHal->GetHwMaxFrameRate(main_idx, capHwFps_Main, prvHwFps_Main);
//    pPipHal->GetHwMaxFrameRate(main2_idx, capHwFps_Main2, prvHwFps_Main2);
//    pPipHal->GetSensorMaxFrameRate(main_idx, capFps_Main, prvFps_Main);
//    pPipHal->GetSensorMaxFrameRate(main2_idx, capFps_Main2, prvFps_Main2);  // main2 always full size(capFps)
    pPipHal->GetHwMaxFrameRate(NSCam::SENSOR_DEV_MAIN, capHwFps_Main, prvHwFps_Main);
    pPipHal->GetHwMaxFrameRate(NSCam::SENSOR_DEV_MAIN_2, capHwFps_Main2, prvHwFps_Main2);
    pPipHal->GetSensorMaxFrameRate(NSCam::SENSOR_DEV_MAIN, capFps_Main, prvFps_Main);
    pPipHal->GetSensorMaxFrameRate(NSCam::SENSOR_DEV_MAIN_2, capFps_Main2, prvFps_Main2);  // main2 always full size(capFps)

    pPipHal->destroyInstance(LOG_TAG);
    MUINT32 const capHwFps  = (capHwFps_Main < capHwFps_Main2) ? capHwFps_Main : capHwFps_Main2;
    MUINT32 const prvHwFps  = (prvHwFps_Main < prvHwFps_Main2) ? prvHwFps_Main : prvHwFps_Main2;
    MUINT32 const capFps    = (capFps_Main < capFps_Main2) ? capFps_Main : capFps_Main2;
    MUINT32 const prvFps    = (prvFps_Main < capFps_Main2) ? prvFps_Main : capFps_Main2;    // NOTE: main2 always full size
    OutData.captureFps      = (capHwFps < capFps) ? capHwFps : capFps;
    OutData.previewFps      = (prvHwFps < prvFps) ? prvHwFps : prvFps;

    switch(g_seImageRatio)
    {
        case eRatio_4_3:
            OutData.stereoSize      = (char*)"4352x1504";   //(1920+256)x2, 1440+64(16-align)
            OutData.stereoSizesStr  = (char*)"4352x1504";
            //5M 4:3, for AP output size(gallery image) and PASS2_CAP_DST
            OutData.refocusSize     = (char*)"2592x1944";
            OutData.refocusSizesStr = (char*)"2592x1944,2408x1536,1920x1440,1280x960";
            break;
        case eRatio_16_9:
        default:
            OutData.stereoSize      = (char*)"4352x1152";   //(1920+256)x2, 1080+72(16-align)
            OutData.stereoSizesStr  = (char*)"4352x1152";
            //5M 16:9, for PASS2_CAP_DST and AP output size(gallery image)
            OutData.refocusSize     = (char*)"3072x1728";   //5M 16:9
            OutData.refocusSizesStr = (char*)"3072x1728,2560x1440,1920x1080,1280x720";
            break;
    }

    MY_LOGD("captureFps(%d) previewFps(%d) stereoSize(%s) stereoSizesStr(%s) refocusSize(%s) refocusSizesStr(%s)",
            OutData.captureFps, OutData.previewFps,
            OutData.stereoSize, OutData.stereoSizesStr,
            OutData.refocusSize, OutData.refocusSizesStr);
    return Result;
}

/**
 *
 */
bool
StereoHalBase::setImageRatio(STEREO_RATIO_E eRatio)
{
    bool bSuccess = true;

    g_seImageRatio = eRatio;
    switch(eRatio)
    {
        case eRatio_4_3:
            MY_LOGD("Set to 4:3");
            g_sResolutionInfo = IMAGE_RESOLUTION_INFO_STRUCT(MSize(1920, 1440), MSize(1920, 1440), 4, 3);
            gnRefocusOffsetY = 64; //1440+64 is 16 align
            break;
        case eRatio_16_9:
            MY_LOGD("Set to 16:9");
            g_sResolutionInfo = IMAGE_RESOLUTION_INFO_STRUCT(MSize(1920, 1080), MSize(1920, 1080), 16, 9);
            gnRefocusOffsetY = 72; //For 16-align
            break;
        default:
            MY_LOGD("Did not assign image ratio!");
            bSuccess = false;
    }

    return bSuccess;
}

STEREO_RATIO_E
StereoHalBase::getImageRatio()
{
    return g_seImageRatio;
}

/*******************************************************************************
*
********************************************************************************/
void
StereoHal::setParameters(sp<IParamsManager> spParamsMgr)
{
/*
    // Stereo Feature
    const char MtkCameraParameters::KEY_STEREO_REFOCUS_MODE[] = "stereo-image-refocus";
    const char MtkCameraParameters::KEY_STEREO_DEPTHAF_MODE[] = "stereo-depth-af";
    const char MtkCameraParameters::KEY_STEREO_DISTANCE_MODE[] = "stereo-distance-measurement";
*/
    mRefocusMode = ( ::strcmp(spParamsMgr->getStr(MtkCameraParameters::KEY_STEREO_REFOCUS_MODE), MtkCameraParameters::ON) == 0 ) ? MTRUE : MFALSE;
    mDepthAF = ( ::strcmp(spParamsMgr->getStr(MtkCameraParameters::KEY_STEREO_DEPTHAF_MODE), MtkCameraParameters::ON) == 0 ) ? MTRUE : MFALSE;
    mDistanceMeasure = ( ::strcmp(spParamsMgr->getStr(MtkCameraParameters::KEY_STEREO_DISTANCE_MODE), MtkCameraParameters::ON) == 0 ) ? MTRUE : MFALSE;
}

/*******************************************************************************
*
********************************************************************************/
MUINT32
StereoHal::getSensorPosition() const
{
    // define:
    //    0 is main-main2 (main in L)
    //    1 is main2-main (main in R)
    customSensorPos_STEREO_t SensorPos;
    SensorPos=getSensorPosSTEREO();

    return SensorPos.uSensorPos;
}


/*******************************************************************************
*
********************************************************************************/
MSize
StereoHal::getMainSize(MSize const imgSize) const
{
    return MSize(imgSize.w * STEREO_FACTOR, imgSize.h * STEREO_FACTOR);
}

/*******************************************************************************
*
********************************************************************************/
MSize
StereoHal::getAlgoInputSize(MUINT32 const SENSOR_INDEX) const
{
//#if ENABLE_ALGO
//    return (SENSOR_INDEX == m_nMain1SensorIdx) ?
//        MSize(gStereoKernelInitInfo.remap_main.rrz_out_width,  gStereoKernelInitInfo.remap_main.rrz_out_height) :
//        MSize(gStereoKernelInitInfo.remap_minor.rrz_out_width, gStereoKernelInitInfo.remap_minor.rrz_out_height);
//#else
//    return (SENSOR_INDEX == m_nMain1SensorIdx) ? g_sResolutionInfo.szMainCam : g_sResolutionInfo.szSubCam;
//#endif
    return (SENSOR_INDEX == m_nMain1SensorIdx) ? g_sResolutionInfo.szMainCam : g_sResolutionInfo.szSubCam;
}

/*******************************************************************************
*
********************************************************************************/
MSize
StereoHal::getRrzSize(MUINT32 const SENSOR_INDEX) const
{
#if ENABLE_ALGO
    return (SENSOR_INDEX == m_nMain1SensorIdx) ?
        MSize(gStereoKernelInitInfo.remap_main.rrz_out_width,  gStereoKernelInitInfo.remap_main.rrz_out_height) :
        MSize(gStereoKernelInitInfo.remap_minor.rrz_out_width, gStereoKernelInitInfo.remap_minor.rrz_out_height);
#else
    return (SENSOR_INDEX == m_nMain1SensorIdx) ? g_sResolutionInfo.szMainCam : g_sResolutionInfo.szSubCam;
#endif
}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::STEREOGetRrzInfo(RRZ_DATA_STEREO_T &OutData) const
{
#if ENABLE_ALGO
    OutData.rrz_crop_main1  = MRect(MPoint(gStereoKernelInitInfo.remap_main.rrz_offset_x,
                                           gStereoKernelInitInfo.remap_main.rrz_offset_y),
                                    MSize(gStereoKernelInitInfo.remap_main.rrz_usage_width,
                                          gStereoKernelInitInfo.remap_main.rrz_usage_height));
    OutData.rrz_size_main1  = getRrzSize(m_nMain1SensorIdx); //MSize(gStereoKernelInitInfo.remap_main.rrz_out_width,gStereoKernelInitInfo.remap_main.rrz_out_height);
    OutData.rrz_crop_main2  = MRect(MPoint(gStereoKernelInitInfo.remap_minor.rrz_offset_x,
                                           gStereoKernelInitInfo.remap_minor.rrz_offset_y),
                                    MSize(gStereoKernelInitInfo.remap_minor.rrz_usage_width,
                                          gStereoKernelInitInfo.remap_minor.rrz_usage_height));
    OutData.rrz_size_main2  = getRrzSize(m_nMain2SensorIdx); //MSize(gStereoKernelInitInfo.remap_minor.rrz_out_width,gStereoKernelInitInfo.remap_minor.rrz_out_height);
#else
    OutData.rrz_crop_main1  = MRect(MPoint(0,100), MSize(2096,1178));
    OutData.rrz_size_main1  = getRrzSize(m_nMain1SensorIdx);
    OutData.rrz_crop_main2  = MRect(MPoint(0,240), MSize(2560,1440));
    OutData.rrz_size_main2  = getRrzSize(m_nMain2SensorIdx);
#endif
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::STEREOGetInfo(HW_DATA_STEREO_T &OutData) const
{
    MBOOL ret = MTRUE;
    switch (FE_BLOCK_NUM)
    {
         case 0:
           OutData.hwfe_block_size     = 32;
       break;
       case 1:
           OutData.hwfe_block_size     = 16;
       break;
       case 2:
           OutData.hwfe_block_size     = 8;
       break;
    }
#if ENABLE_ALGO
    OutData.rgba_image_width    = gStereoKernelTuningParaInfo.rgba_in_width;
    OutData.rgba_image_height   = gStereoKernelTuningParaInfo.rgba_in_height;
    OutData.fefm_image_width    = ((gStereoKernelTuningParaInfo.fefm_in_width/OutData.hwfe_block_size)*56);
    OutData.fefm_imgae_height   = (gStereoKernelTuningParaInfo.fefm_in_height/OutData.hwfe_block_size);
#else
    OutData.rgba_image_width    = 160;
    OutData.rgba_image_height   = 90;
    OutData.fefm_image_width    = getFEImgSize().w/16 * 56;
    OutData.fefm_imgae_height   = getFEImgSize().h/16;
#endif
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::STEREOInit(INIT_DATA_STEREO_IN_T InData, INIT_DATA_STEREO_OUT_T &OutData)
{
    pthread_mutex_lock(&mInitDestroyLock);
    MBOOL bResult = MTRUE;
    MINT32 err = 0; // 0: no error. other value: error.
    int nCheckCC = -1;  //must declare before goto
    if (m_pWorkBuf) {
        free(m_pWorkBuf);
    }
    m_pWorkBuf = NULL;

    //
    m_nCapOrientation = InData.orientation;
    STEREO_KERNEL_SCENARIO_ENUM algoScenario = STEREO_KERNEL_SCENARIO_IMAGE_PREVIEW;    // default value
    mScenario = InData.eScenario;
    switch ( mScenario )
    {
        case STEREO_SCENARIO_PREVIEW:
            MY_LOGD("mScenario = STEREO_SCENARIO_PREVIEW %d", STEREO_SCENARIO_PREVIEW);
            algoScenario = STEREO_KERNEL_SCENARIO_IMAGE_PREVIEW;
            break;
        case STEREO_SCENARIO_CAPTURE:
            MY_LOGD("mScenario = STEREO_SCENARIO_CAPTURE %d", STEREO_SCENARIO_CAPTURE);
            algoScenario = STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_RF;
            break;
        case STEREO_SCENARIO_RECORD:
            MY_LOGD("mScenario = STEREO_SCENARIO_RECORD %d", STEREO_SCENARIO_RECORD);
            algoScenario = STEREO_KERNEL_SCENARIO_VIDEO_RECORD;
            break;
        default:
            MY_LOGD("unsupport scenario(%d)", mScenario);
            break;
    }

    setImageRatio(InData.eImageRatio);
    _setAlgoImgSize( InData.algo_image_size );
    _setMainImgSize( InData.main_image_size );

#if ENABLE_ALGO
    m_nMain1SensorIdx = InData.main1_sensor_index;
    IHalSensorList* sensorlsit = IHalSensorList::get();
    IHalSensor* pIHalSensor = sensorlsit->createSensor("Stereo hal", m_nMain1SensorIdx);
    int sensorDevIdx = sensorlsit->querySensorDevIdx(m_nMain1SensorIdx);
    MY_LOGD("main1 sensorDevIdx %d. sensorIdx %d", sensorDevIdx, m_nMain1SensorIdx);
    MY_LOGD("InData.main1_sensor_index = %d. InData.main2_sensor_index = %d", InData.main1_sensor_index, InData.main2_sensor_index);

    //TODO: adjust to configurable
    MFLOAT fSourceImageRatioX = 1.0f;
    MFLOAT fSourceImageRatioY = 1.0f;

    gStereoKernelInitInfo.main_source_image_width  = InData.main_image_size.w * fSourceImageRatioX;
    gStereoKernelInitInfo.main_source_image_height = InData.main_image_size.h * fSourceImageRatioY;
    gStereoKernelInitInfo.main_output_image_width  = InData.main_image_size.w ;
    gStereoKernelInitInfo.main_output_image_height = InData.main_image_size.h;

    gStereoKernelInitInfo.algo_source_image_width  = g_sResolutionInfo.szMainCam.w * fSourceImageRatioX;
    gStereoKernelInitInfo.algo_source_image_height = g_sResolutionInfo.szMainCam.h * fSourceImageRatioY;
    gStereoKernelInitInfo.algo_output_image_width  = g_sResolutionInfo.szMainCam.w + gnRefocusOffsetX;
    gStereoKernelInitInfo.algo_output_image_height = g_sResolutionInfo.szMainCam.h + gnRefocusOffsetY;

    SensorCropWinInfo rSensorCropInfo;
    ::memset(&rSensorCropInfo, 0, sizeof(SensorCropWinInfo));
    SensorStaticInfo pSensorStaticInfo;

    if(sensorlsit) {
        // Get sensor info
        sensorlsit->querySensorStaticInfo(sensorDevIdx, &pSensorStaticInfo);

        // Get FOV
        main1_FOV_horizontal = pSensorStaticInfo.horizontalViewAngle;
        //main1_FOV_vertical = pSensorStaticInfo.verticalViewAngle;
        MY_LOGD("Main sensor FOV = %.1f", main1_FOV_horizontal);
        gStereoKernelInitInfo.hori_fov_main = main1_FOV_horizontal;

        // Get crop info
        err = pIHalSensor->sendCommand(sensorDevIdx, SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,
                                       (MUINTPTR)&InData.main1_sensor_scenario, (MUINTPTR)&rSensorCropInfo, 0);
        if(err)
        {
            MY_LOGE("SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO() fail. error code: %d.", err);
            bResult = MFALSE;
            goto lb_Abnormal_Exit;
        }

        // Set remap info for algo
        gStereoKernelInitInfo.remap_main.pixel_array_width  = rSensorCropInfo.full_w;
        gStereoKernelInitInfo.remap_main.pixel_array_height = rSensorCropInfo.full_h ;
        gStereoKernelInitInfo.remap_main.sensor_offset_x0   = rSensorCropInfo.x0_offset ;
        gStereoKernelInitInfo.remap_main.sensor_offset_y0   = rSensorCropInfo.y0_offset ;
        gStereoKernelInitInfo.remap_main.sensor_size_w0     = rSensorCropInfo.w0_size ;
        gStereoKernelInitInfo.remap_main.sensor_size_h0     = rSensorCropInfo.h0_size ;
        gStereoKernelInitInfo.remap_main.sensor_scale_w     = rSensorCropInfo.scale_w ;
        gStereoKernelInitInfo.remap_main.sensor_scale_h     = rSensorCropInfo.scale_h ;
        gStereoKernelInitInfo.remap_main.sensor_offset_x1   = rSensorCropInfo.x1_offset ;
        gStereoKernelInitInfo.remap_main.sensor_offset_y1   = rSensorCropInfo.y1_offset ;
        gStereoKernelInitInfo.remap_main.tg_offset_x        = rSensorCropInfo.x2_tg_offset ;
        gStereoKernelInitInfo.remap_main.tg_offset_y        = rSensorCropInfo.y2_tg_offset ;

        gStereoKernelInitInfo.remap_main.rrz_usage_width    = rSensorCropInfo.w2_tg_size;      //sensor out width;
        gStereoKernelInitInfo.remap_main.rrz_usage_height   = (((rSensorCropInfo.w2_tg_size*9/16)>>1)<<1);

        // High resolution sensor exceeds RRZ capability
//        gStereoKernelInitInfo.remap_main.rrz_out_width      = gStereoKernelInitInfo.algo_source_image_width;
        MUINT32 uMaxRRZSize = (MUINT32)ceil(gStereoKernelInitInfo.remap_main.rrz_usage_width * RRZ_CAPIBILITY);
        if(uMaxRRZSize & 0x1) { uMaxRRZSize++ ; }   //rrz_out_width must be even number
        if(uMaxRRZSize > gStereoKernelInitInfo.algo_source_image_width) {
            gStereoKernelInitInfo.remap_main.rrz_out_width  = uMaxRRZSize;
        } else {
            gStereoKernelInitInfo.remap_main.rrz_out_width  = gStereoKernelInitInfo.algo_source_image_width;
        }

//        gStereoKernelInitInfo.remap_main.rrz_out_height     = gStereoKernelInitInfo.algo_source_image_height;
        //rrz_out_height must be even number
        uMaxRRZSize = (MUINT32)ceil(gStereoKernelInitInfo.remap_main.rrz_usage_height * RRZ_CAPIBILITY);
        if(uMaxRRZSize & 0x1) { uMaxRRZSize++ ; }   //rrz_out_width must be even number
        if(uMaxRRZSize > gStereoKernelInitInfo.algo_source_image_height) {
            gStereoKernelInitInfo.remap_main.rrz_out_height  = uMaxRRZSize;
        } else {
            gStereoKernelInitInfo.remap_main.rrz_out_height  = gStereoKernelInitInfo.algo_source_image_height;
        }

        gStereoKernelInitInfo.remap_main.rrz_offset_x       = ((rSensorCropInfo.w2_tg_size - gStereoKernelInitInfo.remap_main.rrz_usage_width )>>1 ) ;
        gStereoKernelInitInfo.remap_main.rrz_offset_y       = ((rSensorCropInfo.h2_tg_size - gStereoKernelInitInfo.remap_main.rrz_usage_height)>>1 ) ;

        MY_LOGD("main cam");
        MY_LOGD("main full_w %d. main_full_h %d",rSensorCropInfo.full_w,rSensorCropInfo.full_h);
        MY_LOGD("x0_offset %d. y0_offset %d. w0_size %d. h0_size %d.",rSensorCropInfo.x0_offset,rSensorCropInfo.y0_offset,rSensorCropInfo.w0_size,rSensorCropInfo.h0_size);
        MY_LOGD("scale_w %d. scale_h %d",rSensorCropInfo.scale_w,rSensorCropInfo.scale_h);
        MY_LOGD("x1_offset %d. y1_offset %d. w1_size %d. h1_size %d.",rSensorCropInfo.x1_offset,rSensorCropInfo.y1_offset,rSensorCropInfo.w1_size,rSensorCropInfo.h1_size);
        MY_LOGD("x2_tg_offset %d. y2_tg_offset %d. w2_tg_size %d. h2_tg_size %d.",rSensorCropInfo.x2_tg_offset,rSensorCropInfo.y2_tg_offset,rSensorCropInfo.w2_tg_size,rSensorCropInfo.h2_tg_size);
    }

    m_nMain2SensorIdx = InData.main2_sensor_index;
    sensorDevIdx = sensorlsit->querySensorDevIdx(m_nMain2SensorIdx);
    MY_LOGD("main2 sensorDevIdx %d. sensorIdx %d", sensorDevIdx, m_nMain2SensorIdx);
    if(sensorlsit) {
        // Get sensor info
        sensorlsit->querySensorStaticInfo(sensorDevIdx, &pSensorStaticInfo);

        // Get FOV
        main2_FOV_horizontal = pSensorStaticInfo.horizontalViewAngle;
        MY_LOGD("Sub sensor FOV = %.1f", main2_FOV_horizontal);
        gStereoKernelInitInfo.hori_fov_minor = main2_FOV_horizontal;
        //main2_FOV_vertical=pSensorStaticInfo.verticalViewAngle;

        // Get crop info
        err = pIHalSensor->sendCommand(sensorDevIdx, SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO,
                                       (MUINTPTR)&InData.main2_sensor_scenario, (MUINTPTR)&rSensorCropInfo, 0);
        if(err)
        {
            MY_LOGE("SENSOR_CMD_GET_SENSOR_CROP_WIN_INFO() fail. error code: %d.", err);
            bResult = MFALSE;
            goto lb_Abnormal_Exit;
        }

        // Set remap info for algo
        gStereoKernelInitInfo.remap_minor.pixel_array_width     = rSensorCropInfo.full_w;
        gStereoKernelInitInfo.remap_minor.pixel_array_height    = rSensorCropInfo.full_h ;
        gStereoKernelInitInfo.remap_minor.sensor_offset_x0      = rSensorCropInfo.x0_offset ;
        gStereoKernelInitInfo.remap_minor.sensor_offset_y0      = rSensorCropInfo.y0_offset ;
        gStereoKernelInitInfo.remap_minor.sensor_size_w0        = rSensorCropInfo.w0_size ;
        gStereoKernelInitInfo.remap_minor.sensor_size_h0        = rSensorCropInfo.h0_size ;
        gStereoKernelInitInfo.remap_minor.sensor_scale_w        = rSensorCropInfo.scale_w ;
        gStereoKernelInitInfo.remap_minor.sensor_scale_h        = rSensorCropInfo.scale_h ;
        gStereoKernelInitInfo.remap_minor.sensor_offset_x1      = rSensorCropInfo.x1_offset ;
        gStereoKernelInitInfo.remap_minor.sensor_offset_y1      = rSensorCropInfo.y1_offset ;
        gStereoKernelInitInfo.remap_minor.tg_offset_x           = rSensorCropInfo.x2_tg_offset ;
        gStereoKernelInitInfo.remap_minor.tg_offset_y           = rSensorCropInfo.y2_tg_offset ;

        //if(gStereoKernelInitInfo.remap_minor.pixel_array_width>2000) //check sensor size is 5M or 2M
        //{
        //  gStereoKernelInitInfo.remap_minor.rrz_out_width = 1920 ;
        //  gStereoKernelInitInfo.remap_minor.rrz_out_height = 1080 ;
        //}
        //else
        {
            gStereoKernelInitInfo.remap_minor.rrz_out_width = 1280 ;
            gStereoKernelInitInfo.remap_minor.rrz_out_height = 720 ;
        }

        //Pre-crop if FOV diff > 5
        MY_LOGD("[Main2] Before precrop rrz_usage_width  %d", gStereoKernelInitInfo.remap_minor.rrz_usage_width);
        MY_LOGD("[Main2] Before precrop rrz_usage_height %d", gStereoKernelInitInfo.remap_minor.rrz_usage_height);
        if(main2_FOV_horizontal > (main1_FOV_horizontal + 5))//FOV diff bigger than 5 will do precrop
        {
            //Linear pre-crop
            gStereoKernelInitInfo.remap_minor.rrz_usage_width = (MINT32)( tan(main1_FOV_horizontal/180.0f*acos(0)) / tan(main2_FOV_horizontal/180.0f*acos(0)) * rSensorCropInfo.w2_tg_size/32 )*32 ;
            gStereoKernelInitInfo.hori_fov_minor = main1_FOV_horizontal;

            MY_LOGD("[Main2] Precrop rrz_usage_width to %d", gStereoKernelInitInfo.remap_minor.rrz_usage_width);
            MY_LOGD("[Main2] Pre-crop FOV to %.1f", gStereoKernelInitInfo.hori_fov_minor);
        }
        else
        {
             gStereoKernelInitInfo.remap_minor.rrz_usage_width  = rSensorCropInfo.w2_tg_size;     //sensor out width ;
             MY_LOGD("[Main2] Set rrz_usage_width to %d", gStereoKernelInitInfo.remap_minor.rrz_usage_width);
        }

        gStereoKernelInitInfo.remap_minor.rrz_usage_height = (((gStereoKernelInitInfo.remap_minor.rrz_usage_width*g_sResolutionInfo.uRatioNumerator/g_sResolutionInfo.uRatioDenomerator)>>1)<<1);
        if(0 == m_uSensorRelativePosition) {
            gStereoKernelInitInfo.remap_minor.rrz_offset_x = ((rSensorCropInfo.w2_tg_size - gStereoKernelInitInfo.remap_minor.rrz_usage_width ) * m_fMinorCamOffsetXRatio ) ;
        } else {
            gStereoKernelInitInfo.remap_minor.rrz_offset_x = ((rSensorCropInfo.w2_tg_size - gStereoKernelInitInfo.remap_minor.rrz_usage_width ) * (1.0f-m_fMinorCamOffsetXRatio) ) ;
        }

        gStereoKernelInitInfo.remap_minor.rrz_offset_y = ((rSensorCropInfo.h2_tg_size - gStereoKernelInitInfo.remap_minor.rrz_usage_height)>>1 ) ;

        MY_LOGD("minor cam");
        MY_LOGD("main full_w %d. main_full_h %d",rSensorCropInfo.full_w,rSensorCropInfo.full_h);
        MY_LOGD("x0_offset %d. y0_offset %d. w0_size %d. h0_size %d.",rSensorCropInfo.x0_offset,rSensorCropInfo.y0_offset,rSensorCropInfo.w0_size,rSensorCropInfo.h0_size);
        MY_LOGD("scale_w %d. scale_h %d",rSensorCropInfo.scale_w,rSensorCropInfo.scale_h);
        MY_LOGD("x1_offset %d. y1_offset %d. w1_size %d. h1_size %d.",rSensorCropInfo.x1_offset,rSensorCropInfo.y1_offset,rSensorCropInfo.w1_size,rSensorCropInfo.h1_size);
        MY_LOGD("x2_tg_offset %d. y2_tg_offset %d. w2_tg_size %d. h2_tg_size %d.",rSensorCropInfo.x2_tg_offset,rSensorCropInfo.y2_tg_offset,rSensorCropInfo.w2_tg_size,rSensorCropInfo.h2_tg_size);
    }
    pIHalSensor->destroyInstance("Stereo hal");

    //Give init tuning params to algo
    gStereoKernelInitInfo.ptuning_para  = &gStereoKernelTuningParaInfo; //got in init()
    gStereoKernelInitInfo.scenario      = (STEREO_KERNEL_SCENARIO_ENUM)algoScenario;

    //Set HWFE block number
    switch (FE_BLOCK_NUM)
    {
    case 0:
        gStereoKernelInitInfo.hwfe_block_size   = 32;
        break;
    case 1:
        gStereoKernelInitInfo.hwfe_block_size   = 16;
        break;
    case 2:
        gStereoKernelInitInfo.hwfe_block_size   = 8;
        break;
    }

    mAlgoSize.w         = gStereoKernelInitInfo.algo_source_image_width;
    mAlgoSize.h         = gStereoKernelInitInfo.algo_source_image_height;
    mAlgoAppendSize.w   = gStereoKernelInitInfo.algo_source_image_width  + gnRefocusOffsetX;
    mAlgoAppendSize.h   = gStereoKernelInitInfo.algo_source_image_height + gnRefocusOffsetY;
    mMainSize.w         = gStereoKernelInitInfo.main_source_image_width;
    mMainSize.h         = gStereoKernelInitInfo.main_source_image_height;
    mFEImgSize.w        = gStereoKernelTuningParaInfo.fefm_in_width;
    mFEImgSize.h        = gStereoKernelTuningParaInfo.fefm_in_height;

    //Set image size to algo
    gStereoKernelInitInfo.fefm_image_width  = gStereoKernelTuningParaInfo.fefm_in_width;
    gStereoKernelInitInfo.fefm_image_height = gStereoKernelTuningParaInfo.fefm_in_height;
    gStereoKernelInitInfo.rgba_image_width  = gStereoKernelTuningParaInfo.rgba_in_width;
    gStereoKernelInitInfo.rgba_image_height = gStereoKernelTuningParaInfo.rgba_in_height;
    gStereoKernelInitInfo.rgba_image_stride = gStereoKernelTuningParaInfo.rgba_in_width;   //it should 16 align
    gStereoKernelInitInfo.rgba_image_depth  = 4;    //4 bytes

    //Get sensor relative position
    customSensorPos_STEREO_t SensorPos;
    SensorPos = getSensorPosSTEREO();
    m_uSensorRelativePosition = SensorPos.uSensorPos;
    gStereoKernelInitInfo.sensor_config         = SensorPos.uSensorPos ; // L-R 0: main-minor , 1: minor-main
    gStereoKernelInitInfo.enable_cc             = 0;   //enable color correction: 1
    gStereoKernelInitInfo.enable_gpu            = 0;

    gStereoKernelInitInfo.stereo_baseline       = STEREO_BASELINE;
    gStereoKernelInitInfo.support_diff_fov_fm   = 1;    // 0: same FOVs,  1: diff FOVs

    //Get min/max dac
    mpHal3A = IHal3A::createInstance(IHal3A::E_Camera_1, m_nMain1SensorIdx, "MTKStereoCamera");
    mpHal3A->send3ACtrl(E3ACtrl_GetDAFTBL, (MUINTPTR)&g_prDafTbl, 0);
    MY_LOGD("g_rDafTbl %p", &g_prDafTbl);

    //Since af_mgr::init may run later, we have to wait for it
    for(int nTimes = 10; nTimes > 0; nTimes--) {
        gStereoKernelInitInfo.af_dac_start = g_prDafTbl->af_dac_min;
        if (0 == gStereoKernelInitInfo.af_dac_start) {
            MY_LOGD("Waiting for af_dac_min...");
            usleep(20 * 1000);
        } else {
            break;
        }
    }

    if (0 == gStereoKernelInitInfo.af_dac_start) {
        MY_LOGE("Cannot get af_dac_min");
    }

    if (STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_RF == algoScenario)
    {
        //Set algo image size
//        gStereoKernelInitInfo.algo_source_image_width   = InData.algo_image_size.w * fSourceImageRatioX - gnRefocusOffsetX;
//        gStereoKernelInitInfo.algo_source_image_height  = InData.algo_image_size.h * fSourceImageRatioY - gnRefocusOffsetY;
//        gStereoKernelInitInfo.algo_output_image_width   = InData.algo_image_size.w;
//        gStereoKernelInitInfo.algo_output_image_height  = InData.algo_image_size.h;

        gStereoKernelInitInfo.remap_minor.rrz_out_width  = g_sResolutionInfo.szSubCam.w ;
        gStereoKernelInitInfo.remap_minor.rrz_out_height = g_sResolutionInfo.szSubCam.h ;
//        if(gStereoKernelInitInfo.remap_minor.pixel_array_width > 2000) //check sensor size is 5M or 2M
//        {
//            gStereoKernelInitInfo.remap_minor.rrz_out_width  = 1920 ;
//            gStereoKernelInitInfo.remap_minor.rrz_out_height = 1080 ;
//        }
//        else
//        {
//            gStereoKernelInitInfo.remap_minor.rrz_out_width  = 1280 ;
//            gStereoKernelInitInfo.remap_minor.rrz_out_height = 720 ;
//        }

        //Pre-crop main2
        MY_LOGD("[Capture-RF Main2] Before precrop rrz_usage_width  %d", gStereoKernelInitInfo.remap_minor.rrz_usage_width);
        MY_LOGD("[Capture-RF Main2] Before precrop rrz_usage_height %d", gStereoKernelInitInfo.remap_minor.rrz_usage_height);
        if(main2_FOV_horizontal > (main1_FOV_horizontal + m_fPrecropAngleForCapture))//FOV diff bigger than 10 will do precrop
        {
            gStereoKernelInitInfo.remap_minor.rrz_usage_width = (MINT32)( tan((main1_FOV_horizontal+m_fPrecropAngleForCapture)/180.0f*acos(0)) / tan(main2_FOV_horizontal/180.0f*acos(0)) * rSensorCropInfo.w2_tg_size/32 )*32 ;
            gStereoKernelInitInfo.hori_fov_minor = main1_FOV_horizontal + m_fPrecropAngleForCapture ;

            MY_LOGD("[Capture-RF Main2] Precrop rrz_usage_width to %d", gStereoKernelInitInfo.remap_minor.rrz_usage_width);
            MY_LOGD("[Capture-RF Main2] Pre-crop FOV to %.1f", gStereoKernelInitInfo.hori_fov_minor);
        }
        else
        {
             gStereoKernelInitInfo.remap_minor.rrz_usage_width = rSensorCropInfo.w2_tg_size;     //sensor out width ;
             MY_LOGD("[Capture-RF Main2] Set rrz_usage_width to %d", gStereoKernelInitInfo.remap_minor.rrz_usage_width);
        }

        gStereoKernelInitInfo.remap_minor.rrz_usage_height = (((gStereoKernelInitInfo.remap_minor.rrz_usage_width*9/16)>>1)<<1);
        if(0 == m_uSensorRelativePosition) {
            gStereoKernelInitInfo.remap_minor.rrz_offset_x = ((rSensorCropInfo.w2_tg_size - gStereoKernelInitInfo.remap_minor.rrz_usage_width ) * m_fMinorCamOffsetXRatio ) ;
        } else {
            gStereoKernelInitInfo.remap_minor.rrz_offset_x = ((rSensorCropInfo.w2_tg_size - gStereoKernelInitInfo.remap_minor.rrz_usage_width ) * (1.0f-m_fMinorCamOffsetXRatio) ) ;
        }
        gStereoKernelInitInfo.remap_minor.rrz_offset_y = ((rSensorCropInfo.h2_tg_size - gStereoKernelInitInfo.remap_minor.rrz_usage_height)>>1 ) ;

        gStereoKernelInitInfo.enable_cc     = 1; //enable color correction: 1
        gStereoKernelInitInfo.enable_gpu    = ENABLE_GPU;
    }

    //rrz_usage -> RRZ -> rrz_out, RRZ cannot scale up
    //Adjust main cam
    if(gStereoKernelInitInfo.remap_main.rrz_usage_width < gStereoKernelInitInfo.remap_main.rrz_out_width) {
        MY_LOGD("[Main]Set rrz_out_width(%d) as rrz_usage_width(%d)",
                gStereoKernelInitInfo.remap_main.rrz_out_width,
                gStereoKernelInitInfo.remap_main.rrz_usage_width);
        gStereoKernelInitInfo.remap_main.rrz_out_width  = gStereoKernelInitInfo.remap_main.rrz_usage_width;
    }

    if(gStereoKernelInitInfo.remap_main.rrz_usage_height < gStereoKernelInitInfo.remap_main.rrz_out_height) {
        MY_LOGD("[Main]Set rrz_out_height(%d) as rrz_usage_height(%d)",
                gStereoKernelInitInfo.remap_main.rrz_out_height,
                gStereoKernelInitInfo.remap_main.rrz_usage_height);
        gStereoKernelInitInfo.remap_main.rrz_out_height = gStereoKernelInitInfo.remap_main.rrz_usage_height;
    }

    //Adjust minor cam
    if(gStereoKernelInitInfo.remap_minor.rrz_usage_width < gStereoKernelInitInfo.remap_minor.rrz_out_width) {
        MY_LOGD("[Minor]Set rrz_out_width(%d) as rrz_usage_width(%d)",
                gStereoKernelInitInfo.remap_minor.rrz_out_width,
                gStereoKernelInitInfo.remap_minor.rrz_usage_width);
        gStereoKernelInitInfo.remap_minor.rrz_out_width  = gStereoKernelInitInfo.remap_minor.rrz_usage_width;
    }

    if(gStereoKernelInitInfo.remap_minor.rrz_usage_height < gStereoKernelInitInfo.remap_minor.rrz_out_height) {
        MY_LOGD("[Minor]Set rrz_out_height(%d) as rrz_usage_height(%d)",
                gStereoKernelInitInfo.remap_minor.rrz_out_height,
                gStereoKernelInitInfo.remap_minor.rrz_usage_height);
        gStereoKernelInitInfo.remap_minor.rrz_out_height = gStereoKernelInitInfo.remap_minor.rrz_usage_height;
    }

    //Check if user want to do verification
    gStereoKernelInitInfo.enable_verify = (checkStereoProperty(PROPERTY_ENABLE_VERIFY) > 0) ? 1 : 0;

    // Only change if the property is set
    nCheckCC = checkStereoProperty(PERPERTY_ENABLE_CC, false);
    if(nCheckCC >= 0) {
        MY_LOGD("Override CC setting: %d", nCheckCC);
        gStereoKernelInitInfo.enable_cc = nCheckCC;
    }

//debug ----------------------------------------------------------------------------------------------------------
    MY_LOGD(" ===== Dump init info for algorithm ===== ");
    MY_LOGD("Sensor index: main(%d) minor(%d)", m_nMain1SensorIdx, m_nMain2SensorIdx);
    MY_LOGD("ptuning_para %d.", gStereoKernelInitInfo.ptuning_para->learn_tolerance);
    MY_LOGD("scenario %d.",     gStereoKernelInitInfo.scenario);

    MY_LOGD("main_source_image_width %d.",  gStereoKernelInitInfo.main_source_image_width);
    MY_LOGD("main_source_image_height %d.", gStereoKernelInitInfo.main_source_image_height);
    MY_LOGD("main_output_image_width %d.",  gStereoKernelInitInfo.main_output_image_width);
    MY_LOGD("main_output_image_height %d.", gStereoKernelInitInfo.main_output_image_height);

    MY_LOGD("algo_source_image_width %d.",  gStereoKernelInitInfo.algo_source_image_width);
    MY_LOGD("algo_source_image_height %d.", gStereoKernelInitInfo.algo_source_image_height);
    MY_LOGD("algo_crop_image_width %d.",    gStereoKernelInitInfo.algo_output_image_width);
    MY_LOGD("algo_crop_image_height %d.",   gStereoKernelInitInfo.algo_output_image_height);

    MY_LOGD("hwfe_block_size %d.",      gStereoKernelInitInfo.hwfe_block_size);
    MY_LOGD("fefm_image_width %d.",     gStereoKernelInitInfo.fefm_image_width);
    MY_LOGD("fefm_image_height %d.",    gStereoKernelInitInfo.fefm_image_height);

    MY_LOGD("rgba_image_width %d.",     gStereoKernelInitInfo.rgba_image_width);
    MY_LOGD("rgba_image_height %d.",    gStereoKernelInitInfo.rgba_image_height);
    MY_LOGD("rgba_image_stride %d.",    gStereoKernelInitInfo.rgba_image_stride);
    MY_LOGD("pixel_array_width %d.",    gStereoKernelInitInfo.remap_main.pixel_array_width);
    MY_LOGD("pixel_array_height %d.",   gStereoKernelInitInfo.remap_main.pixel_array_height);

    MY_LOGD("[Main]sensor_offset_x0 %d.",   gStereoKernelInitInfo.remap_main.sensor_offset_x0);
    MY_LOGD("[Main]sensor_offset_y0 %d.",   gStereoKernelInitInfo.remap_main.sensor_offset_y0);
    MY_LOGD("[Main]sensor_size_w0 %d.",     gStereoKernelInitInfo.remap_main.sensor_size_w0);
    MY_LOGD("[Main]sensor_size_h0 %d.",     gStereoKernelInitInfo.remap_main.sensor_size_h0);
    MY_LOGD("[Main]sensor_scale_w %d.",     gStereoKernelInitInfo.remap_main.sensor_scale_w);
    MY_LOGD("[Main]sensor_scale_h %d.",     gStereoKernelInitInfo.remap_main.sensor_scale_h);
    MY_LOGD("[Main]sensor_offset_x1 %d.",   gStereoKernelInitInfo.remap_main.sensor_offset_x1);
    MY_LOGD("[Main]sensor_offset_y1 %d.",   gStereoKernelInitInfo.remap_main.sensor_offset_y1);

    MY_LOGD("[Main]tg_offset_x %d.",        gStereoKernelInitInfo.remap_main.tg_offset_x);
    MY_LOGD("[Main]tg_offset_y %d.",        gStereoKernelInitInfo.remap_main.tg_offset_y);
    MY_LOGD("[Main]rrz_offset_x %d.",       gStereoKernelInitInfo.remap_main.rrz_offset_x);
    MY_LOGD("[Main]rrz_offset_y %d.",       gStereoKernelInitInfo.remap_main.rrz_offset_y);
    MY_LOGD("[Main]rrz_out_width %d.",      gStereoKernelInitInfo.remap_main.rrz_out_width);
    MY_LOGD("[Main]rrz_out_height %d.",     gStereoKernelInitInfo.remap_main.rrz_out_height);
    MY_LOGD("[Main]rrz_usage_width %d.",    gStereoKernelInitInfo.remap_main.rrz_usage_width);
    MY_LOGD("[Main]rrz_usage_height %d.",   gStereoKernelInitInfo.remap_main.rrz_usage_height);

    MY_LOGD("[Minor]pixel_array_width %d.",     gStereoKernelInitInfo.remap_minor.pixel_array_width);
    MY_LOGD("[Minor]pixel_array_height %d.",    gStereoKernelInitInfo.remap_minor.pixel_array_height);
    MY_LOGD("[Minor]sensor_offset_x0 %d.",      gStereoKernelInitInfo.remap_minor.sensor_offset_x0);
    MY_LOGD("[Minor]sensor_offset_y0 %d.",      gStereoKernelInitInfo.remap_minor.sensor_offset_y0);
    MY_LOGD("[Minor]sensor_size_w0 %d.",        gStereoKernelInitInfo.remap_minor.sensor_size_w0);
    MY_LOGD("[Minor]sensor_size_h0 %d.",        gStereoKernelInitInfo.remap_minor.sensor_size_h0);
    MY_LOGD("[Minor]sensor_scale_w %d.",        gStereoKernelInitInfo.remap_minor.sensor_scale_w);
    MY_LOGD("[Minor]sensor_scale_h %d.",        gStereoKernelInitInfo.remap_minor.sensor_scale_h);
    MY_LOGD("[Minor]sensor_offset_x1 %d.",      gStereoKernelInitInfo.remap_minor.sensor_offset_x1);
    MY_LOGD("[Minor]sensor_offset_y1 %d.",      gStereoKernelInitInfo.remap_minor.sensor_offset_y1);

    MY_LOGD("[Minor]tg_offset_x %d.",       gStereoKernelInitInfo.remap_minor.tg_offset_x);
    MY_LOGD("[Minor]tg_offset_y %d.",       gStereoKernelInitInfo.remap_minor.tg_offset_y);
    MY_LOGD("[Minor]rrz_offset_x %d.",      gStereoKernelInitInfo.remap_minor.rrz_offset_x);
    MY_LOGD("[Minor]rrz_offset_y %d.",      gStereoKernelInitInfo.remap_minor.rrz_offset_y);
    MY_LOGD("[Minor]rrz_out_width %d.",     gStereoKernelInitInfo.remap_minor.rrz_out_width);
    MY_LOGD("[Minor]rrz_out_height %d.",    gStereoKernelInitInfo.remap_minor.rrz_out_height);
    MY_LOGD("[Minor]rrz_usage_width %d.",   gStereoKernelInitInfo.remap_minor.rrz_usage_width);
    MY_LOGD("[Minor]rrz_usage_height %d.",  gStereoKernelInitInfo.remap_minor.rrz_usage_height);

    MY_LOGD("sensor_config %d.",    gStereoKernelInitInfo.sensor_config);
    MY_LOGD("enable_cc %d.",        gStereoKernelInitInfo.enable_cc);
    MY_LOGD("enable_gpu %d.",       gStereoKernelInitInfo.enable_gpu);
    MY_LOGD("hori_fov_main %f.",    gStereoKernelInitInfo.hori_fov_main);
    MY_LOGD("hori_fov_minor %f.",   gStereoKernelInitInfo.hori_fov_minor);
    MY_LOGD("stereo_baseline %f.",  gStereoKernelInitInfo.stereo_baseline);
    MY_LOGD("af_dac_start = %d",    gStereoKernelInitInfo.af_dac_start);
    MY_LOGD("enable_verify %d.",    gStereoKernelInitInfo.enable_verify);
//debug ----------------------------------------------------------------------------------------------------------

    //After a lot of work, we can init algo now
    err = m_pStereoDrv->StereoKernelInit(&gStereoKernelInitInfo);
    if (err)
    {
        MY_LOGE("StereoKernelInit() fail. error code: %d.", err);
        bResult = MFALSE;
        goto lb_Abnormal_Exit;
    }
#endif

    if (   STEREO_SCENARIO_PREVIEW == mScenario
        || STEREO_SCENARIO_RECORD  == mScenario
       )
    {
        OutData.algoin_size = InData.main_image_size;
    }
    else
    {
        OutData.algoin_size = InData.main_image_size * STEREO_FACTOR;
    }

#if ENABLE_ALGO
    //Get algo working buffer size and allocate one
    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_GET_WORK_BUF_INFO, NULL,
                                                &gStereoKernelInitInfo.working_buffer_size);
    if (err)
    {
        MY_LOGE("StereoKernelFeatureCtrl(Get_WORK_BUF_INFO) fail. error code: %d.", err);
        bResult = MFALSE;
        goto lb_Abnormal_Exit;
    }
    _initWorkBuf(gStereoKernelInitInfo.working_buffer_size);
#endif

    //
    #ifdef MTK_CAM_DEPTH_AF_SUPPORT
    mpStereoDepthHal = StereoDepthHalBase::createInstance();    // pStereoDepthHal->init() has already run inside createInstance().

    STEREODEPTH_HAL_INIT_PARAM_STRUCT stStereodepthHalInitParam;
    stStereodepthHalInitParam.stereo_fov_main       = gStereoKernelInitInfo.hori_fov_main;
    stStereodepthHalInitParam.stereo_fov_main2      = gStereoKernelInitInfo.hori_fov_minor;
    stStereodepthHalInitParam.stereo_baseline       = gStereoKernelInitInfo.stereo_baseline;
    stStereodepthHalInitParam.stereo_pxlarr_width   = gStereoKernelInitInfo.remap_main.pixel_array_width;
    stStereodepthHalInitParam.stereo_pxlarr_height  = gStereoKernelInitInfo.remap_main.pixel_array_height;
    stStereodepthHalInitParam.stereo_main12_pos     = m_uSensorRelativePosition;
    stStereodepthHalInitParam.pNvRamDataArray       = m_pVoidGeoData->StereoNvramData.DepthAfData;
    mpStereoDepthHal->StereoDepthInit(&stStereodepthHalInitParam);
    #endif  // MTK_CAM_DEPTH_AF_SUPPORT

lb_Abnormal_Exit:

    //MY_LOGD("- X. Result: %d. work_buf_size: %d. l_crop_offset W/H: (%d, %d). r_crop_offset W/H: (%d, %d).", Result, gStereoKernelInitInfo.working_buffer_size, OutData.left_crop_offset.width, OutData.left_crop_offset.height, OutData.right_crop_offset.width, OutData.right_crop_offset.height);

    return bResult;

}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::_initWorkBuf(const MUINT32 BUFFER_SIZE)
{
    if (m_pWorkBuf) {
        free(m_pWorkBuf);
    }
    m_pWorkBuf = NULL;

    const MUINT32 BUFFER_LEN = sizeof(MUINT8) * BUFFER_SIZE;
    m_pWorkBuf = (MUINT8*)malloc(BUFFER_LEN) ;
    ::memset(m_pWorkBuf, 0, BUFFER_LEN);
    MY_LOGD("Working buffer size: %d addr %p. ", BUFFER_SIZE, m_pWorkBuf);
    MBOOL bResult = MTRUE;
    MINT32 err = 0; // 0: no error. other value: error.
    // Allocate working buffer.
    //     Allocate memory
    //     Set WorkBufInfo
    gStereoKernelWorkBufInfo.ext_mem_size       = gStereoKernelInitInfo.working_buffer_size;
    gStereoKernelWorkBufInfo.ext_mem_start_addr = m_pWorkBuf;

    err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SET_WORK_BUF_INFO,
                                                &gStereoKernelWorkBufInfo, NULL);
    if (err)
    {
        MY_LOGE("StereoKernelFeatureCtrl(SET_WORK_BUF_INFO) fail. error code: %d.", err);
        bResult = MFALSE;
        goto lb_Abnormal_Exit;
    }

lb_Abnormal_Exit:

    MY_LOGD("- X. Result: %d.", bResult);
    return bResult;

}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::STEREOSetParams(SET_DATA_STEREO_T RunData)
{
    pthread_mutex_lock(&mRunLock);
    MBOOL bResult = MTRUE;
    MY_LOGD("SetParams in");
#if ENABLE_ALGO
    mpHal3A->send3ACtrl(E3ACtrl_GetDAFTBL, (MUINTPTR)&g_prDafTbl, 0);
    g_prDafTbl->curr_p2_frm_num = RunData.mMagicNum;    // Update P2 frame number in DAF table.

    MINT32 err = 0; // 0: no error. other value: error.
    //Set graphic buffer to algo
    gStereoKernelProcInfo.src_gb.mGraphicBuffer = RunData.mSrcGraphicBuffer;
    gStereoKernelProcInfo.dst_gb.mGraphicBuffer = RunData.mDstGraphicBuffer;
    gStereoKernelProcInfo.warp_image_addr_src   = (MUINT8*)RunData.mSrcGraphicBufferVA;
    gStereoKernelProcInfo.warp_image_addr_dst   = (MUINT8*)RunData.mDstGraphicBufferVA;
    MY_LOGD("SetParams src:%p dst:%p", gStereoKernelProcInfo.warp_image_addr_src, gStereoKernelProcInfo.warp_image_addr_dst);

    //Set Input/Output graphic buffer to algo
    sp<GraphicBuffer>* srcGBArray[1];
    sp<GraphicBuffer>* dstGBArray[1];
    srcGBArray[0] = (sp<GraphicBuffer>*)RunData.mSrcGraphicBuffer;
    dstGBArray[0] = (sp<GraphicBuffer>*)RunData.mDstGraphicBuffer;
    gStereoKernelProcInfo.InputGB   = (void*)&srcGBArray;
    gStereoKernelProcInfo.OutputGB  = (void*)&dstGBArray;

    //Set RGBA image and HWFE data
    if(0 == m_uSensorRelativePosition)
    {
        gStereoKernelProcInfo.rgba_image_left_addr  = (MUINT8*)RunData.u4RgbaAddr_main1;
        gStereoKernelProcInfo.rgba_image_right_addr = (MUINT8*)RunData.u4RgbaAddr_main2;
        // hwfe
        gStereoKernelProcInfo.hwfe_data_left  = (MUINT16*)RunData.u4FEBufAddr_main1 ; // for store results of HWFE, Left  Image
        gStereoKernelProcInfo.hwfe_data_right = (MUINT16*)RunData.u4FEBufAddr_main2 ; // for store results of HWFE, Right Image
    }
    else
    {
        gStereoKernelProcInfo.rgba_image_left_addr  = (MUINT8*)RunData.u4RgbaAddr_main2;
        gStereoKernelProcInfo.rgba_image_right_addr = (MUINT8*)RunData.u4RgbaAddr_main1;
        // hwfe
        gStereoKernelProcInfo.hwfe_data_left  = (MUINT16*)RunData.u4FEBufAddr_main2; // for store results of HWFE, Left  Image
        gStereoKernelProcInfo.hwfe_data_right = (MUINT16*)RunData.u4FEBufAddr_main1; // for store results of HWFE, Right Image
    }

    // Set AF info according to scenario
    if (STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_RF == gStereoKernelInitInfo.scenario)
    {
        MY_LOGD("Capture mode get dac");
        gStereoKernelProcInfo.af_dac_index  = g_prDafTbl->daf_vec[g_prDafTbl->curr_p1_frm_num % DAF_TBL_QLEN].af_dac_index;
        gStereoKernelProcInfo.af_confidence = g_prDafTbl->daf_vec[g_prDafTbl->curr_p1_frm_num % DAF_TBL_QLEN].af_confidence;
        gStereoKernelProcInfo.af_valid      = g_prDafTbl->daf_vec[g_prDafTbl->curr_p1_frm_num % DAF_TBL_QLEN].af_valid;
    }
    else
    {
        gStereoKernelProcInfo.af_dac_index  = g_prDafTbl->daf_vec[g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN].af_dac_index ;
        gStereoKernelProcInfo.af_confidence = g_prDafTbl->daf_vec[g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN].af_confidence ;
        gStereoKernelProcInfo.af_valid      = g_prDafTbl->daf_vec[g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN].af_valid ;
    }

    if (g_prDafTbl->is_query_happen != INVALID_QUERY) // If is_query_happen is valid
    {
        gStereoKernelProcInfo.af_win_start_x_remap = g_prDafTbl->daf_vec[g_prDafTbl->is_query_happen % DAF_TBL_QLEN].af_win_start_x ; // 1/8 windows of 4096 = 512
        gStereoKernelProcInfo.af_win_end_x_remap   = g_prDafTbl->daf_vec[g_prDafTbl->is_query_happen % DAF_TBL_QLEN].af_win_end_x ; // 2304-1793+1 = 512
        gStereoKernelProcInfo.af_win_start_y_remap = g_prDafTbl->daf_vec[g_prDafTbl->is_query_happen % DAF_TBL_QLEN].af_win_start_y ; // 1/8 windows of 3072 = 384
        gStereoKernelProcInfo.af_win_end_y_remap   = g_prDafTbl->daf_vec[g_prDafTbl->is_query_happen % DAF_TBL_QLEN].af_win_end_y ; // 1728-1345+1 = 384
    }

    MY_LOGD("curr_p2_frm_num = %d (idx: %d)",g_prDafTbl->curr_p2_frm_num, g_prDafTbl->curr_p2_frm_num% DAF_TBL_QLEN);
    MY_LOGD("is_query_happen = %d (idx: %d)",g_prDafTbl->is_query_happen, g_prDafTbl->is_query_happen % DAF_TBL_QLEN);
    MY_LOGD("AF_DAC_Index = %d ",gStereoKernelProcInfo.af_dac_index);
    MY_LOGD("af_confidence = %d",gStereoKernelProcInfo.af_confidence);
    MY_LOGD("af_valid = %d",gStereoKernelProcInfo.af_valid);
    MY_LOGD("af_win_start_x_remap = %d",gStereoKernelProcInfo.af_win_start_x_remap);
    MY_LOGD("af_win_end_x_remap = %d",gStereoKernelProcInfo.af_win_end_x_remap);
    MY_LOGD("af_win_start_y_remap = %d",gStereoKernelProcInfo.af_win_start_y_remap);
    MY_LOGD("af_win_end_y_remap = %d",gStereoKernelProcInfo.af_win_end_y_remap);
    m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SET_PROC_INFO, &gStereoKernelProcInfo, NULL);
#endif
    MY_LOGD("SetParams out");
    return bResult;

}


/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::_stereoDepthAFPrepare(MBOOL bEnableAlgo)
{
    MY_LOGD("Prepare DAF");
    bool bResult = TRUE;

    m_afWindow.start_x = gStereoKernelProcInfo.af_win_start_x_remap ;
    m_afWindow.start_y = gStereoKernelProcInfo.af_win_start_y_remap ;
    m_afWindow.end_x = gStereoKernelProcInfo.af_win_end_x_remap ;
    m_afWindow.end_y = gStereoKernelProcInfo.af_win_end_y_remap ;

    if(gStereoKernelInitInfo.remap_main.sensor_scale_w == 0 || gStereoKernelInitInfo.remap_main.sensor_scale_h == 0)  // exception
    {
        MY_LOGE("remap fail 1.");
        bResult = FALSE;
    }
    else
    {
        MFLOAT scl_w, scl_h, offset_x, offset_y ;
        scl_w = (MFLOAT) gStereoKernelInitInfo.remap_main.sensor_size_w0/ gStereoKernelInitInfo.remap_main.sensor_scale_w ;
        scl_h = (MFLOAT) gStereoKernelInitInfo.remap_main.sensor_size_h0/ gStereoKernelInitInfo.remap_main.sensor_scale_h ;
        offset_x = (gStereoKernelInitInfo.remap_main.tg_offset_x + gStereoKernelInitInfo.remap_main.sensor_offset_x1 ) * scl_w + gStereoKernelInitInfo.remap_main.sensor_offset_x0 ;
        offset_y = (gStereoKernelInitInfo.remap_main.tg_offset_y + gStereoKernelInitInfo.remap_main.sensor_offset_y1 ) * scl_h + gStereoKernelInitInfo.remap_main.sensor_offset_y0 ;

        m_dafWindow.start_x = (MUINT)( m_afWindow.start_x * scl_w + offset_x ) ;
        m_dafWindow.start_y = (MUINT)( m_afWindow.start_y * scl_h + offset_y ) ;
        m_dafWindow.end_x   = (MUINT)( m_afWindow.end_x   * scl_w + offset_x ) ;
        m_dafWindow.end_y   = (MUINT)( m_afWindow.end_y   * scl_h + offset_y ) ;

        MY_LOGD("STEREORun remapping info scl_w %f remap_main.sensor_size_w0 %d remap_main.sensor_scale_w %d",scl_w,gStereoKernelInitInfo.remap_main.sensor_size_w0,gStereoKernelInitInfo.remap_main.sensor_scale_w);
        MY_LOGD("STEREORun remapping info scl_h %f remap_main.sensor_size_h0 %d remap_main.sensor_scale_h %d",scl_h,gStereoKernelInitInfo.remap_main.sensor_size_h0,gStereoKernelInitInfo.remap_main.sensor_scale_h);
        MY_LOGD("STEREORun remapping info offset_x %f remap_main.tg_offset_x %d remap_main.sensor_offset_x1 %d remap_main.sensor_offset_x0 %d",offset_x,gStereoKernelInitInfo.remap_main.tg_offset_x,gStereoKernelInitInfo.remap_main.sensor_offset_x1,gStereoKernelInitInfo.remap_main.sensor_offset_x0);
        MY_LOGD("STEREORun remapping info offset_y %f remap_main.tg_offset_y %d remap_main.sensor_offset_y1 %d remap_main.sensor_offset_y0 %d",offset_y,gStereoKernelInitInfo.remap_main.tg_offset_y,gStereoKernelInitInfo.remap_main.sensor_offset_y1,gStereoKernelInitInfo.remap_main.sensor_offset_y0);
        MY_LOGD("STEREORun remapping info daf_window.start_x %d daf_window.start_y %d daf_window.end_x %d daf_window.end_y %d",
                 m_dafWindow.start_x, m_dafWindow.start_y, m_dafWindow.end_x, m_dafWindow.end_y);
    }

#if ENABLE_ALGO
    #ifdef MTK_CAM_DEPTH_AF_SUPPORT

    if(gStereoKernelResultInfo.coord_trans_para)
    {
        g_prDafTbl->is_daf_run = bEnableAlgo;
        STEREODEPTH_HAL_INIT_PARAM_STRUCT stStereodepthHalInitParam;
        stStereodepthHalInitParam.pCoordTransParam = gStereoKernelResultInfo.coord_trans_para;
        mpStereoDepthHal->StereoDepthSetParams(&stStereodepthHalInitParam);
    }

    #endif  //MTK_CAM_DEPTH_AF_SUPPORT
#endif  //ENABLE_ALGO

    return bResult;
}

bool
StereoHal::_stereoDepthAFGetResult(MBOOL bEnableAlgo)
{
    bool bResult = TRUE;

#if ENABLE_ALGO
    // Algorithm main.
    MINT32 err = 0; // 0: no error. other value: error.
    if(bEnableAlgo)
    {
        err = m_pStereoDrv->StereoKernelMain();
        if (0 == err)
        {
            // Get result.
            pthread_mutex_lock(&mResultInfoLock);
            err = m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_GET_RESULT, NULL, &gStereoKernelResultInfo);
            pthread_mutex_unlock(&mResultInfoLock);
            if (err)
            {
                MY_LOGE("StereoKernelFeatureCtrl(GET_RESULT) fail. error code: %d.", err);
                bResult = FALSE;
            }

            //Dump stereo kernel debug log
            if(STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_RF == gStereoKernelInitInfo.scenario ||
               STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_3D == gStereoKernelInitInfo.scenario)
            {
                if( 1 == checkStereoProperty(PROPERTY_ALGO_BEBUG) ) {
                    static MUINT snLogCount = 0;
                    m_pStereoDrv->StereoKernelFeatureCtrl(STEREO_KERNEL_FEATURE_SAVE_LOG, &snLogCount, NULL);
                }
            }
        } else {
            MY_LOGE("StereoKernelMain() fail. error code: %d.", err);
            bResult = FALSE;
        }
    }

    if (0 == err &&
        STEREO_KERNEL_SCENARIO_IMAGE_CAPTURE_RF == gStereoKernelInitInfo.scenario)
    {
        if (0 == gStereoKernelInitInfo.remap_main.rrz_usage_width ||
            0 == gStereoKernelInitInfo.remap_main.rrz_usage_height )  // exception
        {
            MY_LOGE("remap fail 2.");
            bResult = FALSE;
        }
        else
        {
            MFLOAT scl_w, scl_h;
            scl_w = (MFLOAT)gStereoKernelInitInfo.main_source_image_width / gStereoKernelInitInfo.remap_main.rrz_usage_width ;
            scl_h = (MFLOAT)gStereoKernelInitInfo.main_source_image_height/ gStereoKernelInitInfo.remap_main.rrz_usage_height;
            m_rfsWindow.start_x = (m_afWindow.start_x - gStereoKernelInitInfo.remap_main.rrz_offset_x) * scl_w ;
            m_rfsWindow.start_y = (m_afWindow.start_y - gStereoKernelInitInfo.remap_main.rrz_offset_y) * scl_h ;
            m_rfsWindow.end_x   = (m_afWindow.end_x   - gStereoKernelInitInfo.remap_main.rrz_offset_x) * scl_w ;
            m_rfsWindow.end_y   = (m_afWindow.end_y   - gStereoKernelInitInfo.remap_main.rrz_offset_y) * scl_h ;

            MY_LOGD("STEREORun remapping info rfsWindow.start_x %d rfsWindow.start_y %d rfsWindow.end_x %d rfsWindow.end_y %d",
                     m_rfsWindow.start_x, m_rfsWindow.start_y, m_rfsWindow.end_x, m_rfsWindow.end_y);
        }

        const int IMAGE_SIZE = gStereoKernelInitInfo.algo_output_image_width * gStereoKernelInitInfo.algo_output_image_height;
        const int ALGO_INFO_COUNT   = 12;
        const int VERIFY_INFO_COUNT = 0;//12;
        const int FD_INFO_COUNT = 4;
        const int DEBUG_INFO_SIZE   = sizeof(MUINT32)*(ALGO_INFO_COUNT + VERIFY_INFO_COUNT + FD_INFO_COUNT);
        m_uDebugInfotSize = IMAGE_SIZE + DEBUG_INFO_SIZE;
        if(m_pDebugInforkBuf) {
            free(m_pDebugInforkBuf);
            m_pDebugInforkBuf = NULL;
        }
        m_pDebugInforkBuf = (MUINT8*)malloc( (sizeof(MUINT8) * m_uDebugInfotSize));
        ::memset(m_pDebugInforkBuf, 0, sizeof(MUINT8) * m_uDebugInfotSize);

        MUINT32* temp = (MUINT32*)m_pDebugInforkBuf;
        MUINT8* rgbaimg = (MUINT8*)gStereoKernelProcInfo.warp_image_addr_dst;
        //Algo info
        *(temp)= (mAlgoAppendSize.w*2);
        *(temp+1)= mAlgoAppendSize.h;
        *(temp+2)= gStereoKernelInitInfo.algo_output_image_width;
        *(temp+3)= gStereoKernelInitInfo.algo_output_image_height;
        *(temp+4)= gStereoKernelResultInfo.algo_align_shift_x;
        *(temp+5)= gStereoKernelResultInfo.algo_align_shift_y;
        *(temp+6)= gStereoKernelInitInfo.algo_source_image_width;
        *(temp+7)= gStereoKernelInitInfo.algo_source_image_height;
        *(temp+8)= m_nCapOrientation;
        *(temp+9)= m_uSensorRelativePosition;

        //ALPS01962579: The point is used as the starting point of image refocusing, therefore they cannot be negative
        MPoint ptAF;
        ptAF.x = ((m_rfsWindow.start_x + m_rfsWindow.end_x)/2);
        if(ptAF.x < 0) {
            ptAF.x = 0;
        } else if (ptAF.x >= (gStereoKernelInitInfo.remap_main.pixel_array_width>>1)) {
            ptAF.x = (gStereoKernelInitInfo.remap_main.pixel_array_width>>1) - 1;
        }
        *(temp+10) = ptAF.x;

        ptAF.y = ((m_rfsWindow.start_y + m_rfsWindow.end_y)/2);
        if(ptAF.y < 0) {
            ptAF.y = 0;
        } else if (ptAF.y >= (gStereoKernelInitInfo.remap_main.pixel_array_height>>1)) {
            ptAF.y = (gStereoKernelInitInfo.remap_main.pixel_array_height>>1) - 1;
        }
        *(temp+11) = ptAF.y;

        MY_LOGD("AF point: (%d, %d)", *(temp+10), *(temp+11));

        //Verification info
        *(temp+12)= gStereoKernelResultInfo.verify_geo_quality_level;   // 0: PASS, 1:WARN, 2:FAIL
        *(temp+13)= gStereoKernelResultInfo.verify_pho_quality_level;   // 0: PASS, 1:WARN, 2:FAIL
        *(temp+14)= gStereoKernelResultInfo.verify_geo_statistics[0];   // black boundary, 4 bits (0-15)
        *(temp+15)= gStereoKernelResultInfo.verify_geo_statistics[1];   // black percentage (%)
        *(temp+16)= gStereoKernelResultInfo.verify_geo_statistics[2];   // Space to black margin
        *(temp+17)= gStereoKernelResultInfo.verify_geo_statistics[3];
        *(temp+18)= gStereoKernelResultInfo.verify_geo_statistics[4];
        *(temp+19)= gStereoKernelResultInfo.verify_geo_statistics[5];
        *(temp+20)= gStereoKernelResultInfo.verify_pho_statistics[0];   // mean intensity difference (0-255)
        *(temp+21)= gStereoKernelResultInfo.verify_pho_statistics[1];   // color similarity of R-channel (%)
        *(temp+22)= gStereoKernelResultInfo.verify_pho_statistics[2];   // color similarity of G-channel (%)
        *(temp+23)= gStereoKernelResultInfo.verify_pho_statistics[3];   // color similarity of B-channel (%)

        // Shane 20150323 capture FD
        // Bounds of the face [left, top, right, bottom]. (-1000, -1000) represents
        // the top-left of the camera field of view, and (1000, 1000) represents the
        // bottom-right of the field of view.
        *(temp+24) = (MUINT32)mFDInfo.rect[0]; // left
        *(temp+25) = (MUINT32)mFDInfo.rect[1]; // top
        *(temp+26) = (MUINT32)mFDInfo.rect[2]; // right
        *(temp+27) = (MUINT32)mFDInfo.rect[3]; // bottom
        MY_LOGD("StereoHal capture FD result (%d/%d/%d/%d)",
                (MUINT32)mFDInfo.rect[0],
                (MUINT32)mFDInfo.rect[1],
                (MUINT32)mFDInfo.rect[2],
                (MUINT32)mFDInfo.rect[3]
        );

        MUINT8 *pDebugImageOffset = m_pDebugInforkBuf + DEBUG_INFO_SIZE;
        MUINT8 *pRGBAImgOffset = rgbaimg + 3;
        for(int i = IMAGE_SIZE - 1; i >= 0; i--)
        {
            *(pDebugImageOffset + i)= *(pRGBAImgOffset + (i<<2));
        }
    }

    MY_LOGD("Run get result out");
#endif  // ENABLE_ALGO

    return bResult;
}

bool
StereoHal::STEREODepthAFQuery()
{
#if ENABLE_ALGO
    #ifdef MTK_CAM_DEPTH_AF_SUPPORT

    //1. Get feature matching data from stereo algo (Benson)
    //m_pStereoDrv is static(declared in AppStereoKernel.cpp), just use it
    if (NULL == m_pStereoDrv) {
        MY_LOGE("StereoDrv is NULL");
        return FALSE;
    }

    if (NULL == gStereoKernelResultInfo.coord_trans_para)
    {
        MY_LOGE("coord_trans_para is NULL");
        return FALSE;
    }

    //2. Query
    //2.1 Prepare AF window
    if(gStereoKernelInitInfo.remap_main.sensor_scale_w == 0 || gStereoKernelInitInfo.remap_main.sensor_scale_h == 0)  // exception
    {
        MY_LOGE("Remap fail");
        return FALSE;
    }
    else
    {
        MFLOAT scl_w, scl_h, offset_x, offset_y ;
        scl_w = (MFLOAT) gStereoKernelInitInfo.remap_main.sensor_size_w0/ gStereoKernelInitInfo.remap_main.sensor_scale_w ;
        scl_h = (MFLOAT) gStereoKernelInitInfo.remap_main.sensor_size_h0/ gStereoKernelInitInfo.remap_main.sensor_scale_h ;
        offset_x = (gStereoKernelInitInfo.remap_main.tg_offset_x + gStereoKernelInitInfo.remap_main.sensor_offset_x1 ) * scl_w + gStereoKernelInitInfo.remap_main.sensor_offset_x0 ;
        offset_y = (gStereoKernelInitInfo.remap_main.tg_offset_y + gStereoKernelInitInfo.remap_main.sensor_offset_y1 ) * scl_h + gStereoKernelInitInfo.remap_main.sensor_offset_y0 ;

        AF_WIN dafWin;
        const MUINT32 QUERY_IDX = g_prDafTbl->is_query_happen % DAF_TBL_QLEN;
        dafWin.start_x = g_prDafTbl->daf_vec[QUERY_IDX].af_win_start_x ;   // 1/8 windows of 4096 = 512 ;
        dafWin.start_y = g_prDafTbl->daf_vec[QUERY_IDX].af_win_end_x ;     // 2304-1793+1 = 512
        dafWin.end_x   = g_prDafTbl->daf_vec[QUERY_IDX].af_win_start_y ;   // 1/8 windows of 3072 = 384
        dafWin.end_y   = g_prDafTbl->daf_vec[QUERY_IDX].af_win_end_y ;     // 1728-1345+1 = 384

        AF_WIN_COORDINATE_STRUCT stAfWinCoordinate;
        stAfWinCoordinate.af_win_start_x = (MUINT)( dafWin.start_x * scl_w + offset_x );
        stAfWinCoordinate.af_win_start_y = (MUINT)( dafWin.start_y * scl_h + offset_y );
        stAfWinCoordinate.af_win_end_x   = (MUINT)( dafWin.end_x   * scl_w + offset_x );
        stAfWinCoordinate.af_win_end_y   = (MUINT)( dafWin.end_y   * scl_h + offset_y );

        //2.2 Query
        MY_LOGD("[Query]num_hwfe_match: %d AF win: (%u, %u), (%u, %u)",
                gStereoKernelResultInfo.num_hwfe_match,
                stAfWinCoordinate.af_win_start_x, stAfWinCoordinate.af_win_start_y,
                stAfWinCoordinate.af_win_end_x, stAfWinCoordinate.af_win_end_y);

        //mpStereoDepthHal is static singleton, so we can just use it
        pthread_mutex_lock(&mResultInfoLock);
        mpStereoDepthHal->StereoDepthQuery(gStereoKernelResultInfo.num_hwfe_match,
                                           gStereoKernelResultInfo.hwfe_match_data,
                                           &stAfWinCoordinate);
        pthread_mutex_unlock(&mResultInfoLock);
    }

    #endif  // MTK_CAM_DEPTH_AF_SUPPORT
#endif  // ENABLE_ALGO

    //3. return
    return TRUE;
}

bool
StereoHal::STEREORun(OUT_DATA_STEREO_T &OutData, MBOOL bEnableAlgo)
{
//    MY_LOGD("Enable algo: %d", bEnableAlgo);
    bool bResult = _stereoDepthAFPrepare(bEnableAlgo);

    //is_learning flag may be set before running StereoKernelMain, therefore num_hwfe_match will be 0.
    //So we add s_bDelayLearning to run learning in the future when num_hwfe_match is not 0.

#if ENABLE_ALGO
    #ifdef MTK_CAM_DEPTH_AF_SUPPORT

    if(gStereoKernelResultInfo.coord_trans_para)
    {
        AF_WIN_COORDINATE_STRUCT stAfWinCoordinate;
        stAfWinCoordinate.af_win_start_x    = m_dafWindow.start_x;
        stAfWinCoordinate.af_win_start_y    = m_dafWindow.start_y;
        stAfWinCoordinate.af_win_end_x      = m_dafWindow.end_x;
        stAfWinCoordinate.af_win_end_y      = m_dafWindow.end_y;
        mpStereoDepthHal->StereoDepthPrintDafTable();   // For debug only.

        if (bEnableAlgo && g_prDafTbl->is_query_happen != INVALID_QUERY)
        {
            //Michael: To enhance performance, try to do query by stereo algo driver instead
            MY_LOGD("Query, num_hwfe_match = %d, is_query_happen: %d(idx: %d)",
                     gStereoKernelResultInfo.num_hwfe_match,
                     g_prDafTbl->is_query_happen,
                     g_prDafTbl->is_query_happen % DAF_TBL_QLEN);
            mpStereoDepthHal->StereoDepthRunQuerying(gStereoKernelResultInfo.num_hwfe_match, gStereoKernelResultInfo.hwfe_match_data, &stAfWinCoordinate);
        }

        bResult = (_stereoDepthAFGetResult(bEnableAlgo) && bResult);

        static bool s_bDelayLearning = FALSE;
        if (bEnableAlgo &&
            (g_prDafTbl->daf_vec[g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN].is_learning || s_bDelayLearning))
        {
            MY_LOGD("Learn, is_learning = %d, delay learning = %d, num_hwfe_match = %d",
                      g_prDafTbl->daf_vec[g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN].is_learning,
                      s_bDelayLearning, gStereoKernelResultInfo.num_hwfe_match);

            static int s_nLearningTimeout = LEARNING_TIMEOUT;
            s_bDelayLearning = TRUE;
            if (0 != gStereoKernelResultInfo.num_hwfe_match)
            {
                mpStereoDepthHal->StereoDepthRunLearning(gStereoKernelResultInfo.num_hwfe_match, gStereoKernelResultInfo.hwfe_match_data, &stAfWinCoordinate);
                s_bDelayLearning = FALSE;
                s_nLearningTimeout = LEARNING_TIMEOUT;
            } else {
                s_nLearningTimeout--;
                if(s_nLearningTimeout == 0) {
                    s_bDelayLearning = FALSE;
                    MY_LOGD("Learning timeout");
                }
            }
        }

        MY_LOGD("curr_p2_frm_num(%d Idx:%d), is_query_happen(%d Idx:%d), EnableAlgo(%d), is_learning(%d), num_hwfe_match(%d), hwfe_match_data: 0x%08x (value: %d)",
            g_prDafTbl->curr_p2_frm_num,
            g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN,
            g_prDafTbl->is_query_happen,
            g_prDafTbl->is_query_happen % DAF_TBL_QLEN,
            bEnableAlgo,
            g_prDafTbl->daf_vec[g_prDafTbl->curr_p2_frm_num % DAF_TBL_QLEN].is_learning,
            gStereoKernelResultInfo.num_hwfe_match,
            gStereoKernelResultInfo.hwfe_match_data,
            *(gStereoKernelResultInfo.hwfe_match_data)
        );
    } else {
        MY_LOGD("Get first coord_trans_para");
        bResult = (_stereoDepthAFGetResult(bEnableAlgo) && bResult);
    }

    #endif  // MTK_CAM_DEPTH_AF_SUPPORT
#endif  // ENABLE_ALGO

lb_Abnormal_Exit:

#if ENABLE_ALGO
    if(0 == m_uSensorRelativePosition)
    {
        OutData.algo_main1.p.x  = gStereoKernelResultInfo.algo_left_offset_x;  // Image Capture
        OutData.algo_main1.p.y  = gStereoKernelResultInfo.algo_left_offset_y;  // Image Capture
        OutData.algo_main1.s    = _getAlgoImgSize(0);

        OutData.algo_main2.p.x  = gStereoKernelResultInfo.algo_right_offset_x ;  // Image Capture
        OutData.algo_main2.p.y  = gStereoKernelResultInfo.algo_right_offset_y ;  // Image Capture
        OutData.algo_main2.s    = _getAlgoImgSize(1);
    }
    else
    {
        OutData.algo_main1.p.x  = gStereoKernelResultInfo.algo_right_offset_x;  // Image Capture
        OutData.algo_main1.p.y  = gStereoKernelResultInfo.algo_right_offset_y;  // Image Capture
        OutData.algo_main1.s    = _getAlgoImgSize(0);

        OutData.algo_main2.p.x  = gStereoKernelResultInfo.algo_left_offset_x ;  // Image Capture
        OutData.algo_main2.p.y  = gStereoKernelResultInfo.algo_left_offset_y ;  // Image Capture
        OutData.algo_main2.s    = _getAlgoImgSize(1);
    }

    OutData.main_crop.p.x   = gStereoKernelResultInfo.main_offset_x;  // Image Refocus
    OutData.main_crop.p.y   = gStereoKernelResultInfo.main_offset_y;  // Image Refocus
    OutData.main_crop.s     = _getMainImgSize();
#else
    OutData.main_crop.p.x   = 0;  // Image Refocus
    OutData.main_crop.p.y   = 0;  // Image Refocus
    OutData.main_crop.s     = _getMainImgSize();

    OutData.algo_main1.p.x   = 0;
    OutData.algo_main1.p.y   = 0;
    OutData.algo_main1.s     = _getAlgoImgSize(0);

    OutData.algo_main2.p.x   = 0;
    OutData.algo_main2.p.y   = 0;
    OutData.algo_main2.s     = _getAlgoImgSize(0);
#endif

    MY_LOGD("- X. algo_main1(%d,%d,%d,%d), algo_main2(%d,%d,%d,%d), main_crop(%d,%d,%d,%d)",
            OutData.algo_main1.p.x, OutData.algo_main1.p.y,
            OutData.algo_main1.s.w, OutData.algo_main1.s.h,
            OutData.algo_main2.p.x, OutData.algo_main2.p.y,
            OutData.algo_main2.s.w, OutData.algo_main2.s.h,
            OutData.main_crop.p.x, OutData.main_crop.p.y,
            OutData.main_crop.s.w, OutData.main_crop.s.h);
    MY_LOGD("Run out");

    pthread_mutex_unlock(&mRunLock);
    return bResult;
}

/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::STEREOGetDebugInfo(DBG_DATA_STEREO_T &DbgData)
{
    MBOOL Result = MTRUE;

    DbgData.dbgDataSize=m_uDebugInfotSize;
    DbgData.dbgDataAddr=m_pDebugInforkBuf;
    return Result;
}

/*******************************************************************************
*
********************************************************************************/
bool
StereoHal::STEREODestroy(void)
{
    MY_LOGD("+");
    pthread_mutex_lock(&mRunLock);  //Wait for algo to finish
    MBOOL Result = MTRUE;

#if ENABLE_ALGO
    // Reset algorithm.
    m_pStereoDrv->StereoKernelReset();
    if(m_pWorkBuf) {
        free(m_pWorkBuf);
        m_pWorkBuf = NULL;
    }

    if(m_pDebugInforkBuf) {
        free(m_pDebugInforkBuf);
        m_pDebugInforkBuf = NULL;
    }
    //
    mpHal3A->destroyInstance("MTKStereoCamera");
    //
    #ifdef MTK_CAM_DEPTH_AF_SUPPORT
    mpStereoDepthHal->StereoDepthUninit();
    mpStereoDepthHal->destroyInstance();  // pStereoDepthHal->uninit() has already run inside destroyInstance().

    #endif  // MTK_CAM_DEPTH_AF_SUPPORT
#endif
    MY_LOGD("- X. Result: %d.", Result);
    pthread_mutex_unlock(&mRunLock);
    pthread_mutex_unlock(&mInitDestroyLock);
    return Result;

}

/*******************************************************************************
* lenNVRAM: must use "byte" as unit.
* adb shell setprop debuglog.stereo.printloadednvram 1: print arrNVRAM that loaded from NVRAM.
********************************************************************************/
bool
StereoHal::_loadFromNvram()
{
    //MY_LOGD("- E. arrNVRAM: 0x%08x. lenNVRAM: %d.", arrNVRAM, lenNVRAM);

    MBOOL Result = MTRUE;
    MINT32 err = 0; // 0: no error. other value: error.
    int sensorDevIdx = 0;
    // Check if arrNVRAM is valid.
    //if (arrNVRAM == NULL)
    //{
    //    MY_LOGE("NVRAM array is NULL.");
    //    Result = MFALSE;
    //    goto lb_Abnormal_Exit;
    //}
    IHalSensorList* sensorlsit;
    sensorlsit=IHalSensorList::get();
    sensorDevIdx=sensorlsit->querySensorDevIdx(m_nMain1SensorIdx);

    err = NvBufUtil::getInstance().getBufAndRead(CAMERA_NVRAM_DATA_GEOMETRY, sensorDevIdx, (void*&)m_pVoidGeoData);
    // memcpy(arrNVRAM,m_pVoidGeoData->Data,MAXIMUM_NVRAM_CAMERA_GEOMETRY_FILE_SIZE); // Aaron
lb_Abnormal_Exit:

    MY_LOGD("- X. Result: %d.", Result);

    return Result;

}


/*******************************************************************************
* lenNVRAM: must use "byte" as unit.
* adb shell setprop debuglog.stereo.erasenvram 1: set to 1 to write all 0's into NVRAM. (Remember to set to 0 after erased.)
* adb shell setprop debuglog.stereo.printsavednvram 1: print arrNVRAM that saved to NVRAM.
********************************************************************************/
bool
StereoHal::_saveToNvram()
{
    //MY_LOGD("- E. arrNVRAM: 0x%08x. lenNVRAM: %d.", arrNVRAM, lenNVRAM);

    MBOOL Result = MTRUE;
    MINT32 err = 0; // 0: no error. other value: error.
    // Check if arrNVRAM is valid.
    //if (arrNVRAM == NULL)
    // {
    //    MY_LOGE("NVRAM array is NULL.");
    //    Result = MFALSE;
    //    goto lb_Abnormal_Exit;
    //}
    IHalSensorList* sensorlsit;
    sensorlsit=IHalSensorList::get();
    int sensorDevIdx = sensorlsit->querySensorDevIdx(m_nMain1SensorIdx);

    //NVRAM_CAMERA_GEOMETRY_STRUCT* pVoidGeoData;
    //err = NvBufUtil::getInstance().getBufAndRead(CAMERA_NVRAM_DATA_GEOMETRY, sensorDevIdx, (void*&)pVoidGeoData);
    // memcpy(pVoidGeoData->Data,arrNVRAM,MAXIMUM_NVRAM_CAMERA_GEOMETRY_FILE_SIZE); // Aaron
    err = NvBufUtil::getInstance().write(CAMERA_NVRAM_DATA_GEOMETRY, sensorDevIdx);

    if (err!=0)
    {
        MY_LOGE("Write to NVRAM fail.");
        Result = MFALSE;
        goto lb_Abnormal_Exit;
    }

lb_Abnormal_Exit:

    MY_LOGD("- X. Result: %d.", Result);

    return Result;

}

bool
StereoHal::STEREOSetFDInfo(FD_DATA_STEREO_T fdInfo)
{
    mFDInfo.rect[0] = fdInfo.left;
    mFDInfo.rect[1] = fdInfo.top;
    mFDInfo.rect[2] = fdInfo.right;
    mFDInfo.rect[3] = fdInfo.bottom;

    return true;
}
