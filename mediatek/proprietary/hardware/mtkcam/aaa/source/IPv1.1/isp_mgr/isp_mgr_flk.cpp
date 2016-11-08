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
#define LOG_TAG "isp_mgr_flk"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (0)
#endif

#include <tuning_mgr.h>

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
#include <isp_reg.h>

namespace NSIspTuningv3
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//Flicker
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
ISP_MGR_FLK_CONFIG_T&
ISP_MGR_FLK_CONFIG_T::
getInstance(ESensorDev_T const eSensorDev)
{
    switch (eSensorDev)
{
    case ESensorDev_Main: //  Main Sensor
        return  ISP_MGR_FLK_DEV<ESensorDev_Main>::getInstance();
    case ESensorDev_MainSecond: //  Main Second Sensor
        return  ISP_MGR_FLK_DEV<ESensorDev_MainSecond>::getInstance();
    case ESensorDev_Sub: //  Sub Sensor
        return  ISP_MGR_FLK_DEV<ESensorDev_Sub>::getInstance();
    default:
        MY_ERR("eSensorDev = %d", eSensorDev);
        return  ISP_MGR_FLK_DEV<ESensorDev_Main>::getInstance();
    }
}

#if 0
MBOOL
ISP_MGR_FLK_CONFIG_T::
apply()
{

    ISPDRV_MODE_T drv_mode;
    IspDrv* m_pIspDrv = IspDrv::createInstance();

	drv_mode=ISPDRV_MODE_CQ0;

        m_pIspDrv->cqDelModule(ISP_DRV_CQ0, CAM_DMA_ESFKO);
        m_pIspDrv->cqDelModule(ISP_DRV_CQ0, CAM_DMA_FLKI);
        m_pIspDrv->cqDelModule(ISP_DRV_CQ0, CAM_ISP_FLK);
        m_pIspDrv->cqDelModule(ISP_DRV_CQ0, CAM_TOP_CTL_01);

#if 1
		ISP_BITS(getIspReg(drv_mode),CAM_FLK_CON,FLK_MODE)=reinterpret_cast<ISP_CAM_FLK_CON*>(REG_INFO_VALUE_PTR(CAM_FLK_CON))->FLK_MODE;
		ISP_BITS(getIspReg(drv_mode), CAM_CTL_EN1, FLK_EN) =reinterpret_cast<ISP_CAM_FLK_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_EN1))->FLK_EN;
		ISP_BITS(getIspReg(drv_mode),CAM_CTL_EN1_SET,FLK_EN_SET)=reinterpret_cast<ISP_CAM_CTL_EN1_SET*>(REG_INFO_VALUE_PTR(CAM_CTL_EN1_SET))->FLK_EN_SET;
		ISP_BITS(getIspReg(drv_mode),CAM_CTL_DMA_EN,ESFKO_EN)=reinterpret_cast<ISP_CAM_CTL_DMA_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_EN))->ESFKO_EN;
		ISP_BITS(getIspReg(drv_mode),CAM_CTL_DMA_EN_SET,ESFKO_EN_SET)=reinterpret_cast<ISP_CAM_CTL_DMA_EN_SET*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_EN_SET))->ESFKO_EN_SET;
		ISP_BITS(getIspReg(drv_mode),CAM_CTL_DMA_INT,ESFKO_DONE_EN)=reinterpret_cast<ISP_CAM_CTL_DMA_INT*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_INT))->ESFKO_DONE_EN ;
		ISP_BITS(getIspReg(drv_mode),CAM_CTL_INT_EN,FLK_DON_EN)=reinterpret_cast<ISP_CAM_CTL_INT_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_INT_EN))->FLK_DON_EN ;
#endif

	m_pIspDrv->cqAddModule(ISP_DRV_CQ0, CAM_DMA_ESFKO);
	m_pIspDrv->cqAddModule(ISP_DRV_CQ0, CAM_DMA_FLKI);
	m_pIspDrv->cqAddModule(ISP_DRV_CQ0, CAM_ISP_FLK);
	m_pIspDrv->cqAddModule(ISP_DRV_CQ0, CAM_TOP_CTL_01);

    //FLICKER_LOG("[apply 22]: ,FLK_MODE:%d ,FLK_EN:%d ,FLK_EN_SET:%d,ESFKO_DMA_EN:%d\n",  ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_FLK_CON,FLK_MODE),ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_CTL_EN1,FLK_EN),ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_CTL_EN1_SET,FLK_EN_SET),ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_CTL_DMA_EN,ESFKO_EN));
	//MY_LOG_IF(ENABLE_MY_LOG,"[apply 23]: ,FLK_MODE:%d ,FLK_EN:%d ,FLK_EN_SET:%d,ESFKO_DMA_EN:%d\n",  ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_FLK_CON,FLK_MODE), (int) ISP_REG(getIspReg(ISPDRV_MODE_ISP), CAM_FLK_CON),ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_CTL_EN1,FLK_EN),ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_CTL_EN1_SET,FLK_EN_SET),ISP_BITS(getIspReg(ISPDRV_MODE_ISP),CAM_CTL_DMA_EN,ESFKO_EN));

    return  MTRUE;//writeRegs(CAM_ISP_FLK, ISPDRV_MODE_CQ0, static_cast<RegInfo_T*>(m_pRegInfo), m_u4RegInfoNum);
}
#endif


MVOID
ISP_MGR_FLK_CONFIG_T::
enableFlk(MBOOL enable)
{
#if 0
		reinterpret_cast<ISP_CAM_FLK_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_EN1))->FLK_EN = enable;
		reinterpret_cast<ISP_CAM_CTL_EN1_SET*>(REG_INFO_VALUE_PTR(CAM_CTL_EN1_SET))->FLK_EN_SET = enable;
		reinterpret_cast<ISP_CAM_CTL_DMA_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_EN))->ESFKO_EN = enable;
		reinterpret_cast<ISP_CAM_CTL_DMA_EN_SET*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_EN_SET))->ESFKO_EN_SET = enable;
		reinterpret_cast<ISP_CAM_FLK_CON*>(REG_INFO_VALUE_PTR(CAM_FLK_CON))->FLK_MODE = 0;
		reinterpret_cast<ISP_CAM_CTL_DMA_INT*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_INT))->ESFKO_DONE_EN = enable;
		reinterpret_cast<ISP_CAM_CTL_INT_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_INT_EN))->FLK_DON_EN = enable;
		//reinterpret_cast<ISP_CAM_LSC_EN*>(REG_INFO_VALUE_PTR(CAM_CTL_DMA_INT))->ESFKO_DONE_EN = enable;
		apply();
#endif
}
MVOID
ISP_MGR_FLK_CONFIG_T::
SetFLKWin(MINT32 offsetX, MINT32 offsetY, MINT32 sizeX, MINT32 sizeY)
{
    m_i4OfstX = offsetX;
    m_i4OfstY = offsetY;
    m_i4SizeX = sizeX;
    m_i4SizeY = sizeY;
}
MVOID
ISP_MGR_FLK_CONFIG_T::
SetFKO_DMA_Addr(MINT32 address, MINT32 size)
{
}

MBOOL
ISP_MGR_FLK_CONFIG_T::
apply(TuningMgr& rTuning)
{
#define FLICKER_MAX_LENG 4096

	int imgW, imgH;
	int u4ToleranceLine=20;
	int FLK_DMA_Size;

	CAM_UNI_REG_FLK_A_OFST reg_ofst;
	CAM_UNI_REG_FLK_A_SIZE reg_size;
	CAM_UNI_REG_FLK_A_NUM reg_num;

	imgW = m_i4SizeX-3;
	imgH = m_i4SizeY-25;

	if(imgH > FLICKER_MAX_LENG-6)
		imgH = FLICKER_MAX_LENG-6;

	reg_ofst.Bits.FLK_OFST_X = 0;
	reg_ofst.Bits.FLK_OFST_Y = 0+u4ToleranceLine;
	reg_size.Bits.FLK_SIZE_X = ((imgW-reg_ofst.Bits.FLK_OFST_X)/6)*2;
	reg_size.Bits.FLK_SIZE_Y = ((imgH-reg_ofst.Bits.FLK_OFST_Y+u4ToleranceLine)/6)*2;
	reg_num.Bits.FLK_NUM_X = 3;
	reg_num.Bits.FLK_NUM_Y = 3;

	FLK_DMA_Size=(reg_num.Bits.FLK_NUM_X*reg_num.Bits.FLK_NUM_Y*reg_size.Bits.FLK_SIZE_Y*2)-1;

	rTuning.updateEngine(eTuningMgrFunc_FLK, MTRUE, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_FLK_A_OFST, (MUINT32)reg_ofst.Raw, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_FLK_A_SIZE, (MUINT32)reg_size.Raw, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_FLK_A_NUM, (MUINT32)reg_num.Raw, 0);

	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_FLKO_XSIZE, FLK_DMA_Size, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_FLKO_YSIZE, 0, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_FLKO_STRIDE, FLK_DMA_Size, 0);

	rTuning.updateEngine(eTuningMgrFunc_SGG3, MTRUE, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_SGG3_A_PGN, 0x200, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_SGG3_A_GMRC_1, 0x10080402, 0);
	TUNING_MGR_WRITE_REG_UNI(&rTuning, CAM_UNI_SGG3_A_GMRC_2, 0x804020, 0);
	return MTRUE;
}


}
