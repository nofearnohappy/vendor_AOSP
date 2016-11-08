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

#include <cutils/log.h>
#include "Proximity.h"
#include <utils/SystemClock.h>
#include <utils/Timers.h>
#include <string.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "PROXIMITY"
#endif


#define IGNORE_EVENT_TIME 0//350000000
#define SYSFS_PATH           "/sys/class/input"


/*****************************************************************************/
ProximitySensor::ProximitySensor()
    : SensorBase(NULL, "m_alsps_input"),//PRO_INPUTDEV_NAME
      mEnabled(0),
      mInputReader(32)
{
    mPendingEvent.version = sizeof(sensors_event_t);
    mPendingEvent.sensor = ID_PROXIMITY;
    mPendingEvent.type = SENSOR_TYPE_PROXIMITY;
    memset(mPendingEvent.data, 0x00, sizeof(mPendingEvent.data));
    mPendingEvent.flags = 0;
    mPendingEvent.reserved0 = 0;
    mEnabledTime =0;
    mDataDiv = 1;
    mPendingEvent.timestamp = 0;
    mPendingEvent.distance    = -1; //initialize p sensor value to invalid.
    input_sysfs_path_len = 0;
    memset(input_sysfs_path, 0, PATH_MAX);
    char datapath[64]={"/sys/class/misc/m_alsps_misc/psactive"};
    int fd = -1;
    char buf[64]={0};
    int len;

    mdata_fd = FindDataFd();
    if (mdata_fd >= 0) {
        strcpy(input_sysfs_path, "/sys/class/misc/m_alsps_misc/");
        input_sysfs_path_len = strlen(input_sysfs_path);
    }
    else
    {
        ALOGE("couldn't find input device ");
        return;
    }
    ALOGD("prox misc path =%s", input_sysfs_path);

    fd = open(datapath, O_RDWR);
    if (fd >= 0)
    {
        len = read(fd,buf,sizeof(buf)-1);
        if (len <= 0)
        {
            ALOGD("read div err, len = %d", len);
        }
        else
        {
            buf[len] = '\0';
            sscanf(buf, "%d", &mDataDiv);
            ALOGD("read div buf(%s), mdiv %d", datapath,mDataDiv);
        }
        close(fd);
    }
    else
    {
    ALOGE("open acc misc path %s fail ", datapath);
    }
}

ProximitySensor::~ProximitySensor() {
    if (mdata_fd >= 0)
        close(mdata_fd);

}
int ProximitySensor::FindDataFd() {
    int fd = -1;
    int num = -1;
    char buf[64]={0};
    const char *devnum_dir = NULL;
    char buf_s[64] = {0};
    int len;

    devnum_dir = "/sys/class/misc/m_alsps_misc/psdevnum";

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
        }
    }
    sprintf(buf_s, "/dev/input/event%d", num);
    fd = open(buf_s, O_RDONLY);
    ALOGE_IF(fd<0, "couldn't find input device");
    return fd;
}

int ProximitySensor::enable(int32_t handle, int en)
{
    int fd=-1;
    int flags = en ? 1 : 0;

    ALOGD("PS enable: handle:%d, en:%d \r\n",handle,en);
        strcpy(&input_sysfs_path[input_sysfs_path_len], "psactive");
    ALOGD("path:%s \r\n",input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
          ALOGD("no PS enable control attr\r\n" );
          return -1;
    }

    char buf[2]={0};
    buf[1] = 0;
    if (flags)
    {
         buf[0] = '1';
         mEnabledTime = getTimestamp() + IGNORE_EVENT_TIME;
         mPendingEvent.distance = -1; //reset p sensor value to invalid.
    }
    else
     {
              buf[0] = '0';
    }
    mEnabled = flags; //assign enable after reset p sensor value.
        write(fd, buf, sizeof(buf));
      close(fd);

    ALOGD("PS enable(%d) done", mEnabled );
    return 0;

}
int ProximitySensor::setDelay(int32_t handle, int64_t ns)
{
    int fd=-1;
    ALOGD("setDelay: (handle=%d, ns=%lld)",handle, ns);
        strcpy(&input_sysfs_path[input_sysfs_path_len], "psdelay");
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
           ALOGD("no PS setDelay control attr \r\n" );
          return -1;
    }

    char buf[80]={0};
    sprintf(buf, "%lld", ns);
    write(fd, buf, strlen(buf)+1);
    close(fd);
        return 0;
}
int ProximitySensor::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    int fd=-1;
    int flag=0;

    ALOGE("PS batch: handle:%d, en:%d,samplingPeriodNs:%lld, maxBatchReportLatencyNs:%lld \r\n",handle, flags,samplingPeriodNs, maxBatchReportLatencyNs);

	//Don't change batch status if dry run.
	if (flags & SENSORS_BATCH_DRY_RUN)
		return 0;
	
    if(maxBatchReportLatencyNs == 0){
        flag = 0;
    }else{
        flag = 1;
    }

        strcpy(&input_sysfs_path[input_sysfs_path_len], "psbatch");
    ALOGD("path:%s \r\n",input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd < 0)
    {
          ALOGD("no PS batch control attr\r\n" );
          return -1;
    }

    char buf[2]={0};
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

    ALOGD("PS batch(%d) done", flag );
    return 0;
}

int ProximitySensor::flush(int handle)
{
    ALOGD("handle=%d\n",handle);
    return -errno;
}

int ProximitySensor::readEvents(sensors_event_t* data, int count)
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
       if (type == EV_REL)
        {
            processEvent(event->code, event->value);
            //ALOGE("fwq2....\r\n");
        }
        else if (type == EV_SYN)
        {
            //ALOGE("fwq3....\r\n");
            int64_t time = android::elapsedRealtimeNano();//systemTime(SYSTEM_TIME_MONOTONIC);//timevalToNano(event->time);
            mPendingEvent.timestamp = time;
            if (mEnabled && mPendingEvent.distance >= 0)
            {
                 //ALOGE("fwq4....\r\n");
                 if (mPendingEvent.timestamp >= mEnabledTime)
                 {
                    //ALOGE("fwq5....\r\n");
                     *data++ = mPendingEvent;
                    numEventReceived++;
                 }
                 count--;
				mPendingEvent.distance = -1;
            }
        }
            else if (type != EV_REL)
        {
            ALOGE("ProximitySensor: unknown event (type=%d, code=%d)",
                    type, event->code);
        }
        mInputReader.next();
    }
    //ALOGE("fwq read Event 2\r\n");
    return numEventReceived;
}

void ProximitySensor::processEvent(int code, int value)
{
    ALOGD("processEvent code=%d,value=%d\r\n",code, value);
    switch (code) {
    case EVENT_TYPE_PS_VALUE:
        mPendingEvent.distance= value-1;
        break;
    }

}
