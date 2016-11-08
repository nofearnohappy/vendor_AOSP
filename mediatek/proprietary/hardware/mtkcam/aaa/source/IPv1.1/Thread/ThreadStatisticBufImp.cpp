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
#define LOG_TAG "ThreadStatisticImp"

#include <IThread.h>
#include <pthread.h>
#include <semaphore.h>
#include <utils/threads.h>
#include <utils/List.h>
#include <aaa_error_code.h>
#include <aaa_buf_mgr.h>
#include <aaa_log.h>

namespace NS3Av3
{
using namespace android;

class ThreadStatisticBufImp : public ThreadStatisticBuf
{
public:
    virtual MBOOL               destroyInstance();
    virtual MBOOL               postCmd(void* pArg);
    virtual MBOOL               waitFinished();

                                ThreadStatisticBufImp(MUINT32 u4SensorDev, MUINT32 port);
    virtual                     ~ThreadStatisticBufImp();
private:
    static MVOID*               onThreadLoop(MVOID*);

    MBOOL                       m_fgLogEn;
    MUINT32                     m_u4SensorDev;
    MBOOL                       m_fgTerminate;
    pthread_t                   m_rThread;
    sem_t                       m_SemFinish;
    Mutex                       m_ModuleMtx;
    MUINT32                     m_eSttPort;
};

ThreadStatisticBuf*
ThreadStatisticBuf::
createInstance(MUINT32 u4SensorDev, MUINT32 port)
{
    ThreadStatisticBufImp* pObj = new ThreadStatisticBufImp(u4SensorDev,port);
    return pObj;
}

MBOOL
ThreadStatisticBufImp::
destroyInstance()
{
    waitFinished();
    delete this;
    return MTRUE;
}

MBOOL
ThreadStatisticBufImp::
postCmd(void* pArg)
{
    return MTRUE;
}

MBOOL
ThreadStatisticBufImp::
waitFinished()
{
    Mutex::Autolock autoLock(m_ModuleMtx);
    MY_LOG_IF(m_fgLogEn,"[%s] +\n", __FUNCTION__);
    if(!m_fgTerminate){
        m_fgTerminate = 1;
        MY_LOG_IF(m_fgLogEn,"[%s]sem_wait : m_SemFinish \n", __FUNCTION__);
        ::sem_wait(&m_SemFinish);
        //MY_LOG_IF(m_fgLogEn,"[%s]pthread_join : m_rThread \n", __FUNCTION__);
        //::pthread_join(m_rThread, NULL);
    }
    MY_LOG_IF(m_fgLogEn,"[%s] -\n", __FUNCTION__);
    return MTRUE;
}

ThreadStatisticBufImp::
ThreadStatisticBufImp(MUINT32 u4SensorDev, MUINT32 port)
    : m_fgLogEn(1)
    , m_fgTerminate(0)
    , m_u4SensorDev(u4SensorDev)
    , m_eSttPort(port)
{
    // init something
    ::sem_init(&m_SemFinish, 0, 0);
    // create thread
    ::pthread_create(&m_rThread, NULL, onThreadLoop, this);
    MY_LOG_IF(m_fgLogEn,"[%s]m_u4SensorDev: %d, m_eSttPort: %d \n", __FUNCTION__,m_u4SensorDev, m_eSttPort);
}

ThreadStatisticBufImp::
~ThreadStatisticBufImp()
{
    MY_LOG_IF(m_fgLogEn,"[%s] +\n", __FUNCTION__);
    waitFinished();
    MY_LOG_IF(m_fgLogEn,"[%s] -\n", __FUNCTION__);
}

MVOID*
ThreadStatisticBufImp::
onThreadLoop(MVOID* pArg)
{
    ThreadStatisticBufImp* _this = reinterpret_cast<ThreadStatisticBufImp*>(pArg);
    MUINT32 u4SensorDev = _this->m_u4SensorDev;
    MUINT32 ePort = _this->m_eSttPort;

    while (!_this->m_fgTerminate)
    {
        MY_LOG_IF(_this->m_fgLogEn,"[%s]dequeueHwBuf Start : ePort(%d) \n", __FUNCTION__, ePort);
        if(ePort & DEQUE_STT_AAO)
          IAAABufMgr::getInstance().dequeueHwBuf(u4SensorDev, STT_AAO);
        if(ePort & DEQUE_STT_AFO)
          IAAABufMgr::getInstance().dequeueHwBuf(u4SensorDev, STT_AFO);
        if(ePort & DEQUE_STT_FLKO)
          IAAABufMgr::getInstance().dequeueHwBuf(u4SensorDev, STT_FLKO);
        MY_LOG_IF(_this->m_fgLogEn,"[%s]dequeueHwBuf End : ePort(%d) \n", __FUNCTION__, ePort);    
    }
    MY_LOG("[%s]sem_post : m_SemFinish \n", __FUNCTION__);
    ::sem_post(&_this->m_SemFinish);
    return NULL;
}
};
