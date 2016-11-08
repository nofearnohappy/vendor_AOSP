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
#define LOG_TAG "isp_mgr_sl2"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <cutils/properties.h>
#include <aaa_types.h>
#include <aaa_error_code.h>
#include <aaa_log.h>
#include <camera_custom_nvram.h>
#include <awb_feature.h>
#include <awb_param.h>
#include <ae_feature.h>
#include <ae_param.h>
#include <isp_drv.h>

#include "isp_mgr.h"
#include <tuning_mgr.h>

#if defined(HAVE_AEE_FEATURE)
#include <aee.h>
#define AEE_ASSERT_SL2(String) \
          do { \
              aee_system_exception( \
                  LOG_TAG, \
                  NULL, \
                  DB_OPT_DEFAULT, \
                  String); \
          } while(0)
#else
#define AEE_ASSERT_SL2(String)
#endif

#define CLAMP(x,min,max)       (((x) > (max)) ? (max) : (((x) < (min)) ? (min) : (x)))

#define SLP_PREC_F_SCAL 256

namespace NSIspTuningv3
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// SL2
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
ISP_MGR_SL2_T&
ISP_MGR_SL2_T::
getInstance(ESensorDev_T const eSensorDev)
{
    switch (eSensorDev)
{
    case ESensorDev_Main: //  Main Sensor
        return  ISP_MGR_SL2_DEV<ESensorDev_Main>::getInstance();
    case ESensorDev_MainSecond: //  Main Second Sensor
        return  ISP_MGR_SL2_DEV<ESensorDev_MainSecond>::getInstance();
    case ESensorDev_Sub: //  Sub Sensor
        return  ISP_MGR_SL2_DEV<ESensorDev_Sub>::getInstance();
    default:
        MY_ERR("eSensorDev = %d", eSensorDev);
        return  ISP_MGR_SL2_DEV<ESensorDev_Main>::getInstance();
    }
}

template <>
ISP_MGR_SL2_T&
ISP_MGR_SL2_T::
put(const CROP_RZ_INFO_T& rCropRz, ISP_NVRAM_SL2_T const& rParam)
{
    //Evalutaing slope

    //Comparing which corner is the longest distance, and use it as R.
    
    //corner1
    int SL2_CENTR_Y = rParam.cen.bits.SL2_CENTR_X;
    int SL2_CENTR_X = rParam.cen.bits.SL2_CENTR_X;
    int NSL2_width = m_NSL2width;
    int NSL2_height = m_NSL2height;
    int y_dist_corner1 = (0 - SL2_CENTR_Y);
    int x_dist_corner1 = (0 - SL2_CENTR_X);
    int CircleEq_corner1;

    if(y_dist_corner1 <0 )
        y_dist_corner1 = -y_dist_corner1;

    if(x_dist_corner1 <0 )
        x_dist_corner1 = -x_dist_corner1;


    if(x_dist_corner1 >= y_dist_corner1)
        CircleEq_corner1 = x_dist_corner1;
    else 
        CircleEq_corner1 = y_dist_corner1;

    int tmp_corner1 = ((x_dist_corner1 + y_dist_corner1)*1448) >> 11;

    if(tmp_corner1 >= CircleEq_corner1)
        CircleEq_corner1 = tmp_corner1;

    //corner 2
    int y_dist_corner2 = (0 - SL2_CENTR_Y);
    int x_dist_corner2 = (NSL2_width - SL2_CENTR_X);
    int CircleEq_corner2;

    if(y_dist_corner2 <0 )
        y_dist_corner2 = -y_dist_corner2;

    if(x_dist_corner2 <0 )
        x_dist_corner2 = -x_dist_corner2;


    if(x_dist_corner2 >= y_dist_corner2)
        CircleEq_corner2 = x_dist_corner2;
    else 
        CircleEq_corner2 = y_dist_corner2;

    int tmp_corner2 = ((x_dist_corner2 + y_dist_corner2)*1448) >> 11;

    if(tmp_corner2 >= CircleEq_corner2)
        CircleEq_corner2 = tmp_corner2;



    //corner 3
    int y_dist_corner3 = (NSL2_height - SL2_CENTR_Y);
    int x_dist_corner3 = (0 - SL2_CENTR_X);
    int CircleEq_corner3;

    if(y_dist_corner3 <0 )
        y_dist_corner3 = -y_dist_corner3;

    if(x_dist_corner3 <0 )
        x_dist_corner3 = -x_dist_corner3;


    if(x_dist_corner3 >= y_dist_corner3)
        CircleEq_corner3 = x_dist_corner3;
    else 
        CircleEq_corner3 = y_dist_corner3;

    int tmp_corner3 = ((x_dist_corner3 + y_dist_corner3)*1448) >> 11;

    if(tmp_corner3 >= CircleEq_corner3)
        CircleEq_corner3 = tmp_corner3;



    //corner4
    int y_dist_corner4 = (NSL2_height - SL2_CENTR_Y);
    int x_dist_corner4 = (NSL2_width - SL2_CENTR_X);
    int CircleEq_corner4;

    if(y_dist_corner4 <0 )
        y_dist_corner4 = -y_dist_corner4;

    if(x_dist_corner4 <0 )
        x_dist_corner4 = -x_dist_corner4;


    if(x_dist_corner4 >= y_dist_corner4)
        CircleEq_corner4 = x_dist_corner4;
    else 
        CircleEq_corner4 = y_dist_corner4;

    int tmp_corner4 = ((x_dist_corner4 + y_dist_corner4)*1448) >> 11;

    if(tmp_corner4 >= CircleEq_corner4)
        CircleEq_corner4 = tmp_corner4;



    int maxR;
    if(CircleEq_corner4 > CircleEq_corner3)
        maxR = CircleEq_corner4;
    else
        maxR = CircleEq_corner3;

    if(CircleEq_corner2 > maxR)
        maxR = CircleEq_corner2;

    if(CircleEq_corner1 > maxR)
        maxR = CircleEq_corner1;
    
    int tempGain0 = rParam.rr_con1.bits.SL2_GAIN_0;
    int tempGain1 = rParam.rr_con1.bits.SL2_GAIN_1;
    int tempGain2 = rParam.gain.bits.SL2_GAIN_2;
    int tempGain3 = rParam.gain.bits.SL2_GAIN_3;
    int tempGain4 = rParam.gain.bits.SL2_GAIN_4;
    int tempR0 = rParam.rr_con0.bits.SL2_R_0;
    int tempR1 = rParam.rr_con0.bits.SL2_R_1;
    int tempR2 = rParam.rr_con1.bits.SL2_R_2;
    int SL2_SLP_1;
    int SL2_SLP_2;
    int SL2_SLP_3;
    int SL2_SLP_4;

    if (rCropRz.fgOnOff)
    {
        if (rCropRz.i4RzWidth == 0 || rCropRz.i4Width == 0)
        {
            MY_ERR("Unexpected crop info width(%d), rz width(%d)", rCropRz.i4Width, rCropRz.i4RzWidth);
            AEE_ASSERT_SL2("Unexpected crop info for SL2");
        }
        MFLOAT fRzRto = (MFLOAT)rCropRz.i4RzWidth / rCropRz.i4Width;
        SL2_CENTR_X = (MFLOAT)(SL2_CENTR_X - rCropRz.i4OfstX)*fRzRto;
        SL2_CENTR_Y = (MFLOAT)(SL2_CENTR_Y - rCropRz.i4OfstY)*fRzRto;
        tempR0 = (MFLOAT)tempR0*fRzRto;
        tempR1 = (MFLOAT)tempR1*fRzRto;
        tempR2 = (MFLOAT)tempR2*fRzRto;
        maxR   = (MFLOAT)maxR*fRzRto;
    }
    SL2_SLP_1  = (int)(((float)(tempGain1 - tempGain0)/(float)tempR0)*SLP_PREC_F_SCAL + 0.5);
    SL2_SLP_2  = (int)(((float)(tempGain2 - tempGain1)/(float)(tempR1 - tempR0))*SLP_PREC_F_SCAL + 0.5);
    SL2_SLP_3  = (int)(((float)(tempGain3 - tempGain2)/(float)(tempR2 - tempR1))*SLP_PREC_F_SCAL + 0.5);
    SL2_SLP_4  = (int)(((float)(tempGain4 - tempGain3)/(float)(maxR -    tempR2))*SLP_PREC_F_SCAL + 0.5);

    DIP_X_REG_SL2_CEN rCen;
    DIP_X_REG_SL2_RR_CON0 rRRCond0;
    DIP_X_REG_SL2_RR_CON1 rRRCond1;
    DIP_X_REG_SL2_GAIN rGain;
    DIP_X_REG_SL2_SLP_CON0 rSlopCon0;
    DIP_X_REG_SL2_SLP_CON1 rSlopCon1;

    rCen.Bits.SL2_CENTR_X = SL2_CENTR_X;
    rCen.Bits.SL2_CENTR_Y = SL2_CENTR_Y;
    rRRCond0.Bits.SL2_R_0 = tempR0;
    rRRCond0.Bits.SL2_R_1 = tempR1;
    rRRCond1.Bits.SL2_R_2 = tempR2;
    rRRCond1.Bits.SL2_GAIN_0 = tempGain0;
    rRRCond1.Bits.SL2_GAIN_1 = tempGain1;
    rGain.Bits.SL2_GAIN_2 = tempGain2;
    rGain.Bits.SL2_GAIN_3 = tempGain3;
    rGain.Bits.SL2_GAIN_4 = tempGain4;
    rSlopCon0.Bits.SL2_SLP_1 = SL2_SLP_1;
    rSlopCon0.Bits.SL2_SLP_2 = SL2_SLP_2;
    rSlopCon1.Bits.SL2_SLP_3 = SL2_SLP_3;
    rSlopCon1.Bits.SL2_SLP_4 = SL2_SLP_4;

    REG_INFO_VALUE(DIP_X_SL2_CEN)       = rCen.Raw;
    REG_INFO_VALUE(DIP_X_SL2_RR_CON0)   = rRRCond0.Raw;
    REG_INFO_VALUE(DIP_X_SL2_RR_CON1)   = rRRCond1.Raw;
    REG_INFO_VALUE(DIP_X_SL2_GAIN)      = rGain.Raw;
    REG_INFO_VALUE(DIP_X_SL2_SLP_CON0)  = rSlopCon0.Raw;
    REG_INFO_VALUE(DIP_X_SL2_SLP_CON1)  = rSlopCon1.Raw;
    REG_INFO_VALUE(DIP_X_SL2_RZ)        = (2048<<16) | 2048;
    REG_INFO_VALUE(DIP_X_SL2_XOFF)      = 0;
    REG_INFO_VALUE(DIP_X_SL2_YOFF)      = 0;

    REG_INFO_VALUE(DIP_X_SL2B_CEN)      = rCen.Raw;
    REG_INFO_VALUE(DIP_X_SL2B_RR_CON0)  = rRRCond0.Raw;
    REG_INFO_VALUE(DIP_X_SL2B_RR_CON1)  = rRRCond1.Raw;
    REG_INFO_VALUE(DIP_X_SL2B_GAIN)     = rGain.Raw;
    REG_INFO_VALUE(DIP_X_SL2B_SLP_CON0) = rSlopCon0.Raw;
    REG_INFO_VALUE(DIP_X_SL2B_SLP_CON1) = rSlopCon1.Raw;
    REG_INFO_VALUE(DIP_X_SL2B_RZ)       = (2048<<16) | 2048;
    REG_INFO_VALUE(DIP_X_SL2B_XOFF)     = 0;
    REG_INFO_VALUE(DIP_X_SL2B_YOFF)     = 0;
    
    REG_INFO_VALUE(DIP_X_SL2C_CEN)      = rCen.Raw;
    REG_INFO_VALUE(DIP_X_SL2C_RR_CON0)  = rRRCond0.Raw;
    REG_INFO_VALUE(DIP_X_SL2C_RR_CON1)  = rRRCond1.Raw;
    REG_INFO_VALUE(DIP_X_SL2C_GAIN)     = rGain.Raw;
    REG_INFO_VALUE(DIP_X_SL2C_SLP_CON0) = rSlopCon0.Raw;
    REG_INFO_VALUE(DIP_X_SL2C_SLP_CON1) = rSlopCon1.Raw;
    REG_INFO_VALUE(DIP_X_SL2C_RZ)       = (2048<<16) | 2048;
    REG_INFO_VALUE(DIP_X_SL2C_XOFF)     = 0;
    REG_INFO_VALUE(DIP_X_SL2C_YOFF)     = 0;
    
    REG_INFO_VALUE(DIP_X_SL2D_CEN)      = rCen.Raw;
    REG_INFO_VALUE(DIP_X_SL2D_RR_CON0)  = rRRCond0.Raw;
    REG_INFO_VALUE(DIP_X_SL2D_RR_CON1)  = rRRCond1.Raw;
    REG_INFO_VALUE(DIP_X_SL2D_GAIN)     = rGain.Raw;
    REG_INFO_VALUE(DIP_X_SL2D_SLP_CON0) = rSlopCon0.Raw;
    REG_INFO_VALUE(DIP_X_SL2D_SLP_CON1) = rSlopCon1.Raw;
    REG_INFO_VALUE(DIP_X_SL2D_RZ)       = (2048<<16) | 2048;
    REG_INFO_VALUE(DIP_X_SL2D_XOFF)     = 0;
    REG_INFO_VALUE(DIP_X_SL2D_YOFF)     = 0;

    return  (*this);
}

template <>
ISP_MGR_SL2_T&
ISP_MGR_SL2_T::
get(ISP_NVRAM_SL2_T& rParam)
{
    GET_REG_INFO(DIP_X_SL2_CEN, cen);
    GET_REG_INFO(DIP_X_SL2_RR_CON0, rr_con0);
    GET_REG_INFO(DIP_X_SL2_RR_CON1, rr_con1);
    GET_REG_INFO(DIP_X_SL2_GAIN, gain);

    return  (*this);
}

#define GET_PROP(prop, init, val)\
{\
    char value[PROPERTY_VALUE_MAX] = {'\0'};\
    property_get(prop, value, (init));\
    (val) = atoi(value);\
}

MBOOL
ISP_MGR_SL2_T::
apply(EIspProfile_T eIspProfile, dip_x_reg_t* pReg)
{
    MBOOL bSL2_EN = isEnable();
    ESoftwareScenario eSwScn = static_cast<ESoftwareScenario>(m_rIspDrvScenario[eIspProfile]);

    MINT32 i4Flg = 0;
    GET_PROP("debug.sl2.en", "31", i4Flg);
    MUINT32 u4SL2_EN = (bSL2_EN&&(i4Flg&0x01)) ? 1 : 0;
    MUINT32 u4SL2b_EN = (bSL2_EN&&(i4Flg&0x02)) ? 1 : 0;
    MUINT32 u4SL2c_EN = (bSL2_EN&&(i4Flg&0x04)) ? 1 : 0;
    MUINT32 u4SL2d_EN = (bSL2_EN&&(i4Flg&0x08)) ? 1 : 0;
    MUINT32 u4SL2e_EN = (bSL2_EN&&(i4Flg&0x10)) ? 1 : 0;

    ISP_MGR_CTL_EN_P2_T::getInstance(m_eSensorDev).setEnable_SL2(bSL2_EN);

    if (eIspProfile == EIspProfile_VFB_PostProc ||
        eIspProfile == EIspProfile_Capture_MultiPass_ANR_1 ||
        eIspProfile == EIspProfile_Capture_MultiPass_ANR_2 ||
        eIspProfile == EIspProfile_VSS_Capture_MultiPass_ANR_1 ||
        eIspProfile == EIspProfile_VSS_Capture_MultiPass_ANR_2 ||
        eIspProfile == EIspProfile_MFB_MultiPass_ANR_1 ||
        eIspProfile == EIspProfile_MFB_MultiPass_ANR_2 ||
        eIspProfile == EIspProfile_MFB_Blending_All_Off_SWNR ||
        eIspProfile == EIspProfile_MFB_Blending_All_Off ||
        eIspProfile == EIspProfile_MFB_PostProc_Mixing_SWNR ||
        eIspProfile == EIspProfile_MFB_PostProc_Mixing )
    {
        u4SL2_EN = 0;
    }

    // TOP
    ISP_WRITE_ENABLE_BITS(pReg, DIP_X_CTL_RGB_EN, SL2_EN, u4SL2_EN);
    ISP_WRITE_ENABLE_BITS(pReg, DIP_X_CTL_YUV_EN, SL2B_EN, u4SL2b_EN);
    ISP_WRITE_ENABLE_BITS(pReg, DIP_X_CTL_YUV_EN, SL2C_EN, u4SL2c_EN);
    ISP_WRITE_ENABLE_BITS(pReg, DIP_X_CTL_YUV_EN, SL2D_EN, u4SL2d_EN);
    ISP_WRITE_ENABLE_BITS(pReg, DIP_X_CTL_YUV_EN, SL2E_EN, u4SL2e_EN);

    // Register setting
    writeRegs(static_cast<RegInfo_T*>(m_pRegInfo), m_u4RegInfoNum, pReg);

    MY_LOG_IF(1/*ENABLE_MY_LOG*/,
        "[%s] bSL2_EN(%d), u4SL2_EN(%d), u4SL2b_EN(%d), u4SL2c_EN(%d), DIP_X_SL2_CEN(0x%08x), DIP_X_SL2_RR_CON0(0x%08x), DIP_X_SL2_RR_CON1(0x%08x), DIP_X_SL2_GAIN(0x%08x)",
        __FUNCTION__, bSL2_EN, u4SL2_EN, u4SL2b_EN, u4SL2c_EN,
        REG_INFO_VALUE(DIP_X_SL2_CEN),
        REG_INFO_VALUE(DIP_X_SL2_RR_CON0),
        REG_INFO_VALUE(DIP_X_SL2_RR_CON1),
        REG_INFO_VALUE(DIP_X_SL2_GAIN));

    dumpRegInfo("SL2");

    return  MTRUE;
}

}
