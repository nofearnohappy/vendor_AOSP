#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <tz_cross/ta_test.h>
#include <tz_cross/ta_mem.h>
#include <unistd.h>

/* this code is temporary for debug and will be disabled later */

#define PROT_INF_SZ 32

static void vHexDumpBuf(unsigned char* puc_buf, int i_buf_sz) {
    int i;
    printf("0x");
    for ( i = 0; i < i_buf_sz; ++i ) {
        printf("%02x", puc_buf[i]);
    }
    printf("\n");
}

int i4dump_sec_buf_to_from_norm_buf(unsigned int   ui4_sec_buf_handle,
        unsigned char* puc_norm_buf,
        unsigned int   ui4_sz,
        unsigned       dir        /* 0 -> to, 1 -> from*/ ) {
    TZ_RESULT ret;
    UREE_SESSION_HANDLE mem_session_A;
    UREE_SESSION_HANDLE test_ta_sess;

    UREE_SHAREDMEM_PARAM  shm_param;
    UREE_SHAREDMEM_HANDLE shm_handle_src;
    unsigned int cmd;

    MTEEC_PARAM param[4];

    /* check input parameters */
    if ( NULL == puc_norm_buf ) {
        printf("Error: NULL pointer for normal buffer\n");
        return -1;
    }

    /* create memory and mtee img prot inf gen sessions */
    ret = UREE_CreateSession(TZ_TA_MEM_UUID, &mem_session_A);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("Error: fail to creat memory session (%s)\n", TZ_GetErrorString(ret));
        return -2;
    }

    ret = UREE_CreateSession(TZ_TA_TEST_UUID, &test_ta_sess);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("Error: fail to creat test ta session (%s)\n", TZ_GetErrorString(ret));

        ret = UREE_CloseSession(mem_session_A);
        return -3;
    }

    /* register share memory handles */
    shm_param.buffer = (void *) puc_norm_buf;
    shm_param.size = ui4_sz;
    ret = UREE_RegisterSharedmem(mem_session_A, &shm_handle_src, &shm_param);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("Error: fail to register share memory for normal buffer (%s)\n", TZ_GetErrorString(ret));
        ret = UREE_CloseSession(test_ta_sess);
        ret = UREE_CloseSession(mem_session_A);
        return -4;
    }

    /* perform operation */
    cmd = (dir == 0)? TZCMD_TEST_CP_SBUF2NBUF : TZCMD_TEST_CP_NBUF2SBUF;
    param[0].value.a = ui4_sec_buf_handle;
    param[0].value.b = 0;
    param[1].memref.handle = (uint32_t) shm_handle_src;
    param[1].memref.offset = 0;
    param[1].memref.size = ui4_sz;
    param[2].value.a = ui4_sz;
    param[2].value.b = 0;
    ret = UREE_TeeServiceCall(test_ta_sess,
                              cmd,
                              TZ_ParamTypes3(TZPT_VALUE_INPUT, TZPT_MEMREF_INOUT, TZPT_VALUE_INPUT),
                               param);
    if (ret != TZ_RESULT_SUCCESS) {
         printf("Error: fail to invoke function for test ta (%s)\n", TZ_GetErrorString(ret));
         ret = UREE_UnregisterSharedmem(mem_session_A, shm_handle_src);
         ret = UREE_CloseSession(test_ta_sess);
         ret = UREE_CloseSession(mem_session_A);
         return -5;
    }

    /* un-register share memory handles */
    ret = UREE_UnregisterSharedmem(mem_session_A, shm_handle_src);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("Error: fail to un-register share memory for normal buffer (%s)\n", TZ_GetErrorString(ret));
        ret = UREE_CloseSession(test_ta_sess);
        ret = UREE_CloseSession(mem_session_A);
        return -6;
    }

    /* close memory and mtee img prot inf gen sessions */
    ret = UREE_CloseSession(test_ta_sess);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("Error: fail to close test ta session (%d)\n", ret);

        ret = UREE_CloseSession(mem_session_A);
        return -7;
    }

    ret = UREE_CloseSession(mem_session_A);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("Error: fail to close memory session (%d)\n", ret);
        return -8;
    }

    return 0;
}

void test_dump_sec_buf(void) {
    TZ_RESULT ret;
    UREE_SECURECM_HANDLE mem_handle = 0;
    UREE_SESSION_HANDLE   mem_session_A;

    unsigned char aui1_src[128];
    unsigned char aui1_des[128];

    memset(aui1_src, 0xaa, 128);
    memset(aui1_des, 0x00, 128);

    ret = UREE_CreateSession(TZ_TA_MEM_UUID, &mem_session_A);
    if (ret != TZ_RESULT_SUCCESS) {
        // Should provide strerror style error string in UREE.
        printf("CreateSession Error: %s\n", TZ_GetErrorString(ret));
        return;
    }

    ret = UREE_AllocSecurechunkmem(mem_session_A, &mem_handle, 0, 128);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("UREE_AllocSecureMem Error: %d\n", (uint32_t) ret);
        ret = UREE_CloseSession(mem_session_A);
        return;
    }

    i4dump_sec_buf_to_from_norm_buf(mem_handle, aui1_src, 128, 1);
    i4dump_sec_buf_to_from_norm_buf(mem_handle, aui1_des, 128, 0);

    vHexDumpBuf(aui1_des, 128);

    ret = UREE_UnreferenceSecurechunkmem(mem_session_A, mem_handle);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("UREE_UnReferenceSecureMem Error: %d\n", ret);
        ret = UREE_CloseSession(mem_session_A);
        return;
    }

    ret = UREE_CloseSession(mem_session_A);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("CloseSeesion Error: 0x%x, %d\n", (uint32_t) mem_session_A, ret);
        return;
    }

    return;
}

