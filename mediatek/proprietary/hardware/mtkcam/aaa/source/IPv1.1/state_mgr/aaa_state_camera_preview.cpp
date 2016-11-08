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
#define LOG_TAG "aaa_state_camera_preview"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <aaa_types.h>
#include <aaa_state.h>
#include <aaa_state_mgr.h>
#include <aaa_common_custom.h>
#include <aaa_hal_if.h>
#include <buf_mgr/aaa_buf_mgr.h>

#include <debug_exif/aaa/dbg_aaa_param.h>
#include <debug_exif/aaa/dbg_af_param.h>

#include <aaa/af_param.h>
#include <aaa/awb_param.h>
#include <aaa/flash_param.h>

#include <af_feature.h>
#include <af_algo_if.h>

#include <ae_mgr/ae_mgr_if.h>
#include <af_mgr/af_mgr_if.h>
#include <awb_mgr/awb_mgr_if.h>
#include <lsc_mgr/ILscTsf.h>
#include <flash_mgr/flash_mgr.h>
#include <flash_feature.h>
#include <flicker/flicker_hal_base.h>
#include <sensor_mgr/aaa_sensor_buf_mgr.h>

#include <awb_tuning_custom.h>
#include <flash_awb_param.h>
#include <flash_awb_tuning_custom.h>
#include <flash_tuning_custom.h>


using namespace NS3Av3;
using namespace NSIspTuning;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  StateCameraPreview
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
StateCameraPreview::
StateCameraPreview(MINT32 sensorDevId, StateMgr* pStateMgr)
    : IState("StateCameraPreview", sensorDevId, pStateMgr)
{
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_Uninit
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_Uninit>)
{
    MY_LOG("[StateCameraPreview::sendIntent]<eIntent_Uninit>");
    // State transition: eState_CameraPreview --> eState_Uninit
    m_pStateMgr->transitState(eState_CameraPreview, eState_Uninit);
    return  S_3A_OK;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_CameraPreviewEnd
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_CameraPreviewEnd>)
{
    MRESULT err = S_3A_OK;

    MY_LOG("[StateCameraPreview::sendIntent]<eIntent_CameraPreviewEnd>");
    // State transition: eState_CameraPreview --> eState_Init
    m_pStateMgr->transitState(eState_CameraPreview, eState_Init);

    return  S_3A_OK;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_VsyncUpdate
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_VsyncUpdate>)
{
    MINT32 i4SceneLv = 80;
    MINT32 i4AoeCompLv = 80;
    MBOOL bAEStable = MTRUE;

    // Update frame count
    m_pStateMgr->updateFrameCount();
    MY_LOG_IF(m_i4EnableLog
            , "[StateCameraPreview::sendIntent<eIntent_VsyncUpdate>] enter, frameCnt=%d\n"
            , m_pStateMgr->getFrameCount());

    StatisticBufInfo* rBufInfo;
    if (m_pStateMgr->getFrameCount() < 0) {// AAO statistics is not ready
        //m_pHal3A->m_b3APvInitOK = MFALSE;
        return S_3A_OK;
    }

    // Dequeue AAO buffer from SW buffer
    rBufInfo = IAAABufMgr::getInstance().dequeueSwBuf(m_SensorDevId, STT_AAO);
    MY_LOG_IF(m_i4EnableLog, "[StateCameraPreview::sendIntent<eIntent_VsyncUpdate>] dequeueSwBuf AAO done\n");

    // AE
    //i4SceneLv = IAeMgr::getInstance().getLVvalue(m_SensorDevId, (m_pHal3A->get3APreviewMode() == EPv_Normal) ? MFALSE : MTRUE);
    //i4AoeCompLv = IAeMgr::getInstance().getAOECompLVvalue(m_SensorDevId, (m_pHal3A->get3APreviewMode() == EPv_Normal) ? MFALSE : MTRUE);
    i4SceneLv = IAeMgr::getInstance().getLVvalue(m_SensorDevId, MFALSE);
    i4AoeCompLv = IAeMgr::getInstance().getAOECompLVvalue(m_SensorDevId, MFALSE);
    bAEStable = IAeMgr::getInstance().IsAEStable(m_SensorDevId);

#if CAM3_AF_FEATURE_EN
    if(CUST_LOCK_AE_DURING_CAF())
    {
        if (IAfMgr::getInstance().isFocusFinish(m_SensorDevId) || //if =1, lens are fixed, do AE as usual; if =0, lens are moving, don't do AE
            (IAeMgr::getInstance().IsAEStable(m_SensorDevId) == MFALSE)) //guarantee AE can doPvAE at beginning, until IsAEStable()=1
        {
             IAeMgr::getInstance().setAFAELock(m_SensorDevId, MFALSE);
        }
        else
        {
             IAeMgr::getInstance().setAFAELock(m_SensorDevId, MTRUE);
        }
    }
    else //always do AE, no matter whether lens are moving or not
    {
        IAeMgr::getInstance().setAFAELock(m_SensorDevId, MFALSE);
    }
#endif

    IAeMgr::getInstance().doPvAE(m_SensorDevId, m_pStateMgr->getFrameCount(), reinterpret_cast<MVOID *>(rBufInfo->mVa),
    0, 1, 0);
    if (IAeMgr::getInstance().IsNeedUpdateSensor(m_SensorDevId))
    {
    #if USE_AE_THD
        //m_pHal3A->mbPostAESenThd = MTRUE;
        m_pStateMgr->postToAESenThread();
    #else
        IAeMgr::getInstance().updateSensorbyI2C(m_SensorDevId);
    #endif
    }
    // workaround for iVHDR
    MUINT32 u4AFSGG1Gain;
    IAeMgr::getInstance().getAESGG1Gain(m_SensorDevId, &u4AFSGG1Gain);
    IAfMgr::getInstance().setSGGPGN(m_SensorDevId, (MINT32) u4AFSGG1Gain);

    MY_LOG_IF(m_i4EnableLog, "[StateCameraPreview::sendIntent<eIntent_VsyncUpdate>] doPvAE done\n");

    // AWB
    IAwbMgr::getInstance().doPvAWB(m_SensorDevId, m_pStateMgr->getFrameCount(), bAEStable, i4AoeCompLv, reinterpret_cast<MVOID *>(rBufInfo->mVa));
    MY_LOG_IF(m_i4EnableLog, "[StateCameraPreview::sendIntent<eIntent_VsyncUpdate>] doPvAWB done\n");

#if CAM3_LSC_FEATURE_EN
    // TSF
    AWB_OUTPUT_T rAWBOutput;
    IAwbMgr::getInstance().getAWBOutput(m_SensorDevId, rAWBOutput);
    ILscMgr::TSF_AWB_INFO rAwbInfo;
    ILscMgr::TSF_INPUT_INFO_T rTsfInfo;
    ILscMgr* pLsc = ILscMgr::getInstance(static_cast<ESensorDev_T>(m_SensorDevId));
    rAwbInfo.m_i4LV        = IAeMgr::getInstance().getLVvalue(m_SensorDevId, MTRUE);
    rAwbInfo.m_u4CCT    = IAwbMgr::getInstance().getAWBCCT(m_SensorDevId);
    rAwbInfo.m_RGAIN    = rAWBOutput.rAWBInfo.rCurrentAWBGain.i4R;
    rAwbInfo.m_GGAIN    = rAWBOutput.rAWBInfo.rCurrentAWBGain.i4G;
    rAwbInfo.m_BGAIN    = rAWBOutput.rAWBInfo.rCurrentAWBGain.i4B;
    rAwbInfo.m_FLUO_IDX = rAWBOutput.rAWBInfo.i4FluorescentIndex;
    rAwbInfo.m_DAY_FLUO_IDX = rAWBOutput.rAWBInfo.i4DaylightFluorescentIndex;
    rTsfInfo.eCmd = (0 == m_pStateMgr->getFrameCount()) ? ILscMgr::E_TSF_CMD_BATCH : ILscMgr::E_TSF_CMD_RUN;
    rTsfInfo.u4FrmId = m_pStateMgr->getFrameCount();
    rTsfInfo.rAwbInfo = rAwbInfo;
    rTsfInfo.prAwbStat = reinterpret_cast<MUINT8*>(rBufInfo->mVa);
    rTsfInfo.u4SizeAwbStat = rBufInfo->mSize;

    pLsc->updateTsf(rTsfInfo);

    MY_LOG("lv(%d),cct(%d),rgain(%d),bgain(%d),ggain(%d),fluoidx(%d), dayflouidx(%d)",
            rAwbInfo.m_i4LV,
            rAwbInfo.m_u4CCT,
            rAwbInfo.m_RGAIN,
            rAwbInfo.m_GGAIN,
            rAwbInfo.m_BGAIN,
            rAwbInfo.m_FLUO_IDX,
            rAwbInfo.m_DAY_FLUO_IDX
            );
#endif

#if CAM3_FLICKER_FEATURE_EN
    if (m_pStateMgr->mbIsRecording == MFALSE)
    {
        {
            FlickerInput flkIn;
            FlickerOutput flkOut;
            AE_MODE_CFG_T previewInfo;
            IAeMgr::getInstance().getPreviewParams(m_SensorDevId, previewInfo);
            flkIn.aeExpTime = previewInfo.u4Eposuretime;
            flkIn.afFullStat = IAfMgr::getInstance().getFLKStat(m_SensorDevId);
            //MY_LOG("qq1 bValid %d", flkIn.afFullStat.bValid);
            if(flkIn.afFullStat.bValid==1)
            {
                if(m_pHal3A->getTGInfo() == CAM_TG_1)
                {
                FlickerHalBase::getInstance().update(m_SensorDevId, &flkIn, &flkOut);
                    //MY_LOG("qq2 CAM_TG_1 %d", flkOut.flickerResult);
                }
                else
                {
                    int flkResult;
                    FlickerHalBase::getInstance().getFlickerResult(flkResult);
                    flkOut.flickerResult = flkResult;

                    //MY_LOG("qq2 CAM_TG_2 %d", flkOut.flickerResult);
                }

                if(flkOut.flickerResult == HAL_FLICKER_AUTO_60HZ)
                {
                    MY_LOG_IF(m_i4EnableLog, "setaeflicker 60hz");
                    IAeMgr::getInstance().setAEAutoFlickerMode(m_SensorDevId, 1);
                }
                else
                {
                    MY_LOG_IF(m_i4EnableLog, "setaeflicker 50hz");
                    IAeMgr::getInstance().setAEAutoFlickerMode(m_SensorDevId, 0);
                }

            }
            else
            {
                int flkResult;
                FlickerHalBase::getInstance().getFlickerResult(flkResult);
                flkOut.flickerResult = flkResult;
                if(flkOut.flickerResult == HAL_FLICKER_AUTO_60HZ)
                {
                    MY_LOG_IF(m_i4EnableLog, "setaeflicker 60hz");
                    IAeMgr::getInstance().setAEAutoFlickerMode(m_SensorDevId, 1);
            }
            else
                {
                    MY_LOG_IF(m_i4EnableLog, "setaeflicker 50hz");
                    IAeMgr::getInstance().setAEAutoFlickerMode(m_SensorDevId, 0);
                }
                //MY_LOG("qq1 skip bValid %d %d %d", flkIn.afFullStat.bValid,__LINE__,flkOut.flickerResult);
                MY_LOG_IF(m_i4EnableLog, "skip flicker");
            }

        }

    }
#endif

    return  S_3A_OK;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_PrecaptureStart
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_PrecaptureStart>)
{
    MY_LOG("[StateCameraPreview::sendIntent]<eIntent_PrecaptureStart>");

    // Init
    IAeMgr::getInstance().setAeMeterAreaEn(m_SensorDevId, 1);

    m_pStateMgr->resetPrecapState(); //reset Precap state
    // State transition: eState_CameraPreview --> eState_Precapture
    m_pStateMgr->transitState(eState_CameraPreview, eState_Precapture);

    return  S_3A_OK;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_CaptureStart
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_CaptureStart>)
{
    // Update frame count
    m_pStateMgr->updateFrameCount();
    MY_LOG("sendIntent(intent2type<eIntent_CaptureStart>) line(%d), frame(%d)",__LINE__, m_pStateMgr->getFrameCount());

    MINT32 bIsFlashOn = MFALSE;

#if CAM3_FLASH_FEATURE_EN
    FlashMgr::getInstance().capCheckAndFireFlash_Start(m_SensorDevId);
    bIsFlashOn = FlashMgr::getInstance().isFlashOnCapture(m_SensorDevId);
#endif

    // AWB: update AWB statistics config
    IAwbMgr::getInstance().setStrobeMode(m_SensorDevId, (bIsFlashOn ? AWB_STROBE_MODE_ON : AWB_STROBE_MODE_OFF));


    IAeMgr::getInstance().setStrobeMode(m_SensorDevId, (bIsFlashOn ? MTRUE : MFALSE));
    // AE: update capture parameter
    IAeMgr::getInstance().doCapAE(m_SensorDevId);

    return  S_3A_OK;
}



//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_AFStart
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_AFStart>)
{
    MY_LOG("[StateCameraPreview::sendIntent]<eIntent_AFStart>");

    IAeMgr::getInstance().setAeMeterAreaEn(m_SensorDevId, 1);

#if CAM3_FLASH_FEATURE_EN
    FlashMgr::getInstance().notifyAfEnter(m_SensorDevId);
#endif

    m_pStateMgr->setNextState(eState_Invalid); //reset 3A Next state
    m_pStateMgr->resetAFState(); //only single entrance point: EAFState_T=0
    m_pStateMgr->mAFStateCntSet.resetAll(); //reset all AFState cnt, flags
    m_pStateMgr->transitState(eState_CameraPreview, eState_AF);

    return  S_3A_OK;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  eIntent_AFEnd
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MRESULT
StateCameraPreview::
sendIntent(intent2type<eIntent_AFEnd>)
{
    MY_LOG("[StateCameraPreview::sendIntent]<eIntent_AFEnd>");

    return  S_3A_OK;
}
