#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <signal.h>
#include <pthread.h>
#include <dlfcn.h>
#include <cutils/properties.h>

#define SIGNUM 6

//
// return 0: libunwind doesn't support backtrace api for direct coredump
//        1: support
//
static int support_direct_coredump_bt() {
    void *handle;
    int *IsKernelDumpUserStack = NULL;
    int ret = 0;

    handle = dlopen("libunwind.so", RTLD_LAZY);
    if (!handle) {
        //AEE_LOGE("error: %s\n", dlerror());
        return 0;
    }

    dlerror(); /* Clear any existing error */

    IsKernelDumpUserStack = dlsym(handle, "IsKernelDumpUserStack");

    if (!IsKernelDumpUserStack) {
        //AEE_LOGE("CANNOT find IsKernelDumpUserStack\n");
        dlclose(handle);
        return 0;
    } else {
        //AEE_LOGI("find IsKernelDumpUserStack\n");
        dlclose(handle);
        return 1;
    }
}

__attribute__((constructor)) static void __aeeDirectcoredump_init()
{
	int sigtype[SIGNUM] = {SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGSEGV, SIGTRAP};
	char value[PROPERTY_VALUE_MAX] = {'\0'};

	if (!support_direct_coredump_bt())
		return;

	property_get("ro.build.type", value, "user");
	if (!strncmp(value, "eng", sizeof("eng"))) {
		property_get("persist.aee.core.direct", value, "default");
		if (strncmp(value, "disable", sizeof("disable"))) {
			int loop;
			for (loop = 0; loop < SIGNUM; loop++) {
				signal(sigtype[loop], SIG_DFL);
			}
		}
	}
	else {
		property_get("persist.aee.core.direct", value, "default");
		if (!strncmp(value, "enable", sizeof("enable"))) {
			int loop;
			for (loop = 0; loop < SIGNUM; loop++) {
				signal(sigtype[loop], SIG_DFL);
			}
		}
	}
}
