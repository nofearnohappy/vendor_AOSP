/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#if defined(MTK_FTM_C2K_SUPPORT) && !defined(EVDO_FTM_DT_VIA_SUPPORT)

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <fcntl.h>
#include <c2kutils.h>
#include "common.h"
#include "miniui.h"
#include "ftm.h"

#include "hardware/ccci_intf.h"

extern int send_at(const int fd, const char *pCMD);
extern int wait4_ack(const int fd, char *pACK, int timeout);
extern int openDeviceWithDeviceName(char *deviceName);
extern bool is_support_modem(int modem);

#define TAG    "[RF_C2K] "

#ifdef MTK_ECCCI_C2K
#define MAX_MODEM_INDEX 4
#else
#define MAX_MODEM_INDEX 3
#endif

static void *rf_c2k_update_thread(void *priv);
int rf_c2k_test_entry(struct ftm_param *param, void *priv);
int rf_c2k_test_init(void);
int get_ccci_path(int modem_index,char * path);

static item_t rf_c2k_items[] = {
    item(-1, NULL),
};

struct rf_c2k_factory {
    char info[1024];
    text_t title;
    text_t text;
    struct ftm_module *mod;
    struct itemview *iv;
    pthread_t update_thread;
    bool exit_thread;
    bool test_done;
};

#define mod_to_rf(p)  (struct rf_c2k_factory*)((char*)(p) + sizeof(struct ftm_module))

static void *rf_c2k_update_thread(void *priv)
{
    struct rf_c2k_factory *rf = (struct rf_c2k_factory*)priv;
    struct itemview *iv = rf->iv;
    const int THRESHOLD = -95;//-95dbm
    int sqm = -1;
    int rssi = 0;
    int fd3 = -1;
    int fd1 = -1;
    int i = 0;
    const int HALT_TIME = 200000;//0.2s (200000ms)
    char atDevPath1[32] = {0};
#ifdef MTK_ECCCI_C2K
    char atDevPath3[32] = {0};
#else
    char *atDevPath3 = NULL;
#endif

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    if (is_support_modem(4)) {
#ifdef MTK_FTM_SVLTE_SUPPORT
        LOGD(TAG "Get CCCI path of modem1");
        if (get_ccci_path(0, atDevPath1) == 0) {
            LOGD(TAG "Can't get CCCI path!");
            goto err;
        }
        LOGD(TAG "Go to open modem1 fd1 atDevPath1 = %s", atDevPath1);
        fd1 = openDeviceWithDeviceName(atDevPath1);
        if (fd1 < 0)
         {
             LOGD(TAG "Fail to open fd1\n");
             goto err;
         }
         LOGD(TAG "OK to open fd1\n");

         for (i = 0; i < 30; i++) usleep(50000); //sleep 1.5s wait for modem bootup
#endif
         LOGD(TAG "Go to open C2K modem fd3!");
#ifdef MTK_ECCCI_C2K
         if (get_ccci_path(3, atDevPath3) == 0) {
            LOGD(TAG "Can't get CCCI path!");
            goto err;
         }
#else
         atDevPath3 = viatelAdjustDevicePathFromProperty(VIATEL_CHANNEL_AT);
#endif
         LOGD(TAG "Go to open modem3 fd3 atDevPath3 = %s", atDevPath3);
         fd3 = openDeviceWithDeviceName(atDevPath3);
         if (fd3 < 0)
         {
             LOGD(TAG "Fail to open atDevPath3\n");
             goto err;
         }
         LOGD(TAG "OK to open atDevPath3\n");
         for (i = 0; i < 30; i++) usleep(50000); //sleep 1.5s wait for modem bootup
    } else {
        LOGD(TAG "Not support C2K modem\n");
        goto err;
    }

    int ret = 0;
    int retryCount = 0;
    memset(rf->info, 0, sizeof(rf->info) / sizeof(*(rf->info)));
    if (!rf->test_done) {
        bool ret = false;
        rf->test_done = true;

        const int BUF_SIZE = 256;
        char cmd_buf[BUF_SIZE];
        char rsp_buf[BUF_SIZE];
#ifdef MTK_FTM_SVLTE_SUPPORT
        LOGD(TAG "[MD1] AT polling first:\n");
        do
        {
            send_at (fd1, "AT\r\n");
        } while (wait4_ack (fd1, NULL, 300));
        LOGD(TAG "[MD1]Send AT+ESLP=0 to disable sleep mode:\n");
        if (send_at (fd1, "AT+ESLP=0\r\n")) goto err;
        if (wait4_ack (fd1, NULL, 5000)) goto err;

        LOGD(TAG "[MD1]Send AT+ESIMS=1 to reset SIM1:\n");
        if (send_at (fd1, "AT+ESIMS=1\r\n")) goto err;
        if (wait4_ack (fd1, NULL, 5000)) goto err;

        LOGD(TAG "[MD1]Send AT+EFUN=0\n");
        if (send_at (fd1, "AT+EFUN=0\r\n")) goto err;
        if (wait4_ack(fd1, NULL, 5000)) goto err;

        LOGD(TAG "[MD1]Send AT+EMDSTATUS=1,1\n");
        if (send_at(fd1, "AT+EMDSTATUS=1,1\r\n")) goto err;
        if (wait4_ack(fd1, NULL, 5000)) goto err;

        LOGD(TAG "[MD1]send AT+EFUN=1\n");
        if (send_at (fd1, "AT+EFUN=1\r\n")) goto err;
        if (wait4_ack (fd1, NULL, 3000)) goto err;

#endif
        LOGD(TAG "[MD3]AT polling first:\n");
        send_at(fd3, "ate0q0v1\r\n");
        do
        {
            send_at(fd3, "AT\r\n");
        } while (wait4_ack(fd3, NULL, 3000));

        LOGD(TAG "[MD3]Send AT+CPOF to reboot modem \n");
        if (send_at(fd3, "AT+CPOF\r\n")) goto err;
        wait4_ack(fd3, NULL, 5000);

        LOGD(TAG "[MD3]Wait for +VPON:0, C2K modem turn off:\n");
        wait4_ack (fd3, "+VPON:0", 3000);
#ifdef MTK_FTM_SVLTE_SUPPORT
        LOGD(TAG "[MD3]Send AT+EMDSTATUS=1,1\n");
        if (send_at(fd3, "AT+EMDSTATUS=1,1\r\n")) goto err;
        wait4_ack(fd3, NULL, 5000);
#endif
        /* Reboot modem to make new band setting work */
        LOGD(TAG "[MD3]Send AT+CPON to reboot modem \n");
        if (send_at(fd3, "AT+CPON\r\n")) goto err;
        wait4_ack(fd3, NULL, 5000);

        LOGD(TAG "[MD3]Wait for +VSER:0, found network\n");
        wait4_ack (fd3, "+VSER:0", 15000);

        /* Start RF test */
        retryCount = 0;

        while (retryCount < 100) {
            const char *tok = "+CSQ:";
            char *p = NULL;
            char *value = NULL;
            sqm = -1;
            rssi = 0;

            /* Check RF SQM level. The verdict of RF test is the SQM level shall be greater than 18 */
            LOGD(TAG "\n");
            usleep(HALT_TIME);

            memset(cmd_buf, 0, sizeof(cmd_buf));
            strcpy(cmd_buf, "AT+CSQ\r\n");
            write(fd3, cmd_buf, strlen(cmd_buf));

            LOGD(TAG "Send AT+CSQ to check RF, retryCount=%d \n",retryCount);

            memset(rsp_buf, 0, sizeof(rsp_buf));
            read(fd3, rsp_buf, BUF_SIZE);
            LOGD(TAG "------AT+CSQ start------\n");
            LOGD(TAG "%s\n", rsp_buf);
            LOGD(TAG "------AT+CSQ end------\n");
            retryCount++;

            p = strstr(rsp_buf, tok);
            if (p != NULL)
            {
                LOGD(TAG "p=%s\n", p);
                p = strchr(p, ':');
                if (p == NULL)
                {
                    LOGE(TAG "Invalid CSQ response urc");
                    goto err;
                }
                p++;
                //skip whitespace if any
                while (*p != '\0' && isspace(*p)) {
                   p++;
                }

                value = strsep(&p, ":");
                sqm = atoi(value);
                LOGD(TAG "sqm=%d\n", sqm);

                if (sqm > 0 && sqm < 31) {
                    rssi = sqm * 2 - 114;
                } else if (sqm == 0) {
                    //rssi = -113;
                    //Don't update rssi, and goto retry to get valid value
                    printf("sqm = 0 ignore.\n");
                } else if (sqm == 31) {
                    rssi = -51;
                } else {
                    printf("invalid sqm value.\n");
                }
                LOGD(TAG "rssi=%d\n", rssi);

            }

            /* AT+CSQ might got null immeidate response or modem did not have any measurement result yet. keep retry polling */
            if (rssi >= THRESHOLD && rssi != 0) {
                ret = 1;
                LOGD(TAG "rssi pass in RF test");
                break;
            } else if (rssi < THRESHOLD) {
                ret = 0;
                LOGD(TAG "rssi fail in RF test");
                break;
            }
        }

        if (ret) {
            LOGD(TAG "C2K RF Test result pass\n");
            
            sprintf(rf->info + strlen(rf->info),
                "%s: %s. RSSI value is %d dbm\n", uistr_rf_c2k_test, uistr_info_pass, rssi);
            close(fd3);
            fd3 = -1;
            close(fd1);
            fd1 = -1;

            iv->redraw(iv);
            usleep(25 * HALT_TIME); //Wait 5s to show test info
            LOGD(TAG "%s: Exit\n", __FUNCTION__);

            return NULL;
        } else {
            LOGD(TAG "C2K RF Test result fail\n");
            goto err;
        }

        LOGD(TAG "redraw\n");
        iv->redraw(iv);
    }

err:
    LOGD(TAG "%s: FAIL\n", __FUNCTION__);
    sprintf(rf->info + strlen(rf->info),
        "%s: %s. RSSI value is %d dbm\n", uistr_rf_c2k_test, uistr_info_fail, rssi);

    if (fd1 > 0)
    {
        close(fd1);
    }
    fd1 = -1;
    if (fd3 > 0)
    {
        close(fd3);
    }
    fd3 = -1;

    LOGD(TAG "redraw\n");
    iv->redraw(iv);
    usleep(25 * HALT_TIME); //Wait 5s to show test info
    LOGD(TAG "%s: Exit\n", __FUNCTION__);

    return NULL;
}

int rf_c2k_test_entry(struct ftm_param *param, void *priv)
{
    bool exit = false;
    int  passCount = 0;
    struct rf_c2k_factory *rf = (struct rf_c2k_factory*)priv;
    struct itemview *iv = NULL;

    LOGD(TAG "%s: Start\n", __FUNCTION__);
    strcpy(rf->info,"");
    init_text(&rf->title, param->name, COLOR_YELLOW);
    init_text(&rf->text, &rf->info[0], COLOR_YELLOW);

    if (NULL == rf->iv) {
        iv = ui_new_itemview();
        if (!iv) {
            LOGD(TAG "No memory for item view");
            return -1;
        }
        rf->iv = iv;
    }
    iv = rf->iv;
    iv->set_title(iv, &rf->title);
    iv->set_items(iv, rf_c2k_items, 0);
    iv->set_text(iv, &rf->text);
    iv->start_menu(iv,0);
    iv->redraw(iv);

    rf->exit_thread = false;

    pthread_create(&rf->update_thread, NULL, rf_c2k_update_thread, priv);


    strcpy(rf->info, "");
    rf->test_done = false;
    while (strlen(rf->info) == 0) {
        usleep(200000);
        if (strstr(rf->info, uistr_info_pass)) {
           passCount++;
        }
    }
    LOGD(TAG "passCount = %d\n", passCount);

    //Exit RF Test thread
    rf->exit_thread = true;
    rf->test_done = true;

    pthread_join(rf->update_thread, NULL);

    //Check test result
    if (passCount == 1) {
        rf->mod->test_result = FTM_TEST_PASS;
    } else {
        rf->mod->test_result = FTM_TEST_FAIL;
    }

    LOGD(TAG "%s: End\n", __FUNCTION__);

  return 0;
}

int rf_c2k_test_init(void)
{
    int ret = 0;
    struct ftm_module *mod;
    struct rf_c2k_factory *rf;

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    mod = ftm_alloc(ITEM_RF_C2K_TEST, sizeof(struct rf_c2k_factory));
    if(!mod) {
        return -ENOMEM;
    }
    rf = mod_to_rf(mod);
    rf->mod = mod;
    rf->test_done = true;

    ret = ftm_register(mod, rf_c2k_test_entry, (void*)rf);
    if (ret) {
        LOGD(TAG "register rf_c2k_test_entry failed (%d)\n", ret);
    }

    return ret;
}

int get_ccci_path(int modem_index,char * path)
{
#ifdef MTK_ECCCI_C2K
    int idx[MAX_MODEM_INDEX] = {1, 2, 5, 4};
    int md_sys[MAX_MODEM_INDEX] = {MD_SYS1, MD_SYS2, MD_SYS5, MD_SYS3};
#else
    int idx[MAX_MODEM_INDEX] = {1,2,5};
    int md_sys[MAX_MODEM_INDEX] = {MD_SYS1, MD_SYS2, MD_SYS5};
#endif
    LOGD(TAG "modem_index:%d", modem_index);
    int i = 0;

    if(is_support_modem(idx[modem_index]))
    {
#ifdef MTK_ECCCI_C2K
       if (modem_index == 3) {
            LOGD(TAG "prepare to get md3 node");
            snprintf(path, 32, "%s", ccci_get_node_name(USR_C2K_AT, (CCCI_MD)md_sys[modem_index]));
            LOGD(TAG "got md3 node %s", path);
        } else {
           snprintf(path, 32, "%s", ccci_get_node_name(USR_FACTORY_DATA, (CCCI_MD)md_sys[modem_index]));
        }
#else
       snprintf(path, 32, "%s", ccci_get_node_name(USR_FACTORY_DATA, (CCCI_MD)md_sys[modem_index]));
#endif
       LOGD(TAG "CCCI Path:%s",path);
       return 1 ;
    }
    else
    {
       return 0 ;
    }
}
#endif
