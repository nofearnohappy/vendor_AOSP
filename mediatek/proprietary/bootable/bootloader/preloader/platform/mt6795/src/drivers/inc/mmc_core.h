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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

#ifndef MMC_CORE_H
#define MMC_CORE_H

#include "mmc_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
     EMMC_PART_UNKNOWN=0
    ,EMMC_PART_BOOT1
    ,EMMC_PART_BOOT2
    ,EMMC_PART_RPMB
    ,EMMC_PART_GP1
    ,EMMC_PART_GP2
    ,EMMC_PART_GP3
    ,EMMC_PART_GP4
    ,EMMC_PART_USER
    ,EMMC_PART_END
} Region;

#define MMC_BLOCK_BITS                  (9)
#define MMC_BLOCK_SIZE                  (1 << MMC_BLOCK_BITS)
#define MMC_MAX_BLOCK_SIZE              (1 << MMC_BLOCK_BITS)

#define SD_CMD_BIT                      (1 << 7)
#define SD_CMD_APP_BIT                  (1 << 8)
#define SD_CMD_AUTO_BIT                 (1 << 9)

/* MMC command numbers */
#define MMC_CMD_GO_IDLE_STATE           (0) 
#define MMC_CMD_SEND_OP_COND            (1) 
#define MMC_CMD_ALL_SEND_CID            (2) 
#define MMC_CMD_SET_RELATIVE_ADDR       (3)
#define MMC_CMD_SET_DSR                 (4) 
#define MMC_CMD_SLEEP_AWAKE             (5) 
#define MMC_CMD_SWITCH                  (6)  
#define MMC_CMD_SELECT_CARD             (7) 
#define MMC_CMD_SEND_EXT_CSD            (8) 
#define MMC_CMD_SEND_CSD                (9) 
#define MMC_CMD_SEND_CID                (10)
#define MMC_CMD_READ_DAT_UNTIL_STOP     (11) 
#define MMC_CMD_STOP_TRANSMISSION       (12)
#define MMC_CMD_SEND_STATUS             (13)  
#define MMC_CMD_BUSTEST_R               (14) 
#define MMC_CMD_GO_INACTIVE_STATE       (15)  
#define MMC_CMD_SET_BLOCKLEN            (16)  
#define MMC_CMD_READ_SINGLE_BLOCK       (17) 
#define MMC_CMD_READ_MULTIPLE_BLOCK     (18) 
#define MMC_CMD_BUSTEST_W               (19) 
#define MMC_CMD_WRITE_DAT_UNTIL_STOP    (20) 
#define MMC_CMD21                       (21)             
#define MMC_CMD_SET_BLOCK_COUNT         (23)
#define MMC_CMD_WRITE_BLOCK             (24)
#define MMC_CMD_WRITE_MULTIPLE_BLOCK    (25) 
#define MMC_CMD_PROGRAM_CID             (26) 
#define MMC_CMD_PROGRAM_CSD             (27) 
#define MMC_CMD_SET_WRITE_PROT          (28) 
#define MMC_CMD_CLR_WRITE_PROT          (29) 
#define MMC_CMD_SEND_WRITE_PROT         (30) 
#define MMC_CMD_SEND_WRITE_PROT_TYPE    (31)
#define MMC_CMD_ERASE_WR_BLK_START      (32)
#define MMC_CMD_ERASE_WR_BLK_END        (33)
#define MMC_CMD_ERASE_GROUP_START       (35)
#define MMC_CMD_ERASE_GROUP_END         (36)
#define MMC_CMD_ERASE                   (38)
#define MMC_CMD_FAST_IO                 (39)
#define MMC_CMD_GO_IRQ_STATE            (40)
#define MMC_CMD_LOCK_UNLOCK             (42)
#define MMC_CMD50                       (50)
#define MMC_CMD_APP_CMD                 (55)
#define MMC_CMD_GEN_CMD                 (56)

/* SD Card command numbers */
#define SD_CMD_SEND_RELATIVE_ADDR       (3 | SD_CMD_BIT)
#define SD_CMD_SWITCH                   (6 | SD_CMD_BIT)
#define SD_CMD_SEND_IF_COND             (8 | SD_CMD_BIT)
#define SD_CMD_VOL_SWITCH               (11 | SD_CMD_BIT)
#define SD_CMD_SEND_TUNING_BLOCK        (19 | SD_CMD_BIT)
#define SD_CMD_SPEED_CLASS_CTRL         (20 | SD_CMD_BIT)

#define SD_ACMD_SET_BUSWIDTH	          (6  | SD_CMD_APP_BIT)
#define SD_ACMD_SD_STATUS               (13 | SD_CMD_APP_BIT)
#define SD_ACMD_SEND_NR_WR_BLOCKS       (22 | SD_CMD_APP_BIT)
#define SD_ACMD_SET_WR_ERASE_CNT        (23 | SD_CMD_APP_BIT)
#define SD_ACMD_SEND_OP_COND            (41 | SD_CMD_APP_BIT)
#define SD_ACMD_SET_CLR_CD              (42 | SD_CMD_APP_BIT)
#define SD_ACMD_SEND_SCR                (51 | SD_CMD_APP_BIT)

/* SDIO Card command numbers */
#define SD_IO_SEND_OP_COND              (5 | SD_CMD_BIT) 
#define SD_IO_RW_DIRECT                 (52 | SD_CMD_BIT)
#define SD_IO_RW_EXTENDED               (53 | SD_CMD_BIT)

/* platform dependent command */
#define SD_ATOCMD_STOP_TRANSMISSION     (12 | SD_CMD_AUTO_BIT)
#define SD_ATOCMD_SET_BLOCK_COUNT       (23 | SD_CMD_AUTO_BIT)

#define MSDC_VDD_35_36	0x00800000	/* 3.5 ~ 3.6 */
#define MSDC_VDD_34_35	0x00400000	/* 3.4 ~ 3.5 */
#define MSDC_VDD_33_34	0x00200000	/* 3.3 ~ 3.4 */
#define MSDC_VDD_32_33	0x00100000	/* 3.2 ~ 3.3 */
#define MSDC_VDD_31_32	0x00080000	/* 3.1 ~ 3.2 */
#define MSDC_VDD_30_31	0x00040000	/* 3.0 ~ 3.1 */
#define MSDC_VDD_29_30	0x00020000	/* 2.9 ~ 3.0 */
#define MSDC_VDD_28_29	0x00010000	/* 2.8 ~ 2.9 */
#define MSDC_VDD_27_28	0x00008000	/* 2.7 ~ 2.8 */
#define MSDC_VDD_26_27	0x00004000	/* 2.6 ~ 2.7 */
#define MSDC_VDD_25_26	0x00002000	/* 2.5 ~ 2.6 */
#define MSDC_VDD_24_25	0x00001000	/* 2.4 ~ 2.5 */
#define MSDC_VDD_23_24	0x00000800	/* 2.3 ~ 2.4 */
#define MSDC_VDD_22_23	0x00000400	/* 2.2 ~ 2.3 */
#define MSDC_VDD_21_22	0x00000200	/* 2.1 ~ 2.2 */
#define MSDC_VDD_20_21	0x00000100	/* 2.0 ~ 2.1 */
#define MSDC_VDD_19_20	0x00000080	/* 1.9 - 2.0 */
#define MSDC_VDD_18_19	0x00000040	/* 1.8 - 1.9 */
#define MSDC_VDD_17_18	0x00000020	/* 1.7 - 1.8 */
#define MSDC_VDD_165_170	0x00000010	/* 1.65 - 1.70 */
#define MSDC_VDD_160_165	0x00000008	/* 1.60 - 1.65 */
#define MSDC_VDD_155_160	0x00000004	/* 1.55 - 1.60 */
#define MSDC_VDD_150_155	0x00000002	/* 1.50 - 1.55 */
#define MSDC_VDD_145_150	0x00000001	/* 1.45 - 1.50 */
#define MMC_CARD_BUSY	0x80000000	/* Card Power up status bit */

#define MMC_ERR_NONE          (0)
#define MMC_ERR_TIMEOUT       (1)
#define MMC_ERR_BADCRC        (2)
#define MMC_ERR_FIFO          (3)
#define MMC_ERR_FAILED        (4)
#define MMC_ERR_INVALID       (5)
#define MMC_ERR_CMDTUNEFAIL	  (6)
#define MMC_ERR_READTUNEFAIL  (7)
#define MMC_ERR_WRITETUNEFAIL (8)
#define MMC_ERR_CMD_TIMEOUT	  (9)
#define MMC_ERR_CMD_RSPCRC	  (10)
#define MMC_ERR_ACMD_TIMEOUT  (11)
#define MMC_ERR_ACMD_RSPCRC   (12)




#define MMC_POWER_OFF       0
#define MMC_POWER_UP        1
#define MMC_POWER_ON        2

#define MMC_BUS_WIDTH_1     0
#define MMC_BUS_WIDTH_4     2

#define SD_BUS_WIDTH_1      0
#define SD_BUS_WIDTH_4      2

#define MMC_STATE_PRESENT        (1 << 0)
#define MMC_STATE_READONLY       (1 << 1)
#define MMC_STATE_HIGHSPEED      (1 << 2)
#define MMC_STATE_BLOCKADDR      (1 << 3) 
#define MMC_STATE_HIGHCAPS       (1 << 4)
#define MMC_STATE_UHS1           (1 << 5)
#define MMC_STATE_DDR            (1 << 6)

#define STA_OUT_OF_RANGE         (1UL << 31) 
#define STA_ADDRESS_ERROR        (1 << 30)   
#define STA_BLOCK_LEN_ERROR      (1 << 29)   
#define STA_ERASE_SEQ_ERROR      (1 << 28)
#define STA_ERASE_PARAM          (1 << 27) 
#define STA_WP_VIOLATION         (1 << 26) 
#define STA_CARD_IS_LOCKED       (1 << 25)
#define STA_LOCK_UNLOCK_FAILED   (1 << 24) 
#define STA_COM_CRC_ERROR        (1 << 23) 
#define STA_ILLEGAL_COMMAND      (1 << 22) 
#define STA_CARD_ECC_FAILED      (1 << 21)  
#define STA_CC_ERROR             (1 << 20) 
#define STA_ERROR                (1 << 19) 
#define STA_UNDERRUN             (1 << 18)  
#define STA_OVERRUN              (1 << 17)
#define STA_CID_CSD_OVERWRITE    (1 << 16) 
#define STA_WP_ERASE_SKIP        (1 << 15)
#define STA_CARD_ECC_DISABLED    (1 << 14) 
#define STA_ERASE_RESET          (1 << 13) 
#define STA_STATUS(x)            (x & 0xFFFFE000)
#define STA_CURRENT_STATE(x)     ((x & 0x00001E00) >> 9) 
#define STA_READY_FOR_DATA       (1 << 8) 
#define STA_SWITCH_ERROR         (1 << 7)
#define STA_URGENT_BKOPS         (1 << 6) 
#define STA_APP_CMD              (1 << 5) 

// Card Command Classes (CCC)
#define MMC_CCC_BASIC               (1<<0) 
#define MMC_CCC_STREAM_READ         (1<<1) 
#define MMC_CCC_BLOCK_READ          (1<<2)
#define MMC_CCC_STREAM_WRITE        (1<<3) 
#define MMC_CCC_BLOCK_WRITE         (1<<4) 
#define MMC_CCC_ERASE               (1<<5) 
#define MMC_CCC_WRITE_PROT          (1<<6) 
#define MMC_CCC_LOCK_CARD           (1<<7) 
#define MMC_CCC_APP_SPEC            (1<<8) 
#define MMC_CCC_IO_MODE             (1<<9) 
#define MMC_CCC_SWITCH              (1<<10) 

//CSD register bit
#define CSD_STRUCT_VER_1_0          (0)
#define CSD_STRUCT_VER_1_1          (1) 
#define CSD_STRUCT_VER_1_2          (2) 
#define CSD_STRUCT_EXT_CSD          (3) 

#define CSD_SPEC_VER_0              (0) 
#define CSD_SPEC_VER_1              (1) 
#define CSD_SPEC_VER_2              (2)  
#define CSD_SPEC_VER_3              (3)  
#define CSD_SPEC_VER_4              (4) 

// EXT_CSD register bit
#define EXT_CSD_BADBLK_MGMT             134 
#define EXT_CSD_ENH_START_ADDR          136 
#define EXT_CSD_ENH_SIZE_MULT           140 
#define EXT_CSD_GP1_SIZE_MULT           143 
#define EXT_CSD_GP2_SIZE_MULT           146 
#define EXT_CSD_GP3_SIZE_MULT           149 
#define EXT_CSD_GP4_SIZE_MULT           152 
#define EXT_CSD_PART_SET_COMPL          155 
#define EXT_CSD_PART_ATTR               156 
#define EXT_CSD_MAX_ENH_SIZE_MULT       157 
#define EXT_CSD_PART_SUPPORT            160 
#define EXT_CSD_HPI_MGMT                161 
#define EXT_CSD_RST_N_FUNC              162 
#define EXT_CSD_BKOPS_EN                163 
#define EXT_CSD_BKOPS_START             164 
#define EXT_CSD_WR_REL_PARAM            166 
#define EXT_CSD_WR_REL_SET              167 
#define EXT_CSD_RPMB_SIZE_MULT          168 
#define EXT_CSD_FW_CONFIG               169 
#define EXT_CSD_USR_WP                  171 
#define EXT_CSD_BOOT_WP                 173 
#define EXT_CSD_ERASE_GRP_DEF           175 
#define EXT_CSD_BOOT_BUS_WIDTH          177 
#define EXT_CSD_BOOT_CONFIG_PROT        178 
#define EXT_CSD_PART_CFG                179 
#define EXT_CSD_ERASED_MEM_CONT         181 
#define EXT_CSD_BUS_WIDTH               183 
#define EXT_CSD_HS_TIMING               185 
#define EXT_CSD_PWR_CLASS               187 
#define EXT_CSD_CMD_SET_REV             189 
#define EXT_CSD_CMD_SET                 191 
#define EXT_CSD_REV                     192 
#define EXT_CSD_STRUCT                  194 
#define EXT_CSD_CARD_TYPE               196 
#define EXT_CSD_OUT_OF_INTR_TIME        198 
#define EXT_CSD_PART_SWITCH_TIME        199 
#define EXT_CSD_PWR_CL_52_195           200 
#define EXT_CSD_PWR_CL_26_195           201 
#define EXT_CSD_PWR_CL_52_360           202 
#define EXT_CSD_PWR_CL_26_360           203 
#define EXT_CSD_MIN_PERF_R_4_26         205 
#define EXT_CSD_MIN_PERF_W_4_26         206 
#define EXT_CSD_MIN_PERF_R_8_26_4_25    207 
#define EXT_CSD_MIN_PERF_W_8_26_4_25    208 
#define EXT_CSD_MIN_PERF_R_8_52         209 
#define EXT_CSD_MIN_PERF_W_8_52         210 
#define EXT_CSD_SEC_CNT                 212 
#define EXT_CSD_S_A_TIMEOUT             217 
#define EXT_CSD_S_C_VCCQ                219 
#define EXT_CSD_S_C_VCC                 220 
#define EXT_CSD_HC_WP_GPR_SIZE          221 
#define EXT_CSD_REL_WR_SEC_C            222 
#define EXT_CSD_ERASE_TIMEOUT_MULT      223 
#define EXT_CSD_HC_ERASE_GRP_SIZE       224 
#define EXT_CSD_ACC_SIZE                225 
#define EXT_CSD_BOOT_SIZE_MULT          226 
#define EXT_CSD_BOOT_INFO               228 
#define EXT_CSD_SEC_TRIM_MULT           229 
#define EXT_CSD_SEC_ERASE_MULT          230 
#define EXT_CSD_SEC_FEATURE_SUPPORT     231 
#define EXT_CSD_TRIM_MULT               232 
#define EXT_CSD_MIN_PERF_DDR_R_8_52     234 
#define EXT_CSD_MIN_PERF_DDR_W_8_52     235 
#define EXT_CSD_PWR_CL_DDR_52_195       238 
#define EXT_CSD_PWR_CL_DDR_52_360       239 
#define EXT_CSD_INI_TIMEOUT_AP          241 
#define EXT_CSD_CORRECT_PRG_SECTS_NUM   242
#define EXT_CSD_FIRMWARE_VERSION        261 /* R (5.0) */
#define EXT_CSD_BKOPS_STATUS            246 
#define EXT_CSD_BKOPS_SUPP              502 
#define EXT_CSD_HPI_FEATURE             503 
#define EXT_CSD_S_CMD_SET               504 

/* SEC_FEATURE_SUPPORT[231] */
#define EXT_CSD_SEC_FEATURE_ER_EN       (1<<0)
#define EXT_CSD_SEC_FEATURE_BD_BLK_EN   (1<<2)
#define EXT_CSD_SEC_FEATURE_GB_CL_EN    (1<<4)

/* BOOT_INFO[228] */
#define EXT_CSD_BOOT_INFO_ALT_BOOT      (1<<0)
#define EXT_CSD_BOOT_INFO_DDR_BOOT      (1<<1)
#define EXT_CSD_BOOT_INFO_HS_BOOT       (1<<2)

#define EXT_CSD_CMD_SET_NORMAL          (1<<0)
#define EXT_CSD_CMD_SET_SECURE          (1<<1)
#define EXT_CSD_CMD_SET_CPSECURE        (1<<2)

#define EXT_CSD_CARD_TYPE_26            (1<<0) 
#define EXT_CSD_CARD_TYPE_52            (1<<1) 
#define EXT_CSD_CARD_TYPE_DDR_52        (1<<2) 
#define EXT_CSD_CARD_TYPE_DDR_52_1_2V   (1<<3)

/* BUS_WIDTH[183] */
#define EXT_CSD_BUS_WIDTH_1             (0) 
#define EXT_CSD_BUS_WIDTH_4             (1) 
#define EXT_CSD_BUS_WIDTH_8             (2) 
#define EXT_CSD_BUS_WIDTH_4_DDR         (5) 
#define EXT_CSD_BUS_WIDTH_8_DDR         (6) 

/* ERASED_MEM_CONT[181] */
#define EXT_CSD_ERASED_MEM_CONT_0       (0)
#define EXT_CSD_ERASED_MEM_CONT_1       (1)

/* PARTITION CONFIG[179] */
#define EXT_CSD_PART_CFG_DEFT_PART      (0)
#define EXT_CSD_PART_CFG_BOOT_PART_1    (1)
#define EXT_CSD_PART_CFG_BOOT_PART_2    (2)
#define EXT_CSD_PART_CFG_RPMB_PART      (3)
#define EXT_CSD_PART_CFG_GP_PART_1      (4)
#define EXT_CSD_PART_CFG_GP_PART_2      (5)
#define EXT_CSD_PART_CFG_GP_PART_3      (6)
#define EXT_CSD_PART_CFG_GP_PART_4      (7)
#define EXT_CSD_PART_CFG_EN_NO_BOOT     (0 << 3)
#define EXT_CSD_PART_CFG_EN_BOOT_PART_1 (1 << 3)
#define EXT_CSD_PART_CFG_EN_BOOT_PART_2 (2 << 3)
#define EXT_CSD_PART_CFG_EN_USER_AREA   (7 << 3)
#define EXT_CSD_PART_CFG_EN_NO_ACK      (0 << 6)
#define EXT_CSD_PART_CFG_EN_ACK         (1 << 6)

/* BOOT_CONFIG_PROT[178] */
#define EXT_CSD_EN_PWR_BOOT_CFG_PROT    (1)
#define EXT_CSD_EN_PERM_BOOT_CFG_PROT   (1<<4)  /* Carefully */

/* BOOT_BUS_WIDTH[177] */
#define EXT_CSD_BOOT_BUS_WIDTH_1        (0)
#define EXT_CSD_BOOT_BUS_WIDTH_4        (1)
#define EXT_CSD_BOOT_BUS_WIDTH_8        (2)
#define EXT_CSD_BOOT_BUS_RESET          (1 << 2)
#define EXT_CSD_BOOT_BUS_MODE_DEFT      (0 << 3)
#define EXT_CSD_BOOT_BUS_MODE_HS        (1 << 3)
#define EXT_CSD_BOOT_BUS_MODE_DDR       (2 << 3)

/* ERASE_GROUP_DEF[175] */
#define EXT_CSD_ERASE_GRP_DEF_EN        (1)

/* BOOT_WP[173] */
#define EXT_CSD_BOOT_WP_EN_PWR_WP       (1)
#define EXT_CSD_BOOT_WP_EN_PERM_WP      (1 << 2)
#define EXT_CSD_BOOT_WP_DIS_PERM_WP     (1 << 4)
#define EXT_CSD_BOOT_WP_DIS_PWR_WP      (1 << 6)

/* USER_WP[171] */
#define EXT_CSD_USR_WP_EN_PWR_WP        (1)
#define EXT_CSD_USR_WP_EN_PERM_WP       (1<<2)
#define EXT_CSD_USR_WP_DIS_PWR_WP       (1<<3)
#define EXT_CSD_USR_WP_DIS_PERM_WP      (1<<4)
#define EXT_CSD_USR_WP_DIS_CD_PERM_WP   (1<<6)
#define EXT_CSD_USR_WP_DIS_PERM_PWD     (1<<7)

/* RST_n_FUNCTION[162] */
#define EXT_CSD_RST_N_TEMP_DIS          (0)
#define EXT_CSD_RST_N_PERM_EN           (1) /* carefully */
#define EXT_CSD_RST_N_PERM_DIS          (2) /* carefully */

/* PARTITIONING_SUPPORT[160] */
#define EXT_CSD_PART_SUPPORT_PART_EN     (1)
#define EXT_CSD_PART_SUPPORT_ENH_ATTR_EN (1<<1)

/* PARTITIONS_ATTRIBUTE[156] */
#define EXT_CSD_PART_ATTR_ENH_USR       (1<<0)
#define EXT_CSD_PART_ATTR_ENH_1         (1<<1)
#define EXT_CSD_PART_ATTR_ENH_2         (1<<2)
#define EXT_CSD_PART_ATTR_ENH_3         (1<<3)
#define EXT_CSD_PART_ATTR_ENH_4         (1<<4)

/* PARTITION_SETTING_COMPLETED[156] */
#define EXT_CSD_PART_SET_COMPL_BIT      (1<<0)

/*
 * MMC_SWITCH access modes
 */

#define MMC_SWITCH_MODE_CMD_SET		  (0x00)	
#define MMC_SWITCH_MODE_SET_BITS	  (0x01)	
#define MMC_SWITCH_MODE_CLEAR_BITS	  (0x02)	
#define MMC_SWITCH_MODE_WRITE_BYTE	  (0x03)	

#define MMC_SWITCH_MODE_SDR12       0
#define MMC_SWITCH_MODE_SDR25       1
#define MMC_SWITCH_MODE_SDR50       2
#define MMC_SWITCH_MODE_SDR104      3
#define MMC_SWITCH_MODE_DDR50       4

#define MMC_SWITCH_MODE_DRV_TYPE_B  0
#define MMC_SWITCH_MODE_DRV_TYPE_A  1
#define MMC_SWITCH_MODE_DRV_TYPE_C  2
#define MMC_SWITCH_MODE_DRV_TYPE_D  3

#define MMC_SWITCH_MODE_CL_200MA    0
#define MMC_SWITCH_MODE_CL_400MA    1
#define MMC_SWITCH_MODE_CL_600MA    2
#define MMC_SWITCH_MODE_CL_800MA    3

/* 
 * MMC_ERASE arguments
 */
#define MMC_ERASE_SECURE_REQ        (1 << 31)
#define MMC_ERASE_GC_REQ            (1 << 15)
#define MMC_ERASE_TRIM              (1 << 0)
#define MMC_ERASE_NORMAL            (0)

#define HOST_BUS_WIDTH_1            (1)
#define HOST_BUS_WIDTH_4            (4)
#define HOST_BUS_WIDTH_8            (8)

#define EMMC_BOOT_PULL_CMD_MODE     (0)
#define EMMC_BOOT_RST_CMD_MODE      (1)

enum {
    EMMC_BOOT_PWR_RESET = 0,
    EMMC_BOOT_RST_N_SIG,
    EMMC_BOOT_PRE_IDLE_CMD
};

enum {
    RESP_NONE = 0,
    RESP_R1,
    RESP_R2,
    RESP_R3,
    RESP_R4,
    RESP_R5,
    RESP_R6,
    RESP_R7,
    RESP_R1B
};

struct mmc_cid {
    unsigned int   manfid;    
    char           prod_name[8];
    unsigned int   serial;
    unsigned short oemid;
    unsigned short year;
    unsigned char  hwrev;
    unsigned char  fwrev;
    unsigned char  month;
    unsigned char  cbx;                
};

struct mmc_csd {
    unsigned char  csd_struct;
    unsigned char  mmca_vsn;
    unsigned short cmdclass;
    unsigned short tacc_clks;   
    unsigned int   tacc_ns;     
    unsigned int   r2w_factor;  
    unsigned int   max_dtr;     
    unsigned int   read_blkbits;   
    unsigned int   write_blkbits;  
    unsigned int   capacity;       
    unsigned int   erase_sctsz;    
    unsigned int   write_prot_grpsz;
    unsigned int   read_partial:1,
                   read_misalign:1,
                   write_partial:1,
                   write_misalign:1,
                   write_prot_grp:1,
                   perm_wr_prot:1,
                   tmp_wr_prot:1,
                   erase_blk_en:1,
                   copy:1,
                   dsr:1;
};

struct mmc_raw_ext_csd {
    /* mode segment */
    unsigned char   rsv1[134];
    unsigned char   sec_bad_blk_mgmt;
    unsigned char   rsv2[1];
    unsigned char   enh_start_addr[4];
    unsigned char   enh_sz_mult[3];
    unsigned char   gp_sz_mult[12];
    unsigned char   part_set_cmpl;
    unsigned char   part_attr;
    unsigned char   max_enh_sz_mult[3];
    unsigned char   part_supp;
    unsigned char   rsv3[1];
    unsigned char   rst_n_func;
    unsigned char   rsv4[5];
    unsigned char   rpmb_sz_mult;
    unsigned char   fw_cfg;
    unsigned char   rsv5[1];
    unsigned char   user_wp;
    unsigned char   rsv6[1];
    unsigned char   boot_wp;
    unsigned char   rsv7[1];
    unsigned char   erase_grp_def;
    unsigned char   rsv8[1];
    unsigned char   boot_bus_width;
    unsigned char   boot_cfg_prot;
    unsigned char   part_cfg;
    unsigned char   rsv9[1];
    unsigned char   erase_mem_cont;
    unsigned char   rsv10[1];
    unsigned char   bus_width;
    unsigned char   rsv11[1];
    unsigned char   hs_timing;
    unsigned char   rsv12[1];
    unsigned char   pwr_cls;
    unsigned char   rsv13[1];
    unsigned char   cmd_set_rev;
    unsigned char   rsv14[1];
    unsigned char   cmd_set;

    /* propertities segment */
    unsigned char   ext_csd_rev;
    unsigned char   rsv15[1];
    unsigned char   csd_struct;
    unsigned char   rsv16[1];
    unsigned char   card_type;
    unsigned char   rsv17[1];
    unsigned char   pwr_cls_52_195;
    unsigned char   pwr_cls_26_195;
    unsigned char   pwr_cls_52_360;
    unsigned char   pwr_cls_26_360;
    unsigned char   rsv18[1];
    unsigned char   min_perf_r_4_26;
    unsigned char   min_perf_w_4_26;
    unsigned char   min_perf_r_8_26_4_52;
    unsigned char   min_perf_w_8_26_4_52;
    unsigned char   min_perf_r_8_52;
    unsigned char   min_perf_w_8_52;
    unsigned char   rsv19[1];
    unsigned char   sec_cnt[4];
    unsigned char   rsv20[1];
    unsigned char   slp_awake_tmo;
    unsigned char   rsv21[1];
    unsigned char   slp_curr_vccq;
    unsigned char   slp_curr_vcc;
    unsigned char   hc_wp_grp_sz;
    unsigned char   rel_wr_sec_cnt;
    unsigned char   erase_tmo_mult;
    unsigned char   hc_erase_grp_sz;
    unsigned char   acc_sz;
    unsigned char   boot_sz_mult;
    unsigned char   rsv22[1];
    unsigned char   boot_info;    
    unsigned char   sec_trim_mult;
    unsigned char   sec_erase_mult;
    unsigned char   sec_supp;
    unsigned char   trim_mult;
    unsigned char   rsv23[1];
    unsigned char   min_perf_ddr_r_8_52;
    unsigned char   min_perf_ddr_w_8_52;
    unsigned char   rsv24[2];
    unsigned char   pwr_cls_ddr_52_195;
    unsigned char   pwr_cls_ddr_52_360;
    unsigned char   rsv25[1];
    unsigned char   ini_tmo_ap;
    unsigned char   rsv26[262];
    unsigned char   supp_cmd_set;
    unsigned char   rsv27[7];
};

struct mmc_ext_csd {
    unsigned int    trim_tmo_ms;
    unsigned int    hc_wp_grp_sz;
    unsigned int    hc_erase_grp_sz;
    unsigned int    sectors;
    unsigned int    hs_max_dtr;
    unsigned int    boot_part_sz;
    unsigned int    rpmb_sz;
    unsigned int    access_sz;
    unsigned int    enh_sz;
    unsigned int    enh_start_addr;
    unsigned char   rev;
    unsigned char   boot_info;    
    unsigned char   part_en:1,
                    enh_attr_en:1,
                    ddr_support:1;
    unsigned char   erased_mem_cont;
};

#define SD_SCR_BUS_WIDTH_1	(1<<0)
#define SD_SCR_BUS_WIDTH_4	(1<<2)

struct sd_scr {
    unsigned char   scr_struct;
    unsigned char   sda_vsn;
    unsigned char   data_bit_after_erase;
    unsigned char   security;
    unsigned char   bus_widths;
    unsigned char   sda_vsn3;
    unsigned char   ex_security;
    unsigned char   cmd_support;
};

#define SD_DRV_TYPE_B       (0)
#define SD_DRV_TYPE_A       (1<<0)
#define SD_DRV_TYPE_C       (1<<1)
#define SD_DRV_TYPE_D       (1<<2)

#define SD_MAX_CUR_200MA    (0)
#define SD_MAX_CUR_400MA    (1<<0)
#define SD_MAX_CUR_600MA    (1<<1)
#define SD_MAX_CUR_800MA    (1<<2)

struct sd_switch_caps {
    unsigned int    hs_max_dtr;
    unsigned int    ddr;
    unsigned int    drv_strength;
    unsigned int    max_cur;
};

typedef void (*hw_irq_handler_t)(void);

#define MMC_CAP_4_BIT_DATA      (1 << 0) 
#define MMC_CAP_MULTIWRITE      (1 << 1) 
#define MMC_CAP_BYTEBLOCK       (1 << 2) 
#define MMC_CAP_MMC_HIGHSPEED   (1 << 3) 
#define MMC_CAP_SD_HIGHSPEED    (1 << 4) 
#define MMC_CAP_8_BIT_DATA      (1 << 5) 
#define MMC_CAP_SD_UHS1         (1 << 6) 
#define MMC_CAP_DDR             (1 << 7) 

struct mmc_host
{
    struct mmc_card *card;
    u64 max_hw_segs;
    u64 max_phys_segs;
    u64 max_seg_size;
    u32 max_blk_size;
    u32 max_blk_count;
    u32 base;         
    u32 caps;         
    u32 f_min;        
    u32 f_max;        
    u32 clk;          
    u32 sclk;         
    u32 blklen;       
    u32 blkbits;      
    u32 ocr;          
    u32 ocr_avail;    
    u32 timeout_ns;   
    u32 timeout_clks; 
    u8  clksrc;       
    u8  id;           
    u8  boot_type;    
    bool app_cmd;
    u32  app_cmd_arg;
    u32  time_read;
    void *priv;
    int (*blk_read)(struct mmc_host *host, uchar *dst, ulong src, ulong nblks);
    int (*blk_write)(struct mmc_host *host, ulong dst, uchar *src, ulong nblks);
};

#define MMC_TYPE_UNKNOWN    (0)          
#define MMC_TYPE_MMC        (0x00000001) 
#define MMC_TYPE_SD         (0x00000002) 
#define MMC_TYPE_SDIO       (0x00000004) 

/* MMC device */
struct mmc_card {
    struct mmc_host        *host;       
    unsigned int            nblks;
    unsigned int            blklen;
    unsigned int            ocr;
    unsigned int            maxhz;
    unsigned int            uhs_mode;
    unsigned int            rca;
    unsigned int            type;      
    unsigned short          state;     
    unsigned short          ready;     
    u32                     raw_cid[4];
    u32                     raw_csd[4];
    u32                     raw_scr[2];
    u8                      raw_ext_csd[512]; 
    struct mmc_cid          cid;       
    struct mmc_csd          csd;       
    struct mmc_ext_csd      ext_csd;   
    struct sd_scr           scr;       
    struct sd_switch_caps   sw_caps;   
};

struct mmc_command {
    u32 opcode;
    u32 arg;
    u32 rsptyp;
    u32 resp[4];
    u32 timeout;
    u32 retries;    /* max number of retries */
    u32 error;      /* command error */ 
};

struct mmc_data {
    u8  *buf;
    u32  blks;
    u32  timeout;   /* ms */
};

#define mmc_card_mmc(c)             ((c)->type & MMC_TYPE_MMC)
#define mmc_card_sd(c)              ((c)->type & MMC_TYPE_SD)
#define mmc_card_sdio(c)            ((c)->type & MMC_TYPE_SDIO)

#define mmc_card_set_host(c,h)      ((c)->host = (h))
#define mmc_card_set_unknown(c)     ((c)->type = MMC_TYPE_UNKNOWN)
#define mmc_card_set_mmc(c)         ((c)->type |= MMC_TYPE_MMC)
#define mmc_card_set_sd(c)          ((c)->type |= MMC_TYPE_SD)
#define mmc_card_set_sdio(c)        ((c)->type |= MMC_TYPE_SDIO)

#define mmc_card_present(c)         ((c)->state & MMC_STATE_PRESENT)
#define mmc_card_readonly(c)        ((c)->state & MMC_STATE_READONLY)
#define mmc_card_highspeed(c)       ((c)->state & MMC_STATE_HIGHSPEED)
#define mmc_card_uhs1(c)            ((c)->state & MMC_STATE_UHS1)
#define mmc_card_ddr(c)             ((c)->state & MMC_STATE_DDR)
#define mmc_card_blockaddr(c)       ((c)->state & MMC_STATE_BLOCKADDR)
#define mmc_card_highcaps(c)        ((c)->state & MMC_STATE_HIGHCAPS)

#define mmc_card_set_present(c)     ((c)->state |= MMC_STATE_PRESENT)
#define mmc_card_set_readonly(c)    ((c)->state |= MMC_STATE_READONLY)
#define mmc_card_set_highspeed(c)   ((c)->state |= MMC_STATE_HIGHSPEED)
#define mmc_card_set_uhs1(c)        ((c)->state |= MMC_STATE_UHS1)
#define mmc_card_set_ddr(c)         ((c)->state |= MMC_STATE_DDR)  
#define mmc_card_set_blockaddr(c)   ((c)->state |= MMC_STATE_BLOCKADDR)

#define mmc_card_name(c)            ((c)->cid.prod_name)
#define mmc_card_id(c)              ((c)->host->id)

typedef struct {
    u16 max_current;
    u16 grp6_info;
    u16 grp5_info;
    u16 grp4_info;
    u16 grp3_info;
    u16 grp2_info;
    u16 grp1_info;
    u8  grp6_result:4;
    u8  grp5_result:4;
    u8  grp4_result:4;
    u8  grp3_result:4;
    u8  grp2_result:4;
    u8  grp1_result:4;
    u8  ver;
    u16 grp6_busy;
    u16 grp5_busy;
    u16 grp4_busy;
    u16 grp3_busy;
    u16 grp2_busy;
    u16 grp1_busy;
    u8  rev[34];
} mmc_switch_t;

extern int mmc_init(int id);
extern struct mmc_host *mmc_get_host(int id);
extern struct mmc_card *mmc_get_card(int id);
extern int mmc_init_host(struct mmc_host *host, int id);
extern int mmc_init_card(struct mmc_host *host, struct mmc_card *card);
extern int mmc_set_blk_length(struct mmc_host *host, u32 blklen);
extern int mmc_set_bus_width(struct mmc_host *host, struct mmc_card *card, int width);
extern int mmc_card_avail(struct mmc_host *host);
extern int mmc_card_protected(struct mmc_host *host);
extern void mmc_set_clock(struct mmc_host *host, int ddr, u32 hz);
extern int mmc_block_read(int dev_num, unsigned long blknr, u32 blkcnt, unsigned long *dst);
extern int mmc_block_write(int dev_num, unsigned long blknr, u32 blkcnt, unsigned long *src);
extern int mmc_bread_boot(blkdev_t *bdev, u32 blknr, u32 blks, u8 *buf);
extern int mmc_bwrite_boot(blkdev_t *bdev, u32 blknr, u32 blks, u8 *buf);
extern int mmc_deselect_all_card(struct mmc_host *host);
extern int mmc_select_card(struct mmc_host *host, struct mmc_card *card);

extern int mmc_read_ext_csd(struct mmc_host *host, struct mmc_card *card);
extern int mmc_switch(struct mmc_host *host, struct mmc_card *card, u8 set, u8 index, u8 value);
extern int mmc_read(ulong src, uchar *dst, int size);
extern int mmc_write(uchar *src, ulong dst, int size);

extern int mmc_erase_start(struct mmc_card *card, u64 addr);
extern int mmc_erase_end(struct mmc_card *card, u64 addr);
extern int mmc_erase(struct mmc_card *card, u32 arg);

extern int mmc_io_rw_direct(struct mmc_card *card, int write, unsigned fn,	
    unsigned addr, u8 in, u8* out);
extern int mmc_io_rw_extended(struct mmc_card *card, int write, unsigned fn,
	unsigned addr, int incr_addr, u8 *buf, unsigned blocks, unsigned blksz);
extern int mmc_sdio_proc_pending_irqs(struct mmc_card *card);
extern int mmc_sdio_enable_irq_gap(struct mmc_card *card, int enable);

#ifdef __cplusplus
}
#endif

#endif /* MMC_CORE_H */

