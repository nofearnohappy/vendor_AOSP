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
#include <time.h>
#include <fcntl.h>

/* use nvram */
#include "CFG_BT_File.h"
#include "CFG_BT_Default.h"
#include "CFG_file_lid.h"
#include "libnvram.h"

#include "bt_kal.h"

/**************************************************************************
 *                       D E F I N I T I O N S                            *
***************************************************************************/

typedef union {
  ap_nvram_btradio_struct fields;
  unsigned char raw[sizeof(ap_nvram_btradio_struct)];
} BT_NVRAM_DATA_T;

typedef ENUM_BT_STATUS_T (*HCI_CMD_FUNC_T)(VOID);
typedef struct {
  HCI_CMD_FUNC_T command_func;
} HCI_SEQ_T;

typedef struct {
  UINT32 chip_id;
  BT_NVRAM_DATA_T bt_nvram;
  HCI_SEQ_T *cur_script;
} BT_INIT_VAR_T;

/**************************************************************************
 *                  G L O B A L   V A R I A B L E S                       *
***************************************************************************/

static HCI_CMD_T hciCmd;
static BT_INIT_VAR_T btinit[1];
static INT32  bt_com_port;

/**************************************************************************
 *              F U N C T I O N   D E C L A R A T I O N S                 *
***************************************************************************/

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Local_BD_Addr(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_LinkKeyType(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_UnitKey(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Encryption(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_PinCodeType(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Voice(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_PCM(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Radio(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_TX_Power_Offset(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Sleep_Timeout(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Coex_Performance_Adjust(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_BT_FTR(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_OSC_Info(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_LPO_Info(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_PTA(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_BLEPTA(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Internal_PTA_1(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Internal_PTA_2(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_RF_Reg_100(VOID);
static ENUM_BT_STATUS_T GORMcmd_HCC_RESET(VOID);


static BOOL BT_Get_Local_BD_Addr(UCHAR *);
static VOID GetRandomValue(UCHAR *);
static BOOL WriteBDAddrToNvram(UCHAR *);


//===================================================================
// Combo chip
#ifdef MTK_MT6628
HCI_SEQ_T bt_init_script_6628[] =
{
    {  GORMcmd_HCC_Set_Local_BD_Addr       }, /*0xFC1A*/
    {  GORMcmd_HCC_Set_LinkKeyType         }, /*0xFC1B*/
    {  GORMcmd_HCC_Set_UnitKey             }, /*0xFC75*/
    {  GORMcmd_HCC_Set_Encryption          }, /*0xFC76*/
    {  GORMcmd_HCC_Set_PinCodeType         }, /*0x0C0A*/
    {  GORMcmd_HCC_Set_Voice               }, /*0x0C26*/
    {  GORMcmd_HCC_Set_PCM                 }, /*0xFC72*/
    {  GORMcmd_HCC_Set_Radio               }, /*0xFC79*/
    {  GORMcmd_HCC_Set_TX_Power_Offset     }, /*0xFC93*/
    {  GORMcmd_HCC_Set_Sleep_Timeout       }, /*0xFC7A*/
    {  GORMcmd_HCC_Set_BT_FTR              }, /*0xFC7D*/
    {  GORMcmd_HCC_Set_OSC_Info            }, /*0xFC7B*/
    {  GORMcmd_HCC_Set_LPO_Info            }, /*0xFC7C*/
    {  GORMcmd_HCC_Set_PTA                 }, /*0xFC74*/
    {  GORMcmd_HCC_Set_BLEPTA              }, /*0xFCFC*/
    {  GORMcmd_HCC_RESET                   }, /*0x0C03*/
    {  GORMcmd_HCC_Set_Internal_PTA_1      }, /*0xFCFB*/
    {  GORMcmd_HCC_Set_Internal_PTA_2      }, /*0xFCFB*/
    {  GORMcmd_HCC_Set_RF_Reg_100          }, /*0xFCB0*/
    {  0  },
};
#endif

#ifdef MTK_MT6630
HCI_SEQ_T bt_init_script_6630[] =
{
    {  GORMcmd_HCC_Set_Local_BD_Addr       }, /*0xFC1A*/
    {  GORMcmd_HCC_Set_PCM                 }, /*0xFC72*/
    {  GORMcmd_HCC_Set_Radio               }, /*0xFC79*/
    {  GORMcmd_HCC_Set_TX_Power_Offset     }, /*0xFC93*/
    {  GORMcmd_HCC_Set_Sleep_Timeout       }, /*0xFC7A*/
    {  GORMcmd_HCC_Coex_Performance_Adjust }, /*0xFC22*/
    {  0  },
};
#endif

#if defined(MTK_CONSYS_MT6582) || \
    defined(MTK_CONSYS_MT6592) || \
    defined(MTK_CONSYS_MT6752) || \
    defined(MTK_CONSYS_MT6735) || \
    defined(MTK_CONSYS_MT6580) || \
    defined(MTK_CONSYS_MT6755)
HCI_SEQ_T bt_init_script_consys[] =
{
    {  GORMcmd_HCC_Set_Local_BD_Addr       }, /*0xFC1A*/
    {  GORMcmd_HCC_Set_Radio               }, /*0xFC79*/
    {  GORMcmd_HCC_Set_TX_Power_Offset     }, /*0xFC93*/
    {  GORMcmd_HCC_Set_Sleep_Timeout       }, /*0xFC7A*/
    {  GORMcmd_HCC_RESET                   }, /*0x0C03*/
    {  0  },
};
#endif

/**************************************************************************
 *                          F U N C T I O N S                             *
***************************************************************************/

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Local_BD_Addr(VOID)
{
    UCHAR ucDefaultAddr[6] = {0};
    UCHAR ucZeroAddr[6] = {0};

    hciCmd.opCode = 0xFC1A;
    hciCmd.len = 6;

    LOG_DBG("GORMcmd_HCC_Set_Local_BD_Addr\n");

    switch (btinit->chip_id) {
      case 0x6628:
        memcpy(ucDefaultAddr, stBtDefault_6628.addr, 6);
        break;
      case 0x6630:
        memcpy(ucDefaultAddr, stBtDefault_6630.addr, 6);
        break;
      case 0x6582:
        memcpy(ucDefaultAddr, stBtDefault_6582.addr, 6);
        break;
      case 0x6592:
        memcpy(ucDefaultAddr, stBtDefault_6592.addr, 6);
        break;
      case 0x6752:
        memcpy(ucDefaultAddr, stBtDefault_6752.addr, 6);
        break;
      case 0x0321:
        memcpy(ucDefaultAddr, stBtDefault_6735.addr, 6);
        break;
      case 0x0335:
        memcpy(ucDefaultAddr, stBtDefault_6735m.addr, 6);
        break;
      case 0x0337:
        memcpy(ucDefaultAddr, stBtDefault_6753.addr, 6);
        break;
      case 0x6580:
        memcpy(ucDefaultAddr, stBtDefault_6580.addr, 6);
        break;
      case 0x6755:
        memcpy(ucDefaultAddr, stBtDefault_6755.addr, 6);
        break;
      default:
        LOG_ERR("Unknown combo chip id\n");
        break;
    }

    if ((0 == memcmp(btinit->bt_nvram.fields.addr, ucDefaultAddr, 6)) ||
        (0 == memcmp(btinit->bt_nvram.fields.addr, ucZeroAddr, 6))) {
        LOG_WAN("NVRAM BD address default value\n");
        /* Want to retrieve module eFUSE address on combo chip */
        BT_Get_Local_BD_Addr(btinit->bt_nvram.fields.addr);

        if ((0 == memcmp(btinit->bt_nvram.fields.addr, ucDefaultAddr, 6)) ||
            (0 == memcmp(btinit->bt_nvram.fields.addr, ucZeroAddr, 6))) {
            LOG_WAN("eFUSE address default value\n");
            #ifdef BD_ADDR_AUTOGEN
            GetRandomValue(btinit->bt_nvram.fields.addr);
            #endif
        }
        else {
            LOG_WAN("eFUSE address has valid value\n");
        }

        /* Save BD address to NVRAM */
        WriteBDAddrToNvram(btinit->bt_nvram.fields.addr);
    }
    else {
        LOG_WAN("NVRAM BD address has valid value\n");
    }

    hciCmd.parms[0] = btinit->bt_nvram.fields.addr[5];
    hciCmd.parms[1] = btinit->bt_nvram.fields.addr[4];
    hciCmd.parms[2] = btinit->bt_nvram.fields.addr[3];
    hciCmd.parms[3] = btinit->bt_nvram.fields.addr[2];
    hciCmd.parms[4] = btinit->bt_nvram.fields.addr[1];
    hciCmd.parms[5] = btinit->bt_nvram.fields.addr[0];

    LOG_WAN("Write BD address: %02x-%02x-%02x-%02x-%02x-%02x\n",
            hciCmd.parms[5], hciCmd.parms[4], hciCmd.parms[3],
            hciCmd.parms[2], hciCmd.parms[1], hciCmd.parms[0]);


    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

#ifdef MTK_MT6628
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_LinkKeyType(VOID)
{
    hciCmd.opCode = 0xFC1B;
    hciCmd.len = 1;
    hciCmd.parms[0] = 0x01; /* 00: Unit key; 01: Combination key */

    LOG_DBG("GORMcmd_HCC_Set_LinkKeyType\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_UnitKey(VOID)
{
    hciCmd.opCode = 0xFC75;
    hciCmd.len = 16;

    hciCmd.parms[0] = 0x00;
    hciCmd.parms[1] = 0x00;
    hciCmd.parms[2] = 0x00;
    hciCmd.parms[3] = 0x00;
    hciCmd.parms[4] = 0x00;
    hciCmd.parms[5] = 0x00;
    hciCmd.parms[6] = 0x00;
    hciCmd.parms[7] = 0x00;
    hciCmd.parms[8] = 0x00;
    hciCmd.parms[9] = 0x00;
    hciCmd.parms[10] = 0x00;
    hciCmd.parms[11] = 0x00;
    hciCmd.parms[12] = 0x00;
    hciCmd.parms[13] = 0x00;
    hciCmd.parms[14] = 0x00;
    hciCmd.parms[15] = 0x00;

    LOG_DBG("GORMcmd_HCC_Set_UnitKey\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Encryption(VOID)
{
    hciCmd.opCode = 0xFC76;
    hciCmd.len = 3;

    hciCmd.parms[0] = 0x00;
    hciCmd.parms[1] = 0x02;
    hciCmd.parms[2] = 0x10;

    LOG_DBG("GORMcmd_HCC_Set_Encryption\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_PinCodeType(VOID)
{
    hciCmd.opCode = 0x0C0A;
    hciCmd.len = 1;
    hciCmd.parms[0] = 0x00; /* 00: Variable PIN; 01: Fixed PIN */

    LOG_DBG("GORMcmd_HCC_Set_PinCodeType\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Voice(VOID)
{
    hciCmd.opCode = 0x0C26;
    hciCmd.len = 2;

    hciCmd.parms[0] = btinit->bt_nvram.fields.Voice[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.Voice[1];

    LOG_DBG("GORMcmd_HCC_Set_Voice\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}
#endif

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_PCM(VOID)
{
    hciCmd.opCode = 0xFC72;
    hciCmd.len = 4;

    hciCmd.parms[0] = btinit->bt_nvram.fields.Codec[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.Codec[1];
    hciCmd.parms[2] = btinit->bt_nvram.fields.Codec[2];
    hciCmd.parms[3] = btinit->bt_nvram.fields.Codec[3];

    LOG_DBG("GORMcmd_HCC_Set_PCM\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Radio(VOID)
{
    hciCmd.opCode = 0xFC79;
    hciCmd.len = 6;

    hciCmd.parms[0] = btinit->bt_nvram.fields.Radio[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.Radio[1];
    hciCmd.parms[2] = btinit->bt_nvram.fields.Radio[2];
    hciCmd.parms[3] = btinit->bt_nvram.fields.Radio[3];
    hciCmd.parms[4] = btinit->bt_nvram.fields.Radio[4];
    hciCmd.parms[5] = btinit->bt_nvram.fields.Radio[5];

    LOG_DBG("GORMcmd_HCC_Set_Radio\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_TX_Power_Offset(VOID)
{
    hciCmd.opCode = 0xFC93;
    hciCmd.len = 3;

    hciCmd.parms[0] = btinit->bt_nvram.fields.TxPWOffset[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.TxPWOffset[1];
    hciCmd.parms[2] = btinit->bt_nvram.fields.TxPWOffset[2];

    LOG_DBG("GORMcmd_HCC_Set_TX_Power_Offset\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Sleep_Timeout(VOID)
{
    hciCmd.opCode = 0xFC7A;
    hciCmd.len = 7;

    hciCmd.parms[0] = btinit->bt_nvram.fields.Sleep[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.Sleep[1];
    hciCmd.parms[2] = btinit->bt_nvram.fields.Sleep[2];
    hciCmd.parms[3] = btinit->bt_nvram.fields.Sleep[3];
    hciCmd.parms[4] = btinit->bt_nvram.fields.Sleep[4];
    hciCmd.parms[5] = btinit->bt_nvram.fields.Sleep[5];
    hciCmd.parms[6] = btinit->bt_nvram.fields.Sleep[6];

    LOG_DBG("GORMcmd_HCC_Set_Sleep_Timeout\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

#ifdef MTK_MT6630
static ENUM_BT_STATUS_T GORMcmd_HCC_Coex_Performance_Adjust(VOID)
{
    hciCmd.opCode = 0xFC22;
    hciCmd.len = 6;

    hciCmd.parms[0] = btinit->bt_nvram.fields.CoexAdjust[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.CoexAdjust[1];
    hciCmd.parms[2] = btinit->bt_nvram.fields.CoexAdjust[2];
    hciCmd.parms[3] = btinit->bt_nvram.fields.CoexAdjust[3];
    hciCmd.parms[4] = btinit->bt_nvram.fields.CoexAdjust[4];
    hciCmd.parms[5] = btinit->bt_nvram.fields.CoexAdjust[5];

    LOG_DBG("GORMcmd_HCC_Coex_Performance_Adjust\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else{
        return BT_STATUS_FAILED;
    }
}
#endif

#ifdef MTK_MT6628
static ENUM_BT_STATUS_T GORMcmd_HCC_Set_BT_FTR(VOID)
{
    hciCmd.opCode = 0xFC7D;
    hciCmd.len = 2;

    hciCmd.parms[0] = btinit->bt_nvram.fields.BtFTR[0];
    hciCmd.parms[1] = btinit->bt_nvram.fields.BtFTR[1];

    LOG_DBG("GORMcmd_HCC_Set_BT_FTR\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_OSC_Info(void)
{
    hciCmd.opCode = 0xFC7B;
    hciCmd.len = 5;

    hciCmd.parms[0] = 0x01;
    hciCmd.parms[1] = 0x01;
    hciCmd.parms[2] = 0x14; /* clock drift */
    hciCmd.parms[3] = 0x0A; /* clock jitter */
    hciCmd.parms[4] = 0x08; /* OSC stable time */

    LOG_DBG("GORMcmd_HCC_Set_OSC_Info\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_LPO_Info(VOID)
{
    hciCmd.opCode = 0xFC7C;
    hciCmd.len = 10;

    hciCmd.parms[0] = 0x01; /* LPO source = external */
    hciCmd.parms[1] = 0xFA; /* LPO clock drift = 250ppm */
    hciCmd.parms[2] = 0x0A; /* LPO clock jitter = 10us */
    hciCmd.parms[3] = 0x02; /* LPO calibration mode = manual mode */
    hciCmd.parms[4] = 0x00; /* LPO calibration interval = 10 mins */
    hciCmd.parms[5] = 0xA6;
    hciCmd.parms[6] = 0x0E;
    hciCmd.parms[7] = 0x00;
    hciCmd.parms[8] = 0x40; /* LPO calibration cycles = 64 */
    hciCmd.parms[9] = 0x00;

    LOG_DBG("GORMcmd_HCC_Set_LPO_Info\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_PTA(VOID)
{
    hciCmd.opCode = 0xFC74;
    hciCmd.len = 10;

    hciCmd.parms[0] = 0xC9; /* PTA mode register */
    hciCmd.parms[1] = 0x8B;
    hciCmd.parms[2] = 0xBF;
    hciCmd.parms[3] = 0x00;
    hciCmd.parms[4] = 0x00; /* PTA time register */
    hciCmd.parms[5] = 0x52;
    hciCmd.parms[6] = 0x0E;
    hciCmd.parms[7] = 0x0E;
    hciCmd.parms[8] = 0x1F; /* PTA priority setting */
    hciCmd.parms[9] = 0x1B;

    LOG_DBG("GORMcmd_HCC_Set_PTA\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_BLEPTA(VOID)
{
    hciCmd.opCode = 0xFCFC;
    hciCmd.len = 5;

    hciCmd.parms[0] = 0x16; /* Select BLE PTA command */
    hciCmd.parms[1] = 0x0E; /* BLE PTA time setting */
    hciCmd.parms[2] = 0x0E;
    hciCmd.parms[3] = 0x00; /* BLE PTA priority setting */
    hciCmd.parms[4] = 0x07;

    LOG_DBG("GORMcmd_HCC_Set_BLEPTA\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Internal_PTA_1(VOID)
{
    hciCmd.opCode = 0xFCFB;
    hciCmd.len = 15;

    hciCmd.parms[0] = 0x00;
    hciCmd.parms[1] = 0x01;  /* PTA high level Tx */
    hciCmd.parms[2] = 0x0F;  /* PTA mid level Tx */
    hciCmd.parms[3] = 0x0F;  /* PTA low level Tx */
    hciCmd.parms[4] = 0x01;  /* PTA high level Rx */
    hciCmd.parms[5] = 0x0F;  /* PTA mid level Rx */
    hciCmd.parms[6] = 0x0F;  /* PTA low level Rx */
    hciCmd.parms[7] = 0x01;  /* BLE PTA high level Tx */
    hciCmd.parms[8] = 0x0F;  /* BLE PTA mid level Tx */
    hciCmd.parms[9] = 0x0F;  /* BLE PTA low level Tx */
    hciCmd.parms[10] = 0x01; /* BLE PTA high level Rx */
    hciCmd.parms[11] = 0x0F; /* BLE PTA mid level Rx */
    hciCmd.parms[12] = 0x0F; /* BLE PTA low level Rx */
    hciCmd.parms[13] = 0x02; /* time_r2g */
    hciCmd.parms[14] = 0x01;

    LOG_DBG("GORMcmd_HCC_Set_Internal_PTA_1\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_Internal_PTA_2(VOID)
{
    hciCmd.opCode = 0xFCFB;
    hciCmd.len = 7;

    hciCmd.parms[0] = 0x01;
    hciCmd.parms[1] = 0x19; /* wifi20_hb */
    hciCmd.parms[2] = 0x19; /* wifi20_hb */
    hciCmd.parms[3] = 0x07; /* next RSSI update BT slots */
    hciCmd.parms[4] = 0xD0;
    hciCmd.parms[5] = 0x00; /* stream identify by host */
    hciCmd.parms[6] = 0x01; /* enable auto AFH */

    LOG_DBG("GORMcmd_HCC_Set_Internal_PTA_2\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}

static ENUM_BT_STATUS_T GORMcmd_HCC_Set_RF_Reg_100(VOID)
{
    hciCmd.opCode = 0xFCB0;
    hciCmd.len = 6;

    hciCmd.parms[0] = 0x64;
    hciCmd.parms[1] = 0x01;
    hciCmd.parms[2] = 0x02;
    hciCmd.parms[3] = 0x00;
    hciCmd.parms[4] = 0x00;
    hciCmd.parms[5] = 0x00;

    LOG_DBG("GORMcmd_HCC_Set_RF_Reg_100\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}
#endif

static ENUM_BT_STATUS_T GORMcmd_HCC_RESET(VOID)
{
    hciCmd.opCode = 0x0C03;
    hciCmd.len = 0;

    LOG_DBG("GORMcmd_HCC_RESET\n");

    if (BT_SendHciCommand(bt_com_port, &hciCmd) == TRUE) {
        return BT_STATUS_SUCCESS;
    }
    else {
        return BT_STATUS_FAILED;
    }
}


ENUM_BT_STATUS_T GORM_Init(
    INT32   comPort,
    UINT32  chipId,
    PUCHAR  pucPatchExtData,
    UINT32  u4PatchExtLen,
    PUCHAR  pucPatchData,
    UINT32  u4PatchLen,
    PUCHAR  pucNvRamData,
    UINT32  u4Baud,
    UINT32  u4HostBaud,
    UINT32  u4FlowControl
    )
{
    INT32   i = 0;
    ENUM_BT_STATUS_T status;
    UINT8   ucEventBuf[MAX_EVENT_SIZE];
    UINT32  u4EventLen;

    LOG_DBG("GORM_Init\n");

    /* Save com port fd for GORMcmds */
    bt_com_port = comPort;
    /* Save chip id */
    btinit->chip_id = chipId;
    /* Copy NVRAM data */
    memcpy(btinit->bt_nvram.raw, pucNvRamData, sizeof(ap_nvram_btradio_struct));

    /* General init script */
    switch (btinit->chip_id) {
    #ifdef MTK_MT6628
      case 0x6628:
        btinit->cur_script = bt_init_script_6628;
        break;
    #endif
    #ifdef MTK_MT6630
      case 0x6630:
        btinit->cur_script = bt_init_script_6630;
        break;
    #endif
    #ifdef MTK_CONSYS_MT6582
      case 0x6582:
        btinit->cur_script = bt_init_script_consys;
        break;
    #endif
    #ifdef MTK_CONSYS_MT6592
      case 0x6592:
        btinit->cur_script = bt_init_script_consys;
        break;
    #endif
    #ifdef MTK_CONSYS_MT6752
      case 0x6752:
        btinit->cur_script = bt_init_script_consys;
        break;
    #endif
    #ifdef MTK_CONSYS_MT6735
      case 0x0321:
      case 0x0335:
      case 0x0337:
        btinit->cur_script = bt_init_script_consys;
        break;
    #endif
    #ifdef MTK_CONSYS_MT6580
      case 0x6580:
        btinit->cur_script = bt_init_script_consys;
        break;
    #endif
    #ifdef MTK_CONSYS_MT6755
      case 0x6755:
        btinit->cur_script = bt_init_script_consys;
        break;
    #endif
      default:
        LOG_ERR("Unknown combo chip id\n");
        break;
    }

    /* Can not find matching script, simply skip */
    if ((btinit->cur_script) == NULL) {
        LOG_ERR("No matching init script\n");
        return BT_STATUS_FAILED;
    }

    i = 0;

    while (btinit->cur_script[i].command_func)
    {
        status = btinit->cur_script[i].command_func();
        if (status == BT_STATUS_CANCELLED) {
            i ++;
            continue; /*skip*/
        }

        if (status == BT_STATUS_FAILED) {
            LOG_ERR("Command %d fails\n", i);
            return status;
        }

        if (BT_ReadExpectedEvent(
              comPort,
              ucEventBuf,
              MAX_EVENT_SIZE,
              &u4EventLen,
              0x0E,
              TRUE,
              hciCmd.opCode,
              TRUE,
              0x00) == FALSE) {

            LOG_ERR("Read event of command %d fails\n", i);
            return BT_STATUS_FAILED;
        }

        i ++;
    }

    return BT_STATUS_SUCCESS;
}


static BOOL BT_Get_Local_BD_Addr(UCHAR *pucBDAddr)
{
    HCI_CMD_T cmd;
    UINT32 u4ReadLen = 0;
    UINT8 ucAckEvent[20];

    cmd.opCode = 0x1009;
    cmd.len = 0;

    LOG_DBG("BT_Get_Local_BD_Addr\n");

    if (BT_SendHciCommand(bt_com_port, &cmd) == FALSE) {
        LOG_ERR("Write get BD address command fails\n");
        return FALSE;
    }

    /* Read local BD address from F/W */
    if (BT_ReadExpectedEvent(
          bt_com_port,
          ucAckEvent,
          sizeof(ucAckEvent),
          &u4ReadLen,
          0x0E,
          TRUE,
          0x1009,
          TRUE,
          0x00) == FALSE) {

        LOG_ERR("Read local BD address fails\n");
        return FALSE;
    }

    LOG_WAN("Local BD address: %02x-%02x-%02x-%02x-%02x-%02x\n",
            ucAckEvent[12], ucAckEvent[11], ucAckEvent[10],
            ucAckEvent[9], ucAckEvent[8], ucAckEvent[7]);

    pucBDAddr[0] = ucAckEvent[12];
    pucBDAddr[1] = ucAckEvent[11];
    pucBDAddr[2] = ucAckEvent[10];
    pucBDAddr[3] = ucAckEvent[9];
    pucBDAddr[4] = ucAckEvent[8];
    pucBDAddr[5] = ucAckEvent[7];

    return TRUE;
}

static VOID GetRandomValue(UCHAR string[6])
{
    INT32 iRandom = 0;
    INT32 fd = 0;
    UINT32 seed;

    LOG_WAN("Enable random generation\n");

    /* Initialize random seed */
    srand(time(NULL));
    iRandom = rand();
    LOG_WAN("iRandom = [%d]", iRandom);
    string[0] = (((iRandom>>24|iRandom>>16) & (0xFE)) | (0x02)); /* Must use private bit(1) and no BCMC bit(0) */

    /* second seed */
    struct timeval tv;
    gettimeofday(&tv, NULL);
    srand(tv.tv_usec);
    iRandom = rand();
    LOG_WAN("iRandom = [%d]", iRandom);
    string[1] = ((iRandom>>8) & 0xFF);

    /* third seed */
    fd = open("/dev/urandom", O_RDONLY);
    if (fd > 0) {
        if (read(fd, &seed, sizeof(UINT32)) > 0) {
            srand(seed);
            iRandom = rand();
        }
        close(fd);
    }

    LOG_WAN("iRandom = [%d]", iRandom);
    string[5] = (iRandom & 0xFF);

    return;
}

static BOOL WriteBDAddrToNvram(UCHAR *pucBDAddr)
{
    F_ID bt_nvram_fd = {0};
    INT32 rec_size = 0;
    INT32 rec_num = 0;

    bt_nvram_fd = NVM_GetFileDesc(AP_CFG_RDEB_FILE_BT_ADDR_LID, &rec_size, &rec_num, ISWRITE);
    if (bt_nvram_fd.iFileDesc < 0) {
        LOG_WAN("Open BT NVRAM fails errno %d\n", errno);
        return FALSE;
    }

    if (rec_num != 1) {
        LOG_ERR("Unexpected record num %d\n", rec_num);
        NVM_CloseFileDesc(bt_nvram_fd);
        return FALSE;
    }

    if (rec_size != sizeof(ap_nvram_btradio_struct)) {
        LOG_ERR("Unexpected record size %d ap_nvram_btradio_struct %d\n",
                rec_size, sizeof(ap_nvram_btradio_struct));
        NVM_CloseFileDesc(bt_nvram_fd);
        return FALSE;
    }

    lseek(bt_nvram_fd.iFileDesc, 0, 0);

    /* Update BD address */
    if (write(bt_nvram_fd.iFileDesc, pucBDAddr, 6) < 0) {
        LOG_ERR("Write BT NVRAM fails errno %d\n", errno);
        NVM_CloseFileDesc(bt_nvram_fd);
        return FALSE;
    }

    NVM_CloseFileDesc(bt_nvram_fd);
    return TRUE;
}
