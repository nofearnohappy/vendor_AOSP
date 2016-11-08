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

/******************************************************************************
 * mt6577_gpio.c - MT6577 Linux GPIO Device Driver
 * 
 * Copyright 2008-2009 MediaTek Co.,Ltd.
 * 
 * DESCRIPTION:
 *     This file provid the other drivers GPIO relative functions
 *
 ******************************************************************************/

#include <typedefs.h>
#include <gpio.h>
#include <platform.h>
//#include <pmic_wrap_init.h> 

//#include <cust_power.h>
/******************************************************************************
 MACRO Definition
******************************************************************************/
//#define  GIO_SLFTEST            
#define GPIO_DEVICE "mt-gpio"
#define VERSION     "$Revision$"
/*---------------------------------------------------------------------------*/
#define GPIO_WR32(addr, data)   __raw_writel(data, addr)
#define GPIO_RD32(addr)         __raw_readl(addr)
#define GPIO_SET_BITS(BIT,REG)   ((*(volatile u32*)(REG)) = (u32)(BIT))
#define GPIO_CLR_BITS(BIT,REG)   ((*(volatile u32*)(REG)) &= ~((u32)(BIT)))
//S32 pwrap_read( U32  adr, U32 *rdata ){return 0;}
//S32 pwrap_write( U32  adr, U32  wdata ){return 0;}
	
/*---------------------------------------------------------------------------*/
#define TRUE                   1
#define FALSE                  0
/*---------------------------------------------------------------------------*/
#define MAX_GPIO_REG_BITS      16
#define MAX_GPIO_MODE_PER_REG  5
#define GPIO_MODE_BITS         3 
/*---------------------------------------------------------------------------*/
#define GPIOTAG                "[GPIO] "
#define GPIOLOG(fmt, arg...)   //printf(GPIOTAG fmt, ##arg)
#define GPIOMSG(fmt, arg...)   //printf(fmt, ##arg)
#define GPIOERR(fmt, arg...)   //printf(GPIOTAG "%5d: "fmt, __LINE__, ##arg)
#define GPIOFUC(fmt, arg...)   //printf(GPIOTAG "%s\n", __FUNCTION__)
#define GIO_INVALID_OBJ(ptr)   ((ptr) != gpio_obj)
/******************************************************************************
Enumeration/Structure
******************************************************************************/
/*-------for special kpad pupd-----------*/
struct kpad_pupd {
	unsigned char 	pin;
	unsigned char   reg;
	unsigned char   bit;
};
static struct kpad_pupd kpad_pupd_spec[] = {
	{GPIO119,	0,	2},     //KROW0
	{GPIO120,	0,	6},     //KROW1
	{GPIO121,	0,	10},    //KROW2
	{GPIO122,	1,	2},     //KCOL0
	{GPIO123,	1,	6},     //KCOL1
	{GPIO124,	1,	10}     //KCOL2
};
/*-------for special sim pupd-----------*/
struct sim_pupd {
	unsigned char 	pin;
	unsigned char   reg;
	unsigned char   bit;
};
static struct sim_pupd sim_pupd_spec[] = {
	{GPIO17,	2,	2},       //sim1_clk
	{GPIO18,	2,	10},      //sim1_rst
	{GPIO19,	2,	6},       //sim1_io
	{GPIO20,	3,	2},       //sim2_clk
	{GPIO21,	3,	10},      //sim2_rst
	{GPIO22,	3,	6}        //sim2_io
};
/*---------------------------------------*/
struct mt_gpio_obj {
    GPIO_REGS       *reg;
};
static struct mt_gpio_obj gpio_dat = {
    .reg  = (GPIO_REGS*)(GPIO_BASE),
};
static struct mt_gpio_obj *gpio_obj = &gpio_dat;
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_dir_chip(u32 pin, u32 dir)
{
    u32 pos;
    u32 bit;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= GPIO197)
        return -ERINVAL;

    if (dir >= GPIO_DIR_MAX)
        return -ERINVAL;
    
    pos = pin / MAX_GPIO_REG_BITS;
    bit = pin % MAX_GPIO_REG_BITS;
    
    if (dir == GPIO_DIR_IN)
        GPIO_SET_BITS((1L << bit), &obj->reg->dir[pos].rst);
    else
        GPIO_SET_BITS((1L << bit), &obj->reg->dir[pos].set);
    return RSUCCESS;
    
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_dir_chip(u32 pin)
{    
    u32 pos;
    u32 bit;
    u32 reg;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;
    
    if (pin >= GPIO197)
        return -ERINVAL;
    
    pos = pin / MAX_GPIO_REG_BITS;
    bit = pin % MAX_GPIO_REG_BITS;
    
    reg = GPIO_RD32(&obj->reg->dir[pos].val);
    return (((reg & (1L << bit)) != 0)? 1: 0);        
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_pull_enable_chip(u32 pin, u32 enable)
{
    u32 pos;
    u32 bit;
    u32 reg;
    u32 mask;
    u32 i;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;
    
    if (pin >= GPIO197)
        return -ERINVAL;

    if (enable >= GPIO_PULL_EN_MAX)
        return -ERINVAL;

	/*****************for special kpad pupd, NOTE DEFINITION REVERSE!!!*****************/
	for(i = 0; i < sizeof(kpad_pupd_spec)/sizeof(kpad_pupd_spec[0]); i++){
		if (pin == kpad_pupd_spec[i].pin){
			if (enable == GPIO_PULL_DISABLE){
				GPIO_SET_BITS((3L << (kpad_pupd_spec[i].bit-2)), &obj->reg->kpad_ctrl[kpad_pupd_spec[i].reg].rst);
			} else {
				GPIO_SET_BITS((1L << (kpad_pupd_spec[i].bit-2)), &obj->reg->kpad_ctrl[kpad_pupd_spec[i].reg].set);    //single key: 75K 
			}
			return RSUCCESS;
		}
	}
	/*********************************sim gpio pupd, sim-IO: pullUp 5K*********************************/
	for(i = 0; i < sizeof(sim_pupd_spec)/sizeof(sim_pupd_spec[0]); i++){
		if (pin == sim_pupd_spec[i].pin){
			if (enable == GPIO_PULL_DISABLE){
				GPIO_SET_BITS((3L << (sim_pupd_spec[i].bit-2)), &obj->reg->sim_ctrl[sim_pupd_spec[i].reg].rst);
			} else {
				GPIO_SET_BITS((2L << (sim_pupd_spec[i].bit-2)), &obj->reg->sim_ctrl[sim_pupd_spec[i].reg].set);       //5K: 2'b10
				GPIO_SET_BITS((1L << (sim_pupd_spec[i].bit-2)), &obj->reg->sim_ctrl[sim_pupd_spec[i].reg].rst);
			}
			return RSUCCESS;
		}
	}
	/********************************* DPI special *********************************/
	if (pin == GPIO138) {       //dpi ck
		if (enable == GPIO_PULL_DISABLE) {
			GPIO_SET_BITS(3L, &obj->reg->dpi_ctrl[0].rst);
		} else {
			GPIO_SET_BITS(2L, &obj->reg->dpi_ctrl[0].set);    //defeature to 50K, jerick
			GPIO_SET_BITS(1L, &obj->reg->dpi_ctrl[0].rst);
		}
		return RSUCCESS;
	} else if ( (pin >= GPIO139) && (pin <= GPIO153) ) {
		if (enable == GPIO_PULL_ENABLE) {
			if (pin == GPIO139) {         //dpi DE
				GPIO_SET_BITS(1L, &obj->reg->dpi_ctrl[1].set);
			} else if (pin == GPIO140) {  //dpi D0
				GPIO_SET_BITS(1L, &obj->reg->dpi_ctrl[2].set);
			} else if (pin == GPIO141) {  //dpi D1
				GPIO_SET_BITS((1L<<2), &obj->reg->dpi_ctrl[2].set);
			} else if (pin == GPIO142) {  //dpi D2
				GPIO_SET_BITS((1L<<4), &obj->reg->dpi_ctrl[2].set);
			} else if (pin == GPIO143) {  //dpi D3
				GPIO_SET_BITS((1L<<6), &obj->reg->dpi_ctrl[2].set);
			} else if (pin == GPIO144) {  //dpi D4
				GPIO_SET_BITS((1L<<4), &obj->reg->dpi_ctrl[0].set);
			} else if (pin == GPIO145) {  //dpi D5
				GPIO_SET_BITS((1L<<6), &obj->reg->dpi_ctrl[0].set);
			} else if (pin == GPIO146) {  //dpi D6
				GPIO_SET_BITS((1L<<6), &obj->reg->dpi_ctrl[1].set);
			} else if (pin == GPIO147) {  //dpi D7
				GPIO_SET_BITS((1L<<5), &obj->reg->pullen[13].set);
			} else if (pin == GPIO148) {  //dpi D8
				GPIO_SET_BITS((1L<<7), &obj->reg->pullen[13].set);
			} else if (pin == GPIO149) {  //dpi D9
				GPIO_SET_BITS((1L<<9), &obj->reg->pullen[13].set);
			} else if (pin == GPIO150) {  //dpi D10
				GPIO_SET_BITS((1L<<11), &obj->reg->pullen[13].set);
			} else if (pin == GPIO151) {  //dpi D11
				GPIO_SET_BITS((1L<<13), &obj->reg->pullen[13].set);
			} else if (pin == GPIO152) {  //dpi HSYNC
				GPIO_SET_BITS((1L<<2), &obj->reg->dpi_ctrl[1].set);
			} else if (pin == GPIO153) {  //dpi VSYNC
				GPIO_SET_BITS((1L<<4), &obj->reg->dpi_ctrl[1].set);
			}
			return RSUCCESS;
		} else {      //disable
			if (pin == GPIO139) {         //dpi DE
				GPIO_SET_BITS(1L, &obj->reg->dpi_ctrl[1].rst);
			} else if (pin == GPIO140) {  //dpi D0
				GPIO_SET_BITS(1L, &obj->reg->dpi_ctrl[2].rst);
			} else if (pin == GPIO141) {  //dpi D1
				GPIO_SET_BITS((1L<<2), &obj->reg->dpi_ctrl[2].rst);
			} else if (pin == GPIO142) {  //dpi D2
				GPIO_SET_BITS((1L<<4), &obj->reg->dpi_ctrl[2].rst);
			} else if (pin == GPIO143) {  //dpi D3
				GPIO_SET_BITS((1L<<6), &obj->reg->dpi_ctrl[2].rst);
			} else if (pin == GPIO144) {  //dpi D4
				GPIO_SET_BITS((1L<<4), &obj->reg->dpi_ctrl[0].rst);
			} else if (pin == GPIO145) {  //dpi D5
				GPIO_SET_BITS((1L<<6), &obj->reg->dpi_ctrl[0].rst);
			} else if (pin == GPIO146) {  //dpi D6
				GPIO_SET_BITS((1L<<6), &obj->reg->dpi_ctrl[1].rst);
			} else if (pin == GPIO147) {  //dpi D7
				GPIO_SET_BITS((1L<<5), &obj->reg->pullen[13].rst);
			} else if (pin == GPIO148) {  //dpi D8
				GPIO_SET_BITS((1L<<7), &obj->reg->pullen[13].rst);
			} else if (pin == GPIO149) {  //dpi D9
				GPIO_SET_BITS((1L<<9), &obj->reg->pullen[13].rst);
			} else if (pin == GPIO150) {  //dpi D10
				GPIO_SET_BITS((1L<<11), &obj->reg->pullen[13].rst);
			} else if (pin == GPIO151) {  //dpi D11
				GPIO_SET_BITS((1L<<13), &obj->reg->pullen[13].rst);
			} else if (pin == GPIO152) {  //dpi HSYNC
				GPIO_SET_BITS((1L<<2), &obj->reg->dpi_ctrl[1].rst);
			} else if (pin == GPIO153) {  //dpi VSYNC
				GPIO_SET_BITS((1L<<4), &obj->reg->dpi_ctrl[1].rst);
			}
			return RSUCCESS;
		}
	}
	/********************************* MSDC special *********************************/
	if (pin == GPIO164) {         //ms0 DS
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc0_ctrl4.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc0_ctrl4.set);   //1L:10K
		return RSUCCESS;
	} else if (pin == GPIO165) {  //ms0 RST
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc0_ctrl3.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc0_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO162) {  //ms0 cmd
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc0_ctrl1.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc0_ctrl1.set);
		return RSUCCESS;
	} else if (pin == GPIO163) {  //ms0 clk
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc0_ctrl0.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc0_ctrl0.set);
		return RSUCCESS;
	} else if ( (pin >= GPIO154) && (pin <= GPIO161) ) {  //ms0 data0~7
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc0_ctrl2.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc0_ctrl2.set);
		return RSUCCESS;

	} else if (pin == GPIO170) {  //ms1 cmd
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc1_ctrl1.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc1_ctrl1.set);
		return RSUCCESS;
	} else if (pin == GPIO171) {  //ms1 dat0
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc1_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO172) {  //ms1 dat1
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 4), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 4), &obj->reg->msdc1_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO173) {  //ms1 dat2
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 8), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 8), &obj->reg->msdc1_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO174) {  //ms1 dat3
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 12), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 12), &obj->reg->msdc1_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO175) {  //ms1 clk
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc1_ctrl0.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc1_ctrl0.set);
		return RSUCCESS;

	} else if (pin == GPIO100) {  //ms2 dat0
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc2_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO101) {  //ms2 dat1
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 4), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 4), &obj->reg->msdc2_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO102) {  //ms2 dat2
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 8), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 8), &obj->reg->msdc2_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO103) {  //ms2 dat3
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 12), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 12), &obj->reg->msdc2_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO104) {  //ms2 clk 
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc2_ctrl0.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc2_ctrl0.set);
		return RSUCCESS;
	} else if (pin == GPIO105) {  //ms2 cmd
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc2_ctrl1.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc2_ctrl1.set);
		return RSUCCESS;

	} else if (pin == GPIO23) {  //ms3 dat0
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc3_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO24) {  //ms3 dat1
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 4), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 4), &obj->reg->msdc3_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO25) {  //ms3 dat2
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 8), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 8), &obj->reg->msdc3_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO26) {  //ms3 dat3
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS((3L << 12), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 12), &obj->reg->msdc3_ctrl3.set);
		return RSUCCESS;
	} else if (pin == GPIO27) {  //ms3 clk 
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc3_ctrl0.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc3_ctrl0.set);
		return RSUCCESS;
	} else if (pin == GPIO28) {  //ms3 cmd
		(enable == GPIO_PULL_DISABLE)? GPIO_SET_BITS(3L, &obj->reg->msdc3_ctrl1.rst) : GPIO_SET_BITS(1L, &obj->reg->msdc3_ctrl1.set);
		return RSUCCESS;
	}

	if (0){
		return GPIO_PULL_EN_UNSUPPORTED;
	}else{
		pos = pin / MAX_GPIO_REG_BITS;
		bit = pin % MAX_GPIO_REG_BITS;

		if (enable == GPIO_PULL_DISABLE)
			GPIO_SET_BITS((1L << bit), &obj->reg->pullen[pos].rst);
		else
			GPIO_SET_BITS((1L << bit), &obj->reg->pullen[pos].set);
	}
    return RSUCCESS;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_pull_enable_chip(u32 pin)
{
    u32 pos;
    u32 bit;
    u32 reg;
    u32 i;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;
    
    if (pin >= GPIO197)
        return -ERINVAL;
	
	/*****************for special kpad pupd, NOTE DEFINITION REVERSE!!!*****************/
	for(i = 0; i < sizeof(kpad_pupd_spec)/sizeof(kpad_pupd_spec[0]); i++){
        if (pin == kpad_pupd_spec[i].pin){
			return (((GPIO_RD32(&obj->reg->kpad_ctrl[kpad_pupd_spec[i].reg].val) & (3L << (kpad_pupd_spec[i].bit-2))) != 0)? 1: 0);        
        }
	}
	/*********************************for special sim pupd*********************************/
	for(i = 0; i < sizeof(sim_pupd_spec)/sizeof(sim_pupd_spec[0]); i++){
		if (pin == sim_pupd_spec[i].pin){
			return (((GPIO_RD32(&obj->reg->sim_ctrl[sim_pupd_spec[i].reg].val) & (3L << (sim_pupd_spec[i].bit-2))) != 0)? 1: 0);        
		}
	}
	/*********************************DPI special*********************************/
	if (pin == GPIO138) {	        //dpi ck
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[0].val) & (3L << 0)) != 0)? 1: 0); 
	} else if (pin == GPIO139) {    //dpi DE
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 0)) != 0)? 1: 0); 
	} else if (pin == GPIO140) {	//dpi D0
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 0)) != 0)? 1: 0); 
	} else if (pin == GPIO141) {    //dpi D1
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 2)) != 0)? 1: 0); 
	} else if (pin == GPIO142) {    //dpi D2
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 4)) != 0)? 1: 0); 
	} else if (pin == GPIO143) {    //dpi D3
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 6)) != 0)? 1: 0); 
	} else if (pin == GPIO144) {    //dpi D4
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[0].val) & (1L << 4)) != 0)? 1: 0); 
	} else if (pin == GPIO145) {    //dpi D5
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[0].val) & (1L << 6)) != 0)? 1: 0); 
	} else if (pin == GPIO146) {    //dpi D6
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 6)) != 0)? 1: 0); 
	} else if (pin == GPIO147) {    //dpi D7
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 5)) != 0)? 1: 0); 
	} else if (pin == GPIO148) {    //dpi D8
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 7)) != 0)? 1: 0); 
	} else if (pin == GPIO149) {    //dpi D9
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 9)) != 0)? 1: 0); 
	} else if (pin == GPIO150) {    //dpi D10
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 11)) != 0)? 1: 0); 
	} else if (pin == GPIO151) {    //dpi D11
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 13)) != 0)? 1: 0); 
	} else if (pin == GPIO152) {    //dpi HSYNC
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 2)) != 0)? 1: 0); 
	} else if (pin == GPIO153) {    //dpi VSYNC
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 4)) != 0)? 1: 0); 
	}
    /*********************************MSDC special pupd*********************************/
	if (pin == GPIO164) {         //ms0 DS
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl4.val) & (3L << 0)) != 0)? 1: 0); 
	} else if (pin == GPIO165) {  //ms0 RST
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl3.val) & (3L << 0)) != 0)? 1: 0);  
	} else if (pin == GPIO162) {  //ms0 cmd
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl1.val) & (3L << 0)) != 0)? 1: 0);  
	} else if (pin == GPIO163) {  //ms0 clk
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl0.val) & (3L << 0)) != 0)? 1: 0);
	} else if ((pin >= GPIO154) && (pin <= GPIO161)) {	  //ms0 data0~7
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl2.val) & (3L << 0)) != 0)? 1: 0);
	
	} else if (pin == GPIO170) {  //ms1 cmd
        return (((GPIO_RD32(&obj->reg->msdc1_ctrl1.val) & (3L << 0)) != 0)? 1: 0);       
	} else if (pin == GPIO171) {  //ms1 dat0
        return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (3L << 0)) != 0)? 1: 0);    
	} else if (pin == GPIO172) {  //ms1 dat1
        return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (3L << 4)) != 0)? 1: 0);        
	} else if (pin == GPIO173) {  //ms1 dat2
        return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (3L << 8)) != 0)? 1: 0);        
	} else if (pin == GPIO174) {  //ms1 dat3
        return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (3L << 12)) != 0)? 1: 0);        
	} else if (pin == GPIO175) {  //ms1 clk
        return (((GPIO_RD32(&obj->reg->msdc1_ctrl0.val) & (3L << 0)) != 0)? 1: 0);        

	} else if (pin == GPIO100) {  //ms2 dat0
        return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (3L << 0)) != 0)? 1: 0);        
	} else if (pin == GPIO101) {  //ms2 dat1
        return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (3L << 4)) != 0)? 1: 0);        
	} else if (pin == GPIO102) {  //ms2 dat2
        return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (3L << 8)) != 0)? 1: 0);        
	} else if (pin == GPIO103) {  //ms2 dat3
        return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (3L << 12)) != 0)? 1: 0);        
	} else if (pin == GPIO104) {  //ms2 clk 
        return (((GPIO_RD32(&obj->reg->msdc2_ctrl0.val) & (3L << 0)) != 0)? 1: 0);        
	} else if (pin == GPIO105) {  //ms2 cmd
        return (((GPIO_RD32(&obj->reg->msdc2_ctrl1.val) & (3L << 0)) != 0)? 1: 0);        

	} else if (pin == GPIO23) {  //ms3 dat0
        return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (3L << 0)) != 0)? 1: 0);        
	} else if (pin == GPIO24) {  //ms3 dat1
        return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (3L << 4)) != 0)? 1: 0);        
	} else if (pin == GPIO25) {  //ms3 dat2
        return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (3L << 8)) != 0)? 1: 0);        
	} else if (pin == GPIO26) {  //ms3 dat3
        return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (3L << 12)) != 0)? 1: 0);        
	} else if (pin == GPIO27) {  //ms3 clk 
        return (((GPIO_RD32(&obj->reg->msdc3_ctrl0.val) & (3L << 0)) != 0)? 1: 0);        
	} else if (pin == GPIO28) {  //ms3 cmd
        return (((GPIO_RD32(&obj->reg->msdc3_ctrl1.val) & (3L << 0)) != 0)? 1: 0);        
	}

	if (0){
		return GPIO_PULL_EN_UNSUPPORTED;
	}else{
		pos = pin / MAX_GPIO_REG_BITS;
		bit = pin % MAX_GPIO_REG_BITS;

		reg = GPIO_RD32(&obj->reg->pullen[pos].val);
	}
	return (((reg & (1L << bit)) != 0)? 1: 0);        
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_pull_select_chip(u32 pin, u32 select)
{
    u32 pos;
    u32 bit;
    u32 reg;
    u32 mask;
	u32 i;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= GPIO197)
        return -ERINVAL;
    
    if (select >= GPIO_PULL_MAX)
        return -ERINVAL;

	/***********************for special kpad pupd, NOTE DEFINITION REVERSE!!!**************************/
	for(i = 0; i < sizeof(kpad_pupd_spec)/sizeof(kpad_pupd_spec[0]); i++){
		if (pin == kpad_pupd_spec[i].pin){
			if (select == GPIO_PULL_DOWN)
				GPIO_SET_BITS((1L << kpad_pupd_spec[i].bit), &obj->reg->kpad_ctrl[kpad_pupd_spec[i].reg].set);
			else
				GPIO_SET_BITS((1L << kpad_pupd_spec[i].bit), &obj->reg->kpad_ctrl[kpad_pupd_spec[i].reg].rst);
			return RSUCCESS;
		}
	}
	/*************************for special sim pupd*************************/
	for(i = 0; i < sizeof(sim_pupd_spec)/sizeof(sim_pupd_spec[0]); i++){
		if (pin == sim_pupd_spec[i].pin){
			if (select == GPIO_PULL_DOWN)
				GPIO_SET_BITS((1L << sim_pupd_spec[i].bit), &obj->reg->sim_ctrl[sim_pupd_spec[i].reg].set);
			else
				GPIO_SET_BITS((1L << sim_pupd_spec[i].bit), &obj->reg->sim_ctrl[sim_pupd_spec[i].reg].rst);
			return RSUCCESS;
		}
	}
	/* ************************DPI special *************************/
	if (pin == GPIO138) {	        //dpi ck
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<2), &obj->reg->dpi_ctrl[0].set) : GPIO_SET_BITS( (1L<<2), &obj->reg->dpi_ctrl[0].rst);
	} else if (pin == GPIO139) {    //dpi DE
		//GPIO_SET_BITS( (1L<<0), &obj->reg->dpi_ctrl[1].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<1), &obj->reg->dpi_ctrl[1].set) : GPIO_SET_BITS( (1L<<1), &obj->reg->dpi_ctrl[1].rst);
	} else if (pin == GPIO140) {	//dpi D0
		//GPIO_SET_BITS( (1L<<0), &obj->reg->dpi_ctrl[2].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<1), &obj->reg->dpi_ctrl[2].set) : GPIO_SET_BITS( (1L<<1), &obj->reg->dpi_ctrl[2].rst);
	} else if (pin == GPIO141) {    //dpi D1
		//GPIO_SET_BITS( (1L<<2), &obj->reg->dpi_ctrl[2].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<3), &obj->reg->dpi_ctrl[2].set) : GPIO_SET_BITS( (1L<<3), &obj->reg->dpi_ctrl[2].rst);
	} else if (pin == GPIO142) {    //dpi D2
		//GPIO_SET_BITS( (1L<<4), &obj->reg->dpi_ctrl[2].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<5), &obj->reg->dpi_ctrl[2].set) : GPIO_SET_BITS( (1L<<5), &obj->reg->dpi_ctrl[2].rst);
	} else if (pin == GPIO143) {    //dpi D3
		//GPIO_SET_BITS( (1L<<6), &obj->reg->dpi_ctrl[2].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<7), &obj->reg->dpi_ctrl[2].set) : GPIO_SET_BITS( (1L<<7), &obj->reg->dpi_ctrl[2].rst);
	} else if (pin == GPIO144) {    //dpi D4
		//GPIO_SET_BITS( (1L<<4), &obj->reg->dpi_ctrl[0].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<5), &obj->reg->dpi_ctrl[0].set) : GPIO_SET_BITS( (1L<<5), &obj->reg->dpi_ctrl[0].rst);
	} else if (pin == GPIO145) {    //dpi D5
		//GPIO_SET_BITS( (1L<<6), &obj->reg->dpi_ctrl[0].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<7), &obj->reg->dpi_ctrl[0].set) : GPIO_SET_BITS( (1L<<7), &obj->reg->dpi_ctrl[0].rst);
	} else if (pin == GPIO146) {    //dpi D6
		//GPIO_SET_BITS( (1L<<6), &obj->reg->dpi_ctrl[1].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<7), &obj->reg->dpi_ctrl[1].set) : GPIO_SET_BITS( (1L<<7), &obj->reg->dpi_ctrl[1].rst);
	} else if (pin == GPIO147) {    //dpi D7
		//GPIO_SET_BITS( (1L<<5), &obj->reg->pullen[13].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<6), &obj->reg->pullen[13].set) : GPIO_SET_BITS( (1L<<6), &obj->reg->pullen[13].rst);
	} else if (pin == GPIO148) {    //dpi D8
		//GPIO_SET_BITS( (1L<<7), &obj->reg->pullen[13].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<8), &obj->reg->pullen[13].set) : GPIO_SET_BITS( (1L<<8), &obj->reg->pullen[13].rst);
	} else if (pin == GPIO149) {    //dpi D9
		//GPIO_SET_BITS( (1L<<9), &obj->reg->pullen[13].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<10), &obj->reg->pullen[13].set) : GPIO_SET_BITS( (1L<<10), &obj->reg->pullen[13].rst);
	} else if (pin == GPIO150) {    //dpi D10
		//GPIO_SET_BITS( (1L<<11), &obj->reg->pullen[13].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<12), &obj->reg->pullen[13].set) : GPIO_SET_BITS( (1L<<12), &obj->reg->pullen[13].rst);
	} else if (pin == GPIO151) {    //dpi D11
		//GPIO_SET_BITS( (1L<<13), &obj->reg->pullen[13].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<14), &obj->reg->pullen[13].set) : GPIO_SET_BITS( (1L<<14), &obj->reg->pullen[13].rst);
	} else if (pin == GPIO152) {    //dpi HSYNC
		//GPIO_SET_BITS( (1L<<2), &obj->reg->dpi_ctrl[1].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<3), &obj->reg->dpi_ctrl[1].set) : GPIO_SET_BITS( (1L<<3), &obj->reg->dpi_ctrl[1].rst);
	} else if (pin == GPIO153) {    //dpi VSYNC
		//GPIO_SET_BITS( (1L<<4), &obj->reg->dpi_ctrl[1].set);
		(select == GPIO_PULL_DOWN)? GPIO_SET_BITS( (1L<<5), &obj->reg->dpi_ctrl[1].set) : GPIO_SET_BITS( (1L<<5), &obj->reg->dpi_ctrl[1].rst);
	}
	if ((pin >= GPIO138) && (pin <= GPIO153)) return RSUCCESS;
	/*************************************MSDC special pupd*************************/
	if (pin == GPIO164) {         //ms0 DS
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl4.rst) : GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl4.set);
	} else if (pin == GPIO165) {  //ms0 RST
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl3.rst) : GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl3.set);
	} else if (pin == GPIO162) {  //ms0 cmd
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl1.rst) : GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl1.set);
	} else if (pin == GPIO163) {  //ms0 clk
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl0.rst) : GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl0.set);
	} else if ( (pin >= GPIO154) && (pin <= GPIO161) ) {  //ms0 data0~7
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl2.rst) : GPIO_SET_BITS((1L<<2), &obj->reg->msdc0_ctrl2.set);

	} else if (pin == GPIO170) {   //ms1 cmd
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc1_ctrl1.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc1_ctrl1.set);
	} else if (pin == GPIO171) {   //ms1 dat0
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc1_ctrl3.set);
	} else if (pin == GPIO172) {   //ms1 dat1
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 6), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 6), &obj->reg->msdc1_ctrl3.set);
	} else if (pin == GPIO173) {   //ms1 dat2
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 10), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 10), &obj->reg->msdc1_ctrl3.set);
	} else if (pin == GPIO174) {   //ms1 dat3
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 14), &obj->reg->msdc1_ctrl3.rst) : GPIO_SET_BITS((1L << 14), &obj->reg->msdc1_ctrl3.set);
	} else if (pin == GPIO175) {   //ms1 clk
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc1_ctrl0.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc1_ctrl0.set);

	} else if (pin == GPIO100) {   //ms2 dat0
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc2_ctrl3.set);
	} else if (pin == GPIO101) {   //ms2 dat1
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 6), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 6), &obj->reg->msdc2_ctrl3.set);
	} else if (pin == GPIO102) {   //ms2 dat2
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 10), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 10), &obj->reg->msdc2_ctrl3.set);
	} else if (pin == GPIO103) {   //ms2 dat3
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 14), &obj->reg->msdc2_ctrl3.rst) : GPIO_SET_BITS((1L << 14), &obj->reg->msdc2_ctrl3.set);
	} else if (pin == GPIO104) {   //ms2 clk 
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc2_ctrl0.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc2_ctrl0.set);
	} else if (pin == GPIO105) {   //ms2 cmd
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc2_ctrl1.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc2_ctrl1.set);

	} else if (pin == GPIO23) {   //ms3 dat0
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc3_ctrl3.set);
	} else if (pin == GPIO24) {   //ms3 dat1
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 6), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 6), &obj->reg->msdc3_ctrl3.set);
	} else if (pin == GPIO25) {   //ms3 dat2
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 10), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 10), &obj->reg->msdc3_ctrl3.set);
	} else if (pin == GPIO26) {   //ms3 dat3
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 14), &obj->reg->msdc3_ctrl3.rst) : GPIO_SET_BITS((1L << 14), &obj->reg->msdc3_ctrl3.set);
	} else if (pin == GPIO27) {   //ms3 clk 
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc3_ctrl0.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc3_ctrl0.set);
	} else if (pin == GPIO28) {   //ms3 cmd
		(select == GPIO_PULL_UP)? GPIO_SET_BITS((1L << 2), &obj->reg->msdc3_ctrl1.rst) : GPIO_SET_BITS((1L << 2), &obj->reg->msdc3_ctrl1.set);
	}

	if (0){
		return GPIO_PULL_EN_UNSUPPORTED;
	}else{
		pos = pin / MAX_GPIO_REG_BITS;
		bit = pin % MAX_GPIO_REG_BITS;
		
		if (select == GPIO_PULL_DOWN)
			GPIO_SET_BITS((1L << bit), &obj->reg->pullsel[pos].rst);
		else
			GPIO_SET_BITS((1L << bit), &obj->reg->pullsel[pos].set);
	}
    return RSUCCESS;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_pull_select_chip(u32 pin)
{
    u32 pos;
    u32 bit;
    u32 reg;
	u32 i;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= GPIO197)
        return -ERINVAL;

	/*********************************for special kpad pupd*********************************/
	for(i = 0; i < sizeof(kpad_pupd_spec)/sizeof(kpad_pupd_spec[0]); i++){
		if (pin == kpad_pupd_spec[i].pin){
			reg = GPIO_RD32(&obj->reg->kpad_ctrl[kpad_pupd_spec[i].reg].val);
			return (((reg & (1L << kpad_pupd_spec[i].bit)) != 0)? 0: 1);
		}
	}
	/*********************************for special sim pupd*********************************/
	for(i = 0; i < sizeof(sim_pupd_spec)/sizeof(sim_pupd_spec[0]); i++){
		if (pin == sim_pupd_spec[i].pin){
			reg = GPIO_RD32(&obj->reg->sim_ctrl[sim_pupd_spec[i].reg].val);
			return (((reg & (1L << sim_pupd_spec[i].bit)) != 0)? 0: 1);
		}
	}
	/* ************************DPI special *************************/
	if (pin == GPIO138) {	        //dpi ck
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[0].val) & (1L << 2)) != 0)? 0: 1); 
	} else if (pin == GPIO139) {    //dpi DE
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 1)) != 0)? 0: 1); 
	} else if (pin == GPIO140) {	//dpi D0
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 1)) != 0)? 0: 1); 
	} else if (pin == GPIO141) {    //dpi D1
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 3)) != 0)? 0: 1); 
	} else if (pin == GPIO142) {    //dpi D2
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 5)) != 0)? 0: 1); 
	} else if (pin == GPIO143) {    //dpi D3
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[2].val) & (1L << 7)) != 0)? 0: 1); 
	} else if (pin == GPIO144) {    //dpi D4
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[0].val) & (1L << 5)) != 0)? 0: 1); 
	} else if (pin == GPIO145) {    //dpi D5
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[0].val) & (1L << 7)) != 0)? 0: 1); 
	} else if (pin == GPIO146) {    //dpi D6
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 7)) != 0)? 0: 1); 
	} else if (pin == GPIO147) {    //dpi D7
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 6)) != 0)? 0: 1); 
	} else if (pin == GPIO148) {    //dpi D8
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 8)) != 0)? 0: 1); 
	} else if (pin == GPIO149) {    //dpi D9
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 10)) != 0)? 0: 1); 
	} else if (pin == GPIO150) {    //dpi D10
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 12)) != 0)? 0: 1); 
	} else if (pin == GPIO151) {    //dpi D11
		return (((GPIO_RD32(&obj->reg->pullen[13].val) & (1L << 14)) != 0)? 0: 1); 
	} else if (pin == GPIO152) {    //dpi HSYNC
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 3)) != 0)? 0: 1); 
	} else if (pin == GPIO153) {    //dpi VSYNC
		return (((GPIO_RD32(&obj->reg->dpi_ctrl[1].val) & (1L << 5)) != 0)? 0: 1); 
	}
    /********************************* MSDC special pupd *********************************/
	if (pin == GPIO164) {         //ms0 DS
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl4.val) & (1L << 2)) != 0)? 0: 1); 
	} else if (pin == GPIO165) {  //ms0 RST
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl3.val) & (1L << 2)) != 0)? 0: 1);  
	} else if (pin == GPIO162) {  //ms0 cmd
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl1.val) & (1L << 2)) != 0)? 0: 1);  
	} else if (pin == GPIO163) {  //ms0 clk
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl0.val) & (1L << 2)) != 0)? 0: 1);
	} else if ((pin >= GPIO154) && (pin <= GPIO161)) {	  //ms0 data0~7
        return (((GPIO_RD32(&obj->reg->msdc0_ctrl2.val) & (1L << 2)) != 0)? 0: 1);

	} else if (pin == GPIO170) {  //ms1 cmd
		return (((GPIO_RD32(&obj->reg->msdc1_ctrl1.val) & (1L << 2)) == 0)? 0: 1);        
	} else if (pin == GPIO171) {  //ms1 dat0
		return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (1L << 2)) == 0)? 0: 1);        
	} else if (pin == GPIO172) {  //ms1 dat1
		return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (1L << 6)) == 0)? 0: 1);        
	} else if (pin == GPIO173) {  //ms1 dat2
		return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (1L << 10)) == 0)? 0: 1);        
	} else if (pin == GPIO174) {  //ms1 dat3
		return (((GPIO_RD32(&obj->reg->msdc1_ctrl3.val) & (1L << 14)) == 0)? 0: 1);        
	} else if (pin == GPIO175) {  //ms1 clk
		return (((GPIO_RD32(&obj->reg->msdc1_ctrl0.val) & (1L << 2)) == 0)? 0: 1);        

	} else if (pin == GPIO100) {  //ms2 dat0
		return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (1L << 2)) == 0)? 0: 1);        
	} else if (pin == GPIO101) {  //ms2 dat1
		return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (1L << 6)) == 0)? 0: 1);        
	} else if (pin == GPIO102) {  //ms2 dat2
		return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (1L << 10)) == 0)? 0: 1);        
	} else if (pin == GPIO103) {  //ms2 dat3
		return (((GPIO_RD32(&obj->reg->msdc2_ctrl3.val) & (1L << 14)) == 0)? 0: 1);        
	} else if (pin == GPIO104) {  //ms2 clk 
		return (((GPIO_RD32(&obj->reg->msdc2_ctrl0.val) & (1L << 2)) == 0)? 0: 1);        
	} else if (pin == GPIO105) {  //ms2 cmd
		return (((GPIO_RD32(&obj->reg->msdc2_ctrl1.val) & (1L << 2)) == 0)? 0: 1);        

	} else if (pin == GPIO23) {  //ms3 dat0
		return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (1L << 2)) == 0)? 0: 1);        
	} else if (pin == GPIO24) {  //ms3 dat1
		return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (1L << 6)) == 0)? 0: 1);        
	} else if (pin == GPIO25) {  //ms3 dat2
		return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (1L << 10)) == 0)? 0: 1);        
	} else if (pin == GPIO26) {  //ms3 dat3
		return (((GPIO_RD32(&obj->reg->msdc3_ctrl3.val) & (1L << 14)) == 0)? 0: 1);        
	} else if (pin == GPIO27) {  //ms3 clk 
		return (((GPIO_RD32(&obj->reg->msdc3_ctrl0.val) & (1L << 2)) == 0)? 0: 1);        
	} else if (pin == GPIO28) {  //ms3 cmd
		return (((GPIO_RD32(&obj->reg->msdc3_ctrl1.val) & (1L << 2)) == 0)? 0: 1);        
	}

	if (0){
		return GPIO_PULL_EN_UNSUPPORTED;
	}else{
		pos = pin / MAX_GPIO_REG_BITS;
		bit = pin % MAX_GPIO_REG_BITS;

		reg = GPIO_RD32(&obj->reg->pullsel[pos].val);
	}
    return (((reg & (1L << bit)) != 0)? 1: 0);        
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_out_chip(u32 pin, u32 output)
{
    u32 pos;
    u32 bit;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= GPIO197)
        return -ERINVAL;

    if (output >= GPIO_OUT_MAX)
        return -ERINVAL;
    
    pos = pin / MAX_GPIO_REG_BITS;
    bit = pin % MAX_GPIO_REG_BITS;
    
    if (output == GPIO_OUT_ZERO)
        GPIO_SET_BITS((1L << bit), &obj->reg->dout[pos].rst);
    else
        GPIO_SET_BITS((1L << bit), &obj->reg->dout[pos].set);
    return RSUCCESS;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_out_chip(u32 pin)
{
    u32 pos;
    u32 bit;
    u32 reg;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= GPIO197)
        return -ERINVAL;
    
    pos = pin / MAX_GPIO_REG_BITS;
    bit = pin % MAX_GPIO_REG_BITS;

    reg = GPIO_RD32(&obj->reg->dout[pos].val);
    return (((reg & (1L << bit)) != 0)? 1: 0);        
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_in_chip(u32 pin)
{
    u32 pos;
    u32 bit;
    u32 reg;
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= MAX_GPIO_PIN)
        return -ERINVAL;
    
    pos = pin / MAX_GPIO_REG_BITS;
    bit = pin % MAX_GPIO_REG_BITS;

    reg = GPIO_RD32(&obj->reg->din[pos].val);
    return (((reg & (1L << bit)) != 0)? 1: 0);        
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_mode_chip(u32 pin, u32 mode)
{
    u32 pos;
    u32 bit;
    u32 reg;
    u32 mask = (1L << GPIO_MODE_BITS) - 1;    
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= MAX_GPIO_PIN)
        return -ERINVAL;

    if (mode >= GPIO_MODE_MAX)
        return -ERINVAL;

	pos = pin / MAX_GPIO_MODE_PER_REG;
	bit = pin % MAX_GPIO_MODE_PER_REG;
   
	reg = GPIO_RD32(&obj->reg->mode[pos].val);

	reg &= ~(mask << (GPIO_MODE_BITS*bit));
	reg |= (mode << (GPIO_MODE_BITS*bit));
	
	GPIO_WR32(&obj->reg->mode[pos].val, reg);

    return RSUCCESS;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_mode_chip(u32 pin)
{
    u32 pos;
    u32 bit;
    u32 reg;
    u32 mask = (1L << GPIO_MODE_BITS) - 1;    
    struct mt_gpio_obj *obj = gpio_obj;

    if (!obj)
        return -ERACCESS;

    if (pin >= MAX_GPIO_PIN)
        return -ERINVAL;
    
	pos = pin / MAX_GPIO_MODE_PER_REG;
	bit = pin % MAX_GPIO_MODE_PER_REG;

	reg = GPIO_RD32(&obj->reg->mode[pos].val);
	
	return ((reg >> (GPIO_MODE_BITS*bit)) & mask);
}
void mt_gpio_pin_decrypt(unsigned long *cipher)
{
	//just for debug, find out who used pin number directly
	if((*cipher & (0x80000000)) == 0){
		GPIOERR("Pin %u decrypt warning! \n",*cipher);	
		//dump_stack();
		//return;
	}

	//GPIOERR("Pin magic number is %x\n",*cipher);
	*cipher &= ~(0x80000000);
	return;
}
//set GPIO function in fact
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_dir(u32 pin, u32 dir)
{
	mt_gpio_pin_decrypt(&pin);
    mt_set_gpio_dir_chip(pin,dir);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_dir(u32 pin)
{
	mt_gpio_pin_decrypt(&pin);
    mt_get_gpio_dir_chip(pin);
    return ERINVAL;    
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_pull_enable(u32 pin, u32 enable)
{
	mt_gpio_pin_decrypt(&pin);
    mt_set_gpio_pull_enable_chip(pin,enable);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_pull_enable(u32 pin)
{
	mt_gpio_pin_decrypt(&pin);
    mt_get_gpio_pull_enable_chip(pin);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_pull_select(u32 pin, u32 select)
{
	mt_gpio_pin_decrypt(&pin);
    mt_set_gpio_pull_select_chip(pin,select);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_pull_select(u32 pin)
{
	mt_gpio_pin_decrypt(&pin);
    mt_get_gpio_pull_select_chip(pin);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_out(u32 pin, u32 output)
{
	mt_gpio_pin_decrypt(&pin);
    mt_set_gpio_out_chip(pin,output);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_out(u32 pin)
{
	mt_gpio_pin_decrypt(&pin);
    mt_get_gpio_out_chip(pin);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_in(u32 pin)
{
	mt_gpio_pin_decrypt(&pin);
    mt_get_gpio_in_chip(pin);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_set_gpio_mode(u32 pin, u32 mode)
{
	mt_gpio_pin_decrypt(&pin);
    mt_set_gpio_mode_chip(pin,mode);
    return ERINVAL;
}
/*---------------------------------------------------------------------------*/
s32 mt_get_gpio_mode(u32 pin)
{
	mt_gpio_pin_decrypt(&pin);
    mt_get_gpio_mode_chip(pin);
    return ERINVAL;
}

void mt_gpio_init(void)
{
#ifdef DUMMY_AP
	mt_gpio_set_default();
#endif

#ifdef TINY
	mt_gpio_set_default();
#endif
}
