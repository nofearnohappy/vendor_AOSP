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
#define LOG_TAG "MtkCam/SShot"
//
#include <Log.h>
#if defined(__func__)
#undef __func__
#endif
#define __func__ __FUNCTION__

#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s] " fmt, __func__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s] " fmt, __func__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s] " fmt, __func__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s] " fmt, __func__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s] " fmt, __func__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s] " fmt, __func__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s] " fmt, __func__, ##arg)
#define FUNC_START                  MY_LOGD("+")
#define FUNC_END                    MY_LOGD("-")
//
//
#include <common.h>
using namespace android;
//
//#include <mtkcam/featureio/IHal3A.h>
//using namespace NS3A;
//
#include <camshot/_callbacks.h>
#include <camshot/_params.h>
//
#include <metadata/IMetadata.h>
using namespace NSCam;

#include <v1/Processor/ResultProcessor.h>
using namespace NSCam::v1;

#include "../inc/CamShotImp.h"
#include "../inc/SingleShot.h"
//
// for debug dump
#include <cutils/properties.h>
#include <IHalSensor.h>

#define CHECK_OBJECT(x)  { if (x == NULL) { MY_LOGE("Null %s Object", #x); return MFALSE;}}

/*******************************************************************************
*
********************************************************************************/
namespace NSCamShot {
////////////////////////////////////////////////////////////////////////////////
/*******************************************************************************
*
********************************************************************************/
SingleShot::
SingleShot(
    EShotMode const eShotMode,
    char const*const szCamShotName
)
    : CamShotImp(eShotMode, szCamShotName)
{
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
init()
{
    FUNC_START;
    MBOOL ret = MTRUE;
    FUNC_END;
    //
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
uninit()
{
    MBOOL ret = MTRUE;
    FUNC_START;

    FUNC_END;
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
start(SensorParam const & rSensorParam)
{
    FUNC_START;
    mSensorParam = rSensorParam;
    //
    dumpSensorParam(mSensorParam);
//#warning [TODO] for continouous shot

    FUNC_END;
    //
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
startOne(SensorParam const & rSensorParam)
{
    FUNC_START;
    //
    MBOOL ret = MTRUE;
    FUNC_END;
    //
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
startOne(SensorParam const & rSensorParam, IImageBuffer const *pImgBuffer)
{
    FUNC_START;
    MBOOL ret = MFALSE;
    //
    MY_LOGE("not supported yet");
    //
    FUNC_END;
    //
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
startAsync(SensorParam const & rSensorParam)
{
    FUNC_START;
    //
    MY_LOGE("not supported yet");
    //
    FUNC_END;
    //
    return MFALSE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
stop()
{
    FUNC_START;
//#warning [TODO] for continouous shot

    FUNC_END;
    //
    return MTRUE;
}



/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
setShotParam(ShotParam const & rParam)
{
    FUNC_START;
    mShotParam = rParam;
    //
    dumpShotParam(mShotParam);

    FUNC_END;
    //
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
setJpegParam(JpegParam const & rParam)
{
    FUNC_START;
    mJpegParam = rParam;
    //
    dumpJpegParam(mJpegParam);

    FUNC_END;
    //
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
registerImageBuffer(ECamShotImgBufType const eBufType, IImageBuffer const *pImgBuffer)
{
    MBOOL ret = MTRUE;
    //
    //TODO
    MY_LOGE("not supported yet");
    return MFALSE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
SingleShot::
sendCommand(MINT32 cmd, MINT32 arg1, MINT32 arg2, MINT32 arg3, MVOID* arg4)
{
    //FUNC_START;
    switch( cmd )
    {
#if 0
        case ECamShot_CMD_SET_CAPTURE_STYLE:
            muCapStyle = arg1;
            MY_LOGD("capture style 0x%x", muCapStyle);
            break;
        case ECamShot_CMD_SET_NRTYPE:
            muNRType = arg1;
            MY_LOGD("NR type 0x%x", muNRType);
            break;
        case ECamShot_CMD_SET_VHDR_PROFILE:
            mbVHdr = MTRUE; //enable vhdr mode
            mVhdrProfile = (EIspProfile_T)arg1;
            MY_LOGD("set vhdr profile 0x%x", arg1);
            break;
        case ECamShot_CMD_SET_RWB_PROC_TYPE:
            mbEnGPU = arg1; //enable GPU process
            MY_LOGD("GPU process 0x%x", mbEnGPU);
            break;
#endif
        default:
            MY_LOGE("not supported cmd 0x%x", cmd);
            return MFALSE;
    }
    //FUNC_END;
    //
    return MTRUE;
}


////////////////////////////////////////////////////////////////////////////////
};  //namespace NSCamShot

