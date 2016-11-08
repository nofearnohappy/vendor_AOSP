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


#include <cutils/log.h>

#include <hardware/sensors.h>
#include <linux/hwmsensor.h>
//#include <hwmsen_chip_info.h>
#include "nusensors.h"

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "Sensors_Init"
#endif

#include <hardware/sensors.h>
#include <linux/hwmsensor.h>
#include "hwmsen_custom.h"

typedef enum SENSOR_NUM_DEF
{
     SONSER_UNSUPPORTED = -1,

    #ifdef CUSTOM_KERNEL_ACCELEROMETER
        ACCELEROMETER_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_MAGNETOMETER
        MAGNETOMETER_NUM,
        ORIENTATION_NUM ,
    #endif

    #if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_ALS)
        ALS_NUM,
    #endif
    #if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_PS)
        PS_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_GYROSCOPE
        GYROSCOPE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_BAROMETER
        PRESSURE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_TEMPURATURE
        TEMPURATURE_NUM,
    #endif
    #ifdef CUSTOM_KERNEL_HUMIDITY
        HUMIDITY_NUM,
    #endif
    #ifdef CUSTOM_KERNEL_STEP_COUNTER
        STEP_COUNTER_NUM,
        STEP_DETECTOR_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_SIGNIFICANT_MOTION_SENSOR
        STEP_SIGNIFICANT_MOTION_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_PEDOMETER
        PEDOMETER_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_IN_POCKET_SENSOR
        IN_POCKET_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_ACTIVITY_SENSOR
        ACTIVITY_NUM,
    #endif
	
    #ifdef CUSTOM_KERNEL_PICK_UP_SENSOR
        PICK_UP_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_FACE_DOWN_SENSOR
        FACE_DOWN_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_SHAKE_SENSOR
        SHAKE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_HEART
        HEART_RATE_NUM,
    #endif
    
    #ifdef CUSTOM_KERNEL_TILT_DETECTOR_SENSOR
        TILT_DETECTOR_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_WAKE_GESTURE_SENSOR
        WAKE_GESTURE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_GLANCE_GESTURE_SENSOR
        GLANCE_GESTURE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_GRV_SENSOR
        GAME_ROTATION_VECTOR_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_GMRV_SENSOR
        GEOMAGNETIC_ROTATION_VECTOR_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_RV_SENSOR
        ROTATION_VECTOR_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_GRAVITY_SENSOR
        GRAVITY_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_LINEARACCEL_SENSOR
        LINEARACCEL_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_BRINGTOSEE_SENSOR
        BRINGTOSEE_NUM,
    #endif

    SENSORS_NUM

}SENSOR_NUM_DEF;

#define MAX_NUM_SENSOR      (SENSORS_NUM)


/*--------------------------------------------------------*/
#ifdef CUSTOM_KERNEL_ACCELEROMETER
    #ifndef ACCELEROMETER
        #define ACCELEROMETER             "ACCELEROMETER"
        #define ACCELEROMETER_VENDER         "MTK"
    #endif
    #ifndef ACCELEROMETER_RANGE
        #define ACCELEROMETER_RANGE        32.0f
    #endif
    #ifndef ACCELEROMETER_RESOLUTION
        #define ACCELEROMETER_RESOLUTION    4.0f/1024.0f
    #endif
    #ifndef ACCELEROMETER_POWER
        #define ACCELEROMETER_POWER        130.0f/1000.0f
    #endif
#endif

#if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_PS) 
    #ifndef PROXIMITY
        #define PROXIMITY             "PROXIMITY"
        #define PROXIMITY_VENDER         "MTK"
    #endif
    #ifndef PROXIMITY_RANGE
        #define PROXIMITY_RANGE         1.00f
    #endif
    #ifndef PROXIMITY_RESOLUTION
        #define PROXIMITY_RESOLUTION          1.0f
    #endif
    #ifndef PROXIMITY_POWER
        #define PROXIMITY_POWER            0.13f
    #endif
#endif
#if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_ALS) 
    #ifndef LIGHT
        #define LIGHT                 "LIGHT"
        #define LIGHT_VENDER             "MTK"
    #endif
    #ifndef LIGHT_RANGE
        #define LIGHT_RANGE            10240.0f
    #endif
    #ifndef LIGHT_RESOLUTION
        #define LIGHT_RESOLUTION         1.0f
    #endif
    #ifndef LIGHT_POWER
        #define LIGHT_POWER            0.13f
    #endif
#endif

#ifdef CUSTOM_KERNEL_MAGNETOMETER
    #ifndef MAGNETOMETER
        #define MAGNETOMETER             "MAGNETOMETER"
        #define MAGNETOMETER_VENDER         "MTK"
    #endif
    #ifndef MAGNETOMETER_RANGE
        #define MAGNETOMETER_RANGE         600.0f
    #endif
    #ifndef MAGNETOMETER_RESOLUTION
        #define MAGNETOMETER_RESOLUTION        0.0016667f
    #endif
    #ifndef MAGNETOMETER_POWER
        #define MAGNETOMETER_POWER        0.25f
    #endif

    #ifndef ORIENTATION
        #define ORIENTATION             "ORIENTATION"
        #define ORIENTATION_VENDER         "MTK"
    #endif
    #ifndef ORIENTATION_RANGE
        #define ORIENTATION_RANGE        360.0f
    #endif
    #ifndef ORIENTATION_RESOLUTION
        #define ORIENTATION_RESOLUTION        1.0f
    #endif
    #ifndef ORIENTATION_POWER
        #define ORIENTATION_POWER        0.25f
    #endif
#endif

#ifdef CUSTOM_KERNEL_GYROSCOPE
    #ifndef GYROSCOPE
        #define GYROSCOPE             "GYROSCOPE"
        #define GYROSCOPE_VENDER         "MTK"
    #endif
    #ifndef GYROSCOPE_RANGE
        #define GYROSCOPE_RANGE            34.91f
    #endif
    #ifndef GYROSCOPE_RESOLUTION
        #define GYROSCOPE_RESOLUTION        0.0107f
    #endif
    #ifndef GYROSCOPE_POWER
        #define GYROSCOPE_POWER            6.1f
    #endif
#endif

#ifdef CUSTOM_KERNEL_BAROMETER
    #ifndef PRESSURE
        #define PRESSURE             "PRESSURE"
        #define PRESSURE_VENDER            "MTK"
    #endif
    #ifndef PRESSURE_RANGE
        #define PRESSURE_RANGE             1100.0f
    #endif
    #ifndef PRESSURE_RESOLUTION
        #define PRESSURE_RESOLUTION         100.0f
    #endif
    #ifndef PRESSURE_POWER
        #define PRESSURE_POWER            0.5f
    #endif
#endif

#ifdef CUSTOM_KERNEL_TEMPURATURE
    #ifndef TEMPURATURE
        #define TEMPURATURE             "TEMPURATURE"
        #define TEMPURATURE_VENDER        "MTK"
    #endif
    #ifndef TEMPURATURE_RANGE
        #define TEMPURATURE_RANGE         85.0f
    #endif
    #ifndef TEMPURATURE_RESOLUTION
        #define TEMPURATURE_RESOLUTION         0.1f
    #endif
    #ifndef TEMPURATURE_POWER
        #define TEMPURATURE_POWER         0.5f
    #endif
#endif
#ifdef CUSTOM_KERNEL_HUMIDITY
    #ifndef HUMIDITY
        #define HUMIDITY             "HUMIDITY"
        #define HUMIDITY_VENDER        "MTK"
    #endif
    #ifndef HUMIDITY_RANGE
        #define HUMIDITY_RANGE         85.0f
    #endif
    #ifndef HUMIDITY_RESOLUTION
        #define HUMIDITY_RESOLUTION         0.1f
    #endif
    #ifndef HUMIDITY_POWER
        #define HUMIDITY_POWER         0.5f
    #endif
#endif

#ifdef CUSTOM_KERNEL_STEP_COUNTER
    #ifndef STEP_COUNTER
        #define STEP_COUNTER             "STEP_COUNTER"
        #define STEP_COUNTER_VENDER        "MTK"

        #define STEP_DETECTOR             "STEP_DETECTOR"
        #define STEP_DETECTOR_VENDER    "MTK"
    #endif
    #ifndef STEP_COUNTER_RANGE
        #define STEP_COUNTER_RANGE                 85.0f
        #define STEP_DETECTOR_RANGE             85.0f
    #endif
    #ifndef STEP_COUNTER_RESOLUTION
        #define STEP_COUNTER_RESOLUTION         0.1f
        #define STEP_DETECTOR_RESOLUTION         0.1f
    #endif
    #ifndef STEP_COUNTER_POWER
        #define STEP_COUNTER_POWER                 0.5f
        #define STEP_DETECTOR_POWER             0.5f
    #endif
#endif

#ifdef CUSTOM_KERNEL_SIGNIFICANT_MOTION_SENSOR
    #ifndef SIGNIFICANT_MOTION
        #define SIGNIFICANT_MOTION             "SIGNIFICANT_MOTION"
        #define SIGNIFICANT_MOTION_VENDER    "MTK"
    #endif
    #ifndef SIGNIFICANT_MOTION_RANGE
        #define SIGNIFICANT_MOTION_RANGE         85.0f
    #endif
    #ifndef SIGNIFICANT_MOTION_RESOLUTION
        #define SIGNIFICANT_MOTION_RESOLUTION     0.1f
    #endif
    #ifndef SIGNIFICANT_MOTION_POWER
        #define SIGNIFICANT_MOTION_POWER         0.5f
    #endif
#endif

#ifdef CUSTOM_KERNEL_IN_POCKET_SENSOR
	#ifndef IN_POCKET
		#define IN_POCKET 		"IN_POCKET"
		#define IN_POCKET_VENDER	"MTK"
	#endif
	#ifndef IN_POCKET_RANGE
		#define IN_POCKET_RANGE 	85.0f
	#endif
	#ifndef IN_POCKET_RESOLUTION
		#define IN_POCKET_RESOLUTION 	0.1f
	#endif
	#ifndef IN_POCKET_POWER
		#define IN_POCKET_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_PEDOMETER
	#ifndef PEDOMETER
		#define PEDOMETER 		"PEDOMETER"
		#define PEDOMETER_VENDER	"MTK"
	#endif
	#ifndef PEDOMETER_RANGE
		#define PEDOMETER_RANGE 	85.0f
	#endif
	#ifndef PEDOMETER_RESOLUTION
		#define PEDOMETER_RESOLUTION 	0.1f
	#endif
	#ifndef PEDOMETER_POWER
		#define PEDOMETER_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_ACTIVITY_SENSOR
	#ifndef ACTIVITY
		#define ACTIVITY 		"ACTIVITY"
		#define ACTIVITY_VENDER	"MTK"
	#endif
	#ifndef ACTIVITY_RANGE
		#define ACTIVITY_RANGE 	85.0f
	#endif
	#ifndef ACTIVITY_RESOLUTION
		#define ACTIVITY_RESOLUTION 	0.1f
	#endif
	#ifndef ACTIVITY_POWER
		#define ACTIVITY_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_SHAKE_SENSOR
	#ifndef SHAKE
		#define SHAKE 		"SHAKE"
		#define SHAKE_VENDER	"MTK"
	#endif
	#ifndef SHAKE_RANGE
		#define SHAKE_RANGE 	85.0f
	#endif
	#ifndef SHAKE_RESOLUTION
		#define SHAKE_RESOLUTION 	0.1f
	#endif
	#ifndef SHAKE_POWER
		#define SHAKE_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_PICK_UP_SENSOR
	#ifndef PICK_UP
		#define PICK_UP 		"PICK_UP"
		#define PICK_UP_VENDER	"MTK"
	#endif
	#ifndef PICK_UP_RANGE
		#define PICK_UP_RANGE 	85.0f
	#endif
	#ifndef PICK_UP_RESOLUTION
		#define PICK_UP_RESOLUTION 	0.1f
	#endif
	#ifndef PICK_UP_POWER
		#define PICK_UP_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_FACE_DOWN_SENSOR
	#ifndef FACE_DOWN
		#define FACE_DOWN 		"FACE_DOWN"
		#define FACE_DOWN_VENDER	"MTK"
	#endif
	#ifndef FACE_DOWN_RANGE
		#define FACE_DOWN_RANGE 	85.0f
	#endif
	#ifndef FACE_DOWN_RESOLUTION
		#define FACE_DOWN_RESOLUTION 	0.1f
	#endif
	#ifndef FACE_DOWN_POWER
		#define FACE_DOWN_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_HEART
	#ifndef HEART_RATE
		#define HEART_RATE 		"HEART_RATE"
		#define HEART_RATE_VENDER	"MTK"
	#endif
	#ifndef HEART_RATE_RANGE
		#define HEART_RATE_RANGE 	500.0f
	#endif
	#ifndef HEART_RATE_RESOLUTION
		#define HEART_RATE_RESOLUTION 	0.1f
	#endif
	#ifndef HEART_RATE_POWER
		#define HEART_RATE_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_TILT_DETECTOR_SENSOR
	#ifndef TILT_DETECTOR
		#define TILT_DETECTOR 		"TILT_DETECTOR"
		#define TILT_DETECTOR_VENDER	"MTK"
	#endif
	#ifndef TILT_DETECTOR_RANGE
		#define TILT_DETECTOR_RANGE 	100.0f
	#endif
	#ifndef TILT_DETECTOR_RESOLUTION
		#define TILT_DETECTOR_RESOLUTION 	0.1f
	#endif
	#ifndef TILT_DETECTOR_POWER
		#define TILT_DETECTOR_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_WAKE_GESTURE_SENSOR
	#ifndef WAKE_GESTURE
		#define WAKE_GESTURE 		"WAKE_GESTURE"
		#define WAKE_GESTURE_VENDER	"MTK"
	#endif
	#ifndef WAKE_GESTURE_RANGE
		#define WAKE_GESTURE_RANGE 	85.0f
	#endif
	#ifndef WAKE_GESTURE_RESOLUTION
		#define WAKE_GESTURE_RESOLUTION 	0.1f
	#endif
	#ifndef WAKE_GESTURE_POWER
		#define WAKE_GESTURE_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_GLANCE_GESTURE_SENSOR
	#ifndef GLANCE_GESTURE
		#define GLANCE_GESTURE 		"GLANCE_GESTURE"
		#define GLANCE_GESTURE_VENDER	"MTK"
	#endif
	#ifndef GLANCE_GESTURE_RANGE
		#define GLANCE_GESTURE_RANGE 	85.0f
	#endif
	#ifndef GLANCE_GESTURE_RESOLUTION
		#define GLANCE_GESTURE_RESOLUTION 	0.1f
	#endif
	#ifndef GLANCE_GESTURE_POWER
		#define GLANCE_GESTURE_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_GRV_SENSOR
	#ifndef GAME_ROTATION_VECTOR
		#define GAME_ROTATION_VECTOR 		"GAME ROTATION VECTOR"
		#define GAME_ROTATION_VECTOR_VENDER	"MTK"
	#endif
	#ifndef GAME_ROTATION_VECTOR_RANGE
		#define GAME_ROTATION_VECTOR_RANGE 	10240.0f
	#endif
	#ifndef GAME_ROTATION_VECTOR_RESOLUTION
		#define GAME_ROTATION_VECTOR_RESOLUTION 	1.0f
	#endif
	#ifndef GAME_ROTATION_VECTOR_POWER
		#define GAME_ROTATION_VECTOR_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_GMRV_SENSOR
	#ifndef GEOMAGNETIC_ROTATION_VECTOR
		#define GEOMAGNETIC_ROTATION_VECTOR 		"GEOMAGNETIC ROTATION VECTOR"
		#define GEOMAGNETIC_ROTATION_VECTOR_VENDER	"MTK"
	#endif
	#ifndef GEOMAGNETIC_ROTATION_VECTOR_RANGE
		#define GEOMAGNETIC_ROTATION_VECTOR_RANGE 	10240.0f
	#endif
	#ifndef GEOMAGNETIC_ROTATION_VECTOR_RESOLUTION
		#define GEOMAGNETIC_ROTATION_VECTOR_RESOLUTION 	1.0f
	#endif
	#ifndef GEOMAGNETIC_ROTATION_VECTOR_POWER
		#define GEOMAGNETIC_ROTATION_VECTOR_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_RV_SENSOR
	#ifndef ROTATION_VECTOR
		#define ROTATION_VECTOR 		"ROTATION VECTOR"
		#define ROTATION_VECTOR_VENDER	"MTK"
	#endif
	#ifndef ROTATION_VECTOR_RANGE
		#define ROTATION_VECTOR_RANGE 	10240.0f
	#endif
	#ifndef ROTATION_VECTOR_RESOLUTION
		#define ROTATION_VECTOR_RESOLUTION 	1.0f
	#endif
	#ifndef ROTATION_VECTOR_POWER
		#define ROTATION_VECTOR_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_GRAVITY_SENSOR
	#ifndef GRAVITY
		#define GRAVITY 		"GRAVITY"
		#define GRAVITY_VENDER	"MTK"
	#endif
	#ifndef GRAVITY_RANGE
		#define GRAVITY_RANGE 	10240.0f
	#endif
	#ifndef GRAVITY_RESOLUTION
		#define GRAVITY_RESOLUTION 	1.0f
	#endif
	#ifndef GRAVITY_POWER
		#define GRAVITY_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_LINEARACCEL_SENSOR
	#ifndef LINEARACCEL
		#define LINEARACCEL 		"LINEARACCEL"
		#define LINEARACCEL_VENDER	"MTK"
	#endif
	#ifndef LINEARACCEL_RANGE
		#define LINEARACCEL_RANGE 	10240.0f
	#endif
	#ifndef LINEARACCEL_RESOLUTION
		#define LINEARACCEL_RESOLUTION 	1.0f
	#endif
	#ifndef LINEARACCEL_POWER
		#define LINEARACCEL_POWER 	0.5f
	#endif
#endif

#ifdef CUSTOM_KERNEL_BRINGTOSEE_SENSOR
	#ifndef BRINGTOSEE
		#define BRINGTOSEE 		"BRING TO SEE DETECTOR"
		#define BRINGTOSEE_VENDER	"MTK"
	#endif
	#ifndef BRINGTOSEE_RANGE
		#define BRINGTOSEE_RANGE 	10240.0f
	#endif
	#ifndef BRINGTOSEE_RESOLUTION
		#define BRINGTOSEE_RESOLUTION 	1.0f
	#endif
	#ifndef BRINGTOSEE_POWER
		#define BRINGTOSEE_POWER 	0.5f
	#endif
#endif

/*--------------------------------------------------------*/

struct sensor_t sSensorList[] =
{
#ifdef CUSTOM_KERNEL_ACCELEROMETER
    {
        .name       = ACCELEROMETER,
        .vendor     = ACCELEROMETER_VENDER,
        .version    = 3,
		.handle     = ID_ACCELEROMETER+ID_OFFSET,
        .type       = SENSOR_TYPE_ACCELEROMETER,
        .maxRange   = ACCELEROMETER_RANGE,//32.0f,
        .resolution = ACCELEROMETER_RESOLUTION,//4.0f/1024.0f,
        .power      = ACCELEROMETER_POWER,//130.0f/1000.0f,
        .minDelay   = 10000,
		.maxDelay   = 1000000,
        .reserved   = {}
    },
#endif

#if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_PS) 
    {
        .name       = PROXIMITY,
        .vendor     = PROXIMITY_VENDER,
        .version    = 1,
        .handle     = ID_PROXIMITY+ID_OFFSET,
        .type       = SENSOR_TYPE_PROXIMITY,
        .maxRange   = PROXIMITY_RANGE,//1.00f,
        .resolution = PROXIMITY_RESOLUTION,//1.0f,
        .power      = PROXIMITY_POWER,//0.13f,
        .reserved   = {}
    },
#endif
#if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_ALS) 
    {
        .name       = LIGHT,
        .vendor     = LIGHT_VENDER,
        .version    = 1,
        .handle     = ID_LIGHT+ID_OFFSET,
        .type       = SENSOR_TYPE_LIGHT,
        .maxRange   = LIGHT_RANGE,//10240.0f,
        .resolution = LIGHT_RESOLUTION,//1.0f,
        .power      = LIGHT_POWER,//0.13f,
        .reserved   = {}
    },
#endif

#ifdef CUSTOM_KERNEL_GYROSCOPE
    {
        .name       = GYROSCOPE,
        .vendor     = GYROSCOPE_VENDER,
        .version    = 3,
        .handle     = ID_GYROSCOPE+ID_OFFSET,
        .type       = SENSOR_TYPE_GYROSCOPE,
        .maxRange   = GYROSCOPE_RANGE,//34.91f,
        .resolution = GYROSCOPE_RESOLUTION,//0.0107f,
        .power      = GYROSCOPE_POWER,//6.1f,
        .minDelay   = 10000,
		.maxDelay   = 1000000,
        .reserved   = {}
    },
#endif

#ifdef CUSTOM_KERNEL_MAGNETOMETER
    {
        .name       = ORIENTATION,
        .vendor     = ORIENTATION_VENDER,
        .version    = 3,
        .handle     = ID_ORIENTATION+ID_OFFSET,
        .type       = SENSOR_TYPE_ORIENTATION,
        .maxRange   = ORIENTATION_RANGE,//360.0f,
        .resolution = ORIENTATION_RESOLUTION,//1.0f,
        .power      = ORIENTATION_POWER,//0.25f,
        .minDelay   = 100000,
		.maxDelay   = 1000000,
        .reserved   = {}
    },

    {
        .name       = MAGNETOMETER,
        .vendor     = MAGNETOMETER_VENDER,
        .version    = 3,
        .handle     = ID_MAGNETIC+ID_OFFSET,
        .type       = SENSOR_TYPE_MAGNETIC_FIELD,
        .maxRange   = MAGNETOMETER_RANGE,//600.0f,
        .resolution = MAGNETOMETER_RESOLUTION,//0.0016667f,
        .power      = MAGNETOMETER_POWER,//0.25f,
        .minDelay   = 100000,
		.maxDelay   = 1000000,
        .reserved   = {}
    },
#endif

#ifdef CUSTOM_KERNEL_BAROMETER
    {
        .name       = PRESSURE,
        .vendor     = PRESSURE_VENDER,
        .version    = 3,
        .handle     = ID_PRESSURE+ID_OFFSET,
        .type       = SENSOR_TYPE_PRESSURE,
        .maxRange   = PRESSURE_RANGE,//360.0f,
        .resolution = PRESSURE_RESOLUTION,//1.0f,
        .power      = PRESSURE_POWER,//0.25f,
        .minDelay   = 200000,
		.maxDelay   = 1000000,
        .reserved   = {}
    },
#endif

#ifdef CUSTOM_KERNEL_TEMPURATURE
    {
        .name       = TEMPURATURE,
        .vendor     = TEMPURATURE_VENDER,
        .version    = 1,
        .handle     = ID_TEMPRERATURE+ID_OFFSET,
        .type       = SENSOR_TYPE_TEMPERATURE,
        .maxRange   = TEMPURATURE_RANGE,//600.0f,
        .resolution = TEMPURATURE_RESOLUTION,//0.0016667f,
        .power      = TEMPURATURE_POWER,//0.25f,
        .reserved   = {}
    },
#endif
#ifdef CUSTOM_KERNEL_HUMIDITY
    {
        .name       = HUMIDITY,
        .vendor     = HUMIDITY_VENDER,
        .version    = 1,
        .handle     = ID_HUMIDITY+ID_OFFSET,
        .type       = SENSOR_TYPE_HUMIDITY,
        .maxRange   = HUMIDITY_RANGE,  // 600.0f,
        .resolution = HUMIDITY_RESOLUTION,  // 0.0016667f,
        .power      = HUMIDITY_POWER,  // 0.25f,
        .reserved   = {}
    },
#endif

#ifdef CUSTOM_KERNEL_STEP_COUNTER
    {
        .name       = STEP_COUNTER,
        .vendor     = STEP_COUNTER_VENDER,
        .version    = 1,
        .handle     = ID_STEP_COUNTER+ID_OFFSET,
        .type       = SENSOR_TYPE_STEP_COUNTER,
        .maxRange   = STEP_COUNTER_RANGE,//600.0f,
        .resolution = STEP_COUNTER_RESOLUTION,//0.0016667f,
        .power      = STEP_COUNTER_POWER,//0.25f,
        .reserved   = {}
    },
    {
        .name       = STEP_DETECTOR,
        .vendor     = STEP_DETECTOR_VENDER,
        .version    = 1,
        .handle     = ID_STEP_DETECTOR+ID_OFFSET,
        .type       = SENSOR_TYPE_STEP_DETECTOR,
        .maxRange   = STEP_DETECTOR_RANGE,//600.0f,
        .resolution = STEP_DETECTOR_RESOLUTION,//0.0016667f,
        .power      = STEP_DETECTOR_POWER,//0.25f,
        .reserved   = {}
    },
#endif

#ifdef CUSTOM_KERNEL_SIGNIFICANT_MOTION_SENSOR
    {
        .name        = SIGNIFICANT_MOTION,
        .vendor     = SIGNIFICANT_MOTION_VENDER,
        .version    = 1,
        .handle     = ID_SIGNIFICANT_MOTION+ID_OFFSET,
        .type        = SENSOR_TYPE_SIGNIFICANT_MOTION,
        .maxRange    = SIGNIFICANT_MOTION_RANGE,//600.0f,
        .resolution = SIGNIFICANT_MOTION_RESOLUTION,//0.0016667f,
        .power        = SIGNIFICANT_MOTION_POWER,//0.25f,
        .minDelay   = -1,  //SENSOR_FLAG_ONE_SHOT_MODE
        .reserved    = {}
    },
#endif

#ifdef CUSTOM_KERNEL_GRV_SENSOR
    {
		.name		= GAME_ROTATION_VECTOR,
		.vendor 	= GAME_ROTATION_VECTOR_VENDER,
		.version	= 1,
		.handle 	= ID_GAME_ROTATION_VECTOR+ID_OFFSET,
		.type		= SENSOR_TYPE_GAME_ROTATION_VECTOR,
		.maxRange	= GAME_ROTATION_VECTOR_RANGE,
		.resolution = GAME_ROTATION_VECTOR_RESOLUTION,
		.power		= GAME_ROTATION_VECTOR_POWER,
		.minDelay = 10000,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 64,
		.reserved	= {}
    },
#endif

#ifdef CUSTOM_KERNEL_GMRV_SENSOR
    {
		.name		= GEOMAGNETIC_ROTATION_VECTOR,
		.vendor 	= GEOMAGNETIC_ROTATION_VECTOR_VENDER,
		.version	= 1,
		.handle 	= ID_GEOMAGNETIC_ROTATION_VECTOR+ID_OFFSET,
		.type		= SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR,
		.maxRange	= GEOMAGNETIC_ROTATION_VECTOR_RANGE,
		.resolution = GEOMAGNETIC_ROTATION_VECTOR_RESOLUTION,
		.power		= GEOMAGNETIC_ROTATION_VECTOR_POWER,
		.minDelay = 10000,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 64,
		.reserved	= {}
    },
#endif

#ifdef CUSTOM_KERNEL_RV_SENSOR
	{
		.name		= ROTATION_VECTOR,
		.vendor 	= ROTATION_VECTOR_VENDER,
		.version	= 1,
		.handle 	= ID_ROTATION_VECTOR+ID_OFFSET,
		.type		= SENSOR_TYPE_ROTATION_VECTOR,
		.maxRange	= ROTATION_VECTOR_RANGE,
		.resolution = ROTATION_VECTOR_RESOLUTION,
		.power		= ROTATION_VECTOR_POWER,
		.minDelay = 10000,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 64,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_GRAVITY_SENSOR
	{
		.name		= GRAVITY,
		.vendor 	= GRAVITY_VENDER,
		.version	= 1,
		.handle 	= ID_GRAVITY+ID_OFFSET,
		.type		= SENSOR_TYPE_GRAVITY,
		.maxRange	= GRAVITY_RANGE,
		.resolution = GRAVITY_RESOLUTION,
		.power		= GRAVITY_POWER,
		.minDelay = 10000,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 64,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_LINEARACCEL_SENSOR
	{
		.name		= LINEARACCEL,
		.vendor 	= LINEARACCEL_VENDER,
		.version	= 1,
		.handle 	= ID_LINEAR_ACCELERATION+ID_OFFSET,
		.type		= SENSOR_TYPE_LINEAR_ACCELERATION,
		.maxRange	= LINEARACCEL_RANGE,
		.resolution = LINEARACCEL_RESOLUTION,
		.power		= LINEARACCEL_POWER,
		.minDelay = 10000,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 64,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_HEART
	{ 
		.name		= HEART_RATE,
		.vendor 	= HEART_RATE_VENDER,
		.version	= 1,
		.handle 	= ID_HEART_RATE+ID_OFFSET,
		.type		= SENSOR_TYPE_HEART_RATE,
		.maxRange	= HEART_RATE_RANGE,//600.0f,
		.resolution	= HEART_RATE_RESOLUTION,//0.0016667f,
		.power		= HEART_RATE_POWER,//0.25f,
		.minDelay = 0,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 0,
		.stringType = SENSOR_STRING_TYPE_HEART_RATE,
		.requiredPermission = SENSOR_PERMISSION_BODY_SENSORS,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_BRINGTOSEE_SENSOR
	{
		.name		= BRINGTOSEE,
		.vendor 	= BRINGTOSEE_VENDER,
		.version	= 1,
		.handle 	= ID_BRINGTOSEE+ID_OFFSET,
		.type		= SENSOR_TYPE_BRINGTOSEE,
		.maxRange	= BRINGTOSEE_RANGE,
		.resolution = BRINGTOSEE_RESOLUTION,
		.power		= BRINGTOSEE_POWER,
		.minDelay = 10000,
		.fifoReservedEventCount = 0,
		.fifoMaxEventCount = 64,
		.stringType = SENSOR_STRING_TYPE_BRINGTOSEE,
		//.requiredPermission = SENSOR_PERMISSION_BODY_SENSORS,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_IN_POCKET_SENSOR
	{
		.name		= IN_POCKET,
		.vendor 	= IN_POCKET_VENDER,
		.version	= 1,
		.handle 	= ID_IN_POCKET+ID_OFFSET,
		.type		= SENSOR_TYPE_IN_POCKET,
		.maxRange	= IN_POCKET_RANGE,//600.0f,
		.resolution	= IN_POCKET_RESOLUTION,//0.0016667f,
		.power		= IN_POCKET_POWER,//0.25f,
		.stringType    = SENSOR_STRING_TYPE_IN_POCKET,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_PEDOMETER
	{
		.name		= PEDOMETER,
		.vendor 	= PEDOMETER_VENDER,
		.version	= 1,
		.handle 	= ID_PEDOMETER+ID_OFFSET,
		.type		= SENSOR_TYPE_PEDOMETER,
		.maxRange	= PEDOMETER_RANGE,//600.0f,
		.resolution	= PEDOMETER_RESOLUTION,//0.0016667f,
		.power		= PEDOMETER_POWER,//0.25f,
		.stringType    = SENSOR_STRING_TYPE_PEDOMETER,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_ACTIVITY_SENSOR
	{
		.name		= ACTIVITY,
		.vendor 	= ACTIVITY_VENDER,
		.version	= 1,
		.handle 	= ID_ACTIVITY+ID_OFFSET,
		.type		= SENSOR_TYPE_ACTIVITY,
		.maxRange	= ACTIVITY_RANGE,//600.0f,
		.resolution	= ACTIVITY_RESOLUTION,//0.0016667f,
		.power		= ACTIVITY_POWER,//0.25f,
		.stringType    = SENSOR_STRING_TYPE_ACTIVITY,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_SHAKE_SENSOR
	{
		.name		= SHAKE,
		.vendor 	= SHAKE_VENDER,
		.version	= 1,
		.handle 	= ID_SHAKE+ID_OFFSET,
		.type		= SENSOR_TYPE_SHAKE,
		.maxRange	= SHAKE_RANGE,//600.0f,
		.resolution	= SHAKE_RESOLUTION,//0.0016667f,
		.power		= SHAKE_POWER,//0.25f,
		.minDelay   = -1,  //SENSOR_FLAG_ONE_SHOT_MODE
		.stringType    = SENSOR_STRING_TYPE_SHAKE,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_PICK_UP_SENSOR
	{
		.name		= PICK_UP,
		.vendor 	= PICK_UP_VENDER,
		.version	= 1,
        .handle 	= ID_PICK_UP_GESTURE+ID_OFFSET,
        .type		= SENSOR_TYPE_PICK_UP_GESTURE,
		.maxRange	= PICK_UP_RANGE,//600.0f,
		.resolution	= PICK_UP_RESOLUTION,//0.0016667f,
		.power		= PICK_UP_POWER,//0.25f,
		.minDelay   = -1,  //SENSOR_FLAG_ONE_SHOT_MODE
		.stringType    = SENSOR_STRING_TYPE_PICK_UP_GESTURE,
		.reserved	= {}
	},
#endif

#ifdef CUSTOM_KERNEL_FACE_DOWN_SENSOR
	{
		.name		= FACE_DOWN,
		.vendor 	= FACE_DOWN_VENDER,
		.version	= 1,
		.handle 	= ID_FACE_DOWN+ID_OFFSET,
		.type		= SENSOR_TYPE_FACE_DOWN,
		.maxRange	= FACE_DOWN_RANGE,//600.0f,
		.resolution	= FACE_DOWN_RESOLUTION,//0.0016667f,
		.power		= FACE_DOWN_POWER,//0.25f,
		.minDelay   = -1,  //SENSOR_FLAG_ONE_SHOT_MODE
		.stringType    = SENSOR_STRING_TYPE_FACE_DOWN,
		.reserved	= {}
	},
#endif

//#ifdef CUSTOM_KERNEL_HEART
//    {
//        .name		= HEART_RATE,
//        .vendor 	= HEART_RATE_VENDER,
//        .version	= 1,
//        .handle 	= ID_HEART_RATE+ID_OFFSET,
//        .type		= SENSOR_TYPE_HEART_RATE,
//        .maxRange	= HEART_RATE_RANGE,//600.0f,
//        .resolution	= HEART_RATE_RESOLUTION,//0.0016667f,
//        .power		= HEART_RATE_POWER,//0.25f,
//        .reserved	= {}
//    },
//#endif

#ifdef CUSTOM_KERNEL_TILT_DETECTOR_SENSOR
    {
        .name		= TILT_DETECTOR,
        .vendor 	= TILT_DETECTOR_VENDER,
        .version	= 1,
        .handle 	= ID_TILT_DETECTOR+ID_OFFSET,
        .type		= SENSOR_TYPE_TILT_DETECTOR,
        .maxRange	= TILT_DETECTOR_RANGE,//600.0f,
        .resolution	= TILT_DETECTOR_RESOLUTION,//0.0016667f,
        .power		= TILT_DETECTOR_POWER,//0.25f,
        .reserved	= {}
    },
#endif

#ifdef CUSTOM_KERNEL_WAKE_GESTURE_SENSOR
    {
        .name		= WAKE_GESTURE,
        .vendor 	= WAKE_GESTURE_VENDER,
        .version	= 1,
        .handle 	= ID_WAKE_GESTURE+ID_OFFSET,
        .type		= SENSOR_TYPE_WAKE_GESTURE,
        .maxRange	= WAKE_GESTURE_RANGE,//600.0f,
        .resolution	= WAKE_GESTURE_RESOLUTION,//0.0016667f,
        .power		= WAKE_GESTURE_POWER,//0.25f,
        .reserved	= {}
    },
#endif

#ifdef CUSTOM_KERNEL_GLANCE_GESTURE_SENSOR
    {
        .name		= GLANCE_GESTURE,
        .vendor 	= GLANCE_GESTURE_VENDER,
        .version	= 1,
        .handle 	= ID_GLANCE_GESTURE+ID_OFFSET,
        .type		= SENSOR_TYPE_GLANCE_GESTURE,
        .maxRange	= GLANCE_GESTURE_RANGE,//600.0f,
        .resolution	= GLANCE_GESTURE_RESOLUTION,//0.0016667f,
        .power		= GLANCE_GESTURE_POWER,//0.25f,
        .reserved	= {}
    },
#endif

};

/*****************************************************************************/

/*
 * The SENSORS Module
 */

/*
 * the AK8973 has a 8-bit ADC but the firmware seems to average 16 samples,
 * or at least makes its calibration on 12-bits values. This increases the
 * resolution by 4 bits.
 */

//extern  struct sensor_t sSensorList[MAX_NUM_SENSOR];


static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device);

static int sensors__get_sensors_list(struct sensors_module_t* module,
        struct sensor_t const** list)
{
    ALOGD(" sSensorList addr =%p, module addr =%p\r\n",sSensorList,module);
    ALOGD(" ARRAY_SIZE(sSensorList) =%d SENSORS_NUM=%d MAX_NUM_SENSOR=%d \r\n",ARRAY_SIZE(sSensorList), SENSORS_NUM, MAX_NUM_SENSOR);
    *list = sSensorList;
    return ARRAY_SIZE(sSensorList);

}


static struct hw_module_methods_t sensors_module_methods = {
    .open = open_sensors
};

struct sensors_module_t HAL_MODULE_INFO_SYM = {
    .common = {
        .tag = HARDWARE_MODULE_TAG,
        .version_major = 1,
        .version_minor = 0,
        .id = SENSORS_HARDWARE_MODULE_ID,
        .name = "MTK SENSORS Module",
        .author = "Mediatek",
        .methods = &sensors_module_methods,
    },
    .get_sensors_list = sensors__get_sensors_list,
};

/*****************************************************************************/

static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device)
{
   ALOGD("%s: name: %s! fwq debug\r\n", __func__, name);

   return init_nusensors(module, device);
}
