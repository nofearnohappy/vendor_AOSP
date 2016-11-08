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

#ifndef _MTK_MCU_H_
#define _MTK_MCU_H_

typedef enum
{
    EXT_13M = 1,
    EXT_26M = 2,
    EXT_39M = 3,
    EXT_52M = 4,
    EXT_CLOCK_END,
    AUTO_DETECT_EXT_CLOCK = 254,
    UNKNOWN_EXT_CLOCK = 255
} EXT_CLOCK;

typedef enum
{
    MT6205      = 0,
    MT6205B     = 1,
    MT6218      = 2,
    MT6218B     = 4,
    MT6219      = 5,
    MT6217      = 6,
    MT6228      = 7,
    MT6227      = 8,
    MT6229      = 9,
    MT6226      = 10,
    MT6226M     = 11,
    MT6230      = 12,
    MT6225      = 13,
    MT6268T     = 14,
    MT6223      = 15,
    MT6227D     = 16,
    MT6226D     = 17,
    MT6223P     = 18,
    MT6238      = 19,
    MT6235      = 20,
    MT6235B     = 21,
    TK6516_MD   = 22,
    TK6516_AP   = 23,
    MT6268A     = 24,
    MT6516_MD   = 25,
    MT6516_AP   = 26,
    MT6239      = 27,
    MT6251T     = 28,
    MT6253T     = 29,
    MT6268B     = 30,

    MT6253      = 32,
    MT6253D     = 33,
    MT6236      = 34,

    MT6252      = 37,

    MT6921B      = 40,

    MT6276      = 128,
    MT6251      = 129,
    MT6256      = 130,
    MT6276M      = 131,
	MT6276W      = 132,
	MT6255       = 133,

    MT6250       = 136,
    MT6280       = 137,
    MT6260       = 139,

    MT6261_Dev   = 141,
    MT6261       = 142,
    MT2501       = 143,
    MT2502       = 144,


    BBCHIP_TYPE_END,
    AUTO_DETECT_BBCHIP = 254,
    UNKNOWN_BBCHIP_TYPE = 255
} BBCHIP_TYPE;

#endif
