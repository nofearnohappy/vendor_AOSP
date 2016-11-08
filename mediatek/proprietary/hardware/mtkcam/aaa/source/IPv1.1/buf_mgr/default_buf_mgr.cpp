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
#define LOG_TAG "default_statistic_mgr"

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

#include <camera_custom_nvram.h>
#include <isp_tuning.h>
#include <tuning_mgr.h>

using namespace std;
using namespace android;
using namespace NS3Av3;
using namespace NSCam::NSIoPipe;
using namespace NSCam::NSIoPipe::NSCamIOPipe;

#define MAX_STATISTIC_BUFFER_CNT (2)

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
class DefaultBufMgr
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Ctor/Dtor.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    //  Copy constructor is disallowed.
    DefaultBufMgr(DefaultBufMgr const&);
    //  Copy-assignment operator is disallowed.
    DefaultBufMgr& operator=(DefaultBufMgr const&);

public:
    DefaultBufMgr(ESensorDev_T const eSensorDev);
    ~DefaultBufMgr();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Operations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    static DefaultBufMgr& getInstance(MINT32 const i4SensorDev);
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
    
    MUINT32                m_rAAABufIndex[STT_DMAO_NUM];
    StatisticBuf              m_rAAABufInfo[STT_DMAO_NUM][MAX_STATISTIC_BUFFER_CNT];
    PortID                 portIDMap[STT_DMAO_NUM] = {PORT_AAO, PORT_AFO, PORT_FLKO};
};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
template <ESensorDev_T const eSensorDev>
class DefaultBufMgrDev : public DefaultBufMgr
{
public:
    static
    DefaultBufMgr&
    getInstance()
    {
        static DefaultBufMgrDev<eSensorDev> singleton;
        return singleton;
    }

    DefaultBufMgrDev()
        : DefaultBufMgr(eSensorDev)
    {}

    virtual ~DefaultBufMgrDev() {}
};

#if (CAM3_3ATESTLVL <= CAM3_3AUT)
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
        ret_main = DefaultBufMgr::getInstance(ESensorDev_Main).init(i4SensorIdx);
    if (i4SensorDev & ESensorDev_Sub)
        ret_sub = DefaultBufMgr::getInstance(ESensorDev_Sub).init(i4SensorIdx);
    if (i4SensorDev & ESensorDev_MainSecond)
        ret_main2 = DefaultBufMgr::getInstance(ESensorDev_MainSecond).init(i4SensorIdx);    

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
       ret_main = DefaultBufMgr::getInstance(ESensorDev_Main).uninit();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = DefaultBufMgr::getInstance(ESensorDev_Sub).uninit();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = DefaultBufMgr::getInstance(ESensorDev_MainSecond).uninit();    

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
       ret_main = DefaultBufMgr::getInstance(ESensorDev_Main).start();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = DefaultBufMgr::getInstance(ESensorDev_Sub).start();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = DefaultBufMgr::getInstance(ESensorDev_MainSecond).start();    

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
       ret_main = DefaultBufMgr::getInstance(ESensorDev_Main).stop();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = DefaultBufMgr::getInstance(ESensorDev_Sub).stop();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = DefaultBufMgr::getInstance(ESensorDev_MainSecond).stop();    

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
        return DefaultBufMgr::getInstance(ESensorDev_Main).dequeueHwBuf(port);
    else if (i4SensorDev & ESensorDev_Sub)
        return DefaultBufMgr::getInstance(ESensorDev_Sub).dequeueHwBuf(port);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return DefaultBufMgr::getInstance(ESensorDev_MainSecond).dequeueHwBuf(port);    

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
        return DefaultBufMgr::getInstance(ESensorDev_Main).dequeueSwBuf(port);
    else if (i4SensorDev & ESensorDev_Sub)
        return DefaultBufMgr::getInstance(ESensorDev_Sub).dequeueSwBuf(port);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return DefaultBufMgr::getInstance(ESensorDev_MainSecond).dequeueSwBuf(port);    

    MY_ERR("Incorrect sensor device: i4SensorDev = %d\n", i4SensorDev);
    return NULL;
}
#endif

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
DefaultBufMgr&
DefaultBufMgr::
getInstance(MINT32 const i4SensorDev)
{
    switch (i4SensorDev)
    {
    case ESensorDev_Main: //  Main Sensor
        return  DefaultBufMgrDev<ESensorDev_Main>::getInstance();
    case ESensorDev_MainSecond: //  Main Second Sensor
        return  DefaultBufMgrDev<ESensorDev_MainSecond>::getInstance();
    case ESensorDev_Sub: //  Sub Sensor
        return  DefaultBufMgrDev<ESensorDev_Sub>::getInstance();
    default:
        MY_ERR("i4SensorDev = %d\n", i4SensorDev);
        return  DefaultBufMgrDev<ESensorDev_Main>::getInstance();
    }
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
DefaultBufMgr::
DefaultBufMgr(ESensorDev_T const eSensorDev)
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
DefaultBufMgr::
~DefaultBufMgr()
{

}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
DefaultBufMgr::
init(MINT32 const i4SensorIdx)
{    
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("debug.aao_buf_mgr.enable", value, "1");
    m_bDebugEnable = atoi(value);

    // sensor index
    m_i4SensorIdx = i4SensorIdx;    

    MY_LOG("[%s()] m_eSensorDev: %d, m_i4SensorIdx: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_i4SensorIdx, m_Users);

    Mutex::Autolock lock(m_Lock);

    if (m_Users > 0)
    {
        MY_LOG("%d has created \n", m_Users);
        android_atomic_inc(&m_Users);
        return MTRUE;
    }

    MY_LOG("[%s()] AAO_BUF_SIZE: %d, AFO_BUF_SIZE: %d, FLKO_BUF_SIZE: %d \n", __FUNCTION__, AAO_BUF_SIZE, AFO_BUF_SIZE,FLKO_BUF_SIZE);
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
DefaultBufMgr::
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
DefaultBufMgr::
start()
{
    MY_LOG("[%s()] m_eSensorDev: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_Users);
    if (m_Users <= 0)
    {
        MY_LOG("[%s]Fail : m_Users = %d\n", __FUNCTION__, m_Users);
        return MTRUE;
    }
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
DefaultBufMgr::
stop()
{
    MY_LOG("[%s()] m_eSensorDev: %d, m_Users: %d \n", __FUNCTION__, m_eSensorDev, m_Users);
    if (m_Users <= 0)
    {
        MY_LOG("[%s]Fail : m_Users = %d\n", __FUNCTION__, m_Users);
        return MTRUE;
    }
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
DefaultBufMgr::
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
DefaultBufMgr::
dequeueHwBuf(STT_PORTS port)
{
	  static int i4MagicNumber;
	  ::usleep(33333);
    MY_LOG_IF(m_bDebugEnable,"[%s] m_eSensorDev(%d) + \n", __FUNCTION__, m_eSensorDev);

    char value[PROPERTY_VALUE_MAX] = {'\0'};
    MBOOL bEnable;
    int bufSize;
    switch(port){
      case STT_AAO:
      	bufSize = AAO_BUF_SIZE;
        property_get("aao.dump.enable", value, "0");
        bEnable = atoi(value);
        break;
      case STT_AFO:
      	bufSize = AFO_BUF_SIZE;
        property_get("afo.dump.enable", value, "0");
        bEnable = atoi(value);
        break;
      case STT_FLKO:
      	bufSize = FLKO_BUF_SIZE;
        property_get("flko.dump.enable", value, "0");
        bEnable = atoi(value);
        break;
    }
    
    // create temp buffer data.
    MY_LOG_IF(m_bDebugEnable, "[%s] create buffer(%d) data.\n", __FUNCTION__, bufSize);
    BufInfo* rTempBuf = new BufInfo();
    rTempBuf->mSize = bufSize;
    rTempBuf->mVa = (MUINTPTR)malloc(bufSize + 1);
    rTempBuf->mPortID = i4MagicNumber++;
    memset((void*)rTempBuf->mVa, 1, sizeof(rTempBuf->mSize));
    
    // copy the last HW buffer to SW Buffer.
    MY_LOG_IF(m_bDebugEnable, "[%s] copy the last HW buffer to SW Buffer.\n",__FUNCTION__);
    int index = m_rAAABufIndex[port];
    StatisticBufInfo rHwBuf;
    rHwBuf.mMagicNumber = rTempBuf->FrameBased.mMagicNum_tuning;
    rHwBuf.mSize = rTempBuf->mSize;
    rHwBuf.mVa = rTempBuf->mVa;
    m_rAAABufInfo[port][index].write(rHwBuf);
    MY_LOG_IF(m_bDebugEnable,"[%s] (port,index):(%d,%d), rTempBuf.virtAddr:[0x%x]/phyAddr:[0x%x]/magic number(%d) \n", __FUNCTION__,
              port, index,rTempBuf->mVa,rTempBuf->mPa, rTempBuf->FrameBased.mMagicNum_tuning);
    
    // release temp buffer data.
    MY_LOG_IF(m_bDebugEnable, "[%s] release temp buffer data.\n",__FUNCTION__);
    free((void*)rTempBuf->mVa);
    delete rTempBuf;

    // dump aao/afo/flko data for debug
    if (bEnable) {
    	  MY_LOG_IF(m_bDebugEnable, "[%s] dump aao/afo/flko data for debug.\n",__FUNCTION__);
        char fileName[64];
        switch(port){
          case STT_AAO:
            sprintf(fileName, "/sdcard/aao/aao_temp.raw");
            break;
          case STT_AFO:
            sprintf(fileName, "/sdcard/afo/afo_temp.raw");
            break;
          case STT_FLKO:
            sprintf(fileName, "/sdcard/flko/flko_temp.raw");
            break;
        }
        
        FILE *fp = fopen(fileName, "w");
        if (NULL == fp)
        {
            MY_ERR("fail to open file to save img: %s", fileName);
            MINT32 err;
            switch(port){
              case STT_AAO:
                err = mkdir("/sdcard/aao", S_IRWXU | S_IRWXG | S_IRWXO);
                break;
              case STT_AFO:
                err = mkdir("/sdcard/afo", S_IRWXU | S_IRWXG | S_IRWXO);
                break;
              case STT_FLKO:
                err = mkdir("/sdcard/flko", S_IRWXU | S_IRWXG | S_IRWXO);
                break;
            }
            MY_LOG("err = %d", err);
            return MFALSE;
        }    
        MY_LOG_IF(m_bDebugEnable,"%s\n", fileName);
        fwrite(reinterpret_cast<void *>(rTempBuf->mVa), 1, rTempBuf->mSize, fp);
        fclose(fp);
    }
    MY_LOG_IF(m_bDebugEnable,"[%s] m_eSensorDev(%d) - \n", __FUNCTION__, m_eSensorDev);
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
StatisticBufInfo*
DefaultBufMgr::
dequeueSwBuf(STT_PORTS port)
{
    // switch buffer index.
    int r_index = m_rAAABufIndex[port];
    m_rAAABufIndex[port] = ( r_index + 1 ) % MAX_STATISTIC_BUFFER_CNT;
    MY_LOG_IF(m_bDebugEnable,"[%s] m_eSensorDev(%d), m_rAAABufIndex[%d](%d)\n", __FUNCTION__, m_eSensorDev, port, m_rAAABufIndex[port]);
    //m_rAAABufInfo[port][m_rAAABufIndex[port]].reset();
    
    // the index of read buffer is (current index  + 1) % 2.
    MY_LOG_IF(m_bDebugEnable,"[%s] m_eSensorDev(%d), port(%d), r_index(%d)\n", __FUNCTION__, m_eSensorDev, port, r_index);
    return m_rAAABufInfo[port][r_index].read();
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
DefaultBufMgr::
allocateSwBuf(StatisticBuf &rBufInfo, MUINT32 u4BufSize)
{
    return rBufInfo.allocateBuf(u4BufSize);
}


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL
DefaultBufMgr::
freeSwBuf(StatisticBuf &rBufInfo)
{
    return rBufInfo.freeBuf();
}

