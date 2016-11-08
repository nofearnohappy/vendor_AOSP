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

#ifndef ANDROID_SENSORS_H
#define ANDROID_SENSORS_H

#include <stdint.h>
#include <errno.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#include <linux/input.h>

#include <hardware/hardware.h>
#include <hardware/sensors.h>

#define LOG_TAG "Sensors"

__BEGIN_DECLS

/*****************************************************************************/

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))


/*****************************************************************************/

/*
 * The SENSORS Module
 */

/* the GP2A is a binary proximity sensor that triggers around 5 cm on
 * this hardware */
#define PROXIMITY_THRESHOLD_GP2A  5.0f

#define CONVERT_1            1.0f
#define CONVERT_10            0.1f
#define CONVERT_100            0.01f
#define CONVERT_1000        0.001f
#define CONVERT_10000        0.0001f

#define RANGE_A                     (2*GRAVITY_EARTH)
#define RESOLUTION_A                (RANGE_A/(256*1))

#define SENSOR_STATE_MASK           (0x7FFF)

/*****************************************************************************/

typedef enum {
    ACCELERATION            =0
    ,MAGNETIC
    ,GYRO
    ,LIGHT
    ,PROXIMITY
    ,PRESSURE
    ,HEARTBEAT
    ,ORIENTATION
    ,ROTATIONVECTOR
    ,LINEARACCELERATION
    ,GRAVITY
    ,MAGNETIC_UNCALIBRATED
    ,GYROSCOPE_UNCALIBRATED
    ,GAME_ROTATION_VECTOR
    ,GEOMAGNETIC_ROTATION_VECTOR
    ,STEP_DETECTOR
    ,STEP_COUNTER
    ,SIGNIFICANT_MOTION
    ,TILT
    ,SENSORS_ID_END
} SENSORS_ID;

typedef enum {
    TimestampSync = SENSORS_ID_END+1
    ,FLASH_DATA
    ,META_DATA
    ,MAGNETIC_UNCALIBRATED_BIAS
    ,GYRO_UNCALIBRATED_BIAS
    ,ERROR_MSG
    ,BATCH_TIMEOUT
    ,BATCH_FULL
    ,ACCURACY_UPDATE
    ,CALIBRATOR_UPDATE
    ,MCU_REINITIAL
}MCU_TO_CPU_EVENT_TYPE;

enum ABS_status {
    CW_ABS_X = 0x01,
    CW_ABS_Y,
    CW_ABS_Z,
    CW_ABS_X1,
    CW_ABS_Y1,
    CW_ABS_Z1,
    CW_ABS_TIMEDIFF,
	CW_ABS_TIMEDIFF_WAKE_UP,
    CW_ABS_ACCURACY,
    CW_ABS_TIMEBASE,
	CW_ABS_TIMEBASE_WAKE_UP
};

__END_DECLS

#endif  // ANDROID_SENSORS_H
