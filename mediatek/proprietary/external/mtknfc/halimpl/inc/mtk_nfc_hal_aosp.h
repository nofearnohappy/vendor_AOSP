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
 *  mtk_nfc_hal_aosp.h
 *
 * Project:
 * --------
 *  NFC
 *
 * Description:
 * ------------
 *  Operation System Abstration Layer Implementation
 *
 * Author:
 * -------
 *  Yenchih Kuo
 * 
 *******************************************************************************/
#ifndef MTK_NFC_HAL_AOSP_H
#define MTK_NFC_HAL_AOSP_H


/***************************************************************************** 
 * Include
 *****************************************************************************/ 
#include "mtk_nfc_sys.h"

/***************************************************************************** 
 * Define
 *****************************************************************************/



/***************************************************************************** 
 * Enum
 *****************************************************************************/



/***************************************************************************** 
 * extern Variable
 *****************************************************************************/



/***************************************************************************** 
 * Function Prototypes
 *****************************************************************************/


void* mtk_nfc_HalAosp_thread(void * arg);

NFCSTATUS MtkNfcHalAosp_Open(nfc_stack_callback_t *p_halCB, nfc_stack_data_callback_t *p_hal_dataCB);
NFCSTATUS MtkNfcHalAosp_Close(void);
NFCSTATUS MtkNfcHalAosp_Core_inited(void);
NFCSTATUS MtkNfcHalAosp_PreDiscover(void);
NFCSTATUS MtkNfcHalAosp_Ctrl_granted(void);
NFCSTATUS MtkNfcHalAosp_Power_Cycle(void);
NFCSTATUS MtkNfcHalAosp_write(UINT8* pWriteBuf,UINT32 WriteLen);

NFCSTATUS MtkNfcHalAosp_Init(void);
NFCSTATUS MtkNfcHalAosp_deInit(void);


NFCSTATUS input_message_handle();







#endif /* MTK_NFC_HAL_AOSP_H */

