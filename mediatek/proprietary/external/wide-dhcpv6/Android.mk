# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.


ifneq ($(TARGET_SIMULATOR),true)

LOCAL_PATH := $(call my-dir)
etc_dir := $(TARGET_OUT)/etc/wide-dhcpv6
cflags := -O2 -g -DYYTEXT_POINTER=1 -DHAVE_GETADDRINFO=1 -DHAVE_GETNAMEINFO=1 \
	-DHAVE_IF_NAMETOINDEX=1 -DHAVE_DAEMON=1 -DHAVE_WARNX=1 -DSTDC_HEADERS=1 \
	-DHAVE_SYS_TYPES_H=1 -DHAVE_SYS_STAT_H=1 -DHAVE_STDLIB_H=1 -DHAVE_STRING_H=1 \
	-DHAVE_MEMORY_H=1 -DHAVE_STRINGS_H=1 -DHAVE_INTTYPES_H=1 -DHAVE_STDINT_H=1 \
	-DHAVE_UNISTD_H=1 -DHAVE_FCNTL_H=1 -DHAVE_SYS_IOCTL_H=1 -DHAVE_SYS_TIME_H=1 \
	-DHAVE_SYSLOG_H=1 -DHAVE_UNISTD_H=1 -DTIME_WITH_SYS_TIME=1 \
	-DHAVE_STRUCT_TM_TM_ZONE=1 -DHAVE_TM_ZONE=1 -DHAVE_SIG_ATOMIC_T=1 -DGETPGRP_VOID=1 \
	-DSETPGRP_VOID=1 -DRETSIGTYPE=void -DHAVE_MKTIME=1 -DHAVE_SELECT=1 -DHAVE_SOCKET=1 \
	-DHAVE_ANSI_FUNC=1 -DHAVE_TAILQ_FOREACH_REVERSE_OLD=1 -DHAVE_STDARG_H=1 \
	-DLINE_MAX=2048 -DIPV6_READY_LOGO_P2_DHCPV6 -DHAVE_NETLINK \
	-DSYSCONFDIR=\"/etc/wide-dhcpv6\" -DLOCALDBDIR=\"/data/misc/dhcp\"
prebuilt_etc_files = dhcp6c.conf dhcp6cDNS.conf dhcp6cSIP.conf dhcp6cPD.conf dhcp6c.script dhcp6cPD.script dhcp6cctlkey dhcp6s.conf

#
# DHCPv6 client daemon
#
include $(CLEAR_VARS)
LOCAL_SRC_FILES := dhcp6c.c common.c config.c prefixconf.c dhcp6c_ia.c timer.c \
	dhcp6c_script.c if.c base64.c auth.c dhcp6_ctl.c addrconf.c lease.c \
	cfparse.c cftoken.c 
LOCAL_CFLAGS := $(cflags)
LOCAL_C_INCLUDES := vendor/mediatek/proprietary/external/libifaddrs
LOCAL_SHARED_LIBRARIES := libc libcutils
LOCAL_STATIC_LIBRARIES := libifaddrs
LOCAL_MODULE := dhcp6c
LOCAL_MULTILIB := 32
include $(BUILD_EXECUTABLE)

#
# DHCPv6 server daemon
#
include $(CLEAR_VARS)
LOCAL_SRC_FILES := dhcp6s.c common.c if.c config.c timer.c lease.c \
	base64.c auth.c dhcp6_ctl.c cfparse.c cftoken.c 
LOCAL_CFLAGS := $(cflags)
LOCAL_C_INCLUDES := vendor/mediatek/proprietary/external/libifaddrs
LOCAL_SHARED_LIBRARIES := libc libcutils
LOCAL_STATIC_LIBRARIES := libifaddrs
LOCAL_MODULE := dhcp6s
LOCAL_MULTILIB := 32
include $(BUILD_EXECUTABLE)

#
# DHCPv6 relay agent
#
#include $(CLEAR_VARS)
#LOCAL_SRC_FILES := dhcp6relay.c dhcp6relay_script.c common.c timer.c 
#LOCAL_CFLAGS := $(cflags)
#LOCAL_C_INCLUDES := vendor/mediatek/proprietary/external/libifaddrs
#LOCAL_SHARED_LIBRARIES := libc libcutils
#LOCAL_STATIC_LIBRARIES := libifaddrs
#LOCAL_MODULE := dhcp6relay
LOCAL_MULTILIB := 32
#LOCAL_MODULE_TAGS := user
#include $(BUILD_EXECUTABLE)

#
# DHCPv6 client and server control utility
#
include $(CLEAR_VARS)
LOCAL_SRC_FILES := dhcp6_ctlclient.c base64.c auth.c
LOCAL_CFLAGS := $(cflags)
LOCAL_C_INCLUDES := 
LOCAL_SHARED_LIBRARIES := libc libcutils
LOCAL_MODULE := dhcp6ctl
LOCAL_MULTILIB := 32
include $(BUILD_EXECUTABLE)

#
# Configuration Files
#
########################################################
#include $(CLEAR_VARS)
#LOCAL_SRC_FILES := dhcp6c.conf dhcp6cctlkey
#LOCAL_MODULE = dhcp6.config
#LOCAL_MODULE_TAGS := user
#LOCAL_MODULE_PATH := $(etc_dir)
#LOCAL_MODULE_CLASS := ETC
#include $(BUILD_PREBUILT)
#########################################################
$(foreach t, $(prebuilt_etc_files), \
  $(eval include $(CLEAR_VARS)) \
  $(eval LOCAL_SRC_FILES = $(t)) \
  $(eval LOCAL_MODULE = $(t)) \
  $(eval LOCAL_MODULE_PATH := $(etc_dir)) \
  $(eval LOCAL_MODULE_CLASS := ETC) \
  $(eval include $(BUILD_PREBUILT)) \
)  

endif
