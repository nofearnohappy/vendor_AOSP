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
#define LOG_TAG "isp_mgr_awb_stat"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <cutils/properties.h>
#include <aaa_types.h>
#include <aaa_error_code.h>
#include <aaa_log.h>
#include <drv/isp_reg.h>
#include "isp_mgr.h"

using namespace NSCam;

namespace NSIspTuningv3
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// AWB Statistics Config
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
ISP_MGR_AWB_STAT_CONFIG_T&
ISP_MGR_AWB_STAT_CONFIG_T::
getInstance(ESensorDev_T const eSensorDev)
{
    switch (eSensorDev)
    {
    case ESensorDev_Main: //  Main Sensor
        return  ISP_MGR_AWB_STAT_CONFIG_DEV<ESensorDev_Main>::getInstance();
    case ESensorDev_MainSecond: //  Main Second Sensor
        return  ISP_MGR_AWB_STAT_CONFIG_DEV<ESensorDev_MainSecond>::getInstance();
    case ESensorDev_Sub: //  Sub Sensor
        return  ISP_MGR_AWB_STAT_CONFIG_DEV<ESensorDev_Sub>::getInstance();
    default:
        MY_ERR("eSensorDev = %d", eSensorDev);
        return  ISP_MGR_AWB_STAT_CONFIG_DEV<ESensorDev_Main>::getInstance();
    }
}

MBOOL
ISP_MGR_AWB_STAT_CONFIG_T::
config(AWB_STAT_CONFIG_T& rAWBStatConfig, MBOOL bHBIN2Enable)
{
#if 0 //
    MUINT32 u4AwbMotionThres = 1024;
    addressErrorCheck("Before ISP_MGR_AWB_STAT_CONFIG_T::apply()");

    // CAM_AWB_WIN_ORG
    reinterpret_cast<ISP_CAM_AWB_WIN_ORG_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_ORG))->AWB_W_HORG = rAWBStatConfig.i4WindowOriginX;
    reinterpret_cast<ISP_CAM_AWB_WIN_ORG_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_ORG))->AWB_W_VORG = rAWBStatConfig.i4WindowOriginY;
    // CAM_AWB_WIN_SIZE
    reinterpret_cast<ISP_CAM_AWB_WIN_SIZE_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_SIZE))->AWB_W_HSIZE = rAWBStatConfig.i4WindowSizeX;
    reinterpret_cast<ISP_CAM_AWB_WIN_SIZE_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_SIZE))->AWB_W_VSIZE = rAWBStatConfig.i4WindowSizeY;
    // CAM_AWB_WIN_PIT
    reinterpret_cast<ISP_CAM_AWB_WIN_PIT_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_PIT))->AWB_W_HPIT = rAWBStatConfig.i4WindowPitchX;
    reinterpret_cast<ISP_CAM_AWB_WIN_PIT_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_PIT))->AWB_W_VPIT = rAWBStatConfig.i4WindowPitchY;
    // CAM_AWB_WIN_NUM
    reinterpret_cast<ISP_CAM_AWB_WIN_NUM_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_NUM))->AWB_W_HNUM = rAWBStatConfig.i4WindowNumX;
    reinterpret_cast<ISP_CAM_AWB_WIN_NUM_T*>(REG_INFO_VALUE_PTR(CAM_AWB_WIN_NUM))->AWB_W_VNUM = rAWBStatConfig.i4WindowNumY;
    // CAM_AWB_GAIN1_0
    reinterpret_cast<ISP_CAM_AWB_GAIN1_0_T*>(REG_INFO_VALUE_PTR(CAM_AWB_GAIN1_0))->AWB_GAIN1_R = rAWBStatConfig.i4PreGainR;
    reinterpret_cast<ISP_CAM_AWB_GAIN1_0_T*>(REG_INFO_VALUE_PTR(CAM_AWB_GAIN1_0))->AWB_GAIN1_G = rAWBStatConfig.i4PreGainG;
    // CAM_AWB_GAIN1_1
    reinterpret_cast<ISP_CAM_AWB_GAIN1_1_T*>(REG_INFO_VALUE_PTR(CAM_AWB_GAIN1_1))->AWB_GAIN1_B = rAWBStatConfig.i4PreGainB;
    // CAM_AWB_LMT1_0
    reinterpret_cast<ISP_CAM_AWB_LMT1_0_T*>(REG_INFO_VALUE_PTR(CAM_AWB_LMT1_0))->AWB_LMT1_R = rAWBStatConfig.i4PreGainLimitR;
    reinterpret_cast<ISP_CAM_AWB_LMT1_0_T*>(REG_INFO_VALUE_PTR(CAM_AWB_LMT1_0))->AWB_LMT1_G = rAWBStatConfig.i4PreGainLimitG;
    // CAM_AWB_LMT1_1
    reinterpret_cast<ISP_CAM_AWB_LMT1_1_T*>(REG_INFO_VALUE_PTR(CAM_AWB_LMT1_1))->AWB_LMT1_B = rAWBStatConfig.i4PreGainLimitB;
    // CAM_AWB_LOW_THR
    reinterpret_cast<ISP_CAM_AWB_LOW_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_LOW_THR))->AWB_LOW_THR0 = rAWBStatConfig.i4LowThresholdR;
    reinterpret_cast<ISP_CAM_AWB_LOW_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_LOW_THR))->AWB_LOW_THR1 = rAWBStatConfig.i4LowThresholdG;
    reinterpret_cast<ISP_CAM_AWB_LOW_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_LOW_THR))->AWB_LOW_THR2 = rAWBStatConfig.i4LowThresholdB;
    // CAM_AWB_HI_THR
    reinterpret_cast<ISP_CAM_AWB_HI_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_HI_THR))->AWB_HI_THR0 = rAWBStatConfig.i4HighThresholdR;
    reinterpret_cast<ISP_CAM_AWB_HI_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_HI_THR))->AWB_HI_THR1 = rAWBStatConfig.i4HighThresholdG;
    reinterpret_cast<ISP_CAM_AWB_HI_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_HI_THR))->AWB_HI_THR2 = rAWBStatConfig.i4HighThresholdB;
    // CAM_AWB_PIXEL_CNT0
    reinterpret_cast<ISP_CAM_AWB_PIXEL_CNT0_T*>(REG_INFO_VALUE_PTR(CAM_AWB_PIXEL_CNT0))->AWB_PIXEL_CNT0 =  rAWBStatConfig.i4PixelCountR;
    // CAM_AWB_PIXEL_CNT1
    reinterpret_cast<ISP_CAM_AWB_PIXEL_CNT1_T*>(REG_INFO_VALUE_PTR(CAM_AWB_PIXEL_CNT1))->AWB_PIXEL_CNT1 =  rAWBStatConfig.i4PixelCountG;
    // CAM_AWB_PIXEL_CNT2
    reinterpret_cast<ISP_CAM_AWB_PIXEL_CNT2_T*>(REG_INFO_VALUE_PTR(CAM_AWB_PIXEL_CNT2))->AWB_PIXEL_CNT2 =  rAWBStatConfig.i4PixelCountB;
    // CAM_AWB_ERR_THR
    reinterpret_cast<ISP_CAM_AWB_ERR_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_ERR_THR))->AWB_ERR_THR = rAWBStatConfig.i4ErrorThreshold;
    reinterpret_cast<ISP_CAM_AWB_ERR_THR_T*>(REG_INFO_VALUE_PTR(CAM_AWB_ERR_THR))->AWB_ERR_SFT = rAWBStatConfig.i4ErrorShiftBits;
    
    // CAM_AWB_ROT
    reinterpret_cast<ISP_CAM_AWB_ROT_T*>(REG_INFO_VALUE_PTR(CAM_AWB_ROT))->AWB_C = (rAWBStatConfig.i4Cos >= 0) ? static_cast<MUINT32>(rAWBStatConfig.i4Cos) : static_cast<MUINT32>(1024 + rAWBStatConfig.i4Cos);
    reinterpret_cast<ISP_CAM_AWB_ROT_T*>(REG_INFO_VALUE_PTR(CAM_AWB_ROT))->AWB_S = (rAWBStatConfig.i4Sin >= 0) ? static_cast<MUINT32>(rAWBStatConfig.i4Sin) : static_cast<MUINT32>(1024 + rAWBStatConfig.i4Sin);

    #define AWB_LIGHT_AREA_CFG(TYPE, REG, FIELD, BOUND)\
    if (BOUND >= 0)\
        reinterpret_cast<TYPE*>(REG_INFO_VALUE_PTR(REG))->FIELD = BOUND;\
    else\
        reinterpret_cast<TYPE*>(REG_INFO_VALUE_PTR(REG))->FIELD = (1 << 14) + BOUND;\


    // CAM_AWB_L0
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L0_X_T, CAM_AWB_L0_X, AWB_L0_X_LOW, rAWBStatConfig.i4AWBXY_WINL[0])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L0_X_T, CAM_AWB_L0_X, AWB_L0_X_UP, rAWBStatConfig.i4AWBXY_WINR[0])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L0_Y_T, CAM_AWB_L0_Y, AWB_L0_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[0])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L0_Y_T, CAM_AWB_L0_Y, AWB_L0_Y_UP, rAWBStatConfig.i4AWBXY_WINU[0])

    // CAM_AWB_L1
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L1_X_T, CAM_AWB_L1_X, AWB_L1_X_LOW, rAWBStatConfig.i4AWBXY_WINL[1])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L1_X_T, CAM_AWB_L1_X, AWB_L1_X_UP, rAWBStatConfig.i4AWBXY_WINR[1])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L1_Y_T, CAM_AWB_L1_Y, AWB_L1_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[1])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L1_Y_T, CAM_AWB_L1_Y, AWB_L1_Y_UP, rAWBStatConfig.i4AWBXY_WINU[1])

    // CAM_AWB_L2
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L2_X_T, CAM_AWB_L2_X, AWB_L2_X_LOW, rAWBStatConfig.i4AWBXY_WINL[2])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L2_X_T, CAM_AWB_L2_X, AWB_L2_X_UP, rAWBStatConfig.i4AWBXY_WINR[2])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L2_Y_T, CAM_AWB_L2_Y, AWB_L2_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[2])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L2_Y_T, CAM_AWB_L2_Y, AWB_L2_Y_UP, rAWBStatConfig.i4AWBXY_WINU[2])

    // CAM_AWB_L3
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L3_X_T, CAM_AWB_L3_X, AWB_L3_X_LOW, rAWBStatConfig.i4AWBXY_WINL[3])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L3_X_T, CAM_AWB_L3_X, AWB_L3_X_UP, rAWBStatConfig.i4AWBXY_WINR[3])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L3_Y_T, CAM_AWB_L3_Y, AWB_L3_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[3])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L3_Y_T, CAM_AWB_L3_Y, AWB_L3_Y_UP, rAWBStatConfig.i4AWBXY_WINU[3])

    // CAM_AWB_L4
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L4_X_T, CAM_AWB_L4_X, AWB_L4_X_LOW, rAWBStatConfig.i4AWBXY_WINL[4])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L4_X_T, CAM_AWB_L4_X, AWB_L4_X_UP, rAWBStatConfig.i4AWBXY_WINR[4])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L4_Y_T, CAM_AWB_L4_Y, AWB_L4_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[4])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L4_Y_T, CAM_AWB_L4_Y, AWB_L4_Y_UP, rAWBStatConfig.i4AWBXY_WINU[4])

    // CAM_AWB_L5
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L5_X_T, CAM_AWB_L5_X, AWB_L5_X_LOW, rAWBStatConfig.i4AWBXY_WINL[5])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L5_X_T, CAM_AWB_L5_X, AWB_L5_X_UP, rAWBStatConfig.i4AWBXY_WINR[5])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L5_Y_T, CAM_AWB_L5_Y, AWB_L5_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[5])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L5_Y_T, CAM_AWB_L5_Y, AWB_L5_Y_UP, rAWBStatConfig.i4AWBXY_WINU[5])

    // CAM_AWB_L6
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L6_X_T, CAM_AWB_L6_X, AWB_L6_X_LOW, rAWBStatConfig.i4AWBXY_WINL[6])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L6_X_T, CAM_AWB_L6_X, AWB_L6_X_UP, rAWBStatConfig.i4AWBXY_WINR[6])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L6_Y_T, CAM_AWB_L6_Y, AWB_L6_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[6])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L6_Y_T, CAM_AWB_L6_Y, AWB_L6_Y_UP, rAWBStatConfig.i4AWBXY_WINU[6])

    // CAM_AWB_L7
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L7_X_T, CAM_AWB_L7_X, AWB_L7_X_LOW, rAWBStatConfig.i4AWBXY_WINL[7])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L7_X_T, CAM_AWB_L7_X, AWB_L7_X_UP, rAWBStatConfig.i4AWBXY_WINR[7])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L7_Y_T, CAM_AWB_L7_Y, AWB_L7_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[7])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L7_Y_T, CAM_AWB_L7_Y, AWB_L7_Y_UP, rAWBStatConfig.i4AWBXY_WINU[7])

    // CAM_AWB_L8
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L8_X_T, CAM_AWB_L8_X, AWB_L8_X_LOW, rAWBStatConfig.i4AWBXY_WINL[8])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L8_X_T, CAM_AWB_L8_X, AWB_L8_X_UP, rAWBStatConfig.i4AWBXY_WINR[8])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L8_Y_T, CAM_AWB_L8_Y, AWB_L8_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[8])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L8_Y_T, CAM_AWB_L8_Y, AWB_L8_Y_UP, rAWBStatConfig.i4AWBXY_WINU[8])

    // CAM_AWB_L9
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L9_X_T, CAM_AWB_L9_X, AWB_L9_X_LOW, rAWBStatConfig.i4AWBXY_WINL[9])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L9_X_T, CAM_AWB_L9_X, AWB_L9_X_UP, rAWBStatConfig.i4AWBXY_WINR[9])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L9_Y_T, CAM_AWB_L9_Y, AWB_L9_Y_LOW, rAWBStatConfig.i4AWBXY_WIND[9])
    AWB_LIGHT_AREA_CFG(ISP_CAM_AWB_L9_Y_T, CAM_AWB_L9_Y, AWB_L9_Y_UP, rAWBStatConfig.i4AWBXY_WINU[9])

    //AWB_MOTION_THR
    CAM_REG_AWB_MOTION_THR AwbMotionThr;
    AwbMotionThr.Bits.AWB_MOTION_THR = u4AwbMotionThres;
    REG_INFO_VALUE(CAM_AWB_MOTION_THR) = (MUINT32)AwbMotionThr.Raw;
#else
//addressErrorCheck("Before ISP_MGR_AWB_STAT_CONFIG_T::apply()");
    int BlockNumW = 120;
    int BlockNumH = 90;
    int tgW = 2816;
    int tgH = 2112;
    //int PitchW = tgW/BlockNumW;
    //int PitchH = tgH/BlockNumH;
    // FIXME for development
    int PitchW = (tgW/BlockNumW)/2*2;
    int PitchH = (tgH/BlockNumH)/2*2;
    int SizeW  = (PitchW / 2) * 2;
    int SizeH  = (PitchH / 2) * 2;
    int OriginX = (tgW - PitchW*BlockNumW)/2;
    int OriginY = (tgH - PitchH*BlockNumH)/2;
    MY_LOG("[configAAO_AWB] PitchW/PitchH = %d/%d , SizeW/SizeH = %d/%d , OriginX/OriginY = %d/%d", PitchW,PitchH, SizeW,SizeH,OriginX,OriginY);
    int i4WindowPixelNumR = (SizeW * SizeH) / 4;
    int i4WindowPixelNumG = i4WindowPixelNumR * 2;
    int i4WindowPixelNumB = i4WindowPixelNumR;
    int i4PixelCountR = ((1 << 24) + (i4WindowPixelNumR >> 1)) / i4WindowPixelNumR;
    int i4PixelCountG = ((1 << 24) + (i4WindowPixelNumG >> 1)) / i4WindowPixelNumG;
    int i4PixelCountB = ((1 << 24) + (i4WindowPixelNumB >> 1)) / i4WindowPixelNumB;
    MY_LOG("[configAAO_AWB] i4WindowPixelNumR/G/B = %d/%d/%d , i4PixelCountR/G/B = %d/%d/%d", 
        i4WindowPixelNumR,i4WindowPixelNumG, i4WindowPixelNumB,
        i4PixelCountR,i4PixelCountG,i4PixelCountB);
        
        // CAM_AWB_WIN_ORG
        CAM_REG_AWB_WIN_ORG AwbWinOrg;
        AwbWinOrg.Bits.AWB_W_HORG = OriginX;
        AwbWinOrg.Bits.AWB_W_VORG = OriginY;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_WIN_ORG, (MUINT32)AwbWinOrg.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_WIN_ORG) = (MUINT32)AwbWinOrg.Raw;
        // CAM_AWB_WIN_SIZE
        CAM_REG_AWB_WIN_SIZE AwbWinSize;
        AwbWinSize.Bits.AWB_W_HSIZE = SizeW;
        AwbWinSize.Bits.AWB_W_VSIZE = SizeH;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_WIN_SIZE, (MUINT32)AwbWinSize.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_WIN_SIZE) = (MUINT32)AwbWinSize.Raw;
        // CAM_AWB_WIN_PIT
        CAM_REG_AWB_WIN_PIT AwbWinPit;
        AwbWinPit.Bits.AWB_W_HPIT = PitchW;
        AwbWinPit.Bits.AWB_W_VPIT = PitchH;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_WIN_PIT, (MUINT32)AwbWinPit.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_WIN_PIT) = (MUINT32)AwbWinPit.Raw;
        // CAM_AWB_WIN_NUM
        CAM_REG_AWB_WIN_NUM AwbWinNum;
        AwbWinNum.Bits.AWB_W_HNUM = BlockNumW;
        AwbWinNum.Bits.AWB_W_VNUM = BlockNumH;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_WIN_NUM, (MUINT32)AwbWinNum.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_WIN_NUM) = (MUINT32)AwbWinNum.Raw;
        // CAM_AWB_GAIN1_0
        CAM_REG_AWB_GAIN1_0 AwbGain10;
        AwbGain10.Bits.AWB_GAIN1_R = 0x200;
        AwbGain10.Bits.AWB_GAIN1_G = 0x200;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_GAIN1_0, (MUINT32)AwbGain10.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_GAIN1_0) = (MUINT32)AwbGain10.Raw;
        // CAM_AWB_GAIN1_1
        CAM_REG_AWB_GAIN1_1 AwbGain11;
        AwbGain11.Bits.AWB_GAIN1_B = 0x200;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_GAIN1_1, (MUINT32)AwbGain11.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_GAIN1_1) = (MUINT32)AwbGain11.Raw;
        // CAM_AWB_LMT1_0
        CAM_REG_AWB_LMT1_0 AwbLmt10;
        AwbLmt10.Bits.AWB_LMT1_R = 0xFFF;
        AwbLmt10.Bits.AWB_LMT1_G = 0xFFF;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_LMT1_0, (MUINT32)AwbLmt10.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_LMT1_0) = (MUINT32)AwbLmt10.Raw;
        // CAM_AWB_LMT1_1
        CAM_REG_AWB_LMT1_1 AwbLmt11;
        AwbLmt11.Bits.AWB_LMT1_B = 0xFFF;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_LMT1_1, (MUINT32)AwbLmt11.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_LMT1_1) = (MUINT32)AwbLmt11.Raw;
        // CAM_AWB_LOW_THR
        CAM_REG_AWB_LOW_THR AwbLowThr;
        AwbLowThr.Bits.AWB_LOW_THR0 = 1;
        AwbLowThr.Bits.AWB_LOW_THR1 = 1;
        AwbLowThr.Bits.AWB_LOW_THR2 = 1;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_LOW_THR, (MUINT32)AwbLowThr.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_LOW_THR) = (MUINT32)AwbLowThr.Raw;
        // CAM_AWB_HI_THR
        CAM_REG_AWB_HI_THR AwbHiThr;
        AwbHiThr.Bits.AWB_HI_THR0 = 254;
        AwbHiThr.Bits.AWB_HI_THR1 = 254;
        AwbHiThr.Bits.AWB_HI_THR2 = 254;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_HI_THR, (MUINT32)AwbHiThr.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_HI_THR) = (MUINT32)AwbHiThr.Raw;
        // CAM_AWB_PIXEL_CNT0
        CAM_REG_AWB_PIXEL_CNT0 AwbPixelCnt0;
        AwbPixelCnt0.Bits.AWB_PIXEL_CNT0 = i4PixelCountR;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_PIXEL_CNT0, (MUINT32)AwbPixelCnt0.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_PIXEL_CNT0) = (MUINT32)AwbPixelCnt0.Raw;
        // CAM_AWB_PIXEL_CNT1
        CAM_REG_AWB_PIXEL_CNT1 AwbPixelCnt1;
        AwbPixelCnt1.Bits.AWB_PIXEL_CNT1 = i4PixelCountG;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_PIXEL_CNT1, (MUINT32)AwbPixelCnt1.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_PIXEL_CNT1) = (MUINT32)AwbPixelCnt1.Raw;
        // CAM_AWB_PIXEL_CNT2
        CAM_REG_AWB_PIXEL_CNT2 AwbPixelCnt2;
        AwbPixelCnt2.Bits.AWB_PIXEL_CNT2 = i4PixelCountB;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_PIXEL_CNT2, (MUINT32)AwbPixelCnt2.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_PIXEL_CNT2) = (MUINT32)AwbPixelCnt2.Raw;
        // CAM_AWB_ERR_THR
        CAM_REG_AWB_ERR_THR AwbErrThr;
        AwbErrThr.Bits.AWB_ERR_THR = 20;
        AwbErrThr.Bits.AWB_ERR_SFT =  0;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_ERR_THR, (MUINT32)AwbErrThr.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_ERR_THR) = (MUINT32)AwbErrThr.Raw;
        // CAM_AWB_ROT
        CAM_REG_AWB_ROT AwbRot;
        AwbRot.Bits.AWB_C = 256;
        AwbRot.Bits.AWB_S =   0;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_ROT, (MUINT32)AwbRot.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_ROT) = (MUINT32)AwbRot.Raw;
        // AWB_L0_X
        CAM_REG_AWB_L0_X AwbL0X;
        AwbL0X.Bits.AWB_L0_X_LOW = -250;
        AwbL0X.Bits.AWB_L0_X_UP = -100;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L0_X, (MUINT32)AwbL0X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L0_X) = (MUINT32)AwbL0X.Raw;
        // AWB_L0_Y
        CAM_REG_AWB_L0_Y AwbL0Y;
        AwbL0Y.Bits.AWB_L0_Y_LOW = -600;
        AwbL0Y.Bits.AWB_L0_Y_UP = -361;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L0_Y, (MUINT32)AwbL0Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L0_Y) = (MUINT32)AwbL0Y.Raw;
        // AWB_L1_X
        CAM_REG_AWB_L1_X AwbL1X;
        AwbL1X.Bits.AWB_L1_X_LOW = -782;
        AwbL1X.Bits.AWB_L1_X_UP = -145;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L1_X, (MUINT32)AwbL1X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L1_X) = (MUINT32)AwbL1X.Raw;
        // AWB_L1_Y
        CAM_REG_AWB_L1_Y AwbL1Y;
        AwbL1Y.Bits.AWB_L1_Y_LOW = -408;
        AwbL1Y.Bits.AWB_L1_Y_UP = -310;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L1_Y, (MUINT32)AwbL1Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L1_Y) = (MUINT32)AwbL1Y.Raw;
        // AWB_L2_X
        CAM_REG_AWB_L2_X AwbL2X;
        AwbL2X.Bits.AWB_L2_X_LOW = -782;
        AwbL2X.Bits.AWB_L2_X_UP = -145;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L2_X, (MUINT32)AwbL2X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L2_X) = (MUINT32)AwbL2X.Raw;
        // AWB_L2_Y
        CAM_REG_AWB_L2_Y AwbL2Y;
        AwbL2Y.Bits.AWB_L2_Y_LOW = -515;
        AwbL2Y.Bits.AWB_L2_Y_UP = -408;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L2_Y, (MUINT32)AwbL2Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L2_Y) = (MUINT32)AwbL2Y.Raw;
        
        // AWB_L3_X
        CAM_REG_AWB_L3_X AwbL3X;
        AwbL3X.Bits.AWB_L3_X_LOW = -145;
        AwbL3X.Bits.AWB_L3_X_UP = 18;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L3_X, (MUINT32)AwbL3X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L3_X) = (MUINT32)AwbL3X.Raw;
        // AWB_L3_Y
        CAM_REG_AWB_L3_Y AwbL3Y;
        AwbL3Y.Bits.AWB_L3_Y_LOW = -454;
        AwbL3Y.Bits.AWB_L3_Y_UP = -328;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L3_Y, (MUINT32)AwbL3Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L3_Y) = (MUINT32)AwbL3Y.Raw;
        // AWB_L4_X
        CAM_REG_AWB_L4_X AwbL4X;
        AwbL4X.Bits.AWB_L4_X_LOW = -145;
        AwbL4X.Bits.AWB_L4_X_UP = 23;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L4_X, (MUINT32)AwbL4X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L4_X) = (MUINT32)AwbL4X.Raw;
        // AWB_L4_Y
        CAM_REG_AWB_L4_Y AwbL4Y;
        AwbL4Y.Bits.AWB_L4_Y_LOW = -540;
        AwbL4Y.Bits.AWB_L4_Y_UP = -454;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L4_Y, (MUINT32)AwbL4Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L4_Y) = (MUINT32)AwbL4Y.Raw;
        // AWB_L5_X
        CAM_REG_AWB_L5_X AwbL5X;
        AwbL5X.Bits.AWB_L5_X_LOW = 18;
        AwbL5X.Bits.AWB_L5_X_UP = 199;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L5_X, (MUINT32)AwbL5X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L5_X) = (MUINT32)AwbL5X.Raw;
        // AWB_L5_Y
        CAM_REG_AWB_L5_Y AwbL5Y;
        AwbL5Y.Bits.AWB_L5_Y_LOW = -454;
        AwbL5Y.Bits.AWB_L5_Y_UP = -328;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L5_Y, (MUINT32)AwbL5Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L5_Y) = (MUINT32)AwbL5Y.Raw;
        // AWB_L6_X
        CAM_REG_AWB_L6_X AwbL6X;
        AwbL6X.Bits.AWB_L6_X_LOW = 199;
        AwbL6X.Bits.AWB_L6_X_UP = 529;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L6_X, (MUINT32)AwbL6X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L6_X) = (MUINT32)AwbL6X.Raw;
        // AWB_L6_Y
        CAM_REG_AWB_L6_Y AwbL6Y;
        AwbL6Y.Bits.AWB_L6_Y_LOW = -427;
        AwbL6Y.Bits.AWB_L6_Y_UP = -328;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L6_Y, (MUINT32)AwbL6Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L6_Y) = (MUINT32)AwbL6Y.Raw;
        // AWB_L7_X
        CAM_REG_AWB_L7_X AwbL7X;
        AwbL7X.Bits.AWB_L7_X_LOW = 23;
        AwbL7X.Bits.AWB_L7_X_UP = 199;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L7_X, (MUINT32)AwbL7X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L7_X) = (MUINT32)AwbL7X.Raw;
        // AWB_L7_Y
        CAM_REG_AWB_L7_Y AwbL7Y;
        AwbL7Y.Bits.AWB_L7_Y_LOW = -540;
        AwbL7Y.Bits.AWB_L7_Y_UP = -454;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L7_Y, (MUINT32)AwbL7Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L7_Y) = (MUINT32)AwbL7Y.Raw;
        // AWB_L8_X
        CAM_REG_AWB_L8_X AwbL8X;
        AwbL8X.Bits.AWB_L8_X_LOW = 0;
        AwbL8X.Bits.AWB_L8_X_UP = 0;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L8_X, (MUINT32)AwbL8X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L8_X) = (MUINT32)AwbL8X.Raw;
        // AWB_L8_Y
        CAM_REG_AWB_L8_Y AwbL8Y;
        AwbL8Y.Bits.AWB_L8_Y_LOW = 0;
        AwbL8Y.Bits.AWB_L8_Y_UP = 0;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L8_Y, (MUINT32)AwbL8Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L8_Y) = (MUINT32)AwbL8Y.Raw;
        // AWB_L9_X
        CAM_REG_AWB_L9_X AwbL9X;
        AwbL9X.Bits.AWB_L9_X_LOW = 0;
        AwbL9X.Bits.AWB_L9_X_UP = 0;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L9_X, (MUINT32)AwbL9X.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L9_X) = (MUINT32)AwbL9X.Raw;
        // AWB_L9_Y
        CAM_REG_AWB_L9_Y AwbL9Y;
        AwbL9Y.Bits.AWB_L9_Y_LOW = 0;
        AwbL9Y.Bits.AWB_L9_Y_UP = 0;
        //TUNING_MGR_WRITE_REG_CAM(this->tuningMgr, CAM_AWB_L9_Y, (MUINT32)AwbL9Y.Raw, 0);
        REG_INFO_VALUE(CAM_AWB_L9_Y) = (MUINT32)AwbL9Y.Raw;

        //AWB_SPARE

        //AWB_MOTION_THR
        CAM_REG_AWB_MOTION_THR AwbMotionThr;
        AwbMotionThr.Bits.AWB_MOTION_THR = 1024;
        REG_INFO_VALUE(CAM_AWB_MOTION_THR) = (MUINT32)AwbMotionThr.Raw;
    //}

    //apply();

    //addressErrorCheck("After ISP_MGR_AWB_STAT_CONFIG_T::apply()");

#endif
    return MTRUE;
}

MBOOL
ISP_MGR_AWB_STAT_CONFIG_T::
apply(TuningMgr& rTuning)
{   
    rTuning.updateEngine(eTuningMgrFunc_AA, MTRUE, 0);
    // TOP
    //ISP_MGR_CTL_EN_P1_T::getInstance(m_eSensorDev).setEnable_LSC(fgOnOff);

    // Register setting
    rTuning.tuningMgrWriteRegs(
        static_cast<TUNING_MGR_REG_IO_STRUCT*>(m_pRegInfo),
        m_u4RegInfoNum, 0);

    dumpRegInfo("awb_stat_cfg");
    return MTRUE;
}

#if 0
MBOOL
ISP_MGR_AWB_STAT_CONFIG_T::
apply_TG1()
{
    MY_LOG_IF(IsDebugEnabled(), "%s(): m_eSensorDev = %d, m_i4SensorIndex = %d\n", __FUNCTION__, m_eSensorDev, m_i4SensorIndex);
    dumpRegInfo("AWB_STAT_TG1");

    MUINTPTR handle;

    INormalPipe* pPipe = INormalPipe::createInstance(m_i4SensorIndex,"isp_mgr_awb_stat_tg1");//iopipe2.0

    // get module handle
    if (MFALSE == pPipe->sendCommand(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE, 
                           NSImageio::NSIspio::EModule_AWB, (MINTPTR)&handle, (MINTPTR)(&("isp_mgr_awb_stat_tg1"))))
    {
        //Error Handling
        MY_ERR("EPIPECmd_GET_MODULE_HANDLE fail");
        goto lbExit;
    }

    // set module register 
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_WIN_ORG, m_rIspRegInfo[ERegInfo_CAM_AWB_WIN_ORG].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_WIN_SIZE, m_rIspRegInfo[ERegInfo_CAM_AWB_WIN_SIZE].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_WIN_PIT, m_rIspRegInfo[ERegInfo_CAM_AWB_WIN_PIT].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_WIN_NUM, m_rIspRegInfo[ERegInfo_CAM_AWB_WIN_NUM].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_GAIN1_0, m_rIspRegInfo[ERegInfo_CAM_AWB_GAIN1_0].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_GAIN1_1, m_rIspRegInfo[ERegInfo_CAM_AWB_GAIN1_1].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_LMT1_0, m_rIspRegInfo[ERegInfo_CAM_AWB_LMT1_0].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_LMT1_1, m_rIspRegInfo[ERegInfo_CAM_AWB_LMT1_1].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_LOW_THR, m_rIspRegInfo[ERegInfo_CAM_AWB_LOW_THR].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_HI_THR, m_rIspRegInfo[ERegInfo_CAM_AWB_HI_THR].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_PIXEL_CNT0, m_rIspRegInfo[ERegInfo_CAM_AWB_PIXEL_CNT0].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_PIXEL_CNT1, m_rIspRegInfo[ERegInfo_CAM_AWB_PIXEL_CNT1].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_PIXEL_CNT2, m_rIspRegInfo[ERegInfo_CAM_AWB_PIXEL_CNT2].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_ERR_THR, m_rIspRegInfo[ERegInfo_CAM_AWB_ERR_THR].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_ROT, m_rIspRegInfo[ERegInfo_CAM_AWB_ROT].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L0_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L0_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L0_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L0_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L1_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L1_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L1_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L1_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L2_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L2_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L2_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L2_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L3_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L3_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L3_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L3_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L4_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L4_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L4_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L4_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L5_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L5_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L5_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L5_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L6_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L6_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L6_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L6_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L7_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L7_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L7_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L7_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L8_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L8_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L8_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L8_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L9_X, m_rIspRegInfo[ERegInfo_CAM_AWB_L9_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_L9_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_L9_Y].val);

    // set module config done
    if (MFALSE==pPipe->sendCommand(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
    {    
        //Error Handling
        MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
        goto lbExit;
    }

lbExit:

    // release handle
    if (MFALSE==pPipe->sendCommand(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("isp_mgr_awb_stat_tg1")), MNULL))
    {            
        // Error Handling  
        MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
    }

    pPipe->destroyInstance("isp_mgr_awb_stat_tg1");

    return MTRUE;
}

MBOOL
ISP_MGR_AWB_STAT_CONFIG_T::
apply_TG2()
{
    MY_LOG_IF(IsDebugEnabled(),"%s(): m_eSensorDev = %d, m_i4SensorIndex = %d\n", __FUNCTION__, m_eSensorDev, m_i4SensorIndex);
    dumpRegInfo("AWB_STAT_TG2");
   
    MUINTPTR handle;

    INormalPipe* pPipe = INormalPipe::createInstance(m_i4SensorIndex,"isp_mgr_awb_stat_tg2");//iopipe2.0

    // get module handle
    if (MFALSE == pPipe->sendCommand(NSImageio::NSIspio::EPIPECmd_GET_MODULE_HANDLE, 
                           NSImageio::NSIspio::EModule_AWB_D, (MINTPTR)&handle, (MINTPTR)(&("isp_mgr_awb_stat_tg2"))))
    {
        //Error Handling
        MY_ERR("EPIPECmd_GET_MODULE_HANDLE fail");
        goto lbExit;
    }

    // set module register 
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_WIN_ORG, m_rIspRegInfo[ERegInfo_CAM_AWB_D_WIN_ORG].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_WIN_SIZE, m_rIspRegInfo[ERegInfo_CAM_AWB_D_WIN_SIZE].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_WIN_PIT, m_rIspRegInfo[ERegInfo_CAM_AWB_D_WIN_PIT].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_WIN_NUM, m_rIspRegInfo[ERegInfo_CAM_AWB_D_WIN_NUM].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_GAIN1_0, m_rIspRegInfo[ERegInfo_CAM_AWB_D_GAIN1_0].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_GAIN1_1, m_rIspRegInfo[ERegInfo_CAM_AWB_D_GAIN1_1].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_LMT1_0, m_rIspRegInfo[ERegInfo_CAM_AWB_D_LMT1_0].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_LMT1_1, m_rIspRegInfo[ERegInfo_CAM_AWB_D_LMT1_1].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_LOW_THR, m_rIspRegInfo[ERegInfo_CAM_AWB_D_LOW_THR].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_HI_THR, m_rIspRegInfo[ERegInfo_CAM_AWB_D_HI_THR].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_PIXEL_CNT0, m_rIspRegInfo[ERegInfo_CAM_AWB_D_PIXEL_CNT0].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_PIXEL_CNT1, m_rIspRegInfo[ERegInfo_CAM_AWB_D_PIXEL_CNT1].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_PIXEL_CNT2, m_rIspRegInfo[ERegInfo_CAM_AWB_D_PIXEL_CNT2].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_ERR_THR, m_rIspRegInfo[ERegInfo_CAM_AWB_D_ERR_THR].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_ROT, m_rIspRegInfo[ERegInfo_CAM_AWB_D_ROT].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L0_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L0_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L0_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L0_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L1_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L1_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L1_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L1_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L2_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L2_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L2_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L2_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L3_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L3_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L3_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L3_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L4_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L4_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L4_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L4_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L5_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L5_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L5_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L5_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L6_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L6_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L6_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L6_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L7_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L7_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L7_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L7_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L8_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L8_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L8_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L8_Y].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L9_X, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L9_X].val);
    IOPIPE_SET_MODUL_REG(handle, CAM_AWB_D_L9_Y, m_rIspRegInfo[ERegInfo_CAM_AWB_D_L9_Y].val);

    // set module config done
    if (MFALSE==pPipe->sendCommand(NSImageio::NSIspio::EPIPECmd_SET_MODULE_CFG_DONE, handle, MNULL, MNULL))
    {    
        //Error Handling
        MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
        goto lbExit;
    }

lbExit:

    // release handle
    if (MFALSE==pPipe->sendCommand(NSImageio::NSIspio::EPIPECmd_RELEASE_MODULE_HANDLE, handle, (MINTPTR)(&("isp_mgr_awb_stat_tg2")), MNULL))
    {            
        // Error Handling  
        MY_ERR("EPIPECmd_SET_MODULE_CFG_DONE fail");
    }

    pPipe->destroyInstance("isp_mgr_awb_stat_tg2");

    return MTRUE;
}


MBOOL
ISP_MGR_AWB_STAT_CONFIG_T::
configAAO_AWB(MINT32 i4SensorIndex, AWB_STAT_CONFIG_T& rAWBStatConfig, MINT32 tgW, MINT32 tgH)
{
    
    this->tuningMgr->uninit("AAO_AWB_CFG");
    return MTRUE;
}

#endif


}
