#define LOG_TAG "IBWCService" 

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include "IBWCService.h"

namespace android {

    // Proxy of BWC remote service
    class BpBWCService : public BpInterface<IBWCService> 
    {
    public:
        BpBWCService(const sp<IBinder>& impl) : BpInterface<IBWCService>(impl) 
        {
        }

        virtual status_t setProfile(int32_t profile, int32_t state)
        {
            Parcel data, reply;
            data.writeInt32(profile);
            data.writeInt32(state);

            if (remote()->transact(BWC_SET_PROFILE, data, &reply) != NO_ERROR) {
                ALOGE("setProfile could not contact remote\n");
                return -1;
            }

            return reply.readInt32();
        }

    };

    IMPLEMENT_META_INTERFACE(BWCService, "BWCService");

    // Handling the binder transaction to BWCService 
    status_t BnBWCService::onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags) 
    {    
        status_t ret = 0;


        switch(code) 
        {
            // Set current BWC profile (profile id, state id)
        case BWC_SET_PROFILE:
            {
                int profile = 0;
                int state = 0;
                data.readInt32(&profile);
                data.readInt32(&state);
                ret = setProfile(profile, state);
                reply->writeInt32(ret);
            }
            break;

        default:
            return BBinder::onTransact(code, data, reply, flags);
        }
        return ret;
    }
};
