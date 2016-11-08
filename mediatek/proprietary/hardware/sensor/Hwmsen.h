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

#ifndef __HWM_SENSOR_H__
#define __HWM_SENSOR_H__

#include <stdint.h>
#include <errno.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#include "sensordebug.h"
#include "nusensors.h"
#include "SensorBase.h"
#include "InputEventReader.h"
#include <linux/hwmsensor.h> // by fwq


/*****************************************************************************/

struct input_event;

//static uint32_t g_active_sensors;


struct sensor_delay
{
   int handle;
   uint32_t delay;
};

class Hwmsen : public SensorBase {

    enum {
        Accelerometer    = 0,
        MagneticField    = 1,
        Orientation      = 2,
        Gyro             = 3,
        light            = 4,
        proximity        = 5,
        numSensors       = 6,//hwmsen driver process 6 device data
    };

    uint32_t mActiveSensors;
    int mEnabled;
    InputEventCircularReader mInputReader;

    bool mHasPendingEvent;

    uint64_t mDelays[numSensors];
    uint32_t mPendingMask;


    float indexToValue(size_t index) const;
    void processEvent(int code, int value);
    int update_delay(int what);
	
    int64_t m_hwm_last_ts[numSensors];
    uint64_t data_type;


public:
    int mdata_fd;
    sensors_event_t mPendingEvents[numSensors];
    SensorDebugObject *mHwmSensorDebug;

            Hwmsen();
    virtual ~Hwmsen();
    virtual int readEvents(sensors_event_t* data, int count);
    void readSensorData(void);
    virtual bool hasPendingEvents() const;
    virtual int enable(int32_t handle, int enabled);
    virtual int setDelay(int32_t handle, int64_t ns);
    virtual int batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs);
    virtual int flush(int handle);

    int enableNoHALDataAcc(int en);
    int FindDataFd();
};

/*****************************************************************************/

#endif  // ANDROID_LIGHT_SENSOR_H
