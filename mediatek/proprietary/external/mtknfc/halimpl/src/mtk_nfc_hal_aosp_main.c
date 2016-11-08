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
 *  Yenchih Kuo  2012-08-14
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
#include <semaphore.h>


#include <sys/stat.h>

#include <cutils/log.h> // For Debug
#include <utils/Log.h> // For Debug

#include "mtk_nfc_sys_type.h"
#include "mtk_nfc_sys_type_ext.h"
#include "mtk_nfc_sys.h"

//AOSP
#include <hardware/hardware.h>
#include <hardware/nfc.h>
#include "mtk_nfc_hal_aosp.h"

// LOG Trace
#include "mtk_nfc_log.h"
#define LOG_TAG_T "[SYS]"

#ifdef HALIMPL

static nfc_stack_callback_t* gAOSPHalCallback = NULL;
static nfc_stack_data_callback_t* gAOSPHalDataCallback = NULL;

static sem_t gPreDiscSem;

UINT8 gAospHalThreadExit = FALSE;


#endif

#ifdef HALIMPL
NFCSTATUS android_nfc_aosp_hal_send_msg(unsigned int type, unsigned int len, void *payload)
{
    MTK_NFC_MSG_T *prmsg;
    // malloc msg
    prmsg = (MTK_NFC_MSG_T *)malloc(sizeof(MTK_NFC_MSG_T) + len);
    if (prmsg == NULL)
    {
        NFCE("malloc msg fail\n");
        return FALSE;
    }
    else
    {
        NFCD("android_nfc_aosp_hal_send_msg...(type: %d, len: %d, payload: %p,msg: %p)\n",type, len, payload,prmsg);
        
        // fill type & length
        prmsg->type = type;
        prmsg->length = len;
        if (len > 0) {
            memcpy((UINT8 *)prmsg + sizeof(MTK_NFC_MSG_T), payload, len);
        }

        //send msg to service thread
        if (mtk_nfc_sys_msg_send(MTK_NFC_TASKID_SERVICE,prmsg) != MTK_NFC_SUCCESS)
        {
            NFCE("android_nfc_aosp_hal_send_msg fail\n");
            free(prmsg);
            return MTK_NFC_ERROR;
        }
    }

    return MTK_NFC_SUCCESS;
}

void _MtkNfcHalAosp_EvtCB(nfc_event_t event, UINT32 status)
{
    nfc_status_t event_status;

    if(status == 0)
    {
        event_status = HAL_NFC_STATUS_OK;
    }
    else if(status == 0xFF)
    {
        event_status = HAL_NFC_STATUS_ERR_TRANSPORT;
    }
    else
    {
        event_status = HAL_NFC_STATUS_FAILED;
    }

    NFCD("_MtkNfcHalAosp_EvtCB evet:0x%x, status:%d" ,event, (int)event_status  );
    
    gAOSPHalCallback(event,event_status);
}

void _MtkNfcHalAosp_DataCB(UINT8* Buf, UINT16 len)
{
    NFCD("_MtkNfcHalAosp_DataCB len %d", len  );
    
    gAOSPHalDataCallback(len,Buf);
}


//-------------------------------
NFCSTATUS MtkNfcHalAosp_Open(nfc_stack_callback_t *p_halCB, nfc_stack_data_callback_t *p_hal_dataCB)
{
    NFCSTATUS ret = MTK_NFC_SUCCESS;

    NFCD("MtkNfcHalAosp_Open\n");
    if ((!p_halCB) || (!p_hal_dataCB))
    {
        NFCE("%s Invalid callback function pointers", __FUNCTION__);
        return MTK_NFC_ERROR;
    }

    //register call back funcion
    gAOSPHalCallback = p_halCB;
    gAOSPHalDataCallback = p_hal_dataCB;
    
    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_OEPN_REQ, 0, NULL))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    return ret;
}

NFCSTATUS MtkNfcHalAosp_Close(void)
{
    NFCSTATUS ret = MTK_NFC_SUCCESS;

    NFCD("MtkNfcHalAosp_Close\n");

    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_CLOSE_REQ, 0, NULL))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    return ret;
}

NFCSTATUS MtkNfcHalAosp_Core_inited(void){
    NFCSTATUS ret = MTK_NFC_SUCCESS;

    NFCD("MtkNfcHalAosp_Core_inited\n");

    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_CORE_INITED_REQ, 0, NULL))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    return ret;
}

NFCSTATUS MtkNfcHalAosp_PreDiscover(void)
{
    NFCSTATUS ret = MTK_NFC_SUCCESS;

    NFCD("MtkNfcHalAosp_PreDiscover\n");

    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_PRE_DISCOVERY_REQ, 0, NULL))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    //if(sem_wait(&gPreDiscSem))
    //{
    //    ALOGE("Failed to wait for PreDiscover semaphore (errno=0x%08x)", __FUNCTION__, errno);
        //ret = MTK_NFC_ERROR;
    //}    

    return ret;
}

NFCSTATUS MtkNfcHalAosp_Ctrl_granted(void){
    NFCSTATUS ret = MTK_NFC_SUCCESS;

    NFCD("MtkNfcHalAosp_Ctrl_granted\n");

    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_CTRL_GRANT_REQ, 0, NULL))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    return ret;
}

NFCSTATUS MtkNfcHalAosp_Power_Cycle(void){
    NFCSTATUS ret = MTK_NFC_SUCCESS;

    NFCD("MtkNfcHalAosp_Power_Cycle\n");

    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_PWR_CYCLE_REQ, 0, NULL))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    return ret;
}

NFCSTATUS MtkNfcHalAosp_write(UINT8* pWriteBuf,UINT32 WriteLen)
{
    NFCSTATUS ret = MTK_NFC_SUCCESS;
    mtk_nfc_aosp_hal_write_req_t WriteReq;

    NFCD("MtkNfcHalAosp_write,WriteLen %d\n",WriteLen);
    
    WriteReq.len = WriteLen;
    if(WriteLen > sizeof(WriteReq.data))
    {
        WriteLen = sizeof(WriteReq.data);
    }
    memcpy(WriteReq.data,pWriteBuf,WriteLen);
    

    if ( (ret = android_nfc_aosp_hal_send_msg(MTK_NFC_AOSP_HAL_WRITE_REQ, sizeof(UINT32)+ WriteLen, &WriteReq))  != MTK_NFC_SUCCESS )
    {
        return ret;
    }

    return ret;
}
//-------------------------------

NFCSTATUS MtkNfcHalAosp_Init(void)
{
    NFCSTATUS ret = MTK_NFC_SUCCESS;
#if 1
    if(sem_init(&gPreDiscSem, 0, 0) == -1)
    {
        NFCE(" PreDiscover Semaphore creation failed (errno=0x%x)", errno);
        //ret = MTK_NFC_ERROR;
    }
#endif
    return ret;
}


NFCSTATUS MtkNfcHalAosp_deInit(void)
{
    NFCSTATUS ret = MTK_NFC_SUCCESS;
#if 1
    if (sem_destroy(&gPreDiscSem))
    {
        NFCE("Failed to destroy semaphore (errno=0x%08x)", errno);
    }
#endif
    return ret;
}

//-------------------------------

INT32
mtk_nfc_HalAosp_proc (UINT8 *prmsg)
{
    
    MTK_NFC_MSG_T* pMsg = NULL;
    UINT8* pMsgBody = NULL;
    INT32 ret = MTK_NFC_SUCCESS;


    if (prmsg == NULL)
    {
        NFCE("mtk_nfc_HalAosp_proc, prmsg null\r\n");
        return MTK_NFC_ERROR;
    }

    pMsg = (MTK_NFC_MSG_T*)prmsg;
    pMsgBody = ((uint8_t*)prmsg + sizeof(MTK_NFC_MSG_T));
    
    NFCD("mtk_nfc_HalAosp_proc, proc msg type %d\r\n",pMsg->type);
    
    switch(pMsg->type)
    {

    case MTK_NFC_AOSP_HAL_OEPN_RES:
        {
            NFCD("MTK_NFC_AOSP_HAL_OEPN_RES\r\n");
            mtk_nfc_aosp_hal_oepn_res_t* pOpenRes = (mtk_nfc_aosp_hal_oepn_res_t*)pMsgBody;

            // send CPLT_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB(HAL_NFC_OPEN_CPLT_EVT, pOpenRes->u4Result);

        }
        break;

    case MTK_NFC_AOSP_HAL_CLOSE_RES:
        {
            NFCD("MTK_NFC_AOSP_HAL_CLOSE_RES\r\n");
            mtk_nfc_aosp_hal_close_res_t* pCloseRes = (mtk_nfc_aosp_hal_close_res_t*) pMsgBody;

            // send CPLT_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB(HAL_NFC_CLOSE_CPLT_EVT, pCloseRes->u4Result);

        }
        break;

    case MTK_NFC_AOSP_HAL_WRITE_RES:
        {
            NFCD("MTK_NFC_AOSP_HAL_WRITE_RES\r\n");
            mtk_nfc_aosp_hal_write_res_t* pWriteRes = (mtk_nfc_aosp_hal_write_res_t*) pMsgBody;

            _MtkNfcHalAosp_DataCB(pWriteRes->data,pWriteRes->len);
            
        }
        break;

    case MTK_NFC_AOSP_HAL_CORE_INITED_RES:
        {
            NFCD("MTK_NFC_AOSP_HAL_CORE_INITED_RES\r\n");
            mtk_nfc_halaosp_core_inited_res_t* pCoreInitRes = (mtk_nfc_halaosp_core_inited_res_t*) pMsgBody;

            // send CPLT_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB(HAL_NFC_POST_INIT_CPLT_EVT, pCoreInitRes->u4Result);
        }
        break;

    case MTK_NFC_AOSP_HAL_PRE_DISCOVERY_RES:
        {
            
            mtk_nfc_halaosp_pre_discovery_res_t* pPreDiscoveryRes = (mtk_nfc_halaosp_pre_discovery_res_t*) pMsgBody;

            NFCD("MTK_NFC_AOSP_HAL_PRE_DISCOVERY_RES , result \r\n",pPreDiscoveryRes->u4Result);

            // release semaphore
            //sem_post(&gPreDiscSem);

            // send CPLT_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB(HAL_NFC_PRE_DISCOVER_CPLT_EVT, pPreDiscoveryRes->u4Result);
        }
        break;
        
    case MTK_NFC_AOSP_HAL_CTRL_GRANT_NTF:
        {
            NFCD("MTK_NFC_AOSP_HAL_CTRL_GRANT_NTF\r\n");

            mtk_nfc_halaosp_ctrl_grant_ntf_t* pCtrlGrantNtf = (mtk_nfc_halaosp_ctrl_grant_ntf_t*) pMsgBody;

            _MtkNfcHalAosp_EvtCB(HAL_NFC_REQUEST_CONTROL_EVT, pCtrlGrantNtf->u4Result);
        }
        break;

    case MTK_NFC_AOSP_HAL_CTRL_GRANT_RES:
        {
            NFCD("MTK_NFC_AOSP_HAL_CTRL_GRANT_RES\r\n");
            mtk_nfc_halaosp_ctrl_grant_res_t* pCtrlGrantRes = (mtk_nfc_halaosp_ctrl_grant_res_t*) pMsgBody;

            // send CPLT_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB( HAL_NFC_RELEASE_CONTROL_EVT, pCtrlGrantRes->u4Result);
        }
        break;

    case MTK_NFC_AOSP_HAL_PWR_CYCLE_RES:
        {
            NFCD("MTK_NFC_AOSP_HAL_PWR_CYCLE_RES\r\n");
            mtk_nfc_halaosp_power_cycle_res_t* pPowerCycleRes = (mtk_nfc_halaosp_power_cycle_res_t*) pMsgBody;

            // send CPLT_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB(HAL_NFC_OPEN_CPLT_EVT, pPowerCycleRes->u4Result);
        }
        break;
    case MTK_NFC_EXIT_REQ:
        return MTK_NFC_ERROR;
    default:
        NFCD("mtk_nfc_HalAosp_proc, unknow msg type %d\n",pMsg->type);
            // send ERROR_EVT to AOSP_HAL_CB
            _MtkNfcHalAosp_EvtCB(HAL_NFC_ERROR_EVT, 0xFF);
        break;
    }

   return 0;
}

void mtk_nfc_HalAosp_thread_exit(void)
{
    gAospHalThreadExit = TRUE;
}


void* mtk_nfc_HalAosp_thread(void * arg)
{
    UINT32 ret = MTK_NFC_SUCCESS;
    MTK_NFC_MSG_T *nfc_msg;

    gAospHalThreadExit = FALSE;
    NFCD("%s, Create\n",__FUNCTION__);
    
    while (!gAospHalThreadExit)
    {
        // - recv msg      
        ret = mtk_nfc_sys_msg_recv(MTK_NFC_TASKID_AOSP_HAL, &nfc_msg);
        if (ret == MTK_NFC_SUCCESS && (!gAospHalThreadExit))
        {
            // - proc msg
            if (MTK_NFC_ERROR == mtk_nfc_HalAosp_proc((UINT8*)nfc_msg))
            {
                NFCE("TRIGGER EXIT Hal Aosp thread");
                gAospHalThreadExit = TRUE;
                break;
            }
            
            // - free msg
            mtk_nfc_sys_msg_free(nfc_msg);
        }
        else
        {
            //read msg fail...        
            NFCE("mtk_nfc_HalAosp_thread, read msg fail\n");

            mtk_nfc_sys_sleep(1);
        }    

    }
    
    NFCD("%s, exit\n",__FUNCTION__);
    return NULL;
}




#endif /* #ifdef HALIMPL */

