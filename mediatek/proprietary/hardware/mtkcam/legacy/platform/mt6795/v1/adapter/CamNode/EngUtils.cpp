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
#define LOG_TAG "MtkCam/RawDump"
//
#include <mtkcam/Log.h>
#include <mtkcam/common.h>
//
#include <vector>
using namespace std;
//
#include <camera/MtkCamera.h>
//
#include <mtkcam/v1/config/PriorityDefs.h>
#include <CamUtils.h>
using namespace android;
using namespace MtkCamUtils;
using namespace NSCam;
using namespace NSCam::Utils;
//
#include <sys/prctl.h>
#include <cutils/atomic.h>
//
// #include <mtkcam/hal/IHalSensor.h>
//
// #include <mtkcam/aaa_hal_common.h>
// #include <mtkcam/hal/IHal3A.h>
// using namespace NS3A;
//
#include <mtkcam/v1/IParamsManager.h>
#include <CamNode/RawDumpCmdQueThread.h>
// #include <mtkcam/camnode/IspSyncControl.h>
// #include <CtrlNode/DefaultCtrlNode.h>
// using namespace NSCamNode;
//
// #include <Callback/ICallBackZoom.h>
//
// #include <cutils/atomic.h>
// #include <utils/Mutex.h>
// #include <utils/Condition.h>
//
//#include <camera_custom_nvram.h>
#include <cutils/properties.h>
#include <aaa_types.h>
#include <aaa_error_code.h>
#include <aaa_log.h>
#include <mtkcam/featureio/aaa_hal_if.h>
#include <aaa_hal.h>
#include <camera_custom_nvram.h>
#include <awb_param.h>
#include <af_param.h>
#include <flash_param.h>
#include <ae_param.h>
#include <camera_custom_AEPlinetable.h>
//#include <CamDefs.h>
//#include <faces.h>
#include <isp_tuning.h>
#include <isp_mgr.h>
#include <isp_tuning_mgr.h>
#include <mtkcam/algorithm/lib3a/ae_algo_if.h>
//#include <sensor_hal.h>
#include <nvram_drv_mgr.h>
#include <ae_tuning_custom.h>
#include <aaa_sensor_mgr.h>
#include "camera_custom_hdr.h"
#include <kd_camera_feature.h>
#include <mtkcam/hal/IHalSensor.h>
#include "ae_mgr_if.h"
#include "ae_mgr.h"
using namespace NS3A;
using namespace NSIspTuning;
//
// using namespace android;
//
//
#if defined(__func__)
#undef __func__
#endif
#define __func__ __FUNCTION__

#if 1
#define MY_LOGV(fmt, arg...)        CAM_LOGV("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("(%d)[%s] " fmt, ::gettid(), __FUNCTION__, ##arg)
#else
#define MY_LOGV(fmt, arg...)        CAM_LOGV("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG, __func__, ##arg)
#define MY_LOGD(fmt, arg...)        CAM_LOGD("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG, __func__, ##arg)
#define MY_LOGI(fmt, arg...)        CAM_LOGI("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG, __func__, ##arg)
#define MY_LOGW(fmt, arg...)        CAM_LOGW("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG, __func__, ##arg)
#define MY_LOGE(fmt, arg...)        CAM_LOGE("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG, __func__, ##arg)
#define MY_LOGA(fmt, arg...)        CAM_LOGA("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG,__func__, ##arg)
#define MY_LOGF(fmt, arg...)        CAM_LOGF("[%s] " fmt,  __func__, ##arg); \
                                    printf("[%s/%s] " fmt"\n", LOG_TAG,__func__, ##arg)
#endif
#define FUNC_START  MY_LOGD("+")
#define FUNC_END    MY_LOGD("-")
#define FUNC_NAME   MY_LOGD("")
//
#define FUNCTION_IN               MY_LOGD("+")
#define FUNCTION_OUT              MY_LOGD("-")
//
#define CHECK_RET( exp ) do{if(!(exp)) { MY_LOGE(#exp); return false; }}while(0)
/*******************************************************************************
*
********************************************************************************/
namespace NSCamNode {

/******************************************************************************
*
*******************************************************************************/


class RawDumpCmdQueThread : public IRawDumpCmdQueThread
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Basic Interface
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    // Ask this object's thread to exit. This function is asynchronous, when the
    // function returns the thread might still be running. Of course, this
    // function can be called from a different thread.
    virtual void        requestExit();

    // Good place to do one-time initializations
    virtual status_t    readyToRun();

private:
    // Derived class must implement threadLoop(). The thread starts its life
    // here. There are two ways of using the Thread object:
    // 1) loop: if threadLoop() returns true, it will be called again if
    //          requestExit() wasn't called.
    // 2) once: if threadLoop() returns false, the thread will exit upon return.
    virtual bool        threadLoop();


public:
    static RawDumpCmdQueThread* getInstance(MUINT32 mem_out_width, MUINT32 mem_out_height,MUINT32 bitOrder, MUINT32 bitDepth, sp<IParamsManager> pParamsMgr);
    virtual             ~RawDumpCmdQueThread();
    virtual void setCallbacks(sp<CamMsgCbInfo> const& rpCamMsgCbInfo);
    virtual int setWidth(MUINT32 w);
    virtual int setHeight(MUINT32 h);
    virtual int setStrides(MUINT32 s);
protected:
    RawDumpCmdQueThread(MUINT32 mem_out_width, MUINT32 mem_out_height,MUINT32 bitOrder, MUINT32 bitDepth, sp<IParamsManager> pParamsMgr);
    sp<CamMsgCbInfo>                mpCamMsgCbInfo;
    sp<IParamsManager>           mspParamsMgr;
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Public to IRawDumpCmdQueThread
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    virtual int32_t     getTid()        const   { return mi4Tid; }
    virtual bool        isExitPending() const   { return exitPending(); }
    virtual bool        postCommand(MUINTPTR buf_addr, MUINT32 buf_size, MUINT64 buf_timestamp);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Detail operation
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    bool                init();
    bool                uninit();
    int32_t             getFreeSlot();
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Command-related
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    bool                                getCommand(sp<RawDumpCmdCookie> &rCmdCookie);
    List< sp<RawDumpCmdCookie> >        mCmdQ;
    Mutex                               mCmdMtx;
    Condition                           mCmdCond;

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Members.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    int32_t                      mi4Tid;
    uint32_t                     mFrameCnt;
    int32_t                      mErrorCode;
    uint32_t                     mMemOutWidth;
    uint32_t                     mMemOutHeight;
    uint32_t                     mBitOrder;
    uint32_t                     mBitDepth;
    uint32_t                     mMemStrides;

    uint8_t ** mBufSlot;
    bool   mIsBufUsed[BUFCNT];
    uint64_t mBufTimeStamp[BUFCNT];
};

/******************************************************************************
*
*******************************************************************************/
RawDumpCmdQueThread::RawDumpCmdQueThread(MUINT32 mem_out_width, MUINT32 mem_out_height,MUINT32 bitOrder, MUINT32 bitDepth, sp<IParamsManager> pParamsMgr)
    : mpCamMsgCbInfo (new CamMsgCbInfo)
    , mspParamsMgr(pParamsMgr)
    , mi4Tid(0)
    , mErrorCode(0)
    , mMemOutWidth(mem_out_width)
    , mMemOutHeight(mem_out_height)
    , mBitOrder (bitOrder)
    , mBitDepth (bitDepth)
{
    int i = 0;

    mBufSlot = (uint8_t **) malloc(BUFCNT*sizeof(uint32_t *));

    for (; i<BUFCNT; i++)
    {
        mIsBufUsed[i] = false;
    }
}

/******************************************************************************
*
*******************************************************************************/
RawDumpCmdQueThread::~RawDumpCmdQueThread()
{
    MY_LOGD("this=%p, sizeof:%d", this, sizeof(RawDumpCmdQueThread));
}


/******************************************************************************
*
*******************************************************************************/
void
RawDumpCmdQueThread::requestExit()
{
    FUNCTION_IN;
    //
    Thread::requestExit();
    //
    FUNCTION_OUT;
}

/******************************************************************************
*
*******************************************************************************/
void
RawDumpCmdQueThread::
setCallbacks(sp<CamMsgCbInfo> const& rpCamMsgCbInfo)
{
    //  value copy
    FUNCTION_IN;
    *mpCamMsgCbInfo = *rpCamMsgCbInfo;
    MY_LOGD("RawDumpCmdQueThread: mpCamMsgCbInfo.get(%p), mpCamMsgCbInfo->getStrongCount(%d)", mpCamMsgCbInfo.get(), mpCamMsgCbInfo->getStrongCount());
    FUNCTION_OUT;
}

/******************************************************************************
*
*******************************************************************************/
bool
RawDumpCmdQueThread::
postCommand(MUINTPTR buf_addr, MUINT32 buf_size, MUINT64 buf_timestamp)
{
    FUNCTION_IN;
    //
    bool ret = true;
    CamProfile profile(__FUNCTION__, "RawDumpCmdQueThread::postCommand");
    //
    {
        Mutex::Autolock _l(mCmdMtx);
        // add frame count for remember what frame it is.
        mFrameCnt++;

        MY_LOGD("+ tid(%d), frame_count (%d), buf_addr(%p), buf_size(%d)", ::gettid(), mFrameCnt, buf_addr, buf_size);


        if (!mCmdQ.empty())
        {
            MY_LOGD("queue is not empty, (%d) is in the head of queue, Q size (%d)", ((*mCmdQ.begin())->getSlotIndex()), mCmdQ.size());
        }

        // query a free buffer slot
        int i = getFreeSlot();
        // if buffer is out of use, then drop the reqeust this time.
        if (i >= BUFCNT || buf_size == 0)
        {
            MY_LOGD("- frame dropped:  tid(%d), frame_count(%d), Q size(%d)", ::gettid(), mFrameCnt, mCmdQ.size());
            sp<RawDumpCmdCookie> cmdCookie(new RawDumpCmdCookie(mFrameCnt, i, 0));
            mCmdQ.push_back(cmdCookie);
            mCmdCond.broadcast();
        }
        else
        {
            mIsBufUsed[i] = true;
            if(mspParamsMgr->getInt(MtkCameraParameters::KEY_ENG_VIDEO_RAW_DUMP_RESIZE)==2)
                {
                    mBufSlot[i] = (uint8_t*) malloc(buf_size/2);
                }
            else
                {
                    mBufSlot[i] = (uint8_t*) malloc(buf_size);
                }
            // copy buf_addr to our internal buffer[0-#]
            mBufTimeStamp[i]=buf_timestamp;

            if (mBufSlot[i]!=NULL)
            {
                if(mspParamsMgr->getInt(MtkCameraParameters::KEY_ENG_VIDEO_RAW_DUMP_RESIZE)==2)
                {

                     buf_addr=buf_addr+(mMemStrides*mMemOutHeight/4);
                     buf_size= buf_size/2;

                     memcpy(mBufSlot[i],(uint8_t*)buf_addr,buf_size);
                     MY_LOGD("COPY from %p to %p with %d byte",(uint8_t*)buf_addr,mBufSlot[i],buf_size);
                }
                else
                {
                     memcpy(mBufSlot[i],(uint8_t*)buf_addr,buf_size);
                     MY_LOGD("COPY from %p to %p with %d byte",(uint8_t*)buf_addr,mBufSlot[i],buf_size);
                }
            }
            else
            {
                MY_LOGD("allocate fail, mBufSlot[%d]=null", i);
                MY_LOGD("- frame dropped:  tid(%d), frame_count(%d), Q size(%d)", ::gettid(), mFrameCnt, mCmdQ.size());
                sp<RawDumpCmdCookie> cmdCookie(new RawDumpCmdCookie(mFrameCnt, i, 0));
                mCmdQ.push_back(cmdCookie);
                mCmdCond.broadcast();
                profile.print();
                return ret;
            }
            sp<RawDumpCmdCookie> cmdCookie(new RawDumpCmdCookie(mFrameCnt, i, buf_size));

            mCmdQ.push_back(cmdCookie);
            mCmdCond.broadcast();
            MY_LOGD("- frame added:  tid(%d), slot_index(%d), frame_count(%d), que size(%d)", ::gettid(),i, mFrameCnt, mCmdQ.size());
        }
    }
    //
    profile.print();
    FUNCTION_OUT;
    //
    return ret;
}

/******************************************************************************
*
*******************************************************************************/
bool
RawDumpCmdQueThread::
getCommand(sp<RawDumpCmdCookie> &rCmdCookie)
{
    FUNCTION_IN;
    //
    bool ret = false;
    //
    Mutex::Autolock _l(mCmdMtx);
    //
    MY_LOGD("+ tid(%d), que size(%d)", ::gettid(), mCmdQ.size());
    //
    while ( mCmdQ.empty() && ! exitPending() )
    {
        mCmdCond.wait(mCmdMtx);
    }
    // get the latest frame, e.g. drop the
    if ( !mCmdQ.empty() )
    {
        rCmdCookie = *mCmdQ.begin();
        mCmdQ.erase(mCmdQ.begin());
        ret = true;
        MY_LOGD(" frame[%d] in slot[%d] is dequeued.", rCmdCookie->getFrameCnt(),rCmdCookie->getSlotIndex() );
    }
    //
    MY_LOGD("- tid(%d), que size(%d), ret(%d)", ::gettid(), mCmdQ.size(), ret);
    //
    FUNCTION_OUT;
    //
    return ret;
}


int32_t
RawDumpCmdQueThread::
getFreeSlot()
{
    int i = 0;
    for (; i<BUFCNT; i++)
    {
        if(mIsBufUsed[i]==false)
            break;
    }
    MY_LOGD("+ got Free Slot(%d)", i);

    return i;
}


/******************************************************************************
*
*******************************************************************************/
status_t
RawDumpCmdQueThread::readyToRun()
{
    FUNCTION_IN;
    //
    // (1) set thread name
    ::prctl(PR_SET_NAME,"RawDumpCmdQueThread", 0, 0, 0);

    // (2) set thread priority
    // [!!]Priority RR?
#if MTKCAM_HAVE_RR_PRIORITY
    int32_t const policy = SCHED_RR;
    int32_t const priority = PRIO_RT_CAMERA_PREVIEW;
    struct sched_param sched_p;
    ::sched_getparam(0, &sched_p);
    sched_p.sched_priority = priority;
    ::sched_setscheduler(0, policy, &sched_p);

    //test
    mi4Tid = ::gettid();
    ::sched_getparam(0, &sched_p);
    MY_LOGD(
        "Tid: %d, policy: %d, priority: %d"
        , mi4Tid, ::sched_getscheduler(0)
        , sched_p.sched_priority
    );
#endif
    //
    mFrameCnt = 0;
    //
    FUNCTION_OUT;
    //
    return NO_ERROR;
}

/******************************************************************************
*
*******************************************************************************/
bool
RawDumpCmdQueThread::threadLoop()
{
    FUNCTION_IN;
    //
    bool ret = true;
    sp<RawDumpCmdCookie> pCmdCookie;

    //
    if (getCommand(pCmdCookie))
    {
        CamProfile profile(__FUNCTION__, "RawDumpCmdQueThread::save");
        if (pCmdCookie->getBufSize()==0)
        {
            MY_LOGD("VINCENT, RAW DUMP IS STOPPED");
            mpCamMsgCbInfo->mNotifyCb(MTK_CAMERA_MSG_EXT_NOTIFY, MTK_CAMERA_MSG_EXT_NOTIFY_RAW_DUMP_STOPPED, 0, mpCamMsgCbInfo->mCbCookie);
            return ret;
        }

        if(mErrorCode < 0)
        {
            MY_LOGD("+ [RDCT] tid(%d), Error(%d) returned)", ::gettid(),mErrorCode );

            return ret;
        }
        MY_LOGD("+ [RDCT] tid(%d), slotindex(%d), frame_count(%d))", ::gettid(),pCmdCookie->getSlotIndex(),pCmdCookie->getFrameCnt() );

        // write buffer[0-#] into disc
        String8 ms8RawFilePath(mspParamsMgr->getStr(MtkCameraParameters::KEY_RAW_PATH)); // => /storage/sdcard1/DCIM/CameraEM/Preview01000108ISO0.raw
        String8 ms8RawFileExt(ms8RawFilePath.getPathExtension()); // => .raw
        ms8RawFilePath = ms8RawFilePath.getBasePath(); // => /storage/sdcard1/DCIM/CameraEM/Preview01000108ISO0

        char mpszSuffix[256] = {0};

        if(mspParamsMgr->getInt(MtkCameraParameters::KEY_ENG_VIDEO_RAW_DUMP_RESIZE)==2)
        {
            mMemOutHeight=mMemOutHeight/2;
        }

        sprintf(mpszSuffix, "__%dx%d_%d_%d_%03d_0x%lld", mMemOutWidth, mMemOutHeight, mBitOrder, mBitDepth, pCmdCookie->getFrameCnt(), mBufTimeStamp[pCmdCookie->getSlotIndex()]); /* info from EngShot::onCmd_capture */

        ms8RawFilePath.append(mpszSuffix);
        ms8RawFilePath.append(ms8RawFileExt);
        MY_LOGD("Written buffer addr=%p, buffer size=%d",mBufSlot[pCmdCookie->getSlotIndex()], pCmdCookie->getBufSize());
        bool ret = saveBufToFile(ms8RawFilePath.string(), mBufSlot[pCmdCookie->getSlotIndex()], pCmdCookie->getBufSize()); // bool ret = saveBufToFile(ms8RawFilePath.string(), const_cast<uint8_t*>(puRawImgBuf), u4RawImgSize);
        MY_LOGD("Raw saved: %d: %s", ret, ms8RawFilePath.string());

        // free buffer
        free(mBufSlot[pCmdCookie->getSlotIndex()]);
        mIsBufUsed[pCmdCookie->getSlotIndex()]=false;

        // if there is any error, set mErrorCode;
        if (ret==false)
        {
            mErrorCode = -1;

        #if 1   //defined(MTK_CAMERA_BSP_SUPPORT)
            if (mpCamMsgCbInfo != NULL)
            mpCamMsgCbInfo->mNotifyCb(MTK_CAMERA_MSG_EXT_NOTIFY, MTK_CAMERA_MSG_EXT_NOTIFY_RAW_DUMP_STOPPED, 0, NULL);
        #endif
        }

        if (pCmdCookie->getFrameCnt()== (BUFCNT-1))
        {
            if (mpCamMsgCbInfo!=NULL)
                mpCamMsgCbInfo->mNotifyCb(MTK_CAMERA_MSG_EXT_NOTIFY, MTK_CAMERA_MSG_EXT_NOTIFY_RAW_DUMP_STOPPED, 0, mpCamMsgCbInfo->mCbCookie);
            else
                MY_LOGD("one of them are null");
        }
        profile.print();
    }
    //
    FUNCTION_OUT;
    //
    return ret;
}



/******************************************************************************
*
*******************************************************************************/
bool
RawDumpCmdQueThread::init()
{
    FUNCTION_IN;
    bool ret = true;
    int i = 0;

    mBufSlot = (uint8_t **) malloc(BUFCNT*sizeof(uint32_t *));

    for (; i<BUFCNT; i++)
    {
        mIsBufUsed[i] = false;
    }

lbExit:
    //
    FUNCTION_OUT;

    return ret;
}

/******************************************************************************
*
*******************************************************************************/
bool
RawDumpCmdQueThread::uninit()
{
    FUNCTION_IN;
    //
    bool ret = true;

    // free buffer
    free(mBufSlot);
    //mspPreviewBufHandler->freeBuffer();

    FUNCTION_OUT;
    //
    return ret;
}


/******************************************************************************
*
*******************************************************************************/
RawDumpCmdQueThread*
RawDumpCmdQueThread::
getInstance(MUINT32 mem_out_width, MUINT32 mem_out_height, MUINT32 bitOrder, MUINT32 bitDepth, sp<IParamsManager> pParamsMgr)
{
    return new RawDumpCmdQueThread(mem_out_width, mem_out_height, bitOrder, bitDepth, pParamsMgr);
}


/******************************************************************************
*
*******************************************************************************/
IRawDumpCmdQueThread*
IRawDumpCmdQueThread::
createInstance(MUINT32 mem_out_width, MUINT32 mem_out_height, MUINT32 bitOrder, MUINT32 bitDepth, sp<IParamsManager> pParamsMgr)
{
    return  RawDumpCmdQueThread::getInstance(mem_out_width, mem_out_height, bitOrder, bitDepth,  pParamsMgr);
}

int
RawDumpCmdQueThread::
setWidth(MUINT32 w)
{
    mMemOutWidth = w;
    return 0;
}

int
RawDumpCmdQueThread::
setHeight(MUINT32 h)
{
    mMemOutHeight = h;
    return 0;
}

int
RawDumpCmdQueThread::
setStrides(MUINT32 s)
{
    mMemStrides = s;
    return 0;
}


#if 0
/******************************************************************************
*
*******************************************************************************/
int
RawDumpCmdQueThread::
getMaxBufferCount(MUINT32 buf_size)
{
    uint8_t i, buf_size;
    uint8_t *Buf;

    i = 0;
    while (Buf)
    {
        i++;
        Buf = (uint8_t *)malloc(i * buf_size);
        free(Buf);
    }
    i--;
    i >> 1;
    (3 * i) >> 2;
    return (3 * i) >> 2;
}
#endif

void EngUtil(MINT32 sensorInd, IHal3A* pHal3a, sp<IParamsManager> spParamsMgr)
{
    FUNCTION_IN;

    //(2) BV value (3A --> AP)
    FrameOutputParam_T RTParams;
    pHal3a->getRTParams(RTParams);
    int rt_BV = RTParams.i4BrightValue_x10;
    int rt_FPS = RTParams.u4FRameRate_x10;
    spParamsMgr->updateBrightnessValue(rt_BV);

    // Get fps from timestamp of pass1 buffer
    //spParamsMgr->updatePreviewFrameInterval(0); // Todo: Cannot find the timestampe // spParamsMgr->updatePreviewFrameInterval(i4P1_Frame_Interval);


    int index, shutter, isp_gain, sensor_gain;
    index = RTParams.u4AEIndex;
    shutter = RTParams.u4PreviewShutterSpeed_us; // RTParams.u4ShutterSpeed_us; // NOT in "aaa_hal_common.h" // Only in "aaa_hal_base.h"
    isp_gain = RTParams.u4PreviewISPGain_x1024; // RTParams.u4ISPGain_x1024; // NOT in "aaa_hal_common.h" // Only in "aaa_hal_base.h"
    sensor_gain = RTParams.u4PreviewSensorGain_x1024; // RTParams.u4SensorGain_x1024; // NOT in "aaa_hal_common.h" // Only in "aaa_hal_base.h"
    // Get AE index, shutter, isp_gain, sensor_gain
    spParamsMgr->updatePreviewAEIndexShutterGain(index, shutter, isp_gain, sensor_gain);

    shutter = RTParams.u4CapShutterSpeed_us;
    isp_gain = RTParams.u4CapISPGain_x1024;
    sensor_gain = RTParams.u4CapSensorGain_x1024;
    // Get Capture shutter, isp_gain, sensor_gain from Preview
    spParamsMgr->updateCaptureShutterGain(shutter, isp_gain, sensor_gain);

    { // EV Calibration
        MINT32 iAECurrentEV;
        MUINT32 iOutLen;
        iAECurrentEV = 0;

        NS3A::AeMgr::getInstance(IHalSensorList::get()->querySensorDevIdx(sensorInd)).CCTOPAEGetCurrentEV(&iAECurrentEV, &iOutLen); // MINT32 AeMgr::CCTOPAEGetCurrentEV(MINT32 *a_pAECurrentEV, MUINT32 *a_pOutLen)

        AE_NVRAM_T AENVRAM;
        AENVRAM.rCCTConfig.i4BVOffset = 0;
        NS3A::AeMgr::getInstance(IHalSensorList::get()->querySensorDevIdx(sensorInd)).CCTOPAEGetNVRAMParam(&AENVRAM, &iOutLen); // MINT32 AeMgr::CCTOPAEGetNVRAMParam(MVOID *a_pAENVRAM, MUINT32 *a_pOutLen)

        if (spParamsMgr != NULL)
        {
            spParamsMgr->set(MtkCameraParameters::KEY_ENG_EV_CALBRATION_OFFSET_VALUE, iAECurrentEV + AENVRAM.rCCTConfig.i4BVOffset);

            spParamsMgr->getStr(MtkCameraParameters::KEY_ENG_EV_CALBRATION_OFFSET_VALUE).string();
            CAM_LOGD("KEY_ENG_EV_CALBRATION_OFFSET_VALUE = %s", spParamsMgr->getStr(MtkCameraParameters::KEY_ENG_EV_CALBRATION_OFFSET_VALUE).string()); // debug
        }
        else
        {
            // Show some message for spParamsMgr is NULL
        }
    }

    FUNCTION_OUT;
}

};  //namespace NSCamNode

