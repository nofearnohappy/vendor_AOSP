#define LOG_TAG "BWManager" 

#include <sys/types.h>
#include <cutils/log.h>
#include "BWManager.h"
#include "BWCClient.h"

namespace android {


    ANDROID_SINGLETON_STATIC_INSTANCE(BWManager);

    BWManager::BWManager()
    {

    }


    // Set the BWC profile with BWC binder service
    status_t BWManager::setProfile(int32_t profile, bool isEnable)
    {    
	     BWCClient & bwcProxy = BWCClient::getInstance();
	     return bwcProxy.setProfile(profile, isEnable);
    }


};

