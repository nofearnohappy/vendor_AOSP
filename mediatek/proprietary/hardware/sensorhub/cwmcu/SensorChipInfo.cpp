#include <hardware/sensors.h>
#include <stdlib.h>
#include "sensors.h"
#include "CwMcuSensor.h"


#ifndef PROXIMITY_RANGE
#define PROXIMITY_RANGE         1.00f
#endif
#ifndef PROXIMITY_RESOLUTION
#define PROXIMITY_RESOLUTION          1.0f
#endif
#ifndef PROXIMITY_POWER
#define PROXIMITY_POWER            0.13f
#endif

/* The SENSORS Module */
struct sensor_t sSensorList[] = {
#ifdef CUSTOM_KERNEL_ACCELEROMETER
	{	//ACCELEROMETER
        .name       = "Accelerometer Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = ACCELERATION,
        .type       = SENSOR_TYPE_ACCELEROMETER,
        .maxRange   = RANGE_A,
        .resolution = CONVERT_100,
        .power      = 0.23f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = SENSOR_STRING_TYPE_ACCELEROMETER,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	}, 
#endif
#ifdef CUSTOM_KERNEL_MAGNETOMETER
	{	//Magnetic
        .name       = "Magnetic field Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = MAGNETIC,
        .type       = SENSOR_TYPE_MAGNETIC_FIELD,
        .maxRange   = 200.0f,
        .resolution = CONVERT_100,
        .power      = 6.8f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = SENSOR_STRING_TYPE_MAGNETIC_FIELD,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_GYROSCOPE
	{	//Gyroscope
        .name       = "Gyroscope Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = GYRO,
        .type       = SENSOR_TYPE_GYROSCOPE,
        .maxRange   = 40,
        .resolution = CONVERT_100,
        .power      = 6.1f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},
#endif
#ifdef CUSTOM_KERNEL_ALS
	{	//Light
        .name       = "Light Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = LIGHT,
        .type       = SENSOR_TYPE_LIGHT,
        .maxRange   = 10240.0f,
        .resolution = 1.0f,
        .power      = 0.13f,
        .minDelay	= 0,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_ON_CHANGE_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_PS
	{	//Proximity
        .name       = "Proximity Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = PROXIMITY,
        .type       = SENSOR_TYPE_PROXIMITY,
        .maxRange   = PROXIMITY_RANGE,
        .resolution = PROXIMITY_RESOLUTION,
        .power      = PROXIMITY_POWER,
        .minDelay	= 0,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_WAKE_UP | SENSOR_FLAG_ON_CHANGE_MODE,
	},
#endif
#ifdef CUSTOM_KERNEL_BAROMETER
	{	//Pressure
        .name       = "Pressure Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = PRESSURE,
        .type       = SENSOR_TYPE_PRESSURE,
        .maxRange   = 2000,
        .resolution = 1.0f,
        .power      = 6.1f,
        .minDelay	= 20000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_HEART
	{	//HeartRate
        .name       = "HeartRate Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = HEARTBEAT,
        .type       = SENSOR_TYPE_HEART_RATE,
        .maxRange   = 1000,
        .resolution = 1,
        .power      = 1,
        .minDelay	= 0,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = SENSOR_STRING_TYPE_HEART_RATE,
		.requiredPermission = SENSOR_PERMISSION_BODY_SENSORS,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_ON_CHANGE_MODE,
	},
#endif	
#ifdef CUSTOM_KERNEL_MAGNETOMETER
	{	//Orientation
        .name       = "Orientation Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = ORIENTATION,
        .type       = SENSOR_TYPE_ORIENTATION,
        .maxRange   = 360.0f,
        .resolution = 0.1f,
        .power      = 13.0f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_RV_SENSOR
	{	//Rotation Vector
        .name       = "Rotation Vector",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = ROTATIONVECTOR,
        .type       = SENSOR_TYPE_ROTATION_VECTOR,
        .maxRange   = 1.0f,
        .resolution = 1.0f / (1<<24),
        .power      = 6.1f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},
#endif
#ifdef CUSTOM_KERNEL_LINEARACCEL_SENSOR
	{	//Linear Acceleration
        .name       = "Linear Acceleration",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = LINEARACCELERATION,
        .type       = SENSOR_TYPE_LINEAR_ACCELERATION,
        .maxRange   = RANGE_A,
        .resolution = RESOLUTION_A,
        .power      = 0.2f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},
#endif
#ifdef CUSTOM_KERNEL_GRAVITY_SENSOR
	{	//Gravity
        .name       = "Gravity",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = GRAVITY,
        .type       = SENSOR_TYPE_GRAVITY,
        .maxRange   = GRAVITY_EARTH,
        .resolution = (4.0f*9.81f)/256.0f,
        .power      = 0.2f,
        .minDelay	= 20000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_STEP_COUNTER
	{	//Step Counter
        .name       = "Step Counter",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = STEP_COUNTER,
        .type       = SENSOR_TYPE_STEP_COUNTER,
        .maxRange   = 20000.0f,
        .resolution = 1.0f,
        .power      = 6.1f,
        .minDelay	= 0,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_ON_CHANGE_MODE,
	},	
	{	//Step Detector
        .name       = "Step Detector",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = STEP_DETECTOR,
        .type       = SENSOR_TYPE_STEP_DETECTOR,
        .maxRange   = 2.0f,
        .resolution = 1.0f,
        .power      = 0,
        .minDelay	= -1,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 0,
		.flags = SENSOR_FLAG_SPECIAL_REPORTING_MODE,
	},
#endif	
#ifdef CUSTOM_KERNEL_MAGNETOMETER
	{	//Uncalibrated Magnetic Field Sensor
        .name       = "Uncalibrated Magnetic Field Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = MAGNETIC_UNCALIBRATED,
        .type       = SENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED,
        .maxRange   = 200.0f,
        .resolution = CONVERT_100,
        .power      = 6.8f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif	
#ifdef CUSTOM_KERNEL_GYROSCOPE
	{	//Uncalibrated Gyroscope Sensor
        .name       = "Uncalibrated Gyroscope Sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = GYROSCOPE_UNCALIBRATED,
        .type       = SENSOR_TYPE_GYROSCOPE_UNCALIBRATED,
        .maxRange   = 40,
        .resolution = CONVERT_100,
        .power      = 6.1f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_GRV_SENSOR
	{	//Game Rotation Vector
        .name       = "Game Rotation Vector",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = GAME_ROTATION_VECTOR,
        .type       = SENSOR_TYPE_GAME_ROTATION_VECTOR,
        .maxRange   = 1.0f,
        .resolution = 1.0f / (1<<24),
        .power      = 6.1f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif
#ifdef CUSTOM_KERNEL_GMRV_SENSOR
	{	//Geomagnetic Rotation Vector
        .name       = "Geomagnetic Rotation Vector",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = GEOMAGNETIC_ROTATION_VECTOR,
        .type       = SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR,
        .maxRange   = 1.0f,
        .resolution = 1.0f / (1<<24),
        .power      = 6.1f,
        .minDelay	= 10000,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 10000,
		.flags = SENSOR_FLAG_CONTINUOUS_MODE,
	},	
#endif	
#ifdef CUSTOM_KERNEL_SIGNIFICANT_MOTION_SENSOR
	
	{	//Significant Motions
        .name       = "Significant Motions",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = SIGNIFICANT_MOTION,
        .type       = SENSOR_TYPE_SIGNIFICANT_MOTION,
        .maxRange   = 2.0f,
        .resolution = 1.0f,
        .power      = 0,
        .minDelay	= -1,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 0,
		.flags = SENSOR_FLAG_ONE_SHOT_MODE | SENSOR_FLAG_WAKE_UP,
	},
#endif	
#ifdef CUSTOM_KERNEL_BRINGTOSEE_SENSOR
	{	//Significant Tilt
        .name       = "Tilt sensor",
        .vendor     = "CyWee Group Ltd.",
        .version    = 1,
        .handle     = TILT,
        .type       = SENSOR_TYPE_TILT,
        .maxRange   = 2.0f,
        .resolution = 1.0f,
        .power      = 0,
        .minDelay	= -1,
        .fifoReservedEventCount = 0,
        .fifoMaxEventCount = 0,
		.stringType = NULL,
		.requiredPermission = NULL,
		.maxDelay = 0,
		.flags = SENSOR_FLAG_SPECIAL_REPORTING_MODE | SENSOR_FLAG_WAKE_UP,
	},
#endif	
};

int SensorListNum = sizeof(sSensorList)/sizeof(sSensorList[0]);

