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

#include <unistd.h>
#include "osal_utils.h"


void* MTK_OMX_ALLOC(unsigned int size) {
    return malloc(size);
}


void MTK_OMX_FREE (void* ptr) {
    free (ptr);
}


void* MTK_OMX_MEMSET (void * ptr, int value, unsigned int num) {
    return memset (ptr, value, num);
}

//2012/08/09 Bruce Hsu for IL Component OMX_AllocateBuffer implementation
void* MTK_OMX_MEMALIGN (unsigned int align, unsigned int size) {
        return memalign (align, size);
}

int get_sem_value (sem_t* sem) {
    int value = -100;
    if (0 != sem_getvalue(sem, &value)) {
        ALOGE ("sem_getvalue failed !!!");
    }
    return value;
}

const char* StateToString(OMX_U32 state) {
    switch (state) {
        case OMX_StateInvalid:
            return "Invalid";
        case OMX_StateLoaded:
            return "OMX_StateLoaded";
        case OMX_StateIdle:
            return "OMX_StateIdle";
        case OMX_StateExecuting:
            return "OMX_StateExecuting";
        case OMX_StatePause:
            return "OMX_StatePause";
        case OMX_StateWaitForResources:
            return "OMX_StateWaitForResources";
        default:
            return "Unknown";
    }
}

const char* CommandToString(OMX_U32 cmd) {
    switch (cmd) {
        case OMX_CommandStateSet:
            return "OMX_CommandStateSet";
        case OMX_CommandFlush:
            return "OMX_CommandFlush";
        case OMX_CommandPortDisable:
            return "OMX_CommandPortDisable";
        case OMX_CommandPortEnable:
            return "OMX_CommandPortEnable";
        case OMX_CommandMarkBuffer:
            return "OMX_CommandMarkBuffer";
        default:
            return "Unknown";
    }
}

int pthread_mutex_lock_timeout (pthread_mutex_t *mutex, const int millisec)
{
    int retcode = 0;
    int nWaitMilliSec = millisec;
    int nMicroSecInterval = 2;

    while ((retcode = pthread_mutex_trylock (mutex)) == EBUSY) {
         if (nWaitMilliSec == 0) {
            ALOGE("pthread_mutex_lock_timeout timeout");
            return ETIMEDOUT;
        }

        SLEEP_MS(nMicroSecInterval);
        ALOGE("nWaitMilliSec : %d", nWaitMilliSec);
        if (nWaitMilliSec > nMicroSecInterval) {
            nWaitMilliSec -= (nMicroSecInterval);
        }
        else {
            nWaitMilliSec = 0;
        }
    }

    return retcode;
}


