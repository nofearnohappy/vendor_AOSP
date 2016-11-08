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
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

/*******************************************************************************
 * Filename:
 * ---------
 *  mtk_nfc_android_main.c
 *
 * Project:
 * --------
 *
 * Description:
 * ------------
 *
 * Author:
 * -------
 *  LiangChi Huang, ext 25609, liangchi.huang@mediatek.com, 2012-08-14
 *
 *******************************************************************************/
/*****************************************************************************
 * Include
 *****************************************************************************/
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/fs.h>

#include <termios.h>
#include <pthread.h>

#include <sys/types.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/un.h>
#include <errno.h>   /* Error number definitions */


#include <sys/stat.h>

#include <cutils/log.h> // For Debug
#include <utils/Log.h> // For Debug

#include "mtk_nfc_sys_type.h"
#include "mtk_nfc_sys_type_ext.h"
#include "mtk_nfc_sys.h"
#include "mtk_nfc_log.h"

#ifdef SUPPORT_SHARED_LIBRARY
#include "mtk_nfc_sys_fp.h"
#endif


//yenchih note : build option :  -DPORTING_LAYER , -DHALIMPL
#if((defined HALIMPL)||(defined PORTING_LAYER))
//AOSP

//AOSP
#include <hardware/hardware.h>
#include <hardware/nfc.h>

#include "mtk_nfc_hal_aosp.h"
#endif /* #ifdef HALIMPL */

/*****************************************************************************
 * Define
 *****************************************************************************/
#define C_INVALID_TID  (-1)   /*invalid thread id*/
#define C_INVALID_FD   (-1)   /*invalid file handle*/
#define C_INVALID_SOCKET (-1)
#ifdef SUPPORT_BLOCKING_READ_MECHANISM
#define THREAD_NUM     (4)    /*MAIN/DATA READ/SERVICE/SOCKET THREAD*/
#else
#define THREAD_NUM     (3)    /*MAIN/DATA READ/SERVICE THREAD*/
#endif

//
#define DSP_UART_IN_BUFFER_SIZE (1024)

#define USING_LOCAL_SOCKET
//#define MTKNFC_COMM_SOCK    "/data/mtknfc_server"
#define MTKNFC_COMM_SOCK    "/data/nfc_socket/mtknfc_server"
#define MTKNFC_TEST_MODE_COMM_SOCK    "/data/mtknfc_server"
//#define MTKNFC_COMM_SOCK_EM    "/data/data/com.mediatek.engineermode/mtknfc_server"
//#define MTKNFC_COMM_SOCK_EM    "/data/nfc_socket/mtknfc_server"//"/data/mtknfc_server"


//#define SELF_TEST // for test // MUST DISABLE "SUPPORT_BLOCKING_READ_MECHANISM" in inc/mtk_nfc_sys.h when enabling this define

/*
#ifdef DEBUG_LOG

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "NFC-MW"
#endif

#define NFCD(...)    ALOGD(__VA_ARGS__)
#define NFCW(...)    ALOGW(__VA_ARGS__)
#define NFCE(...)    ALOGE(__VA_ARGS__)
#else
#define NFCD(...)
#define NFCW(...)
#define NFCE(...)
#endif
*/
#ifdef HALIMPL

#define LOG_TAG_T "[AOSP]"

#endif

#define MTK_NFCC_MAX_NFCEE 3

/*****************************************************************************
 * Data Structure
 *****************************************************************************/
typedef enum MTK_NFC_THREAD_NUM
{
   MTK_NFC_THREAD_MAIN = 0,
   MTK_NFC_THREAD_READ,
   MTK_NFC_THREAD_SERVICE,
   MTK_NFC_THREAD_SOCKET,
   MTK_NFC_THREAD_AOSPHAL,
   MTK_NFC_THREAD_END
} MTK_NFC_THREAD_NUM_e;

typedef struct MTK_NFC_THREAD
{
    int                     snd_fd;
    MTK_NFC_THREAD_NUM_e    thread_id;
    pthread_t               thread_handle;
    //void (*thread_body)(void *arg);
    int (*thread_exit)(struct MTK_NFC_THREAD *arg);
    int (*thread_active)(struct MTK_NFC_THREAD *arg);
} MTK_NFC_THREAD_T;


/*****************************************************************************
 * Function Prototype
 *****************************************************************************/
int mtk_nfc_sys_init_android(void);
int mtk_nfc_sys_deinit_android(void);
int mtk_nfc_threads_create(int threadId);
int mtk_nfc_threads_release(void);
void *data_read_thread_func(void * arg);
void *nfc_main_proc_func(void * arg);
#ifdef SELF_TEST
void *nfc_service_thread(void * arg);
#define NFC_SERVER_THREAD_PARAM void* arg

#else
void nfc_service_thread(void );
#define NFC_SERVER_THREAD_PARAM void

#endif
#ifdef SUPPORT_BLOCKING_READ_MECHANISM
void *nfc_socket_thread(void * arg);
#endif
int mtk_nfc_exit_thread_normal(MTK_NFC_THREAD_T *arg);
int mtk_nfc_thread_active_notify(MTK_NFC_THREAD_T *arg);

extern VOID *mtk_nfc_sys_interface_init (const CH* strDevPortName, const INT32 i4Baud);
extern VOID  mtk_nfc_sys_interface_uninit (VOID *pLinkHandle);
extern INT32 mtk_nfc_sys_mutex_deinitialize();


/*****************************************************************************
 * GLobal Variable
 *****************************************************************************/
static MTK_NFC_THREAD_T g_mtk_nfc_thread[MTK_NFC_THREAD_END] = {
{C_INVALID_FD, MTK_NFC_THREAD_MAIN,     C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify},
{C_INVALID_FD, MTK_NFC_THREAD_READ,     C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify},
{C_INVALID_FD, MTK_NFC_THREAD_SERVICE,  C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify},
{C_INVALID_FD, MTK_NFC_THREAD_SOCKET,   C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify},
{C_INVALID_FD, MTK_NFC_THREAD_AOSPHAL,  C_INVALID_TID, mtk_nfc_exit_thread_normal, mtk_nfc_thread_active_notify}

};
#ifdef PORTING_LAYER
nfc_nci_device_t* mHalDeviceContext = NULL;
#endif
// thread exit flag
static volatile int g_ThreadExitReadFunc = FALSE;
static volatile int g_ThreadExitMainProcFunc = FALSE;
static volatile int g_ThreadExitService = FALSE;
#ifdef SUPPORT_BLOCKING_READ_MECHANISM
static volatile int g_ThreadExitSocket = FALSE;
#endif

// physical link handle
int gInterfaceHandle = C_INVALID_FD;

// socket
int gconn_fd_tmp = C_INVALID_SOCKET;

volatile int g_NfcTsetMode = 0;

extern int32_t g_NfcSmallAntennaMode;
extern int32_t g_NfcSmallAntennaFull;


extern int32_t g_NfcSeMap;

/*****************************************************************************
 * Function
 *****************************************************************************/
int _mtk_nfc_sys_thread_exit(MTK_NFC_TASKID_E TaskId)
{
    MTK_NFC_MSG_T *prmsg;
    // malloc msg
    prmsg = (MTK_NFC_MSG_T *)malloc(sizeof(MTK_NFC_MSG_T));
    if (prmsg == NULL)
    {
        NFCE("malloc msg fail\n");
        return FALSE;
    }
    else
    {
        NFCD("_mtk_nfc_sys_thread_exit...(type: %dmsg: %p)\n",MTK_NFC_EXIT_REQ,prmsg);

        // fill type & length
        prmsg->type = MTK_NFC_EXIT_REQ;
        prmsg->length = 0;

        //send msg to service thread
        if (mtk_nfc_sys_msg_send(TaskId,prmsg) != MTK_NFC_SUCCESS)
        //if (mtk_nfc_sys_msg_send(MTK_NFC_TASKID_SERVICE,prmsg) != MTK_NFC_SUCCESS)
        {
            NFCE("_mtk_nfc_sys_thread_exit fail\n");
            free(prmsg);
            return 1;
        }
    }

    return 0;
}

int mtk_nfc_sys_init_android(void)
{
   int result = MTK_NFC_SUCCESS;
   int IsSupportPortingLayer = 0;
   int IsSupportHalImpl = 0;

   #ifdef SUPPORT_SHARED_LIBRARY
   MTK_NFC_SYS_FUNCTION_POINT_T gNfc_sys_fp;

   gNfc_sys_fp.sys_mem_alloc           =    mtk_nfc_sys_mem_alloc;
   gNfc_sys_fp.sys_mem_free            =    mtk_nfc_sys_mem_free;
   gNfc_sys_fp.sys_mutex_create        =    mtk_nfc_sys_mutex_create;
   gNfc_sys_fp.sys_mutex_take          =    mtk_nfc_sys_mutex_take;
   gNfc_sys_fp.sys_mutex_give          =    mtk_nfc_sys_mutex_give;
   gNfc_sys_fp.sys_mutex_destory       =    mtk_nfc_sys_mutex_destory;
   gNfc_sys_fp.sys_msg_alloc           =    mtk_nfc_sys_msg_alloc;
   gNfc_sys_fp.sys_msg_send            =    mtk_nfc_sys_msg_send;
   gNfc_sys_fp.sys_msg_recv            =    mtk_nfc_sys_msg_recv;
   gNfc_sys_fp.sys_msg_free            =    mtk_nfc_sys_msg_free;
   gNfc_sys_fp.sys_dbg_string          =    mtk_nfc_sys_dbg_string;
   gNfc_sys_fp.sys_dbg_trace           =    mtk_nfc_sys_dbg_trace;
   gNfc_sys_fp.sys_dbg_trx_to_file     =    mtk_nfc_sys_dbg_trx_to_file;
   gNfc_sys_fp.sys_timer_create        =    mtk_nfc_sys_timer_create;
   gNfc_sys_fp.sys_timer_start         =    mtk_nfc_sys_timer_start;
   gNfc_sys_fp.sys_timer_stop          =    mtk_nfc_sys_timer_stop;
   gNfc_sys_fp.sys_timer_delete        =    mtk_nfc_sys_timer_delete;
   gNfc_sys_fp.sys_sleep               =    mtk_nfc_sys_sleep;
   gNfc_sys_fp.sys_assert              =    mtk_nfc_sys_assert;
   gNfc_sys_fp.sys_interface_write     =    mtk_nfc_sys_interface_write;
   gNfc_sys_fp.sys_gpio_write          =    mtk_nfc_sys_gpio_write;
   gNfc_sys_fp.sys_gpio_read           =    mtk_nfc_sys_gpio_read;
   gNfc_sys_fp.sys_ioctl_command       =    mtk_nfc_sys_ioctl_command;
   gNfc_sys_fp.sys_ioctl_set_param     =    mtk_nfc_sys_ioctl_set_param;
   gNfc_sys_fp.sys_ioctl_get_param     =    mtk_nfc_sys_ioctl_get_param;

   gNfc_sys_fp.sys_file_open           =    mtk_nfc_sys_file_open;
   gNfc_sys_fp.sys_file_close          =    mtk_nfc_sys_file_close;
   gNfc_sys_fp.sys_file_read           =    mtk_nfc_sys_file_read;
   gNfc_sys_fp.sys_file_seek           =    mtk_nfc_sys_file_seek;
   gNfc_sys_fp.sys_file_tell           =    mtk_nfc_sys_file_tell;
   gNfc_sys_fp.sys_file_rewind         =    mtk_nfc_sys_file_rewind;



#ifdef HALIMPL
   IsSupportHalImpl = 1;
#endif /*#ifdef HALIMPL*/

#ifdef PORTING_LAYER
   IsSupportPortingLayer = 1;
#endif /*#ifdef PORTING_LAYER*/

   NFCD("mtk_nfc_sys_init_android , IsSupportHalImpl %d, IsSupportPortingLayer %d\n",IsSupportHalImpl,IsSupportPortingLayer);

   result =  mtk_nfc_sys_function_registry(&gNfc_sys_fp);

   NFCD("mtk_nfc_sys_function_registry[%d]\n", result);
   #endif

   // Initial global variables
   gInterfaceHandle = C_INVALID_FD;
   g_ThreadExitReadFunc = FALSE;
   g_ThreadExitMainProcFunc = FALSE;
   g_ThreadExitService = FALSE;
#ifdef SUPPORT_BLOCKING_READ_MECHANISM
   g_ThreadExitSocket = FALSE;
#endif

#ifdef SUPPORT_BLOCKING_READ_MECHANISM
   // create event resource
#if 0
   result = mtk_nfc_sys_event_create(MTK_NFC_EVENT_2MAIN);
   if (result != MTK_NFC_SUCCESS) {
      NFCD("mtk_nfc_sys_event_create,MTK_NFC_EVENT_2MAIN ERR\n");
      return (result);
   }
   result = mtk_nfc_sys_event_create(MTK_NFC_EVENT_2SERV);
   if (result != MTK_NFC_SUCCESS) {
      NFCD("mtk_nfc_sys_event_create,MTK_NFC_EVENT_2SERV ERR\n");
      return (result);
   }
#endif
   result = mtk_nfc_sys_event_creates();
   if (result != MTK_NFC_SUCCESS) {
      NFCD("mtk_nfc_sys_event_creates, ERR\n");
      return (result);
   }

#endif

   // Initial system resource - mutex
   result = mtk_nfc_sys_mutex_initialize();
   if (MTK_NFC_SUCCESS != result) {
      NFCD("mtk_nfc_sys_mutex_initialize fail\n");
      return (result);
   }

   // Initial system resource - message queue
   result = mtk_nfc_sys_msg_initialize();
   if (MTK_NFC_SUCCESS != result) {
      NFCD("mtk_nfc_sys_msg_initialize fail\n");
      return (result);
   }

   // Initial physical interface
   result = (int)(intptr_t)mtk_nfc_sys_interface_init(NULL, 0);
   if (result < 0) {
      NFCD("mtk_nfc_sys_interface_init fail, error code %d\n", result);
      return (result);
   }

   ///--enable clock buffer
   if (gInterfaceHandle != -1) //C_INVALID_FD
   {
       NFCD("enable clock buffer. node, %d, \n", gInterfaceHandle);
       mtk_nfc_sys_ioctl_command (MTK_NFC_GPIO_IOCTL, MTK_NFC_IOCTL_CMD_CLOCK_BUF_ENABLE);
   }
   else
   {
       NFCD("i2c device handler is invalid\n");
   }

   // Initialize timer handler
   result = mtk_nfc_sys_timer_init();
   if (result != MTK_NFC_SUCCESS) {
      NFCD("mtk_nfc_sys_timer_init fail, error code %d\n", result);
      return (result);
   }

   // Create Thread
   result = mtk_nfc_threads_create(MTK_NFC_THREAD_READ);
   if (result != MTK_NFC_SUCCESS)
   {
      NFCD("mtk_nfc_threads_create, read thread ERR\n");
      return (result);
   }

   result = mtk_nfc_threads_create(MTK_NFC_THREAD_MAIN);
   if (result != MTK_NFC_SUCCESS)
   {
      NFCD("mtk_nfc_threads_create, main thread ERR\n");
      return (result);
   }

   #if(defined(SELF_TEST)||defined(HALIMPL))
   result = mtk_nfc_threads_create(MTK_NFC_THREAD_SERVICE);
   if (result != MTK_NFC_SUCCESS)
   {
      NFCD("mtk_nfc_threads_create, service thread ERR\n");
      return (result);
   }
   #endif

   #if(defined(SUPPORT_BLOCKING_READ_MECHANISM))
   if(IsSupportPortingLayer)
   {
   result = mtk_nfc_threads_create(MTK_NFC_THREAD_SOCKET);
   if (result != MTK_NFC_SUCCESS)
   {
      NFCD("mtk_nfc_threads_create, socket thread ERR\n");
      return (result);
   }
   }
   #endif

   if(IsSupportHalImpl)
   {
       result = mtk_nfc_threads_create(MTK_NFC_THREAD_AOSPHAL);
       if (result != MTK_NFC_SUCCESS)
       {
          NFCD("mtk_nfc_threads_create, hal aosp thread ERR\n");
          return (result);
       }
   }

   NFCD("mtk_nfc_sys_init_android done,%d\n", result);

   return (result);
}

int mtk_nfc_sys_deinit_android(void)
{
   int result = MTK_NFC_SUCCESS;
   int idx;

   NFCD("mtk_nfc_sys_deinit_android...\n");

   // release thread
   result = mtk_nfc_threads_release();
   if (MTK_NFC_SUCCESS != result)
   {
      NFCD("mtk_nfc_threads_release fail,%d\n", result);
   }

   /// --- disable clock buffer
   if (gInterfaceHandle != -1) //C_INVALID_FD
   {
       NFCD("disable clock buffer. \n");
       mtk_nfc_sys_ioctl_command (MTK_NFC_GPIO_IOCTL, MTK_NFC_IOCTL_CMD_CLOCK_BUF_DISABLE);
   }
   else
   {
       NFCD("i2c device handler is invalid\n");
   }

   // un-initialize physical interface
   mtk_nfc_sys_interface_uninit(NULL);

   // release timer
   // - TBD

   // release message queue
   // - TBD

   // release mutex
   result = mtk_nfc_sys_mutex_deinitialize();
   if (MTK_NFC_SUCCESS != result)
   {
      NFCD("mtk_nfc_sys_mutex_deinitialize fail,%d\n", result);
   }

   #ifdef SUPPORT_BLOCKING_READ_MECHANISM
   // delete event resource to avoid blocking call in main & service thread
#if 0
   result = mtk_nfc_sys_event_delete(MTK_NFC_EVENT_2MAIN);
   NFCD("mtk_nfc_sys_event_delete, MTK_NFC_EVENT_2MAIN,%d\n",result);
   result = mtk_nfc_sys_event_delete(MTK_NFC_EVENT_2SERV);
   NFCD("mtk_nfc_sys_event_delete, MTK_NFC_EVENT_2SERV,%d\n",result);
   #endif
   result = mtk_nfc_sys_event_deletes();
   NFCD("mtk_nfc_sys_event_delete,%d\n",result);

   #endif

   // un-initialize global variables
   // - TBD

   NFCD("mtk_nfc_sys_deinit_android done,%d\n", result);

   return result;
}

int mtk_nfc_threads_create(int threadId)
{
   int result = MTK_NFC_SUCCESS;
   char* NameNfcReadThread = "NfcReadThread";
   char* NameNfcMainThread = "NfcMainThread";
   char* NameNfcServiceThread = "NfcServiceThread";
   char* NameNfcSocketThread = "NfcSocketThread";
   char* NameNfcHalThread = "NfcHalThread";

   if ( threadId >= MTK_NFC_THREAD_END)
   {
      NFCD("mtk_nfc_threads_create fail, invalid threadId, %d\n", threadId);
      result = MTK_NFC_ERROR;
   }

   if(MTK_NFC_THREAD_READ == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle,
                            NULL, data_read_thread_func,
                            (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         #ifdef SUPPORT_BLOCKING_READ_MECHANISM
         g_ThreadExitSocket = TRUE;
         #endif
         NFCD("mtk_nfc_threads_create fail,%d\n", threadId);
         result = MTK_NFC_ERROR;
      }
      else
      {
        pthread_setname_np(g_mtk_nfc_thread[threadId].thread_handle,NameNfcReadThread);
      }
   }

   if(MTK_NFC_THREAD_MAIN == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle,
                            NULL, nfc_main_proc_func,
                            (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         #ifdef SUPPORT_BLOCKING_READ_MECHANISM
         g_ThreadExitSocket = TRUE;
         #endif
         NFCD("mtk_nfc_threads_create fail,%d\n", threadId);
         result = MTK_NFC_ERROR;
      }
      else
      {
        pthread_setname_np(g_mtk_nfc_thread[threadId].thread_handle,NameNfcMainThread);
      }
   }

   #if(defined(SELF_TEST)||defined(HALIMPL))
   if(MTK_NFC_THREAD_SERVICE == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle,
                            NULL, nfc_service_thread,
                            (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         #ifdef SUPPORT_BLOCKING_READ_MECHANISM
         g_ThreadExitSocket = TRUE;
         #endif
         NFCD("mtk_nfc_threads_create fail,%d\n", threadId);
         result = MTK_NFC_ERROR;
      }
      else
      {
        pthread_setname_np(g_mtk_nfc_thread[threadId].thread_handle,NameNfcServiceThread);
      }
   }
   #endif

   #ifdef SUPPORT_BLOCKING_READ_MECHANISM
   if(MTK_NFC_THREAD_SOCKET == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle,
                            NULL, nfc_socket_thread,
                            (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         g_ThreadExitSocket = TRUE;
         NFCD("mtk_nfc_threads_create fail,%d\n", threadId);
         result = MTK_NFC_ERROR;
      }
      else
      {
        pthread_setname_np(g_mtk_nfc_thread[threadId].thread_handle,NameNfcSocketThread);
      }
   }
    #endif

   #ifdef HALIMPL
   if(MTK_NFC_THREAD_AOSPHAL == threadId)
   {
      if (pthread_create(&g_mtk_nfc_thread[threadId].thread_handle,
                            NULL, mtk_nfc_HalAosp_thread,
                            (void*)&g_mtk_nfc_thread[threadId]))
      {
         g_ThreadExitReadFunc = TRUE;
         g_ThreadExitMainProcFunc = TRUE;
         g_ThreadExitService = TRUE;
         g_ThreadExitSocket = TRUE;
         NFCD("mtk_nfc_threads_create fail,%d\n", threadId);
         result = MTK_NFC_ERROR;
      }
      else
      {
        pthread_setname_np(g_mtk_nfc_thread[threadId].thread_handle,NameNfcHalThread);
      }
   }
   #endif

   if(result == MTK_NFC_SUCCESS)
   {
        NFCD("mtk_nfc_threads_create done ,id, %d\n", threadId);
   }


   return (result);
}

int mtk_nfc_threads_release(void)
{
   int result = MTK_NFC_SUCCESS;
   int idx;

   NFCD("mtk_nfc_threads_release...\n");

   for (idx = 0; idx < MTK_NFC_THREAD_END; idx++)
   {
       if (g_mtk_nfc_thread[idx].thread_handle == C_INVALID_TID)
       {
           continue;
       }

       if (!g_mtk_nfc_thread[idx].thread_exit)
       {
           continue;
       }

       // trigger soft irq to exit blocking read
       if (g_mtk_nfc_thread[idx].thread_id == MTK_NFC_THREAD_READ)
       {
           if (gInterfaceHandle != -1) //C_INVALID_FD
           {
               NFCD("trigger soft irq to exit blocking read\n");
               ioctl(gInterfaceHandle, 0xFEFF, 0);
               mtk_nfc_sys_ioctl_command(MTK_NFC_GPIO_IOCTL, MTK_NFC_IOCTL_CMD_EXIT_EINT);
           }
           else
           {
               NFCD("i2c device handler is invalid\n");
           }
       }

       if ((g_mtk_nfc_thread[idx].thread_exit(&g_mtk_nfc_thread[idx])))
       {
           result = MTK_NFC_ERROR;
           NFCD("mtk_nfc_threads_release exit fail,%d\n", idx);
       }
   }

   NFCD("mtk_nfc_threads_release done\n");

   return (result);
}

void *data_read_thread_func(void * arg)
{
    UINT32 ret = MTK_NFC_SUCCESS,i;
    MTK_NFC_THREAD_T *ptr = (MTK_NFC_THREAD_T*)arg;

    if (!arg)
    {
        NFCE("data_read_thread_func, Create ERR !arg\n");
        pthread_exit(NULL);
        return NULL;
    }

    NFCD("[nfc thread]data_read_thread_func, Create\n");

    while (!g_ThreadExitReadFunc)
    {
        INT32 i4ReadLen = 0;
        UINT8 pBuffer[DSP_UART_IN_BUFFER_SIZE];
        INT8 chReadByteOfOnce = 32;

        // blocking read
        i4ReadLen = mtk_nfc_sys_interface_read(pBuffer, chReadByteOfOnce);

        NFCD("READ THREAD,%d \r\n",i4ReadLen);

        if (i4ReadLen > 0)
        {
            ret = mtk_nfc_data_input((const CH*)pBuffer, i4ReadLen);
            if ( ret != MTK_NFC_SUCCESS)
            {
                NFCD("mtkNfcDal_Rx: fail\r\n");
            }
        }
        else
        {
            NFCE("mtk_nfc_sys_interface_read Err\n");

            mtk_nfc_sys_sleep(50);
        }
    }

    NFCD("data_read_thread_func, exit\n");

    g_ThreadExitReadFunc = TRUE;
    pthread_exit((void *)(uintptr_t)ret);

    return NULL;
}

void *nfc_main_proc_func(void * arg)
{
    UINT32 ret = MTK_NFC_SUCCESS;
    MTK_NFC_MSG_T *nfc_msg;
    MTK_NFC_THREAD_T *ptr = (MTK_NFC_THREAD_T*)arg;

    if (!arg)
    {
        NFCD("nfc_main_proc_func, Create ERR !arg\n");
        pthread_exit(NULL);
        return NULL;
    }

    NFCD("[nfc thread]nfc_main_proc_func, Create\n");

    while (!g_ThreadExitMainProcFunc)
    {
        // - recv msg
        ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_MAIN, &nfc_msg);
        if (ret == MTK_NFC_SUCCESS && (!g_ThreadExitMainProcFunc))
        {
            // - proc msg
            mtk_nfc_main_proc(nfc_msg);

            // - free msg
            mtk_nfc_sys_msg_free(nfc_msg);
        }
        else
        {
            //read msg fail...
            NFCD("nfc_main_proc_func, read msg fail\n");

            mtk_nfc_sys_sleep(1); // avoid busy loop
        }
    }

    NFCD("nfc_main_proc_func, exit\n");

    g_ThreadExitMainProcFunc = TRUE;
    pthread_exit((void *)(uintptr_t)ret);

    return NULL;
}

#ifdef SUPPORT_BLOCKING_READ_MECHANISM
void *nfc_socket_thread(void * arg)
{
    UINT32 ret = MTK_NFC_SUCCESS;
    MTK_NFC_MSG_T *nfc_msg;
    MTK_NFC_THREAD_T *ptr = (MTK_NFC_THREAD_T*)arg;

    if (!arg)
    {
        NFCD("nfc_socket_thread, Create ERR !arg\n");
        pthread_exit(NULL);
        return NULL;
    }

    NFCD("[nfc thread]nfc_socket_thread, Create\n");

#ifdef USING_LOCAL_SOCKET
{
    int server_fd = C_INVALID_SOCKET;
    /*socklen_t*/int size;
    struct sockaddr_un server_addr;
    struct sockaddr_un client_addr;

    memset(&server_addr, '\0', sizeof(struct sockaddr_un));

    NFCD("g_NfcTsetMode,%d\r\n",g_NfcTsetMode);

    if(!g_NfcTsetMode) {
       unlink (MTKNFC_COMM_SOCK);
    } else {
       unlink (MTKNFC_TEST_MODE_COMM_SOCK);
    }

    //----------------------------------------------------------------
    // Create a SOCKET for listening for incoming connection requests.
    //----------------------------------------------------------------
    if ((server_fd = socket(AF_LOCAL, SOCK_STREAM, 0)) == -1)
    {
        NFCD("socket error\r\n");
        g_ThreadExitSocket = TRUE;
        pthread_exit(NULL);
    }

    //----------------------------------------------------------------
    // The sockaddr_in structure specifies the address family,
    // IP address, and port for the socket that is being bound.
    //----------------------------------------------------------------
    //Bind and listen
    server_addr.sun_family = AF_LOCAL;

    if(!g_NfcTsetMode) {
       strcpy (server_addr.sun_path, MTKNFC_COMM_SOCK);
    } else {
       strcpy (server_addr.sun_path, MTKNFC_TEST_MODE_COMM_SOCK);
    }

    NFCD("server_fd,%d\r\n",server_fd);
    NFCD("server_addr.sun_path,%s\r\n",server_addr.sun_path);
    NFCD("server_addr.sun_family,%d\r\n",server_addr.sun_family);

    if((bind (server_fd, (struct sockaddr *)&server_addr, sizeof(struct sockaddr_un))) == -1)
    {
        NFCD("bind error\r\n");
        NFCD("fail to get socket from environment: %s\n",strerror(errno));
        close(server_fd);
        g_ThreadExitSocket = TRUE;
        pthread_exit(NULL);
    }

    if(!g_NfcTsetMode)
    {
       int res;
       res = chmod(MTKNFC_COMM_SOCK, S_IRUSR | S_IXUSR | S_IWUSR | S_IRGRP |S_IWGRP | S_IXGRP | S_IXOTH);
       NFCD("chmod1,%d..\r\n",res);
    }
    else
    {
       int res;
       res = chmod(MTKNFC_TEST_MODE_COMM_SOCK, S_IRUSR | S_IXUSR | S_IWUSR | S_IRGRP |S_IWGRP | S_IXGRP | S_IXOTH);
       NFCD("chmod2,%d..\r\n",res);
    }

    //----------------------------------------------------------------
    // Listen for incoming connection requests on the created socket
    //----------------------------------------------------------------
    NFCD("start listen...(server_fd,%d)\r\n", server_fd);
    if(listen (server_fd, 5) == -1)
    {
        NFCD("listent fail: %s\n",strerror(errno));
        close(server_fd);
        g_ThreadExitSocket = TRUE;
        pthread_exit(NULL);
    }

    //----------------------------------------------------------------
    // Waiting for client to connect
    //----------------------------------------------------------------
    NFCD("Waiting for client to connect...\n");
    gconn_fd_tmp = accept(server_fd, (struct sockaddr*)&client_addr, &size);
    NFCD("socket accept,%d\r\n",gconn_fd_tmp);
}
#endif

    while (!g_ThreadExitSocket)
    {
        // - recv msg from socket interface
        ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_SOCKET, &nfc_msg);
        if (ret == MTK_NFC_SUCCESS && (!g_ThreadExitSocket))
        {
            NFCD("nfc_socket_thread, read msg ok (type,%d,len:%d)\n", nfc_msg->type, nfc_msg->length);

            // send msg to service thread internal queue
            mtk_nfc_sys_msg_send(MTK_NFC_TASKID_SERVICE, nfc_msg);
            // 2015/07/06 leo add , EM/FM destroy CMD , close socket thread & nfcstackp exit
            if((nfc_msg->type == MTK_NFC_EM_STOP_CMD) || (nfc_msg->type == MTK_NFC_FM_STOP_CMD))
            {
                NFCD("nfc_socket_thread, MTK_NFC_EM/FM_STOP_CMD exit loop \n");
                g_ThreadExitSocket = TRUE;
            }
        }
        else
        {
            NFCD("nfc_socket_thread, read msg fail,exit socket thread\n");

            //read msg fail...
            g_ThreadExitSocket = TRUE;
            // [Remind] Only for Blocking-read-mechanism.
            // Since nfc_socket_thread is closed , send MTK_NFC_EXIT_REQ message to service.
            // In abnormal case : This mechanism can avoid zombie process(nfcstackp).
            nfc_msg = mtk_nfc_sys_msg_alloc( sizeof(MTK_NFC_MSG_T) );
            nfc_msg->type = MTK_NFC_EXIT_REQ;
            nfc_msg->length = 0;
            NFCD("nfc_socket_thread, send msg to service (MTK_NFC_EXIT_REQ)\n");
            mtk_nfc_sys_msg_send(MTK_NFC_TASKID_SERVICE, nfc_msg);
        }
    }

#ifdef USING_LOCAL_SOCKET
    //----------------------------------------------------------------
    // Close socket
    //----------------------------------------------------------------
    NFCD("gconn_fd_tmp,%02x\r\n",gconn_fd_tmp);
    close(gconn_fd_tmp);
    gconn_fd_tmp = C_INVALID_SOCKET;

    #ifdef USING_LOCAL_SOCKET
    NFCD("g_NfcTsetMode,%d\r\n",g_NfcTsetMode);

    if(!g_NfcTsetMode) {
       unlink (MTKNFC_COMM_SOCK);
    } else {
       unlink (MTKNFC_TEST_MODE_COMM_SOCK);
    }
    g_NfcTsetMode = 0;
    #endif
#endif

    NFCD("nfc_socket_thread, exit\n");

    g_ThreadExitSocket = TRUE;
    pthread_exit((void *)(uintptr_t)ret);

    return NULL;
}
#endif

#ifdef SUPPORT_BLOCKING_READ_MECHANISM
void nfc_service_thread(void)
{
    UINT32 ret = MTK_NFC_SUCCESS;
    MTK_NFC_MSG_T *nfc_msg;

#if 0
    MTK_NFC_THREAD_T *ptr = (MTK_NFC_THREAD_T*)arg;

    if (!arg)
    {
        NFCD("nfc_service_thread, Create ERR !arg\n");
        pthread_exit(NULL);
        return NULL;
    }
#endif

    NFCD("[nfc thread]nfc_service_thread, Create\n");

    while (!g_ThreadExitService)
    {
        // - recv msg
        ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_SERVICE, &nfc_msg);
        if (ret == MTK_NFC_SUCCESS && (!g_ThreadExitService))
        {
            // - proc msg
            if (MTK_NFC_ERROR == mtk_nfc_service_proc((UINT8*)nfc_msg))
            {
                NFCD("TRIGGER EXIT Service thread");
                g_ThreadExitService = TRUE;
                break;
            }

            // - free msg
            mtk_nfc_sys_msg_free(nfc_msg);
        }
        else
        {
            //read msg fail...
            NFCD("nfc_service_thread, read msg fail\n");

            mtk_nfc_sys_sleep(1);
        }

    }

    NFCD("nfc_service_thread, exit\n");
}
#else

void *nfc_service_thread(NFC_SERVER_THREAD_PARAM)
{
   #ifdef USING_LOCAL_SOCKET
   NFCSTATUS result = 0x00;
   int server_fd = C_INVALID_SOCKET, conn_fd = C_INVALID_SOCKET;
   int server_len, client_len;
   /*socklen_t*/int size;
   struct sockaddr_un server_addr;
   struct sockaddr_un client_addr;

   memset(&server_addr, '\0', sizeof(struct sockaddr_un));

   NFCD("[nfc thread]g_NfcTsetMode,%d\r\n",g_NfcTsetMode);

   if(!g_NfcTsetMode)
   {
   unlink (MTKNFC_COMM_SOCK);
   }
   else
   {
      unlink (MTKNFC_TEST_MODE_COMM_SOCK);
   }

   //if((server_fd = socket (AF_UNIX, SOCK_STREAM, 0)) == -1)
   //if((server_fd = socket (AF_LOCAL, SOCK_STREAM, 0)) == -1)
   if((server_fd = socket(AF_LOCAL, SOCK_STREAM, 0)) == -1)
   {
       printf("socket error\r\n");
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }

   server_addr.sun_family = AF_LOCAL;//AF_UNIX;

   if(!g_NfcTsetMode)
   {
   strcpy (server_addr.sun_path, MTKNFC_COMM_SOCK);
   }
   else
   {
      strcpy (server_addr.sun_path, MTKNFC_TEST_MODE_COMM_SOCK);
   }


    //strncpy(server_addr.sun_path, "/mtknfc_server", sizeof(server_addr.sun_path) -1);

   server_len = sizeof (server_addr);

   NFCD("server_fd,%d\r\n",server_fd);
   NFCD("server_addr.sun_path,%s\r\n",server_addr.sun_path);
   NFCD("server_addr.sun_family,%d\r\n",server_addr.sun_family);


   if((bind (server_fd, (struct sockaddr *)&server_addr, sizeof(struct sockaddr_un))) == -1)
   {
       NFCD("bind error\r\n");
       NFCD("fail to get socket from environment: %s\n",strerror(errno));
       close(server_fd);
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }

   if(!g_NfcTsetMode)
   {
      int res;
      res = chmod(MTKNFC_COMM_SOCK, S_IRUSR | S_IXUSR | S_IWUSR | S_IRGRP |S_IWGRP | S_IXGRP | S_IXOTH);
      NFCD("chmod1,%d..\r\n",res);
   }
   else
   {
      int res;
      res = chmod(MTKNFC_TEST_MODE_COMM_SOCK, S_IRUSR | S_IXUSR | S_IWUSR | S_IRGRP |S_IWGRP | S_IXGRP | S_IXOTH);
      NFCD("chmod2,%d..\r\n",res);
   }

   printf("start listen..\r\n");

   NFCD("start listen..\r\n");
   NFCD("server_fd,%d\r\n",server_fd);


   if(listen (server_fd, 5) == -1)
   {
       NFCD("listen error\r\n");
       NFCD("fail listent: %s\n",strerror(errno));
       close(server_fd);
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }

   #else
   NFCSTATUS result = 0x00;
   int server_fd = C_INVALID_SOCKET, conn_fd = C_INVALID_SOCKET, on;
   struct sockaddr_in server_addr;
   struct sockaddr_in client_addr;
   socklen_t size;
   int socket_port = 7500;

   if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == -1)
   {
       printf("socket error\r\n");
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }
   /* Enable address reuse */
   on = 1;
   if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)))
   {
       printf("setsockopt error\r\n");
       close(server_fd);
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }

   server_addr.sin_family = AF_INET;   /*host byte order*/
   server_addr.sin_port = htons(socket_port); /*short, network byte order*/
   server_addr.sin_addr.s_addr = INADDR_ANY; /*automatically fill with my IP*/
   memset(server_addr.sin_zero, 0x00, sizeof(server_addr.sin_zero));

   if (bind(server_fd, (struct sockaddr*)&server_addr, sizeof(server_addr)) == -1)
   {
       printf("bind error\r\n");
       close(server_fd);
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }

   if (listen(server_fd, 5) == -1)
   {
       NFCD("listen error\r\n");
       close(server_fd);
       g_ThreadExitService = TRUE;
       #ifdef SELF_TEST
       pthread_exit(NULL);
       return NULL;
       #endif
   }
   #endif

   NFCD("socket listen success");

   while(!g_ThreadExitService)
   {
      MTK_NFC_MSG_T *nfc_msg;
      size = sizeof(client_addr);

      NFCD("socket waiting accept!!\r\n");

      gconn_fd_tmp = accept(server_fd, (struct sockaddr*)&client_addr, &size);

      if (1) // config Socket read function to non-blocking type
      {
      int x;
      x=fcntl(gconn_fd_tmp,F_GETFL,0);
      fcntl(gconn_fd_tmp,F_SETFL,x | O_NONBLOCK);
      }

      NFCD("socket accept,%x\r\n",gconn_fd_tmp);

      if (gconn_fd_tmp <= 0)
      {
          continue;
      }
      else
      {
         UINT8 buffer[1024];
         int bufsize=1024, readlen;
         int ret;

         while(!g_ThreadExitService)
         {
             readlen = read(gconn_fd_tmp, &buffer[0], bufsize);

             ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_SERVICE, &nfc_msg);

             //NFCD("conn_fd_read, %d\r\n",readlen);

             if(readlen > 0 || ret == MTK_NFC_SUCCESS)
             {
                if(readlen > 0)
                {
                    MTK_NFC_MSG_T *nfc_msg;
                    UINT16 rx_offset;
                    UINT16 pkt_len;

                    NFCD("from,Socket,read,%d\r\n",readlen);

                    // initial
                    rx_offset = 0;

                    while (rx_offset < readlen)
                    {
                        // split nfc msg if needed
                        nfc_msg = (MTK_NFC_MSG_T *)(&buffer[rx_offset]);
                        pkt_len = (nfc_msg->length + sizeof(MTK_NFC_MSG_T));

                        NFCD("rx_offset,%d, pkt_len,%d\r\n",rx_offset, pkt_len);

                        // nfc msg lenght is valid or not
                        if ((rx_offset + pkt_len) > readlen)
                        {
                            NFCD("nfc msg length check fail,%d,%d\r\n", readlen, rx_offset);
                            break;
                        }
                        else
                        {
                            if(mtk_nfc_service_proc(&buffer[rx_offset]) == MTK_NFC_ERROR)
                            {
                                /*EXIT Service thread*/
                                NFCD("TRIGGER EXIT Service thread");
                                g_ThreadExitService = TRUE;
                                break;
                            }

                            // accumulate rx offset to next nfc msg
                            rx_offset += pkt_len;
                        }
                    }
                }

                if (ret == MTK_NFC_SUCCESS)
                {
                    NFCD("from,Msg,ret,%d\r\n",ret);

                    // - proc msg
                    mtk_nfc_service_proc(nfc_msg);

                    // - free msg
                    mtk_nfc_sys_msg_free(nfc_msg);
                }
             }
             else
             {
                mtk_nfc_sys_sleep(20);//sleep 100msec(workaround for deep idle mode)
             }
         }
      }
   }

   close(gconn_fd_tmp);

   gconn_fd_tmp = C_INVALID_SOCKET;
   g_ThreadExitService = TRUE;


   #ifdef USING_LOCAL_SOCKET
   NFCD("g_NfcTsetMode,%d\r\n",g_NfcTsetMode);

   if(!g_NfcTsetMode)
   {
      unlink (MTKNFC_COMM_SOCK);
   }
   else
   {
      unlink (MTKNFC_TEST_MODE_COMM_SOCK);
   }
   g_NfcTsetMode = 0;
   #endif

   NFCD("socket EXIT success");

   #ifdef SELF_TEST
   pthread_exit(NULL);
   #endif

#ifdef SELF_TEST
   return result;
#endif
}
#endif

int mtk_nfc_exit_thread_normal(MTK_NFC_THREAD_T *arg)
{
    int err;
    void *ret;

    NFCD("mtk_nfc_exit_thread_normal (taskid: %d)\n", arg->thread_id);

#if 0 // temp remove below code because main/service thread will be blocked on waiting signal
    if (!arg) {
        return MTK_NFC_ERROR;
    }

    if (arg->thread_id == MTK_NFC_THREAD_MAIN) {
        g_ThreadExitMainProcFunc = TRUE;
    } else if (arg->thread_id == MTK_NFC_THREAD_READ) {
        g_ThreadExitReadFunc = TRUE;
    } else if (arg->thread_id == MTK_NFC_THREAD_SERVICE) {
        g_ThreadExitService = TRUE;
    }
#ifdef SUPPORT_BLOCKING_READ_MECHANISM
    else if (arg->thread_id == MTK_NFC_THREAD_SOCKET) {
        g_ThreadExitSocket = TRUE;
    }
#endif

    err = pthread_join(arg->thread_handle, &ret);
    if (err) {
        NFCD("(%d)ThreadLeaveErr: %d\n", (arg->thread_id), err);
        return err;
    } else {
        NFCD("(%d)ThreadLeaveOK\n", (arg->thread_id));
    }
    arg->thread_handle = C_INVALID_TID;
#else

    if((arg->thread_id == MTK_NFC_THREAD_READ)
    	 #ifdef SUPPORT_BLOCKING_READ_MECHANISM
    	 || (arg->thread_id == MTK_NFC_THREAD_SOCKET)
    	 #endif
    	 )
    {
        if (arg->thread_id == MTK_NFC_THREAD_READ)
       	{
            g_ThreadExitReadFunc = TRUE;
        }
        #ifdef SUPPORT_BLOCKING_READ_MECHANISM
        else if (arg->thread_id == MTK_NFC_THREAD_SOCKET)
        {
            g_ThreadExitSocket = TRUE;
        }
        #endif

        err = pthread_join(arg->thread_handle, &ret);
        if (err) {
            NFCD("(%d)ThreadLeaveErr: %d\n", (arg->thread_id), err);
            return err;
        } else {
            NFCD("(%d)ThreadLeaveOK\n", (arg->thread_id));
        }
        arg->thread_handle = C_INVALID_TID;

    }

#ifdef HALIMPL
    if(arg->thread_id == MTK_NFC_THREAD_SERVICE)
    {
        if(arg->thread_handle != C_INVALID_TID)
        {
            g_ThreadExitService = TRUE;
            _mtk_nfc_sys_thread_exit(MTK_NFC_TASKID_SERVICE);


            err = pthread_join(arg->thread_handle, &ret);
            if (err) {
                NFCD("(%d)ThreadLeaveErr: %d\n", (arg->thread_id), err);
                return err;
            } else {
                NFCD("(%d)ThreadLeaveOK\n", (arg->thread_id));
            }
            arg->thread_handle = C_INVALID_TID;
        }
    }

    if(arg->thread_id == MTK_NFC_THREAD_MAIN)
    {
        if(arg->thread_handle != C_INVALID_TID)
        {
            g_ThreadExitMainProcFunc = TRUE;
            _mtk_nfc_sys_thread_exit(MTK_NFC_TASKID_MAIN);

            err = pthread_join(arg->thread_handle, &ret);
            if (err) {
                NFCD("(%d)ThreadLeaveErr: %d\n", (arg->thread_id), err);
                return err;
            } else {
                NFCD("(%d)ThreadLeaveOK\n", (arg->thread_id));
            }
            arg->thread_handle = C_INVALID_TID;
        }
    }

    if(arg->thread_id == MTK_NFC_THREAD_AOSPHAL)
    {
        if(arg->thread_handle != C_INVALID_TID)
        {
            g_ThreadExitMainProcFunc = TRUE;
            _mtk_nfc_sys_thread_exit(MTK_NFC_TASKID_AOSP_HAL);

            err = pthread_join(arg->thread_handle, &ret);
            if (err) {
                NFCD("(%d)ThreadLeaveErr: %d\n", (arg->thread_id), err);
                return err;
            } else {
                NFCD("(%d)ThreadLeaveOK\n", (arg->thread_id));
            }
            arg->thread_handle = C_INVALID_TID;
        }
    }

#endif
#endif

    return 0;
}

int mtk_nfc_thread_active_notify(MTK_NFC_THREAD_T *arg)
{
    NFCD("mtk_nfc_thread_active_notify,%d\n", arg->thread_id);

    //if (!arg)
    //{
       // MNL_MSG("fatal error: null pointer!!\n");
       // return -1;
    //}
    //if (arg->snd_fd != C_INVALID_FD)
    //{
        //char buf[] = {MNL_CMD_ACTIVE};
        //return mnl_sig_send_cmd(arg->snd_fd, buf, sizeof(buf));
    //}
    return 0;
}

void mtk_nfc_sys_run (void)
{
#ifndef SELF_TEST
    nfc_service_thread();
#else
    {
       //SELF TEST CODE, send message to service thread by socket!!
       {
             //-----------------------------------------------//
             //--------------SOCKET CONNECT------------------//
             //struct sockaddr_in serv_addr;
             struct sockaddr_un serv_addr;
             struct hostent *server;
             int nfc_sockfd = C_INVALID_SOCKET;

             char bug[10]= "TEST CODE";
             int ret=0;

             nfc_sockfd = socket(AF_LOCAL, SOCK_STREAM, 0);
             //nfc_sockfd = socket(AF_INET, SOCK_STREAM, 0);
             if (nfc_sockfd < 0)
             {
                 printf("nfc_open: ERROR opening socket");
                 return (-4);
             }


    serv_addr.sun_family = AF_LOCAL;//AF_UNIX;
    strcpy (serv_addr.sun_path, MTKNFC_COMM_SOCK);
    //len = sizeof (address);

             //bzero((char *) &serv_addr, sizeof(serv_addr));
             //serv_addr.sin_family = AF_INET;

             //serv_addr.sin_addr.s_addr = inet_addr("127.0.0.1");

             //bcopy((const char *)server->h_addr, (char *)&serv_addr.sin_addr.s_addr, server->h_length);

             //serv_addr.sin_port = htons(7500);
             sleep(2);  // sleep 5sec for libmnlp to finish initialization


             printf("connecting...\r\n");

             /* Now connect to the server */
             if (connect(nfc_sockfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0)
             {
                printf("NFC_Open: ERROR connecting");
                return (-6);
             }
             printf("connecting.done\r\n");

             //-----------------------------------------------//
             //--------------TEST CODE FROM HERE------------------//
             if(1)  //
             {
                 int ret,rec_bytes = 0;
                 char tmpbuf[1024], *ptr;

                 s_mtk_nfc_main_msg *p_nfc_main;


                 p_nfc_main = malloc(sizeof(s_mtk_nfc_main_msg));
                 p_nfc_main->msg_type = 201;
                 p_nfc_main->msg_length = 0;


                 ret = write(nfc_sockfd, p_nfc_main, sizeof(s_mtk_nfc_main_msg));

                 printf("write.done,%d\r\n",ret);

                 //Free memory

             while(1)
             {
                rec_bytes = read(nfc_sockfd, &tmpbuf[0], sizeof(tmpbuf));
                printf("rec_bytes,%d\r\n",rec_bytes);
                if (rec_bytes > 0)
                {
                   char *p;
                   s_mtk_nfc_main_msg *p_nfc_main_rec;
                   p_nfc_main_rec = (s_mtk_nfc_main_msg*) tmpbuf;


                   printf("p_nfc_main_rec,%d,%d\r\n",p_nfc_main_rec->msg_type,p_nfc_main_rec->msg_length);

                   break;
                 }
                 else
                 {
                   usleep(1000);//1ms
                 }
             }


                 printf("exit");
                 free(p_nfc_main);
                 p_nfc_main= NULL;



             }
       else
       {
             //ret = write(nfc_sockfd, (const char*)bug, sizeof(bug));
             //printf("GPS_Open: success connecting,ret,%d",ret);
   #if 0
             while(1)
             {
                usleep(1000000);  // sleep 1000 ms
             }
   #endif
       }
          }
    }
#endif
}

int mtk_nfc_small_antenna_check(void)
{
    int result = 0; //not support
    FILE * fp;
    CHAR buf[128];
    CHAR name[30];
    CHAR value[30];
    CHAR *ptr;

    fp = fopen("/system/etc/nfcse.cfg","r");
    if (fp == NULL) {
        NFCD("Can't open nfcse.cfg file. \r\n");
        return result;
    }

    //read file and search "SMALL_ANTENNA_SUPPORT:YES"
    while(fgets(buf, 128, (void *)fp) != NULL) {
        NFCD("%s", buf);

        // bypass comment line
        if (buf[0] == '#') {
            continue;
        }

        // parsing - name
        ptr = strtok(buf, " :#\t\xd\xa");
        if (ptr == NULL) {
            continue;
        }
        strncpy(name, ptr, 30);

        // parsing - value
        ptr = strtok(NULL, " #\t\xd\xa");
        if (ptr == NULL) {
            continue;
        }
        strncpy(value, ptr, 30);

        if(strncmp(name, "SMALL_ANTENNA_SUPPORT", 30) == 0) {
            if (strncmp(value, "YES", 30) == 0) {
                result = 1;
            } else {
                result = 0;
            }
            break;
        }
    }

    fclose(fp);
    return result;
}

int mtk_nfc_small_antenna_full_function(void)
{
    int result = 0; //not support
    FILE * fp;
    CHAR buf[128];
    CHAR name[30];
    CHAR value[30];
    CHAR *ptr;

    fp = fopen("/system/etc/nfc.cfg","r");
    if (fp == NULL) {
        NFCE("Can't open nfc.cfg file. \r\n");
        return result;
    }

    //read file and search "SMALL_ANTENNA_SUPPORT:YES"
    while(fgets(buf, 128, (void *)fp) != NULL) {
        //NFCD("%s", buf);

        // bypass comment line
        if (buf[0] == '#') {
            continue;
        }

        // parsing - name
        ptr = strtok(buf, " :#\t\xd\xa");
        if (ptr == NULL) {
            continue;
        }
        strncpy(name, ptr, 30);

        // parsing - value
        ptr = strtok(NULL, " #\t\xd\xa");
        if (ptr == NULL) {
            continue;
        }
        strncpy(value, ptr, 30);

        if(strncmp(name, "SMALL_ANTENNA_FULL_SUPPORT", 30) == 0) {
            if (strncmp(value, "YES", 30) == 0) {
                result = 1;
            } else {
                result = 0;
            }
            break;
        }
    }

    fclose(fp);
    return result;
}


/**
 * Read SE SIM/SD card form ProjectConfig.mk
 * MTK_NFC_SE_NUM=<num> // bit 0 - SIM1 , bit 1 - SIM2, bit2 - uSD
 * Support
 */
int mtk_nfc_se_map_check(){
	int SEmap=0;
#ifdef MTK_NFC_SE_NUM
	SEmap=atoi(MTK_NFC_SE_NUM);
#else
	SEmap=0; // null MTK_NFC_SE_NUM
#endif
	return SEmap;
}



#ifdef HALIMPL

#define NFC_NCI_MT6605_HARDWARE_MODULE_ID "nfc_nci.mt6605"
//#define NFC_NCI_CONTROLLER "nci"

typedef struct
{
    struct nfc_nci_device nci_device;
    //below declarations are private variables within mtk HAL
    void* data;
}mt6605_dev_t;

static int hal_open (const struct nfc_nci_device *p_dev, nfc_stack_callback_t *p_hal_cback, nfc_stack_data_callback_t *p_hal_data_callback)
{
    int retval = 0;

    NFCD("%s: enter;" ,__FUNCTION__);

    if (MtkNfcHalAosp_Open(p_hal_cback,p_hal_data_callback) != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


static int hal_write (const struct nfc_nci_device *p_dev,
        uint16_t data_len, const uint8_t *p_data)
{
    int retval = 0;

    NFCD("%s: enter;" , __FUNCTION__);

    if (MtkNfcHalAosp_write(p_data,data_len) != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


static int hal_core_initialized (const struct nfc_nci_device *p_dev,
        uint8_t* p_core_init_rsp_params)
{
    int retval = 0;

    NFCD("%s: enter;", __FUNCTION__);

    if (MtkNfcHalAosp_Core_inited() != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


static int hal_pre_discover (const struct nfc_nci_device *p_dev)
{
    int retval = 0;

    NFCD("%s: enter;" , __FUNCTION__);

    if (MtkNfcHalAosp_PreDiscover() != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }
    else
    {
        retval = 1;// to let NCI RF discovery cmd wait until pre discovery done...
        NFCD("Note: hal_pre_discover return 1 to indicate upper layer wait our HAL_NFC_PRE_DISCOVER_CPLT_EVT");
    }

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


static int hal_close (const struct nfc_nci_device *p_dev)
{
    int retval = 0;

    NFCD("%s: enter;" , __FUNCTION__);

    if (MtkNfcHalAosp_Close() != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


static int hal_control_granted (const struct nfc_nci_device *p_dev)
{
    int retval = 0;

    NFCD("%s: enter;" , __FUNCTION__);

    if (MtkNfcHalAosp_Ctrl_granted() != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }

    NFCD("%s: exit %d" ,__FUNCTION__, retval);

    return retval;
}

static int hal_power_cycle (const struct nfc_nci_device *p_dev)
{
    int retval = 0;

    NFCD("%s: enter;" , __FUNCTION__);

    if (MtkNfcHalAosp_Power_Cycle() != MTK_NFC_SUCCESS)
    {
        retval = -EINVAL;
    }

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


static int hal_get_max_nfcee (const struct nfc_nci_device *p_dev, uint8_t* maxNfcee)
{
    int retval = 0;

    NFCD("%s: enter;" , __FUNCTION__);

    *maxNfcee = MTK_NFCC_MAX_NFCEE;

    NFCD("%s: exit %d" , __FUNCTION__, retval);

    return retval;
}


/*************************************
 * Generic device handling.
 *************************************/


/* Close an opened nfc device instance */
static int nfc_close (hw_device_t *dev)
{
    int retval = 0;

    NFCT("%s: enter;" ,__FUNCTION__);

    if (MTK_NFC_SUCCESS != mtk_nfc_sys_deinit_android())
    {
        retval = -EINVAL;
    }
    else
    {
        if(MTK_NFC_SUCCESS != MtkNfcHalAosp_deInit())
        {
            retval = -EINVAL;
        }
    }

    free (dev);


    NFCT("%s: exit %d" ,__FUNCTION__, retval);

    return retval;
}


static int nfc_open (const hw_module_t* module, const char* name, hw_device_t** device)
{

    NFCT("%s: enter;" ,__FUNCTION__);
    int retval = 0; //0 is ok; -1 is error


    int logLevel = mtk_nfc_read_log_cfg_porting();
    NFCT("%s: Set NFC log trace level :%d" ,__FUNCTION__,logLevel);
    mtk_nfc_set_log_level_porting(logLevel);

    if (strcmp (name, NFC_NCI_CONTROLLER) == 0)
    {
        mt6605_dev_t *dev = calloc (1, sizeof(mt6605_dev_t));

        // Common hw_device_t fields
        dev->nci_device.common.tag = HARDWARE_DEVICE_TAG;
        dev->nci_device.common.version = 0x00010000; // [31:16] major, [15:0] minor
        dev->nci_device.common.module = (struct hw_module_t*) module;
        dev->nci_device.common.close = nfc_close;

        // NCI HAL method pointers
        dev->nci_device.open = hal_open;
        dev->nci_device.write = hal_write;
        dev->nci_device.core_initialized = hal_core_initialized;
        dev->nci_device.pre_discover = hal_pre_discover;
        dev->nci_device.close = hal_close;
        dev->nci_device.control_granted = hal_control_granted;
        dev->nci_device.power_cycle = hal_power_cycle;
        //dev->nci_device.get_max_ee = hal_get_max_nfcee;


        // Copy in
        *device = (hw_device_t*) dev;

        if (MTK_NFC_SUCCESS == mtk_nfc_sys_init_android())
        {
            //success..
            if(MTK_NFC_SUCCESS != MtkNfcHalAosp_Init())
            {
                retval = -EINVAL;
            }
        }
        else
        {
            retval = -EINVAL;
        }

    }
    else
    {
        retval = -EINVAL;
    }
    NFCT("%s: exit %d" ,__FUNCTION__, retval);
    return retval;
}


static struct hw_module_methods_t nfc_module_methods =
{
    .open = nfc_open,
};

struct nfc_nci_module_t HAL_MODULE_INFO_SYM =
{
    .common =
    {
        .tag = HARDWARE_MODULE_TAG,
        .module_api_version = 0x0100, // [15:8] major, [7:0] minor (1.0)
        .hal_api_version = 0x00, // 0 is only valid value
        .id = NFC_NCI_MT6605_HARDWARE_MODULE_ID,
        .name = "MT6605 NFC NCI HW HAL",
        .author = "MediaTek Inc",
        .methods = &nfc_module_methods,
    },
};

#endif /* #ifdef HALIMPL */

int main (int argc, char** argv)
{
    int result = MTK_NFC_ERROR;

    NFCT("nfcstackp main\n");
    int logLevel = mtk_nfc_read_log_cfg_porting();
    NFCT("%s: Set NFC log trace level :%d" ,__FUNCTION__,logLevel);
    mtk_nfc_set_log_level_porting(logLevel);

    g_NfcTsetMode = 0;
    if(argc == 2) {
        if(!strcmp(argv[1],"NFC_TEST_MODE")) {
            NFCD("ENTERY_TEST_MODE\r\n");
            g_NfcTsetMode = 1;
        }
#ifdef	PORTING_LAYER
        else if(!strcmp(argv[1],"NFC_AOSP_MODE") || !strcmp(argv[1],"2")  ) {
            char nci_hal_module[64];
            const hw_module_t* hw_module = NULL;

            NFCD("ENTERY_AOSP_MODE\r\n");
            g_NfcTsetMode = 2;
            // set hw module
            strlcpy (nci_hal_module, "nfc_nci.mt6605", sizeof(nci_hal_module));
            NFCD("Load module : %s",nci_hal_module);

            // get hw module
            result = hw_get_module (nci_hal_module, &hw_module);
            if(result != MTK_NFC_SUCCESS){
                NFCD("get_module fail %d \r\n",result);
                return result;
            }

            NFCD("get_module success \r\n");
            // start nfc nci open
                result = nfc_nci_open (hw_module, &mHalDeviceContext);
            if (result != MTK_NFC_SUCCESS){
                NFCD("nfc_nci_open fail !\r\n");
                return result;
                }

            // catch user keyin console
            result = input_message_handle();
            if(result !=MTK_NFC_SUCCESS){
                NFCD("input message handle fail!\r\n");
                return result;
            }
            exit(0);
             //return (MTK_NFC_SUCCESS);
        }
#endif
    }
    //NFCD("LC_TEST_%d\r\n",argc);
    //NFCD("LC_TEST_%s\r\n",argv[0]);
    //NFCD("LC_TEST_%s\r\n",argv[1]);
    NFCD("argc:%d, argv[0]:%s, argv[1]:%s. \r\n",argc, argv[0], argv[1]);
    NFCD("g_NfcTsetMode,%d\r\n",g_NfcTsetMode);  // for FM init

    // check small antenna mode
    g_NfcSmallAntennaMode = mtk_nfc_small_antenna_check();  // card mode only in nfcse.cfg
    g_NfcSmallAntennaFull = mtk_nfc_small_antenna_full_function();  // full function in nfc.cfg
    NFCD("g_NfcSmallAntennaMode,%d\r\n",g_NfcSmallAntennaMode);
    NFCD("g_NfcSmallAntennaFull,%d\r\n",g_NfcSmallAntennaFull);
    g_NfcSeMap = mtk_nfc_se_map_check();
    NFCD("g_NfcSEmap,%d\r\n",g_NfcSeMap);
    result = mtk_nfc_sys_init_android();

    if (MTK_NFC_SUCCESS == result) {
        mtk_nfc_sys_run();
    } else {
        NFCD("mtk_nfc_sys_init_android fail\n");
    }

    mtk_nfc_sys_deinit_android();

    NFCT("nfcstackp exit\n");

    return (MTK_NFC_SUCCESS);
}

