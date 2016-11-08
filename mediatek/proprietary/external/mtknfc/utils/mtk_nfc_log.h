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
 *  mtk_nfc_log.h
 *
 * Project:
 * --------
 *
 * Description:
 * ------------
 *
 * Author:
 * -------
 *  Leo kuo, ext 32813, leo.kuo@mediatek.com, 2015-11-20
 *
 *******************************************************************************/
#ifndef MTK_NFC_LOG_H
#define MTK_NFC_LOG_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef _MSC_VER // cross-platform
#define snprintf _snprintf
#endif

/*****************************************************************************
 * Include
 *****************************************************************************/
#include <stdio.h>
#include <stdarg.h>
#include <string.h>

#include "mtk_nfc_sys_type.h"
#include "mtk_nfc_sys.h"

/*****************************************************************************
 * Define
 *****************************************************************************/
    // - NFC Debug Macro
#define MAX_DEBUG_BUFFER        256//(256*4)
#define MAX_TRACE_BUFFER        256//(256*4)
#define MAX_TRACE_BYTE_PER_LINE (32)

#define LOG_TAG "NFC-MW"
#define REDUCE_DEBUG_MSG_FOR_ANDROID
#define LOG_TAG_T ""
#define NFC_LOGI(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_INF, __VA_ARGS__)
#define NFC_LOGT(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_TRACE, __VA_ARGS__)
#define NFC_LOGW(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_WRN, __VA_ARGS__)
#define NFC_LOGE(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_ERR, __VA_ARGS__)


#define NFC_LOGBI(msg,...) NFC_PRINT_BUFFER_LEVEL_PORTING(LOG_TAG_T#msg,DBG_TYP_INF, __VA_ARGS__) /*  header , buf , len*/
#define NFC_LOGBT(msg,...) NFC_PRINT_BUFFER_LEVEL_PORTING(LOG_TAG_T#msg,DBG_TYP_TRACE, __VA_ARGS__) /*  header , buf , len*/
#define NFC_LOGBW(msg,...) NFC_PRINT_BUFFER_LEVEL_PORTING(LOG_TAG_T#msg,DBG_TYP_WRN, __VA_ARGS__) /*  header , buf , len*/
#define NFC_LOGBE(msg,...) NFC_PRINT_BUFFER_LEVEL_PORTING(LOG_TAG_T#msg,DBG_TYP_ERR, __VA_ARGS__) /*  header , buf , len*/

#define NFC_PRINT(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_INF, __VA_ARGS__)
#define NFC_PRINT_BUFFER(msg,...) NFC_PRINT_BUFFER_LEVEL_PORTING(LOG_TAG_T#msg,DBG_TYP_INF, __VA_ARGS__) /*  header , buf , len*/

#define NFCD(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_INF,__VA_ARGS__)
#define NFCT(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_TRACE,__VA_ARGS__)
#define NFCW(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_WRN,__VA_ARGS__)
#define NFCE(...) NFC_MPRINT_LEVEL_PORTING(LOG_TAG_T,DBG_TYP_ERR,__VA_ARGS__)


/*****************************************************************************
 * Enum
 *****************************************************************************/
     typedef enum
    {
      DBG_MOD_NON = 0,
      DBG_MOD_SYS,
      DBG_MOD_HAL,
      DBG_MOD_NCI,
      DBG_MOD_SHDLC,
      DBG_MOD_BIN,
      DBG_MOD_DAL,
      DBG_MOD_LIB,
      DBG_MOD_P2P,
      DBG_MOD_BOOT,
      DBG_MOD_READER,
      DBG_MOD_AOSP,
      DBG_MOD_AOSP_MW,
      DBG_MOD_AOSP_LIB,
      DBG_MOD_AOSP_HAL,
      DBG_MOD_TIMER,
      DBG_MOD_END
    } mtkNfc_eDbgMod_t;

    typedef enum
    {
      DBG_TYP_NON = 0,
      DBG_TYP_ERR,
      DBG_TYP_WRN,
      DBG_TYP_TRACE,
      DBG_TYP_INF,
      DBG_TYP_END
    } mtkNfc_eDbgTyp_t;


/*****************************************************************************
 * Data Structure
 *****************************************************************************/

/*****************************************************************************
 * Extern Area
 *****************************************************************************/

/*****************************************************************************
 * Function Prototypes
 *****************************************************************************/
// debug string with argument
void _NFC_PRINT(char *str, ...);

void NFC_MPRINT(char* module ,char *str, ...);

void NFC_MPRINT_LEVEL_PORTING(char* module ,mtkNfc_eDbgTyp_t eDbgTyp,char *str, ...);


void _NFC_PRINT_BUFFER(char *msg, uint8_t *buf, uint16_t len);

void NFC_PRINT_BUFFER_LEVEL_PORTING(char *msg, mtkNfc_eDbgTyp_t eDbgTyp, uint8_t *buf, uint16_t len);

void
NFC_LOGD_EXT(mtkNfc_eDbgMod_t eDbgMod, mtkNfc_eDbgTyp_t eDbgTyp, char *data, ...);

void mtk_nfc_set_log_level_porting(UINT8 level);
UINT8 mtk_nfc_read_log_cfg_porting();


// ---------------------------------------------------------------------------
//  NFC debug log
// ---------------------------------------------------------------------------
#define NFC_LOGD(module, type, ...)                     \
{                                                       \
    NFC_LOGD_EXT(module, type, __VA_ARGS__);            \
}






#ifdef __cplusplus
   }  /* extern "C" */
#endif

#endif


