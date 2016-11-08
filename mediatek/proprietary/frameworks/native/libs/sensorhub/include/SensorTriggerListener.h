#ifndef ANDROID_GUI_SENSOR_TRIGGER_LISTENER_H
#define ANDROID_GUI_SENSOR_TRIGGER_LISTENER_H

#include <stdint.h>
#include <sys/types.h>

#include <binder/IBinder.h>
#include <utils/Errors.h>
#include <utils/RefBase.h>

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

class Parcel;

// ----------------------------------------------------------------------------
enum sensor_trigger_type {
    SENSOR_TRIGGER_NOP                      = 0, // interface test message
    SENSOR_TRIGGER_ACTION_DATA              = 1,
};

// ----------------------------------------------------------------------------
class SensorTriggerListener: public RefBase
{
public:
    /*
    For action data,
    msg: SENSOR_TRIGGER_ACTION_DATA
    ext: rid
    */
    virtual void onTrigger(int msg, int ext1, int ext2, const Parcel* obj) = 0;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SENSOR_TRIGGER_LISTENER_H
