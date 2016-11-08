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
 * ppp_btdun_loader.c
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to load ppp_btdun
 *
 * Author:
 * -------
 * SH Lai
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
#define LOG_TAG "ppp_btdun_loader"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <cutils/properties.h>
#include <unistd.h>
#include "utils/Log.h"

#include <sys/types.h>
#include <pwd.h>

#define BT_SOCK_NAME_EXT_ADP_SPP_DATA "ppp.bt.dun"
#define DEFAULT_IP_ADDRESS "192.168.44.1"

#define PPP_SET_NAME				"pppd_btdun"
#define PPP_SET_MODE				"silent"
#define PPP_SET_SOCKNAME		"pppbt-setsockname"
#define PPP_SET_IPADDR		"pppbt-setipaddr"
#define PPP_SET_DNSADDR		"ms-dns"
#define PPP_PARAM_NUM		10

#define SET_ARGV(param, argv) { \
    argv = param; \
    }




uid_t name_to_uid(char const *name)
{
  if (!name)
    return -1;
  long const buflen = sysconf(_SC_GETPW_R_SIZE_MAX);
  if (buflen == -1)
    return -1;
  // requires c99
  char buf[buflen];
  struct passwd pwbuf, *pwbufp;
  if (0 != getpwnam_r(name, &pwbuf, buf, buflen, &pwbufp)
      || !pwbufp)
    return -1;
  return pwbufp->pw_uid;
}


int
main(argc, argv)
    int argc;
    char *argv[];
{
    char *ppp_argv[11] = {NULL};
    int i = 0;
    char *ipbaseaddr = DEFAULT_IP_ADDRESS;
    char dns1addr[PATH_MAX] = "";
    char dns2addr[PATH_MAX] = "";
    int ret = -1;
#if 0
    uid_t btuid;

    btuid = name_to_uid("bluetooth");
    printf("bt uid is %d\n", btuid);
    setuid(btuid);
    if(getuid() != btuid) {
        printf("set uid failed\n");
    }
#endif
    ret = property_get("net.dns1", dns1addr, NULL);
    if (ret <= 0)
    {
        ALOGE("get property net.dns1 failed : %d", ret);
        return -1;
    }
    else
    {
        ret = property_get("net.dns2", dns2addr, "8.8.8.8");
        if (ret <= 0)
        {
            ALOGE("get property net.dns2 failed : %d", ret);
        }
    }
    ALOGD("ip=%s, dns1=%s, dns2=%s", ipbaseaddr, dns1addr, dns2addr);
    
    SET_ARGV(PPP_SET_NAME, ppp_argv[i++]);
    SET_ARGV(PPP_SET_MODE, ppp_argv[i++]);
    SET_ARGV(PPP_SET_SOCKNAME, ppp_argv[i++]);
    SET_ARGV(BT_SOCK_NAME_EXT_ADP_SPP_DATA, ppp_argv[i++]);
    SET_ARGV(PPP_SET_IPADDR, ppp_argv[i++]);
    SET_ARGV(ipbaseaddr, ppp_argv[i++]);
    SET_ARGV(PPP_SET_DNSADDR, ppp_argv[i++]);
    SET_ARGV(dns1addr, ppp_argv[i++]);
    SET_ARGV(PPP_SET_DNSADDR, ppp_argv[i++]);
    SET_ARGV(dns2addr, ppp_argv[i++]);
    
    pid_t pid = fork();
    if (pid == 0)
    {
        int ret = execve("/system/bin/pppd_btdun", ppp_argv, NULL);
        if (ret < 0)
        {
            ALOGE("[DUN][PPP] execute pppd_btdun failed: %s(%d)", strerror(errno), errno);
        }
    }
    else if (pid < 0)
    {
        ALOGE("[DUN][PPP] fork pppd_btdun failed: %s(%d)", strerror(errno), errno);
        return -1;
    }
    else
    {
        ALOGI("[DUN][PPP] save pppd_btdun pid: %d", pid);
    }
    return -1;
}
