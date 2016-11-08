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


#include "typedefs.h"
#include "platform.h"

#include "memory.h"
#include "emi.h"
#include "uart.h"
#include "dramc_register_64.h"
#include "dramc_pi_api_64.h"
#include "dramc_common.h"
#define MOD "MEM"

#include "wdt.h"
#include "emi_hw.h"

extern u32 g_ddr_reserve_enable;
extern  u32 g_ddr_reserve_success;
extern void enable_4GB_mode(void);

#if MEM_TEST
int complex_mem_test (unsigned int start, unsigned int len);
#endif

// --------------------------------------------------------
// init EMI
// --------------------------------------------------------
void
mt_mem_init (void)
{
  unsigned int emi_cona;
	U8 ucstatus = 0;
  /* DDR reserve mode no need to enable memory & test */
  //if((mtk_wdt_boot_check() == WDT_BY_PASS_PWK_REBOOT) && (g_ddr_reserve_enable==1) && (g_ddr_reserve_success==1))
  /*Note: factory reset failed workaround*/
  if((g_ddr_reserve_enable==1) && (g_ddr_reserve_success==1))
  {
    /*EMI register dummy read. Give clock to EMI APB register to avoid DRAM access hang...*/      
    emi_cona = *(volatile unsigned *)(EMI_CONA);      
    print("[DDR Reserve mode] EMI dummy read CONA = 0x%x\n", emi_cona);
    /* Reset EMI MPU protect setting - otherwise we can not use protected region during boot-up time */
    DRV_WriteReg32(EMI_MPUA, 0x0);
    DRV_WriteReg32(EMI_MPUB, 0x0);
    DRV_WriteReg32(EMI_MPUC, 0x0);
    DRV_WriteReg32(EMI_MPUD, 0x0);
    DRV_WriteReg32(EMI_MPUE, 0x0);
    DRV_WriteReg32(EMI_MPUF, 0x0);
    DRV_WriteReg32(EMI_MPUG, 0x0);
    DRV_WriteReg32(EMI_MPUH, 0x0);
    DRV_WriteReg32(EMI_MPUI, 0x0);
    DRV_WriteReg32(EMI_MPUJ, 0x0);
    DRV_WriteReg32(EMI_MPUK, 0x0);
    DRV_WriteReg32(EMI_MPUL, 0x0);
  }
  else /* normal boot */
  {
#if !(CFG_FPGA_PLATFORM)
    mt_set_emi ();
#else

    *(volatile unsigned *)(EMI_APB_BASE+0x00000000) = 0x50065002; //EMI_CONA , single channel/dual rank
	ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00101001);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), 0xC0063201);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x430), 0x10ff10ff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x434), 0xffffffff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x438), 0xffffffff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x43c), 0x0000001f);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x400), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x404), 0x00101000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x418), 0x0000011D); // single channel, dual rank
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x408), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x40C), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x410), 0x03555555);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x41C), 0x11111111);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x420), 0x11111111);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x424), 0x11111111);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x428), 0x0000ffff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x42C), 0x000000ff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1E0), 0x3601ffff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1F8), 0x0c002ec1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x23C), 0x2201ffff);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), 0x00406300);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x048), 0x2200110d);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x08C), 0x00000001);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0D8), 0x40500510);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0E4), 0x00002111);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0B8), 0x99169952);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0BC), 0x99109950);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x090), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x094), 0x80000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0DC), 0x83200200);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0E0), 0x12200200);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x118), 0x00000002);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0F0), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0F4), 0x11000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x168), 0x00000080);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x130), 0x30000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0D8), 0x40700510);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x004), 0xf00485a3);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x124), 0xc0000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x138), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x094), 0x40404040);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1C0), 0x8000c8b8);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), 0x07000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1f8), 0x0c002ec1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00100001);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), 0xc0063201);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x028), 0xf1200f01);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), 0x00251181);   // dual rank 
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x158), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000015);

    mcDELAY_US(200);	// tINIT3 > 200us

    // MR63 -> Reset
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x0000003F);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    mcDELAY_US(10);	// Wait >=10us if not check DAI.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);        

    // MR10 -> ZQ Init
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00FF000A);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    mcDELAY_US(1);		// tZQINIT>=1us
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
   
    // MR1             
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00230001);        
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    mcDELAY_US(1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    
    // MR2
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x00060002);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00001100);    
    mcDELAY_US(1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);


    mcDELAY_US(200);	// tINIT3 > 200us

    // MR63 -> Reset
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x1000003F);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    mcDELAY_US(10);	// Wait >=10us if not check DAI.
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);        

    // MR10 -> ZQ Init
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10FF000A);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    mcDELAY_US(1);		// tZQINIT>=1us
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
   
    // MR1             
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10230001);        
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    mcDELAY_US(1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);
    
    // MR2
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x088), 0x10060002);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000001);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00001100);    
    mcDELAY_US(1);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e4), 0x00000000);    


    //ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x110), 0x00251180);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000011);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00000001);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x084), 0x00000a56);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x080), 0x00000600);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x00c), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x000), 0x555844a3);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x044), 0xa80d0400);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1e8), 0x81000d20);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x008), 0x00406360);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x010), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x100), 0x01008110);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x01c), 0x12121212);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0f8), 0x00000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0fc), 0x07000000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1dc), 0xd2623840);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x1ec), 0x00100001);

    *(volatile unsigned *)(EMI_APB_BASE+0x00000060) = 0x00000400; //Enable EMI

    //Single Channel , Invert 180 degree
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0FC), 0x07020000);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x07c), 0xc0063201);
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x0e4), 0x00000011); 
    ucstatus |= ucDram_Register_Write(mcSET_DRAMC_REG_ADDR(0x080), 0x00000600);     
#endif

#if MEM_TEST
    {
        int i = 0;
        if ((i = complex_mem_test (0x40000000, MEM_TEST_SIZE)) == 0)
        {
            print ("[%s] complex R/W mem test pass\n", MOD);
       }
        else
        {
            print ("[%s] complex R/W mem test fail :%x\n", MOD, i);
            ASSERT(0);
        }
    }
#endif    
  }

  enable_4GB_mode();
#ifdef DDR_RESERVE_MODE  
  /* Always enable DDR-reserve mode */
  rgu_dram_reserved(1);
#endif

}



#if MEM_TEST
// --------------------------------------------------------
// do memory test
// --------------------------------------------------------
#define PATTERN1 0x5A5A5A5A
#define PATTERN2 0xA5A5A5A5

int
complex_mem_test (unsigned int start, unsigned int len)
{
    unsigned char *MEM8_BASE = (unsigned char *) start;
    unsigned short *MEM16_BASE = (unsigned short *) start;
    unsigned int *MEM32_BASE = (unsigned int *) start;
    unsigned int *MEM_BASE = (unsigned int *) start;
    unsigned char pattern8;
    unsigned short pattern16;
    unsigned int i, j, size, pattern32;
    unsigned int value;

    size = len >> 2;

    /* === Verify the tied bits (tied high) === */
    for (i = 0; i < size; i++)
    {
        MEM32_BASE[i] = 0;
    }

    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0)
        {
            return -1;
        }
        else
        {
            MEM32_BASE[i] = 0xffffffff;
        }
    }

    /* === Verify the tied bits (tied low) === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xffffffff)
        {
            return -2;
        }
        else
            MEM32_BASE[i] = 0x00;
    }

    /* === Verify pattern 1 (0x00~0xff) === */
    pattern8 = 0x00;
    for (i = 0; i < len; i++)
        MEM8_BASE[i] = pattern8++;
    pattern8 = 0x00;
    for (i = 0; i < len; i++)
    {
        if (MEM8_BASE[i] != pattern8++)
        {
            return -3;
        }
    }

    /* === Verify pattern 2 (0x00~0xff) === */
    pattern8 = 0x00;
    for (i = j = 0; i < len; i += 2, j++)
    {
        if (MEM8_BASE[i] == pattern8)
            MEM16_BASE[j] = pattern8;
        if (MEM16_BASE[j] != pattern8)
        {
            return -4;
        }
        pattern8 += 2;
    }

    /* === Verify pattern 3 (0x00~0xffff) === */
    pattern16 = 0x00;
    for (i = 0; i < (len >> 1); i++)
        MEM16_BASE[i] = pattern16++;
    pattern16 = 0x00;
    for (i = 0; i < (len >> 1); i++)
    {
        if (MEM16_BASE[i] != pattern16++)
        {
            return -5;
        }
    }

    /* === Verify pattern 4 (0x00~0xffffffff) === */
    pattern32 = 0x00;
    for (i = 0; i < (len >> 2); i++)
        MEM32_BASE[i] = pattern32++;
    pattern32 = 0x00;
    for (i = 0; i < (len >> 2); i++)
    {
        if (MEM32_BASE[i] != pattern32++)
        {
            return -6;
        }
    }

    /* === Pattern 5: Filling memory range with 0x44332211 === */
    for (i = 0; i < size; i++)
        MEM32_BASE[i] = 0x44332211;

    /* === Read Check then Fill Memory with a5a5a5a5 Pattern === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0x44332211)
        {
            return -7;
        }
        else
        {
            MEM32_BASE[i] = 0xa5a5a5a5;
        }
    }

    /* === Read Check then Fill Memory with 00 Byte Pattern at offset 0h === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xa5a5a5a5)
        {
            return -8;
        }
        else
        {
            MEM8_BASE[i * 4] = 0x00;
        }
    }

    /* === Read Check then Fill Memory with 00 Byte Pattern at offset 2h === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xa5a5a500)
        {
            return -9;
        }
        else
        {
            MEM8_BASE[i * 4 + 2] = 0x00;
        }
    }

    /* === Read Check then Fill Memory with 00 Byte Pattern at offset 1h === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xa500a500)
        {
            return -10;
        }
        else
        {
            MEM8_BASE[i * 4 + 1] = 0x00;
        }
    }

    /* === Read Check then Fill Memory with 00 Byte Pattern at offset 3h === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xa5000000)
        {
            return -11;
        }
        else
        {
            MEM8_BASE[i * 4 + 3] = 0x00;
        }
    }

    /* === Read Check then Fill Memory with ffff Word Pattern at offset 1h == */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0x00000000)
        {
            return -12;
        }
        else
        {
            MEM16_BASE[i * 2 + 1] = 0xffff;
        }
    }


    /* === Read Check then Fill Memory with ffff Word Pattern at offset 0h == */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xffff0000)
        {
            return -13;
        }
        else
        {
            MEM16_BASE[i * 2] = 0xffff;
        }
    }


    /*===  Read Check === */
    for (i = 0; i < size; i++)
    {
        if (MEM32_BASE[i] != 0xffffffff)
        {
            return -14;
        }
    }


    /************************************************
    * Additional verification 
    ************************************************/
    /* === stage 1 => write 0 === */

    for (i = 0; i < size; i++)
    {
        MEM_BASE[i] = PATTERN1;
    }


    /* === stage 2 => read 0, write 0xF === */
    for (i = 0; i < size; i++)
    {
        value = MEM_BASE[i];

        if (value != PATTERN1)
        {
            return -15;
        }
        MEM_BASE[i] = PATTERN2;
    }


    /* === stage 3 => read 0xF, write 0 === */
    for (i = 0; i < size; i++)
    {
        value = MEM_BASE[i];
        if (value != PATTERN2)
        {
            return -16;
        }
        MEM_BASE[i] = PATTERN1;
    }


    /* === stage 4 => read 0, write 0xF === */
    for (i = 0; i < size; i++)
    {
        value = MEM_BASE[i];
        if (value != PATTERN1)
        {
            return -17;
        }
        MEM_BASE[i] = PATTERN2;
    }


    /* === stage 5 => read 0xF, write 0 === */
    for (i = 0; i < size; i++)
    {
        value = MEM_BASE[i];
        if (value != PATTERN2)
        {
            return -18;
        }
        MEM_BASE[i] = PATTERN1;
    }


    /* === stage 6 => read 0 === */
    for (i = 0; i < size; i++)
    {
        value = MEM_BASE[i];
        if (value != PATTERN1)
        {
            return -19;
        }
    }


    /* === 1/2/4-byte combination test === */
    i = (unsigned int) MEM_BASE;

    while (i < (unsigned int) MEM_BASE + (size << 2))
    {
        *((unsigned char *) i) = 0x78;
        i += 1;
        *((unsigned char *) i) = 0x56;
        i += 1;
        *((unsigned short *) i) = 0x1234;
        i += 2;
        *((unsigned int *) i) = 0x12345678;
        i += 4;
        *((unsigned short *) i) = 0x5678;
        i += 2;
        *((unsigned char *) i) = 0x34;
        i += 1;
        *((unsigned char *) i) = 0x12;
        i += 1;
        *((unsigned int *) i) = 0x12345678;
        i += 4;
        *((unsigned char *) i) = 0x78;
        i += 1;
        *((unsigned char *) i) = 0x56;
        i += 1;
        *((unsigned short *) i) = 0x1234;
        i += 2;
        *((unsigned int *) i) = 0x12345678;
        i += 4;
        *((unsigned short *) i) = 0x5678;
        i += 2;
        *((unsigned char *) i) = 0x34;
        i += 1;
        *((unsigned char *) i) = 0x12;
        i += 1;
        *((unsigned int *) i) = 0x12345678;
        i += 4;
    }
    for (i = 0; i < size; i++)
    {
        value = MEM_BASE[i];
        if (value != 0x12345678)
        {
            return -20;
        }
    }


    /* === Verify pattern 1 (0x00~0xff) === */
    pattern8 = 0x00;
    MEM8_BASE[0] = pattern8;
    for (i = 0; i < size * 4; i++)
    {
        unsigned char waddr8, raddr8;
        waddr8 = i + 1;
        raddr8 = i;
        if (i < size * 4 - 1)
            MEM8_BASE[waddr8] = pattern8 + 1;
        if (MEM8_BASE[raddr8] != pattern8)
        {
            return -21;
        }
        pattern8++;
    }


    /* === Verify pattern 2 (0x00~0xffff) === */
    pattern16 = 0x00;
    MEM16_BASE[0] = pattern16;
    for (i = 0; i < size * 2; i++)
    {
        if (i < size * 2 - 1)
            MEM16_BASE[i + 1] = pattern16 + 1;
        if (MEM16_BASE[i] != pattern16)
        {
            return -22;
        }
        pattern16++;
    }


    /* === Verify pattern 3 (0x00~0xffffffff) === */
    pattern32 = 0x00;
    MEM32_BASE[0] = pattern32;
    for (i = 0; i < size; i++)
    {
        if (i < size - 1)
            MEM32_BASE[i + 1] = pattern32 + 1;
        if (MEM32_BASE[i] != pattern32)
        {
            return -23;
        }
        pattern32++;
    }

    return 0;
}

#endif
