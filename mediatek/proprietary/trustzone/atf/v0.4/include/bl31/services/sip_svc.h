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

#ifndef __SIP_SVC_H__
#define __SIP_SVC_H__

/*******************************************************************************
 * Defines for runtime services func ids
 ******************************************************************************/
/* SMC32 ID range from 0x82000000 to 0x82000FFF */
/* SMC64 ID range from 0xC2000000 to 0xC2000FFF */

/* For MTK SMC from Trustonic TEE */
/* 0x82000000 - 0x820000FF & 0xC2000000 - 0xC20000FF */
#define MTK_SIP_TBASE_HWUID_AARCH32       0x82000000

/* For MTK SMC from Boot-loader */
/* 0x82000100 - 0x820001FF & 0xC2000100 - 0xC20001FF */
#define MTK_SIP_BL_INIT_AARCH32           0x82000100
#define MTK_SIP_BL_INIT_AARCH64           0xC2000100

/* For MTK SMC from Kernel */
/* 0x82000200 - 0x820002FF & 0xC2000200 - 0xC20002FF */
#define MTK_SIP_KERNEL_TMP_AARCH32        0x82000200
#define MTK_SIP_KERNEL_TMP_AARCH64        0xC2000200
#define MTK_SIP_KERNEL_MCUSYS_WRITE_AARCH32 0x82000201
#define MTK_SIP_KERNEL_MCUSYS_WRITE_AARCH64 0xC2000201
#define MTK_SIP_KERNEL_MCUSYS_ACCESS_COUNT_AARCH32 0x82000202
#define MTK_SIP_KERNEL_MCUSYS_ACCESS_COUNT_AARCH64 0xC2000202
#define MTK_SIP_KERNEL_L2_SHARING_AARCH32  0x82000203
#define MTK_SIP_KERNEL_L2_SHARING_AARCH64  0xC2000203
#define MTK_SIP_KERNEL_WDT_AARCH32 0x82000204
#define MTK_SIP_KERNEL_WDT_AARCH64 0xC2000204
#define MTK_SIP_KERNEL_GIC_DUMP_AARCH32 0x82000205
#define MTK_SIP_KERNEL_GIC_DUMP_AARCH64 0xC2000205

/* For MTK SMC Reserved */
/* 0x82000300 - 0x82000FFF & 0xC2000300 - 0xC2000FFF */

/*
 * Number of SIP calls (above) implemented.
 */
#define MTK_SIP_SVC_NUM_CALLS             2

/*******************************************************************************
 * Defines for SIP Service queries
 ******************************************************************************/
/* 0x82000000 - 0x8200FEFF is SIP service calls */
#define MTK_SIP_SVC_CALL_COUNT      0x8200ff00
#define MTK_SIP_SVC_UID             0x8200ff01
/* 0x8200ff02 is reserved */
#define MTK_SIP_SVC_VERSION         0x8200ff03
/* 0x8200ff04 - 0x8200FFFF is reserved for future expansion */

/* MTK SIP Service Calls version numbers */
#define MTK_SIP_VERSION_MAJOR		0x0
#define MTK_SIP_VERSION_MINOR		0x1

/* The macros below are used to identify SIP calls from the SMC function ID */
/* SMC32 ID range from 0x82000000 to 0x82000FFF */
/* SMC64 ID range from 0xC2000000 to 0xC2000FFF */
#define SIP_FID_MASK                0xf000u
#define SIP_FID_VALUE               0u
#define is_sip_fid(_fid) \
	(((_fid) & SIP_FID_MASK) == SIP_FID_VALUE)

#endif /* __SIP_SVC_H__ */
