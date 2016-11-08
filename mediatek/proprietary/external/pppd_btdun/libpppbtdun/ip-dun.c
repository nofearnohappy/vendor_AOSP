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

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2005
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE. 
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/
/*******************************************************************************
 *
 * Filename:
 * ---------
 * ip-dun.c
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to provide ip transmission with external network
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <sys/ioctl.h>
#include <sys/file.h>
#include <sys/socket.h>
#include <linux/if.h>
#include <linux/in.h>
#include <linux/if_tun.h>
#include <sys/endian.h>

#include "btdun.h"
#include "utils/Log.h"

#define DUN_TUN_NAME     "bt-dun"

#define BTNETWORK_DEV_NETMASK		0xffffff00	//255.255.255.0

#define SIN_ADDR(x)	(((struct sockaddr_in *) (&(x)))->sin_addr.s_addr)
#define SIN_FAMILY(x)	(((struct sockaddr_in *) (&(x)))->sin_family)

int btnfd = -1;
int pppbtfd = -1;
int bt_dun_unit = 0;


pthread_t rcv_thread;
pthread_mutex_t thread_mutex;

extern int pppbt_encode_packet(unsigned char *p, int n);

static void *ip_receive_packet(void *usr_data);
static void ip_output(unsigned char *p, int len);

#if 0
static int ip_dun_config_netdev(int unit, int ipaddr)
{
    struct ifreq ifr;
    int fd;	
    int ret = -1;	

    fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd <= 0)
    {
        ALOGE("ip_dun_config_netdev create socket failed,  %s(%d)",  strerror(errno), errno);    
        return -1;
    }
    
    memset(&ifr, 0, sizeof(ifr));
    sprintf(ifr.ifr_name, "btn%d", unit);	

    SIN_FAMILY(ifr.ifr_addr) = AF_INET;
    SIN_ADDR(ifr.ifr_addr) = htonl(ipaddr);

    if (ioctl(fd, SIOCSIFADDR, &ifr) < 0)
    {
        ALOGE("ip_dun_config_netdev set ip address failed,  %s(%d)",  strerror(errno), errno);    
        goto exit;
    }

    ifr.ifr_flags = IFF_UP;
    if (ioctl(fd, SIOCSIFFLAGS, &ifr) < 0)
    {
        ALOGE("ip_dun_config_netdev up interface failed,  %s(%d)",  strerror(errno), errno);    
        goto exit;
    }

    SIN_FAMILY(ifr.ifr_netmask) = AF_INET;
    SIN_ADDR(ifr.ifr_netmask) = htonl(BTNETWORK_DEV_NETMASK);
    if (ioctl(fd, SIOCSIFNETMASK, &ifr) < 0)
    {
        ALOGE("ip_dun_config_netdev set netmask failed,  %s(%d)",  strerror(errno), errno);    
        goto exit;
    }

    ret = 0;

exit:
    close(fd);
    return ret;	
}
#endif
static int ip_dun_config_tundev(char* ifname, int ipaddr)
{
    struct ifreq ifr;
    int fd;	
    int ret = -1;

    fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0)
    {
        ALOGE("ip_dun_config_tundev create socket failed,  %s(%d)",  strerror(errno), errno);
        return -1;
    }

    memset(&ifr, 0, sizeof(ifr));
    strncpy(ifr.ifr_name, ifname, IFNAMSIZ-1);

    SIN_FAMILY(ifr.ifr_addr) = AF_INET;
    SIN_ADDR(ifr.ifr_addr) = htonl(ipaddr);

    if (ioctl(fd, SIOCSIFADDR, &ifr) < 0)
    {
        ALOGE("ip_dun_config_tundev set ip address failed,  %s(%d)",  strerror(errno), errno);
        goto exit;
    }

    SIN_FAMILY(ifr.ifr_netmask) = AF_INET;
    SIN_ADDR(ifr.ifr_netmask) = htonl(BTNETWORK_DEV_NETMASK);
    if (ioctl(fd, SIOCSIFNETMASK, &ifr) < 0)
    {
        ALOGE("ip_dun_config_tundev set netmask failed,  %s(%d)",  strerror(errno), errno);    
        goto exit;
    }

    if(ioctl( fd, SIOCGIFFLAGS, &ifr ) < 0 )
    {
        ALOGE("ip_dun_config_tundev get interface flag failed,  %s(%d)",  strerror(errno), errno);  
    } else
    {
        ALOGD("get interface flag : 0x%X", ifr.ifr_flags);
    }

    ifr.ifr_flags |= (IFF_UP | IFF_RUNNING);
    if (ioctl(fd, SIOCSIFFLAGS, &ifr) < 0)
    {
        ALOGE("ip_dun_config_tundev up interface failed,  %s(%d)",  strerror(errno), errno);    
        goto exit;
    }

    ret = 0;

exit:
    close(fd);
    return ret;	
}

int ip_dun_create_netdev(const char *ip_base_addr, char *ipcp_config_addr)
{
    struct ifreq ifr;
    int err;
    unsigned int ip1,ip2,ip3,ip4;
    unsigned long ip_net_dev;
    int i;

    ALOGI("[Dun]Create Dun Net dev...");
    
    btnfd = open("/dev/tun", O_RDWR);
    ALOGI("[Dun]Create Dun Net dev : btnfd=%d", btnfd);
    if (btnfd < 0)
    {
        ALOGE("[Dun]open tun device failed, %s(%d)", strerror(errno), errno);
        return -1;		
    }

    memset(&ifr, 0, sizeof(ifr));
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    ALOGI("[Dun]ifr_flags: %x", ifr.ifr_flags);

    strncpy(ifr.ifr_name, DUN_TUN_NAME, IFNAMSIZ);

    if( (err = ioctl(btnfd, TUNSETIFF, (void *) &ifr)) < 0 )
    {
        ALOGE("[Dun] ioctl set iff error:%s(%d)", strerror(errno), errno);
    }
        
    bt_dun_unit++;
    ALOGI("[Dun]new unit: %d", bt_dun_unit);

    sscanf(ip_base_addr, "%d.%d.%d.%d", &ip1, &ip2, &ip3, &ip4);
    ALOGI("[Dun]ip addr: %d.%d.%d.%d\n", ip1, ip2, ip3, ip4);

    ip3 += bt_dun_unit;
    ip_net_dev = ((ip1 << 24) & 0xff000000) |
                    ((ip2 << 16) & 0xff0000) |
                    ((ip3 << 8) & 0xff00) |
                    (ip4 & 0xff);
    
    ALOGI("[Dun]ip addr: 0x%lx\n", ip_net_dev);

    if( ip_dun_config_tundev(DUN_TUN_NAME, ip_net_dev) < 0)
    {
        return -1;
    }

    ip4 = 150;  // allocate "192.168.44.150" to remote device for ipcp configuration
    
    sprintf(ipcp_config_addr, "%d.%d.%d.%d", ip1, ip2, ip3, ip4);

    pthread_mutex_lock(&thread_mutex);
    pthread_create(&rcv_thread, NULL, ip_receive_packet, &btnfd);
    pthread_mutex_unlock(&thread_mutex);

    return 0;
}

void ip_dun_remove_netdev(void)
{
    close(btnfd);
}
void ip_dun_setfd(int fd)
{
     ALOGI("ip_dun_setfd: fd=%d", fd);
    pppbtfd = fd;
}

void ip_send_packet(unsigned char *pkt, int len)
{
    //ALOGI("send ip packet: len %d", len);
    if (write(btnfd, pkt, len) < 0)
    {
        ALOGE("write error: %s (%d)", strerror(errno), errno);
    }
}

static void *ip_receive_packet(void *usr_data)
{
    int fd = *(int *)usr_data;
    unsigned char buffer[PPP_MRU];
    int numRead;
    unsigned char outpacket_buf[PPP_MRU+PPP_HDRLEN]; /* buffer for outgoing packet */

    while (1)
    {
        ALOGI("receive ip packet : fd=%d", fd);
        numRead = read(fd, buffer, PPP_MRU);
        if (numRead < 0)
        {
            ALOGE("read error: %s (%d)", strerror(errno), errno);
            continue;			
        }
        
        outpacket_buf[0] = PPP_IP;
        memcpy(outpacket_buf + 1, buffer, numRead);
        ip_output(outpacket_buf, numRead + 1);
    }
	
    return NULL;	
}


/********************************************************************
 *
 * output - Output PPP packet.
 */

static void ip_output(unsigned char *p, int len)
{
    int proto;

    len = pppbt_encode_packet(p, len);
    ALOGI("write ppp packet to bt: len=%d, fd=%d", len, pppbtfd);

    if (len < PPP_HDRLEN)
	return;
    if (write(pppbtfd, p, len) < 0) {
	if (errno == EWOULDBLOCK || errno == EAGAIN || errno == ENOBUFS
	    || errno == ENXIO || errno == EIO || errno == EINTR)
	    ALOGW("write: warning: %m (%d)", errno);
	else
	    ALOGE("write: error(%d), %s", errno, strerror(errno));
    }
}

#endif
