
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <uree/system.h>
#include <uree/mem.h>
#include <uree/dbg.h>
#include <tz_cross/ta_test.h>
#include <tz_cross/ta_mem.h>


int rng_get_bytes(void *ptr, size_t size, size_t nmemb) {
    UREE_SHAREDMEM_PARAM shm_param;
    UREE_SHAREDMEM_HANDLE shm_handle = 0;
    size_t shm_size = size * nmemb;
    UREE_SESSION_HANDLE test_session, mem_session;
    MTEEC_PARAM param[4];
    TZ_RESULT ret;

    ret = UREE_CreateSession(TZ_TA_MEM_UUID, &mem_session);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("CreateSession TZ_TA_MEM_UUID Error: %s\n", TZ_GetErrorString(ret));
        return -1;
    }
    ret = UREE_CreateSession(TZ_TA_TEST_UUID, &test_session);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("CreateSession TZ_TA_MEM_UUID Error: %s\n", TZ_GetErrorString(ret));
        return -1;
    }

    shm_param.buffer = ptr;
    shm_param.size = shm_size;
    ret = UREE_RegisterSharedmem(mem_session, &shm_handle, &shm_param);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("UREE_RegisterSharedmem Error: %s\n", TZ_GetErrorString(ret));
        goto end;
    }

    param[0].memref.handle = shm_handle;
    param[0].memref.offset = 0;
    param[0].memref.size = shm_size;
    param[1].value.a = size;
    param[2].value.a = nmemb;
    ret = UREE_TeeServiceCall(test_session, TZCMD_TEST_RNG,
            TZ_ParamTypes3(TZPT_MEMREF_INPUT, TZPT_VALUE_INPUT, TZPT_VALUE_INPUT), param);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("TZCMD_TEST_RNG Error: %s\n", TZ_GetErrorString(ret));
    }

    ret = UREE_UnregisterSharedmem(mem_session, shm_handle);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("UREE_UnregisterSharedmem Error: %s\n", TZ_GetErrorString(ret));
    }

end:
    ret = UREE_CloseSession(test_session);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("CloseSeesion Error: 0x%x, %s\n", (uint32_t) test_session, TZ_GetErrorString(ret));
        return -1;
    }
    ret = UREE_CloseSession(mem_session);
    if (ret != TZ_RESULT_SUCCESS) {
        printf("CloseSeesion Error: 0x%x, %s\n", (uint32_t) mem_session, TZ_GetErrorString(ret));
        return -1;
    }
    return 0;
}

int main(int argc, char *argv[]) {
    char *buf = NULL;
    int size = 4;
    char output_file_name[128] = { '\0' };
    int fd = 0;
    int opt;

    while ((opt = getopt(argc, argv, "o:s:")) != -1) {
        switch (opt) {
            case 'o':
                strncpy(output_file_name, optarg, sizeof(output_file_name)-1);
                fd = -1;
                break;
            case 's':
                size = atoi(optarg);
                break;
            default:
                fprintf(stderr, "Usage: %s -o <output file> -s <size>\n", argv[0]);
                return -1;
        }
    }
    if (size <= 0) {
        fprintf(stderr, "Invalidate size specified: %d\n", size);
        return -1;
    }

    if (fd == -1) {
        fd = open(output_file_name, O_WRONLY|O_CREAT, 0644);
        if (fd < 0) {
            fprintf(stderr, "file open fail! %s\n", output_file_name);
            return -1;
        }
    }

    buf = malloc(size);
    if (buf == NULL) {
        fprintf(stderr, "buffer allocation for size %d fail!\n", size);
        close(fd);
        return -1;
    }

    if (0 != rng_get_bytes(buf, size, 1)) {
        fprintf(stderr, "rng_get_bytes(%p, %d, 1) fail!\n", buf, size);
        free(buf);
        close(fd);
        return -1;
    }

    if (-1 == write(fd, buf, size)) {
        fprintf(stderr, "write to fd %d fail!\n", fd);
        free(buf);
        close(fd);
        return -1;
    }

    free(buf);
    close(fd);
    return 0;
}
