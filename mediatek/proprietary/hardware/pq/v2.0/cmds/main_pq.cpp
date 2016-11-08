#define LOG_TAG "PQ"

#define MTK_LOG_ENABLE 1
#include <cutils/log.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <cutils/properties.h>


/*PQService */
#include <dlfcn.h>
#include <math.h>
#include <utils/SortedVector.h>
#include <binder/PermissionCache.h>
#include <android/native_window.h>
#include <gui/ISurfaceComposer.h>
#include <gui/SurfaceComposerClient.h>
#include <ui/DisplayInfo.h>
#include <cutils/memory.h>

 /*PQService */
#include <linux/disp_session.h>

#include "ddp_drv.h"
#include "cust_gamma.h"
#include "cust_color.h"
#include "cust_tdshp.h"

/*PQService */
#include <binder/BinderService.h>
#include <PQService.h>

using namespace android;


/* main_pq*/
int main(int argc, char** argv)
{
    /*PQService  */
    /* following is copied from main_aal.cpp*/
    ALOGD("PQ service start...");

    PQService::publishAndJoinThreadPool(true);
    // When PQ is launched in its own process, limit the number of
    // binder threads to 4.
    ProcessState::self()->setThreadPoolMaxThreadCount(4);

    ALOGD("PQ service exit...");
    return 0;
}
