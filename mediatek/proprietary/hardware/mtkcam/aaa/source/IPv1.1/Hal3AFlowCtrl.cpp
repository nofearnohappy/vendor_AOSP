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
#define LOG_TAG "Hal3AFlowCtrl"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <Hal3AFlowCtrl.h>
#include <cutils/properties.h>
#include <aaa_log.h>

#include <IHalSensor.h>
#ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly - USING_FAKE_SENSOR
#include <drv/src/isp/mt6797/iopipe/CamIO/FakeSensor.h>
#endif

using namespace NS3Av3;
using namespace android;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
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

template <MINT32 sensorDev>
class Hal3AFlowCtrlDev : public Hal3AFlowCtrl
{
public:
    static Hal3AFlowCtrlDev* getInstance()
    {
        static Hal3AFlowCtrlDev<sensorDev> singleton;
        return &singleton;
    }
    Hal3AFlowCtrlDev()
        : Hal3AFlowCtrl()
    {
    }

private:

};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// instancing (no user count protection, only referenced by Hal3AAdpater which
// controls the life cycle from init to uninit)
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
Hal3AIf*
Hal3AFlowCtrl::
createInstance(MINT32 i4SensorOpenIndex)
{
#ifdef USING_MTK_LDVT
    IHalSensorList*const pHalSensorList = TS_FakeSensorList::getTestModel();
#else
    IHalSensorList*const pHalSensorList = IHalSensorList::get();
#endif
    if (!pHalSensorList) return NULL;

    MINT32 i4SensorDev = pHalSensorList->querySensorDevIdx(i4SensorOpenIndex);

    switch (i4SensorDev)
    {
    case SENSOR_DEV_MAIN:
        Hal3AFlowCtrlDev<SENSOR_DEV_MAIN>::getInstance()->init(i4SensorDev, i4SensorOpenIndex);
        return Hal3AFlowCtrlDev<SENSOR_DEV_MAIN>::getInstance();
    break;
    case SENSOR_DEV_SUB:
        Hal3AFlowCtrlDev<SENSOR_DEV_SUB>::getInstance()->init(i4SensorDev, i4SensorOpenIndex);
        return Hal3AFlowCtrlDev<SENSOR_DEV_SUB>::getInstance();
    break;
    case SENSOR_DEV_MAIN_2:
        Hal3AFlowCtrlDev<SENSOR_DEV_MAIN_2>::getInstance()->init(i4SensorDev, i4SensorOpenIndex);
        return Hal3AFlowCtrlDev<SENSOR_DEV_MAIN_2>::getInstance();
    break;
    default:
        MY_ERR("Unsupport sensor device ID: %d\n", i4SensorDev);
        AEE_ASSERT_3A_HAL("Unsupport sensor device.");
        return MNULL;
    }
}

MVOID
Hal3AFlowCtrl::
destroyInstance()
{
    uninit();
}

Hal3AFlowCtrl::
Hal3AFlowCtrl()
    : Hal3AIf()
    , m_fgLogEn(MFALSE)
    , m_i4SensorDev(0)
    , m_i4SensorOpenIdx(0)
    , m_u4FrmIdStat(0)
    , m_u4FlashOnIdx(-1)
    , m_p3AWrap(NULL)
    , m_pThread(NULL)
    , m_pEventIrq(NULL)
    , m_p3ASttCtrl(NULL)
    , m_pCbSet(NULL)
{}

Hal3AFlowCtrl::
~Hal3AFlowCtrl()
{}

MRESULT
Hal3AFlowCtrl::
init(MINT32 i4SensorDev, MINT32 i4SensorOpenIndex) //can be called only once by RAW or YUV, no user count
{
    MY_LOG("[%s] +", __FUNCTION__);

    char cLogLevel[PROPERTY_VALUE_MAX];
    char value[PROPERTY_VALUE_MAX];
    ::property_get("debug.camera.log", cLogLevel, "0");
    m_fgLogEn = atoi(cLogLevel);
    if ( m_fgLogEn == 0 ) {
        ::property_get("debug.camera.log.hal3a", cLogLevel, "0");
        m_fgLogEn = atoi(cLogLevel);
    }

    MRESULT ret = S_3A_OK;
    MBOOL bRet = MTRUE;

#ifdef USING_MTK_LDVT
    IHalSensorList*const pHalSensorList = TS_FakeSensorList::getTestModel();
#else
    IHalSensorList*const pHalSensorList = IHalSensorList::get();
#endif
    if (!pHalSensorList) return NULL;

#if (CAM3_3ATESTLVL <= CAM3_3AUT)
    I3AWrapper::E_TYPE_T eType = I3AWrapper::E_TYPE_DFT;
#else
    MINT32 eSensorType = pHalSensorList->queryType(i4SensorOpenIndex);
    I3AWrapper::E_TYPE_T eType = I3AWrapper::E_TYPE_RAW;
    switch (eSensorType)
    {
    case NSSensorType::eRAW:
        eType = I3AWrapper::E_TYPE_RAW;
        break;
    default:
        eType = I3AWrapper::E_TYPE_DFT;
        break;
    }
#endif

    m_i4SensorDev = i4SensorDev;
    m_i4SensorOpenIdx = i4SensorOpenIndex;

    MY_LOG("[%s] eType(%d), sensor(%d), sensorIdx(%d)", __FUNCTION__, eType, i4SensorDev, i4SensorOpenIndex);

    // create 3A wrapper
    if (m_p3AWrap == NULL)
    {
        m_p3AWrap = I3AWrapper::createInstance(eType, i4SensorOpenIndex);
        if (m_p3AWrap)
        {
            MY_LOG("[%s] m_p3AWrapper(%p) created OK", __FUNCTION__, m_p3AWrap);
        }
        else
        {
            MY_ERR("m_p3AWrapper created fail!");
            AEE_ASSERT_3A_HAL("m_p3AWrapper created fail!");
        }
    }

    // create Vsync event
    //IEventIrq::ConfigParam IrqConfig(i4SensorDev, i4SensorOpenIndex, 5000, IEventIrq::E_Event_Vsync);
    //m_pEventIrq = IEventIrq::createInstance(IrqConfig, "VSIrq");

    // create statistic control
#if (CAM3_3ATESTLVL >= CAM3_3ASTTUT)
    if (m_p3ASttCtrl == NULL)
    {
        m_p3ASttCtrl = Hal3ASttCtrl::createInstance(i4SensorDev, i4SensorOpenIndex);
        if (m_p3ASttCtrl)
        {
            MY_LOG("[%s] m_p3ASttCtrl(%p) created OK", __FUNCTION__, m_p3ASttCtrl);
        }
        else
        {
            MY_ERR("m_p3ASttCtrl created fail!");
            AEE_ASSERT_3A_HAL("m_p3ASttCtrl created fail!");
        }
    }
#endif

    // create AA thread
    if (m_pThread == NULL)
    {
        m_pThread = IThread3A::createInstance(this);
        if (m_pThread)
        {
            MY_LOG("[%s] m_pThread(%p) created OK", __FUNCTION__, m_pThread);
        }
        else
        {
            MY_ERR("m_pThread created fail!");
            AEE_ASSERT_3A_HAL("m_pThread created fail!");
        }
    }

    MY_LOG("[%s] -", __FUNCTION__);
    return S_3A_OK;
}

MRESULT
Hal3AFlowCtrl::
uninit() //can be called only once by RAW or YUV, no user count
{
    MY_LOG("[%s] +", __FUNCTION__);

    MRESULT ret = S_3A_OK;
    MBOOL bRet = MTRUE;

    if (m_pThread)
    {
        m_pThread->destroyInstance();
        m_pThread = NULL;
    }

#if (CAM3_3ATESTLVL >= CAM3_3ASTTUT)
    if (m_p3ASttCtrl)
    {
        m_p3ASttCtrl->destroyInstance();
        m_p3ASttCtrl = NULL;
    }
#endif

    if (m_p3AWrap)
    {
        m_p3AWrap->destroyInstance();
        m_p3AWrap = NULL;
    }

    if (m_pCbSet)
    {
        m_pCbSet = NULL;
        MY_ERR("User did not detach callbacks!");
    }

    m_rResultBuf.clearAll();
    m_rResultBufCur.clearAll();

    MY_LOG("[%s] -", __FUNCTION__);
    return S_3A_OK;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// request/result & callback flows
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
Hal3AFlowCtrl::
sendCommand(ECmd_T const eCmd, MUINTPTR const i4Arg)
{
    return m_pThread->sendCommand(eCmd, i4Arg);
}

MBOOL
Hal3AFlowCtrl::
doUpdateCmd(const ParamIspProfile_T* pParam)
{
    IEventIrq::Duration duration;
    MY_LOG_IF(m_fgLogEn, "[%s] start waitVSirq.", __FUNCTION__);
    CAM_TRACE_FMT_BEGIN("3A_WaitVSync #(%d)", pParam->i4MagicNum);
    m_pEventIrq->wait(duration);
    CAM_TRACE_FMT_END();

    if (m_pCbSet)
        m_pCbSet->doNotifyCb(I3ACallBack::eID_NOTIFY_VSYNC_DONE, pParam->i4MagicNum, 0, 0);

    if (!postCommand(ECmd_Update, pParam))
    {
        MY_ERR("ECmd_Update failed");
        return MFALSE;
    }
    return MTRUE;
}

MBOOL
Hal3AFlowCtrl::
postCommand(ECmd_T const eCmd, const ParamIspProfile_T* pParam)
{
    MY_LOG_IF(m_fgLogEn, "[%s] enter(%d)", __FUNCTION__, eCmd);
    MUINT32 u4MagicNum = 0;
    MUINT32 u4MagicNumCur = 0;
    const ParamIspProfile_T* pParamIspProf = pParam;
    MBOOL fgUpdate = (eCmd == ECmd_Update) && (pParamIspProf->iValidateOpt != ParamIspProfile_T::EParamValidate_None);
    if (eCmd == ECmd_CameraPreviewStart)
    {
        m_rResultBuf.clearAll();
        m_rResultBufCur.clearAll();
        if (m_pEventIrq == NULL)
        {
            IEventIrq::ConfigParam IrqConfig(m_i4SensorDev, m_i4SensorOpenIdx, 5000000, IEventIrq::E_Event_Vsync);
            m_pEventIrq = IEventIrq::createInstance(IrqConfig, "VSIrq");
        }
#if (CAM3_3ATESTLVL >= CAM3_3ASTTUT)
        m_p3ASttCtrl->initStt(m_i4SensorDev, m_i4SensorOpenIdx);
        m_p3ASttCtrl->startStt();
#endif
        m_p3AWrap->start();
    }

    if (fgUpdate)
    {
        MBOOL bPre;
        MBOOL bMainFlash;
        u4MagicNum = pParamIspProf->i4MagicNum;
        u4MagicNumCur = pParamIspProf->i4MagicNumCur;
        bMainFlash = m_p3AWrap->chkMainFlashOnCond();
        bPre = m_p3AWrap->chkPreFlashOnCond();
        if (bMainFlash||bPre) m_u4FlashOnIdx = u4MagicNum;
        if (bMainFlash) m_p3AWrap->setFlashLightOnOff(1, 1);
        if (bPre) m_p3AWrap->setFlashLightOnOff(1, 0);
    }

    m_p3AWrap->postCommand(eCmd, pParamIspProf);

    if (fgUpdate)
    {
        on3AProcFinish(u4MagicNum, u4MagicNumCur);
    }

    if (eCmd == ECmd_CameraPreviewEnd)
    {
        m_p3AWrap->stop();
#if (CAM3_3ATESTLVL >= CAM3_3ASTTUT)
        m_p3ASttCtrl->stopStt();
        m_p3ASttCtrl->uninitStt();
#endif
        if (m_pEventIrq)
        {
            m_pEventIrq->destroyInstance("VSIrq");
            m_pEventIrq = NULL;
        }
    }

    return MTRUE;
}

MVOID
Hal3AFlowCtrl::
on3AProcFinish(MUINT32 u4MagicNum, MUINT32 u4MagicNumCur)
{
    updateResult(u4MagicNum);

    if (m_pCbSet)
    {
        MINT32 i4CurId = m_p3AWrap->getCurrentHwId();
        MY_LOG_IF(m_fgLogEn, "[%s] SOF(0x%x)", __FUNCTION__, i4CurId);

        CAM_TRACE_FMT_BEGIN("3A_CB #(%d), SOF(%d)", u4MagicNum, i4CurId);
        m_pCbSet->doNotifyCb(
            I3ACallBack::eID_NOTIFY_3APROC_FINISH,
            u4MagicNum,     // magic number
            i4CurId,        // SOF idx
            m_u4FrmIdStat);

#warning "FIXME"
/*
        // for flash state
        if (!(m_rParam.u4HalFlag & HAL_FLG_DUMMY))
        {
            m_pCbSet->doNotifyCb(
                I3ACallBack::eID_NOTIFY_CURR_RESULT,
                u4MagicNumCur,
                MTK_FLASH_STATE,
                m_rResult.u1FlashState);
        }
*/
        CAM_TRACE_FMT_END();
    }
}

MVOID
Hal3AFlowCtrl::
updateResult(MUINT32 u4MagicNum)
{
    MY_LOG_IF(m_fgLogEn, "[%s] u4MagicNum(%d)", __FUNCTION__, u4MagicNum);
    // pass1 result
    m_p3AWrap->getCurrResult(u4MagicNum, m_rResult);
    m_rResultBuf.updateResult(u4MagicNum, m_rResult);
    // result for high quality
//    m_rResultBufCur.updateResult(m_u4FrmIdStat, m_rResult);
}

MINT32
Hal3AFlowCtrl::
getResult(MUINT32 i4FrmId, Result_T& rResult)
{
    MINT32 i4Ret = m_rResultBuf.getResult(i4FrmId, rResult);
    if (-1 == i4Ret)
    {
        MY_ERR("Fail get Result(%d)", i4FrmId);
        m_p3AWrap->getCurrResult(i4FrmId, rResult);
    }
    return i4Ret;
}

MINT32
Hal3AFlowCtrl::
getResultCur(MUINT32 i4FrmId, Result_T& rResult)
{
    MINT32 i4Ret = m_rResultBufCur.getResult(i4FrmId, rResult);
    if (-1 == i4Ret)
    {
        MY_ERR("Fail get Result(%d)", i4FrmId);
        m_p3AWrap->getCurrResult(i4FrmId, rResult);
    }
    return i4Ret;
}

MVOID
Hal3AFlowCtrl::
notifyP1Done(MUINT32 u4MagicNum, MVOID* pvArg)
{
    MY_LOG_IF(m_fgLogEn, "[%s] u4MagicNum(%d)", __FUNCTION__, u4MagicNum);
    m_u4FrmIdStat = u4MagicNum;
//    updateImmediateResult(u4MagicNum);
    if (u4MagicNum == m_u4FlashOnIdx)
    {
        m_p3AWrap->setFlashLightOnOff(0, 1); // don't care main or pre
        m_u4FlashOnIdx = -1;
    }
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// setCallbacks
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MINT32
Hal3AFlowCtrl::
attachCb(I3ACallBack* cb)
{
    MY_LOG("[%s] m_pCbSet(0x%08x), cb(0x%08x)", __FUNCTION__, m_pCbSet, cb);
    m_pCbSet = cb;
    return 0;
}

MINT32
Hal3AFlowCtrl::
detachCb(I3ACallBack* cb)
{
    MY_LOG("[%s] m_pCbSet(0x%08x), cb(0x%08x)", __FUNCTION__, m_pCbSet, cb);
    m_pCbSet = NULL;
    return 0;

}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// passing to wrapper functions directly
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#if 0
MBOOL
Hal3AFlowCtrl::isMeetMainFlashOnCondition()
{
    return
        (((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_OFF)||(m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON)) && (m_rParam.u4StrobeMode == MTK_FLASH_MODE_SINGLE))
        ||
        ( ((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON_ALWAYS_FLASH)||((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON_AUTO_FLASH) && isStrobeBVTrigger()))
           && (m_rParam.u1CaptureIntent == MTK_CONTROL_CAPTURE_INTENT_STILL_CAPTURE) );
}

MBOOL
Hal3AFlowCtrl::isMeetPreFlashOnCondition()
{
    return
        (((m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_OFF)||(m_rParam.u4AeMode == MTK_CONTROL_AE_MODE_ON)) && (m_rParam.u4StrobeMode == MTK_FLASH_MODE_TORCH));
}


MBOOL
Hal3AFlowCtrl::isStrobeBVTrigger()
{
    MY_ERR("[%s] it should be overwritten", __FUNCTION__);
    return MFALSE;
}

MBOOL Hal3AFlowCtrl::setFlashLightOnOff(MBOOL bOnOff/*1=on; 0=off*/, MBOOL bMainPre/*1=main; 0=pre*/)
{
    MY_ERR("[%s] it should be overwritten", __FUNCTION__);
    return MTRUE;
}
#endif

MVOID
Hal3AFlowCtrl::
setSensorMode(MINT32 i4SensorMode)
{
    m_p3AWrap->setSensorMode(i4SensorMode);
}

MBOOL
Hal3AFlowCtrl::
setParams(Param_T const &rNewParam)
{
    MY_LOG("[%s]", __FUNCTION__);
    return m_p3AWrap->setParams(rNewParam);
}

MBOOL
Hal3AFlowCtrl::
autoFocus()
{
    return m_p3AWrap->autoFocus();
#if 0
    MY_LOG("[%s()]\n", __FUNCTION__);

if (ENABLE_3A_GENERAL & m_3ACtrlEnable) {
    if ((m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_PICTURE) && (m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_VIDEO))   {
        //ERROR_CHECK(mpStateMgr->sendCmd(ECmd_AFStart));
    }
    //IAfMgr::getInstance().autoFocus(m_i4SensorDev);
}
    return MTRUE;
#endif
}

MBOOL
Hal3AFlowCtrl::
cancelAutoFocus()
{
    return m_p3AWrap->cancelAutoFocus();
#if 0
    MY_LOG("[%s()]\n", __FUNCTION__);

    if ((m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_PICTURE) && (m_rParam.u4AfMode != MTK_CONTROL_AF_MODE_CONTINUOUS_VIDEO))   {
        //ERROR_CHECK(mpStateMgr->sendCmd(ECmd_AFEnd));
    }
    //IAfMgr::getInstance().cancelAutoFocus(m_i4SensorDev);
    return MTRUE;
#endif
}

MVOID
Hal3AFlowCtrl::
setFDEnable(MBOOL fgEnable)
{
    m_p3AWrap->setFDEnable(fgEnable);
}

MBOOL
Hal3AFlowCtrl::
setFDInfo(MVOID* prFaces)
{
    return m_p3AWrap->setFDInfo(prFaces);
}

MBOOL
Hal3AFlowCtrl::
setZoom(MUINT32 u4ZoomRatio_x100, MUINT32 u4XOffset, MUINT32 u4YOffset, MUINT32 u4Width, MUINT32 u4Height)
{
    return MTRUE;
}

MINT32
Hal3AFlowCtrl::
getDelayFrame(EQueryType_T const eQueryType) const
{
    return 0;
}

MBOOL
Hal3AFlowCtrl::
setIspPass2(MINT32 flowType, const NSIspTuning::RAWIspCamInfo& rCamInfo, void* pRegBuf)
{
    return m_p3AWrap->generateP2(flowType, rCamInfo, pRegBuf);
}

MBOOL
Hal3AFlowCtrl::
notifyPwrOn()
{
    //IAfMgr::getInstance().CamPwrOnState(m_i4SensorDev);
    MY_ERR("[%s] it should be overwritten", __FUNCTION__);
    return MTRUE;
}

MBOOL
Hal3AFlowCtrl::
notifyPwrOff()
{
    //IAfMgr::getInstance().CamPwrOffState(m_i4SensorDev);
    MY_ERR("[%s] it should be overwritten", __FUNCTION__);
    return MTRUE;
}

MBOOL
Hal3AFlowCtrl::
checkCapFlash() const
{
    return m_p3AWrap->chkCapFlash();
}

MBOOL
Hal3AFlowCtrl::
getDebugInfo(android::Vector<MINT32>& keys, android::Vector< android::Vector<MUINT8> >& data) const
{
    MY_ERR("[%s] FIXME it should be overwritten", __FUNCTION__);
    return MFALSE;
}

MINT32
Hal3AFlowCtrl::
send3ACtrl(E3ACtrl_T e3ACtrl, MINTPTR i4Arg1, MINTPTR i4Arg2)
{
    return m_p3AWrap->send3ACtrl(e3ACtrl, i4Arg1, i4Arg2);
}
