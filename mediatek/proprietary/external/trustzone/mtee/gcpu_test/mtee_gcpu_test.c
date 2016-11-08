#include <stdio.h>
#include <stdlib.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <time.h>
#include "tz_cross/ta_gcpu.h"
#include "tz_cross/trustzone.h"

int main(int argc, char *argv[]) {
    TZ_RESULT ret;
    int i = 0, loop = 1;
    // MTEEC_PARAM param[4];
    struct timespec tt1, tt2;
    UREE_SESSION_HANDLE ta_gcpu_sess;
    int u4Value1 = 0;
    int u4Mode = 0;
    MTEEC_PARAM param[4];
    uint32_t paramTypes;


    if ( 2 > argc ) {
        printf("usage: mtee_gcpu_test [mode] [loop] [Value1]\n");
        printf("Mode:\n");
        printf("0  -> selftest\n");
        printf("1  -> suspend check\n");
        printf("3  -> AES CBC Perfermance test.\n");
        return 0;
    }

    u4Mode = atoi(argv[1]);
    printf("********************************************\n");
    printf("Mode: %d.\n", u4Mode);

    if (3 == argc) {
        loop = atoi(argv[2]);
    }

    if (4 == argc) {
        u4Value1 = atoi(argv[3]);
    }



    ret = UREE_CreateSession(TZ_TA_GCPU_UUID, &ta_gcpu_sess);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("mtee_gcpu_test: fail to creat session for mtee communication %s\n", TZ_GetErrorString(ret));
        return 1;
    }

    param[0].value.a = u4Value1;
    paramTypes = TZ_ParamTypes1(TZPT_VALUE_INPUT);

    clock_gettime(CLOCK_REALTIME, &tt1);
    for (i = 0; i < loop; i++) {
        printf("Loop: %d\n", i + 1);
        ret = UREE_TeeServiceCall(ta_gcpu_sess, u4Mode, paramTypes, param);

        if (ret != TZ_RESULT_SUCCESS) {
            printf("mtee_gcpu_test: fail to change mtee log level %s, loop: %d\n", TZ_GetErrorString(ret), i);
            return 1;
        }
    }
    clock_gettime(CLOCK_REALTIME, &tt2);

    ret = UREE_CloseSession(ta_gcpu_sess);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("mtee_gcpu_test: fail to close session for mtee communication %s\n", TZ_GetErrorString(ret));
        return 1;
    }

    if ((tt2.tv_nsec - tt1.tv_nsec) < 0)
        printf("Test time: %lf s\n",
    (tt2.tv_sec - tt1.tv_sec - 1) +
    (double)(1000000000 + tt2.tv_nsec - tt1.tv_nsec)/1000000000);
    else
        printf("Test time: %lf s\n", (tt2.tv_sec - tt1.tv_sec) + (double)(tt2.tv_nsec - tt1.tv_nsec)/1000000000);
    // printf ("Succeed to set log level (%d) for mtee !!!\n", param[0].value.a );
    return 0;
}
