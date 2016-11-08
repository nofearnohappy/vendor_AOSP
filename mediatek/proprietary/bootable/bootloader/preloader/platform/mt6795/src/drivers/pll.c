/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include "typedefs.h"
#include "platform.h"


#include "pll.h"
#include "timer.h"
#include "spm.h"
#include "wdt.h"
#include "emi.h"

#include "dramc_common.h"

#if(6795 == MACH_TYPE)
#include "dramc_pi_api_64.h"
#include "dramc_register_64.h"
#else
#include "dramc_pi_api.h"
#include "dramc_register.h"
#endif


int A_Reg3e0=0, A_Reg3e4=0;
int B_Reg3e0=0, B_Reg3e4=0;

extern DRAMC_CTX_T DramCtx_LPDDR3;
extern DRAMC_CTX_T DramCtx_PCDDR3;

void mt_mempll_init(DRAMC_CTX_T *p)
{
    /*p->channel = CHANNEL_A;
    MemPllInit((DRAMC_CTX_T *) p);

    p->channel = CHANNEL_B;*/
    MemPllPreInit((DRAMC_CTX_T *) p);
    MemPllInit((DRAMC_CTX_T *) p);	
    return;
}

void mt_mempll_cali(DRAMC_CTX_T *p)
{
    // called after chA and chB init done
    // MEMPLL05 registers, some are located @ chA and others are @ chB
    p->channel = CHANNEL_A;
    DramcPllPhaseCal(p);
    p->channel = CHANNEL_B;
    DramcPllPhaseCal(p);
	
    //Should only be called after channel A/B MEMPLL phase calibration had been done.
    DramCPllGroupsCal(p);
    return;
}

void mt_mempll_pre(void)
{
#ifdef DDR_RESERVE_MODE  
    unsigned int wdt_mode;
    unsigned int wdt_dbg_ctrl;
#endif
    DRAMC_CTX_T *psDramCtx;

#ifdef DDRTYPE_LPDDR3
    psDramCtx = &DramCtx_LPDDR3;
#endif

#ifdef DDRTYPE_DDR3
    psDramCtx = &DramCtx_PCDDR3;
#endif

#ifdef DDR_RESERVE_MODE  
    wdt_mode = READ_REG(MTK_WDT_MODE);
    wdt_dbg_ctrl = READ_REG(MTK_WDT_DEBUG_CTL);

    print("before mt_mempll_init, wdt_mode = 0x%x, wdt_dbg_ctrl = 0x%x\n", wdt_mode, wdt_dbg_ctrl);     
    if(((wdt_mode & MTK_WDT_MODE_DDR_RESERVE) !=0) && ((wdt_dbg_ctrl & MTK_DDR_RESERVE_RTA) != 0) ) {
        print("[PLL] skip mt_mempll_init!!!\n");
        return;
    }
#endif
    
    print("[PLL] mempll_init\n");
    mt_mempll_init(psDramCtx);
    return;
}

void mt_mempll_post(void)
{
#ifdef DDR_RESERVE_MODE  
    unsigned int wdt_mode;
    unsigned int wdt_dbg_ctrl;
#endif
    DRAMC_CTX_T *psDramCtx;

#ifdef DDRTYPE_LPDDR3
    psDramCtx = &DramCtx_LPDDR3;
#endif

#ifdef DDRTYPE_DDR3
    psDramCtx = &DramCtx_PCDDR3;
#endif

#ifdef DDR_RESERVE_MODE  
    wdt_mode = READ_REG(MTK_WDT_MODE);
    wdt_dbg_ctrl = READ_REG(MTK_WDT_DEBUG_CTL);

    print("before mt_mempll_cali, wdt_mode = 0x%x, wdt_dbg_ctrl = 0x%x\n", wdt_mode, wdt_dbg_ctrl);     
    if(((wdt_mode & MTK_WDT_MODE_DDR_RESERVE) !=0) && ((wdt_dbg_ctrl & MTK_DDR_RESERVE_RTA) != 0) ) {
        print("[PLL] skip mt_mempll_cali!!!\n");
        return;
    }
#endif

    print("[PLL] mempll_cali\n");
    mt_mempll_cali(psDramCtx);
    return;
}



unsigned int mt_get_smallcpu_freq(void)
{
    int output = 0;
    unsigned int temp, clk26cali_0, clk_cfg_8, clk_misc_cfg_1, clk26cali_1;
    unsigned int top_ckmuxsel, top_ckdiv1, ir_rosc_ctl;

    clk26cali_0 = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0 | 0x80); // enable fmeter_en

    clk_misc_cfg_1 = DRV_Reg32(CLK_MISC_CFG_1);
    DRV_WriteReg32(CLK_MISC_CFG_1, 0xFFFFFF00); // select divider

    clk_cfg_8 = DRV_Reg32(CLK_CFG_8);
    DRV_WriteReg32(CLK_CFG_8, (46 << 8)); // select armpll_occ_mon

    top_ckmuxsel = DRV_Reg32(TOP_CKMUXSEL);
    DRV_WriteReg32(TOP_CKMUXSEL, (top_ckmuxsel & 0xFFFFFFFC) | 0x1);

    top_ckdiv1 = DRV_Reg32(TOP_CKDIV1);
    DRV_WriteReg32(TOP_CKDIV1, (top_ckdiv1 & 0xFFFFFFE0) | 0xb);
    
    ir_rosc_ctl = DRV_Reg32(IR_ROSC_CTL);
    DRV_WriteReg32(IR_ROSC_CTL, ir_rosc_ctl | 0x08100000);

    temp = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, temp | 0x1); // start fmeter

    /* wait frequency meter finish */
    while (DRV_Reg32(CLK26CALI_0) & 0x1)
    {
        print("wait for frequency meter finish, CLK26CALI = 0x%x\n", DRV_Reg32(CLK26CALI_0));
        //mdelay(10);
    }

    temp = DRV_Reg32(CLK26CALI_1) & 0xFFFF;

    output = ((temp * 26000) / 1024) * 4; // Khz

    DRV_WriteReg32(CLK_CFG_8, clk_cfg_8);
    DRV_WriteReg32(CLK_MISC_CFG_1, clk_misc_cfg_1);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0);
    DRV_WriteReg32(TOP_CKMUXSEL, top_ckmuxsel);
    DRV_WriteReg32(TOP_CKDIV1, top_ckdiv1);
    DRV_WriteReg32(IR_ROSC_CTL, ir_rosc_ctl);

    //print("CLK26CALI = 0x%x, cpu frequency = %d Khz\n", temp, output);

    return output;
}
unsigned int mt_get_bigcpu_freq(void)
{
    int output = 0;
    unsigned int temp, clk26cali_0, clk_cfg_8, clk_misc_cfg_1, clk26cali_1;
    unsigned int top_ckmuxsel, top_ckdiv1, ir_rosc_ctl, ca15l_mon_sel;

    clk26cali_0 = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0 | 0x80); // enable fmeter_en

    clk_misc_cfg_1 = DRV_Reg32(CLK_MISC_CFG_1);
    DRV_WriteReg32(CLK_MISC_CFG_1, 0xFFFFFF00); // select divider

    clk_cfg_8 = DRV_Reg32(CLK_CFG_8);
    DRV_WriteReg32(CLK_CFG_8, (46 << 8)); // select abist_cksw

    top_ckmuxsel = DRV_Reg32(TOP_CKMUXSEL);
    DRV_WriteReg32(TOP_CKMUXSEL, (top_ckmuxsel & 0xFFFFFFF3) | (0x1<<2));

    top_ckdiv1 = DRV_Reg32(TOP_CKDIV1);
    DRV_WriteReg32(TOP_CKDIV1, (top_ckdiv1 & 0xFFFFFC1F) | (0xb<<5));
    
    ca15l_mon_sel = DRV_Reg32(CA15L_MON_SEL);
    DRV_WriteReg32(CA15L_MON_SEL, ca15l_mon_sel | 0x00000500);
    
    ir_rosc_ctl = DRV_Reg32(IR_ROSC_CTL);
    DRV_WriteReg32(IR_ROSC_CTL, ir_rosc_ctl | 0x10000000);

    temp = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, temp | 0x1); // start fmeter

    /* wait frequency meter finish */
    while (DRV_Reg32(CLK26CALI_0) & 0x1)
    {
        print("wait for frequency meter finish, CLK26CALI = 0x%x\n", DRV_Reg32(CLK26CALI_0));
        //mdelay(10);
    }

    temp = DRV_Reg32(CLK26CALI_1) & 0xFFFF;

    output = ((temp * 26000) / 1024) * 4; // Khz

    DRV_WriteReg32(CLK_CFG_8, clk_cfg_8);
    DRV_WriteReg32(CLK_MISC_CFG_1, clk_misc_cfg_1);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0);
    DRV_WriteReg32(TOP_CKMUXSEL, top_ckmuxsel);
    DRV_WriteReg32(TOP_CKDIV1, top_ckdiv1);
    DRV_WriteReg32(CA15L_MON_SEL, ca15l_mon_sel);
    DRV_WriteReg32(IR_ROSC_CTL, ir_rosc_ctl);

    //print("CLK26CALI = 0x%x, cpu frequency = %d Khz\n", temp, output);

    return output;
}


unsigned int mt_get_mem_freq(void)
{
    int output = 0;
    unsigned int temp, clk26cali_0, clk_cfg_8, clk_misc_cfg_1, clk26cali_1;

    clk26cali_0 = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0 | 0x80); // enable fmeter_en

    clk_misc_cfg_1 = DRV_Reg32(CLK_MISC_CFG_1);
    DRV_WriteReg32(CLK_MISC_CFG_1, 0xFFFFFF00); // select divider

    clk_cfg_8 = DRV_Reg32(CLK_CFG_8);
    DRV_WriteReg32(CLK_CFG_8, (24 << 8)); // select abist_cksw

    temp = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, temp | 0x1); // start fmeter

    /* wait frequency meter finish */
    while (DRV_Reg32(CLK26CALI_0) & 0x1)
    {
        print("wait for frequency meter finish, CLK26CALI = 0x%x\n", DRV_Reg32(CLK26CALI_0));
        //mdelay(10);
    }

    temp = DRV_Reg32(CLK26CALI_1) & 0xFFFF;

    output = (temp * 26000) / 1024; // Khz

    DRV_WriteReg32(CLK_CFG_8, clk_cfg_8);
    DRV_WriteReg32(CLK_MISC_CFG_1, clk_misc_cfg_1);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0);

    //print("CLK26CALI = 0x%x, mem frequency = %d Khz\n", temp, output);

    return output;
}

unsigned int mt_get_bus_freq(void)
{
    int output = 0;
    unsigned int temp, clk26cali_0, clk_cfg_9, clk_misc_cfg_1, clk26cali_2;

    clk26cali_0 = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0 | 0x80); // enable fmeter_en

    clk_misc_cfg_1 = DRV_Reg32(CLK_MISC_CFG_1);
    DRV_WriteReg32(CLK_MISC_CFG_1, 0x00FFFFFF); // select divider

    clk_cfg_9 = DRV_Reg32(CLK_CFG_9);
    DRV_WriteReg32(CLK_CFG_9, (1 << 16)); // select ckgen_cksw

    temp = DRV_Reg32(CLK26CALI_0);
    DRV_WriteReg32(CLK26CALI_0, temp | 0x10); // start fmeter

    /* wait frequency meter finish */
    while (DRV_Reg32(CLK26CALI_0) & 0x10)
    {
        print("wait for frequency meter finish, CLK26CALI = 0x%x\n", DRV_Reg32(CLK26CALI_0));
        //mdelay(10);
    }

    temp = DRV_Reg32(CLK26CALI_2) & 0xFFFF;

    output = (temp * 26000) / 1024; // Khz

    DRV_WriteReg32(CLK_CFG_9, clk_cfg_9);
    DRV_WriteReg32(CLK_MISC_CFG_1, clk_misc_cfg_1);
    DRV_WriteReg32(CLK26CALI_0, clk26cali_0);

    //print("CLK26CALI = 0x%x, bus frequency = %d Khz\n", temp, output);

    return output;
}

//after pmic_init
void mt_pll_post_init(void)
{
    unsigned int temp;
    mt_mempll_pre();
	mt_mempll_post();

    //DRV_WriteReg32(CLK_CFG_0, 0x01000101); //ddrphycfg_ck = 26MHz
    DRV_WriteReg32(CLK_CFG_0, 0x01000101); //mm_ck=vencpll_d2; ddrphycfg_ck=26MHz; mem_clk=mempll; axi_ck=syspll1_d2

    DRV_WriteReg32(CLK_CFG_1, 0x01010100); //mfg_ck=mmpll; venc_ck=vcodecpll; vdec_ck=vcodecpll; pwm=26Mhz

    DRV_WriteReg32(CLK_CFG_2, 0x01010000); //usb20_ck=univpll1_d8; spi_ck=syspll3_d2; uart=26M; camtg_ck=26Mhz

    DRV_WriteReg32(CLK_CFG_3, 0x02060201); //msdc30_1=MSDCPLL_D4; msdc50_0=msdcpll_d4; msdc50_0_hclk=syspll2_d2; usb30=univpll3_d2

    DRV_WriteReg32(CLK_CFG_4, 0x01000101); //aud_intbus=syspll1_d4; audio=26M; msdc30_3=univpll2_d2; msdc30_2=univpll2_d2

    DRV_WriteReg32(CLK_CFG_5, 0x01000100); //mjc=univpll_d3; NULL; scp= syspll1_d2; pmicspi=26MHz

/* CC: modify for testing */
#if 0
    DRV_WriteReg32(CLK_CFG_6, 0x01050101);//cci400 use mainpll
#else
    /* CCI400 use universal PLL */
    //DRV_WriteReg32(CLK_CFG_6, 0x01040101);

    DRV_WriteReg32(CLK_CFG_6, 0x01010101);//aud1=apll2; cci400=vencpll; irda=univpll2_d4; dpi0=tvdpll_d2
#endif

    DRV_WriteReg32(CLK_CFG_7, 0x01010101);//scam=syspll3_d2; axi_mfg=axi; mem_mfg=mmpll; aud2=apll2

    DRV_WriteReg32(CLK_SCP_CFG_0, 0x7FF); // enable scpsys clock off control
    DRV_WriteReg32(CLK_SCP_CFG_1, 0x15); // enable scpsys clock off control

    /*for MTCMOS*/
    spm_mtcmos_ctrl_disp(STA_POWER_ON);


    //step 48
    temp = DRV_Reg32(AP_PLL_CON3);
#if(6595 == MACH_TYPE)
    DRV_WriteReg32(AP_PLL_CON3, temp & 0xFFF44440); // UNIVPLL SW Control
#else
    DRV_WriteReg32(AP_PLL_CON3, temp & 0xFFF55550); // UNIVPLL/CA7PLL SW Control
#endif
    //step 49
    temp = DRV_Reg32(AP_PLL_CON4);
#if(6595 == MACH_TYPE)
    DRV_WriteReg32(AP_PLL_CON4, temp & 0xFFFFFFF4); // UNIVPLL SW Control
#else
    DRV_WriteReg32(AP_PLL_CON4, temp & 0xFFFFFFF5); // UNIVPLL/CA7PLL SW Control
#endif
//    print("mt_pll_post_init: mt_get_smallcpu_freq = %dKhz\n", mt_get_smallcpu_freq());
//    print("mt_pll_post_init: mt_get_bigcpu_freq = %dKhz\n", mt_get_bigcpu_freq());
//    print("mt_pll_post_init: mt_get_bus_freq = %dKhz\n", mt_get_bus_freq());
//    print("mt_pll_post_init: mt_get_mem_freq = %dKhz\n", mt_get_mem_freq());

    #if 0
    print("mt_pll_post_init: AP_PLL_CON3        = 0x%x, GS = 0x00000000\n", DRV_Reg32(AP_PLL_CON3));
    print("mt_pll_post_init: AP_PLL_CON4        = 0x%x, GS = 0x00000000\n", DRV_Reg32(AP_PLL_CON4));
    print("mt_pll_post_init: AP_PLL_CON6        = 0x%x, GS = 0x00000000\n", DRV_Reg32(AP_PLL_CON6));
    print("mt_pll_post_init: CLKSQ_STB_CON0     = 0x%x, GS = 0x05010501\n", DRV_Reg32(CLKSQ_STB_CON0));
    print("mt_pll_post_init: PLL_ISO_CON0       = 0x%x, GS = 0x00080008\n", DRV_Reg32(PLL_ISO_CON0));
    print("mt_pll_post_init: ARMCA15PLL_CON0    = 0x%x, GS = 0x00000101\n", DRV_Reg32(ARMCA15PLL_CON0));
    print("mt_pll_post_init: ARMCA15PLL_CON1    = 0x%x, GS = 0x80108000\n", DRV_Reg32(ARMCA15PLL_CON1));
    print("mt_pll_post_init: ARMCA15PLL_PWR_CON0= 0x%x, GS = 0x00000001\n", DRV_Reg32(ARMCA15PLL_PWR_CON0));
    print("mt_pll_post_init: ARMCA7PLL_CON0     = 0x%x, GS = 0xF1000101\n", DRV_Reg32(ARMCA7PLL_CON0));
    print("mt_pll_post_init: ARMCA7PLL_CON1     = 0x%x, GS = 0x800E8000\n", DRV_Reg32(ARMCA7PLL_CON1));
    print("mt_pll_post_init: ARMCA7PLL_PWR_CON0 = 0x%x, GS = 0x00000001\n", DRV_Reg32(ARMCA7PLL_PWR_CON0));
    print("mt_pll_post_init: MAINPLL_CON0       = 0x%x, GS = 0xF1000101\n", DRV_Reg32(MAINPLL_CON0));
    print("mt_pll_post_init: MAINPLL_CON1       = 0x%x, GS = 0x800A8000\n", DRV_Reg32(MAINPLL_CON1));
    print("mt_pll_post_init: MAINPLL_PWR_CON0   = 0x%x, GS = 0x00000001\n", DRV_Reg32(MAINPLL_PWR_CON0));
    print("mt_pll_post_init: UNIVPLL_CON0       = 0x%x, GS = 0xFF000011\n", DRV_Reg32(UNIVPLL_CON0));
    print("mt_pll_post_init: UNIVPLL_CON1       = 0x%x, GS = 0x80180000\n", DRV_Reg32(UNIVPLL_CON1));
    print("mt_pll_post_init: UNIVPLL_PWR_CON0   = 0x%x, GS = 0x00000001\n", DRV_Reg32(UNIVPLL_PWR_CON0));
    print("mt_pll_post_init: MMPLL_CON0         = 0x%x, GS = 0x00000101\n", DRV_Reg32(MMPLL_CON0));
    print("mt_pll_post_init: MMPLL_CON1         = 0x%x, GS = 0x820D8000\n", DRV_Reg32(MMPLL_CON1));
    print("mt_pll_post_init: MMPLL_PWR_CON0     = 0x%x, GS = 0x00000001\n", DRV_Reg32(MMPLL_PWR_CON0));
    print("mt_pll_post_init: MSDCPLL_CON0       = 0x%x, GS = 0x00000111\n", DRV_Reg32(MSDCPLL_CON0));
    print("mt_pll_post_init: MSDCPLL_CON1       = 0x%x, GS = 0x800F6276\n", DRV_Reg32(MSDCPLL_CON1));
    print("mt_pll_post_init: MSDCPLL_PWR_CON0   = 0x%x, GS = 0x00000001\n", DRV_Reg32(MSDCPLL_PWR_CON0));
    print("mt_pll_post_init: TVDPLL_CON0        = 0x%x, GS = 0x00000101\n", DRV_Reg32(TVDPLL_CON0));
    print("mt_pll_post_init: TVDPLL_CON1        = 0x%x, GS = 0x80112276\n", DRV_Reg32(TVDPLL_CON1));
    print("mt_pll_post_init: TVDPLL_PWR_CON0    = 0x%x, GS = 0x00000001\n", DRV_Reg32(TVDPLL_PWR_CON0));
    print("mt_pll_post_init: VENCPLL_CON0       = 0x%x, GS = 0x00000111\n", DRV_Reg32(VENCPLL_CON0));
    print("mt_pll_post_init: VENCPLL_CON1       = 0x%x, GS = 0x800E989E\n", DRV_Reg32(VENCPLL_CON1));
    print("mt_pll_post_init: VENCPLL_PWR_CON0   = 0x%x, GS = 0x00000001\n", DRV_Reg32(VENCPLL_PWR_CON0));
    print("mt_pll_post_init: MPLL_CON0          = 0x%x, GS = 0x00010111\n", DRV_Reg32(MPLL_CON0));
    print("mt_pll_post_init: MPLL_CON1          = 0x%x, GS = 0x801C0000\n", DRV_Reg32(MPLL_CON1));
    print("mt_pll_post_init: MPLL_PWR_CON0      = 0x%x, GS = 0x00000001\n", DRV_Reg32(MPLL_PWR_CON0));
    print("mt_pll_post_init: VCODECPLL_CON0     = 0x%x, GS = 0x00000121\n", DRV_Reg32(VCODECPLL_CON0));
    print("mt_pll_post_init: VCODECPLL_CON1     = 0x%x, GS = 0x80130000\n", DRV_Reg32(VCODECPLL_CON1));
    print("mt_pll_post_init: VCODECPLL_PWR_CON0 = 0x%x, GS = 0x00000001\n", DRV_Reg32(VCODECPLL_PWR_CON0));
    print("mt_pll_post_init: APLL1_CON0         = 0x%x, GS = 0xF0000131\n", DRV_Reg32(APLL1_CON0));
    print("mt_pll_post_init: APLL1_CON1         = 0x%x, GS = 0xB7945EA6\n", DRV_Reg32(APLL1_CON1));
    print("mt_pll_post_init: APLL1_PWR_CON0     = 0x%x, GS = 0x00000001\n", DRV_Reg32(APLL1_PWR_CON0));
    print("mt_pll_post_init: APLL2_CON0         = 0x%x, GS = 0x00000131\n", DRV_Reg32(APLL2_CON0));
    print("mt_pll_post_init: APLL2_CON1         = 0x%x, GS = 0xBC7EA932\n", DRV_Reg32(APLL2_CON1));
    print("mt_pll_post_init: APLL2_PWR_CON0     = 0x%x, GS = 0x00000001\n", DRV_Reg32(APLL2_PWR_CON0));
    #endif

}

//after pmic_init
void mt_arm_pll_sel(void)
{
    unsigned int temp;
    
    //CA7: INFRA_TOPCKGEN_CKMUXSEL[1:0] (0x10001000) =1
    //CA15: INFRA_TOPCKGEN_CKMUXSEL[3:2] (0x10001000)= 1
    temp = DRV_Reg32(TOP_CKMUXSEL);
#if(6595 == MACH_TYPE)
    DRV_WriteReg32(TOP_CKMUXSEL, temp | 0x5); // switch CA7_ck to ARMCA7PLL, and CA15_ck to ARMCA15PLL
#else
    DRV_WriteReg32(TOP_CKMUXSEL, temp | 0x5); // switch CA15_ck to ARMCA15PLL	
#endif
    print("[PLL] mt_arm_pll_sel done\n");
}

void mt_pll_init(void)
{
    int ret = 0;
    unsigned int temp;

#if(6795 == MACH_TYPE)
    DRV_WriteReg32(ACLKEN_DIV, 0x12); // MCU Bus DIV2
#endif
    //step 1
    DRV_WriteReg32(CLKSQ_STB_CON0, 0x05010501); // reduce CLKSQ disable time
    
    //step 2
    DRV_WriteReg32(PLL_ISO_CON0, 0x00080008); // extend PWR/ISO control timing to 1us
    
    //step 3
    DRV_WriteReg32(AP_PLL_CON6, 0x00000000); //

    /*************
    * xPLL PWR ON 
    **************/
    //step 4
    temp = DRV_Reg32(ARMCA15PLL_PWR_CON0);
    DRV_WriteReg32(ARMCA15PLL_PWR_CON0, temp | 0x1);

#if(6595 == MACH_TYPE)
    temp = DRV_Reg32(ARMCA7PLL_PWR_CON0);
    DRV_WriteReg32(ARMCA7PLL_PWR_CON0, temp | 0x1);
#endif
    //step 5
    temp = DRV_Reg32(MAINPLL_PWR_CON0);
    DRV_WriteReg32(MAINPLL_PWR_CON0, temp | 0x1);
    
    //step 6
    temp = DRV_Reg32(UNIVPLL_PWR_CON0);
    DRV_WriteReg32(UNIVPLL_PWR_CON0, temp | 0x1);
    
    //step 7
    temp = DRV_Reg32(MMPLL_PWR_CON0);
    DRV_WriteReg32(MMPLL_PWR_CON0, temp | 0x1);
    
    //step 8
    temp = DRV_Reg32(MSDCPLL_PWR_CON0);
    DRV_WriteReg32(MSDCPLL_PWR_CON0, temp | 0x1);
    
    //step 9
    temp = DRV_Reg32(VENCPLL_PWR_CON0);
    DRV_WriteReg32(VENCPLL_PWR_CON0, temp | 0x1);
    
    //step 10
    temp = DRV_Reg32(TVDPLL_PWR_CON0);
    DRV_WriteReg32(TVDPLL_PWR_CON0, temp | 0x1);

    //step 11
    temp = DRV_Reg32(MPLL_PWR_CON0);
    DRV_WriteReg32(MPLL_PWR_CON0, temp | 0x1);
    
    //step 12
    temp = DRV_Reg32(VCODECPLL_PWR_CON0);
    DRV_WriteReg32(VCODECPLL_PWR_CON0, temp | 0x1);

    //step 13
    temp = DRV_Reg32(APLL1_PWR_CON0);
    DRV_WriteReg32(APLL1_PWR_CON0, temp | 0x1);
    
    //step 14
    temp = DRV_Reg32(APLL2_PWR_CON0);
    DRV_WriteReg32(APLL2_PWR_CON0, temp | 0x1);

    gpt_busy_wait_us(5); // wait for xPLL_PWR_ON ready (min delay is 1us)

    /******************
    * xPLL ISO Disable
    *******************/
    //step 15
    temp = DRV_Reg32(ARMCA15PLL_PWR_CON0);
    DRV_WriteReg32(ARMCA15PLL_PWR_CON0, temp & 0xFFFFFFFD);
#if(6595 == MACH_TYPE)
    temp = DRV_Reg32(ARMCA7PLL_PWR_CON0);
    DRV_WriteReg32(ARMCA7PLL_PWR_CON0, temp & 0xFFFFFFFD);
#endif
    //step 16
    temp = DRV_Reg32(MAINPLL_PWR_CON0);
    DRV_WriteReg32(MAINPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 17
    temp = DRV_Reg32(UNIVPLL_PWR_CON0);
    DRV_WriteReg32(UNIVPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 18
    temp = DRV_Reg32(MMPLL_PWR_CON0);
    DRV_WriteReg32(MMPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 19
    temp = DRV_Reg32(MSDCPLL_PWR_CON0);
    DRV_WriteReg32(MSDCPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 20
    temp = DRV_Reg32(VENCPLL_PWR_CON0);
    DRV_WriteReg32(VENCPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 21
    temp = DRV_Reg32(TVDPLL_PWR_CON0);
    DRV_WriteReg32(TVDPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 22
    temp = DRV_Reg32(MPLL_PWR_CON0);
    DRV_WriteReg32(MPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 23
    temp = DRV_Reg32(VCODECPLL_PWR_CON0);
    DRV_WriteReg32(VCODECPLL_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 24
    temp = DRV_Reg32(APLL1_PWR_CON0);
    DRV_WriteReg32(APLL1_PWR_CON0, temp & 0xFFFFFFFD);
    
    //step 25
    temp = DRV_Reg32(APLL2_PWR_CON0);
    DRV_WriteReg32(APLL2_PWR_CON0, temp & 0xFFFFFFFD);
    
    /********************
    * xPLL Frequency Set
    *********************/
    //step 26
    //DRV_WriteReg32(ARMCA15PLL_CON1, 0x80108000); // 1716MHz
    DRV_WriteReg32(ARMCA15PLL_CON1, 0x810F8000); // 806MHz

#if(6595 == MACH_TYPE)
    DRV_WriteReg32(ARMCA7PLL_CON1, 0x800B0000); // 1144MHz
#endif

    //step 27
    DRV_WriteReg32(MAINPLL_CON1, 0x800A8000); //1092MHz
    
    //step 28
    DRV_WriteReg32(MMPLL_CON1, 0x82118000); //455MHz
    
    //step 29
    DRV_WriteReg32(MSDCPLL_CON1, 0x800F6276); //800MHz
    
    //step 30
    //temp = ((~(DRV_Reg32(VENCPLL_CON1) & 0x80000000)) & 0x80000000);
    //DRV_WriteReg32(VENCPLL_CON1, temp | 0x000B6000);
    DRV_WriteReg32(VENCPLL_CON1, 0x800F6276); //800MHz
    //DRV_WriteReg32(VENCPLL_CON1, 0x8009A000);

    //step 31
    DRV_WriteReg32(TVDPLL_CON1, 0x80112276); // 594MHz

    //step 32
    DRV_WriteReg32(MPLL_CON1, 0x801C0000);
    DRV_WriteReg32(MPLL_CON0, 0x00010110); //52MHz

    //step 33
#if 1
    DRV_WriteReg32(VCODECPLL_CON1, 0x80130000); // 494MHz
#else
    DRV_WriteReg32(VCODECPLL_CON1, 0x80150000); // 546MHz
#endif

    //APLL1 and APLL2 use the default setting 
    /***********************
    * xPLL Frequency Enable
    ************************/
    //step 34
    temp = DRV_Reg32(ARMCA15PLL_CON0);
    DRV_WriteReg32(ARMCA15PLL_CON0, temp | 0x1);
#if(6595 == MACH_TYPE)
    temp = DRV_Reg32(ARMCA7PLL_CON0);
    DRV_WriteReg32(ARMCA7PLL_CON0, temp | 0x1);
#endif
    //step 35
    temp = DRV_Reg32(MAINPLL_CON0) & (~ 0x70);
/* CC: modify for testing */
#if 1
    /* CCI400 @ 500MHz */
    /* not divide by 2 */
#else
    /* CCI400 @ 250MHz */
    /* divide by 2 */
    temp |= 0x10;
#endif
    DRV_WriteReg32(MAINPLL_CON0, temp | 0x1);
    
    //step 36
    temp = DRV_Reg32(UNIVPLL_CON0);
    DRV_WriteReg32(UNIVPLL_CON0, temp | 0x1);
    
    //step 37
    temp = DRV_Reg32(MMPLL_CON0);
    DRV_WriteReg32(MMPLL_CON0, temp | 0x1);
    
    //step 38
    temp = DRV_Reg32(MSDCPLL_CON0);
    DRV_WriteReg32(MSDCPLL_CON0, temp | 0x1);
    
    //step 39
    temp = DRV_Reg32(VENCPLL_CON0);
    DRV_WriteReg32(VENCPLL_CON0, temp | 0x1);
    
    //step 40
    temp = DRV_Reg32(TVDPLL_CON0);
    DRV_WriteReg32(TVDPLL_CON0, temp | 0x1); 

    //step 41
    temp = DRV_Reg32(MPLL_CON0);
    DRV_WriteReg32(MPLL_CON0, temp | 0x1); 
    
    //step 42
    temp = DRV_Reg32(VCODECPLL_CON0);
    DRV_WriteReg32(VCODECPLL_CON0, temp | 0x1); 
    
    //step 43
    temp = DRV_Reg32(APLL1_CON0);
    DRV_WriteReg32(APLL1_CON0, temp | 0x1); 
    
    //step 44
    temp = DRV_Reg32(APLL2_CON0);
    DRV_WriteReg32(APLL2_CON0, temp | 0x1); 
    
    gpt_busy_wait_us(40); // wait for PLL stable (min delay is 20us)

    /***************
    * xPLL DIV RSTB
    ****************/
    //step 45
#if(6595 == MACH_TYPE)    
    temp = DRV_Reg32(ARMCA7PLL_CON0);
    DRV_WriteReg32(ARMCA7PLL_CON0, temp | 0x01000000);
#endif
    //step 46
    temp = DRV_Reg32(MAINPLL_CON0);
    DRV_WriteReg32(MAINPLL_CON0, temp | 0x01000000);
    
    //step 47
    temp = DRV_Reg32(UNIVPLL_CON0);
    DRV_WriteReg32(UNIVPLL_CON0, temp | 0x01000000);

    /*****************
    * xPLL HW Control
    ******************/
#if 0
    //default is SW mode, set HW mode after MEMPLL caribration
    //step 48
    temp = DRV_Reg32(AP_PLL_CON3);
    DRV_WriteReg32(AP_PLL_CON3, temp & 0xFFF4CCC0); // UNIVPLL SW Control

    //step 49
    temp = DRV_Reg32(AP_PLL_CON4);
    DRV_WriteReg32(AP_PLL_CON4, temp & 0xFFFFFFFC); // UNIVPLL,  SW Control
#endif
    /*************
    * MEMPLL Init
    **************/

//    mt_mempll_pre();

    /**************
    * INFRA CLKMUX
    ***************/

    temp = DRV_Reg32(TOP_DCMCTL);
    DRV_WriteReg32(TOP_DCMCTL, temp | 0x1); // enable infrasys DCM

    //CA7: INFRA_TOPCKGEN_CKDIV1[4:0](0x10001008)
    //CA15: INFRA_TOPCKGEN_CKDIV1[9:5](0x10001008)
    temp = DRV_Reg32(TOP_CKDIV1);
    
#if(6595 == MACH_TYPE)
    DRV_WriteReg32(TOP_CKDIV1, temp & 0xFFFFFC00); // CPU clock divide by 1
#else
    DRV_WriteReg32(TOP_CKDIV1, temp & 0xFFFFFC00); // CPU clock divide by 1
#endif
    //CA7: INFRA_TOPCKGEN_CKMUXSEL[1:0] (0x10001000) =1
    //CA15: INFRA_TOPCKGEN_CKMUXSEL[3:2] (0x10001000)= 1
    #if 0
    temp = DRV_Reg32(TOP_CKMUXSEL);
    DRV_WriteReg32(TOP_CKMUXSEL, temp | 0x5); // switch CA7_ck to ARMCA7PLL, and CA15_ck to ARMCA15PLL
    #endif

    /************
    * TOP CLKMUX
    *************/
//move to pll_post_init    
#if 0
    //DRV_WriteReg32(CLK_CFG_0, 0x01000101); //ddrphycfg_ck = 26MHz
    DRV_WriteReg32(CLK_CFG_0, 0x01000001); //mm_ck=vencpll_d2; ddrphycfg_ck=26MHz; not set mem_clk; axi_ck=syspll1_d2

    DRV_WriteReg32(CLK_CFG_1, 0x01010100); //mfg_ck=mmpll; venc_ck=vcodecpll; vdec_ck=vcodecpll; pwm=26Mhz

    DRV_WriteReg32(CLK_CFG_2, 0x01010000); //usb20_ck=univpll1_d8; spi_ck=syspll3_d2; uart=26M; camtg_ck=26Mhz

    DRV_WriteReg32(CLK_CFG_3, 0x02060201); //msdc30_1=MSDCPLL_D4; msdc50_0=msdcpll_d4; msdc50_0_hclk=syspll2_d2; usb30=univpll3_d2

    DRV_WriteReg32(CLK_CFG_4, 0x01000101); //aud_intbus=syspll1_d4; audio=26M; msdc30_3=univpll2_d2; msdc30_2=univpll2_d2

    DRV_WriteReg32(CLK_CFG_5, 0x01000100); //mjc=univpll_d3; NULL; scp= syspll1_d2; pmicspi=26MHz

/* CC: modify for testing */
#if 0
    DRV_WriteReg32(CLK_CFG_6, 0x01050101);//cci400 use mainpll
#else
    /* CCI400 use universal PLL */
    //DRV_WriteReg32(CLK_CFG_6, 0x01040101);

    DRV_WriteReg32(CLK_CFG_6, 0x01010101);//aud1=apll2; cci400=vencpll; irda=univpll2_d4; dpi0=tvdpll_d2
#endif

    DRV_WriteReg32(CLK_CFG_7, 0x01010101);//scam=syspll3_d2; axi_mfg=axi; mem_mfg=mmpll; aud2=apll2

    DRV_WriteReg32(CLK_SCP_CFG_0, 0x7FF); // enable scpsys clock off control
    DRV_WriteReg32(CLK_SCP_CFG_1, 0x15); // enable scpsys clock off control

    /*for MTCMOS*/
    spm_mtcmos_ctrl_disp(STA_POWER_ON);
#endif


/* remove from preloader, and clk power on at LK by ME5*/
#if 0
    /*turn on DISP*/
    DRV_WriteReg32(DISP_CG_CLR0, 0xFFFFFFFF);
    DRV_WriteReg32(DISP_CG_CLR1, 0x3FF);

    /*Turn on LARB0 OSTD*/
    temp = DRV_Reg32(SMI_LARB0_STAT);
    if(0 == temp)
    {
        DRV_WriteReg32(SMI_LARB0_OSTD_CTRL_EN , 0xffffffff);//Turn on the OSTD on LARB0
    }
    else
    {
//        print("LARB0 is busy , cannot set OSTD 0x%x\n" , temp);
    }

    /*Cautions !!! 
      If more MM engines will be enabled in preloader other than LARB0, 
      please clear LARB clock gate
      and set corresponded LARB# OSTD as following
    DRV_WriteReg32(SMI_LARB1_OSTD_CTRL_EN , 0xffffffff);
    DRV_WriteReg32(SMI_LARB2_OSTD_CTRL_EN , 0xffffffff);
    */
#endif    
}

int spm_mtcmos_ctrl_disp(int state)
{
    int err = 0;
    volatile unsigned int val;
    unsigned long flags;

    spm_write(SPM_POWERON_CONFIG_SET, (SPM_PROJECT_CODE << 16) | (1U << 0));

    if (state == STA_POWER_DOWN) {
        
        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) | SRAM_PDN);
#if 1
        while ((spm_read(SPM_DIS_PWR_CON) & DIS_SRAM_ACK) != DIS_SRAM_ACK) {
        }
#endif
        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) | PWR_ISO);

        val = spm_read(SPM_DIS_PWR_CON);
        val = (val & ~PWR_RST_B) | PWR_CLK_DIS;
        spm_write(SPM_DIS_PWR_CON, val);

        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) & ~(PWR_ON | PWR_ON_S));

        while ((spm_read(SPM_PWR_STATUS) & DIS_PWR_STA_MASK)
                || (spm_read(SPM_PWR_STATUS_2ND) & DIS_PWR_STA_MASK)) {
        }
    } else {    /* STA_POWER_ON */
        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) | PWR_ON);
        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) | PWR_ON_S);

        while (!(spm_read(SPM_PWR_STATUS) & DIS_PWR_STA_MASK) 
                || !(spm_read(SPM_PWR_STATUS_2ND) & DIS_PWR_STA_MASK)) {
        }

        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) & ~PWR_CLK_DIS);
        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) | PWR_RST_B);

        spm_write(SPM_DIS_PWR_CON, spm_read(SPM_DIS_PWR_CON) & ~SRAM_PDN);

#if 1
        while ((spm_read(SPM_DIS_PWR_CON) & DIS_SRAM_ACK)) {
        }
#endif
    }

    return err;
}



int spm_mtcmos_ctrl_mdsys1(int state)
{
    int err = 0;
    volatile unsigned int val;
    unsigned long flags;
    int count = 0;

    if (state == STA_POWER_DOWN) {
        spm_write(TOPAXI_PROT_EN, spm_read(TOPAXI_PROT_EN) | MD1_PROT_MASK);
        while ((spm_read(TOPAXI_PROT_STA1) & MD1_PROT_MASK) != MD1_PROT_MASK) {
            count++;
            if(count>1000)
                break;
        }

        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) | MD_SRAM_PDN);

        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) | PWR_ISO);

        val = spm_read(SPM_MD_PWR_CON);
        val = (val & ~PWR_RST_B) | PWR_CLK_DIS;
        spm_write(SPM_MD_PWR_CON, val);

        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) & ~(PWR_ON | PWR_ON_S));

        while ((spm_read(SPM_PWR_STATUS) & MD1_PWR_STA_MASK)
                || (spm_read(SPM_PWR_STATUS_2ND) & MD1_PWR_STA_MASK)) {
        }

        spm_write(AP_PLL_CON7, (spm_read(AP_PLL_CON7) | 0xF)); //force off LTE
        spm_write(0x10007054, 0x80000000);

    } else {    /* STA_POWER_ON */

#if(6795 == MACH_TYPE)
        spm_write(0x10209904, (spm_read(0x10209904) & (~0x1)));
#endif
        
        spm_write(AP_PLL_CON7, (spm_read(AP_PLL_CON7) & (~0x4))); //turn on LTE, clk
        spm_write(AP_PLL_CON7, (spm_read(AP_PLL_CON7) & (~0x1))); //turn on LTE, mtcmos
        spm_write(AP_PLL_CON7, (spm_read(AP_PLL_CON7) & (~0x8))); //turn on LTE, mtcmos + iso
        spm_write(AP_PLL_CON7, (spm_read(AP_PLL_CON7) & (~0x2))); //turn on LTE, memory
    
        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) | PWR_ON);
        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) | PWR_ON_S);

        while (!(spm_read(SPM_PWR_STATUS) & MD1_PWR_STA_MASK) 
                || !(spm_read(SPM_PWR_STATUS_2ND) & MD1_PWR_STA_MASK)) {
        }

        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) & ~PWR_CLK_DIS);
        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) & ~PWR_ISO);
        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) | PWR_RST_B);

        spm_write(SPM_MD_PWR_CON, spm_read(SPM_MD_PWR_CON) & ~MD_SRAM_PDN);

        spm_write(TOPAXI_PROT_EN, spm_read(TOPAXI_PROT_EN) & ~MD1_PROT_MASK);
        while (spm_read(TOPAXI_PROT_STA1) & MD1_PROT_MASK) {
        }
    }

    return err;
}


