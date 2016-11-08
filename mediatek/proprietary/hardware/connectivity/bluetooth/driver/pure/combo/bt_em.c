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
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <dlfcn.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>

#include "bt_em.h"


/* LOG_TAG must be defined before log.h */
#define LOG_TAG         "BT_EM "
#include <cutils/log.h>

#define BT_EM_DEBUG     1
#define ERR(f, ...)     ALOGE("%s: " f, __FUNCTION__, ##__VA_ARGS__)
#define WAN(f, ...)     ALOGW("%s: " f, __FUNCTION__, ##__VA_ARGS__)
#if BT_EM_DEBUG
#define DBG(f, ...)     ALOGD("%s: " f, __FUNCTION__, ##__VA_ARGS__)
#define TRC(f)          ALOGW("%s #%d", __FUNCTION__, __LINE__)
#else
#define DBG(...)        ((void)0)
#define TRC(f)          ((void)0)
#endif

/**************************************************************************
 *                  G L O B A L   V A R I A B L E S                       *
***************************************************************************/

static int bt_fd = -1;

/* Used to read serial port */
static pthread_t rxThread;

/* mtk bt library */
static void *glib_handle = NULL;
typedef int (*INIT)(void);
typedef int (*DEINIT)(int fd);
typedef int (*WRITE)(int fd, unsigned char *buf, unsigned int len);
typedef int (*READ)(int fd, unsigned char *buf, unsigned int len);

INIT    bt_init = NULL;
DEINIT  bt_restore = NULL;
WRITE   bt_send_data = NULL;
READ    bt_receive_data = NULL;

/**************************************************************************
 *                          F U N C T I O N S                             *
***************************************************************************/

BOOL EM_BT_init(void)
{
    const char *errstr;

    TRC();

    glib_handle = dlopen("libbluetooth_mtk_pure.so", RTLD_LAZY);
    if (!glib_handle) {
        ERR("%s\n", dlerror());
        goto error;
    }

    dlerror(); /* Clear any existing error */

    bt_init = dlsym(glib_handle, "bt_init");
    bt_restore = dlsym(glib_handle, "bt_restore");
    bt_send_data = dlsym(glib_handle, "bt_send_data");
    bt_receive_data = dlsym(glib_handle, "bt_receive_data");

    if ((errstr = dlerror()) != NULL) {
        ERR("Can't find function symbols %s\n", errstr);
        goto error;
    }

    bt_fd = bt_init();
    if (bt_fd < 0)
        goto error;

    DBG("BT is enabled success\n");

    return TRUE;

error:
    if (glib_handle) {
        dlclose(glib_handle);
        glib_handle = NULL;
    }

    return FALSE;
}

void EM_BT_deinit(void)
{
    TRC();

    if (!glib_handle) {
        ERR("mtk bt library is unloaded!\n");
    }
    else {
        if (bt_fd < 0) {
            ERR("bt driver fd is invalid!\n");
        }
        else{
            bt_restore(bt_fd);
            bt_fd = -1;
        }
        dlclose(glib_handle);
        glib_handle = NULL;
    }

    return;
}

BOOL EM_BT_write(unsigned char *buf, unsigned int len)
{
    UINT32 i;

    TRC();

    if (buf == NULL) {
        ERR("NULL write buffer\n");
        return FALSE;
    }

    if ((buf[0] != 0x01) && (buf[0] != 0x02) && (buf[0] != 0x03)) {
        ERR("Invalid packet type 0x%02x\n", buf[0]);
        return FALSE;
    }

    if (!glib_handle) {
        ERR("mtk bt library is unloaded!\n");
        return FALSE;
    }
    if (bt_fd < 0) {
        ERR("bt driver fd is invalid!\n");
        return FALSE;
    }

    if (bt_send_data(bt_fd, buf, len) < 0) {
        return FALSE;
    }

    /* Dump tx packet */
    DBG("write:\n");
    for (i = 0; i < len; i++) {
        DBG("%02x\n", buf[i]);
    }

    return TRUE;
}

BOOL EM_BT_read(unsigned char *buf, unsigned int len, unsigned int *pu4ResultLen)
{
    UINT8 ucHeader = 0;
    UINT32 u4Len = 0, pkt_len = 0;
    UINT32 i, count = 0;

    TRC();

    if (buf == NULL) {
        ERR("NULL read buffer\n");
        return FALSE;
    }

    if (!glib_handle) {
        ERR("mtk bt library is unloaded!\n");
        return FALSE;
    }
    if (bt_fd < 0) {
        ERR("bt driver fd is invalid!\n");
        return FALSE;
    }

LOOP:
    if (bt_receive_data(bt_fd, &ucHeader, sizeof(ucHeader)) < 0) {
        count ++;
        if (count < 3) {
            goto LOOP;
        }
        else {
            *pu4ResultLen = u4Len;
            return FALSE;
        }
    }

    buf[0] = ucHeader;
    u4Len ++;

    switch (ucHeader) {
      case 0x04:
        DBG("Receive HCI event\n");
        if (bt_receive_data(bt_fd, &buf[1], 2) < 0) {
            ERR("Read event header fails\n");
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        u4Len += 2;
        pkt_len = (UINT32)buf[2];
        if ((u4Len + pkt_len) > len) {
            ERR("Read buffer overflow! packet len %d\n", u4Len + pkt_len);
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        if (bt_receive_data(bt_fd, &buf[3], pkt_len) < 0) {
            ERR("Read event param fails\n");
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        u4Len += pkt_len;
        *pu4ResultLen = u4Len;
        break;

      case 0x02:
        DBG("Receive ACL data\n");
        if (bt_receive_data(bt_fd, &buf[1], 4) < 0) {
            ERR("Read ACL header fails\n");
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        u4Len += 4;
        pkt_len = (((UINT32)buf[4]) << 8);
        pkt_len += (UINT32)buf[3]; /*little endian*/
        if ((u4Len + pkt_len) > len) {
            ERR("Read buffer overflow! packet len %d\n", u4Len + pkt_len);
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        if (bt_receive_data(bt_fd, &buf[5], pkt_len) < 0) {
            ERR("Read ACL data fails\n");
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        u4Len += pkt_len;
        *pu4ResultLen = u4Len;
        break;

      case 0x03:
        DBG("Receive SCO data\n");
        if (bt_receive_data(bt_fd, &buf[1], 3) < 0) {
            ERR("Read SCO header fails\n");
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        u4Len += 3;
        pkt_len = (UINT32)buf[3];
        if ((u4Len + pkt_len) > len) {
            ERR("Read buffer overflow! packet len %d\n", u4Len + pkt_len);
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        if (bt_receive_data(bt_fd, &buf[4], pkt_len) < 0) {
            ERR("Read SCO data fails\n");
            *pu4ResultLen = u4Len;
            return FALSE;
        }

        u4Len += pkt_len;
        *pu4ResultLen = u4Len;
        break;

      default:
        ERR("Unexpected BT packet header %02x\n", ucHeader);
        *pu4ResultLen = u4Len;
        return FALSE;
    }

    /* Dump rx packet */
    DBG("read:\n");
    for (i = 0; i < u4Len; i++) {
        DBG("%02x\n", buf[i]);
    }

    /* If debug event, drop and retry */
    if ((buf[0] == 0x04) && (buf[1] == 0xE0)) {
        memset(buf, 0, len);
        u4Len = 0;
        count = 0;
        goto LOOP;
    }

    return TRUE;
}

static void thread_exit(int signo)
{
    pthread_t tid = pthread_self();
    DBG("Thread %lu exits\n", tid);
    pthread_exit(0);
}

static void *BT_Rx_Thread(void *ptr)
{
    UINT8 ucRxBuf[512];
    UINT32 len = 512, u4ResultLen;

    DBG("Thread %lu starts\n", rxThread);
    while (1) {
        EM_BT_read(ucRxBuf, len, &u4ResultLen);
    }

    return 0;
}

void EM_BT_polling_start(void)
{
    DBG("Test start! Keep polling event from Controller\n");

    signal(SIGRTMIN, thread_exit);
    pthread_create(&rxThread, NULL, BT_Rx_Thread, NULL);
    return;
}

void EM_BT_polling_stop(void)
{
    DBG("Test about to end! Stop polling event from Controller\n");

    pthread_kill(rxThread, SIGRTMIN);
    /* Wait until thread exit */
    pthread_join(rxThread, NULL);
    signal(SIGRTMIN, SIG_DFL);
    return;
}


void EM_BT_getChipInfo(BT_CHIP_ID *chip_id, BT_HW_ECO *eco_num)
{
    return;
}

void EM_BT_getPatchInfo(char *patch_id, unsigned int *patch_len)
{
    return;
}