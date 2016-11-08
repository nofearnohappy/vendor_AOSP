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

#define LOG_TAG "MtkCam/DefaultAdapter"
//
#include "MyUtils.h"
//
#include <inc/CamUtils.h>
#include <cutils/properties.h>  // For property_get().

using namespace android;
using namespace MtkCamUtils;
//
#include <camera/MtkCamera.h>
//
#include <inc/ImgBufProvidersManager.h>
//
#include <v1/IParamsManager.h>
#include <v1/ICamAdapter.h>
#include <inc/BaseCamAdapter.h>
//
#include "inc/v3/DefaultAdapter.h"
using namespace NSDefaultAdapter;
//
#include <IHalSensor.h>
using namespace NSCam;
//

/******************************************************************************
*
*******************************************************************************/
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("(%d)(%d)(%s)[%s] " fmt, ::gettid(), getOpenId(), getName(), __FUNCTION__, ##arg)
//
#define MY_LOGV_IF(cond, ...)       do { if ( (cond) ) { MY_LOGV(__VA_ARGS__); } }while(0)
#define MY_LOGD_IF(cond, ...)       do { if ( (cond) ) { MY_LOGD(__VA_ARGS__); } }while(0)
#define MY_LOGI_IF(cond, ...)       do { if ( (cond) ) { MY_LOGI(__VA_ARGS__); } }while(0)
#define MY_LOGW_IF(cond, ...)       do { if ( (cond) ) { MY_LOGW(__VA_ARGS__); } }while(0)
#define MY_LOGE_IF(cond, ...)       do { if ( (cond) ) { MY_LOGE(__VA_ARGS__); } }while(0)
#define MY_LOGA_IF(cond, ...)       do { if ( (cond) ) { MY_LOGA(__VA_ARGS__); } }while(0)
#define MY_LOGF_IF(cond, ...)       do { if ( (cond) ) { MY_LOGF(__VA_ARGS__); } }while(0)

#define MY_LOGD1(...)               MY_LOGD_IF((mLogLevel>=1),__VA_ARGS__)
#define MY_LOGD2(...)               MY_LOGD_IF((mLogLevel>=2),__VA_ARGS__)
#define MY_LOGD3(...)               MY_LOGD_IF((mLogLevel>=3),__VA_ARGS__)

//
#define FUNC_START                  MY_LOGD2("+")
#define FUNC_END                    MY_LOGD2("-")


/******************************************************************************
*
*******************************************************************************/
sp<ICamAdapter>
createDefaultAdapter(
    String8 const&      rName,
    int32_t const       i4OpenId,
    sp<IParamsManager>  pParamsMgr
)
{
    CAM_LOGD("createMtkEngCamAdapter");
    return new CamAdapter(
        rName,
        i4OpenId,
        pParamsMgr
    );
}


/******************************************************************************
*
*******************************************************************************/
CamAdapter::
CamAdapter(
    String8 const&      rName,
    int32_t const       i4OpenId,
    sp<IParamsManager>  pParamsMgr
)
    : BaseCamAdapter(rName, i4OpenId, pParamsMgr)
    //
    , mpStateManager(NULL)
    //
    , mpCaptureCmdQueThread(0)
    , mpZipCallbackThread(0)
    //
    , mpShot(0)
    //
    , mUserName("None")
    //
    , mbTakePicPrvNotStop(false)
    , mbFixFps(false)
    , mPreviewMaxFps(0)
    , mShotMode(0)
    //
    , mLastVdoWidth(0)
    , mLastVdoHeight(0)
    //
    , mEngParam()
    , mLogLevel(0)
    //, mpPipeShot(0)
    , mpFlowControl(NULL)
{
    char cProperty[PROPERTY_VALUE_MAX] = {'\0'};
    ::property_get("debug.camera.adapter.loglevel", cProperty, "2");
    mLogLevel = ::atoi(cProperty);

    MY_LOGD1(
        "sizeof=%d, this=%p, mpStateManager=%p",
        sizeof(CamAdapter),
        this,&mpStateManager
    );


    mpParamsManagerV3 = IParamsManagerV3::createInstance(rName, i4OpenId, pParamsMgr);

    MY_LOGD( "mpImgBufProvidersMgr->getDisplayPvdr() %p", mpImgBufProvidersMgr->getDisplayPvdr().get() );
    mpFlowControl = IFlowControl::createInstance("EngCamAdapter", getOpenId(), 1, pParamsMgr, mpImgBufProvidersMgr);
}


/******************************************************************************
*
*******************************************************************************/
CamAdapter::
~CamAdapter()
{
    MY_LOGD1("tid(%d), OpenId(%d)", ::gettid(), getOpenId());
}


/******************************************************************************
*
*******************************************************************************/
bool
CamAdapter::
init()
{
    FUNC_START;
    bool ret = false;
    status_t status = OK;
    //
    mpStateManager = IStateManager::createInstance();
    if ( mpStateManager != NULL )
    {
        if(!(mpStateManager->init()))
        {
            MY_LOGE("mpStateManager->init fail");
            goto lbExit;
        }
    }
    //
    mpCaptureCmdQueThread = ICaptureCmdQueThread::createInstance(this);
    if  ( mpCaptureCmdQueThread == 0 || OK != (status = mpCaptureCmdQueThread->run() ) )
    {
        MY_LOGE(
            "Fail to run CaptureCmdQueThread - mpCaptureCmdQueThread.get(%p), status[%s(%d)]",
            mpCaptureCmdQueThread.get(), ::strerror(-status), -status
        );
        goto lbExit;
    }
    //

    if (mpFlowControl == NULL)
    {
        MY_LOGE(" mpFlowControl is NULL");
        ret = false;
        goto lbExit;
    }

    ret = true;
lbExit:
    if(!ret)
    {
        MY_LOGE("init() fail; now call uninit()");
        uninit();
    }
    FUNC_END;
    return  ret;
}


/******************************************************************************
*
*******************************************************************************/
bool
CamAdapter::
uninit()
{
    FUNC_START;
    bool ret = true;
    //  Close Command Queue Thread of Capture.
    sp<ICaptureCmdQueThread> pCaptureCmdQueThread = mpCaptureCmdQueThread;
    mpCaptureCmdQueThread = 0;
    if  ( pCaptureCmdQueThread != 0 ) {
        pCaptureCmdQueThread->requestExit();
        pCaptureCmdQueThread->join();
        pCaptureCmdQueThread = 0;
    }
    //
    if( !waitForShotDone() )
    {
        MY_LOGE("wait for shot done failed");
    }
    //
    if( previewEnabled())
    {
        MY_LOGD("Force to stop preview start (%d)",mbTakePicPrvNotStop);
        stopPreview();
        MY_LOGD1("Force to stop preview done");
    }
    //
    if(mpStateManager != NULL)
    {
        mpStateManager->uninit();
        mpStateManager->destroyInstance();
        mpStateManager = NULL;
    }
    //
    mpFlowControl = NULL;
    //
    BaseCamAdapter::uninit();
    //
    FUNC_END;
    return  ret;
}


/******************************************************************************
*
*******************************************************************************/
status_t
CamAdapter::
sendCommand(int32_t cmd, int32_t arg1, int32_t arg2)
{
    FUNC_START;
    MY_LOGD1("cmd(0x%08X),arg1(0x%08X),arg2(0x%08X)",cmd,arg1,arg2);

    //
    switch(cmd)
    {
#warning [TODO] only paser known command
        default:
            return mpFlowControl->sendCommand(cmd, arg1, arg2);
    }

    return  BaseCamAdapter::sendCommand(cmd, arg1, arg2);
}

