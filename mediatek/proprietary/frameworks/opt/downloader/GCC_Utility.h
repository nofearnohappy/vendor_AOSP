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

#ifndef	_GCC_UTILITY_H_
#define	_GCC_UTILITY_H_

#if defined(__GNUC__)
#include <stdio.h>

#include <stdint.h>
#include <stdbool.h>

#include <unistd.h>
#define Sleep(n) usleep((n)*1000)

//typedef uint32_t DWORD;
//typedef uint32_t* LPDWORD;
typedef unsigned long DWORD;
typedef unsigned long* LPDWORD;
typedef char* LPSTR;
typedef unsigned char* LPBYTE;
typedef void* LPVOID;
typedef const void* LPCVOID;

typedef uint16_t WORD;
typedef unsigned int   UINT;
typedef unsigned char  BYTE;

typedef int HANDLE;

#undef NULL
#define NULL 0

#ifdef __cpulsplus
#include <cstdarg>
#endif

typedef __builtin_va_list __gnuc_va_list;
typedef __gnuc_va_list va_list;

#define _vsnprintf vsnprintf
#define stricmp strcasecmp
#define strnicmp strncasecmp

typedef struct _SYSTEMTIME {
  WORD wYear;
  WORD wMonth;
  WORD wDayOfWeek;
  WORD wDay;
  WORD wHour;
  WORD wMinute;
  WORD wSecond;
  WORD wMilliseconds;
} SYSTEMTIME, *PSYSTEMTIME;

DWORD GetCurrentProcessId();

DWORD GetCurrentThreadId();

#define ULONG unsigned long
ULONG GetTickCount();
ULONG GetTickCount2();


void GetLocalTime(PSYSTEMTIME p_systime);
void OutputDebugString(const char* c);
bool WriteFile(HANDLE hFile, LPCVOID buffer, DWORD nNumberOfBytesToWrite, LPDWORD lpNumberOfBytesWritten, void* dummyforAPI);
bool ReadFile(HANDLE hFile, LPVOID buffer, DWORD nNumberOfBytesToRead, LPDWORD lpNumberOfBytesRead, void* dummyforAPI);
bool ChangeBaudRate(HANDLE hCOM, DWORD  baudrate);


FILE *_fsopen (const char *fname, const char *mode, int shflag);
char *_strupr( char *str );

#endif

#endif
