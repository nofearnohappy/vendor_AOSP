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

#include <fcntl.h>
#include <errno.h>
#include <math.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <stdlib.h>
#include <sys/select.h>
#include <cutils/log.h>
#include <utils/BitSet.h>
#include <math.h>

#include "CwMcuSensor.h"

/*****************************************************************************/

CwMcuSensor::CwMcuSensor()
    : SensorBase(NULL, "CwMcuSensor"),
      mEnabled(0),
      mInputReader(4),
      mHasPendingEvent(false){

    int temp_data[255];	
	int rc = 0;
    mPendingEvents[ACCELERATION].version = sizeof(sensors_event_t);
    mPendingEvents[ACCELERATION].sensor = ACCELERATION;
    mPendingEvents[ACCELERATION].type = SENSOR_TYPE_ACCELEROMETER;
	
    mPendingEvents[MAGNETIC].version = sizeof(sensors_event_t);
    mPendingEvents[MAGNETIC].sensor = MAGNETIC;
    mPendingEvents[MAGNETIC].type = SENSOR_TYPE_MAGNETIC_FIELD;
	
    mPendingEvents[GYRO].version = sizeof(sensors_event_t);
    mPendingEvents[GYRO].sensor = GYRO;
    mPendingEvents[GYRO].type = SENSOR_TYPE_GYROSCOPE;

	mPendingEvents[LIGHT].version = sizeof(sensors_event_t);
	mPendingEvents[LIGHT].sensor = LIGHT;
	mPendingEvents[LIGHT].type = SENSOR_TYPE_LIGHT;

	mPendingEvents[PROXIMITY].version = sizeof(sensors_event_t);
	mPendingEvents[PROXIMITY].sensor = PROXIMITY;
	mPendingEvents[PROXIMITY].type = SENSOR_TYPE_PROXIMITY;

    mPendingEvents[PRESSURE].version = sizeof(sensors_event_t);
    mPendingEvents[PRESSURE].sensor = PRESSURE;
    mPendingEvents[PRESSURE].type = SENSOR_TYPE_PRESSURE;
    mPendingEvents[PRESSURE].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[HEARTBEAT].version = sizeof(sensors_event_t);
    mPendingEvents[HEARTBEAT].sensor = HEARTBEAT;
    mPendingEvents[HEARTBEAT].type = SENSOR_TYPE_HEART_RATE;
    mPendingEvents[HEARTBEAT].heart_rate.status = SENSOR_STATUS_ACCURACY_HIGH;
	
    mPendingEvents[ORIENTATION].version = sizeof(sensors_event_t);
    mPendingEvents[ORIENTATION].sensor = ORIENTATION;
    mPendingEvents[ORIENTATION].type = SENSOR_TYPE_ORIENTATION;
    mPendingEvents[ORIENTATION].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[ROTATIONVECTOR].version = sizeof(sensors_event_t);
    mPendingEvents[ROTATIONVECTOR].sensor = ROTATIONVECTOR;
    mPendingEvents[ROTATIONVECTOR].type = SENSOR_TYPE_ROTATION_VECTOR;
    mPendingEvents[ROTATIONVECTOR].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[LINEARACCELERATION].version = sizeof(sensors_event_t);
    mPendingEvents[LINEARACCELERATION].sensor = LINEARACCELERATION;
    mPendingEvents[LINEARACCELERATION].type = SENSOR_TYPE_LINEAR_ACCELERATION;
    mPendingEvents[LINEARACCELERATION].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[GRAVITY].version = sizeof(sensors_event_t);
    mPendingEvents[GRAVITY].sensor = GRAVITY;
    mPendingEvents[GRAVITY].type = SENSOR_TYPE_GRAVITY;
    mPendingEvents[GRAVITY].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[MAGNETIC_UNCALIBRATED].version = sizeof(sensors_event_t);
    mPendingEvents[MAGNETIC_UNCALIBRATED].sensor = MAGNETIC_UNCALIBRATED;
    mPendingEvents[MAGNETIC_UNCALIBRATED].type = SENSOR_TYPE_MAGNETIC_FIELD_UNCALIBRATED;
    mPendingEvents[MAGNETIC_UNCALIBRATED].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[GYROSCOPE_UNCALIBRATED].version = sizeof(sensors_event_t);
    mPendingEvents[GYROSCOPE_UNCALIBRATED].sensor = GYROSCOPE_UNCALIBRATED;
    mPendingEvents[GYROSCOPE_UNCALIBRATED].type = SENSOR_TYPE_GYROSCOPE_UNCALIBRATED;
    mPendingEvents[GYROSCOPE_UNCALIBRATED].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[GAME_ROTATION_VECTOR].version = sizeof(sensors_event_t);
    mPendingEvents[GAME_ROTATION_VECTOR].sensor = GAME_ROTATION_VECTOR;
    mPendingEvents[GAME_ROTATION_VECTOR].type = SENSOR_TYPE_GAME_ROTATION_VECTOR;
    mPendingEvents[GAME_ROTATION_VECTOR].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[GEOMAGNETIC_ROTATION_VECTOR].version = sizeof(sensors_event_t);
    mPendingEvents[GEOMAGNETIC_ROTATION_VECTOR].sensor = GEOMAGNETIC_ROTATION_VECTOR;
    mPendingEvents[GEOMAGNETIC_ROTATION_VECTOR].type = SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
    mPendingEvents[GEOMAGNETIC_ROTATION_VECTOR].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[STEP_COUNTER].version = sizeof(sensors_event_t);
    mPendingEvents[STEP_COUNTER].sensor = STEP_COUNTER;
    mPendingEvents[STEP_COUNTER].type = SENSOR_TYPE_STEP_COUNTER;
    mPendingEvents[STEP_COUNTER].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;
    mPendingEvents[STEP_COUNTER].u64.step_counter = 0;

    mPendingEvents[STEP_DETECTOR].version = sizeof(sensors_event_t);
    mPendingEvents[STEP_DETECTOR].sensor = STEP_DETECTOR;
    mPendingEvents[STEP_DETECTOR].type = SENSOR_TYPE_STEP_DETECTOR;
    mPendingEvents[STEP_DETECTOR].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[SIGNIFICANT_MOTION].version = sizeof(sensors_event_t);
    mPendingEvents[SIGNIFICANT_MOTION].sensor = SIGNIFICANT_MOTION;
    mPendingEvents[SIGNIFICANT_MOTION].type = SENSOR_TYPE_SIGNIFICANT_MOTION;
    mPendingEvents[SIGNIFICANT_MOTION].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[TILT].version = sizeof(sensors_event_t);
    mPendingEvents[TILT].sensor = TILT;
    mPendingEvents[TILT].type = SENSOR_TYPE_TILT;
    mPendingEvents[TILT].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;


	mPendingEvents_flush.version = META_DATA_VERSION;
	mPendingEvents_flush.sensor = 0;
	mPendingEvents_flush.type = SENSOR_TYPE_META_DATA;

	HalInitialTime = 0;
	McuInitialTime = 0;
    PreTimeDiff_normal = 65536;
    PreTimeDiff_batch = 65536;

	ALOGI("--SENHAL-- fd =%d",data_fd);
    if (data_fd) {
        strcpy(input_sysfs_path, "/sys/class/input/");
        strcat(input_sysfs_path, input_name);
        strcat(input_sysfs_path, "/device/");
        input_sysfs_path_len = strlen(input_sysfs_path);
        enable(0, 1);
		strcpy(&input_sysfs_path[input_sysfs_path_len], "calibrator_cmd");        
		ALOGI("--SENHAL-- CYWEE input_sysfs_path %s\n",input_sysfs_path);			
		
		/*cw_calibrator(RapWmcu,ACCELERATION);	
		cw_calibrator(RapWmcu,MAGNETIC);	
		cw_calibrator(RapWmcu,GYRO);
        */
    }
}

CwMcuSensor::~CwMcuSensor() {
    if (mEnabled) {
        enable(0, 0);
    }
}

int CwMcuSensor::setInitialState() {

    return 0;
}

int CwMcuSensor::find_sensor(int32_t handle){
	int what = -1;
	return what;
}

int CwMcuSensor::enable(int32_t handle, int en) {

	int what = -1;
	int err = 0;
	int flags = en ? 1 : 0;
	int fd;
	char buf[10];
	what = handle;
	if (uint32_t(what) >= SENSORS_ID_END)
		return -EINVAL;
	strcpy(&input_sysfs_path[input_sysfs_path_len], "enable");
	fd = open(input_sysfs_path, O_RDWR);
	if (fd >= 0) {
		sprintf(buf, "%d %d\n", what, flags);		
		err = write(fd, buf, sizeof(buf));
		close(fd);
		mEnabled &= ~(1<<what);
		mEnabled |= (uint32_t(flags)<<what);
		sensors_time_mcu[handle] =0;
	}
	/*
	if (what == 1) {
		if(flags){
			cw_calibrator(RapWmcu,MAGNETIC);		
		}else{			
			cw_calibrator(RmcuWap,MAGNETIC);		
		}	
	}
	*/
	mEnabledTime[handle] = 0;
	
	ALOGI("--SENHAL-- enable() fd=%d sensors_id =%d enable =%d mEnabled=%d path=%s",fd ,what,flags,mEnabled,input_sysfs_path);
    return 0;
}

int CwMcuSensor::batch(int handle, int flags, int64_t period_ns, int64_t timeout)
{
	int what = -1;
	int fd = 0;
	char buf[128] = {0};
	int err = 0;
	int delay_ms = 0;
	int timeout_ms = 0;
	bool dryRun = false;

	ALOGI("--SENHAL-- %s in\n", __func__);

	what = handle;
	delay_ms = period_ns/1000000;
	timeout_ms = timeout/1000000;

	if(flags == SENSORS_BATCH_DRY_RUN)
	{
		ALOGI("--SENHAL-- SENSORS_BATCH_DRY_RUN~!!\n");
		dryRun = true;
	}

	if (uint32_t(what) >= SENSORS_ID_END)
		return -EINVAL;

	if(flags == SENSORS_BATCH_WAKE_UPON_FIFO_FULL)
	{
		ALOGI("--SENHAL-- SENSORS_BATCH_WAKE_UPON_FIFO_FULL~!!\n");
	}


	strcpy(&input_sysfs_path[input_sysfs_path_len], "batch");

	fd = open(input_sysfs_path, O_RDWR);

	if(fd >= 0)
	{
		sprintf(buf, "%d %d %d %d\n", what, flags, delay_ms, timeout_ms);
		err = write(fd, buf, sizeof(buf));
		close(fd);
	}

	ALOGI("--SENHAL-- batch() fd=%d, sensors_id =%d, flags =%d, delay_ms= %d, timeout = %d, path=%s\n",fd , what, flags, delay_ms, timeout_ms, input_sysfs_path);

	return 0;
}

int CwMcuSensor::flush(int handle)
{
	int what = -1;
	int fd = 0;
	char buf[10] = {0};
	int err = 0;

	what = handle;

	if (uint32_t(what) >= SENSORS_ID_END)
		return -EINVAL;

	strcpy(&input_sysfs_path[input_sysfs_path_len], "flush");

	fd = open(input_sysfs_path, O_RDWR);

	if(fd >= 0)
	{
		sprintf(buf, "%d\n", what);
		err = write(fd, buf, sizeof(buf));
		close(fd);
	}

	ALOGI("--SENHAL-- flush() fd=%d, sensors_id =%d, path=%s\n",fd , what, input_sysfs_path);

	return 0;
}

bool CwMcuSensor::hasPendingEvents() const {
    return mHasPendingEvent;
}

int CwMcuSensor::setDelay(int32_t handle, int64_t delay_ns)
{
	char buf[80];
    int fd;
    int what = 0;
    int delay_ms = (int)delay_ns/1000000;
	what = handle;
		if (uint32_t(what) >= SENSORS_ID_END)
			return -EINVAL;
		strcpy(&input_sysfs_path[input_sysfs_path_len], "delay_ms");
		fd = open(input_sysfs_path, O_RDWR);
		if (fd >= 0) {
			sprintf(buf, "%d %d\n", what, delay_ms);		
			write(fd, buf, strlen(buf)+1);
			close(fd);
		}
		ALOGI("--SENHAL-- setDelay() sensors_id =%d delay_ms =%d path=%s",what,delay_ms,input_sysfs_path);
    return 0;
}

int CwMcuSensor::readEvents(sensors_event_t* data, int count)
{
	static int64_t prev_time;
	int64_t time_temp = 0;
	if (count < 1)
		return -EINVAL;
	ssize_t n = mInputReader.fill(data_fd);
	if (n < 0)
		return n;

	int numEventReceived = 0;
	input_event const* event;

	while (count && mInputReader.readEvent(&event)) {
		if (event->type == EV_ABS) {
			processEvent(event->code, event->value);
		} else if (event->type == EV_SYN) {
			int64_t time = getTimestamp();
			for (int j=0 ; count && mPendingMask && j<SENSORS_ID_END ; j++) {
				if (mPendingMask & (1<<j)) {
					mPendingMask &= ~(1<<j);
					if(j==LIGHT|| j==PROXIMITY || j==STEP_COUNTER || j==STEP_DETECTOR || j==HEARTBEAT || j==TILT){
						mPendingEvents[j].timestamp = time;
					}
					if (mEnabled & (1<<j)) {
						*data++ = mPendingEvents[j];
						count--;
						numEventReceived++;
						//ALOGI("--SENHAL-- [readEvents] send event~(%d)!! \n", numEventReceived);
					}
				}
			}
			if(flush_event ==1)
			{
				ALOGI("--SENHAL-- send flush event~!!\n");
				flush_event = 0;
				mPendingEvents_flush.timestamp = time;
				*data++ = mPendingEvents_flush;
				count--;
				numEventReceived++;
			}
		} else {}
		mInputReader.next();
	}
	return numEventReceived;
}
void CwMcuSensor::processEvent(int code, int value){
	uint8_t sensorsid = 0;
	int index = 0;
	int16_t data = 0;
	uint8_t data_temp[2];
	int64_t time = 0;
	int64_t time_temp;

	sensorsid = (uint8_t)((uint32_t )value >> 16);
	data |= value;

	switch (code) {
		case CW_ABS_X:
			index = 0;
		break;
		case CW_ABS_Y:
			index = 1;
		break;
		case CW_ABS_Z:
			index = 2;
		break;
		case CW_ABS_X1:
			index = 3;
		break;
		case CW_ABS_Y1:
			index = 4;
		break;
		case CW_ABS_Z1:
			index = 5;
		break;
		case CW_ABS_TIMEDIFF:
			index = 6;
			time_mcu[sensorsid] = data;

			if(PreTimeDiff_normal > data){
                                cw_synctimestamp_normal();
			}
            PreTimeDiff_normal = data;
				
			sensors_time_mcu[sensorsid] = (int64_t)((uint64_t)McuTimeBase_normal+ (uint64_t)time_mcu[sensorsid])*1000000; 
			time_temp = mPendingEvents[sensorsid].timestamp;
			mPendingEvents[sensorsid].timestamp = calculate_offset(sensors_time_mcu[sensorsid]);			

			return;
			break;
		case CW_ABS_TIMEDIFF_WAKE_UP:
			index = 7;
			time_mcu[sensorsid] = data;
						
			if(PreTimeDiff_batch > data){
                                cw_synctimestamp_batch();
			} 
            PreTimeDiff_batch = data;
						
			sensors_time_mcu[sensorsid] = (int64_t)((uint64_t)McuTimeBase_batch + (uint64_t)time_mcu[sensorsid])*1000000; 
			time_temp = mPendingEvents[sensorsid].timestamp;
			mPendingEvents[sensorsid].timestamp = calculate_offset(sensors_time_mcu[sensorsid]);			

			return;
		break;
		case CW_ABS_ACCURACY:
			ALOGI("--SENHAL--mPendingEvents[sensorsid].orientation.status = %d", data);
			mPendingEvents[MAGNETIC].orientation.status = data;
			mPendingEvents[ORIENTATION].orientation.status = data;
			mPendingEvents[MAGNETIC_UNCALIBRATED].orientation.status = data;
			return;
		break;
		case CW_ABS_TIMEBASE:
			cw_synctimestamp();
			McuTimeBase_normal = value;
			return;
		break;
		case CW_ABS_TIMEBASE_WAKE_UP:
                        cw_synctimestamp();
			McuTimeBase_batch = value;
            return;
		break;
		default:
			return;
	}

	switch (sensorsid) {
		case LIGHT:
		case PROXIMITY:
		case HEARTBEAT:
			mPendingMask |= 1<<sensorsid;
			if (index == 0) {
				mPendingEvents[sensorsid].heart_rate.bpm = (float)data;
				mPendingEvents[sensorsid].heart_rate.status= SENSOR_STATUS_ACCURACY_HIGH;
			}
			break;
		case PRESSURE:
		case ORIENTATION:
			mPendingMask |= 1<<sensorsid;
			mPendingEvents[sensorsid].data[index] = (float)data	* CONVERT_10;
			break;
		case ACCELERATION:
		case GYRO:
		case LINEARACCELERATION:
		case GRAVITY:
			mPendingMask |= 1<<sensorsid;
			mPendingEvents[sensorsid].data[index] = (float)data	* CONVERT_100;
			break;
		case TILT:		
			mPendingMask |= 1 << TILT;
			mPendingEvents[TILT].data[index] = (float)data * CONVERT_100;
			ALOGI("--SENHAL--processEvent : tilt wakeup (%f,%f,%f)", mPendingEvents[sensorsid].acceleration.x, mPendingEvents[sensorsid].acceleration.y, mPendingEvents[sensorsid].acceleration.z);
			break;
		case MAGNETIC:
		case MAGNETIC_UNCALIBRATED:
			mPendingMask |= 1<<sensorsid;
			mPendingEvents[sensorsid].data[index] = (float)data	* CONVERT_100;
			break;
		case MAGNETIC_UNCALIBRATED_BIAS:
				mPendingMask |= 1<<MAGNETIC_UNCALIBRATED;
				mPendingEvents[MAGNETIC_UNCALIBRATED].data[index+3] = (float)data	* CONVERT_100;
			break;

		case GYRO_UNCALIBRATED_BIAS:
			mPendingMask |= 1<<GYRO_UNCALIBRATED_BIAS;
			mPendingEvents[GYRO_UNCALIBRATED_BIAS].data[index+3] = (float)data	* CONVERT_100;
			break;			
			
		case ROTATIONVECTOR:
		case GAME_ROTATION_VECTOR:
		case GEOMAGNETIC_ROTATION_VECTOR:
			mPendingMask |= 1<<sensorsid;
			mPendingEvents[sensorsid].data[index] = (float)data	* CONVERT_10000;
			if (index == 2) {
					mPendingEvents[sensorsid].data[3] =
						1 - mPendingEvents[sensorsid].data[0]*mPendingEvents[sensorsid].data[0]
							- mPendingEvents[sensorsid].data[1]*mPendingEvents[sensorsid].data[1]
							- mPendingEvents[sensorsid].data[2]*mPendingEvents[sensorsid].data[2];
					mPendingEvents[sensorsid].data[3] = (mPendingEvents[sensorsid].data[3] > 0) ? (float)sqrt(mPendingEvents[sensorsid].data[3]) : 0;
				}
			//ALOGI("--SENHAL-- sensors_id = %d, index = %d, data= %f", sensorsid, 3, mPendingEvents[sensorsid].data[3]);
			break;
		case GYROSCOPE_UNCALIBRATED:
			mPendingMask |= 1<<sensorsid;
			mPendingEvents[sensorsid].data[index] = (float)data	* CONVERT_100;
			break;
		case STEP_COUNTER:
			if (index == 0) {
				mPendingMask |= 1<<(sensorsid);
				pedometer_l = (uint16_t)data;
			} else if (index == 1) {
				mPendingMask |= 1<<(sensorsid);
				pedometer_h = (uint16_t)data;
			}
				mPendingEvents[sensorsid].u64.step_counter = (((uint64_t)pedometer_h)<<16) | (uint64_t)pedometer_l;
				ALOGI("--SENHAL-- sensors_id=%d,data=%lld", sensorsid, mPendingEvents[sensorsid].u64.step_counter);
			break;
		case STEP_DETECTOR:
		case SIGNIFICANT_MOTION:
			if (index == 0) {
				mPendingMask |= 1<<(sensorsid);
				pedometerd_l = (uint16_t)data;
			}
			mPendingEvents[sensorsid].data[0] =(float)pedometerd_l;
				mPendingEvents[sensorsid].data[1] = 0;
				mPendingEvents[sensorsid].data[2] = 0;
			ALOGI("--SENHAL-- sensors_id=%d,data=%f,%u\n", sensorsid, mPendingEvents[sensorsid].data[0],pedometerd_l);
			break;
		case META_DATA:
			mPendingEvents_flush.meta_data.what = META_DATA_FLUSH_COMPLETE;
			mPendingEvents_flush.meta_data.sensor = data;
			flush_event = 1;
			ALOGI("--SENHAL-- sensors_id=%d,meta_data.what=%d, meta_data.sensor =%d", sensorsid, mPendingEvents_flush.meta_data.what, mPendingEvents_flush.meta_data.sensor);
			break;
		case TimestampSync:
			CpuTimeBaseTemp = getTimestamp();
			ALOGI("--SENHAL-- CwMcuTime:TimestampSync:%lld\n",CpuTimeBaseTemp);
			break;
		case CALIBRATOR_UPDATE:
			//data_temp[0] = (uint8_t)data;
			//data_temp[1] = (uint8_t)(data>>8);
			//cw_calibrator(RmcuWap,data_temp[0]);
			//ALOGI("--SENHAL-- :CALIBRATOR_UPDATE:Id:%u,Status%u\n",data_temp[0],data_temp[1]);
			break;
		case MCU_REINITIAL:
			//cw_calibrator(RapWmcu,ACCELERATION);
			//cw_calibrator(RapWmcu,MAGNETIC);
			//cw_calibrator(RapWmcu,GYRO);		
			ALOGI("--SENHAL-- :MCU_REINITIAL\n");
			PreCpu_Mcu_time_offset = 0;
			break;
		default:
			break;
	}
}

int CwMcuSensor::cw_calibrator(CalibratorCmd cmd, int id) {
	int temp_data[255];
	int rc = 0;
	 if (data_fd) {
		strcpy(input_sysfs_path, "/sys/class/input/");
		strcat(input_sysfs_path, input_name);
		strcat(input_sysfs_path, "/device/");
		input_sysfs_path_len = strlen(input_sysfs_path);
	}
	
	switch (id) {
		case ACCELERATION:
			strcpy(&input_sysfs_path[input_sysfs_path_len], "calibrator_cmd0");
		ALOGI("--SENHAL-- CYWEE input_sysfs_path %s\n",input_sysfs_path);
			if (cmd == RapWmcu){
				rc = cw_read_calibrator_file(id, SAVE_PATH_ACC, temp_data);
				if ((rc>=0) && (temp_data[0]  ==1)) {
					cw_save_calibrator_sysfs(id, input_sysfs_path, temp_data);
			}
			}else if(cmd == RmcuWap){
				cw_read_calibrator_sysfs(id, input_sysfs_path, temp_data);
				if(temp_data[0] !=255 ){	//i2c read mcu fail == 255
					temp_data[0] = 1;
					cw_save_calibrator_file(id, SAVE_PATH_ACC, temp_data);
		}
	}
		break;
		case MAGNETIC:
			strcpy(&input_sysfs_path[input_sysfs_path_len], "calibrator_cmd1");
			ALOGI("--SENHAL-- CYWEE input_sysfs_path %s\n",input_sysfs_path);
			if(cmd == RmcuWap){	
				cw_read_calibrator_sysfs(id, input_sysfs_path, temp_data);
				if(temp_data[0] !=255 ){	//i2c read mcu fail == 255
					if(temp_data[4] >=1){	//accuracy >1
						temp_data[0] = 1;
						cw_save_calibrator_file(id, SAVE_PATH_MAG, temp_data);
					}
				}
			}else if (cmd == RapWmcu){
				rc = cw_read_calibrator_file(id, SAVE_PATH_MAG, temp_data);
				if ((rc>=0) && (temp_data[0]  ==1)) {
					if(temp_data[4] >=1){	//accuracy >1
						cw_save_calibrator_sysfs(id, input_sysfs_path, temp_data);
					}
				}
			}
		break;
		case GYRO:
			strcpy(&input_sysfs_path[input_sysfs_path_len], "calibrator_cmd2");
			ALOGI("--SENHAL-- CYWEE input_sysfs_path %s\n",input_sysfs_path);
			if (cmd == RapWmcu){
				rc = cw_read_calibrator_file(id, SAVE_PATH_GYRO, temp_data);
				if ((rc>=0) && (temp_data[0]  ==1)) {
					cw_save_calibrator_sysfs(id, input_sysfs_path, temp_data);
				}
			}else if(cmd == RmcuWap){
				cw_read_calibrator_sysfs(id, input_sysfs_path, temp_data);
				if(temp_data[0] !=255 ){	//i2c read mcu fail == 255
					temp_data[0] = 1;
					cw_save_calibrator_file(id, SAVE_PATH_GYRO, temp_data);
				}
			}
		break;
		case LIGHT:
		break;
		case PROXIMITY:
		break;
		case PRESSURE:
		break;
		case HEARTBEAT:
			break;
	}
	return 0;
}

int CwMcuSensor::cw_synctimestamp(void) {
	char temp_data[1024];
	int err =0;
	int64_t time = 0;
	int64_t mcutime_normal = 0;
	int64_t mcutime_batch = 0;

	strcpy(&input_sysfs_path[input_sysfs_path_len], "timestamp");
	if(cw_read_sysfs( input_sysfs_path, temp_data, sizeof(temp_data)) >=0){
		err = (int)((int8_t)temp_data[12]);
		if(err == 0){
			
			McuTimeStamp = ((uint32_t)temp_data[3])<<24 | ((uint32_t)temp_data[2])<<16 | ((uint32_t)temp_data[1])<<8 | ((uint32_t)temp_data[0]);
		
			time = getTimestamp();
			Cpu_Mcu_time_offset = time - ((int64_t)McuTimeStamp*1000000);
			if(PreCpu_Mcu_time_offset == 0){
				PreCpu_Mcu_time_offset = Cpu_Mcu_time_offset;
				McuTimeBase_normal = ((uint32_t)temp_data[7])<<24 | ((uint32_t)temp_data[6])<<16 | ((uint32_t)temp_data[5])<<8 | ((uint32_t)temp_data[4]);						
				McuTimeBase_batch = ((uint32_t)temp_data[11])<<24 | ((uint32_t)temp_data[10])<<16 | ((uint32_t)temp_data[9])<<8 | ((uint32_t)temp_data[8]);

                        }else{
                                Cpu_Mcu_time_offset_dy = Cpu_Mcu_time_offset - PreCpu_Mcu_time_offset;
				if(Cpu_Mcu_time_offset_dy <0){
					Cpu_Mcu_time_offset_dy  -= 100000000;
				}
				if(abs(Cpu_Mcu_time_offset_dy) >500000000 ){
                                        PreCpu_Mcu_time_offset = Cpu_Mcu_time_offset;
                                        Cpu_Mcu_time_offset_dy = 0; 
                                }
                        }
			
		}else{
			return -1;
		}
	}
	return 0;
}

int CwMcuSensor::cw_synctimestamp_normal(void) {
	char temp_data[1024];
	int err =0;
	int64_t time = 0;
	uint32_t mcutime_normal = 0;
	uint32_t mcutime_batch = 0;

	strcpy(&input_sysfs_path[input_sysfs_path_len], "timestamp");
	if(cw_read_sysfs( input_sysfs_path, temp_data, sizeof(temp_data)) >=0){
		err = (int)((int8_t)temp_data[12]);
		if(err == 0){
			
			McuTimeStamp = ((uint32_t)temp_data[3])<<24 | ((uint32_t)temp_data[2])<<16 | ((uint32_t)temp_data[1])<<8 | ((uint32_t)temp_data[0]);
			McuTimeBase_normal = ((uint32_t)temp_data[7])<<24 | ((uint32_t)temp_data[6])<<16 | ((uint32_t)temp_data[5])<<8 | ((uint32_t)temp_data[4]);						
			McuTimeBase_batch = ((uint32_t)temp_data[11])<<24 | ((uint32_t)temp_data[10])<<16 | ((uint32_t)temp_data[9])<<8 | ((uint32_t)temp_data[8]);
			
			time = getTimestamp();
			Cpu_Mcu_time_offset = time - ((int64_t)McuTimeStamp*1000000);
			if(PreCpu_Mcu_time_offset == 0){
	
                                PreCpu_Mcu_time_offset = Cpu_Mcu_time_offset;
                        }else{
                                Cpu_Mcu_time_offset_dy = Cpu_Mcu_time_offset - PreCpu_Mcu_time_offset;
				if(Cpu_Mcu_time_offset_dy <0){
					Cpu_Mcu_time_offset_dy  -= 100000000;
				}
				if(abs(Cpu_Mcu_time_offset_dy) >500000000 ){
                                        PreCpu_Mcu_time_offset = Cpu_Mcu_time_offset;
                                        Cpu_Mcu_time_offset_dy = 0; 
                                }
			
                        }
			mcutime_normal = McuTimeStamp - McuTimeBase_normal;
			CpuTimeBase_normal = time - ((int64_t)mcutime_normal*1000000);
			
		}else{
			return -1;
		}
	}
	return 0;
}

int CwMcuSensor::cw_synctimestamp_batch(void) {
	char temp_data[1024];
	int err =0;
	int64_t time = 0;
	uint32_t mcutime_normal = 0;
	uint32_t mcutime_batch = 0;

	strcpy(&input_sysfs_path[input_sysfs_path_len], "timestamp");
	if(cw_read_sysfs( input_sysfs_path, temp_data, sizeof(temp_data)) >=0){
		err = (int)((int8_t)temp_data[12]);
		if(err == 0){
			
			McuTimeStamp = ((uint32_t)temp_data[3])<<24 | ((uint32_t)temp_data[2])<<16 | ((uint32_t)temp_data[1])<<8 | ((uint32_t)temp_data[0]);
			McuTimeBase_normal = ((uint32_t)temp_data[7])<<24 | ((uint32_t)temp_data[6])<<16 | ((uint32_t)temp_data[5])<<8 | ((uint32_t)temp_data[4]);						
			McuTimeBase_batch = ((uint32_t)temp_data[11])<<24 | ((uint32_t)temp_data[10])<<16 | ((uint32_t)temp_data[9])<<8 | ((uint32_t)temp_data[8]);
			
			time = getTimestamp();
			Cpu_Mcu_time_offset = time - ((int64_t)McuTimeStamp*1000000);
			if(PreCpu_Mcu_time_offset == 0){
	
                                PreCpu_Mcu_time_offset = Cpu_Mcu_time_offset;
                        }else{
                                Cpu_Mcu_time_offset_dy = Cpu_Mcu_time_offset - PreCpu_Mcu_time_offset;
				if(Cpu_Mcu_time_offset_dy <0){
					Cpu_Mcu_time_offset_dy  -= 100000000;
				}
				if(abs(Cpu_Mcu_time_offset_dy) >500000000 ){
                                        PreCpu_Mcu_time_offset = Cpu_Mcu_time_offset;
                                        Cpu_Mcu_time_offset_dy = 0; 
                                }
			
                        }

			mcutime_batch = McuTimeStamp - McuTimeBase_batch;
			CpuTimeBase_batch = time - (mcutime_batch*1000000);			
		}else{
			return -1;
		}
	}
	return 0;
}

int64_t CwMcuSensor::calculate_offset(int64_t time){
     
        if(abs(time -PreOffsetDyUpdateTime) > 20000000){
                if(Cpu_Mcu_time_offset_dy >1000000){
                        Cpu_Mcu_time_offset_dy -=1000000;
                        PreCpu_Mcu_time_offset +=1000000;
                }else if(Cpu_Mcu_time_offset_dy < -1000000){
                        Cpu_Mcu_time_offset_dy +=1000000;
                        PreCpu_Mcu_time_offset -=1000000;
                }
                PreOffsetDyUpdateTime = time;
        }
        return time + PreCpu_Mcu_time_offset;
}

int CwMcuSensor::cw_read_sysfs(char *path, char *data, int size){
	int fd = 0;
	int readBytes = 0;
	if ((fd = open(path, O_RDONLY)) < 0) {
		ALOGE("--SENHAL--  Can't open '%s': %s", path, strerror(errno));
		return -1;
	}

	if ((readBytes = read(fd, data, size)) < 0) {
		close(fd);
		return -1;
	}
	ALOGI("CwHal: readBytes:%d\n",readBytes);
	close(fd);
	return readBytes;
}

int CwMcuSensor::cw_read_calibrator_sysfs(int cmd, char *path, int *calib)
{
	int fd = 0;
	char value[1024];
	int readBytes = 0;
	if ((fd = open(path, O_RDONLY)) < 0) {
		ALOGE("--SENHAL--  Can't open '%s': %s", path, strerror(errno));
		return -1;
	}

	if ((readBytes = read(fd, value, sizeof(value))) < 0) {
		close(fd);
		return -1;
	}

	sscanf(value, "%d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
		&calib[0],  &calib[1],  &calib[2],
		&calib[3],  &calib[4],  &calib[5],  &calib[6],  &calib[7],  &calib[8],  &calib[9],  &calib[10], &calib[11], &calib[12],
		&calib[13], &calib[14], &calib[15], &calib[16], &calib[17], &calib[18], &calib[19], &calib[20], &calib[21], &calib[22],
		&calib[23], &calib[24], &calib[25], &calib[26], &calib[27], &calib[28], &calib[29], &calib[30], &calib[31], &calib[32]);

	for (int i = 0; i < 33; i++) {
		ALOGI("--SENHAL-- read sysfs calib[%d] = %d\n", i, calib[i]);
	}
	close(fd);
	return 0;
}

int CwMcuSensor::cw_save_calibrator_sysfs(int cmd, char *path, int *calib)
{
	int i = 0;
	uint32_t data[33]={0};
	int fd = 0;
	int error =0;
	char value[1024];//255
	calib[0] = cmd;
	calib[1] = 0;
	calib[2] = 0;

	ALOGI("--SENHAL-- value = %s\n", value);
	ALOGI("--SENHAL-- value size = %d\n", sizeof(value));

	if ((fd = open(path, O_WRONLY)) < 0) {
		ALOGE("--SENHAL--  Can't open '%s': %s", path, strerror(errno));
		return -1;
	}
	snprintf(value, sizeof(value), "%d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
		calib[0],  calib[1],  calib[2],
		calib[3],  calib[4],  calib[5],  calib[6],  calib[7],  calib[8],  calib[9],  calib[10], calib[11], calib[12],
		calib[13], calib[14], calib[15], calib[16], calib[17], calib[18], calib[19], calib[20], calib[21], calib[22],
		calib[23], calib[24], calib[25], calib[26], calib[27], calib[28], calib[29], calib[30], calib[31], calib[32]);
	if (write(fd, value, sizeof(value)) < 0) {
		ALOGE("--SENHAL-- save calib to sysfs error: %d %s\n",fd, strerror(errno));
		close(fd);
		return -1;
	}
	close(fd);
	return 0;
}

int CwMcuSensor::cw_set_command_sysfs(int cmd, char *path)
{

	int i = 0;
	uint32_t data[33]={0};
	int fd = 0;
	int error =0;
	char value[1024];//255
	int calib[10];
	calib[0] = cmd;
	calib[1] = 0;
	calib[2] = 0;

	ALOGI("--SENHAL-- value = %s\n", value);
	ALOGI("--SENHAL-- value size = %d\n", sizeof(value));

	if ((fd = open(path, O_WRONLY)) < 0) {
		ALOGE("--SENHAL--  Can't open '%s': %s", path, strerror(errno));
		return -1;
	}

	snprintf(value, sizeof(value), "%d %d %d", calib[0], calib[1], calib[2]);
	if (write(fd, value, sizeof(value)) < 0) {
		ALOGE("--SENHAL-- save calib to sysfs error: %d %s\n",fd, strerror(errno));
		close(fd);
		return -1;
	}
	close(fd);
	return 0;
}

int CwMcuSensor::cw_read_calibrator_file(int cmd, char *path, int *calib)
{
	FILE *fp;
	int readBytes = 0;

	ALOGI("--SENHAL-- %s in\n", __func__);

	fp = fopen(path, "r");
	if (!fp) {
		ALOGE("--SENHAL-- open File failed: %s\n", strerror(errno));
		return -1;
	}
	rewind(fp);

	readBytes = fscanf(fp, "%d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
		&calib[0],  &calib[1],  &calib[2],
		&calib[3],  &calib[4],  &calib[5],  &calib[6],  &calib[7],  &calib[8],  &calib[9],  &calib[10], &calib[11], &calib[12],
		&calib[13], &calib[14], &calib[15], &calib[16], &calib[17], &calib[18], &calib[19], &calib[20], &calib[21], &calib[22],
		&calib[23], &calib[24], &calib[25], &calib[26], &calib[27], &calib[28], &calib[29], &calib[30], &calib[31], &calib[32]);

	if (readBytes <= 0) {
		ALOGE("--SENHAL-- Read file error:%s\n", strerror(errno));
		fclose(fp);
		return readBytes;
	} else {
		for (int i = 0; i < 33; i++) {
			ALOGI("--SENHAL-- calib[%d] = %d\n", i, calib[i]);
		}
	}
	fclose(fp);

		ALOGI("--SENHAL-- %s out\n", __func__);

	return 	0;
}

int CwMcuSensor::cw_save_calibrator_file(int cmd, char *path, int *calib)
{
	FILE *fp = NULL;
	int readBytes = 0;
	char value[1024];

	fp = fopen(path, "w+");
	if(!fp) {
		ALOGE("--SENHAL-- open calibrator file error!\n");
		return -1;
	}

	snprintf(value, sizeof(value), "%d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d",
			calib[0],  calib[1],  calib[2],
			calib[3],  calib[4],  calib[5],  calib[6],  calib[7],  calib[8],  calib[9],  calib[10], calib[11], calib[12],
			calib[13], calib[14], calib[15], calib[16], calib[17], calib[18], calib[19], calib[20], calib[21], calib[22],
			calib[23], calib[24], calib[25], calib[26], calib[27], calib[28], calib[29], calib[30], calib[31], calib[32]);

	readBytes = fprintf(fp,"%s",value);

	if (readBytes <= 0) {
		ALOGE("--SENHAL-- write calibrator file error!\n");
		fclose(fp);
		return -1;
	}

	fclose(fp);
	return 0;
}
