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
#define LOG_TAG "isp_mgr_af_stat"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <cutils/properties.h>
#include <aaa_types.h>
#include <aaa_error_code.h>
#include <aaa_log.h>
#include <tuning_mgr.h>
#include <isp_mgr_af_stat.h>

namespace NSIspTuningv3
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// AF Statistics Config
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
ISP_MGR_AF_STAT_CONFIG_T&
ISP_MGR_AF_STAT_CONFIG_T::
getInstance(ESensorDev_T const eSensorDev)
{
    switch (eSensorDev)
    {
    case ESensorDev_Main: //  Main Sensor
        return  ISP_MGR_AF_STAT_CONFIG_DEV<ESensorDev_Main>::getInstance();
    case ESensorDev_MainSecond: //  Main Second Sensor
        return  ISP_MGR_AF_STAT_CONFIG_DEV<ESensorDev_MainSecond>::getInstance();
    case ESensorDev_Sub: //  Sub Sensor
        return  ISP_MGR_AF_STAT_CONFIG_DEV<ESensorDev_Sub>::getInstance();
    default:
        MY_ERR("eSensorDev = %d", eSensorDev);
        return  ISP_MGR_AF_STAT_CONFIG_DEV<ESensorDev_Main>::getInstance();
    }
}

MBOOL
ISP_MGR_AF_STAT_CONFIG_T::
config(AF_CONFIG_T &a_sAFConfig)
{
    MY_LOG("[%s] TGSZ:[W]%d,[H]%d, BINSZ:[W]%d,[H]%d\n",
        __FUNCTION__,
        a_sAFConfig.sTG_SZ.i4W,
        a_sAFConfig.sTG_SZ.i4H,
        a_sAFConfig.sBIN_SZ.i4W,
        a_sAFConfig.sBIN_SZ.i4H );

    MUINT32 af_v_gonly   = a_sAFConfig.AF_V_GONLY;
    MUINT32 af_v_avg_lvl = a_sAFConfig.AF_V_AVG_LVL;

    /**************************     CAM_REG_AF_CON     ********************************/
    CAM_REG_AF_CON reg_af_con;
    reg_af_con.Raw = 0x200000; //default value
    reg_af_con.Bits.AF_BLF_EN       = a_sAFConfig.AF_BLF[0]; /* AF_BLF_EN */
    reg_af_con.Bits.AF_BLF_D_LVL    = a_sAFConfig.AF_BLF[1]; /* AF_BLF_D_LVL */
    reg_af_con.Bits.AF_BLF_R_LVL    = a_sAFConfig.AF_BLF[2]; /* AF_BLF_R_LVL */
    reg_af_con.Bits.AF_BLF_VFIR_MUX = a_sAFConfig.AF_BLF[3]; /* AF_BLF_VFIR_MUX*/
    reg_af_con.Bits.AF_H_GONLY      = a_sAFConfig.AF_H_GONLY;
    reg_af_con.Bits.AF_V_GONLY      = af_v_gonly;
    reg_af_con.Bits.AF_V_AVG_LVL    = af_v_avg_lvl;
    MY_LOG("(0x1A004800) : AFCon 0x%x, V_Gonly %, V_AVG_LVL %d",
        reg_af_con.Raw,
        af_v_gonly,
        af_v_avg_lvl);
    /**************************     CAM_REG_AF_SIZE     ********************************/
    CAM_REG_AF_SIZE reg_af_size;
    reg_af_size.Raw =0;
    reg_af_size.Bits.AF_IMAGE_WD = a_sAFConfig.sBIN_SZ.i4W; /*case for no frontal binning*/
    /*************************     Configure ROI setting     *******************************/
    CAM_REG_AF_VLD   reg_af_vld;
    CAM_REG_AF_BLK_0 reg_af_blk_0;
  CAM_REG_AF_BLK_1 reg_af_blk_1;
    {
        // Convert ROI coordinate from TG coordinate to BIN block coordinate.
        AREA_T Roi2HWCoord = { 0, 0, 0, 0, 0};
        Roi2HWCoord.i4X = a_sAFConfig.sRoi.i4X * a_sAFConfig.sBIN_SZ.i4W / a_sAFConfig.sTG_SZ.i4W;
        Roi2HWCoord.i4Y = a_sAFConfig.sRoi.i4Y * a_sAFConfig.sBIN_SZ.i4H / a_sAFConfig.sTG_SZ.i4H;
        Roi2HWCoord.i4W = a_sAFConfig.sRoi.i4W * a_sAFConfig.sBIN_SZ.i4W / a_sAFConfig.sTG_SZ.i4W;
        Roi2HWCoord.i4H = a_sAFConfig.sRoi.i4H * a_sAFConfig.sBIN_SZ.i4H / a_sAFConfig.sTG_SZ.i4H;
        MUINT32 start_x = Roi2HWCoord.i4X;
        MUINT32 start_y = Roi2HWCoord.i4Y;
        MY_LOG("AF ROI : [X]%d [Y]%d [W]%d [H]%d -> [X]%d [Y]%d [W]%d [H]%d",
            a_sAFConfig.sRoi.i4X,
            a_sAFConfig.sRoi.i4Y,
            a_sAFConfig.sRoi.i4W,
            a_sAFConfig.sRoi.i4H,
            Roi2HWCoord.i4X,
            Roi2HWCoord.i4Y,
            Roi2HWCoord.i4W,
            Roi2HWCoord.i4H
        );
        /**************************     CAM_REG_AF_VLD     ********************************/
      //ofset
        reg_af_vld.Raw =0;
      reg_af_vld.Bits.AF_VLD_XSTART = start_x;
      reg_af_vld.Bits.AF_VLD_YSTART = start_y;
        /**************************     CAM_REG_AF_BLK_0     ******************************/
        reg_af_blk_0.Raw =0;
        {
            MUINT32 win_h_size, win_v_size;
          MUINT32 h_size       = a_sAFConfig.sBIN_SZ.i4W;
          MUINT32 v_size       = a_sAFConfig.sBIN_SZ.i4H;

            //-------------
            // AF block width
            //-------------
          win_h_size = (h_size-start_x) / a_sAFConfig.AF_BLK_XNUM;
          if (win_h_size > 254)
            {
            win_h_size = 254;
          }
          else
            {
                //min constraint
            if((af_v_avg_lvl == 3) && (af_v_gonly == 1))
                {
              win_h_size = (win_h_size < 32)? (32):(win_h_size);
            }
            else if((af_v_avg_lvl == 3) && (af_v_gonly == 0))
                {
              win_h_size = (win_h_size < 16)? (16):(win_h_size);
            }
            else if((af_v_avg_lvl == 2) && (af_v_gonly == 1))
                {
              win_h_size = (win_h_size < 16)? (16):(win_h_size);
            }
            else
                {
              win_h_size = (win_h_size < 8)? (8):(win_h_size);
            }
          }
          if (af_v_gonly == 1)
            win_h_size = win_h_size/4 * 4;
          else
            win_h_size = win_h_size/2 * 2;

            //-------------
            // AF block height
            //-------------
          win_v_size = (v_size-start_y) / a_sAFConfig.AF_BLK_YNUM;
          if (win_v_size > 255)
            win_v_size = 255;
          else
            win_v_size = (win_v_size < 1)? (1):(win_v_size);

          reg_af_blk_0.Bits.AF_BLK_XSIZE = win_h_size;
          reg_af_blk_0.Bits.AF_BLK_YSIZE = win_v_size;
            MY_LOG("(0x1A004838) : AFSZ [W]%d [H]%d", win_h_size, win_v_size);
        }
        /**************************     CAM_REG_AF_BLK_1     ******************************/
        //window num
        reg_af_blk_1.Raw =0;
        reg_af_blk_1.Bits.AF_BLK_XNUM = a_sAFConfig.AF_BLK_XNUM;
        reg_af_blk_1.Bits.AF_BLK_YNUM = a_sAFConfig.AF_BLK_YNUM;
    }
    /**************************     CAM_REG_AF_TH_0     ******************************/
    CAM_REG_AF_TH_0 reg_af_th_0;
    reg_af_th_0.Raw =0;
    reg_af_th_0.Bits.AF_H_TH_0 = a_sAFConfig.AF_TH_H[0];
    reg_af_th_0.Bits.AF_H_TH_1 = a_sAFConfig.AF_TH_H[1];
    /**************************     CAM_REG_AF_TH_1     ******************************/
    CAM_REG_AF_TH_1 reg_af_th_1;
    reg_af_th_1.Raw =0;
    reg_af_th_1.Bits.AF_V_TH     = a_sAFConfig.AF_TH_V;
    reg_af_th_1.Bits.AF_G_SAT_TH = a_sAFConfig.AF_TH_G_SAT;
    /**************************     CAM_REG_AF_FLT_1     ******************************/
    CAM_REG_AF_FLT_1 reg_af_flt_1;
    reg_af_flt_1.Raw =0;
    reg_af_flt_1.Bits.AF_HFLT0_P1 = a_sAFConfig.AF_FIL_H0[0];
    reg_af_flt_1.Bits.AF_HFLT0_P2 = a_sAFConfig.AF_FIL_H0[1];
    reg_af_flt_1.Bits.AF_HFLT0_P3 = a_sAFConfig.AF_FIL_H0[2];
    reg_af_flt_1.Bits.AF_HFLT0_P4 = a_sAFConfig.AF_FIL_H0[3];
    /**************************     CAM_REG_AF_FLT_2     ******************************/
    CAM_REG_AF_FLT_2 reg_af_flt_2;
    reg_af_flt_2.Raw =0;
    reg_af_flt_2.Bits.AF_HFLT0_P5 = a_sAFConfig.AF_FIL_H0[4];
    reg_af_flt_2.Bits.AF_HFLT0_P6 = a_sAFConfig.AF_FIL_H0[5];
    reg_af_flt_2.Bits.AF_HFLT0_P7 = a_sAFConfig.AF_FIL_H0[6];
    reg_af_flt_2.Bits.AF_HFLT0_P8 = a_sAFConfig.AF_FIL_H0[7];
    /**************************     CAM_REG_AF_FLT_3     ******************************/
    CAM_REG_AF_FLT_3 reg_af_flt_3;
    reg_af_flt_3.Raw =0;
    reg_af_flt_3.Bits.AF_HFLT0_P9  = a_sAFConfig.AF_FIL_H0[ 8];
    reg_af_flt_3.Bits.AF_HFLT0_P10 = a_sAFConfig.AF_FIL_H0[ 9];
    reg_af_flt_3.Bits.AF_HFLT0_P11 = a_sAFConfig.AF_FIL_H0[10];
    reg_af_flt_3.Bits.AF_HFLT0_P12 = a_sAFConfig.AF_FIL_H0[11];
    /**************************     CAM_REG_AF_FLT_4     ******************************/
    CAM_REG_AF_FLT_4 reg_af_flt_4;
    reg_af_flt_4.Raw =0;
    reg_af_flt_4.Bits.AF_HFLT1_P1 = a_sAFConfig.AF_FIL_H1[0];
    reg_af_flt_4.Bits.AF_HFLT1_P2 = a_sAFConfig.AF_FIL_H1[1];
    reg_af_flt_4.Bits.AF_HFLT1_P3 = a_sAFConfig.AF_FIL_H1[2];
    reg_af_flt_4.Bits.AF_HFLT1_P4 = a_sAFConfig.AF_FIL_H1[3];
    /**************************     CAM_REG_AF_FLT_5     ******************************/
    CAM_REG_AF_FLT_5 reg_af_flt_5;
    reg_af_flt_5.Raw =0;
    reg_af_flt_5.Bits.AF_HFLT1_P5 = a_sAFConfig.AF_FIL_H1[4];
    reg_af_flt_5.Bits.AF_HFLT1_P6 = a_sAFConfig.AF_FIL_H1[5];
    reg_af_flt_5.Bits.AF_HFLT1_P7 = a_sAFConfig.AF_FIL_H1[6];
    reg_af_flt_5.Bits.AF_HFLT1_P8 = a_sAFConfig.AF_FIL_H1[7];
    /**************************     CAM_REG_AF_FLT_6     ******************************/
    CAM_REG_AF_FLT_6 reg_af_flt_6;
    reg_af_flt_6.Raw =0;
    reg_af_flt_6.Bits.AF_HFLT1_P9  = a_sAFConfig.AF_FIL_H1[ 8];
    reg_af_flt_6.Bits.AF_HFLT1_P10 = a_sAFConfig.AF_FIL_H1[ 9];
    reg_af_flt_6.Bits.AF_HFLT1_P11 = a_sAFConfig.AF_FIL_H1[10];
    reg_af_flt_6.Bits.AF_HFLT1_P12 = a_sAFConfig.AF_FIL_H1[11];
    /**************************     CAM_REG_AF_FLT_7     ******************************/
    CAM_REG_AF_FLT_7 reg_af_flt_7;
    reg_af_flt_7.Raw =0;
    reg_af_flt_7.Bits.AF_VFLT_X0 = a_sAFConfig.AF_FIL_V[0];
    reg_af_flt_7.Bits.AF_VFLT_X1 = a_sAFConfig.AF_FIL_V[1];
    /**************************     CAM_REG_AF_FLT_8     ******************************/
    CAM_REG_AF_FLT_8 reg_af_flt_8;
    reg_af_flt_8.Raw =0;
    reg_af_flt_8.Bits.AF_VFLT_X2 = a_sAFConfig.AF_FIL_V[2];
    reg_af_flt_8.Bits.AF_VFLT_X3 = a_sAFConfig.AF_FIL_V[3];
    /**************************     CAM_REG_SGG1_PGN     ******************************/
    CAM_REG_SGG1_PGN reg_sgg1_pgn;
    reg_sgg1_pgn.Raw =0;
    reg_sgg1_pgn.Bits.SGG_GAIN = a_sAFConfig.i4SGG_GAIN;
    /**************************     CAM_REG_SGG1_GMRC_1     ******************************/
    CAM_REG_SGG1_GMRC_1 reg_sgg1_gmrc_1;
    reg_sgg1_gmrc_1.Raw =0;
    reg_sgg1_gmrc_1.Bits.SGG_GMR_1 = a_sAFConfig.i4SGG_GMR1;
    reg_sgg1_gmrc_1.Bits.SGG_GMR_2 = a_sAFConfig.i4SGG_GMR2;
    reg_sgg1_gmrc_1.Bits.SGG_GMR_3 = a_sAFConfig.i4SGG_GMR3;
    reg_sgg1_gmrc_1.Bits.SGG_GMR_4 = a_sAFConfig.i4SGG_GMR4;
    /**************************     CAM_REG_SGG1_GMRC_2     ******************************/
    CAM_REG_SGG1_GMRC_2 reg_sgg1_gmrc_2;
    reg_sgg1_gmrc_2.Raw =0;
    reg_sgg1_gmrc_2.Bits.SGG_GMR_5 = a_sAFConfig.i4SGG_GMR5;
    reg_sgg1_gmrc_2.Bits.SGG_GMR_6 = a_sAFConfig.i4SGG_GMR6;
    reg_sgg1_gmrc_2.Bits.SGG_GMR_7 = a_sAFConfig.i4SGG_GMR7;

  //xsize/ysize
  MUINT32 xsize = a_sAFConfig.AF_BLK_XNUM*16;
  MUINT32 ysize = a_sAFConfig.AF_BLK_YNUM;
  REG_INFO_VALUE(CAM_AFO_XSIZE)   = xsize;
  REG_INFO_VALUE(CAM_AFO_YSIZE)   = ysize;
  //REG_INFO_VALUE(CAM_AFO_STRIDE) = xsize+1;
    REG_INFO_VALUE(CAM_AF_SIZE)     = reg_af_size.Raw;
  REG_INFO_VALUE(CAM_AF_CON)      = reg_af_con.Raw;
    REG_INFO_VALUE(CAM_AF_VLD)      = reg_af_vld.Raw;
  REG_INFO_VALUE(CAM_AF_BLK_0)    = reg_af_blk_0.Raw;
    REG_INFO_VALUE(CAM_AF_BLK_1)    = reg_af_blk_1.Raw;
  REG_INFO_VALUE(CAM_AF_TH_0)     = reg_af_th_0.Raw;
  REG_INFO_VALUE(CAM_AF_TH_1)     = reg_af_th_1.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_1)    = reg_af_flt_1.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_2)    = reg_af_flt_2.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_3)    = reg_af_flt_3.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_4)    = reg_af_flt_4.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_5)    = reg_af_flt_5.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_6)    = reg_af_flt_6.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_7)    = reg_af_flt_7.Raw;
  REG_INFO_VALUE(CAM_AF_FLT_8)    = reg_af_flt_8.Raw;
  REG_INFO_VALUE(CAM_SGG1_PGN)    = reg_sgg1_pgn.Raw;
  REG_INFO_VALUE(CAM_SGG1_GMRC_1) = reg_sgg1_gmrc_1.Raw;
  REG_INFO_VALUE(CAM_SGG1_GMRC_2) = reg_sgg1_gmrc_2.Raw;

    return MTRUE;
}

MBOOL
ISP_MGR_AF_STAT_CONFIG_T::
apply(TuningMgr& rTuning)
{
    rTuning.updateEngine(eTuningMgrFunc_AF, MTRUE, 0);
    rTuning.updateEngine(eTuningMgrFunc_SGG1, MTRUE, 0);

    // Register setting
    rTuning.tuningMgrWriteRegs(
        static_cast<TUNING_MGR_REG_IO_STRUCT*>(m_pRegInfo),
        m_u4RegInfoNum, 0);

    dumpRegInfo("af_stat_cfg");

    return MTRUE;
}
}
