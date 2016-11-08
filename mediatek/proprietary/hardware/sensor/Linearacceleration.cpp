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
#include <sys/select.h>
#include <string.h>

#include <cutils/log.h>

#include "Linearacceleration.h"
#undef LOG_TAG
#define LOG_TAG "La"


#define IGNORE_EVENT_TIME 350000000
#define SYSFS_PATH           "/sys/class/input"


/*****************************************************************************/
LinearaccelSensor::LinearaccelSensor()
    : SensorBase(NULL, "m_la_input"),//LA_INPUTDEV_NAME
      mEnabled(0),
      mOrientationEnabled(0),
      mInputReader(32)
{
    mPendingEvent.version = sizeof(sensors_event_t);
    mPendingEvent.sensor = ID_LINEAR_ACCELERATION;
    mPendingEvent.type = SENSOR_TYPE_LINEAR_ACCELERATION;
    mPendingEvent.acceleration.status = SENSOR_STATUS_ACCURACY_HIGH;
    memset(mPendingEvent.data, 0x00, sizeof(mPendingEvent.data));
    mPendingEvent.flags = 0;
    mPendingEvent.reserved0 = 0;
	mEnabledTime =0;
	mDataDiv = 1;
	mPendingEvent.timestamp =0;
	input_sysfs_path_len = 0;

    mdata_fd = FindDataFd(); 	
    if (mdata_fd >= 0) {
        strcpy(input_sysfs_path, "/sys/class/misc/m_la_misc/");
        input_sysfs_path_len = strlen(input_sysfs_path);
    }
	//ALOGD("la misc path =%s", input_sysfs_path);

	char datapath[64]={"/sys/class/misc/m_la_misc/laactive"};
	int fd = open(datapath, O_RDWR);
	char buf[64];
	int len;
    if (fd >= 0) {
        len = read(fd, buf, sizeof(buf)-1);
        if(len <= 0) {
            ALOGD("read div err buf(%s)", buf);
        } else {
            buf[len] = '\0';
            sscanf(buf, "%d", &mDataDiv);
            ALOGD("read div buf(%s)", datapath);
            ALOGD("mdiv %d", mDataDiv);
        }
	close(fd);
    } else {
        ALOGE("open linearacceleration misc path %s fail ", datapath);
    }
}

LinearaccelSensor::~LinearaccelSensor() {
	if (mdata_fd >= 0)
		close(mdata_fd);
}

int LinearaccelSensor::FindDataFd() {
	int fd = -1;
	int num = -1;
	char buf[64]={0};
    char devnum_dir[] = "/sys/class/misc/m_la_misc/ladevnum";
	char buf_s[64] = {0};
    int len;

	fd = open(devnum_dir, O_RDONLY);
	if (fd >= 0)
	{
        len = read(fd, buf, sizeof(buf)-1);
        close(fd);
        if (len <= 0) {
            ALOGD("read devnum err buf(%s)", buf);
            return -1;
        } else {
            buf[len] = '\0';
            sscanf(buf, "%d\n", &num);
        }
	}else{
		return -1;
	}
	sprintf(buf_s, "/dev/input/event%d", num);
	fd = open(buf_s, O_RDONLY);
   // //ALOGE_IF(fd<0, "couldn't find input device");
	return fd;
}
int LinearaccelSensor::enableNoHALDataAcc(int en)
{
	int fd;
	//ALOGD("LA enable nodata en(%d) \r\n",en);
    strcpy(&input_sysfs_path[input_sysfs_path_len], "laenablenodata");
	//ALOGD("path:%s \r\n",input_sysfs_path);
	fd = open(input_sysfs_path, O_RDWR);
	if(fd<0)
	{
	  	//ALOGD("no LA enable nodata control attr\r\n" );
	  	return -1;
	}

	char buf[2];
	buf[1] = 0;
	if(1==en)
	{
		buf[0] = '1';
	}
	if(0==en)
	{
		buf[0] = '0';
	}
	
	write(fd, buf, sizeof(buf));
  	close(fd);
	
    //ALOGD("LA enable nodata done");    
    return 0;
}

int LinearaccelSensor::enable(int32_t handle, int en)
{
    int fd;
    int flags = en ? 1 : 0;
	
	//ALOGD("LA enable: handle:%d, en:%d \r\n",handle,en);
    strcpy(&input_sysfs_path[input_sysfs_path_len], "laactive");
	//ALOGD("path:%s \r\n",input_sysfs_path);
	fd = open(input_sysfs_path, O_RDWR);
	if(fd<0)
	{
	  	//ALOGD("no LA enable control attr\r\n" );
	  	return -1;
	}
	
	mEnabled = flags;
	char buf[2];
	buf[1] = 0;
	if (flags) 
	{
 		buf[0] = '1';
     	mEnabledTime = getTimestamp() + IGNORE_EVENT_TIME;
	}
	else 
 	{
      	buf[0] = '0';
	}
    write(fd, buf, sizeof(buf));
  	close(fd);
	
    //ALOGD("LA enable(%d) done", mEnabled );    
    return 0;
}
int LinearaccelSensor::setDelay(int32_t handle, int64_t ns)
{
    int fd;
	//uint32_t ms=0;
	//ALOGD("setDelay: (handle=%d, ns=%d)",handle, ns);
    strcpy(&input_sysfs_path[input_sysfs_path_len], "ladelay");
	//ALOGD("path:%s \r\n",input_sysfs_path);
	fd = open(input_sysfs_path, O_RDWR);
	if(fd<0)
	{
	   	//ALOGD("no LA setDelay control attr \r\n" );
	  	return -1;
	}
	//ms = ns/1000000;
    
	char buf[80];
	sprintf(buf, "%lld", ns);
	write(fd, buf, strlen(buf)+1);
	close(fd);
    return 0;
}

int LinearaccelSensor::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    int fd;
    int flag;
	
	////ALOGE("LA batch: handle:%d, en:%d, maxBatchReportLatencyNs:%lld \r\n",handle, flags, maxBatchReportLatencyNs);
	//Don't change batch status if dry run.
	if (flags & SENSORS_BATCH_DRY_RUN)
		return 0;
	
	if(maxBatchReportLatencyNs == 0){
		flag = 0;
	}else{
		flag = 1;
	}
	
    strcpy(&input_sysfs_path[input_sysfs_path_len], "labatch");
	//ALOGD("path:%s \r\n",input_sysfs_path);
	fd = open(input_sysfs_path, O_RDWR);
	if(fd < 0)
	{
	  	//ALOGD("no LA batch control attr\r\n" );
	  	return -1;
	}
	
	char buf[2];
	buf[1] = 0;
	if (flag) 
	{
 		buf[0] = '1';
	}
	else 
 	{
      	buf[0] = '0';
	}
    write(fd, buf, sizeof(buf));
  	close(fd);
	
    //ALOGD("LA batch(%d) done", flag );    
    return 0;

}

int LinearaccelSensor::flush(int handle)
{
    return -errno;
}

int LinearaccelSensor::readEvents(sensors_event_t* data, int count)
{

   // ALOGE("fwq read Event 1\r\n");
    if (count < 1)
        return -EINVAL;

    ssize_t n = mInputReader.fill(mdata_fd);
    if (n < 0)
        return n;
    int numEventReceived = 0;
    input_event const* event;

    while (numEventReceived < count && 0 < mInputReader.readEvent(&event)) {
        int type = event->type;
	 //ALOGE("fwq1....\r\n");
        if (type == EV_ABS || type == EV_REL) 
		{
                processEvent(event->code, event->value);
		//	ALOGE("fwq2....\r\n");
        } 
		else if (type == EV_SYN) 
        {
          //  ALOGE("fwq3....\r\n");
            int64_t time = timevalToNano(event->time);
            mPendingEvent.timestamp = time;
            if (mEnabled) 
			{
                 //ALOGE("fwq4....\r\n");
			     if (mPendingEvent.timestamp >= mEnabledTime) 
				 {
				    //ALOGE("fwq5....\r\n");
				 	*data++ = mPendingEvent;
					numEventReceived++;
			     }
                 count--;
                
            }
        } 
		else// if (type != EV_ABS) 
        { 
            //ALOGE("LinearaccelSensor: unknown event (type=%d, code=%d)", type, event->code);
        }
        mInputReader.next();
    }
	//ALOGE("fwq read Event 2\r\n");
    return numEventReceived;
}

void LinearaccelSensor::processEvent(int code, int value)
{
	//ALOGD("lael, processEvent code=%d,value=%d\r\n",code, value);
    switch (code) {
	case EVENT_TYPE_LA_STATUS:
		mPendingEvent.acceleration.status = value;
		//ALOGE("LinearaccelSensor: EVENT_TYPE_LA_STATUS");				
		break;
       case EVENT_TYPE_LA_X:
            	mPendingEvent.acceleration.x = (float)value / mDataDiv ;
		//ALOGE("LinearaccelSensor: EVENT_TYPE_LA_X");				
            	break;
       case EVENT_TYPE_LA_Y:
            	mPendingEvent.acceleration.y = (float)value / mDataDiv;
		//ALOGE("LinearaccelSensor: EVENT_TYPE_LA_Y");				
            	break;
       case EVENT_TYPE_LA_Z:
            	mPendingEvent.acceleration.z = (float)value / mDataDiv;
		//ALOGE("LinearaccelSensor: EVENT_TYPE_LA_Z");				
            	break;
    }
}
