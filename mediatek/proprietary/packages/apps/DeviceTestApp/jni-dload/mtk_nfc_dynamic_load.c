/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2012
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONS1D IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

/*******************************************************************************
 * Filename:
 * ---------
 *  mtk_nfc_dynamic_load.c
 *
 * Project:
 * --------
 *
 * Description:
 * ------------
 *
 * Author:
 * -------
 *  LiangChi Huang, ext 25609, liangchi.huang@mediatek.com, 2012-12-17
 *
 *******************************************************************************/
/*****************************************************************************
 * Include
 *****************************************************************************/
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>  /* UNIX standard function definitions */
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <signal.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <sys/ipc.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <sys/un.h>
#include <string.h>

#include <cutils/log.h> // For Debug
#include <utils/Log.h> // For Debug

#include "mtk_nfc_dynamic_load.h"

#define USE_SIGNAL_EVENT_TO_TIMER_CREATE

#define CLOCKID CLOCK_REALTIME
#define SIG SIGRTMIN

#define MTK_NFC_TIMER_INVALID_ID    0xFFFFFFFF

#undef LOG_TAG
#define LOG_TAG "NFC_DYNA_LOAD"
#define DEBUG_LOG


typedef void (*ppCallBck_t)(unsigned int TimerId, void *pContext);

typedef enum
{
    MTK_NFC_TIMER_RESERVE_0 = 0x0,
    MTK_NFC_TIMER_RESERVE_1,
    MTK_NFC_TIMER_RESERVE_2,
    MTK_NFC_TIMER_RESERVE_3,
    MTK_NFC_TIMER_RESERVE_4,
    MTK_NFC_TIMER_MAX_NUM
} MTK_NFC_TIMER_E;

typedef struct
{
    int                is_used;    // 1 = used; 0 = unused
    timer_t             handle;     // system timer handle
    ppCallBck_t         timer_expiry_callback; // timeout callback
    void                *timer_expiry_context; // timeout callback context
    int                is_stopped;    // 1 = stopped; 0 = running
} nfc_timer_table_struct;

static nfc_timer_table_struct nfc_timer_table[MTK_NFC_TIMER_MAX_NUM];

void mtk_nfc_sys_timer_stop (MTK_NFC_TIMER_E timer_slot); // avoid build warning.

void nfc_timer_expiry_hdlr (int sig, siginfo_t *si, void *uc)
{
    int timer_slot;
    timer_t *tidp;
    ppCallBck_t cb_func;
    void *param;

    #ifdef DEBUG_LOG
    ALOGD("[TIMER]Caugh signal %d\n", sig);
    #endif

    tidp = si->si_value.sival_ptr;

    /* Look up timer_slot of this timeout, range = 0 ~ (MTK_NFC_TIMER_MAX_NUM-1) */
    for(timer_slot = 0; timer_slot < MTK_NFC_TIMER_MAX_NUM; timer_slot++)
    {
        if(nfc_timer_table[timer_slot].handle == *tidp)
        {
            break;
        }
    }

    if(timer_slot == MTK_NFC_TIMER_MAX_NUM)    //timer not found in table
    {
        #ifdef DEBUG_LOG
        //ALOGD("[TIMER]timer no found in the table : (handle: 0x%x)\r\n", nfc_timer_table[timer_slot].handle);
        ALOGD("[TIMER]timer no found in the table \r\n");
        #endif
        return;
    }

    //get the cb and param from gps timer pool
    cb_func = nfc_timer_table[timer_slot].timer_expiry_callback;
    param = nfc_timer_table[timer_slot].timer_expiry_context;

    //stop time (windows timer is periodic timer)
    mtk_nfc_sys_timer_stop(timer_slot);

    //execute cb
    (*cb_func)(timer_slot, param);
}



/*****************************************************************************
 * Function
 *  mtk_nfc_sys_timer_init
 * DESCRIPTION
 *  Create a new timer
 * PARAMETERS
 *  NONE
 * RETURNS
 *  a valid timer ID or MTK_NFC_TIMER_INVALID_ID if an error occured
 *****************************************************************************/
int
mtk_nfc_sys_timer_init (
    void
)
{
    int ret;

    struct sigaction sa;

    /* Establish handler for timer signal */
    #ifdef DEBUG_LOG
    ALOGD("Establishing handler for signal %d\n", SIG);
    #endif
    sa.sa_flags = SA_SIGINFO;
    sa.sa_sigaction = nfc_timer_expiry_hdlr;
    sigemptyset(&sa.sa_mask);

    ret = sigaction(SIG, &sa, NULL);
    if (ret == -1) {
        #ifdef DEBUG_LOG
        ALOGD("sigaction fail\r\n");
        #endif
    }

    return ret;
}

/*****************************************************************************
 * Function
 *  mtk_nfc_sys_timer_create
 * DESCRIPTION
 *  Create a new timer
 * PARAMETERS
 *  NONE
 * RETURNS
 *  a valid timer ID or MTK_NFC_TIMER_INVALID_ID if an error occured
 *****************************************************************************/
unsigned int
mtk_nfc_sys_timer_create (
    void
)
{
#ifdef USE_SIGNAL_EVENT_TO_TIMER_CREATE
    int ret;
    unsigned int timer_slot;
#if 0
    sigset_t mask;
#endif
    struct sigevent se;

    /* Look for available time slot */
    for (timer_slot = 0; timer_slot < MTK_NFC_TIMER_MAX_NUM; timer_slot++) {
        if (nfc_timer_table[timer_slot].is_used == 0) {
            break;
        }
    }

    if (timer_slot == MTK_NFC_TIMER_MAX_NUM) {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]no timer slot could be used\r\n");
        #endif
        return MTK_NFC_TIMER_INVALID_ID;
    }

    /* Block timer signal temporarily */
#if 0
    printf("Block signal %d\n", SIG);
    sigemptyset(&mask);
    sigaddset(&mask, SIG);
    if (sigprocmask(SIG_SETMASK, &mask, NULL) == -1) {
        printf("sigprocmask fail\r\n");
        return;
    }
#endif

    /* Create the timer */
    se.sigev_notify = SIGEV_SIGNAL;
    se.sigev_signo = SIG;
    se.sigev_value.sival_ptr = &nfc_timer_table[timer_slot].handle;

    /* Create a POSIX per-process timer */
    if ((ret = timer_create(CLOCKID, &se, &(nfc_timer_table[timer_slot].handle))) == -1)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_create fail, ret:%d, errno:%d, %s\r\n", ret, errno, strerror(errno));
        #endif
        return MTK_NFC_TIMER_INVALID_ID;
    }

    nfc_timer_table[timer_slot].is_used = 1;
    #ifdef DEBUG_LOG
    ALOGD("[TIMER]create,time_slot,%d,handle,0x%x\r\n", timer_slot, nfc_timer_table[timer_slot].handle);
    #endif

    return timer_slot;
#else
    int ret;
    unsigned int timer_slot;
    struct sigevent se;

    se.sigev_notify = SIGEV_THREAD;
    se.sigev_notify_function = nfc_timer_expiry_hdlr;
    se.sigev_notify_attributes = NULL;

    /* Look for available time slot */
    for (timer_slot = 0; timer_slot < MTK_NFC_TIMER_MAX_NUM; timer_slot++)
    {
        if (nfc_timer_table[timer_slot].is_used == 0)
        {
            break;
        }
    }

    if (timer_slot == MTK_NFC_TIMER_MAX_NUM)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]no timer slot could be used\r\n");
        #endif
        return MTK_NFC_TIMER_INVALID_ID;
    }

    se.sigev_value.sival_int = (int) timer_slot;

    /* Create a POSIX per-process timer */
    #ifdef DEBUG_LOG
    ALOGD("handle1:%x\r\n", nfc_timer_table[timer_slot].handle);
    #endif
    if ((ret = timer_create(CLOCK_REALTIME, &se, &(nfc_timer_table[timer_slot].handle))) == -1)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_create fail, ret:%d, errno:%d, %s\r\n", ret, errno, strerror(errno));
        ALOGD("handle2:%x\r\n", nfc_timer_table[timer_slot].handle);
        #endif
        return MTK_NFC_TIMER_INVALID_ID;
    }

    nfc_timer_table[timer_slot].is_used = 1;
    #ifdef DEBUG_LOG
    ALOGD("[TIMER]create,time_slot,%d\r\n", timer_slot);
    #endif

    return timer_slot;
#endif
}

/*****************************************************************************
 * Function
 *  mtk_nfc_sys_timer_start
 * DESCRIPTION
 *  Start a timer
 * PARAMETERS
 *  timer_slot  [IN] a valid timer slot
 *  period      [IN] expiration time in milliseconds
 *  timer_expiry[IN] callback to be called when timer expires
 *  arg         [IN] callback fucntion parameter
 * RETURNS
 *  NONE
 *****************************************************************************/
void
mtk_nfc_sys_timer_start (
    unsigned int      timer_slot,
    unsigned int      period,
    ppCallBck_t timer_expiry,
    void        *arg
)
{
    struct itimerspec its;

    if (timer_slot >= MTK_NFC_TIMER_MAX_NUM)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_slot(%d) exceed max num of nfc timer\r\n", timer_slot);
        #endif
        return;
    }

    if (timer_expiry == NULL)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_expiry_callback == NULL\r\n");
        #endif
        return;
    }

    if (nfc_timer_table[timer_slot].is_used == 0)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_slot(%d) didn't be created\r\n", timer_slot);
        #endif
        return;
    }

    its.it_interval.tv_sec = 0;
    its.it_interval.tv_nsec = 0;
    its.it_value.tv_sec = period / 1000;
    its.it_value.tv_nsec = 1000000 * (period % 1000);
    if ((its.it_value.tv_sec == 0) && (its.it_value.tv_nsec == 0))
    {
        // this would inadvertently stop the timer (TODO: HIKI)
        its.it_value.tv_nsec = 1;
    }

    nfc_timer_table[timer_slot].timer_expiry_callback = timer_expiry;
    nfc_timer_table[timer_slot].timer_expiry_context = arg;
    nfc_timer_table[timer_slot].is_stopped = 0;
    timer_settime(nfc_timer_table[timer_slot].handle, 0, &its, NULL);

    #ifdef DEBUG_LOG
    ALOGD("[TIMER]timer_slot(%d) start, handle(%d)\r\n", timer_slot, nfc_timer_table[timer_slot].handle);
    #endif
}

/*****************************************************************************
 * Function
 *  mtk_nfc_sys_timer_stop
 * DESCRIPTION
 *  Start a timer
 * PARAMETERS
 *  timer_slot    [IN] a valid timer slot
 * RETURNS
 *  NONE
 *****************************************************************************/
void
mtk_nfc_sys_timer_stop (
    MTK_NFC_TIMER_E timer_slot
)
{
    struct itimerspec its = {{0, 0}, {0, 0}};

    if (timer_slot >= MTK_NFC_TIMER_MAX_NUM)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_slot(%d) exceed max num of nfc timer\r\n", timer_slot);
        #endif
        return;
    }

    if (nfc_timer_table[timer_slot].is_used == 0)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_slot(%d) already be deleted\r\n", timer_slot);
        #endif
        return;
    }

    if (nfc_timer_table[timer_slot].is_stopped == 1)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_slot(%d) already be stopped\r\n", timer_slot);
        #endif
        return;
    }

    nfc_timer_table[timer_slot].is_stopped = 1;
    timer_settime(nfc_timer_table[timer_slot].handle, 0, &its, NULL);

    #ifdef DEBUG_LOG
    ALOGD("[TIMER]timer_slot(%d) stop, handle(%d)\r\n", timer_slot, nfc_timer_table[timer_slot].handle);
    #endif
}

/*****************************************************************************
 * Function
 *  mtk_nfc_sys_timer_delete
 * DESCRIPTION
 *  Delete a timer
 * PARAMETERS
 *  timer_slot    [IN] a valid timer slot
 * RETURNS
 *  NONE
 *****************************************************************************/
void
mtk_nfc_sys_timer_delete (
    MTK_NFC_TIMER_E timer_slot
)
{
    if (timer_slot >= MTK_NFC_TIMER_MAX_NUM)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]exceed max num of nfc timer,%d\r\n", timer_slot);
        #endif
        return;
    }

    if (nfc_timer_table[timer_slot].is_used == 0)
    {
        #ifdef DEBUG_LOG
        ALOGD("[TIMER]timer_slot(%d) already be deleted\r\n", timer_slot);
        #endif
        return;
    }

    timer_delete(nfc_timer_table[timer_slot].handle);
    nfc_timer_table[timer_slot].handle = 0;
    nfc_timer_table[timer_slot].timer_expiry_callback = NULL;
    nfc_timer_table[timer_slot].timer_expiry_context = NULL;
    nfc_timer_table[timer_slot].is_used = 0; // clear used flag
    #ifdef DEBUG_LOG
    ALOGD("[TIMER]timer_slot(%d) delete\r\n", timer_slot);
    #endif
}

int handle;
char* DevNode_mt6605 = "/dev/mt6605";
volatile int  mtkNfcQueryChipIdTimeout;


static void
mtkNfcQueryChipIdTimeoutCb (
    uint_t timer_slot,
    void *pContext
)
{
    mtkNfcQueryChipIdTimeout = 0;
    close(handle);
}

MTK_NFC_CHIP_TYPE_E mtk_nfc_get_chip_type (void)
{
    MTK_NFC_CHIP_TYPE_E eChipType = MTK_NFC_CHIP_TYPE_UNKNOW;
    int result;
    char data[32];// ="0x55";
    int len =0;
    unsigned int TimerID;
    int rv;


    //Init Timer
    // Initialize timer handler
    result = mtk_nfc_sys_timer_init();
    if (result < 0)
    {
      #ifdef DEBUG_LOG
      ALOGD("mtk_nfc_sys_timer_init fail, error code %d\n", result);
      #endif
      return (result);
    }
    //{
    //int i;
    //for (i=0;i<100;i++){ usleep(1000);}
    //}

    //ALOGD("NFC_LC_TEST_ADD_DELAY");
    /* TRY NFC*/
    //////////////////////////////////////////////////////////////////
    // (1) open device node
    handle = open(DevNode_mt6605, O_RDWR | O_NOCTTY);
    if (handle < 0)
    {
        #ifdef DEBUG_LOG
        ALOGD("OpenDeviceFail,eChipType,%d",eChipType);
        #endif
        return 	eChipType;
    }
    // (3) mask IRQ by IOCTL
    ioctl(handle,0xFE00,0);
    // (4) re-registration IRQ handle by IOCTL
    ioctl(handle,0xFE01,0);

    //
    ioctl(handle,0x01, ((0x00 << 8) | (0x01)));
    ioctl(handle,0x01, ((0x01 << 8) | (0x00)));
    {
    int i;
    for (i=0;i<10;i++){ usleep(1000);}
    }

    //
    ioctl(handle,0x01, ((0x00 << 8) | (0x00)));
    ioctl(handle,0x01, ((0x01 << 8) | (0x00)));
    {
    int i;
    for (i=0;i<10;i++){ usleep(1000);}
    }
    ioctl(handle,0x01, ((0x01 << 8) | (0x01)));
    //
    {
    int i;
    for (i=0;i<100;i++){ usleep(1000);}
    }

    data[0] = 0x55;
    len = 1;
    if (write(handle, &data[0], len) < 0)
    {
        #ifdef DEBUG_LOG
        ALOGD("MTK_NFC_Write_Err");
        #endif
        return 	eChipType;
    }
    // (6) Read 0x55
    // Set Timeout
    mtkNfcQueryChipIdTimeout = 1;
    TimerID = mtk_nfc_sys_timer_create();
    // Seart Timer and set time-out is 3 sec
    mtk_nfc_sys_timer_start(TimerID, 3000, &mtkNfcQueryChipIdTimeoutCb, NULL);
    len = 32;

    rv = read(handle, &data[0], len);
    if (rv < 0)
    {
        #ifdef DEBUG_LOG
        ALOGD("MTK_NFC_Write_Err");
        #endif
        return 	eChipType;
    }

    if (mtkNfcQueryChipIdTimeout == 1)
    {
        mtk_nfc_sys_timer_stop(TimerID);

        if ((data[0] == 0xA5) /*&& (data[1] == 0x05) && (data[2] == 0x00) && (data[3] == 0x01)*/)
        {
            eChipType = MTK_NFC_CHIP_TYPE_MT6605;
        }

        close(handle);
    }
    mtk_nfc_sys_timer_delete(TimerID);
    //////////////////////////////////////////////////////////////////
    /* TRY NFC END*/

    #ifdef DEBUG_LOG
    ALOGD("eChipType,%d",eChipType);
    #endif

    return 	eChipType;
}

int verify_checksum(unsigned char *buf, unsigned short length )
{
    int i = 0;
    unsigned short sum1 = 0;
    unsigned short sum2 = 0;

    for( i=0; i< length; i++ ) {
        sum1 += *( buf + i );
    }

    sum2 = *(buf + length + 1 );
    sum2 <<= 8;
    sum2 += *(buf + length);

    if (sum1 == sum2) return 1;

    return 0;
}

