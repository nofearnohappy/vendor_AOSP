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
#define LOG_TAG "aaa_statistic_mgr"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <sys/stat.h>
#include <cutils/properties.h>
#include <string.h>

#include <linux/cache.h>
#include <utils/threads.h>
#include <list>

#include <aaa_types.h>
#include <aaa_error_code.h>
#include <aaa_log.h>

#include "aaa_buf_mgr.h"
#include <CamIO/PortMap.h>
#include <CamIO/IStatisticPipe.h>
#include <CamIO/IHalCamIO.h>

#include <isp_tuning.h>

using namespace std;
using namespace android;
using namespace NS3Av3;
using namespace NSCam::NSIoPipe;
using namespace NSCam::NSIoPipe::NSCamIOPipe;

#define MAX_STATISTIC_BUFFER_CNT (2)
#define ENABLE_STT_FLOW_AAO 1
#define ENABLE_STT_FLOW_AFO 2
#define ENABLE_STT_FLOW_FLKO 4

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  AAO buffer
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#define AAO_STT_DATA_SIZE (10)                    // byte
#define AAO_STT_PLINE_NUM (90)
#define AAO_STT_BLOCK_NUM (120)
#define AAO_STT_HIST_BIN_NUM (128)
#define AAO_STT_HIST_DATA_SIZE (3)                // byte
#define AAO_STT_HIST_NUM (4)
#define AAO_BUF_SIZE (AAO_STT_DATA_SIZE * AAO_STT_PLINE_NUM * AAO_STT_BLOCK_NUM + AAO_STT_HIST_BIN_NUM * AAO_STT_HIST_DATA_SIZE * AAO_STT_HIST_NUM)
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  AAO buffer (Separated)
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#define AAO_SEP_AWB_SIZE (4*AAO_STT_BLOCK_NUM*AAO_STT_PLINE_NUM)
#define AAO_SEP_AE_SIZE (2*AAO_STT_BLOCK_NUM*AAO_STT_PLINE_NUM)
#define AAO_SEP_HIST_SIZE (4*4*AAO_STT_HIST_BIN_NUM)
#define AAO_SEP_OVEREXPCNT_SIZE (2*AAO_STT_BLOCK_NUM*AAO_STT_PLINE_NUM)
#define AAO_SEP_LSC_SIZE (2*4*AAO_STT_BLOCK_NUM*AAO_STT_PLINE_NUM)
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  AFO buffer
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#define AFO_WIN_NUM (128)
#define AFO_WIN_DATA (16)                         // byte
#define AFO_BUF_SIZE (AFO_WIN_NUM * AFO_WIN_NUM * AFO_WIN_DATA)

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  FLKO buffer
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#define FLKO_MAX_LENG (4096)
#define FLKO_BUF_SIZE (FLKO_MAX_LENG * 4 * 3)

typedef list<StatisticBuf> BufInfoList_T;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class AAABufMgr
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Ctor/Dtor.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    //  Copy constructor is disallowed.
    AAABufMgr(AAABufMgr const&);
    //  Copy-assignment operator is disallowed.
    AAABufMgr& operator=(AAABufMgr const&);

public:
    AAABufMgr(ESensorDev_T const eSensorDev);
    ~AAABufMgr();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Operations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    static AAABufMgr& getInstance(MINT32 const i4SensorDev);
    MBOOL init(MINT32 const i4SensorIdx);
    MBOOL uninit();
    MBOOL start();
    MBOOL stop();
    MBOOL debugPrint();
    MBOOL dequeueHwBuf(STT_PORTS port);
    StatisticBufInfo* dequeueSwBuf(STT_PORTS port);

    MBOOL allocateSwBuf(StatisticBuf &rBufInfo, MUINT32 u4BufSize);
    MBOOL freeSwBuf(StatisticBuf &rBufInfo);

//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data member
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    ESensorDev_T const     m_eSensorDev;
    volatile MINT32        m_Users;
    mutable android::Mutex m_Lock;
    MBOOL                  m_bDebugEnable;
    MINT32                 m_i4SensorIdx;

    IStatisticPipe*        mpSttPipe;
    MUINT32                m_rAAABufIndex[STT_DMAO_NUM];  // the index of write buffers.
    StatisticBuf           m_rAAABufInfo[STT_DMAO_NUM][MAX_STATISTIC_BUFFER_CNT];
    PortID                 portIDMap[STT_DMAO_NUM] = {PORT_AAO, PORT_AFO, PORT_FLKO};
};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
template <ESensorDev_T const eSensorDev>
class AAABufMgrDev : public AAABufMgr
{
public:
    static
    AAABufMgr&
    getInstance()
    {
        static AAABufMgrDev<eSensorDev> singleton;
        return singleton;
    }

    AAABufMgrDev()
        : AAABufMgr(eSensorDev)
    {}

    virtual ~AAABufMgrDev() {}
};

#if (CAM3_3ATESTLVL > CAM3_3AUT)
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
IAAABufMgr::
init(MINT32 const i4SensorDev, MINT32 const i4SensorIdx)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
        ret_main = AAABufMgr::getInstance(ESensorDev_Main).init(i4SensorIdx);
    if (i4SensorDev & ESensorDev_Sub)
        ret_sub = AAABufMgr::getInstance(ESensorDev_Sub).init(i4SensorIdx);
    if (i4SensorDev & ESensorDev_MainSecond)
        ret_main2 = AAABufMgr::getInstance(ESensorDev_MainSecond).init(i4SensorIdx);

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
IAAABufMgr::
uninit(MINT32 const i4SensorDev)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
       ret_main = AAABufMgr::getInstance(ESensorDev_Main).uninit();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = AAABufMgr::getInstance(ESensorDev_Sub).uninit();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = AAABufMgr::getInstance(ESensorDev_MainSecond).uninit();

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
IAAABufMgr::
start(MINT32 const i4SensorDev)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
       ret_main = AAABufMgr::getInstance(ESensorDev_Main).start();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = AAABufMgr::getInstance(ESensorDev_Sub).start();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = AAABufMgr::getInstance(ESensorDev_MainSecond).start();

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
IAAABufMgr::
stop(MINT32 const i4SensorDev)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
       ret_main = AAABufMgr::getInstance(ESensorDev_Main).stop();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = AAABufMgr::getInstance(ESensorDev_Sub).stop();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = AAABufMgr::getInstance(ESensorDev_MainSecond).stop();

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
IAAABufMgr::
dequeueHwBuf(MINT32 const i4SensorDev, STT_PORTS port)
{
    if (i4SensorDev & ESensorDev_Main)
        return AAABufMgr::getInstance(ESensorDev_Main).dequeueHwBuf(port);
    else if (i4SensorDev & ESensorDev_Sub)
        return AAABufMgr::getInstance(ESensorDev_Sub).dequeueHwBuf(port);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return AAABufMgr::getInstance(ESensorDev_MainSecond).dequeueHwBuf(port);

    MY_ERR("Incorrect sensor device: i4SensorDev = %d\n", i4SensorDev);
    return MFALSE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
StatisticBufInfo*
IAAABufMgr::
dequeueSwBuf(MINT32 const i4SensorDev, STT_PORTS port)
{
    if (i4SensorDev & ESensorDev_Main)
        return AAABufMgr::getInstance(ESensorDev_Main).dequeueSwBuf(port);
    else if (i4SensorDev & ESensorDev_Sub)
        return AAABufMgr::getInstance(ESensorDev_Sub).dequeueSwBuf(port);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return AAABufMgr::getInstance(ESensorDev_MainSecond).dequeueSwBuf(port);

    MY_ERR("Incorrect sensor device: i4SensorDev = %d\n", i4SensorDev);
    return NULL;
}
#endif

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
AAABufMgr&
AAABufMgr::
getInstance(MINT32 const i4SensorDev)
{
    switch (i4SensorDev)
    {
    case ESensorDev_Main: //  Main Sensor
        return  AAABufMgrDev<ESensorDev_Main>::getInstance();
    case ESensorDev_MainSecond: //  Main Second Sensor
        return  AAABufMgrDev<ESensorDev_MainSecond>::getInstance();
    case ESensorDev_Sub: //  Sub Sensor
        return  AAABufMgrDev<ESensorDev_Sub>::getInstance();
    default:
        MY_ERR("i4SensorDev = %d\n", i4SensorDev);
        return  AAABufMgrDev<ESensorDev_Main>::getInstance();
    }
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
AAABufMgr::
AAABufMgr(ESensorDev_T const eSensorDev)
    : m_eSensorDev(eSensorDev)
    , m_Users(0)
    , m_Lock()
    , m_bDebugEnable(MTRUE)
    , m_i4SensorIdx(0)
{

}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
AAABufMgr::
~AAABufMgr()
{

}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
init(MINT32 const i4SensorIdx)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("debug.aaa_buf_mgr.enable", value, "0");
    m_bDebugEnable = atoi(value);

    // sensor index
    m_i4SensorIdx = i4SensorIdx;

    MY_LOG("[%s] m_eSensorDev: %d, m_i4SensorIdx: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_i4SensorIdx, m_Users);

    Mutex::Autolock lock(m_Lock);

    if (m_Users > 0)
    {
        MY_LOG("%d has created \n", m_Users);
        android_atomic_inc(&m_Users);
        return MTRUE;
    }

    // Statistic Pipe init
    MY_LOG("[%s] Statistic Pipe createInstance\n", __FUNCTION__);
    mpSttPipe = IStatisticPipe::createInstance(m_i4SensorIdx, LOG_TAG);
    MY_LOG("[%s] Statistic Pipe init\n", __FUNCTION__);
    if (MFALSE == mpSttPipe->init()) {
        MY_LOG("IStatisticPipe init fail");
        return MFALSE;
    }

    // Statistic Pipe config
    MUINT enable;
    property_get("debug.stt_flow.enable", value, "1");
    enable = atoi(value);

    MY_LOG("[%s] Statistic Pipe config\n", __FUNCTION__);
    std::vector<statPortInfo> vp;
    QInitStatParam statParm(vp);
    if (enable & ENABLE_STT_FLOW_AAO)
        statParm.mStatPortInfo.push_back(statPortInfo(PORT_AAO));
    if (enable & ENABLE_STT_FLOW_AFO)
        statParm.mStatPortInfo.push_back(statPortInfo(PORT_AFO));
    if (enable & ENABLE_STT_FLOW_FLKO)
        statParm.mStatPortInfo.push_back(statPortInfo(PORT_FLKO));

    mpSttPipe->configPipe(statParm);

    MY_LOG("[%s] AAO_BUF_SIZE: %d, AFO_BUF_SIZE: %d, FLKO_BUF_SIZE: %d \n", __FUNCTION__, AAO_BUF_SIZE, AFO_BUF_SIZE,FLKO_BUF_SIZE);
    // allocate SW buffer
    for (MINT32 i = 0; i < MAX_STATISTIC_BUFFER_CNT; i++) {
        // allocate AAO Buffer
        allocateSwBuf(m_rAAABufInfo[STT_AAO][i], AAO_BUF_SIZE);

        // allocate AFO Buffer
        allocateSwBuf(m_rAAABufInfo[STT_AFO][i], AFO_BUF_SIZE);

        // allocate FLKO Buffer
        allocateSwBuf(m_rAAABufInfo[STT_FLKO][i], FLKO_BUF_SIZE);
    }

    //debugPrint();

    android_atomic_inc(&m_Users);

    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
uninit()
{
    MY_LOG("[%s] m_eSensorDev: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_Users);

    Mutex::Autolock lock(m_Lock);

    // If no more users, return directly and do nothing.
    if (m_Users <= 0)
    {
        return MTRUE;
    }

    // More than one user, so decrease one User.
    android_atomic_dec(&m_Users);

    if (m_Users == 0) // There is no more User after decrease one User
    {
        for (MINT32 i = 0; i < STT_DMAO_NUM; i++){
            for (MINT32 j = 0; j < MAX_STATISTIC_BUFFER_CNT; j++) {
              MY_LOG("[%s] ( i , j )=(%d,%d) \n", __FUNCTION__, i, j);
                freeSwBuf(m_rAAABufInfo[i][j]);
            }
        }

        // Statistic Pipe uninit
        mpSttPipe->uninit();
      mpSttPipe->destroyInstance(LOG_TAG);
    }
    else  // There are still some users.
    {
        MY_LOG_IF(m_bDebugEnable,"Still %d users \n", m_Users);
    }

    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
start()
{
    MY_LOG("[%s()] m_eSensorDev: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_Users);
    if (m_Users <= 0)
    {
        MY_LOG("[%s]Fail : m_Users = %d\n", __FUNCTION__, m_Users);
        return MTRUE;
    }
    mpSttPipe->start();
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
stop()
{
    MY_LOG("[%s()] m_eSensorDev: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_Users);
    if (m_Users <= 0)
    {
        MY_LOG("[%s]Fail : m_Users = %d\n", __FUNCTION__, m_Users);
        return MTRUE;
    }
    mpSttPipe->stop();
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
debugPrint()
{
#if 0
    StatisticBufInfo* rSwBufInfo;
    for(MINT32 i = 0; i < STT_DMAO_NUM; i++) {
        for (MINT32 j = 0; j < MAX_STATISTIC_BUFFER_CNT; j++) {
            rSwBufInfo = m_rAAABufInfo[i][j].read();
            MY_LOG("m_rAAABufInfo[%d][%d].virtAddr:[0x%x]\n", i, j,rSwBufInfo->mVa);
        }
    }
#endif
    return MTRUE;
}


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
dequeueHwBuf(STT_PORTS port)
{
    MY_LOG_IF(m_bDebugEnable,"[%s] m_eSensorDev(%d) + \n", __FUNCTION__, m_eSensorDev);

    char value[PROPERTY_VALUE_MAX] = {'\0'};
    MBOOL bEnable;
    switch(port){
      case STT_AAO:
        property_get("aao.dump.enable", value, "0");
        bEnable = atoi(value);
        break;
      case STT_AFO:
        property_get("afo.dump.enable", value, "0");
        bEnable = atoi(value);
        break;
      case STT_FLKO:
        property_get("flko.dump.enable", value, "0");
        bEnable = atoi(value);
        break;
    }


    // deque HW buffer from driver.
    QBufInfo    rDQBuf;
    mpSttPipe->deque(portIDMap[port], rDQBuf);

    // get the last HW buffer to SW Buffer.
    int size = rDQBuf.mvOut.size();
    int index = m_rAAABufIndex[port];
    if(size == 0)
    {
        MY_ERR("[%s] rDQBuf.mvOut.size = 0", __FUNCTION__);
        return MFALSE;
    }
    BufInfo rLastBuf = rDQBuf.mvOut.at( size-1 );

    // copy the last HW buffer to SW Buffer.
    StatisticBufInfo rHwBuf;
    rHwBuf.mMagicNumber = rLastBuf.mMetaData.mMagicNum_tuning;
    rHwBuf.mSize = rLastBuf.mSize;
    rHwBuf.mVa = rLastBuf.mVa;
    m_rAAABufInfo[port][index].write(rHwBuf);

    MY_LOG_IF(m_bDebugEnable, "port(%d), index(%d), va[%p]/pa[0x%08x]/#(%d), Size(%d)",
              port, index, rHwBuf.mVa, rLastBuf.mPa, rHwBuf.mMagicNumber, rHwBuf.mSize);

    // dump aao/afo/flko data for debug
    if (bEnable) {
        char fileName[64];
    static MUINT32 count;
        switch(port){
          case STT_AAO:
            //sprintf(fileName, "/sdcard/aao/aao_%d.raw", rLastBuf.FrameBased.mMagicNum_tuning);
      sprintf(fileName, "/system/bin/aao/aao_%d.raw", count++);
            break;
          case STT_AFO:
            //sprintf(fileName, "/sdcard/afo/afo_%d.raw", rLastBuf.FrameBased.mMagicNum_tuning);
      sprintf(fileName, "/system/bin/afo/afo_%d.raw", count++);
            break;
          case STT_FLKO:
            //sprintf(fileName, "/sdcard/flko/flko_%d.raw", rLastBuf.FrameBased.mMagicNum_tuning);
      sprintf(fileName, "/system/bin/flko/flko_%d.raw", count++);
            break;
        }

        FILE *fp = fopen(fileName, "w");
        if (NULL == fp)
        {
            MY_ERR("fail to open file to save img: %s", fileName);
            MINT32 err;
            switch(port){
              case STT_AAO:
                err = mkdir("/system/bin/aao", S_IRWXU | S_IRWXG | S_IRWXO);
                break;
              case STT_AFO:
                err = mkdir("/system/bin/afo", S_IRWXU | S_IRWXG | S_IRWXO);
                break;
              case STT_FLKO:
                err = mkdir("/system/bin/flko", S_IRWXU | S_IRWXG | S_IRWXO);
                break;
            }
            MY_LOG("err = %d", err);
        } else
    {
            MY_LOG_IF(m_bDebugEnable,"%s\n", fileName);
            fwrite(reinterpret_cast<void *>(rLastBuf.mVa), 1, rLastBuf.mSize, fp);
            fclose(fp);
    }
    }

    // enque HW buffer back driver
  MY_LOG_IF(m_bDebugEnable, "enque Hw buffer back driver.\n");
    mpSttPipe->enque(rDQBuf);

    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
StatisticBufInfo*
AAABufMgr::
dequeueSwBuf(STT_PORTS port)
{
    int r_index = m_rAAABufIndex[port];
    StatisticBufInfo* pBuf = m_rAAABufInfo[port][r_index].read();
    // switch buffer
    m_rAAABufIndex[port] = ( r_index + 1 ) % MAX_STATISTIC_BUFFER_CNT;
    MY_LOG_IF(m_bDebugEnable,"[%s] m_eSensorDev(%d), port(%d), r_index(%d), w_index(%d)\n", __FUNCTION__, m_eSensorDev, port, r_index, m_rAAABufIndex[port]);
    return pBuf;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
allocateSwBuf(StatisticBuf &rBufInfo, MUINT32 u4BufSize)
{
    return rBufInfo.allocateBuf(u4BufSize);
}


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
AAABufMgr::
freeSwBuf(StatisticBuf &rBufInfo)
{
    return rBufInfo.freeBuf();
}

void IAAABufMgr::aaoSeparation(void* aao, int w, int h, void* awb, void* ae,  void* hist, void* overexpcnt,void* lsc)
{
  int i;
  int j;
  MUINT8* pawb=(MUINT8*)awb;
  MUINT8* p=(MUINT8*)aao;

  int lineByte;
  lineByte = (int)(w*8.5+0.5);

    // Fill awb
  if(awb!=0)
  {
    for(j=0;j<90;j++)
    {
      memcpy(pawb, p, w*4);
      p+=lineByte;
      pawb+=w*4;
    }
  }


    // Fill ae
    if (ae!=0)
    {
        MUINT16* pae=(MUINT16* )ae;
        p=(MUINT8*)aao;
        int aaoNoAWBSz;
        aaoNoAWBSz = (int)(w*4);
        for(j=0;j<90;j++)
    {

            for(int k=0;k<60;k++){
                MUINT16 LSBbit30 = (*(p+aaoNoAWBSz+w+k))&& 0xF;
                MUINT16 LSBbit74 = (*(p+aaoNoAWBSz+w+k))>>4;
                MUINT16 MSBfor30 = (*(p+aaoNoAWBSz+2*k));
                MUINT16 MSBfor74 = (*(p+aaoNoAWBSz+2*k+1));
                *(pae+2*k)  = (MSBfor30<<4)+LSBbit30;
                *(pae+2*k+1)= (MSBfor74<<4)+LSBbit74;
            }
        p += lineByte;
            pae+=w;
    }
    }
    //Lsc
  if (lsc!=0)
    {
        MUINT16* plsc=(MUINT16* )lsc;
        p=(MUINT8*)aao;
    int aaoNoLSCsz=(int)(w*6.5);
        for(j=0;j<90;j++)
    {

            for(int k=0;k<w*2;k++){
                MUINT16 LSBbit30 = (*(p+aaoNoLSCsz+k))&& 0xF;
                MUINT16 LSBbit74 = (*(p+aaoNoLSCsz+k))>>4;
                MUINT16 MSBfor30 = (*(p+2*k));
                MUINT16 MSBfor74 = (*(p+2*k+1));
                *(plsc+2*k)  = (MSBfor30<<4)+LSBbit30;
                if (k%2 ==0){
                    *(plsc+2*k+1)= (MSBfor74<<4)+LSBbit74;
                } else {
                    *(plsc+2*k+1)= MSBfor74;
                }
            }
      p += lineByte;
            plsc+=w*4;
    }
    }
    // Fill hist
  if(hist!=0)
  {
    int* pHist=(int* )hist;
    p=(MUINT8*)aao;
    int aaoNoHistSz;
    aaoNoHistSz = lineByte*h;
    p+=aaoNoHistSz;
    for(j=0;j<4;j++)
    {
      for(i=0;i<128;i++)
      {
        int v;
        int b1;
        int b2;
        int b3;
        b1 = *p;
        b2 = *(p+1);
        b3 = *(p+2);
        v = b1+(b2<<8)+(b3<<16);
        p+=3;
        *pHist=v;
        pHist++;
      }
    }
  }
    // Fill overexpcnt
  if(overexpcnt!=0)
  {
        MUINT16* poverexpcnt = (MUINT16* )overexpcnt;
        p=(MUINT8*)aao;
        int aaoNoAWBAESz;
        aaoNoAWBAESz = (int)(w*5.5+0.5);
        for (j=0;j<90;j++){

            for (int k=0; k<120; k++){
                 *(poverexpcnt+k)=*(p+aaoNoAWBAESz+k);
            }
            p += lineByte;
            poverexpcnt+=w;
        }
    }

}