/*
$License:
Copyright (C) 2014 InvenSense Corporation, All Rights Reserved.
$
*/

/*******************************************************************************
*
* $Id$
*
******************************************************************************/

#ifndef ML_DATA_PARSER_H__
#define ML_DATA_PARSER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>

struct vector_data_t {
	int32_t x;
	int32_t y;
	int32_t z;
};

struct rotation_vector_data_t {
	int32_t x;
	int32_t y;
	int32_t z;
	int32_t w;
	uint16_t accuracy;
};

struct uncalibrated_data_t {
	struct vector_data_t uncalib;
	struct vector_data_t bias;
};

union sensor_data {
	struct vector_data_t accelerometer;
	struct vector_data_t magnetic_field;
	// DEPRECATED: orientation
	struct vector_data_t gyroscope;
	uint32_t light;
	uint32_t pressure;
	// DEPRECATED: temperature
	uint16_t proximity;
	struct vector_data_t gravity;
	struct vector_data_t linear_acceleration;
	struct rotation_vector_data_t rotation_vector;
	uint8_t relative_humidity;
	int16_t ambient_temperature;
	struct uncalibrated_data_t magnetic_field_uncalibrated;
	struct rotation_vector_data_t game_rotation_vector;
	struct uncalibrated_data_t gyroscope_uncalibrated;
	uint8_t significant_motion;
	uint8_t step_detector;
	uint64_t step_counter;
	struct rotation_vector_data_t geomagnetic_rotation_vector;
	uint16_t heart_rate;
	// TODO: activity_classification
};

enum sensor_id {
	SENSOR_ID_METADATA				= 0,
	SENSOR_ID_ACCELEROMETER			= 1,
	SENSOR_ID_MAGNETIC_FIELD		= 2,
	SENSOR_ID_ORIENTATION			= 3,
	SENSOR_ID_GYROSCOPE				= 4,
	SENSOR_ID_LIGHT					= 5,
	SENSOR_ID_PRESSURE				= 6,
	SENSOR_ID_TEMPERATURE			= 7,
	SENSOR_ID_PROXIMITY				= 8,
	SENSOR_ID_GRAVITY				= 9,
	SENSOR_ID_LINEAR_ACCELERATION	= 10,
	SENSOR_ID_ROTATION_VECTOR		= 11,
	SENSOR_ID_RELATIVE_HUMIDITY		= 12,
	SENSOR_ID_AMBIENT_TEMPERATURE			= 13,
	SENSOR_ID_MAGNETIC_FIELD_UNCALIBRATED	= 14,
	SENSOR_ID_GAME_ROTATION_VECTOR			= 15,
	SENSOR_ID_GYROSCOPE_UNCALIBRATED		= 16,
	SENSOR_ID_SIGNIFICANT_MOTION			= 17,
	SENSOR_ID_STEP_DETECTOR			        = 18,
	SENSOR_ID_STEP_COUNTER			        = 19,
	SENSOR_ID_GEOMAGNETIC_ROTATION_VECTOR	= 20,
	SENSOR_ID_HEART_RATE			        = 21,
	SENSOR_ID_NON_WAKE_UP_PROXIMITY			= 22,
	SENSOR_ID_WAKE_UP_ACCELEROMETER			= 23,
	SENSOR_ID_WAKE_UP_MAGNETIC_FIELD		= 24,
	SENSOR_ID_WAKE_UP_ORIENTATION			= 25,
	SENSOR_ID_WAKE_UP_GYROSCOPE			    = 26,
	SENSOR_ID_WAKE_UP_LIGHT				    = 27,
	SENSOR_ID_WAKE_UP_PRESSURE			    = 28,
	SENSOR_ID_WAKE_UP_GRAVITY			    = 29,
	SENSOR_ID_WAKE_UP_LINEAR_ACCELERATION	= 30,
	SENSOR_ID_WAKE_UP_ROTATION_VECTOR		= 31,
	SENSOR_ID_WAKE_UP_RELATIVE_HUMIDITY		= 32,
	SENSOR_ID_WAKE_UP_AMBIENT_TEMPERATURE	    	= 33,
	SENSOR_ID_WAKE_UP_MAGNETIC_FIELD_UNCALIBRATED	= 34,
	SENSOR_ID_WAKE_UP_GAME_ROTATION_VECTOR		    = 35,
	SENSOR_ID_WAKE_UP_GYROSCOPE_UNCALIBRATED	    = 36,
	SENSOR_ID_WAKE_UP_STEP_DETECTOR			        = 37,
	SENSOR_ID_WAKE_UP_STEP_COUNTER			        = 38,
	SENSOR_ID_WAKE_UP_GEOMAGNETIC_ROTATION_VECTOR	= 39,
	SENSOR_ID_WAKE_UP_HEART_RATE			        = 40,
	SENSOR_ID_WAKE_UP_TILT_DETECTOR			        = 41,

	SENSOR_ID_ACTIVITY_CLASSIFICATION		        = 252,
	SENSOR_ID_SCREEN_ROTATION			            = 253,
	SENSOR_ID_SELF_TEST				                = 254,
	SENSOR_ID_PLATFORM_SETUP			            = 255,
};

enum sensor_data_status {
	SENSOR_DATA_STATUS_DATA_UPDATED,
	SENSOR_DATA_STATUS_STATE_CHANGED,
	SENSOR_DATA_STATUS_FLUSH,
};

struct sensor_data_t {
	uint8_t id;
	uint8_t answer;
	union {
		uint32_t timestamp;
		uint8_t command;
	};
	uint8_t accuracy;
	uint8_t status;
	uint8_t size;
	union sensor_data data;
};

int get_sensor_data(const void *frame, size_t size, struct sensor_data_t *sensor_data);

#ifdef __cplusplus
}
#endif

#endif	/* ML_DATA_PARSER_H__ */
