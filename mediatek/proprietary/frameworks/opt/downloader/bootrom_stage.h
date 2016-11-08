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

#ifndef _BOOTROM_STAGE_H
#define _BOOTROM_STAGE_H

#include "interface.h"
#include "_External/include/mtk_status.h"

struct image;
struct ExternalMemorySetting;
struct EPP_PARAM;


STATUS_E bootrom_connect(COM_HANDLE com_handle);
STATUS_E bootrom_disable_watchdog(COM_HANDLE com_handle);
STATUS_E bootrom_read_platform_code(COM_HANDLE com_handle, unsigned short *platform_code);
STATUS_E bootrom_latch_powerkey(COM_HANDLE com_handle);
STATUS_E bootrom_SetRemap(COM_HANDLE com_handle, unsigned int mode);
STATUS_E bootrom_SendEPP(COM_HANDLE com_handle,
                const struct image *download_EPP,
                const struct ExternalMemorySetting *externalMemorySetting,
                const int isNFB);
STATUS_E bootrom_send_download_agent(COM_HANDLE com_handle,
                                     const struct image *download_agent);
STATUS_E bootrom_jump_to_download_agent(COM_HANDLE com_handle,
                                        const struct image *download_agent);


//STATUS_E bootloader_disable_watchdog_MT6280(COM_HANDLE com_handle);
//STATUS_E bootloader_latch_powerkey_MT6280(COM_HANDLE com_handle);
//STATUS_E bootloader_GetBootLoaderVer(COM_HANDLE com_handle);
#endif  // _BOOTROM_STAGE_H
