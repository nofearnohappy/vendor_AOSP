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

#ifndef _ROM_INFO_INTERNAL_H_
#define _ROM_INFO_INTERNAL_H_

typedef enum {
	 MTK_ROM_INFO_VER_00 = 0
	,MTK_ROM_INFO_VER_01
	,MTK_ROM_INFO_VER_02
	,MTK_ROM_INFO_VER_03
	,MTK_ROM_INFO_VER_04
	,MTK_ROM_INFO_VER_05
	,MTK_ROM_INFO_VER_06
	,MTK_ROM_INFO_VER_07
} MTK_ROM_INFO_VER;

#define NEW_ROM_INFO_BEGIN			"MTK_ROMINFO_v"
#define NEW_ROM_INFO_BEGIN_LEN		13
#define NEW_ROM_INFO_LOCATE_RANGE	0x100000

#define MAX_ROM_PLATFORM_ID_LENGTH		64
#define MAX_ROM_PROJECT_ID_LENGTH		64

typedef struct {
	unsigned short		m_manu_id;
	unsigned short		m_dev_id;
	unsigned short		m_ext_dev_id1;
	unsigned short		m_ext_dev_id2;
	unsigned int		m_fat_begin_addr;
	unsigned int		m_fat_length;
} FlashDevInfo_ST;


typedef struct {
	char				m_identifier[16];
	char				m_platform_id[128];
	char				m_project_id[64];
	unsigned short		m_nfb_identifier;
	unsigned short		m_flash_dev_cnt;
	FlashDevInfo_ST		m_flash_info[6];
	unsigned int		m_sb_crc;
	unsigned int		m_sb_addr;
	unsigned int		m_sb_length;
} MTK_ROMInfo_v04_ST;

typedef struct {
	char				m_identifier[16];
	char				m_platform_id[128];
	char				m_project_id[64];
	unsigned short		m_nfb_identifier;
	unsigned short		m_flash_dev_cnt;
	FlashDevInfo_ST		m_flash_info[6];
	unsigned int		m_sb_crc;
	unsigned int		m_sb_addr;
	unsigned int		m_sb_length;
//----------------------------------------	
	unsigned int		m_cust_para_addr;
	unsigned int		m_cust_para_len;
	unsigned int		m_bit_ctrl;
} MTK_ROMInfo_v05_ST;

typedef struct {
	char				m_identifier[16];
	char				m_platform_id[128];
	char				m_project_id[64];
	unsigned short		m_nfb_identifier;
	unsigned short		m_flash_dev_cnt;
	FlashDevInfo_ST		m_flash_info[6];
	unsigned int		m_sb_crc;
	unsigned int		m_sb_addr;
	unsigned int		m_sb_length;
	unsigned int		m_cust_para_addr;
	unsigned int		m_cust_para_len;
	unsigned int		m_bit_ctrl;
//----------------------------------------	
	char          		m_nand_fdm_dal_ver[8];
} MTK_ROMInfo_v06_ST;

//
// ROM INFO v7, start from 08B/Maintrunk w0909
//
typedef struct {
	char				m_identifier[16];
	char				m_platform_id[128];
	char				m_project_id[64];
	unsigned short		m_nfb_identifier;
	unsigned short		m_flash_dev_cnt;
	FlashDevInfo_ST		m_flash_info[6];
	unsigned int		m_sb_crc;
	unsigned int		m_sb_addr;
	unsigned int		m_sb_length;
	unsigned int		m_cust_para_addr;
	unsigned int		m_cust_para_len;
	unsigned int		m_bit_ctrl;
	char          		m_nand_fdm_dal_ver[8];
//----------------------------------------	
	unsigned int        m_extsram_size;
	unsigned int        m_bl_maui_paired_ver;     //Allow update MAUI only when matched with BL
	unsigned int        m_feature_combination;    //Increase m_bl_maui_paired_ver if supported feature increases	
} MTK_ROMInfo_v07_ST;

typedef union {
	MTK_ROMInfo_v04_ST	m_v04;
	MTK_ROMInfo_v05_ST	m_v05;
	MTK_ROMInfo_v06_ST	m_v06;
	MTK_ROMInfo_v07_ST	m_v07;
} MTK_ROMInfo_U;


//--------------------------------------------------------------
// BOOT LOADER Related INFO
//--------------------------------------------------------------

/* BOOTLOADER INFO */
#define MTK_BLOADER_INFO_BEGIN	"MTK_BLOADER_INFO_v"

typedef enum {
	 MTK_BLOADER_INFO_VER_UNKNOWN = 0
	,MTK_BLOADER_INFO_VER_01
	,MTK_BLOADER_INFO_VER_02
	,MTK_BLOADER_INFO_VER_03
} MTK_BLOADER_INFO_VER;

typedef struct {
	char			m_identifier[24];
	char			m_filename[64];
	char			m_version[4];
	unsigned int	m_chksum_seed;
	unsigned int	m_start_addr;
	char			m_bin_identifier[8];
} BLoaderInfo_v01_ST;

typedef struct {
	char			m_identifier[24];
	char			m_filename[64];
	char			m_version[4];
	unsigned int	m_chksum_seed;
	unsigned int	m_start_addr;
	unsigned int	m_emi_gen_a;
	unsigned int	m_emi_gen_b;
	unsigned int	m_emi_con_i;
	unsigned int	m_emi_con_i_ext;
	unsigned int	m_emi_con_k;
	unsigned int	m_emi_con_l;
	char			m_bin_identifier[8];
} BLoaderInfo_v02_ST;

typedef struct {
	char			m_identifier[24];
	char			m_filename[64];
	char			m_version[4];
	unsigned int	m_chksum_seed;
	unsigned int	m_start_addr;
	unsigned int	m_emi_gen_a;
	unsigned int	m_emi_gen_b;
	unsigned int	m_emi_gen_c;
	unsigned int	m_emi_con_i;
	unsigned int	m_emi_con_i_ext;
	unsigned int	m_emi_con_k;
	unsigned int	m_emi_con_l;
	char			m_bin_identifier[8];
} BLoaderInfo_v03_ST;

typedef union {
	BLoaderInfo_v01_ST	m_v01;
	BLoaderInfo_v02_ST	m_v02;
	BLoaderInfo_v03_ST	m_v03;
} BLoaderInfo_U;

/* EXT BOOTLOADER INFO */
#define EXT_BLOADER_INFO_ID			"MTK_EBL_INFO_v"
#define EXT_BLOADER_INFO_ID_LEN		14

typedef enum {
	 MTK_EXT_BLOADER_INFO_VER_UNKNOWN = 0
	,MTK_EXT_BLOADER_INFO_VER_01
} MTK_EXT_BLOADER_INFO_VER;

typedef struct {
	char			m_identifier[16];
	unsigned int	m_bl_maui_paired_ver;
	unsigned int	m_feature_combination;
	unsigned char	m_fdm_major_ver;
	unsigned char	m_fdm_minor_ver1;
	unsigned char	m_fdm_minor_ver2;
	unsigned char	m_dal_ver;

	unsigned int	m_reserved[8];

} ExtBLoaderInfo_v01_ST;

typedef union {
	ExtBLoaderInfo_v01_ST	m_v01;
} ExtBLoaderInfo_U;

#endif
