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


#include <hardware/sensors.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <math.h>
#include <string.h>

#include <poll.h>
#include <pthread.h>

#include <linux/input.h>

#include <cutils/atomic.h>
#include <cutils/log.h>

#include "nusensors.h"
#include "Hwmsen.h"
#include "Acceleration.h"
#include "Magnetic.h"
#include "Proximity.h"
#include "Pressure.h"
#include "Temprature.h"
#include "Humidity.h"
#include "Gyroscope.h"
#include "AmbienteLight.h"
#include "BatchSensor.h"
#include "StepCounter.h"

#include "Activity.h"
#include "FaceDown.h"
#include "InPocket.h"
#include "Pedometer.h"
#include "PickUp.h"
#include "Shake.h"
#include "HeartRate.h"
#include "Tilt.h"
#include "WakeGesture.h"
#include "GlanceGesture.h"

#include "Bringtosee.h"
#include "GameRotationVector.h"
#include "GeomagneticRotationVector.h"
#include "RotationVector.h"
#include "Linearacceleration.h"
#include "Gravity.h"

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "Sensors"
#endif
/*****************************************************************************/

struct sensors_poll_context_t {
    struct sensors_poll_device_1 device;// must be first

        sensors_poll_context_t();
        ~sensors_poll_context_t();
    int activate(int handle, int enabled);
    int setDelay(int handle, int64_t ns);
    int pollEvents(sensors_event_t* data, int count);
    int batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs);
    int flush(int handle);

private:
    enum {
        batchsensor = 0,
        hwmsen,
        accel,
        magnetic,
        gyro,
        light,
        proximity,
        pressure,
        temperature,
        humidity,
        stepcounter,
        pedometer,
        in_pocket,
        activity,
        pick_up,
        face_down,
        shake,
        heartrate,
        tilt,
        wake_gesture,
        glance_gesture,
        linear_acceleration,
        rotation_vector,
        game_rotation_vector,
        gravity,
        geomagnetic_rotation_vector,
        bringtosee,
        numSensorDrivers,
        numFds,
    };

    int handleToDriver(int handle) const {
    ALOGE("handleToDriver handle(%d)\n",handle);
        switch (handle) {
            case ID_ACCELEROMETER:
                 return accel;
            case ID_MAGNETIC:
            case ID_ORIENTATION:
                 return magnetic;
            case ID_PROXIMITY:
                 return proximity;
            case ID_LIGHT:
                 return light;
            case ID_GYROSCOPE:
                 return gyro;
        case ID_PRESSURE:
                 return pressure;
        case ID_TEMPRERATURE:
                 return temperature;
        case ID_HUMIDITY:
                 return humidity;
        case ID_STEP_COUNTER:
        case ID_STEP_DETECTOR:
        case ID_SIGNIFICANT_MOTION:
                 return stepcounter;
            case ID_PEDOMETER:
				 return pedometer;
            case ID_IN_POCKET:
				 return in_pocket;
            case ID_ACTIVITY:
				 return activity;
            case ID_PICK_UP_GESTURE:
				 return pick_up;
            case ID_FACE_DOWN:
				 return face_down;
            case ID_SHAKE:
				 return shake;
            case ID_HEART_RATE:
                 return heartrate;
            case ID_TILT_DETECTOR:
                 return tilt;
            case ID_WAKE_GESTURE:
                 return wake_gesture;
            case ID_GLANCE_GESTURE:
                 return glance_gesture;
            case ID_LINEAR_ACCELERATION:
                 return linear_acceleration;
            case ID_ROTATION_VECTOR:
                 return rotation_vector;
            case ID_GAME_ROTATION_VECTOR:
                 return game_rotation_vector;
            case ID_GRAVITY:
                 return gravity;
            case ID_GEOMAGNETIC_ROTATION_VECTOR:
                 return geomagnetic_rotation_vector;
            case ID_BRINGTOSEE:
                 return bringtosee;
        default:
                break;
                 //return pressure;
        }
        return -EINVAL;
    }

    static const size_t wake = numFds - 1;
    static const char WAKE_MESSAGE = 'W';
    struct pollfd mPollFds[numFds];
    int mWritePipeFd;
    SensorBase* mSensors[numSensorDrivers];
};
static sensors_poll_context_t *dev;

/*****************************************************************************/

sensors_poll_context_t::sensors_poll_context_t()
{
    memset(&device, 0, sizeof(device));

    mSensors[hwmsen] = new Hwmsen();
    mPollFds[hwmsen].fd = ((Hwmsen *)mSensors[hwmsen])->mdata_fd;
    mPollFds[hwmsen].events = POLLIN;
    mPollFds[hwmsen].revents = 0;

    mSensors[accel] = new AccelerationSensor();
    mPollFds[accel].fd = ((AccelerationSensor*)mSensors[accel])->mdata_fd;
    mPollFds[accel].events = POLLIN;
    mPollFds[accel].revents = 0;

    mSensors[magnetic] = new MagneticSensor();
    mPollFds[magnetic].fd = ((MagneticSensor*)mSensors[magnetic])->mdata_fd;
    mPollFds[magnetic].events = POLLIN;
    mPollFds[magnetic].revents = 0;

    mSensors[proximity] = new ProximitySensor();
    mPollFds[proximity].fd = ((ProximitySensor*)mSensors[proximity])->mdata_fd;
    mPollFds[proximity].events = POLLIN;
    mPollFds[proximity].revents = 0;

    mSensors[light] = new AmbiLightSensor();
    mPollFds[light].fd = ((AmbiLightSensor*)mSensors[light])->mdata_fd;
    mPollFds[light].events = POLLIN;
    mPollFds[light].revents = 0;

    mSensors[gyro] = new GyroscopeSensor();
    mPollFds[gyro].fd = ((GyroscopeSensor*)mSensors[gyro])->mdata_fd;
    mPollFds[gyro].events = POLLIN;
    mPollFds[gyro].revents = 0;

    mSensors[pressure] = new PressureSensor();
    mPollFds[pressure].fd = ((PressureSensor*)mSensors[pressure])->mdata_fd;
    mPollFds[pressure].events = POLLIN;
    mPollFds[pressure].revents = 0;

    mSensors[temperature] = new TempratureSensor();
    mPollFds[temperature].fd = ((TempratureSensor*)mSensors[temperature])->mdata_fd;
    mPollFds[temperature].events = POLLIN;
    mPollFds[temperature].revents = 0;

    mSensors[humidity] = new HumiditySensor();
    mPollFds[humidity].fd = ((HumiditySensor*)mSensors[humidity])->mdata_fd;
    mPollFds[humidity].events = POLLIN;
    mPollFds[humidity].revents = 0;

    mSensors[stepcounter] = new StepCounterSensor();
    mPollFds[stepcounter].fd = ((StepCounterSensor*)mSensors[stepcounter])->mdata_fd;
    mPollFds[stepcounter].events = POLLIN;
    mPollFds[stepcounter].revents = 0;

    mSensors[batchsensor] = new BatchSensor();
    mPollFds[batchsensor].fd = ((BatchSensor*)mSensors[batchsensor])->mdata_fd;
    mPollFds[batchsensor].events = POLLIN;
    mPollFds[batchsensor].revents = 0;

	mSensors[pedometer] = new PedometerSensor();
    mPollFds[pedometer].fd = ((PedometerSensor*)mSensors[pedometer])->mdata_fd;
    mPollFds[pedometer].events = POLLIN;
    mPollFds[pedometer].revents = 0;

	mSensors[activity] = new ActivitySensor();
    mPollFds[activity].fd = ((ActivitySensor*)mSensors[activity])->mdata_fd;
    mPollFds[activity].events = POLLIN;
    mPollFds[activity].revents = 0;

	mSensors[in_pocket] = new InPocketSensor();
    mPollFds[in_pocket].fd = ((InPocketSensor*)mSensors[in_pocket])->mdata_fd;
    mPollFds[in_pocket].events = POLLIN;
    mPollFds[in_pocket].revents = 0;

	mSensors[pick_up] = new PickUpSensor();
    mPollFds[pick_up].fd = ((PickUpSensor*)mSensors[pick_up])->mdata_fd;
    mPollFds[pick_up].events = POLLIN;
    mPollFds[pick_up].revents = 0;

	mSensors[face_down] = new FaceDownSensor();
    mPollFds[face_down].fd = ((FaceDownSensor*)mSensors[face_down])->mdata_fd;
    mPollFds[face_down].events = POLLIN;
    mPollFds[face_down].revents = 0;

	mSensors[shake] = new ShakeSensor();
    mPollFds[shake].fd = ((ShakeSensor*)mSensors[shake])->mdata_fd;
    mPollFds[shake].events = POLLIN;
    mPollFds[shake].revents = 0;

    mSensors[heartrate] = new HeartRateSensor();
    mPollFds[heartrate].fd = ((HeartRateSensor*)mSensors[heartrate])->mdata_fd;
    mPollFds[heartrate].events = POLLIN;
    mPollFds[heartrate].revents = 0;

    mSensors[tilt] = new TiltSensor();
    mPollFds[tilt].fd = ((TiltSensor*)mSensors[tilt])->mdata_fd;
    mPollFds[tilt].events = POLLIN;
    mPollFds[tilt].revents = 0;

    mSensors[wake_gesture] = new WakeGestureSensor();
    mPollFds[wake_gesture].fd = ((WakeGestureSensor*)mSensors[wake_gesture])->mdata_fd;
    mPollFds[wake_gesture].events = POLLIN;
    mPollFds[wake_gesture].revents = 0;

    mSensors[glance_gesture] = new GlanceGestureSensor();
    mPollFds[glance_gesture].fd = ((GlanceGestureSensor*)mSensors[glance_gesture])->mdata_fd;
    mPollFds[glance_gesture].events = POLLIN;
    mPollFds[glance_gesture].revents = 0;

    mSensors[linear_acceleration] = new LinearaccelSensor();
    mPollFds[linear_acceleration].fd = ((LinearaccelSensor*)mSensors[linear_acceleration])->mdata_fd;
    mPollFds[linear_acceleration].events = POLLIN;
    mPollFds[linear_acceleration].revents = 0;

    mSensors[rotation_vector] = new RotationVectorSensor();
    mPollFds[rotation_vector].fd = ((RotationVectorSensor*)mSensors[rotation_vector])->mdata_fd;
    mPollFds[rotation_vector].events = POLLIN;
    mPollFds[rotation_vector].revents = 0;

    mSensors[game_rotation_vector] = new GameRotationVectorSensor();
    mPollFds[game_rotation_vector].fd = ((GameRotationVectorSensor*)mSensors[game_rotation_vector])->mdata_fd;
    mPollFds[game_rotation_vector].events = POLLIN;
    mPollFds[game_rotation_vector].revents = 0;

    mSensors[gravity] = new GravitySensor();
    mPollFds[gravity].fd = ((GravitySensor*)mSensors[gravity])->mdata_fd;
    mPollFds[gravity].events = POLLIN;
    mPollFds[gravity].revents = 0;

    mSensors[geomagnetic_rotation_vector] = new GeomagneticRotationVectorSensor();
    mPollFds[geomagnetic_rotation_vector].fd = ((GeomagneticRotationVectorSensor*)mSensors[geomagnetic_rotation_vector])->mdata_fd;
    mPollFds[geomagnetic_rotation_vector].events = POLLIN;
    mPollFds[geomagnetic_rotation_vector].revents = 0;

    mSensors[bringtosee] = new BringtoseeSensor();
    mPollFds[bringtosee].fd = ((BringtoseeSensor*)mSensors[bringtosee])->mdata_fd;
    mPollFds[bringtosee].events = POLLIN;
    mPollFds[bringtosee].revents = 0;

    int wakeFds[2];
    int result = pipe(wakeFds);
    if (result < 0) {
        ALOGE_IF(result < 0, "error creating wake pipe (%s)", strerror(errno));
    mWritePipeFd = -1;
    } else {
        result = fcntl(wakeFds[0], F_SETFL, O_NONBLOCK);
        ALOGE_IF(result < 0, "fcntl(wakeFds[0] fail (%s)", strerror(errno));
        result = fcntl(wakeFds[1], F_SETFL, O_NONBLOCK);
        ALOGE_IF(result < 0, "fcntl(wakeFds[1] fail (%s)", strerror(errno));
        mWritePipeFd = wakeFds[1];
    }

    mPollFds[wake].fd = wakeFds[0];
    mPollFds[wake].events = POLLIN;
    mPollFds[wake].revents = 0;
}

sensors_poll_context_t::~sensors_poll_context_t() {
    for (int i=0 ; i<numSensorDrivers ; i++) {
        delete mSensors[i];
    }
    close(mPollFds[wake].fd);
    close(mWritePipeFd);
}

int sensors_poll_context_t::activate(int handle, int enabled)
{
    ALOGD( "activate handle =%d, enable = %d",handle, enabled );
    int err=0;

    int index = handleToDriver(handle);

    //
    if(ID_ORIENTATION == handle)
    {
         //ALOGD( "fwq1111" );
        ((AccelerationSensor*)(mSensors[accel]))->enableNoHALDataAcc(enabled);
        ((Hwmsen*)(mSensors[hwmsen]))->enableNoHALDataAcc(enabled);
    }

    if( (index >= numSensorDrivers) || (index < 0) )
    {
        ALOGD("activate error index=%d\n", index);
        return 0;
    }
    if(NULL != mSensors[index])
    {
       ALOGD( "use new sensor index=%d, mSensors[index](%x)", index, mSensors[index]);
       if(this->device.common.version  >= SENSORS_DEVICE_API_VERSION_1_1)
       {
               ALOGD("support batch active \n" );
               mSensors[batchsensor]->enable(handle, enabled);
       }
       err =  mSensors[index]->enable(handle, enabled);
    }

    if(err || index<0 )
    {
          ALOGD("use old sensor err(%d),index(%d) go to old hwmsen\n",err,index);
        // notify to hwmsen sensor to support old architecture
        err =  mSensors[hwmsen]->enable(handle, enabled);
    }

    if (enabled && !err) {
        const char wakeMessage(WAKE_MESSAGE);
        int result = write(mWritePipeFd, &wakeMessage, 1);
        ALOGE_IF(result<0, "error sending wake message (%s)", strerror(errno));
    }
    return err;
}

int sensors_poll_context_t::setDelay(int handle, int64_t ns)
{
    int err =0;

    int index = handleToDriver(handle);
    
    if( (index >= numSensorDrivers) || (index < 0) )
    { 
    	ALOGE("new setDelay handle error(%d)\n",index);
    	return 0;
    }
    if(NULL != mSensors[index])
    {
       err = mSensors[index]->setDelay(handle, ns);
       ALOGE("new setDelay handle(%d),ns(%lld)m, error(%d), index(%d)\n",handle,ns,err,index);
    }
    if(err || index<0)
    {
        ALOGE("new setDelay handle(%d),ns(%lld) err! go to hwmsen\n",handle,ns);
        // notify to hwmsen sensor to support old architecture
         err = mSensors[hwmsen]->setDelay(handle, ns);
    }
    return err;
}

int sensors_poll_context_t::batch(int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs)
{
    ALOGD( "batch handle =%d, flag=%d, sampling=%lld, batchtimeout=%lld",handle, flags, samplingPeriodNs, maxBatchReportLatencyNs);
    int err=0;

    int index = handleToDriver(handle);

    if( (index >= numSensorDrivers) || (index < 0) )
    { 
    	ALOGE("new setDelay handle error(%d)\n",index);
    	return 0;
    }
    if(maxBatchReportLatencyNs == 0)
    {
        if ((flags & SENSORS_BATCH_DRY_RUN) == 0)//Don't set delay if dry run.
        {
            setDelay(handle, samplingPeriodNs);
        }

        err = mSensors[index]->batch(handle, flags, samplingPeriodNs, maxBatchReportLatencyNs);//tell single sensors to disable there own polling and use

    }else{
        err = mSensors[index]->batch(handle, flags, samplingPeriodNs, maxBatchReportLatencyNs);//tell single sensors to disable there own polling and use

    }

    if(err)
    {
        ALOGE("sensor %d use old arch\n", handle);
        return 0;
    }
    else
    {
        // notify to batch sensor to support new architecture
        ALOGE("sensor %d go to common batch\n", handle);
        err = mSensors[batchsensor]->batch(handle, flags, samplingPeriodNs, maxBatchReportLatencyNs);

        return err;
    }
}
int sensors_poll_context_t::flush(int handle)
{
    ALOGD( "flush handle =%d",handle);
    int err=0;

    int index = handleToDriver(handle);
    if( (index >= numSensorDrivers) || (index < 0) )
    { 
    	ALOGE("new setDelay handle error(%d)\n",index);
    	return 0;
    }
     err = mSensors[index]->flush(handle);
    ALOGE("go to batchsensor(%d)\n", handle);
    // notify to hwmsen sensor to support old architecture
     err = mSensors[batchsensor]->flush(handle);

    const char wakeMessage(WAKE_MESSAGE);
    int result = write(mWritePipeFd, &wakeMessage, 1);
    ALOGE_IF(result<0, "error sending wake message (%s)", strerror(errno));
        
    return err;
}

int sensors_poll_context_t::pollEvents(sensors_event_t* data, int count)
{
    int nbEvents = 0;
    int n = 0;
    //ALOGE("pollEvents count =%d",count );

    do {
        // see if we have some leftover from the last poll()
        for (int i=0 ; count && i<numSensorDrivers ; i++) {
            SensorBase* const sensor(mSensors[i]);
            if ((mPollFds[i].revents & POLLIN) || (sensor->hasPendingEvents())) {
                int nb = sensor->readEvents(data, count);
                
            if (data->type == SENSOR_TYPE_PEDOMETER) {
            	ALOGD("pollEvents pedometer buffer data[0]:%f data[1]:%f data[2]:%f data[3]:%f ", 
                        data->data[0], data->data[1], data->data[2], data->data[3]);
            }
            if (data->type == SENSOR_TYPE_SIGNIFICANT_MOTION) {
            	ALOGD("pollEvents significant buffer data[0]:%f ", 
                        data->data[0]);
            }

                if (nb < count) {
                    // no more data for this sensor
                    mPollFds[i].revents = 0;
                }

                for (int j=0;j<nb;j++)
                {
                    if (data[j].type == SENSOR_TYPE_META_DATA)
                        data[j].meta_data.sensor += ID_OFFSET;
                    else
                        data[j].sensor += ID_OFFSET;
                }
                
                //if(nb < 0||nb > count)
                //    ALOGE("pollEvents count error nb:%d, count:%d, nbEvents:%d", nb, count, nbEvents);//for sensor NE debug
                count -= nb;
                nbEvents += nb;
                data += nb;
                //if(nb < 0||nb > count)
                //    ALOGE("pollEvents count error nb:%d, count:%d, nbEvents:%d", nb, count, nbEvents);//for sensor NE debug
            }
        }


        if (count) {
            // we still have some room, so try to see if we can get
            // some events immediately or just wait if we don't have
            // anything to return

            n = poll(mPollFds, numFds, nbEvents ? 0 : -1);
            if (n<0) {
                int err;
		err = errno;
                ALOGE("poll() failed (%s)", strerror(errno));
                return -err;
            }
            if (mPollFds[wake].revents & POLLIN) {
                char msg;
                int result = read(mPollFds[wake].fd, &msg, 1);
                ALOGE_IF(result<0, "error reading from wake pipe (%s)", strerror(errno));
                ALOGE_IF(msg != WAKE_MESSAGE, "unknown message on wake queue (0x%02x)", int(msg));
                mPollFds[wake].revents = 0;
            }
        }



        // if we have events and space, go read them
    } while (n && count);

    return nbEvents;
}

/*****************************************************************************/

static int poll__close(struct hw_device_t *dev)
{
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    if (ctx) {
        delete ctx;
    }
    return 0;
}

static int poll__activate(struct sensors_poll_device_t *dev,
        int handle, int enabled) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->activate(handle-ID_OFFSET, enabled);
}

static int poll__setDelay(struct sensors_poll_device_t *dev,
        int handle, int64_t ns) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->setDelay(handle-ID_OFFSET, ns);
}

static int poll__poll(struct sensors_poll_device_t *dev,
        sensors_event_t* data, int count) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->pollEvents(data, count);
}

static int poll__batch(struct sensors_poll_device_1 *dev,
        int handle, int flags, int64_t samplingPeriodNs, int64_t maxBatchReportLatencyNs) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->batch(handle-ID_OFFSET, flags, samplingPeriodNs, maxBatchReportLatencyNs);
}

static int poll__flush(struct sensors_poll_device_1 *dev,
        int handle) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->flush(handle-ID_OFFSET);
}

/*****************************************************************************/

int init_nusensors(hw_module_t const* module, hw_device_t** device)
{
    int status = -EINVAL;

    dev = new sensors_poll_context_t();
    memset(&dev->device, 0, sizeof(sensors_poll_device_1));

    dev->device.common.tag = HARDWARE_DEVICE_TAG;
#if defined(SENSOR_BATCH_SUPPORT) || defined(CUSTOM_KERNEL_SENSORHUB)
    dev->device.common.version  = SENSORS_DEVICE_API_VERSION_1_1;
#else
	dev->device.common.version  = SENSORS_DEVICE_API_VERSION_1_0;
#endif
    dev->device.common.module   = const_cast<hw_module_t*>(module);
    dev->device.common.close    = poll__close;
    dev->device.activate        = poll__activate;
    dev->device.setDelay        = poll__setDelay;
    dev->device.poll            = poll__poll;
    dev->device.batch           = poll__batch;
    dev->device.flush            = poll__flush;

    *device = &dev->device.common;
    status = 0;
    return status;
}
