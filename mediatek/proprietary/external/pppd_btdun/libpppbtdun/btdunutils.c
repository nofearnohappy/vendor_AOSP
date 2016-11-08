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
 *
 * Filename:
 * ---------
 * btdunutils.c
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to provide utilities for btdun
 *
 * Author:
 * -------
 * Ting Zheng
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime:
 * $Log: 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
#if defined (__BTMTK__) && defined (__BT_DUN_PROFILE__)
#define LOG_TAG "pppbtdunutils"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

//#include "btdunutils.h"
#include "btdun.h"

#include "utils/Log.h"

typedef unsigned char bool;

typedef struct _GSList GSList;
typedef void* gpointer;
typedef const void *gconstpointer;

 /*
  * Significant octet values.
  */
#define	PPP_ALLSTATIONS	0xff	/* All-Stations broadcast address */
#define	PPP_UI		0x03	/* Unnumbered Information */
#define	PPP_FLAG	0x7e	/* Flag Sequence */
#define	PPP_ESCAPE	0x7d	/* Asynchronous Control Escape */
#define	PPP_TRANS	0x20	/* Asynchronous transparency modifier */

 /*
  * FCS lookup table as calculated by genfcstab.
  */
 static unsigned short fcstab[256] = {
	 0x0000, 0x1189, 0x2312, 0x329b, 0x4624, 0x57ad, 0x6536, 0x74bf,
	 0x8c48, 0x9dc1, 0xaf5a, 0xbed3, 0xca6c, 0xdbe5, 0xe97e, 0xf8f7,
	 0x1081, 0x0108, 0x3393, 0x221a, 0x56a5, 0x472c, 0x75b7, 0x643e,
	 0x9cc9, 0x8d40, 0xbfdb, 0xae52, 0xdaed, 0xcb64, 0xf9ff, 0xe876,
	 0x2102, 0x308b, 0x0210, 0x1399, 0x6726, 0x76af, 0x4434, 0x55bd,
	 0xad4a, 0xbcc3, 0x8e58, 0x9fd1, 0xeb6e, 0xfae7, 0xc87c, 0xd9f5,
	 0x3183, 0x200a, 0x1291, 0x0318, 0x77a7, 0x662e, 0x54b5, 0x453c,
	 0xbdcb, 0xac42, 0x9ed9, 0x8f50, 0xfbef, 0xea66, 0xd8fd, 0xc974,
	 0x4204, 0x538d, 0x6116, 0x709f, 0x0420, 0x15a9, 0x2732, 0x36bb,
	 0xce4c, 0xdfc5, 0xed5e, 0xfcd7, 0x8868, 0x99e1, 0xab7a, 0xbaf3,
	 0x5285, 0x430c, 0x7197, 0x601e, 0x14a1, 0x0528, 0x37b3, 0x263a,
	 0xdecd, 0xcf44, 0xfddf, 0xec56, 0x98e9, 0x8960, 0xbbfb, 0xaa72,
	 0x6306, 0x728f, 0x4014, 0x519d, 0x2522, 0x34ab, 0x0630, 0x17b9,
	 0xef4e, 0xfec7, 0xcc5c, 0xddd5, 0xa96a, 0xb8e3, 0x8a78, 0x9bf1,
	 0x7387, 0x620e, 0x5095, 0x411c, 0x35a3, 0x242a, 0x16b1, 0x0738,
	 0xffcf, 0xee46, 0xdcdd, 0xcd54, 0xb9eb, 0xa862, 0x9af9, 0x8b70,
	 0x8408, 0x9581, 0xa71a, 0xb693, 0xc22c, 0xd3a5, 0xe13e, 0xf0b7,
	 0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64, 0x5fed, 0x6d76, 0x7cff,
	 0x9489, 0x8500, 0xb79b, 0xa612, 0xd2ad, 0xc324, 0xf1bf, 0xe036,
	 0x18c1, 0x0948, 0x3bd3, 0x2a5a, 0x5ee5, 0x4f6c, 0x7df7, 0x6c7e,
	 0xa50a, 0xb483, 0x8618, 0x9791, 0xe32e, 0xf2a7, 0xc03c, 0xd1b5,
	 0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66, 0x7eef, 0x4c74, 0x5dfd,
	 0xb58b, 0xa402, 0x9699, 0x8710, 0xf3af, 0xe226, 0xd0bd, 0xc134,
	 0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7, 0x6e6e, 0x5cf5, 0x4d7c,
	 0xc60c, 0xd785, 0xe51e, 0xf497, 0x8028, 0x91a1, 0xa33a, 0xb2b3,
	 0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60, 0x1de9, 0x2f72, 0x3efb,
	 0xd68d, 0xc704, 0xf59f, 0xe416, 0x90a9, 0x8120, 0xb3bb, 0xa232,
	 0x5ac5, 0x4b4c, 0x79d7, 0x685e, 0x1ce1, 0x0d68, 0x3ff3, 0x2e7a,
	 0xe70e, 0xf687, 0xc41c, 0xd595, 0xa12a, 0xb0a3, 0x8238, 0x93b1,
	 0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62, 0x3ceb, 0x0e70, 0x1ff9,
	 0xf78f, 0xe606, 0xd49d, 0xc514, 0xb1ab, 0xa022, 0x92b9, 0x8330,
	 0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3, 0x2c6a, 0x1ef1, 0x0f78
 };
 
 /*
  * Values for FCS calculations.
  */
#define PPP_INITFCS	0xffff	/* Initial FCS value */
#define PPP_GOODFCS	0xf0b8	/* Good final FCS value */
#define PPP_FCS(fcs, c)	(((fcs) >> 8) ^ fcstab[((fcs) ^ (c)) & 0xff])


/*
 * Procedure to encode the data for async serial transmission.
 * Does octet stuffing (escaping), puts the address/control bytes
 * on if A/C compression is disabled, and does protocol compression.
 */
#define PUT_BYTE(buf, c, islcp, len)	do {		\
	 if (islcp && c < 0x20) {\
		 *buf++ = PPP_ESCAPE;			 \
		 *buf++ = c ^ 0x20; 		 \
		 len += 2;					 \
	 } else if (c == PPP_FLAG) { \
		 *buf++ = PPP_ESCAPE;		 \
		 *buf++ = 0x5e; 		 \
		 len += 2;					 \
	 } else if (c == PPP_ESCAPE) {	 \
		 *buf++ = PPP_ESCAPE;		 \
		 *buf++ = 0x5d; 		 \
		 len += 2;					 \
	 }else {		 \
		 *buf++ = c;			 \
		 len += 1;					 \
	 }							 \
 } while (0)

#define GETSHORT(s, cp) { \
		 (s) = *(cp)++ << 8; \
		 (s) |= *(cp)++; \
	 }


/* list management */
struct _GSList
{
    gpointer data;
    GSList *next;
};

#define  g_slist_next(slist)	         ((slist) ? (((GSList *)(slist))->next) : NULL)
	 

int g_slist_length (GSList *list)
{
  int length;

  length = 0;
  while (list)
    {
      length++;
      list = list->next;
    }

  return length;
}

GSList* g_slist_last (GSList *list)
{
  if (list)
    {
      while (list->next)
	list = list->next;
    }

  return list;
}

GSList* g_slist_append (GSList   *list,
		gpointer  data)
{
  GSList *new_list;
  GSList *last;

  new_list = malloc(sizeof(GSList));
  new_list->data = data;
  new_list->next = NULL;

  if (list)
    {
      last = g_slist_last (list);
      /* g_assert (last != NULL); */
      last->next = new_list;

      return list;
    }
  else
    return new_list;
}

GSList* g_slist_remove (GSList        *list,
		gconstpointer  data)
{
  GSList *tmp, *prev = NULL;

  tmp = list;
  while (tmp)
    {
      if (tmp->data == data)
	{
	  if (prev)
	    prev->next = tmp->next;
	  else
	    list = tmp->next;

	  free(tmp);
	  break;
	}
      prev = tmp;
      tmp = prev->next;
    }

  return list;
}

GSList *packetList = NULL;

struct packet
{
    int length;
    unsigned char data[0];	
};

/********************************************************************
 *
 * pending_packet_list_len - get the length of pending packet list.
 */

int pending_packet_list_len(void)
{
    return g_slist_length(packetList);
}

/********************************************************************
 *
 * read_pending_packet - get a PPP packet from the pending packet list.
 */

int read_pending_packet(unsigned char **p)
{
    //GSList *l = g_slist_next(packetList);
    struct packet *pending_packet = packetList->data;

    *p = pending_packet->data;
    return pending_packet->length;
}

/********************************************************************
 *
 * loop_packet - append a PPP packet to the pending packet list.
 */

int loop_packet(unsigned char *data, int len)
{
    struct packet *pending_packet;

    pending_packet = malloc(sizeof(struct packet) + len);	
    if (pending_packet == NULL)
    {
        return 0;
    }
    pending_packet->length = len;
    memcpy(((unsigned char *)pending_packet) + sizeof(struct packet), data, len);

    packetList = g_slist_append(packetList, pending_packet);	
    return 1;
}

/********************************************************************
 *
 * remove_pending_packet - remove the first PPP pending packet from the list.
 */

void remove_pending_packet(void)
{
    //GSList *l = g_slist_next(packetList);
    struct packet *pending_packet = packetList->data;

    packetList = g_slist_remove(packetList, pending_packet);
    free(pending_packet);
}

char *frame;
int framelen;
int framemax;
int escape_flag;
int flush_flag;
int fcs;

void pppbt_conf(void) {
    framemax = PPP_MRU;
    framemax += PPP_HDRLEN + PPP_FCSLEN;
    frame = malloc(framemax);
    framelen = 0;
    //pend_q = NULL;
    escape_flag = 0;
    flush_flag = 0;
    fcs = PPP_INITFCS;
}

int pppbt_decode_packet(p, n)
unsigned char *p;
int n;
{
#if 1
    int c, rv;

    rv = 0;
    for (; n > 0; --n) {
        c = *p++;
        if (c == PPP_FLAG) {
            if (!escape_flag && !flush_flag
            && framelen > 2 && fcs == PPP_GOODFCS) {
            framelen -= 2;
            if (loop_packet((unsigned char *)frame, framelen))
                rv = 1;
            }
            framelen = 0;
            flush_flag = 0;
            escape_flag = 0;
            fcs = PPP_INITFCS;
            continue;
        }
        if (flush_flag)
            continue;
        if (escape_flag) {
            c ^= PPP_TRANS;
            escape_flag = 0;
        } else if (c == PPP_ESCAPE) {
            escape_flag = 1;
            continue;
        }
        if (framelen >= framemax) {
            flush_flag = 1;
            continue;
        }
        frame[framelen++] = c;
        fcs = PPP_FCS(fcs, c);
    }
    return rv;

#else
    int c;
    int rv = 0;
    int escape_flag = 0;
    int flush_flag = 0;
    int framelen = 0;
    int fcs = PPP_INITFCS;
    unsigned char *tmpbuf;
    unsigned char *pbuf;
    
    tmpbuf = (unsigned char *)malloc(n);
    if (tmpbuf ==NULL)
        return -1;
    
    ALOGI("pppbt decode packet, len: %d", n);
    dbglog("pppbt decode packet, len: %d\n", n);
    
    memcpy(tmpbuf, p, n);	 
    pbuf = tmpbuf;
    
    for (; n > 0; --n) {
        c = *pbuf++;
        //ALOGI("pppbt decode packet: %x, fcs: %x, n: %d", c, fcs, n);
        if (c == PPP_FLAG) {
            if (!escape_flag && !flush_flag
            && framelen > 2 && fcs == PPP_GOODFCS) {
                ALOGI("pppbt decode packet: %d", framelen);
                framelen -= 2;
                loop_packet(p, framelen);
            }
            framelen = 0;
            flush_flag = 0;
            escape_flag = 0;
            fcs = PPP_INITFCS;
            continue;
        }
        if (flush_flag)
            continue;
        if (escape_flag) {
            if (c == 0x5e)
                c = 0x7e;
            else if (c == 0x5d)
                c = 0x7d;
            else		  
                c ^= PPP_TRANS;
            escape_flag = 0;
        } else if (c == PPP_ESCAPE) {
            escape_flag = 1;
            continue;
        }
        if (framelen >= PPP_MRU + PPP_HDRLEN + PPP_FCSLEN) {
            flush_flag = 1;
            continue;
        }
        p[framelen++] = c;
        fcs = PPP_FCS(fcs, c);
    }
    
    //ALOGI("pppbt decode packet, out len: %d", framelen);
    free(tmpbuf);
    
    //if (rv == 0)
    //    return -1;
    
    return framelen;
#endif
}

int pppbt_encode_packet(p, n)
unsigned char *p;
int n;
{
    int fcs;
    int i = 0;
    int framelen = 0;
    int c;
    int proto;
    unsigned char *tmpbuf;
    unsigned char *pbuf;
    bool islcp;
    
    pbuf = p;
    
    if (pbuf[0] == PPP_ALLSTATIONS && pbuf[1] == PPP_UI)
    {
        pbuf += 2;
    }
    GETSHORT(proto, pbuf);
    
    
    /*
    * LCP packets with code values between 1 (configure-reqest)
    * and 7 (code-reject) must be sent as though no options
    * had been negotiated.
    */
    islcp = proto == PPP_LCP && 1 <= *pbuf && *pbuf <= 7;
    ALOGI("pppbt encode packet, proto: %x, %d", proto, islcp);
    
    tmpbuf = (unsigned char *)malloc(n);
    if (tmpbuf ==NULL)
        return -1;
    
    ALOGI("pppbt encode packet, len: %d", n);
    
    memcpy(tmpbuf, p, n);	 
    
    /*
    * Start of a new packet - insert the leading FLAG
    * character.
    */
    *p++ = PPP_FLAG;
    framelen += 1;
    fcs = PPP_INITFCS;
    
    
    /*
    * Once we put in the last byte, we need to put in the FCS
    * and closing flag, so make sure there is at least 7 bytes
    * of free space in the output buffer.
    */
    while (i < n) {
        c = tmpbuf[i++];
        fcs = PPP_FCS(fcs, c);
        PUT_BYTE(p, c, islcp, framelen);
    }
    
    /*
    * We have finished the packet.  Add the FCS and flag.
    */
    fcs = ~fcs;
    c = fcs & 0xff;
    PUT_BYTE(p, c, islcp, framelen);
    c = (fcs >> 8) & 0xff;
    PUT_BYTE(p, c, islcp, framelen);
    *p = PPP_FLAG;
    framelen += 1;
    
    ALOGI("pppbt encode packet, out len: %d", framelen);
    
    free(tmpbuf);
    return framelen;
}

#endif
