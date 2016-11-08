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

#include "StepCounter.h"
#include <utils/SystemClock.h>
#include <utils/Timers.h>
#ifdef LOG_TAG
#undef   LOG_TAG
#define LOG_TAG "STEP_COUNTER"
#endif

#define IGNORE_EVENT_TIME 350000000
#define SYSFS_PATH           "/sys/class/input"


/*****************************************************************************/
StepCounterSensor::StepCounterSensor()
    : SensorBase(NULL, "m_step_c_input"),//_INPUTDEV_NAME
      mEnabled(0),
      mInputReader(32)
{
    mPendingEvent.version = sizeof(sensors_event_t);
    mPendingEvent.sensor = ID_STEP_COUNTER;
    mPendingEvent.type = SENSOR_TYPE_STEP_COUNTER;
    memset(mPendingEvent.data, 0x00, sizeof(mPendingEvent.data));
    mEnabledTime =0;
    mDataDiv = 1;
    mPendingEvent.timestamp =0;
    input_sysfs_path_len = 0;
    memset(input_sysfs_path, 0, PATH_MAX);
    pthread_mutex_init(&mSensorMutex, NULL);

    mdata_fd = FindDataFd();
    if (mdata_fd >= 0) {
        strcpy(input_sysfs_path, "/sys/class/misc/m_step_c_misc/");
        input_sysfs_path_len = strlen(input_sysfs_path);
    }
    ALOGD("StepCounter misc path =%s", input_sysfs_path);

    char datapath[64]={"/sys/class/misc/m_step_c_misc/step_cactive"};
    int fd = open(datapath, O_RDWR);
    char buf[64];
    int len;

    if (fd >= 0)
    {
        len = read(fd,buf,sizeof(buf)-1);
        if(len<=0)
        {
            ALOGD("read div err, len = %d", len);
        }
        else
        {
            buf[len] = '\0';
            sscanf(buf, "%d", &mDataDiv);
            ALOGD("read div buf(%s)", datapath);
            ALOGD("mdiv %d",mDataDiv );
        }
        close(fd);
    }
    else
    {
        ALOGE("open step_c misc path %s fail ", datapath);
    }
}

StepCounterSensor::~StepCounterSensor() {
    if (mdata_fd >= 0)
        close(mdata_fd);
}

int StepCounterSensor::FindDataFd() {
    int fd = -1;
    int num = -1;
    char buf[64]={0};
    const char *devnum_dir = NULL;
    char buf_s[64] = {0};
    int len;

    devnum_dir = "/sys/class/misc/m_step_c_misc/step_cdevnum";
    fd = open(devnum_dir, O_RDONLY);
    if (fd >= 0)
    {
        len = read(fd, buf, sizeof(buf)-1);
        close(fd);
        if (len <= 0)
        {
            ALOGD("read devnum err, len", len);
            return -1;
        }
        else
        {
            buf[len] = '\0';
            sscanf(buf, "%d\n", &num);
        }
    }else{
        return -1;
    }
    sprintf(buf_s, "/dev/input/event%d", num);

    fd = open(buf_s, O_RDONLY);
    ALOGE_IF(fd<0, "couldn't find input device");
    return fd;
}

int StepCounterSensor::enable(int32_t handle, int en)
{
    int fd;
    int flags = en ? 1 : 0;

    ALOGD("step counter enable: handle:%d, en:%d \r\n",handle,en);
        strcpy(&input_sysfs_path[input_sysfs_path_len], "step_cactive");
    ALOGD("path:%s \r\n",input_sysfs_path);
    pthread_mutex_lock(&mSensorMutex);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
          ALOGD("no step counter enable control attr\r\n" );
          pthread_mutex_unlock(&mSensorMutex);
          return -1;
    }

    //mEnabled = flags;
    mEnabled = en ? mEnabled|(1<<handle) : mEnabled&(~(1<<handle)); //there are three sensors, separate the mEnabled bit.
    char buf[120] = {0};
    sprintf(buf, "%d,%d", handle, en);
    ALOGD("step value:%s ,size: %d \r\n",buf, strlen(buf)+1);
    write(fd, buf, strlen(buf)+1);
    ALOGD("step write path:%s \r\n",input_sysfs_path);
    close(fd);
    pthread_mutex_unlock(&mSensorMutex);
    /*
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
    */
    ALOGD("step counter enable(%d) done", mEnabled );
    return 0;
}
int StepCounterSensor::setDelay(int32_t handle, int64_t ns)
{
       //int fd;

    ALOGD("setDelay: regardless of the setDelay() value (handle=%d, ns=%d)",handle, ns);

    return 0;

}
int StepCounterSensor::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    int flag;
    int fd;

    ALOGE("step counter batch: handle:%d, en:%d, samplingPeriodNs:%lld, maxBatchReportLatencyNs:%lld \r\n",handle, flags,samplingPeriodNs, maxBatchReportLatencyNs);

	//Don't change batch status if dry run.
	if (flags & SENSORS_BATCH_DRY_RUN)
		return 0;
	
    if(maxBatchReportLatencyNs == 0){
        flag = 0;
    }else{
        flag = 1;
    }

    strcpy(&input_sysfs_path[input_sysfs_path_len], "step_cbatch");
    ALOGD("path:%s \r\n",input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd < 0)
    {
          ALOGD("no step counter batch control attr\r\n" );
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

    ALOGD("step counter batch(%d) done", flag );
    return 0;

}


int StepCounterSensor::flush(int handle)
{
    ALOGD("handle=%d\n",handle);
    return -errno;
}


int StepCounterSensor::readEvents(sensors_event_t* data, int count)
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
                    if(mPendingEvent.sensor == ID_SIGNIFICANT_MOTION)
                        enable(ID_SIGNIFICANT_MOTION, false); //one shot sensor need auto disable
                }
                count--;
            }
        }
        else if (type != EV_ABS || type !=EV_REL)
        {
            ALOGE("step: unknown event (type=%d, code=%d)",
                    type, event->code);
        }
        mInputReader.next();
    }
    //ALOGE("fwq read Event 2\r\n");
    return numEventReceived;
}

void StepCounterSensor::processEvent(int code, int value)
{
    //ALOGD("processEvent code=%d,value=%d\r\n",code, value);
    //uint64_t stepcount=0;
    switch (code) {
    case EVENT_TYPE_STEP_COUNTER_VALUE:
        mPendingEvent.sensor = ID_STEP_COUNTER;
        mPendingEvent.type = SENSOR_TYPE_STEP_COUNTER;
        mPendingEvent.u64.step_counter= value;
        break;
    case EVENT_TYPE_STEP_DETECTOR_VALUE:
        mPendingEvent.sensor = ID_STEP_DETECTOR;
        mPendingEvent.type = SENSOR_TYPE_STEP_DETECTOR;
        mPendingEvent.data[0] = (float) value;
        break;
    case EVENT_TYPE_SIGNIFICANT_VALUE:
        mPendingEvent.sensor = ID_SIGNIFICANT_MOTION;
        mPendingEvent.type = SENSOR_TYPE_SIGNIFICANT_MOTION;
        mPendingEvent.data[0] = (float) value;
        break;
    }
}
