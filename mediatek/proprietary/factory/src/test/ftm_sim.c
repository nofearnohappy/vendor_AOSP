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

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <fcntl.h>
#include "common.h"
#include "miniui.h"
#include "ftm.h"
#include "uistrings.h"
#include "utils.h"
#include <termios.h>
#include "hardware/ccci_intf.h"

#ifdef FEATURE_FTM_SIM

#define TAG    "[SIM] "
#define SIMLOGD(fmt, arg ...) LOGD(TAG "%s() " fmt, __FUNCTION__, ##arg)
#define SIMLOGE(fmt, arg ...) LOGE(TAG "%s() [%5d]: " fmt, __FUNCTION__, __LINE__, ##arg)
#define NUM_ELEMS(a) (sizeof (a) / sizeof (a)[0])
#define DEVICE_PATH_MAX (32)
//Sorry, pal, I've no idea why it is 7, just tried...
#define RETRY_TIMES (7)

/*For Debug Usage, 2014/11/18 {*/
//#define MTK_ENABLE_MD3
/*For Debug Usage, 2014/11/18 }*/

static void *sim_update_thread(void *priv);
static void *sim_update_thread_for_dualtalk(void *priv);
static void *sim_update_thread_for_sim_swtich(void *priv);
int sim_entry(struct ftm_param *param, void *priv);
int sim_init(void);

static int check3GSwitchStatus(const int fd);
static void sendEsuo(const int fd, int value, int wait_rsp);

static int sim_detect(const int fd, int id);
static int sim_detect_by_usimsmt(const int fd, int id);
static int sim_detect_by_esims(const int fd, int id);

extern int send_at (const int fd, const char *pCMD);

#define ERROR_NONE           0
#define ERROR_INVALID_FD    -1
#define ERROR_AT_FAIL       -2

#define RET_ESIMS_NO        0
#define RET_ESIMS_YES       1

#define AT_CMD_BUFF_SIZE  128
#define HALT_INTERVAL     20000
#define HALT_TIME         100000
#define BUF_SIZE          256

#define AT_RSP_ESIMS    "+ESIMS: "
#define AT_RSP_OK       "OK"

#define AT_ESIMEXIST "ESIMEXIST"

enum {
    ITEM_SIM1,
    ITEM_SIM2,
    ITEM_PASS,
    ITEM_FAIL,
};

static item_t sim_items_manual_mode[] = {
#ifdef MTK_DT_SUPPORT
    item(ITEM_SIM1, "Modem 1(SIM1)"),
    item(ITEM_SIM2, "Modem 2(SIM2)"),
#else
    item(ITEM_SIM1, uistr_info_detect_sim1),
  #ifdef GEMINI
    item(ITEM_SIM2, uistr_info_detect_sim2),
  #endif
#endif
    item(ITEM_PASS, uistr_info_test_pass),
    item(ITEM_FAIL, uistr_info_test_fail),
    item(-1, NULL),
};

static item_t sim_items_auto_mode[] = {
    item(-1, NULL),
};

struct sim_factory {
    char info[1024];
    text_t title;
    text_t text;
    struct ftm_module *mod;
    struct itemview *iv;
    pthread_t update_thread;
    bool exit_thread;
    bool test_done;
    int sim_id;
};

#define mod_to_sim(p)  (struct sim_factory*)((char*)(p) + sizeof(struct ftm_module))

#define SIM_ID_1   1 //slot id
#define SIM_ID_2   2 //slot id
#define SIM_ID_3   3 //slot id

//typedef enum {
//    FALSE = 0,
//    TRUE,
//} _BOOL;

char dev_node[32] = {0};
/// for dual talk
char dev_node_1[32] = {0};
char dev_node_2[32] = {0};

// for SP chip + FP chip DSDA project
char dev_node_data_1[32] = {0};
char dev_node_data_2[32] = {0};

#define EFIFO_IOC_RESET_BUFFER  _IO(CCCI_IOC_MAGIC, 203)

#define DEVICE_NAME_3   "/dev/ttyUSB1"
#define DEVICE_NAME_EXTRA   "/dev/ttyMT0"
int fd_at = -1;
int fd_atdt = -1;
#define SIM_SWITCH_MODE_CDMA    0x010001
#define SIM_SWITCH_MODE_GSM      0x010000
int fd_ioctlmd = -1;
extern int send_at (const int fd, const char *pCMD);
extern int wait4_ack (const int fd, char *pACK, int timeout);
extern int read_ack (const int fd, char *rbuff, int length);

#define SWITCH_TO_MD1 1
#define SWITCH_TO_MD5 2
int current_switch_to = -1;

/******************************************************************************
 * Global variables
 *****************************************************************************/
/*Add for supporting SIM detecting by MD3, 2014/11/18 {*/
typedef enum {
    SIM1 = 0,
    SIM2,
} SimId;

typedef struct {
    SimId sim_id;
    int fd;
    char device_node[DEVICE_PATH_MAX];
    const char **at_set;
    int at_set_count;
    int (*checkFun)(int fd, const char *at_cmd);
} SimItem;

static const char * MD3DEVICE = "/dev/ttySDIO3";

static const char* MD1ATSet[] = {
    "AT\r\n",
    "AT+ESUO=5\r\n",
    "AT+ESIMS\r\n"
};

static const char* MD3ATSet[] = {
    "ate0q0v1\r\n",
    "AT\r\n",
    "AT+ESIMS\r\n"
};

static SimItem s_sim_items[] = {
    {SIM1, -1, {0}, NULL, 0, NULL},
#if defined(GEMINI)
    {SIM2, -1, {0}, NULL, 0, NULL},
#endif
};
/******************************************************************************
 * Functions
 *****************************************************************************/
//TODO: Better refine the code
static int openDeviceNode(const char *device_node, int *ft_node);
static void closeDeviceNode(int *ft_node, char *device_node);
static void initSIMDevice();
static void initATTestSet();
static void closeSIMDevice();
static int detectSIMWarmupATs(int fd, const char **at_set, int at_counts);
static int detectSIMStatus(int fd, const char *at_cmd);
static int detectSIMStatusByC2K(int fd, const char *at_cmd);
static void *sim_update_thread_ex(void *priv);
/*Add for supporting SIM detecting by MD3, 2014/11/18 }*/
static int checkUsimSmtStatus(const char* rsp_buf);

static int checkUsimSmtStatus(const char* rsp_buf) {
    const char *tok_esims = "+USIMSMT: ";
    const char *tok_eind = "+EIND";
    char *p = NULL;
    char *p_eind = NULL;
    int ret = -1;

    p = strstr(rsp_buf, tok_esims);
    p_eind = strstr(rsp_buf, tok_eind);
    if(p) {
        p += strlen(tok_esims);
        if ('1' == *p) {
           ret = RET_ESIMS_YES;
        } else {
           ret = RET_ESIMS_NO;
        }
    } else if(p_eind) {
      	LOGD(TAG "detect +EIND, redo\n");
    }

    return ret;
}

static int checkESIMSStatus(const char* rsp_buf) {
    const char *tok_esims = "+ESIMS: ";
    const char *tok_eind = "+EIND";
    char *p = NULL;
    char *p_eind = NULL;
    int ret = -1;

    p = strstr(rsp_buf, tok_esims);
    p_eind = strstr(rsp_buf, tok_eind);
    if(p) {
        LOGD(TAG "detect +ESIMS\n");
        p += strlen(tok_esims);
        if ('1' == *p) {
           ret = RET_ESIMS_YES;
        } else {
           ret = RET_ESIMS_NO;
        }
    } else if(p_eind) {
      	LOGD(TAG "detect +EIND, redo\n");
    } else {
        LOGD(TAG "detect no match\n");
    }

    LOGD(TAG "detect +ESIMS ret %d\n", ret);
    return ret;
}

/* SVLTE To add new SIM status AT, 2015/02/02 {*/
static void SendSimExistCmd(int fd_at) {
    char cmd_buf[AT_CMD_BUFF_SIZE] = {0};
    tcflush(fd_at, TCIOFLUSH);
    sprintf(cmd_buf, "AT+%s?\r\n", AT_ESIMEXIST);
    write(fd_at, cmd_buf, strlen(cmd_buf));
    SIMLOGD("Send %s", cmd_buf);
}

static bool ReceiveSimExistCmd(int fd_at, char *rsp_buf) {
    int nread = 0;
    int rsp_len = 0;
    int retrys = 0;
    const int RETRY_UP_LIMIT = 10;
    bool is_received = false;
    char *p_tok = NULL;

    while (retrys < RETRY_UP_LIMIT) {
        nread = read_ack(fd_at, rsp_buf, BUF_SIZE);
        rsp_len = strlen(rsp_buf);
        SIMLOGD("nread=%d, rsp_len=%d, rsp_buf=%s", nread, rsp_len, rsp_buf);
        p_tok = strstr(rsp_buf, AT_ESIMEXIST);
        if (-1 != nread && p_tok) {
            is_received = true;
            break;
        } else {
            usleep(30000);
            retrys++;
        }
    }
    return is_received;
}

static int CheckSimExist(const char* rsp_buf) {
    char tok_esim_exist[AT_CMD_BUFF_SIZE] = {0};
    //+ESIMEXIST: 1
    sprintf(tok_esim_exist, "+%s: ", AT_ESIMEXIST);
    char *p = NULL;
    int ret = -1;

    p = strstr(rsp_buf, tok_esim_exist);

    if(p) {
        p += strlen(tok_esim_exist);
        if ('1' == *p) {
           ret = RET_ESIMS_YES;
        } else {
           ret = RET_ESIMS_NO;
        }
    } else {
        SIMLOGD("rep_buf: %s", rsp_buf);
    }

    SIMLOGD("%s: ret = %d.", AT_ESIMEXIST, ret);
    return ret;
}
/* SVLTE To add new SIM status AT, 2015/02/02 }*/

static void *sim_update_thread(void *priv)
{
    LOGD(TAG "%s: Start\n", __FUNCTION__);
    struct sim_factory *sim = (struct sim_factory*)priv;
    struct itemview *iv = sim->iv;
    int ret_ioctl_val = -1, fd_mdreset = -1, i = 0;
    int switchMode = 0;
    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    /*FIXME: To move the CCCI ioctrl to module function call, 2014/12/10{*/
    //mode = 1: md1->sim1&sim2; mode=2: md3->sim1, md1->sim2
    int ccci_mode = 1;
    int ccci_type = 0;
    /*FIXME: To move the CCCI ioctrl to module function call, 2014/12/10}*/

    /// choose fd
    /* Add C2K Modem feature options control 2014/12/31 {
     * MTK_C2K_SUPPORT is a base FO for indicating C2K MD is booted
     * EVDO_DT_SUPPORT is a sub FO for D* C+G/C+L dual talk product
    }*/
    /* Sorry to patch into new MACRO logic(MTK_FTM_SVLTE_SUPPORT), Orz, 2015/01/29 {*/
    /* For SVLTE to skip CCCI SIM switch , 2015/01/29 }*/

    #if !defined(MTK_FTM_SVLTE_SUPPORT) && \
            (defined(EVDO_DT_VIA_SUPPORT) || defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT))
        const char* str_sim1_mode = MTK_TELEPHONY_BOOTUP_MODE_SLOT1;
        const char* str_sim2_mode = MTK_TELEPHONY_BOOTUP_MODE_SLOT2;
        int sim1_mode = atoi(str_sim1_mode);
        int sim2_mode = atoi(str_sim2_mode);
        LOGD("bootup telephony mode [%d, %d].\n", sim1_mode, sim2_mode);
        char dev_node_ioctrl[32] = {0};
        if (sim1_mode == 0 || sim2_mode == 0) {
            /// use ioctrl to do sim switch
            #if defined(MTK_ENABLE_MD1)
                snprintf(dev_node_ioctrl, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM_IOCTL,MD_SYS1));
            #elif defined(MTK_ENABLE_MD2)
                snprintf(dev_node_ioctrl, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM_IOCTL,MD_SYS2));
            #else
                LOGD("not open md1 and md2's ioctrl");
            #endif
        }
        #if defined(MTK_ENABLE_MD1)
            fd_at= open(dev_node, O_RDWR | O_NONBLOCK);
            if (fd_at < 0) {
                LOGD(TAG "md1 open fd_at error");
                return 0;
            }
        #else
            LOGD(TAG "md1 open fd_at %d",fd_at);
        #endif

        if (sim1_mode == 0 || sim2_mode == 0) {
            fd_ioctlmd = open(dev_node_ioctrl, O_RDWR | O_NONBLOCK);
            if (fd_ioctlmd < 0) {
                LOGD(TAG "open fd_ioctlmd error");
                return 0;
            } else {
                LOGD(TAG "open fd_ioctlmd %d",fd_ioctlmd);
            }
        //for (i = 0; i < 30; i++) usleep(50000); // sleep 1s wait for modem bootup
        } else {
            fd_at= open(dev_node, O_RDWR | O_NONBLOCK);
            if (fd_at < 0) {
                LOGD(TAG "md2 open fd_at error");
                return 0;
            } else {
                LOGD(TAG "md2 open fd_at %d",fd_at);
            }

            if (sim1_mode == 0 || sim2_mode == 0) {
                fd_ioctlmd = open(dev_node_ioctrl, O_RDWR | O_NONBLOCK);
                if (fd_ioctlmd < 0) {
                    LOGD(TAG "open fd_ioctlmd error");
                    return 0;
                } else {
                    LOGD(TAG "open fd_ioctlmd %d",fd_ioctlmd);
                }
            }
        }
        /// if bootup mode is W+C or C+G, should do sim switch
        if (sim1_mode == 0 || sim2_mode == 0) {
            ///step1:off modem:AT+EPOF
            do {
                send_at (fd_at, "AT\r\n");
            } while (wait4_ack (fd_at, NULL, 300));

            LOGD(TAG "[AT]Enable Sleep Mode:\n");
            if (send_at (fd_at, "AT+ESLP=1\r\n")) return NULL;
            if (wait4_ack (fd_at, NULL, 3000)) return NULL;

            LOGD(TAG "[AT]Power OFF Modem:\n");
            if (send_at (fd_at, "AT+EFUN=0\r\n")) return NULL;
            wait4_ack (fd_at, NULL, 15000);
            if (send_at (fd_at, "AT+EPOF\r\n")) return NULL;
            wait4_ack (fd_at, NULL, 10000);
            ///step2:CCCI_IOC_ENTER_DEEP_FLIGHT
            LOGD(TAG "[AT]CCCI_IOC_ENTER_DEEP_FLIGHT \n");
            ret_ioctl_val = ioctl(fd_ioctlmd, CCCI_IOC_ENTER_DEEP_FLIGHT);
            LOGD("[AT]CCCI ioctl result: ret_val=%d, request=%d", ret_ioctl_val, CCCI_IOC_ENTER_DEEP_FLIGHT);

            ///step3:modem switch
            #if defined(EVDO_DT_VIA_SUPPORT)
                switchMode = SIM_SWITCH_MODE_GSM;
            #elif defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT)
                ret_ioctl_val = ioctl(fd_ioctlmd, CCCI_IOC_SIM_SWITCH_TYPE, &ccci_type);
                SIMLOGD("CCCI_IOC_SIM_SWITCH_TYPE, type(%d)", ccci_type);
                ccci_type = ccci_type << 16;
                switchMode = ccci_type | ccci_mode;
            #endif
            LOGD(TAG "Begin:switchMode to gsm with index %d", switchMode);
            ret_ioctl_val = ioctl(fd_ioctlmd, CCCI_IOC_SIM_SWITCH, &switchMode);
            if (ret_ioctl_val  == -1) {
                LOGD(TAG "strerror(errno)=%s", strerror(errno));
            }
            ///step4:CCCI_IOC_LEAVE_DEEP_FLIGHT
            LOGD(TAG "[AT]CCCI_IOC_LEAVE_DEEP_FLIGHT \n");
            ret_ioctl_val = ioctl(fd_ioctlmd, CCCI_IOC_LEAVE_DEEP_FLIGHT);
            LOGD("[AT]CCCI ioctl result: ret_val=%d, request=%d", ret_ioctl_val, CCCI_IOC_LEAVE_DEEP_FLIGHT);
            ///wait 50ms close() for
            usleep(50000);

            ///close ttyC0 because of enter/leave flight modem, and  sim switch
            close(fd_at);
            LOGD(TAG "close fd_at %d",fd_at);
            char state_buf[8] = {0};
            /// check md open status
            int ret_mdreset = -1;
            int md_flag = 0;
            int md_retry = 0;
            bool is_md_ready = false;
            while(!is_md_ready && md_retry < RETRY_TIMES) {
                usleep(500000);
                fd_mdreset = open("/sys/class/BOOT/BOOT/boot/md", O_RDWR);
                LOGD(TAG "ret_mdreset = %d", fd_mdreset);
                LOGD(TAG "open sys/class/BOOT/BOOT/boot/md \n");
                if (fd_mdreset < 0) {
                    fd_mdreset = open("/sys/kernel/ccci/boot", O_RDWR);
                    LOGD(TAG "open sys/kernel/ccci/boot \n");
                    md_flag = 1;
                }
                memset(state_buf, 0, sizeof(state_buf));
                if (fd_mdreset >= 0 && md_flag == 0) {
                    ret_mdreset = read(fd_mdreset, state_buf, sizeof(state_buf));
                    LOGD(TAG "flag 0's state_buf = %s, retry = %d",state_buf, md_retry);
                    is_md_ready = (state_buf[0] == '2');
                } else if (fd_mdreset >= 0 && md_flag == 1) {
                    ret_mdreset = read(fd_mdreset, state_buf, sizeof(state_buf));
                    LOGD(TAG "flag 1's state_buf = %s, retry = %d", state_buf, md_retry);
                    //(ASCII)[4B]2 is byte 6, others is byte 4
                    is_md_ready = (state_buf[6] == '2') || (state_buf[4] == '2');
                } else {
                    LOGE (TAG "open md open status file error");
                }
                md_retry++;
                close(fd_mdreset);
                fd_mdreset = -1;
            }

            ///wait a while for modem reset

            //for (i = 0; i < 10; i++) usleep(50000); // sleep 500ms wait for modem bootup

            ///step5: open ttyC0 again for AT cmd
            fd_at= open(dev_node, O_RDWR);
            if (fd_at < 0) {
                LOGD(TAG "open fd_at error");
                return 0;
            } else {
                LOGD(TAG "open fd_at %d",fd_at);
            }
        }
    #else //EVDO_DT_VIA_SUPPORT
        #if defined(PURE_AP_USE_EXTERNAL_MODEM)
            fd_at = open(DEVICE_NAME_3, O_RDWR);
        #else
            fd_at = open(dev_node, O_RDWR);
        #endif
        if(fd_at < 0) {
            LOGD(TAG "Fail to open %s: %s\n", dev_node,strerror(errno));
            return 0;
        }
    #endif  //EVDO_DT_VIA_SUPPORT
    LOGD(TAG "Device has been opened...\n");
    const int rdTimes = 3;
    int rdCount = 0;
    int tobreak = 0;
    int tr = 0, rsp_len = 0, isEsims = 1, ate_count = 1;

    struct termios options;
    cfmakeraw(&options);
    // no timeout but request at least one character per read
    options.c_cc[VTIME] = 0;
    options.c_cc[VMIN]  = 1;
    tcflush(fd_at, TCIOFLUSH);
    if (tcsetattr(fd_at, TCSANOW, &options) == -1) {
        LOGD(TAG "Fail to set %s attributes!! : %s\n",dev_node, strerror(errno));
    }

#if defined(FTM_SIM_USE_USIMSMT)
    isEsims = 0;
#endif
#if defined(GEMINI) || defined(EVDO_DT_VIA_SUPPORT) || defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT)
    ate_count = 2;
    isEsims = 1;
#endif
#if defined(MTK_GEMINI_3SIM_SUPPORT)
    ate_count = 3;
#endif
    LOGD(TAG "isEsims: %d, ate_count: %d", isEsims, ate_count);

//#if defined(PURE_AP_USE_EXTERNAL_MODEM) || defined(FTM_SIM_USE_USIMSMT)
    usleep(3000000);  //ALPS01194291: sleep 3s to wait device ready
    while (ate_count > 0) {
        tobreak = 0;
        if (ate_count == 3) {
            sendEsuo(fd_at, 6, 1);
        } else if (ate_count == 2) {
            sendEsuo(fd_at, 5, 0);
        }
        memset(cmd_buf, 0, sizeof(cmd_buf));
        memset(rsp_buf, 0, sizeof(rsp_buf));
        strcpy(cmd_buf, "ATE0\r\n");
        tcflush(fd_at, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
        write(fd_at, cmd_buf, strlen(cmd_buf));
        LOGD(TAG "Send ATE0\n");
        while (tobreak == 0) {
            read_ack(fd_at, rsp_buf, BUF_SIZE);
            rsp_len = strlen(rsp_buf);
            LOGD(TAG "------AT+ATE0 echo start------\n");
            LOGD(TAG "%d\n", rsp_len);
            LOGD(TAG "%s\n", rsp_buf);
            LOGD(TAG "------AT+ATE0 echo end------\n");
            for (tr = 0; tr < rsp_len; tr++) {
                if (rsp_buf[tr] == 'O') {
                    tobreak = 1;
                    LOGD(TAG "Got Ok!------\n");
                    break;
                }
            }
        }
        if (ate_count > 1) {
            sendEsuo(fd_at, 4, 0);
        }
        ate_count--;
    }
    usleep(HALT_TIME * 10);
//#endif

    int retryTime = 0;

    while(1) {
        LOGD(TAG "in while , sim->exit_thread:%d\n", (sim->exit_thread)? 1 : 0 );
        usleep(500000);
        if (sim->exit_thread) {
            LOGD(TAG "Exit thread\n");
            break;
        }
        //memset(sim->info, 0, sizeof(sim->info) / sizeof(*(sim->info)));
        if (!sim->test_done) {
            int ret = -1;
            sim->test_done = true;
            memset(cmd_buf, 0, sizeof(cmd_buf));
            memset(rsp_buf, 0, sizeof(rsp_buf));

            // to detect 3G capability
            retryTime = 0;

            if (isEsims == 1) {
                 /* Use ESIMS to do SIM detect */
                int sim_switch_flag = 0;
                int swtich_to_SIM2 = 0;
                int swtich_to_SIM3 = 0;
                sim_switch_flag = check3GSwitchStatus(fd_at);
                swtich_to_SIM2 = (((sim->sim_id == SIM_ID_1) && (sim_switch_flag == 2)) ||
                                  ((sim->sim_id == SIM_ID_2) && (sim_switch_flag == 1)) ||
                                  ((sim->sim_id == SIM_ID_2) && (sim_switch_flag == 3)));

                swtich_to_SIM3 = (((sim->sim_id == SIM_ID_1) && (sim_switch_flag == 3)) ||
                                  ((sim->sim_id == SIM_ID_3) && (sim_switch_flag == 1)) ||
                                  ((sim->sim_id == SIM_ID_3) && (sim_switch_flag == 2)));

                //SIM1=4, SIM2=5, SIM3=6
                if(swtich_to_SIM2) {
                    // switch UART to SIM2
                    sendEsuo(fd_at, 5, 0);
                } else if (swtich_to_SIM3) {
                    // switch UART to SIM3
                    sendEsuo(fd_at, 6, 0);
                } else {
                    SIMLOGD("No switch");
                }

                int nread = 0;
                tcflush(fd_at, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
                memset(cmd_buf, 0, sizeof(cmd_buf));
                memset(rsp_buf, 0, sizeof(rsp_buf));

                strcpy(cmd_buf, "AT+ESIMS\r\n");
                write(fd_at, cmd_buf, strlen(cmd_buf));
                LOGD(TAG "Send AT+ESIMS\n");
                tobreak = 0;
                while (tobreak == 0) {
                    nread = read_ack(fd_at, rsp_buf, BUF_SIZE);
                    rsp_len = strlen(rsp_buf);
                    LOGD(TAG "------AT+ESIMS(SIM%d) start------\n", sim->sim_id);
                    LOGD(TAG "nread= %d len=%d buf=%s \n", nread, rsp_len,rsp_buf);
                    LOGD(TAG "------AT+ESIMS(SIM%d) end------\n", sim->sim_id);
                    ret = checkESIMSStatus(rsp_buf);
                    if (ret != -1) {
                        tobreak = 1;
                        LOGD(TAG "Got response!------\n");
                        break;
                    }
                }

                /* SVLTE remote SIM to add an one more step check, 2015/02/02 {*/
                #if defined (MTK_FTM_SVLTE_SUPPORT)
                    if (0 == ret) {
                        memset(rsp_buf, 0, sizeof(rsp_buf));
                        SendSimExistCmd(fd_at);
                        if (ReceiveSimExistCmd(fd_at, rsp_buf)) {
                            ret = CheckSimExist(rsp_buf);
                        }
                    }
                #endif
                /* SVLTE remote SIM to add an one more step check, 2015/02/02 }*/

                // switch only if 3G on SIM 1
                if (swtich_to_SIM2 || swtich_to_SIM3) {
                    sendEsuo(fd_at, 4, 0);
                }
            } else {
                /* Use USIMSMT to do SIM detect */
                do {
                    retryTime++;
                    if (sim->sim_id == SIM_ID_1) {
                        strcpy(cmd_buf, "AT+USIMSMT=1\r\n");
                    } else if (sim->sim_id == SIM_ID_2) {
                        strcpy(cmd_buf, "AT+USIMSMT=2\r\n");
                    }
                    write(fd_at, cmd_buf, strlen(cmd_buf));
                    LOGD(TAG "Send AT+USIMSMT=%d\n", sim->sim_id);
                    usleep(HALT_TIME);
                    read(fd_at, rsp_buf, BUF_SIZE);
                    //usleep(HALT_TIME);
                    tr = 0;
                    tobreak = 0;
                    rsp_len = strlen(rsp_buf);
                    LOGD(TAG "------AT+USIMSMT(SIM%d) start------\n", sim->sim_id);
                    LOGD(TAG "%s\n", rsp_buf);
                    ret = checkUsimSmtStatus(rsp_buf);
                    if ((ret == RET_ESIMS_YES) || (ret == RET_ESIMS_NO)) {
                        for (tr = 0; tr < rsp_len; tr++) {
                            if (rsp_buf[tr] == 'O') {
                                tobreak = 1;
                                LOGD(TAG "Got USIMSMT Ok!------\n");
                                break;
                            }
                        }
                        if (tobreak == 0) {
                            // Didn't get OK yet, so read again
                            memset(rsp_buf, 0, sizeof(rsp_buf));
                            read(fd_at, rsp_buf, BUF_SIZE);
                            LOGD(TAG "%s\n", rsp_buf);
                        }
                    }
                    LOGD(TAG "------AT+USIMSMT(SIM%d) end------\n", sim->sim_id);
                }while (ret == -1 && (retryTime < 3));
            }

            if (ret != -1) {
                rdCount = 0;
            } else {
                if (rdCount < rdTimes) {
                    LOGD(TAG "detect unknown response, redo\n");
                    rdCount++;
                    sim->test_done = false;
                    continue;
                }
            }

            if(ret == RET_ESIMS_YES) {
                sprintf(sim->info + strlen(sim->info),
                    "%s%d: %s.\n", uistr_info_detect_sim, sim->sim_id, uistr_info_pass);
                LOGD(TAG "Detect SIM%d: Pass.\n",sim->sim_id);
            } else {
                sprintf(sim->info + strlen(sim->info),
                    "%s%d: %s!!\n", uistr_info_detect_sim, sim->sim_id, uistr_info_fail);
                LOGD(TAG "Detect SIM%d: Fail.\n",sim->sim_id);
            }
            //LOGD(TAG "redraw\n");
            //iv->redraw(iv);
        } // end if(!sim->test_done)
    } // end while(1)
    #if !defined(MTK_FTM_SVLTE_SUPPORT) && \
        (defined(EVDO_DT_VIA_SUPPORT) || defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT))
        ///maybe not need to do. This is Factory mode test, when normal power on, modem will reset again.
        if (sim1_mode == 0 || sim2_mode == 0) {
            close(fd_at);
            fd_at = -1;
            #if defined(EVDO_DT_VIA_SUPPORT)
                switchMode = SIM_SWITCH_MODE_CDMA;
            #elif defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT)
                SIMLOGD("CCCI_IOC_SIM_SWITCH_TYPE, type(%d)", ccci_type);
                ccci_mode = ccci_mode << 1;
                switchMode = ccci_type | ccci_mode;
            #endif
            LOGD(TAG "End:switchMode to cdma with index %d", switchMode);
            ret_ioctl_val = ioctl(fd_ioctlmd, CCCI_IOC_SIM_SWITCH, &switchMode);
            if (ret_ioctl_val  == -1) {
                LOGD(TAG "strerror(errno)=%s", strerror(errno));
            }
            close(fd_ioctlmd);
            fd_ioctlmd = -1;
        } else {
            close(fd_at);
            fd_at = -1;
        }
    #else
        close(fd_at);
        fd_at = -1;
    #endif
    LOGD(TAG "%s: Exit\n", __FUNCTION__);

    return NULL;
}

static void switchModem(int fd, int mode) {
    int ret_ioctl_val = -1;
    int switchType = 0, switchMode = 0;

    LOGD(TAG "switchModem, start mode: %d", mode);
    //ret_ioctl_val = ioctl(fd, CCCI_IOC_SIM_SWITCH_TYPE, &switchType);
    //if (ret_ioctl_val == -1) {
    //    LOGD(TAG "switchModem, switch type, strerror(errno)=%s", strerror(errno));
    //}
    //switchType = (switchType << 16);
    //switchMode = (switchType | mode);
#ifdef FTM_SIM_SWITCH_X2
    if (mode == 1) {
        switchMode = 0x010003;
    } else {
        switchMode = 0x010004;
    }
#else
    if (mode == 1) {
        switchMode = 0x010001;
    } else {
        switchMode = 0x010002;
    }
#endif
    LOGD(TAG "switchModem, delay 1sec - 1");
    usleep(1000000);
    ret_ioctl_val = ioctl(fd, CCCI_IOC_SIM_SWITCH, &switchMode);
    if (ret_ioctl_val == -1) {
        LOGD(TAG "switchModem, sim switch, strerror(errno)=%s, switchType: %d, switchMode: %d, mode: %d", strerror(errno), switchType, switchMode, mode);
    } else {
        LOGD(TAG "switchModem, sim switch %d, switchType: %d, switchMode: %d", ret_ioctl_val, switchType, switchMode);
        usleep(1000000);
        LOGD(TAG "switchModem, delay 1sec - 2");
    }
}

static void *sim_update_thread_for_sim_swtich(void *priv) {
    LOGD(TAG "%s: Start\n", __FUNCTION__);

    struct sim_factory *sim = (struct sim_factory*)priv;
    struct itemview *iv = sim->iv;
    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int tobreak = 0, tr = 0, rsp_len = 0;
    int ret = 0;
    int ret_fd1 = RET_ESIMS_NO;
    int ret_fd2 = RET_ESIMS_NO;
    int fd_ioctlmd = -1;


    int io_fd1 = -1;
    io_fd1 = open(dev_node_1, O_RDWR);
    if(io_fd1 < 0) {
        LOGD(TAG "fail to open %d", io_fd1);
        return NULL;
    }

    int io_fd2 = -1;
    io_fd2 = open(dev_node_2, O_RDWR);
    if(io_fd2 < 0) {
        LOGD(TAG "fail to open %d", io_fd2);
        close(io_fd1);
        return NULL;
    }

    int fd1 = -1;
    fd1 = open(dev_node_data_1, O_RDWR);
    if(fd1 < 0) {
        LOGD(TAG "fail to open %d", fd1);
        close(io_fd1);
        close(io_fd2);
        return NULL;
    }

    int fd2 = -1;
    fd2 = open(dev_node_data_2, O_RDWR);
    if(fd2 < 0) {
        LOGD(TAG "fail to open %d", fd2);
        close(io_fd1);
        close(io_fd2);
        close(fd1);
        return NULL;
    }

    LOGD(TAG "dual device has been opened...\n");

    ret = ioctl(fd2, EFIFO_IOC_RESET_BUFFER, NULL);
    LOGD(TAG "clear MD5 buffer, ret in open_ccci is %d\n", ret);

    /* Switch modem to 95 */
    switchModem(io_fd2, 1);
    current_switch_to = SWITCH_TO_MD1;

    /* ATE0 for md1 */
    usleep(3000000);  //ALPS01194291: sleep 3s to wait device ready
    memset(cmd_buf, 0, sizeof(cmd_buf));
    memset(rsp_buf, 0, sizeof(rsp_buf));
    strcpy(cmd_buf, "ATE0\r\n");
    tcflush(fd1, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
    write(fd1, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send ATE0\n");
    while (tobreak == 0) {
        read(fd1, rsp_buf, BUF_SIZE);
        rsp_len = strlen(rsp_buf);
        LOGD(TAG "------AT+ATE0 echo start------\n");
        LOGD(TAG "%d\n", rsp_len);
        LOGD(TAG "%s\n", rsp_buf);
        LOGD(TAG "------AT+ATE0 echo end------\n");
        for (tr = 0; tr < rsp_len; tr++) {
            if (rsp_buf[tr] == 'O') {
                tobreak = 1;
                LOGD(TAG "Got Ok!------\n");
                break;
            }
        }
    }
    usleep(HALT_TIME * 10);

    /* ATE0 for md2 */
    usleep(3000000);  //ALPS01194291: sleep 3s to wait device ready
    tobreak = 0;
    tr = 0;
    memset(cmd_buf, 0, sizeof(cmd_buf));
    memset(rsp_buf, 0, sizeof(rsp_buf));
    strcpy(cmd_buf, "ATE0\r\n");
    tcflush(fd2, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
    write(fd2, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send ATE0\n");
    while (tobreak == 0) {
        read(fd2, rsp_buf, (BUF_SIZE-1));
        rsp_len = strlen(rsp_buf);
        LOGD(TAG "------AT+ATE0 echo start------\n");
        LOGD(TAG "%d\n", rsp_len);
        LOGD(TAG "%s\n", rsp_buf);
        LOGD(TAG "------AT+ATE0 echo end------\n");
        for (tr = 0; tr < rsp_len; tr++) {
            if (rsp_buf[tr] == 'O') {
                tobreak = 1;
                LOGD(TAG "Got Ok!------\n");
                break;
            }
        }
    }
    usleep(HALT_TIME * 10);

    while(1) {
        usleep(200000);
        if(sim->exit_thread) {
            LOGD(TAG "exit thread");
            break;
        }

        LOGD(TAG "sim->test_done = %d", sim->test_done);
        if(!sim->test_done) {
            sim->test_done = true;
            ret_fd1 = RET_ESIMS_NO;
            ret_fd2 = RET_ESIMS_NO;
            if(sim->sim_id == SIM_ID_1) {
                if (current_switch_to == SWITCH_TO_MD1) {
                #ifdef FTM_SIM_SWITCH_X2
                    ret_fd1 = sim_detect_by_usimsmt(fd1, SIM_ID_1);
                #else
                    ret_fd1 = sim_detect_by_usimsmt(fd1, SIM_ID_1);
                #endif
                } else if (current_switch_to == SWITCH_TO_MD5) {
                #ifdef FTM_SIM_SWITCH_X2
                    ret_fd1 = sim_detect_by_usimsmt(fd2, SIM_ID_1);
                #else
                    ret_fd2 = sim_detect_by_usimsmt(fd2, SIM_ID_1);
                #endif
                }
            } else if(sim->sim_id == SIM_ID_2) {
                if (current_switch_to == SWITCH_TO_MD1) {
                #ifdef FTM_SIM_SWITCH_X2
                    ret_fd1 = sim_detect_by_usimsmt(fd2, SIM_ID_1);
                #else
                    ret_fd1 = sim_detect_by_usimsmt(fd1, SIM_ID_2);
                #endif
                    /* switch to 6261 */
                    switchModem(io_fd2, 2);
                    current_switch_to = SWITCH_TO_MD5;
                    LOGD(TAG "switch to 6261");
                } else if (current_switch_to == SWITCH_TO_MD5) {
                #ifdef FTM_SIM_SWITCH_X2
                    ret_fd2 = sim_detect_by_usimsmt(fd1, SIM_ID_1);
                #else
                    ret_fd2 = sim_detect_by_usimsmt(fd2, SIM_ID_2);
                #endif
                }
            } else {
                LOGD(TAG "invalid test item: %d\n", sim->sim_id);
            }

            if ((RET_ESIMS_YES == ret_fd1) || (RET_ESIMS_YES == ret_fd2)) {
               sprintf(sim->info + strlen(sim->info),
                       "%s%d: %s.\n", uistr_info_detect_sim, sim->sim_id, uistr_info_pass);
                LOGD (TAG "sim_update_thread:sim->info:%s, lenth:%zu",sim->info,strlen(sim->info));
            } else {
               sprintf(sim->info + strlen(sim->info),
                       "%s%d: %s!!\n", uistr_info_detect_sim, sim->sim_id, uistr_info_fail);
                LOGD (TAG "sim_update_thread:sim->info:%s, lenth:%zu",sim->info,strlen(sim->info));
            }

            //iv->redraw(iv);
        }
    }

    close(fd1);
    close(fd2);
    close(io_fd1);
    close(io_fd2);

    LOGD(TAG "%s: End\n", __FUNCTION__);
    return NULL;
}

static void *sim_update_thread_for_dualtalk(void *priv) {
    LOGD(TAG "%s: Start\n", __FUNCTION__);

    struct sim_factory *sim = (struct sim_factory*)priv;
    struct itemview *iv = sim->iv;
    int ret = RET_ESIMS_NO;
    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int tobreak = 0, tr = 0, rsp_len = 0;

    int fd1 = -1;
    fd1 = open(dev_node_1, O_RDWR);
    if(fd1 < 0) {
        LOGD(TAG "fail to open %d", fd1);
        return NULL;
    }

    int fd2 = -1;
    fd2 = open(dev_node_2, O_RDWR);
    if(fd2 < 0) {
        LOGD(TAG "fail to open %d", fd2);
        return NULL;
    }

    LOGD(TAG "dual device has been opened...\n");

    /* ATE0 for md1 */
    usleep(3000000);  //ALPS01194291: sleep 3s to wait device ready
    memset(cmd_buf, 0, sizeof(cmd_buf));
    memset(rsp_buf, 0, sizeof(rsp_buf));
    strcpy(cmd_buf, "ATE0\r\n");
    tcflush(fd1, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
    write(fd1, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send ATE0\n");
    while (tobreak == 0) {
        read(fd1, rsp_buf, BUF_SIZE);
        rsp_len = strlen(rsp_buf);
        LOGD(TAG "------AT+ATE0 echo start------\n");
        LOGD(TAG "%d\n", rsp_len);
        LOGD(TAG "%s\n", rsp_buf);
        LOGD(TAG "------AT+ATE0 echo end------\n");
        for (tr = 0; tr < rsp_len; tr++) {
            if (rsp_buf[tr] == 'O') {
                tobreak = 1;
                LOGD(TAG "Got Ok!------\n");
                break;
            }
        }
    }
    usleep(HALT_TIME * 10);

    /* ATE0 for md2 */
    usleep(3000000);  //ALPS01194291: sleep 3s to wait device ready
    tobreak = 0;
    tr = 0;
    memset(cmd_buf, 0, sizeof(cmd_buf));
    memset(rsp_buf, 0, sizeof(rsp_buf));
    strcpy(cmd_buf, "ATE0\r\n");
    tcflush(fd2, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
    write(fd2, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send ATE0\n");
    while (tobreak == 0) {
        read(fd2, rsp_buf, (BUF_SIZE-1));
        rsp_len = strlen(rsp_buf);
        LOGD(TAG "------AT+ATE0 echo start------\n");
        LOGD(TAG "%d\n", rsp_len);
        LOGD(TAG "%s\n", rsp_buf);
        LOGD(TAG "------AT+ATE0 echo end------\n");
        for (tr = 0; tr < rsp_len; tr++) {
            if (rsp_buf[tr] == 'O') {
                tobreak = 1;
                LOGD(TAG "Got Ok!------\n");
                break;
            }
        }
    }
    usleep(HALT_TIME * 10);

    while(1) {
        usleep(200000);
        if(sim->exit_thread) {
            LOGD(TAG "exit thread");
            break;
        }

        LOGD(TAG "sim->test_done = %d", sim->test_done);
        if(!sim->test_done) {
            sim->test_done = true;
            if(sim->sim_id == SIM_ID_1) {
                ret = sim_detect(fd1, SIM_ID_1);
            } else if(sim->sim_id == SIM_ID_2) {
                ret = sim_detect(fd2, SIM_ID_2);
            } else {
                LOGD(TAG "invalid test item: %d\n", sim->sim_id);
            }

            char *s = NULL;
            if(RET_ESIMS_YES == ret) {
                s = uistr_info_yes;
            } else if(RET_ESIMS_NO == ret) {
                s = uistr_info_no;
            } else {
                s = uistr_info_fail;
            }
            if (RET_ESIMS_YES == ret) {
               sprintf(sim->info + strlen(sim->info),
                       "%s%d: %s.\n", uistr_info_detect_sim, sim->sim_id, uistr_info_pass);
                LOGD (TAG "sim_update_thread:sim->info:%s, lenth:%zu",sim->info,strlen(sim->info));
            } else {
               sprintf(sim->info + strlen(sim->info),
                       "%s%d: %s!!\n", uistr_info_detect_sim, sim->sim_id, uistr_info_fail);
                LOGD (TAG "sim_update_thread:sim->info:%s, lenth:%zu",sim->info,strlen(sim->info));
            }

            //iv->redraw(iv);
        }
    }

    close(fd1);
    close(fd2);

    LOGD(TAG "%s: End\n", __FUNCTION__);
    return NULL;
}

int sim_entry(struct ftm_param *param, void *priv)
{
    bool exit = false;
    int  passCount = 0;
    struct sim_factory *sim = (struct sim_factory*)priv;
    struct itemview *iv = NULL;

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    strcpy(sim->info, "");
    init_text(&sim->title, param->name, COLOR_YELLOW);
    init_text(&sim->text, &sim->info[0], COLOR_YELLOW);

    if(NULL == sim->iv) {
        iv = ui_new_itemview();
        if(!iv) {
            LOGD(TAG "No memory for item view");
            return -1;
        }
        sim->iv = iv;
    }
    iv = sim->iv;
    iv->set_title(iv, &sim->title);
    iv->set_text(iv, &sim->text);

    LOGD(TAG "param->test_type:%d\n", param->test_type);
    if(FTM_MANUAL_ITEM == param->test_type) {
        iv->set_items(iv, sim_items_manual_mode, 0);
    } else {
        iv->set_items(iv, sim_items_auto_mode, 0);
        iv->start_menu(iv,0);
        iv->redraw(iv);
    }

    sim->exit_thread = false;

#ifdef FTM_SIM_SWITCH
    snprintf(dev_node_1, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM_IOCTL, MD_SYS1));
    snprintf(dev_node_2, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM_IOCTL, MD_SYS5));
    snprintf(dev_node_data_1, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS1));
    snprintf(dev_node_data_2, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS5));
    LOGD(TAG "dev1: %s, dev2: %s", dev_node_1, dev_node_2);
    LOGD(TAG "dev_data_1: %s, dev_data_2: %s", dev_node_data_1, dev_node_data_2);
    /* Switch to MD1 firstly */

    pthread_create(&sim->update_thread, NULL, sim_update_thread_for_sim_swtich, priv);
#else
    #if defined(MTK_DT_SUPPORT) && !defined(EVDO_DT_VIA_SUPPORT) && !defined(EVDO_DT_SUPPORT) && !defined(MTK_C2K_SUPPORT)
        snprintf(dev_node_1, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS1));
        snprintf(dev_node_2, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS2));
        pthread_create(&sim->update_thread, NULL, sim_update_thread_for_dualtalk, priv);
    #else
        /* Disable MD3 SIM detection, since external SIM switch is supported, 2015/01/19
        #if defined(MTK_DT_SUPPORT) && defined(MTK_ENABLE_MD3)
            pthread_create(&sim->update_thread, NULL, sim_update_thread_ex, priv);
        #else
            #if defined(MTK_ENABLE_MD1)
                snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS1));
            #elif defined(MTK_ENABLE_MD2)
                snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS2));
            #elif defined(MTK_ENABLE_MD5)
                snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS5));
            #else
                LOGD("not open md1,md2,md5");
            #endif
            pthread_create(&sim->update_thread, NULL, sim_update_thread, priv);
        #endif
           Disable MD3 SIM detection, since external SIM switch is supported, 2015/01/19 }*/
        #if defined(MTK_ENABLE_MD1)
            snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS1));
        #elif defined(MTK_ENABLE_MD2)
            snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS2));
        #elif defined(MTK_ENABLE_MD5)
            snprintf(dev_node, 32, "%s", ccci_get_node_name(USR_FACTORY_SIM, MD_SYS5));
        #else
            LOGD("not open md1,md2,md5");
        #endif
            pthread_create(&sim->update_thread, NULL, sim_update_thread, priv);
    #endif
#endif

    if(FTM_MANUAL_ITEM == param->test_type) {
        while(!exit) {
            int chosen = iv->run(iv, &exit);
            switch(chosen) {
                case ITEM_SIM1:
                sim->sim_id = SIM_ID_1;
                sim->test_done = false;
                exit = false;
                break;

            case ITEM_SIM2:
                sim->sim_id = SIM_ID_2;
                sim->test_done = false;
                exit = false;
                break;

            case ITEM_PASS:
            case ITEM_FAIL:
                if(ITEM_PASS == chosen) {
                  sim->mod->test_result = FTM_TEST_PASS;
                } else {
                  sim->mod->test_result = FTM_TEST_FAIL;
                }
                LOGD(TAG "sim->mod->test_result:%d\n", sim->mod->test_result);
                sim->exit_thread = true;
                sim->test_done = true;
                exit = true;
                break;

            default:
                sim->exit_thread = true;
                sim->test_done = true;
                exit = true;
                LOGD(TAG "DEFAULT EXIT\n");
                break;
            } // end switch(chosen)
            if(exit) {
                sim->exit_thread = true;
            }
        } // end while(!exit)
    } else if (FTM_AUTO_ITEM == param->test_type) {

        //Detect SIM 1
        LOGD(TAG "FTM_AUTO_ITEM detect SIM1");
        //  strcpy(sim->info, "");
        memset(sim->info, 0, sizeof(sim->info) / sizeof(*(sim->info)));
        sim->sim_id = SIM_ID_1;
        sim->test_done = false;
        while (strlen(sim->info) == 0) {
            LOGD (TAG "detect slot 1:enter");
            LOGD (TAG "sim_entry:sim->info:%s, lenth:%zu",sim->info,strlen(sim->info));
            LOGD(TAG "FTM_AUTO_ITEM detect SIM1 sim->info:%s, uistr_info_pass:%s strstr:%s", sim->info, uistr_info_pass, strstr(sim->info, uistr_info_pass));
            usleep(200000);
            if (strstr(sim->info, uistr_info_pass)) {
                passCount++;
            }
        }
        LOGD(TAG "[SLOT 1]passCount = %d\n", passCount);
        LOGD (TAG "begin redraw");
        iv->redraw(iv);
        LOGD (TAG "end redraw");

        #if defined(GEMINI) || defined(MTK_GEMINI_3SIM_SUPPORT)|| defined(EVDO_DT_VIA_SUPPORT) || defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT) || defined(FTM_SIM_USE_USIMSMT)
            //Detect SIM 2
            //  strcpy(sim->info, "");
            memset(sim->info, 0, sizeof(sim->info) / sizeof(*(sim->info)));
            sim->sim_id = SIM_ID_2;
            sim->test_done = false;
            while (strlen(sim->info) == 0) {
                LOGD (TAG "detect slot 2:enter");
                LOGD (TAG "sim_entry:sim->info:%s, lenth:%zu",sim->info,strlen(sim->info));
                usleep(200000);
                if (strstr(sim->info, uistr_info_pass)) {
                   passCount++;
                }
            }
            LOGD(TAG "[SLOT 2]passCount = %d\n", passCount);
            LOGD (TAG "begin redraw");
            iv->redraw(iv);
            usleep(1000000);
            LOGD (TAG "end redraw");
        #else
            passCount++;
            LOGD(TAG "GEMINI is not defined, do not need to check SIM2\n");
        #endif

        #if defined(MTK_GEMINI_3SIM_SUPPORT)
            //Detect SIM 3
            //  strcpy(sim->info, "");
            memset(sim->info, 0, sizeof(sim->info) / sizeof(*(sim->info)));
            sim->sim_id = SIM_ID_3;
            sim->test_done = false;
            while (strlen(sim->info) == 0) {
                LOGD (TAG "detect slot 3:enter");
                LOGD (TAG "sim_entry:sim->info:%s, lenth:%d",sim->info,strlen(sim->info));
                usleep(200000);
                if (strstr(sim->info, uistr_info_pass)) {
                   passCount++;
                }
            }
            LOGD(TAG "[SLOT 3]passCount = %d\n", passCount);
            LOGD (TAG "begin redraw");
            iv->redraw(iv);
            LOGD (TAG "end redraw");
        #else
            passCount++;
            LOGD(TAG "MTK_GEMINI_3SIM_SUPPORT is not defined, do not need to check SIM3\n");
        #endif

#ifdef FTM_SIM_SWITCH
        /* switch to 6261 and run again */
        //Detect SIM 1
        //  strcpy(sim->info, "");
        memset(sim->info, 0, sizeof(sim->info) / sizeof(*(sim->info)));
        sim->sim_id = SIM_ID_1;
        sim->test_done = false;
        while (strlen(sim->info) == 0) {
            LOGD (TAG "FTM_SIM_SWITCH, detect slot 1:enter");
            LOGD (TAG "sim_entry:sim->info:%s, lenth:%d",sim->info,strlen(sim->info));
            usleep(200000);
            if (strstr(sim->info, uistr_info_pass)) {
                passCount++;
            }
        }
        LOGD(TAG "[SLOT 1]passCount = %d\n", passCount);
        LOGD (TAG "begin redraw");
        iv->redraw(iv);
        usleep(1000000);
        LOGD (TAG "end redraw");

        #if defined(GEMINI) || defined(MTK_GEMINI_3SIM_SUPPORT)|| defined(EVDO_DT_VIA_SUPPORT) || defined(EVDO_DT_SUPPORT) || defined(MTK_C2K_SUPPORT) || defined(FTM_SIM_USE_USIMSMT)
            //Detect SIM 2
            //  strcpy(sim->info, "");
            memset(sim->info, 0, sizeof(sim->info) / sizeof(*(sim->info)));
            sim->sim_id = SIM_ID_2;
            sim->test_done = false;
            while (strlen(sim->info) == 0) {
                LOGD (TAG "FTM_SIM_SWITCH, detect slot 2:enter");
                LOGD (TAG "sim_entry:sim->info:%s, lenth:%d",sim->info,strlen(sim->info));
                usleep(200000);
                if (strstr(sim->info, uistr_info_pass)) {
                   passCount++;
                }
            }
            LOGD(TAG "[SLOT 2]passCount = %d\n", passCount);
            LOGD (TAG "begin redraw");
            iv->redraw(iv);
            usleep(1000000);
            LOGD (TAG "end redraw");
        #else
            passCount++;
            LOGD(TAG "GEMINI is not defined, do not need to check SIM2\n");
        #endif

#endif

        //Exit SIM detect thread
        sim->exit_thread = true;
        sim->test_done = true;
        LOGD(TAG "FTM_AUTO_ITEM detect done");
    }

    pthread_join(sim->update_thread, NULL);

    if (FTM_AUTO_ITEM == param->test_type) {
    //Check test result
#ifdef FTM_SIM_SWITCH
    if (passCount == 5)
#else
    if (passCount == 3)
#endif
    {
        //SIM1, SIM2 and SIM3 are detected.
        sim->mod->test_result = FTM_TEST_PASS;
    } else {
        sim->mod->test_result = FTM_TEST_FAIL;
    }
    }

    LOGD(TAG "%s: End\n", __FUNCTION__);

    return 0;
}

int sim_init(void)
{
    int ret = 0;
    struct ftm_module *mod;
    struct sim_factory *sim;

    LOGD(TAG "%s: Start\n", __FUNCTION__);

    mod = ftm_alloc(ITEM_SIM, sizeof(struct sim_factory));
    if(!mod) {
        return -ENOMEM;
    }
    sim = mod_to_sim(mod);
    sim->mod = mod;
    sim->test_done = true;

    ret = ftm_register(mod, sim_entry, (void*)sim);
    if(ret) {
        LOGD(TAG "register sim_entry failed (%d)\n", ret);
    }

    return ret;
}

static int check3GSwitchStatus(const int fd) {
    // to detect 3G capability
    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int sim_switch_flag = 1; // 1 -> 3G on SIM1, 2 -> 3G on SIM2, 3 -> 3G on SIM3

    tcflush(fd, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
    strcpy(cmd_buf, "AT+ES3G?\r\n");
    write(fd, cmd_buf, strlen(cmd_buf));
    usleep(HALT_TIME);
    LOGD(TAG "Send AT+ES3G?\n");

    const char *TOK_ES3G = "+ES3G: ";
    char *p_es3g = NULL;
    while (p_es3g == NULL) {
        memset(cmd_buf, 0, BUF_SIZE);
        read_ack(fd, cmd_buf, BUF_SIZE);
        LOGD(TAG "3G Capability: %s\n", cmd_buf);
        p_es3g = strstr(cmd_buf, TOK_ES3G);
        if(p_es3g) {
            p_es3g += strlen(TOK_ES3G);
            if('2' == *p_es3g) {
                sim_switch_flag = 2;
            } else if ('4' == *p_es3g) {
                sim_switch_flag = 3;
            }
            LOGD(TAG "3G capability is on SIM %d\n", (sim_switch_flag));
            break;
        } else {
            LOGD(TAG "No response for AT+ES3G? %s\n", p_es3g);
        #if !defined(GEMINI)
            LOGD(TAG "Single SIM project so no need to do AT+ES3G!");
            break;
        #endif
        }
    }

    return sim_switch_flag;
}

static void sendEsuo(const int fd, int value, int wait_echo) {
    char *pRsp = NULL;
    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    sprintf(cmd_buf, "AT+ESUO=%d\r\n", value);
    write(fd, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send AT+ESUO=%d\n", value);
    usleep(HALT_TIME);
    read_ack(fd, rsp_buf, BUF_SIZE);
    LOGD(TAG "wait_rsp %d, %s\n", wait_echo, rsp_buf);
    while (wait_echo) {
        pRsp = strstr(rsp_buf, "OK");
        if (strncmp(rsp_buf, "+ESUO", 5) == 0) {
            LOGD(TAG "sendEsuo got echo!");
            break;
        } else if (pRsp != NULL) {
            LOGD(TAG "sendEsuo got OK!");
            break;
        }
        pRsp = NULL;
        read_ack(fd, rsp_buf, BUF_SIZE);
        LOGD(TAG "read rsp for Esuo, %s\n", rsp_buf);
    }
    LOGD(TAG "sendEsuo end!\n");
}

static int sim_detect(const int fd, int id) {
    LOGD(TAG "%s start\n", __FUNCTION__);

    if (fd < 0) {
        LOGD(TAG "invalid fd\n");
        return ERROR_INVALID_FD;
    }

    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int ret = 0;
    int rsp_len = 0;
    int tobreak = 0;
    int i, j;

    LOGD(TAG "***SIM id = %d\n", id);
    LOGD(TAG "[AT] detect sim status\n");

    int sim_switch_flag = check3GSwitchStatus(fd);
    int shouldSendEsuo = (((id == SIM_ID_1) && (sim_switch_flag == 2)) ||
                          ((id == SIM_ID_2) && (sim_switch_flag == 1)));

    for (j = 0; j < 5; j++) {
        //if (shouldSendEsuo) {
        if (shouldSendEsuo) {
            sendEsuo(fd, 5, 0);
        }

        int nread = 0;
        tcflush(fd_at, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
        memset(cmd_buf, 0, sizeof(cmd_buf));
        memset(rsp_buf, 0, sizeof(rsp_buf));

        strcpy(cmd_buf, "AT+ESIMS\r\n");
        write(fd, cmd_buf, strlen(cmd_buf));
        LOGD(TAG "Send AT+ESIMS\n");
        tobreak = 0;
        while (tobreak == 0) {
            nread = read(fd, rsp_buf, BUF_SIZE);
            rsp_len = strlen(rsp_buf);
            LOGD(TAG "------AT+ESIMS(SIM%d) start------\n", id);
            LOGD(TAG "nread= %d len=%d buf=%s \n", nread, rsp_len,rsp_buf);
            LOGD(TAG "------AT+ESIMS(SIM%d) end------\n", id);
            ret = checkESIMSStatus(rsp_buf);
            if (ret != -1) {
                tobreak = 1;
                LOGD(TAG "Got response!------\n");
                break;
            }
        }

        //if (shouldSendEsuo) {
        if (shouldSendEsuo) {
            sendEsuo(fd, 4, 0);
        }

        // Test 5 times or detect SIM
        if (ret != 0) break;
    }

    LOGD(TAG "%s end\n", __FUNCTION__);

    return ret;
}

static int sim_detect_by_usimsmt(const int fd, int id) {
    LOGD(TAG "%s start\n", __FUNCTION__);

    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int tobreak = 0, tr = 0, rsp_len = 0, ret = 0;

    /* Use USIMSMT to do SIM detect */
    if (id == SIM_ID_1) {
        strcpy(cmd_buf, "AT+USIMSMT=1\r\n");
    } else if (id == SIM_ID_2) {
        strcpy(cmd_buf, "AT+USIMSMT=2\r\n");
    }
    write(fd, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send AT+USIMSMT=%d\n", id);
    usleep(HALT_TIME);
    read(fd, rsp_buf, (BUF_SIZE-1));
    //usleep(HALT_TIME);
    tr = 0;
    tobreak = 0;
    rsp_len = strlen(rsp_buf);
    LOGD(TAG "------AT+USIMSMT(SIM%d) start------\n", id);
    LOGD(TAG "%s\n", rsp_buf);
    ret = checkUsimSmtStatus(rsp_buf);
    if ((ret == RET_ESIMS_YES) || (ret == RET_ESIMS_NO)) {
        for (tr = 0; tr < rsp_len; tr++) {
            if (rsp_buf[tr] == 'O') {
                tobreak = 1;
                LOGD(TAG "Got USIMSMT Ok!------\n");
                break;
            }
        }
        if (tobreak == 0) {
            // Didn't get OK yet, so read again
            memset(rsp_buf, 0, sizeof(rsp_buf));
            read(fd, rsp_buf, BUF_SIZE);
            LOGD(TAG "%s\n", rsp_buf);
        }
    }
    LOGD(TAG "------AT+USIMSMT(SIM%d) end------\n", id);

    LOGD(TAG "%s end\n", __FUNCTION__);

    return ret;
}

static int sim_detect_by_esims(const int fd, int id) {
    LOGD(TAG "%s start\n", __FUNCTION__);

    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int tobreak = 0, tr = 0, rsp_len = 0, ret = 0;

    /* Use USIMSMT to do SIM detect */
    int sim_switch_flag = 0;
    int swtich_to_SIM2 = 0;
    sim_switch_flag = check3GSwitchStatus(fd);
    swtich_to_SIM2 = (((id == SIM_ID_1) && (sim_switch_flag == 2)) ||
                            ((id == SIM_ID_2) && (sim_switch_flag == 1)));

    LOGD(TAG "sim_detect_by_esims, (%d, %d)", sim_switch_flag, swtich_to_SIM2);
    //SIM1=4, SIM2=5, SIM3=6
    if(swtich_to_SIM2) {
        // switch UART to SIM2
        sendEsuo(fd, 5, 0);
    }

    int nread = 0;
    tcflush(fd, TCIOFLUSH);   //ALPS01194291: clear buffer to avoid wrong data
    memset(cmd_buf, 0, sizeof(cmd_buf));
    memset(rsp_buf, 0, sizeof(rsp_buf));

    strcpy(cmd_buf, "AT+ESIMS\r\n");
    write(fd, cmd_buf, strlen(cmd_buf));
    LOGD(TAG "Send AT+ESIMS\n");
    tobreak = 0;
    while (tobreak == 0) {
        nread = read(fd, rsp_buf, BUF_SIZE);
        rsp_len = strlen(rsp_buf);
        LOGD(TAG "------AT+ESIMS(SIM%d) start------\n", id);
        LOGD(TAG "nread= %d len=%d buf=%s \n", nread, rsp_len,rsp_buf);
        LOGD(TAG "------AT+ESIMS(SIM%d) end------\n", id);
        ret = checkESIMSStatus(rsp_buf);
        if (ret != -1) {
            tobreak = 1;
            LOGD(TAG "Got response!------\n");
            break;
        }
    }

     // switch only if 3G on SIM 1
    if (swtich_to_SIM2) {
        sendEsuo(fd, 4, 0);
    }

    LOGD(TAG "%s end\n", __FUNCTION__);

    return ret;
}

/*Add for supporting SIM detecting by MD3, 2014/11/18 {*/
//TODO: Better refine the code
/*---------------------------------------------------------------------------*/
static int openDeviceNode(const char *device_node, int *ft_node) {
    int ret = -1;
    if (device_node && ft_node) {
        do {
            struct termios options;
            *ft_node = open(device_node, O_RDWR | O_NONBLOCK);
            if (*ft_node < 0) {
                SIMLOGE("fail to open %s: %d(%s)\n", device_node, errno, strerror(errno));
                break;
            } else {
                int i = 0;
                SIMLOGD("%s is opened", device_node);
                ret = 0;
                if (!ret) {
                    //FIXME: To remove the sleep wait for modem bootup
                    for (i = 0; i < 10; i++) usleep(HALT_INTERVAL);
                }
            }
        } while (0);
    }
    return ret;
}
/*---------------------------------------------------------------------------*/
static void closeDeviceNode(int *ft_node, char *device_node) {
    if (*ft_node != -1) {
        close(*ft_node);
    }
    *ft_node = -1;
    memset(device_node, 0x00, DEVICE_PATH_MAX);
}
/*---------------------------------------------------------------------------*/
static void initSIMDevice() {

#if defined(MTK_ENABLE_MD1)
    snprintf(s_sim_items[SIM1].device_node, DEVICE_PATH_MAX, "%s",
            ccci_get_node_name(USR_FACTORY_SIM, MD_SYS1));
#elif defined(MTK_ENABLE_MD2)
    snprintf(s_sim_items[SIM1].device_node, DEVICE_PATH_MAX, "%s",
            ccci_get_node_name(USR_FACTORY_SIM, MD_SYS2));
#elif defined(MTK_ENABLE_MD5)
    snprintf(s_sim_items[SIM1].device_node, DEVICE_PATH_MAX, "%s",
            ccci_get_node_name(USR_FACTORY_SIM, MD_SYS5));
#endif

#if defined(GEMINI) && defined(MTK_ENABLE_MD3)
    snprintf(s_sim_items[SIM2].device_node, DEVICE_PATH_MAX, "%s", MD3DEVICE);
#endif
}
/*---------------------------------------------------------------------------*/
static void closeSIMDevice() {
    int i = 0;
    int items_count = NUM_ELEMS(s_sim_items);
    for (; i < items_count; i++) {
        closeDeviceNode(&(s_sim_items[i].fd), s_sim_items[i].device_node);
    }
}
/*---------------------------------------------------------------------------*/
static void initATTestSet() {
    s_sim_items[SIM1].at_set = MD1ATSet;
    s_sim_items[SIM1].at_set_count = NUM_ELEMS(MD1ATSet);
    s_sim_items[SIM1].checkFun = detectSIMStatus;
#if defined(GEMINI) && defined(MTK_ENABLE_MD3)
    s_sim_items[SIM2].at_set = MD3ATSet;
    s_sim_items[SIM2].at_set_count = NUM_ELEMS(MD3ATSet);
    s_sim_items[SIM2].checkFun = detectSIMStatusByC2K;
#endif
}
/*---------------------------------------------------------------------------*/
static int detectSIMWarmupATs(int fd, const char **at_set, int at_counts) {
    int i = 0;
    if ((-1 != fd) && at_set && at_counts) {
        for (; i < at_counts; i++) {
            if (send_at(fd, at_set[i])) {
                SIMLOGE("AT failed: %s", at_set[i]);
                break;
            } else {
                if (wait4_ack(fd, NULL, 30000)) {
                    SIMLOGE("AT timeout w/o ACK: %s", at_set[i]);
                    break;
                } else {
                    SIMLOGD("AT passed: %s", at_set[i]);
                }
            }
        }

    } else {
        SIMLOGE("Device is not ready, %d, %p, %d", fd, at_set, at_counts);
    }
	return (i == at_counts) ? 0 : -1;
}
/*---------------------------------------------------------------------------*/
static int detectSIMStatus(int fd, const char *at_cmd) {
    char cmd_buf[BUF_SIZE] = {0};
    char rsp_buf[BUF_SIZE] = {0};
    int rsp_len = 0;
    int ret = 0;
    int nread = 0;
    int retry = 0;

    strcpy(cmd_buf, at_cmd);

    while (retry < RETRY_TIMES) {
        tcflush(fd, TCIOFLUSH);
        write(fd, cmd_buf, strlen(cmd_buf));
        SIMLOGD("Send %s", cmd_buf);
        //OK, MD needs time to ACK status, waiting...
        usleep(HALT_TIME*5);
        memset(rsp_buf, 0x00, sizeof(rsp_buf));
        nread = read(fd, rsp_buf, BUF_SIZE);
        rsp_len = strlen(rsp_buf);
        SIMLOGD("nread= %d len=%d buf=%s", nread, rsp_len, rsp_buf);
        ret = checkESIMSStatus(rsp_buf);
        if (ret != -1) {
            SIMLOGD("Got response!");
            break;
        } else {
            retry++;
            SIMLOGD("retry %d", retry);
        }
    }

    return (ret == RET_ESIMS_YES) ? RET_ESIMS_YES : RET_ESIMS_NO;
}
/*---------------------------------------------------------------------------*/
static int detectSIMStatusByC2K(int fd, const char *at_cmd) {
    int ret = RET_ESIMS_NO;

    if ((-1 != fd) && at_cmd) {
        if (send_at(fd, at_cmd)) {
            SIMLOGE("AT failed: %s", at_cmd);
        } else {
            if (wait4_ack(fd, NULL, 30000)) {
                SIMLOGE("AT timeout w/o ACK: %s", at_cmd);
            } else {
                ret = RET_ESIMS_YES;
                SIMLOGD("AT passed: %s", at_cmd);
            }
        }
    }

    return ret;
}
/*---------------------------------------------------------------------------*/
static void *sim_update_thread_ex(void *priv) {
	SIMLOGD("Start");
    struct sim_factory *sim = (struct sim_factory*)priv;
    struct itemview *iv = sim->iv;
    int ret = 0;
    int i = 0;
    int j = 0;
    int items_count = NUM_ELEMS(s_sim_items);
    int at_set_count = 0;
    int sim_id = 0;

    //0. Open devices
    initSIMDevice();
    //1. Initialize AT Set
    initATTestSet();
    //2. Send AT command set
    for (; i < items_count; i++) {
        if (sim->exit_thread) {
            SIMLOGD("Exit thread");
            break;
        }
        //TODO: To revise the sync mechanism not by usleep, OMG...
        usleep(HALT_TIME*5);
        sim_id = s_sim_items[i].sim_id+1;
        ret = openDeviceNode(s_sim_items[i].device_node, &(s_sim_items[i].fd));
        if (ret < 0) {
            SIMLOGE("Fail to open device(%d): %d(%s)", sim_id, errno, strerror(errno));
        } else {
            at_set_count = s_sim_items[i].at_set_count;
            if (at_set_count > 1) {
                ret = detectSIMWarmupATs(s_sim_items[i].fd, s_sim_items[i].at_set, at_set_count-1);
            }
            if ((0 == ret) && (at_set_count > 0) && s_sim_items[i].checkFun) {
                //3. Check SIM status
                ret = s_sim_items[i].checkFun(s_sim_items[i].fd,
                        s_sim_items[i].at_set[at_set_count - 1]);
            } else {
                SIMLOGE("Fail to pass all preATs, SIM(%d), ret(%d), count(%d), fun(%p)",
                        sim_id, ret, at_set_count, s_sim_items[i].checkFun);
            }
        }
        //4. Update SIM result
        sim->test_done = true;
        if(ret == RET_ESIMS_YES) {
            sprintf(sim->info + strlen(sim->info),
                "%s%d: %s.\n", uistr_info_detect_sim, sim_id, uistr_info_pass);
            SIMLOGD("Detect SIM%d: Pass.",sim_id);
        } else {
            sprintf(sim->info + strlen(sim->info),
                "%s%d: %s!!\n", uistr_info_detect_sim, sim_id, uistr_info_fail);
            SIMLOGD("Detect SIM%d: Fail.",sim_id);
        }
    }

    //5. Close devices
    closeSIMDevice();

    SIMLOGD("Exit");

    return 0;
}
/*---------------------------------------------------------------------------*/
/*Add for supporting SIM detecting by MD3, 2014/11/18 }*/

#endif
