/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*******************************************************************************
* LEGAL DISCLAIMER

* Copyright (c) 2008 MediaTek Inc.  ALL RIGHTS RESERVED.

* BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES 
* AND AGREES THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK 
* SOFTWARE")RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE 
* PROVIDED TO BUYER ON AN "AS IS" BASIS ONLY.  MEDIATEK EXPRESSLY 
* DISCLAIMS ANY AND ALL WARRANTIES, WHETHER EXPRESS OR IMPLIED, INCLUDING 
* BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
* PARTICULAR PURPOSE, OR NON-INFRINGEMENT. NOR DOES MEDIATEK PROVIDE 
* ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTIES 
* WHICH MAY BE USED BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE.  
* BUYER AGREES TO LOOK ONLY TO SUCH THIRD PARTIES FOR ANY AND ALL 
* WARRANTY CLAIMS RELATING THERETO. MEDIATEK SHALL NOT BE RESPONSIBLE FOR 
* ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION OR CONFORMING 
* TO A PARTICULAR STANDARD OR OPEN FORUM.

* BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE 
* LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER SHALL BE, 
* AT MEDIATEK'S SOLE OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGES PAID BY BUYER TO 
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE. 

* THE MEDIATEK SOFTWARE IS PROVIDED FOR AND ONLY FOR USE WITH MEDIATEK CHIPS 
* OR PRODUCTS.  EXCEPT AS EXPRESSLY PROVIDED, NO LICENSE IS GRANTED BY 
* IMPLICATION OR OTHERWISE UNDER ANY INTELLECTUAL PROPERTY RIGHTS, INCLUDING 
* PATENT OR COPYRIGHTS, OF MEDIATEK.  UNAUTHORIZED USE, REPRODUCTION, OR 
* DISCLOSURE OF THE MEDIATEK SOFTWARE IN WHOLE OR IN PART IS STRICTLY PROHIBITED.

* THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH 
* THE LAWS OF THE REPUBLIC OF SINGAPORE, EXCLUDING ITS CONFLICT OF LAWS 
* PRINCIPLES. ANY DISPUTES, CONTROVERSIES OR CLAIMS RELATING HERETO OR ARISING 
* HEREFROM SHALL BE EXCLUSIVELY SETTLED VIA ARBITRATION IN SINGAPORE UNDER THE 
* THEN CURRENT ARBITRAL RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE.  
* THE LANGUAGE OF ARBITRATION SHALL BE ENGLISH. THE AWARDS OF THE ARBITRATION 
* SHALL BE FINAL AND BINDING UPON BOTH PARTIES AND SHALL BE ENTERED AND 
* ENFORCEABLE IN ANY COURT OF COMPETENT JURISDICTION.
********************************************************************************
*/

#ifndef _INTERFACE_INOUT_H_
#define _INTERFACE_INOUT_H_

#include "../../src/utils/eloop.h"
#ifdef  __cplusplus
extern "C" {
#endif

typedef enum
{
	AUTH_TYPE_NONE_WAPI = 0,	/*no WAPI	*/
	AUTH_TYPE_WAPI,			/*Certificate*/
	AUTH_TYPE_WAPI_PSK,		/*Pre-PSK*/
}wapi_auth_t;

typedef enum
{
	KEY_TYPE_ASCII = 0,			/*ascii		*/
	KEY_TYPE_HEX,				/*HEX*/
}wapi_key_t;

typedef struct
{
	wapi_auth_t authType;		/*Authentication type*/
	union
	{
		struct
		{
			wapi_key_t kt;				/*Key type*/
			unsigned int  kl;			/*key length*/
			unsigned char kv[128];/*value*/
		} ;
		struct
		{
			unsigned char as[2048];	/*ASU Certificate*/
			unsigned char user[2048];/*User Certificate*/
		} ;
	}para;
}wapi_param_t;


typedef enum
{
	CONN_ASSOC = 0,
	CONN_DISASSOC,
}CONN_STATUS;
typedef struct
{
	unsigned char v[6];
	unsigned char pad[2];
}MAC_ADDRESS;

struct wapi_cb_ctx {
	void *ctx; /* pointer to arbitrary upper level context */

	/*face to mt592x*/
	int (*msg_send)(void *priv, const u8 *msg_in, int msg_in_len, 
							 u8 *msg_out, int *msg_out_len);

	/*for testing*/
	void (*wapi_printf)(int level, const char *format, ...);

	/*send output to wpa_ctrl*/
	void (*wpa_msg)(void *ctx, int level, const char *fmt, ...);

	/*set wapi key*/
	int (*set_key)(void *ctx, int alg,
		       const u8 *addr, int key_idx, int set_tx,
		       const u8 *seq, size_t seq_len,
		       const u8 *key, size_t key_len);

	/*send WAI frame*/
	int (*ether_send)(void *ctx, const u8* pbuf, int length);

	/*set wpa_supplicant state*/
	void (*set_state)(void *ctx, int state);

	/*get wpa_supplicant state*/
	int (*get_state)(void *ctx);

	/*send deauthenticateion*/
	void (*deauthenticate)(void *ctx, int reason_code);

	/*set one-shot timer*/
	int  (*set_timer)(unsigned int secs, unsigned int usecs,
			   eloop_timeout_handler handler,
			   void *eloop_data, void *user_data);

	/*clear one-shot timer*/
	int  (*cancel_timer)(eloop_timeout_handler handler,
			 void *eloop_data, void *user_data);
};

int  wapi_lib_init(struct wapi_cb_ctx* ctx);
int  wapi_lib_exit();
int  wapi_set_user(const wapi_param_t* pPar);
void wapi_set_msg(CONN_STATUS action, const MAC_ADDRESS* pBSSID, const MAC_ADDRESS* pLocalMAC, unsigned char *assoc_ie, unsigned char assoc_ie_len);
int  wapi_set_rx_wai(const u8* pbuf, int length);
int  wapi_get_state(void);

#ifdef  __cplusplus
}
#endif

#endif /* _INTERFACE_INOUT_H_  */

