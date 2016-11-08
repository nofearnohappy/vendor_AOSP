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
#define LOG_TAG "afo_buf_mgr"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <cutils/properties.h>
#include <string.h>
#include <aaa_types.h>
#include <aaa_error_code.h>
#include <aaa_log.h>
#include <camera_custom_nvram.h>
#include <ispdrv_mgr.h>
#include <tuning_mgr.h>
#include <linux/cache.h>
#include <utils/threads.h>
#include <list>
#include <isp_tuning.h>

#include <isp_reg.h>
#include <IHalSensor.h>
#include <iopipe/CamIO/INormalPipe.h>
#include "afo_buf_mgr.h"
#if 1//iopipe2.0
#define IOPIPE_SET_MODUL_REG(handle,RegName,Value)
#define IOPIPE_SET_MODUL_REGS(handle, StartRegName, size, ValueArray)
namespace NSImageio {
namespace NSIspio {
enum EModule
{
    //raw
    EModule_OB          = 00,
    EModule_BNR         = 05,
    EModule_LSC         = 10,
    EModule_RPG         = 15,
    EModule_AE          = 20,
    EModule_AWB         = 25,
    EModule_SGG1        = 30,
    EModule_FLK         = 35,
    EModule_AF          = 40,
    EModule_SGG2        = 45,
    EModule_SGG3        = 46,
    EModule_EIS         = 50,
    EModule_LCS         = 55,
    EModule_BPCI        = 60,
    EModule_LSCI        = 65,
    EModule_AAO         = 70,
    EModule_ESFKO       = 75,
    EModule_AFO         = 80,
    EModule_EISO        = 85,
    EModule_LCSO        = 90,
    EModule_iHDR        = 95,
    EModule_CAMSV_IMGO  = 100,
    //raw_d
    EModule_OB_D        = 1000,
    EModule_BNR_D         = 1005,
    EModule_LSC_D       = 1010,
    EModule_RPG_D       = 1015,
    EModule_BPCI_D      = 1020,
    EModule_LSCI_D      = 1025,
    EModule_AE_D        = 1030,
    EModule_AWB_D       = 1035,
    EModule_SGG1_D      = 1040,
    EModule_AF_D        = 1045,
    EModule_LCS_D       = 1050,
    EModule_AAO_D       = 1055,
    EModule_AFO_D       = 1060,
    EModule_LCSO_D      = 1065,
    EModule_iHDR_D      = 1070
};
enum EPIPECmd {

    //  IPECmd_SET_SENSOR_DEV             = 0x1001,
    //  EPIPECmd_SET_SENSOR_GAIN            = 0x1002,
    //  EPIPECmd_SET_SENSOR_EXP             = 0x1003,
    //  EPIPECmd_SET_CAM_MODE               = 0x1004,
        EPIPECmd_SET_SCENE_MODE             = 0x1005,
        EPIPECmd_SET_ISO                    = 0x1006,
        EPIPECmd_SET_FLUORESCENT_CCT        = 0x1007,
        EPIPECmd_SET_SCENE_LIGHT_VALUE      = 0x1008,
        EPIPECmd_VALIDATE_FRAME             = 0x1009,
        EPIPECmd_SET_OPERATION_MODE         = 0x100A,
        EPIPECmd_SET_EFFECT                 = 0x100B,
        EPIPECmd_SET_ZOOM_RATIO             = 0x100C,
        EPIPECmd_SET_BRIGHTNESS             = 0x100D,
        EPIPECmd_SET_CONTRAST               = 0x100E,
        EPIPECmd_SET_EDGE                   = 0x100F,
        EPIPECmd_SET_HUE                    = 0x1010,
        EPIPECmd_SET_SATURATION             = 0x1011,
        EPIPECmd_SEND_TUNING_CMD            = 0x1012,
        EPIPECmd_DECIDE_OFFLINE_CAPTURE     = 0x1013,
        EPIPECmd_LOCK_REG                   = 0x1014,
        EPIPECmd_SET_SHADING_IDX            = 0x1018,

        EPIPECmd_SET_RRZ                    = 0x101A,
        EPIPECmd_SET_P1_UPDATE              = 0x101B,
        EPIPECmd_SET_IMGO                   = 0x101C,

        EPIPECmd_SET_BASE_ADDR              = 0x1102,
        EPIPECmd_SET_CQ_CHANNEL             = 0x1103,
        EPIPECmd_SET_CQ_TRIGGER_MODE        = 0x1104,
        EPIPECmd_AE_SMOOTH                  = 0x1105,
        EPIPECmd_SET_FMT_EN                 = 0x1107,
        EPIPECmd_SET_GDMA_LINK_EN           = 0x1108,
        EPIPECmd_SET_FMT_START              = 0x1109,
        EPIPECmd_SET_CAM_CTL_DBG            = 0x110A,
        EPIPECmd_SET_IMG_PLANE_BY_IMGI      = 0x110B,
        EPIPECmd_SET_P2_QUEUE_CONTROL_STATE = 0x110C,

        EPIPECmd_GET_TG_OUT_SIZE            = 0x110F,
        EPIPECmd_GET_RMX_OUT_SIZE           = 0x1110,
        EPIPECmd_GET_HBIN_INFO              = 0x1111,
        EPIPECmd_GET_EIS_INFO               = 0x1112,
        EPIPECmd_GET_SUGG_BURST_QUEUE_NUM   = 0x1114,

        EPIPECmd_GET_TWIN_INFO              = 0x1116,

        EPIPECmd_SET_EIS_CBFP               = 0X1117,
        EPIPECmd_SET_LCS_CBFP               = 0X1118,
        EPIPECmd_SET_SGG2_CBFP              = 0X1119,

        EPIPECmd_GET_CUR_FRM_STATUS         = 0x111D,
        EPIPECmd_GET_CUR_SOF_IDX            = 0x111E,

        EPIPECmd_ALLOC_UNI                  = 0x1200,
        EPIPECmd_DEALLOC_UNI                = 0x1201,
        //EPIPECmd_ALLOC_FLK_PATH             = 0x1200,
        //EPIPECmd_DEALLOC_FLK_PATH           = 0x1201,
        //EPIPECmd_ALLOC_HDS_PATH             = 0x1202,
        //EPIPECmd_DEALLOC_HDS_PATH           = 0x1203,

        EPIPECmd_SET_NR3D_EN                = 0x1300,
        EPIPECmd_SET_NR3D_DMA_SEL           = 0x1301,
        EPIPECmd_SET_CRZ_EN                 = 0x1302,
        EPIPECmd_SET_JPEG_CFG               = 0x1303,
        EPIPECmd_SET_JPEG_WORKBUF_SIZE      = 0x1304,

        EPIPECmd_SET_MODULE_EN              = 0x1401, //phase out
        EPIPECmd_SET_MODULE_SEL             = 0x1402, //phase out
        EPIPECmd_SET_MODULE_CFG             = 0x1403, //phase out
        EPIPECmd_GET_MODULE_HANDLE          = 0x1404, //phase out
        EPIPECmd_SET_MODULE_CFG_DONE        = 0x1405, //phase out
        EPIPECmd_RELEASE_MODULE_HANDLE      = 0x1406, //phase out
        EPIPECmd_SET_MODULE_DBG_DUMP        = 0x1407, //phase out


        EPIPECmd_GET_SENSOR_PRV_RANGE       = 0x2001,
        EPIPECmd_GET_SENSOR_FULL_RANGE      = 0x2002,
        EPIPECmd_GET_RAW_DUMMY_RANGE        = 0x2003,
        EPIPECmd_GET_SENSOR_NUM             = 0x2004,
        EPIPECmd_GET_SENSOR_TYPE            = 0x2005,
        EPIPECmd_GET_RAW_INFO               = 0x2006,
        EPIPECmd_GET_EXIF_DEBUG_INFO        = 0x2007,
        EPIPECmd_GET_SHADING_IDX            = 0x2008,
        EPIPECmd_GET_ATV_DISP_DELAY         = 0x2009,
        EPIPECmd_GET_SENSOR_DELAY_FRAME_CNT = 0x200A,
        EPIPECmd_GET_CAM_CTL_DBG            = 0x200B,
        EPIPECmd_GET_FMT                    = 0x200C,
        EPIPECmd_GET_GDMA                   = 0x200D,
        EPIPECmd_GET_NR3D_GAIN              = 0x200E,
        EPIPECmd_ISP_RESET                  = 0x4001,
        EPIPECmd_MAX                        = 0xFFFF

};
};
};
#endif

using namespace std;
using namespace android;
using namespace NS3Av3;
using namespace NSIspTuning;
using namespace NSCam;
using namespace NSIoPipe;
using namespace NSCamIOPipe;


typedef list<BufInfo_T> BufInfoList_T;
/*******************************************************************************
*  AFO buffer
*******************************************************************************/
#define AF_HW_WIN 36
#define AF_HW_FLOWIN 3
#define AF_WIN_DATA 8
#define AF_FLOWIN_DATA 12
#define AFO_BUFFER_SIZE (AF_WIN_DATA*AF_HW_WIN+AF_FLOWIN_DATA*AF_HW_FLOWIN)
#define AFO_XSIZE (AFO_BUFFER_SIZE-1)
#define MAX_AFO_BUFFER_CNT (1)

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
class AFOBufMgr
{
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Ctor/Dtor.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    //  Copy constructor is disallowed.
    AFOBufMgr(AFOBufMgr const&);
    //  Copy-assignment operator is disallowed.
    AFOBufMgr& operator=(AFOBufMgr const&);

public:
    AFOBufMgr(ESensorDev_T const eSensorDev);
    ~AFOBufMgr();

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Operations.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    static AFOBufMgr& getInstance(MINT32 const i4SensorDev);
    MBOOL init(MINT32 const i4SensorIdx);
    MBOOL uninit();
    MBOOL debugPrint();
    MBOOL enqueueHwBuf(BufInfo_T& rBufInfo);
    MBOOL dequeueHwBuf(BufInfo_T& rBufInfo);
    MUINT32 getCurrHwBuf();
    MUINT32 getNextHwBuf();
    MBOOL allocateBuf(BufInfo_T &rBufInfo, MUINT32 u4BufSize);
    MBOOL freeBuf(BufInfo_T &rBufInfo);
    MBOOL updateDMABaseAddr(MUINT32 u4BaseAddr);
    MBOOL DMAInit();
    MBOOL DMAUninit();
    MBOOL AFStatEnable(MBOOL En);

    inline MBOOL sendCommandNormalPipe(MINT32 cmd, MINTPTR arg1, MINTPTR arg2, MINTPTR arg3)
    {
        INormalPipe* pPipe = INormalPipe::createInstance(m_i4SensorIdx, "afo_buf_mgr");//iopipe2.0
        MBOOL fgRet = pPipe->sendCommand(cmd, arg1, arg2, arg3);
        pPipe->destroyInstance("afo_buf_mgr");
        return fgRet;
    }
	
    inline MBOOL setTGInfo(MINT32 const i4TGInfo)
    {
        MY_LOG("[%s()]i4TGInfo: %d\n", __FUNCTION__, i4TGInfo);
        switch (i4TGInfo)
        {
            case CAM_TG_1: m_eSensorTG = ESensorTG_1; break;
            case CAM_TG_2: m_eSensorTG = ESensorTG_2; break;
            default:    
                MY_ERR("i4TGInfo= %d", i4TGInfo);
                return MFALSE;
        }
        return MTRUE;
    }

    
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data member
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    ESensorDev_T const     m_eSensorDev;
    ESensorTG_T            m_eSensorTG;
//    IMemDrv*               m_pIMemDrv;
    volatile MINT32        m_Users;
    mutable android::Mutex m_Lock;
    MBOOL                  m_bDebugEnable;
    MINT32                 m_i4SensorIdx;
    INormalPipe*      m_pPipe;//iopipe2.0
    BufInfoList_T          m_rHwBufList;
    BufInfo_T              m_rAFOBufInfo[2];
    MINT32                 m_i4AF_in_Hsize;
    MINT32                 m_i4AF_in_Vsize;
    MINT32                 m_DMAInitDone;
};

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
template <ESensorDev_T const eSensorDev>
class AFOBufMgrDev : public AFOBufMgr
{
public:
    static AFOBufMgr& getInstance()
    {
        static AFOBufMgrDev<eSensorDev> singleton;
        return singleton;
    }

    AFOBufMgrDev(): AFOBufMgr(eSensorDev) {}
    virtual ~AFOBufMgrDev() {}
};


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::init(MINT32 const i4SensorDev, MINT32 const i4SensorIdx)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
        ret_main = AFOBufMgr::getInstance(ESensorDev_Main).init(i4SensorIdx);
    if (i4SensorDev & ESensorDev_Sub)
        ret_sub = AFOBufMgr::getInstance(ESensorDev_Sub).init(i4SensorIdx);
    if (i4SensorDev & ESensorDev_MainSecond)
        ret_main2 = AFOBufMgr::getInstance(ESensorDev_MainSecond).init(i4SensorIdx);    

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::uninit(MINT32 const i4SensorDev)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
       ret_main = AFOBufMgr::getInstance(ESensorDev_Main).uninit();
    if (i4SensorDev & ESensorDev_Sub)
       ret_sub = AFOBufMgr::getInstance(ESensorDev_Sub).uninit();
    if (i4SensorDev & ESensorDev_MainSecond)
       ret_main2 = AFOBufMgr::getInstance(ESensorDev_MainSecond).uninit();    

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::DMAInit(MINT32 const i4SensorDev)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
        ret_main = AFOBufMgr::getInstance(ESensorDev_Main).DMAInit();
    if (i4SensorDev & ESensorDev_Sub)
        ret_sub = AFOBufMgr::getInstance(ESensorDev_Sub).DMAInit();
    if (i4SensorDev & ESensorDev_MainSecond)
        ret_main2 = AFOBufMgr::getInstance(ESensorDev_MainSecond).DMAInit();    

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::DMAUninit(MINT32 const i4SensorDev)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
        ret_main = AFOBufMgr::getInstance(ESensorDev_Main).DMAUninit();
    if (i4SensorDev & ESensorDev_Sub)
        ret_sub = AFOBufMgr::getInstance(ESensorDev_Sub).DMAUninit();
    if (i4SensorDev & ESensorDev_MainSecond)
        ret_main2 = AFOBufMgr::getInstance(ESensorDev_MainSecond).DMAUninit();    

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::AFStatEnable(MINT32 const i4SensorDev, MBOOL En)
{
    MBOOL ret_main, ret_sub, ret_main2;
    ret_main = ret_sub = ret_main2 = MTRUE;

    if (i4SensorDev & ESensorDev_Main)
        ret_main = AFOBufMgr::getInstance(ESensorDev_Main).AFStatEnable(En);
    if (i4SensorDev & ESensorDev_Sub)
        ret_sub = AFOBufMgr::getInstance(ESensorDev_Sub).AFStatEnable(En);
    if (i4SensorDev & ESensorDev_MainSecond)
        ret_main2 = AFOBufMgr::getInstance(ESensorDev_MainSecond).AFStatEnable(En);    

    return ret_main && ret_sub && ret_main2;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::enqueueHwBuf(MINT32 const i4SensorDev, BufInfo_T& rBufInfo)
{
    if (i4SensorDev & ESensorDev_Main)
        return AFOBufMgr::getInstance(ESensorDev_Main).enqueueHwBuf(rBufInfo);
    else if (i4SensorDev & ESensorDev_Sub)
        return AFOBufMgr::getInstance(ESensorDev_Sub).enqueueHwBuf(rBufInfo);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return AFOBufMgr::getInstance(ESensorDev_MainSecond).enqueueHwBuf(rBufInfo);    

    MY_ERR("Incorrect sensor device: i4SensorDev = %d", i4SensorDev);
    return MFALSE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::dequeueHwBuf(MINT32 const i4SensorDev, BufInfo_T& rBufInfo)
{
    if (i4SensorDev & ESensorDev_Main)
        return AFOBufMgr::getInstance(ESensorDev_Main).dequeueHwBuf(rBufInfo);
    else if (i4SensorDev & ESensorDev_Sub)
        return AFOBufMgr::getInstance(ESensorDev_Sub).dequeueHwBuf(rBufInfo);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return AFOBufMgr::getInstance(ESensorDev_MainSecond).dequeueHwBuf(rBufInfo);    

    MY_ERR("Incorrect sensor device: i4SensorDev = %d", i4SensorDev);
    return MFALSE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::updateDMABaseAddr(MINT32 const i4SensorDev)
{
    if (i4SensorDev & ESensorDev_Main)
        return AFOBufMgr::getInstance(ESensorDev_Main).updateDMABaseAddr(AFOBufMgr::getInstance(ESensorDev_Main).getNextHwBuf());
    else if (i4SensorDev & ESensorDev_Sub)
        return AFOBufMgr::getInstance(ESensorDev_Sub).updateDMABaseAddr(AFOBufMgr::getInstance(ESensorDev_Sub).getNextHwBuf());
    else if (i4SensorDev & ESensorDev_MainSecond)
        return AFOBufMgr::getInstance(ESensorDev_MainSecond).updateDMABaseAddr(AFOBufMgr::getInstance(ESensorDev_MainSecond).getNextHwBuf());    

    MY_ERR("Incorrect sensor device: i4SensorDev = %d", i4SensorDev);
    return MFALSE;
}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL IAFOBufMgr::setTGInfo(MINT32 const i4SensorDev, MINT32 const i4TGInfo)
{
    if (i4SensorDev & ESensorDev_Main)
        return AFOBufMgr::getInstance(ESensorDev_Main).setTGInfo(i4TGInfo);
    else if (i4SensorDev & ESensorDev_Sub)
        return AFOBufMgr::getInstance(ESensorDev_Sub).setTGInfo(i4TGInfo);
    else if (i4SensorDev & ESensorDev_MainSecond)
        return AFOBufMgr::getInstance(ESensorDev_MainSecond).setTGInfo(i4TGInfo);    

    MY_ERR("Incorrect sensor device: i4SensorDev = %d", i4SensorDev);
    return MFALSE;
}


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
AFOBufMgr& AFOBufMgr::getInstance(MINT32 const i4SensorDev)
{
    switch (i4SensorDev)
    {
        case ESensorDev_Main: //  Main Sensor
            return  AFOBufMgrDev<ESensorDev_Main>::getInstance();
        case ESensorDev_MainSecond: //  Main Second Sensor
            return  AFOBufMgrDev<ESensorDev_MainSecond>::getInstance();
        case ESensorDev_Sub: //  Sub Sensor
            return  AFOBufMgrDev<ESensorDev_Sub>::getInstance();
        default:
            MY_ERR("i4SensorDev = %d", i4SensorDev);
            return  AFOBufMgrDev<ESensorDev_Main>::getInstance();
    }
}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
AFOBufMgr::AFOBufMgr(ESensorDev_T const eSensorDev)
    : m_eSensorDev(eSensorDev)
    , m_eSensorTG(ESensorTG_None)
//    , m_pIMemDrv(IMemDrv::createInstance())
    , m_Users(0)
    , m_Lock()
{}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
AFOBufMgr::~AFOBufMgr(){}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::init(MINT32 const i4SensorIdx)
{
    MBOOL ret = MTRUE;

    char value[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("debug.afo_buf_mgr.enable", value, "0");
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
    IHalSensorList* const pIHalSensorList = IHalSensorList::get();
    //IHalSensor* pIHalSensor = pIHalSensorList->createSensor("afo_buf_mgr", m_i4SensorIdx);
    //SensorDynamicInfo rSensorDynamicInfo;
    SensorStaticInfo rSensorStaticInfo;
    switch  ( m_eSensorDev )
    {
        case ESensorDev_Main:
            //pIHalSensor->querySensorDynamicInfo(NSCam::SENSOR_DEV_MAIN, &rSensorDynamicInfo);
            pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_MAIN, &rSensorStaticInfo);
            break;
        case ESensorDev_Sub:
            //pIHalSensor->querySensorDynamicInfo(NSCam::SENSOR_DEV_SUB, &rSensorDynamicInfo);
            pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_SUB, &rSensorStaticInfo);
            break;
        case ESensorDev_MainSecond:
            //pIHalSensor->querySensorDynamicInfo(NSCam::SENSOR_DEV_MAIN_2, &rSensorDynamicInfo);
            pIHalSensorList->querySensorStaticInfo(NSCam::SENSOR_DEV_MAIN_2, &rSensorStaticInfo);
            break;
        default:    //  Shouldn't happen.
            MY_ERR("Invalid sensor device: %d", m_eSensorDev);
        return MFALSE;
    }
    m_i4AF_in_Hsize=rSensorStaticInfo.previewWidth;
    m_i4AF_in_Vsize=rSensorStaticInfo.previewHeight;

    //if(pIHalSensor)   pIHalSensor->destroyInstance("afo_buf_mgr");
    MY_LOG("AFO TG = %d, W/H =%d, %d, SensorIdx %d, \n", m_eSensorTG, m_i4AF_in_Hsize, m_i4AF_in_Vsize, m_i4SensorIdx);

    //m_pPipe = INormalPipe::createInstance(m_i4SensorIdx,"afo_buf_mgr");
//    MY_LOG("m_pIMemDrv->init() %d\n", m_pIMemDrv->init());
    m_rHwBufList.clear();
    m_DMAInitDone=0;

    MY_LOG("[AFOBufMgr] allocateBuf \n");
    for(MINT32 i = 0; i < MAX_AFO_BUFFER_CNT; i++) 
    {
        m_rAFOBufInfo[i].useNoncache = 0;
        allocateBuf(m_rAFOBufInfo[i], AFO_BUFFER_SIZE);
        enqueueHwBuf(m_rAFOBufInfo[i]);
    }
    android_atomic_inc(&m_Users);

    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::uninit()
{
    MBOOL ret = MTRUE;
    IMEM_BUF_INFO buf_info;

    MY_LOG("[%s()] - E. m_Users: %d \n", __FUNCTION__, m_Users);

    Mutex::Autolock lock(m_Lock);

    // If no more users, return directly and do nothing.
    if (m_Users <= 0)    return MTRUE;

    // More than one user, so decrease one User.
    android_atomic_dec(&m_Users);

    if (m_Users == 0) // There is no more User after decrease one User
    {
        MY_LOG("[AFOBufMgr] freeBuf\n");
        for (MINT32 i = 0; i < MAX_AFO_BUFFER_CNT; i++) {
            freeBuf(m_rAFOBufInfo[i]);
        }
//        MY_LOG("[AFOBufMgr]m_pIMemDrv uninit\n");
//        m_pIMemDrv->uninit();
        m_DMAInitDone=0;
        //m_pPipe->destroyInstance("afo_buf_mgr");
    }
    else    // There are still some users.
    {
        MY_LOG_IF(m_bDebugEnable,"Still %d users \n", m_Users);
    }

    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::debugPrint()
{
    BufInfoList_T::iterator it;
    for (it = m_rHwBufList.begin(); it != m_rHwBufList.end(); it++ ) 
        MY_LOG("m_rHwBufList.virtAddr:[0x%x]/phyAddr:[0x%x] \n",it->virtAddr,it->phyAddr);

    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::enqueueHwBuf(BufInfo_T& rBufInfo)
{
    MY_LOG_IF(m_bDebugEnable,"AFO %s() m_eSensorDev(%d)\n", __FUNCTION__, m_eSensorDev);
    MY_LOG_IF(m_bDebugEnable,"AFO rBufInfo.virtAddr:[0x%x]/phyAddr:[0x%x] \n",rBufInfo.virtAddr,rBufInfo.phyAddr);
    m_rHwBufList.push_back(rBufInfo);
#if 0	
    m_pIMemDrv->cacheSyncbyRange(IMEM_CACHECTRL_ENUM_INVALID, &rBufInfo);
#endif
    return MTRUE;
}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::dequeueHwBuf(BufInfo_T& rBufInfo)
{
    MY_LOG_IF(m_bDebugEnable,"%s() m_eSensorDev(%d)\n", __FUNCTION__, m_eSensorDev);
    if (m_rHwBufList.size()) 
    {
        rBufInfo = m_rHwBufList.front();
        m_rHwBufList.pop_front();
    }
    MY_LOG_IF(m_bDebugEnable,"rBufInfo.virtAddr:[0x%x]/phyAddr:[0x%x] \n",rBufInfo.virtAddr,rBufInfo.phyAddr);
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MUINT32 AFOBufMgr::getCurrHwBuf()
{
    if (m_rHwBufList.size() > 0) 
    {
        return m_rHwBufList.front().phyAddr;
    }
    else 
    {
        MY_ERR("AFO No free buffer\n");
        return 0;
    }
}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MUINT32 AFOBufMgr::getNextHwBuf()
{
    BufInfoList_T::iterator it;
    if (m_rHwBufList.size() > 1) 
    {
        it = m_rHwBufList.begin();
        it++;
        return it->phyAddr;
    }
    else 
    { // No free buffer
       MY_ERR("AFO No free buffer\n");
       return 0;
    }
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::allocateBuf(BufInfo_T &rBufInfo, MUINT32 u4BufSize)
{
#if 0
    rBufInfo.size = u4BufSize;
    MY_LOG("AFO allocVirtBuf size %d",u4BufSize);
    if (m_pIMemDrv->allocVirtBuf(&rBufInfo)) 
    {
        MY_ERR("m_pIMemDrv->allocVirtBuf() error");
        return MFALSE;
    }
    if (m_pIMemDrv->mapPhyAddr(&rBufInfo)) 
    {
        MY_ERR("m_pIMemDrv->mapPhyAddr() error");
        return MFALSE;
    }
#endif	
    return MTRUE;
}


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::freeBuf(BufInfo_T &rBufInfo)
{
#if 0
    if (m_pIMemDrv->unmapPhyAddr(&rBufInfo))
    {
        MY_ERR("m_pIMemDrv->unmapPhyAddr() error");
        return MFALSE;
    }
    if (m_pIMemDrv->freeVirtBuf(&rBufInfo)) 
    {
        MY_ERR("m_pIMemDrv->freeVirtBuf() error");
        return MFALSE;
    }
#endif	
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::updateDMABaseAddr(MUINT32 u4BaseAddr)
{
    MY_LOG_IF(m_bDebugEnable,"AFO %s() m_eSensorDev(%d) u4BaseAddr=0x%x\n", __FUNCTION__, m_eSensorDev, u4BaseAddr);
    MINT32 i4W = 64;        MINT32 i4H = 64;
    MINT32 i4X = 128;        MINT32 i4Y = 128;        MINT32 wintmp;

    if(!u4BaseAddr) 
    {
        MY_ERR("u4BaseAddr is NULL\n");
        return E_ISPMGR_NULL_ADDRESS;
    }
    MUINTPTR handle;
    MINT32 istwinmode=0;

	if (MFALSE ==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_GET_TWIN_INFO, (MINTPTR)&istwinmode, -1,-1))
        MY_ERR("GET_TWIN_INFO  fail");
    MY_LOG("GET_TWIN_INFO get %d\n", istwinmode);

    if (m_eSensorTG == ESensorTG_1) 
    {
        if (MFALSE ==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE,
                                            NSImageio::NSIspio::EModule_AFO, 
                                            (MINTPTR)&handle, (MINTPTR)(&("AFOBufMgr::DMAConfig()"))))
        {
            MY_ERR("EPIPECmd_GET_MODULE_HANDLE fail");
        }
        else
        {
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_BASE_ADDR, u4BaseAddr);
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_XSIZE, AFO_XSIZE);
            MY_LOG("SET_AFO_CFG_DONE ");
            if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
            {    
                MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
            }
        }
    }
    else  //ESensorTG_2
    {
        if (MFALSE ==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE, 
                                               NSImageio::NSIspio::EModule_AFO_D, 
                                               (MINTPTR)&handle, (MINTPTR)(&("AFOBufMgr::DMAConfig()"))))
        {
            MY_ERR("EPIPECmd_GET_MODULE_HANDLE fail");
        }
        else
        {
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_BASE_ADDR, u4BaseAddr);
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_XSIZE, AFO_XSIZE);
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_OFST_ADDR, 0);
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_YSIZE, 0);
            IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_STRIDE, AFO_XSIZE+1);
            //IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_CON, 0x800A0820);
            //IOPIPE_SET_MODUL_REG(handle, CAM_AFO_D_CON2, 0x00201100);
            MY_LOG("SET_AFO_D_CFG_DONE ");

            if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
            {    
                MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
            }
        }
    }
    if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("AFOBufMgr::DMAConfig()")), MNULL))
    {            
        MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
    }
    if (m_DMAInitDone == 1) return MTRUE;
//=================================================================
    if (m_eSensorTG == ESensorTG_1) 
    {
        if (MFALSE ==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE,
                                            NSImageio::NSIspio::EModule_AF, 
                                            (MINTPTR)&handle, (MINTPTR)(&("AF::DMAConfig()")))
            )
        {
            MY_ERR("EPIPECmd_GET_MODULE_HANDLE AF fail");
        }
        else
        {
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_CON, istwinmode);
            wintmp= i4X  + ((i4X + i4W)<<16) ;
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_WINX_1,  wintmp);
            wintmp=i4X + i4W*2 + ((i4X + i4W*3)<<16) ;
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_WINX_2, wintmp);
            wintmp=(i4X + i4W*4) + ((i4X + i4W*5)<<16) ;
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_WINX_3, wintmp );
            wintmp=  i4Y     + ((i4Y + i4H)<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_WINY_1,  wintmp );
            wintmp= i4Y + i4H*2 + ((i4Y + i4H*3)<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_WINY_2, wintmp );
            wintmp= i4Y + i4H*4 + ((i4Y + i4H*5)<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_WINY_3, wintmp );
            wintmp=i4W + (i4H<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_SIZE, wintmp );
            wintmp=i4X    + (i4Y<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_FLO_WIN_1,  wintmp);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_FLO_WIN_2,  wintmp);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_FLO_WIN_3,  wintmp);
            wintmp=i4W + (i4H<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_FLO_SIZE_1, wintmp );
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_FLO_SIZE_2, wintmp );
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_FLO_SIZE_3, wintmp );
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_IMAGE_SIZE, m_i4AF_in_Hsize);
            MY_LOG("AFO SET_AF_CFG_DONE ");
            if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
            {    
                MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE AF fail");
            }
        }
    }
    else  //ESensorTG_2
    {
        if (MFALSE ==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE,
                                            NSImageio::NSIspio::EModule_AF_D, 
                                            (MINTPTR)&handle, (MINTPTR)(&("AF::DMAConfig()")))
        )
        {
            MY_ERR("EPIPECmd_GET_MODULE_HANDLE AF fail");
        }
        else
        {
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_CON, istwinmode);
            wintmp= i4X  + ((i4X + i4W)<<16) ;
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_WINX_1,  wintmp);
            wintmp=i4X + i4W*2 + ((i4X + i4W*3)<<16) ;
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_WINX_2, wintmp);
            wintmp=(i4X + i4W*4) + ((i4X + i4W*5)<<16) ;
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_WINX_3, wintmp );
            wintmp=  i4Y     + ((i4Y + i4H)<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_WINY_1,  wintmp );
            wintmp= i4Y + i4H*2 + ((i4Y + i4H*3)<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_WINY_2, wintmp );
            wintmp= i4Y + i4H*4 + ((i4Y + i4H*5)<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_WINY_3, wintmp );
            wintmp=i4W + (i4H<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_SIZE, wintmp );
            wintmp=i4X    + (i4Y<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_FLO_WIN_1,  wintmp);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_FLO_WIN_2,  wintmp);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_FLO_WIN_3,  wintmp);
            wintmp=i4W + (i4H<<16);
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_FLO_SIZE_1, wintmp );
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_FLO_SIZE_2, wintmp );
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_FLO_SIZE_3, wintmp );
            IOPIPE_SET_MODUL_REG(handle,  CAM_AF_D_IMAGE_SIZE, m_i4AF_in_Hsize);
            MY_LOG("AFO SET_AF_D_CFG_DONE ");
            if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
            {    
                MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE AF fail");
            }
        }

    
    }
    if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("AF::DMAConfig()")), MNULL))
    {            
        MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE AF fail");
    }
    m_DMAInitDone=1;
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::DMAInit()
{
    MY_LOG_IF(m_bDebugEnable,"AFO %s() m_eSensorDev(%d)\n", __FUNCTION__, m_eSensorDev);
    updateDMABaseAddr(getCurrHwBuf());
    if (m_eSensorTG == ESensorTG_1) 
    {
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_ESFKO, MTRUE, MNULL))
        {               
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_ESFKO");
            return MFALSE;
        }

        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_AFO, MTRUE, MNULL))
        {                 
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_AFO");
            return MFALSE;
        }

    }
    else 
    {
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_AFO_D, MTRUE, MNULL))
        {               
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_AFO");
            return MFALSE;
        }     
    }
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::DMAUninit()
{
    MY_LOG_IF(m_bDebugEnable,"AFO %s() m_eSensorDev(%d)\n", __FUNCTION__, m_eSensorDev);
    if (m_eSensorTG == ESensorTG_1) 
    {
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_ESFKO, MFALSE, MNULL))
        {               
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_ESFKO");
            return MFALSE;
        }

        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_AFO, MFALSE, MNULL))
        {                
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_AF0");
            return MFALSE;
        }
    }
    else 
    {
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_AFO_D, MFALSE, MNULL))
        {                 
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_AFO_D");
            return MFALSE;
        }     
    }
    m_DMAInitDone=0;
    return MTRUE;
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
MBOOL AFOBufMgr::AFStatEnable(MBOOL En)
{
    MY_LOG_IF(m_bDebugEnable,"AFO m_eSensorDev(%d) AFStatEnable(%d)\n",m_eSensorDev, En);
    if (m_eSensorTG == ESensorTG_1) 
    {
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_AF, En, MNULL))
        {                 
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_AF");
            return MFALSE;
        }
          if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_SGG1, En, MNULL))
        {                 
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_SGG1");
            return MFALSE;
        }
    }
    else
    {
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_AF_D, En, MNULL))
        {               
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_AF_D");
            return MFALSE;
        }
        if (MFALSE==sendCommandNormalPipe(NSImageio::NSIspio::EPIPECmd_SET_MODULE_EN, NSImageio::NSIspio::EModule_SGG1_D, En, MNULL))
        {               
            MY_ERR("EPIPECmd_SET_MODULE_En fail: EModule_SGG1_D");
            return MFALSE;
        }
    }
    return MTRUE;
}

