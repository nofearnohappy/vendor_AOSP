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
#include <uree/system.h>
#include <uree/mem.h>
#include "rpmb.h"
#include "uree_rpmb.h"
#define LOG_TAG "rpmb"
#include <log/log.h>

#define RPMB_BUF_SIZE (8*sizeof(rpmb_pkt_t)) /* 4K buffer */
#define RPMB_DEV_NODE "/dev/block/mmcblk0rpmb"
#define EMMC_CID "/sys/devices/mtk-msdc.0/11230000.MSDC0/mmc_host/mmc0/mmc0:0001/cid"

/* #define DEBUG */

int get_emmc_cid(unsigned char cid[16]) {
    int fd;
    int i;
    char str[33];
    char ostr[64];
    char *pstr;
    fd = open(EMMC_CID, O_RDONLY);
    if (fd < 0) return -1;

    if (32 != read(fd, str, 32)) {
        close(fd);
        return -2;
    }

    close(fd);
    str[32] = '\0';
#ifdef DEBUG
    sprintf(ostr, "EMMC_CID:");
    pstr = ostr + strlen("EMMC_CID:");
#endif
    for (i=15; i >= 0; i--) {
        cid[i] = (unsigned char)strtoul(&str[i*2], NULL, 16);
#ifdef DEBUG
        str[i*2] = '\0';
        sprintf(pstr, "%02x", cid[i]);
        pstr+=2;
#endif
    }
#ifdef DEBUG
    fprintf(stderr, "%s\n", ostr);
    ALOGI("%s\n", ostr);
#endif
    return 0;
}

#define READ_COUNTER_RETRY_COUNT (10)
int main(int argc, char *argv[]) {
    TZ_RESULT tzret = TZ_RESULT_SUCCESS;
    UREE_SESSION_HANDLE mem_session;
    UREE_SESSION_HANDLE rpmb_session;
    UREE_SHAREDMEM_HANDLE rpmb_pkt_buf_handle;
    UREE_SHAREDMEM_PARAM shared_mem_param;
    rpmb_pkt_t *rpmb_pkt_buf;
    int ret;
    int fd;
    uint32_t rpmb_cmd;
    unsigned char cid[16];
    int retry_cnt = 0;

    if (-1 == (fd = open(RPMB_DEV_NODE, O_RDWR))) {
        ALOGE("open rpmb node %s fail! %s\n", RPMB_DEV_NODE, strerror(errno));
        return -1;
    }

    tzret = UREE_CreateSession(TZ_TA_RPMB_UUID, &rpmb_session);
    if (tzret != TZ_RESULT_SUCCESS) {
        close(fd);
        ALOGE("create rpmb ta session fail! %s\n", TZ_GetErrorString(tzret));
        return tzret;
    }

    if (0 == get_emmc_cid(cid)) {
        UREE_RpmbInit(rpmb_session, cid, sizeof(cid));
    }

    tzret = UREE_CreateSession(TZ_TA_MEM_UUID, &mem_session);
    if (tzret != TZ_RESULT_SUCCESS) {
        UREE_CloseSession(rpmb_session);
        ALOGE("create memory ta session fail! %s\n", TZ_GetErrorString(tzret));
        close(fd);
        return tzret;
    }

    /* alloc memory, and register for tee use */
    rpmb_pkt_buf = (rpmb_pkt_t *)malloc(RPMB_BUF_SIZE);
    if (rpmb_pkt_buf == NULL) {
        UREE_CloseSession(mem_session);
        UREE_CloseSession(rpmb_session);
        close(fd);
        ALOGE("memory allocation fail (%lx\n)\n", RPMB_BUF_SIZE);
        return -1;
    }
    shared_mem_param.buffer = rpmb_pkt_buf;
    shared_mem_param.size = RPMB_BUF_SIZE;
    tzret = UREE_RegisterSharedmem(mem_session, &rpmb_pkt_buf_handle, &shared_mem_param);
    if (tzret != TZ_RESULT_SUCCESS) {
        ALOGE("register shared memory fail! %s\n", TZ_GetErrorString(tzret));
        return tzret;
    }

    tzret = UREE_RpmbRegWorkBuf(rpmb_session, rpmb_pkt_buf_handle, RPMB_BUF_SIZE);
    if (tzret != TZ_RESULT_SUCCESS) {
        ALOGE("register work buffer fail! %s\n", TZ_GetErrorString(tzret));
        return tzret;
    }

read_counter_retry:

    tzret = UREE_RpmbGetWriteCounter(rpmb_session, (unsigned char *)rpmb_pkt_buf, sizeof(rpmb_pkt_t));
    if (tzret != TZ_RESULT_SUCCESS) {
        ALOGE("rpmb get write counter error 0x%x\n", tzret);
        return tzret;
    }

    if (0 != (ret = EmmcRpmbReadCounter(fd, rpmb_pkt_buf))) {
        ALOGE("get write counter fail\n");
        UREE_CloseSession(mem_session);
        UREE_CloseSession(rpmb_session);
        close(fd);
        return ret;
    }

    tzret = UREE_RpmbFeedbackWriteCounter(rpmb_session, (const unsigned char*)rpmb_pkt_buf, sizeof(rpmb_pkt_t));
    if (tzret != TZ_RESULT_SUCCESS) {
        ALOGE("rpmb feedback write counter error 0x%x\n", tzret);
        if (retry_cnt < READ_COUNTER_RETRY_COUNT) {
            retry_cnt++;
            sleep(1);
            goto read_counter_retry;
        }
        /* write counter verify fail, possibly the emmc is not yet programmed */
        /* if fail, stay in work loop, not return */
        /* return tzret; */
    }

    /* work loop */
    do {
        ALOGV("waiting for command\n");
        tzret = UREE_RpmbWaitForCommand(rpmb_session, &rpmb_cmd);
        if (tzret != TZ_RESULT_SUCCESS) {
            ALOGE("work loop waitcmd error 0x%x (%s)\n", tzret, TZ_GetErrorString(tzret));
            sleep(1);
        }

        switch (NS_RPMB_OP(rpmb_cmd)) {
            case RPMB_READ:
                ALOGD("AuthRead\n");
                EmmcRpmbAuthRead(fd, rpmb_pkt_buf, rpmb_pkt_buf, NS_RPMB_SIZE(rpmb_cmd));
            break;
            case RPMB_WRITE:
                ALOGD("AuthWrite\n");
                EmmcRpmbAuthWrite(fd, rpmb_pkt_buf, rpmb_pkt_buf);
            break;
            case RPMB_EAGAIN:
                /* some signal interrupted the waiting process, go back for wait */
                continue;
            break;
            default:
                ALOGE("unknown cmd received: %d\n", rpmb_cmd);
                sleep(1);
            break;
        }

        ALOGV("Feedback\n");
        tzret = UREE_RpmbFeedbackResult(rpmb_session, rpmb_cmd);
        if (tzret != TZ_RESULT_SUCCESS) {
            ALOGE("work loop feedback error 0x%x (%s)\n", tzret, TZ_GetErrorString(tzret));
            sleep(1);
        }
    }while (1);

    return ret;
}
