#define LOG_TAG "AAL"

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <binder/BinderService.h>
#include <AAL10/AALService.h>

using namespace android;

int main(int argc, char** argv) 
{
    ALOGD("AAL service start...");

    AALService::publishAndJoinThreadPool(true);
    // When AAL is launched in its own process, limit the number of
    // binder threads to 4.
    ProcessState::self()->setThreadPoolMaxThreadCount(4);

    ALOGD("AAL service exit...");
    return 0;
}
