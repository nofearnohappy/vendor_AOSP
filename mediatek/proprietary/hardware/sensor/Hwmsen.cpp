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

#include "Hwmsen.h"
#include <utils/SystemClock.h>
#include <utils/Timers.h>
#include <string.h>

//#include <hwmsen_chip_info.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "Hwmsen_sensors"
#endif

typedef enum SENSOR_NUM_DEF
{
     SONSER_UNSUPPORTED = -1,

    #ifdef CUSTOM_KERNEL_ACCELEROMETER
        ACCELEROMETER_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_MAGNETOMETER
        MAGNETOMETER_NUM,
        ORIENTATION_NUM ,
    #endif

    #if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_ALS)
        ALS_NUM,
    #endif
    #if defined(CUSTOM_KERNEL_ALSPS) || defined(CUSTOM_KERNEL_PS)
        PS_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_GYROSCOPE
        GYROSCOPE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_BAROMETER
        PRESSURE_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_TEMPURATURE
        TEMPURATURE_NUM,
    #endif
    #ifdef CUSTOM_KERNEL_HUMIDITY
        HUMIDITY_NUM,
    #endif
    #ifdef CUSTOM_KERNEL_STEP_COUNTER
        STEP_COUNTER_NUM,
        STEP_DETECTOR_NUM,
    #endif

    #ifdef CUSTOM_KERNEL_SIGNIFICANT_MOTION
        STEP_SIGNIFICANT_MOTION_NUM,
    #endif

    SENSORS_NUM

}SENSOR_NUM_DEF;

Hwmsen::Hwmsen()
    : SensorBase(LS_DEVICE_NAME, "hwmdata"),
      mActiveSensors(0),
      mEnabled(0),
      mInputReader(4),
      mHasPendingEvent(false),
      mPendingMask(0)
{
    memset(mDelays, 0, numSensors);
    memset(m_hwm_last_ts,0,numSensors);
    data_type = 0;
    ALOGD("Hwmsen mPendingEvents len(%d)\r\n",sizeof(mPendingEvents)/sizeof(sensors_event_t));
    memset(mPendingEvents, 0, sizeof(mPendingEvents));
    mPendingEvents[Accelerometer].version = sizeof(sensors_event_t);
    mPendingEvents[Accelerometer].sensor = ID_ACCELEROMETER;
    mPendingEvents[Accelerometer].type = SENSOR_TYPE_ACCELEROMETER;
    mPendingEvents[Accelerometer].acceleration.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[MagneticField].version = sizeof(sensors_event_t);
    mPendingEvents[MagneticField].sensor = ID_MAGNETIC;
    mPendingEvents[MagneticField].type = SENSOR_TYPE_MAGNETIC_FIELD;
    mPendingEvents[MagneticField].magnetic.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[Orientation  ].version = sizeof(sensors_event_t);
    mPendingEvents[Orientation  ].sensor = ID_ORIENTATION;
    mPendingEvents[Orientation  ].type = SENSOR_TYPE_ORIENTATION;
    mPendingEvents[Orientation  ].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[Gyro].version = sizeof(sensors_event_t);
    mPendingEvents[Gyro].sensor = ID_GYROSCOPE;
    mPendingEvents[Gyro].type = SENSOR_TYPE_GYROSCOPE;
    mPendingEvents[Gyro].orientation.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[light].version = sizeof(sensors_event_t);
    mPendingEvents[light].sensor = ID_LIGHT;
    mPendingEvents[light].type = SENSOR_TYPE_LIGHT;

    mPendingEvents[proximity].version = sizeof(sensors_event_t);
    mPendingEvents[proximity].sensor = ID_PROXIMITY;
    mPendingEvents[proximity].type = SENSOR_TYPE_PROXIMITY;
    mdata_fd = FindDataFd();
    mHwmSensorDebug = NULL;

    for (int i=0 ; i<numSensors ; i++)
    {
        mDelays[i] = 200000000; // 200 ms by default
    }
    open_device();
    ALOGD("Hwmsen Construct ok\r\n");

    //{@input value 0 stands for old hwmsen sensor,please refer to nusensor.cpp
    //enmu in sensors_poll_context_t to see other sensors,this is for sensor debug log
    //mHwmSensorDebug = new SensorDebugObject((Hwmsen*)this, 0);
    //@}
}

Hwmsen::~Hwmsen() {
    close_device();
    if (mdata_fd >= 0)
        close(mdata_fd);
}

int Hwmsen::FindDataFd() {
    int fd = -1;
    int num = -1;
    char buf[64]={0};
    const char *devnum_dir = NULL;
    char buf_s[64] = {0};


    devnum_dir = "/sys/class/misc/hwmsensor/hwmsensordevnum";

    fd = open(devnum_dir, O_RDONLY);
    if (fd >= 0)
    {
        int ret = read(fd, buf, sizeof(buf));
		if(ret <= 0) {
			close(fd);
			return -1;
		}
        //num = atoi(buf);
        sscanf(buf, "%d\n", &num);
        close(fd);
    }else{
        return -1;
    }
    sprintf(buf_s, "/dev/input/event%d", num);
    fd = open(buf_s, O_RDONLY);
    ALOGE_IF(fd<0, "couldn't find input device");
    return fd;
}

int Hwmsen::enableNoHALDataAcc(int en)
{
    int io_value = 0;
    if(1==en)
    {
        io_value = ID_ACCELEROMETER;
        if(ioctl(dev_fd, HWM_IO_ENABLE_SENSOR_NODATA, &io_value))
        {
            ALOGE("%s: Enable  nodata old acc error!",__func__);
            return -1;
        }
    }
    if(0==en)
    {
        io_value = ID_ACCELEROMETER;
        if(ioctl(dev_fd, HWM_IO_DISABLE_SENSOR_NODATA, &io_value))
        {
            ALOGE("%s: disable  nodata old acc error!",__func__);
            return -1;
        }
    }
    return 0;
}

int Hwmsen::enable(int32_t handle, int en) {
    //int sensor_type =0;
    int io_value = 0;
    int i=0;
    int flags = en ? 1 : 0;
    int err = 0;
    //uint32_t sensor = 0;
    uint32_t sensor = (1 << handle);    // new active/inactive sensor
    ALOGD("Hwmsen_Enable: handle:%d, en:%d \r\n",handle,en);
    if( handle != ID_ACCELEROMETER && handle != ID_MAGNETIC &&
        handle != ID_ORIENTATION && handle != ID_GYROSCOPE &&
        handle != ID_PROXIMITY && handle != ID_LIGHT)
    {
        ALOGD("enable: (handle=%d) is not hwmsen driver command", handle);
        return 0;
    }

    ALOGD("%s: handle %d, enable or disable %d!", __func__, handle, en);

    if(en == 1)
    {
        switch(handle)
        {
            case ID_ACCELEROMETER:
                m_hwm_last_ts[Accelerometer] = 0;
                break;
            case ID_MAGNETIC:
                m_hwm_last_ts[MagneticField] = 0;
                break;
            case ID_ORIENTATION:
                m_hwm_last_ts[Orientation] = 0;
                break;
            case ID_GYROSCOPE:
                m_hwm_last_ts[Gyro] = 0;
                break;
            case ID_LIGHT:
                m_hwm_last_ts[light] = 0;
                break;
            case ID_PROXIMITY:
                m_hwm_last_ts[proximity] = 0;
                break;
        }

        // TODO:  Device IO control to enable sensor
        //memset(m_hwm_last_ts,0,numSensors);
        if(ioctl(dev_fd, HWM_IO_ENABLE_SENSOR, &handle))
        {
            ALOGE("%s: Enable sensor %d error!",__func__, handle);
            return -1;
        }

        // When start orientation sensor, should start the G and M first.
        if(((mActiveSensors & SENSOR_ORIENTATION) == 0)     // hvae no orientation sensor
            && (sensor & SENSOR_ORIENTATION))                    // new orientation sensor start
        {

            io_value = ID_ACCELEROMETER;
            if(ioctl(dev_fd, HWM_IO_ENABLE_SENSOR_NODATA, &io_value))
            {
                ALOGE("%s: Enable ACCELEROMETR sensor error!",__func__);
                return -1;
            }


            io_value = ID_MAGNETIC;
            if(ioctl(dev_fd, HWM_IO_ENABLE_SENSOR_NODATA, &io_value))
            {
                ALOGE("%s: Enable MAGNETIC sensor error!",__func__);
                return -1;
            }
        }
        mEnabled = 1;
        mActiveSensors |= sensor;
    }
    else
    {
        mActiveSensors &= ~sensor;


        if(0==mActiveSensors)
        {
            mEnabled = 0;
        }

        // When stop Orientation, should stop G and M sensor if they are inactive
        if(((mActiveSensors & SENSOR_ORIENTATION) == 0)
            && (sensor & SENSOR_ORIENTATION))
        {


            io_value = ID_ACCELEROMETER;
            if(ioctl(dev_fd, HWM_IO_DISABLE_SENSOR_NODATA, &io_value))
            {
                ALOGE("%s: Disable ACCELEROMETR sensor error!",__func__);
                return -1;
            }


            io_value = ID_MAGNETIC;
            if(ioctl(dev_fd, HWM_IO_DISABLE_SENSOR_NODATA, &io_value))
            {
                ALOGE("%s: Disable MAGNETIC sensor error!",__func__);
                return -1;
            }

        }

        // TODO: Device IO control disable sensor
        if(ioctl(dev_fd, HWM_IO_DISABLE_SENSOR, &handle))
        {
            ALOGE("%s: Disable sensor %d error!",__func__, handle);
            return -1;
        }
       }

    ALOGD("active_sensors =%x\r\n", mActiveSensors);

    return err;
}

bool Hwmsen::hasPendingEvents() const {
    return mHasPendingEvent;
}


int Hwmsen::readEvents(sensors_event_t* data, int count)
{
    int err=0;
    int i=0;


    if (count < 1)
        {
            //ALOGE("hwmsen: read event count:%d",count);
              return -EINVAL;
          }

    ssize_t n = mInputReader.fill(mdata_fd);
    if (n < 0)
        {
            //ALOGE("hwmsen: read event n:%d", n);
              return n;
          }

    int numEventReceived = 0;
    input_event const* event;

    while (count && mInputReader.readEvent(&event)) {
        int type = event->type;
        //ALOGE("hwmsen: read event (type=%d, code=%d value=%d)",type, event->code,event->value);
        if (type == EV_REL)
        {
            processEvent(event->code, event->value);
        }
        else if (type == EV_SYN)
        {
            readSensorData();
           //ALOGE("hwmsen: EV_SYN event (type=%d, code=%d)",
                   // type, event->code);
           int64_t time = android::elapsedRealtimeNano();

           //ALOGE("hwmsen:  ++mPendingMask:0x%x",mPendingMask);

           for (i=0 ; count && i<numSensors ; i++)
           {
               // ALOGE("hwmsen:  ++mActiveSensors:0x%x",mActiveSensors);
               if(mActiveSensors && (1<<i))
               {
                   if (mPendingMask & (1<<i))
                   {
                       mPendingMask &= ~(1<<i);
                       mPendingEvents[i].timestamp = time;

                       if(i<=Gyro)
                       {
                            if (mPendingEvents[i].timestamp-m_hwm_last_ts[i] > mDelays[i]*18/10)
                            {
                                float delta_mod =
                                    (float)(mPendingEvents[i].timestamp-m_hwm_last_ts[i])/(float)(mDelays[i]);
                                if (mDelays[i] == 1000000000 || m_hwm_last_ts[i] == 0)
                                    delta_mod = 0;
                                /*ALOGE("fwq m-delta_mod=%f ,delta_mod_int=%lld,delta_mod_dec=%f\r\n",
                                    delta_mod,delta_mod_int,delta_mod_dec);*/
                                int loopcout=delta_mod;
                                //ALOGE("fwq hwm-loopcout=%d \r\n",loopcout);

                                if(loopcout>=1 && loopcout<100) {
                                    for(int j=0; j<loopcout; j++) {
                                        mPendingEvents[i].timestamp = time- (loopcout-j)*mDelays[i];
                                        //ALOGE("fwq_n hwm[%d]fack event [%lld ] \r\n",i,mPendingEvents[i].timestamp);
                                        *data++ = mPendingEvents[i];
                                        numEventReceived++;

                                        count--;
                                        if(0==count)
                                        {
                                            break;
                                        }
                                    }
                                }
                            }

                           if(count != 0)
                           {
                               mPendingEvents[i].timestamp=time;
                               *data++ = mPendingEvents[i];
                               count--;
                               numEventReceived++;
                           }
                           m_hwm_last_ts[i] = mPendingEvents[i].timestamp;
                       }else
                       {
                           if(count !=0)
                           {
                               //ALOGE("fwq i=%d\r\n",i);
                               mPendingEvents[i].timestamp=time;
                               *data++ = mPendingEvents[i];
                               count--;
                               numEventReceived++;
                           }
                       }
                       //ALOGE("hwmsen:  count:%d, numEventReceived:%d",count,numEventReceived);
                   }//if (mPendingMask & (1<<i))
               }//if(mActiveSensors && (1<<i))
           }//for (i=0 ; count && i<numSensors ; i++)

        } else {
            ALOGE("hwmsen: unknown event (type=%d, code=%d)",
                    type, event->code);
            mInputReader.next();
        }
        mInputReader.next();
    }

    return numEventReceived;
}

void Hwmsen::processEvent(int code, int value) {
    switch(code)
    {
        case EVENT_TYPE_SENSOR:
            data_type &= 0xFFFFFFFF00000000LL;
            data_type |= (uint64_t)value&0x00000000FFFFFFFFLL;
            // ALOGE("hwmsen: processEvent, EVENT_TYPE_SENSOR = 0x%x, data_type = 0x%llx", value, data_type);
            break;
        case EVENT_TYPE_SENSOR_EXT:
            data_type &= 0x00000000FFFFFFFFLL;
            data_type |= (uint64_t)value<<32;
            // ALOGE("hwmsen: processEvent, EVENT_TYPE_SENSOR_EXT = 0x%x, data_type = 0x%llx", value, data_type);
            break;
        default:
            ALOGE("hwmsen: processEvent code =%d",code);
            ALOGE("hwmsen: processEvent error!!!");
            break;
    }  // switch(code)
}

void Hwmsen::readSensorData(void) {
    hwm_trans_data sensors_data;
    int err =0;
    int i=0;

    memset(&sensors_data, 0 , sizeof(hwm_trans_data));
    sensors_data.data_type = data_type;  // set flag to read specified sensor
    err = ioctl(dev_fd, HWM_IO_GET_SENSORS_DATA, &sensors_data);

    for (i = 0; i < MAX_SENSOR_DATA_UPDATE_ONCE; i++)
    {
        if (sensors_data.data[i].update == 0) {
            break;
        }

        // ALOGE("hwmsen: processEvent, sensors_data.data[%d].sensor = %d",i, sensors_data.data[i].sensor);

        switch (sensors_data.data[i].sensor) {
            case ID_ORIENTATION:
                mPendingMask |= 1 << Orientation;
                mPendingEvents[Orientation].type = SENSOR_TYPE_ORIENTATION;
                mPendingEvents[Orientation].sensor = sensors_data.data[i].sensor;
                mPendingEvents[Orientation].orientation.status = sensors_data.data[i].status;
                mPendingEvents[Orientation].orientation.v[0] = (float)sensors_data.data[i].values[0];
                mPendingEvents[Orientation].orientation.v[1] = (float)sensors_data.data[i].values[1];
                mPendingEvents[Orientation].orientation.v[2] = (float)sensors_data.data[i].values[2];

                mPendingEvents[Orientation].orientation.v[0]/=sensors_data.data[i].value_divide;
                mPendingEvents[Orientation].orientation.v[1]/=sensors_data.data[i].value_divide;
                mPendingEvents[Orientation].orientation.v[2]/=sensors_data.data[i].value_divide;
                mPendingEvents[Orientation].timestamp = sensors_data.data[i].time;
                break;

            case ID_MAGNETIC:
                mPendingMask |= 1 << MagneticField;
                mPendingEvents[MagneticField].type = SENSOR_TYPE_MAGNETIC_FIELD;
                mPendingEvents[MagneticField].sensor = sensors_data.data[i].sensor;
                mPendingEvents[MagneticField].magnetic.status = sensors_data.data[i].status;
                mPendingEvents[MagneticField].magnetic.v[0] = (float)sensors_data.data[i].values[0];
                mPendingEvents[MagneticField].magnetic.v[1] = (float)sensors_data.data[i].values[1];
                mPendingEvents[MagneticField].magnetic.v[2] = (float)sensors_data.data[i].values[2];

                mPendingEvents[MagneticField].magnetic.v[0]/=sensors_data.data[i].value_divide;
                mPendingEvents[MagneticField].magnetic.v[1]/=sensors_data.data[i].value_divide;
                mPendingEvents[MagneticField].magnetic.v[2]/=sensors_data.data[i].value_divide;
                mPendingEvents[MagneticField].timestamp = sensors_data.data[i].time;
                /*ALOGE("[ID_MAGNETIC](%f,%f,%f) \r\n",mPendingEvents[MagneticField].magnetic.v[0],
                    mPendingEvents[MagneticField].magnetic.v[1],
                     mPendingEvents[MagneticField].magnetic.v[2]);*/
                // {@input value 0 stands for old hwmsen sensor,please refer to nusensor.cpp enmu
                // in sensors_poll_context_t to see other sensors
                // mHwmSensorDebug->send_singnal(0);
                // @}
                break;

            case ID_ACCELEROMETER:

                mPendingMask |= 1 << Accelerometer;
                mPendingEvents[Accelerometer].type = SENSOR_TYPE_ACCELEROMETER;
                mPendingEvents[Accelerometer].sensor = sensors_data.data[i].sensor;
                mPendingEvents[Accelerometer].acceleration.status = sensors_data.data[i].status;
                mPendingEvents[Accelerometer].acceleration.v[0] = (float)sensors_data.data[i].values[0];
                mPendingEvents[Accelerometer].acceleration.v[1] = (float)sensors_data.data[i].values[1];
                mPendingEvents[Accelerometer].acceleration.v[2] = (float)sensors_data.data[i].values[2];

                mPendingEvents[Accelerometer].acceleration.v[0]/=sensors_data.data[i].value_divide;
                mPendingEvents[Accelerometer].acceleration.v[1]/=sensors_data.data[i].value_divide;
                mPendingEvents[Accelerometer].acceleration.v[2]/=sensors_data.data[i].value_divide;
                mPendingEvents[Accelerometer].timestamp = sensors_data.data[i].time;
                /*ALOGE("[ID_ACCELEROMETER](%f,%f,%f) \r\n",
                    mPendingEvents[Accelerometer].acceleration.v[0],
                    mPendingEvents[Accelerometer].acceleration.v[1],
                    mPendingEvents[Accelerometer].acceleration.v[2]);*/
                break;
            case ID_GYROSCOPE:

                mPendingMask |= 1 << Gyro;
                mPendingEvents[Gyro].type = SENSOR_TYPE_GYROSCOPE;
                mPendingEvents[Gyro].sensor = sensors_data.data[i].sensor;
                mPendingEvents[Gyro].gyro.status = sensors_data.data[i].status;
                mPendingEvents[Gyro].gyro.v[0] = (float)sensors_data.data[i].values[0];
                mPendingEvents[Gyro].gyro.v[1] = (float)sensors_data.data[i].values[1];
                mPendingEvents[Gyro].gyro.v[2] = (float)sensors_data.data[i].values[2];

                mPendingEvents[Gyro].gyro.v[0]/=sensors_data.data[i].value_divide;
                mPendingEvents[Gyro].gyro.v[1]/=sensors_data.data[i].value_divide;
                mPendingEvents[Gyro].gyro.v[2]/=sensors_data.data[i].value_divide;
                mPendingEvents[Gyro].timestamp = sensors_data.data[i].time;
                /*ALOGE("[ID_GYROSCOPE](%f,%f,%f) \r\n",mPendingEvents[Gyro].gyro.v[0],
                    mPendingEvents[Gyro].gyro.v[1],mPendingEvents[Gyro].gyro.v[2]);*/
                break;

            case ID_PROXIMITY:
                mPendingMask |= 1 << proximity;
                mPendingEvents[proximity].type  = SENSOR_TYPE_PROXIMITY;
                mPendingEvents[proximity].sensor = sensors_data.data[i].sensor;
                mPendingEvents[proximity].distance = (float)sensors_data.data[i].values[0];
                mPendingEvents[proximity].timestamp = sensors_data.data[i].time;
                break;

            case ID_LIGHT:
                mPendingMask |= 1 << light;
                mPendingEvents[light].type = SENSOR_TYPE_LIGHT;
                mPendingEvents[light].sensor = sensors_data.data[i].sensor;
                mPendingEvents[light].light = (float)sensors_data.data[i].values[0];
                mPendingEvents[light].timestamp = sensors_data.data[i].time;
                break;
            default:
                ALOGE("hwmsen: unknown sensor type =%d", sensors_data.data[i].sensor);
        }
    }

    data_type = 0;
}

float Hwmsen::indexToValue(size_t index) const
{
    static const float luxValues[8] = {
            10.0, 160.0, 225.0, 320.0,
            640.0, 1280.0, 2600.0, 10240.0
    };

    const size_t maxIndex = sizeof(luxValues)/sizeof(*luxValues) - 1;
    if (index > maxIndex)
        index = maxIndex;
    return luxValues[index];
}

int Hwmsen::setDelay(int32_t handle, int64_t ns)
{
    int what = -1;
    ALOGD("setDelay: (handle=%d, ns=%lld)",
                    handle, ns);

    if( handle != ID_ACCELEROMETER && handle != ID_MAGNETIC &&
        handle != ID_ORIENTATION && handle != ID_GYROSCOPE &&
        handle != ID_LIGHT)
    {
        ALOGD("setDelay: (handle=%d, ns=%lld) is not hwmsen driver command", handle, ns);
        return 0;
    }

    what = handle;
    /* if (uint32_t(what) >= numSensors)
        return -EINVAL; */

    if (ns < 0)
        return -EINVAL;
    mDelays[what] = ns;
    return update_delay(what);

}

int Hwmsen::update_delay(int what)
{

    struct sensor_delay delayPara;

    //if (mEnabled) //always update delay even sensor is not enabled.
    {
        delayPara.delay = mDelays[what]/1000000;
        delayPara.handle = what;
        ALOGD("setDelay: (what=%d, ms=%d)",
                    delayPara.handle , delayPara.delay);
        if(delayPara.delay < 10)  //set max sampling rate = 100Hz
        {
           ALOGD("Control set delay %d ms is too small \n",delayPara.delay );
           delayPara.delay = 10;

        }
        ALOGD("really setDelay: (what=%d, ms=%d)",
                    delayPara.handle , delayPara.delay);
        if(ioctl(dev_fd, HWM_IO_SET_DELAY, &delayPara))
        {
          ALOGE("%s: Set delay %d ms error ", __func__, delayPara.delay);
          return -errno;
        }
    }
    return 0;
}
int Hwmsen::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    ALOGD("handle=%d,flags=%d,samplingPeriodNs=%lld,maxBatchReportLatencyNs=%lld\n",handle,flags,samplingPeriodNs,maxBatchReportLatencyNs);
    return -errno;
}

int Hwmsen::flush(int handle)
{
    ALOGD("handle=%d\n",handle);
    return -errno;
}
