/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <cutils/properties.h>

/* use nvram */
#include "CFG_BT_File.h"
#include "CFG_BT_Default.h"
#include "CFG_file_lid.h"
#include "libnvram.h"

#include "bt_kal.h"
#include "cust_bt.h"
#include "bt_drv.h"

/* Audio interface & Codec information Mapping */
struct audio_t audio_conf_map[] = {
#if defined(__MTK_MERGE_INTERFACE_SUPPORT__)
    { 0x6628,    { MERGE_INTERFACE,  SYNC_8K,  SHORT_FRAME,  0 } },
#else
    { 0x6628,    { PCM,              SYNC_8K,  SHORT_FRAME,  0 } },
#endif
#if defined(__MTK_MERGE_INTERFACE_SUPPORT__)
    { 0x6630,    { MERGE_INTERFACE,  SYNC_8K,  SHORT_FRAME,  0 } },
#elif defined(__MTK_BT_I2S_SUPPORT__)
    { 0x6630,    { I2S,              SYNC_8K,  SHORT_FRAME,  0 } },
#else
    { 0x6630,    { PCM,              SYNC_8K,  SHORT_FRAME,  0 } },
#endif
    { 0x6582,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x6592,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x6752,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x0321,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x0335,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x0337,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x6580,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0x6755,    { CVSD_REMOVAL,     SYNC_8K,  SHORT_FRAME,  0 } },
    { 0,         { 0 } }
};


/**************************************************************************
 *              F U N C T I O N   D E C L A R A T I O N S                 *
***************************************************************************/

extern BOOL BT_InitDevice(
    INT32   comPort,
    UINT32  chipId,
    PUCHAR  pucNvRamData,
    UINT32  u4Baud,
    UINT32  u4HostBaud,
    UINT32  u4FlowControl,
    SETUP_UART_PARAM_T setup_uart_param
);

extern BOOL BT_DeinitDevice(INT32 comPort);

/**************************************************************************
 *                          F U N C T I O N S                             *
***************************************************************************/

static BOOL is_memzero(unsigned char *buf, int size)
{
    int i;
    for (i = 0; i < size; i++) {
        if (*(buf+i) != 0) return FALSE;
    }
    return TRUE;
}

/* Initialize UART driver */
static int init_uart(char *dev)
{
    int fd, i;

    LOG_TRC();

    fd = open(dev, O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (fd < 0) {
        LOG_ERR("Can't open serial port\n");
        return -1;
    }

    return fd;
}

int bt_get_combo_id(unsigned int *pChipId)
{
    int  chipId_ready_retry = 0;
    char chipId_val[PROPERTY_VALUE_MAX];

    do {
        if (property_get("persist.mtk.wcn.combo.chipid", chipId_val, NULL) &&
            0 != strcmp(chipId_val, "-1")){
            *pChipId = (unsigned int)strtoul(chipId_val, NULL, 16);
            break;
        }
        else {
            chipId_ready_retry ++;
            usleep(500000);
        }
    } while(chipId_ready_retry < 10);

    LOG_DBG("Get combo chip id retry %d\n", chipId_ready_retry);
    if (chipId_ready_retry >= 10) {
        LOG_DBG("Invalid combo chip id!\n");
        return -1;
    }
    else {
        LOG_DBG("Combo chip id %x\n", *pChipId);
        return 0;
    }
}

static int bt_read_nvram(unsigned char *pucNvRamData)
{
    F_ID bt_nvram_fd = {0};
    int rec_size = 0;
    int rec_num = 0;
    ap_nvram_btradio_struct bt_nvram;

    int nvram_ready_retry = 0;
    char nvram_init_val[PROPERTY_VALUE_MAX];

    LOG_TRC();

    /* Sync with Nvram daemon ready */
    do {
        if (property_get("service.nvram_init", nvram_init_val, NULL) &&
            0 == strcmp(nvram_init_val, "Ready"))
            break;
        else {
            nvram_ready_retry ++;
            usleep(500000);
        }
    } while(nvram_ready_retry < 10);

    LOG_DBG("Get NVRAM ready retry %d\n", nvram_ready_retry);
    if (nvram_ready_retry >= 10) {
        LOG_ERR("Get NVRAM restore ready fails!\n");
        return -1;
    }

    bt_nvram_fd = NVM_GetFileDesc(AP_CFG_RDEB_FILE_BT_ADDR_LID, &rec_size, &rec_num, ISREAD);
    if (bt_nvram_fd.iFileDesc < 0) {
        LOG_WAN("Open BT NVRAM fails errno %d\n", errno);
        return -1;
    }

    if (rec_num != 1) {
        LOG_ERR("Unexpected record num %d", rec_num);
        NVM_CloseFileDesc(bt_nvram_fd);
        return -1;
    }

    if (rec_size != sizeof(ap_nvram_btradio_struct)) {
        LOG_ERR("Unexpected record size %d ap_nvram_btradio_struct %d",
                rec_size, sizeof(ap_nvram_btradio_struct));
        NVM_CloseFileDesc(bt_nvram_fd);
        return -1;
    }

    if (read(bt_nvram_fd.iFileDesc, &bt_nvram, rec_num*rec_size) < 0) {
        LOG_ERR("Read NVRAM fails errno %d\n", errno);
        NVM_CloseFileDesc(bt_nvram_fd);
        return -1;
    }

    NVM_CloseFileDesc(bt_nvram_fd);
    memcpy(pucNvRamData, &bt_nvram, sizeof(ap_nvram_btradio_struct));

    return 0;
}


/* MTK specific chip initialize process */
int bt_init(void)
{
    int fd = -1;
    unsigned int chipId = 0;
    unsigned char ucNvRamData[sizeof(ap_nvram_btradio_struct)] = {0};
    unsigned int speed, flow_control;
    SETUP_UART_PARAM_T uart_setup_callback = NULL;

    LOG_TRC();

    fd = init_uart(CUST_BT_SERIAL_PORT);
    if (fd < 0){
        LOG_ERR("Can't initialize" CUST_BT_SERIAL_PORT "\n");
        return -1;
    }

    /* Get combo chip id */
    if (bt_get_combo_id(&chipId) < 0) {
        LOG_ERR("Get combo chip id fails\n");
        goto error;
    }

    /* Read NVRAM data */
    if ((bt_read_nvram(ucNvRamData) < 0) ||
          is_memzero(ucNvRamData, sizeof(ap_nvram_btradio_struct))) {
        LOG_ERR("Read NVRAM data fails or NVRAM data all zero!!\n");
        LOG_WAN("Use %x default value\n", chipId);
        switch (chipId) {
          case 0x6628:
            /* Use MT6628 default value */
            memcpy(ucNvRamData, &stBtDefault_6628, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x6630:
            /* Use MT6630 default value */
            memcpy(ucNvRamData, &stBtDefault_6630, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x6582:
            /* Use MT6582 default value */
            memcpy(ucNvRamData, &stBtDefault_6582, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x6592:
            /* Use MT6592 default value */
            memcpy(ucNvRamData, &stBtDefault_6592, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x6752:
            /* Use MT6752 default value */
            memcpy(ucNvRamData, &stBtDefault_6752, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x0321:
            /* Use MT6735 default value */
            memcpy(ucNvRamData, &stBtDefault_6735, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x0335:
            /* Use MT6735m default value */
            memcpy(ucNvRamData, &stBtDefault_6735m, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x0337:
            /* Use MT6753 default value */
            memcpy(ucNvRamData, &stBtDefault_6753, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x6580:
            /* Use MT6580 default value */
            memcpy(ucNvRamData, &stBtDefault_6580, sizeof(ap_nvram_btradio_struct));
            break;
          case 0x6755:
            /* Use MT6755 default value */
            memcpy(ucNvRamData, &stBtDefault_6755, sizeof(ap_nvram_btradio_struct));
            break;
          default:
            LOG_ERR("Unknown combo chip id\n");
            goto error;
        }
    }

    LOG_WAN("[BDAddr %02x-%02x-%02x-%02x-%02x-%02x][Voice %02x %02x][Codec %02x %02x %02x %02x] \
            [Radio %02x %02x %02x %02x %02x %02x][Sleep %02x %02x %02x %02x %02x %02x %02x][BtFTR %02x %02x] \
            [TxPWOffset %02x %02x %02x][CoexAdjust %02x %02x %02x %02x %02x %02x]\n",
            ucNvRamData[0], ucNvRamData[1], ucNvRamData[2], ucNvRamData[3], ucNvRamData[4], ucNvRamData[5],
            ucNvRamData[6], ucNvRamData[7],
            ucNvRamData[8], ucNvRamData[9], ucNvRamData[10], ucNvRamData[11],
            ucNvRamData[12], ucNvRamData[13], ucNvRamData[14], ucNvRamData[15], ucNvRamData[16], ucNvRamData[17],
            ucNvRamData[18], ucNvRamData[19], ucNvRamData[20], ucNvRamData[21], ucNvRamData[22], ucNvRamData[23], ucNvRamData[24],
            ucNvRamData[25], ucNvRamData[26],
            ucNvRamData[27], ucNvRamData[28], ucNvRamData[29],
            ucNvRamData[30], ucNvRamData[31], ucNvRamData[32], ucNvRamData[33], ucNvRamData[34], ucNvRamData[35]);


    if (BT_InitDevice(
          fd,
          chipId,
          ucNvRamData,
          speed,
          speed,
          flow_control,
          uart_setup_callback) == FALSE) {

        LOG_ERR("Initialize BT device fails\n");
        goto error;
    }

    LOG_WAN("bt_init success\n");
    return fd;

error:
    if (fd >= 0)
        close(fd);
    return -1;
}

/* MTK specific deinitialize process */
int bt_restore(int fd)
{
    LOG_TRC();
    BT_DeinitDevice(fd);
    close(fd);
    return 0;
}

int write_com_port(int fd, unsigned char *buf, unsigned int len)
{
    int nWritten = 0;
    unsigned int bytesToWrite = len;

    if (fd < 0) {
        LOG_ERR("No available com port\n");
        return -EIO;
    }

    while (bytesToWrite > 0) {
        nWritten = write(fd, buf, bytesToWrite);
        if (nWritten < 0) {
            if (errno == EINTR || errno == EAGAIN)
                break;
            else
                return -errno; /* errno used for whole chip reset */
        }
        bytesToWrite -= nWritten;
        buf += nWritten;
    }

    return (len - bytesToWrite);
}

int read_com_port(int fd, unsigned char *buf, unsigned int len)
{
    int nRead = 0;
    unsigned int bytesToRead = len;

    if (fd < 0) {
        LOG_ERR("No available com port\n");
        return -EIO;
    }

    nRead = read(fd, buf, bytesToRead);
    if (nRead < 0) {
        if(errno == EINTR || errno == EAGAIN)
            return 0;
        else
            return -errno; /* errno used for whole chip reset */
    }

    return nRead;
}

int bt_send_data(int fd, unsigned char *buf, unsigned int len)
{
    int bytesWritten = 0;
    unsigned int bytesToWrite = len;

    /* Try to send len bytes data in buffer */
    while (bytesToWrite > 0) {
        bytesWritten = write_com_port(fd, buf, bytesToWrite);
        if (bytesWritten < 0) {
            return -1;
        }
        bytesToWrite -= bytesWritten;
        buf += bytesWritten;
    }

    return 0;
}

int bt_receive_data(int fd, unsigned char *buf, unsigned int len)
{
    int bytesRead = 0;
    unsigned int bytesToRead = len;

    int ret = 0;
    struct timeval tv;
    fd_set readfd;

    tv.tv_sec = 5; /* SECOND */
    tv.tv_usec = 0; /* USECOND */
    FD_ZERO(&readfd);

    /* Try to receive len bytes */
    while (bytesToRead > 0) {

        FD_SET(fd, &readfd);
        ret = select(fd + 1, &readfd, NULL, NULL, &tv);

        if (ret > 0) {
            bytesRead = read_com_port(fd, buf, bytesToRead);
            if (bytesRead < 0) {
                return -1;
            }
            else {
                bytesToRead -= bytesRead;
                buf += bytesRead;
            }
        }
        else if (ret == 0) {
            LOG_ERR("Read com port timeout 5000ms!\n");
            return -1;
        }
        else if ((ret == -1) && (errno == EINTR)) {
            LOG_ERR("select error EINTR\n");
        }
        else {
            LOG_ERR("select error %s(%d)!\n", strerror(errno), errno);
            return -1;
        }
    }

    return 0;
}

static int bt_get_audio_configuration(BT_INFO *pBTInfo)
{
    unsigned int chipId = 0;
    int i;

    LOG_DBG("BT_MTK_OP_AUDIO_GET_CONFIG\n");

    /* Get combo chip id */
    if (bt_get_combo_id(&chipId) < 0) {
        LOG_ERR("Get combo chip id fails\n");
        return -2;
    }

    /* Return the specific audio config on current chip */
    for (i = 0; audio_conf_map[i].chip_id; i++) {
        if (audio_conf_map[i].chip_id == chipId) {
            LOG_DBG("Find chip %x\n", chipId);
            memcpy(&(pBTInfo->audio_conf), &(audio_conf_map[i].audio_conf), sizeof(AUDIO_CONFIG));
            return 0;
        }
    }

    LOG_ERR("Current chip is not included in audio_conf_map\n");
    return -3;
}

int mtk_bt_op(bt_mtk_opcode_t opcode, void *param)
{
    int ret = -1;
    switch (opcode) {
        case BT_MTK_OP_AUDIO_GET_CONFIG: {
            BT_INFO *pBTInfo = (BT_INFO*)param;
            if (pBTInfo != NULL) {
                ret = bt_get_audio_configuration(pBTInfo);
            }
            else {
                LOG_ERR("BT_MTK_OP_AUDIO_GET_CONFIG have NULL as parameter\n");
            }
            break;
        }
    default:
        LOG_ERR("Unknown operation %d\n", opcode);
        break;
    }
    return ret;
}