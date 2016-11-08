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
 *     TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#define LOG_TAG "MtkCam/HDRShot"

#include "MyHdr.h"
#include <utils/threads.h>
#include <sys/prctl.h>  // For prctl()/PR_SET_NAME.

//
#include <mtkcam/Log.h>
#include <mtkcam/common.h>
#include <mtkcam/exif/IBaseCamExif.h>
#include <mtkcam/exif/CamExif.h>
//
#include <Shot/IShot.h>
//
#include "ImpShot.h"
#include "Hdr.h"
//

#define USING_MTK_PERFSERVICE_HDR
#ifdef USING_MTK_PERFSERVICE_HDR
#include "PerfServiceNative.h"
#include <mtkcam/drv/res_mgr_drv.h>

//systrace
#if 1
#ifndef ATRACE_TAG
#define ATRACE_TAG                           ATRACE_TAG_CAMERA
#endif
#include <utils/Trace.h>

#define HDR_TRACE_CALL()                      ATRACE_CALL()
#define HDR_TRACE_NAME(name)                  ATRACE_NAME(name)
#define HDR_TRACE_BEGIN(name)                 ATRACE_BEGIN(name)
#define HDR_TRACE_END()                       ATRACE_END()
#else
#define HDR_TRACE_CALL()
#define HDR_TRACE_NAME(name)
#define HDR_TRACE_BEGIN(name)
#define HDR_TRACE_END()
#endif

//CPU Affinity
#include <linux/mt_sched.h>


#include <dlfcn.h>

#define MY_LOGD                 MY_DBG
#define MY_LOGE                 MY_ERR


static MBOOL   initPerf();
static MBOOL   uninitPerf();

MBOOL enablePerfService(MUINT32 scen);
MBOOL disablePerfService(MUINT32 scen);

#define PERF_LIB_FULLNAME        "libperfservicenative.so"
#define STR_FUNC_REG             "PerfServiceNative_userRegBigLittle"
#define STR_FUNC_ENABLETIMEOUT   "PerfServiceNative_userEnable"
#define STR_FUNC_DISABLE         "PerfServiceNative_userDisable"
#define STR_FUNC_UNREG           "PerfServiceNative_userUnreg"
typedef int (*funcPerfRegBL)(int, int, int, int);
typedef void (*funcEnableTimeout)(int, int);
typedef void (*funcDisable)(int);
typedef int (*funcPerfUnreg)(int);
static Mutex               gPerfLock;
static void*               gPerfLib = NULL;
funcEnableTimeout          gPerfEnable = NULL;
funcDisable                gPerfDisable = NULL;
static int                 gPerfEnabledIdx = -1;
//
struct camera_scenario
{
    MUINT32 scen;
    int bigcore;
    int bigfreq;
    int littlecore;
    int littlefreq;
    int timeout;
};

#define SCENARIO_SIZE   (2)
static camera_scenario gScenTable[SCENARIO_SIZE] =
{
#if 1
#if 0
    { ResMgrDrv::SCEN_SW_CAM_PRV, 2, 2002000, 0, 0, 1 },
    { ResMgrDrv::SCEN_SW_CAM_CAP, 2, 2002000, 0, 0, 1 },
#endif
#if 1
    { ResMgrDrv::SCEN_SW_CAM_PRV, 2, 0, 0, 0, 1 },
    { ResMgrDrv::SCEN_SW_CAM_CAP, 2, 0, 0, 0, 1 },
#endif
#else
    { ResMgrDrv::SCEN_SW_CAM_PRV, 0, 0, 4, 2002000, 1 },
    { ResMgrDrv::SCEN_SW_CAM_CAP, 0, 0, 4, 2002000, 1 },
#endif
};
static camera_scenario gPlusScenTable[SCENARIO_SIZE] =
{
    { ResMgrDrv::SCEN_SW_CAM_PRV, 0, 0, 3, 1950000, 1 },
    { ResMgrDrv::SCEN_SW_CAM_CAP, 0, 0, 3, 1950000, 1 },
};
static int gScenHandle[SCENARIO_SIZE] =
{ -1, -1 };

//-----------------------------------------------------------------------------
MBOOL
initPerf()
{
    HDR_TRACE_CALL();
    MY_LOGD("");
    Mutex::Autolock _l(gPerfLock);
    if( !gPerfLib )
    {
        gPerfLib = ::dlopen(PERF_LIB_FULLNAME, RTLD_NOW);
        if  ( ! gPerfLib )
        {
            char const *err_str = ::dlerror();
            MY_LOGE("dlopen: %s error=%s", PERF_LIB_FULLNAME, (err_str ? err_str : "unknown"));
            goto lbExit;
        }
    }

    gPerfEnable = reinterpret_cast<funcEnableTimeout>(dlsym(gPerfLib, STR_FUNC_ENABLETIMEOUT));
    if( gPerfEnable == NULL )
        MY_LOGE("cannot get %s", STR_FUNC_ENABLETIMEOUT);
    //
    gPerfDisable = reinterpret_cast<funcDisable>(dlsym(gPerfLib, STR_FUNC_DISABLE));
    if( gPerfDisable == NULL )
        MY_LOGE("cannot get %s", STR_FUNC_DISABLE);
    //
    {
        funcPerfRegBL pReg = NULL;
        pReg = reinterpret_cast<funcPerfRegBL>(dlsym(gPerfLib, STR_FUNC_REG));
        if( pReg == NULL )
        {
            MY_LOGE("cannot get %s", STR_FUNC_REG);
            goto lbExit;
        }
        // register scenario

        for( MUINT32 i = 0 ; i < SCENARIO_SIZE; i++ )
        {
            if( gScenHandle[i] == - 1 )
            {
                gScenHandle[i] = pReg(
                        gScenTable[i].bigcore,
                        gScenTable[i].bigfreq,
                        gScenTable[i].littlecore,
                        gScenTable[i].littlefreq
                        );
                if( gScenHandle[i] == -1 )
                {
                    MY_LOGE("register scenario failed");
                    goto lbExit;
                }
            }
        }

    }
    //
lbExit:
    return MTRUE;
}
//-----------------------------------------------------------------------------
MBOOL
uninitPerf()
{
    HDR_TRACE_CALL();
    MY_LOGD("");
    Mutex::Autolock _l(gPerfLock);
    funcPerfUnreg pUnreg = NULL;
    //
    if( !gPerfLib )
    {
        MY_LOGE("no lib");
        return MFALSE;
    }
    //
    if( gPerfEnabledIdx != -1 )
    {
        if( gPerfDisable )
        {
            MY_LOGE("disable previous scen idx %d", gPerfEnabledIdx);
            gPerfDisable(gScenHandle[gPerfEnabledIdx]);
            gPerfEnabledIdx = -1;
        }
        else
        {
            MY_LOGE("cannot disable idx %d", gPerfEnabledIdx);
        }
    }
    //
    pUnreg = reinterpret_cast<funcPerfUnreg>(dlsym(gPerfLib, STR_FUNC_UNREG));
    if( pUnreg != NULL )
    {
        for( MUINT32 i = 0 ; i < SCENARIO_SIZE; i++ )
        {
            if( gScenHandle[i] != - 1 )
            {
                pUnreg(gScenHandle[i]);
                gScenHandle[i] = -1;
            }
        }
    }
    else
    {
        MY_LOGE("cannot get %s", STR_FUNC_UNREG);
    }
    //
    gPerfEnable = NULL;
    gPerfDisable = NULL;
    //
    ::dlclose(gPerfLib);
    gPerfLib = NULL;
    //
    return MTRUE;
}


MBOOL
enablePerfService(MUINT32 scen)
{
    HDR_TRACE_CALL();
    if( !gPerfEnable )
    {
        MY_LOGE("no func");
        return MFALSE;
    }
    //
    if( gPerfEnabledIdx != -1 )
    {
        MY_LOGE("disable previous scen idx %d", gPerfEnabledIdx);
        disablePerfService(gScenTable[gPerfEnabledIdx].scen);
    }
    //
    for( MUINT32 i = 0 ; i < SCENARIO_SIZE; i++ )
    {
        if( gScenTable[i].scen == scen )
        {
            if( gScenHandle[i] != -1 )
            {
                MY_LOGD("enable PerfService, scen %d, idx %d", scen, i);
                gPerfEnable(gScenHandle[i], gScenTable[i].timeout);
                gPerfEnabledIdx = i;
            }
            break;
        }
    }
    //
    if( gPerfEnabledIdx == -1 )
    {
        MY_LOGE("cannot find rule for scen %d", scen);
    }
    //
    return MTRUE;

}
//-----------------------------------------------------------------------------
MBOOL
disablePerfService(MUINT32 scen)
{
    HDR_TRACE_CALL();

    if( !gPerfDisable )
    {
        MY_LOGE("no func");
        return MFALSE;
    }
    //
    if( gPerfEnabledIdx == -1 )
    {
        return MTRUE;
    }
    //
    for( MUINT32 i = 0 ; i < SCENARIO_SIZE; i++ )
    {
        if( gScenTable[i].scen == scen )
        {
            MY_LOGD("disable PerfService, scen %d, idx %d", scen, i);
            if( gPerfEnabledIdx != i )
            {
                MY_LOGE("idx not matched enabled %d vs. %d", gPerfEnabledIdx, i );
            }
            gPerfDisable(gScenHandle[gPerfEnabledIdx]);
            gPerfEnabledIdx = -1;
            break;
        }
    }
    return MTRUE;
}


MBOOL
enableForceToBigCore()
{
    HDR_TRACE_CALL();
    //CPU Affinity--------------------------------------------------------------------------/
    MY_LOGD("HDR flow CPUAffinity Start Pid = %d , Tid = %d\n", getpid(), gettid());

    int cpu_msk = 0xF0;  //Force run on big-core
    MY_LOGD("HDR flow CPUAffinity Set CPU Affinity Mask= %d\n", cpu_msk);

    cpu_set_t cpuset, cpuold, cpubackup;
    int s,j;
    CPU_ZERO(&cpuset);

    for(unsigned int Msk=1, cpu_no=0; Msk<0xFF; Msk<<=1, cpu_no++)
    {
        if(Msk&cpu_msk)
        {
            CPU_SET(cpu_no, &cpuset);
        }
    }

    s = mt_sched_getaffinity(gettid(), sizeof(cpu_set_t), &cpuold, &cpubackup);
    if (s <= 0)
    {
        MY_LOGD("HDR flow CPUAffinity Get fail");
        return MFALSE;
    }
    else
    {
        MY_LOGD("HDR flow CPUAffinity Thread %d Current CPU Affinity %08lx Backup Affinity %08lx", gettid(), cpuold.__bits[0], cpubackup.__bits[0]);
    }

    s = mt_sched_setaffinity(gettid(), sizeof(cpu_set_t), &cpuset);
    if (s != 0)
    {
        MY_LOGD("HDR flow CPUAffinity Set fail");
        return MFALSE;
    }
    else
    {
        s = mt_sched_getaffinity(gettid(), sizeof(cpu_set_t), &cpuset, &cpubackup);
        MY_LOGD("HDR flow CPUAffinity Thread %d New CPU Affinity %08lx Backup Affinity %08lx", gettid(), cpuset.__bits[0], cpubackup.__bits[0]);
    }
    /*--------------------------------------------------------------------------CPU Affinity*/

    return MTRUE;

}


MBOOL
disableForceToBigCore()
{
    HDR_TRACE_CALL();
    int status;

    pid_t tid = gettid();
    status = mt_sched_exitaffinity(gettid());
    if(!status)
    {
        MY_LOGD("HDR flow CPUAffinity Exit affinity to CPU1 successfully, status=%d\n", status);
        return MTRUE;
    }
    else
    {
        MY_LOGD("HDR flow CPUAffinity Exit affinity to CPU1 FAIL, status=%d, tid=%d\n", status, tid);
        return MFALSE;
    }

}


#endif



using namespace android;
using namespace NSShot;

extern "C"
sp<IShot>
createInstance_HdrShot(
    char const*const    pszShotName,
    uint32_t const      u4ShotMode,
    int32_t const       i4OpenId
)
{
    sp<IShot>       pShot = NULL;
    sp<HdrShot>  pImpShot = NULL;
    //
    //  (1.1) new Implementator.
    pImpShot = new HdrShot(pszShotName, u4ShotMode, i4OpenId);
    if  ( pImpShot == 0 ) {
        CAM_LOGE("[%s] new HdrShot", __FUNCTION__);
        goto lbExit;
    }
    //
    //  (1.2) initialize Implementator if needed.
    if  ( ! pImpShot->onCreate() ) {
        CAM_LOGE("[%s] onCreate()", __FUNCTION__);
        goto lbExit;
    }
    //
    //  (2)   new Interface.
    pShot = new IShot(pImpShot);
    if  ( pShot == 0 ) {
        CAM_LOGE("[%s] new IShot", __FUNCTION__);
        goto lbExit;
    }
    //
lbExit:
    //
    //  Free all resources if this function fails.
    if  ( pShot == 0 && pImpShot != 0 ) {
        pImpShot->onDestroy();
        pImpShot = NULL;
    }
    //
    return  pShot;
}


/******************************************************************************
 *  This function is invoked when this object is firstly created.
 *  All resources can be allocated here.
 ******************************************************************************/
bool
HdrShot::
onCreate()
{
    FUNCTION_LOG_START;
    bool ret = true;

    FUNCTION_LOG_END;
    return ret;
}


/******************************************************************************
 *  This function is invoked when this object is ready to destryoed in the
 *  destructor. All resources must be released before this returns.
 ******************************************************************************/
void
HdrShot::
onDestroy()
{
    FUNCTION_LOG_START;
    uninit();
}


/*******************************************************************************
*
*******************************************************************************/
HdrShot::
HdrShot(char const*const pszShotName, uint32_t const u4ShotMode, int32_t const i4OpenId)
    : ImpShot(pszShotName, u4ShotMode, i4OpenId)
    ////    Resolutions.
    , mRaw_Width(0)
    , mRaw_Height(0)
    , mu4W_yuv(0)
    , mu4H_yuv(0)
    , mu4W_small(0)
    , mu4H_small(0)
    , mu4W_se(0)
    , mu4H_se(0)
    , mu4W_dsmap(0)
    , mu4H_dsmap(0)
    , mPostviewWidth(800)
    , mPostviewHeight(600)
    , mPostviewFormat(eImgFmt_YV12)
    , mRotPicWidth(0)
    , mRotPicHeight(0)
    , mRotThuWidth(0)
    , mRotThuHeight(0)

    , mErrorFlag(0)
    //
    , mMainThread(0)
    , mMemoryAllocateThread(0)
    //
    , mTrigger_alloc_working(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mTrigger_alloc_bmap1(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_Capbuf(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_pass2_first(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_pass2_others(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_jpeg_full(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_jpeg_thumbnail(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_working(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_se(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_bmap0(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_bmap1(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_bmap2(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_blending(PTHREAD_MUTEX_INITIALIZER_LOCK)
    , mMemoryReady_postview(PTHREAD_MUTEX_INITIALIZER_LOCK)

    // pipes
    , mpHdrHal(NULL)
    //, mpCamExif({NULL})
    // Buffers
    , mpIMemDrv(NULL)
    , mpIImageBufAllocator(NULL)
    , mpCapBufMgr(NULL)
    //, std::vector<ImageBufferMap> mvImgBufMap;
    , mpHeap(NULL)
    , mTotalBufferSize(0)
    , mTotalKernelBufferSize(0)
    , mTotalUserBufferSize(0)
    //
    //, mpSourceImgBuf({NULL})
    //, mpSmallImgBuf({NULL})
    //, mpSEImgBuf({NULL})
    //, mWeightingBuf({NULL})
    //, mpBlurredWeightMapBuf({NULL})
    //, mpDownSizedWeightMapBuf({NULL})
    , mBlendingBuf(NULL)
    , mpPostviewImgBuf(NULL)
    //
    , mpHdrWorkingBuf(NULL)
    , mpMavWorkingBuf(NULL)
    //
    , mNormalJpegBuf(NULL)
    , mNormalThumbnailJpegBuf(NULL)
    , mHdrJpegBuf(NULL)
    , mHdrThumbnailJpegBuf(NULL)

    //
    //, HDR_PIPE_SET_BMAP_INFO mHdrSetBmapInfo;
    , OriWeight(NULL)
    , BlurredWeight(NULL)

    //
    //, mu4RunningNumber(0)
    , mu4OutputFrameNum(0)
    //, mu4FinalGainDiff[2]
    , mu4TargetTone(0)
    //, HDR_PIPE_HDR_RESULT_STRUCT mrHdrCroppedResult;
    , mfgIsForceBreak(MFALSE)
    //
    , mHdrState(HDR_STATE_INIT)
    //
    , mShutterCBDone(MFALSE)
    , mRawCBDone(MFALSE)
    , mJpegCBDone(MFALSE)
    , mfgIsSkipthumb(MFALSE)
    //
    , mCaptueIndex(0)
    , mSensorType(0)
    //
    , mNrtype(0)

    // for development
    , mTestMode(0)
    , mDebugMode(0)
    , mPerfService(MFALSE)
    , mPrivateData(NULL)
    , mPrivateDataSize(0)

{
    mHDRShotMode = u4ShotMode;
    for(MUINT32 i=0; i<eMaxOutputFrameNum; i++) {
        //mpCamExif[i] = new CamExif;
        mpSourceRawImgBuf[i] = NULL;
        mpSourceImgBuf[i] = NULL;
        mpSmallImgBuf[i] = NULL;
        mpSEImgBuf[i] = NULL;
        mWeightingBuf[i] = NULL;
        mpBlurredWeightMapBuf[i] = NULL;
        mpDownSizedWeightMapBuf[i] = NULL;
    }

    //setShotParam() default values
    ShotParam param;
    param.mi4PictureWidth = 3264;
    param.mi4PictureHeight = 2448;
    param.mi4PostviewWidth = 800;
    param.mi4PostviewHeight = 600;
    setShotParam(&param, sizeof(ShotParam));

    mu4OutputFrameNum = 3;
    mu4FinalGainDiff[0]    = 2048;
    mu4FinalGainDiff[1]    = 512;
    mu4TargetTone        = 150;


}


/******************************************************************************
 *
 ******************************************************************************/
HdrShot::
~HdrShot()
{

}


/******************************************************************************
 *
 ******************************************************************************/
MBOOL
HdrShot::
setCapBufMgr(MVOID* pCapBufMgr)
{
    mpCapBufMgr = (CapBufMgr*)pCapBufMgr;
    return MTRUE;
}


/******************************************************************************
 *
 ******************************************************************************/
bool
HdrShot::
sendCommand(
    uint32_t const  cmd,
    MUINTPTR const  arg1,
    uint32_t const  arg2,
    uint32_t const  arg3
)
{
    FUNCTION_LOG_START;
    bool ret = true;
    //
    switch  (cmd)
    {
    //  This command is to reset this class. After captures and then reset,
    //  performing a new capture should work well, no matter whether previous
    //  captures failed or not.
    //
    //  Arguments:
    //          N/A
    case eCmd_reset:
        ret = onCmd_reset();
        break;

    //  This command is to perform capture.
    //
    //  Arguments:
    //          N/A
    case eCmd_capture:
        ret = onCmd_capture();
        break;

    //  This command is to perform cancel capture.
    //
    //  Arguments:
    //          N/A
    case eCmd_cancel:
        onCmd_cancel();
        break;
    //
    default:
        ret = ImpShot::sendCommand(cmd, arg1, arg2, arg3);
    }

    //
    FUNCTION_LOG_END;
    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
bool
HdrShot::
onCmd_reset()
{
    FUNCTION_LOG_START;
    bool ret = true;

    FUNCTION_LOG_END;
    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
bool
HdrShot::
onCmd_capture()
{
    FUNCTION_LOG_START;
    bool ret = true;
    SetThreadProp(SCHED_OTHER, -20);
    PrintThreadProp(__FUNCTION__);
    pthread_create(&mMainThread, NULL, HdrShot::onCmd_captureTask, this);
    MUINT32    threadRet = 0;
    pthread_join(mMainThread, (void**)&threadRet);
    mMainThread = 0;
    ret = threadRet;
    FUNCTION_LOG_END;
    return ret;
}


MVOID*
HdrShot::
onCmd_captureTask(MVOID *arg)
{
    FUNCTION_LOG_START;
    MUINTPTR ret = true;

    ::prctl(PR_SET_NAME,"HDR_MAIN", 0, 0, 0);
    PrintThreadProp(__FUNCTION__);
    SetThreadProp(SCHED_OTHER, -20);

    HdrShot *self = (HdrShot*)arg;

    if(!self) {
        MY_ERR("arg is null");
        ret = false;
        goto lbExit;
    }
    ret = self->mainflow();
lbExit:
    FUNCTION_LOG_END_MUM;
    return (MVOID*)ret;
}


bool
HdrShot::
mainflow()
{
    FUNCTION_LOG_START;
    bool ret = true;
    CPTLog(Event_HdrShot, CPTFlagStart);

#if (HDR_PROFILE_CAPTURE2)
    MyDbgTimer DbgTmr("capture");
#endif

    #ifdef USING_MTK_PERFSERVICE_HDR
    do
    {
        const char *fname = "/proc/chip/hw_code";
        char str[8] = {0};
        FILE* fp = fopen(fname, "rb");
        int rd_size;

        if (!fp)
        {
            CAM_LOGE("failed to open file [%s]", fname);
            break;
        }

        if (4 > (rd_size = fread(str, 1, sizeof(str), fp)))
        {
            CAM_LOGE("failed to read file [%s]", fname);
            fclose(fp);
            break;
        }

        mPerfService = (strncmp(str, "6795", 4) == 0);

        if (mPerfService)
        {
            MY_LOGD("This is 6795");
            memcpy((void*)gScenTable, (void*)gPlusScenTable, sizeof(camera_scenario) * SCENARIO_SIZE);

            initPerf();
            enablePerfService(gScenTable[1].scen);
            enableForceToBigCore();
        }
        fclose(fp);
    } while(0);
    #endif
    //

    if(mHDRShotMode == eShotMode_ZsdHdrShot){
        mpCapBufMgr = ImpShot::mpCapBufMgr;
    }

    ret = ret
        && init()
        && configureForSingleRun()
        && EVBracketCapture()
        && ImageRegistratoin()
        && WeightingMapGeneration()
        && Blending()
        ;

    //error handler
    if(!mTestMode)
    {

        if(!mShutterCBDone) {
            MY_ERR("send fake onCB_Shutter");
            mpShotCallback->onCB_Shutter(true,0);
        }

        #if 0   //95 hdr didn't use raw callback to save raw image.
        if(!mRawCBDone) {
            MY_ERR("send fake onCB_RawImage");
            MUINT32    u4ExifHeaderSize = 512;
            MUINT32    u4JpegSize = 512;
            MUINT8    puImageBuffer[1024];

            mpShotCallback->onCB_RawImage(0
                                        , u4ExifHeaderSize+u4JpegSize
                                        , reinterpret_cast<uint8_t const*>(puImageBuffer)
                                        );
        }
        #endif

        if(!mJpegCBDone) {
            MY_ERR("send fake onCB_CompressedImage");
            MUINT32    u4ExifHeaderSize = 512;
            MUINT8    puExifHeaderBuf[512];
            MUINT32    u4JpegSize = 512;
            MUINT8     puJpegBuf[512];
            MUINT32    u4Index = 0;
            MBOOL    bFinal = MTRUE;

            mpShotCallback->onCB_CompressedImage(0,
                                         u4JpegSize,
                                         reinterpret_cast<uint8_t const*>(puJpegBuf),
                                         u4ExifHeaderSize,    //header size
                                         puExifHeaderBuf,    //header buf
                                         u4Index,            //callback index
                                         bFinal                //final image
                                         );
        }
    }


    //@TODO list
    //#cancel
    //#multi-thread
    //#fail
    //#document
    //#full frame
    //#speed up do_DownScaleWeightMap()

lbExit:
    //  ()  HDR finished, clear HDR setting.
    do_HdrSettingClear();
    // Don't know exact time of lbExit in HDR flow, so release all again
    // (there is protection in each release function).
    releaseSourceRawImgBuf();
    releaseSourceImgBuf();
    releaseSmallImgBuf();
    releaseSEImgBuf();
    releaseHdrWorkingBuf();
    releaseOriWeightMapBuf();
    releaseDownSizedWeightMapBuf();
    releaseBlurredWeightMapBuf();
    releasePostviewImgBuf();

    releaseNormalJpegBuf();
    releaseNormalThumbnailJpegBuf();
    releaseHdrJpegBuf();
    releaseHdrThumbnailJpegBuf();

    releaseBlendingBuf();

    #if (HDR_PROFILE_CAPTURE2)
    DbgTmr.print("HdrProfiling2:: HDRFinish Time");
    #endif

    CPTLog(Event_HdrShot, CPTFlagEnd);

    #ifdef USING_MTK_PERFSERVICE_HDR
    if (mPerfService)
    {
        disableForceToBigCore();
        disablePerfService(gScenTable[1].scen);
        uninitPerf();
    }
    #endif

    FUNCTION_LOG_END;
    return ret;
}


/******************************************************************************
 *
 ******************************************************************************/
void
HdrShot::
onCmd_cancel()
{
    FUNCTION_LOG_START;
    //mfgIsForceBreak = MTRUE;
    // never cancel since hdr 8M only take 1.4s
    FUNCTION_LOG_END_MUM;
}


/*******************************************************************************
*
*******************************************************************************/
bool
HdrShot::
setShotParam(void const* pParam, size_t const size)
{
    FUNCTION_LOG_START;
    bool ret = true;

    if(!ImpShot::setShotParam(pParam, size)) {
        MY_ERR("[HDR] HdrShot->setShotParam() fail.");
        ret = false;
    }

    FUNCTION_LOG_END;
    return ret;
}






