#ifndef ANDROID_GUI_SENSORHUB_MANAGER_H
#define ANDROID_GUI_SENSORHUB_MANAGER_H

#include <stdint.h>
#include <sys/types.h>

#include <binder/IBinder.h>

#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <utils/Singleton.h>
#include <utils/Vector.h>
#include <utils/Mutex.h>

#include "SensorCondition.h"
#include "SensorAction.h"
#include "ISensorHubServer.h"
#include "ISensorHubClient.h"

// ----------------------------------------------------------------------------
namespace android {

class Parcel;

enum {
    REQUEST_ID_INVALID = -1,
};

// ----------------------------------------------------------------------------
class SensorHubManager : public BnSensorHubClient//, public Singleton<SensorHubManager>
{
public:
    SensorHubManager();
    virtual ~SensorHubManager();

    virtual Vector<int> getContextList();
    virtual Vector<int> getActionList();
    virtual int requestAction(const SensorCondition& condition, const SensorAction& action);
    virtual bool updateCondition(int requestId, const SensorCondition& condition);
    virtual bool cancelAction(int requestId);
    virtual bool enableGestureWakeup(bool enabled);

    void notify(int msg, int ext1, int ext2, const Parcel* obj = NULL);

private:
    // DeathRecipient interface
    void serverDied();
    status_t assertStateLocked();
    sp<SensorTriggerListener> removeActionAlways(int rid);
    sp<SensorTriggerListener> removeActionIfNeed(int rid);
    bool validCondition(const SensorCondition& condition) const;
    bool validAction(const SensorAction& action) const;
    bool validContext(int contextType) const;
    bool validData(int index) const;

    class ActionHolder {
    public:
        int rid;
        bool repeatable;
        sp<SensorTriggerListener> listener;

        ActionHolder();//for Vector
        ActionHolder(int _rid, bool _repeatable, sp<SensorTriggerListener> _listener);
    };

    mutable int mSessionId;
    mutable Mutex mLock;
    mutable sp<ISensorHubServer> mServer;
    mutable Vector<int> mActionList;
    mutable Vector<int> mContextList;
    mutable Vector<int> mDataList;
    mutable Vector<ActionHolder> mTriggerList;
    mutable sp<IBinder::DeathRecipient> mDeathObserver;
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_SENSORHUB_MANAGER_H
