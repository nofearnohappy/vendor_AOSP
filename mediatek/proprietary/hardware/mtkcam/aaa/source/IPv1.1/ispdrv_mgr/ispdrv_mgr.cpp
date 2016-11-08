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
#define LOG_TAG "ispdrv_mgr"

#ifndef ENABLE_MY_LOG
    #define ENABLE_MY_LOG       (1)
#endif

#include <utils/threads.h>
#include <aaa_types.h>
#include <aaa_log.h>
#include <aaa_error_code.h>
#include <isp_reg.h>

#include "ispdrv_mgr.h"

using namespace android;
using namespace NS3Av3;


/*******************************************************************************
* ISP Driver Manager Context
*******************************************************************************/
class IspDrvMgrCtx : public IspDrvMgr
{
protected:  ////    Data Members.
    void*           m_pIspRegP1;
    void*           m_pIspRegP2;
    MINT32          m_Users;
    mutable android::Mutex m_Lock;

private:    ////    Ctor/Dtor
    IspDrvMgrCtx();
    ~IspDrvMgrCtx();

    cam_reg_t    m_rCamReg;
    dip_x_reg_t  m_rDipReg;

public:     ////    Interfaces.
    static IspDrvMgr&       getInstance();
    virtual void*           getIspReg(ISPDRV_MODE_T eIspMode) const;
    virtual MBOOL           readRegs(ISPDRV_MODE_T eIspMode, ISPREG_INFO_T*const pRegInfos, MUINT32 const count);

    virtual MBOOL           writeRegs(ISPDRV_MODE_T eIspMode, ISPREG_INFO_T*const pRegInfos, MUINT32 const count);
    virtual MERROR_ENUM_T   init();
    virtual MERROR_ENUM_T   uninit();
};


IspDrvMgr&
IspDrvMgr::
getInstance()
{
    return IspDrvMgrCtx::getInstance();
}

IspDrvMgr&
IspDrvMgrCtx::
getInstance()
{
    static IspDrvMgrCtx singleton;
    return singleton;
}

IspDrvMgrCtx::
IspDrvMgrCtx()
    : IspDrvMgr()
    , m_pIspRegP1((void*)&m_rCamReg)
    , m_pIspRegP2((void*)&m_rDipReg)
    , m_Users(0)
    , m_Lock()
{
}


IspDrvMgrCtx::
~IspDrvMgrCtx()
{
}


IspDrvMgr::MERROR_ENUM_T
IspDrvMgrCtx::
init()
{
	MY_LOG("[%s()] - E. m_Users: %d \n", __FUNCTION__, m_Users);

	Mutex::Autolock lock(m_Lock);

	if (m_Users > 0)
	{
		MY_LOG("%d has created \n", m_Users);
		android_atomic_inc(&m_Users);
		return IspDrvMgr::MERR_OK;
	}

	android_atomic_inc(&m_Users);

    return IspDrvMgr::MERR_OK;
}


IspDrvMgr::MERROR_ENUM_T
IspDrvMgrCtx::
uninit()
{
	MY_LOG("[%s()] - E. m_Users: %d \n", __FUNCTION__, m_Users);

	Mutex::Autolock lock(m_Lock);

	// If no more users, return directly and do nothing.
	if (m_Users <= 0)
	{
		return IspDrvMgr::MERR_OK;
	}

	// More than one user, so decrease one User.
	android_atomic_dec(&m_Users);

	if (m_Users == 0) // There is no more User after decrease one User
	{
    }
	else	// There are still some users.
	{
		MY_LOG("Still %d users \n", m_Users);
	}

    return IspDrvMgr::MERR_OK;
}

void* IspDrvMgrCtx::getIspReg(ISPDRV_MODE_T eIspDrvMode) const
{
    switch (eIspDrvMode)
    {
    case ISPDRV_MODE_ISP_CAMA:
    case ISPDRV_MODE_ISP_CAMB:
        return m_pIspRegP1;
    case ISPDRV_MODE_ISP_DIP:
        return m_pIspRegP2;
    default:
        MY_ERR("Unsupport ISP drive mode\n");
        return MNULL;
    }
}

MBOOL
IspDrvMgrCtx::
readRegs(ISPDRV_MODE_T eIspDrvMode, ISPREG_INFO_T*const pRegInfos, MUINT32 const count)
{
    return MFALSE;
#if 0
    switch (eIspDrvMode)
    {
    case ISPDRV_MODE_ISP_CAM:
        if  (! m_pIspDrv)
            return  MFALSE;
        return  (m_pIspDrv->readRegs(reinterpret_cast<ISP_DRV_REG_IO_STRUCT*>(pRegInfos), count) < 0) ? MFALSE : MTRUE;
    default:
        MY_ERR("Unsupport ISP drive mode\n");
        return MFALSE;
    }
#endif
}


MBOOL
IspDrvMgrCtx::
writeRegs(ISPDRV_MODE_T eIspDrvMode, ISPREG_INFO_T*const pRegInfos, MUINT32 const count)
{
    MBOOL fgRet = MTRUE;
#if 0
    switch (eIspDrvMode)
    {
    case ISPDRV_MODE_ISP:
        if  (! m_pIspDrv)
            return  MFALSE;
        fgRet = m_pIspDrv->writeRegs(reinterpret_cast<ISP_DRV_REG_IO_STRUCT*>(pRegInfos), count);
        break;
    default:
        MY_ERR("Unsupport ISP drive mode\n");
        return MFALSE;
    }
#endif
    return fgRet;
}

