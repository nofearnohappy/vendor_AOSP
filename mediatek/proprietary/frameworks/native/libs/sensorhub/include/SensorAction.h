#ifndef ANDROID_GUI_SENSOR_ACTION_H
#define ANDROID_GUI_SENSOR_ACTION_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/Flattenable.h>

//#include "shf_define.h"
#include <shf_define.h>
#include "SensorTriggerListener.h"

// ----------------------------------------------------------------------------
namespace android {

// ----------------------------------------------------------------------------
class SensorAction : public LightFlattenable<SensorAction>
{
public:
    enum {
        ACTION_ID_INVALID 			= SHF_ACTION_ID_INVALID,
        ACTION_ID_AP_WAKEUP 		= SHF_ACTION_ID_AP_WAKEUP,
        ACTION_ID_TOUCH_ACTIVE 		= SHF_ACTION_ID_TOUCH_ACTIVE,
        ACTION_ID_TOUCH_DEACTIVE 	= SHF_ACTION_ID_TOUCH_DEACTIVE,
        ACTION_ID_CONSYS_WAKEUP 	= SHF_ACTION_ID_CONSYS_WAKEUP,
    };

    SensorAction(sp<SensorTriggerListener>& listener, bool repeatable, bool checkLast);
    SensorAction(sp<SensorTriggerListener>& listener, bool repeatable);
    SensorAction(int action, bool repeatable, bool checkLast);
    SensorAction();
    SensorAction(const SensorAction& action);
    virtual ~SensorAction();

    inline bool isRepeatable() const { return mRepeatable; }
    inline bool isCheckLast() const { return mCheckLast; }
    inline int getAction() const { return mAction; }
    inline sp<SensorTriggerListener> getListener() const { return mListener; }

    void getStruct(shf_action_id_t* action) const;

    // LightFlattenable protocol
    inline bool isFixedSize() const { return true; }
    virtual size_t getFlattenedSize() const;
    virtual status_t flatten(void* buffer, size_t size) const;
    virtual status_t unflatten(void const* buffer, size_t size);
private:
    sp<SensorTriggerListener> mListener;
    int mAction;
    bool mRepeatable;
    bool mCheckLast;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SENSOR_ACTION_H
