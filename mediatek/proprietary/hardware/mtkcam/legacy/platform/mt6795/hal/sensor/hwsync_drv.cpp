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
//
#define LOG_TAG "Drv/HWsync"

#include <fcntl.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <cutils/atomic.h>
#include <semaphore.h>
#include <cutils/properties.h>  // For property_get().
//
#include "hwsync_drv_imp.h"
/*************************************************************************************
* Log Utility
*************************************************************************************/
#undef   DBG_LOG_TAG                        // Decide a Log TAG for current file.
#define  DBG_LOG_TAG        LOG_TAG

#include "../../core/drv/inc/drv_log.h"    // Note: DBG_LOG_TAG/LEVEL will be used in header file, so header must be included after definition.
DECLARE_DBG_LOG_VARIABLE(hwsyncDrv);
// Clear previous define, use our own define.
#undef LOG_VRB
#undef LOG_DBG
#undef LOG_INF
#undef LOG_WRN
#undef LOG_ERR
#undef LOG_AST
#define LOG_VRB(fmt, arg...)        do { if (hwsyncDrv_DbgLogEnable_VERBOSE) { BASE_LOG_VRB(fmt, ##arg); } } while(0)
#define LOG_DBG(fmt, arg...)        do { if (hwsyncDrv_DbgLogEnable_DEBUG  ) { BASE_LOG_DBG(fmt, ##arg); } } while(0)
#define LOG_INF(fmt, arg...)        do { if (hwsyncDrv_DbgLogEnable_INFO   ) { BASE_LOG_INF(fmt, ##arg); } } while(0)
#define LOG_WRN(fmt, arg...)        do { if (hwsyncDrv_DbgLogEnable_WARN   ) { BASE_LOG_WRN(fmt, ##arg); } } while(0)
#define LOG_ERR(fmt, arg...)        do { if (hwsyncDrv_DbgLogEnable_ERROR  ) { BASE_LOG_ERR(fmt, ##arg); } } while(0)
#define LOG_AST(cond, fmt, arg...)  do { if (hwsyncDrv_DbgLogEnable_ASSERT ) { BASE_LOG_AST(cond, fmt, ##arg); } } while(0)

#define WORK_AROUND_SENSOR_FPS_SETTING 1

pthread_mutex_t mStateMutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t mWaitRevertMutex = PTHREAD_MUTEX_INITIALIZER;

static HWSyncDrvImp singleton;
static IspDrv* gpispDrv=NULL;


MINT32 getUs()
{
   struct timeval tv;
   ::gettimeofday(&tv, NULL);
   return tv.tv_sec * 1000000 + tv.tv_usec;
}


/*******************************************************************************
*
********************************************************************************/
HWSyncDrv* HWSyncDrv::createInstance()
{
    return HWSyncDrvImp::getInstance();
}

/*******************************************************************************
*
********************************************************************************/
HWSyncDrvImp::HWSyncDrvImp()
{
    mCheckCnt=0;
    mAdjIdx=0;
    m1stSync=true;
    mHalSensor=NULL;
    mHalSensorList=NULL;
    mInitPclk[0]=mInitPclk[1]=0;
    mSensorDevArray[0]=mSensorDevArray[1]=SENSOR_DEV_NONE;
    mSensorTgArray[0]=mSensorTgArray[1]=CAM_TG_NONE;
    mSensorModeToggle[0]=mSensorModeToggle[1]=0;
    mSensorCurFpsArray[0]=mSensorCurFpsArray[1]=0;
    mSensorScenArray[0]=mSensorScenArray[1]=SENSOR_SCENARIO_ID_NORMAL_PREVIEW;
    mNeedwaitRevertFps=false;
    mremainUninitIdx=0;
}

/*******************************************************************************
*
********************************************************************************/
HWSyncDrvImp::~HWSyncDrvImp()
{
}

/*******************************************************************************
*
********************************************************************************/
HWSyncDrv* HWSyncDrvImp::getInstance(void)
{
    LOG_INF("singleton[0x%x],getpid[0x%08x],gettid[0x%08x] ",(MINTPTR)&singleton,getpid() ,gettid());
    return &singleton;
}

/*******************************************************************************
*
********************************************************************************/
void HWSyncDrvImp::destroyInstance(void)
{
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::
init(
    MUINT32 sensorIdx)
{
    Mutex::Autolock lock(mLock);
    LOG_INF("+, usercnt(%d),srIdxNum(%d)",mUser,mSensorIdxList.size());
    MINT32 ret = 0;
    //[1] record sensor index if user power on sensor
    if(mSensorIdxList.size()<2)
    {
        MBOOL match=false;
        for (vector<MUINT32>::iterator it = mSensorIdxList.begin(); it != mSensorIdxList.end();)
        {
            if((*it)==sensorIdx)
            {
                match=true;
                break;
            }
            it++;
        }
        if(!match)
        {
            mSensorIdxList.push_back(sensorIdx);
        }
    }
    mremainUninitIdx=0;
    //[2] judge user powen on two sensors or not

    if(mSensorIdxList.size() ==1)
    {
        //create sensor related object to get/set sensor related information
        mHalSensorList=IHalSensorList::get();
        MUINT const pArrayOfIndex[1]={mSensorIdxList[0]};
        mHalSensor=mHalSensorList->createSensor("HWSyncDrc", 1, &pArrayOfIndex[0]); //get sensor handler
        //create isp driver object to use waitIrq
        if(gpispDrv==NULL)
        {
            gpispDrv = IspDrv::createInstance();
            if (!gpispDrv)
            {
                LOG_ERR("IspDrv::createInstance fail \n");
                return -3;
            }
            ret = gpispDrv->init("HWsyncDrv");
            if ( ret<=0 )
            {
                LOG_ERR("gpispDrv->init() fail \n");
                return -4;
            }
        }

        //[3] enable n3d related register
        //n3dEn=1, i2c1_en=1, i2c2_en=1, mode=0, dbg_sel default=3 (cnt1 inner reg)
        MUINT32 regValue=0x746;
        gpispDrv->writeReg(0x80c0,regValue,ISP_DRV_USER_ISPF);
    }
    else if(mSensorIdxList.size() ==2)
    {
        //enable n3d related register
        //n3dEn=1, i2c1_en=1, i2c2_en=1, mode=0, dbg_sel default=0 (ddbg_sel)
        MUINT32 regValue=0x146;
        gpispDrv->writeReg(0x80c0,regValue,ISP_DRV_USER_ISPF);

        //initial state and variables
        pthread_mutex_lock(&mStateMutex);
        mState=HW_SYNC_STATE_READY2RUN;
         pthread_mutex_unlock(&mStateMutex);
        ::sem_init(&mSemHWSyncLv, 0, 0);
        ::sem_init(&mSemJumpoutSync, 0, 0);
        mWaitIrq.Clear = ISP_DRV_IRQ_CLEAR_WAIT;
        mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
        mWaitIrq.Timeout = 200; // 150 msec
        mWaitIrq.Status = CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
        mWaitIrq.UserNumber = ISP_DRV_IRQ_USER_HWSYNC;
        mWaitIrq.UserName = "hwsyncDrv";

        //force to enable vsync interrupt for 2 tgs
    ISP_WRITE_BITS(gpispDrv,CAM_CTL_INT_P1_EN,VS1_INT_EN,1,ISP_DRV_USER_ISPF);
    ISP_WRITE_BITS(gpispDrv,CAM_CTL_INT_P1_EN_D,VS1_INT_EN,1,ISP_DRV_USER_ISPF);

        //create exection thread
        createThread();
    }
    //
    android_atomic_inc(&mUser);
    //
    LOG_INF("-, usercnt(%d),srIdxNum(%d)",mUser,mSensorIdxList.size());
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::
uninit(
    MUINT32 sensorIdx)
{
    Mutex::Autolock lock(mLock);
    LOG_INF("+, usercnt(%d),srIdxNum(%d)",mUser,mSensorIdxList.size());
    MINT32 ret = 0;

    if(mUser<=0)
    {
        LOG_ERR("strange flow mUser(%d)",mUser);
        return -1;
    }
    //
    android_atomic_dec(&mUser);

    //[1] remove sensor index if user power off sensor
    MBOOL match=false;
    int uninitIdx=0;
    for (vector<MUINT32>::iterator it = mSensorIdxList.begin(); it != mSensorIdxList.end();)
    {
        if((*it)==sensorIdx)
        {
            match=true;
            if((uninitIdx==0) && mSensorIdxList.size()==2)
        {
            mremainUninitIdx=1;
        }
            it = mSensorIdxList.erase(it);
            break;
        }
        it++;
        uninitIdx++;
    }
    if(!match)
    {
        LOG_ERR("uninit fail, wrong sensorIdx(%d)",sensorIdx);
        return -1;
    }

        //[3]when used sensor number from 2 to 1, disable hwsync driver function and halsensor/isp_drv object
    if(mSensorIdxList.size() ==0)
    {
        uninitIdx=uninitIdx+mremainUninitIdx;   //last one for uninit needed to be shift
        //disable n3d related register
            MUINT32 regValue=0x0;
            if(gpispDrv)
            {
                gpispDrv->writeReg(0x80c0,regValue,ISP_DRV_USER_ISPF);
            }
            //sensor
            mHalSensorList=NULL;
            mHalSensor=NULL;
            //uninit isp driver
            if(gpispDrv)
        {
            ret = gpispDrv->uninit("HWsyncDrv");
            if ( ret<=0 )
            {
                LOG_ERR("gpispDrv->init() fail \n");
                return -4;
            }
            gpispDrv->destroyInstance();
                gpispDrv=NULL;
            }
    }
    else if(mSensorIdxList.size() ==1)
    {
        //leave state
        pthread_mutex_lock(&mStateMutex);
            mState=HW_SYNC_STATE_READY2LEAVE;
            pthread_mutex_unlock(&mStateMutex);
            //destroy thread and leave
            destroyThread();
            //wait leave signal
            ::sem_wait(&mSemHWSyncLv);
     }

    //[4]remove sensor device info if user power off sensor(need after destroyThread cuz function "revertSenssorFps need senDev info")
    if(uninitIdx<=1)
    {
        LOG_INF("idx(%d,remain_%d),tg(%d)",uninitIdx,mremainUninitIdx,mSensorTgArray[uninitIdx]);
        switch(mSensorTgArray[uninitIdx])
        {
            case CAM_TG_1:
                mSensorDevArray[0]=SENSOR_DEV_NONE;
                mSensorCurFpsArray[0]=0;
                mSensorModeToggle[0]=0;
                break;
            case CAM_TG_2:
                mSensorDevArray[1]=SENSOR_DEV_NONE;
                mSensorCurFpsArray[1]=0;
                mSensorModeToggle[1]=0;
                break;
            default:
                LOG_WRN("wtginf, sindex(0x%x),tg(0x%x)",mSensorIdxList[uninitIdx],mSensorTgArray[uninitIdx]);
                break;
        }
        pthread_mutex_lock(&mWaitRevertMutex);
        mNeedwaitRevertFps=false;
        pthread_mutex_unlock(&mWaitRevertMutex);
    }
    else
    {
        LOG_ERR("wrong uninitIdx(%d) in mSensorIdxList\n",uninitIdx);
        return -2;
    }

    //
    LOG_INF("-, usercnt(%d),srIdxNum(%d)",mUser,mSensorIdxList.size());
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::
sendCommand(
    HW_SYNC_CMD_ENUM cmd,
    MUINT32 senDev,
    MUINT32 senScen,
    MUINT32 currfps)
{
    LOG_INF("+");
    MINT32 ret=0;
    //[1]get mapping relationship between tg and sensro device cuz we only could know tg info after user config sensor,
    //   then sensor would call sendCommand of hwysnc after config two sensors
    MUINT32 localsenDev=SENSOR_DEV_NONE;
    MUINT32 senTg=CAM_TG_NONE;
    SensorDynamicInfo dynamicInfo[1];
    MINT32 fps=0; //unit: the first digit after the decimal point, ex:30fps -> 300
    LOG_INF("mSensorIdxList.size(%d)",mSensorIdxList.size());
    for(int i=0;i<mSensorIdxList.size();i++)
    {
        localsenDev=mHalSensorList->querySensorDevIdx(mSensorIdxList[i]);
        if(localsenDev==senDev)
        {   //set sensor dev if sensorHal.Control do configure
            mHalSensor->querySensorDynamicInfo(senDev,&dynamicInfo[0]);
            senTg=dynamicInfo[0].TgInfo;
            mSensorTgArray[i]=senTg;
            switch(senTg)
            {
                case CAM_TG_1:
                    mSensorDevArray[0]=senDev;
                    mSensorScenArray[0]=senScen;
                    mSensorModeToggle[0]=1-mSensorModeToggle[0];    //change mode setting
                    #if 0
                    if(currfps==0)
                    {   //default frame rate
                ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_GET_DEFAULT_FRAME_RATE_BY_SCENARIO,(MINTPTR)&senScen,(MINTPTR)&fps,0);
                if(ret<0)
                {
                    LOG_ERR("get default fps failed\n");
                    return -1;
                }
                mSensorUserFpsArray[0]=fps;
                    }
                    else
                    {
                        mSensorUserFpsArray[0]=currfps;
                    }
                    #else
                    mSensorCurFpsArray[0]=calCurrentFps(0);
                    #endif
                    LOG_INF("dev(0x%x),scen(%d) cFps(%d), userFps(%d)",mSensorDevArray[0],mSensorScenArray[0],mSensorCurFpsArray[0],currfps);
                    break;
                case CAM_TG_2:
                    mSensorDevArray[1]=senDev;
                    mSensorScenArray[1]=senScen;
                    mSensorModeToggle[1]=1-mSensorModeToggle[1];
                    #if 0
                    if(currfps==0)
                    {   //default frame rate
                ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_GET_DEFAULT_FRAME_RATE_BY_SCENARIO,(MINTPTR)&senScen,(MINTPTR)&fps,0);
                if(ret<0)
                {
                    LOG_ERR("get default fps failed\n");
                    return -2;
                }
                mSensorUserFpsArray[0]=fps;
                    }
                    else
                    {
                        mSensorUserFpsArray[1]=currfps;
                     }
                     #else
                     mSensorCurFpsArray[1]=calCurrentFps(1);
                     #endif
                    LOG_INF("dev(0x%x),scen(%d) cFps(%d), userFps(%d)",mSensorDevArray[1],mSensorScenArray[1],mSensorCurFpsArray[1],currfps);
                    break;
                default:
                    LOG_ERR("wrong tg information, sensor index(0x%x),dev(0x%x),tg(0x%x)",mSensorIdxList[i],localsenDev,senTg);
                    return -3;
                    break;
            }
        }
    }
    LOG_INF("cursenSce(0x%x), tg_dev info (0x%x/0x%x)",senScen,mSensorDevArray[0],mSensorDevArray[1]);

    //[2] only do hwsync in stereo cam feature
    if(mSensorIdxList.size() < 2)
    {
        LOG_INF("no need hwsync,size(%d)",mSensorIdxList.size());
        return 0;
    }


    if((mSensorDevArray[0] | mSensorDevArray[1])!=(SENSOR_DEV_MAIN | SENSOR_DEV_MAIN_2))
    {
        LOG_WRN("DO NOT SUPPORT HW SYNC in curFeature dev(0x%x/0x%x)",mSensorDevArray[0],mSensorDevArray[1]);
    }
    else
    {
        //[3]change mode only when two sensor are configured
        if((mSensorDevArray[0] != SENSOR_DEV_NONE) && (mSensorDevArray[1] != SENSOR_DEV_NONE))
        {
            //do not adopt hwsync
            //1. if fps difference between two sensors are too large (>5), ignore this curz user may send different fps in capture(ex. 24 & 30)
            //2. in change mode stage use fps sent by user to judge there is in switch sensor mode case
            if((mSensorModeToggle[0]!=mSensorModeToggle[1]) /*||
                ((mSensorCurFpsArray[0] - mSensorCurFpsArray[1]) >= 50 || (mSensorCurFpsArray[0] - mSensorCurFpsArray[1]) <= -50 )*/)
            {
                LOG_WRN("do not do hwsync tog(%d/%d) dev(0x%x/0x%x), fps(%d/%d)",mSensorModeToggle[0],mSensorModeToggle[1],mSensorDevArray[0],\
                    mSensorDevArray[1],mSensorCurFpsArray[0],mSensorCurFpsArray[1]);
                //stop current sync procedure and clear all the previous cmd due to user would change sensor mode while running without poweroff then power on
                pthread_mutex_lock(&mStateMutex);
                mState=HW_SYNC_STATE_MODESWITCH;
                mCheckCnt=0;
                pthread_mutex_unlock(&mStateMutex);
                clearExecCmds();
                //revert the fps to avoid the situation that
                //1. user set fps 24 to sensor1
                //2. sync is running and adjust sensor1, may adjust fps to 15
                //3. the following operations are use sensor1 information (this time, the information may wrong, we have to revert to initial fps)
                //    wait to revert in change mode function(we do revert in thread to pipeline execute)
                return -4;
            }

            //do not adopt hwsync if scenario of two sensors are different
            if(mSensorScenArray[0] != mSensorScenArray[1])
            {
                LOG_WRN("different Scenario for two sensors (0x%x/0x%x)",mSensorScenArray[0],mSensorScenArray[1]);
                //return -5; //mark cuz user would send different sensor scenario to let fps of two sensor are close
            }

            switch(cmd)
            {
                case HW_SYNC_CMD_SET_MODE:
                    changeMode();
                    break;
                default:
                    break;
            }
        }
    }
    LOG_INF("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::
changeMode()
{
    LOG_INF("+");
    MINT32 ret=0;
    pthread_mutex_lock(&mWaitRevertMutex);
    LOG_INF("mNeedwaitRevertFps (%d)",mNeedwaitRevertFps);
    if(mNeedwaitRevertFps)
    {
        pthread_mutex_unlock(&mWaitRevertMutex);
        ::sem_wait(&mSemJumpoutSync);
    }
    else
    {
        pthread_mutex_unlock(&mWaitRevertMutex);
    }

    //[1]get sensor initial information
    MUINT32 periodPara=0x0;
    MINT32 pclk=0;
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_GET_PIXEL_CLOCK_FREQ,(MINTPTR)&pclk,0,0);
    if(ret<0)
    {
        LOG_INF("get pclk of sensorDev(0x%x) failed\n",mSensorDevArray[0]);
        return -1;
    }
    mInitPclk[0]=pclk;
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MINTPTR)&periodPara,0,0);
    if(ret<0)
    {
        LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[0]);
    }
    mInitperiodSr1[0]=0x0000FFFF & periodPara;
    mInitperiodSr1[1]=(0xFFFF0000 & periodPara)>>16;
    periodPara=0x0;
    pclk=0;
    ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_GET_PIXEL_CLOCK_FREQ,(MINTPTR)&pclk,0,0);
    if(ret<0)
    {
        LOG_INF("get pclk of sensorDev(0x%x) failed\n",mSensorDevArray[0]);
        return -1;
    }
    mInitPclk[1]=pclk;
    ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MINTPTR)&periodPara,0,0);
    if(ret<0)
    {
        LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[1]);
    }
    mInitperiodSr2[0]=0x0000FFFF & periodPara;
    mInitperiodSr2[1]=(0xFFFF0000 & periodPara)>>16;
    LOG_INF("dev_0x%x/dev_0x%x pclk(%d/%d),period(%d,%d/%d,%d)",mSensorDevArray[0],mSensorDevArray[1],mInitPclk[0],mInitPclk[1],\
        mInitperiodSr1[0],mInitperiodSr1[1],mInitperiodSr2[0],mInitperiodSr2[1]);

    //In initial situation, set the faster one as the sensor we want to adjust
    MINT32 fpsINus1=0,fpsINus2=0;
    fpsINus1=(mInitperiodSr1[0]*(mInitperiodSr1[1]))*100/(mInitPclk[0]/10000);
    fpsINus2=(mInitperiodSr2[0]*(mInitperiodSr2[1]))*100/(mInitPclk[1]/10000);
    if(fpsINus1<=fpsINus2)
    {
        mAdjIdx=0;
    }
    else
    {

        mAdjIdx=1;
    }
    LOG_INF("fps(%d/%d us),adjSrIdx(%d)",fpsINus1,fpsINus2,mAdjIdx);
    m1stSync=true;

    //[3] do i2c sync when change mode
    //Estimate sensor time interval
    #if 0
    char value[32] = {'\0'};
    property_get("camera.hwsync_esT.enable", value, "0");
    int32_t estimate_sensorT_enable = atoi(value);
    if(estimate_sensorT_enable)
    {
        //estimateSensorT();
    }
    ret=doI2CSync();
    #else   //use general one-time sync when changemode
    ret=doOneTimeSync();
    m1stSync=false;
    #endif

    //[4] add the first cmd to start the sync thread
    pthread_mutex_lock(&mStateMutex);
    mState=HW_SYNC_STATE_READY2RUN;
    pthread_mutex_unlock(&mStateMutex);
    mCheckCnt=0;
    ExecCmd cmd=ExecCmd_CHECKDIFF;
    addExecCmd(cmd);
    mNeedwaitRevertFps=true;

    LOG_INF("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MBOOL HWSyncDrvImp::checkNeedSync()
{
    LOG_INF("+,%d",mCheckCnt);
    //check receive leave msg or not, cuz user would change sensor mode and call hwsync send command while sync thread running
    pthread_mutex_lock(&mStateMutex);
    if( mState==HW_SYNC_STATE_MODESWITCH)
    {
        pthread_mutex_unlock(&mStateMutex);
        LOG_INF("receive modeswitch when check");
        revertSensorFps();
        ::sem_post(&mSemJumpoutSync);
        //avoid change mode is called after thread post semaphore
        pthread_mutex_lock(&mWaitRevertMutex);
        mNeedwaitRevertFps=false;
        pthread_mutex_unlock(&mWaitRevertMutex);
        return MTRUE;
    }
    pthread_mutex_unlock(&mStateMutex);

    MINT32 ret=0;
    //[1] wait VD
    mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
    mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }
    mCheckCnt++;
    //[2] check diff, 10 frame once
    SensorN3dDiffCnt mN3DregInfo;
    MUINT32 order=0x0;
    MUINT32 n3dDiffCnt=0x0;
    switch(mCheckCnt)
    {
        case 1:
            ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MINTPTR)&mN3DregInfo,0,0);
            if(ret<0)
            {
                LOG_ERR("query N3D diff fail \n");
                return 0;
            }
            order=(mN3DregInfo.u4DiffCnt & 0x80000000)>>31;
            if(order==1)
            {//vsync1 leads vsync2
                n3dDiffCnt=(mN3DregInfo.u4Vsync2Cnt-mN3DregInfo.u4DebugPort);
            }
            else if(order==0)
            {//vsync2 leads vsync1
                n3dDiffCnt=mN3DregInfo.u4DebugPort;
            }
            LOG_INF("ord(0x%x), Diff Cnt(%d us), cnt1(%d us), cnt2(%d us),dbgP(%d us)\n",order,(n3dDiffCnt*10)/(SMI_CLOCK/100000),\
                (mN3DregInfo.u4Vsync1Cnt*10)/(SMI_CLOCK/100000),(mN3DregInfo.u4Vsync2Cnt*10)/(SMI_CLOCK/100000),(mN3DregInfo.u4DebugPort*10)/(SMI_CLOCK/100000));
        #if 1   //debug
            {
                MUINT32 periodPara=0x0;
            MINT32 pclk=0;
            MUINT32 period[2];
            for(int i=0;i<2;i++)
            {
                ret=mHalSensor->sendCommand(mSensorDevArray[i],SENSOR_CMD_GET_PIXEL_CLOCK_FREQ,(MUINTPTR)&pclk,0,0);
                if(ret<0)
                {
                    LOG_INF("get pclk of sensorDev(0x%x) failed\n",mSensorDevArray[i]);
                    return -1;
                }
                ret=mHalSensor->sendCommand(mSensorDevArray[i],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MUINTPTR)&periodPara,0,0);
                if(ret<0)
                {
                    LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[i]);
                }
                period[0]=0x0000FFFF & periodPara;
                period[1]=(0xFFFF0000 & periodPara)>>16;
                LOG_INF("period (%d/%d)",period[0],period[1]);
            }
            }
            #endif
            if(n3dDiffCnt>DIFF_THRECNT)
            {
                ret=1;
            }
            break;
        case 10:
            mCheckCnt=0;
            break;
        default:
            break;
    }
    //[3] add next cmd and return
    ExecCmd nextCmd=ExecCmd_CHECKDIFF;
    pthread_mutex_lock(&mStateMutex);
    if( ret && (mState==HW_SYNC_STATE_READY2RUN))
    {
        pthread_mutex_unlock(&mStateMutex);
        nextCmd=ExecCmd_DOSYNC;
        pthread_mutex_lock(&mWaitRevertMutex);
        mNeedwaitRevertFps=true;
        pthread_mutex_unlock(&mWaitRevertMutex);
        LOG_INF("Need Sync");
    }
    else
    {
        pthread_mutex_unlock(&mStateMutex);
        nextCmd=ExecCmd_CHECKDIFF;
    }
    addExecCmd(nextCmd);
    LOG_INF("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::getSensorInfo(MUINT32 senDev,MINT32* pclk,MINT32* periodInfo,MINT32* dummyInfo,MINT32 curfps)
{
    LOG_INF("+");
    MINT32 ret=0;
    //[1] get pixel clock frequency
    ret=mHalSensor->sendCommand(senDev,SENSOR_CMD_GET_PIXEL_CLOCK_FREQ,(MINTPTR)&pclk[0],0,0);
    if(ret<0)
    {
        LOG_INF("get pclk of sensorDev(0x%x) failed\n",senDev);
        return -1;
    }
    LOG_INF("pclk=(%d)",pclk[0]);

    //[2] we get the initial sensor period info in change mode, cuz some sensor driver would change the period info(global variable) everytime we set dummy or adjust fps
    switch(senDev)
    {
        case SENSOR_DEV_MAIN:
        periodInfo[0]=mInitperiodSr1[0];
        periodInfo[1]=mInitperiodSr1[1];
            break;
        case SENSOR_DEV_MAIN_2:
        if((WORK_AROUND_SENSOR_FPS_SETTING) && (((mSensorCurFpsArray[0]-mSensorCurFpsArray[1])>30)||((mSensorCurFpsArray[1]-mSensorCurFpsArray[0])>30)) )
        {
            MUINT32 periodPara=0x0;
            ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MUINTPTR)&periodPara,0,0);
            if(ret<0)
            {
            LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[1]);
            }
            periodInfo[0]=0x0000FFFF & periodPara;
            periodInfo[1]=(0xFFFF0000 & periodPara)>>16;
        }
        else
            {
            periodInfo[0]=mInitperiodSr2[0];
            periodInfo[1]=mInitperiodSr2[1];
        }
            break;
        case SENSOR_DEV_SUB:
        default:
            LOG_ERR("wrong senDev(%d)",senDev);
            return -2;
            break;
    }

    MINT32 tmppclk=0;
    //[3] get previous dummy information
    tmppclk=pclk[0]/10000; //change unit base to 10us for calculating
    if((periodInfo[0]+dummyInfo[0])==0 || curfps==0 || mfpsUB==0 || mfpsLB==0)
    {
        LOG_ERR("Some denominator is zero,(ped+dmy)[0x%x],cfps(%d),fpsUB(%d),fpsLB(%d)\n",\
            (periodInfo[0]+dummyInfo[0]),curfps,mfpsUB,mfpsLB);
        return -2;
    }
    dummyInfo[0]=0;
    dummyInfo[1]=(tmppclk*10000)/(periodInfo[0]+dummyInfo[0])/curfps;   //curfps UNIT:frame/sec
    dummyInfo[1]=dummyInfo[1]-periodInfo[1];
    LOG_DBG("dmy[1](0x%x),clk(%d),fps(%d),prd[0](0x%x),[1](0x%x)",dummyInfo[1],tmppclk,curfps,periodInfo[0],periodInfo[1]);
    if((MINT32)dummyInfo[1]<0)
    {
        dummyInfo[1]=0x0;
    }

    //[4] set dummy line upper/lower bound
    if(m1stSync)
    {
        mAdjSrInfo.dummyLUB=0xFFFF;
        mAdjSrInfo.dummyLLB=0x0;
    }
    else
    {//we take care about fps during sync flow, note that unit of m_fpsUpperB and m_fpsLowerB is the third after decimal point
        mAdjSrInfo.dummyLUB=(tmppclk*10000)/(periodInfo[0]+dummyInfo[0])*1000/mfpsLB;
        mAdjSrInfo.dummyLUB=mAdjSrInfo.dummyLUB-periodInfo[1];
        if((MINT32)mAdjSrInfo.dummyLUB<0)
        {
            mAdjSrInfo.dummyLUB=0x0;
        }
        mAdjSrInfo.dummyLLB=(tmppclk*10000)/(periodInfo[0]+dummyInfo[0])*1000/mfpsUB;
        mAdjSrInfo.dummyLLB=mAdjSrInfo.dummyLLB-periodInfo[1];
        if((MINT32)mAdjSrInfo.dummyLLB<0)
        {
            mAdjSrInfo.dummyLLB=0x0;
        }
    }
    LOG_INF("1stSync(%d), Dummy cur(0x%x),uB(0x%x),lB(0x%x),fps(%d)\n",m1stSync,dummyInfo[1],mAdjSrInfo.dummyLUB,mAdjSrInfo.dummyLLB,curfps);
    LOG_INF("-");
    return ret;
}
/*******************************************************************************
*
********************************************************************************/
MBOOL HWSyncDrvImp::checkNeedChangeSr()
{
    LOG_INF("+");
    MINT32 ret=0;
    //[1] get the current situation
    //ret=getSensorInfo((MINT32*)&tmpclk[0],(MINT32*)&mAdjSrInfo.period[0],(MINT32*)&mAdjSrInfo.dummyInfo[0],mCurDftFps);

    //[2]


    LOG_INF("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::revertSensorFps()
{
    LOG_INF("+,mAdjIdx(%d)",mAdjIdx);
    MINT32 ret=0;

    MUINT32 periodPara=0x0;
    MUINT32 period[2];
    ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MINTPTR)&periodPara,0,0);
    if(ret<0)
    {
        LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[mAdjIdx]);
    }
    period[0]=0x0000FFFF & periodPara;
    period[1]=(0xFFFF0000 & periodPara)>>16;
    LOG_INF("period before (%d,%d)",period[0],period[1]);

    //[1]revert fps
    MINT32 fps_ori=0;
    fps_ori=(mAdjSrInfo.pclk*10000)/(mAdjSrInfo.period[0])*1000/(mAdjSrInfo.period[1]);
    fps_ori=fps_ori/100;    //the first digit after the decimal point
    LOG_INF("fps_ori(%d),adj_period(%d,%d),period(1_ %d,%d/2_ %d,%d)",fps_ori,mAdjSrInfo.period[0],mAdjSrInfo.period[1],\
        mInitperiodSr1[0],mInitperiodSr1[1],mInitperiodSr2[0],mInitperiodSr2[1]);
    ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_SET_MAX_FRAME_RATE_BY_SCENARIO,(MINTPTR)&(mSensorScenArray[mAdjIdx]),(MINTPTR)&fps_ori,0);
    if(ret<0)
    {
        LOG_ERR("set default frame rate to sensor failed\n");
        return -1;
    }

    MUINT32 a,b;
    a=getUs();

    //wait fps stable
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }

    b=getUs();
    LOG_DBG("===== ret/period(%d/%d us) =====",ret,b-a);
    ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MINTPTR)&periodPara,0,0);
    if(ret<0)
    {
        LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[mAdjIdx]);
    }
    period[0]=0x0000FFFF & periodPara;
    period[1]=(0xFFFF0000 & periodPara)>>16;
    LOG_INF("period after (%d,%d)",period[0],period[1]);

    LOG_DBG("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::calCurrentFps(int index)
{
    //[1]get sensor information
    int ret=0;
    MUINT32 periodPara=0x0;
    MINT32 pclk=0;
    MUINT32 period[2];
    ret=mHalSensor->sendCommand(mSensorDevArray[index],SENSOR_CMD_GET_PIXEL_CLOCK_FREQ,(MUINTPTR)&pclk,0,0);
    if(ret<0)
    {
        LOG_INF("get pclk of sensorDev(0x%x) failed\n",mSensorDevArray[index]);
        return -1;
    }
    ret=mHalSensor->sendCommand(mSensorDevArray[index],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MUINTPTR)&periodPara,0,0);
    if(ret<0)
    {
        LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[index]);
    }
    period[0]=0x0000FFFF & periodPara;
    period[1]=(0xFFFF0000 & periodPara)>>16;

    //[2] calculate fps
    MINT32 fps=0;
        fps=pclk*10/period[0]/period[1];
        if(fps>290 && fps <310)
        {
            fps=300;
        }
        else if(fps>230 && fps <250)
        {
            fps=240;
        }
        else if(fps>140 && fps <160)
        {
            fps=150;
        }

    LOG_INF("idx(%d) pclk(%d),period(%d,%d), fps(%d)",index,pclk,period[0],period[1],fps);

    return fps;
}


/*******************************************************************************
* return fps, unit is the first digit after the decimal point, ex:30fps -> 300, 29.5 -> 300
********************************************************************************/
MINT32 HWSyncDrvImp::doI2CSync()
{
    LOG_INF("+");
    MINT32 ret=1;
    MUINT32 arg1=0x1;
    //[1] set disable streaming bit to sensors
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_SET_N3D_STOP_STREAMING,arg1,0,0);
    if(ret<0)
    {
        LOG_ERR("disable streaming bit for dev(%d) fail\n",mSensorDevArray[0]);
        return -1;
    }
    ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_SET_N3D_STOP_STREAMING,arg1,0,0);
    if(ret<0)
    {
        LOG_ERR("disable streaming bit for dev(%d) fail\n",mSensorDevArray[1]);
        return -2;
    }

    //[2] set batch i2c related information
    MINT32 sensorT1=0;
    MINT32 sensorT2=0;
    MUINT32 n3dPos=0x0;
    //LOG_INF("customN3D, sensorT1(%d)us,sensorT2(%d)us\n",sensorT1,sensorT2);
    SensorN3dConfig senN3Dconf;
    if(sensorT1>sensorT2)
    {//trigger vs1 before vs2
        senN3Dconf.u4N3dMode=0x2;
        n3dPos=(sensorT1-sensorT2)*(SMI_CLOCK/10000)/100;
    }
    else if(sensorT1<sensorT2)
    {//trigger vs1 after vs2
        senN3Dconf.u4N3dMode=0x3;
        n3dPos=(sensorT1-sensorT2)*(SMI_CLOCK/10000)/100;
    }
    else
    {
        senN3Dconf.u4N3dMode=0x0;
    }
    LOG_INF("mode(0x%x), sensorT1(%d)us,sensorT2(%d)us, pos(0x%x) \n",senN3Dconf.u4N3dMode,sensorT1,sensorT2,n3dPos);
    senN3Dconf.u4N3dEn=0x1;
    senN3Dconf.u4I2C1En=0x1;
    senN3Dconf.u4I2C2En=0x1;
    senN3Dconf.u4DiffCntEn=0x1;
    senN3Dconf.u4DiffCntThr=0x0;
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_SET_N3D_CONFIG,(MINTPTR)(&senN3Dconf),0,0);
    if(ret<0)
    {
        LOG_ERR("enable n3d fail \n");
        return -3;
    }
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_SET_N3D_I2C_POS,(MINTPTR)(&n3dPos),0,0);
    if(ret<0)
    {
        LOG_ERR("setN3DI2CPosition fail\n");
        return -4;
    }
    //
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_SET_N3D_I2C_STREAM_REGDATA,arg1,0,0);
    if(ret<0)
    {
        LOG_ERR("setDatatoI2CBuffer for dev(%d) fail\n",mSensorDevArray[0]);
        return -5;
    }
    ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_SET_N3D_I2C_STREAM_REGDATA,arg1,0,0);
    if(ret<0)
    {
        LOG_ERR("setDatatoI2CBuffer for dev(%d) fail\n",mSensorDevArray[1]);
        return -6;
    }
    usleep(1000);

    //[3] trigger i2c
    MUINT32 n3dI2CTrig1=0x0;
    MUINT32 n3dI2CTrig2=0x0;
    if(senN3Dconf.u4N3dMode==0x0)
    {
        n3dI2CTrig1=0x1;
        n3dI2CTrig2=0x1;
    }
    else
    {
        n3dI2CTrig1=0x1;
        n3dI2CTrig2=0x0;
    }
    ret=mHalSensor->sendCommand(mSensorDevArray[0],SENSOR_CMD_SET_N3D_I2C_TRIGGER,(MINTPTR)(&n3dI2CTrig1),(MINTPTR)(&n3dI2CTrig2),0);
    if(ret<0)
    {
        LOG_ERR("setN3DTrigg fail\n");
        return -7;
    }

    //[4] release trigger lock and wait for finish, ask help from JH to add interface in halsensor
    //ret=mHalSensor->releaseI2CTrigLock();
    //if(ret<0)
    //{
    //  LOG_ERR("releaseI2CTrigLock fail\n");
    //    return -8;
    //}

    LOG_INF("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::doOneTimeSync()
{
    LOG_INF("+");
    MINT32 ret=1;
    MINT32 tmpAdjSrIdx=0;
    MINT32 fpsINus[2]={0,0};//unit is time
    fpsINus[0]=(mInitperiodSr1[0]*(mInitperiodSr1[1]))*100/(mInitPclk[0]/10000);
    fpsINus[1]=(mInitperiodSr2[0]*(mInitperiodSr2[1]))*100/(mInitPclk[1]/10000);
    LOG_INF("fpsINus (%d/%d)",fpsINus[0],fpsINus[1]);

    ////////////////////////////////////////////////////
    if((WORK_AROUND_SENSOR_FPS_SETTING) && (((mSensorCurFpsArray[0]-mSensorCurFpsArray[1])>30)||((mSensorCurFpsArray[1]-mSensorCurFpsArray[0])>30)) )
    {
        //work around for the situation that fps of two sensors are different in a specific mode
        //ex, force fps of two sensors are the same(lower one)
        if((mSensorCurFpsArray[0]-mSensorCurFpsArray[1])>30)
        {
            tmpAdjSrIdx=0;
            mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
                    mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
        }
        else if((mSensorCurFpsArray[1]-mSensorCurFpsArray[0])>30)
        {
            tmpAdjSrIdx=1;
            mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST_D;
                    mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_D_VS1_INT_ST;
        }


        int temppclk=mInitPclk[tmpAdjSrIdx]/10000;
        MUINT32 tempperiod[2];
        switch(tmpAdjSrIdx)
        {
            case 0:
            default:
                tempperiod[0]=mInitperiodSr1[0];
                tempperiod[1]=mInitperiodSr1[1];
                break;
            case 1:
                tempperiod[0]=mInitperiodSr2[0];
                tempperiod[1]=mInitperiodSr2[1];
                break;
        }
        //
        int tempdummyLNeeded=((temppclk*fpsINus[1-tmpAdjSrIdx])/tempperiod[0]/100);
        tempdummyLNeeded=tempdummyLNeeded-tempperiod[1];
        int tempfinaldummyL;
        if((int)tempdummyLNeeded<=0)
        {
            tempfinaldummyL=0;
        }
        else
        {
            tempfinaldummyL=tempdummyLNeeded;
        }
        int tempfps_final=(temppclk*10000)/(tempperiod[0])*1000/(tempperiod[1]+tempfinaldummyL);
        tempfps_final=tempfps_final/100;    //the first digit after the decimal point
        LOG_INF("[work around_setDummy] temppclk(%d),period(%d/%d)\n",temppclk,tempperiod[0],tempperiod[1]);
        LOG_INF("[work around_setDummy] final.dmy(0x%x),fps(%d)\n",tempfinaldummyL,tempfps_final);
        ret=mHalSensor->sendCommand(mSensorDevArray[tmpAdjSrIdx],SENSOR_CMD_SET_MAX_FRAME_RATE_BY_SCENARIO,(MUINTPTR)&(mSensorScenArray[mAdjIdx]),(MUINTPTR)&tempfps_final,0);
        if(ret<0)
        {
            LOG_ERR("set frame rate to sensor failed\n");
            return -1;
        }
    }
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }
    ///////////////////////////////////////////////////




    MINT32 fps_final=0;
    //(1) wait VD and get current n3d register status
    //wait hwsync related registers are stable
    mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
    mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
    for(int i=0;i<3;i++)
    {
        if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
        { LOG_ERR("wait vsync timeout\n"); }
    }
    SensorN3dDiffCnt mN3DregInfo;
    ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MINTPTR)&mN3DregInfo,0,0);
    if(ret<0)
    {
        LOG_ERR("query N3D diff fail \n");
        return -1;
    }
    mN3DInfo.order = (mN3DregInfo.u4DiffCnt & 0x80000000)>>31;  //current frame leading
    mN3DInfo.vdDiffCnt = mN3DregInfo.u4DiffCnt & 0x7fffffff;    //preframe diff
    mN3DInfo.vs_regCnt[0]=mN3DregInfo.u4Vsync1Cnt;
    mN3DInfo.vs_regCnt[1]=mN3DregInfo.u4Vsync2Cnt;
    mN3DInfo.vs2_vs1Cnt=mN3DregInfo.u4DebugPort;
    mAdjSrInfo.sw_curRegCntTus=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;


    //(2)judge sync (always adjust the leading one)
    switch(mN3DInfo.order)
    {
        case 1: //vs1 leads vs2
            mN3DInfo.vdDiffCnt=(mN3DInfo.vs_regCnt[1]-mN3DInfo.vs2_vs1Cnt);
            tmpAdjSrIdx=0;
            mAdjSrInfo.pclk=mInitPclk[0]/10000;
            mAdjSrInfo.period[0]=mInitperiodSr1[0];
            mAdjSrInfo.period[1]=mInitperiodSr1[1];
            mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
            mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
            //align sensor 2 fps
        mCurDftFps=mSensorCurFpsArray[1]/10;
        mfpsUB=mCurDftFps*1000+500; //plus 0.5s
        mfpsLB=mCurDftFps*1000-500; //minus 0.5s
        LOG_INF("senScenf(0x%x/0x%x),fps get(%d/%d),curDFT(%d),UB(%d),LB(%d)",mSensorScenArray[0],mSensorScenArray[1],\
            mSensorCurFpsArray[0],mSensorCurFpsArray[1],mCurDftFps,mfpsUB,mfpsLB);
            break;
        case 0: //vs2 leads vs1
            mN3DInfo.vdDiffCnt=mN3DInfo.vs2_vs1Cnt;
            tmpAdjSrIdx=1;
            mAdjSrInfo.pclk=mInitPclk[1]/10000;
            if((WORK_AROUND_SENSOR_FPS_SETTING) && (((mSensorCurFpsArray[0]-mSensorCurFpsArray[1])>30)||((mSensorCurFpsArray[1]-mSensorCurFpsArray[0])>30)) )
        {
            MUINT32 periodPara=0x0;
            ret=mHalSensor->sendCommand(mSensorDevArray[1],SENSOR_CMD_GET_FRAME_SYNC_PIXEL_LINE_NUM,(MUINTPTR)&periodPara,0,0);
        if(ret<0)
        {
            LOG_ERR("get initial period information of sensorDev(0x%x) failed\n",mSensorDevArray[1]);
        }
        mAdjSrInfo.period[0]=0x0000FFFF & periodPara;
        mAdjSrInfo.period[1]=(0xFFFF0000 & periodPara)>>16;
        }
        else
        {
                mAdjSrInfo.period[0]=mInitperiodSr2[0];
                mAdjSrInfo.period[1]=mInitperiodSr2[1];
            }
            mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST_D;
            mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_D_VS1_INT_ST;
            //align sensor 1 fps
        mCurDftFps=mSensorCurFpsArray[1]/10;
        mfpsUB=mCurDftFps*1000+500; //plus 0.5s
        mfpsLB=mCurDftFps*1000-500; //minus 0.5s
        LOG_INF("senScenf(0x%x/0x%x),fps get(%d/%d),curDFT(%d),UB(%d),LB(%d)",mSensorScenArray[0],mSensorScenArray[1],\
            mSensorCurFpsArray[0],mSensorCurFpsArray[1],mCurDftFps,mfpsUB,mfpsLB);
            break;
        default:
            break;
    }
    mN3DInfo.vdDiffTus=(mN3DInfo.vdDiffCnt*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[0]=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[1]=(mN3DInfo.vs_regCnt[1]*10)/REF_CLOCK;
    mN3DInfo.vs2_vs1Tus=(mN3DInfo.vs2_vs1Cnt*10)/REF_CLOCK;
    MINT32 curdiffT=mN3DInfo.vdDiffCnt*10/REF_CLOCK;    //us unit
    LOG_INF("Ord/adj(%d/%d),vs1_T(0x%x)(%d us), vs2_T(0x%x)(%d us),hwdebugPort(0x%x)(%d us), curVSDiff(%d us)",\
        mN3DInfo.order,tmpAdjSrIdx,mN3DInfo.vs_regCnt[0],mN3DInfo.vs_regTus[0],mN3DInfo.vs_regCnt[1],mN3DInfo.vs_regTus[1],\
        mN3DInfo.vs2_vs1Cnt,mN3DInfo.vs2_vs1Tus,mN3DInfo.vdDiffTus);

    int fpsdifINus=0;
    if( fpsINus[0]> fpsINus[1])
    {
         fpsdifINus=fpsINus[0]- fpsINus[1];
    }
    else
    {
        fpsdifINus=fpsINus[1]- fpsINus[0];
    }
    if((curdiffT<500) && (fpsdifINus<2000))
    {
        LOG_INF("diff is small, return");
        return 1;
    }


    //(3)sync
    MUINT32 nextDiffCnt=0x0;
    MINT32 nextDiffTus=0;
    if((int)mN3DInfo.vs_regCnt[1-tmpAdjSrIdx] > (int)mN3DInfo.vs_regCnt[tmpAdjSrIdx])
    {
        nextDiffCnt=mN3DInfo.vdDiffCnt+(mN3DInfo.vs_regCnt[1-tmpAdjSrIdx]-mN3DInfo.vs_regCnt[tmpAdjSrIdx]);
    }
    else
    {
        nextDiffCnt=mN3DInfo.vdDiffCnt+(mN3DInfo.vs_regCnt[tmpAdjSrIdx]-mN3DInfo.vs_regCnt[1-tmpAdjSrIdx]);
    }
    nextDiffTus=(nextDiffCnt*10)/REF_CLOCK;
    LOG_INF("nextdiff(0x%x)(%d us)\n",nextDiffCnt,nextDiffTus);
    mAdjSrInfo.dummyLNeeded=((mAdjSrInfo.pclk*(mN3DInfo.vs_regTus[1-tmpAdjSrIdx]+nextDiffTus))/mAdjSrInfo.period[0]/100);
    mAdjSrInfo.dummyLNeeded=mAdjSrInfo.dummyLNeeded-mAdjSrInfo.period[1];
    if((int)mAdjSrInfo.dummyLNeeded<=0)
    {
        mAdjSrInfo.dummyLFinal=0;
    }
    else
    {
        mAdjSrInfo.dummyLFinal=mAdjSrInfo.dummyLNeeded;
    }
    fps_final=(mAdjSrInfo.pclk*10000)/(mAdjSrInfo.period[0]+mAdjSrInfo.dummyInfo[0])*1000/(mAdjSrInfo.period[1]+mAdjSrInfo.dummyLFinal);
    fps_final=fps_final/100;    //the first digit after the decimal point
    LOG_INF("[first_setDummy] final.dmy(0x%x),fps(%d)\n",mAdjSrInfo.dummyLFinal,fps_final);
    ret=mHalSensor->sendCommand(mSensorDevArray[tmpAdjSrIdx],SENSOR_CMD_SET_MAX_FRAME_RATE_BY_SCENARIO,(MINTPTR)&(mSensorScenArray[mAdjIdx]),(MINTPTR)&fps_final,0);
    if(ret<0)
    {
        LOG_ERR("set frame rate to sensor failed\n");
        return -1;
    }

    //wait update
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }
    ret=mHalSensor->sendCommand(mSensorDevArray[tmpAdjSrIdx],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MINTPTR)&mN3DregInfo,0,0);
    if(ret<0)
    {
        LOG_ERR("query N3D diff fail \n");
        return -1;
    }
    mN3DInfo.order = (mN3DregInfo.u4DiffCnt & 0x80000000)>>31;  //current frame leading
    mN3DInfo.vdDiffCnt = mN3DregInfo.u4DiffCnt & 0x7fffffff;    //preframe diff
    mN3DInfo.vs_regCnt[0]=mN3DregInfo.u4Vsync1Cnt;
    mN3DInfo.vs_regCnt[1]=mN3DregInfo.u4Vsync2Cnt;
    mN3DInfo.vs2_vs1Cnt=mN3DregInfo.u4DebugPort;
    mAdjSrInfo.sw_curRegCntTus=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vdDiffTus=(mN3DInfo.vdDiffCnt*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[0]=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[1]=(mN3DInfo.vs_regCnt[1]*10)/REF_CLOCK;
    mN3DInfo.vs2_vs1Tus=(mN3DInfo.vs2_vs1Cnt*10)/REF_CLOCK;
    curdiffT=mN3DInfo.vdDiffCnt*10/REF_CLOCK;   //us unit
    LOG_INF("[first_setDummy] after_check Ord/adj(%d/%d),vs1_T(0x%x)(%d us), vs2_T(0x%x)(%d us),hwdebugPort(0x%x)(%d us), curVSDiff(%d us)",\
        mN3DInfo.order,tmpAdjSrIdx,mN3DInfo.vs_regCnt[0],mN3DInfo.vs_regTus[0],mN3DInfo.vs_regCnt[1],mN3DInfo.vs_regTus[1],\
        mN3DInfo.vs2_vs1Cnt,mN3DInfo.vs2_vs1Tus,mN3DInfo.vdDiffTus);

    //set the same fps with the other one sensor
    mAdjSrInfo.dummyLNeeded=((mAdjSrInfo.pclk*fpsINus[1-tmpAdjSrIdx])/mAdjSrInfo.period[0]/100);
    mAdjSrInfo.dummyLNeeded=mAdjSrInfo.dummyLNeeded-mAdjSrInfo.period[1];
    if((int)mAdjSrInfo.dummyLNeeded<=0)
    {
        mAdjSrInfo.dummyLFinal=0;
    }
    else
    {
        mAdjSrInfo.dummyLFinal=mAdjSrInfo.dummyLNeeded;
    }
    fps_final=(mAdjSrInfo.pclk*10000)/(mAdjSrInfo.period[0]+mAdjSrInfo.dummyInfo[0])*1000/(mAdjSrInfo.period[1]+mAdjSrInfo.dummyLFinal);
    fps_final=fps_final/100;    //the first digit after the decimal point
    LOG_INF("[second_setDummy] final.dmy(0x%x),fps(%d)\n",mAdjSrInfo.dummyLFinal,fps_final);
    ret=mHalSensor->sendCommand(mSensorDevArray[tmpAdjSrIdx],SENSOR_CMD_SET_MAX_FRAME_RATE_BY_SCENARIO,(MINTPTR)&(mSensorScenArray[mAdjIdx]),(MINTPTR)&fps_final,0);
    if(ret<0)
    {
        LOG_ERR("set frame rate to sensor failed\n");
        return -1;
    }

    //wait update
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }
    ret=mHalSensor->sendCommand(mSensorDevArray[tmpAdjSrIdx],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MINTPTR)&mN3DregInfo,0,0);
    if(ret<0)
    {
        LOG_ERR("query N3D diff fail \n");
        return -1;
    }
    mN3DInfo.order = (mN3DregInfo.u4DiffCnt & 0x80000000)>>31;  //current frame leading
    mN3DInfo.vdDiffCnt = mN3DregInfo.u4DiffCnt & 0x7fffffff;    //preframe diff
    mN3DInfo.vs_regCnt[0]=mN3DregInfo.u4Vsync1Cnt;
    mN3DInfo.vs_regCnt[1]=mN3DregInfo.u4Vsync2Cnt;
    mN3DInfo.vs2_vs1Cnt=mN3DregInfo.u4DebugPort;
    mAdjSrInfo.sw_curRegCntTus=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vdDiffTus=(mN3DInfo.vdDiffCnt*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[0]=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[1]=(mN3DInfo.vs_regCnt[1]*10)/REF_CLOCK;
    mN3DInfo.vs2_vs1Tus=(mN3DInfo.vs2_vs1Cnt*10)/REF_CLOCK;
    curdiffT=mN3DInfo.vdDiffCnt*10/REF_CLOCK;   //us unit
    LOG_INF("[second_setDummy] after_check Ord/adj(%d/%d),vs1_T(0x%x)(%d us), vs2_T(0x%x)(%d us),hwdebugPort(0x%x)(%d us), curVSDiff(%d us)",\
        mN3DInfo.order,tmpAdjSrIdx,mN3DInfo.vs_regCnt[0],mN3DInfo.vs_regTus[0],mN3DInfo.vs_regCnt[1],mN3DInfo.vs_regTus[1],\
        mN3DInfo.vs2_vs1Cnt,mN3DInfo.vs2_vs1Tus,mN3DInfo.vdDiffTus);

    mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
    mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }
    #if 0
    ret=mHalSensor->sendCommand(mSensorDevArray[tmpAdjSrIdx],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MUNTPTR)&mN3DregInfo,0,0);
    if(ret<0)
    {
        LOG_ERR("query N3D diff fail \n");
        return -1;
    }
    mN3DInfo.order = (mN3DregInfo.u4DiffCnt & 0x80000000)>>31;  //current frame leading
    mN3DInfo.vdDiffCnt = mN3DregInfo.u4DiffCnt & 0x7fffffff;    //preframe diff
    mN3DInfo.vs_regCnt[0]=mN3DregInfo.u4Vsync1Cnt;
    mN3DInfo.vs_regCnt[1]=mN3DregInfo.u4Vsync2Cnt;
    mN3DInfo.vs2_vs1Cnt=mN3DregInfo.u4DebugPort;
    mAdjSrInfo.sw_curRegCntTus=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vdDiffTus=(mN3DInfo.vdDiffCnt*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[0]=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
    mN3DInfo.vs_regTus[1]=(mN3DInfo.vs_regCnt[1]*10)/REF_CLOCK;
    mN3DInfo.vs2_vs1Tus=(mN3DInfo.vs2_vs1Cnt*10)/REF_CLOCK;
    curdiffT=mN3DInfo.vdDiffCnt*10/REF_CLOCK;   //us unit
    LOG_INF("[second_setDummy] after2_check Ord/adj(%d/%d),vs1_T(0x%x)(%d us), vs2_T(0x%x)(%d us),hwdebugPort(0x%x)(%d us), curVSDiff(%d us)",\
        mN3DInfo.order,tmpAdjSrIdx,mN3DInfo.vs_regCnt[0],mN3DInfo.vs_regTus[0],mN3DInfo.vs_regCnt[1],mN3DInfo.vs_regTus[1],\
        mN3DInfo.vs2_vs1Cnt,mN3DInfo.vs2_vs1Tus,mN3DInfo.vdDiffTus);
    #else
    //monitor the query information in the first time checkNeedSync
    #endif

    LOG_INF("-");
    return ret;
}

/*******************************************************************************
*
********************************************************************************/
MINT32 HWSyncDrvImp::doGeneralSync()
{
    LOG_INF("+");
    MINT32 ret=0;
    MINT32 fpsDiffTus=0;
    MINT32 caseIdx=0;
    MINT32 fps_final=0;
    MINT32 tmpclk[1]={0};
    bool pre_updateFps_not0=false;  //fps updated last frame
    bool updateFps=false;
    //[1] state is syncing
    pthread_mutex_lock(&mStateMutex);
    mState=HW_SYNC_STATE_SYNCING;
    pthread_mutex_unlock(&mStateMutex);

    //[2] judge we need to change sensor which is be adjusted (only needed when support dynamic frame rate
    //checkNeedChangeSr();

    //[3] get sensor information
    LOG_INF("adj sensorIdx(0x%x)\n",mAdjIdx);
    ret=getSensorInfo(mSensorDevArray[mAdjIdx],(MINT32*)&tmpclk[0],(MINT32*)&mAdjSrInfo.period[0],(MINT32*)&mAdjSrInfo.dummyInfo[0],mCurDftFps);
    mAdjSrInfo.pclk=tmpclk[0]/10000;    //change unit base to 100us for calculating

    //[4] initial variables
    SensorN3dDiffCnt mN3DregInfo;
    mWaitIrq.Type = ISP_DRV_IRQ_TYPE_INT_P1_ST;
    mWaitIrq.Status=CAM_CTL_INT_P1_STATUS_VS1_INT_ST;
    if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
    { LOG_ERR("wait vsync timeout\n"); }
    ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MINTPTR)&mN3DregInfo,0,0);
    if(ret<0)
    {
        LOG_ERR("query N3D diff fail \n");
        return -1;
    }
    mN3DInfo.order = (mN3DregInfo.u4DiffCnt & 0x80000000)>>31;  //current frame leading
    mN3DInfo.vdDiffCnt = mN3DregInfo.u4DiffCnt & 0x7fffffff;    //preframe diff
    mN3DInfo.vs_regCnt[0]=mN3DregInfo.u4Vsync1Cnt;
    mN3DInfo.vs_regCnt[1]=mN3DregInfo.u4Vsync2Cnt;
    mN3DInfo.vs2_vs1Cnt=mN3DregInfo.u4DebugPort;
    mAdjSrInfo.sw_curRegCntTus=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;

    //[5] do sync
    MINT32 nextDiffTus=0;
    do
    {
        // 1) check
        pthread_mutex_lock(&mStateMutex);
        if(mState==HW_SYNC_STATE_READY2LEAVE)
        {   //receive leave msg while syncing
            pthread_mutex_unlock(&mStateMutex);
            LOG_INF("receive leave msg while syncing");
            return 0;
        }
        else if(mState==HW_SYNC_STATE_MODESWITCH)
        {
            pthread_mutex_unlock(&mStateMutex);
            LOG_INF("receive modeswitch msg while syncing");
            revertSensorFps();
                    ::sem_post(&mSemJumpoutSync);
                    //avoid change mode is called after thread post semaphore
                    pthread_mutex_lock(&mWaitRevertMutex);
                    mNeedwaitRevertFps=false;
                    pthread_mutex_unlock(&mWaitRevertMutex);
            return 0;
        }
        else
        {
            pthread_mutex_unlock(&mStateMutex);
        }



        // 2) waitVD to get the updated situation
        if (gpispDrv->waitIrq(&mWaitIrq) <= 0)
        { LOG_ERR("wait vsync timeout\n"); }
        ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_GET_SENSOR_N3D_DIFFERENCE_COUNT,(MINTPTR)&mN3DregInfo,0,0);
        if(ret<0)
        {
            LOG_ERR("query N3D diff fail \n");
            return -1;
        }
        mN3DInfo.order = (mN3DregInfo.u4DiffCnt & 0x80000000)>>31;  //current frame leading
        mN3DInfo.vdDiffCnt = mN3DregInfo.u4DiffCnt & 0x7fffffff;    //preframe diff
        mN3DInfo.vs_regCnt[0]=mN3DregInfo.u4Vsync1Cnt;
        mN3DInfo.vs_regCnt[1]=mN3DregInfo.u4Vsync2Cnt;
        mN3DInfo.vs2_vs1Cnt=mN3DregInfo.u4DebugPort;

        // 3) update real diff in current frame
        switch(mN3DInfo.order)
        {
            case 1: //vs1 leads vs2
                mN3DInfo.vdDiffCnt=(mN3DInfo.vs_regCnt[1]-mN3DInfo.vs2_vs1Cnt);
                break;
            case 0: //vs2 leads vs1
                mN3DInfo.vdDiffCnt=mN3DInfo.vs2_vs1Cnt;
                break;
            default:
                break;
        }
        mN3DInfo.vdDiffTus=(mN3DInfo.vdDiffCnt*10)/REF_CLOCK;
        mN3DInfo.vs_regTus[0]=(mN3DInfo.vs_regCnt[0]*10)/REF_CLOCK;
        mN3DInfo.vs_regTus[1]=(mN3DInfo.vs_regCnt[1]*10)/REF_CLOCK;
        mN3DInfo.vs2_vs1Tus=(mN3DInfo.vs2_vs1Cnt*10)/REF_CLOCK;
        if((MINT32)mN3DInfo.vs_regCnt[mAdjIdx]>(MINT32)mN3DInfo.vs_regCnt[1-mAdjIdx])
        {   fpsDiffTus=(MINT32)mN3DInfo.vs_regCnt[mAdjIdx]-(MINT32)mN3DInfo.vs_regCnt[1-mAdjIdx];}
        else
        {   fpsDiffTus=(MINT32)mN3DInfo.vs_regCnt[1-mAdjIdx]-(MINT32)mN3DInfo.vs_regCnt[mAdjIdx];}
        fpsDiffTus=fpsDiffTus*10/REF_CLOCK;

        LOG_INF("ord(%d), vdDiff(0x%x/%d us), fpsDiff(%d us), vs1(0x%x/%d us), vs2(0x%x/%d us), dbgP(0x%x/%d us)",mN3DInfo.order,\
            mN3DInfo.vdDiffCnt,mN3DInfo.vdDiffTus,fpsDiffTus,mN3DInfo.vs_regCnt[0],mN3DInfo.vs_regTus[0],mN3DInfo.vs_regCnt[1],mN3DInfo.vs_regTus[1],\
            mN3DInfo.vs2_vs1Cnt,mN3DInfo.vs2_vs1Tus);

        // 4) judge converge or not
        if((mN3DInfo.vdDiffTus<CVGTHRE_VDDIFF) && (fpsDiffTus<CVGTHRE_FPSDIFF))
        {
            LOG_INF("Converge!\n");
            break;
        }

        // 5) gather sync information
        caseIdx=mAdjIdx - mN3DInfo.order;
        if(caseIdx!=0)
        {   //vsync1 leads vsync2
            //the time period we want to increase at next frame
            nextDiffTus=mN3DInfo.vdDiffTus-(mAdjSrInfo.sw_curRegCntTus-mN3DInfo.vs_regTus[1-mAdjIdx]);
            mAdjSrInfo.dummyLNeeded=((mAdjSrInfo.pclk*(mN3DInfo.vs_regTus[1-mAdjIdx]+nextDiffTus))/mAdjSrInfo.period[0]/100);
            mAdjSrInfo.dummyLNeeded=mAdjSrInfo.dummyLNeeded-mAdjSrInfo.period[1];
        }
        else
        {   //vsync2 leads vsync1
            //the time period we want to decrease at next frame
            nextDiffTus=mN3DInfo.vdDiffTus+(mAdjSrInfo.sw_curRegCntTus-mN3DInfo.vs_regTus[1-mAdjIdx]);
            mAdjSrInfo.dummyLNeeded=((mAdjSrInfo.pclk*(mN3DInfo.vs_regTus[1-mAdjIdx]-nextDiffTus))/mAdjSrInfo.period[0]/100);
            mAdjSrInfo.dummyLNeeded=mAdjSrInfo.dummyLNeeded-mAdjSrInfo.period[1];
        }
        if((MINT32)mAdjSrInfo.dummyLNeeded>=(MINT32)mAdjSrInfo.dummyLUB)
        {
            mAdjSrInfo.dummyLFinal=mAdjSrInfo.dummyLUB;
        }
        else if((MINT32)mAdjSrInfo.dummyLNeeded<=(MINT32)mAdjSrInfo.dummyLLB)
        {
            mAdjSrInfo.dummyLFinal=mAdjSrInfo.dummyLLB;
        }
        else
        {
            mAdjSrInfo.dummyLFinal=mAdjSrInfo.dummyLNeeded;
        }
        LOG_INF("adj(%d),swCntT(%d us),nextDiffTus(%d us),cur(%d us),dummyLNeeded(%d)",mAdjIdx,mAdjSrInfo.sw_curRegCntTus,nextDiffTus,mN3DInfo.vdDiffTus,mAdjSrInfo.dummyLNeeded);
        LOG_INF("dummy uB(0x%X),lB(0x%x),curneed(0x%x),final(0x%x)\n",mAdjSrInfo.dummyLUB,mAdjSrInfo.dummyLLB,mAdjSrInfo.dummyLNeeded,mAdjSrInfo.dummyLFinal);

        // 6) set dummy line to sync
        updateFps=true;
        if(mAdjSrInfo.dummyLFinal>0)
        {
            updateFps=true;
            pre_updateFps_not0=true;
        }
        else
        {
            if(pre_updateFps_not0)
            {
                updateFps=true;
            }
            else
            {
                updateFps=false;
                LOG_INF("dummy final_0");
            }
            pre_updateFps_not0=false;
        }

    if(updateFps)
    {
        fps_final=(mAdjSrInfo.pclk*10000)/(mAdjSrInfo.period[0]+mAdjSrInfo.dummyInfo[0])*1000/(mAdjSrInfo.period[1]+mAdjSrInfo.dummyLFinal);
            fps_final=fps_final/100;    //the first digit after the decimal point
            LOG_INF("Final pclk(%d),OriLne(%d),OriPxl(%d),dummyL(%d),fps(%d)",mAdjSrInfo.pclk*10000,mAdjSrInfo.period[1],mAdjSrInfo.period[0],\
                mAdjSrInfo.dummyLFinal,fps_final);
            ret=mHalSensor->sendCommand(mSensorDevArray[mAdjIdx],SENSOR_CMD_SET_MAX_FRAME_RATE_BY_SCENARIO,(MUINTPTR)&(mSensorScenArray[mAdjIdx]),(MUINTPTR)&fps_final,0);
            if(ret<0)
            {
                LOG_ERR("set frame rate to sensor failed\n");
                return -2;
            }
    }

        mAdjSrInfo.dummyInfo[1]=mAdjSrInfo.dummyLFinal;
        mAdjSrInfo.sw_curRegCntTus=(mAdjSrInfo.period[0]*(mAdjSrInfo.period[1]+mAdjSrInfo.dummyLFinal))*100/mAdjSrInfo.pclk;
    }while(1);
    pthread_mutex_lock(&mStateMutex);
    if(mState==HW_SYNC_STATE_SYNCING)
    {
        pthread_mutex_unlock(&mStateMutex);
        mState=HW_SYNC_STATE_READY2RUN;
    }
    pthread_mutex_unlock(&mStateMutex);
    LOG_INF("-");
    return ret;
}

