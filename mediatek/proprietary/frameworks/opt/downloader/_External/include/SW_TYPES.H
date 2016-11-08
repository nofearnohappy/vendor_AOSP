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

#ifndef SW_TYPES_H
#define SW_TYPES_H


/*
 * general definitions
 */

typedef signed char    int8;
typedef signed short   int16;
typedef signed int    int32;
typedef signed int     intx;
typedef unsigned char  uint8;
typedef unsigned short uint16;
typedef unsigned int  uint32;
#ifdef WIN32
	typedef unsigned long uint64;
#else
	typedef unsigned long long uint64;
#endif
typedef unsigned int   uintx;
typedef unsigned char  U8;
typedef unsigned short U16;
typedef unsigned int  U32;
#ifdef _MSC_VER
    typedef unsigned __int64  U64;
#else
	typedef unsigned long long U64;
#endif
typedef int32          S32;
typedef unsigned int   Ux;
typedef unsigned char*  P_U8;
typedef unsigned short* P_U16;
typedef unsigned int*   P_U32;
typedef unsigned int*   P_Ux;
typedef unsigned long   ADDRESS_TYPE;


#ifndef __cplusplus
typedef unsigned char  bool;
#endif

/*
 * Definitions for BOOLEAN
 */

#define FALSE          0
#define TRUE           1

/*
 * Definitions for NULL
 */
#ifndef NULL
#define NULL           0
#endif

/*
 * For GFH library
 */
#if defined(_MSC_VER)
#define __WIN32_STDCALL   __stdcall
#else
#define __WIN32_STDCALL
#endif

#endif  /* SW_TYPES_H */
