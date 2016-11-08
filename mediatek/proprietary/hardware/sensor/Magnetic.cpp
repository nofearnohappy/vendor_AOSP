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

#include "Magnetic.h"
#include <utils/SystemClock.h>
#include <utils/Timers.h>
#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "Magnetic"
#endif


#define IGNORE_EVENT_TIME 350000000
#define SYSFS_PATH           "/sys/class/input"


/*****************************************************************************/
MagneticSensor::MagneticSensor()
    : SensorBase(NULL, "m_mag_input"),//ACC_INPUTDEV_NAME
      mEnabled(0),
      mOrientationEnabled(0),
      mInputReader(32),
      mPendingMask(0)
{
    input_sysfs_path_len = 0;
    memset(input_sysfs_path, 0, PATH_MAX);
    mPendingEvent[0].version = sizeof(sensors_event_t);
    mPendingEvent[0].sensor = ID_MAGNETIC;
    mPendingEvent[0].type = SENSOR_TYPE_MAGNETIC_FIELD;
    mPendingEvent[0].magnetic.status = SENSOR_STATUS_ACCURACY_HIGH;
    memset(mPendingEvent[0].data, 0x00, sizeof(mPendingEvent[0].data));

    mPendingEvent[1].version = sizeof(sensors_event_t);
    mPendingEvent[1].sensor = ID_ORIENTATION;
    mPendingEvent[1].type = SENSOR_TYPE_ORIENTATION;
    mPendingEvent[1].magnetic.status = SENSOR_STATUS_ACCURACY_HIGH;
    memset(mPendingEvent[1].data, 0x00, sizeof(mPendingEvent[1].data));

    mDataDiv_M = 1;
    mDataDiv_O = 1;
    mEnabledTime = 0;
    mMagSensorDebug = NULL;
    char datapath1[64]={"/sys/class/misc/m_mag_misc/magactive"};
    int fd_m = -1;
    char buf_m[64]={0};
    int len_m;

    mdata_fd = FindDataFd();
    if (mdata_fd >= 0) {
        strcpy(input_sysfs_path, "/sys/class/misc/m_mag_misc/");
        input_sysfs_path_len = strlen(input_sysfs_path);
    }
    else
    {
        ALOGE("couldn't find input device ");
        return;
    }
    ALOGD("mag misc path =%s", input_sysfs_path);

    fd_m = open(datapath1, O_RDWR);
    if (fd_m >= 0)
    {
        len_m = read(fd_m,buf_m,sizeof(buf_m)-1);
        if (len_m <= 0)
        {
            ALOGD("read div err, len_m = %d", len_m);
        }
        else
        {
            buf_m[len_m] = '\0';
            sscanf(buf_m, "%d", &mDataDiv_M);
            ALOGD("read div buf(%s), mdiv_M %d",datapath1,mDataDiv_M);
        }
        close(fd_m);
    }
    else
    {
    ALOGE("open mag misc path %s fail ", datapath1);
    }

    char datapath2[64]={"/sys/class/misc/m_mag_misc/magoactive"};
    int fd_o = open(datapath2, O_RDWR);
    char buf_o[64]={0};
    int len_o=0;
    if (fd_o >= 0)
    {
        len_o = read(fd_o,buf_o,sizeof(buf_o)-1);
        if (len_o <= 0)
        {
            ALOGD("read div err, len_o = %d", len_o);
        }
        else
        {
            buf_o[len_o] = '\0';
            sscanf(buf_o, "%d", &mDataDiv_O);
            ALOGD("read div buf(%s), mdiv_O %d",datapath2,mDataDiv_O);
        }
        close(fd_o);
    }
    else
    {
    ALOGE("open mag_o misc path %s fail ", datapath2);
    }

    //{@input value 2 stands for MagneticField sensor,please refer to nusensor.cpp
    //enmu in sensors_poll_context_t to see other sensors,this is for sensor debug log
    //mMagSensorDebug = new SensorDebugObject((SensorBase*)this, 2);
    //@}

}

MagneticSensor::~MagneticSensor() {
    if (mdata_fd >= 0)
        close(mdata_fd);
}

int MagneticSensor::FindDataFd() {
    int fd = -1;
    int num = -1;
    char buf[64]={0};
    const char *devnum_dir = NULL;
    char buf_s[64] = {0};
    int len;

    devnum_dir = "/sys/class/misc/m_mag_misc/magdevnum";

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
    }else{
        return -1;
    }
    sprintf(buf_s, "/dev/input/event%d", num);
    fd = open(buf_s, O_RDONLY);
    ALOGE_IF(fd<0, "couldn't find input device");
    return fd;
}

int MagneticSensor::write_attr(char* path, char* buf,int len)
{
    int fd=0;
    int err=0;
    ALOGD("fwq write attr path %s   \n",path );
    fd = open(path, O_RDWR);
    if (fd >= 0)
    {
        write(fd, buf, len);
        close(fd);
    }
    else
    {
        err =-1;
        ALOGD("fwq write attr %s fail \n",path );
    }

    return err;

}
int MagneticSensor::enable(int32_t handle, int en)
{
    int fd=-1;
    int flags = en ? 1 : 0;
    int err=0;
    char buf[2]={0};
    int index=0;
    ALOGD("fwq enable: handle:%d, en:%d \r\n",handle,en);
    if(ID_ORIENTATION == handle)
    {
       strcpy(&input_sysfs_path[input_sysfs_path_len], "magoactive");
       index = Orientation;
    }
    if(ID_MAGNETIC== handle)
    {
       strcpy(&input_sysfs_path[input_sysfs_path_len], "magactive");
       index = MagneticField;
    }
    ALOGD("handle(%d),path:%s \r\n",handle,input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
        ALOGD("no magntic enable attr \r\n");
        return -1;
    }

    if(0== en )
    {
       mEnabled &= ~(1<<index);
       buf[1] = 0;
       buf[0] = '0';
    }

    if(1== en)
    {
       mEnabled |= (1<<index);
       buf[1] = 0;
       buf[0] = '1';
    }

    write(fd, buf, sizeof(buf));
      close(fd);

    ALOGD("mag(%d)  mEnabled(0x%x) ----\r\n",handle,mEnabled);
    return 0;
}
int MagneticSensor::setDelay(int32_t handle, int64_t ns)
{
    //uint32_t ms=0;
    //ms = ns/1000000;
    int err;
    int fd;
    if(ID_ORIENTATION == handle)
    {
         strcpy(&input_sysfs_path[input_sysfs_path_len], "magodelay");
    }
    if(ID_MAGNETIC == handle)
    {
        strcpy(&input_sysfs_path[input_sysfs_path_len], "magdelay");
    }

    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
          ALOGD("no MAG setDelay control attr\r\n" );
          return -1;
    }

    ALOGD("setDelay: (handle=%d, ms=%lld)",handle , ns);
    char buf[80]={0};
    sprintf(buf, "%lld", (long long int)ns);
    write(fd, buf, strlen(buf)+1);

    close(fd);

    ALOGD("really setDelay: (handle=%d, ns=%lld)",handle , ns);
    return 0;

}
int MagneticSensor::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    int fd=-1;
    int flag=0;
    int err=0;
    char buf[2]={0};
    int index=0;

    ALOGE("Mag batch: handle:%d, en:%d, samplingPeriodNs:%lld,maxBatchReportLatencyNs:%lld \r\n",handle, flags, samplingPeriodNs,maxBatchReportLatencyNs);

	//Don't change batch status if dry run.
	if (flags & SENSORS_BATCH_DRY_RUN)
		return 0;
    if(maxBatchReportLatencyNs == 0){
        flag = 0;
    }else{
        flag = 1;
    }

    if(ID_ORIENTATION == handle)
    {
       strcpy(&input_sysfs_path[input_sysfs_path_len], "magobatch");
       index = Orientation;
    }
    if(ID_MAGNETIC== handle)
    {
       strcpy(&input_sysfs_path[input_sysfs_path_len], "magbatch");
       index = MagneticField;
    }
        ALOGD("handle(%d),path:%s \r\n",handle,input_sysfs_path);
    fd = open(input_sysfs_path, O_RDWR);
    if(fd<0)
    {
        ALOGD("no magntic enable attr \r\n");
        return -1;
    }

    if(0 == flag )
    {
       buf[1] = 0;
       buf[0] = '0';
    }

    if(1 == flag)
    {
       buf[1] = 0;
       buf[0] = '1';
    }

        write(fd, buf, sizeof(buf));
      close(fd);
    return 0;

}

int MagneticSensor::flush(int handle)
{
    ALOGD("handle=%d\n",handle);
    return -errno;
}
int MagneticSensor::readEvents(sensors_event_t* data, int count)
{
    if (count < 1)
    {
        return -EINVAL;
    }

    ssize_t n = mInputReader.fill(mdata_fd);
    if (n < 0)
    {
        return n;
    }
    int numEventReceived = 0;
    input_event const* event;

    while (count && mInputReader.readEvent(&event)) {
        int type = event->type;
        if (type == EV_ABS || type == EV_REL) {
            processEvent(type, event->code, event->value);
            mInputReader.next();
        } else if (type == EV_SYN) {

            int64_t time = android::elapsedRealtimeNano();
            //ALOGE("fwqM1....\r\n");
            for (int j=0 ; count && mPendingMask && j<numSensors ; j++)
            {
                //ALOGE("fwqM2....\r\n");
                if (mPendingMask & (1<<j))
                {
                    //ALOGE("fwqM3....\r\n");
                    mPendingMask &= ~(1<<j);
                    //mPendingEvent[j].timestamp = time;

                    *data++ = mPendingEvent[j];
                    count--;
                    numEventReceived++;
               }
            }
            if (!mPendingMask) {
                mInputReader.next();
            }
        } else {
            ALOGE("unknown event (type=%d, code=%d)",  type, event->code);
            mInputReader.next();
        }
    }

    //{@input value 2 stands for MagneticField sensor,please refer to nusensor.cpp enmu in sensors_poll_context_t to see other sensors
    //mMagSensorDebug->send_singnal(2);
    //@}
    return numEventReceived;
}

void MagneticSensor::processEvent(int type, int code, int value)
{
   //ALOGD("processEvent code=%d,value=%d\r\n",code, value);a
    if (EV_ABS == type)
    {
        switch (code) {
            case EVENT_TYPE_MAG_STATUS:
                mPendingMask |= 1<<MagneticField;
                mPendingEvent[MagneticField].magnetic.status = value;
                break;
            case EVENT_TYPE_MAG_X:
                mPendingMask |= 1<<MagneticField;
                mPendingEvent[MagneticField].magnetic.x = (float)value / (float)mDataDiv_M;
                break;
            case EVENT_TYPE_MAG_Y:
                mPendingMask |= 1<<MagneticField;
                mPendingEvent[MagneticField].magnetic.y = (float)value / (float)mDataDiv_M;
                break;
            case EVENT_TYPE_MAG_Z:
                mPendingMask |= 1<<MagneticField;
                mPendingEvent[MagneticField].magnetic.z = (float)value / (float)mDataDiv_M;
                break;
            //for osensor
            case EVENT_TYPE_ORIENT_STATUS:
                mPendingMask |= 1<<Orientation;
                mPendingEvent[Orientation].orientation.status = value;
                break;
            case EVENT_TYPE_ORIENT_X:
                mPendingMask |= 1<<Orientation;
                mPendingEvent[Orientation].orientation.x = (float)value / (float)mDataDiv_O;
                break;
            case EVENT_TYPE_ORIENT_Y:
                mPendingMask |= 1<<Orientation;
                mPendingEvent[Orientation].orientation.y = (float)value / (float)mDataDiv_O;
                break;
            case EVENT_TYPE_ORIENT_Z:
                mPendingMask |= 1<<Orientation;
                mPendingEvent[Orientation].orientation.z = (float)value / (float)mDataDiv_O;
                break;
        }
    }
    else if (EV_REL == type)
    {
        switch (code) {
            case EVENT_TYPE_MAG_UPDATE:
                mPendingMask |= 1<<MagneticField;
                break;
            case EVENT_TYPE_MAG_TIMESTAMP_HI:
                mPendingEvent[MagneticField].timestamp =
                    (mPendingEvent[MagneticField].timestamp & 0xFFFFFFFFLL) | ((int64_t)value << 32);
                break;
            case EVENT_TYPE_MAG_TIMESTAMP_LO:
                mPendingEvent[MagneticField].timestamp =
                    (mPendingEvent[MagneticField].timestamp & 0xFFFFFFFF00000000LL) | ((int64_t)value & 0xFFFFFFFFLL);
                break;
            case EVENT_TYPE_ORIENT_UPDATE:
                mPendingMask |= 1<<Orientation;
                break;
            case EVENT_TYPE_ORIENT_TIMESTAMP_HI:
                mPendingEvent[Orientation].timestamp =
                    (mPendingEvent[Orientation].timestamp & 0xFFFFFFFFLL) | ((int64_t)value << 32);
                break;
            case EVENT_TYPE_ORIENT_TIMESTAMP_LO:
                mPendingEvent[Orientation].timestamp =
                    (mPendingEvent[Orientation].timestamp & 0xFFFFFFFF00000000LL) | ((int64_t)value & 0xFFFFFFFFLL);
                break;
            default:
                ALOGE("AccelerationSensor: unknown event (type=%d, code=%d)", type, code);
                break;
        }
    }
}
