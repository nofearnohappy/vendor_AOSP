#define LOG_NDEBUG 0
#define LOG_TAG "SensorHubDevice"
#include <utils/Log.h>

#include "SensorHubDevice.h"
#include <SensorContext.h>

namespace android {
// ---------------------------------------------------------------------------
ANDROID_SINGLETON_STATIC_INSTANCE(SensorHubDevice)

SensorHubDevice::SensorHubDevice()
{
    ALOGD("Init SensorHubDevice...");
    shf_open();

    uint8_t actions[] = { //should sync with shf_define.h
        SHF_ACTION_ID_AP_WAKEUP,
        SHF_ACTION_ID_TOUCH_ACTIVE,
        SHF_ACTION_ID_TOUCH_DEACTIVE,
        SHF_ACTION_ID_CONSYS_WAKEUP,
    };
    size_t asize = sizeof(actions)/sizeof(actions[0]);
    mActionList.clear();
    for (size_t i = 0; i < asize; i++) {
        if (shf_action_valid(actions[i])) {
            mActionList.add(actions[i]);
        } else {
            ALOGW("actions[%d]=%d is invalid!", i, actions[i]);
        }
    }

    uint8_t contextsdata[] = { //should sync with shf_define.h
        SHF_DATA_INDEX_CLOCK_TIME,
        SHF_DATA_INDEX_PEDOMETER_LENGTH,
        SHF_DATA_INDEX_ACTIVITY_VEHICLE,
        SHF_DATA_INDEX_INPOCKET_VALUE,
        SHF_DATA_INDEX_SIGNIFICANT_VALUE,
        SHF_DATA_INDEX_PICKUP_VALUE,
        SHF_DATA_INDEX_FACEDOWN_VALUE,
        SHF_DATA_INDEX_SHAKE_VALUE,
        SHF_DATA_INDEX_GESTURE_VALUE,
    };
    uint32_t contexts[] = { //should sync with sensors.h
        SensorContext::Clock::CONTEXT_TYPE,
        SensorContext::Pedometer::CONTEXT_TYPE,
        SensorContext::Activity::CONTEXT_TYPE,
        SensorContext::InPocket::CONTEXT_TYPE,
        SensorContext::SignificantMotion::CONTEXT_TYPE,
        SensorContext::Pickup::CONTEXT_TYPE,
        SensorContext::FaceDown::CONTEXT_TYPE,
        SensorContext::Shake::CONTEXT_TYPE,
        SensorContext::Gesture::CONTEXT_TYPE,
    };
    size_t csize = sizeof(contexts)/sizeof(contexts[0]);
    mContextList.clear();
    for (size_t i = 0; i < csize; i++) {
        if (shf_data_valid(contextsdata[i])) {
            mContextList.add(contexts[i]);
            ALOGV("contexts[%d]=%d", i, contexts[i]);			
        } else {
            ALOGW("cotextdatas[%d]=%d is invalid!", i, contextsdata[i]);
        }
    }
    uint8_t datas[] = {
        /*
        SHF_DATA_INDEX_GSENSOR_X,
        SHF_DATA_INDEX_GSENSOR_Y,
        SHF_DATA_INDEX_GSENSOR_Z,
        SHF_DATA_INDEX_GSENSOR_TIME,

        SHF_DATA_INDEX_AMBIENT_VALUE,
        SHF_DATA_INDEX_AMBIENT_TIME,

        SHF_DATA_INDEX_PROXIMITY_VALUE,
        SHF_DATA_INDEX_PROXIMITY_TIME,
        */
        SHF_DATA_INDEX_CLOCK_TIME,

        SHF_DATA_INDEX_PEDOMETER_LENGTH,
        SHF_DATA_INDEX_PEDOMETER_FREQUENCY,
        SHF_DATA_INDEX_PEDOMETER_COUNT,
        SHF_DATA_INDEX_PEDOMETER_DISTANCE,
        SHF_DATA_INDEX_PEDOMETER_TIME,

        SHF_DATA_INDEX_ACTIVITY_VEHICLE,
        SHF_DATA_INDEX_ACTIVITY_BIKE,
        SHF_DATA_INDEX_ACTIVITY_FOOT,
        SHF_DATA_INDEX_ACTIVITY_STILL,
        SHF_DATA_INDEX_ACTIVITY_UNKNOWN,
        SHF_DATA_INDEX_ACTIVITY_TILT,
        SHF_DATA_INDEX_ACTIVITY_TIME,

        SHF_DATA_INDEX_INPOCKET_VALUE,
        SHF_DATA_INDEX_INPOCKET_TIME,

        SHF_DATA_INDEX_MPACTIVITY_ACTIVITY,
        SHF_DATA_INDEX_MPACTIVITY_CONFIDENCE,
        SHF_DATA_INDEX_MPACTIVITY_TIME,

        SHF_DATA_INDEX_SIGNIFICANT_VALUE,
        SHF_DATA_INDEX_SIGNIFICANT_TIME,

        SHF_DATA_INDEX_PICKUP_VALUE,
        SHF_DATA_INDEX_PICKUP_TIME,

        SHF_DATA_INDEX_FACEDOWN_VALUE,
        SHF_DATA_INDEX_FACEDOWN_TIME,

        SHF_DATA_INDEX_SHAKE_VALUE,
        SHF_DATA_INDEX_SHAKE_TIME,

        SHF_DATA_INDEX_GESTURE_VALUE,
        SHF_DATA_INDEX_GESTURE_TIME,
    };
    size_t dsize = sizeof(datas)/sizeof(datas[0]);
    mDataList.clear();
    for (size_t i = 0; i < dsize; i++) {
        if (shf_data_valid(datas[i])) {
            mDataList.add(datas[i]);
        } else {
            ALOGW("datas[%d]=%d is invalid!", i, datas[i]);
        }
    }
}

SensorHubDevice::~SensorHubDevice()
{
    mActionList.clear();
    mContextList.clear();
	mDataList.clear();
    shf_close();
}

bool SensorHubDevice::isContextAvailable(int contextType)
{
    for (Vector<int>::iterator iter = mContextList.begin(); iter != mContextList.end(); iter++) {
        if (*iter == contextType) {
            return true;
        }
    }
    return false;
}

bool SensorHubDevice::isActionAvailable(int actionId)
{
    for (Vector<int>::iterator iter = mActionList.begin(); iter != mActionList.end(); iter++) {
        if (*iter == actionId) {
            return true;
        }
    }
    return false;
}

bool SensorHubDevice::isDataAvailable(int index)
{
    for (Vector<int>::iterator iter = mDataList.begin(); iter != mDataList.end(); iter++) {
        if (*iter == index) {
            return true;
        }
    }
    return false;
}

Vector<int> SensorHubDevice::getContextList()
{
    return mContextList;
}

Vector<int> SensorHubDevice::getActionList()
{
    return mActionList;
}

Vector<int> SensorHubDevice::getDataList()
{
    return mDataList;
}

shf_action_index_t SensorHubDevice::addAction(shf_condition_index_t condition_index, shf_action_id_t action)
{
    return shf_condition_action_add(condition_index, action);
}

status_t SensorHubDevice::removeAction(shf_condition_index_t condition_index, shf_action_index_t action_index)
{
    return shf_condition_action_remove(condition_index, action_index);
}

status_t SensorHubDevice::addCondition(const shf_condition_t* const condition)
{
    return shf_condition_add(condition);
}

status_t SensorHubDevice::updateCondition(shf_condition_index_t condition_index,
        const shf_condition_t* const condition)
{
    return shf_condition_update(condition_index, condition);
}

bool SensorHubDevice::enableGestureWakeup(bool enable)
{
    return SHF_STATUS_OK == shf_enable_gesture(enable);
}
    
size_t SensorHubDevice::poll(sensor_trigger_data_t* data, size_t size)
{
    return shf_data_poll(data, size);
}

};
