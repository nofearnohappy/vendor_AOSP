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

#include <fcntl.h>
#include <errno.h>
#include <math.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/select.h>
#include <string.h>

#include <cutils/log.h>
#include "BatchSensor.h"

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "BATCH"
#endif
#define SYSFS_PATH           "/sys/class/input"


/*****************************************************************************/
BatchSensor::BatchSensor()
    : SensorBase("/dev/m_batch_misc", "m_batch_input"),//BATCH_INPUTDEV_NAME
      mEnabled(0),
      mInputReader(128),//temp value for 128
      mHasPendingEvent(false)
{
        int handle=0;
        mPendingEvent.version = sizeof(sensors_event_t);
        mPendingEvent.sensor = 0;
        mPendingEvent.type = SENSOR_TYPE_META_DATA;
        memset(mPendingEvent.data, 0x00, sizeof(mPendingEvent.data));
    mPendingEvent.flags = 0;
    mPendingEvent.reserved0 = 0;
        mEnabledTime =0;
    mTimestampHi = 0;

        mPendingEvent.timestamp =0;
        mdata_fd = FindDataFd();
        if (mdata_fd >= 0) {
        strcpy(input_sysfs_path, "/sys/class/misc/m_batch_misc/");
        input_sysfs_path_len = strlen(input_sysfs_path);
        }
    char datapath[64]={"/sys/class/misc/m_batch_misc/batchactive"};
    int fd = open(datapath, O_RDWR);
    char buf[64];
    int len;

    for(int i=0;i<ID_SENSOR_MAX_HANDLE;i++)
    {
        flushSensorReq[i] = 0;
    }

    if (fd >= 0)
    {
        for(int i=0; i<numSensors;i++)
        {
            lseek(fd,0,SEEK_SET);
            sprintf(buf, "%d,%d", i, 2);//write 2 means notify driver I want to read whitch handle
            write(fd, buf, strlen(buf)+1);
            lseek(fd,0,SEEK_SET);
            len = read(fd,buf,sizeof(buf)-1);
            if(len<=0)
            {
                ALOGD("read div err, i = %d, len = %d", i, len);
            }
            else
            {
                buf[len] = '\0';
                sscanf(buf, "%d", &mDataDiv[i]);
                ALOGD("read div buf(%s)", datapath);
                ALOGD("fwq!!mdiv[%d] %d",i,mDataDiv[i] );
            }
        }

        close(fd);
    }
    open_device();
    ALOGD("batch misc path =%s", input_sysfs_path);

}

BatchSensor::~BatchSensor() {
if (mdata_fd >= 0)
        close(mdata_fd);

}
int BatchSensor::FindDataFd() {
    int fd = -1;
    int num = -1;
    char buf[64]={0};
    const char *devnum_dir = NULL;
    char buf_s[64] = {0};
    int len;

    devnum_dir = "/sys/class/misc/m_batch_misc/batchdevnum";
    fd = open(devnum_dir, O_RDONLY);
    if (fd >= 0)
    {
        len = read(fd, buf, sizeof(buf));
        close(fd);
        if (len <= 0 || len == sizeof(buf))
        {
            ALOGD("read devnum err buf(%s), len = %d\n", buf, len);
            return -1;
        }
        else
        {
            buf[len] = '\0';
            sscanf(buf, "%d\n", &num);
            ALOGE("len = %d, buf = %s",len, buf);
        }
    }else{
        return -1;
    }
    sprintf(buf_s, "/dev/input/event%d", num);
    fd = open(buf_s, O_RDONLY);
    ALOGE_IF(fd<0, "couldn't find input device");
    return fd;
}

/*
void BatchSensor::GetSensorDiv(int div[])
{

}
*/

bool BatchSensor::hasPendingEvents() const {
    return mHasPendingEvent;
}

int BatchSensor::enable(int32_t handle, int en)
{
       int fd;
    int flags = en ? 1 : 0;

	if (mdata_fd < 0)
	{
		ALOGD("no batch control attr\r\n" );
	  	return 0;
	}
	
    ALOGD("batch enable: handle:%d, en:%d \r\n",handle,en);
    strcpy(&input_sysfs_path[input_sysfs_path_len], "batchactive");
    ALOGD("path:%s \r\n",input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
          ALOGD("no batch enable control attr\r\n" );
          return -1;
    }

    char buf[120] = {0};
    sprintf(buf, "%d,%d", handle, en);
    ALOGD("batch value:%s ,size: %d \r\n",buf, strlen(buf)+1);
    write(fd, buf, strlen(buf)+1);
    ALOGD("write path:%s \r\n",input_sysfs_path);
    close(fd);
    ALOGD("batch enable(%d) done", mEnabled );
    return 0;
}


int BatchSensor::setDelay(int32_t handle, int64_t ns)
{
    ALOGD("handle=%d,ns=%lld\n",handle,ns);
    return -errno;
}


int BatchSensor::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    int res = 0;
    int fd = 0;

	if (mdata_fd < 0)
	{
		ALOGD("no batch control attr\r\n" );
		
		if (maxBatchReportLatencyNs != 0)
	  		return -1;
		else
			return 0;
	}
	
    if(maxBatchReportLatencyNs != 0)mEnabled = 1;
    else mEnabled = 0;
    if(flags & SENSORS_BATCH_DRY_RUN || flags & SENSORS_BATCH_WAKE_UPON_FIFO_FULL || (flags == 0)){

        strcpy(&input_sysfs_path[input_sysfs_path_len], "batchbatch");
        ALOGD("path:%s \r\n",input_sysfs_path);
        fd = open(input_sysfs_path, O_RDWR);
        if(fd<0)
        {
              ALOGD("no batch batch control attr\r\n" );
              return -1;
        }
        char buf[120] = {0};
        sprintf(buf, "%d,%d,%lld,%lld", handle, flags, samplingPeriodNs, maxBatchReportLatencyNs);
        ALOGD("batch value:%s ,size: %d \r\n",buf, strlen(buf)+1);
           write(fd, buf, strlen(buf)+1);
        ALOGD("write path:%s \r\n",input_sysfs_path);
        close(fd);
        ALOGD("read path:%s \r\n",input_sysfs_path);
        fd = open(input_sysfs_path, O_RDWR);
        if(fd<0)
        {
              ALOGD("no batch batch control attr\r\n" );
              return -1;
        }
        char buf2[120] = {0};
    int len;
    len = read(fd, buf2, sizeof(buf2));
    if (len <= 0) {
        ALOGD("wrong len");
    } else {
        ALOGD("read value:%s  \r\n",buf2);
        sscanf(buf2, "%d", &res);
        ALOGD("return value:%d \r\n",res);
    }
    close(fd);
    }else{
        ALOGD("batch mode is using invaild flag value for this operation!");
        res = -errno;
    }
    return res;
}

int BatchSensor::flush(int handle)
{
    int res = 0;
    int fd = 0;

	if (mdata_fd < 0)
	{
		flushSensorReq[handle]++;
		mHasPendingEvent = true;
		ALOGD("BatchSensor::flush, handle = %d\r\n", handle);
	  	return 0;
	}
	
    strcpy(&input_sysfs_path[input_sysfs_path_len], "batchflush");
    ALOGD("path:%s \r\n",input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
          ALOGD("no batch flush control attr\r\n" );
          return -1;
    }
    char buf[11] = {0};
    sprintf(buf, "%d", handle);
    ALOGD("flush value:%s ,size: %d \r\n",buf, strlen(buf)+1);
    res=write(fd, buf, strlen(buf)+1);
    ALOGD("flush write (%d) \r\n",res);
    close(fd);
    ALOGD("read path:%s \r\n",input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
        ALOGD("no batch batch control attr\r\n" );
        return -1;
    }
    char buf2[5] = {0};
    int len;
    len = read(fd, buf2, sizeof(buf2));
    close(fd);
    if (len <= 0 || len == sizeof(buf2))
    {
        ALOGD("read batchflush err buf(%s)", buf);
        return -1;
    }
    else
    {
        buf2[len] = '\0';
        sscanf(buf2, "%d", &res);
        ALOGD("return value:%d \r\n",res);
    }
    return res;
}

int BatchSensor::readEvents(sensors_meta_data_event_t* data, int count)
{
	int numEventReceived = 0;
    batch_trans_data sensors_data;
    int err;
    int i;
	
	if (mdata_fd < 0)
	{
	    int handle;
		for (handle=0;handle<ID_SENSOR_MAX_HANDLE && count!=0;handle++)
		{
			for (;flushSensorReq[handle]>0 && count>0;flushSensorReq[handle]--)
			{
				mPendingEvent.timestamp = getTimestamp();//time;
				processEvent(EVENT_TYPE_END_FLAG, handle);
				ALOGD("BatchSensor::readEvents, handle = %d\r\n", handle);
				*data++ = mPendingEvent;
				numEventReceived++;
				count--;
			}
		}
		if (handle == ID_SENSOR_MAX_HANDLE && flushSensorReq[handle-1]==0)
			mHasPendingEvent = false;
	  	return numEventReceived;
	}

    //ALOGE("fwq read Event 1\r\n");
    if (count < 1)
        return -EINVAL;

    while(count)
    {
        sensors_data.numOfDataReturn = count<MAX_BATCH_DATA_PER_QUREY?count:MAX_BATCH_DATA_PER_QUREY;
        sensors_data.numOfDataLeft = 0;

        //ALOGD("BatchSensor::getBatchData1, %d, %d, %d\r\n", sensors_data.numOfDataReturn, sensors_data.numOfDataLeft, count);
        err = ioctl(dev_fd, BATCH_IO_GET_SENSORS_DATA, &sensors_data);
        //ALOGD("BatchSensor::getBatchData2, %d, %d\r\n", sensors_data.numOfDataReturn, sensors_data.numOfDataLeft);

        if (err || (sensors_data.numOfDataReturn < 0) || (count < sensors_data.numOfDataReturn) || (sensors_data.numOfDataLeft < 0))
        {
            ALOGE("BatchSensor: ioctl fail : err = %d, numOfDataReturn = %d, numOfDataLeft = %d",
                err, sensors_data.numOfDataReturn, sensors_data.numOfDataLeft);
            break;
        }
        else
        {
            for (i=0;count&&i<sensors_data.numOfDataReturn;i++)
            {
                data->version = sizeof(sensors_event_t);
                if(TypeToSensor(sensors_data.data[i].sensor)<0)
                {
                    ALOGE("BatchSensor: unknown sensor: %d, value:%d", TypeToSensor(sensors_data.data[i].sensor), sensors_data.data[i].sensor);
                    continue;
                }
                else
                {
                    data->sensor = TypeToSensor(sensors_data.data[i].sensor);
                    data->type = sensors_data.data[i].sensor;
                }

                data->timestamp = sensors_data.data[i].time;
                
                if (SENSOR_TYPE_STEP_COUNTER==data->type)
                {
                    data->u64.step_counter = sensors_data.data[i].values[0];
                }else
                {
                    if (data->sensor >= 0 && data->sensor < numSensors && mDataDiv[data->sensor] != 0)
                    {
                        data->data[0] = (float)sensors_data.data[i].values[0] / mDataDiv[data->sensor];
                        data->data[1] = (float)sensors_data.data[i].values[1] / mDataDiv[data->sensor];
                        data->data[2] = (float)sensors_data.data[i].values[2] / mDataDiv[data->sensor];
                    }
                }

                data++;
                count--;
                numEventReceived++;

                //ALOGD("BatchSensor::getBatchData, %d, %d, %lld, %f, %f, %f\r\n", data->sensor, data->type, data->timestamp, data->data[0], data->data[1], data->data[2]);
            }

            if (sensors_data.numOfDataLeft == 0)
                break;
        }
    }

    //ALOGD("BatchSensor::readEvents, %d, %d\r\n", count, numEventReceived);

    if (count < 1)
        return numEventReceived;

    ssize_t n = mInputReader.fill(mdata_fd);
    if (n < 0 && numEventReceived==0)
        return n;
    
    input_event const* event;

    while (count && mInputReader.readEvent(&event)) {
        int type = event->type;
        //ALOGE("debug.... type\r\n");
        if (type == EV_ABS || type == EV_REL)
        {
                switch (event->code) {
                    case EVENT_TYPE_BATCH_READY:
                        break;
                    case EVENT_TYPE_END_FLAG:
                        data->version = META_DATA_VERSION;
                        data->sensor = 0;
                        data->type = SENSOR_TYPE_META_DATA;
                        data->reserved0 = 0;
                        data->timestamp = 0;
                        data->meta_data.what = META_DATA_FLUSH_COMPLETE;
                        data->meta_data.sensor = event->value&0xffff;
                        ALOGD("metadata.sensor =%d\r\n",data->meta_data.sensor);
                        data++;
                        numEventReceived++;
                        count--;
                        break;
                    default:
                        break;
                }
        }
    else if (type == EV_SYN)
        {
        }
        else
        {
            ALOGE("BatchSensor: unknown event (type=%d, code=%d)",
                    type, event->code);
        }
        mInputReader.next();
    }
    //ALOGE("fwq read Event 2\r\n");
    return numEventReceived;
}

void BatchSensor::processEvent(int code, int value)
{
    //ALOGD("processEvent code=%d,value=%d\r\n",code, value);
    switch (code) {
    case EVENT_TYPE_SENSORTYPE:
        if(TypeToSensor(value)<0)
        {
            ALOGE("BatchSensor: unknown sensor: %d, value:%d", TypeToSensor(value), value);
            return;
        }
        mPendingEvent.type = value;
        mPendingEvent.sensor= TypeToSensor(value);

        break;
       case EVENT_TYPE_BATCH_X:

        mPendingEvent.acceleration.x = (float)value / mDataDiv[mPendingEvent.sensor];
                break;
       case EVENT_TYPE_BATCH_Y:
                mPendingEvent.acceleration.y = (float)value/ mDataDiv[mPendingEvent.sensor];
                break;
       case EVENT_TYPE_BATCH_Z:
                mPendingEvent.acceleration.z = (float)value/ mDataDiv[mPendingEvent.sensor];
                break;
        case EVENT_TYPE_BATCH_VALUE:
        if( SENSOR_TYPE_STEP_COUNTER==mPendingEvent.type )
           {
               mPendingEvent.u64.step_counter = value;
           }else
           {
            mPendingEvent.data[0]= (float)value;
           }
        break;
		case EVENT_TYPE_TIMESTAMP_HI:
			mTimestampHi = ((uint64_t)value << 32) & 0xFFFFFFFF00000000LL;
            //ALOGE("mTimestampHi = %lld", mTimestampHi);
			break;
		case EVENT_TYPE_TIMESTAMP_LO:
			mPendingEvent.timestamp = mTimestampHi | ((uint64_t) value & 0xFFFFFFFF);
            //ALOGE("mPendingEvent.timestamp = %lld", mPendingEvent.timestamp);
			break;	
        case EVENT_TYPE_END_FLAG:

        //mPendingEvent.type = SENSOR_TYPE_META_DATA;
        //mPendingEvent.sensor = value&0xffff;
        mPendingEvent.version = META_DATA_VERSION;
        mPendingEvent.sensor = 0;
        mPendingEvent.type = SENSOR_TYPE_META_DATA;
        mPendingEvent.meta_data.what = META_DATA_FLUSH_COMPLETE;
        mPendingEvent.meta_data.sensor = value&0xffff;
        ALOGD("metadata.sensor =%d\r\n",mPendingEvent.meta_data.sensor);
    }

    return;
}

int BatchSensor::TypeToSensor(int type)
{
    int sensor;
    switch(type){
        case SENSOR_TYPE_ACCELEROMETER:
            sensor = ID_ACCELEROMETER;
            break;
        case SENSOR_TYPE_MAGNETIC_FIELD:
            sensor = ID_MAGNETIC;
            break;
        case SENSOR_TYPE_ORIENTATION:
            sensor = ID_ORIENTATION;
            break;
        case SENSOR_TYPE_GYROSCOPE:
            sensor = ID_GYROSCOPE;
            break;
        case SENSOR_TYPE_LIGHT:
            sensor = ID_LIGHT;
            break;
        case SENSOR_TYPE_PROXIMITY:
            sensor = ID_PROXIMITY;
            break;
        case SENSOR_TYPE_PRESSURE:
            sensor = ID_PRESSURE;
            break;
        case SENSOR_TYPE_TEMPERATURE:
            sensor = ID_TEMPRERATURE;
            break;
		case SENSOR_TYPE_GRAVITY:
			sensor = ID_GRAVITY;
			break;
		case SENSOR_TYPE_LINEAR_ACCELERATION:
			sensor = ID_LINEAR_ACCELERATION;
			break;
		case SENSOR_TYPE_ROTATION_VECTOR:
			sensor = ID_ROTATION_VECTOR;
			break;
        case SENSOR_TYPE_GAME_ROTATION_VECTOR:
            sensor = ID_GAME_ROTATION_VECTOR;
            break;
        case SENSOR_TYPE_SIGNIFICANT_MOTION:
            sensor = ID_SIGNIFICANT_MOTION;
            break;
        case SENSOR_TYPE_STEP_DETECTOR:
            sensor = ID_STEP_DETECTOR;
            break;
        case SENSOR_TYPE_STEP_COUNTER:
            sensor = ID_STEP_COUNTER;
            break;
		case SENSOR_TYPE_GEOMAGNETIC_ROTATION_VECTOR:
			sensor = ID_GEOMAGNETIC_ROTATION_VECTOR;
			break;
		case SENSOR_TYPE_PEDOMETER:
			sensor = ID_PEDOMETER;
			break;
		case SENSOR_TYPE_IN_POCKET:
			sensor = ID_IN_POCKET;
			break;
		case SENSOR_TYPE_ACTIVITY:
			sensor = ID_ACTIVITY;
			break;
		case SENSOR_TYPE_PICK_UP_GESTURE:
			sensor = ID_PICK_UP_GESTURE;
			break;
		case SENSOR_TYPE_FACE_DOWN:
			sensor = ID_FACE_DOWN;
			break;
		case SENSOR_TYPE_SHAKE:
			sensor = ID_SHAKE;
			break;
		case SENSOR_TYPE_HEART_RATE:
			sensor = ID_HEART_RATE;
			break;
		case SENSOR_TYPE_BRINGTOSEE:
			sensor = ID_BRINGTOSEE;
			break;
        default:
            sensor = -1;
    }

    return sensor;
}

