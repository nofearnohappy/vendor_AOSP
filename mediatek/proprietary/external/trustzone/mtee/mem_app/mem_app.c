
#include <stdio.h>
#include <stdlib.h>
#include <uree/system.h>
#include <uree/dbg.h>
#include <tz_cross/ta_dbg.h>

int main(int argc, char *argv[]) {
    TZ_RESULT ret;
    UREE_SESSION_HANDLE mem_session;
    UREE_SECUREMEM_INFO info;

    printf("Run memory CA\n");

    ret = UREE_CreateSession(TZ_TA_DBG_UUID, &mem_session);
    if (ret != TZ_RESULT_SUCCESS) {
        // Should provide strerror style error string in UREE.
        printf("CreateSession Error: %s\n", TZ_GetErrorString(ret));
        return 1;
    }

    // get secure memory information
    ret = UREE_GetSecurememinfo(mem_session, &info);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("UREE_GetSecurememinfo Error: %s\n", TZ_GetErrorString(ret));
        return 0;
    }
    printf("secure memory: Used = 0x%x bytes, \
Total = 0x%x bytes, Max free continuous mem sz = 0x%x bytes\n",
    info.used_byte, info.total_byte, info.max_free_cont_mem_sz);

    // get secure chunk memory information
    ret = UREE_GetSecurechunkmeminfo(mem_session, &info);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("UREE_GetSecurechunkmeminfo Error: %s\n", TZ_GetErrorString(ret));
        return 0;
    }
    printf("secure chunk memory: Used = 0x%x bytes, \
Total = 0x%x bytes, Max free continuous mem sz = 0x%x bytes\n",
    info.used_byte, info.total_byte, info.max_free_cont_mem_sz);

    ret = UREE_CloseSession(mem_session);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("CloseSeesion mem_session_A Error: %d\n", ret);
    }

    printf("Memory CA done\n");

    return 0;
}
