#ifndef __AAL_SERVICE_CLIENT_H__
#define __AAL_SERVICE_CLIENT_H__

#include <stdint.h>
#include <sys/types.h>

#include <binder/Binder.h>
#include <utils/Singleton.h>
#include <utils/StrongPointer.h>
#include "IAALService.h"

namespace android {

struct AALParameters
{
    int readabilityLevel;
    int lowBLReadabilityLevel;
    int smartBacklightStrength;
    int smartBacklightRange;
};

class IAALService;
class AALClient : public Singleton<AALClient>
{
    friend class Singleton<AALClient>;

public:
    status_t setMode(int32_t mode);
    status_t setFunction(uint32_t funcFlags);
    status_t setLightSensorValue(int32_t value);
    status_t setScreenState(int32_t state, int32_t brightness);
    status_t setSmartBacklightLevel(int32_t level);
    status_t setToleranceRatioLevel(int32_t level);
    status_t setReadabilityLevel(int32_t level);

    // Dummy APIs to avoid build error
    status_t setSmartBacklightStrength(int32_t level) { return -1; }

    status_t getParameters(AALParameters *outParam) {
        outParam->readabilityLevel = 128;
        outParam->lowBLReadabilityLevel = 128;
        outParam->smartBacklightStrength = 128;
        outParam->smartBacklightRange = 128;
        return -1;
    }

private:
    AALClient();

    // DeathRecipient interface
    void serviceDied();

    status_t assertStateLocked() const;

    mutable Mutex mLock;
    mutable sp<IAALService> mAALService;
    mutable sp<IBinder::DeathRecipient> mDeathObserver;
};

};

#endif
