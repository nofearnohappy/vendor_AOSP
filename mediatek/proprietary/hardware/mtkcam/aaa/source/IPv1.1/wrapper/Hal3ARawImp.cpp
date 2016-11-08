/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#define LOG_TAG "Hal3ARaw"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <cutils/properties.h>
#include <string.h>
#include <aaa_log.h>
#include <IThreadRaw.h>
#include <IHalSensor.h>
#ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly - USING_FAKE_SENSOR
#include <drv/src/isp/mt6797/iopipe/CamIO/FakeSensor.h>
#endif

#include "Hal3ARaw.h"
#include <ae_mgr/ae_mgr.h>
#include <awb_mgr/awb_mgr_if.h>
#include <sensor_mgr/aaa_sensor_mgr.h>
#include <sensor_mgr/aaa_sensor_buf_mgr.h>
#include <isp_tuning/isp_tuning_mgr.h>
#include <isp_mgr/isp_mgr.h>

#include <state_mgr/aaa_state_mgr.h>
#include <state_mgr_af/af_state_mgr.h>

#include <CamIO/INormalPipe.h>
#include <exif/IBaseCamExif.h>
#include <debug_exif/aaa/dbg_aaa_param.h>

#if CAM3_AF_FEATURE_EN
#include <debug_exif/aaa/dbg_af_param.h>
#include <af_feature.h>
#include <af_algo_if.h>
#include <af_mgr/af_mgr_if.h>
#include <af_mgr/af_mgr.h>
#endif

#if CAM3_FLASH_FEATURE_EN
#include <flash_mgr/flash_mgr.h>
#endif

#if CAM3_FLICKER_FEATURE_EN
#include <flicker_hal.h>
#endif

#if CAM3_LSC_FEATURE_EN
#include <ILscMgr.h>
#endif

#if defined(HAVE_AEE_FEATURE)
#include <aee.h>
#define AEE_ASSERT_3A_HAL(String) \
          do { \
              aee_system_exception( \
                  "Hal3A", \
                  NULL, \
                  DB_OPT_DEFAULT, \
                  String); \
          } while(0)
#else
#define AEE_ASSERT_3A_HAL(String)
#endif

#define GET_PROP(prop, init, val)\
{\
    char value[PROPERTY_VALUE_MAX] = {'\0'};\
    property_get(prop, value, (init));\
    (val) = atoi(value);\
}

using namespace android;
using namespace NS3Av3;
using namespace NSIspTuningv3;

class Hal3ARawImp : public Hal3ARaw
{
public:
    static I3AWrapper*  createInstance(MINT32 const i4SensorOpenIndex);
    virtual MVOID       destroyInstance();
    virtual MBOOL       start();
    virtual MBOOL       stop();
    virtual MVOID       setSensorMode(MINT32 i4SensorMode);
    virtual MBOOL       generateP2(MINT32 flowType, const NSIspTuning::RAWIspCamInfo& rCamInfo, void* pRegBuf);
    virtual MBOOL       validateP1(const ParamIspProfile_T& rParamIspProfile, MBOOL fgPerframe);
    virtual MBOOL       setParams(Param_T const &rNewParam);
    virtual MBOOL       autoFocus();
    virtual MBOOL       cancelAutoFocus();
    virtual MVOID       setFDEnable(MBOOL fgEnable);
    virtual MBOOL       setFDInfo(MVOID* prFaces);
    virtual MBOOL       setFlashLightOnOff(MBOOL bOnOff/*1=on; 0=off*/, MBOOL bMainPre/*1=main; 0=pre*/);
    virtual MBOOL       chkMainFlashOnCond() const;
    virtual MBOOL       chkPreFlashOnCond() const;
    virtual MBOOL       isStrobeBVTrigger() const;
    virtual MBOOL       chkCapFlash() const {return MFALSE;}
    virtual MINT32      getCurrResult(MUINT32 i4FrmId, Result_T& rResult) const;
    virtual MINT32      getCurrentHwId() const;
    virtual MBOOL       postCommand(ECmd_T const r3ACmd, const ParamIspProfile_T* pParam = 0);
    virtual MINT32      send3ACtrl(E3ACtrl_T e3ACtrl, MINTPTR iArg1, MINTPTR iArg2){return 0;}

protected:  //    Ctor/Dtor.
                        Hal3ARawImp(MUINT32 const i4SensorDevId, MINT32 const i4SensorOpenIndex);
    virtual             ~Hal3ARawImp(){}

                        Hal3ARawImp(const Hal3ARawImp&);
                        Hal3ARawImp& operator=(const Hal3ARawImp&);

    MBOOL               init();
    MBOOL               uninit();
    MRESULT             updateTGInfo();
    MBOOL               doStart();
    MBOOL               get3AEXIFInfo(EXIF_3A_INFO_T& rExifInfo) const;

private:
    MINT32              m_3ALogEnable;
    volatile int        m_Users;
    mutable Mutex       m_Lock;
    Mutex               mP2Mtx;
    Mutex               m3AOperMtx1;
    Mutex               m3AOperMtx2;
    MINT32              m_i4SensorIdx;
    MINT32              m_i4SensorDev;
    MUINT32             m_u4SensorMode;
    MUINT               m_u4TgInfo;
    MBOOL               m_bEnable3ASetParams;
    MBOOL               m_bFaceDetectEnable;
    MINT32              m_i4InCaptureProcess;

private:
    NSCam::NSIoPipe::NSCamIOPipe::INormalPipe* m_pCamIO;
    IspTuningMgr*       m_pTuning;
    IThreadRaw*         m_pThreadRaw;
    StateMgr*           m_pStateMgr;
    AfStateMgr*         m_pAfStateMgr;
    Param_T             m_rParam;
};

template <MINT32 sensorDevId>
class Hal3ARawImpDev : public Hal3ARawImp
{
public:
    static Hal3ARawImpDev* getInstance(MUINT32 const i4SensorDevId, MINT32 const i4SensorOpenIndex)
    {
        static Hal3ARawImpDev<sensorDevId> singleton(i4SensorDevId, i4SensorOpenIndex);
        return &singleton;
    }
    Hal3ARawImpDev(MUINT32 const i4SensorDevId, MINT32 const i4SensorOpenIndex)
        : Hal3ARawImp(i4SensorDevId, i4SensorOpenIndex)
    {
    }

private:

};

I3AWrapper*
Hal3ARaw::
createInstance(MINT32 const i4SensorOpenIndex)
{
    return Hal3ARawImp::createInstance(i4SensorOpenIndex);
}

I3AWrapper*
Hal3ARawImp::
createInstance(MINT32 const i4SensorOpenIndex)
{
#ifdef USING_MTK_LDVT
    IHalSensorList*const pHalSensorList = TS_FakeSensorList::getTestModel();
#else
    IHalSensorList*const pHalSensorList = IHalSensorList::get();
#endif
    MUINT32 i4SensorDevId = pHalSensorList->querySensorDevIdx(i4SensorOpenIndex);

    switch (i4SensorDevId)
    {
        case SENSOR_DEV_MAIN:
            Hal3ARawImpDev<SENSOR_DEV_MAIN>::getInstance(i4SensorDevId, i4SensorOpenIndex)->init();
            return Hal3ARawImpDev<SENSOR_DEV_MAIN>::getInstance(i4SensorDevId, i4SensorOpenIndex);
        break;
        case SENSOR_DEV_SUB:
            Hal3ARawImpDev<SENSOR_DEV_SUB>::getInstance(i4SensorDevId, i4SensorOpenIndex)->init();
            return Hal3ARawImpDev<SENSOR_DEV_SUB>::getInstance(i4SensorDevId, i4SensorOpenIndex);
        break;
        case SENSOR_DEV_MAIN_2:
            Hal3ARawImpDev<SENSOR_DEV_MAIN_2>::getInstance(i4SensorDevId, i4SensorOpenIndex)->init();
            return Hal3ARawImpDev<SENSOR_DEV_MAIN_2>::getInstance(i4SensorDevId, i4SensorOpenIndex);
        break;
        default:
            MY_ERR("Unsupport sensor device ID: %d\n", i4SensorDevId);
            AEE_ASSERT_3A_HAL("Unsupport sensor device.");
            return MNULL;
    }
}

MVOID
Hal3ARawImp::
destroyInstance()
{
    MY_LOG("[%s]", __FUNCTION__);
    uninit();
}

Hal3ARawImp::
Hal3ARawImp(MUINT32 const i4SensorDevId, MINT32 const i4SensorOpenIndex)
    : m_pCamIO(NULL)
    , m_pTuning(NULL)
    , m_i4SensorIdx(i4SensorOpenIndex)
    , m_i4SensorDev(i4SensorDevId)
    , m_u4SensorMode(0)
{
    MY_LOG("[%s] sensorDev(%d) sensorIdx(%d)", __FUNCTION__, i4SensorDevId, i4SensorOpenIndex);
}

MBOOL
Hal3ARawImp::
init()
{
    GET_PROP("debug.camera.log", "0", m_3ALogEnable);
    if ( m_3ALogEnable == 0 ) {
        GET_PROP("debug.camera.log.hal3a", "0", m_3ALogEnable);
    }

    MY_LOG("[%s] m_Users: %d \n", __FUNCTION__, m_Users);

    // check user count
    MRESULT ret = S_3A_OK;
    MBOOL bRet = MTRUE;
    Mutex::Autolock lock(m_Lock);

    if (m_Users > 0)
    {
        MY_LOG("[%s] %d has created \n", __FUNCTION__, m_Users);
        android_atomic_inc(&m_Users);
        return S_3A_OK;
    }

    // init Thread and state mgr
    m_pThreadRaw = IThreadRaw::createInstance(this, m_i4SensorDev, m_i4SensorIdx);
    m_pStateMgr = new StateMgr(m_i4SensorDev);
    m_pAfStateMgr = new AfStateMgr(m_i4SensorDev);

    m_pStateMgr->setThreadRaw(m_pThreadRaw);

    // AE init
    IAeMgr::getInstance().cameraPreviewInit(m_i4SensorDev, m_i4SensorIdx, m_rParam);

    // AWB init
    IAwbMgr::getInstance().init(m_i4SensorDev, m_i4SensorIdx);

    // AF init
#if CAM3_AF_FEATURE_EN
    IAfMgr::getInstance().setCallbacks(m_i4SensorDev, mpCbSet);
    IAfMgr::getInstance().init(m_i4SensorDev, m_i4SensorIdx);
#endif

    //FLASH init
#if CAM3_FLASH_FEATURE_EN
    FlashMgr::getInstance().init(m_i4SensorDev, m_i4SensorIdx);
#endif

    // TuningMgr init
    if (m_pTuning == NULL)
    {
        m_pTuning = &IspTuningMgr::getInstance();
        if (!m_pTuning->init(m_i4SensorDev, m_i4SensorIdx))
        {
            MY_ERR("Fail to init IspTuningMgr (%d,%d)", m_i4SensorDev, m_i4SensorIdx);
            AEE_ASSERT_3A_HAL("Fail to init IspTuningMgr");
            return MFALSE;
        }
    }

    // state mgr transit to Init state.
    bRet = postCommand(ECmd_Init);
    if (!bRet) AEE_ASSERT_3A_HAL("ECmd_Init fail.");

    MY_LOG("[%s] done\n", __FUNCTION__);
    android_atomic_inc(&m_Users);
    return S_3A_OK;
}

MBOOL
Hal3ARawImp::
uninit()
{
    MRESULT ret = S_3A_OK;
    MBOOL bRet = MTRUE;

    Mutex::Autolock lock(m_Lock);

    // If no more users, return directly and do nothing.
    if (m_Users <= 0)
    {
        return S_3A_OK;
    }
    MY_LOG("[%s] m_Users: %d \n", __FUNCTION__, m_Users);

    // More than one user, so decrease one User.
    android_atomic_dec(&m_Users);

    if (m_Users == 0) // There is no more User after decrease one User
    {
        bRet = postCommand(ECmd_Uninit);
        if (!bRet) AEE_ASSERT_3A_HAL("ECmd_Uninit fail.");

        m_pThreadRaw->destroyInstance();

        MRESULT err = S_3A_OK;

        // AE uninit
        err = IAeMgr::getInstance().uninit(m_i4SensorDev);
        if (FAILED(err)){
            MY_ERR("IAeMgr::getInstance().uninit() fail\n");
            return err;
        }

        // AWB uninit
        err = IAwbMgr::getInstance().uninit(m_i4SensorDev);
        if (FAILED(err)){
            MY_ERR("IAwbMgr::getInstance().uninit() fail\n");
            return E_3A_ERR;
        }

#if CAM3_AF_FEATURE_EN
        // AF uninit
        err = IAfMgr::getInstance().uninit(m_i4SensorDev);
        if (FAILED(err)) {
            MY_ERR("IAfMgr::getInstance().uninit() fail\n");
            return err;
        }
#endif

#if CAM3_FLASH_FEATURE_EN
        // FLASH uninit
        err = FlashMgr::getInstance().uninit(m_i4SensorDev);
        if (FAILED(err)) {
            MY_ERR("FlashMgr::getInstance().uninit() fail\n");
            return err;
        }
#endif

        // TuningMgr uninit
        if (m_pTuning)
        {
            m_pTuning->uninit(m_i4SensorDev);
            m_pTuning = NULL;
        }

        delete m_pStateMgr;
        m_pStateMgr = NULL;
        delete m_pAfStateMgr;
        m_pAfStateMgr = NULL;

        MY_LOG("[%s] done\n", __FUNCTION__);

    }
    else    // There are still some users.
    {
        MY_LOG("[%s] Still %d users \n", __FUNCTION__, m_Users);
    }

    return S_3A_OK;
}

MBOOL
Hal3ARawImp::
start()
{
    MY_LOG("[%s] +", __FUNCTION__);
    MRESULT err = S_3A_OK;
    MRESULT isAFLampOn = MFALSE;
    m_i4InCaptureProcess = 0;

    // NormalIOPipe create instance
    if (m_pCamIO == NULL)
    {
        m_pCamIO = NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::
                        createInstance(m_i4SensorIdx, LOG_TAG);
        if (m_pCamIO == NULL)
        {
            MY_ERR("Fail to create NormalPipe");
            return MFALSE;
        }
    }

    doStart();

    // FIXME (remove): update TG Info to 3A modules
    updateTGInfo();

    // AE start
    err = IAeMgr::getInstance().Start(m_i4SensorDev);
    if (FAILED(err)) {
    MY_ERR("IAeMgr::getInstance().Start() fail\n");
        return err;
    }

    // AWB start
    err = IAwbMgr::getInstance().start(m_i4SensorDev);
    if (!err) {
        MY_ERR("IAwbMgr::getInstance().start() fail\n");
        return E_3A_ERR;
    }

#if CAM3_FLASH_FEATURE_EN
    // Flash cameraPreviewStart + start
    err = FlashMgr::getInstance().cameraPreviewStart(m_i4SensorDev);
    if (!err) {
        MY_ERR("FlashMgr::getInstance().cameraPreviewStart() fail\n");
        return E_3A_ERR;
    }
    err = FlashMgr::getInstance().start(m_i4SensorDev);
    if (!err) {
        MY_ERR("FlashMgr::getInstance().start() fail\n");
        return E_3A_ERR;
    }
    isAFLampOn = FlashMgr::getInstance().isAFLampOn(m_i4SensorDev);
#endif

#if CAM3_AF_FEATURE_EN
    // AF start
    err = IAfMgr::getInstance().Start(m_i4SensorDev);
    if (FAILED(err)) {
        MY_ERR("AfMgr::getInstance().Start() fail\n");
        return err;
    }
    // enable AF thread
    m_pThreadRaw->enableAFThread(m_pAfStateMgr);
#endif

#if CAM3_FLICKER_FEATURE_EN
    // Flicker start
    // AAASensorMgr::getInstance().setFlickerFrameRateActive(m_i4SensorDev, 1);
    FlickerHalBase::getInstance().open(m_i4SensorDev, m_i4SensorIdx, m_u4TgInfo);
#endif

    // apply 3A module's config
    IspTuningMgr::getInstance().validate(m_i4SensorDev, MTRUE);

    // setStrobeMode
    IAeMgr::getInstance().setStrobeMode(m_i4SensorDev, isAFLampOn ? MTRUE : MFALSE);
    IAwbMgr::getInstance().setStrobeMode(m_i4SensorDev, isAFLampOn ? AWB_STROBE_MODE_ON : AWB_STROBE_MODE_OFF);

    MY_LOG("[%s] -", __FUNCTION__);
    return MTRUE;
}

MBOOL
Hal3ARawImp::
stop()
{
    MY_LOG("[%s] +", __FUNCTION__);
    MRESULT err = S_3A_OK;

    // AE stop
    err = IAeMgr::getInstance().Stop(m_i4SensorDev);
    if (FAILED(err)) {
    MY_ERR("IAeMgr::getInstance().Stop() fail\n");
        return err;
    }

    // AWB stop
    err = IAwbMgr::getInstance().stop(m_i4SensorDev);
    if (!err) {
        MY_ERR("IAwbMgr::getInstance().stop() fail\n");
        return E_3A_ERR;
    }

#if CAM3_FLASH_FEATURE_EN
    // Flash cameraPreviewEnd + stop
    err = FlashMgr::getInstance().cameraPreviewEnd(m_i4SensorDev);
    if (!err) {
        MY_ERR("FlashMgr::getInstance().cameraPreviewEnd() fail\n");
        return E_3A_ERR;
    }
    err = FlashMgr::getInstance().stop(m_i4SensorDev);
    if (!err) {
        MY_ERR("FlashMgr::getInstance().stop() fail\n");
        return E_3A_ERR;
    }
#endif

#if CAM3_AF_FEATURE_EN
    // AF stop
    err = IAfMgr::getInstance().Stop(m_i4SensorDev);
    if (FAILED(err)) {
        MY_ERR("AfMgr::getInstance().Stop() fail\n");
        return err;
    }
    // disable AF thread
    m_pThreadRaw->disableAFThread();
#endif

#if CAM3_FLICKER_FEATURE_EN
    // Flicker close
    FlickerHalBase::getInstance().close(m_i4SensorDev);
#endif

    // NormalIOPipe destroy instance
    if (m_pCamIO != NULL)
    {
        m_pCamIO->destroyInstance(LOG_TAG);
        m_pCamIO = NULL;
    }

    MY_LOG("[%s] -", __FUNCTION__);
    return MTRUE;
}

MBOOL
Hal3ARawImp::
generateP2(MINT32 flowType, const NSIspTuning::RAWIspCamInfo& rCamInfo, void* pRegBuf)
{
    MY_LOG("[%s] + flow(%d), buf(%p)", __FUNCTION__, flowType, pRegBuf);
    mP2Mtx.lock();
    IspTuningMgr::getInstance().validatePerFrameP2(m_i4SensorDev, flowType, rCamInfo, pRegBuf);
    mP2Mtx.unlock();
    MY_LOG("[%s] -", __FUNCTION__);
    return MTRUE;
}

MBOOL
Hal3ARawImp::
validateP1(const ParamIspProfile_T& rParamIspProfile, MBOOL fgPerframe)
{
    MY_LOG("[%s]", __FUNCTION__);
    m_pTuning->setIspProfile(m_i4SensorDev, rParamIspProfile.eIspProfile);
    m_pTuning->notifyRPGEnable(m_i4SensorDev, rParamIspProfile.iEnableRPG);
    m_pTuning->validatePerFrame(m_i4SensorDev, rParamIspProfile.i4MagicNum, fgPerframe);
    return MTRUE;
}

MBOOL
Hal3ARawImp::
setParams(Param_T const &rNewParam)
{
    MY_LOG_IF(m_3ALogEnable, "[%s] +", __FUNCTION__);
    Mutex::Autolock autoLock(m3AOperMtx2);

    if (!m_bEnable3ASetParams) return MTRUE;

#if CAM3_LSC_FEATURE_EN
    // ====================================== Shading =============================================
    NSIspTuning::ILscMgr::getInstance(static_cast<ESensorDev_T>(m_i4SensorDev))->setOnOff(rNewParam.u1ShadingMode ? MTRUE : MFALSE);
#endif

    // ====================================== ISP tuning =============================================
    IspTuningMgr::getInstance().setSceneMode(m_i4SensorDev, rNewParam.u4SceneMode);
    IspTuningMgr::getInstance().setEffect(m_i4SensorDev, rNewParam.u4EffectMode);
    IspTuningMgr::getInstance().setIspUserIdx_Bright(m_i4SensorDev, rNewParam.u4BrightnessMode);
    IspTuningMgr::getInstance().setIspUserIdx_Hue(m_i4SensorDev, rNewParam.u4HueMode);
    IspTuningMgr::getInstance().setIspUserIdx_Sat(m_i4SensorDev, rNewParam.u4SaturationMode);
    IspTuningMgr::getInstance().setIspUserIdx_Edge(m_i4SensorDev, rNewParam.u4EdgeMode);
    IspTuningMgr::getInstance().setIspUserIdx_Contrast(m_i4SensorDev, rNewParam.u4ContrastMode);
    IspTuningMgr::getInstance().setEdgeMode(m_i4SensorDev, rNewParam.u1EdgeMode ? MTK_EDGE_MODE_FAST : MTK_EDGE_MODE_OFF);
    IspTuningMgr::getInstance().setNoiseReductionMode(m_i4SensorDev, rNewParam.u1NRMode);
    IspTuningMgr::getInstance().setToneMapMode(m_i4SensorDev, rNewParam.u1TonemapMode);
    if (rNewParam.u1TonemapMode == MTK_TONEMAP_MODE_CONTRAST_CURVE)
    {
        MINT32 i = 0;
        android::Vector<MFLOAT> vecIn, vecOut;
        MINT32 i4Cnt = rNewParam.vecTonemapCurveBlue.size() / 2;
        vecIn.resize(i4Cnt);
        vecOut.resize(i4Cnt);
        MFLOAT* pArrayIn = vecIn.editArray();
        MFLOAT* pArrayOut = vecOut.editArray();
        const MFLOAT* pCurve = rNewParam.vecTonemapCurveBlue.array();
        for (i = i4Cnt; i != 0; i--)
        {
            MFLOAT x, y;
            x = *pCurve++;
            y = *pCurve++;
            *pArrayIn++ = x;
            *pArrayOut++ = y;
            MY_LOG_IF(m_3ALogEnable, "[Blue]#%d(%f,%f)", rNewParam.u4MagicNum, x, y);
        }
        IspTuningMgr::getInstance().setTonemapCurve_Blue(m_i4SensorDev, vecIn.editArray(), vecOut.editArray(), &i4Cnt);

        i4Cnt = rNewParam.vecTonemapCurveGreen.size() / 2;
        vecIn.resize(i4Cnt);
        vecOut.resize(i4Cnt);
        pArrayIn = vecIn.editArray();
        pArrayOut = vecOut.editArray();
        pCurve = rNewParam.vecTonemapCurveGreen.array();
        for (i = i4Cnt; i != 0; i--)
        {
            MFLOAT x, y;
            x = *pCurve++;
            y = *pCurve++;
            *pArrayIn++ = x;
            *pArrayOut++ = y;
            MY_LOG_IF(m_3ALogEnable, "[Green]#%d(%f,%f)", rNewParam.u4MagicNum, x, y);
        }
        IspTuningMgr::getInstance().setTonemapCurve_Green(m_i4SensorDev, vecIn.editArray(), vecOut.editArray(), &i4Cnt);

        i4Cnt = rNewParam.vecTonemapCurveRed.size() / 2;
        vecIn.resize(i4Cnt);
        vecOut.resize(i4Cnt);
        pArrayIn = vecIn.editArray();
        pArrayOut = vecOut.editArray();
        pCurve = rNewParam.vecTonemapCurveRed.array();
        for (i = i4Cnt; i != 0; i--)
        {
            MFLOAT x, y;
            x = *pCurve++;
            y = *pCurve++;
            *pArrayIn++ = x;
            *pArrayOut++ = y;
            MY_LOG_IF(m_3ALogEnable, "[Red]#%d(%f,%f)", rNewParam.u4MagicNum, x, y);
        }
        IspTuningMgr::getInstance().setTonemapCurve_Red(m_i4SensorDev, vecIn.editArray(), vecOut.editArray(), &i4Cnt);
    }

    // ====================================== AE ==============================================
    IAeMgr::getInstance().setAEMinMaxFrameRate(m_i4SensorDev, rNewParam.i4MinFps, rNewParam.i4MaxFps);
    IAeMgr::getInstance().setAEMeteringMode(m_i4SensorDev, rNewParam.u4AeMeterMode);
    IAeMgr::getInstance().setAERotateDegree(m_i4SensorDev, rNewParam.i4RotateDegree);
    IAeMgr::getInstance().setAEISOSpeed(m_i4SensorDev, rNewParam.u4IsoSpeedMode);
    IAeMgr::getInstance().setAEMeteringArea(m_i4SensorDev, &rNewParam.rMeteringAreas);
    IAeMgr::getInstance().setAPAELock(m_i4SensorDev, rNewParam.bIsAELock);
    IAeMgr::getInstance().setAEEVCompIndex(m_i4SensorDev, rNewParam.i4ExpIndex, rNewParam.fExpCompStep);
    IAeMgr::getInstance().setAEMode(m_i4SensorDev, rNewParam.u4AeMode);
    IAeMgr::getInstance().setAEFlickerMode(m_i4SensorDev, rNewParam.u4AntiBandingMode);
    IAeMgr::getInstance().setAECamMode(m_i4SensorDev, rNewParam.u4CamMode);
    IAeMgr::getInstance().setAEShotMode(m_i4SensorDev, rNewParam.u4ShotMode);
    IAeMgr::getInstance().setSceneMode(m_i4SensorDev, rNewParam.u4SceneMode);
    IAeMgr::getInstance().bBlackLevelLock(m_i4SensorDev, rNewParam.u1BlackLvlLock);
    if (rNewParam.u4AeMode == MTK_CONTROL_AE_MODE_OFF)
    {
        AE_SENSOR_PARAM_T strSensorParams;
        strSensorParams.u4Sensitivity   = rNewParam.i4Sensitivity;
        strSensorParams.u8ExposureTime  = rNewParam.i8ExposureTime;
        strSensorParams.u8FrameDuration = rNewParam.i8FrameDuration;
        IAeMgr::getInstance().UpdateSensorParams(m_i4SensorDev, strSensorParams);
    }
    MY_LOG_IF(m_3ALogEnable, "[%s] setAEMode(%d)", __FUNCTION__, rNewParam.u4AeMode);

    // ====================================== AWB ==============================================
    IAwbMgr::getInstance().setAWBLock(m_i4SensorDev, rNewParam.bIsAWBLock);
    IAwbMgr::getInstance().setAWBMode(m_i4SensorDev, rNewParam.u4AwbMode);
    IAwbMgr::getInstance().setColorCorrectionMode(m_i4SensorDev, rNewParam.u1ColorCorrectMode);
    IspTuningMgr::getInstance().setColorCorrectionMode(m_i4SensorDev, rNewParam.u1ColorCorrectMode);
    if (rNewParam.u4AwbMode == MTK_CONTROL_AWB_MODE_OFF &&
        rNewParam.u1ColorCorrectMode == MTK_COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
    {
        IAwbMgr::getInstance().setColorCorrectionGain(m_i4SensorDev, rNewParam.fColorCorrectGain[0], rNewParam.fColorCorrectGain[1], rNewParam.fColorCorrectGain[3]);
        IspTuningMgr::getInstance().setColorCorrectionTransform(m_i4SensorDev,
            rNewParam.fColorCorrectMat[0], rNewParam.fColorCorrectMat[1], rNewParam.fColorCorrectMat[2],
            rNewParam.fColorCorrectMat[3], rNewParam.fColorCorrectMat[4], rNewParam.fColorCorrectMat[5],
            rNewParam.fColorCorrectMat[6], rNewParam.fColorCorrectMat[7], rNewParam.fColorCorrectMat[8]);
    }

#if CAM3_AF_FEATURE_EN
    // ====================================== AF ==============================================
    IAfMgr::getInstance().setAFMode(m_i4SensorDev, rNewParam.u4AfMode);
    if (m_bFaceDetectEnable == MFALSE)
        IAfMgr::getInstance().setAFArea(m_i4SensorDev, rNewParam.rFocusAreas);
    IAfMgr::getInstance().setFullScanstep(m_i4SensorDev, rNewParam.i4FullScanStep);
    IAfMgr::getInstance().setMFPos(m_i4SensorDev, rNewParam.i4MFPos);
    IAfMgr::getInstance().setAndroidServiceState(m_i4SensorDev, rNewParam.bIsSupportAndroidService);
    if (rNewParam.u4AfMode == MTK_CONTROL_AF_MODE_OFF ||
        rNewParam.u4AfMode == MTK_CONTROL_AF_MODE_CONTINUOUS_PICTURE||
        rNewParam.u4AfMode == MTK_CONTROL_AF_MODE_CONTINUOUS_VIDEO)
    {
        EState_T eCurrState = m_pAfStateMgr->getStateStatus().eCurrState;
        if(eCurrState == eState_TAF)
            m_pAfStateMgr->transitState(eCurrState,eState_CAF);
    } else
    {
        EState_T eCurrState = m_pAfStateMgr->getStateStatus().eCurrState;
        if(eCurrState == eState_CAF)
            m_pAfStateMgr->transitState(eCurrState,eState_TAF);
    }
    if (rNewParam.u4AfMode == MTK_CONTROL_AF_MODE_OFF)
    {
        // focus dist
        IAfMgr::getInstance().setFocusDistance(m_i4SensorDev, rNewParam.fFocusDistance);
    }
#endif

#if CAM3_FLASH_FEATURE_EN
    // ====================================== Flash ==============================================
    FlashMgr::getInstance().setAeFlashMode(m_i4SensorDev, rNewParam.u4AeMode, rNewParam.u4StrobeMode);
    int bMulti;
    if(rNewParam.u4CapType == ECapType_MultiCapture)
        bMulti=1;
    else
        bMulti=0;
    FlashMgr::getInstance().setCamMode(m_i4SensorDev, rNewParam.u4CamMode);
    FlashMgr::getInstance().setEvComp(m_i4SensorDev, rNewParam.i4ExpIndex, rNewParam.fExpCompStep);
#endif

#if CAM3_FLICKER_FEATURE_EN
    // ====================================== Flicker ==============================================
    FlickerHalBase::getInstance().setFlickerMode(m_i4SensorDev, rNewParam.u4AntiBandingMode);
#endif

    // ====================================== FlowCtrl ==============================================
    m_rParam = rNewParam;

    MY_LOG_IF(m_3ALogEnable, "[%s] m_rParam.u1ShadingMapMode(%d)", __FUNCTION__, m_rParam.u1ShadingMapMode);

    MY_LOG_IF(m_3ALogEnable, "[%s] -", __FUNCTION__);
    return MTRUE;
}

MBOOL
Hal3ARawImp::
autoFocus()
{
#if CAM3_AF_FEATURE_EN
    MY_LOG("[%s()] + \n", __FUNCTION__);
    m_pAfStateMgr->sendCmd(ECmd_AFStart);

    if ((m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_PICTURE) && (m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_VIDEO))   {
        m_pStateMgr->sendCmd(ECmd_AFStart);
    }
    MY_LOG("[%s()] - \n", __FUNCTION__);
#endif
    return MTRUE;
}

MBOOL
Hal3ARawImp::
cancelAutoFocus()
{
#if CAM3_AF_FEATURE_EN
    MY_LOG("[%s()] + \n", __FUNCTION__);
    m_pAfStateMgr->sendCmd(ECmd_AFEnd);

    if ((m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_PICTURE) && (m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_VIDEO))   {
        m_pStateMgr->sendCmd(ECmd_AFEnd);
    }
    MY_LOG("[%s()] - \n", __FUNCTION__);
#endif
    return MTRUE;
}

MVOID
Hal3ARawImp::
setFDEnable(MBOOL fgEnable)
{
    MY_LOG_IF(m_3ALogEnable, "[%s] fgEnable(%d)", __FUNCTION__, fgEnable);
    m_bFaceDetectEnable = fgEnable;
}

MBOOL
Hal3ARawImp::
setFDInfo(MVOID* prFaces)
{
    MY_LOG_IF(m_3ALogEnable, "[%s] m_bFaceDetectEnable(%d)", __FUNCTION__, m_bFaceDetectEnable);
    if (m_bFaceDetectEnable)
    {
#if CAM3_AF_FEATURE_EN
        IAfMgr::getInstance().setFDInfo(m_i4SensorDev, prFaces);
#endif
        IAeMgr::getInstance().setFDInfo(m_i4SensorDev, prFaces);
    }
    return MTRUE;
}

MBOOL
Hal3ARawImp::
setFlashLightOnOff(MBOOL bOnOff, MBOOL bMainPre)
{
#if CAM3_FLASH_FEATURE_EN
    MY_LOG("[%s] bOnOff(%d) + ", __FUNCTION__, bOnOff);
    if (!bOnOff)
    {
        if (m_i4InCaptureProcess)
        {
            //modified to update strobe state after capture for ae/flash manager
            FlashMgr::getInstance().cameraPreviewStart(m_i4SensorDev);
            IAeMgr::getInstance().setStrobeMode(m_i4SensorDev, MFALSE);
            m_i4InCaptureProcess = 0;
        }
        FlashMgr::getInstance().turnOffFlashDevice(m_i4SensorDev);
    }
    else //flash on
    {
        if (bMainPre) FlashMgr::getInstance().setCaptureFlashOnOff(m_i4SensorDev, 1);
        else FlashMgr::getInstance().setTorchOnOff(m_i4SensorDev, 1);
    }
    MY_LOG("[%s] - ", __FUNCTION__);
#endif
    return MTRUE;
}

MBOOL
Hal3ARawImp::
chkMainFlashOnCond() const
{
    MY_LOG("[%s]", __FUNCTION__);
    return
        (((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_OFF)||(m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON)) && (m_rParam.u4StrobeMode == MTK_FLASH_MODE_SINGLE))
        ||
        ( ((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON_ALWAYS_FLASH)||((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON_AUTO_FLASH) && isStrobeBVTrigger()))
           && (m_rParam.u1CaptureIntent == MTK_CONTROL_CAPTURE_INTENT_STILL_CAPTURE) );
}

MBOOL
Hal3ARawImp::
chkPreFlashOnCond() const
{
    MY_LOG("[%s]", __FUNCTION__);
    return
        (((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_OFF)||(m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON)) && (m_rParam.u4StrobeMode == MTK_FLASH_MODE_TORCH));
}

MBOOL
Hal3ARawImp::
isStrobeBVTrigger() const
{
    return IAeMgr::getInstance().IsStrobeBVTrigger(m_i4SensorDev);
}

MINT32
Hal3ARawImp::
getCurrResult(MUINT32 i4FrmId, Result_T& rResult) const
{
    MY_LOG_IF(m_3ALogEnable, "[%s] + i4MagicNum(%d)", __FUNCTION__, i4FrmId);
    mtk_camera_metadata_enum_android_control_awb_state_t eAwbState;
    rResult.i4FrmId = i4FrmId;

    // clear memory
    rResult.vecShadingMap.clear();
    rResult.vecExifInfo.clear();
    rResult.vecTonemapCurveRed.clear();
    rResult.vecTonemapCurveGreen.clear();
    rResult.vecTonemapCurveBlue.clear();
    rResult.vecColorCorrectMat.clear();

    rResult.u1SceneMode = m_rParam.u4SceneMode;

    // AE
    MUINT8 u1AeState = IAeMgr::getInstance().getAEState(m_i4SensorDev);
    if (IAeMgr::getInstance().IsStrobeBVTrigger(m_i4SensorDev) && u1AeState == MTK_CONTROL_AE_STATE_CONVERGED)
        rResult.u1AeState = MTK_CONTROL_AE_STATE_FLASH_REQUIRED;
    else
        rResult.u1AeState = u1AeState;

    AE_SENSOR_PARAM_T rAESensorInfo;
    IAeMgr::getInstance().getSensorParams(m_i4SensorDev, rAESensorInfo);
    rResult.i8SensorExposureTime = rAESensorInfo.u8ExposureTime;
    rResult.i8SensorFrameDuration = rAESensorInfo.u8FrameDuration;
    rResult.i4SensorSensitivity = rAESensorInfo.u4Sensitivity;
    if (m_rParam.u1RollingShutterSkew)
        rResult.i8SensorRollingShutterSkew = IAeMgr::getInstance().getSensorRollingShutter(m_i4SensorDev);

    // AWB
    IAwbMgr::getInstance().getAWBState(m_i4SensorDev, eAwbState);
    rResult.u1AwbState= eAwbState;
    AWB_GAIN_T rAwbGain;
    IAwbMgr::getInstance().getAWBGain(m_i4SensorDev, rAwbGain, rResult.i4AwbGainScaleUint);
    rResult.i4AwbGain[0] = rAwbGain.i4R;
    rResult.i4AwbGain[1] = rAwbGain.i4G;
    rResult.i4AwbGain[2] = rAwbGain.i4B;
    IAwbMgr::getInstance().getColorCorrectionGain(m_i4SensorDev, rResult.fColorCorrectGain[0],rResult.fColorCorrectGain[1],rResult.fColorCorrectGain[3]);
    rResult.fColorCorrectGain[2] = rResult.fColorCorrectGain[1];
    if (m_rParam.u1ColorCorrectMode != MTK_COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
    {
        rResult.vecColorCorrectMat.resize(9);
        MFLOAT* pfColorCorrectMat = rResult.vecColorCorrectMat.editArray();
    IspTuningMgr::getInstance().getColorCorrectionTransform(m_i4SensorDev,
            pfColorCorrectMat[0], pfColorCorrectMat[1], pfColorCorrectMat[2],
            pfColorCorrectMat[3], pfColorCorrectMat[4], pfColorCorrectMat[5],
            pfColorCorrectMat[6], pfColorCorrectMat[7], pfColorCorrectMat[8]
    );
    }

#if CAM3_AF_FEATURE_EN
    // AF
    rResult.u1AfState = IAfMgr::getInstance().getAFState(m_i4SensorDev);
    if (m_pStateMgr->getStateStatus().eCurrState == eState_AF)
        rResult.u1AfState = MTK_CONTROL_AF_STATE_ACTIVE_SCAN;
    rResult.u1LensState = IAfMgr::getInstance().getLensState(m_i4SensorDev);
    rResult.fLensFocusDistance = IAfMgr::getInstance().getFocusDistance(m_i4SensorDev);
    IAfMgr::getInstance().getFocusRange(m_i4SensorDev, &rResult.fLensFocusRange[0], &rResult.fLensFocusRange[1]);
#endif

#if CAM3_FLASH_FEATURE_EN
    // Flash
    //rResult.u1FlashState = FlashMgr::getInstance()->getFlashState();
#if 1 //mark out temporarily
    rResult.u1FlashState =
        (FlashMgr::getInstance().getFlashState(m_i4SensorDev) == MTK_FLASH_STATE_UNAVAILABLE) ?
        MTK_FLASH_STATE_UNAVAILABLE :
        (FlashMgr::getInstance().isAFLampOn(m_i4SensorDev) ? MTK_FLASH_STATE_FIRED : MTK_FLASH_STATE_READY);
#endif
#endif

#if CAM3_FLICKER_FEATURE_EN
    // Flicker
    MINT32 i4FlkStatus = IAeMgr::getInstance().getAEAutoFlickerState(m_i4SensorDev);
    //FlickerHalBase::getInstance().getFlickerResult(i4FlkStatus);
    MUINT8 u1ScnFlk = MTK_STATISTICS_SCENE_FLICKER_NONE;
    if (i4FlkStatus == 0) u1ScnFlk = MTK_STATISTICS_SCENE_FLICKER_50HZ;
    if (i4FlkStatus == 1) u1ScnFlk = MTK_STATISTICS_SCENE_FLICKER_60HZ;
    rResult.u1SceneFlk = (MUINT8)u1ScnFlk;
#endif

#if CAM3_LSC_FEATURE_EN
    // Shading map
    if (m_rParam.u1ShadingMapMode == MTK_STATISTICS_LENS_SHADING_MAP_MODE_ON)
    {
        rResult.vecShadingMap.resize(m_rParam.u1ShadingMapXGrid*m_rParam.u1ShadingMapYGrid*4);
        MFLOAT* pfShadingMap = rResult.vecShadingMap.editArray();
        NSIspTuning::ILscMgr::getInstance(static_cast<ESensorDev_T>(m_i4SensorDev))->getGainTable(3, m_rParam.u1ShadingMapXGrid, m_rParam.u1ShadingMapYGrid, pfShadingMap);
    }
#endif

    // Tonemap
    if (m_rParam.u1TonemapMode != MTK_TONEMAP_MODE_CONTRAST_CURVE)
    {
        MINT32 i = 0;
        MFLOAT *pIn, *pOut;
        MINT32 i4NumPt;
        IspTuningMgr::getInstance().getTonemapCurve_Blue(m_i4SensorDev, pIn, pOut, &i4NumPt);
        for (i = 0; i < i4NumPt; i++)
        {
            rResult.vecTonemapCurveBlue.push_back(*pIn++);
            rResult.vecTonemapCurveBlue.push_back(*pOut++);
        }
        IspTuningMgr::getInstance().getTonemapCurve_Green(m_i4SensorDev, pIn, pOut, &i4NumPt);
        for (i = 0; i < i4NumPt; i++)
        {
            rResult.vecTonemapCurveGreen.push_back(*pIn++);
            rResult.vecTonemapCurveGreen.push_back(*pOut++);
        }
        IspTuningMgr::getInstance().getTonemapCurve_Red(m_i4SensorDev, pIn, pOut, &i4NumPt);
        for (i = 0; i < i4NumPt; i++)
        {
            rResult.vecTonemapCurveRed.push_back(*pIn++);
            rResult.vecTonemapCurveRed.push_back(*pOut++);
        }
    }

    // Cam Info
    RAWIspCamInfo rCamInfo;
    if (!IspTuningMgr::getInstance().getCamInfo(m_i4SensorDev, rCamInfo))
    {
        MY_ERR("Fail to get CamInfo");
    }
    UtilConvertCamInfo(rCamInfo, rResult.rCamInfo);

    // Exif
    if (m_rParam.u1IsGetExif)
    {
        rResult.vecExifInfo.resize(1);
        get3AEXIFInfo(rResult.vecExifInfo.editTop());
    }

    MY_LOG_IF(m_3ALogEnable, "[%s] - i4MagicNum(%d)", __FUNCTION__, i4FrmId);
    return 0;
}

MINT32
Hal3ARawImp::
getCurrentHwId() const
{
    MINT32 idx = 0;
    m_pCamIO->sendCommand(NSCam::NSIoPipe::NSCamIOPipe::ENPipeCmd_GET_CUR_SOF_IDX,
                        (MINTPTR)&idx, 0, 0);
    MY_LOG("[%s] idx(%d)", __FUNCTION__, idx);
    return idx;
}

MBOOL
Hal3ARawImp::
doStart()
{
    MY_LOG("[%s]+ sensorDev(%d), Mode(%d)", __FUNCTION__, m_i4SensorDev, m_u4SensorMode);

    MUINT32 u4AAWidth, u4AAHight;
    // query input size info for AAO and FLKO
    m_pCamIO->sendCommand(NSCam::NSIoPipe::NSCamIOPipe::ENPipeCmd_GET_HBIN_INFO,
                        (MINTPTR)&u4AAWidth, (MINTPTR)&u4AAHight, 0);
    MY_LOG("[%s] u4AAWidth(%d), u4AAHight(%d)", __FUNCTION__, u4AAWidth, u4AAHight);

    MUINT32 u4AFWidth, u4AFHeight;
    // query input size info for AFO
    m_pCamIO->sendCommand(NSCam::NSIoPipe::NSCamIOPipe::ENPipeCmd_GET_BIN_INFO,
                        (MINTPTR)&u4AFWidth, (MINTPTR)&u4AFHeight, 0);
    MY_LOG("[%s] u4AFWidth(%d), u4AFHeight(%d)", __FUNCTION__, u4AFWidth, u4AFHeight);

    // TODO: update HBIN and BIN info to AE/AWB/AF/FLICKER

    // set sensor mode to 3A modules
    IAeMgr::getInstance().setSensorMode(m_i4SensorDev, m_u4SensorMode, u4AAWidth, u4AAHight);
    IAwbMgr::getInstance().setSensorMode(m_i4SensorDev, m_u4SensorMode,  u4AAWidth, u4AFHeight);
    IAEBufMgr::getInstance().setSensorMode(m_i4SensorDev, m_u4SensorMode);
    NSIspTuning::ILscMgr::getInstance(static_cast<ESensorDev_T>(m_i4SensorDev))->setSensorMode(
        static_cast<ESensorMode_T>(m_u4SensorMode), u4AFWidth, u4AFHeight, MFALSE);

#if CAM3_AF_FEATURE_EN
    IAfMgr::getInstance().setSensorMode(m_i4SensorDev, m_u4SensorMode, u4AFWidth, u4AFWidth);
#endif

#if CAM3_FLICKER_FEATURE_EN
    FlickerHalBase::getInstance().setSensorMode(m_i4SensorDev, m_u4SensorMode, u4AAWidth, u4AAHight);
#endif

    m_pTuning->setSensorMode(m_i4SensorDev, m_u4SensorMode);
    m_pTuning->setIspProfile(m_i4SensorDev, NSIspTuning::EIspProfile_Preview);
    //m_pTuning->validate(m_i4SensorDev, 0, MTRUE);
    MY_LOG("[%s]-", __FUNCTION__);

    return MTRUE;
}

MVOID
Hal3ARawImp::
setSensorMode(MINT32 i4SensorMode)
{
    MY_LOG("[%s] mode(%d)", __FUNCTION__, i4SensorMode);
    m_u4SensorMode = i4SensorMode;
}

MBOOL
Hal3ARawImp::
postCommand(ECmd_T const r3ACmd, const ParamIspProfile_T* pParam)
{
    MY_LOG("[%s]+", __FUNCTION__);
#if CAM3_AF_FEATURE_EN
    // for AF state
    switch(r3ACmd)
    {
        case ECmd_Init:
        case ECmd_Uninit:
        case ECmd_CameraPreviewStart:
        case ECmd_CameraPreviewEnd:
            m_pAfStateMgr->sendCmd(r3ACmd);
            break;
    }
#endif

    // for 3A state
    if ((m_rParam.u1CaptureIntent == MTK_CONTROL_CAPTURE_INTENT_STILL_CAPTURE) && (r3ACmd == ECmd_Update) && (m_rParam.u4AeMode != MTK_CONTROL_AE_MODE_OFF))
    {
        if (m_pStateMgr->getStateStatus().eCurrState == eState_Precapture)
        {
            MY_LOG("Unexpected Operation since precapture is not finished.");
            m_pStateMgr->sendCmd(ECmd_PrecaptureEnd);
        }
#if CAM3_FLASH_FEATURE_EN
        if(!chkMainFlashOnCond())
#endif
        {
            AE_MODE_CFG_T previewInfo;
            IAeMgr::getInstance().getPreviewParams(m_i4SensorDev, previewInfo);
            IAeMgr::getInstance().updateCaptureParams(m_i4SensorDev, previewInfo);
        }

        {
            m_i4InCaptureProcess = 1;
            m_pStateMgr->sendCmd(ECmd_CaptureStart);
        }
    }
    else m_pStateMgr->sendCmd(r3ACmd);

    if (ECmd_Update == r3ACmd)
    {
        if (m_pStateMgr->getFrameCount() >= 0)
        {
#if CAM3_FLASH_FEATURE_EN
            FlashMgr::getInstance().doPreviewOneFrame(m_i4SensorDev);
#endif
            validateP1(*pParam, MTRUE);

        }
    }
    MY_LOG("[%s]-", __FUNCTION__);
    return MTRUE;
}

MRESULT
Hal3ARawImp::
updateTGInfo()
{
    //Before wait for VSirq of IspDrv, we need to query IHalsensor for the current TG info
    IHalSensorList*const pHalSensorList = IHalSensorList::get();
    if (!pHalSensorList)
    {
        MY_ERR("IHalSensorList::get() == NULL");
        return E_3A_ERR;
    }
    const char* const callerName = "Hal3AQueryTG";
    IHalSensor* pHalSensor = pHalSensorList->createSensor(callerName, m_i4SensorIdx);
    //Note that Middleware has configured sensor before
    SensorDynamicInfo senInfo;
    MINT32 i4SensorDevId = pHalSensor->querySensorDynamicInfo(m_i4SensorDev, &senInfo);
    pHalSensor->destroyInstance(callerName);

    MY_LOG("m_i4SensorDev = %d, senInfo.TgInfo = %d\n", m_i4SensorDev, senInfo.TgInfo);

    if ((senInfo.TgInfo != CAM_TG_1) && (senInfo.TgInfo != CAM_TG_2))
    {
        MY_ERR("RAW sensor is connected with TgInfo: %d\n", senInfo.TgInfo);
        return E_3A_ERR;
    }

    m_u4TgInfo = senInfo.TgInfo; //now, TG info is obtained! TG1 or TG2

    IAwbMgr::getInstance().setTGInfo(m_i4SensorDev, m_u4TgInfo);
    IAEBufMgr::getInstance().setTGInfo(m_i4SensorDev, m_u4TgInfo);
    IspTuningMgr::getInstance().setTGInfo(m_i4SensorDev, m_u4TgInfo);

    return S_3A_OK;
}

MBOOL
Hal3ARawImp::
get3AEXIFInfo(EXIF_3A_INFO_T& rExifInfo) const
{
    AE_DEVICES_INFO_T rDeviceInfo;
    AE_SENSOR_PARAM_T rAESensorInfo;

    IAeMgr::getInstance().getSensorParams(m_i4SensorDev, rAESensorInfo);
    rExifInfo.u4CapExposureTime = rAESensorInfo.u8ExposureTime / 1000;  // naro sec change to micro sec
#if CAM3_FLASH_FEATURE_EN
    if (FlashMgr::getInstance().isAFLampOn(m_i4SensorDev))
        rExifInfo.u4FlashLightTimeus = 30000;
    else
#endif
        rExifInfo.u4FlashLightTimeus = 0;

    IAeMgr::getInstance().getSensorDeviceInfo(m_i4SensorDev, rDeviceInfo);
    rExifInfo.u4FNumber     = rDeviceInfo.u4LensFno; // Format: F2.8 = 28
    rExifInfo.u4FocalLength = rDeviceInfo.u4FocusLength_100x; // Format: FL 3.5 = 350
    //rExifInfo.u4SceneMode   = m_rParam.u4SceneMode; // Scene mode   (SCENE_MODE_XXX)
    switch (IAeMgr::getInstance().getAEMeterMode(m_i4SensorDev))
    {
    case LIB3A_AE_METERING_MODE_AVERAGE:
        rExifInfo.u4AEMeterMode = eMeteringMode_Average;
        break;
    case LIB3A_AE_METERING_MODE_CENTER_WEIGHT:
        rExifInfo.u4AEMeterMode = eMeteringMode_Center;
        break;
    case LIB3A_AE_METERING_MODE_SOPT:
        rExifInfo.u4AEMeterMode = eMeteringMode_Spot;
        break;
    default:
        rExifInfo.u4AEMeterMode = eMeteringMode_Other;
        break;
    }
    rExifInfo.i4AEExpBias   = IAeMgr::getInstance().getEVCompensateIndex(m_i4SensorDev); // Exposure index  (AE_EV_COMP_XX)
    rExifInfo.u4AEISOSpeed  = rAESensorInfo.u4Sensitivity;

    rExifInfo.u4AWBMode     = (m_rParam.u4AwbMode == MTK_CONTROL_AWB_MODE_AUTO) ? 0 : 1;
    switch (m_rParam.u4AwbMode)
    {
    case MTK_CONTROL_AWB_MODE_AUTO:
    case MTK_CONTROL_AWB_MODE_WARM_FLUORESCENT:
    case MTK_CONTROL_AWB_MODE_TWILIGHT:
    case MTK_CONTROL_AWB_MODE_INCANDESCENT:
        rExifInfo.u4LightSource = eLightSourceId_Other;
        break;
    case MTK_CONTROL_AWB_MODE_DAYLIGHT:
        rExifInfo.u4LightSource = eLightSourceId_Daylight;
        break;
    case MTK_CONTROL_AWB_MODE_FLUORESCENT:
        rExifInfo.u4LightSource = eLightSourceId_Fluorescent;
        break;
#if 0
    case MTK_CONTROL_AWB_MODE_TUNGSTEN:
        rExifInfo.u4LightSource = eLightSourceId_Tungsten;
        break;
#endif
    case MTK_CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
        rExifInfo.u4LightSource = eLightSourceId_Cloudy;
        break;
    case MTK_CONTROL_AWB_MODE_SHADE:
        rExifInfo.u4LightSource = eLightSourceId_Shade;
        break;
    default:
        rExifInfo.u4LightSource = eLightSourceId_Other;
        break;
    }

    switch (m_rParam.u4SceneMode)
    {
    case MTK_CONTROL_SCENE_MODE_PORTRAIT:
        rExifInfo.u4ExpProgram = eExpProgramId_Portrait;
        break;
    case MTK_CONTROL_SCENE_MODE_LANDSCAPE:
        rExifInfo.u4ExpProgram = eExpProgramId_Landscape;
        break;
    default:
        rExifInfo.u4ExpProgram = eExpProgramId_NotDefined;
        break;
    }

    switch (m_rParam.u4SceneMode)
    {
    case MTK_CONTROL_SCENE_MODE_DISABLED:
    case MTK_CONTROL_SCENE_MODE_NORMAL:
    case MTK_CONTROL_SCENE_MODE_NIGHT_PORTRAIT:
    case MTK_CONTROL_SCENE_MODE_THEATRE:
    case MTK_CONTROL_SCENE_MODE_BEACH:
    case MTK_CONTROL_SCENE_MODE_SNOW:
    case MTK_CONTROL_SCENE_MODE_SUNSET:
    case MTK_CONTROL_SCENE_MODE_STEADYPHOTO:
    case MTK_CONTROL_SCENE_MODE_FIREWORKS:
    case MTK_CONTROL_SCENE_MODE_SPORTS:
    case MTK_CONTROL_SCENE_MODE_PARTY:
    case MTK_CONTROL_SCENE_MODE_CANDLELIGHT:
        rExifInfo.u4SceneCapType = eCapTypeId_Standard;
        break;
    case MTK_CONTROL_SCENE_MODE_PORTRAIT:
        rExifInfo.u4SceneCapType = eCapTypeId_Portrait;
        break;
    case MTK_CONTROL_SCENE_MODE_LANDSCAPE:
        rExifInfo.u4SceneCapType = eCapTypeId_Landscape;
        break;
    case MTK_CONTROL_SCENE_MODE_NIGHT:
        rExifInfo.u4SceneCapType = eCapTypeId_Night;
        break;
    default:
        rExifInfo.u4SceneCapType = eCapTypeId_Standard;
        break;
    }

    return MTRUE;
}



