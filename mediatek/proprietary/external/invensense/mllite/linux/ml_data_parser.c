/*
$License:
Copyright (C) 2014 InvenSense Corporation, All Rights Reserved.
$
*/

#undef MPL_LOG_NDEBUG
#define MPL_LOG_NDEBUG 0 /* Use 0 to turn on MPL_LOGV output */
#undef MPL_LOG_TAG
#define MPL_LOG_TAG "Sensor"

#include <stdint.h>
#include <endian.h>
#include <log.h>
#include "ml_data_parser.h"

#define FIFO_DATA_COMMON_SIZE						6
#define FIFO_DATA_ACCELEROMETER_SIZE				(3 * 4)
#define FIFO_DATA_MAGNETIC_FIELD_SIZE				6
#define FIFO_DATA_GYROSCOPE_SIZE				(3 * 4)
#define FIFO_DATA_LIGHT_SIZE						4
#define FIFO_DATA_PRESSURE_SIZE						4
#define FIFO_DATA_PROXIMITY_SIZE					2
#define FIFO_DATA_GRAVITY_SIZE						6
#define FIFO_DATA_LINEAR_ACCELERATION_SIZE			6
#define FIFO_DATA_ROTATION_VECTOR_SIZE				10
#define FIFO_DATA_RELATIVE_HUMIDITY_SIZE			1
#define FIFO_DATA_AMBIENT_TEMPERATURE_SIZE			2
#define FIFO_DATA_MAGNETIC_FIELD_UNCALIBRATED_SIZE	12
#define FIFO_DATA_GAME_ROTATION_VECTOR_SIZE			(4 * 4 + 2)
#define FIFO_DATA_GYROSCOPE_UNCALIBRATED_SIZE		12
#define FIFO_DATA_STEP_COUNTER_SIZE					8
#define FIFO_DATA_GEOMAGNETIC_ROTATION_VECTOR_SIZE	10
#define FIFO_DATA_HEART_RATE_SIZE					2
// TODO: #define FIFO_DATA_ACTIVITY_CLASSIFICATION_SIZE

#define FIFO_DATA_ID_INDEX							0
#define FIFO_DATA_ID_GET(a)							((a) & 0xFF)

#define FIFO_DATA_STATUS_INDEX						1
#define FIFO_DATA_STATUS_ACCURACY_GET(a)			((a) & 0x03)
#define FIFO_DATA_STATUS_STATUS_GET(a)				(((a) & 0x0C) >> 2)
#define FIFO_DATA_STATUS_SIZE_GET(a)				(((a) & 0x10) >> 4)
#define FIFO_DATA_STATUS_ANSWER_GET(a)				(((a) & 0x20) >> 5)

#define FIFO_DATA_TIMESTAMP_CMD_INDEX				2
/* this function is used to avoid segmentation fault since address may not be 4 byte aligned */
static int32_t inv_le_to_int32(const uint8_t *frame)
{
	return (frame[3] << 24) | (frame[2] << 16) | (frame[1] << 8) | frame[0];
}
static int16_t inv_le_to_int16(const uint8_t *frame)
{
	return (frame[1] << 8) | frame[0];
}

static int parse_common(const uint8_t *frame, size_t size, struct sensor_data_t *sensor_data)
{
	uint8_t data;

	if (size < FIFO_DATA_COMMON_SIZE)
		return -1;

	/* Parse header */
	data = frame[FIFO_DATA_ID_INDEX];
	sensor_data->id = FIFO_DATA_ID_GET(data);

	data = frame[FIFO_DATA_STATUS_INDEX];
	sensor_data->accuracy = FIFO_DATA_STATUS_ACCURACY_GET(data);
	sensor_data->status = FIFO_DATA_STATUS_STATUS_GET(data);
	sensor_data->size = FIFO_DATA_STATUS_SIZE_GET(data);
	sensor_data->answer = FIFO_DATA_STATUS_ANSWER_GET(data);

	/* Parse timestamp/cmd */
	sensor_data->timestamp = inv_le_to_int32(&frame[FIFO_DATA_TIMESTAMP_CMD_INDEX]);

	return 0;
}

static int parse_data_accelerometer(const uint8_t *frame, size_t size, union sensor_data *data)
{
	if (size < FIFO_DATA_ACCELEROMETER_SIZE)
		return -1;

	data->accelerometer.x = inv_le_to_int32(frame);
	data->accelerometer.y = inv_le_to_int32(frame + 4);
	data->accelerometer.z = inv_le_to_int32(frame + 8);

	return 0;
}

static int parse_data_magnetic_field(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read = (const int16_t *)frame;

	if (size < FIFO_DATA_MAGNETIC_FIELD_SIZE)
		return -1;

	data->magnetic_field.x = letoh16(read[0]);
	data->magnetic_field.y = letoh16(read[1]);
	data->magnetic_field.z = letoh16(read[2]);

	return 0;
}

static int parse_data_gyroscope(const uint8_t *frame, size_t size, union sensor_data *data)
{
	if (size < FIFO_DATA_GYROSCOPE_SIZE)
		return -1;

	data->gyroscope.x = inv_le_to_int32(frame);
	data->gyroscope.y = inv_le_to_int32(frame + 4);
	data->gyroscope.z = inv_le_to_int32(frame + 8);

	return 0;
}

static int parse_data_light(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const uint32_t *read = (const uint32_t *)frame;

	if (size < FIFO_DATA_LIGHT_SIZE)
		return -1;

	data->light = letoh32(read[0]);

	return 0;
}

static int parse_data_pressure(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const uint32_t *read = (const uint32_t *)frame;

	if (size < FIFO_DATA_PRESSURE_SIZE)
		return -1;

	data->pressure = letoh32(read[0]);

	return 0;
}

static int parse_data_proximity(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const uint16_t *read = (const uint16_t *)frame;

	if (size < FIFO_DATA_PROXIMITY_SIZE)
		return -1;

	data->proximity = letoh16(read[0]);

	return 0;
}

static int parse_data_gravity(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read = (const int16_t *)frame;

	if (size < FIFO_DATA_GRAVITY_SIZE)
		return -1;

	data->gravity.x = letoh16(read[0]);
	data->gravity.y = letoh16(read[1]);
	data->gravity.z = letoh16(read[2]);

	return 0;
}

static int parse_data_linear_acceleration(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read = (const int16_t *)frame;

	if (size < FIFO_DATA_LINEAR_ACCELERATION_SIZE)
		return -1;

	data->linear_acceleration.x = letoh16(read[0]);
	data->linear_acceleration.y = letoh16(read[1]);
	data->linear_acceleration.z = letoh16(read[2]);

	return 0;
}

static int parse_data_rotation_vector(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read1 = (const int16_t *)frame;
	const uint16_t *read2 = (const uint16_t *)&frame[8];

	if (size < FIFO_DATA_ROTATION_VECTOR_SIZE)
		return -1;

	data->rotation_vector.x = letoh16(read1[0]);
	data->rotation_vector.y = letoh16(read1[1]);
	data->rotation_vector.z = letoh16(read1[2]);
	data->rotation_vector.w = letoh16(read1[3]);
	data->rotation_vector.accuracy = letoh16(read2[0]);

	return 0;
}

static int parse_data_relative_humidity(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const uint8_t *read = (const uint8_t *)frame;

	if (size < FIFO_DATA_RELATIVE_HUMIDITY_SIZE)
		return -1;

	data->relative_humidity = read[0];

	return 0;
}

static int parse_data_ambient_temperature(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read = (const int16_t *)frame;

	if (size < FIFO_DATA_AMBIENT_TEMPERATURE_SIZE)
		return -1;

	data->ambient_temperature = letoh16(read[0]);

	return 0;
}

static int parse_data_magnetic_field_uncalibrated(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read = (const int16_t *)frame;

	if (size < FIFO_DATA_MAGNETIC_FIELD_UNCALIBRATED_SIZE)
		return -1;

	data->magnetic_field_uncalibrated.uncalib.x = letoh16(read[0]);
	data->magnetic_field_uncalibrated.uncalib.y = letoh16(read[1]);
	data->magnetic_field_uncalibrated.uncalib.z = letoh16(read[2]);
	data->magnetic_field_uncalibrated.bias.x = letoh16(read[3]);
	data->magnetic_field_uncalibrated.bias.y = letoh16(read[4]);
	data->magnetic_field_uncalibrated.bias.z = letoh16(read[5]);

	return 0;
}

static int parse_data_game_rotation_vector(const uint8_t *frame, size_t size, union sensor_data *data)
{
	if (size < FIFO_DATA_GAME_ROTATION_VECTOR_SIZE)
		return -1;

	data->game_rotation_vector.x = inv_le_to_int32(frame + 0);
	data->game_rotation_vector.y = inv_le_to_int32(frame + 4);
	data->game_rotation_vector.z = inv_le_to_int32(frame + 8);
	data->game_rotation_vector.w = inv_le_to_int32(frame + 12);
	data->game_rotation_vector.accuracy = inv_le_to_int16(frame + 14);

	return 0;
}

static int parse_data_gyroscope_uncalibrated(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read = (const int16_t *)frame;

	if (size < FIFO_DATA_GYROSCOPE_UNCALIBRATED_SIZE)
		return -1;

	data->gyroscope_uncalibrated.uncalib.x = letoh16(read[0]);
	data->gyroscope_uncalibrated.uncalib.y = letoh16(read[1]);
	data->gyroscope_uncalibrated.uncalib.z = letoh16(read[2]);
	data->gyroscope_uncalibrated.bias.x = letoh16(read[3]);
	data->gyroscope_uncalibrated.bias.y = letoh16(read[4]);
	data->gyroscope_uncalibrated.bias.z = letoh16(read[5]);

	return 0;
}

static int parse_data_step_counter(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const uint64_t *read = (const uint64_t *)frame;

	if (size < FIFO_DATA_STEP_COUNTER_SIZE)
		return -1;

	data->step_counter = letoh64(read[0]);

	return 0;
}

static int parse_data_geomagnetic_rotation_vector(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const int16_t *read1 = (const int16_t *)frame;
	const uint16_t *read2 = (const uint16_t *)&frame[8];

	if (size < FIFO_DATA_GEOMAGNETIC_ROTATION_VECTOR_SIZE)
		return -1;

	data->geomagnetic_rotation_vector.x = letoh16(read1[0]);
	data->geomagnetic_rotation_vector.y = letoh16(read1[1]);
	data->geomagnetic_rotation_vector.z = letoh16(read1[2]);
	data->geomagnetic_rotation_vector.w = letoh16(read1[3]);
	data->geomagnetic_rotation_vector.accuracy = letoh16(read2[0]);

	return 0;
}

static int parse_data_heart_rate(const uint8_t *frame, size_t size, union sensor_data *data)
{
	const uint16_t *read = (const uint16_t *)frame;

	if (size < FIFO_DATA_HEART_RATE_SIZE)
		return -1;

	data->heart_rate = letoh16(read[0]);

	return 0;
}

/* TODO: Activity Classification */
static int parse_data_activity_classification(const uint8_t *frame, size_t size, union sensor_data *data)
{
	(void)frame, (void)size, (void)data;
	return -1;
}


int get_sensor_data(const void *frame, size_t size, struct sensor_data_t *sensor_data)
{
	int ret;

	/* Parse common header */
	ret = parse_common(frame, size, sensor_data);
	if (ret != 0)
		return -1;

	frame += FIFO_DATA_COMMON_SIZE;
	size -= FIFO_DATA_COMMON_SIZE;

	/* Parse sensor specific data */
    MPL_LOGV("data_parser: sensor_data id=%d", sensor_data->id);
//	printf("sendid=%d\n", sensor_data->id);

	switch (sensor_data->id) {
	case SENSOR_ID_ACCELEROMETER:
	case SENSOR_ID_WAKE_UP_ACCELEROMETER:
		ret = parse_data_accelerometer(frame, size, &sensor_data->data);
		sensor_data->size = FIFO_DATA_ACCELEROMETER_SIZE + FIFO_DATA_COMMON_SIZE;
		break;
	case SENSOR_ID_MAGNETIC_FIELD:
	case SENSOR_ID_WAKE_UP_MAGNETIC_FIELD:
		ret = parse_data_magnetic_field(frame, size, &sensor_data->data);
		break;
	/* DEPRECATED: SENSOR_ID_ORIENTATION */
	case SENSOR_ID_GYROSCOPE:
	case SENSOR_ID_WAKE_UP_GYROSCOPE:
		ret = parse_data_gyroscope(frame, size, &sensor_data->data);
		sensor_data->size = FIFO_DATA_GYROSCOPE_SIZE + FIFO_DATA_COMMON_SIZE;
		break;
	case SENSOR_ID_LIGHT:
	case SENSOR_ID_WAKE_UP_LIGHT:
		ret = parse_data_light(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_PRESSURE:
	case SENSOR_ID_WAKE_UP_PRESSURE:
		ret = parse_data_pressure(frame, size, &sensor_data->data);
		break;
	/* DEPRECATED: SENSOR_ID_TEMPERATURE */
	case SENSOR_ID_PROXIMITY:
	case SENSOR_ID_NON_WAKE_UP_PROXIMITY:
		ret = parse_data_proximity(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_GRAVITY:
	case SENSOR_ID_WAKE_UP_GRAVITY:
		ret = parse_data_gravity(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_LINEAR_ACCELERATION:
	case SENSOR_ID_WAKE_UP_LINEAR_ACCELERATION:
		ret = parse_data_linear_acceleration(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_ROTATION_VECTOR:
	case SENSOR_ID_WAKE_UP_ROTATION_VECTOR:
		ret = parse_data_rotation_vector(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_RELATIVE_HUMIDITY:
	case SENSOR_ID_WAKE_UP_RELATIVE_HUMIDITY:
		ret = parse_data_relative_humidity(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_AMBIENT_TEMPERATURE:
	case SENSOR_ID_WAKE_UP_AMBIENT_TEMPERATURE:
		ret = parse_data_ambient_temperature(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_MAGNETIC_FIELD_UNCALIBRATED:
	case SENSOR_ID_WAKE_UP_MAGNETIC_FIELD_UNCALIBRATED:
		ret = parse_data_magnetic_field_uncalibrated(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_GAME_ROTATION_VECTOR:
	case SENSOR_ID_WAKE_UP_GAME_ROTATION_VECTOR:
		ret = parse_data_game_rotation_vector(frame, size, &sensor_data->data);
		sensor_data->size = FIFO_DATA_GAME_ROTATION_VECTOR_SIZE + FIFO_DATA_COMMON_SIZE;
		break;
	case SENSOR_ID_GYROSCOPE_UNCALIBRATED:
	case SENSOR_ID_WAKE_UP_GYROSCOPE_UNCALIBRATED:
		ret = parse_data_gyroscope_uncalibrated(frame, size, &sensor_data->data);
		sensor_data->size = FIFO_DATA_GYROSCOPE_UNCALIBRATED_SIZE + FIFO_DATA_COMMON_SIZE;
		break;
	case SENSOR_ID_SIGNIFICANT_MOTION:
		ret = 0;
		break;
	case SENSOR_ID_STEP_DETECTOR:
		ret = 0;
		break;
	case SENSOR_ID_STEP_COUNTER:
	case SENSOR_ID_WAKE_UP_STEP_COUNTER:
		ret = parse_data_step_counter(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_GEOMAGNETIC_ROTATION_VECTOR:
	case SENSOR_ID_WAKE_UP_GEOMAGNETIC_ROTATION_VECTOR:
		ret = parse_data_geomagnetic_rotation_vector(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_HEART_RATE:
	case SENSOR_ID_WAKE_UP_HEART_RATE:
		ret = parse_data_heart_rate(frame, size, &sensor_data->data);
		break;
	case SENSOR_ID_WAKE_UP_TILT_DETECTOR:
		ret = 0;
		break;
	case SENSOR_ID_ACTIVITY_CLASSIFICATION:
		ret = parse_data_activity_classification(frame, size, &sensor_data->data);
		break;
	default:
		ret = -1;
		break;
	}

    MPL_LOGV("data_parser: sensor_data size=%d", size);
	return ret;
}
