/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_CWMCU_SENSOR_H
#define ANDROID_CWMCU_SENSOR_H

#include <stdint.h>
#include <errno.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#include "sensors.h"
#include "SensorBase.h"
#include "InputEventReader.h"


/*
#define    SAVE_PATH_ACC                "/data/data/com.cywee.calibrator/cw_calibrator_acc.ini"
#define    SAVE_PATH_MAG                "/data/data/com.cywee.calibrator/cw_calibrator_mag.ini"
#define    SAVE_PATH_GYRO                "/data/data/com.cywee.calibrator/cw_calibrator_gyro.ini"
#define    SAVE_PATH_LIGHT                "/data/data/com.cywee.calibrator/cw_calibrator_light.ini"
#define    SAVE_PATH_PROXIMITY            "/data/data/com.cywee.calibrator/cw_calibrator_proximity.ini"
*/
#define    SAVE_PATH_ACC                "/data/system/cw_calibrator_acc.ini"
#define    SAVE_PATH_MAG                "/data/system/cw_calibrator_mag.ini"
#define    SAVE_PATH_GYRO                "/data/system/cw_calibrator_gyro.ini"
#define    SAVE_PATH_LIGHT                "/data/system/cw_calibrator_light.ini"
#define    SAVE_PATH_PROXIMITY            "/data/system/cw_calibrator_proximity.ini"

#define SENSOR_TYPE_TILT (SENSOR_TYPE_DEVICE_PRIVATE_BASE)

/*****************************************************************************/
struct input_event;

typedef enum {
    RmcuWap = 0
    ,RapWmcu
}CalibratorCmd;
typedef enum {
    CALIBRATOR_STATUS_OUT_OF_RANGE= -2,
    CALIBRATOR_STATUS_FAIL= -1,
    CALIBRATOR_STATUS_NON = 0,
    CALIBRATOR_STATUS_INPROCESS = 1,
    CALIBRATOR_STATUS_PASS = 2,
} CALIBRATOR_STATUS;

class CwMcuSensor : public SensorBase {

    uint32_t mEnabled;
    InputEventCircularReader mInputReader;
    sensors_event_t mPendingEvents[SENSORS_ID_END];
    sensors_event_t mPendingEvents_flush;
    uint16_t time_mcu[SENSORS_ID_END];
    uint16_t time_diff[SENSORS_ID_END];
    bool mHasPendingEvent;
    uint32_t mPendingMask;
    char input_sysfs_path[PATH_MAX];
    int input_sysfs_path_len;
    int64_t mEnabledTime[SENSORS_ID_END];
    int flush_event;
    int setInitialState();
    int write_to_file(int type,int data);
    uint16_t pedometer_l;
    uint16_t pedometer_h;
    uint16_t pedometerd_l;
    uint16_t pedometerd_h;


    int64_t HalInitialTime;
    uint32_t McuInitialTime;

	int64_t CpuTimeBase;
	int64_t CpuTimeBase_normal;
	int64_t CpuTimeBase_batch;
	int64_t CpuTimeBaseTemp;

	
	int64_t PreSyncTime;
	uint32_t McuTimeStamp;
	uint32_t PreMcuTimeStamp;
	uint32_t McuTimeBase;
	uint32_t McuTimeBase_normal;
	uint32_t McuTimeBase_batch;
	uint32_t PreTimeDiff_normal;
	uint32_t PreTimeDiff_batch;
	
	/* use for sync time */
	int64_t sensors_time_mcu[SENSORS_ID_END];
	int64_t PreCpu_Mcu_time_offset;
	int64_t Cpu_Mcu_time_offset_dy;
	int64_t PreOffsetDyUpdateTime;
	int64_t Cpu_Mcu_time_offset;
   
public:
    CwMcuSensor();
    virtual ~CwMcuSensor();
    virtual int readEvents(sensors_event_t* data, int count);
    virtual bool hasPendingEvents() const;
    virtual int setDelay(int32_t handle, int64_t ns);
    virtual int enable(int32_t handle, int enabled);
    virtual int batch(int handle, int flags, int64_t period_ns, int64_t timeout);
    virtual int flush(int handle);
    int find_sensor(int32_t handle);
    void processEvent(int code, int value);

	int cw_read_calibrator_file(int cmd, char *path, int *calib);
	int cw_save_calibrator_file(int cmd, char *path, int *calib);
	int cw_read_calibrator_sysfs(int cmd, char *path, int *calib);
	int cw_save_calibrator_sysfs(int cmd, char *path, int *calib);
	int cw_set_command_sysfs(int cmd, char *path);
	int cw_calibrator(CalibratorCmd cmd, int id);
	int cw_synctimestamp_normal(void);
	int cw_synctimestamp_batch(void);
	int cw_synctimestamp(void);
	int cw_read_sysfs(char *path, char *data,int size);
	int64_t calculate_offset(int64_t time);

};

/*****************************************************************************/

#endif  // ANDROID_CWMCU_SENSOR_H
