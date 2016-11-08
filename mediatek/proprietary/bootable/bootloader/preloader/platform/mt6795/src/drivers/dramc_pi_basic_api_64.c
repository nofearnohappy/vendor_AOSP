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
 * $RCSfile: pi_basic_api.c,v $
 * $Revision: #5 $
 *
 *---------------------------------------------------------------------------*/

/** @file dramc_pi_basic_api.c
 *  Basic DRAMC API implementation
 */

//-----------------------------------------------------------------------------
// Include files
//-----------------------------------------------------------------------------
#include "platform.h"
#include "dramc_common.h"
#include "dramc_register_64.h"
#include "dramc_pi_api_64.h"

#ifdef DUAL_RANKS
unsigned int uiDualRank = 1;
#endif

U8 VCOK_Cha_Mempll2, VCOK_Cha_Mempll3, VCOK_Cha_Mempll4;
U8 VCOK_Chb_Mempll2, VCOK_Chb_Mempll3, VCOK_Chb_Mempll4;
U8 VCOK_05PHY_Mempll2, VCOK_05PHY_Mempll3, VCOK_05PHY_Mempll4;

#ifdef WAVEFORM_MEASURE
U32 StartMeasureWaveform = 0;
#endif	//WAVEFORM_MEASURE
#if defined(DQ_PLUS_90) || defined(DQ_MINUS_90_IN_LOW_FREQ)
extern S8 DQ_Phase[2];
#endif
//-----------------------------------------------------------------------------
// Global variables
//-----------------------------------------------------------------------------
#if 0
extern int A_Reg3e0, A_Reg3e4, B_Reg3e0, B_Reg3e4;
#endif
extern const U32 uiLPDDR_PHY_Mapping_POP_CHA[32];
extern const U32 uiLPDDR_PHY_Mapping_POP_CHB[32];
extern U8 opt_gw_coarse_value_R0[2], opt_gw_fine_value_R0[2];
extern U8 opt_gw_coarse_value_R1[2], opt_gw_fine_value_R1[2];


const U32 CHA_Freq_RegAddr[FREQREG_SIZE] =
{
	CHA_DRAMCAO_BASE + 0x0e0,		// GW : R0 Coarse tune. DQSINCTL
	CHA_DRAMCAO_BASE + 0x404,		// GW : R0 Coarse tune. TXDLY_DQSGATE and  TXDLY_DQSGATE_P1
	CHA_DRAMCAO_BASE + 0x410,		// GW : R0 Coarse tune. dly_DQSGATE and  dly_DQSGATE_P1
	CHA_DRAMCAO_BASE + 0x094,		// GW : R0 Fine tune.
	CHA_DRAMCAO_BASE + 0x118,		// GW : R1 Coarse tune. R1DQSINCTL
	CHA_DRAMCAO_BASE + 0x418,		// GW : R1 Coarse tune. TXDLY_R1DQSGATE,  TXDLY_R1DQSGATE_P1, dly_R1DQSGATE and  dly_R1DQSGATE_P1
	CHA_DRAMCAO_BASE + 0x098,		// GW : R1 Fine tune.

	CHA_DRAMCAO_BASE + 0x07c,		// DLE, AC timing : [6:4] = DATLAT[2:0]
	CHA_DRAMCAO_BASE + 0x0e4,		// DLE : [3] = DATLAT[3]
	CHA_DRAMCAO_BASE + 0x0f0,		// DLE : [25] = DATLAT[4]
	CHA_DRAMCAO_BASE + 0x080,		// DLE : [7:5] RX pipe, [12:8] DSEL

	CHA_DRAMCAO_BASE + 0x138,		// RANKINCTL_ROOT1
	CHA_DRAMCAO_BASE + 0x1c4,		//RANKINCTL
#ifdef DQ_MINUS_90_IN_LOW_FREQ
	CHA_DRAMCAO_BASE + 0x41c,
	CHA_DRAMCAO_BASE + 0x420,
	CHA_DRAMCAO_BASE + 0x424,
	CHA_DRAMCAO_BASE + 0x428,
	CHA_DRAMCAO_BASE + 0x42c,
	CHA_DDRPHY_BASE     + 0x430,
	CHA_DDRPHY_BASE     + 0x434,
	CHA_DDRPHY_BASE     + 0x438,
#elif defined(CLKCA_PLUS_90_IN_LOW_FREQ)
	CHA_DRAMCAO_BASE + 0x410,
	CHA_DRAMCAO_BASE + 0x414,
	CHA_DDRPHY_BASE     + 0x430,
	CHA_DDRPHY_BASE     + 0x1e0,
	CHA_DRAMCAO_BASE + 0x1e4,
	CHA_DDRPHY_BASE     + 0x23c,
#endif	// CLKCA_PLUS_90_IN_LOW_FREQ
};

const U32 CHB_Freq_RegAddr[FREQREG_SIZE] =
{
	CHB_DRAMCAO_BASE + 0x0e0,		// GW : R0 Coarse tune. DQSINCTL
	CHB_DRAMCAO_BASE + 0x404,		// GW : R0 Coarse tune. TXDLY_DQSGATE and  TXDLY_DQSGATE_P1
	CHB_DRAMCAO_BASE + 0x410,		// GW : R0 Coarse tune. dly_DQSGATE and  dly_DQSGATE_P1
	CHB_DRAMCAO_BASE + 0x094,		// GW : R0 Fine tune.
	CHB_DRAMCAO_BASE + 0x118,		// GW : R1 Coarse tune. R1DQSINCTL
	CHB_DRAMCAO_BASE + 0x418,		// GW : R1 Coarse tune. TXDLY_R1DQSGATE,  TXDLY_R1DQSGATE_P1, dly_R1DQSGATE and  dly_R1DQSGATE_P1
	CHB_DRAMCAO_BASE + 0x098,		// GW : R1 Fine tune.

	CHB_DRAMCAO_BASE + 0x07c,		// DLE, AC timing : [6:4] = DATLAT[2:0]
	CHB_DRAMCAO_BASE + 0x0e4,		// DLE : [3] = DATLAT[3]
	CHB_DRAMCAO_BASE + 0x0f0,		// DLE : [25] = DATLAT[4]
	CHB_DRAMCAO_BASE + 0x080,		// DLE : [7:5] RX pipe, [12:8] DSEL

	CHB_DRAMCAO_BASE + 0x138,		// RANKINCTL_ROOT1
	CHB_DRAMCAO_BASE + 0x1c4,		//RANKINCTL
#ifdef DQ_MINUS_90_IN_LOW_FREQ
	CHB_DRAMCAO_BASE + 0x41c,
	CHB_DRAMCAO_BASE + 0x420,
	CHB_DRAMCAO_BASE + 0x424,
	CHB_DRAMCAO_BASE + 0x428,
	CHB_DRAMCAO_BASE + 0x42c,
	CHB_DDRPHY_BASE     + 0x430,
	CHB_DDRPHY_BASE     + 0x434,
	CHB_DDRPHY_BASE     + 0x438,
#elif defined(CLKCA_PLUS_90_IN_LOW_FREQ)
	CHB_DRAMCAO_BASE + 0x410,
	CHB_DRAMCAO_BASE + 0x414,
	CHB_DDRPHY_BASE     + 0x430,
	CHB_DDRPHY_BASE     + 0x1e0,
	CHB_DRAMCAO_BASE + 0x1e4,
	CHB_DDRPHY_BASE     + 0x23c,
#endif	// CLKCA_PLUS_90_IN_LOW_FREQ
};

//=== The following 4 array need to be sent to kernel for frequency jumping.
#if 0
U32 CHA_LowFreq_RegVal[FREQREG_SIZE], CHB_LowFreq_RegVal[FREQREG_SIZE];
U32 CHA_HighFreq_RegVal[FREQREG_SIZE], CHB_HighFreq_RegVal[FREQREG_SIZE];
#else
U32 *CHA_LowFreq_RegVal, *CHB_LowFreq_RegVal;
U32 *CHA_HighFreq_RegVal, *CHB_HighFreq_RegVal;
#endif

#ifdef XTALK_SSO_STRESS

// only for SW worst pattern
const static U32 u4xtalk_pat[76] = { \
0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, \
0x01010101, 0x02020202, 0x04040404, 0x08080808, 0x10101010, 0x20202020, 0x40404040, 0x80808080, \
0x00000000, 0x00000000, 0xffffffff, 0xffffffff, \
0xfefefefe, 0xfdfdfdfd, 0xfbfbfbfb, 0xf7f7f7f7, 0xefefefef, 0xdfdfdfdf, 0xbfbfbfbf, 0x7f7f7f7f, \
0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0x00000000, \
0xfefefefe, 0x01010101, 0xfdfdfdfd, 0x02020202, 0xfbfbfbfb, 0x04040404, 0xf7f7f7f7, 0x08080808, \
0xefefefef, 0x10101010, 0xdfdfdfdf, 0x20202020, 0xbfbfbfbf, 0x40404040, 0x7f7f7f7f, 0x80808080, \
0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0x00000000, \
0xfefefefe, 0x01010101, 0xfdfdfdfd, 0x02020202, 0xfbfbfbfb, 0x04040404, 0xf7f7f7f7, 0x08080808, \
0xefefefef, 0x10101010, 0xdfdfdfdf, 0x20202020, 0xbfbfbfbf, 0x40404040, 0x7f7f7f7f, 0x80808080, \
0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, \
};

const static U32 u4xtalk_pat_64[76*2] = { \
0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, \
0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, \
0x01010101, 0x01010101, 0x02020202, 0x02020202, 0x04040404, 0x04040404, 0x08080808, 0x08080808,\
0x10101010, 0x10101010, 0x20202020, 0x20202020, 0x40404040, 0x40404040, 0x80808080, 0x80808080,\
0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xfefefefe, 0xfefefefe, 0xfdfdfdfd, 0xfdfdfdfd, 0xfbfbfbfb, 0xfbfbfbfb, 0xf7f7f7f7, 0xf7f7f7f7,\
0xefefefef, 0xefefefef, 0xdfdfdfdf, 0xdfdfdfdf, 0xbfbfbfbf, 0xbfbfbfbf, 0x7f7f7f7f, 0x7f7f7f7f,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0x00000000, 0x00000000,\
0x00000000, 0x00000000, \
0xfefefefe, 0xfefefefe, 0x01010101, 0x01010101, 0xfdfdfdfd, 0xfdfdfdfd, 0x02020202, 0x02020202,\
0xfbfbfbfb, 0xfbfbfbfb, 0x04040404, 0x04040404, 0xf7f7f7f7, 0xf7f7f7f7, 0x08080808, 0x08080808,\
0xefefefef, 0xefefefef, 0x10101010, 0x10101010, 0xdfdfdfdf, 0xdfdfdfdf, 0x20202020, 0x20202020,\
0xbfbfbfbf, 0xbfbfbfbf, 0x40404040, 0x40404040, 0x7f7f7f7f, 0x7f7f7f7f, 0x80808080, 0x80808080,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000,\
0x00000000, 0x00000000, 0x00000000, 0x00000000, \
0xfefefefe, 0xfefefefe, 0x01010101, 0x01010101, 0xfdfdfdfd, 0xfdfdfdfd, 0x02020202, 0x02020202,\
0xfbfbfbfb, 0xfbfbfbfb, 0x04040404, 0x04040404, 0xf7f7f7f7, 0xf7f7f7f7, 0x08080808, 0x08080808,\
0xefefefef, 0xefefefef, 0x10101010, 0x10101010, 0xdfdfdfdf, 0xdfdfdfdf, 0x20202020, 0x20202020,\
0xbfbfbfbf, 0xbfbfbfbf, 0x40404040, 0x40404040, 0x7f7f7f7f, 0x7f7f7f7f, 0x80808080, 0x80808080,\
0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, \
0xffffffff, 0xffffffff,\
};

const static U32 u4spb0x[64] = { \
0x00000000, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, \
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000, \
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, \
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, \
0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, \
0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, \
0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, \
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0xffffffff, 0xffffffff, \
};

const static U32 u4spb0x_64[64*2] = { \
0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0x00000000, 0x00000000,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x00000000, 0x00000000,\
0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
0xffffffff, 0xffffffff, 0x00000000, 0x00000000, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,\
};
#endif

// num of "1", num of "0", repeat,
// num of "1", num of "0" (at the end)
static U8 ucsso_bsx[6][5] = { \
1, 2, 7, 0, 1, \
2, 1, 15,0, 2, \
3, 2, 7, 1, 0, \
2, 3, 7, 1, 0, \
4, 3, 7, 1, 0, \
3, 4, 7, 0, 0,\
};

//-------------------------------------------------------------------------
// Set Freqence table location for DRAM DVFS
//-------------------------------------------------------------------------
void SetFreqTableAddr()
{
    CHA_LowFreq_RegVal = 0x12FE00;  //Magic number(temp solution), use part of SRAM for RAM console
    CHB_LowFreq_RegVal = CHA_LowFreq_RegVal + FREQREG_SIZE;
    CHA_HighFreq_RegVal = CHB_LowFreq_RegVal + FREQREG_SIZE;
    CHB_HighFreq_RegVal = CHA_HighFreq_RegVal + FREQREG_SIZE;
}

//-------------------------------------------------------------------------
/** ucDramC_Register_Read
 *  DRAMC register read (32-bit).
 *  @param  u4reg_addr    register address in 32-bit.
 *  @param  pu4reg_value  Pointer of register read value.
 *  @retval 0: OK, 1: FAIL
 */
//-------------------------------------------------------------------------
// This function need to be porting by BU requirement
U8 ucDramC_Register_Read(U32 u4reg_addr, U32 *pu4reg_value)
{
U8 ucstatus = 0;

   U32 u4Channel = u4reg_addr >> CH_INFO;
   u4reg_addr &= 0x3fffffff;

   if (u4Channel == CHANNEL_A)
   {
   	*pu4reg_value = 	(*(volatile unsigned int *)(CHA_DRAMCAO_BASE + (u4reg_addr)));
   }
   else
   {
   	*pu4reg_value =  	(*(volatile unsigned int *)(CHB_DRAMCAO_BASE + (u4reg_addr)));
   }

    return ucstatus;
}

//-------------------------------------------------------------------------
/** ucDramC_Register_Write
 *  DRAMC register write (32-bit).
 *  @param  u4reg_addr    register address in 32-bit.
 *  @param  u4reg_value   register write value.
 *  @retval 0: OK, 1: FAIL
 */
//-------------------------------------------------------------------------
// This function need to be porting by BU requirement
U8 ucDramC_Register_Write(U32 u4reg_addr, U32 u4reg_value)
{
U8 ucstatus = 0;

   U32 u4Channel = u4reg_addr >> CH_INFO;
   u4reg_addr &= 0x3fffffff;

   if (u4Channel == CHANNEL_A)
   {
	(*(volatile unsigned int *)(CHA_DRAMCAO_BASE + (u4reg_addr))) = u4reg_value;
   }
   else
   {
	(*(volatile unsigned int *)(CHB_DRAMCAO_BASE + (u4reg_addr))) = u4reg_value;
   }
   dsb();

    return ucstatus;
}

//-------------------------------------------------------------------------
/** ucDram_Register_Read
 *  DRAM register read (32-bit).
 *  @param  u4reg_addr    register address in 32-bit.
 *  @param  pu4reg_value  Pointer of register read value.
 *  @retval 0: OK, 1: FAIL
 */
//-------------------------------------------------------------------------
// This function need to be porting by BU requirement
U8 ucDram_Register_Read(U32 u4reg_addr, U32 *pu4reg_value)
{
U8 ucstatus = 0;

   U32 u4Channel = u4reg_addr >> CH_INFO;
   u4reg_addr &= 0x3fffffff;

   if (u4Channel == CHANNEL_A)
   {
   	*pu4reg_value = 	(*(volatile unsigned int *)(CHA_DRAMCAO_BASE + (u4reg_addr))) |
					(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (u4reg_addr))) |
					(*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + (u4reg_addr)));
   }
   else
   {
   	*pu4reg_value =  	(*(volatile unsigned int *)(CHB_DRAMCAO_BASE + (u4reg_addr))) |
   					(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (u4reg_addr))) |
   					(*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + (u4reg_addr)));
   }

    return ucstatus;
}

//-------------------------------------------------------------------------
/** ucDram_Register_Write
 *  DRAM register write (32-bit).
 *  @param  u4reg_addr    register address in 32-bit.
 *  @param  u4reg_value   register write value.
 *  @retval 0: OK, 1: FAIL
 */
//-------------------------------------------------------------------------
// This function need to be porting by BU requirement
U8 ucDram_Register_Write(U32 u4reg_addr, U32 u4reg_value)
{
U8 ucstatus = 0;

   U32 u4Channel = u4reg_addr >> CH_INFO;
   u4reg_addr &= 0x3fffffff;

   if (u4Channel == CHANNEL_A)
   {
	(*(volatile unsigned int *)(CHA_DRAMCAO_BASE + (u4reg_addr))) = u4reg_value;
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (u4reg_addr))) = u4reg_value;
	(*(volatile unsigned int *)(CHA_DRAMCNAO_BASE + (u4reg_addr))) = u4reg_value;
   }
   else
   {
	(*(volatile unsigned int *)(CHB_DRAMCAO_BASE + (u4reg_addr))) = u4reg_value;
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (u4reg_addr))) = u4reg_value;
	(*(volatile unsigned int *)(CHB_DRAMCNAO_BASE + (u4reg_addr))) = u4reg_value;
   }

   dsb();

    return ucstatus;
}

//-------------------------------------------------------------------------
/** ucDram_Read
 *  DRAM memory read (32-bit).
 *  @param  u4mem_addr    memory address in 32-bit.
 *  @param  pu4mem_value  Pointer of memory read value.
 *  @retval 0: OK, 1: FAIL
 */
//-------------------------------------------------------------------------
// This function need to be porting by BU requirement
U8 ucDram_Read(U32 u4mem_addr, U32 *pu4mem_value)
{
U8 ucstatus = 0;

    *pu4mem_value = 	(*(volatile unsigned int *)(DDR_BASE + (u4mem_addr))) ;

    return ucstatus;
}

//-------------------------------------------------------------------------
/** ucDram_Write
 *  DRAM memory write (32-bit).
 *  @param  u4mem_addr    register address in 32-bit.
 *  @param  u4mem_value   register write value.
 *  @retval 0: OK, 1: FAIL
 */
//-------------------------------------------------------------------------
// This function need to be porting by BU requirement
U8 ucDram_Write(U32 u4mem_addr, U32 u4mem_value)
{
U8 ucstatus = 0;

    (*(volatile unsigned int *)(DDR_BASE + (u4mem_addr))) = u4mem_value;

    return ucstatus;
}

//-------------------------------------------------------------------------
/** Round_Operation
 *  Round operation of A/B
 *  @param  A
 *  @param  B
 *  @retval round(A/B)
 */
//-------------------------------------------------------------------------
U32 Round_Operation(U32 A, U32 B)
{
    U32 temp;

    if (B == 0)
    {
        return 0xffffffff;
    }

    temp = A/B;

    if ((A-temp*B) >= ((temp+1)*B-A))
    {
        return (temp+1);
    }
    else
    {
        return temp;
    }
}

//-------------------------------------------------------------------------
/** MemPllInit
 *  MEMPLL Initialization.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL
 */
//-------------------------------------------------------------------------
DRAM_STATUS_T MemPllPreInit(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4value;

    // error handling
    if (!p)
    {
//        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

#ifdef SPM_MODIFY
    p->channel = CHANNEL_A;
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x1111ff11);
    // for testing
    //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11510111);	// Reg.511h[8]=1 for regiser control 640h
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11511111);

    p->channel = CHANNEL_B;
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x1111ff11);
    // for testing
    //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11510111);	// Reg.511h[8]=1 for regiser control 640h
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11511111);
#endif

    if (p->package == PACKAGE_POP)
    {
        //=================== Integration Part (TOP) =======================
        // Enable MTCMOS power ack switch before ACK_POP_BYP and ACK_SBS_BYP to avoid glitch
        // LDO_PHY2POP_NDIS=1 is better from LDO designer
        // Default: 0x09000000, [15]ACK_SWITCH, [2]MEMCLKENSYNC_SOURCE
        // 0x1000F640[16] & 0x10012640[16] SbS CMDPHYMTCMOS}. Set to 1 to bypass.
        p->channel = CHANNEL_A;
#ifdef SPM_MODIFY
	#ifdef SBSPHY_BYPASS
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09018024);
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008024);
	#endif
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008004);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), 0x09008004);

        p->channel = CHANNEL_B;
#ifdef SPM_MODIFY
	#ifdef SBSPHY_BYPASS
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09018024);
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008024);
	#endif
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008004);
#endif

#if 1
        // ALLCLK_EN
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        // Disable gating function
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
        mcCLR_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
        mcCLR_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);

//#else

        // BYPASS MTCMOS power ack and turn off power for side-by-side PHY
        // [16]ACK_SBS_BYP
        /*
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        mcSET_BIT(u4value, 16);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value); //edward remark

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);*/

        // Disable MTCMOS power ack switch
        // [15]ACK_SWITCH

        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcCLR_BIT(u4value, 15);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
        //ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        //mcCLR_BIT(u4value, 15);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value); // edward remark

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcCLR_BIT(u4value, 15);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

#endif

        // DRAMC 2X mode
        // [0]FDIV2=1
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x07c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x07c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), u4value);

        // Delay counters for SPM power-up MEMPLL
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c0), 0x21271b03);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c4), 0x5096061e);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c0), 0x21271b03);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c4), 0x5096061e);

        // [0][8][11][16][24]select source from register or SPM
        // [12]bypass counter delay chain
#ifndef SPM_MODIFY
       p->channel = CHANNEL_A;
       ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x01011101);

       p->channel = CHANNEL_B;
       ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x01011101);
#endif

        // [27]LDO_PHY2POP_NDIS, [24]LDO_PHY2POP_EN,
        // [7]MEMCLKENMODE, [3]MEMCLKEN_SEL, [0]MEMCLKENB_SEL
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        //mcSET_BIT(u4value, 27); // LDO_PHY2POP_NDIS=1 is better from LDO designer, has set before
 //   #ifdef fcMEMPLL_SEAL_RING
        mcSET_BIT(u4value, 24);
 //   #else
 //       mcCLR_BIT(u4value, 24);
 //   #endif
        mcSET_BIT(u4value, 7);
        mcCLR_BIT(u4value, 3); // must clear to select external source to get correct phase for channel A
        //mcCLR_BIT(u4value, 2); // set channel A & 05PHY to select external source (from channel B)
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        //mcSET_BIT(u4value, 27); // LDO_PHY2POP_NDIS=1 is better from LDO designer, has set before
    //#ifdef fcMEMPLL_SEAL_RING
    //    mcSET_BIT(u4value, 24);
    //#else
    //    mcCLR_BIT(u4value, 24);
    //#endif
    //    mcSET_BIT(u4value, 7);
        mcCLR_BIT(u4value, 3);
        //mcCLR_BIT(u4value, 2);
    // mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        //mcSET_BIT(u4value, 27); // LDO_PHY2POP_NDIS=1 is better from LDO designer, has set before
 //   #ifdef fcMEMPLL_SEAL_RING
        mcSET_BIT(u4value, 24);
 //   #else
 //       mcCLR_BIT(u4value, 24);
 //   #endif
        mcSET_BIT(u4value, 7);
        //mcCLR_BIT(u4value, 3); // no use for channel B
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

    }
    return DRAM_OK;
}

#if defined(MEMPLL_INTERNAL_LOOP_TEST) || defined (MEMPLL_EXTERNAL_LOOP_TEST) || defined(MEASURE_PLL)

//#define TOP_LIMIT  	810000
//#define DOWN_LIMIT	760000
#define TOP_LIMIT  	750000
#define DOWN_LIMIT	650000
unsigned int mt_get_pll_freq(unsigned int ID);

void MemPll_InternalLoop_Test(DRAMC_CTX_T *p, U32 EnableDisableTest)
{
	U8 ucstatus = 0;
	U32 u4value, cal_cnt, freq;

	//CHA PLL2:
	//monitor enable
	// Set 0x1000F614 //RG_MEMPLL2_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000F614)) = ((*(volatile unsigned int *)(0x1000F614)) & 0xfffffffd) | 0x00000002;
	//Set 0x1000F61c[31:0]=0x 0062 1401; //RG_MEMPLL2_MON_EN=1, RG_MEMPLL2_CKSEL_MON=100, RG_MEMPLL2_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff83ffff) | 0x00640000;
	//Set 0x1000F620 //RG_MEMPLL3_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000F620)) = ((*(volatile unsigned int *)(0x1000F620)) & 0xfffffffd) | 0x00000002;
	//Set 0x1000F628[31:0]=0x 0062 1401; //RG_MEMPLL3_MON_EN=1, RG_MEMPLL3_CKSEL_MON=100, RG_MEMPLL3_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff83ffff) | 0x00640000;
	//Set 0x1000F62c //RG_MEMPLL4_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000F62c)) = ((*(volatile unsigned int *)(0x1000F62c)) & 0xfffffffd) | 0x00000002;
	//Set 0x1000F634[31:0]=0x 0060 1401; //RG_MEMPLL4_MON_EN=1, RG_MEMPLL4_CKSEL_MON=100, RG_MEMPLL4_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff83ffff) | 0x00640000;
	//Set 0X1000F60C[31:0]=0X 0000 1BC0; //RG_MEMPLL_TEST[5:4]=01,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11;
	(*(volatile unsigned int *)(0x1000F60C)) = 0x00001BC0;
	//Set 0X1000F638[31:0]= 0X2300 0000; //RG_MEMPLL_TEST_DIV2_SEL=011, /8;
	(*(volatile unsigned int *)(0x1000F638)) = ((*(volatile unsigned int *)(0x1000F638)) & 0xf8ffffff) | 0x03000000;

	freq = mt_get_pll_freq(0x31);
        mcSHOW_DBG_MSG3(("CHA PLL2 output=%d\n", freq));
	if ( (freq < DOWN_LIMIT) || (freq > TOP_LIMIT))
	{
		unsigned int AfterReset = 0;
		mcSHOW_DBG_MSG3(("PLL out of range...\n"));

CHA_PLL2_METER:
		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff83ffff) | 0x00640000;
		freq = mt_get_pll_freq(0x31);
	        mcSHOW_DBG_MSG3(("CHA PLL2 output=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff83ffff) | 0x005c0000;
		freq = mt_get_pll_freq(0x31);
		mcSHOW_DBG_MSG3(("CHA PLL2 feedback=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff87ffff) | 0x00400000;
		freq = mt_get_pll_freq(0x31);
		mcSHOW_DBG_MSG3(("CHA PLL2 REFCLK=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff87ffff) | 0x00480000;
		freq = mt_get_pll_freq(0x31);
		mcSHOW_DBG_MSG3(("CHA PLL2 FBKCLK=%d\n", freq));

	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(20);

		if (AfterReset == 0)
		{
			AfterReset = 1;
			goto CHA_PLL2_METER;
		}

		while(1);
	}

#if 0
	//Select meter clock input
	//Set 0x10000100[31:0] = 0x00003100 //CLK_CFG_8[14:8]=0x30 (AD_MEMPLL2_MONCLK)
	(*(volatile unsigned int *)(0x10000100)) = 0x00003100;

	(*(volatile unsigned int *)(0x10000220)) = 0x00000080;

	//enable meter
	//Set 0x10000214[31:0]=0x00000000;
	(*(volatile unsigned int *)(0x10000214)) = 0xffffff00;

	//Trigger freq meter
	//Set 0x10000220[31:0] = 0x00000081 //CLK26CALI_0[0] = 0x1
	(*(volatile unsigned int *)(0x10000220)) = 0x00000081;

	mcDELAY_US(10);

	//Read meter count
	//cal_cnt = 0x10000224[15:0] //CLK26CALI_1[15:0]
	cal_cnt = (*(volatile unsigned int *)(0x10000224)) & 0x0000ffff;
        mcSHOW_DBG_MSG3(("0x10000224[15:0]=0x%x\n", cal_cnt));
	//Calculate measured freq. freq = (26000 * cal_cnt) / 1024;
	freq = (0x6590 * cal_cnt) / 0x400;
        mcSHOW_DBG_MSG3(("CHA PLL2 output=%d\n", freq));
	if ( (freq < 760000) || (freq > 820000))
	{
	      mcSHOW_DBG_MSG3(("PLL out of range...\n"));
	      //while(1);
	}
#endif

	//CHA PLL4:
	//monitor enable
	//Set 0x1000F614 //RG_MEMPLL2_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000F614)) = ((*(volatile unsigned int *)(0x1000F614)) & 0xfffffffd) | 0x00000002;
	//Set 0x1000F61c[31:0]=0x 0062 1401; //RG_MEMPLL2_MON_EN=1, RG_MEMPLL2_CKSEL_MON=100, RG_MEMPLL2_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff83ffff) | 0x00640000;
	//Set 0x1000F620 //RG_MEMPLL3_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000F620)) = ((*(volatile unsigned int *)(0x1000F620)) & 0xfffffffd) | 0x00000002;
	//Set 0x1000F628[31:0]=0x 0062 1401; //RG_MEMPLL3_MON_EN=1, RG_MEMPLL3_CKSEL_MON=100, RG_MEMPLL3_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff83ffff) | 0x00640000;
	//Set 0x1000F62c //RG_MEMPLL4_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000F62c)) = ((*(volatile unsigned int *)(0x1000F62c)) & 0xfffffffd) | 0x00000002;
	//Set 0x1000F634[31:0]=0x 0060 1401; //RG_MEMPLL4_MON_EN=1, RG_MEMPLL4_CKSEL_MON=100, RG_MEMPLL4_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff83ffff) | 0x00640000;
	//Set 0X1000F60C[31:0]=0X 0000 3BC0; //RG_MEMPLL_TEST[5:4]=11,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11;
	(*(volatile unsigned int *)(0x1000F60C)) = 0x00003BC0;
	//Set 0X1000F638[31:0]= 0X2300 0000; //RG_MEMPLL_TEST_DIV2_SEL=011, /8;
	(*(volatile unsigned int *)(0x1000F638)) = ((*(volatile unsigned int *)(0x1000F638)) & 0xf8ffffff) | 0x03000000;

	freq = mt_get_pll_freq(0x33);
        mcSHOW_DBG_MSG3(("CHA PLL4 output=%d\n", freq));
	if ( (freq < DOWN_LIMIT) || (freq > TOP_LIMIT))
	{
		unsigned int AfterReset = 0;
		mcSHOW_DBG_MSG3(("PLL out of range...\n"));

CHA_PLL4_METER:

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff83ffff) | 0x00640000;
		freq = mt_get_pll_freq(0x33);
		mcSHOW_DBG_MSG3(("CHA PLL4 output=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff83ffff) | 0x005c0000;
		freq = mt_get_pll_freq(0x33);
		mcSHOW_DBG_MSG3(("CHA PLL4 feedback=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff87ffff) | 0x00400000;
		freq = mt_get_pll_freq(0x33);
		mcSHOW_DBG_MSG3(("CHA PLL4 REFCLK=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F61c)) = ((*(volatile unsigned int *)(0x1000F61c)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F628)) = ((*(volatile unsigned int *)(0x1000F628)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F634)) = ((*(volatile unsigned int *)(0x1000F634)) & 0xff87ffff) | 0x00480000;
		freq = mt_get_pll_freq(0x33);
		mcSHOW_DBG_MSG3(("CHA PLL4 FBKCLK=%d\n", freq));

	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(20);

		if (AfterReset == 0)
		{
			AfterReset = 1;
			goto CHA_PLL4_METER;
		}

		while(1);
	}

#if 0
	//Select meter clock input
	//Set 0x10000100[31:0] = 0x00003100 //CLK_CFG_8[14:8]=0x30 (AD_MEMPLL2_MONCLK)
	(*(volatile unsigned int *)(0x10000100)) = 0x00003300;

	(*(volatile unsigned int *)(0x10000220)) = 0x00000080;

	//enable meter
	//Set 0x10000214[31:0]=0x00000000;
	(*(volatile unsigned int *)(0x10000214)) = 0xffffff00;

	//Trigger freq meter
	//Set 0x10000220[31:0] = 0x00000081 //CLK26CALI_0[0] = 0x1
	(*(volatile unsigned int *)(0x10000220)) = 0x00000081;

	mcDELAY_US(10);

	//Read meter count
	//cal_cnt = 0x10000224[15:0] //CLK26CALI_1[15:0]
	cal_cnt = (*(volatile unsigned int *)(0x10000224)) & 0x0000ffff;
	//Calculate measured freq. freq = (26000 * cal_cnt) / 1024;
	freq = (0x6590 * cal_cnt) / 0x400;
        mcSHOW_DBG_MSG3(("CHA PLL4 output=%d\n", freq));
	if ( (freq < 760000) || (freq > 820000))
	{
	      mcSHOW_DBG_MSG3(("PLL out of range...\n"));
	      //while(1);
	}
#endif

	//CHB PLL2:
	//monitor enable
	//; Set 0x10012614 //RG_MEMPLL2_MONREF_EN=1
	(*(volatile unsigned int *)(0x10012614)) = ((*(volatile unsigned int *)(0x10012614)) & 0xfffffffd) | 0x00000002;
	//; Set 0x1001261c[31:0]=0x 0062 1401; //RG_MEMPLL2_MON_EN=1, RG_MEMPLL2_CKSEL_MON=100, RG_MEMPLL2_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff83ffff) | 0x00640000;
	//; Set 0x10012620 //RG_MEMPLL3_MONREF_EN=1
	(*(volatile unsigned int *)(0x10012620)) = ((*(volatile unsigned int *)(0x10012620)) & 0xfffffffd) | 0x00000002;
	//; Set 0x1000F628[31:0]=0x 0062 1401; //RG_MEMPLL3_MON_EN=1, RG_MEMPLL3_CKSEL_MON=100, RG_MEMPLL3_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff83ffff) | 0x00640000;
	//; Set 0x1001262c //RG_MEMPLL4_MONREF_EN=1
	(*(volatile unsigned int *)(0x1001262c)) = ((*(volatile unsigned int *)(0x1001262c)) & 0xfffffffd) | 0x00000002;
	//;Set 0x10012634[31:0]=0x 0060 1401; //RG_MEMPLL4_MON_EN=1, RG_MEMPLL4_CKSEL_MON=100, RG_MEMPLL4_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff83ffff) | 0x00640000;
	//;Set 0X1001260C[31:0]=0X 0000 1BC0; //RG_MEMPLL_TEST[5:4]=01,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11;
	(*(volatile unsigned int *)(0x1001260C)) = 0x00001BC0;
	//; Set 0X10012638[31:0]= 0X2300 0000; //RG_MEMPLL_TEST_DIV2_SEL=011, /8;
	(*(volatile unsigned int *)(0x10012638)) = ((*(volatile unsigned int *)(0x10012638)) & 0xf8ffffff) | 0x03000000;

	freq = mt_get_pll_freq(0x3f);
        mcSHOW_DBG_MSG3(("CHB PLL2 output=%d\n", freq));
	if ( (freq < DOWN_LIMIT) || (freq > TOP_LIMIT))
	{
		unsigned int AfterReset = 0;
		mcSHOW_DBG_MSG3(("PLL out of range...\n"));

CHB_PLL2_METER:

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff83ffff) | 0x00640000;
		freq = mt_get_pll_freq(0x3f);
		mcSHOW_DBG_MSG3(("CHB PLL2 output=%d\n", freq));

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff83ffff) | 0x005c0000;
		freq = mt_get_pll_freq(0x3f);
		mcSHOW_DBG_MSG3(("CHB PLL2 feedback=%d\n", freq));

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff87ffff) | 0x00400000;
		freq = mt_get_pll_freq(0x3f);
		mcSHOW_DBG_MSG3(("CHB PLL2 REFCLK=%d\n", freq));

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff87ffff) | 0x00480000;
		freq = mt_get_pll_freq(0x3f);
		mcSHOW_DBG_MSG3(("CHB PLL2 FBKCLK=%d\n", freq));

	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(20);

		if (AfterReset == 0)
		{
			AfterReset = 1;
			goto CHB_PLL2_METER;
		}

		while(1);
	}

#if 0
	//Select meter clock input
	//; 0x10000100[31:0] = 0x00003E00 //CLK_CFG_8[14:8]=0x3E (AD_MEMPLL_MONCLK2_B)
	(*(volatile unsigned int *)(0x10000100)) = 0x00003f00;

	(*(volatile unsigned int *)(0x10000220)) = 0x00000080;

	//enable meter
	//Set 0x10000214[31:0]=0x00000000;
	(*(volatile unsigned int *)(0x10000214)) = 0xffffff00;

	//Trigger freq meter
	//Set 0x10000220[31:0] = 0x00000081 //CLK26CALI_0[0] = 0x1
	(*(volatile unsigned int *)(0x10000220)) = 0x00000081;

	mcDELAY_US(10);

	//Read meter count
	//cal_cnt = 0x10000224[15:0] //CLK26CALI_1[15:0]
	cal_cnt = (*(volatile unsigned int *)(0x10000224)) & 0x0000ffff;
	//Calculate measured freq. freq = (26000 * cal_cnt) / 1024;
	freq = (0x6590 * cal_cnt) / 0x400;
        mcSHOW_DBG_MSG3(("CHB PLL2 output=%d\n", freq));
	if ( (freq < 760000) || (freq > 820000))
	{
	      mcSHOW_DBG_MSG3(("PLL out of range...\n"));
	      //while(1);
	}
#endif

	//; CHB PLL4:
	//monitor enable
	//; Set 0x10012614 //RG_MEMPLL2_MONREF_EN=1
	(*(volatile unsigned int *)(0x10012614)) = ((*(volatile unsigned int *)(0x10012614)) & 0xfffffffd) | 0x00000002;
	//; Set 0x1001261c[31:0]=0x 0062 1401; //RG_MEMPLL2_MON_EN=1, RG_MEMPLL2_CKSEL_MON=100, RG_MEMPLL2_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff83ffff) | 0x00640000;
	//; Set 0x10012620 //RG_MEMPLL3_MONREF_EN=1
	(*(volatile unsigned int *)(0x10012620)) = ((*(volatile unsigned int *)(0x10012620)) & 0xfffffffd) | 0x00000002;
	//; Set 0x1000F628[31:0]=0x 0062 1401; //RG_MEMPLL3_MON_EN=1, RG_MEMPLL3_CKSEL_MON=100, RG_MEMPLL3_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff83ffff) | 0x00640000;
	//; Set 0x1001262c //RG_MEMPLL4_MONREF_EN=1
	(*(volatile unsigned int *)(0x1001262c)) = ((*(volatile unsigned int *)(0x1001262c)) & 0xfffffffd) | 0x00000002;
	//;Set 0x10012634[31:0]=0x 0060 1401; //RG_MEMPLL4_MON_EN=1, RG_MEMPLL4_CKSEL_MON=100, RG_MEMPLL4_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff83ffff) | 0x00640000;
	//; Set 0X1000F60C[31:0]=0X 0000 3BC0; //RG_MEMPLL_TEST[5:4]=11,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11;
	(*(volatile unsigned int *)(0x1001260C)) = 0x00003BC0;
	//; Set 0X10012638[31:0]= 0X2300 0000; //RG_MEMPLL_TEST_DIV2_SEL=011, /8;
	(*(volatile unsigned int *)(0x10012638)) = ((*(volatile unsigned int *)(0x10012638)) & 0xff87ffff) | 0x00600000;

	freq = mt_get_pll_freq(0x42);
        mcSHOW_DBG_MSG3(("CHB PLL4 output=%d\n", freq));
	if ( (freq < DOWN_LIMIT) || (freq > TOP_LIMIT))
	{
		unsigned int AfterReset = 0;
		mcSHOW_DBG_MSG3(("PLL out of range...\n"));

CHB_PLL4_METER:

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff83ffff) | 0x00640000;
		freq = mt_get_pll_freq(0x42);
		mcSHOW_DBG_MSG3(("CHB PLL4 output=%d\n", freq));

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff83ffff) | 0x005c0000;
		freq = mt_get_pll_freq(0x42);
		mcSHOW_DBG_MSG3(("CHB PLL4 feedback=%d\n", freq));

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff87ffff) | 0x00400000;
		freq = mt_get_pll_freq(0x42);
		mcSHOW_DBG_MSG3(("CHB PLL4 REFCLK=%d\n", freq));

		(*(volatile unsigned int *)(0x1001261c)) = ((*(volatile unsigned int *)(0x1001261c)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x10012628)) = ((*(volatile unsigned int *)(0x10012628)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x10012634)) = ((*(volatile unsigned int *)(0x10012634)) & 0xff87ffff) | 0x00480000;
		freq = mt_get_pll_freq(0x42);
		mcSHOW_DBG_MSG3(("CHB PLL4 FBKCLK=%d\n", freq));

	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(20);

		if (AfterReset == 0)
		{
			AfterReset = 1;
			goto CHB_PLL4_METER;
		}
		while(1);
	}

#if 0
	//Select meter clock input
	//; 0x10000100[31:0] = 0x00003E00 //CLK_CFG_8[14:8]=0x3E (AD_MEMPLL_MONCLK2_B)
	(*(volatile unsigned int *)(0x10000100)) = 0x00004200;

	(*(volatile unsigned int *)(0x10000220)) = 0x00000080;

	//enable meter
	//Set 0x10000214[31:0]=0x00000000;
	(*(volatile unsigned int *)(0x10000214)) = 0xffffff00;

	//Trigger freq meter
	//Set 0x10000220[31:0] = 0x00000081 //CLK26CALI_0[0] = 0x1
	(*(volatile unsigned int *)(0x10000220)) = 0x00000081;

	mcDELAY_US(10);

	//Read meter count
	//cal_cnt = 0x10000224[15:0] //CLK26CALI_1[15:0]
	cal_cnt = (*(volatile unsigned int *)(0x10000224)) & 0x0000ffff;
	//Calculate measured freq. freq = (26000 * cal_cnt) / 1024;
	freq = (0x6590 * cal_cnt) / 0x400;
        mcSHOW_DBG_MSG3(("CHB PLL4 output=%d\n", freq));
	if ( (freq < 760000) || (freq > 820000))
	{
	      mcSHOW_DBG_MSG3(("PLL out of range...\n"));
	      //while(1);
	}
#endif

	//05PHY PLL2:
	//monitor enable
	//; Set 0x1000f664 //RG_MEMPLL2_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000f664)) = ((*(volatile unsigned int *)(0x1000f664)) & 0xfffffffd) | 0x00000002;
	//;0x1000F66c[31:0]=0x 0062 1401; //RG_MEMPLL2_MON_EN=1, RG_MEMPLL2_CKSEL_MON=100, RG_MEMPLL2_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff83ffff) | 0x00640000;
	//; Set 0x1000f670 //RG_MEMPLL3_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000f670)) = ((*(volatile unsigned int *)(0x1000f670)) & 0xfffffffd) | 0x00000002;
	//;0x1000F678[31:0]=0x 0060 1401; //RG_MEMPLL3_MON_EN=1, RG_MEMPLL3_CKSEL_MON=100, RG_MEMPLL3_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff83ffff) | 0x00640000;
	//; Set 0x1000f67c //RG_MEMPLL4_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000f67c)) = ((*(volatile unsigned int *)(0x1000f67c)) & 0xfffffffd) | 0x00000002;
	//; 0x1000F684[31:0]=0x 0062 1401; //RG_MEMPLL4_MON_EN=1, RG_MEMPLL4_CKSEL_MON=100, RG_MEMPLL4_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff83ffff) | 0x00640000;
	//; 0X1000F65C[31:0]=0X 0000 1BC0;//RG_MEMPLL_TEST[5:4]=01,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11;
	(*(volatile unsigned int *)(0X1000F65C)) = 0x00001BC0;
	//;0X1000F688[31:0]= 0X2300 0001;//RG_MEMPLL_TEST_DIV2_SEL=011, /8;
	(*(volatile unsigned int *)(0X1000F688)) = ((*(volatile unsigned int *)(0X1000F688)) & 0xff87ffff) | 0x00600000;

	freq = mt_get_pll_freq(0x4e);
        mcSHOW_DBG_MSG3(("05PHY PLL2 output=%d\n", freq));
	if ( (freq < DOWN_LIMIT) || (freq > TOP_LIMIT))
	{
		unsigned int AfterReset = 0;

		mcSHOW_DBG_MSG3(("PLL out of range...\n"));

PHY05_PLL2_METER:

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff83ffff) | 0x00640000;
		freq = mt_get_pll_freq(0x4e);
		mcSHOW_DBG_MSG3(("05PHY PLL2 output=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff83ffff) | 0x005c0000;
		freq = mt_get_pll_freq(0x4e);
		mcSHOW_DBG_MSG3(("05PHY PLL2 feedback=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff87ffff) | 0x00400000;
		freq = mt_get_pll_freq(0x4e);
		mcSHOW_DBG_MSG3(("05PHY PLL2 REFCLK=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff87ffff) | 0x00480000;
		freq = mt_get_pll_freq(0x4e);
		mcSHOW_DBG_MSG3(("05PHY PLL2 FBKCLK=%d\n", freq));

	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(20);

		if (AfterReset == 0)
		{
			AfterReset = 1;
			goto PHY05_PLL2_METER;
		}

		while(1);
	}

#if 0
	//Select meter clock input
	//; 0x10000100[31:0] = 0x00003E00 //CLK_CFG_8[14:8]=0x3E (AD_MEMPLL_MONCLK2_B)
	(*(volatile unsigned int *)(0x10000100)) = 0x00004E00;

	(*(volatile unsigned int *)(0x10000220)) = 0x00000080;

	//enable meter
	//Set 0x10000214[31:0]=0x00000000;
	(*(volatile unsigned int *)(0x10000214)) = 0xffffff00;

	//Trigger freq meter
	//Set 0x10000220[31:0] = 0x00000081 //CLK26CALI_0[0] = 0x1
	(*(volatile unsigned int *)(0x10000220)) = 0x00000081;

	mcDELAY_US(10);

	//Read meter count
	//cal_cnt = 0x10000224[15:0] //CLK26CALI_1[15:0]
	cal_cnt = (*(volatile unsigned int *)(0x10000224)) & 0x0000ffff;
	//Calculate measured freq. freq = (26000 * cal_cnt) / 1024;
	freq = (0x6590 * cal_cnt) / 0x400;
        mcSHOW_DBG_MSG3(("05PHY PLL2 output=%d\n", freq));
	if ( (freq < 760000) || (freq > 820000))
	{
	      mcSHOW_DBG_MSG3(("PLL out of range...\n"));
	      //while(1);
	}
#endif

	//; 05PHY PLL3:
	//monitor enable
	//; Set 0x1000f664 //RG_MEMPLL2_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000f664)) = ((*(volatile unsigned int *)(0x1000f664)) & 0xfffffffd) | 0x00000002;
	//;0x1000F66c[31:0]=0x 0062 1401; //RG_MEMPLL2_MON_EN=1, RG_MEMPLL2_CKSEL_MON=100, RG_MEMPLL2_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff83ffff) | 0x00640000;
	//; Set 0x1000f670 //RG_MEMPLL3_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000f670)) = ((*(volatile unsigned int *)(0x1000f670)) & 0xfffffffd) | 0x00000002;
	//;0x1000F678[31:0]=0x 0060 1401; //RG_MEMPLL3_MON_EN=1, RG_MEMPLL3_CKSEL_MON=100, RG_MEMPLL3_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff83ffff) | 0x00640000;
	//; Set 0x1000f67c //RG_MEMPLL4_MONREF_EN=1
	(*(volatile unsigned int *)(0x1000f67c)) = ((*(volatile unsigned int *)(0x1000f67c)) & 0xfffffffd) | 0x00000002;
	//; 0x1000F684[31:0]=0x 0062 1401; //RG_MEMPLL4_MON_EN=1, RG_MEMPLL4_CKSEL_MON=100, RG_MEMPLL4_FBCK_MON_EN=1;
	(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff83ffff) | 0x00640000;
	//; 0X1000F65C[31:0]=0X 0000 1BC0;//RG_MEMPLL_TEST[5:4]=01,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11;
	(*(volatile unsigned int *)(0X1000F65C)) = 0x00002BC0;
	//;0X1000F688[31:0]= 0X2300 0001;//RG_MEMPLL_TEST_DIV2_SEL=011, /8;
	(*(volatile unsigned int *)(0X1000F688)) = ((*(volatile unsigned int *)(0X1000F688)) & 0xff87ffff) | 0x00600000;

	freq = mt_get_pll_freq(0x4f);
        mcSHOW_DBG_MSG3(("05PHY PLL3 output=%d\n", freq));
	if ( (freq < DOWN_LIMIT) || (freq > TOP_LIMIT))
	{
		unsigned int AfterReset = 0;
		mcSHOW_DBG_MSG3(("PLL out of range...\n"));

PHY05_PLL3_METER:

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff83ffff) | 0x00640000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff83ffff) | 0x00640000;
		freq = mt_get_pll_freq(0x4f);
		mcSHOW_DBG_MSG3(("05PHY PLL3 output=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff83ffff) | 0x005c0000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff83ffff) | 0x005c0000;
		freq = mt_get_pll_freq(0x4f);
		mcSHOW_DBG_MSG3(("05PHY PLL3 feedback=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff87ffff) | 0x00400000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff87ffff) | 0x00400000;
		freq = mt_get_pll_freq(0x4f);
		mcSHOW_DBG_MSG3(("05PHY PLL3 REFCLK=%d\n", freq));

		(*(volatile unsigned int *)(0x1000F66c)) = ((*(volatile unsigned int *)(0x1000F66c)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F678)) = ((*(volatile unsigned int *)(0x1000F678)) & 0xff87ffff) | 0x00480000;
		(*(volatile unsigned int *)(0x1000F684)) = ((*(volatile unsigned int *)(0x1000F684)) & 0xff87ffff) | 0x00480000;
		freq = mt_get_pll_freq(0x4f);
		mcSHOW_DBG_MSG3(("05PHY PLL3 FBKCLK=%d\n", freq));

	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(20);

		if (AfterReset == 0)
		{
			AfterReset = 1;
			goto PHY05_PLL3_METER;
		}
		while(1);
	}
#if 0
	//Select meter clock input
	//; 0x10000100[31:0] = 0x00003E00 //CLK_CFG_8[14:8]=0x3E (AD_MEMPLL_MONCLK2_B)
	(*(volatile unsigned int *)(0x10000100)) = 0x00004F00;

	(*(volatile unsigned int *)(0x10000220)) = 0x00000080;

	//enable meter
	//Set 0x10000214[31:0]=0x00000000;
	(*(volatile unsigned int *)(0x10000214)) = 0xffffff00;

	//Trigger freq meter
	//Set 0x10000220[31:0] = 0x00000081 //CLK26CALI_0[0] = 0x1
	(*(volatile unsigned int *)(0x10000220)) = 0x00000081;

	mcDELAY_US(10);

	//Read meter count
	//cal_cnt = 0x10000224[15:0] //CLK26CALI_1[15:0]
	cal_cnt = (*(volatile unsigned int *)(0x10000224)) & 0x0000ffff;
	//Calculate measured freq. freq = (26000 * cal_cnt) / 1024;
	freq = (0x6590 * cal_cnt) / 0x400;
        mcSHOW_DBG_MSG3(("05PHY PLL3 output=%d\n", freq));
	if ( (freq < 760000) || (freq > 820000))
	{
	      mcSHOW_DBG_MSG3(("PLL out of range...\n"));
	      //while(1);
	}
#endif

	if (EnableDisableTest)
	{
		// MEMPLL disable...
	       mcSHOW_DBG_MSG3(("Mempll disable...\n"));

	        // MEMPLL*_EN=0
	        p->channel = CHANNEL_A;
	        // MEMPLL2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
	        // MEMPLL4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
	        // MEMPLL05_2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
	        // MEMPLL05_3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
	        // MEMPLL3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
	        // MEMPLL05_4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

	        p->channel = CHANNEL_B;
	        // MEMPLL2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
	        // MEMPLL4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
	        // MEMPLL05_2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
	        // MEMPLL05_3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
	        // MEMPLL3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
	        // MEMPLL05_4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
	        mcCLR_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

		mcDELAY_US(10);

	#ifdef MEMPLLTEST_MPLL_ONOFF
		//0x10209280[6:4]=3'b111   //Default is 3'b100, 3'b111 will cause MPLL clock gating
		(*(volatile unsigned int *)(0x10209280)) |= 0x00000070;
	#endif

	        p->channel = CHANNEL_A;
	        // MEMPLL2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
	        // MEMPLL4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
	        // MEMPLL05_2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
	        // MEMPLL05_3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
	        // MEMPLL3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
	        // MEMPLL05_4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

	        p->channel = CHANNEL_B;
	        // MEMPLL2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
	        // MEMPLL4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
	        // MEMPLL05_2_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
	        // MEMPLL05_3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
	        // MEMPLL3_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
	        // MEMPLL05_4_EN
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
	        mcSET_BIT(u4value, 0);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

	#ifdef MEMPLLTEST_MPLL_ONOFF
		mcDELAY_US(2);
		//0x10209280[6:4]=3'b100 //Resume Default
	//	(*(volatile unsigned int *)(0x10209280)) = ((*(volatile unsigned int *)(0x10209280)) & (~0x00000070)) | 0x00000040;
		(*(volatile unsigned int *)(0x10209280)) = ((*(volatile unsigned int *)(0x10209280)) & (~0x00000070)) | 0x00000000;
	#endif

	#ifdef MEMPLLTEST_POWERON_RESET
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(2);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(13);
	#else
		mcDELAY_US(20);        // Wait settle time.
	#endif

	        mcSHOW_DBG_MSG3(("Mempll enable...\n"));
	}
}

#endif

#if defined(TEST_HIGHFREQ_1466_2_1600) || defined(FREQ_HOPPING_SIM_TEST)

#define TAP_NUM  133
#define SETTLE_TIME	20
void HWGW_Print(DRAMC_CTX_T *p);

void	MempllPCWChange(DRAMC_CTX_T* p, U32 SourceFreq, U32 TargetFreq)
{
	U32 u4value;
	S32 SourcePCW, TargetPCW, Step;
	U32 uiSrcAddr = 0x50000000;

        mcSHOW_DBG_MSG3(("Hopping from %d to %d...\n", SourceFreq, TargetFreq));

        u4value = (*(volatile unsigned int *)(0x10209284));

	SourcePCW = (S32) (u4value & 0x1fffff);
        TargetPCW =  (S32) (SourcePCW * TargetFreq /SourceFreq);

	Step = (TargetPCW - SourcePCW) /TAP_NUM;
	if (Step == 0)
	{
		if (TargetPCW > SourcePCW)
		{
			Step = 1;
		}
		else
		{
			Step = -1;
		}
	}
        while (SourcePCW != TargetPCW)
        {
        	if (TargetPCW > SourcePCW)
        	{
	        	if ((TargetPCW-SourcePCW) < Step)
	        	{
	        		SourcePCW = TargetPCW;
	        	}
	        	else
	        	{
		        	SourcePCW += Step;
	        	}
        	}
        	else
        	{
  	        	if ((SourcePCW-TargetPCW) < (-Step))
	        	{
	        		SourcePCW = TargetPCW;
	        	}
	        	else
	        	{
		        	SourcePCW += Step;
	        	}
        	}
        	p->channel = CHANNEL_A;
		DramcEnterSelfRefresh(p, 1);
        	p->channel = CHANNEL_B;
		DramcEnterSelfRefresh(p, 1);
	        (*(volatile unsigned int *)(0x10209284)) = SourcePCW;
	        (*(volatile unsigned int *)(0x10209284)) = SourcePCW |0x80000000;
	        dsb();
		mcDELAY_US(SETTLE_TIME);
        	p->channel = CHANNEL_A;
		DramcEnterSelfRefresh(p, 0);
        	p->channel = CHANNEL_B;
		DramcEnterSelfRefresh(p, 0);

		//MemPll_InternalLoop_Test(p, 1);
#if defined(DUMMYREAD_DMA) && defined(XTALK_SSO_STRESS)
	        // Dummy read
		if (uiDualRank)
		{
			DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, uiSrcAddr, uiSrcAddr+0x60000000, 0x200, 8, 1, 2);
			DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, uiSrcAddr+0x60000000, uiSrcAddr, 0x200, 8, 1, 2);
		}
		else
		{
			DramcDmaEngine((DRAMC_CTX_T *)p, DMA_OP_READ_WRITE, uiSrcAddr, uiSrcAddr+0x10000000, 0x200, 8, 1, 2);
		}
#else	// DUMMYREAD_DMA
		{
			U32 Count, Temp;

			for (Count = 0; Count<20; Count++)
			{
				 Temp = (*(volatile unsigned int *)(uiSrcAddr + Count));
				if (uiDualRank)
				{
					 Temp = (*(volatile unsigned int *)(uiSrcAddr + 0x60000000 + Count));
				}
			}
		}
#endif	// DUMMYREAD_DMA

		//mcSHOW_DBG_MSG2(("SourcePCW=%xh, TargetPCW=%xh, Reg=%xh.\n", SourcePCW, TargetPCW, (*(volatile unsigned int *)(0x10209284))));
		//HWGW_Print(p);
        }

        // DUAL_FREQ_K needs to be defined because need shuffle table.
	#if defined(FREQ_HOPPING_GW_ADJUST)
	if (SourceFreq < TargetFreq)
	{
		GWRuntimeUpdate_Check(p, TargetFreq, 0);
	}
	else
	{
		GWRuntimeUpdate_Check(p, TargetFreq, 1);
	}
	#endif
}
#endif

//#if defined(MT6795_MODIFICATION) && defined(DUAL_FREQ_K)
#if defined(MT6795_MODIFICATION)

void MemPllSetting(PLL_SETTING* p)
{
    if (p->frequency == 333) // 667Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000EF6F8<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        p->MEMPLL_FBDIV2_6_0 = 0x00000002<<16;   // Edward : ACD request to modify.
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000001<<8;
        p->MEMPLL_IR_3_0 = 0x00000008<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 381) // 763Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000001<<4;
        p->MPLL_SDM_PCW_20_0 = 0x00148c15<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000009 <<16;
        p->MEMPLL_FBDIV2_6_0 = 0x00000004<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000003<<8;
        p->MEMPLL_IR_3_0 = 0x00000008<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 399) // 799Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x0011ee46<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000005 <<16;
        p->MEMPLL_FBDIV2_6_0= 0x00000002<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000003<<8;
        p->MEMPLL_IR_3_0 = 0x00000007<<28;
        p->MEMPLL_IC_3_0 = 0x0000000d<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 400)
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000d7627<<0;  // 800Mbps
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_FBDIV2_6_0 = 0x00000003<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000003<<8;
        p->MEMPLL_IR_3_0 = 0x00000009<<28;
        p->MEMPLL_IC_3_0 = 0x0000000c<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 407)	// 814Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000001<<4;
        p->MPLL_SDM_PCW_20_0 = 0x0015ed5e<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000009<<16;
        p->MEMPLL_FBDIV2_6_0 = 0x00000004<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000002<<8;
        p->MEMPLL_IR_3_0 = 0x0000000d<<28;
        p->MEMPLL_IC_3_0 = 0x0000000f<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 419)	// 838Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000001<<4;
        p->MPLL_SDM_PCW_20_0 = 0x001c3136<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_FBDIV2_6_0 = 0x00000003<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000002<<8;
        p->MEMPLL_IR_3_0 = 0x0000000a<<28;
        p->MEMPLL_IC_3_0 = 0x0000000e<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 533) // only for bring-up, 1066Mbps
    { // change to (1163/2) MHz for simulation model pass, only update here for simplicity
        // MPLL
        //MPLL_POSDIV_2_0 = 0x00000001<<4;
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        //MPLL_SDM_PCW_20_0 = 0x001CB333<<0;
        p->MPLL_SDM_PCW_20_0 = 0x000FA7E0<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000004<<16;
        p->MEMPLL_BR_1_0 = 0x00000002<<10;
        //MEMPLL_BC_1_0 = 0x00000002<<8;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000d<<28;
        //MEMPLL_IC_3_0 = 0x00000007<<8;
        p->MEMPLL_IC_3_0 = 0x00000003<<8;
        p->MEMPLL_BP_3_0 = 0x00000002<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 666) // 1333Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x0011ee46<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000004<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000003<<8;
        p->MEMPLL_IR_3_0 = 0x00000006<<28;
        p->MEMPLL_IC_3_0 = 0x00000007<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 667) // 1335Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000EF9D8<<0;		// Same setting as 1780.
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000001<<8;
        p->MEMPLL_IR_3_0 = 0x00000008<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 685) // 1371.5Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000F6276<<0;		// Same setting as 1600.
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000002<<8;//change BC from 01 -> 10
        p->MEMPLL_IR_3_0 = 0x00000008<<28;
        p->MEMPLL_IC_3_0 = 0x0000000C<<8;//change IC from 0110 -> 1100
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 710) // 1420Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000FEDF2<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000001<<8;
        p->MEMPLL_IR_3_0 = 0x00000008<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 733) // 1466Mbps
    {
    	#if 0
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000e189d<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000006<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000002<<8;
        p->MEMPLL_IR_3_0 = 0x00000008<<28;
        p->MEMPLL_IC_3_0 = 0x0000000c<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
        #endif
        // MPLL
	#if 1
        p->MPLL_POSDIV_2_0 = 0x00000001<<4;
        p->MPLL_SDM_PCW_20_0 = 0x0015ed66<<0;
//        p->MPLL_SDM_PCW_20_0 = 0x0014f0a6<<0;	// 1400Mbps
//        p->MPLL_SDM_PCW_20_0 = 0x00147dc7<<0;	// 1370Mbps
//        p->MPLL_SDM_PCW_20_0 = 0x00143132<<0;	// 1350Mbps
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000008<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000002<<8;
        p->MEMPLL_IR_3_0 = 0x00000006<<28;
        p->MEMPLL_IC_3_0 = 0x00000004<<8;
        p->MEMPLL_BP_3_0 = 0x00000002<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
	#endif
    }
    else if (p->frequency == 800) // 1600Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000d7627<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000003<<8;
        p->MEMPLL_IR_3_0 = 0x00000009<<28;
        p->MEMPLL_IC_3_0 = 0x0000000c<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 839) // 1679Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000001<<4;
        p->MPLL_SDM_PCW_20_0 = 0x00148c15<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x0000000a<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000003<<8;
        p->MEMPLL_IR_3_0 = 0x00000009<<28;
        p->MEMPLL_IC_3_0 = 0x00000007<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 850) // 1700Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000E4D89<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000b<<28;
        p->MEMPLL_IC_3_0 = 0x00000005<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 896) // 1792Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000F13B1<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000c<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 933) // 1866Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000FB31C<<0;  // 1866Mbps
        //MPLL_SDM_PCW_20_0 = 0x0011f220<<0;  // 2133Mbps
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000c<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 1000)
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000ef505<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000008<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000e<<28;
        p->MEMPLL_IC_3_0 = 0x00000007<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 1066)
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000ff3b1<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000008<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000e<<28;
        p->MEMPLL_IC_3_0 = 0x00000007<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 1200) // 2400Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000EAF68<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x0000000a<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000f<<28;
        p->MEMPLL_IC_3_0 = 0x0000000d<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else // highest data rate supported, 1780Mbps
    {
        // MPLL
        p->MPLL_POSDIV_2_0 = 0x00000000<<4;
        p->MPLL_SDM_PCW_20_0 = 0x000EF9D8<<0;
        // MEMPLL
        p->MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        p->MEMPLL_BR_1_0 = 0x00000001<<10;
        p->MEMPLL_BC_1_0 = 0x00000000<<8;
        p->MEMPLL_IR_3_0 = 0x0000000c<<28;
        p->MEMPLL_IC_3_0 = 0x00000006<<8;
        p->MEMPLL_BP_3_0 = 0x00000001<<12;
        p->MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
}

DRAM_STATUS_T MemPllInit(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4value;

    PLL_SETTING High_PllSetting, Low_PllSetting;

    // error handling
    if (!p)
    {
//        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

   // Open feedback clock path for 1 PLL mode.
   (*(volatile unsigned int *)(0x1000f644)) |= (0x01 << 15);
   (*(volatile unsigned int *)(0x10012644)) |= (0x01 << 15);
   (*(volatile unsigned int *)(0x1000f694)) |= (0x01 << 15);

   // E3 ECO item for PLL.
   (*(volatile unsigned int *)(0x1000f694)) |= (0x03 << 26);
   (*(volatile unsigned int *)(0x10012694)) |= (0x03 << 26);

   (*(volatile unsigned int *)(0x1000f640)) |= (0x01 << 14);
   (*(volatile unsigned int *)(0x10012640)) |= (0x01 << 14);

   (*(volatile unsigned int *)(0x1000f690)) |= (0x01 << 26);
   (*(volatile unsigned int *)(0x10012690)) |= (0x01 << 26);

   dsb();


    // frequency-dependent parameters
    High_PllSetting.frequency = p->frequency;
    MemPllSetting(&High_PllSetting);
    Low_PllSetting.frequency = DUAL_FREQ_LOW;
    MemPllSetting(&Low_PllSetting);

    //-------------------------------------------------
    //if (p->channel == CHANNEL_A) // only set once in MPLL
    {
        // RG_MPLL_EN=0 : 0x10209280[0] (disable MPLL first)
        u4value = (*(volatile unsigned int *)(0x10209280));
        mcCLR_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10209280)) = u4value;

        // MPLL config
        // RG_MPLL_MODE_SEL: 0x10209280[16]=1 (from MPLL or XTAL)
        // RG_MPLL_POSDIV[2:0]: 0x10209280[6:4]
        u4value = (*(volatile unsigned int *)(0x10209280));
        mcSET_BIT(u4value, 16);
        u4value = u4value & 0xffffff8f;
        u4value = u4value | High_PllSetting.MPLL_POSDIV_2_0;
        (*(volatile unsigned int *)(0x10209280)) = u4value;

    #ifdef fcMEMPLL_SEAL_RING
        // RG_PLLGP_RESERVE[15]: 0x10209040[31]=1
        u4value = (*(volatile unsigned int *)(0x10209040));
        mcSET_BIT(u4value, 31);
        (*(volatile unsigned int *)(0x10209040)) = u4value;
    #else // CTS
        // RG_PLLGP_RESERVE[15]: 0x10209040[31]=0
        u4value = (*(volatile unsigned int *)(0x10209040));
        mcCLR_BIT(u4value, 31);
        (*(volatile unsigned int *)(0x10209040)) = u4value;
    #endif

        // RG_MPLL_SDM_PCW[30:10]: 0x10209284[20:0] (only 21 bits to registers)
        u4value = (*(volatile unsigned int *)(0x10209284));
        u4value = u4value & 0xffe00000;
        u4value = u4value | High_PllSetting.MPLL_SDM_PCW_20_0;
        (*(volatile unsigned int *)(0x10209284)) = u4value;
    }
    //-------------------------------------------------

    if (p->package == PACKAGE_POP)
    {
        //=================== MEMPLL IP Part =======================
        // mempll config:
        // set to seal ring
        // [29]RG_MEMPLL_REFCK_MODE_SEL -> 1: seal-ring, 0: cts
        // [28]RG_MEMPLL_REFCK_SEL=0
        // [0]RG_MEMPLL_REFCK_EN=0 (only 0x688)
        p->channel = CHANNEL_A;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x20000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x20000000);
    #else // CTS
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x00000000);
    #endif

        p->channel = CHANNEL_B;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x20000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x20000000);
    #else // CTS
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x00000000);
    #endif

	// MEMPLL_BIAS_EN=0 : 0x60c[6]/0x65c[6]
	// MEMPLL_BIAS_LPF_EN=0 : 0x60c[7]/0x65c[7]
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000000);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000000);


        // RG_MEMPLL_REFCK_BYPASS (RG_MEMPLL_RESERVE[3]) = 0, only for Rome
        // Bypass MEMPLL1 to reduce Conduit reference clock skew
        // (HW default value) 0x610[27]=0, 0x660[27]=0

        // mempll 2 config
        // MEMPLL2_AUTOK_EN=1, MEMPLL2_AUTOK_LOAD=1
        // 0x614[25:24]=11;//RG_MEMPLL2_FBKSEL[1:0]=11,  /4;
        // 0x614[22:16];//RG_MEMPLL2_FBDIV;
        // 0X614[31:28];//RG_MEMPLL2_IR[3:0];
        // 0x614[15:14]; //RG_MEMPLL2_PREDIV=0:/1, 1:/2
        // 0X614[11:8];//RG_MEMPLL2_IC[3:0];
        // 0x618[25]=0;//RG_MEMPLL2_FB_MCK_SEL=0;
        // 0X61c[17]=1;//RG_MEMPLL2_FBDIV2_EN=1;
        // 0X61c[11:10];//RG_MEMPLL2_BR[1:0];
        // 0x61c[9:8];//RG_MEMPLL2_BC[1:0];
        // 0X61c[15:12];//RG_MEMPLL2_BP[3:0];
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x61c), 0x00020001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll 4 config
        // 0X634[17]=0;//RG_MEMPLL4_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x634), 0x00000001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll05 2  config
        // 0X66c[17]=1;//RG_MEMPLL05_2_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x66c), 0x00020001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll05 3 config
        // 0X678[17]=0;//RG_MEMPLL05_3_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x678), 0x00000001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll 3
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X628[17]=1;//RG_MEMPLL3_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), 0x07800000 | Low_PllSetting.MEMPLL_IR_3_0 | Low_PllSetting.MEMPLL_FBDIV_6_0 | Low_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), 0x4c00c000 | Low_PllSetting.MEMPLL_M4PDIV_1_0);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), 0x00020001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), 0x00000001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);

        // mempll05 4
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X684[17]=1;//RG_MEMPLL05_4_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), 0x07800000 | Low_PllSetting.MEMPLL_IR_3_0 | Low_PllSetting.MEMPLL_FBDIV_6_0 | Low_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), 0x4c00c000 | Low_PllSetting.MEMPLL_M4PDIV_1_0);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x684), 0x00020001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x684), 0x00000001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x61c), 0x00020001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll 4 config
        // 0X634[17]=0;//RG_MEMPLL4_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x634), 0x00000001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll05 2  config
        // 0X66c[17]=1;//RG_MEMPLL05_2_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x66c), 0x00020001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll05 3 config
        // 0X678[17]=0;//RG_MEMPLL05_3_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), 0x07800000 | High_PllSetting.MEMPLL_IR_3_0 | High_PllSetting.MEMPLL_FBDIV_6_0 | High_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), 0x4c00c000 | High_PllSetting.MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x678), 0x00000001 | High_PllSetting.MEMPLL_BP_3_0 | High_PllSetting.MEMPLL_BR_1_0 | High_PllSetting.MEMPLL_BC_1_0);

        // mempll 3
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X628[17]=1;//RG_MEMPLL3_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), 0x07800000 | Low_PllSetting.MEMPLL_IR_3_0 | Low_PllSetting.MEMPLL_FBDIV_6_0 | Low_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), 0x4c00c000 | Low_PllSetting.MEMPLL_M4PDIV_1_0);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), 0x00020001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), 0x00000001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);

        // mempll05 4
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X684[17]=1;//RG_MEMPLL05_4_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), 0x07800000 | Low_PllSetting.MEMPLL_IR_3_0 | Low_PllSetting.MEMPLL_FBDIV_6_0 | Low_PllSetting.MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), 0x4c00c000 | Low_PllSetting.MEMPLL_M4PDIV_1_0);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x684), 0x00020001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x684), 0x00000001 | Low_PllSetting.MEMPLL_BP_3_0 | Low_PllSetting.MEMPLL_BR_1_0 | Low_PllSetting.MEMPLL_BC_1_0);

        // wait 100ns (after DA_MPLL_SDM_ISO_EN goes LOW)
        mcDELAY_US(1);

        // Power up sequence starts here...
        // MPLL_EN=1
        //if (p->channel == CHANNEL_A) // only set once in MPLL
        {
            // RG_MPLL_EN=1 : 0x10209280[0]
            u4value = (*(volatile unsigned int *)(0x10209280));
            mcSET_BIT(u4value, 0);
            (*(volatile unsigned int *)(0x10209280)) = u4value;
        }

        // MEMPLL_REFCK_EN=1 : 0x688[0] (only one in chip, seal-ring buffer enable)
        p->channel = CHANNEL_A;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #else // CTS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #endif

        p->channel = CHANNEL_B;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #else // CTS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #endif

        // wait 100us
        mcDELAY_US(100);

        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_EN=1 : 0x60c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000040);
        // MEMPLL_BIAS05_EN = 1 : 0x65c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000040);

        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_EN=1 : 0x60c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000040);
        // MEMPLL_BIAS05_EN = 1 : 0x65c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000040);

        // wait 2us
        mcDELAY_US(2);

        // MEMPLL*_EN=1
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);

        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);

        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        // wait 100us
        mcDELAY_US(100);

        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_LPF_EN=1 : 0x60c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        // MEMPLL05_BIAS_LPF_EN=1 : 0x65c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x65c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_LPF_EN=1 : 0x60c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        // MEMPLL05_BIAS_LPF_EN=1 : 0x65c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x65c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), u4value);

        // wait 20us or 30us
        mcDELAY_US(30);

        // check VCO K status and band
        p->channel = CHANNEL_A;
        mcSHOW_DBG_MSG3(("MEMPLL AUTOK status log...channel=%d\n", p->channel));
        mcSHOW_DBG_MSG3(("0x3e0 (MEMPLL 2/3/4 status), 0x3e4 (MEMPLL05 2/3/4 status)\n"));
        mcSHOW_DBG_MSG3(("[29]/[27]/[25] : MEMPLL 2/3/4_AUTOK_PASS\n"));
        mcSHOW_DBG_MSG3(("[22:16]/[14:8] : MEMPLL 2/3_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
	VCOK_Cha_Mempll2 = (u4value >> 16) & 0x7f;
	VCOK_Cha_Mempll3 = (u4value >> 8) & 0x7f;
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Cha_Mempll2=0x%x, VCOK_Cha_Mempll3=0x%x \n",
        	u4value, VCOK_Cha_Mempll2, VCOK_Cha_Mempll3));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e4), &u4value);
        VCOK_05PHY_Mempll2 = (u4value >> 16) & 0x7f;
        VCOK_05PHY_Mempll3 = (u4value >> 8) & 0x7f;
        mcSHOW_DBG_MSG3(("0x3e4=0x%x, VCOK_05PHY_Mempll2=0x%x, VCOK_05PHY_Mempll3=0x%x\n",
        	u4value, VCOK_05PHY_Mempll2, VCOK_05PHY_Mempll3));

        // RG_MEMPLL_RESERVE[2]=1, to select MEMPLL4 band register
        // RGS_MEMPLL4_AUTOK_BAND[6:0]= RGS_MEMPLL4_AUTOK_BAND[6]+RGS_MEMPLL3_AUTOK_BAND[5:0]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);

        mcSHOW_DBG_MSG3(("[6]+[13:8] : MEMPLL 4_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
        VCOK_Cha_Mempll4 = ((u4value >> 8) & 0x3f) | (u4value & 0x40);
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Cha_Mempll4=0x%x\n", u4value, VCOK_Cha_Mempll4));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e4), &u4value);
        VCOK_05PHY_Mempll4 = ((u4value >> 8) & 0x3f) | (u4value & 0x40);
        mcSHOW_DBG_MSG3(("0x3e4=0x%x, VCOK_05PHY_Mempll4=0x%x\n", u4value, VCOK_05PHY_Mempll4));

        // RG_MEMPLL_RESERVE[2]=0, recover back
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcCLR_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);

        p->channel = CHANNEL_B;
        mcSHOW_DBG_MSG3(("MEMPLL AUTOK status log...channel=%d\n", p->channel));
        mcSHOW_DBG_MSG3(("0x3e0 (MEMPLL 2/3/4 status), 0x3e4 (MEMPLL05 2/3/4 status)\n"));
        mcSHOW_DBG_MSG3(("[29]/[27]/[25] : MEMPLL 2/3/4_AUTOK_PASS\n"));
        mcSHOW_DBG_MSG3(("[14:8]/[6:0] : MEMPLL 3/4_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
        VCOK_Chb_Mempll3 = (u4value >> 8) & 0x7f;
        VCOK_Chb_Mempll4 = u4value & 0x7f;
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Chb_Mempll3=0x%x, VCOK_Chb_Mempll4=0x%x\n",
        	u4value, VCOK_Chb_Mempll3, VCOK_Chb_Mempll4));

        // RG_MEMPLL_RESERVE[2]=1, to select MEMPLL4 band register
        // RGS_MEMPLL4_AUTOK_BAND[6:0]= RGS_MEMPLL4_AUTOK_BAND[6]+RGS_MEMPLL3_AUTOK_BAND[5:0]
        // channel B mempll2 <-> mempll4 (HW and register have swap)
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);
        mcSHOW_DBG_MSG3(("[6]+[13:8] : MEMPLL 2_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
	VCOK_Chb_Mempll2 = ((u4value >> 8) & 0x3f) | (u4value & 0x40);
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Chb_Mempll2=0x%x\n", u4value, VCOK_Chb_Mempll2));

        // RG_MEMPLL_RESERVE[2]=0, recover back
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcCLR_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);

        //ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e4), &u4value);
        //mcSHOW_DBG_MSG3(("0x3e4=0x%x\n", u4value));

        // PLL ready (internal loop) <--
#ifdef MEMPLL_INTERNAL_LOOP_TEST
      mcSHOW_DBG_MSG3(("PLL frequency = %d...\n", p->frequency));

	if (p->frequency == 800)
	{
		while (1)
		{
			MemPll_InternalLoop_Test(p, 1);
		}
	}
#endif

#if 1

        // MEMPLL*_EN=0
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        // MEMPLL*_AUTOK_EN=0
        p->channel = CHANNEL_A;
        // MEMPLL2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        // Need to MODIFY MEMPLL FBDIV here for some low data rate
//        if ((p->frequency == 333) || (p->frequency == 400) )
        if (High_PllSetting.MEMPLL_M4PDIV_1_0)
        {
            p->channel = CHANNEL_A;
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);

            p->channel = CHANNEL_B;
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | High_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        }

        if (Low_PllSetting.MEMPLL_M4PDIV_1_0)
        {
            p->channel = CHANNEL_A;
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | Low_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | Low_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

            p->channel = CHANNEL_B;
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | Low_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | Low_PllSetting.MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);
        }

        // wait 1us
        mcDELAY_US(1);

        // MEMPLL*_FB_MCK_SEL=1 (switch to outer loop)
        p->channel = CHANNEL_A;
        // MEMPLL2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);
        // MEMPLL4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);
        // MEMPLL05_2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
        // MEMPLL05_3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
        // MEMPLL3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x624), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), u4value);
        // MEMPLL05_4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x680), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);
        // MEMPLL4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);
        // MEMPLL05_2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
        // MEMPLL05_3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
        // MEMPLL3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x624), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), u4value);
        // MEMPLL05_4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x680), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), u4value);

        // wait 100ns
        mcDELAY_US(1);

        // MEMPLL*_EN=1
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        #ifdef CHA_MEMPLL3_DISABLE
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x620))) &= ~(0x01<<0);
        #endif

	#ifdef CHB_MEMPLL3_PHY05_MEMPLL4_DISABLE
	// (a) 0x1000F690[28]P0x10012690[28]SBS PLL ECO enable register
	// ]1, b}pop enable (0x1000F640[16]P0x10012640[16]), |SΨ쪺SBS PLL.
	// 0x1000F690[28]P0x10012690[28]]0, SBS PLL enableE1 function.
	// (b) 0x1000F690[29] POP PLL ECO enable register
	// ]1, |SΨ쪺POP PLL.
	// 0x1000F690[29]]0, POP PLL enableE1 function.
		(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x690))) |= ((0x01<<28) |(0x01<<29));
		(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x690))) |= (0x01<<28);
	#endif

#ifdef MEMPLL_NEW_POWERON
	*(volatile unsigned int*)(0x10006000) = 0x0b160001;
	*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
	mcDELAY_US(2);
	*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
//	mcDELAY_US(13);
	mcDELAY_US(30);
#else
        // wait 20us or 30us
        mcDELAY_US(30);
#endif

        // PLL ready (external loop) <--
#ifdef MEMPLL_EXTERNAL_LOOP_TEST
      mcSHOW_DBG_MSG3(("PLL frequency = %d...\n", p->frequency));

	if (p->frequency == 800)
	{
		while (1)
		{
			MemPll_InternalLoop_Test(p, 1);
		}
	}
#endif


    #endif


        // wait 1us
        mcDELAY_US(1);
    }
    else // SBS
    {
        // TBD
    }

#ifdef fcMEMPLL_DBG_MONITOR
    // delay 20us
    mcDELAY_US(20);

    // Monitor enable
    // RG_MEMPLL*_MONCK_EN=1; RG_MEMPLL*_MONREF=1
    // PLL2
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
    mcSET_BIT(u4value, 3);
    mcSET_BIT(u4value, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
    // PLL3
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
    mcSET_BIT(u4value, 3);
    mcSET_BIT(u4value, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
    // PLL4
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
    mcSET_BIT(u4value, 3);
    mcSET_BIT(u4value, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

    // 0x61c[22] RG_MEMPLL2_MON_EN=1, 0x61c[21:19] RG_MEMPLL2_CKSEL_MON=100
    // RG_MEMPLL2_MON_EN -> to enable M5
    // RG_MEMPLL2_CKSEL_MON = 100 (select M5)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x61c), &u4value);
    mcSET_BIT(u4value, 22);
    mcSET_FIELD(u4value, 4, 0x00380000, 19);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x61c), u4value);

    // 0x628[22] RG_MEMPLL3_MON_EN=1, 0x628[21:19] RG_MEMPLL3_CKSEL_MON=100
    // RG_MEMPLL3_MON_EN -> to enable M5
    // RG_MEMPLL3_CKSEL_MON = 100 (select M5)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x628), &u4value);
    mcSET_BIT(u4value, 22);
    mcSET_FIELD(u4value, 4, 0x00380000, 19);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), u4value);

    // 0x634[22] RG_MEMPLL4_MON_EN=1, 0x634[21:19] RG_MEMPLL4_CKSEL_MON=100
    // RG_MEMPLL4_MON_EN -> to enable M5
    // RG_MEMPLL4_CKSEL_MON = 100 (select M5)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x634), &u4value);
    mcSET_BIT(u4value, 22);
    mcSET_FIELD(u4value, 4, 0x00380000, 19);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x634), u4value);

    // 0x60c[23:8] RG_MEMPLL_TEST[15:0];
    // RG_MEMPLL_TEST[5:4]=01,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11
    // RG_MEMPLL_TEST[5:4]=01 -> MEMPLL2
    // RG_MEMPLL_TEST[5:4]=10 -> MEMPLL3
    // RG_MEMPLL_TEST[5:4]=11 -> MEMPLL4
    // RG_MEMPLL_TEST[3,1] -> select CKMUX (measure clock or voltage)
    // RG_MEMPLL_TEST[0] -> RG_A2DCK_EN (for FT)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
    mcSET_FIELD(u4value, 3, 0x00000300, 8);
    mcSET_BIT(u4value, 11);
    mcSET_FIELD(u4value, 1, 0x00003000, 12);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);

/*
    //0x638[26:24] RG_MEMPLL_TEST_DIV2_SEL=011, /8 (for FT)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x638), &u4value);
    mcSET_FIELD(u4value, 3, 0x07000000, 24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), u4value);*/
#endif

#if 1
    // for debug
    u4value = DramOperateDataRate(p, 0);
    mcSHOW_DBG_MSG(("DRAM Operation Data Rate: 2PLL DDR-%d\n", u4value));
    u4value = DramOperateDataRate(p, 1);
    mcSHOW_DBG_MSG(("DRAM Operation Data Rate: 1PLL DDR-%d\n", u4value));
#endif

    if (ucstatus)
    {
//        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }
}

#else

DRAM_STATUS_T MemPllInit(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4value;

    // MPLL frequency-dependent parameters
    U32 MPLL_POSDIV_2_0, MPLL_SDM_PCW_20_0;

    // MEMPLL frequency-dependent parameters
    U32 MEMPLL_FBDIV_6_0, MEMPLL_FBDIV2_6_0, MEMPLL_M4PDIV_1_0;
    U32 MEMPLL_BR_1_0, MEMPLL_BC_1_0, MEMPLL_IR_3_0, MEMPLL_IC_3_0, MEMPLL_BP_3_0;
#if 0
#ifdef SPM_MODIFY
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x1111ff11);
    //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11510011);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11510111);	// Reg.511h[8]=1 for regiser control 640h
#endif
#endif

    // error handling
    if (!p)
    {
//        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

    // frequency-dependent parameters
    if (p->frequency == 533) // only for bring-up, 1066Mbps
    { // change to (1163/2) MHz for simulation model pass, only update here for simplicity
        // MPLL
        //MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        //MPLL_SDM_PCW_20_0 = 0x001CB333<<0;
        MPLL_SDM_PCW_20_0 = 0x000FA7E0<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000004<<16;
        MEMPLL_BR_1_0 = 0x00000002<<10;
        //MEMPLL_BC_1_0 = 0x00000002<<8;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000d<<28;
        //MEMPLL_IC_3_0 = 0x00000007<<8;
        MEMPLL_IC_3_0 = 0x00000003<<8;
        MEMPLL_BP_3_0 = 0x00000002<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 733) // 1466Mbps
    {
    	#if 0
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_SDM_PCW_20_0 = 0x001c3136<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000006<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000002<<8;
        MEMPLL_IR_3_0 = 0x00000009<<28;
        MEMPLL_IC_3_0 = 0x0000000d<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
        #endif
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_SDM_PCW_20_0 = 0x0015ed5e<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000008<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000002<<8;
        MEMPLL_IR_3_0 = 0x0000000c<<28;
        MEMPLL_IC_3_0 = 0x0000000f<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 800) // 1600Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000d7627<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000003<<8;
        MEMPLL_IR_3_0 = 0x00000009<<28;
        MEMPLL_IC_3_0 = 0x0000000c<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 333) // 667Mbps
    {
    /*
        // MPLL
        //MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        //MPLL_SDM_PCW_20_0 = 0x001CB333<<0;
        MPLL_SDM_PCW_20_0 = 0x00140000<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000004<<16;
        MEMPLL_BR_1_0 = 0x00000002<<10;
        //MEMPLL_BC_1_0 = 0x00000002<<8;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000d<<28;
        //MEMPLL_IC_3_0 = 0x00000007<<8;
        MEMPLL_IC_3_0 = 0x00000003<<8;
        MEMPLL_BP_3_0 = 0x00000002<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
        */

        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000EF6F8<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        MEMPLL_FBDIV2_6_0 = 0x00000002<<16;   // Edward : ACD request to modify.
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000001<<8;
        MEMPLL_IR_3_0 = 0x00000008<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 381) // 763Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_SDM_PCW_20_0 = 0x00148c15<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000009 <<16;
        MEMPLL_FBDIV2_6_0= 0x00000004<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000003<<8;
        MEMPLL_IR_3_0 = 0x00000008<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 399) // 799Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x0011ee46<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000005 <<16;
        MEMPLL_FBDIV2_6_0= 0x00000002<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000003<<8;
        MEMPLL_IR_3_0 = 0x00000007<<28;
        MEMPLL_IC_3_0 = 0x0000000d<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 666) // 1333Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x0011ee46<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000004<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000003<<8;
        MEMPLL_IR_3_0 = 0x00000006<<28;
        MEMPLL_IC_3_0 = 0x00000007<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 667) // 1335Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000EF9D8<<0;		// Same setting as 1780.
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000001<<8;
        MEMPLL_IR_3_0 = 0x00000008<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 685) // 1371.5Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000F6276<<0;		// Same setting as 1600.
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000002<<8;//change BC from 01 -> 10
        MEMPLL_IR_3_0 = 0x00000008<<28;
        MEMPLL_IC_3_0 = 0x0000000C<<8;//change IC from 0110 -> 1100
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 933) // 1866Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000FB31C<<0;  // 1866Mbps
        //MPLL_SDM_PCW_20_0 = 0x0011f220<<0;  // 2133Mbps
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000c<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 419)	// 838Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_SDM_PCW_20_0 = 0x001c3136<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_FBDIV2_6_0 = 0x00000003<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000002<<8;
        MEMPLL_IR_3_0 = 0x0000000a<<28;
        MEMPLL_IC_3_0 = 0x0000000e<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 407)	// 814Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_SDM_PCW_20_0 = 0x0015ed5e<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000009<<16;
        MEMPLL_FBDIV2_6_0 = 0x00000004<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000002<<8;
        MEMPLL_IR_3_0 = 0x0000000d<<28;
        MEMPLL_IC_3_0 = 0x0000000f<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 400)
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000d7627<<0;  // 800Mbps
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_FBDIV2_6_0 = 0x00000003<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000003<<8;
        MEMPLL_IR_3_0 = 0x00000009<<28;
        MEMPLL_IC_3_0 = 0x0000000c<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000001<<28;
    }
    else if (p->frequency == 1000)
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000ef505<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000008<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000e<<28;
        MEMPLL_IC_3_0 = 0x00000007<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 1066)
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000ff3b1<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000008<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000e<<28;
        MEMPLL_IC_3_0 = 0x00000007<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 710) // 1420Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000FEDF2<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000005<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000001<<8;
        MEMPLL_IR_3_0 = 0x00000008<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 1200) // 2400Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000EAF68<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x0000000a<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000f<<28;
        MEMPLL_IC_3_0 = 0x0000000d<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 896) // 1792Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000F13B1<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000c<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 839) // 1679Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000001<<4;
        MPLL_SDM_PCW_20_0 = 0x00148c15<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x0000000a<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000003<<8;
        MEMPLL_IR_3_0 = 0x00000009<<28;
        MEMPLL_IC_3_0 = 0x00000007<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else if (p->frequency == 850) // 1700Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000E4D89<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000b<<28;
        MEMPLL_IC_3_0 = 0x00000005<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }
    else // highest data rate supported, 1780Mbps
    {
        // MPLL
        MPLL_POSDIV_2_0 = 0x00000000<<4;
        MPLL_SDM_PCW_20_0 = 0x000EF9D8<<0;
        // MEMPLL
        MEMPLL_FBDIV_6_0 = 0x00000007<<16;
        MEMPLL_BR_1_0 = 0x00000001<<10;
        MEMPLL_BC_1_0 = 0x00000000<<8;
        MEMPLL_IR_3_0 = 0x0000000c<<28;
        MEMPLL_IC_3_0 = 0x00000006<<8;
        MEMPLL_BP_3_0 = 0x00000001<<12;
        MEMPLL_M4PDIV_1_0 = 0x00000000<<28;
    }

    //-------------------------------------------------
    //if (p->channel == CHANNEL_A) // only set once in MPLL
    {
        // RG_MPLL_EN=0 : 0x10209280[0] (disable MPLL first)
        u4value = (*(volatile unsigned int *)(0x10209280));
        mcCLR_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10209280)) = u4value;

        // MPLL config
        // RG_MPLL_MODE_SEL: 0x10209280[16]=1 (from MPLL or XTAL)
        // RG_MPLL_POSDIV[2:0]: 0x10209280[6:4]
        u4value = (*(volatile unsigned int *)(0x10209280));
        mcSET_BIT(u4value, 16);
        u4value = u4value & 0xffffff8f;
        u4value = u4value | MPLL_POSDIV_2_0;
        (*(volatile unsigned int *)(0x10209280)) = u4value;

    #ifdef fcMEMPLL_SEAL_RING
        // RG_PLLGP_RESERVE[15]: 0x10209040[31]=1
        u4value = (*(volatile unsigned int *)(0x10209040));
        mcSET_BIT(u4value, 31);
        (*(volatile unsigned int *)(0x10209040)) = u4value;
    #else // CTS
        // RG_PLLGP_RESERVE[15]: 0x10209040[31]=0
        u4value = (*(volatile unsigned int *)(0x10209040));
        mcCLR_BIT(u4value, 31);
        (*(volatile unsigned int *)(0x10209040)) = u4value;
    #endif

        // RG_MPLL_SDM_PCW[30:10]: 0x10209284[20:0] (only 21 bits to registers)
        u4value = (*(volatile unsigned int *)(0x10209284));
        u4value = u4value & 0xffe00000;
        u4value = u4value | MPLL_SDM_PCW_20_0;
        (*(volatile unsigned int *)(0x10209284)) = u4value;
    }
    //-------------------------------------------------

    if (p->package == PACKAGE_POP)
    {
    #if 0
        //=================== Integration Part (TOP) =======================
        // Enable MTCMOS power ack switch before ACK_POP_BYP and ACK_SBS_BYP to avoid glitch
        // LDO_PHY2POP_NDIS=1 is better from LDO designer
        // Default: 0x09000000, [15]ACK_SWITCH, [2]MEMCLKENSYNC_SOURCE
        p->channel = CHANNEL_A;
#ifdef SPM_MODIFY
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008024);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008004);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), 0x09008004);

        p->channel = CHANNEL_B;
#ifdef SPM_MODIFY
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008024);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09008004);
#endif

#if 1
        // ALLCLK_EN
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        // Disable gating function
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
        mcCLR_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
        mcCLR_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);

//#else

        // BYPASS MTCMOS power ack and turn off power for side-by-side PHY
        // [16]ACK_SBS_BYP
        /*
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        mcSET_BIT(u4value, 16);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value); //edward remark

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);*/

        // Disable MTCMOS power ack switch
        // [15]ACK_SWITCH

        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcCLR_BIT(u4value, 15);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
        //ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        //mcCLR_BIT(u4value, 15);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value); // edward remark

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcCLR_BIT(u4value, 15);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

#endif

        // DRAMC 2X mode
        // [0]FDIV2=1
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x07c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x07c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), u4value);

        // Delay counters for SPM power-up MEMPLL
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c0), 0x21271b03);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c4), 0x5096061e);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c0), 0x21271b03);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c4), 0x5096061e);

        // [0][8][11][16][24]select source from register or SPM
        // [12]bypass counter delay chain
#ifndef SPM_MODIFY
       p->channel = CHANNEL_A;
       ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x01011101);

       p->channel = CHANNEL_B;
       ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x01011101);
#endif

        // [27]LDO_PHY2POP_NDIS, [24]LDO_PHY2POP_EN,
        // [7]MEMCLKENMODE, [3]MEMCLKEN_SEL, [0]MEMCLKENB_SEL
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        //mcSET_BIT(u4value, 27); // LDO_PHY2POP_NDIS=1 is better from LDO designer, has set before
 //   #ifdef fcMEMPLL_SEAL_RING
        mcSET_BIT(u4value, 24);
 //   #else
 //       mcCLR_BIT(u4value, 24);
 //   #endif
        mcSET_BIT(u4value, 7);
        mcCLR_BIT(u4value, 3); // must clear to select external source to get correct phase for channel A
        //mcCLR_BIT(u4value, 2); // set channel A & 05PHY to select external source (from channel B)
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        //mcSET_BIT(u4value, 27); // LDO_PHY2POP_NDIS=1 is better from LDO designer, has set before
    //#ifdef fcMEMPLL_SEAL_RING
    //    mcSET_BIT(u4value, 24);
    //#else
    //    mcCLR_BIT(u4value, 24);
    //#endif
    //    mcSET_BIT(u4value, 7);
        mcCLR_BIT(u4value, 3);
        //mcCLR_BIT(u4value, 2);
    // mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        //mcSET_BIT(u4value, 27); // LDO_PHY2POP_NDIS=1 is better from LDO designer, has set before
 //   #ifdef fcMEMPLL_SEAL_RING
        mcSET_BIT(u4value, 24);
 //   #else
 //       mcCLR_BIT(u4value, 24);
 //   #endif
        mcSET_BIT(u4value, 7);
        //mcCLR_BIT(u4value, 3); // no use for channel B
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
    #endif

        //=================== MEMPLL IP Part =======================
        // mempll config:
        // set to seal ring
        // [29]RG_MEMPLL_REFCK_MODE_SEL -> 1: seal-ring, 0: cts
        // [28]RG_MEMPLL_REFCK_SEL=0
        // [0]RG_MEMPLL_REFCK_EN=0 (only 0x688)
        p->channel = CHANNEL_A;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x20000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x20000000);
    #else // CTS
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x00000000);
    #endif

        p->channel = CHANNEL_B;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x20000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x20000000);
    #else // CTS
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), 0x00000000);
    #endif

	// MEMPLL_BIAS_EN=0 : 0x60c[6]/0x65c[6]
	// MEMPLL_BIAS_LPF_EN=0 : 0x60c[7]/0x65c[7]
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000000);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000000);


        // RG_MEMPLL_REFCK_BYPASS (RG_MEMPLL_RESERVE[3]) = 0, only for Rome
        // Bypass MEMPLL1 to reduce Conduit reference clock skew
        // (HW default value) 0x610[27]=0, 0x660[27]=0

        // mempll 2 config
        // MEMPLL2_AUTOK_EN=1, MEMPLL2_AUTOK_LOAD=1
        // 0x614[25:24]=11;//RG_MEMPLL2_FBKSEL[1:0]=11,  /4;
        // 0x614[22:16];//RG_MEMPLL2_FBDIV;
        // 0X614[31:28];//RG_MEMPLL2_IR[3:0];
        // 0x614[15:14]; //RG_MEMPLL2_PREDIV=0:/1, 1:/2
        // 0X614[11:8];//RG_MEMPLL2_IC[3:0];
        // 0x618[25]=0;//RG_MEMPLL2_FB_MCK_SEL=0;
        // 0X61c[17]=1;//RG_MEMPLL2_FBDIV2_EN=1;
        // 0X61c[11:10];//RG_MEMPLL2_BR[1:0];
        // 0x61c[9:8];//RG_MEMPLL2_BC[1:0];
        // 0X61c[15:12];//RG_MEMPLL2_BP[3:0];
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x61c), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll 4 config
        // 0X634[17]=0;//RG_MEMPLL4_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x634), 0x00000001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll05 2  config
        // 0X66c[17]=1;//RG_MEMPLL05_2_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x66c), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll05 3 config
        // 0X678[17]=0;//RG_MEMPLL05_3_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x678), 0x00000001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll 3
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X628[17]=1;//RG_MEMPLL3_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll05 4
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X684[17]=1;//RG_MEMPLL05_4_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x684), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x61c), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll 4 config
        // 0X634[17]=0;//RG_MEMPLL4_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x634), 0x00000001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll05 2  config
        // 0X66c[17]=1;//RG_MEMPLL05_2_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x66c), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll05 3 config
        // 0X678[17]=0;//RG_MEMPLL05_3_FBDIV2_EN=0;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x678), 0x00000001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll 3
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X628[17]=1;//RG_MEMPLL3_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // mempll05 4
        // (Enable signal tie together. Sim error due to unknown dividor. E2 will fix.)
        // 0X684[17]=1;//RG_MEMPLL05_4_FBDIV2_EN=1;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), 0x07800000 | MEMPLL_IR_3_0 | MEMPLL_FBDIV_6_0 | MEMPLL_IC_3_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), 0x4c00c000 | MEMPLL_M4PDIV_1_0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x684), 0x00020001 | MEMPLL_BP_3_0 | MEMPLL_BR_1_0 | MEMPLL_BC_1_0);

        // wait 100ns (after DA_MPLL_SDM_ISO_EN goes LOW)
        mcDELAY_US(1);

        // Power up sequence starts here...
        // MPLL_EN=1
        //if (p->channel == CHANNEL_A) // only set once in MPLL
        {
            // RG_MPLL_EN=1 : 0x10209280[0]
            u4value = (*(volatile unsigned int *)(0x10209280));
            mcSET_BIT(u4value, 0);
            (*(volatile unsigned int *)(0x10209280)) = u4value;
        }

        // MEMPLL_REFCK_EN=1 : 0x688[0] (only one in chip, seal-ring buffer enable)
        p->channel = CHANNEL_A;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #else // CTS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #endif

        p->channel = CHANNEL_B;
    #ifdef fcMEMPLL_SEAL_RING
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #else // CTS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x688), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x688), u4value);
    #endif

        // wait 100us
        mcDELAY_US(100);

        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_EN=1 : 0x60c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000040);
        // MEMPLL_BIAS05_EN = 1 : 0x65c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000040);

        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_EN=1 : 0x60c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), 0xd0000040);
        // MEMPLL_BIAS05_EN = 1 : 0x65c[6]
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), 0xd0000040);

        // wait 2us
        mcDELAY_US(2);

        // MEMPLL*_EN=1
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);

        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);

        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        // wait 100us
        mcDELAY_US(100);

        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_LPF_EN=1 : 0x60c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        // MEMPLL05_BIAS_LPF_EN=1 : 0x65c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x65c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_LPF_EN=1 : 0x60c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        // MEMPLL05_BIAS_LPF_EN=1 : 0x65c[7]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x65c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x65c), u4value);

        // wait 20us or 30us
        mcDELAY_US(30);

        // check VCO K status and band
        p->channel = CHANNEL_A;
        mcSHOW_DBG_MSG3(("MEMPLL AUTOK status log...channel=%d\n", p->channel));
        mcSHOW_DBG_MSG3(("0x3e0 (MEMPLL 2/3/4 status), 0x3e4 (MEMPLL05 2/3/4 status)\n"));
        mcSHOW_DBG_MSG3(("[29]/[27]/[25] : MEMPLL 2/3/4_AUTOK_PASS\n"));
        mcSHOW_DBG_MSG3(("[22:16]/[14:8] : MEMPLL 2/3_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
	VCOK_Cha_Mempll2 = (u4value >> 16) & 0x7f;
	VCOK_Cha_Mempll3 = (u4value >> 8) & 0x7f;
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Cha_Mempll2=0x%x, VCOK_Cha_Mempll3=0x%x \n",
        	u4value, VCOK_Cha_Mempll2, VCOK_Cha_Mempll3));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e4), &u4value);
        VCOK_05PHY_Mempll2 = (u4value >> 16) & 0x7f;
        VCOK_05PHY_Mempll3 = (u4value >> 8) & 0x7f;
        mcSHOW_DBG_MSG3(("0x3e4=0x%x, VCOK_05PHY_Mempll2=0x%x, VCOK_05PHY_Mempll3=0x%x\n",
        	u4value, VCOK_05PHY_Mempll2, VCOK_05PHY_Mempll3));

        // RG_MEMPLL_RESERVE[2]=1, to select MEMPLL4 band register
        // RGS_MEMPLL4_AUTOK_BAND[6:0]= RGS_MEMPLL4_AUTOK_BAND[6]+RGS_MEMPLL3_AUTOK_BAND[5:0]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);

        mcSHOW_DBG_MSG3(("[6]+[13:8] : MEMPLL 4_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
        VCOK_Cha_Mempll4 = ((u4value >> 8) & 0x3f) | (u4value & 0x40);
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Cha_Mempll4=0x%x\n", u4value, VCOK_Cha_Mempll4));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e4), &u4value);
        VCOK_05PHY_Mempll4 = ((u4value >> 8) & 0x3f) | (u4value & 0x40);
        mcSHOW_DBG_MSG3(("0x3e4=0x%x, VCOK_05PHY_Mempll4=0x%x\n", u4value, VCOK_05PHY_Mempll4));

        // RG_MEMPLL_RESERVE[2]=0, recover back
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcCLR_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);

        p->channel = CHANNEL_B;
        mcSHOW_DBG_MSG3(("MEMPLL AUTOK status log...channel=%d\n", p->channel));
        mcSHOW_DBG_MSG3(("0x3e0 (MEMPLL 2/3/4 status), 0x3e4 (MEMPLL05 2/3/4 status)\n"));
        mcSHOW_DBG_MSG3(("[29]/[27]/[25] : MEMPLL 2/3/4_AUTOK_PASS\n"));
        mcSHOW_DBG_MSG3(("[14:8]/[6:0] : MEMPLL 3/4_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
        VCOK_Chb_Mempll3 = (u4value >> 8) & 0x7f;
        VCOK_Chb_Mempll4 = u4value & 0x7f;
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Chb_Mempll3=0x%x, VCOK_Chb_Mempll4=0x%x\n",
        	u4value, VCOK_Chb_Mempll3, VCOK_Chb_Mempll4));

        // RG_MEMPLL_RESERVE[2]=1, to select MEMPLL4 band register
        // RGS_MEMPLL4_AUTOK_BAND[6:0]= RGS_MEMPLL4_AUTOK_BAND[6]+RGS_MEMPLL3_AUTOK_BAND[5:0]
        // channel B mempll2 <-> mempll4 (HW and register have swap)
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);
        mcSHOW_DBG_MSG3(("[6]+[13:8] : MEMPLL 2_AUTOK_BAND\n"));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e0), &u4value);
	VCOK_Chb_Mempll2 = ((u4value >> 8) & 0x3f) | (u4value & 0x40);
        mcSHOW_DBG_MSG3(("0x3e0=0x%x, VCOK_Chb_Mempll2=0x%x\n", u4value, VCOK_Chb_Mempll2));

        // RG_MEMPLL_RESERVE[2]=0, recover back
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        mcCLR_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);

        //ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x3e4), &u4value);
        //mcSHOW_DBG_MSG3(("0x3e4=0x%x\n", u4value));

        // PLL ready (internal loop) <--
#ifdef MEMPLL_INTERNAL_LOOP_TEST
      mcSHOW_DBG_MSG3(("PLL frequency = %d...\n", p->frequency));

	if (p->frequency == 800)
	{
		while (1)
		{
			MemPll_InternalLoop_Test(p, 1);
		}
	}
#endif

#if 1

        // MEMPLL*_EN=0
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        // MEMPLL*_AUTOK_EN=0
        p->channel = CHANNEL_A;
        // MEMPLL2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_AUTOK_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 23);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        // Need to MODIFY MEMPLL FBDIV here for some low data rate
//        if ((p->frequency == 333) || (p->frequency == 400) )
        if (MEMPLL_M4PDIV_1_0)
        {
            p->channel = CHANNEL_A;
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

            p->channel = CHANNEL_B;
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
            u4value = u4value & 0xff80ffff;
            u4value = u4value | MEMPLL_FBDIV2_6_0;
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);
        }

        // wait 1us
        mcDELAY_US(1);

        // MEMPLL*_FB_MCK_SEL=1 (switch to outer loop)
        p->channel = CHANNEL_A;
        // MEMPLL2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);
        // MEMPLL4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);
        // MEMPLL05_2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
        // MEMPLL05_3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
        // MEMPLL3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x624), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), u4value);
        // MEMPLL05_4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x680), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x618), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x618), u4value);
        // MEMPLL4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x630), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x630), u4value);
        // MEMPLL05_2_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x668), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x668), u4value);
        // MEMPLL05_3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x674), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x674), u4value);
        // MEMPLL3_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x624), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x624), u4value);
        // MEMPLL05_4_FB_MCK_SEL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x680), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x680), u4value);

        // wait 100ns
        mcDELAY_US(1);

        // MEMPLL*_EN=1
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        #ifdef CHA_MEMPLL3_DISABLE
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x620))) &= ~(0x01<<0);
        #endif

	#ifdef CHB_MEMPLL3_PHY05_MEMPLL4_DISABLE
	// (a) 0x1000F690[28]P0x10012690[28]SBS PLL ECO enable register
	// ]1, b}pop enable (0x1000F640[16]P0x10012640[16]), |SΨ쪺SBS PLL.
	// 0x1000F690[28]P0x10012690[28]]0, SBS PLL enableE1 function.
	// (b) 0x1000F690[29] POP PLL ECO enable register
	// ]1, |SΨ쪺POP PLL.
	// 0x1000F690[29]]0, POP PLL enableE1 function.
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x690))) |= ((0x01<<28) |(0x01<<29));
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x690))) |= (0x01<<28);
	#endif

#ifdef MEMPLL_NEW_POWERON
	*(volatile unsigned int*)(0x10006000) = 0x0b160001;
	*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
	mcDELAY_US(2);
	*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
//	mcDELAY_US(13);
	mcDELAY_US(30);
#else
        // wait 20us or 30us
        mcDELAY_US(30);
#endif

        // PLL ready (external loop) <--
#ifdef MEMPLL_EXTERNAL_LOOP_TEST
      mcSHOW_DBG_MSG3(("PLL frequency = %d...\n", p->frequency));

	if (p->frequency == 800)
	{
		while (1)
		{
			MemPll_InternalLoop_Test(p, 1);
		}
	}
#endif


    #endif

        // wait 20us
        //mcDELAY_US(20);

        // Power up sequence end...

        // has moved to the above
    /*
        // CLOCK enable
        p->channel = CHANNEL_A;
        // 0x640[4]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
        // 0x690[4]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
        mcSET_BIT(u4value, 4);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value); edward remark

        p->channel = CHANNEL_B;
        // 0x640[4]
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
    */

        // wait 1us
        mcDELAY_US(1);
    }
    else // SBS
    {
        // TBD
    }

#ifdef fcMEMPLL_DBG_MONITOR
    // delay 20us
    mcDELAY_US(20);

    // Monitor enable
    // RG_MEMPLL*_MONCK_EN=1; RG_MEMPLL*_MONREF=1
    // PLL2
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
    mcSET_BIT(u4value, 3);
    mcSET_BIT(u4value, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
    // PLL3
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
    mcSET_BIT(u4value, 3);
    mcSET_BIT(u4value, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
    // PLL4
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
    mcSET_BIT(u4value, 3);
    mcSET_BIT(u4value, 1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

    // 0x61c[22] RG_MEMPLL2_MON_EN=1, 0x61c[21:19] RG_MEMPLL2_CKSEL_MON=100
    // RG_MEMPLL2_MON_EN -> to enable M5
    // RG_MEMPLL2_CKSEL_MON = 100 (select M5)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x61c), &u4value);
    mcSET_BIT(u4value, 22);
    mcSET_FIELD(u4value, 4, 0x00380000, 19);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x61c), u4value);

    // 0x628[22] RG_MEMPLL3_MON_EN=1, 0x628[21:19] RG_MEMPLL3_CKSEL_MON=100
    // RG_MEMPLL3_MON_EN -> to enable M5
    // RG_MEMPLL3_CKSEL_MON = 100 (select M5)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x628), &u4value);
    mcSET_BIT(u4value, 22);
    mcSET_FIELD(u4value, 4, 0x00380000, 19);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x628), u4value);

    // 0x634[22] RG_MEMPLL4_MON_EN=1, 0x634[21:19] RG_MEMPLL4_CKSEL_MON=100
    // RG_MEMPLL4_MON_EN -> to enable M5
    // RG_MEMPLL4_CKSEL_MON = 100 (select M5)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x634), &u4value);
    mcSET_BIT(u4value, 22);
    mcSET_FIELD(u4value, 4, 0x00380000, 19);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x634), u4value);

    // 0x60c[23:8] RG_MEMPLL_TEST[15:0];
    // RG_MEMPLL_TEST[5:4]=01,RG_MEMPLL_TEST[3]=1;RG_MEMPLL_TEST[1:0]=11
    // RG_MEMPLL_TEST[5:4]=01 -> MEMPLL2
    // RG_MEMPLL_TEST[5:4]=10 -> MEMPLL3
    // RG_MEMPLL_TEST[5:4]=11 -> MEMPLL4
    // RG_MEMPLL_TEST[3,1] -> select CKMUX (measure clock or voltage)
    // RG_MEMPLL_TEST[0] -> RG_A2DCK_EN (for FT)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
    mcSET_FIELD(u4value, 3, 0x00000300, 8);
    mcSET_BIT(u4value, 11);
    mcSET_FIELD(u4value, 1, 0x00003000, 12);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);

/*
    //0x638[26:24] RG_MEMPLL_TEST_DIV2_SEL=011, /8 (for FT)
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x638), &u4value);
    mcSET_FIELD(u4value, 3, 0x07000000, 24);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x638), u4value);*/
#endif

#if 1
    // for debug
    u4value = DramOperateDataRate(p, 0);
    mcSHOW_DBG_MSG(("DRAM Operation Data Rate: 2PLL DDR-%d\n", u4value));
    u4value = DramOperateDataRate(p, 1);
    mcSHOW_DBG_MSG(("DRAM Operation Data Rate: 1PLL DDR-%d\n", u4value));
#endif

    if (ucstatus)
    {
//        mcSHOW_ERR_MSG(("register access fail!\n"));
        return DRAM_FAIL;
    }
    else
    {
        return DRAM_OK;
    }
}
#endif

//-------------------------------------------------------------------------
/** MemPllRefClkChange
 *  MEMPLL Refernce Clock Change. This function is used for SSC frequency change.
 *  Note to call this funciton at the duration at least 8*T (T is the XTAL clock period)
 *  @param ncpo_value       NCPO[30:10]
 *  @retval void
 */
//-------------------------------------------------------------------------
void MemPllRefClkChange(U32 ncpo_value)
{
    U32 u4value, u4temp;

    // RG_MPLL_SDM_PCW[30:10]: 0x10209284[20:0]
    u4value = (*(volatile unsigned int *)(0x10209284));
    mcSET_FIELD(u4value, ncpo_value, 0x001fffff, 0);
    (*(volatile unsigned int *)(0x10209284)) = u4value;

    // RG_MPLL_SDM_PCW_CHG: 0x10209284[31], toggle
    u4value = (*(volatile unsigned int *)(0x10209284));
    u4temp = mcTEST_BIT(u4value, 31);
    if (u4temp !=0)
    {
        mcCLR_BIT(u4value, 31);
    }
    else
    {
        mcSET_BIT(u4value, 31);
    }
    (*(volatile unsigned int *)(0x10209284)) = u4value;
}

DRAM_STATUS_T SscEnable(DRAMC_CTX_T *p)
{
U32 u4value;
U8 ucstatus = 0;

    if (p->ssc_en == ENABLE)
    {
        mcSHOW_DBG_MSG3(("Enable SSC...\n"));
        mcFPRINTF((fp_A60808, "Enable SSC...\n"));
        // RG_SYSPLL_SDM_SSC_EN = 1 (0x14[26])
        ucstatus |= ucDram_Register_Read(mcSET_SYS_REG_ADDR(0x014), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_SYS_REG_ADDR(0x014), u4value);
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

U32 DramOperateDataRate(DRAMC_CTX_T *p, U32 MODE_1PLL)
{
    U32 u4value1, u4value2, MPLL_POSDIV, MPLL_PCW, MPLL_FOUT;
    U32 MEMPLL_FBKDIV, MEMPLL_M4PDIV, MEMPLL_FOUT;
    U8 ucstatus = 0;

    u4value1 = (*(volatile unsigned int *)(0x10209280));
    u4value2 = mcGET_FIELD(u4value1, 0x00000070, 4);
    if (u4value2 == 0)
    {
        MPLL_POSDIV = 1;
    }
    else if (u4value2 == 1)
    {
        MPLL_POSDIV = 2;
    }
    else if (u4value2 == 2)
    {
        MPLL_POSDIV = 4;
    }
    else if (u4value2 == 3)
    {
        MPLL_POSDIV = 8;
    }
    else
    {
        MPLL_POSDIV = 16;
    }

    u4value1 = (*(volatile unsigned int *)(0x10209284));
    MPLL_PCW = mcGET_FIELD(u4value1, 0x001fffff, 0);

    MPLL_FOUT = 26/1*MPLL_PCW;
    MPLL_FOUT = Round_Operation(MPLL_FOUT, MPLL_POSDIV*28); // freq*16384

    if (MODE_1PLL)
    {
    	// PLL 3
    	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value1);
    }
    else
    {
    	// PLL 2
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value1);
    }
    MEMPLL_FBKDIV = mcGET_FIELD(u4value1, 0x007f0000, 16);

    MEMPLL_FOUT = MPLL_FOUT*1*4*(MEMPLL_FBKDIV+1);
    MEMPLL_FOUT = Round_Operation(MEMPLL_FOUT, 16384);

    //mcSHOW_DBG_MSG(("MPLL_POSDIV=%d, MPLL_PCW=0x%x, MPLL_FOUT=%d, MEMPLL_FBKDIV=%d, MEMPLL_FOUT=%d\n", MPLL_POSDIV, MPLL_PCW, MPLL_FOUT, MEMPLL_FBKDIV, MEMPLL_FOUT));

    return MEMPLL_FOUT;
}

#ifdef COMBO_MCP
DRAM_STATUS_T DramcPreInit(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
#else
DRAM_STATUS_T DramcPreInit(DRAMC_CTX_T *p)
#endif
{
	    U8 ucstatus = 0;
      U32 u4value;

#if defined(DQ_PLUS_90) || defined(DQ_MINUS_90_IN_LOW_FREQ)
	DQ_Phase[p->channel] = 0;
#endif

#if 0
        // Edward test for ungating.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
        mcSET_BIT(u4value, 3);
        mcSET_BIT(u4value, 26);
        mcCLR_BIT(u4value, 25);
        mcCLR_BIT(u4value, 30);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x130), &u4value);
        mcSET_BIT(u4value, 29);
        mcSET_BIT(u4value, 28);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x130), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1ec), &u4value);
        mcSET_BIT(u4value, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
        mcCLR_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);
#endif

        // sel_ph and write latency setting
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x10f010f0);
      // for testing, 1600 PoP WL issue debug, DQS T/4 in advance (DQS: 1, DQM: 0)
      //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH12), 0x1f001f00);

      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x434), 0xffffffff);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x438), 0xffffffff);
      // for testing, 1600 PoP WL issue debug, DQS T/4 in advance (DQ: 0)
      //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH13), 0x00000000);
      //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH14), 0x00000000);

      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x43c), 0x0000001f);

#ifdef TX_ADV_1T
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x400), 0x00000000);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x404), 0x00302000);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x408), 0x00000000);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x40c), 0x00000000);

  #ifdef DUAL_FREQ_DIFF_RLWL
    	// WL settings.
	if (p->frequency == 533)  // 1160 WL5
	{
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x11112222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x11112222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x11112222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0xffff5555);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x00ff0055);
	}
	else if ((p->frequency == 800) ||(p->frequency == 666) || (p->frequency == 733))
	{
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22222222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22222222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22222222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
	}
	else					// 1866/1780/2000/2133  WL8
	{
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
	}
  #else
    #if !defined(FREQ_BY_CHIP)
	// WL settings.
	#if  defined(DDR_1066) || defined(DDR_800)  || defined(DDR_838)  || defined(DDR_763) || defined(DDR_799)  // 1160 WL5
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x11112222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x11112222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x11112222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0xffff5555);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x00ff0055);
	#elif defined(DDR_1600) || defined(DDR_1466) || defined(DDR_1333) ||defined(DDR_814) // 1600/1466/1333 WL6
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
	#else					// 1866/1780/2000/2133  WL8
	  #ifdef WL11
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x44445555);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x44445555);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x44445555);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0xffff5555);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x00ff0055);
	  #else
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
      #endif
	#endif
	#else //defined(FREQ_BY_CHIP)
	    u4value = mt_get_dram_freq_setting();
        if((u4value == 800) || (u4value == 733)|| (u4value == 666)) //for MT6795M = 666, MT6795 = 733, MT6795T = 800
        {
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22222222);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22222222);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22222222);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
        }
        else if((u4value == 896) || (u4value == 890)) // will not go to here for all  MT6795/M/T series, reserved code here
        {
	      #ifdef WL11
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x44445555);
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x44445555);
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x44445555);
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0xffff5555);
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x00ff0055);
	      #else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x33333333);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x33333333);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x33333333);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
	      #endif
        }
	#endif
  #endif
#else
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x400), 0x11111111);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x404), 0x01413111);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x408), 0x11111111);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x40c), 0x11111111);

    #if !defined(FREQ_BY_CHIP)
	#if  defined(DDR_1066) || defined(DDR_800)  || defined(DDR_838)  || defined(DDR_763) ||	defined(DDR_799)  // 1160 WL5
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22223333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22223333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22223333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0xffff5555);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x00ff0055);
	#elif defined(DDR_1600) || defined(DDR_1466) || defined(DDR_1333) ||defined(DDR_814)  // 1600/1466/1333 WL6
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x33333333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x33333333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x33333333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
	#else					// 1866/1780/2000/2133  WL8
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x44444444);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x44444444);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x44444444);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
#endif
	#else //defined(FREQ_BY_CHIP)
	u4value = mt_get_dram_freq_setting();
	if((u4value == 800) || (u4value == 733)|| (u4value == 666)) //for MT6795M = 666, MT6795 = 733, MT6795T = 800
    {
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x33333333);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
    }
	#endif
#endif

#ifdef CS_ADV
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x410), 0x04951555);	// edward adjust CS
#else
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x410), 0x04955555);
#endif
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x418), 0x00000432);
      // for testing, 1600 PoP WL issue debug, DQS T/4 in advance
      //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SELPH11), 0x000000aa);

      // CA phase select
#ifdef CS_ADV
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e0), 0x2211ffff); // edward adjust CS
#else
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e0), 0x2201ffff);
#endif

#ifdef COMBO_MCP
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f8), emi_set->DRAMC_ACTIM05T_VAL);	// 0.5T?
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f8), LPDDR3_AC_TIME_05T_1f8_LOW);	// 0.5T?
	}
	else
	{
      		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f8), LPDDR3_AC_TIME_05T_1f8);	// 0.5T?
	}
	#else
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f8), LPDDR3_AC_TIME_05T_1f8);	// 0.5T?
	#endif
#endif

#ifdef CS_ADV
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x23c), 0x2211ffff); // edward adjust CS
#else
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x23c), 0x2201ffff);
#endif

      // LDO setting
      ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x644), &u4value);
      //mcSET_FIELD(u4value, 0x01, 0x00003f00, 8);
      mcSET_FIELD(u4value, 0x00, 0x00003f00, 8);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x644), u4value);

      // DIV2 clock phase sync
      // [5] MEMCLKENB, [3] MEMCLKEN_SEL (switch to internal for DIV2 clock sync)
      // move to the above, not set here
      /*
      ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
      mcCLR_BIT(u4value, 3);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

      ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
      mcCLR_BIT(u4value, 3);
      ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);
     */

      //DramcDiv2PhaseSync(p);

#if 0
    // delay 20T
    if (p->channel == CHANNEL_B) // only do channel B div2 sync
    {
#ifdef SPM_MODIFY
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcSET_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
#else
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcSET_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
#endif
    }
    else
    {
#ifdef SPM_MODIFY
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 2); // channel A internal sync, not from channel B
		mcSET_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);


		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
		mcCLR_BIT(u4value, 2); // 05PHY internal sync, not from channel B
		mcSET_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
		mcCLR_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);
#else
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcSET_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
#endif
      }
#endif
    return DRAM_OK;
  //    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), 0x09000099);	// Edward test.
}

#if defined(DQ_PLUS_90) || defined(DQ_MINUS_90_IN_LOW_FREQ)
void DramcDQ_PLUS_90(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0;
	//Only for WL=6. defined(DDR_1600) || defined(DDR_1466) || defined(DDR_1333) // 1600/1466/1333 WL6

	DramcEnterSelfRefresh(p, 1); // enter self refresh

	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22223333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22223333);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0xAAAA0000);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x5500FF);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x1F001F00);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x434), 0x00000000);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x438), 0x00000000);

	DramcDiv2PhaseSync((DRAMC_CTX_T *) p);

	DramcEnterSelfRefresh(p, 0); // leave self refresh
	DQ_Phase[p->channel] = 1;
}

void DramcDQ_PLUS_0(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0;
	//Only for WL=6. defined(DDR_1600) || defined(DDR_1466) || defined(DDR_1333) // 1600/1466/1333 WL6

	DramcEnterSelfRefresh(p, 1); // enter self refresh

	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x10f010f0);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x434), 0xffffffff);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x438), 0xffffffff);

	DramcDiv2PhaseSync((DRAMC_CTX_T *) p);

	DramcEnterSelfRefresh(p, 0); // leave self refresh
	DQ_Phase[p->channel] = 0;
}

void DramcDQ_MINUS_90(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0;
	//Only for WL=6. defined(DDR_1600) || defined(DDR_1466) || defined(DDR_1333) // 1600/1466/1333 WL6

	DramcEnterSelfRefresh(p, 1); // enter self refresh

	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x000000aa);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x1f001f00);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x434), 0x00000000);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x438), 0x00000000);

	DramcDiv2PhaseSync((DRAMC_CTX_T *) p);

	DramcEnterSelfRefresh(p, 0); // leave self refresh
	DQ_Phase[p->channel] = -1;
}
#endif	// defined(DQ_PLUS_90) || defined(DQ_MINUS_90_IN_LOW_FREQ)

#ifdef DQ_MINUS_90_IN_LOW_FREQ
void DramcDQ_ShiftBack_90(DRAMC_CTX_T *p)
{
	if (DQ_Phase[p->channel]==1)
	{
		// Now 90 degree. Shift to 0 degree.
		DramcDQ_PLUS_0(p);
	}
	else if (DQ_Phase[p->channel]==0)
	{
		// Now 0 degree. Shift to -90 degree.
		DramcDQ_MINUS_90(p);
	}
	else
	{
		// should not happen.
		mcSHOW_ERR_MSG(("[Error][Error] DQ -90 degree still want to shift -90 degree.!\n"));
	}
}
#endif

#ifdef CLKCA_PLUS_90_IN_LOW_FREQ
void 	CLKCAAdv90(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0;
	U32 u4value;

	DramcEnterSelfRefresh(p, 1); // enter self refresh

	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x410), 0x05EA6AA5);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x414), 0xAAAAAAAA);

	#ifdef DQ_PLUS_90
	if (DQ_Phase[p->channel] == 1)
	{
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x3F003F00 );
	}
	else
	{
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x30F030F0);
	}
	#else	// DQ_PLUS_90
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x30F030F0);
//	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x20F020F0);
	#endif	// DQ_PLUS_90

	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e0), 0x01300000);
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e4), &u4value);
	mcSET_BIT(u4value, 15);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), u4value);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x23c), 0x11300000);

	DramcDiv2PhaseSync((DRAMC_CTX_T *) p);
	DramcEnterSelfRefresh(p, 0); // Leave self refresh
}
#endif	// CLKCA_PLUS_90_IN_LOW_FREQ

U32 DramcGetMR2ByFreq(int dram_freq)
{
    U32 value = 0;

    switch(dram_freq)
    {
        case 381:   // 763MHz
        case 399:   // 799MHz
        case 400:   // 800MHz
        case 419:   // 838MHz
        case 533:   // 1160MHz
            value = 0x00170002; // RL9 WL4
            break;
        case 666:   // 1333MHz
            value = 0x00180002; // RL10 WL5
            break;
        case 407:   // 814MHz
        case 733:   // 1466MHz
        case 800:   // 1600MHz
            value = 0x001A0002; // RL12 WL6
            break;
        case 890:   // 1780MHz
        case 896:   // 1792MHz
        case 933:   // 1866MHz
            value = 0x001C0002; // RL14 WL8
       	    break;
        defalut:
            value = 0x001C0002;
            break;
    }
    //print("DramcGetMR2ByFreq: freq = %d, MR2 = 0x%x\n", dram_freq, value);
    return value;
}


//-------------------------------------------------------------------------
/** DramcInit
 *  DRAMC Initialization.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL
 */
//-------------------------------------------------------------------------
#ifdef COMBO_MCP
DRAM_STATUS_T DramcInit(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
#else
DRAM_STATUS_T DramcInit(DRAMC_CTX_T *p)
#endif
{
    // This function is implemented based on DE's bring up flow for DRAMC

    U8 ucstatus = 0;
    U32 u4value;

    SetFreqTableAddr();

    u4value = *(volatile unsigned *)(EMI_APB_BASE+0x0);

    if (p->channel == CHANNEL_A)
    {
	// EMI_CONA[17]
	if (u4value & 0x00020000)
	{
		uiDualRank = 1;
	}
	else
	{
		uiDualRank = 0;
	}
    }
    else
    {
	// EMI_CONA[16]
	if (u4value & 0x00010000)
	{
		uiDualRank = 1;
	}
	else
	{
		uiDualRank = 0;
	}
    }

#ifdef SINGLE_RANK_TEST
    uiDualRank = 0;
#endif

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return DRAM_FAIL;
    }

    if ((p->package!= PACKAGE_SBS) && (p->package != PACKAGE_POP))
    {
        mcSHOW_ERR_MSG(("argument POP should be 0(SBS dram) or 1(POP dram)!\n"));
        return DRAM_FAIL;
    }

    if (p->dram_type == TYPE_LPDDR3)
    {




#ifndef SPM_MODIFY
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x00001010);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x00000000);
#endif

        // 1800Mbps
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), 0x00403300);

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x048), emi_set->DRAMC_TEST2_4_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x048), LPDDR3_TEST2_4_048_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x048), LPDDR3_TEST2_4_048);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x048), LPDDR3_TEST2_4_048);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x08c), 0x00e00000);

#ifdef COMBO_MCP
    #ifdef DUAL_RANKS
	    if (uiDualRank)
	    {
            	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), emi_set->DRAMC_RKCFG_VAL | 0x00000001);
	    }
	    else
	    {
            	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), emi_set->DRAMC_RKCFG_VAL & 0xfffffffe);
	    }
    #else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), emi_set->DRAMC_RKCFG_VAL & 0xfffffffe);
    #endif
#else
	#ifdef DUAL_RANKS
	if (uiDualRank)
	{
		#ifdef DUAL_FREQ_DIFF_ACTIMING
		if (p->frequency == DUAL_FREQ_LOW)
		{
	        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110_LOW | 0x00000001);
		}
		else
		{
        		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110 | 0x00000001);
		}
        #else
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110 | 0x00000001);
		#endif

	}
	else
	{
		#ifdef DUAL_FREQ_DIFF_ACTIMING
		if (p->frequency == DUAL_FREQ_LOW)
		{
	        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110_LOW & 0xfffffffe);
		}
		else
		{
        		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110 & 0xfffffffe);
		}
		#else
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110 & 0xfffffffe);
		#endif

	}
#else
		#ifdef DUAL_FREQ_DIFF_ACTIMING
		if (p->frequency == DUAL_FREQ_LOW)
		{
	        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110_LOW & 0xfffffffe);
		}
		else
		{
        		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110 & 0xfffffffe);
		}
		#else
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), LPDDR3_RKCFG_110 & 0xfffffffe);
		#endif

#endif
#endif

        // pimux
        // [11:10] DQIENQKEND 01 -> 00 for DATLAT calibration issue, DQS input enable will refer to DATLAT
        // if need to enable this (for power saving), do it after all calibration done
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0d8), 0x40100510);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0d8), 0x00100110);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000001);

#ifdef CKE_EXTEND_1T
	if (p->frequency > 800)
	{
		// Reg.138[4] tCKEH/tCKEL extend 1T
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), 0x80000c10);
        }
	else
#endif
	{
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), 0x80000c00);
	}

	// set driving to max?
	//ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0b8), 0xff70ff70);
	//ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0bc), 0xff70ff70);
    #ifdef FTTEST_ZQONLY
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0b8), 0xff00ff00 | (7<<20) |(7<<4));
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0bc), 0xff00ff00 | (7<<20) |(7<<4));
    #else
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0b8), 0x99009900 | (DRIVING_DS2_0<<20) |(DRIVING_DS2_0<<4));
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0bc), 0x99009900 | (DRIVING_DS2_0<<20) |(DRIVING_DS2_0<<4));
    #endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x090), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x094), 0x40404040);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0dc), 0x00000000);

        // [3:0] move to 0x80[4:0]. This value should be DATLAT-3. It is used for runtime RX DQ/DQS K (not use??).
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e0), 0x15000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x118), 0x00000005);

	// [25] DATLAT4
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f0), 0x02000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), 0x11000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x168), 0x00000080);
//        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x130), 0x30000000);	// Clock pads enable.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x130), 0x10000000);	// Only enable clock pad 0.
        mcDELAY_US(1);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0d8), 0x40300510);

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x004), emi_set->DRAMC_CONF1_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x004), LPDDR3_CONF1_004_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x004), LPDDR3_CONF1_004);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x004), LPDDR3_CONF1_004);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x124), 0xc0000011);
//        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x094), 0x40404040);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1c0), 0x00000000);


#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), emi_set->DRAMC_ACTIM_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), LPDDR3_ACTIM_000_LOW);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), LPDDR3_ACTIM_000);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), LPDDR3_ACTIM_000);
	#endif
#endif


#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), emi_set->DRAMC_MISCTL0_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), LPDDR3_MISCTL0_VAL_0fc_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), LPDDR3_MISCTL0_VAL_0fc);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), LPDDR3_MISCTL0_VAL_0fc);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00100000);

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), emi_set->DRAMC_DDR2CTL_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), LPDDR3_DDR2CTL_07C_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), LPDDR3_DDR2CTL_07C);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), LPDDR3_DDR2CTL_07C);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x080), 0x00000be0);

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x028), 0xf1000000);

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), emi_set->DRAMC_ACTIM1_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), LPDDR3_ACTIM1_1E8_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), LPDDR3_ACTIM1_1E8);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), LPDDR3_ACTIM1_1E8);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x158), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x054), 0x00000001);	// Disable ODT before ZQ calibration.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000005);
        mcDELAY_US(200);	// tINIT3 > 200us

    #ifdef FTTEST_ZQONLY
	return;
    #endif
        // MR63 -> Reset
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_63);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_63);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(10);	// Wait >=10us if not check DAI.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR10 -> ZQ Init
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_10);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_10);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);		// tZQINIT>=1us
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

    #ifdef LPDDR_MAX_DRIVE
        // MR3, driving stregth
        // for testing, set to max
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00010003);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    #elif defined(LPDDR_MIN_DRIVE)
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00030003);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    #endif

        // MR1
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_1);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_1);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR2
        #ifdef DUAL_FREQ_DIFF_RLWL
	if (p->frequency == DUAL_FREQ_HIGH)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_FREQ_HIGH_MR2);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_FREQ_LOW_MR2);
	}
        #else
#ifdef COMBO_MCP
    #if 0
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_2);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), DramcGetMR2ByFreq(mt_get_dram_freq_setting()));
    #endif
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_2);
#endif
        #endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
        // MR11, ODT disable.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x0000000b);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), 0x11100000);	// [24] CKE1 on=1 no effect because Reg.1ech already set CKE1=CKE0.

#ifdef DUAL_RANKS

	if (uiDualRank)
	{
	        // MR63 -> Reset
		#ifdef COMBO_MCP
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_63);
		#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_63);
		#endif
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(10);	// Wait >=10us if not check DAI.
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	        // MR10 -> ZQ Init
		#ifdef COMBO_MCP
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_10);
		#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_10);
		#endif
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);		// tZQINIT>=1us
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	    #ifdef LPDDR_MAX_DRIVE
	        // MR3, driving stregth
	        // for testing, set to max
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10010003);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
	    #elif defined(LPDDR_MIN_DRIVE)
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10030003);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
	    #endif

	        // MR1
		#ifdef COMBO_MCP
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_1);
		#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_1);
		#endif
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	        // MR2
	        #ifdef DUAL_FREQ_DIFF_RLWL
		if (p->frequency == DUAL_FREQ_HIGH)
		{
		        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_FREQ_HIGH_MR2);
		}
		else
		{
		        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_FREQ_LOW_MR2);
		}
		#else
#ifdef COMBO_MCP
    #if 0
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_2);
#else
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | DramcGetMR2ByFreq(mt_get_dram_freq_setting()));
    #endif
#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_2);
#endif
		#endif

	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	        // MR11, ODT disable.
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x1000000b);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	}

#endif

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), emi_set->DRAMC_GDDR3CTL1_VAL);	// [24] CKE1 on=0 no effect because Reg.1ech already set CKE1=CKE0.
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), LPDDR3_GDDR3CTL1_0F4_LOW);	// [24] CKE1 on=0 no effect because Reg.1ech already set CKE1=CKE0.
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), LPDDR3_GDDR3CTL1_0F4);	// [24] CKE1 on=0 no effect because Reg.1ech already set CKE1=CKE0.
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), LPDDR3_GDDR3CTL1_0F4);	// [24] CKE1 on=0 no effect because Reg.1ech already set CKE1=CKE0.
	#endif
#endif


        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00000004);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000002);
       // ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00050000);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), 0x30111190);

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), emi_set->DRAMC_PD_CTRL_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), LPDDR3_PD_CTRL_1DC_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), LPDDR3_PD_CTRL_1DC);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), LPDDR3_PD_CTRL_1DC);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000001);
#ifdef DISABLE_DUALSCHED
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00100000);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00100001);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x084), 0x00000a56);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x00c), 0x00000000);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), 0x880D476D);

        // disable DQSICALI_NEW [23] (HW RX window calibration)

#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x044), emi_set->DRAMC_TEST2_3_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x044), LPDDR3_TEST2_3_044_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x044), LPDDR3_TEST2_3_044);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x044), LPDDR3_TEST2_3_044);
	#endif
#endif


        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), 0x11002059);
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), emi_set->DRAMC_CONF2_VAL);
#else
	#ifdef DUAL_FREQ_DIFF_ACTIMING
	if (p->frequency == DUAL_FREQ_LOW)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), LPDDR3_CONF2_008_LOW);
	}
	else
	{
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), LPDDR3_CONF2_008);
	}
	#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), LPDDR3_CONF2_008);
	#endif
#endif

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x010), 0x00000000);
#ifdef DISABLE_DRVREF
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x100), 0x00000000);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x100), 0x01000000);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x12121212);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f8), 0x00000000);

#ifdef CLKTDN_ENABLE
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0d4), &u4value);
        mcSET_BIT(u4value, 1);	// CLKTDN
        mcSET_BIT(u4value, 31);	// DS3TDN
        mcSET_BIT(u4value, 29);	// DS2TDN
        mcSET_BIT(u4value, 27);	// DS1TDN
        mcSET_BIT(u4value, 25);	// DS0TDN
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0d4), u4value);
#endif

#ifdef BYPASS_DUMMYPAD
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x124), &u4value);
        mcSET_BIT(u4value, 18);
        mcSET_BIT(u4value, 17);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x124), u4value);
#endif

#ifdef PBREF_DISBYREFNUM
	// R_DMDISREFNUM[2:0] : 0x138[23:21]  Derping suggests to set to 3.
	// R_DMPBREF_DISBYREFNUM   0x138[19]  1: enable function, 0: disable function
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x138), &u4value);
	mcSET_FIELD(u4value, 3, 0x00e00000, 21);
        mcSET_BIT(u4value, 19);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), u4value);
#endif

#ifdef DISABLE_PERBANK_REFRESH
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x110), &u4value);
        mcCLR_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), u4value);
#endif


#ifdef REFTHD_ADJUST_0
	// Clear R_DMREFTHD(dramc AO) 0x8[26:24]=0 for reduce special command (MR4) wait refresh queue time.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x008), &u4value);
	u4value &= 0xf8ffffff;
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), u4value);
#endif

#ifdef SREF_HW_LOSS_ECO
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x138), &u4value);
        mcSET_BIT(u4value, 20);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), u4value);
#endif

#if defined(STBCALDIS_HW_ENABLE) || defined(STBCALDIS_HW_DISABLE)
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x138), &u4value);
	#if defined(STBCALDIS_HW_ENABLE)
        mcCLR_BIT(u4value, 16);
	#elif defined(STBCALDIS_HW_DISABLE)
        mcSET_BIT(u4value, 16);
	#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), u4value);    
#endif

	DramcEnterSelfRefresh(p, 1);
	// Duty default value.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x148), 0x10010000);
#if 1
	mcDELAY_US(20);	// Wait PLL lock.
#else
	*(volatile unsigned int*)(0x10006000) = 0x0b160001;
	*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
	mcDELAY_US(2);
	*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
	mcDELAY_US(13);
#endif
	DramcEnterSelfRefresh(p, 0);

	if (uiDualRank==0)
	{
		// Single rank. CKE1 always off. [21]=1
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xf4), &u4value);
	        mcSET_BIT(u4value, 21);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xf4), u4value);
	}

#ifdef DISABLE_RW_PENDING
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x168), &u4value);
        mcSET_BIT(u4value, 9);
        mcSET_BIT(u4value, 8);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x168), u4value);
#endif

#ifdef DDRPIPE_CG_GATING
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x138), &u4value);
        mcSET_BIT(u4value, 29);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), u4value);
#endif

#ifdef WAVEFORM_MEASURE
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
        mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);
#endif

        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), 0x27100000);
        // sync mod (Rome will use sync mode only)
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), 0x27000000);

        // Set calibration related default value here. Need to set and enable.
        // GW default value
        // Rank 0 coarse tune 1 M_CK
	// Set rank 0 coarse tune and fine tune back.
	// DQSINCTL: 0xe0[27:24], TXDLY_DQSGATE: 0x404[14:12] for P0 [22:20] for P1
	// dly_DQSGATE: 0x410[23:22], dly_DQSGATE_P1: 0x410[25:24]
	/*
        dqsi_gw_dly_coarse_factor_handler(p, 32);
        // Reg.94h
        dqsi_gw_dly_fine_factor_handler(p, 16);
        // Reg.118h[3:0],  TXDLY_R1DQSGATE:0x418[6:4],  TXDLY_R1DQSGATE_P1: 0x418[10:8]
        // dly_R1DQSGATE: 0x418[1:0], dly_R1DQSGATE_P1: 0x418[3:2]
        dqsi_gw_dly_coarse_factor_handler_rank1(p, 32);
        // Reg.98h
        dqsi_gw_dly_fine_factor_handler_rank1(p, 16);         */

	// For DLE issue, TX delay not set now.
	//#if 1
#ifdef LOOPBACK_TEST
	if (p->channel==CHANNEL_A)
	{
		// Channel A middle = -7 (-DQ, +DQS) Win(-14,1)
		// DQS RX input delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x018), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x00000000);		// Need to adjust further
		// DQ input delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x210), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x214), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x218), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x21c), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x220), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x224), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x228), 0x07070707);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x22c), 0x07070707);
	}
	else
	{
		// Channel B middle = 22 (-DQ, +DQS) Win(14,31)
		// DQS RX input delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x018), 0x16161616);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x16161616);	// Need to adjust further.

		// DQ input delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x210), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x214), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x218), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x21c), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x220), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x224), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x228), 0x00000000);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x22c), 0x00000000);
	}
#else	// LOOPBACK_TEST
	// RX per-bit calibration.
	if (p->channel==CHANNEL_A)
	{
		if (uiDualRank)
		{
			// DQS RX input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x018), 0x08080908);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x08080908);		// Need to adjust further
			// DQ input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x210), 0x01010300);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x214), 0x06030002);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x218), 0x01010201);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x21c), 0x03020002);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x220), 0x00010103);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x224), 0x02010201);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x228), 0x02040200);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x22c), 0x02020201);
		}
		else
		{
			// DQS RX input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x018), 0x05030404);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x05030404);		// Need to adjust further
			// DQ input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x210), 0x02020200);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x214), 0x04030101);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x218), 0x00010100);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x21c), 0x02010100);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x220), 0x01010100);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x224), 0x02010101);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x228), 0x03030300);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x22c), 0x04040304);
		}

		#if 0
		#ifdef fcMEMPLL_SEAL_RING
		// This value may be impact by clock delay CATRAINING_STEP. Should be set in CTS mode.
		// TX per-bit calibration.
		// DQ output delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x200), 0x56666666);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x204), 0x56655555);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x208), 0x33433222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x20c), 0x55555555);
		// DQM output delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x010), 0x00005356);
		// DQS output delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x014), 0x2144);
		#endif
		#endif
	}
	else
	{
		if (uiDualRank)
		{
			// DQS RX input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x018), 0x0B0B060B);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x0B0B060B);	// Need to adjust further.
			// DQ input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x210), 0x00020202);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x214), 0x02020202);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x218), 0x01020201);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x21c), 0x01010100);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x220), 0x01010101);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x224), 0x01000002);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x228), 0x02000201);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x22c), 0x00010101);
		}
		else
		{
			// DQS RX input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x018), 0x09080408);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x09080408);	// Need to adjust further.
			// DQ input delay
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x210), 0x01010202);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x214), 0x00000100);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x218), 0x01000301);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x21c), 0x03020203);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x220), 0x01010200);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x224), 0x01000100);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x228), 0x04000403);
			ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x22c), 0x01020302);
		}
		#if 0
		#ifdef fcMEMPLL_SEAL_RING
		// This value may be impact by clock delay CATRAINING_STEP. Should be set in CTS mode.
		// TX per-bit calibration.
		// DQ output delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x200), 0x66666655);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x204), 0x22222222);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x208), 0x22212233);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x20c), 0x23334344);
		// DQM output delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x010), 0x00003226);
		// DQS output delay
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x014), 0x2113);
		#endif
		#endif
	}

#endif	// LOOPBACK_TEST

	/*
	// DLE calibration
	// Reg.7ch[6:4]
	// Reg.e4h[4] 		DATLAT3
	// Reg.f0h[25]		DATLAT4
	dle_factor_handler(p, 18);

	// TX per-bit calibration.
	// DQ output delay
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x200), 0x01211221);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x204), 0x22222222);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x208), 0x12222220);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x20c), 0x11111111);
	// DQM output delay
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x010), 0x00001121);
	// DQS output delay
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x014), 0x0);
	*/
    }
    else if (p->dram_type == TYPE_PCDDR3)
    {
        if (p->package == PACKAGE_POP)
        {
            mcSHOW_ERR_MSG(("don't support POP ddr3 now!\n"));
            return DRAM_FAIL;

        }

        // sel_ph and write latency
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x00f000f0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x434), 0xffffffff);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x438), 0xffffffff);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x43c), 0x0000001f);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x400), 0x11111111);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x404), 0x01312111);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x408), 0x11111111);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x40c), 0x11111111);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x410), 0x05d55555);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x418), 0x00000217);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41c), 0x33333333);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x33333333);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x33333333);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x5555ffff);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42c), 0x005500ff);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f8), 0x040006e4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e0), 0x08000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x23c), 0x00000000);



        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x644), 0x00000100);
#ifndef SPM_MODIFY
        // SPM control
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x00001010);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x00000000);
#endif

        // ========dramc_init============
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x048), 0x2300d10d);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x08c), 0x00e00000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), 0x00011400);
        // [11:10] DQIENQKEND 01 -> 00 for DATLAT calibration issue, DQS input enable will refer to DATLAT
        // if need to enable this (for power saving), do it after all calibration done
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0d8), 0x00100110);

        // DDR3 reset
        mcDELAY_US(200);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x000000a3);
        mcDELAY_US(500);

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), 0x80000c00);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0b8), 0x99009900  | (DRIVING_DS2_0<<20) |(DRIVING_DS2_0<<4));
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0bc), 0x99009900  | (DRIVING_DS2_0<<20) |(DRIVING_DS2_0<<4));
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x090), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x094), 0x40404040);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0dc), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e0), 0x14000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x118), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f0), 0x02000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), 0x11000000);
        // sync mod (Rome will use sync mode only)
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), 0x17000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x168), 0x00000080);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x130), 0x30000000);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x004), 0xc0748481);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x124), 0xc0000011);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1c0), 0x00000000);
        // 2013/10/18, JC, for DDR3, DISDMOEDIS [16] must be 1 due to self refresh exit clock stable timing (tCKSRX) issue
#ifdef DISABLE_DUALSCHED
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00110000);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00110001);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), 0x00003301);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x080), 0x000008e0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x028), 0xf1000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x158), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x000000a7);
        mcDELAY_US(2);
        // write ODT fixed to disable
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x054), 0x00000001);

        // MR2
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00004020);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR3
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00006000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR1
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00002000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR0
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00001f15);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // ZQ calibration enable
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00000400);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000010);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
        mcDELAY_US(1);

        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00001100);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x000000a3);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), 0xd5643840);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x00c), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), 0x558d48e7);
        //  disable DQSICALI_NEW [23] : HW RX window calibration
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x044), 0x9f3A0480);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), 0x80000060);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), 0x0000006c);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x010), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x100), 0x00000000);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x12121212);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f8), 0x00000000);
        // =========finish dramc_init======
    }
    else
    {
        mcSHOW_ERR_MSG(("unknow dram type  should be lpddr2 or lpddr3 or ddr3!\n"));
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

DRAM_STATUS_T DramcInit_test(DRAMC_CTX_T *p, EMI_SETTINGS* emi_set)
{
    // This function is implemented based on DE's bring up flow for DRAMC

    U8 ucstatus = 0;
    U32 u4value;

    //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000005);
    // CKEFIXON before MRW
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0e4), &u4value);
    mcSET_BIT(u4value, 2);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), u4value);

    mcDELAY_US(200);	// tINIT3 > 200us

#if 1
        // MR63 -> Reset
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_63);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_63);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(10);	// Wait >=10us if not check DAI.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR10 -> ZQ Init
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_10);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_10);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);		// tZQINIT>=1us
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

    #ifdef LPDDR_MAX_DRIVE
        // MR3, driving stregth
        // for testing, set to max
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00010003);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    #elif defined(LPDDR_MIN_DRIVE)
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00030003);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    #endif

        // MR1
#ifdef COMBO_MCP
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_1);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_1);
#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        // MR2
        #ifdef DUAL_FREQ_DIFF_RLWL
	if (p->frequency == DUAL_FREQ_HIGH)
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_FREQ_HIGH_MR2);
	}
	else
	{
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_FREQ_LOW_MR2);
	}
        #else
#ifdef COMBO_MCP
    #if 0
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), emi_set->iLPDDR3_MODE_REG_2);
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), DramcGetMR2ByFreq(mt_get_dram_freq_setting()));
    #endif
#else
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), LPDDR3_MODE_REG_2);
#endif
        #endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
        // MR11, ODT disable.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x0000000b);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
        mcDELAY_US(1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), 0x11100000);	// [24] CKE1 on=1 no effect because Reg.1ech already set CKE1=CKE0.
        // CKE1FIXON=1 before MRS
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0f4), &u4value);
        mcSET_BIT(u4value, 20);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), u4value);
#endif

#if 1
#ifdef DUAL_RANKS

	if (uiDualRank)
	{
	        // MR63 -> Reset
		#ifdef COMBO_MCP
	    	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_63);
		#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_63);
		#endif
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(10);	// Wait >=10us if not check DAI.
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	        // MR10 -> ZQ Init
		#ifdef COMBO_MCP
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_10);
		#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_10);
		#endif
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);		// tZQINIT>=1us
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	    #ifdef LPDDR_MAX_DRIVE
	        // MR3, driving stregth
	        // for testing, set to max
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10010003);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
	    #elif defined(LPDDR_MIN_DRIVE)
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10030003);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
	    #endif

	        // MR1
		#ifdef COMBO_MCP
        	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_1);
		#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_1);
		#endif
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	        // MR2
	        #ifdef DUAL_FREQ_DIFF_RLWL
		if (p->frequency == DUAL_FREQ_HIGH)
		{
		        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_FREQ_HIGH_MR2);
		}
		else
		{
		        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_FREQ_LOW_MR2);
		}
		#else
#ifdef COMBO_MCP
    #if 0
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | emi_set->iLPDDR3_MODE_REG_2);
#else
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | DramcGetMR2ByFreq(mt_get_dram_freq_setting()));
    #endif
#else
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10000000 | LPDDR3_MODE_REG_2);
#endif
		#endif

	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	        // MR11, ODT disable.
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x1000000b);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
	        mcDELAY_US(1);
	        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);

	}

#endif
#endif

        // CKE1FIXON=0
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0f4), &u4value);
        mcCLR_BIT(u4value, 20);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f4), u4value);

        // CKEFIXON=0
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000001);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x0e4), &u4value);
        mcCLR_BIT(u4value, 2);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), u4value);

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

void DramcEnterSelfRefresh(DRAMC_CTX_T *p, U8 op)
{
U8 ucstatus = 0;
U32 uiTemp;

    if (op == 1) // enter self refresh
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), &uiTemp);
        mcSET_BIT(uiTemp, POS_CONF1_SELFREF);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), uiTemp);
        mcDELAY_US(2);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMDRESP), &uiTemp);
        while ( (mcTEST_BIT(uiTemp, POS_SPCMDRESP_SREF_STATE))==0)
        {
            mcSHOW_DBG_MSG3(("Still not enter self refresh...\n"));
    	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMDRESP), &uiTemp);
        }
    }
    else // exit self refresh
    {
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), &uiTemp);
        mcCLR_BIT(uiTemp, POS_CONF1_SELFREF);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), uiTemp);
        mcDELAY_US(2);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMDRESP), &uiTemp);
        while ( (mcTEST_BIT(uiTemp, POS_SPCMDRESP_SREF_STATE))!=0)
        {
            mcSHOW_DBG_MSG3(("Still not exit self refresh...\n"));
    	    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_SPCMDRESP), &uiTemp);
        }
    }

    // tREFI/4 may cause self refresh fail. Set to tREFI manual first. After SF, switch back.
//    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x114), uiReg114h);
}

U32 PLL_LowFreq_RegVal[PLLGRPREG_SIZE],  PLL_HighFreq_RegVal[PLLGRPREG_SIZE];

#ifndef MT6795_MODIFICATION
#define PLLGRPREG_SIZE	21
#define VCOK_CHB_MEMPLL2		9
#define VCOK_CHB_MEMPLL4		10
#define VCOK_CHA_MEMPLL2		11
#define VCOK_CHA_MEMPLL4		12
#define VCOK_05PHY_MEMPLL2		13
#define VCOK_05PHY_MEMPLL3		14
#define MEMPLL_SETTING_START 	15
#define MEMPLL_SETTING_STOP 	20
#define MPLL_PCM				0
const U32 PLL_RegAddr[PLLGRPREG_SIZE] =
{
	0x10209284,		// MPLL PCM - with update bit.
	0x10209284,		// MPLL PCM
	0x10209280,		// MPLL POS_DIV  --> New for 1466 because different POSDIV.

	CHA_DDRPHY_BASE + 0x618,	// CHA MEMPLL2 phase
	CHA_DDRPHY_BASE + 0x630,	// CHA MEMPLL4 phase
	CHB_DDRPHY_BASE + 0x618,	// CHB MEMPLL2 phase
	CHB_DDRPHY_BASE + 0x630,	// CHB MEMPLL4 phase
	CHA_DDRPHY_BASE + 0x668,	// 05PHY MEMPLL2 phase
	CHA_DDRPHY_BASE + 0x674,	// 05PHY MEMPLL3 phase

	// CHB PLL group delay
	CHB_DDRPHY_BASE + 0x61c,
	CHB_DDRPHY_BASE + 0x634,

	// CHA PLL group delay
	CHA_DDRPHY_BASE + 0x61c,
	CHA_DDRPHY_BASE + 0x634,

	// 05PHY PLL group delay
	CHA_DDRPHY_BASE + 0x66c,
	CHA_DDRPHY_BASE + 0x678,

	CHA_DDRPHY_BASE + 0x614,	// CHA MEMPLL2 settings.
	CHA_DDRPHY_BASE + 0x62c,	// CHA MEMPLL4 settings.
	CHA_DDRPHY_BASE + 0x664,	// 05PHY MEMPLL2 settings.
	CHA_DDRPHY_BASE + 0x670,	// 05PHY MEMPLL3 settings.
	CHB_DDRPHY_BASE + 0x614,	// CHB MEMPLL2 settings.
	CHB_DDRPHY_BASE + 0x62c,	// CHB MEMPLL4 settings.
};

//U32 PLL_LowFreq_RegVal[PLLGRPREG_SIZE],  PLL_HighFreq_RegVal[PLLGRPREG_SIZE];

void DramcSavePLLSetting(DRAMC_CTX_T *p)
{
	U32 Offset;

	for (Offset = 0; Offset < PLLGRPREG_SIZE; Offset++)
	{
		if (p->frequency == DUAL_FREQ_LOW)
		{
			PLL_LowFreq_RegVal[Offset] = (*(volatile unsigned int *)(PLL_RegAddr[Offset]));
			if ((Offset >= MEMPLL_SETTING_START) && (Offset <= MEMPLL_SETTING_STOP))
			{
				PLL_LowFreq_RegVal[Offset] &= 0xfbfffffe; 	// MEMPLL enable & AUTOK_LOAD=0
			}
			else if (Offset == MPLL_PCM)
			{
				PLL_LowFreq_RegVal[Offset] |= 0x80000000;	// MPLL_SDM_PCW_CHG
			}
		}
		else
		{
			PLL_HighFreq_RegVal[Offset] = (*(volatile unsigned int *)(PLL_RegAddr[Offset]));
			if ((Offset >= MEMPLL_SETTING_START) && (Offset <= MEMPLL_SETTING_STOP))
			{
				PLL_HighFreq_RegVal[Offset] &= 0xfbfffffe; 	// MEMPLL enable & AUTOK_LOAD=0
			}
			else if (Offset == MPLL_PCM)
			{
				PLL_HighFreq_RegVal[Offset] |= 0x80000000;	// MPLL_SDM_PCW_CHG
			}

		}
	}


	if (p->frequency == DUAL_FREQ_LOW)
	{
		PLL_LowFreq_RegVal[VCOK_CHB_MEMPLL2] = (PLL_LowFreq_RegVal[VCOK_CHB_MEMPLL2] & (~0x0000007f)) | VCOK_Chb_Mempll2;
		PLL_LowFreq_RegVal[VCOK_CHB_MEMPLL4] = (PLL_LowFreq_RegVal[VCOK_CHB_MEMPLL4] & (~0x0000007f)) | VCOK_Chb_Mempll4;
		PLL_LowFreq_RegVal[VCOK_CHA_MEMPLL2] = (PLL_LowFreq_RegVal[VCOK_CHA_MEMPLL2] & (~0x0000007f)) | VCOK_Cha_Mempll2;
		PLL_LowFreq_RegVal[VCOK_CHA_MEMPLL4] = (PLL_LowFreq_RegVal[VCOK_CHA_MEMPLL4] & (~0x0000007f)) | VCOK_Cha_Mempll4;
		PLL_LowFreq_RegVal[VCOK_05PHY_MEMPLL2] = (PLL_LowFreq_RegVal[VCOK_05PHY_MEMPLL2] & (~0x0000007f)) | VCOK_05PHY_Mempll2;
		PLL_LowFreq_RegVal[VCOK_05PHY_MEMPLL3] = (PLL_LowFreq_RegVal[VCOK_05PHY_MEMPLL3] & (~0x0000007f)) | VCOK_05PHY_Mempll3;
	}
	else
	{
		PLL_HighFreq_RegVal[VCOK_CHB_MEMPLL2] = (PLL_HighFreq_RegVal[VCOK_CHB_MEMPLL2] & (~0x0000007f)) | VCOK_Chb_Mempll2;
		PLL_HighFreq_RegVal[VCOK_CHB_MEMPLL4] = (PLL_HighFreq_RegVal[VCOK_CHB_MEMPLL4] & (~0x0000007f)) | VCOK_Chb_Mempll4;
		PLL_HighFreq_RegVal[VCOK_CHA_MEMPLL2] = (PLL_HighFreq_RegVal[VCOK_CHA_MEMPLL2] & (~0x0000007f)) | VCOK_Cha_Mempll2;
		PLL_HighFreq_RegVal[VCOK_CHA_MEMPLL4] = (PLL_HighFreq_RegVal[VCOK_CHA_MEMPLL4] & (~0x0000007f)) | VCOK_Cha_Mempll4;
		PLL_HighFreq_RegVal[VCOK_05PHY_MEMPLL2] = (PLL_HighFreq_RegVal[VCOK_05PHY_MEMPLL2] & (~0x0000007f)) | VCOK_05PHY_Mempll2;
		PLL_HighFreq_RegVal[VCOK_05PHY_MEMPLL3] = (PLL_HighFreq_RegVal[VCOK_05PHY_MEMPLL3] & (~0x0000007f)) | VCOK_05PHY_Mempll3;
	}
}

#endif

void DramcSaveFreqSetting(DRAMC_CTX_T *p)
{
	U32 Offset;

	if (p->frequency == DUAL_FREQ_LOW)
	{
	        mcSHOW_DBG_MSG(("Save low frequency registers setting.\n"));
	}
	else
	{
	        mcSHOW_DBG_MSG(("Save high frequency registers setting.\n"));
	}

	#ifndef MT6795_MODIFICATION
	DramcSavePLLSetting(p);
	#endif

	for (Offset = 0; Offset < FREQREG_SIZE; Offset++)
	{
		if (p->frequency == DUAL_FREQ_LOW)
		{
			CHA_LowFreq_RegVal[Offset] = (*(volatile unsigned int *)(CHA_Freq_RegAddr[Offset]));
			CHB_LowFreq_RegVal[Offset] = (*(volatile unsigned int *)(CHB_Freq_RegAddr[Offset]));
		}
		else
		{
			CHA_HighFreq_RegVal[Offset] = (*(volatile unsigned int *)(CHA_Freq_RegAddr[Offset]));
			CHB_HighFreq_RegVal[Offset] = (*(volatile unsigned int *)(CHB_Freq_RegAddr[Offset]));
		}
	}
}

void DramcDumpFreqSetting(DRAMC_CTX_T *p)
{
	U32 Offset;
	U32 *pFreqRegSettingA;
	U32 *pFreqRegSettingB;
#ifndef MT6795_MODIFICATION
	U32 *pPLLGrpRegSetting;

	pPLLGrpRegSetting = PLL_LowFreq_RegVal;
#endif

        mcSHOW_DBG_MSG(("Low frequency registers setting.\n"));

	pFreqRegSettingA = CHA_LowFreq_RegVal;
	pFreqRegSettingB = CHB_LowFreq_RegVal;

DUMP_START:

#ifndef MT6795_MODIFICATION
        mcSHOW_DBG_MSG(("[PLL Group] Register number = %d\n", PLLGRPREG_SIZE));

	for (Offset = 0; Offset < PLLGRPREG_SIZE; Offset++)
	{
        	mcSHOW_DBG_MSG(("[PLL Group] Addr %xh = %xh\n", PLL_RegAddr[Offset], pPLLGrpRegSetting[Offset]));
	}
#endif

        mcSHOW_DBG_MSG(("[Channel A] Register number = %d\n", FREQREG_SIZE));

	for (Offset = 0; Offset < FREQREG_SIZE; Offset++)
	{
        	mcSHOW_DBG_MSG(("[Channel A] Addr %xh = %xh\n", CHA_Freq_RegAddr[Offset], pFreqRegSettingA[Offset]));
	}

        mcSHOW_DBG_MSG(("[Channel B] Register number = %d\n", FREQREG_SIZE));

	for (Offset = 0; Offset < FREQREG_SIZE; Offset++)
	{
        	mcSHOW_DBG_MSG(("[Channel B] Addr %xh = %xh\n", CHB_Freq_RegAddr[Offset], pFreqRegSettingB[Offset]));
	}

	if (pFreqRegSettingA == CHA_LowFreq_RegVal)
	{
	        mcSHOW_DBG_MSG(("High frequency registers setting. Register number = %d\n", FREQREG_SIZE));
		pFreqRegSettingA = CHA_HighFreq_RegVal;
		pFreqRegSettingB = CHB_HighFreq_RegVal;
#ifndef MT6795_MODIFICATION
		pPLLGrpRegSetting = PLL_HighFreq_RegVal;
#endif
		goto DUMP_START;
	}
}

void DramcDumpFreqReg(DRAMC_CTX_T *p)
{
	U32 Offset;

#ifndef MT6795_MODIFICATION
        mcSHOW_DBG_MSG(("[PLL Group] Register number = %d\n", PLLGRPREG_SIZE));

	for (Offset = 0; Offset < PLLGRPREG_SIZE; Offset++)
	{
        	mcSHOW_DBG_MSG(("[PLL Group] Addr %xh = %xh\n", PLL_RegAddr[Offset], (*(volatile unsigned int *)(PLL_RegAddr[Offset]))));
	}
#endif

        mcSHOW_DBG_MSG(("[Channel A] Register number = %d\n", FREQREG_SIZE));

	for (Offset = 0; Offset < FREQREG_SIZE; Offset++)
	{
        	mcSHOW_DBG_MSG(("[Channel A] Addr %xh = %xh\n", CHA_Freq_RegAddr[Offset],(*(volatile unsigned int *)(CHA_Freq_RegAddr[Offset]))));
	}
        mcSHOW_DBG_MSG(("[Channel B] Register number = %d\n", FREQREG_SIZE));

	for (Offset = 0; Offset < FREQREG_SIZE; Offset++)
	{
        	mcSHOW_DBG_MSG(("[Channel B] Addr %xh = %xh\n", CHB_Freq_RegAddr[Offset], (*(volatile unsigned int *)(CHB_Freq_RegAddr[Offset]))));
	}
}


void DramcMempllEnable(DRAMC_CTX_T *p, U32 RegOffset, U8 Enable)
{
	U32 u4value = 0;
	U8 ucstatus = 0;

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(RegOffset), &u4value);
        if (Enable)
        {
	        mcSET_BIT(u4value, 0);
        }
        else
        {
	        mcCLR_BIT(u4value, 0);
        }
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(RegOffset), u4value);
}

#ifdef DUAL_FREQ_K

// Use p->frequency to decide to high or low.
void DramcDFS(DRAMC_CTX_T *p)
{
	RXPERBIT_LOG_PRINT = 0;
	p->channel = CHANNEL_A;
	DramcEnterSelfRefresh(p, 1); // enter self refresh
	p->channel = CHANNEL_B;
	DramcEnterSelfRefresh(p, 1); // enter self refresh
	mcDELAY_US(1);

	DramcSwitchFreq(p, 0);
	DramcRestoreFreqSetting(p);
	DramcPhyReset(p);
	DramcDiv2PhaseSync((DRAMC_CTX_T *) p);

	p->channel = CHANNEL_A;
	DramcEnterSelfRefresh(p, 0); // enter self refresh
	p->channel = CHANNEL_B;
	DramcEnterSelfRefresh(p, 0); // enter self refresh
	mcDELAY_US(1);
	RXPERBIT_LOG_PRINT = 1;
}

#ifdef MT6795_MODIFICATION

void Dramc2PLLEnable(DRAMC_CTX_T *p, U8 Enable)
{
#ifdef DUAL_FREQ_MEMPLL_DISABLE
        // MEMPLL*_EN=1
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        DramcMempllEnable(p, 0x614, Enable);
        // MEMPLL4_EN
        DramcMempllEnable(p, 0x62c, Enable);
        // MEMPLL05_2_EN
        DramcMempllEnable(p, 0x664, Enable);
        // MEMPLL05_3_EN
        DramcMempllEnable(p, 0x670, Enable);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        DramcMempllEnable(p, 0x614, Enable);
        // MEMPLL4_EN
        DramcMempllEnable(p, 0x62c, Enable);
        // MEMPLL05_2_EN
        DramcMempllEnable(p, 0x664, Enable);
        // MEMPLL05_3_EN
        DramcMempllEnable(p, 0x670, Enable);

	#ifdef MEMPLL_NEW_POWERON
	*(volatile unsigned int*)(0x10006000) = 0x0b160001;
	*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
	mcDELAY_US(2);
	*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
	mcDELAY_US(30);
	#else
        // wait 20us or 30us
        mcDELAY_US(30);
	#endif
#endif 	// DUAL_FREQ_MEMPLL_DISABLE
}

void Dramc1PLLEnable(DRAMC_CTX_T *p, U8 Enable)
{
#ifdef DUAL_FREQ_MEMPLL_DISABLE
        // MEMPLL*_EN=1
        p->channel = CHANNEL_A;
        // MEMPLL3_EN
        DramcMempllEnable(p, 0x620, Enable);
        // MEMPLL05_4_EN
        DramcMempllEnable(p, 0x67c, Enable);

        p->channel = CHANNEL_B;
        // MEMPLL3_EN
        DramcMempllEnable(p, 0x620, Enable);
        // MEMPLL05_4_EN
        DramcMempllEnable(p, 0x67c, Enable);

	#ifdef MEMPLL_NEW_POWERON
	*(volatile unsigned int*)(0x10006000) = 0x0b160001;
	*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
	mcDELAY_US(2);
	*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
	mcDELAY_US(30);
	#else
        // wait 20us or 30us
        mcDELAY_US(30);
	#endif
#endif	// DUAL_FREQ_MEMPLL_DISABLE
}

void DramcSwitchFreq(DRAMC_CTX_T *p, U8 InitTime)
{
	U32 u4value;

	mcSHOW_DBG_MSG(("Switch frequency to %d...\n", p->frequency));
	mcFPRINTF((fp_A60808, "Switch frequency to %d...\n", p->frequency));

	if (InitTime)
	{
		p->channel = CHANNEL_A;
		DramcEnterSelfRefresh(p, 1); // enter self refresh
		p->channel = CHANNEL_B;
		DramcEnterSelfRefresh(p, 1); // enter self refresh
		mcDELAY_US(1);
	}
	else
	{
		if (p->frequency == DUAL_FREQ_HIGH)
		{
			Dramc2PLLEnable(p, 1);
		}
		else
		{
			Dramc1PLLEnable(p, 1);
		}
	}

	if (((*(volatile unsigned int *)(0x100041c0)) & 0x80000000 ) && ((*(volatile unsigned int *)(0x100111c0)) & 0x80000000 ))
	{
		// HW GW enable. Do HW GW sync to shuffle table.
		if (p->frequency == DUAL_FREQ_LOW)
		{
			// Switch to low. Current is high.
			CHA_HighFreq_RegVal[3] = (*(volatile unsigned int *)(0x1020e374));              // cha r0
			CHA_HighFreq_RegVal[6] = (*(volatile unsigned int *)(0x1020e378));              // cha r1
			CHB_HighFreq_RegVal[3] = (*(volatile unsigned int *)(0x10213374));              // chb r0
			CHB_HighFreq_RegVal[6] = (*(volatile unsigned int *)(0x10213378));              // chb r1
		}
		else
		{
			// Switch to high. Current is low.
			CHA_LowFreq_RegVal[3] = (*(volatile unsigned int *)(0x1020e374));               // cha r0
			CHA_LowFreq_RegVal[6] = (*(volatile unsigned int *)(0x1020e378));               // cha r1
			CHB_LowFreq_RegVal[3] = (*(volatile unsigned int *)(0x10213374));               // chb r0
			CHB_LowFreq_RegVal[6] = (*(volatile unsigned int *)(0x10213378));               // chb r1
		}
	}

        (*(volatile unsigned int *)(0x10006000)) = 0x0b160001;	// Open route for switch 1PLL/2PLL mode.
	if (p->frequency == DUAL_FREQ_HIGH)
	{
		// 2 PLL mode.
		(*(volatile unsigned int *)(0x10006014)) &= (~(0x01 << 29));
	}
	else
	{
		// 1 PLL mode.
		(*(volatile unsigned int *)(0x10006014)) |= (0x01 << 29);
	}

	// The following phase sync may be no need according to KaiHsin reference flow.
	DramcDiv2PhaseSync(p);

	//DramcDumpFreqReg(p);
	//DramcDumpFreqSetting(p);
	mcSHOW_DBG_MSG(("mt_pll_post_init: mt_get_mem_freq = %dKhz\n", mt_get_mem_freq()));

	if (InitTime)
	{
		p->channel = CHANNEL_A;
		DramcEnterSelfRefresh(p, 0); // exit self refresh
		p->channel = CHANNEL_B;
		DramcEnterSelfRefresh(p, 0); // exit self refresh
		mcDELAY_US(1);
	}
	else
	{
		if (p->frequency == DUAL_FREQ_HIGH)
		{
			Dramc1PLLEnable(p, 0);
		}
		else
		{
			Dramc2PLLEnable(p, 0);
		}

	}
}

#else



void DramcRestorePLLSetting(DRAMC_CTX_T *p)
{
	U32 Offset;

	for (Offset = 0; Offset < PLLGRPREG_SIZE; Offset++)
	{
		if (p->frequency == DUAL_FREQ_LOW)
		{
			(*(volatile unsigned int *)(PLL_RegAddr[Offset])) = PLL_LowFreq_RegVal[Offset];
		}
		else
		{
			(*(volatile unsigned int *)(PLL_RegAddr[Offset])) = PLL_HighFreq_RegVal[Offset];
		}
	}
}

void DramcSwitchFreq(DRAMC_CTX_T *p, U8 InitTime)
{
	U32 u4value;

	mcSHOW_DBG_MSG(("Switch frequency to %d...\n", p->frequency));
	mcFPRINTF((fp_A60808, "Switch frequency to %d...\n", p->frequency));

	if (InitTime)
	{
		p->channel = CHANNEL_A;
		DramcEnterSelfRefresh(p, 1); // enter self refresh
		p->channel = CHANNEL_B;
		DramcEnterSelfRefresh(p, 1); // enter self refresh
		mcDELAY_US(1);
	}



	if (InitTime)
	{
		#ifdef SPM_CONTROL_AFTERK
		TransferToRegControl();
		#endif
		mt_mempll_init(p);
		//MemPllPreInit(p);
		//MemPllInit(p);
		mcDELAY_US(1);
		mt_mempll_cali(p);
	}
	else
	{
		if (((*(volatile unsigned int *)(0x100041c0)) & 0x80000000 ) && ((*(volatile unsigned int *)(0x100111c0)) & 0x80000000 ))
		{
			// HW GW enable. Do HW GW sync to shuffle table.
			if (p->frequency == DUAL_FREQ_LOW)
			{
				// Switch to low. Current is high.
				CHA_HighFreq_RegVal[3] = (*(volatile unsigned int *)(0x1020e374));              // cha r0
				CHA_HighFreq_RegVal[6] = (*(volatile unsigned int *)(0x1020e378));              // cha r1
				CHB_HighFreq_RegVal[3] = (*(volatile unsigned int *)(0x10213374));              // chb r0
				CHB_HighFreq_RegVal[6] = (*(volatile unsigned int *)(0x10213378));              // chb r1
			}
			else
			{
				// Switch to high. Current is low.
				CHA_LowFreq_RegVal[3] = (*(volatile unsigned int *)(0x1020e374));               // cha r0
				CHA_LowFreq_RegVal[6] = (*(volatile unsigned int *)(0x1020e378));               // cha r1
				CHB_LowFreq_RegVal[3] = (*(volatile unsigned int *)(0x10213374));               // chb r0
				CHB_LowFreq_RegVal[6] = (*(volatile unsigned int *)(0x10213378));               // chb r1
			}
		}
		//MemPllInit(p);
		//mcDELAY_US(1);
	        // MEMPLL Off

	        // CHANNEL_A;
	        // MEMPLL2_EN=0
	        (*(volatile unsigned int *)(0x1000f614)) &= 0xfffffffe;
	        // MEMPLL4_EN=0
	        (*(volatile unsigned int *)(0x1000f62c)) &= 0xfffffffe;
	        // MEMPLL05_2_EN=0
	        (*(volatile unsigned int *)(0x1000f664)) &= 0xfffffffe;
	        // MEMPLL05_3_EN=0
	        (*(volatile unsigned int *)(0x1000f670)) &= 0xfffffffe;
	        // MEMPLL3_EN=0
	        (*(volatile unsigned int *)(0x1000f620)) &= 0xfffffffe;
	        // MEMPLL05_4_EN=0
	        (*(volatile unsigned int *)(0x1000f67c)) &= 0xfffffffe;

	        // CHANNEL_B;
	        // MEMPLL2_EN=0
	        (*(volatile unsigned int *)(0x10012614)) &= 0xfffffffe;
	        // MEMPLL4_EN=0
	        (*(volatile unsigned int *)(0x1001262c)) &= 0xfffffffe;
	        // MEMPLL05_2_EN=0
	        (*(volatile unsigned int *)(0x10012664)) &= 0xfffffffe;
	        // MEMPLL05_3_EN=0
	        (*(volatile unsigned int *)(0x10012670)) &= 0xfffffffe;
	        // MEMPLL3_EN=0
	        (*(volatile unsigned int *)(0x10012620)) &= 0xfffffffe;
	        // MEMPLL05_4_EN=0
	        (*(volatile unsigned int *)(0x1001267c)) &= 0xfffffffe;

		#ifdef DUAL_FREQ_MPLL_ONOFF
	        // RG_MPLL_EN=0 : 0x10209280[0] (disable MPLL first)
	       	u4value = (*(volatile unsigned int *)(0x10209280));
	        mcCLR_BIT(u4value, 0);
	        (*(volatile unsigned int *)(0x10209280)) = u4value;
	        #endif

		// AutoK_Load = 0. load from registers.
	        // CHANNEL_A;
	        (*(volatile unsigned int *)(0x1000f614)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1000f62c)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1000f664)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1000f670)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1000f620)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1000f67c)) &= ~(0x01 << 26);

	        // CHANNEL_B;
	        (*(volatile unsigned int *)(0x10012614)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1001262c)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x10012664)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x10012670)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x10012620)) &= ~(0x01 << 26);
	        (*(volatile unsigned int *)(0x1001267c))  &= ~(0x01 << 26);

	        mcDELAY_US(1);  // Wait 100ns

		DramcRestorePLLSetting(p);
		//mt_mempll_cali(p);

	        mcDELAY_US(1);  // Wait 100ns


         	#ifdef DUAL_FREQ_MPLL_ONOFF
	        // MEMPLL On
	        //RG_MPLL_EN=1 : 0x10209280[0] (Enable MPLL first)
	        u4value = (*(volatile unsigned int *)(0x10209280));
	        mcSET_BIT(u4value, 0);
	        (*(volatile unsigned int *)(0x10209280)) = u4value;
	        #endif

	        // CHANNEL_A;
	        // MEMPLL2_EN=1
	        (*(volatile unsigned int *)(0x1000f614)) |= 0x00000001;
	        // MEMPLL4_EN=1
	        (*(volatile unsigned int *)(0x1000f62c)) |= 0x00000001;
	        // MEMPLL05_2_EN=1
	        (*(volatile unsigned int *)(0x1000f664)) |= 0x00000001;
	        // MEMPLL05_3_EN=1
	        (*(volatile unsigned int *)(0x1000f670)) |= 0x00000001;
	        // MEMPLL3_EN=1
	        (*(volatile unsigned int *)(0x1000f620)) |= 0x00000001;
	        // MEMPLL05_4_EN=1
	        (*(volatile unsigned int *)(0x1000f67c)) |= 0x00000001;

	        // CHANNEL_B;
	        // MEMPLL2_EN=1
	        (*(volatile unsigned int *)(0x10012614)) |= 0x00000001;
	        // MEMPLL4_EN=1
	        (*(volatile unsigned int *)(0x1001262c)) |= 0x00000001;
	        // MEMPLL05_2_EN=1
	        (*(volatile unsigned int *)(0x10012664)) |= 0x00000001;
	        // MEMPLL05_3_EN=1
	        (*(volatile unsigned int *)(0x10012670)) |= 0x00000001;
	        // MEMPLL3_EN=1
	        (*(volatile unsigned int *)(0x10012620)) |= 0x00000001;
	        // MEMPLL05_4_EN=1
	        (*(volatile unsigned int *)(0x1001267c)) |= 0x00000001;

	        mcDELAY_US(20);  // Wait 20us

		#ifdef DFS_RESET
	        // Add reset
		*(volatile unsigned int*)(0x10006000) = 0x0b160001;
		*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
		mcDELAY_US(1);
		*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
		mcDELAY_US(1);
		#endif


	}
	DramcDiv2PhaseSync(p);

	//DramcDumpFreqReg(p);
	//DramcDumpFreqSetting(p);
	//mcSHOW_DBG_MSG(("mt_pll_post_init: mt_get_mem_freq = %dKhz\n", mt_get_mem_freq()));

	if (InitTime)
	{
		p->channel = CHANNEL_A;
		DramcEnterSelfRefresh(p, 0); // exit self refresh
		p->channel = CHANNEL_B;
		DramcEnterSelfRefresh(p, 0); // exit self refresh
	}


}
#endif



void DramcRestoreFreqSetting(DRAMC_CTX_T *p)
{
	U32 Offset, HWEnable;

  SetFreqTableAddr();

	if ((*(volatile unsigned int *)(CHA_DRAMCAO_BASE+0x1c0)) & (0x80000000))
	{
		HWEnable = 1;
	}
	else
	{
		HWEnable = 0;
	}

	if (HWEnable)
	{
		// Disable HW gating.
		(*(volatile unsigned int *)(CHA_DRAMCAO_BASE+0x1c0)) &= 0x7fffffff;
		(*(volatile unsigned int *)(CHB_DRAMCAO_BASE+0x1c0)) &= 0x7fffffff;
	}

	for (Offset = 0; Offset < FREQREG_SIZE; Offset++)
	{
		if (p->frequency == DUAL_FREQ_LOW)
		{
			(*(volatile unsigned int *)(CHA_Freq_RegAddr[Offset])) = CHA_LowFreq_RegVal[Offset];
			(*(volatile unsigned int *)(CHB_Freq_RegAddr[Offset])) = CHB_LowFreq_RegVal[Offset];
		}
		else
		{
			(*(volatile unsigned int *)(CHA_Freq_RegAddr[Offset])) = CHA_HighFreq_RegVal[Offset];
			(*(volatile unsigned int *)(CHB_Freq_RegAddr[Offset])) = CHB_HighFreq_RegVal[Offset];
		}
	}

	if (HWEnable)
	{
		// Enable HW gating.
		(*(volatile unsigned int *)(CHA_DRAMCAO_BASE+0x1c0)) |= 0x80000000;
		(*(volatile unsigned int *)(CHB_DRAMCAO_BASE+0x1c0)) |= 0x80000000;
	}
}

#endif

void DramcLowFreqWrite(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U16 u2freq_orig;
    U32 u4err_value;

    if (p->fglow_freq_write_en == ENABLE)
    {
        u2freq_orig = p->frequency;
        p->frequency = p->frequency_low;
        mcSHOW_DBG_MSG(("Enable low speed write function...\n"));
        mcFPRINTF((fp_A60808, "Enable low speed write function...\n"));
        // we will write data in memory on a low frequency,to make sure the data we write is  right
        // then use engine2 read to do the calibration
        // so ,we will do :
        // 1.change freq
        // 2. use self test engine2 write to write data ,and check the data is right or not
        // 3.change freq to original value

        // 1. change freq
        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 1); // enter self refresh
        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 1); // enter self refresh
        mcDELAY_US(1);
	#ifdef SPM_CONTROL_AFTERK
	TransferToRegControl();
	#endif
	mt_mempll_init(p);
	//MemPllPreInit(p);
        //MemPllInit(p);
        mcDELAY_US(1);
        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 0); // exit self refresh
        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 0); // exit self refresh

        // Need to do phase sync after change frequency
        //p->channel = CHANNEL_A;
        //DramcDiv2PhaseSync(p);
        //p->channel = CHANNEL_B;
        DramcDiv2PhaseSync(p);

        // double check frequency
        //mcSHOW_DBG_MSG(("Low Speed Write: mt_get_mem_freq = %dKhz\n", mt_get_mem_freq()));

        // 2. use self test engine2 to write data (only support AUDIO or XTALK pattern)
        p->channel = CHANNEL_A;
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }
        else if (p->test_pattern== TEST_XTALK_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("ERROR! Only support AUDIO or XTALK in Low Speed Write and High Speed Read calibration!! \n"));
            u4err_value = 0xffffffff;
        }

        // May error due to gating not calibrate @ low speed
        mcSHOW_DBG_MSG(("Low speed write error value: 0x%x\n", u4err_value));
        mcFPRINTF((fp_A60808, "Low speed write error value: 0x%x\n", u4err_value));

        p->channel = CHANNEL_B;
        if (p->test_pattern== TEST_AUDIO_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 1, 0, 0, 0);
        }
        else if (p->test_pattern== TEST_XTALK_PATTERN)
        {
            u4err_value = DramcEngine2(p, TE_OP_WRITE_READ_CHECK, p->test2_1, p->test2_2, 2, 0, 0, 0);
        }
        else
        {
            mcSHOW_ERR_MSG(("ERROR! Only support AUDIO or XTALK in Low Speed Write and High Speed Read calibration!! \n"));
            u4err_value = 0xffffffff;
        }

        // May error due to gating not calibrate @ low speed
        mcSHOW_DBG_MSG(("Low speed write error value: 0x%x\n", u4err_value));
        mcFPRINTF((fp_A60808, "Low speed write error value: 0x%x\n", u4err_value));

        // do phy reset due to ring counter may be wrong
        p->channel = CHANNEL_A;
        DramcPhyReset(p);
        p->channel = CHANNEL_B;
        DramcPhyReset(p);

        // 3. change to original freq
        p->frequency = u2freq_orig;
        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 1); // enter self refresh
        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 1); // enter self refresh
        mcDELAY_US(1);
	#ifdef SPM_CONTROL_AFTERK
	TransferToRegControl();
	#endif
	mt_mempll_init(p);
	//MemPllPreInit(p);
        //MemPllInit(p);
        mcDELAY_US(1);
        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 0); // exit self refresh
        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 0); // exit self refresh

        // Need to do phase sync after change frequency
        //p->channel = CHANNEL_A;
        //DramcDiv2PhaseSync(p);
        //p->channel = CHANNEL_B;
        DramcDiv2PhaseSync(p);

        // double check frequency
        //mcSHOW_DBG_MSG(("High Speed Read: mt_get_mem_freq = %dKhz\n", mt_get_mem_freq()));
    }
}

void DramcDiv2PhaseSync(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4value;

    //if (p->channel == CHANNEL_B) // only do channel B div2 sync
    {
#ifdef SPM_MODIFY
        u4value = (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x640));
        mcCLR_BIT(u4value, 5);
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x640)) = u4value;

        mcDELAY_US(1);

        u4value = (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x640));
        mcSET_BIT(u4value, 5);
        (*(volatile unsigned int *)(CHB_DDRPHY_BASE + 0x640)) = u4value;
#else
               /*
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcSET_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);*/
#endif
    }
    /*else
    {
#ifdef SPM_MODIFY
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 2); // channel A internal sync, not from channel B
		mcSET_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);


		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
		mcCLR_BIT(u4value, 2); // 05PHY internal sync, not from channel B
		mcSET_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
		mcCLR_BIT(u4value, 1);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);
#else
		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcSET_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

		mcDELAY_US(1);

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
		mcCLR_BIT(u4value, 5);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);
#endif
      }*/
}

void DramcPhyReset(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;//, ucref_cnt;
    U32 u4value;

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

}

void DramcPhyReset_DramcRST(DRAMC_CTX_T *p)
{
	U8 ucstatus = 0;//, ucref_cnt;
	U32 u4value;

	// reset phy
	// 0x0f0[28] = 1 -> 0
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
	mcSET_BIT(u4value, POS_PHYCTL1_PHYRST);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value);

        // Toggle DRAMC SW RESET.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe4), &u4value);
        mcSET_BIT(u4value, 11);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), u4value);

	//delay 10ns, need to change when porting
	mcDELAY_US(1);

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), &u4value);
	mcCLR_BIT(u4value, POS_PHYCTL1_PHYRST);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_PHYCTL1), u4value);

        // Toggle DRAMC SW RESET.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe4), &u4value);
        mcCLR_BIT(u4value, 11);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0xe4), u4value);
}



void DramcRANKINCTLConfig(DRAMC_CTX_T *p)
{

#ifdef DUAL_RANKS
    U8 ucstatus = 0;
    U32 u4value;
    U32 u4CoarseTune_MCK;

    if (uiDualRank)
    {
    	// RANKINCTL_ROOT1 = DQSINCTL+reg_TX_DLY_DQSGATE (min of RK0 and RK1)-1.
    	#if 0
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0xe0), &u4value);
        u4CoarseTune_MCK = mcGET_FIELD(u4value, 0x0f000000, 24);
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x404), &u4value);
	u4CoarseTune_MCK += mcGET_FIELD(u4value, 0x00007000, 12);
	#else
	if (opt_gw_coarse_value_R0[p->channel] < opt_gw_coarse_value_R1[p->channel])
	{
		u4CoarseTune_MCK = opt_gw_coarse_value_R0[p->channel] >> 2;
	}
	else
	{
		u4CoarseTune_MCK = opt_gw_coarse_value_R1[p->channel] >> 2;
	}
	/*
	if (u4CoarseTune_MCK >= 2)
	{
		u4CoarseTune_MCK -= 2;
	}
	else if (u4CoarseTune_MCK >= 1)
	{
		u4CoarseTune_MCK--;
	}*/
	#endif

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x138), &u4value);
        mcSET_FIELD(u4value, u4CoarseTune_MCK, 0x0f, 0);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), u4value);

	// RANKINCTL = RANKINCTL_ROOT1+0
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1c4), &u4value);
	//u4CoarseTune_MCK += 2;
	u4CoarseTune_MCK += 0;
        mcSET_FIELD(u4value, u4CoarseTune_MCK, 0x000f0000, 16);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1c4), u4value);
    }
#endif

#ifdef DISABLE_FR_REFRESH
	{
		U32 REFCNT;

	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x008), &u4value);
	        mcSHOW_ERR_MSG(("[M_CK_REFRESH] Original Reg.008h[10:8]=%x\n", u4value));
		REFCNT = u4value & 0xff;

	#ifdef DUAL_FREQ_K
		if (p->frequency == DUAL_FREQ_LOW)
		{
			// For M_CK extend.
		        REFCNT = REFCNT * DUAL_FREQ_LOW / DUAL_FREQ_HIGH;
		}
	#endif

	        REFCNT = REFCNT *92/100;	// For SSC
	#ifdef DISABLE_FR_REFRESH_MAX_REFCNT
		REFCNT = 0xff;
	#endif

	        u4value = (u4value & 0xffffff00) | REFCNT;
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), u4value);
	        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x008), &u4value);
	        mcSHOW_ERR_MSG(("[M_CK_REFRESH] REFCNT = %x, New Reg.008h[10:8]=%x\n", REFCNT, u4value));

		ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
		mcCLR_BIT(u4value, 24);
		ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);
	}
#endif

#ifdef TEMP_SENSOR_ENABLE

    mcSHOW_ERR_MSG(("[DRAMC] TEMP SENSOR is enabled\n"));
    if ((p->dram_type == TYPE_LPDDR3) || (p->dram_type == TYPE_LPDDR2))
    {
        // Enable rum time refresh rate auto update
        // important!!

	// The following derating AC timings need to be modified according to different parts AC timings + 1.875ns + 1 DRAMC clk
        unsigned int TRRD_DERATE = 0x05 <<28;
	unsigned int TRPAB_DERATE = 0x02 << 24;
	unsigned int  TRP_DERATE = 0x09 << 20;
	unsigned int  TRAS_DERATE = 0x0d <<16;
	unsigned int  TRC_DERATE = 0x17 << 8;
	unsigned int  TRCD_DERATE = 0x09 <<4;

	/* setup derating AC timing & enable */
	 ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f0),
	 	TRRD_DERATE | TRPAB_DERATE | TRP_DERATE | TRAS_DERATE | TRC_DERATE | TRCD_DERATE | 0x01);

        // set DQ bit 0, 1, 2 pinmux
        if (p->channel == CHANNEL_A)
        {
            if (p->dram_type == TYPE_LPDDR3)
            {
                // refer to CA training pinmux array
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), &u4value);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[0], MASK_RRRATE_CTL_BIT0_SEL, POS_RRRATE_CTL_BIT0_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[1], MASK_RRRATE_CTL_BIT1_SEL, POS_RRRATE_CTL_BIT1_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[2], MASK_RRRATE_CTL_BIT2_SEL, POS_RRRATE_CTL_BIT2_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHA[3], MASK_RRRATE_CTL_BIT3_SEL, POS_RRRATE_CTL_BIT3_SEL);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), u4value);
            }
            else // LPDDR2
            {
                //TBD
            }
        }
        else
        {
            if (p->dram_type == TYPE_LPDDR3)
            {
                // refer to CA training pinmux array
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), &u4value);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[0], MASK_RRRATE_CTL_BIT0_SEL, POS_RRRATE_CTL_BIT0_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[1], MASK_RRRATE_CTL_BIT1_SEL, POS_RRRATE_CTL_BIT1_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[2], MASK_RRRATE_CTL_BIT2_SEL, POS_RRRATE_CTL_BIT2_SEL);
                mcSET_FIELD(u4value, uiLPDDR_PHY_Mapping_POP_CHB[3], MASK_RRRATE_CTL_BIT3_SEL, POS_RRRATE_CTL_BIT3_SEL);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RRRATE_CTL), u4value);
            }
            else // LPDDR2
            {
                //TBD
            }
        }

        //Set MRSMA to MR4.
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x88), 0x04);

        // set refrcnt
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_ACTIM1), &u4value);
        #ifdef STRESS_MR4_ZQCS
        mcSET_FIELD(u4value, 0x08, MASK_ACTIM1_REFRCNT, POS_ACTIM1_REFRCNT);
        #else
        mcSET_FIELD(u4value, 0xff, MASK_ACTIM1_REFRCNT, POS_ACTIM1_REFRCNT);
        #endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_ACTIM1), u4value);

        mcDELAY_MS(1);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);

        mcSHOW_ERR_MSG(("MRR(MR4) Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
    }
#endif

#ifdef tREFI_DIV4_MANUAL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x114), &u4value);
        mcSET_BIT(u4value, 31);
        mcSET_FIELD(u4value, 5, 0x70000000, 28);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x114), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        mcSHOW_ERR_MSG(("MRR(MR4) Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
#endif

#ifdef tREFI_DIV2_MANUAL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x114), &u4value);
        mcSET_BIT(u4value, 31);
        mcSET_FIELD(u4value, 4, 0x70000000, 28);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x114), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        mcSHOW_ERR_MSG(("MRR(MR4) Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
#endif

#ifdef tREFI_DIV1_MANUAL
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x114), &u4value);
	#ifdef tREFI_DIV1_MANUAL_ENABLE
        mcSET_BIT(u4value, 31);
	#endif
        mcSET_FIELD(u4value, 3, 0x70000000, 28);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x114), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x114), &u4value);
        mcSHOW_ERR_MSG(("MRR(MR4) Reg.0x114=%x\n", u4value));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        mcSHOW_ERR_MSG(("MRR(MR4) Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
#endif

    if (p->dram_type == TYPE_LPDDR3)
    {
    // Disable Per-bank Refresh when refresh rate >= 5 (only for LPDDR3)
        // Set (0x110[6])
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RKCFG), &u4value);
        mcSET_BIT(u4value, POS_RKCFG_PBREF_DISBYRATE);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_RKCFG), u4value);
    }



#ifdef CLK_UNGATING
        // Edward test for ungating.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
        mcSET_BIT(u4value, 3);
        mcSET_BIT(u4value, 26);
        mcCLR_BIT(u4value, 25);
        mcCLR_BIT(u4value, 30);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x130), &u4value);
        mcSET_BIT(u4value, 29);
        mcSET_BIT(u4value, 28);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x130), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1ec), &u4value);
        mcSET_BIT(u4value, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), u4value);

#else

	#if defined(DRAMCLK_NO_GATING) || defined(COMBPHY_GATING_SAME_DDRPHY)
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
	#ifdef DRAMCLK_NO_GATING
        mcSET_BIT(u4value, 26);
	#endif
	#ifdef COMBPHY_GATING_SAME_DDRPHY
        mcSET_BIT(u4value, 3);
	#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);
	#endif
#endif

#ifdef DCM_ENABLE
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1dc), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), u4value);
#endif

}

void DramcRunTimeConfig(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
    U32 u4value;

    // Enable HW gating here?!
#ifdef HW_GATING
    // 0x1c0[31]
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), &u4value);
    mcSET_BIT(u4value, POS_DQSCAL0_STBCALEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_DQSCAL0), u4value);
#endif


#ifdef CLK_UNGATING
        // Edward test for ungating.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
        mcCLR_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);

#else
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x63c), &u4value);
	#ifdef ENABLE_DDYPHY_GATING
        mcSET_BIT(u4value, 2);	//DDRPHY C/A M_CK clock gating enable
        mcSET_BIT(u4value, 1);	//DDRPHY DQ M_CK clock gating enable
	#else
        mcCLR_BIT(u4value, 2);	//DDRPHY C/A M_CK clock gating enable
        mcCLR_BIT(u4value, 1);	//DDRPHY DQ M_CK clock gating enable
	#endif
	#ifdef DUMMYPAD_PD_DISABLE
	 mcSET_BIT(u4value, 3);
	#endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x63c), u4value);

	#ifdef ENABLE_05PHY_CMDCG
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x68c), &u4value);
        mcSET_BIT(u4value, 2);	//PoP 05PHY CMDdynamic clock gating
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x68c), u4value);
        #endif
#endif

#ifdef EMI_OPTIMIZATION
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1ec), &u4value);
    #ifdef RWSPLIT_ENABLE
    u4value |= 0x0008cf11;
    #else
    u4value |= 0x00084f11;
    #endif
    #ifdef DISABLE_DUALSCHED
    u4value &= (~0x01);
    #endif
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), u4value);
#endif

#ifdef ZQCS_ENABLE
    if (p->dram_type == TYPE_LPDDR3)
    {
    #ifdef 	ZQCS_DUAL
        // 1. Set (0x1e4[23:16] ZQCSCNT)=0
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e4), &u4value);
        mcSET_FIELD(u4value, 0x00, 0x00ff0000, 16);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), u4value);
    	// 2. Set (0x1ec[24] ZQCSMASK) for different channels.
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1ec), &u4value);
    	if (p->channel==CHANNEL_A)
    	{
	        mcCLR_BIT(u4value, 24);
    	}
    	else
    	{
	        mcSET_BIT(u4value, 24);
    	}
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), u4value);
	// 3. Enable (0x1ec[25] ZQCSDUAL)
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1ec), &u4value);
        mcSET_BIT(u4value, 25);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), u4value);
    #endif
        // Set (0x1e4[23:16] ZQCSCNT)
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e4), &u4value);
    #ifdef STRESS_MR4_ZQCS
        mcSET_FIELD(u4value, 0x08, 0x00ff0000, 16);
    #else
        mcSET_FIELD(u4value, 0xff, 0x00ff0000, 16);
    #endif
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), u4value);
    }
#endif
}

#ifdef SPM_CONTROL_AFTERK
void TransferToSPMControl(void)
{
	// 0x1020900c
	// Bit 7 : MPLL_PWR_SEL
	// Bit 11 : MPLL_ISO_SEL
	// Bit 15 : MPLL_EN_SEL
	(*(volatile unsigned int *)(0x1020900c)) &= ~((0x01<<7) | (0x01<<11) | (0x01<<15));

	// 0x1000f5cc
	// Bit 0 : 1: use ddrphy reg to control MEMPLL-ISO/sc_apbias_off; 0: use spm to control
	// Bit 4 : 1: bypass MEMPLL-ISO delay chain; 0: not bypass
	// Bit 8 : 1: use ddrphy register to control DIV2 SYNC; 0: use spm to control
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5cc))) &= ~((0x01<<0) | (0x01<<8));

	// 0x100125cc
	// Bit 0 : 1: use ddrphy reg to control MEMPLL-ISO/sc_apbias_off; 0: use spm to control
	// Bit 8 : 1: use ddrphy register to control DIV2 SYNC; 0: use spm to control
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5cc))) &= ~((0x01<<0) | (0x01<<8));

	// 0x1000f5c8
	// Bit 0 : 1: use ddrphy register to control ALLCLK_EN/mempllout_off; 0: use spm to control
	// Bit 9 : 1: use ddrphy register to control MEMPLL2_EN/mempll1_off; 0: use spm to control
	// Bit 10 : 1: use ddrphy register to control MEMPLL3_EN/mempll1_off; 0: use spm to control
	// Bit 11 : 1: use ddrphy register to control MEMPLL4_EN/mempll1_off; 0: use spm to control
	// Bit 16 : 1: use ddrphy register to control MEMPLL_BIAS_LPF_EN/mempll2_off; 0: use spm to control
	// Bit 24 : 1: use ddrphy register to control MEMPLL_BIAS_EN/sc_apbias_off; 0: use spm to control
	//(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) &= ~((0x01<<0) | (0x01<<9) | (0x01<<10) | (0x01<<11) | (0x01<<16) | (0x01<<24));
	#ifdef CHA_MEMPLL3_DISABLE
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) &= ~((0x01<<9) | (0x01<<11) | (0x01<<16) | (0x01<<24));
	#else
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) &= ~((0x01<<9) | (0x01<<10) | (0x01<<11) | (0x01<<16) | (0x01<<24));
	#endif

	// 0x100125c8
	// Bit 0 : 1: use ddrphy register to control ALLCLK_EN/mempllout_off; 0: use spm to control
	// Bit 9 : 1: use ddrphy register to control MEMPLL2_EN/mempll1_off; 0: use spm to control
	// Bit 10 : 1: use ddrphy register to control MEMPLL3_EN/mempll1_off; 0: use spm to control
	// Bit 11 : 1: use ddrphy register to control MEMPLL4_EN/mempll1_off; 0: use spm to control
	// Bit 16 : 1: use ddrphy register to control MEMPLL_BIAS_LPF_EN/mempll2_off; 0: use spm to control
	// Bit 24 : 1: use ddrphy register to control MEMPLL_BIAS_EN/sc_apbias_off; 0: use spm to control
	//(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5c8))) &= ~((0x01<<0) | (0x01<<9) | (0x01<<10) | (0x01<<11) | (0x01<<16) | (0x01<<24));
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5c8))) &= ~((0x01<<9) | (0x01<<10) | (0x01<<11) | (0x01<<16) | (0x01<<24));

}

void TransferToRegControl(void)
{
	// 0x1020900c
	// Bit 7 : MPLL_PWR_SEL
	// Bit 11 : MPLL_ISO_SEL
	// Bit 15 : MPLL_EN_SEL
	(*(volatile unsigned int *)(0x1020900c)) |= ((0x01<<7) | (0x01<<11) | (0x01<<15));

	// 0x1000f5cc
	// Bit 0 : 1: use ddrphy reg to control MEMPLL-ISO/sc_apbias_off; 0: use spm to control
	// Bit 4 : 1: bypass MEMPLL-ISO delay chain; 0: not bypass
	// Bit 8 : 1: use ddrphy register to control DIV2 SYNC; 0: use spm to control
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5cc))) |= ((0x01<<0) | (0x01<<8));

	// 0x100125cc
	// Bit 0 : 1: use ddrphy reg to control MEMPLL-ISO/sc_apbias_off; 0: use spm to control
	// Bit 8 : 1: use ddrphy register to control DIV2 SYNC; 0: use spm to control
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5cc))) |= ((0x01<<0) | (0x01<<8));

	// 0x1000f5c8
	// Bit 0 : 1: use ddrphy register to control ALLCLK_EN/mempllout_off; 0: use spm to control
	// Bit 9 : 1: use ddrphy register to control MEMPLL2_EN/mempll1_off; 0: use spm to control
	// Bit 10 : 1: use ddrphy register to control MEMPLL3_EN/mempll1_off; 0: use spm to control
	// Bit 11 : 1: use ddrphy register to control MEMPLL4_EN/mempll1_off; 0: use spm to control
	// Bit 16 : 1: use ddrphy register to control MEMPLL_BIAS_LPF_EN/mempll2_off; 0: use spm to control
	// Bit 24 : 1: use ddrphy register to control MEMPLL_BIAS_EN/sc_apbias_off; 0: use spm to control
	#ifdef CHA_MEMPLL3_DISABLE
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) |= ((0x01<<9) | (0x01<<11) | (0x01<<16) | (0x01<<24));
	#else
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) |= ((0x01<<9) | (0x01<<10) | (0x01<<11) | (0x01<<16) | (0x01<<24));
	#endif

	// 0x100125c8
	// Bit 0 : 1: use ddrphy register to control ALLCLK_EN/mempllout_off; 0: use spm to control
	// Bit 9 : 1: use ddrphy register to control MEMPLL2_EN/mempll1_off; 0: use spm to control
	// Bit 10 : 1: use ddrphy register to control MEMPLL3_EN/mempll1_off; 0: use spm to control
	// Bit 11 : 1: use ddrphy register to control MEMPLL4_EN/mempll1_off; 0: use spm to control
	// Bit 16 : 1: use ddrphy register to control MEMPLL_BIAS_LPF_EN/mempll2_off; 0: use spm to control
	// Bit 24 : 1: use ddrphy register to control MEMPLL_BIAS_EN/sc_apbias_off; 0: use spm to control
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5c8))) |= ((0x01<<9) | (0x01<<10) | (0x01<<11) | (0x01<<16) | (0x01<<24));
}

#endif

//-------------------------------------------------------------------------
/** DramcEngine1
 *  start the self test engine inside dramc to test dram w/r.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  test2_0         (U32): 16bits,set pattern1 [31:24] and set pattern0 [23:16].
 *  @param  test2_1         (U32): 28bits,base address[27:0].
 *  @param  test2_2         (U32): 28bits,offset address[27:0].
 *  @param  loopforever     (S16):  0 read\write one time ,then exit
 *                                 >0 enable eingie1, after "loopforever" second ,write log and exit
 *                                 -1 loop forever to read\write, every "period" seconds ,check result ,only when we find error,write log and exit
 *                                 -2 loop forever to read\write, every "period" seconds ,write log ,only when we find error,write log and exit
 *                                 -3 just enable loop forever ,then exit
 *  @param period           (U8):  it is valid only when loopforever <0; period should greater than 0
 *  @retval status          (U32): return the value of DM_CMP_ERR  ,0  is ok ,others mean  error
 */
//-------------------------------------------------------------------------
U32 DramcEngine1(DRAMC_CTX_T *p, U32 test2_1, U32 test2_2, S16 loopforever, U8 period)
{
    // This function may not need to be modified unless test engine-1 design has changed

    U8 ucengine_status;
    U8 ucstatus = 0, ucnumber = 0;
    U32 u4value, u4result = 0xffffffff;
    U8 ucloop_count = 0;

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return u4result;;
    }

    // This is TA1 limitation
    // offset must be 0x7ff
    if ((test2_2&0x00ffffff) != 0x000007ff)
    {
        mcSHOW_ERR_MSG(("TA1 offset must be 0x7ff!!\n"));
        mcSET_FIELD(test2_2, 0x7ff, 0x00ffffff, 0);
        mcSHOW_DBG_MSG2(("Force test2_2 to 0x%8x\n", test2_2));
    }

    // we get the status
    // loopforever    period    status    mean
    //     0             x         1       read\write one time ,then exit ,don't write log
    //    >0             x         2       read\write in a loop,after "loopforever" seconds ,disable it ,return the R\W status
    //    -1            >0         3       read\write in a loop,every "period" seconds ,check result ,only when we find error,write log and exit
    //    -2            >0         4       read\write in a loop,every "period" seconds ,write log ,only when we find error,write log and exit
    //    -3             x         5       just enable loop forever , then exit (so we should disable engine1 outside the function)
    if (loopforever == 0)
    {
        ucengine_status = 1;
    }
    else if (loopforever > 0)
    {
        ucengine_status = 2;
    }
    else if (loopforever == -1)
    {
        if (period > 0)
        {
            ucengine_status = 3;
        }
        else
        {
            mcSHOW_ERR_MSG(("parameter 'status' should be equal or greater than 0\n"));
            return u4result;
        }
    }
    else if (loopforever == -2)
    {
        if (period > 0)
        {
            ucengine_status = 4;
        }
        else
        {
            mcSHOW_ERR_MSG(("parameter 'status' should be equal or greater than 0\n"));
            return u4result;
        }
    }
    else if (loopforever == -3)
    {
        ucengine_status = 5;
    }
    else
    {
        mcSHOW_ERR_MSG(("wrong parameter!\n"));
        mcSHOW_ERR_MSG(("loopforever    period    status    mean \n"));
        mcSHOW_ERR_MSG(("      0                x           1         read/write one time ,then exit ,don't write log\n"));
        mcSHOW_ERR_MSG(("    >0                x           2         read/write in a loop,after [loopforever] seconds ,disable it ,return the R/W status\n"));
        mcSHOW_ERR_MSG(("    -1              >0           3         read/write in a loop,every [period] seconds ,check result ,only when we find error,write log and exit\n"));
        mcSHOW_ERR_MSG(("    -2              >0           4         read/write in a loop,every [period] seconds ,write log ,only when we find error,write log and exit\n"));
        mcSHOW_ERR_MSG(("    -3                x           5         just enable loop forever , then exit (so we should disable engine1 outside the function)\n"));
        return u4result;
    }

    // set ADRDECEN=0,address decode not by DRAMC
    //2012/10/03, the same as A60806, for TA&UART b'31=1; for TE b'31=0
    //2013/7/9, for A60808, always set to 1
#if 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcCLR_BIT(u4value, POS_LPDDR2_ADRDECEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);
#endif

    // step
    // 1.select loop forever or not
    // 2.set pattern, base address,offset address
    // 3.enable test1
    // 4.run different code according status
    // 5.diable test1
    // 6.return DM_CMP_ERR

    if (ucengine_status == 4)
    {
        mcSHOW_DBG_MSG(("============================================\n"));
        mcSHOW_DBG_MSG(("enable test egine1 loop forever\n"));
        mcSHOW_DBG_MSG(("============================================\n"));
        ucnumber = 1;
    }

    // 1.
    if (loopforever != 0)
    {
        // enable infinite loop
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), &u4value);
        mcSET_BIT(u4value, POS_CONF1_TESTLP);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), u4value);
    }
    else
    {
        // disable infinite loop
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), &u4value);
        mcCLR_BIT(u4value, POS_CONF1_TESTLP);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF1), u4value);
    }
    // 2.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_1), test2_1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_2), test2_2);
    // 3.
    // enable test engine 1 (first write and then read???)
    // disable it before enable ,DM_CMP_ERR may not be 0,because may be loopforever and don't disable it before
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
    mcCLR_BIT(u4value, POS_CONF2_TEST1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);

    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
    mcSET_BIT(u4value, POS_CONF2_TEST1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);
    // 4.
    if (ucengine_status == 1)
    {
        // read data compare ready check
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
        // infinite loop??? check DE about the time???
        ucloop_count = 0;
        while(mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_CPT) == 0)
        {
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
 /*           mcDELAY_MS(CMP_CPT_POLLING_PERIOD);
            ucloop_count++;
            if (ucloop_count > MAX_CMP_CPT_WAIT_LOOP)
            {
                mcSHOW_ERR_MSG(("TESTRPT_DM_CMP_CPT polling timeout\n"));
                break;
            }*/
        }

        // delay 10ns after ready check from DE suggestion (1ms here)
        //mcDELAY_MS(1);
        mcDELAY_US(10);

        // save  DM_CMP_ERR, 0 is ok ,others are fail,disable test engine 1
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
        u4result = mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_ERR);
        //mcSHOW_DBG_MSG2(("0x3fc = %x\n", u4value));
    }
    else if (ucengine_status == 2)
    {
        // wait "loopforever" seconds
        mcDELAY_MS(loopforever*1000);
        // get result, no need to check read data compare ready???
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
        u4result = mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_ERR);
    }
    else if (ucengine_status == 3)
    {
        while(1)
        {
            // wait "period" seconds
            mcDELAY_MS(period*1000);
            // get result
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
            u4result = mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_ERR);
            if (u4result == 0)
            {
                // pass, continue to check
                continue;
            }
            // some bit error
            // write log
            mcSHOW_DBG_MSG(("%d#    CMP_ERR = 0x%8x\n", ucnumber, u4result));
            break;
        }
    }
    else if (ucengine_status == 4)
    {
        while(1)
        {
            // wait "period" seconds
            mcDELAY_MS(period*1000);
            // get result
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
            u4result = mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_ERR);

            // write log
            mcSHOW_DBG_MSG(("%d#    CMP_ERR = 0x%8x\n", ucnumber, u4result));

            if (u4result == 0)
            {
                // pass, continue to check
                continue;
            }
            // some bit error
            break;
        }
    }
    else if (ucengine_status == 5)
    {
        // loopforever is  enable ahead ,we just exit this function
        return 0;
    }
    else
    {
    }

    // 5. disable engine1
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
    mcCLR_BIT(u4value, POS_CONF2_TEST1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);

    // 6.
    // set ADRDECEN to 1
    //2013/7/9, for A60808, always set to 1
#if 0
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcSET_BIT(u4value, POS_LPDDR2_ADRDECEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);
#endif

    return u4result;
}

//-------------------------------------------------------------------------
/** DramcEngine2
 *  start the self test engine 2 inside dramc to test dram w/r.
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param  wr              (DRAM_TE_OP_T): TE operation
 *  @param  test2_1         (U32): 28bits,base address[27:0].
 *  @param  test2_2         (U32): 28bits,offset address[27:0]. (unit is 16-byte, i.e: 0x100 is 0x1000).
 *  @param  loopforever     (S16): 0    read\write one time ,then exit
 *                                >0 enable eingie2, after "loopforever" second ,write log and exit
 *                                -1 loop forever to read\write, every "period" seconds ,check result ,only when we find error,write log and exit
 *                                -2 loop forever to read\write, every "period" seconds ,write log ,only when we find error,write log and exit
 *                                -3 just enable loop forever ,then exit
 *  @param period           (U8):  it is valid only when loopforever <0; period should greater than 0
 *  @param log2loopcount    (U8): test loop number of test agent2 loop number =2^(log2loopcount) ,0 one time
 *  @retval status          (U32): return the value of DM_CMP_ERR  ,0  is ok ,others mean  error
 */
//-------------------------------------------------------------------------
U32 DramcEngine2(DRAMC_CTX_T *p, DRAM_TE_OP_T wr, U32 test2_1, U32 test2_2, U8 testaudpat, S16 loopforever, U8 period, U8 log2loopcount)
{
    U8 ucengine_status;
    U8 ucstatus = 0, ucloop_count = 0;
    U32 u4value, u4result = 0xffffffff;
    U32 u4log2loopcount = (U32) log2loopcount;

    // error handling
    if (!p)
    {
        mcSHOW_ERR_MSG(("context is NULL\n"));
        return u4result;
    }

    // check loop number validness
//    if ((log2loopcount > 15) || (log2loopcount < 0))		// U8 >=0 always.
    if (log2loopcount > 15)
    {
        mcSHOW_ERR_MSG(("wrong parameter log2loopcount:    log2loopcount just 0 to 15 !\n"));
        return u4result;
    }

    // disable self test engine1 and self test engine2
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
    mcCLR_MASK(u4value, MASK_CONF2_TE12_ENABLE);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);

    // we get the status
    // loopforever    period    status    mean
    //     0             x         1       read\write one time ,then exit ,don't write log
    //    >0             x         2       read\write in a loop,after "loopforever" seconds ,disable it ,return the R\W status
    //    -1            >0         3       read\write in a loop,every "period" seconds ,check result ,only when we find error,write log and exit
    //    -2            >0         4       read\write in a loop,every "period" seconds ,write log ,only when we find error,write log and exit
    //    -3             x         5       just enable loop forever , then exit (so we should disable engine1 outside the function)
    if (loopforever == 0)
    {
        ucengine_status = 1;
    }
    else if (loopforever > 0)
    {
        ucengine_status = 2;
    }
    else if (loopforever == -1)
    {
        if (period > 0)
        {
            ucengine_status = 3;
        }
        else
        {
            mcSHOW_ERR_MSG(("parameter 'period' should be equal or greater than 0\n"));
            return u4result;
        }
    }
    else if (loopforever == -2)
    {
        if (period > 0)
        {
            ucengine_status = 4;
        }
        else
        {
            mcSHOW_ERR_MSG(("parameter 'period' should be equal or greater than 0\n"));
            return u4result;
        }
    }
    else if (loopforever == -3)
    {
        if (period > 0)
        {
            ucengine_status = 5;
        }
        else
        {
            mcSHOW_ERR_MSG(("parameter 'period' should be equal or greater than 0\n"));
            return u4result;
        }
    }
    else
    {
        mcSHOW_ERR_MSG(("parameter 'loopforever' should be 0 -1 -2 -3 or greater than 0\n"));
        return u4result;
    }

    // set ADRDECEN=0, address decode not by DRAMC
    //2012/10/03, the same as A60806, for TA&UART b'31=1; for TE b'31=0
    //2013/7/9, for A60808, always set to 1
#ifdef fcFOR_A60806_TEST
    ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
    mcCLR_BIT(u4value, POS_LPDDR2_ADRDECEN);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);
#endif

    // 1.set pattern ,base address ,offset address
    // 2.select  ISI pattern or audio pattern or xtalk pattern
    // 3.set loop number
    // 4.enable read or write
    // 5.loop to check DM_CMP_CPT
    // 6.return CMP_ERR
    // currently only implement ucengine_status = 1, others are left for future extension
    /*if (ucengine_status == 4)
    {
        mcSHOW_DBG_MSG(("============================================\n"));
        mcSHOW_DBG_MSG(("enable test egine2 loop forever\n"));
        mcSHOW_DBG_MSG(("============================================\n"));
    }*/
    u4result = 0;
    while(1)
    {
        // 1
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_1), test2_1);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_2), test2_2);

        // 2 & 3
        // (TESTXTALKPAT, TESTAUDPAT) = 00 (ISI), 01 (AUD), 10 (XTALK), 11 (UNKNOWN)
        if (testaudpat == 2)   // xtalk
        {
            // select XTALK pattern
            // set addr 0x044 [7] to 0
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4value);
            mcCLR_BIT(u4value, POS_TEST2_3_TESTAUDPAT);
            mcSET_FIELD(u4value, u4log2loopcount, MASK_TEST2_3_TESTCNT, POS_TEST2_3_TESTCNT);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4value);

            // set addr 0x48[16] to 1, TESTXTALKPAT = 1
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), &u4value);
            mcSET_BIT(u4value,POS_TEST2_4_TESTXTALKPAT);
            mcCLR_BIT(u4value, POS_TEST2_4_TESTAUDBITINV);
            mcCLR_BIT(u4value, POS_TEST2_4_TESTAUDMODE);  // for XTALK pattern characteristic, we don' t enable write after read
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), u4value);
        }
        else if (testaudpat == 1)   // audio
        {
            // set AUDINIT=0x11 AUDINC=0x0d AUDBITINV=1 AUDMODE=1
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), &u4value);
            mcSET_FIELD(u4value, 0x00000011, MASK_TEST2_4_TESTAUDINIT, POS_TEST2_4_TESTAUDINIT);
            mcSET_FIELD(u4value, 0x0000000d, MASK_TEST2_4_TESTAUDINC, POS_TEST2_4_TESTAUDINC);
            mcSET_BIT(u4value, POS_TEST2_4_TESTAUDBITINV);
            mcSET_BIT(u4value, POS_TEST2_4_TESTAUDMODE);
            mcCLR_BIT(u4value,POS_TEST2_4_TESTXTALKPAT);     // Edward : This bit needs to be disable in audio. Otherwise will fail.
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), u4value);

            // set addr 0x044 [7] to 1 ,select audio pattern
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4value);
            mcSET_BIT(u4value, POS_TEST2_3_TESTAUDPAT);
            mcSET_FIELD(u4value, u4log2loopcount, MASK_TEST2_3_TESTCNT, POS_TEST2_3_TESTCNT);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4value);
        }
        else   // ISI
        {
            // select ISI pattern
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), &u4value);
            mcCLR_BIT(u4value, POS_TEST2_3_TESTAUDPAT);
            mcSET_FIELD(u4value, u4log2loopcount, MASK_TEST2_3_TESTCNT, POS_TEST2_3_TESTCNT);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_3), u4value);

            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), &u4value);
            mcCLR_BIT(u4value,POS_TEST2_4_TESTXTALKPAT);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), u4value);
        }

        // 4
        if (wr == TE_OP_READ_CHECK)
        {
            if ((testaudpat == 1) || (testaudpat == 2))
            {
                //if audio pattern, enable read only (disable write after read), AUDMODE=0x48[15]=0
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), &u4value);
                mcCLR_BIT(u4value, POS_TEST2_4_TESTAUDMODE);
                ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TEST2_4), u4value);
            }

            // enable read, 0x008[31:29]
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
            mcSET_FIELD(u4value, (U32) 2, MASK_CONF2_TE12_ENABLE, POS_CONF2_TEST1);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);
        }
        else if (wr == TE_OP_WRITE_READ_CHECK)
        {
            // enable write
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
            mcSET_FIELD(u4value, (U32) 4, MASK_CONF2_TE12_ENABLE, POS_CONF2_TEST1);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);

            // read data compare ready check
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
            ucloop_count = 0;
            while(mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_CPT) == 0)
            {
                ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
/*            mcDELAY_US(CMP_CPT_POLLING_PERIOD);
                ucloop_count++;
                if (ucloop_count > MAX_CMP_CPT_WAIT_LOOP)
                {
                    //mcSHOW_ERR_MSG(("TESTRPT_DM_CMP_CPT polling timeout: %d\n", ucloop_count));
                #ifndef fcWAVEFORM_MEASURE // for testing, waveform ,measurement
                    break;
                #endif
                }*/
            }

            // disable write
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
            mcSET_FIELD(u4value, (U32) 0, MASK_CONF2_TE12_ENABLE, POS_CONF2_TEST1);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);

            // enable read
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
            mcSET_FIELD(u4value, (U32) 2, MASK_CONF2_TE12_ENABLE, POS_CONF2_TEST1);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);
        }

        // 5
        // read data compare ready check
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
        ucloop_count = 0;
        while(mcCHK_BIT1(u4value, POS_TESTRPT_DM_CMP_CPT) == 0)
        {
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_TESTRPT), &u4value);
/*            mcDELAY_US(CMP_CPT_POLLING_PERIOD);
            ucloop_count++;
            if (ucloop_count > MAX_CMP_CPT_WAIT_LOOP)
            {
                mcSHOW_ERR_MSG(("TESTRPT_DM_CMP_CPT polling timeout\n"));
            #ifndef fcWAVEFORM_MEASURE // for testing, waveform ,measurement
                break;
            #endif
            }*/
        }

        // delay 10ns after ready check from DE suggestion (1ms here)
        mcDELAY_US(1);

        // 6
        // return CMP_ERR, 0 is ok ,others are fail,diable test2w or test2r
        // get result
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CMP_ERR), &u4value);
        // or all result
        u4result |= u4value;
        // disable read
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), &u4value);
        mcCLR_MASK(u4value, MASK_CONF2_TE12_ENABLE);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_CONF2), u4value);

        // handle status
        if (ucengine_status == 1)
        {
            // set ADRDECEN to 1
            //2013/7/9, for A60808, always set to 1
        #ifdef fcFOR_A60806_TEST
            ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), &u4value);
            mcSET_BIT(u4value, POS_LPDDR2_ADRDECEN);
            ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(DRAMC_REG_LPDDR2), u4value);
        #endif
            break;
        }
        else if (ucengine_status == 2)
        {
            mcSHOW_ERR_MSG(("not support for now\n"));
            break;
        }
        else if (ucengine_status == 3)
        {
            mcSHOW_ERR_MSG(("not support for now\n"));
            break;
        }
        else if (ucengine_status == 4)
        {
            mcSHOW_ERR_MSG(("not support for now\n"));
            break;
        }
        else if (ucengine_status == 5)
        {
            mcSHOW_ERR_MSG(("not support for now\n"));
            break;
        }
        else
        {
            mcSHOW_ERR_MSG(("not support for now\n"));
            break;
        }
    }

    return u4result;
}

//-------------------------------------------------------------------------
/** DramcRegDump
 *  Dump all registers (DDRPHY and DRAMC)
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @retval status          (DRAM_STATUS_T): DRAM_OK or DRAM_FAIL
 */
//-------------------------------------------------------------------------
DRAM_STATUS_T DramcRegDump(DRAMC_CTX_T *p)
{
    U8 ucstatus = 0;
//    U16 ii;
//    U32 u4addr, u4value;
#if 0
    mcSHOW_DBG_MSG(("\n=================A60808 PLL/PHY register value================================"));
    for (ii=0; ii<0x4f4; ii=ii+4)
    {
        // confirm SoC platform has "%" operation
        if (ii%24==0)
        {
            mcSHOW_DBG_MSG(("\n0x%8x | ", mcSET_PHY_REG_ADDR(0x000)+ii));
        }
        u4addr = mcSET_PHY_REG_ADDR(0x000)+ii;
        ucstatus |= ucDram_Register_Read(u4addr, &u4value);
        mcSHOW_DBG_MSG((" %8x", u4value));
    }

    mcSHOW_DBG_MSG(("\n=================A60808 PLL Wrapper register value================================"));
    for (ii=0xa4c; ii<0xb98; ii=ii+4)
    {
        // confirm SoC platform has "%" operation
        if ((ii-0xa4c)%24==0)
        {
            mcSHOW_DBG_MSG(("\n0x%8x | ", mcSET_PHY_REG_ADDR(0x000)+ii));
        }
        u4addr = mcSET_PHY_REG_ADDR(0x000)+ii;
        ucstatus |= ucDram_Register_Read(u4addr, &u4value);
        mcSHOW_DBG_MSG((" %8x", u4value));
    }

    mcSHOW_DBG_MSG(("\n mcSET_PHY_REG_ADDR(0xf94) | "));
    u4addr = mcSET_PHY_REG_ADDR(0xf94);
    ucstatus |= ucDram_Register_Read(u4addr, &u4value);
    mcSHOW_DBG_MSG((" %8x", u4value));

    mcSHOW_DBG_MSG(("\n=========A60808 dramc register value==========================="));
    for (ii=0; ii<0x400; ii=ii+4)
    {
        // confirm SoC platform has "%" operation
        if (ii%24==0)
        {
            mcSHOW_DBG_MSG(("\n0x%8x | ", mcSET_DRAMC_REG_ADDR(ii)));
        }
        u4addr = mcSET_DRAMC_REG_ADDR(ii);
        ucstatus |= ucDram_Register_Read(u4addr, &u4value);
        mcSHOW_DBG_MSG((" %8x", u4value));
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

    // log example
    /*
    =================A60808 PLL/PHY register value================================
mcSET_PHY_REG_ADDR(0x000) |  80178017   550000        0        0        0        0
0x20001018 |  80178017  1458017 80170145 80178017   558017 80170055
0x20001030 |         0        0        2   900002   8e002e ff000100
0x20001048 |  803df088       b8 b86f0080 80250080 80250080 80250080
0x20001060 |  80250080 80250080 80250080 80250080 80250080 80250080
0x20001078 |  80250080 80250080 80250080 80250080 80250080 80250080
0x20001090 |  80250080 80250080 80250080 80250080 80250080 80250080
0x200010a8 |  80250080 80250080 80650080 80250080 802500b8 b8654000
0x200010c0 |         0      534 18838000    10003  d0b0d0b        0
0x200010d8 |    230000        0 f0001717     b8b8      300 2e800000
0x200010f0 |         0        0        0        0        0        0
0x20001108 |         0        0        0        0        0        0
0x20001120 |         0        0        0        0        0        0
0x20001138 |         0        0        0        0        0        0
0x20001150 |         0        0        0        0        0        0
0x20001168 |         0        0        0        0        0        0
0x20001180 |         0        0        0        0        0        0
0x20001198 |         0        0        0        0        0        0
0x200011b0 |         0        0        0        0        0        0
0x200011c8 |         0        0        0        0        0        0
0x200011e0 |         0        0        0        0        0        0
0x200011f8 |         0        0 80178017   550000        0        0
0x20001210 |         0        0 80178017  1458017 80170145 80178017
0x20001228 |    558017 80170055        0        0        2   8a0002
0x20001240 |    87002e ff000100 8000e088       b8 b86f8017 80170055
0x20001258 |         0        0        0        0 80178017  1458017
0x20001270 |  80170145 80178017   558017 80170055        0        0
0x20001288 |         2   980002   98002e ff000100 8000e088       b8
0x200012a0 |  b86f1050  d997883 8000100c 10ff01c2 57570000  29a1883
0x200012b8 |  80000100  2000000  29a1883 80000001    30c02  d000000
0x200012d0 |  23000000 c0000a0d  a0e0000 23000000 c0000600        0
0x200012e8 |  362a0ba0        0        0        0        0        0
0x20001300 |         0        0        0        0        0        0
0x20001318 |         0        0        0        0        0        0
0x20001330 |         0        0        0        0        0        0
0x20001348 |         0        0        0        0        0        0
0x20001360 |         0        0        0        0        0        0
0x20001378 |         0        0        0        0        0        0
0x20001390 |         0        0        0        0        0        0
0x200013a8 |         0        0        0        0        0        0
0x200013c0 |         0        0        0        0        0        0
0x200013d8 |         0        0        0        0        0        0
0x200013f0 |         0        0        0        0 80178017   550000
0x20001408 |         0        0        0        0 80178017  1458017
0x20001420 |  80170145 80178017   558017 80170055        0        0
0x20001438 |         2   8a0002   8b002e ff000100 8000e088       b8
0x20001450 |  b86f0080 80250080 80250080 80020080 80250080 80250080
0x20001468 |  80250080 80250080 80250080 80250080 80250080 80250080
0x20001480 |  80250080 80250080 80250080 80250080 80250080 80250080
0x20001498 |  80250080 80250080 80250080 80250080 80250080 80250080
0x200014b0 |  80650080 80250080 802500b8 b8654000        0      534
0x200014c8 |  18838000    10003  a090b08        0   230000        0
0x200014e0 |  f0001717     b8b8      300 2e800000        0
=================A60808 PLL Wrapper register value================================
0x20001a4c |  83446655     2800  1100051        0        0        0
0x20001a64 |         0        0        0        0        0        0
0x20001a7c |         0        0        0        0        0 b04c033e
0x20001a94 |         0        0        0        0        0        0
0x20001aac |         0        0        0        0        0        0
0x20001ac4 |         0        0        0        0        0        0
0x20001adc |         0        0        0        0        0        0
0x20001af4 |         0        0        0 aa220000        0 aa220000
0x20001b0c |         0 aa220000        0 aa220000        0        0
0x20001b24 |         0        0        0        0        0        0
0x20001b3c |         0   110000    80300        0        0        0
0x20001b54 |         0        0     1010        0        0        0
0x20001b6c |         0        0        0        0        0        0
0x20001b84 |         0        0        0        0      110
0x20001f94 |       110
=========A60808 dramc register value===========================
0x20002000 |  66fe49ff f07486e3    4794c        0        0        0
0x20002018 |         0        0        0        0 f1200f01 55010000
0x20002030 |  33000fff 55020000 33000fff 55000000 aa000400 28880480
0x20002048 |      d10d        0        0        0        0        0
0x20002060 |         0        0        0        0        0        0
0x20002078 |         0 e28743dd        0        0     2004        1
0x20002090 |         0 40404040        0        0        0        0
0x200020a8 |         0        0        0        0 aa22aa22 aa22aa22
0x200020c0 |         0        0        0        0        0        0
0x200020d8 |    100900 83008008 10008008       b2        0        0
0x200020f0 |         0  1000000 edcb000f 37010000        0        0
0x20002108 |         0        0  b051100        0        0        0
0x20002120 |         0 aa080088        0        0 50000000        0
0x20002138 |         0        0        0        0        0        0
0x20002150 |         0        0        0        0        0        0
0x20002168 |        80        0        0        0        0        0
0x20002180 |         0        0        0        0        0        0
0x20002198 |         0        0        0        0        0        0
0x200021b0 |         0        0        0        0 8000c8b8        0
0x200021c8 |         0        0        0        0   c80000 10622842
0x200021e0 |  88000000        0      690        0        0        0
0x200021f8 |         0        0        0        0        0        0
0x20002210 |   1020102  1020102  1020102  2010102  3010102        0
0x20002228 |   2030303  1030101        0        0        0        0
0x20002240 |         0        0        0        0        0        0
0x20002258 |         0        0        0        0        0        0
0x20002270 |         0        0        0        0        0        0
0x20002288 |         0        0        0        0        0        0
0x200022a0 |         0        0        0        0        0        0
0x200022b8 |         0        0        0        0        0        0
0x200022d0 |         0        0        0        0        0        0
0x200022e8 |         0        0        0        0        0        0
0x20002300 |         0        0        0        0        0        0
0x20002318 |         0        0 ffffffff ffffffff        0   d92fc3
0x20002330 |    d92fc3        0        0        0        0 46e407c0
0x20002348 |       400        0        0        0        0        0
0x20002360 |    d92fc3        0        0        0        0 40404040
0x20002378 |         0        0        0        0        0        3
0x20002390 |         3        3        3        0        0        0
0x200023a8 |         0        0        0        0      300        0
0x200023c0 |         0        0        0        0        0        0
0x200023d8 |         0        0        0        0        0        0
0x200023f0 |         0        0        0        0
   */
}

#ifdef XTALK_SSO_STRESS
//-------------------------------------------------------------------------
/** DramcWorstPat_mem
 *  Write HFID worst pattern to memory
 *  @param p                Pointer of context created by DramcCtxCreate.
 *  @param src_addr         (U32): DRAM src address
 *  @retval status          (U32): return the value of ERR_VALUE, 0 is ok, others mean  error
 */
//-------------------------------------------------------------------------
void DramcWorstPat_mem(DRAMC_CTX_T *p, U32 src_addr)
{
    U8 ii, jj, kk, repeat;
    U32 size=src_addr;

    //mcSHOW_DBG_MSG(("START to write worst pattern to memory...\n"));

    // XTALK worst pattern
    for (ii =0 ; ii < 76; ii++)
    {
        ucDram_Write(src_addr, u4xtalk_pat[ii]);
        src_addr +=4;
    }
    // SSO worst pattern
    //  sso_bs1A
    //   sso1x_bs ~ sso6x_bs
    for (ii = 1; ii <= 6; ii++)
    {
        repeat = 7;

        for (jj = 0; jj <= repeat; jj++)
        {
            for (kk = 0; kk < ii; kk++)
            {
                // send "1"
                ucDram_Write(src_addr, 0xffffffff);
                src_addr +=4;
            }
            for (kk = 0; kk < ii; kk++)
            {
                // send "0"
                ucDram_Write(src_addr, 0x00000000);
                src_addr +=4;
            }
        }

        // step_1x
        ucDram_Write(src_addr, 0x00000000);
        src_addr +=4;
    }

    //   sso6x_bs
    /*
    repeat = 7;
    for (jj = 0; jj <= repeat; jj++)
    {
        for (kk = 0; kk < 6; kk++)
        {
            // send "1"
            ucDram_Write(src_addr, 0xffffffff);
            src_addr +=4;
        }
        for (kk = 0; kk < 6; kk++)
        {
            // send "0"
            ucDram_Write(src_addr, 0x00000000);
            src_addr +=4;
        }
    }*/

    //   step_1y
    ucDram_Write(src_addr, 0xffffffff);
    src_addr +=4;

    //   spb0x
    for (ii =0 ; ii < 64; ii++)
    {
        ucDram_Write(src_addr, u4spb0x[ii]);
        src_addr +=4;
    }

    //  sso_bs2A
    for (ii = 0; ii < 6; ii++)
    {
        repeat = ucsso_bsx[ii][2];

        for (jj = 0; jj <= repeat; jj++)
        {
            for (kk = 0; kk < ucsso_bsx[ii][0]; kk++)
            {
                // send "1"
                ucDram_Write(src_addr, 0xffffffff);
                src_addr +=4;
            }
            for (kk = 0; kk < ucsso_bsx[ii][1]; kk++)
            {
                // send "0"
                ucDram_Write(src_addr, 0x00000000);
                src_addr +=4;
            }
        }

        // END; step_xx
        for (kk = 0; kk < ucsso_bsx[ii][3]; kk++)
        {
            // send "1"
            ucDram_Write(src_addr, 0xffffffff);
            src_addr +=4;
        }
        for (kk = 0; kk < ucsso_bsx[ii][4]; kk++)
        {
            // send "0"
            ucDram_Write(src_addr, 0x00000000);
            src_addr +=4;
        }
    }

//    mcSHOW_DBG_MSG(("END to write worst pattern to memory! addr=%xh %d size=%xh %d\n",
//    	src_addr, src_addr, src_addr-size, src_addr-size));
}

#ifdef NEW_WORST_PAT_64

// According to Yi-Chih, EMI_CONA could not be adjusted during runtime.
void DramcWorstPat_mem_64(DRAMC_CTX_T *p, U32 src_addr)
{
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~0x01);	// disable dual channel.
	dsb();

	p->channel = CHANNEL_A;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) &= (~(0x01<<18));
	dsb();
	DramcWorstPat_mem((DRAMC_CTX_T *)p, (U32)src_addr>>1);

	p->channel = CHANNEL_B;
	*(volatile unsigned *)(EMI_APB_BASE+0x00000000) |= ((0x01<<18));
	dsb();
	DramcWorstPat_mem((DRAMC_CTX_T *)p, (U32)src_addr>>1);

        *(volatile unsigned *)(EMI_APB_BASE+0x00000000) = LPDDR3_EMI_CONA;
	dsb();
}

#else

void DramcWorstPat_mem_64(DRAMC_CTX_T *p, U32 src_addr)
{
    U8 ii, jj, kk, repeat;
    U32 size=src_addr;

    //mcSHOW_DBG_MSG(("START to write worst pattern to memory...\n"));

    // XTALK worst pattern
    for (ii =0 ; ii < 76*2; ii++)
    {
        ucDram_Write(src_addr, u4xtalk_pat_64[ii]);
        src_addr +=4;
    }
    // SSO worst pattern
    //  sso_bs1A
    //   sso1x_bs ~ sso6x_bs
    for (ii = 1; ii <= 6; ii++)
    {
        repeat = 7;

        for (jj = 0; jj <= repeat; jj++)
        {
            for (kk = 0; kk < ii; kk++)
            {
                // send "1"
                ucDram_Write(src_addr, 0xffffffff);
                src_addr +=4;
                ucDram_Write(src_addr, 0xffffffff);
                src_addr +=4;
            }
            for (kk = 0; kk < ii; kk++)
            {
                // send "0"
                ucDram_Write(src_addr, 0x00000000);
                src_addr +=4;
                ucDram_Write(src_addr, 0x00000000);
                src_addr +=4;
            }
        }

        // step_1x
        ucDram_Write(src_addr, 0x00000000);
        src_addr +=4;
        ucDram_Write(src_addr, 0x00000000);
        src_addr +=4;
    }

    //   sso6x_bs
    repeat = 7;
    for (jj = 0; jj <= repeat; jj++)
    {
        for (kk = 0; kk < 6; kk++)
        {
            // send "1"
            ucDram_Write(src_addr, 0xffffffff);
            src_addr +=4;
            ucDram_Write(src_addr, 0xffffffff);
            src_addr +=4;        }
        for (kk = 0; kk < 6; kk++)
        {
            // send "0"
            ucDram_Write(src_addr, 0x00000000);
            src_addr +=4;
            ucDram_Write(src_addr, 0x00000000);
            src_addr +=4;
        }
    }

    //   step_1y
    ucDram_Write(src_addr, 0xffffffff);
    src_addr +=4;
    ucDram_Write(src_addr, 0xffffffff);
    src_addr +=4;

    //   spb0x
    for (ii =0 ; ii < 64*2; ii++)
    {
        ucDram_Write(src_addr, u4spb0x_64[ii]);
        src_addr +=4;
    }

    //  sso_bs2A
    for (ii = 0; ii < 6; ii++)
    {
        repeat = ucsso_bsx[ii][2];

        for (jj = 0; jj <= repeat; jj++)
        {
            for (kk = 0; kk < (ucsso_bsx[ii][0]*2); kk++)
            {
                // send "1"
                ucDram_Write(src_addr, 0xffffffff);
                src_addr +=4;
            }
            for (kk = 0; kk < (ucsso_bsx[ii][1]*2); kk++)
            {
                // send "0"
                ucDram_Write(src_addr, 0x00000000);
                src_addr +=4;
            }
        }

        // END; step_xx
        for (kk = 0; kk < (ucsso_bsx[ii][3]*2); kk++)
        {
            // send "1"
            ucDram_Write(src_addr, 0xffffffff);
            src_addr +=4;
        }
        for (kk = 0; kk < (ucsso_bsx[ii][4]*2); kk++)
        {
            // send "0"
            ucDram_Write(src_addr, 0x00000000);
            src_addr +=4;
        }
    }

//    mcSHOW_DBG_MSG(("END to write worst pattern to memory! addr=%xh %d size=%xh %d\n",
//    	src_addr, src_addr, src_addr-size, src_addr-size));
}

#endif

//U32 uiPatLenB = 3584;  // Xtalk + SSO
U32 uiPatLenB;  // Xtalk + SSO

void DramcWorstPat(DRAMC_CTX_T *p, U32 src_addr, U8 ChannelNum)
{

}

U32 DramcDmaEngine(DRAMC_CTX_T *p, DRAM_DMA_OP_T op, U32 src_addr, U32 dst_addr, U32 trans_len, U8 burst_len, U8 check_result, U8 ChannelNum)
{
	int err =  0;
	unsigned int data, uiPatRange;
	int i;
	unsigned int uiCompErr = 0, len;
	unsigned int *src_buffp1;
	unsigned int *dst_buffp1;

	if (ChannelNum==1)
	{
		uiPatLenB = XTALK_SSO_LEN;
	}
	else
	{
		uiPatLenB = XTALK_SSO_LEN << 1;
	}

	if (src_addr)
	{
		src_buffp1 = src_addr;
	}
	else
	{
		src_buffp1 = 0x40000000;
	}

	if (dst_addr)
	{
		dst_buffp1 = dst_addr;
	}
	else
	{
		dst_buffp1 = 0x50000000;
	}

	len = trans_len;

	if (len < uiPatLenB) {
		// Should be larger.
		len = uiPatLenB;
	}
	// Have the DMA length to be the multiple of uiPatternLen.
	len = ((len+uiPatLenB-1)/uiPatLenB) * uiPatLenB;

#ifdef DMA_FIXED_LEN
	len  = DMA_FIXED_LEN;
#endif

	if (check_result == 0x0f)
	{
		// Only do comparison.
		goto DMA_COMPARE;
	}

	if (check_result != 0)
	{
	for (i = 0 ; i < (len/sizeof(unsigned int)) ; i++) {
	    *((unsigned int *)dst_buffp1+i) = 0;
	}

	if (ChannelNum==1)
	{
		DramcWorstPat_mem((DRAMC_CTX_T *)p, (U32)src_buffp1-DDR_BASE);
	}
	else
	{
		DramcWorstPat_mem_64((DRAMC_CTX_T *)p, (U32)src_buffp1-DDR_BASE);
	}

	for (i = 0 ; i < len ; i+=uiPatLenB) {
		memcpy((void *)src_buffp1 + i, src_buffp1, uiPatLenB);
	}
	}

DMA_INIT:
	 *((unsigned int *)(0x10212c18)) = (DMA_BURST_LEN & 0x07)  << 16; //BURST_LEN:7-8,R/W
	 *((unsigned int *)(0x10212c1c)) = src_buffp1;
	 *((unsigned int *)(0x10212c20)) = dst_buffp1;
	 *((unsigned int *)(0x10212c24)) = len;
	 dsb();
	 *((unsigned int *)(0x10212c08)) = 0x1; //start dma
	 dsb();

#ifdef DMA_ALE_BLOCK_TEST
	//set ALEBLOCK(0x138[5]) to 1 to block EMI's ALE
	//*PDEF_DRAMC0_REG_138 = dramc0_dummy | 0x00000020;
	//*PDEF_DRAMC1_REG_138 = dramc1_dummy | 0x00000020;
	Reg_Sync_Writel(CHA_DRAMCAO_BASE+0x138, Reg_Readl(CHA_DRAMCAO_BASE+0x138) | 0x00000020);
	Reg_Sync_Writel(CHB_DRAMCAO_BASE+0x138, Reg_Readl(CHB_DRAMCAO_BASE+0x138) | 0x00000020);
        mcDELAY_US(200);
	Reg_Sync_Writel(CHA_DRAMCAO_BASE+0x138, Reg_Readl(CHA_DRAMCAO_BASE+0x138) & (~0x00000020));
	Reg_Sync_Writel(CHB_DRAMCAO_BASE+0x138, Reg_Readl(CHB_DRAMCAO_BASE+0x138) & (~0x00000020));
#endif

	if (check_result == 0)
	{
		// Not wait and return.
		return uiCompErr;
	}

DMA_COMPARE:

	 while(*((unsigned int *)(0x10212c08))) ;

#ifdef WAVEFORM_MEASURE
	if (StartMeasureWaveform)
	{
	 goto DMA_INIT;
	}
#endif

	for (i = 0 ; i < (len/sizeof(unsigned int)) ; i++)
	{
		#ifdef DUMMY_READ
		unsigned int DummyRead = *((volatile unsigned int *)dst_buffp1+i);
		DummyRead = *((volatile unsigned int *)dst_buffp1+i);
		DummyRead = *((volatile unsigned int *)src_buffp1+i);
		DummyRead = *((volatile unsigned int *)src_buffp1+i);
		#endif
		if (*((unsigned int *)dst_buffp1+i) != *((unsigned int *)src_buffp1+i))
		{
			err = 1;
	   		uiCompErr |= ((*((unsigned int *)dst_buffp1+i)) ^ (*((unsigned int *)src_buffp1+i)));
			#ifdef DMA_ERROR_DISPLAY
			dbg_print("Source %xh = %xh Destination %xh= %xh diff=%xh\n",
				(unsigned int)((unsigned int *)src_buffp1+i), (*((unsigned int *)src_buffp1+i)),
				(unsigned int)((unsigned int *)dst_buffp1+i), (*((unsigned int *)dst_buffp1+i)),
				(*((unsigned int *)src_buffp1+i)) ^ (*((unsigned int *)dst_buffp1+i)));
			#endif
		}
        }

	#ifdef DMA_ERROR_STOP
	    if (uiCompErr) {
		must_print("Enter to continue...\n");
		while (1) {
			if ( UART_Get_Command() )  {
				 break;
			 }
		}
	    }
	#endif

    return uiCompErr;
}

#ifdef SMALL_SIZE_DMA_TEST
U32 DramcDmaEngineInit(DRAMC_CTX_T *p, DRAM_DMA_OP_T op, U32 src_addr, U32 dst_addr, U32 trans_len, U8 ChannelNum)
{
	int err =  0;
	unsigned int data, uiPatRange;
	int i;
	unsigned int uiCompErr = 0, len;
	unsigned int *src_buffp1;
	unsigned int *dst_buffp1;

	if (ChannelNum==1)
	{
		uiPatLenB = XTALK_SSO_LEN;
	}
	else
	{
		uiPatLenB = XTALK_SSO_LEN << 1;
	}

	if (src_addr)
	{
		src_buffp1 = src_addr;
	}
	else
	{
		src_buffp1 = 0x40000000;
	}

	if (dst_addr)
	{
		dst_buffp1 = dst_addr;
	}
	else
	{
		dst_buffp1 = 0x50000000;
	}

	len = trans_len;

	if (len < uiPatLenB) {
		// Should be larger.
		len = uiPatLenB;
	}
	// Have the DMA length to be the multiple of uiPatternLen.
	len = ((len+uiPatLenB-1)/uiPatLenB) * uiPatLenB;

	for (i = 0 ; i < (len/sizeof(unsigned int)) ; i++) {
	    *((unsigned int *)dst_buffp1+i) = 0;
	}

	if (ChannelNum==1)
	{
		DramcWorstPat_mem((DRAMC_CTX_T *)p, (U32)src_buffp1-DDR_BASE);
	}
	else
	{
		DramcWorstPat_mem_64((DRAMC_CTX_T *)p, (U32)src_buffp1-DDR_BASE);
	}

	for (i = 0 ; i < len ; i+=uiPatLenB) {
		memcpy((void *)src_buffp1 + i, src_buffp1, uiPatLenB);
	}

	return len;
}

U32 DramcDmaEngineTransfer(DRAMC_CTX_T *p, DRAM_DMA_OP_T op, U32 src_addr, U32 dst_addr, U32 trans_len, U8 burst_len, U8 check_result, U8 ChannelNum)
{
	int err =  0;
	unsigned int data, uiPatRange;
	int i;
	unsigned int uiCompErr = 0, len;
	unsigned int *src_buffp1;
	unsigned int *dst_buffp1;

	if (src_addr)
	{
		src_buffp1 = src_addr;
	}
	else
	{
		src_buffp1 = 0x40000000;
	}

	if (dst_addr)
	{
		dst_buffp1 = dst_addr;
	}
	else
	{
		dst_buffp1 = 0x50000000;
	}

	len = trans_len;

	for (i = 0 ; i < (len/sizeof(unsigned int)) ; i++) {
	    *((unsigned int *)dst_buffp1+i) = 0;
	}

	#if 0
	memcpy((void *)dst_buffp1, src_buffp1, len);
	#else
	 *((unsigned int *)(0x10212c18)) = (DMA_BURST_LEN & 0x07)  << 16; //BURST_LEN:7-8,R/W
	 *((unsigned int *)(0x10212c1c)) = src_buffp1;
	 *((unsigned int *)(0x10212c20)) = dst_buffp1;
	 *((unsigned int *)(0x10212c24)) = len;
	 dsb();
	 *((unsigned int *)(0x10212c08)) = 0x1; //start dma

	 while(*((unsigned int *)(0x10212c08))) ;
        #endif

	for (i = 0 ; i < (len/sizeof(unsigned int)) ; i++)
	{
		if (*((unsigned int *)dst_buffp1+i) != *((unsigned int *)src_buffp1+i))
		{
			err = 1;
	   		uiCompErr |= ((*((unsigned int *)dst_buffp1+i)) ^ (*((unsigned int *)src_buffp1+i)));
			#ifdef DMA_ERROR_DISPLAY
			dbg_print("Source %xh = %xh Destination %xh= %xh diff=%xh\n",
				(unsigned int)((unsigned int *)src_buffp1+i), (*((unsigned int *)src_buffp1+i)),
				(unsigned int)((unsigned int *)dst_buffp1+i), (*((unsigned int *)dst_buffp1+i)),
				(*((unsigned int *)src_buffp1+i)) ^ (*((unsigned int *)dst_buffp1+i)));
			#endif
		}
        }

	#ifdef DMA_ERROR_STOP
	    if (uiCompErr) {
		must_print("Enter to continue...\n");
		while (1) {
			if ( UART_Get_Command() )  {
				 break;
			 }
		}
	    }
	#endif

    return uiCompErr;
}
#endif

#endif

#ifdef MEMPLL_RESET_TEST
void Mempll_Reset(DRAMC_CTX_T *p)
{
	mcSHOW_DBG_MSG(("MEMPLL reset test\n"));

        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 1);
        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 1);

        // Sleep
        *(volatile unsigned int*)(0x10006000) = 0x0b160001;
         *(volatile unsigned int*)(0x10006010) |= (0x01 << 2);	// 0x10006010[2]=1 //memck_off = 1, infra CG off
         *(volatile unsigned int*)(0x10006010) |= (0x01 << 8);	//mempllout_off = 1, DDRPHY CG off
        *(volatile unsigned int*)(0x10006010) |= (0x01 << 27);  	//(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
        mcDELAY_US(5);

        // Wake up
        *(volatile unsigned int*)(0x10006010) &= ~(0x01 << 27);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
        mcDELAY_US(2);      	// PLL settle time 1us->2u
	 *(volatile unsigned int*)(0x10006010) &= ~(0x01 << 8);	// 0x10006010[8]=0? //mempllout_off = 0?, DDRPHY CG on, phase sync
	mcDELAY_US(1);  	// delay 2T @ 26M
	 *(volatile unsigned int*)(0x10006010) &= ~(0x01 << 2);	// 0x10006010[2]=0 ?//memck_off = 0, infra CG on

        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 0);
        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 0);
}
#endif

void REFRATE_Manual(DRAMC_CTX_T *p, U32 ManualEnable)
{
        U8 ucstatus = 0;
        U32 u4value;

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x114), &u4value);
        if (ManualEnable)
        {
        	mcSET_BIT(u4value, 31);
        }
        else
        {
        	mcCLR_BIT(u4value, 31);
        }
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x114), u4value);

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x114), &u4value);
        mcSHOW_ERR_MSG(("Reg.0x114=%x\n", u4value));

        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x03B8), &u4value);
        mcSHOW_ERR_MSG(("Reg.3B8h[10:8]=%x\n", (u4value & 0x700)>>8));
}

#ifdef SUSPEND_TEST

#ifdef SUSPEND_TEST_DRAMC_SWRST_TEST
void Suspend_Resume(DRAMC_CTX_T *p)
{
        U8 ucstatus = 0;
        U32 u4value;

	mcSHOW_DBG_MSG(("DRAMC reset test\n"));

	(*(volatile unsigned int *)(0x10004028)) &= (~(0x01<<30));         // cha DLLFRZ=0
	(*(volatile unsigned int *)(0x10011028)) &= (~(0x01<<30));         // chb DLLFRZ=0
	(*(volatile unsigned int *)(0x10004094)) = (*(volatile unsigned int *)(0x1020e374));         // cha r0
	(*(volatile unsigned int *)(0x10004098)) = (*(volatile unsigned int *)(0x1020e378));         // cha r1
	(*(volatile unsigned int *)(0x10011094)) = (*(volatile unsigned int *)(0x10213374));         // chb r0
	(*(volatile unsigned int *)(0x10011098)) = (*(volatile unsigned int *)(0x10213378));         // chb r1
	(*(volatile unsigned int *)(0x10004028)) |= (0x01<<30);              // cha DLLFRZ=1
	(*(volatile unsigned int *)(0x10011028)) |= (0x01<<30);              // chb DLLFRZ=1

        p->channel = CHANNEL_A;
        DramcEnterSelfRefresh(p, 1);
        DramcPhyReset_DramcRST(p);
        DramcEnterSelfRefresh(p, 0);

        p->channel = CHANNEL_B;
        DramcEnterSelfRefresh(p, 1);
        DramcPhyReset_DramcRST(p);
        DramcEnterSelfRefresh(p, 0);
}

#else

void Suspend_Resume(DRAMC_CTX_T *p)
{
        U8 ucstatus = 0;
        U32 u4value;

#ifdef SUSPEND_RDATARST_IN_MEMPLL_ENABLE
	mcSHOW_DBG_MSG(("Suspend_Resume test with RDATA reset in MEMPLL enable...\n"));
#endif

#ifdef SUSPEND_DRAMCRST_IN_MEMPLL_ENABLE
	mcSHOW_DBG_MSG(("Suspend_Resume test with DRAMC reset in MEMPLL enable...\n"));
#endif

#ifdef SUSPEND_DRAMCRST_IN_MEMPLL_DISABLE
	mcSHOW_DBG_MSG(("Suspend_Resume test with DRAMC reset in MEMPLL disable...\n"));
#endif

#ifdef SUSPEND_DRAMCRST_AFTER_SF
	mcSHOW_DBG_MSG(("Suspend_Resume test with DRAMC reset after self refresh...\n"));
#endif

#ifdef HWGW_SYNC_OUTSIDE_SF
	// DCMEN
	(*(volatile unsigned int *)(0x100041dc)) =  (*(volatile unsigned int *)(0x100041dc)) & (~(0x01<<25));         // cha DCMEN=0
	(*(volatile unsigned int *)(0x100111dc)) =  (*(volatile unsigned int *)(0x100111dc)) & (~(0x01<<25));         // chb DCMEN=0

	// MANUDLLFRZ
	(*(volatile unsigned int *)(0x10004044)) = (*(volatile unsigned int *)(0x10004044)) |(0x01<<12);            // cha MANUDLLFRZ=1
	(*(volatile unsigned int *)(0x10011044)) = (*(volatile unsigned int *)(0x10011044)) | (0x01<<12);            // chb MANUDLLFRZ=1

	// wait for DCMEN
        mcDELAY_US(1);

	(*(volatile unsigned int *)(0x10004094)) = (*(volatile unsigned int *)(0x1020e374));         // cha r0
	(*(volatile unsigned int *)(0x10004098)) = (*(volatile unsigned int *)(0x1020e378));         // cha r1
	(*(volatile unsigned int *)(0x10011094)) = (*(volatile unsigned int *)(0x10213374));         // chb r0
	(*(volatile unsigned int *)(0x10011098)) = (*(volatile unsigned int *)(0x10213378));         // chb r1

	// MANUDLLFRZ
	(*(volatile unsigned int *)(0x10004044)) = (*(volatile unsigned int *)(0x10004044)) & (~(0x01<<12));         // cha MANUDLLFRZ=0
	(*(volatile unsigned int *)(0x10011044)) = (*(volatile unsigned int *)(0x10011044)) & (~(0x01<<12));         // chb MANUDLLFRZ=0

	// DCMEN
	(*(volatile unsigned int *)(0x100041dc)) = (*(volatile unsigned int *)(0x100041dc)) |(0x01<<25);            // cha DCMEN=1
	(*(volatile unsigned int *)(0x100111dc)) = (*(volatile unsigned int *)(0x100111dc)) |(0x01<<25);            // chb DCMEN=1
#endif

        //Switch to SW control, bypass SPM
        TransferToRegControl();

        p->channel = CHANNEL_A;

#ifdef SUSPEND_REFRATE_MANUAL
	REFRATE_Manual(p, 1);
#endif

#ifdef SUSPEND_DISABLE_MR4
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e8), &u4value);
	mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), u4value);
#endif
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x1111ff11);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11510111);	// Reg.511h[8]=1 for regiser control 640h

        // Enter self-refresh (enable SELFREF & polling the SREF_STATE) by SRAM execution
        // DRAMC_WRITE_REG(DRAMC_READ_REG(0x04) | (0x1<<26), 0x04);
        // while ( (DRAMC_READ_REG(0x3b8) & (0x01<<16))==0);
        DramcEnterSelfRefresh(p, 1);

        p->channel = CHANNEL_B;

#ifdef SUSPEND_REFRATE_MANUAL
	REFRATE_Manual(p, 1);
#endif

#ifdef SUSPEND_DISABLE_MR4
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e8), &u4value);
	mcSET_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), u4value);
#endif
        //switch to SW control, bypass SPM
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5c8), 0x1111ff11);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), 0x11510111);	// Reg.511h[8]=1 for regiser control 640h

        // Enter self-refresh (enable SELFREF & polling the SREF_STATE) by SRAM execution
        // DRAMC_WRITE_REG(DRAMC_READ_REG(0x04) | (0x1<<26), 0x04);
        // while ( (DRAMC_READ_REG(0x3b8) & (0x01<<16))==0);
        DramcEnterSelfRefresh(p, 1);

#ifdef HWGW_SYNC_INSIDE_SF
	(*(volatile unsigned int *)(0x10004094)) = (*(volatile unsigned int *)(0x1020e374));         // cha r0
	(*(volatile unsigned int *)(0x10004098)) = (*(volatile unsigned int *)(0x1020e378));         // cha r1
	(*(volatile unsigned int *)(0x10011094)) = (*(volatile unsigned int *)(0x10213374));         // chb r0
	(*(volatile unsigned int *)(0x10011098)) = (*(volatile unsigned int *)(0x10213378));         // chb r1
#endif

        *(volatile unsigned int*)(0x10006000) = 0x0b160001;
        *(volatile unsigned int*)(0x10006010) |= (0x01 << 2);	// 0x10006010[2]=1 //memck_off = 1, infra CG off
        *(volatile unsigned int*)(0x10006010) |= (0x01 << 8);	//mempllout_off = 1, DDRPHY CG off

	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) &= ~(0x01<<0);	// Switch ALLCK_EN from register to SPM (default is register). So disable.
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5c8))) &= ~(0x01<<0);	// Switch ALLCK_EN from register to SPM (default is register)

#if 0
        p->channel = CHANNEL_B;
        // Turn on memory clock cg (RG_DMALL_CK_EN)
        // DRAMC_WRITE_REG((DRAMC_READ_REG(0x640)) & (0xffffffef), 0x640);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcCLR_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        p->channel = CHANNEL_A;
        // Turn on memory clock cg (RG_DMALL_CK_EN)
        // DRAMC_WRITE_REG((DRAMC_READ_REG(0x640)) & (0xffffffef), 0x640);
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcCLR_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

	p->channel = CHANNEL_B;
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
	mcCLR_BIT(u4value, 5);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

	p->channel = CHANNEL_A;
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
	mcSET_BIT(u4value, 1);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
	mcSET_BIT(u4value, 1);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);
        //?? Turn on mempllout cg (RG_DMPLL2_CK_EN)
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x640)) & (0xffffffdf), 0x640);
#endif

        // Turn off mempll_div_en/mempll2_en/3_en/4_en
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x604)) & (0xfeffffff), 0x604);
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x60c)) & (0xfffbffff), 0x60c);
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x614)) & (0xfffbffff), 0x614);
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x61c)) & (0xfffbffff), 0x61c);

        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcCLR_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcCLR_BIT(u4value, 6);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        // MEMPLL_BIAS_LPF_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcCLR_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);

        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcCLR_BIT(u4value, 6);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        // MEMPLL_BIAS_LPF_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcCLR_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);

        // RG_MPLL_EN=0 : 0x10209280[0] (disable MPLL)
        u4value = (*(volatile unsigned int *)(0x10209280));
        mcCLR_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10209280)) = u4value;

	// MEMPLL_SDM_ISO
        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x5cc), &u4value);
        mcSET_BIT(u4value, 3);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), u4value);

        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x5cc), &u4value);
        mcSET_BIT(u4value, 3);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), u4value);

        mcDELAY_US(100);

#ifdef DDRPHY_POWER_DOWN
	//0x10006240[4] PWR_CLK_DIS: Disables subsys clock
	// 0: clock enable
	// 1: clock disable
	//
	//0x10006240[3] PWR_ON_2ND: Subsys power-on request
	// 0: power off
	// 1: power on
	//
	//0x10006240[2]PWR_ON: Subsys power-on request
	// 0: power off
	// 1: power on
	//
	//0x10006240[1] PWR_ISO: Enables Subsys isolation
	// 0: isolation disable
	// 1: isolation enable
	//
	//0x10006240[0] PWR_RST_B: Subsys power-on reset
	// 0: reset asserted
	// 1: reset deasserted
	//
	//DEFINE PDEF_SLEEP_DPY_PWR_CON = 0x10006240
	//<DDRPHY Power Off>
	//r3 = #PDEF_SLEEP_DPY_PWR_CON
	//*r3 = #0xD
	//*r3 = #0xFF0D // SRAM/ROM power down
	//*r3 = #0xFF0F // Set power iso
	//*r3 = #0xFF1E // Set clock disable and assert pwr_rst_b
	//*r3 = #0xFF12 // Set power_on/power_on_2nd to low

        //
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcCLR_BIT(u4value, 4);
        mcSET_BIT(u4value, 3);
        mcSET_BIT(u4value, 2);
        mcCLR_BIT(u4value, 1);
        mcSET_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        //set DDRPHY ISO_EN
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcSET_BIT(u4value, 1);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        //Set clock disable and assert pwr_rst_b
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcSET_BIT(u4value, 4);
        mcCLR_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        //Set power_on/power_on_2nd to low
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcCLR_BIT(u4value, 3);
        mcCLR_BIT(u4value, 2);
        (*(volatile unsigned int *)(0x10006240)) = u4value;

        mcDELAY_US(100);
#endif  //DDRPHY_POWER_DOWN

#ifdef SUSPEND_PWR_RST_B
	// KH test
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcCLR_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        mcDELAY_US(100);
#endif

	pmic_Vcore1_adjust(8);	// Vcore = 0.9V
	pmic_Vcore2_adjust(8);
        mcDELAY_US(100);

#ifdef DUAL_FREQ_TEST
        if (p->frequency == DUAL_FREQ_HIGH)
        {
		pmic_Vcore1_adjust(6);	// 1.125V
		pmic_Vcore2_adjust(6);
        }
        else
        {
		pmic_Vcore1_adjust(7);	// 1.0V
		pmic_Vcore2_adjust(7);
        }
#else
	pmic_Vcore1_adjust(6);	// 1.125V
	pmic_Vcore2_adjust(6);
#endif
        mcDELAY_US(100);

#if 0
        // Infra reset
        *(volatile unsigned *)(0x10001034) |= (1 << 0x2);
        *(volatile unsigned *)(0x10001034) &= ~(1 << 0x2);

        // Turn on mempll_en -> wait 30us -> Seems no such settings in 6595?? remove.
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x600)) |(0x04), 0x600);
        // ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x604), &u4value);
        // mcSET_BIT(u4value, 28);
        // ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x604), u4value);
        // mcDELAY_US(30);
#endif

        //<DDRPHY Power On>
        //r3 = #PDEF_SLEEP_DPY_PWR_CON
        //*r3 = #0x16 // Set power on
        //*r3 = #0x1E // Set power on_s
        //*r3 = #0xE // Release clock disable
        //*r3 = #0xF // Release power reset_b

#ifdef DDRPHY_POWER_DOWN
        //Set power on
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcSET_BIT(u4value, 2);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        //Set power on_s
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcSET_BIT(u4value, 3);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        //Release clock disable
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcCLR_BIT(u4value, 4);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        //Release power reset_b
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcSET_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
	mcDELAY_US(100);
#endif //DDRPHY_POWER_DOWN

#ifdef SUSPEND_PWR_RST_B
	// KH test
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcSET_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
        mcDELAY_US(100);
#endif

        p->channel = CHANNEL_A;

#ifdef SUSPEND_DRAMCRST_IN_MEMPLL_DISABLE
        // Toggle DRAMC SW RESET.
        DramcPhyReset_DramcRST(p);
#endif

        //(*(volatile unsigned int *)(0x10006240)) &= 0xfffffffd; // ISO = 0
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x5cc), &u4value);
        mcCLR_BIT(u4value, 3);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), u4value);
        mcDELAY_US(100);

        p->channel = CHANNEL_B;
#ifdef SUSPEND_DRAMCRST_IN_MEMPLL_DISABLE
        // Toggle DRAMC SW RESET.
        DramcPhyReset_DramcRST(p);
#endif

        //(*(volatile unsigned int *)(0x10006240)) &= 0xfffffffd; // ISO = 0
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x5cc), &u4value);
        mcCLR_BIT(u4value, 3);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x5cc), u4value);
        mcDELAY_US(100);

        // RG_MPLL_EN=0 : 0x10209280[0] (Enable MPLL)
        u4value = (*(volatile unsigned int *)(0x10209280));
        mcSET_BIT(u4value, 0);
        (*(volatile unsigned int *)(0x10209280)) = u4value;

        // Turn on mempll_div_en -> wait 30us
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x604)) | (0x01000000), 0x604);
        // MEMPLL_DIV_EN
        //ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x610), &u4value);
        //mcSET_BIT(u4value, 16);
        //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x610), u4value);
        //mcDELAY_US(30);

        // Turn on mempll2_en/3_en/4_en -> wait 30us
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x60c)) | (0x00040000), 0x60c);
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x614)) | (0x00040000), 0x614);
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x61c)) | (0x00040000), 0x61c);
        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 6);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 6);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        mcDELAY_US(2);
        p->channel = CHANNEL_A;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);
        // MEMPLL05_2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x664), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x664), u4value);
        // MEMPLL05_3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x670), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x670), u4value);
        // MEMPLL05_4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x67c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x67c), u4value);
        p->channel = CHANNEL_B;
        // MEMPLL2_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x614), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x614), u4value);
        // MEMPLL3_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x620), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x620), u4value);
        // MEMPLL4_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x62c), &u4value);
        mcSET_BIT(u4value, 0);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x62c), u4value);

#ifdef MEMPLL_NEW_POWERON
	*(volatile unsigned int*)(0x10006000) = 0x0b160001;
	*(volatile unsigned int*)(0x10006010) |= 0x08000000;  //(4) 0x10006010[27]=1  //Request MEMPLL reset/pdn mode
	mcDELAY_US(2);
	*(volatile unsigned int*)(0x10006010) &= ~(0x08000000);  //(1) 0x10006010[27]=0 //Unrequest MEMPLL reset/pdn mode and wait settle (1us for reset)
	mcDELAY_US(13);
#else
        mcDELAY_US(20);
#endif

        p->channel = CHANNEL_A;
        // MEMPLL_BIAS_LPF_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);
        p->channel = CHANNEL_B;
        // MEMPLL_BIAS_LPF_EN
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x60c), &u4value);
        mcSET_BIT(u4value, 7);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x60c), u4value);

        mcDELAY_US(30);

#if 0
        //?? Turn off mempllout cg (RG_DMPLL2_CK_EN)
        // DRAMC_WRITE_REG((DRAMC_READ_REG(0x640)) | (0x00000020), 0x640);
        // Turn off memory clock cg (RG_DMALL_CK_EN) -> wait 9us
        //DRAMC_WRITE_REG((DRAMC_READ_REG(0x640)) | (0x00000010), 0x640);
        p->channel = CHANNEL_B;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

        p->channel = CHANNEL_A;
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
        mcSET_BIT(u4value, 4);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

	p->channel = CHANNEL_B;
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
	mcSET_BIT(u4value, 5);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

	p->channel = CHANNEL_A;
	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x640), &u4value);
	mcCLR_BIT(u4value, 1);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x640), u4value);

	ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x690), &u4value);
	mcCLR_BIT(u4value, 1);
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x690), u4value);
        mcDELAY_US(9);
#endif

 	//DramcDiv2PhaseSync(p);

	TransferToSPMControl();
	(*(volatile unsigned int *)(CHA_DDRPHY_BASE + (0x5c8))) |= (0x01<<0);	// Switch ALLCK_EN from SPM to register (default is register). So enable.
	(*(volatile unsigned int *)(CHB_DDRPHY_BASE + (0x5c8))) |= (0x01<<0);

	// The following causes DIV2 phase sync.
	 *(volatile unsigned int*)(0x10006010) &= ~(0x01 << 8);	// 0x10006010[8]=0? //mempllout_off = 0?, DDRPHY CG on, phase sync
        mcDELAY_US(1);
	 *(volatile unsigned int*)(0x10006010) &= ~(0x01 << 2);	// 0x10006010[2]=0 ?//memck_off = 0, infra CG on


#ifdef DDRPHY_POWER_DOWN
        mcDELAY_US(10);

        //// power iso should be release after mempll turning on
        ////-- DDR Release ISO
        //r3 = #PDEF_SLEEP_DPY_PWR_CON
        //*r3 = #0xD
        //Release clock disable
        u4value = (*(volatile unsigned int *)(0x10006240));
        mcCLR_BIT(u4value, 1);
        (*(volatile unsigned int *)(0x10006240)) = u4value;
#endif //DDRPHY_POWER_DOWN
      	mcDELAY_US(10);

        p->channel = CHANNEL_A;
#ifdef SUSPEND_DRAMCRST_IN_MEMPLL_ENABLE
	DramcPhyReset_DramcRST(p);
#endif

#ifdef SUSPEND_RDATARST_IN_MEMPLL_ENABLE
        DramcPhyReset(p);
#endif

#ifdef SUSPEND_DISABLE_MR4
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e8), &u4value);
	mcCLR_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), u4value);
#endif

#ifdef SUSPEND_REFRATE_MANUAL
	REFRATE_Manual(p, 0);
#endif

        // Exit self-refresh
        //DRAMC_WRITE_REG(DRAMC_READ_REG(0x04) & ~(0x1<<26), 0x04);
        //while ( (DRAMC_READ_REG(0x3b8) & (0x01<<16))==1);
        DramcEnterSelfRefresh(p, 0);


        p->channel = CHANNEL_B;
#ifdef SUSPEND_DRAMCRST_IN_MEMPLL_ENABLE
	DramcPhyReset_DramcRST(p);
#endif

#ifdef SUSPEND_RDATARST_IN_MEMPLL_ENABLE
        DramcPhyReset(p);
#endif

#ifdef SUSPEND_DISABLE_MR4
        ucstatus |= ucDram_Register_Read(mcSET_DRAMC_REG_ADDR(0x1e8), &u4value);
	mcCLR_BIT(u4value, 26);
        ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), u4value);
#endif

#ifdef SUSPEND_REFRATE_MANUAL
	REFRATE_Manual(p, 0);
#endif

        // Exit self-refresh
        //DRAMC_WRITE_REG(DRAMC_READ_REG(0x04) & ~(0x1<<26), 0x04);
        //while ( (DRAMC_READ_REG(0x3b8) & (0x01<<16))==1);
        DramcEnterSelfRefresh(p, 0);

	#ifdef DUMMY_READ_AFTER_RESUME
	{
		unsigned int DummyRead;
		DummyRead = *((volatile unsigned int *)0x40000000);
		//DummyRead = *((volatile unsigned int *)0x40000000);
		DummyRead = *((volatile unsigned int *)0x40000100);
		//DummyRead = *((volatile unsigned int *)0x40000100);
		DummyRead = *((volatile unsigned int *)0xa0000000);
		//DummyRead = *((volatile unsigned int *)0xa0000000);
		DummyRead = *((volatile unsigned int *)0xa0000100);
		//DummyRead = *((volatile unsigned int *)0xa0000100);
	}
	#endif

#ifdef SUSPEND_DRAMCRST_AFTER_SF
        p->channel = CHANNEL_A;
	DramcPhyReset_DramcRST(p);
        p->channel = CHANNEL_B;
	DramcPhyReset_DramcRST(p);
#endif


        //dbg_print("Exit self-refresh.\n");
        //gpt_busy_wait_ms(100);
}
#endif

#endif

#ifdef DDR_FT_LOAD_BOARD
void LoadBoardGpioInit(void)
{
	U32 u4temp;

	//Set GPIO mode registers (3bits each)
	u4temp = (*(volatile unsigned int *)(0x10005000 + (0x600)));
	// GPIO0
	mcSET_FIELD(u4temp, 0x0, 0x00000007, 0);
	// GPIO2
	mcSET_FIELD(u4temp, 0x0, 0x000001c0, 6);
	// GPIO3
	mcSET_FIELD(u4temp, 0x0, 0x00000e00, 9);
	// GPIO4
	mcSET_FIELD(u4temp, 0x0, 0x00007000, 12);
	(*(volatile unsigned int *)(0x10005000 + (0x600))) = u4temp;

	u4temp = (*(volatile unsigned int *)(0x10005000 + (0x610)));
	// GPIO5
	//mcSET_FIELD(u4temp, 0x0, 0x00000007, 0);
	// GPIO6
	mcSET_FIELD(u4temp, 0x0, 0x00000038, 3);
	// GPIO7
	mcSET_FIELD(u4temp, 0x0, 0x000001c0, 6);
	// GPIO8
	mcSET_FIELD(u4temp, 0x0, 0x00000e00, 9);
	// GPIO9
	mcSET_FIELD(u4temp, 0x0, 0x00007000, 12);
	(*(volatile unsigned int *)(0x10005000 + (0x610))) = u4temp;

	// Set GPIO direction
	u4temp = (*(volatile unsigned int *)(0x10005000 + (0x000)));
	mcSET_BIT(u4temp, 0);
	mcSET_BIT(u4temp, 2);
	mcSET_BIT(u4temp, 3);
	mcSET_BIT(u4temp, 4);
	//mcSET_BIT(u4temp, 5);
	mcSET_BIT(u4temp, 6);
	mcSET_BIT(u4temp, 7);
	mcSET_BIT(u4temp, 8);
	mcSET_BIT(u4temp, 9);
	(*(volatile unsigned int *)(0x10005000 + (0x000))) = u4temp;
}
void GpioOutput(U8 gpio_no, U8 low_high)
{
	U32 u4temp;

	if(low_high == 0)
	{
		u4temp = (*(volatile unsigned int *)(0x10005000 + (0x400)));
		mcCLR_BIT(u4temp, gpio_no); // GPIO0~15
		(*(volatile unsigned int *)(0x10005000 + (0x400))) = u4temp;
	}
	else
	{
		u4temp = (*(volatile unsigned int *)(0x10005000 + (0x400)));
		mcSET_BIT(u4temp, gpio_no); // GPIO0~15
		(*(volatile unsigned int *)(0x10005000 + (0x400))) = u4temp;
	}
}
void LoadBoardShowResult(U8 step, U8 error_type, U8 channel, U8 complete)
{
	mcSHOW_DBG_MSG(("result: "));
	switch(complete)
	{
		case FLAG_NOT_COMPLETE_OR_FAIL:
			//GpioOutput(7, 0);
			GpioOutput(9, 0);
			mcSHOW_DBG_MSG(("0"));
			break;
		case FLAG_COMPLETE_AND_PASS:
			//GpioOutput(7, 1);
			GpioOutput(9, 1);
			mcSHOW_DBG_MSG(("1"));
			break;
		default:
			break;
	}

	switch(channel)
	{
		case CHANNEL_A:
			GpioOutput(8, 0);
			mcSHOW_DBG_MSG(("0"));
			break;
		case CHANNEL_B:
			GpioOutput(8, 1);
			mcSHOW_DBG_MSG(("1"));
			break;
		default:
			break;
	}

	switch(error_type)
	{
		case FLAG_CALIBRATION_PASS:
			GpioOutput(7, 0);
			GpioOutput(6, 0);
			mcSHOW_DBG_MSG(("00"));
			break;
		case FLAG_WINDOW_TOO_SMALL:
			GpioOutput(7, 0);
			GpioOutput(6, 1);
			mcSHOW_DBG_MSG(("01"));
			break;
		case FLAG_WINDOW_TOO_BIG:
			GpioOutput(7, 1);
			GpioOutput(6, 0);
			mcSHOW_DBG_MSG(("10"));
			break;
		case FLAG_CALIBRATION_FAIL:
			GpioOutput(7, 1);
			GpioOutput(6, 1);
			mcSHOW_DBG_MSG(("11"));
			break;
		default:
			break;
	}

	switch(step)
	{
		case FLAG_PLLPHASE_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 0);
			GpioOutput(2, 0);
			GpioOutput(0, 0);
			mcSHOW_DBG_MSG(("0000"));
			break;
		case FLAG_PLLGPPHASE_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 0);
			GpioOutput(2, 0);
			GpioOutput(0, 1);
			mcSHOW_DBG_MSG(("0001"));
			break;
		case FLAG_IMPEDANCE_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 0);
			GpioOutput(2, 1);
			GpioOutput(0, 0);
			mcSHOW_DBG_MSG(("0010"));
			break;
		case FLAG_CA_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 0);
			GpioOutput(2, 1);
			GpioOutput(0, 1);
			mcSHOW_DBG_MSG(("0011"));
			break;
		case FLAG_WL_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 1);
			GpioOutput(2, 0);
			GpioOutput(0, 0);
			mcSHOW_DBG_MSG(("0100"));
			break;
		case FLAG_GATING_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 1);
			GpioOutput(2, 0);
			GpioOutput(0, 1);
			mcSHOW_DBG_MSG(("0101"));
			break;
		case FLAG_RX_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 1);
			GpioOutput(2, 1);
			GpioOutput(0, 0);
			mcSHOW_DBG_MSG(("0110"));
			break;
		case FLAG_DATLAT_CALIBRATION:
			GpioOutput(4, 0);
			GpioOutput(3, 1);
			GpioOutput(2, 1);
			GpioOutput(0, 1);
			mcSHOW_DBG_MSG(("0111"));
			break;
		case FLAG_TX_CALIBRATION:
			GpioOutput(4, 1);
			GpioOutput(3, 0);
			GpioOutput(2, 0);
			GpioOutput(0, 0);
			mcSHOW_DBG_MSG(("1000"));
			break;
		default:
			break;
	}

	mcSHOW_DBG_MSG(("\n"));
}
#endif


