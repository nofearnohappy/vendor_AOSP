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
 *  mtk_nfcstackp.c
 *
 * Project:
 * --------
 *  native nfc stack 
 * Description:
 * ------------
 *  test /UT/IT
 *
 * Author:
 * -------
 *  leo.kuo, 2015-06-23
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
#ifdef SUPPORT_SHARED_LIBRARY
#include "mtk_nfc_sys_fp.h"
#endif

#include <hardware/hardware.h>
#include <hardware/nfc.h>
/*****************************************************************************
 * Define
 *****************************************************************************/

//#define SELF_TEST // for test // MUST DISABLE "SUPPORT_BLOCKING_READ_MECHANISM" in inc/mtk_nfc_sys.h when enabling this define
        
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

#define HAL_WRITE_OP_START  1000
#define HAL_WRITE_OP_END    5000

typedef enum
{
    //Core
    MTK_ST_TM_HalWrite_NONE,
    MTK_ST_TM_HalWrite_CORE_RESET,
    MTK_ST_TM_HalWrite_CORE_INIT,
    MTK_ST_TM_HalWrite_SET_CONFIG,
    MTK_ST_TM_HalWrite_GET_CONFIG,
    MTK_ST_TM_HalWrite_CONN_CREATE,
    MTK_ST_TM_HalWrite_CONN_CLOSE,
    //RF management
    MTK_ST_TM_HalWrite_RF_Discovery_Map,
    MTK_ST_TM_HalWrite_SET_LISTEN_MODE_ROUTING,
    MTK_ST_TM_HalWrite_GET_LISTEN_MODE_ROUTING,
    MTK_ST_TM_HalWrite_RF_DISCOVER,
    MTK_ST_TM_HalWrite_RF_DISCOVER_SELECT,
    MTK_ST_TM_HalWrite_RF_DEACTIVATE,
    MTK_ST_TM_HalWrite_RF_T3T_POLLING,
    MTK_ST_TM_HalWrite_RF_PARAMETER_UPDATE,
    
    //NFCEE management
    MTK_ST_TM_HalWrite_NFCEE_DISCOVER,
    MTK_ST_TM_HalWrite_NFCEE_MODE_SET,
    
}MtkNfcStackp_TM_HalWrite_et;

typedef enum
{
    MTK_ST_TM_HAS_IDLE,
    MTK_ST_TM_HAS_WAIT_HAL_RSP,
    
}MtkNfcStackp_TM_halActionState_et;

/*****************************************************************************
 * structure 
 *****************************************************************************/
typedef struct
{
    UINT32 opcode;
    MtkNfcStackp_TM_HalWrite_et Action;
}MtkNfcStackp_TM_HalWrite_Table_st;

/*****************************************************************************
 * Function Prototype
 *****************************************************************************/

/*****************************************************************************
 * GLobal Variable
 *****************************************************************************/
extern nfc_nci_device_t* mHalDeviceContext;
static MtkNfcStackp_TM_halActionState_et gHalActionState;


const MtkNfcStackp_TM_HalWrite_Table_st HalWriteMapping[]=
{
    //Core
    {HAL_WRITE_OP_START +   0,MTK_ST_TM_HalWrite_CORE_RESET},
    {HAL_WRITE_OP_START +   1,  MTK_ST_TM_HalWrite_CORE_INIT},
    {HAL_WRITE_OP_START +   2,  MTK_ST_TM_HalWrite_SET_CONFIG},
    {HAL_WRITE_OP_START +   3,  MTK_ST_TM_HalWrite_GET_CONFIG},
    {HAL_WRITE_OP_START +   4,  MTK_ST_TM_HalWrite_CONN_CREATE},
    {HAL_WRITE_OP_START +   5,  MTK_ST_TM_HalWrite_CONN_CLOSE},
    //RF management
    {HAL_WRITE_OP_START +  100 +  0,  MTK_ST_TM_HalWrite_RF_Discovery_Map},
    {HAL_WRITE_OP_START +  100 +  1,  MTK_ST_TM_HalWrite_SET_LISTEN_MODE_ROUTING},
    {HAL_WRITE_OP_START +  100 +  2,  MTK_ST_TM_HalWrite_GET_LISTEN_MODE_ROUTING},
    {HAL_WRITE_OP_START +  100 +  3,  MTK_ST_TM_HalWrite_RF_DISCOVER},
    {HAL_WRITE_OP_START +  100 +  4,  MTK_ST_TM_HalWrite_RF_DISCOVER_SELECT},
    {HAL_WRITE_OP_START +  100 +  5,  MTK_ST_TM_HalWrite_RF_DEACTIVATE},
    {HAL_WRITE_OP_START +  100 +  6,  MTK_ST_TM_HalWrite_RF_T3T_POLLING},
    {HAL_WRITE_OP_START +  100 +  7,  MTK_ST_TM_HalWrite_RF_PARAMETER_UPDATE},
    //NFCEE management
    {HAL_WRITE_OP_START +  200 +  0,  MTK_ST_TM_HalWrite_NFCEE_DISCOVER},
    {HAL_WRITE_OP_START +  200 +  1,  MTK_ST_TM_HalWrite_NFCEE_MODE_SET},
};

/*****************************************************************************
 * Function
 *****************************************************************************/


 
/*******************************************************************************
** Author : Leo.Kuo
**
** Function:        isNumeric
**
** Arguments:		char
**
** Description:
**
** Returns:
**
*******************************************************************************/
NFCSTATUS isNumeric (const char * s)
{
    if (s == NULL || *s == '\0' || isspace(*s)){
      return MTK_NFC_ERROR;
    }
    else{
      return MTK_NFC_SUCCESS;
    }
}



void nfc_stack_CB(nfc_event_t event, nfc_status_t event_status)
{
    NFCD("nfc_stack_CB event = %d , status %d",(UINT16)event,(UINT16)event_status);
    printf("nfc_stack_CB event = %d , status %d\r\n",(UINT16)event,(UINT16)event_status);
    gHalActionState = MTK_ST_TM_HAS_IDLE;
}

void nfc_stack_data_CB(uint16_t data_len, uint8_t* p_data)
{
    NFCD("nfc_stack_data_CB data_len = %d ",(UINT16)data_len);
    printf("nfc_stack_data_CB data_len = %d\r\n",(UINT16)data_len);
    gHalActionState = MTK_ST_TM_HAS_IDLE;
}


NFCSTATUS MtkNfcStackp_TM_HalWrite_GetAction (UINT32 opcode,UINT8* pCmd,UINT16* pCmdLen)
{
    UINT32 cnt1;
    NFCSTATUS ret = MTK_NFC_SUCCESS;
    MtkNfcStackp_TM_HalWrite_et Action = MTK_ST_TM_HalWrite_NONE;

    *pCmdLen = 0;
    for(cnt1 = 0; cnt1 < sizeof(HalWriteMapping)/sizeof(HalWriteMapping[0]);++cnt1)
    {
        if(HalWriteMapping[cnt1].opcode == opcode)
        {
            Action = HalWriteMapping[cnt1].Action;
            break;
        }
    }

    //NCI Action
    switch(Action)
    {
    ///////////////Core
    case MTK_ST_TM_HalWrite_CORE_RESET:
        {
            UINT8 Cmd[] = {0x20,0x00,0x01,0x00};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_CORE_INIT:
        {
            UINT8 Cmd[] = {0x20,0x01,0x00};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_SET_CONFIG:
        {
            UINT8 Cmd[] = {0x20,0x02,0x05,0x01,0x02,0x02,0x34,0x56};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_GET_CONFIG:
        {
            UINT8 Cmd[] = {0x20,0x03,0x02,0x01,0x02};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_CONN_CREATE:
        {
            UINT8 Cmd[] = {0x20,0x04,0x06,0x03,0x01,0x01,0x02,0x01,0x00};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_CONN_CLOSE:
        {
            UINT8 Cmd[] = {0x20,0x05,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    ///////////////RF management
    case MTK_ST_TM_HalWrite_RF_Discovery_Map:
        {
            UINT8 Cmd[] = {0x21,0x00,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_SET_LISTEN_MODE_ROUTING:
        {
            UINT8 Cmd[] = {0x21,0x01,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_GET_LISTEN_MODE_ROUTING:
        {
            UINT8 Cmd[] = {0x21,0x02,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_RF_DISCOVER:
        {
            UINT8 Cmd[] = {0x21,0x03,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_RF_DISCOVER_SELECT:
        {
            UINT8 Cmd[] = {0x21,0x04,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_RF_DEACTIVATE:
        {
            UINT8 Cmd[] = {0x21,0x06,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_RF_T3T_POLLING:
        {
            UINT8 Cmd[] = {0x21,0x08,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_RF_PARAMETER_UPDATE:
        {
            UINT8 Cmd[] = {0x21,0x0B,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    ///////////////NFCEE management
    case MTK_ST_TM_HalWrite_NFCEE_DISCOVER:
        {
            UINT8 Cmd[] = {0x21,0x0B,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
    case MTK_ST_TM_HalWrite_NFCEE_MODE_SET:
        {
            UINT8 Cmd[] = {0x21,0x0B,0x01,0x01};
            memcpy(pCmd,Cmd,sizeof(Cmd));
            *pCmdLen = sizeof(Cmd);
        }
        break;
        
    default:
        ret = MTK_NFC_ERROR;
        break;
    }


    
    return ret;
}

NFCSTATUS MtkNfcStackp_TM_HalWrite(UINT32 opcode)
{
    NFCSTATUS ret;
    UINT8 Cmd[600];
    UINT16 CmdLen;

    ret = MtkNfcStackp_TM_HalWrite_GetAction(opcode,Cmd,&CmdLen);
    if(ret == MTK_NFC_SUCCESS)
    {
        gHalActionState = MTK_ST_TM_HAS_WAIT_HAL_RSP;
        
        NFCD("[AOSP]MtkNfcStackp_TM_HalWrite ,Cmd = %x %x,CmdLen = %d",Cmd[0],Cmd[1],CmdLen);
        
        mHalDeviceContext->write(mHalDeviceContext, CmdLen, Cmd);
    }
    return ret;
}




/*******************************************************************************
** Author : Leo.Kuo
**
** Function:        NFC_OP_CODE
**
** Arguments:		char* op
**
** Description:		TEST operation
**
** Returns:         return_type
**
*******************************************************************************/
NFCSTATUS NFC_OP_CODE(char* op)
{
    NFCD("%s :%s\r\n","NFC_OP_CODE",op);
	UINT32 ret;
	// only support Number
	if (isNumeric(op) == MTK_NFC_SUCCESS) {
		UINT32 num = atoi(op);


		if((num >= HAL_WRITE_OP_START)&&(num <= HAL_WRITE_OP_END))
		{
		    ret = MtkNfcStackp_TM_HalWrite(num);
		}
		else
		{
    		switch (num) {
    			case 1: {
                    NFCD("Hal OPEN\n");
                    printf("Hal OPEN\n");
                    gHalActionState = MTK_ST_TM_HAS_WAIT_HAL_RSP;
                    mHalDeviceContext->open (mHalDeviceContext, nfc_stack_CB, nfc_stack_data_CB);
    				break;
    				}
    			case 2:{
    				NFCD("Hal CLOSE\n");
    				printf("Hal CLOSE\n");
    				gHalActionState = MTK_ST_TM_HAS_WAIT_HAL_RSP;
    				mHalDeviceContext->close(mHalDeviceContext);
    				break;
    				}
    			case 3:{
    				printf("Hal Power  Cycle\n");
    				gHalActionState = MTK_ST_TM_HAS_WAIT_HAL_RSP;
    				mHalDeviceContext->power_cycle(mHalDeviceContext);
    				//mHalDeviceContext->core_initialized(mHalDeviceContext, p_core_init_rsp_params);
    				break;
    			}
    			case 4:{
    				printf("Hal Core Init\n");
    				gHalActionState = MTK_ST_TM_HAS_WAIT_HAL_RSP;
    				mHalDeviceContext->core_initialized(mHalDeviceContext, NULL);
    				break;
    			}
    			case 5:{
    				printf("Hal Control granted\n");
    				gHalActionState = MTK_ST_TM_HAS_WAIT_HAL_RSP;
    				mHalDeviceContext->control_granted(mHalDeviceContext);
    				break;
    			}
    			default:
    				return MTK_NFC_ERROR;
    			}
        }
	}else{
		return MTK_NFC_ERROR;
		}


	return MTK_NFC_SUCCESS;
}



/*******************************************************************************
** Author : Leo.Kuo
**
** Function:        input_message_handle
**
** Arguments:		void
**
** Description:		user input message handle
**
** Returns:         void
**
*******************************************************************************/
NFCSTATUS input_message_handle()
{
	//keyboard event handle
    NFCD("%s","input_message_handle");
    char input[128];
	char *delim = ",";
	char *pch;
	int i = 1; // loop count
	int result =0;
	int cnt1;
    while(TRUE){
        printf("Enter OP code >>");
        
        if(scanf("%s",input) == EOF)
        {
            //scanf EOF , break it.
            break;
        }
        
        printf("OP:<%s>\n",input);

		// exit loop
		if(!strcmp(input,"q") || !strcmp(input,"Q")) {
			break;
		}

		// parse user key in
        pch=strtok(input,delim);
        while(pch != NULL){
            printf("CMD %d >%s > start !\r\n",i++,pch);
		// start OP Code operation
			result = NFC_OP_CODE(pch);
			if(result != MTK_NFC_SUCCESS){
				NFCD("input_message_handle , not support OP type!\n");
				printf("input_message_handle , not support OP type!");
				break;
			}
            pch=strtok(NULL,delim);
        }
		printf("-----------------------------------------------------\n");

        //Add for sync hal state...
		for(cnt1 =0 ;cnt1 < 5000; ++cnt1)
		{
		    if(gHalActionState == MTK_ST_TM_HAS_IDLE)
		    {
		        break;
		    }
		    mtk_nfc_sys_sleep(1);
		}
		
    }
	printf("STOP Operation !!\n");
	return MTK_NFC_SUCCESS;

}


