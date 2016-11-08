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
//
//
//#include <mtkcam/featureio/IHal3A.h>
//using namespace NS3A;
//
#include <camshot/_callbacks.h>
#include <camshot/_params.h>
//
#include "../inc/CamShotImp.h"
#include "../inc/StereoShot.h"
//
#include <hwutils/CamManager.h>
using namespace NSCam::Utils;
//
using namespace NSCam;
#include <utils/include/Format.h>
using namespace NSCam::Utils::Format;
//
//#include <mtkcam/featureio/stereo_hal_base.h>
//
// buffer usage
#include <imageio/ispio_pipe_ports.h>
//
// for debug dump
#include <cutils/properties.h>
using namespace android;
#include <IHalSensor.h>


/*******************************************************************************
*
********************************************************************************/
namespace NSCamShot {
////////////////////////////////////////////////////////////////////////////////

/*******************************************************************************
*
********************************************************************************/
StereoShot::
StereoShot(
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
StereoShot::
init()
{
#if 0
    FUNC_START;
    MBOOL ret = MTRUE;
    String8 const s8MainIdKey("MTK_SENSOR_DEV_MAIN");
    String8 const s8Main2IdKey("MTK_SENSOR_DEV_MAIN_2");
    Utils::Property::tryGet(s8MainIdKey, mSensorId_Main);
    Utils::Property::tryGet(s8Main2IdKey, mSensorId_Main2);
    //
    mpStereoHal = StereoHalBase::createInstance();
    //
    if( !mpAllocBufHandler )
    {
        mpAllocBufHandler = AllocBufHandler::createInstance();
        ret = mpAllocBufHandler->init();
    }
    if ( !mpAllocBufHandler_Main2 )
    {
        mpAllocBufHandler_Main2 = AllocBufHandler::createInstance();
        ret = mpAllocBufHandler_Main2->init();
    }
    FUNC_END;
    //
    return ret && (sem_init(&mShotDone, 0, 0) == 0);
#endif
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
uninit()
{
    MBOOL ret = MTRUE;
#if 0
    FUNC_START;

    if( mpAllocBufHandler )
    {
        mpAllocBufHandler->uninit();
        mpAllocBufHandler->destroyInstance();
        mpAllocBufHandler = NULL;
    }
    if ( mpAllocBufHandler_Main2 )
    {
        mpAllocBufHandler_Main2->uninit();
        mpAllocBufHandler_Main2->destroyInstance();
        mpAllocBufHandler_Main2 = NULL;
    }
    if( mpPrvBufHandler )
    {
        // no need to destroy, since this is passed from adapter
        mpPrvBufHandler = NULL;
    }

    FUNC_END;
    //
    if( sem_destroy(&mShotDone) != 0 )
        ret = MFALSE;
#endif
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
start(SensorParam const & rSensorParam)
{
#if 0
    FUNC_START;
    mSensorParam = rSensorParam;
    //
    dumpSensorParam(mSensorParam);

    FUNC_END;
    //
#endif
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
startOne(SensorParam const & rSensorParam)
{
    FUNC_START;

    MBOOL ret = MTRUE;
#if 0
    MUINT32 RotPicWidth, RotPicHeight;
    MUINT32 RotThuWidth, RotThuHeight;
    MSize mainSize = mpStereoHal->getMainSize( MSize(mShotParam.u4PictureWidth, mShotParam.u4PictureHeight) );
    MY_LOGD("1 mainSize(%dx%d)", mainSize.w, mainSize.h);
    //
    mSensorParam = rSensorParam;
    //
    // dump infos
    dumpSensorParam(mSensorParam);
    //query flash on/off
    {
        IHal3A* pHal3A = IHal3A::createInstance( IHal3A::E_Camera_1, mSensorParam.u4OpenID, LOG_TAG);
        if( pHal3A == NULL ) {
            MY_LOGE("create 3A failed");
            return MFALSE;
        }

        MBOOL bFlashOn = pHal3A->isNeedFiringFlash();
        MBOOL isYuv =
            NSCam::IHalSensorList::get()->queryType( mSensorParam.u4OpenID ) == NSCam::NSSensorType::eYUV;
        pHal3A->destroyInstance(LOG_TAG);
        pHal3A = NULL;

        if( !isYuv                                          &&  // not yuv sensor
            (!CamManager::getInstance()->isMultiDevice())   &&  // only single device support pure raw flow
            (bFlashOn && mSensorParam.u4RawType != 3)       ||  // if flash on && not forced pre-processed
            mSensorParam.u4RawType == 0                         // if pure-raw
            )
        {
            mbPureRaw = MTRUE;
            MY_LOGD("flash %d, rawtype %d: use pureraw",
                    bFlashOn, mSensorParam.u4RawType);
        }

        {   //debug
            char value[PROPERTY_VALUE_MAX] = {'\0'};
            int val;
            property_get( "debug.sshot.pureraw", value, "-1");
            val = atoi(value);
            if( val != -1 ) {
                mbPureRaw = val;
                MY_LOGD("force pure-raw %d", mbPureRaw);
            }
        }

        // overwrite rawtype
        if( mbPureRaw && mSensorParam.u4RawType == 1) {
            mSensorParam.u4RawType = 0;
            MY_LOGD("update shot param: use pure raw");
        }
    }
    //
    updateProfile(SHOT_PROFILE_NORMAL);
    //
    getRotatedPicSize(&RotPicWidth, &RotPicHeight, &RotThuWidth, &RotThuHeight);
    //
    MY_LOGD("enable msg (notify, data) = (0x%x,0x%x)", mi4NotifyMsgSet, mi4DataMsgSet);
    if( !isDataMsgEnabled(ECamShot_DATA_MSG_ALL) && !isNotifyMsgEnabled(ECamShot_NOTIFY_MSG_ALL) )
    {
        MY_LOGE("no data/msg enabled");
        return MFALSE;
    }

    MUINT32 dataInBit = 0x0;
    MUINT32 CBDataSet = 0x0;
    MUINT32 nodeDataInBit = 0x0;
    updateNeededData(&dataInBit, &CBDataSet);
    updateNeededNodeData(dataInBit, &nodeDataInBit);

    if( !mpGraph )
    {
        mpGraph = ICamGraph::createInstance(mSensorParam.u4OpenID, "SShot");
    }

    // update registered buffers
    ret = ret && doRegisterBuffers();

    if( dataInBit & DATA_DUALITSBS )
    {
        ret = ret
            && prepareMemory(
                    DATA_DUALITSBS,
                    mJpegParam.u4JpsWidth, mJpegParam.u4JpsHeight,
                    eImgFmt_YUY2,
                    0,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_DUALITSBS )
    {
        ret = ret && enableData(DATA_DUALITSBS);
    }

    //assign buffer handler
    mpGraph->setBufferHandler(   PASS1_RESIZEDRAW   ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   PASS1_FULLRAW      ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   STEREO_IMG         ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   STEREO_RGB         ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   PASS1_RESIZEDRAW   ,   mpAllocBufHandler_Main2,    getOpenId_Main2());
    mpGraph->setBufferHandler(   PASS1_FULLRAW      ,   mpAllocBufHandler_Main2,    getOpenId_Main2());
    mpGraph->setBufferHandler(   STEREO_IMG         ,   mpAllocBufHandler_Main2,    getOpenId_Main2());
    mpGraph->setBufferHandler(   STEREO_RGB         ,   mpAllocBufHandler_Main2,    getOpenId_Main2());


    if( dataInBit & DATA_MAINYUV )
    {
        ret = ret && enableData(DATA_MAINYUV);
    }

    if( dataInBit & DATA_RESIZEDYUV )
    {
        ret = ret && enableData(DATA_RESIZEDYUV);
    }

    if( dataInBit & DATA_ITMAINYUV )
    {
        ret = ret && enableData(DATA_ITMAINYUV);
    }

    if( dataInBit & DATA_DUALIT_M )
    {
        ret = ret && enableData(DATA_DUALIT_M);
    }

    if( dataInBit & DATA_MAINJPEG )
    {
        ret = ret && enableData(DATA_MAINJPEG);
    }

    if( dataInBit & DATA_MAINJPS )
    {
        ret = ret && enableData(DATA_MAINJPS);
    }

    if( dataInBit & DATA_THUMBJPEG )
    {
        ret = ret && enableData(DATA_THUMBJPEG);
    }

    ret = ret && createNodes(nodeDataInBit, CBDataSet, NULL, NULL);

    ret = ret && connectNodes(nodeDataInBit);

    ret = ret && mpGraph->init();

    ret = ret && mpGraph->start();

    if( dataInBit & DATA_MAINYUV )
    {
        ret = ret
            && prepareMemory(
                    DATA_MAINYUV,
                    mainSize.w, mainSize.h,
                    eImgFmt_YUY2,
                    0,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_ITMAINYUV )
    {
        ret = ret
            && prepareMemory(
                    DATA_ITMAINYUV,
                    RotPicWidth, RotPicHeight,
                    eImgFmt_YUY2,
                    mShotParam.u4PictureTransform,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_DUALIT_M )
    {
        ret = ret
            && prepareMemory(
                    DATA_DUALIT_M,
                    RotThuWidth, RotThuHeight,
                    eImgFmt_YUY2,
                    mShotParam.u4PictureTransform,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_MAINJPEG )
    {
        ret = ret
            && prepareMemory(
                    DATA_MAINJPEG,
                    RotPicWidth, RotPicHeight,
                    eImgFmt_JPEG,
                    0);
    }

    if( dataInBit & DATA_MAINJPS )
    {
        ret = ret
            && prepareMemory(
                    DATA_MAINJPS,
                    mJpegParam.u4JpsWidth, mJpegParam.u4JpsHeight,
                    eImgFmt_JPEG,
                    0);
    }

    if( dataInBit & DATA_THUMBJPEG )
    {
        ret = ret
            && prepareMemory(
                DATA_THUMBJPEG,
                RotThuWidth, RotThuHeight,
                eImgFmt_JPEG,
                0);
    }

    if( ret )
    {
        MINT32 sem_ret;
        while( 0 !=( sem_ret = sem_wait( &mShotDone ) ) && errno == EINTR );
        if( sem_ret < 0 )
        {
            MY_LOGE("errno = %d", errno);
            ret = MFALSE;
        }
        MY_LOGD("shot finished");
    }

    mpGraph->stop();

    mpGraph->uninit();

    mpGraph->disconnect();

    destroyNodes();

    mpGraph->destroyInstance();
    mpGraph = NULL;
    //
    reset();
    //
    FUNC_END;
    //
#endif
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
startOne(
    SensorParam const & rSensorParam,
    IImageBuffer const *pImgBuf_0,
    IImageBuffer const *pImgBuf_1,
    IImageBuffer const *pBufPrv_0,
    IImageBuffer const *pBufPrv_1
)
{
    FUNC_START;

    MBOOL ret = MTRUE;
#if 0
    MUINT32 RotPicWidth, RotPicHeight;
    MUINT32 RotThuWidth, RotThuHeight;
    MSize mainSize = mpStereoHal->getMainSize( MSize(mShotParam.u4PictureWidth, mShotParam.u4PictureHeight) );
    MY_LOGD("2 mainSize(%dx%d)", mainSize.w, mainSize.h);
    //
    updateProfile(SHOT_PROFILE_ZSD);
    mSensorParam = rSensorParam;
    //
    // dump infos
    MY_LOGD("src buf 0x%x, 0x%x, 0x%x, 0x%x", pImgBuf_0, pImgBuf_1, pBufPrv_0, pBufPrv_1);
    dumpSensorParam(mSensorParam);
    //
    getRotatedPicSize(&RotPicWidth, &RotPicHeight, &RotThuWidth, &RotThuHeight);
    //
    MY_LOGD("enable msg (notify, data) = (0x%x,0x%x)", mi4NotifyMsgSet, mi4DataMsgSet);
    if( !isDataMsgEnabled(ECamShot_DATA_MSG_ALL) && !isNotifyMsgEnabled(ECamShot_NOTIFY_MSG_ALL) )
    {
        MY_LOGE("no data/msg enabled");
        return MFALSE;
    }

    {
        Mutex::Autolock _l(mLock);
        mFinishedData = 0x0;
    }

    MUINT32 dataInBit = 0x0;
    MUINT32 CBDataSet = 0x0;
    MUINT32 nodeDataInBit = 0x0;
    updateNeededData(&dataInBit, &CBDataSet);
    updateNeededNodeData(dataInBit, &nodeDataInBit);

    if( !mpGraph )
    {
        mpGraph = ICamGraph::createInstance(mSensorParam.u4OpenID, "SShot");
    }

    // update registered buffers
    ret = ret && doRegisterBuffers();

    if( dataInBit & DATA_DUALITSBS )
    {
        ret = ret
            && prepareMemory(
                    DATA_DUALITSBS,
                    mJpegParam.u4JpsWidth, mJpegParam.u4JpsHeight,
                    eImgFmt_YUY2,
                    0,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_DUALITSBS )
    {
        ret = ret && enableData(DATA_DUALITSBS);
    }

    //assign buffer handler
    mpGraph->setBufferHandler(   PASS1_RESIZEDRAW   ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   STEREO_IMG         ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   STEREO_RGB         ,   mpAllocBufHandler,          getOpenId_Main());
    mpGraph->setBufferHandler(   PASS1_RESIZEDRAW   ,   mpAllocBufHandler_Main2,    getOpenId_Main2());
    mpGraph->setBufferHandler(   STEREO_IMG         ,   mpAllocBufHandler_Main2,    getOpenId_Main2());
    mpGraph->setBufferHandler(   STEREO_RGB         ,   mpAllocBufHandler_Main2,    getOpenId_Main2());

    if( dataInBit & DATA_MAINYUV )
    {
        ret = ret && enableData(DATA_MAINYUV);
    }

    if( dataInBit & DATA_RESIZEDYUV )
    {
        ret = ret && enableData(DATA_RESIZEDYUV);
    }

    if( dataInBit & DATA_ITMAINYUV )
    {
        ret = ret && enableData(DATA_ITMAINYUV);
    }

    if( dataInBit & DATA_DUALIT_M )
    {
        ret = ret && enableData(DATA_DUALIT_M);
    }

    if( dataInBit & DATA_DUALITSBS )
    {
        ret = ret && enableData(DATA_DUALITSBS);
    }

    if( dataInBit & DATA_MAINJPEG )
    {
        ret = ret && enableData(DATA_MAINJPEG);
    }

    if( dataInBit & DATA_MAINJPS )
    {
        ret = ret && enableData(DATA_MAINJPS);
    }

    if( dataInBit & DATA_THUMBJPEG )
    {
        ret = ret && enableData(DATA_THUMBJPEG);
    }

    ret = ret && createNodes(nodeDataInBit, CBDataSet, pImgBuf_0, pImgBuf_1);

    ret = ret && connectNodes(nodeDataInBit);

    ret = ret && mpGraph->init();

    ret = ret && mpGraph->start();

    // prepare pass2/jpeg buffers
    if( dataInBit & DATA_MAINYUV )
    {
        ret = ret
            && prepareMemory(
                    DATA_MAINYUV,
                    mainSize.w, mainSize.h,
                    eImgFmt_YUY2,
                    0,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_ITMAINYUV )
    {
        ret = ret
            && prepareMemory(
                    DATA_ITMAINYUV,
                    RotPicWidth, RotPicHeight,
                    eImgFmt_YUY2,
                    mShotParam.u4PictureTransform,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_DUALIT_M )
    {
        ret = ret
            && prepareMemory(
                    DATA_DUALIT_M,
                    RotThuWidth, RotThuHeight,
                    eImgFmt_YUY2,
                    mShotParam.u4PictureTransform,
                    NSImageio::NSIspio::EPortCapbility_Cap);
    }

    if( dataInBit & DATA_MAINJPEG )
    {
        ret = ret
            && prepareMemory(
                    DATA_MAINJPEG,
                    RotPicWidth, RotPicHeight,
                    eImgFmt_JPEG,
                    0);
    }

    if( dataInBit & DATA_MAINJPS )
    {
        ret = ret
            && prepareMemory(
                    DATA_MAINJPS,
                    mJpegParam.u4JpsWidth, mJpegParam.u4JpsHeight,
                    eImgFmt_JPEG,
                    0);
    }

    if( dataInBit & DATA_THUMBJPEG )
    {
        ret = ret
            && prepareMemory(
                DATA_THUMBJPEG,
                RotThuWidth, RotThuHeight,
                eImgFmt_JPEG,
                mShotParam.u4PictureTransform);
    }

    if( ret )
    {
        MINT32 sem_ret;
        while( 0 !=( sem_ret = sem_wait( &mShotDone ) ) && errno == EINTR );
        if( sem_ret < 0 )
        {
            MY_LOGE("errno = %d", errno);
            ret = MFALSE;
        }
        MY_LOGD("shot finished");
    }

    mpGraph->stop();

    mpGraph->uninit();

    mpGraph->disconnect();

    destroyNodes();

    mpGraph->destroyInstance();
    mpGraph = NULL;
    //
    FUNC_END;
    //
#endif
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
startAsync(SensorParam const & rSensorParam)
{
    FUNC_START;
    //
    FUNC_END;
    //
    return MTRUE;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
stop()
{
    FUNC_START;

    FUNC_END;
    //
    return MTRUE;
}



/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
setShotParam(ShotParam const & rParam)
{
#if 0
    FUNC_START;
    mShotParam = rParam;
    //
    dumpShotParam(mShotParam);

    FUNC_END;
    //
#endif
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
setJpegParam(JpegParam const & rParam)
{
#if 0
    FUNC_START;
    mJpegParam = rParam;
    //
    dumpJpegParam(mJpegParam);

    FUNC_END;
    //
#endif
    return MTRUE;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
registerImageBuffer(ECamShotImgBufType const eBufType, IImageBuffer const *pImgBuffer)
{
    MBOOL ret = MTRUE;
#if 0
    MUINT32 internaldata;

    MY_LOGD("buf type 0x%x, buf 0x%x", eBufType, pImgBuffer);
    switch( eBufType )
    {
        case ECamShot_BUF_TYPE_RAW:
            ret = MFALSE;
            break;
        case ECamShot_BUF_TYPE_YUV:
            internaldata = DATA_MAINYUV;
            break;
        case ECamShot_BUF_TYPE_JPEG:
            internaldata = DATA_MAINJPEG;
            break;
        default:
            ret = MFALSE;
            break;
    }

    if( ret ) {
        muRegisteredBufType |= internaldata;
        regbuf_t buf = { internaldata, pImgBuffer };
        mvRegBuf.push_back(buf);
    } else {
        MY_LOGE("not support buf type(0x%x), buf 0x%x", eBufType, pImgBuffer);
    }
    //
#endif
    return ret;
}


/*******************************************************************************
*
********************************************************************************/
MBOOL
StereoShot::
sendCommand(MINT32 cmd, MINT32 arg1, MINT32 arg2, MINT32 arg3)
{
#if 0
    //FUNC_START;
    switch( cmd )
    {
        case ECamShot_CMD_SET_CAPTURE_STYLE:
            muCapStyle = arg1;
            MY_LOGD("capture style 0x%x", muCapStyle);
            break;
        case ECamShot_CMD_SET_NRTYPE:
            muNRType = arg1;
            MY_LOGD("NR type 0x%x", muNRType);
            break;
        default:
            MY_LOGW("unsupport cmd 0x%x", cmd);
            break;
    }
    //FUNC_END;
    //
#endif
    return MTRUE;
}

////////////////////////////////////////////////////////////////////////////////
};  //namespace NSCamShot

