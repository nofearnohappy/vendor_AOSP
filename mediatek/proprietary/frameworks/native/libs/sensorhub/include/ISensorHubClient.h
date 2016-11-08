#ifndef ANDROID_GUI_ISENSORHUB_CLIENT_H
#define ANDROID_GUI_ISENSORHUB_CLIENT_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <binder/Parcel.h>

#include <binder/IInterface.h>

namespace android {
// ----------------------------------------------------------------------------
enum {
};

class ISensorHubClient : public IInterface
{
public:
    DECLARE_META_INTERFACE(SensorHubClient);

    virtual void notify(int msg, int ext1, int ext2, const Parcel* obj) = 0;
};

// ----------------------------------------------------------------------------

class BnSensorHubClient : public BnInterface<ISensorHubClient>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

// ----------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_GUI_ISENSORHUB_CLIENT_H
