#ifndef ANDROID_SENSORHUB_DEVICE_H
#define ANDROID_SENSORHUB_DEVICE_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Vector.h>
#include <utils/Singleton.h>
#include <utils/String8.h>

#include <shf_define.h>
#include <shf_hal.h>

// ----------------------------------------------------------------------------
namespace android {

// ----------------------------------------------------------------------------
class SensorHubDevice : public Singleton<SensorHubDevice> {
    friend class Singleton<SensorHubDevice>;
    Vector<int> mContextList;
    Vector<int> mActionList;
    Vector<int> mDataList;

public:
    SensorHubDevice();
    virtual ~SensorHubDevice();

    virtual bool isContextAvailable(int contextType);
    virtual bool isActionAvailable(int actionId);
    virtual bool isDataAvailable(int index);

    virtual Vector<int> getContextList();
    virtual Vector<int> getActionList();
    virtual Vector<int> getDataList();
    
    virtual shf_action_index_t addAction(shf_condition_index_t condition_index, shf_action_id_t action);
    virtual status_t removeAction(shf_condition_index_t condition_index, shf_action_index_t action_index);

    virtual status_t addCondition(const shf_condition_t* const condition);
    virtual status_t updateCondition(shf_condition_index_t condition_index,
        const shf_condition_t* const condition);
    virtual bool enableGestureWakeup(bool enable);
    virtual size_t poll(sensor_trigger_data_t* data, size_t size);

};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SENSORHUB_DEVICE_H
