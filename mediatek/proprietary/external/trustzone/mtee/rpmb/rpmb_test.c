#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <tz_cross/ta_rpmb.h>
#include <tz_cross/ta_mem.h>
#include "rpmb.h"
#include <uree/system.h>
#include <uree/mem.h>
#include "uree_rpmb.h"
#include <tz_cross/ta_test.h>

#define RPMB_DEV_NODE "/dev/block/mmcblk0rpmb"

char *program_fail_reason[8] = {
 "Operation OK",
 "General failure",
 "Authentication failure (MAC comparison not matching)",
 "Counter failure (counter not matching in comparison)",
 "Address failure (address out of range, wrong address alignment)",
 "Write failure (data/counter/result write failure)",
 "Read failure (data/counter/result read failure)",
 "Authentication key not yet programmed"
};

int main(int argc, char *argv[]) {
    TZ_RESULT tzret = TZ_RESULT_SUCCESS;
    UREE_SESSION_HANDLE rpmb_session;
    rpmb_pkt_t *rpmb_pkt_buf, *rpmb_res_pkt;
    char *pcbuf;
    int ret;
    int fd;
    unsigned int i;
    UREE_SESSION_HANDLE mem_session;
    UREE_SHAREDMEM_HANDLE rpmb_pkt_buf_handle;
    UREE_SHAREDMEM_PARAM shared_mem_param;
    UREE_SESSION_HANDLE test_session;
    int opt;
    int key_program = 0;
    int test_writeread = 0;
    int rpmb_read_addr = 0;
    int rpmb_read_size = 256;

    while ((opt=getopt(argc, argv, "pts:")) != -1) {
        switch (opt) {
            case 'p':
                key_program = 1;
            break;
            case 't':
                test_writeread = 1;
            break;
            case 's':
                rpmb_read_size = atoi(optarg);
            break;
        }
    }
    if (argc > optind) {
        rpmb_read_addr = strtoul(argv[optind], NULL, 10);
        printf("arg:%s => read offset=0x%x\n", argv[optind], rpmb_read_addr);
    }


    if (-1 == (fd = open(RPMB_DEV_NODE, O_RDWR))) {
        fprintf(stderr, "open rpmb node %s fail! %s\n", RPMB_DEV_NODE, strerror(errno));
        return -1;
    }

    rpmb_pkt_buf = malloc(sizeof(rpmb_pkt_t));
    memset(rpmb_pkt_buf, 0, sizeof(rpmb_pkt_t));

    tzret = UREE_CreateSession(TZ_TA_RPMB_UUID, &rpmb_session);
    if (tzret != TZ_RESULT_SUCCESS) {
        close(fd);
        fprintf(stderr, "create rpmb ta session fail! %s\n", TZ_GetErrorString(tzret));
        return tzret;
    }

    if (key_program != 0) {
        tzret = UREE_RpmbKeyToProgram(rpmb_session, rpmb_pkt_buf->au1KeyMac, RPMB_KEY_LENGTH);

        if (tzret != TZ_RESULT_SUCCESS) {
            UREE_CloseSession(rpmb_session);
            close(fd);
            fprintf(stderr, "rpmb key to program fail! %s\n", TZ_GetErrorString(tzret));
            return tzret;
        }

        if (0 != (ret = EmmcRpmbProgramKey(fd, rpmb_pkt_buf, rpmb_pkt_buf))) {
            fprintf(stderr, "program rpmb key fail, ret = %d\n", ret);
            free(rpmb_pkt_buf);
            close(fd);
            if (ret > 0) {
                printf("error is \"%s\"\n", program_fail_reason[ret]);
            }
            return ret;
        }
        printf("key_program done!\n");
    }

    tzret = UREE_CreateSession(TZ_TA_MEM_UUID, &mem_session);
    if (tzret != TZ_RESULT_SUCCESS) {
        fprintf(stderr, "create memory ta session fail! %s\n", TZ_GetErrorString(tzret));
        UREE_CloseSession(rpmb_session);
        close(fd);
        return tzret;
    }

    /* alloc memory, and register for tee use */
    shared_mem_param.buffer = rpmb_pkt_buf;
    shared_mem_param.size = sizeof(rpmb_pkt_t);
    tzret = UREE_RegisterSharedmem(mem_session, &rpmb_pkt_buf_handle, &shared_mem_param);
    if (tzret != TZ_RESULT_SUCCESS) {
        fprintf(stderr, "register shared memory fail! %s\n", TZ_GetErrorString(tzret));
        UREE_CloseSession(mem_session);
        UREE_CloseSession(rpmb_session);
        close(fd);
        return tzret;
    }

    rpmb_res_pkt = malloc(rpmb_read_size+RPMB_DATA_LENGTH);
    if (rpmb_res_pkt == NULL) {
        fprintf(stderr, "malloc fore read buffer fail! errno=%d (%s)\n", errno, strerror(errno));
        UREE_CloseSession(mem_session);
        UREE_CloseSession(rpmb_session);
        close(fd);
        return ret;
    }
    memset(rpmb_res_pkt, 0, rpmb_read_size+RPMB_DATA_LENGTH);
    memset(rpmb_pkt_buf, 0, sizeof(rpmb_pkt_t));
    rpmb_pkt_buf->u2Address = rpmb_read_addr;
    memset(rpmb_pkt_buf->au1Nonce, 0xaa, RPMB_NONCE_LENGTH);
    ret = EmmcRpmbAuthRead(fd, rpmb_pkt_buf, rpmb_res_pkt, rpmb_read_size);
    if (0 != ret) {
        fprintf(stderr, "rpmb read @0 fail! ret=%d errno=%d (%s)\n", ret, errno, strerror(errno));
        free(rpmb_res_pkt);
        UREE_CloseSession(mem_session);
        UREE_CloseSession(rpmb_session);
        close(fd);
        return ret;
    }

    printf("\nread buf:");
    pcbuf = (char *)rpmb_res_pkt;
    do {
        for (i=0; i < sizeof(rpmb_pkt_t); i++) {
            if (i%16 == 0) printf("\n");
            printf("%02x ", pcbuf[i]);
        }
        rpmb_read_size -= RPMB_DATA_LENGTH;
        pcbuf += sizeof(rpmb_pkt_t);
        printf("\n---\n");
    }while (rpmb_read_size > 0);
    printf("\nResult=%d (%s)\n", rpmb_res_pkt->u2Result,
           program_fail_reason[(rpmb_res_pkt->u2Result <= 7)?
           (rpmb_res_pkt->u2Result):(7)]);

    free(rpmb_res_pkt);
    if (test_writeread != 0) {
        tzret = UREE_CreateSession(TZ_TA_TEST_UUID, &test_session);
        if (tzret != TZ_RESULT_SUCCESS) {
            fprintf(stderr, "create test ta session fail! %s\n", TZ_GetErrorString(tzret));
            ret = tzret;
            goto end;
        }
        do {
            #define RPMB_READ_WRITE_TEST_SIZE (32768)
            unsigned char *writebuffer;
            unsigned char *readbuffer;
            UREE_SHAREDMEM_HANDLE read_buf_handle, write_buf_handle;
            UREE_SHAREDMEM_PARAM rw_buf_param;

            writebuffer = malloc(RPMB_READ_WRITE_TEST_SIZE);
            readbuffer = malloc(RPMB_READ_WRITE_TEST_SIZE);

            rw_buf_param.buffer = writebuffer;
            rw_buf_param.size = RPMB_READ_WRITE_TEST_SIZE;
            tzret = UREE_RegisterSharedmem(mem_session, &write_buf_handle, &rw_buf_param);
            if (tzret != TZ_RESULT_SUCCESS) {
                printf("reg for write shared memory fail!\n");
                return -1;
            }

            rw_buf_param.buffer = readbuffer;
            rw_buf_param.size = RPMB_READ_WRITE_TEST_SIZE;
            tzret = UREE_RegisterSharedmem(mem_session, &read_buf_handle, &rw_buf_param);
            if (tzret != TZ_RESULT_SUCCESS) {
                printf("reg for read shared memory fail!\n");
                return -1;
            }
            for (i=0; i < RPMB_READ_WRITE_TEST_SIZE; i++)
                writebuffer[i] = i/256;
            memset(readbuffer, 0, RPMB_READ_WRITE_TEST_SIZE);

            fprintf(stderr, "rpmbTest\n");
            UREE_RpmbTestWriteRead(test_session, write_buf_handle, read_buf_handle, RPMB_READ_WRITE_TEST_SIZE);
            UREE_UnregisterSharedmem(mem_session, read_buf_handle);
            UREE_UnregisterSharedmem(mem_session, write_buf_handle);
            for (i=0; i < RPMB_READ_WRITE_TEST_SIZE; i++) {
                if (i%16 == 0) printf("\n%08x:", i);
                printf("%02x", readbuffer[i]);
            }
        } while (0);
        UREE_CloseSession(test_session);
    }

end:
    UREE_UnregisterSharedmem(mem_session, rpmb_pkt_buf_handle);
    UREE_CloseSession(mem_session);
    UREE_CloseSession(rpmb_session);
    close(fd);
    ret = 0;
    return ret;
}
