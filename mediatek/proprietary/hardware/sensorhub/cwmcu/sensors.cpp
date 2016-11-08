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

#include <hardware/sensors.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <math.h>
#include <poll.h>
#include <pthread.h>
#include <stdlib.h>

#include <linux/input.h>

#include <utils/Atomic.h>
#include <utils/Log.h>

#include "CwMcuSensor.h"
#include "sensors.h"

/*****************************************************************************/

#define DELAY_OUT_TIME 0x7FFFFFFF

#define LIGHT_SENSOR_POLLTIME    2000000000
#define AKM_FTRACE 0
#define AKM_DEBUG 0
#define AKM_DATA 0

extern struct sensor_t sSensorList[];
extern int SensorListNum;
/*****************************************************************************/

static int open_sensors(const struct hw_module_t* module, const char* id,
                        struct hw_device_t** device);

static int sensors__get_sensors_list(struct sensors_module_t* module,
                                     struct sensor_t const** list)
{
        *list = sSensorList;
        return SensorListNum;
}

static struct hw_module_methods_t sensors_module_methods = {
        open: open_sensors
};

struct sensors_module_t HAL_MODULE_INFO_SYM = {
    .common = {
        .tag = HARDWARE_MODULE_TAG,
        .version_major = 1,
        .version_minor = 0,
        .id = SENSORS_HARDWARE_MODULE_ID,
        .name = "CyWee SENSORS Module",
        .author = "CyWee Group Ltd.",
        .methods = &sensors_module_methods,
        .dso = NULL,
        .reserved = {0}
        },
        get_sensors_list: sensors__get_sensors_list,
};

struct sensors_poll_context_t {
    sensors_poll_device_1_t device; // must be first

    sensors_poll_context_t();
    ~sensors_poll_context_t();
    int activate(int handle, int enabled);
    int setDelay(int handle, int64_t ns);
    int pollEvents(sensors_event_t* data, int count);
    int batch(int handle, int flags, int64_t period_ns, int64_t timeout);
    int flush(int handle);

private:
    enum {
        cwmcu            = 0,
        numSensorDrivers,
        numFds,
    };

    static const size_t wake = numFds - 1;
    static const char WAKE_MESSAGE = 'W';
    struct pollfd mPollFds[numFds];
    int mWritePipeFd;
    SensorBase* mSensors[numSensorDrivers];

int handleToDriver(int handle) const {
    switch (handle) {
        case ACCELERATION:
        case MAGNETIC:
        case GYRO:
        case LIGHT:
        case PROXIMITY:
        case PRESSURE:
        case HEARTBEAT:
        case ORIENTATION:
        case ROTATIONVECTOR:
        case LINEARACCELERATION:
        case GRAVITY:
        case MAGNETIC_UNCALIBRATED:
        case GYROSCOPE_UNCALIBRATED:
        case GAME_ROTATION_VECTOR:
        case GEOMAGNETIC_ROTATION_VECTOR:
        case STEP_DETECTOR:
        case STEP_COUNTER:
        case SIGNIFICANT_MOTION:
        case TILT:
            return cwmcu;
    }
        return -EINVAL;
    }
};

/*****************************************************************************/

sensors_poll_context_t::sensors_poll_context_t()
{
    mSensors[cwmcu] = new CwMcuSensor();
    mPollFds[cwmcu].fd = mSensors[cwmcu]->getFd();
    mPollFds[cwmcu].events = POLLIN;
    mPollFds[cwmcu].revents = 0;

    ALOGI("--SENSORHAL-- mPollFds[cwmcu].fd = %d\n", mSensors[cwmcu]->getFd());

    int wakeFds[2];
    int result = pipe(wakeFds);
    //LOGE_IF(result<0, "error creating wake pipe (%s)", strerror(errno));
    fcntl(wakeFds[0], F_SETFL, O_NONBLOCK);
    fcntl(wakeFds[1], F_SETFL, O_NONBLOCK);
    mWritePipeFd = wakeFds[1];

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

int sensors_poll_context_t::activate(int handle, int enabled) {
    int index = handleToDriver(handle);

    if (index < 0) return index;
    int err =  mSensors[index]->enable(handle, enabled);
    if (enabled && !err) {
        const char wakeMessage(WAKE_MESSAGE);
        int result = write(mWritePipeFd, &wakeMessage, 1);
    }
    return err;
}

int sensors_poll_context_t::setDelay(int handle, int64_t ns) {

    int index = handleToDriver(handle);
    if (index < 0) return index;
    return mSensors[index]->setDelay(handle, ns);
}

int sensors_poll_context_t::pollEvents(sensors_event_t* data, int count)
{
    int nbEvents = 0;
    int n = 0;
    do {
        // see if we have some leftover from the last poll()
        for (int i=0 ; count && i<numSensorDrivers ; i++) {
            SensorBase* const sensor(mSensors[i]);
            if ((mPollFds[i].revents & POLLIN) || (sensor->hasPendingEvents())) {
                int nb = sensor->readEvents(data, count);
                if (nb < count) {
                    // no more data for this sensor
                    mPollFds[i].revents = 0;
                }
                count -= nb;
                nbEvents += nb;
                data += nb;
            }
        }

        if (count) {
            // we still have some room, so try to see if we can get
            // some events immediately or just wait if we don't have
            // anything to return
            do {
                n = poll(mPollFds, numFds, nbEvents ? 0 : -1);
            } while (n < 0 && errno == EINTR);
            if (n<0) {
                return -errno;
            }
            if (mPollFds[wake].revents & POLLIN) {
                char msg;
                int result = read(mPollFds[wake].fd, &msg, 1);
                mPollFds[wake].revents = 0;
            }
        }
        // if we have events and space, go read them
    } while (n && count);

    return nbEvents;
}

int sensors_poll_context_t::batch(int handle, int flags, int64_t period_ns, int64_t timeout)
{
    ALOGI("--SENHAL-- %s\n", __func__);

    int index = handleToDriver(handle);

    if (index < 0)
        return index;

    ALOGI("--SENHAL-- handle = %d, flags = %d, period_ns = %lld, timeout = %lld\n", handle, flags, period_ns, timeout);

    int err = mSensors[index]->batch(handle, flags, period_ns, timeout);

    return err;
}

int sensors_poll_context_t::flush(int handle)
{
    ALOGI("--SENHAL-- %s\n", __func__);

    int index = handleToDriver(handle);

    if (index < 0)
        return index;

    int err = mSensors[index]->flush(handle);

    return 0;
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
    return ctx->activate(handle, enabled);
}

static int poll__setDelay(struct sensors_poll_device_t *dev,
        int handle, int64_t ns) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->setDelay(handle, ns);
}

static int poll__poll(struct sensors_poll_device_t *dev,
        sensors_event_t* data, int count) {
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->pollEvents(data, count);
}

static int poll__batch(struct sensors_poll_device_1* dev,
            int handle, int flags, int64_t period_ns, int64_t timeout)
{
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->batch(handle, flags, period_ns, timeout);
}

static int poll__flush(struct sensors_poll_device_1* dev, int handle)
{
    sensors_poll_context_t *ctx = (sensors_poll_context_t *)dev;
    return ctx->flush(handle);
}

/*****************************************************************************/

/** Open a new instance of a sensor device using name */
static int open_sensors(const struct hw_module_t* module, const char* id,
                        struct hw_device_t** device)
{
        int status = -EINVAL;
        sensors_poll_context_t *dev = new sensors_poll_context_t();

        memset(&dev->device, 0, sizeof(sensors_poll_device_1_t));

        dev->device.common.tag = HARDWARE_DEVICE_TAG;
        dev->device.common.version  = SENSORS_DEVICE_API_VERSION_1_3;    //1.1 support batch, setdelay() is not used. SENSORS_DEVICE_API_VERSION_1_1
        dev->device.common.module   = const_cast<hw_module_t*>(module);
        dev->device.common.close    = poll__close;
        dev->device.activate        = poll__activate;
        dev->device.setDelay        = poll__setDelay;
        dev->device.poll            = poll__poll;

        dev->device.batch            = poll__batch;
        dev->device.flush            = poll__flush;

        *device = &dev->device.common;
        status = 0;

        return status;
}

