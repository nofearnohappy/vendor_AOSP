#ifndef __BWC_SERVICE_H__
#define __BWC_SERVICE_H__

#include <utils/threads.h>
#include "IBWCService.h"
#include "bandwidth_control.h"


namespace android
{

    class BWCService : 
        public BinderService<BWCService>, 
        public BnBWCService,
        public Thread
    {
        friend class BinderService<BWCService>;
    public:

        BWCService();
        ~BWCService();

        static char const* getServiceName() { return "BWC"; }


        virtual status_t setProfile(int32_t profile, int32_t state);

    private:
        virtual void onFirstRef();
        virtual status_t readyToRun();
        virtual bool threadLoop();

        mutable Mutex mLock;
        BWC bwc; // BW controller implementation
    };
};
#endif
