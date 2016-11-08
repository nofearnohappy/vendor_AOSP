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
#define LOG_TAG "aaa_hal_sttCtrl"

#include <isp_tuning.h>
#include <aaa_hal_sttCtrl.h>
#include <cutils/properties.h>
#include <aaa_log.h>

#if defined(HAVE_AEE_FEATURE)
#include <aee.h>
#define AEE_ASSERT_3A_HAL(String) \
          do { \
              aee_system_exception( \
                  "Hal3ASttCtrl", \
                  NULL, \
                  DB_OPT_DEFAULT, \
                  String); \
          } while(0)
#else
#define AEE_ASSERT_3A_HAL(String)
#endif

#define ENABLE_STT_FLOW_AAO 1
#define ENABLE_STT_FLOW_AFO 2
#define ENABLE_STT_FLOW_FLKO 4

using namespace NS3Av3;

Hal3ASttCtrl*
Hal3ASttCtrl::
createInstance(MINT32 i4SensorDevId, MINT32 i4SensorOpenIndex)
{
    MY_LOG("Hal3ASttFlow::createInstance i4SensorDevId(%d), i4SensorOpenIndex(%d)\n"
           , i4SensorDevId
           , i4SensorOpenIndex);

    switch (i4SensorDevId)
    {
        case ESensorDev_Main:
            //Hal3ASttCtrlDev<ESensorDev_Main>::getInstance()->initStt(i4SensorDevId, i4SensorOpenIndex);
            return Hal3ASttCtrlDev<ESensorDev_Main>::getInstance();
        break;
        case ESensorDev_Sub:
            //Hal3ASttCtrlDev<ESensorDev_Sub>::getInstance()->initStt(i4SensorDevId, i4SensorOpenIndex);
            return Hal3ASttCtrlDev<ESensorDev_Sub>::getInstance();
        break;
        case ESensorDev_MainSecond:
            //Hal3ASttCtrlDev<ESensorDev_MainSecond>::getInstance()->initStt(i4SensorDevId, i4SensorOpenIndex);
            return Hal3ASttCtrlDev<ESensorDev_MainSecond>::getInstance();
        break;
        default:
            MY_ERR("Unsupport sensor device ID: %d\n", i4SensorDevId);
            AEE_ASSERT_3A_HAL("Unsupport sensor device.");
            return MNULL;
    }
}

Hal3ASttCtrl*
Hal3ASttCtrl::
getInstance(MINT32 i4SensorDevId)
{
    switch (i4SensorDevId)
    {
        case ESensorDev_Main:
            return Hal3ASttCtrlDev<ESensorDev_Main>::getInstance();
        case ESensorDev_Sub:
            return Hal3ASttCtrlDev<ESensorDev_Sub>::getInstance();
        case ESensorDev_MainSecond:
            return Hal3ASttCtrlDev<ESensorDev_MainSecond>::getInstance();
        default:
            MY_ERR("Unsupport sensor device ID: %d\n", i4SensorDevId);
            AEE_ASSERT_3A_HAL("Unsupport sensor device.");
            return MNULL;
    }

}

MVOID
Hal3ASttCtrl::
destroyInstance()
{
    MY_LOG("[%s] \n", __FUNCTION__);
    //uninitStt();
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
Hal3ASttCtrl::Hal3ASttCtrl()
    : m_Users(0)
    , m_Lock()
{}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
Hal3ASttCtrl::~Hal3ASttCtrl()
{}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
Hal3ASttCtrl::
initStt(MINT32 i4SensorDevId, MINT32 i4SensorOpenIndex)
{
    MY_LOG("[%s] i4SensorDevId(%d), i4SensorOpenIndex(%d)\n"
           , __FUNCTION__
           , i4SensorDevId
           , i4SensorOpenIndex);

    Mutex::Autolock lock(m_Lock);

    // check user count
    if (m_Users > 0)
    {
        MY_LOG_IF(1,"%d has created \n", m_Users);
        android_atomic_inc(&m_Users);
        return MTRUE;
    }

    m_i4SensorDev = i4SensorDevId;
    // AAA statistic mgr init
    IAAABufMgr::getInstance().init(m_i4SensorDev, i4SensorOpenIndex);
    android_atomic_inc(&m_Users);
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
Hal3ASttCtrl::
uninitStt()
{
    MY_LOG("[%s] \n", __FUNCTION__);

    Mutex::Autolock lock(m_Lock);

    // If no more users, return directly and do nothing.
    if (m_Users <= 0)
    {
        return MTRUE;
    }

    // More than one user, so decrease one User.
    android_atomic_dec(&m_Users);

    if(m_Users == 0)
    {
        // AAA statistic mgr uninit
        IAAABufMgr::getInstance().uninit(m_i4SensorDev);
    }
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
Hal3ASttCtrl::
startStt()
{
    MY_LOG("[%s] \n", __FUNCTION__);
    MY_LOG("IAAABugMgr start\n");
    IAAABufMgr::getInstance().start(m_i4SensorDev);

    char value[256] = {'\0'};
    MUINT enable;
    property_get("debug.stt_flow.enable", value, "1");
    enable = atoi(value);

    if(enable & ENABLE_STT_FLOW_AAO)
        rAAOBufThread = ThreadStatisticBuf::createInstance(m_i4SensorDev, DEQUE_STT_AAO);
    if(enable & ENABLE_STT_FLOW_FLKO)
        rFLKBufThread = ThreadStatisticBuf::createInstance(m_i4SensorDev, DEQUE_STT_FLKO);
    if(enable & ENABLE_STT_FLOW_AFO)
        rAFOBufThread = ThreadStatisticBuf::createInstance(m_i4SensorDev, DEQUE_STT_AFO);
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
Hal3ASttCtrl::
stopStt()
{
    MY_LOG("[%s] \n", __FUNCTION__);
    if(rAAOBufThread != NULL)
        rAAOBufThread->destroyInstance();
    if(rFLKBufThread != NULL)
      rFLKBufThread->destroyInstance();
    if(rAFOBufThread != NULL)
        rAFOBufThread->destroyInstance();

    MY_LOG("IAAABugMgr stop\n");
    IAAABufMgr::getInstance().stop(m_i4SensorDev);
    return MTRUE;
}



