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
#include <cutils/log.h>
#include "Bringtosee.h"
#include <utils/SystemClock.h>
#include <utils/Timers.h>
#include <string.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "BRINGTOSEE"
#endif

#define IGNORE_EVENT_TIME 350000000
#define SYSFS_PATH           "/sys/class/input"

/*****************************************************************************/
BringtoseeSensor::BringtoseeSensor()
    : SensorBase(NULL, "m_bts_input"),//_INPUTDEV_NAME
      mEnabled(0),
      mInputReader(32)
{
    mPendingEvent.version = sizeof(sensors_event_t);
    mPendingEvent.sensor = ID_BRINGTOSEE;
    mPendingEvent.type = SENSOR_TYPE_BRINGTOSEE;
    memset(mPendingEvent.data, 0x00, sizeof(mPendingEvent.data));
    mPendingEvent.flags = 0;
    mPendingEvent.reserved0 = 0;
    mEnabledTime =0;
    mDataDiv = 1;
    mPendingEvent.timestamp =0;
    input_sysfs_path_len = 0;
    input_sysfs_path[PATH_MAX];
    memset(input_sysfs_path, 0, PATH_MAX);
    m_bts_last_ts = 0;
    m_bts_delay= 0;
    batchMode=0;
    firstData = 1;
    char datapath[64]={"/sys/class/misc/m_bts_misc/btsactive"};
    int fd = -1;
    char buf[64]={0};
    int len;

    mdata_fd = FindDataFd();
    if (mdata_fd >= 0) {
        strcpy(input_sysfs_path, "/sys/class/misc/m_bts_misc/");
        input_sysfs_path_len = strlen(input_sysfs_path);
    }
    else
    {
        ALOGE("couldn't find input device ");
        return;
    }
    ALOGD("Bringtosee misc path =%s", input_sysfs_path);

    fd = open(datapath, O_RDWR);
    if (fd >= 0)
    {
        len = read(fd,buf,sizeof(buf)-1);
        if (len <= 0)
        {
            ALOGE("read dev err, len = %d", len);
        }
        else
        {
            buf[len] = '\0';
            sscanf(buf, "%d", &mDataDiv);
            ALOGD("read div buf(%s), mdiv %d", datapath, mDataDiv);
        }
        close(fd);
    }
    else
    {
        ALOGE("open bts misc path %s fail ", datapath);
    }
}

BringtoseeSensor::~BringtoseeSensor() {
	if (mdata_fd >= 0)
		close(mdata_fd);
}

int BringtoseeSensor::FindDataFd() {
	int fd = -1;
	int num = -1;
	char buf[64]={0};
	const char *devnum_dir = NULL;
	char buf_s[64] = {0};
	int len;

	devnum_dir = "/sys/class/misc/m_bts_misc/btsdevnum";

	
	fd = open(devnum_dir, O_RDONLY);
	if (fd >= 0)
	{
        len = read(fd, buf, sizeof(buf)-1);
        close(fd);
        if (len <= 0)
        {
             ALOGD("read devnum err, len = %d", len);
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

int BringtoseeSensor::enable(int32_t handle, int en)
{
    int fd=-1;
    int flags = en ? 1 : 0;
    char buf[2] = {0};
	
	ALOGD("bts enable: handle:%d, en:%d \r\n",handle,en);
    	strcpy(&input_sysfs_path[input_sysfs_path_len], "btsactive");
	ALOGD("path:%s \r\n",input_sysfs_path);
	fd = open(input_sysfs_path, O_RDWR);
	if(fd<0)
	{
	  	ALOGD("no bts enable control attr\r\n" );
	  	return -1;
	}
	
	mEnabled = flags;
	buf[1] = 0;
	if (flags) 
	{
 		buf[0] = '1';
     		mEnabledTime = getTimestamp() + IGNORE_EVENT_TIME;
		 m_bts_last_ts = 0;
	}
	else 
 	{
      		buf[0] = '0';
	}
    	write(fd, buf, sizeof(buf));
  	close(fd);
    ALOGD("bts enable(%d) done", mEnabled );    
    return 0;
}
int BringtoseeSensor::setDelay(int32_t handle, int64_t ns)
{
   	//int fd;

    ALOGD("setDelay: regardless of the setDelay() value (handle=%d, ns=%lld)", handle, ns);
  
    return 0;

}
int BringtoseeSensor::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    	int flag;
	int fd;
	char buf[2];
	
	ALOGE("bts batch: handle:%d, en:%d, maxBatchReportLatencyNs:%lld \r\n",handle, flags, maxBatchReportLatencyNs);

	//Don't change batch status if dry run.
	if (flags & SENSORS_BATCH_DRY_RUN)
		return 0;

	if(maxBatchReportLatencyNs == 0){
		flag = 0;
	}else{
		flag = 1;
	}

    //From batch mode to normal mode.
    if (1 == batchMode && 0 == flag)
    {
        firstData = 1;
        //mEnabledTime = getTimestamp() + IGNORE_EVENT_TIME;
    }
    batchMode = flag;
	
    strcpy(&input_sysfs_path[input_sysfs_path_len], "btsbatch");
	ALOGD("path:%s \r\n",input_sysfs_path);
	fd = open(input_sysfs_path, O_RDWR);
	if(fd < 0)
	{
	  	ALOGD("no bts batch control attr\r\n" );
	  	return -1;
	}
	
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
	
    ALOGD("bts batch(%d) done", flag );    
    return 0;

}

int BringtoseeSensor::flush(int handle)
{
    ALOGD("handle=%d\n",handle);
    return -errno;
}

int BringtoseeSensor::readEvents(sensors_event_t* data, int count)
{

    //ALOGE("fwq read Event 1\r\n");
    if (count < 1)
        return -EINVAL;

    ssize_t n = mInputReader.fill(mdata_fd);
    if (n < 0)
        return n;
    int numEventReceived = 0;
    input_event const* event;

    while (count && mInputReader.readEvent(&event)) {
        int type = event->type;
		//ALOGE("fwq1....\r\n");
        if (type == EV_ABS || type == EV_REL) 
		{
            processEvent(event->code, event->value);
			//ALOGE("fwq2....\r\n");
        } 
		else if (type == EV_SYN) 
        {
            //ALOGE("fwq3....\r\n");
            int64_t time = android::elapsedRealtimeNano();//systemTime(SYSTEM_TIME_MONOTONIC);//timevalToNano(event->time);
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
		else
        { 
            ALOGE("bts: unknown event (type=%d, code=%d)",
                    type, event->code);
        }
        mInputReader.next();
    }
	//ALOGE("fwq read Event 2\r\n");
    return numEventReceived;
}

void BringtoseeSensor::processEvent(int code, int value)
{
    ALOGD("BringtoseeSensor::processEvent code=%d,value=%d\r\n",code, value);
    switch (code) {
	case EVENT_TYPE_BTS_VALUE:
		mPendingEvent.data[0] = (float) value;
		break;
    }
}
