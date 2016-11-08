#include "string.h"
#include "stdio.h"
#include "sys/ioctl.h"
#include "linux/mmc/ioctl.h"
#include "linux/major.h"
#include "tz_cross/ta_rpmb.h"
#include "rpmb.h"
#include <errno.h>
#include <string.h>
#define LOG_TAG "rpmb"
#include <log/log.h>

/* #define DEBUG */

static void ReverseEndian(void *data, size_t size) {
    unsigned int i;
    char tmp;
    char *swp = (char *)data;
#ifdef DEBUG
    char buf[64] = {'\0'};
    fprintf(stderr, "\nrvbuf @%p, size:%zx", data, size);
    sprintf(buf, "\nrvbuf@%p, size:%zx", data, size);
#endif
    for (i = 0 ; i< (size/2); ++i) {
        tmp = swp[i];
        swp[i] = swp[size-1-i];
        swp[size-1-i] = tmp;
#ifdef DEBUG
        if (i%16 == 0) {
            fprintf(stderr, "\n");
            ALOGD("%s", buf);
        }
        fprintf(stderr, "%02x ", swp[i]);
        sprintf(buf+3*(i%16), "%02x ", swp[i]);
#endif
    }
#ifdef DEBUG
    while (i < size) {
        if (i%16 == 0) {
            fprintf(stderr, "\n");
            ALOGD("%s", buf);
        }
        fprintf(stderr, "%02x ", swp[i]);
        sprintf(buf+3*(i%16), "%02x ", swp[i]);
        i++;
    }
    if (i%16 != 1)
        ALOGD("%s", buf);
#endif
}

int EmmcRpmbProgramKey(int fd, rpmb_pkt_t *ps_rpmb_pkt, rpmb_pkt_t *ps_res_pkt) {
    int ret;
    struct mmc_ioc_cmd idata;

    ps_rpmb_pkt->u2ReqResp = 0x1;

    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_WRITE_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 1<<31;

    ReverseEndian(ps_rpmb_pkt, sizeof(rpmb_pkt_t));
    mmc_ioc_cmd_set_data(idata, ps_rpmb_pkt);
    ALOGD("%s: write_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(wr_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    memset(ps_res_pkt, 0, sizeof(rpmb_pkt_t));
    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_READ_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 0;
    mmc_ioc_cmd_set_data(idata, ps_rpmb_pkt);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(wr_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }
    ReverseEndian(ps_res_pkt, sizeof(rpmb_pkt_t));

    if (ps_res_pkt->u2Result != 0) {
        ALOGE("%s: program fail, result=0x%x", __FUNCTION__, ps_res_pkt->u2Result);
        return ps_res_pkt->u2Result;
    }

    return ret;
}

int EmmcRpmbReadCounter(int fd, rpmb_pkt_t *ps_rpmb_pkt) {
    struct mmc_ioc_cmd idata;
    int ret;
    int idx;

    ps_rpmb_pkt->u2ReqResp = 0x2;
    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_WRITE_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 1;

    ReverseEndian(ps_rpmb_pkt, sizeof(rpmb_pkt_t));
    mmc_ioc_cmd_set_data(idata, ps_rpmb_pkt);
    ALOGD("%s: write_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(wr_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    memset(ps_rpmb_pkt, 0, sizeof(rpmb_pkt_t));

    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_READ_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 0;

    mmc_ioc_cmd_set_data(idata, ps_rpmb_pkt);
    ALOGD("%s: read_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(rd_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    ReverseEndian(ps_rpmb_pkt, sizeof(rpmb_pkt_t));
    return 0;
}


int EmmcRpmbAuthWrite(int fd, rpmb_pkt_t *pwrite_rpmb_pkt, rpmb_pkt_t *result_pkt) {
    struct mmc_ioc_cmd idata;
    int ret;
    int idx, blkcnt;

    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_WRITE_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = pwrite_rpmb_pkt->u2BlockCount;
    idata.write_flag = 1<<31;

    blkcnt = pwrite_rpmb_pkt->u2BlockCount;
    for (idx=0; idx < blkcnt; idx++)
        ReverseEndian(&pwrite_rpmb_pkt[idx], sizeof(rpmb_pkt_t));
    mmc_ioc_cmd_set_data(idata, pwrite_rpmb_pkt);
    ALOGD("%s: write_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(wr_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    /* send read result request */
    memset(result_pkt, 0, sizeof(rpmb_pkt_t));
    result_pkt->u2ReqResp = 0x5;

    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_WRITE_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 1;

    ReverseEndian(result_pkt, sizeof(rpmb_pkt_t));
    mmc_ioc_cmd_set_data(idata, result_pkt);
    ALOGD("%s: write_multiple_block 2\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(wr_mul_blk_2): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    /* read back the result */
    memset(result_pkt, 0, sizeof(rpmb_pkt_t));

    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_READ_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 0;

    mmc_ioc_cmd_set_data(idata, result_pkt);
    ALOGD("%s: read_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(rd_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    ReverseEndian(result_pkt, sizeof(rpmb_pkt_t));

    return 0;
}

int EmmcRpmbAuthRead(int fd, rpmb_pkt_t *pread_rpmb_pkt, rpmb_pkt_t *result_pkt, unsigned int size) {
    int cnt = (size + RPMB_DATA_LENGTH - 1)/RPMB_DATA_LENGTH;
    int idx;
    struct mmc_ioc_cmd idata;
    int ret;

    pread_rpmb_pkt->u2ReqResp = 0x0004;
    /* send read result request */
    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_WRITE_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = 1;
    idata.write_flag = 1;

    ReverseEndian(pread_rpmb_pkt, sizeof(rpmb_pkt_t));
    mmc_ioc_cmd_set_data(idata, pread_rpmb_pkt);
    ALOGD("%s: write_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(wr_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    memset(result_pkt, 0, sizeof(rpmb_pkt_t));
    /* read back the result */
    memset(&idata, 0, sizeof(idata));
    idata.flags = MMC_RSP_SPI_R1 | MMC_RSP_R1 | MMC_CMD_ADTC;
    idata.opcode = MMC_READ_MULTIPLE_BLOCK;
    idata.arg = 0;
    idata.blksz = 512;
    idata.blocks = cnt;
    idata.write_flag = 0;

    mmc_ioc_cmd_set_data(idata, result_pkt);
    ALOGD("%s: read_multiple_block\n", __FUNCTION__);
    if ((ret=ioctl(fd, MMC_IOC_CMD, &idata)) != 0) {
        ALOGE("%s(rd_mul_blk): ioctl ret %d, errno=%d (%s)", __FUNCTION__, ret, errno, strerror(errno));
        return ret;
    }

    for (idx=0; idx < cnt; idx++)
        ReverseEndian(&result_pkt[idx], sizeof(rpmb_pkt_t));

    return 0;
}

