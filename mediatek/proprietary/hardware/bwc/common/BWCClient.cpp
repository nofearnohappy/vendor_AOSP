#define LOG_TAG "BWCClient" 

#include <sys/types.h>
#include <cutils/log.h>
#include <binder/IServiceManager.h>
#include <binder/ProcessState.h>
#include "BWCClient.h"
#include "IBWCService.h"
#include "bandwidth_control.h"

namespace android {


    ANDROID_SINGLETON_STATIC_INSTANCE(BWCClient);

    BWCClient::BWCClient()
    {
        assertStateLocked();
    }

    status_t BWCClient::assertStateLocked() const 
    {
        int count = 0;
        if (mBWCService == NULL) 
        {
            // try for one second
            const String16 name("BWC");
            do {
                status_t err = getService(name, &mBWCService);
                if (err == NAME_NOT_FOUND) {
                    if (count < 3)
                    {
                        ALOGW("BWCService not published, waiting...");  
                        usleep(100000);
                        count++;
                        continue;
                    }
                    return err;
                }
                if (err != NO_ERROR) {
                    return err;
                }
                break;
            } while (true);

            class DeathObserver : public IBinder::DeathRecipient {
                BWCClient & mBWCClient;
                virtual void binderDied(const wp<IBinder>& who) {
                    ALOGW("BWC Service died [%p]", who.unsafe_get());
                }
            public:
                DeathObserver(BWCClient & service) : mBWCClient(service) { }
            };

            mDeathObserver = new DeathObserver(*const_cast<BWCClient*>(this));
            IInterface::asBinder(mBWCService)->linkToDeath(mDeathObserver);
        }
        return NO_ERROR;
    }

    // Set the BWC profile with BWC binder service
    status_t BWCClient::setProfile(int32_t profile, bool isEnable)
    {    
        status_t err;
        Mutex::Autolock _l(mLock);
        // Retrieve the service object
        err = assertStateLocked();
        // To make sure that the value of the parameter passed to the service
        // is valid
        if (err == NO_ERROR) {
            int32_t state = 0;

            if(isEnable == true){
                state = 1;
            }
            // Set profile with remote service
            err = mBWCService->setProfile((int32_t)profile, state);
        }
        return err;
    }


};
