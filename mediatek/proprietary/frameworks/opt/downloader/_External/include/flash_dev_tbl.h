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

#ifndef _FLASH_DEV_TBL_H_
#define _FLASH_DEV_TBL_H_

#include "SW_TYPES.H"
#include "DOWNLOAD.H"

#pragma pack(push) /* push current alignment to stack */
#pragma pack(4) /* set alignment to 1 byte boundary */

#define MAX_SECTOR_REGION_PER_DIE	5
#define MAX_STATUS_CHECK_RETRY		5
#define MAX_OTP_REGION_PER_DIE		2

// flash command callback function 
typedef bool		(*CB_NOR_CHECK_DEV_ID)(volatile uint16 *die1_addr, volatile uint16 *die2_addr);
typedef bool		(*CB_NOR_CHECK_DEV_IDLE)(const uint32 addr);
typedef void		(*CB_NOR_SECTOR_ERASE)(const uint32 blockaddr);
typedef STATUS_E	(*CB_NOR_SECTOR_ERASE_CHECK_DONE)(const uint32 blockaddr);
typedef void		(*CB_NOR_PROGRAM_PRE_PROCESS)(void);
typedef void		(*CB_NOR_PROGRAM_POST_PROCESS)(void);
typedef void		(*CB_NOR_PROGRAM_ENTER)(const uint32 blockaddr);
typedef void		(*CB_NOR_PROGRAM_EXIT)(const uint32 blockaddr);
typedef void		(*CB_NOR_PROGRAM)(const uint32 blockaddr, const uint32 prog_addr, const uint16 data);
typedef STATUS_E	(*CB_NOR_PROGRAM_CHECK_DONE)(const uint32 prog_addr);
typedef void		(*CB_NOR_BUF_PROGRAM)(const uint32 blockaddr, const uint32 prog_addr, const uint16 *data, const uint32 length_in_word);
typedef STATUS_E	(*CB_NOR_BUF_PROGRAM_CHECK_DONE)(const uint32 prog_addr);

typedef struct {
	NOR_CMD_Callback_ID_E			m_cb_gid;
	CB_NOR_CHECK_DEV_ID				m_cb_chk_dev_id;
	CB_NOR_CHECK_DEV_IDLE			m_cb_chk_dev_idle;
	CB_NOR_SECTOR_ERASE				m_cb_sec_erase;
	CB_NOR_SECTOR_ERASE_CHECK_DONE	m_cb_sec_erase_chk_done;
	CB_NOR_PROGRAM_PRE_PROCESS		m_cb_pgm_pre_process;
	CB_NOR_PROGRAM_POST_PROCESS		m_cb_pgm_post_process;
	CB_NOR_PROGRAM_ENTER			m_cb_pgm_enter;
	CB_NOR_PROGRAM_EXIT				m_cb_pgm_exit;
	CB_NOR_PROGRAM					m_cb_pgm;
	CB_NOR_PROGRAM_CHECK_DONE		m_cb_pgm_check_done;
	CB_NOR_BUF_PROGRAM				m_cb_buf_pgm;
	CB_NOR_BUF_PROGRAM_CHECK_DONE	m_cb_buf_pgm_check_done;
} NOR_CMD_Callback_S;

//OTP_Drivers
typedef void		(*CB_NOR_OTP_ENTER)(const uint32 blockaddr);
typedef void		(*CB_NOR_OTP_READ)(const uint32 blockaddr, const uint32 prog_addr, volatile uint16 * data);
typedef void		(*CB_NOR_OTP_PROGRAM)(const uint32 blockaddr, const uint32 prog_addr, const uint16 data);
typedef void		(*CB_NOR_OTP_LOCK)(const uint32 blockaddr, const uint32 prog_addr, const uint16 data);
typedef STATUS_E	(*CB_NOR_OTP_LOCK_CHECKSTATUS)(const uint32 blockaddr, const uint32 prog_addr, const uint16 data);
typedef void		(*CB_NOR_OTP_EXIT)(const uint32 blockaddr);

typedef struct {
	CB_NOR_OTP_ENTER				m_cb_otp_enter;
	CB_NOR_OTP_READ					m_cb_otp_read;
	CB_NOR_OTP_PROGRAM				m_cb_otp_pgm;
	CB_NOR_OTP_LOCK					m_cb_otp_lock;
	CB_NOR_OTP_LOCK_CHECKSTATUS		m_cb_otp_lock_checkstatus;
	CB_NOR_OTP_EXIT					m_cb_otp_exit;
} NOR_OTP_CMD_Callback_S;

typedef struct {
	// sector layout 
	uint32		m_base_offset;
	uint32		m_sector_count;
	uint32		m_sector_size;
} NOR_SectorRegion_S;

typedef struct {

	// total size (in bytes) on this die 
	uint32		m_size;

	// grouped sector region map 
	NOR_SectorRegion_S	m_region_map[MAX_SECTOR_REGION_PER_DIE];

} NOR_Die_Layout_S;

typedef struct {

	// total size (in bytes) on this die 
	uint32		m_size;

	// grouped sector region map 
	NOR_SectorRegion_S	m_region_map[MAX_OTP_REGION_PER_DIE];

} NOR_OTP_Layout_S;

typedef struct {

	// memory layout map for each die 
	uint32          m_die_layout; // const NOR_Die_Layout_S*

} NOR_Die_Info_S;

typedef struct {

	// die info
	NOR_Die_Info_S	m_die[MAX_DIE_IN_MCP];

	//OTP layout
	uint32          m_otp_layout; // const NOR_OTP_Layout_S*

	// die count 
	uint16		m_die_count;

	// H/W flash manufacture id and device code 
	uint16		m_manufacture_code;
	uint16		m_dev_code;
	uint16		m_ext_dev_code1;
	uint16		m_ext_dev_code2;

} NOR_HW_Info_S;

typedef struct {

	// flash id defined in DOWNLOAD.H 
	NOR_DeviceID_E			m_device_id;

	// flash device H/W info 
	NOR_HW_Info_S		m_hw_info;

	// flash command callback function 
	uint32                  m_cmd; //const NOR_CMD_Callback_S*

	uint32                  m_otp_cmd; //const NOR_OTP_CMD_Callback_S*

} NOR_Device_S;

#pragma pack(pop) /* restore original alignment from stack */

#ifdef __cplusplus
extern "C" {
#endif

extern const NOR_Device_S 	g_FlashDevTbl_Internal[];
extern 		 NOR_Device_S 	*g_FlashDevTbl;
extern		 NOR_Device_S   g_FlashDev;
extern uint16				g_FlashType;

#ifdef __cplusplus
}
#endif

#endif
