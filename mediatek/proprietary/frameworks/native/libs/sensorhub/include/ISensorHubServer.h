#ifndef ANDROID_GUI_ISENSORHUB_SERVER_H
#define ANDROID_GUI_ISENSORHUB_SERVER_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <utils/Vector.h>

#include <binder/IInterface.h>

#include "SensorAction.h"
#include "SensorCondition.h"

namespace android {
// ----------------------------------------------------------------------------

class ISensorHubClient;
class Parcel;

class ISensorHubServer : public IInterface
{
public:
    DECLARE_META_INTERFACE(SensorHubServer);

    virtual Vector<int> getContextList() = 0;
    virtual Vector<int> getActionList() = 0;
    virtual void attachClient(const sp<ISensorHubClient>& client) = 0;
    virtual void detachClient(const sp<ISensorHubClient>& client) = 0;
    virtual int requestAction(const SensorCondition& condition, const SensorAction& action) = 0;
    virtual bool updateAction(int requestId, const SensorCondition& condition, const SensorAction& action) = 0;
    virtual bool updateCondition(int requestId, const SensorCondition& condition) = 0;
    virtual bool cancelAction(int requestId) = 0;
    virtual bool enableGestureWakeup(bool enable) = 0;
};

// ----------------------------------------------------------------------------

class BnSensorHubServer : public BnInterface<ISensorHubServer>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_ISENSORHUB_SERVER_H

