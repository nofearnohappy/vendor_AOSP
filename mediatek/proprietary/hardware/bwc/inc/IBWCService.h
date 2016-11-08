#ifndef __IBWCSERVICE_H__
#define __IBWCSERVICE_H__

#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <binder/BinderService.h>

namespace android
{
    // Interface definition: Remote serice to manage system bandwidth
    class IBWCService : public IInterface 
    {
    protected:
        enum {
            BWC_SET_PROFILE = IBinder::FIRST_CALL_TRANSACTION
        };

    public:
        DECLARE_META_INTERFACE(BWCService);

        virtual status_t setProfile(int32_t profile, int32_t state) = 0;
    };

    class BnBWCService : public BnInterface<IBWCService> 
    {
        virtual status_t onTransact(uint32_t code,
            const Parcel& data,
            Parcel* reply,
            uint32_t flags = 0);
    };    
};

#endif




