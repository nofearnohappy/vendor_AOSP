/*----------------------------------------------------------------------------*
 * Copyright Statement:                                                       *
 *                                                                            *
 *   This software/firmware and related documentation ("MediaTek Software")   *
 * are protected under international and related jurisdictions'copyright laws *
 * as unpublished works. The information contained herein is confidential and *
 * proprietary to MediaTek Inc. Without the prior written permission of       *
 * MediaTek Inc., any reproduction, modification, use or disclosure of        *
 * MediaTek Software, and information contained herein, in whole or in part,  *
 * shall be strictly prohibited.                                              *
 * MediaTek Inc. Copyright (C) 2010. All rights reserved.                     *
 *                                                                            *
 *   BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND     *
 * AGREES TO THE FOLLOWING:                                                   *
 *                                                                            *
 *   1)Any and all intellectual property rights (including without            *
 * limitation, patent, copyright, and trade secrets) in and to this           *
 * Software/firmware and related documentation ("MediaTek Software") shall    *
 * remain the exclusive property of MediaTek Inc. Any and all intellectual    *
 * property rights (including without limitation, patent, copyright, and      *
 * trade secrets) in and to any modifications and derivatives to MediaTek     *
 * Software, whoever made, shall also remain the exclusive property of        *
 * MediaTek Inc.  Nothing herein shall be construed as any transfer of any    *
 * title to any intellectual property right in MediaTek Software to Receiver. *
 *                                                                            *
 *   2)This MediaTek Software Receiver received from MediaTek Inc. and/or its *
 * representatives is provided to Receiver on an "AS IS" basis only.          *
 * MediaTek Inc. expressly disclaims all warranties, expressed or implied,    *
 * including but not limited to any implied warranties of merchantability,    *
 * non-infringement and fitness for a particular purpose and any warranties   *
 * arising out of course of performance, course of dealing or usage of trade. *
 * MediaTek Inc. does not provide any warranty whatsoever with respect to the *
 * software of any third party which may be used by, incorporated in, or      *
 * supplied with the MediaTek Software, and Receiver agrees to look only to   *
 * such third parties for any warranty claim relating thereto.  Receiver      *
 * expressly acknowledges that it is Receiver's sole responsibility to obtain *
 * from any third party all proper licenses contained in or delivered with    *
 * MediaTek Software.  MediaTek is not responsible for any MediaTek Software  *
 * releases made to Receiver's specifications or to conform to a particular   *
 * standard or open forum.                                                    *
 *                                                                            *
 *   3)Receiver further acknowledge that Receiver may, either presently       *
 * and/or in the future, instruct MediaTek Inc. to assist it in the           *
 * development and the implementation, in accordance with Receiver's designs, *
 * of certain softwares relating to Receiver's product(s) (the "Services").   *
 * Except as may be otherwise agreed to in writing, no warranties of any      *
 * kind, whether express or implied, are given by MediaTek Inc. with respect  *
 * to the Services provided, and the Services are provided on an "AS IS"      *
 * basis. Receiver further acknowledges that the Services may contain errors  *
 * that testing is important and it is solely responsible for fully testing   *
 * the Services and/or derivatives thereof before they are used, sublicensed  *
 * or distributed. Should there be any third party action brought against     *
 * MediaTek Inc. arising out of or relating to the Services, Receiver agree   *
 * to fully indemnify and hold MediaTek Inc. harmless.  If the parties        *
 * mutually agree to enter into or continue a business relationship or other  *
 * arrangement, the terms and conditions set forth herein shall remain        *
 * effective and, unless explicitly stated otherwise, shall prevail in the    *
 * event of a conflict in the terms in any agreements entered into between    *
 * the parties.                                                               *
 *                                                                            *
 *   4)Receiver's sole and exclusive remedy and MediaTek Inc.'s entire and    *
 * cumulative liability with respect to MediaTek Software released hereunder  *
 * will be, at MediaTek Inc.'s sole discretion, to replace or revise the      *
 * MediaTek Software at issue.                                                *
 *                                                                            *
 *   5)The transaction contemplated hereunder shall be construed in           *
 * accordance with the laws of Singapore, excluding its conflict of laws      *
 * principles.  Any disputes, controversies or claims arising thereof and     *
 * related thereto shall be settled via arbitration in Singapore, under the   *
 * then current rules of the International Chamber of Commerce (ICC).  The    *
 * arbitration shall be conducted in English. The awards of the arbitration   *
 * shall be final and binding upon both parties and shall be entered and      *
 * enforceable in any court of competent jurisdiction.                        *
 *---------------------------------------------------------------------------*/
/*-----------------------------------------------------------------------------
 *
 * $Author: jc.wu $
 * $Date: 2012/6/5 $
 * $RCSfile: pi_calibration_api.c,v $
 * $Revision: #5 $
 *
 *---------------------------------------------------------------------------*/

/** @file pi_calibration_api.c
 *  Basic DRAMC calibration API implementation
 */

//-----------------------------------------------------------------------------
// Include files
//-----------------------------------------------------------------------------
#include "dramc_common.h"
#include "dramc_register.h"
#include "dramc_pi_api.h"

#define fcWL_ALL   // Do Write Leveling with all DQS together
//#define fcFOR_DQSGDUALP_ENABLE   // Dual-phase DQS clock gating control enabling (new gating, not define as MT6589)

//-----------------------------------------------------------------------------
// Global variables
//-----------------------------------------------------------------------------
#if 1 //add by KT, following variables are defined in ett_cust.hw_dqsi_gw.c
static U32 dqs_gw[DQS_GW_LEN];
static U8 dqs_gw_coarse, dqs_gw_fine, dqs_gw_fine_cnt;
static U8 opt_gw_coarse_value, opt_gw_fine_value;
U8 CurrentRank = 0;
U8 opt_gw_coarse_value_R0[2], opt_gw_fine_value_R0[2];
U8 opt_gw_coarse_value_R1[2], opt_gw_fine_value_R1[2];
static U8 dqs_gw_cnt_break;
#endif
static U8 fgwrlevel_done[2] = {0,0};
U8 RXPERBIT_LOG_PRINT = 1;
static S8 wrlevel_dqs_final_delay[2][DQS_NUMBER];
static U16 u2rx_window_sum;

#ifdef WL_CLKADJUST
static S8 CATrain_ClkDelay[2];
#endif
static U8 ucDLESetting;

#ifdef EYE_SCAN

typedef struct _EYESCAN_WIN
{
	U8 ucsetup_pass_number;
	U8 uchold_pass_number;
	#ifdef ACDIO_TEST	
	U8 window_number;
	#endif	
} EYESCAN_WIN;

EYESCAN_WIN EyeScanWin[32];

#endif


//U8 ucswap_table[2][32] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 , 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 
//                                            0, 1, 2, 3, 8, 9, 10, 11, 4, 5, 6, 7, 12, 13, 14, 15, 16, 17, 18, 19, 24, 25, 26, 27, 20, 21, 22, 23, 28, 29, 30, 31};

//-------------------------------------------------------------------------
/** DramcPllPhaseCal
 *  start PLL Phase Calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  apply           (U8): 0 don't apply the register we set  1 apply the register we set ,default don't apply.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
#ifdef fcNEW_PLL_PHASE_CALIB
DRAM_STATUS_T DramcPllPhaseCal(DRAMC_CTX_T *p)
{
// MEMPLL's are different for A60808 and MT6595
    U8 ucstatus = 0;
    U16 one_count = 0, zero_count = 0;
    U8 pll1st_done = 0, pll2nd_done = 0, pll3rd_done = 0, pll4th_done = 0;
    U8 pll1st_dl = 0, pll2nd_dl = 0, pll3rd_dl = 0, pll4th_dl = 0;
    U8 pll1st_phase=0, pll2nd_phase=0, pll3rd_phase=0, pll4th_phase=0;
    S8 ret = 0;
    U32 u4value;
    
    // for PoP, use MEMPLL 2,4 and MEMPLL05 2, 3
    // for SBS, use MEMPLL 2,3,4 (no SBS for Rome)

    if (p->package == PACKAGE_SBS)
    {
        return DRAM_OK;
    }

    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] ===== PLL Phase Calibration:CHANNEL %d (0: CHA, 1: CHB) =====\n", p->channel));

    // 1. Set jitter meter clock to internal FB path    
    // MEMPLL*_FB_MCK_SEL = 0;
    // Not necessary for A60808
    // It has no phase difference when internal or external loop

    // 2. Set jitter meter count number    
    // JMTRCNT = 0x400;    // JMTRCNT:0x74[30:16]. Set jitter meter count number to 1024
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);
    
    // MEMPLL 4 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);

    // POP(DDRPHY05) MEMPLL 2 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x18c), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x18c), u4value);

    // POP(DDRPHY05) MEMPLL 3 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x190), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x190), u4value);

    while(1)
    {         
            // POP case
            if (p->channel == CHANNEL_B)
            {
            	// POP05 only exists in channel A. No need to do the phase calibration in channel B.
            	pll3rd_done = pll4th_done = 1;
            }
            
            if (!pll1st_done)    // MEMPLL 2
            {
                if (pll1st_phase == 0) // initial phase set to 0 for REF and FBK
                {
                    // RG_MEPLL2_REF_DL:0x618[12:8]. MEMPLL2 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);

                    // RG_MEPLL2_FBK_DL:0x618[4:0]. MEMPLL2 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);                    
                }
                else if (pll1st_phase == 1) // REF lag FBK, delay FBK
                {
                    // RG_MEPLL2_REF_DL:0x618[12:8]. MEMPLL2 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);

                    // RG_MEPLL2_FBK_DL:0x618[4:0]. MEMPLL2 FBK_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                    mcSET_FIELD(u4value, pll1st_dl, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value); 
                }
                else // REF lead FBK, delay REF
                {
                    // RG_MEPLL2_REF_DL:0x618[12:8]. MEMPLL2 REF_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                    mcSET_FIELD(u4value, pll1st_dl, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);

                    // RG_MEPLL2_FBK_DL:0x618[4:0]. MEMPLL2 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value); 
                }                
            }
            if (!pll2nd_done)    // MEMPLL 4
            {
                if (pll2nd_phase == 0) // initial phase set to 0 for REF and FBK
                {
                    // RG_MEPLL4_REF_DL:0x630[12:8]. MEMPLL4 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);

                    // RG_MEPLL4_FBK_DL:0x630[4:0]. MEMPLL4 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);                  
                }
                else if (pll2nd_phase == 1) // REF lag FBK, delay FBK
                {
                    // RG_MEPLL4_REF_DL:0x630[12:8]. MEMPLL4 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);

                    // RG_MEPLL4_FBK_DL:0x630[4:0]. MEMPLL4 FBK_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                    mcSET_FIELD(u4value, pll2nd_dl, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value); 
                }
                else // REF lead FBK, delay REF
                {
                    // RG_MEPLL4_REF_DL:0x630[12:8]. MEMPLL4 REF_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                    mcSET_FIELD(u4value, pll2nd_dl, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);

                    // RG_MEPLL4_FBK_DL:0x630[4:0]. MEMPLL4 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value); 
                }                
            }
            if (!pll3rd_done)    // MEMPLL05 2
            {
                if (pll3rd_phase == 0) // initial phase set to 0 for REF and FBK
                {
                    // RG_MEPLL05_2_REF_DL:0x668[12:8]. MEMPLL05_2 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);

                    // RG_MEPLL05_2_FBK_DL:0x668[4:0]. MEMPLL05_2 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
                }
                else if (pll3rd_phase == 1) // REF lag FBK, delay FBK
                {
                    // RG_MEPLL05_2_REF_DL:0x668[12:8]. MEMPLL05_2 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);

                    // RG_MEPLL05_2_FBK_DL:0x668[4:0]. MEMPLL05_2 FBK_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                    mcSET_FIELD(u4value, pll3rd_dl, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
                }
                else // REF lead FBK, delay REF
                {
                    // RG_MEPLL05_2_REF_DL:0x668[12:8]. MEMPLL05_2 REF_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                    mcSET_FIELD(u4value, pll3rd_dl, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);

                    // RG_MEPLL05_2_FBK_DL:0x668[4:0]. MEMPLL05_2 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
                }                
            }
            if (!pll4th_done)    // MEMPLL05 3
            {
                if (pll4th_phase == 0) // initial phase set to 0 for REF and FBK
                {
                    // RG_MEPLL05_3_REF_DL:0x674[12:8]. MEMPLL05_3 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);

                    // RG_MEPLL05_3_FBK_DL:0x674[4:0]. MEMPLL05_3 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
                }
                else if (pll4th_phase == 1) // REF lag FBK, delay FBK
                {
                    // RG_MEPLL05_3_REF_DL:0x674[12:8]. MEMPLL05_3 REF_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);

                    // RG_MEPLL05_3_FBK_DL:0x674[4:0]. MEMPLL05_3 FBK_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                    mcSET_FIELD(u4value, pll4th_dl, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
                }
                else // REF lead FBK, delay REF
                {
                    // RG_MEPLL05_3_REF_DL:0x674[12:8]. MEMPLL05_3 REF_DL to increase
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                    mcSET_FIELD(u4value, pll4th_dl, 0x00001f00, 8);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);

                    // RG_MEPLL05_3_FBK_DL:0x674[4:0]. MEMPLL05_3 FBK_DL to 0
                    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                    mcSET_FIELD(u4value, 0x0, 0x0000001f, 0);
                    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
                }                
            }

            // delay 20us for external loop PLL stable
            mcDELAY_US(20);

            // 4. Enable jitter meter
	    //MEMPLL 2 jitter meter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

	    // MEMPLL 4 jitter metter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);

            if (p->channel == CHANNEL_A)
            {
		    // POP(DDRPHY05) MEMPLL 2 jitter metter enable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x18c), &u4value);
	            mcSET_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x18c), u4value);

		    // POP(DDRPHY05) MEMPLL 3 jitter metter enable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x190), &u4value);
	            mcSET_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x190), u4value);
            }


            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
            mcDELAY_US(fcJMETER_WAIT_DONE_US);
            // Another solution is to use done to check ready. Check Reg.32ch[7:0]=01101010b (0x6a) 0: not ready. 1: update result.
            /*
            do 
            {
            	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x32c), &u4value);
            }
            while ((u4value & 0x6a)!=0x6a);
	   */

            // 6. Check jitter meter counter value
            if (!pll1st_done)    // MEMPLL 2
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x320), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);

                if (pll1st_phase == 0)
                {                   
                    if (one_count > (zero_count+fcJMETER_COUNT/10))
                    {
                        // REF lag FBK
                        pll1st_phase = 1;
                        pll1st_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 initial phase: REF lag FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else if (zero_count > (one_count+fcJMETER_COUNT/10))
                    {
                        // REF lead FBK
                        pll1st_phase = 2;
                        pll1st_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 initial phase: REF lead FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else
                    {
                        // in phase at initial
                        pll1st_done = 1;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 initial phase: REF in-phase FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));                        
                    }
                }
                else if (pll1st_phase == 1)
                {
                    if ((zero_count+fcJMETER_COUNT/10) >= one_count)
                    {
                        pll1st_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 REF_DL: 0x0, FBK_DL: 0x%x, one_cnt/zero_cnt = %d/%d\n", pll1st_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll1st_dl++;
                    }
                }
                else
                {
                    if ((one_count+fcJMETER_COUNT/10) >= zero_count)
                    {
                        pll1st_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 REF_DL: 0x%x, FBK_DL: 0x0, one_cnt/zero_cnt = %d/%d\n", pll1st_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll1st_dl++;
                    }
                }                
            }

            if (!pll2nd_done)    // MEMPLL 4
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x328), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);
                
                if (pll2nd_phase == 0)
                {
                    if (one_count > (zero_count+fcJMETER_COUNT/10))
                    {
                        // REF lag FBK
                        pll2nd_phase = 1;
                        pll2nd_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 initial phase: REF lag FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else if (zero_count > (one_count+fcJMETER_COUNT/10))
                    {
                        // REF lead FBK
                        pll2nd_phase = 2;
                        pll2nd_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 initial phase: REF lead FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else
                    {
                        // in phase at initial
                        pll2nd_done = 1;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 initial phase: REF in-phase FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));                        
                    }
                }
                else if (pll2nd_phase == 1)
                {
                    if ((zero_count+fcJMETER_COUNT/10) >= one_count)
                    {
                        pll2nd_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 REF_DL: 0x0, FBK_DL: 0x%x, one_cnt/zero_cnt = %d/%d\n", pll2nd_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll2nd_dl++;
                    }
                }
                else
                {
                    if ((one_count+fcJMETER_COUNT/10) >= zero_count)
                    {
                        pll2nd_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 REF_DL: 0x%x, FBK_DL: 0x0, one_cnt/zero_cnt = %d/%d\n", pll2nd_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll2nd_dl++;
                    }
                }
            }

            if (!pll3rd_done)    // MEMPLL05 2
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x33c), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);
                
                if (pll3rd_phase == 0)
                {
                    if (one_count > (zero_count+fcJMETER_COUNT/10))
                    {
                        // REF lag FBK
                        pll3rd_phase = 1;
                        pll3rd_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 2 initial phase: REF lag FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else if (zero_count > (one_count+fcJMETER_COUNT/10))
                    {
                        // REF lead FBK
                        pll3rd_phase = 2;
                        pll3rd_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 2 initial phase: REF lead FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else
                    {
                        // in phase at initial
                        pll3rd_done = 1;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 2 initial phase: REF in-phase FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));                        
                    }
                }
                else if (pll3rd_phase == 1)
                {
                    if ((zero_count+fcJMETER_COUNT/10) >= one_count)
                    {
                        pll3rd_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 2 REF_DL: 0x0, FBK_DL: 0x%x, one_cnt/zero_cnt = %d/%d\n", pll3rd_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll3rd_dl++;
                    }
                }
                else
                {
                    if ((one_count+fcJMETER_COUNT/10) >= zero_count)
                    {
                        pll3rd_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 2 REF_DL: 0x%x, FBK_DL: 0x0, one_cnt/zero_cnt = %d/%d\n", pll3rd_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll3rd_dl++;
                    }
                }
            }

            if (!pll4th_done)    // MEMPLL05 3
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x340), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);
                
                if (pll4th_phase == 0)
                {
                    if (one_count > (zero_count+fcJMETER_COUNT/10))
                    {
                        // REF lag FBK
                        pll4th_phase = 1;
                        pll4th_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 3 initial phase: REF lag FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else if (zero_count > (one_count+fcJMETER_COUNT/10))
                    {
                        // REF lead FBK
                        pll4th_phase = 2;
                        pll4th_dl++;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 3 initial phase: REF lead FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
                    }
                    else
                    {
                        // in phase at initial
                        pll4th_done = 1;

                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 3 initial phase: REF in-phase FBK, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));                        
                    }
                }
                else if (pll4th_phase == 1)
                {
                    if ((zero_count+fcJMETER_COUNT/10) >= one_count)
                    {
                        pll4th_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 3 REF_DL: 0x0, FBK_DL: 0x%x, one_cnt/zero_cnt = %d/%d\n", pll4th_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll4th_dl++;
                    }
                }
                else
                {
                    if ((one_count+fcJMETER_COUNT/10) >= zero_count)
                    {
                        pll4th_done = 1;
                        mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 3 REF_DL: 0x%x, FBK_DL: 0x0, one_cnt/zero_cnt = %d/%d\n", pll4th_dl, one_count, zero_count));
                    }
                    else
                    {
                        pll4th_dl++;
                    }
                }
            }
            

            // 7. Reset jitter meter value
	    //MEMPLL 2 jitter meter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

	    // MEMPLL 4 jitter metter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);

            if (p->channel == CHANNEL_A)
            {
		    // POP(DDRPHY05) MEMPLL 2 jitter metter disable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x18c), &u4value);
	            mcCLR_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x18c), u4value);

		    // POP(DDRPHY05) MEMPLL 3 jitter metter disable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x190), &u4value);
	            mcCLR_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x190), u4value);            
            }

            // 8. All done?! early break
            if (pll1st_done && pll2nd_done && pll3rd_done && pll4th_done)
            {
                ret = 0;
                break;
            }

            // 9. delay line overflow?! break
            if ((pll1st_dl >= 32) || (pll2nd_dl >= 32) || (pll3rd_dl >= 32) || (pll4th_dl >=32))
            {
                ret = -1;
                break;
            }
    }         
    
    if (ret != 0)
    {
        mcSHOW_ERR_MSG(("MEMPLL calibration fail\n"));
    }

    if (ucstatus != 0)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));        
    }
       

    /***********************************************
    * 7. Set jitter meter clock to external FB path
    ************************************************/
    //MEMPLL*_FB_MCK_SEL = 1;
    // Not necessary for A60808 

#ifdef DDR_FT_LOAD_BOARD
    if (ret !=0)
    {
        LoadBoardShowResult(FLAG_PLLPHASE_CALIBRATION, FLAG_CALIBRATION_FAIL, 0, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);
    }
    else if ((pll1st_dl >= PLL_PHCALIB_BOUND) || (pll2nd_dl >= PLL_PHCALIB_BOUND) || (pll3rd_dl >= PLL_PHCALIB_BOUND) || (pll4th_dl >=PLL_PHCALIB_BOUND))
    {
        // too large skew between REF and FBK
        LoadBoardShowResult(FLAG_PLLPHASE_CALIBRATION, FLAG_WINDOW_TOO_SMALL, 0, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);
    }
#endif

    if ((ucstatus != 0) || (ret != 0))
    {
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }

}
#else
DRAM_STATUS_T DramcPllPhaseCal(DRAMC_CTX_T *p)
{
// MEMPLL's are different for A60808 and MT6595
    U8 ucstatus = 0;
    U16 one_count = 0, zero_count = 0;
    U8 pll1st_done = 0, pll2nd_done = 0, pll3rd_done = 0, pll4th_done = 0;
    U8 pll1st_dl = 0, pll2nd_dl = 0, pll3rd_dl = 0, pll4th_dl = 0;
    S8 ret = 0;
    U32 u4value;
    
    // for PoP, use MEMPLL 2,4 and MEMPLL05 2, 3
    // for SBS, use MEMPLL 2,3,4

    // 1. Set jitter meter clock to internal FB path    
    // MEMPLL*_FB_MCK_SEL = 0;
    // Not necessary for A60808
    // It has no phase difference when internal or external loop

    // 2. Set jitter meter count number    
    // JMTRCNT = 0x400;    // JMTRCNT:0x74[30:16]. Set jitter meter count number to 1024
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

    // MEMPLL 3 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d0), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d0), u4value);

    // MEMPLL 4 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);

    // POP(DDRPHY05) MEMPLL 2 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x18c), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x18c), u4value);

    // POP(DDRPHY05) MEMPLL 3 jitter metter count
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x190), &u4value);
    mcSET_FIELD(u4value, fcJMETER_COUNT, 0xffff0000, 16);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x190), u4value);

    while(1)
    {          
        if (p->package == PACKAGE_SBS)
        {
            // 3. Adjust delay chain tap number
        
            if (!pll1st_done)    // MEMPLL 2
            {
                // RG_MEPLL2_REF_DL:0x618[12:8]. MEMPLL2 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);

                // RG_MEPLL2_FBK_DL:0x618[4:0]. MEMPLL2 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                mcSET_FIELD(u4value, pll1st_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);
            }
            if (!pll2nd_done)    // MEMPLL 3
            {
                // RG_MEPLL3_REF_DL:0x624[12:8]. MEMPLL3 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x624), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), u4value);

                // RG_MEPLL3_FBK_DL:0x624[4:0]. MEMPLL3 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x624), &u4value);
                mcSET_FIELD(u4value, pll2nd_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), u4value);
            }
            if (!pll3rd_done)    // MEMPLL 4
            {
                // RG_MEPLL4_REF_DL:0x630[12:8]. MEMPLL4 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);

                // RG_MEPLL4_FBK_DL:0x630[4:0]. MEMPLL4 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                mcSET_FIELD(u4value, pll3rd_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);
            }

            // 4. Enable jitter meter
	    // MEMPLL 2 jitter metter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

	    // MEMPLL 3 jitter metter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d0), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d0), u4value);

	    // MEMPLL 4 jitter metter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);            

            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
            mcDELAY_US(fcJMETER_WAIT_DONE_US);
            // Another solution is to use done to check ready. Check Reg.32ch[7:0]=00001110b (0x0e) 0: not ready. 1: update result.
            /*
            do 
            {
            	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x32c), &u4value);
            }
            while ((u4value & 0x0e)!=0x0e);
	   */
	   
            // 6. Check jitter meter counter value
            if (!pll1st_done)    // MEMPLL 2
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x320), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);

                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 FB_DL: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL2 FB_DL: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));
                    pll1st_done = 1;
                }
                else
                {
                    pll1st_dl++;
                }
            }

            if (!pll2nd_done)    // MEMPLL 3
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x324), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);

                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL3 FB_DL: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL3 FB_DL: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));
                    pll2nd_done = 1;
                }
                else
                {
                    pll2nd_dl++;
                }
            }

            if (!pll3rd_done)    // MEMPLL 4
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x328), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);

                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 FB_DL: 0x%x, 1/0 = %d/%d\n", pll3rd_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL4 FB_DL: 0x%x, 1/0 = %d/%d\n", pll3rd_dl, one_count, zero_count));
                    pll3rd_done = 1;
                }
                else
                {
                    pll3rd_dl++;
                }
            }

            // 7. Reset jitter meter value
	    // MEMPLL 2 jitter metter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

	    // MEMPLL 3 jitter metter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d0), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d0), u4value);

	    // MEMPLL 4 jitter metter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);                        

            // 8. All done?! early break
            if (pll1st_done && pll2nd_done && pll3rd_done)
            {
                ret = 0;
                break;
            }

            // 9. delay line overflow?! break
            if (pll1st_dl >= 32 || pll2nd_dl >= 32 || pll3rd_dl >= 32)
            {
                ret = -1;
                break;
            }
        }
        else
        {
            // POP case
            if (p->channel == CHANNEL_B)
            {
            	// POP05 only exists in channel A. No need to do the phase calibration in channel B.
            	pll3rd_done = pll4th_done = 1;
            }
            
            if (!pll1st_done)    // MEMPLL 2
            {
                // RG_MEPLL2_REF_DL:0x618[12:8]. MEMPLL2 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);

                // RG_MEPLL2_FBK_DL:0x618[4:0]. MEMPLL2 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
                mcSET_FIELD(u4value, pll1st_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);
            }
            if (!pll2nd_done)    // MEMPLL 4
            {
                // RG_MEPLL4_REF_DL:0x630[12:8]. MEMPLL4 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);

                // RG_MEPLL4_FBK_DL:0x630[4:0]. MEMPLL4 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
                mcSET_FIELD(u4value, pll2nd_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);
            }
            if (!pll3rd_done)    // MEMPLL05 2
            {
                // RG_MEPLL05_2_REF_DL:0x668[12:8]. MEMPLL05_2 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);

                // RG_MEPLL05_2_FBK_DL:0x668[4:0]. MEMPLL05_2 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
                mcSET_FIELD(u4value, pll3rd_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
            }
            if (!pll4th_done)    // MEMPLL05 3
            {
                // RG_MEPLL05_3_REF_DL:0x674[12:8]. MEMPLL05_3 REF_DL fixed to 0x10
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                mcSET_FIELD(u4value, 0x10, 0x00001f00, 8);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);

                // RG_MEPLL05_3_FBK_DL:0x674[4:0]. MEMPLL05_3 FBK_DL inc 1
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
                mcSET_FIELD(u4value, pll4th_dl, 0x0000001f, 0);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
            }

            // 4. Enable jitter meter
	    //MEMPLL 2 jitter meter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

	    // MEMPLL 4 jitter metter enable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
            mcSET_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);

            if (p->channel == CHANNEL_A)
            {
		    // POP(DDRPHY05) MEMPLL 2 jitter metter enable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x18c), &u4value);
	            mcSET_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x18c), u4value);

		    // POP(DDRPHY05) MEMPLL 3 jitter metter enable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x190), &u4value);
	            mcSET_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x190), u4value);
            }


            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
            mcDELAY_US(fcJMETER_WAIT_DONE_US);
            // Another solution is to use done to check ready. Check Reg.32ch[7:0]=01101010b (0x6a) 0: not ready. 1: update result.
            /*
            do 
            {
            	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x32c), &u4value);
            }
            while ((u4value & 0x6a)!=0x6a);
	   */

            // 6. Check jitter meter counter value
            if (!pll1st_done)    // MEMPLL 2
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x320), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);                

                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL2 FB_DL: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL2 FB_DL: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));
                    pll1st_done = 1;
                }
                else
                {
                    pll1st_dl++;
                }
            }

            if (!pll2nd_done)    // MEMPLL 4
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x328), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);
                
                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL4 FB_DL: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL4 FB_DL: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));
                    pll2nd_done = 1;
                }
                else
                {
                    pll2nd_dl++;
                }
            }

            if (!pll3rd_done)    // MEMPLL05 2
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x33c), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);
                
                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 2 FB_DL: 0x%x, 1/0 = %d/%d\n", pll3rd_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL05 2 FB_DL: 0x%x, 1/0 = %d/%d\n", pll3rd_dl, one_count, zero_count));
                    pll3rd_done = 1;
                }
                else
                {
                    pll3rd_dl++;
                }
            }

            if (!pll4th_done)    // MEMPLL05 3
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x340), &u4value);
                one_count = (U16) mcGET_FIELD(u4value, 0xffff0000, 16);
                zero_count = (U16) mcGET_FIELD(u4value, 0x0000ffff, 0);
                
                if (zero_count > (fcJMETER_COUNT/2))
                {
                    mcSHOW_DBG_MSG(("[PLL_Phase_Calib] PLL05 3 FB_DL: 0x%x, 1/0 = %d/%d\n", pll4th_dl, one_count, zero_count));
                    mcFPRINTF((fp_A60808, "[PLL_Phase_Calib] PLL05 3 FB_DL: 0x%x, 1/0 = %d/%d\n", pll4th_dl, one_count, zero_count));
                    pll4th_done = 1;
                }
                else
                {
                    pll4th_dl++;
                }
            }

            // 7. Reset jitter meter value
	    //MEMPLL 2 jitter meter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1cc), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1cc), u4value);

	    // MEMPLL 4 jitter metter disable
	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1d4), &u4value);
            mcCLR_BIT(u4value, 0);
	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1d4), u4value);

            if (p->channel == CHANNEL_A)
            {
		    // POP(DDRPHY05) MEMPLL 2 jitter metter disable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x18c), &u4value);
	            mcCLR_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x18c), u4value);

		    // POP(DDRPHY05) MEMPLL 3 jitter metter disable
		    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x190), &u4value);
	            mcCLR_BIT(u4value, 0);
		    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x190), u4value);            
            }

            // 8. All done?! early break
            if (pll1st_done && pll2nd_done && pll3rd_done && pll4th_done)
            {
                ret = 0;
                break;
            }

            // 9. delay line overflow?! break
            if ((pll1st_dl >= 32) || (pll2nd_dl >= 32) || (pll3rd_dl >= 32) || (pll4th_dl >=32))
            {
                ret = -1;
                break;
            }
        }         
    }

#if 0
    // for testing
    if (p->ssc_en == ENABLE)
    {
        // RG_SYSPLL_SDM_SSC_EN = 1 (0x14[26])
        ucstatus |= ucDram_Register_Read(mcSET_SYS_REG_ADDR(0x014), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_SYS_REG_ADDR(0x014), u4value);
    }
#endif

    if (ret != 0)
    {
        mcSHOW_ERR_MSG(("MEMPLL calibration fail\n"));
    }

    if (ucstatus != 0)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));        
    }
       

    /***********************************************
    * 7. Set jitter meter clock to external FB path
    ************************************************/
    //MEMPLL*_FB_MCK_SEL = 1;
    // Not necessary for A60808 

    if ((ucstatus != 0) || (ret != 0))
    {
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }

}
#endif

//-------------------------------------------------------------------------
/** DramCPllGroupsCal
 *  PLL Groups Skew Calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
#ifdef fcNEW_PLL_GPPHASE_CALIB
DRAM_STATUS_T DramCPllGroupsCal(DRAMC_CTX_T *p)
{
    U16 one_count = 0, zero_count = 0;
    U8 pll1st_done = 0, pll2nd_done = 0;
    U8 pll1st_dl = 0, pll2nd_dl = 0;
    U8 pll1st_phase = 0, pll2nd_phase = 0;
    U8 pll1st_ref_dl = 0, pll1st_fbk_dl = 0, pll2nd_ref_dl = 0, pll2nd_fbk_dl = 0, pllmax_ref_dl = 0;
    S8 ret = 0;
    U32 u4value;
    U8 ucstatus = 0;

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x638), &u4value);
    if ((u4value & (1<<29)))
    {
        // Only CTS mode needs to do the PLL group calibration.
        // Seal ring mode do this may cause larger skew. So directly return;
        return DRAM_OK;
    }

    mcSHOW_DBG_MSG(("[PLL_Group_Calib] ===== PLL Group Phase calibration:(ChB vs. ChA)/(ChB vs. 05PHY) =====\n"));

    // POP : K CHA & CHB. Then CHB & 05PHY. Set CHB to max.
    
    // 1. Set jitter meter clock to internal FB path    
    // MEMPLL*_FB_MCK_SEL = 0;
    // Not necessary for A60808
    // It has no phase difference when internal or external loop

    // First K CHA & CHB
    // 2. Set jitter meter count number    
    // JMTRCNT = 0x400;    // JMTRCNT:0x1000f1d8[31:16]. Set jitter meter count number to 1024
    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) &= 0x0000ffff;
    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) |= (fcJMETER_COUNT<<16);
    
    while(1)
    {
            if (pll1st_phase == 0)
            {
                // Fix channel B delay to 0x0.                
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x0 << 24);

                // Fix channel A delay to 0x0
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) |= (0x0 << 24);
            }
            else if (pll1st_phase == 1) // chB lag chA, delay chA
            {
                // Fix channel B delay to 0x0.                
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x0 << 24);

                // channel A delay to increase
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) |= (pll1st_dl << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) |= (pll1st_dl << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) |= (pll1st_dl << 24);
            }
            else // chB lead chA, delay chB
            {
                // channel B delay to increase.                
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (pll1st_dl << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (pll1st_dl << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (pll1st_dl << 24);

                // Fix channel A delay to 0x0
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) |= (0x0 << 24);
            }	    
        
            // delay 20us for PLL stable
            mcDELAY_US(20);

            // 4. Enable jitter meter
	    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
	    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) |= 0x00000001;

            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
            // mcDELAY_US(fcJMETER_WAIT_DONE_US);
           while (((*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x2bc)) & (0x01<<31))==0);

	    u4value = (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x2bc));
	    one_count = (U16) mcGET_FIELD(u4value, 0x7fff0000, 16);
	    zero_count = (U16) mcGET_FIELD(u4value, 0x00007fff, 0);

	    if (pll1st_phase == 0)
	    {                   
	        if (one_count > (zero_count+fcJMETER_COUNT/10))
	        {
                   // Ch-B lag Ch-A
                   pll1st_phase = 1;
                   pll1st_dl++;

                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] Initial phase: Ch-B lag Ch-A, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
	        }
	        else if (zero_count > (one_count+fcJMETER_COUNT/10))
	        {
                   // Ch-B lead Ch-A
                   pll1st_phase = 2;
                   pll1st_dl++;

                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] Initial phase: Ch-B lead Ch-A, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
	        }
	        else
	        {
                   // in phase at initial
                   pll1st_done = 1;

                   pll1st_ref_dl = 0;
                   pll1st_fbk_dl = 0;

                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] Initial phase: Ch-B in-phase Ch-A, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));                        
	        }
	    }
	    else if (pll1st_phase == 1)
	    {
	        if ((zero_count+fcJMETER_COUNT/10) >= one_count)
	        {
                   pll1st_done = 1;

                   pll1st_ref_dl = 0;
                   pll1st_fbk_dl = pll1st_dl;
                    
                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] CHB_DL: 0x0, CHA_DL: 0x%x, one_cnt/zero_cnt = %d/%d\n", pll1st_dl, one_count, zero_count));
	        }
	        else
	        {
                   pll1st_dl++;
	        }
	    }
	    else
	    {
	        if ((one_count+fcJMETER_COUNT/10) >= zero_count)
	        {
                   pll1st_done = 1;

                   pll1st_ref_dl = pll1st_dl;
                   pll1st_fbk_dl = 0;
                   
                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] CHB_DL: 0x%x, CHA_DL: 0x0, one_cnt/zero_cnt = %d/%d\n", pll1st_dl, one_count, zero_count));
	        }
	        else
	        {
                   pll1st_dl++;
	        }
	    }
	   
            // 7. Reset jitter meter value
	    // MEMPLL 2 jitter metter disable
	    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
	    
            // 8. All done?! early break
            if (pll1st_done)
            {
                ret = 0;
                break;
            }

            // 9. delay line overflow?! break
            if (pll1st_dl >= 64)
            {
                ret = -1;
                break;
            }
    }

    if (ret != 0)
    {
        mcSHOW_ERR_MSG(("[PLL_Group_Calib] MEMPLL group phase calibration fail (channel B vs. channel A)\n"));
    #ifdef DDR_FT_LOAD_BOARD
        LoadBoardShowResult(FLAG_PLLGPPHASE_CALIBRATION, FLAG_CALIBRATION_FAIL, 0, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);
    #endif
        return DRAM_FAIL;
    }

#ifdef DDR_FT_LOAD_BOARD
    if ((pll1st_dl >= PLL_GPPHCALIB_BOUND))
    {
        // too large skew between Ch-B and Ch-A
        LoadBoardShowResult(FLAG_PLLGPPHASE_CALIBRATION, FLAG_WINDOW_TOO_SMALL, 0, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);
    }
#endif

    if (p->package == PACKAGE_POP)
    {
        // K 05PHY & CHB
        // 2. Set jitter meter count number    
        // JMTRCNT = 0x400;    // JMTRCNT:0x100121d8[30:16]. Set jitter meter count number to 1024
        (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) &= 0x0000ffff;
        (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) |= (fcJMETER_COUNT<<16);
	    
        while(1)
        {
            if (pll2nd_phase == 0)
            {
                // Fix channel B delay to 0x0.                
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x0 << 24);

                // Fix 05PHY delay to 0x0
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) |= (0x0 << 24);
            }
            else if (pll2nd_phase == 1) // chB lag 05PHY, delay 05PHY
            {
                // Fix channel B delay to 0x0.                
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x0 << 24);

                // 05PHY delay to increase
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) |= (pll2nd_dl << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) |= (pll2nd_dl << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) |= (pll2nd_dl << 24);
            }
            else // chB lead 05PHY, delay chB
            {
                // channel B delay to increase.                
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (pll2nd_dl << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (pll2nd_dl << 24);
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (pll2nd_dl << 24);

                // Fix 05PHY delay to 0x0
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) |= (0x0 << 24);
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) &= 0xc0ffffff;
                (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) |= (0x0 << 24);
            }	  
            
            // delay 20us for PLL stable
            mcDELAY_US(20);
	        
            // 4. Enable jitter meter
            (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
            (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) |= 0x00000001;

            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
            //mcDELAY_US(fcJMETER_WAIT_DONE_US);
            while (((*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x2bc)) & (0x01<<31))==0);

            u4value = (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x2bc));
            one_count = (U16) mcGET_FIELD(u4value, 0x7fff0000, 16);
            zero_count = (U16) mcGET_FIELD(u4value, 0x00007fff, 0);

            if (pll2nd_phase == 0)
	    {                   
	        if (one_count > (zero_count+fcJMETER_COUNT/10))
	        {
                   // Ch-B lag 05PHY
                   pll2nd_phase = 1;
                   pll2nd_dl++;

                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] Initial phase: Ch-B lag 05PHY, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
	        }
	        else if (zero_count > (one_count+fcJMETER_COUNT/10))
	        {
                   // Ch-B lead 05PHY
                   pll2nd_phase = 2;
                   pll2nd_dl++;

                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] Initial phase: Ch-B lead 05PHY, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));
	        }
	        else
	        {
                   // in phase at initial
                   pll2nd_done = 1;

                   pll2nd_ref_dl = 0;
                   pll2nd_fbk_dl = 0;

                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] Initial phase: Ch-B in-phase 05PHY, one_cnt/zero_cnt = %d/%d\n", one_count, zero_count));                        
	        }
	    }
	    else if (pll2nd_phase == 1)
	    {
	        if ((zero_count+fcJMETER_COUNT/10) >= one_count)
	        {
                   pll2nd_done = 1;

                   pll2nd_ref_dl = 0;
                   pll2nd_fbk_dl = pll2nd_dl;
                   
                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] CHB_DL: 0x0, 05PHY_DL: 0x%x, one_cnt/zero_cnt = %d/%d\n", pll2nd_dl, one_count, zero_count));
	        }
	        else
	        {
                   pll2nd_dl++;
	        }
	    }
	    else
	    {
	        if ((one_count+fcJMETER_COUNT/10) >= zero_count)
	        {
                   pll2nd_done = 1;

                   pll2nd_ref_dl = pll2nd_dl;
                   pll2nd_fbk_dl = 0;
                   
                   mcSHOW_DBG_MSG(("[PLL_Group_Calib] CHB_DL: 0x%x, 05PHY_DL: 0x0, one_cnt/zero_cnt = %d/%d\n", pll2nd_dl, one_count, zero_count));
	        }
	        else
	        {
                   pll2nd_dl++;
	        }
	    }
		   
	    // 7. Reset jitter meter value
	    // MEMPLL 2 jitter metter disable
	    (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
		    
	    // 8. All done?! early break
	    if (pll2nd_done)
	    {
	        break;
	    }

	    // 9. delay line overflow?! break
	    if (pll2nd_dl >= 64)
	    {
	        ret = -1;
	        break;
	    }
	}

        if (ret != 0)
        {
            mcSHOW_ERR_MSG(("[PLL_Group_Calib] MEMPLL group phase calibration fail (channel B vs. 05PHY)\n"));
        #ifdef DDR_FT_LOAD_BOARD
            LoadBoardShowResult(FLAG_PLLGPPHASE_CALIBRATION, FLAG_CALIBRATION_FAIL, 1, FLAG_NOT_COMPLETE_OR_FAIL);            
            while(1);
        #endif
            return DRAM_FAIL;
        }

    #ifdef DDR_FT_LOAD_BOARD
        if ((pll2nd_dl >= PLL_GPPHCALIB_BOUND))
        {
            // too large skew between Ch-B and 05PHY
            LoadBoardShowResult(FLAG_PLLGPPHASE_CALIBRATION, FLAG_WINDOW_TOO_SMALL, 1, FLAG_NOT_COMPLETE_OR_FAIL);            
            while(1);
        }
    #endif

        // align max reference delay
        pllmax_ref_dl = max(pll1st_ref_dl, pll2nd_ref_dl);
        if (pllmax_ref_dl != pll1st_ref_dl)
        {
            pll1st_fbk_dl = pll1st_fbk_dl + (pllmax_ref_dl-pll1st_ref_dl);
        }

        if (pllmax_ref_dl != pll2nd_ref_dl)
        {
            pll2nd_fbk_dl = pll2nd_fbk_dl + (pllmax_ref_dl-pll2nd_ref_dl);
        }

        if (pll1st_fbk_dl>=64)
        {
            pll1st_fbk_dl = 63;
        #ifdef DDR_FT_LOAD_BOARD
            // not enough delay taps
            LoadBoardShowResult(FLAG_PLLGPPHASE_CALIBRATION, FLAG_WINDOW_TOO_BIG, 0, FLAG_NOT_COMPLETE_OR_FAIL);            
            while(1);
        #endif
        }

        if (pll2nd_fbk_dl>=64)
        {
            pll2nd_fbk_dl = 63;
        #ifdef DDR_FT_LOAD_BOARD
            // not enough delay taps
            LoadBoardShowResult(FLAG_PLLGPPHASE_CALIBRATION, FLAG_WINDOW_TOO_BIG, 1, FLAG_NOT_COMPLETE_OR_FAIL);            
            while(1);
        #endif
        }

        // set to chb (reference) delay
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (pllmax_ref_dl << 24);
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (pllmax_ref_dl << 24);
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (pllmax_ref_dl << 24);

        // set to cha delay
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) |= (pll1st_fbk_dl << 24);
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) |= (pll1st_fbk_dl << 24);
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) |= (pll1st_fbk_dl << 24);

        // set to 05PHY delay
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) |= (pll2nd_fbk_dl << 24);
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) |= (pll2nd_fbk_dl << 24);
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) &= 0xc0ffffff;
        (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) |= (pll2nd_fbk_dl << 24);

        mcSHOW_DBG_MSG(("[PLL_Group_Calib] Final CHB_DL=0x%x, CHA_DL=0x%x, 05PHY_DL=0x%x\n", pllmax_ref_dl, pll1st_fbk_dl, pll2nd_fbk_dl)); 
    }        

    /***********************************************
    * 7. Set jitter meter clock to external FB path
    ************************************************/
    //MEMPLL*_FB_MCK_SEL = 1;
    // Not necessary for A60808 

    return DRAM_OK;
}
#else
DRAM_STATUS_T DramCPllGroupsCal(DRAMC_CTX_T *p)
{
    U16 one_count = 0, zero_count = 0;
    U8 pll1st_done = 0, pll2nd_done = 0;
    U8 pll1st_dl = 0, pll2nd_dl = 0;
    S8 ret = 0;
    U32 u4value;
    U8 ucstatus = 0;

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x638), &u4value);
	if ((u4value & (1<<29)))
	{
		// Only CTS mode needs to do the PLL group calibration.
		// Seal ring mode do this may cause larger skew. So directly return;
        return DRAM_OK;
	}

    #if 0
    // Fix channel B delay to 0x020.
    // 0x1001261c[29:24] RG_MEMPLL2_DL_RESERVE = 0x20;
    // 0x10012628[29:24] RG_MEMPLL3_DL_RESERVE = 0x20;
    // 0x10012634[29:24] RG_MEMPLL4_DL_RESERVE = 0x20;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x20 << 24);
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x20 << 24);
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x20 << 24);

    // 3. Adjust delay chain tap number of CHA
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) |= (0x1e << 24);
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) |= (0x1e << 24);
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) |= (0x1e << 24);

	    // 3. Adjust delay chain tap number of 05PHY
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) &= 0xc0ffffff;
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) |= (0x25 << 24);
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) &= 0xc0ffffff;
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) |= (0x25 << 24);
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) &= 0xc0ffffff;
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) |= (0x25 << 24);
    #else
    

    // POP : Set 0x20 to CHB. Then K CHA & CHB. Then CHB & 05PHY.
    // SBS : Set 0x20 to CHB. Then K CHA & CHB.

    // 1. Set jitter meter clock to internal FB path    
    // MEMPLL*_FB_MCK_SEL = 0;
    // Not necessary for A60808
    // It has no phase difference when internal or external loop

    // First K CHA & CHB
    // 2. Set jitter meter count number    
    // JMTRCNT = 0x400;    // JMTRCNT:0x1000f1d8[31:16]. Set jitter meter count number to 1024
    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) &= 0x0000ffff;
    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) |= (fcJMETER_COUNT<<16);

    // Fix channel B delay to 0x020.
    // 0x1001261c[29:24] RG_MEMPLL2_DL_RESERVE = 0x20;
    // 0x10012628[29:24] RG_MEMPLL3_DL_RESERVE = 0x20;
    // 0x10012634[29:24] RG_MEMPLL4_DL_RESERVE = 0x20;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x20 << 24);
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x20 << 24);
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x20 << 24);
    while(1)
    {         
	    // 3. Adjust delay chain tap number of CHA
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x61c)) |= (pll1st_dl << 24);
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x628)) |= (pll1st_dl << 24);
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x634)) |= (pll1st_dl << 24);
        
           mcDELAY_US(1);

            // 4. Enable jitter meter
	    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
	    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) |= 0x00000001;

            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
            // mcDELAY_US(fcJMETER_WAIT_DONE_US);
           while (((*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x2bc)) & (0x01<<31))==0);

	    u4value = (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x2bc));
	    one_count = (U16) mcGET_FIELD(u4value, 0x7fff0000, 16);
	    zero_count = (U16) mcGET_FIELD(u4value, 0x00007fff, 0);	    

	    //mcSHOW_DBG_MSG(("[PLL_Group_Calib] CHA DL_RESERVE: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));

            // 6. Check jitter meter counter value
            if (zero_count > (fcJMETER_COUNT/2))
            {
                mcSHOW_DBG_MSG(("[PLL_Group_Calib] CHA DL_RESERVE: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));
                mcFPRINTF((fp_A60808, "[PLL_Group_Calib] CHA DL_RESERVE: 0x%x, 1/0 = %d/%d\n", pll1st_dl, one_count, zero_count));
                pll1st_done = 1;
            }
            else
            {
                pll1st_dl++;
            }
	   
            // 7. Reset jitter meter value
	    // MEMPLL 2 jitter metter disable
	    (*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
	    
            // 8. All done?! early break
            if (pll1st_done)
            {
                ret = 0;
                break;
            }

            // 9. delay line overflow?! break
            if (pll1st_dl >= 64)
            {
                ret = -1;
                break;
            }
    }

    if (p->package == PACKAGE_POP)
    {
	    // K 05PHY & CHB
	    // 2. Set jitter meter count number    
	    // JMTRCNT = 0x400;    // JMTRCNT:0x100121d8[30:16]. Set jitter meter count number to 1024
	    (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) &= 0x0000ffff;
	    (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) |= (fcJMETER_COUNT<<16);

	    // Fix channel B delay to 0x020.
	    // 0x1001261c[29:24] RG_MEMPLL2_DL_RESERVE = 0x20;
	    // 0x10012628[29:24] RG_MEMPLL3_DL_RESERVE = 0x20;
	    // 0x10012634[29:24] RG_MEMPLL4_DL_RESERVE = 0x20;
	    // Already set in the CHA/CHB calibration. Skip.
	    /*
	    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x61c)) |= (0x20 << 24);
	    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x628)) |= (0x20 << 24);
	    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) &= 0xc0ffffff;
	    (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x634)) |= (0x20 << 24);
	    */
	    
	    while(1)
	    {         
		    // 3. Adjust delay chain tap number of 05PHY
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) &= 0xc0ffffff;
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x66c)) |= (pll2nd_dl << 24);
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) &= 0xc0ffffff;
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x678)) |= (pll2nd_dl << 24);
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) &= 0xc0ffffff;
		    (*(volatile unsigned int *)(CHA_DDRPHY_BASE + 0x684)) |= (pll2nd_dl << 24);
	        
		    mcDELAY_US(1);
	        
	            // 4. Enable jitter meter
		    (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
		    (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) |= 0x00000001;

	            // 5. Wait for jitter meter complete (depend on clock 20MHz, 1024/20M=51.2us )
	            // mcDELAY_US(fcJMETER_WAIT_DONE_US);
	            while (((*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x2bc)) & (0x01<<31))==0);

		    u4value = (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x2bc));
		    one_count = (U16) mcGET_FIELD(u4value, 0x7fff0000, 16);
		    zero_count = (U16) mcGET_FIELD(u4value, 0x00007fff, 0);	    

		    //mcSHOW_DBG_MSG(("[PLL_Group_Calib] 05PHY DL_RESERVE: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));

	            // 6. Check jitter meter counter value
	            if (zero_count > (fcJMETER_COUNT/2))
	            {
	                mcSHOW_DBG_MSG(("[PLL_Group_Calib] 05PHY DL_RESERVE: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));
	                mcFPRINTF((fp_A60808, "[PLL_Group_Calib] 05PHY DL_RESERVE: 0x%x, 1/0 = %d/%d\n", pll2nd_dl, one_count, zero_count));
	                pll2nd_done = 1;
	            }
	            else
	            {
	                pll2nd_dl++;
	            }
		   
	            // 7. Reset jitter meter value
		    // MEMPLL 2 jitter metter disable
		    (*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + 0x1d8)) &= 0xfffffffe;
		    
	            // 8. All done?! early break
	            if (pll2nd_done)
	            {
	                break;
	            }

	            // 9. delay line overflow?! break
	            if (pll2nd_dl >= 64)
	            {
	                ret = -1;
	                break;
	            }
	    }
    }
#endif
    if (ret != 0)
    {
        mcSHOW_ERR_MSG(("MEMPLL group phase calibration fail\n"));
    }

    /***********************************************
    * 7. Set jitter meter clock to external FB path
    ************************************************/
    //MEMPLL*_FB_MCK_SEL = 1;
    // Not necessary for A60808 

    if (ret != 0)
    {
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }

}
#endif

//-------------------------------------------------------------------------
/** DramcSwImpedanceCal
 *  start TX OCD impedance calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  apply           (U8): 0 don't apply the register we set  1 apply the register we set ,default don't apply.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
DRAM_STATUS_T DramcSwImpedanceCal(DRAMC_CTX_T *p, U8 apply)
{
// drv*_status
//       0: idle
//       1: to be done
//       2: Calibration OK
//     -1: Calibration FAIL
//     -2: Calibration NOT FOUND
    S8 drvp_status, drvn_status;
    U8 ii, drvp = 0xf, drvn = 0xf;
    U8 ucstatus =0;
    U32 u4value, u4backup_reg;
        
    mcSHOW_DBG_MSG(("[Imp Calibration] Start SW impedance calibration...\n"));
    mcFPRINTF((fp_A60808, "[Imp Calibration] Start SW impedance calibration...\n"));
            
    // 1.Initialization
    // DRVREF (REG.100[24]) = 0: change will be apply directly 
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), &u4value);
    mcCLR_BIT(u4value, POS_OCDK_DRVREF);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), u4value);
    
    // Enable R_DMMIOCKCTRLOFF (REG.1DC[26]), always no gating
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), &u4value);
    u4backup_reg = u4value;
    mcSET_BIT(u4value, POS_DRAMC_PD_CTRL_MIOCKCTRLOFF);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), u4value);

    // Set R_DMIMPCAL_HW (REG.1C8[1]) = 0 (SW)
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcCLR_BIT(u4value, POS_IMPCAL_IMPCAL_HW);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

    // Set R_DMIMPCALI_EN (REG.1C8[0]) = 1
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcSET_BIT(u4value, POS_IMPCAL_IMPCALI_EN);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);
            
    // 2.DRVP calibration
    // R_DMIMP_CALI_ENP (REG.1C8[5]) = 1
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcSET_BIT(u4value, POS_IMPCAL_IMP_CALI_ENP);
    mcCLR_BIT(u4value, POS_IMPCAL_IMP_CALI_ENN);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

    // delay 10ns
    mcDELAY_US(1);

    drvp_status = 0;
    for(ii = 0 ; ii <=15; ii ++)
    {
        // Set R_DMIMPDRVP (REG.1C8[11:8])
        ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
        mcSET_FIELD(u4value, ii, MASK_IMPCAL_IMPDRVP, POS_IMPCAL_IMPDRVP);
        ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

        // delay 10ns
        mcDELAY_US(1);

        // Set R_DMIMP_PDP (REG.1C8[7]) = 1
        ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
        mcSET_BIT(u4value, POS_IMPCAL_IMPPDP);
        ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

        // delay 10ns
        mcDELAY_US(1);

        // Set R_DMIMP_PDP (REG.1C8[7]) = 0
        ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
        mcCLR_BIT(u4value, POS_IMPCAL_IMPPDP);
        ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

        // delay 10ns
        mcDELAY_US(1);

        // Check DMCMPOUT (REG.3DC[31])
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DLLSTATUS0), &u4value);
        //mcSHOW_DBG_MSG2(("[Imp Calibration] DRVP: %d, CMPOUT: %d\n", ii, mcTEST_BIT(u4value, POS_DLLSTATUS0_CMPOT)>>POS_DLLSTATUS0_CMPOT));
        if (mcTEST_BIT(u4value, POS_DLLSTATUS0_CMPOT) == 0)
        {
            if (drvp_status == 1)
            {
               
                drvp = ii;
		        mcSHOW_DBG_MSG(("[Imp Calibration] Calibration done 0-1-0 case...DRVP:%d\n", drvp));
                drvp_status = 2;

                break;
            }
            else if (drvp_status == 0)
            {
                // 0-0: to next DRVP
            }
            else
            {
                // wrong status
                mcSHOW_ERR_MSG(("[Imp Calibration] DRVP error: wrong status=%d!!\n", drvp_status));
                break;
            }
        }
        else
        {
            if (drvp_status == 1)
            {
                // 1-1: calibration done
                drvp = ii -1;
                mcSHOW_DBG_MSG(("[Imp Calibration] Calibration done...DRVP:%d\n", drvp));
                mcFPRINTF((fp_A60808, "[Imp Calibration] Calibration done...DRVP:%d\n", drvp));

                // Set R_DMIMPDRVP (REG.1C8[11:8])
                ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
                mcSET_FIELD(u4value, drvp, MASK_IMPCAL_IMPDRVP, POS_IMPCAL_IMPDRVP);
                ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

                drvp_status = 2;
                break;
            }
            else if (drvp_status == 0)
            {
                // 0-1: calibration to be done
                drvp_status = 1;
                //mcSHOW_DBG_MSG2(("[Imp Calibration] Calibration to be done...DRVP:%d\n", ii));
                //mcFPRINTF((fp_A60808, "[Imp Calibration] Calibration to be done...DRVP:%d\n", ii));
            }
            else
            {
                // wrong status
                mcSHOW_ERR_MSG(("[Imp Calibration] DRVP error: wrong status=%d!!\n", drvp_status));
                break;
            }
        }
    }

    if (ii == 16)
    {
        drvp_status = -2;
        mcSHOW_ERR_MSG(("[Imp Calibration] NO VALID DRVP!!\n"));
    #ifdef DDR_FT_LOAD_BOARD
        LoadBoardShowResult(FLAG_IMPEDANCE_CALIBRATION, FLAG_CALIBRATION_FAIL, 0, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);    
    #endif
    }

    // 3.DRVN calibration
    // R_DMIMP_CALI_ENN (REG.1C8[4]) = 1
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcCLR_BIT(u4value, POS_IMPCAL_IMP_CALI_ENP);
    mcSET_BIT(u4value, POS_IMPCAL_IMP_CALI_ENN);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

    // delay 10ns
    mcDELAY_US(1);

    drvn_status = 0;
    for(ii = 0 ; ii <=15; ii ++)
    {
        // Set R_DMIMPDRVN (REG.1C8[15:12])
        ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
        mcSET_FIELD(u4value, ii, MASK_IMPCAL_IMPDRVN, POS_IMPCAL_IMPDRVN);
        ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

        // delay 10ns
        mcDELAY_US(1);

        // Set R_DMIMP_PDN (REG.1C8[6]) = 1
        ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
        mcSET_BIT(u4value, POS_IMPCAL_IMPPDN);
        ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

        // delay 10ns
        mcDELAY_US(1);

        // Set R_DMIMP_PDN (REG.1C8[6]) = 0
        ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
        mcCLR_BIT(u4value, POS_IMPCAL_IMPPDN);
        ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

        // delay 10ns
        mcDELAY_US(1);

        // Check DMCMPOUTN (REG.3DC[30])
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DLLSTATUS0), &u4value);
        //mcSHOW_DBG_MSG2(("[Imp Calibration] DRVN: %d, CMPOUT: %d\n", ii, mcTEST_BIT(u4value, POS_DLLSTATUS0_CMPOUTN)>>POS_DLLSTATUS0_CMPOUTN));
        if (mcTEST_BIT(u4value, POS_DLLSTATUS0_CMPOUTN) == 0)
        {
            if (drvn_status == 1)
            {        
                if (ii >=1)
                {
                    drvn = ii -1;
                }
                else
                {
                    drvn = 0;
                }
                mcSHOW_DBG_MSG(("[Imp Calibration] Calibration done 0-1-1 case!...DRVN:%d\n", drvn));
                // Set R_DMIMPDRVN (REG.1C8[15:12])
                ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
                mcSET_FIELD(u4value, drvn, MASK_IMPCAL_IMPDRVN, POS_IMPCAL_IMPDRVN);
                ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

                drvn_status = 2;
                break;
            }
            else if (drvn_status == 0)
            {
                // 0-0: to next DRVN
            }
            else
            {
                // wrong status
                mcSHOW_ERR_MSG(("[Imp Calibration] DRVN error: wrong status=%d!!\n", drvn_status));
                break;
            }
        }
        else
        {
            if (drvn_status == 1)
            {
                // 1-1: calibration done
                if (ii >=2)
                {
                    drvn = ii -2;
                }
                else
                {
                    drvn = 0;
                }
                mcSHOW_DBG_MSG(("[Imp Calibration] Calibration done...DRVN:%d\n", drvn));
                mcFPRINTF((fp_A60808, "[Imp Calibration] Calibration done...DRVN:%d\n", drvn));

                // Set R_DMIMPDRVN (REG.1C8[15:12])
                ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
                mcSET_FIELD(u4value, drvn, MASK_IMPCAL_IMPDRVN, POS_IMPCAL_IMPDRVN);
                ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

                drvn_status = 2;
                break;
            }
            else if (drvn_status == 0)
            {
                // 0-1: calibration to be done
                drvn_status = 1;
                //mcSHOW_DBG_MSG2(("[Imp Calibration] Calibration to be done...DRVN:%d\n", ii));
                //mcFPRINTF((fp_A60808, "[Imp Calibration] Calibration to be done...DRVN:%d\n", ii));
            }
            else
            {
                // wrong status
                mcSHOW_ERR_MSG(("[Imp Calibration] DRVN error: wrong status=%d!!\n", drvn_status));
                break;
            }
        }
    }

    if (ii == 16)
    {
        drvn_status = -2;
        mcSHOW_ERR_MSG(("[Imp Calibration] NO VALID DRVN!!\n"));
    #ifdef DDR_FT_LOAD_BOARD
        LoadBoardShowResult(FLAG_IMPEDANCE_CALIBRATION, FLAG_CALIBRATION_FAIL, 1, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);    
    #endif
    }

    // 4. Set calibration result to output driving
    if (apply == 1)
    {
        if ((drvp_status == 2) && (drvn_status == 2))
        {        
SET_DRIVING:        
#ifdef  MAX_DRIVING	
	    drvp = drvn = 0x0f;
#endif
  
            //DQS, DQ
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRVCTL0), &u4value);
            mcSET_FIELD(u4value, drvp, MASK_DRVCTL0_DQSDRVP, POS_DRVCTL0_DQSDRVP);
            mcSET_FIELD(u4value, drvn, MASK_DRVCTL0_DQSDRVN, POS_DRVCTL0_DQSDRVN);
            mcSET_FIELD(u4value, drvp, MASK_DRVCTL0_DQDRVP, POS_DRVCTL0_DQDRVP);
            mcSET_FIELD(u4value, drvn, MASK_DRVCTL0_DQDRVN, POS_DRVCTL0_DQDRVN);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRVCTL0), u4value);
            mcSHOW_DBG_MSG2(("[Imp Calibration] Reg: 0x%x, Val: 0x%x\n", DRAMC_REG_DRVCTL0, u4value));

            // CLK, CMD
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRVCTL1), &u4value);
            mcSET_FIELD(u4value, drvp, MASK_DRVCTL1_CLKDRVP, POS_DRVCTL1_CLKDRVP);
            mcSET_FIELD(u4value, drvn, MASK_DRVCTL1_CLKDRVN, POS_DRVCTL1_CLKDRVN);
            mcSET_FIELD(u4value, drvp, MASK_DRVCTL1_CMDDRVP, POS_DRVCTL1_CMDDRVP);
            mcSET_FIELD(u4value, drvn, MASK_DRVCTL1_CMDDRVN, POS_DRVCTL1_CMDDRVN);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRVCTL1), u4value);
            mcSHOW_DBG_MSG2(("[Imp Calibration] Reg: 0x%x, Val: 0x%x\n", DRAMC_REG_DRVCTL1, u4value));           

            // DQ_2, CMD_2
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IODRV4), &u4value);
            mcSET_FIELD(u4value, drvp, MASK_IODRV4_DQDRVP_2, POS_IODRV4_DQDRVP_2);
            mcSET_FIELD(u4value, drvn, MASK_IODRV4_DQDRVN_2, POS_IODRV4_DQDRVN_2);
            mcSET_FIELD(u4value, drvp, MASK_IODRV4_CMDDRVP_2, POS_IODRV4_CMDDRVP_2);
            mcSET_FIELD(u4value, drvn, MASK_IODRV4_CMDDRVN_2, POS_IODRV4_CMDDRVN_2);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IODRV4), u4value);
            mcSHOW_DBG_MSG2(("[Imp Calibration] Reg: 0x%x, Val: 0x%x\n", DRAMC_REG_IODRV4, u4value));

            if (p->channel == CHANNEL_A)
            {
            	p->channel  = CHANNEL_B;
            	goto SET_DRIVING;
            }
            else
            {
            	p->channel  = CHANNEL_A;
            }
        }
    }
    
    // 5. restore settings
    // R_DMMIOCKCTRLOFF (REG.1DC[26])
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), u4backup_reg);

    // R_DMIMP_CALI_ENP (REG.1C8[5]) = 0
    // R_DMIMP_CALI_ENN (REG.1C8[4]) = 0
    // R_DMIMPCALI_EN (REG.1C8[0]) = 0
    // R_DMIMPDRVP / R_DMIMPDRVN = 0 to avoid current leakage
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcCLR_BIT(u4value, POS_IMPCAL_IMP_CALI_ENP);
    mcCLR_BIT(u4value, POS_IMPCAL_IMP_CALI_ENN);
    mcCLR_BIT(u4value, POS_IMPCAL_IMPCALI_EN);
    mcSET_FIELD(u4value, 0, MASK_IMPCAL_IMPDRVP, POS_IMPCAL_IMPDRVP);
    mcSET_FIELD(u4value, 0, MASK_IMPCAL_IMPDRVN, POS_IMPCAL_IMPDRVN);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

    // DRVREF (REG.100[24]) = 1: change will be apply during refresh
#ifndef DISABLE_DRVREF
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), &u4value);
    mcSET_BIT(u4value, POS_OCDK_DRVREF);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), u4value);
#endif    

    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));        
    }

    if ((ucstatus != 0) || (drvp_status != 2) || (drvn_status != 2))
    {
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }            
}

//-------------------------------------------------------------------------
/** DramcHwImpedanceCal
 *  start TX OCD impedance calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  apply           (U8): 0 don't apply the register we set  1 apply the register we set ,default don't apply.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
// only for ES validation
DRAM_STATUS_T DramcHwImpedanceCal(DRAMC_CTX_T *p)
{
    U8 ucstatus =0, uctemp;
    U32 u4value;
        
    mcSHOW_DBG_MSG(("[HW Imp Calibr] Start HW impedance calibration...\n"));
    mcFPRINTF((fp_A60808, "[HW Imp Calibr] Start HW impedance calibration...\n"));
            
    // DRVREF (REG.100[24]) = 0: change will be apply directly 
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), &u4value);
    mcCLR_BIT(u4value, POS_OCDK_DRVREF);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), u4value);
    
    // Set R_DMIMPCAL_HW (REG.1C8[1]) = 1 (HW)
    // Set R_DMIMPCAL_CHKCYCLE (REG.1C8[19][3:2]) = 3
    // Set R_DMIMPCAL_CALEN_CYCLE (REG.1C8[18:16]) = 3
    // Set R_DMIMPCAL_CALICNT (REG.1C8[31:28]) = 7
    // Set R_DMIMPCALCNT (REG.1C8[27:20]) = 0 @ init; 0xf @ run-time
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcSET_BIT(u4value, 1);
    mcSET_FIELD(u4value, 0x3, 0x0000000c, 2);
    mcSET_FIELD(u4value, 0x3, 0x00070000, 16);
    mcSET_FIELD(u4value, 0x7, 0xf0000000, 28);
    mcSET_FIELD(u4value, 0x0, 0x0ff00000, 20);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

    // Set R_DMIMPCALI_EN (REG.1C8[0]) = 1
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcSET_BIT(u4value, 0);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);
            
    // Check calibrated result
    // only show in drvp_status in A60808, not sure if HW has filled into driving (Rome will have registers to read)
    // ~1.5us
    mcDELAY_US(5);

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DLLSTATUS0), &u4value);
    uctemp = (U8) mcGET_FIELD(u4value, 0x3c000000, 26);
    mcSHOW_DBG_MSG(("[HW Imp Calibr] drvp_save  : %d\n",uctemp));
    mcFPRINTF((fp_A60808, "[HW Imp Calibr] drvp_save  : %2d\n",uctemp));
    uctemp = (U8) mcGET_FIELD(u4value, 0x03c00000, 22);
    mcSHOW_DBG_MSG(("[HW Imp Calibr] drvn_save  : %d\n",uctemp));
    mcFPRINTF((fp_A60808, "[HW Imp Calibr] drvn_save  : %2d\n",uctemp));
    uctemp = (U8) mcGET_FIELD(u4value, 0x003c0000, 18);
    mcSHOW_DBG_MSG(("[HW Imp Calibr] drvp_save_2: %d\n",uctemp));
    mcFPRINTF((fp_A60808, "[HW Imp Calibr] drvp_save_2: %2d\n",uctemp));
    uctemp = (U8) mcGET_FIELD(u4value, 0x0003c000, 14);
    mcSHOW_DBG_MSG(("[HW Imp Calibr] drvp_save_2: %d\n",uctemp));
    mcFPRINTF((fp_A60808, "[HW Imp Calibr] drvp_save_2: %2d\n",uctemp));
        
    // restore settings
    // R_DMIMPCALI_EN (REG.1C8[0]) = 0
    ucstatus |= ucDramC_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), &u4value);
    mcCLR_BIT(u4value, POS_IMPCAL_IMPCALI_EN);
    ucstatus |= ucDramC_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_IMPCAL), u4value);

    // DRVREF (REG.100[24]) = 1: change will be apply during refresh
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), &u4value);
    mcSET_BIT(u4value, POS_OCDK_DRVREF);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_OCDK), u4value);

    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));        
    }

    if (ucstatus != 0)
    {
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }            
}

// LPDDR DQ -> PHY DQ mapping
const U32 uiLPDDR_PHY_Mapping_POP_CHA[32] = {16, 17, 18, 19, 20, 21, 22, 23, 15, 14, 13, 12, 11, 10, 9, 8,
                                                                                  31, 30, 29, 28, 27, 26, 25, 24, 0, 1, 2, 3, 4, 5, 6, 7};
const U32 uiLPDDR_PHY_Mapping_POP_CHB[32] = {15, 14, 13, 12, 11, 10, 9, 8, 16, 17, 18, 19, 20, 21, 22, 23,
	                                                                           0, 1, 2, 3, 4, 5, 6, 7, 31, 30, 29, 28, 27, 26, 25, 24};
//-------------------------------------------------------------------------
/** DramcCATraining
 *  start the calibrate the skew between Clk pin and CAx pins.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
#define MAX_CLKO_DELAY         15
#define CATRAINING_NUM        10    
DRAM_STATUS_T DramcCATraining(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 uiTemp, uiDelay, uiFinishCount, uiCA, uiMR41=1, uiReg1DCh, uiReg1E0h, uiRisingEdge, uiFallingEdge;
    U32 u4prv_register_0fc, u4prv_register_044, u4prv_register_63c;
   
    S8 iCenter[CATRAINING_NUM],  iFirstClkPass[CATRAINING_NUM], iLastClkPass[CATRAINING_NUM];
    S8 iFirstCAPass[CATRAINING_NUM], iLastCAPass[CATRAINING_NUM], iMaxCenter;
    S8 iCAShift[CATRAINING_NUM];
#ifdef CKE_CS_DLY_SETTING
    S8 CAShift_Avg = 0;
#endif
    S8 iBestFirstClkPass[CATRAINING_NUM], iBestLastClkPass[CATRAINING_NUM];
    S8 iBestFirstCAPass[CATRAINING_NUM], iBestLastCAPass[CATRAINING_NUM];
    S32 iPass, iClkWinSize, iCAWinSize;
    U32 *uiLPDDR_PHY_Mapping;
    
    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

    if (p->dram_type != TYPE_LPDDR3)
    {
        mcSHOW_ERR_MSG(("Wrong DRAM TYPE. Only support LPDDR3 in CA training!!\n"));
        return DRAM_FAIL;
    }

    // Disable clock gating to prevent DDRPHY enter idle.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &uiTemp);
    u4prv_register_63c = uiTemp;
    mcCLR_BIT(uiTemp, 2);
    mcCLR_BIT(uiTemp, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), uiTemp);

    // Edward : no idea why TXP>1 will cause CA training fail. Now set it after CA training.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0fc), &u4prv_register_0fc);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), (u4prv_register_0fc & 0x8fffffff));

    //disable auto refresh: REFCNT_FR_CLK = 0 (0x1dc[23:16]), ADVREFEN = 0 (0x44[30])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &uiTemp);
    u4prv_register_044 = uiTemp;
    mcCLR_BIT(uiTemp, POS_TEST2_3_ADVREFEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), uiTemp);

    if (p->channel == CHANNEL_A)
    {
        uiLPDDR_PHY_Mapping = (U32 *)uiLPDDR_PHY_Mapping_POP_CHA;
    }
    else
    {
        uiLPDDR_PHY_Mapping = (U32 *)uiLPDDR_PHY_Mapping_POP_CHB;
    }

    for (uiCA=0; uiCA<CATRAINING_NUM; uiCA++) 
    {
        iLastClkPass[uiCA] = iLastCAPass[uiCA] = -1;
        iFirstClkPass[uiCA] = iFirstCAPass[uiCA] = -1;
        iBestLastClkPass[uiCA] = iBestLastCAPass[uiCA] = -1;
        iBestFirstClkPass[uiCA] = iBestFirstCAPass[uiCA] = -1;        
    }
    
    // Sweep clock output delay first.
       
    // Keep DQ input always ON.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xd8), &uiTemp);
    uiTemp |= 0x0000f000;
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xd8), uiTemp);

    // Let MIO_CK always ON.
    // Disable auto refresh: REFCNT_FR_CLK = 0 (0x1dc[23:16])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &uiTemp);
    uiReg1DCh = uiTemp;
    mcSET_BIT(uiTemp, 26);
    mcCLR_MASK(uiTemp, MASK_DRAMC_PD_CTRL_REFCNT_FR_CLK);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), uiTemp);

#if 0
    //FIXDQIEN = 1111 (0xd8[15:12])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &uiTemp);
    uiRegD8h = uiTemp;
    mcSET_MASK(uiTemp, MASK_MCKDLY_FIXDQIEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), uiTemp);
#endif

    //Enable DQ_O1, SELO1ASO=1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &uiTemp);
    uiReg1E0h = uiTemp;
    mcSET_BIT(uiTemp, POS_LPDDR2_SELO1ASO);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), uiTemp);

    CA_TRAINING_BEGIN:

    // Set CA0~CA3, CA5~CA8 output delay to 0.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1a8), 0);    // CA0~CA3
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ac), 0);    // CA4~CA7
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1b0), 0);    // CA8~CA11
    
    // CS extent enable (need DRAM to support)
    // for testing

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &uiTemp);
    mcSET_BIT(uiTemp, 13);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x80), uiTemp);


    // Enter MR 41/MR48
    // Set MA & OP.
    if (uiMR41) 
    {
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x88), 0x00a40029);    
    } 
    else     
    {
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x88), 0x00c00030);    
    }
    // Hold the CA bus stable for at least one cycle.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x4c), &uiTemp);
    mcSET_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);
    // MRW
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e4), &uiTemp);
    mcSET_BIT(uiTemp, 0);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), uiTemp);
    mcDELAY_US(1);
    mcCLR_BIT(uiTemp, 0);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), uiTemp);
    // Disable CA bus stable.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x4c), &uiTemp);
    mcCLR_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);

    // Wait tCACKEL(10 tck) before CKE low
    mcDELAY_US(1);

    // CKE low
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe4), &uiTemp);
    mcSET_BIT(uiTemp, 3);
    mcCLR_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), uiTemp);
    
    // Set CA0~CA3, CA5~CA8 rising/falling golden value.
    if (uiMR41) 
    {
        //  01010101b -> 10101010b : Golden value = 1001100110011001b=0x9999
        //  11111111b -> 00000000b : Golden value = 0101010101010101b=0x5555
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x50), 0x55555555);
    }
    else
    {
        //  00010001b -> 00000000b : Golden value = 0000000100000001b=0x0101
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x50), 0x01010101);
    }

    // Wait tCAENT(10 tck) before pattern output
    mcDELAY_US(1);

    // Delay clock output delay to do CA training in order to get the pass window.
    uiFinishCount = 0;
    for (uiDelay=0; uiDelay<=MAX_CLKO_DELAY; uiDelay++) 
    {     
        //DramcEnterSelfRefresh(p, 1);           
        // Set Clk output delay
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &uiTemp);
        mcSET_FIELD(uiTemp, uiDelay, 0x0f000000, 24);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), uiTemp);    
        //DramcEnterSelfRefresh(p, 0);
        
        // CA training pattern output enable
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x4c), &uiTemp);
        mcSET_BIT(uiTemp, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);
        // delay 2 DRAM clock cycle
        mcDELAY_US(1);
        mcCLR_BIT(uiTemp, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);    
        
        // Wait tADR(20ns) before CA sampled values available in DQ.
        mcDELAY_US(1);

        // Get DQ value.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x380), &uiTemp);

        //mcSHOW_DBG_MSG2(("[CA Training] CLK delay -- %d, MR41 -- %d, DQ_O1 -- 0x%x\n", uiDelay, uiMR41, uiTemp));

        // Compare with golden value.
        if (uiMR41) 
        {
            for (uiCA=0; uiCA<CATRAINING_NUM; uiCA++) 
            {
                if ((uiCA==4) || (uiCA==9)) 
                {
                    continue;
                }
                if ( (iFirstClkPass[uiCA]==-1) || (iLastClkPass[uiCA]==-1)) 
                {
                    if (uiCA<4) 
                    {
                        uiRisingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[uiCA<<1]);
                        uiFallingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA<<1)+1]); 
                    }
                    else
                    {
                        uiRisingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA-1)<<1]);
                        uiFallingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[((uiCA-1)<<1)+1]); 
                    }
                    // Check 1->0 transition.
                    if ((uiRisingEdge!=0) && (uiFallingEdge==0)) 
                    {
                        iPass = 1;
                    } 
                    else
                    {
                        iPass = 0;
                    }


                    if (iFirstClkPass[uiCA]==-1)
                    {
                        if (iPass == 1) 
                        {
                            iFirstClkPass[uiCA] = uiDelay;
                        }
                    }
                    else
                    {
                        if (iLastClkPass[uiCA]==-1)
                        {                    
                            if (iPass == 0) 
                            {
                                iLastClkPass[uiCA] = uiDelay-1;
                                uiFinishCount++;
                            }
                            else
                            {
                                if (uiDelay==MAX_CLKO_DELAY)
                                {
                                    iLastClkPass[uiCA] = uiDelay;
                                    uiFinishCount++;
                                }
                            }
                            if (iLastClkPass[uiCA]!=-1)
                            {
                            	if ( (iLastClkPass[uiCA]-iFirstClkPass[uiCA]) > (iBestLastClkPass[uiCA]-iBestFirstClkPass[uiCA]))
                            	{
                            		iBestLastClkPass[uiCA] = iLastClkPass[uiCA];
                            		iBestFirstClkPass[uiCA] = iFirstClkPass[uiCA];
                            	}
                            	iLastClkPass[uiCA] = iFirstClkPass[uiCA] = -1;
                            }
                        }
                    }
                }
            }

            // Wait tCACD(22clk) before output CA pattern to DDR again..
            mcDELAY_US(1);        
            
            //if (uiFinishCount==8) {
            //    break;
            //} 
        }
        else
        {
            // MR 48 
            uiCA = 4;
            
MR48_CHECKRESULT_CLK:            
    
            if ((iFirstClkPass[uiCA]==-1) || (iLastClkPass[uiCA]==-1))
            {
                uiRisingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA==4) ? 0 : 8]);
                uiFallingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA==4) ? 1 : 9]); 

                // Check 1->0 transition.
                if ((uiRisingEdge!=0) && (uiFallingEdge==0)) 
                {
                    iPass = 1;
                }            
                else
                {
                    iPass = 0;
                }
                if (iFirstClkPass[uiCA]==-1)
                {
                    if (iPass==1)
                    {
                        iFirstClkPass[uiCA] = uiDelay;
                    }
                }
                else
                {
                    if (iLastClkPass[uiCA]==-1)
                    {
                        if (iPass==0)
                        {
                            iLastClkPass[uiCA] = uiDelay-1;
                            uiFinishCount++;
                        }
                        else
                        {
                            if (uiDelay==MAX_CLKO_DELAY)
                            {
                                iLastClkPass[uiCA] = uiDelay;
                                uiFinishCount++;                            
                            }
                        }
                        if (iLastClkPass[uiCA]!=-1)
                        {
                        	if ( (iLastClkPass[uiCA]-iFirstClkPass[uiCA]) > (iBestLastClkPass[uiCA]-iBestFirstClkPass[uiCA]))
                        	{
                        		iBestLastClkPass[uiCA] = iLastClkPass[uiCA];
                        		iBestFirstClkPass[uiCA] = iFirstClkPass[uiCA];
                        	}
                        	iLastClkPass[uiCA] = iFirstClkPass[uiCA] = -1;
                        }                        
                    }
                }
            }

            if (uiCA==4)
            {
                uiCA=9;
                goto MR48_CHECKRESULT_CLK;
            }
            
            // Wait tCACD(22clk) before output CA pattern to DDR again..
            mcDELAY_US(1);                    

            //if (uiFinishCount==2) {
            //    break;
            //} 
        }
    }

    //DramcEnterSelfRefresh(p, 1);    
    // Set Clk output delay to 0.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &uiTemp);
    mcSET_FIELD(uiTemp, 0, 0x0f000000, 24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), uiTemp);  
    //DramcEnterSelfRefresh(p, 0);

    // Delay CA output delay to do CA training in order to get the pass window.
    uiFinishCount = 0;
    for (uiDelay=0; uiDelay<=MAX_CLKO_DELAY; uiDelay++) 
    {
        // Set CA0~CA3, CA5~CA8 output delay.
        uiTemp = uiDelay | (uiDelay<<8) | (uiDelay<<16) | (uiDelay<<24);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1a8), uiTemp);    // CA0~CA3
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ac), uiTemp);    // CA4~CA7
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1b0), uiTemp);    // CA8~CA11
        
        // CA training pattern output enable
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x4c), &uiTemp);
        mcSET_BIT(uiTemp, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);
        // delay 2 DRAM clock cycle
        mcDELAY_US(1);
        mcCLR_BIT(uiTemp, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);    
        
        // Wait tADR(20ns) before CA sampled values available in DQ.
        mcDELAY_US(1);

        // Get DQ value.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x380), &uiTemp);

        //mcSHOW_DBG_MSG2(("[CA Training] CA delay -- %d, MR41 -- %d, DQ_O1 -- 0x%x\n", uiDelay, uiMR41, uiTemp));

        // Compare with golden value.
        if (uiMR41) 
        {
            for (uiCA=0; uiCA<CATRAINING_NUM; uiCA++) 
            {
                if ((uiCA==4) || (uiCA==9)) 
                {
                    continue;
                }
                if ( (iFirstCAPass[uiCA]==-1) || (iLastCAPass[uiCA]==-1)) 
                {
                    if (uiCA<4) 
                    {
                        uiRisingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[uiCA<<1]);
                        uiFallingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA<<1)+1]); 
                    }
                    else
                    {
                        uiRisingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA-1)<<1]);
                        uiFallingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[((uiCA-1)<<1)+1]); 
                    }
                    // Check 1->0 transition.
                    if ((uiRisingEdge!=0) && (uiFallingEdge==0)) 
                    {
                        iPass = 1;
                    } 
                    else
                    {
                        iPass = 0;
                    }

                    if (iFirstCAPass[uiCA]==-1)
                    {
                        if (iPass == 1) 
                        {
                            iFirstCAPass[uiCA] = uiDelay;
                        }
                    }
                    else
                    {
                        if (iLastCAPass[uiCA]==-1)
                        {                    
                            if (iPass == 0) 
                            {
                                iLastCAPass[uiCA] = uiDelay-1;
                                uiFinishCount++;
                            }
                            else
                            {
                                if (uiDelay==MAX_CLKO_DELAY)
                                {
                                    iLastCAPass[uiCA] = uiDelay;
                                    uiFinishCount++;
                                }
                            }
                            if (iLastCAPass[uiCA]!=-1)
                            {
	                            if ( (iLastCAPass[uiCA]-iFirstCAPass[uiCA]) > (iBestLastCAPass[uiCA]-iBestFirstCAPass[uiCA]) )
	                            {
	                            	iBestLastCAPass[uiCA] = iLastCAPass[uiCA];
	                            	iBestFirstCAPass[uiCA] = iFirstCAPass[uiCA];
	                            }
	                            iLastCAPass[uiCA] = iFirstCAPass[uiCA] = -1;
                            }
                        }
                    }
                }
            }
        
            // Wait tCACD(22clk) before output CA pattern to DDR again..
            mcDELAY_US(1);        
            
            //if (uiFinishCount==8) {
            //    break;
            //} 
        }
        else
        {
            // MR 48
            uiCA = 4;
            
MR48_CHECKRESULT_CA:            
    
            if ((iFirstCAPass[uiCA]==-1) || (iLastCAPass[uiCA]==-1))
            {
                uiRisingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA==4) ? 0 : 8]);
                uiFallingEdge = uiTemp & (0x01 << uiLPDDR_PHY_Mapping[(uiCA==4) ? 1 : 9]); 

                // Check 1->0 transition.
                if ((uiRisingEdge!=0) && (uiFallingEdge==0)) 
                {
                    iPass = 1;
                }            
                else
                {
                    iPass = 0;
                }
        
                if (iFirstCAPass[uiCA]==-1)
                {
                    if (iPass==1)
                    {
                        iFirstCAPass[uiCA] = uiDelay;
                    }
                }
                else
                {
                    if (iLastCAPass[uiCA]==-1)
                    {
                        if (iPass==0)
                        {
                            iLastCAPass[uiCA] = uiDelay-1;
                            uiFinishCount++;
                        }
                        else
                        {
                            if (uiDelay==MAX_CLKO_DELAY)
                            {
                                iLastCAPass[uiCA] = uiDelay;
                                uiFinishCount++;                            
                            }
                        }
                        if (iLastCAPass[uiCA]!=-1)
                        {
                            if ( (iLastCAPass[uiCA]-iFirstCAPass[uiCA]) > (iBestLastCAPass[uiCA]-iBestFirstCAPass[uiCA]) )
                            {
                            	iBestLastCAPass[uiCA] = iLastCAPass[uiCA];
                            	iBestFirstCAPass[uiCA] = iFirstCAPass[uiCA];
                            }
                            iLastCAPass[uiCA] = iFirstCAPass[uiCA] = -1;
                        }                        
                    }
                }
            }

            if (uiCA==4)
            {
                uiCA=9;
                goto MR48_CHECKRESULT_CA;
            }

            // Wait tCACD(22clk) before output CA pattern to DDR again..
            mcDELAY_US(1);                    

            //if (uiFinishCount==2) {
            //    break;
            //}             
        }
    }    

    // CS extent disable
    // for testing
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &uiTemp);
    mcCLR_BIT(uiTemp, 13);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x80), uiTemp);   

    if (uiMR41==0) 
    {
        // Disable fix DQ input enable.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xd8), &uiTemp);
        uiTemp = uiTemp & 0xffff0fff;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xd8), uiTemp);
    }

    // Wait tCACKEN (10ck)
    mcDELAY_US(1);        

    // CKE high
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe4), &uiTemp);
    mcCLR_BIT(uiTemp, 3);
    mcSET_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), uiTemp);

    if (uiMR41) 
    {
        uiMR41 = 0;
        goto CA_TRAINING_BEGIN;
    }

    // CS extent enable
    // for testing
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &uiTemp);
    mcSET_BIT(uiTemp, 13);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x80), uiTemp); 

    // MR42 to leave CA training.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x88), 0x00a8002a);    
    // Hold the CA bus stable for at least one cycle.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x4c), &uiTemp);
    mcSET_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);
    // MRW
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e4), &uiTemp);
    mcSET_BIT(uiTemp, 0);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), uiTemp);
    mcDELAY_US(1);
    mcCLR_BIT(uiTemp, 0);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), uiTemp);    
    // Disable the hold the CA bus stable for at least one cycle.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x4c), &uiTemp);
    mcCLR_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x4c), uiTemp);

    // CS extent disable
    // for testing
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &uiTemp);
    mcCLR_BIT(uiTemp, 13);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x80), uiTemp);   
    
    // Disable CKE high
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe4), &uiTemp);
    mcCLR_BIT(uiTemp, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), uiTemp);

    // Calculate the middle range & max middle.
    mcSHOW_DBG_MSG3(("=========================================\n"));
    mcSHOW_DBG_MSG3(("1. [Channel %d] CA training window before adjustment. \n", p->channel));
    mcSHOW_DBG_MSG3(("x=Pass window CA(max~min) Clk(min~max) center. \n"));
    mcSHOW_DBG_MSG3(("y=CA0~CA9\n"));
    mcSHOW_DBG_MSG3(("=========================================\n"));

    mcFPRINTF((fp_A60808, "=========================================\n"));
    mcFPRINTF((fp_A60808, "1. CA training window before adjustment.\n"));
    mcFPRINTF((fp_A60808, "x=Pass window CA(max~min) Clk(min~max) center. \n"));
    mcFPRINTF((fp_A60808, "y=CA0~CA9\n"));
    mcFPRINTF((fp_A60808, "=========================================\n"));

    iMaxCenter = 0 - MAX_CLKO_DELAY;
    for (uiCA=0; uiCA<CATRAINING_NUM; uiCA++)
    {
        iClkWinSize = iBestLastClkPass[uiCA] - iBestFirstClkPass[uiCA];
        iCAWinSize = iBestLastCAPass[uiCA] - iBestFirstCAPass[uiCA];
        if (iClkWinSize >= iCAWinSize)
        {
            if (iCAWinSize>0)
            {
                iCenter[uiCA] =  (iClkWinSize - iCAWinSize)>>1;
            }
            else
            {
                iCenter[uiCA] =  iBestFirstClkPass[uiCA] + (iClkWinSize>>1);
            }
        }
        else
        {
            if (iClkWinSize>0)
            {
                iCenter[uiCA] =  (iClkWinSize - iCAWinSize)/2;
            }
            else
            {
                iCenter[uiCA] =  0-(iBestFirstCAPass[uiCA] + (iCAWinSize>>1));
            }
        }
        mcSHOW_DBG_MSG3(("CA%d     CA(%d~%d) Clk(%d~%d) %d\n", uiCA, iBestLastCAPass[uiCA],  iBestFirstCAPass[uiCA],
            iBestFirstClkPass[uiCA], iBestLastClkPass[uiCA], iCenter[uiCA]));

        mcFPRINTF((fp_A60808, "CA%d     CA(%d~%d) Clk(%d~%d) %d\n", uiCA, iBestLastCAPass[uiCA],  iBestFirstCAPass[uiCA],
            iBestFirstClkPass[uiCA], iBestLastClkPass[uiCA], iCenter[uiCA]));

        if (iCenter[uiCA]  > iMaxCenter)
        {
            iMaxCenter = iCenter[uiCA];
        }
#ifdef EYE_SCAN
	EyeScanWin[uiCA].ucsetup_pass_number = iCAWinSize;
	EyeScanWin[uiCA].uchold_pass_number = iClkWinSize;
#endif             

    #ifdef DDR_FT_LOAD_BOARD
        if ((iCAWinSize==0) && (iClkWinSize == 0))
        {
            LoadBoardShowResult(FLAG_CA_CALIBRATION, FLAG_CALIBRATION_FAIL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
            while(1);    
        }
        else if ((iCAWinSize+iClkWinSize)<=CA_TRAINING_BOUND)
        {
            LoadBoardShowResult(FLAG_CA_CALIBRATION, FLAG_WINDOW_TOO_SMALL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
            while(1);  
        }
    #endif
    }
    mcSHOW_DBG_MSG3(("=========================================\n"));
    mcSHOW_DBG_MSG3(("Max center = %d\n\n", iMaxCenter));
    mcFPRINTF((fp_A60808, "=========================================\n"));
    mcFPRINTF((fp_A60808, "Max center = %d\n\n", iMaxCenter));

    // Calculate the shift value
    mcSHOW_DBG_MSG3(("=========================================\n"));
    mcSHOW_DBG_MSG3(("2. [Channel %d]CA training window after adjustment.\n", p->channel));
    mcSHOW_DBG_MSG3(("x=CA shift     Adjusted Pass window CA(max~min) Clk(min~max) center.\n"));
    mcSHOW_DBG_MSG3(("y=CA0~CA9\n"));
    mcSHOW_DBG_MSG3(("=========================================\n"));    

    mcFPRINTF((fp_A60808, "=========================================\n"));
    mcFPRINTF((fp_A60808, "2. [Channel %d] CA training window after adjustment.\n", p->channel));
    mcFPRINTF((fp_A60808, "x=CA shift     Adjusted Pass window CA(max~min) Clk(min~max) center.\n"));
    mcFPRINTF((fp_A60808, "y=CA0~CA9\n"));
    mcFPRINTF((fp_A60808, "=========================================\n"));    


    if (iMaxCenter < 0)
    {
        // Clk output delay could not be negative. Need to adjust into 0.
        iMaxCenter = 0;
        mcSHOW_DBG_MSG3(("Max center < 0. Adjust to 0. \n\n"));
    }

    for (uiCA=0; uiCA<CATRAINING_NUM; uiCA++)
    {
        iCAShift[uiCA] = iMaxCenter - iCenter[uiCA]+CATRAINING_STEP;
        if (iCAShift[uiCA]>=MAX_CLKO_DELAY)
        {
            iCAShift[uiCA] = MAX_CLKO_DELAY;
        }
#ifdef CKE_CS_DLY_SETTING
        CAShift_Avg += iCAShift[uiCA];
#endif  
        mcSHOW_DBG_MSG3(("CA%d     Shift %d     CA(%d~%d) Clk(%d~%d) %d\n", uiCA, iCAShift[uiCA], 
            iBestLastCAPass[uiCA]-iCAShift[uiCA], iBestFirstCAPass[uiCA],  
            iBestFirstClkPass[uiCA], iBestLastClkPass[uiCA]+iCAShift[uiCA], iCenter[uiCA]+iCAShift[uiCA]));

        mcFPRINTF((fp_A60808, "CA%d     Shift %d     CA(%d~%d) Clk(%d~%d) %d\n", uiCA, iCAShift[uiCA], 
            iBestLastCAPass[uiCA]-iCAShift[uiCA], iBestFirstCAPass[uiCA],  
            iBestFirstClkPass[uiCA], iBestLastClkPass[uiCA]+iCAShift[uiCA], iCenter[uiCA]+iCAShift[uiCA]));
    }
    mcSHOW_DBG_MSG3(("=========================================\n"));    
    mcFPRINTF((fp_A60808, "=========================================\n"));    

    // Restore the registers' values.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), uiReg1DCh);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), uiReg1E0h);
    //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), uiRegD8h);

    // Write shift value into CA output delay.
    uiTemp = iCAShift[0] + (iCAShift[1]<<8) + (iCAShift[2]<<16) + (iCAShift[3]<<24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1a8), uiTemp);    // CA0~CA3
    mcSHOW_DBG_MSG3(("Reg.1A8h=%xh\n", uiTemp));    
    mcFPRINTF((fp_A60808, "Reg.1A8h=%xh\n", uiTemp));    
    uiTemp = iCAShift[4] + (iCAShift[5]<<8) + (iCAShift[6]<<16) + (iCAShift[7]<<24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ac), uiTemp);    // CA4~CA7
    mcSHOW_DBG_MSG3(("Reg.1ACh=%xh\n", uiTemp));    
    mcFPRINTF((fp_A60808, "Reg.1ACh=%xh\n", uiTemp));    
    uiTemp = iCAShift[8] + (iCAShift[9]<<8);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1b0), uiTemp);    // CA8~CA11
    mcSHOW_DBG_MSG3(("Reg.1B0h=%xh\n", uiTemp));    
    mcFPRINTF((fp_A60808, "Reg.1B0h=%xh\n", uiTemp)); 

#ifdef CKE_CS_DLY_SETTING
    CAShift_Avg = (CAShift_Avg + (CATRAINING_NUM>>1)) /CATRAINING_NUM;
    // CKEDLY : Reg.1B8h[12:8].  CSDLY : Reg.1B8h[4:0]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1b8), &uiTemp);
    uiTemp = (uiTemp & 0xffffe0e0) | (CAShift_Avg << 8) | (CAShift_Avg << 0);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1b8), uiTemp);
    mcSHOW_DBG_MSG3(("Reg.1B8h=%xh\n", uiTemp));    

    // CKE1DLY : Reg.1C4h[28:24]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1c4), &uiTemp);
    uiTemp = (uiTemp & 0xe0ffffff) | (CAShift_Avg << 24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1c4), uiTemp);
    mcSHOW_DBG_MSG3(("Reg.1C4h=%xh\n", uiTemp));    

    // CS1DLY : Reg.0Ch[31:28]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x00c), &uiTemp);
    uiTemp = (uiTemp & 0x0fffffff) | (CAShift_Avg << 28);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x00c), uiTemp);
    mcSHOW_DBG_MSG3(("Reg.00ch=%xh\n", uiTemp));    
#endif    

    DramcEnterSelfRefresh(p, 1);    
    // Write max center value into Clk output delay.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &uiTemp);
    mcSET_FIELD(uiTemp, iMaxCenter+CATRAINING_STEP, 0x0f000000, 24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), uiTemp);    
    mcSHOW_DBG_MSG3(("Reg.0Ch=%xh\n", uiTemp));    
    mcFPRINTF((fp_A60808, "Reg.0Ch=%xh\n", uiTemp));    
    DramcEnterSelfRefresh(p, 0);
#ifdef WL_CLKADJUST
    CATrain_ClkDelay[p->channel] = iMaxCenter;
    mcSHOW_DBG_MSG3(("CATrain_ClkDelay=%d...\n", CATrain_ClkDelay[p->channel]));
#endif

    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), u4prv_register_0fc);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4prv_register_044);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4prv_register_63c);


    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }    
}

#ifdef WL_CLKADJUST
//-------------------------------------------------------------------------
/** DramcWriteLeveling
 *  start Write Leveling Calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  apply           (U8): 0 don't apply the register we set  1 apply the register we set ,default don't apply.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
#ifdef COMBO_MCP
DRAM_STATUS_T DramcWriteLeveling(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
#else
DRAM_STATUS_T DramcWriteLeveling(DRAMC_CTX_T *p)
#endif
{
// Note that below procedure is based on "ODT off"
    U8 ucstatus = 0;
    U32 u4value, u4dq_o1, u4temp;
    U32 u4prv_register_1dc, u4prv_register_044, u4prv_register_0e4, u4prv_register_13c, u4prv_register_0d8;
    U8 ucsample_status[DQS_NUMBER], ucdq_o1_perbyte[DQS_NUMBER], ucdq_o1_index[DQS_NUMBER];
    U8 byte_i, ucsample_count;
    S8 ii, ClockDelayMax = MAX_TX_DQSDLY_TAPS;
#ifdef CKE_CS_DLY_SETTING
    S8 CAShift_Avg;
#endif

    fgwrlevel_done[p->channel] = 0;

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

    if ((p->dram_type != TYPE_PCDDR3) && (p->dram_type != TYPE_LPDDR3))
    {
        mcSHOW_ERR_MSG(("Wrong DRAM TYPE. Only support DDR3 and LPDDR3 in write leveling!!\n"));
        return DRAM_FAIL;
    }

    // this depends on pinmux
    // select first bit of each byte

    // channel A/B, LP3-POP and DDR3-SBS
    if (p->channel == CHANNEL_A)
    {
        if (p->dram_type == TYPE_LPDDR3)
        {
            ucdq_o1_index[0]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[0];
            ucdq_o1_index[1]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[8];
            ucdq_o1_index[2]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[16];
            ucdq_o1_index[3]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[24];
        }
        else
        {
            ucdq_o1_index[0]=8;
            ucdq_o1_index[1]=4;
            ucdq_o1_index[2]=25;
            ucdq_o1_index[3]=22;
        }
    }
    else
    {
        if (p->dram_type == TYPE_LPDDR3)
        {
            ucdq_o1_index[0]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[0];
            ucdq_o1_index[1]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[8];
            ucdq_o1_index[2]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[16];
            ucdq_o1_index[3]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[24];
        }
        else
        {
            ucdq_o1_index[0]=14;
            ucdq_o1_index[1]=5;
            ucdq_o1_index[2]=20;
            ucdq_o1_index[3]=27;
        }
    }
    
    // backup mode settings
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), &u4prv_register_1dc);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4prv_register_044);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4prv_register_0e4);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4prv_register_13c);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4prv_register_0d8);

    //write leveling mode initialization
    //Make CKE fixed at 1 (Put this before issuing MRS): CKEFIXON = 1 (0xe4[2])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4value);
    mcSET_BIT(u4value, POS_PADCTL4_CKEFIXON);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), u4value);

    //disable auto refresh: REFCNT_FR_CLK = 0 (0x1dc[23:16]), ADVREFEN = 0 (0x44[30])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), &u4value);
    mcCLR_MASK(u4value, MASK_DRAMC_PD_CTRL_REFCNT_FR_CLK);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), u4value);

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4value);
    mcCLR_BIT(u4value, POS_TEST2_3_ADVREFEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4value);
    
/*
    //Enable Write ODT: WOEN = 1 (0x7c[3])
    //may no need to set here, initial value
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), &u4value);
    mcSET_BIT(u4value, POS_DDR2CTL_WOEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), u4value);

    //ODT, DQIEN fixed at 1; FIXODT = 1 (0xd8[23]), FIXDQIEN = 1111 (0xd8[15:12])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4value);
    mcSET_BIT(u4value, POS_MCKDLY_FIXODT);
    mcSET_MASK(u4value, MASK_MCKDLY_FIXDQIEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4value);
*/
    //FIXDQIEN = 1111 (0xd8[15:12])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4value);
    mcSET_MASK(u4value, MASK_MCKDLY_FIXDQIEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4value);

    //Enable DQ_O1, SELO1ASO=1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcSET_BIT(u4value, POS_LPDDR2_SELO1ASO);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);

    // enable DDR write leveling mode
    if (p->dram_type == TYPE_PCDDR3)
    {
        //issue MR1[7] to enable write leveling
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DEFAULT_MR1_VALUE_DDR3 | 0x00000080);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }
    else // LPDDR3
    {
        // issue MR2[7] to enable write leveling (refer to DEFAULT MR2 value)
    	#ifdef DUAL_FREQ_DIFF_RLWL
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_LOW_MR2 | 0x00800000);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_HIGH_MR2 | 0x00800000);
	}	    
	#else
#ifdef COMBO_MCP
    #if 0
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), emi_set->iLPDDR3_MODE_REG_2 | 0x00800000);
#else	
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DramcGetMR2ByFreq(mt_get_dram_freq_setting()) | 0x00800000);
    #endif        
#else	
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_MODE_REG_2 | 0x00800000);
#endif        
	#endif

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }

    //wait tWLDQSEN (25 nCK / 25ns) after enabling write leveling mode (DDR3 / LPDDDR3)
    mcDELAY_US(1);

    //Set {R_DQS_B3_G R_DQS_B2_G R_DQS_B1_G R_DQS_B0_G}=1010: 0x13c[4:1] (this depends on sel_ph setting)
    //Enable Write leveling: 0x13c[0]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
    mcSET_FIELD(u4value, 0xa, MASK_WRLEV_DQS_Bx_G, POS_WRLEV_DQS_Bx_G);
    mcSET_BIT(u4value, POS_WRLEV_WRITE_LEVEL_EN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);

    // wait tWLMRD (40 nCL / 40 ns) before DQS pulse (DDR3 / LPDDR3)
    mcDELAY_US(1);    

    //Proceed write leveling...
    //Initilize sw parameters
    for (ii=0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        ucsample_status[ii] = 0;
        wrlevel_dqs_final_delay[p->channel][ii] = 0;
    }

    //used for WL done status
    // each bit of sample_cnt represents one-byte WL status
    // 1: done or N/A. 0: NOK
    if (p->data_width == DATA_WIDTH_32BIT)
    {
        ucsample_count = 0xf0;
    }
    else if ((p->data_width == DATA_WIDTH_16BIT))
    {
        ucsample_count = 0xfc;
    }
    else
    {
        ucsample_count = 0xf0;
    }

    mcSHOW_DBG_MSG(("===============================================================================\n"));
    mcSHOW_DBG_MSG(("\n        [Channel %d]dramc_write_leveling_swcal\n", p->channel));
    mcSHOW_DBG_MSG(("===============================================================================\n"));
    mcSHOW_DBG_MSG(("delay  byte0  byte1  byte2  byte3\n"));
    mcSHOW_DBG_MSG(("-----------------------------\n"));

    mcFPRINTF((fp_A60808, "===============================================================================\n"));
    mcFPRINTF((fp_A60808, "\n        dramc_write_leveling_swcal\n"));
    mcFPRINTF((fp_A60808, "===============================================================================\n"));
    mcFPRINTF((fp_A60808, "delay  byte0  byte1  byte2  byte3\n"));
    mcFPRINTF((fp_A60808, "-----------------------------\n"));

    //not sure LP3 can be WL together 
#ifndef fcWL_ALL
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
#endif
    {
    #ifndef fcWL_ALL
        // select respective DQS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
        mcSET_FIELD(u4value, ((U8)1<<byte_i), MASK_WRLEV_DQS_SEL, POS_WRLEV_DQS_SEL);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);
    #else
        // select all DQS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
        mcSET_FIELD(u4value, 0xf, MASK_WRLEV_DQS_SEL, POS_WRLEV_DQS_SEL);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);
    #endif
    
        for (ii=(-MAX_TX_DQSDLY_TAPS+1); ii<MAX_TX_DQSDLY_TAPS; ii++)
        {
        #ifndef fcWL_ALL
            if (ii >= 0)
	    {        
	    	    // Set Clk output delay to 0.
	            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &u4value);
		    mcSET_FIELD(u4value, 0, 0x0f000000, 24);
	    	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), u4value);    
	    	    
	            // set DQS delay
	            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), &u4value); 
	            mcSET_FIELD(u4value, ii, ((U32)0xf)<<(4*byte_i), 4*byte_i);       
	            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
            }
            else
            {
            	    // Adjust Clk output delay.
	            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &u4value);
		    mcSET_FIELD(u4value, -ii, 0x0f000000, 24);
	    	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), u4value);    

            	    // Set DQS output delay to 0
		    u4value = 0;
            	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
            }
        #else
            if (ii >= 0)
	    {
	    	    // Set Clk output delay to 0.
	            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &u4value);
		    mcSET_FIELD(u4value, 0, 0x0f000000, 24);
	    	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), u4value);    
	    	    
	    	    // Adjust DQS output delay.
		    u4value = 0;
	            for (byte_i= 0; byte_i < (p->data_width/DQS_BIT_NUMBER); byte_i++)
	            {
	                u4value += (((U32)ii)<<(4*byte_i));        
	            }
	            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
            }
            else
            {
            	    // Adjust Clk output delay.
	            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &u4value);
		    mcSET_FIELD(u4value, -ii, 0x0f000000, 24);
	    	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), u4value);    

            	    // Set DQS output delay to 0
		    u4value = 0;
            	    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
            }
        #endif            
            
            //Trigger DQS pulse, R_DQS_WLEV: 0x13c[8] from 1 to 0
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
            mcSET_BIT(u4value, POS_WRLEV_DQS_WLEV);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);
            mcCLR_BIT(u4value, POS_WRLEV_DQS_WLEV);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);

            //wait tWLO (7.5ns / 20ns) before output (DDR3 / LPDDR3)
            mcDELAY_US(1);

            //Read DQ_O1 from register, 0x380
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x380), &u4dq_o1);
            //mcSHOW_DBG_MSG2(("loop: %2d DQ_O1: 0x%8x \n", ii, u4dq_o1));

        #ifdef fcWL_ALL
            mcSHOW_DBG_MSG(("%d    ", ii));
            mcFPRINTF((fp_A60808, "%d    ", ii));
            for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
        #endif
            {
                ucdq_o1_perbyte[byte_i] = (U8)((u4dq_o1>>ucdq_o1_index[byte_i])&0x00000001);        

	    #ifdef WL_TEST
	    	if (byte_i==0)
	    	{
	    		// 0-1-0
			if ((ii <= -11) || (ii>-5) || (ii==-9))
			{
				ucdq_o1_perbyte[byte_i]  = 1;
			} 
			else
			{
				ucdq_o1_perbyte[byte_i]  = 0;
			}
	    	}
	    	else if (byte_i==1)
	    	{
	    		// 1-0-1-0-1
			if ((ii <= -11) || (ii>=-3) || (ii==-9) || (ii==-7))
			{
				ucdq_o1_perbyte[byte_i]  = 1;
			} 
			else
			{
				ucdq_o1_perbyte[byte_i]  = 0;
			}
	    	
	    	}
	    	else if (byte_i==2)
	    	{
	    		// 1-0-1-1-0-0-0
			if ((ii <= -11) || (ii>-2) || (ii==-9) || (ii==-8))
			{
				ucdq_o1_perbyte[byte_i]  = 1;
			} 
			else
			{
				ucdq_o1_perbyte[byte_i]  = 0;
			}
	    	
	    	}
	    	else if (byte_i==3)
	    	{
			ucdq_o1_perbyte[byte_i]  = 0;
			// 0-0-..-0-1
/*			if (ii == (MAX_TX_DQSDLY_TAPS-1))
			{
				ucdq_o1_perbyte[byte_i]  = 1;
			} 
			else
			{
				ucdq_o1_perbyte[byte_i]  = 0;
			}*/
	    	
	    		// 1-0-1-1-1-0-0-0
/*			if ((ii <= -3) || (ii>5) || (ii==-1) || (ii==0) || (ii==1))
			{
				ucdq_o1_perbyte[byte_i]  = 1;
			} 
			else
			{
				ucdq_o1_perbyte[byte_i]  = 0;
			}*/
	    	
	    	}
	    		
	    #endif
            #ifndef fcWL_ALL
                mcSHOW_DBG_MSG(("%d    %d\n", ii,  ucdq_o1_perbyte[byte_i]));
                mcFPRINTF((fp_A60808, "%d    %d\n", ii,  ucdq_o1_perbyte[byte_i]));
            #else
                mcSHOW_DBG_MSG(("%d    ", ucdq_o1_perbyte[byte_i]));
                mcFPRINTF((fp_A60808, "%d    ", ucdq_o1_perbyte[byte_i]));
            #endif

                //sample from 0 to 1        
                // Mark the following because of the case 1000000....00000001111..
               /* if ((ucsample_status[byte_i]==0) && (ucdq_o1_perbyte[byte_i]==1))
                {
                    ucsample_status[byte_i] = 2;
                    //record delay value
                    wrlevel_dqs_final_delay[p->channel][byte_i] = ii;
                #ifndef fcWL_ALL
                    // to the next byte
                    //break;
                #else
                    //used for early break
                    ucsample_count++;
                #endif
                }
                else */
                // ucsample_status :
                //		1 : 0.
                //		2 : 0-1
                //		3 : 0-1-1
                //		4 : 0-1-1-1
                //		...
                if ((ucsample_status[byte_i]==0) && (ucdq_o1_perbyte[byte_i]==0))
                {
                    ucsample_status[byte_i] = 1;
                }
                else if ((ucsample_status[byte_i]==1) && (ucdq_o1_perbyte[byte_i]==1))
                {
                    ucsample_status[byte_i] = 2;
                    if (ii==(MAX_TX_DQSDLY_TAPS-1))	// Only in last tap, otherwise keep searching consecutive 1s.
                    {
                    //record delay value
                    wrlevel_dqs_final_delay[p->channel][byte_i] = ii;
                #ifndef fcWL_ALL
                    // to the next byte
                    //break;
                #else
	                    //used for WL done status
	                    ucsample_count |= (0x01 << byte_i);
                #endif
                }  
            }
                else if (ucsample_status[byte_i]>=2)	// (ucsample_status[byte_i]-1) consecutive 1s.
                {
                	if (ucdq_o1_perbyte[byte_i]==1)
                	{
	              		// 0-1-1
	                    	ucsample_status[byte_i]++;
				//record delay value
				wrlevel_dqs_final_delay[p->channel][byte_i] = ii -(ucsample_status[byte_i]-2);
				#ifndef fcWL_ALL
				// to the next byte
				//break;
				#else
	                 //used for WL done status
				ucsample_count |= (0x01 << byte_i);
				#endif              		
                	}
                	else
                	{
                		if (ucsample_status[byte_i] <= 4)
                		{
	                		// 0-1-0, 0
	                		// Maybe unstable or glitch during transition boundary like 1-1-0-1-0-0-0-0-0
	               			ucsample_status[byte_i] = 0;
		                        ucsample_count &= (~(0x01 << byte_i));
                		}
                	}
                }
            }
        #ifdef fcWL_ALL
            mcSHOW_DBG_MSG(("\n"));
            mcFPRINTF((fp_A60808, "\n"));  
        #endif
        }
    }

    if (ucsample_count==0xff)
    {
                // all bytes are done
                fgwrlevel_done[p->channel] = 1;
    }

#ifdef fcWL_ALL
    mcSHOW_DBG_MSG(("pass byte mask = %xh, fgwrlevel_done[%d]=%xh\n", ucsample_count, p->channel, fgwrlevel_done[p->channel]));
    mcFPRINTF(("pass byte mask = %xh, fgwrlevel_done[%d]=%xh\n", ucsample_count, p->channel, fgwrlevel_done[p->channel]));
#endif
    mcSHOW_DBG_MSG(("byte_i    status    best delay\n"));
    mcFPRINTF((fp_A60808, "byte_i    status    best delay\n"));
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
    {
        mcSHOW_DBG_MSG(("%d    %d    %d\n", byte_i, ucsample_status[byte_i], wrlevel_dqs_final_delay[p->channel][byte_i]));
        mcFPRINTF((fp_A60808, "%d    %d    %d\n", byte_i, ucsample_status[byte_i], wrlevel_dqs_final_delay[p->channel][byte_i]));
        if (ClockDelayMax > wrlevel_dqs_final_delay[p->channel][byte_i])
        {
        	ClockDelayMax = wrlevel_dqs_final_delay[p->channel][byte_i];
        }
    }    
    mcSHOW_DBG_MSG(("========================================\n"));
    mcFPRINTF((fp_A60808, "========================================\n"));

#ifdef DDR_FT_LOAD_BOARD
    if (!fgwrlevel_done[p->channel])
    {
        LoadBoardShowResult(FLAG_WL_CALIBRATION, FLAG_CALIBRATION_FAIL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1); 
    }
#endif

    if (ClockDelayMax > 0)
    {
    	ClockDelayMax = 0;
    }
    else
    {
    	ClockDelayMax = -ClockDelayMax;
    }

    // Adjust Clk & CA if needed
    if (CATrain_ClkDelay[p->channel] < ClockDelayMax)
    {
    	S32 Diff = ClockDelayMax - CATrain_ClkDelay[p->channel];
    	U8 RAXDLY[4];

	//DramcEnterSelfRefresh(p, 1);    
	// Write max center value into Clk output delay.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &u4value);
	mcSET_FIELD(u4value, ClockDelayMax, 0x0f000000, 24);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), u4value);    
	mcSHOW_DBG_MSG3(("Reg.0Ch=%xh\n", u4value));    
	mcFPRINTF((fp_A60808, "Reg.0Ch=%xh\n", u4value));    
	//DramcEnterSelfRefresh(p, 0);    	

	mcSHOW_DBG_MSG(("CA adjust %d taps... \n", Diff));

	// Write shift value into CA output delay.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1a8), &u4value);
	for (ii=0; ii<4; ii++)
	{
		RAXDLY[ii] =  ((u4value>>(ii<<3)) & 0x0f) + Diff;
		if (RAXDLY[ii] > 0x0f)
		{
			RAXDLY[ii] = 0x0f;
		}
#ifdef CKE_CS_DLY_SETTING
		CAShift_Avg += RAXDLY[ii];
#endif
	}

	u4value = RAXDLY[0] + (RAXDLY[1]<<8) + (RAXDLY[2]<<16) + (RAXDLY[3]<<24);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1a8), u4value);    // CA0~CA3
	mcSHOW_DBG_MSG3(("Reg.1A8h=%xh\n", u4value));    
	mcFPRINTF((fp_A60808, "Reg.1A8h=%xh\n", u4value));    

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1ac), &u4value);
	for (ii=0; ii<4; ii++)
	{
		RAXDLY[ii] =  ((u4value>>(ii<<3)) & 0x0f) + Diff;
		if (RAXDLY[ii] > 0x0f)
		{
			RAXDLY[ii] = 0x0f;
		}
#ifdef CKE_CS_DLY_SETTING
		CAShift_Avg += RAXDLY[ii];
#endif	
	}
	u4value = RAXDLY[0] + (RAXDLY[1]<<8) + (RAXDLY[2]<<16) + (RAXDLY[3]<<24);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ac), u4value);    // CA4~CA7
	mcSHOW_DBG_MSG3(("Reg.1ACh=%xh\n", u4value));    
	mcFPRINTF((fp_A60808, "Reg.1ACh=%xh\n", u4value));    

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1b0), &u4value);
	for (ii=0; ii<2; ii++)
	{
		RAXDLY[ii] =  ((u4value>>(ii<<3)) & 0x0f) + Diff;
		if (RAXDLY[ii] > 0x0f)
		{
			RAXDLY[ii] = 0x0f;
		}
#ifdef CKE_CS_DLY_SETTING
		CAShift_Avg += RAXDLY[ii];
#endif	
	}
	u4value = RAXDLY[0] + (RAXDLY[1]<<8);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1b0), u4value);    // CA4~CA7
	mcSHOW_DBG_MSG3(("Reg.1B0h=%xh\n", u4value));    
	mcFPRINTF((fp_A60808, "Reg.1B0h=%xh\n", u4value));    

#ifdef CKE_CS_DLY_SETTING
	CAShift_Avg = (CAShift_Avg + (CATRAINING_NUM>>1)) /CATRAINING_NUM;
	// CKEDLY : Reg.1B8h[12:8].  CSDLY : Reg.1B8h[4:0]
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1b8), &u4value);
	u4value = (u4value & 0xffffe0e0) | (CAShift_Avg << 8) | (CAShift_Avg << 0);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1b8), u4value);
	mcSHOW_DBG_MSG3(("Reg.1B8h=%xh\n", u4value));    

	// CKE1DLY : Reg.1C4h[28:24]
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1c4), &u4value);
	u4value = (u4value & 0xe0ffffff) | (CAShift_Avg << 24);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1c4), u4value);
	mcSHOW_DBG_MSG3(("Reg.1C4h=%xh\n", u4value));    

	// CS1DLY : Reg.0Ch[31:28]
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x00c), &u4value);
	u4value = (u4value & 0x0fffffff) | (CAShift_Avg << 28);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x00c), u4value);
	mcSHOW_DBG_MSG3(("Reg.00ch=%xh\n", u4value));    
#endif     
    }
    else
    {
    	ClockDelayMax = CATrain_ClkDelay[p->channel];
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0c), &u4value);
	mcSET_FIELD(u4value, ClockDelayMax, 0x0f000000, 24);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0c), u4value);        	
    }    
    mcSHOW_DBG_MSG(("After adjustment...\n"));
    mcSHOW_DBG_MSG(("Clk output delay = %d\n", ClockDelayMax));
    
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
    {
    	wrlevel_dqs_final_delay[p->channel][byte_i] += (ClockDelayMax);
    	if (wrlevel_dqs_final_delay[p->channel][byte_i] > 0xf)
    	{
    		wrlevel_dqs_final_delay[p->channel][byte_i] = 0x0f;
    	}
    	mcSHOW_DBG_MSG(("DQS%d output delay =  %d\n", byte_i, wrlevel_dqs_final_delay[p->channel][byte_i]));
    }

    // write leveling done, mode settings recovery if necessary
    // recover mode registers
    if (p->dram_type == TYPE_PCDDR3)
    {
        //issue MR1[7] to enable write leveling
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DEFAULT_MR1_VALUE_DDR3);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }
    else // LPDDR3
    {
        // issue MR2[7] to enable write leveling (refer to DEFAULT MR2 value)
    	#ifdef DUAL_FREQ_DIFF_RLWL
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_LOW_MR2);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_HIGH_MR2);
	}	
	#else
#ifdef COMBO_MCP	
    #if 0	
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), emi_set->iLPDDR3_MODE_REG_2);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DramcGetMR2ByFreq(mt_get_dram_freq_setting()));
    #endif    
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_MODE_REG_2);
#endif        
	#endif

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }

    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4prv_register_13c);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), u4prv_register_1dc);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4prv_register_044);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), u4prv_register_0e4);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4prv_register_0d8);    
    
    //Disable DQ_O1, SELO1ASO=0 for power saving
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcCLR_BIT(u4value, POS_LPDDR2_SELO1ASO);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);

    // set to best values
    // DQS
    u4value = 0;
    for (byte_i= 0; byte_i < (p->data_width/DQS_BIT_NUMBER); byte_i++)
    {
        u4value += (((U32)wrlevel_dqs_final_delay[p->channel][byte_i])<<(4*byte_i));        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);

    mcSHOW_DBG_MSG2(("[write leveling]DQS: 0x%x", u4value));
    
    // DQM
    if (p->data_width == DATA_WIDTH_16BIT)
    {
        // for DQC case, DQM3 is CS#
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4temp);
        mcSET_FIELD(u4temp, u4value, 0x000000ff, 0);
        u4value = u4temp;
    }
    else
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4temp);
        mcSET_FIELD(u4temp, u4value, 0x0000ffff, 0);
        u4value = u4temp;        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), u4value);

    mcSHOW_DBG_MSG2((" DQM: 0x%x\n", u4value));

    // DQ delay, each DQ has 4 bits. Each register contains 8-bit DQ's
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER); byte_i++)
    {
        u4value = 0;
        for (ii = 0; ii < DQS_BIT_NUMBER; ii++)
        {
            u4value += (((U32) wrlevel_dqs_final_delay[p->channel][byte_i]) << (4*ii));
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*byte_i), u4value);
        mcSHOW_DBG_MSG2(("[write leveling]DQ byte%d reg: 0x%x val: 0x%x\n", byte_i, mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*byte_i), u4value));
    }   

    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }    

    // log example
/*
===================================================================

                dramc_write_leveling_swcal
                apply=1 channel=2(2:cha, 3:chb)
===================================================================
delay  byte0  byte1  byte2  byte3
-----------------------------
  0    0    0    0    1
  1    0    0    0    1
  2    0    0    1    1
  3    0    0    1    1
  4    0    0    1    1
  5    0    0    1    1
  6    0    0    1    1
  7    0    0    1    1
  8    0    0    1    1
  9    0    0    1    1
 10    0    0    1    1
 11    1    1    1    1
pass bytecount = 4
byte_i    status    best delay
0         2         11
1         2         11
2         2         2
3         2         0
*/
    return DRAM_OK;
}

#else
//-------------------------------------------------------------------------
/** DramcWriteLeveling
 *  start Write Leveling Calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  apply           (U8): 0 don't apply the register we set  1 apply the register we set ,default don't apply.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
#ifdef COMBO_MCP
DRAM_STATUS_T DramcWriteLeveling(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
#else
DRAM_STATUS_T DramcWriteLeveling(DRAMC_CTX_T *p)
#endif
{
// Note that below procedure is based on "ODT off"
    U8 ucstatus = 0;
    U32 u4value, u4dq_o1, u4temp;
    U32 u4prv_register_1dc, u4prv_register_044, u4prv_register_0e4, u4prv_register_13c, u4prv_register_0d8;
    U8 ucsample_status[DQS_NUMBER], ucdq_o1_perbyte[DQS_NUMBER], ucdq_o1_index[DQS_NUMBER];
    U8 ii, byte_i, ucsample_count;

    fgwrlevel_done[p->channel] = 0;

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

    if ((p->dram_type != TYPE_PCDDR3) && (p->dram_type != TYPE_LPDDR3))
    {
        mcSHOW_ERR_MSG(("Wrong DRAM TYPE. Only support DDR3 and LPDDR3 in write leveling!!\n"));
        return DRAM_FAIL;
    }

    // this depends on pinmux
    // select first bit of each byte

    // channel A/B, LP3-POP and DDR3-SBS
    if (p->channel == CHANNEL_A)
    {
        if (p->dram_type == TYPE_LPDDR3)
        {
            ucdq_o1_index[0]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[0];
            ucdq_o1_index[1]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[8];
            ucdq_o1_index[2]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[16];
            ucdq_o1_index[3]=(U8) uiLPDDR_PHY_Mapping_POP_CHA[24];
        }
        else
        {
            ucdq_o1_index[0]=8;
            ucdq_o1_index[1]=4;
            ucdq_o1_index[2]=25;
            ucdq_o1_index[3]=22;
        }
    }
    else
    {
        if (p->dram_type == TYPE_LPDDR3)
        {
            ucdq_o1_index[0]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[0];
            ucdq_o1_index[1]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[8];
            ucdq_o1_index[2]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[16];
            ucdq_o1_index[3]=(U8) uiLPDDR_PHY_Mapping_POP_CHB[24];
        }
        else
        {
            ucdq_o1_index[0]=14;
            ucdq_o1_index[1]=5;
            ucdq_o1_index[2]=20;
            ucdq_o1_index[3]=27;
        }
    }
    
    // backup mode settings
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), &u4prv_register_1dc);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4prv_register_044);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4prv_register_0e4);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4prv_register_13c);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4prv_register_0d8);

    //write leveling mode initialization
    //Make CKE fixed at 1 (Put this before issuing MRS): CKEFIXON = 1 (0xe4[2])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4value);
    mcSET_BIT(u4value, POS_PADCTL4_CKEFIXON);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), u4value);

    //disable auto refresh: REFCNT_FR_CLK = 0 (0x1dc[23:16]), ADVREFEN = 0 (0x44[30])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), &u4value);
    mcCLR_MASK(u4value, MASK_DRAMC_PD_CTRL_REFCNT_FR_CLK);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), u4value);

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4value);
    mcCLR_BIT(u4value, POS_TEST2_3_ADVREFEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4value);
    
/*
    //Enable Write ODT: WOEN = 1 (0x7c[3])
    //may no need to set here, initial value
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), &u4value);
    mcSET_BIT(u4value, POS_DDR2CTL_WOEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), u4value);

    //ODT, DQIEN fixed at 1; FIXODT = 1 (0xd8[23]), FIXDQIEN = 1111 (0xd8[15:12])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4value);
    mcSET_BIT(u4value, POS_MCKDLY_FIXODT);
    mcSET_MASK(u4value, MASK_MCKDLY_FIXDQIEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4value);
*/
    //FIXDQIEN = 1111 (0xd8[15:12])
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4value);
    mcSET_MASK(u4value, MASK_MCKDLY_FIXDQIEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4value);

    //Enable DQ_O1, SELO1ASO=1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcSET_BIT(u4value, POS_LPDDR2_SELO1ASO);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);

    // enable DDR write leveling mode
    if (p->dram_type == TYPE_PCDDR3)
    {
        //issue MR1[7] to enable write leveling
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DEFAULT_MR1_VALUE_DDR3 | 0x00000080);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }
    else // LPDDR3
    {
        // issue MR2[7] to enable write leveling (refer to DEFAULT MR2 value)
    	#ifdef DUAL_FREQ_DIFF_RLWL
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_LOW_MR2 | 0x00800000);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_HIGH_MR2 | 0x00800000);
	}	
	#else
#ifdef COMBO_MCP	
    #if 0	
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), emi_set->iLPDDR3_MODE_REG_2 | 0x00800000);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DramcGetMR2ByFreq(mt_get_dram_freq_setting()) | 0x00800000);
    #endif          
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_MODE_REG_2 | 0x00800000);
#endif        
	#endif

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }

    //wait tWLDQSEN (25 nCK / 25ns) after enabling write leveling mode (DDR3 / LPDDDR3)
    mcDELAY_US(1);

    //Set {R_DQS_B3_G R_DQS_B2_G R_DQS_B1_G R_DQS_B0_G}=1010: 0x13c[4:1] (this depends on sel_ph setting)
    //Enable Write leveling: 0x13c[0]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
    mcSET_FIELD(u4value, 0xa, MASK_WRLEV_DQS_Bx_G, POS_WRLEV_DQS_Bx_G);
    mcSET_BIT(u4value, POS_WRLEV_WRITE_LEVEL_EN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);

    // wait tWLMRD (40 nCL / 40 ns) before DQS pulse (DDR3 / LPDDR3)
    mcDELAY_US(1);    

    //Proceed write leveling...
    //Initilize sw parameters
    for (ii=0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        ucsample_status[ii] = 0;
        wrlevel_dqs_final_delay[p->channel][ii] = 0;
    }
    //used for early break
    ucsample_count = 0;

    mcSHOW_DBG_MSG(("===============================================================================\n"));
    mcSHOW_DBG_MSG(("\n        dramc_write_leveling_swcal\n"));
    mcSHOW_DBG_MSG(("===============================================================================\n"));
    mcSHOW_DBG_MSG(("delay  byte0  byte1  byte2  byte3\n"));
    mcSHOW_DBG_MSG(("-----------------------------\n"));

    mcFPRINTF((fp_A60808, "===============================================================================\n"));
    mcFPRINTF((fp_A60808, "\n        dramc_write_leveling_swcal\n"));
    mcFPRINTF((fp_A60808, "===============================================================================\n"));
    mcFPRINTF((fp_A60808, "delay  byte0  byte1  byte2  byte3\n"));
    mcFPRINTF((fp_A60808, "-----------------------------\n"));

    //not sure LP3 can be WL together 
#ifndef fcWL_ALL
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
#endif
    {
    #ifndef fcWL_ALL
        // select respective DQS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
        mcSET_FIELD(u4value, ((U8)1<<byte_i), MASK_WRLEV_DQS_SEL, POS_WRLEV_DQS_SEL);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);
    #else
        // select all DQS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
        mcSET_FIELD(u4value, 0xf, MASK_WRLEV_DQS_SEL, POS_WRLEV_DQS_SEL);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);
    #endif
    
        for (ii=0; ii<MAX_TX_DQSDLY_TAPS; ii++)
        {
        #ifndef fcWL_ALL
            // set DQS delay
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), &u4value); 
            mcSET_FIELD(u4value, ii, ((U32)0xf)<<(4*byte_i), 4*byte_i);       
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
        #else
            u4value = 0;
            for (byte_i= 0; byte_i < (p->data_width/DQS_BIT_NUMBER); byte_i++)
            {
                u4value += (((U32)ii)<<(4*byte_i));        
            }
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
        #endif            
            
            //Trigger DQS pulse, R_DQS_WLEV: 0x13c[8] from 1 to 0
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), &u4value);
            mcSET_BIT(u4value, POS_WRLEV_DQS_WLEV);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);
            mcCLR_BIT(u4value, POS_WRLEV_DQS_WLEV);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4value);

            //wait tWLO (7.5ns / 20ns) before output (DDR3 / LPDDR3)
            mcDELAY_US(1);

            //Read DQ_O1 from register, 0x380
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x380), &u4dq_o1);
            //mcSHOW_DBG_MSG2(("loop: %d DQ_O1: 0x%x \n", ii, u4dq_o1));

        #ifdef fcWL_ALL
            mcSHOW_DBG_MSG(("%d    ", ii));
            mcFPRINTF((fp_A60808, "%d    ", ii));
            for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
        #endif
            {
                ucdq_o1_perbyte[byte_i] = (U8)((u4dq_o1>>ucdq_o1_index[byte_i])&0x00000001);        

            #ifndef fcWL_ALL
                mcSHOW_DBG_MSG(("%d    %d\n", ii,  ucdq_o1_perbyte[byte_i]));
                mcFPRINTF((fp_A60808, "%d    %d\n", ii,  ucdq_o1_perbyte[byte_i]));
            #else
                mcSHOW_DBG_MSG(("%d    ", ucdq_o1_perbyte[byte_i]));
                mcFPRINTF((fp_A60808, "%d    ", ucdq_o1_perbyte[byte_i]));
            #endif

                //sample from 0 to 1        
                if ((ucsample_status[byte_i]==0) && (ucdq_o1_perbyte[byte_i]==1))
                {
                    ucsample_status[byte_i] = 2;
                    //record delay value
                    wrlevel_dqs_final_delay[p->channel][byte_i] = 0;
                #ifndef fcWL_ALL
                    // to the next byte
                    //break;
                #else
                    //used for early break
                    ucsample_count++;
                #endif
                }
                else if ((ucsample_status[byte_i]==0) && (ucdq_o1_perbyte[byte_i]==0))
                {
                    ucsample_status[byte_i] = 1;
                }
                else if ((ucsample_status[byte_i]==1) && (ucdq_o1_perbyte[byte_i]==1))
                {
                    ucsample_status[byte_i] = 2;
                    //record delay value
                    wrlevel_dqs_final_delay[p->channel][byte_i] = ii;
                #ifndef fcWL_ALL
                    // to the next byte
                    //break;
                #else
                    //used for early break
                    ucsample_count++;
                #endif
                }  
            }
        #ifdef fcWL_ALL
            mcSHOW_DBG_MSG(("\n"));
            mcFPRINTF((fp_A60808, "\n"));
            //early break, may be marked for debug use
            if (ucsample_count==(p->data_width/DQS_BIT_NUMBER))
            {
                fgwrlevel_done[p->channel] = 1;
                //break;
            }   
        #endif
        }
    }

#ifdef fcWL_ALL
    mcSHOW_DBG_MSG(("pass bytecount = %d\n", ucsample_count));
    mcFPRINTF((fp_A60808, "pass bytecount = %d\n", ucsample_count));
#endif
    mcSHOW_DBG_MSG(("byte_i    status    best delay\n"));
    mcFPRINTF((fp_A60808, "byte_i    status    best delay\n"));
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER);  byte_i++)
    {
        mcSHOW_DBG_MSG(("%d    %d    %d\n", byte_i, ucsample_status[byte_i], wrlevel_dqs_final_delay[p->channel][byte_i]));
        mcFPRINTF((fp_A60808, "%d    %d    %d\n", byte_i, ucsample_status[byte_i], wrlevel_dqs_final_delay[p->channel][byte_i]));
    }    
    mcSHOW_DBG_MSG(("========================================\n"));
    mcFPRINTF((fp_A60808, "========================================\n"));

    // write leveling done, mode settings recovery if necessary
    // recover mode registers
    if (p->dram_type == TYPE_PCDDR3)
    {
        //issue MR1[7] to enable write leveling
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), DEFAULT_MR1_VALUE_DDR3);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }
    else // LPDDR3
    {
        // issue MR2[7] to enable write leveling (refer to DEFAULT MR2 value)
    	#ifdef DUAL_FREQ_DIFF_RLWL
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_LOW_MR2);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_FREQ_HIGH_MR2);
	}	
	#else        
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MRS), LPDDR3_MODE_REG_2);
	#endif

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
        mcSET_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
        mcDELAY_US(1);
        mcCLR_BIT(u4value, POS_SPCMD_MRWEN);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    }

    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_WRLEV), u4prv_register_13c);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DRAMC_PD_CTRL), u4prv_register_1dc);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4prv_register_044);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), u4prv_register_0e4);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4prv_register_0d8);    
    
    //Disable DQ_O1, SELO1ASO=0 for power saving
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcCLR_BIT(u4value, POS_LPDDR2_SELO1ASO);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);

    // set to best values
    // DQS
    u4value = 0;
    for (byte_i= 0; byte_i < (p->data_width/DQS_BIT_NUMBER); byte_i++)
    {
        u4value += (((U32)wrlevel_dqs_final_delay[p->channel][byte_i])<<(4*byte_i));        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);

    mcSHOW_DBG_MSG2(("[write leveling]DQS: 0x%x", u4value));
    
    // DQM
    if (p->data_width == DATA_WIDTH_16BIT)
    {
        // for DQC case, DQM3 is CS#
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4temp);
        mcSET_FIELD(u4temp, u4value, 0x000000ff, 0);
        u4value = u4temp;
    }
    else
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4temp);
        mcSET_FIELD(u4temp, u4value, 0x0000ffff, 0);
        u4value = u4temp;        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), u4value);

    mcSHOW_DBG_MSG2((" DQM: 0x%x\n", u4value));

    // DQ delay, each DQ has 4 bits. Each register contains 8-bit DQ's
    for (byte_i = 0; byte_i < (p->data_width/DQS_BIT_NUMBER); byte_i++)
    {
        u4value = 0;
        for (ii = 0; ii < DQS_BIT_NUMBER; ii++)
        {
            u4value += (((U32) wrlevel_dqs_final_delay[p->channel][byte_i]) << (4*ii));
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*byte_i), u4value);
        mcSHOW_DBG_MSG2(("[write leveling]DQ byte%d reg: 0x%x val: 0x%x\n", byte_i, mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*byte_i), u4value));
    }   
     
    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }

    // log example
/*
===================================================================

                dramc_write_leveling_swcal
                apply=1 channel=2(2:cha, 3:chb)
===================================================================
delay  byte0  byte1  byte2  byte3
-----------------------------
  0    0    0    0    1
  1    0    0    0    1
  2    0    0    1    1
  3    0    0    1    1
  4    0    0    1    1
  5    0    0    1    1
  6    0    0    1    1
  7    0    0    1    1
  8    0    0    1    1
  9    0    0    1    1
 10    0    0    1    1
 11    1    1    1    1
pass bytecount = 4
byte_i    status    best delay
0         2         11
1         2         11
2         2         2
3         2         0
*/
    return DRAM_OK;
}
#endif


#if 1//add by KT, define the function in ett_cust.hw_dqsi_gw.c
//-------------------------------------------------------------------------
/** DramcRxdqsGatingCal
 *  start the dqsien software calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
/*
 * nr_bit_set: Get the number of bits set in the given value.
 * @val: the gieven value
 * Return the number of bits set.
 */
static U8 nr_bit_set(U32 val)
{
    U8 i, cnt;

    for (cnt = 0, i = 0; i < 32; i++) 
    {
        if (val & (1 << i)) 
        {
            cnt++;
        }
        else
        {
            if (cnt != 0)
            {
                // cnt !=0, val ==0, break;
                dqs_gw_cnt_break = 1;
                break;
            }
        }
    }

    return cnt;
}

/*
 * first_bit_set: Get the first bit set in the given value.
 * @val: the gieven value
 * Return the first bit set.
 */
static U8 first_bit_set(U32 val)
{
    U8 i;

    for (i = 0; i < 32; i++) 
    {
        if (val & (1 << i)) 
        {
            return i;
        }
    }

    return (0xff);
}

void dqsi_gw_dly_coarse_factor_handler(DRAMC_CTX_T *p, U8 curr_val) 
{
    U8 ucstatus = 0;
    U32 u4value;
    U8 curr_val_P1;
    U32 u4Temp;
#if 1    
    U32 u4CoarseTuneStart = curr_val>>2;
    // This margin is to have rank1 coarse tune 3 M_CK smaller margin (up & down).
    // According to SY, DQSINCTL=0 will have problem.
    if (u4CoarseTuneStart > 3)
    {
    	u4CoarseTuneStart -= 3;	
    } 
    else
    {
    	if (u4CoarseTuneStart)
    	{
    		u4CoarseTuneStart = 1;
    	}
    }
#else
    U32 u4CoarseTuneStart = DQS_GW_COARSE_START>>2;
#endif

    if (u4CoarseTuneStart > 15) {
    	u4CoarseTuneStart = 15;
    }

    curr_val_P1 = curr_val + 2; // diff is 0.5T (need to check with DE)
    
    // Rank 0 P0/P1 coarse tune settings.
    // DQSINCTL: 0xe0[27:24]: A60808 with extra 1 bit (unit : 1 DRAMC clock)
    // From coarse tune 22, so set DQSINCTL = 4 first....or (DQS_GW_COARSE_START)>>2??
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), &u4value);
    mcSET_FIELD(u4value, u4CoarseTuneStart, MASK_DQSCTL1_DQSINCTL, POS_DQSCTL1_DQSINCTL);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), u4value);

    // TXDLY_DQSGATE: 0x404[14:12] (unit : 1 DRAMC clock). 
    // DQSINCTL does not have P1. So need to use TXDLY_DQSGATE/TXDLY_DQSGATE_P1 to set different 1 M_CK coarse tune values for P0 & P1.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH2), &u4value);
    u4Temp = (curr_val>>2) - (u4CoarseTuneStart);
    mcSET_FIELD(u4value, u4Temp, MASK_SELPH2_TXDLY_DQSGATE, POS_SELPH2_TXDLY_DQSGATE);
    u4Temp = (curr_val_P1>>2) - (u4CoarseTuneStart);
    mcSET_FIELD(u4value, u4Temp, MASK_SELPH2_TXDLY_DQSGATE_P1, POS_SELPH2_TXDLY_DQSGATE_P1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH2), u4value);

    // dly_DQSGATE: 0x410[23:22] (unit : 0.25 DRAMC clock)
    // dly_DQSGATE_P1: 0x410[25:24] (unit : 0.25 DRAMC clock)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH5), &u4value);
    mcSET_FIELD(u4value, curr_val&0x3, MASK_SELPH5_dly_DQSGATE, POS_SELPH5_dly_DQSGATE);
    mcSET_FIELD(u4value, curr_val_P1&0x3, MASK_SELPH5_dly_DQSGATE_P1, POS_SELPH5_dly_DQSGATE_P1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH5), u4value);
}

void dqsi_gw_dly_fine_factor_handler(DRAMC_CTX_T *p, U8 curr_val) 
{
    U8 ucstatus = 0;
    U32 u4value;

    // DQS?IEN: 0x94 (each with 7 bits)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSIEN), &u4value);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R0DQS0IEN, POS_DQSIEN_R0DQS0IEN);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R0DQS1IEN, POS_DQSIEN_R0DQS1IEN);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R0DQS2IEN, POS_DQSIEN_R0DQS2IEN);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R0DQS3IEN, POS_DQSIEN_R0DQS3IEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSIEN), u4value);
}

void dqsi_gw_dly_coarse_factor_handler_rank1(DRAMC_CTX_T *p, U8 curr_val, U8 DQSINCTL) 
{
    U8 ucstatus = 0;
    U32 u4value;
    U8 curr_val_P1;
    U32 u4Temp;
#if 1    
    U32 u4CoarseTuneStart = DQSINCTL;	// According to DE, DQSINCTL & R1DQSINCTL set to the same.
#else
    U32 u4CoarseTuneStart = DQS_GW_COARSE_START>>2;
#endif

    // Should not happen >15.
    /*
    if (u4CoarseTuneStart > 15) {
    	u4CoarseTuneStart = 15;
    }*/

    curr_val_P1 = curr_val + 2; // diff is 0.5T (need to check with DE)
    
    // TO be simple, assume curr_val < 62
    // R1DQSINCTL: 0x118[3:0]: (unit : 1 DRAMC clock)
    // From coarse tune 22, so set DQSINCTL = 4 first....or (DQS_GW_COARSE_START)>>2.
    // According to Derping, 6595 will only check DQSINCTL and ignore R1DQSINCTL because of P1 supporting. So R1DQSINCTL may be removed. But here still set the same value as DQSINCTL for safety.
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL2), &u4value);
    mcSET_FIELD(u4value, u4CoarseTuneStart, MASK_DQSCTL2_DQSINCTL, POS_DQSCTL2_DQSINCTL);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL2), u4value);

    // TXDLY_R1DQSGATE:      0x418[6:4] (unit : 1 DRAMC clock)
    // TXDLY_R1DQSGATE_P1: 0x418[10:8] (unit : 1 DRAMC clock)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH6_1), &u4value);
    u4Temp = (curr_val>>2) - (u4CoarseTuneStart);
    mcSET_FIELD(u4value, u4Temp, MASK_SELPH6_1_TXDLY_R1DQSGATE, POS_SELPH6_1_TXDLY_R1DQSGATE);
    u4Temp = (curr_val_P1>>2) - (u4CoarseTuneStart);
    mcSET_FIELD(u4value, u4Temp, MASK_SELPH6_1_TXDLY_R1DQSGATE_P1, POS_SELPH6_1_TXDLY_R1DQSGATE_P1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH6_1), u4value);

    // dly_R1DQSGATE:      0x418[1:0] (unit : 0.25 DRAMC clock)
    // dly_R1DQSGATE_P1: 0x418[3:2] (unit : 0.25 DRAMC clock)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH6_1), &u4value);
    mcSET_FIELD(u4value, curr_val&0x3, MASK_SELPH6_1_dly_R1DQSGATE, POS_SELPH6_1_dly_R1DQSGATE);
    mcSET_FIELD(u4value, curr_val_P1&0x3, MASK_SELPH6_1_dly_R1DQSGATE_P1, POS_SELPH6_1_dly_R1DQSGATE_P1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH6_1), u4value);

}

void dqsi_gw_dly_fine_factor_handler_rank1(DRAMC_CTX_T *p, U8 curr_val) 
{
    U8 ucstatus = 0;
    U32 u4value;

    // R1DQS?IEN: 0x98 (each with 7 bits)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_R1DQSIEN), &u4value);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R1DQS0IEN, POS_DQSIEN_R1DQS0IEN);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R1DQS1IEN, POS_DQSIEN_R1DQS1IEN);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R1DQS2IEN, POS_DQSIEN_R1DQS2IEN);
    mcSET_FIELD(u4value, curr_val&0x7f, MASK_DQSIEN_R1DQS3IEN, POS_DQSIEN_R1DQS3IEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_R1DQSIEN), u4value);
}

void dqs_gw_counter_reset(DRAMC_CTX_T *p) 
{
    U8 ucstatus = 0;//, ucref_cnt;
    U32 u4value;
    
    // reset dqs_counter ,we should set the bit to 1,then to 0, it can't return 0 by itself
    // 0x1e4[9] = 1 -> 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
    mcSET_BIT(u4value, POS_SPCMD_DQSGCNTRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    mcCLR_BIT(u4value, POS_SPCMD_DQSGCNTRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);

    DramcPhyReset(p);    
}

#if 0
// for testing, debug for DATLAT shift issue (only need DIV2 phase sync)
// for testing, debug for SW reset issue (need phy reset+data counter reset+DIV2 phase sync)
void dqs_gw_counter_reset_test(DRAMC_CTX_T *p) 
{
    U8 ucstatus = 0;//, ucref_cnt;
    U32 u4value, u4temp;

#if 0
    // reset dqs_counter ,we should set the bit to 1,then to 0, it can't return 0 by itself
    // 0x1e4[9] = 1 -> 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
    mcSET_BIT(u4value, POS_SPCMD_DQSGCNTRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    mcCLR_BIT(u4value, POS_SPCMD_DQSGCNTRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
#endif

#if 0
    // for testing
    // 2013/7/15, for DQ as commands case, once PHY reset, need to enter self refresh for safe operation to DDR
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CLK1DELAY), &u4temp);
    if (mcTEST_BIT(u4temp, POS_CLK1DELAY_DQCMD))
    {
        DramcEnterSelfRefresh(p, 1);
    }
#endif

#if 0
    // reset phy
    // 0x0f0[28] = 1 -> 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
    mcSET_BIT(u4value, POS_PHYCTL1_PHYRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value);

    // read data counter reset
    // 0x0f4[25] = 1 -> 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), &u4value);
    mcSET_BIT(u4value, POS_GDDR3CTL1_RDATRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), u4value);

    //delay 10ns, need to change when porting
    mcDELAY_US(1);  

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
    mcCLR_BIT(u4value, POS_PHYCTL1_PHYRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value);    

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), &u4value);
    mcCLR_BIT(u4value, POS_GDDR3CTL1_RDATRST);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), u4value);
#endif

#ifdef SPM_MODIFY
    // DIV2 clock phase sync
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
    mcCLR_BIT(u4value, 5);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
    mcSET_BIT(u4value, 5);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
#else
    // DIV2 clock phase sync
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
    mcSET_BIT(u4value, 5);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
    mcCLR_BIT(u4value, 5);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
#endif    


#if 0
    // for testing
    // 2013/7/15, for DQ as commands case, after PHY reset, exit self refresh
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CLK1DELAY), &u4temp);
    if (mcTEST_BIT(u4temp, POS_CLK1DELAY_DQCMD))
    {
        // wait tCKESR between entry and exit self refresh (no need since we have delay in PHY reset)
        //mcDELAY_US(1);
        DramcEnterSelfRefresh(p, 0);
    }
#endif
}
#endif

void dqs_gw_result(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4coarse_result01, u4coarse_result23, u4golden_counter;

    // enable TE2, audio pattern
    DramcEngine2(p, TE_OP_READ_CHECK, 0x55000000, 0xaa000000 | DQS_GW_TE_OFFSET, 1, 0, 0, 0);

    // Get 0x3c0, 0x3c4 : coarse result of DQS0, 1, 2, 3
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSGNWCNT0), &u4coarse_result01);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSGNWCNT1), &u4coarse_result23);

    mcSHOW_DBG_MSG2(("%x    %x\n", u4coarse_result01, u4coarse_result23));
    mcFPRINTF((fp_A60808, "%x    %x\n", u4coarse_result01, u4coarse_result23));

    u4golden_counter = DQS_GW_GOLD_COUNTER_32BIT;
    
    if ((u4coarse_result01 == u4golden_counter) && (u4coarse_result23 == u4golden_counter))    
    {
        dqs_gw[dqs_gw_coarse] |= (1 << dqs_gw_fine); //attension:dqs_gw_fine steps must less then 32
    }    

    dqs_gw_fine++;
    dqs_gw_fine_cnt++;
    
    if ((dqs_gw_fine >= DQS_GW_LEN_PER_COARSE_ELEMENT) || (dqs_gw_fine_cnt >= DQS_GW_FINE_MAX))
    {
        dqs_gw_coarse++; 
        dqs_gw_fine = 0;
        
        if (dqs_gw_fine_cnt >= DQS_GW_FINE_MAX)
        {
            dqs_gw_fine_cnt = 0;
        }        
    }
    if (dqs_gw_coarse > DQS_GW_LEN) 
    {
        mcSHOW_ERR_MSG(("Critical error!! dqs_gw_coarse > DQS_GW_LEN\n"));
    }

    
}

void dqs_gw_calib(DRAMC_CTX_T *p)
{
    U8 i, j;
    //U32 u4value;
    
    for ( i = DQS_GW_COARSE_START; i <= DQS_GW_COARSE_END; i+=DQS_GW_COARSE_STEP) 
    {
        //adjust factor steps
        dqsi_gw_dly_coarse_factor_handler(p, i);        

        for ( j = DQS_GW_FINE_START; j <= DQS_GW_FINE_END; j+=DQS_GW_FINE_STEP) 
        {
            //adjust factor steps
            dqsi_gw_dly_fine_factor_handler(p, j);

            mcSHOW_DBG_MSG2(("COARSE TUNE: %d; FINE TUNE: %d -- ", i, j));
            mcFPRINTF((fp_A60808, "COARSE TUNE: %d; FINE TUNE: %d -- ", i, j));
            //ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), &u4value);
            //mcSHOW_DBG_MSG(("%x, ", u4value));
            //ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x404), &u4value);
            //mcSHOW_DBG_MSG(("%x, ", u4value));
            //ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x40c), &u4value);
            //mcSHOW_DBG_MSG(("%x, ", u4value));
            
            // gating counter, phy and read data counter reset
            dqs_gw_counter_reset(p);

            //run test code
            dqs_gw_result(p);
        }
        dqs_gw_fine = 0;    
        dqs_gw_fine_cnt = 0;
    }    
    dqs_gw_coarse = 0;

    // gating counter, phy and read data counter reset
    dqs_gw_counter_reset(p);
}

DRAM_STATUS_T DramcRxdqsGatingCal(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4value, dqs_gw_val;
    U8 i, j, k, opt_coarse_idx, opt_fine_idx, cnt, first_bit_idx, max=0;
#ifdef fcNEW_GATING_FINETUNE_LIMIT
    U8 opt_fine_idx_0, first_bit_idx_0;
#endif
    
#ifdef fcNEW_GATING_FINETUNE_LIMIT_2
    U8 opt_fine_idx_new = 0xff, opt_fine_idx_old = 0xff;
#endif

#ifdef FINETUNE_CENTER
    U8 MinCenterDiff = 0xff, CenterDiff, fine_idx;
#endif

    mcSHOW_DBG_MSG(("\n\n[Channel %d] [Rank %d] Start tuning DRAMC Gating Window for rank 0...\n\n", p->channel, CurrentRank));

    dqs_gw_coarse = 0;      //from begin of coarse tune, reset to 0
    dqs_gw_fine = 0;          //from begin of fine tune, reset to 0
    dqs_gw_fine_cnt = 0;   //from begin of fine tune, reset to 0
    dqs_gw_cnt_break = 0;

    for (i = 0; i < DQS_GW_LEN; i++) 
    {
        dqs_gw[i] = 0;
    }

    #if 1
    // Disable HW gating first
    // 0x1c0[31]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), &u4value);
    mcCLR_BIT(u4value, POS_DQSCAL0_STBCALEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), u4value);
    #endif
    
    /* 1.enable burst mode for gating window */
    /*   enable DQS gating window counter */
    // 0x0e0[28] = 1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), &u4value);
    mcSET_BIT(u4value, POS_DQSCTL1_DQSIENMODE);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), u4value);
    // 0x1e4[8] = 1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
    mcSET_BIT(u4value, POS_SPCMD_DQSGCNTEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);

    // new gating, dual-phase DQS clock gating control enabling
    // 0x124[30] = 1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSGCTL), &u4value);
    mcSET_BIT(u4value, POS_DQSGCTL_DQSGDUALP);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSGCTL), u4value);

    dqs_gw_calib(p);
    
    for (i = 0; i < DQS_GW_COARSE_MAX; i++) 
    {
        // find the max passed window
        cnt = 0;
        dqs_gw_cnt_break = 0;
        for (j = 0; j < DQS_GW_LEN_PER_COARSE; j++)
        {        
            cnt += nr_bit_set(dqs_gw[i*DQS_GW_LEN_PER_COARSE + j]);
            if (dqs_gw_cnt_break == 1)
            {
                // Has count but the next is "0", break
                break;
            }
        }
            
    #ifdef fcNEW_GATING_FINETUNE_LIMIT_2
        if (cnt) 
        {
            for (j = 0; j < DQS_GW_LEN_PER_COARSE; j++)
            {
                first_bit_idx = first_bit_set(dqs_gw[i*DQS_GW_LEN_PER_COARSE+j]);
                if (first_bit_idx != (0xff))
                {
                    opt_fine_idx_new = j*DQS_GW_LEN_PER_COARSE_ELEMENT + first_bit_idx + cnt / 2;                    
                    break;
                }
            }

            mcSHOW_DBG_MSG2(("[New Gating with FineTune Limit2] cnt=%d, first_bit_idx=%d, coarse_idx=%d, opt_fine_idx_new=%d,  opt_fine_idx_old=%d\n", cnt, first_bit_idx, i, opt_fine_idx_new, opt_fine_idx_old));                
            if (opt_fine_idx_new > opt_fine_idx_old)
            {
                mcSHOW_DBG_MSG(("[New Gating with FineTune Limit2] Current OPT finetune > Previous. Fake window. BREAK!!\n"));
                
                break;
            }
            else
            {
                opt_fine_idx_old = opt_fine_idx_new;
            }
        }
    #endif
            
    #ifdef FINETUNE_CENTER
      	if (cnt)
      	{
		for (j = 0; j < DQS_GW_LEN_PER_COARSE; j++)
		{
			first_bit_idx = first_bit_set(dqs_gw[i*DQS_GW_LEN_PER_COARSE+j]);
			if (first_bit_idx != (0xff))
			{
				fine_idx = j*DQS_GW_LEN_PER_COARSE_ELEMENT + first_bit_idx + cnt / 2;                    
				break;
			}
		}    
		fine_idx = DQS_GW_FINE_START+fine_idx*DQS_GW_FINE_STEP;

		if (fine_idx > (DQS_GW_FINE_END>>1))
		{
			CenterDiff = fine_idx - (DQS_GW_FINE_END>>1);
		}
		else
		{
			CenterDiff = (DQS_GW_FINE_END>>1) - fine_idx ;
		}
		mcSHOW_DBG_MSG(("first_bit_idx=%d, fine_idx=%d, CenterDiff=%d, MinCenterDiff=%d\n", 
			first_bit_idx, fine_idx, CenterDiff, MinCenterDiff));
	        if (CenterDiff < MinCenterDiff)
	        {
	        	MinCenterDiff = CenterDiff;
			max = cnt;
			opt_coarse_idx = i;
	        }
	        if (first_bit_idx==0)
	        {
	        	break;
	        }
      	}
    #else            
        if (cnt >= max)   // Edward : coarse tune with left window has smaller shift in case of voltage change.
        {
            max = cnt;
            opt_coarse_idx = i;
        }
     #endif
    }

#ifdef DUAL_RANKS
// MT6595 coarse tune could be set into different values in dual ranks no matter 0.25T or 1T. If something wrong, need to enable this again.  
#endif

#ifdef CUSTOMIZE_COARSE_TUNE
    // 1 M_CK difference + 0.25 M_CK difference.  R0>R1	(28, 27)
    // 1 M_CK difference + 0.25 M_CK difference.  R0<R1   (27, 28)
    // 1 M_CK difference.  R0>R1	(28,24)  (29,25)
    // 1 M_CK difference.  R0<R1  (24,28)  (29,25)
    // 0.25 M_CK difference.  R0>R1 (27,24) (26,25)
    // 0.25 M_CK difference.  R0<R1 (24,27) (25,26)
    if (CurrentRank==0)
    {
    	if (p->channel==CHANNEL_A)
	    	opt_coarse_idx = 30;
    	else
	    	opt_coarse_idx = 29;
    }
    else
    {
    	if (p->channel==CHANNEL_A)
	    	opt_coarse_idx = 28;
    	else
	    	opt_coarse_idx = 27;
    }
    opt_coarse_idx -=DQS_GW_COARSE_START;
    for (j = 0; j < DQS_GW_LEN_PER_COARSE; j++)
    {        
        cnt += nr_bit_set(dqs_gw[opt_coarse_idx*DQS_GW_LEN_PER_COARSE + j]);
        if (dqs_gw_cnt_break == 1)
        {
            // Has count but the next is "0", break
            break;
        }
    }  
    max = cnt;
#endif

    cnt = max;
    if (cnt) 
    {
        for (i = 0; i < DQS_GW_LEN_PER_COARSE; i++)
        {
            first_bit_idx = first_bit_set(dqs_gw[opt_coarse_idx*DQS_GW_LEN_PER_COARSE+i]);
            if (first_bit_idx != (0xff))
            {
                opt_fine_idx = i*DQS_GW_LEN_PER_COARSE_ELEMENT + first_bit_idx + cnt / 2;
                break;
            }
        }        

    #ifdef fcNEW_GATING_FINETUNE_LIMIT   // to filter fake passed window
        for (i = 0; i < DQS_GW_LEN_PER_COARSE; i++)
        {
            first_bit_idx_0 = first_bit_set(dqs_gw[(opt_coarse_idx-1)*DQS_GW_LEN_PER_COARSE+i]);
            if (first_bit_idx_0 != (0xff))
            {
                opt_fine_idx_0 = i*DQS_GW_LEN_PER_COARSE_ELEMENT + first_bit_idx_0;
                break;
            }
        }

        mcSHOW_DBG_MSG2(("[New Gating with FineTune Limit] opt_coarse_idx=%d/%d, first_bit_idx=%d/%d, opt_fine_idx=%d/%d\n", opt_coarse_idx-1, opt_coarse_idx, first_bit_idx_0, first_bit_idx, opt_fine_idx_0, opt_fine_idx));
        mcFPRINTF((fp_A60808, "[New Gating with FineTune Limit] opt_coarse_idx=%d/%d, first_bit_idx=%d/%d, opt_fine_idx=%d/%d\n", opt_coarse_idx-1, opt_coarse_idx, first_bit_idx_0, first_bit_idx, opt_fine_idx_0, opt_fine_idx));

        if ((first_bit_idx_0>first_bit_idx) && (opt_fine_idx_0 < opt_fine_idx))
        {
            opt_fine_idx = opt_fine_idx_0;
            mcSHOW_DBG_MSG2(("[New Gating with FineTune Limit] Limit opt_fine_idx to %d!!\n", opt_fine_idx_0));
            mcFPRINTF((fp_A60808, "[New Gating with FineTune Limit] Limit opt_fine_idx to %d!!\n", opt_fine_idx_0));
        }        
    #endif
        
        opt_gw_coarse_value = DQS_GW_COARSE_START+opt_coarse_idx*DQS_GW_COARSE_STEP;
        opt_gw_fine_value = DQS_GW_FINE_START+opt_fine_idx*DQS_GW_FINE_STEP;

#ifdef DUAL_RANKS
	if (CurrentRank==0) 
	{
		opt_gw_coarse_value_R0[p->channel] = opt_gw_coarse_value;
		opt_gw_fine_value_R0[p->channel] = opt_gw_fine_value;
	}
	else
	{
		opt_gw_coarse_value_R1[p->channel] = opt_gw_coarse_value;
		opt_gw_fine_value_R1[p->channel] = opt_gw_fine_value;
	}
#endif	
	
	
        mcSHOW_DBG_MSG(("*********************************************************\n"));
        mcSHOW_DBG_MSG(("Rank %d DQS GW Calibration\n", CurrentRank));
        mcSHOW_DBG_MSG(("Optimal coarse tune value %d, optimal fine tune value %d\n", opt_gw_coarse_value, opt_gw_fine_value));
        mcSHOW_DBG_MSG(("*********************************************************\n"));

        mcFPRINTF((fp_A60808, "*********************************************************\n"));
        mcFPRINTF((fp_A60808, "Rank %d DQS GW Calibration\n", CurrentRank));
        mcFPRINTF((fp_A60808, "Optimal coarse tune value %d, optimal fine tune value %d\n", opt_gw_coarse_value, opt_gw_fine_value));
        mcFPRINTF((fp_A60808, "*********************************************************\n"));
        
        for (i = 0; i < DQS_GW_FINE_MAX; i++)
        {
            if ((i%16) == 0)
            {
                mcSHOW_DBG_MSG(("\n    "));
                mcFPRINTF((fp_A60808, "\n    "));
            }
            mcSHOW_DBG_MSG(("%d", DQS_GW_FINE_START+i*DQS_GW_FINE_STEP));
            mcFPRINTF((fp_A60808, "%4d", DQS_GW_FINE_START+i*DQS_GW_FINE_STEP));
        }
        mcSHOW_DBG_MSG(("\n     ---------------------------------------------------------------\n"));
        mcFPRINTF((fp_A60808, "\n     ---------------------------------------------------------------\n"));
        
        for (i = 0; i < DQS_GW_COARSE_MAX; i++)
        {
            for (j = 0; j < DQS_GW_LEN_PER_COARSE; j++)
            {
                dqs_gw_val = dqs_gw[i*DQS_GW_LEN_PER_COARSE+j];
                for (k = 0; k < DQS_GW_LEN_PER_COARSE_ELEMENT; k++)
                {
                    if ((k%16) == 0)
                    {
                        mcSHOW_DBG_MSG(("\n%d|",DQS_GW_COARSE_START+i*DQS_GW_COARSE_STEP));
                        mcFPRINTF((fp_A60808, "\n%d|",DQS_GW_COARSE_START+i*DQS_GW_COARSE_STEP));
                    }
                    mcSHOW_DBG_MSG(("%d", ((dqs_gw_val >> k) & 0x1)));
                    mcFPRINTF((fp_A60808, "%d", ((dqs_gw_val >> k) & 0x1)));
                    if ((j == (DQS_GW_LEN_PER_COARSE-1)) && ((j*DQS_GW_LEN_PER_COARSE_ELEMENT+k) == (DQS_GW_FINE_MAX-1)))
                    {
                        break;
                    }
                }
            }
        }

        mcSHOW_DBG_MSG(("\n"));
        mcFPRINTF((fp_A60808, "\n"));

        /* setup the opt coarse value and fine value according to calibration result*/
        dqsi_gw_dly_coarse_factor_handler(p, opt_gw_coarse_value);
        dqsi_gw_dly_fine_factor_handler(p, opt_gw_fine_value);        
    }
    else 
    {
        mcSHOW_ERR_MSG(("Cannot find any pass-window\n"));
    #ifdef DDR_FT_LOAD_BOARD
        LoadBoardShowResult(FLAG_GATING_CALIBRATION, FLAG_CALIBRATION_FAIL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1); 
    #endif
    }

#ifdef DDR_FT_LOAD_BOARD
    // To check gating window size reasonable?! (consider fake passed window)
#endif
    
    /* disable DQS gating window counter */
    // 0x1e4[8] = 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), &u4value);
    mcCLR_BIT(u4value, POS_SPCMD_DQSGCNTEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMD), u4value);
    
    #if 0 // for testing, move to the last
    // Enable HW gating here?!
    // 0x1c0[31]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), &u4value);
    mcSET_BIT(u4value, POS_DQSCAL0_STBCALEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), u4value);
    #endif
    
// DramcRxdqsGatingCal_exit:
    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }

/*
          0    8   16   24   32   40   48   56   64   72   80   88   96  104  112  120
      --------------------------------------------------------------------------------
0007:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
0008:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
0009:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
000A:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
000B:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
000C:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
000D:|    0    0    0    0    0    0    0    0    0    0    0    0    1    1    1    1
000E:|    0    0    0    0    0    0    0    1    1    1    1    1    1    1    1    1
000F:|    0    0    0    1    1    1    1    1    1    1    1    0    0    0    0    0
0010:|    1    1    1    1    1    1    1    0    0    0    0    0    0    0    0    0
0011:|    1    1    1    0    0    0    0    0    0    0    0    0    0    0    0    0
0012:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
0013:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
0014:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
0015:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
0016:|    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0    0
*/
}

int mt_get_rank_number(void)
{
    if((opt_gw_coarse_value_R0[0] != 0) &&
       (opt_gw_coarse_value_R0[1] != 0) &&
       (opt_gw_coarse_value_R1[0] != 0) &&
       (opt_gw_coarse_value_R1[1] != 0))
    {
        //rank 0 and rank 1 calibration pass
        return 2;
    }
    else if( 
       (opt_gw_coarse_value_R0[0] != 0) &&
       (opt_gw_coarse_value_R0[1] != 0) &&
       (opt_gw_coarse_value_R1[0] == 0) &&
       (opt_gw_coarse_value_R1[1] == 0))
    {
        //rank 0 calibration pass, rank 1 calibration fail
        return 1;
    } 
    else
        return -1;  //error    
}

#ifdef MATYPE_ADAPTATION	
U8 MATYPE_Adaptation(DRAMC_CTX_T *p, U32 u4Rank)
{
	U8 ucstatus = 0;
	U32 u4value, u4Rank_Col;

	// Modify MA type based on EMI_CONA
	u4value = *(volatile unsigned *)(EMI_APB_BASE+0x0);

	if (u4Rank == 0)
	{
		// Rank 0.
		if (p->channel == CHANNEL_A)
		{
			// Channel A
			u4Rank_Col = mcGET_FIELD(u4value, 0x00000030, 4);
		}
		else
		{
			// Channel B.
			u4Rank_Col = mcGET_FIELD(u4value, 0x00300000, 20);
		}
	}
	else
	{
		// Rank 1.
		if (p->channel == CHANNEL_A)
		{
			// Channel A
			u4Rank_Col = mcGET_FIELD(u4value, 0x000000c0, 6);
		}
		else
		{
			// Channel B.
			u4Rank_Col = mcGET_FIELD(u4value, 0x00c00000, 22);
		}
	}

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x04), &u4value);
	
	if (u4Rank_Col<3) 
	{
	        mcSET_FIELD(u4value, u4Rank_Col+1, 0x00000300, 8);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x04), u4value);
	}
	else
	{
		// Should not be 3. Something wrong...
		return 1;
	}		

	return ucstatus;
}
#endif

DRAM_STATUS_T DualRankDramcRxdqsGatingCal(DRAMC_CTX_T *p)
{
	U8 ucstatus;
	U32 uiTemp, DQSINCTL;
	DRAM_STATUS_T Ret = DRAM_OK;

	// Rank 0 GW calibration.
	CurrentRank = 0;
#ifdef MATYPE_ADAPTATION	
	MATYPE_Adaptation(p, 0);
#endif

	DramcRxdqsGatingCal(p);	

	// Get Reg.e0h[27:24] DQSINCTL after rank 0 calibration.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe0), &DQSINCTL);	
	DQSINCTL = (DQSINCTL >> 24) & 0xf;


	// Rank 1 GW calibration.
	// Swap CS0 and CS1.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &uiTemp);
	uiTemp = uiTemp |0x08;
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), uiTemp);

	CurrentRank = 1;
#ifdef MATYPE_ADAPTATION	
	MATYPE_Adaptation(p, 1);
#endif
	DramcRxdqsGatingCal(p);	

	//No need to set rank 1 coarse tune because only one set registers for coarse tune in 808
        dqsi_gw_dly_coarse_factor_handler_rank1(p, opt_gw_coarse_value_R1[p->channel], DQSINCTL);
        dqsi_gw_dly_fine_factor_handler_rank1(p, opt_gw_fine_value_R1[p->channel]);     
        
	// Swap CS back.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &uiTemp);
	uiTemp = uiTemp & (~0x08);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), uiTemp);

	CurrentRank = 0;
#ifdef MATYPE_ADAPTATION	
	MATYPE_Adaptation(p, 0);
#endif	
	// Set rank 0 coarse tune and fine tune back.
        dqsi_gw_dly_coarse_factor_handler(p, opt_gw_coarse_value_R0[p->channel]);
        dqsi_gw_dly_fine_factor_handler(p, opt_gw_fine_value_R0[p->channel]);        


        return Ret;
}
#endif
//-------------------------------------------------------------------------
/** DramcRxWindowPerbitCal (v2 version)
 *  start the rx dqs perbit sw calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
DRAM_STATUS_T DramcRxWindowPerbitCal(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0, fgfail, ii, jj, kk;
    U32 u4value, u4err_value, u4fail_bit;

    RXDQS_PERBIT_DLY_T dqdqs_perbit_dly[DQ_DATA_WIDTH];
    U8 ucbit_first, ucbit_last, uchold_pass_number, ucsetup_pass_number;
    U8 ucmax_dqsdly_byte[DQS_NUMBER];
    
    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }    

    // for debug
    mcSHOW_DBG_MSG3(("\n[Channel %d] RX DQS per bit para : test pattern=%d, test2_1=0x%x, test2_2=0x%x, LSW_HSR=%d\n", p->channel, p->test_pattern, p->test2_1, p->test2_2, p->fglow_freq_write_en));    

    // 1.delay DQ ,find the pass widnow (left boundary).
    // 2.delay DQS find the pass window (right boundary). 
    // 3.Find the best DQ / DQS to satify the middle value of the overall pass window per bit
    // 4.Set DQS delay to the max per byte, delay DQ to de-skew

    // 1
    // set DQS delay to 0 first
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DELDLY1), 0);

    fgfail = 0;

    // initialize parameters
    for (ii = 0; ii < p->data_width; ii++)
    {
        dqdqs_perbit_dly[ii].first_dqdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqdly_pass = -2;
        dqdqs_perbit_dly[ii].first_dqsdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqsdly_pass = -2;
        dqdqs_perbit_dly[ii].best_first_dqdly_pass = -1;
        dqdqs_perbit_dly[ii].best_last_dqdly_pass = -2;
        dqdqs_perbit_dly[ii].best_first_dqsdly_pass = -1;
        dqdqs_perbit_dly[ii].best_last_dqsdly_pass = -2;        
    }

    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("Start DQ delay to find pass range, DQS delay fixed to 0...\n"));
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("x-axis is bit #; y-axis is DQ delay (%d~%d)\n", 0, MAX_RX_DQDLY_TAPS-1));

    mcFPRINTF((fp_A60808, "Start RX DQ/DQS calibration...\n"));
    mcFPRINTF((fp_A60808, "x-axis is bit #; y-axis is DQ delay (%d~%d)\n", 0, MAX_RX_DQDLY_TAPS-1));
    
    // delay DQ from 0 to 15 to get the setup time
    for (ii = 0; ii < MAX_RX_DQDLY_TAPS; ii++)
    {
        for (jj=0; jj<p->data_width; jj=jj+4)
        {
            //every 4bit dq have the same delay register address
            u4value = ((U32) ii) + (((U32)ii)<<8) + (((U32)ii)<<16) + (((U32)ii)<<24);  
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQIDLY1+jj), u4value);            
        }

/*
No need due to no RX DQS glitch for A60808
        //Reset after scan to avoid error gating counter due to DQS glitch
        //reset phy R_DMPHYRST: 0xf0[28] 
        // 0x0f0[28] = 1 -> 0
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
        mcSET_BIT(u4value, POS_PHYCTL1_PHYRST);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value);
        //delay 10ns, 1ms here
        mcDELAY_MS(1);
        mcCLR_BIT(u4value, POS_PHYCTL1_PHYRST);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value);

        // read data counter reset
        // 0x0f4[25] = 1 -> 0
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), &u4value);
        mcSET_BIT(u4value, POS_GDDR3CTL1_RDATRST);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), u4value);
        //delay 10ns, 1ms here
        mcDELAY_MS(1);                
        mcCLR_BIT(u4value, POS_GDDR3CTL1_RDATRST);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_GDDR3CTL1), u4value);       
*/
#ifdef XTALK_SSO_K
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~0x01);	// disable dual channel.
	if (p->channel == CHANNEL_A)
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~(0x01<<18));
	}
	else
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= ((0x01<<18));
	}
        u4err_value = DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, DDR_BASE, 
        	DDR_BASE+0x100000, XTALK_SSO_LEN<<3, 8, 1, 1);

	#ifdef SINGLE_CHANNEL_ENABLE        
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) = LPDDR3_EMI_CONA;
	#else
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= (0x01);	// enable dual channel.
        #endif
#else
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }
            else
            {
                u4err_value = 0;
                for (jj = 0; jj < 1; jj++)
                {
                    u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
                }
            }
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
            else
            {
                u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("Not support test pattern!! Use audio pattern by default.\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }        
#endif

        // check fail bit ,0 ok ,others fail
        for (jj = 0; jj < p->data_width; jj++)
        {
            u4fail_bit = u4err_value&((U32)1<<jj);

            if (u4fail_bit == 0)
            {
                if (dqdqs_perbit_dly[jj].first_dqdly_pass == -1)
                {
                    // first DQ pass delay tap
                    dqdqs_perbit_dly[jj].first_dqdly_pass = ii;
                }
                if (dqdqs_perbit_dly[jj].last_dqdly_pass == -2)
                {
                    if (ii == (MAX_RX_DQDLY_TAPS-1))
                    {
                        // pass to the last tap
                        dqdqs_perbit_dly[jj].last_dqdly_pass = ii;
                        if ( (dqdqs_perbit_dly[jj].last_dqdly_pass-dqdqs_perbit_dly[jj].first_dqdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqdly_pass-dqdqs_perbit_dly[jj].best_first_dqdly_pass))
                        {
    			     dqdqs_perbit_dly[jj].best_last_dqdly_pass =  dqdqs_perbit_dly[jj].last_dqdly_pass;
    			     dqdqs_perbit_dly[jj].best_first_dqdly_pass = dqdqs_perbit_dly[jj].first_dqdly_pass;
                        }	  
                        // clear to find the next pass range if it has
                        dqdqs_perbit_dly[jj].first_dqdly_pass = -1;
                        dqdqs_perbit_dly[jj].last_dqdly_pass = -2;                        
                    }
                }
            }
            else
            {
                if ((dqdqs_perbit_dly[jj].first_dqdly_pass != -1)&&(dqdqs_perbit_dly[jj].last_dqdly_pass == -2))
                {
                    dqdqs_perbit_dly[jj].last_dqdly_pass = ii -1;
                    if ( (dqdqs_perbit_dly[jj].last_dqdly_pass-dqdqs_perbit_dly[jj].first_dqdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqdly_pass-dqdqs_perbit_dly[jj].best_first_dqdly_pass))
                    {
			     dqdqs_perbit_dly[jj].best_last_dqdly_pass =  dqdqs_perbit_dly[jj].last_dqdly_pass;
			     dqdqs_perbit_dly[jj].best_first_dqdly_pass = dqdqs_perbit_dly[jj].first_dqdly_pass;
                }
                    // clear to find the next pass range if it has
                    dqdqs_perbit_dly[jj].first_dqdly_pass = -1;
                    dqdqs_perbit_dly[jj].last_dqdly_pass = -2;
                }
            }

                    
                
            if (u4fail_bit == 0)
            {
                mcSHOW_DBG_MSG3(("o"));
                mcFPRINTF((fp_A60808, "o"));
            }
            else
            {
                {
                    mcSHOW_DBG_MSG3(("x"));
                    mcFPRINTF((fp_A60808, "x"));
                }
            }
            
        }
        mcSHOW_DBG_MSG3(("\n"));
        mcFPRINTF((fp_A60808, "\n"));
    }

    // 2
    //set dq delay to 0
    for (ii=0; ii<p->data_width; ii=ii+4)
    {
        //every 4bit dq have the same delay register address
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQIDLY1+ii), 0);            
    }

    //dqs from 1
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("Start DQS delay to find pass range, DQ delay fixed to 0...\n"));
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("x-axis is bit #; y-axis is DQS delay (%d~%d)\n", 1, MAX_RX_DQSDLY_TAPS-1));

    mcFPRINTF((fp_A60808, "-----------------------\n"));
    mcFPRINTF((fp_A60808, "x-axis is bit #; y-axis is DQS delay (%d~%d)\n", 1, MAX_RX_DQSDLY_TAPS-1));
    
    // because the tap DQdly=0 DQSdly=0 will be counted when we delay dq ,so we don't count it here
    // so we set first dqs delay to 1
    for (ii = 1; ii < MAX_RX_DQSDLY_TAPS; ii++)
    {
        // 0x18
        u4value = 0;
        for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
        {
            u4value += (((U32)ii)<<(8*jj));        
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DELDLY1), u4value);        
        
#ifdef XTALK_SSO_K
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~0x01);	// disable dual channel.
	if (p->channel == CHANNEL_A)
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~(0x01<<18));
	}
	else
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= ((0x01<<18));
	}
        u4err_value = DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, DDR_BASE, 
        	DDR_BASE+0x100000, XTALK_SSO_LEN<<3, 8, 1, 1);
        
	#ifdef SINGLE_CHANNEL_ENABLE        
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) = LPDDR3_EMI_CONA;
	#else
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= (0x01);	// enable dual channel.
        #endif
#else
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }
            else
            {
                u4err_value = 0;
                for (jj = 0; jj < 1; jj++)
                {
                    u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
                }
            }          
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
            else
            {
                u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("not support test pattern\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }
#endif


        // check fail bit ,0 ok ,others fail
        for (jj = 0; jj < p->data_width; jj++)
        {
            u4fail_bit = u4err_value&((U32)1<<jj);

            if (u4fail_bit == 0)
            {
                if (dqdqs_perbit_dly[jj].first_dqsdly_pass == -1)
                {
                    // first DQS pass delay tap
                    dqdqs_perbit_dly[jj].first_dqsdly_pass = ii;
                }
                if (dqdqs_perbit_dly[jj].last_dqsdly_pass == -2)
                {
                    if (ii == (MAX_RX_DQSDLY_TAPS-1))
                    {
                        // pass to the last tap
                        dqdqs_perbit_dly[jj].last_dqsdly_pass = ii;
                        if ( (dqdqs_perbit_dly[jj].last_dqsdly_pass-dqdqs_perbit_dly[jj].first_dqsdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqsdly_pass-dqdqs_perbit_dly[jj].best_first_dqsdly_pass))
                        {
    			     dqdqs_perbit_dly[jj].best_last_dqsdly_pass =  dqdqs_perbit_dly[jj].last_dqsdly_pass;
    			     dqdqs_perbit_dly[jj].best_first_dqsdly_pass = dqdqs_perbit_dly[jj].first_dqsdly_pass;
                        }	  
                        // clear to find the next pass range if it has
                        dqdqs_perbit_dly[jj].first_dqsdly_pass = -1;
                        dqdqs_perbit_dly[jj].last_dqsdly_pass = -2;                               
                    }
                }
            }
            else
            {
                if ((dqdqs_perbit_dly[jj].first_dqsdly_pass != -1)&&(dqdqs_perbit_dly[jj].last_dqsdly_pass == -2))
                {
                    dqdqs_perbit_dly[jj].last_dqsdly_pass = ii -1;
                    if ( (dqdqs_perbit_dly[jj].last_dqsdly_pass-dqdqs_perbit_dly[jj].first_dqsdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqsdly_pass-dqdqs_perbit_dly[jj].best_first_dqsdly_pass))
                    {
			     dqdqs_perbit_dly[jj].best_last_dqsdly_pass =  dqdqs_perbit_dly[jj].last_dqsdly_pass;
			     dqdqs_perbit_dly[jj].best_first_dqsdly_pass = dqdqs_perbit_dly[jj].first_dqsdly_pass;
                    }	  
                    // clear to find the next pass range if it has
                    dqdqs_perbit_dly[jj].first_dqsdly_pass = -1;
                    dqdqs_perbit_dly[jj].last_dqsdly_pass = -2;                                       
                }
            }

            if (u4fail_bit == 0)
            {
                mcSHOW_DBG_MSG3(("o"));
                mcFPRINTF((fp_A60808, "o"));
            }
            else
            {
                {
                    mcSHOW_DBG_MSG3(("x"));
                    mcFPRINTF((fp_A60808, "x"));
                }
            }            
        }
        mcSHOW_DBG_MSG3(("\n"));
        mcFPRINTF((fp_A60808, "\n"));
    }

    // 3
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("Start calculate dq time and dqs time / \n"));
    mcSHOW_DBG_MSG3(("Find max DQS delay per byte / Adjust DQ delay to align DQS...\n"));
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 

    mcFPRINTF((fp_A60808, "------------------------------------------------------\n")); 
    mcFPRINTF((fp_A60808, "Start calculate dq time and dqs time / \n"));
    mcFPRINTF((fp_A60808, "Find max DQS delay per byte / Adjust DQ delay to align DQS...\n"));
    mcFPRINTF((fp_A60808, "------------------------------------------------------\n")); 

    u2rx_window_sum = 0;
    //As per byte, check max DQS delay in 8-bit. Except for the bit of max DQS delay, delay DQ to fulfill setup time = hold time
    for (ii = 0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        ucbit_first = DQS_BIT_NUMBER*ii;
        ucbit_last = DQS_BIT_NUMBER*ii+DQS_BIT_NUMBER-1;
        ucmax_dqsdly_byte[ii] = 0;
        for (jj = ucbit_first; jj <= ucbit_last; jj++)
        {
            // hold time = DQS pass taps
            uchold_pass_number = dqdqs_perbit_dly[jj].best_last_dqsdly_pass - dqdqs_perbit_dly[jj].best_first_dqsdly_pass + 1;
            // setup time = DQ pass taps
            ucsetup_pass_number = dqdqs_perbit_dly[jj].best_last_dqdly_pass - dqdqs_perbit_dly[jj].best_first_dqdly_pass + 1;
            u2rx_window_sum += (uchold_pass_number+ucsetup_pass_number);

            if (uchold_pass_number > ucsetup_pass_number)
            {
                if (ucsetup_pass_number != 0)
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxoooooooo|ooooooooooooooooooooxxxxx
                    dqdqs_perbit_dly[jj].best_dqdly = 0;
                    dqdqs_perbit_dly[jj].best_dqsdly = (uchold_pass_number - ucsetup_pass_number) / 2;

                    if (dqdqs_perbit_dly[jj].best_dqsdly > ucmax_dqsdly_byte[ii])
                    {
                        ucmax_dqsdly_byte[ii] = dqdqs_perbit_dly[jj].best_dqsdly;
                    }
                }
                else
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxxxxxxxxx|xxxooooooooooxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqdly = 0;
                    dqdqs_perbit_dly[jj].best_dqsdly = (uchold_pass_number - ucsetup_pass_number) / 2 + dqdqs_perbit_dly[jj].best_first_dqsdly_pass;

                    if (dqdqs_perbit_dly[jj].best_dqsdly > ucmax_dqsdly_byte[ii])
                    {
                        ucmax_dqsdly_byte[ii] = dqdqs_perbit_dly[jj].best_dqsdly;
                    }
                }
            }
            else if (uchold_pass_number < ucsetup_pass_number)
            {
                if (uchold_pass_number != 0)
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxoooooooooooooooooo|ooooooooxxxxxxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = (ucsetup_pass_number - uchold_pass_number) / 2;                    
                }
                else
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxoooooooooooooooxxx|xxxxxxxxxxxxxxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = (ucsetup_pass_number - uchold_pass_number) / 2 + dqdqs_perbit_dly[jj].best_first_dqdly_pass;                    
                }
            }
            else   // hold time == setup time
            {
                if (uchold_pass_number != 0)
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxxxoooooo|ooooooxxxxxxxxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = 0;    
                }
                else
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxxxxxxxxxx
                    // mean this bit is error
                    mcSHOW_DBG_MSG4(("error on bit %d ,setup_time =hold_time =0!!\n", jj));
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = 0;
                    fgfail = 1;
                #ifdef DDR_FT_LOAD_BOARD
                    if (RXPERBIT_LOG_PRINT == 1) // ignore duty DramcClkDutyCal()
                    {
                        LoadBoardShowResult(FLAG_RX_CALIBRATION, FLAG_CALIBRATION_FAIL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
                        while(1); 
                    }
                #endif
                }
            }            
            mcSHOW_DBG_MSG4(("bit#%d : dq time=%d dqs time=%d win=%d center=(%d, %d)\n", jj, ucsetup_pass_number, uchold_pass_number, ucsetup_pass_number+uchold_pass_number, dqdqs_perbit_dly[jj].best_dqdly, dqdqs_perbit_dly[jj].best_dqsdly));
            mcFPRINTF((fp_A60808, "bit#%2d : dq time=%2d dqs time=%2d win=%2d center=(%2d, %2d)\n", jj, ucsetup_pass_number, uchold_pass_number, ucsetup_pass_number+uchold_pass_number, dqdqs_perbit_dly[jj].best_dqdly, dqdqs_perbit_dly[jj].best_dqsdly));   
#ifdef EYE_SCAN
	    EyeScanWin[jj].ucsetup_pass_number = ucsetup_pass_number;
	    EyeScanWin[jj].uchold_pass_number = uchold_pass_number;
#endif   

        #ifdef DDR_FT_LOAD_BOARD
            if (RXPERBIT_LOG_PRINT == 1) // ignore duty DramcClkDutyCal()
            {
                if ((ucsetup_pass_number+uchold_pass_number)<=RXWIN_CALIB_BOUND)
                {
                    LoadBoardShowResult(FLAG_RX_CALIBRATION, FLAG_WINDOW_TOO_SMALL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
                    while(1); 
                }
            }
        #endif
        }

        mcSHOW_DBG_MSG4(("----seperate line----\n"));
        mcFPRINTF((fp_A60808, "----seperate line----\n"));

        // we delay DQ or DQS to let DQS sample the middle of tx pass window for all the 8 bits,
        for (jj = ucbit_first; jj <= ucbit_last; jj++)
        {
            // set DQS to max for 8-bit
            if (dqdqs_perbit_dly[jj].best_dqsdly < ucmax_dqsdly_byte[ii])
            {
                // delay DQ to compensate extra DQS delay
                dqdqs_perbit_dly[jj].best_dqdly = dqdqs_perbit_dly[jj].best_dqdly + (ucmax_dqsdly_byte[ii] - dqdqs_perbit_dly[jj].best_dqsdly);
                // max limit to 15
                dqdqs_perbit_dly[jj].best_dqdly = ((dqdqs_perbit_dly[jj].best_dqdly > (MAX_RX_DQDLY_TAPS-1)) ? (MAX_RX_DQDLY_TAPS-1) : dqdqs_perbit_dly[jj].best_dqdly);
            }            
        }
    }
       
    mcSHOW_DBG_MSG4(("==================================================\n"));
    mcSHOW_DBG_MSG4(("    dramc_rxdqs_perbit_swcal\n"));
    mcSHOW_DBG_MSG4(("    channel=%d(0:cha, 1:chb) \n", p->channel));
    mcSHOW_DBG_MSG4(("    bus width=%d\n", p->data_width));
    mcSHOW_DBG_MSG4(("==================================================\n"));
    mcSHOW_DBG_MSG4(("DQS Delay :\n DQS0 = %d DQS1 = %d DQS2 = %d DQS3 = %d\n", ucmax_dqsdly_byte[0], ucmax_dqsdly_byte[1], ucmax_dqsdly_byte[2], ucmax_dqsdly_byte[3]));
    mcSHOW_DBG_MSG4(("DQ Delay :\n"));

    mcFPRINTF((fp_A60808, "==================================================\n"));
    mcFPRINTF((fp_A60808, "    dramc_rxdqs_perbit_swcal\n"));
    mcFPRINTF((fp_A60808, "    channel=%d(0:cha, 1:chb) \n", p->channel));
    mcFPRINTF((fp_A60808, "    bus width=%d\n", p->data_width));
    mcFPRINTF((fp_A60808, "==================================================\n"));
    mcFPRINTF((fp_A60808, "DQS Delay :\n DQS0 = %d DQS1 = %d DQS2 = %d DQS3 = %d\n", ucmax_dqsdly_byte[0], ucmax_dqsdly_byte[1], ucmax_dqsdly_byte[2], ucmax_dqsdly_byte[3]));
    mcFPRINTF((fp_A60808, "DQ Delay :\n"));
    
    for (ii = 0; ii < p->data_width; ii=ii+4)
    {
        mcSHOW_DBG_MSG4(("DQ%d = %d DQ%d = %d DQ%d = %d DQ%d = %d \n", ii, dqdqs_perbit_dly[ii].best_dqdly, ii+1, dqdqs_perbit_dly[ii+1].best_dqdly, ii+2, dqdqs_perbit_dly[ii+2].best_dqdly, ii+3, dqdqs_perbit_dly[ii+3].best_dqdly));
        mcFPRINTF((fp_A60808, "DQ%2d = %2d DQ%2d = %2d DQ%2d = %2d DQ%2d = %2d \n", ii, dqdqs_perbit_dly[ii].best_dqdly, ii+1, dqdqs_perbit_dly[ii+1].best_dqdly, ii+2, dqdqs_perbit_dly[ii+2].best_dqdly, ii+3, dqdqs_perbit_dly[ii+3].best_dqdly));
    }
    mcSHOW_DBG_MSG4(("________________________________________________________________________\n"));
    mcFPRINTF((fp_A60808, "________________________________________________________________________\n"));

    
    // set dqs delay
    u4value = 0;
    for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
    {
        u4value += (((U32)ucmax_dqsdly_byte[jj])<<(8*jj));        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DELDLY1), u4value);    
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), u4value);    // Edward : Set rank 1 also.

    
    // set dq delay
    for (jj=0; jj<p->data_width; jj=jj+4)
    {
        // 20130528, no need to swap, since DQ_DLY & CMP_ERR has swapped by HW
        #if 0
        if (p->en_4bitMux == ENABLE)
        {
            kk = ucswap_table[1][jj];
        }
        else
        {
            kk = ucswap_table[0][jj];
        }
        #else
            kk = jj;            
        #endif
        //every 4bit dq have the same delay register address
        u4value = ((U32) dqdqs_perbit_dly[kk].best_dqdly) + (((U32)dqdqs_perbit_dly[kk+1].best_dqdly)<<8) + (((U32)dqdqs_perbit_dly[kk+2].best_dqdly)<<16) + (((U32)dqdqs_perbit_dly[kk+3].best_dqdly)<<24);  
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQIDLY1+jj), u4value);            
    }              
    
    DramcPhyReset(p);		// Wrong duty may cause GW wrong. Reset here.

    // Log example
    /*
------------------------------------------------------
Start calculate dq time and dqs time /
Find max DQS delay per byte / Adjust DQ delay to align DQS...
------------------------------------------------------
bit# 0 : dq time=11 dqs time= 8
bit# 1 : dq time=11 dqs time= 8
bit# 2 : dq time=11 dqs time= 6
bit# 3 : dq time=10 dqs time= 8
bit# 4 : dq time=11 dqs time= 8
bit# 5 : dq time=10 dqs time= 8
bit# 6 : dq time=11 dqs time= 8
bit# 7 : dq time= 9 dqs time= 6
----seperate line----
bit# 8 : dq time=12 dqs time= 7
bit# 9 : dq time=10 dqs time= 8
bit#10 : dq time=11 dqs time= 8
bit#11 : dq time=10 dqs time= 8
bit#12 : dq time=11 dqs time= 8
bit#13 : dq time=11 dqs time= 8
bit#14 : dq time=11 dqs time= 8
bit#15 : dq time=12 dqs time= 8
----seperate line----
bit#16 : dq time=11 dqs time= 7
bit#17 : dq time=10 dqs time= 8
bit#18 : dq time=11 dqs time= 7
bit#19 : dq time=11 dqs time= 6
bit#20 : dq time=10 dqs time= 9
bit#21 : dq time=11 dqs time=10
bit#22 : dq time=11 dqs time=10
bit#23 : dq time= 9 dqs time= 9
----seperate line----
bit#24 : dq time=12 dqs time= 6
bit#25 : dq time=13 dqs time= 6
bit#26 : dq time=13 dqs time= 7
bit#27 : dq time=11 dqs time= 7
bit#28 : dq time=12 dqs time= 8
bit#29 : dq time=10 dqs time= 8
bit#30 : dq time=13 dqs time= 7
bit#31 : dq time=11 dqs time= 8
----seperate line----
==================================================
    dramc_rxdqs_perbit_swcal_v2
    channel=2(2:cha, 3:chb) apply = 1
==================================================
DQS Delay :
 DQS0 = 0 DQS1 = 0 DQS2 = 0 DQS3 = 0
DQ Delay :
DQ 0 =  1 DQ 1 =  1 DQ 2 =  2 DQ 3 =  1
DQ 4 =  1 DQ 5 =  1 DQ 6 =  1 DQ 7 =  1
DQ 8 =  2 DQ 9 =  1 DQ10 =  1 DQ11 =  1
DQ12 =  1 DQ13 =  1 DQ14 =  1 DQ15 =  2
DQ16 =  2 DQ17 =  1 DQ18 =  2 DQ19 =  2
DQ20 =  0 DQ21 =  0 DQ22 =  0 DQ23 =  0
DQ24 =  3 DQ25 =  3 DQ26 =  3 DQ27 =  2
DQ28 =  2 DQ29 =  1 DQ30 =  3 DQ31 =  1
_______________________________________________________________
   */
    
    if (fgfail == 1)
    {
        mcSHOW_DBG_MSG4(("RX DQ/DQS calibration fail!\n"));
        return DRAM_FAIL;
    }
    
    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }
}

#if 0
DRAM_STATUS_T DramcRxWindowPerbitCal_test(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0, fgfail, ii, jj, kk;
    U32 u4value, u4err_value, u4fail_bit, u4fail_bit_R, u4fail_bit_F;
    RXDQS_PERBIT_DLY_T dqdqs_perbit_dly[DQ_DATA_WIDTH];
    U8 ucbit_first, ucbit_last, uchold_pass_number, ucsetup_pass_number;
    U8 ucmax_dqsdly_byte[DQS_NUMBER];
    
    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }    
        
    // initialize parameters
    for (ii = 0; ii < p->data_width; ii++)
    {
        dqdqs_perbit_dly[ii].first_dqdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqdly_pass = -2;
        dqdqs_perbit_dly[ii].first_dqsdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqsdly_pass = -2;
    }

        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }
            else
            {
                u4err_value = 0;
                for (jj = 0; jj < 1; jj++)
                {
                    u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
                }
            }
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
            else
            {
                u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("Not support test pattern!! Use audio pattern by default.\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }        

        // check fail bit ,0 ok ,others fail
        for (jj = 0; jj < p->data_width; jj++)
        {
            u4fail_bit = u4err_value&((U32)1<<jj);

            if (u4fail_bit == 0)
            {
                if (dqdqs_perbit_dly[jj].first_dqdly_pass == -1)
                {
                    // first DQ pass delay tap
                    dqdqs_perbit_dly[jj].first_dqdly_pass = ii;
                }
                if (dqdqs_perbit_dly[jj].last_dqdly_pass == -2)
                {
                    if (ii == (MAX_RX_DQDLY_TAPS-1))
                    {
                        // pass to the last tap
                        dqdqs_perbit_dly[jj].last_dqdly_pass = ii;
                    }
                }
            }
            else
            {
                if ((dqdqs_perbit_dly[jj].first_dqdly_pass != -1)&&(dqdqs_perbit_dly[jj].last_dqdly_pass == -2))
                {
                    dqdqs_perbit_dly[jj].last_dqdly_pass = ii -1;
                }
            }

            if (u4fail_bit == 0)
            {
                mcSHOW_DBG_MSG2(("o"));
                mcFPRINTF((fp_A60808, "o"));
            }
            else
            {
                {
                    mcSHOW_DBG_MSG2(("x"));
                    mcFPRINTF((fp_A60808, "x"));
                }
            }
            
        }
        mcSHOW_DBG_MSG2(("\n"));
        mcFPRINTF((fp_A60808, "\n"));
    

    return DRAM_OK;
    
}
#endif

void DramcClkDutyCal(DRAMC_CTX_T *p)
{
    U16 max_win_size = 0;
    U8 ii, jj, max_duty_sel, max_duty, ucstatus = 0;
    U32 u4value;
    
    for (ii = 0; ii < 2; ii++)
    {        
        for (jj = 0; jj < 4; jj++)
        {
	    DramcEnterSelfRefresh(p, 1);    
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCLKDUTY), &u4value);
            if (ii == 0)
            {
                mcCLR_BIT(u4value, POS_PHYCLKDUTY_CMDCLKP0DUTYSEL);
                mcSET_FIELD(u4value, 0, MASK_PHYCLKDUTY_CMDCLKP0DUTYN, POS_PHYCLKDUTY_CMDCLKP0DUTYN);
                mcSET_FIELD(u4value, jj, MASK_PHYCLKDUTY_CMDCLKP0DUTYP, POS_PHYCLKDUTY_CMDCLKP0DUTYP);
            }
            else
            {
                mcSET_BIT(u4value, POS_PHYCLKDUTY_CMDCLKP0DUTYSEL);
                mcSET_FIELD(u4value, jj, MASK_PHYCLKDUTY_CMDCLKP0DUTYN, POS_PHYCLKDUTY_CMDCLKP0DUTYN);
                mcSET_FIELD(u4value, 0, MASK_PHYCLKDUTY_CMDCLKP0DUTYP, POS_PHYCLKDUTY_CMDCLKP0DUTYP);
            }            
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCLKDUTY), u4value);
#if	1
            mcDELAY_US(20);	// Wait PLL lock.
#else	            
   	    *(volatile unsigned int*)(0x10006000) = 0x0b160001; 
   	    *(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode 
   	    mcDELAY_US(2);
   	    *(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
   	    mcDELAY_US(13);
#endif            
	    DramcEnterSelfRefresh(p, 0);    
            
            RXPERBIT_LOG_PRINT = 0;
            DramcRxWindowPerbitCal(p);
            RXPERBIT_LOG_PRINT = 1;

            if (u2rx_window_sum > max_win_size)
            {
                max_win_size = u2rx_window_sum;
                max_duty_sel = ii;
                max_duty = jj;
            }

            mcSHOW_DBG_MSG2(("[Channel %d CLK DUTY CALIB] DUTY_SEL=%d, DUTY=%d --> rx window size=%d\n", p->channel, ii, jj, u2rx_window_sum));
            mcFPRINTF((fp_A60808, "[CLK DUTY CALIB] DUTY_SEL=%d, DUTY=%d --> rx window size=%d\n", ii, jj, u2rx_window_sum));
        }
    }

    // set optimal CLK duty settings
    DramcEnterSelfRefresh(p, 1);    
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCLKDUTY), &u4value);
    if (max_duty_sel == 0)
    {
        mcCLR_BIT(u4value, POS_PHYCLKDUTY_CMDCLKP0DUTYSEL);
        mcSET_FIELD(u4value, 0, MASK_PHYCLKDUTY_CMDCLKP0DUTYN, POS_PHYCLKDUTY_CMDCLKP0DUTYN);
        mcSET_FIELD(u4value, max_duty, MASK_PHYCLKDUTY_CMDCLKP0DUTYP, POS_PHYCLKDUTY_CMDCLKP0DUTYP);
    }
    else
    {
        mcSET_BIT(u4value, POS_PHYCLKDUTY_CMDCLKP0DUTYSEL);
        mcSET_FIELD(u4value, max_duty, MASK_PHYCLKDUTY_CMDCLKP0DUTYN, POS_PHYCLKDUTY_CMDCLKP0DUTYN);
        mcSET_FIELD(u4value, 0, MASK_PHYCLKDUTY_CMDCLKP0DUTYP, POS_PHYCLKDUTY_CMDCLKP0DUTYP);
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCLKDUTY), u4value);
#if	1
    mcDELAY_US(20);	// Wait PLL lock.
#else	            
    *(volatile unsigned int*)(0x10006000) = 0x0b160001; 
    *(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode 
    mcDELAY_US(2);
    *(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
    mcDELAY_US(13);
#endif
    DramcEnterSelfRefresh(p, 0);    

    mcSHOW_DBG_MSG(("[Channel %d CLK DUTY CALIB] Final DUTY_SEL=%d, DUTY=%d --> rx window size=%d, Reg.148h=%xh\n", p->channel, max_duty_sel, max_duty, max_win_size, u4value));
    mcFPRINTF((fp_A60808, "[CLK DUTY CALIB] Final DUTY_SEL=%d, DUTY=%d --> rx window size=%d\n", max_duty_sel, max_duty, max_win_size));
}
#if 1 //add by KT, define the function in ett_cust.dle.c
void dle_factor_handler(DRAMC_CTX_T *p, U8 curr_val) 
{
    U8 ucstatus = 0;
    U32 u4value;
    U32 u4curr_val_DSEL;

    /* DATLAT: DRAMC_DDR2CTL[4:6], 3 bits */
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), &u4value);
    mcSET_FIELD(u4value, (curr_val&0x7), MASK_DDR2CTL_DATLAT, POS_DDR2CTL_DTALAT);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), u4value);

    /* DATLAT3: DRAMC_PADCTL1[4], 1 bit */
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4value);
    mcSET_FIELD(u4value, ((curr_val>>3)&0x1), MASK_PADCTL4_DATLAT3, POS_PADCTL4_DATLAT3);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), u4value); 

    /* DATLAT4: 0xf0[25], 1 bit */
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
    mcSET_FIELD(u4value, ((curr_val>>4)&0x1), MASK_PHYCTL1_DATLAT4, POS_PHYCTL1_DATLAT4);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value); 

    // DATLAT_DSEL (Reg.80h[12:8]) = DATLAT - 2*3 - 2*1 (Speed >=1600)
    // DATLAT_DSEL = DATLAT - 2*1 - 2*1 (Speed <1600)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &u4value);
    if ((u4value & 0xe0)==0xe0)
    {
    	// 3 pipes
    	if (curr_val>=8)
    	{
    		u4curr_val_DSEL = curr_val -8;
    	}
    	else
    	{
    		u4curr_val_DSEL = 0;
    	}    	
    }
    else
    {
    	// should be 1 pipe.
    	if (curr_val>=4)
    	{
    		u4curr_val_DSEL = curr_val -4;
    	}
    	else
    	{
    		u4curr_val_DSEL = 0;
    	}    	
    }

    /*
    if (p->frequency < 1600)
    {
    	if (curr_val>=4)
    	{
    		u4curr_val_DSEL = curr_val -4;
    	}
    	else
    	{
    		u4curr_val_DSEL = 0;
    	}
    }
    else
    {
    	if (curr_val>=8)
    	{
    		u4curr_val_DSEL = curr_val -8;
    	}
    	else
    	{
    		u4curr_val_DSEL = 0;
    	}
    }
    */

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &u4value);
    mcSET_FIELD(u4value, u4curr_val_DSEL, 0x00001f00, 8);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x80), u4value); 
       

    // only for HW run time test engine use, optimize bandwidth

    if (curr_val >= 3)
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x080), &u4value);
        mcSET_FIELD(u4value, (curr_val-3)&0x1f, 0x0000001f, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x080), u4value);    
    }
    else
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x080), &u4value);
        mcSET_FIELD(u4value, 0, 0x0000001f, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x080), u4value);
    }

}

// The difference of tDQSCK (2.5~5.5ns) is 3ns which is larger than 1 M_CK (2.14ns in 1866Mbps).
// Should set to the maximum of dual rank. 
// If calibration could detect the end of window, set the center of overlapped window.
DRAM_STATUS_T DramcDualRankRxdatlatCal(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0, ucR0DLESetting;
	U32 u4value;

	// Rank 0 DLE calibration.
	CurrentRank = 0;
	DramcRxdatlatCal((DRAMC_CTX_T *) p);	
	ucR0DLESetting = ucDLESetting;

	// Rank 1 DLE calibration.
	// Swap CS0 and CS1.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &u4value);
	u4value = u4value |0x08;
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), u4value);

	// Set rank 1 coarse tune and fine tune back.
        dqsi_gw_dly_coarse_factor_handler(p, opt_gw_coarse_value_R1[p->channel]);
        dqsi_gw_dly_fine_factor_handler(p, opt_gw_fine_value_R1[p->channel]);        
        
	CurrentRank = 1;
	//DramcPhyReset(p);

	DramcRxdatlatCal((DRAMC_CTX_T *) p);	
	mcSHOW_DBG_MSG(("Rank 0 DLE calibrated setting = %xh. Rank 1 DLE calibrated setting = %xh\n", ucR0DLESetting, ucDLESetting));
	if (ucDLESetting < ucR0DLESetting)
	{
		mcSHOW_DBG_MSG(("Rank 0 %xh > Rank 1 %xh. Set to rank 0 %xh.\n", ucR0DLESetting, ucDLESetting, ucR0DLESetting));
		dle_factor_handler(p, ucR0DLESetting);
	}
	else
	{
		mcSHOW_DBG_MSG(("Rank 0 %xh < Rank 1 %xh. Use rank 1 %xh.\n", ucR0DLESetting, ucDLESetting, ucDLESetting));
	}
	
	// Set rank 0 coarse tune and fine tune back.
        dqsi_gw_dly_coarse_factor_handler(p, opt_gw_coarse_value_R0[p->channel]);
        dqsi_gw_dly_fine_factor_handler(p, opt_gw_fine_value_R0[p->channel]);        

	// Swap CS back.
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &u4value);
	u4value = u4value & (~0x08);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), u4value);
	
	CurrentRank = 0;
	//DramcPhyReset(p);
}
			
//-------------------------------------------------------------------------
/** DramcRxdatlatCal
 *  scan the pass range of DATLAT for DDRPHY read data window.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
DRAM_STATUS_T DramcRxdatlatCal(DRAMC_CTX_T *p)
{
//A60807
    U8 ucstatus = 0, ii, ucStartCalVal=0;
    U32 u4prv_register_07c, u4prv_register_0e4;
    U32 u4prv_register_0f0, u4prv_register_080, u4prv_register_0d8;

    U32 u4value, u4err_value;
    U8 ucfirst, ucbegin, ucsum, ucbest_step;    

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }
    mcSHOW_DBG_MSG(("\n==============================================================\n"));
    mcSHOW_DBG_MSG(("    [Channel %d] [Rank %d] DATLAT calibration \n", p->channel, CurrentRank));
    mcSHOW_DBG_MSG(("    channel=%d(0:cha) \n", p->channel));
    mcSHOW_DBG_MSG(("==============================================================\n"));

    mcFPRINTF((fp_A60808, "\n==============================================================\n"));
    mcFPRINTF((fp_A60808, "    DATLAT calibration \n"));
    mcFPRINTF((fp_A60808, "    channel=%d(0:cha) \n", p->channel));
    mcFPRINTF((fp_A60808, "==============================================================\n"));


    // [11:10] DQIENQKEND 01 -> 00 for DATLAT calibration issue, DQS input enable will refer to DATLAT
    // if need to enable this (for power saving), do it after all calibration done
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), &u4value);
    u4prv_register_0d8 = u4value;
    mcSET_FIELD(u4value, 0x00, MASK_MCKDLY_DQIENQKEND, POS_MCKDLY_DQIENQKEND);
    mcCLR_BIT(u4value, 4);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4value);    

    // pre-save
    // 0x07c[6:4]   DATLAT bit2-bit0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), &u4prv_register_07c);
    // 0x0e4[4]     DALLAT bit3
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4prv_register_0e4);
    // 0x0f0[25]    DATLAT bit 4
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4prv_register_0f0);
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &u4prv_register_080);

    // init best_step to default
    ucbest_step = (U8) (((u4prv_register_07c>>4)&0x7) | ((u4prv_register_0e4>>1)&0x8) | ((u4prv_register_0f0>>21)&0x10));

    mcSHOW_DBG_MSG(("DATLAT Default value = 0x%x\n", ucbest_step));
    mcFPRINTF((fp_A60808, "DATLAT Default value = 0x%x\n", ucbest_step));

    // 1.set DATLAT 0-15 (0-21 for MT6595)
    // 2.enable engine1 or engine2 
    // 3.check result  ,3~4 taps pass 
    // 4.set DATLAT 2nd value for optimal

    // Initialize
    ucfirst = 0xff;
    ucbegin = 0;
    ucsum = 0;        

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &u4value);

    if ((u4value & 0xe0)==0xe0)
    {
    	// 3 pipes
   	ucStartCalVal = 8;
    }
    else
    {
    	// Should be 1 pipe.
   	ucStartCalVal = 4;
    }
/*    
   if (p->frequency < 1600)
   {
   	ucStartCalVal = 4;
   }
   else
   {
   	ucStartCalVal = 8;
   }
*/   
        
    for (ii = ucStartCalVal; ii < DATLAT_TAP_NUMBER; ii++)
    // for testing
    //for (ii = 8; ii < DATLAT_TAP_NUMBER; ii++)
    {        
        // 1
        dle_factor_handler(p, ii);

        // 2
    #if 1
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }
            else
            {
                u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);                      
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            if (p->fglow_freq_write_en == ENABLE)
            {
                u4err_value = DramcEngine2(p, TE_OP_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }
            else
            {
                u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else if (p->test_pattern == TEST_TA1_SIMPLE)
        {
            u4err_value = DramcEngine1(p, 0x55000000, 0xaa0007ff, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("Not support test pattern!! Use audio pattern by default.\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }
    #else    // fix TA1 to do DATLAT
        u4err_value = DramcEngine1(p, p->test2_1, p->test2_2, 0, 0);
    #endif
        
        if (u4err_value == 0)
        {
            if (ucbegin == 0)
            {
                // first tap which is pass
                ucfirst = ii;
                ucbegin = 1;
            }
            if (ucbegin == 1)
            {
                ucsum++;
            }
        }
        else
        {
            if (ucbegin == 1)
            {
                // pass range end
                ucbegin = 0xff;
            }
        }

	DramcPhyReset(p);		// Wrong duty may cause GW wrong. Reset here.

        mcSHOW_DBG_MSG(("TAP=%d, err_value=0x%x, begin=%d, first=%d, sum=%d\n", ii, u4err_value, ucbegin, ucfirst, ucsum));
        mcFPRINTF((fp_A60808, "TAP=%d, err_value=0x%x, begin=%d, first=%d, sum=%d\n", ii, u4err_value, ucbegin, ucfirst, ucsum));
    }

    // 4
    if (ucsum == 0)
    {
        mcSHOW_ERR_MSG(("no DATLAT taps pass, DATLAT calibration fail!!\n"));
    } 
    else if (ucsum == 1)
    {
        ucbest_step = ucfirst;
        mcSHOW_ERR_MSG(("only one pass tap!!\n"));
    }
    else
    {
        ucbest_step = ucfirst + 1;
    }    

   #ifdef FIX_DLE_19
   ucbest_step = 19;
   #endif
    mcSHOW_DBG_MSG(("pattern=%d(0: ISI, 1: AUDIO, 2: TA4, 3: TA4-3) first_step=%d total pass=%d best_step=%d\n", p->test_pattern, ucfirst, ucsum, ucbest_step));
    mcFPRINTF((fp_A60808, "pattern=%d(0: ISI, 1: AUDIO, 2: TA4, 3: TA4-3) first_step=%d total pass=%d best_step=%d\n", p->test_pattern, ucfirst, ucsum, ucbest_step));
        
    if (ucsum == 0)
    {
        mcSHOW_ERR_MSG(("DATLAT calibration fail, write back to default values!\n"));
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), u4prv_register_07c);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), u4prv_register_0e4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4prv_register_0f0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x80), u4prv_register_080);
    }
    else
    {
        dle_factor_handler(p, ucbest_step);
    }

    ucDLESetting = ucbest_step;
    // [11:10] DQIENQKEND 01 -> 00 for DATLAT calibration issue, DQS input enable will refer to DATLAT
    // if need to enable this (for power saving), do it after all calibration done    

    mcSET_FIELD(u4prv_register_0d8, 0x01, MASK_MCKDLY_DQIENQKEND, POS_MCKDLY_DQIENQKEND);
    mcSET_BIT(u4prv_register_0d8, 4);   	// If init value is 0 and DE ask to set to 1, this line should be executed.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_MCKDLY), u4prv_register_0d8);      

#ifdef DDR_FT_LOAD_BOARD
    if (ucsum == 0)
    {
        LoadBoardShowResult(FLAG_DATLAT_CALIBRATION, FLAG_CALIBRATION_FAIL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);
    }
    else if (ucsum <= DATLAT_CALIB_BOUND)
    {
        LoadBoardShowResult(FLAG_DATLAT_CALIBRATION, FLAG_WINDOW_TOO_SMALL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
        while(1);
    }
#endif

    if (ucstatus)
    {
        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }
}
#endif

//-------------------------------------------------------------------------
/** DramcTxWindowPerbitCal (v2)
 *  TX DQS per bit SW calibration.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  apply           (U8): 0 don't apply the register we set  1 apply the register we set ,default don't apply.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL 
 */
//-------------------------------------------------------------------------
DRAM_STATUS_T DramcTxWindowPerbitCal(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0, ii, jj, uc_dly;
    U32 u4value, u4err_value, u4fail_bit;

    TXDQS_PERBIT_DLY_T dqdqs_perbit_dly[DQ_DATA_WIDTH];
    U8 ucbit_first, ucbit_last, uchold_pass_number, ucsetup_pass_number;
    U8 ucmax_dqsdly_byte[DQS_NUMBER], ucave_dqdly_byte[DQS_NUMBER];
    U8 ucfail = 0;

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

    DramcPhyReset(p);

    // for debug
    mcSHOW_DBG_MSG3(("[Channel %d] TX DQS per bit para : test pattern=%d, test2_1=0x%x, test2_2=0x%x\n", p->channel, p->test_pattern, p->test2_1, p->test2_2));    

    // delay DQS from first_dqs_dly to 15 to get the hold time
    // set DQ delay first
    uc_dly = FIRST_TX_DQ_DELAY;
    u4value = 0;
    for (ii = 0; ii < DQS_BIT_NUMBER; ii++)
    {
        u4value += (((U32) uc_dly) << (4*ii));
    }

    for (ii = 0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*ii), u4value);
    }

    // set DQM delay
    if (p->data_width == DATA_WIDTH_16BIT)
    {
        // for DQC, DQM3 is CS#
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4value);
        mcSET_FIELD(u4value, 0, 0x000000ff, 0);
    }
    else
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4value);
        u4value &= 0xffff0000;
    }
    for (ii = 0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        u4value += (((U32) uc_dly) << (4*ii));
    }    
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), u4value);
        
    // initialize parameters
    for (ii = 0; ii < p->data_width; ii++)
    {
        dqdqs_perbit_dly[ii].first_dqdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqdly_pass = -2;
        dqdqs_perbit_dly[ii].first_dqsdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqsdly_pass = -2;
        dqdqs_perbit_dly[ii].best_first_dqdly_pass = -1;
        dqdqs_perbit_dly[ii].best_last_dqdly_pass = -2;
        dqdqs_perbit_dly[ii].best_first_dqsdly_pass = -1;
        dqdqs_perbit_dly[ii].best_last_dqsdly_pass = -2;                
    }

    // for debug
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("Start DQS delay to find pass range, DQ delay fixed to 0x%x...\n", u4value));
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("x-axis is bit #; y-axis is DQS delay (%d~%d)\n", FIRST_TX_DQS_DELAY+1, MAX_TX_DQSDLY_TAPS-1));    

    mcFPRINTF((fp_A60808, "Start TX DQ/DQS calibration...\n"));
    mcFPRINTF((fp_A60808, "x-axis is bit #; y-axis is DQS delay (%d~%d)\n", FIRST_TX_DQS_DELAY+1, MAX_TX_DQSDLY_TAPS-1));    
    
    // because the tap DQdly=0 DQSdly=0 will be counted when we delay dq ,so we don't count it here
    // so we set first dqs delay to 1
    for (ii = (FIRST_TX_DQS_DELAY+1); ii < MAX_TX_DQSDLY_TAPS; ii++)
    {    
        uc_dly = ii;    
        u4value = 0;
        for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
        {
            u4value += (((U32)uc_dly)<<(4*jj));        
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
        
        // for debug
        //mcSHOW_DBG_MSG3(("DQS dly:%d, DQS delay(swap):%d, Reg:0x%x\n", ii, uc_dly, u4value));

#ifdef XTALK_SSO_K
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~0x01);	// disable dual channel.
	if (p->channel == CHANNEL_A)
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~(0x01<<18));
	}
	else
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= ((0x01<<18));
	}
        u4err_value = DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, DDR_BASE, 
        	DDR_BASE+0x100000, XTALK_SSO_LEN<<3, 8, 1, 1);
        
	#ifdef SINGLE_CHANNEL_ENABLE        
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) = LPDDR3_EMI_CONA;
	#else
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= (0x01);	// enable dual channel.
        #endif
#else
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("Not support test pattern!! Use audio pattern by default.\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }
#endif

        // check fail bit ,0 ok ,others fail
        for (jj = 0; jj < p->data_width; jj++)
        {
            u4fail_bit = u4err_value&((U32)1<<jj);

            if (u4fail_bit == 0)
            {
                if (dqdqs_perbit_dly[jj].first_dqsdly_pass == -1)
                {
                    // first DQS pass delay tap
                    dqdqs_perbit_dly[jj].first_dqsdly_pass = ii;
                }
                if (dqdqs_perbit_dly[jj].last_dqsdly_pass == -2)
                {
                    if (ii == (MAX_TX_DQSDLY_TAPS-1))
                    {
                        // pass to the last tap
                        dqdqs_perbit_dly[jj].last_dqsdly_pass = ii;
                        if ( (dqdqs_perbit_dly[jj].last_dqsdly_pass-dqdqs_perbit_dly[jj].first_dqsdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqsdly_pass-dqdqs_perbit_dly[jj].best_first_dqsdly_pass))
                        {
    			     dqdqs_perbit_dly[jj].best_last_dqsdly_pass =  dqdqs_perbit_dly[jj].last_dqsdly_pass;
    			     dqdqs_perbit_dly[jj].best_first_dqsdly_pass = dqdqs_perbit_dly[jj].first_dqsdly_pass;
                        }	  
                        // clear to find the next pass range if it has
                        dqdqs_perbit_dly[jj].first_dqsdly_pass = -1;
                        dqdqs_perbit_dly[jj].last_dqsdly_pass = -2;                                
                    }
                }
            }
            else
            {
                if ((dqdqs_perbit_dly[jj].first_dqsdly_pass != -1)&&(dqdqs_perbit_dly[jj].last_dqsdly_pass == -2))
                {
                    dqdqs_perbit_dly[jj].last_dqsdly_pass = ii -1;
                    if ( (dqdqs_perbit_dly[jj].last_dqsdly_pass-dqdqs_perbit_dly[jj].first_dqsdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqsdly_pass-dqdqs_perbit_dly[jj].best_first_dqsdly_pass))
                    {
			     dqdqs_perbit_dly[jj].best_last_dqsdly_pass =  dqdqs_perbit_dly[jj].last_dqsdly_pass;
			     dqdqs_perbit_dly[jj].best_first_dqsdly_pass = dqdqs_perbit_dly[jj].first_dqsdly_pass;
                    }	  
                    // clear to find the next pass range if it has
                    dqdqs_perbit_dly[jj].first_dqsdly_pass = -1;
                    dqdqs_perbit_dly[jj].last_dqsdly_pass = -2;                                   
                }
            }

            if (u4fail_bit == 0)
            {
                mcSHOW_DBG_MSG3(("o"));
                mcFPRINTF((fp_A60808, "o"));
            }
            else
            {
                {
                    mcSHOW_DBG_MSG3(("x"));
                    mcFPRINTF((fp_A60808, "x"));
                }
            }            
        }
        mcSHOW_DBG_MSG3(("\n"));
        mcFPRINTF((fp_A60808, "\n"));
    }

    // set first DQS delay
    uc_dly = FIRST_TX_DQS_DELAY;
    u4value = 0;
    for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
    {
        u4value += (((U32)uc_dly)<<(4*jj));        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
    
    // for debug
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("Start DQ delay to find pass range, DQS delay fixed to 0x%x...\n", u4value));
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("x-axis is bit #; y-axis is DQ delay (%d~%d)\n", FIRST_TX_DQ_DELAY, MAX_TX_DQDLY_TAPS-1));

    mcFPRINTF((fp_A60808, "-------------------------\n"));
    mcFPRINTF((fp_A60808, "x-axis is bit #; y-axis is DQ delay (%d~%d)\n", FIRST_TX_DQ_DELAY, MAX_TX_DQDLY_TAPS-1));
    
    // delay DQ from first_dq_dly to 15 to get the setup time
    for (ii = FIRST_TX_DQ_DELAY; ii < MAX_TX_DQDLY_TAPS; ii++)
    {    
        uc_dly = ii;    
        // set DQ delay
        u4value = 0;
        for (jj = 0; jj < DQS_BIT_NUMBER; jj++)
        {
            u4value += (((U32) uc_dly) << (4*jj));
        }
        for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
        {
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*jj), u4value);
        }
        
        // set DQM delay        
        if (p->data_width == DATA_WIDTH_16BIT)
        {
            // for DQC, DQM3 is CS#
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4value);
            mcSET_FIELD(u4value, 0, 0x000000ff, 0);
        }
        else
        {
  	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4value);
            u4value &= 0xffff0000;
        }
        for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
        {
            u4value += (((U32) uc_dly) << (4*jj));
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), u4value);

        // for debug
        //mcSHOW_DBG_MSG3(("DQ delay:%d, DQ delay (swap):%d, Register:0x%x\n", ii, uc_dly, u4value));

#ifdef XTALK_SSO_K
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~0x01);	// disable dual channel.
	if (p->channel == CHANNEL_A)
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~(0x01<<18));
	}
	else
	{
		*(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= ((0x01<<18));
	}
        u4err_value = DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, DDR_BASE, 
        	DDR_BASE+0x100000, XTALK_SSO_LEN<<3, 8, 1, 1);
        
	#ifdef SINGLE_CHANNEL_ENABLE        
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) = LPDDR3_EMI_CONA;
	#else
        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= (0x01);	// enable dual channel.
        #endif
#else
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("Not support test pattern!! Use audio pattern by default.\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }
#endif

        // check fail bit ,0 ok ,others fail
        for (jj = 0; jj < p->data_width; jj++)
        {
            u4fail_bit = u4err_value&((U32)1<<jj);

            if (u4fail_bit == 0)
            {
                if (dqdqs_perbit_dly[jj].first_dqdly_pass == -1)
                {
                    // first DQS pass delay tap
                    dqdqs_perbit_dly[jj].first_dqdly_pass = ii;
                }
                if (dqdqs_perbit_dly[jj].last_dqdly_pass == -2)
                {
                    if (ii == (MAX_TX_DQDLY_TAPS-1))
                    {
                        // pass to the last tap
                        dqdqs_perbit_dly[jj].last_dqdly_pass = ii;
                        if ( (dqdqs_perbit_dly[jj].last_dqdly_pass-dqdqs_perbit_dly[jj].first_dqdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqdly_pass-dqdqs_perbit_dly[jj].best_first_dqdly_pass))
                        {
    			     dqdqs_perbit_dly[jj].best_last_dqdly_pass =  dqdqs_perbit_dly[jj].last_dqdly_pass;
    			     dqdqs_perbit_dly[jj].best_first_dqdly_pass = dqdqs_perbit_dly[jj].first_dqdly_pass;
                        }	  
                        // clear to find the next pass range if it has
                        dqdqs_perbit_dly[jj].first_dqdly_pass = -1;
                        dqdqs_perbit_dly[jj].last_dqdly_pass = -2;                           
                    }
                }
            }
            else
            {
                if ((dqdqs_perbit_dly[jj].first_dqdly_pass != -1)&&(dqdqs_perbit_dly[jj].last_dqdly_pass == -2))
                {
                    dqdqs_perbit_dly[jj].last_dqdly_pass = ii -1;
                    if ( (dqdqs_perbit_dly[jj].last_dqdly_pass-dqdqs_perbit_dly[jj].first_dqdly_pass) > (dqdqs_perbit_dly[jj].best_last_dqdly_pass-dqdqs_perbit_dly[jj].best_first_dqdly_pass))
                    {
			     dqdqs_perbit_dly[jj].best_last_dqdly_pass =  dqdqs_perbit_dly[jj].last_dqdly_pass;
			     dqdqs_perbit_dly[jj].best_first_dqdly_pass = dqdqs_perbit_dly[jj].first_dqdly_pass;
                    }	  
                    // clear to find the next pass range if it has
                    dqdqs_perbit_dly[jj].first_dqdly_pass = -1;
                    dqdqs_perbit_dly[jj].last_dqdly_pass = -2;                    
                }
            }

            if (u4fail_bit == 0)
            {
                mcSHOW_DBG_MSG3(("o"));
                mcFPRINTF((fp_A60808, "o"));
            }
            else
            {
                {
                    mcSHOW_DBG_MSG3(("x"));
                    mcFPRINTF((fp_A60808, "x"));
                }
            }            
        }
        mcSHOW_DBG_MSG3(("\n"));
        mcFPRINTF((fp_A60808, "\n"));
    }

    // for debug
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 
    mcSHOW_DBG_MSG3(("Start calculate setup time and hold time / \n"));
    mcSHOW_DBG_MSG3(("Find max DQS delay per byte / Adjust DQ delay to align DQS...\n"));
    mcSHOW_DBG_MSG3(("------------------------------------------------------\n")); 

    mcFPRINTF((fp_A60808, "------------------------------------------------------\n")); 
    mcFPRINTF((fp_A60808, "Start calculate setup time and hold time / \n"));
    mcFPRINTF((fp_A60808, "Find max DQS delay per byte / Adjust DQ delay to align DQS...\n"));
    mcFPRINTF((fp_A60808, "------------------------------------------------------\n")); 
    
    //As per byte, check max DQS delay in 8-bit. Except for the bit of max DQS delay, delay DQ to fulfill setup time = hold time
    for (ii = 0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        ucbit_first = DQS_BIT_NUMBER*ii;
        ucbit_last = DQS_BIT_NUMBER*ii+DQS_BIT_NUMBER-1;
        ucmax_dqsdly_byte[ii] = 0;
        // init accumulation variable to 0
        ucave_dqdly_byte[ii] = 0;    // for DQM
        
        for (jj = ucbit_first; jj <= ucbit_last; jj++)
        {
            // hold time = DQS pass taps
            uchold_pass_number = dqdqs_perbit_dly[jj].best_last_dqsdly_pass - dqdqs_perbit_dly[jj].best_first_dqsdly_pass + 1;
            // setup time = DQ pass taps
            ucsetup_pass_number = dqdqs_perbit_dly[jj].best_last_dqdly_pass - dqdqs_perbit_dly[jj].best_first_dqdly_pass + 1;

            if (uchold_pass_number > ucsetup_pass_number)
            {
                if (ucsetup_pass_number != 0)
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxoooooooo|ooooooooooooooooooooxxxxx
                    dqdqs_perbit_dly[jj].best_dqdly = 0;
                    dqdqs_perbit_dly[jj].best_dqsdly = (uchold_pass_number - ucsetup_pass_number) / 2;

                    if (dqdqs_perbit_dly[jj].best_dqsdly > ucmax_dqsdly_byte[ii])
                    {
                        ucmax_dqsdly_byte[ii] = dqdqs_perbit_dly[jj].best_dqsdly;
                    }
                }
                else
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxxxxxxxxx|xxxooooooooooxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqdly = 0;
                    dqdqs_perbit_dly[jj].best_dqsdly = (uchold_pass_number - ucsetup_pass_number) / 2 + dqdqs_perbit_dly[jj].best_first_dqsdly_pass;

                    if (dqdqs_perbit_dly[jj].best_dqsdly > ucmax_dqsdly_byte[ii])
                    {
                        ucmax_dqsdly_byte[ii] = dqdqs_perbit_dly[jj].best_dqsdly;
                    }
                }
            }
            else if (uchold_pass_number < ucsetup_pass_number)
            {
                if (uchold_pass_number != 0)
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxoooooooooooooooooo|ooooooooxxxxxxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = (ucsetup_pass_number - uchold_pass_number) / 2;                    
                }
                else
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxoooooooooooooooxxx|xxxxxxxxxxxxxxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = (ucsetup_pass_number - uchold_pass_number) / 2 + dqdqs_perbit_dly[jj].best_first_dqdly_pass;                    
                }
            }
            else   // hold time == setup time
            {
                if (uchold_pass_number != 0)
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxxxoooooo|ooooooxxxxxxxxxxxxxxxxxxx
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = 0;    
                }
                else
                {
                    // like this:
                    // setup time(dq delay)     hold time(dqs delay)
                    // 15                  0 1                       15 tap
                    // xxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxxxxxxxxxx
                    // mean this bit is error
                    mcSHOW_DBG_MSG4(("error on bit %d ,setup_time =hold_time =0!!\n", jj));
                    dqdqs_perbit_dly[jj].best_dqsdly = 0;
                    dqdqs_perbit_dly[jj].best_dqdly = 0;
                    ucfail = 1;
                #ifdef DDR_FT_LOAD_BOARD
                    LoadBoardShowResult(FLAG_TX_CALIBRATION, FLAG_CALIBRATION_FAIL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
                    while(1); 
                #endif
                }
            }            
            mcSHOW_DBG_MSG4(("bit#%d : setup time=%d hold time=%d win=%d, center (DQ,DQS)=(%d, %d)\n", jj, ucsetup_pass_number, uchold_pass_number, ucsetup_pass_number+uchold_pass_number, dqdqs_perbit_dly[jj].best_dqdly, dqdqs_perbit_dly[jj].best_dqsdly));
            mcFPRINTF((fp_A60808, "bit#%2d : setup time=%2d hold time=%2d win=%2d, center (DQ,DQS)=(%2d, %2d)\n", jj, ucsetup_pass_number, uchold_pass_number, ucsetup_pass_number+uchold_pass_number, dqdqs_perbit_dly[jj].best_dqdly, dqdqs_perbit_dly[jj].best_dqsdly));
#ifdef EYE_SCAN
	    EyeScanWin[jj].ucsetup_pass_number = ucsetup_pass_number;
	    EyeScanWin[jj].uchold_pass_number = uchold_pass_number;
#endif            

        #ifdef DDR_FT_LOAD_BOARD
            if ((ucsetup_pass_number+uchold_pass_number)<=TXWIN_CALIB_BOUND)
            {
                LoadBoardShowResult(FLAG_TX_CALIBRATION, FLAG_WINDOW_TOO_SMALL, p->channel, FLAG_NOT_COMPLETE_OR_FAIL);            
                while(1); 
            }
        #endif
        }

        // we delay DQ or DQS to let DQS sample the middle of tx pass window for all the 8 bits,
        for (jj = ucbit_first; jj <= ucbit_last; jj++)
        {
            // set DQS to max for 8-bit
            if (dqdqs_perbit_dly[jj].best_dqsdly < ucmax_dqsdly_byte[ii])
            {
                // delay DQ to compensate extra DQS delay
                dqdqs_perbit_dly[jj].best_dqdly = dqdqs_perbit_dly[jj].best_dqdly + (ucmax_dqsdly_byte[ii] - dqdqs_perbit_dly[jj].best_dqsdly);
                // max limit to 15
                dqdqs_perbit_dly[jj].best_dqdly = ((dqdqs_perbit_dly[jj].best_dqdly > (MAX_TX_DQDLY_TAPS-1)) ? (MAX_TX_DQDLY_TAPS-1) : dqdqs_perbit_dly[jj].best_dqdly);
            }
            ucave_dqdly_byte[ii] += dqdqs_perbit_dly[jj].best_dqdly;            
        }
        // take the average of DQ for DQM
        ucave_dqdly_byte[ii] = ucave_dqdly_byte[ii] / DQS_BIT_NUMBER;
    }
       
   mcSHOW_DBG_MSG4(("==================================================\n"));
   mcSHOW_DBG_MSG4(("        dramc_txdqs_perbit_swcal\n"));
   mcSHOW_DBG_MSG4(("           channel=%d(0:cha, 1:chb) \n", p->channel));
   mcSHOW_DBG_MSG4(("           bus width=%d\n", p->data_width));
   mcSHOW_DBG_MSG4(("==================================================\n"));
   mcSHOW_DBG_MSG4(("DQS Delay :\n DQS0 = %d DQS1 = %d DQS2 = %d DQS3 = %d\n", ucmax_dqsdly_byte[0], ucmax_dqsdly_byte[1], ucmax_dqsdly_byte[2], ucmax_dqsdly_byte[3]));
   mcSHOW_DBG_MSG4(("DQM Delay :\n DQM0 = %d DQM1 = %d DQM2 = %d DQM3 = %d\n", ucave_dqdly_byte[0], ucave_dqdly_byte[1], ucave_dqdly_byte[2], ucave_dqdly_byte[3]));
   mcSHOW_DBG_MSG4(("DQ Delay :\n"));

    mcFPRINTF((fp_A60808, "==================================================\n"));
    mcFPRINTF((fp_A60808, "        dramc_txdqs_perbit_swcal\n"));
    mcFPRINTF((fp_A60808, "           channel=%d(0:cha, 1:chb) \n", p->channel));
    mcFPRINTF((fp_A60808, "           bus width=%d\n", p->data_width));
    mcFPRINTF((fp_A60808, "==================================================\n"));
    mcFPRINTF((fp_A60808, "DQS Delay :\n DQS0 = %d DQS1 = %d DQS2 = %d DQS3 = %d\n", ucmax_dqsdly_byte[0], ucmax_dqsdly_byte[1], ucmax_dqsdly_byte[2], ucmax_dqsdly_byte[3]));
    mcFPRINTF((fp_A60808, "DQM Delay :\n DQM0 = %d DQM1 = %d DQM2 = %d DQM3 = %d\n", ucave_dqdly_byte[0], ucave_dqdly_byte[1], ucave_dqdly_byte[2], ucave_dqdly_byte[3]));
    mcFPRINTF((fp_A60808, "DQ Delay :\n"));
    
    for (ii = 0; ii < p->data_width; ii=ii+4)
    {
       mcSHOW_DBG_MSG4(("DQ%d = %d DQ%d = %d DQ%d = %d DQ%d = %d \n", ii, dqdqs_perbit_dly[ii].best_dqdly, ii+1, dqdqs_perbit_dly[ii+1].best_dqdly, ii+2, dqdqs_perbit_dly[ii+2].best_dqdly, ii+3, dqdqs_perbit_dly[ii+3].best_dqdly));
        mcFPRINTF((fp_A60808, "DQ%2d = %2d DQ%2d = %2d DQ%2d = %2d DQ%2d = %2d \n", ii, dqdqs_perbit_dly[ii].best_dqdly, ii+1, dqdqs_perbit_dly[ii+1].best_dqdly, ii+2, dqdqs_perbit_dly[ii+2].best_dqdly, ii+3, dqdqs_perbit_dly[ii+3].best_dqdly));
    }
   mcSHOW_DBG_MSG4(("________________________________________________________________________\n")); 
    mcFPRINTF((fp_A60808, "________________________________________________________________________\n"));    

    // Add CLK to DQS/DQ skew after write leveling
    if (fgwrlevel_done[p->channel])
    {
       mcSHOW_DBG_MSG4(("Add CLK to DQS/DQ skew based on write leveling.\n"));
        mcFPRINTF((fp_A60808, "Add CLK to DQS/DQ skew based on write leveling.\n"));
        
        for (ii = 0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
        {
           mcSHOW_DBG_MSG4(("DQS%d: %d  ", ii, wrlevel_dqs_final_delay[p->channel][ii]));
            mcFPRINTF((fp_A60808, "DQS%d: %d  ", ii, wrlevel_dqs_final_delay[p->channel][ii]));

#ifdef NEW_TX_DELAY_For_WL
	    if (ucmax_dqsdly_byte[ii] <= wrlevel_dqs_final_delay[p->channel][ii])
	    {
		// DQ
		ucbit_first = DQS_BIT_NUMBER*ii;
		ucbit_last = DQS_BIT_NUMBER*ii+DQS_BIT_NUMBER-1;
		for (jj = ucbit_first; jj <= ucbit_last; jj++)
		{
		    dqdqs_perbit_dly[jj].best_dqdly += (wrlevel_dqs_final_delay[p->channel][ii] - ucmax_dqsdly_byte[ii]);
		    // max limit to 15
		    dqdqs_perbit_dly[jj].best_dqdly = ((dqdqs_perbit_dly[jj].best_dqdly > (MAX_TX_DQDLY_TAPS-1)) ? (MAX_TX_DQDLY_TAPS-1) : dqdqs_perbit_dly[jj].best_dqdly);
		}	    	
		// DQM
		ucave_dqdly_byte[ii] += (wrlevel_dqs_final_delay[p->channel][ii] - ucmax_dqsdly_byte[ii]);
		// max limit to 15
		ucave_dqdly_byte[ii] = ((ucave_dqdly_byte[ii] > (MAX_TX_DQDLY_TAPS-1)) ? (MAX_TX_DQDLY_TAPS-1) : ucave_dqdly_byte[ii]);
		
		// DQS
		ucmax_dqsdly_byte[ii] = wrlevel_dqs_final_delay[p->channel][ii];		
	    }
	    else
	    {
	    	// ucmax_dqsdly_byte[ii] > wrlevel_dqs_final_delay[p->channel][ii])
	    	// Originally should move clk delay and CA delay accordingly. Then GW calibration again. Too complicated.
	    	// DQ/DQS skew should not be large according to DE. So sacrifice the Clk/DQS margin by keeping the clk out delay.
		mcSHOW_DBG_MSG4(("[Warning] DQSO %d in TX per-bit = %d > DQSO %d in WL = %d  ", 
			ii, ucmax_dqsdly_byte[ii], ii, wrlevel_dqs_final_delay[p->channel][ii]));
	    }

#else
            // DQS
            ucmax_dqsdly_byte[ii] += wrlevel_dqs_final_delay[p->channel][ii];
            // max limit to 15
            ucmax_dqsdly_byte[ii] = ((ucmax_dqsdly_byte[ii] > (MAX_TX_DQSDLY_TAPS-1)) ? (MAX_TX_DQSDLY_TAPS-1) : ucmax_dqsdly_byte[ii]);

            // DQM
            ucave_dqdly_byte[ii] += wrlevel_dqs_final_delay[p->channel][ii];
            // max limit to 15
            ucave_dqdly_byte[ii] = ((ucave_dqdly_byte[ii] > (MAX_TX_DQDLY_TAPS-1)) ? (MAX_TX_DQDLY_TAPS-1) : ucave_dqdly_byte[ii]);

            // DQ
            ucbit_first = DQS_BIT_NUMBER*ii;
            ucbit_last = DQS_BIT_NUMBER*ii+DQS_BIT_NUMBER-1;
            for (jj = ucbit_first; jj <= ucbit_last; jj++)
            {
                dqdqs_perbit_dly[jj].best_dqdly += wrlevel_dqs_final_delay[p->channel][ii];
                // max limit to 15
                dqdqs_perbit_dly[jj].best_dqdly = ((dqdqs_perbit_dly[jj].best_dqdly > (MAX_TX_DQDLY_TAPS-1)) ? (MAX_TX_DQDLY_TAPS-1) : dqdqs_perbit_dly[jj].best_dqdly);
            }
#endif            
        }
       mcSHOW_DBG_MSG4(("\n"));
        mcFPRINTF((fp_A60808, "\n"));
    }
   
    // Set best delay to registers
    // 0x014, DQS delay, each DQS has 4 bits. DQS0 [3:0], DQS1 [7:4], DQS2 [11:8], DQS3 [15:12]
    u4value = 0;
    for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
    {
        u4value += (((U32)ucmax_dqsdly_byte[jj])<<(4*jj));        
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL3), u4value);
   mcSHOW_DBG_MSG4(("TX DQS register 0x%x=0x%x\n", DRAMC_REG_PADCTL3, u4value));
    mcFPRINTF((fp_A60808, "TX DQS register 0x%x=0x%x\n", DRAMC_REG_PADCTL3, u4value));

    // 0x200~0x20c, DQ delay, each DQ has 4 bits. Each register contains 8-bit DQ's
    for (ii = 0; ii < (p->data_width/DQS_BIT_NUMBER); ii++)
    {
        u4value = 0;
        ucbit_first = DQS_BIT_NUMBER*ii;
        ucbit_last = DQS_BIT_NUMBER*ii+DQS_BIT_NUMBER-1;
        for (jj = ucbit_first; jj <= ucbit_last; jj++)
        {
            u4value += (((U32) (dqdqs_perbit_dly[jj].best_dqdly)) << (4*(jj-ucbit_first)));
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQODLY1+4*ii), u4value);
       mcSHOW_DBG_MSG4(("TX DQ register 0x%x=0x%x\n", DRAMC_REG_DQODLY1+4*ii, u4value));
        mcFPRINTF((fp_A60808, "TX DQ register 0x%2x=0x%8x\n", DRAMC_REG_DQODLY1+4*ii, u4value));
    } 
    // set DQM delay
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), &u4value);
        u4value &= 0xffff0000;
    }
    for (jj = 0; jj < (p->data_width/DQS_BIT_NUMBER); jj++)
    {
        u4value += (((U32) ucave_dqdly_byte[jj]) << (4*jj));
    }
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL2), u4value);
   mcSHOW_DBG_MSG4(("TX DQM register 0x%x=0x%x\n", DRAMC_REG_PADCTL2, u4value));
    mcFPRINTF((fp_A60808, "TX DQM register 0x%2x=0x%8x\n", DRAMC_REG_PADCTL2, u4value));

    DramcPhyReset(p);		

    // Log Example
/*
DQS Delay :
 DQS0 = 1 DQS1 = 2 DQS2 = 0 DQS3 = 1
DQ Delay :
 DQ0 = 0 DQ1 = 2 DQ2 = 1 DQ3 = 0 
 DQ4 = 2 DQ5 = 2 DQ6 = 3 DQ7 = 1 
 DQ8 = 1 DQ9 = 0 DQ10 = 2 DQ11 = 0 
 DQ12 = 2 DQ13 = 0 DQ14 = 6 DQ15 = 4 
 DQ16 = 1 DQ17 = 1 DQ18 = 0 DQ19 = 1 
 DQ20 = 1 DQ21 = 2 DQ22 = 1 DQ23 = 0 
 DQ24 = 1 DQ25 = 0 DQ26 = 0 DQ27 = 1 
 DQ28 = 3 DQ29 = 0 DQ30 = 5 DQ31 = 2 
________________________________________________________________________
bit         setup time           hold time
          15             0 1             15
 BIT 0 |  xxxxxxxxx======= =========xxxxxx  setup time :7 hold time : 9
 BIT 1 |  xxxxxxx========= =======xxxxxxxx  setup time :9 hold time : 7
 BIT 2 |  xxxxxxxx======== =======xxxxxxxx  setup time :8 hold time : 7
 BIT 3 |  xxxxxxxxxx====== =========xxxxxx  setup time :6 hold time : 9
 BIT 4 |  xxxxxxxx======== ======xxxxxxxxx  setup time :8 hold time : 6
 BIT 5 |  xxxxxxxx======== =====xxxxxxxxxx  setup time :8 hold time : 5
 BIT 6 |  xxxxxxx========= =====xxxxxxxxxx  setup time :9 hold time : 5
 BIT 7 |  xxxxxxxxxx====== =======xxxxxxxx  setup time :6 hold time : 7

 BIT 8 |  xxxxxxxxxx====== =========xxxxxx  setup time :6 hold time : 9
 BIT 9 |  xxxxxxxxxxxx==== =========xxxxxx  setup time :4 hold time : 9
BIT 10 |  xxxxxxxxxx====== =======xxxxxxxx  setup time :6 hold time : 7
BIT 11 |  xxxxxxxxxxxx==== ========xxxxxxx  setup time :4 hold time : 8
BIT 12 |  xxxxxxxx======== =======xxxxxxxx  setup time :8 hold time : 7
BIT 13 |  xxxxxxxxxxxx==== =========xxxxxx  setup time :4 hold time : 9
BIT 14 |  xxxx============ ====xxxxxxxxxxx  setup time :12 hold time : 4
BIT 15 |  xxxxxx========== =====xxxxxxxxxx  setup time :10 hold time : 5

BIT 16 |  xxxxxxxxxx====== ====xxxxxxxxxxx  setup time :6 hold time : 4
BIT 17 |  xxxxxxxxxx====== ====xxxxxxxxxxx  setup time :6 hold time : 4
BIT 18 |  xxxxxxxxxxx===== ====xxxxxxxxxxx  setup time :5 hold time : 4
BIT 19 |  xxxxxxxxx======= ====xxxxxxxxxxx  setup time :7 hold time : 4
BIT 20 |  xxxxxxxxxx====== ====xxxxxxxxxxx  setup time :6 hold time : 4
BIT 21 |  xxxxxxxx======== ====xxxxxxxxxxx  setup time :8 hold time : 4
BIT 22 |  xxxxxxxxx======= ====xxxxxxxxxxx  setup time :7 hold time : 4
BIT 23 |  xxxxxxxxxxx===== ====xxxxxxxxxxx  setup time :5 hold time : 4

BIT 24 |  xxxxxxxx======== =========xxxxxx  setup time :8 hold time : 9
BIT 25 |  xxxxxxxx======== ==========xxxxx  setup time :8 hold time : 10
BIT 26 |  xxxxxxxx======== ==========xxxxx  setup time :8 hold time : 10
BIT 27 |  xxxxxxx========= ========xxxxxxx  setup time :9 hold time : 8
BIT 28 |  xxxxxx========== ======xxxxxxxxx  setup time :10 hold time : 6
BIT 29 |  xxxxxxxx======== ==========xxxxx  setup time :8 hold time : 10
BIT 30 |  xxx============= =====xxxxxxxxxx  setup time :13 hold time : 5
BIT 31 |  xxxxxx========== ========xxxxxxx  setup time :10 hold time : 8
*/

    if (ucfail == 1)
    {
        return DRAM_FAIL;
    }

#ifdef DDR_FT_LOAD_BOARD
    if (p->channel == CHANNEL_B)
    {
        LoadBoardShowResult(FLAG_TX_CALIBRATION, FLAG_CALIBRATION_PASS, p->channel, FLAG_COMPLETE_AND_PASS);
    }
#endif

    return DRAM_OK;
}

#if 0
DRAM_STATUS_T DramcTxWindowPerbitCal_test(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0, ii, jj, uc_dly;
    U32 u4value, u4err_value, u4fail_bit, u4fail_bit_R, u4fail_bit_F;
    TXDQS_PERBIT_DLY_T dqdqs_perbit_dly[DQ_DATA_WIDTH];
    U8 ucbit_first, ucbit_last, uchold_pass_number, ucsetup_pass_number;
    U8 ucmax_dqsdly_byte[DQS_NUMBER], ucave_dqdly_byte[DQS_NUMBER];
    U8 ucfail = 0;

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }
            
    // initialize parameters
    for (ii = 0; ii < p->data_width; ii++)
    {
        dqdqs_perbit_dly[ii].first_dqdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqdly_pass = -2;
        dqdqs_perbit_dly[ii].first_dqsdly_pass = -1;
        dqdqs_perbit_dly[ii].last_dqsdly_pass = -2;
    }
           
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            // enable TE2, audio pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_ISI_PATTERN)
        {
            // enable TE2, ISI pattern
            u4err_value = 0;
            for (jj = 0; jj < 1; jj++)
            {
                u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 0, 0, 0, 0);
            }            
        }
        else if (p->test_pattern == TEST_XTALK_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else if (p->test_pattern == TEST_MIX_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
            u4err_value |= DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("Not support test pattern!! Use audio pattern by default.\n"));
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }

        // check fail bit ,0 ok ,others fail
        for (jj = 0; jj < p->data_width; jj++)
        {
            u4fail_bit = u4err_value&((U32)1<<jj);

            if (u4fail_bit == 0)
            {
                if (dqdqs_perbit_dly[jj].first_dqsdly_pass == -1)
                {
                    // first DQS pass delay tap
                    dqdqs_perbit_dly[jj].first_dqsdly_pass = ii;
                }
                if (dqdqs_perbit_dly[jj].last_dqsdly_pass == -2)
                {
                    if (ii == (MAX_TX_DQSDLY_TAPS-1))
                    {
                        // pass to the last tap
                        dqdqs_perbit_dly[jj].last_dqsdly_pass = ii;
                    }
                }
            }
            else
            {
                if ((dqdqs_perbit_dly[jj].first_dqsdly_pass != -1)&&(dqdqs_perbit_dly[jj].last_dqsdly_pass == -2))
                {
                    dqdqs_perbit_dly[jj].last_dqsdly_pass = ii -1;
                }
            }

            if (u4fail_bit == 0)
            {
                mcSHOW_DBG_MSG3(("o"));
                mcFPRINTF((fp_A60808, "o"));
            }
            else
            {
                {
                    mcSHOW_DBG_MSG3(("x"));
                    mcFPRINTF((fp_A60808, "x"));
                }
            }            
        }
        mcSHOW_DBG_MSG3(("\n"));
        mcFPRINTF((fp_A60808, "\n"));    
    
    return DRAM_OK;
}
#endif

#ifdef EYE_SCAN

#ifdef ACDIO_TEST	
#define REFTEST_NUM		3
const S32 RefTest[3] = {4, 15, 27};
const S32 RefTestV[3] = {400, 600, 800};
#endif

void DramcEyeScan(DRAMC_CTX_T *p)
{
	const U32 uiPHY_LPDDR_Mapping_POP_CHA[32] = {24, 25, 26, 27, 28, 29, 30, 31, 
												 15, 14, 13, 12, 11, 10, 9, 8,  
												 0, 1, 2, 3, 4, 5, 6, 7,
												 23, 22, 21, 20, 19, 18, 17, 16};
	const U32 uiPHY_LPDDR_Mapping_POP_CHB[32] = {16, 17, 18, 19, 20, 21, 22, 23,
												     7, 6, 5, 4, 3, 2, 1, 0,
												     8, 9, 10, 11, 12, 13, 14, 15, 
												     31, 30, 29, 28, 27, 26, 25, 24};
	U8 ucBit, ucCount;
	U32 *uiLPDDR_PHY_Mapping;
	U32 APRefTune, DQRefTune, CARefTune;
	U32 DS = 3;
	S32 ucRefTune;
#ifdef ACDIO_TEST	
	S32 ucRefTestCnt;
	S32 TotalWin;
#endif	

	if (p->channel == CHANNEL_A)
	{
	    uiLPDDR_PHY_Mapping = (U32 *)uiPHY_LPDDR_Mapping_POP_CHA;
	}
	else
	{
	    uiLPDDR_PHY_Mapping = (U32 *)uiPHY_LPDDR_Mapping_POP_CHB;
	}	
	
#ifndef ACDIO_TEST	
	// RX data eye
	mcSHOW_DBG_MSG2(("***************  RX data eye *******************\n"));
	for (ucBit=0; ucBit<32; ucBit++)
	{
		mcSHOW_DBG_MSG2(("\n[RX] DDRPHY DQ %d LPDDR DQ %d\n", ucBit, uiLPDDR_PHY_Mapping[ucBit]));
		mcSHOW_DBG_MSG2(("================================================= \n"));
	
		for (ucRefTune=31; ucRefTune>=0; ucRefTune--)
		{
			if (ucRefTune<10) {
				mcSHOW_DBG_MSG2((" %d | ", ucRefTune));
			} else {
				mcSHOW_DBG_MSG2(("%d | ", ucRefTune));
			}

			pmic_config_interface(0x8004, 0x0, 0x1, 7); // 0x8004 bit7 = 1'b0
			// Bit 0:RG_EN_REF_XX
			// Bit 2~1:RG_DS_XX
			// Bit 7~3:RG_REFTUNE_XX
			pmic_config_interface(0x8006, (ucRefTune<<3) | (DS<<1) | (1), 0xFFFF, 0);
			//pmic_config_interface(0x8008, (ucRefTune<<3) | (3<<1) | (1), 0xFFFF, 0);
			//pmic_config_interface(0x800A, (ucRefTune<<3) | (3<<1) | (1), 0xFFFF, 0);

			//pmic_read_interface(0x8006, &APRefTune, 0xFFFF, 0);
			//pmic_read_interface(0x8008, &DQRefTune, 0xFFFF, 0);
			//pmic_read_interface(0x800A, &CARefTune ,0xFFFF, 0);		
			//mcSHOW_DBG_MSG2(("APRefTune=%xh,  DQRefTune=%xh, CARefTune=%xh\n", APRefTune, DQRefTune, CARefTune));
			mcDELAY_MS(100);
			
			RXPERBIT_LOG_PRINT = 0;
			DramcRxWindowPerbitCal((DRAMC_CTX_T *) p);	
			RXPERBIT_LOG_PRINT = 1;
			mcSHOW_DBG_MSG2(("setup time=%d hold time=%d",  
				EyeScanWin[ucBit].ucsetup_pass_number, EyeScanWin[ucBit].uchold_pass_number));
			for (ucCount=16; ucCount>0; ucCount--) 
			{
				if (ucCount>EyeScanWin[ucBit].ucsetup_pass_number)
				{
					mcSHOW_DBG_MSG2(("X"));
				}
				else
				{
					mcSHOW_DBG_MSG2(("O"));
				}
			}
			for (ucCount=1; ucCount<=64; ucCount++) 
			{
				if (ucCount>EyeScanWin[ucBit].uchold_pass_number)
				{
					mcSHOW_DBG_MSG2(("X"));
				}
				else
				{
					mcSHOW_DBG_MSG2(("O"));
				}
			}
			mcSHOW_DBG_MSG2(("\n"));

		}
	}
	pmic_config_interface(0x8006, 0x007D, 0xFFFF, 0);

	
	DramcRxWindowPerbitCal((DRAMC_CTX_T *) p);	
#endif	

#if 1
	// TX data eye
	mcSHOW_DBG_MSG2(("***************  TX data eye *******************\n"));
	#ifdef ACDIO_TEST	
	TotalWin = 0;
	#endif
	for (ucBit=0; ucBit<32; ucBit++)
	{
		mcSHOW_DBG_MSG2(("\n[TX] DDRPHY DQ %d LPDDR %d TX \n", ucBit, uiLPDDR_PHY_Mapping[ucBit]));
		mcSHOW_DBG_MSG2(("================================================= \n"));
	
		#ifdef ACDIO_TEST	
		for (ucRefTestCnt=0; ucRefTestCnt<REFTEST_NUM; ucRefTestCnt++)
		{
			ucRefTune = RefTest[ucRefTestCnt];
			if (ucRefTune<10) {
				mcSHOW_DBG_MSG2((" %d(%dmv) | ", ucRefTune, RefTestV[ucRefTestCnt]));
			} else {
				mcSHOW_DBG_MSG2(("%d(%dmv) | ", ucRefTune, RefTestV[ucRefTestCnt]));
			}		
		#else
		for (ucRefTune=31; ucRefTune>=0; ucRefTune--)
		{
			if (ucRefTune<10) {
				mcSHOW_DBG_MSG2((" %d | ", ucRefTune));
			} else {
				mcSHOW_DBG_MSG2(("%d | ", ucRefTune));
			}
		#endif

			pmic_config_interface(0x8004, 0x0, 0x1, 7); // 0x8004 bit7 = 1'b0
			// Bit 0:RG_EN_REF_XX
			// Bit 2~1:RG_DS_XX
			// Bit 7~3:RG_REFTUNE_XX
			pmic_config_interface(0x8008, (ucRefTune<<3) | (DS<<1) | (1), 0xFFFF, 0);
			mcDELAY_MS(100);
			
			RXPERBIT_LOG_PRINT = 0;
			DramcTxWindowPerbitCal((DRAMC_CTX_T *) p);	
			RXPERBIT_LOG_PRINT = 1;
			#ifdef ACDIO_TEST	
			EyeScanWin[ucBit].window_number = EyeScanWin[ucBit].ucsetup_pass_number+EyeScanWin[ucBit].uchold_pass_number;
			mcSHOW_DBG_MSG2(("window=%d setup time=%d hold time=%d",  
				EyeScanWin[ucBit].window_number ,
				EyeScanWin[ucBit].ucsetup_pass_number, EyeScanWin[ucBit].uchold_pass_number));
			TotalWin += EyeScanWin[ucBit].window_number ;
			#endif

			#ifndef ACDIO_TEST	
			for (ucCount=16; ucCount>0; ucCount--) 
			{
				if (ucCount>EyeScanWin[ucBit].ucsetup_pass_number)
				{
					mcSHOW_DBG_MSG2(("X", ucRefTune));
				}
				else
				{
					mcSHOW_DBG_MSG2(("O"));
				}
			}
			for (ucCount=1; ucCount<=15; ucCount++) 
			{
				if (ucCount>EyeScanWin[ucBit].uchold_pass_number)
				{
					mcSHOW_DBG_MSG2(("X"));
				}
				else
				{
					mcSHOW_DBG_MSG2(("O"));
				}
			}
			#endif
			mcSHOW_DBG_MSG2(("\n"));
		}
	}		
	#ifdef ACDIO_TEST	
	mcSHOW_DBG_MSG2(("\n\nChannel %d Total window=%d\n\n",  p->channel, TotalWin));
	#endif		
	pmic_config_interface(0x8008, 0x007D, 0xFFFF, 0);
	DramcTxWindowPerbitCal((DRAMC_CTX_T *) p);	
	
#ifndef ACDIO_TEST	
	// CA eye
	mcSHOW_DBG_MSG2(("***************  CA eye *******************\n"));
	for (ucBit=0; ucBit<10; ucBit++)
	{
		mcSHOW_DBG_MSG2(("\nCA %d  \n", ucBit));
		mcSHOW_DBG_MSG2(("================================================= \n"));
	
		for (ucRefTune=31; ucRefTune>=0; ucRefTune--)
		{
			if (ucRefTune<10) {
				mcSHOW_DBG_MSG2((" %d | ", ucRefTune));
			} else {
				mcSHOW_DBG_MSG2(("%d | ", ucRefTune));
			}

			pmic_config_interface(0x8004, 0x0, 0x1, 7); // 0x8004 bit7 = 1'b0
			// Bit 0:RG_EN_REF_XX
			// Bit 2~1:RG_DS_XX
			// Bit 7~3:RG_REFTUNE_XX
			pmic_config_interface(0x800A, (ucRefTune<<3) | (DS<<1) | (1), 0xFFFF, 0);
			mcDELAY_MS(100);
			
			RXPERBIT_LOG_PRINT = 0;
			DramcCATraining((DRAMC_CTX_T *) p);
			RXPERBIT_LOG_PRINT = 1;
			for (ucCount=16; ucCount>0; ucCount--) 
			{
				if (ucCount>EyeScanWin[ucBit].ucsetup_pass_number)
				{
					mcSHOW_DBG_MSG2(("X", ucRefTune));
				}
				else
				{
					mcSHOW_DBG_MSG2(("O"));
				}
			}
			for (ucCount=1; ucCount<=15; ucCount++) 
			{
				if (ucCount>EyeScanWin[ucBit].uchold_pass_number)
				{
					mcSHOW_DBG_MSG2(("X"));
				}
				else
				{
					mcSHOW_DBG_MSG2(("O"));
				}
			}
			mcSHOW_DBG_MSG2(("\n"));

		}
	}	
	pmic_config_interface(0x800A, 0x007D, 0xFFFF, 0);
	DramcCATraining((DRAMC_CTX_T *) p);
#endif	

#endif	
}
#endif		

#ifdef LOOPBACK_TEST

void DramcLoopbackTest(DRAMC_CTX_T *p)
{
	unsigned int DATLAT=0, WLAT=0, CoarseTune=0, ucStartCalVal;  
	U8 ucstatus = 0, i, j;
	U32 u4value;
	
	mcSHOW_DBG_MSG2(("Loop back test start...\n"));

	#if 1
	// Disable HW gating first
	// 0x1c0[31]
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), &u4value);
	mcCLR_BIT(u4value, POS_DQSCAL0_STBCALEN);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), u4value);
	#endif

	/* 1.enable burst mode for gating window */
	// 0x0e0[28] = 1
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), &u4value);
	mcSET_BIT(u4value, POS_DQSCTL1_DQSIENMODE);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), u4value);

	// new gating, dual-phase DQS clock gating control enabling
	// 0x124[30] = 1
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSGCTL), &u4value);
	mcSET_BIT(u4value, POS_DQSGCTL_DQSGDUALP);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSGCTL), u4value);

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &u4value);
	if ((u4value & 0xe0)==0xe0)
	{
		// 3 pipes
		ucStartCalVal = 8;
	}
	else
	{
		// Should be 1 pipe.
		ucStartCalVal = 4;
	}
        
	for  (DATLAT = ucStartCalVal; DATLAT < DATLAT_TAP_NUMBER; DATLAT++) {
		mcSHOW_DBG_MSG2(("\nDATLAT = %xh...\n", DATLAT));
		dle_factor_handler(p, DATLAT);
		ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DDR2CTL), &u4value);
		mcSHOW_DBG_MSG2(("Reg.7ch=%xh\n", u4value));
		ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PADCTL4), &u4value);
		mcSHOW_DBG_MSG2(("Reg.e4h=%xh\n", u4value));
		ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
		mcSHOW_DBG_MSG2(("Reg.f0h=%xh\n", u4value));
		ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x80), &u4value);
		mcSHOW_DBG_MSG2(("Reg.80h=%xh\n", u4value));

		mcSHOW_DBG_MSG2(("   |0   8  16  24  32  40  48  56  64  72  80  88  96 104 112 120\n"));
		mcSHOW_DBG_MSG2(("=============================================================\n"));

		for ( i = 1; i <= DQS_GW_COARSE_END; i+=DQS_GW_COARSE_STEP) 
		{
                        if (i<10) {
                                mcSHOW_DBG_MSG2((" %d |", i));
                        } else {
                                mcSHOW_DBG_MSG2(("%d |", i));
                        }		
			//adjust factor steps
			dqsi_gw_dly_coarse_factor_handler(p, i);        

			for ( j = DQS_GW_FINE_START; j <= DQS_GW_FINE_END; j+=DQS_GW_FINE_STEP) 
			{
				//adjust factor steps
				dqsi_gw_dly_fine_factor_handler(p, j);

				//0xd8[15:12] = 0xF             DQIEN fix high
				ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xd8), &u4value);
				u4value |= 0x0000f000;
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xd8), u4value);
				    
				//0xfc[17] = 0x1                    internal loopback data path switch enable
				ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xfc), &u4value);
				mcSET_BIT(u4value, 17);
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xfc), u4value);

				//0x104 = 0x55aaaa55         loopback test pattern1
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x104), 0x55aaaa55);
				
				//0x108 = 0xcc3333cc          loopback test pattern2
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x108), 0xcc3333cc);

				//0x10c = 0x78788787         loopback test pattern3
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x10c), 0x78788787);

				//0x1dc[26] and [3] = 0x1  turn off dynamic clock  
				ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
				mcSET_BIT(u4value, 26);
				mcSET_BIT(u4value, 3);
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);
				
				//0xf8[8] = 1                           enable loopback test
				ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xf8), &u4value);
				mcSET_BIT(u4value, 8);
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xf8), u4value);

				mcDELAY_US(20);
				
				//Result, see 0x3fc[24] = 0x0 (loopback test fail flag)
				ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3fc), &u4value);
				if (u4value & (0x1 << 24))
				{
					// Fail
					mcSHOW_DBG_MSG2(("X   "));
				}
				else
				{
					// Pass
					mcSHOW_DBG_MSG2(("O   "));
				}

				//0xf8[8] = 0                           disable loopback test
				ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xf8), &u4value);
				mcCLR_BIT(u4value, 8);
				ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xf8), u4value);
				
			        // Dramc software reset 0xe4[11] (dramc ao register)
				// Toggle DRAMC SW RESET.
			        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe4), &u4value);
			        mcSET_BIT(u4value, 11);
			        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), u4value);
			        mcCLR_BIT(u4value, 11);
			        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), u4value);

				//ddrphy software reset 0xf0[28] (ddrphy register) 
				DramcPhyReset(p);

				// ddrphy chip reset 0x100000060[31:24]=0x88, 0x100000060[6]=0x1
				u4value = (*(volatile unsigned int *)(0x10000060));
				u4value &= (0x00ffffff);
				u4value |= (0x88000000);
				u4value |= (0x01 << 6);
				(*(volatile unsigned int *)(0x10000060)) = u4value;
			}
			ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCTL1), &u4value);
			mcSHOW_DBG_MSG2(("    Reg.e0h=%xh", u4value));
			ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH2), &u4value);
			mcSHOW_DBG_MSG2(("    Reg.404h=%xh", u4value));
			ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH5), &u4value);
			mcSHOW_DBG_MSG2(("    Reg.410h=%xh", u4value));
			mcSHOW_DBG_MSG2(("\n"));
		}
	}

}

#endif
