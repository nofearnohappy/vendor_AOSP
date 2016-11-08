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

#ifndef _INTERFACE_H
#define _INTERFACE_H

#include <stdio.h>
#if defined(_MSC_VER)
    #include <windows.h>
#endif


//
// GSM manipulation
//
void gsm_reset();

#define TIMEOUT_5S  5000
#define TIMEOUT_10S 10000


//
// Communication
//
typedef enum
{
    COM_STATUS_DONE = 0,
    COM_STATUS_READ_TIMEOUT,
    COM_STATUS_WRITE_TIMEOUT,
    COM_STATUS_ERROR
} COM_STATUS;

typedef enum
{
    FLOW_CONTROL_DISABLED = 0,
    FLOW_CONTROL_HARDWARE
} FLOW_CONTROL;

#if defined(_MSC_VER)
    typedef HANDLE COM_HANDLE;
    #define INVALID_COM_HANDLE INVALID_HANDLE_VALUE
#elif defined(__GNUC__)
    //typedef void* COM_HANDLE;
    typedef int COM_HANDLE;
    #define INVALID_COM_HANDLE (NULL)
#else
    #error Please provide COM_HANLDE definition for your own platform
    //typedef void* COM_HANDLE;
    //#define INVALID_COM_HANDLE (NULL)
#endif

COM_STATUS com_open(COM_HANDLE *com_handle, unsigned int baudrate, unsigned int comPortNum);
COM_STATUS com_close(COM_HANDLE *com_handle);
COM_STATUS com_change_timeout(COM_HANDLE com_handle,
															unsigned int read_timeout_in_ms,
															unsigned int write_timeout_in_ms);
COM_STATUS com_enable_hardware_flow_control(COM_HANDLE com_handle);
COM_STATUS com_change_baudrate(COM_HANDLE com_handle, unsigned int baudrate);

COM_STATUS com_send_data(COM_HANDLE com_handle,
                         const unsigned char *data, unsigned int len);

COM_STATUS com_send_byte(COM_HANDLE com_handle, unsigned char data);
COM_STATUS com_send_word(COM_HANDLE com_handle, unsigned short data);
COM_STATUS com_send_dword(COM_HANDLE com_handle, unsigned int data);


COM_STATUS com_recv_data(COM_HANDLE com_handle,
                         unsigned char *data, unsigned int len);

COM_STATUS com_recv_data_chk_len(COM_HANDLE com_handle,
                         unsigned char *data, unsigned int len);

COM_STATUS com_recv_byte_without_retry(COM_HANDLE com_handle, unsigned char *data);
COM_STATUS com_recv_byte(COM_HANDLE com_handle, unsigned char *data);
COM_STATUS com_recv_word(COM_HANDLE com_handle, unsigned short *data);
COM_STATUS com_recv_dword(COM_HANDLE com_handle, unsigned int *data);


//
// Logging
//
void log_output(const char *format, ...);
void log_linux_errno (const char *func);

//
// Progress
//

extern char *g_property_name;
int set_progress(unsigned int finished_percentage);
int set_error_status(unsigned int status);




//
// Misc.
//
#if !defined(__GNUC__)
void sleep(unsigned int ms);
#endif

#endif // _INTERFACE_H
