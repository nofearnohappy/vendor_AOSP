#define LOG_TAG "BWC"

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <binder/BinderService.h>
#include <BWCService.h>

using namespace android;

int main(int argc, char** argv) 
{
    ALOGD("BWC service start");
    BWC_UNUSED(argc);
    BWC_UNUSED(argv);
    BWCService::publishAndJoinThreadPool(true);
    // When BWC is launched in its own process, limit the number of
    // binder threads to 4.
    ProcessState::self()->setThreadPoolMaxThreadCount(4);

    ALOGD("BWC service exit");
    return 0;
}
