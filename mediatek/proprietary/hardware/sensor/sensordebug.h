/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2012. All rights reserved.
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

#ifndef ANDROID_SENSOR_DEBUG_H
#define ANDROID_SENSOR_DEBUG_H

#include <hardware/sensors.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <math.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdlib.h>

#include <poll.h>
#include <pthread.h>

#include <linux/input.h>

#include <cutils/atomic.h>
#include <cutils/log.h>

#include <signal.h>
#include <pthread.h>

#include "SensorBase.h"

//for M-sensor Accurancy Debug log
#define SENSOR_LOG  "/data/msensor1.log"
#define SENSOR_LOG2 "/data/msensor2.log"
#define MAX_RECORD_LEN 1048576//5242880(5MB) //2097152 (2MB)/ 1048576(1MB)
#define TEMP_BUFFER_SIZE 2048


/*****************************************************************************/

class SensorDebugObject {
protected:
    int m_G_active_log_path_id;
    int m_MSENSOR_ACCURANCY;
    int m_Mode_value;
    int m_Data_len;
    bool m_Is_old_m_driver;
    bool m_Is_old_g_driver;

    double m_MAG_DATA;

    char m_Data_buffer[TEMP_BUFFER_SIZE+100];
    char m_Record_path[2][20];

    static unsigned int SensorDebugBit;
    static SensorBase* mDebugsensorlist[3];

    pthread_t mThread;
    pthread_cond_t m_Sensor_event_cond;
    pthread_mutex_t m_Sensor_mutex;

    void write_sensor_log(char *buf,int len);
    void m_sensor_debug_func();
    void g_sensor_debug_func();
    bool is_old_structure(int sensor);
    static void * sensors_debug(void *para);

public:

            SensorDebugObject(SensorBase* sensorbaseList, int sensor);
    virtual ~SensorDebugObject();
    void send_singnal(int i);
};



/*****************************************************************************/

#endif  // ANDROID_SENSOR_DEBUG_H
