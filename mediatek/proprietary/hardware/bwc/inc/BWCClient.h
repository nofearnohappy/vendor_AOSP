#ifndef __BWC_SERVICE_CLIENT_H__
#define __BWC_SERVICE_CLIENT_H__

#include <stdint.h>
#include <sys/types.h>

#include <binder/Binder.h>
#include <utils/Singleton.h>
#include <utils/StrongPointer.h>

namespace android {

    class IBWCService;
    // Bandwidth Manager class
    // A proxy to remote BWC service
    class BWCClient : public Singleton<BWCClient>
    {
        friend class Singleton<BWCClient>;

    public:
        status_t setProfile(int32_t profile, bool isEnable);

    private:    
        BWCClient();

        // DeathRecipient interface
        void serviceDied();

        status_t assertStateLocked() const;

        mutable Mutex mLock;
        mutable sp<IBWCService> mBWCService;
        mutable sp<IBinder::DeathRecipient> mDeathObserver;
    };

};

#endif
