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

#ifndef _DA_STAGE_H
#define _DA_STAGE_H


#include "interface.h"
#include "_External/include/mtk_status.h"
#include "download_images.h"
#include "_External/include/DOWNLOAD.H"
#include "image.h"

#define PACKET_RE_TRANSMISSION_TIMES 3

typedef struct pt_resident
{
    unsigned char name[MAX_PARTITION_NAME_LEN];     /* partition name */
    U64 size;                          /* partition size */
    U64 offset;                        /* partition start */
    U64 mask_flags;                    /* partition flags */

} pt_resident; //store data by 64 bit

typedef struct pt_info
{
    int sequencenumber:8;
    int tool_or_sd_update:8;
    int mirror_pt_dl:4;   //mirror download OK
    int mirror_pt_has_space:4;
    int pt_changed:4;
    int pt_has_space:4;
} pt_info;

typedef struct _bmt_entry
{
    U16 bad_index;     // bad block index
    U16 mapped_index;  // mapping block index in the replace pool
}bmt_entry;


typedef enum{
    FEATURE_CHECK_WITH_ARM_BL = 0,
    FEATURE_CHECK_WITH_MAUI = 1,
    FEATURE_CHECK_WITH_EXT_BL = 2
}Feature_Check_Type;


typedef struct
{
    // NOR flash report
    STATUS_E            m_nor_ret;
    HW_ChipSelect_E     m_nor_chip_select[2];
    unsigned short      m_nor_flash_id;
    unsigned int        m_nor_flash_size;
    unsigned int        m_nor_flash_size_die1;
    unsigned short      m_nor_flash_dev_code_1;
    unsigned short      m_nor_flash_dev_code_2;
    unsigned short      m_nor_flash_dev_code_3;
    unsigned short      m_nor_flash_dev_code_4;
    STATUS_E            m_nor_flash_otp_status;
    unsigned int        m_nor_flash_otp_size;
    unsigned short      m_nor_flash_id_die2;
    unsigned int        m_nor_flash_size_die2;
    unsigned short      m_nor_flash_dev_code_1_die2;
    unsigned short      m_nor_flash_dev_code_2_die2;
    unsigned short      m_nor_flash_dev_code_3_die2;
    unsigned short      m_nor_flash_dev_code_4_die2;
    STATUS_E            m_nor_flash_otp_status_die2;
    unsigned int        m_nor_flash_otp_size_die2;


    // NAND flash report
    STATUS_E            m_nand_ret;
    HW_ChipSelect_E     m_nand_chip_select;
    unsigned short      m_nand_flash_id;
    unsigned int        m_nand_flash_size;
    unsigned short      m_nand_flash_dev_code_1;
    unsigned short      m_nand_flash_dev_code_2;
    unsigned short      m_nand_flash_dev_code_3;
    unsigned short      m_nand_flash_dev_code_4;
	unsigned short		m_nand_flash_dev_code_1_part2;
	unsigned short		m_nand_flash_dev_code_2_part2;
	unsigned short		m_nand_flash_dev_code_3_part2;
	unsigned short		m_nand_flash_dev_code_4_part2;
    unsigned short      m_nand_pagesize;
    unsigned short      m_nand_sparesize;
    unsigned short      m_nand_pages_per_block;
    unsigned char       m_nand_io_interface;
    unsigned char       m_nand_addr_cycle;

    // EMMC flash report
    STATUS_E            m_emmc_ret;
    unsigned char       m_emmc_manufacture_id;
    char                m_emmc_product_name[8];
    unsigned char       m_emmc_partitioned;
    unsigned int        m_emmc_boot1_size; // unit: 512 bytes
    unsigned int        m_emmc_boot2_size; // unit: 512 bytes
    unsigned int        m_emmc_rpmb_size;  // unit: 512 bytes
    unsigned int        m_emmc_gp1_size;   // unit: 512 bytes
    unsigned int        m_emmc_gp2_size;   // unit: 512 bytes
    unsigned int        m_emmc_gp3_size;   // unit: 512 bytes
    unsigned int        m_emmc_gp4_size;   // unit: 512 bytes
    unsigned int        m_emmc_ua_size;    // unit: 512 bytes

    // Internal RAM report
    STATUS_E            m_int_sram_ret;
    unsigned int        m_int_sram_size;

    // External RAM report
    STATUS_E            m_ext_ram_ret;
    HW_RAMType_E        m_ext_ram_type;
    HW_ChipSelect_E     m_ext_ram_chip_select;
    unsigned int        m_ext_ram_size;

} DA_REPORT_T;




STATUS_E da_connect(COM_HANDLE com_handle,
                    const struct image *nor_flash_table,
                    const struct image *nand_flash_table,
                    unsigned int bmt_address);

STATUS_E da_disconnect(COM_HANDLE com_handle);

STATUS_E da_EnableWatchDog(COM_HANDLE com_handle, unsigned short  ms_timeout_interval);

STATUS_E da_change_baudrate_phase1(COM_HANDLE com_handle);

STATUS_E da_change_baudrate_phase2(COM_HANDLE com_handle);


STATUS_E da_NAND_ReadBlock(COM_HANDLE com_handle,
						unsigned int startAddr,
						unsigned int blockSize,
						NUTL_ReadFlag_E flag,
						unsigned char *Buffer,
						unsigned int *buf_len);

STATUS_E da_FormatFAT(COM_HANDLE com_handle,
						NUTL_EraseFlag_E  flag,
						unsigned int  bValidation,
                         Range *nor_fat_range,
                         Range *nand_fat_range);

STATUS_E da_FormatFlash(COM_HANDLE com_handle,
						HW_StorageType_E  type,
						NUTL_EraseFlag_E  flag,
						unsigned int  bValidation,
						unsigned int address,
						unsigned int length);


STATUS_E da_write_boot_loader(COM_HANDLE com_handle,
                         Region_images *boot_region_images,
						 const int isNFB);

STATUS_E da_write_NFB_images(COM_HANDLE com_handle,
                             Region_images *main_region_images,
                             const unsigned int packet_length);

STATUS_E da_write_NOR_images(COM_HANDLE com_handle,
                         Region_images *main_region_images,
                         const unsigned int packet_length);



STATUS_E SV5_CMD_CheckBootLoaderFeature_CheckLoadType(COM_HANDLE com_handle,
                         const struct image *img, Feature_Check_Type checkType);
STATUS_E SV5_CMD_GetSettingOfCBR(COM_HANDLE com_handle, unsigned int *startBlockOfCBR,
                            unsigned int *numNormalBlock, unsigned int *numFastlBlock,
                            unsigned int *numSpareBlock, unsigned int *numMaxRecordInBlock,
                            unsigned int *numTotalBlock, unsigned char *bReadOnlyMode,
                            unsigned int *cbr_version);

STATUS_E SV5_CMD_FormatCBR(COM_HANDLE com_handle);
STATUS_E SV5_CMD_CreateCBR(COM_HANDLE com_handle, unsigned int numNormalBlock,
                            unsigned int numFastlBlock, unsigned int numMinSpareBlock,
                            unsigned int numMaxSpareBlock, unsigned int numMaxRecordInBlock,
                            unsigned char bReadOnlyMode,  unsigned int cbr_version);

STATUS_E SV5_CMD_SetMemBlock(COM_HANDLE com_handle,
                            Region_images *main_region_images,
                            unsigned char *get_num_of_unchanged_data_blocks);
STATUS_E SV5_CMD_ERASE_MAUI_INFO(COM_HANDLE com_handle);
STATUS_E SV5_CMD_WriteData(COM_HANDLE com_handle,
                            Region_images *main_region_images,
                            unsigned int packet_length,
                            unsigned char num_of_unchanged_data_blocks);



STATUS_E SV5_CMD_BACKUP_AND_ERASE_HB(COM_HANDLE com_handle);
STATUS_E SV5_CMD_RESTORE_HB(COM_HANDLE com_handle);
STATUS_E SV5_CMD_ReadBMT(COM_HANDLE com_handle, DA_REPORT_T *p_da_report);
STATUS_E SV5_CMD_NAND_DL_Partition_Data(COM_HANDLE com_handle,
                                const struct image *linux_images,
                                const unsigned int num_linux_images,
                                unsigned int packetLength);
STATUS_E SV5_CMD_NAND_DL_Encrypt_Partition_Data(COM_HANDLE com_handle,
                                const struct image *sec_img);

STATUS_E SV5_CMD_ReadPartitionInfo(COM_HANDLE com_handle,
                                 pt_resident* part,
                                 unsigned int * part_num,
                                 const unsigned int max_num);
STATUS_E SV5_CMD_WritePartitionInfo(COM_HANDLE com_handle,
                                 const struct image *linux_images,
                                 const unsigned int num_linux_images,
                                 pt_resident* new_part,
                                 const unsigned int max_num,
                                 int bIsUpdated);

STATUS_E SV5_FlashTool_ReadPartitionCount(COM_HANDLE com_handle, unsigned int* count);
STATUS_E SV5_FlashTool_CheckPartitionTable(COM_HANDLE com_handle,
                                    const struct image *linux_images,
                                    const unsigned int num_linux_images,
                                    pt_resident* new_part,
                                    pt_info* pi);




#endif  // _DA_STAGE_H
